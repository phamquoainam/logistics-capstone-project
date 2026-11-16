import { test, expect } from '@playwright/test';
import { organizationA } from './support/business-fixture';
import { merchantFacilityFixture, siteId } from './support/merchant-facility-fixture';
const base = '/merchant/' + organizationA + '/facilities';
test('directory search stays out of URLs and default/archive follow explicit backend outcomes', async ({ page, context }) => {
  const api = await merchantFacilityFixture(context); page.on('dialog', d => { void d.accept(); }); await page.goto(base);
  await expect(page.getByRole('link', { name: 'Synthetic warehouse', exact: true })).toBeVisible();
  await page.getByLabel('Tìm địa điểm', { exact: true }).fill('Synthetic'); await page.getByRole('button', { name: 'Tìm kiếm', exact: true }).click();
  await expect.poll(() => api.searches.some(q => q.method === 'POST')).toBe(true); expect(page.url()).not.toContain('Synthetic');
  await page.getByRole('button', { name: 'Đặt mặc định', exact: true }).click(); await expect(page.locator('.wf-default-tag')).toBeVisible(); expect(api.writes.at(-1)?.expected).toBe('"d0"');
  await page.getByRole('button', { name: 'Vô hiệu hóa', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('đang được dùng'); expect(api.state.sites[0].state).toBe('ACTIVE');
  await page.getByRole('button', { name: 'Gỡ địa điểm mặc định', exact: true }).click(); await expect(page.locator('.wf-default-tag')).toHaveCount(0); await page.getByRole('button', { name: 'Vô hiệu hóa', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chưa có địa điểm phù hợp' })).toBeVisible();
});
test('create uses real contact fields and unknown write must be recovered before another command', async ({ page, context }) => {
  const api = await merchantFacilityFixture(context); await page.goto(base); await page.getByRole('button', { name: 'Thêm địa điểm' }).click(); const form = page.getByRole('form', { name: 'Tạo địa điểm' });
  await form.getByLabel('Tên địa điểm', { exact: true }).fill('Synthetic new warehouse'); await form.getByLabel('Mã địa điểm', { exact: true }).fill('NEW-01');
  await form.getByLabel('Họ tên người gửi', { exact: true }).fill('Synthetic representative'); await form.getByLabel('Số điện thoại người gửi', { exact: true }).fill('0901234567'); await form.getByLabel('Địa chỉ chi tiết (người gửi)', { exact: true }).fill('Synthetic street'); await form.getByLabel('Tỉnh / thành phố (người gửi)', { exact: true }).fill('Synthetic area'); await form.getByLabel('Phường / xã / địa phương (người gửi)', { exact: true }).fill('Synthetic locality');
  api.state.unknownNext = true; await form.getByRole('button', { name: 'Lưu địa điểm', exact: true }).click(); await expect(page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' })).toBeVisible(); await expect(form.getByRole('button', { name: 'Lưu địa điểm', exact: true })).toBeDisabled(); expect(api.writes).toHaveLength(1);
  await page.getByRole('button', { name: 'Kiểm tra kết quả thao tác' }).click(); await expect(page.getByRole('link', { name: 'Synthetic new warehouse', exact: true })).toBeVisible(); expect(api.writes).toHaveLength(1); expect(api.writes[0].body).toMatchObject({ operatingHours: null, senderContact: null, returnContact: null });
});
test('detail separates current contact/hours from responsibility and saving a team grants no rights', async ({ page, context }) => {
  const api = await merchantFacilityFixture(context); await page.goto(base + '/' + siteId); await expect(page.getByRole('heading', { name: 'Synthetic warehouse', exact: true })).toBeVisible(); await expect(page.getByRole('heading', { name: 'Giờ hoạt động', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Nhân viên phụ trách', exact: true }).click(); await expect(page.getByRole('cell', { name: 'Synthetic Customer', exact: true })).toBeVisible(); await page.getByRole('checkbox', { name: 'Synthetic Customer: Chuẩn bị hàng', exact: true }).check(); await page.getByRole('button', { name: 'Lưu trách nhiệm' }).click(); await expect.poll(() => api.state.teamVersion).toBe(1); expect(api.writes.at(-1)?.expected).toBe('"v0"'); expect(api.writes.some(w => /grant|membership/.test(w.path))).toBe(false);
  await page.getByRole('button', { name: 'Pickup', exact: true }).click(); await expect(page.getByRole('heading', { name: 'Chưa hỗ trợ yêu cầu Pickup' })).toBeVisible(); await expect(page.getByRole('button', { name: /Tạo Pickup|Carrier Accepted|Đã nhận hàng/ })).toHaveCount(0);
});
test('refresh clears a revoked directory and mobile editor has unique labelled fields without page overflow', async ({ page, context }, info) => {
  const api = await merchantFacilityFixture(context); await page.goto(base); await expect(page.getByRole('link', { name: 'Synthetic warehouse', exact: true })).toBeVisible(); api.state.denied = true; await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.getByRole('alert')).toContainText('không còn khả dụng'); await expect(page.locator('.wf-card tbody tr')).toHaveCount(0);
  api.state.denied = false; await page.getByRole('button', { name: 'Làm mới', exact: true }).click(); await expect(page.getByRole('link', { name: 'Synthetic warehouse', exact: true })).toBeVisible(); await page.getByRole('button', { name: 'Chỉnh sửa', exact: true }).click();
  const form = page.getByRole('form', { name: 'Chỉnh sửa địa điểm' }); await form.getByRole('checkbox', { name: 'Khai báo thông tin người gửi', exact: true }).check(); await form.getByRole('checkbox', { name: 'Khai báo liên hệ nhận hoàn riêng', exact: true }).check();
  expect(await form.locator('[id]').evaluateAll(nodes => new Set(nodes.map(n => n.id)).size === nodes.length)).toBe(true);
  await page.setViewportSize({ width: 390, height: 844 }); expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth + 1)).toBe(true); await page.screenshot({ path: info.outputPath('merchant-facility-editor-mobile-synthetic.png'), fullPage: true, animations: 'disabled' });
});
