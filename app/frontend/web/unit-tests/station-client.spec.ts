import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
const actor = '10000000-0000-4000-8000-000000000001', shipment = '10000000-0000-4000-8000-000000000002', parcel = '10000000-0000-4000-8000-000000000003', key = '10000000-0000-4000-8000-000000000004';
const now = Date.parse('2026-11-02T00:00:00Z'), station = 'SIM-DROP-A1';
const handling = { shipmentId: shipment, state: 'AT_STATION', version: 1, parcels: [{ parcelId: parcel, parcelNumber: 1, barcode: 'AP-P-' + parcel.replaceAll('-', ''), state: 'AT_STATION' }], events: [{ id: key, type: 'CARRIER_ACCEPTED_AT_STATION', stationCode: station, occurredAt: new Date(now).toISOString(), provenance: 'SIMULATION' }], labelAvailable: true, simulation: true };
function fixture(work: (path: string, init: RequestInit) => Response | Promise<Response>) {
  const calls: { path: string; init: RequestInit }[] = [];
  const fetcher: typeof fetch = async (input, init = {}) => { const path = String(input);calls.push({ path, init });if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-challenge' });if (path.endsWith('/refresh')) return Response.json({ accountId: actor, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() });if (path.endsWith('/me')) return Response.json({ accountId: actor, displayName: 'Synthetic Station User', verifiedEmail: 'station@example.invalid', securityVersion: 1 });return work(path, init); };
  return { client: createCustomerClient({ fetch: fetcher, lock: work => work(), now: () => now }), calls };
}
test('station permission denial does not infer a grant from an authenticated personal session', async () => {
  const f = fixture(() => Response.json({ code: 'AUTHORITY_REQUIRED' }, { status: 403 }));await f.client.initialize();await expect(f.client.listStations()).rejects.toMatchObject({ status: 403, code: 'AUTHORITY_REQUIRED' });expect(f.client.getSnapshot().status).toBe('authenticated');expect(f.calls.filter(v => v.path.includes('station-operations') && v.init.method === 'POST')).toHaveLength(0);
});
test('operational projection rejects fake delivered states wrong identifiers and inconsistent parcel aggregation', async () => {
  const good = fixture(() => Response.json(handling));await good.client.initialize();expect((await good.client.readPilotHandling(shipment)).state).toBe('AT_STATION');
  for (const invalid of [{ ...handling, state: 'DELIVERED' }, { ...handling, shipmentId: actor }, { ...handling, events: [] }, { ...handling, parcels: [{ ...handling.parcels[0], barcode: 'secret@example.invalid' }] }]) { const f = fixture(() => Response.json(invalid));await f.client.initialize();await expect(f.client.readPilotHandling(shipment)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' }); }
});
test('private label download validates the PDF and does not send credentials through a URL', async () => {
  const f = fixture(() => new Response('%PDF-1.7\nsynthetic unit bytes', { headers: { 'Content-Type': 'application/pdf' } }));await f.client.initialize();expect((await f.client.downloadPilotLabel(shipment, station)).type).toBe('application/pdf');const sent = f.calls.find(v => v.path.endsWith('/labels.pdf'))!;expect(sent.path).toBe('/api/v1/station-operations/stations/' + station + '/shipments/' + shipment + '/labels.pdf');expect(sent.init.headers).toEqual({ Authorization: 'Bearer synthetic-access' });
  const invalid = fixture(() => new Response('<html>not a PDF</html>', { headers: { 'Content-Type': 'application/pdf' } }));await invalid.client.initialize();await expect(invalid.client.downloadPilotLabel(shipment)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
});
test('unknown station acceptance uses the original key and receipt without POST retries or payment authority', async () => {
  const f = fixture((_path, init) => { if (init.method === 'POST') throw new Error('synthetic lost reply');return Response.json({ operation: 'ACCEPT', targetId: key, shipmentId: shipment, outcome: 'COMMITTED' }); });await f.client.initialize();await expect(f.client.acceptStationShipment(station, shipment, [parcel], 0, key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' });expect(await f.client.stationReceipt(station, key)).toMatchObject({ operation: 'ACCEPT', shipmentId: shipment });const writes = f.calls.filter(v => v.path.endsWith('/acceptance'));expect(writes).toHaveLength(1);expect(JSON.parse(String(writes[0].init.body))).toEqual({ scanIds: [parcel], inspection: 'MATCHED' });expect(writes[0].init.headers).toMatchObject({ 'Idempotency-Key': key, 'If-Match': '"i0"', 'X-CSRF-Token': 'synthetic-challenge' });
});
