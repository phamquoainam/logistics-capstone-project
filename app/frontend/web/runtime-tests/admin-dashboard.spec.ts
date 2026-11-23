import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (!directory) throw new Error('OWNED_ADMIN_CONTEXT_REQUIRED');
test.describe.configure({ mode: 'serial' });
test('real SMTP invitation activates through HTTPS without session restore or automatic login', async ({ page }) => {
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { password: string; activation: { proofId: string; token: string; username: string } };
  const requests: string[] = []; page.on('request', request => { const path = new URL(request.url()).pathname; if (path.startsWith('/api/')) requests.push(path); });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/admin/activate#proofId=' + fixture.activation.proofId + '&token=' + fixture.activation.token);
  await expect(page.getByRole('heading', { name: 'Kích hoạt tài khoản quản trị' })).toBeVisible();
  expect(new URL(page.url()).hash).toBe(''); expect(requests).toEqual([]);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill(fixture.password); await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill(fixture.password);
  const accepted = page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/auth/admin-invitations/accept');
  await page.getByRole('button', { name: 'Xác minh và đặt mật khẩu' }).click(); expect((await accepted).status()).toBe(204);
  await expect(page).toHaveURL(/\/admin\/login$/); await expect(page.getByRole('status').filter({ hasText: 'Đã xác minh' })).toBeVisible();
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill(fixture.activation.username); await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click(); await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 2 vận đơn' })).toBeVisible();
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page).toHaveURL(/\/admin\/login$/);
});
async function login(page: import('@playwright/test').Page) {
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { username: string; password: string };
  await page.goto('/admin/login'); await page.getByLabel('Tên đăng nhập', { exact: true }).fill(fixture.username); await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 2 vận đơn' })).toBeVisible();
}
test('real SYSTEM HTTPS login reads confirmed PostgreSQL counts and exact unpaid obligations', async ({ page }) => {
  await login(page); await expect(page.getByRole('button', { name: 'Kiện hàng: 5 kiện' })).toBeVisible(); await expect(page.getByRole('button', { name: 'Đã tiếp nhận: 1 vận đơn' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Cước chưa thanh toán: 125.000 VND' })).toBeVisible(); await expect(page.getByRole('heading', { name: 'Tình trạng API quản trị' })).toBeVisible();
  await page.getByText('Nguồn dữ liệu & phạm vi thống kê', { exact: true }).click(); await expect(page.getByText('Nguồn nghiệp vụ chưa được triển khai').first()).toBeVisible(); await expect(page.getByText('Giao thành công', { exact: true })).toHaveCount(0);
  await page.screenshot({ path: resolve(directory!, 'admin-real-desktop.png'), fullPage: true, animations: 'disabled' });
});
test('real KPI drill-through preserves snapshot filters and contributing shipments', async ({ page }) => {
  await login(page); await page.getByRole('button', { name: 'Đã tiếp nhận: 1 vận đơn' }).click(); await expect(page.getByRole('table')).toBeVisible(); await expect(page.locator('tbody tr')).toHaveCount(1); await expect(page.locator('tbody tr').first()).toContainText('Đã tiếp nhận'); await expect(page.locator('tbody tr').first().locator('td').last()).toHaveText('3');
  await page.getByRole('button', { name: 'Về tổng quan' }).click(); await page.getByRole('button', { name: '7 ngày', exact: true }).click(); await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 2 vận đơn' })).toBeVisible();
  await page.getByRole('button', { name: 'Chờ bàn giao: 1 vận đơn' }).click(); await expect(page.locator('tbody tr')).toHaveCount(1); await expect(page.locator('tbody tr').first()).toContainText('Chờ bàn giao');
});
test('real refresh cookies restore the Admin session and mobile dashboard fits the viewport', async ({ page }) => {
  await login(page); await page.reload(); await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 2 vận đơn' })).toBeVisible(); await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  await page.screenshot({ path: resolve(directory!, 'admin-real-mobile.png'), fullPage: true, animations: 'disabled' });
});
test('real logout invalidates the cookie-bound session and denies unauthenticated APIs', async ({ page, context }) => {
  await login(page); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page).toHaveURL(/\/admin\/login$/); await page.goto('/admin'); await expect(page).toHaveURL(/\/admin\/login$/);
  const denied = await context.request.get('/api/v1/admin/dashboard/context'); expect(denied.status()).toBe(401);
});
