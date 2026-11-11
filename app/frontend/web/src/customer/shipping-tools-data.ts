import type { DraftParcel, PilotCatalog, PilotShipmentView } from './client';
import { parcelServiceIssues } from './shipping-entry';

export function trackingCodes(text: string): string[] {
  if (text.length > 2048) throw new Error('Nhập tối đa 10 mã vận đơn.');
  const values = text.trim().split(/[\s,;]+/u).filter(Boolean);
  if (!values.length || values.some(v => !/^AP-SIM-[a-f0-9]{32}$/iu.test(v))) throw new Error('Mã vận đơn không hợp lệ. Hãy sao chép đầy đủ mã từ chi tiết vận đơn.');
  const codes = [...new Set(values.map(v => 'AP-SIM-' + v.slice(7).toLowerCase()))];
  if (codes.length > 10) throw new Error('Mỗi lần tra cứu tối đa 10 mã khác nhau.');
  return codes;
}
/** Existing own-only search can also match reference/contact: bind the exact code, never first row. */
export async function ownCode(code: string, read: (page: number, code: string) => Promise<{ items: PilotShipmentView[]; hasNext: boolean }>, current: () => boolean) {
  const seen = new Set<string>();
  for (let page = 0; page < 10; page++) {
    if (!current()) throw new Error('LOOKUP_CANCELLED');
    const result = await read(page, code); if (!current()) throw new Error('LOOKUP_CANCELLED');
    for (const row of result.items) { if (seen.has(row.shipment.id)) throw new Error('LOOKUP_CHANGED'); seen.add(row.shipment.id); }
    const matches = result.items.filter(row => row.shipment.code === code);
    if (matches.length > 1) throw new Error('LOOKUP_AMBIGUOUS');
    if (matches[0]) return matches[0];
    if (!result.hasNext) return undefined;
  }
  throw new Error('LOOKUP_LIMIT');
}
export type MeasuredParcel = Pick<DraftParcel, 'weightGrams' | 'lengthMm' | 'widthMm' | 'heightMm' | 'declaredValueVnd'>;
export function dimensionalGrams(numerator: string, divisor: string): string {
  const n = BigInt(numerator), d = BigInt(divisor), whole = n / d, remainder = n % d;
  const fraction = (remainder * 10000n / d).toString().padStart(4, '0').replace(/0+$/u, '');
  return whole.toLocaleString('vi-VN') + (fraction ? ',' + fraction : '') + (remainder * 10000n % d ? ' (xấp xỉ)' : '');
}
export type PreparationSeed = { source: 'CATALOG_ESTIMATE_1'; originCode: string; destinationCode: string; parcels: MeasuredParcel[] };
export function preparationSeed(value: unknown): PreparationSeed {
  const record = (v: unknown): v is Record<string, unknown> => !!v && typeof v === 'object' && !Array.isArray(v);
  if (!record(value) || Object.keys(value).sort().join() !== ['source', 'originCode', 'destinationCode', 'parcels'].sort().join() || value.source !== 'CATALOG_ESTIMATE_1' || !['SIM-A1/SIM-A2', 'SIM-A2/SIM-A1', 'SIM-A1/SIM-B1', 'SIM-B1/SIM-A1'].includes(value.originCode + '/' + value.destinationCode) || !Array.isArray(value.parcels) || value.parcels.length < 1 || value.parcels.length > 5) throw new Error('PREPARATION_INVALID');
  for (const p of value.parcels) if (!record(p) || Object.keys(p).sort().join() !== ['weightGrams', 'lengthMm', 'widthMm', 'heightMm', 'declaredValueVnd'].sort().join() || !['weightGrams', 'lengthMm', 'widthMm', 'heightMm', 'declaredValueVnd'].every(k => Number.isSafeInteger(p[k]) && Number(p[k]) >= (k === 'declaredValueVnd' ? 0 : 1)) || Number(p.weightGrams) > 20000 || Number(p.declaredValueVnd) > 1_000_000_000_000 || ['lengthMm', 'widthMm', 'heightMm'].some(k => Number(p[k]) > 1000) || Number(p.lengthMm) + 2 * (Number(p.widthMm) + Number(p.heightMm)) > 2000) throw new Error('PREPARATION_INVALID');
  return structuredClone(value) as PreparationSeed;
}
export function indicativeEstimate(catalog: PilotCatalog, origin: string, destination: string, parcels: MeasuredParcel[]) {
  const route = catalog.coverage.routes.find(r => r.originCode === origin && r.destinationCode === destination);
  if (!route) throw new Error('Tuyến chưa được hỗ trợ.');
  const issues = parcelServiceIssues(parcels.map(p => ({ ...p, description: '', quantity: 1 })), catalog.tariff);
  if (issues.length) throw new Error(issues.join(' '));
  if (parcels.some(p => !Number.isSafeInteger(p.declaredValueVnd) || p.declaredValueVnd < 0 || p.declaredValueVnd > 1_000_000_000_000)) throw new Error('Giá trị khai báo chưa hợp lệ.');
  const t = catalog.tariff, step = BigInt(t.stepGrams), divisor = BigInt(t.volumetricDivisor);
  const lines = parcels.map(p => {
    const volume = BigInt(p.lengthMm) * BigInt(p.widthMm) * BigInt(p.heightMm), actual = BigInt(p.weightGrams) * divisor;
    const basis = volume > actual ? volume : actual, chargeable = (basis + divisor * step - 1n) / (divisor * step) * step;
    const base = BigInt(route.classification === 'INTRA' ? t.intraFirstVnd : t.interFirstVnd), increment = BigInt(route.classification === 'INTRA' ? t.intraStepVnd : t.interStepVnd), additional = (chargeable / step - 1n) * increment;
    return { actualGrams: String(p.weightGrams), dimensionalNumerator: String(volume), dimensionalDivisor: String(divisor), chargeableGrams: String(chargeable), baseVnd: String(base), additionalVnd: String(additional), totalVnd: String(base + additional) };
  });
  return { lines, totalVnd: String(lines.reduce((n, p) => n + BigInt(p.totalVnd), 0n)), route, tariffVersion: t.version };
}
