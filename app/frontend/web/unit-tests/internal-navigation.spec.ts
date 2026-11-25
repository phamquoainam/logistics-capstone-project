import { test, expect } from '@playwright/test';
import { canOpenOverview, overviewEntries, overviewSelection, adminGroups, adminModules, adminTabs, canOpenModule, canSwitchPortal, resolveAdminModule, sectionState, snapshotState } from '../src/internal/navigation';
import type { AdminContext, Section } from '../src/admin/data';

const entry: AdminContext['permissions'] = ['ADMIN_DASHBOARD_READ'];
test('one internal navigation retains eight parent groups and all twenty-three source module IDs', () => {
  expect(adminGroups).toHaveLength(8);
  expect(adminModules.map(m => m.id)).toEqual(Array.from({ length: 23 }, (_, n) => `ADM-${String(n + 1).padStart(2, '0')}`));
  expect(new Set(adminModules.map(m => m.path)).size).toBe(23);
  expect(new Set(adminGroups.flatMap(g => g.modules))).toEqual(new Set(adminModules.map(m => m.id)));
  expect(adminModules.filter(m => m.extension).map(m => m.id)).toHaveLength(6);
});
test('known read permissions do not imply IAM write pricing approval or another execution portal', () => {
  const all: AdminContext['permissions'] = [...entry, 'ADMIN_MERCHANT_READ', 'ADMIN_NETWORK_READ', 'ADMIN_SHIPMENT_READ', 'ADMIN_FINANCE_READ', 'ADMIN_TECHNICAL_READ'];
  expect(canOpenModule('ADM-11', entry)).toBe(false);
  expect(canOpenModule('ADM-14', ['ADMIN_DASHBOARD_READ', 'ADMIN_TECHNICAL_READ'])).toBe(true);
  for (const id of ['ADM-07', 'ADM-12', 'ADM-13', 'ADM-16', 'ADM-18']) expect(canOpenModule(id, all)).toBe(false);
  for (const source of ['ops', 'finance', 'admin'] as const) for (const target of ['ops', 'finance', 'admin'] as const) expect(canSwitchPortal(source, target)).toBe(source === target);
});
test('dashboard tabs require their current granular read grant and no role label grants access', () => {
  expect(adminTabs(entry).map(t => t.id)).toEqual(['overview', 'risk', 'decisions']);
  expect(adminTabs([...entry, 'ADMIN_FINANCE_READ']).map(t => t.id)).toEqual(['overview', 'finance', 'risk', 'decisions']);
  expect(resolveAdminModule('/admin/dashboard')?.id).toBe('ADM-01');
  expect(resolveAdminModule('/admin/shipments')?.id).toBe('ADM-08');
  for (const path of ['/admin/unknown', '/admin//shipments', '/ops', '/admin/shipments/foreign-id']) expect(resolveAdminModule(path)).toBeUndefined();
});
test('empty available data stays distinct from unavailable forbidden and degraded sources', () => {
  const section = { key: 'SHIPMENTS', availability: 'AVAILABLE', metrics: [{ key: 'BOOKINGS', value: '0' }], series: [] } as unknown as Section;
  expect(sectionState(true, section)).toBe('EMPTY');
  expect(sectionState(true, { ...section, metrics: [{ ...section.metrics[0], value: '2' }] })).toBe('AVAILABLE');
  expect(sectionState(true, { ...section, availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] })).toBe('UNAVAILABLE');
  expect(sectionState(false, section)).toBe('FORBIDDEN');
  expect(sectionState(true, section, true)).toBe('DEGRADED');
});
test('freshness uses the actual snapshot cutoff and never invents a last source event or metric rate', () => {
  const now = Date.parse('2026-11-23T04:00:00Z');
  expect(snapshotState('2026-11-23T04:00:00Z', now)).toBe('CURRENT');
  expect(snapshotState('2026-11-23T03:45:00Z', now)).toBe('STALE');
  expect(snapshotState('invalid', now)).toBe('STALE');
});

test('overview maps four views without granting Finance Operations or general policy writes', () => {
  expect(overviewEntries.map(v => v.id)).toEqual(['executive', 'approvals', 'risk', 'health']);
  expect(canOpenOverview('health', ['ADMIN_DASHBOARD_READ'], [])).toBe(false);
  expect(canOpenOverview('approvals', ['ADMIN_DASHBOARD_READ'], ['application.read'])).toBe(false);
  expect(canOpenOverview('approvals', ['ADMIN_DASHBOARD_READ'], ['application.read', 'task.read'])).toBe(true);
  expect(overviewSelection('/admin', '?tab=risk')).toBe('risk');expect(overviewSelection('/admin/account', '?tab=approvals')).toBe('approvals');
  expect(canOpenModule('ADM-18', ['ADMIN_DASHBOARD_READ'], [])).toBe(false);
  expect(canOpenModule('ADM-18', ['ADMIN_DASHBOARD_READ'], ['application.read'])).toBe(true);
});
