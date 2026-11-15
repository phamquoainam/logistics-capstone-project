import { test, expect } from '@playwright/test';
import { actionSourceQuery, unassignedActionQuery } from '../src/customer/organization-action-center';

test('internal preparation work never silently ignores inherited shipment filters', () => {
  expect(actionSourceQuery(new URLSearchParams({ category: 'ACTION' })).draftComparable).toBe(true);
  const value = actionSourceQuery(new URLSearchParams({ category: 'ACTION', page: '3', facilityId: '10000000-0000-4000-8000-000000000001', sourceChannel: 'WEB' }));
  expect(value.valid).toBe(true); expect(value.draftComparable).toBe(false); expect(value.filters.has('page')).toBe(false); expect(value.filters.get('sourceChannel')).toBe('WEB');
});
test('unassigned work preserves exact scope selectors without mutating original list intent', () => {
  const original = new URLSearchParams({ from: '2026-11-01', until: '2026-11-15', sourceChannel: 'WEB' }), value = unassignedActionQuery(original, 2);
  expect(value.get('from')).toBe('2026-11-01'); expect(value.get('until')).toBe('2026-11-15'); expect(value.get('sourceChannel')).toBe('WEB'); expect(value.get('assignee')).toBe('NONE'); expect(value.get('page')).toBe('2'); expect(original.has('assignee')).toBe(false);
});
test('conflicting assignee requires an explicit filter change and forged selectors stay invalid', () => {
  const query = new URLSearchParams({ category: 'ACTION', assignee: '10000000-0000-4000-8000-000000000001' }); expect(actionSourceQuery(query).conflictingAssignee).toBe(true); expect(() => unassignedActionQuery(actionSourceQuery(query).filters, 0)).toThrow();
  for (const raw of ['category=ACTION&ownerId=other', 'category=ACTION&category=ALL', 'category=TRANSIT', 'category=ACTION&page=-1']) expect(actionSourceQuery(new URLSearchParams(raw)).valid, raw).toBe(false);
  expect(() => unassignedActionQuery(new URLSearchParams(), Infinity)).toThrow();
});
