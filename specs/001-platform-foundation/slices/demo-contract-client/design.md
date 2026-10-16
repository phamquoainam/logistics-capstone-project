# Design 001-CONTRACT — preserve the approved Demo-001 wire behavior

**Current C-06 verification - 2026-10-17 /1f26854:** C-01-06 local deliverables
VERIFIED; C-06 LOCAL_DONE. D1-07 PARTIAL / READY_FOR_PR; remote CI/integration
UNVERIFIED, full001/Day1 PARTIAL. No C-06 commit/push/PR. Fresh package85,
web24/browser7 twice, mobile Jest1/compiler4/Hermes, architecture23,
H2 40+8 named skips, actual PG48/no skips and privacy guard14 PASS.
Mobile audit58 full/35 omit-dev and SDK dev4 high remain FAIL/OPEN; web0/0.
[C-06 final evidence](../../../../docs/implementation/d1-07-c06-final-evidence.md).
Earlier execution/checkpoint statements below are historical.

**Historical C-05 checkpoint — 2026-10-17 /6656753:** compile-only mobile consumer
DONE locally. Public SDK/Fetch factory types resolve in unchanged Expo Bundler/
react-native compiler config; real compiler controls and actual SDK-entry Hermes
bundle PASS. Starter UI/entry/config/native unchanged, separate export PASS.
[C-05 evidence/graph/security](../../../../docs/implementation/d1-07-c05-mobile-evidence.md).
TEST03/09 mobile portions PASS; SDK85 regression PASS. No device/network claim.
Mobile audits59 full/35 omit-dev and SDK dev4 high remain FAIL/OPEN; C-06 combined
gates NOT STARTED. Full001/Day1 PARTIAL. Prior execution notes are historical.

**Historical C-04 execution — 2026-10-16 /a2609f9:** local web adoption DONE;
generated operation callbacks/types/public package exports, minimal ApiError
adapter and per-request JWT. Existing UI/session/form/key behavior preserved.
Web quality gates/24 adapter +7 real-browser tests, SDK85/wire27 and Maven40
with8 expected PG-only skips PASS. No OAS/generated/backend change.
[C-04 evidence/mapping](../../../../docs/implementation/d1-07-c04-web-evidence.md).
TEST04/08 web portions PASS; TEST03/09 web subset PASS. C-05/06 not started,
mobile/final TEST09/10 portions UNVERIFIED; full001/Day1 PARTIAL.
Prior checkpoint notes retain their earlier execution scope.

**Status:** ACCEPTED_FOR_DEC_29_SCOPE, 2026-10-15; slice IN_PROGRESS / PARTIAL.
Approval source: the user's explicit “DEC-29 — APPROVED | D1-06 Definition of
Ready Closure” message. D1-06 documentation/DoR DONE.
**D1-07 C-01 final technical acceptance / C-02 local DONE:** TEST01 PASS41,
SDK tests PASS44/no skips; deterministic16-file source matches two scratch runs.
Dev audit4 high findings OPEN, runtime0, bounded YAML-loader avoidance verified.
C-03 actual HTTP/PG local DONE: two27/27-test runs,108 responses each, all11
operations and cleanup PASS/no mismatches. C-04–06 NOT STARTED;
actual consumer/browser/final combined gates UNVERIFIED.
[C-03 harness/HTTP/SQL evidence](../../../../docs/implementation/d1-07-c03-wire-evidence.md).
[C-02 configuration, findings and evidence](../../../../docs/implementation/d1-07-c02-sdk-evidence.md).
[C-01 evidence](../../../../docs/implementation/d1-07-c01-contract-evidence.md).
Execution baseline20072b6; original design baseline3186f01;
[spec](spec.md), [decision/gaps](../../../../docs/implementation/d1-06-slice-readiness.md).

## Ownership and compatibility

Identity owns auth/users; Merchant owns merchant profile query; Orders owns draft
and admin shipment queries; Platform owns existing local-demo health and contract
tooling. HTTP schema is an external wire contract, not a new Java module API.
Keep a single backend, existing api/internal boundaries and minimal trusted actor.
Generated TS does not import Java internals or authorize calls client-side.

No SQL/schema/migration/transaction changes. Existing owner lookup, scoped unique
constraints, row locking and receipt/event writes remain in ShipmentService and
its module-local persistence port/adapter. No new coordinator/event or outbox.
No changes to V1/V2, token policies, seed/account/role policies, CORS or server bind.
If observed wire behavior conflicts with this inventory, stop and report drift;
do not change business code or expected statuses to fit a preferred generator.

## Contract authority and drift policy

At D1-06, no OpenAPI document or wire fixture has been generated/executed. The
approved Demo-001 behavior and baseline3186f01 controller/record/security/service
source are the evidence for this approved inventory/design. Existing test source
supports specified scenarios; it is not proof a future schema matches responses.
After reviewing the refinements, the user explicitly approved DEC-29. This
accepts the design/inventory and tooling scope, not a claim that a future
OpenAPI document or generated SDK has passed wire/runtime tests.

After approved D1-07 C-01 review, `contracts/openapi/demo-001.yaml` becomes the
single executable HTTP schema source for this bounded demo, and its synthetic
examples are validated fixtures. SDK output is derived, never an independent
contract. Preserve the parent001 REQ/AC and keep broad ADR-0003 PROPOSED. Do not
introduce a second OAS file for the same demo or annotate code to auto-redefine
the agreed wire API. Future wire changes require spec/contract review first.

Three separate drift controls are mandatory:

1. Source inventory versus OAS: compare all11 explicit method/path pairs,
   operation request DTOs/constraints, security, supported response status/body/
   headers against reviewed source references. Implicit framework HEAD/OPTIONS
   are not new business operations. New/missing/renamed operations fail inventory.
2. OAS versus wire: capture actual isolated-backend responses, pick schema by
   operation/status/content-type, validate body and selected headers; also send
   negative/security/replay fixtures. Mocks and static field-name comparisons
   alone cannot prove wire conformance. Unmodeled responses are findings, not
   permission to alter backend or silently loosen schemas.
3. OAS versus generated package: regenerate twice with exact tool/config/lock
   into different ignored directories and compare relative-file inventories and
   content to each other and tracked generated source. Sort comparison paths,
   but do not strip meaningful content/timestamps to manufacture determinism.
   Configure deterministic tool output; platform line-ending policy must be
   explicit. Missing/stale output and unsupported refs fail nonzero.

Every difference gets a source/fixture explanation and review. Never regenerate
and auto-bless a changed contract merely because backend/UI tests still pass.

## Complete operation inventory

Contract file proposal: `contracts/openapi/demo-001.yaml`, OpenAPI3.1.0, local
server base `/api/demo/v1`; paths below are relative to that base. Full assembled
URLs must have the prefix exactly once. OperationIds below are client identifiers,
not new HTTP paths. Include shared400 where request decoding/validation applies,
401/403 for protected calls, and safe500/default handling; do not fabricate503
or a new error envelope. Public requests may still receive401 for an explicitly
supplied invalid bearer token through the existing security filter.

| Method/path | operationId | Owner / permission | Request | Successful response / additional observed errors |
|---|---|---|---|---|
| GET /health | getDemoHealth | platform / public | None | 200 DemoHealth; describes constant status, not dependency readiness |
| POST /auth/login | loginDemo | identity / public | LoginInput | 200 LoginResult; 400 invalid,401 INVALID_CREDENTIALS,429 LOGIN_RATE_LIMIT |
| GET /auth/me | getDemoCurrentUser | identity / any enabled authenticated account | Bearer | 200 AccountView; missing/invalid/expired/disabled principal401 |
| GET /merchant/me | getDemoCurrentMerchant | merchant / MERCHANT | Bearer | 200 MerchantView;403 MERCHANT_UNAVAILABLE if merchant lookup fails |
| GET /shipments | listDemoShipments | orders / MERCHANT | Bearer | 200 ShipmentView[], most recent200, no pagination object |
| POST /shipments | createDemoShipment | orders / MERCHANT | Bearer, required Idempotency-Key, DraftInput | 201 ShipmentView with Location, including replay;409 REFERENCE_EXISTS/IDEMPOTENCY_CONFLICT |
| GET /shipments/{id} | getDemoShipment | orders / MERCHANT + owner | UUID id, Bearer | 200 ShipmentDetail;404 SHIPMENT_NOT_FOUND also for cross-owner |
| POST /shipments/{id}/cancel | cancelDemoShipment | orders / MERCHANT + owner | UUID id, Bearer, required Idempotency-Key, CancelInput | 200 ShipmentView;404 same hidden ownership;409 INVALID_TRANSITION/IDEMPOTENCY_CONFLICT |
| GET /admin/users | listDemoUsers | identity / ADMIN | Bearer | 200 AccountView[]; sanitized, no password/hash |
| GET /admin/overview | getDemoOverview | orders / ADMIN | Bearer | 200 Overview counts |
| GET /admin/shipments | listDemoShipmentSummaries | orders / ADMIN | Bearer | 200 ShipmentSummary[], most recent200; no full recipient/address fields |

Source anchors: identity.internal.web.AuthController (login/me/admin users),
merchant.internal.web.MerchantController, orders.internal.web.ShipmentController
(six operations), platform.internal.web.DemoHealth, identity.internal.infrastructure.
DemoSecurity, platform.internal.web.DemoErrors, LoginService, ShipmentService,
MerchantRepository, and application-demo.properties. The read-only enumerator
again found exactly11 unique method/path pairs; checkpoint/hash evidence is in
ignored `.demo/d1-06/source-operation-inventory.json`.

| Error/security case | Existing source outcome to preserve | Planned fixture / source evidence |
|---|---|---|
| Anonymous protected operation | 401 UNAUTHENTICATED from configured security entrypoint | Existing anonymousUnauthorizedAndWrongRoleForbidden plus actual wire check |
| Malformed/tampered/expired token or unavailable principal | 401 INVALID_TOKEN via resource-server entrypoint; issuer elogistics-demo, HS256, current account enabled/role/scope reloaded | tamperedTokenAndClientRoleInjectionRejected, signedExpiredTokenRejected, accountStatusAndRoleRecheckedAfterIssue |
| Valid principal with wrong role | 403 FORBIDDEN; ADMIN cannot call merchant/finance operations by inheritance | anonymousUnauthorizedAndWrongRoleForbidden |
| Login bad password/disabled user | 401 INVALID_CREDENTIALS, never password echo | wrongPasswordAndDisabledLogin |
| Login throttling | 429 LOGIN_RATE_LIMIT after five failed attempts for remoteAddress:username within five-minute window; existing2000-entry guard | rateLimitFailedLogins; no new Retry-After header promised |
| JSON/Bean Validation/missing header/bad path UUID | 400 INVALID_REQUEST from explicit exception handlers; no submitted password or per-field error array | validationOwnerAndStatusInjectionRejected plus missing/blank/key/body/path wire fixtures |
| Present but invalid key | 400 INVALID_OPERATION_KEY, existing regex1..100 | New invalid key fixture; backend service source determines outcome |
| Merchant missing/unavailable | 403 MERCHANT_UNAVAILABLE; Orders also checks enabled/MERCHANT/non-null scope before merchant lookup | MerchantRepository/ShipmentService source; isolated membership-change fixture |
| Absent/other merchant shipment | 404 SHIPMENT_NOT_FOUND on read and cancel, no object distinction | ownDraftAndCrossMerchantIsolation |
| Same key/different normalized payload | 409 IDEMPOTENCY_CONFLICT | createReplayConflictAndReferenceUniqueness / cancelGuardReplayAndImmutableEvents |
| New key/same merchant reference | 409 REFERENCE_EXISTS | createReplayConflictAndReferenceUniqueness |
| Cancel non-DRAFT with a new key | 409 INVALID_TRANSITION; replay old matching receipt is still200 | cancelGuardReplayAndImmutableEvents |
| Database access exception | 500 DATABASE_ERROR with existing safe flat message | DemoErrors source; safe failure fixture without changing developer database |

These are source-defined application errors, not a claim every framework/proxy
failure is that flat shape. Keep existing nonJSON/generic HTTP fallback for
unmodeled gateway/framework errors; do not invent503 dependency semantics or
require backend changes to return a newly preferred shape. JWT validation skew
remains the framework default; do not invent a zero-skew policy. No refresh token,
server logout or new correlation header is added. Exact application messages
are preserved, not replaced by a new error taxonomy.

`AccountView` source record includes enabled, omitted from the older short demo
design table but present in backend and current web type. Capture all source
fields, not the abbreviated table. `MerchantRepository.require` throws403
MERCHANT_UNAVAILABLE when no organization is found; ShipmentService owner checks
call the same public capability. All new contract content must preserve these
branches; do not constrain error.code globally to a narrower list than the source.

## Schema inventory and wire constraints

All names/types below come from existing Java records, controller methods and
web types. Every output record field is required as a JSON property; nullable
values are explicit. Verify this with actual serialized responses in D1-07,
not merely reflection or TypeScript assignability.

| Schema | Source-exact properties / constraints |
|---|---|
| LoginInput | username/password strings, nonblank, max80/max72; reject unknown fields; password writeOnly in schema, never an actual secret in examples. LoginService also enforces BCrypt UTF-8 byte limit72 |
| AccountView | id UUID; username/displayName/role strings; enabled boolean; merchantId UUID or null. No passwordHash/credential/token in this record |
| LoginResult | accessToken string; expiresIn integer (existing value900 seconds); user AccountView. Tokens are runtime values only, not schema examples/log data |
| MerchantView | id UUID, displayName/reference strings |
| DraftInput | reference/pickupAddress/recipientName/recipientPhone/deliveryStreet/deliveryWard/deliveryProvince/parcelDescription strings; weightKg number. All required, unknown fields rejected; no merchantId/owner/status/role input |
| DraftInput constraints | reference max80 and [A-Za-z0-9][A-Za-z0-9._/-]{0,79}; pickupAddress max500; recipientName max160; recipientPhone [+0-9 ()-]{7,30}; deliveryStreet max300; deliveryWard/province max160; parcelDescription max300. Strings trimmed before @NotBlank/size/pattern; validate whitespace fixtures server-side. Weight0.001..1000, at most3 fractional digits, @Digits integer4; JSON number, no string conversion |
| CancelInput | reason required nonblank string max300, no extra properties; existing service trims reason before hash/event |
| ShipmentView | id/merchantId/createdBy UUID; reference/pickupAddress/recipientName/recipientPhone/deliveryStreet/deliveryWard/deliveryProvince/parcelDescription strings; weightKg number; status DRAFT or CANCELLED; createdAt/updatedAt ISO UTC date-time strings |
| ShipmentEvent | id/actorId UUID; eventType DRAFT_CREATED or DRAFT_CANCELLED; reason string or null; occurredAt ISO UTC date-time |
| ShipmentDetail | shipment ShipmentView, events ShipmentEvent[], addressVerification existing literal UNVERIFIED_DRAFT_INPUT |
| ShipmentSummary | id/merchantId UUID, reference string, status DRAFT or CANCELLED, createdAt date-time; no recipient/address data |
| Overview | total/draft/cancelled integer counts (Java long); do not coerce to strings or add financial fields |
| DemoHealth | status existing literal UP, scope existing literal local-demo; no DB/cache/broker state or credentials |
| ErrorBody | code/message strings; exact current flat shape; INVALID_REQUEST, INVALID_OPERATION_KEY, UNAUTHENTICATED, INVALID_TOKEN, FORBIDDEN and domain codes from current implementation. Database adapter errors map to500 DATABASE_ERROR with safe message |

Idempotency-Key: required only on create/cancel; regex `[A-Za-z0-9._:-]{1,100}`.
Create scope merchant+create+key; cancel merchant+cancel:shipmentId+key. Canonical
hash uses trimmed strings/normalized decimal and trimmed cancel reason. Same
key+payload replays current shipment representation, not a cached original body;
different payload409. No client retry policy, key regeneration or new semantics.

Use bearerAuth with operation-local public security=[] for health/login. OpenAPI
role/ownership descriptions supplement but cannot replace server checks. Preserve
fresh account lookup, enabled/role/merchant scope recheck, token memory-only on
web, no client-supplied actor and no role-based token trust. Existing generic
403/admin rules and cross-merchant404 remain; don't introduce new permission.

## Accepted client and tooling design

DEC-29 approval is recorded; C-01 installed only the validator dependencies.
Generator/TS installation and strict package build are now verified in C-02. Approved pins
must be exact in the new private package/lock, with Node24.13.0/npm11.6.2 engines
and strict engines:
@hey-api/openapi-ts0.99.0, @redocly/cli2.64.0, TypeScript6.0.3; Ajv8.20.0 and
ajv-formats3.0.1 for test validation. Generate types, operation SDK functions and
bundled Fetch client from local schema using built-in plugin names; no separate
client-fetch runtime package, cloud schema registry, new workspace manager or
Vite codegen plugin. Metadata ranges/official sources are in the assessment.

All proposed versions were rechecked by exact-version registry endpoints after
owner feedback; Redocly2.64.0 exists (released2026-10-15T07:20:07.640Z), so no
fallback to2.63.0 is needed. HeyAPI0.99.0 tarball integrity and bundled Fetch
sources were checked read-only. Do not install deprecated standalone
@hey-api/client-fetch0.13.1. The new package's TypeScript6.0.3 is dev tooling;
web6.0.3 and mobile~6.0.3/locked6.0.3 declarations are unchanged. This verifies
existence/declared constraints, not complete transitive/build compatibility.

Planned private `@elogistics/api-client` layout under accepted DEC-29: src/generated (generator-owned),
src/index.ts (exports), package scripts/config/tests, dist (ignored build output),
package-lock.json. Generated TypeScript SOURCE is an intentional reviewed artifact
that can be tracked; binary/dist/log/APK/credentials cannot. Never hand-edit codegen.
Generate twice into two distinct ignored temp directories; compare normalized
paths/bytes and compare with tracked output. Fail on stale/missing files, broken
refs, duplicate operationId, wrong security/body/header declaration; no skips.

Web adds `file:../../../packages/api-client` dependency and imports generated SDK
operations/types through a small adapter that preserves ApiError/network/nonJSON
fallbacks. Convert dynamic generic useData/request callers to concrete generated
operation callbacks and inferred result types; keep rendered UI/session/form/key
behavior intact. Schema-derived aliases are acceptable; manual copies, generic
`as T` response success casts, any casts to hide incompatibilities are not.
Configure browser same-origin base + /api/demo/v1, retain local Vite proxy. Avoid
duplicated prefixes; never hardcode developer host or token in generated source.

Mobile local file dependency and type-tests/api-client.ts prove shared package
import/signatures with the existing RN compiler. No App.tsx/index.ts/native config
or Metro changes, network call, token persistence or UI added. This is portability
at type level only; bundling/device networking for a future shipper consumer is
still UNVERIFIED. Do not claim mobile API integration based on typecheck alone.

## Fixtures and verification matrix (01/02/05–07 PASS; 03/04 package portions PASS)

C-02 execution:85 total tests/no skips across C-01 and SDK; TEST03 consumer and
TEST04 web-adapter portions, and TEST08–10 remain unexecuted. C-03 now executes
TEST05/06/07 against real HTTP/PostgreSQL,27/27 twice; its scope/evidence above
supersedes earlier NOT RUN wire fixture notes in the design history. The source schema
is unchanged; its pattern-only string allOf branches gain only redundant string
types in generator parser configuration to prevent unknown request fields. Ajv
equivalence/compiler fixtures verify the adaptation, no generated edits/casts.
The supported generator receives the checked Redocly object with local refs to
avoid the advised js-yaml path; advisories remain OPEN. Exact gate evidence is
in the C-02 record above. These are execution notes, not a scope/pin/AC change.

| Test ID | Fixture / assertion | Coverage |
|---|---|---|
| TEST-001-CONTRACT-01 | Validate OAS structure, local refs/unique operationIds/all11 method/path pairs and operation-level auth; synthetic invalid ref/security/header fixture must fail same checker. Executed C-01:41 tests PASS/no skips,37 negative fixtures; details in C-01 evidence | SC-01; REQ-001-04 subset; PASS local |
| TEST-001-CONTRACT-02 | Two-temp-dir generation + checked-in drift check; temporary stale/missing output detected without touching production | SC-02 reproducibility |
| TEST-001-CONTRACT-03 | Generated SDK typecheck; compile-negative method/path/body/header cases isolated with explicit expected diagnostic assertions, not production suppressions | SC-02/05 |
| TEST-001-CONTRACT-04 | Fetch spy: path/base prefix, method/body serialization, per-request Bearer, required idempotency headers, no token on anonymous login/health;401/409/network/nonJSON errors retain ApiError behavior | SC-03/04/05 |
| TEST-001-CONTRACT-05 | Isolated real backend: all11 actual success status/body schemas, login/me nullable fields, create Location/replay201, draft/cancel/detail/events/admin/health; validate with Ajv using OAS3.1 components, not handcrafted expected DTO copies | SC-01/03 |
| TEST-001-CONTRACT-06 | Synthetic A/B/admin/operational/disabled accounts; absent/expired/tampered token, wrong role, hidden cross-owner404, unknown input fields/invalid weight/blank strings/key, duplicate ref, conflicting replay/non-DRAFT cancellation and throttling | SC-04 security/negative |
| TEST-001-CONTRACT-07 | Same payload/key replay, changed payload conflict and concurrent create through real backend; SQL/event/receipt assertions reused from backend tests, not mocked DB correctness | SC-04 BR-01/02 subset |
| TEST-001-CONTRACT-08 | Existing three browser regressions + generated web client login/create/read/cancel/admin; preserve input/key on failed request/retry | SC-03/04 NFR-U02 |
| TEST-001-CONTRACT-09 | Package clean install/build/test, web lint/typecheck/test/build, mobile lint/typecheck/test and compile-consumer; no need rebuild unchanged native app | SC-02/05 |
| TEST-001-CONTRACT-10 | Existing Maven architecture/H240+8 expected PG skips/real PG48+0 when available; report privacy guard and bounded changed-file/secret audit | SC-04/05, SC-AC-06 regression; counts are baseline, not a new execution claim |

Test-side response validation is distinct from generating TS types (no runtime
validation claim from a compiler). Ajv2020 mode/format plugin must handle the
local OAS3.1 component dialect; unsupported keywords/ref resolution are failures,
not silently disabled validation. Nullability and additionalProperties rules must
match actual HTTP; Java Bean Validation still handles trim/byte precision rules.

Ajv has one bounded purpose: validate actual JSON bodies against the operation's
request/response component schemas in contract tests; it does not validate the
whole OpenAPI document, authorize a user or run in the shipped app. Use Ajv2020
for draft2020-12; register the local component graph with stable IDs and resolve
local refs without dropping constraints. OpenAPI annotation handling is explicit;
unknown assertion keywords/unresolved refs fail instead of globally disabling
strict validation. Set no coercion, default injection or additional-property
removal: tests must observe original payloads. `ajv-formats` adds UUID/date-time
checks. Tests include invalid UUID/time, missing fields, nullability, extra input
fields and broken refs; fixtures must prove the same checker rejects them.

Redocly validates the OpenAPI structure/refs and configured lint rules; HeyAPI
generates client code; Ajv/formats independently checks wire data. Each new tool
is a private-package devDependency, no validator dependency is added to either
frontend runtime and no extra swagger-parser/ajv-cli package is justified.

Existing web transport tests use `{healthy:true}` for health and synthetic-id
strings. These intentionally test generic transport behavior; they are **not**
positive DemoHealth/UUID schema fixtures and must not be copied as contract
truth. D1-07 uses separate schema-valid synthetic fixtures (health exactly
UP/local-demo, valid UUID IDs, nullable merchantId/reason, real login return keys)
and separately labeled invalid fixtures. Actual success/error recordings remain
in memory/private diagnostics, with generated tokens/passwords never committed.

Source comparison/test-source review are complete. C-03 now verifies the approved
wire fixtures on isolated PostgreSQL/backend, including trim/null/decoding cases;
this supersedes D1-06's planned NOT RUN state. It is not exhaustive framework
coercion coverage. Canonical SDK requests use current web numeric
weight values. Do not assert the backend rejects every other JSON representation
unless a fixture proves it, and do not add client-side validation that rejects
previously accepted input or changes the server's error behavior.

### Concrete fixture definitions for D1-07

Canonical synthetic draft body (use an isolated DB; generate a unique valid
reference for independent cases, retain it for replay/conflict cases):

```json
{"reference":"CONTRACT-001","pickupAddress":"Synthetic pickup","recipientName":"Synthetic recipient","recipientPhone":"0000000000","deliveryStreet":"Synthetic street","deliveryWard":"Synthetic ward","deliveryProvince":"Synthetic province","parcelDescription":"Synthetic parcel","weightKg":1.25}
```

- F-01 health: unauthenticated GET, expected200 body exactly
  `{"status":"UP","scope":"local-demo"}` ignoring object property order.
- F-02 session: synthetic seeded merchant/admin/operator/disabled accounts;
  passwords generated privately at test startup. Login result requires
  accessToken/expiresIn/user; check900 seconds and user.enabled/nullable merchantId,
  then call auth/me with the actual in-memory token. No fake token positive case.
- F-03 owner A: create the canonical draft with key `contract.create.001`, expect
  201 and matching Location/resource UUID; list/detail includes it and exactly
  one creation event with null reason. Owner B gets404 on read/cancel and cannot
  list A's ID. Admin summaries omit recipientPhone/address properties.
- F-04 replay/state: same body/key returns the same ID with201/Location; change
  weight to2.5 with that key ->409 IDEMPOTENCY_CONFLICT. New key/same reference
  ->409 REFERENCE_EXISTS. Cancel reason `Synthetic cancel` with a valid new key
  ->200 CANCELLED; same cancel key/body replays200; changed reason conflicts;
  a fresh key on CANCELLED ->409 INVALID_TRANSITION. Concurrent matching creation
  retains one event/receipt; verify existing DB assertions, not response ID alone.
- F-05 malformed input: weight -1/1.2345, blank deliveryWard, ownerId/status/role
  injection, absent key, key with spaces, invalid UUID; use distinct new reference
  where relevant. Expect the source-specific400 code; ensure no rows/events added.
  Include padded-valid strings and whitespace-only strings to preserve trimming.
- F-06 auth/failure: no token/expired/tampered token, disabled or changed live
  account scope, wrong role, sixth failed login attempt; assert source-defined
  401/403/429 and safe error bodies. Use only an isolated account/DB, never modify
  developer memberships to make a fixture. Test missing/nonJSON server replies
  in the adapter separately from claimed backend application errors.
- F-07 checker negatives: clone valid examples into ignored temporary data;
  remove a required output field, replace a UUID/time, remove required header/
  security declaration, corrupt a local ref, or stale a generated file. Assert
  rejection by the same relevant checker; production/source files stay valid.

These definitions are frozen test intent for owner review, not executed fixtures.
New actual captures must reconcile with this source-backed proposal before the
contract is accepted as conformant; preserve secrets by validating in memory and
publishing only allowlisted operation/status/assertion outcomes.

Use synthetic seed fixtures only. H2 in-memory is allowed for existing HTTP
contract/UI smoke, never as proof of PostgreSQL constraints. For PG regression,
use a newly owned isolated test DB/container using the existing approved harness;
do not mutate developer DB/volume or replace Testcontainers requirements.
Any test provisioning/writes happen only in D1-07 with isolation verified.

Keep current backend test suite inventory/architecture/CI workflow unchanged.
Add contract checks in the private client package and existing Node/Playwright
HTTP harness against a safe test backend. If implementation requires adding a
Surefire test, the fail-closed report allowlist must be reviewed explicitly:
never bypass it or publish raw logs. New CI/frontend workflow is outside this
slice unless separately approved. Local gate scripts must fail nonzero on a
test failure; existing three backend checks remain required after publish.

## Rollback strategy and remaining execution assumptions

Rollback is application/client-only because this slice has no schema/data/event
change. Before adoption, retain the baseline3186f01 api.ts/App.tsx/manifests/locks
in Git as the comparison point; preserve concurrent work. If SDK behavior differs,
stop adoption and report the failing wire/browser fixture. Correct it within the
approved adapter/contract scope or prepare a reviewable patch restoring only
the slice's consumer changes and matching manifests/locks to baseline behavior.
Do not reset a branch, discard unrelated changes or hand-edit generated output.

After a separately authorized publication, rollback is a separately reviewed
revert/change PR with normal history; no force push/reset. Restore consumer calls
and dependency references together, then run the original web/mobile/backend
gates. Never roll back V1/V2, touch database/volumes, remove user data, wipe AVDs
or delete caches. The generated package may remain unused until the corrected
adoption is approved; no destructive cleanup is part of rollback.

Open execution assumptions for D1-07: approved-tools registry access, a complete
compatible locked dependency graph, actual generated output compiling with TS6,
local package built before consumer install/build, available isolated test backend
and safe credentials/ports, and successful real wire/browser/regression tests.
If any fails, report FAIL/BLOCKED without a version fallback, peer suppression,
test weakening or widening the file scope. These are future verification gates,
not failures claimed now and not reasons to mark untested runtime compatibility
PASS. Formal DEC-29 approval is now resolved; no in-scope design/approval blocker
remains. Future execution failures must still be reported rather than waived.
