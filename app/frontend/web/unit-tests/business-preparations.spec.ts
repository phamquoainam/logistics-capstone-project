import { expect, test } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
import { businessContextsFixture } from '../ui-unit-tests/support/business-fixture';
const context = { ...businessContextsFixture[0], permissions: [...businessContextsFixture[0].permissions, 'shipment.draft.read', 'shipment.draft.write'] as typeof businessContextsFixture[0]['permissions'] };
const account = '10000000-0000-4000-8000-000000000001', target = '10000000-0000-4000-8000-000000000805', key = '10000000-0000-4000-8000-000000000806';
const contact = { name: 'Synthetic contact', phone: '+84901234567', address: { country: 'VN', administrativeArea: 'Synthetic area', locality: 'Synthetic locality', addressLine: 'Synthetic street' } };
const input = { sender: contact, recipient: contact, parcels: [{ description: 'Synthetic parcel', quantity: 1, weightGrams: 500, lengthMm: 100, widthMm: 100, heightMm: 100, declaredValueVnd: 0 }] };
const draft = { id: target, organizationId: context.organizationId, facilityId: null, creatorAccountId: account, state: 'DRAFT' as const, preparationState: 'DRAFT' as const, version: 1, input, sourceChannel: null, sourceOrderReference: null, createdAt: '2026-11-10T01:00:00Z', updatedAt: '2026-11-10T01:00:00Z', allowedActions: ['UPDATE', 'ARCHIVE'] as ('UPDATE' | 'ARCHIVE')[] };
function harness(handle: (path: string, init: RequestInit) => Promise<Response>) {
  const now = Date.now(); return createCustomerClient({ lock: work => work(), now: () => now, fetch: async (path, init = {}) => {
    const value = String(path); if (value.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-csrf' });
    if (value.endsWith('/refresh')) return Response.json({ accountId: account, accessToken: 'synthetic-access', expiresAt: new Date(now + 900000).toISOString() });
    if (value.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 });
    return handle(value, init);
  } });
}
test('organization preparation writes bind route CSRF idempotency and exact version without Personal owner', async () => {
  const client = harness(async (path, init) => {
    expect(path).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/shipment-preparations/' + target);
    expect(init.method).toBe('PUT'); expect(init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-csrf', 'Idempotency-Key': key, 'If-Match': '"v1"', Authorization: 'Bearer synthetic-access' });
    expect(JSON.parse(init.body as string)).toEqual(input);
    return Response.json({ ...draft, version: 2 }, { headers: { 'Cache-Control': 'no-store', ETag: '"v2"' } });
  });
  try { await client.initialize(); expect((await client.saveBusinessPreparation(context, input, key, draft)).version).toBe(2); } finally { client.dispose(); }
});
test('wrong organization response and malformed successful write are never accepted as saved', async () => {
  const client = harness(async () => Response.json({ ...draft, organizationId: businessContextsFixture[1].organizationId }, { headers: { 'Cache-Control': 'no-store', ETag: '"v1"' } }));
  try { await client.initialize(); await expect(client.readBusinessPreparation(context, target)).rejects.toThrow('RESPONSE_INVALID'); await expect(client.saveBusinessPreparation(context, input, key)).rejects.toThrow('OUTCOME_UNKNOWN'); } finally { client.dispose(); }
});
test('unknown preparation write is resolved by scoped receipt without a blind second POST', async () => {
  let posts = 0; const client = harness(async (path, init) => {
    if (init.method === 'POST') { posts++; throw new Error('synthetic-lost-response'); }
    expect(path).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/shipping-preparation-receipts/CREATE/' + key);
    return Response.json({ operationId: 'CREATE', targetId: target, outcome: 'COMMITTED', version: 1 }, { headers: { 'Cache-Control': 'no-store' } });
  });
  try { await client.initialize(); await expect(client.saveBusinessPreparation(context, input, key)).rejects.toThrow('OUTCOME_UNKNOWN'); expect((await client.businessPreparationReceipt(context, 'CREATE', key)).targetId).toBe(target); expect(posts).toBe(1); } finally { client.dispose(); }
});
const importInput = { ...input, reference: null, instructions: null, facilityId: null, sourceChannel: 'CSV', sourceOrderReference: 'SOURCE-1' };
const job = { id: target, organizationId: context.organizationId, facilityId: null, creatorAccountId: account, templateVersion: 'BUSINESS_CSV_V1', state: 'PREVIEW', version: 1, totalRows: 1, validRows: 1, invalidRows: 0, createdRows: 0, conflictRows: 0, createdAt: draft.createdAt, updatedAt: draft.updatedAt, rows: [{ rowNumber: 1, state: 'VALID', input: importInput, errors: [], preparationId: null, version: 1 }], page: 0, size: 20, hasNext: false };
test('CSV preview preserves row facts and cannot claim Draft creation before explicit commit', async () => {
  const client = harness(async (path, init) => {
    expect(path).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/shipment-imports'); expect(init.method).toBe('POST');
    expect(JSON.parse(init.body as string)).toEqual({ templateVersion: 'BUSINESS_CSV_V1', csv: 'synthetic raw CSV' });
    expect(init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-csrf', 'Idempotency-Key': key });
    return Response.json(job, { headers: { 'Cache-Control': 'no-store', ETag: '"v1"' } });
  });
  try { await client.initialize(); const result = await client.previewBusinessImport(context, 'synthetic raw CSV', key); expect(result.createdRows).toBe(0); expect(result.rows[0].preparationId).toBeNull(); expect(result.rows[0].input?.parcels).toEqual(input.parcels); } finally { client.dispose(); }
});
test('CSV response rejects foreign organization and inconsistent aggregate or row creation evidence', async () => {
  let data = { ...job }; const client = harness(async () => Response.json(data, { headers: { 'Cache-Control': 'no-store', ETag: '"v1"' } }));
  try {
    await client.initialize();
    for (const patch of [{ organizationId: businessContextsFixture[1].organizationId }, { totalRows: 2 }, { createdRows: 1 }, { rows: [{ ...job.rows[0], preparationId: key }] }]) {
      data = { ...job, ...patch }; await expect(client.readBusinessImport(context, target)).rejects.toThrow('RESPONSE_INVALID');
    }
    await expect(client.previewBusinessImport(context, 'synthetic raw CSV', key)).rejects.toThrow('OUTCOME_UNKNOWN');
  } finally { client.dispose(); }
});
test('import history binds page and current organization, rejects foreign or duplicate jobs', async () => {
  let items = [job]; const client = harness(async (path, init) => {
    expect(path).toBe('/api/v1/merchant-organizations/' + context.organizationId + '/shipment-imports?page=0&size=20'); expect(init.method).toBe('GET');
    return Response.json({ items, page: 0, size: 20, hasNext: false }, { headers: { 'Cache-Control': 'no-store' } });
  });
  try { await client.initialize(); expect((await client.listBusinessImports(context)).items[0].id).toBe(target); items = [{ ...job, organizationId: businessContextsFixture[1].organizationId }]; await expect(client.listBusinessImports(context)).rejects.toThrow('RESPONSE_INVALID'); items = [job, job]; await expect(client.listBusinessImports(context)).rejects.toThrow('RESPONSE_INVALID'); } finally { client.dispose(); }
});
