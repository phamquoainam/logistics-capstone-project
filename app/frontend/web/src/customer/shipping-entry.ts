import type { DraftParcel } from './client';

/** Human centimetres -> the existing integer millimetre contract, without float rounding. */
export function centimetresToMillimetres(value: string): number {
  const match = /^(\d+)(?:\.(\d))?$/.exec(value);
  if (!match) return Number.NaN;
  const result = Number(match[1]) * 10 + Number(match[2] ?? '0');
  return Number.isSafeInteger(result) && result >= 1 && result <= 1_000_000 ? result : Number.NaN;
}

/** Facts per physical Parcel: quantity is its contents, not a price/weight multiplier. */
export function parcelTotals(parcels: DraftParcel[]) {
  const sum = (field: 'weightGrams' | 'declaredValueVnd') => parcels.every(p => Number.isSafeInteger(p[field]) && p[field] >= (field === 'weightGrams' ? 1 : 0))
    ? parcels.reduce((total, p) => total + p[field], 0) : undefined;
  return { parcels: parcels.length, weightGrams: sum('weightGrams'), declaredValueVnd: sum('declaredValueVnd') };
}
