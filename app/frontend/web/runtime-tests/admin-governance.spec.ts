import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (!directory) throw new Error('OWNED_ADMIN_CONTEXT_REQUIRED');

test('genuine HTTPS SYSTEM reviewer uses scoped queue, immutable facts and current PG18 checklist', async ({ page, context }) => {
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { username: string; password: string; applicationId: string; flow?: 'REVIEW' | 'REJECT' | 'APPROVE' };
  if (!/^[a-f0-9-]{36}$/.test(fixture.applicationId)) throw new Error('OWNED_APPLICATION_REQUIRED');
  await page.goto('/admin/login');await page.getByLabel('Tên đăng nhập', { exact: true }).fill(fixture.username);await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);
  const logged = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/auth/login');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  const login = await logged;expect(login.status()).toBe(200);let access = (await login.json() as { accessToken: string }).accessToken;
  await expect(page.getByRole('heading', { name: 'Tổng quan điều hành', exact: true })).toBeVisible();
  await page.goto('/admin/merchants/applications');
  await expect(page.getByRole('heading', { name: 'Hồ sơ đã nộp', exact: true })).toBeVisible();
  await expect(page.locator('tbody tr')).toHaveCount(1);await expect(page.locator('tbody')).not.toContainText('business-contact@example.invalid');
  await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();
  if (fixture.flow === 'REJECT' || fixture.flow === 'APPROVE') {
    const approval = fixture.flow === 'APPROVE';
    await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 6', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true })).toHaveCount(0);
    if (approval) await page.getByLabel('Quyết định', { exact: true }).selectOption('APPROVE');
    await page.getByLabel(approval ? 'Lý do phê duyệt' : 'Lý do từ chối', { exact: true }).fill('Synthetic genuine HTTPS independent rejection');
    await page.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill(fixture.password);
    const step = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/admin/governance-reauthentications');
    const decision = page.waitForResponse(r => new URL(r.url()).pathname.endsWith(approval ? '/approval' : '/rejection'));
    await page.getByRole('button', { name: approval ? 'Xác thực & phê duyệt hồ sơ' : 'Xác thực & từ chối hồ sơ', exact: true }).click();
    expect((await step).status()).toBe(200);expect((await decision).status()).toBe(200);
    await expect(page.getByText(approval ? 'Đã duyệt · Revision 1 · Phiên bản 7' : 'Từ chối · Revision 1 · Phiên bản 7', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Mật khẩu xác thực lại', { exact: true })).toHaveCount(0);
  } else {
  await expect(page.getByText('Đã nộp · Revision 1 · Phiên bản 4', { exact: true })).toBeVisible();
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic HTTPS beginning');
  await page.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true }).click();
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 5', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Synthetic shorter display name', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true })).toBeDisabled();
  for (const name of ['Hồ sơ đăng ký hợp lệ', 'Thẩm quyền đại diện hợp lệ', 'Liên hệ đã xác minh', 'Đã đối chiếu trùng lặp', 'Không có xung đột đã biết']) await page.getByRole('checkbox', { name, exact: true }).check();
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic HTTPS complete checklist');
  const ready = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/review'));
  await page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true }).click();expect((await ready).status()).toBe(200);
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 6', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true })).toBeDisabled();
  }
  if (fixture.flow === 'APPROVE') {
    await page.goto('/admin/merchants/organizations');await expect(page.getByRole('heading', { name: 'Tổ chức Merchant', exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Mở tổ chức', exact: true }).click();
    for (const operation of ['SUSPEND', 'REINSTATE'] as const) {
      await expect(page.getByRole('heading', { name: operation === 'SUSPEND' ? 'Tạm ngừng tổ chức' : 'Khôi phục tổ chức', exact: true })).toBeVisible();
      await page.getByLabel('Lý do thay đổi', { exact: true }).fill('Synthetic HTTPS organization ' + operation);
      await page.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill(fixture.password);
      const changed = page.waitForResponse(r => new URL(r.url()).pathname.endsWith(operation === 'SUSPEND' ? '/suspension' : '/reinstatement'));
      await page.getByRole('button', { name: operation === 'SUSPEND' ? 'Xác thực & tạm ngừng' : 'Xác thực & khôi phục', exact: true }).click();expect((await changed).status()).toBe(200);
      await expect(page.getByText(operation === 'SUSPEND' ? 'Tạm ngừng · Phiên bản 2' : 'Đang hoạt động · Phiên bản 3', { exact: true })).toBeVisible();
    }
  }
  // The command also refreshes scope/list through independent read admissions.
  // Finish those requests before checking a different principal request boundary;
  // concurrent population-lock contention is intentionally SERVICE_UNAVAILABLE.
  await page.waitForLoadState('networkidle');
  const foreign = await context.request.get('/api/v1/admin/merchant-applications/70000000-0000-4000-8000-000000000001', { headers: { Authorization: 'Bearer ' + access } });expect(foreign.status()).toBe(404);
  const anonymous = await context.request.get('/api/v1/admin/merchant-applications/' + fixture.applicationId);expect(anonymous.status()).toBe(401);access = '';
  await page.setViewportSize({ width: 390, height: 844 });expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: resolve(directory!, 'governance-real-mobile.png'), fullPage: true, animations: 'disabled' });
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(page).toHaveURL(/\/admin\/login$/);
});
