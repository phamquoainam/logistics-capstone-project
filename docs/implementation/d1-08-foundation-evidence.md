# D1-08 Task B — Redis/RabbitMQ foundation evidence

Execution2026-10-17, Asia/Saigon. Branch feature/d1-08-redis-rabbit-foundation;
HEAD/base f20c8c642849ce79a9585ae8c70fe42faaab1c0e. User explicitly approved
B-01..B-09 and local implementation/verification, not commit/push/PR or merge.
**LOCAL_DONE / READY_FOR_REVIEW** for approved Task B scope. Final real runtime,
backend, contract/wire and Task A HTTP regressions PASS; no mandatory local gate
waived. Remote Task B CI/publication/integration remain UNVERIFIED/not authorized.
Feature001/Day1 remain PARTIAL. [ADR-0005](../architecture/decisions/ADR-0005-foundation-dependencies.md),
[operator runbook](../runbooks/redis-rabbit-foundation.md).

## Baseline and approved scope

Started clean develop, local/origin/live remote f20c8c6; PR10 merged with parents
26afba781c1faad664687990dedecb1cf4ed57e1 and a63abc73ae7b74058135434afaa318da84a8cb55.
Task A actual develop push run38959330146 SUCCESS,23 architecture/44 H2+12
PG-only skips/56 PostgreSQL/guards15 per job; its scope is integrated DONE.
Created previously absent feature/d1-08-redis-rabbit-foundation without reset,
stash/rebase or rewriting existing work. Read AGENTS/DoR/Feature001/Day1/
dependency order/Task A proposal and explicit Task B approval.

No domain behavior, 11 API operations, OpenAPI/generated SDK, frontend/mobile,
V1/V2, developer PostgreSQL configuration, Task A JDBC adapter, architecture
rules or GitHub workflow/ruleset changes. No business cache, queues/exchanges,
consumers/messages, outbox/inbox, Testcontainers or remote CI expansion.

## Images, platform, license and dependency graph

Reverified official Docker Registry tag/index bytes and SHA256, selected
linux/amd64 manifest, pulled exact approved pins and inspected Docker's local
linux/amd64 identity/RepoDigests/Descriptor. No substitutions:

```text
redis:8.4.7-trixie@sha256:43afe4a9a43080e64dfa29699eef903fc9d54991fe8b03d57237c05bed2a396c
rabbitmq:4.3.6@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620
```

| Image | linux/amd64 manifest |
|---|---|
| Redis | sha256:3cec5b939d7b6a96e3850ba39e841c6304a0f948c1824c720b8e7d4bb94738e5 |
| RabbitMQ | sha256:b5486a425d396b3d11e4e1fe41ff503eca3e85576bdc20db112369f6387e639f |

Docker Engine29.6.2/Desktop4.84.0, Compose5.3.1, Linux/amd64 were actually used.
The containerd image store reports Image.Id as the OCI index digest here, not
the config digest. An initial pre-run verification assertion assumed otherwise;
Descriptor/RepoDigests/platform resolved it without changing any approved pin.

User selected Redis AGPLv3, unmodified official server. Retained upstream full
license and [notice/compliance record](../../infra/redis/NOTICE.md). License-file
SHA256=4a0e416b9537688f30dfe69ddaceb2ca64d96b7df02a0a6760d376890ddc4e40.
No image redistribution/server fork occurs. Source/notice obligations must be
reviewed for the actual future distribution model; no blanket compliance claim.

Added only Boot-managed starter-data-redis and starter-amqp; existing Java21,
Boot4.1.1/Maven3.10.0 unchanged. Resolved packaged graph includes Lettuce7.5.2.RELEASE,
Spring Data Redis4.1.1, Spring AMQP/Rabbit4.1.1, Rabbit client5.30.0,
Netty4.2.17.Final and Reactor3.8.7, with no explicit version overrides.
New container/transitive Java vulnerability certification remains UNVERIFIED;
functional compatibility below is not a zero-advisory claim. Existing SDK dev4
HIGH/mobile58 full/35 omit-dev/web0 snapshots are unchanged, not re-audited/fixed.
Maintenance owner: repository owner; Rabbit4.3 community support ends2027-01-31.

## Security, networking and data policy

Standalone additive Compose, generated names, own bridge; does not include/start/
recreate developer PostgreSQL. Only Redis127.0.0.1:6380->6379 and Rabbit
127.0.0.1:5673->5672 published. Actual inspection rejects other published ports;
no15672/4369/25672 host exposure. Image pulls do not start unrelated tools.

Redis default user disabled; named foundation_probe has only PING/INFO and
required HELLO/SELECT/CLIENT SETNAME/SETINFO. No +@all/business key writes.
Correct auth/PING/INFO PASS, incorrect password/default user rejected, SET denied.
Redis /data tmpfs, RDB/AOF disabled; never a correctness store.

Rabbit user foundation_probe, no admin tags, dedicated /elogistics-foundation,
deny-all resource regexes. Correct authenticated channel/RPC PASS; wrong password,
wrong vhost and queue declaration rejected by actual broker. No RabbitAdmin
declaration/listeners, application queues/messages or custom exchanges. Definitions
use salted password hashes and core import, not the management plugin. Stable
hostname foundation-rabbit and new Rabbit named volume preserve node metadata.

Credentials, ACL/definitions and raw diagnostics remain ignored .demo only.
Bootstrap refuses unknown volumes/projects, retargeted state and mismatched saved
bytes; rerun preserves credentials. Windows protected directory DACL restricts
the new private task tree to current owner/SYSTEM/Administrators, files inherit
effective access. POSIX directory/file modes700/600. No global ACL/settings change.
Private files/hashes are sensitive even when not plaintext. No password CLI args;
Redis test passwords use binary stdin to preserve bytes on Windows.

Test-only fixture_writer has limited synthetic key commands; it is absent from
development bootstrap. A test-admin-created marker vhost proves Rabbit persistence
independently from startup reimport. All such resources are disposable and owned.

## Profile/probe implementation and source evidence

Default/demo/demo,postgres/migration exclude new client auto-configurations.
Foundation alone opts clients in; reactive Redis/repositories remain excluded.
Full target requires demo,postgres,foundation; missing/blank credentials and
incomplete profile combinations fail startup. Full readiness=readinessState,db,
redis,rabbit; liveness=livenessState. Exact anonymous GET/status-only exposure and
all existing business/security contracts remain unchanged.

Boot-native Redis health performs authenticated INFO. Actual Boot4.1.1 native
Rabbit health was characterized UP while broker paused because a cached channel
returns server properties without a fresh AMQP exchange. Standalone native test
PASS in .demo/d1-08-foundation/characterize-8780adf45935, then retained in the final
real-infrastructure suite, where native UP contrasts with active DOWN.

The approved fallback is a small platform Actuator contributor: health-only
Rabbit factory, fresh authenticated connection, dedicated channel, synchronous
basicQos RPC and unconditional connection abort/close. No queue/message/consumer,
application RPC/prefetch mutation or custom background recovery/executor. Tests
verify repeated probe worker cleanup and unchanged application RPC timeout;
broker inspection finds no remaining readiness connections.

Configured/checked Redis connect1s and command2s. Probe-only Rabbit connect,
handshake/RPC/shutdown2s each, heartbeat10s. Existing application Spring clients
remain Boot-managed; Task A Hikari/JDBC policies are unchanged. Fault windows
exercise unresponsive and stopped services, not every possible DNS/TCP partition.
The12s/30s targets are measured acceptance budgets, not an HTTP SLA or an isolated
measurement of every individual protocol timeout.

## Final real runtime evidence

Directory .demo/d1-08-foundation/run-e4a652d0bd75:
safe-results.json PASS,16 grouped gates; safe-reports exact5/5/no skip; safe-cleanup
all true. Actual missing-credential and incomplete-profile JAR startups fail for
the intended configuration reason. Healthy readiness200 in45ms, liveness200 in7ms.
The same backend process handles every following failure/recovery:

| Dependency/fault | Two readiness503 observations, ms | Recovery200, ms | Liveness |
|---|---|---:|---|
| Redis pause |2066 /2071 |22 |200 |
| Redis stop/start |2091 /2097 |3994 |200 |
| Rabbit pause |1026 /1023 |42 |200 |
| Rabbit stop/start |40 /8 |3131 |200 |
| PostgreSQL pause |1045 /2037 |32 |200 |
| PostgreSQL stop/start |2020 /2035 |812 |200 |

Every response is exactly status-only UP/DOWN; all detection<12s and recovery<30s.
These are local observations. Runtime recovery is not failed-Flyway startup recovery.
Redis synthetic key exists before each restart/recreate and is absent afterwards;
runtime user cannot write it. Rabbit marker vhost/user/vhost metadata survive both
restart and forced recreate, authenticated full readiness recovers, no queues exist.
Recreate uses a different container ID with the same owned data volume/node identity.

Full harness uses a fresh labeled PostgreSQL test volume for stop/start retention;
it reuses existing CI bootstrap/role SQL without changing its source/migrations.
Standard PostgreSQL/Task A regressions still use their original tmpfs fixture.
No developer database/session is targeted. Cleanup verifies captured ownership,
removes only new test containers/volumes/network and preserves complete baseline
container/volume/network inventories. Backend stopped and6380/5673/5433 released.

## Commands and regression inventory

| Actual command / location | Result |
|---|---|
| app/backend Wrapper -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -DskipTests package |0; new starters resolved with existing BOM; not counted as tests |
| Root python -B -m unittest discover -s infra/foundation -p 'test_*.py' -v |0;7/7 bootstrap/collision/hash/private-state safety |
| Root python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v |0;17/17 fail-closed report/privacy tests |
| Root python -B packages/api-client/scripts/check-backend-baselines.py |0; three clean Wrapper verify modes, exact discovery/guards/privacy/owned cleanup |
| Root python -B app/backend/verify-foundation.py |0; real5/5 JUnit +16 runtime/config/persistence gates and all cleanup/privacy checks |
| packages/api-client npm.cmd run verify:c01 |0; strict lint0 errors/warnings/ignored; exact11 inventory;41/41 tests/no skips |
| packages/api-client npm.cmd run test:wire |0 on unchanged-command retry;27/27,108 responses34 positive/74 negative,11 operations,0 mismatches; prior Node/libuv crash disclosed below |
| Root python -B app/backend/verify-health.py |0; actual PG HTTP UP/DOWN/RECOVERY, liveness independence, status-only security, cleanup/privacy PASS |

Final backend directory .demo/d1-07-c06/backend-cc76fdd79075:

| Mode | Discovered | Executed/PASS | Exact skips |
|---|---:|---:|---:|
| Architecture |23 |23 |0 |
| H2 |65 |48 |17 |
| PostgreSQL18 |65 |60 |5 |
| Explicit foundation |5 |5 |0 |

Architecture covers39 production classes, implemented identity/merchant/orders/
platform; same23 rules/effectiveness tests,11 negative violations/one positive
control. Six absent business modules remain N/A. H2 skips12 existing PG-only cases
plus5 explicit foundation cases; PG skips only those5 infrastructure cases, all
executed in separate real foundation mode. No unknown/missing/failed/skipped-
required test accepted. New3 config validation tests and1 profile-isolation test
run in both regular database modes. Task A four PG safety tests all execute PASS;
V1/V2 cold/upgrade histories remain1,2. Counts overlap; not a unique-test total.
CI workflow/ruleset unchanged; new Task B remote CI is UNVERIFIED.

## Disclosed unsuccessful attempts and corrections

- Initial image identity assumption confused OCI index ID with config ID on Docker
  containerd store; corrected metadata verification, never changed image bytes/pins.
- First native Rabbit characterization timed out before pause because an oversized
  Docker inspect stdout filled Windows process pipe. Limited output to exact ID/
  labels, same timeout/assertions; native cached-UP characterization then PASS.
- Early auth harness did not collect Redis CLI auth stderr; binary stdin correction
  also removed Windows CRLF from piped password. Correct server credentials/ACL
  were retained. Failed runs are not counted as service PASS.
- First full PG stop/start attempt used tmpfs, which loses the synthetic database
  on container stop. Corrected test topology to a new labeled retained test volume;
  actual restart/recovery then PASS without increasing any timeout or changing DDL.
- Initial Windows recursive inheritance removal left generated private files with
  ineffective access entries, caught by bootstrap rerun tests. Fixed protected
  root/child inheritance within Task B files, retained bytes/credentials;7/7 PASS.
- Wire attempt .demo/d1-07-c03/run-86a3558d13e2 failed before cases: Redocly bundled
  successfully but Node24.13.0 Windows libuv asserted UV_HANDLE_CLOSING. Cleanup/
  privacy PASS; unchanged-command retry run-263c0064e39e PASS27/27,108 responses,
  all11 operations/0 mismatches, owned cleanup/privacy PASS. No dependency/contract/
  assertion fix; Windows toolchain flakiness remains an OPEN maintenance finding.

Earlier full runtime run-8a1a1d8fc951 also PASS, before strengthened timeout/worker/
startup assertions. Final run-e4a652d0bd75 above supersedes it. Failed attempts
cleaned only their owned test resources; none is silently reported PASS.

## Exact changed-file inventory

31 files including this evidence; no generated SDK/native/build artifacts:

```text
.gitignore
app/backend/pom.xml
app/backend/src/main/java/com/elogistic/backend/platform/internal/infrastructure/FoundationConfiguration.java
app/backend/src/main/resources/application.properties
app/backend/src/main/resources/application-demo.properties
app/backend/src/main/resources/application-postgres.properties
app/backend/src/main/resources/application-migration.properties
app/backend/src/main/resources/application-foundation.properties
app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java
app/backend/src/test/java/com/elogistic/backend/demo/FoundationDependencyTests.java
app/backend/src/test/java/com/elogistic/backend/demo/FoundationProfileTests.java
app/backend/src/test/java/com/elogistic/backend/platform/internal/infrastructure/FoundationConfigurationTests.java
app/backend/verify-foundation.py
infra/compose/foundation.yaml
infra/foundation/manage.py
infra/foundation/test_manage.py
infra/redis/redis.conf
infra/redis/LICENSE.txt
infra/redis/NOTICE.md
infra/rabbitmq/rabbitmq.conf
scripts/ci/verify-reports.py
scripts/ci/test_verify_reports.py
docs/architecture/decisions/ADR-0005-foundation-dependencies.md
docs/decisions/open-questions.md
docs/implementation/d1-08-foundation-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
docs/runbooks/redis-rabbit-foundation.md
specs/001-platform-foundation/design.md
specs/001-platform-foundation/tasks.md
```

## Scope closure and remaining gates

All mandatory approved Task B local gates PASS; LOCAL_DONE, ready for owner review.
Wire final directory .demo/d1-07-c03/run-263c0064e39e; Task A HTTP final evidence
.demo/d1-08-health/runtime-f8e07553f200.json and owned harness
.demo/d1-07-c04/run-4d76b915da79. Both cleanup/privacy PASS. This is not a claim
that the preceding Node/libuv toolchain crash was fixed.
No commit/push/PR/merge. Protected paths/original spec and requirements unchanged;
bounded UTF8/link/secret/artifact audit PASS, no staged/generated/private files.
Final bounded source audit checked43 generated private credential/hash values;
effective private DACL PASS. Docker final inventory retains4 existing containers
and5 volumes,0 Task B containers;6380/5673/5433 are released. This is not a full
repository-history or supply-chain security audit.

Trace REQ-001-04/05, AC-001-01/04 subsets, EC-001-02, NFR-O02/M02 and
TEST-001-FOUNDATION-01..04. No REQ/AC text is amended. Full clean-clone combined
onboarding/independent developer AC-001-05, Testcontainers, literal red-then-fixed
CI experiment, contract/SDK/web/mobile CI expansion and open dependency/security
maintenance remain separate. No new owner decision is required for already approved
local Task B configuration; remote publication/integration needs separate approval.
