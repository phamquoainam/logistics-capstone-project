import { test, expect } from '@playwright/test';
import { ApiError, request } from '../src/api';

const originalFetch = globalThis.fetch;
test.afterEach(() => { globalThis.fetch = originalFetch; });

test('authenticated mutations preserve method, body, token and idempotency key', async () => {
  globalThis.fetch = async (url, options) => {
    expect(url).toBe('/api/demo/v1/shipments');
    expect(options?.method).toBe('POST');
    expect(options?.body).toBe('{"reference":"test"}');
    expect(options?.headers).toEqual({
      'Content-Type': 'application/json',
      Authorization: 'Bearer synthetic-token',
      'Idempotency-Key': 'synthetic-operation',
    });
    return Response.json({ id: 'synthetic-id' });
  };
  await expect(request('/shipments', 'synthetic-token', {
    method: 'POST', body: '{"reference":"test"}',
    headers: { 'Idempotency-Key': 'synthetic-operation' },
  })).resolves.toEqual({ id: 'synthetic-id' });
});

test('anonymous reads do not manufacture authorization or content-type headers', async () => {
  globalThis.fetch = async (_url, options) => {
    expect(options?.headers).toEqual({});
    return Response.json({ healthy: true });
  };
  await expect(request('/health')).resolves.toEqual({ healthy: true });
});

test('HTTP conflict preserves backend status, code and message', async () => {
  globalThis.fetch = async () => Response.json({ code: 'IDEMPOTENCY_CONFLICT', message: 'Conflict' }, { status: 409 });
  await expect(request('/shipments')).rejects.toMatchObject({ status: 409, code: 'IDEMPOTENCY_CONFLICT', message: 'Conflict' });
});

test('network failure is a typed retryable error rather than success', async () => {
  globalThis.fetch = async () => { throw new TypeError('synthetic network failure'); };
  await expect(request('/shipments')).rejects.toBeInstanceOf(ApiError);
  await expect(request('/shipments')).rejects.toMatchObject({ status: 0, code: 'NETWORK_ERROR' });
});

test('non-JSON server failure retains HTTP status with a safe fallback', async () => {
  globalThis.fetch = async () => new Response('upstream unavailable', { status: 503 });
  await expect(request('/shipments')).rejects.toMatchObject({ status: 503, code: 'API_ERROR' });
});
