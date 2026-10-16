# Private generated Fetch SDK

C-06 freshly verifies contract/SDK85, web24/browser7 twice, mobile compiler4/
Hermes/Jest1, architecture23/H2 40+8 skips/PG48+0 and guard14. Actual two generated
outputs and source match raw16-file bytes. Local backend helper:
`python -B scripts/check-backend-baselines.py` from this package. It reuses CI
provisioning on an available test-only5433; never stop an existing service.
[Final evidence/known risks](../../docs/implementation/d1-07-c06-final-evidence.md).
No official integrated DONE/remote PASS or security-audit-clean claim.

Current checkpoint: **C-01-06 local VERIFIED; D1-07 PARTIAL / READY_FOR_PR**.
The private package exports all eleven generated operations/types at
`@elogistics/api-client`, its default `client`, and Fetch factories/config/types
at `@elogistics/api-client/client`. ESM JavaScript and declarations are built into
ignored dist; NodeNext and Bundler resolution are tested at package level.
Web now consumes public generated operations/types through a minimal ApiError
adapter/local file dependency. Mobile now has a compile-only public-export
fixture/local dependency, without starter UI/network changes. Its actual Expo
compiler/four negative controls and separate SDK-entry Metro/Hermes bundle PASS
(23 modules/14 SDK); unchanged starter export PASS576 modules/zero SDK modules.
[C-05 evidence](../../docs/implementation/d1-07-c05-mobile-evidence.md).
Mobile audits remain FAIL/OPEN (current58/omit-dev35; prior C-05 snapshot59), SDK dev4 high still OPEN.
C-06 combined local gates PASS; remote checks/integration UNVERIFIED; no device runtime claim.
[C-04 evidence](../../docs/implementation/d1-07-c04-web-evidence.md): web24 adapter
and7 real-browser tests PASS, contract/SDK85 and wire27 PASS. No global token config.

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
# Real React web + Edge on owned PostgreSQL/backend; web npm ci required:
npm.cmd run test:web
# Mobile npm ci required first; actual Expo compiler + SDK-entry Metro/Hermes:
node scripts/check-mobile-consumer.mjs
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
