import { test, expect } from '@playwright/test';
import type { BrowserContext, Route } from '@playwright/test';

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
  await expect(page).toHaveURL(/\/login$/); await expect(page.getByRole('status')).toContainText('Chào mừng bạn đến với movapost.');
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
test('mobile labels keyboard navigation and public login layout remain usable', async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/login'); await expect(page.getByLabel('Email đã xác minh')).toBeVisible(); await expect(page.getByLabel('Mật khẩu', { exact: true })).toBeVisible(); await page.keyboard.press('Tab'); await expect(page.getByRole('link', { name: 'Đến nội dung chính' })).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.getByRole('heading', { name: 'Đăng nhập bằng mật khẩu' }).focus(); await page.screenshot({ path: '../../../.demo/spec003-implementation/customer-login-mobile.png', fullPage: true, animations: 'disabled' });
});

test('public website routes, utilities and mobile navigation disclose pending services', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto('/');
  await expect(page.getByRole('heading', { name: /Mỗi kiện hàng/ })).toBeVisible();
  await expect(page.getByRole('img', { name: /Minh họa các kiện hàng/ })).toBeVisible();
  await page.screenshot({ path: '../../../.demo/spec003-implementation/public-home-desktop.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('tab', { name: 'Ước tính cước' }).click();
  await expect(page.getByRole('tabpanel')).toContainText('SẮP RA MẮT');
  await page.getByRole('tabpanel').getByRole('link', { name: 'Xem thông tin tiện ích' }).click();
  await expect(page.getByRole('heading', { name: 'Ước tính cước phí' })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 }); await page.goto('/');
  await page.screenshot({ path: '../../../.demo/spec003-implementation/public-home-mobile.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Mở menu' }).click();
  await page.getByRole('navigation', { name: 'Điều hướng chính' }).getByRole('link', { name: 'Tin & hướng dẫn' }).click();
  await expect(page.getByRole('heading', { name: 'Cập nhật & hướng dẫn' })).toBeVisible();
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
test('movapost brand, keyboard utility tabs, reduced motion and responsive navigation', async ({ page, context }) => {
  await fixture(context);await page.emulateMedia({reducedMotion:'reduce'});await page.goto('/');
  await expect(page.getByRole('link',{name:'movapost., về trang chủ'}).first()).toBeVisible();await expect(page.getByRole('link',{name:'ĐĂNG NHẬP / ĐĂNG KÝ',exact:true})).toBeVisible();
  await page.getByRole('tab',{name:'Tra cứu vận đơn'}).focus();await page.keyboard.press('ArrowRight');await expect(page.getByRole('tab',{name:'Ước tính cước'})).toBeFocused();await expect(page.getByRole('tab',{name:'Ước tính cước'})).toHaveAttribute('aria-selected','true');
  expect(await page.locator('.ps-hero-art img').evaluate(element=>getComputedStyle(element).animationName)).toBe('none');
  await page.setViewportSize({width:360,height:800});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.getByRole('button',{name:'Mở menu'}).click();await page.getByRole('navigation',{name:'Điều hướng chính'}).getByRole('link',{name:'Hỗ trợ',exact:true}).click();await expect(page.getByRole('heading',{name:'Hỗ trợ khách hàng'})).toBeVisible();
});

test('registration success opens login with a centered dismissible welcome popup only after server confirmation', async ({ page, context }) => {
  let release!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  await fixture(context,false,async (route,call)=>{if(!call.path.endsWith('/verifications'))return false;await pending;await route.fulfill({status:204});return true;});
  await page.setViewportSize({width:390,height:844});await page.goto(`/verify-email#proofId=${proofId}&expiresAt=${new Date(Date.now()+120000).toISOString()}`);
  await page.getByLabel('Mã xác minh 6 chữ số').fill('123456');await page.getByRole('button',{name:'Xác minh tài khoản'}).click();
  await expect(page.getByRole('button',{name:'Đang xác minh…'})).toBeDisabled();await expect(page.locator('.cp-success-popup')).toHaveCount(0);expect(new URL(page.url()).pathname).toBe('/verify-email');
  release();await expect(page).toHaveURL(/\/login$/);await expect(page.getByRole('heading',{name:'Đăng nhập bằng mật khẩu'})).toBeVisible();
  const popup=page.locator('.cp-success-popup');await expect(popup).toContainText('Bạn đã đăng ký thành công!');await expect(popup).toContainText('Chào mừng bạn đến với movapost.');
  await expect(popup).toHaveAttribute('role','status');const box=await popup.boundingBox();expect(box).not.toBeNull();expect(Math.abs(box!.x+box!.width/2-195)).toBeLessThan(2);expect(box!.y).toBeLessThan(24);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.screenshot({path:'../../../.demo/spec003-implementation/auth-success-mobile.png',fullPage:true,animations:'disabled'});
  await page.setViewportSize({width:1280,height:900});const desktop=await popup.boundingBox();expect(Math.abs(desktop!.x+desktop!.width/2-640)).toBeLessThan(2);
  await page.screenshot({path:'../../../.demo/spec003-implementation/auth-success-desktop.png',fullPage:true,animations:'disabled'});
  await page.getByRole('button',{name:'Đóng thông báo thành công'}).click();await expect(popup).toHaveCount(0);await expect(page.getByLabel('Email đã xác minh')).toBeVisible();
  await page.reload();await expect(popup).toHaveCount(0);
});
test('rejected password reset stays on its form and never shows success or redirects', async ({ page, context }) => {
  await fixture(context,false,async(route,call)=>{if(!call.path.endsWith('/recovery-confirm'))return false;await route.fulfill({status:422,contentType:'application/json',body:JSON.stringify({code:'TOKEN_INVALID_OR_EXPIRED'})});return true;});
  await page.goto(`/recover/confirm#proofId=${proofId}&proof=${'B'.repeat(43)}`);await page.getByLabel('Mật khẩu mới',{exact:true}).fill('synthetic-recovered-password');await page.getByLabel('Nhập lại mật khẩu mới').fill('synthetic-recovered-password');await page.getByRole('button',{name:'Xác nhận mật khẩu mới'}).click();
  await expect(page.getByRole('alert')).toContainText('không còn hiệu lực');await expect(page).toHaveURL(/\/recover\/confirm$/);await expect(page.locator('.cp-success-popup')).toHaveCount(0);
});
