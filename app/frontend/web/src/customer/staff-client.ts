import { clearOperationsInvitation, withOperationsInvitation } from './operations-invitation-proof';
import { claimEvidenceList, evidenceTransfer } from './claim-evidence';
import type { ClaimEvidence } from './claim-evidence';
import { claimDetail, claimPage } from './claim-intake-data';
import { CustomerApiError } from './client';
import { organizationTicket, organizationTicketDetail, organizationTicketPage, organizationSupportReceipt } from './organization-support-data';
import { clearStaffInvitation, withStaffInvitation } from './staff-invitation-proof';
import { supportDetail, supportPage, supportSummary } from './support-ticket-data';

export type StaffSubject = { accountId: string; displayName: string; principalType: 'STAFF'; purpose: 'SUPPORT'; environment: 'LOCAL' | 'TEST' };
type PurposeSubject<P> = Omit<StaffSubject, 'purpose'> & { purpose: P };
type State<P = 'SUPPORT'> = Readonly<{ status: 'loading' | 'guest' | 'authenticated'; subject: PurposeSubject<P> | null; error: string | null }>;
type Ports = { fetch: typeof fetch; lock: <T>(work: () => Promise<T>) => Promise<T>; now: () => number };
const id = (v: unknown): v is string => typeof v === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000';
const object = (v: unknown): v is Record<string, unknown> => v !== null && typeof v === 'object' && !Array.isArray(v);
const invalid = () => new CustomerApiError('RESPONSE_INVALID');
const requireId = (v: string) => { if (!id(v)) throw new CustomerApiError('VALIDATION_FAILED'); return v; };
const browserLock: Ports['lock'] = work => typeof navigator !== 'undefined' && navigator.locks ? navigator.locks.request('argopost.staff.session', work) : Promise.reject(new CustomerApiError('UNSUPPORTED_BROWSER'));
/** Fixed Staff purpose. No Customer session, localStorage, public enrollment, or blind command retry. */
export function createStaffClient<P extends 'SUPPORT' | 'OPERATIONS' = 'SUPPORT'>(ports: Ports = { fetch: (...args) => fetch(...args), lock: browserLock, now: () => Date.now() }, purpose: P = 'SUPPORT' as P) {
  const prefix = purpose === 'OPERATIONS' ? 'ops-auth' : 'staff-auth';
  let access: { token: string; accountId: string; expires: number } | undefined;
  let state: State<P> = Object.freeze({ status: 'loading', subject: null, error: null });
  const listeners = new Set<() => void>(); let initialization: Promise<void> | undefined;
  const publish = (next: State<P>) => { state = Object.freeze(next); listeners.forEach(f => f()); };
  const forget = (error: string | null = null) => { access = undefined; publish({ status: 'guest', subject: null, error }); };
  async function request(path: string, method = 'GET', body?: unknown, headers: Record<string, string> = {}) {
    let response: Response;
    try { response = await ports.fetch('/api/v1/' + path, { method, headers: body === undefined ? headers : { ...headers, 'Content-Type': 'application/json' }, body: body === undefined ? undefined : JSON.stringify(body), credentials: 'same-origin', mode: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(30000) }); }
    catch { throw new CustomerApiError(method === 'GET' ? 'NETWORK_UNAVAILABLE' : 'OUTCOME_UNKNOWN'); }
    let value: unknown;
    try { const text = await response.text(); if (text.length > 1048576) throw invalid(); if (response.status === 204) { if (text) throw invalid(); } else { const contentType = response.headers.get('Content-Type')?.split(';', 1)[0];if (contentType !== 'application/json' && !(purpose === 'OPERATIONS' && headers.Accept === 'application/vnd.argopost.station-cargo.v2+json' && contentType === headers.Accept)) throw invalid(); value = JSON.parse(text); } }
    catch { throw new CustomerApiError(method === 'GET' ? 'RESPONSE_INVALID' : 'OUTCOME_UNKNOWN'); }
    if (!response.ok) { if (response.status === 401) forget('SESSION_INVALID'); const code = object(value) && typeof value.code === 'string' && /^[A-Z_]{1,64}$/.test(value.code) ? value.code : 'REQUEST_REJECTED'; throw new CustomerApiError(code, response.status); }
    return value;
  }
  async function csrf() { const value = await request(prefix + '/csrf'); if (!object(value) || Object.keys(value).length !== 1 || typeof value.csrfToken !== 'string' || value.csrfToken.length < 16 || value.csrfToken.length > 512) throw invalid(); return value.csrfToken; }
  function session(value: unknown) {
    if (!object(value) || Object.keys(value).length !== 3 || !id(value.accountId) || typeof value.accessToken !== 'string' || value.accessToken.length < 100 || value.accessToken.length > 8192 || typeof value.expiresAt !== 'string') throw invalid();
    const expires = Date.parse(value.expiresAt); if (!Number.isFinite(expires) || expires <= ports.now() || expires > ports.now() + 901000) throw invalid();
    access = { token: value.accessToken, accountId: value.accountId, expires };
  }
  async function me() {
    if (!access) throw new CustomerApiError('SESSION_INVALID');
    const v = await request(prefix + '/me', 'GET', undefined, { Authorization: 'Bearer ' + access.token });
    if (!object(v) || Object.keys(v).length !== 5 || v.accountId !== access.accountId || typeof v.displayName !== 'string' || !v.displayName.trim() || [...v.displayName].length > 160 || v.principalType !== 'STAFF' || v.purpose !== purpose || !['LOCAL', 'TEST'].includes(String(v.environment))) { forget('RESPONSE_INVALID'); throw invalid(); }
    publish({ status: 'authenticated', subject: v as PurposeSubject<P>, error: null });
  }
  async function refresh() { session(await request(prefix + '/refresh', 'POST', undefined, { 'X-CSRF-Token': await csrf() })); await me(); }
  async function authorized<T>(path: string, parse: (v: unknown) => T, method = 'GET', body?: unknown, key?: string, version?: number, intake = false) {
    if (!access || access.expires <= ports.now() + 30000) await refresh();
    if (!access) throw new CustomerApiError('SESSION_INVALID');
    const headers: Record<string, string> = { Authorization: 'Bearer ' + access.token };
    if (purpose === 'OPERATIONS' && intake) headers.Accept = 'application/vnd.argopost.station-cargo.v2+json';
    if (method !== 'GET') { headers['X-CSRF-Token'] = await csrf(); headers['Idempotency-Key'] = requireId(key ?? ''); if (!intake || version !== undefined) { if (!Number.isSafeInteger(version) || Number(version) < (intake ? 0 : 1)) throw new CustomerApiError('VALIDATION_FAILED'); headers['If-Match'] = '"' + (intake ? 'i' : 'v') + version + '"'; } }
    try { return parse(await request(path, method, body, headers)); } catch (e) { if (e instanceof CustomerApiError) throw e; throw invalid(); }
  }
  return {
    subscribe: (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener); }; }, getSnapshot: () => state,
    initialize: () => initialization ??= ports.lock(async () => { try { await refresh(); } catch (e) { forget(e instanceof CustomerApiError ? e.code : 'STAFF_UNAVAILABLE'); } }),
    login: (email: string, password: string) => ports.lock(async () => { forget(); session(await request(prefix + '/login', 'POST', { email, password }, { 'X-CSRF-Token': await csrf() })); await me(); }),
    logout: () => ports.lock(async () => { try { await request(prefix + '/logout', 'POST', undefined, { 'X-CSRF-Token': await csrf() }); } finally { forget(); } }),
    accept: (password: string, key: string) => ports.lock(async () => { const challenge = await csrf(); await (purpose === 'OPERATIONS' ? withOperationsInvitation : withStaffInvitation)((proofId, token) => request(prefix + '/invitations/accept', 'POST', { proofId, token, password }, { 'Idempotency-Key': requireId(key), 'X-CSRF-Token': challenge })); if (purpose === 'OPERATIONS') clearOperationsInvitation(); else clearStaffInvitation(); }),
    operationsRequest: <T>(path: string, parse: (v: unknown) => T, method = 'GET', body?: unknown, key?: string, version?: number) => ports.lock(() => {
      if (purpose !== 'OPERATIONS' || !/^ops\/sites(?:\/[a-f0-9-]{36}(?:\/.*)?)?$/.test(path)) throw new CustomerApiError('AUTHORITY_REQUIRED');
      return authorized(path, parse, method, body, key, version, true);
    }),
    operationsLabel: (site: string, shipment: string) => ports.lock(async () => {
      if (purpose !== 'OPERATIONS') throw new CustomerApiError('AUTHORITY_REQUIRED');requireId(site);requireId(shipment);
      if (!access || access.expires <= ports.now() + 30000) await refresh();if (!access) throw new CustomerApiError('SESSION_INVALID');
      const response = await ports.fetch('/api/v1/ops/sites/' + site + '/shipments/' + shipment + '/labels.pdf', { headers: { Authorization: 'Bearer ' + access.token }, credentials: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(30000) });
      if (response.status === 401) forget('SESSION_INVALID');if (!response.ok || !response.headers.get('Content-Type')?.startsWith('application/pdf') || !response.headers.get('Cache-Control')?.includes('no-store')) throw invalid();
      const bytes = new Uint8Array(await response.arrayBuffer());if (bytes.length < 5 || bytes.length > 16777216 || new TextDecoder().decode(bytes.slice(0,5)) !== '%PDF-') throw invalid();return new Blob([bytes], { type: 'application/pdf' });
    }),
    claimEvidence: (org: string, intake: string) => ports.lock(() => { requireId(org); return authorized('support-operations/organization-claim-intakes/' + requireId(intake) + '/evidence', v => claimEvidenceList(v, intake)); }),
    downloadClaimEvidence: (org: string, metadata: ClaimEvidence) => ports.lock(async () => { requireId(org); if (!access || access.expires <= ports.now() + 30000) await refresh(); if (!access) throw new CustomerApiError('SESSION_INVALID'); try { const result = await evidenceTransfer(ports.fetch, '/api/v1/support-operations/organization-claim-intakes/' + requireId(metadata.intakeId) + '/evidence/' + requireId(metadata.id), access.token, metadata); if (!(result instanceof Blob)) throw invalid(); return result; } catch (e) { if (e instanceof CustomerApiError && e.status === 401) forget('SESSION_INVALID'); throw e; } }),
    claimIntakes: (org: string, ticket: string, page = 0) => ports.lock(() => { requireId(org); requireId(ticket); if (!Number.isSafeInteger(page) || page < 0 || page > 100000) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/organization-cases/' + ticket + '/claim-intakes?page=' + page, v => claimPage(v, org, ticket, page)); }),
    claimIntake: (org: string, id: string) => ports.lock(() => authorized('support-operations/organization-claim-intakes/' + requireId(id), v => claimDetail(v, requireId(org), id))),
    commandClaim: (org: string, id: string, action: string, message: string, version: number, key: string) => ports.lock(() => { if (!['BEGIN_REVIEW', 'REQUEST_INFORMATION', 'CLOSE_WITHOUT_DECISION'].includes(action)) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/organization-claim-intakes/' + requireId(id) + '/commands', v => claimDetail(v, requireId(org), id), 'POST', { action, message }, key, version); }),
    organizationSupportQueue: (org: string, page = 0) => ports.lock(() => { requireId(org); if (!Number.isSafeInteger(page) || page < 0 || page > 100000) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/organization-queue?organizationId=' + org + '&page=' + page + '&size=20', v => organizationTicketPage(v, org, page)); }),
    organizationSupportCase: (org: string, target: string) => ports.lock(() => authorized('support-operations/organization-cases/' + requireId(target), v => organizationTicketDetail(v, requireId(org), target, true))),
    assignOrganizationSupportCase: (org: string, target: string, assignee: string, version: number, key: string) => ports.lock(() => authorized('support-operations/organization-cases/' + requireId(target) + '/assignments', v => { const t = organizationTicket(v, requireId(org)); if (t.id !== target) throw invalid(); return t; }, 'POST', { assigneeAccountId: requireId(assignee) }, key, version)),
    commandOrganizationSupportCase: (org: string, target: string, action: string, message: string, version: number, key: string) => ports.lock(() => { if (!['START', 'REPLY', 'REQUEST_INFORMATION', 'RESOLVE', 'REOPEN'].includes(action)) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/organization-cases/' + requireId(target) + (action === 'REPLY' ? '/replies' : '/transitions'), v => organizationTicketDetail(v, requireId(org), target, true), 'POST', action === 'REPLY' ? { message } : { action, message }, key, version); }),
    organizationCarrierSupportReceipt: (op: 'CARRIER_ASSIGN' | 'CARRIER_REPLY' | 'CARRIER_TRANSITION', key: string) => ports.lock(() => { if (!['CARRIER_ASSIGN', 'CARRIER_REPLY', 'CARRIER_TRANSITION'].includes(op)) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/organization-receipts/' + op + '/' + requireId(key), organizationSupportReceipt); }),
    readSupportQueue: (page = 0) => ports.lock(() => { if (!Number.isSafeInteger(page) || page < 0 || page > 100000) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/queue?page=' + page + '&size=20', v => supportPage(v, page)); }),
    readSupportCase: (target: string) => ports.lock(() => authorized('support-operations/cases/' + requireId(target), v => { const d = supportDetail(v, true); if (d.ticket.id !== target) throw invalid(); return d; })),
    assignSupportCase: (target: string, assignee: string, version: number, key: string) => ports.lock(() => authorized('support-operations/cases/' + requireId(target) + '/assignments', v => { const s = supportSummary(v); if (s.id !== target) throw invalid(); return s; }, 'POST', { assigneeAccountId: requireId(assignee) }, key, version)),
    transitionSupportCase: (target: string, action: string, message: string, version: number, key: string) => ports.lock(() => { if (!['START', 'REQUEST_INFORMATION', 'RESOLVE', 'REOPEN'].includes(action)) throw new CustomerApiError('VALIDATION_FAILED'); return authorized('support-operations/cases/' + requireId(target) + '/transitions', v => { const d = supportDetail(v, true); if (d.ticket.id !== target) throw invalid(); return d; }, 'POST', { action, message }, key, version); }),
  };
}
export const staffClient = createStaffClient();
