# Portable develop baseline — execution record

2026-11-27 · Owner release exception · LOCAL/SOURCE/TEST only.
Status: **IN_PROGRESS — NOT PUBLISHED / NOT MERGED**.

Publication checkpoint: **PR52 OPEN — MERGE_BLOCKED**. Source is published on the
release branch; the initial status above records preparation time, not live Git
state. The latest GitHub checks/merge record at PR52 are authoritative.

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

### Retained clean-clone findings and corrections

Candidate cfc5d7db10f17a35d98003f3d401e9c7b054206b was genuinely cloned using
git clone --no-local. Cold DB/Backend/mail migrated/started correctly, but Windows
checkout CRLF in clamd.conf broke the real scanner: FAIL retained. Added LF only
for infra/local/*.conf; no migration or scanner guard weakened. Failed project
was stopped and its volumes retained.

Expanded Compose smoke found Business Contact confirmation FAIL despite real
SMTP: proxy added a second Cache-Control header, violating the client's exact
no-store contract. Hid only that upstream duplicate; gateway still emits no-store.
Rebuilt Web only. Smoke including actual Business Contact proof issuance/SMTP/
confirmation and persisted draft attestation then PASS. No submission/approval
or privileged account/grant fabricated. Each explicit smoke invocation creates
a new synthetic account/draft; failed runs/audit remain. Zero-account snapshot
is historical, not the state after opt-in testing.

Original staged Owner instructions/North Star were explicitly added alongside
the157 unstaged WIP paths. Existing attributes retained. Protected V47's original
final blank line and supplied Markdown hard line breaks preserved; attributes
describe those formats. Release-only OrganizationCasePolicy Java EOF blank line
trimmed without logic changes or overwriting original WIP. Candidate validator
and secret scan PASS. A second new clone/cluster will verify both corrections.

### Final clean-clone gate — PASS

Exact runtime candidate **d6f25865297b610adcce9bbfa09ca547e9dc35cf**, cloned
with git clone --no-local into a separate directory, clean Git status. Only
documented public .env project/port overrides were created (5444/8026 to avoid
the first baseline stack); no .demo/private source, home credentials or host
dependencies are build inputs. Actual docker compose config / up --build -d
--wait PASS; all five services healthy. Second cold target:
argopost_admin_uat_1d1d18d3f42a, deployment
5de0097f-6dd9-48b3-9ccb-a14be93fc606. Cold and stop/up restart both PASS,
identical48 migration/checksum rows and one ENV_BIND audit/receipt. Initial
accounts0/grants0, SYSTEM INACTIVE; no private-table access for BRC operator.
Backend/migration uid10001 probes cannot read operator credentials; migrator
cannot read bootstrap SQL holding role passwords. No permission repairs.

Real HTTPS smoke from that clean clone PASS at localhost:5444, including bound
Business Contact verification, retained DRAFT, session cookies/CSRF/isolation/
responsive/logout. Host test dependency fetch initially hit sandbox EACCES to
SheetJS CDN; normal reviewed network escalation retried the same locked npm ci
successfully. Docker startup itself requires no host Node/npm. No global setting
or credential change. Canonical role-generator targeted unit1/1 PASS.

Backend JAR SHA256:
8317ccdd0136dfd8b01a692b25d635cb3a2c4b08bf2b1b1571134014ed6a16f3.
Web index SHA256:
3f631c9b2b88ee02c4d996bb6568f42ffb1f556bef68274c4b30dc1fefcea1f3.
Machine-readable catalog/evidence: baseline-verification.json. Any subsequent
documentation-only commit does not change these runtime inputs. CI and merge
remain mandatory, pending gates. No three-machine parity claim.

### PR52 initial CI failures and exact corrections

Initial PR head237dcf400d57d0986ab1c43fcf86aabad00b310c:
Backend verification run45974787711 failed all three jobs in the Python report
guard, before running Backend tests. The accepted release source introduced16
NWS/S1/read-preparation integration cases, one migration case and three unit
suites absent from the old explicit inventory. Expanded exact names/order:
Identity grants478, migration85; aggregate H2 passed756 and PG passed1347.
Retained every original case, skip allowance, failed/missing/renamed/unknown
rejection and private diagnostic boundary; no assertion or business gate removed.
Added source/mode and negative-report checks for the three unit suites.
Intermediate guard run exposed old aggregate totals; that FAIL is retained.
Final isolated Linux no-network/read-only guard run: **149/149 PASS**.

Portable workflow run45974777740 failed parsing job env: runner context is not
available there. Moved only PLAYWRIGHT_BROWSERS_PATH to the browser step env.
Official actionlint1.7.12 archive SHA256
6e7241b51e6817ea6a047693d8e6fed13b31819c9a0dd6c5a726e1592d22f6e9
verified; old workflow reproduces exact error, corrected workflow PASS. No
Backend/domain/migration/ACL change or need to repeat the accepted S1 suite.

PR: https://github.com/phamquoainam/logistics-capstone-project/pull/52.
Latest exact-head GitHub checks and merge record are authoritative for publication;
this execution history does not imply CI/merge PASS. Original auto-review usage
failure temporarily blocked a read-only diagnostic; resumed through normal
review after user continuation, without a second execution path or bypass.

### PG integration failures on b9afb26 and focused repairs

Run46004001802: Architecture PASS, Compose run46004001940 PASS; H2 failed at
WRAPPER_VERSION DOWNLOAD_FAILED before tests. PG completed29m25s with1325 passed,
5 allowed skips and22 nonpassing cases. No full-suite PASS is claimed.

Focused reproduction de463f3746f14502995661d12bc36ed0 was RED: latest-history
assertion still ended at V47; historical-to-latest counts were two too small;
current intake reader ran on a V40 Dashboard fixture without owner_type.
Preserved original fixture source in the private preservation archive first.

Repairs change test fixtures only, not V1-V49, application code, SQL guards or
ACL: exact migration history now includes48/49;15 upgrade expectations account
for the two approved migrations, retaining all historical targets, zero-rerun,
checksum and row-preservation assertions. Exact foundation owner-table inventory
is171, verified against the authoritative V49 catalog (was stale164). Dashboard
live intake fixture migrates to latest before invoking the current intake code;
historical onboarding target tests remain at their original versions.

Focused276fe614f74f473fb3a32efdecca1bf8: **4/4 PASS**. Additional focused
5715fce404fa4a6e92fda5d5dddb3ff6: **17/17 PASS**, including14 remaining migration
failures and genuine Admin Dashboard/Core/Business S1 HTTPS browser scenarios.
Both runs Maven0, sourceUnchanged=true, snapshot SHA256
0f878a1a3de443e5f89d0ac77c861d236d7e83e59e091a52a8e221ea3a80eb00.
CI-only browser failures are not declared resolved solely from local results;
the next exact-head CI must confirm them. No retries/assertion/authority weakening.

Latest complete audit:186 changed/staged/untracked root paths are all in the
explicit release allowlist; zero unexplained paths. Original HEAD/index retained,
all48 migration bytes unchanged. Only intentional release documentation/source
inventory and the three backed-up test fixtures differ from the original157-path
WIP snapshot. No source from another concurrent writer was overwritten.

Inventory categories now distinguish APPROVED, REQUIRED and UNAPPROVED proposal
documents (documentation only, no runtime). LOCAL_ONLY and SECRET/SENSITIVE
exclusions remain explicit. Spec012 remains PROPOSED/BLOCKED_CONTRACT. No claim
that local fixture fixes are already in develop; publication is through PR52 and
post-merge checks on its eventual exact merge commit.

### Exact-head35bff88 result: one CI-only BIA failure remains

Backend run46011132559: H2 and Architecture PASS. Portable Compose run46011132698
PASS. PG job140546626248 completed29m22s; all previous schema/count/reader failures
cleared, but biaRealHttpsCustomerApplicationToBusinessStation remains nonpassing.
Local focused17/17 includes that case; this is not evidence of CI resolution.
PR52 remains OPEN/MERGE_BLOCKED. No migration or runtime authority changed.

Added test-only bounded diagnostics to distinguish the existing browser stages
and label/mail/scanner postconditions. The classifier accepts only literal
allowlisted markers from two exact known test identities, retains SQL/resource
error precedence, and never emits arbitrary messages, paths, emails, credentials,
proofs or response bodies. Unknown/conflicting/foreign markers stay generic.
Every original assertion, timeout and report acceptance gate remains intact.
Classifier/privacy tests5/5 and Maven offline test-compile PASS. This is a
diagnostic checkpoint, not a fix or readiness claim; exact-head CI is required.
