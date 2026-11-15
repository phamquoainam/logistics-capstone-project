import { test, expect } from '@playwright/test';
import { organizationQuery, organizationReturn, organizationDetailLink } from '../src/customer/organization-management';
import { parseAssignment, parseAssignmentMembers, parseInternalHistory, parseOrganizationExport, exportColumns } from '../src/customer/business-management-data';
import { csvCell } from '../src/customer/shipment-management';
import { createCustomerClient } from '../src/customer/client';
import { businessContextsFixture } from '../ui-unit-tests/support/business-fixture';
const org = '10000000-0000-4000-8000-000000000001', shipment = '10000000-0000-4000-8000-000000000002';
test('organization list query retains filter cohort and canonical waiting category', () => {
  const q = organizationQuery(new URLSearchParams({ category: 'WAITING', sourceChannel: 'WEB', sort: 'FEE_DESC', page: '2' })); expect(q.valid).toBe(true); expect(q.api.get('status')).toBe('WAITING_FOR_HANDOVER'); expect(q.api.has('category')).toBe(false); expect(q.api.get('page')).toBe('2'); expect(organizationQuery(new URLSearchParams('status=WAITING_FOR_HANDOVER')).category).toBe('WAITING');
});
test('organization query rejects duplicate, forged and overflowing selectors', () => {
  for (const raw of ['page=2147483648', 'page=-1', 'page=01', 'category=ANYTHING', 'accountId=' + org, 'search=a&search=b', 'search=%00', 'page=Infinity']) expect(organizationQuery(new URLSearchParams(raw)).valid, raw).toBe(false);
});
test('organization detail return keeps only exact same-organization valid list URL', () => {
  const base = '/merchant/' + org + '/shipments', q = new URLSearchParams({ search: 'AP-SIM-test', sourceChannel: 'WEB', page: '2' }), link = organizationDetailLink(org, shipment, q), returned = new URL(link, 'https://local.invalid').searchParams.get('returnTo'); expect(organizationReturn(org, returned)).toBe(base + '?' + q);
  for (const value of ['https://evil.invalid/', '//evil.invalid', '/personal/shipments', '/merchant/' + shipment + '/shipments', base + '/x', base + '?secret=x', base + '?page=0&page=1', base + '#secret']) expect(organizationReturn(org, value)).toBe(base);
});
test('virtual assignment is read-only version zero and versions bind organization and Shipment', () => {
  const value = { shipmentId: shipment, organizationId: org, assigneeAccountId: null, version: 0, updatedAt: null }; expect(parseAssignment(value, org, shipment)).toEqual(value);
  for (const bad of [{ ...value, version: 1 }, { ...value, assigneeAccountId: org }, { ...value, organizationId: shipment }, { ...value, shipmentId: org }, { ...value, token: 'not-allowed' }]) expect(() => parseAssignment(bad, org, shipment)).toThrow();
});
test('directory exposes display names only and rejects contacts or repeated accounts', () => {
  const member = { accountId: org, membershipId: shipment, displayName: 'Synthetic Member', role: 'OPERATOR' }, page = { items: [member], page: 0, size: 20, hasNext: false }; expect(parseAssignmentMembers(page, 0).items[0].displayName).toBe('Synthetic Member'); expect(() => parseAssignmentMembers({ ...page, items: [{ ...member, email: 'synthetic@example.invalid' }] }, 0)).toThrow(); expect(() => parseAssignmentMembers({ ...page, items: [member, member] }, 0)).toThrow(); expect(() => parseAssignmentMembers(page, 1)).toThrow();
});
test('export schema refuses unexpected PII, oversized output and numeric spreadsheet cells', () => {
  const value = { schemaVersion: 'ORGANIZATION_EXPORT_1', exportId: shipment, organizationId: org, generatedAt: '2026-11-13T00:00:00Z', timeZone: 'Asia/Ho_Chi_Minh', piiIncluded: false, rowCount: 1, columns: exportColumns, rows: [exportColumns.map(() => '')] }; expect(parseOrganizationExport(value, org, false).rowCount).toBe(1);
  for (const bad of [{ ...value, piiIncluded: true }, { ...value, columns: [...exportColumns, 'recipientEmail'] }, { ...value, rowCount: 201 }, { ...value, rows: [[25000, ...value.rows[0].slice(1)]] }]) expect(() => parseOrganizationExport(bad, org, false)).toThrow();
});
test('CSV protects formula and DDE prefixes while keeping content literal', () => {
  for (const value of ['=HYPERLINK(1)', '+84901234567', '-1+1', '@SUM(A1)', '\t=1', '\r=1']) expect(csvCell(value)).toContain("'"); expect(csvCell('Synthetic, "parcel"')).toBe('"Synthetic, ""parcel"""');
});
test('internal history enforces monotonic versions and never accepts Carrier events', () => {
  const event = { id: shipment, operation: 'ASSIGNED', actorAccountId: org, priorAssigneeAccountId: null, assigneeAccountId: org, priorVersion: 0, newVersion: 1, occurredAt: '2026-11-13T00:00:00Z' }, page = { items: [event], page: 0, size: 20, hasNext: false }; expect(parseInternalHistory(page, 0).items).toHaveLength(1);
  for (const bad of [{ ...event, operation: 'DELIVERED' }, { ...event, newVersion: 3 }, { ...event, assigneeAccountId: null }]) expect(() => parseInternalHistory({ ...page, items: [bad] }, 0)).toThrow(); expect(() => parseInternalHistory({ ...page, items: [event, event] }, 0)).toThrow();
});
function transport(handle: (path: string, init: RequestInit) => Promise<Response>) {
  const now = Date.now(); return createCustomerClient({ lock: work => work(), now: () => now, fetch: async (path, init = {}) => { const url = String(path); if (url.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' }); if (url.endsWith('/refresh')) return Response.json({ accountId: org, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() }); if (url.endsWith('/me')) return Response.json({ accountId: org, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 }); return handle(url, init); } });
}
test('assignment transport sends a0 with current CSRF and only accepted body fields', async () => {
  const context = businessContextsFixture[0], key = '10000000-0000-4000-8000-000000000003'; let writes = 0;
  const client = transport(async (path, init) => { writes++; expect(path).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/shipments/' + shipment + '/assignment'); expect(init.method).toBe('PUT'); expect(init.headers).toMatchObject({ 'If-Match': '"a0"', 'Idempotency-Key': key, 'X-CSRF-Token': 'synthetic-csrf' }); expect(JSON.parse(init.body as string)).toEqual({ assigneeAccountId: org, reasonCode: 'INITIAL_ASSIGNMENT' }); return Response.json({ shipmentId: shipment, organizationId: context.organizationId, assigneeAccountId: org, version: 1, updatedAt: '2026-11-13T00:00:00Z' }, { headers: { 'Cache-Control': 'no-store', ETag: '"a1"' } }); });
  try { await client.initialize(); expect((await client.updateBusinessAssignment(context, shipment, org, 'INITIAL_ASSIGNMENT', 0, key)).version).toBe(1); expect(writes).toBe(1); } finally { client.dispose(); }
});
test('malformed assignment success is uncertain and receipt resolution remains read only', async () => {
  const context = businessContextsFixture[0], key = '10000000-0000-4000-8000-000000000003', requests: string[] = [];
  const client = transport(async (path, init) => { requests.push(String(init.method ?? 'GET') + ' ' + path); return path.includes('/shipment-assignment-receipts/') ? Response.json({ targetId: shipment, version: 1, outcome: 'COMMITTED' }, { headers: { 'Cache-Control': 'no-store' } }) : Response.json({ shipmentId: shipment, organizationId: org, assigneeAccountId: org, version: 1, updatedAt: '2026-11-13T00:00:00Z' }, { headers: { 'Cache-Control': 'no-store', ETag: '"a1"' } }); });
  try { await client.initialize(); await expect(client.updateBusinessAssignment(context, shipment, org, 'INITIAL_ASSIGNMENT', 0, key)).rejects.toThrow('OUTCOME_UNKNOWN'); expect((await client.readBusinessAssignmentReceipt(context, key)).targetId).toBe(shipment); expect(requests.map(r => r.split(' ')[0])).toEqual(['PUT', 'GET']); } finally { client.dispose(); }
});
