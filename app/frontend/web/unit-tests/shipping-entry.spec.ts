import { test, expect } from '@playwright/test';
import { centimetresToMillimetres, parcelTotals } from '../src/customer/shipping-entry';
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
