import { gridRowsFromCsv } from './business-import-grid';
import type { ImportGridRow } from './business-import-grid';
function worker<T>(request: { operation: 'read' | 'write'; buffer?: ArrayBuffer; rows?: ImportGridRow[]; template?: boolean }, signal?: AbortSignal): Promise<T> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) { reject(new Error('FILE_CANCELLED')); return; }
    const task = new Worker(new URL('./business-import.worker.ts', import.meta.url), { type: 'module' });
    const finish = (result?: T, error?: string) => { clearTimeout(timer); signal?.removeEventListener('abort', cancel); task.terminate(); if (error) reject(new Error(error)); else resolve(result!); };
    const cancel = () => finish(undefined, 'FILE_CANCELLED'), timer = setTimeout(() => finish(undefined, 'XLSX_TIMEOUT'), 8000);
    signal?.addEventListener('abort', cancel, { once: true }); task.onmessage = event => finish(event.data.result as T, event.data.error); task.onerror = () => finish(undefined, 'XLSX_INVALID');
    task.postMessage(request, request.buffer ? [request.buffer] : []);
  });
}
export async function readImportFile(file: File, signal?: AbortSignal): Promise<ImportGridRow[]> {
  if (/\.csv$/i.test(file.name)) { if (file.size > 262144) throw new Error('FILE_SIZE'); return gridRowsFromCsv(new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer())); }
  if (/\.xlsx$/i.test(file.name)) { if (file.size > 1048576) throw new Error('FILE_SIZE'); return worker<ImportGridRow[]>({ operation: 'read', buffer: await file.arrayBuffer() }, signal); }
  throw new Error('FILE_EXTENSION');
}
export async function exportImportExcel(rows: ImportGridRow[], template = false, signal?: AbortSignal): Promise<Blob> {
  const buffer = await worker<ArrayBuffer>({ operation: 'write', rows, template }, signal); return new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
}
