export const metricDefinitions = {
  BOOKED_SHIPMENTS: { title: 'Vận đơn đã đặt', unit: 'SHIPMENT', dimension: 'shipments', description: 'Số vận đơn đã Booking trong kỳ; mỗi vận đơn chỉ tính một lần.' },
  AWAITING_HANDOVER: { title: 'Chờ bàn giao', unit: 'SHIPMENT', dimension: 'status', description: 'Vận đơn đặt trong kỳ chưa có sự kiện Carrier tiếp nhận được ghi nhận.' },
  ACCEPTED_SHIPMENTS: { title: 'Đã tiếp nhận', unit: 'SHIPMENT', dimension: 'status', description: 'Vận đơn đặt trong kỳ đã được Carrier nhận tại Station; không đồng nghĩa đã giao.' },
  CONFIRMED_FEES: { title: 'Cước đã xác nhận', unit: 'VND', dimension: 'fees', description: 'Cước chốt khi Booking của vận đơn trong kỳ. Đây không phải số tiền đã thu.' },
  UNPAID_OBLIGATIONS: { title: 'Nghĩa vụ chưa thanh toán', unit: 'VND', dimension: 'fees', description: 'Nghĩa vụ UNPAID của các vận đơn trong kỳ, đối chiếu từ Finance. Chức năng thu tiền chưa mở.' },
  CREATED_PARCELS: { title: 'Kiện hàng đã tạo', unit: 'PARCEL', dimension: 'parcels', description: 'Số kiện thuộc các vận đơn trong bộ lọc. Một vận đơn có thể gồm nhiều kiện.' },
} as const;
export type MetricId = keyof typeof metricDefinitions;
export type AnalyticsMetric = { id: MetricId; unit: 'SHIPMENT' | 'PARCEL' | 'VND'; value: string; previousValue: string | null; changePercent: string | null; points: { date: string; value: string }[] };
export type AnalyticsReport = {
  schemaVersion: 'PERSONAL_ANALYTICS_1'; context: 'PERSONAL'; timeZone: 'Asia/Ho_Chi_Minh'; generatedAt: string;
  period: { startInclusive: string; endExclusive: string; previousStartInclusive: string | null; previousEndExclusive: string | null };
  metrics: AnalyticsMetric[]; status: { key: 'WAITING_FOR_HANDOVER' | 'AT_STATION'; shipments: string }[];
  volume: { date: string; shipments: string; confirmedFeeVnd: string; unpaidVnd: string }[];
  services: { code: 'ROAD_STANDARD'; shipments: string; confirmedFeeVnd: string }[];
  routes: { datasetVersion: string; origin: string; destination: string; shipments: string; confirmedFeeVnd: string }[];
  parcels: { count: string; multiParcelShipments: string; weight: { key: string; parcels: string }[] };
  shipments: { total: string; page: number; size: 20; hasNext: boolean; items: { id: string; code: string; bookedAt: string; status: 'WAITING_FOR_HANDOVER' | 'AT_STATION'; service: 'ROAD_STANDARD'; origin: string; destination: string; parcels: string; weightGrams: string; confirmedFeeVnd: string; unpaidVnd: string }[] };
};
type Obj = Record<string, unknown>;
function check(value: unknown): asserts value { if (!value) throw new Error('ANALYTICS_RESPONSE_INVALID'); }
function object(v: unknown, fields: string[]): asserts v is Obj { check(v !== null && typeof v === 'object' && !Array.isArray(v) && Object.keys(v).sort().join() === fields.sort().join()); }
function integer(v: unknown): asserts v is string { check(typeof v === 'string' && /^(0|[1-9][0-9]{0,39})$/.test(v)); }
function date(v: unknown): asserts v is string { check(typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v) && Number.isFinite(Date.parse(v)) && new Date(v).toISOString().slice(0, 10) === v); }
function instant(v: unknown): asserts v is string { check(typeof v === 'string' && v.endsWith('Z') && Number.isFinite(Date.parse(v))); }
function list(v: unknown, max: number): asserts v is unknown[] { check(Array.isArray(v) && v.length <= max); }
function text(v: unknown, max = 64): asserts v is string { check(typeof v === 'string' && v.length > 0 && v.length <= max && !/[\u0000-\u001f\u007f]/.test(v)); }
const sum = (values: string[]) => values.reduce((n, v) => n + BigInt(v), 0n);
const status = (v: unknown) => v === 'WAITING_FOR_HANDOVER' || v === 'AT_STATION';
const route = (a: unknown, b: unknown) => ['SIM-A1/SIM-A2', 'SIM-A2/SIM-A1', 'SIM-A1/SIM-B1', 'SIM-B1/SIM-A1'].includes(a + '/' + b);

/** Reject malformed/cross-context payloads before components can treat them as valid or zero. */
export function parseAnalytics(v: unknown): AnalyticsReport {
  object(v, ['schemaVersion', 'context', 'timeZone', 'generatedAt', 'period', 'metrics', 'status', 'volume', 'services', 'routes', 'parcels', 'shipments']);
  check(v.schemaVersion === 'PERSONAL_ANALYTICS_1' && v.context === 'PERSONAL' && v.timeZone === 'Asia/Ho_Chi_Minh'); instant(v.generatedAt);
  object(v.period, ['startInclusive', 'endExclusive', 'previousStartInclusive', 'previousEndExclusive']); instant(v.period.startInclusive); instant(v.period.endExclusive);
  check(Date.parse(v.period.startInclusive) <= Date.parse(v.period.endExclusive));
  for (const key of ['previousStartInclusive', 'previousEndExclusive']) if (v.period[key] !== null) instant(v.period[key]);
  check((v.period.previousStartInclusive === null) === (v.period.previousEndExclusive === null));
  list(v.metrics, 6); check(v.metrics.length === 6); const ids = new Set<string>();
  for (const m of v.metrics) {
    object(m, ['id', 'unit', 'value', 'previousValue', 'changePercent', 'points']); text(m.id); check(Object.hasOwn(metricDefinitions, m.id) && !ids.has(m.id)); ids.add(m.id);
    check(m.unit === metricDefinitions[m.id as MetricId].unit); integer(m.value); if (m.previousValue !== null) integer(m.previousValue);
    check(m.changePercent === null || typeof m.changePercent === 'string' && /^-?\d{1,40}\.\d$/.test(m.changePercent));
    if (m.previousValue === null || m.previousValue === '0') check(m.changePercent === null);
    list(m.points, 366); const dates = new Set<string>();
    for (const p of m.points) { object(p, ['date', 'value']); date(p.date); integer(p.value); check(!dates.has(p.date)); dates.add(p.date); }
    check(sum(m.points.map(p => (p as Obj).value as string)) === BigInt(m.value));
  }
  list(v.status, 2); check(v.status.length === 2); const statuses = new Set();
  for (const s of v.status) { object(s, ['key', 'shipments']); check(status(s.key) && !statuses.has(s.key)); statuses.add(s.key); integer(s.shipments); }
  list(v.volume, 366); for (const p of v.volume) { object(p, ['date', 'shipments', 'confirmedFeeVnd', 'unpaidVnd']); date(p.date); integer(p.shipments); integer(p.confirmedFeeVnd); integer(p.unpaidVnd); }
  list(v.services, 1); for (const s of v.services) { object(s, ['code', 'shipments', 'confirmedFeeVnd']); check(s.code === 'ROAD_STANDARD'); integer(s.shipments); integer(s.confirmedFeeVnd); }
  list(v.routes, 4); const routes = new Set(); for (const r of v.routes) { object(r, ['datasetVersion', 'origin', 'destination', 'shipments', 'confirmedFeeVnd']); check(r.datasetVersion === 'ARGO-SIM-COVERAGE-1' && route(r.origin, r.destination) && !routes.has(r.origin + '/' + r.destination)); routes.add(r.origin + '/' + r.destination); integer(r.shipments); integer(r.confirmedFeeVnd); }
  object(v.parcels, ['count', 'multiParcelShipments', 'weight']); integer(v.parcels.count); integer(v.parcels.multiParcelShipments); list(v.parcels.weight, 3); check(v.parcels.weight.length === 3);
  const weights = new Set(); for (const w of v.parcels.weight) { object(w, ['key', 'parcels']); check(['UP_TO_500G', 'UP_TO_5KG', 'OVER_5KG'].includes(String(w.key)) && !weights.has(w.key)); weights.add(w.key); integer(w.parcels); }
  object(v.shipments, ['total', 'page', 'size', 'hasNext', 'items']); integer(v.shipments.total); check(Number.isInteger(v.shipments.page) && Number(v.shipments.page) >= 0 && Number(v.shipments.page) <= 500 && v.shipments.size === 20 && typeof v.shipments.hasNext === 'boolean'); list(v.shipments.items, 20);
  const shipments = new Set(); for (const s of v.shipments.items) { object(s, ['id', 'code', 'bookedAt', 'status', 'service', 'origin', 'destination', 'parcels', 'weightGrams', 'confirmedFeeVnd', 'unpaidVnd']); check(typeof s.id === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(s.id) && !shipments.has(s.id)); shipments.add(s.id); text(s.code); check(/^AP-SIM-[a-f0-9]{32}$/.test(s.code)); instant(s.bookedAt); check(status(s.status) && s.service === 'ROAD_STANDARD' && route(s.origin, s.destination)); for (const field of ['parcels', 'weightGrams', 'confirmedFeeVnd', 'unpaidVnd']) integer(s[field]); check(BigInt(String(s.parcels)) >= 1n && BigInt(String(s.parcels)) <= 5n); }
  const report = v as unknown as AnalyticsReport, metric = (id: MetricId) => BigInt(report.metrics.find(m => m.id === id)!.value), total = BigInt(report.shipments.total);
  check(total === metric('BOOKED_SHIPMENTS') && sum(report.status.map(s => s.shipments)) === total && sum(report.routes.map(r => r.shipments)) === total && sum(report.services.map(s => s.shipments)) === total && sum(report.volume.map(p => p.shipments)) === total);
  check(metric('CREATED_PARCELS') === BigInt(report.parcels.count) && sum(report.parcels.weight.map(w => w.parcels)) === metric('CREATED_PARCELS'));
  check(sum(report.volume.map(p => p.confirmedFeeVnd)) === metric('CONFIRMED_FEES') && sum(report.volume.map(p => p.unpaidVnd)) === metric('UNPAID_OBLIGATIONS'));
  check(report.shipments.hasNext === (BigInt(report.shipments.page * 20 + report.shipments.items.length) < total));
  return report;
}
