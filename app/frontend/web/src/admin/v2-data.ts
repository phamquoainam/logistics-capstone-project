import { AdminSessionError } from './session';
import { date, parseContext, parseDashboard, parseShipments, queryParameters } from './data';
import type { AdminContext, Query, Shipment, MetricKey } from './data';
import { governanceId } from './governance';
export const v2Media = 'application/vnd.argopost.admin-dashboard.v2+json';
export const merchantKeys = ['APPLICATIONS_SUBMITTED', 'APPLICATIONS_UNDER_REVIEW', 'APPLICATIONS_NEEDS_INFORMATION', 'APPLICATIONS_APPROVED', 'APPLICATIONS_REJECTED', 'APPLICATIONS_WITHDRAWN', 'APPLICATION_PROCESSING_TOTAL_MS', 'APPLICATION_PROCESSING_AVG_MS', 'APPLICATION_PROCESSING_MIN_MS', 'APPLICATION_PROCESSING_MAX_MS'] as const;
export type QueryV2 = Query & { applicationState: string | null; policyId: string | null };
export type MetricV2 = { key: string; value: string | null; unit: string; drilldown: { path: string; filters: Record<string, string>; metric: string } | null };
export type MetadataV2 = { metricKey: string; definitionVersion: 'ARGO_ADMIN_METRICS_2'; cohortKind: 'BOOKED_SHIPMENT' | 'FIRST_SUBMITTED_APPLICATION'; timeZone: 'Asia/Ho_Chi_Minh'; numerator: string; denominator: string | null; unit: string; sampleCount: string | null; lastSourceEventAt: string | null; sourceCheckedAt: string; freshness: 'CURRENT_TRANSACTION'; availabilityReason: string | null };
export type SectionV2 = { key: string; availability: string; reasonCode: string; metrics: MetricV2[]; series: { from: string; to: string; metric: string; value: string }[] };
export type DashboardV2 = { schemaVersion: 'ADMIN_DASHBOARD_2'; query: QueryV2; generatedAt: string; snapshotId: string; sections: SectionV2[]; resourceScopeVersion: string; metricMetadata: MetadataV2[] };
export type ApplicationPageV2 = { schemaVersion: 'ADMIN_DASHBOARD_2'; items: { id: string; state: string; submittedAt: string; updatedAt: string }[]; page: number; size: number; hasNext: boolean; snapshotId: string; resourceScopeVersion: string; metricMetadata: MetadataV2[] };
function bad(): never { throw new AdminSessionError('RESPONSE_INVALID'); }
const shape = (v: unknown, keys: string[]) => { if (!v || typeof v !== 'object' || Array.isArray(v) || Object.keys(v).length !== keys.length || keys.some(k => !Object.hasOwn(v, k))) bad();return v as Record<string, unknown>; };
const integer = (v: unknown): v is string => typeof v === 'string' && /^(0|[1-9][0-9]{0,99})$/.test(v);
const instant = (v: unknown): v is string => typeof v === 'string' && v.endsWith('Z') && Number.isFinite(Date.parse(v));
const hash = (v: unknown) => typeof v === 'string' && /^[a-f0-9]{64}$/.test(v);
const states = ['SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED', 'WITHDRAWN'];
export function parametersV2(q: QueryV2) { const p = queryParameters(q);if (q.applicationState) p.set('applicationState', q.applicationState);if (q.policyId) p.set('policyId', q.policyId);return p; }
export function parseContextV2(value: unknown, expectedAccount: string): AdminContext & { resourceScopeVersion: string; resourceScopes: { kind: string; id: string; capabilities: string[]; version: string }[] } {
  const v = shape(value, ['schemaVersion', 'accountId', 'displayName', 'permissions', 'environment', 'timeZone', 'regionFilterAvailable', 'resourceScopeVersion', 'resourceScopes']);
  if (v.schemaVersion !== 'ADMIN_DASHBOARD_2' || !hash(v.resourceScopeVersion) || !Array.isArray(v.resourceScopes) || v.resourceScopes.length > 100) bad();
  const current = parseContext(Object.fromEntries(['accountId', 'displayName', 'permissions', 'environment', 'timeZone', 'regionFilterAvailable'].map(k => [k, v[k]])), expectedAccount), seen = new Set<string>();
  const capabilities = ['ADMIN_SHIPMENT_READ', 'ADMIN_FINANCE_READ', 'ADMIN_TECHNICAL_READ', 'application.read', 'application.evidence.read', 'org.read', 'audit.read', 'task.read'];
  for (const item of v.resourceScopes as unknown[]) { const s = shape(item, ['kind', 'id', 'capabilities', 'version']);if (!['REALM_BOUND_LOCAL', 'MERCHANT_POLICY', 'MERCHANT_ORGANIZATION'].includes(String(s.kind)) || !governanceId(s.id) || !hash(s.version) || !Array.isArray(s.capabilities) || !s.capabilities.length || new Set(s.capabilities).size !== s.capabilities.length || s.capabilities.some(c => !capabilities.includes(String(c))) || seen.has(s.kind + ':' + s.id)) bad();seen.add(s.kind + ':' + s.id); }
  return { ...current, resourceScopeVersion: v.resourceScopeVersion as string, resourceScopes: v.resourceScopes as { kind: string; id: string; capabilities: string[]; version: string }[] };
}
function metadata(value: unknown, metrics: MetricV2[], generatedAt?: string): MetadataV2[] {
  if (!Array.isArray(value) || value.length > 32 || value.length !== metrics.length) bad();const seen = new Set<string>();
  for (const item of value as unknown[]) {
    const m = shape(item, ['metricKey', 'definitionVersion', 'cohortKind', 'timeZone', 'numerator', 'denominator', 'unit', 'sampleCount', 'lastSourceEventAt', 'sourceCheckedAt', 'freshness', 'availabilityReason']), metric = metrics.find(v => v.key === m.metricKey);
    if (!metric || seen.has(metric.key) || m.definitionVersion !== 'ARGO_ADMIN_METRICS_2' || m.timeZone !== 'Asia/Ho_Chi_Minh' || m.cohortKind !== (metric.key.startsWith('APPLICATION') ? 'FIRST_SUBMITTED_APPLICATION' : 'BOOKED_SHIPMENT') || !integer(m.numerator) || m.denominator !== null && !integer(m.denominator) || m.sampleCount !== null && !integer(m.sampleCount) || m.unit !== metric.unit || !instant(m.sourceCheckedAt) || generatedAt && m.sourceCheckedAt !== generatedAt || m.lastSourceEventAt !== null && (!instant(m.lastSourceEventAt) || Date.parse(m.lastSourceEventAt) > Date.parse(m.sourceCheckedAt as string)) || m.freshness !== 'CURRENT_TRANSACTION' || m.availabilityReason !== null) bad();
    if (metric.key === 'APPLICATION_PROCESSING_AVG_MS') { if (!integer(m.denominator) || m.sampleCount !== m.denominator || (m.denominator === '0' ? metric.value !== null || m.numerator !== '0' : metric.value !== (BigInt(m.numerator as string) / BigInt(m.denominator as string)).toString())) bad(); }
    else if (m.denominator !== null || m.numerator !== metric.value) bad();
    if (metric.key.startsWith('APPLICATION_PROCESSING_') ? !integer(m.sampleCount) : m.sampleCount !== null) bad();seen.add(metric.key);
  }
  return value as MetadataV2[];
}
export function parseDashboardV2(value: unknown, context: AdminContext, expected: QueryV2): DashboardV2 {
  const v = shape(value, ['schemaVersion', 'query', 'generatedAt', 'snapshotId', 'sections', 'resourceScopeVersion', 'metricMetadata']), q = shape(v.query, ['from', 'to', 'grain', 'shipmentStatus', 'regionId', 'applicationState', 'policyId']);
  if (v.schemaVersion !== 'ADMIN_DASHBOARD_2' || !instant(v.generatedAt) || !hash(v.resourceScopeVersion) || typeof v.snapshotId !== 'string' || v.snapshotId.length > 4096 || !/^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]{43}$/.test(v.snapshotId) || !Array.isArray(v.sections) || v.sections.length > 6 || Object.keys(q).some(k => q[k] !== expected[k as keyof QueryV2])) bad();
  if (q.applicationState !== null && !states.includes(String(q.applicationState)) || q.policyId !== null && !governanceId(q.policyId) || q.shipmentStatus !== null && (q.applicationState !== null || q.policyId !== null)) bad();
  const all: MetricV2[] = [], legacy: unknown[] = [], names = new Set<string>();
  for (const item of v.sections as unknown[]) {
    const s = shape(item, ['key', 'availability', 'reasonCode', 'metrics', 'series']);if (typeof s.key !== 'string' || names.has(s.key) || !Array.isArray(s.metrics) || !Array.isArray(s.series)) bad();names.add(s.key);
    if (s.key !== 'MERCHANT') {
      const copy = { ...s, metrics: (s.metrics as unknown[]).map(raw => { const m = shape(raw, ['key', 'value', 'unit', 'drilldown']);if (m.drilldown !== null) { const d = shape(m.drilldown, ['path', 'filters', 'metric']);const filters = shape(d.filters, [...parametersV2(expected).keys()]);for (const [key, val] of parametersV2(expected)) if (filters[key] !== val) bad();return { ...m, drilldown: { ...d, filters: Object.fromEntries(queryParameters(expected)) } }; }return m; }) };legacy.push(copy);all.push(...s.metrics as MetricV2[]);continue;
    }
    if (!context.permissions.includes('ADMIN_MERCHANT_READ') || !['AVAILABLE', 'POLICY_NOT_CONFIGURED', 'SOURCE_NOT_IMPLEMENTED'].includes(String(s.availability)) || typeof s.reasonCode !== 'string' || !/^[A-Z0-9_]{1,100}$/.test(s.reasonCode)) bad();
    legacy.push({ key: 'MERCHANT', availability: 'SOURCE_NOT_IMPLEMENTED', reasonCode: 'SUBMITTED_APPLICATION_SOURCE_REQUIRED', metrics: [], series: [] });
    if (s.availability !== 'AVAILABLE') { if (s.metrics.length || s.series.length) bad();continue; }
    const keys = new Set<string>();for (const raw of s.metrics as unknown[]) {
      const m = shape(raw, ['key', 'value', 'unit', 'drilldown']);if (!merchantKeys.includes(m.key as typeof merchantKeys[number]) || keys.has(String(m.key)) || (m.value === null ? m.key !== 'APPLICATION_PROCESSING_AVG_MS' : !integer(m.value)) || m.unit !== (String(m.key).startsWith('APPLICATION_PROCESSING_') ? 'MILLISECOND' : 'APPLICATION')) bad();keys.add(String(m.key));
      if (m.drilldown !== null) { const d = shape(m.drilldown, ['path', 'filters', 'metric']);if (d.path !== '/api/v1/admin/dashboard/merchant-applications' || d.metric !== m.key || m.value === null) bad();const f = shape(d.filters, [...parametersV2(expected).keys()]);for (const [key, val] of parametersV2(expected)) if (f[key] !== val) bad(); }
      all.push(m as MetricV2);
    }
    for (const key of merchantKeys.slice(0, 8)) if (!keys.has(key)) bad();const values = new Map((s.metrics as MetricV2[]).map(m => [m.key, m.value]));let statesTotal = 0n;for (const key of merchantKeys.slice(1, 6)) statesTotal += BigInt(values.get(key)!);if (statesTotal > BigInt(values.get('APPLICATIONS_SUBMITTED')!)) bad();
    const seriesKeys = merchantKeys.slice(0, 6);for (const key of seriesKeys) { const points = (s.series as unknown[]).map(raw => shape(raw, ['from', 'to', 'metric', 'value'])).filter(p => p.metric === key);if (!points.length || points.length > 366) bad();let cursor = expected.from, sum = 0n;for (const point of points) { if (point.from !== cursor || !date(point.to) || point.to <= cursor || !integer(point.value)) bad();const end = new Date(cursor + 'T00:00:00Z');if (expected.grain === 'MONTH') end.setUTCMonth(end.getUTCMonth() + 1, 1);else end.setUTCDate(end.getUTCDate() + (expected.grain === 'WEEK' ? (8 - end.getUTCDay()) % 7 || 7 : 1));const until = new Date(Date.parse(expected.to) + 86400000).toISOString().slice(0, 10);if (point.to !== (end.toISOString().slice(0, 10) < until ? end.toISOString().slice(0, 10) : until)) bad();sum += BigInt(point.value);cursor = point.to; }if (cursor !== new Date(Date.parse(expected.to) + 86400000).toISOString().slice(0, 10) || sum.toString() !== values.get(key)) bad(); }if (s.series.length !== (s.series as { metric: string }[]).filter(p => seriesKeys.includes(p.metric as typeof seriesKeys[number])).length) bad();
  }
  parseDashboard({ query: { from: q.from, to: q.to, grain: q.grain, shipmentStatus: q.shipmentStatus, regionId: q.regionId }, generatedAt: v.generatedAt, snapshotId: v.snapshotId, sections: legacy }, context, expected);
  const meta = metadata(v.metricMetadata, all, v.generatedAt as string);const processing = meta.find(m => m.metricKey === 'APPLICATION_PROCESSING_AVG_MS');if (processing) { const terminal = all.filter(m => ['APPLICATIONS_APPROVED', 'APPLICATIONS_REJECTED'].includes(m.key)).reduce((n, m) => n + BigInt(m.value!), 0n);if (processing.sampleCount !== terminal.toString()) bad();if (terminal === 0n && all.some(m => ['APPLICATION_PROCESSING_MIN_MS', 'APPLICATION_PROCESSING_MAX_MS'].includes(m.key))) bad();if (terminal > 0n) { const min = all.find(m => m.key === 'APPLICATION_PROCESSING_MIN_MS'), max = all.find(m => m.key === 'APPLICATION_PROCESSING_MAX_MS'), avg = all.find(m => m.key === 'APPLICATION_PROCESSING_AVG_MS');if (!min || !max || !avg || BigInt(min.value!) > BigInt(avg.value!) || BigInt(avg.value!) > BigInt(max.value!)) bad(); } }
  return v as DashboardV2;
}
export function parseApplicationsV2(value: unknown, report: DashboardV2, metric: string, page: number): ApplicationPageV2 {
  const v = shape(value, ['schemaVersion', 'items', 'page', 'size', 'hasNext', 'snapshotId', 'resourceScopeVersion', 'metricMetadata']);if (v.schemaVersion !== 'ADMIN_DASHBOARD_2' || v.snapshotId !== report.snapshotId || v.resourceScopeVersion !== report.resourceScopeVersion || v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const ids = new Set<string>();for (const item of v.items as unknown[]) { const r = shape(item, ['id', 'state', 'submittedAt', 'updatedAt']);if (!governanceId(r.id) || ids.has(r.id) || !states.includes(String(r.state)) || !instant(r.submittedAt) || !instant(r.updatedAt) || metric.startsWith('APPLICATIONS_') && metric !== 'APPLICATIONS_SUBMITTED' && r.state !== metric.substring(13) || report.query.applicationState && r.state !== report.query.applicationState) bad();ids.add(r.id); }
  const selected = report.sections.flatMap(s => s.metrics).find(m => m.key === metric);if (!selected) bad();const current = metadata(v.metricMetadata, [selected]);const original = report.metricMetadata.find(m => m.metricKey === metric);if (!original || current[0].numerator !== original.numerator || current[0].denominator !== original.denominator || current[0].sampleCount !== original.sampleCount) bad();return v as ApplicationPageV2;
}

export function parseShipmentPageV2(value: unknown, report: DashboardV2, metric: string, page: number): Omit<ApplicationPageV2, 'items'> & { items: Shipment[] } {
  const v = shape(value, ['schemaVersion', 'items', 'page', 'size', 'hasNext', 'snapshotId', 'resourceScopeVersion', 'metricMetadata']);
  if (v.schemaVersion !== 'ADMIN_DASHBOARD_2' || v.resourceScopeVersion !== report.resourceScopeVersion || !['BOOKINGS', 'WAITING_HANDOVER', 'ACCEPTED', 'PARCELS', 'CONFIRMED_FEES', 'UNPAID_FEES'].includes(metric)) bad();
  const original = report.sections.flatMap(s => s.metrics).find(m => m.key === metric);if (!original) bad();metadata(v.metricMetadata, [original]);
  parseShipments({ items: v.items, page: v.page, size: v.size, hasNext: v.hasNext, snapshotId: v.snapshotId }, { query: report.query, generatedAt: report.generatedAt, snapshotId: report.snapshotId, sections: [] }, metric as MetricKey, page, 20);
  return v as Omit<ApplicationPageV2, 'items'> & { items: Shipment[] };
}
