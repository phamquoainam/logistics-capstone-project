# D1-03 — PostgreSQL execution evidence

## Reconciliation after D1-04 — 2026-10-14

D1-03 was merged by PR #3 into develop `ccafeb4`; merge is not an acceptance gate.
The historical evidence below proves the approved local PostgreSQL milestone and
local build/test/migration deliverables. Current D1-04 regression also passes:
48 PostgreSQL tests without failure/error/skip, H2 40 executed/pass plus eight
PG-only skips, web typecheck/build/three browser tests PASS. Architecture local
verification is now PASS; it is no longer an unimplemented dependency.

Keep **D1-03 overall PARTIAL** under the existing Day 1 dependency/closure scope:
D1-02 is not closed, independent clean-machine reproduction is still unverified.
Those conditions are not silently waived by merging or local DB/architecture PASS.
CI/full feature 001 gates remain separate and unverified; no AC change. Proposed
D1-04 local DONE and next independent CI slice are documented in
[D1-04 evidence](d1-04-architecture-evidence.md). Historical “next D1-04” notes
below refer to the earlier D1-03 execution, not current next-task advice.

2026-10-13 (+07), explicit user approval of DEC-26 and Phases A–E. Baseline clean develop HEAD `08c0c3424ce30c5f3c74c3af6a80d92a04e8a1ef`; new branch `feature/d1-03-postgresql-foundation`. No commit/push/PR/merge. [Runbook](../runbooks/postgresql-foundation.md), [ADR](../architecture/decisions/ADR-0002-database-ownership.md).

## Environment/configuration

Java 21.0.10 / Boot 4.1.1 / Maven 3.10.0 unchanged. Boot BOM resolves PostgreSQL JDBC 42.7.13 / Flyway 12.4.0. Docker Engine 29.6.2 / Compose 5.3.1, Linux/amd64; data D:\DockerDesktopData\DockerDesktopWSL. Readiness free C/D/E: 1.56/97.04/37.32 GiB; no observed disk failure.

Manifest reverified by `docker buildx imagetools inspect postgres:18.6-bookworm` before pull; index digest `sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c`, amd64 manifest `sha256:9551dd5bf356409a21b31248f49b33bba9bb1877365f3a7e933aecd9af3cf52d`. SQL reports PostgreSQL 18.6 Debian. New project/container elogistics-d1/elogistics-d1-postgres18, DB elogistics_dev, bind 127.0.0.1:5433:5432, new volume elogistics_d1_pg18_dev_data at /var/lib/postgresql. Volume absence checked before creation. Native postgres PID 8596 still listens on 5432; other Docker resources untouched.

NOLOGIN owner owns DB/schemas/tables; migrator can SET ROLE owner; runtime only required DML, no owner membership. Nine domain schemas plus platform; only five approved demo tables. PostgreSQL global V1/V2 history platform.flyway_schema_history; H2 location/applied V1/public history unchanged. No cross-schema FKs, speculative P0 tables, H2 conversion or framework version changes.

## Actual verification

Docker used the installed per-user executable (not on session PATH). Backend commands use ignored D cache; command paths below are repo-relative unless backend/web directory is stated.

| Gate / actual command | Result | Evidence / limit |
|---|---|---|
| Git status/branch/HEAD, branch absence; `git switch -c feature/d1-03-postgresql-foundation` | PASS | Clean baseline, no overwrite |
| `powershell.exe -NoProfile -ExecutionPolicy Bypass -File infra/postgres/start-local.ps1` | PASS | Verified digest pull, resource collision guards, Compose config, healthy container, actual SQL connectivity |
| Docker inspect health/ports/mounts | PASS | healthy; loopback 5433; approved named volume/mount |
| Online backend `mvnw.cmd -B -ntp -Dmaven.repo.local=D:\TLCN\e-logistic\.demo\maven-repository verify` | PASS | New BOM dependencies resolved, original 14 tests pass |
| Final H2 `mvnw.cmd -o -B -ntp -f=app/backend/pom.xml -Dmaven.repo.local=D:\TLCN\e-logistic\.demo\maven-repository -Ddebug=false -Dtrace=false verify` | PASS | 14 executed/pass; eight PostgreSQL-only conditionally skipped; 21:00:46 +07 |
| `powershell.exe ... -File app/backend/verify-postgres.ps1 -Offline` | PASS | Final 21:05:17 +07: 22 tests, 0 fail/error/skip. 13 demo + eight foundation use PostgreSQL; original contextLoads intentionally has no datasource |
| Auth/RBAC/merchant/shipment regressions | PASS | BCrypt; disabled/expired/tampered token; 401/403; A/B 404; invalid input; replay/conflict 409; DRAFT/cancel/events/admin; seven auth + six shipment tests |
| Ownership / separate principal | PASS | Nine schemas + platform owned by owner; five tables + history; runtime connections elogistics_runtime, Flyway principal migrator then SET ROLE owner |
| Runtime permission isolation | PASS | CREATE schema/table, ALTER/DROP/TRUNCATE, history and SET ROLE denied SQLSTATE 42501; non-superuser/non-CREATEDB/non-CREATEROLE |
| Event/receipt immutability | PASS | Runtime UPDATE/DELETE denied on both tables |
| `run-postgres-demo.ps1 -MigrationOnly` cold-start | PASS | Empty dev DB V1→V2; no HTTP/seed; validate and exit 0 |
| Flyway checksum/validate/rerun | PASS | Two versions/checksums unchanged, zero rerun migrations; account count retained |
| Upgrade V1→V2 | PASS | Isolated upgrade DB retains technical probe written after V1, applies V2, validates/reruns zero. Not a production/P0 populated business-data upgrade claim |
| PostgreSQL transaction/concurrency | PASS | Concurrent create/cancel produce one effect; failed FK rolls back earlier write; weight/status constraints enforced |
| Web `npm.cmd run typecheck`; `npm.cmd run build` | PASS | Vite 8.3.4, 25 modules, JS 253.67 kB/gzip 79.87 kB; no web dependency/lock changes |
| Web `npm.cmd run test:e2e`, installed Edge | PASS | Three tests; 7.5s initial, 7.3s final runtime-only after restart; real render/create/detail/cancel/isolation/admin/retry |
| `python app/backend/smoke-demo.py create --database postgres` | PASS | ID 8d76636e-e2b6-4a70-bfb1-6efb25b0bf62 / PERSIST-5c1847696585, DRAFT |
| Stop own backend; `docker restart elogistics-d1-postgres18`; start-local health | PASS | Same volume, no reset/deletion/bootstrap replay |
| Migration job rerun after restart | PASS | Two validated, version 2, no migration necessary, exit 0 |
| `run-postgres-demo.ps1 -RuntimeOnly`; `python ... smoke-demo.py verify --database postgres` | PASS | No migrator credentials/seed; same ID/createdAt/one event and durable receipt replay |
| Direct SQL after restart | PASS | Same DRAFT; one creation event/receipt; V1 checksum -1533163014, V2 1234525598 unchanged |
| Existing H2 runtime DEMO_PORT=8082; `python ... smoke-demo.py verify --database h2 --base-url http://127.0.0.1:8082` | PASS | Old ID 5e98258b-f5d3-44e0-9234-574605ca0034 / PERSIST-51f90ead5948, timestamp/event/receipt preserved; public V1 validates/reruns zero |
| Git diff/secret audit | PASS | git diff --check clean; 32 changed/new Git-visible files scanned against 16 generated private values, no leaks; private settings/H2 data ignored. H2 applied migration/profile and web lock unchanged |
| Shared remote deployment | UNVERIFIED | Job/runtime separation executed locally only |
| Architecture/CI / Testcontainers provisioning | UNVERIFIED | Real PG via managed Compose test DBs, not a Testcontainers run |
| Web lint | UNVERIFIED | No lint script exists |
| Clean-machine reproduction / remaining D1-02 gates | UNVERIFIED / PARTIAL | Same machine/cache does not close these gates |

## Isolation/artifacts/failures

Final test DB elogistics_test_91be82a1b660 and upgrade elogistics_test_upgrade_91be82a1b660. Each run creates fresh test roles/DBs; URL guards reject dev/native targets. Earlier test runs retained; no DROP/role cleanup. Private settings `.demo/postgres/test-run-*.json` remain ignored.

Ignored logs under `.demo/postgres/`: maven-postgres-final.log, maven-h2-final.log, migration-job.log, migration-job-rerun.log, backend.log, backend-runtime-only.log, h2-runtime-regression.log, web-smoke-final.log. Surefire snapshots surefire-postgres/ and surefire-h2/; current target reports final PG. Screenshots in `.demo/evidence/`; postgres-persistence-proof.json is separate from preserved H2 persistence-proof.json. Do not share settings/raw auth traces.

Resolved: duplicate PostgreSQL POM declarations removed before successful builds; first PostgreSQL test incorrectly expected owner on raw Flyway DataSource connection before initSql, corrected to verify configured SET ROLE and resulting owner (permissions were not loosened); Windows quoting broke Docker label/mount templates, replaced with JSON inspection and rerun passed. H2's existing Flyway verified-version warning and Playwright color-env warning are nonfatal; no version downgrades.

## Closure

Approved PostgreSQL/Demo-001 DB milestone verified. **D1-03 remains PARTIAL**: unfinished D1-02 dependency and independent clean-machine reproduction remain; architecture/CI/full feature gates are unverified. No AC reduction, Redis/RabbitMQ implementation, production migration or H2 conversion.

Highest-priority independent next task: **D1-04 automated module-boundary verification**, with a negative fixture proving forbidden cross-module internal dependency fails. Complete environment/reproducibility gates before full Day 1/feature 001 closure.

## Changed files

32 files, full local inventory `.demo/postgres/changed-files.txt`:
- Backend: pom.xml; run-postgres-demo.ps1; verify-postgres.ps1; smoke-demo.py; platform/internal/MigrationJob.java; application-postgres.properties; application-migration.properties; db/migration/V1__ownership_schemas.sql and V2__demo_identity_merchant_drafts.sql; DemoTestSupport.java and PostgresFoundationTests.java.
- Infrastructure: infra/compose/postgres.yaml; infra/postgres/.env.example, bootstrap.sh, bootstrap.sql, start-local.ps1, prepare-test.py.
- Documentation: ADR-0002, data-ownership.md, open-questions.md, day-1-plan.md, demo-001-evidence.md, this evidence, traceability.md, local-development.md, demo-local.md, postgresql-foundation.md.
- Specs: foundation spec/design/tasks metadata/evidence, demo design/tasks evidence; original acceptance criteria unchanged.

Final operational state: PostgreSQL container healthy; backend at 127.0.0.1:8080 in runtime-only mode after verified migration job; Vite at 127.0.0.1:5173. Temporary H2 smoke backend on 8082 stopped. No database deletion or native service changes.
