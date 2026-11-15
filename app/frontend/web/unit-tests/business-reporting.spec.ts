import { test, expect } from '@playwright/test';
import { parseOperationsReport, parseFinanceReport, parseReportExport, parseReportHistory } from '../src/customer/business-report-data';
import { businessReportQuery, reportAverage, reportDetailLink, reportToManagement } from '../src/customer/business-report-navigation';
import { organizationReturn } from '../src/customer/organization-management';
import { createCustomerClient } from '../src/customer/client';
import { operationsFixture, financeFixture, exportFixture, reportingContext, reportShipment, reportExportId } from '../ui-unit-tests/support/business-report-fixture';
const org = reportingContext.organizationId;

test('operations reconciles whole cohort with multiple Parcels and internal events remain separate', () => {
  const value = parseOperationsReport(operationsFixture(), org);
  expect(value.summary.shipments).toBe('2'); expect(value.summary.parcels).toBe('3'); expect(value.preparations.total).toBe('4');
  expect(value.trend.reduce((n, b) => n + BigInt(b.shipments), 0n)).toBe(2n);
  for (const bad of [{ ...value, organizationId: reportShipment }, { ...value, recipientEmail: 'synthetic@example.invalid' }, { ...value, summary: { ...value.summary, shipments: '3' } }]) expect(() => parseOperationsReport(bad, org)).toThrow();
});
test('financial absence stays null across all operation dimensions and cannot become zero', () => {
  const value = operationsFixture(new URLSearchParams(), false); expect(parseOperationsReport(value, org).financialState).toBe('FORBIDDEN');
  expect(() => parseOperationsReport({ ...value, summary: { ...value.summary, confirmedFeesVnd: '0' } }, org)).toThrow();
  value.breakdowns.facility[0].unpaidFeesVnd = '0'; expect(() => parseOperationsReport(value, org)).toThrow();
});
test('report parser rejects broken calendar chains, dimensions, internal grain and fake availability', () => {
  const base = operationsFixture();
  for (const mutate of [(v: typeof base) => { v.trend[0].until = v.period.from; }, (v: typeof base) => { v.breakdowns.route.push(v.breakdowns.route[0]); }, (v: typeof base) => { v.activity.created = '5'; }, (v: typeof base) => { v.capabilities[0].state = 'AVAILABLE'; }]) { const value = structuredClone(base); mutate(value); expect(() => parseOperationsReport(value, org)).toThrow(); }
});
test('Finance pagination preserves summary and rejects fabricated PAID or mismatched fee snapshot', () => {
  const value = financeFixture(); expect(parseFinanceReport(value, org, 0).items).toHaveLength(2);
  const second = financeFixture(new URLSearchParams({ page: '1' })); expect(parseFinanceReport(second, org, 1).summary.unpaidFeesVnd).toBe('75000'); expect(second.items).toHaveLength(0);
  for (const patch of [{ paymentState: 'PAID' }, { confirmedVnd: '1' }, { chargeableGrams: '999' }, { recipientName: 'Not allowed' }]) expect(() => parseFinanceReport({ ...value, items: [{ ...value.items[0], ...patch }, value.items[1]] }, org, 0)).toThrow();
  expect(() => parseFinanceReport({ ...value, hasNext: true }, org, 0)).toThrow();
});
test('averages retain rational exact VND above JS precision and round HALF_UP', () => {
  expect(reportAverage({ totalVnd: '18446744073709551615', shipmentCount: '2' })).toBe('9223372036854775808'); expect(reportAverage({ totalVnd: '1', shipmentCount: '2' })).toBe('1'); expect(reportAverage({ totalVnd: '0', shipmentCount: '0' })).toBeNull();
});
test('report query and exact drill preserve cohort but never forward grouping or pagination', () => {
  const raw = new URLSearchParams({ from: '2026-11-01', until: '2026-11-15', interval: 'QUARTER', sourceChannel: 'WEB', panel: 'dimensions' }), q = businessReportQuery(raw, false); expect(q.valid).toBe(true); expect(q.api.has('panel')).toBe(false);
  const drill = reportToManagement(q.api, { origin: 'SIM-A1', destination: 'SIM-A2' }); expect(drill.get('from')).toBe('2026-11-01'); expect(drill.get('sourceChannel')).toBe('WEB'); expect(drill.has('interval')).toBe(false); expect(drill.get('destination')).toBe('SIM-A2');
  for (const bad of ['from=2026-11-01', 'page=0', 'interval=WEEK&interval=DAY', 'ownerId=' + org, 'panel=anything', 'sourceChannel=%00', 'page=Infinity']) expect(businessReportQuery(new URLSearchParams(bad), false).valid, bad).toBe(false);
});
test('detail can return only to valid same Organization report or Finance scope', () => {
  const q = new URLSearchParams({ from: '2026-11-01', until: '2026-11-15', panel: 'contracts' }), target = new URL(reportDetailLink(org, reportShipment, 'finance', q), 'https://local.invalid').searchParams.get('returnTo');
  expect(organizationReturn(org, target)).toBe('/merchant/' + org + '/finance?' + q);
  for (const bad of ['/merchant/' + reportShipment + '/finance', '/merchant/' + org + '/reports?from=2026-11-01&from=x', '/merchant/' + org + '/finance?token=x', 'https://evil.invalid/', '/merchant/' + org + '/reports#token']) expect(organizationReturn(org, bad)).toBe('/merchant/' + org + '/shipments');
});
test('export and history protocols reject PII, extra columns, oversized rows and unknown selectors', () => {
  const value = exportFixture(); expect(parseReportExport(value, org, 'OPERATIONS', 'trend').rows[0][4]).toBe('=SYNTHETIC(1)');
  for (const bad of [{ ...value, rowCount: 1001 }, { ...value, columns: [...value.columns, 'phone'] }, { ...value, organizationId: reportShipment }]) expect(() => parseReportExport(bad, org, 'OPERATIONS', 'trend')).toThrow();
  const history = { items: [{ exportId: reportExportId, kind: 'OPERATIONS', section: 'trend', query: { from: '2026-11-01', until: '2026-11-02', interval: 'DAY' }, rowCount: 1, occurredAt: value.generatedAt }], page: 0, size: 20, hasNext: false }; expect(parseReportHistory(history, 0).items).toHaveLength(1);
  expect(() => parseReportHistory({ ...history, items: [{ ...history.items[0], query: { search: 'private contact' } }] }, 0)).toThrow();
});
test('report transport is authenticated, no-store, Organization-bound and refuses broadened searched exports', async () => {
  const calls: string[] = [], now = Date.now();
  const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async (path, init = {}) => {
    const url = String(path); if (url.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' }); if (url.endsWith('/refresh')) return Response.json({ accountId: org, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() }); if (url.endsWith('/me')) return Response.json({ accountId: org, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    calls.push(url); expect(url).toContain('/merchant-organizations/' + org + '/'); expect(init.cache).toBe('no-store'); expect(init.headers).toMatchObject({ Authorization: 'Bearer synthetic-access' });
    const body = url.includes('shipping-report-exports') ? exportFixture('FINANCE') : url.includes('shipping-finance') ? financeFixture() : operationsFixture(); return Response.json(body, { headers: { 'Cache-Control': 'no-store' } });
  } });
  try { await client.initialize(); await client.readBusinessOperations(reportingContext, new URLSearchParams()); await client.readBusinessFinance(reportingContext, new URLSearchParams());
    await expect(client.exportBusinessReport(reportingContext, new URLSearchParams({ search: 'private' }), 'FINANCE', 'trend')).rejects.toThrow(); expect(calls).toHaveLength(2);
    await client.exportBusinessReport(reportingContext, new URLSearchParams({ page: '1', size: '20', sort: 'NEWEST', interval: 'MONTH', sourceChannel: 'WEB' }), 'FINANCE', 'trend'); expect(calls[2]).toContain('sourceChannel=WEB'); expect(calls[2]).not.toContain('page='); expect(calls[2]).not.toContain('sort=');
  } finally { client.dispose(); }
});
