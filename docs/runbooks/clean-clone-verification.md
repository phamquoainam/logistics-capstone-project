# Isolated clean-clone verification — AC-001-01

## Credential store Phase 1 boundary

[Protected-store evidence](../implementation/ac-001-04-credential-store-evidence.md)
adds a read-only migration dry-run and synthetic security/launcher tests.
The standalone verification bootstrap below remains unchanged, session-owned and
independent of developer credentials. Do not point it at the protected development
store or interpret this phase as migration of retained verification sessions.
Existing private evidence is preserved. New protected PostgreSQL test records
have their own purpose-bound sessions; real transfer/retirement is unapproved.
No AC-001-04/05 acceptance, raw-cache finding closure or AC-001-01 gate rerun is
implied. Consult [local commands](local-development.md) before selecting a source.

**Owner acceptance recorded — 2026-10-18:** AC-001-01 is **DONE /
PASS_WITH_DOCUMENTED_FINDINGS** for integrated242e3d31192fb493938b09b398b4cd1ddd29a83b.
[Formal record and limitations](../implementation/ac-001-01-final-acceptance.md)
contains24 PASS/0 FAIL/1 UNVERIFIED, retained raw privacy scan FAIL and accepted
native wrapper deviation. The completion/pending statements below describe the
procedure and historical checkpoints, not a revocation of that scoped approval.
No command or safety requirement changes here. AC-001-04/05 remain independently
unaccepted; Feature001/D1-08/Day1 remain PARTIAL. Raw diagnostics are private and
not approved for publication; consult the record before reusing or sharing evidence.

This is a verification-only path. Existing PostgreSQL/foundation developer commands
remain unchanged. Do not use their settings, containers, volumes or database for
this procedure. The implementation branch's local smoke is not official AC-001-01
acceptance; publish/review/merge, verify develop CI, then use a NEW Git clone.
The previously blocked clone remains historical failure evidence.

## Preconditions and boundaries

Use approved Java21/Wrapper3.10.0, Node24.13.0/npm11.6.2, Python3.12+ (3.14 tested),
Docker Desktop Linux/amd64 engine and Compose. Existing machine tools/caches do
not make this a clean-machine or independent-developer experiment. AC-001-05 is
separate. Do not install/upgrade tools or change approved image/dependency pins.

Start in a clean terminal at the fresh clone root. The runner rejects inherited
PG_*, DEMO_*, SPRING_*, FOUNDATION_*, VERIFY_*, COMPOSE_*, DOCKER_*, VITE_* and JVM/
Node injection options. Do not change global variables to work around a rejection.
Review/remove only unintended process-scoped overrides, then invoke explicitly.
No developer .env is sourced: Compose uses a dedicated empty env file, controlled
environment, explicit config and project directory; Java loads classpath config only.

Before init, record clone SHA/status and prove no ignored outputs were copied.
Use a separately named clone on D, never copy node_modules/target/.demo/.env.

## Session initialization and real dry-run

```powershell
git rev-parse HEAD
git status --short
$verifySession = python -B infra/verification/manage.py init | ConvertFrom-Json
$verifyPlan = python -B infra/verification/manage.py dry-run --session $verifySession | ConvertFrom-Json
```

Check LASTEXITCODE after every command. init creates only synthetic private files
and baseline resource IDs under `.demo/verification/<session>/`. Existing settings
must match exactly; missing/mismatched files are rejected, never regenerated.
Windows directory ACL permits owner/SYSTEM/Administrators; POSIX directory700.
Do not print settings.json, password files, definitions hashes or child environment.

Dry-run invokes actual Compose config resolution, not container startup. It checks
exact service/project/resource/secret/mount identities, images, environment, no
external resources/privilege/socket mounts and loopback published port0. Each
session has a unique database, project, two volumes and network. No container_name
is configured; Compose names containers under that unique project.

Pins are read from approved existing Compose files and cross-checked with the
existing foundation pin source. Canonical repository@digest is the same approved
image bytes and resolves an untagged cache. Pull only these resolved identities
if absent; do not change source pins or use latest:

```powershell
$verifyDockerCommand = Get-Command docker -ErrorAction SilentlyContinue
$verifyDocker = if ($verifyDockerCommand) { $verifyDockerCommand.Source } else {
  Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe'
}
$verifyPlan.images.PSObject.Properties | ForEach-Object {
  & $verifyDocker pull --platform linux/amd64 $_.Value
  if ($LASTEXITCODE -ne 0) { throw 'Approved image pull failed; stop' }
}
```

## Build and owned infrastructure

```powershell
$verifyMavenRepo = Join-Path (Get-Location).Path '.demo/verification-build/maven-repository'
Push-Location app/backend
try {
  .\mvnw.cmd -B -ntp --settings ../../scripts/ci/maven-settings.xml "-Dmaven.repo.local=$verifyMavenRepo" -Ddebug=false -Dtrace=false verify
  if ($LASTEXITCODE -ne 0) { throw 'Backend verify failed; do not start services' }
} finally { Pop-Location }
python -B infra/verification/manage.py up --session $verifySession
if ($LASTEXITCODE -ne 0) { throw 'Owned startup failed; inspect session before cleanup/retry' }
python -B infra/verification/manage.py status --session $verifySession
if ($LASTEXITCODE -ne 0) { throw 'Owned stack validation failed' }
python -B infra/verification/manage.py check --session $verifySession
if ($LASTEXITCODE -ne 0) { throw 'Controlled checks failed; preserve private evidence' }
```

Capture private logs without streaming raw Maven/Surefire diagnostics into public
reports; use existing scripts/ci report/privacy guards. `up` never pulls images or
uses a fixed host port. Actual Docker inspect bindings must all be127.0.0.1/random
and match owned IDs. Redis is tmpfs/ephemeral; PostgreSQL and Rabbit persist in
separate new named volumes. Only Redis/Rabbit approved least-privilege probes are
configured; no business queues/cache functions or management UI.

check is a one-time EMPTY-database controlled scenario. It uses TCP password
authentication, one-shot migration profile for V1/V2/validate, schema/table owner
and runtime DDL denial checks, rerun/checksums and a technical persistence marker.
It starts real demo,postgres,foundation on loopback port0, verifies liveness/
readiness/Demo API/auth and approved active Rabbit probe, restarts only captured
PostgreSQL, refreshes bindings, reruns migration and boots a new backend using
current bindings. No Flyway clean/repair or migration editing. Repeating up/status
is safe; a new empty-database check requires a new session. Record exact gates
from checks.json; same-process recovery is tested only if Docker retained ports.

## Web and mobile gates for the later full acceptance run

SDK must be built before file-based web/mobile consumers. In packages/api-client:
`npm.cmd ci`, `npm.cmd run verify:c02`. Then web: `npm.cmd ci`, `npm.cmd run lint`,
`npm.cmd run typecheck`, `npm.cmd test`, `npm.cmd run build`. Mobile: `npm.cmd ci`,
`npm.cmd run lint`, `npm.cmd run typecheck`, `npm.cmd test -- --watch=false`,
`npm.cmd run build:android:bundle`. Use committed lockfiles and record each exit.
These commands are acceptance gates, not claims they ran in this implementation.

For an owned web/backend smoke, keep this process running in terminal1:

```powershell
python -B infra/verification/manage.py serve --session $verifySession
```

It prints only owned backend URL and the generated Vite config path. In terminal2,
at app/frontend/web, run the installed Vite CLI with `--config <printed-path>`.
The temporary config imports approved web config, points /api at this owned backend
and requests loopback port0. Record actual Vite URL and API/browser evidence. Do
not edit the developer proxy or use its default8080/5173 endpoints. Ctrl+C in
terminal1 stops only its captured Java child. Stop the owned Vite process too.

Android compile/bundle is not device runtime. AC-001-01 still needs the approved
native spike log; record the actual SDK/AVD/build/runtime scope and permissions
before device work. No AVD reset, pre-existing app/data deletion or automatic SDK
installation. Metro/Hermes alone cannot substitute for an APK/device gate.

## Ownership cleanup and failure handling

### Windows blockers — current safety checkpoint, 2026-10-18

Read [the current remediation/recovery report](../implementation/ac-001-01-windows-remediation.md)
before another runtime session. The original failed-session resources have now been
removed by exact owner-authorized recovery; do not repeat that cleanup. Historical
failed-clone evidence remains. Resource Saver was manually disabled/applied by the
owner; do not automate global setting changes. The fresh lifecycle session is also
cleaned. Engine reachability alone never authorizes removal or another stack:
re-prove captured ownership/consumers/attachments and obtain task-specific approval.
Never rewrite the baseline to hide bridge drift or use global cleanup/restart.

Verification Rabbit health/permission commands run as OS user rabbitmq through
gosu; official entrypoint bootstrap is unchanged. Cookie must remain session-private,
owned by that non-root identity, mode400/600 and readable. Inspect metadata only,
never print/copy cookie content. A root-owned cookie in a failed volume is a STOP,
not permission to chmod/chown/delete it. Corrected runtime cold-start/restart/
recreate/authentication/cleanup now PASS locally in session
`a677c73052e94396b1cd8501127335c8`; cookie999:999/0400, actual server999, no EACCES.
Its exact IDs and sanitized evidence are in the report. This does not complete
AC-001-01 or authorize reuse of the cleaned session.

For controlled lifecycle cleanup, check current Engine/bridge, exact owned IDs/
labels/mounts, all running/stopped consumers and network references before EACH
destructive operation, and compare foreign metadata after each. The runtime proof
used explicit bounded stop then exact-ID rm without force/volume flags, followed
by unused owned-volume and empty owned-network removal. Only after all absent was
normal harness down repeated to prove idempotence. No source guard was weakened.

After fixes are reviewed/published, a NEW Windows checkout gets generated SDK LF
from package-local attributes even with autocrlf=true. Existing checkouts are not
automatically rewritten. First prove no user edits; prefer the mandated new clone.
Do not relax byte comparison or globally change Git. The isolated checkout-policy
regression is `python -m unittest discover -s packages/api-client/tests -p test*.py`.

If Node reports UV_HANDLE_CLOSING, preserve exact command/exit/hook stack and private
logs. Current retries passed but cause is OPEN; no Node upgrade/stdin workaround
is approved. A native timeout without a new APK is not native acceptance. Diagnose
dependency resolution/task progress/resource pressure before any longer build;
installed development-client rendering alone does not satisfy the new-APK gate.

```powershell
python -B infra/verification/manage.py down --session $verifySession
python -B infra/verification/manage.py down --session $verifySession
git status --short
```

down validates private settings, daemon, all current labels/names/mounts, captured
IDs, volume consumers and network attachments BEFORE removal. It removes only
captured IDs/names, never compose down/prune/prefix cleanup. Repeat cleanup is
idempotent. A cleaned session cannot start again. Keep private evidence files.

Interrupted startup remains phase starting with captured owned resources. Inspect
and use owned cleanup before a new session; never reuse failed partial data as a
cold-start success. If ownership cannot be proved, STOP without removals and
report exact IDs. Do not weaken ownership checks, rewrite baseline or remove
foreign resources to satisfy a comparison.

Prior correlated idle stop/wake/default-bridge changes were traced to Resource
Saver. The current proof retained bridge20f26ad4 throughout; this does not waive
any future drift. Any missing baseline resource blocks cleanup before removals.
Record the delta and request/review a precise recovery proof; do not run broad
cleanup. A later successful session does not erase this earlier finding.

## Acceptance evidence

Record Git SHA, clone provenance, tools, exact commands/exits/timings, resolved
Compose identities, authenticated connections, Flyway checksums/roles, live health,
SDK/web/mobile results, actual UI/device scope, owned process/resource IDs,
restart/persistence, bounded credential scans and before/after inventory equality.
Publish only sanitized summaries. Required order: approved commit/PR -> required
CI -> owner merge -> actual develop CI -> NEW clean clone -> complete procedure.
AC-001-01 remains UNVERIFIED until all mandatory gates pass there. Feature001,
D1-08 and Day1 remain PARTIAL; this runbook does not waive AC-001-04/05.
