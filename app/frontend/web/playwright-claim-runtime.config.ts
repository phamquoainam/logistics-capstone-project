import { defineConfig } from '@playwright/test';
import customer from './playwright-customer-runtime.config';
export default defineConfig({ ...customer, testMatch: 'claim-intake.spec.ts', timeout: 45000 });
