import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import { businessToolsFixture, toolsId } from './support/business-tools-fixture';
import { businessContextsFixture, organizationA } from './support/business-fixture';
import type { BusinessPermission } from '../src/customer/business-data';
import { facilityFixture, siteId } from './support/merchant-facility-fixture';
import { teamCapabilities } from '../src/customer/organization-team';
async function fixture(context: BrowserContext, permissions: BusinessPermission[], role: 'OWNER' | 'MANAGER' = 'OWNER') {
  await businessToolsFixture(context);
  await context.route('**/api/v1/customer/business-contexts', route => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [{ ...businessContextsFixture[0], role, permissions }] }) }));
}
test('invite lost response reuses exact command and only explicit operational grants', async ({ page, context }) => {
  await fixture(context, ['dashboard.read', 'team.invite']);
  const calls: { key?: string; body: unknown }[] = []; let invitation: unknown;
  await context.route('**/team/invitations**', async route => {
    const req = route.request(), data = req.postDataJSON(), search = new URL(req.url()).pathname.endsWith('/search');
    const reply = (body: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify(body) });
    if (search) return reply({ items: invitation ? [invitation] : [], page: data.page, size: data.size, hasNext: false });
    calls.push({ key: req.headers()['idempotency-key'], body: data });
    invitation = { id: toolsId(901), maskedEmail: 's***@example.invalid', role: 'OPERATOR', grants: data.grants, state: 'PENDING', version: 1, createdAt: new Date().toISOString(), expiresAt: new Date(Date.now() + 259200000).toISOString(), deliveryState: 'QUEUED' };
    if (calls.length === 1) return route.abort('connectionfailed'); return reply(invitation, 201);
  });
  await page.goto('/merchant/' + organizationA + '/team'); const panel = page.getByRole('region', { name: 'Lời mời thành viên', exact: true });
  await panel.getByLabel('Email người nhận', { exact: true }).fill('synthetic@example.invalid'); await panel.getByLabel('Lý do quản trị', { exact: true }).fill('Synthetic controlled invite');
  await panel.getByRole('button', { name: 'Gửi lời mời', exact: true }).click(); await expect(panel.getByLabel('Email người nhận', { exact: true })).toBeDisabled();
  await panel.getByRole('button', { name: 'Gửi lại đúng yêu cầu', exact: true }).click(); await expect(panel.getByRole('status').filter({ hasText: 'Đã lưu lời mời' })).toBeVisible();
  expect(calls).toHaveLength(2); expect(calls[1]).toEqual(calls[0]); expect(calls[0].body).toMatchObject({ grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }] });
  await expect(panel).toContainText('s***@example.invalid'); await expect(panel).not.toContainText('synthetic@example.invalid');
});
test('Manager invitation cannot choose Manager or finance and never sees membership admin actions', async ({ page, context }) => {
  await fixture(context, ['dashboard.read', 'team.invite', 'shipment.draft.read'], 'MANAGER');
  await context.route('**/team/invitations/search', route => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify({ items: [], page: 0, size: 20, hasNext: false }) }));
  await page.goto('/merchant/' + organizationA + '/team'); const panel = page.getByRole('region', { name: 'Lời mời thành viên', exact: true });
  await expect(panel.getByRole('combobox', { name: 'Vai trò được mời', exact: true }).locator('option')).toHaveCount(1); await panel.locator('summary').click();
  await expect(panel.getByRole('checkbox', { name: /tài chính|Team/i })).toHaveCount(0); await expect(page.getByRole('button', { name: 'Tạm khóa Membership', exact: true })).toHaveCount(0);
});
test('sensitive lost response keeps consumed proof and old ETag without repeating password verification', async ({ page, context }) => {
  await fixture(context, ['dashboard.read', 'team.read', 'team.membership.manage']);
  const member = { membershipId: toolsId(902), displayName: 'Synthetic Operator', role: 'OPERATOR', state: 'ACTIVE', authorityVersion: 1, version: 1, createdAt: '2026-11-17T00:00:00Z', updatedAt: '2026-11-17T00:00:00Z' };
  const grants = [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }]; let reauth = 0; const writes: { body: unknown; key?: string; etag?: string }[] = [];
  await context.route('**/api/v1/customer-auth/reauthentication', route => { reauth++; return route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify({ proofId: toolsId(903), expiresAt: new Date(Date.now() + 900000).toISOString() }) }); });
  await context.route('**/team/**', async route => {
    const req = route.request(), path = new URL(req.url()).pathname;
    if (path.endsWith('/suspend')) { writes.push({ body: req.postDataJSON(), key: req.headers()['idempotency-key'], etag: req.headers()['if-match'] }); member.state = 'SUSPENDED'; member.version = 2; member.authorityVersion = 2; if (writes.length === 1) return route.abort('connectionfailed'); }
    const body = path.endsWith('/search') ? { items: [member], page: 0, size: 20, hasNext: false } : { ...member, grants };
    return route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v' + member.version + '"' }, body: JSON.stringify(body) });
  });
  await page.goto('/merchant/' + organizationA + '/team'); await page.getByRole('button', { name: 'Xem quyền của Synthetic Operator', exact: true }).click();
  await page.getByRole('button', { name: 'Tạm khóa Membership', exact: true }).click(); await page.getByLabel('Lý do thay đổi', { exact: true }).fill('Synthetic offboarding'); await page.getByLabel('Mật khẩu hiện tại', { exact: true }).fill('Synthetic-test-only-password-2026');
  await page.getByRole('button', { name: 'Xác nhận thay đổi', exact: true }).click(); await page.getByRole('button', { name: 'Kiểm tra lại đúng yêu cầu', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Mở lại Membership', exact: true })).toBeVisible(); expect(reauth).toBe(1); expect(writes).toHaveLength(2); expect(writes[1]).toEqual(writes[0]); expect(writes[0].etag).toBe('"v1"');
  expect(JSON.stringify(writes)).not.toContain('Synthetic-test-only-password-2026');
});
test('audit-only grant displays current history without granting directory or administration', async ({ page, context }) => {
  await fixture(context, ['dashboard.read', 'team.audit.read']); let denied = false;
  await context.route('**/team/audit/search', route => {
    const q = route.request().postDataJSON();
    return route.fulfill({ status: denied ? 403 : 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(denied ? { code: 'AUTHORITY_REQUIRED' } : { items: q.operation ? [] : [{ id: toolsId(910), actorMembershipId: toolsId(911), targetMembershipId: null, operation: 'INVITE', reason: 'Synthetic audited invitation', occurredAt: new Date(Date.parse(q.from) + 3600000).toISOString() }], page: q.page, size: q.size, hasNext: false }) });
  });
  await page.goto('/merchant/' + organizationA + '/team'); const panel = page.getByRole('region', { name: 'Lịch sử quản trị nhân sự', exact: true }); await expect(panel).toContainText('Synthetic audited invitation');
  await expect(page.getByRole('region', { name: 'Danh sách thành viên', exact: true })).toHaveCount(0); await expect(page.getByRole('region', { name: 'Lời mời thành viên', exact: true })).toHaveCount(0);
  denied = true; await panel.getByRole('button', { name: 'Lọc lịch sử', exact: true }).click(); await expect(panel.getByRole('alert')).toContainText('quyền xem đã thay đổi'); await expect(panel).not.toContainText('Synthetic audited invitation');
});
test('Warehouse invitation uses only current self scopes and validates read dependencies without broadening', async ({ page, context }) => {
  await fixture(context, ['dashboard.read', 'team.read', 'team.invite'], 'MANAGER'); const writes: unknown[] = [];
  await context.route('**/facilities?**', route => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify({ items: [facilityFixture()], page: 0, size: 20, hasNext: false }) }));
  await context.route('**/team/members/**', route => {
    const body = new URL(route.request().url()).pathname.endsWith('/search') ? { items: [], page: 0, size: 20, hasNext: false } : { membershipId: businessContextsFixture[0].membershipId, displayName: 'Synthetic Manager', role: 'MANAGER', state: 'ACTIVE', authorityVersion: 1, version: 1, createdAt: '2026-11-17T00:00:00Z', updatedAt: '2026-11-17T00:00:00Z', grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }, ...['facility.read', 'shipment.draft.read', 'shipment.draft.write'].map(permission => ({ permission, scope: 'FACILITY', facilityId: siteId }))] };
    return route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify(body) });
  });
  await context.route('**/team/invitations**', route => {
    const data = route.request().postDataJSON(), search = new URL(route.request().url()).pathname.endsWith('/search');
    if (!search) writes.push(data);
    return route.fulfill({ status: search ? 200 : 201, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v1"' }, body: JSON.stringify(search ? { items: [], page: data.page, size: data.size, hasNext: false } : { id: toolsId(920), maskedEmail: 's***@example.invalid', role: 'OPERATOR', grants: data.grants, state: 'PENDING', version: 1, createdAt: new Date().toISOString(), expiresAt: new Date(Date.now() + 259200000).toISOString(), deliveryState: 'QUEUED' }) });
  });
  await page.goto('/merchant/' + organizationA + '/team'); const panel = page.getByRole('region', { name: 'Lời mời thành viên', exact: true });
  await panel.getByRole('combobox', { name: 'Phạm vi quyền vận hành', exact: true }).selectOption('FACILITY'); await panel.getByRole('combobox', { name: 'Kho giới hạn quyền', exact: true }).selectOption(siteId);
  await panel.locator('summary').click(); await panel.getByRole('checkbox', { name: teamCapabilities['shipment.draft.write'].label + ' · Kho đã chọn', exact: true }).check();
  await panel.getByLabel('Email người nhận', { exact: true }).fill('synthetic@example.invalid'); await panel.getByLabel('Lý do quản trị', { exact: true }).fill('Synthetic warehouse scope');
  await panel.getByRole('button', { name: 'Gửi lời mời', exact: true }).click(); await expect(panel.getByRole('alert')).toContainText('Cần chọn quyền đọc cùng phạm vi'); expect(writes).toHaveLength(0);
  await panel.getByRole('checkbox', { name: teamCapabilities['shipment.draft.read'].label + ' · Kho đã chọn', exact: true }).check(); await panel.getByRole('button', { name: 'Gửi lời mời', exact: true }).click();
  await expect(panel.getByRole('status').filter({ hasText: 'Đã lưu lời mời' })).toBeVisible(); expect(writes).toHaveLength(1);
  expect(writes[0]).toMatchObject({ grants: [{ permission: 'dashboard.read', scope: 'ORGANIZATION', facilityId: null }, { permission: 'shipment.draft.write', scope: 'FACILITY', facilityId: siteId }, { permission: 'shipment.draft.read', scope: 'FACILITY', facilityId: siteId }] });
});
