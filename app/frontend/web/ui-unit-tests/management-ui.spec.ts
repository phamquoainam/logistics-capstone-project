import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { managementFixture } from './support/management-fixture';
import { analyticsFixture } from './support/analytics-fixture';

async function fixture(context: BrowserContext, count = 45) {
  const reads: URL[] = [];
  const records = Array.from({ length: Math.ceil(count / 20) }, (_, page) => managementFixture(new URLSearchParams({ page: String(page) }), count).items).flat();
  await context.route('**/api/v1/**', async route => {
    const u = new URL(route.request().url()), path = u.pathname;
    const reply = (value: unknown, headers: Record<string, string> = {}) => route.fulfill({ contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-management-challenge' });
    if (path.endsWith('/refresh')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/shipments')) { reads.push(u); return reply(managementFixture(u.searchParams, count)); }
    if (path.endsWith('/personal-analytics')) return reply(analyticsFixture(u.searchParams, 25));
    if (path.endsWith('/shipment-drafts')) return reply({ items: [], page: 0, size: 20, hasNext: false });
    const record = records.find(i => path.endsWith('/shipments/' + i.record.shipment.id));
    if (record) return reply(record.record, { ETag: '"v1"' });
    return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'ISOLATED_UI_UNSUPPORTED' }) });
  }); return reads;
}
test('management reconciles KPI status and source pages through URL Back and Forward', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  await expect(page.getByRole('button', { name: /Tổng vận đơn/ })).toContainText('45');
  await page.getByRole('button', { name: /Tại Station/ }).click(); await expect(page).toHaveURL(/status=AT_STATION/);
  await expect(page.locator('.pm-results-heading')).toContainText('22 vận đơn'); await expect(page.getByRole('button', { name: /Tổng vận đơn/ })).toContainText('45');
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(2);
  await page.goBack(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20); await page.goForward(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(2);
  await page.locator('.pm-table tbody tr').first().getByRole('link', { name: 'Xem vận đơn →', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn', exact: true })).toBeVisible();
});
test('advanced filters are sent together to Backend and reset page rather than hiding local rows', async ({ page, context }) => {
  const reads = await fixture(context); await page.goto('/personal/shipments?page=1'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  await page.getByLabel('Người nhận', { exact: true }).fill('Synthetic contact'); await page.getByLabel('Từ ngày', { exact: true }).fill('2026-11-02'); await page.getByLabel('Đến ngày', { exact: true }).fill('2026-11-02');
  await page.getByLabel('Trạng thái', { exact: true }).selectOption('AT_STATION'); await page.getByLabel('Dịch vụ', { exact: true }).selectOption('ROAD_STANDARD'); await page.getByLabel('Tuyến gửi hàng', { exact: true }).selectOption('SIM-A1/SIM-A2'); await page.getByLabel('Nghĩa vụ cước', { exact: true }).selectOption('UNPAID'); await page.getByLabel('Sắp xếp', { exact: true }).selectOption('FEE_DESC');
  await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(8);
  await expect(page.locator('.pm-table tbody tr').first()).toContainText('68.000 ₫'); const p = reads.at(-1)!.searchParams;
  expect(Object.fromEntries(p)).toMatchObject({ view: 'management', recipient: 'Synthetic contact', from: '2026-11-02', until: '2026-11-03', status: 'AT_STATION', service: 'ROAD_STANDARD', origin: 'SIM-A1', destination: 'SIM-A2', obligation: 'UNPAID', sort: 'FEE_DESC' }); expect(p.has('page')).toBe(false);
  await page.getByRole('button', { name: 'Xóa bộ lọc', exact: true }).click(); await expect(page.locator('.pm-results-heading')).toContainText('45 vận đơn');
});
test('nine categories distinguish Draft and unavailable lifecycle from an empty successful result', async ({ page, context }) => {
  const reads = await fixture(context); await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  const before = reads.length; await expect(page.getByRole('navigation', { name: 'Nhóm vận đơn' }).getByRole('link')).toHaveCount(9);
  await page.getByRole('navigation', { name: 'Nhóm vận đơn' }).getByRole('link', { name: 'Đơn nháp', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đơn nháp', exact: true })).toBeVisible(); await expect(page.getByRole('heading', { name: 'Bạn chưa có bản nháp', exact: true })).toBeVisible();
  await page.getByRole('navigation', { name: 'Nhóm vận đơn' }).getByRole('link', { name: /Đang vận chuyển/ }).click(); await expect(page.locator('.pm-dependency')).toContainText('Carrier Transit'); await expect(page.locator('.pm-kpis')).toHaveCount(0); expect(reads.length).toBe(before);
  await page.goto('/personal/action-required'); await expect(page.getByRole('heading', { name: 'Chưa có nguồn yêu cầu xử lý' })).toBeVisible(); await expect(page.getByRole('button', { name: /Sửa COD|Duyệt|Đồng ý/ })).toHaveCount(0);
  const summary = page.getByRole('button', { name: 'Quản lý', exact: true }); await summary.focus(); await page.keyboard.press('Enter'); await expect(page.getByRole('link', { name: 'Cước phí & Thống kê', exact: true })).toBeHidden(); await page.keyboard.press('Enter'); await expect(page.getByRole('link', { name: 'Cước phí & Thống kê', exact: true })).toBeVisible();
});
test('fees reuse Analytics and documents deep link opens the existing Shipment document tab', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/billing'); await expect(page.getByRole('heading', { name: 'Cước phí & Thống kê', exact: true })).toBeVisible(); await expect(page.locator('.an-kpi')).toHaveCount(3); await expect(page.locator('.an-kpi[data-metric=UNPAID_OBLIGATIONS]')).toContainText('925.000'); await expect(page.locator('.an-fee-card')).toContainText('UNPAID'); await expect(page.getByRole('button', { name: /Rút tiền|Thanh toán|Ví/ })).toHaveCount(0);
  await page.goto('/personal/shipments'); await page.locator('.pm-table tbody tr').first().getByRole('link', { name: 'Nhãn & chứng từ', exact: true }).click(); await expect(page.getByRole('tab', { name: 'Chứng từ', exact: true })).toHaveAttribute('aria-selected', 'true');
});

for (const width of [1440, 390]) test(`navigation has one account entry and stable Management disclosure at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 }); await page.goto('/personal/overview');
  await expect(page.locator('.an-kpi')).toHaveCount(4);
  const sidebar = page.getByRole('complementary', { name: 'Thanh điều hướng Personal' });
  if (width < 760) await page.getByRole('button', { name: 'Mở menu cá nhân' }).click();
  await expect(sidebar.locator('.pw-account-links')).toHaveCount(0); await expect(sidebar.getByRole('link', { name: /Hồ sơ|Bảo mật|Merchant/ })).toHaveCount(0);
  await expect(page.locator('.pw-header-context')).toHaveCount(0);
  const toggle = sidebar.getByRole('button', { name: 'Quản lý', exact: true }), items = sidebar.locator('#personal-management-links');
  const font = (node: Element) => { const css = getComputedStyle(node); return [css.fontFamily, css.fontSize, css.fontWeight, css.lineHeight, css.letterSpacing]; };
  expect(await toggle.evaluate(font)).toEqual(await sidebar.getByRole('link', { name: 'Sổ địa chỉ', exact: true }).evaluate(font));
  await expect(toggle).toHaveAttribute('aria-expanded', 'false'); await expect(items).toBeHidden();
  for (let i = 0; i < 4; i++) { await toggle.click(); await expect(toggle).toHaveAttribute('aria-expanded', i % 2 === 0 ? 'true' : 'false'); }
  await toggle.focus(); await page.keyboard.press('Enter'); await expect(items).toBeVisible();
  await page.keyboard.press('Escape'); await expect(items).toBeHidden(); await expect(toggle).toBeFocused();
  await page.keyboard.press('Space'); await expect(items).toBeVisible();
  await items.getByRole('link', { name: 'Quản lý vận đơn', exact: true }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  if (width < 760) await page.getByRole('button', { name: 'Mở menu cá nhân' }).click();
  await toggle.click(); await expect(items).toBeHidden();
  await sidebar.getByRole('link', { name: 'Tổng quan', exact: true }).click(); await expect(page.locator('.an-kpi')).toHaveCount(4);
  if (width < 760) await page.getByRole('button', { name: 'Mở menu cá nhân' }).click();
  await expect(toggle).toHaveAttribute('aria-expanded', 'false'); await expect(items).toBeHidden();
  await toggle.click(); await expect(items).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: `../../../.demo/navigation-polish-${width}.png`, fullPage: true, animations: 'disabled' });
  if (width < 760) await page.getByRole('button', { name: 'Đóng menu cá nhân' }).click();
  await page.getByLabel('Menu tài khoản Synthetic Customer', { exact: true }).click();
  const account = page.locator('.pw-user-dropdown');
  for (const [label, path] of [['Hồ sơ cá nhân', 'profile'], ['Bảo mật tài khoản', 'security'], ['Hồ sơ Merchant', 'applications']]) await expect(account.getByRole('link', { name: label, exact: true })).toHaveAttribute('href', '/personal/' + path);
});
test('failed or malformed management reads do not become zero KPIs or an empty success', async ({ page, context }) => {
  await fixture(context); let fail = true; await context.route('**/api/v1/customer/shipments?*', route => route.fulfill({ status: fail ? 503 : 200, contentType: 'application/json', body: JSON.stringify(fail ? { code: 'UNAVAILABLE' } : managementFixture(new URL(route.request().url()).searchParams, 45)) }));
  await page.goto('/personal/shipments'); await expect(page.getByRole('alert')).toContainText('chưa được xác nhận'); await expect(page.locator('.pm-kpis')).toHaveCount(0); fail = false; await page.getByRole('button', { name: 'Tải lại vận đơn' }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  await page.goto('/personal/shipments?ownerId=foreign'); await expect(page.getByRole('alert')).toContainText('Bộ lọc không hợp lệ'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(0);
});
for (const width of [1440, 768, 390]) test(`management responsive table and active navigation at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 }); await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  const region = page.getByRole('region', { name: 'Bảng quản lý vận đơn', exact: true }); await region.focus(); await page.keyboard.press('ArrowRight'); await expect(region).toBeVisible();
  await page.getByRole('heading', { name: 'Quản lý vận đơn', exact: true }).focus(); await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `../../../.demo/management-${width}.png`, fullPage: true, animations: 'disabled' });
});
