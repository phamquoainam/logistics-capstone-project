import { defineConfig } from '@playwright/test';
import { resolve } from 'node:path';

const origin = new URL(process.env.S003_BROWSER_ORIGIN ?? '');
const pin = process.env.S003_BROWSER_SPKI ?? '', directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (origin.protocol !== 'https:' || origin.hostname !== '127.0.0.1' || origin.pathname !== '/' || origin.username || origin.password || origin.search || origin.hash || !directory || !/^[A-Za-z0-9+/]{43}=$/.test(pin)) throw new Error('OWNED_CUSTOMER_BROWSER_CONTEXT_REQUIRED');
/** Actual HTTPS server/owned PG18/SMTP. No API fulfilment, trust-all TLS, retries or existing browser reuse. */
export default defineConfig({
  testDir: './runtime-tests', testMatch: 'customer-entry.spec.ts', workers: 1, retries: 0, forbidOnly: true,
  globalTimeout: 120000, timeout: 25000, expect: { timeout: 12000 },
  globalSetup: './runtime-tests/support/tls-preflight.ts', reporter: './tests/support/safe-reporter.ts',
  outputDir: resolve(directory, 'browser-owned-output'),
  use: { baseURL: origin.origin, browserName: 'chromium', headless: true, ignoreHTTPSErrors: false, screenshot: 'off', trace: 'off', video: 'off',
    // Synthetic fixture key only; Node preflight independently validates its CA/hostname and rejects a wrong hostname.
    launchOptions: { args: ['--ignore-certificate-errors-spki-list=' + pin] } },
});
