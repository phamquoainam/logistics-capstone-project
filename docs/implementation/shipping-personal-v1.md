# ArgoPost Shipping V1 / Personal — delivery tracker

Authority: Owner directive cb874be9-2dc4-4108-8fbc-e5dc8365db69 and subsequent
controlled-pilot decision, 2026-11-01. One working directory D:\TLCN\e-logistic.
Baseline: PR37 merged manually into develop fecd646fcf2e3ee9fae0d2bc598cc57623e1d95f;
clean root fast-forwarded safely; feature/shipping-policy-personal-v1, no new worktree.

[Business decision packet](../decisions/shipping-policy-v1-owner-packet.md) and
[bounded draft contract](personal-shipping-drafts-contract.md) distinguish authorized
implementation from candidate commercial/operational values. No Full Personal acceptance.

## Execution tasks

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
