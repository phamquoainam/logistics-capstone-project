// Test-only override. Normal dev/preview proxy and ports remain in vite.config.ts.
import { mergeConfig } from 'vite';
import baseline from './vite.config.ts';

const target = process.env.C04_BACKEND_ORIGIN;
const port = Number(process.env.C04_WEB_PORT);
if (!target || !/^http:\/\/127\.0\.0\.1:\d+$/.test(target) || !Number.isInteger(port) || port < 1024 || port > 65535) {
  throw new Error('Use the isolated C-04 web harness');
}
export default mergeConfig(baseline, { server: { port, proxy: { '/api': { target } } } });
