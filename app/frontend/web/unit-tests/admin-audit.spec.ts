import { test, expect } from '@playwright/test';
import { parseGovernanceAudit } from '../src/admin/audit-data';
import { canOpenModule } from '../src/internal/navigation';
const id = '10000000-0000-4000-8000-000000000001';
const row = { id, actorId: id, operation: 'REJECT', targetType: 'APPLICATION', targetId: id, priorVersion: 6, newVersion: 7, revision: 1, policyVersion: '1', reasonCode: 'INDEPENDENT_REJECTION', occurredAt: '2026-11-24T01:00:00Z', requestId: id };
const page = { items: [row], page: 0, size: 20, hasNext: false };
test('audit metadata rejects private fields, foreign domain/target, duplicates and invalid pagination', () => {
  expect(parseGovernanceAudit(page, 'MERCHANT_APPLICATION', 0, id)).toEqual(page);
  for (const value of [{ ...page, items: [{ ...row, protectedPayload: 'private' }] }, { ...page, items: [row, row] }, { ...page, hasNext: true }, { ...page, items: [{ ...row, operation: 'PAYMENT_COLLECTED' }] }]) expect(() => parseGovernanceAudit(value, 'MERCHANT_APPLICATION', 0)).toThrow();
  expect(() => parseGovernanceAudit(page, 'MERCHANT_ORGANIZATION', 0)).toThrow();
  expect(() => parseGovernanceAudit(page, 'MERCHANT_APPLICATION', 0, '20000000-0000-4000-8000-000000000001')).toThrow();
  const organization = { ...row, operation: 'SUSPEND', targetType: 'ORGANIZATION', revision: null };
  expect(parseGovernanceAudit({ ...page, items: [organization] }, 'MERCHANT_ORGANIZATION', 0).items[0].operation).toBe('SUSPEND');
});
test('audit navigation needs explicit domain capability as well as entry, never a role-title or Merchant READ inference', () => {
  expect(canOpenModule('ADM-16', ['ADMIN_DASHBOARD_READ'], ['audit.read'])).toBe(true);
  expect(canOpenModule('ADM-16', ['ADMIN_DASHBOARD_READ', 'ADMIN_MERCHANT_READ'])).toBe(false);
  expect(canOpenModule('ADM-16', [], ['audit.read'])).toBe(false);
  expect(canOpenModule('ADM-02', ['ADMIN_DASHBOARD_READ'], ['audit.read'])).toBe(false);
});
