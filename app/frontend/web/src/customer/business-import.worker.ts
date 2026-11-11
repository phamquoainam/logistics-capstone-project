import { readImportXlsx, writeImportXlsx } from './business-import-excel';
import type { ImportGridRow } from './business-import-grid';
self.addEventListener('message', async (event: MessageEvent<{ operation: 'read' | 'write'; buffer?: ArrayBuffer; rows?: ImportGridRow[]; template?: boolean }>) => {
  try {
    const result = event.data.operation === 'read' ? await readImportXlsx(event.data.buffer!) : writeImportXlsx(event.data.rows ?? [], !!event.data.template);
    self.postMessage({ result });
  } catch (error) { self.postMessage({ error: error instanceof Error && /^[A-Z_]+$/.test(error.message) ? error.message : 'XLSX_INVALID' }); }
});
