import { test, expect } from '@playwright/test';
import { businessToolsFixture, toolsItem } from './support/business-tools-fixture';
import { organizationA } from './support/business-fixture';
const base = '/merchant/' + organizationA;
test('root Business tools use exact organization codes and do not expose contacts or Personal sources', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/lookup');
  const nav = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true });
  await expect(nav.getByRole('link', { name: 'Tra cứu & Báo giá', exact: true })).toHaveAttribute('aria-current', 'page');
  const s = toolsItem().record.shipment; await page.getByLabel('Danh sách mã', { exact: true }).fill(s.code + '\n' + s.code); await page.getByRole('button', { name: 'Tra cứu', exact: true }).click();
  await expect(page.locator('.bst-match')).toHaveCount(1); await expect(page.locator('.bst-match')).not.toContainText('+849');
  await page.screenshot({ path: '../../../.demo/business-shipping-tools/tracking-desktop.png', fullPage: true, animations: 'disabled' });
  await expect(page.getByRole('link', { name: 'Xem hành trình', exact: true })).toHaveAttribute('href', /tab=tracking/);
  const csv = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất kết quả CSV' }).click(); expect((await csv).suggestedFilename()).toContain('argopost-tracking-');
  const exported = api.calls.find(c => c.path.endsWith('/shipment-exports')); expect(exported?.query.get('includePii')).toBe('false'); expect(exported?.query.get('ids')).toBe(s.id);
  await page.getByLabel('Tra cứu theo', { exact: true }).selectOption('PARCEL'); await page.getByLabel('Danh sách mã').fill('AP-P-' + s.parcels[0].id.replaceAll('-', '')); await page.getByRole('button', { name: 'Tra cứu', exact: true }).click(); await expect(page.locator('.bst-match')).toHaveCount(1);
  expect(api.calls.every(c => c.path.startsWith('/api/v1/merchant-organizations/' + organizationA))).toBe(true);
});
test('revoked lookup is unknown and blocks export rather than reusing prior results', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/lookup'); await page.getByLabel('Danh sách mã').fill(toolsItem().record.shipment.code); await page.getByRole('button', { name: 'Tra cứu', exact: true }).click(); await expect(page.locator('.bst-match')).toHaveCount(1);
  api.state.denied = true; await page.getByRole('button', { name: 'Tra cứu', exact: true }).click(); await expect(page.locator('.bst-match')).toHaveCount(0); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByRole('button', { name: 'Xuất kết quả CSV' })).toBeDisabled();
});
test('batch prices saved versions and resolves a lost quote before sending the next one', async ({ page, context }) => {
  const api = await businessToolsFixture(context); api.state.unknownNext = true; await page.goto(base + '/lookup?view=quote');
  for (const n of [0, 1]) await page.locator('.bst-drafts label').filter({ hasText: 'ORDER-' + n }).getByRole('checkbox').check();
  await page.getByLabel(/^Tất cả kiện là hàng thông thường/).check(); await page.getByRole('button', { name: 'Lấy báo giá đã chọn', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Kiểm tra kết quả báo giá' })).toBeVisible(); expect(api.quotes.size).toBe(1); await expect(page.getByRole('button', { name: 'Lấy báo giá đã chọn', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả báo giá' }).click(); await expect(page.locator('.bst-quote-row').first()).toContainText('25.000');
  await page.getByRole('button', { name: 'Lấy báo giá đã chọn', exact: true }).click(); await expect.poll(() => api.quotes.size).toBe(2); await expect(page.getByRole('link', { name: 'Kiểm tra & Xác nhận Booking', exact: true })).toHaveCount(2);
  await expect(page.locator('.bst-quotation')).not.toContainText('15:01');
  await expect(page.locator('.bst-drafts label').first()).toHaveCSS('flex-direction', 'row');
  expect(api.calls.filter(c => c.path.endsWith('/quotes') && c.method === 'POST')).toHaveLength(2); expect(api.calls.some(c => c.path.endsWith('/bookings'))).toBe(false);
  await page.screenshot({ path: '../../../.demo/business-shipping-tools/quotes-desktop.png', fullPage: true, animations: 'disabled' });
  const csv = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất báo giá CSV' }).click(); expect((await csv).suggestedFilename()).toBe('argopost-business-quotes.csv');
  expect(api.calls.filter(c => c.path.includes('/shipping-quotes/')).length).toBeGreaterThanOrEqual(3);
  api.state.denied = true; await page.getByRole('button', { name: 'Xuất báo giá CSV' }).click(); await expect(page.getByRole('alert')).toContainText('Chưa tạo file');
});
test('a suspended Merchant contract has no Personal tariff fallback', async ({ page, context }) => {
  const api = await businessToolsFixture(context); api.state.quoteEnabled = false; await page.goto(base + '/lookup?view=quote'); await expect(page.locator('.bst-contract')).toContainText('Chưa có khả năng báo giá'); await expect(page.getByRole('button', { name: 'Lấy báo giá đã chọn' })).toBeDisabled(); expect(api.calls.some(c => c.path.includes('/customer/'))).toBe(false);
});
test('labels filter actual cohorts, prepare private PDF and discard denied refresh data', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/labels'); await expect(page.locator('.om-table tbody tr')).toHaveCount(2);
  await page.getByLabel('Tìm vận đơn', { exact: true }).fill(toolsItem().record.shipment.code); await page.getByRole('button', { name: 'Áp dụng bộ lọc' }).click(); await expect(page.locator('.om-table tbody tr')).toHaveCount(1);
  await page.getByRole('button', { name: 'Xem / In PDF' }).click(); await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toBeVisible(); await expect(page.locator('.bst-preview')).toContainText('trang tiếp nối');
  api.state.denied = true; await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('quyền đã thay đổi'); await expect(page.locator('.bst-preview')).toHaveCount(0); await expect(page.locator('.om-table tbody tr')).toHaveCount(0);
});
test('selected labels reuse the whole authorized PDF bundle and deny output on a failed read', async ({ page, context }) => {
  const api = await businessToolsFixture(context); await page.goto(base + '/labels'); await page.getByRole('checkbox', { name: 'Chọn nhãn trên trang', exact: true }).check();
  const zip = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click(); expect((await zip).suggestedFilename()).toBe('argopost-business-labels.zip');
  await expect(page.getByRole('status')).toContainText('Đã tải nhãn của 2 vận đơn');
  api.state.labelDenied = true; await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa tạo ZIP');
  expect(api.calls.filter(c => c.path.endsWith('/labels')).every(c => c.method === 'GET')).toBe(true);
});
test('mobile lookup quote and label controls contain long source references and keep table scrolling local', async ({ page, context }) => {
  const api = await businessToolsFixture(context); api.drafts[0].input.reference = 'LONGREFERENCE'.repeat(6);
  await page.setViewportSize({ width: 390, height: 900 });
  for (const route of ['/lookup', '/lookup?view=quote', '/labels']) {
    await page.goto(base + route); await expect(page.locator('.bst-page h1')).toBeVisible();
    if (route.includes('quote')) { await expect(page.locator('.bst-drafts label')).toHaveCount(2); await expect(page.locator('.bst-drafts label').first()).toContainText('LONGREFERENCE'); }
    if (route === '/labels') await expect(page.locator('.om-table tbody tr')).toHaveCount(2);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
});
for (const width of [390, 1440]) test(`Business calculator and tools stay usable at ${width}px without fake Pickup/receipt`, async ({ page, context }) => {
  await businessToolsFixture(context); await page.setViewportSize({ width, height: 1000 }); await page.goto(base + '/labels?view=calculator'); await expect(page.getByRole('heading', { name: 'Kiểm tra trọng lượng & Kích thước' })).toBeVisible();
  await page.getByLabel('Trọng lượng (g)', { exact: true }).fill('501'); await expect(page.locator('.bst-catalog-grid')).toContainText('1.000 g'); await page.getByLabel('Dài (mm)', { exact: true }).fill('1001'); await expect(page.getByRole('alert')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/business-shipping-tools/calculator-' + width + '.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Chuẩn bị & Bàn giao', exact: true }).click(); await expect(page.locator('.bst-catalog-grid')).toContainText('không phải biên nhận Carrier');
});
