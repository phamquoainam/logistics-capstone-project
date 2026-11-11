import type { ManagementPage } from './client';

export const optionalManagementColumns = { contacts: 'Người gửi / người nhận', route: 'Tuyến / dịch vụ', parcels: 'Kiện hàng', fees: 'Cước / nghĩa vụ' };
export type ManagementColumn = keyof typeof optionalManagementColumns;
export const detailTabs = ['overview', 'parcels', 'tracking', 'handover', 'fees', 'documents', 'support', 'history'] as const;
export const shipmentDate = (value: string) => new Date(value).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh', dateStyle: 'short', timeStyle: 'short' });

export function managementReturn(value: unknown): string | undefined {
  if (typeof value !== 'string' || value.length > 4096 || !/^\/personal\/(?:shipments|lookup|labels)(?:\?[^#]*)?$/.test(value)) return undefined;
  const url = new URL(value, 'https://local.invalid');
  return [...url.searchParams.keys()].every(key => ['page', 'size', 'search', 'recipient', 'from', 'until', 'status', 'service', 'origin', 'destination', 'obligation', 'sort', 'category', 'tab', 'view'].includes(key)) ? value : undefined;
}

export function csvCell(value: string) {
  // Quoting alone does not stop spreadsheet formula/DDE execution.
  const safe = /^[\s\uFEFF]*[=+@-]/.test(value) || /^[\t\r\n]/.test(value) ? "'" + value : value;
  return '"' + safe.replaceAll('"', '""') + '"';
}
export function managementCsv(data: ManagementPage, items: ManagementPage['items'], columns: ManagementColumn[], query: string) {
  const head = ['Mã vận đơn', 'Ngày tạo (giờ Việt Nam)', 'Tham chiếu'];
  if (columns.includes('contacts')) head.push('Người gửi', 'Người nhận', 'Điện thoại người nhận');
  if (columns.includes('route')) head.push('Khu vực gửi', 'Khu vực nhận', 'Dịch vụ');
  if (columns.includes('parcels')) head.push('Số kiện');
  if (columns.includes('fees')) head.push('Cước xác nhận (VND)', 'Nghĩa vụ', 'Người trả cước');
  head.push('Trạng thái');
  const rows = [['ARGOPOST LOCAL/TEST SIMULATION — Không phải chứng từ thương mại'], ['Phạm vi', `Trang ${data.page + 1}; ${items.length} vận đơn đã tải; không phải toàn bộ ${data.total} kết quả`], ['Bộ lọc', query], head];
  for (const item of items) {
    const s = item.record.shipment, row = [s.code, shipmentDate(s.createdAt), s.input.reference ?? ''];
    if (columns.includes('contacts')) row.push(s.input.sender.name, s.input.recipient.name, s.input.recipient.phone);
    if (columns.includes('route')) row.push(s.quote.route.originCode, s.quote.route.destinationCode, 'ROAD_STANDARD');
    if (columns.includes('parcels')) row.push(String(s.parcels.length));
    if (columns.includes('fees')) row.push(String(s.totalVnd), item.record.obligation.state, 'SENDER');
    row.push(item.status === 'AT_STATION' ? 'Đã tiếp nhận tại Station' : 'Chờ bàn giao'); rows.push(row);
  }
  return '\uFEFF' + rows.map(row => row.map(csvCell).join(',')).join('\r\n') + '\r\n';
}

export function crc32(bytes: Uint8Array) {
  let crc = 0xffffffff;
  for (const byte of bytes) { crc ^= byte; for (let bit = 0; bit < 8; bit++) crc = crc >>> 1 ^ (crc & 1 ? 0xedb88320 : 0); }
  return (crc ^ 0xffffffff) >>> 0;
}
/** Stored ZIP only: preserves the authorized PDF bytes; never rewrites or merges labels. */
export function labelZip(files: { name: string; bytes: Uint8Array<ArrayBuffer> }[]) {
  if (!files.length || files.length > 20 || new Set(files.map(f => f.name)).size !== files.length || files.some(f => !/^[a-zA-Z0-9_-]+\.pdf$/.test(f.name) || f.name.length > 100) || files.reduce((n, f) => n + f.bytes.length, 0) > 32 * 1024 * 1024) throw new Error('LABEL_BUNDLE_LIMIT');
  const parts: Uint8Array<ArrayBuffer>[] = [], central: Uint8Array<ArrayBuffer>[] = []; let offset = 0;
  for (const file of files) {
    const name = new TextEncoder().encode(file.name), crc = crc32(file.bytes), size = file.bytes.length;
    const local = new Uint8Array(30 + name.length), l = new DataView(local.buffer);
    l.setUint32(0, 0x04034b50, true); l.setUint16(4, 20, true); l.setUint16(10, 0, true); l.setUint16(12, 33, true); l.setUint32(14, crc, true); l.setUint32(18, size, true); l.setUint32(22, size, true); l.setUint16(26, name.length, true); local.set(name, 30);
    const entry = new Uint8Array(46 + name.length), c = new DataView(entry.buffer);
    c.setUint32(0, 0x02014b50, true); c.setUint16(4, 20, true); c.setUint16(6, 20, true); c.setUint16(14, 33, true); c.setUint32(16, crc, true); c.setUint32(20, size, true); c.setUint32(24, size, true); c.setUint16(28, name.length, true); c.setUint32(42, offset, true); entry.set(name, 46);
    parts.push(local, file.bytes); central.push(entry); offset += local.length + size;
  }
  const length = central.reduce((n, p) => n + p.length, 0), end = new Uint8Array(22), e = new DataView(end.buffer);
  e.setUint32(0, 0x06054b50, true); e.setUint16(8, files.length, true); e.setUint16(10, files.length, true); e.setUint32(12, length, true); e.setUint32(16, offset, true);
  return new Blob([...parts, ...central, end], { type: 'application/zip' });
}
