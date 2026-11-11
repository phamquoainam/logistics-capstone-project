import { test, expect } from '@playwright/test';
import { centimetresToMillimetres, emptyParcel, parcelServiceIssues, parcelTotals, quotedFeeBreakdown } from '../src/customer/shipping-entry';
import type { PilotCatalog, PilotQuote } from '../src/customer/client';
const parcel = { description: 'Synthetic box', quantity: 3, weightGrams: 501, lengthMm: 123, widthMm: 100, heightMm: 100, declaredValueVnd: 100000 };

test('centimetres serialize exactly into the existing millimetre contract', () => {
  for (const [cm, mm] of [['0.1', 1], ['2.9', 29], ['12.3', 123], ['100', 1000], ['100000', 1000000]] as const) expect(centimetresToMillimetres(cm)).toBe(mm);
});
test('dimensions never round unsupported precision or invalid values into valid facts', () => {
  for (const value of ['', '0', '-1', '1.23', '1e2', 'Infinity', '100000.1']) expect(Number.isNaN(centimetresToMillimetres(value))).toBe(true);
});
test('multiple physical parcels aggregate facts without multiplying by goods quantity', () => {
  expect(parcelTotals([parcel, { ...parcel, quantity: 10, weightGrams: 999, declaredValueVnd: 200000 }])).toEqual({ parcels: 2, weightGrams: 1500, declaredValueVnd: 300000 });
});
test('incomplete measurements are absent rather than fabricated as zero; declared zero stays valid', () => {
  expect(parcelTotals([{ ...parcel, weightGrams: Number.NaN, declaredValueVnd: 0 }])).toEqual({ parcels: 1, weightGrams: undefined, declaredValueVnd: 0 });
});

const limits = { maxParcels: 5, maxActualGrams: 20000, maxDimensionMm: 1000, maxGirthMm: 2000 } as PilotCatalog['tariff'];
test('new parcels require actual measurements rather than accepting guessed shipping facts', () => {
  const value = emptyParcel();
  expect(parcelTotals([value]).weightGrams).toBeUndefined();
  expect([value.weightGrams, value.lengthMm, value.widthMm, value.heightMm].every(Number.isNaN)).toBe(true);
  expect(parcelServiceIssues([value], limits)).toHaveLength(1);
});
test('service preflight accepts exact approved weight dimension and girth boundaries', () => {
  expect(parcelServiceIssues([{ ...parcel, weightGrams: 20000, lengthMm: 1000, widthMm: 250, heightMm: 250 }], limits)).toEqual([]);
  expect(parcelServiceIssues([{ ...parcel, lengthMm: 600, widthMm: 350, heightMm: 350 }], limits)).toEqual([]);
});

test('fee explanation reconciles the exact server snapshot and fails closed on another tariff or total', () => {
  const tariff = { version: 'VERSION-1', stepGrams: 500, intraFirstVnd: 25000, intraStepVnd: 3000, interFirstVnd: 38000, interStepVnd: 5000 } as PilotCatalog['tariff'];
  const quote = { tariffVersion: tariff.version, route: { classification: 'INTRA' } } as PilotQuote;
  const line = { parcelNumber: 1, actualGrams: 501, chargeableGrams: 1000, feeVnd: 28000 };
  expect(quotedFeeBreakdown(quote, line, tariff)).toEqual({ base: 25000, additional: 3000, steps: 1 });
  expect(quotedFeeBreakdown({ ...quote, tariffVersion: 'OTHER' }, line, tariff)).toBeUndefined();
  expect(quotedFeeBreakdown(quote, { ...line, feeVnd: 25000 }, tariff)).toBeUndefined();
});
test('service preflight identifies the affected physical parcel without multiplying contents quantity', () => {
  expect(parcelServiceIssues([parcel, { ...parcel, weightGrams: 20001, lengthMm: 1001, widthMm: 500 }], limits)).toEqual([
    'Kiện 2: trọng lượng thực vượt 20 kg.', 'Kiện 2: mỗi chiều không được vượt 100 cm.', 'Kiện 2: L + 2(R + C) vượt 200 cm.',
  ]);
  expect(parcelServiceIssues(Array.from({ length: 6 }, () => parcel), limits)).toEqual(['Dịch vụ nhận từ 1 đến 5 kiện.']);
});
