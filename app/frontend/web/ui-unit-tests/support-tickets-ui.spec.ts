import { test, expect } from '@playwright/test';
import type { BrowserContext, Route } from '@playwright/test';
import { supportDetailFixture, supportId, supportSummaryFixture } from './support/support-ticket-fixture';
type Call = { path: string; method: string; body?: Record<string, unknown>; key?: string; version?: string };
async function fixture(context: BrowserContext, extra?: (route: Route, call: Call) => Promise<boolean>) {
  const calls: Call[] = [], owner = '10000000-0000-4000-8000-000000000001';
  await context.route('**/api/v1/**', async route => {
    const r = route.request(), path = new URL(r.url()).pathname, call = { path, method: r.method(), body: r.postData() ? r.postDataJSON() : undefined, key: r.headers()['idempotency-key'], version: r.headers()['if-match'] }; calls.push(call);
    if (extra && await extra(route, call)) return;
    const reply = (body: unknown, status = 200) => route.fulfill({ contentType: 'application/json', status, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-ticket-csrf' });
    if (path.endsWith('/refresh')) return reply({ accessToken: 'synthetic-ticket-access', accountId: owner, expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: owner, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path === '/api/v1/customer/support-tickets') return reply({ items: [supportSummaryFixture], page: Number(new URL(r.url()).searchParams.get('page') ?? '0'), size: 20, hasNext: false });
    if (path.endsWith('/' + supportId)) return reply(supportDetailFixture());
    return reply({ code: 'ISOLATED_UI_UNSUPPORTED' }, 503);
  }); return calls;
}
test('Ticket create keeps an unknown command locked and resolves by receipt without another write', async ({ page, context }) => {
  let committed = false, receipts = 0;
  const calls = await fixture(context, async (route, call) => {
    if (call.method === 'POST' && call.path === '/api/v1/customer/support-tickets') { committed = true; await route.abort('failed'); return true; }
    if (call.path.includes('/support-receipts/CREATE/')) { receipts++; await route.fulfill({ status: receipts === 1 ? 404 : 200, contentType: 'application/json', body: JSON.stringify(receipts === 1 ? { code: 'RESOURCE_NOT_FOUND' } : { operationId: 'CREATE', targetId: supportId, outcome: 'COMMITTED', version: 1 }) }); return true; }
    if (committed && call.path.endsWith('/' + supportId)) { await route.fulfill({ contentType: 'application/json', body: JSON.stringify(supportDetailFixture()) }); return true; } return false;
  });
  await page.goto('/personal/support'); await page.getByRole('button', { name: 'Tạo yêu cầu hỗ trợ', exact: true }).click(); await page.getByLabel('Tiêu đề', { exact: true }).fill(supportSummaryFixture.subject); await page.getByLabel('Mô tả vấn đề').fill('Synthetic initial message'); await page.getByRole('button', { name: 'Gửi yêu cầu hỗ trợ', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định'); await expect(page.getByLabel('Tiêu đề', { exact: true })).toBeDisabled(); await expect(page.getByText('Đã gửi thành công', { exact: false })).toHaveCount(0);
  await page.getByRole('button', { name: 'Kiểm tra kết quả gửi' }).click(); await expect(page.getByRole('alert')).toContainText('vẫn chưa xác định');
  await page.getByRole('button', { name: 'Kiểm tra kết quả gửi' }).click(); await expect(page.getByRole('heading', { name: 'Chi tiết yêu cầu', exact: true })).toBeVisible(); await expect(page.locator('.su-conversation')).toContainText('Synthetic initial message'); expect(calls.filter(c => c.method === 'POST' && !c.path.includes('/customer-auth/'))).toHaveLength(1);
});
test('Ticket filter hides prior cohort while loading and failed reads cannot become empty success', async ({ page, context }) => {
  let release!: () => void; const pending = new Promise<void>(r => { release = r; });
  await fixture(context, async (route, call) => { if (call.path !== '/api/v1/customer/support-tickets' || !route.request().url().includes('NEEDS_INFORMATION')) return false; await pending; await route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ code: 'SERVICE_UNAVAILABLE' }) }); return true; });
  await page.goto('/personal/support'); await expect(page.locator('.su-ticket-list')).toContainText(supportSummaryFixture.subject); await page.getByLabel('Trạng thái', { exact: true }).selectOption('NEEDS_INFORMATION'); await expect(page.getByRole('status')).toContainText('Đang tải yêu cầu'); await expect(page.locator('.su-ticket-list')).toHaveCount(0); release(); await expect(page.getByRole('alert')).toBeVisible(); await expect(page.getByText('Chưa có yêu cầu phù hợp với bộ lọc.')).toHaveCount(0); await expect(page.getByRole('button', { name: 'Tạo yêu cầu hỗ trợ', exact: true })).toBeDisabled();
});
for (const width of [1440, 390]) test(`Ticket conversation is readable and hostile markup remains text at ${width}px`, async ({ page, context }) => {
  await fixture(context, async (route, call) => { if (!call.path.endsWith('/' + supportId)) return false; const value = supportDetailFixture(); value.publicMessages[0].message = '<img src=x onerror=alert(1)>\nTiếng Việt có dấu'; await route.fulfill({ contentType: 'application/json', body: JSON.stringify(value) }); return true; });
  await page.setViewportSize({ width, height: 1000 }); await page.goto('/personal/support?ticket=' + supportId); await expect(page.locator('.su-conversation')).toContainText('<img src=x onerror=alert(1)>'); await expect(page.locator('.su-conversation img')).toHaveCount(0); await expect(page.getByRole('button', { name: /bồi thường|hủy vận đơn|thanh toán/i })).toHaveCount(0); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await page.screenshot({ path: `../../../.demo/support-account-review/ticket-${width}.png`, fullPage: true, animations: 'disabled' });
});
test('stale reply preserves editable content and cannot fabricate a staff result', async ({ page, context }) => {
  const calls = await fixture(context, async (route, call) => { if (!call.path.endsWith('/replies')) return false; await route.fulfill({ status: 409, contentType: 'application/json', body: JSON.stringify({ code: 'PRECONDITION_STALE' }) }); return true; });
  await page.goto('/personal/support?ticket=' + supportId); await page.getByLabel('Nội dung phản hồi').fill('Synthetic retained reply'); await page.getByRole('button', { name: 'Gửi phản hồi', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('đã thay đổi'); await expect(page.getByLabel('Nội dung phản hồi')).toHaveValue('Synthetic retained reply'); expect(calls.filter(c => c.method === 'POST' && !c.path.includes('/customer-auth/'))).toHaveLength(1); expect(calls.find(c => c.method === 'POST' && !c.path.includes('/customer-auth/'))?.version).toBe('"v1"');
});
test('Support operations deny customers without issuing assignment or state writes', async ({ page, context }) => {
  const calls = await fixture(context, async (route, call) => { if (!call.path.startsWith('/api/v1/support-operations/')) return false; await route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ code: 'SUPPORT_AUTHORITY_REQUIRED' }) }); return true; });
  await page.goto('/operations/support'); await expect(page.getByRole('heading', { name: 'Xử lý hỗ trợ', exact: true })).toBeVisible(); await expect(page.getByRole('alert')).toContainText('chưa có quyền'); await expect(page.getByRole('button', { name: 'Nhận xử lý' })).toHaveCount(0); expect(calls.some(c => c.method !== 'GET' && !c.path.includes('/customer-auth/'))).toBe(false);
});
