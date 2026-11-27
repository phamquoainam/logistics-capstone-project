import { defineConfig } from '@playwright/test';
import base from './playwright-admin-runtime.config';
export default defineConfig({ ...base, testMatch: 'admin-core.spec.ts', timeout: 120000, globalTimeout: 210000 });
