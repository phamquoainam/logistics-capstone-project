import { test, expect } from '@playwright/test';
import { crc32, labelZip, managementCsv, managementReturn } from '../src/customer/shipment-management';
import { managementFixture } from '../ui-unit-tests/support/management-fixture';

test('detail return accepts only bounded Personal list URLs and never external paths', () => {
  expect(managementReturn('/personal/shipments?page=2&status=AT_STATION')).toBe('/personal/shipments?page=2&status=AT_STATION');
  for (const value of ['https://foreign.invalid', '//foreign.invalid', '/personal/shipments/foreign', '/personal/shipments?ownerId=other', '/personal/shipments#x', '/personal/shipments?' + 'x'.repeat(4100), null]) expect(managementReturn(value)).toBeUndefined();
});
test('CSV is a bounded source-page snapshot with exact VND, selected columns and simulation provenance', () => {
  const data = managementFixture(new URLSearchParams(), 45), item = data.items[0];
  item.record.shipment.input.reference = '=HYPERLINK("unsafe")';
  item.record.shipment.input.recipient.name = ' \t+SUM(1,2)';
  const csv = managementCsv(data, [item], ['contacts', 'fees'], 'status=AT_STATION');
  expect(csv.startsWith('\uFEFF')).toBe(true); expect(csv).toContain('LOCAL/TEST SIMULATION');
  expect(csv).toContain('1 vận đơn đã tải; không phải toàn bộ 45');
  expect(csv).toContain('"\'=HYPERLINK(""unsafe"")"'); expect(csv).toContain('"\' \t+SUM(1,2)"');
  expect(csv).toContain('"' + item.record.shipment.totalVnd + '"'); expect(csv).toContain('"\'+84901234567"');
  expect(csv).not.toContain(item.record.shipment.input.recipient.address.addressLine);
  expect(csv).not.toContain('Khu vực gửi');
  expect(managementCsv(data, [item], [], '')).not.toContain(item.record.shipment.input.recipient.phone);
});
test('ZIP retains each PDF byte stream and uses standard CRC32 plus deterministic member offsets', async () => {
  expect(crc32(new TextEncoder().encode('123456789'))).toBe(0xcbf43926);
  const files = [{ name: 'shipment-a.pdf', bytes: new TextEncoder().encode('%PDF-1.7\nsynthetic-a') }, { name: 'shipment-b.pdf', bytes: new TextEncoder().encode('%PDF-1.7\nsynthetic-b') }];
  const result = new Uint8Array(await labelZip(files).arrayBuffer()), view = new DataView(result.buffer);
  expect(view.getUint32(0, true)).toBe(0x04034b50); expect(view.getUint32(result.length - 22, true)).toBe(0x06054b50);
  expect(view.getUint16(result.length - 12, true)).toBe(2);
  const decoded = new TextDecoder().decode(result); for (const f of files) { expect(decoded).toContain(f.name); expect(decoded).toContain(new TextDecoder().decode(f.bytes)); }
});
test('label bundle rejects duplicates traversal oversized memory use and more than current-page20 files', () => {
  const file = { name: 'shipment-a.pdf', bytes: new Uint8Array(1) };
  for (const files of [[], [file, file], [{ ...file, name: '../other.pdf' }], Array.from({ length: 21 }, (_, n) => ({ ...file, name: `shipment-${n}.pdf` })), [{ ...file, bytes: new Uint8Array(32 * 1024 * 1024 + 1) }]]) expect(() => labelZip(files)).toThrow('LABEL_BUNDLE_LIMIT');
});
