import { readFileSync, writeFileSync } from 'node:fs';
import { test, expect } from '@playwright/test';
import type { BrowserContext, Page } from '@playwright/test';

const email = 'browser-customer@example.invalid', originalPassword = 'synthetic-browser-enrollment-password', newPassword = 'synthetic-browser-recovered-password';
const origin = process.env.S003_BROWSER_ORIGIN!;
let context: BrowserContext, page: Page, secondary: BrowserContext, secondaryPage: Page, capturedAccess = '';
test.describe.configure({ mode: 'serial' });
test.beforeAll(async ({ browser }) => { context = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }); page = await context.newPage(); });
test.afterAll(async () => { capturedAccess = ''; await secondary?.close(); await context?.close(); });

async function mail(index: number): Promise<{ code: string }> {
  const inbox = new URL(process.env.S003_BROWSER_INBOX ?? '');
  if (inbox.protocol !== 'http:' || inbox.hostname !== '127.0.0.1' || inbox.username || inbox.password || inbox.search || inbox.hash || inbox.pathname !== '/') throw new Error('OWNED_INBOX_CONTEXT_REQUIRED');
  let code = '';
  await expect.poll(async () => {
    const response = await fetch(new URL('/mail/' + index, inbox), { headers: { 'X-Owned-Inbox-Nonce': process.env.S003_BROWSER_INBOX_NONCE ?? '' }, signal: AbortSignal.timeout(3000) });
    if (response.status === 202) return false;
    if (response.status !== 200) throw new Error('OWNED_INBOX_READ_FAILED');
    const value: unknown = await response.json();
    if (typeof value !== 'object' || value === null || !('code' in value) || typeof value.code !== 'string' || !/^\d{6}$/.test(value.code) || Object.keys(value).length !== 1) throw new Error('OWNED_INBOX_MESSAGE_INVALID');
    code=value.code;return true;
  }, { timeout: 15000, message: 'Actual SMTP message must be received' }).toBe(true);
  return { code };
}
async function login(target: Page, password: string) {
  await target.getByLabel('Email đã xác minh').fill(email); await target.getByLabel('Mật khẩu', { exact: true }).fill(password); await target.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

test('retail guest registration uses the real HTTPS API', async () => {
  await page.goto('/'); await expect(page.getByRole('heading', { name: 'ArgoPost', exact: true })).toHaveCount(1);
  await expect(page.locator('.ps-hero-art img')).toBeVisible();
  const media = await page.request.get('/api/v1/public-media/hero-network.svg'); expect(media.status()).toBe(200);
  expect(media.headers()['content-type']).toContain('image/svg+xml');
  const denied = await page.request.post('/api/v1/public-media/hero-network.svg'); expect(denied.status()).toBe(403);
  await page.goto('/register'); await expect(page.getByRole('heading', { name: 'Tạo tài khoản mới' })).toBeVisible();
  await page.getByLabel('Họ và tên', { exact: true }).fill('Người gửi hàng thử'); await page.getByLabel('Email', { exact: true }).fill(email); await page.getByLabel('Số điện thoại liên hệ').fill('0901234567'); await page.getByLabel('Mật khẩu', { exact: true }).fill(originalPassword); await page.getByLabel('Nhập lại mật khẩu').fill(originalPassword); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Đăng ký ngay', exact: true }).click();
  await expect(page).toHaveURL(/\/verify-email$/);await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeVisible();
});
test('verification consumes the actual SMTP message only after confirmation', async () => {
  const received = await mail(0); await expect(page.getByRole('timer')).toContainText(':'); expect(new URL(page.url()).hash).toBe('');
  await page.getByLabel('Mã xác minh 6 chữ số').fill(received.code!); await page.getByRole('button', { name: 'Xác minh tài khoản' }).click(); await expect(page.getByRole('status')).toContainText('Bạn đã đăng ký thành công!'); await expect(page).toHaveURL(/\/login$/);await expect(page.getByRole('heading',{name:'Đăng nhập bằng mật khẩu'})).toBeVisible();
});
test('verified login has isolated customer cookies and no SYSTEM authority', async ({ browser }) => {
  const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/customer-auth/login' && r.request().method() === 'POST'); await login(page, originalPassword); const received = await response; expect(received.status()).toBe(200); const body = await received.json(); if (typeof body.accessToken !== 'string') throw new Error('CUSTOMER_ACCESS_RESPONSE_INVALID'); capturedAccess = body.accessToken;
  await expect(page.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
  await expect(page.locator('.an-header')).toContainText('Người gửi hàng thử');
  await page.getByRole('button', { name: 'ArgoPost Personal', exact: true }).click();
  await expect(page.locator('#workspace-choices')).toContainText('Chưa kích hoạt');
  await expect(page.locator('#workspace-choices').getByRole('button', { name: /ArgoPost Business/ })).toBeDisabled();
  await expect(page.locator('#workspace-choices a[href^="/merchant"]')).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(page.locator('#workspace-choices')).toHaveCount(0);
  await expect(page.locator('.an-empty')).toContainText('Chưa có vận đơn');
  await page.goto('/merchant'); await expect(page.getByRole('heading', { name: 'Chưa có quyền truy cập doanh nghiệp' })).toBeVisible();
  await expect(page.locator('.bw-dashboard')).toHaveCount(0); await page.goto('/personal');
  const cookies = await context.cookies(); expect(cookies.some(c => c.name === '__Host-elogistics.customer.refresh' && c.httpOnly && c.secure && c.sameSite === 'Strict' && c.path === '/')).toBe(true); expect(cookies.some(c => c.name === '__Host-elogistics.refresh')).toBe(false);
  expect(await page.evaluate(async token => (await fetch('/api/v1/auth/me', { headers: { Authorization: 'Bearer ' + token }, cache: 'no-store' })).status, capturedAccess)).toBe(401);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  secondary = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }); secondaryPage = await secondary.newPage(); await secondaryPage.goto('/login'); await login(secondaryPage, originalPassword); await expect(secondaryPage.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
});
test('profile lost response resolves through the real committed receipt', async () => {
  await page.locator('.pw-user summary').click(); await page.locator('.pw-user-dropdown').getByRole('link', { name: 'Hồ sơ cá nhân', exact: true }).click(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Người gửi hàng thử'); await page.getByLabel('Tên hiển thị trong hồ sơ').fill('Tên gửi hàng đã cập nhật');
  let writes = 0;
  // Fault injection only: forward the actual protected PUT, require real commit, then lose its response.
  await page.route('**/api/v1/customer/profile', async route => {
    if (route.request().method() !== 'PUT') { await route.continue(); return; }
    writes++; const committed = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(committed.status()).toBe(200); await route.abort('failed');
  });
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu' }).click(); await expect(page.getByText('Đã lưu hồ sơ.', { exact: true })).toBeVisible(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Tên gửi hàng đã cập nhật'); expect(writes).toBe(1); await page.unroute('**/api/v1/customer/profile');
  await page.reload(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Tên gửi hàng đã cập nhật');
  await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
});
test('support and avatar settings reuse actual self profile without claiming unsupported workflows', async () => {
  const writes: string[] = [];
  const watch = (request: import('@playwright/test').Request) => { if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method()) && !request.url().includes('/customer-auth/')) writes.push(new URL(request.url()).pathname); };
  page.on('request', watch);
  await page.locator('.pw-user summary .pw-avatar').click();
  await page.locator('.pw-user-dropdown').getByRole('link', { name: 'Cài đặt & Quyền riêng tư' }).click();
  await expect(page.getByRole('heading', { name: 'Cài đặt & Quyền riêng tư', exact: true })).toBeVisible();
  await expect(page.getByText('Tiếng Việt', { exact: true })).toBeVisible();
  await expect(page.locator('.sa-settings-grid input,.sa-settings-grid select')).toHaveCount(0);
  await page.reload(); await expect(page.getByText('Tiếng Việt', { exact: true })).toBeVisible();
  await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Hỗ trợ & Khiếu nại', exact: true }).click();
  await page.getByRole('button', { name: 'Hướng dẫn & Quy trình', exact: true }).click(); await page.getByLabel('Tìm trong hướng dẫn').fill('Code128'); await expect(page.locator('.sa-faq details')).toHaveCount(1);
  await page.getByRole('button', { name: 'Khiếu nại & Bồi thường', exact: true }).click();
  await expect(page.locator('.sa-availability')).toContainText('Chưa mở tiếp nhận trực tuyến');
  await expect(page.locator('input[type=file],textarea,button[type=submit]')).toHaveCount(0);
  await page.locator('.pw-user summary').click(); await page.keyboard.press('Escape'); await expect(page.locator('.pw-user-dropdown')).not.toBeVisible();
  await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
  page.off('request', watch); expect(writes).toEqual([]);
});

test('personal support ticket uses real staff assignment conversation review and closure', async ({ browser }) => {
  await page.goto('/personal/support'); await page.getByRole('button', { name: 'Tạo yêu cầu hỗ trợ', exact: true }).click();
  await page.getByLabel('Tiêu đề', { exact: true }).fill('Synthetic browser support'); await page.getByLabel('Mô tả vấn đề').fill('Synthetic customer question');
  let writes = 0;
  await page.route('**/api/v1/customer/support-tickets', async route => {
    if (route.request().method() !== 'POST') { await route.continue(); return; }
    writes++; const committed = await route.fetch({ maxRetries: 0, timeout: 15000 }); expect(committed.status()).toBe(201); await route.abort('failed');
  });
  await page.getByRole('button', { name: 'Gửi yêu cầu hỗ trợ', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định');
  await page.getByRole('button', { name: 'Kiểm tra kết quả gửi' }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết yêu cầu', exact: true })).toBeVisible(); expect(writes).toBe(1); await page.unroute('**/api/v1/customer/support-tickets');
  const target = new URL(page.url()).searchParams.get('ticket'); expect(target).toMatch(/^[0-9a-f-]{36}$/);
  const denied = await page.request.get('/api/v1/support-operations/queue', { headers: { Authorization: 'Bearer ' + capturedAccess } }); expect(denied.status()).toBe(403);
  const staff = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }), staffPage = await staff.newPage();
  try {
    await staffPage.goto('/login'); await staffPage.getByLabel('Email đã xác minh').fill('browser-staff@example.invalid'); await staffPage.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-session-password'); await staffPage.getByRole('button', { name: 'Đăng nhập', exact: true }).click(); await expect(staffPage.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
    await staffPage.goto('/operations/support'); await staffPage.getByRole('button', { name: 'Nhận xử lý', exact: true }).click(); await expect(staffPage.locator('.su-conversation')).toContainText('Synthetic customer question');
    await staffPage.getByLabel('Nội dung công khai cho khách hàng').fill('Synthetic investigation started'); await staffPage.getByRole('button', { name: 'Bắt đầu xử lý', exact: true }).click();
    await staffPage.getByLabel('Nội dung công khai cho khách hàng').fill('Synthetic information requested'); await staffPage.getByRole('button', { name: 'Yêu cầu bổ sung', exact: true }).click(); await expect(staffPage.locator('.su-metadata')).toContainText('Cần bổ sung');
    await page.getByRole('button', { name: 'Cập nhật hội thoại', exact: true }).click(); await expect(page.locator('.su-metadata')).toContainText('Cần bổ sung'); await page.getByLabel('Nội dung phản hồi').fill('Synthetic customer clarification'); await page.getByRole('button', { name: 'Gửi phản hồi', exact: true }).click(); await expect(page.locator('.su-metadata')).toContainText('Đang xử lý');
    await staffPage.getByRole('button', { name: 'Tải lại dữ liệu hỗ trợ', exact: true }).click(); await expect(staffPage.locator('.su-conversation')).toContainText('Synthetic customer clarification'); await staffPage.getByLabel('Nội dung công khai cho khách hàng').fill('Synthetic first resolution'); await staffPage.getByRole('button', { name: 'Gửi kết quả giải quyết', exact: true }).click(); await expect(staffPage.locator('.su-metadata')).toContainText('Đã giải quyết');
    await page.getByRole('button', { name: 'Cập nhật hội thoại', exact: true }).click(); await page.getByLabel('Nội dung đề nghị xem xét lại').fill('Synthetic review request'); await page.getByRole('button', { name: 'Đề nghị xem xét lại', exact: true }).click(); await expect(page.locator('.su-conversation')).toContainText('Synthetic review request'); await expect(page.getByLabel('Nội dung phản hồi')).toHaveCount(0);
    await staffPage.getByRole('button', { name: 'Tải lại dữ liệu hỗ trợ', exact: true }).click(); await staffPage.getByLabel('Nội dung công khai cho khách hàng').fill('Synthetic review accepted'); await staffPage.getByRole('button', { name: 'Chấp nhận xem xét lại', exact: true }).click(); await expect(staffPage.locator('.su-metadata')).toContainText('Đang xử lý');
    await staffPage.getByLabel('Nội dung công khai cho khách hàng').fill('Synthetic final resolution'); await staffPage.getByRole('button', { name: 'Gửi kết quả giải quyết', exact: true }).click(); await expect(staffPage.locator('.su-metadata')).toContainText('Đã giải quyết');
    await page.getByRole('button', { name: 'Cập nhật hội thoại', exact: true }).click(); page.once('dialog', dialog => dialog.accept()); await page.getByRole('button', { name: 'Xác nhận kết quả & Đóng yêu cầu', exact: true }).click(); await expect(page.locator('.su-metadata')).toContainText('Đã đóng'); await expect(page.locator('.su-conversation li')).toHaveCount(10); await page.reload(); await expect(page.locator('.su-metadata')).toContainText('Đã đóng');
    await staffPage.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(staffPage.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible();
  } finally { await staff.close(); }
  await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Tổng quan', exact: true }).click();
});

test('business workspace uses real organization membership without reusing Personal facts', async ({ browser }) => {
  const business = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false }), businessPage = await business.newPage();
  const first = '10000000-0000-4000-8000-000000000901', second = '10000000-0000-4000-8000-000000000902';
  try {
    await businessPage.goto('/login'); await businessPage.getByLabel('Email đã xác minh').fill('browser-staff@example.invalid'); await businessPage.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-session-password'); await businessPage.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await expect(businessPage.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
    await businessPage.getByRole('button', { name: 'ArgoPost Personal', exact: true }).click(); await businessPage.locator('a[href="/merchant/' + first + '/overview"]').click();
    await expect(businessPage.locator('.bw-kpi')).toHaveCount(4); await expect(businessPage.locator('.bw-kpi strong')).toHaveText(['—', '—', '—', '—']);
    await expect(businessPage.getByRole('heading', { name: 'Chi phí vận chuyển', exact: true })).toBeVisible(); await expect(businessPage.getByRole('button', { name: 'Tạo vận đơn', exact: true })).toBeDisabled();
    await businessPage.getByRole('button', { name: '7 ngày', exact: true }).click(); await expect(businessPage.locator('.bw-kpi')).toHaveCount(4); await businessPage.getByLabel('Thống kê theo').selectOption('WEEK'); await expect(businessPage.locator('.bw-main-chart')).toContainText('Theo tuần');
    await businessPage.getByRole('navigation', { name: 'Điều hướng Business' }).getByRole('link', { name: 'Tạo vận đơn', exact: true }).click();
    for (const kind of ['người gửi', 'người nhận']) {
      await businessPage.getByLabel('Họ tên ' + kind, { exact: true }).fill('Synthetic Business ' + kind);
      await businessPage.getByLabel('Số điện thoại ' + kind, { exact: true }).fill('0901234567');
      await businessPage.getByLabel('Địa chỉ chi tiết (' + kind + ')', { exact: true }).fill('Synthetic Business street');
      await businessPage.getByLabel('Tỉnh / thành phố (' + kind + ')', { exact: true }).fill('Synthetic area');
      await businessPage.getByLabel('Phường / xã (' + kind + ')', { exact: true }).fill('Synthetic locality');
    }
    await businessPage.getByLabel('Mô tả kiện 1', { exact: true }).fill('Synthetic Business parcel');
    for (const field of ['Trọng lượng (g)', 'Dài (cm)', 'Rộng (cm)', 'Cao (cm)']) await businessPage.getByLabel(field + ' — kiện 1', { exact: true }).fill(field === 'Trọng lượng (g)' ? '500' : '10');
    await businessPage.getByLabel('Mã tham chiếu', { exact: true }).fill('SYNTHETIC-BUSINESS-ONLY');
    let businessWrites = 0;
    await businessPage.route('**/api/v1/merchant-organizations/' + first + '/shipment-preparations', async route => {
      if (route.request().method() !== 'POST') return route.continue();
      businessWrites++; await route.fetch(); await route.abort('failed');
    });
    await businessPage.getByRole('button', { name: 'Lưu bản nháp doanh nghiệp', exact: true }).click();
    await expect(businessPage.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true })).toBeVisible();
    await expect(businessPage.getByRole('button', { name: 'Lưu bản nháp doanh nghiệp', exact: true })).toBeDisabled();
    await businessPage.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true }).click();
    await expect(businessPage).toHaveURL(/create\?draft=/); expect(businessWrites).toBe(1);
    await businessPage.reload(); await expect(businessPage.getByLabel('Mã tham chiếu', { exact: true })).toHaveValue('SYNTHETIC-BUSINESS-ONLY');
    await businessPage.getByLabel('Ghi chú', { exact: true }).fill('Synthetic revised instructions');
    await businessPage.getByRole('button', { name: 'Lưu bản nháp doanh nghiệp', exact: true }).click();
    await expect(businessPage.getByRole('status')).toContainText('Đã lưu bản nháp doanh nghiệp');
    await businessPage.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/business-preparation-real.png', fullPage: true, animations: 'disabled' });
    businessPage.once('dialog', dialog => dialog.accept()); await businessPage.getByRole('button', { name: 'Lưu trữ bản nháp', exact: true }).click();
    await expect(businessPage.getByRole('heading', { name: 'Chưa có bản nháp', exact: true })).toBeVisible();
    await businessPage.getByRole('navigation', { name: 'Điều hướng Business' }).getByRole('link', { name: 'Nhập file', exact: true }).click();
    const templateDownload = businessPage.waitForEvent('download'); await businessPage.getByRole('button', { name: 'Tải file mẫu', exact: true }).click(); await businessPage.getByRole('button', { name: /CSV UTF-8/ }).click();
    expect((await templateDownload).suggestedFilename()).toBe('argopost-business-mau.csv');
    const headers = ['source_channel', 'source_order_reference', 'sender_name', 'sender_phone', 'sender_country', 'sender_administrative_area', 'sender_locality', 'sender_address_line', 'recipient_name', 'recipient_phone', 'recipient_country', 'recipient_administrative_area', 'recipient_locality', 'recipient_address_line', 'parcels_json', 'reference', 'instructions'];
    const csvRows = [1, 2].map(n => ['BROWSER_CSV', 'CSV-' + n, n === 1 ? 'Synthetic sender' : '=Synthetic invalid formula', '0901234567', 'VN', 'Synthetic area', 'Synthetic locality', 'Synthetic sender street', 'Synthetic recipient', '0901234567', 'VN', 'Synthetic destination', 'Synthetic locality', 'Synthetic recipient street', JSON.stringify([{ description: 'Synthetic imported parcel', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }]), 'SYNTHETIC-IMPORT-' + n, ''].map(v => '"' + v.replaceAll('"', '""') + '"').join(','));
    const csv = headers.join(',') + '\r\n' + csvRows.join('\r\n') + '\r\n';
    await businessPage.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic.csv', mimeType: 'text/csv', buffer: Buffer.from(csv) });
    await expect(businessPage.getByLabel('Điện thoại người gửi — dòng 1', { exact: true })).toHaveValue('0901234567');
    await businessPage.getByRole('button', { name: 'Xuất bảng hiện tại', exact: true }).click();
    const excelDownload = businessPage.waitForEvent('download'); await businessPage.getByRole('button', { name: /Excel .xlsx/ }).click();
    const exportedExcel = await excelDownload; await businessPage.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: readFileSync((await exportedExcel.path())!) });
    await expect(businessPage.getByLabel('Mã đơn nguồn — dòng 2', { exact: true })).toHaveValue('CSV-2');
    let previews = 0, commits = 0;
    await businessPage.route('**/api/v1/merchant-organizations/' + first + '/shipment-imports', async route => { if (route.request().method() !== 'POST') return route.continue(); previews++; const actual = await route.fetch({ maxRetries: 0 }); expect(actual.status()).toBe(201); await route.abort('failed'); });
    await businessPage.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click();
    await expect(businessPage.getByRole('button', { name: 'Kiểm tra kết quả nhập', exact: true })).toBeVisible(); await businessPage.getByRole('button', { name: 'Kiểm tra kết quả nhập', exact: true }).click();
    await expect(businessPage.locator('.bp-import-totals dd')).toHaveText(['2', '1', '1', '0', '0']); expect(previews).toBe(1);
    await businessPage.getByLabel('Chọn dòng 2', { exact: true }).check(); await expect(businessPage.getByRole('button', { name: 'Lưu 0 dòng thành nháp', exact: true })).toBeDisabled();
    await businessPage.getByRole('button', { name: 'Chọn tất cả dòng hợp lệ', exact: true }).click();
    await businessPage.route('**/api/v1/merchant-organizations/' + first + '/shipment-imports/*/commit', async route => { commits++; const actual = await route.fetch({ maxRetries: 0 }); expect(actual.status()).toBe(200); await route.abort('failed'); });
    businessPage.once('dialog', dialog => dialog.accept()); await businessPage.getByRole('button', { name: 'Lưu 1 dòng thành nháp', exact: true }).click();
    await expect(businessPage.getByRole('button', { name: 'Kiểm tra kết quả nhập', exact: true })).toBeVisible(); await businessPage.getByRole('button', { name: 'Kiểm tra kết quả nhập', exact: true }).click();
    await expect(businessPage.locator('.bp-import-totals dd')).toHaveText(['2', '0', '1', '1', '0']); expect(commits).toBe(1);
    await businessPage.reload(); await expect(businessPage.locator('.bp-import-totals dd')).toHaveText(['2', '0', '1', '1', '0']);
    await businessPage.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/business-import-real.png', fullPage: true, animations: 'disabled' });
    await businessPage.getByRole('button', { name: 'Synthetic organization', exact: true }).click(); await businessPage.locator('a[href="/merchant/' + second + '/overview"]').click(); await expect(businessPage).toHaveURL(new RegExp(second + '/overview$'));
    await expect(businessPage.locator('.bw-kpi')).toHaveCount(4); await expect(businessPage.getByRole('heading', { name: 'Chi phí vận chuyển', exact: true })).toHaveCount(0); await expect(businessPage.getByRole('heading', { name: 'Hoạt động nhân viên', exact: true })).toHaveCount(0);
    await businessPage.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/business-real-limited-context.png', fullPage: true, animations: 'disabled' });
    await businessPage.getByRole('button', { name: 'Synthetic organization', exact: true }).click(); await businessPage.getByRole('link', { name: /ArgoPost Personal/ }).click(); await expect(businessPage.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
    await businessPage.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(businessPage.getByLabel('Email đã xác minh')).toBeVisible();
  } finally { await business.close(); }
});
test('personal address book persists CRUD and resolves a lost create through its real receipt', async () => {
  await page.getByRole('button', { name: 'Quản lý', exact: true }).click();
  await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Sổ địa chỉ', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Sổ địa chỉ của bạn đang trống' })).toBeVisible();
  await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click();
  await page.getByLabel('Tên gợi nhớ', { exact: false }).fill('Synthetic browser address'); await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Synthetic browser contact'); await page.getByLabel('Số điện thoại liên hệ', { exact: true }).fill('0901234567'); await page.getByLabel('Tỉnh / thành phố').fill('Synthetic area'); await page.getByLabel('Phường / xã / địa phương').fill('Synthetic locality'); await page.getByLabel('Địa chỉ chi tiết').fill('Synthetic street'); await page.getByRole('checkbox').check();
  let writes = 0;
  await page.route('**/api/v1/customer/addresses', async route => {
    if (route.request().method() !== 'POST') { await route.continue(); return; }
    writes++; const committed = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(committed.status()).toBe(201); await route.abort('failed');
  });
  await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Họ tên liên hệ', { exact: true })).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' }).click();
  await expect(page.getByText('Đã lưu địa chỉ.', { exact: true })).toBeVisible(); expect(writes).toBe(1); await page.unroute('**/api/v1/customer/addresses');
  await page.reload(); await expect(page.locator('.ab-card')).toContainText('Synthetic browser address'); await expect(page.locator('.ab-card')).toContainText('+84901234567'); await expect(page.locator('.ab-card')).toContainText('Chưa xác minh');
  await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click(); await page.getByLabel('Tên gợi nhớ', { exact: false }).fill('Updated browser address'); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByText('Đã lưu địa chỉ.', { exact: true })).toBeVisible(); await page.reload(); await expect(page.locator('.ab-card')).toContainText('Updated browser address');
  await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click(); await page.getByRole('checkbox').uncheck(); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.locator('.ab-default')).toHaveCount(0);
  await page.getByRole('button', { name: 'Đặt mặc định', exact: true }).click(); await expect(page.locator('.ab-default')).toBeVisible();
  await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click();
  await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Synthetic browser contact'); await page.getByLabel('Số điện thoại liên hệ', { exact: true }).fill('0901-234-567'); await page.getByLabel('Tỉnh / thành phố').fill('Synthetic area'); await page.getByLabel('Phường / xã / địa phương').fill('Synthetic locality'); await page.getByLabel('Địa chỉ chi tiết', { exact: true }).fill('Synthetic street');
  await page.getByRole('button', { name: 'Chuẩn hóa cách nhập', exact: true }).click(); await expect(page.getByLabel('Số điện thoại liên hệ', { exact: true })).toHaveValue('+84901234567');
  await page.getByRole('button', { name: 'Kiểm tra trùng liên hệ', exact: true }).click(); await expect(page.locator('.ab-check-result')).toContainText('Có liên hệ đang hoạt động trùng');
  page.once('dialog', dialog => dialog.accept()); await page.getByRole('button', { name: 'Đóng chỉnh sửa', exact: true }).click();
  await page.getByRole('button', { name: 'Dùng tạo vận đơn', exact: true }).click(); await expect(page.locator('#sender-name')).toHaveValue('Synthetic browser contact'); await expect(page.locator('#sender-addressLine')).toHaveValue('Synthetic street');
  await page.locator('.se-contact').first().getByRole('link', { name: 'Sổ địa chỉ', exact: false }).click();
  await expect(page.locator('.ab-card')).toHaveCount(1);
  await page.getByRole('button', { name: 'Lưu trữ', exact: true }).click(); await page.getByRole('button', { name: 'Xác nhận lưu trữ', exact: true }).click(); await expect(page.getByText('Đã lưu trữ địa chỉ.', { exact: true })).toBeVisible(); await page.reload(); await expect(page.getByRole('heading', { name: 'Sổ địa chỉ của bạn đang trống' })).toBeVisible();
  await page.goto('/personal/action-required'); await expect(page.getByRole('heading', { name: 'Chưa có nguồn yêu cầu xử lý' })).toBeVisible(); await page.getByRole('button', { name: 'Yêu cầu đã gửi', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chưa mở theo dõi yêu cầu đã gửi' })).toBeVisible();
});
test('personal shipping preparation persists address snapshots parcels and unknown-write recovery', async () => {
  await page.goto('/personal/addresses'); await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click();
  await page.getByLabel('Tên gợi nhớ', { exact: false }).fill('Draft sender source'); await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Synthetic draft sender'); await page.getByLabel('Số điện thoại liên hệ', { exact: true }).fill('0901234567'); await page.getByLabel('Tỉnh / thành phố').fill('Synthetic area'); await page.getByLabel('Phường / xã / địa phương').fill('Synthetic locality'); await page.getByLabel('Địa chỉ chi tiết').fill('Synthetic draft street'); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByText('Đã lưu địa chỉ.', { exact: true })).toBeVisible();
  await page.goto('/personal/create'); await page.locator('#sender-book').selectOption({ label: 'Draft sender source · +84901234567' }); await expect(page.locator('#sender-name')).toHaveValue('Synthetic draft sender');
  await page.locator('#recipient-name').fill('Synthetic draft recipient'); await page.locator('#recipient-phone').fill('0901234567'); await page.locator('#recipient-administrativeArea').fill('Synthetic destination'); await page.locator('#recipient-locality').fill('Synthetic locality'); await page.locator('#recipient-addressLine').fill('Synthetic recipient street'); await page.locator('#parcel-description-0').fill('Synthetic parcel one'); for (const [field, value] of Object.entries({ weightGrams: '500', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + field + '0').fill(value); await page.getByRole('button', { name: '+ Thêm kiện (tối đa 5)', exact: true }).click(); await page.locator('#parcel-description-1').fill('Synthetic parcel two'); for (const [field, value] of Object.entries({ weightGrams: '500', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + field + '1').fill(value); await page.locator('#draft-reference').fill('Browser preparation');
  let writes = 0;
  await page.route('**/api/v1/customer/shipment-drafts', async route => { if (route.request().method() !== 'POST') { await route.continue(); return; } writes++; const actual = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(actual.status()).toBe(201); await route.abort('failed'); });
  await page.getByRole('button', { name: 'Lưu bản nháp', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.locator('#sender-name')).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra kết quả lưu', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết bản nháp', exact: true })).toBeVisible(); expect(writes).toBe(1); await page.unroute('**/api/v1/customer/shipment-drafts');
  await page.reload(); await expect(page.getByRole('heading', { name: 'Browser preparation', exact: true })).toBeVisible(); await page.getByRole('tab', { name: 'Kiện hàng', exact: true }).click(); await expect(page.getByRole('tabpanel')).toContainText('Synthetic parcel two'); await page.getByRole('tab', { name: 'Cước & COD', exact: true }).click(); await expect(page.getByRole('tabpanel')).toContainText('Chưa có báo giá');
  await page.getByRole('link', { name: 'Tiếp tục chỉnh sửa' }).click(); await page.locator('#draft-reference').fill('Browser preparation edited'); await page.getByRole('button', { name: 'Lưu bản nháp', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Browser preparation edited', exact: true })).toBeVisible(); page.once('dialog', dialog => dialog.accept()); await page.getByRole('button', { name: 'Lưu trữ bản nháp', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Bạn chưa có bản nháp', exact: true })).toBeVisible();
});
test('approved simulation quote booking persists shipment parcels and UNPAID obligation with receipt recovery', async () => {
  await page.goto('/personal/lookup?view=estimate'); await page.getByLabel('Nơi gửi', { exact: true }).selectOption('SIM-A1'); await page.getByLabel('Nơi nhận', { exact: true }).selectOption('SIM-A2');
  await page.getByLabel('Khối lượng kiện 1', { exact: true }).fill('501'); for (const field of ['Dài', 'Rộng', 'Cao']) await page.getByLabel(field + ' kiện 1', { exact: true }).fill('10');
  await page.getByRole('button', { name: 'Thêm kiện', exact: true }).click(); await page.getByLabel('Khối lượng kiện 2', { exact: true }).fill('500'); for (const field of ['Dài', 'Rộng', 'Cao']) await page.getByLabel(field + ' kiện 2', { exact: true }).fill('10');
  await page.getByRole('button', { name: 'Tính cước tham khảo', exact: true }).click(); await expect(page.locator('.st-estimate-total')).toContainText('53.000');
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/tools-estimate-real.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Dùng thông tin này tạo vận đơn' }).click(); await expect(page.locator('#weightGrams0')).toHaveValue('501'); await expect(page.locator('#weightGrams1')).toHaveValue('500'); await expect(page.locator('#sender-name')).toHaveValue('');
  await page.locator('#sender-book').selectOption({ label: 'Draft sender source · +84901234567' });
  await page.locator('#recipient-name').fill('Synthetic pilot recipient'); await page.locator('#recipient-phone').fill('0901234567'); await page.locator('#recipient-administrativeArea').fill('Synthetic address area'); await page.locator('#recipient-locality').fill('Synthetic address locality'); await page.locator('#recipient-addressLine').fill('Synthetic pilot address'); await page.locator('#parcel-description-0').fill('Ordinary test parcel one'); for (const [field, value] of Object.entries({ weightGrams: '501', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + field + '0').fill(value); await page.locator('#weightGrams0').fill('501');  await page.locator('#parcel-description-1').fill('Ordinary test parcel two'); for (const [field, value] of Object.entries({ weightGrams: '500', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + field + '1').fill(value); await page.locator('#draft-reference').fill('Pilot booking browser'); await page.getByRole('button', { name: 'Tiếp tục báo giá →', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Xác nhận thông tin & báo giá', exact: true })).toBeVisible(); await expect(page).toHaveURL(/\/personal\/create$/);
  await page.locator('#pilot-origin').selectOption('SIM-A1'); await page.locator('#pilot-destination').selectOption('SIM-A2'); await page.getByLabel('Tôi xác nhận chỉ khai hàng thông thường', { exact: false }).check();
  let quoteWrites = 0, bookingWrites = 0;
  await page.route('**/api/v1/customer/shipment-drafts/*/quotes', async route => { quoteWrites++; const actual = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(actual.status()).toBe(201); await route.abort('failed'); });
  await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await page.getByRole('button', { name: 'Kiểm tra kết quả báo giá', exact: true }).click(); await expect(page.locator('.pb-total')).toContainText('53.000 VND'); expect(quoteWrites).toBe(1); await page.unroute('**/api/v1/customer/shipment-drafts/*/quotes');
  await page.route('**/api/v1/customer/shipment-drafts/*/bookings', async route => { bookingWrites++; const actual = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 }); expect(actual.status()).toBe(201); await route.abort('failed'); });
  await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await page.getByRole('button', { name: 'Kiểm tra kết quả booking', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn', exact: true })).toBeVisible(); expect(bookingWrites).toBe(1); await page.unroute('**/api/v1/customer/shipment-drafts/*/bookings');
  await page.reload(); await expect(page.locator('.pb-shipment-heading')).toContainText('AP-SIM-'); await expect(page.locator('.pb-shipment-heading')).toContainText('UNPAID'); await page.getByRole('tab', { name: 'Kiện hàng', exact: true }).click(); await expect(page.getByRole('tabpanel')).toContainText('Ordinary test parcel two'); await page.getByRole('tab', { name: 'Cước & COD', exact: true }).click(); await expect(page.getByRole('tabpanel')).toContainText('53.000 VND'); await expect(page.getByRole('tabpanel')).toContainText('UNPAID'); await expect(page.getByRole('button', { name: 'Thanh toán', exact: true })).toHaveCount(0);
  await page.goto('/personal/overview');
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('1');
  await expect(page.locator('.an-kpi')).toHaveCount(4);
  await expect(page.locator('[data-metric=UNPAID_OBLIGATIONS] .an-kpi-value')).toContainText('53.000');
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/analytics-real-overview.png', fullPage: true, animations: 'disabled' });
  await page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value').click();
  await expect(page).toHaveURL(/\/personal\/overview\/shipments\?/);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1);
  await expect(page.locator('.an-kpi,.an-time-chart,.an-dimension-nav')).toHaveCount(0);
  await expect(page.locator('.an-source tbody tr')).toContainText('2 kiện');
  await expect(page.locator('.an-source tbody tr')).toContainText('53.000');
  await page.locator('.an-source tbody td:first-child a').click();
  await page.getByRole('link', { name: 'Quay lại danh sách', exact: true }).click();
  await expect(page).toHaveURL(/\/personal\/overview\/shipments\?/);
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1);
  await page.goto('/personal/shipments'); await page.getByLabel('Mã vận đơn / tham chiếu / liên hệ').fill('Pilot booking browser'); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await page.locator('#pilot-status').selectOption('WAITING_FOR_HANDOVER'); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.locator('.pm-table')).toContainText('AP-SIM-'); await expect(page.locator('.pm-table')).toContainText('UNPAID');
  await expect(page.getByRole('button', { name: /Tổng vận đơn/ })).toContainText('1');
  const managementDay = new Date(Date.now() + 7 * 3600000).toISOString().slice(0, 10);
  await page.getByLabel('Người nhận', { exact: true }).fill('Synthetic pilot recipient');
  await page.getByLabel('Từ ngày', { exact: true }).fill(managementDay); await page.getByLabel('Đến ngày', { exact: true }).fill(managementDay);
  await page.getByLabel('Dịch vụ', { exact: true }).selectOption('ROAD_STANDARD'); await page.getByLabel('Tuyến gửi hàng', { exact: true }).selectOption('SIM-A1/SIM-A2'); await page.getByLabel('Nghĩa vụ cước', { exact: true }).selectOption('UNPAID'); await page.getByLabel('Sắp xếp', { exact: true }).selectOption('FEE_DESC');
  await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click(); await expect(page.locator('.pm-table tbody tr')).toHaveCount(1); await expect(page.locator('.pm-table')).toContainText('53.000 ₫');
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/management-real-booked.png', fullPage: true, animations: 'disabled' });
  const listUrl = page.url();
  await page.locator('.pm-table tbody tr').first().getByRole('link', { name: 'Hành trình', exact: true }).click();
  await expect(page.getByRole('tab', { name: 'Hành trình', exact: true })).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('link', { name: 'Danh sách vận đơn', exact: true }).click(); await expect(page).toHaveURL(listUrl);
  await expect(page.locator('.pm-table tbody tr')).toHaveCount(1);
  await page.locator('.pm-table tbody input[type=checkbox]').first().check();
  const csvDownload = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV đã chọn', exact: true }).click();
  const csv = await csvDownload; await csv.saveAs(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/management-source.csv');
  const csvText = readFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/management-source.csv', 'utf8');
  expect(csvText).toContain('LOCAL/TEST SIMULATION'); expect(csvText).toContain('53000'); expect(csvText).toContain('UNPAID'); expect(csvText).not.toContain('"PAID"');
  const zipDownload = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click();
  const zip = await zipDownload; await zip.saveAs(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/management-labels.zip');
  const zipBytes = readFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/management-labels.zip');
  expect(zipBytes.readUInt32LE(0)).toBe(0x04034b50); expect(zipBytes.readUInt16LE(zipBytes.length - 12)).toBe(1);
  await expect(page.locator('.pm-downloads [role=status]')).toContainText('Đã tải nhãn của 1 vận đơn');

  await page.goto('/personal/reports?section=parcels');
  await expect(page.locator('.sr-totals')).toContainText('1.001'); await expect(page.locator('.sr-totals')).toContainText('1.500');
  await page.getByRole('button', { name: 'Thời gian', exact: true }).click();
  await expect(page.locator('.sr-analysis')).toContainText('0 vận đơn đã tiếp nhận / 1 vận đơn'); await expect(page.locator('.sr-totals')).toContainText('N/A');
  await page.getByRole('button', { name: 'Sản lượng', exact: true }).click(); await page.getByLabel('Nhóm báo cáo', { exact: true }).selectOption('quarter');
  await page.locator('.sr-volume-grid').getByRole('link', { name: /1 vận đơn, 2 kiện/ }).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1); await expect(page.locator('.an-source tbody')).toContainText('53.000');
  await page.getByRole('navigation', { name: 'Đường dẫn phân tích' }).getByRole('link', { name: 'Báo cáo gửi hàng', exact: true }).click();
  await expect(page.getByLabel('Nhóm báo cáo')).toHaveValue('quarter');
  const reportDownload = page.waitForEvent('download'); await page.getByRole('button', { name: 'Xuất CSV', exact: true }).click();
  await (await reportDownload).saveAs(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/shipping-report.csv');
  const reportText = readFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/shipping-report.csv', 'utf8');
  expect(reportText).toContain('LOCAL/TEST SIMULATION'); expect(reportText).toContain('53000'); expect(reportText).not.toContain('example.invalid');
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/shipping-reports-real.png', fullPage: true, animations: 'disabled' });
  await page.goto('/personal/billing'); await expect(page.locator('.an-kpi')).toHaveCount(3); await expect(page.locator('[data-metric=UNPAID_OBLIGATIONS]')).toContainText('53.000'); await expect(page.getByRole('button', { name: /Rút tiền|Thanh toán/ })).toHaveCount(0);
  await page.getByRole('button', { name: 'Chi tiết cước', exact: true }).click();
  await expect(page.locator('.sf-snapshot tbody tr')).toHaveCount(2); await expect(page.locator('.sf-snapshot tbody')).toContainText('501 g'); await expect(page.locator('.sf-snapshot tbody')).toContainText('1.000 g');
  await expect(page.locator('.sf-snapshot')).toContainText('53.000');
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/shipping-fees-real.png', fullPage: true, animations: 'disabled' });
  await page.goto('/personal/labels'); await expect(page.locator('.st-label-list tbody tr')).toHaveCount(1); await expect(page.locator('.st-label-list tbody')).toContainText('2 kiện · Có thể có trang tiếp nối');
  const lookupCode = (await page.locator('.st-label-list tbody td.st-code').innerText()).trim();
  await page.getByLabel('Chọn tất cả nhãn trên trang').check(); const toolsZip = page.waitForEvent('download'); await page.getByRole('button', { name: 'Tải nhãn đã chọn (.zip)', exact: true }).click(); await (await toolsZip).saveAs(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/tools-labels.zip');
  const toolsBytes = readFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/tools-labels.zip'); expect(toolsBytes.readUInt32LE(0)).toBe(0x04034b50); expect(toolsBytes.readUInt16LE(toolsBytes.length - 12)).toBe(1);
  await page.getByRole('button', { name: 'Xem / In nhãn', exact: true }).click(); await page.getByRole('button', { name: 'Tải / In nhãn', exact: true }).click(); await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toHaveAttribute('href', /^blob:/);
  await page.screenshot({ path: process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/tools-labels-real.png', fullPage: true, animations: 'disabled' });
  await page.goto('/personal/lookup'); await page.getByLabel('Mã vận đơn', { exact: true }).fill(lookupCode + '\nAP-SIM-' + 'f'.repeat(32)); await page.locator('.st-search').getByRole('button', { name: 'Tra cứu vận đơn', exact: true }).click(); await expect(page.locator('.st-tracking-results>article')).toHaveCount(2); await expect(page.locator('.st-tracking-results')).toContainText('Không tìm thấy vận đơn phù hợp hoặc bạn không có quyền'); await expect(page.locator('.st-tracking-results')).not.toContainText('Synthetic pilot recipient');
  await page.getByRole('button', { name: 'Xem trạng thái & hành trình' }).click(); await expect(page.locator('.sl-timeline')).toContainText('Booking đã xác nhận'); await expect(page.locator('.sl-timeline')).not.toContainText('Carrier tiếp nhận');
  await page.goto('/personal/shipments'); await expect(page.locator('.pm-table tbody tr')).toHaveCount(1);

});
test('station staff scans all parcel labels and atomically accepts custody with visible Personal tracking', async ({ browser }) => {
  let labelStage = 'LABEL_NAVIGATION';
  let shipmentPath = '', codes: string[] = [];
  try {
  await page.getByRole('link', { name: 'Xem vận đơn →', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn', exact: true })).toBeVisible();
  shipmentPath = new URL(page.url()).pathname;
  await page.getByRole('tab', { name: 'Kiện hàng', exact: true }).click();
  await expect(page.locator('code.sl-code')).toHaveCount(2);
  codes = await page.locator('code.sl-code').allTextContents();expect(codes).toHaveLength(2);
  // Independent read-only observations, not retries: any failure aborts the journey immediately.
  for (let labelRead = 0; labelRead < 5; labelRead++) {
  const save = page.getByRole('link', { name: 'Lưu PDF', exact: true }), previousUrl = await save.count() ? await save.getAttribute('href') : null;
  const pdfResponse = page.waitForResponse(r => r.url().endsWith('/labels.pdf'));
  labelStage = 'LABEL_REQUEST';
  await page.getByRole('button', { name: 'Tải / In nhãn', exact: true }).click();
  labelStage = 'LABEL_RESPONSE_WAIT';const pdf = await pdfResponse;
  labelStage = 'LABEL_HTTP_' + pdf.status();expect(pdf.status()).toBe(200);
  labelStage = 'LABEL_MIME';expect(pdf.headers()['content-type']).toBe('application/pdf');
  labelStage = 'LABEL_PRIVACY';expect(pdf.headers()['cache-control']).toContain('no-store');expect(pdf.headers()['content-disposition']).toContain('attachment;');
  labelStage = 'LABEL_ACTIONS';
  await expect(page.getByRole('link', { name: 'Lưu PDF', exact: true })).toBeVisible();await expect(page.getByRole('link', { name: 'Mở bản in PDF', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tải / In nhãn', exact: true })).toBeEnabled();if (previousUrl) await expect(save).not.toHaveAttribute('href', previousUrl);
  labelStage = 'LABEL_DOWNLOAD';const downloadEvent = page.waitForEvent('download');await page.getByRole('link', { name: 'Lưu PDF', exact: true }).click();const download = await downloadEvent;
  labelStage = 'LABEL_FILE_BINDING';expect(download.suggestedFilename()).toBe('argopost-simulation-' + shipmentPath.split('/').at(-1) + '.pdf');
  const target = process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/customer-label-' + labelRead + '.pdf';await download.saveAs(target);
  labelStage = 'LABEL_FILE_MAGIC';const bytes = readFileSync(target);expect(bytes.byteLength).toBeGreaterThan(5);expect(bytes.byteLength).toBeLessThanOrEqual(12 * 1024 * 1024);expect(bytes.subarray(0, 5).toString()).toBe('%PDF-');
  }
  } catch (error) {
    try { writeFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/label-failure-stage.json', JSON.stringify({ stage: labelStage })); } catch { /* Preserve the original assertion/transport failure. */ }
    throw error;
  }
  await page.goto('/operations/station');await expect(page.getByRole('alert')).toContainText('chưa có permission');
  const staff = await browser.newContext({ baseURL: origin, ignoreHTTPSErrors: false });const stationPage = await staff.newPage();
  try {
    await stationPage.goto('/operations/station');await stationPage.getByLabel('Email đã xác minh', { exact: true }).fill('browser-staff@example.invalid');await stationPage.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-session-password');const staffLogin = stationPage.waitForResponse(r => r.url().endsWith('/customer-auth/login') && r.request().method() === 'POST');await stationPage.getByRole('button', { name: 'Đăng nhập', exact: true }).click();expect((await staffLogin).status()).toBe(200);
    await expect(stationPage.getByRole('heading', { name: 'Tiếp nhận tại Station', exact: true })).toBeVisible();await expect(stationPage.locator('#intake-station')).toHaveValue('SIM-DROP-A1');
    let scanWrites = 0, acceptanceWrites = 0;
    await stationPage.route('**/api/v1/station-operations/stations/SIM-DROP-A1/scans', async route => { scanWrites++;const actual = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 });expect(actual.status()).toBe(200);await route.abort('failed'); });
    await stationPage.getByLabel('Scan hoặc nhập mã kiện', { exact: true }).fill(codes[0]);await stationPage.getByRole('button', { name: 'Scan & đối chiếu kiện', exact: true }).click();await expect(stationPage.getByRole('heading', { name: 'Kết quả chưa xác định', exact: true })).toBeVisible();await stationPage.getByRole('button', { name: 'Kiểm tra kết quả đã lưu', exact: true }).click();await expect(stationPage.getByText('1/2 kiện đã scan', { exact: false })).toBeVisible();expect(scanWrites).toBe(1);await stationPage.unroute('**/api/v1/station-operations/stations/SIM-DROP-A1/scans');
    await expect(stationPage.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true })).toBeDisabled();
    await stationPage.getByLabel('Scan hoặc nhập mã kiện', { exact: true }).fill(codes[1]);await stationPage.getByRole('button', { name: 'Scan & đối chiếu kiện', exact: true }).click();await expect(stationPage.getByText('2/2 kiện đã scan', { exact: false })).toBeVisible();
    await stationPage.getByRole('checkbox').check();
    await stationPage.route('**/api/v1/station-operations/stations/SIM-DROP-A1/shipments/*/acceptance', async route => { acceptanceWrites++;const actual = await route.fetch({ maxRedirects: 0, maxRetries: 0, timeout: 15000 });expect(actual.status()).toBe(200);await route.abort('failed'); });
    await stationPage.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true }).click();await expect(stationPage.getByRole('heading', { name: 'Kết quả chưa xác định', exact: true })).toBeVisible();await stationPage.getByRole('button', { name: 'Kiểm tra kết quả đã lưu', exact: true }).click();await expect(stationPage.locator('.so-intake')).toContainText('Đã tiếp nhận tại Station');await expect(stationPage.locator('.so-intake')).toContainText('UNPAID');expect(acceptanceWrites).toBe(1);await stationPage.reload();await expect(stationPage.getByRole('heading', { name: 'Tiếp nhận tại Station', exact: true })).toBeVisible();
    await stationPage.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(stationPage.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu', exact: true })).toBeVisible();
  } catch (error) { writeFileSync(process.env.S003_BROWSER_PRIVATE_DIRECTORY! + '/station-page-private.json', JSON.stringify({ path: new URL(stationPage.url()).pathname, headings: await stationPage.locator('h1').allTextContents(), alerts: await stationPage.getByRole('alert').allTextContents() }));throw error; } finally { await staff.close(); }
  await page.goto(shipmentPath);await page.getByRole('tab', { name: 'Hành trình', exact: true }).click();await expect(page.getByRole('tabpanel')).toContainText('Carrier tiếp nhận tại Station');await expect(page.getByRole('tabpanel')).toContainText('SIM-DROP-A1');await page.reload();await page.getByRole('tab', { name: 'Lấy hàng & Giao hàng', exact: true }).click();await expect(page.getByRole('tabpanel')).toContainText('Đã tiếp nhận tại Station');await page.getByRole('tab', { name: 'Cước & COD', exact: true }).click();await expect(page.getByRole('tabpanel')).toContainText('UNPAID');
  await page.goto('/personal/overview');
  await expect(page.locator('[data-metric=ACCEPTED_SHIPMENTS] .an-kpi-value')).toContainText('1');
  await expect(page.locator('[data-metric=AWAITING_HANDOVER] .an-kpi-value')).toContainText('0');
  await page.locator('.an-status-legend').getByRole('link', { name: /Đã nhận tại Station/ }).click();
  await expect(page.locator('.an-source tbody tr')).toHaveCount(1);
  await expect(page.locator('.an-source tbody tr')).toContainText('Đã nhận tại Station');
  await page.goBack(); await expect(page).toHaveURL(/\/personal\/overview(?:\?|$)/); await expect(page.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible();
  const timingResponse = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/customer/personal-shipping-reports');
  await page.goto('/personal/reports?section=timing');
  await expect(page).toHaveURL(/\/personal\/reports\?section=timing$/); await expect(page.getByRole('heading', { name: 'Báo cáo gửi hàng', exact: true })).toBeVisible();
  const timingHttp = await timingResponse; expect(timingHttp.status()).toBe(200);
  await expect(page.locator('.sr-analysis')).toContainText('1 vận đơn đã tiếp nhận / 1 vận đơn'); await expect(page.locator('.sr-totals')).not.toContainText('N/A');
  await page.getByRole('link', { name: 'Xem vận đơn trong mẫu', exact: true }).click(); await expect(page.locator('.an-source tbody tr')).toHaveCount(1); await expect(page.locator('.an-source tbody')).toContainText('Đã nhận tại Station');
  await page.goto('/personal/shipments');await page.locator('#pilot-status').selectOption('AT_STATION'); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click();await expect(page.locator('.pm-table')).toContainText('Tại Station');await page.locator('#pilot-status').selectOption('WAITING_FOR_HANDOVER'); await page.getByRole('button', { name: 'Tìm vận đơn', exact: true }).click();await expect(page.getByRole('heading', { name: 'Không có vận đơn phù hợp', exact: true })).toBeVisible();
});

test('merchant draft uses the real HTTPS API and resolves one lost write', async () => {
  await page.locator('.pw-user summary').click(); await page.locator('.pw-user-dropdown').getByRole('link', { name: 'Hồ sơ Merchant', exact: true }).click();
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
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); await page.getByRole('link', { name: 'Quên mật khẩu?' }).click(); await page.getByLabel('Email của tài khoản').fill(email); await page.getByRole('button', { name: 'Yêu cầu khôi phục mật khẩu' }).click(); await expect(page).toHaveURL(/\/recover\/otp$/);await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeVisible();
});
test('recovery confirmation changes the credential with the actual second message', async () => {
  await page.getByLabel('Mã xác minh 6 chữ số').fill((await mail(1)).code);await page.getByRole('button',{name:'Tiếp tục',exact:true}).click();await expect(page).toHaveURL(/\/recover\/confirm$/); await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toBeVisible(); expect(new URL(page.url()).hash).toBe(''); await page.getByLabel('Mật khẩu mới', { exact: true }).fill(newPassword); await page.getByLabel('Nhập lại mật khẩu mới').fill(newPassword); await page.getByRole('button', { name: 'Xác nhận mật khẩu mới' }).click(); await expect(page.getByRole('status')).toContainText('Bạn đã đổi mật khẩu thành công!');await expect(page).toHaveURL(/\/login$/);
});
test('fresh login rejects the old credential and revoked browser loses its workspace', async () => {
  await secondaryPage.goto('/personal/profile'); await expect(secondaryPage.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); await expect(secondaryPage.getByLabel('Tên hiển thị trong hồ sơ')).toHaveCount(0);
  await expect(page).toHaveURL(/\/login$/); await login(page, originalPassword); await expect(page.getByRole('alert')).toContainText('Email hoặc mật khẩu chưa đúng'); await login(page, newPassword); await expect(page.getByRole('heading', { name: 'Tổng quan', exact: true })).toBeVisible(); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible();
});
test('unknown email and invalid proof do not fabricate account activation', async () => {
  await page.goto('/recover'); await page.getByLabel('Email của tài khoản').fill('unknown-browser@example.invalid'); await page.getByRole('button', { name: 'Yêu cầu khôi phục mật khẩu' }).click(); await expect(page).toHaveURL(/\/recover\/otp$/);await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeVisible();
  await page.goto('/verify-email#proofId=20000000-0000-4000-8000-000000000001&proof=' + 'A'.repeat(43)); await page.getByRole('button', { name: 'Xác nhận email của tôi' }).click(); await expect(page.getByRole('alert')).toContainText('không còn hiệu lực'); await expect(page.getByText('Email đã được xác minh.', { exact: true })).toHaveCount(0);
});
