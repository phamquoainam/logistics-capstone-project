import { test, expect } from '@playwright/test';
import { adminAccount, adminContext, adminReport } from '../ui-unit-tests/support/admin-data';
import { parseContextV2, parseDashboardV2, parseApplicationsV2 } from '../src/admin/v2-data';
import type { QueryV2, DashboardV2 } from '../src/admin/v2-data';
import { parseWorkPage } from '../src/admin/MerchantWork';
const query: QueryV2 = { from: '2026-11-01', to: '2026-11-02', grain: 'DAY', shipmentStatus: null, regionId: null, applicationState: null, policyId: null };
function report(): DashboardV2 {
  const old = adminReport(query), at = old.generatedAt;
  const keys = ['APPLICATIONS_SUBMITTED', 'APPLICATIONS_UNDER_REVIEW', 'APPLICATIONS_NEEDS_INFORMATION', 'APPLICATIONS_APPROVED', 'APPLICATIONS_REJECTED', 'APPLICATIONS_WITHDRAWN', 'APPLICATION_PROCESSING_TOTAL_MS', 'APPLICATION_PROCESSING_AVG_MS', 'APPLICATION_PROCESSING_MIN_MS', 'APPLICATION_PROCESSING_MAX_MS'];
  const merchant = { key: 'MERCHANT', availability: 'AVAILABLE', reasonCode: 'CANONICAL_FIRST_SUBMITTED_COHORT', metrics: keys.map((key, index) => ({ key, value: index === 0 ? '2' : index === 1 || index === 3 ? '1' : index >= 6 ? '2000' : '0', unit: index >= 6 ? 'MILLISECOND' : 'APPLICATION', drilldown: { path: '/api/v1/admin/dashboard/merchant-applications', filters: { from: query.from, to: query.to, grain: query.grain }, metric: key } })), series: keys.slice(0, 6).flatMap((key, i) => [{ from: query.from, to: '2026-11-02', metric: key, value: '0' }, { from: '2026-11-02', to: '2026-11-03', metric: key, value: i === 0 ? '2' : i === 1 || i === 3 ? '1' : '0' }]) };
  const sections = [...old.sections.filter(s => s.key !== 'MERCHANT'), merchant];
  return { ...old, query, schemaVersion: 'ADMIN_DASHBOARD_2', resourceScopeVersion: '1'.repeat(64), sections, metricMetadata: sections.flatMap(s => s.metrics.map(m => ({ metricKey: m.key, definitionVersion: 'ARGO_ADMIN_METRICS_2', cohortKind: m.key.startsWith('APPLICATION') ? 'FIRST_SUBMITTED_APPLICATION' : 'BOOKED_SHIPMENT', timeZone: 'Asia/Ho_Chi_Minh', numerator: m.value, denominator: m.key === 'APPLICATION_PROCESSING_AVG_MS' ? '1' : null, unit: m.unit, sampleCount: m.key.startsWith('APPLICATION_PROCESSING') ? '1' : null, lastSourceEventAt: at, sourceCheckedAt: at, freshness: 'CURRENT_TRANSACTION', availabilityReason: null }))) };
}
test('v2 canonical metadata and safe application drill-through reject changed scope, private fields and calendar drift', () => {
  const v = report();expect(parseDashboardV2(v, adminContext(), query)).toEqual(v);
  const metric = 'APPLICATIONS_APPROVED', metadata = v.metricMetadata.filter(m => m.metricKey === metric);
  const page = { schemaVersion: v.schemaVersion, items: [{ id: '30000000-0000-4000-8000-000000000001', state: 'APPROVED', submittedAt: '2026-11-01T00:00:00Z', updatedAt: v.generatedAt }], page: 0, size: 20, hasNext: false, snapshotId: v.snapshotId, resourceScopeVersion: v.resourceScopeVersion, metricMetadata: metadata };
  expect(parseApplicationsV2(page, v, metric, 0)).toEqual(page);
  for (const item of [{ ...page, resourceScopeVersion: '2'.repeat(64) }, { ...page, items: [{ ...page.items[0], email: 'private@example.invalid' }] }, { ...page, items: [{ ...page.items[0], state: 'REJECTED' }] }]) expect(() => parseApplicationsV2(item, v, metric, 0)).toThrow();
  const wrong = structuredClone(v);wrong.sections.find(s => s.key === 'MERCHANT')!.series[0].to = '2026-11-03';expect(() => parseDashboardV2(wrong, adminContext(), query)).toThrow();
});
test('v2 duration denominators, missing min/max, unknown schema and absent scope fail closed', () => {
  for (const mutate of [(v: DashboardV2) => { v.metricMetadata.find(m => m.metricKey === 'APPLICATION_PROCESSING_AVG_MS')!.denominator = '0'; }, (v: DashboardV2) => { v.sections.find(s => s.key === 'MERCHANT')!.metrics.pop();v.metricMetadata.pop(); }, (v: DashboardV2) => { v.resourceScopeVersion = ''; }]) { const v = report();mutate(v);expect(() => parseDashboardV2(v, adminContext(), query)).toThrow(); }
  expect(() => parseContextV2({ ...adminContext(), schemaVersion: 'ADMIN_DASHBOARD_2', resourceScopeVersion: 'a'.repeat(64), resourceScopes: [] }, adminAccount)).not.toThrow();
});
test('work projection permits only canonical navigation and actions without arbitrary tasks or deadlines', () => {
  const app = '30000000-0000-4000-8000-000000000001';const item = { id: '40000000-0000-4000-8000-000000000001', kind: 'MERCHANT_DECISION', applicationId: app, revision: 1, assignmentVersion: 1, createdAt: '2026-11-01T00:00:00Z', dueAt: null, targetPath: '/admin/merchants/applications?applicationId=' + app, allowedActions: ['APPROVE', 'REJECT'] };const page = { items: [item], page: 0, size: 20, hasNext: false };
  expect(parseWorkPage(page, 0, '')).toEqual(page);
  for (const bad of [{ ...item, dueAt: '2026-12-01' }, { ...item, targetPath: '/admin/users' }, { ...item, allowedActions: ['PAY'] }, { ...item, protectedPayload: 'private' }]) expect(() => parseWorkPage({ ...page, items: [bad] }, 0, '')).toThrow();
});
