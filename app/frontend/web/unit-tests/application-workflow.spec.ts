import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
const account = '10000000-0000-4000-8000-000000000001', app = '10000000-0000-4000-8000-000000000002', key = '10000000-0000-4000-8000-000000000003';
const now = Date.parse('2026-11-24T00:00:00Z');
const requirements = { applicationId: app, policyId: key, policyVersion: '7', consentVersion: 'SYNTHETIC-7', consentStatements: { accuracy: 'Synthetic accuracy', representation: 'Synthetic representation' }, requiredEvidence: ['REGISTRATION', 'REPRESENTATIVE_AUTHORIZATION'] };
function fixture(reply: (path: string, init: RequestInit) => Response | Promise<Response>) {
  const calls: { path: string; init: RequestInit }[] = [];
  const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async (url, init) => {
    const path = String(url); calls.push({ path, init: init ?? {} });
    if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return Response.json({ accessToken: 'synthetic-access', accountId: account, expiresAt: new Date(now + 900000).toISOString() });
    if (path.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    return reply(path, init ?? {});
  } });
  return { client, calls };
}
test('requirements preserve exact published version and self target without mutation', async () => {
  const f = fixture(() => Response.json(requirements, { headers: { 'Cache-Control': 'no-store' } })); await f.client.initialize();
  expect(await f.client.applicationRequirements(app)).toEqual(requirements);
  const call = f.calls.at(-1)!; expect(call.path).toBe('/api/v1/merchant-applications/' + app + '/requirements'); expect(call.init.method).toBe('GET'); expect(call.init.body).toBeUndefined(); expect(call.init.headers).toEqual({ Authorization: 'Bearer synthetic-access' });
});
test('foreign, incomplete, malformed or unpublished attestation data fails closed', async () => {
  for (const value of [ { ...requirements, applicationId: account }, { ...requirements, policyVersion: '0' }, { ...requirements, policyVersion: '9223372036854775808' }, { ...requirements, consentStatements: { accuracy: '', representation: 'Synthetic' } }, { ...requirements, consentStatements: { accuracy: 'e\u0301', representation: 'Synthetic' } }, { ...requirements, requiredEvidence: ['REGISTRATION', 'REGISTRATION'] }, { ...requirements, extra: true } ]) {
    const f = fixture(() => Response.json(value, { headers: { 'Cache-Control': 'no-store' } })); await f.client.initialize(); await expect(f.client.applicationRequirements(app)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
  }
});
test('multipart uses existing admission and browser boundary; unknown upload retries the same file and key explicitly', async () => {
  let writes = 0;
  const evidence = { id: key, applicationId: app, purpose: 'REGISTRATION', state: 'CLEAN', mime: 'image/png', bytes: 3, sha256: 'a'.repeat(64), version: 1, screenedAt: new Date(now).toISOString() };
  const f = fixture(async (_path, init) => { if (++writes === 1) throw new TypeError('Synthetic lost response'); expect(init.body).toBeInstanceOf(FormData); return Response.json(evidence, { status: 201, headers: { 'Cache-Control': 'no-store', ETag: '"v1"' } }); });
  await f.client.initialize(); const file = new File(['png'], 'synthetic.png', { type: 'image/png' });
  await expect(f.client.uploadApplicationEvidence(app, 'REGISTRATION', file, key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect(writes).toBe(1);
  expect(await f.client.uploadApplicationEvidence(app, 'REGISTRATION', file, key)).toEqual(evidence);
  const calls = f.calls.filter(c => c.path.endsWith('/evidence')); expect(calls).toHaveLength(2);
  for (const c of calls) { expect(c.init.headers).toEqual({ Authorization: 'Bearer synthetic-access', 'X-CSRF-Token': 'synthetic-csrf', 'Idempotency-Key': key }); expect((c.init.body as FormData).get('purpose')).toBe('REGISTRATION'); expect(c.init.credentials).toBe('same-origin'); }
});
test('submission retry preserves body, stale ETag and key; does not accept receipt as current entity', async () => {
  let writes = 0; const result = { id: app, applicantAccountId: account, state: 'SUBMITTED', revision: 1, version: 4, draft: {} };
  const f = fixture(() => { if (++writes === 1) throw new TypeError('Synthetic lost response'); return Response.json(result, { headers: { 'Cache-Control': 'no-store', ETag: '"v4"' } }); });
  await f.client.initialize(); await expect(f.client.applicationCommand(app, 'submit', 'Synthetic display', '"v3"', key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect(writes).toBe(1);
  expect((await f.client.applicationCommand(app, 'submit', 'Synthetic display', '"v3"', key)).application.version).toBe(4);
  const calls = f.calls.filter(c => c.path.endsWith('/submit')); expect(calls).toHaveLength(2); expect(calls[0].init.body).toBe(calls[1].init.body); expect(calls[0].init.headers).toEqual(calls[1].init.headers); expect(JSON.parse(calls[0].init.body as string)).toEqual({ organizationDisplayName: 'Synthetic display' });
});
