import type { BusinessShipmentView, PilotHandling } from './client';
export type Assignment = { shipmentId: string; organizationId: string; assigneeAccountId: string | null; version: number; updatedAt: string | null };
export type ManagementAction = 'READ_LABEL' | 'EXPORT' | 'EXPORT_PII' | 'ASSIGN';
export type OrganizationManagementItem = { record: BusinessShipmentView; status: 'WAITING_FOR_HANDOVER' | 'AT_STATION'; handling?: PilotHandling; source: { sourceChannel: string | null; sourceOrderReference: string | null; facilityId: string | null; importJobIds: string[] }; assignment: Assignment; allowedActions: ManagementAction[] };
export type OrganizationManagementPage = { schemaVersion: 'ORGANIZATION_MANAGEMENT_1' | 'ORGANIZATION_MANAGEMENT_2'; generatedAt: string; timeZone: 'Asia/Ho_Chi_Minh'; summary: { bookedShipments: string; waitingForHandover: string; unassigned: string; confirmedFeesVnd: string; unpaidFeesVnd: string }; total: string; page: number; size: 20; hasNext: boolean; items: OrganizationManagementItem[] };
export type OrganizationExport = { schemaVersion: 'ORGANIZATION_EXPORT_1'; exportId: string; organizationId: string; generatedAt: string; timeZone: 'Asia/Ho_Chi_Minh'; piiIncluded: boolean; rowCount: number; columns: string[]; rows: string[][] };
export type AssignmentMember = { accountId: string; membershipId: string; displayName: string; role: 'OWNER' | 'MANAGER' | 'OPERATOR' };
export type InternalHistory = { id: string; operation: 'ASSIGNED' | 'REASSIGNED' | 'UNASSIGNED'; actorAccountId: string; priorAssigneeAccountId: string | null; assigneeAccountId: string | null; priorVersion: number; newVersion: number; occurredAt: string };
export const exportColumns = ['shipmentCode', 'bookingTimeVietnam', 'reference', 'sourceChannel', 'sourceOrderReference', 'service', 'origin', 'destination', 'parcelCount', 'totalVnd', 'paymentState', 'creatorAccountId', 'facilityId', 'importJobIds', 'assigneeAccountId'];
export const exportPiiColumns = ['recipientName', 'recipientPhone', 'recipientAddress'];
function check(value: unknown): asserts value { if (!value) throw new Error('BUSINESS_MANAGEMENT_RESPONSE_INVALID'); }
function object(value: unknown, keys: string[]): asserts value is Record<string, unknown> { check(value && typeof value === 'object' && !Array.isArray(value) && Object.keys(value).length === keys.length && keys.every(k => Object.hasOwn(value, k))); }
export function managementId(value: unknown): asserts value is string { check(typeof value === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(value) && value !== '00000000-0000-0000-0000-000000000000'); }
function nullableId(value: unknown) { if (value !== null) managementId(value); }
function integer(value: unknown): asserts value is number { check(Number.isSafeInteger(value) && Number(value) >= 0); }
function instant(value: unknown): asserts value is string { check(typeof value === 'string' && /^\d{4}-\d\d-\d\dT.*Z$/.test(value) && Number.isFinite(Date.parse(value))); }
function text(value: unknown, max: number): asserts value is string { check(typeof value === 'string' && value.trim().length > 0 && value.length <= max && !/[\u0000-\u001f\u007f]/.test(value)); }
function count(value: unknown): asserts value is string { check(typeof value === 'string' && /^(0|[1-9][0-9]{0,79})$/.test(value)); }
export function parseAssignment(value: unknown, org: string, shipment: string): Assignment {
  object(value, ['shipmentId', 'organizationId', 'assigneeAccountId', 'version', 'updatedAt']); check(value.shipmentId === shipment && value.organizationId === org); managementId(value.shipmentId); nullableId(value.assigneeAccountId); integer(value.version);
  if (!value.version) check(value.assigneeAccountId === null && value.updatedAt === null); else instant(value.updatedAt);
  return value as Assignment;
}
export function parseOrganizationPage(value: unknown, org: string, page: number, record: (raw: unknown) => BusinessShipmentView, parseHandling?: (raw: unknown, shipmentId: string) => PilotHandling): OrganizationManagementPage {
  object(value, ['schemaVersion', 'generatedAt', 'timeZone', 'summary', 'total', 'page', 'size', 'hasNext', 'items']); const v2 = value.schemaVersion === 'ORGANIZATION_MANAGEMENT_2'; check((v2 || value.schemaVersion === 'ORGANIZATION_MANAGEMENT_1') && (!v2 || typeof parseHandling === 'function') && value.timeZone === 'Asia/Ho_Chi_Minh' && value.page === page && value.size === 20 && typeof value.hasNext === 'boolean'); instant(value.generatedAt); count(value.total);
  object(value.summary, ['bookedShipments', 'waitingForHandover', 'unassigned', 'confirmedFeesVnd', 'unpaidFeesVnd']); Object.values(value.summary).forEach(count);
  check(value.total === value.summary.bookedShipments && BigInt(String(value.summary.waitingForHandover)) <= BigInt(value.total) && (v2 || value.summary.bookedShipments === value.summary.waitingForHandover) && value.summary.confirmedFeesVnd === value.summary.unpaidFeesVnd && BigInt(String(value.summary.unassigned)) <= BigInt(value.total));
  check(Array.isArray(value.items) && value.items.length <= 20); const ids = new Set();
  const items = value.items.map(raw => {
    object(raw, ['record', 'status', 'source', 'assignment', 'allowedActions', ...(v2 ? ['handling'] : [])]); const data = record(raw.record), shipment = data.shipment; check((raw.status === 'WAITING_FOR_HANDOVER' || v2 && raw.status === 'AT_STATION') && shipment.organizationId === org && !ids.has(shipment.id)); ids.add(shipment.id);
    const handling = v2 ? parseHandling!(raw.handling, shipment.id) : undefined;
    if (handling) check(handling.shipmentId === shipment.id && handling.state === raw.status && handling.parcels.length === shipment.parcels.length && handling.parcels.every((p, i) => p.parcelId === shipment.parcels[i].id) && handling.events.every(e => e.stationCode === shipment.quote.route.dropOffCode));
    object(raw.source, ['sourceChannel', 'sourceOrderReference', 'facilityId', 'importJobIds']); check(raw.source.facilityId === shipment.facilityId); check((raw.source.sourceChannel === null) === (raw.source.sourceOrderReference === null)); if (raw.source.sourceChannel !== null) { text(raw.source.sourceChannel, 40); text(raw.source.sourceOrderReference, 80); }
    check(Array.isArray(raw.source.importJobIds) && raw.source.importJobIds.length <= 100 && new Set(raw.source.importJobIds).size === raw.source.importJobIds.length); raw.source.importJobIds.forEach(managementId);
    const assignment = parseAssignment(raw.assignment, org, shipment.id); check(Array.isArray(raw.allowedActions) && raw.allowedActions.length <= 4 && new Set(raw.allowedActions).size === raw.allowedActions.length && raw.allowedActions.every(v => ['READ_LABEL', 'EXPORT', 'EXPORT_PII', 'ASSIGN'].includes(v)) && (!raw.allowedActions.includes('EXPORT_PII') || raw.allowedActions.includes('EXPORT')));
    return { ...raw, record: data, assignment, ...(handling ? { handling } : {}) } as OrganizationManagementItem;
  });
  const offset = BigInt(page) * 20n, remaining = BigInt(value.total) > offset ? BigInt(value.total) - offset : 0n; check(BigInt(items.length) === (remaining > 20n ? 20n : remaining) && value.hasNext === (remaining > 20n));
  return { ...value, items } as OrganizationManagementPage;
}
export function parseOrganizationExport(value: unknown, org: string, pii: boolean): OrganizationExport {
  object(value, ['schemaVersion', 'exportId', 'organizationId', 'generatedAt', 'timeZone', 'piiIncluded', 'rowCount', 'columns', 'rows']); check(value.schemaVersion === 'ORGANIZATION_EXPORT_1' && value.organizationId === org && value.timeZone === 'Asia/Ho_Chi_Minh' && value.piiIncluded === pii); managementId(value.exportId); instant(value.generatedAt); integer(value.rowCount); check(value.rowCount <= 200);
  const expected = [...exportColumns, ...(pii ? exportPiiColumns : [])]; check(Array.isArray(value.columns) && value.columns.join('|') === expected.join('|') && Array.isArray(value.rows) && value.rows.length === value.rowCount && value.rows.every(row => Array.isArray(row) && row.length === expected.length && row.every(v => typeof v === 'string' && v.length <= 4096)));
  return value as OrganizationExport;
}
export function parseAssignmentReceipt(value: unknown) { object(value, ['targetId', 'version', 'outcome']); managementId(value.targetId); integer(value.version); check(value.version > 0 && value.outcome === 'COMMITTED'); return value as { targetId: string; version: number; outcome: 'COMMITTED' }; }
export function parseAssignmentMembers(value: unknown, page: number) { object(value, ['items', 'page', 'size', 'hasNext']); check(value.page === page && value.size === 20 && typeof value.hasNext === 'boolean' && Array.isArray(value.items) && value.items.length <= 20); const ids = new Set(); value.items.forEach(v => { object(v, ['accountId', 'membershipId', 'displayName', 'role']); managementId(v.accountId); managementId(v.membershipId); text(v.displayName, 160); check(['OWNER', 'MANAGER', 'OPERATOR'].includes(String(v.role)) && !ids.has(v.accountId)); ids.add(v.accountId); }); return value as { items: AssignmentMember[]; page: number; size: 20; hasNext: boolean }; }
export function parseInternalHistory(value: unknown, page: number) {
  object(value, ['items', 'page', 'size', 'hasNext']); check(value.page === page && value.size === 20 && typeof value.hasNext === 'boolean' && Array.isArray(value.items) && value.items.length <= 20); const ids = new Set(); let previous = Infinity;
  value.items.forEach(v => { object(v, ['id', 'operation', 'actorAccountId', 'priorAssigneeAccountId', 'assigneeAccountId', 'priorVersion', 'newVersion', 'occurredAt']); managementId(v.id); managementId(v.actorAccountId); nullableId(v.priorAssigneeAccountId); nullableId(v.assigneeAccountId); integer(v.priorVersion); integer(v.newVersion); instant(v.occurredAt); check(v.newVersion === v.priorVersion + 1 && v.newVersion < previous && !ids.has(v.id) && v.priorAssigneeAccountId !== v.assigneeAccountId); const correct = v.assigneeAccountId === null ? 'UNASSIGNED' : v.priorAssigneeAccountId === null ? 'ASSIGNED' : 'REASSIGNED'; check(v.operation === correct); ids.add(v.id); previous = v.newVersion; });
  return value as { items: InternalHistory[]; page: number; size: 20; hasNext: boolean };
}
