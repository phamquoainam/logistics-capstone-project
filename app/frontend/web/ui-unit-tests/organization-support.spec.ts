import { test, expect } from '@playwright/test';
import { businessToolsFixture, toolsItem } from './support/business-tools-fixture';
import { organizationA, organizationB } from './support/business-fixture';
const base = '/merchant/' + organizationA;

test('Organization support is a root menu and its guides link to the admitted business only', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/support');
  const nav = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true });
  await expect(nav.getByRole('link', { name: 'Hỗ trợ & Khiếu nại', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('heading', { name: 'Hỗ trợ & Khiếu nại', exact: true })).toBeVisible(); await page.getByLabel('Tìm trong hướng dẫn', { exact: true }).fill('báo giá');
  await expect(page.locator('.os-guides details')).toHaveCount(2); await page.getByText('Báo giá hết hạn hoặc bản nháp vừa thay đổi?', { exact: true }).click();
  await expect(page.getByRole('link', { name: 'Kiểm tra báo giá', exact: true })).toHaveAttribute('href', base + '/lookup?view=quote');
  expect(api.calls.every(c => c.method === 'GET' && c.path.startsWith('/api/v1/merchant-organizations/' + organizationA))).toBe(true);
  expect(api.calls.filter(c => /support-tickets|\/shipments\//.test(c.path))).toHaveLength(0); await expect(page.locator('.os-support a[href^="/personal"]')).toHaveCount(0);
});
test('Shipment context is read from the exact current Organization API without creating a support resource', async ({ page, context }) => {
  const api = await businessToolsFixture(context), item = toolsItem(), id = item.record.shipment.id;
  await context.route('**/merchant-organizations/' + organizationA + '/shipments/' + id, route => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify(item.record) }));
  await page.goto(base + '/support?view=tickets&shipment=' + id);
  await expect(page.getByRole('region', { name: 'Vận đơn liên quan' })).toContainText(item.record.shipment.code);
  await expect(page.locator('.os-context')).not.toContainText('+849'); await expect(page.locator('.os-context')).not.toContainText('Synthetic contact');
  const href = await page.getByRole('link', { name: 'Xem hành trình', exact: true }).getAttribute('href'); expect(new URL(href!, 'https://local.invalid').searchParams.get('returnTo')).toBe(base + '/support?view=tickets&shipment=' + id);
  await expect(page.getByRole('button', { name: /Gửi|Tạo Ticket|Bồi thường|Thu tiền/ })).toHaveCount(0); expect(api.calls.every(c => c.method === 'GET')).toBe(true);
});
test('foreign malformed or duplicate Shipment selectors never fall back to Personal data', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/support?shipment=bad'); await expect(page.getByRole('alert')).toContainText('Liên kết hỗ trợ không hợp lệ'); expect(api.calls.filter(c => /support-tickets|\/shipments\//.test(c.path))).toHaveLength(0);
  await page.goto(base + '/support?shipment=' + organizationB); await expect(page.getByRole('alert')).toContainText('Không có quyền'); await expect(page.locator('.os-context-facts')).toHaveCount(0);
  expect(api.calls.every(c => c.path.startsWith('/api/v1/merchant-organizations/' + organizationA) && c.method === 'GET')).toBe(true);
});
test('Claim and change guidance does not become a Carrier or financial command', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/support?view=claims'); await expect(page.locator('.os-availability')).toContainText('người phê duyệt độc lập'); await expect(page.getByRole('button', { name: /Gửi|Duyệt|Thanh toán|Tải lên/ })).toHaveCount(0);
  await page.getByRole('link', { name: 'Yêu cầu thay đổi', exact: true }).click(); await expect(page.locator('.os-availability')).toContainText('chưa mở gửi các lệnh thay đổi');
  expect(api.calls.every(c => c.method === 'GET' && c.path.startsWith('/api/v1/merchant-organizations/' + organizationA))).toBe(true);
  expect(api.calls.filter(c => /support-tickets|\/shipments\//.test(c.path))).toHaveLength(0);
});
for (const width of [390, 1440]) test(`support hub is readable at ${width}px without simulated Ticket counts`, async ({ page, context }) => {
  await businessToolsFixture(context); await page.setViewportSize({ width, height: 1000 }); await page.goto(base + '/support'); await expect(page.locator('.os-guides')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await expect(page.locator('.os-support')).not.toContainText('0 Ticket');
  await page.screenshot({ path: '../../../.demo/organization-support/hub-' + width + '.png', fullPage: true, animations: 'disabled' });
});
