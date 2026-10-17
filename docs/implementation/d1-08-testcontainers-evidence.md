# D1-08 Task C — Testcontainers PostgreSQL/Flyway evidence

Execution2026-10-17, Asia/Saigon. **LOCAL_DONE / READY_FOR_REVIEW** for the approved
Task C scope; remote Task C CI is UNVERIFIED. Feature001/Day1 remain PARTIAL.
[ADR-0006](../architecture/decisions/ADR-0006-testcontainers-verification.md),
[runbook](../runbooks/local-development.md#d1-08-task-c--testcontainers-postgresqlflyway).

## Baseline and scope

Clean develop/local origin/live remote all fe5b440af8f541289a7defc5c2ae39036bfd9b11.
Actual develop [run39002192227](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39002192227)
push/SUCCESS and three jobs rechecked. Created previously absent local/remote
feature/d1-08-testcontainers-foundation; no reset/stash/rebase/commit/push/PR.
Task A/B integrated DONE and D1-07 DONE per user-approved context; older evidence
headers remain dated snapshots, not grounds to reopen those scoped milestones.

Read AGENTS/DoR, Feature001 spec/design/tasks, Day1 plan, Task B evidence, approved
Task C preflight/direction. Production source, old PostgreSQL harness, V1/V2,
Compose, 11 APIs, OpenAPI/SDK, architecture rules and workflow/ruleset unchanged.

## Gate0 / actual runtime compatibility

Gate0 PASS for the approved Windows Docker Desktop runtime. Published2.0.5 source
was re-read from Maven Central, reconciled with generic documentation, then a
separate ignored canary ran before the backend POM was changed. No disabled
Ryuk/checks, Alpine execution, TCP daemon or privileged fallback was used.

- Java21.0.10; Boot4.1.1; Wrapper Maven3.10.0.
- Resolved classpath: core, junit-jupiter, postgresql, jdbc and database-commons
  Testcontainers artifacts all2.0.5; no dependency version overrides.
- Boot metadata: Jupiter6.0.3, Surefire3.5.6, Flyway12.4.0.
- Docker Desktop4.84.0/Engine29.6.2/API1.55, Linux/amd64, Windows named-pipe
  desktop-linux endpoint. Process-scoped DOCKER_HOST came from context inspect;
  TESTCONTAINERS_HOST_OVERRIDE=127.0.0.1. No global Docker settings changed.
- Exact pins are in ADR-0006. Registry index/amd64 manifest/config bytes were
  verified during preflight; actual Docker RepoDigests/OS/architecture inspected.
  No image vulnerability/signature certification is inferred.
- Actual resources: exactly pinned PostgreSQL and Ryuk; privileged=false for both;
  published ports127.0.0.1/random. Only Ryuk mounts `/var/run/docker.sock`.
- PostgreSQL has one newly created anonymous volume. Ryuk has no persistent
  volume. PostgreSQL session/task labels and Ryuk session-derived name/task label
  verified. Ryuk intentionally is not registered to reap itself.
- No Alpine or other helper attempt in successful canary/full suite. The allowlist
  rejects unapproved attempts before pull and retains a rejection ledger.
- SPIs exist only in test resources/classes. Built application JAR contains zero
  Testcontainers libraries, policy classes or corresponding test resources.

Canary history: first failed PostgreSQL constructor compatibility for tag@digest;
second failed an incorrect assumption that Ryuk has the database session label;
third PASS1/1 with compatible-image hint and correct Ryuk identity checks. No
image/privilege/ownership policy was relaxed.

## Tests, commands and executed evidence

All Wrapper commands used `-B -ntp --settings scripts/ci/maven-settings.xml` and
an explicit isolated Maven repository. Working directory for backend commands:
`app/backend`. The final runs used a newly created
`.demo/d1-08-task-c/clean-maven-repository`; no existing cache was removed.
This proves fresh artifact resolution, not independent-machine onboarding.

| Actual command/gate | Result |
|---|---|
| Wrapper -Ptestcontainers-postgres -Dtest=PostgresTestcontainersTests,TestcontainersRuntimePolicyTests clean verify | PASS13/13,0 skips;63.15s including fresh dependency resolution |
| Wrapper clean verify | PASS48,17 expected skips;65 discovered;32.45s; no Task C reports/skips |
| Wrapper -Ptestcontainers-postgres -Ddemo.test.database=postgres clean verify | Final PASS73,5 expected skips;78 discovered;60.06s; exact report guard PASS |
| Wrapper -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests clean verify | PASS23/23,0 skips;12.67s |
| python -B -m unittest discover -s scripts/ci -p 'test_*.py' | PASS25/25 including missing/skipped TC, privacy and cleanup negative controls |
| Git Bash bash -n scripts/ci/run-backend.sh | PASS; ShellCheck binary unavailable locally, not claimed |
| packages/api-client: npm run verify:c01 | PASS lint0 errors/0 warnings/0 ignored;11-operation inventory;41 tests |
| packages/api-client: npm run test:wire | PASS27/27;11/11 operations;34 positive+74 negative responses;0 mismatches; privacy/owned cleanup PASS |
| git diff --check | PASS |

The full PostgreSQL run includes all7 new database tests,6 policy tests,4 Task A
PostgresHealthSafetyTests,12 architecture fixture tests (11 forbidden cases and
one positive control), plus all other existing regressions. Five foundation
Redis/RabbitMQ tests remain named skips here and retain their separate local
Task B evidence; they are not counted as remotely executed.

| Test ID / method | Scope / trace | Result |
|---|---|---|
| TEST-001-TC-01 actualPostgres18UsesPinnedIsolatedContainer | REQ-001-02/05; actual PG18, approved identity, real bindings, fsync/synchronous_commit on | PASS |
| TEST-001-TC-02 coldStartAppliesV1AndV2 | REQ-001-02/T-001-09; empty DB -> V1/V2 | PASS |
| TEST-001-TC-03 validateAndRerunPreserveDataAndChecksums | AC-001-02 subset; zero rerun migrations, marker/checksums unchanged | PASS |
| TEST-001-TC-04 upgradeV1ToV2PreservesExistingData | REQ-001-02/AC-001-02 subset | PASS |
| TEST-001-TC-05 schemasAndObjectsBelongToOwner | REQ-001-02;10 schemas including platform,6 tables, separate migration principal | PASS |
| TEST-001-TC-06 runtimeCannotDdlAssumeOwnerOrReadHistory | REQ-001-02/05; eight denied statements/SQLSTATE42501, restricted roles | PASS |
| TEST-001-TC-07 restartPreservesCommittedDataAndHistory | AC-001-02 subset; same container/volume, new JDBC connections, preserved marker/history | PASS |
| TEST-001-TC-POLICY | REQ-001-05/AC-001-04 subset;6 Java policy tests and25 Python report/privacy/cleanup tests | PASS |

Development failures were retained in ignored diagnostics: duplicated new POM
insertions (fixed), Docker Java Mount.getType API mismatch (replaced by actual
volume inspection), then3 restart-related errors from a stale random port cache.
Fixture now reads live bindings from the owned container before JDBC connections.
Focused and full PostgreSQL reruns PASS, including a final rerun after fresh-file
credential protection. No assertions, business semantics or migration were weakened.

## Isolation, privacy and cleanup

Six independent databases per suite, synthetic owner/migrator/runtime roles;
bootstrap superuser only provisions. Migrator SET ROLE selects NOLOGIN owner;
runtime cannot DDL, assume owner or read schema history. Resources close before
restart. No Spring context, H2 fallback or fixed5433 access in the new suite.
Existing harness still owns its separate tmpfs PG5433 container for old tests.

Credential evidence uses CREATE_NEW in a fresh private directory; local Windows
DACL grants current owner/SYSTEM/Administrators, CI private directory0700/file0600.
Synthetic secrets stay out of commands/public artifacts. Final private Maven log,
raw Surefire diagnostics and sanitized reports scanned: PASS. Cleanup guard also
ran against actual final Docker state: PASS; only owned recorded IDs/volume are
checked, no deletion/prune performed by this guard.

Final publishable-file review: exactly18 files/7 Markdown, strict UTF-8 and
relative file links PASS;16 generated synthetic credential values checked against
all18 files with zero matches. This is a bounded scan, not a full-history audit.

Normal JUnit container/anonymous-volume cleanup and Ryuk exit were observed.
Focused/full PostgreSQL snapshots preserve all baseline containers, volumes and
network IDs with no new remaining resources. Canary's initial default `bridge`
network changed ID while the named developer Compose network remained unchanged;
retained events show connect/disconnect, not a proven root cause. No network
delete commands were issued; no claim that every initial network ID was preserved.
Subsequent focused/full runs have exact unchanged network inventories. This
environment observation remains OPEN; no developer-data loss was observed.

Ignored diagnostics: `.demo/d1-08-testcontainers-canary/` and protected
`.demo/d1-08-task-c/private/`. Published documentation contains no raw test output,
credential values or generated artifacts.

## CI and limits

Existing Backend PostgreSQL18 script enables the controlled Maven profile,
effective non-privileged/no-reuse/checks-on settings, private credential audit and
read-only owned-cleanup audit. Exact report inventory makes Docker failure,
missing profile/tests, skipped Task C cases and Maven failure red. Default/H2
does not discover Task C tests; no additional allowed skips. Three required job
names and workflow/ruleset unchanged; current job timeout remains30min.

New Task C hosted Linux/PR/develop execution is UNVERIFIED because no publish was
authorized. No deliberate JVM-kill test, clean-machine onboarding, independent
developer reproduction, security certification or full infrastructure CI claim.
Existing audits remain OPEN: SDK dev4HIGH, mobile58full/35omit-dev; web0 snapshot.
New Maven/helper-image vulnerability scans are UNVERIFIED. Owner: repository owner.
Remaining Feature001 gates include onboarding, literal red-then-fixed CI,
broader consumer/full-infrastructure CI and security/config acceptance.

## Exact publishable inventory —18 files

1. app/backend/pom.xml
2. app/backend/src/test/java/com/elogistic/backend/demo/PostgresTestcontainersTests.java
3. app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java
4. app/backend/src/test/java/com/elogistic/backend/testsupport/TestcontainersRuntimePolicy.java
5. app/backend/src/test/java/com/elogistic/backend/testsupport/TestcontainersRuntimePolicyTests.java
6. app/backend/src/test/resources/testcontainers.properties
7. app/backend/src/test/resources/META-INF/services/org.testcontainers.utility.ImageNameSubstitutor
8. app/backend/src/test/resources/META-INF/services/org.testcontainers.core.CreateContainerCmdModifier
9. scripts/ci/run-backend.sh
10. scripts/ci/verify-reports.py
11. scripts/ci/test_verify_reports.py
12. docs/architecture/decisions/ADR-0006-testcontainers-verification.md
13. docs/implementation/d1-08-testcontainers-evidence.md
14. docs/implementation/day-1-plan.md
15. docs/quality/traceability.md
16. docs/runbooks/local-development.md
17. specs/001-platform-foundation/design.md
18. specs/001-platform-foundation/tasks.md

This reconciles the proposed18-file inventory:11 build/test/CI files and7 Markdown
files. Ignored canary POM/classes, resolved dependencies, private evidence and
target output are not publishable changes. No original REQ/AC text changed.
