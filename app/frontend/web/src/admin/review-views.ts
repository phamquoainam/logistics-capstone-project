import { AdminSessionError } from './session';
import { governanceId, parseApplication, parseOrganization } from './governance';
import type { ApplicationSummary, OrganizationSummary } from './governance';

const bad = (): never => { throw new AdminSessionError('RESPONSE_INVALID'); };
export const record = (v: unknown): Record<string, unknown> => v !== null && typeof v === 'object' && !Array.isArray(v) ? v as Record<string, unknown> : bad();
const shape = (v: unknown, fields: string[]) => { const r = record(v);if (Object.keys(r).length !== fields.length || fields.some(k => !Object.hasOwn(r, k))) bad();return r; };
const number = (v: unknown, min = 0): v is number => Number.isSafeInteger(v) && Number(v) >= min;
const stamp = (v: unknown) => typeof v === 'string' && /Z$/.test(v) && Number.isFinite(Date.parse(v));
const digest = (v: unknown) => typeof v === 'string' && /^[a-f0-9]{64}$/.test(v);
const cursor = (v: unknown): v is string | null => v === null || typeof v === 'string' && /^[A-Za-z0-9_-]{1,512}$/.test(v);
export type SearchPage<T> = { items: T[]; matchedCount: number; nextCursor: string | null; snapshotVersion: string };
export function searchPage<T>(raw: unknown, parse: (v: unknown) => T): SearchPage<T> {
  const v = shape(raw, ['items', 'matchedCount', 'nextCursor', 'snapshotVersion']);
  if (!Array.isArray(v.items) || v.items.length > 20 || !number(v.matchedCount) || v.matchedCount < v.items.length || !cursor(v.nextCursor) || !digest(v.snapshotVersion)) bad();
  const items = (v.items as unknown[]).map(parse);if (new Set(items.map(v => record(v).id)).size !== items.length) bad();return { items, matchedCount: v.matchedCount as number, nextCursor: v.nextCursor as string | null, snapshotVersion: v.snapshotVersion as string };
}
export const applicationSearchPage = (raw: unknown) => searchPage<ApplicationSummary>(raw, v => parseApplication(v));
export function organizationSearchPage(raw: unknown) { return searchPage<OrganizationSummary>(raw, value => {
  const v = shape(value, ['id', 'displayName', 'state', 'admissionOrigin', 'version', 'initialOwnerMembershipId', 'currentOwnerMembershipId', 'ownershipVersion', 'activation', 'membershipCounts']);
  if (v.currentOwnerMembershipId !== v.initialOwnerMembershipId || v.ownershipVersion !== null) bad();
  const { currentOwnerMembershipId: _current, ownershipVersion: _version, ...base } = v;void _current;void _version;
  return parseOrganization({ ...base, allowedActions: [] });
}); }
export type ReviewComment = { id: string; applicationId: string; revision: number; actorAccountId: string; createdAt: string; text: string };
export function comment(raw: unknown, target: string): ReviewComment {
  const v = shape(raw, ['id', 'applicationId', 'revision', 'actorAccountId', 'createdAt', 'text']);
  if (!governanceId(v.id) || v.applicationId !== target || !number(v.revision, 1) || !governanceId(v.actorAccountId) || !stamp(v.createdAt) || typeof v.text !== 'string' || !v.text.trim() || [...v.text].length > 2000) bad();return v as ReviewComment;
}
export function commentPage(raw: unknown, target: string) {
  const v = shape(raw, ['items', 'nextCursor', 'disclosureEventId']);if (!Array.isArray(v.items) || v.items.length > 20 || !cursor(v.nextCursor) || !governanceId(v.disclosureEventId)) bad();
  const items = (v.items as unknown[]).map(v => comment(v, target));if (new Set(items.map(v => v.id)).size !== items.length) bad();return { items, nextCursor: v.nextCursor as string | null };
}
export type Comparison = { applicationId: string; fromRevision: number; toRevision: number; changes: { field: string; before: unknown; after: unknown }[]; evidenceAdded: string[]; evidenceRemoved: string[]; contactVerifiedAt: { from: string; to: string } };
const diffFields = ['entityType', 'jurisdiction', 'registeredName', 'legalIdentifier', 'registeredAddress', 'operatingAddress', 'representativeName', 'representativeEmail', 'representativeRelationship', 'declarations', 'activationDisplayName'];
export function comparison(raw: unknown, target: string): Comparison {
  const v = shape(raw, ['applicationId', 'fromRevision', 'toRevision', 'fromDigest', 'toDigest', 'changes', 'evidenceAdded', 'evidenceRemoved', 'contactVerifiedAt', 'disclosureEventId']);
  if (v.applicationId !== target || !number(v.fromRevision, 1) || !number(v.toRevision, 2) || Number(v.fromRevision) >= Number(v.toRevision) || !digest(v.fromDigest) || !digest(v.toDigest) || !governanceId(v.disclosureEventId) || !Array.isArray(v.changes) || v.changes.length > 11) bad();
  const seen = new Set<string>();for (const raw of v.changes as unknown[]) { const f = shape(raw, ['field', 'before', 'after']);if (typeof f.field !== 'string' || !diffFields.includes(f.field) || seen.has(f.field)) bad();seen.add(String(f.field)); }
  for (const key of ['evidenceAdded', 'evidenceRemoved']) { const ids = v[key];if (!Array.isArray(ids) || ids.length > 5 || ids.some(id => !governanceId(id)) || new Set(ids).size !== ids.length) bad(); }
  const times = shape(v.contactVerifiedAt, ['from', 'to']);if (!stamp(times.from) || !stamp(times.to)) bad();return v as Comparison;
}
export function checks(raw: unknown, target: string) {
  const v = shape(raw, ['applicationId', 'revision', 'checks', 'foreignMatches', 'automaticVerification']);if (v.applicationId !== target || !number(v.revision, 1) || !Array.isArray(v.checks) || v.checks.length > 8 || v.foreignMatches !== 'NOT_EXPOSED' || v.automaticVerification !== 'NOT_PROVIDED') bad();
  const codes = ['SUBMISSION_BINDING', 'CONTACT_REVERIFY_REQUIRED', 'EVIDENCE_NOT_CLEAN', 'BUSINESS_KEY_CONFLICT', 'REVIEWER_CONFLICT', 'EXTERNAL_VERIFICATION'];const seen = new Set<string>();
  for (const raw of v.checks as unknown[]) { const c = shape(raw, ['code', 'result', 'sourceVersion', 'sourceEventId']);if (!codes.includes(String(c.code)) || seen.has(String(c.code)) || !['PASS', 'FAIL', 'UNAVAILABLE'].includes(String(c.result)) || c.sourceVersion !== null && typeof c.sourceVersion !== 'string' || c.sourceEventId !== null && !governanceId(c.sourceEventId)) bad();seen.add(String(c.code)); }
  return v.checks as { code: string; result: string }[];
}
export function profile(raw: unknown, target: string) {
  const v = shape(raw, ['organizationId', 'organizationVersion', 'initialOwnerMembershipId', 'currentOwnerMembershipId', 'activationApplicationId', 'legal', 'contact', 'facilities', 'pricing', 'holds', 'disclosureEventId']);
  if (v.organizationId !== target || !number(v.organizationVersion, 1) || !governanceId(v.initialOwnerMembershipId) || v.currentOwnerMembershipId !== v.initialOwnerMembershipId || v.activationApplicationId !== null && !governanceId(v.activationApplicationId) || !governanceId(v.disclosureEventId)) bad();
  const blocks: Record<string, Record<string, unknown>> = {};
  const fields: Record<string, string[]> = { legal: ['registeredName', 'entityType', 'jurisdiction', 'legalIdentifier', 'registeredAddress', 'operatingAddress'], contact: ['representativeName', 'relationship', 'verifiedAt', 'maskedEmail'], facilities: ['items'], pricing: [], holds: [] };
  for (const key of Object.keys(fields)) {
    const b = record(v[key]);if (!['AVAILABLE', 'UNAVAILABLE', 'FORBIDDEN'].includes(String(b.availability)) || Object.keys(b).some(k => !['availability', 'sourceVersion', 'asOf', 'reason', ...fields[key]].includes(k))) bad();
    if (b.availability === 'AVAILABLE' ? typeof b.sourceVersion !== 'string' || !stamp(b.asOf) || b.reason !== null : b.sourceVersion !== null || b.asOf !== null || typeof b.reason !== 'string') bad();
    blocks[key] = b;
  }
  return blocks;
}
export type Member = { membershipId: string; accountId: string; role: string; state: string; version: number; authorityVersion: number; isInitialOwner: boolean; isCurrentOwner: boolean };
export function members(raw: unknown, target: string) {
  const v = shape(raw, ['organizationId', 'organizationVersion', 'items', 'pendingInvitationCount', 'nextCursor', 'disclosureEventId']);if (v.organizationId !== target || !number(v.organizationVersion, 1) || !Array.isArray(v.items) || v.items.length > 20 || !number(v.pendingInvitationCount) || !cursor(v.nextCursor) || !governanceId(v.disclosureEventId)) bad();
  const ids = new Set<string>();for (const raw of v.items as unknown[]) { const m = shape(raw, ['membershipId', 'accountId', 'role', 'state', 'version', 'authorityVersion', 'isInitialOwner', 'isCurrentOwner']);if (!governanceId(m.membershipId) || ids.has(m.membershipId) || !governanceId(m.accountId) || !['OWNER', 'MANAGER', 'OPERATOR'].includes(String(m.role)) || !['ACTIVE', 'SUSPENDED', 'REVOKED'].includes(String(m.state)) || !number(m.version, 1) || !number(m.authorityVersion, 1) || typeof m.isInitialOwner !== 'boolean' || typeof m.isCurrentOwner !== 'boolean') bad();ids.add(m.membershipId as string); }
  return { items: v.items as Member[], nextCursor: v.nextCursor as string | null, pendingInvitationCount: v.pendingInvitationCount as number };
}
