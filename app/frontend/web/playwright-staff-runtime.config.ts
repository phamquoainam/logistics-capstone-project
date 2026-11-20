import { defineConfig } from '@playwright/test';
import customer from './playwright-customer-runtime.config';
/** Same owned TLS/PG/SMTP/privacy gates; focused Staff slice, no route fulfilment. */
export default defineConfig({ ...customer, testMatch: 'staff-entry.spec.ts' });
