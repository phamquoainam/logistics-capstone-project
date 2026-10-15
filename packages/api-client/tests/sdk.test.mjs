import { test, before } from 'node:test';
import assert from 'node:assert/strict';
import ts from 'typescript';
import { resolve, dirname } from 'node:path';
import { writeFileSync, mkdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { packageRoot, loadContract, fixtureValidator, resolveLocal } from '../scripts/contract-lib.mjs';
import { generationConfig, checkGenerationConfig, snapshot, generatedRoot, compareSnapshots, sdkScratch, assertLocalRefs } from '../scripts/generation-lib.mjs';
import * as sdk from '@elogistics/api-client';
import { createClient } from '@elogistics/api-client/client';

let contract;
before(() => { contract = loadContract(); });

function diagnostics(body, bundler = false) {
  const filename = resolve(packageRoot, 'tests/virtual-consumer.ts');
  const options = { strict: true, noEmit: true, skipLibCheck: false, types: [],
    target: ts.ScriptTarget.ES2022, module: bundler ? ts.ModuleKind.ESNext : ts.ModuleKind.NodeNext,
    moduleResolution: bundler ? ts.ModuleResolutionKind.Bundler : ts.ModuleResolutionKind.NodeNext };
  const host = ts.createCompilerHost(options), read = host.readFile.bind(host), exists = host.fileExists.bind(host);
  host.readFile = file => resolve(file) === filename ? body : read(file);
  host.fileExists = file => resolve(file) === filename || exists(file);
  host.getSourceFile = (file, languageVersion) => {
    const content = host.readFile(file);
    return content === undefined ? undefined : ts.createSourceFile(file, content, languageVersion, true);
  };
  return ts.getPreEmitDiagnostics(ts.createProgram([filename], options, host));
}

const typedConsumer = `
    import { loginDemo, createDemoShipment, cancelDemoShipment, getDemoShipment,
      type DraftInput, type CancelInput, type LoginDemoData, type AccountView, type ShipmentEvent }
      from '@elogistics/api-client';
    type Equal<A,B> = (<T>()=>T extends A?1:2) extends (<T>()=>T extends B?1:2) ? true : false;
    const strings: Equal<DraftInput[Exclude<keyof DraftInput,'weightKg'>], string> = true;
    const reason: Equal<CancelInput['reason'], string> = true;
    const password: Equal<LoginDemoData['body']['password'], string> = true;
    const weight: Equal<DraftInput['weightKg'], number> = true;
    const merchant: Equal<AccountView['merchantId'], string | null> = true;
    const event: Equal<ShipmentEvent['reason'], string | null> = true;
    declare const draft: DraftInput;
    loginDemo({body:{username:'synthetic',password:'SYNTHETIC-INPUT'}});
    createDemoShipment({body:draft,headers:{'Idempotency-Key':'type.create.001'}});
    cancelDemoShipment({path:{id:'synthetic-path'},body:{reason:'synthetic'},headers:{'Idempotency-Key':'type.cancel.001'}});
    getDemoShipment({path:{id:'synthetic-path'}});
  `;
for (const bundler of [false, true]) {
  test(`package declarations preserve request/nullable types with ${bundler ? 'Bundler' : 'NodeNext'} resolution`, () => {
    const found = diagnostics(typedConsumer, bundler);
    assert.deepEqual(found.map(d => ts.flattenDiagnosticMessageText(d.messageText, '\n')), []);
  });
}

for (const [name, body, code] of [
  ['missing create header', `import {createDemoShipment,type DraftInput} from '@elogistics/api-client'; declare const body:DraftInput; createDemoShipment({body});`, 2345],
  ['missing cancel key', `import {cancelDemoShipment} from '@elogistics/api-client'; cancelDemoShipment({path:{id:'id'},body:{reason:'x'},headers:{}});`, 2322],
  ['missing UUID path', `import {getDemoShipment} from '@elogistics/api-client'; getDemoShipment({});`, 2345],
  ['numeric UUID path', `import {getDemoShipment} from '@elogistics/api-client'; getDemoShipment({path:{id:42}});`, 2322],
  ['numeric login field', `import {loginDemo} from '@elogistics/api-client'; loginDemo({body:{username:42,password:'x'}});`, 2322],
  ['wrong request DTO', `import {createDemoShipment} from '@elogistics/api-client'; createDemoShipment({body:{reason:'x'},headers:{'Idempotency-Key':'x'}});`, 2353],
  ['string weight', `import {type DraftInput} from '@elogistics/api-client'; const weight:DraftInput['weightKg']='1.25';`, 2322],
  ['invented operation', `import {confirmDemoShipment} from '@elogistics/api-client';`, 2305],
  ['invented endpoint override', `import {getDemoShipment} from '@elogistics/api-client'; getDemoShipment({path:{id:'x'},url:'/invented'});`, 2353],
  ['invented query parameter', `import {listDemoShipments} from '@elogistics/api-client'; listDemoShipments({query:{page:1}});`, 2322],
]) {
  test(`negative compiler fixture rejects ${name}`, () => {
    const found = diagnostics(body);
    assert(found.some(d => d.code === code), found.map(d => `${d.code}: ${ts.flattenDiagnosticMessageText(d.messageText, '\n')}`).join('\n'));
    assert(!found.some(d => [2307, 6053].includes(d.code)), 'A missing module/file is not the expected type rejection');
  });
}

test('parser enrichment retains schema validation outcomes and all pattern constraints', () => {
  const patched = structuredClone(contract), before = JSON.stringify(contract);
  for (const [name, schema] of Object.entries(patched.components.schemas)) generationConfig.parser.patch.schemas(name, schema);
  let branches = 0;
  for (const [name, schema] of Object.entries(contract.components.schemas)) {
    const a = fixtureValidator(contract, name), b = fixtureValidator(patched, name);
    for (const sample of schema.examples ?? []) assert.equal(a(sample), b(sample));
    for (const [field, property] of Object.entries(schema.properties ?? {})) {
      if (!property['x-java-not-blank']) continue;
      const changed = patched.components.schemas[name].properties[field];
      assert.equal(changed.allOf[0].pattern, property.allOf[0].pattern);
      assert.equal(changed.allOf[0].type, 'string'); branches++;
      for (const value of ['', '  ', '\u2003', 'synthetic', null, 42, 'x'.repeat(501)]) {
        const sample = structuredClone(schema.examples[0]); sample[field] = value;
        assert.equal(a(sample), b(sample), `${name}.${field}`);
      }
    }
  }
  assert.equal(branches, 11); assert.equal(JSON.stringify(contract), before);
});

test('actual generation succeeds with the installed js-yaml loader blocked', () => {
  const result = spawnSync(process.execPath, ['--input-type=module', '-e', `
    import { registerHooks } from 'node:module';
    import assert from 'node:assert/strict';
    globalThis.blockedYamlCalls = 0;
    registerHooks({ load(url, context, nextLoad) {
      if (url.includes('/node_modules/js-yaml/')) return { format:'module', shortCircuit:true,
        source: 'export function load(){globalThis.blockedYamlCalls++;throw new Error("BLOCKED_JS_YAML_LOAD")} export default {load};' };
      return nextLoad(url, context);
    }});
    const yaml = await import('js-yaml');
    assert.throws(() => yaml.load('synthetic'), /BLOCKED_JS_YAML_LOAD/);
    globalThis.blockedYamlCalls = 0;
    const { generateFresh } = await import('./scripts/generation-lib.mjs');
    const files = await generateFresh();
    assert(files.size > 0); assert.equal(globalThis.blockedYamlCalls, 0);
    console.log('PASS: zero installed js-yaml.load calls during actual generation');
  `], { cwd: packageRoot, encoding: 'utf8', timeout: 60_000 });
  assert.equal(result.status, 0, result.stdout + result.stderr);
  assert.match(result.stdout, /zero installed js-yaml.load calls/);
});

test('generation ref guard rejects remote and unresolved refs', () => {
  const changed = structuredClone(contract);
  changed.components.schemas.LoginResult.properties.user.$ref = 'https://contract.invalid/remote.yaml';
  assert.throws(() => assertLocalRefs(changed), /Only local refs/);
  changed.components.schemas.LoginResult.properties.user.$ref = '#/components/schemas/Missing';
  assert.throws(() => assertLocalRefs(changed), /Unresolved ref/);
});

for (const [name, mutate, expected] of [
  ['missing SDK plugin', d => { d.plugins = d.plugins.filter(p => typeof p !== 'object' || p.name !== '@hey-api/sdk'); }, /plugins/],
  ['unbundled Fetch', d => { d.plugins[2].bundle = false; }, /Bundled Fetch/],
  ['wrong ESM extension', d => { d.output.module.extension = '.ts'; }, /ESM/],
  ['destructive cleanup', d => { d.output.clean = true; }, /non-destructive/],
  ['remote contract source', d => { d.input = 'https://contract.invalid/unapproved.yaml'; }, /local contract/],
]) {
  test(`negative generation configuration detects ${name}`, () => {
    // Callbacks cannot be structuredCloned; the nested scalar options can.
    const changed = { ...generationConfig, plugins: structuredClone(generationConfig.plugins), output: structuredClone(generationConfig.output) };
    mutate(changed); assert(checkGenerationConfig(changed).some(p => expected.test(p)));
  });
}

for (const [name, mutate, expected] of [
  ['stale bytes', files => { files.set('sdk.gen.ts', Buffer.from('stale generated code\n')); }, /Changed bytes/],
  ['missing operation file', files => { files.delete('sdk.gen.ts'); }, /Missing file/],
  ['unexpected artifact', files => { files.set('unexpected.txt', Buffer.from('unapproved')); }, /Unexpected file/],
]) {
  test(`negative generated drift fixture detects ${name}`, () => {
    const expectedFiles = snapshot(generatedRoot), files = new Map(expectedFiles); mutate(files);
    const directory = sdkScratch('drift-');
    for (const [name, bytes] of files) { const path = resolve(directory, name); mkdirSync(dirname(path), { recursive: true }); writeFileSync(path, bytes); }
    assert(compareSnapshots(expectedFiles, snapshot(directory)).some(p => expected.test(p)));
  });
}

function sample(schema) {
  if (schema.$ref) return structuredClone(resolveLocal(contract, schema.$ref).examples[0]);
  if (schema.type === 'array') return [sample(schema.items)];
  throw new Error('Operation fixture must derive from an approved schema example');
}

test('public package exports exactly eleven generated operation functions', () => {
  const ids = Object.values(contract.paths).flatMap(path => Object.values(path).map(op => op.operationId));
  assert.deepEqual(Object.keys(sdk).filter(key => typeof sdk[key] === 'function').sort(), ids.sort());
  assert.equal(sdk.client.getConfig().baseUrl, '/api/demo/v1');
});

for (const [path, item] of Object.entries({
  '/health': ['get', 'getDemoHealth'], '/auth/login': ['post', 'loginDemo'],
  '/auth/me': ['get', 'getDemoCurrentUser'], '/merchant/me': ['get', 'getDemoCurrentMerchant'],
  '/shipments': ['get', 'listDemoShipments'], '/shipments#create': ['post', 'createDemoShipment'],
  '/shipments/{id}': ['get', 'getDemoShipment'], '/shipments/{id}/cancel': ['post', 'cancelDemoShipment'],
  '/admin/users': ['get', 'listDemoUsers'], '/admin/overview': ['get', 'getDemoOverview'],
  '/admin/shipments': ['get', 'listDemoShipmentSummaries'],
})) {
  test(`generated Fetch request preserves ${item[0].toUpperCase()} ${path.replace('#create', '')}`, async () => {
    const [method, id] = item, suffix = path.replace('#create', ''), operation = contract.paths[suffix][method];
    const status = id === 'createDemoShipment' ? 201 : 200;
    const responseBody = sample(operation.responses[status].content['application/json'].schema);
    let called = 0, observed;
    const client = createClient({ baseUrl: 'https://contract.invalid/api/demo/v1', auth: () => 'SYNTHETIC-TRANSPORT-TOKEN',
      fetch: async request => { called++; observed = request;
        return Response.json(responseBody, { status, headers: status === 201 ? { Location: '/api/demo/v1/shipments/synthetic-id' } : {} }); } });
    const options = { client };
    if (suffix.includes('{id}')) options.path = { id: 'synthetic/id with space' };
    if (operation.requestBody) options.body = sample(operation.requestBody.content['application/json'].schema);
    if (operation['x-idempotency']) options.headers = { 'Idempotency-Key': 'sdk.fixture.001' };
    const result = await sdk[id](options);
    assert.equal(called, 1); assert.equal(result.response.status, status); assert.deepEqual(result.data, responseBody);
    assert.equal(observed.method, method.toUpperCase());
    assert.equal(observed.url, 'https://contract.invalid/api/demo/v1' + suffix.replace('{id}', 'synthetic%2Fid%20with%20space'));
    assert.equal(observed.headers.get('Authorization'), operation.security.length ? 'Bearer SYNTHETIC-TRANSPORT-TOKEN' : null);
    if (options.headers) assert.equal(observed.headers.get('Idempotency-Key'), options.headers['Idempotency-Key']);
    if (options.body) { assert.equal(observed.headers.get('Content-Type'), 'application/json'); assert.deepEqual(JSON.parse(await observed.text()), options.body); }
    if (status === 201) assert.equal(result.response.headers.get('Location'), '/api/demo/v1/shipments/synthetic-id');
  });
}

test('caller input, retry key and per-request auth remain unchanged across explicit calls', async () => {
  let token = 'SYNTHETIC-A'; const requests = [];
  const client = createClient({ baseUrl: 'https://contract.invalid/api/demo/v1', auth: () => token,
    fetch: async request => { requests.push(request); return Response.json(contract.components.schemas.ShipmentView.examples[0], { status: 201 }); } });
  const body = structuredClone(contract.components.schemas.DraftInput.examples[0]); body.reference = '  PADDED-INPUT  ';
  const before = JSON.stringify(body), headers = { 'Idempotency-Key': 'same.explicit.retry' };
  await sdk.createDemoShipment({ client, body, headers }); token = 'SYNTHETIC-B';
  await sdk.createDemoShipment({ client, body, headers });
  assert.equal(requests.length, 2); assert.equal(JSON.stringify(body), before);
  for (const request of requests) { assert.deepEqual(JSON.parse(await request.text()), body); assert.equal(request.headers.get('Idempotency-Key'), headers['Idempotency-Key']); }
  assert.equal(requests[0].headers.get('Authorization'), 'Bearer SYNTHETIC-A');
  assert.equal(requests[1].headers.get('Authorization'), 'Bearer SYNTHETIC-B');
});

for (const status of [400, 401, 403, 404, 409, 429, 500]) {
  test(`generated transport retains HTTP ${status} and flat error body without retry`, async () => {
    let calls = 0; const error = { code: 'SYNTHETIC_ERROR', message: 'Synthetic transport response' };
    const client = createClient({ baseUrl: 'https://contract.invalid/api/demo/v1', fetch: async () => { calls++; return Response.json(error, { status }); } });
    const result = await sdk.getDemoHealth({ client });
    assert.equal(calls, 1); assert.equal(result.response.status, status); assert.deepEqual(result.error, error); assert.equal(result.data, undefined);
  });
}

test('nonJSON and network failures remain failures with no implicit retry', async () => {
  let calls = 0;
  const client = createClient({ baseUrl: 'https://contract.invalid/api/demo/v1', fetch: async () => { calls++; return new Response('synthetic gateway failure', { status: 502 }); } });
  const nonJSON = await sdk.getDemoHealth({ client });
  assert.equal(nonJSON.response.status, 502); assert.equal(nonJSON.error, 'synthetic gateway failure'); assert.equal(calls, 1);
  const offline = new TypeError('synthetic offline');
  client.setConfig({ fetch: async () => { calls++; throw offline; } });
  const network = await sdk.getDemoHealth({ client });
  assert.equal(network.error, offline); assert.equal(network.response, undefined); assert.equal(calls, 2);
  await assert.rejects(sdk.getDemoHealth({ client, throwOnError: true }), error => error === offline); assert.equal(calls, 3);
});
