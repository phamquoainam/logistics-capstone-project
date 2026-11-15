import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { organizationA, organizationB, businessContextsFixture } from './support/business-fixture';
import { exportFixture, financeFixture, operationsFixture, reportingContext, reportExportId } from './support/business-report-fixture';
import { dateOffset, vietnamToday } from '../src/customer/business-report-navigation';

async function fixture(context: BrowserContext, { financial = true, denyFinance = false, denyExport = false } = {}) {
  const queries: { path: string; query: URLSearchParams }[] = [], exports: URLSearchParams[] = [];
  await context.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname, query = url.searchParams;
    const reply = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: organizationA, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: organizationA, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [reportingContext, { ...businessContextsFixture[1], permissions: reportingContext.permissions }] });
    queries.push({ path, query });
    const org = path.includes(organizationB) ? organizationB : organizationA;
    if (path.endsWith('/operations-reports')) return reply(operationsFixture(query, financial, org));
    if (path.endsWith('/shipping-finance')) return denyFinance ? reply({ code: 'RESOURCE_NOT_FOUND' }, 404) : reply(financeFixture(query, org));
    if (path.endsWith('/shipping-report-exports')) { exports.push(query); return denyExport ? reply({ code: 'RESOURCE_NOT_FOUND' }, 404) : reply(exportFixture(query.get('kind')!, query.get('section')!, org)); }
    if (path.endsWith('/shipping-report-history')) return reply({ items: [{ exportId: reportExportId, kind: 'OPERATIONS', section: 'trend', query: { from: '2026-11-01', until: '2026-11-15', interval: 'WEEK', sourceChannel: 'WEB' }, rowCount: 2, occurredAt: new Date().toISOString() }], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/shipments') && query.get('view') === 'management') return reply({ schemaVersion: 'ORGANIZATION_MANAGEMENT_1', generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', summary: { bookedShipments: '0', waitingForHandover: '0', unassigned: '0', confirmedFeesVnd: '0', unpaidFeesVnd: '0' }, total: '0', page: Number(query.get('page') ?? 0), size: 20, hasNext: false, items: [] });
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { queries, exports };
}
const base = '/merchant/' + organizationA;

test('Management contains two distinct report screens with real cohort and clear internal grains', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/reports?from=2026-11-01&until=2026-11-15');
  await expect(page.getByRole('heading', { name: 'Báo cáo vận hành Merchant', exact: true })).toBeVisible();
  await expect(page.locator('.mr-kpis > *')).toContainText(['Vận đơn trong kỳ2', 'Kiện hàng3', 'Trọng lượng thực / tính cước', '37.500']);
  const menu = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true });
  await expect(menu.getByRole('link', { name: 'Báo cáo vận hành Merchant', exact: true })).toBeVisible(); await expect(menu.getByRole('link', { name: 'Cước, COD & Đối soát', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Hoạt động nội bộ', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Nháp & hoạt động nội bộ', exact: true })).toBeVisible(); await expect(page.locator('.mr-internal-states > div')).toContainText(['Đang nháp1', 'Đã Booking2', 'Đã lưu trữ1']);
  await menu.getByRole('link', { name: 'Cước, COD & Đối soát', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Cước, COD & Đối soát', exact: true })).toBeVisible(); await expect(page.locator('.mr-kpis')).toContainText('75.000'); await expect(page.locator('.mr-table-scroll tbody tr')).toHaveCount(2); expect(api.queries.some(r => r.path.includes('/customer/'))).toBe(false);
});

test('grouping and source filters reach Backend; chart drill keeps exact interval endpoints and route', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/reports?from=2026-11-01&until=2026-11-15&sourceChannel=WEB');
  await page.getByLabel('Nhóm thời gian', { exact: true }).selectOption('WEEK'); await expect(page.locator('.mr-bars').first().getByRole('button')).toHaveCount(3);
  expect(api.queries.some(r => r.query.get('interval') === 'WEEK' && r.query.get('sourceChannel') === 'WEB')).toBe(true);
  await page.locator('.mr-bars').first().getByRole('button').first().click(); await expect(page).toHaveURL(/\/shipments\?/); const q = new URL(page.url()).searchParams; expect(q.get('from')).toBe('2026-11-01'); expect(q.get('until')).toBe('2026-11-02'); expect(q.get('sourceChannel')).toBe('WEB'); expect(q.has('interval')).toBe(false);
  await page.goto(base + '/reports?from=2026-11-01&until=2026-11-15&panel=dimensions'); await page.getByLabel('Chiều phân tích').selectOption('route'); await page.locator('.mr-dimensions').first().getByRole('button').first().click(); await expect(page).toHaveURL(/origin=SIM-A1&destination=SIM-A2/);
});

test('unbound warehouse opens bounded contributions without broadening list filters', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/reports?from=2026-11-01&until=2026-11-15&panel=dimensions&sourceChannel=WEB');
  await page.locator('.mr-dimensions').first().getByRole('button', { name: /Chưa gắn kho/ }).click();
  await expect(page.getByRole('dialog', { name: 'Vận đơn đóng góp' })).toBeVisible(); await expect(page.getByRole('dialog')).toContainText('kiểm tra 0 vận đơn nguồn'); expect(new URL(page.url()).pathname).toBe(base + '/reports');
  const source = api.queries.filter(r => r.path.endsWith('/shipments')).at(-1)!; expect(source.query.get('sourceChannel')).toBe('WEB'); expect(source.query.get('from')).toBe('2026-11-01'); expect(source.query.get('until')).toBe('2026-11-15'); await page.getByRole('dialog').press('Escape'); await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('Finance denial and missing operation money never render stale or fabricated financial amounts', async ({ page, context }) => {
  await fixture(context, { financial: false, denyFinance: true }); await page.goto(base + '/reports'); await expect(page.locator('.mr-kpis')).toContainText('Cần quyền Finance'); await expect(page.locator('.mr-kpis')).not.toContainText('37.500');
  await page.goto(base + '/finance'); await expect(page.getByRole('alert')).toContainText('Chưa có quyền đọc'); await expect(page.locator('.mr-kpis')).toHaveCount(0); await expect(page.locator('.mr-bars')).toHaveCount(0);
});

test('Finance statement links keep return context and unsupported COD never becomes a payment action', async ({ page, context }) => {
  await fixture(context); await page.goto(base + '/finance?from=2026-11-01&until=2026-11-15&sourceChannel=WEB');
  const link = page.locator('.mr-table-scroll tbody a').first(); await expect(link).toBeVisible(); const href = await link.getAttribute('href'), returnTo = new URL(href!, 'https://local.invalid').searchParams.get('returnTo'); expect(returnTo).toBe(base + '/finance?from=2026-11-01&until=2026-11-15&sourceChannel=WEB');
  await page.getByRole('button', { name: 'Giá & chênh lệch', exact: true }).click(); await page.getByText('Chi tiết tính cước', { exact: true }).first().click(); await expect(page.locator('.mr-table-scroll').first()).toContainText('Kiện 2: 500 g thực / 500 g tính cước');
  await page.getByRole('button', { name: 'COD & Đối soát', exact: true }).click(); await expect(page.locator('.mr-dependencies')).toContainText('Chưa mở chính sách COD'); await expect(page.getByRole('button', { name: /Thanh toán|Chuyển trả|Ghi PAID/ })).toHaveCount(0);
});

test('report downloads protect literal CSV/XLSX and history reopens current authorized filters', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/reports?from=2026-11-01&until=2026-11-15&interval=WEEK&sourceChannel=WEB');
  const event = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV', exact: true }).click(); const file = await event, fs = await import('node:fs/promises'), text = await fs.readFile((await file.path())!, 'utf8'); expect(text).toContain('LOCAL/TEST SIMULATION'); expect(text).toContain("'=SYNTHETIC(1)"); expect(text).not.toContain('recipient');
  const xlsxEvent = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất Excel', exact: true }).click(); const xlsxFile = await xlsxEvent, xlsx = await import('xlsx'), workbook = xlsx.read(await fs.readFile((await xlsxFile.path())!)), cell = workbook.Sheets.BaoCao.E7; expect(cell.v).toBe('=SYNTHETIC(1)'); expect(cell.t).toBe('s'); expect(cell.f).toBeUndefined(); expect(api.exports[0].get('sourceChannel')).toBe('WEB');
  await page.getByRole('button', { name: 'Lịch sử xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Lịch sử xuất báo cáo của tôi', exact: true })).toBeVisible(); await expect(page.getByText(/không phải tải một bản chứng từ cũ/)).toBeVisible(); await page.getByRole('link', { name: 'Mở lại bộ lọc', exact: true }).click(); await expect(page.getByLabel('Nhóm thời gian')).toHaveValue('WEEK'); expect(new URL(page.url()).searchParams.get('sourceChannel')).toBe('WEB');
});

test('export failure and Finance search do not download a broader dataset', async ({ page, context }) => {
  const api = await fixture(context, { denyExport: true }); const downloads: string[] = []; page.on('download', d => downloads.push(d.suggestedFilename()));
  await page.goto(base + '/reports'); await page.getByRole('button', { name: 'Xuất CSV', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa thể xuất'); expect(downloads).toHaveLength(0);
  await page.goto(base + '/finance?search=SYNTHETIC'); await expect(page.locator('.mr-kpis')).toBeVisible(); await expect(page.getByRole('button', { name: 'Xuất CSV', exact: true })).toBeDisabled(); await expect(page.getByRole('button', { name: 'Xuất Excel', exact: true })).toBeDisabled(); expect(api.exports).toHaveLength(1);
});

test('Organization switching and mobile layout never retain old scope or overflow the page', async ({ page, context }, info) => {
  const api = await fixture(context); await page.goto(base + '/reports'); await expect(page.locator('.mr-kpis')).toBeVisible();
  await page.goto('/merchant/' + organizationB + '/reports'); await expect(page.locator('.mr-heading')).toContainText('Synthetic Store B'); await expect(page.locator('.mr-kpis')).toBeVisible(); expect(api.queries.at(-1)!.path).toContain(organizationB);
  await page.setViewportSize({ width: 390, height: 844 }); await expect(page.locator('.pw-sidebar')).toBeHidden(); await expect(page.locator('.mr-kpis')).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true); await page.screenshot({ path: info.outputPath('reports-mobile-synthetic.png'), fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 1440, height: 1000 }); await page.screenshot({ path: info.outputPath('reports-desktop-synthetic.png'), fullPage: true, animations: 'disabled' });
});

test('maximum daily reporting period stays inside the mobile viewport with accessible bucket drill', async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width: 390, height: 844 });
  const today = vietnamToday(), query = new URLSearchParams({ from: dateOffset(today, -365), until: dateOffset(today, 1) });
  await page.goto(base + '/reports?' + query); const chart = page.locator('.mr-bars').first(); await expect(chart.getByRole('button')).toHaveCount(366);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
  const last = chart.getByRole('button').last(); await last.focus(); await last.press('Enter'); await expect(page).toHaveURL(/\/shipments\?/);
  expect(new URL(page.url()).searchParams.get('from')).toBe(today); expect(new URL(page.url()).searchParams.get('until')).toBe(dateOffset(today, 1));
});
