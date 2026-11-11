import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { managementFixture } from './support/management-fixture';
import { analyticsFixture, shippingReportsFixture } from './support/analytics-fixture';
async function fixture(context: BrowserContext, count = 25) {
  await context.route('**/api/v1/**', async route => { const path = new URL(route.request().url()), reply = (v: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(v) });
    if (path.pathname.endsWith('/csrf')) return reply({ challenge: 'synthetic-analytics-csrf' });
    if (path.pathname.endsWith('/refresh')) return reply({ accessToken: 'synthetic-analytics-access', accountId: '10000000-0000-4000-8000-000000000001', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.pathname.endsWith('/me')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.pathname.endsWith('/personal-shipping-reports')) return reply(shippingReportsFixture(path.searchParams, count));
    if (path.pathname.endsWith('/personal-analytics')) return reply(analyticsFixture(path.searchParams, count));
    return route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'ISOLATED_UI_UNSUPPORTED' }) });
  });
}

const period = 'from=2026-11-01&until=2026-11-04';
const overview = '/personal/overview?' + period;
const list = '/personal/overview/shipments?' + period;
const metric = (id: string) => `[data-metric=${id}] .an-kpi-value`;

test('Overview is one compact summary without report tabs and with source-bound chart summaries', async ({ page, context }) => {
  await fixture(context); await page.goto(overview);
  await expect(page.locator('.an-kpi')).toHaveCount(4);
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25');
  await expect(page.locator('.an-source tbody tr')).toHaveCount(5);
  await expect(page.locator('.an-status-card,.an-volume-card,.an-parcel-chart')).toHaveCount(3);
  await expect(page.locator('.an-summary-grid,.an-dimension-nav,.an-quick-actions,.an-kpi-bottom svg')).toHaveCount(0);
  await expect(page.locator('.an-dashboard').getByRole('link', { name: /Báo cáo gửi hàng/ })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Áp dụng', exact: true })).toHaveCount(0);
  await expect(page.locator('.an-parcel-total')).toContainText('26');
});

test('one status click opens contributing rows only and Back/Forward restores source context', async ({ page, context }) => {
  await fixture(context); await page.goto(overview);
  await page.locator('.an-status-legend').getByRole('link', { name: /Đã nhận tại Station/ }).click();
  await expect(page).toHaveURL(/overview\/shipments\?.*status=AT_STATION/);
  await expect(page.getByRole('heading', { name: 'Vận đơn đã tiếp nhận', exact: true })).toBeVisible();
  await expect(page.locator('.an-kpi,.an-time-chart,.an-donut,.an-dimension-nav,.an-filter-toolbar')).toHaveCount(0);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(12);
  await expect(page.locator('.an-row-count')).toContainText('12 vận đơn');
  expect(new URL(page.url()).searchParams.get('from')).toBe('2026-11-01');
  await page.goBack(); await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25');
  await page.goForward(); await expect(page.locator('.an-source tbody tr')).toHaveCount(12);
  await page.getByRole('navigation', { name: 'Đường dẫn phân tích' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25');
});

for (const dimension of ['status', 'services', 'routes', 'fees', 'parcels']) test(`legacy ${dimension} bookmarks remain list-only compatible`, async ({ page, context }) => {
  await fixture(context, 2); await page.goto('/personal/overview/' + dimension + '?' + period + '&status=AT_STATION');
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1);
  await expect(page.locator('.an-kpi,.an-time-chart,.an-dimension-nav')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Bỏ lọc trạng thái', exact: true })).toBeVisible();
});

test('global selects apply immediately without an Apply button and keep the same date cohort', async ({ page, context }) => {
  await fixture(context); await page.goto(overview);
  await page.getByLabel('Trạng thái', { exact: true }).selectOption('AT_STATION');
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('12');
  await page.getByLabel('Tuyến', { exact: true }).selectOption('SIM-A1/SIM-A2');
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('12');
  const p = new URL(page.url()).searchParams; expect(p.get('from')).toBe('2026-11-01'); expect(p.get('destination')).toBe('SIM-A2');
  await page.getByRole('button', { name: 'Xóa bộ lọc', exact: true }).click();
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25');
  expect(new URL(page.url()).searchParams.has('status')).toBe(false);
});

test('zero KPI and status groups cannot expand a filtered cohort through a contradictory drill', async ({ page, context }) => {
  await fixture(context); await page.goto(overview + '&status=AT_STATION');
  await expect(page.locator(metric('AWAITING_HANDOVER'))).toHaveText('0vận đơn');
  await expect(page.locator('[data-metric=AWAITING_HANDOVER] a')).toHaveCount(0);
  await expect(page.locator('.an-status-legend').getByRole('link', { name: /Chờ bàn giao/ })).toHaveCount(0);
  await page.locator(metric('BOOKED_SHIPMENTS')).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(12);
  await expect(page.locator('.an-source tbody tr')).not.toContainText(['Chờ bàn giao']);
});

test('presets apply in one click and Back/Forward restore dates and global selections', async ({ page, context }) => {
  await fixture(context, 2); await page.goto(overview + '&status=AT_STATION');
  const tomorrow = new Date(Date.now() + 7 * 3600000 + 86400000).toISOString().slice(0, 10);
  for (const days of [1, 7, 30, 90]) {
    const name = days === 1 ? 'Hôm nay' : `${days} ngày`; await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click(); await page.getByRole('button', { name, exact: true }).click();
    await expect(page.getByRole('dialog', { name: 'Chọn khoảng thời gian', exact: true })).toHaveCount(0);
    const p = new URL(page.url()).searchParams; expect(p.get('from')).toBe(new Date(Date.parse(tomorrow) - days * 86400000).toISOString().slice(0, 10)); expect(p.get('status')).toBe('AT_STATION');
  }
  await page.goBack(); await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click(); await expect(page.getByRole('button', { name: '30 ngày', exact: true })).toHaveAttribute('aria-pressed', 'true'); await page.keyboard.press('Escape');
  await page.goForward(); await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click(); await expect(page.getByRole('button', { name: '90 ngày', exact: true })).toHaveAttribute('aria-pressed', 'true');
});

test('custom dates require one confirmation and cancel/invalid input never changes applied filters', async ({ page, context }) => {
  await fixture(context); await page.goto(overview);
  await expect(page.getByLabel('Từ ngày', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click();
  await page.getByLabel('Từ ngày', { exact: true }).fill('31/02/2026');
  await page.getByRole('button', { name: 'Xác nhận khoảng ngày', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('dd/MM/yyyy'); expect(page.url()).toContain(period);
  await page.getByRole('button', { name: 'Hủy', exact: true }).click();
  await expect(page.getByLabel('Từ ngày', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click();
  for (const [from, to] of [['01/01/2024', '01/11/2026'], ['01/01/2099', '02/01/2099']]) {
    await page.getByLabel('Từ ngày', { exact: true }).fill(from); await page.getByLabel('Đến ngày', { exact: true }).fill(to);
    await page.getByRole('button', { name: 'Xác nhận khoảng ngày', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('366 ngày');
  }
  await page.getByLabel('Từ ngày', { exact: true }).fill('02/11/2026'); await page.getByLabel('Đến ngày', { exact: true }).fill('03/11/2026');
  await page.getByRole('button', { name: 'Xác nhận khoảng ngày', exact: true }).click();
  await expect(page.locator('.an-period-trigger>span').first()).toHaveText('02/11/2026 – 03/11/2026');
  await expect(page.getByLabel('Từ ngày', { exact: true })).toHaveCount(0);
});

test('source list sorts immediately, searches explicitly and paginates without duplicate KPI blocks', async ({ page, context }) => {
  await fixture(context); await page.goto(list); await expect(page.locator('.an-source tbody tr')).toHaveCount(20);
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.locator('.an-source tbody tr')).toHaveCount(5);
  await page.getByLabel('Sắp xếp', { exact: true }).selectOption('FEE_DESC');
  await expect(page.locator('.an-source tbody tr')).toHaveCount(20); await expect(page.locator('.an-source tbody tr').first()).toContainText('49.000');
  await page.getByLabel('Tìm mã vận đơn', { exact: true }).fill('AP-SIM-' + '1'.padStart(32, '0')); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1); await expect(page.locator('.an-kpi')).toHaveCount(0);
});

test('chart focus exposes exact point and drills straight into contributing Shipments', async ({ page, context }) => {
  await fixture(context); await page.goto(overview);
  await page.locator('.an-chart-bar').first().focus(); await expect(page.locator('.an-volume-card .an-chart-context')).toContainText('01/11/2026');
  await page.keyboard.press('Enter'); await expect(page).toHaveURL(/overview\/shipments\?.*day=2026-11-01/);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(9); await expect(page.locator('.an-time-chart,.an-kpi')).toHaveCount(0);
  await page.getByRole('navigation', { name: 'Đường dẫn phân tích' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
  await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25');
});

test('one chart switches fee/volume locally and restores its mode after a drill', async ({ page, context }) => {
  await fixture(context); let reads = 0; page.on('request', request => { if (request.url().includes('/personal-analytics?')) reads++; });
  await page.goto(overview); await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('25'); const initial = reads;
  await page.getByRole('button', { name: 'Cước', exact: true }).click();
  await expect(page.locator('.ac-column')).toHaveCount(3); expect(reads).toBe(initial);
  await expect(page.locator('.an-volume-card .ac-chart-summary')).toContainText('925.000 ₫');
  await page.locator('.an-chart-bar').first().focus(); await expect(page.locator('.ac-tooltip')).toContainText('333.000 ₫');
  await page.keyboard.press('Enter'); await expect(page.locator('.an-source tbody tr')).toHaveCount(9);
  await page.getByRole('navigation', { name: 'Đường dẫn phân tích' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Cước', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Vận đơn', exact: true }).click();
  const plot = await page.locator('.an-volume-card .ac-plot').boundingBox(), svg = await page.locator('.an-volume-card .ac-plot-svg').boundingBox(), line = await page.locator('.an-volume-card .ac-line').boundingBox();
  expect(Math.abs(svg!.width - plot!.width)).toBeLessThan(1); expect(line!.width).toBeGreaterThan(plot!.width / 2);
  const tick = await page.locator('.an-volume-card .ac-x-axis span').first().boundingBox(), first = await page.locator('.an-chart-bar').first().boundingBox();
  expect(Math.abs(tick!.x + tick!.width / 2 - first!.x - first!.width / 2)).toBeLessThan(1);
});

test('Parcel drill explains matching Shipments while retaining all their Parcels', async ({ page, context }) => {
  await fixture(context, 2); await page.goto(overview); await expect(page.locator('.an-parcel-total')).toContainText('3');
  await page.locator('.ac-parcel-bars').getByRole('link', { name: /Trên 0,5 đến 5 kg/ }).click();
  await expect(page).toHaveURL(/overview\/shipments\?.*weight=UP_TO_5KG/); await expect(page.locator('.an-source tbody tr')).toHaveCount(1);
  await expect(page.locator('.an-source tbody tr')).toContainText('2 kiện');
  await page.getByRole('button', { name: 'Bỏ lọc trọng lượng', exact: true }).click(); await expect(page.locator('.an-source tbody tr')).toHaveCount(2);
});

test('removing chips clears route pairs and resets out-of-range pages without losing dates', async ({ page, context }) => {
  await fixture(context); await page.goto(list + '&origin=SIM-A1&destination=SIM-A2&search=AP-SIM&page=9');
  await expect(page.locator('.an-source tbody tr')).toHaveCount(0); await page.getByRole('button', { name: 'Về trang đầu', exact: true }).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(20);
  await page.getByRole('button', { name: 'Bỏ lọc tuyến', exact: true }).click();
  expect(new URL(page.url()).searchParams.has('destination')).toBe(false); expect(new URL(page.url()).searchParams.get('until')).toBe('2026-11-04');
  await page.getByRole('button', { name: 'Bỏ lọc mã vận đơn', exact: true }).click(); await expect(page.locator('.an-row-count')).toContainText('25');
});

test('legacy Overview row-specific URLs redirect to list results instead of hiding filters in KPIs', async ({ page, context }) => {
  await fixture(context); await page.goto(overview + '&sort=FEE_DESC&page=1');
  await expect(page).toHaveURL(/overview\/shipments\?.*sort=FEE_DESC&page=1/);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(5); await expect(page.locator('.an-kpi')).toHaveCount(0);
});

test('empty, unavailable and malformed data remain distinct with no fabricated counts or charts', async ({ page, context }) => {
  await fixture(context, 0); let failed = true;
  await context.route('**/api/v1/customer/personal-analytics?*', route => failed ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'ANALYTICS_UNAVAILABLE' }) }) : route.fulfill({ contentType: 'application/json', body: JSON.stringify(analyticsFixture(new URL(route.request().url()).searchParams)) }));
  await page.goto(overview); await expect(page.getByRole('alert')).toContainText('Số liệu chưa được xác nhận'); await expect(page.locator('.an-kpi')).toHaveCount(0);
  failed = false; await page.getByRole('button', { name: 'Thử lại Analytics', exact: true }).click();
  await expect(page.locator('.an-empty')).toHaveCount(1); await expect(page.locator('.an-kpi')).toHaveCount(4);
  await expect(page.locator('.an-time-chart,.an-donut,.an-source')).toHaveCount(0);
  for (const text of await page.locator('.an-trend').allTextContents()) expect(text).not.toContain('%');
  await expect(page.locator('.an-dashboard').getByRole('link', { name: /Tạo vận đơn/ })).toHaveCount(1);
  await context.route('**/api/v1/customer/personal-analytics?*', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ ...analyticsFixture(), context: 'BUSINESS' }) }));
  await page.reload(); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.locator('.an-kpi')).toHaveCount(0);
});

test('refresh refetches the current query without requiring filter reapplication', async ({ page, context }) => {
  await fixture(context); let reads = 0;
  await context.route('**/api/v1/customer/personal-analytics?*', route => { reads++; return route.fulfill({ contentType: 'application/json', body: JSON.stringify(analyticsFixture(new URL(route.request().url()).searchParams, reads)) }); });
  await page.goto(overview); await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('1'); const original = page.url();
  await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('2'); expect(page.url()).toBe(original);
});

test('malformed date deep links are recoverable without a page crash', async ({ page, context }) => {
  await fixture(context); const errors: string[] = []; page.on('pageerror', error => errors.push(error.name));
  await context.route('**/api/v1/customer/personal-analytics?*', route => new URL(route.request().url()).searchParams.get('from') === 'invalid' ? route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ code: 'VALIDATION_FAILED' }) }) : route.fulfill({ contentType: 'application/json', body: JSON.stringify(analyticsFixture(new URL(route.request().url()).searchParams, 2)) }));
  await page.goto('/personal/overview?from=invalid&until=invalid'); await expect(page.getByRole('alert')).toContainText('Bộ lọc không hợp lệ');
  await page.getByRole('button', { name: 'Xóa bộ lọc', exact: true }).click(); await expect(page.locator(metric('BOOKED_SHIPMENTS'))).toContainText('2'); expect(errors).toEqual([]);
});

for (const width of [1440, 768, 390]) for (const count of [0, 1, 75]) test(`focused Overview ${count} records at ${width}px stays bounded and keyboard usable`, async ({ page, context }) => {
  await fixture(context, count); await page.setViewportSize({ width, height: 960 }); await page.goto(overview);
  await expect(page.locator('.an-kpi')).toHaveCount(4); await expect(page.locator('.an-source tbody tr')).toHaveCount(Math.min(count, 5));
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await expect(page.locator('.an-kpi .an-info')).toHaveCount(0);
  await expect(page.locator('.an-kpi-spark')).toHaveCount(4);
  const value = page.locator(metric('UNPAID_OBLIGATIONS')), description = await value.getAttribute('aria-describedby');
  await expect(page.locator('[id="' + description + '"]')).toContainText('UNPAID');
  const periodButton = page.getByRole('button', { name: 'Khoảng thời gian', exact: true }); await periodButton.focus(); await page.keyboard.press('Enter');
  const dialog = page.getByRole('dialog', { name: 'Chọn khoảng thời gian', exact: true }); await expect(dialog).toBeVisible();
  const bounds = await dialog.boundingBox(); expect(bounds!.x).toBeGreaterThanOrEqual(0); expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width);
  await page.keyboard.press('Escape'); await expect(periodButton).toBeFocused();
  await page.getByRole('heading', { name: 'Tổng quan', exact: true }).focus(); await page.mouse.move(0, 0); await page.evaluate(() => scrollTo(0, 0));
  await page.screenshot({ path: `../../../.demo/overview-simplification/${count}-${width}.png`, fullPage: true, animations: 'disabled' });
});

for (const width of [1440, 390]) test(`large exact VND tooltips remain bounded at ${width}px`, async ({ page, context }) => {
  await fixture(context, 1); const value = '18446744073709551614';
  await context.route('**/api/v1/customer/personal-analytics?*', route => {
    const report = analyticsFixture(new URL(route.request().url()).searchParams, 1);
    for (const m of report.metrics.filter(m => m.unit === 'VND')) { m.value = value; m.points[0].value = value; }
    report.volume[0].confirmedFeeVnd = value; report.volume[0].unpaidVnd = value; report.routes[0].confirmedFeeVnd = value; report.services[0].confirmedFeeVnd = value; report.shipments.items[0].confirmedFeeVnd = value; report.shipments.items[0].unpaidVnd = value;
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify(report) });
  });
  await page.setViewportSize({ width, height: 960 }); await page.goto(overview + '&compare=none'); await page.getByRole('button', { name: 'Cước', exact: true }).click();
  await page.locator('.an-chart-bar').first().focus(); await expect(page.locator('.ac-tooltip')).toContainText('18.446.744.073.709.551.614 ₫');
  await expect(page.locator('.an-volume-card .ac-chart-summary')).toContainText('Không đối chiếu kỳ trước');
  const tip = await page.locator('.ac-tooltip').boundingBox(); expect(tip!.x).toBeGreaterThanOrEqual(0); expect(tip!.x + tip!.width).toBeLessThanOrEqual(width);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test('private document detail and return retain the exact contributing page', async ({ page, context }) => {
  await fixture(context); await page.goto(list + '&sort=FEE_DESC&page=1');
  const href = await page.locator('.an-source tbody td:first-child a').first().getAttribute('href');
  const record = managementFixture(new URLSearchParams(), 5).items.map(item => item.record).find(r => href?.endsWith(r.shipment.id)); expect(record).toBeDefined();
  let reads = 0; await context.route('**/api/v1/customer/shipments/' + record!.shipment.id, route => { reads++; return route.fulfill({ contentType: 'application/json', headers: { ETag: '"v1"' }, body: JSON.stringify(record) }); });
  await page.locator('.an-source tbody tr').first().getByRole('link', { name: /^Xem nhãn/ }).click();
  await expect(page.getByRole('tab', { name: 'Chứng từ', exact: true })).toHaveAttribute('aria-selected', 'true'); expect(reads).toBe(1);
  await page.getByRole('link', { name: 'Quay lại danh sách', exact: true }).click();
  await expect(page).toHaveURL(/sort=FEE_DESC&page=1/); await expect(page.locator('.an-source tbody tr')).toHaveCount(5); await expect(page.locator('.an-kpi')).toHaveCount(0);
});


for (const width of [1440, 390]) test(`list-only status results have no duplicated dashboard at ${width}px`, async ({ page, context }) => {
  await fixture(context, 25); await page.setViewportSize({ width, height: 960 }); await page.goto(list + '&status=AT_STATION');
  await expect(page.locator('.an-source tbody tr')).toHaveCount(12); await expect(page.locator('.an-kpi,.an-time-chart,.an-donut,.an-filter-toolbar,.an-dimension-nav')).toHaveCount(0);
  expect(await page.locator('.an-table-scroll').evaluate(node => getComputedStyle(node).maxHeight)).toBe('none');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: `../../../.demo/overview-simplification/list-${width}.png`, fullPage: true, animations: 'disabled' });
});

test('recent rows use distinct short codes and Personal alias returns to its actual Overview', async ({ page, context }) => {
  await fixture(context, 2); await page.goto('/personal?' + period);
  await expect(page.locator('.an-recent tbody td:first-child a')).toHaveCount(2);
  const codes = await page.locator('.an-recent tbody td:first-child a').allTextContents(); expect(new Set(codes).size).toBe(2);
  const href = await page.locator('.an-recent tbody td:first-child a').first().getAttribute('href');
  const record = managementFixture(new URLSearchParams(), 2).items.map(item => item.record).find(r => href?.endsWith(r.shipment.id)); expect(record).toBeDefined();
  await context.route('**/api/v1/customer/shipments/' + record!.shipment.id, route => route.fulfill({ contentType: 'application/json', headers: { ETag: '"v1"' }, body: JSON.stringify(record) }));
  await page.locator('.an-recent tbody td:first-child a').first().click();
  await page.getByRole('link', { name: 'Về Tổng quan', exact: true }).click();
  await expect(page).toHaveURL(/personal\/overview\?from=2026-11-01/); await expect(page.locator('.an-kpi')).toHaveCount(4);
});


test('compact title/filters precede KPI cards without a large empty header region', async ({ page, context }) => {
  await fixture(context, 25); await page.setViewportSize({ width: 1440, height: 960 }); await page.goto(overview);
  await expect(page.locator('.an-kpi')).toHaveCount(4);
  const title = await page.getByRole('heading', { name: 'Tổng quan', exact: true }).boundingBox(), cards = await page.locator('.an-kpi-strip').boundingBox();
  expect(cards!.y - title!.y).toBeLessThan(110);
  await expect(page.locator('.an-period-caption,.an-presets')).toHaveCount(0);
});

test('each KPI sparkline uses its own exact metric series and zero series stay real zeros', async ({ page, context }) => {
  await fixture(context, 2); await page.goto(overview);
  const report = analyticsFixture(new URLSearchParams(period), 2);
  for (const id of ['BOOKED_SHIPMENTS', 'AWAITING_HANDOVER', 'ACCEPTED_SHIPMENTS', 'UNPAID_OBLIGATIONS']) {
    await expect(page.locator(`[data-metric=${id}] .an-kpi-spark`)).toHaveAttribute('data-values', report.metrics.find(m => m.id === id)!.points.map(p => p.value).join(','));
    const svg = await page.locator(`[data-metric=${id}] .an-kpi-spark`).boundingBox(); expect(svg!.width).toBeGreaterThan(40); expect(svg!.height).toBeGreaterThan(25);
  }
  await page.getByLabel('Trạng thái', { exact: true }).selectOption('AT_STATION');
  await expect(page.locator('[data-metric=AWAITING_HANDOVER] .an-kpi-spark')).toHaveAttribute('data-values', '0,0,0');
  await expect(page.locator('[data-metric=AWAITING_HANDOVER] a')).toHaveCount(0);
});

test('six meaningful charts include source route count, confirmed fees and Parcel timeline with direct drill', async ({ page, context }) => {
  await fixture(context, 2); await page.goto(overview);
  await expect(page.locator('.an-overview-charts .an-card:not(.an-source)')).toHaveCount(6);
  await expect(page.locator('.an-route-volume')).toContainText('2 vận đơn');
  await expect(page.locator('.an-route-cost')).toContainText('51.000 ₫');
  await expect(page.locator('.an-parcel-timeline .ac-chart-summary')).toContainText('3 kiện');
  await page.locator('.an-route-cost .ac-route-bars a').click();
  await expect(page).toHaveURL(/overview\/shipments\?.*origin=SIM-A1&destination=SIM-A2/);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(2); await expect(page.locator('.an-kpi,.an-time-chart')).toHaveCount(0);
  await page.goBack(); await page.locator('.an-parcel-timeline .an-chart-bar').first().focus();
  await expect(page.locator('.an-parcel-timeline .an-chart-context')).toContainText('2 kiện');
  await page.keyboard.press('Enter'); await expect(page.locator('.an-source tbody tr')).toHaveCount(1); await expect(page.locator('.an-source tbody tr')).toContainText('2 kiện');
});

test('calendar date selection is bounded, supports keyboard inspection and keeps drafts until confirmed', async ({ page, context }) => {
  await fixture(context, 2); await page.goto(overview); const original = page.url();
  await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click();
  await page.getByRole('button', { name: '02/11/2026', exact: true }).click();
  await page.getByRole('button', { name: '03/11/2026', exact: true }).click();
  expect(page.url()).toBe(original); await page.getByRole('button', { name: 'Xác nhận khoảng ngày', exact: true }).click();
  await expect(page.locator('.an-period-trigger')).toContainText('02/11/2026 – 03/11/2026');
  await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click();
  const day = page.getByRole('button', { name: '02/11/2026', exact: true }); await day.focus(); await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('button', { name: '03/11/2026', exact: true })).toBeFocused();
  await page.keyboard.press('Escape'); await expect(page.getByRole('button', { name: 'Khoảng thời gian', exact: true })).toBeFocused();
});


for (const width of [1440, 390]) test(`calendar popover is legible, dismissible and bounds future days at ${width}px`, async ({ page, context }) => {
  await fixture(context, 2); await page.setViewportSize({ width, height: 960 }); await page.goto(overview);
  await page.getByRole('button', { name: 'Khoảng thời gian', exact: true }).click();
  const dialog = page.getByRole('dialog', { name: 'Chọn khoảng thời gian', exact: true }); await expect(dialog).toBeVisible();
  await expect(dialog.locator('.an-calendar-month')).toHaveCount(2);
  const today = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10);
  for (const cell of await dialog.locator('[data-date]').all()) {
    const day = await cell.getAttribute('data-date'); if (day! > today) await expect(cell).toBeDisabled();
  }
  const bounds = await dialog.boundingBox(); expect(bounds!.x).toBeGreaterThanOrEqual(0); expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width); expect(bounds!.y + bounds!.height).toBeLessThanOrEqual(960);
  await page.screenshot({ path: `../../../.demo/overview-vivid/calendar-${width}.png`, fullPage: false, animations: 'disabled' });
  await expect(dialog).toBeVisible();
  await page.getByRole('heading', { name: 'Hoạt động gửi hàng', exact: true }).click(); await expect(dialog).toHaveCount(0);
});
