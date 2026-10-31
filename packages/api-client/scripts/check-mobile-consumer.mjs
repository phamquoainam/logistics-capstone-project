// Verification-only Node tooling. Never exported to or imported by the app.
import { createRequire } from 'node:module';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve, dirname, relative } from 'node:path';
import { spawnSync } from 'node:child_process';
import { randomUUID, createHash } from 'node:crypto';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../../..');
const mobile = resolve(root, 'app/frontend/mobile');
const requireMobile = createRequire(resolve(mobile, 'package.json'));
const ts = requireMobile('typescript');
const output = resolve(root, '.demo/d1-07-c05', `sdk-${randomUUID()}`);
mkdirSync(output, { recursive: true });
function check(value, message) { if (!value) throw new Error(message); }

const parsed = ts.getParsedCommandLineOfConfigFile(resolve(mobile, 'tsconfig.json'), {}, ts.sys);
check(parsed && !parsed.errors.length, 'Actual Expo tsconfig must load');
const fixture = resolve(mobile, 'type-tests/api-client.ts');
check(parsed.fileNames.includes(fixture.replaceAll('\\', '/')) || parsed.fileNames.includes(fixture), 'Mobile fixture must be in the normal typecheck');
const diagnostics = ts.getPreEmitDiagnostics(ts.createProgram(parsed.fileNames, parsed.options));
check(!diagnostics.length, 'Actual Expo/RN compiler must pass before negative controls');
check(parsed.options.moduleResolution === ts.ModuleResolutionKind.Bundler && parsed.options.customConditions.includes('react-native'), 'Retain Expo Bundler/react-native resolution');

const virtual = resolve(mobile, 'type-tests/__c05_negative_control__.ts');
const controls = [
  ['missing idempotency header', "consumer.createShipment(token, body, {});", 2345],
  ['wrong numeric weight', "consumer.createShipment(token, { ...body, weightKg: 'wrong' }, headers);", 2322],
  ['wrong path type', 'consumer.shipment(token, 123);', 2345],
  ['unexported generated subpath', "import { createDemoShipment } from '@elogistics/api-client/src/generated/sdk.gen.js'; void createDemoShipment;", 2307],
];
const controlResults = [];
for (const [name, statement, code] of controls) {
  const source = `import { createMobileContractConsumer } from './api-client';
declare const consumer: ReturnType<typeof createMobileContractConsumer>;
declare const token: string;
declare const body: Parameters<typeof consumer.createShipment>[1];
declare const headers: Parameters<typeof consumer.createShipment>[2];
${statement}`;
  const host = ts.createCompilerHost(parsed.options);
  const read = host.readFile.bind(host), exists = host.fileExists.bind(host);
  host.readFile = path => resolve(path) === virtual ? source : read(path);
  host.fileExists = path => resolve(path) === virtual || exists(path);
  const errors = ts.getPreEmitDiagnostics(ts.createProgram([...parsed.fileNames, virtual], { ...parsed.options, noUncheckedSideEffectImports: true }, host));
  check(errors.length === 1 && errors[0].code === code && errors[0].file && resolve(errors[0].file.fileName) === virtual, `Negative compiler control failed: ${name}; observed codes ${errors.map(error => error.code).join(',')}`);
  controlResults.push({ name, detected: true, diagnostic: code });
}

const bundle = resolve(output, 'sdk.android.hbc'), mapPath = resolve(output, 'sdk.android.hbc.map');
const args = [resolve(mobile, 'node_modules/expo/bin/cli'), 'export:embed', '--platform', 'android',
  '--entry-file', 'type-tests/api-client.ts', '--dev', 'false', '--bytecode', '--max-workers', '2',
  '--bundle-output', bundle, '--sourcemap-output', mapPath];
const result = spawnSync(process.execPath, args, { cwd: mobile, encoding: 'utf8', timeout: 180_000,
  env: { ...process.env, CI: '1', EXPO_OFFLINE: '1', EXPO_NO_DOTENV: '1', EXPO_NO_TELEMETRY: '1' } });
writeFileSync(resolve(output, 'metro-private.log'), (result.stdout ?? '') + (result.stderr ?? ''));
check(result.status === 0 && !result.error, 'Actual Metro/Hermes SDK entry failed; inspect ignored diagnostic log');
const bytes = readFileSync(bundle);
// Same HBC header checked by the installed Expo CLI exportHermes implementation.
check(bytes.subarray(0, 8).toString('hex') === 'c61fbc03c103191f', 'Expected actual Hermes bytecode, not plain JS');
function sourcesOf(map) { return [...(map.sources ?? []), ...(map.sections ?? []).flatMap(section => sourcesOf(section.map))]; }
const sources = [...new Set(sourcesOf(JSON.parse(readFileSync(mapPath, 'utf8'))).map(path => path.replaceAll('\\', '/')))];
const sdkSources = sources.filter(path => path.includes('packages/api-client/dist/'));
for (const required of ['index.js', 'generated/sdk.gen.js', 'generated/client/client.gen.js']) {
  check(sdkSources.some(path => path.endsWith(`/dist/${required}`)), `Metro must include public SDK value module: ${required}`);
}
check(sources.some(path => path.endsWith('type-tests/api-client.ts')), 'Actual mobile consumer entry missing');
const toolingPattern = /(?:node_modules\/(?:@hey-api\/|@redocly\/|ajv(?:-formats)?\/|typescript\/|js-yaml\/))|packages\/api-client\/(?:scripts|tests)\//;
check(!sources.some(path => toolingPattern.test(path)), 'Node-only contract tooling must not enter mobile SDK graph');
for (const path of sdkSources) {
  const suffix = path.split('packages/api-client/dist/')[1];
  const content = readFileSync(resolve(root, 'packages/api-client/dist', suffix), 'utf8');
  for (const match of content.matchAll(/\bfrom\s+['"]([^'"]+)['"]/g)) {
    check(match[1].startsWith('.'), 'SDK runtime must use only bundled relative imports, no Node/npm runtime dependency');
  }
}
const report = { status: 'PASS', compiler: { version: ts.version, resolution: 'Bundler', conditions: parsed.options.customConditions,
  actualProject: true, negativeControls: controlResults }, metro: { realEntry: 'type-tests/api-client.ts', hermes: true,
  modules: sources.length, sdkModules: sdkSources.length, sdkSources: sdkSources.map(path => path.split('packages/api-client/')[1]).sort(),
  toolingModules: 0, bytes: bytes.length, sha256: createHash('sha256').update(bytes).digest('hex') },
  businessRequestsExecuted: 0, starterEntryChanged: false };
writeFileSync(resolve(output, 'safe-mobile-results.json'), JSON.stringify(report, null, 2) + '\n');
console.log(`PASS: actual Expo/RN compiler + ${controls.length} negative controls; actual Metro/Hermes SDK entry ${sources.length} modules/${sdkSources.length} SDK modules; no tooling imports`);
console.log(`Ignored evidence: ${relative(root, output)}`);
