# D1-08 Task A - safe PostgreSQL-aware health/readiness

Execution2026-10-17, Asia/Saigon; branch feature/d1-08-health-readiness;
baseline26afba781c1faad664687990dedecb1cf4ed57e1. **Local safety/regression DONE,
READY_FOR_PR**. This is the pre-publication evidence snapshot. Actual remote
checks/integration remain UNVERIFIED here; the authorized PR publication report
will record subsequent remote results. No auto-merge. Feature001/Day1 PARTIAL;
Task B Redis/RabbitMQ DESIGN/PENDING APPROVAL. D1-07 documentation closure is separate.

## Baseline, approval and root cause

Read root AGENTS, DoR, Feature001 spec/design/tasks, Day1/traceability, accepted
DB/architecture/DEC-29 boundaries, original Task A and its final review. Started
on the existing feature branch/26afba7 with the expected15 uncommitted Task A
files, no staged changes. Preserved all existing work; no reset/stash/rebase.
The user explicitly authorized minimal adapter/config/tests for timeout safety,
then commit/push/one PR only after all required safety/regression gates PASS.

The rejected configuration was a Hikari datasource property socketTimeout=3.
It affected all runtime JDBC socket reads, not only health. The prior isolated
pgJDBC42.7.13 characterization used SELECT pg_sleep(4):3-second setting failed
in3015ms, SQLSTATE08006/connection closed; default control completed4015ms.
No claim an existing business endpoint was measured above3s. Global query-read
policy was not approved, so final review stopped publication before any commit.

The fix removes that global property. Existing Hikari acquisition2s/validation1s
and PostgreSQL physical connect2s remain explicit profile-wide settings; none
limits the duration of an already-running business query. Flyway uses its existing
separate migration connection/configuration; no migration policy change.

## Alternatives and implemented design

| Option | Assessment |
|---|---|
| A - scoped validation using existing runtime pool | SELECTED: preserves detection of application-pool exhaustion and does not add resources/pools |
| B - separate health connection/pool | Rejected: could report UP while runtime pool cannot serve; extra credentials/resource/lifecycle complexity |
| C - unchanged Boot default DB indicator / HTTP or future timeout | Default indicator has no applicable public validation-timeout setter here; caller cancellation does not prove JDBC stopped. Not selected |

One small class DatabaseReadinessConfiguration in platform/internal/infrastructure
registers an Actuator HealthIndicator named dbHealthIndicator, profile demo.
Boot4.1.1 backs off its conditional default db contributor for this bean name.
No custom health framework, new domain module or dependency version change.
The runtime HikariDataSource is injected directly; no cross-module internal access.

Each probe:
1. Borrows exclusively from the configured application pool (acquisition2s).
2. Captures the existing network timeout and calls JDBC Connection.isValid(1).
3. Returns UP only for a valid open connection with the original timeout restored.
4. Evicts false/failed/unrestored connections through Hikari; closes every borrow.
5. Returns status-only DOWN on acquisition/validation/close/eviction exceptions,
   with no exception/credential/JDBC-URL details attached.

Pinned pgJDBC42.7.13 source verifies isValid(1) sends a real validation exchange,
sets its network timeout only if necessary, restores it in finally, and returns
false on socket/connection failure. Its socket timeout closes the broken physical
connection; explicit eviction prevents reuse. The adapter creates no executor,
future, background task or permanent per-connection network setting. H2 uses its
native isValid support. Unknown driver behavior that does not preserve state
fails closed; portability to other drivers is not claimed.

Primary sources read, not installed/upgraded:
- https://jdbc.postgresql.org/documentation/use/
- https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.13/postgresql-42.7.13-sources.jar
- https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-jdbc/4.1.1/spring-boot-jdbc-4.1.1-sources.jar

## Profiles, endpoints, status and security

Default/non-demo opts management access out/probes off; existing contextLoads
verifies no DataSource. demo opts in explicitly, without Kubernetes detection;
demo uses H2, demo,postgres uses PostgreSQL. Migration job remains non-web.
Liveness group=livenessState; readiness group=readinessState,db, membership
validation enabled. Same application port; no new endpoint in Demo001 OAS/SDK.
Only anonymous GET /actuator/health/liveness and /actuator/health/readiness are
permitted. Aggregate health, db/nested health, /actuator discovery, env/beans/
metrics and non-GET probe methods are denied; tested ADMIN denial too.
Web exposure=health only, discovery off/JMX excluded, details/components never.
Default mappings UP200 and DOWN/OUT_OF_SERVICE503 retained. Existing
/api/demo/v1/health stays constant200 UP/local-demo; it is not readiness.
JWT/RBAC/live-account/merchant scope/session/idempotency behavior is unchanged.

| Timing scope | Current implementation / limit |
|---|---|
| Pool borrow/validation | Hikari2000ms/1000ms, shared runtime-pool policy |
| Physical PostgreSQL connect | connectTimeout2s, not a running-query deadline |
| Borrowed health operation | JDBC-native isValid(1); pinned-driver scoped/restored validation timeout |
| Business socket reads | No repository-imposed global socketTimeout; tested network timeout0 before/after health |
| HTTP duration | Observed local latency only; no universal HTTP SLA added |
| Flyway/startup | Unchanged; runtime recovery is not proof of startup recovery |

## Real PostgreSQL safety tests

PostgresHealthSafetyTests runs with actual demo,postgres profiles and isolated
DynamicPropertySource DB credentials. Only its test runtime pool capacity is
one/min-idle0 for deterministic reuse/exhaustion; no production pool-size change.
Four mandatory PG tests, all PASS, no mocked JDBC:

| Method | Actual required proof |
|---|---|
| longRuntimeReadAndPhysicalReuseRemainSafeAfterSuccessfulProbe | Two SELECT pg_sleep(4) calls succeed before/after probe; same physical backend PID; timeout0 restored; connection still valid. Suite case8.020s |
| concurrentLongReadIsNotInterruptedByExhaustedPoolProbe | One-slot runtime pool holds real4s read while probe returns DOWN after acquisition budget; read completes4.011s; next probe UP |
| terminatedPhysicalConnectionIsDiscardedAndReplacementIsSafe | Same-runtime-user observer terminates only captured own test session; broken connection discarded by pool/indicator; replacement PID differs, timeout0 and validation valid |
| stalledSocketProbeIsBoundedEvictsAndRecoversWithoutTimeoutLeak | Ownership-checked tmpfs PG paused; two exact503 DOWN responses, liveness200, no borrowed-connection leak; unpause recovers; replacement PID differs and4s query succeeds. Case7.734s |

Broken-idle detection may happen inside Hikari before the indicator borrows;
first status can therefore be DOWN or UP on an already healthy replacement.
The test strictly requires a different valid physical PID, zero leaked borrows
and later UP. The stall test requires503, not a relaxed status assertion.
AfterAll verifies zero active borrows, closes the dedicated test context's pool,
and checks that its workers terminate; the test executor is also closed.

Docker operations only select exactly one CI-owned test label/name/tmpfs/no-volume
container and bind its label to the current PG_TEST_DB_URL suffix on5433. Only
that captured ID is paused/unpaused; finally revalidates ownership. Observer
termination is restricted to a PID borrowed from this isolated runtime pool.
No developer DB/container/volume or other application session is targeted.

## Commands, results and exact discovery

| Actual command / location | Exit and result |
|---|---|
| Root python -B packages/api-client/scripts/check-backend-baselines.py |0; Wrapper three clean verify modes, existing resolved offline repository, exact report guards/privacy/owned cleanup |
| Root python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v |0;15/15, including missing portable/PG health suite rejection for both full modes |
| Root python -B app/backend/verify-health.py |0; actual owned PG/HTTP UP-DOWN-RECOVERY, final aggregate privacy/cleanup PASS |
| packages/api-client npm.cmd run verify:c01 |0; Redocly0 errors/warnings/ignored, exact11 inventory,41/41/no skips |
| packages/api-client npm.cmd run test:wire |0;27/27,108 responses34 positive/74 negative,11 operations,0 mismatches; owned cleanup/privacy PASS |
| Git diff-check, exact inventory/UTF-8/links/bounded secrets |PASS:17 exact files, UTF-8/links/Python syntax,6 current private values absent; protected paths/original REQ/AC unchanged |

Wrapper mode arguments from app/backend are unchanged:
-o -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -Ddebug=false -Dtrace=false
plus clean verify; architecture adds -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests;
PG adds -Ddemo.test.database=postgres; H2 adds no mode flag.

| Mode | Discovered | Executed/PASS | Exact skips | Maven/guard |
|---|---:|---:|---:|---|
| Architecture |23 |23 |0 |0/0 |
| Default/H2 |56 |44 |12 PG-only methods |0/0 |
| PostgreSQL |56 |56 |0 |0/0 |

Four original portable health tests run in H2 and PG. Four new PG safety tests
are explicitly skipped in H2 alongside eight existing PostgresFoundationTests;
all twelve are mandatory PASS in PG mode. REQUIRED lists every exact method;
PG_ONLY contains only those two named suites, not a wildcard skip exception.
Unknown/missing/skipped-required/failed reports and nonzero Maven still fail.
Architecture remains11 production/scope+12 fixtures (11 forbidden/one allowed),
now covering38 production classes including the new infrastructure configuration.
Counts overlap across modes and are not a unique-test total. No assertions,
architecture rules or test selection were weakened; no CI jobs were added.

## Current local evidence and failure history

Final regression: .demo/d1-07-c06/backend-6fcf7962e1eb, three safe-reports folders,
Maven/guard exits0 and log privacy PASS; all four owned cleanup flags PASS.
Initial fix regression backend-14bd35031ac4 had one FAIL before pause: Java on
Windows lost embedded quotes in Docker label Go-template. Replaced it with
JSON-label inspection and exact parsing; preserved every safety assertion and
reran. It is not counted as PASS. Initial Task A duplicate-POM edit/callback
return-type failures and rejected socket3 policy are historical, not hidden.

Final actual HTTP health report: .demo/d1-08-health/runtime-f578359457ce.json;
new owned harness directory .demo/d1-07-c04/run-24d942271605. PG18.6 Debian,
new elogistics_wire_24d942271605, DB63223/backend63224 loopback ports, unchanged
Flyway1,2/five synthetic accounts. Readiness200 then paused PG503 in1018ms and
2025ms; liveness remained200. Unpause recovered200 in18ms,198ms through final
liveness/SQL confirmation, same backend process. These are local observations,
not an SLA or network-partition/startup/Flyway recovery certification.
Actual wire run .demo/d1-07-c03/run-3bc75e0fb1d6 also completed exit0,
27 tests/108 responses, unchanged V1/V2, privacy/cleanup PASS.
The reused C-04/C-06 output directory names do not modify those tasks' evidence.

## Privacy, cleanup and traceability

Raw context/env/passwords/JWTs/Maven/Surefire logs remain ignored. Public evidence
is allowlisted; log scans check generated credentials/JWT patterns. All harnesses
snapshot previous containers/volumes and remove only captured owned test IDs,
never -v/prune/down/reset/clean/repair. H2/domain data and PostgreSQL V1/V2 are
unchanged. Final bounded source/secret checks PASS before commit. Current test5 ports
5433,59959,59960,63223,63224 are closed;0 owned test containers remain.
This is not a full-history/supply-chain or exploitability audit.

TEST-001-HEALTH-01..04 retain general probes/security/runtime/regression mapping;
TEST-001-HEALTH-05 covers the four new real-PG timeout/reuse/concurrency/cleanup
cases. REQ-001-04/05, AC-001-01/04 subsets and NFR-O02/M02 gain evidence, not
full-feature acceptance. Redis/RabbitMQ, Testcontainers, full independent
cold-start/onboarding, broader CI/security maintenance and literal AC-001-03
red-then-fixed experiment remain open. Task B remains DESIGN/PENDING APPROVAL.

## Exact Task A changed-file inventory

Seventeen Git-visible files:12 modified,5 new; approved expanded adapter/test scope.

```text
app/backend/pom.xml
app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/DemoSecurity.java
app/backend/src/main/java/com/elogistic/backend/platform/internal/infrastructure/DatabaseReadinessConfiguration.java
app/backend/src/main/resources/application.properties
app/backend/src/main/resources/application-demo.properties
app/backend/src/main/resources/application-postgres.properties
app/backend/src/test/java/com/elogistic/backend/demo/FoundationHealthTests.java
app/backend/src/test/java/com/elogistic/backend/demo/PostgresHealthSafetyTests.java
app/backend/verify-health.py
scripts/ci/verify-reports.py
scripts/ci/test_verify_reports.py
docs/implementation/d1-08-health-readiness-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/design.md
specs/001-platform-foundation/tasks.md
```

No contract/generated SDK/Flyway/business semantics/architecture rule/workflow/
Docker/native/frontend baseline changes. Original REQ/AC are unchanged.
Only Task A files may be committed/pushed after final safety PASS; never merge.
