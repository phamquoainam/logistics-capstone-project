import { AdminSessionError } from './session';

export const permissions = ['ADMIN_DASHBOARD_READ', 'ADMIN_MERCHANT_READ', 'ADMIN_NETWORK_READ', 'ADMIN_SHIPMENT_READ', 'ADMIN_FINANCE_READ', 'ADMIN_TECHNICAL_READ'] as const;
export type Permission = typeof permissions[number];
export type AdminContext = { accountId: string; displayName: string; permissions: Permission[]; environment: 'LOCAL' | 'TEST'; timeZone: 'Asia/Ho_Chi_Minh'; regionFilterAvailable: false };
export type Query = { from: string; to: string; grain: 'DAY' | 'WEEK' | 'MONTH'; shipmentStatus: 'BOOKED' | 'AT_STATION' | null; regionId: null };
export type MetricKey = 'BOOKINGS' | 'WAITING_HANDOVER' | 'ACCEPTED' | 'PARCELS' | 'CONFIRMED_FEES' | 'UNPAID_FEES';
export type Metric = { key: MetricKey; value: string; unit: 'SHIPMENT' | 'PARCEL' | 'VND'; drilldown: { path: '/api/v1/admin/dashboard/shipments'; filters: Record<string, string>; metric: MetricKey } | null };
export type Point = { from: string; to: string; metric: MetricKey; value: string };
export type Section = { key: 'MERCHANT' | 'NETWORK' | 'SHIPMENTS' | 'FINANCE' | 'SLA' | 'PRIORITIES'; availability: 'AVAILABLE' | 'SOURCE_NOT_IMPLEMENTED' | 'POLICY_NOT_CONFIGURED'; reasonCode: string; metrics: Metric[]; series: Point[] };
export type Dashboard = { query: Query; generatedAt: string; snapshotId: string; sections: Section[] };
export type Shipment = { id: string; code: string; ownerKind: 'PERSONAL' | 'BUSINESS'; bookedAt: string; status: 'BOOKED' | 'AT_STATION'; parcelCount: number };
export type ShipmentPage = { items: Shipment[]; page: number; size: number; hasNext: boolean; snapshotId: string };
export type PlatformStatus = { checkedAt: string; apiAvailability: 'UP' | 'UNAVAILABLE'; queues: { availability: string; metrics: Metric[] }; scheduledJobs: { availability: string; metrics: Metric[] }; integrationFailures: { availability: string; metrics: Metric[] } };
export const metricLabels: Record<MetricKey, string> = { BOOKINGS: 'Vận đơn đã đặt', WAITING_HANDOVER: 'Chờ bàn giao', ACCEPTED: 'Đã tiếp nhận', PARCELS: 'Kiện hàng', CONFIRMED_FEES: 'Cước đã xác nhận', UNPAID_FEES: 'Cước chưa thanh toán' };
const fail = (): never => { throw new AdminSessionError('RESPONSE_INVALID'); };
function obj(value: unknown, keys: string[]): Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return fail();
  const record = value as Record<string, unknown>;
  if (Object.keys(record).length !== keys.length || keys.some(k => !(k in record))) return fail();
  return record;
}
const id = (v: unknown): v is string => typeof v === 'string' && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000';
const integer = (v: unknown): v is string => typeof v === 'string' && /^(?:0|[1-9][0-9]{0,39})$/.test(v);
export const date = (v: unknown): v is string => typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) && Number.isFinite(Date.parse(v)) && new Date(v).toISOString().slice(0, 10) === v;
const instant = (v: unknown): v is string => typeof v === 'string' && v.length <= 40 && /Z$/.test(v) && Number.isFinite(Date.parse(v));
export function parseContext(value: unknown, accountId: string): AdminContext {
  const v = obj(value, ['accountId', 'displayName', 'permissions', 'environment', 'timeZone', 'regionFilterAvailable']);
  if (v.accountId !== accountId || !id(v.accountId) || typeof v.displayName !== 'string' || !v.displayName.trim() || [...v.displayName].length > 160 || !Array.isArray(v.permissions) || v.permissions.length > 6 || !v.permissions.includes('ADMIN_DASHBOARD_READ') || v.permissions.some(p => !permissions.includes(p as Permission)) || new Set(v.permissions).size !== v.permissions.length || !['LOCAL', 'TEST'].includes(String(v.environment)) || v.timeZone !== 'Asia/Ho_Chi_Minh' || v.regionFilterAvailable !== false) return fail();
  return v as AdminContext;
}
function parseQuery(value: unknown): Query {
  const q = obj(value, ['from', 'to', 'grain', 'shipmentStatus', 'regionId']);
  if (!date(q.from) || !date(q.to) || q.from > q.to || (Date.parse(q.to) - Date.parse(q.from)) / 86400000 >= 366 || !['DAY', 'WEEK', 'MONTH'].includes(String(q.grain)) || q.shipmentStatus !== null && !['BOOKED', 'AT_STATION'].includes(String(q.shipmentStatus)) || q.regionId !== null) return fail();
  return q as Query;
}
export function queryParameters(q: Query) {
  const p = new URLSearchParams({ from: q.from, to: q.to, grain: q.grain });
  if (q.shipmentStatus) p.set('shipmentStatus', q.shipmentStatus);
  return p;
}
function bucketEnd(from: string, query: Query) {
  const cursor = new Date(from + 'T00:00:00Z');
  if (query.grain === 'MONTH') cursor.setUTCMonth(cursor.getUTCMonth() + 1, 1);
  else cursor.setUTCDate(cursor.getUTCDate() + (query.grain === 'WEEK' ? (8 - cursor.getUTCDay()) % 7 || 7 : 1));
  return new Date(Math.min(cursor.getTime(), Date.parse(query.to) + 86400000)).toISOString().slice(0, 10);
}
export function parseDashboard(value: unknown, context: AdminContext, expected: Query): Dashboard {
  const v = obj(value, ['query', 'generatedAt', 'snapshotId', 'sections']), q = parseQuery(v.query);
  if (queryParameters(q).toString() !== queryParameters(expected).toString() || !instant(v.generatedAt) || typeof v.snapshotId !== 'string' || v.snapshotId.length > 4096 || !/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]{43}$/.test(v.snapshotId) || !Array.isArray(v.sections) || v.sections.length > 6) return fail();
  const required: Record<string, Permission> = { MERCHANT: 'ADMIN_MERCHANT_READ', NETWORK: 'ADMIN_NETWORK_READ', SHIPMENTS: 'ADMIN_SHIPMENT_READ', FINANCE: 'ADMIN_FINANCE_READ', SLA: 'ADMIN_SHIPMENT_READ', PRIORITIES: 'ADMIN_SHIPMENT_READ' };
  const names = new Set<string>();
  for (const value of v.sections) {
    const s = obj(value, ['key', 'availability', 'reasonCode', 'metrics', 'series']);
    if (typeof s.key !== 'string' || !(s.key in required) || names.has(s.key) || !context.permissions.includes(required[s.key]) || !['AVAILABLE', 'SOURCE_NOT_IMPLEMENTED', 'POLICY_NOT_CONFIGURED'].includes(String(s.availability)) || typeof s.reasonCode !== 'string' || !/^[A-Z0-9_]{1,100}$/.test(s.reasonCode) || !Array.isArray(s.metrics) || !Array.isArray(s.series) || s.metrics.length > 4 || s.series.length > 1464) return fail();
    names.add(s.key);
    if (s.availability !== 'AVAILABLE') { if (s.metrics.length || s.series.length) return fail(); continue; }
    const allowed: MetricKey[] = s.key === 'SHIPMENTS' ? ['BOOKINGS', 'WAITING_HANDOVER', 'ACCEPTED', 'PARCELS'] : s.key === 'FINANCE' ? ['CONFIRMED_FEES', 'UNPAID_FEES'] : [];
    if (s.metrics.length !== allowed.length || !allowed.length) return fail();
    const keys = new Set<string>();
    for (const value of s.metrics) {
      const m = obj(value, ['key', 'value', 'unit', 'drilldown']);
      if (!allowed.includes(m.key as MetricKey) || keys.has(String(m.key)) || !integer(m.value) || m.unit !== (s.key === 'FINANCE' ? 'VND' : m.key === 'PARCELS' ? 'PARCEL' : 'SHIPMENT')) return fail();
      keys.add(String(m.key));
      if (m.drilldown !== null) {
        const d = obj(m.drilldown, ['path', 'filters', 'metric']);
        if (!context.permissions.includes('ADMIN_SHIPMENT_READ') || d.path !== '/api/v1/admin/dashboard/shipments' || d.metric !== m.key) return fail();
        const filters = obj(d.filters, [...queryParameters(q).keys()]);
        for (const [key, value] of queryParameters(q)) if (filters[key] !== value) return fail();
      }
    }
    for (const value of s.series) {
      const p = obj(value, ['from', 'to', 'metric', 'value']);
      if (!date(p.from) || !date(p.to) || p.from >= p.to || p.from < q.from || Date.parse(p.to) > Date.parse(q.to) + 86400000 || !allowed.includes(p.metric as MetricKey) || !integer(p.value)) return fail();
    }
    for (const raw of s.metrics) {
      const m = raw as Metric, points = (s.series as Point[]).filter(p => p.metric === m.key);
      let end = q.from;
      for (const point of points) { if (point.from !== end || point.to !== bucketEnd(point.from, q)) return fail(); end = point.to; }
      if (Date.parse(end) !== Date.parse(q.to) + 86400000 || points.reduce((n, p) => n + BigInt(p.value), 0n) !== BigInt(m.value)) return fail();
    }
    if (s.key === 'SHIPMENTS') {
      const metrics = s.metrics as Metric[], get = (k: MetricKey) => BigInt(metrics.find(m => m.key === k)!.value);
      if (get('WAITING_HANDOVER') + get('ACCEPTED') !== get('BOOKINGS') || get('PARCELS') < get('BOOKINGS') || get('PARCELS') > get('BOOKINGS') * 5n) return fail();
    }
  }
  return v as Dashboard;
}
export function parseShipments(value: unknown, report: Dashboard, metric: MetricKey, page: number, size: number): ShipmentPage {
  const v = obj(value, ['items', 'page', 'size', 'hasNext', 'snapshotId']);
  if (v.page !== page || v.size !== size || v.snapshotId !== report.snapshotId || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > size || v.hasNext && v.items.length !== size) return fail();
  const ids = new Set<string>();
  for (const raw of v.items) {
    const row = obj(raw, ['id', 'code', 'ownerKind', 'bookedAt', 'status', 'parcelCount']);
    if (!id(row.id) || ids.has(row.id) || typeof row.code !== 'string' || !/^AP-SIM-[a-f0-9]{32}$/.test(row.code) || !['PERSONAL', 'BUSINESS'].includes(String(row.ownerKind)) || !instant(row.bookedAt) || !['BOOKED', 'AT_STATION'].includes(String(row.status)) || !Number.isInteger(row.parcelCount) || Number(row.parcelCount) < 1 || Number(row.parcelCount) > 5 || Date.parse(row.bookedAt) > Date.parse(report.generatedAt)) return fail();
    const at = new Date(Date.parse(row.bookedAt) + 7 * 3600000).toISOString().slice(0, 10);
    if (at < report.query.from || at > report.query.to || report.query.shipmentStatus && row.status !== report.query.shipmentStatus || metric === 'ACCEPTED' && row.status !== 'AT_STATION' || metric === 'WAITING_HANDOVER' && row.status !== 'BOOKED') return fail();
    ids.add(row.id);
  }
  return v as ShipmentPage;
}
export type ShipmentOversight = {
  shipment: Shipment;
  parcels: { id: string; code: string; actualGrams: number; lengthMm: number; widthMm: number; heightMm: number }[];
  tracking: { id: string; kind: 'CARRIER_ACCEPTED_AT_STATION'; occurredAt: string }[];
  acceptance: { eventId: string; occurredAt: string; stationCode: string } | null;
  snapshotId: string;
};
export function parseShipmentOversight(value: unknown, expected: Shipment, report: Dashboard): ShipmentOversight {
  const v = obj(value, ['shipment', 'parcels', 'tracking', 'acceptance', 'snapshotId']);
  if (v.snapshotId !== report.snapshotId) return fail();
  const row = parseShipments({ items: [v.shipment], page: 0, size: 1, hasNext: false, snapshotId: v.snapshotId }, report, 'BOOKINGS', 0, 1).items[0];
  if ((Object.keys(row) as (keyof Shipment)[]).some(key => row[key] !== expected[key]) || !Array.isArray(v.parcels) || v.parcels.length !== row.parcelCount || !Array.isArray(v.tracking) || v.tracking.length > 1) return fail();
  const ids = new Set<string>();
  for (const raw of v.parcels) {
    const p = obj(raw, ['id', 'code', 'actualGrams', 'lengthMm', 'widthMm', 'heightMm']);
    if (!id(p.id) || ids.has(p.id) || p.code !== 'AP-P-' + p.id.replaceAll('-', '')) return fail();
    for (const key of ['actualGrams', 'lengthMm', 'widthMm', 'heightMm']) if (!Number.isSafeInteger(p[key]) || Number(p[key]) <= 0 || Number(p[key]) > (key === 'actualGrams' ? 20000 : 1000)) return fail();
    if (Number(p.lengthMm) + 2 * (Number(p.widthMm) + Number(p.heightMm)) > 2000) return fail();
    ids.add(p.id);
  }
  for (const raw of v.tracking) {
    const event = obj(raw, ['id', 'kind', 'occurredAt']);
    if (!id(event.id) || event.kind !== 'CARRIER_ACCEPTED_AT_STATION' || !instant(event.occurredAt) || Date.parse(event.occurredAt) < Date.parse(row.bookedAt) || Date.parse(event.occurredAt) > Date.parse(report.generatedAt)) return fail();
  }
  if (row.status === 'BOOKED') { if (v.acceptance !== null || v.tracking.length) return fail(); }
  else {
    if (v.tracking.length !== 1) return fail();
    const a = obj(v.acceptance, ['eventId', 'occurredAt', 'stationCode']);
    if (!id(a.eventId) || !instant(a.occurredAt) || a.occurredAt !== (v.tracking[0] as Record<string, unknown>).occurredAt || typeof a.stationCode !== 'string' || !/^[A-Z0-9_-]{1,64}$/.test(a.stationCode)) return fail();
  }
  return v as ShipmentOversight;
}
export function parsePlatform(value: unknown): PlatformStatus {
  const v = obj(value, ['checkedAt', 'apiAvailability', 'queues', 'scheduledJobs', 'integrationFailures']);
  if (!instant(v.checkedAt) || !['UP', 'UNAVAILABLE'].includes(String(v.apiAvailability))) return fail();
  for (const key of ['queues', 'scheduledJobs', 'integrationFailures']) { const source = obj(v[key], ['availability', 'metrics']); if (source.availability !== 'SOURCE_NOT_IMPLEMENTED' || !Array.isArray(source.metrics) || source.metrics.length) return fail(); }
  return v as PlatformStatus;
}
export function todayVietnam(now = Date.now()) { return new Date(now + 7 * 3600000).toISOString().slice(0, 10); }
export function daysBefore(date: string, days: number) { return new Date(Date.parse(date) - days * 86400000).toISOString().slice(0, 10); }
export function displayDate(value: string) { const [year, month, day] = value.slice(0, 10).split('-'); return `${day}/${month}/${year}`; }
export function displayNumber(value: string) { return BigInt(value).toLocaleString('vi-VN'); }
