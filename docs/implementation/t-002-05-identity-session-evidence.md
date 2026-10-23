# T-002-05 Identity Session Application checkpoint - 2026-10-23

Status: **T_002_05_SESSION_APPLICATION_IMPLEMENTED_VERIFICATION_PENDING** under
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
