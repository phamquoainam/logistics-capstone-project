import type { DraftParcel, PilotCatalog, PilotQuote } from './client';

/** Explain an already-issued server fee only when the immutable tariff agrees. Never issues a price. */
export function quotedFeeBreakdown(quote: PilotQuote, line: PilotQuote['feeLines'][number], tariff: PilotCatalog['tariff']) {
  if (quote.tariffVersion !== tariff.version || line.chargeableGrams % tariff.stepGrams !== 0) return undefined;
  const base = quote.route.classification === 'INTRA' ? tariff.intraFirstVnd : tariff.interFirstVnd;
  const step = quote.route.classification === 'INTRA' ? tariff.intraStepVnd : tariff.interStepVnd;
  const steps = line.chargeableGrams / tariff.stepGrams - 1, additional = steps * step;
  return steps >= 0 && base + additional === line.feeVnd ? { base, additional, steps } : undefined;
}

/** No measured facts are assumed on behalf of the sender. */
export const emptyParcel = (): DraftParcel => ({ description: '', quantity: 1, weightGrams: NaN, lengthMm: NaN, widthMm: NaN, heightMm: NaN, declaredValueVnd: 0 });

/** Presentation preflight only; authoritative eligibility is always checked by Backend. */
export function parcelServiceIssues(parcels: DraftParcel[], limits: PilotCatalog['tariff']): string[] {
  const issues: string[] = [];
  if (!parcels.length || parcels.length > limits.maxParcels) issues.push(`Dịch vụ nhận từ 1 đến ${limits.maxParcels} kiện.`);
  parcels.forEach((p, index) => {
    const label = `Kiện ${index + 1}`;
    if (![p.weightGrams, p.lengthMm, p.widthMm, p.heightMm].every(v => Number.isSafeInteger(v) && v > 0)) { issues.push(`${label}: cần khai đầy đủ trọng lượng và kích thước.`); return; }
    if (p.weightGrams > limits.maxActualGrams) issues.push(`${label}: trọng lượng thực vượt ${limits.maxActualGrams / 1000} kg.`);
    if (Math.max(p.lengthMm, p.widthMm, p.heightMm) > limits.maxDimensionMm) issues.push(`${label}: mỗi chiều không được vượt ${limits.maxDimensionMm / 10} cm.`);
    if (p.lengthMm + 2 * (p.widthMm + p.heightMm) > limits.maxGirthMm) issues.push(`${label}: L + 2(R + C) vượt ${limits.maxGirthMm / 10} cm.`);
  });
  return issues;
}

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
