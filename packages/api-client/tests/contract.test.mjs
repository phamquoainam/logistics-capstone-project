import { test, before } from 'node:test';
import assert from 'node:assert/strict';
import { writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { loadContract, checkContract, fixtureValidator, redocly, tempDirectory } from '../scripts/contract-lib.mjs';

let contract;
before(() => { contract = loadContract(); });

test('source inventory, DTO properties, authorization and wire metadata match the current backend', () => {
  assert.deepEqual(checkContract(contract), []);
});

for (const [name, mutate, expected] of [
  ['missing operation', d => { delete d.paths['/merchant/me']; }, /inventory|eleven/],
  ['invented operation', d => { d.paths['/invented'] = structuredClone(d.paths['/health']); }, /inventory|eleven/],
  ['missing protected security', d => { delete d.paths['/shipments'].get.security; }, /auth drift/],
  ['wrong RBAC', d => { d.paths['/admin/users'].get['x-roles'] = ['MERCHANT']; }, /RBAC/],
  ['optional idempotency header', d => { d.components.parameters.IdempotencyKey.required = false; }, /idempotency/],
  ['wrong creation status', d => { d.paths['/shipments'].post.responses['200'] = d.paths['/shipments'].post.responses['201']; delete d.paths['/shipments'].post.responses['201']; }, /success status/],
  ['missing error status', d => { delete d.paths['/shipments/{id}'].get.responses['404']; }, /HTTP status inventory/],
  ['lost ownership metadata', d => { delete d.paths['/shipments/{id}'].get['x-ownership']; }, /ownership/],
  ['wrong success body', d => { d.paths['/auth/me'].get.responses['200'].content['application/json'].schema.$ref = '#/components/schemas/MerchantView'; }, /success response schema/],
  ['wrong request body', d => { d.paths['/shipments'].post.requestBody.content['application/json'].schema.$ref = '#/components/schemas/CancelInput'; }, /request schema/],
  ['wrong error code', d => { d.components.responses.CreateConflict['x-error-codes'] = ['OTHER']; }, /error codes/],
  ['wrong error status binding', d => { d.paths['/shipments/{id}'].get.responses['404'].$ref = '#/components/responses/CreateConflict'; }, /error response binding/],
  ['wrong error shape', d => { d.components.responses.InvalidRequest.content['application/json'].schema.$ref = '#/components/schemas/AccountView'; }, /flat error schema/],
  ['invented error message', d => { d.components.responses.NotFound.content['application/json'].examples.SHIPMENT_NOT_FOUND.value.message = 'invented'; }, /source error example/],
  ['lost Location', d => { delete d.paths['/shipments'].post.responses['201'].headers; }, /Location/],
  ['invented DTO property', d => { d.components.schemas.AccountView.properties.passwordHash = { type: 'string' }; }, /property set/],
  ['missing required property', d => { d.components.schemas.AccountView.required = ['id']; }, /required properties/],
  ['lost nullability', d => { d.components.schemas.AccountView.properties.merchantId.type = 'string'; }, /nullability/],
  ['changed validation bound', d => { d.components.schemas.LoginInput.properties.username.maxLength = 81; }, /@Size/],
  ['lost nonblank constraint', d => { delete d.components.schemas.CancelInput.properties.reason.allOf; }, /@NotBlank/],
]) {
  test(`negative inventory fixture detects ${name}`, () => {
    const changed = structuredClone(contract); mutate(changed);
    assert(checkContract(changed).some(message => expected.test(message)), `Expected detection of ${name}`);
  });
}

for (const [name, mutate, expectedRule] of [
  ['broken ref', d => { d.paths['/health'].get.responses['200'].content['application/json'].schema.$ref = '#/components/schemas/DoesNotExist'; }, 'no-unresolved-refs'],
  ['duplicate operationId', d => { d.paths['/health'].get.operationId = d.paths['/auth/me'].get.operationId; }, 'operation-operationId-unique'],
  ['invalid parameter location', d => { d.components.parameters.ShipmentId.in = 'invented'; }, 'struct'],
]) {
  test(`negative Redocly fixture detects ${name}`, () => {
    const changed = structuredClone(contract); mutate(changed);
    const file = resolve(tempDirectory('negative-'), 'invalid.json');
    writeFileSync(file, JSON.stringify(changed));
    const result = redocly(['lint', file, '--format', 'json']);
    assert.equal(result.status, 1, result.output);
    const report = JSON.parse(result.stdout);
    assert(report.problems.some(problem => problem.ruleId === expectedRule), result.output);
  });
}

test('all documented component examples validate through strict Ajv2020 and formats without modifying data', t => {
  let examples = 0;
  for (const [name, schema] of Object.entries(contract.components.schemas)) {
    const validate = fixtureValidator(contract, name);
    for (const sample of schema.examples ?? []) {
      const before = JSON.stringify(sample);
      assert(validate(sample), `${name}: ${JSON.stringify(validate.errors)}`);
      assert.equal(JSON.stringify(sample), before); examples++;
    }
  }
  assert.equal(examples, 13); t.diagnostic(`${examples} component examples validated`);
});

test('all documented flat error examples validate against the same ErrorBody schema', t => {
  const validate = fixtureValidator(contract, 'ErrorBody'); let examples = 0;
  for (const response of Object.values(contract.components.responses)) {
    for (const { value } of Object.values(response.content?.['application/json']?.examples ?? {})) {
      assert(validate(value), JSON.stringify(validate.errors));
      assert(response['x-error-codes'].includes(value.code)); examples++;
    }
  }
  assert(examples > 0); t.diagnostic(`${examples} application error examples validated`);
});

for (const [name, schema, mutate, keyword] of [
  ['missing response property', 'AccountView', x => { delete x.enabled; }, 'required'],
  ['invalid UUID', 'AccountView', x => { x.id = 'synthetic-id'; }, 'format'],
  ['invalid time', 'ShipmentView', x => { x.createdAt = 'yesterday'; }, 'format'],
  ['unknown request owner', 'DraftInput', x => { x.ownerId = 'injected'; }, 'additionalProperties'],
  ['wrong numeric weight type', 'DraftInput', x => { x.weightKg = '1.25'; }, 'type'],
  ['invalid weight precision', 'DraftInput', x => { x.weightKg = 1.2345; }, 'multipleOf'],
  ['invalid weight range', 'DraftInput', x => { x.weightKg = -1; }, 'minimum'],
  ['unknown state', 'ShipmentView', x => { x.status = 'CONFIRMED'; }, 'enum'],
  ['null nonnullable field', 'AccountView', x => { x.username = null; }, 'type'],
  ['old envelope shape', 'ErrorBody', x => { delete x.code; x.error = { code: 'FORBIDDEN' }; }, 'required'],
  ['blank login', 'LoginInput', x => { x.username = ' \t\r\n'; }, 'pattern'],
  ['Unicode blank reason', 'CancelInput', x => { x.reason = '\u2003\u3000'; }, 'pattern'],
  ['blank draft address', 'DraftInput', x => { x.pickupAddress = '    '; }, 'pattern'],
  ['unnormalized newline in canonical reference', 'DraftInput', x => { x.reference += '\n'; }, 'pattern'],
]) {
  test(`negative JSON fixture detects ${name}`, () => {
    const value = structuredClone(contract.components.schemas[schema].examples[0]); mutate(value);
    const before = JSON.stringify(value), validate = fixtureValidator(contract, schema);
    assert.equal(validate(value), false);
    assert(validate.errors.some(error => error.keyword === keyword), JSON.stringify(validate.errors));
    assert.equal(JSON.stringify(value), before);
  });
}

test('nullable fields, decimal boundaries and local ref conjunctions preserve constraints', () => {
  const account = structuredClone(contract.components.schemas.AccountView.examples[0]); account.merchantId = null;
  assert(fixtureValidator(contract, 'AccountView')(account));
  const event = structuredClone(contract.components.schemas.ShipmentEvent.examples[0]); event.reason = null;
  assert(fixtureValidator(contract, 'ShipmentEvent')(event));
  const draft = structuredClone(contract.components.schemas.DraftInput.examples[0]);
  for (const weight of [0.001, 1.001, 1000]) { draft.weightKg = weight; assert(fixtureValidator(contract, 'DraftInput')(draft)); }
  const document = structuredClone(contract);
  document.components.schemas.RestrictedDraft = { $ref: '#/components/schemas/DraftInput',
    type: 'object', properties: { weightKg: { type: 'number', maximum: 1 } } };
  const validate = fixtureValidator(document, 'RestrictedDraft');
  draft.weightKg = 0.5; assert(validate(draft));
  draft.weightKg = 1.001; assert.equal(validate(draft), false);
  draft.weightKg = 0.5; delete draft.recipientName; assert.equal(validate(draft), false);
});
