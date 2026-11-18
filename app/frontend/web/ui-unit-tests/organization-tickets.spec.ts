import { test, expect } from '@playwright/test';
import { businessToolsFixture, toolsAccount, toolsId } from './support/business-tools-fixture';
import { organizationA } from './support/business-fixture';
const root = '/merchant/' + organizationA, target = toolsId(150), time = '2026-11-17T00:00:00Z';
function ticket() { return { id: target, code: 'AP-OST-' + target.replaceAll('-', ''), organizationId: organizationA, facilityId: null, creatorAccountId: toolsAccount, kind: 'GENERAL', shipmentId: null, category: 'ACCOUNT_HELP', subject: 'Synthetic support question', state: 'NEW', version: 1, createdAt: time, updatedAt: time, requestedPriority: 'NORMAL', priority: 'NORMAL', responseDueAt: null, teamLabel: null, internalAssigneeAccountId: null }; }
function detail() { return { ...ticket(), publicMessages: [{ id: toolsId(151), authorKind: 'MERCHANT', message: 'Synthetic question', createdAt: time, eventType: 'CREATE' }], internalAssignment: { ticketId: target, organizationId: organizationA, assigneeAccountId: null, version: 0, updatedAt: null }, allowedActions: ['REPLY'] }; }
test('Ticket lost create recovers original receipt without blind duplicate and refresh remains bound', async ({ page, context }) => {
  await businessToolsFixture(context); let created = false, writes = 0, key = '';
  await context.route('**/merchant-organizations/' + organizationA + '/support-**', async route => {
    const req = route.request(), u = new URL(req.url()), path = u.pathname; const reply = (v: unknown, status = 200, etag?: string) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(v) });
    if (path.includes('/support-receipts/')) { expect(path).toContain('/CREATE/' + key); return reply({ operationId: toolsId(152), targetId: target, outcome: 'COMMITTED', version: 1 }); }
    if (path.endsWith('/support-tickets') && req.method() === 'POST') { expect(req.headers()['x-csrf-token']).toBeTruthy(); writes++; key = req.headers()['idempotency-key']; created = true; return route.abort('connectionfailed'); }
    if (path.endsWith('/support-tickets/' + target)) return reply(detail(), 200, '"v1"');
    return reply({ items: created ? [ticket()] : [], page: 0, size: 20, hasNext: false });
  });
  await page.goto(root + '/support?view=tickets'); await page.getByRole('button', { name: 'Tạo Ticket', exact: true }).click(); await page.getByLabel('Tiêu đề', { exact: true }).fill('Synthetic support question'); await page.getByLabel('Nội dung', { exact: true }).fill('Synthetic question'); await page.getByRole('button', { name: 'Gửi yêu cầu', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Kết quả chưa xác định'); await expect(page.getByRole('button', { name: 'Gửi yêu cầu', exact: true })).toBeDisabled(); await page.getByRole('button', { name: 'Kiểm tra biên nhận', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết Ticket', exact: true })).toBeVisible(); expect(writes).toBe(1); await expect(page.locator('.os-conversation')).toContainText('Synthetic question'); await page.reload(); await expect(page.locator('.os-conversation')).toContainText('Synthetic question');
});
test('Ticket filters use body-only search and unauthorized responses remove stale data', async ({ page, context }) => {
  await businessToolsFixture(context); let denied = false; const calls: string[] = [];
  await context.route('**/merchant-organizations/' + organizationA + '/support-tickets**', route => { const req = route.request(); calls.push(req.url()); if (req.method() === 'POST') { expect(new URL(req.url()).pathname).toMatch(/\/search$/); expect(req.postDataJSON().search).toBe('Synthetic'); expect(req.headers()['x-csrf-token']).toBeTruthy(); denied = true; }
    return route.fulfill({ status: denied ? 404 : 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(denied ? { code: 'RESOURCE_NOT_FOUND' } : { items: [ticket()], page: 0, size: 20, hasNext: false }) }); });
  await page.goto(root + '/support?view=tickets'); await expect(page.getByText('Synthetic support question', { exact: true })).toBeVisible();
  await page.getByText('Bộ lọc khác', { exact: true }).click(); const filtered = page.waitForRequest(r => new URL(r.url()).searchParams.get('requestedPriority') === 'HIGH'); await page.getByLabel('Mức độ đề nghị', { exact: true }).selectOption('HIGH'); expect(new URL((await filtered).url()).searchParams.get('page')).toBe('0');
  await page.getByLabel('Tìm mã, tiêu đề, tham chiếu').fill('Synthetic'); await page.getByRole('button', { name: 'Tìm Ticket', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Không có quyền'); await expect(page.getByText('Synthetic support question', { exact: true })).toHaveCount(0); expect(calls.every(url => !url.includes('Synthetic'))).toBe(true);
});
test('assignment candidates remain selectable after real-shaped member projection loads', async ({ page, context }) => {
  await businessToolsFixture(context); await context.route('**/merchant-organizations/' + organizationA + '/support-tickets/' + target + '**', route => {
    const path = new URL(route.request().url()).pathname; const body = path.endsWith('/assignment-candidates') ? { items: [{ accountId: toolsAccount, membershipId: toolsId(159), displayName: 'Synthetic operator' }], page: 0, size: 20, hasNext: false } : path.endsWith('/internal-history') ? { items: [], page: 0, size: 20, hasNext: false } : { ...detail(), allowedActions: ['REPLY', 'ASSIGN'] };
    return route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify(body) });
  });
  await page.goto(root + '/support?view=tickets&ticket=' + target); await page.getByText('Phân công phối hợp nội bộ', { exact: true }).click(); await expect(page.getByLabel('Người phối hợp', { exact: true }).locator('option[value="' + toolsAccount + '"]')).toBeAttached(); await page.getByLabel('Người phối hợp', { exact: true }).selectOption(toolsAccount); await expect(page.getByRole('button', { name: 'Lưu phân công', exact: true })).toBeEnabled();
});
for (const width of [390, 1440]) test(`Organization Ticket detail is readable at ${width}px with safe content`, async ({ page, context }) => {
  await businessToolsFixture(context); await context.route('**/merchant-organizations/' + organizationA + '/support-tickets/' + target, route => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify({ ...detail(), subject: 'Synthetic <script> question' }) }));
  await page.setViewportSize({ width, height: 1000 }); await page.goto(root + '/support?view=tickets&ticket=' + target); await expect(page.getByRole('heading', { name: 'Synthetic <script> question', exact: true })).toBeVisible(); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true); await expect(page.locator('.os-support script')).toHaveCount(0); await page.screenshot({ path: '../../../.demo/organization-support/ticket-' + width + '.png', fullPage: true, animations: 'disabled' });
});
