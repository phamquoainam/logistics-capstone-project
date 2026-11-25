# AON V39/V40 source/test execution

2026-11-22. Branch `feature/argopost-admin-dashboard`, HEAD
`d7329a84822f88a1c01e70a0e23b065123860410`.
Current checkpoint: **ADMIN_ONBOARDING_SOURCE_TEST_READY**.
Scope of this report: Owner-approved Option B SOURCE/TEST. Subsequent separately
approved real isolated deployment/UAT is recorded in
[deployment execution](admin-uat-deployment-execution.md).

## Current completion record (supersedes historical continuation below)

### PR integration regression correction — 2026-11-23

PR51 first integration run45106846643 at HEAD63e7365 / merge revision80014fa:
Architecture and H2 PASS; full PostgreSQL FAIL (1263 executed,1246 passed,
17 failures,0 errors,5 expected skips). This result is retained, not replaced by
the earlier focused source/test or live UAT acceptance.

Sanitized reports identified stale Foundation migration inventories and repeat
upgrade assertions. Isolated reproduction `.demo/cskh-focused-0619affe50614f85a710140238387676`
ran three affected cases, all failed: expected37 vs actual39 migrations;
expected140 vs actual143 owner tables; repeat migrate expected2 vs actual0.
The latest stream correctly has V39/V40 and three Admin tables. Twelve repeat
migration assertions had incorrectly advanced zero to two while first-upgrade
counts were updated. This is a regression-test defect, not a schema/runtime defect.

Correction is limited to three test files: PostgresTestcontainersTests,
PostgresFoundationTests and IdentityMigrationTests. Exact version lists now
include39/40 (still no33), exact table inventory includes the three Admin tables,
and repeat migrate is strictly zero. Checksum/data preservation, owner equality,
ACL/security assertions and all historical SQL remain unchanged. Focused rerun
`.demo/cskh-focused-8a95e65cc76c49228d10ef806d13c9ea` PASS15/15, Maven0,
no skips/errors/failures, source unchanged during execution. The two external
Foundation cases and full regression still require new exact-revision CI before
merge; no old-head PASS is substituted.

The exact parent manifest remains byte-identical. Owner separately approved both
AON-ACL-01 and AON-ACL-02, source/test only. V39/V40, offline adapters, protected
SMTP, activation UI and Dashboard integration are implemented. At this source/test
checkpoint no real target was deployed; later authorized live results are linked
above. The older prototype/blocked paragraphs below are
retained history, not the current implementation or ACL status.

Latest owned PostgreSQL18 consolidated evidence:
`.demo/cskh-focused-b576405bc96541ecb2463b6a374a5006`, Maven0,
41/41 PASS, no skipped cases, sourceUnchanged=true, source snapshot
`dd1947ff5fe84ccdc2557ac8daddfada5b621f37b4569fdcea94790fec8aebcd`.
This includes five actual HTTPS Browser cases on the current source-built
Spring/React/PostgreSQL fixture, not a mock browser-only backend.

Final DTO review then found unpaired UTF16 surrogates could be replaced by JDBC
before the SQL digest saw them. TDD evidence
`.demo/cskh-focused-292049ac80224b02a6f23120be4d7b32`: 1 FAIL before correction.
The existing offline command records now validate exact UTF8, no NUL and Unicode
codepoint lengths (display1..160/reason10..500), without trimming/normalization.
No API/schema/authority expansion. Affected regression
`.demo/cskh-focused-78fddfd120634bb2b305ac5a87a5aecb`: 5/5 PASS,
sourceUnchanged=true, final source snapshot
`b3270ea526e939cfedfa234433de8ef647f56084a182ba46191d22dde752077b`.
It covers boundary Unicode, Admin and Root provisioning, terminal revoke,
resend/cancel and concurrent issue/acceptance. Unaffected gates from the41-case
run are retained; their source has not changed.

### AON-T-01–10 coverage

| Gate | Observed source/test coverage | Result |
|---|---|---|
| AON-T-01 | V38→V39→V40 upgrade/validate/no-op; historical checksums; role/catalog ACL; raw control-function denial; direct-DML/SET ROLE denial; V40 late-DDL rollback/retry | PASS |
| AON-T-02 | Transaction reservation/seal rollback; Customer/enrollment collisions; generated fresh principal; exact READ set; Unicode input preservation | PASS |
| AON-T-03 | Protected outbox delivered to actual loopback SMTP inbox; genuine HMAC proof + password engine; HTTPS activation and real SYSTEM login | PASS, isolated fixtures only |
| AON-T-04 | Same-key committed replay, mismatch, current revoked operator and recreated-role OID denial; concurrent issue/acceptance; no authority resurrection | PASS |
| AON-T-05 | Expiry, resend old-proof invalidation, committed3/account/hour and20/operator/hour quotas; cancellation; worker lease recovery/retry/current eligibility | PASS |
| AON-T-06 | Current marker/grants/session revocation; Root/Customer/Staff isolation; six APIs; exact fees/counts and authenticated cohort snapshot/tamper/drift | PASS |
| AON-T-07 | TLS/Origin/SYSTEM CSRF/strict body/query denial; proof memory-only and early fragment scrub; desktop/mobile activation/login/refresh/logout | PASS |
| AON-T-08 | Actual two-connection Hikari pool exhaustion/recovery; lock-before-snapshot concurrency; unavailable DB; owned HTTPS process readiness/stop | PASS |
| AON-T-09 | Default-off gates; terminal Admin revoke/environment/realm disable; sessions revoked; replay cannot reactivate; no down migration/repair | PASS, owned fixtures |
| AON-T-10 | Actual protected Root invitation and cryptographic acceptance before unchanged V4 activation; Initial Root binding; cancelled quarantine; terminal disable | PASS, owned fixtures |

The manifest's original PROPOSED/NOT_RUN fields are immutable approval evidence;
this execution report records the current observed results. Real controlled-mailbox
delivery, real operator membership and deployment/UAT are NOT RUN and unauthorized
in this checkpoint. No `ADMIN_DASHBOARD_LOCAL_UAT_READY` claim.

### Other final gates

- Frontend `npm.cmd run typecheck`, `lint`, `build`, `build:customer`: PASS.
- Admin unit tests21/21 and UI tests13/13: PASS; logs
  `.demo/aon-close-unit.log`, `.demo/aon-close-ui.log` and scoped build logs.
- Report-policy self-tests136/136 PASS (`.demo/aon-report-policy-consolidated.log`);
  prerequisite generators4/4 PASS. Finite expected inventories expanded only for
  actual new test methods; collector/privacy functions and outer Docker hooks unchanged.
- Identity validator530/530 PASS, classification CONTRACT_ONLY_NOT_RUNTIME.
- Repository implementation validator PASS; ownership CONSISTENT; ten preexisting
  broken links retained, no new validator exemption.
- Backend `mvnw.cmd ... clean verify` PASS; fresh official report collector:
  discovered748, executed731, passed731, failed0, errors0, expected skips17.
  Fresh architecture suites23/23 PASS. Evidence:
  `.demo/aon-close-h2-28eea679f0c34d97a159ffa120c11cbb/fresh-reports/summary.json`.
  Packaged JAR SHA256
  `7415fb2995073b3ee52c060c2e69a45fa6dd17c58b016c19de2137193c5bed80`;
  built only, never deployed. A first Maven verify exited0 and
  executed731 passing tests/17 expected skips, but collector rejected five stale
  PostgreSQL XML files. That collection is FAIL, retained privately. Fresh
  `clean verify` passed the official gate; no assertion/filter bypass.
- Generated test-credential privacy audit PASS on both final PG evidence sets.
  Read-only owned-resource absence verification is recorded for the actual test
  suites only; no claim that a full three-suite PostgreSQL CI job was executed.
- `git diff --check` PASS. Existing customer bundle >500kB advisory remains;
  it is not hidden or treated as a new functional blocker.

No unresolved source/test security finding remains in the approved scope.
This is not a full PostgreSQL1263-test run or GitHub CI PASS. No workflow was
dispatched and no old revision's CI is substituted for the uncommitted source.

### Reproducible commands

- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/ci/run-cskh-focused.ps1 -Tests '<approved affected methods>' -Offline`;
  exact41+5 method names and statuses are in the two protected summary.json files.
- Backend: `mvnw.cmd -o -B -ntp --settings ../../scripts/ci/maven-settings.xml -Dmaven.repo.local=D:/TLCN/e-logistic/.demo/maven-repository clean verify`;
  `python ../../scripts/ci/verify-reports.py --source target/surefire-reports --output <private reports> --mode h2 --maven-exit 0`.
- Frontend: `npm.cmd run typecheck`, `npm.cmd run lint`, `npm.cmd run build`,
  `npm.cmd run build:customer`; `npm.cmd test -- unit-tests/admin-dashboard.spec.ts unit-tests/admin-session.spec.ts unit-tests/admin-invitation.spec.ts`;
  `npm.cmd run test:customer-ui -- ui-unit-tests/admin-activation.spec.ts ui-unit-tests/admin-dashboard.spec.ts ui-unit-tests/admin-login.spec.ts`.
- `python -m unittest discover -s scripts/ci -p test_verify_reports.py`;
  prerequisite tests via `test_admin_role_prerequisites.py` and `test_staff_role_prerequisites.py`.
- `node contracts/identity-access/validate.mjs`; implementation validator
  `docs/reconciliation/v3/validate.py::run(implementation=True)`.

### Source scope and security

- V39 adds exactly three approved Identity tables, finite SQL operators, receipt/
  audit/proof/outbox consistency guards and physical database/realm binding.
- V40 adds only approved first-bootstrap wrappers/receipt cases, reusing unchanged
  V4 invariants; no existing Root is converted or replaced.
- Dedicated offline Java adapters are not runtime beans. Runtime has no operator
  DataSource, writer membership or public provisioning endpoint. Six READ codes
  remain canonical account_grant rows, mandatory ADMIN_DASHBOARD_READ only by
  explicit selection; no implicit IAM, Carrier or Finance write.
- SQL provenance guards retain SECURITY INVOKER/current_user. Rejected broad
  definer/xmin rewrite was never applied. ACL01 grants only the boolean predicate
  to Staff writer; ACL02 removes raw pg_control_system PUBLIC execution only in
  privileged owned-test prerequisites, retaining approved explicit callers.
- Existing Identity proof/credential/session/CSRF engines and JavaMailSender are
  reused. New activation POST validates exact origin/body/CSRF, bounded inputs and
  rate limits; acceptance creates no session. Delivery never activates an account.
- UI scrubs proof fragments before session initialization, stores no proof in
  local/session storage and redirects verified Admin to normal login.
- Confirmed fees and UNPAID are not paid revenue. Missing Merchant review, official
  network, advanced Carrier lifecycle/SLA/settlement/telemetry sources stay UNAVAILABLE.

### Preservation and publication boundary

All51 original WIP entries verified against the preserved ZIP, CRC PASS. ZIP SHA
`01aceab2ee1927b3a429e12c2c573cd1ff66eeaf9e1b76f273d8d626c83c8626`.
All37 historical migrations unchanged; V33 remains absent. New SQL SHA256:

- V39: `48510ec55434143218c222782d9e7e89ccc553585ce12ab638c733e52ec95fbd`.
- V40: `7c663b1df790bc26eaa020c366c4784c89e76a6979d10377bd8043d4422e486c`.

Branch/HEAD unchanged, no staged changes;80 current modified/untracked file paths.
No Git commit/push/merge, demo/shared/Production SQL or real provisioning/grants,
SYSTEM activation, deployment, credentials/certificates/global config changes.
Raw XML/logs, fixture keys, proofs and private runtime files remain ignored and
ACL-protected; only finite outcomes/checksums are reported here.

The [isolated runbook](admin-local-readiness-runbook.md) provides prerequisite,
operator composition, selected READ set, controlled-mailbox acceptance, process
and rollback checklists. Exact real target/operator/READ-set authorization remains
a separate deployment checkpoint, not a request to approve V39/V40 again.

## Historical source/test continuation (superseded above)

Latest Owner directive approves AON-ACL-01 exactly and reaffirms full Option B
source/test execution. The one predicate EXECUTE grant is now applied before
ownership transfer; no rejected definer/provenance rewrite was used.
Evidence `.demo/cskh-focused-7264967df73c4dfa833c9fbf8af6f128`: focused PG18
Staff writer regression 1/1 PASS, sourceUnchanged=true. Catalog assertions verify
writer ownership, no Staff membership in Admin writer, no private marker SELECT,
no eligibility/environment EXECUTE and no PUBLIC predicate EXECUTE. A real Staff
writer UPDATE of an unmarked account affects exactly one row.

## Approval gate and preservation

Owner selected Option B and authorized V39/V40 SOURCE/TEST ONLY. Full contract and
745-line manifest read; exact SHA-256 verified:
`025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.
Original approved artifact bytes remain unchanged. Latest Owner restrictions
override the earlier conditional deployment wording: no real operator membership,
Root/Admin provisioning, SYSTEM activation, deployment, Git publication or shared
database mutation. Only synthetic owned PG18 fixtures may exercise those paths.

All51 starting WIP files archived/hash-verified with37 migration fingerprints in
`.demo/aon-source-test-preflight-51a8745e5dc1465584904d339ab1cd52`.
V39/V40 were unoccupied at preflight. No V1-V38 edit or V33 placeholder.

## Sequence and current task state

- [x] Read AGENTS/DoR, Owner exact artifacts, related ADRs and Identity source.
- [x] Verify approved checksum, branch/WIP, free migration numbers and preservation.
- [x] Write prerequisite-generator tests before source; RED missing module observed.
- [x] Implement exact three new NOLOGIN role prerequisites; focused Python tests4/4 PASS.
- [x] V39 guards/operator/receipts and PG18 upgrade/ACL verification.
- [x] V40 genuine first-Root verification and controlled realm wrappers.
- [x] Offline Java operator/activation/protected SMTP integration and frontend.
- [x] AON-T-01-10, affected security/HTTP/Browser/regression and final validators.
- [x] ADMIN_ONBOARDING_SOURCE_TEST_READY, bounded to source/test evidence above.

Plan uses existing privileged bootstrap hooks: Java StaffRoleTestBootstrap
delegates to new AdminRoleTestBootstrap; Python staff_role_prerequisites delegates
to the exact new generator. The two outer hooks, pinned Testcontainers ownership,
container/volume cleanup, original migration functions and report collectors stay
unchanged. These helpers do not connect to developer DB or grant operator login
membership. Flyway/runtime keep NOCREATEROLE; no CREATE ROLE in migration.

New application SQL is Identity-owned, plus the approved Merchant-owned exclusion
trigger. There is no permission from Root to Dashboard, Customer/Staff conversion,
new auth/provider/session engine or write capability outside the manifest.

## Observed tests, not forecasts

Continuation evidence (each runner reports sourceUnchanged=true):

- `.demo/cskh-focused-c5f8d92856fc457d815f33c0704b6068`: V39 physical binding,
  unsealed rollback, request replay/current operator, exact READ set, terminal
  cancel and Staff ACL: 3/3 PASS.
- `.demo/cskh-focused-e5ad82e395c840a990176d2699ce5640`: V39 canonical HMAC/AES-GCM
  activation, wrong proof, exact replay, version change and terminal revoke,
  plus reservation regression: 2/2 PASS.
- `.demo/cskh-focused-2eb154c799c74f05a96d122bff7dbb04`: V39/V40 upgrade/validate,
  historical checksum preservation and private ACL; actual loopback SMTP receipt
  for Initial Root, real cryptographic acceptance, V4 activation and terminal
  disable/replay; V39 activation regression: 3/3 PASS.
- `.demo/cskh-focused-6657dcc2780041e9a9fb7622aa8cf16e`: canonical-fixture Admin
  HTTP CSRF/proof/login/revocation, six APIs, partial permission/session revocation
  and pre-lock concurrency: 4/4 PASS.
- Frontend typecheck, lint and customer build PASS after activation UI changes.
  Existing large-bundle advisory remains; no warning suppression was added.

These are owned isolated fixtures, including a real loopback SMTP server, not
Owner mailbox delivery or deployed Local UAT. Mandatory remaining tests and
source/runtime integration still prevent a readiness verdict.

| Evidence | Result |
|---|---|
| `.demo/aon-prerequisite-red.log` | RED: new prerequisite module absent before implementation |
| `.demo/aon-prerequisite-green.log` | 3/3 new generator tests PASS |
| `.demo/aon-prerequisite-integration.log` | 4/4 combined Staff/Admin generator tests PASS |
| `.demo/cskh-focused-0ba934f4856c43d6b703eb86983ad756` | RED: one owned PG18 case, Flyway reports missing target40; no fake migration PASS; sourceUnchanged=true |

V39 is an incomplete schema/guard prototype, not a finished migration/integration
diff. Its approved operator procedures are not implemented yet. V40 is not created.
Do not infer readiness from a successful prototype DDL or a missing-target RED.

## Historical blocker AON-ACL-01 (resolved by exact Owner amendment)

The regression exposed two separate issues; neither is hidden as PASS:

1. Routine implementation defect: a shared trigger used CASE to access differently
   shaped NEW records. PG18 reported `record new has no field account_id`. Fixed
   by structural IF/ELSIF, without schema/authority change.
2. ACL ordering defect: REVOKE/GRANT after ownership transfer to NOINHERIT writer
   emitted warnings01006/01007 and left PUBLIC EXECUTE. The first positive Staff
   update only proved compatibility with that incorrectly broad ACL. Strengthened
   test checked catalog ACL, actual current_user and one-row UPDATE, and failed.
   ACL now executes before ownership transfer, preserving the approved caller list.

After that correction, the catalog assertion denying Staff EXECUTE passes, then
the legitimate unmarked Staff update fails with `permission denied for function
local_admin_registered`, in guard_local_admin_account. Parent AON-SQL-19 does not
list identity_staff_writer. No new predicate grant was applied to work around it.

Evidence:

- `.demo/cskh-focused-e6e1b1ff18e744e19e09b6878e221b32`: first trigger-record ERROR.
- `.demo/cskh-focused-e3569a45777a47f591e1efce5b23b9ab`: positive Staff update PASS
  before adding the ACL assertion; not accepted as least-privilege proof.
- `.demo/cskh-focused-54cfde5b42d14509aca13abc2aa6c549` and
  `.demo/cskh-focused-bfdd97fa916040e0b28cf7c3afb2f343`: expected false / actual true
  ACL failure; private Flyway diagnostics identify the no-op ACL warnings.
- `.demo/cskh-focused-90c3199c8c8446ce91794acf051e5065`: corrected ACL,1 mandatory
  regression FAIL at Staff UPDATE, SQL permission denial. sourceUnchanged=true.

Automatic approval review rejected an alternative patch converting several
provenance guards to SECURITY DEFINER and transaction/xmin-based checks: stated
risk was broader persistent privilege-boundary changes and unproven bypass safety.
That patch was not applied; no indirect workaround, GUC authority or guard
disablement was used. Current guards retain SECURITY INVOKER/current_user checks.
The independently safe ACL-order correction was applied and verified as above.

One narrowly reviewable [AON-ACL-01 delta](admin-onboarding-staff-predicate-acl.delta.md)
requests only boolean predicate EXECUTE for the existing NOLOGIN Staff writer,
source/test only. It does not ask to approve V39/V40 again. Remaining guard-dependent
onboarding/Root/HTTP/Browser acceptance is stopped until this exact gap is resolved.
No ADMIN_ONBOARDING_SOURCE_TEST_READY or real Local UAT claim.

No live SMTP/onboarding/UAT, full-suite or CI PASS is claimed. No real services,
accounts, operator memberships, credentials, certificates or global settings changed.

Final bounded checks at the ACL checkpoint: report-inventory tests2/2 PASS;
new prerequisite tests3 plus retained Staff prerequisite1 =4/4 PASS. The two new
PG migration/regression identities are mandatory in the finite collector inventory;
its functions are unchanged and no failing case was removed/skipped. Repository
validator PASS, ownership CONSISTENT,10 preexisting broken links retained;
git diff whitespace check PASS. Approved manifest and all37 historical SQL hashes
match preservation. Rejected guard rewrite and unapproved Staff EXECUTE are both
absent from source. The current mandatory PG regression remains FAIL as recorded.
