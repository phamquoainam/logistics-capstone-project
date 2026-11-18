import { test, expect } from '@playwright/test';
import { organizationSupportLink, organizationSupportQuery } from '../src/customer/organization-support';
import { organizationReturn } from '../src/customer/organization-management';
const org = '10000000-0000-4000-8000-000000000001', shipment = '10000000-0000-4000-8000-000000000002';
test('support navigation retains only known view and an actual identifier shape', () => {
  const url = organizationSupportLink(org, 'tickets', shipment), parsed = organizationSupportQuery(new URL(url, 'https://local.invalid').searchParams);
  expect(parsed).toEqual({ valid: true, view: 'tickets', shipment, ticket: null }); expect(organizationReturn(org, url)).toBe(url);
  expect(organizationSupportQuery(new URLSearchParams()).view).toBe('help');
});
test('support query and detail return reject duplicate forged cross-organization and secret selectors', () => {
  for (const value of ['view=anything', 'view=help&view=tickets', 'shipment=bad', 'shipment=' + shipment + '&shipment=' + shipment, 'shipment=00000000-0000-0000-0000-000000000000', 'email=someone@example.invalid']) expect(organizationSupportQuery(new URLSearchParams(value)).valid).toBe(false);
  for (const value of [organizationSupportLink(shipment, 'claims', shipment), '/merchant/' + org + '/support?accountId=' + org, '/merchant/' + org + '/support#secret', 'https://evil.invalid/merchant/' + org + '/support']) expect(organizationReturn(org, value)).toBe('/merchant/' + org + '/shipments');
});
