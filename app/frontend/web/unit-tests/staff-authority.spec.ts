import { test, expect } from '@playwright/test';
import { captureStaffInvitation, clearStaffInvitation, hasStaffInvitation, withStaffInvitation } from '../src/customer/staff-invitation-proof';
import { createStaffClient } from '../src/customer/staff-client';

const account = '10000000-0000-4000-8000-000000000001', key = '20000000-0000-4000-8000-000000000001';
test.afterEach(clearStaffInvitation);
test('Staff invitation is scrubbed immediately and rejects duplicates and query-secret inputs', () => {
  const seen: unknown[][] = [], history = { replaceState: (...values: unknown[]) => { seen.push(values); } };
  captureStaffInvitation({ pathname: '/staff/activate', search: '', hash: '#proofId=' + account + '&token=' + 'a'.repeat(43) }, history);
  expect(seen[0]).toEqual([null, '', '/staff/activate']); expect(hasStaffInvitation()).toBe(true);
  expect(withStaffInvitation((proof, token) => [proof, token.length])).toEqual([account, 43]);
  for (const hash of ['#proofId=' + account + '&token=' + 'a'.repeat(43) + '&token=' + 'b'.repeat(43), '#token=short', '#proofId=' + account + '&token=' + 'a'.repeat(43) + '&role=OWNER']) {
    captureStaffInvitation({ pathname: '/staff/activate', search: '', hash }, history); expect(hasStaffInvitation()).toBe(false);
  }
  captureStaffInvitation({ pathname: '/staff/activate', search: '?token=forged', hash: '#proofId=' + account + '&token=' + 'a'.repeat(43) }, history); expect(hasStaffInvitation()).toBe(false);
});
function fixture(handler?: (path: string, init: RequestInit) => Response) {
  const calls: { path: string; init: RequestInit }[] = [], now = Date.parse('2026-11-19T00:00:00Z');
  const transport: typeof fetch = async (url, init = {}) => {
    const path = String(url); calls.push({ path, init }); if (handler) return handler(path, init);
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-staff-challenge-123456' });
    if (path.endsWith('/login') || path.endsWith('/refresh')) return Response.json({ accessToken: 's'.repeat(128), expiresAt: new Date(now + 900000).toISOString(), accountId: account });
    if (path.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic Staff', principalType: 'STAFF', purpose: 'SUPPORT', environment: 'TEST' });
    return new Response(null, { status: 204 });
  };
  return { calls, client: createStaffClient({ fetch: transport, lock: work => work(), now: () => now }) };
}
test('Staff browser session uses only Staff routes and fixed private transport options', async () => {
  const { client, calls } = fixture(); await client.initialize(); expect(client.getSnapshot().subject?.principalType).toBe('STAFF');
  await client.login('staff@example.invalid', 'Synthetic long password!9'); await client.logout();
  expect(client.getSnapshot().status).toBe('guest');
  expect(calls.every(c => c.path.startsWith('/api/v1/staff-auth/'))).toBe(true);
  for (const call of calls) expect(call.init).toMatchObject({ credentials: 'same-origin', redirect: 'error', referrerPolicy: 'no-referrer', cache: 'no-store', mode: 'same-origin' });
  expect(calls.find(c => c.path.endsWith('/login'))?.init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-staff-challenge-123456' });
});
test('Customer-shaped principal cannot enter Staff memory', async () => {
  const f = fixture(path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-staff-challenge-123456' }) : path.endsWith('/refresh') ? Response.json({ accessToken: 's'.repeat(128), expiresAt: '2026-11-19T00:15:00Z', accountId: account }) : Response.json({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'customer@example.invalid', securityVersion: 1 }));
  await f.client.initialize(); expect(f.client.getSnapshot().status).toBe('guest'); expect(f.client.getSnapshot().subject).toBe(null);
});
test('activation preserves exact key on explicit retry and never retries a network loss automatically', async () => {
  captureStaffInvitation({ pathname: '/staff/activate', search: '', hash: '#proofId=' + account + '&token=' + 'a'.repeat(43) }, { replaceState: () => {} });
  let posts = 0;
  const f = fixture(path => { if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-staff-challenge-123456' }); posts++; throw new Error('Synthetic network loss'); });
  await expect(f.client.accept('Synthetic long password!9', key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect(posts).toBe(1); expect(hasStaffInvitation()).toBe(true);
  await expect(f.client.accept('Synthetic long password!9', key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect(posts).toBe(2);
  const writes = f.calls.filter(c => c.path.endsWith('/accept')); expect(writes[0].init.body).toBe(writes[1].init.body); expect(writes[0].init.headers).toEqual(writes[1].init.headers);
});
