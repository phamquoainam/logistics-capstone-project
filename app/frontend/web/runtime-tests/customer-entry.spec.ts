import { test, expect } from '@playwright/test';
import type { BrowserContext, Page } from '@playwright/test';

const email = 'browser-customer@example.invalid', originalPassword = 'synthetic-browser-enrollment-password', newPassword = 'synthetic-browser-recovered-password';
const origin = process.env.S003_BROWSER_ORIGIN!;
let context: BrowserContext, page: Page, secondary: BrowserContext, secondaryPage: Page, capturedAccess = '';
test.describe.configure({ mode: 'serial' });
test.beforeAll(async ({ browser }) => { context = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }); page = await context.newPage(); });
test.afterAll(async () => { capturedAccess = ''; await secondary?.close(); await context?.close(); });

async function mail(index: number): Promise<{ link: string; code?: string }> {
  const inbox = new URL(process.env.S003_BROWSER_INBOX ?? '');
  if (inbox.protocol !== 'http:' || inbox.hostname !== '127.0.0.1' || inbox.username || inbox.password || inbox.search || inbox.hash || inbox.pathname !== '/') throw new Error('OWNED_INBOX_CONTEXT_REQUIRED');
  let link = '', code: string | undefined;
  await expect.poll(async () => {
    const response = await fetch(new URL('/mail/' + index, inbox), { headers: { 'X-Owned-Inbox-Nonce': process.env.S003_BROWSER_INBOX_NONCE ?? '' }, signal: AbortSignal.timeout(3000) });
    if (response.status === 202) return false;
    if (response.status !== 200) throw new Error('OWNED_INBOX_READ_FAILED');
    const value: unknown = await response.json();
    if (typeof value !== 'object' || value === null || !('link' in value) || typeof value.link !== 'string') throw new Error('OWNED_INBOX_MESSAGE_INVALID');
    const target = new URL(value.link);
    if (target.origin !== origin || target.pathname !== (index === 0 ? '/verify-email' : '/recover/confirm') || target.search) throw new Error('OWNED_INBOX_MESSAGE_INVALID');
    link = target.href;
    if (index === 0) { if (!('code' in value) || typeof value.code !== 'string' || !/^\d{6}$/.test(value.code)) throw new Error('OWNED_INBOX_OTP_INVALID'); code = value.code; }
    return true;
  }, { timeout: 15000, message: 'Actual SMTP message must be received' }).toBe(true);
  return { link, code };
}
async function login(target: Page, password: string) {
  await target.getByLabel('Email đã xác minh').fill(email); await target.getByLabel('Mật khẩu', { exact: true }).fill(password); await target.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

test('retail guest registration uses the real HTTPS API', async () => {
  await page.goto('/'); await expect(page.getByRole('heading', { name: /Chuyển phát rõ từng chặng/ })).toBeVisible();
  const media = await page.request.get('/api/v1/public-media/hero-network.svg'); expect(media.status()).toBe(200);
  expect(media.headers()['content-type']).toContain('image/svg+xml');
  const denied = await page.request.post('/api/v1/public-media/hero-network.svg'); expect(denied.status()).toBe(403);
  await page.goto('/register'); await expect(page.getByRole('heading', { name: 'Tạo tài khoản mới' })).toBeVisible();
  await page.getByLabel('Họ và tên', { exact: true }).fill('Người gửi hàng thử'); await page.getByLabel('Email', { exact: true }).fill(email); await page.getByLabel('Số điện thoại liên hệ').fill('0901234567'); await page.getByLabel('Mật khẩu', { exact: true }).fill(originalPassword); await page.getByLabel('Nhập lại mật khẩu').fill(originalPassword); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Đăng ký ngay', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Yêu cầu đã được tiếp nhận');
});
test('verification consumes the actual SMTP message only after confirmation', async () => {
  const received = await mail(0); await page.goto(received.link); await expect(page.getByRole('timer')).toContainText('Mã hết hạn sau'); expect(new URL(page.url()).hash).toBe('');
  await page.getByLabel('Mã xác minh 6 chữ số').fill(received.code!); await page.getByRole('button', { name: 'Xác minh tài khoản' }).click(); await expect(page.getByRole('status')).toContainText('Email đã được xác minh'); await page.getByRole('link', { name: 'Đăng nhập', exact: true }).last().click();
});
test('verified login has isolated customer cookies and no SYSTEM authority', async ({ browser }) => {
  const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/customer-auth/login' && r.request().method() === 'POST'); await login(page, originalPassword); const received = await response; expect(received.status()).toBe(200); const body = await received.json(); if (typeof body.accessToken !== 'string') throw new Error('CUSTOMER_ACCESS_RESPONSE_INVALID'); capturedAccess = body.accessToken;
  await expect(page.getByRole('heading', { name: 'Xin chào, Người gửi hàng thử' })).toBeVisible();
  await page.goto('/merchant'); await expect(page.getByRole('heading', { name: 'Không gian Merchant chưa khả dụng' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('không hiển thị dữ liệu Merchant'); await page.goto('/personal');
  const cookies = await context.cookies(); expect(cookies.some(c => c.name === '__Host-elogistics.customer.refresh' && c.httpOnly && c.secure && c.sameSite === 'Strict' && c.path === '/')).toBe(true); expect(cookies.some(c => c.name === '__Host-elogistics.refresh')).toBe(false);
  expect(await page.evaluate(async token => (await fetch('/api/v1/auth/me', { headers: { Authorization: 'Bearer ' + token }, cache: 'no-store' })).status, capturedAccess)).toBe(401);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  secondary = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }); secondaryPage = await secondary.newPage(); await secondaryPage.goto('/login'); await login(secondaryPage, originalPassword); await expect(secondaryPage.getByRole('heading', { name: 'Xin chào, Người gửi hàng thử' })).toBeVisible();
});
test('profile lost response resolves through the real committed receipt', async () => {
  await page.getByRole('link', { name: 'Hồ sơ', exact: true }).click(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Người gửi hàng thử'); await page.getByLabel('Tên hiển thị trong hồ sơ').fill('Tên gửi hàng đã cập nhật');
  let writes = 0;
  // Fault injection only: forward the actual protected PUT, require real commit, then lose its response.
  await page.route('**/api/v1/customer/profile', async route => {
    if (route.request().method() !== 'PUT') { await route.continue(); return; }
    writes++; const committed = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(committed.status()).toBe(200); await route.abort('failed');
  });
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu' }).click(); await expect(page.getByText('Đã lưu hồ sơ.', { exact: true })).toBeVisible(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Tên gửi hàng đã cập nhật'); expect(writes).toBe(1); await page.unroute('**/api/v1/customer/profile');
});
test('merchant draft uses the real HTTPS API and resolves one lost write', async () => {
  await page.getByRole('link', { name: 'Merchant', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Hồ sơ đăng ký Merchant' })).toBeVisible();
  await page.getByRole('button', { name: 'Tạo bản nháp Merchant' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết hồ sơ Merchant' })).toBeVisible();
  const applicationId = new URL(page.url()).pathname.split('/').at(-1);
  expect(applicationId).toMatch(/^[0-9a-f-]{36}$/);
  await page.getByLabel('Tên pháp nhân đăng ký').fill('Synthetic browser merchant');
  let writes = 0;
  await page.route('**/api/v1/merchant-applications/*', async route => {
    if (route.request().method() !== 'PUT') { await route.continue(); return; }
    writes++; const committed = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(committed.status()).toBe(200); await route.abort('failed');
  });
  await page.getByRole('button', { name: 'Lưu bản nháp' }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định');
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu bản nháp' }).click();
  await expect(page.getByText('Đã lưu bản nháp.', { exact: true })).toBeVisible();
  await expect(page.getByLabel('Tên pháp nhân đăng ký')).toHaveValue('Synthetic browser merchant');
  expect(writes).toBe(1); await page.unroute('**/api/v1/merchant-applications/*');
  await page.getByRole('link', { name: 'Danh sách hồ sơ' }).click();
  await expect(page.getByText('Synthetic browser merchant', { exact: true })).toBeVisible();
});
test('logout and recovery request use the actual verified destination', async () => {
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); await page.getByRole('link', { name: 'Quên mật khẩu?' }).click(); await page.getByLabel('Email của tài khoản').fill(email); await page.getByRole('button', { name: 'Yêu cầu khôi phục mật khẩu' }).click(); await expect(page.getByRole('status')).toContainText('Đã tiếp nhận yêu cầu');
});
test('recovery confirmation changes the credential with the actual second message', async () => {
  await page.goto((await mail(1)).link); await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toBeVisible(); expect(new URL(page.url()).hash).toBe(''); await page.getByLabel('Mật khẩu mới', { exact: true }).fill(newPassword); await page.getByLabel('Nhập lại mật khẩu mới').fill(newPassword); await page.getByRole('button', { name: 'Xác nhận mật khẩu mới' }).click(); await expect(page.getByRole('status')).toContainText('Mật khẩu đã được cập nhật');
});
test('fresh login rejects the old credential and revoked browser loses its workspace', async () => {
  await secondaryPage.getByRole('link', { name: 'Hồ sơ', exact: true }).click(); await expect(secondaryPage.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); await expect(secondaryPage.getByLabel('Tên hiển thị trong hồ sơ')).toHaveCount(0);
  await page.getByRole('link', { name: 'Đăng nhập', exact: true }).last().click(); await login(page, originalPassword); await expect(page.getByRole('alert')).toContainText('Email hoặc mật khẩu chưa đúng'); await login(page, newPassword); await expect(page.getByRole('heading', { name: 'Xin chào, Người gửi hàng thử' })).toBeVisible(); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible();
});
test('unknown email and invalid proof do not fabricate account activation', async () => {
  await page.goto('/recover'); await page.getByLabel('Email của tài khoản').fill('unknown-browser@example.invalid'); await page.getByRole('button', { name: 'Yêu cầu khôi phục mật khẩu' }).click(); await expect(page.getByRole('status')).toContainText('Đã tiếp nhận yêu cầu');
  await page.goto('/verify-email#proofId=20000000-0000-4000-8000-000000000001&proof=' + 'A'.repeat(43)); await page.getByRole('button', { name: 'Xác nhận email của tôi' }).click(); await expect(page.getByRole('alert')).toContainText('Liên kết không còn hiệu lực'); await expect(page.getByText('Email đã được xác minh.', { exact: true })).toHaveCount(0);
});
