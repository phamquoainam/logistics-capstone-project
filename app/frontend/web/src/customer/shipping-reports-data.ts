import type { ShippingReport } from './analytics-data';
import { csvCell } from './shipment-management';
import { shiftDate } from './AnalyticsPeriodPicker';

export const reportSections = { volume: 'Sản lượng', status: 'Trạng thái', services: 'Dịch vụ', routes: 'Tuyến gửi hàng', parcels: 'Kiện hàng', timing: 'Thời gian', delivery: 'Hiệu quả giao hàng', exceptions: 'Ngoại lệ' };
export const reportGroups = { day: 'Ngày', week: 'Tuần', month: 'Tháng', quarter: 'Quý', year: 'Năm' };
export type ReportSection = keyof typeof reportSections;
export type ReportGroup = keyof typeof reportGroups;
export const exactNumber = (v: string) => BigInt(v).toLocaleString('vi-VN');
export function ratio(n: string, d: string, percent = false) {
  if (BigInt(d) === 0n) return 'N/A';
  const scaled = (BigInt(n) * (percent ? 10000n : 100n) + BigInt(d) / 2n) / BigInt(d);
  return exactNumber(String(scaled / 100n)) + ',' + String(scaled % 100n).padStart(2, '0') + (percent ? '%' : '');
}
export function durationLabel(total: string | null, sample = '1') {
  if (total === null || BigInt(sample) === 0n) return 'N/A';
  const ms = (BigInt(total) + BigInt(sample) / 2n) / BigInt(sample);
  if (ms > 0n && ms < 1000n) return 'Dưới 1 giây';
  const seconds = (ms + 500n) / 1000n, hours = seconds / 3600n, minutes = seconds % 3600n / 60n;
  return hours ? `${exactNumber(String(hours))} giờ ${minutes} phút` : minutes ? `${minutes} phút ${seconds % 60n} giây` : `${seconds} giây`;
}
export function defaultReportPeriod() {
  const today = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10);
  return { from: shiftDate(today, -29), until: shiftDate(today, 1) };
}
function bucketStart(date: string, group: ReportGroup) {
  if (group === 'day') return date;
  if (group === 'week') return shiftDate(date, -(new Date(date).getUTCDay() + 6) % 7);
  if (group === 'month') return date.slice(0, 7) + '-01';
  return date.slice(0, 4) + '-' + (group === 'year' ? '01' : String(Math.floor((Number(date.slice(5, 7)) - 1) / 3) * 3 + 1).padStart(2, '0')) + '-01';
}
function bucketEnd(start: string, group: ReportGroup) {
  if (group === 'day' || group === 'week') return shiftDate(start, group === 'day' ? 1 : 7);
  const date = new Date(start); date.setUTCMonth(date.getUTCMonth() + (group === 'month' ? 1 : group === 'quarter' ? 3 : 12)); return date.toISOString().slice(0, 10);
}
export type ReportBucket = { date: string; from: string; until: string; label: string; partial: boolean; shipments: string; parcels: string; confirmedVnd: string };
/** Full captured daily cohort only, never the20-row source page. */
export function reportBuckets(source: ShippingReport, group: ReportGroup, from: string, until: string): ReportBucket[] {
  const parcels = source.report.metrics.find(m => m.id === 'CREATED_PARCELS')!.points, volume = source.report.volume;
  if (parcels.length !== volume.length || parcels.some((p, index) => p.date !== volume[index].date)) throw new Error('REPORT_DAILY_SOURCE_REQUIRED');
  const result = new Map<string, ReportBucket>();
  volume.forEach((point, index) => {
    const start = bucketStart(point.date, group), end = bucketEnd(start, group);
    const lower = start < from ? from : start, upper = end > until ? until : end;
    let row = result.get(start);
    if (!row) { row = { date: start, from: lower, until: upper, label: group === 'year' ? start.slice(0, 4) : group === 'quarter' ? `Q${Math.floor((Number(start.slice(5, 7)) - 1) / 3) + 1}/${start.slice(0, 4)}` : group === 'month' ? start.slice(5, 7) + '/' + start.slice(0, 4) : start.slice(8) + '/' + start.slice(5, 7), partial: lower !== start || upper !== end || upper > new Date(Date.parse(source.report.generatedAt) + 7 * 3600000).toISOString().slice(0, 10), shipments: '0', parcels: '0', confirmedVnd: '0' }; result.set(start, row); }
    row.shipments = String(BigInt(row.shipments) + BigInt(point.shipments)); row.parcels = String(BigInt(row.parcels) + BigInt(parcels[index].value)); row.confirmedVnd = String(BigInt(row.confirmedVnd) + BigInt(point.confirmedFeeVnd));
  });
  return [...result.values()];
}
export function reportCsv(source: ShippingReport, section: ReportSection, buckets: ReportBucket[], query: string) {
  const r = source.report, rows: string[][] = [['ARGOPOST LOCAL/TEST SIMULATION — Báo cáo Customer, không phải chứng từ thương mại'], ['Phân tích', reportSections[section]], ['Bộ lọc', query], ['Thời điểm dữ liệu (UTC)', r.generatedAt], ['Kỳ Booking (UTC)', r.period.startInclusive, r.period.endExclusive]];
  if (section === 'volume') { rows.push(['Từ ngày VN', 'Đến ngày VN (không bao gồm)', 'Kỳ chưa đủ', 'Vận đơn', 'Kiện', 'Cước xác nhận VND']); for (const b of buckets) rows.push([b.from, b.until, b.partial ? 'Có' : 'Không', b.shipments, b.parcels, b.confirmedVnd]); }
  if (section === 'status') { rows.push(['Trạng thái hiện tại của Booking cohort', 'Vận đơn', 'Mẫu số']); for (const s of r.status) rows.push([s.key, s.shipments, r.shipments.total]); }
  if (section === 'services') { rows.push(['Dịch vụ', 'Vận đơn', 'Cước xác nhận VND']); for (const s of r.services) rows.push([s.code, s.shipments, s.confirmedFeeVnd]); }
  if (section === 'routes') { rows.push(['Dataset', 'Vùng gửi', 'Vùng nhận', 'Vận đơn', 'Cước xác nhận VND']); for (const v of r.routes) rows.push([v.datasetVersion, v.origin, v.destination, v.shipments, v.confirmedFeeVnd]); }
  if (section === 'parcels') { rows.push(['Kiện', r.parcels.count], ['Vận đơn', r.shipments.total], ['Trọng lượng thực g', source.parcelWeights.actualGrams], ['Trọng lượng tính cước g', source.parcelWeights.chargeableGrams], ['Dải trọng lượng thực', 'Kiện']); for (const w of r.parcels.weight) rows.push([w.key, w.parcels]); }
  if (section === 'timing') rows.push(['Mẫu Shipment đã tiếp nhận', source.acceptanceTiming.sampleShipments], ['Tổng thời gian Booking→Acceptance ms', source.acceptanceTiming.totalDurationMillis], ['Thấp nhất ms', source.acceptanceTiming.minimumMillis ?? 'N/A'], ['Cao nhất ms', source.acceptanceTiming.maximumMillis ?? 'N/A']);
  return '\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n') + '\r\n';
}
