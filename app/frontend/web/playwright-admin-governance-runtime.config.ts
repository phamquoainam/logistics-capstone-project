import { defineConfig } from '@playwright/test';
import base from './playwright-admin-runtime.config';
/** Same owned TLS pins and redacted reporter; a separate explicit genuine workflow fixture. */
export default defineConfig({ ...base, testMatch: 'admin-governance.spec.ts', timeout: 60000 });
