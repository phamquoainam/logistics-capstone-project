import { defineConfig } from '@playwright/test';
import { randomUUID } from 'node:crypto';
const nonce = randomUUID();
process.env.ELOGISTICS_UI_UNIT_SERVER_NONCE = nonce;
/** Development StrictMode regression only; intercepted synthetic APIs, not E2E. */
export default defineConfig({
  testDir: './ui-unit-tests', testMatch: /organization-(contacts|actions)\.spec\.ts/,
  workers: 1, retries: 0, reporter: 'list',
  globalTeardown: './ui-unit-tests/support/teardown.ts',
  outputDir: '../../../.demo/organization-action-address/development-ui-results',
  use: { baseURL: 'http://127.0.0.1:5181', channel: process.env.GITHUB_ACTIONS === 'true' ? undefined : 'msedge', headless: true, viewport: { width: 1280, height: 900 }, screenshot: 'off', trace: 'off' },
  webServer: { command: 'node ui-unit-tests/support/organization-dev-server.mjs', env: { ELOGISTICS_UI_UNIT_SERVER_NONCE: nonce }, url: 'http://127.0.0.1:5181', timeout: 30000, reuseExistingServer: false },
});
