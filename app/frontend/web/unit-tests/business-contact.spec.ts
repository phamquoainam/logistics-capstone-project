import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
import { captureBusinessContactProof, hasBusinessContactProof, withBusinessContactProof, clearBusinessContactProof } from '../src/customer/business-contact-proof';

const account = '10000000-0000-4000-8000-000000000001', app = '10000000-0000-4000-8000-000000000002', proof = '10000000-0000-4000-8000-000000000003', key = '10000000-0000-4000-8000-000000000004';
const secret = 'X'.repeat(43), now = Date.parse('2026-11-24T00:00:00Z');
test.afterEach(clearBusinessContactProof);
test('captures a contact proof before rendering, strips URL, and clears explicitly', () => {
  const writes: string[] = [];
  captureBusinessContactProof({ pathname: '/personal/profile', search: '', hash: '#applicationId=' + app + '&proofId=' + proof + '&proof=' + secret }, { replaceState: (_data, _unused, url) => { writes.push(String(url)); } });
  expect(writes).toEqual(['/personal/profile']); expect(hasBusinessContactProof()).toBe(true);
  expect(withBusinessContactProof(p => [p.applicationId, p.proofId, p.proof])).toEqual([app, proof, secret]);
  clearBusinessContactProof(); expect(hasBusinessContactProof()).toBe(false); expect(() => withBusinessContactProof(() => undefined)).toThrow('BUSINESS_CONTACT_PROOF_UNAVAILABLE');
});
test('query secrets, duplicates, zero UUID and malformed fragments never become a proof', () => {
  for (const [search, hash] of [
    ['?proof=' + secret, '#applicationId=' + app + '&proofId=' + proof + '&proof=' + secret],
    ['', '#applicationId=' + app + '&proofId=' + proof + '&proof=' + secret + '&proof=' + secret],
    ['', '#applicationId=00000000-0000-0000-0000-000000000000&proofId=' + proof + '&proof=' + secret],
    ['', '#applicationId=' + app + '&proofId=' + proof + '&proof=invalid'],
  ]) { captureBusinessContactProof({ pathname: '/personal/profile', search, hash }, { replaceState: () => undefined }); expect(hasBusinessContactProof()).toBe(false); }
});
function fixture(reply: (path: string, init: RequestInit) => Response) {
  const calls: { path: string; init: RequestInit }[] = [];
  const fetcher: typeof fetch = async (path, init) => {
    const route = String(path); calls.push({ path: route, init: init ?? {} });
    if (route.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' });
    if (route.endsWith('/refresh')) return Response.json({ accessToken: 'synthetic-access', accountId: account, expiresAt: new Date(now + 900000).toISOString() });
    if (route.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    return reply(route, init ?? {});
  };
  return { client: createCustomerClient({ fetch: fetcher, lock: work => work(), now: () => now }), calls };
}
test('contact commands use same-origin bearer/CSRF and exact bodies, never URL proofs', async () => {
  const f = fixture(path => path.endsWith('/business-contact-proofs')
    ? Response.json({ requestId: proof, accepted: true }, { status: 202, headers: { 'Cache-Control': 'no-store' } })
    : Response.json({ id: key, applicationId: app, verifiedAt: new Date(now).toISOString(), expiresAt: new Date(now + 900000).toISOString() }, { headers: { 'Cache-Control': 'no-store' } }));
  await f.client.initialize(); await f.client.requestBusinessContact(app, 'contact@example.invalid', key); await f.client.confirmBusinessContact(app, proof, secret, key);
  const writes = f.calls.filter(c => c.path.includes('/business-contact-'));
  expect(writes).toHaveLength(2); expect(writes[1].path).toBe('/api/v1/customer/business-contact-confirmations');
  expect(writes[1].init.headers).toMatchObject({ Authorization: 'Bearer synthetic-access', 'X-CSRF-Token': 'synthetic-csrf', 'Idempotency-Key': key });
  expect(JSON.parse(String(writes[1].init.body))).toEqual({ applicationId: app, proofId: proof, proof: secret }); expect(writes[1].init.referrerPolicy).toBe('no-referrer');
  expect(writes.every(c => !c.path.includes(secret) && c.init.credentials === 'same-origin')).toBe(true);
});
test('a foreign confirmation or response extra fields cannot be saved as a verified fact', async () => {
  for (const value of [
    { id: key, applicationId: account, verifiedAt: new Date(now).toISOString(), expiresAt: new Date(now + 900000).toISOString() },
    { id: key, applicationId: app, verifiedAt: new Date(now).toISOString(), expiresAt: new Date(now + 900000).toISOString(), email: 'private@example.invalid' },
  ]) { const f = fixture(() => Response.json(value, { headers: { 'Cache-Control': 'no-store' } })); await f.client.initialize(); await expect(f.client.confirmBusinessContact(app, proof, secret, key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); }
});
test('the exact six-field approved Application shape does not imply Organization membership', async () => {
  const f = fixture(() => Response.json({ id: app, applicantAccountId: account, state: 'APPROVED', revision: 1, version: 4, draft: { registeredName: 'Synthetic' } }, { headers: { ETag: '"v4"' } }));
  await f.client.initialize(); const result = await f.client.readApplication(app); expect(result.application.state).toBe('APPROVED'); expect(result.application.organizationId).toBeUndefined();
});
