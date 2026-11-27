import { organizationCommandDigest } from '../src/admin/MerchantOrganizations';
import { test, expect } from '@playwright/test';
import { rejectionDigest } from '../src/admin/AdminRejection';
import { createAdminSession } from '../src/admin/session';

const id = '10000000-0000-4000-8000-000000000001';
const realm = '20000000-0000-4000-8000-000000000001';
const now = Date.parse('2026-11-21T00:00:00Z');
const account = { id, username: 'synthetic-admin', displayName: 'Quản trị thử nghiệm', email: 'admin@example.invalid', status: 'ACTIVE', grants: [{ role: 'IDENTITY_MANAGEMENT', scopeType: 'IDENTITY', scopeId: realm }], version: 1, emailVerified: true };
const session = () => ({ accessToken: 'a'.repeat(128), tokenType: 'Bearer', expiresIn: 900, sessionExpiresAt: new Date(now + 28800000).toISOString(), user: account });
function fixture(handle?: (path: string, init: RequestInit) => Response | Promise<Response>, protocol = 'https:') {
  const calls: { path: string; init: RequestInit }[] = [];
  const client = createAdminSession({ protocol: () => protocol, now: () => now, lock: work => work(), fetch: async (url, init = {}) => {
    const path = String(url); calls.push({ path, init });
    if (handle) return handle(path, init);
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(account);
    if (path.endsWith('/logout')) return new Response(null, { status: 204 });
    return Response.json(session());
  } });
  return { client, calls };
}
test('accepted SYSTEM transport, exact username, fresh /me and memory-only public state', async () => {
  const { client, calls } = fixture(); await client.login(' exact username ', 'synthetic-password');
  expect(client.getSnapshot()).toEqual({ status: 'authenticated', subject: { id, displayName: account.displayName, authorizationVersion: 1 }, error: null });
  const login = calls.find(c => c.path.endsWith('/login'))!;
  expect(JSON.parse(String(login.init.body))).toEqual({ username: ' exact username ', password: 'synthetic-password' });
  expect(login.init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-csrf-challenge-123456' });
  expect(calls.at(-1)?.path).toBe('/api/v1/auth/me');
  expect(calls.at(-1)?.init.headers).toMatchObject({ Authorization: 'Bearer ' + 'a'.repeat(128) });
  expect(JSON.stringify(client.getSnapshot())).not.toContain('accessToken');
  expect(JSON.stringify(client.getSnapshot())).not.toContain('admin@example.invalid');
  for (const c of calls) expect(c.init).toMatchObject({ credentials: 'same-origin', mode: 'same-origin', redirect: 'error', cache: 'no-store', referrerPolicy: 'no-referrer' });
  await client.logout(); expect(client.getSnapshot().status).toBe('guest');
  expect(calls.every(c => c.path.startsWith('/api/v1/auth/'))).toBe(true);
});
test('initialize is single-flight and refresh survives browser reload using only accepted cookies transport', async () => {
  const { client, calls } = fixture(); await Promise.all([client.initialize(), client.initialize()]);
  expect(calls.filter(c => c.path.endsWith('/refresh'))).toHaveLength(1);
  expect(client.getSnapshot().status).toBe('authenticated');
});
test('plain HTTP fails before credentials or session requests leave the browser', async () => {
  const { client, calls } = fixture(undefined, 'http:'); await client.initialize();
  expect(client.getSnapshot().error).toBe('HTTPS_REQUIRED');
  await expect(client.login('synthetic-admin', 'synthetic-password')).rejects.toMatchObject({ code: 'HTTPS_REQUIRED' });
  expect(calls).toHaveLength(0);
});
test('Customer, Staff, demo and mismatched current accounts cannot enter the SYSTEM UI', async () => {
  for (const value of [{ accountId: id, verifiedEmail: 'test@example.invalid' }, { principalType: 'STAFF', purpose: 'SUPPORT', accountId: id }, { ...account, grants: [{ role: 'ADMIN', scopeType: 'IDENTITY', scopeId: realm }] }, { ...account, id: realm }, { ...account, emailVerified: false }, { ...account, grants: [...account.grants, ...account.grants] }]) {
    const { client } = fixture(path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : path.endsWith('/me') ? Response.json(value) : Response.json(session()));
    await expect(client.login('synthetic-admin', 'synthetic-password')).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
    expect(client.getSnapshot().subject).toBeNull();
  }
});
test('malformed, expired, HTML and overlarge responses fail closed', async () => {
  for (const response of [() => Response.json(session(), { status: 201 }), () => Response.json({ ...session(), expiresIn: 0 }), () => Response.json({ ...session(), tokenType: 'Other' }), () => Response.json({ ...session(), sessionExpiresAt: '2026-11-20T00:00:00Z' }), () => new Response('<html>unavailable</html>', { headers: { 'Content-Type': 'text/html' } }), () => new Response('x'.repeat(65537))]) {
    const { client } = fixture(path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : response());
    await expect(client.login('synthetic-admin', 'synthetic-password')).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
    expect(client.getSnapshot().subject).toBeNull();
  }
});
test('lost login/refresh responses are never retried automatically', async () => {
  let writes = 0;
  const { client } = fixture(path => { if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }); writes++; throw Error('synthetic lost response'); });
  await expect(client.login('synthetic-admin', 'synthetic-password')).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' });
  expect(writes).toBe(1); expect(client.getSnapshot().subject).toBeNull();
});
test('rate limiting exposes only finite codes, not backend messages or passwords', async () => {
  const { client } = fixture(path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : Response.json({ code: 'RATE_LIMITED', message: 'sensitive diagnostic' }, { status: 429 }));
  await expect(client.login('synthetic-admin', 'synthetic-password')).rejects.toMatchObject({ code: 'RATE_LIMITED' });
  expect(JSON.stringify(client.getSnapshot())).not.toContain('sensitive diagnostic');
});
test('logout loss clears local authority but does not claim server-side revocation', async () => {
  const { client } = fixture(path => {
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(account);
    if (path.endsWith('/logout')) throw Error('synthetic lost response');
    return Response.json(session());
  });
  await client.login('synthetic-admin', 'synthetic-password');
  await expect(client.logout()).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' });
  expect(client.getSnapshot()).toEqual({ status: 'guest', subject: null, error: 'LOGOUT_UNCONFIRMED' });
});
test('current-session revocation clears the admitted screen on recheck', async () => {
  let revoked = false;
  const { client } = fixture(path => {
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return revoked ? Response.json({ code: 'SESSION_INVALID' }, { status: 401 }) : Response.json(account);
    return Response.json(session());
  });
  await client.login('synthetic-admin', 'synthetic-password'); revoked = true;
  await client.checkSession(); expect(client.getSnapshot()).toEqual({ status: 'guest', subject: null, error: 'SESSION_INVALID' });
});
test('missing browser lock never silently falls back to unsafe parallel refresh', async () => {
  let calls = 0;
  const client = createAdminSession({ protocol: () => 'https:', now: () => now, fetch: async () => { calls++; return Response.json(session()); }, lock: () => Promise.reject(Error('synthetic unavailable lock')) });
  await client.initialize(); expect(client.getSnapshot().status).toBe('guest'); expect(calls).toBe(0);
});
test('canonical Unicode username boundary is preserved; invalid inputs do not send credentials', async () => {
  const valid = fixture(); await valid.client.login('🚚'.repeat(80), 'Đ'.repeat(36));
  expect(JSON.parse(String(valid.calls.find(c => c.path.endsWith('/login'))?.init.body)).username).toBe('🚚'.repeat(80));
  for (const [username, password] of [[' ', 'synthetic-password'], ['🚚'.repeat(81), 'synthetic-password'], ['valid', 'Đ'.repeat(37)], ['valid', '\uD800'], ['\uD800', 'valid']]) {
    const f = fixture(); await expect(f.client.login(username, password)).rejects.toMatchObject({ code: 'VALIDATION_FAILED' }); expect(f.calls).toHaveLength(0);
  }
});


test('bounded governance routes reuse SYSTEM transport without arbitrary URL or actor authority', async () => {
  const { client, calls } = fixture(async path => {
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(account);
    if (path.startsWith('/api/v1/auth/')) return Response.json(session());
    return Response.json({ accepted: true });
  });
  await client.login('synthetic-admin', 'synthetic-password');
  await client.read('governance-context', new URLSearchParams(), v => v);
  await client.read('application-list', new URLSearchParams({ from: '2026-11-01', to: '2026-11-21' }), v => v);
  await client.read('application-detail', new URLSearchParams(), v => v, id);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/' + id);
  await expect(client.read('application-detail', new URLSearchParams(), v => v, '../users')).rejects.toMatchObject({ code: 'VALIDATION_FAILED' });
  const key = '30000000-0000-4000-8000-000000000001';
  const body = { action: 'BEGIN', revision: 1, policyVersion: '1', checklist: null, reason: 'Synthetic review' };
  await client.governanceWrite('review', id, 3, key, body, v => v);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/' + id + '/review');
  expect(calls.at(-1)?.init).toMatchObject({ method: 'POST', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer', headers: { 'Idempotency-Key': key, 'If-Match': '"v3"', Authorization: 'Bearer ' + 'a'.repeat(128) } });
  expect(JSON.parse(String(calls.at(-1)?.init.body))).toEqual(body);
  expect(JSON.stringify(client.getSnapshot())).not.toContain('accessToken');
});

test('review views keep sensitive search out of URL and reuse bounded authenticated transport', async () => {
  const { client, calls } = fixture(async path => {
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(account);
    if (path.startsWith('/api/v1/auth/')) return Response.json(session());
    return Response.json({ accepted: true });
  });await client.login('synthetic-admin', 'synthetic-password');
  const body = { legalIdentifier: 'SYNTHETIC-PRIVATE-123', purpose: 'MERCHANT_REVIEW' };
  await client.governanceView('application-search', body, v => v);expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/search');expect(calls.at(-1)?.init).toMatchObject({ method: 'POST', credentials: 'same-origin', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer', headers: { Authorization: 'Bearer ' + 'a'.repeat(128) } });expect(JSON.parse(String(calls.at(-1)?.init.body))).toEqual(body);
  await expect(client.governanceView('profile', {}, v => v, '../users')).rejects.toMatchObject({ code: 'VALIDATION_FAILED' });
  await client.governanceWrite('comment', id, 7, realm, { revision: 2, text: 'Synthetic note' }, v => v);expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/' + id + '/review-comments');expect(calls.at(-1)?.init.headers).toMatchObject({ 'Idempotency-Key': realm, 'If-Match': '"v7"' });
});

test('governance lost response is explicit and same-key retry preserves body and ETag', async () => {
  let writes = 0;
  const { client, calls } = fixture(async (path) => {
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(account);
    if (path.startsWith('/api/v1/auth/')) return Response.json(session());
    if (++writes === 1) throw new Error('SYNTHETIC_LOST_RESPONSE');
    return Response.json({ accepted: true });
  });
  await client.login('synthetic-admin', 'synthetic-password');
  const key = '30000000-0000-4000-8000-000000000002', body = { revision: 1, fieldCodes: ['registeredAddress'], reason: 'Synthetic request' };
  await expect(client.governanceWrite('information', id, 4, key, body, v => v)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' });
  expect(writes).toBe(1);
  await client.governanceWrite('information', id, 4, key, body, v => v);
  const sent = calls.filter(c => c.path.endsWith('/request-information'));
  expect(sent).toHaveLength(2);expect(sent[0].init.body).toBe(sent[1].init.body);expect(sent[0].init.headers).toEqual(sent[1].init.headers);
});

test('step-up and independent rejection use exact approved routes and a canonical credential-free digest', async () => {
  const { client, calls } = fixture(async path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : path.endsWith('/me') ? Response.json(account) : path.startsWith('/api/v1/auth/') ? Response.json(session()) : Response.json({ accepted: true }));
  await client.login('synthetic-admin', 'synthetic-password'); const key = '30000000-0000-4000-8000-000000000003';
  const input = { id, version: 10, revision: 2, policy: '7', reason: 'Synthetic independent reason' };
  const digest = await rejectionDigest(input); const canonical = JSON.stringify({ operation: 'REJECT', originalVersion: 10, policy: '7', reason: input.reason, revision: 2, target: id });
  expect(digest).toBe([...new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(canonical)))].map(b => b.toString(16).padStart(2, '0')).join(''));
  await client.governanceWrite('stepup', id, 10, key, { operation: 'REJECT', targetId: id, expectedVersion: 10, commandDigest: digest, password: 'synthetic-stepup-password' }, v => v);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/governance-reauthentications'); expect(calls.at(-1)?.init.headers).not.toHaveProperty('If-Match');
  await client.governanceWrite('rejection', id, 10, key, { revision: 2, policyVersion: '7', reauthenticationProofId: key, reason: input.reason }, v => v);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/' + id + '/rejection'); expect(calls.at(-1)?.init.headers).toMatchObject({ 'If-Match': '"v10"', 'Idempotency-Key': key });
  expect(String(calls.at(-1)?.init.body)).not.toContain('password'); expect(JSON.stringify(client.getSnapshot())).not.toContain('synthetic-stepup-password');
});

test('private evidence transport accepts only bounded attachment bytes through the current SYSTEM session', async () => {
  const headers = { 'Content-Type': 'image/png', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Content-Disposition': 'attachment; filename="merchant-evidence"' };
  const { client, calls } = fixture(async path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : path.endsWith('/me') ? Response.json(account) : path.startsWith('/api/v1/auth/') ? Response.json(session()) : new Response(new Uint8Array([1, 2, 3]), { headers }));
  await client.login('synthetic-admin', 'synthetic-password');const evidence = '30000000-0000-4000-8000-000000000004';const blob = await client.governanceEvidence(id, evidence);
  expect(blob.type).toBe('image/png');expect([...new Uint8Array(await blob.arrayBuffer())]).toEqual([1, 2, 3]);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-applications/' + id + '/evidence/' + evidence + '/content?purpose=MERCHANT_REVIEW');expect(calls.at(-1)?.init).toMatchObject({ method: 'GET', credentials: 'same-origin', cache: 'no-store', redirect: 'error', referrerPolicy: 'no-referrer' });
  await expect(client.governanceEvidence('../other', evidence)).rejects.toMatchObject({ code: 'VALIDATION_FAILED' });
});
test('private evidence rejects HTML, missing privacy headers and oversized bodies', async () => {
  for (const response of [() => new Response('synthetic', { headers: { 'Content-Type': 'text/html' } }), () => new Response('synthetic', { headers: { 'Content-Type': 'image/png' } }), () => new Response(new Uint8Array(5242881), { headers: { 'Content-Type': 'image/png', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Content-Disposition': 'attachment; filename="merchant-evidence"' } })]) {
    const { client } = fixture(async path => path.endsWith('/csrf') ? Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' }) : path.endsWith('/me') ? Response.json(account) : path.startsWith('/api/v1/auth/') ? Response.json(session()) : response());
    await client.login('synthetic-admin', 'synthetic-password');await expect(client.governanceEvidence(id, realm)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
  }
});

test('approval digest is operation-bound and excludes password and proof', async () => {
  const input = { id: '40000000-0000-4000-8000-000000000001', version: 10, revision: 2, policy: '7', reason: 'Synthetic approval' };
  const approved = await rejectionDigest({ ...input, operation: 'APPROVE' });
  expect(approved).not.toBe(await rejectionDigest(input));
  expect(approved).toMatch(/^[a-f0-9]{64}$/);
});

test('organization lifecycle digest cannot be reused for the inverse operation', async () => {
  const id = '40000000-0000-4000-8000-000000000001';
  expect(await organizationCommandDigest(id, 1, 'SUSPEND', 'Synthetic reason')).not.toBe(await organizationCommandDigest(id, 1, 'REINSTATE', 'Synthetic reason'));
});
