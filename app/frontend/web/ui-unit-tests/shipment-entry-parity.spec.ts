import { test, expect } from '@playwright/test';
import type { BrowserContext, Page } from '@playwright/test';
import { businessContextsFixture, organizationA } from './support/business-fixture';
const account = '10000000-0000-4000-8000-000000000001', id = '10000000-0000-4000-8000-000000000002';
async function fixture(context: BrowserContext, lost = false, writable = true, shipping = false) {
  let input: Record<string, unknown>, version = 1, posts = 0; const requests: string[] = [];
  const normalized = () => { const result = { sender: input.sender, recipient: input.recipient, parcels: input.parcels, reference: input.reference ?? null, instructions: input.instructions ?? null }; for (const kind of ['sender', 'recipient'] as const) { const contact = result[kind] as { phone: string }; result[kind] = { ...contact, phone: contact.phone.startsWith('0') ? '+84' + contact.phone.slice(1) : contact.phone.startsWith('84') ? '+' + contact.phone : contact.phone }; } return result; };
  const business = () => ({ id, organizationId: organizationA, facilityId: null, creatorAccountId: account, state: 'DRAFT', preparationState: 'DRAFT', version, input: normalized(), sourceChannel: input.sourceChannel ?? null, sourceOrderReference: input.sourceOrderReference ?? null, createdAt: '2026-11-13T01:00:00Z', updatedAt: '2026-11-13T01:00:00Z', allowedActions: writable ? ['UPDATE', 'ARCHIVE'] : [] });
  await context.route('**/api/v1/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname; requests.push(req.method() + ' ' + path);
    const reply = (body: unknown, status = 200, etag?: string) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: account, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [{ ...businessContextsFixture[0], permissions: [...businessContextsFixture[0].permissions, 'shipment.draft.read', ...(writable ? ['shipment.draft.write', 'shipment.import'] : []), ...(shipping ? ['shipment.quote', 'shipment.book', 'shipment.label.read'] : [])] }] });
    if (path.endsWith('/addresses')) return reply({ items: [], page: 0, size: 20, hasNext: false });
    if (path.includes('/merchant-organizations/') && /shipment-preparations(?:\/[^/]+)?$/.test(path)) {
      if (req.method() === 'POST' || req.method() === 'PUT') { posts++; input = req.postDataJSON(); if (req.method() === 'PUT') version++; if (lost) return route.abort('failed'); return reply(business(), req.method() === 'POST' ? 201 : 200, '"v' + version + '"'); }
      if (input) return reply(business(), 200, '"v' + version + '"');
    }
    if (path.includes('/shipping-preparation-receipts/')) return reply({ operationId: 'CREATE', targetId: id, outcome: 'COMMITTED', version });
    if (path.endsWith('/shipment-drafts') && req.method() === 'POST') { posts++; input = req.postDataJSON(); return reply({ id, accountId: account, ownerType: 'PERSONAL', state: 'DRAFT', input: normalized(), version: 1, createdAt: '2026-11-13T01:00:00Z', updatedAt: '2026-11-13T01:00:00Z' }, 201, '"v1"'); }
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { requests, posts: () => posts, body: () => input };
}

test('Business creation owns manual/import navigation without a separate import menu', async ({ page, context }) => {
  const api = await fixture(context); await page.goto('/merchant/' + organizationA + '/create');
  const menu = page.getByRole('navigation', { name: 'Điều hướng Business', exact: true }), modes = page.getByRole('navigation', { name: 'Cách tạo vận đơn', exact: true });
  await expect(menu.getByRole('link', { name: 'Nhập file', exact: true })).toHaveCount(0);
  await expect(menu.getByRole('link', { name: 'Vận đơn', exact: true })).toHaveCount(0); await expect(menu.getByRole('link', { name: 'Đơn nháp', exact: true })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Vận đơn đã tạo', exact: true })).toHaveAttribute('href', '/merchant/' + organizationA + '/shipments'); await expect(page.getByRole('link', { name: 'Danh sách bản nháp', exact: true })).toHaveAttribute('href', '/merchant/' + organizationA + '/preparations');
  await expect(modes.getByRole('link', { name: 'Tạo thủ công', exact: true })).toHaveAttribute('aria-current', 'page');
  await modes.getByRole('link', { name: 'Nhập CSV/Excel', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Nhập đơn hàng từ file', exact: true })).toBeVisible();
  await expect(menu.getByRole('link', { name: 'Tạo vận đơn', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(modes.getByRole('link', { name: 'Nhập CSV/Excel', exact: true })).toHaveAttribute('aria-current', 'page');
  await modes.getByRole('link', { name: 'Tạo thủ công', exact: true }).click(); await expect(page.locator('#sender-name')).toBeVisible(); expect(api.posts()).toBe(0);
});

test('changing manual creation mode confirms unsaved edits and never saves implicitly', async ({ page, context }) => {
  const api = await fixture(context); await page.goto('/merchant/' + organizationA + '/create'); await page.locator('#sender-name').fill('Synthetic unsaved sender');
  page.once('dialog', async dialog => { expect(dialog.type()).toBe('confirm'); await dialog.dismiss(); });
  await page.getByRole('navigation', { name: 'Cách tạo vận đơn', exact: true }).getByRole('link', { name: 'Nhập CSV/Excel', exact: true }).click(); await expect(page.locator('#sender-name')).toHaveValue('Synthetic unsaved sender');
  page.once('dialog', dialog => dialog.accept()); await page.getByRole('link', { name: 'Nhập CSV/Excel', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Nhập đơn hàng từ file', exact: true })).toBeVisible(); expect(api.posts()).toBe(0);
});
async function fill(page: Page) {
  for (const kind of ['sender', 'recipient']) { await page.locator('#' + kind + '-name').fill('Synthetic ' + kind); await page.locator('#' + kind + '-phone').fill('0901234567'); await page.locator('#' + kind + '-addressLine').fill('Synthetic street'); await page.locator('#' + kind + '-administrativeArea').fill('Synthetic area'); await page.locator('#' + kind + '-locality').fill('Synthetic locality'); }
  await page.locator('#parcel-description-0').fill('Synthetic parcel'); await page.locator('#weightGrams0').fill('500'); for (const key of ['lengthMm', 'widthMm', 'heightMm']) await page.locator('#' + key + '0').fill('10.1'); await page.locator('#draft-reference').fill('SYNTHETIC-REF');
}
test('both workspaces share measured fields, units and saved snapshot without crossing transport ownership', async ({ page, context }) => {
  const api = await fixture(context);
  for (const url of ['/personal/create', '/merchant/' + organizationA + '/create']) {
    const business = url.startsWith('/merchant'); await page.goto(url); await expect(page.getByRole('heading', { name: business ? 'Tạo đơn doanh nghiệp' : 'Tạo đơn', exact: true })).toBeVisible();
    await expect(page.locator('#weightGrams0')).toHaveValue(''); await expect(page.locator('#lengthMm0')).toHaveValue(''); await fill(page); await page.locator('#sender-phone').fill('84901234567');
    await expect(page.getByRole('heading', { name: 'Thông tin hàng hóa', exact: true })).toBeVisible(); await expect(page.getByRole('heading', { name: 'Dịch vụ vận chuyển', exact: true })).toBeVisible(); await expect(page.getByRole('heading', { name: 'Cước & thanh toán', exact: true })).toBeVisible();
    const before = api.posts(); await page.getByRole('button', { name: business ? 'Xem lại thông tin' : 'Tiếp tục báo giá →', exact: true }).click(); await expect(page.getByRole('heading', { name: business ? 'Xác nhận thông tin doanh nghiệp' : 'Xác nhận thông tin & báo giá', exact: true })).toBeVisible();
    await expect(page.getByLabel('Kiện hàng đã lưu')).toContainText('10,1 × 10,1 × 10,1 cm'); expect(api.posts()).toBe(before + 1); expect((api.body().parcels as { lengthMm: number }[])[0].lengthMm).toBe(101);
    expect(Object.keys(api.body())).not.toContain('organizationId'); expect(Object.keys(api.body())).not.toContain('accountId');
    if (business) { await expect(page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true })).toHaveCount(0); await expect(page.getByText('Synthetic Store A', { exact: true }).last()).toBeVisible(); }
  }
  expect(api.requests.filter(r => r.startsWith('POST') && !r.endsWith('/customer-auth/refresh'))).toEqual(['POST /api/v1/customer/shipment-drafts', 'POST /api/v1/merchant-organizations/' + organizationA + '/shipment-preparations']);
});
test('creation mode cannot abandon an unknown write or expose import without permission', async ({ page, context }) => {
  const api = await fixture(context, true); await page.goto('/merchant/' + organizationA + '/create'); await fill(page);
  await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click();
  const modes = page.getByRole('navigation', { name: 'Cách tạo vận đơn', exact: true });
  await expect(page.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true })).toBeVisible(); await expect(modes.getByRole('link', { name: 'Nhập CSV/Excel', exact: true })).toHaveCount(0);
  await expect(modes.locator('[aria-disabled=true]')).toHaveText('Nhập CSV/Excel'); expect(api.posts()).toBe(1);
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Xác nhận thông tin doanh nghiệp' })).toBeVisible();
});

test('read-only creation does not expose an import link', async ({ page, context }) => {
  await fixture(context, false, false); await page.goto('/merchant/' + organizationA + '/create');
  const modes = page.getByRole('navigation', { name: 'Cách tạo vận đơn', exact: true });
  await expect(modes.getByRole('link', { name: 'Nhập CSV/Excel', exact: true })).toHaveCount(0); await expect(modes.locator('[aria-disabled=true]')).toHaveText('Nhập CSV/Excel');
});

test('Merchant source pair is required together, immutable after save, and no Personal address book is loaded', async ({ page, context }) => {
  const api = await fixture(context); await page.goto('/merchant/' + organizationA + '/create'); await fill(page); await page.getByLabel('Nguồn đơn', { exact: true }).fill('WEB');
  await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); expect(api.posts()).toBe(0); expect(await page.getByLabel('Mã đơn nguồn', { exact: true }).evaluate((element: HTMLInputElement) => element.validity.valueMissing)).toBe(true);
  await page.getByLabel('Mã đơn nguồn', { exact: true }).fill('SOURCE-1'); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Xác nhận thông tin doanh nghiệp' })).toBeVisible(); await expect(page.locator('.se-review-grid')).toContainText('SOURCE-1');
  await page.getByRole('button', { name: '← Chỉnh sửa thông tin', exact: true }).click(); await expect(page.getByLabel('Nguồn đơn', { exact: true })).toBeDisabled(); await expect(page.getByLabel('Mã đơn nguồn', { exact: true })).toHaveValue('SOURCE-1'); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Xác nhận thông tin doanh nghiệp' })).toBeVisible(); expect(api.posts()).toBe(1); expect(api.requests.some(r => r.includes('/customer/addresses'))).toBe(false);
});
test('lost Merchant review save resolves through scoped receipt and preserves intent without a second POST', async ({ page, context }) => {
  const api = await fixture(context, true); await page.goto('/merchant/' + organizationA + '/create'); await fill(page); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await expect(page.getByRole('button', { name: 'Lưu bản nháp doanh nghiệp', exact: true })).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Xác nhận thông tin doanh nghiệp' })).toBeVisible(); expect(api.posts()).toBe(1); await page.reload(); await expect(page.getByLabel('Kiện hàng đã lưu')).toContainText('Synthetic parcel');
});
test('Merchant without write rights cannot create or request review', async ({ page, context }) => {
  const api = await fixture(context, false, false); await page.goto('/merchant/' + organizationA + '/create'); await expect(page.getByRole('button', { name: 'Lưu bản nháp doanh nghiệp', exact: true })).toBeDisabled(); await expect(page.getByRole('button', { name: 'Xem lại thông tin', exact: true })).toBeDisabled(); await expect(page.locator('#sender-name')).toBeDisabled(); expect(api.posts()).toBe(0);
});
async function shippingFixture(context: BrowserContext, lostBooking = false, expiresSoon = false) {
  const api = await fixture(context, false, true, true), qid = '10000000-0000-4000-8000-000000000003', sid = '10000000-0000-4000-8000-000000000004', contractId = '10000000-0000-4000-8000-000000000005';
  let quote: Record<string, unknown>, booked = false, bookings = 0, quotes = 0;
  const routeData = { datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA', dropOffCode: 'SIM-DROP-A1' };
  await context.route('**/api/v1/merchant-organizations/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname, now = Date.now(), base = '/api/v1/merchant-organizations/' + organizationA;
    const reply = (body: unknown, status = 200, etag?: string) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(body) });
    if (path.endsWith('/shipping-catalog')) return reply({ simulation: true, serviceCode: 'ROAD_STANDARD', quoteEnabled: true, bookingEnabled: true, coverage: { datasetVersion: 'ARGO-SIM-COVERAGE-1', namespace: 'ARGO_SIMULATED_ADMIN_1', provenance: 'SIMULATION', effectiveFrom: new Date(now).toISOString(), areas: [{ code: 'SIM-A1', region: 'SIM-REGION-A', label: 'Synthetic origin', dropOffCode: 'SIM-DROP-A1' }, { code: 'SIM-A2', region: 'SIM-REGION-A', label: 'Synthetic destination', dropOffCode: 'SIM-DROP-A2' }], routes: [routeData] }, tariff: { version: 'ARGO-ROAD-TARIFF-1', intraFirstVnd: 25000, intraStepVnd: 3000, interFirstVnd: 38000, interStepVnd: 5000, stepGrams: 500, volumetricDivisor: 5000, maxParcels: 5, maxActualGrams: 20000, maxDimensionMm: 1000, maxGirthMm: 2000, ttlSeconds: 900, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' }, contract: { id: contractId, organizationId: organizationA, version: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', state: 'ACTIVE', effectiveFrom: new Date(now - 1000).toISOString(), effectiveTo: null }, paymentMethod: 'CASH_ON_HANDOVER', payer: 'SENDER', paymentCollectionEnabled: false });
    if (path.endsWith('/quotes')) {
      quotes++; const body = api.body(), parcels = body.parcels as Record<string, unknown>[], created = now - (expiresSoon ? 898000 : 0);
      quote = { id: qid, organizationId: organizationA, facilityId: null, creatorAccountId: account, preparationId: id, preparationVersion: 1, contractId, contractVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', route: routeData, parcels: parcels.map(({ weightGrams, lengthMm, widthMm, heightMm }) => ({ weightGrams, lengthMm, widthMm, heightMm })), feeLines: [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: 25000 }], totalVnd: 25000, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE', createdAt: new Date(created).toISOString(), expiresAt: new Date(created + 900000).toISOString(), consumedBy: null };
      expect(req.headers()['if-match']).toBe('"v1"'); expect(req.headers()['idempotency-key']).toMatch(/^[a-f0-9-]{36}$/); expect(req.postDataJSON()).toEqual({ originCode: 'SIM-A1', destinationCode: 'SIM-A2', ordinaryGoodsConfirmed: true }); return reply({ quote, usable: true, serverTime: new Date(now).toISOString() }, 201);
    }
    const record = () => { const body = structuredClone(api.body()); for (const k of ['sender', 'recipient']) body[k] = { ...(body[k] as object), phone: '+84901234567' }; return { simulation: true, shipment: { id: sid, organizationId: organizationA, facilityId: null, creatorAccountId: account, code: 'AP-SIM-' + sid.replaceAll('-', ''), preparationId: id, preparationVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input: { ...body, instructions: null }, quote: { ...quote, consumedBy: sid }, totalVnd: 25000, createdAt: new Date(now).toISOString(), parcels: [{ id: qid, parcelNumber: 1, facts: (body.parcels as object[])[0] }] }, obligation: { id: qid, organizationId: organizationA, facilityId: null, shipmentId: sid, quoteId: qid, amountVnd: 25000, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-BUSINESS-LOCAL-01', createdAt: new Date(now).toISOString() } }; };
    if (path.endsWith('/bookings')) { bookings++; booked = true; expect(req.postDataJSON()).toEqual({ quoteId: qid }); return lostBooking ? route.abort('failed') : reply(record(), 201, '"v1"'); }
    if (path.includes('/shipping-booking-receipts/')) return reply({ targetId: sid, outcome: 'COMMITTED' });
    if (path === base + '/shipment-preparations/' + id && booked) return reply({ id, organizationId: organizationA, facilityId: null, creatorAccountId: account, state: 'BOOKED', preparationState: 'DRAFT', version: 2, input: record().shipment.input, sourceChannel: null, sourceOrderReference: null, createdAt: new Date(now).toISOString(), updatedAt: new Date(now).toISOString(), allowedActions: [] }, 200, '"v2"');
    if (path === base + '/shipments/' + sid && booked) return reply(record(), 200, '"v1"');
    if (path === base + '/shipments') return reply({ items: booked ? [record()] : [], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/labels')) return route.fulfill({ contentType: 'application/pdf', headers: { 'Cache-Control': 'no-store' }, body: '%PDF-Synthetic UI fixture only' });
    return route.fallback();
  });
  return { api, bookings: () => bookings, quotes: () => quotes };
}
test('Business saved review quotes and books with its own namespace then shows immutable detail and private label', async ({ page, context }) => {
  const api = await shippingFixture(context); await page.goto('/merchant/' + organizationA + '/create'); await fill(page); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.locator('.pb-total')).toContainText('25.000'); await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn doanh nghiệp', exact: true })).toBeVisible(); await expect(page.locator('.pb-unpaid')).toHaveText('UNPAID'); await expect(page.getByLabel('Kiện hàng đã lưu')).toContainText('Synthetic parcel'); const download = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn tất cả kiện', exact: true }).click(); expect((await download).suggestedFilename()).toMatch(/^argopost-.*\.pdf$/); expect(api.quotes()).toBe(1); expect(api.bookings()).toBe(1); expect(api.api.requests.some(r => r.includes('/customer/addresses'))).toBe(false);
});
test('unknown Business Booking locks edit and resolves its scoped receipt without another POST', async ({ page, context }) => {
  const api = await shippingFixture(context, true); await page.goto('/merchant/' + organizationA + '/create'); await fill(page); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click(); await expect(page.getByRole('button', { name: '← Chỉnh sửa thông tin', exact: true })).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra kết quả booking', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn doanh nghiệp', exact: true })).toBeVisible(); expect(api.bookings()).toBe(1);
});
test('expired Business Quote cannot send a Booking', async ({ page, context }) => {
  const api = await shippingFixture(context, false, true); await page.goto('/merchant/' + organizationA + '/create'); await fill(page); await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click(); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true })).toBeDisabled({ timeout: 8000 }); expect(api.bookings()).toBe(0);
});
for (const width of [390, 768, 1440]) test(`aligned entry layout is bounded with complete facts at ${width}px`, async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width, height: 960 });
  for (const url of ['/personal/create', '/merchant/' + organizationA + '/create']) {
    await page.goto(url); await expect(page.locator('#sender-name')).toBeVisible(); await fill(page); await page.getByRole('button', { name: '+ Thêm kiện (tối đa 5)', exact: true }).click(); await expect(page.locator('#weightGrams1')).toHaveValue(''); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: '../../../.demo/shipment-entry-parity/' + (url.startsWith('/personal') ? 'personal' : 'business') + '-' + width + '.png', fullPage: true, animations: 'disabled' });
  }
});
