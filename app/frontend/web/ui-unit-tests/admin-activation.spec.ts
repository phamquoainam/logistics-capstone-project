import { test, expect } from '@playwright/test';
const origin = 'https://admin.example.invalid', id = '10000000-0000-4000-8000-000000000001', token = 'A'.repeat(43);
test('activation scrubs proof before API work, validates password and returns to login without logging in', async ({ page, context }) => {
  const calls: string[] = [];
  await context.route(origin + '/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (!path.startsWith('/api/')) return route.fulfill({ response: await context.request.get('http://127.0.0.1:5181' + path) });
    calls.push(path);
    if (path === '/api/v1/auth/csrf') return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ csrfToken: 'synthetic-admin-csrf-challenge' }) });
    if (path === '/api/v1/auth/admin-invitations/accept') return route.fulfill({ status: 204 });
    return route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ code: 'SESSION_INVALID' }) });
  });
  await page.goto(origin + '/admin/activate#proofId=' + id + '&token=' + token); await expect(page.getByRole('heading', { name: 'Kích hoạt tài khoản quản trị' })).toBeVisible();
  expect(new URL(page.url()).hash).toBe(''); expect(calls).toEqual([]);
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('short'); await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill('short'); await page.getByRole('button', { name: 'Xác minh và đặt mật khẩu' }).click();
  await expect(page.getByRole('alert')).toContainText('15 đến 72'); expect(calls).toEqual([]);
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('synthetic password for test'); await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill('synthetic password for test'); await page.getByRole('button', { name: 'Xác minh và đặt mật khẩu' }).click();
  await expect(page).toHaveURL(origin + '/admin/login'); await expect(page.getByLabel('Tên đăng nhập', { exact: true })).toBeVisible(); expect(calls.filter(p => p.endsWith('/login'))).toEqual([]);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
test('query-carried or absent invitation never sends an acceptance request', async ({ page, context }) => {
  const calls: string[] = [];
  await context.route(origin + '/**', async route => {
    const path = new URL(route.request().url()).pathname;
    if (path.startsWith('/api/')) { calls.push(path); return route.fulfill({ status: 503 }); }
    return route.fulfill({ response: await context.request.get('http://127.0.0.1:5181' + path) });
  });
  await page.goto(origin + '/admin/activate?proofId=' + id + '#proofId=' + id + '&token=' + token);
  await expect(page.getByRole('alert')).toContainText('Chưa có lời mời hợp lệ'); await expect(page).toHaveURL(origin + '/admin/activate'); expect(calls).toEqual([]);
});
