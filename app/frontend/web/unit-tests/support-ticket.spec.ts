import { test, expect } from '@playwright/test';
import { createCustomerClient } from '../src/customer/client';
import { supportDetail, supportPage, supportReceipt, supportSummary } from '../src/customer/support-ticket-data';
import { supportDetailFixture, supportId, supportSummaryFixture } from '../ui-unit-tests/support/support-ticket-fixture';
const key = '10000000-0000-4000-8000-000000000099', account = '10000000-0000-4000-8000-000000000001';
test('Ticket parser preserves exact flat contract, Unicode paragraphs and supported state authority', () => {
  const wire = supportDetailFixture(); wire.publicMessages[0].message = 'Tiếng Việt\nDòng tiếp theo <script>không phải HTML</script>';
  const data = supportDetail(wire); expect(data.ticket).toEqual(supportSummaryFixture); expect(data.publicMessages[0].message).toContain('\n');
  expect(supportPage({ items: [supportSummaryFixture], page: 0, size: 20, hasNext: false }, 0).items).toHaveLength(1);
  for (const value of [{ ...wire, ownerId: account }, { ticket: supportSummaryFixture, publicMessages: wire.publicMessages, allowedActions: [] }, { ...wire, allowedActions: ['RESOLVE'] }, { ...wire, version: 2 }, { ...wire, publicMessages: [] }]) expect(() => supportDetail(value)).toThrow();
  expect(() => supportSummary({ ...supportSummaryFixture, state: 'constructor' })).toThrow(); expect(() => supportSummary({ ...supportSummaryFixture, responseDueAt: '2026-11-01T02:00:00Z' })).toThrow();
});
test('Ticket parser rejects mismatched kind, hidden staff data, duplicate messages and false receipt binding', () => {
  expect(() => supportSummary({ ...supportSummaryFixture, kind: 'SHIPMENT' })).toThrow(); expect(() => supportSummary({ ...supportSummaryFixture, category: 'SHIPMENT_TRACE' })).toThrow();
  const v = supportDetailFixture(); expect(() => supportDetail({ ...v, publicMessages: [{ ...v.publicMessages[0], actorId: account }] })).toThrow();
  expect(() => supportDetail({ ...v, version: 2, publicMessages: [v.publicMessages[0], v.publicMessages[0]] })).toThrow();
  expect(() => supportReceipt({ operationId: 'CREATE', targetId: supportId, outcome: 'COMMITTED', version: 1 }, 'REPLY')).toThrow();
  expect(() => supportPage({ items: [supportSummaryFixture], page: 0, size: 20, hasNext: true }, 0)).toThrow();
});
function client(handler: (path: string, init: RequestInit) => Promise<Response>) {
  return createCustomerClient({ lock: work => work(), now: () => Date.now(), fetch: async (input, init = {}) => {
    const path = String(input); if (path.endsWith('/csrf')) return Response.json({ challenge: 'synthetic-support-csrf' }); if (path.endsWith('/refresh')) return Response.json({ accountId: account, accessToken: 'synthetic-support-access', expiresAt: new Date(Date.now() + 900000).toISOString() }); if (path.endsWith('/me')) return Response.json({ accountId: account, displayName: 'Synthetic', verifiedEmail: 'synthetic@example.invalid', securityVersion: 1 }); if (path.endsWith('/logout')) return new Response(null, { status: 204 }); expect(init.headers).toMatchObject({ Authorization: 'Bearer synthetic-support-access' }); return handler(path, init);
  } });
}
test('Ticket transport sends existing Bearer/CSRF/ETag/key, resolves a lost write without repeating POST', async () => {
  let posts = 0; const c = client(async (path, init) => { if (init.method === 'POST') { posts++; expect(init.headers).toMatchObject({ 'X-CSRF-Token': 'synthetic-support-csrf', 'If-Match': '"v1"', 'Idempotency-Key': key }); throw new Error('synthetic loss'); } expect(path).toContain('support-receipts/REPLY/' + key); return Response.json({ operationId: 'REPLY', targetId: supportId, outcome: 'COMMITTED', version: 2 }); });
  await c.initialize(); await expect(c.commandSupportTicket(supportId, 'REPLY', 'Synthetic reply', 1, key)).rejects.toMatchObject({ code: 'OUTCOME_UNKNOWN' }); expect((await c.readSupportReceipt('REPLY', key)).targetId).toBe(supportId); expect(posts).toBe(1);
});
test('Ticket transport rejects wrong target and stale logged-out generation without disclosing a thread', async () => {
  const wrong = client(async () => Response.json(supportDetailFixture())); await wrong.initialize(); await expect(wrong.readSupportTicket(key)).rejects.toMatchObject({ code: 'RESPONSE_INVALID' });
  let release!: (response: Response) => void, started!: () => void; const pending = new Promise<Response>(r => { release = r; }), called = new Promise<void>(r => { started = r; });
  const c = client(async () => { started(); return pending; }); await c.initialize(); const request = c.readSupportTicket(supportId), rejected = expect(request).rejects.toMatchObject({ code: 'SESSION_CHANGED' }); await called; await c.logout(); release(Response.json(supportDetailFixture())); await rejected;
});
