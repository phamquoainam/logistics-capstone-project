import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import type { BrowserContext } from '@playwright/test';
import { analyticsFixture, shippingReportsFixture } from './support/analytics-fixture';
import { managementFixture } from './support/management-fixture';

const period = 'from=2026-11-01&until=2026-11-04', reports = '/personal/reports?' + period, billing = '/personal/billing?' + period;
async function fixture(context: BrowserContext, count = 25) {
  const reads: URL[] = [], methods: string[] = [];
  await context.route('**/api/v1/**', route => {
    const url = new URL(route.request().url()), path = url.pathname; methods.push(route.request().method());
    const reply = (value: unknown, headers: Record<string, string> = {}) => route.fulfill({ contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-report-challenge' });
    if (path.endsWith('/refresh')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', accessToken: 'synthetic-report-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: '10000000-0000-4000-8000-000000000001', displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/personal-shipping-reports')) { reads.push(url); return reply(shippingReportsFixture(url.searchParams, count)); }
    if (path.endsWith('/personal-analytics')) return reply(analyticsFixture(url.searchParams, count));
    if (path.includes('/shipments/')) { const row = shippingReportsFixture(new URLSearchParams(period), 1).report.shipments.items[0], data = managementFixture(new URLSearchParams(), 1).items[0].record; data.shipment.id = row.id; data.shipment.code = row.code; return reply(data, { ETag: '"v1"' }); }
    if (path.endsWith('/shipping-catalog')) return reply({ simulation: true, serviceCode: 'ROAD_STANDARD', quoteEnabled: true, bookingEnabled: true, coverage: { datasetVersion: 'ARGO-SIM-COVERAGE-1', namespace: 'ARGO_SIMULATED_ADMIN_1', provenance: 'SIMULATION', effectiveFrom: '2026-11-01T00:00:00Z', areas: [{ code: 'SIM-A1', region: 'SIM-REGION-A', label: 'Synthetic A1', dropOffCode: 'SIM-DROP-A1' }, { code: 'SIM-A2', region: 'SIM-REGION-A', label: 'Synthetic A2', dropOffCode: 'SIM-DROP-A2' }], routes: [{ datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA', dropOffCode: 'SIM-DROP-A1' }] }, tariff: { version: 'ARGO-ROAD-TARIFF-1', intraFirstVnd: 25000, intraStepVnd: 3000, interFirstVnd: 38000, interStepVnd: 5000, stepGrams: 500, volumetricDivisor: 5000, maxParcels: 5, maxActualGrams: 20000, maxDimensionMm: 1000, maxGirthMm: 2000, ttlSeconds: 900, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' }, paymentMethod: 'CASH_ON_HANDOVER', payer: 'SENDER', paymentCollectionEnabled: false });
    return route.fulfill({ status: 503, contentType: 'application/json', body: '{"code":"ISOLATED_UI_UNSUPPORTED"}' });
  }); return { reads, methods };
}

test('Reports is a distinct Management entry and does not clone Overview KPI navigation', async ({ page, context }) => {
  await fixture(context); await page.goto(reports);
  await expect(page.getByRole('heading', { name: 'Báo cáo gửi hàng', exact: true })).toBeVisible();
  await expect(page.locator('#personal-management-links').getByRole('link', { name: 'Báo cáo gửi hàng', exact: true })).toHaveClass('active');
  await expect(page.getByRole('navigation', { name: 'Nội dung báo cáo' }).getByRole('button')).toHaveCount(8);
  await expect(page.locator('.an-kpi')).toHaveCount(0); await expect(page.getByRole('region', { name: 'Biểu đồ sản lượng' })).toBeVisible();
});
test('all five calendar groupings reconcile full data while UI selectors never become API parameters', async ({ page, context }) => {
  const { reads } = await fixture(context, 45); await page.goto(reports); await expect(page.locator('.sr-totals')).toContainText('45');
  for (const group of ['day', 'week', 'month', 'quarter', 'year']) { await page.getByLabel('Nhóm báo cáo', { exact: true }).selectOption(group); await expect(page.locator('.sr-totals')).toContainText('45'); }
  expect(reads).toHaveLength(1); expect(reads[0].searchParams.get('bucket')).toBe('day'); expect(reads[0].searchParams.has('group')).toBe(false); expect(reads[0].searchParams.has('section')).toBe(false);
});
test('quarter drill clips the period and returns to the exact reporting dimension instead of Overview', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/reports?from=2025-12-30&until=2026-01-03&group=quarter');
  const point = page.locator('.sr-volume-grid a').first(); await expect(point).toHaveAttribute('href', /from=2025-12-30&until=2026-01-01/); await point.click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(20); await expect(page.locator('.an-kpi')).toHaveCount(0);
  const back = page.getByRole('navigation', { name: 'Đường dẫn phân tích' }).getByRole('link', { name: 'Báo cáo gửi hàng', exact: true }); await back.click();
  await expect(page).toHaveURL(/group=quarter/); await expect(page.getByLabel('Nhóm báo cáo')).toHaveValue('quarter');
});
test('status ratios and acceptance durations use the full cohort, not the twenty-row page or delivery events', async ({ page, context }) => {
  await fixture(context, 45); await page.goto(reports + '&section=timing');
  await expect(page.locator('.sr-analysis')).toContainText('22 vận đơn đã tiếp nhận / 45 vận đơn'); await expect(page.locator('.sr-totals')).toContainText('1 phút 0 giây');
  await page.getByRole('button', { name: 'Trạng thái', exact: true }).click(); await expect(page.locator('.sr-status-list')).toContainText('48,89%');
  await expect(page.locator('.sr-status-list')).toContainText('Mẫu số: 45');
  await page.locator('.sr-status-list').getByRole('link', { name: 'Đã tiếp nhận tại Station', exact: true }).click(); await expect(page).toHaveURL(/status=AT_STATION/);
});
test('zero denominator and absent sample stay N/A while future delivery and exceptions never show fake zero reports', async ({ page, context }) => {
  await fixture(context, 0); await page.goto(reports + '&section=timing'); await expect(page.locator('.sr-totals')).toContainText('N/A');
  for (const section of ['Hiệu quả giao hàng', 'Ngoại lệ']) { await page.getByRole('button', { name: section, exact: true }).click(); await expect(page.locator('.sr-missing')).toBeVisible(); await expect(page.getByRole('button', { name: 'Xuất CSV', exact: true })).toBeDisabled(); await expect(page.locator('.sr-totals')).toHaveCount(0); }
});
test('report CSV exports full period aggregates without PII or twenty-row truncation', async ({ page, context }) => {
  await fixture(context, 45); await page.goto(reports + '&group=year'); await expect(page.locator('.sr-totals')).toBeVisible();
  const downloading = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV', exact: true }).click(); const file = await downloading, text = await readFile((await file.path())!, 'utf8');
  expect(text).toContain('LOCAL/TEST SIMULATION'); expect(text).toContain('"45","45"'); expect(text).not.toContain('example.invalid'); expect(text).not.toContain('AP-SIM-');
});
test('fees use exact cohort totals and page-only CSV while pagination does not change aggregates', async ({ page, context }) => {
  await fixture(context); await page.goto(billing); await expect(page.locator('.sf-list tbody tr')).toHaveCount(20); await expect(page.locator('[data-metric=CONFIRMED_FEES]')).toContainText('925.000');
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.locator('.sf-list tbody tr')).toHaveCount(5); await expect(page.locator('[data-metric=CONFIRMED_FEES]')).toContainText('925.000');
  const downloading = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV', exact: true }).click(); const file = await downloading, text = await readFile((await file.path())!, 'utf8');
  expect(text).toContain('Trang 2; 5/25 vận đơn'); expect(text).toContain('UNPAID'); expect(text).not.toContain('"PAID"'); expect(text).not.toContain('+84901234567');
});

test('empty fee page recovery preserves the approved period obligation and sorting', async ({ page, context }) => {
  const { reads } = await fixture(context); await page.goto(billing + '&page=2&obligation=UNPAID&sort=FEE_DESC');
  await expect(page.locator('.sr-empty')).toContainText('Trang này không có vận đơn'); await expect(page.getByRole('button', { name: 'Xuất CSV', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Về trang đầu, giữ bộ lọc', exact: true }).click(); await expect(page.locator('.sf-list tbody tr')).toHaveCount(20);
  expect(Object.fromEntries(reads.at(-1)!.searchParams)).toMatchObject({ from: '2026-11-01', until: '2026-11-04', obligation: 'UNPAID', sort: 'FEE_DESC' }); expect(reads.at(-1)!.searchParams.has('page')).toBe(false);
});
test('immutable fee detail displays actual dimensional billable weights and booked tariff without claiming VAT or collected cash', async ({ page, context }) => {
  await fixture(context, 1); await page.goto(billing); await page.getByRole('button', { name: 'Chi tiết cước', exact: true }).click();
  await expect(page.locator('.sf-snapshot h3')).toHaveText('Snapshot tính cước lúc Booking');
  await expect(page.locator('.sf-snapshot tbody')).toContainText('200 g'); await expect(page.locator('.sf-snapshot tbody')).toContainText('500 g'); await expect(page.locator('.sf-snapshot tbody')).toContainText('25.000');
  await expect(page.locator('.sf-snapshot')).toContainText('không phải VAT 0%'); await expect(page.locator('.sf-snapshot')).toContainText('Giá trị hàng không phải COD hoặc tiền đã thu');
  await page.getByRole('link', { name: /^AP-SIM-/ }).click(); await page.getByRole('link', { name: 'Quay lại danh sách', exact: true }).click(); await expect(page).toHaveURL(new RegExp('/personal/billing\\?' + period));
});
test('payment refund COD and inquiry panels neither fetch unsupported sources nor expose financial mutations', async ({ page, context }) => {
  const { reads, methods } = await fixture(context); await page.goto(billing); await expect(page.locator('.sf-list tbody tr')).toHaveCount(20); const before = reads.length;
  for (const name of ['Lịch sử thanh toán', 'Điều chỉnh / hoàn cước', 'COD', 'Tra soát cước']) { await page.getByRole('button', { name, exact: true }).click(); await expect(page.locator('.sr-missing')).toBeVisible(); await expect(page.locator('.sf-kpis')).toHaveCount(0); }
  expect(reads.length).toBe(before); expect(methods.filter(m => m !== 'GET')).toEqual(['POST']); await expect(page.getByRole('button', { name: /Thanh toán ngay|Hoàn tiền|Rút tiền/ })).toHaveCount(0);
});
test('malformed RF05 or rejected query never becomes a successful zero financial report', async ({ page, context }) => {
  await fixture(context); await context.route('**/personal-shipping-reports?**', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ ...shippingReportsFixture(new URL(route.request().url()).searchParams), acceptanceTiming: { sampleShipments: '999', totalDurationMillis: '0', minimumMillis: null, maximumMillis: null } }) }));
  await page.goto(billing); await expect(page.getByRole('alert')).toContainText('Chưa xác nhận được dữ liệu'); await expect(page.locator('.sf-kpis')).toHaveCount(0); await expect(page.getByRole('button', { name: 'Xuất CSV', exact: true })).toHaveCount(0);
});
for (const width of [1440, 768, 390]) test(`reports and charges remain readable and bounded at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 });
  for (const [path, kind] of [[reports, 'reports'], [billing, 'fees']]) { await page.goto(path); await expect(page.locator(kind === 'reports' ? '.sr-volume-grid' : '.sf-list tbody tr').first()).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: `../../../.demo/reports-fees-review/${kind}-${width}.png`, fullPage: true, animations: 'disabled' }); }
});
