# Local Redis/RabbitMQ foundation

Scope: approved [ADR-0005](../architecture/decisions/ADR-0005-foundation-dependencies.md).
One Spring Boot artifact; no business caching/messaging. PostgreSQL instructions
and volumes remain unchanged. [Evidence](../implementation/d1-08-foundation-evidence.md).

## Prerequisites

Docker Desktop Linux engine/Compose, approved Java21/Boot4.1.1/Maven3.10.0,
Python3.14 was verified locally (Windows launcher requires3.12+ for junction
checks), standard library only. Commands below run at repository root. Docker
CLI may be on PATH; on Windows the existing Desktop user-install path is a fallback.
Run with the same Windows account that owns private settings. Ports6380/5673 must
be free or already belong to this exact saved project; do not stop other services.

Pull exact approved pins, never latest or an alternate digest:

```powershell
docker pull --platform linux/amd64 redis:8.4.7-trixie@sha256:43afe4a9a43080e64dfa29699eef903fc9d54991fe8b03d57237c05bed2a396c
docker pull --platform linux/amd64 rabbitmq:4.3.6@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620
python -B infra/foundation/manage.py up
python -B infra/foundation/manage.py status
```

Bootstrap generates credentials once under ignored .demo/foundation, configures
private directory permissions and verifies saved bytes/ownership on rerun. It
does not print secrets, place passwords on command arguments, overwrite mismatched
private files, pull alternate images or delete/reset any resource. If a volume
exists without its private settings, stop and investigate ownership/credential
recovery. Preserve those settings with restricted access for persistent Rabbit
data; do not regenerate credentials against an existing volume.

Redis publishes127.0.0.1:6380 only; Rabbit127.0.0.1:5673 only. No management UI.
Compose project elogistics-foundation uses its own bridge and Rabbit volume
elogistics_d1_rabbitmq_dev_data. Redis is ephemeral; Rabbit hostname/node identity
and metadata persist. A green container diagnostic is not application readiness.

## Backend target

Prepare PostgreSQL and existing Demo private configuration using the approved
[PostgreSQL runbook](postgresql-foundation.md); this launcher does not start or
modify PostgreSQL containers. Build the current backend JAR with Wrapper verify.
Then:

```powershell
python -B infra/foundation/manage.py run-backend
```

It reads existing ignored PostgreSQL/Demo settings, supplies credentials in child
environment and starts demo,postgres,foundation. It does not seed new users; normal
Flyway V1/V2 validation/migration policy remains unchanged. This developer launcher
is provided for explicit operator use; Task B verification uses owned synthetic
databases, never the developer database. Missing settings must be resolved using
the existing runbooks, not reset/overwritten.

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/liveness
Invoke-RestMethod http://127.0.0.1:8080/actuator/health/readiness
```

Healthy responses are exactly status UP/HTTP200. A required dependency failure
gives readiness DOWN/HTTP503; liveness remains UP while the backend is healthy.
Aggregate/nested health, config/management endpoints and non-GET probes stay denied.
The existing /api/demo/v1/health is a demo endpoint, not dependency readiness.

demo and demo,postgres retain their existing commands and require no Redis/Rabbit.
Missing/blank foundation credentials or missing demo/postgres profile fails startup.
Dependency-down startup is not a migration bypass: failed PostgreSQL/Flyway startup
is distinct from runtime dependency recovery.

## Isolated verification

```powershell
python -B -m unittest discover -s infra/foundation -p 'test_*.py' -v
python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
python -B app/backend/verify-foundation.py
python -B packages/api-client/scripts/check-backend-baselines.py
```

The foundation harness requires6380/5673/5433 free. It creates fresh labeled
Redis/Rabbit containers/network/volume and PostgreSQL test volume/roles/databases,
runs actual JUnit and HTTP checks, then removes only its captured owned resources.
PostgreSQL test data uses a fresh labeled volume so stop/start retains the fixture;
tmpfs is unsuitable for that particular restart assertion. Existing Task A safety
regressions still run separately with their original owned tmpfs PostgreSQL.

Never point the fault harness at developer containers or manually pause them.
Evidence/private logs are under ignored .demo/d1-08-foundation/run-*. Public
summaries contain allowlisted counts/latencies, not env/JWT/raw Surefire output.
Failures retain private diagnostics; cleanup checks existing resource inventories.
Redis fixture-writer and Rabbit persistence-marker vhost exist only inside the
disposable test harness, not the development bootstrap or application permissions.

Timeout targets are measured, not SLAs. Do not increase budgets, add global JDBC
socket deadlines, grant broad privileges or skip tests to hide failures. Inspect
the bounded test result and private diagnostics; preserve existing resources.
