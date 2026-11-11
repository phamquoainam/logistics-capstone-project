import { test, expect } from '@playwright/test';
import type { BrowserContext, Page } from '@playwright/test';
import { managementFixture } from './support/management-fixture';
import { toolsCatalog } from './support/shipping-tools-fixture';
const owner = '10000000-0000-4000-8000-000000000001';
async function fixture(context: BrowserContext, options: { catalogFailure?: boolean; readFailure?: boolean; deniedPdf?: boolean; count?: number } = {}) {
  const calls: { method: string; url: URL }[] = [], source = managementFixture(new URLSearchParams(), options.count ?? 2).items.map(i => i.record);
  await context.route('**/api/v1/**', route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname; calls.push({ method: request.method(), url });
    const reply = (data: unknown, status = 200, headers: Record<string, string> = {}) => route.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(data) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accessToken: 'synthetic-tools-access', accountId: owner, expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: owner, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/shipping-catalog')) return options.catalogFailure ? reply({ code: 'PILOT_DISABLED' }, 503) : reply(toolsCatalog);
    if (path === '/api/v1/customer/shipments') {
      if (options.readFailure) return reply({ code: 'SERVICE_UNAVAILABLE' }, 503);
      const data = managementFixture(url.searchParams, 0, source); return reply(url.searchParams.get('view') === 'management' ? data : { items: data.items.map(i => i.record), page: data.page, size: 20, hasNext: data.hasNext });
    }
    if (path.endsWith('/labels.pdf')) { expect(request.headers().authorization).toBe('Bearer synthetic-tools-access'); return options.deniedPdf ? reply({ code: 'DENIED' }, 403) : route.fulfill({ contentType: 'application/pdf', body: '%PDF-1.7\nSYNTHETIC PRIVATE LABEL' }); }
    const record = source.find(s => path.includes(s.shipment.id));
    if (record && path.endsWith('/handling')) return reply({ shipmentId: record.shipment.id, state: 'WAITING_FOR_HANDOVER', version: 0, parcels: record.shipment.parcels.map(p => ({ parcelId: p.id, parcelNumber: p.parcelNumber, barcode: 'AP-P-' + p.id.replaceAll('-', ''), state: 'NOT_ACCEPTED' })), events: [], labelAvailable: true, simulation: true });
    if (record) return reply(record, 200, { ETag: '"v1"' });
    if (path.endsWith('/addresses')) return reply({ items: [], page: Number(url.searchParams.get('page') ?? '0'), size: 20, hasNext: false });
    return reply({ code: 'ISOLATED_UI_UNSUPPORTED' }, 503);
  }); return { calls, source };
}
async function measurements(page: Page) {
  await page.getByLabel('Nơi gửi', { exact: true }).selectOption('SIM-A1'); await page.getByLabel('Nơi nhận', { exact: true }).selectOption('SIM-B1');
  await page.getByLabel('Khối lượng kiện 1', { exact: true }).fill('501'); for (const k of ['Dài', 'Rộng', 'Cao']) await page.getByLabel(k + ' kiện 1', { exact: true }).fill('10');
}
test('lookup and labels are peer menu pages with dedicated content, no copied management KPIs', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/lookup'); await expect(page.getByRole('heading', { name: 'Tra cứu & Báo giá', exact: true })).toBeVisible(); await expect(page.locator('.pm-kpis,.pm-categories')).toHaveCount(0);
  const menu = page.getByRole('complementary'); await expect(menu.locator('#personal-management-links').getByRole('link', { name: 'Tra cứu & Báo giá' })).toHaveCount(0); await expect(menu.getByRole('link', { name: 'In nhãn & Tiện ích', exact: true }).locator('.pw-nav-dot')).toHaveCount(0);
});
test('multiple code lookup deduplicates, hides contacts and distinguishes missing from source failure', async ({ page, context }) => {
  const { calls, source } = await fixture(context); await page.goto('/personal/lookup'); const code = source[0].shipment.code;
  await page.getByLabel('Mã vận đơn', { exact: true }).fill(code + '\n' + code + ', AP-SIM-' + 'f'.repeat(32)); await page.locator('.st-search').getByRole('button', { name: 'Tra cứu vận đơn', exact: true }).click();
  await expect(page.locator('.st-tracking-results article')).toHaveCount(2); await expect(page.locator('.st-tracking-results')).toContainText('không có quyền'); await expect(page.locator('.st-tracking-results')).not.toContainText('Synthetic contact');
  expect(calls.filter(c => c.url.pathname.endsWith('/shipments'))).toHaveLength(2); await page.getByRole('button', { name: 'Xem trạng thái & hành trình' }).click(); await expect(page.locator('.sl-timeline')).toContainText('Booking đã xác nhận');
  await page.getByRole('link', { name: 'Chi tiết vận đơn', exact: true }).click(); await page.getByRole('link', { name: 'Về tra cứu', exact: true }).click(); await expect(page.getByLabel('Mã vận đơn', { exact: true })).toHaveValue(code + '\n' + code + ', AP-SIM-' + 'f'.repeat(32));
});
test('malformed and oversized code batches make no Shipment requests', async ({ page, context }) => {
  const { calls } = await fixture(context); await page.goto('/personal/lookup'); await page.getByLabel('Mã vận đơn', { exact: true }).fill('someone@example.invalid'); await page.locator('.st-search').getByRole('button', { name: 'Tra cứu vận đơn', exact: true }).click(); await expect(page.getByRole('alert')).toBeVisible();
  expect(calls.filter(c => c.url.pathname.endsWith('/shipments'))).toHaveLength(0);
});
test('failed lookup and catalog reads never become not-found or a zero-price success', async ({ page, context }) => {
  const { source } = await fixture(context, { readFailure: true, catalogFailure: true }); await page.goto('/personal/lookup'); await page.getByLabel('Mã vận đơn', { exact: true }).fill(source[0].shipment.code); await page.locator('.st-search').getByRole('button', { name: 'Tra cứu vận đơn', exact: true }).click(); await expect(page.locator('.st-tracking-results')).toContainText('Chưa xác nhận được kết quả');
  await page.getByRole('button', { name: 'Ước tính cước', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa thể tính cước'); await expect(page.locator('.st-estimate-total')).toHaveCount(0);
});
test('indicative quote handoff retains measured facts, no contacts money or active quote and no automatic writes', async ({ page, context }) => {
  const { calls } = await fixture(context); await page.goto('/personal/lookup?view=estimate'); await measurements(page); await page.getByRole('button', { name: 'Tính cước tham khảo', exact: true }).click(); await expect(page.locator('.st-estimate-total')).toHaveText('43.000 ₫');
  await page.getByRole('button', { name: 'Dùng thông tin này tạo vận đơn' }).click(); await expect(page.locator('#weightGrams0')).toHaveValue('501'); await expect(page.locator('#lengthMm0')).toHaveValue('10'); await expect(page.locator('#sender-name')).toHaveValue('');
  expect(calls.filter(c => !['GET'].includes(c.method) && !c.url.pathname.endsWith('/refresh'))).toHaveLength(0); await expect(page.locator('.pb-total')).toHaveCount(0);
});
test('unsupported route stays NOT_SUPPORTED and point filtering never supplies real locations or ETA', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/lookup?view=coverage'); await page.getByLabel('Khu vực gửi', { exact: true }).selectOption('SIM-A2'); await page.getByLabel('Khu vực nhận', { exact: true }).selectOption('SIM-B1'); await expect(page.getByRole('status')).toContainText('NOT_SUPPORTED');
  await page.getByLabel('Lọc khu vực điểm gửi').selectOption('SIM-A1'); await expect(page.locator('.st-drop-point')).toHaveCount(1); await expect(page.locator('.st-drop-point')).toContainText('SIM-DROP-A1'); await expect(page.locator('.st-catalog-grid')).toContainText('Chưa có địa chỉ, tọa độ');
});
test('estimate facts are saved only on explicit Draft submission and the chosen route reaches fresh Quote review', async ({ page, context }) => {
  await fixture(context); let writes = 0;
  await context.route('**/api/v1/customer/shipment-drafts', route => { writes++; return route.fulfill({ status: 201, contentType: 'application/json', headers: { ETag: '"v1"' }, body: JSON.stringify({ id: '60000000-0000-4000-8000-000000000001', accountId: owner, ownerType: 'PERSONAL', state: 'DRAFT', input: route.request().postDataJSON(), version: 1, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z' }) }); });
  await page.goto('/personal/lookup?view=estimate'); await measurements(page); await page.getByRole('button', { name: 'Tính cước tham khảo', exact: true }).click(); await page.getByRole('button', { name: 'Dùng thông tin này tạo vận đơn' }).click(); expect(writes).toBe(0);
  for (const kind of ['sender', 'recipient']) { await page.locator('#' + kind + '-name').fill('Synthetic contact'); await page.locator('#' + kind + '-phone').fill('+84901234567'); await page.locator('#' + kind + '-administrativeArea').fill('Synthetic area'); await page.locator('#' + kind + '-locality').fill('Synthetic locality'); await page.locator('#' + kind + '-addressLine').fill('Synthetic street'); }
  await page.locator('#parcel-description-0').fill('Synthetic goods'); await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click(); await expect(page.locator('#pilot-destination')).toHaveValue('SIM-B1'); await expect(page.locator('.pb-total')).toHaveCount(0); expect(writes).toBe(1); await expect(page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true })).toBeDisabled();
});
test('tampered navigation money or quote metadata cannot become a preparation input', async ({ page, context }) => {
  await fixture(context); await page.goto('/personal/create'); await page.evaluate(() => { history.replaceState({ ...history.state, usr: { shippingPreparation: { source: 'CATALOG_ESTIMATE_1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', parcels: [], totalVnd: 1, quoteId: 'fake' } } }, ''); }); await page.reload(); await expect(page.getByRole('alert')).toContainText('Nguồn thông tin tạo đơn không hợp lệ'); await expect(page.locator('#sender-name')).toHaveCount(0);
});
test('label PDF and ZIP use authorized bytes, no financial or Carrier writes; documents retain return context', async ({ page, context }) => {
  const { calls } = await fixture(context); await page.goto('/personal/labels'); await expect(page.locator('.st-label-list tbody tr')).toHaveCount(2);
  await expect(page.locator('.st-label-list tbody')).toContainText('Có thể có trang tiếp nối'); await expect(page.locator('.st-label-list tbody')).not.toContainText('trang /');
  await page.getByLabel('Chọn tất cả nhãn trên trang').check(); const download = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click(); expect((await download).suggestedFilename()).toBe('argopost-private-labels.zip');
  await page.getByRole('button', { name: 'Xem / In nhãn', exact: true }).first().click(); await expect(page.locator('.st-label-preview')).toContainText('chọn đủ các trang mang cùng mã Parcel'); await page.getByRole('button', { name: 'Tải / In nhãn', exact: true }).click(); await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toHaveAttribute('href', /^blob:/);
  await page.locator('.st-label-list').getByRole('link', { name: 'Chứng từ', exact: true }).first().click(); await expect(page.getByRole('tab', { name: 'Chứng từ', exact: true })).toHaveAttribute('aria-selected', 'true'); await page.getByRole('link', { name: 'Danh sách nhãn', exact: true }).click(); await expect(page).toHaveURL(/\/personal\/labels$/);
  expect(calls.filter(c => c.method !== 'GET' && !c.url.pathname.endsWith('/refresh'))).toHaveLength(0);
});
test('private PDF denial never exposes a prepared link or a partial ZIP', async ({ page, context }) => {
  await fixture(context, { deniedPdf: true }); await page.goto('/personal/labels'); await page.getByLabel('Chọn tất cả nhãn trên trang').check(); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa tạo ZIP');
  await page.getByRole('button', { name: 'Xem / In nhãn', exact: true }).first().click(); await page.getByRole('button', { name: 'Tải / In nhãn', exact: true }).click(); await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toHaveCount(0);
});
test('labels pagination keeps search and never includes Draft or fabricated receipt/POD', async ({ page, context }) => {
  await fixture(context, { count: 25 }); await page.goto('/personal/labels?search=AP-SIM-&page=2'); await expect(page.getByRole('heading', { name: 'Trang này không có nhãn' })).toBeVisible(); await page.getByRole('button', { name: 'Về trang đầu, giữ bộ lọc' }).click(); await expect(page.locator('.st-label-list tbody tr')).toHaveCount(20);
  await page.getByText('Biên nhận bàn giao & Proof of Delivery', { exact: true }).click(); await expect(page.locator('.st-document-scope')).toContainText('Chưa có chứng từ'); await expect(page.getByRole('button', { name: /Tạo mã|Xác nhận bàn giao|Tải POD/ })).toHaveCount(0);
});
for (const width of [1440, 390]) test(`tools stay within ${width}px and retain native keyboard-readable forms`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 });
  for (const [path, name] of [['/personal/lookup?view=estimate', 'estimate'], ['/personal/lookup?view=coverage', 'coverage'], ['/personal/labels', 'labels']]) { await page.goto(path); await expect(page.getByRole('heading', { level: 1 })).toBeVisible(); if (name === 'labels') await expect(page.locator('.st-label-list tbody tr')).toHaveCount(2); else await expect(page.locator('.st-catalog,.st-estimate').first()).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: `../../../.demo/lookup-label-review/${name}-${width}.png`, fullPage: true, animations: 'disabled' }); }
});
