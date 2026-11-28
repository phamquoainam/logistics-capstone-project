import { expect, test } from '@playwright/test';
import { applicationSearchPage, comment, commentPage, comparison, members, profile } from '../src/admin/review-views';
import { parseGovernanceContext } from '../src/admin/governance';
const id = '10000000-0000-4000-8000-000000000001', app = '20000000-0000-4000-8000-000000000001', other = '30000000-0000-4000-8000-000000000001';
test('encrypted comment projection is exact-target and rejects private metadata, duplicate rows and cross-target text', () => {
  const row = { id, applicationId: app, revision: 1, actorAccountId: other, createdAt: '2026-11-26T00:00:00Z', text: '<script>literal text</script>' };
  expect(comment(row, app)).toEqual(row);expect(() => comment(row, other)).toThrow();expect(() => comment({ ...row, protectedText: 'secret' }, app)).toThrow();expect(() => commentPage({ items: [row, row], nextCursor: null, disclosureEventId: other }, app)).toThrow();
});
test('comparison binds immutable revisions, approved fields and source digests; foreign or draft fields are denied', () => {
  const row = { applicationId: app, fromRevision: 1, toRevision: 2, fromDigest: 'a'.repeat(64), toDigest: 'b'.repeat(64), changes: [{ field: 'registeredName', before: 'Synthetic 1', after: 'Synthetic 2' }], evidenceAdded: [], evidenceRemoved: [], contactVerifiedAt: { from: '2026-11-25T00:00:00Z', to: '2026-11-26T00:00:00Z' }, disclosureEventId: id };
  expect(comparison(row, app)).toEqual(row);expect(() => comparison(row, other)).toThrow();expect(() => comparison({ ...row, changes: [{ field: 'password', before: null, after: 'bad' }] }, app)).toThrow();expect(() => comparison({ ...row, toRevision: 1 }, app)).toThrow();
});
test('membership oversight rejects recipient emails and credentials; source-less profile cannot claim AVAILABLE', () => {
  const member = { membershipId: id, accountId: other, role: 'OWNER', state: 'ACTIVE', version: 1, authorityVersion: 1, isInitialOwner: true, isCurrentOwner: true };
  const row = { organizationId: app, organizationVersion: 1, items: [member], pendingInvitationCount: 0, nextCursor: null, disclosureEventId: other };
  expect(members(row, app).items).toEqual([member]);expect(() => members({ ...row, items: [{ ...member, email: 'not-allowed@example.invalid' }] }, app)).toThrow();
  const block = { availability: 'UNAVAILABLE', sourceVersion: null, asOf: null, reason: 'SOURCE_NOT_IMPLEMENTED' };
  const p = { organizationId: app, organizationVersion: 1, initialOwnerMembershipId: id, currentOwnerMembershipId: id, activationApplicationId: null, legal: block, contact: block, facilities: block, pricing: block, holds: block, disclosureEventId: other };
  expect(profile(p, app).pricing.availability).toBe('UNAVAILABLE');expect(() => profile({ ...p, pricing: { ...block, availability: 'AVAILABLE' } }, app)).toThrow();
});
test('search cohort/cursor validation and newly approved capabilities remain bounded', () => {
  const row = { items: [], matchedCount: 0, nextCursor: null, snapshotVersion: 'a'.repeat(64) };expect(applicationSearchPage(row).matchedCount).toBe(0);expect(() => applicationSearchPage({ ...row, matchedCount: -1 })).toThrow();expect(() => applicationSearchPage({ ...row, nextCursor: 'raw private query=' })).toThrow();
  const context = { accountId: id, realmId: other, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: other, organizationId: null, permissions: ['application.read', 'application.comment.write'], version: 1, expiresAt: '2026-11-26T12:00:00Z' }] };expect(parseGovernanceContext(context, id)).toEqual(context);expect(() => parseGovernanceContext({ ...context, assignments: [{ ...context.assignments[0], permissions: ['org.ownership.execute'] }] }, id)).toThrow();
});
