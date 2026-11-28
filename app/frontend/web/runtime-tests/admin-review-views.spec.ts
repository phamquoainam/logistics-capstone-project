import { test, expect } from '@playwright/test';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
const directory = process.env.S003_BROWSER_PRIVATE_DIRECTORY;
if (!directory) throw new Error('OWNED_ADMIN_CONTEXT_REQUIRED');

test('M2-R01 genuine HTTPS PG18 search, immutable comparison, comments and Organization disclosures', async ({ page, context }) => {
  const fixture = JSON.parse(await readFile(resolve(directory!, 'fixture-login.json'), 'utf8')) as { username: string; password: string; applicationId: string };
  await page.goto('/admin/login');await page.getByLabel('Tên đăng nhập', { exact: true }).fill(fixture.username);await page.getByLabel('Mật khẩu', { exact: true }).fill(fixture.password);
  const login = page.waitForResponse(r => new URL(r.url()).pathname === '/api/v1/auth/login');await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();const session = await login;expect(session.status()).toBe(200);const access = (await session.json() as { accessToken: string }).accessToken;
  await expect(page.getByRole('heading', { name: 'Tổng quan điều hành', exact: true })).toBeVisible();await page.goto('/admin/merchants/applications');
  await page.getByText('Tìm kiếm nâng cao theo dữ liệu đã xác minh', { exact: true }).click();await page.getByLabel('Mã đăng ký', { exact: true }).fill('SYNTHETIC-REG-1');
  const search = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/merchant-applications/search'));await page.getByRole('button', { name: 'Tìm trong phạm vi', exact: true }).click();expect((await search).status()).toBe(200);await expect(page.getByText('1 kết quả phù hợp', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Mở kết quả', exact: true }).click();await expect(page.getByText('Đã duyệt · Revision 2 · Phiên bản 12', { exact: true })).toBeVisible();
  const comparison = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/revision-comparisons'));await page.getByRole('button', { name: 'So sánh revision', exact: true }).click();expect((await comparison).status()).toBe(200);await expect(page.getByRole('cell', { name: 'activationDisplayName', exact: true })).toBeVisible();
  const checks = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/checks'));await page.getByRole('button', { name: 'Kiểm tra nguồn hiện hành', exact: true }).click();expect((await checks).status()).toBe(200);await expect(page.getByText('SUBMISSION_BINDING', { exact: true })).toBeVisible();
  const text = 'Synthetic HTTPS review note <script>never executable</script>';await page.getByLabel('Ghi chú nội bộ', { exact: true }).fill(text);const comment = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/review-comments'));await page.getByRole('button', { name: 'Lưu ghi chú', exact: true }).click();const saved = await comment;expect(saved.status()).toBe(200);
  const history = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/comment-disclosures'));await page.getByRole('button', { name: 'Đọc ghi chú nội bộ', exact: true }).click();expect((await history).status()).toBe(200);await expect(page.getByText(text, { exact: true })).toBeVisible();await expect(page.locator('script').filter({ hasText: 'never executable' })).toHaveCount(0);
  const app = fixture.applicationId, root = '/api/v1/admin/merchant-applications/' + app;
  expect((await context.request.post(root + '/revision-comparisons', { headers: { Authorization: 'Bearer ' + access, Origin: new URL(page.url()).origin }, data: { fromRevision: 1, toRevision: 2, purpose: 'OTHER' } })).status()).toBe(400);
  expect((await context.request.post(root + '/review-comments', { headers: { Authorization: 'Bearer ' + access, Origin: 'https://foreign.example.invalid', 'Idempotency-Key': crypto.randomUUID(), 'If-Match': '"v12"' }, data: { revision: 2, text: 'Denied foreign origin' } })).status()).toBe(401);
  await page.getByRole('link', { name: 'Mở Organization đã kích hoạt', exact: true }).click();await expect(page.getByRole('heading', { name: 'Hồ sơ và thành viên tổ chức', exact: true })).toBeVisible();
  const profile = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/profile-disclosures'));await page.getByRole('button', { name: 'Đọc hồ sơ tổ chức', exact: true }).click();expect((await profile).status()).toBe(200);await expect(page.getByRole('heading', { name: 'Pháp lý', exact: true })).toBeVisible();
  const members = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/membership-disclosures'));await page.getByRole('button', { name: 'Đọc metadata thành viên', exact: true }).click();expect((await members).status()).toBe(200);await expect(page.getByRole('cell', { name: 'Current Owner', exact: true })).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true);
});
