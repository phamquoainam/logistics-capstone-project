import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../../', import.meta.url));
const nonce = process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE;
if (!nonce || !/^[0-9a-f-]{36}$/.test(nonce)) throw new Error('OWNED_UI_UNIT_SERVER_NONCE_REQUIRED');
// Test-only React development effects. No API proxy, runtime credentials or TLS changes.
const server = await createServer({
  configFile: false, root, mode: 'test',
  define: { 'import.meta.env.VITE_PORTAL_MODE': JSON.stringify('customer') },
  plugins: [react(), { name: 'owned-organization-ui-unit-server', configureServer(vite) {
    vite.middlewares.use((request, response, next) => {
      response.setHeader('Cache-Control', 'no-store');
      if (request.url === '/__isolated-ui-unit-stop') {
        if (request.method !== 'POST' || request.headers['x-owned-ui-unit-nonce'] !== nonce) { response.writeHead(404); response.end(); return; }
        response.writeHead(204); response.end(); setImmediate(() => { void vite.close().then(() => process.exit(0)); }); return;
      }
      if (request.url?.startsWith('/api/')) { response.writeHead(503, { 'Content-Type': 'application/json' }); response.end('{"code":"ISOLATED_UI_HAS_NO_BACKEND"}'); return; }
      next();
    });
  } }],
  server: { host: '127.0.0.1', port: 5181, strictPort: true, fs: { strict: true, allow: [root, fileURLToPath(new URL('../../../../../packages/api-client/', import.meta.url))] } },
});
await server.listen();
const stop = () => { void server.close().then(() => process.exit(0)); };
process.once('SIGTERM', stop); process.once('SIGINT', stop);
