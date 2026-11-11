import type { BusinessPreparationInput, DraftParcel } from './client';

export const importColumns = [
  ['sourceOrderReference', 'source_order_reference', 'Mã đơn nguồn', 'source', 160, 80],
  ['sourceChannel', 'source_channel', 'Nguồn đơn', 'source', 130, 40],
  ['senderName', 'sender_name', 'Tên người gửi', 'sender', 190, 160],
  ['senderPhone', 'sender_phone', 'Điện thoại người gửi', 'sender', 160, 32],
  ['senderAddressLine', 'sender_address_line', 'Địa chỉ gửi', 'sender', 260, 500],
  ['senderAdministrativeArea', 'sender_administrative_area', 'Tỉnh / thành gửi', 'sender', 180, 160],
  ['senderLocality', 'sender_locality', 'Phường / xã gửi', 'sender', 180, 160],
  ['senderCountry', 'sender_country', 'Quốc gia gửi', 'sender', 110, 2],
  ['recipientName', 'recipient_name', 'Tên người nhận', 'recipient', 190, 160],
  ['recipientPhone', 'recipient_phone', 'Điện thoại nhận', 'recipient', 160, 32],
  ['recipientAddressLine', 'recipient_address_line', 'Địa chỉ nhận', 'recipient', 260, 500],
  ['recipientAdministrativeArea', 'recipient_administrative_area', 'Tỉnh / thành nhận', 'recipient', 180, 160],
  ['recipientLocality', 'recipient_locality', 'Phường / xã nhận', 'recipient', 180, 160],
  ['recipientCountry', 'recipient_country', 'Quốc gia nhận', 'recipient', 110, 2],
  ['description', 'description', 'Tên hàng hóa', 'parcel', 210, 160],
  ['quantity', 'quantity', 'Số lượng', 'parcel', 110, 16],
  ['weightGrams', 'weight_grams', 'Khối lượng (g)', 'parcel', 130, 16],
  ['declaredValueVnd', 'declared_value_vnd', 'Khai giá (₫)', 'parcel', 140, 18],
  ['lengthCm', 'length_cm', 'Dài (cm)', 'parcel', 110, 16],
  ['widthCm', 'width_cm', 'Rộng (cm)', 'parcel', 110, 16],
  ['heightCm', 'height_cm', 'Cao (cm)', 'parcel', 110, 16],
  ['reference', 'reference', 'Tham chiếu', 'source', 160, 80],
  ['instructions', 'instructions', 'Ghi chú', 'source', 260, 1000],
] as const;
export type ImportColumn = typeof importColumns[number][0];
export const parcelColumns = importColumns.filter(c => c[3] === 'parcel');
export type ParcelCell = 'description' | 'quantity' | 'weightGrams' | 'lengthCm' | 'widthCm' | 'heightCm' | 'declaredValueVnd';
export type RawParcel = Record<ParcelCell, string>;
export type ImportGridRow = { id: string; cells: Record<ImportColumn, string>; extras: RawParcel[]; rawParcels?: string };
export const canonicalHeaders = ['source_channel', 'source_order_reference', 'sender_name', 'sender_phone', 'sender_country', 'sender_administrative_area', 'sender_locality', 'sender_address_line', 'recipient_name', 'recipient_phone', 'recipient_country', 'recipient_administrative_area', 'recipient_locality', 'recipient_address_line', 'parcels_json', 'reference', 'instructions'];
export const flatHeaders = importColumns.map(c => c[1]);
export const flatLabels = importColumns.map(c => c[2] + (c[0] === 'reference' || c[0] === 'instructions' ? '' : '*'));
export const extraHeaders = ['source_channel', 'source_order_reference', 'parcel_number', 'description', 'quantity', 'weight_grams', 'declared_value_vnd', 'length_cm', 'width_cm', 'height_cm'];
export const extraLabels = ['Nguồn đơn*', 'Mã đơn nguồn*', 'Số thứ tự kiện*', 'Tên hàng hóa*', 'Số lượng*', 'Khối lượng (g)*', 'Khai giá (₫)*', 'Dài (cm)*', 'Rộng (cm)*', 'Cao (cm)*'];
export const blankParcel = (): RawParcel => ({ description: '', quantity: '1', weightGrams: '', lengthCm: '', widthCm: '', heightCm: '', declaredValueVnd: '0' });
export function blankImportRow(): ImportGridRow {
  const cells = Object.fromEntries(importColumns.map(c => [c[0], ''])) as Record<ImportColumn, string>;
  return { id: crypto.randomUUID(), cells: { ...cells, ...blankParcel(), sourceChannel: 'MANUAL', senderCountry: 'VN', recipientCountry: 'VN' }, extras: [] };
}
export function hasImportData(row: ImportGridRow) { const empty = blankImportRow().cells; return !!row.rawParcels || !!row.extras.length || importColumns.some(c => row.cells[c[0]] !== empty[c[0]]); }
const fail = (code: string): never => { throw new Error(code); };
export function parseDelimited(text: string, separator = ',', maximumRows = 101): string[][] {
  if (text.startsWith('\uFEFF')) text = text.slice(1);
  const records: string[][] = []; let record: string[] = [], value = '', quoted = false, closed = false, started = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) { if (c === '"') { if (text[i + 1] === '"') { value += '"'; i++; } else { quoted = false; closed = true; } } else value += c; continue; }
    if (c === '"') { if (started || closed) fail('FILE_QUOTES'); quoted = true; started = true; continue; }
    if (c === separator || c === '\r' || c === '\n') {
      record.push(value); value = ''; started = false; closed = false;
      if (c !== separator) { if (c === '\r' && text[i + 1] === '\n') i++; records.push(record); record = []; if (records.length > maximumRows) fail('FILE_ROWS'); }
    } else { if (closed) fail('FILE_QUOTES'); value += c; started = true; }
  }
  if (quoted) fail('FILE_QUOTES'); if (started || closed || record.length) { record.push(value); records.push(record); }
  if (records.length > maximumRows) fail('FILE_ROWS'); return records;
}
export function integerCell(value: string): number | string { return /^\d+$/.test(value.trim()) && Number.isSafeInteger(Number(value)) ? Number(value) : value; }
export function millimetresCell(value: string): number | string { const m = /^(\d+)(?:\.(\d))?$/.exec(value.trim()); if (!m) return value; const mm = Number(m[1]) * 10 + Number(m[2] ?? 0); return Number.isSafeInteger(mm) ? mm : value; }
function parcelValue(parcel: RawParcel) { return { description: parcel.description, quantity: integerCell(parcel.quantity), weightGrams: integerCell(parcel.weightGrams), lengthMm: millimetresCell(parcel.lengthCm), widthMm: millimetresCell(parcel.widthCm), heightMm: millimetresCell(parcel.heightCm), declaredValueVnd: integerCell(parcel.declaredValueVnd) }; }
function rawParcel(p: DraftParcel): RawParcel { return { description: p.description, quantity: String(p.quantity), weightGrams: String(p.weightGrams), lengthCm: String(p.lengthMm / 10), widthCm: String(p.widthMm / 10), heightCm: String(p.heightMm / 10), declaredValueVnd: String(p.declaredValueVnd) }; }
export function rowFromPreparation(input: BusinessPreparationInput): ImportGridRow {
  const row = blankImportRow();
  for (const kind of ['sender', 'recipient'] as const) {
    const c = input[kind]; Object.assign(row.cells, { [kind + 'Name']: c.name, [kind + 'Phone']: c.phone, [kind + 'Country']: c.address.country, [kind + 'AdministrativeArea']: c.address.administrativeArea, [kind + 'Locality']: c.address.locality, [kind + 'AddressLine']: c.address.addressLine });
  }
  Object.assign(row.cells, rawParcel(input.parcels[0]), { sourceChannel: input.sourceChannel ?? '', sourceOrderReference: input.sourceOrderReference ?? '', reference: input.reference ?? '', instructions: input.instructions ?? '' });
  row.extras = input.parcels.slice(1).map(rawParcel); return row;
}
export function canonicalValues(row: ImportGridRow): string[] {
  const c = row.cells;
  return [c.sourceChannel, c.sourceOrderReference, c.senderName, c.senderPhone, c.senderCountry, c.senderAdministrativeArea, c.senderLocality, c.senderAddressLine, c.recipientName, c.recipientPhone, c.recipientCountry, c.recipientAdministrativeArea, c.recipientLocality, c.recipientAddressLine, row.rawParcels ?? JSON.stringify([parcelValue(c), ...row.extras.map(parcelValue)]), c.reference, c.instructions];
}
const quote = (value: string) => '"' + value.replaceAll('"', '""') + '"';
export function serverCsv(rows: ImportGridRow[]): string {
  if (!rows.length || rows.length > 100) fail('FILE_ROWS');
  const csv = canonicalHeaders.join(',') + '\r\n' + rows.map(row => canonicalValues(row).map(quote).join(',')).join('\r\n') + '\r\n';
  if (new TextEncoder().encode(csv).length > 262144) fail('FILE_SIZE'); return csv;
}
export function exportCsv(rows: ImportGridRow[]): string {
  return '\uFEFF' + canonicalHeaders.join(',') + '\r\n' + rows.map(row => canonicalValues(row).map((v, i) => quote(/^[\s\uFEFF]*[=+@-]/.test(v) && !([3, 9].includes(i) && /^\+84[35789]\d{8}$/.test(v)) ? "'" + v : v)).join(',')).join('\r\n') + '\r\n';
}
export function flatCsvTemplate() { return '\uFEFF' + flatLabels.map(quote).join(',') + '\r\n'; }
function match(actual: string[], expected: readonly string[]) { return actual.length === expected.length && actual.every((v, i) => v.trim() === expected[i]); }
export function gridRowsFromRecords(records: string[][]): ImportGridRow[] {
  if (!records.length) fail('FILE_HEADER'); const header = records[0], canonical = match(header, canonicalHeaders);
  if (!canonical && !match(header, flatHeaders) && !match(header, flatLabels)) fail('FILE_HEADER');
  const content = records.slice(1).filter(row => row.some(v => v !== '')); if (content.length > 100) fail('FILE_ROWS');
  return content.map(values => {
    if (values.length !== header.length) fail('FILE_COLUMNS'); const row = blankImportRow();
    if (!canonical) { importColumns.forEach((c, i) => { row.cells[c[0]] = values[i]; }); return row; }
    for (const col of importColumns) { const i = canonicalHeaders.indexOf(col[1]); if (i >= 0) row.cells[col[0]] = values[i]; }
    try {
      const parsed: unknown = JSON.parse(values[14]); const fields = ['description', 'quantity', 'weightGrams', 'lengthMm', 'widthMm', 'heightMm', 'declaredValueVnd'];
      if (!Array.isArray(parsed) || !parsed.length || parsed.length > 5 || parsed.some(p => !p || typeof p !== 'object' || Object.keys(p).length !== fields.length || !fields.every(f => Object.hasOwn(p, f)) || typeof p.description !== 'string' || fields.slice(1).some(f => !Number.isSafeInteger(p[f])))) throw new Error();
      Object.assign(row.cells, rawParcel(parsed[0] as DraftParcel)); row.extras = parsed.slice(1).map(p => rawParcel(p as DraftParcel));
    } catch { row.rawParcels = values[14]; }
    return row;
  });
}
export function gridRowsFromCsv(csv: string) { if (new TextEncoder().encode(csv).length > 262144) fail('FILE_SIZE'); return gridRowsFromRecords(parseDelimited(csv)); }
export function localCellIssues(row: ImportGridRow): Partial<Record<ImportColumn, string>> {
  const out: Partial<Record<ImportColumn, string>> = {};
  for (const col of importColumns) {
    const value = row.cells[col[0]], optional = ['reference', 'instructions'].includes(col[0]);
    if (!optional && !value.trim()) out[col[0]] = 'Thông tin bắt buộc';
    else if ([...value].length > col[5] || /[\u0000-\u001f\u007f]/u.test(value)) out[col[0]] = 'Độ dài hoặc ký tự chưa hợp lệ';
    else if (!col[0].endsWith('Phone') && /^[\s\uFEFF]*[=+@-]/.test(value)) out[col[0]] = 'Không dùng công thức';
  }
  for (const kind of ['sender', 'recipient'] as const) {
    const phone = row.cells[(kind + 'Phone') as ImportColumn].replace(/[ ()-]/g, '');
    if (!/^(?:0|84|\+84)[35789]\d{8}$/.test(phone)) out[(kind + 'Phone') as ImportColumn] = 'Số di động Việt Nam chưa hợp lệ';
    if (row.cells[(kind + 'Country') as ImportColumn] !== 'VN') out[(kind + 'Country') as ImportColumn] = 'Chỉ hỗ trợ VN';
  }
  for (const [field, maximum, min] of [['quantity', 1e6, 1], ['weightGrams', 1e9, 1], ['declaredValueVnd', 1e12, 0]] as const) { const v = integerCell(row.cells[field]); if (typeof v !== 'number' || v < min || v > maximum) out[field] = 'Nhập số nguyên trong giới hạn'; }
  for (const field of ['lengthCm', 'widthCm', 'heightCm'] as const) { const v = millimetresCell(row.cells[field]); if (typeof v !== 'number' || v < 1 || v > 1e6) out[field] = 'Nhập cm, tối đa 1 số lẻ'; }
  return out;
}
export function applyGridPaste(rows: ImportGridRow[], rowId: string, column: ImportColumn, text: string, visible: ImportColumn[], locked: Set<string>): ImportGridRow[] {
  const values = parseDelimited(text, '\t', 101); const startRow = rows.findIndex(r => r.id === rowId), startCol = visible.indexOf(column);
  if (startRow < 0 || startCol < 0 || startRow + values.length > 100 || values.some(r => r.length > visible.length - startCol)) fail('PASTE_RANGE');
  const next = structuredClone(rows); while (next.length < startRow + values.length) next.push(blankImportRow());
  values.forEach((row, r) => { if (locked.has(next[startRow + r].id)) fail('ROW_LOCKED'); row.forEach((value, c) => { const key = visible[startCol + c]; if (value.length > 4000) fail('FILE_SIZE'); next[startRow + r].cells[key] = value; if (parcelColumns.some(p => p[0] === key)) delete next[startRow + r].rawParcels; }); });
  return next;
}
