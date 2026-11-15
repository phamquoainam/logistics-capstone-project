import type { FinanceItem, FinanceReport, OperationsReport, ReportBucket, ReportDimension } from '../../src/customer/business-report-data';
import { reportDimensions, reportExportColumns } from '../../src/customer/business-report-data';
import { businessContextsFixture, organizationA } from './business-fixture';
import { businessReportQuery } from '../../src/customer/business-report-navigation';

/** Synthetic protocol fixtures only; never used by the product or full-stack acceptance. */
export const reportAccount = '10000000-0000-4000-8000-000000000001';
export const reportShipment = '10000000-0000-4000-8000-000000000011';
export const reportSite = '10000000-0000-4000-8000-000000000021';
export const reportExportId = '10000000-0000-4000-8000-000000000031';
export const reportingContext = { ...businessContextsFixture[0], permissions: ['dashboard.read', 'shipment.draft.read', 'shipment.reports.read', 'shipment.finance.read', 'shipment.export'] };
const total = { shipments: '2', parcels: '3', actualGrams: '1500', chargeableGrams: '1500', confirmedFeesVnd: '75000', unpaidFeesVnd: '75000' };
const zero = { shipments: '0', parcels: '0', actualGrams: '0', chargeableGrams: '0', confirmedFeesVnd: '0', unpaidFeesVnd: '0' };
const add = (d: string, days: number) => new Date(Date.parse(d + 'T00:00:00Z') + days * 86400000).toISOString().slice(0, 10);
function calendarStart(d: string, interval: string) {
  const date = new Date(d + 'T00:00:00Z');
  if (interval === 'WEEK') date.setUTCDate(date.getUTCDate() - (date.getUTCDay() + 6) % 7);
  if (interval === 'MONTH' || interval === 'QUARTER' || interval === 'YEAR') date.setUTCDate(1);
  if (interval === 'QUARTER') date.setUTCMonth(Math.floor(date.getUTCMonth() / 3) * 3);
  if (interval === 'YEAR') date.setUTCMonth(0);
  return date.toISOString().slice(0, 10);
}
function next(d: string, interval: string) {
  if (interval === 'DAY' || interval === 'WEEK') return add(d, interval === 'DAY' ? 1 : 7);
  const date = new Date(d + 'T00:00:00Z'); date.setUTCMonth(date.getUTCMonth() + (interval === 'MONTH' ? 1 : interval === 'QUARTER' ? 3 : 12)); return date.toISOString().slice(0, 10);
}
export function operationsFixture(raw = new URLSearchParams(), financial = true, org = organizationA): OperationsReport {
  const q = businessReportQuery(raw, raw.has('page') || raw.has('search') || raw.has('sort')).api;
  const from = q.get('from')!, until = q.get('until')!, interval = q.get('interval')!, trend: ReportBucket[] = [];
  for (let current = calendarStart(from, interval); current < until; current = next(current, interval)) {
    const end = next(current, interval), start = current < from ? from : current;
    trend.push({ ...(trend.length === 0 ? total : zero), from: start, until: end > until ? until : end, partial: current < from || end >= until });
  }
  const group = (key: string, label = key): ReportDimension => ({ ...total, key, label });
  const result: OperationsReport = {
    schemaVersion: 'ORGANIZATION_OPERATIONS_1', organizationId: org, generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh',
    period: { from, until, interval, startInclusive: new Date(from + 'T00:00:00+07:00').toISOString(), endExclusive: new Date(until + 'T00:00:00+07:00').toISOString() },
    financialState: financial ? 'AVAILABLE' : 'FORBIDDEN', summary: { ...total, waitingForHandover: '2', unassigned: '1', averageFee: { totalVnd: '75000', shipmentCount: '2' } }, trend,
    breakdowns: { facility: [group('UNBOUND')], creator: [group(reportAccount)], channel: [group('CHANNEL:WEB', 'WEB')], service: [group('ROAD_STANDARD')], route: [group('SIM-A1/SIM-A2')] },
    preparations: { state: 'AVAILABLE', reasonCode: null, total: '4', draft: '1', booked: '2', archived: '1' },
    activity: { state: 'AVAILABLE', reasonCode: null, created: '4', booked: '2', byActor: [{ accountId: reportAccount, created: '4', booked: '2' }] },
    capabilities: [{ id: 'COD', state: 'NOT_SUPPORTED', reasonCode: 'BUSINESS_COD_POLICY_REQUIRED' }],
  };
  if (!financial) { result.summary.confirmedFeesVnd = null; result.summary.unpaidFeesVnd = null; result.summary.averageFee = null; for (const row of [...result.trend, ...reportDimensions.flatMap(key => result.breakdowns[key])]) { row.confirmedFeesVnd = null; row.unpaidFeesVnd = null; } }
  return result;
}
export function financeItem(index = 0): FinanceItem {
  const shipmentId = '10000000-0000-4000-8000-' + (11 + index).toString().padStart(12, '0');
  const feeLines = index === 0 ? [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }, { parcelNumber: 2, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }] : [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }];
  const amount = feeLines.reduce((n, line) => n + line.feeVnd, 0).toString(), weight = feeLines.reduce((n, line) => n + line.actualGrams, 0).toString();
  return { shipmentId, code: 'AP-SIM-' + shipmentId.replaceAll('-', ''), createdAt: new Date().toISOString(), facilityId: null, creatorAccountId: reportAccount, sourceChannel: 'WEB', sourceOrderReference: 'SYNTHETIC-' + index, service: 'ROAD_STANDARD', origin: 'SIM-A1', destination: 'SIM-A2', contractId: reportSite, contractVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', quoteId: reportExportId, quoteVnd: amount, confirmedVnd: amount, differenceVnd: '0', paymentState: 'UNPAID', actualGrams: weight, chargeableGrams: weight, feeLines };
}
export function financeFixture(q = new URLSearchParams(), org = organizationA): FinanceReport {
  const ops = operationsFixture(q, true, org), page = Number(q.get('page') ?? 0);
  return { schemaVersion: 'ORGANIZATION_FINANCE_1', organizationId: org, generatedAt: ops.generatedAt, timeZone: ops.timeZone, period: ops.period, summary: { shipments: '2', confirmedFeesVnd: '75000', unpaidFeesVnd: '75000', quoteFeesVnd: '75000', differenceVnd: '0', averageFee: { totalVnd: '75000', shipmentCount: '2' } }, trend: ops.trend, breakdowns: ops.breakdowns, total: '2', page, size: 20, hasNext: false, items: page === 0 ? [financeItem(), financeItem(1)] : [], capabilities: ops.capabilities };
}
export function exportFixture(kind = 'OPERATIONS', section = 'trend', org = organizationA) {
  return { schemaVersion: 'ORGANIZATION_REPORT_EXPORT_1', exportId: reportExportId, organizationId: org, generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', kind, section, rowCount: 1, columns: reportExportColumns, rows: [['channel', '2026-11-01', '2026-11-02', 'CHANNEL:WEB', '=SYNTHETIC(1)', '2', '3', '1500', '1500', '75000', '75000']] };
}
