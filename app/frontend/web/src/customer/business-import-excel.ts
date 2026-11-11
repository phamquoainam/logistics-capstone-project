import { read, writeXLSX, utils } from 'xlsx';
import type { CellObject, WorkSheet } from 'xlsx';
import { crc32 } from './shipment-management';
import { blankImportRow, blankParcel, flatHeaders, flatLabels, extraHeaders, extraLabels, importColumns, parcelColumns, gridRowsFromRecords } from './business-import-grid';
import type { ImportGridRow, ParcelCell } from './business-import-grid';

const fail = (code: string): never => { throw new Error(code); };
const names = { main: 'DonHang', parcels: 'KienHang', guide: 'HuongDan' };
const mainHeaders = ['import_key', ...flatHeaders], mainLabels = ['Liên kết kiện', ...flatLabels];
const parcelHeaders = ['import_key', 'parcel_number', ...extraHeaders.slice(3)], parcelLabels = ['Liên kết kiện*', ...extraLabels.slice(2)];
const maximumFile = 1048576, maximumEntry = 2097152, maximumExpanded = 8388608;

/** Validate actual ZIP expansion before the Excel parser sees any workbook content. */
export async function preflightXlsx(buffer: ArrayBuffer): Promise<void> {
  const bytes = new Uint8Array(buffer), view = new DataView(buffer); if (bytes.length < 22 || bytes.length > maximumFile) fail('FILE_SIZE');
  let end = -1; for (let i = bytes.length - 22; i >= Math.max(0, bytes.length - 65557); i--) if (view.getUint32(i, true) === 0x06054b50 && i + 22 + view.getUint16(i + 20, true) === bytes.length) { end = i; break; }
  if (end < 0 || view.getUint16(end + 4, true) || view.getUint16(end + 6, true)) fail('XLSX_ARCHIVE');
  const count = view.getUint16(end + 10, true), start = view.getUint32(end + 16, true), length = view.getUint32(end + 12, true);
  if (count < 1 || count > 64 || count !== view.getUint16(end + 8, true) || start + length !== end) fail('XLSX_ARCHIVE');
  let cursor = start, total = 0; const seen = new Set<string>(), ranges: [number, number][] = [];
  for (let i = 0; i < count; i++) {
    if (cursor + 46 > end || view.getUint32(cursor, true) !== 0x02014b50) fail('XLSX_ARCHIVE');
    const flags = view.getUint16(cursor + 8, true), method = view.getUint16(cursor + 10, true), compressed = view.getUint32(cursor + 20, true), expanded = view.getUint32(cursor + 24, true), nameLength = view.getUint16(cursor + 28, true), extra = view.getUint16(cursor + 30, true), comment = view.getUint16(cursor + 32, true), local = view.getUint32(cursor + 42, true), checksum = view.getUint32(cursor + 16, true);
    if (flags & 1 || ![0, 8].includes(method) || expanded > maximumEntry || compressed > maximumFile || local + 30 >= start || cursor + 46 + nameLength + extra + comment > end) fail('XLSX_ARCHIVE');
    const name = new TextDecoder('utf-8', { fatal: true }).decode(bytes.slice(cursor + 46, cursor + 46 + nameLength));
    if (seen.has(name) || name.includes('\\') || name.split('/').some(s => s === '..' || s === '.') || /[\u0000-\u001f]/.test(name) || !/^(?:\[Content_Types\]\.xml|_rels\/|docProps\/|xl\/)/.test(name) || /vba|activex|embeddings|externallinks|connections|queries|customui|oleobject/i.test(name)) fail('XLSX_ACTIVE_CONTENT');
    seen.add(name); if (view.getUint32(local, true) !== 0x04034b50 || view.getUint16(local + 8, true) !== method || view.getUint16(local + 6, true) & 1) fail('XLSX_ARCHIVE');
    const localNameSize = view.getUint16(local + 26, true), dataStart = local + 30 + localNameSize + view.getUint16(local + 28, true), dataEnd = dataStart + compressed;
    if (dataEnd > start || new TextDecoder().decode(bytes.slice(local + 30, local + 30 + localNameSize)) !== name) fail('XLSX_ARCHIVE');
    ranges.push([local, dataEnd]); const content = bytes.slice(dataStart, dataEnd); let plain: Uint8Array<ArrayBuffer>;
    if (method === 0) plain = content;
    else {
      const reader = new Blob([content]).stream().pipeThrough(new DecompressionStream('deflate-raw')).getReader(), chunks: Uint8Array<ArrayBuffer>[] = []; let size = 0;
      try { for (;;) { const result = await reader.read(); if (result.done) break; size += result.value.length; if (size > expanded || size > maximumEntry || total + size > maximumExpanded) fail('XLSX_EXPANSION'); chunks.push(new Uint8Array(result.value)); } }
      finally { await reader.cancel(); }
      plain = new Uint8Array(size); let offset = 0; for (const chunk of chunks) { plain.set(chunk, offset); offset += chunk.length; }
    }
    total += plain.length; if (plain.length !== expanded || total > maximumExpanded || crc32(plain) !== checksum) fail('XLSX_EXPANSION');
    if (/\.(?:xml|rels)$/.test(name)) {
      const xml = new TextDecoder('utf-8', { fatal: true }).decode(plain);
      if (/<!DOCTYPE|<!ENTITY|TargetMode\s*=\s*["']External["']/i.test(xml)) fail('XLSX_ACTIVE_CONTENT');
      if (/^xl\/worksheets\//.test(name)) {
        if (/<(?:\w+:)?f(?:\s|>)/i.test(xml)) fail('XLSX_FORMULA');
        let cells = 0; for (const cell of xml.matchAll(/<(?:\w+:)?c\b[^>]*\br=["']([A-Z]+)(\d+)["']/g)) { if (++cells > 6000 || Number(cell[2]) > 501 || utils.decode_col(cell[1]) > 63) fail('XLSX_RANGE'); }
      }
    }
    cursor += 46 + nameLength + extra + comment;
  }
  ranges.sort((a, b) => a[0] - b[0]); if (ranges.some((r, i) => i > 0 && ranges[i - 1][1] > r[0]) || cursor !== end || !seen.has('[Content_Types].xml') || !seen.has('xl/workbook.xml')) fail('XLSX_ARCHIVE');
}
function cellText(cell?: CellObject): string {
  if (!cell || cell.t === 'z') return ''; if (cell.f || cell.F) fail('XLSX_FORMULA'); if (cell.l) fail('XLSX_ACTIVE_CONTENT');
  if (cell.t === 's' && typeof cell.v === 'string') return cell.v;
  if (cell.t === 'n' && typeof cell.v === 'number' && Number.isFinite(cell.v) && Math.abs(cell.v) <= Number.MAX_SAFE_INTEGER) { if (typeof cell.z === 'string' && /^0{2,16}$/.test(cell.z) && Number.isSafeInteger(cell.v)) return String(cell.v).padStart(cell.z.length, '0'); return String(cell.v); }
  return fail('XLSX_CELL_TYPE');
}
function matrix(sheet: WorkSheet, columns: number, rows: number): string[][] {
  if (!sheet['!ref']) throw new Error('FILE_HEADER'); const range = utils.decode_range(sheet['!ref']);
  if (range.s.r !== 0 || range.s.c !== 0 || range.e.r >= rows || range.e.c >= columns || sheet['!merges']?.length || sheet['!rows']?.some(r => r?.hidden) || sheet['!cols']?.some(c => c?.hidden)) fail('XLSX_RANGE');
  const result: string[][] = []; for (let r = 0; r <= range.e.r; r++) { const row: string[] = []; for (let c = 0; c < columns; c++) row.push(cellText(sheet[utils.encode_cell({ r, c })])); result.push(row); } return result;
}
function same(actual: string[], expected: string[]) { return actual.length === expected.length && actual.every((v, i) => v.trim() === expected[i]); }
export async function readImportXlsx(buffer: ArrayBuffer): Promise<ImportGridRow[]> {
  await preflightXlsx(buffer);
  const book = read(buffer, { type: 'array', cellFormula: true, cellHTML: false, cellText: false, cellNF: true, cellStyles: true, cellDates: true, WTF: true });
  if (!book.SheetNames.includes(names.main) || book.SheetNames.some(name => !Object.values(names).includes(name)) || book.Workbook?.Sheets?.some(sheet => sheet.Hidden)) fail('XLSX_SHEETS');
  const main = matrix(book.Sheets[names.main], mainHeaders.length, 101);
  if (!same(main[0], mainHeaders) && !same(main[0], mainLabels)) fail('FILE_HEADER');
  const records = main.slice(1).filter(r => r.some(v => v !== '')); const rows = gridRowsFromRecords([flatHeaders, ...records.map(r => r.slice(1))]);
  const byKey = new Map<string, number>(); const ambiguous = new Set<string>();
  records.forEach((r, i) => { if (byKey.has(r[0])) ambiguous.add(r[0]); byKey.set(r[0], i); });
  if (book.Sheets[names.parcels]) {
    const extra = matrix(book.Sheets[names.parcels], parcelHeaders.length, 401); if (!same(extra[0], parcelHeaders) && !same(extra[0], parcelLabels)) fail('FILE_HEADER');
    const used = new Map<number, Set<number>>();
    for (const r of extra.slice(1).filter(r => r.some(v => v !== ''))) {
      const parent = byKey.get(r[0]), number = Number(r[1]);
      if (!r[0] || parent === undefined || ambiguous.has(r[0]) || !/^[2-5]$/.test(r[1]) || used.get(parent)?.has(number)) throw new Error('XLSX_PARCEL_LINK');
      const keys: ParcelCell[] = ['description', 'quantity', 'weightGrams', 'declaredValueVnd', 'lengthCm', 'widthCm', 'heightCm'];
      const parcel = blankParcel(); keys.forEach((key, i) => { parcel[key] = r[i + 2]; }); rows[parent].extras[number - 2] = parcel; if (!used.has(parent)) used.set(parent, new Set()); used.get(parent)!.add(number);
    }
    for (const [index, positions] of used) { if (Array.from({ length: positions.size }, (_, n) => n + 2).some(n => !positions.has(n)) || rows[index].extras.length > 4) fail('XLSX_PARCEL_LINK'); }
  }
  // Guide is part of this format, never an alternative data sheet. Do not accept formulas hidden there.
  if (book.Sheets[names.guide]) matrix(book.Sheets[names.guide], 4, 30);
  return rows;
}
export function writeImportXlsx(rows: ImportGridRow[], template = false): ArrayBuffer {
  if (rows.length > 100 || rows.some(r => r.rawParcels)) fail('XLSX_RAW_PARCELS');
  const data = template ? [blankImportRow()] : rows, main: (string | number)[][] = [mainLabels], extra: (string | number)[][] = [parcelLabels];
  const number = (v: string, key: string): string | number => parcelColumns.some(c => c[0] === key && key !== 'description') && /^\d+(?:\.\d+)?$/.test(v) && Number(v) <= Number.MAX_SAFE_INTEGER ? Number(v) : v;
  data.forEach((row, index) => {
    const key = 'ROW-' + String(index + 1).padStart(3, '0'); main.push([key, ...importColumns.map(c => number(row.cells[c[0]], c[0]))]);
    row.extras.forEach((parcel, p) => extra.push([key, p + 2, ...(['description', 'quantity', 'weightGrams', 'declaredValueVnd', 'lengthCm', 'widthCm', 'heightCm'] as const).map(k => number(parcel[k], k))]));
  });
  const book = utils.book_new(), sheet = utils.aoa_to_sheet(main), parcels = utils.aoa_to_sheet(extra);
  sheet['!cols'] = [{ wch: 15 }, ...importColumns.map(c => ({ wch: c[0].includes('Address') ? 36 : c[3] === 'parcel' ? 18 : 24 }))];
  sheet['!autofilter'] = { ref: utils.encode_range({ s: { r: 0, c: 0 }, e: { r: Math.max(1, main.length - 1), c: mainHeaders.length - 1 } }) };
  parcels['!cols'] = parcelHeaders.map((_, i) => ({ wch: i === 2 ? 32 : 21 }));
  const guide = utils.aoa_to_sheet([
    ['ARGOPOST BUSINESS', 'Nhập đơn hàng từ Excel'],
    ['DonHang', 'Mỗi dòng là một đơn. Điền các cột có dấu *. Tối đa 100 đơn.'],
    ['Số điện thoại / mã nguồn', 'Giữ kiểu văn bản để không mất số 0 đầu hoặc mã dài.'],
    ['Đơn vị', 'Khối lượng g. Kích thước cm, tối đa 1 số lẻ. Khai giá VND.'],
    ['Liên kết kiện', 'Giữ mã ROW của đơn khi điền kiện bổ sung. Không phải mã vận đơn.'],
    ['KienHang', 'Kiện 2 đến 5. Điền Liên kết kiện trùng đơn và thứ tự liên tục.'],
    ['Dữ liệu', 'VN. Không dùng công thức, macro, liên kết ngoài hoặc ô gộp.'],
    ['Lưu nháp', 'Mở file, sửa trong bảng, kiểm tra dữ liệu và chọn dòng để lưu nháp.'],
  ]); guide['!cols'] = [{ wch: 27 }, { wch: 85 }];
  utils.book_append_sheet(book, sheet, names.main); utils.book_append_sheet(book, parcels, names.parcels); utils.book_append_sheet(book, guide, names.guide);
  book.Props = { Title: 'ArgoPost Business - Nhập đơn hàng', Author: 'ArgoPost' };
  return writeXLSX(book, { type: 'array', compression: true, bookSST: true }) as ArrayBuffer;
}
