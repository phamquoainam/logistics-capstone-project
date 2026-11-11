import { test, expect } from '@playwright/test';
import { dimensionalGrams, indicativeEstimate, ownCode, preparationSeed, trackingCodes } from '../src/customer/shipping-tools-data';
import { toolsCatalog } from '../ui-unit-tests/support/shipping-tools-fixture';
import { managementFixture } from '../ui-unit-tests/support/management-fixture';
const parcel = { weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 };
test('code batch canonicalizes and deduplicates only valid opaque codes, with ten-code and payload limits', () => {
  const code = 'AP-SIM-' + 'a'.repeat(32); expect(trackingCodes(code + '\n' + code.toUpperCase() + '; ' + code)).toEqual([code]);
  for (const value of ['', 'someone@example.invalid', 'AP-P-' + 'a'.repeat(32), 'x'.repeat(2049), Array.from({ length: 11 }, (_, n) => 'AP-SIM-' + n.toString(16).padStart(32, '0')).join(',')]) expect(() => trackingCodes(value)).toThrow();
});
test('exact-code lookup rejects substring collateral and traverses later own pages', async () => {
  const rows = managementFixture(new URLSearchParams(), 2).items.map(i => i.record), code = rows[1].shipment.code; let reads = 0;
  expect(await ownCode(code, async page => { reads++; return { items: [rows[page]], hasNext: page === 0 }; }, () => true)).toEqual(rows[1]); expect(reads).toBe(2);
  expect(await ownCode(code, async () => ({ items: [rows[0]], hasNext: false }), () => true)).toBeUndefined();
});
test('lookup cancellation, repeated source rows and scan exhaustion are unknown rather than not-found', async () => {
  const row = managementFixture(new URLSearchParams(), 1).items[0].record;
  await expect(ownCode('missing', async () => ({ items: [row], hasNext: true }), () => true)).rejects.toThrow('LOOKUP_CHANGED');
  await expect(ownCode('missing', async () => ({ items: [], hasNext: true }), () => true)).rejects.toThrow('LOOKUP_LIMIT');
  let read = false; await expect(ownCode('missing', async () => { read = true; return { items: [], hasNext: false }; }, () => false)).rejects.toThrow('LOOKUP_CANCELLED'); expect(read).toBe(false);
});
test('indicative tariff calculation is per Parcel, exact across half-kg and volumetric boundaries', () => {
  expect(dimensionalGrams('1001', '5000')).toBe('0,2002'); expect(dimensionalGrams('2510000', '5000')).toBe('502');
  const result = indicativeEstimate(toolsCatalog, 'SIM-A1', 'SIM-A2', [{ ...parcel, weightGrams: 501 }, parcel]); expect(result.totalVnd).toBe('53000'); expect(result.lines.map(l => l.chargeableGrams)).toEqual(['1000', '500']);
  const volume = indicativeEstimate(toolsCatalog, 'SIM-A1', 'SIM-A2', [{ ...parcel, lengthMm: 251 }]); expect(volume.lines[0].chargeableGrams).toBe('1000'); expect(volume.totalVnd).toBe('28000');
  expect(indicativeEstimate(toolsCatalog, 'SIM-A1', 'SIM-B1', [{ ...parcel, weightGrams: 501, declaredValueVnd: 1e12 }]).totalVnd).toBe('43000');
});
test('unsupported directions, missing measured facts and actual service limits cannot become a valid estimate', () => {
  for (const p of [{ ...parcel, weightGrams: 0 }, { ...parcel, weightGrams: 20001 }, { ...parcel, lengthMm: 1001 }, { ...parcel, lengthMm: 1000, widthMm: 500, heightMm: 500 }, { ...parcel, declaredValueVnd: 1.2 }]) expect(() => indicativeEstimate(toolsCatalog, 'SIM-A1', 'SIM-A2', [p])).toThrow();
  expect(() => indicativeEstimate(toolsCatalog, 'SIM-A2', 'SIM-B1', [parcel])).toThrow('Tuyến chưa được hỗ trợ'); expect(() => indicativeEstimate(toolsCatalog, 'SIM-A1', 'SIM-A2', Array(6).fill(parcel))).toThrow();
});
test('handoff admits only raw valid measurements and route, never client money quotes credentials or invalid facts', () => {
  const seed = { source: 'CATALOG_ESTIMATE_1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', parcels: [parcel] };
  expect(preparationSeed(seed)).toEqual(seed); expect(preparationSeed(seed)).not.toBe(seed);
  for (const value of [{ ...seed, totalVnd: 1 }, { ...seed, quoteId: 'fake' }, { ...seed, parcels: [{ ...parcel, code: 'fake' }] }, { ...seed, destinationCode: 'SIM-A1' }, { ...seed, parcels: [] }, { ...seed, parcels: [{ ...parcel, weightGrams: NaN }] }]) expect(() => preparationSeed(value)).toThrow('PREPARATION_INVALID');
});
