# D1-07 — C-01 final acceptance / C-02 deterministic generated SDK

**C-06 cross-reference - 2026-10-17 /1f26854:** this is the original dated
checkpoint evidence, not a claim all gates below ran again at its original date.
[C-06 final combined evidence](d1-07-c06-final-evidence.md) freshly verifies
C-01-06 local gates, actual PG48/no skips and HTTP/browser repeatability;
C-05 accepted/committed as1f26854. D1-07 PARTIAL / READY_FOR_PR; remote CI/integration
UNVERIFIED. Current audits web0/0, SDK4-high/0, mobile58/35; prior snapshot59 is
retained historically, not silently rewritten as remediation. C-06 uncommitted.

Execution2026-10-15, existing feature/d1-07-demo-contract-client,
HEAD20072b65cc183153bd26a614103ab9e8a442a713 unchanged.
**C-01 final technical acceptance DONE; C-02 local implementation/verification
DONE. Dev dependency advisories OPEN. C-03–06 NOT STARTED; D1-07, full001 and
Day1 remain PARTIAL.** No commit/push/PR/merge.

## Authorization and C-01 final acceptance

The user's attached task “D1-07 — C-01 Final Acceptance & C-02 Deterministic
Generated SDK” explicitly authorizes C-02 on the same branch if C-01 gates PASS.
No extra branch approval is needed. Initial inventory matches the nineteen
uncommitted C-01 files, with no unrelated changes. A SHA256 manifest saved in
ignored .demo/d1-07-c02/c01-checkpoint.json preserves their starting fingerprints.
Existing Git work was not reset, stashed, deleted or replaced by another baseline.

Re-read AGENTS/DoR/principles, DEC-29/ADR-0003 and slice spec/design/tasks.
Reviewed the actual OAS, four controllers, DTO/records, token/security/login,
merchant and order service behavior and existing backend test assertions. Exact
eleven method/path/operationIds, types/nullable properties, flat responses/errors,
status/header/validation, JWT/public endpoints, matcher RBAC, trusted owner and
idempotent replay match the reviewed source. No added endpoint/envelope/business
behavior or serious contract/security mismatch found. Source/static checks are
bounded, and actual HTTP serialization remains mandatory C-03 evidence.

Independent controls use source-derived route/record checks plus source-reviewed
expectations, actual strict Ajv fixtures and the real pinned Redocly CLI. Negative
tests assert specific diagnostics/rules, not just process success. Final C-01
regression:41 pass,0 fail/skip;37 dedicated negative fixtures;13 component and18
application-error examples. Only publishing metadata info-license is disabled;
repository license is not approved, so none is invented. Structural/ref/security/
operation/unused-component validation and fail-on-warning policy remain enabled.

**author-contract.py disposition:** reviewed beginning/output logic and callers.
This is one-time ignored scratch authoring code, not required package tooling;
its older draft can overwrite the accepted YAML and lacks later refinements.
Keep it ignored/untracked and do not run/promote it. No package/CI command calls
it, and it is not executed or deleted in this task. The YAML is the sole reviewed
schema source; Redocly bundle output is temporary parsed data, not a second source.

C-01 contract SHA256 remains exactly:
`7944466e247e85069ba747ed4d62dac2f2891dd8fff64d8fa160f6ed22d44b82`.
Its scripts/tests/redocly config remain byte-identical to the starting checkpoint.
Only approved package metadata/lock and current documentation evolve for C-02.

## Package implementation and compatibility finding

Private @elogistics/api-client0.0.0, ESM, exact Node24.13.0/npm11.6.2 strict engines.
Five exact dev pins: @hey-api/openapi-ts0.99.0, TypeScript6.0.3,
@redocly/cli2.64.0, Ajv8.20.0 and formats3.0.1. No production npm dependencies,
deprecated standalone Fetch installation, overrides or consumer TS change.

Package entry exports generated operations/types plus default client; ./client
exports generated Fetch factories/config/types. TypeScript target ES2022,
DOM/DOM.Iterable, strict NodeNext, explicit .js imports, declarations/noEmitOnError,
skipLibCheck=false. dist is ignored; no source map/build outputs tracked. Actual
package declarations are compiled with NodeNext and Bundler resolver fixtures.
This is not an assertion that RN/browser projects have adopted or built the SDK.

**Found and fixed in C-02:** the first generated request properties became
unknown for pattern-only allOf branches despite an enclosing string type. A
positive compiler fixture genuinely FAILED four string equality assertions even
though the package itself compiled. No generated code or contract was edited to
hide this. The source-reviewed parser configuration adds only redundant
type=string to those branches; existing pattern/maxLength/required remain intact.
Eleven affected properties, original examples and invalid/valid field variations
have matching strict Ajv acceptance before/after enrichment. Compiler fixtures
now require exact strings, numeric weight, writeOnly login password input and
nullable merchant/reason outputs. defineConfig is awaited; the configuration
guard also caught an initial unawaited Promise instead of silently accepting
generator defaults. Final output is entirely regenerated from the fixed config.

Generated artifact inventory:16 TypeScript files, including sdk.gen.ts,
types.gen.ts, client.gen.ts, entry, four client/ and eight core/ files. The CLI's
“4 files” status counts its top-level artifacts; recursive snapshot counts all16.
Schema/source-level operations exported and executed in Fetch fixtures:

```text
getDemoHealth             GET  /health
loginDemo                 POST /auth/login
getDemoCurrentUser        GET  /auth/me
getDemoCurrentMerchant    GET  /merchant/me
listDemoShipments         GET  /shipments
createDemoShipment        POST /shipments
getDemoShipment           GET  /shipments/{id}
cancelDemoShipment        POST /shipments/{id}/cancel
listDemoUsers             GET  /admin/users
getDemoOverview           GET  /admin/overview
listDemoShipmentSummaries GET  /admin/shipments
```

Prefix is the schema-relative /api/demo/v1, no local machine URL/token/merchant ID.
Tests use contract.invalid with injected Fetch, synthetic credentials and schema
examples; no network or backend DB is contacted by SDK request tests. The bundled
client/core retains upstream casts/internal any types; no handwritten operation,
successful-response cast or manual generic HTTP replacement was introduced.

## Determinism, commands and results

All npm commands run in packages/api-client. `npm.cmd ci --no-audit --no-fund`
PASS:59 packages, no peer/engine override. Full local gate `npm.cmd run verify:c02`
PASS after clean install. It calls the following commands without generating over
the checked source:

| Actual gate | Result |
|---|---|
| `npm.cmd run verify:c01` | PASS: strict lint0/0/0, inventory11,41 tests/no skips |
| `npm.cmd run check:generated` | PASS: two distinct fresh scratch outputs and source match all16 paths/bytes |
| `npm.cmd run typecheck` | PASS, TS6.0.3 strict |
| `npm.cmd run build` | PASS, real ESM JS/declarations |
| `npm.cmd run test:sdk` | PASS:44 tests,0 fail/skip/cancel/todo |
| `npm.cmd run generate` | PASS: repeated explicit regeneration produces the same16-file digest |
| `npm.cmd audit --json` | FAIL/advisory:4 high package findings,0 critical; not hidden |
| `npm.cmd audit --omit=dev --json` | PASS:0 findings, no runtime dependencies |

Total final automated tests: **85 passed,0 failed/skipped** across C-01 and C-02.
The verify:c02 command does not include npm audit; the separate audit command
retains its real nonzero result. It is not a zero-vulnerability gate claim.

Two-directory/source file-inventory+raw-byte SHA256 aggregate:
`e3ce1e183afbeb7f9cb14f70674c5a475adafb069a125fe9bf969fdcfe0ecfe6`.
Names are sorted and length-delimited before hashing; contents are not normalized,
stripped or timestamp-filtered. No generated timestamp/absolute host path exists.
Canonical generated files are UTF-8/LF. Git core.autocrlf=true is unchanged; a
future checkout converting LF may fail the byte gate until an explicit reviewed
regeneration restores canonical bytes. No cross-OS checkout PASS is claimed.

The44 SDK tests cover: two positive declaration resolvers; ten negative compiler
fixtures with actual expected TS diagnostic codes (missing create/cancel key,
missing/wrong path, wrong login/body/weight, nonexistent operation/path/query);
schema-preserving parser enrichment; actual generation with YAML loader blocked;
external/unresolved local-ref rejection; five negative configuration fixtures
(missing SDK, unbundled client, bad extension, destructive cleanup, remote source);
three actual isolated filesystem drift fixtures (changed/missing/extra files);
exact eleven exported functions; eleven generated Fetch calls; explicit retry
key/input/per-request auth preservation; seven injected400/401/403/404/409/429/500
responses; nonJSON/network/throwOnError failure behavior without automatic retries.

Initial negative tests used inaccurate expected diagnostic numbers for missing
cancel key/nonexistent export. Actual TS6 diagnostics2322/2305 correctly rejected
them; fixture expectations were corrected to those exact codes, not broadened
to arbitrary failures. A missing module/file diagnostic is explicitly rejected
as a false positive. Request fixtures assert method, encoded path, base once,
body/header/auth and response/status/Location, deriving JSON shapes from OAS.
Injected error statuses for transport tests do not claim the real health endpoint
emits those statuses. Web ApiError adaptation remains C-04, HTTP truth remains C-03.

## Dependency security assessment — OPEN maintenance finding

Lock graph59 entries: MIT51, Python-2.0 one, ISC4, BSD-2-Clause one,
BSD-3-Clause one, Apache-2.0 one. All entries have resolved URLs/integrity;
no install scripts. engine-strict clean install succeeds on the pinned toolchain.
No incompatible/duplicate compiler peer is reported by npm ls.

New audit path: HeyAPI0.99.0 → json-schema-ref-parser1.4.4 → js-yaml4.2.0;
shared0.5.0 is also affected. Four high package findings represent three source
DoS advisories plus inherited package effects, not four distinct exploits:
[merge chains](https://github.com/advisories/GHSA-52cp-r559-cp3m),
[omap](https://github.com/advisories/GHSA-5p4m-2wfm-xmqj),
[empty merge sources](https://github.com/advisories/GHSA-2883-xcg3-v3hh).
Runtime audit0. Neither audit was a whole-web/mobile/repository security audit.
Existing consumer advisories remain open; Redocly bundled internals are not
individually enumerated by this npm audit.

Bounded mitigation: the supported generation wrapper gives HeyAPI the checked
Redocly-parsed object and rejects nonlocal/unresolved refs. It does not pass a
YAML string/path to the vulnerable parser. An isolated Node registerHooks test
replaces installed js-yaml imports with a load function that throws; a positive
control confirms the trap, then actual generation succeeds with zero load calls.
This does not patch/remove the vulnerable package, prove arbitrary direct CLI
use safe, or audit Redocly itself. Direct YAML HeyAPI CLI use is not the supported
workflow. No exploit payload or destructive stress test was executed.

Owner: project owner/platform dependency maintenance. Disposition OPEN with this
scope/mitigation, not silently ACCEPTED/DEFERRED. A patched transitive/generator
version change requires separate review/approval; audit-fix's suggested downgrade
is not applied. The approved task DoD requires disclosure/assessment, not zero
advisories; all mandatory C-02 compatibility and generation gates passed without
changing that criterion. No known affected YAML-loader call is used on the
supported, reviewed local generation path.

## Traceability, safety and C-03 prerequisites

TEST-001-CONTRACT-01: final C-01 PASS. TEST02: two-directory/source determinism and
drift rejection PASS. TEST03: package build/declarations/negative types PASS;
actual mobile consumer remains C-05. TEST04: generated transport portion PASS;
web ApiError adapter/browser portion remains C-04. SC-AC-01 validation/generation
and local determinism gates PASS; no full SC-AC-02–06 or full001 DONE is inferred.

Scope limited to package config/scripts/tests/generated source and relevant
READMEs/evidence/SDD status. Backend, web/mobile manifests/source/locks, workflow,
architecture rules, database/Flyway V1/V2/Docker/native configuration unchanged.
No DB/container/device action or new business behavior. dist/node_modules/temp
reports remain ignored; generator-owned TS is intended tracked source. Final
UTF-8/changed whitespace/new links/bounded secret and artifact checks recorded
with Git diff --check. Original requirements/AC text preserved.

**Next C-03 prerequisites:** keep C-01 source hash/C-02 deterministic gate passing;
use existing isolated test backend/harness and synthetic accounts/secrets/ports;
verify DB isolation before writes; capture all eleven real success/error responses
and validate operation/status/header/body with OAS/Ajv; reuse auth/owner/replay/
concurrency scenarios and distinguish real SQL evidence from transport mocks.
Stop on wire/source conflict instead of modifying business code or loosening the
contract. No developer DB writes. C-03 execution is not authorized/performed by
this C-01/C-02 task; follow its separate implementation instruction.

## Final file inventory

Combined working tree:43 files (7 tracked modifications,36 untracked files),
including all19 original C-01 files. C-02 adds24 files and evolves12 existing
C-01 documentation/package-metadata files; the contract/validators/tests/config
remain unchanged. Sixteen generated TS files are intentional source, not build
artifacts. Complete combined inventory:

```text
contracts/openapi/README.md
contracts/openapi/demo-001.yaml
docs/implementation/d1-07-c01-contract-evidence.md
docs/implementation/d1-07-c02-sdk-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
packages/api-client/.npmrc
packages/api-client/README.md
packages/api-client/openapi-ts.config.ts
packages/api-client/package-lock.json
packages/api-client/package.json
packages/api-client/redocly.yaml
packages/api-client/scripts/check-contract.mjs
packages/api-client/scripts/check-generated.mjs
packages/api-client/scripts/contract-lib.mjs
packages/api-client/scripts/generate.mjs
packages/api-client/scripts/generation-lib.mjs
packages/api-client/scripts/lint-contract.mjs
packages/api-client/src/generated/client.gen.ts
packages/api-client/src/generated/client/client.gen.ts
packages/api-client/src/generated/client/index.ts
packages/api-client/src/generated/client/types.gen.ts
packages/api-client/src/generated/client/utils.gen.ts
packages/api-client/src/generated/core/auth.gen.ts
packages/api-client/src/generated/core/bodySerializer.gen.ts
packages/api-client/src/generated/core/params.gen.ts
packages/api-client/src/generated/core/pathSerializer.gen.ts
packages/api-client/src/generated/core/queryKeySerializer.gen.ts
packages/api-client/src/generated/core/serverSentEvents.gen.ts
packages/api-client/src/generated/core/types.gen.ts
packages/api-client/src/generated/core/utils.gen.ts
packages/api-client/src/generated/index.ts
packages/api-client/src/generated/sdk.gen.ts
packages/api-client/src/generated/types.gen.ts
packages/api-client/src/index.ts
packages/api-client/tests/contract.test.mjs
packages/api-client/tests/sdk.test.mjs
packages/api-client/tsconfig.json
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
specs/001-platform-foundation/tasks.md
```
