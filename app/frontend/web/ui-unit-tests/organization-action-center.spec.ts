import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { businessContextsFixture, organizationA, organizationB } from './support/business-fixture';
const account = '10000000-0000-4000-8000-000000000001', draftId = '10000000-0000-4000-8000-000000000002', shipmentId = '10000000-0000-4000-8000-000000000003', quoteId = '10000000-0000-4000-8000-000000000004', parcelId = '10000000-0000-4000-8000-000000000005';
const base = '/merchant/' + organizationA, contexts = businessContextsFixture.map(c => ({ ...c, permissions: ['dashboard.read', 'shipment.draft.read', 'shipment.draft.write'] }));
const contact = { name: 'Synthetic work contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
const parcel = { description: 'Synthetic parcel', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }, input = { reference: 'SYNTHETIC-WORK', instructions: null, sender: contact, recipient: contact, parcels: [parcel] };
function preparation(org = organizationA, editable = true) { return { id: draftId, organizationId: org, facilityId: null, creatorAccountId: account, state: 'DRAFT', preparationState: 'DRAFT', version: 1, input, sourceChannel: null, sourceOrderReference: null, createdAt: '2026-11-15T00:00:00Z', updatedAt: '2026-11-15T00:00:00Z', allowedActions: editable ? ['UPDATE', 'ARCHIVE'] : [] }; }
function management(page: number, org: string) {
  const quote = { id: quoteId, organizationId: org, facilityId: null, creatorAccountId: account, preparationId: draftId, preparationVersion: 1, contractId: parcelId, contractVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', route: { datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA', dropOffCode: 'SIM-DROP-A1' }, parcels: [{ weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100 }], feeLines: [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }], totalVnd: 25000, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE', createdAt: '2026-11-15T00:00:00Z', expiresAt: '2026-11-15T00:15:00Z', consumedBy: shipmentId };
  const record = { simulation: true, shipment: { id: shipmentId, organizationId: org, facilityId: null, creatorAccountId: account, code: 'AP-SIM-' + shipmentId.replaceAll('-', ''), preparationId: draftId, preparationVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input, quote, totalVnd: 25000, createdAt: quote.createdAt, parcels: [{ id: parcelId, parcelNumber: 1, facts: parcel }] }, obligation: { id: parcelId, organizationId: org, facilityId: null, shipmentId, quoteId, amountVnd: 25000, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-BUSINESS-LOCAL-01', createdAt: quote.createdAt } };
  return { schemaVersion: 'ORGANIZATION_MANAGEMENT_1', generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', summary: { bookedShipments: '1', waitingForHandover: '1', unassigned: '1', confirmedFeesVnd: '25000', unpaidFeesVnd: '25000' }, total: '1', page, size: 20, hasNext: false, items: page ? [] : [{ record, status: 'WAITING_FOR_HANDOVER', source: { sourceChannel: null, sourceOrderReference: null, facilityId: null, importJobIds: [] }, assignment: { shipmentId, organizationId: org, assigneeAccountId: null, version: 0, updatedAt: null }, allowedActions: ['ASSIGN'] }] };
}
async function fixture(context: BrowserContext, { editable = true, denySecond = false, foreign = false } = {}) {
  const calls: { path: string; query: URLSearchParams; method: string }[] = [];
  await context.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url()), path = url.pathname, query = url.searchParams;
    const reply = (body: unknown, status = 200) => route.fulfill({ status, headers: { 'Cache-Control': 'no-store' }, contentType: 'application/json', body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' }); if (path.endsWith('/refresh')) return reply({ accountId: account, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() }); if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: contexts });
    calls.push({ path, query, method: route.request().method() }); const org = path.includes(organizationB) ? organizationB : organizationA, page = Number(query.get('page') ?? 0);
    if (path.endsWith('/shipment-preparations')) return denySecond && page ? reply({ code: 'RESOURCE_NOT_FOUND' }, 404) : reply({ items: page ? [] : [preparation(foreign ? organizationB : org, editable)], page, size: 20, hasNext: page === 0 });
    if (path.endsWith('/shipments') && query.get('view') === 'management') return reply(management(page, org));
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { calls };
}

test('Action Center is one Management child with paged Draft work and correct source labels', async ({ page, context }, info) => {
  const api = await fixture(context); await page.goto(base + '/shipments?category=ACTION'); await expect(page.getByRole('heading', { name: 'Đơn cần xử lý', exact: true })).toBeVisible(); await expect(page.locator('.oa-table-scroll')).toContainText('SYNTHETIC-WORK'); await expect(page.locator('.oa-count')).toHaveText('1 bản trên trang');
  const menu = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true }); await expect(menu.getByRole('link', { name: 'Đơn cần xử lý', exact: true })).toHaveAttribute('aria-current', 'page'); await expect(menu.getByRole('link', { name: 'Quản lý vận đơn', exact: true })).not.toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('link', { name: 'Tiếp tục bản nháp', exact: true })).toHaveAttribute('href', base + '/create?draft=' + draftId); await page.screenshot({ path: info.outputPath('action-desktop-synthetic.png'), fullPage: true, animations: 'disabled' }); await page.setViewportSize({ width: 390, height: 844 }); await expect(page.locator('.pw-sidebar')).toBeHidden(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true); await page.screenshot({ path: info.outputPath('action-mobile-synthetic.png'), fullPage: true, animations: 'disabled' }); await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.locator('.oa-empty h3')).toHaveText('Không có bản nháp trên trang này'); expect(api.calls.filter(c => c.path.endsWith('/shipment-preparations')).map(c => c.query.get('page'))).toEqual(['0', '1']); expect(api.calls.every(c => c.method === 'GET')).toBe(true);
});

test('unassigned Bookings preserve management scope and detail return without creating Carrier incidents', async ({ page, context }) => {
  const api = await fixture(context); await page.goto(base + '/shipments?category=ACTION&sourceChannel=WEB&from=2026-11-01&until=2026-11-15'); await expect(page.getByRole('heading', { name: 'Vận đơn chưa phân công nội bộ', exact: true })).toBeVisible(); await expect(page.locator('.oa-count')).toHaveText('1 vận đơn phù hợp');
  const call = api.calls.filter(c => c.path.endsWith('/shipments')).at(-1)!; expect(call.query.get('assignee')).toBe('NONE'); expect(call.query.get('sourceChannel')).toBe('WEB'); expect(call.query.get('from')).toBe('2026-11-01'); const href = await page.getByRole('link', { name: 'Xem & phân công', exact: true }).getAttribute('href'); expect(new URL(href!, 'https://local.invalid').searchParams.get('returnTo')).toBe(base + '/shipments?category=ACTION&sourceChannel=WEB&from=2026-11-01&until=2026-11-15&assignee=NONE');
  const before = api.calls.length; await page.getByRole('button', { name: 'Yêu cầu từ Carrier', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chưa có yêu cầu phản hồi từ Carrier', exact: true })).toBeVisible(); expect(api.calls).toHaveLength(before); await expect(page.locator('.oa-count')).toHaveCount(0); await expect(page.getByRole('button', { name: /Xác nhận đã giao|Thu tiền|Phê duyệt bồi thường/ })).toHaveCount(0);
});

test('Drafts cannot silently broaden an inherited warehouse filter and read-only rows cannot edit', async ({ page, context }) => {
  const api = await fixture(context, { editable: false }); await page.goto(base + '/shipments?category=ACTION&facilityId=' + account); await page.getByRole('button', { name: 'Hoàn tất bản nháp', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Nguồn có bộ lọc riêng', exact: true })).toBeVisible(); expect(api.calls.some(c => c.path.endsWith('/shipment-preparations'))).toBe(false);
  await page.getByRole('button', { name: 'Xóa bộ lọc vận đơn để xem bản nháp', exact: true }).click(); await expect(page.getByRole('link', { name: 'Xem bản nháp', exact: true })).toBeVisible(); await expect(page.getByRole('link', { name: 'Tiếp tục bản nháp', exact: true })).toHaveCount(0); expect(new URL(page.url()).searchParams.has('facilityId')).toBe(false);
});

test('revoked page and foreign organization response clear previous work instead of reporting zero', async ({ page, context }) => {
  await fixture(context, { denySecond: true }); await page.goto(base + '/shipments?category=ACTION'); await expect(page.locator('.oa-table-scroll tbody tr')).toHaveCount(1); await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('quyền theo kho'); await expect(page.locator('.oa-table-scroll')).toHaveCount(0);
});

test('foreign Draft cannot appear and internal queue is responsive without a parallel Personal source', async ({ page, context }) => {
  const api = await fixture(context, { foreign: true }); await page.setViewportSize({ width: 390, height: 844 }); await page.goto(base + '/shipments?category=ACTION'); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.locator('.oa-table-scroll')).toHaveCount(0); expect(api.calls.some(c => c.path.includes('/customer/'))).toBe(false); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
});
