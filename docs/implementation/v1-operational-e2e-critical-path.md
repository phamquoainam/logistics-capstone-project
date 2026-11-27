# V1 E2E CRITICAL PATH & IMPLEMENTATION PLAN

**Owner priority effective 2026-11-26 · SOURCE/TEST audit.**
**Latest result2026-11-27:** `V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST`.
The initial inventory and PARTIAL findings below are historical. Both audiences
now have genuine HTTPS/PG18 Booking→all-Parcel Intake→Custody→Tracking evidence;
V48/V49 schema/owner/ACL gates pass. See
[final Slice1 evidence](v1-dual-audience-source-test-execution.md).
The next missing step is shared Spec012 Inventory→Sorting→Manifest, requiring
a bounded approved contract. No deployment or downstream readiness is implied.
Current task: operational shipment E2E; portal completeness follows V1.
Read AGENTS, full Project North Star, current source, canonical spec register,
NWS/Business shipping/P0/Claims contracts and evidence. This report does not
authorize new APIs, schema, grants, deployment or business state transitions.

## 1. Disposition and preservation

**Menu2 Step B = DEFERRED_POST_V1 — NOT APPROVED FOR EXECUTION.**
This supersedes its earlier technical-review priority, not accepted business
policy or preparation evidence. No Lifecycle purpose/permission/guard/hold/
RESTRICTED/CLOSED/Owner Transfer implementation is started.

Preserve the original Step B manifest bytes and SHA-256:
`b88e6d9df08c60d78d45afde9883dc424ed5ccb9aa5b12033f271d39e9cd1cd4`.
The original proposal's embedded status is historical; effective Owner disposition
is DEFERRED_POST_V1 in this record and Menu12 readiness. Do not rewrite the hash
or classify it REJECTED/COMPLETED. Keep Menu1, M2-R01, M2-L01 preparation,
V46/V47/current-owner bridge, NWS and all prior evidence.

Branch `develop`, HEAD `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`.
Preflight `.demo/v1-e2e-preflight.json` fingerprints 165 baseline files, including
all 46 migrations V1–V47 (no V33), WIP, approved artifacts and staged index.
No stash/reset/clean/repair, shared/demo/UAT/Production access or AWS changes.

## 2. Code-grounded implementation inventory

`IMPLEMENTED_AND_VERIFIED` below identifies bounded accepted component evidence;
it does not assert a fresh exact-current-revision run or an integrated V1 PASS.
`IMPLEMENTED_NOT_INTEGRATED` means source exists but the required downstream
transition or same-journey evidence is absent. Spec/register target status alone
is not used as implementation evidence.

| V1 capability | Canonical source / actual code | Current classification and evidence |
|---|---|---|
| Register, verify, Customer login | Identity credential/proof/session engines, `ProductionAuthenticationHttp`, Customer UI | IMPLEMENTED_AND_VERIFIED, bounded historical Customer HTTPS journeys; preserve current audience/security controls |
| Business Application → independent review → activation | Merchant V41–V43, `JdbcMerchantGovernance`, genuine Identity contexts and protected evidence; Admin workflow/UI | IMPLEMENTED_AND_VERIFIED component; real Customer-to-independent activation artifact `e341ff…` 45/45 historical PASS including HTTPS parent; not a newly run full V1 case |
| Shipping enablement of new Organization | V43 `activate_approved_application` inserts only dashboard.read; existing `configure_local_workspace` and `configure_local_business_contract` govern later explicit grants/contract | IMPLEMENTED_NOT_INTEGRATED as an automatic onboarding→shipping journey. Do not silently auto-grant Owner or price a new Org with Personal tariff. Isolated integration must explicitly record trusted operator setup through approved functions |
| Merchant single Draft, Quote, Booking, Parcel/label | `BusinessPreparationHttp`, `BusinessShippingHttp`, `JdbcBusinessQuotes`, `JdbcBusinessShipments`, V24–V26 | IMPLEMENTED_AND_VERIFIED bounded Local/Test component: actual Merchant tariff/TTL/snapshot, Organization+Facility RBAC, immutable Parcel IDs and PDF; business-shipping-tools report records 39/39 focused tests and real Browser parent. These are historical component results |
| Excel/CSV import | browser Worker/grid → existing server preview/validate/idempotent commit to Organization Draft | IMPLEMENTED_AND_VERIFIED for Draft creation; import-grid report records real HTTPS XLSX→validation→persisted Draft. Bulk Quote/Booking is separate; an imported Draft is not a Shipment or acceptance |
| Finance confirmed fee/obligation | `JdbcBusinessObligations`, `finance.pilot_shipping_obligation`, V26 | IMPLEMENTED_AND_VERIFIED only UNPAID obligation/snapshot/read bindings. No collection, PAID evidence, ledger settlement or reconciliation engine implemented |
| Dedicated Operations Staff/site scope | V44/V45, `JdbcOperationalStaffAdmission`, `JdbcCarrierNetworkReads`, `/ops/station` | IMPLEMENTED_AND_VERIFIED NWS foundation: separate OPS audience, real site assignment/version/revocation. Network entries/topology do not imply transport execution |
| Station Intake/custody/tracking | `OperationalStationHttp`, `JdbcPilotHandling`, `JdbcStationIntake`, V21/V45 canonical custody/event tables | IMPLEMENTED_AND_VERIFIED for PERSONAL only. `JdbcPilotHandling.lookup/atStation` explicitly restrict PERSONAL; personal finance handover reader matches PERSONAL. Business extension BLOCKED_CONTRACT |
| Merchant sees current Carrier acceptance | `JdbcBusinessShipmentManagement`, existing Business Shipment DTO and reports | PARTIAL. Booking snapshots preserve NOT_ACCEPTED; no Business carrier tracking reader. Management assumes pre-acceptance state and waiting count. Cannot mark acceptance by editing the immutable snapshot or fabricate a timeline |
| Parcel Inventory | real `dispatch.pilot_custody` / intake/Parcel events exist | PARTIAL: source for custody at intake exists, but no scoped Inventory API/current location projection and no custody movement lifecycle. No second inventory store permitted |
| Sorting / Manifest | Dispatch production source inventory has Station/site readers/intake; no sorting/manifest command/storage | MISSING + BLOCKED_CONTRACT. Spec012 is a target with DECISION_REQUIRED API/schema/permissions; no approved Spec012 technical implementation manifest found |
| Transfer / Hub Receiving / destination arrival | no production Transfer/receiving state machine or sender/receiver custody events | MISSING + BLOCKED_CONTRACT (spec013). Hub metadata or Merchant WFP handover PDFs are not receiving evidence |
| Dispatch / Courier / Attempt / POD / Delivered | no DeliveryRun/dispatch/attempt/POD backend; `app/frontend/mobile/App.tsx` is default Expo starter | MISSING + BLOCKED_CONTRACT (spec014/015/029); no actual Courier login/assignment app, no recipient confirmation/POD implemented |
| Failure / retry / return | support may record allegations; no physical attempt/retry/return custody state machine | MISSING for logistics execution (spec015/016). Ticket text cannot become failed delivery or return evidence |
| Merchant tracking / Admin oversight | scoped Business list/lookup and Admin Shipment/Parcel/UNPAID reads exist | PARTIAL; need actual new source events for Business acceptance and downstream lifecycle. No public recipient tracking channel inferred from private Customer lookup |
| Support / Claims | Orders-owned Organization Support V35, Claim Intake V38, evidence, CSKH conversations and lifecycle | IMPLEMENTED_AND_VERIFIED bounded ticket/intake/review/supplement/closure with historical HTTP/Browser evidence. Not compensation entitlement, award, payout or physical delivery verification |

Actual production Dispatch files are Station intake/network APIs/configuration and
JDBC readers/intake. Finance files are obligation writers/readers/configuration.
No production sorting/manifest/transfer/DeliveryRun/payment/reconciliation table
was found in the current Flyway stream. Merchant V32 packing/handover preparation
must never be counted as a Carrier manifest or custody transfer.

## 3. Evidence audit and limits

Read the existing protected XML and fingerprint artifacts without opening secrets,
mail or raw HTTP payloads. Sanitized result:
`.demo/v1-e2e-evidence-audit.json`.

| Existing run | Actual parsed result | Current fingerprint comparison |
|---|---|---|
| NWS `85576b6743d54da2b6051316e9c9fd0b` | 7 tests, 0 failures/errors/skips; genuine Ops invitation, scoped intake, HTTPS+PG18 parent and upgrade | 54/63 recorded files unchanged; 9 changed, including common HTTP config/test host/UI. Core Dispatch classes remain unchanged; cannot claim full current host equivalence |
| Merchant P0 `e341ff862d8b40c09d87fdca83db0892` | 45 tests, 0 failures/errors/skips; 44 security + actual Customer→independent activation HTTPS parent | 117/148 unchanged; 31 changed since run. Keep historical acceptance; current consolidated Slice1 must exercise impacted integration |

Newer accepted R01/preparation evidence remains in Menu12 readiness. It does not
prove Business Station Intake or Courier/POD. No old PASS is relabelled FAIL;
neither is it presented as a fresh run. Reuse unchanged domain evidence, then run
one focused integrated scenario when Business Intake authority is ready.

## 4. Critical path and smallest executable slice

**Slice1 is not ready to execute end-to-end today.** The first runtime blocker is
Merchant Booking→Station, followed by the missing Merchant current-tracking
projection. New Organization also needs explicit shipping grants/contract setup;
activation deliberately does not perform those actions.

1. Reuse verified enrollment, application, independent review, activation.
2. In isolated test setup, explicitly configure only the synthetic target Org's
   shipping grants and tariff using existing approved operator functions; record
   receipts. Do not insert fake Bookings, grant all Owners or change real accounts.
3. Merchant creates one Draft with two Parcels, obtains actual Merchant Quote,
   confirms one Booking/UNPAID and downloads actual Code128 labels.
4. **After exact approval of [BIA-V1 delta](v1-business-station-intake.delta.md):**
   dedicated OPS actor at exact origin site scans both Parcels; one atomic
   acceptance/custody/tracking/audit/receipt commit; Merchant reads real events.
5. Assert original Booking/price/Parcel snapshots and UNPAID unchanged; no Pickup,
   PAID, IN_TRANSIT or DELIVERED; wrong site/authority/replay/race/rollback deny.

This adds no new Quote, Booking, Identity or custody engine. A PERSONAL NWS
scenario is reusable regression evidence, not a substitute for this Merchant
scenario. No new production code can close this blocker within the existing
PERSONAL-only intake approval. Do not run an inevitably blocked Business scenario
merely to produce another known-red log.

## 5. Delivery order and parallel ownership

| Priority / workstream | Accountable domain / canonical source | Dependency and acceptance |
|---|---|---|
| 0: Business Intake extension + tracking | Orders + Dispatch + Finance; Workflow composes public ports, Merchant eligibility | BIA-V1 exact SOURCE/TEST approval and shared-owner coordination; finish Slice1 before claiming its E2E PASS |
| 1: Inventory → Sorting → Manifest | Dispatch, canonical intake custody + Orders Parcel IDs; Merchant-approved route/site versions | Spec012 bounded technical contract; current-holder query, authorized sort evidence, exact two-Parcel Draft→Seal, immutable set, no movement on seal |
| 2: Transfer → Hub Receiving | Dispatch with Orders events | Spec013 sender/receiver confirmations, actual discrepancy policy, exactly one current holder, no transfer from topology alone |
| 3: Dispatch → Courier → POD | Dispatch assignments; Identity Courier admission; Orders attempt/POD; Mobile UI | Spec014/015/029 exact permission/proof/storage/recipient confirmation; real custody handoff and attempt before Delivered |
| Parallel design: money | Finance, existing confirmed obligation as upstream | Decide supported V1 verified fee-payment method/authority/receipt/reconciliation. NO_COD is smallest proposed option, not an Owner-approved payment engine |
| Parallel design: failure/return + support | Orders/Dispatch; reuse CSKH intake/evidence | Physical exception/retry/return events first; link existing Ticket/Claim. Compensation remains blocked on policy |
| Parallel integration consumer | Customer + Admin readers owned by respective teams | Consume approved event ports, no new oversight engine or shared Menu6–9 implementation overwrite |

These are team ownership work packages, not permission to spawn agents, contact
teammates, edit their WIP or assume unmerged GitHub work is available. Exact shared
source revision/interface coordination precedes edits. Existing AWS/S3 provider
configuration is reused only when approved POD/evidence requirements need it;
no infrastructure mutation is part of this audit.

## 6. Acceptance map — all are NOT RUN as integrated V1 scenarios

| Owner scenario | Required real actions and assertions | Present blocker |
|---|---|---|
| V1-E2E-01 | Customer registration/verification/login → application/contact/evidence → independent reviewer/decider → atomic Org/Owner activation | Existing component can be reused; common-host regression at Slice1 integration, no new authority |
| V1-E2E-02 | Explicit authorized shipping setup → Merchant two-Parcel Quote/Booking → label decode/ownership/snapshot/UNPAID | Existing bounded source; do not infer automatic grants/contract from activation |
| V1-E2E-03 | Actual XLSX Worker → server row errors/preview → explicit idempotent Draft commit → Quote/Booking for valid Draft | Draft import evidence exists; imported rows are not automatically Booked; include a valid row in later consolidated flow |
| V1-E2E-04 | Merchant intake → scoped Inventory → real sort → exact sealed Manifest → sender transfer → receiver scans | Business Intake approval, then Spec012/013 contracts and implementation |
| V1-E2E-05 | destination receiving → assigned DeliveryRun → genuine Courier login/custody → attempt/POD → Delivered | Canonical Dispatch/Courier/POD missing |
| V1-E2E-06 | Existing UNPAID obligation → genuine authorized payment evidence → matching reconciliation | Finance method/authority and execution contract missing; no fake transaction/COD/settlement |
| V1-E2E-07 | physical failed attempt/return or shipment issue → Merchant Ticket/Claim → assigned CSKH review/reply/closure | Reuse Support/Claim intake; downstream physical exception source missing; compensation excluded |
| V1-E2E-08 | wrong tenant/site/actor/audience, revoke/expiry, duplicate/stale/concurrent commands, rollback and custody conservation at every boundary | Test per new slice; no full-suite reruns unless impacts justify |

All scenarios use genuine HTTPS Backend/PostgreSQL and real business commands.
Synthetic Identity/operator/site/contract setup must be labelled fixture setup;
no direct-SQL business acceptance/transfer/POD/payment facts. Loopback SMTP is
test verification, not real customer mailbox delivery.

## 7. Current outcome and next gate

Completed now: code/evidence inventory, critical-path plan, Step B deferral,
preservation and focused technical approval proposal for the first blocker.
No runtime feature, migration, deployment or real grant was performed. No Backend,
PG18 or Browser suite was rerun during this documentation-only audit.

**Next: approve BIA-V1 SOURCE/TEST and coordinate the named owning interfaces;
then execute the single Merchant→Station HTTPS/PG18 slice.** Follow with Parcel
Inventory→Sorting→Manifest. Do not return to Menu2 Step B or Full Portal work.

Readiness: **V1_CRITICAL_PATH_AUDITED — SLICE_1_BLOCKED_CONTRACT**.
Not `V1_OPERATIONAL_E2E_ACCEPTED`, Full Specification, Local UAT or Production Ready.

### Verification recorded for this audit

- Document/reference/source-characterization checks: **15/15 PASS** in
  `.demo/v1-e2e-document-validation.json`.
- `python docs/reconciliation/v3/validate.py --implementation`: exit0,
  ownership CONSISTENT; `git diff --check`: exit0.
- Fresh read-only GitHub inventory: 51 branches, 0 open PRs; complete develop,
  admin-dashboard, business-shipping-tools, merchant-facilities and organization-
  management trees show no V48+. No version reserved; unpublished WIP still
  requires coordination. `.demo/v1-e2e-github-preflight.json`.
- Preservation: 165 baseline fingerprints; only readiness, Step B review banner
  and traceability intentionally changed. Other 162 files, all 46 migrations,
  original Step B manifest, AGENTS/North Star, HEAD and index unchanged. Two new
  review documents; no Backend/Frontend/DB source delta or runtime tests.
- Exact BIA-V1 delta SHA-256 for the one Owner review checkpoint:
  `ba0812bc41572e0870cba0b7a1572426321430530698e9b5aa697f9205aa06ea`.
  BIA-POL/PORT/HTTP/DTO/DB items remain PROPOSED pending approval and shared-owner
  coordination. Existing Business tariff and Merchant approval policy are not
  being resubmitted; only the PERSONAL→BUSINESS intake boundary is new.

## 8. Execution update — 2026-11-27 (supersedes historical proposal status)

Owner's dual-audience directive approves BIA-V1 SOURCE/TEST. Original approved
delta bytes/SHA above and this audit history are preserved. Menu2 Step B remains
DEFERRED_POST_V1. Current evidence and exact changed-source/test inventory:
[dual-audience execution report](v1-dual-audience-source-test-execution.md).

- PERSONAL current-source HTTPS + Backend + PostgreSQL18 intake scenario PASS;
  two real Parcels accepted at the assigned Station, tracking and UNPAID checked.
- BUSINESS PG18/HTTP Quote/Booking/ownership/handling/Management V2 and UI
  presentation checks PASS; Business Ops acceptance E2E remains NOT RUN.
- V48 ownership envelope, scoped owner-neutral reads and Business management
  projections implemented. Dispatch V2 shared intake/composition remains closed.
- BIA-TIMESTAMP-01 awaits exact approval for Booking.createdAt in CarrierCargo.
- A focused catalog gate exposed V46 review-comment table owned by LOGIN
  migrator; BIA-OWNER-01 proposes a forward-only repair. No V46 edit or owner
  assertion relaxation. Cold migrations and preservation pass; ownership FAIL.

Effective readiness: **V1_SLICE_1_PARTIAL — BUSINESS_INTAKE_BLOCKED_CONTRACT;
SCHEMA_OWNERSHIP_FIX_REQUIRED**. Neither Business acceptance nor dual-audience
readiness is inferred from PERSONAL PASS or intercepted UI fixtures. Complete
these gates before Spec012 Inventory→Sorting→Manifest; no new downstream engine
or real environment mutation is authorized by this status update.

## Final Slice1 disposition — 2026-11-27, supersedes preceding PARTIAL status

**V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST**. Final Business HTTPS/PG18
run `2e00170d4f234bbfb9897d3dd72262c8` passed1/1 through canonical application
approval, explicit shipping setup, real Quote/Booking/labels, Ops verification/
login, exact two-Parcel acceptance and Merchant AT_STATION. Personal HTTPS
passed on identical production source. Database ownership/complete deferred
guards/ACL, negative security, races/replay/rollback and immutable UNPAID gates
passed. Earlier failures and fingerprints remain in the detailed execution log.

The next smallest unblocker is
[SPM-V1 review proposal](spec012-station-preparation-contract.proposed.md): one
shared Dispatch custody reader, explicit destination routing rule, versioned
sort evidence and exact-set Draft→Seal manifest. It identifies site permissions,
API/DTO/port, schema/ACL and four business-policy decisions in one review package.
No downstream code, migration reservation or real authority has been created.
This proposal does not accept full Spec012 or resume deferred Menu2 Step B.
