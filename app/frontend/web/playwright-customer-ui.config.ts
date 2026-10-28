import { defineConfig } from '@playwright/test';
import { randomUUID } from 'node:crypto';
const nonce = process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE ?? randomUUID();
process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE = nonce;
/** Isolated UI units with explicit intercepted API fixtures, never full-stack acceptance evidence. */
export default defineConfig({
  testDir: './ui-unit-tests', workers: 1, retries: 0, reporter: 'list',
  globalTeardown: './ui-unit-tests/support/teardown.ts',
  outputDir: '../../../.demo/spec003-implementation/customer-ui-unit-results',
  use: { baseURL: 'http://127.0.0.1:5181', channel: 'msedge', headless: true, viewport: { width: 1280, height: 900 }, screenshot: 'off', trace: 'off' },
  webServer: { command: 'npm run build:customer && node ui-unit-tests/support/static-server.mjs', env: { ELOGISTICS_UI_UNIT_SERVER_NONCE: nonce }, url: 'http://127.0.0.1:5181', timeout: 60000, reuseExistingServer: false },
});
