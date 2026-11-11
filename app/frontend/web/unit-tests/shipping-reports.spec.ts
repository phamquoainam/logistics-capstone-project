import { test, expect } from '@playwright/test';
import { parseShippingReport } from '../src/customer/analytics-data';
import { durationLabel, ratio, reportBuckets, reportCsv } from '../src/customer/shipping-reports-data';
import { shippingReportsFixture } from '../ui-unit-tests/support/analytics-fixture';

const period = new URLSearchParams({ from: '2025-12-30', until: '2026-01-03', compare: 'none' });
test('RF05 parses exact cohort weights and sample size while retaining old nested response', () => {
  const source = shippingReportsFixture(period, 45), parsed = parseShippingReport(source);
  expect(parsed.report.shipments.items).toHaveLength(20); expect(parsed.parcelWeights.actualGrams).toBe('22500'); expect(parsed.acceptanceTiming.sampleShipments).toBe('22');
});
test('RF05 rejects partial inconsistent or foreign shaped summaries rather than treating them as zero', () => {
  const source = shippingReportsFixture(period, 2);
  for (const change of [(s: typeof source) => { s.acceptanceTiming.sampleShipments = '2'; }, (s: typeof source) => { s.acceptanceTiming.minimumMillis = null; }, (s: typeof source) => { s.acceptanceTiming.totalDurationMillis = '1'; }, (s: typeof source) => { s.parcelWeights.chargeableGrams = '999'; }, (s: typeof source) => { s.parcelWeights.actualGrams = '999999'; }, (s: typeof source) => { s.report.metrics[0].points[0].date = '2025-12-01'; }]) { const value = structuredClone(source); change(value); expect(() => parseShippingReport(value)).toThrow(); }
  expect(() => parseShippingReport({ ...source, ownerId: 'foreign' })).toThrow();
});
test('empty acceptance sample is N/A while a real zero duration remains a real observed zero', () => {
  expect(parseShippingReport(shippingReportsFixture(period, 0)).acceptanceTiming.minimumMillis).toBeNull();
  expect(durationLabel('0', '0')).toBe('N/A'); expect(durationLabel(null)).toBe('N/A'); expect(durationLabel('0', '1')).toBe('0 giây'); expect(durationLabel('500', '1')).toBe('Dưới 1 giây'); expect(durationLabel('120000', '2')).toBe('1 phút 0 giây');
});
test('day week month quarter and year rollups reconcile the full captured source beyond the first page', () => {
  const source = shippingReportsFixture(period, 45);
  for (const group of ['day', 'week', 'month', 'quarter', 'year'] as const) {
    const rows = reportBuckets(source, group, period.get('from')!, period.get('until')!);
    expect(rows.reduce((n, r) => n + BigInt(r.shipments), 0n)).toBe(45n); expect(rows.reduce((n, r) => n + BigInt(r.parcels), 0n)).toBe(45n);
    expect(rows.reduce((n, r) => n + BigInt(r.confirmedVnd), 0n)).toBe(BigInt(source.report.metrics.find(m => m.id === 'CONFIRMED_FEES')!.value));
  }
});
test('quarter/year drill intervals are clipped to the exact global range and labelled partial', () => {
  const rows = reportBuckets(shippingReportsFixture(period, 4), 'quarter', '2025-12-30', '2026-01-03');
  expect(rows.map(r => [r.label, r.from, r.until, r.partial])).toEqual([['Q4/2025', '2025-12-30', '2026-01-01', true], ['Q1/2026', '2026-01-01', '2026-01-03', true]]);
});
test('report CSV exports full aggregate rows not twenty source records and labels scope/time', () => {
  const source = shippingReportsFixture(period, 45), rows = reportBuckets(source, 'year', '2025-12-30', '2026-01-03');
  const csv = reportCsv(source, 'volume', rows, '=synthetic-formula');
  expect(csv.startsWith('\uFEFF')).toBe(true); expect(csv).toContain('LOCAL/TEST SIMULATION'); expect(csv).toContain('Thời điểm dữ liệu'); expect(csv).toContain('"\'=synthetic-formula"'); expect(csv).not.toContain('example.invalid'); expect(csv).not.toContain('AP-SIM-');
});
test('ratio uses exact integer arithmetic with a defined denominator, never fabricated growth', () => {
  expect(ratio('1', '3', true)).toBe('33,33%'); expect(ratio('0', '0', true)).toBe('N/A'); expect(ratio('900719925474099300', '3')).toBe('300.239.975.158.033.100,00');
});
