# Slice 001-CONTRACT — Demo-001 executable contract and generated client

**Status:** IN_PROGRESS / PARTIAL; C-01 final technical acceptance DONE,
C-02 local deterministic SDK DONE under the user's conditional authorization;
C-03 actual HTTP/PostgreSQL local DONE, two27-test runs/no skips,108 responses
each,11 operations/no contract mismatches. C-04–06 NOT STARTED.
[C-03 evidence](../../../../docs/implementation/d1-07-c03-wire-evidence.md).
DEC-29 ACCEPTED on2026-10-15 by the user's
explicit “DEC-29 — APPROVED | D1-06 Definition of Ready Closure” message.
D1-06 documentation/DoR DONE. TEST-001-CONTRACT-01 PASS:41 tests/no skips;
C-02 adds44 passing SDK tests/no skips; real wire gates PASS at C-03,
consumer/browser/final combined gates UNVERIFIED. Dev graph audit4 high findings OPEN,
runtime0; supported generation avoids the affected YAML loader without a pin change.
[C-02 evidence](../../../../docs/implementation/d1-07-c02-sdk-evidence.md).
[C-01 evidence](../../../../docs/implementation/d1-07-c01-contract-evidence.md).
Parent Feature001 remains DRAFT_SPEC/PARTIAL. This subordinate slice does not
replace or edit parent REQ/AC. [Assessment/decision](../../../../docs/implementation/d1-06-slice-readiness.md).
Owners: platform contract tooling; identity/merchant/orders HTTP contract owners;
web integration. Reviewer/decision owner: project owner. Actors: developer/CI,
existing enabled demo merchant/admin; no new business role.

## Outcome and scope

Publish one source-exact OpenAPI3.1 contract for the eleven existing Demo-001
operations; generate a real typed Fetch SDK and use it for the existing web
login→merchant→create/list/detail/cancel DRAFT and admin-read journeys. Validate
both contract structure and actual backend responses. Keep HTTP JSON/statuses,
authentication, ownership, retry/idempotency, UI behavior and migrations intact.
Mobile compiles a shared-client consumer only; no mobile UI/network journey.

Out of scope: full production API/envelope migration, refresh/logout/revocation,
new merchant/address/pricing/shipment behaviors, money/stock/POD/GPS/offline,
providers/events/Outbox/Inbox, database DDL, new readiness semantics, Testcontainers,
full-stack Compose/Redis/RabbitMQ, dependency remediation, CI/workflow expansion.
These full001/future obligations remain open, not DEFERRED or silently waived.

## Trace and slice requirements

| Slice ID | Requirement | Parent/source subset |
|---|---|---|
| SC-01 | One OpenAPI source with all eleven existing operations, request/response/status/header/security constraints and synthetic examples | REQ-001-04 contract subset; T-001-02 subset; NFR-M02 |
| SC-02 | Exact pinned generation yields operation functions, types and Fetch transport; repeated generation/drift check is deterministic | REQ-001-04; NFR-O03/M02; not types-only/manual HTTP with a generic cast |
| SC-03 | Existing web journeys consume generated operations without changing business/error/input-retention behavior | DEMO-01..04; FR-A01/A02/M01/M04/M05/AD01 subsets; NFR-U02 |
| SC-04 | Existing auth/RBAC/cross-merchant404/idempotency/concurrency/transaction outcomes remain intact and contract-tested | BR-01/02 demo subset; NFR-S01/S02/C02; existing002/003/005 demo scope only |
| SC-05 | Client is private/local, no secret/server credential/client role authority; mobile compiler accepts a consumer with no runtime UI changes | REQ-001-05; NFR-S02/M01; no claim of009/010/015 behavior |

Parent AC-001-01/04/05 are supported only in contract/build/config subsets.
SC completion cannot close dependency readiness/full onboarding or the full
Feature001 Definition of Done. No FR or business policy is invented here.

UC trace is limited to the approved demo: A01 login, AD02 role/object enforcement,
M01 profile read, M04 draft creation, M06/M07 cancel/read and AD01 sanitized admin
read subsets. These mappings follow the [source requirements catalog](../../../../docs/domain/requirements-catalog.md)
and do not complete full session administration, merchant/address or shipment
features. No new reporting requirement is introduced by contract codification.

## Journey and acceptance criteria (new slice criteria, not parent replacements)

- **SC-AC-01:** Given the approved existing wire inventory and a clean lockfile
  installation, when validation and generation run, then all11 operations have
  unique stable operationIds, request/error/success schemas resolve, and two
  generation runs produce identical SDK/types/transport bytes. An invalid ref
  or omitted required contract element makes the check fail.
- **SC-AC-02:** Given the existing isolated demo backend and a synthetic merchant,
  when the generated-client web journey logs in, reads current merchant, creates,
  lists, reads and cancels its draft, then actual bodies/statuses/Location match
  schemas and existing browser assertions pass. Admin summary/users work through
  generated operations; no manual response DTO copy remains in web api.ts.
- **SC-AC-03:** Given expired/invalid/disabled credentials, a wrong role, a second
  merchant, invalid/missing key, invalid body, duplicate reference or invalid
  transition, when calling through the SDK, then existing401/403/404/400/409/429
  semantics and safe {code,message} errors are preserved as applicable, with no
  unauthorized write or information disclosure.
- **SC-AC-04:** Given the same create/cancel key, when replaying same input or
  racing identical creation, then one effect remains; different input conflicts.
  Create replay retains current HTTP201/Location and current resource semantics.
  Network/nonJSON failure retains input/key and existing ApiError fallback; no
  implicit retry/new key/success state is added by the generated transport.
- **SC-AC-05:** Given the shared package and existing web/mobile compiler targets,
  when clean installs, package build, consumer typechecks and web build/tests run,
  then generated methods and parameter/body/header types are consumed without
  suppressing peer errors or casting arbitrary successful JSON to caller types.
  Mobile test is compile-only; no device/network/native capability claim.
- **SC-AC-06:** Given final changes, when regression and bounded config/secret/
  artifact audits run, then backend verify/architecture and existing browser
  regressions pass, V1/V2 and business source remain unchanged, secrets/raw
  credential-bearing reports are unpublished, and evidence records exact commands,
  versions, counts/skips/commit. Remote required checks are checked after a
  separately authorized publish, not asserted from local tests.

## Edge cases and preconditions

Request string trimming precedes validation in DraftInput; do not describe raw
whitespace as valid merely because minLength passes. Decimal weight wire value
remains a JSON number; no money DTO exists. Null merchantId/event reason must
stay nullable. Method/path/header prefixing, relative browser base URL, 200 array
instead of envelope, replay201, immutable receipt/current representation,
JSON parse failure and live account/role revalidation are regression cases.
No token refresh endpoint or refresh-token mechanism exists in this slice.

Contract authority, three drift controls, schema-valid versus deliberately invalid
fixtures, exact-version metadata evidence and application-only rollback are
defined in the revised design. C-01/C-02 package gates and C-03 HTTP/PG gates now
PASS; consumer/browser/final gates are NOT RUN. C-03 evidence proves its executed
wire fixtures, while C-01 static checks alone do not prove HTTP conformance.

Preconditions: approved DEC-29, unchanged Demo-001/accepted boundaries and baseline
toolchains, network permission to resolve approved dev tools, safe isolated test
credentials/database and available test ports. Credentials are generated into
ignored storage/environment; never copy developer credentials into fixtures.
No dependency on unapproved full002/003/005/native/provider policies is introduced.

Implementation tests: TEST-001-CONTRACT-01/02 PASS locally; TEST03 package and
TEST04 generated-transport portions PASS; TEST05/06/07 actual HTTP/PG PASS.
Consumer/adapter portions and TEST08–10 remain NOT RUN for final slice completion.
No full slice completion is implied.
See [design](design.md) for wire inventory and
fixtures; [tasks](tasks.md) for DoR/DoD and D1-07 implementation instructions.
