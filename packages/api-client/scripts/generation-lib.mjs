import { readdirSync, readFileSync, mkdirSync, writeFileSync, existsSync, lstatSync, mkdtempSync } from 'node:fs';
import { resolve, relative, dirname, sep } from 'node:path';
import { createHash } from 'node:crypto';
import { createClient } from '@hey-api/openapi-ts';
import configured from '../openapi-ts.config.ts';
import { packageRoot, repoRoot, loadContract, checkContract, resolveLocal } from './contract-lib.mjs';

export const generatedRoot = resolve(packageRoot, 'src/generated');
export const generationConfig = await configured;

export function sdkScratch(prefix = 'sdk-') {
  const parent = resolve(repoRoot, '.demo/d1-07-c02'); mkdirSync(parent, { recursive: true });
  return mkdtempSync(resolve(parent, prefix));
}

export function checkGenerationConfig(value) {
  const problems = [];
  const plugins = value.plugins ?? [];
  const names = plugins.map(plugin => typeof plugin === 'string' ? plugin : plugin.name);
  if (JSON.stringify(names) !== JSON.stringify(['@hey-api/typescript', '@hey-api/sdk', '@hey-api/client-fetch'])) problems.push('Expected only types, SDK and bundled Fetch plugins');
  const fetch = plugins.find(p => typeof p === 'object' && p.name === '@hey-api/client-fetch');
  if (fetch?.bundle !== true || fetch?.baseUrl !== true) problems.push('Bundled Fetch must use the contract server prefix');
  if (value.output?.module?.extension !== '.js' || value.output?.clean !== false) problems.push('ESM extension or non-destructive output policy drift');
  if (value.input !== '../../contracts/openapi/demo-001.yaml') problems.push('Only the reviewed local contract input is allowed');
  return problems;
}

export function assertLocalRefs(document) {
  function visit(value) {
    if (!value || typeof value !== 'object') return;
    if (typeof value.$ref === 'string') resolveLocal(document, value.$ref);
    for (const child of Object.values(value)) visit(child);
  }
  visit(document);
}

export function snapshot(directory) {
  if (!existsSync(directory)) throw new Error(`Missing generated directory: ${directory}`);
  if (lstatSync(directory).isSymbolicLink()) throw new Error('Generated directory must not be a link');
  const result = new Map();
  function visit(folder) {
    for (const entry of readdirSync(folder, { withFileTypes: true }).sort((a, b) => a.name.localeCompare(b.name, 'en'))) {
      const file = resolve(folder, entry.name);
      if (entry.isSymbolicLink()) throw new Error(`Unexpected generated link: ${file}`);
      if (entry.isDirectory()) visit(file);
      else if (entry.isFile()) result.set(relative(directory, file).split(sep).join('/'), readFileSync(file));
      else throw new Error(`Unexpected generated entry: ${file}`);
    }
  }
  visit(directory);
  if (!result.size) throw new Error('Generated output cannot be empty');
  return result;
}

export function compareSnapshots(expected, actual) {
  const differences = [];
  for (const name of new Set([...expected.keys(), ...actual.keys()])) {
    if (!expected.has(name)) differences.push(`Unexpected file: ${name}`);
    else if (!actual.has(name)) differences.push(`Missing file: ${name}`);
    else if (!expected.get(name).equals(actual.get(name))) differences.push(`Changed bytes: ${name}`);
  }
  return differences;
}

export function digest(files) {
  const hash = createHash('sha256');
  for (const name of [...files.keys()].sort()) {
    const bytes = files.get(name);
    hash.update(`${name.length}:${name}:${bytes.length}:`); hash.update(bytes);
  }
  return hash.digest('hex');
}

export async function generateFresh() {
  const config = generationConfig;
  const document = loadContract();
  assertLocalRefs(document);
  const problems = [...checkGenerationConfig(config), ...checkContract(document)];
  if (problems.length) throw new Error(problems.join('\n'));
  // Only a fresh task-owned scratch directory reaches the generator.
  const output = resolve(sdkScratch(), 'generated');
  // Redocly reads the single YAML source; HeyAPI receives a checked object.
  // No YAML string/file or external ref reaches its js-yaml dependency.
  await createClient({ ...config, input: document,
    output: { ...config.output, path: output, tsConfigPath: resolve(packageRoot, 'tsconfig.json') } });
  return snapshot(output);
}

export function writeGenerated(files) {
  if (existsSync(generatedRoot)) {
    const current = snapshot(generatedRoot);
    const extra = [...current.keys()].filter(name => !files.has(name));
    if (extra.length) throw new Error(`Refusing to delete unexpected/stale generated files: ${extra.join(', ')}`);
  }
  for (const [name, bytes] of files) {
    const file = resolve(generatedRoot, name);
    if (!file.startsWith(generatedRoot + sep)) throw new Error('Generated path escaped its directory');
    mkdirSync(dirname(file), { recursive: true }); writeFileSync(file, bytes);
  }
}
