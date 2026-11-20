import { test, expect } from '@playwright/test';
import { organizationSupportLink, organizationSupportQuery } from '../src/customer/organization-support';
import { organizationReturn } from '../src/customer/organization-management';
const org = '10000000-0000-4000-8000-000000000001', shipment = '10000000-0000-4000-8000-000000000002';
test('support navigation retains only known view and an actual identifier shape', () => {
  const url = organizationSupportLink(org, 'tickets', shipment), parsed = organizationSupportQuery(new URL(url, 'https://local.invalid').searchParams);
  expect(parsed).toEqual({ valid: true, view: 'tickets', shipment, ticket: null, intake: null }); expect(organizationReturn(org, url)).toBe(url);
  expect(organizationSupportQuery(new URLSearchParams()).view).toBe('help');
});
test('Claim deep link binds a valid intake selector to one Ticket without allowing secret or duplicate selectors', () => {
  expect(organizationSupportQuery(new URLSearchParams({ view: 'tickets', ticket: org, intake: shipment })).valid).toBe(true);
  for (const raw of ['view=claims&intake=' + shipment, 'view=tickets&intake=' + shipment, 'view=tickets&ticket=' + org + '&intake=' + shipment + '&intake=' + shipment, 'view=tickets&ticket=' + org + '&intake=bad']) expect(organizationSupportQuery(new URLSearchParams(raw)).valid).toBe(false);
});
test('support query and detail return reject duplicate forged cross-organization and secret selectors', () => {
  for (const value of ['view=anything', 'view=help&view=tickets', 'shipment=bad', 'shipment=' + shipment + '&shipment=' + shipment, 'shipment=00000000-0000-0000-0000-000000000000', 'email=someone@example.invalid']) expect(organizationSupportQuery(new URLSearchParams(value)).valid).toBe(false);
  for (const value of [organizationSupportLink(shipment, 'claims', shipment), '/merchant/' + org + '/support?accountId=' + org, '/merchant/' + org + '/support#secret', 'https://evil.invalid/merchant/' + org + '/support']) expect(organizationReturn(org, value)).toBe('/merchant/' + org + '/shipments');
});
