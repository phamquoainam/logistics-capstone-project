/** Accepted SYSTEM authentication + Owner-approved ADM read transport; no grant or write operation. */
type Subject = Readonly<{ id: string; displayName: string; authorizationVersion: number }>;
type State = Readonly<{ status: 'loading' | 'guest' | 'authenticated'; subject: Subject | null; error: string | null }>;
type Ports = { fetch: typeof fetch; now: () => number; protocol: () => string; lock: <T>(work: () => Promise<T>) => Promise<T> };
type Account = { id: string; displayName: string; signature: string };
export class AdminSessionError extends Error {
  constructor(readonly code: string) { super(code); this.name = 'AdminSessionError'; }
}
const record = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value);
const uuid = (value: unknown): value is string => typeof value === 'string' && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(value) && value !== '00000000-0000-0000-0000-000000000000';
const invalid = () => new AdminSessionError('RESPONSE_INVALID');
function account(value: unknown): Account {
  if (!record(value) || Object.keys(value).length !== 8 || !uuid(value.id) || typeof value.username !== 'string' || !/\S/u.test(value.username) || [...value.username].length > 80 || typeof value.email !== 'string' || value.email.length > 254 || typeof value.displayName !== 'string' || !value.displayName.trim() || [...value.displayName].length > 160 || value.status !== 'ACTIVE' || value.emailVerified !== true || !Number.isSafeInteger(value.version) || Number(value.version) < 1 || !Array.isArray(value.grants) || value.grants.length < 1 || value.grants.length > 6) throw invalid();
  const roles = new Set<string>(), realms = new Set<string>();
  for (const grant of value.grants as unknown[]) {
    if (!record(grant) || Object.keys(grant).length !== 3 || typeof grant.role !== 'string' || grant.scopeType !== 'IDENTITY' || !uuid(grant.scopeId) || roles.has(grant.role)) throw invalid();
    roles.add(grant.role); realms.add(grant.scopeId);
  }
  if (realms.size !== 1 || !(roles.size === 1 && roles.has('IDENTITY_MANAGEMENT')) && (!roles.has('ADMIN_DASHBOARD_READ') || [...roles].some(role => !['ADMIN_DASHBOARD_READ', 'ADMIN_MERCHANT_READ', 'ADMIN_NETWORK_READ', 'ADMIN_SHIPMENT_READ', 'ADMIN_FINANCE_READ', 'ADMIN_TECHNICAL_READ'].includes(role)))) throw invalid();
  return { id: value.id, displayName: value.displayName, signature: JSON.stringify([value.version, [...realms].sort(), [...roles].sort()]) };
}
const browserLock: Ports['lock'] = work => typeof navigator !== 'undefined' && navigator.locks
  ? navigator.locks.request('argopost.system.session', work)
  : Promise.reject(new AdminSessionError('UNSUPPORTED_BROWSER'));

export function createAdminSession(ports: Ports = { fetch: (...args) => fetch(...args), now: Date.now, protocol: () => location.protocol, lock: browserLock }) {
  let token: string | undefined;
  let expiresAt = 0;
  let signature: string | undefined, authorizationVersion = 0;
  let state: State = Object.freeze({ status: 'loading', subject: null, error: null });
  let initialization: Promise<void> | undefined;
  const listeners = new Set<() => void>();
  const publish = (next: State) => { state = Object.freeze(next); listeners.forEach(fn => fn()); };
  const forget = (error: string | null = null) => { token = undefined; expiresAt = 0; signature = undefined; publish({ status: 'guest', subject: null, error }); };
  const admit = (current: Account) => { if (signature !== current.signature) authorizationVersion++; signature = current.signature; publish({ status: 'authenticated', subject: Object.freeze({ id: current.id, displayName: current.displayName, authorizationVersion }), error: null }); };
  async function request(operation: 'csrf' | 'login' | 'refresh' | 'me' | 'logout', body?: unknown, csrf?: string) {
    if (ports.protocol() !== 'https:') throw new AdminSessionError('HTTPS_REQUIRED');
    const read = operation === 'csrf' || operation === 'me';
    const headers: Record<string, string> = {};
    if (csrf) headers['X-CSRF-Token'] = csrf;
    if (operation === 'me') { if (!token) throw new AdminSessionError('SESSION_INVALID'); headers.Authorization = 'Bearer ' + token; }
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    let response: Response;
    try { response = await ports.fetch('/api/v1/auth/' + operation, { method: read ? 'GET' : 'POST', body: body === undefined ? undefined : JSON.stringify(body), headers, credentials: 'same-origin', mode: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(15000) }); }
    catch { throw new AdminSessionError(read ? 'NETWORK_UNAVAILABLE' : 'OUTCOME_UNKNOWN'); }
    if (!response.ok) {
      // Never display or retain raw provider messages, exception bodies or identifiers.
      throw new AdminSessionError(response.status === 429 ? 'RATE_LIMITED' : response.status === 401 ? operation === 'login' ? 'INVALID_CREDENTIALS' : 'SESSION_INVALID' : response.status === 403 ? 'ACCESS_DENIED' : 'SERVICE_UNAVAILABLE');
    }
    if (response.status !== (operation === 'logout' ? 204 : 200)) throw invalid();
    const text = await response.text().catch(() => { throw new AdminSessionError(read ? 'NETWORK_UNAVAILABLE' : 'OUTCOME_UNKNOWN'); });
    if (operation === 'logout') { if (response.status !== 204 || text !== '') throw invalid(); return undefined; }
    if (text.length > 65536 || !response.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) throw invalid();
    try { return JSON.parse(text) as unknown; } catch { throw invalid(); }
  }
  async function csrf() {
    const value = await request('csrf');
    if (!record(value) || Object.keys(value).length !== 1 || typeof value.csrfToken !== 'string' || value.csrfToken.length < 16 || value.csrfToken.length > 4096) throw invalid();
    return value.csrfToken;
  }
  async function establish(value: unknown) {
    if (!record(value) || Object.keys(value).length !== 5 || typeof value.accessToken !== 'string' || !/^[A-Za-z0-9_.-]{32,8192}$/.test(value.accessToken) || value.tokenType !== 'Bearer' || !Number.isSafeInteger(value.expiresIn) || Number(value.expiresIn) < 1 || Number(value.expiresIn) > 900 || typeof value.sessionExpiresAt !== 'string' || !Number.isFinite(Date.parse(value.sessionExpiresAt)) || Date.parse(value.sessionExpiresAt) <= ports.now()) throw invalid();
    const initial = account(value.user);
    token = value.accessToken;
    expiresAt = Math.min(ports.now() + Number(value.expiresIn) * 1000, Date.parse(value.sessionExpiresAt));
    const current = account(await request('me'));
    if (current.id !== initial.id) throw invalid();
    admit(current);
  }
  async function restore() {
    try { await ports.lock(async () => { await establish(await request('refresh', undefined, await csrf())); }); }
    catch (error) { forget(error instanceof AdminSessionError ? error.code : 'SERVICE_UNAVAILABLE'); }
  }
  return {
    getSnapshot: () => state,
    subscribe: (fn: () => void) => { listeners.add(fn); return () => { listeners.delete(fn); }; },
    initialize: () => initialization ??= restore(),
    discard: () => forget('SESSION_INVALID'),
    read: <T>(operation: 'context' | 'dashboard' | 'shipments' | 'merchant-applications' | 'network' | 'platform', parameters: URLSearchParams, parse: (value: unknown) => T): Promise<T> => ports.lock(async () => {
      try {
        if (!['context', 'dashboard', 'shipments', 'merchant-applications', 'network', 'platform'].includes(operation)) throw new AdminSessionError('VALIDATION_FAILED');
        if (ports.protocol() !== 'https:') throw new AdminSessionError('HTTPS_REQUIRED');
        if (state.status !== 'authenticated' || !token) throw new AdminSessionError('SESSION_INVALID');
        if (expiresAt <= ports.now() + 30000) await establish(await request('refresh', undefined, await csrf()));
        const path = operation === 'dashboard' ? '' : '/' + operation;
        let response: Response;
        try { response = await ports.fetch('/api/v1/admin/dashboard' + path + (parameters.size ? '?' + parameters.toString() : ''), { method: 'GET', headers: { Authorization: 'Bearer ' + token }, credentials: 'same-origin', mode: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(15000) }); }
        catch { throw new AdminSessionError('NETWORK_UNAVAILABLE'); }
        if (!response.ok) throw new AdminSessionError(response.status === 401 ? 'SESSION_INVALID' : response.status === 403 ? 'ACCESS_DENIED' : response.status === 409 ? 'SNAPSHOT_EXPIRED' : response.status === 422 ? 'SOURCE_UNAVAILABLE' : response.status === 400 ? 'VALIDATION_FAILED' : 'SERVICE_UNAVAILABLE');
        const text = await response.text();
        if (response.status !== 200 || text.length > 1048576 || !response.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) throw invalid();
        let value: unknown; try { value = JSON.parse(text); } catch { throw invalid(); }
        try { return parse(value); } catch (failure) { if (failure instanceof AdminSessionError) throw failure; throw invalid(); }
      } catch (failure) { if (failure instanceof AdminSessionError && failure.code === 'SESSION_INVALID') forget('SESSION_INVALID'); throw failure; }
    }),
    checkSession: async () => {
      try { await ports.lock(async () => {
        if (state.status !== 'authenticated' || !state.subject) return;
        if (expiresAt <= ports.now() + 30000) await establish(await request('refresh', undefined, await csrf()));
        else {
          const current = account(await request('me'));
          if (current.id !== state.subject.id) throw invalid();
          admit(current);
        }
      }); }
      catch (error) { forget(error instanceof AdminSessionError ? error.code : 'SERVICE_UNAVAILABLE'); }
    },
    login: async (username: string, password: string) => {
      try { await ports.lock(async () => {
        forget();
        if (!/\S/u.test(username) || [...username].length > 80 || /[\uD800-\uDFFF]/u.test(username) || !password || /[\uD800-\uDFFF]/u.test(password) || new TextEncoder().encode(password).length > 72) throw new AdminSessionError('VALIDATION_FAILED');
        await establish(await request('login', { username, password }, await csrf()));
      }); }
      catch (error) { forget(error instanceof AdminSessionError ? error.code : 'SERVICE_UNAVAILABLE'); throw error; }
    },
    logout: async () => {
      try { await ports.lock(async () => { await request('logout', undefined, await csrf()); }); forget(); }
      catch (error) { forget('LOGOUT_UNCONFIRMED'); throw error; }
    },
  };
}
export const adminSession = createAdminSession();
