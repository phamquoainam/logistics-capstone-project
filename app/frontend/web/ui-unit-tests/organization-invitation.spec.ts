import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
const id = '10000000-0000-4000-8000-000000000901', member = '10000000-0000-4000-8000-000000000902', account = '10000000-0000-4000-8000-000000000001';
const token = 'A'.repeat(43), path = '/organization-invitations/' + id;
async function fixture(context: BrowserContext, state = { guest: false, uncertain: false, denied: false }) {
  const calls: { path: string; key?: string; body: unknown }[] = [];
  await context.route('**/api/v1/**', async route => {
    const request = route.request(), pathname = new URL(request.url()).pathname;
    const reply = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(body) });
    if (pathname.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (pathname.endsWith('/refresh') || pathname.endsWith('/login')) {
      if (state.guest && !pathname.endsWith('/login')) return reply({ code: 'SESSION_INVALID' }, 401);
      state.guest = false; return reply({ accountId: account, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    }
    if (pathname.endsWith('/logout')) return route.fulfill({ status: 204 });
    if (pathname.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    calls.push({ path: pathname, key: request.headers()['idempotency-key'], body: request.postDataJSON() });
    if (state.uncertain) { state.uncertain = false; return route.abort('connectionfailed'); }
    if (state.denied) return reply({ code: 'TEAM_INVITATION_INVALID_OR_EXPIRED' }, 422);
    const accept = pathname.endsWith('/accept');
    return reply({ invitationId: id, outcome: accept ? 'ACCEPTED' : 'DECLINED', membershipId: accept ? member : null });
  });
  return calls;
}
test('recipient landing scrubs proof and requires explicit acceptance with current session and CSRF', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(path + '#proof=' + token);
  await expect(page.getByRole('button', { name: 'Chấp nhận lời mời', exact: true })).toBeVisible();
  expect(new URL(page.url()).hash).toBe(''); expect(await page.locator('body').innerText()).not.toContain(token); expect(calls).toHaveLength(0);
  await page.getByRole('button', { name: 'Chấp nhận lời mời', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã chấp nhận lời mời'); expect(calls).toHaveLength(1); expect(calls[0].body).toEqual({ proof: token });
  expect(await page.evaluate(() => JSON.stringify({ local: { ...localStorage }, session: { ...sessionStorage } }))).not.toContain(token);
});
test('lost acceptance response freezes operation and reuses key and proof', async ({ page, context }) => {
  const calls = await fixture(context, { guest: false, uncertain: true, denied: false }); await page.goto(path + '#proof=' + token);
  await page.getByRole('button', { name: 'Chấp nhận lời mời', exact: true }).click(); await expect(page.getByRole('button', { name: 'Từ chối', exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Kiểm tra lại đúng yêu cầu', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã chấp nhận lời mời'); expect(calls).toHaveLength(2); expect(calls[1]).toEqual(calls[0]);
});
test('guest authenticates before decision without carrying proof through URL or login body', async ({ page, context }) => {
  const calls = await fixture(context, { guest: true, uncertain: false, denied: false }); await page.goto(path + '#proof=' + token);
  await page.getByRole('link', { name: 'Đăng nhập để tiếp tục', exact: true }).click();
  await page.getByLabel('Email đã xác minh', { exact: true }).fill('synthetic@example.invalid'); await page.getByLabel('Mật khẩu', { exact: true }).fill('Synthetic-test-only-password-2026');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(new RegExp(path + '$')); await page.getByRole('button', { name: 'Từ chối', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã từ chối lời mời'); expect(calls).toHaveLength(1); expect(calls[0].path).toContain('/decline');
});
test('invalid or wrong-recipient invitation never grants success and can be retried explicitly', async ({ page, context }) => {
  await fixture(context, { guest: false, uncertain: false, denied: true }); await page.goto(path + '#proof=' + token);
  await page.getByRole('button', { name: 'Chấp nhận lời mời', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('không còn hiệu lực hoặc email tài khoản chưa đúng');
  await expect(page.getByRole('link', { name: 'Về không gian cá nhân' })).toHaveCount(0);
});
