import { expect, test } from '@playwright/test';
import { currentPolicyVersion, parseApplication, parseApplicationPage, parseGovernanceContext, parseReviewers, parseApplicationEvents, parseOrganization, parseOrganizationPage } from '../src/admin/governance';

const actor = '10000000-0000-4000-8000-000000000001', realm = '20000000-0000-4000-8000-000000000001', policy = '30000000-0000-4000-8000-000000000001', app = '40000000-0000-4000-8000-000000000001';
const summary = { id: app, state: 'SUBMITTED', revision: 2, version: 8, legalNameMasked: 'S***', legalIdentifierMasked: '***1234', policyId: policy, reviewerId: null, firstSubmittedAt: '2026-11-24T00:00:00Z', updatedAt: '2026-11-24T01:00:00Z', activation: null, allowedActions: ['ASSIGN_REVIEWER'] };

test('audit metadata binds exact target and page without protected content or duplicate events', () => {
  const row = { id: actor, actorId: realm, operation: 'READY_FOR_DECISION', targetType: 'APPLICATION', targetId: app, priorVersion: 8, newVersion: 9, revision: 2, policyVersion: '7', reasonCode: 'REVIEW_COMPLETED', occurredAt: '2026-11-24T00:00:00Z', requestId: policy };
  const page = { items: [row], page: 0, size: 20, hasNext: false };expect(parseApplicationEvents(page, app, 0)).toEqual(page);
  expect(() => parseApplicationEvents({ ...page, items: [row, row] }, app, 0)).toThrow();
  expect(() => parseApplicationEvents({ ...page, items: [{ ...row, targetId: policy }] }, app, 0)).toThrow();
  expect(() => parseApplicationEvents({ ...page, items: [{ ...row, protectedPayload: 'private' }] }, app, 0)).toThrow();
  expect(() => parseApplicationEvents({ ...page, hasNext: true }, app, 0)).toThrow();
});
test('governance context is exact own authority with finite scoped capabilities', () => {
  const context = { accountId: actor, realmId: realm, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['application.read', 'application.assign'], version: 1, expiresAt: '2026-11-24T04:00:00Z' }] };
  expect(parseGovernanceContext(context, actor)).toEqual(context);
  expect(() => parseGovernanceContext({ ...context, accountId: realm }, actor)).toThrow();
  expect(() => parseGovernanceContext({ ...context, assignments: [{ ...context.assignments[0], permissions: ['*'] }] }, actor)).toThrow();
  expect(() => parseGovernanceContext({ ...context, assignments: [{ ...context.assignments[0], organizationId: realm }] }, actor)).toThrow();
});
test('queue rejects duplicate application rows, extra fields and foreign detail target', () => {
  expect(parseApplication(summary, app)).toEqual(summary);
  expect(() => parseApplication(summary, policy)).toThrow();
  expect(() => parseApplication({ ...summary, email: 'synthetic@example.invalid' })).toThrow();
  expect(parseApplicationPage({ items: [summary], page: 0, size: 20, hasNext: false }, 0).items).toHaveLength(1);
  expect(() => parseApplicationPage({ items: [summary, summary], page: 0, size: 20, hasNext: false }, 0)).toThrow();
  expect(() => parseApplicationPage({ items: [summary], page: 0, size: 20, hasNext: true }, 0)).toThrow();
});
test('policy version comes from exact immutable revision metadata; no default version', () => {
  const row = { applicationId: app, revision: 2, submittedAt: '2026-11-24T00:00:00Z', policyVersion: '7', organizationDisplayName: 'Synthetic display', digest: 'b'.repeat(64), evidenceIds: [actor, realm], contactVerifiedAt: '2026-11-23T23:59:00Z' };
  const page = { items: [row], page: 0, size: 20, hasNext: false };
  expect(currentPolicyVersion(page, app, 2)).toBe('7');
  expect(() => currentPolicyVersion(page, app, 1)).toThrow();
  expect(() => currentPolicyVersion({ ...page, items: [{ ...row, applicationId: realm }] }, app, 2)).toThrow();
});
test('candidate projection admits only bounded current server candidates, not role titles or credentials', () => {
  expect(parseReviewers({ items: [{ accountId: actor, displayName: 'Synthetic reviewer' }], page: 0, size: 20, hasNext: false }, 0).items[0].accountId).toBe(actor);
  expect(() => parseReviewers({ items: [{ accountId: actor, displayName: 'Synthetic reviewer', permissions: ['application.decide'] }], page: 0, size: 20, hasNext: false }, 0)).toThrow();
});

test('organization directory distinguishes immutable activation from current state and rejects private counts', () => {
  const value = { id: realm, displayName: 'Synthetic organization', state: 'SUSPENDED', admissionOrigin: 'APPLICATION_APPROVED', version: 2, initialOwnerMembershipId: actor, activation: { applicationId: app, revision: 2, decisionEventId: policy, organizationId: realm, ownerMembershipId: actor, state: 'ACTIVE', activatedAt: '2026-11-24T00:00:00Z' }, membershipCounts: { OWNER: { ACTIVE: 1 } }, allowedActions: ['REINSTATE'] };
  expect(parseOrganization(value, realm)).toEqual(value);
  expect(() => parseOrganization({ ...value, activation: null })).toThrow();
  expect(() => parseOrganization({ ...value, allowedActions: ['SUSPEND'] })).toThrow();
  expect(() => parseOrganization({ ...value, membershipCounts: { CUSTOMER_EMAIL: { ACTIVE: 1 } } })).toThrow();
  expect(() => parseOrganization({ ...value, activation: { ...value.activation, organizationId: app } })).toThrow();
  expect(() => parseOrganizationPage({ items: [value, value], page: 0, size: 20, hasNext: false }, 0)).toThrow();
  expect(parseOrganization({ ...value, activation: null, admissionOrigin: 'LOCAL_OPERATOR_PROVISIONED', allowedActions: [] })).toBeTruthy();
});
