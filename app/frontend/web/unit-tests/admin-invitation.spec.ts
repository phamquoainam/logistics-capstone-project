import { test, expect } from '@playwright/test';
import { acceptAdminInvitation, AdminInvitationError, captureAdminInvitation, clearAdminInvitation, hasAdminInvitation } from '../src/admin/invitation';
const id = '10000000-0000-4000-8000-000000000001', token = 'A'.repeat(43);
function capture(hash = '#proofId=' + id + '&token=' + token, search = '') {
  const replaced: unknown[][] = [];
  captureAdminInvitation({ pathname: '/admin/activate', hash, search }, { replaceState: (...values) => { replaced.push(values); } });
  expect(replaced).toEqual([[null, '', '/admin/activate']]);
}
test.afterEach(clearAdminInvitation);
test('live Location query is checked before replaceState removes it', () => {
  let search = '?proofId=forbidden';
  const location = { pathname: '/admin/activate', hash: '#proofId=' + id + '&token=' + token, get search() { return search; } };
  captureAdminInvitation(location, { replaceState: () => { search = ''; } });
  expect(hasAdminInvitation()).toBe(false);
});
test('entry fragment is scrubbed synchronously and rejects duplicate, query or noncanonical proof', () => {
  capture(); expect(hasAdminInvitation()).toBe(true);
  for (const fragment of ['#proofId=' + id + '&token=' + token + '&token=' + token, '#proofId=' + id + '&token=' + 'a'.repeat(43), '#proofId=00000000-0000-0000-0000-000000000000&token=' + token]) { capture(fragment); expect(hasAdminInvitation()).toBe(false); }
  capture(undefined, '?token=forbidden'); expect(hasAdminInvitation()).toBe(false);
});
test('acceptance uses SYSTEM CSRF and the fixed endpoint without bearer or automatic session', async () => {
  capture(); const calls: { path: string; options?: RequestInit }[] = [];
  const send: typeof fetch = async (input, options) => {
    calls.push({ path: String(input), options });
    return calls.length === 1 ? new Response(JSON.stringify({ csrfToken: 'synthetic-admin-csrf-challenge' }), { status: 200, headers: { 'Content-Type': 'application/json' } }) : new Response(null, { status: 204 });
  };
  await acceptAdminInvitation('synthetic password for test', id, send, 'https:');
  expect(calls.map(c => c.path)).toEqual(['/api/v1/auth/csrf', '/api/v1/auth/admin-invitations/accept']);
  expect(calls[1].options?.headers).toEqual({ 'Content-Type': 'application/json', 'X-CSRF-Token': 'synthetic-admin-csrf-challenge', 'Idempotency-Key': id });
  expect(calls.every(c => c.options?.credentials === 'same-origin' && c.options?.referrerPolicy === 'no-referrer' && c.options?.redirect === 'error')).toBe(true);
  expect(hasAdminInvitation()).toBe(false);
});
test('password policy rejects before network and unknown write outcome never auto retries', async () => {
  capture(); let calls = 0;
  const send: typeof fetch = async () => { calls++; if (calls === 1) return new Response(JSON.stringify({ csrfToken: 'synthetic-admin-csrf-challenge' }), { headers: { 'Content-Type': 'application/json' } }); throw new Error('private transport details'); };
  await expect(acceptAdminInvitation('short', id, send, 'https:')).rejects.toEqual(new AdminInvitationError('PASSWORD_INVALID')); expect(calls).toBe(0);
  await expect(acceptAdminInvitation('synthetic password for test', id, send, 'https:')).rejects.toEqual(new AdminInvitationError('OUTCOME_UNKNOWN')); expect(calls).toBe(2); expect(hasAdminInvitation()).toBe(true);
});
