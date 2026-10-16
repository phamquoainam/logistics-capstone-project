import { checkContract, loadContract } from './contract-lib.mjs';
const problems = checkContract(loadContract());
if (problems.length) { console.error(problems.join('\n')); process.exitCode = 1; }
else console.log('PASS: eleven source/contract operations, records, auth, parameters and status invariants');
