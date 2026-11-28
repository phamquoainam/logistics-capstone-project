import { test, expect } from '@playwright/test';

test('verified Operations Staff accepts both real Customer parcels at its assigned Station without changing UNPAID', async ({ browser, page, context }) => {
  const origin = new URL(process.env.S003_BROWSER_ORIGIN ?? '').origin;
  const inbox = new URL(process.env.S003_BROWSER_INBOX ?? '');
  if (inbox.protocol !== 'http:' || inbox.hostname !== '127.0.0.1' || inbox.pathname !== '/') throw new Error('OWNED_INBOX_REQUIRED');
  let invitation = '', access = '';
  await expect.poll(async () => {
    const response = await fetch(new URL('/mail/0', inbox), { headers: { 'X-Owned-Inbox-Nonce': process.env.S003_BROWSER_INBOX_NONCE ?? '' }, signal: AbortSignal.timeout(3000) });
    if (response.status === 202) return false;
    if (response.status !== 200) throw new Error('OPS_INBOX_FAILED');
    const v: unknown = await response.json();
    if (!v || typeof v !== 'object' || !('invitation' in v) || typeof v.invitation !== 'string' || !v.invitation.startsWith(origin + '/ops/activate#proofId=')) throw new Error('OPS_INBOX_INVALID');
    invitation = v.invitation; return true;
  }, { timeout: 15000 }).toBe(true);
  await page.goto(invitation); invitation = '';
  await expect(page).toHaveURL(origin + '/ops/activate');
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('Synthetic browser Operations password!9');
  await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill('Synthetic browser Operations password!9');
  await page.getByRole('button', { name: 'Kích hoạt tài khoản', exact: true }).click();
  await expect(page).toHaveURL(origin + '/ops/login');
  page.on('response', response => { if (response.url() === origin + '/api/v1/ops-auth/login' && response.ok()) void response.json().then((v: { accessToken: string }) => { access = v.accessToken; }); });
  await page.getByLabel('Email nhân viên').fill('browser-operations@example.invalid');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Synthetic browser Operations password!9');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tiếp nhận tại Station', exact: true })).toBeVisible();
  await expect.poll(() => access.length > 100).toBe(true);
  await expect.poll(async () => {
    const r = await page.request.get('/api/v1/ops/sites', { headers: { Authorization: 'Bearer ' + access, Origin: origin } });
    return r.status() === 200 && (await r.json() as { sites: unknown[] }).sites.length === 1;
  }).toBe(true);
  await page.reload(); await expect(page.locator('#intake-station')).toHaveValue('SIM-DROP-A1');
  for (const name of ['__Host-argopost.ops.csrf', '__Host-argopost.ops.refresh']) expect((await context.cookies()).find(c => c.name === name)).toMatchObject({ secure: true, httpOnly: true, sameSite: 'Strict', path: '/' });
  expect((await context.cookies()).some(c => c.name.includes('customer.refresh') || c.name.includes('staff.refresh'))).toBe(false);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  expect((await page.request.get('/api/v1/customer-auth/me', { headers: { Authorization: 'Bearer ' + access, Origin: origin } })).status()).toBe(401);
  expect((await page.request.get('/api/v1/staff-auth/me', { headers: { Authorization: 'Bearer ' + access, Origin: origin } })).status()).toBe(401);
  const customer = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false });
  try {
    const sender = await customer.newPage();
    await sender.goto('/login'); await sender.getByLabel('Email đã xác minh').fill('customer@example.invalid'); await sender.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-session-password'); await sender.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await expect(sender).toHaveURL(/\/personal/);
    await sender.goto('/personal/lookup?view=estimate'); await sender.getByLabel('Nơi gửi', { exact: true }).selectOption('SIM-A1'); await sender.getByLabel('Nơi nhận', { exact: true }).selectOption('SIM-A2');
    await sender.getByLabel('Khối lượng kiện 1', { exact: true }).fill('501'); for (const field of ['Dài', 'Rộng', 'Cao']) await sender.getByLabel(field + ' kiện 1', { exact: true }).fill('10');
    await sender.getByRole('button', { name: 'Thêm kiện', exact: true }).click(); await sender.getByLabel('Khối lượng kiện 2', { exact: true }).fill('500'); for (const field of ['Dài', 'Rộng', 'Cao']) await sender.getByLabel(field + ' kiện 2', { exact: true }).fill('10');
    await sender.getByRole('button', { name: 'Tính cước tham khảo', exact: true }).click(); await expect(sender.locator('.st-estimate-total')).toContainText('53.000'); await sender.getByRole('button', { name: 'Dùng thông tin này tạo vận đơn' }).click();
    for (const prefix of ['sender', 'recipient']) for (const [field, value] of [['name', 'Synthetic ' + prefix], ['phone', '0901234567'], ['administrativeArea', 'Synthetic area'], ['locality', 'Synthetic locality'], ['addressLine', 'Synthetic test location']]) await sender.locator('#' + prefix + '-' + field).fill(value);
    for (let i = 0; i < 2; i++) await sender.locator('#parcel-description-' + i).fill('Synthetic ordinary parcel ' + (i + 1));
    await sender.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click(); await expect(sender.getByRole('heading', { name: 'Xác nhận thông tin & báo giá', exact: true })).toBeVisible();
    await sender.locator('#pilot-origin').selectOption('SIM-A1'); await sender.locator('#pilot-destination').selectOption('SIM-A2'); await sender.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check(); await sender.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(sender.locator('.pb-total')).toContainText('53.000');
    await sender.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click(); await expect(sender.getByRole('heading', { name: 'Chi tiết vận đơn', exact: true })).toBeVisible();
    await sender.getByRole('tab', { name: 'Kiện hàng', exact: true }).click(); const parcelCodes = sender.getByRole('tabpanel').locator('code.sl-code');await expect(parcelCodes).toHaveCount(2);const codes = await parcelCodes.allTextContents(); expect(codes).toHaveLength(2);
    for (let i = 0; i < codes.length; i++) {
      await page.locator('#intake-barcode').fill(codes[i]); await page.getByRole('button', { name: 'Scan & đối chiếu kiện', exact: true }).click(); await expect(page.locator('.so-intake')).toContainText((i + 1) + '/2 kiện đã scan');
      if (i === 0) await expect(page.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true })).toBeDisabled();
    }
    await page.getByRole('checkbox', { name: /Tôi đã đối chiếu đủ kiện/ }).check(); await page.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true }).click(); await expect(page.locator('.so-intake .so-heading')).toContainText('Đã tiếp nhận tại Station'); await expect(page.locator('.so-intake')).toContainText('UNPAID');
    await page.getByRole('button', { name: 'Tải / In nhãn', exact: true }).click(); await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toBeVisible();
    await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/operations-real-desktop.png', fullPage: true, animations: 'disabled' });
    await page.setViewportSize({ width: 390, height: 844 }); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/operations-real-mobile.png', fullPage: true, animations: 'disabled' });
    await sender.reload(); await expect(sender.locator('.pb-shipment-heading')).toContainText('UNPAID'); await sender.getByRole('tab', { name: 'Hành trình', exact: true }).click(); await expect(sender.getByRole('tabpanel')).toContainText('Carrier tiếp nhận tại Station');
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập Operations', exact: true })).toBeVisible();
    expect((await page.request.get('/api/v1/ops-auth/me', { headers: { Authorization: 'Bearer ' + access, Origin: origin } })).status()).toBe(401); access = '';
  } finally { await customer.close(); }
});
