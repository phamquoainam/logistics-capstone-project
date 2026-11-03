import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { analyticsFixture } from './support/analytics-fixture';
async function fixture(context: BrowserContext, count = 25) {
  await context.route('**/api/v1/**', async route => { const path = new URL(route.request().url()), reply = (v: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(v) });
    if (path.pathname.endsWith('/csrf')) return reply({ challenge: 'synthetic-analytics-csrf' });
    if (path.pathname.endsWith('/refresh')) return reply({ accessToken: 'synthetic-analytics-access', accountId: '10000000-0000-4000-8000-000000000001', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.pathname.endsWith('/me')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.pathname.endsWith('/personal-analytics')) return reply(analyticsFixture(path.searchParams, count));
    return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'ISOLATED_UI_UNSUPPORTED' }) });
  });
}
test('overview status drill keeps dates, reconciles source count and supports Back/Forward', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/overview?from=2026-11-01&until=2026-11-04');
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('25');
  await page.locator('.an-status-legend').getByRole('link', { name: /Đã nhận tại Station/ }).click();
  await expect(page).toHaveURL(/\/personal\/overview\/status\?.*status=AT_STATION/); expect(new URL(page.url()).searchParams.get('from')).toBe('2026-11-01'); expect(new URL(page.url()).searchParams.get('until')).toBe('2026-11-04');
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('12'); await expect(page.locator('.an-source tbody tr')).toHaveCount(12); await expect(page.getByRole('navigation', { name: 'Đường dẫn phân tích' })).toContainText('Trạng thái vận đơn');
  await page.goBack(); await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('25'); await page.goForward(); await expect(page.locator('.an-source tbody tr')).toHaveCount(12);
});
test('analytics source table sorts searches paginates and opens existing Shipment detail', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/overview/shipments?from=2026-11-01&until=2026-11-04'); await expect(page.locator('.an-source tbody tr')).toHaveCount(20);
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page).toHaveURL(/page=1/); await expect(page.locator('.an-source tbody tr')).toHaveCount(5); await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('25');
  await page.getByLabel('Sắp xếp', { exact: true }).selectOption('FEE_DESC'); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.locator('.an-source tbody tr')).toHaveCount(20); await expect(page.locator('.an-source tbody tr').first()).toContainText('49.000');
  await page.getByLabel('Tìm mã vận đơn', { exact: true }).fill('AP-SIM-' + '1'.padStart(32, '0')); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.locator('.an-source tbody tr')).toHaveCount(1); await page.locator('.an-source tbody a').click(); await expect(page).toHaveURL(/\/personal\/shipments\/20000000-/);
});
test('chart context is available on keyboard focus and point drill retains the global range', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/overview?from=2026-11-01&until=2026-11-04'); const bar = page.locator('.an-chart-bar').first(); await bar.focus(); await expect(page.locator('.an-chart-context')).toContainText(/01[-/]11/); await page.keyboard.press('Enter'); await expect(page).toHaveURL(/day=2026-11-01/); await expect(page.locator('.an-source tbody tr')).toHaveCount(9); expect(new URL(page.url()).searchParams.get('until')).toBe('2026-11-04');
});
for (const width of [1440, 768, 390]) test(`analytics responsive layout and tap metric explanation at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 }); await page.goto('/personal/overview?from=2026-11-01&until=2026-11-04'); await expect(page.locator('.an-kpi')).toHaveCount(6); await expect(page.locator('.an-source tbody tr')).toHaveCount(20);
  if (width > 760) {
    const logo = page.locator('.pw-sidebar-brand img'); await expect(logo).toHaveAttribute('src', '/media/argopost-logo-primary.png');
    await expect.poll(() => logo.evaluate(img => (img as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
  }
  const toolbar = await page.locator('.pw-header').boundingBox(), heading = await page.getByRole('heading', { name: 'Tổng quan gửi hàng', exact: true }).boundingBox();
  expect(toolbar!.y + toolbar!.height).toBeLessThan(heading!.y);
  await expect(page.getByRole('textbox', { name: 'Tìm kiếm vận đơn chưa khả dụng' })).toHaveCount(0);
  await page.getByLabel('Giải thích Nghĩa vụ chưa thanh toán', { exact: true }).click(); await expect(page.locator('.an-info[open]')).toContainText('UNPAID'); const bounds = await page.locator('.an-info[open] p').boundingBox(); expect(bounds!.x).toBeGreaterThanOrEqual(0); expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width);
  await page.getByLabel('Giải thích Nghĩa vụ chưa thanh toán', { exact: true }).click(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.evaluate(() => scrollTo(0, 0)); await page.screenshot({ path: `../../../.demo/analytics-overview-${width}.png`, fullPage: true, animations: 'disabled' });
});
test('compact filters retain advanced selections and reset the same analytics cohort', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/overview?from=2026-11-01&until=2026-11-04');
  await expect(page.getByLabel('Trạng thái', { exact: true })).toBeHidden();
  await page.getByText('Bộ lọc khác', { exact: false }).click();
  await page.getByLabel('Trạng thái', { exact: true }).selectOption('AT_STATION');
  await page.getByRole('button', { name: 'Áp dụng', exact: true }).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(12);
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('12');
  await expect(page.getByLabel('Trạng thái', { exact: true })).toHaveValue('AT_STATION');
  await page.getByRole('button', { name: 'Đặt lại', exact: true }).click();
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('25');
  expect(new URL(page.url()).searchParams.has('status')).toBe(false);
});
test('unavailable and malformed analytics never fabricate zero cards; retry can recover', async ({ page, context }) => {
  await fixture(context, 0); let reads = 0; await context.route('**/api/v1/customer/personal-analytics?*', async route => { reads++; if (reads === 1) return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'ANALYTICS_UNAVAILABLE' }) }); return route.fulfill({ contentType: 'application/json', body: JSON.stringify(analyticsFixture(new URL(route.request().url()).searchParams)) }); });
  await page.goto('/personal'); await expect(page.getByRole('alert')).toContainText('Số liệu chưa được xác nhận'); await expect(page.locator('.an-kpi')).toHaveCount(0); await page.getByRole('button', { name: 'Thử lại Analytics' }).click(); await expect(page.locator('.an-empty')).toContainText('Chưa có vận đơn'); await expect(page.locator('.an-kpi')).toHaveCount(6); await expect(page.locator('.an-trend').first()).toContainText('Chưa có đối chiếu hợp lệ');
  await context.route('**/api/v1/customer/personal-analytics?*', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ ...analyticsFixture(), context: 'BUSINESS' }) })); await page.reload(); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.locator('.an-kpi')).toHaveCount(0);
});
test('reapplying filters and resetting the default range cannot leave Analytics loading forever', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/overview'); await expect(page.locator('.an-kpi')).toHaveCount(6);
  for (let i = 0; i < 2; i++) { await page.getByRole('button', { name: 'Áp dụng', exact: true }).click(); await expect(page.locator('.an-kpi')).toHaveCount(6); }
  for (let i = 0; i < 2; i++) { await page.getByRole('button', { name: 'Đặt lại', exact: true }).click(); await expect(page.locator('.an-kpi')).toHaveCount(6); }
});
test('invalid date deep links produce a recoverable validation error instead of crashing the page', async ({ page, context }) => {
  await fixture(context); const errors: string[] = []; page.on('pageerror', error => errors.push(error.name));
  await context.route('**/api/v1/customer/personal-analytics?*', route => route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 'VALIDATION_FAILED' }) }));
  await page.goto('/personal/overview?from=invalid&until=invalid'); await expect(page.getByRole('alert')).toContainText('Bộ lọc không hợp lệ'); await expect(page.locator('.an-kpi')).toHaveCount(0); expect(errors).toEqual([]);
});
