import { defineConfig, loadEnv } from 'vite';
import { readFileSync } from 'node:fs';
import react from '@vitejs/plugin-react';
const proxy = { '/api': { target: 'http://127.0.0.1:8080', changeOrigin: false } };
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const portalMode = env.VITE_PORTAL_MODE ?? (mode === 'customer' ? 'customer' : 'demo');
  if (!['demo', 'customer'].includes(portalMode)) throw new Error('PORTAL_MODE_INVALID');
  const common = { plugins: [react()], define: { 'import.meta.env.VITE_PORTAL_MODE': JSON.stringify(portalMode) }, build: { outDir: portalMode === 'customer' ? 'dist-customer' : 'dist' } };
  if (command !== 'serve' || portalMode !== 'customer') return { ...common, server: { port: 5173, strictPort: true, proxy }, preview: { port: 4173, strictPort: true, proxy } };
  // Explicit local sandbox TLS. No insecure backend proxy, generated key or trust-all fallback.
  const target = new URL(env.ELOGISTICS_CUSTOMER_DEV_API_ORIGIN ?? '');
  if (target.protocol !== 'https:' || !['localhost', '127.0.0.1', '[::1]'].includes(target.hostname) || target.pathname !== '/' || target.username || target.password || target.search || target.hash) throw new Error('CUSTOMER_SANDBOX_API_ORIGIN_REQUIRED');
  if (!env.ELOGISTICS_CUSTOMER_DEV_TLS_KEY || !env.ELOGISTICS_CUSTOMER_DEV_TLS_CERT) throw new Error('CUSTOMER_SANDBOX_TLS_REQUIRED');
  const https = { key: readFileSync(env.ELOGISTICS_CUSTOMER_DEV_TLS_KEY), cert: readFileSync(env.ELOGISTICS_CUSTOMER_DEV_TLS_CERT) };
  const customerProxy = { '/api': { target: target.origin, changeOrigin: false, secure: true } };
  const headers = { 'Cache-Control': 'no-store', 'Referrer-Policy': 'no-referrer', 'X-Content-Type-Options': 'nosniff' };
  return { ...common, server: { host: '127.0.0.1', port: 5173, strictPort: true, https, headers, proxy: customerProxy }, preview: { host: '127.0.0.1', port: 4173, strictPort: true, https, headers, proxy: customerProxy } };
});
