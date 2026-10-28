import { createServer } from 'node:http';
import { readFile, realpath } from 'node:fs/promises';
import { resolve, sep, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
// Owned local static server for isolated browser units. It never implements a successful API.
const root = await realpath(fileURLToPath(new URL('../../dist-customer/', import.meta.url)));
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml' };
const nonce = process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE;
if (!nonce || !/^[0-9a-f-]{36}$/.test(nonce)) throw new Error('OWNED_UI_UNIT_SERVER_NONCE_REQUIRED');
const server = createServer(async (request, response) => {
  response.setHeader('Cache-Control', 'no-store'); response.setHeader('Referrer-Policy', 'no-referrer'); response.setHeader('X-Content-Type-Options', 'nosniff');
  try {
    const url = new URL(request.url, 'http://127.0.0.1:5181');
    if (url.pathname === '/__isolated-ui-unit-stop') {
      if (request.method !== 'POST' || request.headers['x-owned-ui-unit-nonce'] !== nonce) { response.writeHead(404); response.end(); return; }
      response.writeHead(204); response.end();
      setImmediate(() => { server.closeAllConnections(); server.close(() => process.exit(0)); }); return;
    }
    if (url.pathname.startsWith('/api/')) { response.writeHead(503, { 'Content-Type': 'application/json' }); response.end('{"code":"ISOLATED_UI_HAS_NO_BACKEND"}'); return; }
    if (request.method !== 'GET' && request.method !== 'HEAD') { response.writeHead(405); response.end(); return; }
    let file = resolve(root, '.' + decodeURIComponent(url.pathname));
    if (file !== root && !file.startsWith(root + sep)) throw new Error('INVALID_STATIC_PATH');
    if (!extname(file)) file = resolve(root, 'index.html');
    file = await realpath(file); if (!file.startsWith(root + sep)) throw new Error('INVALID_STATIC_PATH');
    const bytes = await readFile(file); response.writeHead(200, { 'Content-Type': mime[extname(file)] ?? 'application/octet-stream' }); response.end(request.method === 'HEAD' ? undefined : bytes);
  } catch { response.writeHead(404); response.end(); }
});
server.listen(5181, '127.0.0.1');
const stop = () => { server.closeAllConnections(); server.close(() => process.exit(0)); };
process.once('SIGTERM', stop); process.once('SIGINT', stop);
