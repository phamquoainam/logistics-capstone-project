import { test, expect } from '@playwright/test';
import { ApiError, createDemoApi } from '../src/api';

const baseUrl = 'http://web-test.invalid/api/demo/v1';
const api = createDemoApi(baseUrl);
const draft = { reference: 'test', pickupAddress: 'Demo pickup', recipientName: 'Demo', recipientPhone: '0000000000', deliveryStreet: 'Demo street', deliveryWard: 'Demo ward', deliveryProvince: 'Demo province', parcelDescription: 'Demo parcel', weightKg: 1.25 };
function requestOf(input: RequestInfo | URL) {
  expect(input).toBeInstanceOf(Request);
  if (!(input instanceof Request)) throw new Error('Expected generated Fetch Request');
  return input;
}

const originalFetch = globalThis.fetch;
test.afterEach(() => { globalThis.fetch = originalFetch; });

test('authenticated mutations preserve method, body, token and idempotency key', async () => {
  globalThis.fetch = async input => {
    const request = requestOf(input);
    expect(request.url).toBe(`${baseUrl}/shipments`);
    expect(request.method).toBe('POST');
    expect(await request.json()).toEqual(draft);
    expect(Object.fromEntries(request.headers)).toEqual({
      'content-type': 'application/json',
      authorization: 'Bearer synthetic-token',
      'idempotency-key': 'synthetic-operation',
    });
    return Response.json({ id: 'synthetic-id' });
  };
  await expect(api.createShipment('synthetic-token', draft, 'synthetic-operation')).resolves.toEqual({ id: 'synthetic-id' });
});

test('anonymous reads do not manufacture authorization or content-type headers', async () => {
  globalThis.fetch = async input => {
    expect(Object.fromEntries(requestOf(input).headers)).toEqual({});
    return Response.json({ healthy: true });
  };
  await expect(api.health()).resolves.toEqual({ healthy: true });
});

test('HTTP conflict preserves backend status, code and message', async () => {
  globalThis.fetch = async () => Response.json({ code: 'IDEMPOTENCY_CONFLICT', message: 'Conflict' }, { status: 409 });
  await expect(api.createShipment('synthetic-token', draft, 'same-key')).rejects.toMatchObject({ status: 409, code: 'IDEMPOTENCY_CONFLICT', message: 'Conflict' });
});

test('network failure is a typed retryable error rather than success', async () => {
  globalThis.fetch = async () => { throw new TypeError('synthetic network failure'); };
  await expect(api.shipments()).rejects.toBeInstanceOf(ApiError);
  await expect(api.shipments()).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
});

test('non-JSON server failure retains HTTP status with a safe fallback', async () => {
  globalThis.fetch = async () => new Response('upstream unavailable', { status: 503 });
  await expect(api.shipments()).rejects.toMatchObject({ status: 503, code: 'API_ERROR', message: 'Thao tác chưa hoàn tất. Kiểm tra backend và thử lại.' });
});

for (const [status, code] of [[401, 'INVALID_TOKEN'], [403, 'FORBIDDEN'], [404, 'SHIPMENT_NOT_FOUND'], [409, 'IDEMPOTENCY_CONFLICT'], [429, 'LOGIN_RATE_LIMIT']] as const) {
  test(`adapter retains ${status}/${code} without treating authorization as authentication`, async () => {
    globalThis.fetch = async () => Response.json({ code, message: 'Synthetic backend message' }, { status });
    await expect(api.currentUser('synthetic-token')).rejects.toMatchObject({ status, code, message: 'Synthetic backend message' });
  });
}
test('malformed JSON responses and empty error body use safe fallback, never raw upstream text', async () => {
  for (const text of ['{broken', '']) {
    globalThis.fetch = async () => new Response(text, { status: 502, headers: { 'Content-Type': 'application/json' } });
    await expect(api.shipments()).rejects.toMatchObject({ status: 502, code: 'API_ERROR', message: 'Thao tác chưa hoàn tất. Kiểm tra backend và thử lại.' });
  }
  globalThis.fetch = async () => new Response('synthetic private upstream detail', { status: 200, headers: { 'Content-Type': 'application/json' } });
  await expect(api.shipments()).rejects.toMatchObject({ status: 200, code: 'API_ERROR', message: 'Thao tác chưa hoàn tất. Kiểm tra backend và thử lại.' });
});
test('current token is per request and never retained across users or anonymous calls', async () => {
  const authorization: (string | null)[] = [];
  globalThis.fetch = async input => { authorization.push(requestOf(input).headers.get('Authorization')); return Response.json({}); };
  await api.currentUser('synthetic-a'); await api.currentUser('synthetic-b'); await api.currentUser(); await api.health(); await api.login({ username: 'synthetic', password: 'synthetic-only' });
  expect(authorization).toEqual(['Bearer synthetic-a', 'Bearer synthetic-b', null, null, null]);
});
test('transport does not retry or rotate keys; caller retry reuses exact raw body and key', async () => {
  const seen: { key: string | null; body: string }[] = [];
  globalThis.fetch = async input => { const request = requestOf(input); seen.push({ key: request.headers.get('Idempotency-Key'), body: await request.text() }); throw new TypeError('synthetic network failure'); };
  const body = { ...draft, reference: '  padded  ' };
  await expect(api.createShipment('synthetic', body, 'retained-key')).rejects.toMatchObject({ status: 0 });
  expect(seen).toHaveLength(1);
  await expect(api.createShipment('synthetic', body, 'retained-key')).rejects.toMatchObject({ status: 0 });
  expect(seen).toEqual([{ key: 'retained-key', body: JSON.stringify(body) }, { key: 'retained-key', body: JSON.stringify(body) }]);
});
const operations = [
  ['health', 'GET', '/health', () => api.health(), false],
  ['login', 'POST', '/auth/login', () => api.login({ username: 'synthetic', password: 'synthetic-only' }), false],
  ['currentUser', 'GET', '/auth/me', () => api.currentUser('synthetic'), true],
  ['merchant', 'GET', '/merchant/me', () => api.merchant('synthetic'), true],
  ['shipments', 'GET', '/shipments', () => api.shipments('synthetic'), true],
  ['createShipment', 'POST', '/shipments', () => api.createShipment('synthetic', draft, 'create-key'), true],
  ['shipment', 'GET', '/shipments/synthetic-id', () => api.shipment('synthetic', 'synthetic-id'), true],
  ['cancelShipment', 'POST', '/shipments/synthetic-id/cancel', () => api.cancelShipment('synthetic', 'synthetic-id', { reason: '  preserve raw reason  ' }, 'cancel-key'), true],
  ['users', 'GET', '/admin/users', () => api.users('synthetic'), true],
  ['overview', 'GET', '/admin/overview', () => api.overview('synthetic'), true],
  ['summaries', 'GET', '/admin/shipments', () => api.summaries('synthetic'), true],
] as const;
for (const [name, method, path, call, protectedCall] of operations) {
  test(`public SDK mapping: ${name}`, async () => {
    globalThis.fetch = async input => {
      const request = requestOf(input);
      expect(request.url).toBe(baseUrl + path); expect(request.method).toBe(method);
      expect(request.headers.get('Authorization')).toBe(protectedCall ? 'Bearer synthetic' : null);
      if (name === 'login') expect(await request.json()).toEqual({ username: 'synthetic', password: 'synthetic-only' });
      if (name === 'cancelShipment') { expect(request.headers.get('Idempotency-Key')).toBe('cancel-key'); expect(await request.json()).toEqual({ reason: '  preserve raw reason  ' }); }
      return Response.json({});
    };
    await call();
  });
}
