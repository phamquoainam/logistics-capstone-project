import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './unit-tests',
  workers: 1,
  retries: 0,
  reporter: 'list',
  outputDir: '../../../.demo/d1-02/web-unit-results',
});
