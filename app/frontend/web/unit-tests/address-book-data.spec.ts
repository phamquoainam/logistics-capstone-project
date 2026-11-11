import { test, expect } from '@playwright/test';
import { addressMatchKey, normalizeAddressInput } from '../src/customer/address-book-data';
import type { AddressInput } from '../src/customer/client';
const input: AddressInput = { kind: 'SENDER', label: '  Nhà  riêng ', contactName: '  Nguyễn  A ', contactPhone: '0901-234-567', address: { country: 'VN', administrativeArea: '  Tỉnh A ', locality: ' Phường  B ', addressLine: ' 12  Đường C ' }, isDefault: false };
test('normalization is explicit, immutable, preserves accents and standardizes only known formatting', () => {
  const before = structuredClone(input), n = normalizeAddressInput(input);
  expect(input).toEqual(before); expect(n.contactPhone).toBe('+84901234567'); expect(n.contactName).toBe('Nguyễn A'); expect(n.address.addressLine).toBe('12 Đường C');
  expect(normalizeAddressInput({ ...input, contactPhone: '84 901 234 567' }).contactPhone).toBe(n.contactPhone);
  expect(normalizeAddressInput({ ...input, contactPhone: 'invalid' }).contactPhone).toBe('invalid');
  expect(normalizeAddressInput({ ...input, contactName: 'Bad\nName' }).contactName).toContain('\n');
});
test('duplicate identity ignores label/default but retains kind, phone, name, accents and entire location', () => {
  const n = normalizeAddressInput(input), key = addressMatchKey(input);
  expect(addressMatchKey({ ...n, label: 'Khác', isDefault: true, contactName: 'NGUYỄN A' })).toBe(key);
  for (const change of [{ kind: 'RECIPIENT' as const }, { contactPhone: '+84901234568' }, { contactName: 'Nguyen A' }, { address: { ...n.address, locality: 'Phường C' } }]) expect(addressMatchKey({ ...n, ...change })).not.toBe(key);
});
