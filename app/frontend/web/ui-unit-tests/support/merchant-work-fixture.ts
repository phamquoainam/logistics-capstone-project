import type { BrowserContext } from '@playwright/test';
import type { HandoverBatch, ParcelPreparation } from '../../src/customer/merchant-facility-work-data';
import { merchantFacilityFixture, siteId } from './merchant-facility-fixture';
import { organizationA } from './business-fixture';
const shipmentId = '10000000-0000-4000-8000-000000000031', prepId = '10000000-0000-4000-8000-000000000032', batchId = '10000000-0000-4000-8000-000000000033';
export async function merchantWorkFixture(context: BrowserContext) {
  const api = await merchantFacilityFixture(context);
  api.state.sites[0].allowedActions.push('READ_PREPARATION', 'PREPARE_PARCELS', 'READ_HANDOVER', 'MANAGE_HANDOVER');
  const parcels = [1, 2].map(n => ({ id: '10000000-0000-4000-8000-00000000004' + n, parcelNumber: n, facts: { description: 'Synthetic parcel ' + n, quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 } }));
  const preparation: ParcelPreparation = { id: prepId, organizationId: organizationA, facilityId: siteId, shipmentId, shipmentCode: 'AP-SIM-' + shipmentId.replaceAll('-', ''), state: 'READY_TO_PACK', version: 1, cycle: 1, createdAt: '2026-11-16T00:00:00Z', updatedAt: '2026-11-16T00:00:00Z', parcels, checks: [], allowedActions: ['CHECK', 'PACK'] };
  const batch: HandoverBatch = { id: batchId, organizationId: organizationA, facilityId: siteId, state: 'DRAFT', version: 1, cycle: 1, createdAt: preparation.createdAt, updatedAt: preparation.createdAt, shipments: [{ shipmentId, shipmentCode: preparation.shipmentCode, parcels }], checks: [], history: [{ operation: 'HANDOVER_CREATE', actorAccountId: organizationA, reason: null, occurredAt: preparation.createdAt }], allowedActions: ['ITEMS', 'CHECK', 'READY', 'CANCEL'] };
  const state = { preparation, batch, unknownNext: false, denied: false };
  const writes: { path: string; method: string; body: unknown; expected?: string }[] = [], receipts = new Map<string, object>();
  await context.route('**/api/v1/**', async route => {
    const req = route.request(), url = new URL(req.url()), path = url.pathname, method = req.method();
    const work = path.includes('/shipment-preparation-work') || path.endsWith('/preparation-work'), handover = path.includes('/handover-preparations'), receipt = path.includes('/facility-work-receipts/');
    if (!work && !handover && !receipt) return route.fallback();
    const reply = (body: unknown, status = 200, version?: number) => route.fulfill({ status, contentType: 'application/json', headers: { 'Cache-Control': 'no-store', ...(version ? { ETag: '"v' + version + '"' } : {}) }, body: JSON.stringify(body) });
    if (state.denied) return reply({ code: 'RESOURCE_NOT_FOUND' }, 404);
    if (receipt) { const key = path.split('/').at(-1)!; return receipts.has(key) ? reply(receipts.get(key)) : reply({ code: 'RESOURCE_NOT_FOUND' }, 404); }
    if (method === 'GET') {
      if (path.endsWith('/preparation-work')) return reply({ items: [{ shipmentId, shipmentCode: preparation.shipmentCode, parcelCount: 2, preparation }], page: 0, size: 20, hasNext: false });
      if (path.endsWith('/handover-preparations')) return reply({ items: [batch], page: 0, size: 20, hasNext: false });
      if (path.endsWith('/document')) return route.fulfill({ status: 200, contentType: 'application/pdf', headers: { 'Cache-Control': 'no-store', 'Content-Disposition': 'attachment; filename=synthetic-preparation.pdf' }, body: Buffer.from('%PDF-1.4\nSynthetic UI transport fixture only\n%%EOF') });
      return work ? reply(preparation, 200, preparation.version) : reply(batch, 200, batch.version);
    }
    const body = req.postDataJSON(), target = work ? preparation : batch;
    if (req.headers()['if-match'] !== '"v' + target.version + '"') return reply({ code: 'PRECONDITION_STALE' }, 409);
    if (path.endsWith('/checks')) {
      const result = work ? body.condition : body.result;
      target.checks = [...target.checks.filter(c => c.parcelId !== body.parcelId), { parcelId: body.parcelId, result, note: body.note }];
    } else if (path.endsWith('/transitions')) {
      if (body.action === 'PACK') { if (preparation.checks.length !== 2 || preparation.checks.some(c => c.result !== 'OK')) return reply({ code: 'WORK_PARCELS_INCOMPLETE' }, 409); preparation.state = 'PACKED'; preparation.allowedActions = ['READY', 'REOPEN']; }
      if (body.action === 'READY') {
        if (work) { preparation.state = 'READY_TO_HANDOVER'; preparation.allowedActions = ['REOPEN']; }
        else { if (preparation.state !== 'READY_TO_HANDOVER' || batch.checks.length !== 2 || batch.checks.some(c => c.result !== 'PRESENT')) return reply({ code: 'WORK_PARCELS_INCOMPLETE' }, 409); batch.state = 'READY'; batch.allowedActions = ['ITEMS', 'REOPEN', 'CANCEL']; }
      }
      if (body.action === 'REOPEN') { if (work) { preparation.state = 'READY_TO_PACK'; preparation.allowedActions = ['CHECK', 'PACK']; } else { batch.state = 'DRAFT'; batch.allowedActions = ['ITEMS', 'CHECK', 'READY', 'CANCEL']; } target.checks = []; target.cycle++; }
      if (body.action === 'CANCEL' && !work) { batch.state = 'CANCELLED'; batch.allowedActions = []; }
    }
    const action = path.endsWith('/checks') ? 'CHECK' : body.action; target.version++;
    const operation = (work ? 'PREPARATION_' : 'HANDOVER_') + action;
    if (!work) batch.history.push({ operation, actorAccountId: organizationA, reason: body.reason ?? null, occurredAt: batch.updatedAt });
    writes.push({ path, method, body, expected: req.headers()['if-match'] }); receipts.set(req.headers()['idempotency-key'], { operationId: operation, targetId: target.id, version: target.version, outcome: 'COMMITTED' });
    if (state.unknownNext) { state.unknownNext = false; return route.abort('connectionfailed'); } return reply(target, 200, target.version);
  });
  return { ...api, work: state, workWrites: writes };
}
