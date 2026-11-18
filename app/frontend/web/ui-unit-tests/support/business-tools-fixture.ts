import type { BrowserContext } from '@playwright/test';
import type { BusinessCatalog, BusinessPreparation, BusinessQuoteView, BusinessShipmentView } from '../../src/customer/client';
import type { OrganizationManagementItem, OrganizationManagementPage } from '../../src/customer/business-management-data';
import { businessContextsFixture, organizationA } from './business-fixture';
import { toolsCatalog } from './shipping-tools-fixture';
import { exportColumns } from '../../src/customer/business-management-data';
export const toolsAccount = '10000000-0000-4000-8000-000000000001';
export const toolsId = (n: number) => '10000000-0000-4000-8000-' + String(n).padStart(12, '0');
export const toolsContext = { ...businessContextsFixture[0], permissions: ['dashboard.read', 'shipment.draft.read', 'shipment.draft.write', 'shipment.quote', 'shipment.book', 'shipment.label.read', 'shipment.export'] };
export function toolsDraft(n = 0): BusinessPreparation {
  const contact = { name: 'Synthetic contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
  return { id: toolsId(20 + n), organizationId: organizationA, facilityId: null, creatorAccountId: toolsAccount, state: 'DRAFT', preparationState: 'DRAFT', version: 1, input: { reference: 'ORDER-' + n, instructions: null, sender: contact, recipient: contact, parcels: [{ description: 'Synthetic parcel', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }] }, sourceChannel: 'WEB', sourceOrderReference: 'SOURCE-' + n, createdAt: '2026-11-16T00:00:00Z', updatedAt: '2026-11-16T00:00:00Z', allowedActions: ['UPDATE', 'ARCHIVE'] };
}
export function toolsQuote(d = toolsDraft(), n = 0): BusinessQuoteView {
  const createdAt = new Date().toISOString();
  return { quote: { id: toolsId(40 + n), organizationId: organizationA, facilityId: d.facilityId, creatorAccountId: toolsAccount, preparationId: d.id, preparationVersion: d.version, contractId: toolsId(10), contractVersion: 1, tariffVersion: toolsCatalog.tariff.version, route: toolsCatalog.coverage.routes[0], parcels: d.input.parcels.map(p => ({ weightGrams: p.weightGrams, lengthMm: p.lengthMm, widthMm: p.widthMm, heightMm: p.heightMm })), feeLines: d.input.parcels.map((p, i) => ({ parcelNumber: i + 1, actualGrams: p.weightGrams, chargeableGrams: 500, feeVnd: 25000 })), totalVnd: d.input.parcels.length * 25000, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE', createdAt, expiresAt: new Date(Date.parse(createdAt) + 900000).toISOString(), consumedBy: null }, usable: true, serverTime: createdAt };
}
export function toolsItem(n = 0): OrganizationManagementItem {
  const d = toolsDraft(n), q = toolsQuote(d, n), id = toolsId(60 + n), parcelId = toolsId(80 + n);
  const record: BusinessShipmentView = { simulation: true, shipment: { id, organizationId: organizationA, facilityId: null, creatorAccountId: toolsAccount, code: 'AP-SIM-' + id.replaceAll('-', ''), preparationId: d.id, preparationVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input: d.input, quote: { ...q.quote, consumedBy: id }, totalVnd: q.quote.totalVnd, createdAt: q.quote.createdAt, parcels: [{ id: parcelId, parcelNumber: 1, facts: d.input.parcels[0] }] }, obligation: { id: toolsId(100 + n), organizationId: organizationA, facilityId: null, shipmentId: id, quoteId: q.quote.id, amountVnd: q.quote.totalVnd, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-BUSINESS-LOCAL-01', createdAt: q.quote.createdAt } };
  return { record, status: 'WAITING_FOR_HANDOVER', source: { sourceChannel: d.sourceChannel, sourceOrderReference: d.sourceOrderReference, facilityId: null, importJobIds: [] }, assignment: { shipmentId: id, organizationId: organizationA, assigneeAccountId: null, version: 0, updatedAt: null }, allowedActions: ['READ_LABEL', 'EXPORT'] };
}
export function toolsPage(items = [toolsItem()]): OrganizationManagementPage { const fees = items.reduce((n, i) => n + i.record.shipment.totalVnd, 0); return { schemaVersion: 'ORGANIZATION_MANAGEMENT_1', generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', summary: { bookedShipments: String(items.length), waitingForHandover: String(items.length), unassigned: String(items.length), confirmedFeesVnd: String(fees), unpaidFeesVnd: String(fees) }, total: String(items.length), page: 0, size: 20, hasNext: false, items }; }
export async function businessToolsFixture(context: BrowserContext) {
  const drafts = [toolsDraft(), toolsDraft(1)], quotes = new Map<string, BusinessQuoteView>(), receipts = new Map<string, string>();
  const state = { denied: false, unknownNext: false, quoteEnabled: true, labelDenied: false }, calls: { path: string; method: string; query: URLSearchParams; body?: unknown }[] = [];
  await context.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname, method = req.method();
    const reply = (data: unknown, status = 200, etag?: string) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(data) });
    if (path.endsWith('/csrf')) return reply({ challenge: 'synthetic-csrf' });
    if (path.endsWith('/refresh')) return reply({ accountId: toolsAccount, accessToken: 'synthetic-access', expiresAt: new Date(Date.now() + 900000).toISOString() });
    if (path.endsWith('/me')) return reply({ accountId: toolsAccount, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    if (path.endsWith('/business-contexts')) return reply({ schemaVersion: 'BUSINESS_CONTEXTS_V1', items: [toolsContext] });
    calls.push({ path, method, query: url.searchParams, body: req.postData() ? req.postDataJSON() : undefined });
    if (!path.startsWith('/api/v1/merchant-organizations/' + organizationA) || state.denied) return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
    if (path.endsWith('/shipping-catalog')) { const catalog: BusinessCatalog = { ...toolsCatalog, quoteEnabled: state.quoteEnabled, bookingEnabled: state.quoteEnabled, tariff: state.quoteEnabled ? toolsCatalog.tariff : null, contract: { id: toolsId(10), organizationId: organizationA, version: 1, tariffVersion: toolsCatalog.tariff.version, state: state.quoteEnabled ? 'ACTIVE' : 'SUSPENDED', effectiveFrom: '2026-11-01T00:00:00Z', effectiveTo: null } }; return reply(catalog); }
    if (path.endsWith('/shipment-preparations')) return reply({ items: drafts, page: 0, size: 20, hasNext: false });
    const d = drafts.find(d => path.includes('/shipment-preparations/' + d.id));
    if (d && path.endsWith('/quotes')) { const q = toolsQuote(d, quotes.size); quotes.set(q.quote.id, q); receipts.set(req.headers()['idempotency-key'], q.quote.id); if (state.unknownNext) { state.unknownNext = false; return route.abort('connectionfailed'); } return reply(q, 201); }
    if (d && method === 'GET') return reply(d, 200, '"v1"');
    if (path.includes('/shipping-quote-receipts/')) { const id = receipts.get(path.split('/').at(-1)!); return id ? reply({ targetId: id, outcome: 'COMMITTED' }) : reply({ code: 'RESOURCE_NOT_FOUND' }, 404); }
    if (path.includes('/shipping-quotes/')) { const q = quotes.get(path.split('/').at(-1)!); return q ? reply(q) : reply({ code: 'RESOURCE_NOT_FOUND' }, 404); }
    if (path.endsWith('/labels')) return state.labelDenied ? reply({ code: 'RESOURCE_NOT_FOUND' }, 404) : route.fulfill({ contentType: 'application/pdf', headers: { 'Cache-Control': 'no-store' }, body: '%PDF-1.4\nSynthetic UI protocol fixture\n%%EOF' });
    if (path.endsWith('/shipment-exports')) { const ids = url.searchParams.get('ids')?.split(',') ?? []; return reply({ schemaVersion: 'ORGANIZATION_EXPORT_1', exportId: toolsId(120), organizationId: organizationA, generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', piiIncluded: false, rowCount: ids.length, columns: exportColumns, rows: ids.map(id => exportColumns.map(k => k === 'shipmentCode' ? 'AP-SIM-' + id.replaceAll('-', '') : 'synthetic')) }); }
    if (path.endsWith('/shipments')) { const items = [toolsItem(), toolsItem(1)]; return reply(toolsPage(url.searchParams.has('search') ? items.filter(i => (JSON.stringify(i) + i.record.shipment.parcels.map(p => 'AP-P-' + p.id.replaceAll('-', '')).join(' ')).includes(url.searchParams.get('search')!)) : items)); }
    return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
  });
  return { state, calls, drafts, quotes };
}
