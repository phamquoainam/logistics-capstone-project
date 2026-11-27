import { test, expect } from '@playwright/test';
import { parseCasePage, parseCasePolicies, parseOrganizationCase, parseCaseDisclosure } from '../src/admin/organization-cases';
import { createAdminSession } from '../src/admin/session';

const org = '10000000-0000-4000-8000-000000000001', id = '20000000-0000-4000-8000-000000000001';
const row = { id, organizationId: org, type: 'CLOSE', state: 'DRAFT', version: 1, policyId: id, policyVersion: 1, organizationVersion: 1, makerActorKind: 'SYSTEM', makerAccountId: id, reviewerAccountId: null, deciderAccountId: null, executorAccountId: null, createdAt: '2026-11-26T00:00:00Z', expiresAt: '2026-12-03T00:00:00Z', effectiveAt: null, allowedActions: ['SUBMIT', 'CANCEL'], lastEventId: org, isExpired: false, asOf: '2026-11-26T00:01:00Z' };
test('case projection denies foreign ownership, unknown authority and malformed snapshots', () => {
  expect(parseOrganizationCase(row, org)).toEqual(row);
  for (const value of [{ ...row, organizationId: id }, { ...row, allowedActions: ['EXECUTE'] }, { ...row, isExpired: true }, { ...row, version: 0 }, { ...row, privatePayload: 'denied' }, { ...row, expiresAt: 'invalid' }]) expect(() => parseOrganizationCase(value, org)).toThrow();
  expect(() => parseCasePage({ items: [row, row], nextCursor: null }, org)).toThrow();
  expect(parseCasePage({ items: [], nextCursor: null }, org).items).toEqual([]);
});
test('policy metadata preserves unavailable facts without inventing policy or proof limits', () => {
  expect(parseCasePolicies({ items: [] })).toEqual([]);
  const policy = { policyId: id, code: 'ORG_GOVERNANCE_1', version: 1, allowedCaseTypes: ['CLOSE'], validFrom: row.createdAt, validTo: null, limits: { retention_policy_reference: null }, requiredChecklists: ['authority'], proofAge: null, partyConsentTTL: 86400, readPreservation: {} };
  expect(parseCasePolicies({ items: [policy] })[0].policyId).toBe(id);
  expect(() => parseCasePolicies({ items: [{ ...policy, allowedCaseTypes: ['ANYTHING'] }] })).toThrow();
});
test('private disclosure is bound to exact source version and an allowlisted payload', () => {
  const current = parseOrganizationCase(row, org), value = { caseId: id, organizationId: org, version: 1, revision: 1, casePayload: {}, reason: 'Synthetic review reason', requestedFieldCodes: ['reason'], sourceEventId: org, disclosedAt: row.asOf };
  expect(parseCaseDisclosure(value, current)).toEqual(value);
  for (const invalid of [{ ...value, organizationId: id }, { ...value, version: 2 }, { ...value, sourceEventId: id }, { ...value, casePayload: { proof: 'denied' } }, { ...value, token: 'denied' }, { ...value, requestedFieldCodes: ['privateCredential'] }]) expect(() => parseCaseDisclosure(invalid, current)).toThrow();
});
test('case transport uses exact Organization paths, retained write intent and denied arbitrary operation', async () => {
  const calls: { path: string; init: RequestInit }[] = [];let lost = true;
  const now = Date.parse(row.createdAt), user = { id, username: 'synthetic', displayName: 'Synthetic Admin', email: 'synthetic@example.invalid', status: 'ACTIVE', version: 1, emailVerified: true, grants: [{ role: 'ADMIN_DASHBOARD_READ', scopeType: 'IDENTITY', scopeId: org }, { role: 'ADMIN_MERCHANT_READ', scopeType: 'IDENTITY', scopeId: org }] };
  const client = createAdminSession({ now: () => now, protocol: () => 'https:', lock: work => work(), fetch: async (url, init = {}) => {
    const path = String(url);calls.push({ path, init });
    if (path.endsWith('/csrf')) return Response.json({ csrfToken: 'synthetic-csrf-challenge-123456' });
    if (path.endsWith('/me')) return Response.json(user);
    if (path.endsWith('/login')) return Response.json({ accessToken: 'a'.repeat(128), tokenType: 'Bearer', expiresIn: 900, sessionExpiresAt: new Date(now + 28800000).toISOString(), user });
    if (path.endsWith('/submission') && lost) { lost = false;throw new Error('SYNTHETIC_LOST_RESPONSE'); }
    return Response.json(path.endsWith('/governance-policies') ? { items: [] } : row);
  } });
  await client.login('synthetic', 'synthetic-password');
  const intent = { caseId: id, version: 1, key: org, body: { reason: 'Synthetic submission' } };
  await expect(client.organizationCase(org, 'submission', v => parseOrganizationCase(v, org), intent)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' });
  await client.organizationCase(org, 'submission', v => parseOrganizationCase(v, org), intent);
  const writes = calls.filter(c => c.path.endsWith('/submission'));expect(writes).toHaveLength(2);expect(writes[0].init.headers).toEqual(writes[1].init.headers);expect(writes[0].init.body).toEqual(writes[1].init.body);expect(writes[0].init.headers).toMatchObject({ 'Idempotency-Key': org, 'If-Match': '"v1"' });
  const before = calls.length;await expect(client.organizationCase(org, 'execution' as 'submission', v => v, intent)).rejects.toMatchObject({ code: 'VALIDATION_FAILED' });expect(calls).toHaveLength(before);
  expect(await client.organizationCase(org, 'policies', parseCasePolicies)).toEqual([]);
  expect(calls.at(-1)?.path).toBe('/api/v1/admin/merchant-organizations/' + org + '/governance-policies');
  expect(writes.every(c => c.init.credentials === 'same-origin' && c.init.referrerPolicy === 'no-referrer' && c.init.redirect === 'error')).toBe(true);
});
