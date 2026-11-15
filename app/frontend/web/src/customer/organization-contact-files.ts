export function contactFileWorker<T>(request: { operation: 'read' | 'write'; buffer?: ArrayBuffer; rows?: string[][] }, signal?: AbortSignal): Promise<T> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) { reject(new Error('FILE_CANCELLED')); return; } const worker = new Worker(new URL('./organization-contact-files.worker.ts', import.meta.url), { type: 'module' });
    const finish = (result?: T, error?: string) => { clearTimeout(timer); signal?.removeEventListener('abort', cancel); worker.terminate(); if (error) reject(new Error(error)); else resolve(result!); }, cancel = () => finish(undefined, 'FILE_CANCELLED'), timer = setTimeout(() => finish(undefined, 'FILE_TIMEOUT'), 8000);
    signal?.addEventListener('abort', cancel, { once: true }); worker.onmessage = e => finish(e.data.result as T, e.data.error); worker.onerror = () => finish(undefined, 'CONTACT_FILE_INVALID'); worker.postMessage(request, request.buffer ? [request.buffer] : []);
  });
}
export async function readContactFile(file: File, signal?: AbortSignal): Promise<string> {
  if (/\.csv$/i.test(file.name) && file.size <= 262144) return new TextDecoder('utf-8', { fatal: true }).decode(await file.arrayBuffer());
  if (/\.xlsx$/i.test(file.name) && file.size <= 1048576) return contactFileWorker<string>({ operation: 'read', buffer: await file.arrayBuffer() }, signal); throw new Error('CONTACT_FILE_INVALID');
}
