import { test, expect } from '@playwright/test';
import { parseOrganizationPage } from '../src/customer/business-management-data';
import type { BusinessShipmentView, PilotHandling } from '../src/customer/client';
import { customerClient } from '../src/customer/client';
import { toolsItem } from '../ui-unit-tests/support/business-tools-fixture';

const org = '10000000-0000-4000-8000-000000000001', id = '10000000-0000-4000-8000-000000000002', parcel = '10000000-0000-4000-8000-000000000003';
// This parser unit isolates the already separately validated immutable Booking record.
const record = { shipment: { id, organizationId: org, facilityId: null, parcels: [{ id: parcel }], quote: { route: { dropOffCode: 'SIM-DROP-A1' } } } } as unknown as BusinessShipmentView;
function page() {
  const handling: PilotHandling = { shipmentId: id, state: 'AT_STATION', version: 1, parcels: [{ parcelId: parcel, parcelNumber: 1, barcode: 'AP-P-' + parcel.replaceAll('-', ''), state: 'AT_STATION' }], events: [{ id: org, type: 'CARRIER_ACCEPTED_AT_STATION', stationCode: 'SIM-DROP-A1', occurredAt: '2026-11-27T00:01:00Z', provenance: 'SIMULATION' }], labelAvailable: true, simulation: true };
  return { schemaVersion: 'ORGANIZATION_MANAGEMENT_2', generatedAt: '2026-11-27T00:02:00Z', timeZone: 'Asia/Ho_Chi_Minh', summary: { bookedShipments: '1', waitingForHandover: '0', unassigned: '1', confirmedFeesVnd: '25000', unpaidFeesVnd: '25000' }, total: '1', page: 0, size: 20, hasNext: false, items: [{ record, status: 'AT_STATION', handling, source: { sourceChannel: null, sourceOrderReference: null, facilityId: null, importJobIds: [] }, assignment: { shipmentId: id, organizationId: org, assigneeAccountId: null, version: 0, updatedAt: null }, allowedActions: ['READ_LABEL'] }] };
}
const read = (value: unknown) => parseOrganizationPage(value, org, 0, () => record, raw => raw as PilotHandling);

test('Business accepted event remains distinct from immutable Booking state and UNPAID', () => {
  const result = read(page()); expect(result.items[0].status).toBe('AT_STATION'); expect(result.summary.waitingForHandover).toBe('0'); expect(result.summary.unpaidFeesVnd).toBe('25000');
});
test('Business tracking must bind actual Parcel set and displayed current state', () => {
  const mismatch = page(); mismatch.items[0].handling.parcels[0].parcelId = org; expect(() => read(mismatch)).toThrow();
  const stale = page(); stale.items[0].status = 'WAITING_FOR_HANDOVER'; expect(() => read(stale)).toThrow();
  const foreign = page(); foreign.items[0].handling.shipmentId = org; expect(() => read(foreign)).toThrow();
});
test('Business waiting counters and current source metadata fail closed', () => {
  const inflated = page(); inflated.summary.waitingForHandover = '2'; expect(() => read(inflated)).toThrow();
  const missing = page() as unknown as { items: { handling?: PilotHandling }[] }; delete missing.items[0].handling; expect(() => read(missing)).toThrow();
});

function carrierView() {
  const s = toolsItem().record.shipment, p = s.parcels[0];
  return { cargo: { shipmentId: s.id, shipmentCode: s.code, ownerType: 'BUSINESS', accountId: null as string | null, organizationId: s.organizationId as string | null, facilityId: s.facilityId, quoteId: s.quote.id, totalVnd: s.totalVnd, createdAt: s.createdAt, input: s.input, route: s.quote.route, parcels: s.parcels, handling: { shipmentId: s.id, state: 'WAITING_FOR_HANDOVER', version: 0, parcels: [{ parcelId: p.id, parcelNumber: 1, barcode: 'AP-P-' + p.id.replaceAll('-', ''), state: 'NOT_ACCEPTED' }], events: [], labelAvailable: true, simulation: true } }, scans: [] as { id: string; shipmentId: string; parcelId: string; barcode: string; stationCode: string; scannedAt: string }[], paymentState: 'UNPAID', simulation: true, policyVersion: 'BR13-BUSINESS-INTAKE-01' };
}
test('OPS V2 preserves canonical Business ownership and actual Booking time', () => {
  const v = carrierView();expect(customerClient.parseCarrierStationView(v, 'SIM-DROP-A1').cargo.createdAt).toBe(v.cargo.createdAt);
  const p = carrierView();p.cargo.ownerType = 'PERSONAL';p.cargo.accountId = p.cargo.shipmentId;p.cargo.organizationId = null;p.policyVersion = 'BR13-LOCAL-INTAKE-01';expect(customerClient.parseCarrierStationView(p, 'SIM-DROP-A1').cargo.ownerType).toBe('PERSONAL');
});
test('OPS V2 rejects missing/forged timestamp ownership and wrong Parcel facts', () => {
  const missing = carrierView();missing.cargo.createdAt = '';expect(() => customerClient.parseCarrierStationView(missing, 'SIM-DROP-A1')).toThrow();
  const owner = carrierView();owner.cargo.accountId = owner.cargo.shipmentId;expect(() => customerClient.parseCarrierStationView(owner, 'SIM-DROP-A1')).toThrow();
  const facts = carrierView();facts.cargo.parcels[0].facts = { ...facts.cargo.parcels[0].facts, weightGrams: 501 };expect(() => customerClient.parseCarrierStationView(facts, 'SIM-DROP-A1')).toThrow();
  const old = carrierView(), p = old.cargo.handling.parcels[0];old.scans.push({ id: old.cargo.shipmentId, shipmentId: old.cargo.shipmentId, parcelId: p.parcelId, barcode: p.barcode, stationCode: 'SIM-DROP-A1', scannedAt: '2020-01-01T00:00:00Z' });expect(() => customerClient.parseCarrierStationView(old, 'SIM-DROP-A1')).toThrow();
});
