import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { parseOrganizationCase } from '../src/admin/organization-cases';
const directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (!directory) throw new Error('OWNED_ADMIN_CONTEXT_REQUIRED');

test('M2 Lifecycle scoped preparation on genuine HTTPS and PostgreSQL18', async ({ page, context }) => {
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { username: string; password: string; organizationId: string };
  await page.goto('/admin/login');await page.getByLabel('Tên đăng nhập', { exact: true }).fill(fixture.username);await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);
  const login = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/auth/login');await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();const session = await login;expect(session.status()).toBe(200);const access = (await session.json() as { accessToken: string }).accessToken;
  await expect(page.getByRole('heading', { name: 'Tổng quan điều hành', exact: true })).toBeVisible();
  await page.goto('/admin/merchants/organizations?view=lifecycle&organizationId=' + fixture.organizationId);
  const panel = page.locator('.ap-organization-cases');await expect(panel.getByRole('heading', { name: 'Hồ sơ Lifecycle', exact: true })).toBeVisible();
  const risk = panel.getByRole('row').filter({ has: page.getByRole('cell', { name: 'RISK_HOLD', exact: true }) });await expect(risk).toHaveCount(1);
  const detail = page.waitForResponse(r => r.request().method() === 'GET' && /\/governance-cases\/[0-9a-f-]{36}$/.test(new URL(r.url()).pathname));await risk.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();const opened = await detail;expect(opened.status()).toBe(200);parseOrganizationCase(await opened.json(), fixture.organizationId);
  await expect(panel.getByRole('heading', { name: 'RISK_HOLD · Nháp', exact: true })).toBeVisible();
  const disclosure = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/disclosures'));await panel.getByRole('button', { name: 'Đọc nội dung hồ sơ', exact: true }).click();expect((await disclosure).status()).toBe(200);await expect(panel.getByText('Synthetic preparation', { exact: true })).toBeVisible();
  await panel.getByLabel('Thao tác', { exact: true }).selectOption('SUBMIT');await panel.getByLabel('Lý do nghiệp vụ', { exact: true }).fill('Synthetic browser submission');
  const write = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/submission'));await panel.getByRole('button', { name: 'Lưu yêu cầu Lifecycle', exact: true }).click();const saved = await write;expect(saved.status()).toBe(200);
  const row = await saved.json() as { id: string; state: string; version: number };expect(row.state).toBe('SUBMITTED');expect(row.version).toBe(2);
  await expect(panel.getByRole('heading', { name: 'RISK_HOLD · Đã nộp', exact: true })).toBeVisible();
  const root = '/api/v1/admin/merchant-organizations/' + fixture.organizationId + '/governance-cases/' + row.id;
  expect((await context.request.post(root + '/submission', { headers: { Authorization: 'Bearer ' + access, Origin: new URL(page.url()).origin, 'Idempotency-Key': crypto.randomUUID() }, data: { reason: 'Missing ETag' } })).status()).toBe(428);
  expect((await context.request.post(root + '/cancellation', { headers: { Authorization: 'Bearer ' + access, Origin: 'https://foreign.example.invalid', 'Idempotency-Key': crypto.randomUUID(), 'If-Match': '"v2"' }, data: { reason: 'Denied foreign origin' } })).status()).toBe(401);
  expect((await context.request.get('/api/v1/admin/merchant-organizations/10000000-0000-4000-8000-000000000099/governance-cases', { headers: { Authorization: 'Bearer ' + access } })).status()).toBe(404);
  await page.setViewportSize({ width: 390, height: 844 });expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
  await page.screenshot({ path: resolve(directory!, 'lifecycle-mobile.png'), fullPage: true });
});
