import { test, expect } from '@playwright/test';
import type { BrowserContext, Page } from '@playwright/test';
import { managementFixture } from './support/management-fixture';
import { analyticsFixture, shippingReportsFixture } from './support/analytics-fixture';

// Explicit intercepted UI fixtures only; real PostgreSQL/browser receipts are a separate required gate.
const owner = '10000000-0000-4000-8000-000000000001', draftId = '10000000-0000-4000-8000-000000000002', quoteId = '10000000-0000-4000-8000-000000000003', shipmentId = '10000000-0000-4000-8000-000000000004';
async function pilotFixture(context: BrowserContext, expiresSoon = false) {
  const now = Date.now(), created = now - (expiresSoon ? 898000 : 0), contact = { name: 'Synthetic contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } }, parcel = { description: 'Ordinary synthetic box', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 };
  const route = { datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA', dropOffCode: 'SIM-DROP-A1' }, input = { reference: 'Synthetic UI draft', instructions: null, sender: contact, recipient: contact, parcels: [parcel] };
  const quote = { id: quoteId, accountId: owner, draftId, draftVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', route, parcels: [{ weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100 }], feeLines: [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }], totalVnd: 25000, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE', createdAt: new Date(created).toISOString(), expiresAt: new Date(created + 900000).toISOString(), consumedBy: null };
  const shipment = { id: shipmentId, accountId: owner, code: 'AP-SIM-' + shipmentId.replaceAll('-', ''), draftId, draftVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input, quote: { ...quote, consumedBy: shipmentId }, totalVnd: 25000, createdAt: new Date(now).toISOString(), parcels: [{ id: quoteId, parcelNumber: 1, facts: parcel }] }, obligation = { id: quoteId, accountId: owner, shipmentId, quoteId, amountVnd: 25000, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-LOCAL-PILOT-01', createdAt: new Date(now).toISOString() };
  const calls: { path: string; method: string; key?: string }[] = [];
  await context.route('**/api/v1/**', async request => {
    const path = new URL(request.request().url()).pathname, method = request.request().method(); calls.push({ path, method, key: request.request().headers()['idempotency-key'] }); const reply = (value: unknown, status = 200, headers: Record<string, string> = {}) => request.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (path.endsWith('/refresh')) return reply({ accountId: owner, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: owner, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-challenge' });
    if (path.endsWith('/addresses')) return reply({ items: [], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/shipping-catalog')) return reply({ simulation: true, serviceCode: 'ROAD_STANDARD', quoteEnabled: true, bookingEnabled: true, coverage: { datasetVersion: 'ARGO-SIM-COVERAGE-1', namespace: 'ARGO_SIMULATED_ADMIN_1', provenance: 'SIMULATION', effectiveFrom: new Date(created).toISOString(), areas: [{ code: 'SIM-A1', region: 'SIM-REGION-A', label: 'Khu mô phỏng A1', dropOffCode: 'SIM-DROP-A1' }, { code: 'SIM-A2', region: 'SIM-REGION-A', label: 'Khu mô phỏng A2', dropOffCode: 'SIM-DROP-A2' }], routes: [route] }, tariff: { version: 'ARGO-ROAD-TARIFF-1', intraFirstVnd: 25000, intraStepVnd: 3000, interFirstVnd: 38000, interStepVnd: 5000, stepGrams: 500, volumetricDivisor: 5000, maxParcels: 5, maxActualGrams: 20000, maxDimensionMm: 1000, maxGirthMm: 2000, ttlSeconds: 900, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' }, paymentMethod: 'CASH_ON_HANDOVER', payer: 'SENDER', paymentCollectionEnabled: false });
    if (path.endsWith('/personal-shipping-reports')) return reply(shippingReportsFixture(new URL(request.request().url()).searchParams, 1));
    if (path.endsWith('/personal-analytics')) return reply(analyticsFixture(new URL(request.request().url()).searchParams, 1));
    if (path === '/api/v1/customer/shipments') return reply(new URL(request.request().url()).searchParams.get('view') === 'management' ? managementFixture(new URL(request.request().url()).searchParams, 1, [{ simulation: true, shipment, obligation }]) : { items: [{ simulation: true, shipment, obligation }], page: 0, size: 20, hasNext: false });
    if (path === '/api/v1/customer/shipment-drafts') return reply({ items: [], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/quotes')) return reply({ quote, usable: true, serverTime: new Date(now).toISOString() }, 201);
    if (path.endsWith('/bookings')) return request.abort('failed');
    if (path.includes('/booking-receipts/')) return reply({ targetId: shipmentId, outcome: 'COMMITTED' });
    if (path === '/api/v1/customer/shipments/' + shipmentId) return reply({ simulation: true, shipment, obligation }, 200, { ETag: '"v1"' });
    if (path === '/api/v1/customer/shipment-drafts/' + draftId) return reply({ id: draftId, accountId: owner, ownerType: 'PERSONAL', state: 'DRAFT', input, version: 1, createdAt: new Date(now).toISOString(), updatedAt: new Date(now).toISOString() }, 200, { ETag: '"v1"' });
    return reply({ code: 'ISOLATED_UI_ROUTE_UNSUPPORTED' }, 503);
  });
  return calls;
}
for (const viewport of [{ width: 1280, height: 900 }, { width: 390, height: 844 }]) {
  test(`Personal pages omit the environment banner at ${viewport.width}px while retaining business state`, async ({ page, context }) => {
    await pilotFixture(context); await page.setViewportSize(viewport);
    const pages = [
      ['/personal/create', 'Tạo đơn'],
      ['/personal/create?draft=' + draftId, 'Tiếp tục bản nháp'],
      ['/personal/shipments', 'Quản lý vận đơn'],
      ['/personal/shipments?tab=drafts', 'Đơn nháp'],
      ['/personal/lookup', 'Tra cứu & Báo giá'],
      ['/personal/billing', 'Cước & Thanh toán'],
      ['/personal/drafts/' + draftId, 'Chi tiết bản nháp'],
      ['/personal/shipments/' + shipmentId, 'Chi tiết vận đơn'],
    ];
    for (const [path, heading] of pages) {
      await page.goto(path);
      await expect(page.getByRole('heading', { name: heading, exact: true })).toBeVisible();
      await expect(page.getByText('ArgoPost Local/Capstone Pilot · Mô phỏng', { exact: true })).toHaveCount(0);
      await expect(page.locator('.sd-policy')).toHaveCount(0);
      if (path === '/personal/shipments') {
        await expect(page.locator('.pm-table')).toContainText('UNPAID · Chưa thu tiền');
      }
      if (path === '/personal/shipments/' + shipmentId) await expect(page.locator('.pb-unpaid')).toHaveText('UNPAID');
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      if (path === '/personal/create') {
        await expect(page.getByRole('button', { name: 'Làm mới', exact: true })).toBeVisible();
        await page.screenshot({ path: `../../../.demo/argopost-create-${viewport.width}.png`, fullPage: true, animations: 'disabled' });
      }
    }
    await page.screenshot({ path: `../../../.demo/personal-no-pilot-banner-${viewport.width}.png`, fullPage: true, animations: 'disabled' });
  });
}
test('pilot UI recovers an unknown booking once and never represents UNPAID as collected', async ({ page, context }) => {
  const calls = await pilotFixture(context); await page.goto('/personal/drafts/' + draftId); await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check(); await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.locator('.pb-total')).toContainText('25.000 VND'); await page.screenshot({ path: '../../../.demo/pilot-booking-desktop.png', fullPage: true, animations: 'disabled' }); await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true })).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra kết quả booking', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn', exact: true })).toBeVisible(); await page.getByRole('tab', { name: 'Cước & COD', exact: true }).click(); await expect(page.getByRole('tabpanel')).toContainText('UNPAID'); await expect(page.getByRole('button', { name: 'Thanh toán', exact: true })).toHaveCount(0); expect(calls.filter(v => v.path.endsWith('/bookings') && v.method === 'POST')).toHaveLength(1); expect(calls.filter(v => v.path.endsWith('/quotes') || v.path.endsWith('/bookings')).every(v => !!v.key)).toBe(true); await page.setViewportSize({ width: 390, height: 844 }); await expect(page.locator('#personal-sidebar')).not.toBeVisible(); await page.screenshot({ path: '../../../.demo/pilot-shipment-mobile.png', fullPage: true, animations: 'disabled' }); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
test('pilot quote countdown expires without sending a booking or extending the quote', async ({ page, context }) => {
  const calls = await pilotFixture(context, true); await page.clock.install(); await page.goto('/personal/drafts/' + draftId); await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check(); await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.getByRole('timer')).toBeVisible(); await page.clock.runFor(3000); await expect(page.getByRole('timer')).toContainText('không còn hiệu lực'); await expect(page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true })).toBeDisabled(); expect(calls.filter(v => v.path.endsWith('/bookings'))).toHaveLength(0); expect(calls.filter(v => v.path.endsWith('/quotes') && v.method === 'POST')).toHaveLength(1);
});

test('entry reviews a saved snapshot before quote and locks editing through an unknown booking', async ({ page, context }) => {
  const calls = await pilotFixture(context); await page.goto('/personal/create?draft=' + draftId);
  await expect(page.locator('#lengthMm0')).toHaveValue('10');
  await expect(page.locator('.se-action-bar')).toContainText('Chưa có báo giá');
  await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Xác nhận thông tin & báo giá', exact: true })).toBeFocused();
  await expect(page.locator('#sender-name')).toHaveCount(0);
  expect(calls.filter(c => c.method !== 'GET' && !c.path.endsWith('/refresh'))).toHaveLength(0);
  await expect(page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true })).toBeDisabled();
  await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check();
  await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click();
  await expect(page.locator('.pb-total')).toContainText('25.000 VND');
  await page.screenshot({ path: '../../../.demo/argopost-create-review.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click();
  await expect(page.getByRole('button', { name: '← Chỉnh sửa thông tin', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả booking', exact: true }).click();
  await expect(page.locator('.pb-shipment-heading')).toContainText('UNPAID');
  expect(calls.filter(c => c.path.endsWith('/bookings') && c.method === 'POST')).toHaveLength(1);
});

test('editing after review retires its quote and preserves integer facts on a stale save', async ({ page, context }) => {
  await pilotFixture(context); let writes = 0;
  await context.route('**/api/v1/customer/shipment-drafts/' + draftId, async route => {
    if (route.request().method() !== 'PUT') return route.fallback();
    writes++; const input = route.request().postDataJSON(); expect(input.parcels[0].lengthMm).toBe(123); expect(input.parcels[0].quantity).toBe(3);
    expect(route.request().headers()['if-match']).toBe('"v1"'); expect(route.request().headers()['idempotency-key']).toBeTruthy();
    return route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ code: 'PRECONDITION_STALE' }) });
  });
  await page.goto('/personal/create?draft=' + draftId);
  await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check();
  await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.locator('.pb-total')).toBeVisible();
  await page.getByRole('button', { name: '← Chỉnh sửa thông tin', exact: true }).click();
  await expect(page.locator('.pb-total')).toHaveCount(0);
  await page.locator('#lengthMm0').fill('12.3'); await page.locator('#quantity0').fill('3');
  await expect(page.getByLabel('Tổng thông tin kiện')).toContainText('500 g');
  await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Bản nháp đã thay đổi');
  await expect(page.locator('#lengthMm0')).toHaveValue('12.3'); await expect(page.locator('#sender-name')).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true })).toHaveCount(0); expect(writes).toBe(1);
});

async function fillNewEntry(page: Page) {
  for (const kind of ['sender', 'recipient']) for (const [field, value] of Object.entries({ name: 'Synthetic ' + kind, phone: '+84901234567', addressLine: 'Synthetic street', administrativeArea: 'Synthetic area', locality: 'Synthetic locality' })) await page.locator('#' + kind + '-' + field).fill(value);
  await page.locator('#parcel-description-0').fill('Synthetic goods');
  for (const [field, value] of Object.entries({ weightGrams: '500', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + field + '0').fill(value);
}
test('lost entry save recovers its exact receipt into review without a second POST', async ({ page, context }) => {
  await pilotFixture(context); let writes = 0, stored: unknown;
  const record = () => ({ id: draftId, accountId: owner, ownerType: 'PERSONAL', state: 'DRAFT', input: stored, version: 1, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z' });
  await context.route('**/api/v1/customer/shipment-drafts', async route => { writes++; stored = route.request().postDataJSON(); return route.abort('failed'); });
  await context.route('**/api/v1/customer/shipment-draft-receipts/**', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ operationId: 'createShippingDraft', targetId: draftId, outcome: 'COMMITTED', version: 1 }) }));
  await context.route('**/api/v1/customer/shipment-drafts/' + draftId, route => route.fulfill({ headers: { ETag: '"v1"' }, contentType: 'application/json', body: JSON.stringify(record()) }));
  await page.goto('/personal/create'); await fillNewEntry(page);
  await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.locator('#sender-name')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Làm mới', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Xác nhận thông tin & báo giá', exact: true })).toBeVisible();
  await expect(page.locator('.se-review-contact').first()).toContainText('Synthetic sender'); expect(writes).toBe(1);
});
test('entry reset asks before dropping unsaved fields and never mutates server data', async ({ page, context }) => {
  const calls = await pilotFixture(context); await page.goto('/personal/create'); await page.locator('#sender-name').fill('Synthetic unsaved');
  page.once('dialog', dialog => dialog.dismiss()); await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.locator('#sender-name')).toHaveValue('Synthetic unsaved');
  page.once('dialog', dialog => dialog.accept()); await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.locator('#sender-name')).toHaveValue('');
  expect(calls.filter(c => c.method === 'POST' && !c.path.endsWith('/refresh'))).toHaveLength(0);
});


test('new parcel measurements are empty and cannot create a draft from guessed weights or dimensions', async ({ page, context }) => {
  const calls = await pilotFixture(context); await page.goto('/personal/create');
  for (const field of ['weightGrams', 'lengthMm', 'widthMm', 'heightMm']) await expect(page.locator('#' + field + '0')).toHaveValue('');
  await page.getByRole('button', { name: '+ Thêm kiện (tối đa 5)', exact: true }).click();
  for (const field of ['weightGrams', 'lengthMm', 'widthMm', 'heightMm']) await expect(page.locator('#' + field + '1')).toHaveValue('');
  await page.getByRole('button', { name: 'Lưu bản nháp', exact: true }).click();
  expect(calls.filter(c => c.method === 'POST' && c.path.endsWith('/shipment-drafts'))).toHaveLength(0);
  await expect(page.getByLabel('Tổng thông tin kiện')).toContainText('— g');
});

test('default contact is explicitly copied across address pages and never overwrites edits on initial load', async ({ page, context }) => {
  await pilotFixture(context); const queries: string[] = [];
  const contact = { id: quoteId, accountId: owner, kind: 'SENDER', label: 'Default source', contactName: 'Synthetic default sender', contactPhone: '+84901234567', address: { country: 'VN', administrativeArea: 'Area', locality: 'Locality', addressLine: 'Default street' }, isDefault: true, verificationStatus: 'UNVERIFIED', version: 1, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z' };
  await context.route('**/api/v1/customer/addresses?**', route => {
    const q = new URL(route.request().url()).searchParams; queries.push(q.toString()); const index = Number(q.get('page'));
    return route.fulfill({ contentType: 'application/json', body: JSON.stringify({ items: q.get('kind') === 'SENDER' && index === 1 ? [contact] : [], page: index, size: 20, hasNext: q.get('kind') === 'SENDER' && index === 0 }) });
  });
  await page.goto('/personal/create'); await page.locator('#sender-name').fill('Manual sender');
  await expect(page.locator('#sender-book')).toBeVisible(); await expect(page.locator('#sender-name')).toHaveValue('Manual sender');
  await page.getByRole('button', { name: 'Dùng địa chỉ mặc định', exact: true }).first().click();
  await expect(page.locator('#sender-name')).toHaveValue(contact.contactName); await expect(page.locator('#sender-addressLine')).toHaveValue(contact.address.addressLine);
  expect(queries.some(q => q.includes('page=1') && q.includes('kind=SENDER'))).toBe(true);
  await page.locator('#sender-name').fill('Edited snapshot'); await expect(page.locator('#sender-name')).toHaveValue('Edited snapshot');
  await page.getByRole('button', { name: 'Dùng địa chỉ mặc định', exact: true }).nth(1).click();
  await expect(page.getByRole('status')).toContainText(['Đã điền địa chỉ mặc định', 'Chưa có địa chỉ mặc định']);
  await expect(page.locator('#recipient-name')).toHaveValue('');
});

test('catalog limits identify invalid saved parcels and block quote without mutating the draft', async ({ page, context }) => {
  const calls = await pilotFixture(context);
  await context.route('**/api/v1/customer/shipment-drafts/' + draftId, async route => {
    // Use the established synthetic contact contract, not a real account or address.
    const contact = { name: 'Synthetic boundary', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Area', locality: 'Locality', addressLine: 'Street' } };
    return route.fulfill({ headers: { ETag: '"v1"' }, contentType: 'application/json', body: JSON.stringify({ id: draftId, accountId: owner, ownerType: 'PERSONAL', state: 'DRAFT', input: { sender: contact, recipient: contact, parcels: [{ description: 'Oversize synthetic parcel', quantity: 1, weightGrams: 20001, lengthMm: 1001, widthMm: 500, heightMm: 500, declaredValueVnd: 0 }] }, version: 1, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() }) });
  });
  await page.goto('/personal/create?draft=' + draftId); await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Kiện 1: trọng lượng thực vượt 20 kg.');
  await expect(page.getByRole('alert')).toContainText('mỗi chiều không được vượt 100 cm');
  await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check();
  await expect(page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true })).toBeDisabled();
  expect(calls.filter(c => c.path.endsWith('/quotes') || c.method === 'PUT')).toHaveLength(0);
  await page.getByRole('button', { name: '← Chỉnh sửa thông tin', exact: true }).click();
  await expect(page.locator('#weightGrams0')).toHaveValue('20001');
});

for (const width of [1440, 390]) test(`complete saved review and quote facts stay readable at ${width}px`, async ({ page, context }) => {
  await pilotFixture(context); await page.setViewportSize({ width, height: 960 }); await page.goto('/personal/create?draft=' + draftId);
  await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click();
  await expect(page.getByLabel('Kiện hàng đã lưu')).toContainText('Ordinary synthetic box');
  await expect(page.getByLabel('Kiện hàng đã lưu')).toContainText('10 × 10 × 10 cm');
  await expect(page.locator('.se-review-note')).toContainText('Synthetic UI draft');
  await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check();
  await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click();
  await expect(page.locator('.pb-fee-line')).toContainText('Thực 0.5 kg');
  await expect(page.locator('.pb-fee-line')).toContainText('Cước đầu: 25.000 VND');
  await expect(page.locator('.pb-policy-summary')).toContainText('Chưa có cam kết thời gian giao');
  await expect(page.locator('.pb-policy-summary')).toContainText('Không áp dụng');
  await expect(page.locator('.pb-quote')).toContainText('chờ bàn giao · UNPAID, chưa thu tiền');
  await expect(page.getByRole('button', { name: 'Thanh toán', exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: `../../../.demo/create-shipment-review/review-${width}.png`, fullPage: true, animations: 'disabled' });
});


test('default address network failure keeps current input and locks save during lookup', async ({ page, context }) => {
  const calls = await pilotFixture(context); await page.goto('/personal/create'); await fillNewEntry(page);
  let release!: () => void; const gate = new Promise<void>(resolve => { release = resolve; });
  await context.route('**/api/v1/customer/addresses?**', async route => { await gate; return route.abort('failed'); });
  await page.getByRole('button', { name: 'Dùng địa chỉ mặc định', exact: true }).first().click();
  await expect(page.getByRole('button', { name: 'Lưu bản nháp', exact: true })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true })).toBeDisabled();
  await expect(page.locator('#sender-name')).toBeDisabled(); release();
  await expect(page.getByText('Không tải được địa chỉ mặc định. Nội dung hiện tại được giữ nguyên.', { exact: true })).toBeVisible();
  await expect(page.locator('#sender-name')).toHaveValue('Synthetic sender'); await expect(page.locator('#sender-name')).toBeEnabled();
  expect(calls.filter(c => c.path.endsWith('/shipment-drafts') && c.method === 'POST')).toHaveLength(0);
});
