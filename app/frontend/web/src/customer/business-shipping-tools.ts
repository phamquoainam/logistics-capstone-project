import type { OrganizationManagementItem, OrganizationManagementPage } from './business-management-data';
import type { BusinessQuoteView, BusinessPreparation } from './client';

/** Monotonic countdown can never exceed the duration reported by the server. */
export function remainingBusinessQuoteSeconds(view: BusinessQuoteView, deadline: number, now: number) {
  return Math.max(0, Math.min(Math.ceil((Date.parse(view.quote.expiresAt) - Date.parse(view.serverTime)) / 1000), Math.ceil((deadline - now) / 1000)));
}

export function quoteMatchesDraft(view: BusinessQuoteView, draft: BusinessPreparation) {
  const q = view.quote;
  return q.organizationId === draft.organizationId && q.preparationId === draft.id && q.preparationVersion === draft.version && q.facilityId === draft.facilityId && q.parcels.length === draft.input.parcels.length && q.parcels.every((p, i) => ['weightGrams', 'lengthMm', 'widthMm', 'heightMm'].every(k => p[k as keyof typeof p] === draft.input.parcels[i][k as keyof typeof p]));
}

export type LookupKind = 'SHIPMENT' | 'PARCEL' | 'REFERENCE';
export function businessLookupInputs(text: string, kind: LookupKind): string[] {
  if (text.length > 2048) throw new Error('Nhập tối đa 10 mã, mỗi mã tối đa 160 ký tự.');
  const parts = text.trim().split(kind === 'REFERENCE' ? /[\n;]+/u : /[\s,;]+/u).map(v => v.trim()).filter(Boolean);
  if (!parts.length || parts.some(p => p.length > 160 || /[\u0000-\u001f\u007f]/u.test(p))) throw new Error('Nhập mã hợp lệ; mỗi tham chiếu đặt trên một dòng.');
  const pattern = kind === 'SHIPMENT' ? /^AP-SIM-[a-f0-9]{32}$/iu : /^AP-P-[a-f0-9]{32}$/iu;
  if (kind !== 'REFERENCE' && parts.some(p => !pattern.test(p))) throw new Error('Sao chép đầy đủ mã vận đơn hoặc mã kiện đã xác nhận.');
  const values = [...new Set(parts.map(p => kind === 'REFERENCE' ? p : kind === 'SHIPMENT' ? 'AP-SIM-' + p.slice(7).toLowerCase() : 'AP-P-' + p.slice(5).toLowerCase()))];
  if (values.length > 10) throw new Error('Mỗi lần tra cứu tối đa 10 mã khác nhau.');
  return values;
}
export async function lookupBusinessShipment(value: string, kind: LookupKind, read: (query: URLSearchParams) => Promise<OrganizationManagementPage>, current: () => boolean) {
  const matches: OrganizationManagementItem[] = [], seen = new Set<string>();
  for (let page = 0; page < 10; page++) {
    if (!current()) throw new Error('LOOKUP_CANCELLED');
    const data = await read(new URLSearchParams({ search: value, page: String(page) }));
    if (!current()) throw new Error('LOOKUP_CANCELLED');
    for (const item of data.items) {
      const s = item.record.shipment;
      if (seen.has(s.id)) throw new Error('LOOKUP_CHANGED');
      seen.add(s.id);
      const match = kind === 'SHIPMENT' ? s.code === value : kind === 'PARCEL' ? s.parcels.some(p => 'AP-P-' + p.id.replaceAll('-', '') === value) : s.input.reference === value || item.source.sourceOrderReference === value;
      if (match) matches.push(item);
      if (matches.length > 20) throw new Error('LOOKUP_LIMIT');
    }
    if (kind !== 'REFERENCE' && matches.length) {
      if (matches.length !== 1) throw new Error('LOOKUP_AMBIGUOUS');
      return matches;
    }
    if (!data.hasNext) return matches;
  }
  throw new Error('LOOKUP_LIMIT');
}
