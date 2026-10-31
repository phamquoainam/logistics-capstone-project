import { acceptRecoveryGrant, beginOtp, clearEntryProof, getOtpChallenge, getVerificationLocator, withEntryProof } from './entry-proof';
export type Subject = { accountId: string; displayName: string; verifiedEmail: string; securityVersion: number };
export type ProfileInput = { displayName: string; preferredLanguage: 'vi' | 'en'; businessContactEmail?: string; businessContactPhone?: string };
export type CustomerProfile = ProfileInput & { id: string; accountId: string; version: number };
export type VersionedProfile = { profile: CustomerProfile; etag: string };
export type Enrollment = { email: string; password: string; displayName: string; phone: string; consentVersion: string };
export type ApplicationAddress = { country: string; administrativeArea: string; locality: string; addressLine: string };
export type ApplicationDraft = { entityType?: 'COMPANY' | 'HOUSEHOLD_BUSINESS'; jurisdiction?: string; registeredName?: string; legalIdentifier?: string; registeredAddress?: ApplicationAddress; operatingAddress?: ApplicationAddress; representativeName?: string; representativeEmail?: string; representativeRelationship?: string; businessContactAttestationId?: string; evidenceIds?: string[]; declarations?: { accuracy: true; representation: true; consentVersion: string } };
export type MerchantApplication = { id: string; applicantAccountId: string; state: 'DRAFT' | 'SUBMITTED' | 'UNDER_REVIEW' | 'NEEDS_INFORMATION' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'; revision: number; version: number; draft?: ApplicationDraft; submission?: ApplicationDraft; reviewRegionId?: string; informationRequest?: string; organizationId?: string };
export type VersionedApplication = { application: MerchantApplication; etag: string };
export type ApplicationPage = { items: MerchantApplication[]; page: number; size: number; hasNext: boolean };
export type SessionState = Readonly<{ status: 'loading' | 'guest' | 'authenticated'; subject: Subject | null; error: string | null }>;
type Access = { accessToken: string; expiresAt: string; accountId: string };
type Lock = <T>(work: () => Promise<T>) => Promise<T>;
type Ports = { fetch: typeof fetch; lock: Lock; now: () => number };
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const record = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v);
const text = (v: unknown, maximum: number): v is string => typeof v === 'string' && v.length > 0 && v.length <= maximum;
const characters = (v: unknown, maximum: number): v is string => typeof v === 'string' && [...v].length > 0 && [...v].length <= maximum;
const version = (v: unknown): v is number => Number.isSafeInteger(v) && (v as number) >= 1;
function exact(v: Record<string, unknown>, required: string[], optional: string[] = []): boolean {
  return required.every(k => Object.hasOwn(v, k)) && Object.keys(v).every(k => required.includes(k) || optional.includes(k));
}
export class CustomerApiError extends Error {
  constructor(readonly code: string, readonly status = 0, readonly retryAfter?: number) { super(code); this.name = 'CustomerApiError'; }
}
const unknown = () => new CustomerApiError('OUTCOME_UNKNOWN');
function malformed(): never { throw new CustomerApiError('RESPONSE_INVALID'); }
const browserLock: Lock = work => {
  if (typeof navigator === 'undefined' || !navigator.locks) return Promise.reject(new CustomerApiError('UNSUPPORTED_BROWSER'));
  return navigator.locks.request('elogistics.customer.session', work);
};
/** Same-origin purpose-specific transport; no persistent token, demo fallback or blind POST retry. */
export function createCustomerClient(ports: Ports = { fetch: (...args) => fetch(...args), lock: browserLock, now: () => Date.now() }) {
  let access: Access | undefined, generation = 0, initialization: Promise<void> | undefined;
  let state: SessionState = Object.freeze({ status: 'loading', subject: null, error: null });
  let pending: { key: string; route: string; body: string; challenge: string } | undefined;
  const listeners = new Set<() => void>();
  const channel = typeof window !== 'undefined' && typeof BroadcastChannel !== 'undefined' ? new BroadcastChannel('elogistics.customer.session-state') : undefined;
  function publish(next: SessionState) { state = Object.freeze(next); listeners.forEach(f => f()); }
  function forget(error: string | null = null) { access = undefined; generation++; pending = undefined; publish({ status: 'guest', subject: null, error }); }
  if (channel) channel.onmessage = () => forget('SESSION_CHANGED');
  async function request(path: string, method = 'GET', body?: string, headers: Record<string, string> = {}, maximum = 65536): Promise<{ value: unknown; response: Response }> {
    let response: Response;
    try { response = await ports.fetch('/api/v1/' + path, { method, body, headers: body === undefined ? headers : { ...headers, 'Content-Type': 'application/json' }, credentials: 'same-origin', mode: 'same-origin', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(30000) }); }
    catch { throw method === 'GET' ? new CustomerApiError('NETWORK_UNAVAILABLE') : unknown(); }
    let value: unknown;
    try {
      const raw = await response.text(); if (raw.length > maximum) malformed();
      if (response.status !== 204) { if (!response.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) malformed(); value = JSON.parse(raw); }
      else if (raw !== '') malformed();
    } catch { throw method === 'GET' ? new CustomerApiError('RESPONSE_INVALID') : unknown(); }
    if (!response.ok) {
      const code = record(value) && typeof value.code === 'string' && /^[A-Z][A-Z0-9_]{0,63}$/.test(value.code) ? value.code : 'REQUEST_REJECTED';
      const retry = Number(response.headers.get('Retry-After'));
      if (response.status === 401 && access) forget('SESSION_INVALID');
      throw new CustomerApiError(code, response.status, Number.isInteger(retry) && retry > 0 && retry <= 86400 ? retry : undefined);
    }
    return { value, response };
  }
  async function csrf(): Promise<string> {
    const { value } = await request('customer-auth/csrf');
    if (!record(value) || !exact(value, ['challenge']) || !text(value.challenge, 512)) malformed(); return value.challenge;
  }
  function readAccess(value: unknown): Access {
    if (!record(value) || !exact(value, ['accessToken', 'expiresAt', 'accountId']) || !text(value.accessToken, 8192) || typeof value.accountId !== 'string' || !uuid.test(value.accountId) || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= ports.now() || Date.parse(value.expiresAt) > ports.now() + 901000) malformed();
    return value as Access;
  }
  async function subject(): Promise<Subject> {
    if (!access) throw new CustomerApiError('SESSION_INVALID');
    const expected = access.accountId, current = generation;
    const { value } = await request('customer-auth/me', 'GET', undefined, { Authorization: 'Bearer ' + access.accessToken });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    if (!record(value) || !exact(value, ['accountId', 'displayName', 'verifiedEmail', 'securityVersion']) || value.accountId !== expected || !characters(value.displayName, 160) || !text(value.verifiedEmail, 254) || !version(value.securityVersion)) malformed(); return value as Subject;
  }
  async function refreshLocked(): Promise<void> {
    try {
      const challenge = await csrf(), result = await request('customer-auth/refresh', 'POST', undefined, { 'X-CSRF-Token': challenge });
      access = readAccess(result.value); generation++; const user = await subject(); publish({ status: 'authenticated', subject: user, error: null });
    } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); throw error; }
  }
  async function ensureAccess(): Promise<void> { if (!access) throw new CustomerApiError('SESSION_INVALID'); if (Date.parse(access.expiresAt) <= ports.now() + 30000) await refreshLocked(); }
  async function logoutLocked(): Promise<void> {
    const challenge = await csrf(); await request('customer-auth/logout', 'POST', undefined, { 'X-CSRF-Token': challenge }); forget(); channel?.postMessage('changed');
  }
  async function preauth(route: string, input: object, key: string, noContent: boolean): Promise<string | undefined> {
    if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
    return ports.lock(async () => {
      if (access) throw new CustomerApiError('CUSTOMER_ALREADY_SIGNED_IN');
      const body = JSON.stringify(input);
      if (pending?.key === key && (pending.route !== route || pending.body !== body)) throw new CustomerApiError('IDEMPOTENCY_CONFLICT');
      const challenge = pending?.key === key ? pending.challenge : await csrf(); pending = { key, route, body, challenge };
      const { value, response } = await request('customer-auth/' + route, 'POST', body, { 'X-CSRF-Token': challenge, 'Idempotency-Key': key });
      if (noContent ? response.status !== 204 : response.status !== 202 || !record(value) || !exact(value, ['accepted', 'requestId']) || value.accepted !== true || typeof value.requestId !== 'string' || !uuid.test(value.requestId)) throw unknown();
      pending = undefined;
      return !noContent && record(value) ? value.requestId as string : undefined;
    });
  }
  function profile(value: unknown, response: Response): VersionedProfile {
    if (!record(value) || !exact(value, ['id', 'accountId', 'displayName', 'preferredLanguage', 'version'], ['businessContactEmail', 'businessContactPhone']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.accountId !== access?.accountId || !version(value.version) || !characters(value.displayName, 160) || !['vi', 'en'].includes(value.preferredLanguage as string) || ('businessContactEmail' in value && !text(value.businessContactEmail, 254)) || ('businessContactPhone' in value && !characters(value.businessContactPhone, 32))) malformed();
    const etag = response.headers.get('ETag'); if (etag !== '"v' + value.version + '"') malformed(); return { profile: value as CustomerProfile, etag };
  }
  function draft(value: unknown): value is ApplicationDraft {
    if (!record(value) || !exact(value, [], ['entityType', 'jurisdiction', 'registeredName', 'legalIdentifier', 'registeredAddress', 'operatingAddress', 'representativeName', 'representativeEmail', 'representativeRelationship', 'businessContactAttestationId', 'evidenceIds', 'declarations'])) return false;
    if ('entityType' in value && !['COMPANY', 'HOUSEHOLD_BUSINESS'].includes(value.entityType as string)) return false;
    for (const [name, max] of Object.entries({ jurisdiction: 64, registeredName: 240, legalIdentifier: 80, representativeName: 160, representativeEmail: 254, representativeRelationship: 160 })) if (name in value && !characters(value[name], max)) return false;
    for (const name of ['registeredAddress', 'operatingAddress']) if (name in value) {
      const a = value[name]; if (!record(a) || !exact(a, ['country', 'administrativeArea', 'locality', 'addressLine']) || typeof a.country !== 'string' || !/^[A-Z]{2}$/.test(a.country) || !characters(a.administrativeArea, 160) || !characters(a.locality, 160) || !characters(a.addressLine, 500)) return false;
    }
    if ('businessContactAttestationId' in value && (typeof value.businessContactAttestationId !== 'string' || !uuid.test(value.businessContactAttestationId))) return false;
    if ('evidenceIds' in value && (!Array.isArray(value.evidenceIds) || value.evidenceIds.length > 5 || new Set(value.evidenceIds).size !== value.evidenceIds.length || value.evidenceIds.some(id => typeof id !== 'string' || !uuid.test(id)))) return false;
    if ('declarations' in value) { const d = value.declarations; if (!record(d) || !exact(d, ['accuracy', 'representation', 'consentVersion']) || d.accuracy !== true || d.representation !== true || !characters(d.consentVersion, 64)) return false; }
    return true;
  }
  function application(value: unknown): MerchantApplication {
    if (!record(value) || !exact(value, ['id', 'applicantAccountId', 'state', 'revision', 'version'], ['draft', 'submission', 'reviewRegionId', 'informationRequest', 'organizationId']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.applicantAccountId !== access?.accountId || !version(value.revision) || !version(value.version) || !['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED', 'WITHDRAWN'].includes(value.state as string)) malformed();
    for (const name of ['draft', 'submission']) if (name in value && !draft(value[name])) malformed();
    if ('reviewRegionId' in value && (typeof value.reviewRegionId !== 'string' || !uuid.test(value.reviewRegionId))) malformed();
    if ('informationRequest' in value && !characters(value.informationRequest, 1000)) malformed();
    if (value.state === 'APPROVED' ? typeof value.organizationId !== 'string' || !uuid.test(value.organizationId) : 'organizationId' in value) malformed();
    return value as MerchantApplication;
  }
  async function applicationWrite(path: string, method: 'POST' | 'PUT', input: ApplicationDraft, key: string, etag?: string): Promise<VersionedApplication> {
    if (!uuid.test(key) || !draft(input) || method === 'PUT' && !/^"v[1-9][0-9]*"$/.test(etag ?? '')) throw new CustomerApiError('VALIDATION_FAILED');
    await ensureAccess(); const current = generation, challenge = await csrf();
    const result = await request(path, method, JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'Idempotency-Key': key, ...(etag ? { 'If-Match': etag } : {}) });
    if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
    try { const value = application(result.value); if (result.response.status !== (method === 'POST' ? 201 : 200) || result.response.headers.get('ETag') !== '"v' + value.version + '"') malformed(); return { application: value, etag: '"v' + value.version + '"' }; } catch { throw unknown(); }
  }
  return {
    getSnapshot: () => state,
    subscribe: (listener: () => void) => { listeners.add(listener); return () => { listeners.delete(listener); }; },
    initialize(): Promise<void> {
      initialization ??= ports.lock(async () => {
        try { await refreshLocked(); } catch (error) {
          if (error instanceof CustomerApiError && error.status === 401) {
            // Explicitly clear the known invalid/replayed family, without retrying refresh.
            try { await logoutLocked(); } catch { forget('SESSION_INVALID'); }
          }
        }
      }).catch(error => { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); }); return initialization;
    },
    async login(email: string, password: string): Promise<void> {
      await ports.lock(async () => {
        try {
          const challenge = await csrf(), { value } = await request('customer-auth/login', 'POST', JSON.stringify({ email, password }), { 'X-CSRF-Token': challenge });
          access = readAccess(value); generation++; pending = undefined; const user = await subject(); publish({ status: 'authenticated', subject: user, error: null }); channel?.postMessage('changed');
        } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'SESSION_INVALID'); throw error; }
      });
    },
    logout: () => ports.lock(async () => { try { await logoutLocked(); } catch (error) { forget(error instanceof CustomerApiError ? error.code : 'OUTCOME_UNKNOWN'); throw error; } }),
    discardPending() { pending = undefined; },
    async enroll(input: Enrollment, key: string): Promise<void> { const id = await preauth('enrollments', input, key, false); beginOtp('verification', id!, input.email, ports.now() + 120000); },
    async resend(email: string, key: string): Promise<void> { const id = await preauth('verification-resend', { email }, key, false); beginOtp('verification', id!, email, ports.now() + 120000); },
    async recover(email: string, key: string): Promise<void> { const id = await preauth('recovery', { email }, key, false); beginOtp('recovery', id!, email, ports.now() + 120000); },
    async verifyRecovery(code: string, key: string): Promise<void> {
      const locator = getOtpChallenge('recovery');
      if (!locator || !/^\d{6}$/.test(code) || !uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ports.lock(async () => {
        if (access) throw new CustomerApiError('CUSTOMER_ALREADY_SIGNED_IN');
        const body = JSON.stringify({ proofId: locator.proofId, proof: code });
        if (pending?.key === key && (pending.route !== 'recovery-verifications' || pending.body !== body)) throw new CustomerApiError('IDEMPOTENCY_CONFLICT');
        const challenge = pending?.key === key ? pending.challenge : await csrf(); pending = { key, route: 'recovery-verifications', body, challenge };
        const { value, response } = await request('customer-auth/recovery-verifications', 'POST', body, { 'X-CSRF-Token': challenge, 'Idempotency-Key': key });
        if (response.status !== 200 || !record(value) || !exact(value, ['proofId', 'proof', 'expiresAt']) || value.proofId !== locator.proofId || typeof value.proof !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(value.proof) || typeof value.expiresAt !== 'string' || !Number.isFinite(Date.parse(value.expiresAt)) || Date.parse(value.expiresAt) <= ports.now() || Date.parse(value.expiresAt) > ports.now() + 121000) throw unknown();
        acceptRecoveryGrant({ proofId: locator.proofId, proof: value.proof }); pending = undefined;
      });
    },
    async verify(key: string, code?: string): Promise<void> {
      const locator = getVerificationLocator();
      if (locator) {
        if (!code || !/^\d{6}$/.test(code)) throw new CustomerApiError('VALIDATION_FAILED');
        await preauth('verifications', { proofId: locator.proofId, proof: code }, key, true);
      } else await withEntryProof('verification', proof => preauth('verifications', proof, key, true));
      clearEntryProof();
    },
    async confirmRecovery(newPassword: string, key: string): Promise<void> { await withEntryProof('recovery', proof => preauth('recovery-confirm', { ...proof, newPassword }, key, true)); clearEntryProof(); },
    readProfile: () => ports.lock(async () => {
      await ensureAccess(); const current = generation, result = await request('customer/profile', 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); return profile(result.value, result.response);
    }),
    updateProfile: (input: ProfileInput, etag: string, key: string) => ports.lock(async () => {
      if (!/^"v[1-9][0-9]*"$/.test(etag) || !uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, challenge = await csrf();
      const result = await request('customer/profile', 'PUT', JSON.stringify(input), { Authorization: 'Bearer ' + access!.accessToken, 'X-CSRF-Token': challenge, 'If-Match': etag, 'Idempotency-Key': key });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); try { return profile(result.value, result.response); } catch { throw unknown(); }
    }),
    profileReceipt: (key: string) => ports.lock(async () => {
      if (!uuid.test(key)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation;
      const { value } = await request('onboarding-receipts/updateCustomerProfile/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== 'updateCustomerProfile' || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed();
      return { targetId: value.targetId, version: value.version };
    }),
    listApplications: (page = 0, search = '', state = '') => ports.lock(async (): Promise<ApplicationPage> => {
      if (!Number.isSafeInteger(page) || page < 0 || search.length > 160) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation, query = new URLSearchParams({ page: String(page), size: '20', ...(search ? { search } : {}), ...(state ? { state } : {}) });
      const { value } = await request('merchant-applications?' + query, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken }, 4 * 1024 * 1024);
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['items', 'page', 'size', 'hasNext']) || !Array.isArray(value.items) || value.items.length > 20 || value.page !== page || value.size !== 20 || typeof value.hasNext !== 'boolean') malformed();
      return { items: value.items.map(application), page, size: 20, hasNext: value.hasNext };
    }),
    readApplication: (id: string) => ports.lock(async (): Promise<VersionedApplication> => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); await ensureAccess(); const current = generation;
      const result = await request('merchant-applications/' + id, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED'); const value = application(result.value), etag = result.response.headers.get('ETag'); if (value.id !== id || etag !== '"v' + value.version + '"') malformed(); return { application: value, etag };
    }),
    createApplication: (input: ApplicationDraft, key: string) => ports.lock(() => applicationWrite('merchant-applications', 'POST', input, key)),
    updateApplication: (id: string, input: ApplicationDraft, etag: string, key: string) => ports.lock(() => {
      if (!uuid.test(id)) throw new CustomerApiError('VALIDATION_FAILED'); return applicationWrite('merchant-applications/' + id, 'PUT', input, key, etag);
    }),
    applicationReceipt: (operation: 'createApplicationDraft' | 'updateApplicationDraft', key: string) => ports.lock(async () => {
      if (!uuid.test(key) || !['createApplicationDraft', 'updateApplicationDraft'].includes(operation)) throw new CustomerApiError('VALIDATION_FAILED');
      await ensureAccess(); const current = generation;
      const { value } = await request('onboarding-receipts/' + operation + '/' + key, 'GET', undefined, { Authorization: 'Bearer ' + access!.accessToken });
      if (current !== generation) throw new CustomerApiError('SESSION_CHANGED');
      if (!record(value) || !exact(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== operation || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) malformed(); return { targetId: value.targetId, version: value.version };
    }),
    dispose() { forget(); channel?.close(); listeners.clear(); },
  };
}
export const customerClient = createCustomerClient();
