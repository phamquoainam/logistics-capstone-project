import { AdminSessionError } from './session';
import { governanceId } from './governance';

export const caseStates = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'READY_FOR_DECISION', 'APPROVED', 'REJECTED', 'EXECUTED', 'CANCELLED', 'EXPIRED'] as const;
const types = ['RESTRICT', 'RELEASE_RESTRICTION', 'CLOSE', 'RISK_HOLD', 'RELEASE_HOLD', 'DISPUTE', 'REVERIFY'];
const actions = ['SUBMIT', 'ASSIGN', 'BEGIN', 'REQUEST_INFORMATION', 'RESUBMIT', 'READY_FOR_DECISION', 'CANCEL'];
export const caseStateLabels: Record<string, string> = { DRAFT: 'Nháp', SUBMITTED: 'Đã nộp', UNDER_REVIEW: 'Đang thẩm định', NEEDS_INFORMATION: 'Cần bổ sung', READY_FOR_DECISION: 'Chờ quyết định', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', EXECUTED: 'Đã thực thi', CANCELLED: 'Đã hủy', EXPIRED: 'Hết hạn' };
export type OrganizationCase = { id: string; organizationId: string; type: string; state: string; version: number; policyId: string; policyVersion: number; organizationVersion: number; makerActorKind: 'SYSTEM'; makerAccountId: string; reviewerAccountId: string | null; deciderAccountId: string | null; executorAccountId: string | null; createdAt: string; expiresAt: string; effectiveAt: string | null; allowedActions: string[]; lastEventId: string; isExpired: boolean; asOf: string };
const object = (v: unknown): v is Record<string, unknown> => !!v && typeof v === 'object' && !Array.isArray(v);
const invalid = () => new AdminSessionError('RESPONSE_INVALID');
const stamp = (v: unknown): v is string => typeof v === 'string' && Number.isFinite(Date.parse(v));
export function parseOrganizationCase(value: unknown, org: string): OrganizationCase {
  if (!object(value) || Object.keys(value).length !== 20 || value.organizationId !== org || !governanceId(value.id) || !types.includes(String(value.type)) || !caseStates.includes(value.state as typeof caseStates[number]) || value.makerActorKind !== 'SYSTEM' || !governanceId(value.makerAccountId) || !governanceId(value.lastEventId) || !governanceId(value.policyId) || ['version', 'policyVersion', 'organizationVersion'].some(k => !Number.isSafeInteger(value[k]) || Number(value[k]) < 1) || ['reviewerAccountId', 'deciderAccountId', 'executorAccountId'].some(k => value[k] !== null && !governanceId(value[k])) || ['createdAt', 'expiresAt', 'asOf'].some(k => !stamp(value[k])) || value.effectiveAt !== null && !stamp(value.effectiveAt) || typeof value.isExpired !== 'boolean' || !Array.isArray(value.allowedActions) || new Set(value.allowedActions).size !== value.allowedActions.length || value.allowedActions.some(a => typeof a !== 'string' || !actions.includes(a))) throw invalid();
  if (Date.parse(String(value.expiresAt)) <= Date.parse(String(value.createdAt)) || value.isExpired && value.allowedActions.length) throw invalid();
  return value as OrganizationCase;
}
export type CasePage = { items: OrganizationCase[]; nextCursor: string | null };
export function parseCasePage(v: unknown, org: string): CasePage {
  if (!object(v) || Object.keys(v).length !== 2 || !Array.isArray(v.items) || v.items.length > 50 || v.nextCursor !== null && (typeof v.nextCursor !== 'string' || v.nextCursor.length > 8192)) throw invalid();
  const items = v.items.map(i => parseOrganizationCase(i, org));if (new Set(items.map(i => i.id)).size !== items.length) throw invalid();return { items, nextCursor: v.nextCursor as string | null };
}
export type CasePolicy = { policyId: string; code: string; version: number; allowedCaseTypes: string[]; validFrom: string; validTo: string | null };
export function parseCasePolicies(v: unknown): CasePolicy[] {
  if (!object(v) || Object.keys(v).length !== 1 || !Array.isArray(v.items) || v.items.length > 100) throw invalid();
  return v.items.map(p => {
    if (!object(p) || Object.keys(p).length !== 11 || !governanceId(p.policyId) || typeof p.code !== 'string' || !p.code || !Number.isSafeInteger(p.version) || Number(p.version) < 1 || !Array.isArray(p.allowedCaseTypes) || p.allowedCaseTypes.some(t => !types.includes(String(t))) || !stamp(p.validFrom) || p.validTo !== null && !stamp(p.validTo) || !object(p.limits) || !Array.isArray(p.requiredChecklists) || p.proofAge !== null && (!Number.isSafeInteger(p.proofAge) || Number(p.proofAge) < 1) || !Number.isSafeInteger(p.partyConsentTTL) || Number(p.partyConsentTTL) < 1 || !object(p.readPreservation)) throw invalid();return p as CasePolicy;
  });
}
export type CaseDisclosure = { caseId: string; organizationId: string; version: number; revision: number; casePayload: Record<string, unknown>; reason: string; requestedFieldCodes: string[]; sourceEventId: string; disclosedAt: string };
export function parseCaseDisclosure(v: unknown, row: OrganizationCase): CaseDisclosure {
  if (!object(v) || Object.keys(v).length !== 9 || v.caseId !== row.id || v.organizationId !== row.organizationId || v.version !== row.version || !Number.isSafeInteger(v.revision) || Number(v.revision) < 1 || !governanceId(v.sourceEventId) || v.sourceEventId !== row.lastEventId || !stamp(v.disclosedAt) || typeof v.reason !== 'string' || !v.reason.trim() || [...v.reason].length > 300 || !Array.isArray(v.requestedFieldCodes) || v.requestedFieldCodes.length > 16 || v.requestedFieldCodes.some(c => typeof c !== 'string' || !['reason', 'deniedCapabilities', 'effectiveAt', 'expiresAt', 'holdCode', 'statement', 'fieldCodes', 'evidenceIds'].includes(c)) || !object(v.casePayload)) throw invalid();
  const p = v.casePayload;
  if (row.type === 'CLOSE' ? Object.keys(p).length !== 0 : row.type === 'RISK_HOLD' ? Object.keys(p).length !== 2 || !['BOOKING_HOLD', 'CONTRACT_CHANGE_HOLD'].includes(String(p.holdCode)) || p.expiresAt !== null && !stamp(p.expiresAt) : row.type === 'RESTRICT' ? Object.keys(p).length !== 3 || !Array.isArray(p.deniedCapabilities) || p.deniedCapabilities.length !== 4 || new Set(p.deniedCapabilities).size !== 4 || p.deniedCapabilities.some(c => !['QUOTE_NEW', 'BOOKING_CONFIRM', 'TEAM_DELEGATION', 'CONTRACT_CHANGE'].includes(String(c))) || !stamp(p.effectiveAt) || p.expiresAt !== null && !stamp(p.expiresAt) : true) throw invalid();
  return v as CaseDisclosure;
}
