import { test, expect } from '@playwright/test';
import { read, writeXLSX, utils } from 'xlsx';
import { applyGridPaste, blankImportRow, canonicalValues, exportCsv, flatCsvTemplate, gridRowsFromCsv, hasImportData, importColumns, integerCell, localCellIssues, millimetresCell, parseDelimited, serverCsv } from '../src/customer/business-import-grid';
import { preflightXlsx, readImportXlsx, writeImportXlsx } from '../src/customer/business-import-excel';

function sample() {
  const r = blankImportRow(); Object.assign(r.cells, { sourceOrderReference: '0001234567890123456789', sourceChannel: 'CSV', senderName: 'Nguyễn Thử Nghiệm', senderPhone: '0901234567', senderCountry: 'VN', senderAdministrativeArea: 'SIM-A1', senderLocality: 'Phường thử', senderAddressLine: '12, Đường "Mẫu"', recipientName: 'Trần Kiểm Thử', recipientPhone: '+84912345678', recipientAdministrativeArea: 'SIM-A2', recipientLocality: 'Xã thử', recipientAddressLine: 'Địa chỉ tổng hợp', description: 'Hộp thử nghiệm', quantity: '1', weightGrams: '500', lengthCm: '10.1', widthCm: '12', heightCm: '13.5', declaredValueVnd: '0' }); return r;
}
function rewrite(buffer: ArrayBuffer, change: (book: ReturnType<typeof read>) => void) { const book = read(buffer, { type: 'array', cellNF: true, cellStyles: true }); change(book); return writeXLSX(book, { type: 'array', compression: true }) as ArrayBuffer; }
test('CSV preserves Unicode, quoted contacts, leading zero identifiers, all five parcels and exact dimensions', () => {
  const row = sample(); row.extras = Array.from({ length: 4 }, (_, i) => ({ description: 'Kiện ' + (i + 2), quantity: '1', weightGrams: String(501 + i), lengthCm: '10.1', widthCm: '20', heightCm: '30', declaredValueVnd: '0' }));
  const csv = serverCsv([row]), restored = gridRowsFromCsv(csv)[0]; expect(canonicalValues(restored)).toEqual(canonicalValues(row)); expect(restored.cells.senderPhone).toBe('0901234567'); expect(restored.extras).toHaveLength(4); expect(localCellIssues(row)).toEqual({});
  expect(gridRowsFromCsv(exportCsv([row])).map(canonicalValues)).toEqual([canonicalValues(row)]); expect(gridRowsFromCsv(flatCsvTemplate())).toEqual([]);
});
test('invalid input remains invalid: no silent numeric truncation or loss of malformed parcels', () => {
  expect(integerCell('1.5')).toBe('1.5'); expect(millimetresCell('1.01')).toBe('1.01'); expect(millimetresCell('1.1')).toBe(11);
  const row = sample(); row.rawParcels = '[bad'; const recovered = gridRowsFromCsv(serverCsv([row]))[0]; expect(recovered.rawParcels).toBe('[bad'); expect(serverCsv([recovered])).toContain('[bad'); expect(() => writeImportXlsx([recovered])).toThrow('XLSX_RAW_PARCELS');
  row.cells.senderName = '=DANGEROUS()'; expect(serverCsv([row])).toContain('"=DANGEROUS()"'); expect(exportCsv([row])).toContain('"\'=DANGEROUS()"');
  expect(() => gridRowsFromCsv('bad,header\n1,2')).toThrow('FILE_HEADER'); expect(() => parseDelimited('"unclosed')).toThrow('FILE_QUOTES'); expect(() => serverCsv(Array.from({ length: 101 }, sample))).toThrow('FILE_ROWS'); expect(() => gridRowsFromCsv('a'.repeat(262145))).toThrow('FILE_SIZE');
});
test('rectangular paste is atomic, respects locks and bounds, and keeps explicit zero values', () => {
  const rows = [blankImportRow(), blankImportRow()], columns = importColumns.map(c => c[0]);
  expect(hasImportData(rows[0])).toBe(false); const changed = applyGridPaste(rows, rows[0].id, 'sourceOrderReference', '001\tCSV\n002\tOMS', columns, new Set());
  expect(changed.map(r => [r.cells.sourceOrderReference, r.cells.sourceChannel])).toEqual([['001', 'CSV'], ['002', 'OMS']]); expect(rows[0].cells.sourceOrderReference).toBe('');
  expect(() => applyGridPaste(rows, rows[0].id, 'sourceOrderReference', '1\n2', columns, new Set([rows[1].id]))).toThrow('ROW_LOCKED'); expect(rows[0].cells.sourceOrderReference).toBe('');
  expect(() => applyGridPaste(rows, rows[0].id, 'instructions', 'a\tb', columns, new Set())).toThrow('PASTE_RANGE');
  expect(applyGridPaste(rows, rows[0].id, 'declaredValueVnd', '0', columns, new Set())[0].cells.declaredValueVnd).toBe('0');
});
test('Excel preserves all facts and parcel links when worksheet rows are reordered', async () => {
  const first = sample(), second = sample(); second.cells.sourceOrderReference = 'SECOND'; first.extras = [{ description: 'Kiện bổ sung', quantity: '2', weightGrams: '700', lengthCm: '15.5', widthCm: '20', heightCm: '10', declaredValueVnd: '0' }];
  const workbook = writeImportXlsx([first, second]); await preflightXlsx(workbook); const loaded = await readImportXlsx(workbook); expect(loaded.map(canonicalValues)).toEqual([first, second].map(canonicalValues));
  const book = read(workbook, { type: 'array' }); expect(book.Sheets.DonHang.E2.t).toBe('s'); expect(book.Sheets.DonHang.B2.v).toBe(first.cells.sourceOrderReference);
  const reordered = rewrite(workbook, book => { const matrix = utils.sheet_to_json<unknown[]>(book.Sheets.DonHang, { header: 1 }); book.Sheets.DonHang = utils.aoa_to_sheet([matrix[0], matrix[2], matrix[1]]); });
  const sorted = await readImportXlsx(reordered); expect(sorted[0].extras).toEqual([]); expect(sorted[1].extras).toEqual(first.extras);
  expect(await readImportXlsx(writeImportXlsx([], true))).toHaveLength(1);
});
test('Excel rejects formulas, external links, hidden data, unsupported cells and broken parcel links', async () => {
  const buffer = writeImportXlsx([sample()]);
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.DonHang.B2 = { t: 's', v: 'cached', f: 'WEBSERVICE("https://example.invalid")' }; }))).rejects.toThrow('XLSX_FORMULA');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.HuongDan.B2 = { t: 'n', v: 1, f: '1+1' }; }))).rejects.toThrow('XLSX_FORMULA');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.DonHang.B2.l = { Target: 'https://example.invalid/' }; }))).rejects.toThrow('XLSX_ACTIVE_CONTENT');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.DonHang['!rows'] = [{ hidden: true }]; }))).rejects.toThrow('XLSX_RANGE');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.DonHang['!merges'] = [{ s: { r: 1, c: 1 }, e: { r: 1, c: 2 } }]; }))).rejects.toThrow('XLSX_RANGE');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.DonHang.B2 = { t: 'b', v: true }; }))).rejects.toThrow('XLSX_CELL_TYPE');
  await expect(readImportXlsx(rewrite(buffer, book => { book.Sheets.KienHang.A2 = { t: 's', v: 'MISSING' }; book.Sheets.KienHang.B2 = { t: 'n', v: 2 }; book.Sheets.KienHang['!ref'] = 'A1:I2'; }))).rejects.toThrow('XLSX_PARCEL_LINK');
});
test('Excel preflight rejects oversized or damaged archives before parsing', async () => {
  await expect(preflightXlsx(new ArrayBuffer(1048577))).rejects.toThrow('FILE_SIZE');
  const buffer = writeImportXlsx([sample()]), changed = buffer.slice(0), bytes = new Uint8Array(changed); bytes[100] ^= 255; await expect(preflightXlsx(changed)).rejects.toThrow();
  await expect(readImportXlsx(buffer.slice(0, buffer.byteLength - 1))).rejects.toThrow('XLSX_ARCHIVE');
});
