import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';
import { businessContextsFixture, organizationA } from './support/business-fixture';
import { blankImportRow, canonicalValues, gridRowsFromCsv, serverCsv } from '../src/customer/business-import-grid';
import { readImportXlsx, writeImportXlsx } from '../src/customer/business-import-excel';
import { readFile } from 'node:fs/promises';
const account = '10000000-0000-4000-8000-000000000001', jobId = '10000000-0000-4000-8000-000000000808', draftId = '10000000-0000-4000-8000-000000000809';
function sample() { const r = blankImportRow(); Object.assign(r.cells, { sourceChannel: 'CSV', sourceOrderReference: '001-SYNTHETIC', senderName: 'Nguyễn Thử Nghiệm', senderPhone: '0901234567', senderAdministrativeArea: 'SIM-A1', senderLocality: 'Synthetic ward', senderAddressLine: 'Synthetic address', recipientName: 'Synthetic Recipient', recipientPhone: '0912345678', recipientAdministrativeArea: 'SIM-A2', recipientLocality: 'Synthetic ward', recipientAddressLine: 'Synthetic destination', description: 'Synthetic parcel', weightGrams: '500', lengthCm: '10', widthCm: '10', heightCm: '10' }); return r; }
const contact = { name: 'Synthetic contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
const input = { facilityId: null, sourceChannel: 'CSV', sourceOrderReference: '001-SYNTHETIC', reference: null, instructions: null, sender: contact, recipient: contact, parcels: [{ description: 'Synthetic parcel', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }] };
async function setup(page: Page) {
  let previews = 0, commits = 0, version = 1;
  let job: Record<string, unknown>;
  const result = () => ({ id: jobId, organizationId: organizationA, facilityId: null, creatorAccountId: account, templateVersion: 'BUSINESS_CSV_V1', state: version === 1 ? 'PREVIEW' : 'COMPLETED', version, totalRows: 1, validRows: version === 1 ? 1 : 0, invalidRows: 0, createdRows: version === 1 ? 0 : 1, conflictRows: 0, createdAt: '2026-11-10T01:00:00Z', updatedAt: '2026-11-10T01:01:00Z', page: 0, size: 20, hasNext: false, rows: [{ rowNumber: 1, state: version === 1 ? 'VALID' : 'CREATED', input, errors: [], preparationId: version === 1 ? null : draftId, version }] });
  await page.context().route('**/api/v1/**', async route => {
    const path = new URL(route.request().url()).pathname;
    const reply = (body: unknown, etag?: string) => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(body) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: account, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: account, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [{ ...businessContextsFixture[0], permissions: [...businessContextsFixture[0].permissions, 'shipment.draft.read', 'shipment.draft.write', 'shipment.import'] }] });
    if (path.endsWith('/shipment-imports') && route.request().method() === 'POST') { previews++; const payload = route.request().postDataJSON(); expect(payload.templateVersion).toBe('BUSINESS_CSV_V1'); const row = gridRowsFromCsv(payload.csv)[0]; input.recipient.name = row.cells.recipientName; version = 1; job = result(); return reply(job, '"v1"'); }
    if (path.endsWith('/commit')) { commits++; expect(route.request().headers()['if-match']).toBe('"v1"'); expect(route.request().postDataJSON()).toEqual({ rows: [1] }); version = 2; job = result(); return reply(job, '"v2"'); }
    if (path.endsWith('/shipment-imports/' + jobId)) return reply(job, '"v' + version + '"');
    if (path.endsWith('/shipment-imports')) return reply({ items: job ? [job] : [], page: 0, size: 20, hasNext: false });
    return route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 'RESOURCE_NOT_FOUND' }) });
  });
  await page.goto('/merchant/' + organizationA + '/imports'); await expect(page.getByRole('heading', { name: 'Nhập đơn hàng từ file' })).toBeVisible();
  return { counts: () => ({ previews, commits }) };
}
test('import mode retains the creation menu and protects unsubmitted worksheet data', async ({ page }) => {
  const api = await setup(page); const modes = page.getByRole('navigation', { name: 'Cách tạo vận đơn', exact: true });
  await expect(page.getByRole('navigation', { name: 'Điều hướng Business', exact: true }).getByRole('link', { name: 'Tạo vận đơn', exact: true })).toHaveAttribute('aria-current', 'page');
  await expect(modes.getByRole('link', { name: 'Nhập CSV/Excel', exact: true })).toHaveAttribute('aria-current', 'page');
  await page.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic.csv', mimeType: 'text/csv', buffer: Buffer.from(serverCsv([sample()])) });
  await expect(page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true })).toHaveValue('001-SYNTHETIC');
  page.once('dialog', dialog => dialog.dismiss()); await modes.getByRole('link', { name: 'Tạo thủ công', exact: true }).click(); await expect(page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true })).toHaveValue('001-SYNTHETIC');
  page.once('dialog', dialog => dialog.accept()); await modes.getByRole('link', { name: 'Tạo thủ công', exact: true }).click(); await expect(page.locator('#sender-name')).toBeVisible(); expect(api.counts()).toEqual({ previews: 0, commits: 0 });
});

test('worksheet edits invalidate backend checks; undo, selection, explicit commit and real history UI', async ({ page }) => {
  const api = await setup(page); await page.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic.csv', mimeType: 'text/csv', buffer: Buffer.from(serverCsv([sample()])) });
  await expect(page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true })).toHaveValue('001-SYNTHETIC');
  await page.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click(); await expect(page.locator('.ig-totals dd')).toHaveText(['1', '1', '0', '0', '0']);
  await page.getByRole('button', { name: 'Chọn tất cả dòng hợp lệ', exact: true }).click(); await expect(page.getByRole('button', { name: 'Lưu 1 dòng thành nháp' })).toBeEnabled();
  await page.getByLabel('Tên người nhận — dòng 1', { exact: true }).fill('Changed recipient'); await expect(page.getByRole('button', { name: 'Lưu 0 dòng thành nháp' })).toBeDisabled(); await expect(page.locator('.ig-totals dd').nth(1)).toHaveText('—');
  await page.getByRole('button', { name: 'Hoàn tác', exact: true }).click(); await expect(page.getByLabel('Tên người nhận — dòng 1', { exact: true })).toHaveValue('Synthetic Recipient'); await expect(page.locator('.ig-totals dd').nth(1)).toHaveText('—');
  await page.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click(); await expect(page.locator('.ig-totals dd').nth(1)).toHaveText('1'); await page.getByRole('button', { name: 'Chọn tất cả dòng hợp lệ', exact: true }).click();
  page.once('dialog', d => d.accept()); await page.getByRole('button', { name: 'Lưu 1 dòng thành nháp' }).click(); await expect(page.locator('.ig-totals dd')).toHaveText(['1', '0', '0', '1', '0']); await expect(page.getByLabel('Tên người nhận — dòng 1', { exact: true })).toHaveAttribute('readonly', '');
  expect(api.counts()).toEqual({ previews: 2, commits: 1 }); await page.getByRole('button', { name: 'Lịch sử nhập file', exact: true }).click(); await expect(page.getByRole('dialog')).toContainText(jobId.slice(0, 8)); await page.getByRole('button', { name: 'Mở đợt nhập' }).click(); await expect(page.getByRole('link', { name: 'Mở nháp', exact: true })).toHaveAttribute('href', '/merchant/' + organizationA + '/create?draft=' + draftId);
});
test('actual browser Worker reads and writes XLSX without losing contacts or extra parcels', async ({ page }) => {
  await setup(page); const row = sample(); row.extras = [{ description: 'Synthetic parcel 2', quantity: '1', weightGrams: '650', lengthCm: '12.5', widthCm: '11', heightCm: '10', declaredValueVnd: '0' }];
  await page.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer: Buffer.from(writeImportXlsx([row])) });
  await expect(page.getByLabel('Điện thoại người gửi — dòng 1', { exact: true })).toHaveValue('0901234567'); await expect(page.getByLabel('Quản lý kiện dòng 1')).toContainText('2 kiện');
  await page.screenshot({ path: '../../../.demo/business-import-grid/excel-open-desktop.png', fullPage: true, animations: 'disabled' });
  await page.getByRole('button', { name: 'Xuất bảng hiện tại', exact: true }).click(); const exporting = page.waitForEvent('download'); await page.getByRole('button', { name: /Excel .xlsx/ }).click(); const out = await exporting, bytes = await readFile((await out.path())!); const rows = await readImportXlsx(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength)); expect(rows.map(canonicalValues)).toEqual([canonicalValues(row)]);
  await page.getByRole('button', { name: 'Tải file mẫu', exact: true }).click(); const template = page.waitForEvent('download'); await page.getByRole('button', { name: /CSV UTF-8/ }).click(); expect((await template).suggestedFilename()).toBe('argopost-business-mau.csv');
});
test('grid search, visible columns, keyboard paste, parcel editor and local undo work without API writes', async ({ page }) => {
  const api = await setup(page); await page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true }).fill('SOURCE-ONE'); await page.getByLabel('Mã đơn nguồn — dòng 2', { exact: true }).fill('SOURCE-TWO');
  await page.getByLabel('Tìm trong bảng nhập').fill('TWO'); await expect(page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true })).toHaveCount(0); await expect(page.getByLabel('Mã đơn nguồn — dòng 2', { exact: true })).toBeVisible(); await page.getByLabel('Tìm trong bảng nhập').clear();
  await page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true }).evaluate(element => { const data = new DataTransfer(); data.setData('text/plain', 'PASTED-ONE\tCSV\nPASTED-TWO\tOMS'); element.dispatchEvent(new ClipboardEvent('paste', { clipboardData: data, bubbles: true, cancelable: true })); });
  await expect(page.getByLabel('Mã đơn nguồn — dòng 1', { exact: true })).toHaveValue('PASTED-ONE'); await expect(page.getByLabel('Nguồn đơn — dòng 2', { exact: true })).toHaveValue('OMS');
  await page.getByRole('button', { name: 'Quản lý kiện dòng 1', exact: true }).click(); await page.getByRole('button', { name: 'Thêm kiện (tối đa 5)', exact: true }).click(); await page.getByRole('button', { name: 'Áp dụng kiện hàng', exact: true }).click(); await expect(page.getByRole('button', { name: 'Quản lý kiện dòng 1', exact: true })).toContainText('2 kiện');
  await page.getByRole('button', { name: 'Hoàn tác', exact: true }).click(); await expect(page.getByRole('button', { name: 'Quản lý kiện dòng 1', exact: true })).toContainText('1 kiện'); await page.getByRole('button', { name: 'Làm lại', exact: true }).click(); await expect(page.getByRole('button', { name: 'Quản lý kiện dòng 1', exact: true })).toContainText('2 kiện');
  await page.getByRole('button', { name: 'Chọn cột hiển thị', exact: true }).click(); await page.getByRole('dialog').getByLabel('Tên người gửi', { exact: true }).uncheck(); await page.getByRole('button', { name: 'Đóng cửa sổ' }).click(); await expect(page.getByLabel('Tên người gửi — dòng 1', { exact: true })).toHaveCount(0); expect(api.counts()).toEqual({ previews: 0, commits: 0 });
});
test('more than twenty selected rows commit in bounded versioned chunks without duplicate drafts', async ({ page }) => {
  await setup(page); const created = new Set<number>(), batches: number[][] = [], keys: string[] = []; let version = 1;
  const job = (pageNumber: number) => ({ id: jobId, organizationId: organizationA, facilityId: null, creatorAccountId: account, templateVersion: 'BUSINESS_CSV_V1', state: !created.size ? 'PREVIEW' : created.size === 23 ? 'COMPLETED' : 'PARTIAL', version, totalRows: 23, validRows: 23 - created.size, invalidRows: 0, createdRows: created.size, conflictRows: 0, createdAt: '2026-11-10T01:00:00Z', updatedAt: '2026-11-10T01:01:00Z', page: pageNumber, size: 20, hasNext: pageNumber === 0, rows: Array.from({ length: 23 }, (_, i) => ({ rowNumber: i + 1, state: created.has(i + 1) ? 'CREATED' : 'VALID', input: { ...input, sourceOrderReference: 'SYNTHETIC-' + (i + 1) }, errors: [], preparationId: created.has(i + 1) ? '10000000-0000-4000-8000-' + String(i + 1).padStart(12, '0') : null, version: created.has(i + 1) ? 2 : 1 })).slice(pageNumber * 20, pageNumber * 20 + 20) });
  await page.route('**/api/v1/merchant-organizations/' + organizationA + '/shipment-imports**', async route => {
    const request = route.request(), url = new URL(request.url());
    if (url.pathname.endsWith('/commit')) { expect(request.headers()['if-match']).toBe('"v' + version + '"'); const rows = request.postDataJSON().rows as number[]; expect(rows.length).toBeLessThanOrEqual(20); rows.forEach(n => { expect(created.has(n)).toBe(false); created.add(n); }); batches.push(rows); keys.push(request.headers()['idempotency-key']); version++; }
    const result = job(Number(url.searchParams.get('page') ?? 0)); return route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ETag: '"v' + version + '"' }, body: JSON.stringify(result) });
  });
  const rows = Array.from({ length: 23 }, (_, i) => { const row = sample(); row.cells.sourceOrderReference = 'SYNTHETIC-' + (i + 1); return row; });
  await page.getByLabel('Chọn file CSV hoặc Excel', { exact: true }).setInputFiles({ name: 'synthetic-23.csv', mimeType: 'text/csv', buffer: Buffer.from(serverCsv(rows)) }); await page.getByRole('button', { name: 'Kiểm tra dữ liệu', exact: true }).click(); await expect(page.locator('.ig-totals dd')).toHaveText(['23', '23', '0', '0', '0']);
  await page.getByRole('button', { name: 'Chọn tất cả dòng hợp lệ', exact: true }).click(); page.once('dialog', d => d.accept()); await page.getByRole('button', { name: 'Lưu 23 dòng thành nháp', exact: true }).click(); await expect(page.locator('.ig-totals dd')).toHaveText(['23', '0', '0', '23', '0']);
  expect(batches.map(rows => rows.length)).toEqual([20, 3]); expect([...created]).toEqual(Array.from({ length: 23 }, (_, i) => i + 1)); expect(new Set(keys).size).toBe(2); await expect(page.getByRole('link', { name: 'Mở nháp', exact: true })).toHaveCount(23);
});
