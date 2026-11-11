import { expect, test } from '@playwright/test';
import { parseBusinessContexts, parseBusinessReport } from '../src/customer/business-data';
import { businessContextsFixture, businessReportFixture } from '../ui-unit-tests/support/business-fixture';
import { createCustomerClient } from '../src/customer/client';

test('Business contexts use explicit distinct organization grants, never role-label authority', () => {
  expect(parseBusinessContexts({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: businessContextsFixture })).toHaveLength(2);
  for (const items of [[businessContextsFixture[0], businessContextsFixture[0]], [{ ...businessContextsFixture[0], permissions: [] }], [{ ...businessContextsFixture[0], permissions: ['dashboard.read', '*'] }], [{ ...businessContextsFixture[0], accountId: 'foreign' }]]) expect(() => parseBusinessContexts({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items })).toThrow();
});
test('missing Business facts remain null and empty charts, not fake observed zero', () => {
  const source = businessReportFixture(), result = parseBusinessReport(source, businessContextsFixture[0], new URLSearchParams());
  expect(result.metrics.every(m => m.value === null && m.points.length === 0)).toBe(true);
  for (const alter of [(v: typeof source) => { v.metrics[0].value = '0' as never; }, (v: typeof source) => { v.charts[0].series = [1]; }, (v: typeof source) => { v.recentShipments.items = [{ ownerId: 'personal' }]; }, (v: typeof source) => { v.quickActions[0].state = 'AVAILABLE'; }]) { const v = structuredClone(source); alter(v); expect(() => parseBusinessReport(v, businessContextsFixture[0], new URLSearchParams())).toThrow(); }
});
test('Business report rejects wrong organization, membership, stale authority and financial overdisclosure', () => {
  const source = businessReportFixture();
  for (const context of [{ ...businessContextsFixture[0], organizationId: businessContextsFixture[1].organizationId }, { ...businessContextsFixture[0], membershipId: businessContextsFixture[1].membershipId }, { ...businessContextsFixture[0], authorityVersion: 2 }, { ...businessContextsFixture[0], permissions: ['dashboard.read'] as const }]) expect(() => parseBusinessReport(source, context as typeof businessContextsFixture[0], new URLSearchParams())).toThrow();
  expect(parseBusinessReport(businessReportFixture(businessContextsFixture[1]), businessContextsFixture[1], new URLSearchParams()).metrics.some(m => m.unit === 'VND')).toBe(false);
});
test('Business dates bind Vietnam inclusive days and exact requested interval; response extras deny', () => {
  const query = new URLSearchParams({ from: '2026-11-01', to: '2026-11-10', interval: 'WEEK' }), source = businessReportFixture(businessContextsFixture[0], query);
  expect(parseBusinessReport(source, businessContextsFixture[0], query).period.startInclusive).toBe('2026-10-31T17:00:00.000Z');
  expect(() => parseBusinessReport({ ...source, period: { ...source.period, interval: 'DAY' } }, businessContextsFixture[0], query)).toThrow();
  expect(() => parseBusinessReport({ ...source, personalOrders: [] }, businessContextsFixture[0], query)).toThrow();
  expect(() => parseBusinessReport({ ...source, period: { ...source.period, startInclusive: '2026-11-01T00:00:00Z' } }, businessContextsFixture[0], query)).toThrow();
});
test('Business authenticated transport rejects late responses after logout', async () => {
  const now = Date.now(), owner = '10000000-0000-4000-8000-000000000001';
  let release!: (v: Response) => void, requested = false; const pending = new Promise<Response>(resolve => { release = resolve; });
  const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async (input, init = {}) => {
    const path = String(input); if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-business-csrf' });
    if (path.endsWith('/refresh')) return Response.json({ accountId: owner, accessToken: 'synthetic-business-access', expiresAt: new Date(now + 900000).toISOString() });
    if (path.endsWith('/me')) return Response.json({ accountId: owner, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/logout')) return new Response(null, { status: 204 });
    expect(init.headers).toMatchObject({ Authorization: 'Bearer synthetic-business-access' }); requested = true; return pending;
  } });
  try { await client.initialize(); const reading = client.businessContexts(), rejected = expect(reading).rejects.toThrow('SESSION_CHANGED'); await expect.poll(() => requested).toBe(true); await client.logout(); release(Response.json({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: businessContextsFixture }, { headers: { 'Cache-Control': 'no-store' } })); await rejected; } finally { client.dispose(); }
});
test('Business authenticated transport refuses cacheable responses', async () => {
  const now = Date.now(), owner = '10000000-0000-4000-8000-000000000001';
  const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async input => {
    const path = String(input); if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return Response.json({ accountId: owner, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() });
    if (path.endsWith('/me')) return Response.json({ accountId: owner, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    return Response.json({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: businessContextsFixture });
  } });
  try { await client.initialize(); await expect(client.businessContexts()).rejects.toThrow(); } finally { client.dispose(); }
});
