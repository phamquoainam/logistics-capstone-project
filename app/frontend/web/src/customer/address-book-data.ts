import type { AddressInput } from './client';

const tidy = (value: string) => value.normalize('NFC').trim().replace(/ +/gu, ' ');
export function normalizeAddressInput(value: AddressInput): AddressInput {
  let phone = value.contactPhone.trim().replace(/[ ()-]/gu, '');
  if (/^0[35789][0-9]{8}$/u.test(phone)) phone = '+84' + phone.slice(1);
  else if (/^84[35789][0-9]{8}$/u.test(phone)) phone = '+' + phone;
  return { ...value, ...(value.label !== undefined ? { label: tidy(value.label) } : {}), contactName: tidy(value.contactName), contactPhone: phone,
    address: { country: value.address.country, administrativeArea: tidy(value.address.administrativeArea), locality: tidy(value.address.locality), addressLine: tidy(value.address.addressLine) } };
}
/** Advisory comparison only: no geocoding, accent removal, identity proof or DB uniqueness. */
export function addressMatchKey(value: AddressInput): string {
  const n = normalizeAddressInput(value);
  return JSON.stringify([n.kind, n.contactName.toLocaleLowerCase('vi'), n.contactPhone, n.address.country,
    n.address.administrativeArea.toLocaleLowerCase('vi'), n.address.locality.toLocaleLowerCase('vi'), n.address.addressLine.toLocaleLowerCase('vi')]);
}
