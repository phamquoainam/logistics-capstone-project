import { applyOrganizationContactRows, blankImportRow } from '../src/customer/business-import-grid';
import { test, expect } from '@playwright/test';
import { parseOrganizationContact, parseOrganizationContactPage, parseContactDefault, contactSnapshot } from '../src/customer/organization-contacts-data';
import { createCustomerClient } from '../src/customer/client';
import { businessContextsFixture } from '../ui-unit-tests/support/business-fixture';
const context = businessContextsFixture[0], id = '10000000-0000-4000-8000-000000000031';
const contact = { id, organizationId: context.organizationId, kind: 'RECIPIENT', facilityId: null, label: null, reference: null, group: 'Synthetic', contactName: 'Synthetic recipient', contactPhone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' }, state: 'ACTIVE', verificationStatus: 'UNVERIFIED', version: 1, createdAt: '2026-11-15T00:00:00Z', updatedAt: '2026-11-15T00:00:00Z', archivedAt: null, allowedActions: ['UPDATE', 'ARCHIVE', 'SET_DEFAULT'] };
test('contact parsing rejects foreign ownership, credentials and lifecycle inconsistencies', () => {
  const c = parseOrganizationContact(contact, context.organizationId); const snapshot = contactSnapshot(c); snapshot.address.addressLine = 'Edited'; expect(c.address.addressLine).toBe('Synthetic street');
  for (const bad of [{ ...contact, organizationId: id }, { ...contact, verifiedEmail: 'synthetic@example.invalid' }, { ...contact, verificationStatus: 'VERIFIED' }, { ...contact, state: 'ARCHIVED' }, { ...contact, allowedActions: ['MARK_DELIVERED'] }]) expect(() => parseOrganizationContact(bad, context.organizationId)).toThrow();
  expect(() => parseOrganizationContactPage({ items: [contact, contact], page: 0, size: 20, hasNext: false }, context.organizationId, 0)).toThrow();
});
test('virtual defaults have no contact and cannot silently masquerade as saved state', () => {
  expect(parseContactDefault({ kind: 'SENDER', facilityId: null, contactId: null, version: 0, updatedAt: null }, 'SENDER', null).version).toBe(0);
  expect(() => parseContactDefault({ kind: 'SENDER', facilityId: null, contactId: id, version: 0, updatedAt: null }, 'SENDER', null)).toThrow();
});
test('contact search uses POST body with CSRF and no mutation key or contact PII in URL', async () => {
  const requests: string[] = [], now = Date.now(); const client = createCustomerClient({ lock: work => work(), now: () => now, fetch: async (path, init = {}) => {
    const url = String(path); if (url.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' }); if (url.endsWith('/refresh')) return Response.json({ accountId: id, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() }); if (url.endsWith('/me')) return Response.json({ accountId: id, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    requests.push(url); expect(url).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/contacts/search'); expect(init.method).toBe('POST'); expect(init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-csrf' }); expect(init.headers).not.toHaveProperty('Idempotency-Key'); expect(JSON.parse(init.body as string)).toEqual({ page: 0, size: 20, search: 'Synthetic' }); return Response.json({ items: [contact], page: 0, size: 20, hasNext: false }, { headers: { 'Cache-Control': 'no-store' } });
  } }); try { await client.initialize(); expect((await client.listOrganizationContacts(context, { page: 0, search: 'Synthetic' })).items).toHaveLength(1); expect(requests).toHaveLength(1); } finally { client.dispose(); }
});

test('bulk contact copy changes only explicit unlocked rows and never creates a shipment', () => {
  const first = blankImportRow(), second = blankImportRow(); const snapshot = { name: 'Synthetic copied', phone: '+84901234567', address: { country: 'VN' as const, administrativeArea: 'Area', locality: 'Locality', addressLine: 'Street' } };
  const copied = applyOrganizationContactRows([first, second], new Set([first.id]), new Set(), 'recipient', snapshot); expect(copied[0].cells.recipientName).toBe(snapshot.name); expect(copied[0].cells.senderName).toBe(first.cells.senderName); expect(copied[1]).toBe(second); expect(first.cells.recipientName).toBe(''); expect(() => applyOrganizationContactRows([first, second], new Set([second.id]), new Set([second.id]), 'recipient', snapshot)).toThrow('ROW_LOCKED');
});
