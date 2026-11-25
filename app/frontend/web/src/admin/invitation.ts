type Proof = { proofId: string; token: string };
let proof: Proof | undefined;
/** Synchronous entry capture, before session restoration. No router/browser storage receives secrets. */
export function captureAdminInvitation(location: Pick<Location, 'pathname' | 'hash' | 'search'>, history: Pick<History, 'replaceState'>) {
  if (location.pathname !== '/admin/activate') return;
  const fragment = location.hash, hasQuery = location.search !== '';
  history.replaceState(null, '', '/admin/activate');
  proof = undefined;
  if (hasQuery) return;
  const values = new URLSearchParams(fragment.replace(/^#/, ''));
  const id = values.get('proofId'), token = values.get('token');
  if ([...values.keys()].length !== 2 || values.getAll('proofId').length !== 1 || values.getAll('token').length !== 1 || !id || !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(id) || id === '00000000-0000-0000-0000-000000000000' || !token || !/^[A-Za-z0-9_-]{43}$/.test(token)) return;
  try { const bytes = atob(token.replace(/-/g, '+').replace(/_/g, '/') + '='); if (bytes.length !== 32 || btoa(bytes).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '') !== token) return; } catch { return; }
  proof = { proofId: id, token };
}
export const hasAdminInvitation = () => proof !== undefined;
export const clearAdminInvitation = () => { proof = undefined; };
export class AdminInvitationError extends Error { constructor(readonly code: string) { super(code); this.name = 'AdminInvitationError'; } }
export async function acceptAdminInvitation(password: string, key: string, send: typeof fetch = fetch, protocol = location.protocol) {
  if (protocol !== 'https:' || !proof) throw new AdminInvitationError('INVITATION_UNAVAILABLE');
  if ([...password].length < 15 || [...password].length > 72 || new TextEncoder().encode(password).length > 72 || /[\uD800-\uDFFF]/u.test(password)) throw new AdminInvitationError('PASSWORD_INVALID');
  const options: RequestInit = { credentials: 'same-origin', mode: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer', signal: AbortSignal.timeout(15000) };
  let response: Response;
  try {
    const csrf = await send('/api/v1/auth/csrf', { ...options, method: 'GET' });
    if (!csrf.ok || csrf.status !== 200 || !csrf.headers.get('Content-Type')?.startsWith('application/json')) throw new AdminInvitationError('ACCESS_DENIED');
    const text = await csrf.text(); if (text.length > 8192) throw new AdminInvitationError('ACCESS_DENIED');
    const data: unknown = JSON.parse(text);
    if (!data || typeof data !== 'object' || Object.keys(data).length !== 1 || !('csrfToken' in data) || typeof data.csrfToken !== 'string' || data.csrfToken.length < 16 || data.csrfToken.length > 4096) throw new AdminInvitationError('ACCESS_DENIED');
    response = await send('/api/v1/auth/admin-invitations/accept', { ...options, method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': data.csrfToken, 'Idempotency-Key': key }, body: JSON.stringify({ ...proof, password }) });
  } catch (error) { if (error instanceof AdminInvitationError) throw error; throw new AdminInvitationError('OUTCOME_UNKNOWN'); }
  if (response.status !== 204) throw new AdminInvitationError(response.status === 429 ? 'RATE_LIMITED' : response.status === 409 ? 'IDEMPOTENCY_CONFLICT' : response.status === 400 ? 'TOKEN_INVALID_OR_EXPIRED' : response.status === 403 ? 'ACCESS_DENIED' : 'SERVICE_UNAVAILABLE');
  if (await response.text() !== '') throw new AdminInvitationError('OUTCOME_UNKNOWN');
  clearAdminInvitation();
}
