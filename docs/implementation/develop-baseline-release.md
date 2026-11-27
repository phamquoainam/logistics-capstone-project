# Portable develop baseline — execution record

2026-11-27 · Owner release exception · LOCAL/SOURCE/TEST only.
Status: **IN_PROGRESS — NOT PUBLISHED / NOT MERGED**.

Nam is temporary Integration Writer for Docker/build/local configuration and
accepted Slice1 integration. Hoàng/Triết retain equal domain authority. No domain
contract, historical migration, real grant or production authority is expanded.
No concurrent remote PR was open at audit; origin/develop equals local
`d5a0c240841bdb35d9ef6064cddba696a5b2fa62`. Original WIP/index and all48 migration
files V1–V49 excluding33 are fingerprinted in protected local preservation data.

## Audit before changes

| SYMPTOM | ROOT_CAUSE | CURRENT_SOURCE | MINIMAL_FIX | DOMAIN_WRITER | APPROVAL | REQUIRED_TEST |
|---|---|---|---|---|---|---|
| Clone lacks accepted S1 | V44–V49/source/contracts remain untracked/WIP | Git inventory + S1 execution | Explicit reviewed release inclusion; never broad add | Nam integration, canonical owners unchanged | Owner baseline release | Clean checkout build + S1 HTTPS/PG18 |
| local.cmd fails on another machine | Requires private `.demo/local-runtime/backend-v40.jar`, LOCALAPPDATA settings, named demo container and V35 | scripts/dev/start-local.ps1 | Separate portable Compose path; preserve legacy demo launcher | Nam | Portability scope | Cold start without those paths |
| AWS/Gmail prerequisite | Old launcher calls personal AWS profile/bucket; provision-local selects real Gmail/S3 | scripts/dev/provision-local.mjs | Existing local public media + private evidence filesystem; isolated loopback SMTP sink | Platform/Identity configuration only | Existing adapters + baseline directive | No AWS config/mount/egress fallback; genuine email proof |
| No application Compose | Existing stacks cover infrastructure/verification only | infra/compose and infra/verification | Root Compose with build/init/migrate/Backend/Web health ordering | Nam | Baseline directive | Config/build/health/restart |
| Fresh PG cannot migrate current stream | Historic bootstrap lacks canonical NOLOGIN prerequisites introduced later | infra/postgres/bootstrap.sql; scripts/ci/staff_role_prerequisites.py | Reuse exact canonical prerequisite SQL generator in isolated new cluster | DB bootstrap orchestration | Existing approved role/ACL policy; no new grants | Owner/ACL/checksum/upgrade tests |
| TLS/session not generic | Local certificates/config/JWK live outside Git | existing local scripts and Identity configuration | Generate unique protected material per named volume; same-origin HTTPS and verified TLS proxy | Nam | Local config only | HTTPS/CSRF/cookie/audience regressions |

## Bounded patch and validation plan

Add root compose.yaml, build Dockerfiles, protected material generation and
canonical new-volume DB initialization, explicit migration/binding jobs, local
SMTP sink, real scanner where Merchant evidence needs it, runbook and static/
runtime validation. Reuse filesystem evidence; S3 emulator is unnecessary for
the current accepted path. No Redis/Rabbit dependency is enabled in current
Identity/Customer/Postgres profiles. Courier mobile remains optional host Expo;
it is not a containerized device/emulator.

Preserve legacy demo volumes/JARs/settings and all migration bytes. Default stack
creates no user, fake approval, Staff assignment, acceptance or payment. Synthetic
account setup is opt-in and must use canonical workflows, with verification.

Gates: config → image build → cold isolated PG initialization/migration through
V49 → Web/Backend/DB health → persistent-volume restart/checksums → clean checkout
reproduction → Personal+Business S1 HTTPS/PG18 → affected UI/build/security → secret
scan → scoped branch/PR → CI/review/protection → merge → exact SHA/post-merge smoke.
No gate can be substituted by the earlier S1 report or an unrun command.

## Team documents and release boundary

Initially DOCUMENTS_PENDING; the four team files were subsequently provided under
`docs/implementation/v1-team/` and are included unchanged. Assignment headers are
v1.5; the companion calendar header remains v1.3. Its S1 PARTIAL planning text is
historical, superseded by the accepted S1 execution report. Hoàng/Triết must ACK
the released revision and writer boundaries; three-machine parity is not claimed.

Spec012 remains PROPOSED/BLOCKED_CONTRACT; no runtime SPM API/DDL/ACL. Deferred
Menu2 StepB is excluded. Existing accepted R01/Lifecycle preparation dependencies
must be explicitly accounted for if needed by the accepted current source.

Git publication is conditional: no force push, protection/review bypass or direct
develop write. If any mandatory gate fails, retain PR and report MERGE_BLOCKED.

## Actual baseline verification, 2026-11-27

- Docker VHDX verified on D in the configured DockerDesktopData directory;
  capacity checked before builds. No prune, deletion or storage/global changes.
- Initial project argopost-local: cold migrations PASS, Backend FAIL because
  canonical Admin physical environment binding was missing. Correctly denied
  migrator authority was not bypassed. Stopped volumes are preserved.
- BRC-ACL-01 Owner approval verified, document SHA256
  `a15804e3a026e314345e326855bb2485efaa1aec3e01baf7c291903487c2d0da`.
  Implemented only in a second NEW project argopost-baseline. Operator secrets
  have a separate volume, not mounted to Backend/Web/migrate. No role retrofit.
- Actual Compose build/up PASS: PG18.6, database
  `argopost_admin_uat_dbef5eae06b7`, deployment
  `2e8a6dc3-8ff5-4caa-a408-840fa983a16f`; five long-running services healthy.
  First Web health FAIL (BusyBox wget lacks CA flag) corrected with curl and
  CA verification, never trust-all TLS. Failure evidence retained.
- Catalog:48 migrations, no V33, zero failed migrations or LOGIN-owned domain
  tables. V48 checksum -1477124834; V49 -2022074179. BRC operator has exactly
  canonical membership INHERIT=true/SET=false/ADMIN=false, no privileged role
  attributes, zero private-table read/DML, no pg_control_system() access.
  Runtime cannot call pg_control_system() either. Canonical EXECUTE list retained.
- Same-key bind-admin replay and ordinary up -d PASS. Cold/restart database,
  migration/checksum entries and ENV_BIND receipt/audit counts identical, counts1.
  SYSTEM stays INACTIVE; initial accounts0/grants0. Later opt-in browser smoke
  creates one synthetic Customer through actual enrollment, not bootstrap DML.
- Compose HTTPS browser smoke PASS: registration/SMTP OTP/verification/login/
  logout, Secure+HttpOnly+SameSite=Strict host refresh cookie, CSRF403,
  Customer-to-SYSTEM401, no persistent browser token,390px responsive layout.
  Exact generated certificate SPKI pin; ignoreHTTPSErrors=false. Secrets not logged.
- Fresh S1 run c3cda1604f0b462fa968795f51869700: **6/6 PASS**, Maven0,
  SOURCE_UNCHANGED=true; snapshot SHA256
  `bb37d1807ae9cbd4ea34cb27e6efe3e57909d66e2b8bf2fc1460e06bf3a55c81`.
  run-cskh-focused.ps1 executes actual dual-audience HTTPS/PG18, atomic Business
  intake/negative/race/rollback, populated upgrade and cold ownership checks.
  These isolated fixtures are separate from persistent Compose. No full Backend
  suite rerun or implied privileged identities in persistent Compose.
- Frontend npm.cmd typecheck/lint/test PASS,272/272 units. Container Web
  build:customer/typecheck and Backend package PASS. npm.ps1 was blocked by host
  execution policy; npm.cmd used without changing that policy. Existing large
  frontend chunk warning retained.

Protected evidence: .demo/portable-baseline/ cold/restart/final catalog, build,
browser logs and original preservation inventory. No private logs are published.
Source generation, TLS, role prerequisite and runtime isolation tests run inside
the tools image build. New CI job repeats clean checkout cold/restart and actual
Customer HTTPS smoke with isolated SMTP, no AWS profile/S3/real SMTP.

## Release inclusion and handoff

baseline-release-inventory.json is the explicit allowlist for157 pre-existing
accepted WIP paths plus portable baseline/team documents. NWS V44/V45, accepted
M2-R01 V46 and Lifecycle PREPARATION V47 are transitive dependencies of current
S1 source/stream. Their accepted implementation is preserved. Deferred StepB,
Owner Transfer and Spec012 documents remain proposals, not activated functionality.
No .demo/.local/private certificate/key/log/cloud profile/build output or DB data
is included. Release worktree is separate; original index/WIP preservation checked.
Existing gitattributes safeguards retained; shell entrypoints use LF for Windows,
new approved artifacts and migrations preserve exact bytes.

Pending: clean clone build/start/restart/authentication, published CI/review gates,
exact merged SHA and post-merge smoke. Existing Admin multi-user READ contention
from the S1 report is an unchanged IAM limitation. No three-machine/device/full
logistics E2E/Production claim. Handoff packet is prepared, not sent or ACKed;
Hoàng receives S1 contracts plus Spec012 PROPOSED, Triết downstream boundaries.
