import { test, expect } from '@playwright/test';
import type { BrowserContext, Route } from '@playwright/test';
const account = '10000000-0000-4000-8000-000000000001';
async function fixture(context: BrowserContext, special?: (route: Route, path: string) => Promise<boolean>, guest = false) {
  const calls: { path: string; method: string }[] = [];
  await context.route('**/api/v1/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname; calls.push({ path, method: request.method() });
    if (special && await special(route, path)) return;
    const reply = (body: unknown, status = 200, headers: Record<string, string> = {}) => route.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-support-csrf' });
    if (path.endsWith('/refresh')) return guest ? reply({ code: 'SESSION_INVALID' }, 401) : reply({ accessToken: 'synthetic-support-access', accountId: account, expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/logout')) return route.fulfill({ status: 204 });
    if (path.endsWith('/profile')) return reply({ id: account, accountId: account, displayName: 'Synthetic profile', preferredLanguage: 'en', version: 1 }, 200, { ETag: '"v1"' });
    return reply({ code: 'ISOLATED_UI_UNSUPPORTED' }, 503);
  }); return calls;
}

test('support is a peer navigation item with searchable purposeful guidance and no ticket mutation', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/personal/support?view=help');
  await expect(page.getByRole('heading', { name: 'Hỗ trợ & Khiếu nại', exact: true })).toBeVisible();
  const nav = page.getByRole('navigation', { name: 'Điều hướng Personal' });
  await expect(nav.locator('#personal-management-links').getByRole('link', { name: 'Hỗ trợ & Khiếu nại' })).toHaveCount(0);
  await expect(nav.getByRole('link', { name: 'Hỗ trợ & Khiếu nại', exact: true }).locator('.pw-nav-dot')).toHaveCount(0);
  await page.getByLabel('Tìm trong hướng dẫn').fill('Code128'); await expect(page.locator('.sa-faq details')).toHaveCount(1);
  await page.locator('.sa-faq summary').click(); await expect(page.locator('.sa-faq')).toContainText('trang tiếp nối');
  await page.getByRole('button', { name: 'Tài khoản', exact: true }).click(); await expect(page.getByRole('status')).toHaveText('0 hướng dẫn phù hợp');
  await page.getByLabel('Tìm trong hướng dẫn').fill(''); await expect(page.locator('.sa-faq details')).toHaveCount(3);
  await page.getByRole('button', { name: 'Khiếu nại & Bồi thường', exact: true }).click(); await expect(page).toHaveURL(/kind=claim/);
  await expect(page.locator('.sa-request')).toContainText('Quyết định và chi trả cần được phê duyệt riêng');
  await expect(page.locator('.sa-request')).toContainText('Chưa tiếp nhận tải lên');
  await page.getByRole('button', { name: 'Yêu cầu thay đổi vận đơn', exact: true }).click(); await expect(page.locator('.sa-request')).toContainText('không gửi lệnh hủy');
  await expect(page.locator('input[type=file],textarea,button[type=submit]')).toHaveCount(0);
  expect(calls.filter(c => !c.path.startsWith('/api/v1/customer-auth/'))).toEqual([]);
});

for (const width of [1440, 768, 390, 360]) test(`avatar opens accessible viewport-safe account disclosure at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 900 }); await page.goto('/personal/support?view=help');
  const trigger = page.getByLabel('Menu tài khoản Synthetic Customer', { exact: true }), popup = page.locator('.pw-user-dropdown');
  await trigger.locator('.pw-avatar').click(); await expect(popup).toBeVisible();
  const box = (await popup.boundingBox())!; expect(box.x).toBeGreaterThanOrEqual(0); expect(box.x + box.width).toBeLessThanOrEqual(width); expect(box.y + box.height).toBeLessThan(900);
  await expect(popup.getByRole('link')).toHaveCount(4);
  await page.screenshot({ path: `../../../.demo/support-account-review/account-${width}.png`, animations: 'disabled' });
  await page.keyboard.press('Escape'); await expect(popup).not.toBeVisible(); await expect(trigger).toBeFocused();
  await page.keyboard.press('Enter'); await expect(popup).toBeVisible(); await page.locator('.sa-request h2').click(); await expect(popup).not.toBeVisible();
  await trigger.click(); await popup.getByRole('link', { name: 'Cài đặt & Quyền riêng tư' }).click(); await expect(page).toHaveURL(/\/personal\/settings$/); await expect(popup).not.toBeVisible();
  await expect(page.getByText('Tiếng Anh', { exact: true })).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: `../../../.demo/support-account-review/settings-${width}.png`, fullPage: true, animations: 'disabled' });
  await trigger.focus(); await page.keyboard.press('Enter'); await page.keyboard.press('Tab'); await expect(popup.getByRole('link', { name: 'Hồ sơ cá nhân', exact: true })).toBeFocused();
  for (let i = 0; i < 4; i++) await page.keyboard.press('Tab'); await expect(popup).not.toBeVisible();
});

test('settings reads real profile preference, exposes no fabricated notification, payment or data-request controls', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/personal/settings');
  await expect(page.getByText('Tiếng Anh', { exact: true })).toBeVisible(); await expect(page.locator('.sa-settings-grid')).toContainText('Giao diện hiện dùng tiếng Việt');
  await expect(page.locator('.sa-settings-grid input,.sa-settings-grid select')).toHaveCount(0);
  await expect(page.getByRole('button', { name: /lưu|thanh toán|xóa dữ liệu/i })).toHaveCount(0);
  await page.getByRole('link', { name: 'Đổi ngôn ngữ ưu tiên' }).click(); await expect(page.getByLabel('Ngôn ngữ ưu tiên')).toHaveValue('en');
  await expect(page.getByText(/Liên hệ tự khai, chưa xác minh/)).toBeVisible();
  expect(calls.filter(c => !c.path.startsWith('/api/v1/customer-auth/')).every(c => c.path === '/api/v1/customer/profile' && c.method === 'GET')).toBe(true);
});

test('failed and foreign profile reads cannot show a made-up saved preference; retry is read-only', async ({ page, context }) => {
  let reads = 0;
  const calls = await fixture(context, async (route, path) => {
    if (!path.endsWith('/profile')) return false; reads++;
    if (reads === 1) await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'SERVICE_UNAVAILABLE' }) });
    else if (reads === 2) await route.fulfill({ contentType: 'application/json', headers: { ETag: '"v1"' }, body: JSON.stringify({ id: account, accountId: '10000000-0000-4000-8000-000000000002', displayName: 'Foreign profile', preferredLanguage: 'en', version: 1 }) });
    else return false; return true;
  });
  await page.goto('/personal/settings'); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByText('Tiếng Việt', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Tải lại tùy chọn' }).click(); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.getByText('Foreign profile', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Tải lại tùy chọn' }).click(); await expect(page.getByText('Tiếng Anh', { exact: true })).toBeVisible(); expect(reads).toBe(3);
  expect(calls.filter(c => c.path.endsWith('/profile')).every(c => c.method === 'GET')).toBe(true);
});

test('security reset uses existing logout then recovery, not a fake password or email change', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/personal/security'); await expect(page.getByText('synthetic@example.invalid', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Đăng xuất để đặt lại mật khẩu' }).click(); await expect(page).toHaveURL(/\/recover$/);
  await expect(page.getByLabel('Email của tài khoản')).toBeVisible(); expect(calls.filter(c => c.path.endsWith('/logout'))).toHaveLength(1);
  expect(calls.some(c => /email-change|recovery-confirm|support-ticket|claim/.test(c.path))).toBe(false);
});

test('guests cannot read settings or support behind personal admission', async ({ page, context }) => {
  const calls = await fixture(context, undefined, true); await page.goto('/personal/settings'); await expect(page).toHaveURL(/\/login$/);
  expect(calls.some(c => c.path.endsWith('/profile'))).toBe(false); await expect(page.locator('.sa-settings-grid')).toHaveCount(0);
});
