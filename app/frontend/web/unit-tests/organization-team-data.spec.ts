import { test, expect } from '@playwright/test';
import { teamMembers, teamMember, teamRoleTemplates, teamDecision, teamInvitation, teamInvitations, teamAudits, missingTeamGrantReads } from '../src/customer/organization-team-data';
import type { TeamQuery } from '../src/customer/organization-team-data';
const id = '10000000-0000-4000-8000-000000000901';
const row = { membershipId: id, displayName: 'Synthetic member', role: 'OWNER', state: 'ACTIVE', authorityVersion: 1, version: 1, createdAt: '2026-11-17T00:00:00Z', updatedAt: '2026-11-17T00:00:00Z' };
const query: TeamQuery = { search: '', role: null, state: null, facilityId: null, page: 0, size: 20 };
test('directory DTO enforces page/filter binding and excludes Identity/private fields', () => {
  expect(teamMembers({ items: [row], page: 0, size: 20, hasNext: false }, query).items).toHaveLength(1);
  for (const bad of [{ ...row, email: 'synthetic@example.invalid' }, { ...row, accountId: id }, { ...row, role: 'ROOT_ADMIN' }, { ...row, state: 'APPROVED' }, { ...row, membershipId: 'bad' }]) expect(() => teamMembers({ items: [bad], page: 0, size: 20, hasNext: false }, query)).toThrow('TEAM_RESPONSE_INVALID');
  expect(() => teamMembers({ items: [row, row], page: 0, size: 20, hasNext: false }, query)).toThrow();
  expect(() => teamMembers({ items: [row], page: 1, size: 20, hasNext: false }, query)).toThrow();
  expect(() => teamMembers({ items: [row], page: 0, size: 20, hasNext: false }, { ...query, state: 'REVOKED' })).toThrow();
});
test('member detail binds the requested member and explicit scopes, without deriving role grants', () => {
  const detail = { ...row, grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }] };
  expect(teamMember(detail, id).grants).toHaveLength(1);
  expect(() => teamMember(detail, '10000000-0000-4000-8000-000000000902')).toThrow();
  expect(() => teamMember({ ...detail, grants: [{ permission: 'team.root', scope: 'ORGANIZATION', facilityId: null }] }, id)).toThrow();
  expect(() => teamMember({ ...detail, grants: [{ permission: 'dashboard.read', scope: 'FACILITY', facilityId: null }] }, id)).toThrow();
  expect(() => teamMember({ ...detail, grants: [...detail.grants, { facilityId: null, scope: 'ORGANIZATION', permission: 'dashboard.read' }] }, id)).toThrow();
});
test('recipient decision binds invitation, selected operation and actual membership outcome', () => {
  expect(teamDecision({ invitationId: id, outcome: 'ACCEPTED', membershipId: id }, id, true).outcome).toBe('ACCEPTED');
  expect(teamDecision({ invitationId: id, outcome: 'DECLINED', membershipId: null }, id, false).membershipId).toBeNull();
  expect(() => teamDecision({ invitationId: id, outcome: 'ACCEPTED', membershipId: null }, id, true)).toThrow();
  expect(() => teamDecision({ invitationId: id, outcome: 'DECLINED', membershipId: id }, id, false)).toThrow();
  expect(() => teamDecision({ invitationId: id, outcome: 'DECLINED', membershipId: null }, id, true)).toThrow();
  expect(() => teamDecision({ invitationId: id, outcome: 'ACCEPTED', membershipId: id, email: 'synthetic@example.invalid' }, id, true)).toThrow();
});
test('role catalog cannot introduce extra roles or platform permissions', () => {
  const value = { items: ['OWNER', 'MANAGER', 'OPERATOR'].map(role => ({ role, version: 1, allowedPermissions: ['dashboard.read'] })) };
  expect(teamRoleTemplates(value).items).toHaveLength(3);
  expect(() => teamRoleTemplates({ items: [...value.items, { ...value.items[0], role: 'WAREHOUSE' }] })).toThrow();
  expect(() => teamRoleTemplates({ items: value.items.map(r => ({ ...r, allowedPermissions: ['ROOT_ADMIN'] })) })).toThrow();
});
test('invitation projection requires masked recipient, operational intent and exact page binding', () => {
  const invitation = { id, maskedEmail: 's***@example.invalid', role: 'OPERATOR', grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }], state: 'PENDING', version: 1, createdAt: '2026-11-17T00:00:00Z', expiresAt: '2026-11-20T00:00:00Z', deliveryState: 'QUEUED' };
  expect(teamInvitation(invitation).state).toBe('PENDING');
  for (const change of [{ maskedEmail: 'synthetic@example.invalid' }, { role: 'OWNER' }, { expiresAt: '2026-11-19T00:00:00Z' }, { deliveryState: 'INBOX_CONFIRMED' }, { proof: 'secret' }, { grants: [...invitation.grants, { permission: 'team.invite', scope: 'ORGANIZATION', facilityId: null }] }]) expect(() => teamInvitation({ ...invitation, ...change })).toThrow();
  expect(teamInvitations({ items: [invitation], page: 0, size: 20, hasNext: false }, { state: null, page: 0, size: 20 }).items).toHaveLength(1);
  expect(() => teamInvitations({ items: [invitation], page: 0, size: 20, hasNext: false }, { state: 'REVOKED', page: 0, size: 20 })).toThrow();
});
test('audit binds exact filters and period without exposing Identity fields or unknown authority', () => {
  const query = { membershipId: null, operation: null, from: '2026-11-17T00:00:00Z', to: '2026-11-18T00:00:00Z', page: 0, size: 20 };
  const audit = { id, actorMembershipId: id, targetMembershipId: null, operation: 'INVITE', reason: 'Synthetic invitation', occurredAt: '2026-11-17T05:00:00Z' };
  const page = { items: [audit], page: 0, size: 20, hasNext: false };
  expect(teamAudits(page, query).items).toHaveLength(1);
  for (const bad of [{ ...audit, actorAccountId: id }, { ...audit, operation: 'TRANSFER_OWNER' }, { ...audit, actorMembershipId: null }, { ...audit, occurredAt: query.to }]) expect(() => teamAudits({ ...page, items: [bad] }, query)).toThrow();
  expect(() => teamAudits(page, { ...query, operation: 'ACCESS' })).toThrow();
});
test('write capabilities require reads at the same facility or Organization without broadening grants', () => {
  const base = { permission: 'dashboard.read' as const, scope: 'ORGANIZATION' as const, facilityId: null };
  const write = { permission: 'shipment.draft.write' as const, scope: 'FACILITY' as const, facilityId: id };
  expect(missingTeamGrantReads([base, write])).toEqual(['shipment.draft.read']);
  expect(missingTeamGrantReads([base, write, { permission: 'shipment.draft.read', scope: 'FACILITY', facilityId: '10000000-0000-4000-8000-000000000902' }])).toEqual(['shipment.draft.read']);
  expect(missingTeamGrantReads([base, write, { permission: 'shipment.draft.read', scope: 'ORGANIZATION', facilityId: null }])).toEqual([]);
  expect(missingTeamGrantReads([base, { permission: 'contacts.import', scope: 'FACILITY', facilityId: id }])).toEqual(['contacts.read', 'contacts.write']);
});
