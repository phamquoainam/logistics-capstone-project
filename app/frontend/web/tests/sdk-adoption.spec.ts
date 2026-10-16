import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { settings, syntheticExpiredToken } from './support/settings';

test.beforeAll(() => { if (!process.env.C04_CONTEXT) throw new Error('Run through the isolated C-04 web harness'); });
let pageErrors: string[] = [];
test.beforeEach(({ page }) => { pageErrors = []; page.on('pageerror', error => pageErrors.push(error.name)); });
test.afterEach(() => { expect(pageErrors).toEqual([]); });

async function login(page: Page, username = 'demo.merchant.a', password = settings.merchantAPassword) {
  await page.goto('/');
  await page.getByLabel('Tên đăng nhập').fill(username);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập →' }).click();
}
async function fillDraft(page: Page, reference: string) {
  await page.getByRole('link', { name: '＋ Tạo vận đơn nháp' }).first().click();
  for (const [label, value] of [['Mã tham chiếu', reference], ['Địa chỉ lấy hàng', 'Synthetic pickup'], ['Tên người nhận', 'Synthetic recipient'], ['Số điện thoại', '0000000000'], ['Số nhà, đường', 'Synthetic street'], ['Phường / xã', 'Synthetic ward'], ['Tỉnh / thành phố', 'Synthetic province'], ['Mô tả kiện hàng', 'Synthetic parcel'], ['Khối lượng (kg)', '1.25']]) {
    await page.getByLabel(label).fill(value);
  }
}

test('real SDK browser errors retain RBAC, ownership, token and rate-limit semantics', async ({ page }) => {
  await page.goto('/');
  // Dynamic import executes the actual web adapter through Vite, not a mock SDK.
  // Only safe statuses/codes/counts leave the browser; tokens remain in memory.
  const results = await page.evaluate(async ({ password, expiredToken }) => {
    const modulePath = '/src/api.ts';
    const { api }: typeof import('../src/api') = await import(/* @vite-ignore */ modulePath);
    const outcomes: { status: number; code: string }[] = [];
    const denied = async (call: () => Promise<unknown>) => {
      try { await call(); outcomes.push({ status: 200, code: 'UNEXPECTED_SUCCESS' }); }
      catch (error) { if (typeof error !== 'object' || !error || !('status' in error) || !('code' in error)) throw new Error('Missing ApiError'); outcomes.push({ status: Number(error.status), code: String(error.code) }); }
    };
    const health = await api.health();
    const a = await api.login({ username: 'demo.merchant.a', password });
    const b = await api.login({ username: 'demo.merchant.b', password });
    const current = await api.currentUser(a.accessToken);
    await denied(() => api.overview(a.accessToken));
    await denied(() => api.shipments('synthetic-invalid-token'));
    await denied(() => api.currentUser(expiredToken));
    await denied(() => api.shipments());
    const draft = { reference: 'SDK-BROWSER-' + crypto.randomUUID(), pickupAddress: 'Synthetic pickup', recipientName: 'Synthetic recipient', recipientPhone: '0000000000', deliveryStreet: 'Synthetic street', deliveryWard: 'Synthetic ward', deliveryProvince: 'Synthetic province', parcelDescription: 'Synthetic parcel', weightKg: 1 };
    const shipment = await api.createShipment(a.accessToken, draft, crypto.randomUUID());
    await denied(() => api.shipment(b.accessToken, shipment.id));
    await denied(() => api.cancelShipment(b.accessToken, shipment.id, { reason: 'denied' }, crypto.randomUUID()));
    await denied(() => api.createShipment(a.accessToken, draft, crypto.randomUUID()));
    for (let i = 0; i < 6; i++) await denied(() => api.login({ username: 'synthetic-rate-limit-browser', password: 'synthetic-invalid-password' }));
    const detail = await api.shipment(a.accessToken, shipment.id);
    return { healthy: health.status === 'UP', scoped: current.merchantId === shipment.merchantId,
      events: detail.events.length, status: detail.shipment.status, outcomes };
  }, { password: settings.merchantAPassword, expiredToken: syntheticExpiredToken() });
  expect(results.healthy).toBe(true); expect(results.scoped).toBe(true);
  expect(results.events).toBe(1); expect(results.status).toBe('DRAFT');
  expect(results.outcomes).toEqual([
    { status: 403, code: 'FORBIDDEN' }, { status: 401, code: 'INVALID_TOKEN' }, { status: 401, code: 'INVALID_TOKEN' }, { status: 401, code: 'UNAUTHENTICATED' },
    { status: 404, code: 'SHIPMENT_NOT_FOUND' }, { status: 404, code: 'SHIPMENT_NOT_FOUND' }, { status: 409, code: 'REFERENCE_EXISTS' },
    ...Array.from({ length: 5 }, () => ({ status: 401, code: 'INVALID_CREDENTIALS' })), { status: 429, code: 'LOGIN_RATE_LIMIT' },
  ]);
});

test('current-user polling ignores simulated403; actual invalid bearer401 logs out', async ({ page }) => {
  await page.clock.install();
  await login(page);
  await expect(page.getByRole('heading', { name: 'Merchant A · Demo' })).toBeVisible();
  let forbidden = 0, invalid = 0;
  // /auth/me normally has no role403: this is explicitly a frontend branch fixture.
  await page.route('**/api/demo/v1/auth/me', route => { forbidden++; return route.fulfill({ status: 403, json: { code: 'FORBIDDEN', message: 'Synthetic forbidden' } }); });
  await page.clock.fastForward(60_000);
  await expect.poll(() => forbidden).toBe(1);
  await expect(page.getByRole('button', { name: 'Đăng xuất' })).toBeVisible();
  await page.unroute('**/api/demo/v1/auth/me');
  await page.route('**/api/demo/v1/auth/me', route => { invalid++; return route.continue({ headers: { ...route.request().headers(), authorization: 'Bearer synthetic-invalid-token' } }); });
  await page.clock.fastForward(60_000);
  await expect.poll(() => invalid).toBe(1);
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
});

test('logout and next merchant login do not persist or reuse previous session', async ({ page }) => {
  await login(page);
  await expect(page.getByRole('heading', { name: 'Merchant A · Demo' })).toBeVisible();
  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  const loginHeaders: (string | undefined)[] = [];
  page.on('request', request => { if (request.url().endsWith('/api/demo/v1/auth/login')) loginHeaders.push(request.headers().authorization); });
  await login(page, 'demo.merchant.b', settings.merchantBPassword);
  await expect(page.getByRole('heading', { name: 'Merchant B · Demo' })).toBeVisible();
  expect(loginHeaders).toEqual([undefined]);
  expect(await page.evaluate(() => ({ local: localStorage.length, session: sessionStorage.length }))).toEqual({ local: 0, session: 0 });
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Chào mừng trở lại' })).toBeVisible();
});

test('lost committed create/cancel responses retain inputs and keys; retries replay one effect', async ({ page }) => {
  await login(page);
  const reference = 'LOST-' + Date.now();
  await fillDraft(page, reference);
  const createKeys: (string | undefined)[] = [], createStatuses: number[] = [];
  let loseCreate = true;
  await page.route('**/api/demo/v1/shipments', async route => {
    if (route.request().method() !== 'POST') return route.continue();
    createKeys.push(route.request().headers()['idempotency-key']);
    const response = await route.fetch(); createStatuses.push(response.status());
    if (loseCreate) { loseCreate = false; return route.abort(); }
    return route.fulfill({ response });
  });
  const save = page.getByRole('button', { name: 'Lưu vận đơn nháp →' });
  await save.click();
  await expect(page.getByRole('alert')).toContainText('Không thể kết nối backend');
  await expect(page.getByLabel('Mã tham chiếu')).toHaveValue(reference);
  await page.getByLabel('Tên người nhận').fill('Changed after uncertainty');
  await save.click();
  await expect(page.getByRole('alert')).toContainText('Mã thao tác');
  expect(createStatuses).toEqual([201, 409]);
  await page.getByLabel('Tên người nhận').fill('Synthetic recipient');
  await save.click();
  await expect(page.getByText('Đã tạo bản nháp', { exact: true })).toHaveCount(1);
  expect(createStatuses).toEqual([201, 409, 201]);
  expect(new Set(createKeys).size).toBe(1); expect(createKeys[0]).toBeTruthy();
  const cancelKeys: (string | undefined)[] = [], cancelStatuses: number[] = [];
  let loseCancel = true;
  await page.route('**/api/demo/v1/shipments/*/cancel', async route => {
    cancelKeys.push(route.request().headers()['idempotency-key']);
    const response = await route.fetch(); cancelStatuses.push(response.status());
    if (loseCancel) { loseCancel = false; return route.abort(); }
    return route.fulfill({ response });
  });
  await page.getByLabel('Lý do hủy').fill('Synthetic cancel after lost response');
  await page.getByRole('button', { name: 'Hủy bản nháp', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Không thể kết nối backend');
  await expect(page.getByLabel('Lý do hủy')).toHaveValue('Synthetic cancel after lost response');
  await page.getByRole('button', { name: 'Hủy bản nháp', exact: true }).click();
  await expect(page.getByText('Đã hủy bản nháp', { exact: true })).toHaveCount(1);
  await expect(page.getByText('Đã tạo bản nháp', { exact: true })).toHaveCount(1);
  expect(cancelStatuses).toEqual([200, 200]); expect(new Set(cancelKeys).size).toBe(1); expect(cancelKeys[0]).toBeTruthy();
});
