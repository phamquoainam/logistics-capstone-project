# ADM-05 + ADM-12 ? Foundation readiness review

Current checkpoint: **NWS-V1 FOUNDATION SOURCE/TEST READY** (2026-11-25).

Owner-approved manifest bytes and 42 historical migration hashes preserved.
V44/V45 and the dedicated OPS/site-bound intake path passed isolated PG18 and
genuine HTTPS browser gates. NWS-ACL-02 is resolved; no demo/UAT deployment or
real grants. See [execution evidence](carrier-network-operational-staff-execution.md)
and [approval composition](carrier-network-operational-staff-execution-manifest.json).
This is not Full ADM-05/12 or Carrier Operations E2E readiness.

## Historical contract-review checkpoint (retained)

Date: 2026-11-25. **BLOCKED_CONTRACT ? NWS-V1 technical review prepared; new foundation NOT implemented.**

## Baseline and preservation

- Read entire AGENTS/North Star; first incomplete gate is Station/Staff contract, not another Dashboard implementation.
- Branch `develop`, HEAD `d5a0c240841bdb35d9ef6064cddba696a5b2fa62`; existing staged documentation and unstaged Menu1 WIP retained.
- Backend, workflow dependency and existing Station OpenAPI paths have no committed or working diff from tested PR51 head `6e4e1c3a7d38c810e215ea4de96db1f2ddadd35a`.
- Current stream: 42 migration files, highest V43, V33 absent by historical design. Proposed V44/V45 currently free; no migration created/applied.
- No Docker/runtime/DB access or mutation, real email, account/grant/operator provisioning, deployment, commit/push/merge in this task.

## Existing vs missing

| Area | Existing verified implementation | Missing / classification |
|---|---|---|
| Station source | V21 three SIM-DROP stations, active/drop-off flags and exact assignment interval/version; JdbcStationIntake current checks | General Station/Hub location/version/capability/topology and site-binding source: BLOCKED_CONTRACT |
| Staff | Canonical CSKH Staff purpose SUPPORT and separate verified session engine; V21 Customer-admitted Station assignment | Dedicated OPERATIONS purpose, OPS audience and site-bound admission: BLOCKED_CONTRACT. Existing CSKH does not imply Ops access. |
| Admission | StationIntakeHttp explicitly composes CustomerAdmission and Dispatch. JdbcStaffAdmission/JdbcIdentitySessionStore use support_staff_eligible | New genuine Ops context and protected adapter; no Customer-context fabrication, no portal grant union |
| Intake/custody | Same actor/station scans, exact all-Parcel set, state/version, live authority locking; acceptance/custody/audit/receipt and Orders events one transaction | Reuse implementation. New site-assignment evidence/FK branch needs exact approval; no duplicate acceptance engine |
| Service/price | PilotCoverage and real PilotQuoteApplication/BusinessQuotes already implemented | Ops cannot call Customer-context-only PilotCoverage: one proposed overload, no new tariff/routes |
| Admin source | AdminNetworkSource truthfully SOURCE_NOT_IMPLEMENTED; network READ grant exists | Two bounded directory GETs proposed. No Admin write, Staff-role editor or inherited Ops permission |
| Payment/business | Approved PERSONAL Local/Test UNPAID intake exception; Business Quote/Booking exists separately | No Business intake exception inferred, no PAID/Pickup/COD/Delivered |

## Reused test evidence, not a new test run

CI run [45548559303](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/45548559303) at exact tested PR51 head above.
Local sanitized artifact `.demo/pr51-ci-6e4e1c3/postgres/tests.xml`, SHA-256 `f655217ad8308e73c172da821dd24b43c253dc56e5f6dee4587332d3d700a107`.
Artifact parsed in this review; **13/13 relevant Station cases PASS**. Accepted post-merge CI45557771726 and source-tree equivalence remain prior evidence, not freshly rerun CI.

| Existing test | Artifact result |
|---|---|
| `stationCustodyAuditReceiptAndTrackingFaultsRollBackAcceptanceEntirely` | PASS |
| `stationForeignShipmentScansAndRevocationAfterLookupCannotBecomeCustody` | PASS |
| `stationAuthorizedAllParcelAcceptancePersistsCustodyTrackingAndUnpaidAtomically` | PASS |
| `stationIncompleteForeignDuplicateScansMismatchAndStaleVersionCannotAccept` | PASS |
| `stationSameKeyConcurrentAcceptanceReplaysAndDifferentBindingConflicts` | PASS |
| `stationDisabledProductionModesAndRevokedIdentityCannotPerformAcceptance` | PASS |
| `stationInactiveCapabilityInjectedAuthorityAndRuntimePrivilegeExpansionAreDenied` | PASS |
| `stationCustomerMissingRevokedWrongStationAndUnknownBarcodeAreDenied` | PASS |
| `stationTwoStaffRaceHasOneAcceptanceAndEveryParcelEventExactlyOnce` | PASS |
| `stationLabelsArePrivateStableMultiParcelPdfAndDraftsNeverGetLabels` | PASS |
| `stationV21OwnerProvisioningIsExplicitAuditedAndNotRuntimeCallable` | PASS |
| `stationV21DatabaseRejectsUnapprovedNetworkAndMutableAuthorityIdentity` | PASS |
| `stationV21UpgradePreservesV20SnapshotsAndSeedsNoStaffAuthority` | PASS |

The positive HTTP/PostgreSQL test checks one acceptance, two custody rows, one Shipment event, two Parcel events, unchanged immutable booking NOT_ACCEPTED snapshot and actual current AT_STATION, with UNPAID intact. Negative cases cover wrong/missing/revoked assignment, site disabled, identity revocation, mismatched/partial scans, replay/races and all-effect rollback.
These tests use verified Customer principals under ADR-0014. They are **not evidence that the proposed dedicated Ops Staff path already works**. No full suite or redundant integration rerun was needed because no runtime source/dependency changed.

## Exact proposal and acceptance boundary

- [Contract](carrier-network-operational-staff-contract.proposed.md).
- [Technical manifest](carrier-network-operational-staff-technical-manifest.proposed.json).
- Manifest SHA-256: `8adc4229ff00e728f322dd9341cb146a82e2dc16e1470dc077ec05eb55ac4ce5`.
- Stable NWS IDs enumerate purpose/audience/proof/receipt amendments, exact ports/HTTP/SQL, six Dispatch tables, legacy-compatible intake FK branch, offline ACL and narrow architecture exception.
- Initial implementation request is SOURCE/TEST on disposable isolated PostgreSQL18 only. It does not select a real Staff, site dataset, operator login/membership or authorize deployment.
- Existing identity.support_staff_registered is a generic registered-Staff exclusion predicate; keep it. SUPPORT-only eligibility and legacy Support grant authority must remain purpose-specific.
- Source metadata/full-spec classification is not changed to READY. Owner approval is required before new API/schema/authority implementation under AGENTS. NWS-T-01..06 are NOT_RUN_PROPOSED, not PASS.

## Next dependency

First approve/implement the bounded site + dedicated Ops admission/intake contract. After its acceptance: spec012 Parcel/site inventory and Sorting -> immutable Manifest set/sealing with custody holder, destination, concurrency and discrepancy contract; then spec013 sender/receiver-confirmed Transfer, then spec014 Dispatch. None may be synthesized from a topology link or Merchant handover list.

## Checks executed in this review

- PASS: manifest JSON parse, unique stable IDs, 15 distinct HTTP method/path pairs,
  four public ports, six proposed Dispatch tables, explicit NOT_RUN status on all
  new implementation tests, and manifest SHA matching this report.
- PASS: preserved AGENTS/North Star byte hashes; no Backend/migration/OpenAPI
  source delta from the verified revision; 13/13 relevant artifact results parsed.
- PASS: `git diff --check` for unstaged work (exit0).
- PASS: `python docs/reconciliation/v3/validate.py --implementation` (exit0),
  errors empty, semantic ownership CONSISTENT, unresolved ownership empty. Ten
  pre-existing shipper-mobile documentation links remain historical warnings;
  they were not introduced or changed here.
- New unit/PG/HTTP/HTTPS/UI implementation tests: **NOT RUN**, because new
  API/schema/authority is still proposed and no executable source was changed.
  Docker, real credentials and demo/UAT databases were not needed for this review.
