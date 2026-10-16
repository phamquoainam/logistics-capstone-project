import { generateFresh, snapshot, generatedRoot, compareSnapshots, digest } from './generation-lib.mjs';

const first = await generateFresh(), second = await generateFresh();
const differences = [
  ...compareSnapshots(first, second).map(message => `Two-directory determinism: ${message}`),
  ...compareSnapshots(first, snapshot(generatedRoot)).map(message => `Source drift: ${message}`),
];
if (differences.length) throw new Error(differences.join('\n'));
console.log(`PASS: two distinct scratch generations and source match byte-for-byte (${first.size} files); SHA256 ${digest(first)}`);
