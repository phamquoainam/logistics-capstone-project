import { test, expect } from '@playwright/test';
import { parseTask, parseTaskDetail, parseTaskPage, parseTaskNotice } from '../src/customer/organization-actions-data';
const org = '10000000-0000-4000-8000-000000000011', id = '10000000-0000-4000-8000-000000000012';
const task = { id, organizationId: org, facilityId: null, targetKind: 'SHIPMENT', targetId: org, type: 'SHIPMENT_FOLLOW_UP', title: 'Synthetic follow-up', state: 'OPEN', priority: 'NORMAL', assigneeAccountId: null, dueAt: null, initialDueAt: null, version: 1, createdAt: '2026-11-15T00:00:00Z', updatedAt: '2026-11-15T00:00:00Z', completedAt: null, overdue: false, allowedActions: ['ASSIGNMENT', 'COMMENT', 'CANCEL', 'POLICY'] };
test('internal task projection rejects Carrier states, foreign Organizations and mismatched sources', () => {
  expect(parseTask(task, org).state).toBe('OPEN');
  for (const bad of [{ ...task, organizationId: id }, { ...task, type: 'PREPARATION_REVIEW' }, { ...task, state: 'DELIVERED' }, { ...task, allowedActions: ['PAID'] }, { ...task, completedAt: task.createdAt }, { ...task, state: 'CANCELLED' }]) expect(() => parseTask(bad, org)).toThrow();
});
test('task cohort counts and monotonic event history must match the actual returned scope', () => {
  const page = { items: [task], page: 0, size: 20, hasNext: false, total: 1, counts: { OPEN: 1, IN_PROGRESS: 0, AWAITING_REVIEW: 0, COMPLETED: 0, CANCELLED: 0 }, generatedAt: task.createdAt };
  expect(parseTaskPage(page, org, 0).total).toBe(1); expect(() => parseTaskPage({ ...page, total: 2 }, org, 0)).toThrow();
  const detail = { task, details: 'Synthetic description', events: [{ id, operation: 'CREATE', actorAccountId: org, message: null, version: 1, occurredAt: task.createdAt }] }; expect(parseTaskDetail(detail, org).events).toHaveLength(1); expect(() => parseTaskDetail({ ...detail, events: [] }, org)).toThrow(); expect(() => parseTaskDetail({ ...detail, events: [{ ...detail.events[0], version: 2 }] }, org)).toThrow();
});
test('in-app notices contain only own task metadata and never embedded contact or finance data', () => {
  const notice = { id, organizationId: org, taskId: id, eventId: org, type: 'ASSIGNMENT', createdAt: task.createdAt, readAt: null }; expect(parseTaskNotice(notice, org).readAt).toBeNull(); expect(() => parseTaskNotice({ ...notice, email: 'synthetic@example.invalid' }, org)).toThrow(); expect(() => parseTaskNotice({ ...notice, type: 'CARRIER_DELIVERED' }, org)).toThrow();
});
