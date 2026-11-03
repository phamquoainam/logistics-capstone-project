import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';

const owner = '10000000-0000-4000-8000-000000000001', draftId = '10000000-0000-4000-8000-000000000002', quoteId = '10000000-0000-4000-8000-000000000003', shipmentId = '10000000-0000-4000-8000-000000000004', key = '10000000-0000-4000-8000-000000000010';
const now = Date.parse('2026-11-02T00:00:00Z');
const contact = { name: 'Synthetic contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
const parcel = { description: 'Ordinary synthetic box', quantity: 1, weightGrams: 501, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 };
const quote = { id: quoteId, accountId: owner, draftId, draftVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', route: { datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA', dropOffCode: 'SIM-DROP-A1' }, parcels: [{ weightGrams: 501, lengthMm: 100, widthMm: 100, heightMm: 100 }], feeLines: [{ parcelNumber: 1, actualGrams: 501, chargeableGrams: 1000, feeVnd: 28000 }], totalVnd: 28000, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE', createdAt: new Date(now).toISOString(), expiresAt: new Date(now + 900000).toISOString(), consumedBy: null as string | null };
const booked = { simulation: true, shipment: { id: shipmentId, accountId: owner, code: 'AP-SIM-' + shipmentId.replaceAll('-', ''), draftId, draftVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input: { reference: null, instructions: null, sender: contact, recipient: contact, parcels: [parcel] }, quote: { ...quote, consumedBy: shipmentId }, totalVnd: 28000, createdAt: new Date(now).toISOString(), parcels: [{ id: key, parcelNumber: 1, facts: parcel }] }, obligation: { id: key, accountId: owner, shipmentId, quoteId, amountVnd: 28000, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-LOCAL-PILOT-01', createdAt: new Date(now).toISOString() } };
const management = { schemaVersion: 'PERSONAL_MANAGEMENT_1', generatedAt: new Date(now).toISOString(), timeZone: 'Asia/Ho_Chi_Minh', summary: { totalShipments: '1', waitingForHandover: '1', atStation: '0' }, total: '1', page: 0, size: 20, hasNext: false, items: [{ record: booked, status: 'WAITING_FOR_HANDOVER' }] };
function fixture(handler: (path: string, init: RequestInit) => Response | Promise<Response>) {
  const calls: { path: string; init: RequestInit }[] = [];
  const fetcher: typeof fetch = async (input, init = {}) => { const path = String(input); calls.push({ path, init }); if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-challenge' }); if (path.endsWith('/refresh')) return Response.json({ accountId: owner, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() }); if (path.endsWith('/me')) return Response.json({ accountId: owner, displayName: 'Synthetic Customer', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 }); return handler(path, init); };
  return { client: createCustomerClient({ fetch: fetcher, lock: work => work(), now: () => now }), calls };
}
test('management read preserves query intent and never sends owner or mutation authority', async () => {
  const f = fixture(() => Response.json(management)); await f.client.initialize(); const result = await f.client.listPersonalManagement(new URLSearchParams({ recipient: 'Synthetic contact', obligation: 'UNPAID' })); expect(result.total).toBe('1'); const call = f.calls.at(-1)!; expect(call.init.method).toBe('GET'); expect(new URL(call.path, 'https://example.invalid').searchParams.get('view')).toBe('management'); expect(call.init.body).toBeUndefined();
});
test('management decoder rejects inconsistent KPI money ownership or page results', async () => {
  for (const value of [{ ...management, summary: { ...management.summary, totalShipments: '2' } }, { ...management, hasNext: true }, { ...management, total: '2' }, { ...management, items: [{ ...management.items[0], status: 'DELIVERED' }] }, { ...management, items: [{ ...management.items[0], record: { ...booked, shipment: { ...booked.shipment, accountId: key } } }] }]) {
    const f = fixture(() => Response.json(value)); await f.client.initialize(); await expect(f.client.listPersonalManagement(new URLSearchParams())).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
  }
});
test('management rejects duplicate and foreign owner selectors before network access', async () => {
  const f = fixture(() => Response.json(management)); await f.client.initialize(); const before = f.calls.length;
  for (const query of ['ownerId=foreign', 'status=AT_STATION&status=WAITING_FOR_HANDOVER', 'organizationId=foreign']) await expect(f.client.listPersonalManagement(new URLSearchParams(query))).rejects.toMatchObject({ code: 'VALIDATION_FAILED' }); expect(f.calls.length).toBe(before);
});
test('pilot requests send scope-safe intent and version/key headers without prices or owner', async () => {
  const f = fixture(() => Response.json({ quote, usable: true, serverTime: new Date(now).toISOString() }, { status: 201 })); await f.client.initialize(); expect((await f.client.createPilotQuote(draftId, 1, 'SIM-A1', 'SIM-A2', true, key)).quote.totalVnd).toBe(28000); const call = f.calls.find(v => v.path.endsWith('/quotes'))!; expect(JSON.parse(String(call.init.body))).toEqual({ originCode: 'SIM-A1', destinationCode: 'SIM-A2', ordinaryGoodsConfirmed: true }); expect(call.init.headers).toMatchObject({ Authorization: 'Bearer synthetic-access', 'X-CSRF-Token': 'synthetic-challenge', 'If-Match': '"v1"', 'Idempotency-Key': key });
});
test('pilot quote rejects foreign owners fee mismatch and incorrect server binding', async () => {
  for (const invalid of [{ ...quote, accountId: draftId }, { ...quote, totalVnd: 1 }, { ...quote, feeLines: [{ ...quote.feeLines[0], feeVnd: 0.5 }] }]) { const f = fixture(() => Response.json({ quote: invalid, usable: true, serverTime: new Date(now).toISOString() })); await f.client.initialize(); await expect(f.client.readPilotQuote(quoteId)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' }); }
});
test('pilot shipment accepts only matched immutable UNPAID obligation and no carrier acceptance', async () => {
  const f = fixture(() => Response.json(booked, { headers: { ETag: '"v1"' } })); await f.client.initialize(); expect((await f.client.readPilotShipment(shipmentId)).obligation.state).toBe('UNPAID'); for (const invalid of [{ ...booked, obligation: { ...booked.obligation, state: 'PAID' } }, { ...booked, obligation: { ...booked.obligation, amountVnd: 1 } }, { ...booked, shipment: { ...booked.shipment, carrierState: 'DELIVERED' } }, { ...booked, simulation: false }]) { const bad = fixture(() => Response.json(invalid, { headers: { ETag: '"v1"' } })); await bad.client.initialize(); await expect(bad.client.readPilotShipment(shipmentId)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' }); }
});
test('pilot unknown booking never retries POST and receipt does not fabricate payment', async () => {
  let writes = 0; const f = fixture((_path, init) => { if (init.method === 'POST') { writes++; throw new Error('synthetic lost reply'); } return Response.json({ targetId: shipmentId, outcome: 'COMMITTED' }); }); await f.client.initialize(); await expect(f.client.confirmPilotBooking(draftId, 1, quoteId, key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect(await f.client.pilotBookingReceipt(key)).toEqual({ targetId: shipmentId }); expect(writes).toBe(1);
});
