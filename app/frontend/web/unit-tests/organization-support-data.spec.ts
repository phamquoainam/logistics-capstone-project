import { test, expect } from '@playwright/test';
import { organizationTicketDetail, organizationTicketPage, organizationSupportReport, organizationSupportPreferences, organizationSupportNotice } from '../src/customer/organization-support-data';
import { teamRoleTemplates, teamInvitation } from '../src/customer/organization-team-data';
const org = '10000000-0000-4000-8000-000000000901', id = '10000000-0000-4000-8000-000000000902', actor = '10000000-0000-4000-8000-000000000903', time = '2026-11-17T00:00:00Z';
const row = { id, code: 'AP-OST-' + id.replaceAll('-', ''), organizationId: org, facilityId: null, creatorAccountId: actor, kind: 'GENERAL', shipmentId: null, category: 'ACCOUNT_HELP', subject: 'Synthetic support', state: 'NEW', version: 1, createdAt: time, updatedAt: time, requestedPriority: 'HIGH', priority: 'NORMAL', responseDueAt: null, teamLabel: null, internalAssigneeAccountId: null };
const detail = { ...row, publicMessages: [{ id: actor, authorKind: 'MERCHANT', message: 'Synthetic question', createdAt: time, eventType: 'CREATE' }], internalAssignment: { ticketId: id, organizationId: org, assigneeAccountId: null, version: 0, updatedAt: null }, allowedActions: ['REPLY', 'ASSIGN'] };
test('Organization Support validates current organization typed ownership and private projection', () => {
  expect(organizationTicketDetail(detail, org, id).allowedActions).toContain('REPLY');
  for (const changed of [{ organizationId: id }, { code: 'AP-ST-PERSONAL' }, { ownerAccountId: actor }, { responseDueAt: time }, { priority: 'HIGH' }, { state: 'DELIVERED' }, { publicMessages: [] }, { allowedActions: ['PAID'] }, { internalAssignment: { ...detail.internalAssignment, organizationId: id } }]) expect(() => organizationTicketDetail({ ...detail, ...changed }, org, id)).toThrow();
  expect(() => organizationTicketPage({ items: [row, row], page: 0, size: 20, hasNext: false }, org, 0)).toThrow();
  expect(() => organizationTicketPage({ items: [row], page: 1, size: 20, hasNext: false }, org, 0)).toThrow();
});
test('support reports reject fake SLA inconsistent denominators and assignment as response', () => {
  const zero = { sampleCount: '0', totalMs: '0', minMs: null, maxMs: null };
  const r = { schemaVersion: 'ORGANIZATION_SUPPORT_REPORT_1', organizationId: org, generatedAt: time, timeZone: 'Asia/Ho_Chi_Minh', period: { from: '2026-11-01', until: '2026-11-18' }, totalTickets: '1', byState: { NEW: '1', IN_PROGRESS: '0', NEEDS_INFORMATION: '0', RESOLVED: '0', CLOSED: '0' }, byCategory: { ACCOUNT_HELP: '1', SHIPMENT_TRACE: '0', DAMAGE_REPORT: '0', LOSS_REPORT: '0', FEE_REVIEW: '0', OTHER: '0' }, byRequestedPriority: { LOW: '0', NORMAL: '0', HIGH: '1' }, firstResponse: zero, firstResolution: zero, slaState: 'UNPUBLISHED' };
  expect(organizationSupportReport(r, org).firstResponse.sampleCount).toBe('0');
  for (const changed of [{ slaState: 'ON_TIME' }, { totalTickets: '2' }, { firstResponse: { ...zero, sampleCount: '2' } }, { firstResolution: { ...zero, minMs: '0' } }, { firstResponse: { sampleCount: '1', totalMs: '10', minMs: '20', maxMs: '30' } }]) expect(() => organizationSupportReport({ ...r, ...changed }, org)).toThrow();
});
test('notification preferences are default off and notices bind real committed source ids', () => {
  expect(organizationSupportPreferences({ enabled: false, eventTypes: [], version: 0, updatedAt: null }).version).toBe(0);
  expect(() => organizationSupportPreferences({ enabled: true, eventTypes: [], version: 0, updatedAt: null })).toThrow();
  expect(() => organizationSupportPreferences({ enabled: true, eventTypes: ['REPLY', 'REPLY'], version: 1, updatedAt: time })).toThrow();
  const notice = { id, organizationId: org, ticketId: id, eventId: actor, type: 'REPLY', createdAt: time, readAt: null, version: 1 };
  expect(organizationSupportNotice(notice, org).version).toBe(1);
  expect(() => organizationSupportNotice({ ...notice, organizationId: id }, org)).toThrow();
  expect(() => organizationSupportNotice({ ...notice, version: 2 }, org)).toThrow();
});
test('Support permissions never expand Team role templates or invitation delegation', () => {
  for (const permission of ['support.read', 'support.write', 'support.assign', 'support.reports.read', 'support.notifications.manage']) {
    const templates = { items: ['OWNER', 'MANAGER', 'OPERATOR'].map(role => ({ role, version: 1, allowedPermissions: ['dashboard.read', permission] })) };
    expect(() => teamRoleTemplates(templates)).toThrow();
    expect(() => teamInvitation({ id, maskedEmail: 's***@example.invalid', role: 'OPERATOR', grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }, { permission, scope: 'ORGANIZATION', facilityId: null }], state: 'PENDING', version: 1, createdAt: time, expiresAt: '2026-11-20T00:00:00Z', deliveryState: 'QUEUED' })).toThrow();
  }
});
