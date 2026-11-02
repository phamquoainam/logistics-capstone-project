# ArgoPost Shipping V1 / Personal — delivery tracker

Authority: Owner directive cb874be9-2dc4-4108-8fbc-e5dc8365db69 and subsequent
controlled-pilot decision, 2026-11-01. One working directory D:\TLCN\e-logistic.
Baseline: PR37 merged manually into develop fecd646fcf2e3ee9fae0d2bc598cc57623e1d95f;
clean root fast-forwarded safely; feature/shipping-policy-personal-v1, no new worktree.

[Business decision packet](../decisions/shipping-policy-v1-owner-packet.md) and
[bounded draft contract](personal-shipping-drafts-contract.md) distinguish authorized
implementation from candidate commercial/operational values. No Full Personal acceptance.

## Current checkpoint — approved Personal pilot, 2026-11-02

Authority: final Owner approval of ARGO-SIM-COVERAGE-1 and
[BR13-LOCAL-PILOT-01](../decisions/br13-local-pilot-01.md).
[Exact API/data/transaction contract](personal-pilot-booking-contract.md).
Earlier draft-only states below are historical and superseded within this pilot.
PR38 was subsequently merged at0704a6bfb42455a89bcffb16eb731108d0b3e121;
post-merge CI41331696199 SUCCESS on that exact merge. The original checkpoint
evidence below is historical. The next bounded increment is tracked separately in
[Milestone B](milestone-b-station-acceptance.md), including labels and the explicitly
approved unpaid simulated Station intake; it does not reopen commercial operations.

- [x] PILOT-DECISION — DONE: exact service/tariff/tax,3 synthetic areas,4 directed
  routes, simulated drop-off only and bounded UNPAID booking amendment recorded.
- [x] PILOT-DATA — DONE: new V20 only; immutable approved coverage/tariff seeds,
  exact quote breakdown constraints, relational Shipment/Parcel snapshots,
  narrow runtime DML and immutable Finance obligation. Upgrade/negative PG tests PASS.
- [x] PILOT-QUOTE — DONE / VERIFIED_LOCAL: actual versioned DB-backed calculator,
  per-parcel integer/dimensional rounding,15m expiry, strict owned source inputs,
  receipt recovery, unsupported-route/goods/measurement denial and idempotency.
- [x] PILOT-BOOKING — DONE / VERIFIED_LOCAL: quote consumption, DRAFT→BOOKED,
  Shipment/Parcels, audit/receipt and UNPAID obligation in one local transaction;
  same/different-key races, expired/stale/foreign denial and fault rollback PASS.
- [x] PILOT-MANAGEMENT — DONE / VERIFIED_LOCAL: actual own Shipment list/search/
  status filter/detail, quote/address/parcel snapshot, UNPAID views and drop-off
  information; unknown POST outcomes resolved by original receipt, no blind retry.
- [x] PILOT-LOCAL-CHECKPOINT — DONE: full PG1040 PASS/5 policy skips, original-DB
  V20 migration/preservation and actual local HTTPS readiness verified below.
- [ ] PILOT-HOSTED-CHECKPOINT — exact-source hosted CI recorded in PR38 metadata
  after commit/push; no previous revision's evidence substituted.

This is **PERSONAL BOOKING scope**, not Full Personal/Spec acceptance. Carrier
Acceptance/Delivered, Pickup, Payment Collection/PAID, COD, labels, Support/Claims,
Production and Merchant operation remain unavailable. No account receives those
permissions. The UI identifies all route/handover/fee data as simulation.

Architecture now has seven nonempty implemented packages: identity,merchant,
orders,pricing,finance,platform,workflow. The nine domain-module architecture and
all dependency/transaction/security rules remain; workflow uses public APIs and
current CustomerAdmission, never client owner fields or cross-module internals.

### Current local evidence

Final focused backend41 PASS includes10 pilot PG cases,2 migration cases, the
actual12-case HTTPS/PG18/test-SMTP browser suite,23 architecture cases and5 domain
cases. Full H2 verify654 PASS/17 policy skips,0 failures/errors. Frontend51 unit
and33 isolated UI cases PASS; after a checkbox alignment refinement, the2 affected
pilot UI cases PASS again. Typecheck/lint/default+customer builds PASS.
Report-policy/privacy130/130 PASS. New and existing OpenAPI/source/whitespace
validators PASS. Desktop/mobile screenshots inspected. Raw evidence remains
ignored under .demo/pilot-v20-*; no live Gmail/S3 retest or new delivery claim.

Focused evidence: .demo/pilot-v20-final-focused-9d9975a4-9b95-46ac-a007-27cbb229a5b1.
H2 receipt: .demo/pilot-v20-h2-sanitized/summary.json. Hosted CI/source SHA will
be recorded in PR38 metadata after normal commit/push; no automatic merge.

Full CI-style PG18 regression:1040 PASS/5 policy skips,0 failures/errors; sanitized
report guard, generated-credential privacy and owned-resource absence audit PASS.
Actual browser12/12 PASS, no page errors; original container restored healthy with
the same identity and data volume. Evidence:
.demo/pilot-v20-ci-pg-e90d2bbc-5abc-4f24-b071-5eaa14eb1527/sanitized and its
customer-browser child's safe-browser-results.json. Rechecking cleanup from a new
shell initially lacked the original execution binding/Docker path and failed closed;
the read-only audit passed with both explicitly supplied. No cleanup gate weakened.

Official local migrate applied V20 successfully. Aggregate fingerprints of existing
account/identifier/profile/application/PW03 current/history and PW04 draft/current/
history/audit/receipts all match the pre-migration checkpoint; V1–V19 checksum prefix
unchanged. No dev Customer/contact/draft/shipment test row inserted. Existing secret,
certificate, AWS/Gmail and PostgreSQL setup preserved; no init/reset/volume removal.
Launcher returned LOCAL_FULL_STACK_HTTP_READY. Fresh-browser trusted HTTPS smoke:
8 protected routes redirect to login,7 Customer APIs return401, no page errors or
TLS bypass. Gmail delivery and S3 were not retested for this shipping-only scope.

Owner local path: sign in at https://127.0.0.1:5173/login, prepare a draft at
/personal/create, open its detail to select an approved synthetic route, declare
ordinary goods, request quote and confirm. Actual records appear at
/personal/shipments; drafts use the same route with query parameter tab=drafts. Every booked
record stays UNPAID/NOT_ACCEPTED and only shows simulated drop-off information.

## Historical PW04 execution tasks — prior draft-only checkpoint

- [x] BASELINE — DONE: fetch/verify manual PR37 merge and synchronize clean root.
- [x] AUDIT — DONE: canonical business rules/state/module ownership and source inventory;
  identify absent operational catalog/coverage/tariff/carrier/finance/support authority.
- [x] DECISION — DONE as a packet, not approval: Owner responded with pilot-in-principle;
  exact activation decisions remain pending. BR13 conflict is explicit, not silently changed.
- [x] DRAFT — DONE / VERIFIED_LOCAL: shared Orders preparation, immutable contact/
  parcel snapshots, current self admission, version/key/audit/receipt, real UI/PW03.
- [x] VERIFY_LOCAL — DONE: focused tests, upgrade/negative/races/browser and required
  H2/PG regression, report-policy/privacy and source/contract checks PASS.
- PUBLISH / HOSTED_CI — recorded subsequently in PR metadata with exact source/integration
  SHA and sanitized artifacts; the source document is the pre-publication checkpoint.

## Capability inventory

| Capability | UI | API | Database | Authorization | Tests | Dependency | Status |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Customer Identity/profile/account | Reused | Existing | V7–V17 | Existing current admission | Prior accepted regression | None for preparation | DONE/reused |
| Address Book | Reused PW03 | Existing6 operations | V18 unchanged | Self only | Prior PR37 + current regression | Raw/unverified only | DONE/reused |
| Shared shipping drafts | Create/resume/list/detail/archive;8 detail sections honest about absent facts |6 draft operations + availability | New V19 orders-owned4 tables | Workflow supplies current self Access; no client owner | Actual PG + HTTPS browser verified | No approved rate or coverage required to prepare | DONE / VERIFIED_LOCAL |
| Service/Pricing foundation | No active service or quote displayed | No quote issuance | No active tariff seeded | No publishing authority invented | Candidate golden calculation tests | Final service/tax/fee/goods rules | PARTIAL/inactive |
| Coverage/service/station lookup | Unavailable with dependency explanation | No authoritative source | No demo coverage seeded | Deny by default | No unsupported route can book | Dataset/region/routes/capabilities approval | BLOCKED |
| Quote/Booking | Disabled | No confirm/quote/payment transition endpoint | No confirmed Shipment/quote/payment rows created | Customer cannot create carrier or money facts | Direct attempts rejected | Packet decisions + operational/Finance source | BLOCKED |
| Shipment management/dashboard | Draft list/detail real; existing profile dashboard/quick actions reused | No accepted-shipment statistics/source | Drafts only | Self scope | Draft tests; no fake counters | Booking and carrier source | PARTIAL |
| Pickup/Tracking/Returns/Labels/POD/Documents | Existing unavailable modules and detail sections | No source | No fabricated events/docs | No Customer operation grant | Negative boundary tests | Authorized Carrier/Dispatch/Document contracts | BLOCKED |
| Billing/COD | Explicit unavailable; no PAID | No collection API | No fake ledger | Finance authority absent | No transition route | BR13 amendment and collection authority | BLOCKED |
| Support/Claims | Existing guidance only, no fake ticket resolution | No accepted Support/Claims intake/handler/evidence lifecycle | No duplicate support master | No carrier/Claims authority invented | Existing source-denial UI | Owning spec020/021 contract/case types/authority | BLOCKED |
| Business Workspace | Existing Personal dropdown, Business unavailable | No membership provider | No org grant | Approved Merchant application is not membership | Existing security tests | Current membership/permissions provider | BLOCKED/outside full implementation |

Architecture inventory now includes six nonempty implemented Java packages
identity/merchant/orders/pricing/platform/workflow. pricing contains only an
inactive candidate calculator; not a service/tariff/quote implementation. No
ArchUnit direction, encapsulation, negative-fixture or transaction rule changes.

## Evidence and limits

Initial domain/architecture26 PASS. New draft focused PG34 PASS including8 HTTP/DB
ownership/race/replay/rollback/security tests. Final focused run had actual browser
11/11 PASS and V18→V19 preservation PASS; one stale architecture inventory failed
after pricing acquired its first real class. Inventory updated specifically; no
gate relaxed. Initial V19 function CASE syntax failed in disposable tests and was
fixed before any deployment; V1–V18 immutable. Frontend47 units and focused2 UI PASS;
typecheck/lint/customer build and OpenAPI validator PASS.
All raw evidence stays ignored in .demo; no real Gmail/AWS test or credential access.

Final local checkpoint: clean H2 verify652 PASS/17 policy skips (Architecture23
included); full CI-style PG18 verify1026 PASS/5 policy skips,0 failures/errors.
Sanitized report guard, generated-credential privacy and owned-resource cleanup
PASS; original container restored healthy with volume unchanged. Real HTTPS/PG18/
test-SMTP browser11/11 PASS/CLEANUP/NONE, LOCAL_UNPUBLISHED. Full frontend47 unit,
31 isolated UI, typecheck/lint/default+customer builds PASS; mobile screenshot
viewed; OpenAPI/source validators and whitespace checks PASS. Report-policy130/130
PASS with explicit new test/source/migration inventories and all prior negative
controls retained. No redundant rerun after these successful checkpoints.

Evidence directories (ignored): .demo/shipping-h2-sanitized and
.demo/shipping-v1-ci-pg-9b5b85e9-ecae-409b-bff7-97671cb50054/sanitized;
safe browser receipt under that run's customer-browser child. Earlier focused
failures remain accurately preserved: fixture helper typo, new V19 CASE syntax,
OpenAPI optional license metadata and architecture's old nonempty-package inventory.
All fixed specifically; no security/architecture rule or report guard bypass.

Official local launcher migrated V19 and returned LOCAL_FULL_STACK_HTTP_READY.
Aggregate fingerprints of existing account/identifier/profile/application **and
PW03 current/history** were compared before/after; all unchanged. Earlier migration
version/checksum prefix preserved, no dev test account/contact/draft inserted.
No init, certificate replacement, key rotation, old migration edit, DB reset or
extra worktree. Local usage: sign in, then https://127.0.0.1:5173/personal/create
or /personal/shipments. Gmail configured, no new live delivery/S3 verification claim.

Next business action is complete Owner disposition of the packet's final activation
table, especially exact pilot data/capability, tax/fees/goods limits and bounded
BR13 exception/collection authority. Independent preparation is delivered; Personal
Full Feature Ready, active pricing, booked shipments and operational acceptance
are explicitly **not** achieved by this checkpoint.
