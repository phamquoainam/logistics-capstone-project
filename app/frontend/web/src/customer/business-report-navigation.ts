import { organizationFilterKeys } from './organization-management';
export const reportQueryKeys = ['from', 'until', 'interval', 'service', 'origin', 'destination', 'facilityId', 'creatorId', 'assignee', 'sourceChannel', 'batchId', 'status', 'obligation', 'handover', 'page', 'size', 'search', 'sort', 'panel'];
export const reportPanels = ['volume', 'dimensions', 'activity', 'availability', 'statement', 'contracts', 'cod', 'documents', 'history'];
export const vietnamToday = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
export function dateOffset(value: string, days: number) { return new Date(Date.parse(value + 'T00:00:00Z') + days * 86400000).toISOString().slice(0, 10); }
export function businessReportQuery(raw: URLSearchParams, finance: boolean) {
  const page = Number(raw.get('page') ?? 0), panel = raw.get('panel') ?? (finance ? 'statement' : 'volume'), interval = raw.get('interval') ?? 'DAY';
  const valid = [...raw.keys()].every(k => reportQueryKeys.includes(k) && raw.getAll(k).length === 1 && (raw.get(k)?.length ?? 0) <= 160 && !/[\u0000-\u001f\u007f]/.test(raw.get(k) ?? '')) && reportPanels.includes(panel) && ['DAY', 'WEEK', 'MONTH', 'QUARTER', 'YEAR'].includes(interval) && Number.isSafeInteger(page) && page >= 0 && page <= 2147483647 && /^(0|[1-9][0-9]*)$/.test(raw.get('page') ?? '0') && (finance || !['page', 'size', 'search', 'sort'].some(k => raw.has(k))) && raw.has('from') === raw.has('until');
  const api = new URLSearchParams(raw); api.delete('panel'); api.set('interval', interval); if (!api.has('from')) { const today = vietnamToday(); api.set('from', dateOffset(today, -29)); api.set('until', dateOffset(today, 1)); }
  return { valid, page, panel, api, key: api.toString() };
}
export function reportToManagement(parameters: URLSearchParams, extra: Record<string, string> = {}) { const result = new URLSearchParams(); for (const key of organizationFilterKeys) if (!['page', 'size', 'sort'].includes(key) && parameters.has(key)) result.set(key, parameters.get(key)!); for (const [k, v] of Object.entries(extra)) result.set(k, v); return result; }
export function reportDetailLink(org: string, shipment: string, route: 'reports' | 'finance', query: URLSearchParams) { return '/merchant/' + org + '/shipments/' + shipment + '?' + new URLSearchParams({ returnTo: '/merchant/' + org + '/' + route + (query.size ? '?' + query : '') }); }
export function reportAverage(value: { totalVnd: string; shipmentCount: string } | null) { if (!value || BigInt(value.shipmentCount) === 0n) return null; const n = BigInt(value.totalVnd), d = BigInt(value.shipmentCount); return ((n * 2n + d) / (2n * d)).toString(); }
