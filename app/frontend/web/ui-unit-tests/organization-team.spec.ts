import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { businessToolsFixture } from './support/business-tools-fixture';
import { businessContextsFixture, organizationA, organizationB } from './support/business-fixture';
const base = '/merchant/' + organizationA;
async function teamFixture(context: BrowserContext) {
  const api = await businessToolsFixture(context); let values = businessContextsFixture, denied = false, paused = false;
  let resume = () => {};
  await context.route('**/api/v1/customer/business-contexts', async route => {
    api.calls.push({ method: route.request().method(), path: new URL(route.request().url()).pathname, query: new URLSearchParams() });
    if (paused) await new Promise<void>(resolve => { resume = resolve; });
    await route.fulfill({ status: denied ? 503 : 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(denied ? { code: 'SERVICE_UNAVAILABLE' } : { schemaVersion: 'BUSINESS_CONTEXTS_V1', items: values }) });
  });
  return { ...api, values: (next: typeof values) => { values = next; }, deny: () => { denied = true; }, pause: () => { paused = true; }, resume: () => { paused = false; resume(); } };
}
test('Team root menu reads the current self and does not invent a directory, invites or admin grants', async ({ page, context }) => {
  const api = await teamFixture(context); await page.goto(base + '/team');
  await expect(page.getByRole('heading', { name: 'Nhân sự & Phân quyền', exact: true })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Điều hướng Business', exact: true }).getByRole('link', { name: 'Nhân sự & Phân quyền', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.getByRole('region', { name: 'Membership của tôi', exact: true })).toContainText('Chủ doanh nghiệp');
  const rights = page.getByRole('region', { name: 'Quyền của tôi', exact: true }); await expect(rights).toContainText('3 quyền');
  await expect(rights).not.toContainText('Mời thành viên'); await expect(rights).not.toContainText('Tạo và sửa bản nháp');
  await expect(page.locator('.otm-team')).toContainText('không bao gồm các quyền chỉ được cấp tại từng kho');
  await expect(page.getByRole('button', { name: /Mời|Thu hồi|Chuyển quyền|Lưu quyền/ })).toHaveCount(0);
  expect(api.calls.every(c => c.method === 'GET')).toBe(true); expect(api.calls.filter(c => /team\/|invitations|personal-analytics/.test(c.path))).toHaveLength(0);
});
test('capability search is local and clearing restores only actual grants', async ({ page, context }) => {
  const api = await teamFixture(context); await page.goto(base + '/team'); await expect(page.locator('.otm-count')).toHaveText('3 quyền'); const count = api.calls.length;
  await page.getByRole('searchbox', { name: 'Tìm quyền', exact: true }).fill('tài chính'); await expect(page.locator('.otm-capabilities li')).toHaveCount(1);
  await page.getByRole('searchbox', { name: 'Tìm quyền', exact: true }).fill('không tồn tại'); await expect(page.getByRole('status')).toContainText('Không tìm thấy quyền');
  await page.getByRole('searchbox', { name: 'Tìm quyền', exact: true }).clear(); await expect(page.locator('.otm-capabilities li')).toHaveCount(3); expect(api.calls).toHaveLength(count);
});
test('refresh clears old grants during loading and honors reduced authority', async ({ page, context }) => {
  const api = await teamFixture(context); await page.goto(base + '/team'); await expect(page.locator('.otm-count')).toHaveText('3 quyền');
  api.values([{ ...businessContextsFixture[0], authorityVersion: 2, permissions: ['dashboard.read'] }]); api.pause();
  await page.getByRole('button', { name: 'Kiểm tra lại quyền', exact: true }).click(); await expect(page.locator('.otm-capabilities')).toHaveCount(0); await expect(page.getByRole('status')).toContainText('Đang kiểm tra Membership');
  api.resume(); await expect(page.locator('.otm-count')).toHaveText('1 quyền'); await expect(page.locator('.otm-capabilities')).not.toContainText('tài chính');
});
for (const failure of ['revoked', 'unavailable', 'replaced', 'stale'] as const) test(`Team hides previous self data after ${failure}`, async ({ page, context }) => {
  const api = await teamFixture(context); await page.goto(base + '/team'); await expect(page.locator('.otm-count')).toHaveText('3 quyền');
  if (failure === 'unavailable') api.deny();
  else api.values(failure === 'revoked' ? [] : [{ ...businessContextsFixture[0], ...(failure === 'replaced' ? { membershipId: businessContextsFixture[1].membershipId } : { authorityVersion: 0 }) }]);
  await page.getByRole('button', { name: 'Kiểm tra lại quyền', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('Thông tin quyền cũ đã được ẩn');
  await expect(page.locator('.otm-member,.otm-capabilities')).toHaveCount(0); expect(api.calls.every(c => c.method === 'GET')).toBe(true);
});
test('switching Organization replaces the self Membership and capabilities', async ({ page, context }) => {
  await teamFixture(context); await page.goto(base + '/team'); await expect(page.locator('.otm-count')).toHaveText('3 quyền');
  await page.getByRole('button', { name: 'Synthetic Store A', exact: true }).click(); await page.getByRole('link', { name: /Synthetic Store B/ }).click();
  await page.getByRole('navigation', { name: 'Điều hướng Business', exact: true }).getByRole('link', { name: 'Nhân sự & Phân quyền', exact: true }).click();
  await expect(page).toHaveURL(new RegExp(organizationB + '/team$')); await expect(page.locator('.otm-count')).toHaveText('1 quyền'); await expect(page.locator('.otm-member')).toContainText('Nhân viên vận hành'); await expect(page.locator('.otm-member')).not.toContainText('Synthetic Store A');
});
for (const width of [390, 1440]) test(`Team has no overflow at ${width}px`, async ({ page, context }) => {
  await teamFixture(context); await page.setViewportSize({ width, height: 1000 }); await page.goto(base + '/team'); await expect(page.locator('.otm-count')).toHaveText('3 quyền');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/organization-team/self-' + width + '.png', fullPage: true, animations: 'disabled' });
});

async function directoryFixture(context: BrowserContext) {
  const api = await teamFixture(context); api.values([{ ...businessContextsFixture[0], permissions: ['dashboard.read', 'team.read'] }]); let denied = false;
  const row = { membershipId: businessContextsFixture[0].membershipId, displayName: 'Synthetic Team Member', role: 'OWNER', state: 'ACTIVE', authorityVersion: 2, version: 2, createdAt: '2026-11-17T00:00:00Z', updatedAt: '2026-11-17T00:00:00Z' };
  await context.route('**/merchant-organizations/' + organizationA + '/team/**', async route => {
    const request = route.request(), url = new URL(request.url()); api.calls.push({ method: request.method(), path: url.pathname, query: url.searchParams, body: request.postDataJSON() ?? undefined });
    if (denied) return route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ code: 'AUTHORITY_REQUIRED' }) });
    let value: unknown; const headers: Record<string, string> = { 'Cache-Control': 'no-store' };
    if (url.pathname.endsWith('/members/search')) { const q = request.postDataJSON(); value = { items: row.displayName.toLowerCase().includes(q.search.toLowerCase()) && (!q.state || q.state === row.state) && (!q.role || q.role === row.role) ? [row] : [], page: q.page, size: q.size, hasNext: false }; }
    else if (url.pathname.endsWith('/role-templates')) value = { items: ['OWNER', 'MANAGER', 'OPERATOR'].map(role => ({ role, version: 1, allowedPermissions: ['dashboard.read'] })) };
    else { value = { ...row, grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }] }; headers.ETag = '"v2"'; }
    return route.fulfill({ contentType: 'application/json', headers, body: JSON.stringify(value) });
  });
  return { api, deny: () => { denied = true; } };
}
test('directory lists actual server members and filters without placing names in URLs', async ({ page, context }) => {
  const { api } = await directoryFixture(context); await page.goto(base + '/team'); const directory = page.getByRole('region', { name: 'Danh sách thành viên', exact: true });
  await expect(directory).toContainText('Synthetic Team Member'); await expect(directory.getByRole('button', { name: 'Tiếp', exact: true })).toBeDisabled();
  await page.getByRole('searchbox', { name: 'Tìm thành viên', exact: true }).fill('No match'); await directory.getByRole('button', { name: 'Áp dụng', exact: true }).click(); await expect(directory.getByRole('status')).toContainText('Không có thành viên');
  const request = api.calls.filter(c => c.path.endsWith('/team/members/search')).at(-1)!; expect(request.method).toBe('POST'); expect(request.query.toString()).toBe(''); expect(request.body).toMatchObject({ search: 'No match', page: 0 });
  await directory.getByRole('button', { name: 'Đặt lại', exact: true }).click(); await expect(directory).toContainText('Synthetic Team Member');
  await directory.getByRole('button', { name: 'Xem quyền của Synthetic Team Member', exact: true }).click(); await expect(page.getByRole('region', { name: 'Quyền thành viên', exact: true })).toContainText('Xem tổng quan doanh nghiệp');
  await directory.getByRole('button', { name: 'Mẫu vai trò', exact: true }).click(); await expect(page.getByRole('region', { name: 'Mẫu vai trò', exact: true })).toContainText('không tự cấp');
  expect(api.calls.filter(c => c.method !== 'GET' && !c.path.endsWith('/team/members/search'))).toHaveLength(0);
});
test('directory hides previously loaded members after permission denial', async ({ page, context }) => {
  const fixture = await directoryFixture(context); await page.goto(base + '/team'); const directory = page.getByRole('region', { name: 'Danh sách thành viên', exact: true }); await expect(directory).toContainText('Synthetic Team Member');
  fixture.deny(); await directory.getByRole('button', { name: 'Áp dụng', exact: true }).click(); await expect(directory.getByRole('alert')).toContainText('Dữ liệu cũ đã được ẩn'); await expect(directory).not.toContainText('Synthetic Team Member');
  await expect(page.getByRole('button', { name: /Mời thành viên|Lưu quyền|Tạm khóa|Thu hồi Membership/ })).toHaveCount(0);
});
