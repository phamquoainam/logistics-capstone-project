# D1-05 — CI Architecture Gate & Clean Runner Verification

**D1-05-B update (2026-10-14): PUBLISHED / PARTIAL.** User authorized reviewed
commit/push/PR creation; implementation commit `91944517eea4ff4254421c8b0aa753f5c70cba03`
is pushed to `origin/feature/d1-05-ci-verification`. PR creation is currently
BLOCKED by connection failures on authenticated API POST; actual Actions remains
UNVERIFIED until a PR exists. Read-only GitHub API works. Develop protection is
verified, but none of the three CI checks is required. No merge/settings change.
The original D1-05 no-publish statements below retain their historical scope.
See the D1-05-B execution section at the end for publication and acceptance gaps.

**2026-10-14 — PARTIAL / READY FOR REMOTE VERIFICATION.** Configuration and local
clean Linux execution PASS. Actual GitHub Actions and required-check enforcement
are UNVERIFIED: user explicitly forbids commit/push/PR/merge in this task.
No remote run URL exists for this change. This is not an actual CI PASS report.

## Git checkpoint

Initial `develop` with upstream `origin/develop`, clean working tree. Both local
refs and `git ls-remote origin refs/heads/develop` resolved to
`2c609c42863505d0e28400eaa26f36c72b062769` (PR #4 D1-04 merge).
`git merge-base --is-ancestor 3cfacdf HEAD` and `3c2bd8f HEAD` exited 0;
architecture/fixture tests, Compose/bootstrap, V1/V2, PostgreSQL configuration,
integration tests and D1-03/D1-04 evidence are present in the merged tree.
Local/remote D1-05 branch did not exist; created `feature/d1-05-ci-verification`
from this HEAD. Final HEAD unchanged, no upstream on this new unpushed branch.
All current changes belong to this task; no initial uncommitted changes existed.

Remote baseline inspection initially failed under network sandbox; the authorized
read-only retry succeeded. Branch creation required Git metadata permission.
Neither is a remaining toolchain blocker. No commit/push/PR/merge/reset/stash.

## Implementation

[Workflow](../../.github/workflows/backend-verification.yml) `Backend verification`:
PR into develop, push develop, optional manual dispatch; three independent matrix
jobs on Ubuntu 24.04: `Architecture gate`, `Backend default / H2`,
`Backend PostgreSQL 18`. Full-SHA Actions, read-only permission, no persisted Git
credential or Maven-cache restore. Temurin 21.0.10+7; Java/Boot/Maven/ArchUnit and
PostgreSQL image stay on approved baseline. See [CI operations](../quality/ci-verification.md)
for action provenance, failure behavior and required-check setup.

New `scripts/ci/` files: `run-backend.sh`, `maven-settings.xml`,
`verify-reports.py`, `test_verify_reports.py`, `prepare-postgres.py`.
Reports require named suites/methods and valid counters; missing/zero/skipped
architecture or fixture tests fail even if Maven returned zero. No skipTests,
suppression, dependency changes, business source edits or Maven plugin changes.
No Failsafe needed: existing `*Tests` are discovered by Surefire/Jupiter.

Docs updated: this evidence, CI quality guide, Day 1 plan, local runbook,
traceability and feature 001 spec/design/tasks metadata. Original AC/REQ remain
unchanged. POM, wrappers, Flyway V1/V2, Docker configuration, dev/native DB,
authentication/RBAC/merchant ownership/shipment semantics and web are unchanged.

## Actual local commands and results

The following is a **local clean Linux simulation**, not a GitHub-hosted run.
Three separate disposable containers were started from a locally built Ubuntu
24.04/Temurin 21.0.10+7 image. Base image pinned to
`eclipse-temurin:21.0.10_7-jdk-noble@sha256:cfa5cae8f739b7b0113cd9410874718456b79a4f459e203aa0c5b6e7dcd1d7b2`.
Only Python/curl/ShellCheck/actionlint were added inside that temporary image;
nothing was installed into Windows or repository dependencies. Actionlint 1.7.12
official release checksum verified before execution.

`git archive HEAD app/backend` staged tracked source with Git LF bytes (Unix
wrapper in Windows working copy has CRLF). Overlaid only new CI scripts/workflow;
no host target, .m2, private .demo, settings, H2 files or PG volume was copied or
mounted. Each mode started with a new empty Maven user home and explicit empty
CI settings; wrapper distribution and all Maven dependencies resolved online.

| Actual command / gate | Result | Evidence / scope |
|---|---|---|
| `python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v` on Windows; same `python3` command on Linux | PASS 14 | Missing/zero/skip/duplicate/malformed/count mismatch/Maven failure/PG skip masking/secret filtering and exact CapturedOutput naming tested |
| `docker exec elogistics-ci-linux-architecture-20261014 actionlint .github/workflows/backend-verification.yml` | PASS exit 0 | Workflow syntax, expressions, action inputs and inline shell; full-SHA release refs checked separately |
| `docker exec ... shellcheck scripts/ci/run-backend.sh` | PASS exit 0 | First warning about unquoted comma in -Dtest array argument fixed by quoting; no suppression |
| `docker exec elogistics-ci-linux-architecture-20261014 bash scripts/ci/run-backend.sh architecture` | PASS exit 0 | 23 discovered/executed/pass, zero failures/errors/skips; 11 production/scope + 12 fixture tests |
| `docker exec --env RUNNER_TEMP=/tmp/ci-final elogistics-ci-linux-h2-20261014 bash scripts/ci/run-backend.sh h2` | PASS exit 0 | 48 discovered, 40 executed/pass, eight conditionally skipped PG-only tests; no database container/network dependency |
| `python -B scripts/ci/prepare-postgres.py start --directory .demo/d1-05/postgres-private-final --network-container elogistics-ci-linux-postgres-20261014` | PASS | New PG18 container, tmpfs data, two isolated test databases/three roles; shared runner loopback, no host port or dev volume |
| `docker exec --env RUNNER_TEMP=/tmp/ci-final-2 --env-file .demo/d1-05/postgres-private-final/postgres.env elogistics-ci-linux-postgres-20261014 bash scripts/ci/run-backend.sh postgres` | PASS exit 0 | 48 discovered/executed/pass, zero failures/errors/skips; new Maven cache and fresh PG databases |
| Private Maven log check for generated PG passwords; published-artifact credential/raw-output audit | PASS | No CI password in tested Maven log/public artifacts; public XML contains no properties/system-out/system-err/failure bodies |
| Git diff whitespace and protected-source diff checks | PASS | No business/build/migration/Compose edits; HEAD unchanged |
| Actual GitHub Actions checkout/setup/upload/container lifecycle | UNVERIFIED | Local Linux uses the same verify/guard scripts, but is not the GitHub runner/action service |
| Required status checks on develop / deliberate-red remote PR | UNVERIFIED | No repo setting or remote experiment authorized/performed |

Inside `run-backend.sh`, the actual wrapper command is `bash ./mvnw --version`,
then `bash ./mvnw -B -ntp -Dstyle.color=never --settings <CI settings>
-Dmaven.repo.local=<fresh temp repository> -Ddebug=false -Dtrace=false clean verify`,
adding `-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests` for architecture
or `-Ddemo.test.database=postgres` for PostgreSQL. Version checks PASS Java 21 /
Maven 3.10.0. No offline/local-cache result is presented as clean-runner evidence.

### Findings corrected during verification

Initial H2/PG Maven executions were green, but the new guard rejected the actual
Surefire name `credentialsAndBearerTokenDoNotAppearInApplicationLogs(CapturedOutput)`.
Final code recognizes only this exact known alias, with regression and rejection
tests for arbitrary dynamic names; discovery/failure criteria were not weakened.

An intermediate PostgreSQL rerun reused a fixture database and failed (34 passed,
14 failed/error). The suite is designed for newly provisioned databases, including
its V1-only upgrade probe. Only the earlier D1-05-owned temporary PG container was
removed; fresh DB/roles/container and a fresh Maven home yielded final 48 PASS.
CI provisions once per fresh job; retries must not reuse test data. These failed
attempts are not counted as final PASS or hidden by a workflow exception.

### Negative fixture proof

Existing `financeCannotUseOrderInternalRepository` evaluates/checks the actual
`MODULE_BOUNDARIES` rule against isolated Finance → Orders repository bytecode,
asserts the forbidden dependency detail and thrown AssertionError. Ten other
fixtures cover direction/leaked internals/cycle/JDBC/controller transaction/broad
AccountView/domain actor violations; one positive control accepts minimal actor
at the application boundary. All 12 fixture methods executed on Linux in all
three modes. Production tests are non-vacuous on 37 classes in
identity/merchant/orders/platform. No production violation or suppression added.
Six missing business modules remain N/A, not architecture-PASS.

The report guard independently demonstrates failure for a failed fixture report
and for nonzero Maven with green reports. Remote enforcement still requires the
real workflow and required checks; the local assertion proof is not a red PR run.

## Artifacts and isolation

Local sanitized evidence: `.demo/d1-05/evidence/{architecture,h2,postgres}/`
contains only `summary.json` and `tests.xml` (ignored, not committed).

| Mode | SHA-256 summary.json | SHA-256 tests.xml |
|---|---|---|
| architecture | `0d2b9972f1d8464736228c4ac0cf110ed42a3a0591ac90d4af060fdbe6a3e3dd` | `675b90283ffc9b8db5a8f609f76a2c4cde8c5ddf7879c277460091003f00974f` |
| h2 | `895707e574519a6c4d42d5949de63d5f0e63f9a4f2967228d2a1926656a83e2c` | `b7c316fc478366b1422a1e715b27b54762ea9dc2135044d9c5632207c87e95d2` |
| postgres | `8a781158f88330fe0eb50be0aae682d695b668155eaf4df165a6e4a3666eed13` | `1e0322fee064546c168943bd4c1fa3c77fa975b0c2fbd00be4db7cf1cd00bf05` |

CI publishes these two allowlisted outputs for each mode, seven-day retention;
raw diagnostics are private, never artifacts. Provisioning captures psql output,
uses random temporary passwords and GITHUB_ENV masks; no real credentials in Git.
Local ephemeral PG runs in runner network namespace so the existing
`elogistics-d1-postgres18` remained healthy on 127.0.0.1:5433 throughout. Native PG18,
developer database/volume, migrations and Docker/WSL data are not targeted.
CI's hosted-runner mode binds its own 127.0.0.1:5433, separate from this simulation.

After saving sanitized evidence, helper cleanup verified ownership and removed the
new temporary PostgreSQL container. A separate name/label/image/no-mount check
removed only the three Linux simulation runners; no volume or existing container
was removed. Final `docker ps` shows `elogistics-d1-postgres18` healthy on the
same host port. Raw runner diagnostics were discarded; ignored local provisioning
env/state and sanitized artifacts remain private, and the test image is retained.

## Status reconciliation and remaining gates

| Scope | Current assessment |
|---|---|
| D1-03 PostgreSQL local milestone | Completed/PASS; original restart/persistence evidence retained; current fresh-PG verify PASS |
| D1-03 task-level Day 1 checklist | PARTIAL: D1-02 dependency and full independent onboarding remain open. Fresh Linux backend proof narrows the gap, does not prove web/mobile/full cold start |
| D1-04 approved local task | DONE for approved local scope: user confirms reviewer acceptance and merge, commit 2c609c4; historical evidence retained |
| D1-05 configuration + local Linux gates | PASS / READY FOR REMOTE VERIFICATION |
| D1-05 actual hosted CI + required checks | UNVERIFIED; D1-05 overall PARTIAL |
| REQ-001-03 / EC-001-03 / AC-001-03 | Local rule/fixture proof PASS, executable workflow prepared; actual CI/enforcement remain PARTIAL |
| Full feature 001 / Day 1 | PARTIAL, unchanged AC. Missing modules not declared verified |

Other open gates: D1-02 native development-build readiness, mobile lint/test,
Node/npm pin approval and log disposition, web lint; reviewed OpenAPI/generated
client/health and Redis/RabbitMQ scope for full acceptance; actual independent
developer runbook execution. Managed PG container verification is not claimed as
Testcontainers implementation. No implicit DEFERRED or changed acceptance criteria.

**One highest-priority next task:** publish the reviewed D1-05 branch only after
authorization, create PR → develop and verify the three real GitHub runs/artifacts
plus develop required checks; record tested SHA and run URL before proposing DONE.
No business feature or D1-06 implementation starts as a substitute for this gate.

## D1-05-B — Publication and acceptance review

User explicitly authorized safety review, commit, ordinary feature push and PR
creation; prohibited merge/reset/force push/data deletion/settings changes.

| Phase / actual operation | Result / evidence |
|---|---|
| Git baseline and inventory | PASS branch feature/d1-05-ci-verification, initial HEAD 2c609c4, exact 14 candidate files matching original report; remote develop unchanged |
| Diff/staging safety | PASS git diff --check and staged diff; only explicit 14 paths added; private `.demo`, env/state, logs, target and generated artifacts excluded |
| Credentials/artifact review | PASS candidates contain none of known temporary PG passwords or selected GitHub/AWS/private-key patterns; sanitized artifact hashes still match original evidence |
| Report guard rerun | PASS `python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v`, 14 tests. No source/test/build change required |
| Commit | PASS `git commit -m "ci: add backend architecture and PostgreSQL verification"`; SHA `91944517eea4ff4254421c8b0aa753f5c70cba03`, 14 files, 942 insertions/5 deletions |
| Push | PASS `git push --set-upstream origin feature/d1-05-ci-verification`; upstream SHA equals local implementation HEAD; no force push |
| Create/reuse PR | BLOCKED: two Python API POST attempts and a PowerShell HTTP POST failed connection; subsequent GET found no matching open PR. No duplicate/merge attempted |
| Access fallback | Existing Git credential authenticates repo read/push and protection reads without token output/persistence. gh absent; browser inventory empty and IAB reports unavailable. No new tool/token/account installed or created |
| Actual GitHub Actions | UNVERIFIED: branch push alone is not a trigger for this workflow; no PR exists yet and branch-runs API returned zero runs. No remote URL or remote test count asserted |

GitHub repository is public, authenticated permission metadata includes admin/
push/read. API reads confirm Actions enabled with allowed_actions=all. POST
failure is reported as a network/environment blocker, not an unproven permission
denial. Authentication remains only in memory; helper files in ignored `.demo`
contain no GitHub token. Windows core.autocrlf=true normalizes staged Bash to LF;
Unix wrapper invocation still uses Bash.

### Required-check audit (read only)

GET `/repos/phamquoainam/logistics-capstone-project/branches/develop/protection`
returned HTTP 200; GET `/rules/branches/develop` returned `[]`.

| Protection setting | Actual result |
|---|---|
| Develop protected | YES |
| Require PR | Configured; required_approving_review_count=0, no code-owner/last-push approval requirement |
| Apply to admins | enforce_admins.enabled=true |
| Required status checks | NOT CONFIGURED: no required_status_checks object in classic protection; no effective ruleset adds checks |
| Conversation resolution | Required |
| Force pushes / deletion | Both disabled |
| Three new CI checks required | NO: Architecture gate; Backend default / H2; Backend PostgreSQL 18 |
| Actual PR cannot merge while any new mandatory check is pending/red | UNVERIFIED for a PR; configuration does not enforce these checks because they are not required |

Maintainer must first create the PR and allow these check names to appear. Then
open repository Settings → Branches → edit the existing develop protection.
Keep required PR/admin/force-push/deletion/conversation policy. Enable
`Require status checks to pass before merging`, select exactly `Architecture gate`,
`Backend default / H2`, `Backend PostgreSQL 18` from GitHub Actions, and save.
No setting was changed by Codex. [Official UI instructions](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/managing-a-branch-protection-rule).

### Manual PR fallback and closure

Create at [develop ← D1-05 compare](https://github.com/phamquoainam/logistics-capstone-project/compare/develop...feature/d1-05-ci-verification?expand=1).
Title: `ci: verify backend architecture and PostgreSQL on clean runners`.
Prepared description is in ignored `.demo/d1-05-b/pr-body.md` and covers scope,
architecture, H2/PG, report/privacy guard, local evidence and pending remote gates.
After creation, provide the PR URL; read-only API can inspect the three runs and
download sanitized artifacts. Do not merge before acceptance/protection review.

D1-05 stays PARTIAL: publication PASS, actual hosted execution UNVERIFIED and
required checks NOT CONFIGURED. REQ-001-03/AC-001-03 remain PARTIAL. The highest
priority next task is **create the PR into develop so actual CI can execute**,
then continue remote verification. Other Day 1/feature gates retain their status.
