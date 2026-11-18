import { test, expect } from '@playwright/test';
import { businessLookupInputs, lookupBusinessShipment, quoteMatchesDraft, remainingBusinessQuoteSeconds } from '../src/customer/business-shipping-tools';
import { toolsItem, toolsPage, toolsDraft, toolsQuote } from '../ui-unit-tests/support/business-tools-fixture';

test('business lookup supports exact Shipment Parcel and spaced source references with bounded deduplication', () => {
  const s = toolsItem().record.shipment.code;
  expect(businessLookupInputs(s.toUpperCase() + ',' + s, 'SHIPMENT')).toEqual([s]);
  expect(businessLookupInputs('ORDER with spaces;ORDER with spaces\nOTHER', 'REFERENCE')).toEqual(['ORDER with spaces', 'OTHER']);
  for (const value of ['', 'AP-SIM-1', 'x'.repeat(2049)]) expect(() => businessLookupInputs(value, 'SHIPMENT')).toThrow();
  expect(() => businessLookupInputs(Array.from({ length: 11 }, (_, n) => String(n)).join('\n'), 'REFERENCE')).toThrow();
  expect(() => businessLookupInputs('bad\u0000ref', 'REFERENCE')).toThrow();
});
test('lookup excludes substring collateral and binds Parcel and source reference rather than first result', async () => {
  const a = toolsItem(), b = toolsItem(1), page = toolsPage([a, b]);
  expect(await lookupBusinessShipment(b.record.shipment.code, 'SHIPMENT', async () => page, () => true)).toEqual([b]);
  expect(await lookupBusinessShipment('AP-P-' + b.record.shipment.parcels[0].id.replaceAll('-', ''), 'PARCEL', async () => page, () => true)).toEqual([b]);
  expect(await lookupBusinessShipment('SOURCE-1', 'REFERENCE', async () => page, () => true)).toEqual([b]);
  expect(await lookupBusinessShipment('SOURCE', 'REFERENCE', async () => page, () => true)).toEqual([]);
});
test('scan cancellation failure repetition and exhaustion never become a not-found response', async () => {
  await expect(lookupBusinessShipment('missing', 'REFERENCE', async () => { throw new Error('denied'); }, () => true)).rejects.toThrow('denied');
  await expect(lookupBusinessShipment('missing', 'REFERENCE', async () => toolsPage(), () => false)).rejects.toThrow('CANCELLED');
  await expect(lookupBusinessShipment('missing', 'REFERENCE', async () => ({ ...toolsPage(), hasNext: true }), () => true)).rejects.toThrow('CHANGED');
  let reads = 0; await expect(lookupBusinessShipment('missing', 'REFERENCE', async () => { reads++; return { ...toolsPage([]), hasNext: true }; }, () => true)).rejects.toThrow('LIMIT'); expect(reads).toBe(10);
});
test('quote handoff binds actual organization draft version facility and every measured Parcel', () => {
  const d = toolsDraft(), q = toolsQuote(d); expect(quoteMatchesDraft(q, d)).toBe(true);
  for (const patch of [{ organizationId: toolsDraft(1).id }, { version: 2 }, { facilityId: toolsDraft(1).id }, { id: toolsDraft(1).id }, { input: { ...d.input, parcels: [{ ...d.input.parcels[0], weightGrams: 1000 }] } }]) expect(quoteMatchesDraft(q, { ...d, ...patch })).toBe(false);
});
test('fractional performance-clock rounding cannot show longer than the server Quote TTL', () => {
  const q = toolsQuote(); expect(remainingBusinessQuoteSeconds(q, 900000.0000000001, 0)).toBe(900);
  expect(remainingBusinessQuoteSeconds(q, 900000, 1500)).toBe(899); expect(remainingBusinessQuoteSeconds(q, 900000, 900001)).toBe(0);
  expect(remainingBusinessQuoteSeconds({ ...q, serverTime: q.quote.expiresAt }, 900000, 0)).toBe(0);
});
test('a shared reference with more than twenty exact matches asks to narrow rather than rendering or exporting a partial set', async () => {
  const items = Array.from({ length: 21 }, (_, n) => { const i = toolsItem(n); i.record.shipment.input.reference = 'SHARED'; return i; });
  await expect(lookupBusinessShipment('SHARED', 'REFERENCE', async p => p.get('page') === '0' ? { ...toolsPage(items.slice(0, 20)), hasNext: true } : toolsPage(items.slice(20)), () => true)).rejects.toThrow('LOOKUP_LIMIT');
});
