import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';

// Intercepted browser contracts only; no real SYSTEM login, Realm activation or DB evidence.
const origin = 'https://admin.example.invalid';
const id = '10000000-0000-4000-8000-000000000001';
const user = { id, username: 'synthetic-admin', displayName: 'Quản trị thử nghiệm', email: 'admin@example.invalid', status: 'ACTIVE', grants: [{ role: 'IDENTITY_MANAGEMENT', scopeType: 'IDENTITY', scopeId: '20000000-0000-4000-8000-000000000001' }], version: 1, emailVerified: true };
async function fixture(context: BrowserContext, options: { authenticated?: boolean; reject?: number; logoutFails?: boolean; loginDelay?: boolean } = {}) {
  let authenticated = options.authenticated ?? false, logoutFails = options.logoutFails ?? false;
  const calls: { path: string; method: string }[] = [];
  await context.route(origin + '/**', async route => {
    const request = route.request(), url = new URL(request.url());
    if (!url.pathname.startsWith('/api/')) {
      // Static UI comes from the owned test server; browser origin stays HTTPS.
      const response = await context.request.get('http://127.0.0.1:5181' + url.pathname);
      return route.fulfill({ response });
    }
    calls.push({ path: url.pathname, method: request.method() });
    const reply = (data: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(data) });
    if (url.pathname === '/api/v1/admin/dashboard/context') return reply({ code: 'ACCESS_DENIED' }, 403);
    if (url.pathname === '/api/v1/auth/csrf') return reply({ csrfToken: 'synthetic-admin-csrf-challenge' });
    if (url.pathname === '/api/v1/auth/logout') {
      if (logoutFails) { logoutFails = false; return route.abort(); }
      authenticated = false; return route.fulfill({ status: 204 });
    }
    if (url.pathname === '/api/v1/auth/login' && options.reject) return reply({ code: 'INVALID_CREDENTIALS', message: 'private diagnostic must not appear' }, options.reject);
    if (url.pathname === '/api/v1/auth/login') {
      if (options.loginDelay) await new Promise(resolve => setTimeout(resolve, 150));
      expect(request.postDataJSON()).toEqual({ username: 'synthetic-admin', password: 'synthetic-password' });
      expect(request.headers()['x-csrf-token']).toBe('synthetic-admin-csrf-challenge');
      authenticated = true;
    }
    if (!authenticated) return reply({ code: 'SESSION_INVALID' }, 401);
    if (url.pathname === '/api/v1/auth/me') {
      expect(request.headers().authorization).toBe('Bearer ' + 'a'.repeat(128)); return reply(user);
    }
    if (url.pathname === '/api/v1/auth/login' || url.pathname === '/api/v1/auth/refresh') return reply({ accessToken: 'a'.repeat(128), tokenType: 'Bearer', expiresIn: 900, sessionExpiresAt: new Date(Date.now() + 28800000).toISOString(), user });
    return reply({ code: 'UNSUPPORTED_TEST_ROUTE' }, 503);
  });
  return calls;
}
test('guest enters SYSTEM login directly; login does not imply Carrier data access', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await expect(page).toHaveURL(origin + '/admin/login');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('synthetic-admin');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-password');
  await page.getByRole('button', { name: 'Hiện mật khẩu', exact: true }).click();
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveAttribute('type', 'text');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(origin + '/admin');
  await expect(page.getByRole('heading', { name: 'Tổng quan điều hành' })).toBeVisible();
  await expect(page.getByText('Quyền quản trị Identity hiện tại chưa bao gồm quyền đọc dữ liệu điều hành Carrier.')).toBeVisible();
  expect(calls.every(call => call.path.startsWith('/api/v1/auth/') || call.path === '/api/v1/admin/dashboard/context')).toBe(true);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await expect(page).toHaveURL(origin + '/admin/login');
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');
});
test('invalid login is generic, clears password and never displays private diagnostics', async ({ page, context }) => {
  const calls = await fixture(context, { reject: 401 }); await page.goto(origin + '/admin/login');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('synthetic-admin'); await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-password');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Tên đăng nhập hoặc mật khẩu không đúng');
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');
  await expect(page.getByText('private diagnostic must not appear')).toHaveCount(0);
  expect(calls.filter(c => c.path.endsWith('/login'))).toHaveLength(1);
});
test('duplicate form submission sends one login request', async ({ page, context }) => {
  const calls = await fixture(context, { loginDelay: true }); await page.goto(origin + '/admin/login');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('synthetic-admin'); await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-password');
  await page.locator('form').evaluate(form => { form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true })); });
  await expect(page).toHaveURL(origin + '/admin'); expect(calls.filter(c => c.path.endsWith('/login'))).toHaveLength(1);
});
test('rate limited login gives a clear retry message without private details', async ({ page, context }) => {
  await fixture(context, { reject: 429 }); await page.goto(origin + '/admin/login');
  await page.getByLabel('Tên đăng nhập', { exact: true }).fill('synthetic-admin'); await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-password');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Có quá nhiều yêu cầu'); await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');
});
test('refresh restores accepted SYSTEM session; failed logout offers explicit retry', async ({ page, context }) => {
  const calls = await fixture(context, { authenticated: true, logoutFails: true }); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Đăng xuất', exact: true })).toBeVisible();
  await page.reload(); await expect(page.getByRole('button', { name: 'Đăng xuất', exact: true })).toBeVisible();
  expect(calls.filter(c => c.path.endsWith('/refresh'))).toHaveLength(2);
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('chưa xác nhận được đăng xuất trên máy chủ');
  expect(calls.filter(c => c.path.endsWith('/logout'))).toHaveLength(1);
  await page.getByRole('button', { name: 'Thử đăng xuất lại' }).click();
  await expect(page.getByRole('alert')).toHaveCount(0);
  expect(calls.filter(c => c.path.endsWith('/logout'))).toHaveLength(2);
});
test('HTTP blocks credentials without contacting an authentication endpoint', async ({ page }) => {
  const calls: string[] = []; await page.route('**/api/**', async route => { calls.push(route.request().url()); await route.abort(); });
  await page.goto('/admin/login'); await expect(page.getByRole('alert')).toContainText('HTTPS');
  await expect(page.getByRole('button', { name: 'Đăng nhập', exact: true })).toBeDisabled(); expect(calls).toEqual([]);
});
test('desktop and mobile login remain compact, readable and free of horizontal overflow', async ({ page, context }) => {
  await fixture(context); await page.goto(origin + '/admin/login');
  await expect(page.getByRole('heading', { name: 'Đăng nhập quản trị' })).toBeVisible();
  await expect(page).toHaveTitle('ArgoPost | Quản trị');
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-login-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.getByRole('heading', { name: 'Đăng nhập quản trị' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-login-mobile.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('link', { name: 'Website ArgoPost' }).click();
  await expect(page).toHaveURL(origin + '/');
  await expect(page.getByRole('heading', { name: 'Đăng nhập quản trị' })).toHaveCount(0);
});
