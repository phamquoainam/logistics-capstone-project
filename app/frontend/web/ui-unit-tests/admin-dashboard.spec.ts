import { test, expect } from '@playwright/test';
import type { BrowserContext } from '@playwright/test';
import type { Dashboard, MetricKey, Query } from '../src/admin/data';
import { daysBefore, todayVietnam } from '../src/admin/data';
import { adminContext, adminPermissions, adminReport, adminRows, adminUser, adminOversight } from './support/admin-data';

const origin = 'https://admin.example.invalid';
test.afterEach(async ({ page }) => { if (!page.isClosed()) await page.waitForLoadState('networkidle'); });
async function governanceFixture(context: BrowserContext, lost = false) {
  await fixture(context);
  const app = '40000000-0000-4000-8000-000000000001', policy = '30000000-0000-4000-8000-000000000001', realm = '20000000-0000-4000-8000-000000000001';
  const now = new Date().toISOString(), writes: { body: string; key: string; version: string }[] = [];
  let current = { id: app, state: 'SUBMITTED', revision: 1, version: 3, legalNameMasked: 'S***', legalIdentifierMasked: '***1234', policyId: policy, reviewerId: adminUser.id, firstSubmittedAt: now, updatedAt: now, activation: null, allowedActions: ['BEGIN'] };
  const receipt = { ...current, state: 'UNDER_REVIEW', version: 4, allowedActions: ['REQUEST_INFORMATION', 'READY_FOR_DECISION'] };
  await context.route(origin + '/api/v1/admin/merchant**', async route => {
    const url = new URL(route.request().url()), reply = (v: unknown) => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(v) });
    if (url.pathname.endsWith('/context')) return reply({ accountId: adminUser.id, realmId: realm, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['application.read', 'application.review', 'application.evidence.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] });
    if (url.pathname.endsWith('/revisions')) return reply({ items: [{ applicationId: app, revision: 1, submittedAt: now, policyVersion: '7', organizationDisplayName: 'Synthetic business', digest: 'a'.repeat(64), evidenceIds: [realm, policy], contactVerifiedAt: now }], page: 0, size: 20, hasNext: false });
    if (url.pathname.endsWith('/review-data')) return reply({ application: current, submission: { entityType: 'COMPANY', jurisdiction: 'SYNTHETIC', registeredName: 'Synthetic business', legalIdentifier: 'SYNTHETIC-1234', registeredAddress: { country: 'TEST', administrativeArea: 'SIM-A1', locality: 'Synthetic locality', addressLine: 'Synthetic registered address' }, operatingAddress: { country: 'TEST', administrativeArea: 'SIM-A1', locality: 'Synthetic locality', addressLine: 'Synthetic operating address' }, representativeName: 'Synthetic contact', representativeEmail: 'synthetic@example.invalid', representativeRelationship: 'Representative', businessContactAttestationId: realm, evidenceIds: [realm, policy], declarations: { accuracy: true, representation: true, consentVersion: 'consent-test-1' } }, evidence: [realm, policy].map((id, i) => ({ id, applicationId: app, purpose: i ? 'REPRESENTATIVE_AUTHORIZATION' : 'REGISTRATION', state: 'CLEAN', mime: 'image/png', bytes: 100, sha256: 'a'.repeat(64), version: 1, screenedAt: now })), contactVerifiedAt: now, policyVersion: '7', reviewEvents: [], organizationDisplayName: 'Synthetic business' });
    if (route.request().method() === 'POST') {
      const headers = route.request().headers(), body = route.request().postData()!;writes.push({ body, key: headers['idempotency-key'], version: headers['if-match'] });
      if (lost && writes.length === 1) { current = { ...current, state: 'NEEDS_INFORMATION', version: 5, allowedActions: [] };return route.abort('failed'); }
      if (lost) return reply(receipt);
      const input = JSON.parse(body);current = { ...current, state: 'UNDER_REVIEW', version: input.action === 'BEGIN' ? 4 : 5, allowedActions: input.action === 'BEGIN' ? ['REQUEST_INFORMATION', 'READY_FOR_DECISION'] : ['REQUEST_INFORMATION'] };return reply(current);
    }
    if (url.pathname.endsWith('/merchant-applications')) return reply({ items: [current], page: 0, size: 20, hasNext: false });
    return reply(current);
  });
  return writes;
}

test('overview work center separates approvals profile and own authority with no unsupported security writes', async ({ page, context }) => {
  await fixture(context);
  const policy = '30000000-0000-4000-8000-000000000001', app = '40000000-0000-4000-8000-000000000001', now = new Date().toISOString();
  let auditOnly = false;
  await context.route(origin + '/api/v1/admin/merchant-governance/context', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ accountId: adminUser.id, realmId: policy, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: auditOnly ? ['audit.read'] : ['application.read', 'task.read', 'audit.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] }) }));
  const kinds: string[] = [];let denied = false;
  await context.route(origin + '/api/v1/admin/work-items**', route => { kinds.push(new URL(route.request().url()).searchParams.get('kind') ?? '');return route.fulfill({ status: denied ? 403 : 200, contentType: 'application/json', body: JSON.stringify(denied ? { code: 'ACCESS_DENIED' } : { items: [{ id: policy, kind: 'MERCHANT_DECISION', applicationId: app, revision: 1, assignmentVersion: 1, createdAt: now, dueAt: null, targetPath: '/admin/merchants/applications?applicationId=' + app, allowedActions: ['APPROVE', 'REJECT'] }], page: 0, size: 20, hasNext: false }) }); });
  await page.goto(origin + '/admin/account?tab=approvals');await expect(page.getByRole('link', { name: 'My Approvals', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(page.locator('tbody')).toContainText(app);expect(kinds).toEqual(['MERCHANT_DECISION']);
  await page.getByRole('tab', { name: 'Notifications', exact: true }).click();await expect(page.getByRole('heading', { name: 'Thông báo Admin chưa có nguồn', exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'Profile', exact: true }).click();await expect(page.getByRole('heading', { name: 'Tài khoản & Quyền hiện hành', exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'MFA & Sessions', exact: true }).click();await expect(page.getByRole('heading', { name: 'MFA & danh sách phiên', exact: true })).toBeVisible();await expect(page.getByRole('button', { name: 'Đăng xuất tất cả' })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Approvals', exact: true }).click();denied = true;await page.getByRole('button', { name: 'Làm mới công việc', exact: true }).click();await expect(page.getByRole('alert')).toContainText('chưa có quyền');await expect(page.locator('tbody tr')).toHaveCount(0);
  await page.goto(origin + '/admin/governance/authority');await expect(page.getByRole('heading', { name: 'Authority Matrix', exact: true })).toBeVisible();await expect(page.locator('tbody')).toContainText('task.read');await expect(page.getByRole('button', { name: /Cấp quyền|Phát hành policy/ })).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-overview-authority-mobile.png', fullPage: true, animations: 'disabled' });
  await expect(page.getByRole('link', { name: 'Xem quyết định đang chờ của tôi', exact: true })).toBeVisible();
  auditOnly = true;await page.getByRole('button', { name: 'Đối chiếu quyền hiện hành', exact: true }).click();
  await expect(page.locator('tbody')).toContainText('audit.read');await expect(page.locator('tbody')).not.toContainText('task.read');
  await expect(page.getByRole('link', { name: 'Xem quyết định đang chờ của tôi', exact: true })).toHaveCount(0);
});

test('Merchant review uses real policy metadata, explicit disclosure and an unchecked checklist', async ({ page, context }) => {
  const writes = await governanceFixture(context);await page.goto(origin + '/admin/merchants/applications');
  await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic beginning');
  await page.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true }).click();
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 4', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true })).toBeDisabled();
  await page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Synthetic business', exact: true })).toBeVisible();
  for (const name of ['Hồ sơ đăng ký hợp lệ', 'Thẩm quyền đại diện hợp lệ', 'Liên hệ đã xác minh', 'Đã đối chiếu trùng lặp', 'Không có xung đột đã biết']) await page.getByRole('checkbox', { name, exact: true }).check();
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic complete review');
  await page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true }).click();
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 5', { exact: true })).toBeVisible();
  expect(writes).toHaveLength(2);expect(JSON.parse(writes[0].body).policyVersion).toBe('7');expect(JSON.parse(writes[1].body).checklist).toEqual({ registrationValid: true, representationValid: true, contactValid: true, duplicatesClear: true, conflictsClear: true });
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-governance-review.png', fullPage: true });
});

test('Merchant review deep link refresh does not reopen the active application after BEGIN', async ({ page, context }) => {
  const writes = await governanceFixture(context);
  const app = '40000000-0000-4000-8000-000000000001';
  const details: string[] = [];
  context.on('request', request => { if (new URL(request.url()).pathname === '/api/v1/admin/merchant-applications/' + app && request.method() === 'GET') details.push(request.url()); });
  await page.goto(origin + '/admin/merchants/applications?applicationId=' + app);
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic deep link review');
  await page.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true }).click();
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 4', { exact: true })).toBeVisible();
  await page.waitForLoadState('networkidle');
  expect(details).toHaveLength(2); // Initial deep link + canonical post-command refresh only.
  await page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
  for (const name of ['Hồ sơ đăng ký hợp lệ', 'Thẩm quyền đại diện hợp lệ', 'Liên hệ đã xác minh', 'Đã đối chiếu trùng lặp', 'Không có xung đột đã biết']) await page.getByRole('checkbox', { name, exact: true }).check();
  await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic review completed');
  await page.getByRole('button', { name: 'Hoàn tất thẩm định', exact: true }).click();
  await expect(page.getByText('Đang thẩm định · Revision 1 · Phiên bản 5', { exact: true })).toBeVisible();
  expect(writes).toHaveLength(2);
});

test('Merchant review clears disclosed data when current governance admission is revoked', async ({ page, context }) => {
  await governanceFixture(context);
  await page.goto(origin + '/admin/merchants/applications?applicationId=40000000-0000-4000-8000-000000000001');
  await page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
  await expect(page.getByText('synthetic@example.invalid', { exact: true })).toBeVisible();
  await context.route(origin + '/api/v1/admin/merchant-governance/context', route => route.fulfill({ status: 403, contentType: 'application/json', body: JSON.stringify({ code: 'ACCESS_DENIED' }) }));
  await page.getByRole('button', { name: 'Làm mới', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('chưa có quyền');
  await expect(page.getByText('synthetic@example.invalid', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true })).toHaveCount(0);
});

test('Merchant review lost response keeps the original key/ETag and rereads canonical state after receipt replay', async ({ page, context }) => {
  const writes = await governanceFixture(context, true);await page.goto(origin + '/admin/merchants/applications');
  await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();await page.getByRole('textbox', { name: 'Lý do', exact: true }).fill('Synthetic lost response');await page.getByRole('button', { name: 'Bắt đầu thẩm định', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định được kết quả');expect(writes).toHaveLength(1);
  await page.getByRole('button', { name: 'Thử lại đúng yêu cầu', exact: true }).click();
  await expect(page.getByText('Cần bổ sung · Revision 1 · Phiên bản 5', { exact: true })).toBeVisible();
  expect(writes).toHaveLength(2);expect(writes[0]).toEqual(writes[1]);
});

test('Menu 2 exposes all five entries, protected review details and immutable revision metadata without owner-transfer writes', async ({ page, context }) => {
  const writes = await governanceFixture(context);
  const app = '40000000-0000-4000-8000-000000000001', policy = '30000000-0000-4000-8000-000000000001';
  await context.route(origin + '/api/v1/admin/merchant-governance/context', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ accountId: adminUser.id, realmId: policy, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['application.read', 'application.review', 'application.evidence.read', 'org.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] }) }));
  await page.goto(origin + '/admin/merchants/applications?view=review');
  const menu = page.getByRole('navigation', { name: 'Merchant & Phê duyệt', exact: true });
  await expect(menu.getByRole('link')).toHaveCount(5);
  await expect(menu.getByRole('link', { name: 'Review Workspace', exact: true })).toHaveAttribute('aria-current', 'page');
  await page.getByRole('combobox', { name: 'Policy scope', exact: true }).selectOption(policy);
  await page.getByLabel('Được phân công cho tôi', { exact: true }).check();
  const filtered = page.waitForRequest(request => { const url = new URL(request.url());return url.pathname === '/api/v1/admin/merchant-applications' && url.searchParams.get('policyId') === policy && url.searchParams.get('reviewerId') === adminUser.id && url.searchParams.get('page') === '0'; });
  await page.getByRole('button', { name: 'Làm mới', exact: true }).click();await filtered;
  await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();
  await expect(page.getByText('synthetic@example.invalid', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Mở dữ liệu thẩm định', exact: true }).click();
  await expect(page.getByText('synthetic@example.invalid', { exact: true })).toBeVisible();
  await expect(page.getByText('Địa chỉ đăng ký', { exact: true })).toBeVisible();
  await expect(page.getByText('Synthetic registered address, Synthetic locality, SIM-A1, TEST', { exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Consent đã nộp', exact: true })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Consent đã nộp', exact: true })).toContainText('consent-test-1');
  await page.getByRole('button', { name: 'Xem lịch sử revision', exact: true }).click();
  await expect(page.getByRole('region', { name: 'Revision history' }).getByRole('cell', { name: 'Synthetic business', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Revision sau', exact: true })).toBeDisabled();
  await menu.getByRole('link', { name: 'Owner Transfer', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Owner Transfer', exact: true })).toBeVisible();
  await expect(page.getByText(/BLOCKED_COORDINATION · Policy nghiệp vụ đã được duyệt; chưa mở thực thi chuyển Owner/)).toBeVisible();
  await expect(page.getByRole('button', { name: /Chuyển Owner|Phê duyệt chuyển/ })).toHaveCount(0);
  expect(writes).toHaveLength(0);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-menu2-mobile.png', fullPage: true, animations: 'disabled' });
});

test('independent decider reauthenticates once and retries the exact rejection without retaining a password', async ({ page, context }) => {
  await fixture(context);
  const app = '40000000-0000-4000-8000-000000000003', policy = '30000000-0000-4000-8000-000000000001', realm = '20000000-0000-4000-8000-000000000001', proof = '50000000-0000-4000-8000-000000000001';
  const now = new Date().toISOString(), decisions: { body: string; key: string; etag: string }[] = []; let proofs = 0;
  let current = { id: app, state: 'UNDER_REVIEW', revision: 2, version: 10, legalNameMasked: 'D***', legalIdentifierMasked: '***4321', policyId: policy, reviewerId: realm, firstSubmittedAt: now, updatedAt: now, activation: null, allowedActions: ['REJECT'] };
  await context.route(origin + '/api/v1/admin/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const reply = (v: unknown) => route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(v) });
    if (path === '/api/v1/admin/governance-reauthentications') { proofs++; const body = route.request().postDataJSON(); expect(body.operation).toBe('REJECT'); expect(body.targetId).toBe(app); expect(body.expectedVersion).toBe(10); expect(body.commandDigest).toMatch(/^[a-f0-9]{64}$/); return reply({ proofId: proof, expiresAt: new Date(Date.now() + 899000).toISOString() }); }
    if (path.endsWith('/context') && path.includes('merchant-governance')) return reply({ accountId: adminUser.id, realmId: realm, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['application.read', 'application.decide', 'audit.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] });
    if (path.endsWith('/revisions')) return reply({ items: [{ applicationId: app, revision: 2, submittedAt: now, policyVersion: '7', organizationDisplayName: 'Synthetic decision business', digest: 'a'.repeat(64), evidenceIds: [realm, policy], contactVerifiedAt: now }], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/events')) return reply({ items: [{ id: proof, actorId: realm, operation: 'READY_FOR_DECISION', targetType: 'APPLICATION', targetId: app, priorVersion: 9, newVersion: 10, revision: 2, policyVersion: '7', reasonCode: 'REVIEW_COMPLETED', occurredAt: now, requestId: policy }], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/rejection')) { const headers = route.request().headers(); decisions.push({ body: route.request().postData()!, key: headers['idempotency-key'], etag: headers['if-match'] }); current = { ...current, state: 'REJECTED', version: 11, allowedActions: [] }; if (decisions.length === 1) return route.abort('failed'); return reply(current); }
    if (path.endsWith('/merchant-applications')) return reply({ items: [current], page: 0, size: 20, hasNext: false });
    if (path.endsWith(app)) return reply(current);
    return route.fallback();
  });
  await page.goto(origin + '/admin/merchants/applications'); await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Quyết định độc lập' })).toBeVisible();
  await page.getByRole('button', { name: 'Đọc lịch sử hồ sơ', exact: true }).click(); await expect(page.getByRole('cell', { name: 'REVIEW_COMPLETED', exact: true })).toBeVisible();
  await page.getByLabel('Lý do từ chối', { exact: true }).fill('Synthetic independent reason'); await page.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill('synthetic-stepup-password'); await page.getByRole('button', { name: 'Xác thực & từ chối hồ sơ' }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định kết quả'); await expect(page.getByLabel('Mật khẩu xác thực lại', { exact: true })).toHaveCount(0); expect(proofs).toBe(1); expect(decisions).toHaveLength(1);
  await page.getByRole('button', { name: 'Thử lại đúng quyết định' }).click(); await expect(page.getByRole('heading', { name: 'Quyết định độc lập' })).toHaveCount(0);
  expect(proofs).toBe(1); expect(decisions).toHaveLength(2); expect(decisions[0]).toEqual(decisions[1]); expect(JSON.parse(decisions[0].body)).toEqual({ revision: 2, policyVersion: '7', reauthenticationProofId: proof, reason: 'Synthetic independent reason' });
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
test('independent approval preserves one stepup and exact command after lost response', async ({ page, context }) => {
  await fixture(context);
  const app = '40000000-0000-4000-8000-000000000003', policy = '30000000-0000-4000-8000-000000000001', realm = '20000000-0000-4000-8000-000000000001', proof = '50000000-0000-4000-8000-000000000001';
  const now = new Date().toISOString(), decisions: { body: string; key: string; etag: string }[] = []; let proofs = 0;
  let current: Record<string, unknown> = { id: app, state: 'UNDER_REVIEW', revision: 2, version: 10, legalNameMasked: 'D***', legalIdentifierMasked: '***4321', policyId: policy, reviewerId: realm, firstSubmittedAt: now, updatedAt: now, activation: null, allowedActions: ['REJECT', 'APPROVE'] };
  await context.route(origin + '/api/v1/admin/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const reply = (v: unknown) => route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(v) });
    if (path === '/api/v1/admin/governance-reauthentications') { proofs++; const body = route.request().postDataJSON(); expect(body.operation).toBe('APPROVE'); expect(body.targetId).toBe(app); expect(body.expectedVersion).toBe(10); expect(body.commandDigest).toMatch(/^[a-f0-9]{64}$/); return reply({ proofId: proof, expiresAt: new Date(Date.now() + 899000).toISOString() }); }
    if (path.endsWith('/context') && path.includes('merchant-governance')) return reply({ accountId: adminUser.id, realmId: realm, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['application.read', 'application.decide', 'audit.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] });
    if (path.endsWith('/revisions')) return reply({ items: [{ applicationId: app, revision: 2, submittedAt: now, policyVersion: '7', organizationDisplayName: 'Synthetic decision business', digest: 'a'.repeat(64), evidenceIds: [realm, policy], contactVerifiedAt: now }], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/events')) return reply({ items: [{ id: proof, actorId: realm, operation: 'READY_FOR_DECISION', targetType: 'APPLICATION', targetId: app, priorVersion: 9, newVersion: 10, revision: 2, policyVersion: '7', reasonCode: 'REVIEW_COMPLETED', occurredAt: now, requestId: policy }], page: 0, size: 20, hasNext: false });
    if (path.endsWith('/approval')) { const headers = route.request().headers(); decisions.push({ body: route.request().postData()!, key: headers['idempotency-key'], etag: headers['if-match'] }); current = { ...current, state: 'APPROVED', version: 11, activation: { applicationId: app, revision: 2, decisionEventId: proof, organizationId: realm, ownerMembershipId: policy, state: 'ACTIVE', activatedAt: now }, allowedActions: [] }; if (decisions.length === 1) return route.abort('failed'); return reply(current); }
    if (path.endsWith('/merchant-applications')) return reply({ items: [current], page: 0, size: 20, hasNext: false });
    if (path.endsWith(app)) return reply(current);
    return route.fallback();
  });
  await page.goto(origin + '/admin/merchants/applications'); await page.getByRole('button', { name: 'Mở hồ sơ', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Quyết định độc lập' })).toBeVisible();
  await page.getByRole('button', { name: 'Đọc lịch sử hồ sơ', exact: true }).click(); await expect(page.getByRole('cell', { name: 'REVIEW_COMPLETED', exact: true })).toBeVisible();
  await page.getByLabel('Quyết định', { exact: true }).selectOption('APPROVE');
  await page.getByLabel('Lý do phê duyệt', { exact: true }).fill('Synthetic independent reason'); await page.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill('synthetic-stepup-password'); await page.getByRole('button', { name: 'Xác thực & phê duyệt hồ sơ' }).click();
  await expect(page.getByRole('alert')).toContainText('Chưa xác định kết quả'); await expect(page.getByLabel('Mật khẩu xác thực lại', { exact: true })).toHaveCount(0); expect(proofs).toBe(1); expect(decisions).toHaveLength(1);
  await page.getByRole('button', { name: 'Thử lại đúng quyết định' }).click(); await expect(page.getByRole('heading', { name: 'Quyết định độc lập' })).toHaveCount(0);
  expect(proofs).toBe(1); expect(decisions).toHaveLength(2); expect(decisions[0]).toEqual(decisions[1]); expect(JSON.parse(decisions[0].body)).toEqual({ revision: 2, policyVersion: '7', reauthenticationProofId: proof, reason: 'Synthetic independent reason' });
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
test('organization lifecycle keeps exact key on lost response and refreshes current state', async ({ page, context }) => {
  await fixture(context);const org = '60000000-0000-4000-8000-000000000001', member = '60000000-0000-4000-8000-000000000002', proof = '60000000-0000-4000-8000-000000000003', application = '60000000-0000-4000-8000-000000000004';
  let current = { id: org, displayName: 'Synthetic directory company', state: 'ACTIVE', admissionOrigin: 'APPLICATION_APPROVED', version: 1, initialOwnerMembershipId: member, activation: { applicationId: application, revision: 1, decisionEventId: proof, organizationId: org, ownerMembershipId: member, state: 'ACTIVE', activatedAt: new Date().toISOString() }, membershipCounts: { OWNER: { ACTIVE: 1 } }, allowedActions: ['SUSPEND'] };
  const requests: { body: string; key: string; etag: string }[] = [];let proofs = 0;
  await context.route(origin + '/api/v1/admin/**', async route => {
    const path = new URL(route.request().url()).pathname;const reply = (value: unknown) => route.fulfill({ status: 200, contentType: 'application/json', headers: { 'Cache-Control': 'no-store' }, body: JSON.stringify(value) });
    if (path === '/api/v1/admin/merchant-organizations') return reply({ items: [current], page: 0, size: 20, hasNext: false });
    if (path === '/api/v1/admin/merchant-organizations/' + org) return reply(current);
    if (path === '/api/v1/admin/governance-reauthentications') { proofs++;expect(route.request().postDataJSON().operation).toBe('SUSPEND');return reply({ proofId: proof, expiresAt: new Date(Date.now() + 899000).toISOString() }); }
    if (path.endsWith('/suspension')) { const headers = route.request().headers();requests.push({ body: route.request().postData()!, key: headers['idempotency-key'], etag: headers['if-match'] });current = { ...current, state: 'SUSPENDED', version: 2, allowedActions: ['REINSTATE'] };if (requests.length === 1) return route.abort('failed');return reply(current); }
    return route.fallback();
  });
  await page.goto(origin + '/admin/merchants/organizations');await page.getByRole('button', { name: 'Mở tổ chức', exact: true }).click();
  await page.getByLabel('Lý do thay đổi', { exact: true }).fill('Synthetic directory action');await page.getByLabel('Mật khẩu xác thực lại', { exact: true }).fill('synthetic-stepup-password');
  await page.getByRole('button', { name: 'Xác thực & tạm ngừng', exact: true }).click();await expect(page.getByRole('alert')).toContainText('Chưa xác định kết quả');
  await expect(page.getByRole('button', { name: 'Về directory', exact: true })).toBeDisabled();await page.getByRole('button', { name: 'Thử lại đúng yêu cầu', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Khôi phục tổ chức', exact: true })).toBeVisible();expect(proofs).toBe(1);expect(requests).toHaveLength(2);expect(requests[0]).toEqual(requests[1]);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('Merchant directory owns exactly one primary heading after its canonical list loads', async ({ page, context }) => {
  await fixture(context);
  await context.route(origin + '/api/v1/admin/merchant-organizations**', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ items: [], page: 0, size: 20, hasNext: false }) }));
  await page.goto(origin + '/admin/merchants/organizations');
  await expect(page.getByText('Chưa có tổ chức thuộc bộ lọc và phạm vi hiện tại.', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toHaveCount(1);
  await expect(page.getByRole('heading', { name: 'Tổ chức Merchant', exact: true })).toBeVisible();
});

test('Organization activation deep link binds exact target and handles denied access without exposing prior details', async ({ page, context }) => {
  await fixture(context);
  const id = '60000000-0000-4000-8000-000000000001', owner = '60000000-0000-4000-8000-000000000002';
  let denied = false;
  await context.route(origin + '/api/v1/admin/merchant-organizations**', route => {
    const path = new URL(route.request().url()).pathname;
    const value = { id, displayName: 'Synthetic target organization', state: 'ACTIVE', admissionOrigin: 'LOCAL_OPERATOR_PROVISIONED', version: 1, initialOwnerMembershipId: owner, activation: null, membershipCounts: { OWNER: { ACTIVE: 1 } }, allowedActions: [] };
    return route.fulfill({ status: denied && path.endsWith(id) ? 403 : 200, contentType: 'application/json', body: JSON.stringify(path.endsWith(id) ? denied ? { code: 'ACCESS_DENIED' } : value : { items: [], page: 0, size: 20, hasNext: false }) });
  });
  await page.goto(origin + '/admin/merchants/organizations?organizationId=' + id);
  await expect(page.getByRole('heading', { name: 'Synthetic target organization', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Xác thực & tạm ngừng' })).toHaveCount(0);
  denied = true;await page.reload();
  await expect(page.getByRole('alert')).toContainText('không có quyền');
  await expect(page.getByRole('heading', { name: 'Synthetic target organization', exact: true })).toHaveCount(0);
});
async function fixture(context: BrowserContext, limited = false, expired = false, stale = false) {
  const calls: string[] = [], ctx = adminContext(limited ? ['ADMIN_DASHBOARD_READ', 'ADMIN_SHIPMENT_READ'] : adminPermissions);
  let report: Dashboard;
  await context.route(origin + '/**', async route => {
    const url = new URL(route.request().url());
    if (!url.pathname.startsWith('/api/')) return route.fulfill({ response: await context.request.get('http://127.0.0.1:5181' + url.pathname) });
    calls.push(url.pathname + url.search);
    const reply = (value: unknown, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(value) });
    if (url.pathname.endsWith('/csrf')) return reply({ csrfToken: 'synthetic-admin-csrf-challenge' });
    if (url.pathname.endsWith('/refresh')) return reply({ accessToken: 'a'.repeat(128), tokenType: 'Bearer', expiresIn: 900, sessionExpiresAt: new Date(Date.now() + 28800000).toISOString(), user: adminUser });
    if (url.pathname.endsWith('/me')) return reply(adminUser);
    if (url.pathname.endsWith('/context')) return reply(ctx);
    if (url.pathname === '/api/v1/admin/dashboard') {
      const p = url.searchParams, q: Query = { from: p.get('from')!, to: p.get('to')!, grain: p.get('grain') as Query['grain'], shipmentStatus: p.get('shipmentStatus') as Query['shipmentStatus'], regionId: null };
      report = adminReport(q, ctx); if (stale) report.generatedAt = new Date(Date.now() - 901000).toISOString(); return reply(report);
    }
    if (url.pathname.endsWith('/shipments')) return expired ? reply({ code: 'SNAPSHOT_EXPIRED' }, 409) : reply(adminRows(report, url.searchParams.get('metric') as MetricKey));
    if (/^\/api\/v1\/admin\/shipments\/[a-f0-9-]{36}$/.test(url.pathname)) return expired ? reply({ code: 'SNAPSHOT_EXPIRED' }, 409) : reply(adminOversight(report, adminRows(report, url.searchParams.get('metric') as MetricKey).items[0]));
    if (url.pathname.endsWith('/platform')) return reply({ checkedAt: new Date().toISOString(), apiAvailability: 'UP', queues: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] }, scheduledJobs: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] }, integrationFailures: { availability: 'SOURCE_NOT_IMPLEMENTED', metrics: [] } });
    return reply({ code: 'ACCESS_DENIED' }, 403);
  });
  return calls;
}
test('KPI drill-through and back retain cohort without redundant dashboard requests', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByRole('button', { name: 'Chờ bàn giao: 2 vận đơn' }).click();
  await expect(page.getByRole('table')).toBeVisible();
  const request = calls.find(v => v.startsWith('/api/v1/admin/dashboard/shipments'))!;
  expect(request).toContain('snapshotId='); expect(request).toContain('metric=WAITING_HANDOVER'); expect(request).toContain('grain=DAY');
  await page.getByRole('button', { name: 'Về tổng quan' }).click();
  await expect(page.getByRole('img', { name: 'Vận đơn đã đặt theo thời gian Booking' })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
});
test('filters apply once and protected financial and technical widgets are absent for limited roles', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await expect(page.getByText('Cước chưa thanh toán', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Tình trạng API quản trị', { exact: true })).toHaveCount(0);
  expect(calls.some(v => v.includes('/platform'))).toBe(false);
  await page.getByRole('button', { name: '7 ngày', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(2);
});
test('expired selector blocks rows and requires explicit refresh instead of silently changing cohort', async ({ page, context }) => {
  const calls = await fixture(context, false, true); await page.goto(origin + '/admin');
  await page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' }).click();
  await expect(page.getByRole('alert')).toContainText('Số liệu đã thay đổi');
  await expect(page.getByRole('table')).toHaveCount(0);
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard/shipments'))).toHaveLength(1);
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
});
test('responsive Dashboard shows truthful source availability without unsupported KPI counters', async ({ page, context }) => {
  await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByText('Nguồn dữ liệu & phạm vi thống kê', { exact: true }).click();
  await expect(page.getByText('Nguồn nghiệp vụ chưa được triển khai').first()).toBeVisible();
  await expect(page.getByText('Giao thành công', { exact: true })).toHaveCount(0);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-dashboard-desktop.png', fullPage: true, animations: 'disabled' });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-dashboard-mobile.png', fullPage: true, animations: 'disabled' });
});

test('internal shell groups overview with work and distinguishes seven authorized dashboard views', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await expect(page.locator('.iw-nav-group')).toHaveCount(9);
  const overviewMenu = page.locator('.iw-nav-group').first();
  await expect(overviewMenu.locator('summary')).toHaveText('Tổng quan');
  await expect(overviewMenu.locator(':scope > div > a, :scope > div > button')).toHaveText(['Executive Dashboard', /My Approvals/, 'Operational Risk', 'Service Health']);
  const accountMenu = page.locator('.iw-nav-group').last();
  await expect(accountMenu.locator('summary')).toHaveText('Tài khoản & Công việc');
  await accountMenu.locator('summary').click();
  await expect(accountMenu.getByRole('link', { name: 'Tài khoản & Công việc', exact: true })).toHaveAttribute('href', '/admin/account');
  await expect(page.getByRole('tab')).toHaveCount(7);
  await page.getByRole('tab', { name: 'Finance Exposure', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Cước chưa thanh toán: 125.000 VND' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Merchant Queue', exact: true }).click();
  await expect(page.locator('[data-state="UNAVAILABLE"]')).toContainText('Chưa có nguồn hồ sơ');
  await expect(page.locator('.ap-kpi')).toHaveCount(0);
  await page.getByRole('tab', { name: 'Decisions', exact: true }).click();
  await expect(page.locator('[data-state="UNAVAILABLE"]')).toContainText('ApprovalTask');
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
});

test('direct unauthorized tab and module deny without fetching protected sources', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto(origin + '/admin?tab=finance');
  await expect(page.locator('[data-state="FORBIDDEN"]')).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Finance Exposure' })).toHaveCount(0);
  await expect(page.locator('.ap-kpi')).toHaveCount(0);
  expect(calls.some(v => v.includes('/platform'))).toBe(false);
  await page.goto(origin + '/admin/security/staff');
  await expect(page.locator('[data-state="FORBIDDEN"]')).toBeVisible();
  await expect(page.getByRole('button', { name: /approve|grant|publish/i })).toHaveCount(0);
});

test('shipment oversight deep link uses the same canonical list and safe back navigation', async ({ page, context }) => {
  const calls = await fixture(context), today = todayVietnam(), from = daysBefore(today, 6);
  await page.goto(origin + '/admin/shipments?from=' + from + '&to=' + today);
  await expect(page.getByRole('heading', { name: 'Giám sát vận đơn', exact: true })).toBeVisible();
  await expect(page.getByRole('table')).toBeVisible();
  expect(calls.filter(v => v.includes('/shipments?'))[0]).toContain('metric=BOOKINGS');
  await page.getByRole('button', { name: 'Về tổng quan', exact: true }).click();
  await expect(page).toHaveURL(/\/admin\?/);
  expect(new URL(page.url()).searchParams.get('from')).toBe(from);
  expect(new URL(page.url()).searchParams.get('to')).toBe(today);
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
});

test('expired snapshot is degraded and cannot launch a misleading drill-through', async ({ page, context }) => {
  const calls = await fixture(context, false, false, true); await page.goto(origin + '/admin');
  await expect(page.getByRole('alert')).toContainText('Snapshot đã hết thời hạn');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeDisabled();
  expect(calls.some(v => v.includes('/shipments?'))).toBe(false);
  await page.getByText('Định nghĩa chỉ số & Tính cập nhật', { exact: true }).click();
  await expect(page.getByText('Chưa được API canonical cung cấp.', { exact: false })).toBeVisible();
});

test('portal selector never lends SYSTEM Admin authority to operations or finance execution', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.locator('.iw-portals>summary').click();
  await expect(page.getByRole('button', { name: /^Carrier Operations/ })).toBeDisabled();
  await expect(page.getByRole('button', { name: /^Carrier Business & Finance/ })).toBeDisabled();
  calls.length = 0; await page.goto(origin + '/ops');
  await expect(page.getByRole('heading', { name: 'Đăng nhập Operations', exact: true })).toBeVisible();
  expect(calls).toEqual(['/api/v1/ops-auth/csrf', '/api/v1/ops-auth/refresh']);
  await page.goto(origin + '/finance');
  await expect(page.getByRole('heading', { name: 'Đăng nhập CSKH', exact: true })).toBeVisible();
  expect(calls.some(v => v.startsWith('/api/v1/support-operations/'))).toBe(false);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});

test('own account view shows current Backend grants without management or session-list authority', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin/account?tab=profile');
  await expect(page.getByRole('heading', { name: 'Tài khoản & Quyền hiện hành' })).toBeVisible();
  await expect(page.getByText('ADMIN_FINANCE_READ', { exact: true })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(0);
  await expect(page.locator('.iw-account').getByRole('button', { name: /cấp quyền|mời|MFA/i })).toHaveCount(0);
});

test('unknown region and scope query values are not silently treated as authorized filters', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin?regionId=foreign');
  await expect(page.getByRole('alert')).toContainText('Chọn khoảng thời gian hợp lệ');
  expect(calls.some(v => v.startsWith('/api/v1/admin/dashboard?'))).toBe(false);
});

test('mobile drawer backdrop remains clickable outside the sidebar', async ({ page, context }) => {
  await fixture(context); await page.setViewportSize({ width: 390, height: 844 }); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByRole('button', { name: 'Mở menu nội bộ', exact: true }).click();
  await expect(page.getByRole('navigation', { name: 'Menu phân hệ' })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng menu nội bộ', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Mở menu nội bộ', exact: true })).toHaveAttribute('aria-expanded', 'false');
  await expect(page.getByRole('button', { name: 'Đóng menu nội bộ', exact: true })).toHaveCount(0);
});

test('skip navigation stays visually clipped until keyboard focus and reaches the main content', async ({ page, context }) => {
  await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  const skip = page.getByRole('link', { name: 'Đến nội dung chính', exact: true });
  await expect(skip).toHaveCSS('clip-path', 'inset(50%)');
  await page.keyboard.press('Tab'); await expect(skip).toBeFocused();
  await expect(skip).toHaveCSS('clip-path', 'none');
  await page.keyboard.press('Enter'); await expect(page.locator('#internal-content')).toBeFocused();
});

test('KPI spark labels use the selected Booking grain rather than claiming daily data for week or month', async ({ page, context }) => {
  await fixture(context); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  await page.getByLabel('Thống kê theo', { exact: true }).selectOption('WEEK');
  await expect(page.getByRole('img', { name: 'Xu hướng vận đơn đã đặt theo tuần Booking', exact: true })).toBeVisible();
  await expect(page.getByRole('img', { name: 'Xu hướng vận đơn đã đặt theo ngày Booking', exact: true })).toHaveCount(0);
  await page.getByLabel('Thống kê theo', { exact: true }).selectOption('MONTH');
  await expect(page.getByRole('img', { name: 'Xu hướng vận đơn đã đặt theo tháng Booking', exact: true })).toBeVisible();
});

test('keyboard tab navigation selects only permitted panels and retains cohort without extra requests', async ({ page, context }) => {
  const calls = await fixture(context, true); await page.goto(origin + '/admin');
  await expect(page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' })).toBeVisible();
  const overview = page.getByRole('tab', { name: 'Tổng quan', exact: true });
  await overview.focus(); await page.keyboard.press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'Decisions', exact: true })).toBeFocused();
  await expect(page.getByRole('tabpanel', { name: 'Decisions', exact: true })).toBeVisible();
  await page.keyboard.press('Home'); await expect(overview).toBeFocused();
  await expect(page.getByRole('tabpanel', { name: 'Tổng quan', exact: true })).toBeVisible();
  expect(calls.filter(v => v.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
  expect(calls.some(v => v.includes('/platform'))).toBe(false);
});

test('masked shipment detail keeps the cohort and returns to the existing list without refreshing KPI', async ({ page, context }) => {
  const calls = await fixture(context); await page.goto(origin + '/admin');
  await page.getByRole('button', { name: 'Vận đơn đã đặt: 3 vận đơn' }).click();
  await page.getByRole('button', { name: /^Xem chi tiết AP-SIM-/ }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết giám sát vận đơn' })).toBeFocused();
  await expect(page.getByRole('columnheader', { name: 'Mã kiện' })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'AP-P-40000000000040008000000000000001', exact: true })).toBeVisible();
  const detail = calls.find(path => path.startsWith('/api/v1/admin/shipments/'))!;
  expect(detail).toContain('snapshotId='); expect(detail).toContain('metric=BOOKINGS');expect(detail).not.toContain('page=');
  await page.getByRole('button', { name: 'Về danh sách vận đơn' }).click();
  await expect(page.getByRole('columnheader', { name: 'Mã vận đơn' })).toBeVisible();
  expect(calls.filter(path => path.startsWith('/api/v1/admin/dashboard?'))).toHaveLength(1);
  expect(calls.filter(path => path.startsWith('/api/v1/admin/dashboard/shipments'))).toHaveLength(1);
});


test('scoped audit shows metadata, preserves domain filters and clears rows on denial', async ({ page, context }) => {
  await fixture(context);const app = '30000000-0000-4000-8000-000000000001', policy = '40000000-0000-4000-8000-000000000001', realm = '20000000-0000-4000-8000-000000000001';
  await context.route(origin + '/api/v1/admin/merchant-governance/context', route => route.fulfill({ contentType: 'application/json', body: JSON.stringify({ accountId: adminUser.id, realmId: realm, authorityVersion: 'a'.repeat(64), assignments: [{ id: app, scopeType: 'POLICY', policyId: policy, organizationId: null, permissions: ['audit.read'], version: 1, expiresAt: new Date(Date.now() + 3600000).toISOString() }] }) }));
  const calls: string[] = [];let denied = false;
  await context.route(origin + '/api/v1/admin/audit?**', route => {
    calls.push(route.request().url());return route.fulfill({ status: denied ? 403 : 200, contentType: 'application/json', body: JSON.stringify(denied ? { code: 'ACCESS_DENIED' } : { items: [{ id: app, actorId: adminUser.id, operation: 'REJECT', targetType: 'APPLICATION', targetId: app, priorVersion: 6, newVersion: 7, revision: 1, policyVersion: '1', reasonCode: 'INDEPENDENT_REJECTION', occurredAt: new Date().toISOString(), requestId: policy }], page: 0, size: 20, hasNext: false }) });
  });
  await page.goto(origin + '/admin/audit');await expect(page.getByRole('heading', { name: 'Lịch sử kiểm toán Merchant', exact: true })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'INDEPENDENT_REJECTION', exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.screenshot({ path: '../../../.demo/spec003-implementation/admin-audit-responsive.png', fullPage: true, animations: 'disabled' });
  expect(new URL(calls[0]).searchParams.get('domain')).toBe('MERCHANT_APPLICATION');expect(calls[0]).not.toContain('actorId=');
  denied = true;await page.getByRole('button', { name: 'Làm mới', exact: true }).click();await expect(page.getByRole('alert')).toBeVisible();await expect(page.getByRole('cell', { name: 'INDEPENDENT_REJECTION', exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
});
