import { defineConfig } from '@playwright/test';
import base from './playwright-admin-runtime.config';
export default defineConfig({ ...base, testMatch: 'admin-review-views.spec.ts', timeout: 90000 });
