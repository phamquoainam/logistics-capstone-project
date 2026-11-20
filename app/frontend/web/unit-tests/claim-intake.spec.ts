import { expect, test } from '@playwright/test';
import { claimDetail, claimPage, claimReport, claimNotices } from '../src/customer/claim-intake-data';

const id = '10000000-0000-4000-8000-000000000001', org = '20000000-0000-4000-8000-000000000001';
const summary = { id, organizationId: org, facilityId: null, ticketId: id, shipmentId: id, creatorAccountId: id, category: 'DAMAGE', state: 'RECEIVED', version: 1, createdAt: '2026-11-20T00:00:00Z', updatedAt: '2026-11-20T00:00:00Z', eligibility: 'UNASSESSED', compensation: 'BLOCKED' };
const detail = { intake: summary, events: [{ id, authorKind: 'MERCHANT', operation: 'CREATE', message: 'Synthetic intake', version: 1, occurredAt: '2026-11-20T00:00:00Z' }], allowedActions: ['SUPPLEMENT'] };
test('Claim response remains bound to Organization and Ticket with no financial authority', () => {
  expect(claimDetail(detail, org, id).intake.compensation).toBe('BLOCKED');
  expect(claimPage({ items: [summary], page: 0, size: 20, hasNext: false }, org, id, 0).items).toHaveLength(1);
  for (const intake of [{ ...summary, organizationId: id }, { ...summary, compensation: 'APPROVED' }, { ...summary, state: 'PAID' }, { ...summary, eligibility: 'ELIGIBLE' }]) expect(() => claimDetail({ ...detail, intake }, org)).toThrow('CLAIM_RESPONSE_INVALID');
  expect(() => claimPage({ items: [summary], page: 0, size: 20, hasNext: false }, org, org, 0)).toThrow();
});
test('Claim analytics reject inconsistent cohorts, fictitious SLA and fabricated duration samples', () => {
  const empty = { sampleCount: '0', totalMs: '0', minMs: null, maxMs: null }, one = { sampleCount: '1', totalMs: '2500', minMs: '2500', maxMs: '2500' };
  const report = { schemaVersion: 'CLAIM_INTAKE_REPORT_1', organizationId: org, generatedAt: '2026-11-20T00:00:00Z', total: '1', byState: { RECEIVED: '0', UNDER_REVIEW: '0', NEEDS_INFORMATION: '1', CLOSED_WITHOUT_DECISION: '0' }, byCategory: { LOSS: '0', DAMAGE: '1', DELAY: '0', SERVICE_DISPUTE: '0' }, cleanEvidence: '1', firstStaffResponse: one, intakeClosure: empty, slaState: 'UNPUBLISHED', compensation: 'BLOCKED' };
  expect(claimReport(report, org).total).toBe('1');
  for (const bad of [{ ...report, total: '2' }, { ...report, slaState: 'ON_TIME' }, { ...report, compensation: 'PAID' }, { ...report, firstStaffResponse: { ...one, totalMs: '0' } }, { ...report, intakeClosure: one }]) expect(() => claimReport(bad, org)).toThrow();
  const notice = { id, intakeId: id, ticketId: id, eventId: id, type: 'REVIEW', createdAt: '2026-11-20T00:00:00Z' }; expect(claimNotices({ items: [notice], page: 0, hasNext: false }, 0).items).toHaveLength(1); expect(() => claimNotices({ items: [notice, notice], page: 0, hasNext: false }, 0)).toThrow();
});
test('Claim history and actions reject fabricated, duplicated or out-of-order state', () => {
  for (const changed of [{ ...detail, events: [] }, { ...detail, allowedActions: ['APPROVE'] }, { ...detail, allowedActions: ['SUPPLEMENT', 'SUPPLEMENT'] }, { ...detail, events: [{ ...detail.events[0], version: 2 }] }, { ...detail, events: [{ ...detail.events[0], operation: 'PAY' }] }]) expect(() => claimDetail(changed, org)).toThrow('CLAIM_RESPONSE_INVALID');
});
