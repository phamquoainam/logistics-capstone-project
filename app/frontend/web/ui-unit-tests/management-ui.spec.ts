import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import type { PilotShipmentView } from '../src/customer/client';
import type { BrowserContext } from '@playwright/test';
import { managementFixture } from './support/management-fixture';
import { analyticsFixture, shippingReportsFixture } from './support/analytics-fixture';

function handling(record: PilotShipmentView, accepted = false) {
  return { shipmentId: record.shipment.id, state: accepted ? 'AT_STATION' : 'WAITING_FOR_HANDOVER', version: accepted ? 1 : 0, parcels: record.shipment.parcels.map(p => ({ parcelId: p.id, parcelNumber: p.parcelNumber, barcode: 'AP-P-' + p.id.replaceAll('-', ''), state: accepted ? 'AT_STATION' : 'NOT_ACCEPTED' })), events: accepted ? [{ id: '60000000-0000-4000-8000-000000000001', type: 'CARRIER_ACCEPTED_AT_STATION', stationCode: 'SIM-DROP-A1', occurredAt: record.shipment.createdAt, provenance: 'SIMULATION' }] : [], labelAvailable: true, simulation: true };
}
async function fixture(context: BrowserContext, count = 45) {
  const reads: URL[] = [];
  const records = Array.from({ length: Math.ceil(count / 20) }, (_, page) => managementFixture(new URLSearchParams({ page: String(page) }), count).items).flat();
  await context.route('**/api/v1/**', async route => {
    const u = new URL(route.request().url()), path = u.pathname;
    const reply = (value: unknown, headers: Record<string, string> = {}) => route.fulfill({ contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-management-challenge' });
    if (path.endsWith('/refresh')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/shipments')) { reads.push(u); if (u.searchParams.has('sort') && !['NEWEST', 'OLDEST', 'FEE_DESC', 'FEE_ASC'].includes(u.searchParams.get('sort')!)) return route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 'VALIDATION_FAILED' }) }); return reply(managementFixture(u.searchParams, count)); }
    if (path.endsWith('/personal-shipping-reports')) return reply(shippingReportsFixture(u.searchParams, 25));
    if (path.endsWith('/personal-analytics')) return reply(analyticsFixture(u.searchParams, 25));
    if (path.endsWith('/shipment-drafts')) return reply({ items: [], page: 0, size: 20, hasNext: false });
    const handled = records.find(i => path.endsWith('/shipments/' + i.record.shipment.id + '/handling'));
    if (handled) return reply(handling(handled.record, handled.status === 'AT_STATION'));
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
  const summary = page.getByRole('button', { name: 'Quản lý', exact: true }); await summary.focus(); await page.keyboard.press('Enter'); await expect(page.getByRole('link', { name: 'Cước & Thanh toán', exact: true })).toBeHidden(); await page.keyboard.press('Enter'); await expect(page.getByRole('link', { name: 'Cước & Thanh toán', exact: true })).toBeVisible();
});
test('fees reuse Analytics and documents deep link opens the existing Shipment document tab', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/billing'); await expect(page.getByRole('heading', { name: 'Cước & Thanh toán', exact: true })).toBeVisible(); await expect(page.locator('.an-kpi')).toHaveCount(3); await expect(page.locator('.an-kpi[data-metric=UNPAID_OBLIGATIONS]')).toContainText('925.000'); await expect(page.locator('.sf-list')).toContainText('UNPAID'); await expect(page.getByRole('button', { name: /Rút tiền|Thanh toán|Ví/ })).toHaveCount(0);
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
  expect(await toggle.evaluate(font)).toEqual(await sidebar.getByRole('link', { name: 'Tạo vận đơn', exact: true }).evaluate(font));
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
  await page.getByText('Chọn cột', { exact: true }).click();
  const menu = page.locator('.pm-column-picker>div'), box = await menu.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0); expect(box!.x + box!.width).toBeLessThanOrEqual(width);
  await menu.getByLabel('Người gửi / người nhận', { exact: true }).focus(); await page.keyboard.press('Escape'); await expect(menu).toBeHidden();
  await page.getByRole('heading', { name: 'Quản lý vận đơn', exact: true }).focus(); await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `../../../.demo/management-${width}.png`, fullPage: true, animations: 'disabled' });
});


test('WAITING category bookmark sends its real status filter rather than displaying all shipments', async ({ page, context }) => {
  const reads = await fixture(context); await page.goto('/personal/shipments?category=WAITING');
  await expect(page.locator('.pm-results-heading')).toContainText('23 vận đơn phù hợp');
  expect(reads.at(-1)!.searchParams.get('status')).toBe('WAITING_FOR_HANDOVER');
  await expect(page.locator('.pm-table tbody tr').first()).toContainText('Chờ bàn giao');
});

test('detail preserves source page filters columns and named URL tab on refresh and return', async ({ page, context }) => {
  await fixture(context); const source = '/personal/shipments?status=AT_STATION&page=1&sort=FEE_DESC'; await page.goto(source);
  await expect(page.locator('.pm-table tbody tr')).toHaveCount(2);
  await page.getByText('Chọn cột', { exact: true }).click(); await page.locator('.pm-column-picker').getByLabel('Người gửi / người nhận', { exact: true }).uncheck(); await page.keyboard.press('Escape');
  await page.locator('.pm-table tbody tr').first().getByRole('link', { name: 'Hành trình', exact: true }).click();
  await expect(page.getByRole('tab', { name: 'Hành trình', exact: true })).toHaveAttribute('aria-selected', 'true');
  await expect(page).toHaveURL(/tab=tracking/); await page.reload();
  await expect(page.getByRole('tabpanel')).toContainText('Carrier tiếp nhận tại Station');
  await page.getByRole('tab', { name: 'Hành trình', exact: true }).focus(); await page.keyboard.press('End');
  await expect(page.getByRole('tab', { name: 'Lịch sử', exact: true })).toBeFocused(); await expect(page).toHaveURL(/tab=history/);
  await page.getByRole('link', { name: 'Danh sách vận đơn', exact: true }).click();
  await expect(page).toHaveURL(source); await expect(page.locator('.pm-table tbody tr')).toHaveCount(2);
  await expect(page.locator('.pm-table thead')).not.toContainText('Người gửi / người nhận');
});

test('empty page recovery removes only page while preserving the source filters and sort', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/shipments?page=4&status=AT_STATION&sort=OLDEST');
  await expect(page.getByRole('heading', { name: 'Trang này không có vận đơn', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Về trang đầu', exact: true }).click();
  await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  const p = new URL(page.url()).searchParams; expect(p.has('page')).toBe(false); expect(p.get('status')).toBe('AT_STATION'); expect(p.get('sort')).toBe('OLDEST');
});

test('visible-column CSV exports current selected rows only and selection resets on another page', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  await page.getByText('Chọn cột', { exact: true }).click(); await page.locator('.pm-column-picker').getByLabel('Người gửi / người nhận', { exact: true }).uncheck(); await page.keyboard.press('Escape');
  await page.locator('.pm-table tbody input[type=checkbox]').first().check();
  const downloading = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV đã chọn', exact: true }).click();
  const file = await downloading; const path = await file.path(); const text = await readFile(path!, 'utf8');
  expect(text).toContain('1 vận đơn đã tải; không phải toàn bộ 45'); expect(text).toContain('LOCAL/TEST SIMULATION'); expect(text).not.toContain('+84901234567');
  await expect(page.getByRole('status')).toContainText('Đã xuất 1 vận đơn');
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  await expect(page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true })).toBeDisabled();
  await expect(page.locator('.pm-table tbody input:checked')).toHaveCount(0);
});

test('selected labels use per-shipment private GET and create an actual two-member PDF ZIP without mutations', async ({ page, context }) => {
  await fixture(context); const calls: { path: string; method: string }[] = [];
  await context.route('**/labels.pdf', route => { calls.push({ path: new URL(route.request().url()).pathname, method: route.request().method() }); expect(route.request().headers().authorization).toBe('Bearer synthetic-access'); return route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.7\nSYNTHETIC PRIVATE LABEL ' + calls.length }); });
  await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  for (const n of [0, 1]) await page.locator('.pm-table tbody input[type=checkbox]').nth(n).check();
  const downloading = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click();
  const file = await downloading; expect(file.suggestedFilename()).toBe('argopost-private-labels.zip'); await file.saveAs('../../../.demo/shipment-management-review/labels-synthetic.zip');
  await expect(page.getByRole('status')).toContainText('Đã tải nhãn của 2 vận đơn');
  expect(calls).toHaveLength(2); expect(calls.every(c => c.method === 'GET')).toBe(true); expect(new Set(calls.map(c => c.path)).size).toBe(2);
});

test('denied member aborts the bundle without a partial download or success notification', async ({ page, context }) => {
  await fixture(context); let reads = 0, downloads = 0; page.on('download', () => downloads++);
  await context.route('**/labels.pdf', route => { reads++; return route.fulfill({ status: reads === 1 ? 200 : 403, contentType: reads === 1 ? 'application/pdf' : 'application/json', body: reads === 1 ? '%PDF-1.7 synthetic' : '{"code":"DENIED"}' }); });
  await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  for (const n of [0, 1, 2]) await page.locator('.pm-table tbody input[type=checkbox]').nth(n).check();
  await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa tạo ZIP'); expect(reads).toBe(2); expect(downloads).toBe(0);
  await expect(page.locator('.pm-downloads [role=status]')).toHaveCount(0);
});

test('all eight tabs share one exact handling projection and refresh never retains stale acceptance', async ({ page, context }) => {
  await fixture(context); let reads = 0, broken = false;
  const record = managementFixture(new URLSearchParams(), 1).items[0].record;
  await context.route('**/handling', route => { reads++; return route.fulfill({ status: broken ? 503 : 200, contentType: 'application/json', body: JSON.stringify(broken ? { code: 'UNAVAILABLE' } : handling(record, true)) }); });
  await page.goto('/personal/shipments/' + record.shipment.id); await expect(page.locator('.sl-status')).toContainText('Carrier đã nhận tại Station');
  for (const tab of ['Tổng quan', 'Kiện hàng', 'Hành trình', 'Lấy hàng & Giao hàng', 'Cước & COD', 'Chứng từ', 'Yêu cầu & Hỗ trợ', 'Lịch sử']) { await page.getByRole('tab', { name: tab, exact: true }).click(); await expect(page.getByRole('tabpanel')).toBeVisible(); }
  expect(reads).toBe(1); await expect(page.getByRole('tabpanel')).toContainText('không phải nhật ký audit nội bộ');
  broken = true; await page.getByRole('button', { name: 'Làm mới trạng thái', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Không suy đoán trạng thái');
  await expect(page.locator('.sl-status')).not.toContainText('Carrier đã nhận'); expect(reads).toBe(2);
  await expect(page.getByRole('button', { name: /Hủy vận đơn|Giao lại|Sửa Tracking|Thu tiền/ })).toHaveCount(0);
});

test('handling with another Parcel set fails closed before rendering foreign barcode or enabling labels', async ({ page, context }) => {
  await fixture(context); const record = managementFixture(new URLSearchParams(), 1).items[0].record, mismatch = handling(record);
  mismatch.parcels[0].parcelId = '40000000-0000-4000-8000-999999999999'; mismatch.parcels[0].barcode = 'AP-P-' + mismatch.parcels[0].parcelId.replaceAll('-', '');
  await context.route('**/handling', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify(mismatch) }));
  await page.goto('/personal/shipments/' + record.shipment.id + '?tab=parcels'); await expect(page.getByRole('alert')).toContainText('Không suy đoán');
  await expect(page.locator('code.sl-code')).toHaveCount(0); await expect(page.getByRole('button', { name: 'Tải / In nhãn', exact: true })).toHaveCount(0);
});


test('too-wide date filter is explained before querying and unknown sort is never applied client-side', async ({ page, context }) => {
  const reads = await fixture(context); await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(20);
  const before = reads.length; await page.getByLabel('Từ ngày', { exact: true }).fill('2024-01-01'); await page.getByLabel('Đến ngày', { exact: true }).fill('2026-01-01');
  await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('tối đa 366 ngày'); expect(reads.length).toBe(before);
  await page.goto('/personal/shipments?sort=UPDATED'); await expect(page.getByRole('alert')).toContainText('Bộ lọc không hợp lệ'); await expect(page.locator('.pm-table')).toHaveCount(0);
});
