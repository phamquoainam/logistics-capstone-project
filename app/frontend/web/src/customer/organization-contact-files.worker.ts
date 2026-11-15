import { read, writeXLSX, utils } from 'xlsx';
import { preflightXlsx } from './business-import-excel';
import { contactHeaders } from './organization-contacts-data';
const invalid = (): never => { throw new Error('CONTACT_XLSX_INVALID'); };
self.addEventListener('message', async (event: MessageEvent<{ operation: 'read' | 'write'; buffer?: ArrayBuffer; rows?: string[][] }>) => {
  try {
    let result: string | ArrayBuffer;
    if (event.data.operation === 'read') {
      const buffer = event.data.buffer!; await preflightXlsx(buffer); const book = read(buffer, { type: 'array', cellFormula: true, cellText: false, cellHTML: false, cellDates: true, WTF: true });
      if (book.SheetNames.length !== 1 || book.Workbook?.Sheets?.some(sheet => sheet.Hidden)) invalid(); const sheet = book.Sheets[book.SheetNames[0]]; if (!sheet['!ref'] || sheet['!merges']?.length || sheet['!rows']?.some(r => r?.hidden) || sheet['!cols']?.some(c => c?.hidden)) invalid();
      const range = utils.decode_range(sheet['!ref']!); if (range.s.r !== 0 || range.s.c !== 0 || range.e.r > 100 || range.e.c !== 9 || range.e.r < 1) invalid(); const rows: string[][] = [];
      for (let r = 0; r <= range.e.r; r++) { const row: string[] = []; for (let c = 0; c < 10; c++) { const cell = sheet[utils.encode_cell({ r, c })]; if (!cell || cell.t === 'z') { row.push(''); continue; } if (cell.f || cell.F || cell.l || cell.t !== 's' || typeof cell.v !== 'string') invalid(); row.push(cell.v as string); } rows.push(row); }
      if (rows[0].join('|') !== contactHeaders.join('|')) invalid(); result = rows.map(row => row.map(cell => '"' + cell.replaceAll('"', '""') + '"').join(',')).join('\r\n') + '\r\n'; if (new TextEncoder().encode(result).length > 262144) invalid();
    } else {
      const rows = event.data.rows ?? []; if (rows.length > 201 || rows.some(row => row.length !== 10 || row.some(v => typeof v !== 'string' || v.length > 1000))) invalid();
      const sheet = utils.aoa_to_sheet([contactHeaders, ...rows]); sheet['!cols'] = contactHeaders.map((_, i) => ({ wch: i === 9 ? 55 : 25 })); const book = utils.book_new(); utils.book_append_sheet(book, sheet, 'DanhBa'); book.Props = { Title: 'ArgoPost LOCAL TEST SIMULATION - Danh ba', Author: 'ArgoPost' }; result = writeXLSX(book, { type: 'array', compression: true }) as ArrayBuffer;
    }
    self.postMessage({ result });
  } catch { self.postMessage({ error: 'CONTACT_XLSX_INVALID' }); }
});
