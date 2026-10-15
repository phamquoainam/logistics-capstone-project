import { contractPath, redocly } from './contract-lib.mjs';
const result = redocly(['lint', contractPath, '--format', 'json']);
process.stdout.write(result.output);
if (result.status !== 0) process.exit(1);
const report = JSON.parse(result.stdout);
if (report.totals.errors || report.totals.warnings || report.totals.ignored) process.exit(1);
