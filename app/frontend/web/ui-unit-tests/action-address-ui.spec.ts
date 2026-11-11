import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import type { AddressInput, PersonalAddress } from '../src/customer/client';
const account = '10000000-0000-4000-8000-000000000001', id = '10000000-0000-4000-8000-000000000010';
const address = (patch: Partial<PersonalAddress> = {}): PersonalAddress => ({ id, accountId: account, kind: 'SENDER', label: 'Synthetic home', contactName: 'Synthetic contact', contactPhone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' }, isDefault: false, verificationStatus: 'UNVERIFIED', version: 1, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z', ...patch });
async function source(context: BrowserContext, options: { duplicate?: boolean; failure?: boolean; foreign?: boolean; wrongKind?: boolean; stale?: boolean; unknownDefault?: boolean; laterPage?: boolean } = {}) {
  const reads: URL[] = [], writes: { path: string; body: AddressInput; etag?: string; key?: string }[] = [];
  let current = address(), committed = false;
  await context.route('**/api/v1/**', async route => {
    const request = route.request(), url = new URL(request.url()), path = url.pathname;
    const reply = (data: unknown, status = 200, headers: Record<string, string> = {}) => route.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(data) });
    if (request.method() === 'GET') reads.push(url);
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-ui-csrf' });
    if (path.endsWith('/refresh')) return reply({ accessToken: 'synthetic-ui-access', accountId: account, expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.includes('/address-receipts/')) return reply({ operationId: 'updatePersonalAddress', targetId: id, outcome: 'COMMITTED', version: 2 });
    if (path.endsWith('/addresses') && request.method() === 'GET') {
      const scanning = url.searchParams.get('search')?.startsWith('+84');
      if (scanning && options.failure) return reply({ code: 'SERVICE_UNAVAILABLE' }, 503);
      const page = Number(url.searchParams.get('page') ?? '0');
      const items = scanning ? options.duplicate && page === 1 ? [address({ id: '10000000-0000-4000-8000-000000000011' })] : [] : page === 0 ? [current] : [];
      return reply({ items, page, size: 20, hasNext: page === 0 && (scanning ? !!options.duplicate : !!options.laterPage) });
    }
    if (path.endsWith('/addresses/' + id) && request.method() === 'GET') return reply({ ...current, ...(options.foreign ? { accountId: '10000000-0000-4000-8000-000000000002' } : {}), ...(options.wrongKind ? { kind: 'RECIPIENT' } : {}) }, 200, { ETag: '"v' + current.version + '"' });
    if (path.endsWith('/addresses/' + id) && request.method() === 'PUT') {
      const body = request.postDataJSON(); writes.push({ path, body, etag: request.headers()['if-match'], key: request.headers()['idempotency-key'] });
      if (options.stale) return reply({ code: 'PRECONDITION_STALE' }, 409);
      current = address({ ...body, version: 2 }); committed = true;
      if (options.unknownDefault) return route.abort('failed');
      return reply(current, 200, { ETag: '"v2"' });
    }
    return reply({ code: 'ISOLATED_SOURCE_NOT_CONFIGURED' }, 503);
  }); return { reads, writes, committed: () => committed };
}
async function fill(page: import('@playwright/test').Page) {
  await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Synthetic contact'); await page.getByLabel('Số điện thoại liên hệ', { exact: true }).fill('0901-234-567');
  await page.getByLabel('Tỉnh / thành phố').fill('Synthetic area'); await page.getByLabel('Phường / xã / địa phương').fill('Synthetic locality'); await page.getByLabel('Địa chỉ chi tiết', { exact: true }).fill('Synthetic street');
}
test('Address Book belongs only to Management and copies an owned contact into a fresh unsaved shipment', async ({ page, context }) => {
  const calls = await source(context); await page.goto('/personal/addresses');
  await expect(page.locator('#personal-management-links').getByRole('link', { name: 'Sổ địa chỉ', exact: true })).toHaveClass('active');
  await expect(page.getByRole('complementary').getByRole('link', { name: 'Sổ địa chỉ', exact: true })).toHaveCount(1);
  await page.getByRole('button', { name: 'Dùng tạo vận đơn', exact: true }).click(); await expect(page.locator('#sender-name')).toHaveValue('Synthetic contact'); await expect(page.locator('#sender-addressLine')).toHaveValue('Synthetic street');
  expect(calls.reads.some(u => u.pathname.endsWith('/addresses/' + id))).toBe(true); expect(calls.writes).toHaveLength(0);
  await page.locator('.se-contact').first().getByRole('link', { name: 'Sổ địa chỉ', exact: false }).click();
  await expect(page.locator('#personal-management-links')).toBeVisible(); await expect(page.locator('#personal-management-links').getByRole('link', { name: 'Sổ địa chỉ', exact: true })).toHaveClass('active');
});
for (const kind of ['foreign', 'wrongKind'] as const) test(`${kind} selected contact never fills a shipment or becomes a successful write`, async ({ page, context }) => {
  const calls = await source(context, { [kind]: true }); await page.goto('/personal/create?senderAddressId=' + id); await expect(page.getByRole('alert')).toContainText('Không thể dùng liên hệ'); await expect(page.locator('#sender-name')).toHaveCount(0); expect(calls.writes).toHaveLength(0);
});
test('draft plus a selected address is rejected before either detail is read', async ({ page, context }) => {
  const calls = await source(context); await page.goto('/personal/create?draft=' + id + '&senderAddressId=' + id); await expect(page.getByRole('alert')).toContainText('Không thay thế thông tin'); expect(calls.reads.some(u => /\/(addresses|shipment-drafts)\//.test(u.pathname))).toBe(false);
});
test('duplicate scan covers later pages, warns without merging and hides a stale result after input changes', async ({ page, context }) => {
  const calls = await source(context, { duplicate: true }); await page.goto('/personal/addresses'); await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click(); await fill(page);
  await page.getByRole('button', { name: 'Kiểm tra trùng liên hệ', exact: true }).click(); await expect(page.locator('.ab-check-result')).toContainText('Có liên hệ đang hoạt động trùng');
  expect(calls.reads.some(u => u.searchParams.get('page') === '1' && u.searchParams.get('search') === '+84901234567')).toBe(true); expect(calls.writes).toHaveLength(0);
  await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Another contact'); await expect(page.locator('.ab-check-result')).toHaveCount(0);
});
test('failed duplicate lookup is unknown, never a no-duplicates success; normalization is explicit', async ({ page, context }) => {
  await source(context, { failure: true }); await page.goto('/personal/addresses'); await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click(); await fill(page);
  await expect(page.getByLabel('Số điện thoại liên hệ', { exact: true })).toHaveValue('0901-234-567'); await page.getByRole('button', { name: 'Chuẩn hóa cách nhập', exact: true }).click(); await expect(page.getByLabel('Số điện thoại liên hệ', { exact: true })).toHaveValue('+84901234567');
  await page.getByRole('button', { name: 'Kiểm tra trùng liên hệ', exact: true }).click(); await expect(page.locator('.ab-check-result')).toContainText('Không thể kết luận không trùng');
});
test('default shortcut uses original ETag and one idempotent write; lost response resolves by receipt', async ({ page, context }) => {
  const calls = await source(context, { unknownDefault: true }); await page.goto('/personal/addresses'); await page.getByRole('button', { name: 'Đặt mặc định', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByRole('button', { name: 'Dùng tạo vận đơn', exact: true })).toBeDisabled(); expect(calls.committed()).toBe(true);
  await page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' }).click(); await expect(page.locator('.ab-default')).toBeVisible(); expect(calls.writes).toHaveLength(1); expect(calls.writes[0]).toMatchObject({ etag: '"v1"', body: { isDefault: true } }); expect(calls.writes[0].key).toMatch(/^[a-f0-9-]{36}$/);
});
test('stale default never claims success and can refresh the list', async ({ page, context }) => {
  await source(context, { stale: true }); await page.goto('/personal/addresses'); await page.getByRole('button', { name: 'Đặt mặc định', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Địa chỉ đã thay đổi'); await expect(page.locator('.ab-default')).toHaveCount(0); await page.getByRole('button', { name: 'Tải lại danh sách địa chỉ' }).click(); await expect(page.getByRole('alert')).toHaveCount(0);
});
test('empty later address page retains filters; archive explains no restore and no shipment rewrite', async ({ page, context }) => {
  const calls = await source(context, { laterPage: true }); await page.goto('/personal/addresses'); await page.getByRole('button', { name: 'Lưu trữ', exact: true }).click(); await expect(page.getByRole('alertdialog')).toContainText('chưa hỗ trợ khôi phục'); await expect(page.getByRole('alertdialog')).toContainText('vận đơn đã tạo không thay đổi'); await page.getByRole('button', { name: 'Giữ địa chỉ' }).click();
  await page.getByLabel('Tìm liên hệ hoặc địa chỉ').fill('Synthetic'); await page.getByRole('button', { name: 'Tìm địa chỉ', exact: true }).click();
  await page.getByRole('button', { name: 'Trang sau', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Trang này không còn liên hệ' })).toBeVisible(); await page.getByRole('button', { name: 'Về trang đầu', exact: true }).click(); await expect(page.locator('.ab-card')).toBeVisible(); expect(calls.reads.at(-1)!.searchParams.get('search')).toBe('Synthetic');
  await page.getByRole('button', { name: 'Đặt lại bộ lọc' }).click(); expect(calls.writes).toHaveLength(0);
});
test('Action Center is explicitly unavailable in both views and never turns UNPAID into incidents', async ({ page, context }) => {
  const calls = await source(context); await page.goto('/personal/action-required'); await expect(page.getByRole('heading', { name: 'Chưa có nguồn yêu cầu xử lý' })).toBeVisible();
  await page.getByRole('button', { name: 'Yêu cầu đã gửi', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chưa mở theo dõi yêu cầu đã gửi' })).toBeVisible();
  await page.getByText('Những vấn đề và phản hồi sẽ được quản lý tại đây', { exact: true }).click(); await expect(page.locator('.ac-guide')).toContainText('UNPAID không tự trở thành sự cố');
  expect(calls.reads.filter(u => !/\/(me|csrf)$/.test(u.pathname))).toHaveLength(0); expect(calls.writes).toHaveLength(0); await expect(page.getByRole('button', { name: /Giao lại|Xác nhận|Bồi thường|Thu tiền/ })).toHaveCount(0);
});
for (const width of [1440, 390]) test(`address and action layouts remain bounded at ${width}px`, async ({ page, context }) => {
  await source(context); await page.setViewportSize({ width, height: 960 });
  for (const [path, name] of [['/personal/addresses', 'addresses'], ['/personal/action-required', 'actions']]) { await page.goto(path); await expect(page.getByRole('heading', { level: 1 })).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: `../../../.demo/action-address-review/${name}-${width}.png`, fullPage: true, animations: 'disabled' }); }
});
