import { test, after } from 'node:test';
import { randomUUID, createHmac } from 'node:crypto';
import { wire, context, contract, operations, ok, equal, sql, counts, cases, saveReport } from './wire-lib.mjs';

const actors = {}, tokens = {};
let shipment;
const draft = { ...contract.components.schemas.DraftInput.examples[0], reference: 'WIRE-CANONICAL' };
function scenario(name, fn) {
  test(name, { timeout: 30_000 }, async () => {
    try { await fn(); cases.push({ name, status: 'PASS' }); }
    catch (error) { cases.push({ name, status: 'FAIL' }); throw error; }
  });
}
after(saveReport);

scenario('real PostgreSQL18 migrations/roles are isolated before HTTP writes', () => {
  ok(context.postgresVersion.startsWith('18.'), 'Use actual PostgreSQL18');
  equal(context.migrations, '1,2', 'Cold DB must apply existing V1/V2');
  equal(sql('SELECT current_database();'), context.database, 'SQL must reach owned test DB');
  equal(counts(), { shipments: 0, events: 0, receipts: 0 }, 'Fresh wire database starts without business data');
  equal(sql("SELECT count(*) FROM pg_roles WHERE rolname LIKE 'elogistics_wire_%_runtime' AND NOT rolsuper AND NOT rolcreatedb AND NOT rolcreaterole;"), '1', 'Runtime role is unprivileged');
});

scenario('GET health returns existing constant JSON', async () => {
  equal((await wire('getDemoHealth')).data, { status: 'UP', scope: 'local-demo' }, 'Existing health body is constant, not readiness');
});

scenario('actual logins return typed JWT response for four enabled synthetic roles', async () => {
  for (const [name, username] of Object.entries({ a: 'demo.merchant.a', b: 'demo.merchant.b', admin: 'demo.admin', operator: 'demo.operator' })) {
    const result = await wire('loginDemo', { body: { username, password: context.password } });
    actors[name] = result.data.user; tokens[name] = result.data.accessToken;
    ok(typeof tokens[name] === 'string' && tokens[name].split('.').length === 3, 'Server must issue an actual JWT');
    const claims = JSON.parse(Buffer.from(tokens[name].split('.')[1], 'base64url').toString());
    equal(claims.iss, 'elogistics-demo', 'JWT issuer must match source');
    equal(claims.sub, actors[name].id, 'JWT subject must be the server account');
    equal(claims.exp - claims.iat, 900, 'JWT lifetime must remain900 seconds');
  }
});

scenario('GET auth/me preserves enabled account and nullable merchant scope', async () => {
  for (const name of ['a', 'b', 'admin', 'operator']) equal((await wire('getDemoCurrentUser', { token: tokens[name] })).data, actors[name], 'Current account is loaded server-side');
  equal(actors.admin.merchantId, null, 'Admin nullable merchantId must be serialized');
  ok(actors.a.merchantId !== actors.b.merchantId, 'Merchant fixtures have distinct scopes');
});

scenario('GET merchant/me returns the authenticated merchant profile', async () => {
  for (const name of ['a', 'b']) equal((await wire('getDemoCurrentMerchant', { token: tokens[name] })).data.id, actors[name].merchantId, 'Merchant is derived from trusted principal');
});

scenario('GET shipments starts with an empty owner-scoped array', async () => {
  equal((await wire('listDemoShipments', { token: tokens.a })).data, [], 'List is a JSON array with no envelope');
});

scenario('POST shipments creates one DRAFT and Location using runtime PostgreSQL', async () => {
  shipment = (await wire('createDemoShipment', { token: tokens.a, key: 'wire.create.001', body: draft })).data;
  equal(shipment.status, 'DRAFT', 'New state remains DRAFT');
  equal(shipment.merchantId, actors.a.merchantId, 'Owner is trusted scope');
  equal(shipment.createdBy, actors.a.id, 'Actor is trusted account');
  equal(counts(), { shipments: 1, events: 1, receipts: 1 }, 'Actual PG has one effect/event/receipt');
});

scenario('GET shipment detail contains nullable creation reason and immutable creation event', async () => {
  const detail = (await wire('getDemoShipment', { token: tokens.a, id: shipment.id })).data;
  equal(detail.shipment, shipment, 'Detail preserves current shipment fields');
  equal(detail.addressVerification, 'UNVERIFIED_DRAFT_INPUT', 'No address verification behavior is added');
  equal(detail.events.length, 1, 'Exactly one creation event');
  equal(detail.events[0].reason, null, 'Creation reason must be present and null');
  equal(detail.events[0].actorId, actors.a.id, 'Tracking actor must match trusted actor');
});

scenario('owner list and admin summaries preserve resource versus summary boundaries', async () => {
  const own = (await wire('listDemoShipments', { token: tokens.a })).data;
  ok(own.some(row => row.id === shipment.id), 'Own created shipment appears');
  const summary = (await wire('listDemoShipmentSummaries', { token: tokens.admin })).data;
  equal(summary.length, 1, 'Admin sees current summary');
  ok(!('recipientPhone' in summary[0]) && !('deliveryStreet' in summary[0]), 'Summary excludes recipient contact/address');
});

scenario('admin users are sanitized and overview counts match PostgreSQL', async () => {
  const users = (await wire('listDemoUsers', { token: tokens.admin })).data;
  equal(users.length, 5, 'Only synthetic seed accounts exist');
  ok(users.every(user => !('passwordHash' in user) && !('password' in user)), 'No password material returned');
  equal((await wire('getDemoOverview', { token: tokens.admin })).data, { total: 1, draft: 1, cancelled: 0 }, 'HTTP overview reflects actual PG state');
});

scenario('same create key/body replays201 with current resource and no additional SQL effect', async () => {
  const before = counts();
  equal((await wire('createDemoShipment', { token: tokens.a, key: 'wire.create.001', body: draft })).data, shipment, 'Create replay returns same resource');
  equal(counts(), before, 'Replay cannot insert a second shipment/event/receipt');
});

scenario('changed payload and duplicate merchant reference return distinct409 codes', async () => {
  const before = counts();
  await wire('createDemoShipment', { token: tokens.a, key: 'wire.create.001', body: { ...draft, weightKg: 2.5 }, status: 409, code: 'IDEMPOTENCY_CONFLICT' });
  await wire('createDemoShipment', { token: tokens.a, key: 'wire.new.reference', body: draft, status: 409, code: 'REFERENCE_EXISTS' });
  equal(counts(), before, 'Conflicts preserve all effects');
});

scenario('cross-merchant reads/cancels are indistinguishable404 and lists stay scoped', async () => {
  const before = counts();
  const hidden = (await wire('getDemoShipment', { token: tokens.b, id: shipment.id, status: 404, code: 'SHIPMENT_NOT_FOUND' })).data;
  const missing = (await wire('getDemoShipment', { token: tokens.b, id: randomUUID(), status: 404, code: 'SHIPMENT_NOT_FOUND' })).data;
  equal(hidden, missing, 'Owner denial must not disclose resource existence');
  await wire('cancelDemoShipment', { token: tokens.b, id: shipment.id, body: { reason: 'Synthetic denied' }, key: 'wire.denied', status: 404, code: 'SHIPMENT_NOT_FOUND' });
  equal((await wire('listDemoShipments', { token: tokens.b })).data, [], 'B never lists A rows');
  equal(counts(), before, 'Cross-owner denied cancellation has no effect');
});

scenario('merchant scopes independently own identical create keys and references', async () => {
  const before = counts();
  const own = (await wire('createDemoShipment', { token: tokens.b, key: 'wire.create.001', body: draft })).data;
  ok(own.id !== shipment.id, 'The same key/reference in a second merchant has a separate resource');
  equal(own.merchantId, actors.b.merchantId, 'The second resource belongs to B');
  equal((await wire('createDemoShipment', { token: tokens.b, key: 'wire.create.001', body: draft })).data.id, own.id, 'B replays its own receipt');
  const listed = (await wire('listDemoShipments', { token: tokens.b })).data;
  ok(listed.some(row => row.id === own.id) && !listed.some(row => row.id === shipment.id), 'B only lists its scoped row');
  equal(counts(), { shipments: before.shipments + 1, events: before.events + 1, receipts: before.receipts + 1 }, 'Merchant-scoped key creates exactly one independent effect');
});

function argsFor(id) {
  const args = {};
  if (operations[id].path.includes('{id}')) args.id = shipment.id;
  if (id === 'createDemoShipment') args.body = { ...draft, reference: 'WIRE-DENIED' };
  if (id === 'cancelDemoShipment') args.body = { reason: 'Synthetic denied' };
  if (operations[id]['x-idempotency']) args.key = 'wire.denied';
  return args;
}

scenario('all nine protected operations reject missing credentials with401', async () => {
  const before = counts();
  for (const [id, op] of Object.entries(operations)) if (op.security.length) await wire(id, { ...argsFor(id), status: 401 });
  equal(counts(), before, 'Unauthenticated writes do not take effect');
});

scenario('all eleven operations reject an explicitly invalid bearer token', async () => {
  for (const id of Object.keys(operations)) {
    const args = argsFor(id);
    if (id === 'loginDemo') args.body = { username: 'demo.admin', password: context.password };
    await wire(id, { ...args, headers: { Authorization: 'Bearer synthetic.invalid.token' }, status: 401, code: 'INVALID_TOKEN' });
  }
});

scenario('RBAC matrix denies operational roles/admin-as-merchant/merchant-as-admin', async () => {
  const before = counts();
  for (const [id, op] of Object.entries(operations)) {
    const role = op['x-roles'][0];
    if (role === 'MERCHANT' || role === 'ADMIN') {
      await wire(id, { ...argsFor(id), token: tokens.operator, status: 403, code: 'FORBIDDEN' });
      await wire(id, { ...argsFor(id), token: role === 'MERCHANT' ? tokens.admin : tokens.a, status: 403, code: 'FORBIDDEN' });
    }
  }
  equal(counts(), before, 'RBAC denial causes no writes');
});

function signed(claims) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify(claims)).toString('base64url');
  return header + '.' + payload + '.' + createHmac('sha256', context.tokenSecret).update(header + '.' + payload).digest('base64url');
}

scenario('expired/tampered/wrong-issuer JWTs are rejected by the real decoder', async () => {
  const now = Math.floor(Date.now() / 1000), claims = { sub: actors.a.id, iss: 'elogistics-demo', iat: now - 600, exp: now - 120 };
  await wire('getDemoCurrentUser', { token: signed(claims), status: 401, code: 'INVALID_TOKEN' });
  await wire('getDemoCurrentUser', { token: signed({ ...claims, exp: now + 900, iss: 'wrong-synthetic-issuer' }), status: 401, code: 'INVALID_TOKEN' });
  const parts = tokens.a.split('.'); parts[2] = (parts[2][0] === 'a' ? 'b' : 'a') + parts[2].slice(1);
  await wire('getDemoCurrentUser', { token: parts.join('.'), status: 401, code: 'INVALID_TOKEN' });
  await wire('listDemoUsers', { token: signed({ ...claims, iat: now, exp: now + 900, role: 'ADMIN' }), status: 403, code: 'FORBIDDEN' });
});

scenario('live enabled flag/role/merchant scope is reloaded rather than trusted from JWT', async () => {
  const id = actors.a.id, merchant = actors.a.merchantId;
  try {
    sql(`UPDATE identity.demo_account SET enabled=false WHERE id='${id}';`);
    await wire('getDemoCurrentUser', { token: tokens.a, status: 401, code: 'INVALID_TOKEN' });
    sql(`UPDATE identity.demo_account SET enabled=true, role='WAREHOUSE_OPERATOR',merchant_id=NULL WHERE id='${id}';`);
    await wire('listDemoShipments', { token: tokens.a, status: 403, code: 'FORBIDDEN' });
    sql(`UPDATE identity.demo_account SET role='MERCHANT',merchant_id='${randomUUID()}' WHERE id='${id}';`);
    await wire('getDemoCurrentMerchant', { token: tokens.a, status: 403, code: 'MERCHANT_UNAVAILABLE' });
    await wire('listDemoShipments', { token: tokens.a, status: 403, code: 'MERCHANT_UNAVAILABLE' });
  } finally { sql(`UPDATE identity.demo_account SET enabled=true,role='MERCHANT',merchant_id='${merchant}' WHERE id='${id}';`); }
});

scenario('disabled account and wrong password preserve safe login errors', async () => {
  await wire('loginDemo', { body: { username: 'demo.disabled', password: context.password }, status: 401, code: 'INVALID_CREDENTIALS' });
  await wire('loginDemo', { body: { username: 'demo.admin', password: 'synthetic-wrong-password' }, status: 401, code: 'INVALID_CREDENTIALS' });
});

scenario('invalid request bodies never write shipments/events/receipts', async () => {
  const before = counts();
  const bodies = [{ ...draft, weightKg: -1 }, { ...draft, weightKg: 1.2345 }, { ...draft, weightKg: 1000.001 },
    { ...draft, deliveryWard: '   ' }, { ...draft, recipientPhone: 'not-a-phone' }, { ...draft, ownerId: actors.b.id },
    { ...draft, status: 'CANCELLED' }, { ...draft, role: 'ADMIN' }, { ...draft, weightKg: null },
    Object.fromEntries(Object.entries(draft).filter(([name]) => name !== 'reference'))];
  for (const body of bodies) await wire('createDemoShipment', { token: tokens.a, key: 'wire.invalid.body', body, status: 400, code: 'INVALID_REQUEST', invalidRequest: true });
  equal(counts(), before, 'Bean Validation/Jackson denial is transaction-safe');
});

scenario('missing/invalid keys, invalid UUID and malformed JSON retain documented400 errors', async () => {
  const before = counts();
  await wire('createDemoShipment', { token: tokens.a, body: draft, status: 400, code: 'INVALID_REQUEST', invalidRequest: true });
  await wire('createDemoShipment', { token: tokens.a, key: 'invalid key', body: draft, status: 400, code: 'INVALID_OPERATION_KEY', invalidRequest: true });
  await wire('getDemoShipment', { token: tokens.a, id: 'not-a-uuid', status: 400, code: 'INVALID_REQUEST', invalidRequest: true });
  await wire('createDemoShipment', { token: tokens.a, key: 'wire.invalid.json', raw: '{', status: 400, code: 'INVALID_REQUEST', invalidRequest: true });
  await wire('cancelDemoShipment', { token: tokens.a, id: shipment.id, key: 'wire.blank.reason', body: { reason: '\u2003 ' }, status: 400, code: 'INVALID_REQUEST', invalidRequest: true });
  equal(counts(), before, 'Invalid input/headers/path never change DB counts');
});

scenario('raw padded draft values normalize server-side and replay with canonical values', async () => {
  const input = { ...draft, reference: '  WIRE-PADDED  ', pickupAddress: '  Synthetic pickup  ' };
  const result = (await wire('createDemoShipment', { token: tokens.a, key: 'wire.padded', body: input, canonicalRequest: true })).data;
  equal(result.reference, 'WIRE-PADDED', 'Backend performs its existing trim');
  equal(result.pickupAddress, 'Synthetic pickup', 'Backend canonical address matches source');
  const before = counts();
  const replay = (await wire('createDemoShipment', { token: tokens.a, key: 'wire.padded', body: { ...input, reference: 'WIRE-PADDED', pickupAddress: 'Synthetic pickup' } })).data;
  equal(replay.id, result.id, 'Hash uses normalized input'); equal(counts(), before, 'Canonical replay has one effect');
});

scenario('cancel/replay/conflict transitions preserve reason normalization and one cancellation effect', async () => {
  const result = (await wire('cancelDemoShipment', { token: tokens.a, id: shipment.id, key: 'wire.cancel.001', body: { reason: '  Synthetic cancel  ' } })).data;
  equal(result.status, 'CANCELLED', 'Existing DRAFT cancellation transition');
  const before = counts();
  equal((await wire('cancelDemoShipment', { token: tokens.a, id: shipment.id, key: 'wire.cancel.001', body: { reason: 'Synthetic cancel' } })).data, result, 'Canonical reason replays receipt');
  await wire('cancelDemoShipment', { token: tokens.a, id: shipment.id, key: 'wire.cancel.001', body: { reason: 'Changed reason' }, status: 409, code: 'IDEMPOTENCY_CONFLICT' });
  await wire('cancelDemoShipment', { token: tokens.a, id: shipment.id, key: 'wire.cancel.fresh', body: { reason: 'Synthetic cancel' }, status: 409, code: 'INVALID_TRANSITION' });
  const detail = (await wire('getDemoShipment', { token: tokens.a, id: shipment.id })).data;
  equal(detail.events.length, 2, 'Exactly creation plus one cancellation event');
  equal(detail.events[1].reason, 'Synthetic cancel', 'Stored event reason is trimmed');
  equal(counts(), before, 'Replays/conflicts do not append duplicate effects');
  const creationReplay = (await wire('createDemoShipment', { token: tokens.a, key: 'wire.create.001', body: draft })).data;
  equal(creationReplay.status, 'CANCELLED', 'Create replay returns current representation, still201');
});

scenario('concurrent identical create requests produce one PG shipment/event/receipt', async () => {
  const before = counts(), body = { ...draft, reference: 'WIRE-CONCURRENT-CREATE' };
  const responses = await Promise.all([0, 1].map(() => wire('createDemoShipment', { token: tokens.a, key: 'wire.concurrent.create', body })));
  equal(responses[0].data.id, responses[1].data.id, 'Concurrent matching requests identify one resource');
  equal(counts(), { shipments: before.shipments + 1, events: before.events + 1, receipts: before.receipts + 1 }, 'Real PG atomic create invariant');
  const id = responses[0].data.id;
  equal(sql(`SELECT count(*) FROM orders.demo_event WHERE shipment_id='${id}' AND event_type='DRAFT_CREATED';`), '1', 'Concurrent create has one committed creation event');
  equal(sql(`SELECT count(*) FROM orders.demo_operation WHERE shipment_id='${id}' AND operation='create' AND operation_key='wire.concurrent.create';`), '1', 'Concurrent create has one scoped receipt');
});

scenario('concurrent matching cancel requests have one PG cancellation event/receipt', async () => {
  const created = (await wire('createDemoShipment', { token: tokens.a, key: 'wire.concurrent.cancel.create', body: { ...draft, reference: 'WIRE-CONCURRENT-CANCEL' } })).data;
  const before = counts();
  const responses = await Promise.all([0, 1].map(() => wire('cancelDemoShipment', { token: tokens.a, id: created.id, key: 'wire.concurrent.cancel', body: { reason: 'Concurrent synthetic cancel' } })));
  equal(responses[0].data, responses[1].data, 'Matching cancel receipts replay one current resource');
  equal(counts(), { shipments: before.shipments, events: before.events + 1, receipts: before.receipts + 1 }, 'Real PG atomic cancel invariant');
  equal(sql(`SELECT count(*) FROM orders.demo_event WHERE shipment_id='${created.id}' AND event_type='DRAFT_CANCELLED';`), '1', 'Exactly one committed cancellation event');
  equal(sql(`SELECT count(*) FROM orders.demo_operation WHERE shipment_id='${created.id}' AND operation='cancel:${created.id}' AND operation_key='wire.concurrent.cancel';`), '1', 'Exactly one scoped cancellation receipt');
});

scenario('actual login limiter returns429 after five failed attempts', async () => {
  for (let attempt = 0; attempt < 5; attempt++) await wire('loginDemo', { body: { username: 'wire.synthetic.unknown', password: context.password }, status: 401, code: 'INVALID_CREDENTIALS' });
  await wire('loginDemo', { body: { username: 'wire.synthetic.unknown', password: context.password }, status: 429, code: 'LOGIN_RATE_LIMIT' });
});
