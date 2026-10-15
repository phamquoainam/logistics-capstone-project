# D1-07 / C-01 — OpenAPI Contract Foundation evidence

**Later final acceptance checkpoint:** the user's C-01 Final Acceptance/C-02
task authorizes continuation when final review passes. C-01 review and41 tests
PASS; no source/schema/security mismatch found. C-02 subsequently implemented
and locally verified. [Current C-02 evidence](d1-07-c02-sdk-evidence.md) records
the19-file preservation manifest, contract hash, generated SDK/type compatibility
finding and new dev-graph audit4 high OPEN/runtime0. Earlier audit0/no-SDK/
pending-review statements below describe the original C-01 checkpoint only.
Actual HTTP conformance remains unexecuted C-03, not inferred from acceptance.

Execution date: 2026-10-15. Scope: C-01 only under accepted DEC-29;
[spec](../../specs/001-platform-foundation/slices/demo-contract-client/spec.md),
[design](../../specs/001-platform-foundation/slices/demo-contract-client/design.md),
[tasks](../../specs/001-platform-foundation/slices/demo-contract-client/tasks.md).
**C-01 local implementation/verification DONE; owner contract review pending
before C-02. D1-07/slice IN_PROGRESS/PARTIAL; C-02–06 NOT STARTED.
Feature001 and Day1 remain PARTIAL.**

## Git and source checkpoint

- Existing clean branch `feature/d1-07-demo-contract-client` reused without
  checkout/reset/rebase/stash. Initial working tree had no tracked/untracked work.
- HEAD/local develop/origin/develop and live remote develop all matched
  `20072b65cc183153bd26a614103ab9e8a442a713`.
- GitHub API for [PR #8](https://github.com/phamquoainam/logistics-capstone-project/pull/8)
  reported merged=true, merge SHA above, merged_at `2026-10-15T14:07:17Z`.
- `git branch --show-current`, `git status --short --untracked-files=all`,
  `git rev-parse HEAD`, `git rev-parse develop`, `git rev-parse origin/develop`,
  `git ls-remote origin refs/heads/develop` and PR API checks PASS.
- `git diff 3186f01 HEAD --name-only -- app infra scripts .github` empty:
  D1-06 publication/merge did not alter executable baseline. Source reviewed at
  current HEAD: all four controllers, records, security/token/login, errors,
  merchant lookup, order service/store/repository, Jackson config and V2 invariants.
- No Git publication/mutation in this task. Final HEAD stays the same.

## Eleven-operation inventory — PASS

Prefix `/api/demo/v1`; there are no declared query parameters.

| Method/path suffix | operationId | Access | Success schema/status |
|---|---|---|---|
| GET /health | getDemoHealth | public; explicitly invalid bearer still rejected | DemoHealth /200 |
| POST /auth/login | loginDemo | public login, existing failed-attempt limiter | LoginResult /200 |
| GET /auth/me | getDemoCurrentUser | authenticated current account | AccountView /200 |
| GET /merchant/me | getDemoCurrentMerchant | MERCHANT, trusted merchant lookup | MerchantView /200 |
| GET /shipments | listDemoShipments | MERCHANT, own rows only | ShipmentView[] /200 |
| POST /shipments | createDemoShipment | MERCHANT, own rows, Idempotency-Key | ShipmentView /201, Location |
| GET /shipments/{id} | getDemoShipment | MERCHANT, UUID, cross-owner404 | ShipmentDetail /200 |
| POST /shipments/{id}/cancel | cancelDemoShipment | MERCHANT, UUID, cross-owner404, Idempotency-Key | ShipmentView /200 |
| GET /admin/users | listDemoUsers | ADMIN | AccountView[] /200 |
| GET /admin/overview | getDemoOverview | ADMIN | Overview /200 |
| GET /admin/shipments | listDemoShipmentSummaries | ADMIN | ShipmentSummary[] /200 |

Controller ownership: Identity3, Merchant1, Orders6, Platform1. The source
inventory scans RestControllers recursively rather than assuming a fixed list.
Unsupported mapping syntax fails closed. Twelve Java record field sets map to
schemas; DemoHealth is the thirteenth schema and matches its source Map constants.
Request max lengths/patterns/decimal limits, required fields, output nullable
merchantId/reason, UUID/time formats and enum states are represented. All current
record fields are required in output, including nullable properties.

JWT bearer auth is stateless; current enabled account/role/merchant scope comes
from backend verification. RBAC is compared to first matching actual security
matcher per operation. No Identity internal/module boundary or caller-supplied
owner is introduced. Existing fixed900-second token lifetime, UTF-8 BCrypt
password limit and process-local login limiter are documented, not modified.

Create key scope merchant+create+key; cancel scope merchant+cancel:shipmentId+key.
Keys retain the Java full-match1..100 rule. Replay returns the current resource,
create201+Location/cancel200; different canonical input conflicts409. Existing
transaction/locking/event/receipt behavior remains server-owned. Flat application
errors preserve400/401/403/404/409/429/500 and source code/message examples.
Generic framework/proxy/unhandled failures have an explicitly unmodeled default
body, not an invented guarantee of flat JSON.

## Tooling and commands

Working directory `packages/api-client`, Windows PowerShell;
Node24.13.0/npm11.6.2. New package is private, no runtime dependencies.

| Actual command | Result | Evidence/limit |
|---|---|---|
| `npm.cmd install --no-audit --no-fund` | PASS | Initial install,7 packages; exact approved three devDependencies |
| `npm.cmd ci --no-audit --no-fund` | PASS | Clean lockfile install,7 packages, no version/engine overrides |
| `npm.cmd ls --all` | PASS | @redocly/cli2.64.0; ajv8.20.0; ajv-formats3.0.1; no duplicate Ajv |
| `.\node_modules\.bin\redocly.cmd --version` | PASS | 2.64.0 |
| `npm.cmd run contract:lint` | PASS | Redocly2.64.0, errors0/warnings0/ignored0 |
| `npm.cmd run contract:inventory` | PASS |11 exact source/OAS operations; DTO/auth/status/header/error checks |
| `npm.cmd test` | PASS |41 tests,41 pass,0 fail,0 skipped,0 cancelled/todo |
| `npm.cmd run verify:c01` | PASS | Same lint/inventory/test gates in order, after clean install |
| `npm.cmd audit --json` | PASS |0 known advisories in the new reported package graph |
| `npm.cmd audit --omit=dev --json` | PASS |0 advisories; no runtime package dependency |
| `git diff --check` and untracked text/links/protected-path review | PASS | Final bounded safety check; no source/config/migration/workflow change |

Locked development graph: Redocly2.64.0 (MIT), Ajv8.20.0 (MIT),
ajv-formats3.0.1 (MIT), fast-deep-equal3.1.3 (MIT), fast-uri3.1.8 (BSD-3-Clause),
json-schema-traverse1.0.0 (MIT), require-from-string2.0.2 (MIT). Lockfile records
registry URLs and SHA512 integrity. Redocly's published CLI is bundled; npm audit
does not separately enumerate embedded code. Zero advisories is bounded registry
evidence, not a guarantee of absence of vulnerabilities or a whole-repo audit.
Existing web/mobile advisories remain unchanged/open. No audit fix was run.

The approved generator0.99.0 and TypeScript6.0.3 are not installed for C-01.
Runtime/build compatibility of generator/SDK/consumers remains UNVERIFIED until
C-02 and subsequent tasks. Ajv/formats stay exclusively in the contract harness.

## TEST-001-CONTRACT-01 and rule effectiveness

41 tests comprise four positive tests and **37 negative fixtures**:

- 20 source/contract negatives: absent/invented operation; missing security;
  wrong RBAC; optional key; wrong creation status; missing404; lost owner scope;
  wrong success/request schema; wrong error code/status binding/shape/message;
  lost Location; invented DTO field; missing required field; lost nullability;
  changed size constraint; removed nonblank constraint.
- 3 Redocly negatives: unresolved ref, duplicated operationId, illegal parameter
  location. The actual same pinned CLI/config returns exit1 with the expected
  `no-unresolved-refs`, `operation-operationId-unique`, `struct` rule respectively.
- 14 Ajv negatives: missing required property; invalid UUID/time; injected request
  owner; string weight; excessive weight precision; negative weight; unknown
  shipment state; null nonnullable property; old envelope; blank login; Unicode
  blank cancellation reason; blank canonical address; unnormalized newline in
  canonical reference. Each asserts false plus the expected validator keyword.

Positive controls: source/invariant checker returns no problems; all13 component
examples validate; all18 application-error examples validate; nullable fields and
weights0.001/1.001/1000 validate. Total31 documented JSON examples. Ajv2020 is
strict with formats and no coercion/default insertion/property stripping. Tests
also assert examples are not mutated and local-ref siblings intersect with,
rather than replace, referenced constraints. Invalid fixtures are isolated in ignored
`.demo/d1-07-c01/`; no intentionally invalid production document is kept.

## Findings, limits and disposition

1. Initial lint FAIL: one unused response and missing license metadata. Removed
   the redundant response. Only `info-license` is disabled, with explicit config
   rationale: repository license has not been approved. No invented MIT license
   for the project. `recommended-strict` structural/security/reference/unused
   checks remain enabled; any reported warning/ignored problem also fails gate.
2. Public health invalid bearer401 differs from protected missing-credential401;
   merchant-unavailable403 differs from pure role denial. Separate components
   capture those distinctions. No endpoint/status/body behavior was changed.
3. No source/approved-scope conflict found. Canonical request constraints are not
   a complete model of raw Jackson/Java acceptance: DraftInput trims first and
   normalizes decimal; CancelInput validates raw reason then trims; Java UTF-16
   length and password UTF-8 bytes need actual wire fixtures in C-03. A padded
   raw reference can normalize to a valid value. Schema-only negatives do not
   claim the backend rejects that raw request. This documented boundary does
   not waive C-03 or permit adding a client validator that changes behavior.
4. Source-drift checks are bounded parsers/invariants, not runtime/serialization
   proof. HTTP wire responses, auth/replay/concurrency/database outcomes, SDK
   generation/determinism, TS consumer builds and browser regressions are
   **UNVERIFIED for this slice**, assigned C-02–06. They were not needed to
   mutate/execute the unchanged backend for this C-01 contract-only checkpoint.

## Trace, safety and next dependency

TEST-001-CONTRACT-01 supports SC-01 / REQ-001-04 contract subset / NFR-M02 and
the validation portion of SC-AC-01. Full SC-AC-01 also requires C-02 deterministic
generation and is not DONE. SC-02–05/other SC-AC and TEST02–10 remain unverified
for this slice; no original parent REQ/AC is replaced or checked complete.

New executable files are limited to contract test tooling in packages/api-client;
contract plus READMEs and SDD/evidence/checkpoint metadata are the other changes.
No backend/web/mobile source, manifests/locks, POM, workflow, architecture rules,
Docker/database/Flyway/native configuration, credentials or generated SDK changed.
Existing root ignores cover node_modules/scratch. No commit/push/PR performed.

Final inventory:19 files (7 tracked modifications,12 new), no staged files:

```text
contracts/openapi/README.md
contracts/openapi/demo-001.yaml
packages/api-client/.npmrc
packages/api-client/README.md
packages/api-client/package.json
packages/api-client/package-lock.json
packages/api-client/redocly.yaml
packages/api-client/scripts/check-contract.mjs
packages/api-client/scripts/contract-lib.mjs
packages/api-client/scripts/lint-contract.mjs
packages/api-client/tests/contract.test.mjs
docs/implementation/d1-07-c01-contract-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/tasks.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
```

Final safety review PASS: UTF-8, changed/new whitespace, new relative file links,
bounded private-key/GitHub-token/AWS-key pattern scan; no stage/publication.
Original parent001 spec/design and full-task tail unchanged; slice requirement/
SC-AC text unchanged. Four JavaScript files pass `node --check`. These bounded
checks do not claim a comprehensive repository/history secret audit.

**Next: owner review of this C-01 document/schema/findings, then C-02 only on
explicit implementation instruction.** C-02 must clean-install approved
generator0.99.0/TS6.0.3, review graph/licenses/integrity, generate actual operation
SDK/types/bundled Fetch, verify two-directory byte determinism and drift negatives,
type/transport fixtures and private package build. Do not use deprecated standalone
client-fetch, consumer TS upgrade, manual generic HTTP substitute or peer override.
