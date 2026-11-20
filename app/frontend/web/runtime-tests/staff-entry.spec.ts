import { test, expect } from '@playwright/test';

test('dedicated Staff activates from actual SMTP then uses isolated HTTPS sessions and denies Customer authority', async ({ page, context }) => {
  const origin = new URL(process.env.S003_BROWSER_ORIGIN ?? '').origin;
  const inbox = new URL(process.env.S003_BROWSER_INBOX ?? '');
  if (inbox.protocol !== 'http:' || inbox.hostname !== '127.0.0.1' || inbox.pathname !== '/' || inbox.username || inbox.password) throw new Error('OWNED_INBOX_REQUIRED');
  let invitation = '';
  await expect.poll(async () => {
    const response = await fetch(new URL('/mail/0', inbox), { headers: { 'X-Owned-Inbox-Nonce': process.env.S003_BROWSER_INBOX_NONCE ?? '' }, signal: AbortSignal.timeout(3000) });
    if (response.status === 202) return false;
    if (response.status !== 200) throw new Error('STAFF_TEST_INBOX_FAILED');
    const v: unknown = await response.json();
    if (!v || typeof v !== 'object' || Object.keys(v).length !== 1 || !('invitation' in v) || typeof v.invitation !== 'string' || !v.invitation.startsWith(origin + '/staff/activate#proofId=')) throw new Error('STAFF_TEST_INBOX_INVALID');
    invitation = v.invitation; return true;
  }, { timeout: 15000 }).toBe(true);
  await page.goto(invitation); invitation = '';
  await expect(page).toHaveURL(origin + '/staff/activate');
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('Synthetic browser Staff password!9');
  await page.getByLabel('Nhập lại mật khẩu', { exact: true }).fill('Synthetic browser Staff password!9');
  await page.getByRole('button', { name: 'Kích hoạt tài khoản', exact: true }).click();
  await expect(page).toHaveURL(origin + '/staff/login');
  let access = '';
  page.on('response', response => { if (response.url() === origin + '/api/v1/staff-auth/login' && response.ok()) void response.json().then((v: { accessToken: string }) => { access = v.accessToken; }); });
  await page.getByLabel('Email nhân viên').fill('browser-cskh@example.invalid');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('Synthetic browser Staff password!9');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Hỗ trợ doanh nghiệp', exact: true })).toBeVisible();
  await expect.poll(() => access.length > 100).toBe(true);
  const cookies = await context.cookies();
  for (const name of ['__Host-argopost.staff.csrf', '__Host-argopost.staff.refresh']) expect(cookies.find(c => c.name === name)).toMatchObject({ secure: true, httpOnly: true, sameSite: 'Strict', path: '/' });
  expect(cookies.some(c => c.name.includes('customer.refresh'))).toBe(false);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
  const denied = await page.request.get('/api/v1/customer-auth/me', { headers: { Authorization: 'Bearer ' + access, Origin: origin } }); expect(denied.status()).toBe(401);
  const deniedQueue = await page.request.get('/api/v1/support-operations/organization-queue?organizationId=10000000-0000-4000-8000-000000000099', { headers: { Authorization: 'Bearer ' + access, Origin: origin } }); expect(deniedQueue.status()).toBe(403);
  await page.reload(); await expect(page.getByRole('heading', { name: 'Hỗ trợ doanh nghiệp', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Đăng xuất CSKH', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Đăng nhập CSKH', exact: true })).toBeVisible();
  const old = await page.request.get('/api/v1/staff-auth/me', { headers: { Authorization: 'Bearer ' + access, Origin: origin } }); access = ''; expect(old.status()).toBe(401);
});
