import { expect, test } from '@playwright/test';
import type { BrowserContext, Route } from '@playwright/test';
import { businessContextsFixture, businessReportFixture, organizationA, organizationB } from './support/business-fixture';
const account = '10000000-0000-4000-8000-000000000001';
async function fixture(context: BrowserContext, special?: (route: Route, path: string) => Promise<boolean>) {
  const calls: { path: string; method: string; query: string }[] = [];
  await context.route('**/api/v1/**', async route => {
    const r = route.request(), u = new URL(r.url()), path = u.pathname; calls.push({ path, method: r.method(), query: u.search });
    if (special && await special(route, path)) return;
    const reply = (value: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(value) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-business-csrf' });
    if (path.endsWith('/refresh')) return reply({ accessToken: 'synthetic-business-access', accountId: account, expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/logout')) return route.fulfill({ status: 204 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: businessContextsFixture });
    if (path.endsWith('/dashboard')) { const item = businessContextsFixture.find(c => path.includes(c.organizationId)); return reply(item ? businessReportFixture(item, u.searchParams) : { code: 'BUSINESS_CONTEXT_UNAVAILABLE' }, item ? 200 : 404); }
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  }); return calls;
}
test('Business opens current Backend context with truthful source states and disabled workflows', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/merchant'); await expect(page).toHaveURL(new RegExp(organizationA + '/overview$'));
  await expect(page.getByRole('heading', { name: 'Synthetic Store A', exact: true })).toBeVisible(); await expect(page.locator('.bw-kpi strong')).toHaveText(['—', '—', '—', '—']);
  await expect(page.getByRole('button', { name: 'Tạo vận đơn', exact: true })).toBeDisabled(); await expect(page.locator('.bw-dashboard canvas,.bw-dashboard .analytics-sparkline')).toHaveCount(0);
  await expect(page.getByRole('navigation', { name: 'Điều hướng Business' }).getByRole('link')).toHaveCount(1);
  expect(calls.filter(c => !c.path.includes('customer-auth')).every(c => c.method === 'GET' && !c.path.includes('personal-analytics'))).toBe(true);
  await page.screenshot({ path: '../../../.demo/business-dashboard-review/business-ui-desktop.png', fullPage: true, animations: 'disabled' });
});
test('real contexts switch organization and omit finance/team for a limited member', async ({ page, context }) => {
  await fixture(context); await page.goto('/merchant/' + organizationA + '/overview'); await expect(page.getByRole('heading', { name: 'Chi phí vận chuyển' })).toBeVisible();
  await page.getByRole('button', { name: 'Synthetic Store A', exact: true }).click(); await page.getByRole('link', { name: /Synthetic Store B/ }).click();
  await expect(page.getByRole('heading', { name: 'Synthetic Store B', exact: true })).toBeVisible(); await expect(page.locator('.bw-kpi')).toHaveCount(4); await expect(page.getByRole('heading', { name: 'Chi phí vận chuyển' })).toHaveCount(0); await expect(page.getByRole('heading', { name: 'Hoạt động nhân viên' })).toHaveCount(0); await expect(page.locator('.bw-metric-catalog')).not.toContainText('COD');
  await page.getByRole('button', { name: 'Synthetic Store B', exact: true }).click(); await page.getByRole('link', { name: /ArgoPost Personal/ }).click(); await expect(page).toHaveURL(/\/personal$/);
});
test('date and interval controls make one exact scoped query; pending range hides previous data', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/merchant/' + organizationA + '/overview'); await expect(page.locator('.bw-kpi')).toHaveCount(4);
  await page.getByRole('button', { name: '7 ngày', exact: true }).click(); await expect(page.locator('.bw-kpi')).toHaveCount(4); await page.getByLabel('Thống kê theo').selectOption('WEEK');
  await expect.poll(() => calls.filter(c => c.path.endsWith('/dashboard')).at(-1)?.query).toContain('interval=WEEK');
  await page.getByRole('button', { name: 'Tùy chọn', exact: true }).click(); await page.getByLabel('Từ ngày', { exact: true }).fill('2026-01-01'); await page.getByRole('button', { name: 'Áp dụng', exact: true }).click();
  await expect.poll(() => calls.filter(c => c.path.endsWith('/dashboard')).at(-1)?.query).toContain('from=2026-01-01');
});
test('foreign context and no membership never produce a Business dashboard', async ({ page, context }) => {
  await fixture(context, async (route, path) => { if (!path.endsWith('/business-contexts')) return false; await route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [] }) }); return true; });
  await page.goto('/merchant'); await expect(page.getByRole('heading', { name: 'Chưa có quyền truy cập doanh nghiệp' })).toBeVisible(); await expect(page.locator('.bw-dashboard')).toHaveCount(0);
  await page.goto('/merchant/' + organizationB + '/overview'); await expect(page.getByRole('heading', { name: 'Không gian làm việc không khả dụng' })).toBeVisible();
});
test('revoked or malformed report clears data and does not reuse Personal totals', async ({ page, context }) => {
  let count = 0; await fixture(context, async (route, path) => { if (!path.endsWith('/dashboard') || ++count === 1) return false; await route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 'BUSINESS_CONTEXT_UNAVAILABLE' }) }); return true; });
  await page.goto('/merchant/' + organizationA + '/overview'); await expect(page.locator('.bw-kpi')).toHaveCount(4); await page.getByRole('button', { name: 'Làm mới', exact: true }).click();
  await expect(page.getByRole('alert')).toBeVisible(); await expect(page.locator('.bw-kpi')).toHaveCount(0); await expect(page.locator('.bw-dashboard')).not.toContainText('63.000');
});
for (const width of [360, 768, 1440]) test(`Business layout and workspace disclosure are usable at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 1000 }); await page.goto('/merchant/' + organizationA + '/overview'); await expect(page.locator('.bw-kpi')).toHaveCount(4);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Synthetic Store A', exact: true }).click(); const choice = page.getByRole('link', { name: /Synthetic Store B/ }); await expect(choice).toBeVisible(); const box = await choice.boundingBox(); expect(box!.x).toBeGreaterThanOrEqual(0); expect(box!.x + box!.width).toBeLessThanOrEqual(width);
  await page.keyboard.press('Escape'); await expect(choice).toHaveCount(0);
  if (width === 360) await page.screenshot({ path: '../../../.demo/business-dashboard-review/business-ui-mobile.png', fullPage: true, animations: 'disabled' });
});
test('Business uses an accessible orange primary while returning to Personal restores navy', async ({ page, context }) => {
  await fixture(context); await page.goto('/merchant/' + organizationA + '/overview'); await expect(page.locator('.bw-kpi')).toHaveCount(4);
  const active = page.getByRole('navigation', { name: 'Điều hướng Business' }).getByRole('link', { name: 'Tổng quan', exact: true });
  await expect(active).toHaveCSS('background-color', 'rgb(255, 122, 0)'); await expect(page.getByRole('button', { name: '30 ngày', exact: true })).toHaveCSS('background-color', 'rgb(255, 122, 0)');
  const contrast = await active.evaluate(element => {
    const style = getComputedStyle(element), luminance = (text: string) => { const rgb = text.match(/[\d.]+/g)!.slice(0, 3).map(Number).map(n => { const s = n / 255; return s <= .04045 ? s / 12.92 : ((s + .055) / 1.055) ** 2.4; }); return rgb[0] * .2126 + rgb[1] * .7152 + rgb[2] * .0722; };
    const a = luminance(style.color), b = luminance(style.backgroundColor); return (Math.max(a, b) + .05) / (Math.min(a, b) + .05);
  }); expect(contrast).toBeGreaterThanOrEqual(4.5);
  await page.screenshot({ path: '../../../.demo/business-shipping-review/business-orange-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 360, height: 1000 }); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/business-shipping-review/business-orange-mobile.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Synthetic Store A', exact: true }).click(); await page.getByRole('link', { name: /ArgoPost Personal/ }).click();
  await page.setViewportSize({ width: 1280, height: 900 }); await expect(page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Tổng quan', exact: true })).toHaveCSS('background-color', 'rgb(11, 45, 91)');
  await expect(page.locator('.cp-business-portal')).toHaveCount(0);
});
