# T-002-03 — Production Identity persistence implementation

Latest source checkpoint: **RI_LIFECYCLE_01_CORRECTED_CI_READY** under
OWNER_RI_LIFECYCLE_01_CORRECTION: GO. Per-suite lifecycle accounting and existing
workflow wiring are corrected;88 process-free policy tests PASS. Java publication/
teardown, compilation and PostgreSQL remain UNVERIFIED, not runtime PASS.
T-002-03 stays IMPLEMENTED_VERIFICATION_PENDING. See
[current correction/handoff](#ri-lifecycle-01-source-correction--2026-10-21).

Checkpoint: 2026-10-21. **T_002_03_IMPLEMENTED_VERIFICATION_PENDING**.
Authority: OWNER_T_002_03_IMPLEMENTATION_APPROVAL: GO. No runtime authorization
covering this exact migration/test revision was established; no Java/Maven/H2,
Docker/PostgreSQL, credential access or developer service operation was performed.

## SDD and baseline

Branch `feature/ac-001-04-s3a-runtime-fail-fast`, HEAD
`e8dd52c6baff25ad5c75b80a85e7c81a9f85587f`; index empty. Pre-task public source
inventory449 files,22 modified/untracked working paths, preserved in place.
An ignored, hash-only preflight record is `[REDACTED_PRIVATE_PREFLIGHT_RECORD]`;
it contains no credentials and does not copy protected source or raw telemetry.

Read root AGENTS, [SDD workflow](../README-SDD.md), [PRD](../prd.md),
[principles](../principles.md), [DoR](../codex/definition-of-ready.md),
[dependency order](dependency-order.md), [feature register](../../specs/INDEX.md),
[canonical spec](../../specs/002-identity-access/spec.md),
[design](../../specs/002-identity-access/design.md),
[tasks](../../specs/002-identity-access/tasks.md),
[ADR-0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
[contract semantics](../../contracts/identity-access/README.md),
[accepted contract](../../contracts/openapi/identity-access.yaml),
[T-002-02 acceptance](t-002-02-identity-contract-evidence.md),
[ownership](../architecture/data-ownership.md), [security](../architecture/security.md),
[module catalog](../architecture/module-catalog.md),
[transactions](../architecture/transactions-events.md),
[state machines](../domain/state-machines.md), [business rules](../domain/business-rules.md),
[test strategy](../quality/test-strategy.md),
[PostgreSQL runbook](../runbooks/postgresql-foundation.md), migrations,
Demo account repository, PostgreSQL regressions, Testcontainers support/policy,
POM, profile configuration and strict CI report inventory.
Original report DOCX was not available for direct validation in this task;
canonical source references are used, not invented report quotations.

Canonical T-002-03 DoD: identify Identity model; Flyway apply from empty AND
upgrade; prove indexes/constraints/ownership and preserve existing schema.
T-002-02 accepted contract SHA-256 remains
`acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128`.
Its512 checks/strict lint are historical accepted evidence, not rerun here.
Explicit current approval authorizes this model/migration implementation, not
database execution or amendments to ADR/REQ/AC. Scope splits unresolved full
Foundation runtime work from process-free migration implementation.

## Actual inventory and physical model

Observed PostgreSQL location before edits:
`app/backend/src/main/resources/db/migration/V1__ownership_schemas.sql` and
`V2__demo_identity_merchant_drafts.sql`, plus `.gitkeep`. Therefore next version
is3; implemented file is
[V3__production_identity.sql](../../app/backend/src/main/resources/db/migration/V3__production_identity.sql).
The earlier handoff's longer V3 filename was a proposal, not an applied migration.
H2 has its independent demo location; it is not changed or used to verify V3.
Global history stays `platform.flyway_schema_history`; migrator SET ROLE selects
NOLOGIN owner, runtime receives explicit DML only. No BEGIN/COMMIT, IF NOT EXISTS,
baseline, clean, repair, extensions, prior-schema mutation or secret seed SQL.
Flyway supplies the transactional boundary and rerun/checksum detection.

| Physical table | Requirement / decision | Constraints and ownership |
|---|---|---|
| identity.user_account | REQ-002-01/03/05, DEC-04/O4/O6 | UUID, exact C-collated unique username,1..80 code points/nonblank ECMAScript whitespace validation without transforming stored text; display/email; nullable pending BCrypt hash; state/activation/verification/disable combination; positive version/auth_version; UTC timestamps |
| identity.account_grant | REQ-002-01/04, O5 | account/role/scope-type/public-ID composite PK, no duplicates; five contract scope types; account FK only |
| identity.auth_session | REQ-002-02/05, DEC-03/O3 | session ID also refresh-family ID; account FK, auth_version, last meaningful activity, idle/absolute deadlines, revocation; maximum8h/30min row-local bounds |
| identity.refresh_token | REQ-002-03/05, DEC-03/O3 | unique32-byte token hash; same-family predecessor FK, unique successor, single family root and single unconsumed/unrevoked member; issued/expiry/consumed/revoked times |
| identity.action_proof | REQ-002-03/05, DEC-04 | invitation/recovery purpose, account FK, unique32-byte hash; maximum24h/15min; consumption/revocation; one outstanding proof per account/purpose |
| identity.csrf_binding | REQ-002-02/03, O2 | server preauth/session binding, unique cookie hash, challenge hash, optional session FK, expiry/revocation; no plaintext cookie/challenge |
| identity.command_receipt | REQ-002-04/05, O5/O7 | actor/proof/preauth typed FKs, generated binding ID, operation+UUID key PK, keyed-input fingerprint storage, target/version/status metadata, timestamps; preauth recovery target is NULL for both eligible/ineligible inputs |
| identity.audit_entry | REQ-002-04, O5/O7 | Identity actor/target FKs, event/source/request/time and bounded reason; runtime SELECT/INSERT only, no arbitrary secret-bearing JSON payload |

Eight tables,16 explicit indexes (in addition to PK/unique backing indexes).
No table is created in another module. No external scope FK or automatic
role/Finance/Operations permission seed. Role codes and public scope IDs are
references, NOT proof of assignability; catalog/permission semantics remain
default-deny application code backed by approved public authority contracts.
No separate catalog/permission CRUD tables are invented without a use case.

SHA-256 digest width is a storage implementation choice for high-entropy opaque
token hashes, not a claim of runtime randomness or hashing execution. Password
hash shape checks are not BCrypt execution or password-strength validation.
Application must enforce password byte/codepoint policy before BCrypt.

Production `user_account` is the sole new production account source of truth;
`demo_account` stays authoritative only inside existing Demo profile/trust domain.
No identity/credential import, username rewrite, seed bypass or API change.
V3 does not write any Demo row. Runtime Demo compatibility is still UNVERIFIED
until upgrade and regression execute. SQL grants do not establish role authority.

Row-local SQL limits do not prove cross-row state/security invariants. Later
canonical tasks must enforce current account/session state, token expiry bounded
by its session, compare-and-consume rotation, replay family revocation, no idle
refresh on polling, no self-management, last-admin serialization, proof eligibility,
exact-Origin/CSRF validation and atomic grants/versions/revocations/receipt/audit.
Expired-but-unconsumed tokens/proofs must explicitly be revoked/consumed before
replacement; partial indexes deliberately do not depend on wall-clock NOW().
Revoked/consumed rows and binding subjects must be retained for receipt replay;
foreign keys intentionally prevent premature retention cleanup. No retention
duration or purge job is invented. No runtime DELETE of accounts/sessions/proofs.

Receipt storage contains no raw request/response secret cache. Secret-bearing
input fingerprinting needs keyed hashing to avoid password/proof dictionary
exposure; implementation/secret-operation review belongs to later tasks.
Reconstruction of safe response snapshots and authority-before-replay are not
implemented here. Audit reason/source redaction is a future application test.
Sandbox delivery/outbox payload confidentiality/provider/worker behavior remains
T-002-05/08 design+implementation work; no speculative plaintext-proof outbox
column or delivery platform is introduced by this schema task.

## Files changed in this task

Exactly8 paths relative to the preflight (4 new,4 changed):

1. New migration linked above.
2. [IdentityMigrationTests.java](../../app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityMigrationTests.java):14 real PostgreSQL test methods, written but NOT COMPILED/NOT RUN.
3. [test_identity_migration.py](../../app/backend/test_identity_migration.py): stdlib-only fixed-source checks and mutation controls; no child process/network/DB write.
4. [PostgresFoundationTests.java](../../app/backend/src/test/java/com/elogistic/backend/demo/PostgresFoundationTests.java): exact owned-table total14 instead of6; historical V1-to-V2 upgrade explicitly targets2.
5. [PostgresTestcontainersTests.java](../../app/backend/src/test/java/com/elogistic/backend/demo/PostgresTestcontainersTests.java): complete stream now1/2/3, exact table total14, retain separate V1-to-V2 regression at target2. Method identities preserved for existing report policy.
6. [tasks.md](../../specs/002-identity-access/tasks.md): append implemented/pending status, leave T-002-03 checkbox open.
7. [supplemental traceability](../quality/feature-002-recovery-traceability.md): requirement-to-schema/test mapping; protected main file not edited.
8. This sanitized evidence/runtime authorization packet.

No production Java/REST/security/DTO, accepted contract/ADR/decision, POM,
dependency/version, CI/workflow, frontend/SDK, prior migration or configuration
change. No staged files or new Git branch/worktree.

## Verification actually executed

| Gate / command | Actual result | Exit / limitation |
|---|---|---|
| git status --short / branch --show-current / rev-parse HEAD / diff --cached --name-only | Expected branch/HEAD,22 pre-existing working paths, empty index preserved |0 |
| python -B app/backend/test_identity_migration.py |15 tests PASS,0 FAIL;15 mutation subcontrols reject expected reasons;1 nonpolicy comment positive control PASS |0; subcontrols are INCLUDED in15 test methods, not extra database tests |
| Same command after final refresh-root constraint/test addition |15/15 PASS on final SQL/Java/static revision |0; no cached result reused |
| SQL source/contract model inventory and delimiter checks | Eight Identity tables,16 explicit indexes;17 wire operations unchanged; eight idempotent operations represented in receipt schema | Lexical/source only, NOT PostgreSQL parsing |
| Optional installed parser availability check | pglast/sqlparse/sqlglot/javalang absent |0; no dependency installed; SQL parser / Java compilation UNVERIFIED |
| git diff --check | PASS |0; pre-existing LF/CRLF notices only |
| Exact allowlist, protected fingerprints, original migration/contract/manifest checks, UTF-8/local links and bounded disclosure checks | Final results recorded below | Source/privacy boundary only |
| Java compilation / PostgreSQL migration / negative SQL constraints / existing backend regression | UNVERIFIED / NOT RUN | No exact-revision runtime authorization; no process/service launch |
| Current GitHub CI | UNVERIFIED / NOT RUN | No publication permitted; existing strict report inventory lacks the new test class |

An initial read-only rg used nonexistent wildcard paths on Windows and exited1;
subsequent bounded searches used actual script directories successfully. This was
an inventory-command issue, not migration or database evidence. No retries of
runtime/security execution occurred.

### Prepared PostgreSQL suite, not executed

All methods in IdentityMigrationTests use a fresh database per method inside
one pinned isolated Testcontainers PostgreSQL instance; no Spring context or
developer URL, no skipped-on-Docker-unavailable annotation, no fixed port/reuse.
They reuse existing ownership/image/loopback/cleanup policy unchanged.

| Method | Expected assertions / trace |
|---|---|
| coldStartValidateAndRerunPreserveHistoryAndIdentityData | Empty V1/V2/V3 migrate; exact history; validate; rerun0; checksums and production account retained |
| upgradeV2PreservesAllDemoTablesWithoutImportingAccounts | Populate all five Demo tables; V2-to-V3 applies1; immutable prior checksums, existing Demo rows/receipt retained; zero automatic production imports |
| ownerOwnsEightTablesIndexesAndOnlyIdentityForeignKeys | Eight owner-owned tables, PK indexes, required lookup/partial indexes; no cross-module FK |
| usernamesRemainExactCaseUnicodeAndRejectDuplicatesBlankAndNull | Exact duplicate23505, NULL23502, length22001, blank Unicode23514; case, whitespace-surrounded and decomposed Unicode identities remain distinct |
| accountLifecycleRequiresVerifiedActivatedCredentialAndPositiveVersions | Active/disabled shape, credential/hash, positive versions; valid pending/activated/disabled/reactivated row representations |
| grantsAllowMultipleScopesButRejectDuplicatesInvalidScopesAndMissingAccounts | Multi-role/scope writes, duplicate23505, scope23514, absent account23503, NULL23502 |
| sessionsRejectInvalidDeadlinesMissingAccountsAndNullExpiry |8h/30min bounds, clock checks, missing account FK, NULL expiry |
| refreshHashesMustBeUniqueCorrectSizeAndHaveExistingSession | Hash uniqueness/length, session FK, expiry ordering |
| refreshLineageCannotCrossFamiliesForkOrHaveTwoLiveMembers | Same-family FK, single live member/root/successor, self-parent rejection; not a concurrent application rotation test |
| proofPurposeLifetimeHashAndSingleOutstandingProofAreEnforced | Purpose,24h/15min limits, uniqueness, consumed time and replacement representation |
| csrfSupportsPreauthAndSessionBindingButRejectsBadHashesAndReferences | Both binding representations; FK, hash width and clock negatives |
| receiptsBindActorProofOrPreauthWithoutExistenceDisclosureOrDuplicateKey | Preauth receipt without account; actor/proof subjects; duplicate key, wrong operation/binding and FK negatives |
| runtimeCanAppendAuditButCannotDdlDeleteAccountsOrMutateAuditAndReceipts | Runtime append works; UPDATE/DELETE/TRUNCATE/DDL/SET ROLE denied42501, audit subject FK |
| preexistingSchemaDriftFailsWithoutBaselineOrRepair | Deliberate isolated preexisting-table conflict fails V3; prior history/marker survive, no repair/clean |

## Bounded PostgreSQL authorization packet — NOT EXECUTED

Recommended environment: separately approved disposable Linux runner/session
with local Unix Docker socket, Java21 and already installed/verified Maven3.10.0.
Do NOT route current Windows execution around unresolved Foundation containment.
This is a proposed alternative requiring explicit owner approval, not a current
execution authorization. No Wrapper, H2, native build or developer DB is needed.
If approved Linux toolchain/socket/process containment is unavailable, STOP;
no automatic installation, daemon TCP exposure, Docker restart or retry.

### Mandatory pre-execution gates

1. Owner authorizes this exact migration/test/static SHA set and the Linux process
   isolation mechanism; recheck hashes, branch/HEAD/inventory and all11 Foundation
   hashes before execution. No receipt from another source revision is accepted.
2. Dedicated synthetic source snapshot and fresh private directory outside public
   artifacts, mode0700 at creation/umask077, no symlink/foreign directory reuse.
   Do not copy `.demo`, `.env`, protected store or developer credential settings.
   Private directory/ACL operations are confined to new owned Linux paths.
3. Inspect exact local Docker daemon/context/socket identity and resource baseline;
   never enable TCP, privileged containers or mount socket into PostgreSQL.
   Ryuk alone receives `/var/run/docker.sock`; disclose its daemon-wide deletion
   capability and obtain this task's explicit approval. Existing policy requires
   Ryuk enabled/unprivileged, checks enabled, no reuse, label/run/session ownership,
   loopback random ports and two allowed images only.
4. PostgreSQL image:
   `postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c`.
   Helper:
   `testcontainers/ryuk:0.14.0@sha256:7c1a8a9a47c780ed0f983770a662f80deb115d95cce3e2daa3d12115b8cd28f0`.
   Require already cached approved images; no unapproved pull/helper substitution.
   Testcontainers2.0.5 stays BOM-managed. Docker/service availability failure is
   FAIL/BLOCKED, not a valid skip.
5. Verify Maven binary and3.10.0 distribution provenance, Java21 binary, isolated
   Maven home/local repository, no inherited settings/mirrors/extensions/agents,
   telemetry options or auth environment. Dependency resolution/network, if
   needed, requires separately bounded approval; prefer approved warm cache
   copied without developer settings. No global environment mutation.
6. Resource/process preflight must approve one owned process group/cgroup with
   bounded timeout, accounting of Maven/Surefire descendants and checked cleanup;
   no killing unrelated Java processes. Abort on unknown owner/security state.

### Exact proposed execution commands after those gates PASS

Below paths are approval-packet bindings, not claims they already exist. Resolve
and fingerprint the actual approved Maven path and owned directory before run.
Use one attempt; no retry after failure. Run in a dedicated Linux shell whose
environment contains synthetic values only; no developer environment inheritance.

```bash
cd "$T00203_SOURCE/app/backend"
umask 077
mkdir "$T00203_PRIVATE/runtime" "$T00203_PRIVATE/maven-home"
timeout --signal=TERM --kill-after=15s 600s env \
  -u JAVA_TOOL_OPTIONS -u JDK_JAVA_OPTIONS -u _JAVA_OPTIONS \
  -u JAVA_OPTS -u MAVEN_OPTS -u MAVEN_ARGS \
  MAVEN_USER_HOME="$T00203_PRIVATE/maven-home" \
  TASK_C_PRIVATE_DIRECTORY="$T00203_PRIVATE/runtime" \
  DOCKER_HOST=unix:///var/run/docker.sock \
  TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1 \
  TESTCONTAINERS_RYUK_DISABLED=false \
  TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=false \
  TESTCONTAINERS_REUSE_ENABLE=false TESTCONTAINERS_CHECKS_DISABLE=false \
  "$T00203_MAVEN" -B -ntp -o \
  --settings "$T00203_SOURCE/scripts/ci/maven-settings.xml" \
  "-Dmaven.repo.local=$T00203_APPROVED_REPOSITORY" \
  -Ddebug=false -Dtrace=false -Dstyle.color=never \
  -Ptestcontainers-postgres -Dtest=IdentityMigrationTests \
  clean verify > "$T00203_PRIVATE/maven.log" 2>&1
```

Record real exit and timeout status immediately, before other commands. Never
stream raw Maven/XML/errors. Timeout is an additional time bound, not proof of
descendant containment; use the separately approved Linux isolation envelope.
Do not fall back online or to another JVM/DB if offline resolution fails.
New synthetic PostgreSQL databases/roles are generated by existing support,
never the developer PostgreSQL5432/5433 or an existing volume. Test data is fixed
example.invalid identities/addresses and invalid synthetic BCrypt hash fixtures.

Require exactly the14 listed class/method reports, all PASS/0 skips, no other
class, duplicate/missing report, test error, dump or JVM crash artifact. Compare
exact method inventory from approved test source, not counts alone. Parse raw
reports privately and output only fixed method/result projections. Read the
generated `runtime/task-c-secrets.txt` only inside this approved synthetic session
to scan raw logs/XML and selected sanitized deliverables; do not output values.
Require existing `audit_task_c_credentials` and `audit_task_c_cleanup` controls
from unchanged `scripts/ci/verify-reports.py` on this session's exact files. Those
controls and Docker cleanup require explicit runtime permission, not execution
here. Retain private diagnostics on any failure; no public raw artifacts.

Testcontainers owns container/anonymous-volume cleanup at JVM shutdown. Reinspect
exact run/session labels, mounts, consumers and IDs before any leftover cleanup.
Verify owned resources absent and foreign inventory preserved after completion;
no global prune, Compose down, developer volume deletion or silent force cleanup.
If safe cleanup cannot be established, STOP and request exact-resource recovery.
Metadata matching does not prove developer database content integrity.

### Regression and CI integration boundary

The existing strict `verify-reports.py` full-mode collector cannot accept this
new class: its allowlist and expected test inventory are fixed. It must NOT be
silently bypassed or claimed PASS. Proposed isolated14-case runtime evidence is
separate from full CI acceptance. A subsequent owner-approved integration must
extend the strict class/method policy and its missing/extra/skip/privacy tests;
those files are protected and unchanged in this task. Three required job names,
assertion strength and ruleset must remain unchanged. Then run existing backend
regressions + new migration suite on clean PostgreSQL and required CI, with actual
counts collected from reports. No precomputed/guessed GitHub PASS or test totals.

## DoD and handoff

| Mandatory condition | Status |
|---|---|
| Current migration/model implementation | IMPLEMENTED; eight tables/16 explicit indexes |
| Accepted-contract/schema support review | Source mapping complete; runtime enforcement not implied |
| Static source/mutation/immutability checks | PASS15/15 |
| Java compilation, Flyway from empty, V2 upgrade, constraint/index/ownership tests | UNVERIFIED; exact-revision runtime approval pending |
| Actual Demo compatibility and backend regression | UNVERIFIED |
| Supplemental trace/task/evidence | Updated; no protected main trace change |
| Full-feature strict CI/main trace/runtime acceptance | OPEN; requires separately authorized integration |

T-002-03 checkbox remains open. Next canonical task is **T-002-04 Domain**:
Identity domain types/guards for account/grants/lifecycle/session/proof policies,
happy/negative/boundary/transition tests; prerequisite T-002-03 acceptance.
It is NOT started here. Feature002 and AC-002-01..05 remain NOT ACCEPTED;
Feature001/D1-08/Day1 remain PARTIAL. Foundation H2-12B/Wrapper/S3-B/S4 findings
retain their existing status; this task reopens none of those investigations.

**Exactly one immediate priority:** owner approval of bounded isolated PostgreSQL
verification on this source checkpoint. Stop before launching any runtime.

## Final source fingerprints and safety review

| Source | SHA-256 |
|---|---|
| V3__production_identity.sql | c870bc7fa2225e7497534caf717ce6ad8d0f51c8fffbca272f8c5bdaf84985fb |
| IdentityMigrationTests.java | 9d3aad913f3d4f9d828a7cd55d317003a20f1751ea7b4ee23f6d928f206b0aee |
| test_identity_migration.py | 0957926571225263433fb749855bd9d6af949d051f611412de309f5ad4187b67 |
| PostgresFoundationTests.java | a66991a347921cefd61a4b6f5e03d5170d5f7e208366a6244d9a38f7a5bb9c50 |
| PostgresTestcontainersTests.java | ee0c569ec31f5f680569c43fee4c8b30cf1fb59f5849a5add42a8643fe8026a9 |

Final whole-public-inventory and protected-file comparison is checked against the
pre-task manifest, not against HEAD (which contains unfinished user work).
The exact command/results are recorded in the final verification note below.

### Final verification note

Final allowed-scope/integrity script exit0: exact8 changed paths against the
pre-task public inventory,4 new/4 existing;453 public files versus449 before;
28 working paths versus22 before, with all original paths preserved. No removed
public path, unexpected delta or staged change. Branch/HEAD unchanged. All11
Foundation fingerprints match the pre-task working files; the original four
H2-12B fingerprints also match their recorded values. Canonical spec, accepted
contract/ADR/register, all prior migrations, POM/CI/config and other source are
byte-identical to preflight. Main traceability is among the protected files.

UTF-8 strict decoding/zero replacement characters and41 relative file targets
PASS. Selected8-file disclosure scan found no PEM private-key header, GitHub/AWS
credential pattern or JWT-shaped bearer; generated SQL/test fixtures are
synthetic. This bounded source scan is not a whole-disk/history audit or proof
that future application logs redact secrets. No raw telemetry/credential file
was opened or published. git check-ignore confirms the hash-only preflight
record is ignored; it is retained without deleting any existing evidence.
git diff --check exit0 (pre-existing line-ending notices only).

| Protected working path | SHA-256 before = after |
|---|---|
| infra/verification/maven_h2.py | 42198a9be515196b3e3acce57967335ccb8414a98859b633c744345cb0e349eb |
| infra/verification/test_maven_h2.py | 85a5bc2c0189f9b7b11a1fed98b80bd11561ef5f34f9149748a7005766c03dd3 |
| docs/implementation/ac-001-04-s3a-r1-evidence.md | 668733d05374d4216eb5cbce16b6f16d11aaa1069a1c7a7a8f89863daa410658 |
| docs/quality/traceability.md | 0e992a189ff22e0876d3f7616890b15cf23188abf0e6723022696e6b3bc89eb9 |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoAccountSeed.java | 38f698f420521fbc6c8af562039cd09de8d0f9f8400fff56842fb82fa2cd2ba5 |
| app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoSeedPreflight.java | d5a7fe07321fd68502e6942761bcb9be82f13416aa86763b10d3c3f1e4d4198e |
| app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/DemoSeedPreflightTests.java | 276bd82d81bf2fc75be2072456ef41e95fbd20df565fb38ac5b82f1a0c53f995 |
| infra/verification/fail_fast.py | ec349558c7aa197663bc5a6ea88e139069a5d4359f164faa2d065ba24c095524 |
| infra/verification/test_fail_fast.py | c80d775d8f72351744e30cf49f0ce7660bf875f5fe7c71eee278b1965b80d0dc |
| scripts/ci/verify-reports.py | e47150d1949681998de8dc77d93269369d9fbe94bee78b42a96675da58b0974d |
| scripts/ci/test_verify_reports.py | 2ce80192430e055be6e6e5d8c75dc4602f7cce6555a709a2643d5b86075a1ad6 |

## T-002-03-PG conditional verification preflight — 2026-10-21

Authority: OWNER_T_002_03_ISOLATED_POSTGRES_VERIFICATION: CONDITIONAL_GO.
This grants one bounded batch only after every mandatory source/environment/
containment/privacy gate passes. It does not waive the packet's Linux isolation
requirements or authorize modifying protected report policy. This supersedes
the earlier lack-of-runtime-authorization statement only conditionally; no
approved executable environment was established and no JVM attempt was consumed.

### Fresh source and SDD preflight

Root AGENTS and relevant SDD/REQ-002-01..05/AC-002-01..05, DEC-03/04/O1-O7,
database ownership/test strategy, actual execution packet, migration test/support,
Testcontainers configuration/POM and Foundation execution policy were read.
Canonical T-002-03 requires real empty-database and upgrade migration with
indexes/constraints/ownership and schema preservation. This verification task
also explicitly requires compilation, strict inventory, containment/cleanup and
valid evidence before DONE. Neither static checks nor targeted tests waive those
requirements. T-002-04 remains dependent on T-002-03 acceptance.

Fresh branch/HEAD match expected;28 original working paths,453 public files,
empty index. Five migration/test/static fingerprints and11 protected Foundation
fingerprints exactly match the packet's16 recorded entries, including the original
four H2-12B hashes. An ignored hash-only preflight record was saved at
`[REDACTED_PRIVATE_PREFLIGHT_RECORD]`; no private configuration or credentials
were read. Final doc-only delta is checked against this fresh preflight.

An initial fingerprint locator exited1 because a recursive basename search also
matched an ignored build copy. It stopped before writing a snapshot or running
tests. Reconciliation used the five exact source paths already linked in the
packet; all16 hashes then matched, exit0. No alternate source revision was used
and no build-copy contents were accepted as execution source.

### Runtime plan gate (not an execution result)

| Item | Fresh finding | Gate |
|---|---|---|
| Exact source revision |5 approved source fingerprints +11 Foundation fingerprints match; original V1/V2/H2 migration and accepted contract unchanged | PASS |
| Windows Java | Get-Command resolves [REDACTED_PRIVATE_LOCAL_PATH]/bin/java.exe; release metadata says Oracle21.0.10, Windows/x86_64; executable was not launched | Metadata identified; NOT an approved Linux runtime |
| Windows Maven | PATH resolves [REDACTED_PRIVATE_LOCAL_PATH]/bin/mvn.cmd; this is a candidate3.9.12 directory, not proof of executing Maven3.10.0 | Does not establish required runner; no version JVM launched |
| Proposed Linux environment | Read-only WSL enumeration: Ubuntu Stopped (WSL2); docker-desktop Running (WSL2) | BLOCKED: no ready Linux toolchain/isolation envelope established; Ubuntu was not started |
| Linux Java21 / Maven3.10.0 / isolated dependency cache | No approved exact executable path, provenance or prepared source/private-directory bindings established for the packet | UNVERIFIED / mandatory gate not met; do not assert software is absent |
| Process containment / observers | Existing infra/verification/maven_h2.py explicitly supports DIRECT_VERIFIED_MAVEN_H2 and WRAPPER_VERIFICATION only; packet requires separately established Linux process group/cgroup, ownership/accounting and checked cleanup | BLOCKED for this proposed PG execution; no H2 observer reused, relaxed or bypassed |
| Expected process tree | Proposed owned launcher -> Maven JVM -> Surefire fork(s); Docker-managed PostgreSQL/Ryuk separate resources require daemon/session ownership controls | Design only; no actual process/accounting evidence |
| Database isolation / limits / image cache / daemon identity | Existing support has pinned images, random loopback ports, synthetic per-test DB/roles, no reuse; actual daemon/cache/resource-limit and live isolation checks not completed after execution gate blocked | UNVERIFIED; Docker-Desktop-running does not prove these gates |
| Cleanup / privacy | Existing source defines owned lifecycle, synthetic secret register and credential/cleanup guards; private runtime directory/session was not created | Prepared policy only, no runtime cleanup/privacy PASS claimed |

The initial sandboxed `wsl.exe --list --verbose` returned E_ACCESSDENIED/exit1.
The same read-only enumeration with permitted escalation completed exit0 and
provided the states above. No automatic-approval rejection, WSL start/shutdown,
Docker CLI mutation or daemon change occurred. Docker was not found on PATH or
at the two common Windows executable locations checked; this bounded lookup does
NOT prove Docker is uninstalled. No further Docker access was necessary once the
mandatory execution-path gate could not pass.

Historical H2-12/H2-12B security status is preserved. It is not evidence of a new
PostgreSQL security failure and is not generalized into a ban on every Linux JVM.
The Windows observer has no approved PG mode; the proposed alternative Linux
envelope is unestablished. A timeout alone is not containment. No runner/observer
or telemetry harness is implemented to work around this preflight result.

### Strict inventory investigation

Authoritative collector is `scripts/ci/verify-reports.py`, called after Maven by
`scripts/ci/run-backend.sh`. Read-only module import (Python -B, no verify/cleanup
function invocation) confirmed:

- FQCN `com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests`
  is absent from REQUIRED, expected_suites('postgres') and TESTCONTAINERS_ONLY.
- PostgreSQL inventory currently contains19 expected classes. This is a class
  inventory count, NOT executed test cases or a CI result.
- Collector rejects any suite absent from expected before accepting its methods;
  required missing/extra/duplicate/skipped cases fail closed. New class cannot be
  called CI PASS under the current policy, even if its14 methods later pass.
- Source discovery is plausible through existing Surefire naming and Jupiter:
  class ends Tests, has14 @Test methods and task-c tag; the existing
  testcontainers-postgres profile enables that tag and task.c.enabled. Default/H2
  excludes it. Actual discovery and compilation remain UNVERIFIED.
- The collector is a post-execution acceptance gate, not an inherent prohibition
  on test-compile. Compilation/targeted tests still need an approved runtime
  envelope. Current stop is caused by the unresolved execution path; inventory
  independently prevents full acceptance. No compile failure is inferred.
- A separately permitted targeted run could provide supplemental PG evidence,
  but could not replace required full inventory/CI acceptance. None was started.

Minimal proposed policy change, requiring separate explicit authorization because
both files are protected here: add exactly14 reviewed methods under the new FQCN
to REQUIRED; classify the class PostgreSQL/Testcontainers-only; extend
`scripts/ci/test_verify_reports.py` controls for exact inventory, each missing/
skipped/failed case, extra/duplicate/untrusted names and privacy publication.
Preserve existing expected cases/skips, guards, three required job names and
ruleset. No generic allowlist, partial-success report mode or deceptive renaming.
Neither protected policy file, POM nor workflow was changed in this task.

### Actual verification matrix

| Gate / command | Actual result | Exit code |
|---|---|---|
| Exact branch/HEAD/status/index and packet fingerprint check | PASS5/5 source +11/11 Foundation; empty index |0 (initial path-locator issue reconciled as above) |
| Read-only strict inventory evaluation | Class absent in all three governing sets; source-level acceptance blocker confirmed |0 |
| python -B app/backend/test_identity_migration.py |15 tests PASS,0 failures/errors;15 mutation subcontrols +1 positive control included |0 |
| git diff --check | PASS; existing line-ending notices only |0 |
| PG-1 Java/test compilation | UNVERIFIED / NOT RUN | N/A |
| PG-2 empty V1/V2/V3 migration | UNVERIFIED / NOT RUN | N/A |
| PG-3 V2 upgrade / Demo data preservation | UNVERIFIED / NOT RUN | N/A |
| PG-4 actual14 PostgreSQL test methods/negative SQL assertions | UNVERIFIED / NOT RUN | N/A |
| PG-5 actual PostgreSQL/backend regressions | UNVERIFIED / NOT RUN | N/A |
| PG-6 runtime shutdown/resource cleanup/accounting | NOT_APPLICABLE: no runtime resources/processes created; no cleanup executed | N/A |
| Live database isolation, resource limits and runtime privacy guards | UNVERIFIED / NOT RUN | N/A |

Runtime batch attempts0; JVM/Maven launches0; new containers/databases/volumes0;
real migrations executed0; actual PostgreSQL assertions0. No functional migration
or build defect was demonstrated. Existing SQL/test implementation is preserved.
Only the three allowed Feature002 documents are updated, plus ignored hash-only
preflight metadata; no raw report/credential publication.

**Verdict: T_002_03_ENVIRONMENT_BLOCKED.** Implementation status stays
IMPLEMENTED_VERIFICATION_PENDING; T-002-03 is not DONE. AC-002-01..05/Feature002
remain NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

**One next priority:** approve a narrowly scoped T-002-03-PG readiness integration
task that binds a concrete Linux/clean-runner Java21/Maven3.10.0 containment path
and permits the exact two-file strict inventory integration above, preserving all
existing controls. Do not rewrite V3, start T-002-04, publish or rerun runtime as
an implicit consequence of this blocked preflight.

Final PG-preflight integrity check exit0: exactly the three authorized Markdown
documents changed versus the fresh PG manifest; all other450 public files,
including migration/tests/contract/POM/CI and all11 protected Foundation files,
are byte-identical. Public inventory453 files and working inventory28 paths are
unchanged; branch/HEAD unchanged, index empty. UTF-8/bounded document disclosure
and44 local file/anchor links PASS. git diff --check exit0, pre-existing line-ending
notices only. No stage/commit/push/PR/merge, source patch or runtime launch occurred.

## T-002-03-RI readiness integration — 2026-10-21

Authority: OWNER_T_002_03_READINESS_INTEGRATION_APPROVAL: CONDITIONAL_GO.
This explicitly grants a narrow exception for the two authoritative inventory
files; earlier11/11-unchanged statements describe earlier checkpoints. In THIS
checkpoint nine protected files remain byte-identical and two have authorized
inventory/test-only deltas. No governance rule preventing this explicit owner
exception was found; no immutable deployed migration or accepted decision is
changed. No Java/Maven/PostgreSQL execution is authorized or performed here.

### Baseline, source and exact allowed changes

Branch/HEAD remain feature/ac-001-04-s3a-runtime-fail-fast /
e8dd52c6baff25ad5c75b80a85e7c81a9f85587f. Fresh preflight:28 working paths,
453 public files, empty index,5/5 migration/test/static hashes and11/11 initial
Foundation hashes matched the packet. Hash-only snapshot:
`[REDACTED_PRIVATE_PREFLIGHT_RECORD]`, ignored, no credentials/source copy.

Only five public paths are edited: scripts/ci/verify-reports.py,
scripts/ci/test_verify_reports.py, this evidence, Feature002 tasks and supplemental
traceability. No SQL/Java/POM/workflow/accepted contract/ADR/REQ/AC changes.
Previously unfinished Foundation work is preserved, not compared against HEAD
as if HEAD were the working baseline.

| Approved policy path | Before SHA-256 | After SHA-256 |
|---|---|---|
| scripts/ci/verify-reports.py | e47150d1949681998de8dc77d93269369d9fbe94bee78b42a96675da58b0974d | 03308b5aff08a558816aab7c7d71d386442cc57b40eb01fe8789443498acf9a8 |
| scripts/ci/test_verify_reports.py | 2ce80192430e055be6e6e5d8c75dc4602f7cce6555a709a2643d5b86075a1ad6 | e11b9736956c54e5d502cd4067ad1737cad7cbd97807acabe9a6e3b35b230076 |

V3 SHA-256 remains c870bc7fa2225e7497534caf717ce6ad8d0f51c8fffbca272f8c5bdaf84985fb.
IdentityMigrationTests SHA-256 remains
9d3aad913f3d4f9d828a7cd55d317003a20f1751ea7b4ee23f6d928f206b0aee.
The other three source fingerprints in the earlier packet also remain unchanged.

### Strict inventory implemented and verified

Collector constant IDENTITY_MIGRATION names the exact FQCN
com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests.
REQUIRED declares the14 exact methods in the earlier prepared-suite table; the
list is literal/source-reviewed, never inferred from XML or a wildcard. It is
added to TESTCONTAINERS_ONLY, NOT PG_ONLY (which models H2 expected skips).
Consequently only postgres mode requires it, always PASS; architecture/H2/
foundation inventories are unchanged. No existing identity is removed/replaced.

Tests contain an independent literal14-method list AND compare it to the actual
Java package/method source. Eleven new Python test methods cover exact inventory,
positive publication, every missing method14, every renamed method14, each
skip/failure/error14x3, missing whole suite, duplicate case/suite, forged class,
extra method, out-of-mode reports, malformed/incomplete metadata and nonzero
Maven exit. Payload markers are synthetic; names, failure text, system-out/err and
properties cannot leak through selected sanitized outputs. Consistent counters
and exact expected rejection messages distinguish missing-test detection from an
unrelated XML/count rejection. These are synthetic collector tests, not SQL tests.

One existing test changes only its PostgreSQL expected total from256 to270
(+14); unchanged predictions: architecture23/0 skips, H2231/17, foundation5/0.
PostgreSQL now expects20 classes /275 cases:270 PASS and5 existing foundation
skips. These are source/synthetic inventory expectations, NOT observed Java/CI
results. The14 new migration methods must have0 skips and remain mandatory.

Actual command, after checking temp-file/subprocess side effects:

```powershell
python -B -m unittest discover -s scripts/ci -p test_verify_reports.py -v
python -B app/backend/test_identity_migration.py
git diff --check
```

First command completed once: **67 tests PASS,0 failures/errors/skips**,29.235s,
exit0. All56 original report controls retained plus11 new test methods.
Second: **15 tests PASS**, exit0;15 migration source mutation subcontrols and1
comment positive control included, not extra PostgreSQL executions.
diff --check exit0 with pre-existing line-ending notices only. Synthetic XML and
public-output fixtures were confined to each unittest TemporaryDirectory and
cleaned by its registered cleanup. Existing Docker cleanup-control calls are
mocked; no Docker subprocess/container or native JVM was invoked by these tests.

Source-delta proof reverses only the authorized inventory patch IN MEMORY and
matches both reconstructed originals to their preflight SHA-256 (including EOL
variant verification), without writing/restoring any protected source. All four
collector function ASTs are identical. All19 previous REQUIRED class/method lists
are identical; architecture/H2/foundation expected_suites are identical. Existing
test/helper definitions65 retained; only the one aggregate-count expectation
changes as stated. All other existing test ASTs are identical. No logging,
redaction, XML schema, failure semantics, credential or cleanup function changed.

### Exactly one selected execution candidate

**Existing GitHub-hosted ubuntu-24.04, Backend verification / verify matrix
mode=postgres, job name Backend PostgreSQL 18.** No alternative WSL or new runner
container is selected, started or configured. GitHub CLI is not available at the
bounded PATH/common-location lookup. Public REST metadata was read with no token,
workflow dispatch, publication or setting change:

- Workflow383888734 is active at .github/workflows/backend-verification.yml.
- [Historical baseline run39211616700](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39211616700)
  is push on exact e8dd52c6baff25ad5c75b80a85e7c81a9f85587f, attempt1, SUCCESS.
- PostgreSQL job117926070152 ran in GitHub Actions group, runner label
  ubuntu-24.04, with Java setup, disposable PostgreSQL provisioning and Wrapper
  verify/discovery audit SUCCESS. The other two required jobs also SUCCESS.
- This establishes a real existing hosted path at the BASELINE only. It does NOT
  verify current uncommitted V3/policy changes or current runner capacity/cache.

The browser tool could not access the API URL; sandboxed urllib then reported
URLError. A permitted read-only network escalation returned the metadata above
(exit0), followed by workflow/jobs metadata (exit0). No credential or raw test log
was fetched. WSL was not started; no local service/resource mutation was needed.

This candidate follows existing accepted Task-C/CI governance and separates
ephemeral hosted resources from developer services. It uses the EXISTING Linux
Wrapper workflow, not the Windows-only H2/Wrapper observer. The earlier proposed
Linux direct-Maven snippet is superseded as the SELECTED candidate, not silently
executed or generalized into permission to run Windows Wrapper. Runtime still
needs separate owner authorization and fresh pre-execution gates.

| Component | Exact candidate/configured evidence | Readiness limit |
|---|---|---|
| Source | Future approved commit containing V3/test/two policy hashes; current base HEAD above | Current work is uncommitted; no hosted checkout can contain it yet; publication separately authorized |
| Java | actions/setup-java Temurin major21, signature verification, no JDK cache; resolved version logged | Exact resolved minor/binary is future execution evidence, not asserted from Windows Java |
| Maven | Existing wrapper properties specify Apache Maven3.10.0; run-backend.sh verifies actual version and fresh Maven home | Clean runner downloads distribution/dependencies; no local Maven3.9.12 substitution |
| Boot/Testcontainers | Boot4.1.1, test-scope Testcontainers2.0.5 BOM baseline, no manifest change | Compilation/current runtime compatibility still UNVERIFIED |
| PostgreSQL images | Same18.6-bookworm and Ryuk0.14.0 immutable identities in this packet/ADR-0006 | Live runner image identity/architecture/startup checks and pull permission required |
| Isolation | GitHub-hosted dedicated job, contents:read, checkout credentials not persisted; tmpfs CI DB, synthetic roles; separate Testcontainers random loopback mapping/anonymous data volume | Confirm actual fresh runner/daemon/socket and ownership before execution; never local Docker/developer volumes |
| Timeout/resources | Workflow timeout-minutes30, no new concurrency/job names | Hosted VM CPU/RAM/disk and per-container resource limits are not measured/configured here; approve/verify concrete budgets before batch |
| Network/cache | Fresh MAVEN_USER_HOME and explicit CI settings; fixed existing workflow downloads Java/Maven/dependencies/actions/images | No download in this task. Owner must bound future approved HTTPS registry/artifact access; no arbitrary install/provider/telemetry permissions |
| Process accounting | Runner -> shell -> Maven JVM -> Surefire JVM(s); runner Docker daemon separately starts owned baseline PG, Task-C PG and Ryuk | No active runner exists for this source, no live process accounting/timeout termination audit; do not claim Windows Job evidence or new Linux cgroup controls |
| Cleanup/privacy | Existing checked label ownership, temporary secrets, read-only cleanup audits, sanitized-only artifact paths | Multi-suite cleanup evidence gap below prevents VERIFICATION_READY |

### RI-LIFECYCLE-01: concrete multi-suite cleanup evidence limitation

Source inspection shows BOTH PostgresTestcontainersTests and the new
IdentityMigrationTests own class-scoped containers and call
PostgresTestcontainersSupport.verifyRuntime in @AfterAll. That helper writes
Files.write(PRIVATE.resolve("task-c-runtime.txt"), proof), replacing prior proof.
The unchanged audit_task_c_cleanup requires exactly1 anonymousVolume and2
distinct container IDs (one PG plus Ryuk), one session and one taskRun.

If both classes share a JVM, the later writer replaces the earlier suite's
container/anonymous-volume evidence; querying remaining labeled containers does
not establish absence of an earlier anonymous volume. Separate forks with the
same directory are not a proven workaround: privateDirectory rejects an existing
task-c-runtime.txt, and the current workflow does not configure independent
per-suite private directories/manifests. Actual scheduling/fork behavior and
cleanup outcomes have NOT been run. This is a source-level multi-suite evidence
coverage gap, not an observed volume leak, security incident, SQL defect or
permission to weaken cleanup. Inventory acceptance alone cannot close it.

Current narrow approval forbids changing unrelated security/audit mechanisms;
therefore helper/cleanup guard remain unchanged. Minimum proposed follow-up:
owner-scoped multi-suite lifecycle-evidence correction in exactly
PostgresTestcontainersSupport.java, audit_task_c_cleanup in verify-reports.py and
focused test_verify_reports.py controls. Preserve exact task/session/resource
ownership, fail-closed missing/foreign/duplicate/substitution checks and verify
all owned suite containers/volumes; do not merely accept arbitrary additional
IDs/volumes or overwrite historical proof. No generic process harness, business
code, migration, dependency or workflow redesign is needed. This is PROPOSED,
not performed/approved by this inventory-only exception.

### Executable packet for the selected existing job — NOT RUN

Input identity: base HEAD above; migration/test hashes unchanged; collector
03308b5aff08a558816aab7c7d71d386442cc57b40eb01fe8789443498acf9a8 and policy tests
e11b9736956c54e5d502cd4067ad1737cad7cbd97807acabe9a6e3b35b230076.
These two hashes identify THIS readiness checkpoint. A future authorized cleanup
correction changes source fingerprints and requires a refreshed bounded packet;
do not execute a different revision under these hashes. Snapshot all auxiliary
helper/workflow/script bytes as well before any future approved publication.

Required gates BEFORE launching the candidate:

1. Resolve RI-LIFECYCLE-01 with reviewed controls, then freeze exact source/test/
   collector/helper SHA set. Preserve the nine protected Foundation paths and
   original V1/V2/H2/contract identities. No functional mutation is assumed safe.
2. Separately authorize a Git checkpoint/publication and ONE hosted batch on the
   resulting exact commit; no publication or workflow dispatch in this task.
3. Verify GitHub workflow/event/ref/checkout SHA, ubuntu-24.04 hosted identity,
   job30min limit and approved CPU/RAM/disk/process/network budgets. No custom
   self-hosted/Windows substitution. Check effective environment/options without
   printing values; no developer settings, secret context or inherited agents.
4. Confirm fresh RUNNER_TEMP namespaces, no symlink/foreign state, daemon/socket
   identity and initial resource inventory. Explicitly approve Ryuk's existing
   nonprivileged Docker socket access and only approved digest pulls if missing.
5. Record expected process/owned Docker identities and bounded termination plan.
   Require credentials and all raw logs/XML to stay private; define cleanup and
   evidence retention before executing any Java/version probe or provisioning.

The exact existing workflow commands, presented for future authorization only:

```bash
python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
python3 -B scripts/ci/prepare-postgres.py start --directory "$RUNNER_TEMP/d1-05-postgres"
VERIFICATION_MODE=postgres bash scripts/ci/run-backend.sh postgres
# Existing separate always() cleanup step, including failure/cancellation handling:
python3 -B scripts/ci/prepare-postgres.py cleanup --directory "$RUNNER_TEMP/d1-05-postgres"
```

Do not paste this into a local developer shell. prepare-postgres's host5433 bind
is allowed ONLY on that fresh hosted runner, not developer5433. State/secret files
are below $RUNNER_TEMP/d1-05-postgres; baseline PG uses new tmpfs storage, no named
developer volume. Task-C supplies separate synthetic per-test databases/roles,
random127.0.0.1 ports and fresh anonymous volume. Internal Wrapper command is
exactly the existing run-backend.sh online clean verify with explicit settings,
fresh repository, -Ddemo.test.database=postgres and -Ptestcontainers-postgres;
no targeted -Dtest shortcut and no weakened strict report mode.

Expected transient runner mapping:

- Source: $GITHUB_WORKSPACE, exact future approved commit; no ignored .demo/private stores copied.
- Maven: $RUNNER_TEMP/d1-05-maven/repository, fresh per job.
- Private: $RUNNER_TEMP/d1-05-private/{java-version.txt,maven-version.txt,maven.log,task-c-secrets.txt,task-c-runtime.txt}; raw target/surefire-reports also PRIVATE.
- Public artifacts: ONLY $RUNNER_TEMP/d1-05-reports/summary.json and tests.xml, existing7-day retention, no hidden files/raw logs/credentials.
- Existing always cleanup validates recorded CI label/name before removing its owned temporary PG. Testcontainers normal teardown/Ryuk handle Task-C resources; future corrected complete cleanup proof must pass. Never global prune/down/delete developer data.

| Check | PASS requirement / evidence | FAIL/STOP |
|---|---|---|
| Compilation | Real Maven/test compilation succeeds, private log and real exit | Compile/dependency failure; no retry or version substitution |
| Empty migration/history | Identity cold-start case observes V1/V2/V3 and validate/rerun0/checksums | Missing/failed/skipped method, history drift, unexpected schema |
| Upgrade | Real V2 Demo fixtures retained after V3; no account import | Lost/changed Demo data, duplicate/unexpected effects |
|14 Identity methods | Every exact method PASS,0 skips; actual XML class/name inventory matches literal policy | Any missing/renamed/extra/duplicate/skipped/error/failure |
| Existing PG regression |8 PostgresFoundation +7 PostgresTestcontainers +6 runtime-policy methods, plus remaining required backend/architecture/S2/seed/health tests | No test removed/ignored; actual strict collector failures block acceptance |
| Full report inventory | Actual postgres summary PASS, expected270 PASS/5 allowed skips across20 classes, not predictions copied as results | Nonzero Maven, mismatch, absent report/guard, unexpected skip |
| Privacy | Existing synthetic credential audits on real private logs/XML and selected public outputs; no leaked payloads | Any leak/unknown evidence; keep diagnostics private, stop |
| Lifecycle/accounting | RI-LIFECYCLE-01 resolved; all owned processes/containers/volumes accounted and absent after teardown; foreign baseline preserved | Incomplete/overwritten/foreign proof, live descendant or unsafe cleanup; no automatic recovery/retry |
| Remote identity | Actual run URL/id/attempt/event/ref/SHA and all required job states recorded | Wrong/stale SHA or old PR/baseline evidence substituted |

The existing workflow does not currently persist all proposed preflight/resource
projections or multi-suite lifecycle proof. These are concrete pending conditions,
not gates silently assumed PASS. Current readiness approval does not authorize
workflow changes or runtime; any necessary expanded scope must be reviewed first.

### Current result and SDD disposition

| Gate | Actual result | Exit |
|---|---|---|
| Exact finite inventory and source matching | PASS14 methods; new class PG-only; old19 class lists unchanged |0 |
| Existing + new strict report controls |67/67 PASS, no failures/errors/skips; synthetic fixtures only |0 |
| Migration source controls |15/15 PASS; SQL/Java unchanged |0 |
| Collector security/cleanup functions and non-PG inventories | Four function ASTs identical; old inventories preserved |0 |
| Historical hosted candidate discovery | Active workflow; baseline run/job metadata verified via read-only API |0; not current revision runtime |
| Multi-suite runtime cleanup completeness | BLOCKED by RI-LIFECYCLE-01; no unauthorized correction | N/A |
| Current Java compilation / actual migration / runtime privacy/accounting / remote CI | UNVERIFIED / NOT RUN | N/A |
| Publication/execution | No commit/push/PR/dispatch; no JVM/Maven/Docker/DB startup/download | N/A |

**T_002_03_CI_READY_RUNTIME_PENDING.** Strict inventory source integration is
complete, but VERIFICATION_READY would be inaccurate while cleanup completeness
and live runner gates remain unresolved. T-002-03 IMPLEMENTED_VERIFICATION_PENDING,
T-002-04 NOT STARTED, Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. No current PostgreSQL PASS or SQL/build failure claim.

**One next action:** approve the bounded RI-LIFECYCLE-01 multi-suite cleanup-
evidence correction described above, retaining the selected existing Ubuntu job
and all fail-closed ownership/privacy checks. Stop before runtime/publication.

Final RI integrity check exit0: exact five-path delta versus fresh RI preflight,
two authorized policy files plus three SDD documents;448 other public files
byte-identical. Nine remaining protected paths, including four H2-12B files/main
traceability, unchanged. Original policy hashes matched before the exception;
authorized final hashes match the table above.453 public files/28 working paths
unchanged; original modifications retained, index empty, branch/HEAD unchanged.
UTF-8/bounded five-file disclosure and46 local file/anchor targets PASS;
git diff --check exit0 with prior line-ending notices only. No runtime resource
cleanup was needed or claimed PASS. No commit/push/PR/merge/workflow trigger.

## RI-LIFECYCLE-01 source correction — 2026-10-21

Authority: OWNER_RI_LIFECYCLE_01_CORRECTION: GO. This explicitly permits the verified
Testcontainers helper, cleanup auditor and directly related process-free tests.
No Java/Maven/DB/container execution, workflow change or publication is authorized.
Read actual helper/suites/audit/CI call chain, relevant SDD/ownership/ADR-0006,
test strategy and private/owned-resource runbook boundaries; accepted Identity
contract/DEC-03/04/O1-O7 and H2 investigations are not reopened.

### Root cause and source-defined inventory

Before correction, both known suites wrote Files.write to the same
task-c-runtime.txt; later proof replaced earlier proof. The guard accepted one
anonymous volume and two container IDs, not the full two-suite lifecycle. This
was a source-level evidence gap, never an observed leak.

| Suite | Actual lifecycle | Corrected evidence | Ownership / absence |
|---|---|---|---|
| com.elogistic.backend.demo.PostgresTestcontainersTests | One class-scoped non-reused PG, including restart with same ID/volume | task-c-runtime.d/demo-postgres.{reserved,observed,created,verified,stopped} | Inspect task/session labels, pinned image, mount and consumers; SDK stop then inspect absence |
| com.elogistic.backend.identity.internal.infrastructure.IdentityMigrationTests | One separate class-scoped non-reused PG; per-test DB/roles | task-c-runtime.d/identity-migration.{reserved,observed,created,verified,stopped} | Same validation; distinct PG and anonymous volume required |
| Shared Testcontainers reaper | One Ryuk per JVM/session, existing nonprivileged socket policy | Explicit same Ryuk tuple in both known suite records | Same image/ID/run label/session-derived name; aggregate absence after JVM shutdown |

Combined expected Task-C resources are derived from these two suites: two distinct
PG IDs, two distinct anonymous-volume IDs, one explicitly shared Ryuk ID. Duplicate
PG/volume identities or a different second Ryuk fail; this is not a generic
counter increase/deduplication rule. Existing CI's separate tmpfs PostgreSQL is
outside these suites and retains its existing label-checked always-cleanup step.
No new network or developer resource is created by this correction.

Both suites already use ExecutionMode.SAME_THREAD; no parallel execution is added.
The helper refuses an unexpected concurrent extra owned container. The protocol
is for the existing shared-JVM execution; an unexpected fork sharing private
state fails exclusive initialization rather than overwriting it. Actual runner
scheduling remains runtime evidence, not inferred from source.

### Implementation and failure boundaries

Only six public paths change: PostgresTestcontainersSupport.java,
scripts/ci/verify-reports.py, scripts/ci/test_verify_reports.py, this evidence,
Feature002 tasks and supplemental traceability. Both suite sources/assertions,
V1/V2/V3, manifests, POM, workflow, runtime policy SPI, contract/ADR/REQ/AC and
nine other Foundation paths are unchanged.

- task-c-runtime.txt remains the existing consumer entrypoint, now a bounded
  version2 manifest declaring EXACTLY both suite identities. Records are fixed
  sibling paths under task-c-runtime.d; no directory walk to arbitrary evidence.
- Execution binds to GITHUB_RUN_ID/GITHUB_RUN_ATTEMPT/GITHUB_JOB when GITHUB_ACTIONS
  is true. A local run requires a separately approved fresh TASK_C_EXECUTION_ID
  UUID; no implicit old/local identity or hosted-to-local override. This task
  sets no real environment variables or credentials. Tests patch child-Python
  environment only and restore it with unittest cleanup.
- Without changing suite code, container() binds its caller through StackWalker
  against the two exact known FQCNs, then reserves a CREATE_NEW record. Unknown or
  ambiguous caller fails before startup. Startup attempts are bounded to1.
- containerIsCreated retains the exact observed PG ID BEFORE provenance checks,
  so an early failure retains useful ownership-UNVERIFIED diagnostics. Only
  inspected task/session/image/mount/consumer identities become created facts.
  observed alone never authorizes teardown or cleanup PASS.
- containerIsStarted performs existing image/privilege/loopback/runtime checks and
  writes verified facts. Repeated existing verifyRuntime calls/restarts may only
  reuse byte-identical proof for the same suite/resources; they do not overwrite.
- stop rechecks exact IDs, labels, mount/volume identity and ALL consumers,
  including stopped containers, before delegating to SDK super.stop(). It then
  checks PG container/volume actual absence before publishing stopped. An
  idempotent repeat verifies absence again; it never targets a new identity.
  Missing/foreign identity refuses SDK deletion and records fixed failure metadata;
  no force/prune/fallback resource deletion is added. Existing Ryuk remains enabled
  and retains its previously approved Testcontainers session-cleanup behavior.
- Metadata publication uses exclusive CREATE_NEW and complete file write/force,
  not REPLACE_EXISTING/overwrite. Duplicate reservations/changed proof fail.
  Interrupted/truncated files remain blocking, not silently repaired. This is
  not a claim of crash-atomic multi-file commit; the post-process parser rejects
  any incomplete required record. Private parent and new lifecycle directory
  remain protected; records contain only fixed protocol/identity/image metadata.
- Fixed failure records preserve startup/provenance/stop uncertainty. A missing,
  failed or never-started suite cannot pass. Business test failure is still
  controlled independently by Maven/strict reports; cleanup may only succeed if
  complete provenance AND actual absence are independently proved.

The cleanup auditor is READ-ONLY. It validates exact manifest/stage fields,
same current execution/run/session, image identities, owner labels, volume/PG
binding, observed/created/verified/stopped agreement and explicit shared Ryuk.
It rejects unexpected keys/types/files, missing stages, duplicate suite/resource
claims, symlink/nonregular/hardlinked files, oversized/truncated data, stale
job/attempt and unapproved formats. No legacy single-suite fallback is permitted
in the combined mandatory job; old private evidence is not migrated/deleted.

Only after complete evidence passes does it issue the existing read-only queries
for all container IDs, all volume names and remaining run-labeled containers.
Every recorded ID/volume must be absent and no unexpected owned container may
remain. A stopped flag, mocked success string or filesystem marker is never a
substitute for Docker absence. Foreign resources are observed but never selected
for deletion; invalid provenance does not even invoke Docker from the auditor.

### Actual process-free verification

Commands executed after reviewing filesystem/mock side effects:

```powershell
python -B -m unittest discover -s scripts/ci -p test_verify_reports.py -v
python -B app/backend/test_identity_migration.py
git diff --check
```

First policy invocation:87 PASS/0 failures/errors/skips,26.246s, exit0. One source
wiring control was then added; final invocation **88 PASS/0 failures/errors/skips**,
24.935s, exit0. No failing run or runtime retry. All67 previous test-method ASTs
are byte-for-byte equivalent at AST level, including all14-method inventory
negative controls. Only their synthetic runtime_evidence fixture helper changes
from legacy text to complete v2 metadata.21 new lifecycle test methods cover:

| Control group | Actual evidence limit |
|---|---|
| Exact two suite records, explicit shared Ryuk, independent/union identities and different record creation order | Python protocol/parser controls, not executed Java publication |
| Every required stage missing, failed/never-started suite, observation retained, duplicate suite/record/resource | Exact rejection reasons before Docker; synthetic owned temporary files only |
| Truncation, duplicate/unknown/missing fields, aliases, stale self-consistent execution and hosted attempt mismatch | Actual parser rejects; no sensitive text emitted |
| Label/session mismatch, resource substitution, wrong type/image/volume binding | Actual strict auditor rejects; no deletion API used |
| Each of three Task-C container IDs and both volumes still present, unexpected owned resource, Docker error/timeout | Simulated Docker inventories; actual absence NOT claimed |
| Unknown/foreign resources, false cleanup flag, private payload | Read-only command allowlist and no raw output; foreign resources never deleted |
| Java writer boundary/source protocol, existing workflow call/manifest entrypoint | Source-contract controls only; Java compiler/filesystem/native execution UNVERIFIED |

Migration source checks: **15 PASS**,0 failures, exit0 on this checkpoint;
includes the15 mutation subcontrols/one positive control, not PostgreSQL tests.
Synthetic directories and hardlinks were scoped to unittest TemporaryDirectory
and removed by registered test cleanup; no existing evidence or developer path
was targeted. All Docker calls in tests were mocked. No real Docker audit,
credential file read, JVM, Maven, PostgreSQL or container was launched.

Bounded cached Testcontainers2.0.5 class-file metadata was read without javap/JVM
or downloads. start/stop and lifecycle hook signatures exist; the cached two-arg
containerIsStarted hook delegates to the overridable one-arg hook. This validates
the selected hook API metadata, NOT Java compilation or actual startup behavior.

The authorized patch was reversed IN MEMORY only and both reconstructed policy
sources matched their pre-task SHA-256. All67 original test-method ASTs remain
identical; collector verify(), expected_suites(), credential audit, REQUIRED,
14 exact methods/classification and non-lifecycle policy remain unchanged. No
XML/report schema or privacy publication rule was relaxed.

### Current source identity / refreshed CI packet

| Path | Before SHA-256 | After SHA-256 |
|---|---|---|
| app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java | 03b96e4551fa4ddbff2d3fdf14226a6f16220d5b6be8531bfaa6ed6e9c509df4 | 6989f0e14cbc73e91eab1d874c082d9ef60e76ba42a7722897567dbc96d557d9 |
| scripts/ci/verify-reports.py | 03308b5aff08a558816aab7c7d71d386442cc57b40eb01fe8789443498acf9a8 | 02d2abb0760c9cfc87a0cd71aab06c24d482da7ceb7f3c7f75e6a7b479ba1321 |
| scripts/ci/test_verify_reports.py | e11b9736956c54e5d502cd4067ad1737cad7cbd97807acabe9a6e3b35b230076 | d14ece5e2164a2fd4c60d6234a9345900ae29f98c542c0fb05df6c920e48d797 |

V3 remains c870bc7fa2225e7497534caf717ce6ad8d0f51c8fffbca272f8c5bdaf84985fb;
IdentityMigrationTests remains
9d3aad913f3d4f9d828a7cd55d317003a20f1751ea7b4ee23f6d928f206b0aee.
Both suites/business assertions, static migration checks and prior migrations
are unchanged. The nine protected Foundation hashes remain those already
recorded above, excluding the two explicitly authorized CI file deltas.

Selected path remains Backend PostgreSQL18 on existing hosted ubuntu-24.04.
**No workflow wiring edit is required:** run-backend.sh already calls
audit_task_c_cleanup(private / 'task-c-runtime.txt'); that entrypoint now follows
the exact two-suite v2 records. Existing TASK_C_PRIVATE_DIRECTORY points to the
same private directory; GitHub supplies execution/job/attempt identity. Only
summary.json/tests.xml are public artifacts; manifest, records, secrets and raw
logs/XML remain private. Baseline CI PG cleanup remains separate and unchanged.

Existing future commands and all pre-execution gates in the RI packet still
apply, using the UPDATED hashes above, not the earlier collector/helper revision:

```bash
python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
python3 -B scripts/ci/prepare-postgres.py start --directory "$RUNNER_TEMP/d1-05-postgres"
VERIFICATION_MODE=postgres bash scripts/ci/run-backend.sh postgres
# Existing always() cleanup step, including failure handling:
python3 -B scripts/ci/prepare-postgres.py cleanup --directory "$RUNNER_TEMP/d1-05-postgres"
```

These are NOT commands to run locally or under this task's authorization.
Future batch must compile helper/tests, execute all14 Identity methods and
existing PostgreSQL/Testcontainers regressions, then require real report/privacy/
complete cleanup evidence. Predicted full PostgreSQL inventory remains270 PASS/
5 expected skips, not newly executed results. Actual JVM teardown/absence,
SDK consumer/reaper behavior, process/daemon isolation and runner budgets must
still pass fresh runtime gates; synthetic controls certify none of these.

### Source-provenance/publication handoff

Current HEAD is still e8dd52c6baff25ad5c75b80a85e7c81a9f85587f; changes are
uncommitted. Historical run39211616700 does not test them. The smallest trusted
handoff is a separately authorized final safety/dependency-closure review and
scoped commit/PR of the exact reviewed source to the existing hosted workflow.
No archive injection, temporary branch, hidden source replacement or dispatch
against old HEAD is proposed.

The closure cannot blindly stage only these six files: V3, IdentityMigrationTests,
the two updated PostgreSQL suites and prior DemoSeedPreflight/seed/test changes
are also uncommitted dependencies of the compiled source/strict report inventory.
A publication task must explicitly review that source closure plus necessary
Feature002 SDD/accepted contract records, preserve the three final hashes above
and V3/test hashes, and record the resulting full commit SHA. Do not stage all
working changes or publish unrelated H2 diagnostic scripts/protected traceability
under an inferred permission. If any prerequisite lacks publication approval,
stop at that exact closure boundary; do not omit required tests/code to get CI
green. No such commit/PR or workflow trigger occurs here.

### Status

| Gate | Actual result | Exit |
|---|---|---|
| Per-suite source correction and combined strict auditor | Implemented; complete fixed inventory, no overwrite/fallback | Source review |
| Current policy/lifecycle/14-method controls |88 PASS; all67 original controls retained |0 |
| Current migration source checks |15 PASS; V1/V2/V3/contract unchanged |0 |
| Existing workflow entrypoint / record path contract | PASS, no YAML/launcher change needed |0 |
| Java compilation/publication/SDK lifecycle and real PG cleanup | UNVERIFIED / NOT RUN | N/A |
| Actual PostgreSQL/remote CI acceptance | UNVERIFIED / NOT RUN | N/A |

**RI_LIFECYCLE_01_CORRECTED_CI_READY** is source/validation/wiring readiness only.
T-002-03 remains IMPLEMENTED_VERIFICATION_PENDING; T-002-04 NOT STARTED;
Feature002 ACs NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL. No runtime cleanup
PASS, resource leak, database execution or broader security certification claimed.

**One next action:** owner-authorize the scoped final safety/dependency-closure
publication and bounded GitHub CI checkpoint described above on this exact
revision. Stop before commit, push, PR or runtime execution.

Final-revision note: observation publication is also ordered before comparing
the callback ID to getContainerId(), so a structurally valid but mismatched
creation ID remains recorded as unverified; it still cannot authorize teardown.
Its source-order control was added and the full final policy suite rerun:
**88 PASS/0 failures/errors/skips,25.630s, exit0**. Final source hashes are the
three values in this correction's table; prior intermediate timings above are
not substituted for this final run. Java delimiter balance/source hook metadata
checks passed, without claiming compilation or Java filesystem execution.

Final integrity/UTF-8/disclosure/link check exit0: exact six-path delta against
the fresh lifecycle preflight,447 other public files byte-identical;453 public
files unchanged. Working paths28 ->29 only because the existing tracked helper
is newly modified; every pre-existing working change remains. Nine other
Foundation protected paths and all four H2-12B hashes unchanged; previous
14-method inventory deltas retained. Both suites, SQL/contract/POM/workflow/
launcher unchanged.48 local file/anchor targets PASS; git diff --check exit0
(prior line-ending notices only). Branch/HEAD unchanged, index empty; no
commit/push/PR/merge, service startup or workflow dispatch. No runtime cleanup
result is claimed.
