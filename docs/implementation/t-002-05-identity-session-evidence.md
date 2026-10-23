# T-002-05 Identity Session Application checkpoint - 2026-10-23

Status: **T_002_05_SESSION_CI_VERIFIED**.
The final CI closure section supersedes earlier pending/runtime-unverified
statements only for tested source caa150fb; preceding sections retain their
historical implementation/local checkpoint state.
Original implementation authorization:
`OWNER_IDENTITY_SESSION_APPLICATION: CONDITIONAL_GO`.
T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED.
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains. No original REQ/AC is changed.

## Baseline and canonical ownership

PR23 is actually MERGED at `d0615d2d1fb4706228114aab0ef30e06228a5095`, freshly
fetched and verified as origin/develop. Isolated worktree
`D:/TLCN/e-logistic-identity-session`, branch
`feature/002-identity-session-application`, starts at that exact baseline.
The original dirty Foundation worktree and clean Activation worktree are retained.
Six original Foundation SHA-256 fingerprints and the original empty index are
preserved; no source from that dirty worktree is copied into this slice.

Canonical T-002-05 owns backend commands, inward ports, Application orchestration
and Identity-local runtime JDBC transactions. This slice implements loginIdentity,
refreshIdentity and logoutIdentity. T-002-06 retains JWT signing/verification,
HTTP routing, exact Origin, server-bound CSRF proof/family validation, challenge/
cookie rebinding, rate limiting and secure response mapping. T-002-07 retains UI,
in-memory access handling and client single-flight. No new endpoint or HTTP DTO.

`SessionAdmission` is a mandatory trusted per-request security port, NOT a client
boolean or identity assertion. The future adapter must capture/verify genuine
server-side request state and enforce Origin/CSRF/throttling even on malformed
requests; a request UUID is correlation only. Refresh/logout additionally pass
the family resolved from the stored opaque-token digest, never a client account.
No default permissive adapter exists. Explicit production composition requires
non-null admission and is not a Spring bean/controller. Test-only permitting
lambdas/mocks are not registered in production.

## Implemented transitions and security

1. Login validates exact Username and legacy-compatible PasswordPolicy.validateLogin
   (not the new-password minimum), then uses authoritative account lookup under
   the shared Identity administration transaction lock. Real BCrypt checks a
   dummy verifier for unknown/missing credential rows. Unknown/wrong/pending/
   disabled/ineligible accounts share INVALID_CREDENTIALS, without writes.
2. Supported eligibility is deliberately bounded to the currently approved exact
   Identity Management/IDENTITY pair in a registered ACTIVE Realm. Missing,
   inactive, mixed/external or unsupported owner scopes fail closed. No general
   Merchant/Warehouse/Hub login authority or Realm registration is invented.
3. Create a session from the Domain: absolute8h, meaningful-activity idle30min,
   access signing facts capped at15min and both stored deadlines. Persist the
   session, SHA-256 digest of fresh256-bit canonical Base64url refresh material
   and fixed audit atomically. Account version/grants/Realm are not mutated.
4. Refresh hashes the exact canonical43-byte opaque token, resolves its actual
   session, locks current account/session/proof-token state and rechecks ACTIVE/
   verified account, current authVersion, Realm/grant eligibility and deadlines.
   Reuse RefreshFamily.rotate. Compare-and-consume prior token before insertion
   of its unique successor in the same transaction. No idle/absolute extension,
   replay grace, idempotency receipt or additional permission is introduced.
5. Consumed-token replay revokes session and ALL family refresh rows plus fixed
   audit. Return an internal denial outcome from the transaction, then raise
   REFRESH_REPLAY AFTER commit: throwing inside the callback would wrongly undo
   the revocation. Mandatory audit/persistence failure rolls back every effect.
   Concurrent same-token calls produce one rotation followed by replay/family
   revocation, not two usable successors. Strict client single-flight remains.
6. Logout allows a known expired/revoked family after trusted admission; revokes
   only that resolved session's family, audits only a real change and is a
   no-op on repetition. Missing cookie still invokes mandatory preauth admission;
   it is not a CSRF exemption. Malformed/unresolvable cookie denies as CSRF_REJECTED
   rather than selecting an arbitrary client session. No sessions are restored.
7. Same runtime DataSource/READ_COMMITTED transaction and existing administration
   advisory lock;15s transaction,5s lock and10s statement budgets retained. No
   new container suite, privilege, schema, migration or external side effect.
8. Tokens are defensive owned byte buffers with explicit close/wipe; audit/errors/
   toString are fixed/redacted; only digests/verifiers persist. Returned refresh
   material is for a trusted backend transport adapter only, never JSON/logs.
   AccessGrant contains unsigned signing facts, NEVER a verified principal or JWT.
   Future T-002-06 must use distinct production signing/validation and current
   persisted session checks; this slice cannot authorize a bearer from a user ID.
   Immutable Java password Strings cannot be promised securely erased.

## Tests and verification

New authored inventory:20 IdentitySessionApplicationTests unit/crypto methods and
18 `session*` PostgreSQL methods in existing IdentityGrantApplicationTests.
All105 previous method bodies are retained; grant/application suite now123.
Existing three-suite Testcontainers lifecycle/ownership registry, immutable images,
strict expected skips and cleanup/privacy requirements are unchanged.

Unit cases cover valid login, dummy comparison, wrong/unknown/pending/disabled,
Realm denial, missing/rejected admission, input bounds, rotation/deadlines,
committed replay denial, stale authVersion, expiry/revocation/account substitution,
logout with/without cookie, idempotence, byte ownership/redaction and real BCrypt/
SecureRandom/SHA-256. PostgreSQL cases cover real runtime-role persistence,
credential/eligibility denial, rotation lineage, committed replay revocation,
concurrent refresh, idle/absolute expiry, authVersion/Realm invalidation,
cross-account logout isolation, mandatory-write rollback, security-port binding,
secret persistence negatives and concurrent Realm-state admission.

| Gate | Actual result |
| --- | --- |
| `python -B -m unittest discover -s scripts/ci -p test_verify_reports.py` |116/116 PASS; new3 controls include290 missing/renamed/skipped/failure/error method mutation subcases; synthetic policy evidence only |
| `python -B app/backend/test_identity_migration.py` |15/15 PASS; static controls, not PostgreSQL execution |
| Java21/direct cached Maven3.10.0 offline focused `test` |BLOCKED before compilation by unresolved org.testcontainers:testcontainers:jar:2.0.5; no test PASS inferred |
| Same focused Maven test after normal dependency resolution, explicit CI/global settings and child environment allowlist |PASS: main/test compilation; Session20, Activation12, Architecture11+negative/control12 =55 tests,0 failures/errors/skips |
| Full default/H2 `clean verify` and actual strict report guard |PASS:367 discovered,350 PASS/0 FAIL/0 ERROR/17 exact expected skips; strict guard exit0 |
| New Session PostgreSQL18 runtime |UNVERIFIED: Docker CLI unavailable in this execution environment; no developer PostgreSQL fallback/container operation |
| Current-source GitHub Actions |NOT RUN; no dispatch/publication authorized; PR23 CI does not verify this uncommitted source |

Maven was invoked via Java21/Classworlds from the existing approved Maven3.10.0
wrapper distribution, not PATH Maven3.9.12. Explicit `scripts/ci/maven-settings.xml`
was used for both user/global settings; child environment allowlisted system/JDK/
temporary paths only, excluding developer service credentials, Java agent/options
and application settings. Raw Maven output stayed in memory; sanitized local
metadata is in ignored `.demo/session-local-check/`. This is not new wrapper
verification, production HTTP evidence or PostgreSQL runtime acceptance.

Future current-source PostgreSQL expectation:523 PASS/5 expected foundation skips,
UNVERIFIED. Actual local default/H2 is350 PASS/17 expected skips; Architecture23/0
skips and116 policy controls PASS. Remote current-source execution remains pending.
Strict inventory retains every new20+18 identity and rejects missing/renamed/
skipped/failed/error cases. No disabled test or relaxed skip/assertion is used.

## File scope and handoff

Production: IdentitySessionCommands, IdentitySessions, SessionAdmission,
SessionCredentials, IdentitySessionStore, ProtectedSessionCredentials,
JdbcIdentitySessionStore, ProductionIdentitySessionComposition; accepted fixed
error codes added in IdentityCommandFailure. Tests: new unit class and appended
PostgreSQL cases only. CI: finite verify-reports/test_verify_reports inventory,
not workflow/job names, runner script, cleanup registry or branch ruleset.
SDD: this evidence, Application evidence link, design/tasks append and supplemental
traceability. Protected main traceability is not edited. V1-V5 Git blobs, accepted
contracts, Domain and Demo source remain unchanged.

Prepare a separate authorized current-source CI checkpoint for PostgreSQL523/5,
real transaction/race/rollback, V1-V5 regressions and credential/privacy/owned
cleanup; do not reuse Activation run39920826371. No automatic commit/push/PR or
workflow dispatch. PostgreSQL tests need a genuine disposable Docker runtime,
existing approved images/helper policy and synthetic role credentials only.

Remaining T-002-05: current-source PostgreSQL session acceptance, current-session/
meaningful-activity integration, recovery, resend, lifecycle/query and remaining
owner authority/security use cases. HTTP admission/JWT/CSRF/cookies/rate limiting
and secure request mapping remain later T-002-06. Delivery worker/provider and
maintenance remain T-002-08 after canonical dependencies. No Production Identity
end-to-end/deployment acceptance is implied.

Business implementation is NOT ready. Dependency001->002->003; T-003-01 Review
and T-003-02 accepted Contract precede T-003-03 migration (first production
persistence), then T-003-04/05 Domain/Application. When prerequisites are actually
satisfied, emit BUSINESS_FEATURE_IMPLEMENTATION_REVIEW_REQUIRED and obtain owner
review of task/workflows/roles/API/DB/UI/AC/decisions BEFORE implementation.
No Merchant, Business Admin, Shipper or other business module is started here.


## Actual local runtime and strict-report checkpoint

Direct Java21 / approved cached Maven3.10.0 `clean verify`, explicit user/global
CI settings, child-only environment allowlist and512MiB Maven heap: exit0,
BUILD SUCCESS. Actual H2 strict guard invocation:

```text
python -B scripts/ci/verify-reports.py --source app/backend/target/surefire-reports --output .demo/session-local-check/h2-sanitized --mode h2 --maven-exit 0
h2: PASS; discovered=367, executed=350, passed=350, skipped=17
```

Seventeen named skips are unchanged: FoundationDependencyTests5,
PostgresFoundationTests8, PostgresHealthSafetyTests4. Testcontainers-tagged suites
are excluded by the existing default Maven profile, not counted as new skips.
Session20, Activation12, Domain48, architecture11+negative/control12 and existing
Demo authentication/shipment regressions all actually ran. Focused55 overlaps
with this full350; it is not55 additional tests. No PG source execution is inferred
from successful test compilation. No raw Maven output is retained/published.
PostgreSQL runtime and current-source GitHub remain UNVERIFIED, so the slice
verdict remains IMPLEMENTED_VERIFICATION_PENDING, not VERIFIED.

## Exact working change manifest

All paths below are relative to the isolated Session worktree. New/untracked
source and SDD are intentional; index remains empty. Generated target and private
.demo evidence remain ignored. No commit/push/PR/dispatch was performed.

- `app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/api/IdentitySessionCommands.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/application/IdentitySessions.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentitySessionStore.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/SessionAdmission.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/SessionCredentials.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentitySessionStore.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentitySessionComposition.java`
- `app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProtectedSessionCredentials.java`
- `app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentitySessionApplicationTests.java`
- `app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java`
- `docs/implementation/t-002-05-identity-application-evidence.md`
- `docs/implementation/t-002-05-identity-session-evidence.md`
- `docs/quality/feature-002-recovery-traceability.md`
- `scripts/ci/test_verify_reports.py`
- `scripts/ci/verify-reports.py`
- `specs/002-identity-access/design.md`
- `specs/002-identity-access/tasks.md`


## Session CI verified closure - 2026-10-23

**T_002_05_SESSION_CI_VERIFIED** under
`OWNER_SESSION_CI_EVIDENCE_CLOSURE: CONDITIONAL_GO`.
This is bounded Session Application acceptance, not completion of T-002-05 or
Production Identity. The original local/verification-pending records above are
preserved as history and superseded only for the tested source below.

### Run and source provenance

[Backend verification run39932569608, attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39932569608)
completed SUCCESS; event `workflow_dispatch`; workflow383888734,
`.github/workflows/backend-verification.yml`; branch
`feature/002-identity-session-application`.
Published/tested source and actual checkout in all three jobs:
`caa150fb3c1a2c4ac1a60d27566f07f6a3c3c479`, parent
`d0615d2d1fb4706228114aab0ef30e06228a5095` (merged PR23).
Source publication contained18 approved paths, one implementation commit,
one normal push and one workflow dispatch; no PR, merge or rerun.

| Job | Job ID | Discovered | PASS / executed | FAIL | ERROR | Expected skips |
| --- | --- | --- | --- | --- | --- | --- |
| Architecture gate |120344179401 |23 |23 |0 |0 |0 |
| Backend default / H2 |120344178985 |367 |350 |0 |0 |17 |
| Backend PostgreSQL 18 |120344179274 |528 |523 |0 |0 |5 |

Main/test compilation and current-source Maven Wrapper clean verify succeeded.
Architecture includes11 production/scope cases and12 negative-fixture/control
cases. H2 skips exactly FoundationDependencyTests5, PostgresFoundationTests8 and
PostgresHealthSafetyTests4. PostgreSQL skips exactly FoundationDependencyTests5;
these are not Redis/RabbitMQ runtime PASS. There are no unexpected skips.
Each job passed116 report-policy controls and strict finite method/status/skip
discovery; all actual job checkout projections identify the full source SHA above.

Session unit20/20 PASS in both H2 and PostgreSQL. All18 new Session PostgreSQL
methods PASS; all105 prior IdentityGrantApplicationTests methods PASS, including
20 Activation PG methods. Combined suite123/123 PASS. Retained regression evidence
also includes Activation unit12, Domain48, migration25, crypto10, provisioning
unit12/PG18, Realm unit8/PG7, authority unit9/PG12 and original grant33. These groups
overlap with CI totals and each other; do not add them as extra executed tests.

### Actual PostgreSQL security evidence

- `sessionLoginPersistsOnlyDigestSessionAndAudit`: real runtime-role session,
  refresh digest and audit persisted atomically; account version unchanged.
  Credential, pending/disabled, inactive Realm/unsupported scope and concurrent
  Realm-deactivation admission negatives also PASS without unauthorized writes.
- `sessionRefreshRotatesPredecessorAndPreservesDeadlines`: unique successor and
  predecessor consumption PASS; absolute8h/idle30min are not extended by refresh.
  Access facts remain capped at15min and session deadlines. Exact expiry,
  stale authVersion, disabled account and Realm-deactivation rejection PASS.
- **`sessionReplayCommitsFamilyRevocationBeforeDenial` PASS**: after the caller
  receives REFRESH_REPLAY, the stored session and all family refresh rows are
  revoked, and the latest successor cannot refresh. The denial is raised after
  the transaction returns/commits, not inside the callback where it would undo
  required revocation. This is real PostgreSQL evidence, not the mock unit case.
- **`sessionConcurrentRefreshHasOneRotationThenReplayRevocation` PASS**: two
  concurrent calls using the same token yield one rotation and one replay;
  only two token rows exist and the session is durably revoked. No replay grace,
  second usable successor or client retry exemption is introduced.
- Cross-account logout isolation/idempotence, expired logout and no-cookie
  admission, authoritative family binding and secret-persistence negatives PASS.
  `sessionLoginMandatoryWriteFailuresRollBackEverything`,
  `sessionRefreshSuccessorFailureRollsBackConsumption` and
  `sessionReplayAuditFailureRollsBackRevocation` PASS: mandatory failures retain
  atomic state; an audit-failure exception is not falsely returned as a completed
  REFRESH_REPLAY transition.

Exact20 unit/18 PG method identities and the preserved105 outcomes were compared
with the tested source and sanitized JUnit XML. No methods/assertions, skip policy,
Testcontainers lifecycle registry, SQL or contract were weakened for this result.
`AccessGrant` remains unsigned Application signing facts, not an issued JWT,
verified HTTP principal or a new authentication bypass. Test-only permitting
SessionAdmission fixtures prove orchestration/DB behavior, not production HTTP
Origin/CSRF/throttling. Composition remains explicit and unregistered.

### Artifact integrity, privacy and cleanup

Fresh GitHub metadata was rechecked against the retained sanitized ZIPs during
closure. All artifacts belong to run39932569608/sourcecaa150fb/attempt1; job matrix
names, successful upload steps and summary modes match their producing jobs.
All3 ZIP SHA-256 values match authoritative GitHub digest metadata:

| Artifact | GitHub ID | Verified ZIP SHA-256 = GitHub digest |
| --- | --- | --- |
| backend-architecture-1 |12492900195 |9dba838bd971aca0d9df164cf59ab5f58ff78d1adb090c41ac30c8447bfcbf85 |
| backend-h2-1 |12491972893 |dd6e2ece19ca808f05c9affb8bb2b9f463422e32b2f090d8138380fb72a50496 |
| backend-postgres-1 |12493045204 |b77d0940c98c42bd5cfb3a9847c79440465759d286512105defa5136fcd8790a |

ZIP CRC, exact `summary.json`/`tests.xml` entry allowlist, summary/XML counter and
method identity reconciliation PASS. XML contains only fixed fields/statuses and
the existing fixed non-sensitive skip placeholder, no raw failure/system output,
properties, credentials or private diagnostics. Sanitized evidence remains in
ignored `.demo/session-ci-publication/`; it is not staged or published by closure.

Every job's temporary database credential log audit PASS. PostgreSQL generated
Testcontainers credential privacy and owned-resource cleanup audits PASS, as did
removal of only the recorded disposable CI PostgreSQL container. Three existing
suite ownership/lifecycle requirements remained intact. Raw job logs were handled
only in memory for fixed SHA/count/status projections; no raw diagnostics, access
token or signed artifact URL was stored in the evidence/public documentation.

### Remaining coverage and SDD boundary

The workflow still does not invoke `app/backend/test_identity_migration.py` or
`app/backend/test_identity_invitation_outbox.py`; neither Python source suite has
CI execution evidence from this run. Java migration25/25 PASS is distinct from
Python source-suite execution. Historical Windows CRLF/LF source-guard findings,
accepted migration pins and unresolved historical bootstrap attribution remain
unchanged; successful Session bootstrap is not proof of a permanent root-cause fix.

T-002-05 overall remains **PARTIAL/unchecked**; Feature002 and AC-002-01..05 remain
**NOT ACCEPTED**; `GRANT_MUTATION_FUNCTIONALITY_LIMITED` remains. Feature001/D1-08/
Day1 remain PARTIAL. Pending: current-session/activity integration, recovery,
resend, lifecycle/query, remaining authority/security integration and canonical
HTTP/JWT/CSRF/cookie adapters. Delivery/provider/maintenance integration belongs
to T-002-08 after dependencies. No end-to-end or deployment-ready identity claim.

This closure publishes exactly this evidence and
[the supplemental trace](../quality/feature-002-recovery-traceability.md#session-ci-verified-trace---2026-10-23).
It is a later docs-only commit: run39932569608 verifies sourcecaa150fb above,
NOT the documentation revision recording it. No manual CI dispatch/rerun or PR/
merge is part of closure. Original six Foundation fingerprints, clean Activation
worktree, V1-V5 and all unrelated source/contracts are preserved. Business-module
readiness is not reached; the documented owner-review gate remains in force.


## Session transaction ownership correction - 2026-10-23

Owner authorization: OWNER_SESSION_TRANSACTION_OWNERSHIP_FIX: CONDITIONAL_GO.
Baseline HEAD: 0551b27976e8b6e7d78ecbd504335c82c0c28d14. This section records
uncommitted corrective source; it is NOT evidence that historical run39932569608
(sourcecaa150fb) tested the correction. No commit/push/dispatch/PR/merge performed.

### Finding and enforcement

The Session JDBC store delegates to JdbcIdentityGrantStore's TransactionTemplate
with default REQUIRED propagation. In a compatible caller READ_COMMITTED transaction
on the same DataSource, callback completion is not physical commit. Raising
REFRESH_REPLAY afterward could roll back family revocation/audit with the caller.
This violates durable replay denial despite the prior direct-call tests passing.

JdbcIdentitySessionStore.transaction now rejects before entering the shared store
when TransactionSynchronizationManager reports an actual transaction, active
synchronization, or any pre-bound resource. The resource check is deliberately
conservative, including unknown/unrelated resource keys: a clean Spring transaction
context is required rather than guessing DataSource wrapper/resource ownership.
The internal IllegalStateException has the fixed, value-free message
SESSION_TRANSACTION_OWNERSHIP_REQUIRED; no public HTTP/error-code contract changed.
All persistence-backed Login/Refresh/Logout paths use this boundary. Missing-cookie
Logout still performs mandatory admission without a persistence transaction.
No caller resource is suspended, committed, rolled back, unbound or cleared by the
production guard. Shared Grant propagation, SQL, admission, deadlines, authVersion,
Realm policy, rotation/audit semantics and cleanup registration are unchanged.

### Added regression inventory

Four real unit methods (Mockito only proves pre-JDBC rejection, not PG durability):
- storeRejectsActualAmbientTransactionBeforeJdbcAccess
- storeRejectsAmbientSynchronizationBeforeJdbcAccess
- storeRejectsBoundDataSourceBeforeJdbcAccess
- storeRejectsUnknownBoundResourceBeforeJdbcAccess

Five added PostgreSQL methods in IdentityGrantApplicationTests:
- sessionAmbientRefreshRejectedBeforeRotationAndCallerCommit
- sessionAmbientReplayRejectedBeforeCallerRollback
- sessionDirectReplayRevocationAndAuditAreIndependentlyCommitted
- sessionAmbientLoginRejectedBeforeCallerCommit
- sessionAmbientLogoutRejectedBeforeCallerRollback

Outer-transaction tests use the SAME runtime DataSource instance and real
JdbcTransactionManager/TransactionTemplate at READ_COMMITTED as Session composition.
Caught ownership rejection leaves transaction control with the caller. Committed
state is checked using independent runtime connections only after the outer scope
finishes; snapshots cover account/grant/session/refresh/audit state. Direct replay
checks revoked family plus replay audit in a separate JDBC transaction and rejects
the successor. Existing replay/concurrency methods remain untouched and mandatory.
Normal Session-owned Login/Refresh/Logout positive controls remain in scope.

Strict report inventories now require24 Session unit identities and23 Session PG
identities (128 total existing Grant/Provision/Outbox/Activation/Session PG methods).
H2 synthetic inventory totals354 pass/17 skip; PostgreSQL expected inventory totals
532 pass/5 skip. These fixture expectations are NOT observed PostgreSQL execution.
Every new identity is covered by existing missing/renamed/skipped/failure/error
negative report-policy controls; no skip/inventory/privacy relaxation was added.

### Actual local execution and limitations

| Gate | Actual result |
| --- | --- |
| Direct Maven3.10.0 / Java21.0.10 version preflight | PASS |
| Offline direct Maven verify, synthetic user/global settings, allowlisted child environment | exit0 / BUILD SUCCESS |
| Strict H2 report guard, fresh reports only | PASS: discovered371, executed354, passed354, failure0, error0, expected skips17 |
| Session unit, including all4 new ownership guards | 24/24 PASS |
| Architecture / negative fixtures | 23/23 PASS (11 rules +12 fixture cases) |
| Invitation Activation unit | 12/12 PASS |
| Domain unit | 48/48 PASS |
| Grant authority / Realm policy unit | 9/9 and8/8 PASS |
| Strict Python report-policy controls | 116/116 PASS |
| Python migration source suite | 15/15 PASS |
| Python invitation/outbox source suite | 9/10 methods PASS; one method has2 failed subcases (V3/V4 working-tree raw-byte pins) |
| New PostgreSQL Java compilation | PASS; all5 new methods present in compiled class |
| Current-source PostgreSQL execution, replay/concurrency, cleanup/privacy runtime | UNVERIFIED: Docker CLI absent from PATH and checked installation paths |
| Current-source GitHub CI / PR integration | NOT RUN; separate owner-authorized checkpoint required |

The two raw-byte subcase failures are pre-existing Windows checkout line endings:
V3/V4 bytes equal the pre-edit fingerprints; HEAD Git blobs match the immutable
pinned hashes; CRLF-to-LF projection of working bytes equals those exact HEAD
blobs. This attribution does NOT turn the failed raw-byte assertions into PASS.
No migration bytes, expected pins, source guard or Git configuration were changed.

The initial local command invocation exited1 before test reports with no captured
compiler diagnostic. Corrected cmd.exe quoting was verified by direct --version,
then the offline verify above succeeded in43.7s. No raw Maven output was persisted
by the launcher; only sanitized status/count projections were retained. Maven's
ignored raw Surefire reports stay local/private, never proposed for publication.
No PostgreSQL services or Testcontainers resources were started locally.

Commands executed: python -B -m unittest discover -s scripts/ci -p test_verify_reports.py;
direct verified Maven3.10.0/bin/mvn.cmd -o -B -ntp -s <synthetic-settings>
-gs <synthetic-settings> -Dmaven.repo.local=<resolved-local-cache> -Dstyle.color=never
-DargLine=<bounded-heap-and-session-temp> verify; python -B scripts/ci/verify-reports.py
--source <fresh-private-reports> --output <sanitized-output> --mode h2 --maven-exit 0.
The two static Python suites were executed through unittest in memory; output
records test/method/subcase identities without raw assertion diagnostics.

Private ignored evidence: .demo/session-transaction-ownership-fix/ contains
pre-edit tracked SHA-256 manifest, local status/results, outbox hash attribution,
fresh raw reports and sanitized summaries/XML. Raw reports are not public evidence.
Preflight confirmed clean Session index/tree and matching local/remote HEAD before
patch; PR23 merge is an ancestor. Six original Foundation fingerprints and the
Activation worktree remain protected; final checks are recorded privately.

Verdict: T_002_05_SESSION_TRANSACTION_OWNERSHIP_FIXED_VERIFICATION_PENDING.
Source guard and local Java/H2 coverage are implemented/verified; PostgreSQL
ownership, durable replay and concurrency still require current-source runtime
verification. Historical18 Session PG PASS cannot establish this new fix. A new
owner-authorized PostgreSQL CI checkpoint is required before PR-readiness review.
T-002-05 stays PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. No further Identity/business slice
or Foundation acceptance is implied. Original requirements and AC are unchanged.


## Session transaction ownership CI verified closure - 2026-10-23

Owner authorization: OWNER_SESSION_SECURITY_CI_CLOSURE: CONDITIONAL_GO.
This checkpoint supersedes ONLY the PostgreSQL verification-pending conclusion
of the preceding transaction ownership correction section. It preserves that
historical local result and the original Session implementation evidence.

### Tested revision and actual CI

Workflow: Backend verification (ID383888734),
[run39986298097](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39986298097),
attempt1, event workflow_dispatch, branch feature/002-identity-session-application.
Tested source SHA: c851f5dc73809dafd3b22c4bf3cd81ccdcc1de17.
Fresh GitHub run/job metadata and each job's actual checkout-log projection agree
on this exact SHA. All three jobs completed SUCCESS:

| Required job | Job ID | Discovered | PASS | FAIL | ERROR | Expected skips |
| --- | --- | --- | --- | --- | --- | --- |
| Architecture gate | 120526250050 | 23 | 23 | 0 | 0 | 0 |
| Backend default / H2 | 120526250627 | 371 | 354 | 0 | 0 | 17 |
| Backend PostgreSQL 18 | 120526250490 | 537 | 532 | 0 | 0 | 5 |

Exact sanitized JUnit method identities match the strict finite inventories;
no unexpected skips. H2 skips are8 PostgresFoundationTests,4 PostgresHealthSafetyTests
and5 FoundationDependencyTests. PostgreSQL skips only the5 Redis/RabbitMQ
FoundationDependencyTests: they are NOT remote Redis/RabbitMQ runtime PASS.
Java compilation and current-source Maven verification completed successfully.

### Transaction ownership security regression evidence

Session unit:24/24 PASS in H2 and PostgreSQL, including4/4 new ownership controls:
- storeRejectsActualAmbientTransactionBeforeJdbcAccess
- storeRejectsAmbientSynchronizationBeforeJdbcAccess
- storeRejectsBoundDataSourceBeforeJdbcAccess
- storeRejectsUnknownBoundResourceBeforeJdbcAccess

Five new methods executed against real PostgreSQL and PASS:

| PostgreSQL method | Verified outcome |
| --- | --- |
| sessionAmbientRefreshRejectedBeforeRotationAndCallerCommit | Same application runtime DataSource and real JdbcTransactionManager, caller READ_COMMITTED; ownership rejection before rotation; independent post-commit snapshot unchanged; direct refresh positive control succeeds |
| sessionAmbientReplayRejectedBeforeCallerRollback | Consumed-token call inside caller transaction raises ownership failure, not REFRESH_REPLAY; caller explicitly rolls back; independent state unchanged; current successor remains usable until a genuine direct replay |
| sessionDirectReplayRevocationAndAuditAreIndependentlyCommitted | Direct rotation succeeds; old-token replay returns REFRESH_REPLAY; a separate JDBC transaction observes revoked session/family and required audit; successor rejected |
| sessionAmbientLoginRejectedBeforeCallerCommit | Caller transaction rejected before Session writes; independent state unchanged after commit; normal Session-owned Login succeeds |
| sessionAmbientLogoutRejectedBeforeCallerRollback | Caller transaction rejected without taking over rollback; independent state unchanged after caller rollback; normal Logout revokes family and remains idempotent |

The tests use the actual application Session JDBC composition, the SAME runtime
DataSource instance and real JdbcTransactionManager/TransactionTemplate. Snapshot
assertions after outer scope completion use independent runtime connections.
The direct replay test starts a new READ_COMMITTED JDBC transaction to verify
committed revocation and audit, rather than asserting inside the revoked operation.
Session does not commit, roll back or suspend any caller-owned transaction.

Both existing critical methods also executed and PASS:
- sessionReplayCommitsFamilyRevocationBeforeDenial
- sessionConcurrentRefreshHasOneRotationThenReplayRevocation

Previous123 PostgreSQL methods:123/123 PASS. Relevant combined suite:128/128 PASS,
including23/23 Session PostgreSQL cases (previous18 plus5). Session unit24 and
these PostgreSQL figures overlap the job totals above; do NOT add them as separate
executions to inflate532. Domain48, Migration25, Crypto10, Grant/Realm, Provisioning,
Outbox and Invitation Activation inventories remain present and PASS, including
existing transaction, audit rollback, authority and concurrency regressions.

### Privacy, report policy and artifact integrity

116 report-policy controls per job PASS. Actual job checkout, report-policy and
fixed security-status messages were projected from one aggregate log per job;
per-step duplicates were excluded, not counted as additional executions.
Temporary database credential log audits PASS in all three jobs. PostgreSQL's
generated Testcontainers credential privacy audit and owned-resource cleanup audit
PASS; cleanup removed only the recorded disposable CI PostgreSQL container.

Three sanitized ZIPs were downloaded with GitHub CLI and all3 SHA-256 values
matched available GitHub metadata. ZIP CRC and exact entry allowlist PASS:
only sanitized summary.json and tests.xml, with fixed skip text; no raw diagnostics,
properties, session credentials or runtime log files in published artifacts.

| Artifact | GitHub artifact ID | Verified SHA-256 |
| --- | --- | --- |
| backend-architecture-1 | 12514933948 | 5c3fdeab4eb3c731ea0d0bcf60e06734799734ff9309ca84cfc3af553b43a374 |
| backend-h2-1 | 12515233796 | 21d638248898dd81814bad039ab121588abcaf494d60b6e3c6e354228c626ddd |
| backend-postgres-1 | 12515917895 | 8b780cf372b645acc7f8e4bc4af014b2bc58c74e302981cd9f8e6341fb629979 |

Private retained evidence: .demo/session-transaction-ownership-ci/ contains
sanitized summaries/XML, artifact integrity, exact method reconciliation and fixed
checkout/privacy/cleanup projections. CI logs and run-log archive were processed
only in memory; raw Maven diagnostics, passwords, tokens and sensitive logs were
not copied to SDD documents. API job-log access was unavailable; the official
run-log archive supplied the verified projections without persisting raw logs.

### Specific finding disposition and outstanding boundaries

Previous blocker: T_002_05_SESSION_PR_SECURITY_BLOCKED, specifically the shared
REQUIRED callback completion versus physical-commit gap. A compatible outer
transaction could otherwise roll back replay revocation after outward denial.
The approved JdbcIdentitySessionStore-specific fail-closed guard rejects actual
transactions, synchronization or ambiguous bound resources before shared-store
participation; no shared Grant propagation or public HTTP contract change.

This specific finding now has passing real PostgreSQL transaction-ownership,
independent-connection replay durability and concurrency regression evidence.
Verified checkpoint: T_002_05_SESSION_TRANSACTION_OWNERSHIP_CI_VERIFIED.
It is ready for final PR-readiness reassessment; this is not completion of all
Session security work, PR integration verification or production deployment.

Original Session run39932569608 remains historical for sourcecaa150fb3c1a2c4ac1a60d27566f07f6a3c3c479.
The security correction is tested by run39986298097 on sourcec851f5dc73809dafd3b22c4bf3cd81ccdcc1de17.
This closure is a LATER docs-only commit: neither run is execution evidence for
that documentation revision. Its exact docs SHA and push result are recorded in
the publication handoff/private checkpoint; no self-referential commit hash is
invented in tracked Markdown. No new CI dispatch/rerun, PR or merge is authorized.

Keep historical Maven Wrapper bootstrap attribution OPEN / NOT REPRODUCED:
this successful clean-runner bootstrap does not prove the old root cause fixed.
Current CI does not invoke the Python migration/outbox source suites. Windows
outbox source guard remains9/10 methods PASS with2 failed V3/V4 CRLF raw-byte
subcases; pinned canonical LF blobs match and working bytes remain unchanged.
V1-V5 canonical Git identities and accepted integrity expectations are preserved;
no SQL edit, hash-pin replacement or migration repair was introduced.

T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains. Pending: current-session/activity,
recovery, resend, lifecycle/query, remaining authority/security and HTTP/JWT/cookie/
CSRF/throttling adapters. Delivery/provider integration stays T-002-08 after its
dependencies. Feature001/D1-08/Day1 remain PARTIAL. No full Production Identity,
end-to-end business capability or business-module implementation approval implied.

Closure scope is exactly this file and the
[supplemental security CI trace](../quality/feature-002-recovery-traceability.md#session-transaction-ownership-ci-verified-trace---2026-10-23).
Protected main traceability, all Java/tests/SQL/CI policies, six original Foundation
fingerprints and the clean Activation worktree remain unchanged by this closure.
