# T-002-05 — Production Identity Application: first slice

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
