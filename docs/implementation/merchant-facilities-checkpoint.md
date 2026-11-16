# Merchant Facilities V1 implementation checkpoint

2026-11-16. Baseline develop `10a6a6c9925b4e34ca0b79caa53532369995f8f8`.
Branch `feature/argopost-merchant-facilities`, existing root directory only.
Owner accepted [WFP V1](merchant-facilities-contract.proposed.md); no additional
source approval outstanding. Nine earlier working files remain preserved with
verified SHA-256 copies in ignored `.demo/merchant-facilities/before-source`.

## Implemented and verified slices

WFP-03: private Organization/site-scoped directory and details, immutable unique
code, site type, representative/sender/return contact snapshots, seven-day hours,
versioned edits/archive and independent default binding. Legacy metadata remains
explicitly unknown. Referenced/default/assigned/unfinished sites cannot archive.

WFP-04: current verified same-Organization/site member candidates; versioned
PREPARATION/HANDOVER/RETURNS responsibilities. Responsibilities never grant RBAC.
Finite site scope, current revocation, ETag, idempotent actor-bound receipts and
mandatory audit/history apply in Backend and PostgreSQL, not only in UI.

WFP-05: explicit authorized site/default selection in manual and CSV/Excel entry;
sender copying requires confirmation; import preview keeps the site binding.
Accepted Management/report site filters retain the exact period/cohort and omit
money without Finance rights. Unbound Bookings never become invented sites or
silently change their confirmed facility/sender snapshot.

WFP-06: real confirmed BUSINESS Shipment/Parcel preparation at its active site,
all-Parcel checks before PACKED/READY_TO_HANDOVER, reasoned reopen invalidates
checks. Internal handover lists have one active binding per Shipment, bounded
selection, explicit PRESENT/MISSING/VERIFY, all-source readiness, reopen/cancel,
atomic audit/receipt and authorized private A4 PDF with every actual Parcel ID.
Every document retains LOCAL/TEST SIMULATION and NOT_CARRIER_ACCEPTANCE.
No Carrier acceptance/custody/tracking, PAID, COD, pickup, return or stock write.

## Source changes

Merchant API/domain/JDBC master, defaults, responsibilities; Orders API/JDBC
confirmed-Parcel work and private PDF; Workflow HTTP controllers and existing
admission/configuration wiring. Forward migrations V31/V32, existing V1-V30
unchanged. Additive Business directory/editor/picker/work/data parsers, source
report screen, manual/import selection and root navigation. Synthetic regression
fixtures, actual HTTPS Browser case and exact CI report/source inventories.

## Observed verification

- Focused PG18/domain/PDF: 14 PASS. Includes populated V30 -> V31 -> V32 upgrade,
  prior checksums/data/grants, default/archive race, concurrent handover binding,
  evidence fault rollback, revoked/wrong-site/foreign Parcel/Production denial.
- Full metadata HTTP test (sender/return/hours) PASS after adding regression data.
- H2/architecture `clean verify` in isolated target: 691 PASS,17 configured skips;
  sanitized report-policy PASS. Running application JAR is not overwritten.
- `npm.cmd test`:163 PASS; `npm.cmd run typecheck`:PASS.
- Customer production build PASS; existing bundle-size advisory remains unchanged.
- UI fixtures:20 PASS for directory/work/site activity/report regression;15 PASS
  for Business/manual/import/grid regression, then the one old menu-count
  assertion updated for the new root item and its focused test PASS.
- Mobile handover PNG visually inspected: contained layout, readable labels and
  source identifiers. Intercepted UI fixtures are not full-stack evidence.
- Actual HTTPS/PostgreSQL18/test-SMTP Browser:18 journeys PASS in focused run,
  including site/create/default/team -> two-Parcel Booking -> packing -> internal
  handover PDF -> reload. Test host explicitly registers new routes/controllers.
  Current final suite additionally verifies downloaded PDF against committed IDs.
- Initial whole-source PG18 run:1160 discovered,1129 PASS,14 stale migration-count
  assertions failed and17 skipped. The local invocation lacked the Demo
  PostgreSQL switch, causing12 mandatory PostgreSQL-only skips. This is not a
  whole-suite PASS.377 Identity/Business cases and18 real Browser journeys PASS.
  Corrective run uses the official isolated Foundation PostgreSQL credentials and
  `-Ddemo.test.database=postgres`, all Demo tests and all62 migration tests.
  Strict expected counts/history now include V31/V32 (114 tables); no assertion
  removed or loosened. Final corrective run:107 discovered,102 PASS,5 configured
  Foundation-only skips; all62 migrations and all12 PostgreSQL Foundation/Health
  cases PASS. The V24 privilege denial is checked before V31; current V32 also
  rejects a complete valid Facility row without authority/history/audit/receipt.
- Incremental regression: original raw suite reports retained, affected complete
  classes replaced by unedited corrective-run XML.739 compiled production class/
  resource files (including migrations) have identical SHA-256 across both runs.
  Exact report-policy over the combined reports:1160 discovered,1155 PASS,5
  allowed skips,0 failures/errors. Original lifecycle cleanup audit PASS; partial
  run stopped records verified; test-credential privacy and owned CI DB cleanup
  PASS. Provenance explicitly says INCREMENTAL_LOCAL_REGRESSION_NOT_A_SINGLE_CI_RUN.
  This is not a claim that the initial failed Maven invocation or hosted CI passed.
- Report-policy selftests:132 tests, OK after exact inventory updates
  (seven application tests, one migration test and six unit/PDF tests added).
  Windows PowerShell wrapped native stderr as NativeCommandError; the unittest
  final result itself is132 tests / OK, with no failed testcase.
- Source validator `python docs/reconciliation/v3/validate.py --implementation`:PASS
  (no errors); `git diff --check`:PASS. Temporary verification POMs removed.
- WFP-07 local gates DONE. Hosted CI has not been dispatched for this uncommitted
  source. Branch HEAD remains10a6a6c;53 source/docs/test working paths, no deletion.

## WFP-08 boundary and approved deployment plan

Read-only container check on2026-11-16: original ID
`965042a98c43043378b8757a13cb4364a063a289c59e35f43ee7c9eeac761a63`,
state exited (health reports starting). This run did not stop/restart it. Last
known live Flyway baseline is V30; recheck after an approved start, not a claim
of current live SQL readiness.

At the earlier source/test checkpoint, no V31/V32 had been applied to
`elogistics_dev`, no real-account grants or demo rows had been created. Existing container/certificates/keys and
all old Customer/Personal/Business data retained. No commit/push/PR/merge claim.
Prior PR47 CI is baseline evidence only, not current-source hosted CI evidence.

The following bounded plan was subsequently approved and executed:
1. Start the existing stopped container only if needed; verify its identity/health
   and actual Flyway baseline (last known V30), preserve prior data
   evidence; use official Flyway lifecycle for V31/V32, never repair/clean/reset.
2. Only the previously Owner-confirmed demo account in NamShop Demo
   (explicit UUID targets supplied privately in the conversation): explicit audited offline
   `merchant.configure_local_facility_grants`, ORGANIZATION scope, GRANT only
   facility.read, facility.manage, defaults.manage, shipment.prepare,
   shipment.handover.read and shipment.handover.manage. Retain existing rights.
   No new Organization/Membership, other-account grant or sample facility.
3. Restart only launcher-owned Backend/Frontend using existing secrets/cert;
   verify readiness, migration checksums, current scope and both workspaces.
4. Owner can create their own site and test the new screens. Pickup/Carrier
   acceptance/collection stay blocked; no default site or fake handover is seeded.

WFP-08 specifically excludes automatic live migration/grants from WFP-02 approval.
That separate decision was subsequently received and executed below; no further
implementation, tariff, coverage or UNPAID approval is outstanding.


## Executed Owner-approved WFP-08 deployment - 2026-11-16

Owner explicitly approved the bounded deployment described above in a subsequent
conversation decision. No further source approval or credentials were requested.

- Original PostgreSQL container ID verified before start and after deployment;
  same existing volume/mount identities and settings. Launcher found no remaining
  owned application processes. PostgreSQL18.6 healthy, actual baseline V30.
- Custom pg_dump backup retained with SHA-256 in an owner/SYSTEM-only ignored
  deployment directory. No restore drill or disaster-recovery PASS is claimed.
- Official `scripts/dev/local.cmd migrate`:PASS. V31/V32 applied; full preexisting
  table fingerprints over original columns unchanged, V1-V30 history/checksums/
  success preserved. Mount comparison uses destination identity, not Docker's
  nondeterministic array ordering. No reset, repair, volume removal or init.
- Audited `merchant.configure_local_facility_grants` adds only the six approved
  rights to the exact privately confirmed account in NamShop Demo. All existing
  grants retained; other memberships unchanged; target authority/version increments
  exactly once; operator audit verified. No new account or membership.
- Before application restart, all other preexisting tables still match their
  post-migration fingerprints. No facility, Shipment or internal work seeded.
- `scripts/dev/local.cmd start` and `status`:LOCAL_FULL_STACK_HTTP_READY.
  Existing TLS certificate/trust and secret-loading retained; no credential/key
  rotation. Frontend5173, Backend8443, original PostgreSQL5433.
- Strict TLS direct checks:Frontend root, exact facilities route, new UI module
  and Backend public media200; unauthenticated facilities and handover APIs401,
  Frontend API proxy401. No trust-all TLS or minted Owner token used.
- Browser and Windows computer-use initialization both failed with
  `failed to write kernel assets: The system cannot find the path specified`.
  No automated Owner-session UI claim. Owner was asked to reload NamShop Demo
  and confirm the directory/Add Site screen without sharing credentials.
- Pickup, Business Carrier Acceptance, Station/Hub administration, COD and
  collection remain outside this deployment. No new Git commit/push/PR/hosted CI.

WFP-08 deployment DONE. Owner subsequently confirmed that the NamShop Demo
directory and Add Site button display correctly without permission/loading errors.
This closes the requested manual screen check; it does not claim a new live
CRUD/handover transaction or automated Owner-session Browser run.
