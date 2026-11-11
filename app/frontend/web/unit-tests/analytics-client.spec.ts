import { test, expect } from '@playwright/test';
import { parseAnalytics } from '../src/customer/analytics-data';
import { createCustomerClient } from '../src/customer/client';
import { analyticsFixture } from '../ui-unit-tests/support/analytics-fixture';

test('analytics decoding preserves zero and missing comparison instead of fabricating a trend', () => {
  const r = parseAnalytics(analyticsFixture()); expect(r.metrics.every(m => m.value === '0' && m.changePercent === null)).toBe(true); expect(r.shipments.items).toHaveLength(0);
});
test('analytics rejects Business context PII fields and unknown metrics before render', () => {
  for (const mutate of [(v: Record<string, unknown>) => { v.context = 'BUSINESS'; }, (v: Record<string, unknown>) => { v.ownerId = 'foreign'; }, (v: Record<string, unknown>) => { v.email = 'synthetic@example.invalid'; }]) { const r = structuredClone(analyticsFixture()); mutate(r); expect(() => parseAnalytics(r)).toThrow('ANALYTICS_RESPONSE_INVALID'); }
  const r = analyticsFixture(); r.metrics[0].id = 'REVENUE' as never; expect(() => parseAnalytics(r)).toThrow();
});
test('analytics chart and contributing totals cannot diverge or multiply Parcel counts into Shipment counts', () => {
  const r = analyticsFixture(new URLSearchParams('from=2026-11-01&until=2026-11-04'), 2); expect(parseAnalytics(r).shipments.total).toBe('2'); expect(r.parcels.count).toBe('3'); r.shipments.total = '3'; expect(() => parseAnalytics(r)).toThrow();
  const other = analyticsFixture(new URLSearchParams('from=2026-11-01&until=2026-11-04'), 2); other.volume[0].confirmedFeeVnd = '0'; expect(() => parseAnalytics(other)).toThrow();
});
test('VND integer strings beyond Javascript precision survive the client unchanged', () => {
  const r = analyticsFixture(new URLSearchParams('from=2026-11-01&until=2026-11-04'), 1), value = '18446744073709551614';
  for (const m of r.metrics.filter(m => m.unit === 'VND')) { m.value = value; m.points[0].value = value; }
  r.volume[0].confirmedFeeVnd = value; r.volume[0].unpaidVnd = value; r.routes[0].confirmedFeeVnd = value; r.services[0].confirmedFeeVnd = value; r.shipments.items[0].confirmedFeeVnd = value; r.shipments.items[0].unpaidVnd = value;
  expect(parseAnalytics(r).metrics.find(m => m.id === 'CONFIRMED_FEES')!.value).toBe(value);
});
test('unsupported delivery service routes negative money and false zero-denominator growth reject', () => {
  const variants = [analyticsFixture(), analyticsFixture(), analyticsFixture()]; variants[0].status[0].key = 'DELIVERED' as never; variants[1].metrics[0].value = '-1'; variants[2].metrics[0].changePercent = '100.0'; for (const r of variants) expect(() => parseAnalytics(r)).toThrow();
});
test('analytics reuses the authenticated self transport and rejects a response from a logged-out generation', async () => {
  let release!: (response: Response) => void; const pending = new Promise<Response>(resolve => { release = resolve; }), calls: string[] = [];
  const now = Date.now(), owner = '10000000-0000-4000-8000-000000000001';
  const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async (input, init = {}) => { const path = String(input); calls.push(path); if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' }); if (path.endsWith('/refresh')) return Response.json({ accountId: owner, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() }); if (path.endsWith('/me')) return Response.json({ accountId: owner, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 }); if (path.endsWith('/logout')) return new Response(null, { status: 204 }); expect(init.headers).toMatchObject({ Authorization: 'Bearer synthetic-access' }); return pending; } });
  await client.initialize(); const reading = client.readAnalytics(new URLSearchParams()); const rejection = expect(reading).rejects.toThrow('SESSION_CHANGED'); await expect.poll(() => calls.some(p => p.includes('personal-analytics'))).toBe(true); await client.logout(); release(Response.json(analyticsFixture())); await rejection;
});


test('analytics rejects contradictory status KPIs and dimension fee sums instead of showing conflicting Overview facts', () => {
  const parameters = new URLSearchParams('from=2026-11-01&until=2026-11-04');
  for (const field of ['status', 'route', 'service']) {
    const r = analyticsFixture(parameters, 2);
    if (field === 'status') { r.status[0].shipments = '0'; r.status[1].shipments = '2'; }
    if (field === 'route') r.routes[0].confirmedFeeVnd = '1';
    if (field === 'service') r.services[0].confirmedFeeVnd = '1';
    expect(() => parseAnalytics(r)).toThrow('ANALYTICS_RESPONSE_INVALID');
  }
});
