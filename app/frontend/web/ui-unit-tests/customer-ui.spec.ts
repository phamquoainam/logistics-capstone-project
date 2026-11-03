import { test, expect } from '@playwright/test';
import type { BrowserContext, Route } from '@playwright/test';
import { analyticsFixture } from './support/analytics-fixture';
import { managementFixture } from './support/management-fixture';

// Explicit intercepted API fixtures: UI unit evidence only, never real BE/PG/full-Spec E2E PASS.
const account = '10000000-0000-4000-8000-000000000001', proofId = '10000000-0000-4000-8000-000000000010';
type Call = { path: string; method: string; key?: string; body?: Record<string, unknown> };
async function fixture(context: BrowserContext, authenticated = false, special?: (route: Route, call: Call) => Promise<boolean>) {
  const calls: Call[] = [];
  await context.route('**/api/v1/**', async route => {
    const request = route.request(), path = new URL(request.url()).pathname;
    const call: Call = { path, method: request.method(), key: request.headers()['idempotency-key'], body: request.postData() ? request.postDataJSON() : undefined }; calls.push(call);
    if (special && await special(route, call)) return;
    const reply = (value: unknown, status = 200, headers: Record<string, string> = {}) => route.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-ui-csrf' });
    if (path.endsWith('/refresh') || path.endsWith('/login')) return authenticated || path.endsWith('/login') ? reply({ accessToken: 'synthetic-ui-access', accountId: account, expiresAt: new Date(Date.now() + 900000).toISOString() }) : reply({ code: 'SESSION_INVALID' }, 401);
    if (path.endsWith('/logout')) return route.fulfill({ status: 204 });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Người dùng thử', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/personal-analytics')) return reply(analyticsFixture(new URL(request.url()).searchParams));
    if (path === '/api/v1/customer/shipments') return reply(managementFixture(new URL(request.url()).searchParams));
    if (path.endsWith('/profile')) return reply({ id: account, accountId: account, displayName: 'Hồ sơ thử', preferredLanguage: 'vi', version: 1 }, 200, { ETag: '"v1"' });
    if (path.endsWith('/recovery-verifications')) return reply({ proofId, proof: 'C'.repeat(43), expiresAt: new Date(Date.now() + 90000).toISOString() });
    if (path.endsWith('/verifications') || path.endsWith('/recovery-confirm')) return route.fulfill({ status: 204 });
    if (path.endsWith('/enrollments') || path.endsWith('/recovery') || path.endsWith('/verification-resend')) return reply({ accepted: true, requestId: proofId }, 202);
    return reply({ code: 'ISOLATED_FIXTURE_ROUTE_UNSUPPORTED' }, 503);
  });
  return calls;
}
test('registration validates password and prevents duplicate submission without claiming email delivery', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/register');
  expect(await page.getByRole('checkbox').locator('..').evaluate(element => getComputedStyle(element).display)).toBe('flex');
  expect(await page.getByRole('checkbox').locator('..').evaluate(element => getComputedStyle(element).flexDirection)).toBe('row');
  await page.screenshot({ path: '../../../.demo/spec003-implementation/customer-register-desktop.png', fullPage: true, animations: 'disabled' });
  await page.getByLabel('Họ và tên', { exact: true }).fill('Người dùng thử'); await page.getByLabel('Email', { exact: true }).fill('synthetic@example.invalid'); await page.getByLabel('Số điện thoại liên hệ').fill('0901-234-567'); await page.getByLabel('Mật khẩu', { exact: true }).fill('short'); await page.getByLabel('Nhập lại mật khẩu').fill('short'); await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Đăng ký ngay', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('15 ký tự'); expect(calls.filter(c => c.path.endsWith('/enrollments'))).toHaveLength(0);
  await page.getByLabel('Mật khẩu', { exact: true }).fill('synthetic-enrollment-password'); await page.getByRole('button', { name: 'Đăng ký ngay', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('chưa khớp'); expect(calls.filter(c => c.path.endsWith('/enrollments'))).toHaveLength(0);
  await page.getByLabel('Nhập lại mật khẩu').fill('synthetic-enrollment-password'); await page.getByRole('button', { name: 'Đăng ký ngay', exact: true }).dblclick();
  await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeVisible(); await expect(page).toHaveURL(/\/verify-email$/); expect(calls.filter(c => c.path.endsWith('/enrollments'))).toHaveLength(1); expect(calls.find(c => c.path.endsWith('/enrollments'))?.body?.phone).toBe('+84901234567'); await expect(page.locator('.cp-otp-intro')).toContainText('Nếu email đủ điều kiện'); await expect(page.getByRole('button', { name: /Gửi lại sau/ })).toBeDisabled();
  await page.screenshot({path:'../../../.demo/spec003-implementation/customer-otp-desktop.png',fullPage:true,animations:'disabled'});
});
test('verification strips fragment before API calls and requires explicit confirmation', async ({ page, context }) => {
  const calls = await fixture(context), proof = 'A'.repeat(43); const urls: string[] = []; page.on('request', request => urls.push(request.url()));
  await page.goto('/verify-email#proofId=' + proofId + '&proof=' + proof); await expect(page.getByRole('heading', { name: 'Xác minh email', exact: true })).toBeVisible();
  expect(new URL(page.url()).hash).toBe(''); await expect(page.locator('body')).not.toContainText(proof); expect(calls.filter(c => c.path.endsWith('/verifications'))).toHaveLength(0); expect(urls.some(url => url.includes(proof))).toBe(false);
  await page.getByRole('button', { name: 'Xác nhận email của tôi' }).click(); await expect(page.getByRole('status')).toContainText('Bạn đã đăng ký thành công!'); expect(calls.filter(c => c.path.endsWith('/verifications'))).toHaveLength(1);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
test('registration OTP requires typed code and expires after two minutes', async ({ page, context }) => {
  const calls = await fixture(context);
  const start = new Date('2026-10-30T10:00:00Z'); await page.clock.install({ time: start });
  await page.goto(`/verify-email#proofId=${proofId}&expiresAt=2026-10-30T10:02:00Z`);
  expect(new URL(page.url()).hash).toBe('');
  await expect(page.getByRole('timer')).toContainText('02:00');
  await page.getByLabel('Mã xác minh 6 chữ số').fill('123456');
  await page.getByRole('button', { name: 'Xác minh tài khoản' }).click();
  await expect(page).toHaveURL(/\/login$/); await expect(page.getByRole('status')).toContainText('Chào mừng bạn đến với ArgoPost');
  await expect.poll(() => calls.filter(call => call.path.endsWith('/verifications')).length).toBe(1);
  expect(calls.find(call => call.path.endsWith('/verifications'))?.body).toEqual({proofId,proof:'123456'});
  await page.goto('/login');
  await page.goto(`/verify-email#proofId=${proofId}&expiresAt=2026-10-30T10:02:00Z`);
  await expect(page.getByRole('timer')).toHaveText(/^(02:00|01:5[0-9])$/);
  await page.clock.fastForward(120000);
  await expect(page.getByRole('timer')).toContainText('Mã đã hết hạn');
  await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Yêu cầu hướng dẫn xác minh' })).toBeVisible();
});
test('recovery confirmation retains exact command after an unknown response without automatic retry', async ({ page, context }) => {
  let attempts = 0; const calls = await fixture(context, false, async (route, call) => {
    if (!call.path.endsWith('/recovery-confirm')) return false;
    if (++attempts === 1) await route.abort('failed'); else await route.fulfill({ status: 204 }); return true;
  });
  await page.goto('/recover/confirm#proofId=' + proofId + '&proof=' + 'B'.repeat(43)); await page.getByLabel('Mật khẩu mới', { exact: true }).fill('synthetic-recovered-password'); await page.getByLabel('Nhập lại mật khẩu mới').fill('synthetic-recovered-password'); await page.getByRole('button', { name: 'Xác nhận mật khẩu mới' }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page).toHaveURL(/\/recover\/confirm$/); await expect(page.locator('.cp-success-popup')).toHaveCount(0); expect(attempts).toBe(1); await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Xác nhận mật khẩu mới' }).click(); await expect(page.getByRole('status')).toContainText('Bạn đã đổi mật khẩu thành công!');
  await expect(page).toHaveURL(/\/login$/); await expect(page.getByLabel('Email đã xác minh')).toBeVisible();
  const commands = calls.filter(c => c.path.endsWith('/recovery-confirm')); expect(commands).toHaveLength(2); expect(commands[0].key).toBe(commands[1].key); expect(commands[0].body).toEqual(commands[1].body); expect(new URL(page.url()).hash).toBe('');
});
test('profile unknown outcome stays unknown until scoped receipt GET confirms commit', async ({ page, context }) => {
  let updated = false, receiptQueries = 0;
  const calls = await fixture(context, true, async (route, call) => {
    if (call.path.endsWith('/profile') && call.method === 'PUT') { updated = true; await route.abort('failed'); return true; }
    if (call.path.includes('/onboarding-receipts/')) { receiptQueries++; await route.fulfill({ status: receiptQueries === 1 ? 404 : 200, contentType: 'application/json', body: JSON.stringify(receiptQueries === 1 ? { code: 'RESOURCE_NOT_FOUND' } : { operationId: 'updateCustomerProfile', targetId: account, outcome: 'COMMITTED', version: 2 }) }); return true; }
    if (updated && call.path.endsWith('/profile') && call.method === 'GET') { await route.fulfill({ status: 200, contentType: 'application/json', headers: { ETag: '"v2"' }, body: JSON.stringify({ id: account, accountId: account, displayName: 'Tên đã lưu', preferredLanguage: 'vi', version: 2 }) }); return true; }
    return false;
  });
  await page.goto('/personal/profile'); await page.getByLabel('Tên hiển thị trong hồ sơ').fill('Tên đã lưu'); await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toBeDisabled();
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu' }).click(); await expect(page.getByRole('alert')).toContainText('vẫn đang ở trạng thái chưa xác định'); await expect(page.getByText('Đã lưu hồ sơ.', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Kiểm tra kết quả lưu' }).click(); await expect(page.getByText('Đã lưu hồ sơ.', { exact: true })).toBeVisible(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Tên đã lưu'); expect(calls.filter(c => c.method === 'PUT')).toHaveLength(1);
});
test('cross-tab logout removes personal form data and prevents stale workspace display', async ({ page, context }) => {
  await fixture(context, true); await page.goto('/personal/profile'); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveValue('Hồ sơ thử');
  const other = await context.newPage(); await other.goto('/personal'); await other.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toHaveCount(0); await expect(page.getByText('Hồ sơ thử', { exact: true })).toHaveCount(0); await other.close();
});
test('guest deep link cannot render or request a protected personal profile', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/personal/profile'); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible(); expect(new URL(page.url()).pathname).toBe('/login'); expect(calls.filter(c => c.path.endsWith('/profile'))).toHaveLength(0);
});
test('merchant route fails closed without an authoritative active membership', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto('/merchant');
  await expect(page.getByRole('heading', { name: 'Không gian Merchant chưa khả dụng' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('không hiển thị dữ liệu Merchant hoặc cấp quyền từ đường dẫn');
  expect(calls.some(call => call.path.includes('merchant'))).toBe(false);
});

test('Personal dashboard reads self Analytics without demo data or fabricated counters', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto('/personal');
  await expect(page.getByRole('heading', { name: 'Tổng quan gửi hàng', exact: true })).toBeVisible();
  await expect(page.locator('.an-empty')).toContainText('Chưa có vận đơn');
  await expect(page.locator('[data-metric=BOOKED_SHIPMENTS] .an-kpi-value')).toContainText('0');
  expect(calls.filter(call => call.path === '/api/v1/customer/personal-analytics' && call.method === 'GET')).toHaveLength(1);
  expect(calls.some(call => /demo|notification/.test(call.path))).toBe(false);
  await expect(page.getByRole('button', { name: /th?ng b?o/i })).toHaveCount(0);
  await page.reload(); await expect(page.locator('.an-empty')).toContainText('Chưa có vận đơn');
});

test('Personal Analytics loading and a failed read cannot become an empty-success dashboard', async ({ page, context }) => {
  let release!: () => void; const pending = new Promise<void>(resolve => { release = resolve; }); let reads = 0;
  await fixture(context, true, async (route, call) => {
    if (call.path !== '/api/v1/customer/personal-analytics' || call.method !== 'GET') return false;
    reads++; if (reads > 1) return false;
    await pending; await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'SERVICE_UNAVAILABLE' }) }); return true;
  });
  await page.goto('/personal'); await expect(page.getByRole('status')).toContainText('Đang đọc dữ liệu phân tích');
  await expect(page.locator('.an-kpi')).toHaveCount(0); release();
  await expect(page.getByRole('alert')).toContainText('Không tải được dữ liệu phân tích');
  await expect(page.locator('.an-kpi')).toHaveCount(0);
  await page.getByRole('button', { name: 'Thử lại Analytics' }).click(); await expect(page.locator('.an-empty')).toContainText('Chưa có vận đơn'); expect(reads).toBe(2);
});

test('workspace disclosure never grants Business from an approved application or a URL', async ({ page, context }) => {
  const calls = await fixture(context, true, async (route, call) => {
    if (call.path !== '/api/v1/merchant-applications') return false;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ id: proofId, applicantAccountId: account, state: 'APPROVED', revision: 1, version: 1, organizationId: '10000000-0000-4000-8000-000000000099' }], page: 0, size: 20, hasNext: false }) }); return true;
  });
  await page.goto('/personal/applications'); await expect(page.getByText('Đã duyệt · phiên bản 1')).toBeVisible();
  const trigger = page.getByRole('button', { name: 'ArgoPost Personal', exact: true }); await trigger.focus(); await page.keyboard.press('Enter');
  const choices = page.locator('#workspace-choices'); await expect(choices).toBeVisible();
  await expect(choices.locator('a[href^="/merchant"]')).toHaveCount(0); await expect(choices).toContainText('Hồ sơ đăng ký không cấp quyền');
  await page.keyboard.press('Escape'); await expect(choices).toHaveCount(0); await expect(trigger).toBeFocused();
  await trigger.click(); await page.getByRole('heading', { name: 'Hồ sơ đăng ký Merchant' }).click({ position: { x: 4, y: 4 } }); await expect(choices).toHaveCount(0);
  await page.goto('/merchant/10000000-0000-4000-8000-000000000099/profile');
  await expect(page.getByRole('heading', { name: 'Không gian Merchant chưa khả dụng' })).toBeVisible();
  expect(calls.some(call => /\/merchant\/|\/organizations|\/contexts/.test(call.path))).toBe(false);
});

test('deferred Personal modules explain dependencies without sending unsupported mutations', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto('/personal');
  const navigation = page.getByRole('navigation', { name: 'Điều hướng Personal', exact: true });
  for (const title of ['In nhãn & Tiện ích', 'Hỗ trợ & Khiếu nại']) {
    await navigation.getByRole('link', { name: title, exact: false }).click();
    await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible();
    await expect(page.locator('.pw-module-state')).toContainText('Chưa khả dụng'); await expect(page.locator('.pw-module-state input')).toHaveCount(0);
    await expect(page.locator('.pw-module-state button[type="submit"]')).toHaveCount(0);
  }
  expect(calls.filter(call => !call.path.startsWith('/api/v1/customer-auth/') && call.path !== '/api/v1/customer/personal-analytics')).toHaveLength(0);
});

test('Personal shell supports narrow navigation, keyboard dismissal and accessible logout', async ({ page, context }) => {
  await fixture(context, true); await page.goto('/personal');
  for (const width of [1440, 1024, 768, 390, 360]) {
    await page.setViewportSize({ width, height: 950 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await expect(page.getByRole('button', { name: 'ArgoPost Personal', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Đăng xuất', exact: true })).toBeVisible();
    if (width === 1440 || width === 390) await page.screenshot({ path: `../../../.demo/argopost-personal/dashboard-${width}.png`, fullPage: true, animations: 'disabled' });
  }
  const menu = page.getByRole('button', { name: 'Mở menu cá nhân' }); await menu.click();
  await expect(page.getByRole('navigation', { name: 'Điều hướng Personal' })).toBeVisible(); await page.keyboard.press('Escape'); await expect(menu).toBeFocused();
  await menu.click(); await page.getByRole('navigation', { name: 'Điều hướng Personal' }).getByRole('link', { name: 'Hồ sơ', exact: true }).click();
  await expect(page.getByLabel('Tên hiển thị trong hồ sơ')).toBeVisible(); await expect(menu).toHaveAttribute('aria-expanded', 'false');
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' })).toBeVisible();
  await expect(page.locator('.pw-shell')).toHaveCount(0);
});

test('Personal page regions keep headings and controls clear on desktop tablet and mobile', async ({ page, context }) => {
  await fixture(context, true, async (route, call) => {
    if (!['/api/v1/customer/addresses', '/api/v1/merchant-applications'].includes(call.path)) return false;
    await route.fulfill({ contentType: 'application/json', body: JSON.stringify({ items: [], page: 0, size: 20, hasNext: false }) }); return true;
  });
  const errors: string[] = []; page.on('pageerror', error => errors.push(error.name));
  for (const width of [1440, 768, 390]) {
    await page.setViewportSize({ width, height: 960 });
    for (const route of ['profile', 'security', 'create', 'addresses', 'shipments', 'lookup', 'billing', 'labels', 'support', 'applications']) {
      await page.goto('/personal/' + route);
      const heading = page.locator('#main-content h1'); await expect(heading).toBeVisible();
      const topbar = await page.locator('.pw-header').boundingBox(), title = await heading.boundingBox();
      expect(title!.y).toBeGreaterThan(topbar!.y + topbar!.height);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      if (['profile', 'create', 'shipments'].includes(route)) await page.screenshot({ path: `../../../.demo/customer-portal-${route}-${width}.png`, fullPage: true, animations: 'disabled' });
    }
  }
  expect(errors).toEqual([]);
});

test('unconfirmed logout for password recovery cannot claim success or open recovery automatically', async ({ page, context }) => {
  await fixture(context, true, async (route, call) => {
    if (!call.path.endsWith('/logout')) return false;
    await route.abort('failed'); return true;
  });
  await page.goto('/personal/security'); await page.getByRole('button', { name: 'Đăng xuất để đặt lại mật khẩu' }).click();
  await expect(page).toHaveURL(/\/login$/); await expect(page.getByRole('alert')).toContainText('Chưa xác định');
  await expect(page.locator('.pw-shell')).toHaveCount(0); await expect(page.getByRole('heading', { name: 'Quên mật khẩu' })).toHaveCount(0);
});

test('Personal security starts recovery only after a confirmed logout without sending an email automatically', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto('/personal/security');
  await page.getByRole('button', { name: 'Đăng xuất để đặt lại mật khẩu' }).click();
  await expect(page).toHaveURL(/\/recover$/); await expect(page.getByLabel('Email của tài khoản')).toBeVisible();
  await expect(page.locator('.pw-shell')).toHaveCount(0);
  expect(calls.filter(call => call.path.endsWith('/logout') && call.method === 'POST')).toHaveLength(1);
  expect(calls.filter(call => call.path.endsWith('/recovery'))).toHaveLength(0);
});
test('mobile labels keyboard navigation and public login layout remain usable', async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/login'); await expect(page.getByLabel('Email đã xác minh')).toBeVisible(); await expect(page.getByLabel('Mật khẩu', { exact: true })).toBeVisible(); await page.keyboard.press('Tab'); await expect(page.getByRole('link', { name: 'Đến nội dung chính' })).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' }).focus(); await page.screenshot({ path: '../../../.demo/spec003-implementation/customer-login-mobile.png', fullPage: true, animations: 'disabled' });
});

test('public website routes, utilities and mobile navigation disclose pending services', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/');
  await expect(page.getByRole('heading', { name: 'ArgoPost', exact: true })).toHaveCount(1);
  await expect(page.getByRole('img', { name: /Minh họa các kiện hàng/ })).toBeVisible();
  await page.screenshot({ path: '../../../.demo/spec003-implementation/public-home-desktop.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('tab', { name: 'Ước tính cước' }).click();
  await expect(page.getByRole('tabpanel')).not.toContainText('SẮP RA MẮT');
  await page.getByRole('tabpanel').getByRole('link', { name: 'Xem thông tin tiện ích' }).click();
  await expect(page.getByRole('heading', { name: 'Ước tính cước phí' })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/');
  await page.screenshot({ path: '../../../.demo/spec003-implementation/public-home-mobile.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Mở menu' }).click();
  await page.getByRole('navigation', { name: 'Điều hướng chính' }).getByRole('link', { name: 'Hỗ trợ', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Hỗ trợ khách hàng' })).toBeVisible(); await page.goto('/news');
  await page.getByRole('button', { name: 'Nền tảng', exact: true }).click();
  await expect(page.locator('.ps-guide-grid article')).toHaveCount(1);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  expect(calls.filter(c => c.path.includes('tracking') || c.path.includes('quote'))).toHaveLength(0);
});

test('recovery opens OTP immediately, resends with retained email, verifies before password form and keeps capability out of storage', async ({ page, context }) => {
  const calls = await fixture(context);
  await page.setViewportSize({width:390,height:844});
  await page.goto('/recover'); await page.getByLabel('Email của tài khoản').fill('synthetic@example.invalid'); await page.getByRole('button', { name: 'Yêu cầu khôi phục mật khẩu' }).click();
  await expect(page).toHaveURL(/\/recover\/otp$/); await expect(page.getByLabel('Mã xác minh 6 chữ số')).toBeVisible();
  await expect(page.getByLabel('Email của tài khoản')).toHaveCount(0); await expect(page.getByRole('button', { name: /Gửi lại sau/ })).toBeDisabled();
  await page.screenshot({path:'../../../.demo/spec003-implementation/customer-otp-mobile.png',fullPage:true,animations:'disabled'});
  await page.clock.install(); await page.clock.fastForward(31000); await page.getByRole('button', { name: 'Gửi lại mã', exact: true }).click();
  await expect.poll(() => calls.filter(call => call.path.endsWith('/recovery')).length).toBe(2);
  expect(calls.filter(call => call.path.endsWith('/recovery'))[1].body).toEqual({email:'synthetic@example.invalid'});
  await page.getByLabel('Mã xác minh 6 chữ số').fill('123456'); await page.getByRole('button', { name: 'Tiếp tục', exact: true }).click();
  await expect(page).toHaveURL(/\/recover\/confirm$/); await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toBeVisible();
  expect(calls.find(call => call.path.endsWith('/recovery-verifications'))?.body).toEqual({proofId,proof:'123456'});
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('synthetic-recovered-password'); await page.getByLabel('Nhập lại mật khẩu mới').fill('synthetic-recovered-password'); await page.getByRole('button', { name: 'Xác nhận mật khẩu mới' }).click();
  await expect(page.getByRole('status')).toContainText('Bạn đã đổi mật khẩu thành công!');
  expect(calls.find(call => call.path.endsWith('/recovery-confirm'))?.body?.proof).toBe('C'.repeat(43));
  expect(await page.evaluate(() => [localStorage.length,sessionStorage.length])).toEqual([0,0]);
});
test('unknown recovery still opens the same OTP screen and does not expose password form on invalid code', async ({ page, context }) => {
  await fixture(context, false, async (route, call) => { if(!call.path.endsWith('/recovery-verifications')) return false; await route.fulfill({status:422,contentType:'application/json',body:JSON.stringify({code:'TOKEN_INVALID_OR_EXPIRED'})});return true; });
  await page.goto('/recover');await page.getByLabel('Email của tài khoản').fill('unknown@example.invalid');await page.getByRole('button',{name:'Yêu cầu khôi phục mật khẩu'}).click();
  await page.getByLabel('Mã xác minh 6 chữ số').fill('123456');await page.getByRole('button',{name:'Tiếp tục',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('không còn hiệu lực');await expect(page.getByLabel('Mật khẩu mới',{exact:true})).toHaveCount(0);
});
test('ArgoPost brand, keyboard utility tabs, reduced motion and responsive navigation', async ({ page, context }) => {
  await fixture(context);await page.emulateMedia({reducedMotion:'reduce'});await page.goto('/');
  await expect(page.getByRole('link',{name:'ArgoPost, về trang chủ'}).first()).toBeVisible();await expect(page.getByRole('link',{name:'ĐĂNG NHẬP / ĐĂNG KÝ',exact:true})).toBeVisible();
  await page.getByRole('tab',{name:'Tra cứu vận đơn'}).focus();await page.keyboard.press('ArrowRight');await expect(page.getByRole('tab',{name:'Ước tính cước'})).toBeFocused();await expect(page.getByRole('tab',{name:'Ước tính cước'})).toHaveAttribute('aria-selected','true');
  expect(await page.locator('.ps-hero-art img').evaluate(element=>getComputedStyle(element).animationName)).toBe('none');
  await page.setViewportSize({width:360,height:800});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.getByRole('button',{name:'Mở menu'}).click();await page.getByRole('navigation',{name:'Điều hướng chính'}).getByRole('link',{name:'Hỗ trợ',exact:true}).click();await expect(page.getByRole('heading',{name:'Hỗ trợ khách hàng'})).toBeVisible();
});

test('ArgoPost solutions distinguish the road focus from unavailable air and sea services', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/');
  const air = page.getByRole('article').filter({ has: page.getByRole('heading', { name: 'Vận chuyển hàng không' }) });
  const sea = page.getByRole('article').filter({ has: page.getByRole('heading', { name: 'Vận chuyển đường biển' }) });
  for (const unavailable of [air, sea]) { await expect(unavailable).toContainText('CHƯA PHÁT TRIỂN'); await expect(unavailable.getByRole('link')).toHaveCount(0); await expect(unavailable.getByRole('button')).toHaveCount(0); }
  const road = page.getByRole('article').filter({ has: page.getByRole('heading', { name: 'Vận chuyển đường bộ' }) });
  await expect(road).toContainText('đang được hoàn thiện'); await road.getByRole('link', { name: 'Tìm hiểu dịch vụ' }).click();
  await expect(page).toHaveURL(/\/services$/); await expect(page.getByRole('heading', { name: 'Dịch vụ chuyển phát' })).toBeVisible();
  expect(calls.filter(call => /tracking|quote|shipment/.test(call.path))).toHaveLength(0);
});

test('homepage has compact reasons and ends with neutral action rows instead of the old band and FAQ', async ({ page, context }) => {
  await fixture(context); await page.emulateMedia({ reducedMotion: 'reduce' }); await page.goto('/');
  const reasons = page.locator('.ps-why'), start = page.locator('.ps-start'), cards = page.locator('.ps-start-card');
  expect((await reasons.boundingBox())!.y).toBeLessThan((await start.boundingBox())!.y);
  await expect(reasons.locator('.ps-why-tile')).toHaveCount(4); await expect(cards).toHaveCount(3);
  await expect(page.locator('.ps-why-band')).toHaveCount(0); await expect(page.locator('.ps-home-faq')).toHaveCount(0);
  await expect(page.getByText('BẠN HỎI, ARGOPOST TRẢ LỜI', { exact: true })).toHaveCount(0);
  expect((await page.locator('#solutions').boundingBox())!.y).toBeLessThan((await start.boundingBox())!.y);
  await expect(page.locator('#public-main > section').last()).toHaveClass(/ps-start/);
  const boxes = await cards.evaluateAll(elements => elements.map(element => { const box = element.getBoundingClientRect(); return { x: box.x, y: box.y, width: box.width }; }));
  expect(boxes[1].y).toBeGreaterThan(boxes[0].y); expect(boxes[2].y).toBeGreaterThan(boxes[1].y); expect(boxes[0].x).toBe(boxes[1].x); expect(boxes[0].width).toBe(boxes[1].width);
  for (const card of await cards.all()) expect(await card.evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 255, 255)');
  await cards.nth(1).hover(); expect(await cards.nth(1).evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 104, 1)');
  expect(await cards.first().evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 255, 255)');
  await page.mouse.move(0, 0); await cards.first().getByRole('link').focus(); expect(await cards.first().evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 104, 1)');
});

test('service portraits keep gray air/sea and a navy road default', async ({ page, context }) => {
  await fixture(context); await page.emulateMedia({ reducedMotion: 'reduce' }); await page.goto('/');
  const cards = page.locator('.ps-freight-card'); await expect(cards).toHaveCount(3); await cards.first().scrollIntoViewIfNeeded();
  const boxes = await cards.evaluateAll(elements => elements.map(element => { const box = element.getBoundingClientRect(); return { x: box.x, y: box.y, width: box.width, height: box.height }; }));
  expect(boxes[0].y).toBe(boxes[1].y); expect(boxes[1].y).toBe(boxes[2].y); expect(boxes[1].x).toBeGreaterThan(boxes[0].x); expect(boxes[0].height).toBeGreaterThan(boxes[0].width);
  for (const card of await page.locator('.ps-freight-card.is-coming').all()) expect(await card.evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(233, 237, 241)');
  expect(await page.locator('.ps-freight-land').evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(1, 36, 81)');
  const art = await page.request.get('/media/argopost-freight-scenes-v2.png'); expect(art.ok()).toBe(true); expect(art.headers()['content-type']).toContain('image/png');
  await page.screenshot({ path: '../../../.demo/argopost-ui/portrait-services-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 }); await cards.first().scrollIntoViewIfNeeded();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  const mobileBoxes = await cards.evaluateAll(elements => elements.map(element => element.getBoundingClientRect().y)); expect(mobileBoxes[1]).toBeGreaterThan(mobileBoxes[0]);
});

test('service cards turn orange and scale on hover without changing unavailable service controls', async ({ page, context }) => {
  await fixture(context); await page.emulateMedia({ reducedMotion: 'no-preference' }); await page.goto('/');
  await page.locator('#solutions').scrollIntoViewIfNeeded(); await expect(page.locator('#solutions')).toHaveClass(/is-visible/);
  const cards = page.locator('.ps-freight-card');
  for (const [index, card] of (await cards.all()).entries()) {
    await page.mouse.move(0, 0);
    const background = index < 2 ? 'rgb(233, 237, 241)' : 'rgb(1, 36, 81)';
    await expect.poll(() => card.evaluate(element => getComputedStyle(element).backgroundColor)).toBe(background);
    await card.hover();
    await expect.poll(() => card.evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 104, 1)');
    await expect.poll(() => card.evaluate(element => new DOMMatrix(getComputedStyle(element).transform).a)).toBeGreaterThan(1.01);
    if (index < 2) { await expect(card).toContainText('CHƯA PHÁT TRIỂN'); await expect(card.getByRole('link')).toHaveCount(0); await expect(card.getByRole('button')).toHaveCount(0); }
    await page.mouse.move(0, 0);
    await expect.poll(() => card.evaluate(element => getComputedStyle(element).backgroundColor)).toBe(background);
  }
  await cards.nth(1).hover(); await expect.poll(() => cards.nth(1).evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 104, 1)');
  await page.locator('#solutions').screenshot({ path: '../../../.demo/argopost-ui/services-hover-final.png', animations: 'disabled' });
  await page.mouse.move(0, 0); await page.locator('.ps-service-cta').focus();
  await expect.poll(() => cards.last().evaluate(element => getComputedStyle(element).backgroundColor)).toBe('rgb(255, 104, 1)');
});

test('ArgoPost supplied logos load and public/auth layouts stay within narrow and desktop viewports', async ({ page, context }) => {
  await fixture(context); await page.emulateMedia({ reducedMotion: 'reduce' });
  for (const width of [360, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    for (const route of ['/', '/login', '/register', '/recover', '/about']) {
      await page.goto(route); await expect(page).toHaveTitle(/ArgoPost/);
      const logo = page.locator('.ps-header .ps-brand img'); await expect(logo).toHaveAttribute('src', '/media/argopost-logo-primary.png');
      await expect.poll(() => logo.evaluate(element => (element as HTMLImageElement).naturalWidth)).toBeGreaterThan(0);
      expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
      await expect(page.getByText('Xác minh email an toàn', { exact: true })).toHaveCount(0);
      if (route === '/') { await expect(page.locator('.ps-footer .ps-brand img')).toHaveAttribute('src', '/media/argopost-logo-white.png'); await expect(page.locator('.ps-freight-land')).toBeVisible(); }
      if (route === '/login') { await expect(page.getByLabel('Email đã xác minh')).toBeVisible(); await expect(page.getByLabel('Mật khẩu', { exact: true })).toBeVisible(); }
      await page.screenshot({ path: `../../../.demo/argopost-ui/${route === '/' ? 'home' : route.slice(1)}-${width}.png`, fullPage: true, animations: 'disabled' });
    }
  }
  const favicon = await page.request.get('/media/argopost-app-icon.png'); expect(favicon.ok()).toBe(true);
});

test('registration success opens login with a centered dismissible welcome popup only after server confirmation', async ({ page, context }) => {
  let release!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  await fixture(context,false,async (route,call)=>{if(!call.path.endsWith('/verifications'))return false;await pending;await route.fulfill({status:204});return true;});
  await page.setViewportSize({width:390,height:844});await page.goto(`/verify-email#proofId=${proofId}&expiresAt=${new Date(Date.now()+120000).toISOString()}`);
  await page.getByLabel('Mã xác minh 6 chữ số').fill('123456');await page.getByRole('button',{name:'Xác minh tài khoản'}).click();
  await expect(page.getByRole('button',{name:'Đang xác minh…'})).toBeDisabled();await expect(page.locator('.cp-success-popup')).toHaveCount(0);expect(new URL(page.url()).pathname).toBe('/verify-email');
  release();await expect(page).toHaveURL(/\/login$/);await expect(page.getByRole('heading',{name:'Đăng nhập bằng mật khẩu'})).toBeVisible();
  const popup=page.locator('.cp-success-popup');await expect(popup).toContainText('Bạn đã đăng ký thành công!');await expect(popup).toContainText('Chào mừng bạn đến với ArgoPost');
  await expect(popup).toHaveAttribute('role','status');const box=await popup.boundingBox();expect(box).not.toBeNull();expect(Math.abs(box!.x+box!.width/2-195)).toBeLessThan(2);expect(box!.y).toBeLessThan(24);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:'../../../.demo/spec003-implementation/auth-success-mobile.png',fullPage:true,animations:'disabled'});
  await page.setViewportSize({width:1280,height:900});const desktop=await popup.boundingBox();expect(Math.abs(desktop!.x+desktop!.width/2-640)).toBeLessThan(2);
  await page.screenshot({path:'../../../.demo/spec003-implementation/auth-success-desktop.png',fullPage:true,animations:'disabled'});
  await page.getByRole('button',{name:'Đóng thông báo thành công'}).click();await expect(popup).toHaveCount(0);await expect(page.getByLabel('Email đã xác minh')).toBeVisible();
  await page.reload();await expect(popup).toHaveCount(0);
});
async function fillAddress(page: import('@playwright/test').Page, label = 'Nhà riêng') {
  await page.getByLabel('Tên gợi nhớ', { exact: false }).fill(label); await page.getByLabel('Họ tên liên hệ', { exact: true }).fill('Người gửi thử'); await page.getByLabel('Số điện thoại liên hệ', { exact: true }).fill('0901234567'); await page.getByLabel('Tỉnh / thành phố').fill('Địa phương thử'); await page.getByLabel('Phường / xã / địa phương').fill('Phường thử'); await page.getByLabel('Địa chỉ chi tiết').fill('Địa chỉ thử');
}
function addressValue(body?: Record<string, unknown>, version = 1) { return { ...body, id: proofId, accountId: account, contactPhone: '+84901234567', verificationStatus: 'UNVERIFIED', version, createdAt: '2026-11-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z' }; }

test('shipping preparation uses PW03 snapshots and resolves a lost save without another mutation', async ({ page, context }) => {
  const source={kind:'SENDER',label:'Synthetic source',contactName:'Synthetic sender',contactPhone:'+84901234567',address:{country:'VN',administrativeArea:'Area',locality:'Locality',addressLine:'Street'},isDefault:false};
  let stored:Record<string,unknown>|undefined;
  const calls=await fixture(context,true,async(route,call)=>{
    if(call.path.includes('/customer/addresses')){await route.fulfill({contentType:'application/json',body:JSON.stringify({items:new URL(route.request().url()).searchParams.get('kind')==='SENDER'?[addressValue(source)]:[],page:0,size:20,hasNext:false})});return true;}
    if(call.path.includes('/shipment-draft-receipts/')){await route.fulfill({contentType:'application/json',body:JSON.stringify({operationId:'createShippingDraft',targetId:proofId,outcome:'COMMITTED',version:1})});return true;}
    if(!call.path.includes('/shipment-drafts'))return false;
    if(call.method==='POST'){stored={id:proofId,accountId:account,ownerType:'PERSONAL',state:'DRAFT',input:{...call.body,sender:{...source,name:source.contactName,phone:source.contactPhone}},version:1,createdAt:'2026-11-01T00:00:00Z',updatedAt:'2026-11-01T00:00:00Z'};const input=structuredClone(call.body!);input.sender={name:'Synthetic sender',phone:'+84901234567',address:source.address};input.recipient={name:'Synthetic recipient',phone:'+84901234567',address:source.address};stored.input=input;await route.abort('failed');return true;}
    await route.fulfill({contentType:'application/json',headers:{ETag:'"v1"'},body:JSON.stringify(stored)});return true;
  });
  await page.goto('/personal/create');await page.locator('#sender-book').selectOption(proofId);await expect(page.locator('#sender-name')).toHaveValue('Synthetic sender');await page.locator('#recipient-name').fill('Synthetic recipient');await page.locator('#recipient-phone').fill('0901234567');await page.locator('#recipient-administrativeArea').fill('Area');await page.locator('#recipient-locality').fill('Locality');await page.locator('#recipient-addressLine').fill('Street');await page.locator('#parcel-description-0').fill('Synthetic box');await page.locator('#draft-reference').fill('UI preparation');await page.getByRole('button',{name:'Lưu bản nháp',exact:true}).click();await expect(page.getByRole('alert')).toContainText('Chưa xác định');await expect(page.locator('#sender-name')).toBeDisabled();await page.getByRole('button',{name:'Kiểm tra kết quả lưu',exact:true}).click();await expect(page.getByRole('heading',{name:'Chi tiết bản nháp',exact:true})).toBeVisible();expect(calls.filter(c=>c.path==='/api/v1/customer/shipment-drafts'&&c.method==='POST')).toHaveLength(1);await page.getByRole('tab',{name:'Chứng từ',exact:true}).click();await expect(page.getByRole('tabpanel')).toContainText('không phát hành nhãn');await expect(page.getByRole('button',{name:'Xác nhận booking'})).toHaveCount(0);
  await page.setViewportSize({width:390,height:844});await expect(page.locator('#personal-sidebar')).not.toBeVisible();await page.screenshot({path:'../../../.demo/shipping-draft-mobile.png',fullPage:true,animations:'disabled'});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});

test('address book creates edits archives and refreshes server-confirmed contacts only', async ({ page, context }) => {
  let item: Record<string, unknown> | undefined, writes = 0;
  const calls = await fixture(context, true, async (route, call) => {
    if (!call.path.includes('/customer/addresses')) return false;
    const reply = (value: unknown, status = 200, headers: Record<string, string> = {}) => route.fulfill({ status, contentType: 'application/json', headers, body: JSON.stringify(value) });
    if (call.method === 'GET') { await reply({ items: item ? [item] : [], page: 0, size: 20, hasNext: false }); return true; }
    writes++;
    if (call.path.endsWith('/archive')) { item = undefined; await route.fulfill({ status: 204 }); return true; }
    item = addressValue(call.body, call.method === 'PUT' ? 2 : 1); await reply(item, call.method === 'POST' ? 201 : 200, { ETag: '"v' + item.version + '"' }); return true;
  });
  await page.goto('/personal/addresses'); await expect(page.getByRole('heading', { name: 'Sổ địa chỉ của bạn đang trống' })).toBeVisible(); await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click(); await fillAddress(page); await page.getByRole('checkbox').check(); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByRole('status')).toContainText('Đã lưu địa chỉ');
  await expect(page.locator('.ab-card')).toContainText('Chưa xác minh'); await expect(page.locator('.ab-default')).toContainText('Mặc định'); await page.reload(); await expect(page.locator('.ab-card')).toContainText('Nhà riêng');
  await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click(); await page.getByLabel('Tên gợi nhớ', { exact: false }).fill('Địa chỉ đã sửa'); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.locator('.ab-card')).toContainText('Địa chỉ đã sửa');
  await page.screenshot({ path: '../../../.demo/argopost-personal/address-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 950 }); await expect(page.locator('.pw-sidebar')).toBeHidden(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: '../../../.demo/argopost-personal/address-mobile.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Lưu trữ', exact: true }).click(); await expect(page.getByRole('alertdialog')).toContainText('Lịch sử vẫn được bảo toàn'); await page.getByRole('button', { name: 'Xác nhận lưu trữ', exact: true }).click(); await expect(page.getByRole('status').filter({ hasText: 'Đã lưu trữ địa chỉ' })).toBeVisible(); await expect(page.locator('.ab-card')).toHaveCount(0); expect(writes).toBe(3);
  expect(calls.filter(call => call.path.includes('/customer/addresses') && call.method !== 'GET').every(call => !!call.key)).toBe(true);
});

test('address unknown outcome locks the original form and resolves by receipt without a second POST', async ({ page, context }) => {
  let item: Record<string, unknown> | undefined, queries = 0;
  const calls = await fixture(context, true, async (route, call) => {
    if (call.path.includes('/address-receipts/')) { queries++; await route.fulfill({ status: queries === 1 ? 404 : 200, contentType: 'application/json', body: JSON.stringify(queries === 1 ? { code: 'RESOURCE_NOT_FOUND' } : { operationId: 'createPersonalAddress', targetId: proofId, outcome: 'COMMITTED', version: 1 }) }); return true; }
    if (!call.path.includes('/customer/addresses')) return false;
    if (call.method === 'POST') { item = addressValue(call.body); await route.abort('failed'); return true; }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: item ? [item] : [], page: 0, size: 20, hasNext: false }) }); return true;
  });
  await page.goto('/personal/addresses'); await page.getByRole('button', { name: '+ Thêm địa chỉ', exact: true }).click(); await fillAddress(page); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Họ tên liên hệ', { exact: true })).toBeDisabled(); await expect(page.getByText('Đã lưu địa chỉ.', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' }).click(); await expect(page.getByRole('alert')).toContainText('vẫn chưa xác định'); await page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' }).click(); await expect(page.getByText('Đã lưu địa chỉ.', { exact: true })).toBeVisible(); await expect(page.locator('.ab-card')).toContainText('Nhà riêng'); expect(calls.filter(call => call.path === '/api/v1/customer/addresses' && call.method === 'POST')).toHaveLength(1);
});

test('address stale version preserves edits and malformed foreign data is never rendered', async ({ page, context }) => {
  const body = { kind: 'SENDER', label: 'Own address', contactName: 'Synthetic name', address: { country: 'VN', administrativeArea: 'Area', locality: 'Locality', addressLine: 'Street' }, isDefault: false }; let foreign = false;
  const calls = await fixture(context, true, async (route, call) => {
    if (!call.path.includes('/customer/addresses')) return false;
    if (call.method === 'PUT') { await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ code: 'PRECONDITION_STALE' }) }); return true; }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ items: [{ ...addressValue(body), ...(foreign ? { accountId: '10000000-0000-4000-8000-000000000002', label: 'Foreign private address' } : {}) }], page: 0, size: 20, hasNext: false }) }); return true;
  });
  await page.goto('/personal/addresses'); await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click(); await page.getByLabel('Tên gợi nhớ', { exact: false }).fill('Unsaved value'); await page.getByRole('button', { name: 'Lưu địa chỉ', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Địa chỉ đã thay đổi'); await expect(page.getByLabel('Tên gợi nhớ', { exact: false })).toHaveValue('Unsaved value'); expect(calls.filter(call => call.method === 'PUT')).toHaveLength(1);
  foreign = true; page.on('dialog', dialog => dialog.accept()); await page.getByRole('button', { name: 'Tải lại bản hiện tại' }).click(); await expect(page.getByRole('alert')).toContainText('Không xác thực được dữ liệu'); await expect(page.getByText('Foreign private address', { exact: true })).toHaveCount(0); await expect(page.locator('.ab-card')).toHaveCount(0);
});

test('rejected password reset stays on its form and never shows success or redirects', async ({ page, context }) => {
  await fixture(context,false,async(route,call)=>{if(!call.path.endsWith('/recovery-confirm'))return false;await route.fulfill({status:422,contentType:'application/json',body:JSON.stringify({code:'TOKEN_INVALID_OR_EXPIRED'})});return true;});
  await page.goto(`/recover/confirm#proofId=${proofId}&proof=${'B'.repeat(43)}`);await page.getByLabel('Mật khẩu mới',{exact:true}).fill('synthetic-recovered-password');await page.getByLabel('Nhập lại mật khẩu mới').fill('synthetic-recovered-password');await page.getByRole('button',{name:'Xác nhận mật khẩu mới'}).click();
  await expect(page.getByRole('alert')).toContainText('không còn hiệu lực');await expect(page).toHaveURL(/\/recover\/confirm$/);await expect(page.locator('.cp-success-popup')).toHaveCount(0);
});
