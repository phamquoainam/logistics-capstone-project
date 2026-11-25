# ArgoPost Admin Dashboard — source/test execution

Owner-authorized ADM-02–08. Execution 2026-11-21/22, Asia/Ho_Chi_Minh.
Checkpoint: **ADMIN_DASHBOARD_SOURCE_TEST_READY** (bounded approved source/test).
Latest continuation: **ADMIN_ONBOARDING_SOURCE_TEST_READY**; both ACL amendments
approved and tested. See [current AON execution](admin-onboarding-execution.md).
Subsequent separately approved real deployment reached ADMIN_DASHBOARD_LOCAL_UAT_READY;
see [live execution](admin-uat-deployment-execution.md). Earlier preparation and
ACL-blocked text below is historical and superseded by those execution reports.
Branch `feature/argopost-admin-dashboard`; unchanged HEAD/base
`d7329a84822f88a1c01e70a0e23b065123860410`. No commit/push/merge/deployment.

## Authority and preservation

[Owner approval](argopost-admin-dashboard-owner-approval.md) authorizes the exact
[contract](admin-dashboard-contract.proposed.md), SHA-256
`69d1ac81606199a2db644d8038553e19fd78bfddb2608e92713b2fa1396e2289`.
The original approved bytes, including historical PROPOSED status, are preserved;
this approval supersedes the earlier ADM-01 pending-approval boundary.

Preflight archive `.demo/admin-preflight-3b07944984b247f5a034d2731202fa33/wip.zip`
was verified before implementation. It preserves 11 original WIP files and a
fingerprint inventory of all 37 historical migrations. No migration was added or
changed; V33 remains absent. Existing CSKH manifest is unchanged:
`c6a784d48e827330fee46e09175c27939238c259680c2f54487ab1ef1ec3409c`.

No demo database, account, grant, realm, binding, feature flag, credentials,
certificate, runtime process or global environment was changed. Synthetic ACTIVE
SYSTEM principals and grants exist only inside owned isolated PG18 test fixtures;
they are not a deployable provisioning mechanism.

## Task disposition

- [x] ADM-01 DONE: existing SYSTEM login/CSRF/refresh/current-session/logout UI.
- [x] ADM-02 DONE: six persisted read capabilities and dedicated current admission.
- [x] ADM-03 DONE: exactly six approved GET operations with granular Backend checks.
- [x] ADM-04 DONE: bounded DTO/query, exact values and authenticated snapshot selector.
- [x] ADM-05 DONE: owner read ports composed in one read-only repeatable-read callback.
- [x] ADM-06 DONE: canonical Shipment/Parcel/UNPAID facts; truthful unavailable sources.
- [x] ADM-07 DONE: responsive Dashboard, filters, charts and same-cohort drill-through.
- [x] ADM-08 DONE: focused PostgreSQL/HTTP/Browser/security/regression gates,
  Backend H2, frontend gates, finite report inventory and repository validator.

This bounded delivery does not mark Spec027 BC-031 or Spec028 BC-035 as fully
accepted. Advanced business sources and real Admin onboarding remain dependencies.
[ADR-0016](../architecture/decisions/ADR-0016-bounded-carrier-admin-reads.md)
records the accepted bounded security and transaction design.

## Source inventory and implemented behavior

- Identity: `AdminReadContext`, `AdminAdmission`, `AdminFailure`,
  `AdminSessionPolicy`, `JdbcAdminAdmission`; existing session-store/configuration
  composition and request-ID access. Six approved current grants, ENTRY required,
  verified ACTIVE dedicated SYSTEM account, same registered ACTIVE realm, no
  Customer identifier/SUPPORT registration/Initial Root/mixed/unknown grants.
  Existing IAM assignability, bootstrap and last-admin rules remain unchanged.
- The feature defaults disabled. LOCAL/TEST flag plus the existing physical DB
  environment predicate are required; prod/production profiles explicitly deny.
  No new table, migration, privilege, grant operator or authority engine.
- Admission acquires the existing Identity population lock before opening MVCC
  read snapshot. Busy authority mutation fails closed. Context is callback/thread
  confined; sessions/grants are current, with no read heartbeat or business write.
  Enabled composition needs two available pool connections; connection/statement
  timeout denies availability rather than returning partial data.
- Orders owns confirmed PERSONAL/BUSINESS Shipment/Parcel reads; Finance owns
  exact integer VND obligations. Immutable acceptance events are tested with EXISTS,
  not multiplying joins. One distinct Shipment cohort drives counts, fees and lists.
  Bounded 100,000-row cohort; larger capture fails 503 rather than undercounting.
- Workflow exposes only context, dashboard, shipments, merchant-applications,
  network and platform GET operations. Unknown/duplicate queries reject. Finite
  errors; no-store/no-referrer; no sender/recipient PII in list DTOs.
- Notifications owns aggregation, Vietnam calendar DAY/WEEK/MONTH buckets and
  15-minute stateless HMAC snapshots bound to actor/realm/current grant version,
  normalized query, cutoff and source digest. Tamper, expiry, cross-actor replay,
  partial permission revocation and unreproducible changed cohorts deny. Offset
  pages use bookedAt DESC/id DESC. Chart/KPI clicks retain the captured cohort.
- Frontend `src/admin`: separate ArgoPost navy/orange shell and primary logo,
  compact filters, six permitted KPI/sparkline cards, Booking bars, supported-state
  donut, confirmed-fee bars, bounded list and explicit loading/empty/error/409 UX.
  Permission-dependent sections are omitted. Technical check is technical-role
  only, scoped to the responding Admin API and DB, not whole-platform health.
  Access remains memory-only, with existing secure cookies/CSRF, periodic current
  checks and cross-tab logout invalidation. No automatic retry of ambiguous auth POSTs.
- Report-policy changes only add finite test identities. Existing collector/privacy
  function ASTs and pinned Testcontainers ownership protocol stay unchanged.

Unavailable: submitted Merchant review, official Station/Hub/Partner counts,
advanced Carrier lifecycle, SLA/escalation/decision queues, collection/paid/overdue,
COD/reconciliation and unsourced queue/job/integration telemetry. Merchant Drafts
are not applications, SIM points are not official network, and UNPAID is not revenue.
Merchant/network list endpoints return 422 SOURCE_UNAVAILABLE after authorization.

## Observed verification

| Check / command | Actual result |
|---|---|
| Backend `mvnw.cmd ... test` (offline cached Maven, unchanged H2 profile) | Initial 747 cases: 14 Windows Temp ACL errors, 0 assertion failures, 17 profile skips. Same-source rerun with filesystem access: **730 PASS / 17 SKIP / 0 FAIL / 0 ERROR**, BUILD SUCCESS. No test/security change to resolve ACL. |
| Existing H2 report collector, real fresh 72 XML files | **PASS**, discovered747/executed730/skipped17; private `admin-final-evidence-e007259a33254e4ead7dc2dfcbe1fcfd/h2-safe`. |
| Architecture | **23 PASS**, also included in successful complete H2 run; no collaborator allowlist changes. |
| New Admin domain/configuration units | **8 PASS** in successful H2 run (5 arithmetic/query/snapshot + 3 default-off/Production config). |
| `run-cskh-focused.ps1 -Tests IdentityGrantApplicationTests#adminDashboard*+sessionRealmDeactivationRejectsRefreshWithoutMutation+customerLoginUsesIndependentVerifiedBindingWithoutSystemGrant -Offline` | **12/12 PASS**, PG18 actual; Maven0, sourceUnchanged=true. |
| Additional affected HTTP regression: `supportTicketStaffRevocationAssignmentAndHttpBoundaryDeny+customerHttpSystemTokenAndRevokedAccountCannotBecomeCustomerAuthority` in the same owned suite | **2/2 PASS**, PG18 actual; Maven0, sourceUnchanged=true. |
| Real HTTPS Browser/Backend/PG18 (inside Admin focused test) | **4/4 PASS**; real authentication, exact metrics, drill/filter/back, secure refresh/mobile and logout. No API response interception. |
| `npm.cmd test -- admin-session.spec.ts admin-dashboard.spec.ts customer-client.spec.ts staff-authority.spec.ts` | **40 PASS**: 17 Admin + 19 Customer + 4 Staff. |
| `npm.cmd run test:customer-ui -- admin-login.spec.ts admin-dashboard.spec.ts customer-ui.spec.ts` | **47 PASS**: 11 Admin + 36 Customer/Public; intercepted fixture UI tests, not real-data acceptance. |
| Frontend typecheck/lint/build:customer/build | PASS; Customer build also rebuilt by final UI gate. Final lint and demo build exit0. Existing >500kB bundle advisory retained. |
| Report-policy unit checks | Initial134-method run: 127 methods passed; 7 methods had8 inventory assertions fail after adding Admin cases. All7 affected methods plus2 new Admin negative-inventory methods rerun: **9/9 PASS**. Total current136-method coverage is across these runs, not a claimed single clean full run. |
| Implementation validator | PASS: zero errors, ownership CONSISTENT; 10 preexisting broken links retained. |
| `git diff --check` | PASS after preserving LF in the report inventory; no broad line-ending rewrite. |

PG12 evidence: `.demo/cskh-focused-b3dc308279ad4a26aa85e3c549891245`, source snapshot
`f4d3c3dda3b8c6e2b733293fae32d2452812cafa9ce3a93b9764e044789f452c`.
PG2 evidence: `.demo/cskh-focused-0c1fc3aa5dad449c815c9c0401a462a1`, source snapshot
`087482850a0b4f3ba38aa4bb4c09f40f32b488e4e4748fdff75fde63c667d374`.
Both snapshots remained unchanged throughout each run. The later snapshot includes
CI finite-inventory updates; Admin application behavior was unchanged. Subsequent
changes are evidence/docs and finite collector-test inventories only.

Existing credential-ledger privacy audits passed for both focused runs, scanning
private Maven/XML diagnostics without printing synthetic credential material.
Only synthetic visual PNGs were copied to `.demo/admin-visual-review/`; desktop
1280px and mobile390px were inspected: readable, correct values, no horizontal
page overflow. No real customer PII or credentials appear in these images.

The focused run uses the **existing** IdentityGrantApplicationTests container/
database lifecycle, not a new suite alias. Its actual cleanup verification ran.
The complete three-suite Task C cleanup audit/full PG regression/GitHub CI were
**not run and are not claimed PASS** by this bounded checkpoint.

## Security and regression evidence

Ten Admin PG cases cover six HTTP paths, SYSTEM/IAM separation, no PII,
PERSONAL+BUSINESS ownership facts and exact money, per-permission omission/denial,
missing ENTRY, disabled/Production gates, Customer/Staff/mixed realm denial,
revocation, Origin/HTTPS/query-token rejection, transaction read-only enforcement,
context lifetime, unknown/duplicate filters, tamper/expiry/replay/drift and the
concurrent authority-write/read gap. Snapshot source/count mismatches fail closed.

Initial runs retained as evidence: standalone test class correctly rejected by
existing lifecycle allowlist (no test DB claim); first owned PG run5/6 passed,
with one overbroad privacy assertion falsely matching NOT_REVENUE reason text,
corrected to inspect forbidden metric keys while retaining PII/payment negatives.
No business rule/security gate was relaxed. One earlier intercepted UI teardown
race was fixed by waiting for networkidle; final47/47 passed without retry.
Initial Maven cache argument failure was not a logical TDD RED; no false RED claim.

The report-policy corrections update H2/PG inventory totals722→730/1230→1248,
Identity owned-suite count408→418, source declaration order and the legacy
administration-prefix partition to include the ten separately required Admin
Dashboard methods. All original test identities and collector/privacy functions
remain intact. The two new inventory tests additionally require exact source
method identities and reject missing/skipped/failed/error/renamed Admin cases.
An intermediate focused command named a nonexistent Python test class; corrected
command9/9 passed. This was a test-selection error, not a product/source failure.

Final preservation: archive integrity and all11 original WIP hashes PASS;
all37 migration hashes unchanged; approved Admin and existing CSKH hashes match;
finite source secret-pattern scan has no findings; git diff whitespace check PASS.
No unresolved blocking security finding was identified within the approved scope.

## Remaining boundary

Real Admin onboarding, exact account/grants, SYSTEM activation, runtime pool sizing
and Local/Test deployment/UAT need a separate Owner checkpoint. They are not
implemented by fixtures or permission self-grant. No existing demo mutation or
publication was performed. Missing advanced business sources remain unavailable,
not invented metrics, approvals or financial activity.

## Historical 2026-11-22 preparation - superseded by AON execution

Historical preparation checkpoint: **ADMIN_ONBOARDING_AUTHORITY_APPROVAL_REQUIRED**.
Owner subsequently approved Option B; current source/test execution and the one
new ACL caller dependency are recorded in [AON execution](admin-onboarding-execution.md).
Prior ADMIN_DASHBOARD_SOURCE_TEST_READY remains accepted; this is not live UAT.
The latest Owner directive permits preparation and later isolated deployment only
after canonical operator/prerequisites are valid. It explicitly requires technical
approval for new provisioning authority/API/schema; it does not approve the new
proposal by implication.

- [x] Preserve/verify46 starting WIP paths and37 migration fingerprints at
  `.demo/admin-local-readiness-preflight-e8cfbd636cc94517b4504894a085e8b2`.
- [x] Inspect existing Identity/Realm/invitation/session/operator engines and ADRs.
- [x] Produce one exact consolidated technical delta, review, coverage and runbook.
- [ ] New authority approval and implementation: BLOCKED; no privileged source/SQL.
- [ ] Real isolated target/Root prerequisite, provisioning/verification and grants.
- [ ] Isolated deployment, live authentication/UAT and rollback rehearsal.

Findings: existing ordinary provisioning permits only the exact management pair;
invitation delivery/activation also require it, so the accepted read codes cannot
be issued by that path. STAFF/SUPPORT provisioning is not SYSTEM onboarding.
Initial V4 activation additionally needs real bootstrap-account verification,
but ordinary invitation currently requires an ACTIVE realm. No fixture/manual
verification or direct DML is an acceptable solution.

Prepared [core/optional-Realm contract](admin-onboarding-contract.proposed.md),
[exact manifest](admin-onboarding-technical-manifest.proposed.json),
[single technical review](admin-onboarding-technical-review.md),
[isolated deployment/rollback runbook](admin-local-readiness-runbook.md) and
[functional coverage](admin-local-functional-coverage.csv).
All new schema/API/operator signatures remain PROPOSED. V39 core is independent
of optional V40: the latter requires affirmative authorization for a NEW isolated
Initial Root only, or an already legitimate ACTIVE isolated realm must be provided.
Existing Root, shared demo and management grants are never auto-modified.

Read-only environment observation: Docker Engine29.6.2 available; existing exact
container `elogistics-d1-postgres18` was exited (stale health=starting), image pinned
PG18.6, volume `elogistics_d1_pg18_dev_data`. SQL discovery could not execute because
the container was stopped. No start/restart/recreate, SQL mutation, migration,
binding, grant, identity, feature, certificate, credential or global-config change.
Current live Flyway/realm state is NOT REVERIFIED; no claim of Local HEALTHY/READY.

No previous suites were rerun: only documentation/manifest validation is needed
for this continuation. New AON-T-01-10, SMTP inbox delivery, real onboarding,
authentication/UAT and rollback remain NOT RUN/BLOCKED_AUTHORITY. No full-suite,
CI or ADMIN_DASHBOARD_LOCAL_UAT_READY claim. No commit/push/merge.

Proposal/document checks: PASS,78 unique stable IDs,20 exact SQL signatures,
19 coverage rows with existing evidence links; every new authority object remains
PROPOSED. Manifest SHA-256
`025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.
Application/CI source unchanged from this continuation's46-file preservation
checkpoint; all37 historical migrations and original approved contract unchanged.
No new onboarding/security integration test is labeled PASS. Private validation
metadata is under the same readiness-preflight directory. Whitespace check PASS.
