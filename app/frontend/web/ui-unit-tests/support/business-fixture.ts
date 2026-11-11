import type { BusinessContext } from '../../src/customer/business-data';
import { businessChartTitles, businessMetrics } from '../../src/customer/business-data';
export const organizationA = '10000000-0000-4000-8000-000000000801', organizationB = '10000000-0000-4000-8000-000000000802';
export const businessContextsFixture: BusinessContext[] = [
  { organizationId: organizationA, displayName: 'Synthetic Store A', membershipId: '10000000-0000-4000-8000-000000000803', role: 'OWNER', permissions: ['dashboard.read', 'dashboard.finance.read', 'dashboard.staff.read'], authorityVersion: 1 },
  { organizationId: organizationB, displayName: 'Synthetic Store B', membershipId: '10000000-0000-4000-8000-000000000804', role: 'OPERATOR', permissions: ['dashboard.read'], authorityVersion: 3 },
];
export function businessReportFixture(context = businessContextsFixture[0], query = new URLSearchParams()) {
  const from = query.get('from') ?? '2026-11-01', to = query.get('to') ?? '2026-11-10', interval = query.get('interval') ?? 'DAY', finance = context.permissions.includes('dashboard.finance.read'), staff = context.permissions.includes('dashboard.staff.read');
  return { schemaVersion: 'BUSINESS_DASHBOARD_V1', context: { organizationId: context.organizationId, displayName: context.displayName, membershipId: context.membershipId, authorityVersion: context.authorityVersion }, generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', period: { from, to, interval, startInclusive: new Date(from + 'T00:00:00+07:00').toISOString(), endExclusive: new Date(Date.parse(to + 'T00:00:00+07:00') + 86400000).toISOString() },
    capabilities: ['SHIPPING', ...(finance ? ['FINANCE'] : [])].map(id => ({ id, state: 'SOURCE_NOT_CONNECTED', reasonCode: `MERCHANT_${id}_SOURCE_REQUIRED` })),
    metrics: Object.entries(businessMetrics).filter(([id, [, unit]]) => finance || !['VND', 'VND_PER_SHIPMENT'].includes(unit) && !id.startsWith('COD')).map(([id, [, unit]]) => ({ id, unit, state: 'SOURCE_NOT_CONNECTED', value: null, points: [] as unknown[] })),
    charts: Object.keys(businessChartTitles).filter(id => (finance || id !== 'FEE_TREND') && (staff || id !== 'STAFF_BREAKDOWN')).map(id => ({ id, state: 'SOURCE_NOT_CONNECTED', series: [] as unknown[] })), recentShipments: { state: 'SOURCE_NOT_CONNECTED', items: [] as unknown[] }, attention: { state: 'SOURCE_NOT_CONNECTED', items: [] as unknown[] }, quickActions: ['CREATE', 'IMPORT', 'LABELS', 'MANIFEST', 'PICKUP', 'ACTION_CENTER'].map(id => ({ id, state: 'NOT_SUPPORTED', reasonCode: 'MERCHANT_WORKFLOW_REQUIRED' })) };
}
