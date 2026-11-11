export const ticketStates = { NEW: 'Mới', IN_PROGRESS: 'Đang xử lý', NEEDS_INFORMATION: 'Cần bổ sung', RESOLVED: 'Đã giải quyết', CLOSED: 'Đã đóng' } as const;
export const ticketCategories = { ACCOUNT_HELP: 'Tài khoản', SHIPMENT_TRACE: 'Tra soát hành trình', FEE_REVIEW: 'Kiểm tra cước', OTHER: 'Hỗ trợ khác' } as const;
export type TicketState = keyof typeof ticketStates;
export type TicketCategory = keyof typeof ticketCategories;
export type SupportCreate = { kind: 'GENERAL' | 'SHIPMENT'; shipmentId?: string; category: TicketCategory; subject: string; message: string };
export type SupportSummary = { id: string; code: string; kind: 'GENERAL' | 'SHIPMENT'; shipmentId: string | null; category: TicketCategory; subject: string; state: TicketState; version: number; createdAt: string; updatedAt: string; priority: 'NORMAL'; responseDueAt: null; teamLabel: string | null };
export type SupportMessage = { id: string; authorKind: 'CUSTOMER' | 'SUPPORT'; message: string; createdAt: string; eventType: string };
export type SupportDetail = { ticket: SupportSummary; publicMessages: SupportMessage[]; allowedActions: string[] };
export type SupportPage = { items: SupportSummary[]; page: number; size: 20; hasNext: boolean };
export type SupportReceipt = { operationId: string; targetId: string; outcome: 'COMMITTED'; version: number };
export type SupportOperation = 'CREATE' | 'REPLY' | 'REQUEST_REVIEW' | 'ACKNOWLEDGE';
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
const obj = (v: unknown): v is Record<string, unknown> => typeof v === 'object' && v !== null && !Array.isArray(v);
const shape = (v: Record<string, unknown>, names: string[]) => names.length === Object.keys(v).length && names.every(k => Object.hasOwn(v, k));
const text = (v: unknown, max: number, multiline = false): v is string => typeof v === 'string' && v.trim().length > 0 && [...v].length <= max && !(multiline ? /[\u0000-\u0008\u000b-\u001f\u007f]/u : /[\u0000-\u001f\u007f]/u).test(v);
const time = (v: unknown): v is string => typeof v === 'string' && /^\d{4}-\d\d-\d\dT/.test(v) && Number.isFinite(Date.parse(v));
const version = (v: unknown): v is number => Number.isSafeInteger(v) && Number(v) > 0;
function invalid(): never { throw new Error('SUPPORT_RESPONSE_INVALID'); }
export function supportSummary(value: unknown): SupportSummary {
  if (!obj(value) || !shape(value, ['id', 'code', 'kind', 'shipmentId', 'category', 'subject', 'state', 'version', 'createdAt', 'updatedAt', 'priority', 'responseDueAt', 'teamLabel']) || typeof value.id !== 'string' || !uuid.test(value.id) || value.code !== 'AP-T-' + value.id.replaceAll('-', '') || !['GENERAL', 'SHIPMENT'].includes(String(value.kind)) || !Object.hasOwn(ticketCategories, String(value.category)) || !Object.hasOwn(ticketStates, String(value.state)) || !text(value.subject, 160) || !version(value.version) || !time(value.createdAt) || !time(value.updatedAt) || Date.parse(value.updatedAt) < Date.parse(value.createdAt) || value.priority !== 'NORMAL' || value.responseDueAt !== null || value.teamLabel !== null && !text(value.teamLabel, 160)) invalid();
  if (value.kind === 'GENERAL' ? value.shipmentId !== null : typeof value.shipmentId !== 'string' || !uuid.test(value.shipmentId)) invalid();
  if (value.category === 'SHIPMENT_TRACE' && value.kind !== 'SHIPMENT') invalid();
  return value as SupportSummary;
}
export function supportDetail(value: unknown, staff = false): SupportDetail {
  if (!obj(value) || !shape(value, ['id', 'code', 'kind', 'shipmentId', 'category', 'subject', 'state', 'version', 'createdAt', 'updatedAt', 'priority', 'responseDueAt', 'teamLabel', 'publicMessages', 'allowedActions'])) invalid();
  const { publicMessages, allowedActions, ...summary } = value;
  const ticket = supportSummary(summary);
  const events = ['CREATED', 'REPLY', 'REQUEST_REVIEW', 'ACKNOWLEDGE', 'ASSIGN', 'START', 'REQUEST_INFORMATION', 'RESOLVE', 'REOPEN'];
  if (!Array.isArray(value.publicMessages) || value.publicMessages.length !== ticket.version || !Array.isArray(value.allowedActions) || new Set(value.allowedActions).size !== value.allowedActions.length) invalid();
  const ids = new Set<string>(); let last = Date.parse(ticket.createdAt);
  for (const m of value.publicMessages) {
    if (!obj(m) || !shape(m, ['id', 'authorKind', 'message', 'createdAt', 'eventType']) || typeof m.id !== 'string' || !uuid.test(m.id) || ids.has(m.id) || !text(m.message, 4000, true) || !['CUSTOMER', 'SUPPORT'].includes(String(m.authorKind)) || !events.includes(String(m.eventType)) || !time(m.createdAt) || Date.parse(m.createdAt) < last || Date.parse(m.createdAt) > Date.parse(ticket.updatedAt)) invalid();
    ids.add(m.id); last = Date.parse(m.createdAt);
  }
  const permitted = staff ? ticket.state === 'NEW' ? ['START'] : ticket.state === 'IN_PROGRESS' ? ['REQUEST_INFORMATION', 'RESOLVE'] : ticket.state === 'RESOLVED' ? ['REOPEN'] : [] : ['NEW', 'IN_PROGRESS', 'NEEDS_INFORMATION'].includes(ticket.state) ? ['REPLY'] : ticket.state === 'RESOLVED' ? ['REQUEST_REVIEW', 'ACKNOWLEDGE'] : [];
  if (!value.allowedActions.every(a => typeof a === 'string' && permitted.includes(a))) invalid();
  return { ticket, publicMessages: publicMessages as SupportMessage[], allowedActions: allowedActions as string[] };
}
export function supportPage(value: unknown, page: number): SupportPage {
  if (!obj(value) || !shape(value, ['items', 'page', 'size', 'hasNext']) || value.page !== page || value.size !== 20 || typeof value.hasNext !== 'boolean' || !Array.isArray(value.items) || value.items.length > 20 || value.hasNext && value.items.length !== 20) invalid();
  value.items.forEach(supportSummary); if (new Set((value.items as SupportSummary[]).map(v => v.id)).size !== value.items.length) invalid();
  return value as SupportPage;
}
export function supportReceipt(value: unknown, operation: SupportOperation): SupportReceipt {
  if (!obj(value) || !shape(value, ['operationId', 'targetId', 'outcome', 'version']) || value.operationId !== operation || typeof value.targetId !== 'string' || !uuid.test(value.targetId) || value.outcome !== 'COMMITTED' || !version(value.version)) invalid();
  return value as SupportReceipt;
}
