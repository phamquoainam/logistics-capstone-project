import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { spawnSync } from 'node:child_process';
import { isDeepStrictEqual } from 'node:util';
import { loadContract, fixtureValidator, resolveLocal, packageRoot } from '../scripts/contract-lib.mjs';
import * as sdk from '@elogistics/api-client';
import { createClient } from '@elogistics/api-client/client';

if (!process.env.C03_CONTEXT) throw new Error('Use the owned PostgreSQL wire harness; no arbitrary backend target is accepted');
export const context = JSON.parse(readFileSync(process.env.C03_CONTEXT, 'utf8'));
const target = new URL(context.baseUrl);
if (target.hostname !== '127.0.0.1' || target.pathname !== '/api/demo/v1' || !context.database?.startsWith('elogistics_wire_')) throw new Error('Invalid isolated target');
export const contract = loadContract();
export const operations = Object.fromEntries(Object.entries(contract.paths).flatMap(([path, item]) =>
  Object.entries(item).map(([method, op]) => [op.operationId, { path, method, ...op }])));
export const observations = [];
export const cases = [];
export const mismatches = [];
let stopped = false;
const validators = new Map();

export function ok(condition, message) { if (!condition) throw new Error(message); }
export function equal(actual, expected, message) { ok(isDeepStrictEqual(actual, expected), message); }

function validate(schema, value) {
  const key = JSON.stringify(schema);
  if (!validators.has(key)) {
    const doc = { ...contract, components: { ...contract.components,
      schemas: { ...contract.components.schemas, WireFixture: schema } } };
    validators.set(key, fixtureValidator(doc, 'WireFixture'));
  }
  const validator = validators.get(key);
  return validator(value) ? [] : validator.errors.map(error => ({ keyword: error.keyword,
    instancePath: error.instancePath, schemaPath: error.schemaPath }));
}

function drift(id, expected, actual, errors = []) {
  stopped = true;
  mismatches.push({ operationId: id, expected, actual, schemaErrors: errors });
  throw new Error(`WIRE_DRIFT ${id}: ${expected}; observed ${actual}`);
}

function definitionOf(value) { return value.$ref ? resolveLocal(contract, value.$ref) : value; }

export function canonicalDraft(body) {
  return Object.fromEntries(Object.entries(body).map(([key, value]) => [key, typeof value === 'string' ? value.trim() : value]));
}

export async function wire(id, options = {}) {
  if (stopped) throw new Error('BLOCKED_AFTER_PRIOR_WIRE_DRIFT');
  const op = operations[id];
  if (!op) throw new Error('Unknown reviewed operation');
  const expected = options.status ?? (id === 'createDemoShipment' ? 201 : 200);
  if (op.requestBody && !options.invalidRequest) {
    const input = options.canonicalRequest ? canonicalDraft(options.body) : options.body;
    const errors = validate(op.requestBody.content['application/json'].schema, input);
    ok(!errors.length, 'Positive request must conform to the documented canonical schema');
  }
  const headers = { ...options.headers };
  if (options.key !== undefined) headers['Idempotency-Key'] = options.key;
  if (!options.invalidRequest) for (const raw of op.parameters ?? []) {
    const parameter = definitionOf(raw);
    const value = parameter.in === 'path' ? options.id : headers[parameter.name];
    ok(!parameter.required || value !== undefined, 'Positive request must include required parameters');
    if (value !== undefined) ok(!validate(parameter.schema, value).length, 'Positive parameter must conform to OAS');
  }
  let observed;
  const client = createClient({ baseUrl: context.baseUrl, auth: options.token,
    fetch: async request => { observed = { method: request.method, url: request.url, body: await request.clone().text() };
      return globalThis.fetch(request); } });
  let response, body;
  if (options.raw !== undefined) {
    const url = context.baseUrl + op.path.replace('{id}', encodeURIComponent(options.id));
    if (options.token) headers.Authorization = 'Bearer ' + options.token;
    const result = await fetch(url, { method: op.method.toUpperCase(), headers: { 'Content-Type': 'application/json', ...headers }, body: options.raw, signal: AbortSignal.timeout(15_000) });
    response = result;
    try { body = await result.json(); } catch { drift(id, 'application/json response', 'nonJSON'); }
  } else {
    const args = { client, headers, signal: AbortSignal.timeout(15_000) };
    if (options.id !== undefined) args.path = { id: options.id };
    if (options.body !== undefined) args.body = options.body;
    const result = await sdk[id](args);
    response = result.response; body = response?.ok ? result.data : result.error;
    if (!response) throw new Error(`Actual HTTP transport failed for ${id}`);
    equal(observed.method, op.method.toUpperCase(), 'Generated method must match OAS');
    equal(observed.url, context.baseUrl + op.path.replace('{id}', encodeURIComponent(options.id)), 'Generated URL must match OAS without duplicated prefix');
    if (options.body) equal(JSON.parse(observed.body), options.body, 'Actual serialized request must preserve caller body');
  }
  if (response.status !== expected) drift(id, `HTTP ${expected}`, `HTTP ${response.status}`);
  const declared = op.responses[String(response.status)];
  if (!declared) drift(id, 'explicit operation status', `undeclared HTTP ${response.status}`);
  const definition = definitionOf(declared);
  if (!response.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) drift(id, 'JSON Content-Type', 'other content type');
  const schema = definition.content?.['application/json']?.schema;
  if (!schema) drift(id, 'explicit JSON schema', 'missing schema');
  const errors = validate(schema, body);
  if (errors.length) drift(id, 'response schema conformance', 'invalid body', errors);
  if (definition['x-error-codes'] && !definition['x-error-codes'].includes(body.code)) drift(id, 'documented error code', 'unexpected code');
  if (options.code && body.code !== options.code) drift(id, `source error code ${options.code}`, 'different error code');
  if (response.status === 201 && response.headers.get('Location') !== '/api/demo/v1/shipments/' + body.id)
    drift(id, 'Location identifies returned resource', 'different Location');
  observations.push({ operationId: id, status: response.status, responseSchema: 'PASS',
    requestSchema: options.invalidRequest ? 'NEGATIVE_INPUT' : options.canonicalRequest ? 'CANONICAL_AFTER_DOCUMENTED_TRIM' : 'PASS' });
  return { data: body, response };
}

export function sql(statement) {
  const result = spawnSync(process.env.C03_PYTHON ?? 'python', [resolve(packageRoot, 'scripts/wire-harness.py'), 'sql'],
    { cwd: packageRoot, env: process.env, input: statement, encoding: 'utf8', timeout: 30_000 });
  ok(result.status === 0, 'Owned PostgreSQL fixture query/update failed; no SQL diagnostics printed');
  return result.stdout.trim();
}

export function counts() {
  return JSON.parse(sql(`SELECT json_build_object('shipments',(SELECT count(*) FROM orders.demo_shipment),
    'events',(SELECT count(*) FROM orders.demo_event),'receipts',(SELECT count(*) FROM orders.demo_operation))::text;`));
}

export function saveReport() {
  const successful = new Set(observations.filter(o => o.status < 300).map(o => o.operationId));
  const report = { cases, requests: observations, successOperations: [...successful].sort(), mismatches,
    positiveRequests: observations.filter(o => o.status < 300).length,
    negativeRequests: observations.filter(o => o.status >= 400).length };
  writeFileSync(resolve(context.directory, 'safe-wire-results.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(`Wire observations: ${successful.size}/11 successful operations; ${report.positiveRequests} positive / ${report.negativeRequests} negative HTTP responses; ${mismatches.length} mismatches`);
}
