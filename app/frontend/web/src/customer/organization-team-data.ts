import { businessPermissions } from './business-data';
import type { BusinessPermission } from './business-data';
export type TeamMember = { membershipId: string; displayName: string; role: 'OWNER' | 'MANAGER' | 'OPERATOR'; state: 'ACTIVE' | 'SUSPENDED' | 'REVOKED'; authorityVersion: number; version: number; createdAt: string; updatedAt: string };
export type TeamQuery = { search: string; role: TeamMember['role'] | null; state: TeamMember['state'] | null; facilityId: string | null; page: number; size: number };
export type TeamMemberPage = { items: TeamMember[]; page: number; size: number; hasNext: boolean };
export type TeamGrant = { permission: BusinessPermission; scope: 'ORGANIZATION' | 'FACILITY'; facilityId: string | null };
export function missingTeamGrantReads(grants: TeamGrant[]): BusinessPermission[] {
  const missing = new Set<BusinessPermission>();
  for (const g of grants) {
    const required: BusinessPermission[] = [];
    if (g.permission.startsWith('shipment.') && g.permission !== 'shipment.draft.read') required.push('shipment.draft.read');
    if (['contacts.write', 'contacts.import', 'contacts.export'].includes(g.permission)) required.push('contacts.read');
    if (g.permission === 'contacts.import') required.push('contacts.write');
    if (g.permission === 'facility.manage') required.push('facility.read');
    if (g.permission === 'shipment.handover.manage') required.push('shipment.handover.read');
    for (const permission of required) if (!grants.some(read => read.permission === permission && (read.scope === 'ORGANIZATION' || read.facilityId === g.facilityId))) missing.add(permission);
  }
  return [...missing];
}
export type TeamMemberDetail = TeamMember & { grants: TeamGrant[] };
export type TeamRoleTemplates = { items: { role: TeamMember['role']; version: number; allowedPermissions: BusinessPermission[] }[] };
const fields = ['membershipId', 'displayName', 'role', 'state', 'authorityVersion', 'version', 'createdAt', 'updatedAt'];
function check(ok: unknown): asserts ok { if (!ok) throw new Error('TEAM_RESPONSE_INVALID'); }
function object(v: unknown, keys: string[]): asserts v is Record<string, unknown> { check(v && typeof v === 'object' && !Array.isArray(v) && Object.keys(v).length === keys.length && keys.every(k => Object.hasOwn(v, k))); }
function uuid(v: unknown) { check(typeof v === 'string' && /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(v) && v !== '00000000-0000-0000-0000-000000000000'); }
function member(v: unknown, detail = false): asserts v is TeamMember {
  object(v, detail ? [...fields, 'grants'] : fields); uuid(v.membershipId); check(typeof v.displayName === 'string' && v.displayName.trim().length > 0 && [...v.displayName].length <= 160 && !/[\u0000-\u001f\u007f]/.test(v.displayName));
  check(['OWNER', 'MANAGER', 'OPERATOR'].includes(String(v.role)) && ['ACTIVE', 'SUSPENDED', 'REVOKED'].includes(String(v.state)));
  for (const k of ['version', 'authorityVersion']) check(Number.isSafeInteger(v[k]) && Number(v[k]) > 0);
  for (const k of ['createdAt', 'updatedAt']) check(typeof v[k] === 'string' && /^\d{4}-\d{2}-\d{2}T.*Z$/.test(v[k]) && Number.isFinite(Date.parse(v[k])));
  check(Date.parse(String(v.updatedAt)) >= Date.parse(String(v.createdAt)));
}
export function teamMembers(value: unknown, q: TeamQuery): TeamMemberPage {
  object(value, ['items', 'page', 'size', 'hasNext']); check(value.page === q.page && value.size === q.size && typeof value.hasNext === 'boolean' && Array.isArray(value.items) && value.items.length <= q.size);
  const ids = new Set(); for (const row of value.items) { member(row); check(!ids.has(row.membershipId) && (!q.role || row.role === q.role) && (!q.state || row.state === q.state)); ids.add(row.membershipId); }
  return value as TeamMemberPage;
}
export function teamMember(value: unknown, id: string): TeamMemberDetail {
  member(value, true); check(value.membershipId === id); const grants = (value as TeamMemberDetail).grants; check(Array.isArray(grants) && grants.length <= 200);
  const keys = new Set(); for (const g of grants) { object(g, ['permission', 'scope', 'facilityId']); check(businessPermissions.includes(g.permission as BusinessPermission) && ['ORGANIZATION', 'FACILITY'].includes(String(g.scope))); check((g.scope === 'ORGANIZATION') === (g.facilityId === null)); if (g.facilityId !== null) uuid(g.facilityId); const key = JSON.stringify([g.permission, g.scope, g.facilityId]); check(!keys.has(key)); keys.add(key); }
  return value as TeamMemberDetail;
}
export function teamRoleTemplates(value: unknown): TeamRoleTemplates {
  object(value, ['items']); check(Array.isArray(value.items) && value.items.length === 3); const roles = new Set();
  for (const row of value.items) { object(row, ['role', 'version', 'allowedPermissions']); check(['OWNER', 'MANAGER', 'OPERATOR'].includes(String(row.role)) && !roles.has(row.role)); roles.add(row.role); check(row.version === 1 && Array.isArray(row.allowedPermissions) && new Set(row.allowedPermissions).size === row.allowedPermissions.length && row.allowedPermissions.every(p => businessPermissions.includes(p as BusinessPermission) && !String(p).startsWith('team.') && !String(p).startsWith('support.'))); }
  return value as TeamRoleTemplates;
}
export type TeamDecision = { invitationId: string; outcome: 'ACCEPTED' | 'DECLINED'; membershipId: string | null };
export const teamAuditOperations = ['INVITE', 'RESEND', 'REVOKE_INVITATION', 'ACCESS', 'SUSPEND', 'RESUME', 'REVOKE_MEMBER', 'ACCEPT', 'DECLINE'] as const;
export type TeamAuditOperation = typeof teamAuditOperations[number];
export type TeamAuditQuery = { membershipId: string | null; operation: TeamAuditOperation | null; from: string; to: string; page: number; size: number };
export type TeamAudit = { id: string; actorMembershipId: string | null; targetMembershipId: string | null; operation: TeamAuditOperation; reason: string; occurredAt: string };
export type TeamAuditPage = { items: TeamAudit[]; page: number; size: number; hasNext: boolean };
export function teamAudits(value: unknown, q: TeamAuditQuery): TeamAuditPage {
  object(value, ['items', 'page', 'size', 'hasNext']); check(value.page === q.page && value.size === q.size && typeof value.hasNext === 'boolean' && Array.isArray(value.items) && value.items.length <= q.size);
  const ids = new Set();
  for (const row of value.items) {
    object(row, ['id', 'actorMembershipId', 'targetMembershipId', 'operation', 'reason', 'occurredAt']); uuid(row.id); check(!ids.has(row.id)); ids.add(row.id);
    check(teamAuditOperations.includes(row.operation as TeamAuditOperation) && (!q.operation || row.operation === q.operation));
    if (row.actorMembershipId !== null) uuid(row.actorMembershipId); else check(row.operation === 'DECLINE');
    if (row.targetMembershipId !== null) uuid(row.targetMembershipId); check(!q.membershipId || row.targetMembershipId === q.membershipId);
    check(typeof row.reason === 'string' && row.reason.trim().length > 0 && [...row.reason].length <= 300 && !/[\u0000-\u001f\u007f]/.test(row.reason));
    check(typeof row.occurredAt === 'string' && /^\d{4}-\d{2}-\d{2}T.*Z$/.test(row.occurredAt) && Date.parse(row.occurredAt) >= Date.parse(q.from) && Date.parse(row.occurredAt) < Date.parse(q.to));
  }
  return value as TeamAuditPage;
}
export type TeamInvitationState = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'REVOKED' | 'EXPIRED' | 'INVALIDATED';
export type TeamInvitation = { id: string; maskedEmail: string; role: 'MANAGER' | 'OPERATOR'; grants: TeamGrant[]; state: TeamInvitationState; version: number; expiresAt: string; deliveryState: string; createdAt: string };
export type TeamInvitationQuery = { state: TeamInvitationState | null; page: number; size: number };
export type TeamInvitationPage = { items: TeamInvitation[]; page: number; size: number; hasNext: boolean };
export type TeamInvitationInput = { email: string; role: 'MANAGER' | 'OPERATOR'; grants: TeamGrant[]; reason: string };
export function teamInvitation(value: unknown): TeamInvitation {
  object(value, ['id', 'maskedEmail', 'role', 'grants', 'state', 'version', 'expiresAt', 'deliveryState', 'createdAt']); uuid(value.id);
  check(typeof value.maskedEmail === 'string' && /^[^\s@]\*\*\*@[^\s@]+$/.test(value.maskedEmail) && value.maskedEmail.length <= 260);
  check(['MANAGER', 'OPERATOR'].includes(String(value.role)) && ['PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED', 'INVALIDATED'].includes(String(value.state)) && Number.isSafeInteger(value.version) && Number(value.version) > 0);
  for (const field of ['createdAt', 'expiresAt']) check(typeof value[field] === 'string' && /^\d{4}-\d{2}-\d{2}T.*Z$/.test(value[field]) && Number.isFinite(Date.parse(value[field])));
  check(Date.parse(String(value.expiresAt)) >= Date.parse(String(value.createdAt)) + 72 * 3600000);
  check(['QUEUED', 'SENDING', 'RETRY_WAIT', 'ACCEPTED_BY_PROVIDER', 'DELIVERED', 'FAILED', 'EXPIRED', 'BLOCKED'].includes(String(value.deliveryState)));
  const detail = teamMember({ membershipId: value.id, displayName: 'Invitation', role: value.role, state: 'ACTIVE', authorityVersion: 1, version: 1, createdAt: value.createdAt, updatedAt: value.createdAt, grants: value.grants }, String(value.id));
  check(detail.grants.length > 0 && !detail.grants.some(g => (g.permission.startsWith('team.') || g.permission.startsWith('support.'))) && detail.grants.some(g => g.permission === 'dashboard.read' && g.scope === 'ORGANIZATION'));
  return value as TeamInvitation;
}
export function teamInvitations(value: unknown, q: TeamInvitationQuery): TeamInvitationPage {
  object(value, ['items', 'page', 'size', 'hasNext']); check(value.page === q.page && value.size === q.size && typeof value.hasNext === 'boolean' && Array.isArray(value.items) && value.items.length <= q.size);
  const ids = new Set(); for (const item of value.items) { const row = teamInvitation(item); check(!ids.has(row.id) && (!q.state || row.state === q.state)); ids.add(row.id); }
  return value as TeamInvitationPage;
}
export function teamDecision(value: unknown, id: string, accept: boolean): TeamDecision {
  object(value, ['invitationId', 'outcome', 'membershipId']); uuid(value.invitationId);
  check(value.invitationId === id && value.outcome === (accept ? 'ACCEPTED' : 'DECLINED'));
  if (accept) uuid(value.membershipId); else check(value.membershipId === null);
  return value as TeamDecision;
}
