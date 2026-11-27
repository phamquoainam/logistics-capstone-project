import { test, expect } from '@playwright/test';
import { businessToolsFixture, toolsContext, toolsDraft, toolsItem, toolsPage, toolsId } from './support/business-tools-fixture';
import type { PilotHandling } from '../src/customer/client';

/** Intercepted presentation regression only; never evidence of Carrier acceptance. */
test('Merchant detail renders current Station event without rewriting Booking or UNPAID', async ({ page, context }) => {
  const api = await businessToolsFixture(context), item = toolsItem(), shipment = item.record.shipment;
  const handling: PilotHandling = { shipmentId: shipment.id, state: 'AT_STATION', version: 1, parcels: shipment.parcels.map(p => ({ parcelId: p.id, parcelNumber: p.parcelNumber, barcode: 'AP-P-' + p.id.replaceAll('-', ''), state: 'AT_STATION' })), events: [{ id: toolsId(500), type: 'CARRIER_ACCEPTED_AT_STATION', stationCode: shipment.quote.route.dropOffCode, occurredAt: shipment.createdAt, provenance: 'SIMULATION' }], labelAvailable: true, simulation: true };
  const data = { ...toolsPage([item]), schemaVersion: 'ORGANIZATION_MANAGEMENT_2', summary: { ...toolsPage([item]).summary, waitingForHandover: '0' }, items: [{ ...item, status: 'AT_STATION', handling }] };
  const base = '/api/v1/merchant-organizations/' + toolsContext.organizationId;
  await context.route('**/api/v1/**', async route => {
    const url = new URL(route.request().url()), path = url.pathname;
    const reply = (body: unknown, etag?: string) => route.fulfill({ contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(etag ? { ETag: etag } : {}) }, body: JSON.stringify(body) });
    if (path === base + '/shipments' && url.searchParams.get('view') === 'management') {
      expect(route.request().headers().accept).toBe('application/vnd.argopost.business-management.v2+json'); return reply(data);
    }
    if (path === base + '/shipments/' + shipment.id) return reply(item.record, '"v1"');
    if (path === base + '/shipment-preparations/' + shipment.preparationId) return reply({ ...toolsDraft(), state: 'BOOKED', version: 2, updatedAt: shipment.createdAt, allowedActions: [] }, '"v2"');
    if (path === base + '/shipments/' + shipment.id + '/internal-history') return reply({ items: [], page: 0, size: 20, hasNext: false });
    return route.fallback();
  });
  await page.goto('/merchant/' + toolsContext.organizationId + '/shipments/' + shipment.id);
  await expect(page.locator('.pb-shipment-heading')).toContainText('Đã tiếp nhận tại Station');
  await expect(page.locator('.pb-shipment-heading')).toContainText('UNPAID');
  await page.getByRole('tab', { name: 'Hành trình', exact: true }).click();
  await expect(page.getByRole('tabpanel')).toContainText('Carrier đã tiếp nhận tại SIM-DROP-A1');
  await expect(page.getByRole('tabpanel')).not.toContainText('Chưa có sự kiện Carrier');
  await page.getByRole('tab', { name: 'Lấy hàng & Giao hàng', exact: true }).click();
  await expect(page.getByRole('tabpanel')).toContainText('Đã có sự kiện tiếp nhận đủ kiện');
  expect(shipment.carrierState).toBe('NOT_ACCEPTED'); expect(item.record.obligation.state).toBe('UNPAID');
  expect(api.calls.filter(c => c.method !== 'GET' && !c.path.endsWith('/refresh') && !c.path.endsWith('/csrf'))).toEqual([]);
});
