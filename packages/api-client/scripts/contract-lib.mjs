import { readFileSync, readdirSync, mkdirSync, mkdtempSync } from 'node:fs';
import { dirname, resolve, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawnSync } from 'node:child_process';
import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';

export const packageRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
export const repoRoot = resolve(packageRoot, '../..');
export const contractPath = resolve(repoRoot, 'contracts/openapi/demo-001.yaml');
// Java 21 Character.isWhitespace, used by String.isBlank / @NotBlank.
export const nonBlankPattern = '[^\\u0009-\\u000D\\u001C-\\u0020\\u1680\\u2000-\\u2006\\u2008-\\u200A\\u2028\\u2029\\u205F\\u3000]';
const cli = resolve(packageRoot, 'node_modules/@redocly/cli/bin/cli.js');
const config = resolve(packageRoot, 'redocly.yaml');

export function redocly(args) {
  const result = spawnSync(process.execPath, [cli, ...args, '--config', config, '--lint-config', 'error'], {
    cwd: packageRoot, encoding: 'utf8', timeout: 60_000,
    env: { ...process.env, REDOCLY_TELEMETRY: 'off', NO_COLOR: '1' },
  });
  if (result.error) throw result.error;
  return { status: result.status, output: result.stdout + result.stderr, stdout: result.stdout };
}

export function tempDirectory(prefix = 'contract-') {
  const parent = resolve(repoRoot, '.demo/d1-07-c01');
  mkdirSync(parent, { recursive: true });
  return mkdtempSync(resolve(parent, prefix));
}

export function loadContract(input = contractPath) {
  const output = resolve(tempDirectory('bundle-'), 'contract.json');
  const result = redocly(['bundle', input, '--output', output, '--ext', 'json']);
  if (result.status !== 0) throw new Error(`Contract bundle failed: ${result.output}`);
  return JSON.parse(readFileSync(output, 'utf8'));
}

export function resolveLocal(document, reference) {
  if (!reference.startsWith('#/')) throw new Error(`Only local refs allowed: ${reference}`);
  const value = reference.slice(2).split('/').reduce((node, part) =>
    node?.[part.replaceAll('~1', '/').replaceAll('~0', '~')], document);
  if (value === undefined) throw new Error(`Unresolved ref: ${reference}`);
  return value;
}

export function dereference(document, value, active = new Set()) {
  if (Array.isArray(value)) return value.map(item => dereference(document, item, active));
  if (value && typeof value === 'object') {
    if (value.$ref) {
      if (active.has(value.$ref)) throw new Error(`Unexpected recursive ref: ${value.$ref}`);
      const next = new Set(active).add(value.$ref);
      const { $ref, ...siblings } = value;
      const target = dereference(document, resolveLocal(document, $ref), next);
      // OAS3.1 / JSON Schema ref siblings are a conjunction, not an override.
      return Object.keys(siblings).length
        ? { allOf: [target, dereference(document, siblings, next)] } : target;
    }
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, dereference(document, item, active)]));
  }
  return value;
}

export function fixtureValidator(document, name) {
  const ajv = new Ajv2020({ strict: true, allErrors: true, coerceTypes: false,
    useDefaults: false, removeAdditional: false, multipleOfPrecision: 12 });
  addFormats(ajv);
  // These annotations preserve Java normalization/constraints; they are not assertions.
  for (const keyword of ['x-java-not-blank', 'x-server-trim', 'x-source-record']) ajv.addKeyword(keyword);
  return ajv.compile(dereference(document, document.components.schemas[name]));
}

export const javaRoot = resolve(repoRoot, 'app/backend/src/main/java/com/elogistic/backend');
export function source(relative) { return readFileSync(resolve(javaRoot, relative), 'utf8'); }

// A balanced scanner handles record annotations (including their quoted regexes).
function recordFields(text, name) {
  const start = text.indexOf(`record ${name}(`);
  if (start < 0) throw new Error(`Missing source record: ${name}`);
  let depth = 1, quoted = false, escaped = false, end = start + `record ${name}(`.length;
  const begin = end;
  for (; end < text.length && depth; end++) {
    const c = text[end];
    if (escaped) { escaped = false; continue; }
    if (quoted && c === '\\') { escaped = true; continue; }
    if (c === '"') quoted = !quoted;
    if (!quoted) { if (c === '(') depth++; if (c === ')') depth--; }
  }
  if (depth) throw new Error(`Unclosed source record: ${name}`);
  const body = text.slice(begin, end - 1);
  const fields = [...body.matchAll(/\b(UUID|String|boolean|int|long|BigDecimal|Instant|AccountView|ShipmentView|List<ShipmentEvent>)\s+(\w+)/g)];
  return fields.map((m, index) => ({ type: m[1], name: m[2], annotations:
    body.slice(index ? fields[index - 1].index + fields[index - 1][0].length : 0, m.index) }));
}

const records = {
  LoginInput: ['identity/internal/web/AuthController.java', 'LoginRequest'],
  LoginResult: ['identity/internal/application/LoginService.java', 'LoginResult'],
  AccountView: ['identity/api/AccountView.java', 'AccountView'],
  MerchantView: ['merchant/api/MerchantView.java', 'MerchantView'],
  DraftInput: ['orders/internal/domain/DraftInput.java', 'DraftInput'],
  CancelInput: ['orders/internal/web/ShipmentController.java', 'CancelInput'],
  ShipmentView: ['orders/internal/domain/ShipmentView.java', 'ShipmentView'],
  ShipmentEvent: ['orders/internal/domain/ShipmentEvent.java', 'ShipmentEvent'],
  ShipmentDetail: ['orders/internal/application/ShipmentService.java', 'Detail'],
  ShipmentSummary: ['orders/internal/application/port/ShipmentStore.java', 'Summary'],
  Overview: ['orders/internal/application/port/ShipmentStore.java', 'Overview'],
  ErrorBody: ['platform/internal/web/DemoErrors.java', 'ErrorBody'],
};

export function readSourceOperations() {
  const controllers = readdirSync(javaRoot, { recursive: true, withFileTypes: true })
    .filter(entry => entry.isFile() && entry.name.endsWith('.java'))
    .map(entry => relative(javaRoot, resolve(entry.parentPath, entry.name)))
    .filter(file => /@RestController\b/.test(source(file)));
  return controllers.flatMap(file => {
    const text = source(file);
    const prefix = text.match(/@RequestMapping\("([^"]+)"\)/)?.[1] ?? '';
    const matches = [...text.matchAll(/@(Get|Post|Put|Patch|Delete)Mapping\("([^"]+)"\)/g)];
    // Fail closed when the simple source inventory needs an explicit parser update.
    if ((text.match(/@(Get|Post|Put|Patch|Delete|Request)Mapping\b/g) ?? []).length !==
      matches.length + (prefix ? 1 : 0)) throw new Error(`Unsupported controller mapping in ${file}`);
    return matches
      .map(match => ({ method: match[1].toLowerCase(), path: prefix + match[2], file }));
  }).filter(operation => operation.path.startsWith('/api/demo/v1/'));
}

// Source-reviewed expectations, independent of the authored YAML. They freeze
// this approved slice; they are not a replacement for C-03 HTTP conformance.
const responseShapes = {
  getDemoHealth: 'DemoHealth', loginDemo: 'LoginResult', getDemoCurrentUser: 'AccountView',
  getDemoCurrentMerchant: 'MerchantView', listDemoShipments: 'ShipmentView[]',
  createDemoShipment: 'ShipmentView', getDemoShipment: 'ShipmentDetail',
  cancelDemoShipment: 'ShipmentView', listDemoUsers: 'AccountView[]',
  getDemoOverview: 'Overview', listDemoShipmentSummaries: 'ShipmentSummary[]',
};

const requestShapes = { loginDemo: 'LoginInput', createDemoShipment: 'DraftInput', cancelDemoShipment: 'CancelInput' };

const errorCodes = {
  InvalidRequest: ['INVALID_REQUEST'], InvalidMutation: ['INVALID_REQUEST', 'INVALID_OPERATION_KEY'],
  Unauthenticated: ['UNAUTHENTICATED', 'INVALID_TOKEN'], LoginUnauthorized: ['INVALID_CREDENTIALS', 'INVALID_TOKEN'],
  InvalidToken: ['INVALID_TOKEN'], Forbidden: ['FORBIDDEN'], MerchantForbidden: ['FORBIDDEN', 'MERCHANT_UNAVAILABLE'], NotFound: ['SHIPMENT_NOT_FOUND'],
  CreateConflict: ['REFERENCE_EXISTS', 'IDEMPOTENCY_CONFLICT'], CancelConflict: ['INVALID_TRANSITION', 'IDEMPOTENCY_CONFLICT'],
  RateLimited: ['LOGIN_RATE_LIMIT'], DatabaseError: ['DATABASE_ERROR'],
};

export function checkContract(document) {
  const problems = [];
  const require = (ok, message) => { if (!ok) problems.push(message); };
  require(document.openapi === '3.1.0', 'OpenAPI version must remain 3.1.0');
  require(JSON.stringify(document.servers?.map(s => s.url)) === '["/api/demo/v1"]', 'Server prefix drift');
  const operations = Object.entries(document.paths ?? {}).flatMap(([path, item]) =>
    Object.entries(item).filter(([method]) => ['get', 'post', 'put', 'patch', 'delete', 'head', 'options', 'trace'].includes(method))
      .map(([method, operation]) => ({ method, path: '/api/demo/v1' + path, operation })));
  const fromSource = readSourceOperations();
  const key = o => `${o.method.toUpperCase()} ${o.path}`;
  require(fromSource.length === 11 && operations.length === 11, 'Expected eleven nonempty source/contract operations');
  require(JSON.stringify(fromSource.map(key).sort()) === JSON.stringify(operations.map(key).sort()), 'Source/contract method/path inventory drift');
  require(new Set(operations.map(o => o.operation.operationId)).size === operations.length, 'Duplicate operationId');
  const ids = ['getDemoHealth', 'loginDemo', 'getDemoCurrentUser', 'getDemoCurrentMerchant', 'listDemoShipments',
    'createDemoShipment', 'getDemoShipment', 'cancelDemoShipment', 'listDemoUsers', 'getDemoOverview', 'listDemoShipmentSummaries'];
  require(JSON.stringify(operations.map(o => o.operation.operationId).sort()) === JSON.stringify(ids.sort()), 'Approved operationId inventory drift');
  require(document.components?.securitySchemes?.bearerAuth?.type === 'http' &&
    document.components.securitySchemes.bearerAuth.scheme === 'bearer' &&
    document.components.securitySchemes.bearerAuth.bearerFormat === 'JWT', 'JWT bearer scheme drift');
  const security = source('identity/internal/infrastructure/DemoSecurity.java');
  const matchers = [...security.matchAll(/\.requestMatchers\(HttpMethod\.(GET|POST),([^)]*)\)\.(permitAll\(\)|authenticated\(\)|hasRole\("([^"]+)"\))/g)]
    .map(match => ({ method: match[1].toLowerCase(), paths: [...match[2].matchAll(/"([^"]+)"/g)].map(p => p[1]),
      role: match[3] === 'permitAll()' ? 'PUBLIC' : match[3] === 'authenticated()' ? 'AUTHENTICATED' : match[4] }));
  const matchesPath = (pattern, path) => new RegExp('^' + pattern.split('**').map(part =>
    part.split('*').map(s => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('[^/]*')).join('.*') + '$').test(path);
  for (const { method, path, operation: op } of operations) {
    const publicOperation = path.endsWith('/health') || path.endsWith('/auth/login');
    require(JSON.stringify(op.security) === JSON.stringify(publicOperation ? [] : [{ bearerAuth: [] }]), `${key({method,path})}: explicit auth drift`);
    const role = publicOperation ? 'PUBLIC' : path.endsWith('/auth/me') ? 'AUTHENTICATED' : path.includes('/admin/') ? 'ADMIN' : 'MERCHANT';
    require(JSON.stringify(op['x-roles']) === JSON.stringify([role]), `${path}: RBAC metadata drift`);
    const sourceRole = matchers.find(m => m.method === method && m.paths.some(p => matchesPath(p, path)))?.role;
    require(sourceRole === role, `${path}: source security matcher drift`);
    const success = method === 'post' && path.endsWith('/shipments') ? '201' : '200';
    require(Boolean(op.responses?.[success]), `${path}: success status ${success} missing`);
    const successSchema = op.responses?.[success]?.content?.['application/json']?.schema;
    const shape = responseShapes[op.operationId];
    require(shape?.endsWith('[]')
      ? successSchema?.type === 'array' && successSchema.items?.$ref === `#/components/schemas/${shape.slice(0, -2)}`
      : successSchema?.$ref === `#/components/schemas/${shape}`, `${path}: success response schema drift`);
    if (requestShapes[op.operationId]) require(op.requestBody?.content?.['application/json']?.schema?.$ref ===
      `#/components/schemas/${requestShapes[op.operationId]}`, `${path}: request schema drift`);
    const expectedStatuses = [success, '401', '500', 'default'];
    if (!publicOperation) expectedStatuses.push('403');
    if (method === 'post' || path.includes('{id}')) expectedStatuses.push('400');
    if (path.includes('{id}')) expectedStatuses.push('404');
    if (method === 'post' && !publicOperation) expectedStatuses.push('409');
    if (op.operationId === 'loginDemo') expectedStatuses.push('429');
    require(JSON.stringify(Object.keys(op.responses ?? {}).sort()) === JSON.stringify(expectedStatuses.sort()), `${path}: HTTP status inventory drift`);
    const responseRefs = { '400': method === 'post' && !publicOperation ? 'InvalidMutation' : 'InvalidRequest',
      '401': op.operationId === 'loginDemo' ? 'LoginUnauthorized' : publicOperation ? 'InvalidToken' : 'Unauthenticated',
      '403': role === 'MERCHANT' ? 'MerchantForbidden' : 'Forbidden', '404': 'NotFound',
      '409': path.endsWith('/cancel') ? 'CancelConflict' : 'CreateConflict', '429': 'RateLimited',
      '500': 'DatabaseError', default: 'UnexpectedError' };
    for (const status of expectedStatuses.filter(s => s !== success)) require(op.responses?.[status]?.$ref ===
      `#/components/responses/${responseRefs[status]}`, `${path}: ${status} error response binding drift`);
    if (success === '201') require(op.responses?.['201']?.headers?.Location?.required === true &&
      op.responses['201'].headers.Location.schema?.format === 'uri-reference', `${path}: create Location drift`);
    const parameters = (op.parameters ?? []).map(p => p.$ref ? resolveLocal(document, p.$ref) : p);
    require(parameters.every(p => p.in !== 'query'), `${path}: invented query parameter`);
    if (path.includes('{id}')) require(parameters.some(p => p.name === 'id' && p.in === 'path' && p.required && p.schema?.format === 'uuid'), `${path}: UUID path constraint missing`);
    if (method === 'post' && !publicOperation) {
      const header = parameters.find(p => p.name === 'Idempotency-Key' && p.in === 'header');
      require(header?.required && header.schema?.maxLength === 100 && header.schema?.minLength === 1 &&
        header.schema?.pattern === '^[A-Za-z0-9._:-]{1,100}$(?![\\s\\S])', `${path}: required idempotency key constraint drift`);
      require(Boolean(op.responses?.['409']), `${path}: conflict response missing`);
      const scope = path.endsWith('/cancel') ? 'merchant+cancel:shipmentId+key' : 'merchant+create+key';
      require(op['x-idempotency']?.scope === scope && op['x-idempotency']?.successStatus === Number(success) &&
        Boolean(op['x-idempotency']?.replay), `${path}: idempotency semantics drift`);
    }
    if (path.includes('/shipments/{id}')) require(op['x-ownership'] === 'merchant-from-trusted-principal; cross-owner=404', `${path}: ownership404 semantics missing`);
    if (method === 'post') require(op.requestBody?.required === true, `${path}: required JSON body missing`);
    for (const status of publicOperation ? ['401'] : ['401', '403']) require(Boolean(op.responses?.[status]), `${path}: missing${status}`);
  }
  const allSource = readdirSync(javaRoot, { recursive: true, withFileTypes: true })
    .filter(entry => entry.isFile() && entry.name.endsWith('.java'))
    .map(entry => readFileSync(resolve(entry.parentPath, entry.name), 'utf8')).join('\n');
  for (const [name, codes] of Object.entries(errorCodes)) {
    const response = document.components?.responses?.[name];
    require(JSON.stringify(response?.['x-error-codes']) === JSON.stringify(codes), `${name}: error codes drift`);
    require(response?.content?.['application/json']?.schema?.$ref === '#/components/schemas/ErrorBody', `${name}: flat error schema drift`);
    for (const code of codes) {
      const example = response?.content?.['application/json']?.examples?.[code]?.value;
      require(example?.code === code && typeof example.message === 'string' &&
        allSource.includes(`"${code}"`) && allSource.includes(`"${example.message}"`), `${name}.${code}: source error example drift`);
    }
  }
  const schemas = document.components?.schemas ?? {};
  for (const [name, [file, record]] of Object.entries(records)) {
    const fields = recordFields(source(file), record), schema = schemas[name];
    require(fields.length > 0 && !!schema, `Missing nonempty schema/source record ${name}`);
    if (!schema) continue;
    require(JSON.stringify(Object.keys(schema.properties ?? {}).sort()) === JSON.stringify(fields.map(f => f.name).sort()), `${name}: source property set drift`);
    require(JSON.stringify([...(schema.required ?? [])].sort()) === JSON.stringify(fields.map(f => f.name).sort()), `${name}: required properties drift`);
    require(schema.additionalProperties === false, `${name}: unexpected wire properties allowed`);
    for (const field of fields) {
      const p = schema.properties?.[field.name]; if (!p) continue;
      const value = p.$ref ? resolveLocal(document, p.$ref) : p;
      const types = Array.isArray(value.type) ? value.type : [value.type];
      const wanted = { UUID: 'string', String: 'string', boolean: 'boolean', int: 'integer', long: 'integer', BigDecimal: 'number', Instant: 'string', 'List<ShipmentEvent>': 'array', AccountView: 'object', ShipmentView: 'object' }[field.type];
      require(types.includes(wanted), `${name}.${field.name}: source type drift`);
      const nullable = (name === 'AccountView' && field.name === 'merchantId') || (name === 'ShipmentEvent' && field.name === 'reason');
      require(types.includes('null') === nullable, `${name}.${field.name}: nullability drift`);
      if (field.type === 'UUID') require(value.format === 'uuid', `${name}.${field.name}: UUID format drift`);
      if (field.type === 'Instant') require(value.format === 'date-time', `${name}.${field.name}: time format drift`);
      const max = field.annotations.match(/@Size\(max=(\d+)\)/)?.[1];
      if (max) require(value.maxLength === Number(max), `${name}.${field.name}: @Size drift`);
      if (field.annotations.includes('@NotBlank')) require(value.minLength === 1 && value['x-java-not-blank'] === true &&
        value.allOf?.some(s => s.pattern === nonBlankPattern), `${name}.${field.name}: @NotBlank drift`);
      const regex = field.annotations.match(/@Pattern\(regexp="([^"]+)"\)/)?.[1];
      if (regex) require(value.pattern === `^(?:${regex})$(?![\\s\\S])`, `${name}.${field.name}: Java full-match pattern drift`);
    }
  }
  const weightAnnotations = recordFields(source(records.DraftInput[0]), 'DraftInput').find(f => f.name === 'weightKg').annotations;
  const minimum = Number(weightAnnotations.match(/@DecimalMin\("([^"]+)"\)/)?.[1]);
  const maximum = Number(weightAnnotations.match(/@DecimalMax\("([^"]+)"\)/)?.[1]);
  const fraction = Number(weightAnnotations.match(/@Digits\(integer=\d+,fraction=(\d+)\)/)?.[1]);
  require(schemas.DraftInput?.properties?.weightKg?.minimum === minimum && schemas.DraftInput?.properties?.weightKg?.maximum === maximum && schemas.DraftInput?.properties?.weightKg?.multipleOf === 10 ** -fraction, 'Weight bounds/precision drift');
  const health = source('platform/internal/web/DemoHealth.java');
  require(health.includes('"status","UP","scope","local-demo"') && schemas.DemoHealth?.properties?.status?.const === 'UP' && schemas.DemoHealth?.properties?.scope?.const === 'local-demo', 'Existing constant demo health drift');
  require(source('orders/internal/application/ShipmentService.java').includes('[A-Za-z0-9._:-]{1,100}'), 'Source idempotency pattern changed');
  return problems;
}
