# V1 dual-audience Slice 1 — source/test execution

> **Active continuation — 2026-11-27:** Owner's UNBLOCK V1 directive approves
> BIA-TIMESTAMP-01 and BIA-OWNER-01 after exact delta verification. Subsequent
> explicit BIA-DB-GUARD-01 approval binds SHA-256
> `6950d9601e81b5a6da3589d492cae5f990db64b82fa254b50172da5fc42ed240`.
> Historical PARTIAL findings below are preserved; completed acceptance is
> recorded in the final continuation section. Original proposal bytes stay
> unchanged. No deployment or Git publication is authorized.

2026-11-27 · **V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST**.
The initial PARTIAL report below is historical; the final acceptance section
records the superseding evidence and remaining downstream limitations.
No deployment, real grant, SMTP email, Git publication or downstream logistics
execution is authorized or claimed here.

## Authority and preservation

Latest Owner directive: V1 DUAL-AUDIENCE SHIPMENT E2E, both PERSONAL and BUSINESS.
BIA-V1 is approved SOURCE/TEST by that directive. Original approved proposal
bytes retain their historical status text; this execution record supersedes
that status, without rewriting the approval artifact:

- `v1-business-station-intake.delta.md` SHA-256:
  `ba0812bc41572e0870cba0b7a1572426321430530698e9b5aa697f9205aa06ea`.
- Menu2 Step B stays DEFERRED_POST_V1. Its original manifest remains
  `b88e6d9df08c60d78d45afde9883dc424ed5ccb9aa5b12033f271d39e9cd1cd4`.
- Baseline: develop `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`;
  `.demo/bia-v1-preflight.json` inventories 537 source/WIP files and the index.
  All 46 existing V1–V47 migration files (no V33), HEAD and index remain unchanged.
- Migration preflight `.demo/bia-v1-migration-preflight.json` inventoried 51 remote
  branches / 0 open PRs and relevant complete trees before creating V48. No
  subsequent migration number is reserved by this report.

## Implemented within the approved bounded contract

| Owner / files | Actual change | Limit |
|---|---|---|
| Orders `PilotHandling`, `JdbcPilotHandling`, `PilotBookingConfiguration` | Owner-neutral canonical carrier reads, original PERSONAL compatibility, Organization-owned handling and shared tracking projection; new intake flag defaults false | CarrierCargo intentionally has no unapproved createdAt field |
| Merchant `PilotCoverage`, `JdbcPilotCoverage`, `CustomerProfileConfiguration` | Genuine Ops context/physical transaction, existing Merchant lock, ACTIVE Organization/current Owner/facility eligibility | No Staff Merchant Membership or real grant |
| Finance `BusinessObligations`, `JdbcBusinessObligations` | Exact read-only BUSINESS obligation/quote/amount/facility/UNPAID binding | No collection, PAID, COD or ledger write |
| Orders management port/reader/filter/config and Workflow `OrganizationShipmentManagementHttp` | Opt-in schema2 handling in a scoped cohort; actual WAITING_FOR_HANDOVER / AT_STATION, correct waiting count | Immutable Booking carrier_state remains snapshot; old V1 fails closed for an accepted result cohort |
| Workflow `BusinessShippingHttp` | Approved self-Organization/facility handling endpoint using existing permission | No public tracking or creator-as-owner substitution |
| Dispatch `OperationalStationIntake` | Approved owner-neutral overload declarations, default fail-closed | Shared implementation and OPS V2 HTTP/composition remain unfinished; no Business acceptance claimed |
| V48 `business_station_intake_ownership.sql` | Owner tags, XOR checks, composite Shipment/Organization/facility/acceptance and Parcel FKs; original PERSONAL facts retained | No new grants/roles/tables; strengthened direct-DML/complete pipeline gates still pending |
| Customer `client`, `business-management-data`, Business shipment tabs/detail/list | Exact management V2 parsing and current tracking presentation, immutable snapshot distinguished from actual events, UNPAID retained | Fixture UI evidence is presentation-only |
| Migration tests / implementation inventory | Current version list, forward migration count and exact table catalog inventory | Ownership assertions retained; real V46 owner defect remains FAIL |

## Actual test evidence

Protected artifacts are local `.demo/cskh-focused-<run>/`; no credentials,
proofs, raw mail or private logs are copied into this report.

| Gate | Observed result / run | Meaning |
|---|---|---|
| TDD schema | `be038b2c7d51412fb91a46ff4f4ded72`: 1 FAIL; `8559327358fa4c3fa764a59b14ad36b4`: 1 PASS | Missing schema reproduced, additive envelope applied |
| Compile failure retained | `5636db262436468795db54e4febacdda`: compilation failed, no fresh tests | Duplicate method insertion corrected; not counted as tests |
| TDD handling | `1cb3a4658c62495285316bb46c6ed927`: 1 FAIL | Endpoint absent before implementation |
| First integrated regression | `efa5c09e29ef4e5bb4619748d165aee3`: 3/3 PASS | Schema, Business handling/ownership and existing PERSONAL intake |
| TDD management V2 | `f96df81b7b394066a9f331ffd4c9789d`: 1 FAIL | Old response did not satisfy approved V2 |
| Implemented Backend gates | `bad063ec46f84ba1910aaf06562a74c2`: 3/3 PASS, source unchanged | Real PG18 + HTTP: ownership envelope, Business quote/book/handling/management, Personal NWS scope/atomicity |
| Current HTTPS/architecture batch | `7d8233a9539343cb8dbea4fafea926c7`: 7 tests, 5 PASS / 2 FAIL, source unchanged | Personal real HTTPS+PG18 PASS, four architecture checks PASS; two migration assertions failed |
| Focused migration follow-up | `623c8b70941047b7ac0d2535e76f6ca6`: 3 tests, 2 PASS / 1 FAIL, source unchanged | Cold 47-migration stream and populated V43→V48 identity/profile/address/checksum preservation PASS; exact catalog ownership FAIL |
| Parser/management unit tests | 13/13 PASS | New V2 parser assertions plus existing Organization management tests |
| Business UI presentation test | 1/1 PASS, build/typecheck PASS | Intercepted synthetic fixture only; not Business intake E2E |
| Typecheck and targeted ESLint | Exit 0 | Existing build chunk-size warning is retained; no Backend full suite or CI result claimed |
| Documentation/ownership inventory | `python docs/reconciliation/v3/validate.py --implementation`: exit0, CONSISTENT, no errors; `git diff --check`: exit0 | The prior undeclared-path failure was corrected by inventorying V48 and the two new tests; no authority granted by the inventory |

Backend fingerprints:

- 3/3 Backend batch: `91348746ea8e8d4be28e6a8de1f20144dbcdb43dfa0f9ad4a06eca5a4a815700`.
- HTTPS/architecture: `0c1e6e18d180d3b47a1dbc063aa74ab161a55815768dc1c438de295bc4466fef`.
- Migration follow-up: `b20e7cbb04ff109a23dcf7ca2bbcfd9259f8473cc89a182b343437a9b647a464`.

The last two batches differ only by targeted migration-test corrections; no
production source changed after the Personal HTTPS PASS. Latest fingerprint is
not substituted for an unexecuted Business HTTPS acceptance.

Commands used (repository root unless stated):

```powershell
.\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#biaOwnershipSchemaBindsCarrierEventsToCanonicalOwners+biaBusinessHandlingRetainsOrganizationOwnershipAndUnpaid+nwsScopedIntakeReusesAtomicCustodyAndCurrentAuthority' -Offline
.\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#nwsRealHttpsBrowserAndPostgres,ArchitectureTests#modulesOnlyUsePublicApis+publicApisDoNotLeakInternals+modulesFollowApprovedDependencyDirection+moduleGraphHasNoCycles,PostgresTestcontainersTests#schemasAndObjectsBelongToOwner,IdentityMigrationTests#nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary' -Offline
.\scripts\ci\run-cskh-focused.ps1 -Tests 'PostgresTestcontainersTests#schemasAndObjectsBelongToOwner+coldStartAppliesV1AndV2,IdentityMigrationTests#nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary' -Offline
# app/frontend/web:
npx.cmd playwright test -c playwright-unit.config.ts business-intake-read.spec.ts organization-management.spec.ts
npx.cmd playwright test -c playwright-customer-ui.config.ts business-intake-read-ui.spec.ts
npm.cmd run typecheck
npx.cmd eslint src/customer/business-management-data.ts src/customer/client.ts src/customer/BusinessShipmentTabs.tsx src/customer/BusinessShipments.tsx src/customer/OrganizationManagement.tsx unit-tests/business-intake-read.spec.ts ui-unit-tests/business-intake-read-ui.spec.ts --max-warnings 0
```

## Separate audience acceptance

**PERSONAL:** actual HTTPS Browser + Backend + PostgreSQL18 scenario PASS on
current production source. Dedicated Operations invitation/verification and OPS
login, genuine Customer login/Quote/Booking/two Parcels/labels, exact-site scan
and acceptance, canonical tracking, unchanged UNPAID and desktop/mobile render.
Safe Browser result: one case passed, zero errors; visual mobile artifact was
inspected. Synthetic account/operator/site setup is test fixture setup, not
real deployment or complete public registration acceptance.

**BUSINESS:** actual PG18/HTTP quote/book/handling/Management V2 ownership and
UNPAID checks PASS. Test setup uses the existing Local/Test operator fixture
and synthetic pricing-contract fixture; it is not canonical Merchant-application
approval → Ops acceptance E2E. No Business acceptance, custody or tracking event
was manufactured to close that gate. V1-S1-B remains NOT RUN/BLOCKED.

## Exact outstanding blockers

1. **BIA-TIMESTAMP-01 — BLOCKED_CONTRACT**: approved CarrierCargo omits immutable
   Booking.createdAt needed by Dispatch scan-time validation. Pending exact DTO
   amendment at `v1-business-intake-carrier-timestamp.delta.md`. Do not substitute
   the clock, cross-read private Orders SQL or fake a PERSONAL Shipment.
2. **BIA-OWNER-01 — SECURITY FINDING / FIX REQUIRED**: actual catalog has
   `merchant.application_review_comment` owned by LOGIN migrator. V46 RESET ROLE
   is the cause. The ownership gate now compares the complete exact relation set
   and rejects unexpected owners; it is not weakened to accept this owner.
   Proposed minimal forward correction: `v1-review-comment-owner.delta.md`.
3. **MISSING_APPROVED implementation/gates after unblock**: Dispatch shared
   owner-neutral core + Ops V2 composition, complete ownership/direct-DML guards,
   Business wrong-scope/finance mismatch/revocation/race/replay/rollback and
   consolidated Merchant-approval → Business shipping → intake HTTPS scenario.
   None is marked PASS based on the available read-only Business fixture.

Current readiness: **V1_SLICE_1_PARTIAL — BUSINESS_INTAKE_BLOCKED_CONTRACT;
SCHEMA_OWNERSHIP_FIX_REQUIRED**. Not BIA-V1 READY, not dual-audience Slice1
READY, not Local UAT or Production Ready.

## Next package after Slice1 acceptance

Spec012: one shared Dispatch-owned canonical custody inventory, eligibility and
versioned sort decisions, exact real Parcel-set Manifest Draft→Seal. Reuse
Orders Parcel IDs, NWS current site admission and accepted custody. Do not create
an inventory mirror, movement on Seal, or Merchant handover evidence masquerading
as Carrier acceptance. Exact API/schema/permissions/transitions need a bounded
Spec012 contract before runtime implementation; Transfer/Dispatch/POD follow
their own owning contracts. Menu2 Step B remains deferred.

## Approved unblock implementation — 2026-11-27 continuation

This section supersedes the old blocker descriptions above without deleting
their failure evidence. BIA-TIMESTAMP-01, BIA-OWNER-01 and BIA-DB-GUARD-01 are
Owner-approved for isolated SOURCE/TEST. Approval is not itself a test result.

- CarrierCargo now carries authoritative immutable Booking.createdAt. Dispatch
  re-reads the cargo through Orders before accepting normalized facts. Both
  audiences use one scan/accept/custody implementation; legacy Personal receipt
  digests and DTOs remain compatible.
- Ops V2 composes genuine Identity, current Merchant Organization/facility,
  Dispatch site authority and Finance's exact existing UNPAID binding. The
  Merchant lock precedes Dispatch authority; receipt retrieval and replay also
  revalidate current admission. No new business authority or collection.
- V48 strengthens the exact two approved deferred guards. Their SECURITY
  INVOKER/signatures/owners/ACL remain unchanged, and existing runtime SELECT
  privileges suffice, including audit/receipt validation. No SELECT was granted.
- Forward-only V49 repairs the catalog-proven V46 review-comment table owner.
  A populated guarded comment and all historical checksums survive the repair.
- Ops transport accepts only its explicitly negotiated V2 media type; an actual
  HTTPS run exposed the former JSON-only response rejection. Business details
  read current tracking while retaining the original Booking snapshot.

Current migration SHA-256 values:

- V48: `be5fc80332e19aa89302e2548e8c7fc4402192da18f7f84e60889d81aea15321`.
- V49: `92976319fb025afb313d97373f83956488ea1c1957ca4f0c059dee27247a0316`.

### Focused evidence retained

All run directories below are protected `.demo/cskh-focused-<id>`. Reports and
source fingerprints remain local; no credential/proof/raw email is reproduced.

| Run | Actual result | Disposition |
|---|---|---|
| `27a35771674a43d6b28ecc4fd1aee2b5` | 5 tests, 4 PASS / 1 FAIL | Cold migrations, ownership, populated upgrade and Personal intake passed. Business source used nonexistent `origin`; corrected to canonical `admission_origin`. |
| `e789b5abc5a94734aca0c26794cec563` | 1/1 PASS | Initial real Business HTTP intake. |
| `4013b37eeb2a479bb12eafd57cf950db` | Business Browser FAIL | Wrong Parcel selector; changed to Business Parcel component. |
| `acf8771fad1144a1884159546371c1fa` | 11 tests, 7 PASS / 3 FAIL / 1 ERROR | Migration/ACL/populated-comment upgrade passed. Corrected Dispatch-to-Pricing dependency, request-bound test probe and negotiated Ops media type. Browser remained incomplete. |
| `dede4ed6fded4f35867deb6d08b0fcc6` | Compile FAIL, no fresh tests | Test variable shadowed package name; import corrected. |
| `a9675cc3734545949261b201c45d6603` | 8 tests, 7 PASS / 1 FAIL | Architecture 4/4, Business HTTP security, legacy Personal intake and actual Personal HTTPS passed; Business Browser failed an unrelated old Admin Inbox assertion. |
| `fe537b9b816846a983cb97d7069df36b` | 2 tests, 1 PASS / 1 FAIL | Extended Business HTTP/Organization race passed; old artificial admission-lock Browser fault remained unsuitable for the shipping scenario. Original Admin fault branch is preserved. |
| `1bdee7cb12a24555809a0b0d23f48d3a` | 3 tests, 2 PASS / 1 FAIL | Final Business security pipeline and legacy Personal intake passed. Business Browser read Parcel text before render; now waits for exact count two. |
| `9264900d7205438da959b1131c8627d8` | 2 Browser tests FAIL | Personal needed the same render wait; Business Review Workspace was unavailable. |
| `b4a4dc2dd04f4cd7a98d04148109e90e` | 1 Browser test FAIL | Redacted diagnostics show concurrent Admin reads returning 503, no SQL exception. Existing admission intentionally uses an exclusive try-lock. Shipping test now logs out each Admin role before entering the next; Backend security is unchanged. |
| `4221778d40bc40d29782196b94f8ae62` | 2 tests, 1 PASS / 1 FAIL | Personal actual HTTPS passed again. Business completed independent application approval and two-Parcel Quote/Booking/label download; loopback SMTP fixture accepted only two connections, omitting the third Ops invitation. Fixture capacity corrected to exact three for Business; no production transport change. |

`biaBusinessIntakeUsesCanonicalTimestampOwnershipAndAtomicPipeline` exercises
real PG18/HTTP with genuine request context: exact two-Parcel acceptance, V1
denial, incorrect owner/Organization/facility/quote/amount, constructed cargo,
Booking/scan chronology, stale version, duplicate/missing/foreign scans,
same-key replay, concurrency, Organization suspend race and revoked site denial.
Eleven malformed direct-DML transactions fail at commit; five late failure
points leave no partial custody/tracking/audit/receipt. Direct DML creates no
successful acceptance fixture. Finance remains UNPAID and snapshots immutable.

`nwsForwardUpgradePreservesV43RowsChecksumsAndPrivateGuardBoundary` proves actual
catalog owner/prosecdef/ACL preservation, exact current 48-migration stream and
populated data. `merchantR01SearchCommentsAndScope` covers the affected populated
V46 comment upgrade. Architecture checks pass without broadening dependency
rules. Frontend parser/transport focused batch: 11/11 PASS; typecheck/build and
targeted ESLint PASS. Existing build chunk-size advisory is unchanged.

The current consolidated HTTPS rerun is recorded separately after completion.
Until its Business and Personal gates pass, Slice1 remains PARTIAL. No full
Backend-suite, GitHub CI, local deployment or Production readiness is claimed.

## Final Slice1 acceptance — 2026-11-27

**V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST.** No outstanding mandatory
Slice1 gate. This does not accept full Spec012, physical transfer/delivery,
Payment/COD, local deployment or Production. Earlier PARTIAL/FAIL sections are
historical evidence, not the current disposition.

Final Business run `2e00170d4f234bbfb9897d3dd72262c8`: **1/1 PASS**, Maven exit0,
source unchanged. Source snapshot SHA-256:
`bb37d1807ae9cbd4ea34cb27e6efe3e57909d66e2b8bf2fc1460e06bf3a55c81`.
Final stage `BUSINESS_AT_STATION_VERIFIED`. Actual isolated HTTPS/PG18/SMTP/scanner:
Customer login → submitted application + real contact proof + screened evidence
→ independent reviewer/decider → atomic Organization activation → explicit
canonical operator shipping grants and Merchant pricing contract → Business UI
Quote/Booking two Parcels → actual PDF download → real Ops invitation/proof
activation/login → two physical-intake scan commands → one atomic acceptance
→ Merchant reads AT_STATION with UNPAID. The operator setup is explicit test
setup, not automatic shipping authorization upon Merchant approval.

The parent checks actual acceptance/custody/tracking/obligation and activation
records. PDFBox checks two pages/UNPAID/Local-Test marking; ZXing decodes both
actual Code128 images at300DPI to the exact persisted Parcel IDs. Three real
loopback SMTP messages were accepted by the test provider; no real email was sent.
Desktop and390px mobile screenshots were visually inspected: no overlap or
horizontal overflow, two accepted Parcels and unchanged UNPAID are visible.

Personal `nwsRealHttpsBrowserAndPostgres` passed in
`4221778d40bc40d29782196b94f8ae62` (batch1 PASS /1 Business-fixture FAIL retained).
Its source snapshot is
`895889ce4cb3f0cdd659f11198d2a6d66435501d9bb553fcf908c55be0a23107`.
Production fingerprints are identical to the final Business run; only the
separate Business SMTP test fixture was corrected afterward. No unnecessary
Personal or full-suite rerun was performed.

| Required gate | Final evidence |
|---|---|
| BIA-T-01 schema/ownership/ACL and historical preservation | Cold start, complete owner inventory, populated V43 upgrade and populated guarded V46 comment upgrade PASS in `acf8771fad1144a1884159546371c1fa`; V48/V49 and those test definitions match final source. Final recheck:46 historical migrations/HEAD/index unchanged, original approved hashes exact. |
| BIA-T-02 canonical Business pipeline, snapshots/UNPAID | Full real HTTP security pipeline PASS in `1bdee7cb12a24555809a0b0d23f48d3a`; actual Business Browser final PASS above. |
| BIA-T-03 authorization/ownership/time/finance denial | Same final HTTP pipeline and retained NWS authority/site tests; genuine request-bound probes, no constructed authority. |
| BIA-T-04 atomicity/negative DML/replay/races | Eleven invalid commit groups, five late-failure points, same-key race and Organization suspend/site revoke checks PASS in `1bdee7cb12a24555809a0b0d23f48d3a`. Current production and test definitions match that run. |
| BIA-T-05 both audiences HTTPS + compatibility/UI/architecture | Personal and Business runs above; architecture4/4 PASS with unchanged dependencies. Final frontend11/11 parser/transport units +2/2 UI regressions PASS; UI server rebuilt/typechecked current Customer artifact, targeted ESLint and reconciliation validator PASS. |

Final commands:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#biaRealHttpsCustomerApplicationToBusinessStation+nwsRealHttpsBrowserAndPostgres' -Offline
# After correcting only the Business loopback inbox capacity:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\ci\run-cskh-focused.ps1 -Tests 'IdentityGrantApplicationTests#biaRealHttpsCustomerApplicationToBusinessStation' -Offline
# app/frontend/web:
npx.cmd playwright test -c playwright-unit.config.ts business-intake-read.spec.ts operations-authority.spec.ts station-client.spec.ts
npx.cmd playwright test -c playwright-customer-ui.config.ts operations-ui.spec.ts business-intake-read-ui.spec.ts
npx.cmd eslint runtime-tests/admin-core.spec.ts runtime-tests/operations-entry.spec.ts src/customer/staff-client.ts src/customer/operations-client.ts src/customer/StationOperations.tsx unit-tests/business-intake-read.spec.ts --max-warnings 0
# repository:
python docs/reconciliation/v3/validate.py --implementation
git diff --check
```

Operational observation retained: existing Admin READ admission deliberately
uses an exclusive nonblocking population lock. Simultaneously active independent
Admin browser contexts can receive503. BIA performs reviewer/decider sessions
sequentially, preserving independent actors and canonical commands; it makes no
claim of resolving or accepting general multi-user Admin contention. That IAM
behavior was not changed as part of Station Intake.

Next: one bounded Spec012 proposal for shared PERSONAL/BUSINESS canonical Parcel
Inventory → Sorting → Manifest. No downstream engine or permission is approved
by this Slice1 acceptance. Menu2 Step B remains deferred.
