import { defineConfig } from '@playwright/test';
import base from './playwright-admin-runtime.config';
export default defineConfig({ ...base, testMatch: 'admin-organization-cases.spec.ts', timeout: 90000 });
