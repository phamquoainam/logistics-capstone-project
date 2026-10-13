import { defineConfig } from '@playwright/test';
export default defineConfig({ testDir: './tests', workers: 1, retries: 0, reporter: 'list', outputDir: '../../../.demo/playwright-results', use: { baseURL: 'http://127.0.0.1:5173', channel: 'msedge', headless: true, viewport: { width: 1440, height: 960 }, screenshot: 'only-on-failure', trace: 'off' } });
