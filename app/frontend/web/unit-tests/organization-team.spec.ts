import { test, expect } from '@playwright/test';
import { currentTeamContext, teamCapabilities, teamCapabilityGroups } from '../src/customer/organization-team';
import { businessPermissions } from '../src/customer/business-data';
import { businessContextsFixture } from '../ui-unit-tests/support/business-fixture';

test('self context requires exact Organization, Membership and nonstale authority', () => {
  const expected = businessContextsFixture[0];
  expect(currentTeamContext(businessContextsFixture, expected)).toBe(expected);
  expect(() => currentTeamContext([], expected)).toThrow('BUSINESS_CONTEXT_UNAVAILABLE');
  expect(() => currentTeamContext([businessContextsFixture[1]], expected)).toThrow('BUSINESS_CONTEXT_UNAVAILABLE');
  expect(() => currentTeamContext([expected, expected], expected)).toThrow('BUSINESS_CONTEXT_UNAVAILABLE');
  expect(() => currentTeamContext([{ ...expected, membershipId: businessContextsFixture[1].membershipId }], expected)).toThrow('BUSINESS_CONTEXT_UNAVAILABLE');
  expect(() => currentTeamContext([{ ...expected, authorityVersion: 1 }], { ...expected, authorityVersion: 2 })).toThrow('BUSINESS_CONTEXT_UNAVAILABLE');
  const reduced = { ...expected, authorityVersion: 2, permissions: ['dashboard.read' as const] };
  expect(currentTeamContext([reduced], expected)).toEqual(reduced);
});
test('capability projection never derives admin or facility rights from the role', () => {
  expect(Object.keys(teamCapabilities).sort()).toEqual([...businessPermissions].sort());
  const owner = { ...businessContextsFixture[0], permissions: ['dashboard.read' as const] };
  expect(teamCapabilityGroups(owner)).toEqual([{ group: 'Báo cáo', items: [{ permission: 'dashboard.read', label: 'Xem tổng quan doanh nghiệp' }] }]);
  expect(teamCapabilityGroups(owner, 'tài chính')).toEqual([]);
  expect(teamCapabilityGroups(businessContextsFixture[0], 'dashboard.finance.read')[0].items).toHaveLength(1);
  expect(teamCapabilityGroups(businessContextsFixture[0], '  TÀI CHÍNH  ')[0].group).toBe('Tài chính');
});
