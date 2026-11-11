export const businessPermissions = ['dashboard.read', 'dashboard.finance.read', 'dashboard.staff.read', 'shipment.draft.read', 'shipment.draft.write', 'shipment.import', 'shipment.template.manage', 'shipment.prepare', 'shipment.quote', 'shipment.book', 'shipment.label.read', 'facility.manage', 'defaults.manage'] as const;
export type BusinessPermission = typeof businessPermissions[number];
export type BusinessContext = { organizationId: string; displayName: string; membershipId: string; role: 'OWNER' | 'MANAGER' | 'OPERATOR'; permissions: BusinessPermission[]; authorityVersion: number };
export const businessMetrics = {
  BOOKED_SHIPMENTS: ['Vận đơn', 'SHIPMENT'], AWAITING_HANDOVER: ['Chờ bàn giao', 'SHIPMENT'], ACCEPTED_SHIPMENTS: ['Đã tiếp nhận', 'SHIPMENT'],
  IN_TRANSIT: ['Đang vận chuyển', 'SHIPMENT'], DELIVERED: ['Giao thành công', 'SHIPMENT'], DELIVERY_FAILED: ['Giao thất bại', 'SHIPMENT'], RETURNED: ['Hoàn hàng', 'SHIPMENT'], ACTION_REQUIRED: ['Cần xử lý', 'SHIPMENT'],
  PARCELS: ['Kiện hàng', 'PARCEL'], ACTUAL_WEIGHT_GRAMS: ['Trọng lượng thực', 'GRAM'],
  CONFIRMED_FEES: ['Cước xác nhận', 'VND'], OUTSTANDING_FEES: ['Cước chưa thanh toán', 'VND'], COST_PER_SHIPMENT: ['Chi phí mỗi vận đơn', 'VND_PER_SHIPMENT'],
  COD_EXPECTED: ['COD dự kiến', 'VND'], COD_COLLECTED: ['COD đã thu', 'VND'], COD_RECONCILED: ['COD đã đối soát', 'VND'], COD_REMITTED: ['COD đã chuyển trả', 'VND'],
  DELIVERY_SUCCESS_RATE: ['Tỷ lệ giao thành công', 'PERCENT'], RETURN_RATE: ['Tỷ lệ hoàn', 'PERCENT'], PICKUP_SUCCESS_RATE: ['Tỷ lệ lấy hàng thành công', 'PERCENT'], BOOKING_TO_ACCEPTANCE_MS: ['Booking đến tiếp nhận', 'MILLISECOND'],
} as const;
export type BusinessMetricId = keyof typeof businessMetrics;
export const businessChartTitles = { VOLUME_TREND: 'Sản lượng gửi hàng', STATUS_DISTRIBUTION: 'Trạng thái vận đơn', SERVICE_BREAKDOWN: 'Dịch vụ vận chuyển', ROUTE_BREAKDOWN: 'Tuyến gửi hàng', FEE_TREND: 'Chi phí vận chuyển', FACILITY_BREAKDOWN: 'Kho & Điểm lấy', STAFF_BREAKDOWN: 'Hoạt động nhân viên', CHANNEL_BREAKDOWN: 'Cửa hàng & Kênh bán' } as const;
type Metric = { id: BusinessMetricId; unit: string; state: 'SOURCE_NOT_CONNECTED'; value: null; points: [] };
type Chart = { id: keyof typeof businessChartTitles; state: 'SOURCE_NOT_CONNECTED'; series: [] };
export type BusinessReport = { schemaVersion: 'BUSINESS_DASHBOARD_V1'; context: Pick<BusinessContext, 'organizationId' | 'displayName' | 'membershipId' | 'authorityVersion'>; generatedAt: string; timeZone: 'Asia/Ho_Chi_Minh'; period: { from: string; to: string; startInclusive: string; endExclusive: string; interval: 'DAY' | 'WEEK' | 'MONTH' }; capabilities: { id: string; state: string; reasonCode: string }[]; metrics: Metric[]; charts: Chart[]; recentShipments: { state: 'SOURCE_NOT_CONNECTED'; items: [] }; attention: { state: 'SOURCE_NOT_CONNECTED'; items: [] }; quickActions: { id: string; state: 'NOT_SUPPORTED'; reasonCode: 'MERCHANT_WORKFLOW_REQUIRED' }[] };
const uuid = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/;
function check(ok: unknown): asserts ok { if (!ok) throw new Error('BUSINESS_RESPONSE_INVALID'); }
function object(v: unknown, keys: string[]): asserts v is Record<string, unknown> { check(v && typeof v === 'object' && !Array.isArray(v) && Object.keys(v).length === keys.length && keys.every(k => Object.hasOwn(v, k))); }
function id(v: unknown): asserts v is string { check(typeof v === 'string' && uuid.test(v) && v !== '00000000-0000-0000-0000-000000000000'); }
function name(v: unknown): asserts v is string { check(typeof v === 'string' && v.trim().length > 0 && [...v].length <= 160 && !/[\u0000-\u001f\u007f]/u.test(v)); }
function positive(v: unknown) { check(Number.isSafeInteger(v) && Number(v) > 0); }
function date(v: unknown): asserts v is string { check(typeof v === 'string' && /^(?:20\d{2}|2100)-\d{2}-\d{2}$/.test(v) && new Date(v).toISOString().slice(0, 10) === v); }
function instant(v: unknown): asserts v is string { check(typeof v === 'string' && /^\d{4}-\d{2}-\d{2}T.*Z$/.test(v) && Number.isFinite(Date.parse(v))); }
function array(v: unknown): asserts v is unknown[] { check(Array.isArray(v) && v.length <= 100); }
const financial = (key: string) => ['CONFIRMED_FEES', 'OUTSTANDING_FEES', 'COST_PER_SHIPMENT', 'COD_EXPECTED', 'COD_COLLECTED', 'COD_RECONCILED', 'COD_REMITTED'].includes(key);
export function parseBusinessContexts(value: unknown): BusinessContext[] {
  object(value, ['schemaVersion', 'items']); check(value.schemaVersion === 'BUSINESS_CONTEXTS_V1'); array(value.items);
  const organizations = new Set(), memberships = new Set();
  for (const row of value.items) {
    object(row, ['organizationId', 'displayName', 'membershipId', 'role', 'permissions', 'authorityVersion']); id(row.organizationId); id(row.membershipId); name(row.displayName); positive(row.authorityVersion);
    check(!organizations.has(row.organizationId) && !memberships.has(row.membershipId)); organizations.add(row.organizationId); memberships.add(row.membershipId);
    check(['OWNER', 'MANAGER', 'OPERATOR'].includes(String(row.role))); array(row.permissions); check(row.permissions.includes('dashboard.read') && new Set(row.permissions).size === row.permissions.length && row.permissions.every(p => businessPermissions.includes(p as BusinessPermission)));
  }
  return value.items as BusinessContext[];
}
export function parseBusinessReport(value: unknown, expected: BusinessContext, query: URLSearchParams): BusinessReport {
  object(value, ['schemaVersion', 'context', 'generatedAt', 'timeZone', 'period', 'capabilities', 'metrics', 'charts', 'recentShipments', 'attention', 'quickActions']);
  check(value.schemaVersion === 'BUSINESS_DASHBOARD_V1' && value.timeZone === 'Asia/Ho_Chi_Minh'); instant(value.generatedAt);
  object(value.context, ['organizationId', 'displayName', 'membershipId', 'authorityVersion']); check(value.context.organizationId === expected.organizationId && value.context.membershipId === expected.membershipId && value.context.authorityVersion === expected.authorityVersion && value.context.displayName === expected.displayName);
  object(value.period, ['from', 'to', 'startInclusive', 'endExclusive', 'interval']); date(value.period.from); date(value.period.to); instant(value.period.startInclusive); instant(value.period.endExclusive);
  check(value.period.to >= value.period.from && ['DAY', 'WEEK', 'MONTH'].includes(String(value.period.interval)));
  check(Date.parse(value.period.startInclusive) === Date.parse(value.period.from + 'T00:00:00+07:00') && Date.parse(value.period.endExclusive) === Date.parse(value.period.to + 'T00:00:00+07:00') + 86400000 && Date.parse(value.period.endExclusive) - Date.parse(value.period.startInclusive) <= 366 * 86400000);
  for (const key of ['from', 'to', 'interval']) if (query.has(key)) check(value.period[key] === query.get(key));
  const finance = expected.permissions.includes('dashboard.finance.read'), staff = expected.permissions.includes('dashboard.staff.read');
  array(value.metrics); const wantedMetrics = Object.keys(businessMetrics).filter(k => finance || !financial(k)); check(value.metrics.length === wantedMetrics.length);
  const metricIds = new Set(); for (const m of value.metrics) {
    object(m, ['id', 'unit', 'state', 'value', 'points']); check(wantedMetrics.includes(String(m.id)) && !metricIds.has(m.id)); metricIds.add(m.id);
    check(m.unit === businessMetrics[m.id as BusinessMetricId][1] && m.state === 'SOURCE_NOT_CONNECTED' && m.value === null); array(m.points); check(m.points.length === 0);
  }
  const wantedCharts = Object.keys(businessChartTitles).filter(k => (finance || k !== 'FEE_TREND') && (staff || k !== 'STAFF_BREAKDOWN'));
  array(value.charts); check(value.charts.length === wantedCharts.length); const charts = new Set();
  for (const c of value.charts) { object(c, ['id', 'state', 'series']); check(wantedCharts.includes(String(c.id)) && !charts.has(c.id) && c.state === 'SOURCE_NOT_CONNECTED'); charts.add(c.id); array(c.series); check(c.series.length === 0); }
  array(value.capabilities); check(value.capabilities.length === (finance ? 2 : 1)); const capabilities = new Set();
  for (const c of value.capabilities) { object(c, ['id', 'state', 'reasonCode']); check(['SHIPPING', ...(finance ? ['FINANCE'] : [])].includes(String(c.id)) && !capabilities.has(c.id) && c.state === 'SOURCE_NOT_CONNECTED' && c.reasonCode === `MERCHANT_${String(c.id)}_SOURCE_REQUIRED`); capabilities.add(c.id); }
  for (const key of ['recentShipments', 'attention']) { object(value[key], ['state', 'items']); check(value[key].state === 'SOURCE_NOT_CONNECTED'); array(value[key].items); check(value[key].items.length === 0); }
  array(value.quickActions); check(value.quickActions.length === 6); const actions = new Set();
  for (const a of value.quickActions) { object(a, ['id', 'state', 'reasonCode']); check(['CREATE', 'IMPORT', 'LABELS', 'MANIFEST', 'PICKUP', 'ACTION_CENTER'].includes(String(a.id)) && !actions.has(a.id) && a.state === 'NOT_SUPPORTED' && a.reasonCode === 'MERCHANT_WORKFLOW_REQUIRED'); actions.add(a.id); }
  return value as unknown as BusinessReport;
}
