# T-002-05 — Production Identity Application: first slice

Latest checkpoint: **T_002_05_FIRST_SLICE_CI_VERIFIED** on source
1698940e4d4d888993daa2e38e995014c4f3282b, run39641670408 attempt1.
[Actual CI verification and remaining scope](#t-002-05-first-slice-actual-ci-verification--2026-10-22).
Full T-002-05 remains PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED.

## Historical source-only checkpoint — 2026-10-21

Checkpoint: 2026-10-21. Authority: OWNER_T_002_05_APPLICATION_IMPLEMENTATION: GO.
**T_002_05_IMPLEMENTED_VERIFICATION_PENDING** for the first bounded
`replaceIdentityGrants` slice. Full T-002-05 remains PARTIAL, checkbox open.
Java compilation, ArchUnit, PostgreSQL/JUnit execution and new remote CI are
**UNVERIFIED**, not inferred from authored tests or Python fixture results.
Feature002/AC-002-01..05 remain NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## Authority, baseline and readiness

- Branch feature/002-identity-domain-ci; unchanged HEAD
  `14988796a97ddce8d7223dac5d8bc8b8e60e4f68`; initial index empty.
- T-002-01..04 DONE at their accepted checkpoints. Domain source run39626192090
  attempt1 tested bbb1098, not this Application working tree. No current Java
  result is claimed from that run.
- [Canonical task](../../specs/002-identity-access/tasks.md): implement use cases,
  port, public module API and workflow, with role update/assignment and audit in
  one Identity transaction, never update order/finance. DoD verbatim:
  **Transaction consistent; không import internals module khác; integration test pass.**
- [DoR](../codex/definition-of-ready.md), [feature design](../../specs/002-identity-access/design.md),
  [ADR-0009 / DEC-03, DEC-04, O1–O7](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
  [accepted contract semantics](../../contracts/identity-access/README.md),
  [Domain handoff](t-002-04-identity-domain-evidence.md#t-002-05-application-handoff),
  [test/feature DoD](../quality/test-strategy.md) bound this implementation.
- Missing business-owner integrations are isolated behind a deny-by-default
  port. No provider choice, scope catalog or permission fallback is invented.
  Other Identity operations and HTTP/security wiring are outside this slice.

## Source mapping and dependency direction

| Responsibility | Implementation / existing policy | Boundary |
|---|---|---|
| Backend public command/result | `identity.api.IdentityGrantCommands`, `IdentityCommandFailure` | Accepted grant/account fields, safe contract codes; no internal types or credential hashes |
| Orchestration | `identity.internal.application.ReplaceIdentityGrants` | Implements public command, depends on Domain and inward ports, not JDBC or another module |
| Trusted owner authority | `application.port.GrantAuthority` | In-process trusted implementation only; absent provider denies, caller cannot submit authorities |
| Transaction/persistence port | `application.port.IdentityGrantStore` | One callback transaction with account, session, receipt, audit and revocation operations |
| PostgreSQL adapter | `infrastructure.JdbcIdentityGrantStore` | JDBC runtime-role DML, Spring transaction coordination; no schema/migration changes |
| Existing authorization and versions | `IdentityAccount.replaceGrants`, `ManagementAuthority.requireManagement`, `AccountVersion.next` | Reuses full-target/union/self/last-admin/version policies |
| Existing session admission | `IdentitySession.admit` | Current account status/authVersion, revocation, idle/absolute deadlines |

The only Domain edit makes existing `ManagementAuthority.requireManagement`
public within the internal Domain package API, so Application can invoke the
same guard before receipt disclosure without projecting a fake mutation or
fake administrator count. Its body/invariants are unchanged. No public module
API exposes this internal class. Domain regressions must run again in CI.

Composition is explicit via constructors. No new component/profile/controller is
registered into Demo, H2 or production startup. Future HTTP/security adapters
must authenticate a production session before invoking the backend command;
passing an arbitrary session UUID from an HTTP request is not authentication.
The use case independently reloads the session/account and takes no actor grant
claims. Demo AccountQueries/AccountAccess/TokenService/seed/RBAC are untouched.

## Transaction and security effects

1. Start/join a JDBC transaction at READ COMMITTED, validate compatible joined
   transaction isolation/read-only state. Transaction budget15s; SET LOCAL
   lock timeout5s and statement timeout10s. No global configuration change.
2. Acquire fixed Identity-local transaction advisory lock `0x4944454e54495459`.
   Lock/read current actor account, actor session and target account. This
   serializes competing grant commands before administrator population counts.
3. Admit the current session; resolve owner authority through the trusted port;
   call the SAME Domain guard over current AND requested grants. Missing owner,
   unknown scope/role, incomplete manageable/assignable sets fail closed.
4. Validate accepted nonempty unique replacement, reason1..300 Unicode code
   points, well-formed UTF-8, positive expected version and command/correlation
   UUIDs. HTTP missing/malformed If-Match parsing belongs to T-002-06.
5. Recheck current session deadlines after target lock/authority resolution.
   Check authorized actor/operation/key receipt BEFORE stale-version mutation
   preconditions. Fingerprint length-prefixed UTF-8 fields with SHA-256: exact
   target, original version, reason and sorted exact role/scope triples. Grant
   ordering and a new correlation ID do not change command identity.
6. For a new command, use actual locked administrator population and Domain
   replacement; conditional account update increments version AND auth_version;
   delete/reinsert complete grants; revoke all unrevoked target sessions AND
   refresh tokens; append audit and bound receipt, then return safe account view.
7. Any mandatory write failure propagates and rolls back the transaction. No
   automatic retry, independent best-effort write, external mail/network call,
   Redis lock, new SQL schema or mutable audit/receipt is introduced.

| V3 table | Effect |
|---|---|
| identity.user_account | Current account locks; conditional version/auth_version increment, updated_at |
| identity.account_grant | Read complete target grant set, replace atomically |
| identity.auth_session | Current actor admission; revoke all target sessions |
| identity.refresh_token | Revoke target refresh family members |
| identity.audit_entry | Append actor/target/reason/correlation/time for successful replacement |
| identity.command_receipt | Append ACTOR binding, operation/key, fingerprint, target, original/result version, status200 and retention timestamps |

Current implementation requires explicitly supplied retention of at least24h;
this conservative implementation choice needs owner review before production
composition, not a newly accepted grant-retention policy. An attempted change
to remove that lower bound was rejected by automatic approval review as a
potential weakening of replay/idempotency protection; it was NOT applied.
The existing source guard is retained. There is no default
deployment config or expiry-based key reuse. Existing receipts remain bound,
even after expires_at, until an independently approved retention policy exists.
No raw request/credential/response cache. Replay returns original appliedVersion
plus a freshly authorized CURRENT account view, with replay metadata internal
to this backend module API. It does not claim byte-identical historical contact
data or HTTP/ETag replay acceptance; later HTTP adapter tests must verify the
accepted wire behavior against this separation.

Self-management is denied. A disabled target can have its grants replaced but
stays disabled, matching existing Domain/contract (grant PUT has no new lifecycle
error); it never regains sessions. Admin does not acquire Finance permission.
Concurrent opposite admin removals must leave one administrator because the
loser's authoritative account/session has been changed/revoked before its turn.
This is an IMPLEMENTED lock protocol with AUTHORED tests, not a runtime proof.
Future disable/provision/other population-changing commands must acquire the
same advisory lock before row locks; no concurrency guarantee for unimplemented
paths, arbitrary direct SQL or missing owner-authority integrations is claimed.

## Authored tests and requirement trace

New `IdentityGrantApplicationTests`: **33 plain @Test methods**, @Tag("task-c"),
no @Disabled. Uses the existing approved immutable PostgreSQL18 Testcontainers
fixture, random localhost port, synthetic per-run credentials, a fresh database
and owner/migrator/runtime roles per method, V1/V2/V3 cold-start and runtime-role
DML. No developer database/credential fallback. JDBC resources close before
container teardown. Authority fixtures are synthetic trusted in-process inputs,
not production owner integrations or accepted production principals.

| Test coverage group | Representative identities | Trace | Runtime |
|---|---|---|---|
| Multi-role/multi-scope, stored account/grants/version/audit/receipt | authorizedReplacementPersistsMultipleScopesVersionsAuditAndReceipt | REQ-002-01/04, AC-002-01 | UNVERIFIED |
| Session/refresh invalidation and current admission | allTargetSessionsAndRefreshTokensAreRevokedWithoutTouchingActor; disabledActorCannotSubmitCommand; expiredSessionAndChangedAuthVersionAreRejected | REQ-002-02/05, AC-002-04/05 | UNVERIFIED |
| Full-target current/requested union, hidden grants, no provider/unknown scope, self/last-admin | partialAuthorityCannotRemoveHiddenCurrentGrant; requestedGrantOutsideAssignableScopeIsDenied; missingOwnerProviderDefaultsDenyEvenForEmptyTarget; selfGrantManagementIsForbidden | REQ-002-01/02/04, AC-002-02/04 | UNVERIFIED |
| Authorized replay/canonical set, conflicting payload/target/version/reason and current authority | authorizedReplayPrecedesStaleVersionAndDoesNotRepeatEffects; sameKeyChangedExpectedVersionConflictsBeforeStaleCheck; lostManagementAuthorityCannotReplayReceipt | REQ-002-04/05, AC-002-03/05 | UNVERIFIED |
| Duplicate/empty/malformed/invalid input, absent target and disabled-target preservation | duplicateGrantsAreRejectedWithoutWrites; invalidCommandMetadataDoesNotMutateState; disabledTargetStaysDisabledAndDoesNotRegainSessions | REQ-002-01/02, AC-002-02/05 | UNVERIFIED |
| Real database fault rollback, compare all six tables from new reads | auditWriteFailureRollsBackAllSecurityEffects; receiptWriteFailureRollsBackAuditAndAllSecurityEffects; refreshRevocationFailureRollsBackSessionAndGrantWrites | REQ-002-04/05, AC-002-02/05 | UNVERIFIED |
| Two concurrent transactions, lost-update prevention, same-key exactly-one mutation, last admin | concurrentVersionsProduceOneMutationAndOneStaleResult; concurrentSameKeyCommandsCreateOneReceiptAndOneAudit; concurrentOppositeAdminRemovalCannotRemoveLastAdministrator | REQ-002-01/04/05, AC-002-03/05 | UNVERIFIED |
| Result shape/redacted representations | resultContainsOnlyAcceptedAccountFieldsAndRedactsToString | REQ-002-02/03, AC-002-04 | UNVERIFIED |

Fault triggers are created ONLY by the test migrator/owner inside its fresh
synthetic database, never by production runtime. Concurrency uses two worker
threads with bounded latch/future waits and executor shutdown; unexpected
exceptions fail tests, not converted to permitted outcomes.

## Strict CI integration and process-safe results

- Finite33 class/method identities added to verify-reports.py, PostgreSQL-only,
  no allowed skips. Missing, renamed, failed, skipped or extra invocations fail.
- Exact new `identity-grants` suite registration in existing Java lifecycle
  manifest and Python guard/independent fixtures. Only metadata inventory changes;
  ownership/session/image/socket/cleanup/privacy logic remains unchanged. A
  third dedicated container/volume must be observed, verified and absent after
  teardown; missing/failed suite blocks cleanup acceptance before Docker checks.
- No workflow/job/ruleset/POM/dependency/image changes. Existing three required
  check names are preserved. H2 excludes the Task-C suite instead of counting
  it as valid skipped tests.
- EXPECTED future inventories: Architecture23 PASS/0 skips; H2279 PASS/17 skips;
  PostgreSQL351 PASS/5 skips (356 discovered). These are not executed results.

| Actual command/check | Result | Exit / limitation |
|---|---|---|
| Initial python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v | 96/97 PASS,1 FAIL: old PostgreSQL318 total vs required351 |1; corrected finite expected total, no bypass |
| Final python -B -m unittest discover -s scripts/ci -p 'test_*.py' |97/97 PASS,30.711s |0; synthetic policy fixtures, NOT Java execution |
| Source boundary/contract inventory, UTF-8, relative links, disclosure, hashes, diff check | See final source verification record below | Process-safe only |
| Java compile/JUnit/ArchUnit/PostgreSQL/transaction fault/concurrency | UNVERIFIED | Not run; current task has no fresh local JVM/Docker or CI dispatch authorization |

Four new policy controls cover exact source inventory and PostgreSQL discovery,
every missing method, skip/failure/error/rename rejection, and diagnostic
redaction. Original controls remain active, including negative fixtures, S2,
seed, migration14, Domain48, credentials and owned cleanup. Synthetic lifecycle
fixtures now give three DISTINCT PostgreSQL container/volume pairs with one
shared Ryuk identity; no safety assertion is relaxed.

## Git safety and next verification handoff

Only this evidence, tasks and supplemental Feature002 traceability change as
SDD records. Protected main traceability and five other preexisting Foundation
files remain byte-identical; T-002-03 evidence remains byte-identical. Accepted
OpenAPI/ADR, immutable V1/V2/V3, POM, Demo, frontend, CI workflow and developer
services/credentials/ACL are untouched. No branch change, stage, commit, push,
PR, merge, workflow dispatch, Java, Maven or Docker execution.

Next authorized runtime should verify exact source fingerprints, then compile
and run the existing Architecture/H2/PostgreSQL workflow on this revision,
including all33 new tests, migration14, Domain48, strict reports/expected skips,
credential privacy and three-suite Testcontainers cleanup. Until actual success,
no transaction/concurrency guarantee or current CI PASS is claimed.

Remaining canonical Application scope includes provisioning/invitation and
lifecycle orchestration, login/session/refresh/logout/recovery use cases and
safe account/role/scope queries as scoped by the accepted contract. Real owner
authority adapters, transaction coordination for all population-changing paths,
deployment retention/composition and later HTTP/authentication mapping still
need their respective bounded implementations/reviews. Do not auto-start them
or mark full T-002-05/Feature002 accepted from this first slice.

## Final process-safe verification and fingerprints

Static source checks PASS, exit0: public Account/Grant field sets match the
accepted OpenAPI; no cross-module internal imports, public-internal leak or
Application-to-JDBC dependency was found; production SQL names only the six
owned Identity tables. Domain authorization guard body unchanged. Existing
collector function ASTs and all preexisting required test identities unchanged.
UTF-8 and bounded disclosure scan PASS;36 relative links/anchors PASS; all ten
canonical task entries including checkbox/DoD/REQ/AC bodies unchanged.
The initial SQL-name inspection regex included Java package imports; after
restricting it to SQL FROM/UPDATE/INTO/JOIN references, the six-table check PASS.
This was an inspection-script correction, not a production-code change.

Working diff:14 task paths (11 Java/test/policy,3 SDD); six preexisting
Foundation paths remain separate. Source/runtime compatibility is UNVERIFIED.

| Current source/test/policy path | SHA-256 |
|---|---|
| app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java | `4e106edaf2507d99a7a1fe31a38d33dc6ae10a25f570d7428f337a5f6cb0f7d0` |
| app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityGrantCommands.java | `ece3d07b6c42830ec724872125bfa335570cf02c840b0170a5c1d5264c08b1be` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ReplaceIdentityGrants.java | `38391aada8a3ac2606f6d91202e717cedd0ae28f16119af354373eb45ab7c592` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/GrantAuthority.java | `b516cb4923b58ed0a4b3f652293df0eb1e7b04beddf852637b8cbb20620e1964` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentityGrantStore.java | `9ffa3e7d6d6957a4caf5ef5beaafcb6316ffbc9d6bb9630c6b89941919391255` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityGrantStore.java | `bf96bb21c849c8d0dd4e69dddd7378448f23ec0e5759a2ca5122439ea3c62065` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java | `edca9014d8ccacdbe0345b7afe7596470180c61afd6c240a848e4fb5b06adafa` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/domain/ManagementAuthority.java | `880e8a6acdc0ecf4ad2af89640cae2d4918288af16842b741d448c1d42450119` |
| app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java | `00c07013537275fb5a9cd702d17aaf690f718da0d922424717e9bdd362ec62a2` |
| scripts/ci/test_verify_reports.py | `d2381ce75ce8249ed2a5c8e3d085cf4a7105bdb00188407d0db5ce009670e4f5` |
| scripts/ci/verify-reports.py | `d497b87209c278233425d936e9223cbaa46df739b551d98916638208577ae20b` |

Protected baseline fingerprints below matched before/after this task. The
T-002-03 record is already committed at baseline, with no outstanding edit.

| Preserved path | Before = after SHA-256 |
|---|---|
| docs/quality/traceability.md | `0e992a189ff22e0876d3f7616890b15cf23188abf0e6723022696e6b3bc89eb9` |
| docs/implementation/ac-001-04-s3a-r1-evidence.md | `668733d05374d4216eb5cbce16b6f16d11aaa1069a1c7a7a8f89863daa410658` |
| infra/verification/fail_fast.py | `ec349558c7aa197663bc5a6ea88e139069a5d4359f164faa2d065ba24c095524` |
| infra/verification/maven_h2.py | `42198a9be515196b3e3acce57967335ccb8414a98859b633c744345cb0e349eb` |
| infra/verification/test_fail_fast.py | `c80d775d8f72351744e30cf49f0ce7660bf875f5fe7c71eee278b1965b80d0dc` |
| infra/verification/test_maven_h2.py | `85a5bc2c0189f9b7b11a1fed98b80bd11561ef5f34f9149748a7005766c03dd3` |
| docs/implementation/t-002-03-identity-migration-evidence.md | `40d7fcb34b73afaaf604e863cf315b2e042b8c16de44be918cf88215978c9c5b` |

## T-002-05 first slice actual CI verification — 2026-10-22

**T_002_05_FIRST_SLICE_CI_VERIFIED** for replaceIdentityGrants ONLY.
OWNER_T_002_05_FIRST_SLICE_CI_APPROVAL: CONDITIONAL_GO gates are satisfied.
Full T-002-05 remains PARTIAL and unchecked. Feature002/AC-002-01..05 NOT
ACCEPTED; Feature001/D1-08/Day1 PARTIAL. This supersedes historical pending
statements only for the exact first-slice gates verified here.

### Review, publication and provenance

- Preflight branch feature/002-identity-domain-ci, HEAD1498879, index empty;
  exact14 task paths plus six protected Foundation dirty paths. All18 evidence
  fingerprints matched. No concurrent source change.
- Fresh process-safe command: python -B -m unittest discover -s scripts/ci
  -p 'test_*.py':97/97 PASS,31.227s, exit0. Source/import/SQL/contract/UTF-8/
  disclosure/link checks PASS,36 relative links. Existing report functions and
  required identities unchanged; Java writer differs only in exact suite map.
- Review found no blocking source defect for this bounded slice. Current
  persisted account/session and full-target/current-requested union guards
  precede receipt disclosure; authorized receipt precedes stale non-replay
  precondition. Existing24h minimum retained; no reduction was applied.
- Staged diff check found one surplus blank EOF line in this NEW evidence
  document (the earlier unstaged diff check did not include untracked files).
  Removed only that empty line; staged diff check then exit0. No source or
  assertions changed in this CI checkpoint.
- Created feature/002-identity-application-ci from verified published Domain
  closure14988796a97ddce8d7223dac5d8bc8b8e60e4f68 without stash/reset/worktree.
- Application commit `1698940e4d4d888993daa2e38e995014c4f3282b`, sole parent
  `14988796a97ddce8d7223dac5d8bc8b8e60e4f68`, message
  feat(identity): implement transactional grant replacement.
  Exactly14 paths,992 insertions/13 deletions; full staged patch reviewed;
  staged blobs matched reviewed bytes; no Foundation/private artifact included.
- One normal push exit0; independently checked remote Application SHA exact.
  Domain closure/Migration refs unchanged. PR20 was already MERGED at current
  preflight; its head branch remained bf80d84, not modified by this task.
  Remote develop baseline8cc201da66d6551eca2bddc294e2e65d618f1f7b was preserved.
- Authenticated GitHub account phamquoainam, push permission confirmed;
  Application branch absent before creation and not covered by active
  develop-only ruleset25049940 or classic main/develop protection.
  Existing workflow blob667bf2251ba3b9b4b21685cdf5f306d2f1c82d95 unchanged.
  No settings change, bypass, PR creation or merge.
- Exactly ONE native dispatch, HTTP204, registered workflow383888734.
  [Run39641670408 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39641670408),
  event workflow_dispatch, branch feature/002-identity-application-ci,
  completed/SUCCESS. ALL THREE job consoles independently show exact checkout
  `1698940e4d4d888993daa2e38e995014c4f3282b`. No synthetic PR merge SHA or post-merge develop run is claimed.
  No local Java/Maven/Docker, source fix or workflow rerun occurred.

### Exact published manifest

```text
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityGrantCommands.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ReplaceIdentityGrants.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/GrantAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentityGrantStore.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/domain/ManagementAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityGrantStore.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java
app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java
docs/implementation/t-002-05-identity-application-evidence.md
docs/quality/feature-002-recovery-traceability.md
scripts/ci/test_verify_reports.py
scripts/ci/verify-reports.py
specs/002-identity-access/tasks.md
```

### Actual CI jobs and sanitized inventories

| Job | Job evidence | Discovered | Executed / PASS | FAIL / ERROR | Expected skips |
|---|---|---|---|---|---|
| Architecture gate | [119366514250](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39641670408/job/119366514250) |23 |23 |0 /0 |0 |
| Backend default / H2 | [119366514927](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39641670408/job/119366514927) |296 |279 |0 /0 |17 |
| Backend PostgreSQL 18 | [119366514741](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39641670408/job/119366514741) |356 |351 |0 /0 |5 |

Actual Java compile/backend verify,11 production architecture rules and12
negative/positive-control cases PASS. All48 existing Domain methods PASS in
BOTH H2 and PostgreSQL. PostgreSQL migration14/14, Foundation rerun regression,
all8 PostgresFoundationTests and4 PostgresHealthSafetyTests PASS. Counts come
from downloaded sanitized reports, not the previous forecast.

H2 skips exactly8 PostgresFoundationTests +4 PostgresHealthSafetyTests +5
FoundationDependencyTests. PostgreSQL skips exactly5 FoundationDependencyTests.
Task-C suites are excluded from H2 discovery rather than counted as valid skips.
No Redis/RabbitMQ remote runtime coverage is claimed. No prohibited new skip.

### Artifact identity and publication/privacy boundary

Each GitHub ZIP digest matched SHA-256 computed over its actual downloaded
bytes. Metadata binds same run/attempt/branch/source SHA. Each ZIP contains
exactly summary.json and tests.xml, no symlink/encrypted/extra entries; bounded
uncompressed sizes checked. Summaries have PASS/problems=[] and agree with XML
suite attributes, totals and exact finite class/method/outcome inventory.
Only known testcase identities and fixed sanitized outcome text were accepted.

| Artifact | ID | Actual verified ZIP SHA-256 |
|---|---|---|
| backend-architecture-1 |12365392978 |`0dff0fafbdf616fb287cf9c65bcda1637edc5dbb39f0e628855ce98fb533ec04` |
| backend-h2-1 |12365363537 |`224d5ae295e926b13be5bec70e2784a1cbec624f24dde6aea8277cc8efce5e93` |
| backend-postgres-1 |12365143730 |`986ce2dec4d22f3963d5aa7148f957767fcbc81ef30137e4363a26561af69a0f` |

Actual console evidence shows97 policy controls plus OK in EACH job;
verify/discovery and artifact upload steps SUCCESS. Temporary database
credential audit PASS in all three jobs. PostgreSQL generated Testcontainers
credential privacy and complete owned cleanup audits PASS. Separate temporary
PostgreSQL harness cleanup step SUCCESS. Task-C runtime checks are
NOT_APPLICABLE in Architecture/H2, not claimed executed there.

Logs were projected in memory to checkout SHA, numeric policy count and fixed
guard statuses only. An initial timestamp-stripping regex in the read-only
inspection failed to extract checkout/count lines; correcting that inspection
regex confirmed all three exact SHAs and97 counts, with no source/runtime rerun.
Raw private Maven diagnostics, credentials and signed artifact URLs were not
published or used as test evidence. Downloads did not forward GitHub
Authorization to redirected storage URLs.

### Exact33 Application outcomes

Every identity below appears as passed in PostgreSQL sanitized XML on the
tested Application SHA; none skipped, failed or errored.

| IdentityGrantApplicationTests method | Actual PostgreSQL |
|---|---|
| authorizedReplacementPersistsMultipleScopesVersionsAuditAndReceipt | PASS |
| allTargetSessionsAndRefreshTokensAreRevokedWithoutTouchingActor | PASS |
| authorizedReplayPrecedesStaleVersionAndDoesNotRepeatEffects | PASS |
| reorderedGrantSetHasSameCanonicalReceiptFingerprint | PASS |
| sameKeyChangedPayloadConflictsWithoutWrites | PASS |
| sameKeyChangedTargetConflictsWithoutWrites | PASS |
| sameKeyChangedExpectedVersionConflictsBeforeStaleCheck | PASS |
| sameKeyChangedReasonConflictsWithoutWrites | PASS |
| revokedActorSessionCannotReplayReceipt | PASS |
| lostManagementAuthorityCannotReplayReceipt | PASS |
| missingOwnerProviderDefaultsDenyEvenForEmptyTarget | PASS |
| partialAuthorityCannotRemoveHiddenCurrentGrant | PASS |
| requestedGrantOutsideAssignableScopeIsDenied | PASS |
| nonManagementActorCannotUsePermissiveOwnerFixture | PASS |
| selfGrantManagementIsForbidden | PASS |
| lastAdministratorCannotRemoveItsOwnManagementGrant | PASS |
| staleNonReplayVersionIsRejectedWithoutWrites | PASS |
| duplicateGrantsAreRejectedWithoutWrites | PASS |
| emptyReplacementIsRejectedByAcceptedContractBoundary | PASS |
| malformedScopeIsRejectedWithoutWrites | PASS |
| unknownResourceScopeIsNotGrantedImplicitly | PASS |
| missingAndUnauthorizedTargetsHaveSameSafeClassification | PASS |
| disabledActorCannotSubmitCommand | PASS |
| expiredSessionAndChangedAuthVersionAreRejected | PASS |
| invalidCommandMetadataDoesNotMutateState | PASS |
| disabledTargetStaysDisabledAndDoesNotRegainSessions | PASS |
| auditWriteFailureRollsBackAllSecurityEffects | PASS |
| receiptWriteFailureRollsBackAuditAndAllSecurityEffects | PASS |
| refreshRevocationFailureRollsBackSessionAndGrantWrites | PASS |
| concurrentVersionsProduceOneMutationAndOneStaleResult | PASS |
| concurrentSameKeyCommandsCreateOneReceiptAndOneAudit | PASS |
| concurrentOppositeAdminRemovalCannotRemoveLastAdministrator | PASS |
| resultContainsOnlyAcceptedAccountFieldsAndRedactsToString | PASS |

These actual tests verify stored multi-role/multi-scope replacement, version/
authVersion increments, target session/refresh revocation, audit/receipt,
current authority, hidden-scope/default-deny/self guards, stale/replay/conflict,
and database-trigger fault rollback across six tables. Two-thread PostgreSQL
tests prove one mutation for competing versions/same key and preservation of
one active administrator for opposite grant removals under THIS lock protocol.
No claim is made for unimplemented lifecycle/population-changing paths until
they participate in the same protocol and have their own integration tests.

### Testcontainers lifecycle acceptance

The new suite owns its own approved-image PostgreSQL container/anonymous
volume, with random localhost port, synthetic credentials and fresh DB/roles
per method. It does not share another suite's PostgreSQL. The exact three-suite
registry derives expected resources; shared Ryuk identity remains explicit.
Per-suite reserved/observed/created/verified/stopped records must all match
current execution/session/ownership/image and distinct resource identities.

Actual cleanup audit PASS therefore covers all three registered suite-owned
PostgreSQL pairs and their shared reaper, with absence and no remaining owned
run labels. Missing/stale/foreign/substituted records still fail closed before
any cleanup audit action. Writer teardown retains ownership/mount/consumer
checks before deletion; no guard logic changed. No local developer Docker,
PostgreSQL, credentials, volumes or ACL were accessed/mutated by this task.

### Acceptance and next Application dependency

- First replaceIdentityGrants application/persistence transaction slice:
  **CI_VERIFIED** on exact Application commit above. Canonical T-002-05 remains
  PARTIAL; its integration-test gate is satisfied for this slice only.
- Production authority integrations remain absent/default-deny. Synthetic
  authority fixtures prove transaction orchestration, not real owner-provider
  availability or production authentication. No public HTTP/API integration,
  JWT/CSRF/cookie admission, historical-response wire/ETag or full Feature002
  acceptance is implied.
- Remaining Application scope: production authority integration where required,
  provisioning/invitation/activation, disable/reactivate, recovery, login/
  session/refresh/logout and safe account/role/scope queries under accepted
  contracts. Future population changes must use the same serialization rule.
- Only three SDD closure files are edited AFTER this run. Those uncommitted
  documentation bytes are NOT part of tested/pushed1698940; no automatic
  second commit/push, new PR or dispatch. All11 source/test/policy fingerprints
  above still match the exact tested commit. Six Foundation files and
  T-002-03 evidence remain byte-identical; index empty after source commit.
- Next bounded implementation should establish trusted production
  grant-authority composition through approved public contracts/ports while
  maintaining default-deny; unavailable owner integrations are a dependency,
  not a reason to invent scope permissions. No next slice implemented here.

## T-002-05-AUTH — bounded production composition — 2026-10-22

Authority: OWNER_T_002_05_AUTHORITY_COMPOSITION: GO.
**T_002_05_AUTHORITY_DEPENDENCY_BLOCKED** for full production owner authority.
The independently valid composition/denial boundary is IMPLEMENTED; all NEW
Java/JUnit/PostgreSQL results are UNVERIFIED. Prior first-slice CI acceptance on
1698940/run39641670408 remains historical and does not cover these new bytes.
Full T-002-05 PARTIAL; T-002-01..04 DONE; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL.

### Exact trust and ownership boundaries

| Boundary | Current authoritative path | This checkpoint |
|---|---|---|
| Actor authentication | Later production HTTP/security adapter must validate JWT/session binding before invoking IdentityGrantCommands | No controller/JWT/CSRF/cookie/Demo-principal adapter installed; a submitted session UUID alone is never authentication |
| Current actor/session | Existing ReplaceIdentityGrants obtains actor ID through auth_session, locks persisted account/session and checks status, revocation, deadlines and authVersion | Reused byte-identically, including repeat deadline admission after target/authority resolution |
| Identity Management entry condition | Existing IdentityAccount.managementAdmin and Domain requireManagement | New wrapper prefilters inactive/non-management state; no new role catalog or automatic Finance permission |
| Target authority/assignability | Existing GrantAuthority port, then ManagementAuthority.requireManagement over complete current/requested grants | Missing/null/failing provider denies; returned snapshots still face the unchanged Domain actor/authVersion/target/version/full-union/self guards |
| Resource ownership | Must come from approved public owner contracts, not account_grant existence or resource existence | No suitable production owner contract currently exists; all unsupported scopes, including IDENTITY, remain denied |
| Atomic business effects | Existing JDBC transaction/advisory lock/row locks/conditional version and receipt protocol | No change to existing use case, adapter, SQL or Domain policy |

### Implemented source and composition path

- New internal Application FailClosedGrantAuthority decorates the EXISTING
  GrantAuthority port. Missing actor/target/requested context, inactive/non-admin
  actor, missing provider, null result or RuntimeException produces no allow
  snapshot. It does not log/rethrow a provider message or synthesize authority.
  VM Errors propagate. Exact immutable requested grants are passed to a trusted
  delegate; the wrapper is not itself an owner decision or authentication API.
- New infrastructure ProductionIdentityGrantComposition.create constructs the
  real JdbcIdentityGrantStore + FailClosedGrantAuthority + ReplaceIdentityGrants
  and exposes only IdentityGrantCommands. The default overload installs no owner
  source, so current persisted admission is followed by fail-closed denial.
  The explicit delegate overload is infrastructure injection of the existing
  trusted port, never request data; no permissive production delegate is supplied.
- Composition uses no new persistence/owner abstraction, cross-module imports,
  network call, schema, role seed, default credential, framework profile or
  automatic Demo component registration. Later production composition/security
  wiring remains separate; this is callable backend composition, not an enabled
  production HTTP endpoint.
- Existing24h minimum receipt guard and all original atomic writes, lock ordering,
  expected-version/replay/conflict rules are byte-identical to tested1698940.

### Concrete public-contract dependency

Inspected production public APIs: identity AccountAccess/AccountView and
AuthenticatedActor belong to Demo/current minimal trust paths, while the new
IdentityGrantCommands is a command API, not a resource authority query.
MerchantAccess only declares MerchantView require(UUID id); MerchantView carries
id/name/code, no actor-bound management, target authority, scope validity or role
assignability. No Warehouse/Hub/Assignment public owner API exists in current
production source. V3 account_grant stores references; neither a resource catalog
nor scope-management delegation is established by its rows.

| Scope | Missing authoritative dependency | Default now |
|---|---|---|
| IDENTITY | Approved meaning of scopeId, scope ownership/delegation, authority over whole target including empty grants, and valid/assignable role pairs | DENY; no hardcoded root/global scope or inference from matching UUIDs |
| MERCHANT | Approved actor-bound merchant management/assignability query; require(id) proves existence only | DENY; no cross-module SQL or reverse Identity-to-Merchant import |
| WAREHOUSE | Warehouse owner authority contract and implementation | DENY |
| HUB / ASSIGNMENT | Dispatch owner authority contracts and implementations | DENY |

O5 full-target authority is accepted, but those concrete owner semantics and
contracts are not supplied by that principle alone. Required contract must bind
current actor/authVersion, target/version (even empty target), complete existing
and requested grant sets, exact manageable/assignable role-scope pairs and
resource validity. It must define transaction-coherent ownership resolution,
failure/incomplete-data denial and TOCTOU/locking semantics, using approved
public boundaries. These are dependency requirements, not a newly implemented
API/DTO/permission model.

Module Catalog/ArchUnit currently forbid Identity depending on Merchant or
other business modules, and Merchant already depends on Identity. An approved
composition/public-contract bridge must respect that direction and avoid a
cycle; do not simply add Identity-to-Merchant calls. A new persisted authority
model/migration, if needed, requires a separate approved design; V1/V2/V3 are
immutable. No external owner lookup may run while Identity locks are held
without an explicitly approved safe transaction design.

### Authored tests and strict inventory

New pure FailClosedGrantAuthorityTests:8 methods. They exercise missing/null/
failing provider, inactive/non-management/missing context, immutable exact
delegation and Error propagation. The successful delegate case is a synthetic
protocol fixture ONLY; it is not a successful production owner integration.

Added11 methods to the EXISTING IdentityGrantApplicationTests, retaining all33
prior method bodies exactly. The44-method suite keeps the same owned container,
fresh DB/roles per method and existing three-suite lifecycle; no new container
registration or cleanup-policy change. New cases verify default production
denial, no mutation/receipt disclosure on provider failure, hidden grants,
missing owner with empty target, disabled/revoked/expired/authVersion admission,
snapshot substitution/assignability, specific self-denial, and concurrent
actor authVersion/management-grant change.

Concurrent tests hold the actual Identity advisory transaction lock in a
competing runtime-role transaction; they require a real ungranted advisory lock
in pg_locks before committing the actor change. The waiting command must then
reload current actor state, deny, never call the provider and leave target/
receipt/audit unchanged. Bounded waits and executor shutdown are explicit.
These are AUTHORED PostgreSQL proofs, not executed results.

| New unit method | Runtime |
|---|---|
| missingProviderDeniesWithoutInventingIdentityScopeAuthority | UNVERIFIED |
| nullProviderResultDeniesWithoutFallback | UNVERIFIED |
| providerFailureIsDeniedWithoutExposingItsMessage | UNVERIFIED |
| disabledActorNeverReachesProvider | UNVERIFIED |
| unrelatedRoleNeverEstablishesManagementAuthority | UNVERIFIED |
| missingActorTargetOrRequestedContextDeniesBeforeProvider | UNVERIFIED |
| trustedDelegateReceivesExactCurrentSnapshotsAndImmutableRequestedSet | UNVERIFIED |
| vmErrorsAreNotSwallowedAsOrdinaryProviderUnavailability | UNVERIFIED |

| Added PostgreSQL method | Runtime |
|---|---|
| productionDefaultCompositionDeniesWithoutResourceContracts | UNVERIFIED |
| productionProviderFailureIsSanitizedBeforeReceiptDisclosure | UNVERIFIED |
| productionIncompleteOwnerContextCannotRemoveHiddenGrants | UNVERIFIED |
| productionMissingProviderRejectsEvenAnEmptyTargetGrantSet | UNVERIFIED |
| productionDisabledActorIsRejectedBeforeAuthorityLookup | UNVERIFIED |
| productionRevokedOrExpiredSessionIsRejectedBeforeAuthorityLookup | UNVERIFIED |
| productionStaleAuthorizationVersionIsRejectedBeforeAuthorityLookup | UNVERIFIED |
| productionSnapshotSubstitutionAndUnassignableScopeAreRejected | UNVERIFIED |
| productionSelfManagementPreservesSpecificDomainDenial | UNVERIFIED |
| productionConcurrentAuthorizationVersionChangeIsObservedAfterLock | UNVERIFIED |
| productionConcurrentManagementGrantLossIsObservedAfterLock | UNVERIFIED |

The original33 positive/negative/rollback/replay/concurrency methods, Domain48
and migration14 requirements are preserved. Existing synthetic fixtures seed
two denial tests; no NEW successful production-owner mutation is invented.
Collector adds exact8 unit +11 PostgreSQL identities; old required methods,
all collector function ASTs, skip categories and lifecycle registry unchanged.
Expected FUTURE totals only: Architecture23/0 skips; H2287/17 expected skips;
PostgreSQL370/5 expected skips. These totals are not Java runtime outcomes.

### Actual process-safe verification and safety

- python -B -m unittest discover -s scripts/ci -p 'test_*.py':
  **101/101 PASS**,33.034s, exit0. Four additional synthetic guard controls cover
  exact source/modes, missing suite/every method, invalid outcomes/rename and
  private diagnostic redaction. Existing controls remain active.
- Source inspection confirms original33 method bodies unchanged, finite new
  inventories exact, no forbidden module imports or Application-to-JDBC coupling,
  and unchanged collector logic/lifecycle. Python/static checks are not JUnit.
- No local Java/Maven/PostgreSQL/Docker, workflow dispatch, publication or PR.
  The earlier dispatch approval was consumed and is not reused.
- Six protected Foundation files and T-002-03 evidence remain byte-identical.
  All three outstanding CI closure documents retain their exact pre-task bytes
  as a prefix; only these AUTH additions are appended. No accepted historical
  source hash, CI result, REQ/AC, task body or DONE checkbox is overwritten.
- Same branch feature/002-identity-application-ci / HEAD1698940, index empty;
  Migration/Domain/Application local publication refs unchanged. Ignored
  evidence, credentials, ACL, developer services, migrations, dependencies,
  accepted OpenAPI/ADRs and architecture/workflow files untouched.

| Current AUTH source/test/policy path | SHA-256 |
|---|---|
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java | `032d44c61ff13f31f1d8c40a745c57e323a436ea8a776396789265105e4c1b29` |
| scripts/ci/test_verify_reports.py | `edcf92026fc33aa56edb990cc4b229191e2600ac77009ba926ee80e570bdba56` |
| scripts/ci/verify-reports.py | `2d61d1af2566331e8090a6eb03f37b5ccacfed9d01c5d261fe21b612ace367f6` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/FailClosedGrantAuthority.java | `11dd42fd2970e712b5697fc4856144c3dd6a38c3b4a0cabea308bb4d9c0d8ed1` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityGrantComposition.java | `4afa215c67a7dd59b1c14ac200977f96496be83106e8151ba029cd94ebfe0791` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/application/FailClosedGrantAuthorityTests.java | `d92cbfd71697eb1f45f6ced7aa716ea27b66c4b902ff774dac12c0ca948c196c` |

### Status and handoff

Independent production composition/availability boundary: source IMPLEMENTED,
runtime UNVERIFIED. Trusted persisted actor/session admission: connected through
the existing transaction, new composition integration runtime UNVERIFIED.
Resource-owner authority integration: DEPENDENCY BLOCKED, default-deny for all
unsupported scope contexts. Production HTTP/security remains unimplemented in
its assigned task. No full T-002-05/Feature002 acceptance.

Highest-priority dependency is a bounded approved Identity-management/resource
authority contract/composition decision compatible with current module direction,
starting with the independently implementable Identity-owned authority scope.
Do not invent that policy to obtain a positive fixture or claim production
readiness. New-source CI requires a separate exact publication/runtime approval;
no previous successful run covers these new classes/tests.

## T-002-05-AUTH-CONTRACT — owner decision packet — 2026-10-22

**T_002_05_AUTHORITY_CONTRACT_READY_FOR_OWNER_APPROVAL**.
This is a bounded contract-resolution checkpoint, not approval of the policy
below. Every new policy/API/consistency choice is **PROPOSED /
OWNER_DECISION_REQUIRED**. No production permission provider, public Java port
or owner behavior is added in this checkpoint. T-002-05 remains PARTIAL.

### A. Accepted semantics versus missing policy

Sources: [ADR-0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md)
DEC-03/04 and O5/O6/O7; [accepted contract](../../contracts/identity-access/README.md);
[Module Catalog](../architecture/module-catalog.md); existing ManagementAuthority,
IdentityAccount, Grant, ReplaceIdentityGrants and V3 account_grant.

| Established and retained | What it does NOT establish |
|---|---|
| Current persisted actor/account/session admission, ACTIVE state and current authVersion; JWT/session binding belongs to the production security adapter | Demo AuthenticatedActor, a submitted session ID or a role claim cannot authenticate a production actor |
| managementAdmin() requires ACTIVE plus IDENTITY_MANAGEMENT on an IDENTITY scope | It does not define scopeId meaning, wildcard/root status, delegation or assignable role pairs |
| Domain requires actor/authVersion/target/version binding, including empty target grants | A correctly bound ManagementAuthority is trusted policy input; its constructor does not derive authority |
| manageable covers current UNION proposed, assignable covers all proposed pairs | Scope/role cross-products, any-match checks and resource existence never authorize replacement |
| O6 self/last-admin and O7 authorize-before-replay/stale guards | No exception for a missing owner or a deployment administrator |
| V3 stores exact role_code/scope_type/scope_id references and versions | It has no realm registry, target-to-realm relation, delegation or resource-owner permission catalog |

IdentityAuthorizationTests explicitly supply trusted synthetic authority fixtures,
including an empty-target example. They verify consumption/guards, not the policy
that produces those fixtures. OpenAPI Grant examples contain one synthetic
IDENTITY UUID, not a registered global administrative scope. V3 explicitly says
that role catalog seed does not imply authority. There is no accepted mapping
from actor grants to manageable/assignable sets. This is a missing decision,
not a demonstrated contradiction between accepted sources.

### B. Exact dependencies still absent

| Scope / owner | Actual public API | Missing authority |
|---|---|---|
| IDENTITY / Identity | IdentityGrantCommands is a backend command; AccountAccess and AuthenticatedActor are existing Demo/minimal security contracts | Identity scope identifier semantics, exact delegation policy and authority over an unscoped/empty target |
| MERCHANT / Merchant | MerchantAccess.require(UUID) returns MerchantView(id, displayName, reference) | Actor-bound, target-bound permission to manage/assign each exact role on the merchant; valid owner state and concurrent revocation protocol |
| WAREHOUSE / Warehouse | No production public owner API | Same operation-specific policy and public query for warehouse-owned resources |
| HUB, ASSIGNMENT / Dispatch | No production public owner API | Distinct hub/assignment identities, actor management policy and owner-controlled state/locking |

Finance is not a new scope type in the accepted five-type contract. A FINANCE
role is not automatically assignable through any existing scope or administrator.
Do not add a sixth type, global permission catalog or owner fallback.

### C. Exact public SPI proposal

All paths/signatures in this section are PROPOSED, NOT IMPLEMENTED and require
owner approval before adding production source. Reuse the accepted public
IdentityGrantCommands.GrantData rather than exposing internal Grant/IdentityAccount.

Proposed file:
app/backend/src/main/java/com/elogistic/backend/identity/api/ResourceGrantAuthority.java

~~~java
package com.elogistic.backend.identity.api;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.elogistic.backend.identity.api.IdentityGrantCommands.GrantData;

public interface ResourceGrantAuthority {
    Decision evaluate(Request request);

    enum Operation { REPLACE_IDENTITY_GRANTS }
    enum Action { MANAGE, ASSIGN }
    enum Status { AUTHORIZED, DENIED, UNKNOWN }
    enum Reason {
        PERMITTED, NOT_PERMITTED, UNSUPPORTED, UNAVAILABLE,
        INCOMPLETE, STALE, INVALID_CONTEXT
    }
    record Actor(UUID accountId, long authVersion) {}
    record Target(UUID accountId, long version) {}
    record Check(GrantData grant, Action action) {}
    record Request(UUID invocationId, Actor actor, Target target,
                   Operation operation, Set<GrantData> current,
                   Set<GrantData> requested) {}
    record Outcome(Status status, Reason reason) {}
    record Decision(Request binding, Map<Check, Outcome> outcomes) {}
}
~~~

Implementation obligations for these proposed records: nonnull fields/elements,
positive versions, exact accepted GrantData syntax, immutable defensive copies,
bounded-by-the-existing-command collections, and redacted toString on requests,
decisions and actor/target records. No new wire field, HTTP endpoint or client
authority input. No secret, JWT/session ID, reason text, account contact, password
hash or persistence type crosses this SPI. invocationId is a fresh server-generated
per-evaluation nonce, NOT the client requestId/idempotency key, and not a reusable
authorization capability.

Trusted source: Identity's module-local adapter constructs Actor/Target and
current/requested from the already locked and admitted state in ReplaceIdentityGrants,
never from a Demo principal or supplied authority snapshot. Public DTOs are not
proof of authentication by themselves. Only a trusted DI-installed implementation
is invoked, synchronously, by that adapter; no endpoint accepts a Request/Decision.

Resource semantics: external current/requested grants retain exact accepted
role/scopeType/scopeId triples. MERCHANT identifies Merchant's organization UUID;
WAREHOUSE identifies Warehouse's warehouse UUID; HUB and ASSIGNMENT identify
Dispatch's separate public resource IDs. An owner must verify those identities
against its own authoritative data; a UUID match alone proves nothing.
IDENTITY is handled only by the local policy, never by a business-owner callback.

For C=current, P=requested in a given external partition, required outcome keys
are EXACTLY {Check(g,MANAGE): g in C union P} union
{Check(g,ASSIGN): g in P}. MANAGE includes grants being removed; ASSIGN is needed
even for retained requested grants, matching the existing Domain guard.
Each decision is specifically for the bound actor/authVersion, target/version,
operation, nonce and exact input sets. No wildcard/role-only/resource-only result.

Valid outcomes: AUTHORIZED/PERMITTED, DENIED/NOT_PERMITTED, or UNKNOWN with one
of UNSUPPORTED/UNAVAILABLE/INCOMPLETE/STALE/INVALID_CONTEXT. Every other combination,
missing/extra/null outcome, wrong binding, malformed response or absent provider
denies the entire command. RuntimeException is contained by the existing
fail-closed boundary; VM Errors propagate with transaction rollback.
DENIED and UNKNOWN remain distinguishable inside this internal protocol but both
map to existing ACCOUNT_NOT_ACCESSIBLE before receipt/account disclosure. No
new public error code/status or raw owner diagnostic is exposed.

### D. Owner API bridge and dependency direction

Proposed owner public query signatures, in their own named files:

~~~java
// merchant/api/MerchantGrantAuthority.java
Decision evaluateGrantReplacement(Request request);
// warehouse/api/WarehouseGrantAuthority.java
Decision evaluateGrantReplacement(Request request);
// dispatch/api/DispatchGrantAuthority.java
Decision evaluateGrantReplacement(Request request);
~~~

Here Request and Decision are ResourceGrantAuthority nested public types above.
Each declaration belongs in its corresponding public interface; it is not a
method added to MerchantAccess. Merchant accepts only MERCHANT partitions;
Warehouse only WAREHOUSE; Dispatch only HUB/ASSIGNMENT partitions. Every wrong or
unsupported type returns UNKNOWN/UNSUPPORTED, never existence-based approval.
These are exact proposed signatures, not a claim of implemented owner policy.

Recommended bridge: workflow.internal.application.ResourceGrantAuthorityCoordinator
implements identity.api.ResourceGrantAuthority and calls ONLY those owner public
interfaces. It routes the complete external set by owner, preserving actor,
target, nonce and operation, then verifies exact partition decisions and merges
them. Absent owner leaves UNKNOWN, never an allow stub. Dispatcher processes HUB
before ASSIGNMENT within its partition. No workflow access to identity.internal.

Identity's own internal adapter implements the existing internal GrantAuthority,
maps Domain snapshots to/from the public SPI and combines the local policy with
external decisions. Its module-local configuration may inject the public SPI
and construct the existing internal composition. Workflow does NOT call/import
ProductionIdentityGrantComposition or GrantAuthority; those are internal.
No cross-module configuration factory exposing internal types is proposed.

Compile-time graph: workflow -> identity.api + merchant.api + warehouse.api +
dispatch.api; identity -> its own api/internal ONLY; merchant -> identity.api.
The proposed Warehouse/Dispatch APIs would also import the Identity-owned DTOs:
those two new module-to-Identity directions are NOT in the current Module
Catalog and need explicit bounded approval and exact ArchUnit rules before
implementation. Identity never imports Merchant/Warehouse/Dispatch, so the
Identity-to-Merchant cycle is avoided. The initial Merchant-only bridge can use
existing directions; do not create Warehouse/Dispatch stubs merely for parity.
Alternative owner-specific DTOs mapped only in workflow would avoid the two new
directions but duplicate protocol types; that is an alternative for owner choice,
not a silent platform/shared-domain extraction.

Identity assembles manageable/assignable only from explicitly authorized exact
pairs. A local or external denial/unknown aborts the whole command; partial union
coverage is never a partial success. Construct ManagementAuthority with current
actor/authVersion/target/version ONLY after the target policy and every required
pair check pass. Existing Domain requireManagement then runs unchanged.
All-IDENTITY requests do not require an external callback. Mixed requests do.

### E. Identity-local policy decision and implementation status

**No genuine allowing Identity provider implemented:** the accepted mapping is
ambiguous. Existing fail-closed decorator/composition and their19 authored tests
remain byte-identical. No convenience default or permissive production fixture.

Recommended first bounded policy, **OWNER_DECISION_REQUIRED**:
IDENTITY scopeId means an explicitly registered Identity-administration realm,
not accountId, deployment-owner privilege or a wildcard. Realm IDs enter the
module through trusted explicit composition configuration, never request claims;
there is no default/sample realm registration. The initial provider manages and
assigns ONLY exact IDENTITY_MANAGEMENT/IDENTITY/registered-realm triples already
held by the current admitted actor. It delegates no other role and no external
resource authority. Another IDENTITY role, unregistered realm, mismatching realm,
missing configuration or unsupported context denies.

For the initial Identity-only slice require a nonempty current target grant set
and complete current/requested union coverage by those exact actor-held pairs;
bind the target/version explicitly. For an empty current target, no accepted
target-to-realm relation exists: return UNKNOWN/INCOMPLETE, do not infer a realm
from proposed input alone. This proposed restriction is NOT a new accepted
business requirement and is NOT retroactively applied to accepted Domain behavior
or tests. Supporting empty/unscoped targets needs a separate explicit owner policy
(e.g. authoritative realm membership, with a future migration if persisted).
No V3 modification is authorized. Last-admin protection stays at its existing
accepted population boundary; no unapproved per-realm replacement of that guard.

Approval of this proposal would permit an Identity-local implementation inside
the existing GrantAuthority port, with injected immutable registered IDs, current
actor/target data and existing locking. It would not authorize Merchant/other
owner behavior, runtime activation/bootstrap, new configuration reads or a
successful full Feature002 acceptance. If owner requires broader Identity or
empty-target management now, that semantic decision must precede implementation.

### F. Transaction, stale authority and negative tests

Proposed consistency model (requires approval per participating owner):
- Same in-process call/thread, same existing PostgreSQL database/DataSource and
  joined Identity transaction. Require transaction participation, compatible
  isolation/read-write and bounded remaining budget; no REQUIRES_NEW, async,
  remote service, cached result or detached decision.
- Retain Identity administration -> actor/session/target locks first. Then acquire
  owner locks in deterministic MERCHANT -> WAREHOUSE -> HUB -> ASSIGNMENT order,
  UUID ascending within each type. Owner implementations lock stable resource/
  authority guard rows before reading permissions and retain locks until the
  Identity transaction ends, including when replaying a receipt.
- Every path that mutates those owner authority facts must acquire the same
  owner guard locks. Missing membership rows need a stable guard-row/predicate
  protocol; locking only existing permission rows cannot protect absence.
  A version/timestamp check alone is not protection against a change after check.
- Owner operations must not hold owner locks then synchronously call Identity
  mutation/admission APIs: that would invert the declared order. Exact owner
  mutation-path inventory and a real two-transaction regression are preconditions
  for declaring an owner implementation supported.
- Owner reads go through owner APIs and its own tables, never Identity SQL against
  another schema. Identity still writes only its own grants/session/audit/receipt.
  Existing15s transaction/5s lock/10s statement ceilings are preserved; callbacks
  must respect remaining budget, not reset it per resource.
- Rollback and deny on missing participation, stale/unknown permissions, timeout,
  invalid coverage or owner inability to guarantee locking. No auto retry or
  receipt disclosure after authority loss. Recheck session deadlines as existing
  use case does; construct no long-lived capability/cache.
- The shared DB and matching transaction manager alone are NOT evidence of owner
  coherence. Until the owner locking/mutation protocol is implemented and tested,
  external authority stays unsupported/denied.

Planned additional tests after policy approval; NOT authored/executed here:

| Test ID | Assertion / expected verification |
|---|---|
| AUTHC-01 | Approved registered exact Identity pair, different eligible target and full union: real provider positive; no synthetic authority used as production policy |
| AUTHC-02 | Unregistered/wrong realm, unsupported role, mismatched pair, missing config and empty-target unresolved context deny with no writes |
| AUTHC-03 | Disabled/revoked/expired/stale actor denied before provider; no client principal/claims trusted |
| AUTHC-04 | Owner resource exists but explicit management DENIED: no assignment, receipt/contact leakage or writes |
| AUTHC-05 | Each unsupported scope, null/throwing/missing owner, UNKNOWN and every malformed result deny |
| AUTHC-06 | Actor/authVersion/target/version/nonce/operation/input substitution, missing or extra outcome, manage-only versus assign-only, role/scope cross-product deny |
| AUTHC-07 | Hidden current grants, removed grant, retained requested grant and mixed owners require all checks; no any-match success |
| AUTHC-08 | Real owner revocation/disable/deletion racing replacement follows approved lock order and denies if revocation precedes admission; if mutation linearizes first, it commits before owner change |
| AUTHC-09 | No transaction, separate DataSource/REQUIRES_NEW, missing guard lock, timeout or incomplete owner query rolls back and exposes only safe failure |
| AUTHC-10 | Existing self/last-admin/version/replay/rollback/audit/revocation/24h minimum and all original regressions remain active |
| AUTHC-11 | ArchUnit actual production graph + negative fixtures prove no owner internals, forbidden reverse import, cycle or public API internal type |

Preserve original33 verified grant methods, Domain48 and migration14. Preserve
the8 unit +11 PostgreSQL AUTH methods and strict101-control report inventory;
all19 new Java cases still UNVERIFIED. Run39641670408 verifies only1698940.
A later scoped CI authorization must pin the final source and verify all three
existing checks, exact outcomes/skips, privacy and owned cleanup. No new report
counts are claimed for tests that are only planned above.

### G. Approval matrix and bounded checkpoint result

| Decision | Recommendation | Status / approver |
|---|---|---|
| AUTHC-D1 Identity scope/delegation | Registered administration-realm UUIDs; only exact actor-held IDENTITY_MANAGEMENT pairs, no global/operational fallback; initial empty-target context denied | OWNER_DECISION_REQUIRED / project owner, Identity reviewer |
| AUTHC-D2 Public boundary | Identity-owned narrow SPI + module-local adapter + workflow coordinator; Merchant public owner query; no direct Identity-to-Merchant call | OWNER_DECISION_REQUIRED / architecture owner |
| AUTHC-D3 Other owners | Keep Warehouse/Hub/Assignment denied until their policies exist; if using these common DTOs, approve only named Warehouse/Dispatch-to-Identity API dependencies; otherwise owner-specific DTO mapping | OWNER_DECISION_REQUIRED / architecture and resource owners |
| AUTHC-D4 Consistency | Joined local transaction plus stable owner guard locks and participating mutation paths; no network/cached authority | OWNER_DECISION_REQUIRED / architecture and resource owners |
| AUTHC-D5 Runtime | Exact-source publication/contained CI with existing strict report/privacy/cleanup gates after implementation | Separate future owner authorization; NOT granted by this proposal |

This packet resolves the design options enough for an explicit owner decision;
it does NOT resolve the runtime dependency. No accepted ADR/OpenAPI, module
catalog/rule, retention policy, SQL, source or test assertion is changed.
No additional actor permission requirement is silently made mandatory.

Working baseline: feature/002-identity-application-ci, HEAD1698940, index empty.
Only this evidence file, canonical tasks supplement and supplemental trace receive
append-only additions. All pre-checkpoint bytes of those three documents remain
as prefixes; six Foundation paths, T-002-03 evidence and every preexisting
source/test/policy file remain byte-identical. No Java/Maven/PostgreSQL/Docker,
credential/ACL access, CI dispatch, branch operation or publication.

### Actual checks for this contract checkpoint

- python -B -m unittest discover -s scripts/ci -p 'test_*.py':
  **101/101 PASS**,31.426s, exit0. Uses synthetic local reports/temporary test
  directories and mocked Docker queries; no JVM/database/owner runtime evidence.
- Fresh working inventory matched the preceding AUTH checkpoint:484 tracked/
  nonignored files, same15 dirty paths and empty index before edits.
- Final documentation/source integrity, relative-link/UTF-8/disclosure and
  CRLF-aware git diff checks are required before reporting this packet complete.
- Planned AUTHC-01..11 are test specifications, not11 additional tests executed.
  No Java compilation, successful allowing owner provider, new CI run or full
  Application/Feature acceptance is claimed.

Final integrity checks: PASS. Exactly3 authorized document additions across484
tracked/nonignored files; no added/deleted path. All3 pre-task byte prefixes
preserved; all other481 file hashes unchanged, including the6 protected
Foundation paths, T-002-03 evidence and all19 AUTH test methods. Five new relative
links/anchors resolve; strict UTF-8, bounded disclosure scan and CRLF-aware
git diff --check PASS. Same15 dirty paths, unchanged refs/HEAD and empty index.

## T-002-05-AUTHC-RESOLUTION — restricted owner decision — 2026-10-22

Authority: **OWNER_AUTHC_DECISION: CONDITIONAL_APPROVAL_WITH_RESTRICTIONS**.
Verdict: **T_002_05_IDENTITY_SCOPE_MAPPING_BLOCKED**.
This checkpoint supersedes only the pending decision state of the preceding
AUTH-CONTRACT proposal. It does not promote that entire proposal to ACCEPTED.
T-002-05 remains PARTIAL/unchecked; Feature002/AC-002-01..05 NOT ACCEPTED.

### A. Exact AUTHC-D1–D4 disposition

| Decision | Owner authorization now effective | Not approved / implementation limit |
|---|---|---|
| AUTHC-D1 | Authoritative active account/session/grants; explicit manageable/assignable exact pairs; complete current/requested union; self/last-admin guards; no wildcard or automatic Finance/Operations. For this replacement slice deny a target with no existing grants absent a separate accepted empty-target path | Holding an ordinary grant is NOT delegation. Prior proposed actor-held-pair delegation and realm registration are NOT approved. No scope UUID, realm registry or implicit global interpretation may be invented |
| AUTHC-D2 | SPI identity.api.ResourceGrantAuthority.evaluate(Request): Decision approved IN PRINCIPLE only within existing module directions; trusted actor/authVersion, target and expected version, exact operation/full relevant sets/resource context; AUTHORIZED/DENIED/UNKNOWN and fail-closed private failures | Not acceptance of every previously proposed record/signature or new dependency. No DTO/caller-supplied identity establishes trust. No public SPI source, factory or workflow is installed during this blocked checkpoint |
| AUTHC-D3 | Merchant/Warehouse/Hub/Assignment/other owner scopes stay denied until adequate approved actor-bound owner APIs exist; existence is not management permission | No external policy, synthetic production owner, Demo principal/global-admin fallback or internal import. Previous Warehouse/Dispatch-to-Identity dependency proposals remain unapproved |
| AUTHC-D4 | Existing proven Identity-local PostgreSQL transaction/locks/versions, atomic revocation/audit/receipt, conflict semantics, last-admin protection and >=24h retention retained | Independent owner transactions are not assumed joined; no stale/cache/network authority. Prior external owner guard-lock design remains a proposal, not a proven or accepted implementation |

No accepted OpenAPI/ADR is edited. The retained prior proposal is historical:
in particular its configured realm registry and actor-held grant delegation
must NOT be read as approved by the current conditional authorization.

### B. Verified scope representation and exact remaining decision

Inspected [accepted Identity contract](../../contracts/identity-access/README.md),
[ADR-0009 O5/O6/O7](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
Feature002 spec/design/tasks, Domain ResourceScope/Grant/ManagementAuthority and
V3. Actual facts:

- contracts/openapi/identity-access.yaml, components.schemas.Grant.scopeId:
  UUID syntax only. The example is synthetic; no definition makes that UUID
  an account, an administrative realm or a registered global scope.
- Domain ResourceScope(Type.IDENTITY, UUID) is a typed reference and explicitly
  proves no ownership/assignability. Grant.isIdentityManagement identifies the
  management entry role/type, not a resource relation or delegation policy.
- V3 identity.account_grant(scope_type,scope_id,role_code) stores a nonnull UUID
  and role reference. It has no Identity-resource/realm registry, FK to a scope
  owner, delegation flag or authoritative manageable/assignable relation.
  identity.user_account identifies accounts, but no accepted rule equates its id
  with an IDENTITY scope_id.
- ManagementAuthority requires trusted exact manageable/assignable input and
  actor/authVersion/target/version binding. Its guard consumes that input; it
  does not derive it from possession of a grant.
- Generic Domain guards/tests support trusted target-bound authority even for
  empty grant sets. That is not a separate accepted empty-target
  replaceIdentityGrants provisioning path. Domain and its tests remain unchanged.

**One remaining owner decision: IDENTITY_SCOPE_RESOURCE_AND_ASSIGNABILITY.**
Define what authoritative Identity resource an IDENTITY scopeId references, and
the explicit operation-specific mapping from current management authority to
manageable and assignable exact role/resource pairs. State where that mapping
is authoritatively represented and how revocation is serialized. No mapping
exists in the inspected accepted artifacts; an allowing provider cannot be
grounded safely.

Least-invasive alternatives for that ONE model decision (neither implemented):
1. If an existing Identity resource/account is intended, approve its exact
   identifier relation and explicit management/assignment policy over that
   resource in the contract/Domain model; only reuse V3 if its current data and
   invariants can actually represent the approved policy. Do not silently treat
   grant possession as delegation or scopeId as targetId.
2. If an administrative realm is intended, explicitly approve its minimal
   authoritative resource/delegation representation and a separate append-only
   migration if needed. Do not configure a sample UUID or fabricate a registration
   subsystem. V1/V2/V3 remain immutable.

Until this decision, every unsupported Identity authority path stays DENIED.
This is a known missing mapping, not evidence of a contradictory accepted spec.

### C. Minimal implementation actually made

No allowing Identity-local provider was implemented, and no permission was
enabled. The independently approved empty-target restriction is implemented in
the existing FailClosedGrantAuthority decorator:

- After existing null/current active-management checks and before any provider
  call, target.grants().isEmpty() returns null.
- The existing ReplaceIdentityGrants composition maps this missing authority to
  ACCOUNT_NOT_ACCESSIBLE before receipt/account disclosure or mutation.
- Default production composition still installs no authority delegate and denies
  all unsupported paths. The trusted delegate overload remains explicit internal
  DI only; test delegates that return an allow snapshot are negative controls,
  not installed production policies.
- No change to accepted Domain guards, public command/HTTP contract, SQL,
  configuration, bootstrap/provisioning, framework registration or other module.
  The check is at the replacement-specific production composition boundary,
  not the generic Domain provision/disable/authority behavior.

### D. Trusted state and local consistency

ProductionIdentityGrantComposition -> JdbcIdentityGrantStore ->
ReplaceIdentityGrants -> FailClosedGrantAuthority remains the actual call path.
The use case obtains actor ID from the persisted admitted session, locks current
actor/session/target, checks activation/revocation/authVersion/deadlines and reads
complete grants. HTTP/security must still authenticate session/JWT binding;
a client session UUID, Demo role or caller-supplied authority is not trusted.

The new empty-target denial reads that locked target snapshot; no additional
query, cache or race window is introduced. Existing administration advisory lock,
account/session row locks, expected-version/replay order, post-resolution session
deadline check, local mutation/audit/receipt/revocation atomicity, timeout budgets
and >=24h guard remain byte-identical. It adds no external callback/network,
independent transaction or new owner lock assumption.

External scopes remain DENIED in the actual default composition because there
is no approved owner delegate. MerchantAccess.require(UUID) remains existence
only. No new owner adapter/public SPI is created simply to return UNKNOWN.
Future SPI must distinguish current target version from the command's original
expectedVersion (preserving authorized-replay-before-stale behavior); D2 does
not authorize confusing those two values or trusting a claimed actor version.

### E. Authored tests and exact inventory

| Test / control | New assertion | Actual status |
|---|---|---|
| FailClosedGrantAuthorityTests.emptyTargetIsDeniedBeforeAnAllowingDelegate | Active management actor + empty target + nonempty request denies and never calls a synthetic allowing delegate | AUTHORED / Java runtime UNVERIFIED |
| IdentityGrantApplicationTests.productionEmptyTargetIsDeniedBeforeAnAllowingDelegate | Real composition planned against isolated PG; persisted target grants empty; capable synthetic delegate never invoked; safe denial and all six observed Identity tables unchanged | AUTHORED / PostgreSQL runtime UNVERIFIED |
| IdentityAuthorityCompositionInventoryTests.test_empty_target_denial_cases_are_mandatory_and_cannot_skip | Each new method is mandatory in its applicable modes; omission/skip rejected; restored synthetic report accepted | Executed process-safe Python control PASS |

Original33 grant integration +11 prior AUTH PostgreSQL method bodies unchanged;
8 prior authority unit method bodies unchanged. Domain48 and migration14 source/
requirements unchanged. Total authored authority unit suite9; grant PostgreSQL
suite45 (33 original +12 authority). The prior19 unverified cases remain intact;
two new Java cases bring unverified authority additions to21, not21 executed.

Collector appends only the two exact identities. Expected synthetic FUTURE
inventories: Architecture23/0 skips; H2288/17 expected skips; PostgreSQL372/5
expected skips. These are required-count calculations, NOT fresh Java outcomes.
Finite-source inventory tests updated from8 to9 and11 to12; no old method,
expected skip, failure assertion or cleanup requirement removed.

### F. Actual process-safe verification

- python -B -m unittest discover -s scripts/ci -p 'test_*.py':
  **102/102 PASS**,34.349s, exit0. Existing101 controls plus one meaningful
  report omission/skip/positive-restoration control. Synthetic temporary reports;
  mocked Docker queries, no real JVM/database/container launch.
- Static method-body hashes: all52 pre-task test methods retained byte-for-byte
  in decoded Java source (8 unit +44 PostgreSQL); only two methods appended.
  All8 collector function ASTs unchanged: report/privacy/cleanup execution logic
  and Testcontainers lifecycle registry remain unchanged.
- No positive authorization path enabled; no new concurrency guarantee invented.
  Existing real-lock concurrency tests remain authored/UNVERIFIED for current
  composition source. This new denial requires no new concurrency harness.
- Java compilation,9 authority unit tests,45 PostgreSQL grant cases, current
  ArchUnit and actual new-source privacy/cleanup remain UNVERIFIED. No currently
  approved runtime path was consumed, no publication or CI dispatch.
  Run39641670408 remains historical verification of1698940 only.

### G. Source/test/policy fingerprints

| Current path changed by this checkpoint | SHA-256 |
|---|---|
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/FailClosedGrantAuthority.java | `f92499139e8516c9d88f6628a390b018494f8368ef58234972d452acf3cab019` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/application/FailClosedGrantAuthorityTests.java | `486b28b4d1323be04ddb23acce274143898930711ff9565effa92cc966bb5758` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java | `840b12ae2725de452c6f2670d91260a023c2bac6fee160ce0a016169f578abca` |
| scripts/ci/verify-reports.py | `b42a8b16ea90cc9e719fd224dafdac71a9d4c4226d27e2c72885ba9cf057e826` |
| scripts/ci/test_verify_reports.py | `c9a8d84d26c84d509e4a623f074fb189fccaace140edc63e6242192f41849249` |

### H. Git, docs and remaining scope

Baseline branch feature/002-identity-application-ci; HEAD
1698940e4d4d888993daa2e38e995014c4f3282b; index empty. Expected changes are the
five paths above plus append-only additions in this evidence, tasks and
supplemental traceability. Preserve all pre-task document bytes; preserve six
Foundation paths, T-002-03 evidence, accepted Migration/Domain, composition/store/
use-case source, public APIs/contracts/ADRs and publication refs.

T-002-05 stays PARTIAL: allowing Identity authority mapping/provider,
transaction-compatible resource-owner integrations and their runtime verification,
plus remaining approved Application use cases are incomplete. No provisioning,
invitation/recovery, HTTP/security or unrelated owner work is performed here.
Feature002/AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.
No branch synchronization, Git staging/publication, SQL, credential/ACL access,
developer-service operation or automatic next-task execution.

Final integrity checks PASS: exactly8 expected modified paths across484 tracked/
nonignored files, no new/deleted path; all other476 SHA-256 values unchanged.
All3 pre-task SDD byte prefixes preserved. Four new relative links/anchors,
strict UTF-8, bounded disclosure scan and CRLF-aware git diff --check PASS.
Same branch/HEAD/refs, same15 dirty paths, empty index. Six Foundation paths,
T-002-03 record, V1/V2/V3, accepted Domain/public contracts, original use case/
JDBC composition and Testcontainers lifecycle source remain byte-identical.

## T-002-05-AUTHC-REALM — registered Identity Realm decision — 2026-10-22

Authority: **OWNER_IDENTITY_REALM_ARCHITECTURE: CONDITIONALLY_APPROVED**.
Verdict: **T_002_05_REALM_MIGRATION_APPROVAL_REQUIRED**.
The Identity resource meaning is now resolved at architectural level:
**IDENTITY.scopeId is the UUID of a registered authoritative Identity Realm**.
This supersedes only the preceding unknown resource meaning. Persistence,
bootstrap/activation and delegation details below remain OWNER_DECISION_REQUIRED.
T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED.

### A. Decision record and bounded product fit

Sources: [PRD](../prd.md), [canonical feature](../../specs/002-identity-access/spec.md),
[dependency order](dependency-order.md), [Module Catalog](../architecture/module-catalog.md),
[ADR-0009 / DEC-03/04 / O1–O7](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
[accepted contract](../../contracts/identity-access/README.md), V1/V2/V3,
Domain/account/session/authority tests and the preceding AUTHC-D1–D4 records.

| Question | Evidence and conclusion |
|---|---|
| One system Identity Realm? | Current PRD/Feature002 can be served by one system-level administrative boundary. Adopt that as the proposed bounded MVP deployment model under this owner direction; no authoritative requirement for per-organization realms was found |
| Is single-organization hosting a canonical fact? | NOT ESTABLISHED. FR-A02 mentions organization/resource scope; the glossary defines Merchant as an organization/shipper. That does not define platform tenant hosting or a root organization. One system security Realm is not a claim that all Merchants are one organization |
| Authoritative tenant/organization root? | No Production Identity tenant/root organization/realm membership model exists. V2 merchant.demo_merchant is a Demo business organization, not a tenant or Realm registry. Do not map merchantId/accountId/deployment owner to Realm |
| Does V3 register Realm identity? | No. account_grant.scope_id is UUID-only; no registry/status/FK or source of active registered Realm truth |
| Delegation accepted already? | Current managementAdmin is an entry predicate; ManagementAuthority consumes exact trusted manageable/assignable pairs. Neither defines who may delegate IDENTITY_MANAGEMENT |
| MVP limit | At most one explicitly registered SYSTEM Realm in the proposed representation; no organization-specific realm feature, membership engine, permission catalog or cross-module dependency change |

The owner-approved semantic invariant is registration, not magic UUID or implicit
global administration. Account/session/authVersion admission, full-target current/
requested union, explicit assignability, self/last-admin, empty-target replacement
denial and external default-deny remain mandatory. No original REQ/AC is rewritten.

This is the Feature002 SDD decision record for the approved intent. Before
implementation of the proposed model, record its exact approved persistence/
delegation policy in a focused new ADR (next available ADR-0010 if still free)
and contract revision. ADR-0009's accepted browser/session policies remain intact.

### B. Smallest safe authoritative representation — proposed

One new Identity-owned table, identity.realm, plus a generated reference column
and FK on existing identity.account_grant. No account-to-tenant field, realm
membership table, generic permission matrix, cross-schema FK or new endpoint.

- id: UUID primary key, explicitly supplied from controlled registration input.
- code: SYSTEM only, unique, so this bounded schema permits at most one system
  Realm; changing to organization-specific realms requires a later decision.
- status: INACTIVE or ACTIVE. Initial registration INACTIVE; existence is never
  authorization. Controlled activation is a separate deliberate bootstrap step.
- registered_at / updated_at: UTC timestamps with monotonic record clock check.
- Runtime may SELECT Realm only. Existing NOLOGIN owner/migrator controls its
  registration/state; no runtime insert/update/delete/DDL or public bootstrap API.
- Generated account_grant.identity_realm_id equals scope_id only for IDENTITY,
  otherwise NULL. FK to realm.id prevents unknown Identity references without
  imposing a false FK to Merchant/Warehouse/Dispatch. The original exact grant PK
  and accepted role/scope fields remain unchanged. Active state and delegation
  remain transactional Application policy, not implied by FK validity.

A configuration UUID alone or an injected Set<UUID> is not the registration
source of truth. Registration is persisted by controlled migration/bootstrap;
missing schema/row/inactive status is denied. No fallback registration from the
first observed grant or a random UUID at application startup.

### C. Exact proposed forward migration — NOT a migration file

Current global stream is V1__ownership_schemas.sql,
V2__demo_identity_merchant_drafts.sql, V3__production_identity.sql.
Next available version observed is **V4__identity_system_realm.sql**.
The following is a reviewable design only; no .sql file is created/applied.

~~~sql
-- PROPOSED / OWNER_DECISION_REQUIRED.
-- Flyway's existing migration principal SET ROLE owner owns these objects.
-- identitySystemRealmId must be an explicit validated UUID; NO default.
CREATE TABLE identity.realm (
 id UUID PRIMARY KEY,
 code VARCHAR(16) NOT NULL UNIQUE CHECK (code = 'SYSTEM'),
 status VARCHAR(8) NOT NULL CHECK (status IN ('INACTIVE', 'ACTIVE')),
 registered_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT ck_identity_realm_clock CHECK (updated_at >= registered_at)
);

INSERT INTO identity.realm(id,code,status,registered_at,updated_at)
VALUES ('${identitySystemRealmId}'::uuid,'SYSTEM','INACTIVE',
        CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);

-- Never relabel existing grants or infer an authoritative ID from their rows.
DO $realm_preflight$
BEGIN
 IF EXISTS (
   SELECT 1 FROM identity.account_grant
   WHERE scope_type = 'IDENTITY'
     AND scope_id <> '${identitySystemRealmId}'::uuid
 ) THEN
   RAISE EXCEPTION 'IDENTITY_REALM_MAPPING_REVIEW_REQUIRED';
 END IF;
END;
$realm_preflight$;

ALTER TABLE identity.account_grant
 ADD COLUMN identity_realm_id UUID GENERATED ALWAYS AS
   (CASE WHEN scope_type = 'IDENTITY' THEN scope_id ELSE NULL::uuid END) STORED;

ALTER TABLE identity.account_grant
 ADD CONSTRAINT fk_identity_grant_realm
 FOREIGN KEY (identity_realm_id) REFERENCES identity.realm(id)
 ON UPDATE RESTRICT ON DELETE RESTRICT;

REVOKE ALL ON TABLE identity.realm FROM PUBLIC;
GRANT SELECT ON TABLE identity.realm TO "${runtimeRole}";
~~~

The stored generated column and ordinary FK approach follows the PostgreSQL18
[generated-column](https://www.postgresql.org/docs/18/ddl-generated-columns.html)
and [table-constraint](https://www.postgresql.org/docs/18/sql-createtable.html)
mechanisms. This is documentation-level feasibility, NOT executed SQL validation.
Use STORED explicitly; do not rely on the server's default generated-column kind.

Implementation-level approvals needed: this V4 shape, explicit Realm registration
input and INACTIVE-to-ACTIVE bootstrap procedure. Migration startup must validate
canonical UUID syntax before Flyway interpolation, use no fallback, and withhold
raw database diagnostics from public artifacts. The Realm UUID is an identifier,
not a credential; no real identifier/secret is read in this checkpoint.

Deploy with the existing migration/owner path and a quiesced compatible app.
Preflight only the intended database and distinct IDENTITY references with owner
authorization. If any existing IDENTITY UUID differs from the explicitly chosen
Realm ID, STOP before deployment. No automatic data rewriting, registering every
legacy UUID, deleting grants, seed account import, repair/clean or V1/V2/V3 edits.
A separate reviewed data-mapping plan would be required for such data.

Migration failure must roll back its transaction; do not accept a partially
created registry/FK or unvalidated constraint. Flyway rerun/validate and V3 checksum/
Demo preservation are mandatory gates. Never drop V4 or edit applied SQL to roll
back an application version; retain additive schema and disable the new provider.

Compatibility surface to verify before merging V4:
- Existing JdbcIdentityGrantStore uses explicit column lists; preserve that path.
- Existing Application fixture and IdentityMigrationTests contain positional
  five-value account_grant inserts. Test their behavior with the new generated
  column on actual PostgreSQL; prefer explicit original column lists where an
  adaptation is necessary, preserving original assertions and method identities.
- Existing migration helpers, PostgresTestcontainersTests and PostgresFoundationTests
  assert latest count/history1/2/3 and exact table ownership counts. Update latest
  expectations to include V4 only when authorized; retain explicit V1/V2/V3
  compatibility/upgrade coverage. Do not hide V4 by pinning every suite to V3.
- Existing Testcontainers flyway helper and separate CI PostgreSQL migration
  launcher currently supply runtimeRole only. Both need an explicit synthetic
  Realm UUID per isolated DB/session and consistent registration fixture.
  No developer credential/config fallback and no new container suite required.

### D. Distinct management and delegation semantics

| Fact / right | Accepted effect | Still not an allow decision |
|---|---|---|
| Ordinary grant possession | May be input to its own feature's functional checks | No right to manage accounts, grant or revoke another account's roles |
| Registered ACTIVE Realm | Establishes a valid Identity security resource | No actor permission, membership privilege or global administrator |
| Active actor + current valid session/authVersion + IDENTITY_MANAGEMENT on relevant Realm | Necessary management entry conditions | Does not by itself define assignability/delegation |
| Authority over target/current UNION requested exact grants | Required by accepted ManagementAuthority | Must be established from explicit approved local/owner policy, not request claims |
| Assigning or removing a grant | Requires exact manageable/assignable coverage, self/last-admin and transaction guards | No role-name wildcard, role/scope cross-product or hidden-grant removal |

**Remaining delegation decision — OWNER_DECISION_REQUIRED:**
Does a currently admitted Identity Management Admin in the registered SYSTEM
Realm have explicit authority to manage AND assign/revoke
IDENTITY_MANAGEMENT/IDENTITY/that-same-Realm for another already-scoped account?

Recommended smallest policy to approve explicitly: YES, for that exact pair
only, within this bounded replacement slice. Treat it as an explicit policy of
the dedicated management role, not as a general inference that holding any grant
permits delegation. No additional role or generic permission catalog. Require
nonempty existing target grants, all current/requested Identity pairs in the
same active registered Realm, and all existing Domain checks. Mixed external
grants remain denied until each owner API is approved. Actor cannot manage self;
last-active-admin protection remains unchanged for this single system Realm.
Initial account grants still belong to separately authorized provisioning.

If owner instead requires non-delegating managers, retain DENY for assignment of
IDENTITY_MANAGEMENT and specify a separate authoritative delegation capability/
bootstrap policy before enabling that path. Do not invent that capability or
an extra database table during this checkpoint. The recommendation above is NOT
accepted merely because the Realm architectural direction is accepted.

### E. Implementation boundary and future local transaction wiring

No new Realm production class, SQL migration, registry lookup or allowing
provider is implemented: authoritative persistence representation requires V4
implementation approval, and management-role delegation is not settled.
Adding a UUID value object or unbacked port alone would not establish registration.
The already implemented fail-closed composition and empty-target restriction
remain intact; no new authorization path is opened.

After those approvals, the smallest wiring is Identity-local:
existing composition -> current ReplaceIdentityGrants transaction -> local
GrantAuthority adapter -> same-transaction identity.realm lookup and explicit
accepted pair policy -> existing ManagementAuthority guard. No business-owner
imports, public Realm endpoint, cache, asynchronous/network call or global
permission service is needed for this first scope.

Keep Identity advisory administration lock -> actor/session/target lock order.
Read Realm under that held administration lock using the same transaction-bound
DataSource; all controlled Realm status changes/registration must acquire the
same Identity administration lock before mutation. Runtime remains SELECT-only
on realm; do not grant UPDATE just to add a row-locking clause. A separate owner
connection without this protocol cannot prove consistency. Activation/deactivation
must be a controlled owner operation with no arbitrary runtime bypass or fallback.

Revalidate active registered Realm and actor/session/target on every operation/
authorized replay; unknown/inactive/mismatched realm, missing registry or
unsupported delegation denies before receipt disclosure. Never rely on detached
snapshots. Preserve expected-version order, authVersion, deadline recheck, atomic
grant/session/refresh/audit/receipt writes and >=24h retention. Realm membership
cannot bypass Domain self-management or last-admin guards. No cross-module
transaction atomicity is claimed.

### F. Focused verification plan — not executed cases

Use the existing isolated PostgreSQL infrastructure and existing suite lifecycle;
do not add a fourth Testcontainers suite just for Realm. Extend existing suites
with exact report identities after implementation approval.

| Planned gate | Required evidence |
|---|---|
| REALM-01 cold start | Explicit synthetic registration creates exactly one INACTIVE SYSTEM Realm, owner ownership/runtime SELECT-only, complete V1..V4 history |
| REALM-02 safe upgrade | Synthetic V3 DB with matching IDENTITY scope upgrades with account/grant/Demo/session/audit/receipt data and V1/V2/V3 checksums preserved; no Identity grants case also valid |
| REALM-03 bad legacy mapping | Different/multiple IDENTITY UUIDs stop V4 atomically; old data/history remain; no inferred registration or rewritten grants |
| REALM-04 integrity | Unknown realm grant FK fails; external scope references remain unaffected; generated reference cannot be forged; referenced Realm cannot be removed/renamed |
| REALM-05 runtime privilege | Runtime cannot insert/update/delete/truncate/register/activate Realm or assume owner; existing grant DML remains permitted under its constraints |
| REALM-06 actual local policy | After explicit delegation approval, real provider checks registered ACTIVE same Realm and precise role/target sets; registration or ordinary grant alone never authorizes |
| REALM-07 denial boundaries | Unknown/inactive/missing realm, mismatched pair, unsupported role/external owner, self, disabled/stale/revoked actor and empty target all deny without mutation/disclosure |
| REALM-08 concurrency | Real transactions serialize Realm deactivation and grant replacement on approved local lock protocol; outcome reflects linearization order; last-admin races remain protected |
| REALM-09 regressions | All original33 grant/48 Domain/14 migration and newer21 authority cases retained; rerun/validate, replay/conflict/rollback/revocation/privacy/owned cleanup PASS on exact source |

All REALM-01..09 are a plan, NOT authored JUnit cases or reported PASS.
No implementation test is added for an invariant not implemented this turn.
Current source-derived inventory remains9 authority unit and45 grant PostgreSQL
methods; original33 plus12 authority PG. Existing21 authority additions are
runtime UNVERIFIED, as are any future Realm tests.

Actual command this checkpoint:
python -B -m unittest discover -s scripts/ci -p 'test_*.py':
**102/102 PASS**,38.157s, exit0. Synthetic report/policy tests, no Java,
PostgreSQL, Docker or backend launch; no CI dispatch. Historical run39641670408
does not cover the newer authority/empty-target source or future V4.

### G. Bounded implementation proposal and approval dependencies

| Approval item | Exact scope / dependency |
|---|---|
| REALM-P1 representation | Proposed one SYSTEM Realm, V4 table/generated FK, explicit UUID registration and fail-on-unmapped-legacy-data strategy; no organization/tenant mapping |
| REALM-P2 bootstrap | Owner/migrator registration INACTIVE, controlled activation/state change under existing Identity lock; no account/default grant created by registry migration |
| REALM-P3 delegation | Explicit recommended same-Realm IDENTITY_MANAGEMENT pair management/assignment/revocation rule above, or an owner-specified alternative; ordinary grant possession never enough |

Next implementation files, only AFTER approval:
- New V4__identity_system_realm.sql; a focused new Realm ADR/accepted contract
  semantic clarification and current migration/test configuration for its explicit
  UUID placeholder. Existing V1/V2/V3 remain immutable.
- Identity-local Realm value/lookup-policy adapter and explicit production
  composition changes needed for the approved policy, with no public business
  owner dependency. Do not build an enterprise IAM framework.
- Existing IdentityMigrationTests/IdentityGrantApplicationTests/Domain unit suites,
  PostgresTestcontainersSupport, PostgresTestcontainersTests, PostgresFoundationTests
  and actual PostgreSQL migration launchers/configuration that consume latest.
  Exact files in scripts/infra selected from their real current call sites during
  the implementation preflight, not broad launcher replacement.
- scripts/ci/verify-reports.py and test_verify_reports.py: finite new identities
  and honest version/table counts, no relaxed failures/skips/privacy/cleanup.
  Keep existing three check names; lifecycle unchanged unless a proven need arises.
- Only necessary evidence/tasks/supplemental trace; protected main trace remains
  untouched. No new HTTP/provisioning/organization/owner feature in this scope.

These three items form one small reviewable approval package. Registration
persistence is the immediate implementation prerequisite; delegation also must be
settled before a provider returns AUTHORIZED. Approval of architectural Realm
meaning alone is not permission to apply this SQL or activate management rights.

### H. Git and status

Branch feature/002-identity-application-ci, HEAD
1698940e4d4d888993daa2e38e995014c4f3282b, index empty.
This checkpoint changes only the existing Application evidence, tasks supplement
and supplemental traceability by appending this decision/design. All prior
source/tests/empty-target guard/strict inventory, six Foundation files, T-002-03/
Domain acceptance records, V1/V2/V3, accepted ADR/OpenAPI/public contracts,
workflow and local refs are preserved. No runtime/config/credential/ACL operation,
new migration file, Git synchronization/publication or new allowing provider.

Final verdict remains T_002_05_REALM_MIGRATION_APPROVAL_REQUIRED.
T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL.

Final checks PASS: only3 authorized SDD documents changed across484 tracked/
nonignored files; all other481 SHA-256 values unchanged. All3 prior document
byte prefixes preserved;8 new relative links/anchors resolve. UTF-8, bounded
disclosure and CRLF-aware git diff --check PASS. Same15 dirty paths, same branch/
HEAD/refs, empty index; no new/deleted path or migration file. Source-derived
counts remain9 authority unit /45 grant PG methods, not executed Java results.

## T-002-05-REALM-IMPLEMENTATION — approved local Realm provider — 2026-10-22

Authority: OWNER_REALM_P1 APPROVED_WITH_SAFETY_CONDITIONS,
OWNER_REALM_P2 APPROVED_WITH_CONTROLLED_BOOTSTRAP,
OWNER_REALM_P3 APPROVED_FOR_EXACT_SAME_REALM_IDENTITY_MANAGEMENT_DELEGATION.
**T_002_05_REALM_PROVIDER_IMPLEMENTED_VERIFICATION_PENDING** supersedes the
preceding migration/delegation approval blockers for this exact bounded scope.
It is source implementation acceptance only, NOT SQL/Java/runtime acceptance.
T-002-05 PARTIAL/unchecked; Feature002/AC-002-01..05 NOT ACCEPTED.

### Approved decision and source implementation

[ADR-0010](../architecture/decisions/ADR-0010-identity-system-realm.md) records
the exact owner-approved one-SYSTEM-Realm model, target scope-context predicate,
bootstrap and delegated exact-pair policy. [Operator procedure](../runbooks/identity-realm-bootstrap.md).
The [accepted contract README](../../contracts/identity-access/README.md) receives
an explicit semantic clarification; OpenAPI bytes/JSON fields/routes/statuses,
ADR-0009, DEC-03/04 and O1–O7 stay unchanged. No organization tenancy or generic
permission catalog is added.

| Part | Actual implementation | Runtime |
|---|---|---|
| REALM-P1 | V4__identity_system_realm.sql adds identity.realm; explicit identitySystemRealmId placeholder; INACTIVE registration; one SYSTEM code; IDENTITY-only stored generated reference/FK; unknown legacy ID causes transactional failure | UNVERIFIED |
| REALM-P2 | Owner-only SECURITY INVOKER bootstrap_system_realm_admin and activate_system_realm; unique bootstrap_account_id/first-activation metadata; fixed Identity lock; atomic provenance audit | UNVERIFIED; no privileged production execution |
| REALM-P3 | IdentityRealmGrantPolicy + JdbcIdentityRealmGrantAuthority installed in the default ProductionIdentityGrantComposition | Authored positive/negative/concurrency tests; UNVERIFIED |

V1/V2/V3 are not edited. V4 does not infer the Realm UUID from legacy data or
create a default active administrator. Any legacy IDENTITY scope differing from
the explicit registered ID aborts the migration; no rewriting, deletion, repair
or automatic registration of multiple legacy IDs. The FK uses NULL only for
non-Identity scopes; it does not claim ownership over external resources.
Runtime receives SELECT-only Realm access, with no registration/activation/DDL.
Existing account/grant/session/receipt/audit columns were inspected before adding
the new registry/binding fields. No Hibernate auto-DDL or dependency/version change.

Bootstrap INSERTs only a NEW PENDING_ACTIVATION account and exact Realm grant,
stores the authoritative initial-admin binding in runtime-read-only Realm state
and appends owner principal/request provenance in the same SQL transaction.
Existing UUID/username, duplicate bootstrap or audit failure rolls back all writes;
there is no account upsert/takeover or plain password input.

Realm activation checks the exact newly bootstrapped account, ACTIVE verified/
activated-credential state, consumed nonrevoked invitation created after registry
registration, and exact grant set. It then activates and appends audit atomically.
The function cannot create/consume invitation proof, set a credential, verify
email, run twice or reactivate a previously activated Realm. The initial
invitation workflow remains a separate unfinished Application use case. Until
that real workflow is completed and verified, production bootstrap must STOP
at pending state; fixtures simulating invitation are test-only, not a bypass.

### Trusted target scope context and production authority

The authoritative relation is existing identity.account_grant.account_id ->
current target user_account, with exact role/scope rows loaded after the existing
Identity advisory/account/session locks. For this restricted replacement:
- Current actor account/session/grants are reloaded/admitted by unchanged
  ReplaceIdentityGrants, including active state, revocation/deadlines/authVersion.
  Submitted actor claims, arbitrary session IDs and Demo principal are never
  authentication; later HTTP/security must authenticate the session binding.
- The JDBC provider requires an actual writable transaction bound to the SAME
  DataSource and verifies current-backend ownership of the existing administration
  advisory lock before reading Realm. Missing transaction/lock/registry denies.
- Exactly one registered ACTIVE SYSTEM Realm with completed controlled activation
  metadata is required. No request/config UUID is treated as live authority and
  no network/cache/cross-module lookup is used.
- Actor must hold the exact current IDENTITY_MANAGEMENT pair in that Realm.
  REALM-P3 explicitly authorizes management/assignability of that one dedicated
  privileged pair; possession of ordinary grants still delegates nothing.
- Target must be another existing account with NONEMPTY current grant context;
  ALL current AND requested grants must be that same exact pair. This is a
  target-bound complete relationship/state check, not inference from FK/existence
  alone. No account-to-tenant/organization membership invariant is introduced.
  Disabled targets retain their state according to accepted Domain behavior;
  no session/credential/lifecycle activation is performed by grant replacement.
- Same existing ManagementAuthority.requireManagement guard runs afterward.
  Self-management, last-active-admin/version guards, actor/target binding,
  authorize-before-replay and authorized-replay-before-stale ordering stay intact.
  All external/other-role/mixed/empty-target paths deny; no owner stub or fallback.

Default production composition now installs the genuine JDBC Realm provider
behind existing FailClosedGrantAuthority. The explicit internal delegate overload
is retained for trusted composition/tests; synthetic delegates are not installed
as production owners. Ordinary provider RuntimeException yields safe existing
ACCOUNT_NOT_ACCESSIBLE without cause/message disclosure; VM Errors are not
converted to authorization.

### Transactions, bootstrap safety and compatibility

Existing ReplaceIdentityGrants/JdbcIdentityGrantStore,24h minimum, fixed advisory
key5279420865908921433, READ COMMITTED/row locks/timeouts and atomic grant/version/
authVersion/session/refresh/audit/receipt effects remain unchanged. Bootstrap
functions take that SAME lock. Controlled Realm state maintenance must also take
it before writes and retain it through commit. Runtime cannot update registry;
no assumption of joined independent transactions or privilege grant for a
row-locking workaround is introduced.

Migration-enabled PostgreSQL profiles now require explicit IDENTITY_SYSTEM_REALM_ID
and pass it to Flyway. No global environment is changed by this task. Testcontainers
fixtures explicitly register a synthetic ID per database; Application fixture uses
its actual synthetic ADMIN scope ID. Existing temporary CI database provisioning
and local isolated-test launcher supply PG_TEST_IDENTITY_REALM_ID (test fixture
only); no production fallback or developer credential read. Local launcher edited
but not executed. Runtime-only migration-disabled usage needs pre-applied V4.

Latest migration history/table counters in existing tests are updated honestly to
V1..V4 and15 total owned platform/Identity/Demo tables. Explicit V1/V2 upgrade
coverage remains unchanged. Original Identity migration fourteen method identities
and assertions remain, with current-version expectations and explicit column list
adaptations where necessary. Original33 grant method bodies are retained;
new default-provider tests are additional. No skip category or test failure rule
is weakened. Same three Testcontainers suites/labels/cleanup registry; helper
changes are Flyway placeholder wiring plus isolated synthetic bootstrap fixture,
not lifecycle/ownership/cleanup logic.

The older full clean-clone checks have preexisting V1/V2/table-count assumptions;
they are not rewritten here or claimed PASS. That Foundation onboarding/config
reconciliation is separate. No developer bootstrap/data, Compose, credentials,
ACL/owner, native app or backend process was touched.

### Actual test inventory and authored cases

Source counts independently reconciled against the strict report inventory:
- FailClosedGrantAuthorityTests:9 retained.
- IdentityRealmGrantPolicyTests:8 NEW pure policy methods.
- IdentityMigrationTests:22 total =14 retained +8 NEW Realm/bootstrap methods.
- IdentityGrantApplicationTests:52 total =33 original +12 retained AUTH
  (including empty-target restriction) +7 NEW genuine provider methods.
- Accepted Domain48 unchanged. Existing21 AUTH additions retained and still
  runtime UNVERIFIED. New Realm Java methods total23, also UNVERIFIED.

New8 pure policy cases cover exact bounded authority/binding, no registered Realm,
ordinary-grant nondelegation, actual nonempty same-Realm target context, cross-Realm
actor, external/hidden/unrelated pairs, disabled/missing actor/input and existing
Domain self-management enforcement.

New8 migration/bootstrap cases cover INACTIVE registration/owner/runtime privileges,
unknown Identity FK/external compatibility/generated-column integrity, valid V3
upgrade/checksum/data preservation, atomic unmatched-legacy rejection, pending-only/
duplicate/takeover denial, invitation prerequisite/activation/replay/reactivation,
audit-failure rollback and wrong-bootstrap-target denial.

New7 production cases cover genuine same-Realm replacement/authorized replay and
revocation, inactive/unavailable registry sanitization, external/hidden targets,
current session/self/stale-version guards, atomic audit-failure rollback, required
same transaction/advisory lock, and real two-transaction Realm-deactivation-before-
admission denial. No permissive mock production provider is used for positive
production tests. Tests simulate invitation completion only inside isolated DB
fixtures, explicitly not production proof or mail-delivery acceptance.

Required FUTURE result counts (not actual Java outcomes):
Architecture23/0 skips; H2296 PASS/17 expected skips (313 discovered);
PostgreSQL395 PASS/5 expected skips (400 discovered). Exact report/privacy guards,
missing/failed/skipped method rejection and complete owned cleanup remain required.

### Actual process-safe checks and resolved failures

1. python -B -m unittest discover -s scripts/ci -p 'test_*.py':
   first104-test attempt55.484s exit1 with22 subtest failures. Cause: an existing
   negative XML fixture still declared13 tests after removing one method from
   the now22-method suite; the collector correctly rejected malformed counts.
   Fixed that fixture to explicit reviewed inventory length minus one, preserving
   exact missing-method reason assertions.
2. Same command after correction: **104/104 PASS**,50.539s, exit0.
   Existing102 controls plus2 Realm inventory/privacy/missing/failure controls.
3. python -B -m unittest discover -s app/backend -p 'test_identity_migration.py':
   initial15-test run exit1 with2 stale source inventory expectations (V1..V3
   filenames and14 methods). Updated only to exact V1..V4 and22 methods;
   immutable migration checks and all safety/negative source controls retained.
   Final **15/15 PASS**,0.017s, exit0.
4. Python AST/source count checks PASS. All8 collector function ASTs unchanged
   versus published HEAD; only finite identities/count expectations added.
   Testcontainers support lifecycle prefix/suffix unchanged; no registry or
   resource-accounting bypass. Final diff/link/UTF-8/hash checks reported below.

These are Python/static/synthetic controls, not executed Java/SQL. Java compilation,
SQL parser/migration execution, bootstrap privilege/atomicity, actual JUnit,
ArchUnit/current-source H2/PG and privacy/cleanup runtime remain UNVERIFIED.
No JVM/Maven/PostgreSQL/Docker/CI dispatch. Historical39641670408 covers1698940
only, not current Realm or earlier uncommitted AUTH source.

An initial automatic approval review referred to an obsolete four-file limit.
Read-only reinspection of the current owner task established explicit V4/
integration/report-policy authorization. Partial fixture application was inventoried
before completing the remaining authorized changes; the later scoped edits were
allowed. No indirect bypass, Git mutation or unresolved permission block remains.

### Scoped source fingerprints before closure append

| Current implementation/test/config/decision path | SHA-256 |
|---|---|
| app/backend/src/main/java/com/elogistic/backend/identity/internal/application/IdentityRealmGrantPolicy.java | `20c3fa6677899552f4250d240c761f2f7027091ad7c97a9ad6313f0dc4733a6e` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityRealmGrantAuthority.java | `0bb7a9f8e0d4a18faae6f9a1a8d7daa29fe96a00538c8aea99e77d19646ca8d7` |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityGrantComposition.java | `d3088e55b9b8562826812614d1f4d9112ace37240847b18dbfa7b6f54ec9e5ee` |
| app/backend/src/main/resources/db/migration/V4__identity_system_realm.sql | `c2254b9fc8f292e171f4fde49cda15c38c63c940b1c74a2ccae780ec94643062` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityRealmGrantPolicyTests.java | `6aa22baad11081773e267abea3336a7c7f36c67f92998909b5cdcac6804f6771` |
| docs/architecture/decisions/ADR-0010-identity-system-realm.md | `4743bb35f2f3cbda0d10b96bddf0fdafee28a8b9af3dea5bb7dc64281b8b3f0a` |
| docs/runbooks/identity-realm-bootstrap.md | `cc6431a5e27c770b8922868a81ad3140ad91de3577f9539e5e366c40dda6d26a` |
| app/backend/src/main/resources/application-migration.properties | `c3ee48edfe41497e93f193ea307c5cea861088c29c6855fa3833fa43fd9a4649` |
| app/backend/src/main/resources/application-postgres.properties | `9d36ec9fc03dfab835be4906fe09214b2737f50c947a0c357d64425434a60bb2` |
| app/backend/src/test/java/com/elogistic/backend/demo/DemoTestSupport.java | `a94f5f7a02d500aacce1cda8e8aa25c3377c35da654e1c4d80a2e39d9d593394` |
| app/backend/src/test/java/com/elogistic/backend/demo/PostgresFoundationTests.java | `3f07f002ec04fc276e02afa2394eb6bd39acb997fe77a974de513f3e28a0c55c` |
| app/backend/src/test/java/com/elogistic/backend/demo/PostgresTestcontainersTests.java | `cfcf6b7963a0fdbd2acbaeaa6bf8352db0e98cd313295a15f4c8457f951ba6c5` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java | `ac36c378f7d14d864d7a43ff7fd70612d1f3bc5ce4c2a6d0f40b04d3ecdba4a1` |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityMigrationTests.java | `5bdb2954de3f193baac9fa78b3aefef7513f57ffd12f9ac560f503b5351e7529` |
| app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java | `6e7d28aae7a15b92a03cf01fd4aec5bf337ab02052d2985aa4cf518c33632ba2` |
| app/backend/test_identity_migration.py | `f36bcf19ac6165e03b1802be93ce3230d4109ce6b2830254bdda68660ba65448` |
| app/backend/verify-postgres.ps1 | `cd07096627935a2c9dc02fbeaa2fa0d32dea86cb5d1af96fb28c85f89573220d` |
| contracts/identity-access/README.md | `ad9c98cb019f37f10905c10326d5a8768e2cf3a81000d76b178a1759562eb0e3` |
| scripts/ci/prepare-postgres.py | `890fb00c9ad793d21c19fb671b515b5d55a39722f4ecc0b1174daecd4e65ce74` |
| scripts/ci/test_verify_reports.py | `1dc4656a132e5b8c9e1ea2856aacd179646a62d8e62b3b8523307691ac3352f2` |
| scripts/ci/verify-reports.py | `ae69f5ed6a75d4ef4801b503c952e7d87a31dccc6d2514347f487503062f4cdf` |

### Runtime/publication handoff and remaining task

Before any runtime proof, owner must separately authorize a contained exact-source
Java/PostgreSQL path or scoped publication/hosted CI. Do not run the edited local
Wrapper launcher as an implicit fallback. Required hosted jobs stay Architecture
gate, Backend default / H2, Backend PostgreSQL 18. Their actual checkout must include
all source dependencies, new V4 and explicit synthetic UUID fixtures; exact finite
reports, privacy and all three-suite owned cleanup must PASS. No old run substitution.

Publication dependency closure is this Realm checkpoint plus the previously
unpublished FailClosedGrantAuthority/FailClosedGrantAuthorityTests and existing
AUTH changes already embedded in current files, while excluding six Foundation
paths. No staging/commit/push/PR/dispatch is authorized here. Bootstrap and invitation
proof simulation helpers must remain test-only.

T-002-05 still needs actual Realm/current-source verification, real accepted initial
invitation/provisioning and remaining Application use cases, and approved external
owner integrations. HTTP/session signature/CSRF/auth APIs are later tasks; no
Feature002/AC-002-01..05 acceptance. Six Foundation files, T-002-03/Domain acceptance,
V1/V2/V3 and accepted OpenAPI bytes are protected and preserved.

Branch feature/002-identity-application-ci / HEAD1698940e4d4d888993daa2e38e995014c4f3282b;
index empty, no history rewrite, branch sync, credential access, privileged
production bootstrap or external-resource mutation.

Final integrity checks PASS:24 checkpoint paths (6 new,18 modified),490 total
tracked/nonignored files,466 other hashes unchanged. Four existing document byte
prefixes preserved;13 new relative links/anchors resolve; UTF-8/bounded disclosure
and CRLF-aware diff checks PASS. Original33 grant method bodies unchanged against
published HEAD. Six Foundation paths, T-002-03/Domain acceptance, V1/V2/V3, accepted
OpenAPI, Domain source and minimum24h use-case/store logic preserved.
Same branch/HEAD/refs,32 dirty paths including all prior work, empty index; no
publication/runtime/credentials or resource mutation. Closure-document SHA-256
values are reported separately from this self-containing source manifest.

Bounded business limitation: because this slice permits only one exact management
pair, rejects an empty target and retains the accepted nonempty replacement
contract, its currently allowed replacement preserves that pair while applying
version/audit/session-revocation effects. It does not demonstrate first-time grant
assignment or removal of the last pair. Initial assignment belongs to provisioning;
other target role/resource combinations need approved owner authority. No empty
request, ordinary role or external scope is enabled to manufacture a broader
positive path. Full grant lifecycle/Feature002 acceptance remains pending.

## T-002-05-PROVISION — bounded invitation provisioning implementation — 2026-10-22

OWNER_T_002_05_PROVISIONING_IMPLEMENTATION: GO. Source baseline/branch:
feature/002-identity-application-ci at4a90bafc00ddfd89ec9ea0023534027e6bf1a395,
empty index, six pre-existing Foundation dirty paths preserved.
OWNER_PROVISION_INITIAL_IDENTITY_PAIR: APPROVED_WITH_RESTRICTIONS was received
during preflight; its exact narrow extension is recorded in
[ADR-0010](../architecture/decisions/ADR-0010-identity-system-realm.md#bounded-ordinary-provisioning-extension--owner-approved-2026-10-22).

### Previously verified baseline, not new provisioning evidence

[Run39795074146 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39795074146)
completed SUCCESS on exact4a90baf: Architecture23 PASS/0 skips, H2296 PASS/17
expected skips, PostgreSQL395 PASS/5 expected skips. Domain48, authority9,
Realm policy8, migration22, grant integration52 and104 policy controls/job passed.
Both earlier PostgreSQL regression failures were corrected by two test changes:
15 exact owner tables including Realm, and generated-column428C9 distinguished
from independent42501 UPDATE privilege rejection. V1..V4 remained immutable.
This supersedes prior Realm runtime-PENDING statements for that source only.

### Canonical scope and explicit decisions

Accepted POST /api/v1/admin/users / provisionIdentityAccount requires username,
displayName, email and a nonempty unique grant set; Idempotency-Key is required.
No password, target ID, verification state, request If-Match or bootstrap operation
is accepted. Exact username/case/Unicode sequence and contacts are retained.
Email uses the accepted harness's full email format; malformed Unicode/NUL cannot
be persisted as PostgreSQL text. Email is contact data, NOT a unique identity:
no email-uniqueness constraint or verification bypass was invented.

Only an admitted actual Identity Management actor in ACTIVE registered Realm R
can explicitly request the exact initial management pair for R. New target ID
is generated server-side. Membership is established by atomic creation of that
account/grant with the existing V4 Realm FK; no independent account-tenant model.
New accounts stay PENDING_ACTIVATION, UNVERIFIED, version/authVersion1, without
password or session. Empty/mixed/external/other-role/cross-Realm requests deny.
The old empty-target replacement guard, self/last-admin guards and bootstrap
procedures remain unchanged. This is not full grant lifecycle completion.

### Implemented code and transaction behavior

- IdentityProvisionCommands exposes the accepted backend command and reuses the
  safe Account/Result DTOs; AUTHORITY_REQUIRED/USERNAME_CONFLICT are existing
  accepted wire error codes added to the internal command error enum.
- ProvisionInput validates payloads, rejects duplicate/malformed grants, and
  fingerprints exact contact/username plus sorted grant pairs. It never accepts
  an initial password; command/input string representations are redacted.
- ProvisionIdentityAccount and ports IdentityProvisionStore, ProvisionAuthority,
  InvitationIssuer implement admission, authority, pending creation, invitation
  metadata, audit and receipt orchestration. No HTTP or external mail adapter.
- JdbcIdentityProvisionStore reuses JdbcIdentityGrantStore's existing transaction,
  READ_COMMITTED, administration lock and timeouts. All inserts use the same
  runtime DataSource. Username conflict is INSERT-only/DO NOTHING, never takeover.
- JdbcIdentityRealmGrantAuthority shares its existing registry/transaction/lock
  checks between separate new-account and replay authority paths. Replacement
  still delegates to the unchanged nonempty-current-target policy.
- ProductionIdentityProvisionComposition installs the genuine JDBC authority.
  It is explicit trusted infrastructure composition, not Demo/HTTP registration.

Ordering: administration lock -> current actor/session admission -> payload ->
internal actor/key receipt lookup -> current new-target/full stored replay-target
authority -> deadline recheck -> authorized replay/conflict -> username uniqueness ->
required invitation adapter -> pending Domain creation -> prepared proof validation ->
deadline recheck -> account/grants -> proof -> same-transaction durable intent ->
deadline recheck -> audit -> receipt -> authorized result.
Receipt lookup is internal only; no receipt/existence/contact result is returned
before authority. Replays precede username conflict and do not regenerate invitation.
Different keys for one username serialize and only one insert can win. Receipt
retention is at least24h. Invalid proof metadata or any mandatory write/intent/close
failure rolls the transaction back. No external delivery inside the transaction.

### Explicit durable invitation dependency

V3/V4 contain action_proof hash/state, but NO durable invitation delivery-intent
table. No V5, provider, key configuration or cryptographic implementation has
been invented. InvitationIssuer.Prepared keeps raw material private to a future
reviewed adapter; only proof metadata and a32-byte protected token digest cross
the port. It must enqueue a durable protected intent on the SAME transaction,
then destroy transient material. Missing adapter raises
PROVISIONING_INVITATION_ADAPTER_REQUIRED before any account/grant/proof write;
this internal configuration failure is not a new accepted HTTP response mapping.

The production default is therefore safe but NOT deployably usable provisioning.
A separately approved protected delivery schema/key/issuer implementation is a
mandatory completion dependency. Do not inject a synthetic fixture into deployment.

### Tests authored versus executed

New12 IdentityProvisionInputTests methods cover exact identity/contact, Unicode
bounds, missing keys, invalid/duplicate grants, full email format, canonical
fingerprints, immutability/redaction, exact initial authority/new-target binding,
scope denial, pending-state eligibility, unchanged replacement denial and
pending/unverified/version1 Domain result.

New18 PostgreSQL methods are appended to IdentityGrantApplicationTests (70 total;
all52 original method bodies retained). They use the real JDBC Realm provider.
Only invitation preparation/delivery is a clearly marked test fixture: fresh
test-only identity.synthetic_invitation_intent contains proof/account IDs and
SYNTHETIC_NO_TOKEN, never a token/ciphertext. Owner-only fixture DDL in isolated
test databases is not a production migration. It proves transaction participation,
not secure token generation, real durable outbox compatibility or email delivery.

Authored cases cover pending account/grant/proof/audit/receipt/test-intent state;
exact replay and changed-payload conflict; duplicate username; missing/revoked/
expired/stale/disabled actor; inactive/missing/cross-Realm/mixed/external authority;
missing adapter; malformed digest, wrong purpose/subject/lifetime/consumed proof;
grant/proof/delivery/audit/receipt fault rollback; concurrent same-key and username
races; current target grant union and session replay guards; no Realm activation/
bootstrap/session side effects; real competing actor/Realm state changes.

Actual process-safe checks:
- python -B -m unittest discover -s scripts/ci -p 'test_*.py':
 107/107 PASS,60.835s, exit0. Three new strict inventory/privacy controls; all
 old104 preserved. New methods missing/renamed/failed/error/skipped are rejected.
- python -B -m unittest discover -s app/backend -p 'test_identity_migration.py':
 15/15 PASS,0.018s, exit0; immutable migration/source guards retained.
- Java compilation/JUnit, current-source ArchUnit/H2/PostgreSQL, production crypto/
 delivery, runtime logs and current-source Testcontainers cleanup: UNVERIFIED.
 The earlier one-run CI authorizations are consumed; no new runtime path is inferred.
 No local JVM/Maven/Docker/native execution or GitHub dispatch in this checkpoint.

Required FUTURE inventories (NOT measured outcomes): Architecture23/0 skips;
H2308 PASS/17 expected skips (325 discovered); PostgreSQL425 PASS/5 expected skips
(430 discovered). Migration22, prior Domain48, authority9 and Realm8 remain.
Three Testcontainers suite ownership/cleanup registry, all collector functions,
prior method identities, required skips and CI workflow are unchanged.

### Status and scoped verification handoff

T_002_05_PROVISIONING_IMPLEMENTED_VERIFICATION_PENDING applies only to these
bounded components; deployable invitation delivery remains BLOCKED on its explicit
schema/protection/issuer decision and implementation. Do not describe the positive
test fixture path as complete production provisioning or bootstrap activation.
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains for the replacement operation.
T-002-01..04 DONE; T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED.
Feature001/D1-08/Day1 PARTIAL. Main traceability and all six Foundation paths,
T-002-03 evidence, accepted OpenAPI/REQ/AC, V1..V4 and original Domain preserved.
No staging, commit, push, PR, merge, workflow dispatch, credentials or developer
services. Source/runtime verification must use a separately authorized current
revision after the narrow delivery dependency is resolved or explicitly bounded.

### Final source-only checkpoint inventory

Exactly19 task-owned paths (9 new,10 modified); six unrelated Foundation paths
remain unstaged and byte-identical to the preflight. Original52 grant test bodies,
replacement policy body and four SDD/ADR original byte prefixes preserved.
Finite import/delimiter/UTF-8/disclosure checks PASS;71 relative documentation
file links resolve. Tracked-delta and new-file whitespace checks PASS. These
static checks are not Java compilation. Final focused inventory controls after
fixture/validation edits:3/3 PASS,2.708s, exit0. No additional runtime or Git action.

```text
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityProvisionCommands.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/IdentityRealmGrantPolicy.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ProvisionIdentityAccount.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ProvisionInput.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentityProvisionStore.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/InvitationIssuer.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/ProvisionAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityProvisionStore.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityRealmGrantAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityProvisionComposition.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityProvisionInputTests.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java
docs/architecture/decisions/ADR-0010-identity-system-realm.md
docs/implementation/t-002-05-identity-application-evidence.md
docs/quality/feature-002-recovery-traceability.md
scripts/ci/test_verify_reports.py
scripts/ci/verify-reports.py
specs/002-identity-access/tasks.md
```


## T-002-05 Invitation Outbox checkpoint — 2026-10-23

**T_002_05_INVITATION_OUTBOX_IMPLEMENTED_VERIFICATION_PENDING.**
Owner scope: `OWNER_T_002_05_SECURE_INVITATION_STORAGE: GO` and
`OWNER_T_002_05_TRANSACTIONAL_OUTBOX: CONDITIONAL_GO`. T-002-05 stays PARTIAL;
Feature 002 and AC-002-01..05 stay NOT ACCEPTED. This supersedes the prior
missing-production-issuer/outbox *source* dependency only; it does not replace
pending provisioning/runtime verification with historical green CI.

### Schema and bounded security decision

[ADR-0011](../architecture/decisions/ADR-0011-identity-invitation-outbox.md)
records the approved bounded implementation. V3/V4 cannot recover a delivery
secret after commit; `SYNTHETIC_NO_TOKEN` is only a transaction test fixture.
Append V5__identity_invitation_outbox.sql; never edit V1-V4. Preserve legacy
proof rows with NULL verifier_key_id. New outbox rows have exact composite proof
binding, unique proof/message identities, Realm FK, protected payload, lease,
attempt/retry metadata and fixed outcome codes. Runtime gains SELECT/INSERT/UPDATE
only on this Identity table, no DELETE/TRUNCATE/owner/global privileges.

Issuer: 256-bit CSPRNG token, 43-byte unpadded Base64url; HMAC-SHA-256 over the
versioned proof/account/purpose/time/key context; MessageDigest.isEqual comparison.
Delivery: independent AES-256-GCM key, random 12-byte nonce, 128-bit tag, exact
context AAD including private destination/Realm and stable message identity.
PostgreSQL microseconds are canonicalized before HMAC/AEAD. Raw token is never
stored plaintext, passed as a JDBC parameter, returned in Account, receipt/audit,
logged or sent to an external provider. Returned internal worker material is
transient, redacted and closeable/wiped. Java cannot promise erasure of provider,
GC or historical storage copies; no such claim is made.

External composition receives independently supplied verifier/encryption key rings
and explicit lease/retry policy. No secret file read, default key or environment
mutation was introduced. Invalid/missing/shared-purpose keys fail closed. Current
IDs issue; retained old IDs read. Rotation must retain live proof/payload keys;
unknown keys deny. Production sender/worker configuration remains unapproved and
unregistered, with no SMTP/provider calls or automatic provisioning bootstrap.

### Atomicity, replay and delivery boundary

The existing trusted actor/session/authVersion, exact same ACTIVE Realm delegation,
initial pair, administration lock, username uniqueness and >=24h receipt retention
remain. Exact authorized replay returns before crypto preparation. New commands
prepare protected material then append account + grants + proof + durable intent +
audit + receipt in the existing runtime DataSource transaction. Any mandatory
persistence error rolls everything back. No external delivery occurs inside it.

Internal JdbcInvitationOutbox claims one pending/recoverable row under the existing
administration lock and FOR UPDATE/SKIP LOCKED, with 1-10 attempts, 5-300s lease,
1-300s retry delay. Open rechecks current proof/account/destination/Realm/grant
eligibility and verifies AEAD/HMAC from a fresh DB read. Stale leases cannot open
or acknowledge. SENT/permanent failure/exhaustion clears protected payload;
expired/consumed/revoked/ineligible intents are purgeable in bounded 100-row batches.
No domain account/proof/audit/receipt is removed or activated by delivery outcomes.
QUEUED != SENT != CONSUMED != ACTIVE. Transport is at-least-once; provider dedupe
on stable message identity is a future sender requirement, not exactly-once email.

**Retention limitation:** logical expiry is enforced at 24h; physical ciphertext
purge is invocation-driven, not a running scheduled service. Production enablement
requires a maintenance schedule (also while sending is disabled), backlog monitoring,
key retention/rotation and DB/backup retention policy. No physical purge SLA or
production delivery readiness is claimed before those integrations are verified.

### Authored versus executed verification

| Gate | Current result | Evidence / scope |
|---|---|---|
| Python report-policy controls | PASS 110/110, 58.866s | `python -B -m unittest discover -s scripts/ci -p 'test_*.py'` |
| Static Identity migration + outbox source controls | PASS 25/25, 0.372s | `python -B -m unittest discover -s app/backend -p 'test_identity_*.py'`; 15 prior controls + 10 new source controls, including 10 schema mutation subcases and a comment positive control |
| Exact finite inventory/privacy negatives | PASS within 110 controls | missing/renamed/failed/error/skipped new crypto and PG methods reject; private diagnostics never enter public reports |
| Collector algorithms / Foundation integrity | PASS | all 7 collector function AST bodies unchanged against HEAD; six protected Foundation hashes, T-002-03 evidence and accepted OpenAPI hashes match recorded baseline |
| Java compilation / JCE unit execution | UNVERIFIED | no fresh authorized Java/Maven execution; 10 genuine crypto/policy JUnit methods authored |
| Real PostgreSQL V5, outbox, rollback/concurrency | UNVERIFIED | 15 real secure issuer/outbox methods + 3 V5 migration methods authored; not replaced by synthetic fixture evidence |
| ArchUnit / H2 / PostgreSQL current revision | UNVERIFIED | exact required inventory prepared; no runtime or CI dispatch authorized |
| Physical purge scheduling / SMTP / activation consumer | NOT IMPLEMENTED | separate approved worker/provider and activation integration needed |

The earlier Python attempts found stale finite totals (22->25 migration methods,
H2 308->318, PG 425->453). These were corrected by adding the exact new reviewed
identities; no assertion/skip/privacy/cleanup waiver. Final results above supersede
those source-control failures only. The previous run39795074146 validates published
4a90baf, NOT any uncommitted provisioning/V5/outbox source.

New Java methods (all execution UNVERIFIED): InvitationProtectionTests10;
IdentityGrantApplicationTests secure-prefix15; IdentityMigrationTests invitation-prefix3.
The previous 70 grant/provisioning methods and 22 migration methods remain required.
Only complete-stream/table inventory expectations and the synthetic issuer's added
destination argument were adapted. Full expected runtime inventory:

| Mode | Expected passed | Expected skips | Discovered |
|---|---:|---:|---:|
| Architecture | 23 | 0 | 23 |
| H2 | 318 | 17 | 335 |
| PostgreSQL 18 | 453 | 5 | 458 |

PostgreSQL includes 85 grant/application methods, 25 migration methods, 10 crypto
unit methods, all 48 Domain methods, existing Realm/authority/Demo/foundation/safety
regressions. The three owned Testcontainers suites and cleanup policy are unchanged.
Expected skips retain their precise prior classification; they are not optional new
tests. New outbox tests must pass, never skip, in the PostgreSQL job.

### Git safety and exact scoped working manifest

Branch feature/002-identity-application-ci; HEAD
4a90bafc00ddfd89ec9ea0023534027e6bf1a395, index empty. No stage/commit/push/dispatch/PR.
Six Foundation paths remain separate/protected. The scoped accumulated working
manifest has 30 task paths: all 19 prior Provision paths retained plus 11 additional
paths (including V5, crypto/outbox adapters/tests, regression inventory and ADR).
This is an accumulated publication candidate, not a claim all 30 were newly created
by Invitation Outbox. No SQL was executed; V1-V4, contract, T-002-03 evidence,
workflow, POM, dependencies, developer services and credentials remain unchanged.
Sanitized SHA-256 manifest is retained privately at
`.demo/t-002-05-invitation-outbox/assessment.json`; no key/token is placed there.

```text
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java
app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityProvisionCommands.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/IdentityRealmGrantPolicy.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ProvisionIdentityAccount.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/ProvisionInput.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentityProvisionStore.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/InvitationIssuer.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/ProvisionAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/InvitationProtection.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityProvisionStore.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityRealmGrantAuthority.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcInvitationOutbox.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityProvisionComposition.java
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/SecureInvitationIssuer.java
app/backend/src/main/resources/db/migration/V5__identity_invitation_outbox.sql
app/backend/src/test/java/com/elogistic/backend/demo/PostgresFoundationTests.java
app/backend/src/test/java/com/elogistic/backend/demo/PostgresTestcontainersTests.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityProvisionInputTests.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityMigrationTests.java
app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/InvitationProtectionTests.java
app/backend/test_identity_invitation_outbox.py
app/backend/test_identity_migration.py
docs/architecture/decisions/ADR-0010-identity-system-realm.md
docs/architecture/decisions/ADR-0011-identity-invitation-outbox.md
docs/implementation/t-002-05-identity-application-evidence.md
docs/quality/feature-002-recovery-traceability.md
scripts/ci/test_verify_reports.py
scripts/ci/verify-reports.py
specs/002-identity-access/tasks.md
```

### Remaining canonical Application work

Real sender/worker (including retention scheduling), invitation acceptance and
password setup with atomic proof consumption, recovery request/confirmation,
login/refresh/logout/session admission, account/role/scope queries, disable/reactivate,
resend and remaining resource-owner authority integration are not completed by this
checkpoint. Preserve GRANT_MUTATION_FUNCTIONALITY_LIMITED: current replacement
still permits only the exact bounded pair and meaningful grant changes remain
unproven. HTTP/security, browser flows and full Feature 002 AC remain pending.

Next single gate: owner-authorized scoped current-revision compilation and CI
verification of Provision + V5 + crypto + durable outbox, retaining all report,
privacy and owned Testcontainers cleanup guards, before worker/activation expansion.


## Provisioning and Secure Outbox CI closure - 2026-10-23

**T_002_05_PROVISION_OUTBOX_CI_VERIFIED**, for this bounded Application checkpoint
only, under OWNER authorization GO - EVIDENCE VERIFICATION ONLY.
[Actual GitHub Actions run](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39896561084),
attempt **1**, event **workflow_dispatch**, workflow
`.github/workflows/backend-verification.yml`, branch
`feature/002-identity-application-ci`, tested SHA
**0098461f2c4b242956c401430e5d5ca304521eb8**. Actual run and all three required
jobs completed SUCCESS. Each job log contains the exact checkout SHA and confirms
110 report-policy controls completed successfully. The workflow uses the unchanged
strict inventory, expected skips, privacy and owned-resource cleanup policies.

This supersedes IMPLEMENTED_VERIFICATION_PENDING for the exact Provisioning + V5
+ Crypto/Outbox source above and the FIX-03 dispatch blocker. It does not claim
any later documentation edits were tested by this run. The previous run39889957889
on3304a670 remains historical failing evidence, not current acceptance.

### Actual sanitized results and scope

| Job | Discovered | Executed / PASS | FAIL | ERROR | Expected skips |
|---|---:|---:|---:|---:|---:|
| Architecture gate | 23 | 23 | 0 | 0 | 0 |
| Backend default / H2 | 335 | 318 | 0 | 0 | 17 |
| Backend PostgreSQL 18 | 458 | 453 | 0 | 0 | 5 |

H2 skips are the existing 8 PostgreSQL foundation + 4 PostgreSQL health safety +
5 real Redis/Rabbit foundation methods; PostgreSQL skips only those 5 real
Redis/Rabbit foundation methods. Testcontainers classes remain explicitly
PostgreSQL-only discovery; no absent mandatory or unexpectedly skipped test was
accepted. XML class/method/status inventory exactly matches the reviewed policy
for every job, with no duplicates or extra methods. No rule/count/assertion was
weakened to accept these reports.

PostgreSQL evidence includes overlapping inventories (do not sum these subsets):

| Inventory | Actual PASS |
|---|---:|
| Identity Domain | 48/48 |
| Realm unit / authority unit | 8/8 and 9/9 |
| IdentityMigrationTests | 25/25, including all 3 V5 methods |
| IdentityGrantApplicationTests | 85/85 |
| Provisioning unit | 12/12 |
| Provisioning PostgreSQL | 18/18 |
| InvitationProtectionTests crypto/unit | 10/10 |
| Secure issuer/outbox PostgreSQL | 15/15 |
| Realm PostgreSQL subset | 7/7 |

`provisionWaitsForConcurrentActorAndRealmStateBeforeAdmission` is now **PASS**.
FIX-03 changes only that test and its directly required helper: expected data is
read through the authority writer connection after its deliberate actor/Realm
change, while the administration lock is still held; after commit and exact
SESSION_INVALID/AUTHORITY_REQUIRED denial, a fresh runtime connection must match
all 10 snapshotted tables. Both actor and Realm scenarios and the lock-wait gate
remain; an explicit unfinished-future assertion was added. Account/grant/proof,
real and synthetic intent, audit/receipt/session and Realm state are all checked.
The earlier sanitized XML did not expose the precise failing assertion; no raw
Maven diagnostics were used to invent one. Source diagnosis and the current
passing runtime now establish acceptance of this test-only correction.

V1-V5 cold start, V4-to-V5 legacy-data/checksum preservation, proof association,
uniqueness, protected shape, table ownership and runtime privilege negatives all
passed. Provisioning atomicity, missing-dependency/fault rollback, current Realm /
session/authVersion admission, replay/key conflict/username races, encrypted fresh
DB read, AEAD/HMAC tampering/rotation, lease/reclaim/outcome/retry/expiry/purge and
unchanged Domain/Realm/Grant/Demo/safety regressions passed through their exact
required test inventories. This is test evidence, not production deployment.

### Artifact integrity and privacy

Three sanitized artifact ZIPs were downloaded with the existing GitHub CLI at
`C:/Program Files/GitHub CLI/gh.exe`, using read-only `gh api` artifact endpoints.
Each ZIP SHA-256 matched its GitHub artifact digest; **3/3 PASS, none UNVERIFIED**.
Only summary.json/tests.xml are present in each ZIP; no raw Maven diagnostics,
properties, system-out/system-err or unapproved testcase metadata were published.
Artifact summaries have zero problems and exact totals agree with sanitized XML.

| Artifact | SHA-256 verified against GitHub metadata |
|---|---|
| backend-postgres-1 | 87bfc72210ef6fa9c4e321acbea9dfc1d19a4bb13f5836fff3735a01e41f8eb5 |
| backend-h2-1 | c4aa1c1d65ba1c3c1d8d5f2ddb922a7db219c80f7e3166f9b2f97c2a923774e4 |
| backend-architecture-1 | 928521f3bf9d5668da8d29d9acc94ccf8187b7e6fb8c04373492ec1d9433e399 |

Job-log projections were obtained through read-only GitHub API after the CLI job-log
endpoint failed. Only allowlisted checkout/policy/privacy/cleanup facts were saved;
raw job log content was processed in memory and not retained as evidence files.
All jobs passed the temporary DB credential log audit. PostgreSQL additionally
passed generated Testcontainers credential privacy and owned-resource cleanup
audits; its final owned temporary PostgreSQL cleanup step succeeded. The existing
three-suite lifecycle accounting was retained, not inferred from old resource IDs.

Private isolated evidence packet:
`.demo/t-002-05-ci-closure-39896561084/` contains CLI-downloaded ZIPs, extracted
sanitized files, GitHub metadata, exact inventory verification, sanitized job
projections and acceptance.json. No raw credentials or private Maven diagnostics
were copied into tracked documentation. These artifacts are not committed by this
closure task.

### Git and acceptance boundary

Verification ran against local HEAD0098461f, with an empty index and only the six
protected Foundation changes before documentation edits. Their SHA-256 fingerprints
were checked against the retained baseline; source, V1-V5 SQL, tests/assertions,
OpenAPI, ADR/security/CI policies and T-002-03 evidence remain unchanged.
Only this evidence file, Feature002 tasks and supplemental traceability are updated
after CI. The run does **not** test those subsequent Markdown edits. No stage,
commit, push, dispatch, PR or merge was performed in this closure.

**T-002-05 stays PARTIAL/unchecked. Feature002 and AC-002-01..05 NOT ACCEPTED.**
Remaining: approved email sender/worker, maintenance and scheduled physical purge,
invitation consumption, password setup/account activation, other canonical
Application use cases, later HTTP/browser/integration acceptance.
**GRANT_MUTATION_FUNCTIONALITY_LIMITED** stays recorded: this initial-pair
provisioning acceptance does not authorize general effective grant replacement.
QUEUED, SENT, CONSUMED and ACTIVE remain distinct; no actual email delivery,
production worker, activation or bootstrap execution is implied.
