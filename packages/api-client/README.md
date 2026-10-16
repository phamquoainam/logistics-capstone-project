# Private generated Fetch SDK

Current checkpoint: **C-01 accepted; C-02/C-03 local DONE; C-04–06 NOT STARTED**.
The private package exports all eleven generated operations/types at
`@elogistics/api-client`, its default `client`, and Fetch factories/config/types
at `@elogistics/api-client/client`. ESM JavaScript and declarations are built into
ignored dist; NodeNext and Bundler resolution are tested at package level.
Web/mobile manifests/source/locks are unchanged; no consumer integration is claimed.

Exact devDependencies: HeyAPI0.99.0, TypeScript6.0.3, Redocly2.64.0,
Ajv8.20.0, formats3.0.1. There are no runtime npm dependencies and no separately
installed client-fetch package. The same-named plugin bundles Fetch/core source
from the MIT-licensed generator; keep its generated headers/provenance. Project
licensing is not assigned by this package.

Node24.13.0 / npm11.6.2 are exact engines with engine-strict enabled. From this
directory:

```powershell
npm.cmd ci --no-audit --no-fund
npm.cmd run contract:lint
npm.cmd run contract:inventory
npm.cmd test
# Or run all three gates, failing on the first error:
npm.cmd run verify:c01
npm.cmd run check:generated
npm.cmd run typecheck
npm.cmd run build
npm.cmd run test:sdk
# Complete C-01/C-02 local gate; does not rewrite generated source:
npm.cmd run verify:c02
# Real HTTP/PG test; requires current backend JAR, Docker and Python:
npm.cmd run test:wire
npm.cmd audit --json
npm.cmd audit --omit=dev --json
```

After an intentional, reviewed contract/config change, `npm.cmd run generate`
recreates only src/generated from fresh scratch output. Never hand-edit those
files. Unexpected/stale extra files cause refusal, not deletion. The verification
command generates into two distinct ignored directories and compares exact file
inventories/bytes against each other and src/generated. It never rewrites source
to make a drift check pass. Current output:16 UTF-8/LF files, no timestamps or
machine-specific configuration. Comparison does not normalize line endings or
strip any content. With Git core.autocrlf=true, a later checkout may convert LF;
review an LF-only failure and explicitly regenerate before rerunning the byte
gate. No Git/global settings or acceptance criteria are changed here.

Generation reads the single YAML source through Redocly, checks source invariants
and local refs, then passes the resulting object to HeyAPI. A parser patch adds
only a redundant string type to pattern-only allOf branches where the parent
already asserts string: it avoids0.99.0 emitting unknown request fields without
removing any constraint or changing the source contract. Schema equivalence
fixtures and compiler negatives protect this configuration. Config is awaited
because defineConfig returns a Promise in this pinned version.

Default base is the contract's relative `/api/demo/v1`; use a per-consumer client
with an explicit origin/base for non-browser environments. Auth is supplied per
request/client at runtime; public operations do not attach configured bearer auth.
No retries, input normalization, client authorization or runtime schema validator
are added. SDK transport fixtures inject Fetch responses; they are not HTTP
backend conformance or web ApiError adapter tests.

**Audit:** full dev graph reports4 high package findings through js-yaml4.2.0;
runtime audit reports0. Findings remain OPEN, not fixed/accepted automatically.
The supported wrapper feeds HeyAPI an object with local refs, avoiding its YAML
loader; an isolated positive-control test blocks that loader and actual generation
still succeeds with zero calls. This is a bounded mitigation, not an audit PASS
or a claim about arbitrary CLI use/untrusted YAML/Redocly bundled code. Do not
replace the wrapper with direct YAML CLI generation without reviewing this risk.
No override/audit fix/version fallback is applied.

`contract:lint` uses the pinned local CLI/config, requires zero errors/warnings/
ignored reports, and does not use a downloaded npx version. `contract:inventory`
compares the contract to current Java source plus source-reviewed invariants.
`test` uses Node's built-in test runner, the same source guard, Redocly config
and strict Ajv2020 + formats. Ajv does not coerce, insert defaults or strip fields.
Decimal fixture checks account for floating-point multiplication precision;
Java BigDecimal and actual wire validation are checked separately by C-03's
actual HTTP/PG fixtures; schema-only tests do not replace that evidence.

Bundle JSON and intentionally invalid fixture documents are validation scratch
files in the ignored `.demo/d1-07-c01/` directory. They are not generated SDK
output. C-02 scratch files stay in .demo/d1-07-c02; generated TypeScript source
under src/generated is intended for review/tracking. `node_modules`, dist and
scratch logs are ignored. Nothing here connects to
a developer database or installs an application/runtime validator.

C-03's separate owned harness creates a fresh labeled PG18.6 tmpfs container,
random loopback ports and synthetic credentials, runs the actual backend with
demo,postgres, and cleans only its captured process/container ID. It never reads
developer DB credentials/data, deletes volumes or changes migrations. Build the
current backend JAR first; verify:c02 builds the SDK before real wire tests.
Twenty-seven scenarios /108 HTTP responses per final run PASS twice, with real
SQL invariants and exact schema/status/header/error checks. Secrets/raw backend
logs remain ignored; safe results contain only allowlisted case/status outcomes.
See [C-03 evidence](../../docs/implementation/d1-07-c03-wire-evidence.md).

See [contract notes](../../contracts/openapi/README.md) for canonical input and
license-metadata limitations, [C-01 evidence](../../docs/implementation/d1-07-c01-contract-evidence.md)
and [C-02 evidence](../../docs/implementation/d1-07-c02-sdk-evidence.md).
