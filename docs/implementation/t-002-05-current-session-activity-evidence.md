# T-002-05 Current Session and meaningful Activity Application evidence

Current source CI checkpoint: **T_002_05_CURRENT_SESSION_ACTIVITY_CI_VERIFIED**.
See [verified CI closure](#verified-current-sessionactivity-ci-checkpoint---2026-10-24).
The implementation/local checkpoint below is retained as historical pre-CI evidence.

Date: 2026-10-24 (Asia/Saigon). Owner: OWNER_IDENTITY_CURRENT_SESSION_ACTIVITY:
CONDITIONAL_GO. Verdict:
T_002_05_CURRENT_SESSION_ACTIVITY_IMPLEMENTED_VERIFICATION_PENDING.

## Baseline and preservation

PR24 is actually MERGED at f935c8e9fdfac5dd0fd97e1a5d2a07fa2e238ede, with parents
d0615d2d1fb4706228114aab0ef30e06228a5095 and
c3808e030ff353455f282834f5dc3ae2bae87d66. origin/develop matches that merge.
New isolated branch: feature/002-identity-current-session-activity; worktree:
D:/TLCN/e-logistic-identity-current-session. Initial tree clean/index empty.
Original Foundation six uncommitted paths and previous Activation/Session worktrees
are preserved; no reset/clean/stash/rebase or old-branch development. This checkpoint
has no commit, push, PR, merge or CI dispatch.

## Canonical ownership and Definition of Ready

- [Feature specification](../../specs/002-identity-access/spec.md) REQ-002-02/03/05
  and AC-002-02/03/04/05 supply session validity, authorization, privacy/revocation
  and edge/concurrency supporting coverage, not whole-AC acceptance.
- [Tasks](../../specs/002-identity-access/tasks.md): T-002-05 owns Application use
  cases/ports/local transactions. T-002-06 owns HTTP/security; T-002-07 owns UI.
- [Accepted contract](../../contracts/openapi/identity-access.yaml), operation
  getIdentityMe (GET /auth/me relative to /api/v1): approved Account fields,
  SESSION_INVALID denial; /me polling never extends idle.
- [Contract semantics](../../contracts/identity-access/README.md),
  [ADR-0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md)
  and [ADR-0010](../architecture/decisions/ADR-0010-identity-system-realm.md):
  trusted production access plus authoritative current account/session/grants;
  access<=15m, absolute<=8h, idle<=30m; only accepted meaningful user activity
  extends idle. Existing Domain IdentitySession.Activity.USER_ACTION is the
  server-classified transition; refresh/CSRF/health/me/background polling excluded.
- Existing guarded Session JDBC transaction, authoritative account view, ACTIVE
  Realm eligibility and existing auth_session columns suffice. No migration.
- No accepted activity-feed/audit-query endpoint or pagination DTO exists in this
  scope. Activity means idle accounting, not raw audit access. Feed/pagination are
  NOT APPLICABLE here; no new endpoint, unrestricted audit query or retention policy.
- The trusted admission interface is a required inward port. Its actual production
  signature/issuer/audience/JWT/Origin/CSRF/throttling adapter remains T-002-06;
  Application implementation is independently testable, with no permissive default.
  No blocking external-owner/provider decision is bypassed. Unsupported contexts
  retain the existing fail-closed Realm policy; no business-role expansion.

## Implemented Application behavior

IdentityCurrentSessionCommands accepts only an internal request correlation UUID:
getIdentityMe returns the existing approved IdentityGrantCommands.Account;
recordMeaningfulActivity returns void. No client account/session IDs, activity
boolean, bearer/refresh token, digest or new response fields enter this API.

CurrentSessionAdmission must resolve a genuine trusted production context, not
convert unsigned AccessGrant, Demo AuthenticatedActor or request IDs into proof.
It supplies verified sub/sid/authVersion/issuedAt/expiry. MEANINGFUL_ACTIVITY is a
distinct admission operation that additionally attests an actually completed,
authorized user action classified by server use-case policy. Merely being an
authenticated read does not qualify. Real transport/admission wiring is not claimed.

Application validates access-fact completeness, positive authVersion, issue/expiry
ordering and <=900s access lifetime before store access, then rechecks time AFTER
locks to reject token expiry while waiting. It enters the existing Session-owned
transaction; locks the Identity administration population, authoritative account
and session; binds stored owner to trusted subject/session/authVersion; applies
Domain active/revoked/idle/absolute/monotonic-clock checks and ACTIVE Realm/full
supported grant-context eligibility. Token deadlines may not exceed session idle/
absolute deadlines. No arbitrary target/Realm selection or Demo fallback exists.

getIdentityMe reads the approved immutable Account view and never records activity,
issues/rotates credentials or adds audit/receipt rows. ETag/no-store/HTTP rendering
remain T-002-06. Activity uses the existing Domain USER_ACTION transition and
updates ONLY last_activity_at and idle_expires_at, conditional on the exact locked
session/account/authVersion/previous timestamps and nonrevoked state. Absolute
expiry, refresh family, credentials, grants, account versions, audit/receipt rows
are unchanged; repeated same-instant activity is a no-op. Idle is capped by absolute
expiry and can never move backwards or revive an expired/revoked session.

The existing guard rejects ambient transactions, active synchronization or bound
resources before shared REQUIRED participation. No shared Grant propagation,
REQUIRES_NEW or caller transaction suspension/commit/rollback is added. The same
administration/row-lock protocol serializes activity/read against revocation and
Realm deactivation. Conditional write failure propagates for rollback. No new
Application logic alters strict refresh replay or the five prior ownership tests.
Composition is explicit/unregistered and requires admission, store and clock.

## Exact test inventory

New unit methods (21):

- currentMeReturnsApprovedAccountWithoutActivityMutation
- meaningfulActivityUsesDistinctTrustedAdmissionAndDomainTransition
- repeatedActivityAtSameInstantDoesNotWrite
- missingAdmissionCannotCompose
- missingRequestIdCannotReachAdmissionOrStore
- rejectedTrustedAdmissionNeverReachesStore
- authenticatedReadDoesNotAuthorizeMeaningfulActivity
- malformedTrustedContextsFailWithoutDatabaseAccess
- expiredFutureOrOverlongAccessFactsFailClosed
- accessExpiryIsRecheckedAfterWaitingForLocks
- crossAccountSessionSubstitutionFailsBeforeAccountDisclosure
- missingPersistedSessionFailsClosed
- staleTrustedOrPersistedAuthorizationVersionFailsClosed
- revokedSessionFailsClosed
- exactIdleOrAbsoluteExpiryCannotBeExtended
- disabledPendingAndUnverifiedAccountsFailClosed
- inactiveRealmOrUnsupportedGrantContextFailsClosed
- meaningfulActivityIsCappedByAbsoluteDeadline
- backwardClockCannotAdvanceOrRewindActivity
- activityPersistenceFailureIsPropagatedForTransactionRollback
- currentSessionBoundaryExposesNoClientIdentityActivityFlagOrSecrets

New PostgreSQL methods (14) in the existing IdentityGrantApplicationTests suite:

- currentSessionMeReturnsApprovedAccountWithoutExtendingIdle
- currentSessionActivityUpdatesOnlyOwnedIdleFields
- currentSessionAccountAndRealmSubstitutionFailClosed
- currentSessionExpiredAccessCannotReadOrRecordActivity
- currentSessionStaleVersionAndDisabledAccountDenyWithoutMutation
- currentSessionPendingAccountCannotUseOldSession
- currentSessionExactIdleAbsoluteAndRevocationDenyWithoutMutation
- currentSessionInactiveRealmAndExternalGrantsFailClosed
- currentSessionActivityNeverExtendsAbsoluteDeadline
- currentSessionActivityWriteFailureRollsBackAllState
- currentSessionOwnedBoundaryRejectsAmbientReadAndActivity
- currentSessionWaitingActivityObservesConcurrentRevocation
- currentSessionWaitingMeObservesConcurrentRealmDeactivation
- currentSessionConcurrentActivityCannotRewindIdle

PostgreSQL cases reuse isolated runtime roles/DataSource, existing ownership/cleanup
registry, real JdbcTransactionManager and READ_COMMITTED transactions. They check
state on independent connections after outer completion, unchanged protected rows,
owned/no-op activity, exact expiry/revocation/Realm denial, mutation fault rollback,
ambient read/activity rejection, controlled concurrent revocation/Realm changes and
monotonic concurrent activity. Synthetic trusted-context fixtures are explicitly
not production JWT/HTTP proof. No developer URL/credentials or new Task C suite.

All previous128 PG methods and24 Session unit methods are preserved. Existing
five ownership methods and both replay/concurrency cases remain mandatory.
Strict finite inventory adds only the discovered21 unit +14 PG identities:
142 combined PG methods; projected H2375/17 expected skips, PG567/5 expected skips.
PG projections are NOT runtime results. New report-policy controls reject every
new missing/renamed/skipped/failed/error case and prevent private diagnostic text
from entering sanitized output; previous discovery/skip/lifecycle guards remain.

## Actual local verification

| Gate | Result |
| --- | --- |
| Verified direct Java21.0.10 / Maven3.10.0 --version | PASS |
| Offline Maven verify, synthetic user/global settings, allowlisted child environment | exit0 / BUILD SUCCESS;42.3s |
| Strict fresh H2 report guard | PASS:392 discovered /375 PASS /17 expected skips /0 failure/error |
| Current Session unit | 21/21 PASS |
| Previous Session unit, including ownership guards | 24/24 PASS |
| Architecture including negative fixtures | 23/23 PASS |
| Domain | 48/48 PASS |
| Activation unit / Provision input / Grant authority / Realm policy | 12/12 /12/12 /9/9 /8/8 PASS |
| Python report-policy | 119/119 PASS;73.278s |
| Python migration source | 15/15 PASS |
| Python outbox source | 9/10 methods PASS; one method fails2 historical V3/V4 CRLF byte-pin subcases |
| New PG Java compilation | PASS,14 methods present in compiled test class |
| Real PostgreSQL current-source runtime/concurrency/cleanup | UNVERIFIED: Docker Engine unavailable |
| GitHub current-source CI | NOT RUN |

Docker CLI is available at this checkpoint; docker context show returns desktop-linux.
docker --context desktop-linux version --format '{{.Server.Version}}' exits1 with
cannot-find endpoint category; context endpoint is npipe:////./pipe/dockerDesktopLinuxEngine.
No Docker startup/restart/config change, containers/volumes or developer PostgreSQL
access was attempted. C-drive free space alone was not treated as a blocker.

Commands: python -B -m unittest discover -s scripts/ci -p test_verify_reports.py;
verified direct Maven3.10.0/bin/mvn.cmd -o -B -ntp -s <synthetic-settings>
-gs <synthetic-settings> -Dmaven.repo.local=<resolved-cache> -Dstyle.color=never
-DargLine=<bounded-heap-and-session-temp> verify; python -B scripts/ci/verify-reports.py
--source <fresh-private-reports> --output <sanitized-output> --mode h2 --maven-exit 0.
Static migration/outbox suites ran via unittest with in-memory diagnostics and
sanitized method/subcase results. Maven console output was handled in memory;
ignored raw Surefire reports remain private, never tracked/public deliverables.

Retained ignored evidence: .demo/current-session-activity/ contains pre-edit SHA-256,
actual method inventory, Maven summary, sanitized H2 XML/summary, static-suite
results and final integrity assessment. No real credential/secret store was read.
V1-V5 and contracts remain byte-identical to pre-edit baseline. Canonical V3/V4
Git blob hashes retain their accepted pins; CRLF working-tree failures are not
relabelled PASS and no SQL/hash pin/guard was changed. Historical Maven Wrapper
bootstrap attribution stays OPEN / NOT REPRODUCED; direct Maven success is not a
new Wrapper verification result. Python source suites remain outside hosted CI.

## SDD status and next verification gate

T-002-05 PARTIAL/unchecked; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. No new endpoint, migration, accepted
wire-contract change, provider or business-module scope. Remaining: real current-
source PG runtime/CI; production JWT/HTTP/cookie/CSRF/throttling and meaningful-action
admission integration (T-002-06); recovery, resend, remaining lifecycle/query and
authority use cases; T-002-08 delivery/provider/maintenance work. Full Foundation/
Day1 acceptance is not implied. Business-feature owner review stays mandatory.

Next checkpoint requires separate authorization to publish and run the existing
three CI jobs against the exact source revision, with all21 unit/14 PG new cases,
142 prior+new PG suite methods, strict reports/privacy/owned cleanup. Existing
Session source/PR CI is historical evidence only and does not verify this code.


## Verified Current Session/Activity CI checkpoint - 2026-10-24

Owner: OWNER_CURRENT_SESSION_ACTIVITY_CI_CLOSURE: CONDITIONAL_GO.
Current slice verdict: **T_002_05_CURRENT_SESSION_ACTIVITY_CI_VERIFIED**.
The preceding implementation/local results are historical pre-CI evidence and
remain intact. This checkpoint supersedes ONLY their current-source PG/CI pending
status; it does not erase the local Docker limitation or historical findings.

### Source and run provenance

- Workflow: Backend verification, ID 383888734, backend-verification.yml.
- [Run 40062627539](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40062627539):
  attempt 1, event workflow_dispatch, completed/SUCCESS; created 2026-10-24T04:30:44Z,
  updated 2026-10-24T04:34:44Z.
- Branch: feature/002-identity-current-session-activity.
- CI-tested source commit: 231ce63fa6ca5d12aae57e8ad2069d95ada59da9.
  Actual checkout and job head SHA match this full SHA in all three jobs.
- Source publication: exactly 14 approved paths, one implementation commit and one
  normal push; parent f935c8e9fdfac5dd0fd97e1a5d2a07fa2e238ede (PR24 merge).
- This subsequent closure publication changes ONLY this evidence document and
  supplemental Feature 002 traceability. Its separate docs-only commit is NOT the
  CI-tested source revision. No CI rerun/dispatch, PR creation or merge is authorized
  by this documentation checkpoint. No claim that CI tested the later docs revision.

### Actual reports and finite inventory

| Exact job | Job ID | Discovered | PASS | FAIL | ERROR | Expected skips | Report-policy controls |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Architecture gate | 120779198071 | 23 | 23 | 0 | 0 | 0 | 119 PASS |
| Backend default / H2 | 120779198318 | 392 | 375 | 0 | 0 | 17 | 119 PASS |
| Backend PostgreSQL 18 | 120779198284 | 572 | 567 | 0 | 0 | 5 | 119 PASS |

Sanitized XML exactly matches the approved class/method identities and outcome
inventory. H2 skips only the eight PostgresFoundationTests, four
PostgresHealthSafetyTests and five FoundationDependencyTests. PostgreSQL skips
only the same five Redis/RabbitMQ FoundationDependencyTests; those are NOT remote
infrastructure verification. No new Current Session/Activity test is skipped.

- Current Session unit: 21/21 PASS in both H2 and PostgreSQL jobs.
- New Current Session PostgreSQL: 14/14 PASS, exact methods listed above.
- Previous 128 PostgreSQL method identities: 128/128 PASS, retained source bodies
  unchanged. Combined IdentityGrantApplicationTests: 142/142 PASS.
- Prior Session unit 24/24 and PG Session 23/23 remain PASS, including all five
  ownership regressions and both replay/concurrency cases. Domain 48, Identity
  Migration 25, invitation protection 10 and existing Activation/Provisioning/Outbox/
  Realm/Grant inventories are present and PASS.

These are overlapping subsets of the full job totals, NOT additional tests to add
to 375/567. In particular 142=128+14, and 21 unit cases occur within each full job.

### Application/Persistence security evidence

The 21 unit cases plus 14 real PostgreSQL integration cases verify the independently
composed Application/Persistence boundary. PostgreSQL tests use synthetic trusted
admission, isolated runtime roles and real transactions; they do NOT establish
production signature verification or a completed HTTP/Spring Security adapter.

| Behavior | PostgreSQL method evidence (all PASS) |
| --- | --- |
| Approved Account only, authoritative session owner; /me never writes activity or extends idle | currentSessionMeReturnsApprovedAccountWithoutExtendingIdle; currentSessionAccountAndRealmSubstitutionFailClosed |
| Meaningful activity updates only last_activity_at/idle_expires_at; repeat is a no-op | currentSessionActivityUpdatesOnlyOwnedIdleFields |
| Access expiry, persisted authVersion, ACTIVE account/Realm eligibility and default deny | currentSessionExpiredAccessCannotReadOrRecordActivity; currentSessionStaleVersionAndDisabledAccountDenyWithoutMutation; currentSessionPendingAccountCannotUseOldSession; currentSessionInactiveRealmAndExternalGrantsFailClosed |
| Exact idle/absolute deadline and revocation denial; absolute deadline cap | currentSessionExactIdleAbsoluteAndRevocationDenyWithoutMutation; currentSessionActivityNeverExtendsAbsoluteDeadline |
| Failed write rolls back; owned boundary rejects ambient reads/activity | currentSessionActivityWriteFailureRollsBackAllState; currentSessionOwnedBoundaryRejectsAmbientReadAndActivity |
| Locked authoritative state observes concurrent revocation/Realm deactivation; idle cannot rewind | currentSessionWaitingActivityObservesConcurrentRevocation; currentSessionWaitingMeObservesConcurrentRealmDeactivation; currentSessionConcurrentActivityCannotRewindIdle |

CurrentSessionAdmission remains mandatory, with distinct CURRENT_SESSION and
MEANINGFUL_ACTIVITY operations. Application never trusts client identity/activity
flags. The activity admission contract must attest a completed, authorized action;
unit tests prove an authenticated read cannot qualify through that boundary.
Production enforcement by its actual trusted adapter remains T-002-06, UNVERIFIED.
Post-lock time/deadline revalidation is exercised by unit tests; real PostgreSQL
concurrency cases verify authoritative state and session monotonicity under locks.

Existing owned-transaction guard, fail-closed ambient transaction detection,
durable refresh-replay family revocation, authVersion invalidation, Realm isolation,
access<=15 minutes, absolute<=8 hours and idle<=30 minutes are preserved. No Demo
principal fallback, client flag shortcut, credential disclosure or shared Grant
transaction propagation modification was introduced. No current-session read is
classified as meaningful activity. Absolute expiry/revocation cannot be revived.

### Artifact, privacy and cleanup integrity

| Sanitized artifact | Artifact ID | GitHub SHA-256 digest (matched local ZIP) |
| --- | --- | --- |
| backend-architecture-1 | 12547967462 | 6cd9e562c2087914b331ea8ceaeef7bc7cca7329247b6b62401b47fc87c9a5ff |
| backend-h2-1 | 12547224416 | 2cec6a3b2b35f322615adc5be435a9fdcfdda1e17d716e75bfedb525f7d2989d |
| backend-postgres-1 | 12547972954 | b1f2f506efc9dc88451b465659be99a63b1c37ead46641eb5ad54f4a5a989de3 |

All three ZIPs pass CRC and contain only summary.json/tests.xml. XML has only
allowlisted class/method/outcome/count fields, with fixed redacted skip text; no
raw properties, system output or diagnostic bodies. Artifact head SHAs match the
run/source. 119 report-policy controls PASS per job, with no inventory relaxation.
Temporary database credential log audit PASS in all jobs. Generated Testcontainers
credential privacy audit, owned-resource cleanup audit and disposable PostgreSQL
cleanup PASS in the PostgreSQL job. All corresponding steps completed successfully.
Only sanitized projections of fixed guard markers/checkout/counts were persisted;
raw CI logs were processed in memory, not saved or quoted in public documentation.
Retained ignored packet: .demo/current-session-activity-ci/; closure validation is
private in .demo/current-session-activity-ci-closure/. Neither directory is published.

### Remaining scope, historical findings and status

- T-002-06 production HTTP/JWT/cookie/CSRF/throttling, trusted current-session
  admission and completed-user-action adapter wiring remain incomplete. This CI
  does not prove those adapters or end-to-end Production Identity readiness.
- Recovery, Resend and remaining lifecycle/query/authority operations remain open;
  delivery/provider integration remains under canonical T-002-08.
- Historical Maven bootstrap attribution remains OPEN / NOT REPRODUCED. This run
  verifies Java21/Wrapper3.10.0 on a clean runner but does not resolve that cause.
- Python migration/outbox source suites still lack current workflow execution
  evidence. Windows outbox source-check remains 9/10 methods PASS, one method with
  two historical V3/V4 CRLF raw-byte subcase failures. No new PASS is inferred.
  Canonical V1-V5 Git blobs/pins and accepted contracts are unchanged.
- Six original Foundation fingerprints, main traceability and previous Activation/
  Session worktrees are unchanged; no source, policy, SQL or test assertion changed
  during this docs-only closure.

T-002-05 stays PARTIAL/unchecked; Feature 002 and AC-002-01..05 stay NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Feature 001/D1-08/Day 1 remain PARTIAL.
Current Session/Activity source CI is VERIFIED, with no whole-feature acceptance.
Next checkpoint is a bounded PR-readiness review; no PR or merge in this task.
Before full Merchant, Business Admin, Shipper or other business-role implementation:
BUSINESS_FEATURE_IMPLEMENTATION_REVIEW_REQUIRED, with canonical feature/task IDs,
role-permission matrix, workflows, API/database/UI scope and acceptance criteria;
explicit owner review/approval is still required.
