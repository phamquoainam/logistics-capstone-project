import { AdminSessionError } from './session';

export const applicationStates = ['SUBMITTED', 'UNDER_REVIEW', 'NEEDS_INFORMATION', 'APPROVED', 'REJECTED', 'WITHDRAWN'] as const;
export const stateLabels: Record<string, string> = { SUBMITTED: 'Đã nộp', UNDER_REVIEW: 'Đang thẩm định', NEEDS_INFORMATION: 'Cần bổ sung', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối', WITHDRAWN: 'Đã rút' };
export type Assignment = { id: string; scopeType: 'POLICY' | 'ORGANIZATION'; policyId: string | null; organizationId: string | null; permissions: string[]; version: number; expiresAt: string };
export type GovernanceContext = { accountId: string; realmId: string; authorityVersion: string; assignments: Assignment[] };
export type ApplicationSummary = { id: string; state: string; revision: number; version: number; legalNameMasked: string; legalIdentifierMasked: string; policyId: string; reviewerId: string | null; firstSubmittedAt: string | null; updatedAt: string; activation: unknown; allowedActions: string[] };
export type ApplicationPage = { items: ApplicationSummary[]; page: number; size: number; hasNext: boolean };
export type ReviewerPage = { items: { accountId: string; displayName: string }[]; page: number; size: number; hasNext: boolean };
export type ReviewData = { application: ApplicationSummary; submission: Record<string, unknown>; evidence: { id: string; purpose: string; state: string; sha256: string }[]; contactVerifiedAt: string; policyVersion: string; reviewEvents: unknown[]; organizationDisplayName: string };
const bad = (): never => { throw new AdminSessionError('RESPONSE_INVALID'); };
const object = (v: unknown): Record<string, unknown> => v && typeof v === 'object' && !Array.isArray(v) ? v as Record<string, unknown> : bad();
const shape = (v: unknown, keys: string[]) => { const r = object(v); if (Object.keys(r).length !== keys.length || keys.some(k => !Object.hasOwn(r, k))) bad(); return r; };
export const governanceId = (v: unknown): v is string => typeof v === 'string' && /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000';
const positive = (v: unknown) => Number.isSafeInteger(v) && Number(v) > 0;
const instant = (v: unknown) => typeof v === 'string' && /Z$/.test(v) && Number.isFinite(Date.parse(v));
const text = (v: unknown, limit: number) => typeof v === 'string' && !!v.trim() && [...v].length <= limit;
const capabilities = ['application.read', 'application.evidence.read', 'application.assign', 'application.review', 'application.decide', 'org.read', 'org.suspend', 'org.reinstate', 'audit.read', 'task.read'];
export function parseGovernanceContext(value: unknown, account: string): GovernanceContext {
  const v = shape(value, ['accountId', 'realmId', 'authorityVersion', 'assignments']);
  if (v.accountId !== account || !governanceId(v.realmId) || typeof v.authorityVersion !== 'string' || !/^[a-f0-9]{64}$/.test(v.authorityVersion) || !Array.isArray(v.assignments) || v.assignments.length > 100) bad();
  const ids = new Set<string>();
  for (const raw of v.assignments as unknown[]) {
    const a = shape(raw, ['id', 'scopeType', 'policyId', 'organizationId', 'permissions', 'version', 'expiresAt']);
    if (!governanceId(a.id) || ids.has(a.id) || !positive(a.version) || !instant(a.expiresAt) || !Array.isArray(a.permissions) || !a.permissions.length || a.permissions.length > 10 || new Set(a.permissions).size !== a.permissions.length || a.permissions.some(p => typeof p !== 'string' || !capabilities.includes(p))) bad();
    if (a.scopeType === 'POLICY' ? !governanceId(a.policyId) || a.organizationId !== null : a.scopeType !== 'ORGANIZATION' || !governanceId(a.organizationId) || a.policyId !== null) bad();
    ids.add(a.id as string);
  }
  return v as GovernanceContext;
}
export function parseApplication(value: unknown, target?: string): ApplicationSummary {
  const v = shape(value, ['id', 'state', 'revision', 'version', 'legalNameMasked', 'legalIdentifierMasked', 'policyId', 'reviewerId', 'firstSubmittedAt', 'updatedAt', 'activation', 'allowedActions']);
  if (!governanceId(v.id) || target && v.id !== target || !applicationStates.includes(v.state as typeof applicationStates[number]) || !positive(v.revision) || !positive(v.version) || !text(v.legalNameMasked, 240) || !text(v.legalIdentifierMasked, 80) || !governanceId(v.policyId) || v.reviewerId !== null && !governanceId(v.reviewerId) || v.firstSubmittedAt !== null && !instant(v.firstSubmittedAt) || !instant(v.updatedAt) || !Array.isArray(v.allowedActions) || v.allowedActions.some(a => !['ASSIGN_REVIEWER', 'BEGIN', 'READY_FOR_DECISION', 'REQUEST_INFORMATION', 'APPROVE', 'REJECT'].includes(String(a)))) bad();
  if (v.activation !== null) {
    const a = shape(v.activation, ['applicationId', 'revision', 'decisionEventId', 'organizationId', 'ownerMembershipId', 'state', 'activatedAt']);
    if (a.applicationId !== v.id || a.revision !== v.revision || a.state !== 'ACTIVE' || !governanceId(a.decisionEventId) || !governanceId(a.organizationId) || !governanceId(a.ownerMembershipId) || !instant(a.activatedAt) || v.state !== 'APPROVED') bad();
  }
  return v as ApplicationSummary;
}
export function parseApplicationPage(value: unknown, page: number): ApplicationPage {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']);
  if (v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const items = (v.items as unknown[]).map(v => parseApplication(v)); if (new Set(items.map(a => a.id)).size !== items.length) bad();
  return { items, page, size: 20, hasNext: v.hasNext as boolean };
}
export function parseReviewers(value: unknown, page: number): ReviewerPage {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']);
  if (v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const ids = new Set<string>();for (const raw of v.items as unknown[]) { const a = shape(raw, ['accountId', 'displayName']);if (!governanceId(a.accountId) || ids.has(a.accountId) || !text(a.displayName, 160)) bad();ids.add(a.accountId as string); }
  return v as ReviewerPage;
}
export function parseReviewData(value: unknown, target: string): ReviewData {
  const v = shape(value, ['application', 'submission', 'evidence', 'contactVerifiedAt', 'policyVersion', 'reviewEvents', 'organizationDisplayName']);parseApplication(v.application, target);
  if (!instant(v.contactVerifiedAt) || !text(v.policyVersion, 64) || !text(v.organizationDisplayName, 160) || !Array.isArray(v.evidence) || v.evidence.length < 2 || v.evidence.length > 5 || !Array.isArray(v.reviewEvents) || v.reviewEvents.length > 50) bad();
  const input = shape(v.submission, ['entityType', 'jurisdiction', 'registeredName', 'legalIdentifier', 'registeredAddress', 'operatingAddress', 'representativeName', 'representativeEmail', 'representativeRelationship', 'businessContactAttestationId', 'evidenceIds', 'declarations']);
  for (const field of ['registeredName', 'legalIdentifier', 'representativeName', 'representativeEmail', 'representativeRelationship']) if (!text(input[field], field === 'registeredName' ? 240 : field === 'representativeEmail' ? 254 : 160)) bad();
  for (const raw of v.evidence as unknown[]) { const e = shape(raw, ['id', 'applicationId', 'purpose', 'state', 'mime', 'bytes', 'sha256', 'version', 'screenedAt']);if (!governanceId(e.id) || e.applicationId !== target || !['REGISTRATION', 'REPRESENTATIVE_AUTHORIZATION'].includes(String(e.purpose)) || e.state !== 'CLEAN' || typeof e.sha256 !== 'string' || !/^[a-f0-9]{64}$/.test(e.sha256) || !positive(e.version) || !positive(e.bytes) || Number(e.bytes) > 5242880 || !instant(e.screenedAt)) bad(); }
  return v as ReviewData;
}
export function currentPolicyVersion(value: unknown, target: string, revision: number): string {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']);if (v.page !== 0 || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20) bad();
  for (const raw of v.items as unknown[]) {
    const r = shape(raw, ['applicationId', 'revision', 'submittedAt', 'policyVersion', 'organizationDisplayName', 'digest', 'evidenceIds', 'contactVerifiedAt']);
    if (r.applicationId !== target || !positive(r.revision) || !instant(r.submittedAt) || !instant(r.contactVerifiedAt) || !text(r.organizationDisplayName, 160) || !text(r.policyVersion, 64) || typeof r.digest !== 'string' || !/^[a-f0-9]{64}$/.test(r.digest) || !Array.isArray(r.evidenceIds) || r.evidenceIds.length < 2 || r.evidenceIds.length > 5 || r.evidenceIds.some(id => !governanceId(id))) bad();
    if (r.revision === revision) return r.policyVersion as string;
  }
  return bad();
}

export type ApplicationAuditPage = { items: { id: string; actorId: string; operation: string; targetType: 'APPLICATION'; targetId: string; priorVersion: number; newVersion: number; revision: number | null; policyVersion: string; reasonCode: string; occurredAt: string; requestId: string }[]; page: number; size: number; hasNext: boolean };
export function parseApplicationEvents(value: unknown, target: string, page: number): ApplicationAuditPage {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']); if (v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const ids = new Set<string>();
  for (const item of v.items as unknown[]) {
    const e = shape(item, ['id', 'actorId', 'operation', 'targetType', 'targetId', 'priorVersion', 'newVersion', 'revision', 'policyVersion', 'reasonCode', 'occurredAt', 'requestId']);
    if (!governanceId(e.id) || ids.has(e.id as string) || !governanceId(e.actorId) || !governanceId(e.requestId) || e.targetType !== 'APPLICATION' || e.targetId !== target || !['SUBMIT', 'RESUBMIT', 'WITHDRAW', 'ASSIGN_REVIEWER', 'BEGIN_REVIEW', 'READY_FOR_DECISION', 'REQUEST_INFORMATION', 'APPROVE', 'REJECT', 'READ_DISCLOSURE', 'RESERVE_EVIDENCE', 'FINALIZE_EVIDENCE', 'ARCHIVE_EVIDENCE'].includes(String(e.operation)) || !Number.isSafeInteger(e.priorVersion) || Number(e.priorVersion) < 0 || !Number.isSafeInteger(e.newVersion) || Number(e.newVersion) < Number(e.priorVersion) || e.revision !== null && !positive(e.revision) || !text(e.policyVersion, 64) || typeof e.reasonCode !== 'string' || !/^[A-Z_]{1,40}$/.test(e.reasonCode) || !instant(e.occurredAt)) bad();
    ids.add(e.id as string);
  }
  return v as ApplicationAuditPage;
}

export type OrganizationSummary = { id: string; displayName: string; state: 'ACTIVE' | 'SUSPENDED'; admissionOrigin: 'APPLICATION_APPROVED' | 'LOCAL_OPERATOR_PROVISIONED'; version: number; initialOwnerMembershipId: string; activation: { applicationId: string; revision: number; decisionEventId: string; organizationId: string; ownerMembershipId: string; state: 'ACTIVE'; activatedAt: string } | null; membershipCounts: Record<string, Record<string, number>>; allowedActions: ('SUSPEND' | 'REINSTATE')[] };
export type OrganizationPage = { items: OrganizationSummary[]; page: number; size: number; hasNext: boolean };
export function parseOrganization(value: unknown, target?: string): OrganizationSummary {
  const v = shape(value, ['id', 'displayName', 'state', 'admissionOrigin', 'version', 'initialOwnerMembershipId', 'activation', 'membershipCounts', 'allowedActions']);
  if (!governanceId(v.id) || target && v.id !== target || !text(v.displayName, 160) || !['ACTIVE', 'SUSPENDED'].includes(String(v.state)) || !['APPLICATION_APPROVED', 'LOCAL_OPERATOR_PROVISIONED'].includes(String(v.admissionOrigin)) || !positive(v.version) || !governanceId(v.initialOwnerMembershipId) || !v.membershipCounts || typeof v.membershipCounts !== 'object' || Array.isArray(v.membershipCounts) || !Array.isArray(v.allowedActions) || new Set(v.allowedActions).size !== v.allowedActions.length || v.allowedActions.some(a => !['SUSPEND', 'REINSTATE'].includes(String(a)))) bad();
  for (const [role, counts] of Object.entries(v.membershipCounts as Record<string, unknown>)) {
    if (!['OWNER', 'MANAGER', 'OPERATOR'].includes(role) || !counts || typeof counts !== 'object' || Array.isArray(counts)) bad();
    for (const [state, count] of Object.entries(counts as Record<string, unknown>)) if (!['ACTIVE', 'SUSPENDED', 'REVOKED'].includes(state) || !Number.isSafeInteger(count) || Number(count) < 0) bad();
  }
  if (v.admissionOrigin === 'APPLICATION_APPROVED') {
    const a = shape(v.activation, ['applicationId', 'revision', 'decisionEventId', 'organizationId', 'ownerMembershipId', 'state', 'activatedAt']);
    if (!governanceId(a.applicationId) || !positive(a.revision) || !governanceId(a.decisionEventId) || a.organizationId !== v.id || a.ownerMembershipId !== v.initialOwnerMembershipId || a.state !== 'ACTIVE' || !instant(a.activatedAt)) bad();
  } else if (v.activation !== null || (v.allowedActions as unknown[]).length) bad();
  if ((v.allowedActions as unknown[]).some(action => action === 'SUSPEND' && v.state !== 'ACTIVE' || action === 'REINSTATE' && v.state !== 'SUSPENDED')) bad();
  return v as OrganizationSummary;
}
export function parseOrganizationPage(value: unknown, page: number): OrganizationPage {
  const v = shape(value, ['items', 'page', 'size', 'hasNext']);
  if (v.page !== page || v.size !== 20 || typeof v.hasNext !== 'boolean' || !Array.isArray(v.items) || v.items.length > 20 || v.hasNext && v.items.length !== 20) bad();
  const items = (v.items as unknown[]).map(item => parseOrganization(item));if (new Set(items.map(item => item.id)).size !== items.length) bad();return { items, page, size: 20, hasNext: v.hasNext as boolean };
}
