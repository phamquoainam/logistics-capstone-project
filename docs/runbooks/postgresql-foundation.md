# PostgreSQL foundation — D1-03 local runbook

Scope: approved DEC-26 / [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md). [Execution evidence](../implementation/d1-03-postgresql-evidence.md). One backend artifact, Java 21 / Boot 4.1.1 / Maven 3.10.0 unchanged. Prerequisites: running Docker Desktop Linux Engine/Compose, Java, Python and Node/npm already verified.

## Start infrastructure

From repository root:
```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\infra\postgres\start-local.ps1
```
ExecutionPolicy applies only to the child process. Script finds Docker on PATH or its per-user Desktop installation. It validates Compose, starts the pinned image, waits for health and runs a real SQL connectivity query. First launch rejects an existing named volume without local settings; subsequent launch verifies project ownership. No prune/down/delete/reset actions are built into this workflow.

| Item | Configuration |
|---|---|
| Project/container | elogistics-d1 / elogistics-d1-postgres18 |
| Image | postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c |
| Database/host bind | elogistics_dev / 127.0.0.1:5433:5432 |
| Named volume/mount | elogistics_d1_pg18_dev_data / /var/lib/postgresql |
| Bootstrap administrator | elogistics_bootstrap; only isolated container administration |
| Object/database owner | elogistics_owner, NOLOGIN |
| Migration/runtime login | elogistics_migrator / elogistics_runtime, separate credentials |

Private generated settings/password files live in ignored `.demo/postgres/`; seed/token credentials remain in `.demo/settings.json`. The tracked `infra/postgres/.env.example` contains placeholders only. Compose secrets mount password files; SQL receives passwords through psql environment variables, never tracked literals. Preserve settings together with their volume. Do not regenerate credentials to resolve a login failure.

Native PostgreSQL 18 on E/5432 is unrelated. This workflow does not migrate H2 data or attach an existing database directory.

## Build and run backend/web

Stop only this backend before rebuilding the JAR on Windows. From repository root:
```powershell
$foundationMavenRepo = Join-Path (Get-Location).Path '.demo/maven-repository'
Push-Location app/backend
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=$foundationMavenRepo" -Ddebug=false -Dtrace=false verify
Pop-Location
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\app\backend\run-postgres-demo.ps1
```
Launcher activates `demo,postgres` in that order, loads private local credentials, migrates with the migrator and serves at 127.0.0.1:8080 through the runtime DataSource. Seed creates missing synthetic accounts; existing hashes/roles are not reset. The same accounts and UI contract as [Demo-001](demo-local.md) apply.

In another terminal:
```powershell
Push-Location app/frontend/web
npm.cmd run dev
```
Open http://127.0.0.1:5173/. Vite proxies /api to the backend. H2 fallback still uses `run-demo.ps1` and its existing `db/demo-migration/V1`; run H2 and PostgreSQL on different backend ports if comparing them.

## Migrations, roles and execution

PostgreSQL history is `platform.flyway_schema_history`. V1 creates nine ownership schemas; V2 creates approved merchant/account/shipment/event/operation tables. Seeds are application runners outside SQL migrations. UTC TIMESTAMPTZ, UUID, numeric weight, uniqueness/status/weight/FK constraints preserve Demo-001 semantics. Runtime events/receipts have SELECT/INSERT only. Runtime does not own tables, create schemas/tables, alter/drop/truncate or read Flyway history.

Six unimplemented domain schemas intentionally have no business tables. All object owners use the NOLOGIN DB owner; domain ownership is enforced by application public APIs, not separate runtime DB logins. Automated architecture verification is still required.

Shared environment: supply PG_DB_URL, PG_MIGRATOR_USER/PASSWORD, PG_OWNER_ROLE and PG_RUNTIME_USER to a controlled migration job; execute the same JAR with `--spring.profiles.active=migration --debug=false --trace=false`. Job has no HTTP server/seed and exits after migration/validate; failure exits nonzero and stops deployment. Deploy runtime with profiles `demo,postgres`, `PG_MIGRATION_ENABLED=false`, seed disabled and only runtime DB credentials plus approved auth configuration. No migration secrets are required in runtime. Shared deployment itself has not been performed.

Local simulation using generated settings:
```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\app\backend\run-postgres-demo.ps1 -MigrationOnly
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\app\backend\run-postgres-demo.ps1 -RuntimeOnly
```
Modes are mutually exclusive. Local runtime-only simulation suppresses migrator credentials and seed.

## Verify

Stop the running backend before the Maven command:
```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\app\backend\verify-postgres.ps1 -Offline
```
Omit `-Offline` when dependencies are not cached. Each run provisions new `elogistics_test_<suffix>` and upgrade test databases with separate test owner/migrator/runtime roles inside the isolated container. Names/credentials are in `.demo/postgres/test-run-*.json`. Tests reject a dev/native URL. Test databases are retained; no automatic dropping/cleanup occurs. This is real PostgreSQL integration, not H2 or a Testcontainers execution claim.

Suite: original contextLoads plus 13 Demo-001 regressions and eight PostgreSQL tests. Tests cover actual engine/principals, schema ownership, denied DDL/history/owner role, immutable events/receipts, checksums/validate/rerun, V1→V2 upgrade with retained technical test data, FK-triggered transaction rollback and concurrent cancel. Existing suite covers 401/403, cross-merchant 404, invalid input, replay/conflict and concurrent create.

With PostgreSQL backend and web running:
```powershell
python app/backend/smoke-demo.py create --database postgres
# Stop only this backend. Restart only elogistics-d1-postgres18 using Docker Desktop/CLI.
# Run start-local.ps1 to wait for health, then run-postgres-demo.ps1 again.
python app/backend/smoke-demo.py verify --database postgres
Push-Location app/frontend/web
npm.cmd run typecheck
npm.cmd run build
npm.cmd run test:e2e
Pop-Location
```
Smoke evidence path is `.demo/evidence/postgres-persistence-proof.json`; original H2 proof is preserved. `--database` selects an evidence namespace, not a database switch; engine verification is separate in integration tests/logs. Optional `--base-url` permits checking H2 on another port. Browser tests require Edge already installed and local demo settings.

Do not modify applied SQL/checksums or run Flyway clean/repair/baseline. Preserve failing DB/log/history, investigate and write a reviewed forward migration. Do not use `docker compose down -v`, volume deletion or cache pruning as troubleshooting. Role names used in Flyway placeholders must remain stable for that history; changes require an explicit migration/review.

No web lint script exists yet. CI architecture gate, Testcontainers provisioning and independent clean-machine reproduction remain separate unverified gates; this runbook does not close full feature 001.
