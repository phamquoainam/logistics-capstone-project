export const claimCategories = { LOSS: 'Nghi mất hàng', DAMAGE: 'Hàng hư hỏng', DELAY: 'Giao chậm', SERVICE_DISPUTE: 'Tranh chấp dịch vụ' } as const;
export const claimStates = { RECEIVED: 'Đã tiếp nhận hồ sơ', UNDER_REVIEW: 'Đang xem xét hồ sơ', NEEDS_INFORMATION: 'Cần bổ sung', CLOSED_WITHOUT_DECISION: 'Đã đóng hồ sơ' } as const;
export type ClaimSummary = { id: string; organizationId: string; facilityId: string | null; ticketId: string; shipmentId: string; creatorAccountId: string; category: keyof typeof claimCategories; state: keyof typeof claimStates; version: number; createdAt: string; updatedAt: string; eligibility: 'UNASSESSED'; compensation: 'BLOCKED' };
export type ClaimDetail = { intake: ClaimSummary; events: { id: string; authorKind: 'MERCHANT' | 'SUPPORT'; operation: string; message: string; version: number; occurredAt: string }[]; allowedActions: string[] };
function check(value: unknown): asserts value { if (!value) throw new Error('CLAIM_RESPONSE_INVALID'); }
function shape(v: unknown, fields: string[]): asserts v is Record<string, unknown> { check(v && typeof v === 'object' && !Array.isArray(v) && Object.keys(v).length === fields.length && fields.every(k => Object.hasOwn(v, k))); }
function uuid(v: unknown) { check(typeof v === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000'); }
function time(v: unknown) { check(typeof v === 'string' && /Z$/.test(v) && Number.isFinite(Date.parse(v))); }
export function claimSummary(v: unknown, org: string): ClaimSummary {
  shape(v, ['id', 'organizationId', 'facilityId', 'ticketId', 'shipmentId', 'creatorAccountId', 'category', 'state', 'version', 'createdAt', 'updatedAt', 'eligibility', 'compensation']);
  for (const k of ['id', 'organizationId', 'ticketId', 'shipmentId', 'creatorAccountId']) uuid(v[k]); if (v.facilityId !== null) uuid(v.facilityId);
  check(v.organizationId === org && Object.hasOwn(claimCategories, String(v.category)) && Object.hasOwn(claimStates, String(v.state)) && Number.isSafeInteger(v.version) && Number(v.version) > 0 && v.eligibility === 'UNASSESSED' && v.compensation === 'BLOCKED');
  time(v.createdAt); time(v.updatedAt); check(Date.parse(String(v.updatedAt)) >= Date.parse(String(v.createdAt))); return v as ClaimSummary;
}
export function claimDetail(v: unknown, org: string, id?: string): ClaimDetail {
  shape(v, ['intake', 'events', 'allowedActions']); const intake = claimSummary(v.intake, org); if (id) check(intake.id === id);
  check(Array.isArray(v.events) && v.events.length === intake.version && Array.isArray(v.allowedActions) && new Set(v.allowedActions).size === v.allowedActions.length);
  for (const [i, e] of v.events.entries()) { shape(e, ['id', 'authorKind', 'operation', 'message', 'version', 'occurredAt']); uuid(e.id); check(['MERCHANT', 'SUPPORT'].includes(String(e.authorKind)) && ['CREATE', 'SUPPLEMENT', 'BEGIN_REVIEW', 'REQUEST_INFORMATION', 'CLOSE_WITHOUT_DECISION'].includes(String(e.operation)) && e.version === i + 1 && typeof e.message === 'string' && [...e.message].length <= 4000); time(e.occurredAt); }
  check(v.allowedActions.every(a => ['SUPPLEMENT', 'BEGIN_REVIEW', 'REQUEST_INFORMATION', 'CLOSE_WITHOUT_DECISION', 'VIEW_EVIDENCE', 'ATTACH_EVIDENCE'].includes(a))); return v as ClaimDetail;
}
export function claimPage(v: unknown, org: string, ticket: string | undefined, page: number) {
  shape(v, ['items', 'page', 'size', 'hasNext']); check(Array.isArray(v.items) && v.items.length <= 20 && v.page === page && v.size === 20 && typeof v.hasNext === 'boolean'); const items = v.items.map(i => claimSummary(i, org)); check((ticket === undefined || items.every(i => i.ticketId === ticket)) && new Set(items.map(i => i.id)).size === items.length); return { items, page, hasNext: v.hasNext };
}
export type ClaimSamples = { sampleCount: string; totalMs: string; minMs: string | null; maxMs: string | null };
function count(v: unknown): asserts v is string { check(typeof v === 'string' && /^(0|[1-9][0-9]*)$/.test(v)); }
function samples(v: unknown, total: string): ClaimSamples { shape(v, ['sampleCount', 'totalMs', 'minMs', 'maxMs']); count(v.sampleCount); count(v.totalMs); check(BigInt(v.sampleCount) <= BigInt(total)); if (v.sampleCount === '0') check(v.totalMs === '0' && v.minMs === null && v.maxMs === null); else { count(v.minMs); count(v.maxMs); check(BigInt(v.minMs) <= BigInt(v.maxMs) && BigInt(v.totalMs) >= BigInt(v.minMs) * BigInt(v.sampleCount) && BigInt(v.totalMs) <= BigInt(v.maxMs) * BigInt(v.sampleCount)); } return v as ClaimSamples; }
export function claimReport(v: unknown, org: string) {
  shape(v, ['schemaVersion', 'organizationId', 'generatedAt', 'total', 'byState', 'byCategory', 'cleanEvidence', 'firstStaffResponse', 'intakeClosure', 'slaState', 'compensation']); check(v.schemaVersion === 'CLAIM_INTAKE_REPORT_1' && v.organizationId === org && v.slaState === 'UNPUBLISHED' && v.compensation === 'BLOCKED'); time(v.generatedAt); count(v.total); count(v.cleanEvidence);
  const total = v.total;
  function breakdown(value: unknown, keys: string[]) { shape(value, keys); const result: Record<string, string> = {}; let sum = 0n; for (const k of keys) { count(value[k]); result[k] = value[k]; sum += BigInt(value[k]); } check(sum === BigInt(total)); return result; }
  const byState = breakdown(v.byState, Object.keys(claimStates)), byCategory = breakdown(v.byCategory, Object.keys(claimCategories)), firstStaffResponse = samples(v.firstStaffResponse, v.total), intakeClosure = samples(v.intakeClosure, v.total); check(intakeClosure.sampleCount === byState.CLOSED_WITHOUT_DECISION && BigInt(v.cleanEvidence) <= BigInt(v.total) * 10n); return { total: v.total, byState, byCategory, firstStaffResponse, intakeClosure, cleanEvidence: v.cleanEvidence, generatedAt: v.generatedAt as string };
}
export function claimNotices(v: unknown, page: number) {
  shape(v, ['items', 'page', 'hasNext']); check(Array.isArray(v.items) && v.items.length <= 20 && v.page === page && typeof v.hasNext === 'boolean');
  const items = v.items.map(item => { shape(item, ['id', 'intakeId', 'ticketId', 'eventId', 'type', 'createdAt']); for (const k of ['id', 'intakeId', 'ticketId', 'eventId']) uuid(item[k]); check(['REVIEW', 'NEEDS_INFORMATION', 'REPLY'].includes(String(item.type))); time(item.createdAt); return item as { id: string; intakeId: string; ticketId: string; eventId: string; type: string; createdAt: string }; }); check(new Set(items.map(i => i.id)).size === items.length); return { items, page, hasNext: v.hasNext };
}
