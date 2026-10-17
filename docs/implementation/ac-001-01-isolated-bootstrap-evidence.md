# AC-001-01 isolated bootstrap implementation evidence

Execution 2026-10-17, Asia/Saigon. Implementation LOCAL_DONE / READY_FOR_REVIEW.
Official AC-001-01 remains UNVERIFIED pending integrated-baseline fresh clone.
Feature001/D1-08/Day1 remain PARTIAL. No commit/push/PR/merge is authorized here.

## Baseline and original blocker

Clean develop/local origin matched `af0c02096f2560335e187108ea06c2d472bc1842`;
created `feature/ac-001-01-isolated-bootstrap`. Read AGENTS/DoR, Feature001
spec/design/tasks, exact AC-001-01, PostgreSQL/foundation/local runbooks,
accepted ADRs, Task A/B/C/D evidence and original blocked preflight.

Original clone `D:\TLCN\verification\ac-001-01-20261017-7bab6133\repo` and ignored
`.demo/ac-001-01-preflight/` snapshots remain intact. Developer Compose explicitly
names developer container/volume; bootstrap assumes elogistics_dev and foundation
launcher reads old private settings. Safe InitializeOnly probe returned1 at its
collision guard. No component evidence was substituted for a full cold-start.

## Isolation and effective configuration

[ADR-0007](../architecture/decisions/ADR-0007-isolated-verification-bootstrap.md),
[commands/runbook](../runbooks/clean-clone-verification.md).
Standalone Compose avoids all developer inheritance. Each session owns a unique
project/database/roles, two named volumes, one network and generated container
names. No existing DB/data/credentials are read or copied. Private generated
settings/secret bytes are checked on reuse, never silently regenerated/rotated.
Controlled environment/paths reject redirection; explicit empty env-file prevents
developer .env shadowing. Java classpath config and generated temporary Vite proxy
preserve existing developer/application files.

Real Compose config checks validate services, labels/names, exact images, secret
paths/mappings, mounts/read-only flags, environment, no privilege/host networking,
loopback published0 and Redis tmpfs. Runtime checks re-read owned IDs, mounts,
network attachments, volume consumers and actual random bindings. Only local
Docker daemon access; no socket mounts/privileged containers. Settings and logs
are private, public results contain IDs/statuses rather than secrets/raw output.

Source-of-truth images (unchanged official linux/amd64 identities):

| Source version | Canonical runtime identity |
|---|---|
| PostgreSQL18.6-bookworm | postgres@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c |
| Redis8.4.7-trixie | redis@sha256:43afe4a9a43080e64dfa29699eef903fc9d54991fe8b03d57237c05bed2a396c |
| RabbitMQ4.3.6 | rabbitmq@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620 |

Pins are read from existing Compose, cross-checked with the foundation pin source;
canonical refs fix local tagless-cache lookup without changing image bytes.
Provisioning SQL is verification-only; Flyway V1/V2 remain byte-identical.

## Controlled runtime, not official clean-clone acceptance

Final session `a06a8cdae0354bd08e3bcc6f6295b159`:
project elogistics-verify-a06a8cdae0354bd08e3bcc6f6295b159;
database elogistics_verify_a06a8cdae0354bd08e3bcc6f6295b159.
Initial random loopback bindings: PostgreSQL32774, Redis32773, Rabbit32772.
Ports are observations, not configuration to copy. PostgreSQL can receive a new
random port on restart; the runner re-inspects and boots a new backend with it.

`python -B infra/verification/manage.py check --session <session>` PASS9 gates,
63.407s: TCP PostgreSQL authentication/principals; Redis authenticated PING and
write denial; anonymous Redis denial/Rabbit non-admin and denied vhost patterns;
empty V1/V2,10 owned schemas/6 tables/owner NOLOGIN/no elevated roles and6 runtime
DDL/owner/history denials; Flyway validate/rerun/checksums; full profile with real
active AMQP readiness/liveness/Demo auth API; owned PostgreSQL restart/marker
persistence; post-restart migration; new backend using verified live bindings.

Owned Vite proxy/auth API smoke PASS at backend127.0.0.1:55866 and web:55881,
using the generated verification config and existing local dependencies. Requests
through Vite reached the owned backend, admin login/me succeeded; private logs
passed bounded credential/JWT checks and captured child processes were stopped.
This is HTTP proxy smoke, not browser UI, clean install or native runtime evidence.

Final `down` PASS twice: baselinePreserved=true, inventoryEqualsBaseline=true.
Only captured3 containers/2 volumes/1 network removed. Private evidence retained
under `.demo/verification/<session>/`. No developer resources were removed/restarted.

## Earlier failures and operational finding — retained

Initial real dry-run rejected Compose's generated secret-name metadata; validator
was corrected to require the exact generated name AND exact path. No path/identity
check was disabled. The final dry-run passes the stricter service/mount checks.

First implementation session e356bd14782c410888f3839d4d21793d passed7 runtime
gates, but default Docker bridge ID changed from3caf20845aced14f59122a8ac48e0b8959f847c4ae26371550b6e92c08df8be1
to383892950a5493dedf5027e1086ff939505643ddff6bc978c68cc73e9aec632b.
Automatic cleanup correctly BLOCKED before removals; snapshot was not rewritten.
Developer containers/volumes/custom network remained present. Initiating cause is
UNVERIFIED; no daemon restart/default-network removal command was issued.

An independent recovery re-proved exact owned labels/IDs/mounts/consumers/network
attachments, then removed only captured3 containers/2 volumes/1 session network.
Current foreign inventory was preserved exactly. This manual recovery is disclosed
and is NOT an automatic-cleanup PASS or proof of original full inventory equality.
`manual-recovery.json` preserves the discrepancy. Subsequent final session had
normal/idempotent cleanup and exact equality; the earlier bridge finding remains OPEN.

## Regression results

New isolation tests:40 PASS. Existing foundation bootstrap safety:7 PASS.
CI report/privacy guard tests:25 PASS. Architecture Wrapper clean verify:23 PASS,
0 skips, report guard PASS. H2 clean verify:48 PASS/17 named skips,0 failures/errors;
strict class/method report guard PASS,25.203s. PostgreSQL clean verify:73 PASS/5
expected foundation skips,0 failures/errors,78 discovered,62.344s, exact reports
PASS. Includes Task A4 JDBC safety + Task C7 database/6 policy + existing8 PG
tests. Task C generated credential and owned cleanup audits PASS. No reports,
assertions or test selection were weakened. Java21/Boot4.1.1/Wrapper3.10.0 unchanged.

Actual regression command flags: Wrapper `-o -B -ntp --settings scripts/ci/maven-settings.xml
-Dmaven.repo.local=<existing-resolved-cache> -Ddebug=false -Dtrace=false clean verify`;
architecture selects both existing architecture classes; PG additionally enables
`-Ddemo.test.database=postgres -Ptestcontainers-postgres`. Approved existing CI
provisioner supplied a fresh tmpfs PG test container/two synthetic test databases,
separate from the new verification stack. Both H2/PG use unchanged exact report
guard. Evidence under `.demo/ac-001-01-implementation/regressions-*`.

Task B original5/5 real foundation JUnit tests and16 runtime/fault/persistence
gates PASS; credential/privacy, owned cleanup, port release and all foreign
containers/volumes/networks preserved. Six individual pause/stop/recovery cases
kept the same backend; Redis ephemeral and Rabbit metadata restart/recreate
checks PASS. Evidence `.demo/d1-08-foundation/run-8a87cb3c571d/`.
Invocation used the unchanged harness assertions with two explicitly disclosed
process-local adapters: same approved digest via canonical ref in a temporary
image-only Compose override/inspect translation, and existing resolved Maven
cache location. This is NOT a claim the stock legacy command works unchanged
with tagless image-cache references. No developer/CI source or image bytes changed.

Final real CLI dry-run after validator hardening PASS in session
117215dd61044e97b1f3ef8edb8acb04; no services created, closure/idempotence PASS.
Python AST, Bash syntax, UTF-8/links and git diff checks PASS. ShellCheck binary
is unavailable, so ShellCheck remains UNVERIFIED rather than claimed PASS.

| Actual command family | Exit / result |
|---|---|
| python -B -m unittest discover -s infra/verification -p test_*.py | 0;40 PASS,0 skips |
| python -B -m unittest discover -s infra/foundation -p test_*.py | 0;7 PASS |
| python -B -m unittest discover -s scripts/ci -p test_*.py | 0;25 PASS |
| manage.py init / dry-run / up / status on final runtime session | 0; own synthetic settings/resolved config/live bindings |
| manage.py check on a06a8cdae0354bd08e3bcc6f6295b159 | 0;9 gates/63.407s |
| manage.py down on final runtime session, twice | 0;exact baseline equality both times |
| First-session normal down | 1;bridge drift guard blocked BEFORE removal |
| .demo/ac-001-01-implementation/web-smoke.py | 0;owned Vite proxy/auth smoke; existing local dependencies |
| .demo/ac-001-01-implementation/regressions.py postgres | 0;PG73/5, guards/privacy/owned cleanup PASS |
| .demo/ac-001-01-implementation/foundation-regression.py | 0;unchanged Task B5/5+16 gates with disclosed same-byte/cache adapters |

Final bounded source/runtime-log privacy audit checked36 generated credential/hash
values; no plaintext credentials/JWT/private keys in changed source/docs or runtime
diagnostics. Credential files themselves are intentional private inputs, excluded
from public log reports. All3 verification sessions have zero remaining owned
resources; old blocked clone unchanged; index empty and implementation uncommitted.
Exact changed-file/hash manifest is `.demo/ac-001-01-implementation/final-summary.json`.
Windows Docker Desktop is the tested target. Linux/POSIX runtime compatibility,
including secret mount ownership/permissions, is UNVERIFIED; no Linux-bootstrap
or new remote CI coverage is claimed by existing backend workflow evidence.

Local regression driver initially hit a directory-name collision after H2 passed,
before PG started. That evidence-tooling failure cleaned its owned provisioning
resources and preserved inventory. Driver was corrected; only missing PG gate
was rerun. This was not a backend/test failure or permission bypass.

## Acceptance boundary and remaining gates

Local implementation smoke is not AC-001-01 PASS. Required future sequence:
owner review -> authorized commit/push/implementation PR -> required checks ->
owner merge -> actual develop CI -> NEW clean clone -> full ordered cold-start,
SDK generation/build, web build/browser, mobile/native spike and cleanup evidence.
Do not use this implementation working tree or the old blocked clone as the final
acceptance environment. AC-001-05 requires another developer and remains separate.

ShellCheck availability, JVM-kill cleanup and Docker bridge investigation remain
explicit limitations; security/dependency findings are unchanged. No broad Docker
cleanup, developer volume/data/cache deletion, migration/API/SDK/dependency change
or full-feature closure. Existing developer source/configuration remain unchanged.
