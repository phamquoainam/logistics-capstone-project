# T-002-05 Current Session and meaningful Activity Application evidence

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
