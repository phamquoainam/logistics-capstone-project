# AC-001-01 — Windows cold-start blocker remediation

Date: 2026-10-18. Verdict: **PARTIAL**. AC-001-01: **BLOCKED**.
Feature001, D1-08 and Day1 remain PARTIAL; AC-001-05 is not complete.
This report supplements, and does not replace, the historical failed acceptance.

## Latest checkpoint — RabbitMQ lifecycle locally accepted, 2026-10-18

RabbitMQ remediation: **LOCAL_DONE / READY_FOR_REVIEW**. Overall Windows
cold-start remediation remains PARTIAL; **AC-001-01 remains BLOCKED**.
The sections after this checkpoint retain the earlier failure/recovery snapshots.

Owner-authorized recovery has completed; do not repeat it. The earlier PostgreSQL
container and the five subsequently approved resources are absent. Private recovery
proof: `.demo/ac-001-01-five-resource-recovery-20261018/recovery-result.json`.
Resource Saver was disabled/applied manually by the owner. Prior Docker idle
stop/wake/bridge changes correlate with Resource Saver lifecycle logs; the prior
OPEN cause is now explained for those correlated transitions. The exact historical
root-owned Rabbit cookie creator remains unproven; competing root CLI is a
hypothesis, not a demonstrated creator.

The current branch/HEAD remain `feature/ac-001-01-windows-cold-start-fixes` /
`82c2ddcf21363b95efeff49f62cd60fa11e03c0a`; index empty. Preflight matched all12
remediation file hashes plus4696 historical files. Foreign inventory matched the
completed recovery:4 containers/5 volumes/4 networks, including mounts, ownership,
creation metadata and network references. Bind sources were compared with the
existing harness's Windows path normalization and mounts sorted by destination;
Docker inspect path spelling/list order differences were not resource mutations.
No resource was created during those initial conservative preflight stops.

Exactly one fresh runtime session was created:
`a677c73052e94396b1cd8501127335c8`, project
`elogistics-verify-a677c73052e94396b1cd8501127335c8`.
Private evidence: `.demo/ac-001-01-rabbit-lifecycle-20261017T201621Z/`, including
`result.json`, `owned-cold.json`, `owned-recreated.json`, `cleanup-targets.json`,
`foreign-before.json`, `foreign-after.json` and `final-audit.json`.
The directory timestamp is UTC; the local date is2026-10-18 (UTC+7).

| Owned resource | Exact identity; all absent after cleanup |
|---|---|
| PostgreSQL container | `1731e5edb941527b2998cad71d78e4c72ffdecfe838ff823b91df2ede80afabf` |
| Redis container | `d84f390ad671925eaa7d1fd06afc370e841a1b8f135efc8ab13ad60fd3c0c040` |
| Rabbit cold/restart container | `429e379b3462150fa95edcc04e6dcb58098c4b9256843bcbfead502fc5a7d6e0` |
| Rabbit recreated container | `18ec87ae11578c28270e4b350eb2d7185a218be16ddc068d66ede48f63433aec` |
| PostgreSQL volume | `elogistics-verify-a677c73052e94396b1cd8501127335c8-pg` |
| Rabbit volume | `elogistics-verify-a677c73052e94396b1cd8501127335c8-rabbit` |
| Network | `cdce2e036c0d0e455c92655db41653ddc0fee2dbd38e4f023fa3391db1bf3609` |

All three images were the cached approved immutable digests; no pull or pin change.
Rabbit image: `rabbitmq@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620`.
The existing `Session.init/dry_run/up/status/discover` performed creation and
effective/actual configuration checks; a private stdlib-only driver recorded real
AMQP0-9-1 results. No dependency was installed or source safety guard changed.

| Actual gate | Result and evidence |
|---|---|
| Preflight | PASS; context desktop-linux, Resource Saver=false; live system-df/fresh Engine time, cached pins and available RAM/D disk checked |
| Fresh-volume cold-start | PASS; Compose startup19.843s, all three containers healthy; Rabbit exact approved image |
| Cookie/server/CLI identity | PASS at cold/restart/recreate: HOME `/var/lib/rabbitmq`, cookie owner999:999/mode0400/readable; actual beam.smp UID/GID999; diagnostics run through gosu rabbitmq, no EACCES |
| Real AMQP authentication/permissions | PASS at cold/restart/recreate: dedicated foundation_probe user, tags[], dedicated `/elogistics-foundation`; authenticated channel, queue.declare403, wrong-password403, wrong-vhost530; configure/read/write all `(?!)`, no queues/messages |
| Same-container restart | PASS,31.642s gate; same captured ID, volume/cookie and test-created synthetic vhost marker preserved; ports re-inspected |
| Recreate | PASS,49.856s gate; old captured ID stopped/removed, absence discovered before new ID capture; same persistent volume/cookie/marker, no auth/permissions change |
| PG/Redis isolation | PASS before and after recreation: same PG/Redis IDs, runtime/migrator TCP identities, Redis authenticated PING/write denial/anonymous denial; only127.0.0.1 random mappings |
| Controlled cleanup | PASS; exact captured resources stopped then removed, no force/volume flags; ownership/consumers/attachments/live Engine/foreign equality checked before every destructive command |
| Repeated cleanup | PASS; existing Session.down invoked twice after resources absent, no further deletion and exact inventory equality |

Rabbit host mapping was127.0.0.1:32769 at cold-start/restart, then32772 after
recreate; PostgreSQL32768/Redis32770 stayed unchanged. No management UI exposed.
The synthetic extra vhost existed only as a persistence fixture in the owned
Rabbit volume and disappeared with cleanup. No business queue/message was created.
Cookie fingerprints were compared in memory only; no cookie value/hash published.
The official entrypoint still performs its normal bootstrap; the server is non-root.

An initial negative-auth probe received EOF because it had not advertised AMQP
`authentication_failure_close`. Server logs identified invalid credentials, while
the positive connection and unauthorized-queue denial had already worked. The
private probe now advertises that capability and receives403; original diagnostics
are retained. Its initial transient-queue attempt also emitted a deprecation log;
the final durable synthetic declaration is still denied403. No server configuration,
assertion, authentication, permission or health requirement was relaxed.

All12 lifecycle mutation commands exited0;31 stability checkpoints retained the
same Engine/backend/WSL processes and default bridge
`20f26ad4016cff06feb52201d45422175aa91020b7da6cc41093a9d813682c08`.
Seven real runtime gates plus preflight PASS (8/8), no skips. Foreign before/after
metadata matched4/5/4 exactly; no developer SQL/data operation was issued. Metadata
equality does not prove database content integrity. No new owned resource remains.

Current focused commands: `python -m unittest discover -s infra/verification -p
test_manage.py`43 PASS; analogous infra/foundation7, scripts/ci
test_verify_reports.py25, packages/api-client/tests test_checkout_policy.py2:
**77 tests PASS/0 FAIL/0 SKIP**. Python AST3 and Git Bash `bash -n
infra/verification/bootstrap.sh` exited0. ShellCheck remains unavailable/UNVERIFIED.
Final UTF-8/link/diff, source preservation and credential/token/private-key scans
are recorded in `final-audit.json`; only sanitized metadata is published here.

Remaining gates: Node/libuv cause OPEN/NOT REPRODUCED (no runtime fix claimed),
new native APK acceptance still BLOCKED after the historical timeout, fixes not
published/hosted-tested, full NEW-clone acceptance not rerun, AC-001-05 independent
developer acceptance separate. Current SDK/web PASS evidence below is retained
from the earlier remediation run, not rerun or upgraded to full acceptance here.
Next step is review of this12-file checkpoint, then separately authorized
commit/push/PR and integration CI. Agree the native-build diagnostic/budget plan
before a new full acceptance attempt. No commit, push, PR, merge or native build
was performed. Feature001/D1-08/Day1 remain PARTIAL; original REQ/AC unchanged.

## Final publication review — 2026-10-18, before commit

Explicit owner authorization now covers this exact12-file checkpoint, one normal
commit, normal feature-branch push and one PR to develop; no merge is authorized.
Remote develop still matched baseline82c2ddc; no existing feature PR/remote branch
was found at preflight. No business/API/OpenAPI/migration/dependency/image/CI or
architecture-enforcement changes were introduced. The AMQP capability adjustment
exists only in the ignored runtime driver, not in tracked source or tests.

Final regression commands were rerun with exit0: verification43, foundation7,
report/privacy25 and LF checkout2 (77/0/0); `npm.cmd run verify:c02` (contract41,
SDK44, exact11-operation inventory, two independent generations matching all16
source files, typecheck/build); web lint/typecheck/test/build (24 tests); Python
AST3 and Bash syntax. No libuv assertion reproduced; this remains OPEN, not fixed.
No clean install, fresh APK, full acceptance or repeated Docker lifecycle is
claimed. ShellCheck, JVM-kill cleanup and Linux/POSIX bootstrap runtime remain
UNVERIFIED. Existing lifecycle8/8,31 stability checkpoints and4696 historical hashes
were revalidated as prior evidence, not presented as a newly rerun Docker test.

The generated SDK status anomaly was stale index stat/conversion metadata after
the earlier CRLF checkout: e.g. client.gen.ts cached size871 versus actual LF855.
All16 HEAD/index/raw blob IDs matched; `ls-files --eol` reported i/lf,w/lf and
text/eol=lf. Ordinary refresh did not resolve status. A private index-copy experiment
proved reinserting each SAME blob ID followed by refresh clears the metadata-only
anomaly with an empty staged diff. That exact metadata reconciliation was applied;
all repository index mode/OID/stage entries and working bytes stayed identical.
Generated paths are now clean, with no generated output staged. No reset, generated
rewrite, global Git configuration change or determinism relaxation was used.

Private publication evidence is under `.demo/ac-001-01-windows-publish/`, including
EOL review, private index experiments, captured regression exits and staged/remote
review records. Review SHA/run/check results in the later PR report; a pre-commit
document cannot attest to future hosted execution. Hosted backend CI does not run
the43 verification safety tests, checkout-policy suite, Rabbit lifecycle or SDK/web
gates. AC-001-01 stays BLOCKED; Feature001/D1-08/Day1 remain PARTIAL.

## Historical remediation snapshot — before controlled recovery

## Baseline and safety

Original repository: `D:\TLCN\e-logistic`. Clean develop/local origin baseline:
`82c2ddcf21363b95efeff49f62cd60fa11e03c0a`; live remote was checked before branching.
Current branch: `feature/ac-001-01-windows-cold-start-fixes`, same HEAD.
No commit, stage, push, PR, merge, reset, stash or global configuration changes.
The failed clone `D:\TLCN\verification\ac-001-01-fresh-20261017-50688775\repo`
and its historical evidence are retained. New private diagnostics are under
`.demo/ac-001-01-windows-fixes/`; no old ignored files or secrets were copied into source.

PR15 integration was already verified: merge parents
`af0c02096f2560335e187108ea06c2d472bc1842` and
`1d2bc54041ee4bb39dd31b0f60ea000326ba30bf`, merge tree equals feature tree.
[Develop run 39044755713](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39044755713)
is baseline evidence, **not hosted verification of these uncommitted fixes**.

## Gate 0 — actual Docker state

Engine was reachable during this review, without an agent-issued restart.
CLI29.6.2 / Compose5.3.1; context `desktop-linux`, local named pipe
`npipe:////./pipe/dockerDesktopLinuxEngine`; daemon ID
`358b01ea-4f0a-4953-980b-bce1b3b988da`.
Session: `ccc7aa20421043e083b506d67b32b3a9`.
Project: `elogistics-verify-ccc7aa20421043e083b506d67b32b3a9`.

| Actual retained resource | Identity | Current evidence |
|---|---|---|
| PostgreSQL container | `86e354cfb6f4d0d05ac7758ccc7181a787866beab95257b0ec940d860add3c49` | Stopped, exit255 |
| Redis container | `b97f100b2765c6e650d8896349549a52ef66994e150fb1a747a2316b5de45240` | Stopped, exit255 |
| RabbitMQ container | `d54a5c7e220132fae9527d8d23471efb4398a652be573a36df8a8ef9cada48e5` | Stopped, exit1 |
| PostgreSQL volume | `elogistics-verify-ccc7aa20421043e083b506d67b32b3a9-pg` | Local driver; only captured PostgreSQL consumer |
| RabbitMQ volume | `elogistics-verify-ccc7aa20421043e083b506d67b32b3a9-rabbit` | Local driver; only captured Rabbit consumer |
| Session network | `30c66685a44f2a4d103a227c249e536d0d670e981b7ff4b17efa8276551b40bd` | Correct name/labels; no current attachments |

Names, session/Compose labels, images, mounts, volume options/consumers and network
attachments were inspected and matched captured ownership. Four baseline foreign
containers and five baseline volumes still exist. Original default bridge
`383892950a5493dedf5027e1086ff939505643ddff6bc978c68cc73e9aec632b`
is missing. Do not rewrite the original baseline or count this as exact equality.
Current bridge is
`deb417ccac9dccf819d555822ed9e0f71e986d2f08e38451dd56d9aa784445fa`,
created `2026-10-17T18:29:45.778234314Z`; this later snapshot is distinct from
earlier bridge snapshots and is not attributed to an agent restart.
Daemon/bridge lifecycle correlation does not prove who recreated the bridge.
Read-only event retrieval can time out; absence of retained events is not proof
that no restart occurred. Exact recreation cause remains UNVERIFIED.

### Proposed recovery, NOT executed

1. Obtain owner authorization for removal of precisely the retained resources above.
2. Immediately recheck local context, daemon, exact IDs/names, both ownership labels,
   approved images, mounts, consumers and network attachments against original records.
   STOP on mismatch, foreign attachment/consumer, or daemon unavailability.
3. After authorization, remove only those three captured container IDs, then the
   two exact owned volume names, then that network ID. This destroys failed-session
   data; it does not preserve the failed database. Retain all private evidence.
4. Compare foreign inventory with the pre-recovery snapshot. Disclose bridge drift
   separately; never remove/recreate the default bridge to force equality.
5. Review the recovery result before opening a new runtime session. No prune,
   unscoped Compose down, daemon restart or developer-resource change.

No removals, restarts, container starts or new Docker resources were performed.
Only Compose config rendering was run. Runtime permission remains pending.

## RabbitMQ cookie diagnosis and minimal correction

Exact unchanged image:
`rabbitmq@sha256:25736a5d515fe5dcf4c7eec2125b09a0e2043cc934ac0a7aca198c1058564620`.
Stopped-container filesystem tar **headers only** prove cookie owner0:0, mode0400,
size20 at `/var/lib/rabbitmq/.erlang.cookie`; payload was not extracted or logged.
Parent directory is999:999, mode01777. Image passwd maps `rabbitmq` to999:999,
HOME `/var/lib/rabbitmq`. Named-volume storage, not a Windows cookie bind mount.

The exact image entrypoint starts as root, chowns non-rabbitmq-owned data at server
bootstrap, then re-execs through `gosu rabbitmq`. The server therefore cannot read
the observed root0400 cookie: this EACCES mechanism is confirmed. Cookie provenance
before startup and its exact creator are UNVERIFIED. The old root health CLI can
create a competing root cookie after entrypoint ownership traversal; this is a
supported race hypothesis, not a proven attribution for this historical file.
Health attempts occurred while the server was still initializing.

Verification Compose now executes all three existing diagnostics through
`CMD gosu rabbitmq sh -eu -c ...`. Permission-inspection `rabbitmqctl` calls use
the same OS user. Effective-config policy rejects root/disabled/incomplete probes.
The official server entrypoint, volume persistence, application probe user/vhost,
approved digest and access permissions remain unchanged. No new cookie secret,
chmod workaround, permanent root service or developer Compose change.

The runtime check now validates HOME, readability and cookie owner/group/mode
(400 or600) under the non-root service identity; only metadata is returned.
New unit regressions reject root/world-readable metadata and verify the command
for simulated start/restart/recreate identities. They do **not** establish real
cold-start/persistence. Real new-session startup, authenticated AMQP, same-cookie
restart/recreate, PostgreSQL/Redis preservation and owned idempotent cleanup remain
BLOCKED until controlled recovery is authorized and completed.

## SDK newline diagnosis and policy

All16 Git blobs were LF, Windows working checkouts CRLF, generator outputs LF.
System Git `core.autocrlf=true`; affected files had no eol attribute. Only newline
bytes differed. The package-local `.gitattributes` now sets
`src/generated/**/*.ts text eol=lf`; handwritten sources keep their existing policy.
No generator serialization or strict comparison was relaxed. The original working
copies were normalized only after verifying they equaled HEAD after CRLF-to-LF;
all16 are byte-identical to their original Git blobs. No generated content change.

Two actual private Git checkout-index tests use all16 real Git blobs and isolated
Git config with autocrlf=true/input. Before policy: Windows case FAIL; after policy:
2/2 PASS, including an unrelated handwritten-file control. Real repository index
and global settings are untouched. These tests ran on Windows; the input/LF case
checks LF compatibility but is not a Linux-host runtime claim.

After review/publish, use a NEW clone for acceptance. Existing CRLF working copies
need a separately reviewed refresh or byte-exact newline conversion after checking
there are no user edits; attributes alone do not rewrite existing working files.
Never use reset or mass normalization as a default repair.

## Node/libuv investigation

Approved runtime unchanged: Node24.13.0 x64, npm11.6.2, libuv1.51.0;
`C:\nvm4w\nodejs\node.exe`, SHA256
`d14ba95cdce1ef7dc9ad3ac74949ca5db38b27378ee30f30a23cf26f9e875a11`.
Windows11 Home Single Language10.0.26300/build26300. No alternate installed
runtime was established. Historical assertion:
`Assertion failed: !(handle->flags & UV_HANDLE_CLOSING), file src\win\async.c, line 76`.

Historical `npm.cmd run test:sdk` reported44 failed registrations,0 passed. Repeated
stacks point to the shared before-hook (`sdk.test.mjs:13`, `loadContract()`), which
spawns Redocly, rather than44 independent SDK behavior failures. The historical
nonzero result remains a failure; no successful retry erases it. Retained command
`sdk-components.json` records exit1; no exit status was suppressed. This is not
evidence that44 independent assertion bodies actually executed.

This turn: synthetic immediate exit5/5, pending crypto-worker exit10/10, Redocly
version5/5, actual contract bundle via parent spawnSync5/5 with default stdin and
5/5 with ignored stdin, local HTTP fetch followed by process.exit10/10: all exit0,
no assertion (40 attempts). Redocly key installed artifacts match the failed clone.
Full `npm.cmd run verify:c02` then exited0 in24.512s: C-01 tests41 PASS, SDK44 PASS,
strict16-file regeneration, typecheck and build PASS. Existing subprocess code
was unchanged. This is current PASS evidence, **not a libuv root-cause fix**.

[Node issue56645](https://github.com/nodejs/node/issues/56645) and
[Node issue64322](https://github.com/nodejs/node/issues/64322) document similar
Windows async-close assertions, including a Redocly report. The
[pinned Node24.13.0 libuv source](https://raw.githubusercontent.com/nodejs/node/v24.13.0/deps/uv/src/win/async.c)
locates the assertion in uv_async_send. These support a runtime hypothesis, not
proof of identical causality. Finding remains OPEN / NOT REPRODUCED this turn.
No stdin workaround, arbitrary delay, baseline change or test skip was introduced.

## Native APK timeout assessment

Historical Gradle invocation: `android/gradlew.bat -p android :app:assembleDebug
--no-daemon --console=plain -PreactNativeArchitectures=x86_64 --max-workers=1`,
with separate fresh GRADLE_USER_HOME, temp onD and existing SDK onE. Budget1500s,
observed1515.8s; no BUILD SUCCESS/FAILURE, no new APK. Last task:
`expo-modules-core:compileDebugKotlin` — timeout during build, before installation.
Gradle9.3.1 distribution downloaded; dependency/plugin resolution also consumed
time. One sampled Java thread was doing HTTP dependency reads; no proven deadlock
or OOM.16GB host had about2.1GB free during emulator/npm concurrency; daemon about
1.07GB. Resource pressure and cold cache are hypotheses, not confirmed root causes.
Repository HTTP probes returned200 but do not prove Gradle's complete network path.
Java21/SDK/API37 prerequisites were present; no version change is justified.

Previously installed Pixel7 development client rendered fresh Metro JavaScript.
That does not prove a newly built APK. No long build was repeated this turn.
CNG had modified two package scripts; original own changes were restored and the
side-effect patch retained. A future build must guard these changes explicitly.

Smallest separate plan: record clean-clone SHA and cold-cache provenance; preflight
required dependency access with Gradle diagnostic logs; build without emulator/npm
contention, observe task/CPU/memory progress at bounded checkpoints, agree an explicit
budget based on measured progress before a longer attempt. Preserve diagnostics on
timeout. Only after APK exists: hash/signature, install captured package on Pixel7,
launch/render/Metro/crash observation. No version changes, AVD wipe or cache deletion.

## Verification and remaining acceptance

| Actual command / gate | Result |
|---|---|
| `python -m unittest discover -s infra/verification -p test_manage.py` | 43 PASS, exit0; new controls demonstrated RED before implementation |
| `python -m unittest discover -s infra/foundation -p test*.py` | 7 PASS, exit0 |
| `python -m unittest discover -s scripts/ci -p test*.py` | 25 PASS, exit0 |
| `python -m unittest discover -s packages/api-client/tests -p test*.py` | 2 PASS, exit0; actual isolated Git conversion |
| `npm.cmd run verify:c02` in packages/api-client | PASS, exit0;41+44 tests, strict generation/typecheck/build |
| Web `npm.cmd run lint`, `run typecheck`, `test`, `run build` | All exit0;24 tests PASS; existing installed dependencies, not a fresh install |
| `docker compose ... config --format json` + effective policy | PASS, exit0; temporary synthetic files, zero Docker resources created |
| Python AST / Git Bash `bash -n infra/verification/bootstrap.sh` / UTF-8 / relative links / `git diff --check` | PASS; no shell changes; ShellCheck unavailable, UNVERIFIED |
| Bounded source scan / historical preservation | PASS;11 known credential values plus JWT/private-key patterns absent;5 original snapshot hashes unchanged |
| Real corrected Rabbit startup/restart/recreate/cleanup | BLOCKED; owner recovery authorization pending |
| New native APK | BLOCKED acceptance; historical timeout, no rerun |
| Linux-host / remote fixes CI | UNVERIFIED; no publication authorized |

Changes are verification-only Python/Compose, two narrow SDK policy/tests and the
linked documentation/status additions. No business code, dependency manifests,
API/SDK Git blob content, image pins, Flyway, CI/ruleset or original REQ/AC change.
Cookie values, credentials and raw logs remain private; only sanitized evidence
is suitable for review. Original historical snapshot hashes must remain identical.

### Exact intended content-change inventory (12 files)

- `infra/verification/compose.yaml`
- `infra/verification/manage.py`
- `infra/verification/checks.py`
- `infra/verification/test_manage.py`
- `packages/api-client/.gitattributes` (new)
- `packages/api-client/tests/test_checkout_policy.py` (new)
- `docs/implementation/ac-001-01-windows-remediation.md` (new)
- `docs/implementation/day-1-plan.md`
- `docs/quality/traceability.md`
- `docs/runbooks/clean-clone-verification.md`
- `docs/architecture/decisions/ADR-0007-isolated-verification-bootstrap.md`
- `specs/001-platform-foundation/tasks.md`

Git status can additionally flag16 generated working paths following byte-only
checkout normalization/attribute introduction. `git diff` has no generated-content
changes and each working file was compared directly with its original HEAD blob.
These are not16 additional SDK source modifications. No files are staged.

Next prerequisite is owner-approved exact failed-session recovery, followed by
focused new-session Rabbit lifecycle tests. A full NEW clone acceptance additionally
requires reviewed publication/integration/develop CI, the whole ordered SDK/web/
mobile/backend/infrastructure sequence, a new APK and cleanup evidence. Independent
developer AC-001-05 remains separate. No AC or parent task is closed by this report.
