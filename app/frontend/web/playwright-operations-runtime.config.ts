import { defineConfig } from '@playwright/test';
import customer from './playwright-customer-runtime.config';
/** One owned HTTPS/PG18/SMTP journey; no API fulfilment or trust-all TLS. */
export default defineConfig({ ...customer, testMatch: 'operations-entry.spec.ts', timeout: 90000 });
