import { test, expect } from '@playwright/test';
import type { Page } from '@playwright/test';
import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (!directory) throw new Error('OWNED_ADMIN_CORE_CONTEXT_REQUIRED');
test('Customer submission through independent review activates real Organization and reconciles Dashboard Work and Audit', async ({ browser, page }, testInfo) => {
  page.setDefaultTimeout(15000);
  const mark = (stage: string) => writeFile(resolve(directory!, 'safe-core-stage.json'), JSON.stringify({ stage }));
  await mark('CUSTOMER_LOGIN');
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { password: string; customerEmail: string; reviewer: string; decider: string; reviewerId: string; inboxOrigin: string; inboxNonce: string; businessIntake?: boolean };
  if (fixture.businessIntake) test.setTimeout(180000);
  const login = async (target: Page, username: string) => { await target.goto('/admin/login');await target.getByLabel('Tên đăng nhập', { exact: true }).fill(username);await target.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);await target.getByRole('button', { name: 'Đăng nhập', exact: true }).click();await expect(target.getByRole('heading', { name: 'Tổng quan điều hành', exact: true })).toBeVisible();await target.waitForLoadState('networkidle'); };
  await page.goto('/login');await page.getByLabel('Email đã xác minh').fill(fixture.customerEmail);await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();await expect(page).toHaveURL(new RegExp('/personal$'));
  await mark('APPLICATION_DRAFT');
  await page.goto('/personal/applications');await page.getByRole('button', { name: 'Tạo bản nháp Merchant', exact: true }).click();await expect(page).toHaveURL(new RegExp('/personal/applications/[a-f0-9-]{36}$'));const application = new URL(page.url()).pathname.split('/').at(-1)!;
  await page.locator('#application-entity').selectOption('COMPANY');
  for (const [key, value] of Object.entries({ registeredName: 'Synthetic Browser Merchant', jurisdiction: 'SYNTHETIC-J1', legalIdentifier: 'SYNTHETIC-CORE-1', representativeName: 'Synthetic Representative', representativeEmail: 'business-contact@example.invalid', representativeRelationship: 'Authorized representative' })) await page.locator('#application-' + key).fill(value);
  for (const prefix of ['registered', 'operating']) { await page.getByRole('checkbox', { name: prefix === 'registered' ? 'Bổ sung địa chỉ đăng ký' : 'Bổ sung địa chỉ hoạt động', exact: true }).check();for (const [key, value] of Object.entries({ country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic test-only address' })) await page.locator('#' + prefix + '-' + key).fill(value); }
  const saved = page.waitForResponse(r => r.request().method() === 'PUT' && new URL(r.url()).pathname.endsWith('/' + application));await page.getByRole('button', { name: 'Lưu bản nháp', exact: true }).click();expect((await saved).status()).toBe(200);
  await page.getByRole('button', { name: 'Gửi email xác minh liên hệ', exact: true }).click();
  await mark('CONTACT_VERIFICATION');
  let contact = '';await expect.poll(async () => { const r = await page.request.get(fixture.inboxOrigin + '/contact', { headers: { 'X-Test-Nonce': fixture.inboxNonce } });if (r.status() !== 200) return false;contact = (await r.json() as { link: string }).link;return !!contact; }).toBe(true);
  await page.goto(contact);await page.getByRole('button', { name: 'Xác nhận email liên hệ', exact: true }).click();await expect(page).toHaveURL(new RegExp('/personal/applications/' + application + '$'));contact = '';
  await mark('EVIDENCE');
  for (const purpose of ['REGISTRATION', 'REPRESENTATIVE_AUTHORIZATION']) {
    await page.locator('#application-evidence-purpose').selectOption(purpose);await page.locator('#application-evidence-file').setInputFiles({ name: 'synthetic-evidence.png', mimeType: 'image/png', buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGP4X8Xwv4qBgVs3mls3GgAkXgQZvgJAbwAAAABJRU5ErkJggg==', 'base64') });
    const uploaded = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/evidence'));await page.getByRole('button', { name: 'Tải lên & lưu tài liệu', exact: true }).click();expect((await uploaded).status()).toBe(201);await page.waitForLoadState('networkidle');
  }
  await mark('SUBMISSION');
  await page.locator('#application-display-name').fill('Synthetic Core Merchant');await page.getByRole('checkbox', { name: 'Synthetic accuracy statement', exact: true }).check();await page.getByRole('checkbox', { name: 'Synthetic representation statement', exact: true }).check();const submitted = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/submit'));await page.getByRole('button', { name: 'Nộp hồ sơ xét duyệt', exact: true }).click();expect((await submitted).status()).toBe(200);await page.waitForLoadState('networkidle');
  const decisionContext = await browser.newContext({ baseURL: new URL(page.url()).origin }), reviewerContext = await browser.newContext({ baseURL: new URL(page.url()).origin });
  decisionContext.setDefaultTimeout(15000);reviewerContext.setDefaultTimeout(15000);
  const httpFacts: { path: string; status: number; keys?: string[]; items?: number; code?: string }[] = [];
  for (const ctx of [decisionContext, reviewerContext]) ctx.on('response', response => {
    const path = new URL(response.url()).pathname;if (!path.startsWith('/api/v1/admin/')) return;
    const entry: typeof httpFacts[number] = { path: path.replace(/[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}/g, ':id'), status: response.status() };httpFacts.push(entry);
    void response.json().then((v: unknown) => { if (!v || typeof v !== 'object' || Array.isArray(v)) return;entry.keys = Object.keys(v);if ('items' in v && Array.isArray(v.items)) entry.items = v.items.length;if ('code' in v && typeof v.code === 'string' && /^[A-Z_]{3,80}$/.test(v.code)) entry.code = v.code; }).catch(() => undefined);
  });
  try {
    await mark('ASSIGNMENT');
    const decider = await decisionContext.newPage(), reviewer = await reviewerContext.newPage();await login(decider, fixture.decider);await decider.goto('/admin/merchants/applications?applicationId=' + application);
    // Assignment controls remain canonical; selectors bind their existing label, not a test-only authority.
    const select = decider.getByRole('combobox').filter({ has: decider.locator('option[value="' + fixture.reviewerId + '"]') });await select.selectOption(fixture.reviewerId);await decider.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic full-stack assignment');const assignment = decider.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/review-assignment'));await decider.getByRole('button', { name: 'Phân công', exact: true }).click();expect((await assignment).status()).toBe(200);await decider.waitForLoadState('networkidle');
    // BIA verifies the shipment journey, not simultaneous Admin browsing.
    // Close each role's live session before the next actor, retaining the
    // production fail-closed population lock and all independent decisions.
    if (fixture.businessIntake) { await decider.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(decider).toHaveURL(/\/admin\/login$/);await decider.goto('about:blank'); }
    await mark('REVIEW');
    await login(reviewer, fixture.reviewer);if (fixture.businessIntake) await reviewer.goto('/admin/merchants/applications?applicationId=' + application);else { await reviewer.goto('/admin/account');await expect(reviewer.getByRole('link', { name: 'Mở hồ sơ', exact: true })).toBeVisible();await reviewer.getByRole('link', { name: 'Mở hồ sơ', exact: true }).click(); }await reviewer.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic full-stack review');await reviewer.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true }).click();await reviewer.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
    for (const name of ['Hồ sơ đăng ký hợp lệ', 'Thẩm quyền đại diện hợp lệ', 'Liên hệ đã xác minh', 'Đã đối chiếu trùng lặp', 'Không có xung đột đã biết']) await reviewer.getByRole('checkbox', { name, exact: true }).check();await reviewer.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic complete checklist');await Promise.all([reviewer.waitForLoadState('networkidle'), decider.waitForLoadState('networkidle'), page.waitForLoadState('networkidle')]);
    if (fixture.businessIntake) {
      const review = reviewer.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/review'));
      await reviewer.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true }).click();expect((await review).status()).toBe(200);await reviewer.waitForLoadState('networkidle');
    } else {
    const hold = await page.request.post(fixture.inboxOrigin + '/admission-hold?hold', { headers: { 'X-Test-Nonce': fixture.inboxNonce } });expect(hold.status()).toBe(204);
    const ready = reviewer.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/review'));await reviewer.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true }).click();const rejected = await ready;expect(rejected.status()).toBe(503);const original = rejected.request();
    const release = await page.request.post(fixture.inboxOrigin + '/admission-hold?release', { headers: { 'X-Test-Nonce': fixture.inboxNonce } });expect(release.status()).toBe(204);
    await expect(reviewer.getByRole('button', { name: 'Th\u1eed l\u1ea1i \u0111\u00fang y\u00eau c\u1ea7u', exact: true })).toBeVisible();
    const retry = reviewer.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/review'));
    await reviewer.getByRole('button', { name: 'Th\u1eed l\u1ea1i \u0111\u00fang y\u00eau c\u1ea7u', exact: true }).click();const committed = await retry;expect(committed.status()).toBe(200);
    expect(committed.request().headers()['idempotency-key']).toBe(original.headers()['idempotency-key']);expect(committed.request().headers()['if-match']).toBe(original.headers()['if-match']);expect(committed.request().postData()).toBe(original.postData());await reviewer.waitForLoadState('networkidle');
    }
    if (fixture.businessIntake) { await reviewer.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(reviewer).toHaveURL(/\/admin\/login$/);await reviewer.goto('about:blank');await login(decider, fixture.decider); }
    await mark('APPROVAL');
    if (fixture.businessIntake) await decider.goto('/admin/merchants/applications?applicationId=' + application);else { await decider.goto('/admin/account');await decider.getByRole('link', { name: 'Mở hồ sơ', exact: true }).click(); }await decider.getByLabel('Quyết định', { exact: true }).selectOption('APPROVE');await decider.getByLabel('Lý do phê duyệt', { exact: true }).fill('Synthetic independent approval');await decider.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill(fixture.password);const approved = decider.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/approval'));await decider.getByRole('button', { name: 'Xác thực & phê duyệt hồ sơ', exact: true }).click();expect((await approved).status()).toBe(200);await decider.waitForLoadState('networkidle');
    await mark('DASHBOARD');
    // The existing Admin acceptance keeps all its assertions. BIA reuses only
    // the canonical approval journey; it does not rerun unrelated Portal UX.
    if (!fixture.businessIntake) {
    await decider.goto('/admin?tab=merchant');await expect(decider.getByRole('region', { name: 'Dashboard v2' })).toBeVisible();await expect(decider.locator('.ap-v2-kpi').filter({ hasText: 'Đã duyệt' }).locator('strong')).toHaveText('1');await expect(decider.locator('.ap-v2 > section.ap-panel tbody')).toContainText(application);
    await decider.locator('.ap-v2-kpi').filter({ hasText: 'Đã duyệt' }).click();await expect(decider.locator('.ap-v2 > section.ap-panel tbody tr')).toHaveCount(1);await expect(decider.locator('.ap-v2 > section.ap-panel tbody')).not.toContainText('business-contact@example.invalid');
    await decider.goto('/admin/account');await expect(decider.getByText('Không có công việc có thể thực hiện trong phạm vi hiện tại.', { exact: true })).toBeVisible();
    await mark('AUDIT_AND_NOTICES');
    await decider.goto('/admin/audit');await expect(decider.locator('tbody')).toContainText('APPROVE');await expect(decider.locator('tbody')).not.toContainText('business-contact@example.invalid');await expect.poll(async () => { const r = await page.request.get(fixture.inboxOrigin + '/contact', { headers: { 'X-Test-Nonce': fixture.inboxNonce } });return r.status() === 200 ? (await r.json() as { count: number }).count : 0; }).toBe(2);
    await decider.setViewportSize({ width: 390, height: 844 });expect(await decider.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);await decider.screenshot({ path: resolve(directory!, 'admin-core-real-mobile.png'), fullPage: true, animations: 'disabled' });
    }
    for (const target of fixture.businessIntake ? [decider] : [decider, reviewer]) { expect(await target.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);await target.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(target).toHaveURL(new RegExp('/admin/login$')); }
    await page.reload();await page.goto('/personal/applications/' + application);await expect(page.getByText('Hồ sơ đã được ghi nhận. Thông tin đã nộp được lưu theo từng lần xét duyệt.', { exact: true })).toBeVisible();
    if (fixture.businessIntake) {
      await mark('EXPLICIT_SHIPPING_SETUP');
      const setup = await page.request.post(fixture.inboxOrigin + '/shipping-setup?' + application, { headers: { 'X-Test-Nonce': fixture.inboxNonce } });expect(setup.status()).toBe(200);
      const { organizationId } = await setup.json() as { organizationId: string };
      await page.goto('/merchant/' + organizationId + '/create');
      await expect(page.locator('#sender-name')).toBeVisible();
      for (const prefix of ['sender', 'recipient']) for (const [field, value] of Object.entries({ name: 'Synthetic ' + prefix, phone: '0901234567', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic test-only address' })) await page.locator('#' + prefix + '-' + field).fill(value);
      await page.getByRole('button', { name: '+ Thêm kiện (tối đa 5)', exact: true }).click();
      for (let i = 0; i < 2; i++) { await page.locator('#parcel-description-' + i).fill('Synthetic Business parcel ' + (i + 1));for (const [key, value] of Object.entries({ quantity: '1', weightGrams: '500', declaredValueVnd: '0', lengthMm: '10', widthMm: '10', heightMm: '10' })) await page.locator('#' + key + i).fill(value); }
      await page.getByRole('button', { name: 'Xem lại thông tin', exact: true }).click();await expect(page.getByRole('heading', { name: 'Xác nhận thông tin doanh nghiệp', exact: true })).toBeVisible();
      await page.locator('#pilot-origin').selectOption('SIM-A1');await page.locator('#pilot-destination').selectOption('SIM-A2');await page.getByRole('checkbox', { name: /Tôi xác nhận chỉ khai hàng thông thường/ }).check();
      await mark('BUSINESS_QUOTE_BOOKING');await page.getByRole('button', { name: 'Lấy báo giá mô phỏng', exact: true }).click();await expect(page.locator('.pb-total')).toContainText('50.000');
      await page.getByRole('button', { name: 'Xác nhận đặt vận đơn mô phỏng', exact: true }).click();await expect(page.getByRole('heading', { name: 'Chi tiết vận đơn doanh nghiệp', exact: true })).toBeVisible();await expect(page.locator('.pb-unpaid')).toHaveText('UNPAID');
      const shipment = new URL(page.url()).pathname.split('/').at(-1)!;await page.getByRole('tab', { name: 'Kiện hàng', exact: true }).click();const parcelCodes = page.getByRole('tabpanel').locator('code.om-parcel-code');await expect(parcelCodes).toHaveCount(2);const codes = await parcelCodes.allTextContents();expect(codes).toHaveLength(2);
      await page.getByRole('tab', { name: 'Chứng từ', exact: true }).click();const download = page.waitForEvent('download');await page.getByRole('button', { name: 'Tải nhãn tất cả kiện', exact: true }).click();await (await download).saveAs(resolve(directory!, 'bia-labels.pdf'));
      const opsContext = await browser.newContext({ baseURL: new URL(page.url()).origin });
      try {
        const ops = await opsContext.newPage();ops.setDefaultTimeout(15000);let invitation = '';
        await expect.poll(async () => { const r = await page.request.get(fixture.inboxOrigin + '/ops-invitation', { headers: { 'X-Test-Nonce': fixture.inboxNonce } });if (r.status() !== 200) return false;invitation = (await r.json() as { link: string }).link;return !!invitation; }).toBe(true);
        await mark('GENUINE_OPS_VERIFICATION');await ops.goto(invitation);invitation = '';await ops.getByLabel('Mật khẩu mới', { exact: true }).fill(fixture.password);await ops.getByLabel('Nhập lại mật khẩu', { exact: true }).fill(fixture.password);await ops.getByRole('button', { name: 'Kích hoạt tài khoản', exact: true }).click();await expect(ops).toHaveURL(/\/ops\/login$/);
        expect((await page.request.post(fixture.inboxOrigin + '/ops-authority', { headers: { 'X-Test-Nonce': fixture.inboxNonce } })).status()).toBe(204);
        await ops.getByLabel('Email nhân viên').fill('bia-operations@example.invalid');await ops.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);await ops.getByRole('button', { name: 'Đăng nhập', exact: true }).click();await expect(ops.getByRole('heading', { name: 'Tiếp nhận tại Station', exact: true })).toBeVisible();
        await mark('BUSINESS_SCAN_ACCEPTANCE');
        for (let i = 0; i < codes.length; i++) { await ops.locator('#intake-barcode').fill(codes[i]);await ops.getByRole('button', { name: 'Scan & đối chiếu kiện', exact: true }).click();await expect(ops.locator('.so-intake')).toContainText((i + 1) + '/2 kiện đã scan');if (i === 0) await expect(ops.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true })).toBeDisabled(); }
        await ops.getByRole('checkbox', { name: /Tôi đã đối chiếu đủ kiện/ }).check();await ops.getByRole('button', { name: 'Xác nhận tiếp nhận đủ kiện', exact: true }).click();await expect(ops.locator('.so-intake .so-heading')).toContainText('Đã tiếp nhận tại Station');await expect(ops.locator('.so-intake')).toContainText('UNPAID');
        await ops.screenshot({ path: resolve(directory!, 'bia-operations-desktop.png'), fullPage: true });await ops.setViewportSize({ width: 390, height: 844 });expect(await ops.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);await ops.screenshot({ path: resolve(directory!, 'bia-operations-mobile.png'), fullPage: true });
        await page.goto('/merchant/' + organizationId + '/shipments/' + shipment);await expect(page.locator('.pb-unpaid')).toHaveText('UNPAID');await page.getByRole('tab', { name: 'Hành trình', exact: true }).click();await expect(page.getByRole('tabpanel')).toContainText('Carrier đã tiếp nhận tại SIM-DROP-A1');
        await mark('BUSINESS_AT_STATION_VERIFIED');await ops.getByRole('button', { name: 'Đăng xuất', exact: true }).click();await expect(ops).toHaveURL(/\/ops\/login$/);
      } finally { await opsContext.close(); }
    }
  } finally {
    const states = await Promise.all([decisionContext, reviewerContext].flatMap(c => c.pages()).map(async p => ({ path: new URL(p.url()).pathname, headings: await p.getByRole('heading').allTextContents(), alerts: await p.getByRole('alert').allTextContents(), buttons: await p.getByRole('button').allTextContents() })));
    await writeFile(resolve(directory!, 'safe-role-ui-state.json'), JSON.stringify(states));await writeFile(resolve(directory!, 'safe-http-facts.json'), JSON.stringify(httpFacts));
    const closed = await Promise.allSettled([decisionContext.close(), reviewerContext.close()]);if (testInfo.status === testInfo.expectedStatus) expect(closed.every(v => v.status === 'fulfilled')).toBe(true);
  }
});
