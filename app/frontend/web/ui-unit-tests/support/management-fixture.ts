import type { ManagementPage, PilotShipmentView } from '../../src/customer/client';

/** Isolated synthetic UI transport only; never a seed or PostgreSQL acceptance source. */
export function managementFixture(p = new URLSearchParams(), count = 0, records?: PilotShipmentView[]): ManagementPage {
  const owner = '10000000-0000-4000-8000-000000000001';
  const source = records ?? Array.from({ length: count }, (_, i): PilotShipmentView => {
    const id = '20000000-0000-4000-8000-' + String(i + 1).padStart(12, '0'), quoteId = '30000000-0000-4000-8000-' + String(i + 1).padStart(12, '0'), parcelId = '40000000-0000-4000-8000-' + String(i + 1).padStart(12, '0');
    const contact = { name: 'Synthetic contact ' + i, phone: '+84901234567', address: { country: 'VN' as const, administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
    const facts = { description: 'Synthetic ordinary goods', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }, fee = 25000 + i * 1000, createdAt = '2026-11-0' + (i % 3 + 1) + 'T01:00:00Z';
    const quote = { id: quoteId, accountId: owner, draftId: id, draftVersion: 1, tariffVersion: 'ARGO-ROAD-TARIFF-1', route: { datasetVersion: 'ARGO-SIM-COVERAGE-1', originCode: 'SIM-A1', destinationCode: 'SIM-A2', classification: 'INTRA' as const, dropOffCode: 'SIM-DROP-A1' }, parcels: [{ weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100 }], feeLines: [{ parcelNumber: 1, actualGrams: 500, chargeableGrams: 500, feeVnd: fee }], totalVnd: fee, taxTreatment: 'SIMULATION_NOT_TAX_INVOICE' as const, createdAt, expiresAt: createdAt.replace('01:00:', '01:15:'), consumedBy: id };
    return { simulation: true, shipment: { id, accountId: owner, code: 'AP-SIM-' + id.replaceAll('-', ''), draftId: id, draftVersion: 1, bookingState: 'CONFIRMED', carrierState: 'NOT_ACCEPTED', input: { reference: 'Synthetic reference ' + i, instructions: null, sender: contact, recipient: contact, parcels: [facts] }, quote, totalVnd: fee, createdAt, parcels: [{ id: parcelId, parcelNumber: 1, facts }] }, obligation: { id: parcelId, accountId: owner, shipmentId: id, quoteId, amountVnd: fee, currency: 'VND', state: 'UNPAID', payer: 'SENDER', method: 'CASH_ON_HANDOVER', policyVersion: 'BR13-LOCAL-PILOT-01', createdAt } };
  });
  const base = source.map((record, i) => ({ record, status: (i % 2 ? 'AT_STATION' : 'WAITING_FOR_HANDOVER') as ManagementPage['items'][number]['status'] })).filter(({ record: { shipment: s } }) => {
    const search = p.get('search')?.toLowerCase() ?? '', recipient = p.get('recipient')?.toLowerCase() ?? '';
    return (!search || [s.code, s.input.reference ?? '', s.input.sender.name, s.input.sender.phone, s.input.recipient.name, s.input.recipient.phone].join(' ').toLowerCase().includes(search)) && (!recipient || (s.input.recipient.name + ' ' + s.input.recipient.phone).toLowerCase().includes(recipient)) && (!p.has('from') || s.createdAt >= p.get('from')!) && (!p.has('until') || s.createdAt < p.get('until')!) && (!p.has('origin') || s.quote.route.originCode === p.get('origin') && s.quote.route.destinationCode === p.get('destination'));
  });
  const selected = base.filter(i => !p.has('status') || i.status === p.get('status'));
  selected.sort((a, b) => {
    const result = p.get('sort') === 'FEE_DESC' ? b.record.shipment.totalVnd - a.record.shipment.totalVnd : p.get('sort') === 'FEE_ASC' ? a.record.shipment.totalVnd - b.record.shipment.totalVnd : (p.get('sort') === 'OLDEST' ? 1 : -1) * a.record.shipment.createdAt.localeCompare(b.record.shipment.createdAt);
    return result || a.record.shipment.id.localeCompare(b.record.shipment.id);
  });
  const page = Number(p.get('page') ?? 0);
  return { schemaVersion: 'PERSONAL_MANAGEMENT_1', generatedAt: new Date().toISOString(), timeZone: 'Asia/Ho_Chi_Minh', summary: { totalShipments: String(base.length), waitingForHandover: String(base.filter(i => i.status === 'WAITING_FOR_HANDOVER').length), atStation: String(base.filter(i => i.status === 'AT_STATION').length) }, total: String(selected.length), page, size: 20, hasNext: (page + 1) * 20 < selected.length, items: selected.slice(page * 20, (page + 1) * 20) };
}
