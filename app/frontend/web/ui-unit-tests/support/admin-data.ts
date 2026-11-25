import type { AdminContext, Dashboard, Metric, MetricKey, Query, ShipmentPage } from '../../src/admin/data';
import { daysBefore } from '../../src/admin/data';
export const adminAccount = '10000000-0000-4000-8000-000000000001';
export const adminPermissions: AdminContext['permissions'] = ['ADMIN_DASHBOARD_READ', 'ADMIN_MERCHANT_READ', 'ADMIN_NETWORK_READ', 'ADMIN_SHIPMENT_READ', 'ADMIN_FINANCE_READ', 'ADMIN_TECHNICAL_READ'];
export const adminContext = (permissions = adminPermissions): AdminContext => ({ accountId: adminAccount, displayName: 'Quản trị thử nghiệm', permissions, environment: 'TEST', timeZone: 'Asia/Ho_Chi_Minh', regionFilterAvailable: false });
export const adminUser = { id: adminAccount, username: 'synthetic-admin', displayName: 'Quản trị thử nghiệm', email: 'admin@example.invalid', status: 'ACTIVE', grants: adminPermissions.map(role => ({ role, scopeType: 'IDENTITY', scopeId: '20000000-0000-4000-8000-000000000001' })), version: 1, emailVerified: true };
export function adminReport(query: Query, context = adminContext(), empty = false): Dashboard {
  const values: Record<MetricKey, string> = { BOOKINGS: '3', WAITING_HANDOVER: '2', ACCEPTED: '1', PARCELS: '6', CONFIRMED_FEES: '125000', UNPAID_FEES: '125000' };
  const report: Dashboard = { query, generatedAt: new Date().toISOString(), snapshotId: 'synthetic_selector.' + 'a'.repeat(43), sections: [] };
  for (const section of ['SHIPMENTS', 'FINANCE'] as const) {
    if (!context.permissions.includes(section === 'SHIPMENTS' ? 'ADMIN_SHIPMENT_READ' : 'ADMIN_FINANCE_READ')) continue;
    const keys: MetricKey[] = section === 'SHIPMENTS' ? ['BOOKINGS', 'WAITING_HANDOVER', 'ACCEPTED', 'PARCELS'] : ['CONFIRMED_FEES', 'UNPAID_FEES'];
    const metrics: Metric[] = keys.map(key => ({ key, value: empty ? '0' : values[key], unit: section === 'FINANCE' ? 'VND' : key === 'PARCELS' ? 'PARCEL' : 'SHIPMENT', drilldown: context.permissions.includes('ADMIN_SHIPMENT_READ') ? { path: '/api/v1/admin/dashboard/shipments', metric: key, filters: { from: query.from, to: query.to, grain: query.grain, ...(query.shipmentStatus ? { shipmentStatus: query.shipmentStatus } : {}) } } : null }));
    const series = metrics.flatMap(metric => {
      const points = [];
      for (let day = query.from; day <= query.to;) {
        const date = new Date(day + 'T00:00:00Z');
        if (query.grain === 'MONTH') date.setUTCMonth(date.getUTCMonth() + 1, 1);
        else date.setUTCDate(date.getUTCDate() + (query.grain === 'WEEK' ? (8 - date.getUTCDay()) % 7 || 7 : 1));
        const end = date.toISOString().slice(0, 10) > query.to ? daysBefore(query.to, -1) : date.toISOString().slice(0, 10);
        points.push({ from: day, to: end, metric: metric.key, value: end > query.to ? metric.value : '0' }); day = end;
      }
      return points;
    });
    report.sections.push({ key: section, availability: 'AVAILABLE', reasonCode: 'SYNTHETIC_BROWSER_FIXTURE', metrics, series });
  }
  if (context.permissions.includes('ADMIN_MERCHANT_READ')) report.sections.push({ key: 'MERCHANT', availability: 'SOURCE_NOT_IMPLEMENTED', reasonCode: 'SUBMITTED_APPLICATION_SOURCE_REQUIRED', metrics: [], series: [] });
  if (context.permissions.includes('ADMIN_NETWORK_READ')) report.sections.push({ key: 'NETWORK', availability: 'SOURCE_NOT_IMPLEMENTED', reasonCode: 'OFFICIAL_NETWORK_SOURCE_REQUIRED', metrics: [], series: [] });
  return report;
}
export function adminRows(report: Dashboard, metric: MetricKey): ShipmentPage {
  const accepted = metric === 'ACCEPTED';
  const rows: ShipmentPage['items'] = [{ id: '30000000-0000-4000-8000-000000000001', code: 'AP-SIM-' + 'a'.repeat(32), ownerKind: 'PERSONAL', bookedAt: new Date(Math.min(Date.parse(report.query.to + 'T00:00:01+07:00'), Date.now() - 1000)).toISOString(), status: accepted ? 'AT_STATION' : 'BOOKED', parcelCount: 2 }];
  return { items: rows, page: 0, size: 20, hasNext: false, snapshotId: report.snapshotId };
}

/** Synthetic masked fixture, never a source of production facts. */
export function adminOversight(report: Dashboard, shipment = adminRows(report, 'BOOKINGS').items[0]) {
  const parcels = Array.from({ length: shipment.parcelCount }, (_, i) => {
    const id = `40000000-0000-4000-8000-${String(i + 1).padStart(12, '0')}`;
    return { id, code: 'AP-P-' + id.replaceAll('-', ''), actualGrams: 1000, lengthMm: 100, widthMm: 100, heightMm: 100 };
  });
  const at = new Date(Math.min(Date.parse(shipment.bookedAt) + 1000, Date.parse(report.generatedAt))).toISOString();
  return { shipment, parcels, tracking: shipment.status === 'AT_STATION' ? [{ id: '50000000-0000-4000-8000-000000000001', kind: 'CARRIER_ACCEPTED_AT_STATION', occurredAt: at }] : [], acceptance: shipment.status === 'AT_STATION' ? { eventId: '60000000-0000-4000-8000-000000000001', occurredAt: at, stationCode: 'SIM-DROP-A1' } : null, snapshotId: report.snapshotId };
}
