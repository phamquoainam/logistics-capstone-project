# Backend CI verification

**D1-05-B publication (2026-10-14):** implementation commit 9194451 pushed on
feature/d1-05-ci-verification. Actual hosted CI still UNVERIFIED pending PR creation
(API POST connection blocker). Read-only audit confirms develop protection but
no required status checks/effective ruleset. Keep D1-05 PARTIAL; no settings changed.
[Publication and manual PR fallback](../implementation/d1-05-ci-evidence.md#d1-05-b--publication-and-acceptance-review).

Workflow: [Backend verification](../../.github/workflows/backend-verification.yml).
Scope: D1-05, approved after the D1-04 merge. No deployment or business changes.
Status: **READY FOR REMOTE VERIFICATION**; local execution and remote Actions are
separate gates. [Execution evidence](../implementation/d1-05-ci-evidence.md).

## Triggers and required checks

PR targeting `develop`, push to `develop`, and manual `workflow_dispatch`.
No path filter or privilege-bearing `pull_request_target`. Matrix jobs have stable
names: `Architecture gate`, `Backend default / H2`, `Backend PostgreSQL 18`.
Each runs independently on `ubuntu-24.04`; fail-fast is disabled so all results
remain visible. Timeout 30 minutes, read-only contents permission, checkout does
not persist credentials, concurrent superseded runs are canceled.

Actions use full commit SHAs for checkout v7.0.1, setup-java v6.0.0 and
upload-artifact v7.0.2, checked against official release tags. Review SHA updates
explicitly; do not replace them with floating tags. Java is Temurin 21.0.10+7,
matching the verified Java 21 baseline; Boot 4.1.1/Maven 3.10.0 remain unchanged.
Sources: [GitHub supply-chain guidance](https://docs.github.com/en/actions/reference/security/secure-use),
[checkout releases](https://github.com/actions/checkout/releases),
[setup-java v6](https://github.com/actions/setup-java/releases/tag/v6.0.0),
[upload-artifact releases](https://github.com/actions/upload-artifact/releases).

An executable workflow alone does not enforce merging. After a successful actual
PR run, an authorized repository maintainer must configure a branch rule/ruleset
for `develop` requiring all three checks. Select the exact check contexts shown
by GitHub, with GitHub Actions as their source. Keep required review and existing
protection policy. No repository setting was changed in D1-05.
[Official required-status-check behavior](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).

## Execution and discovery

All jobs run `python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v`
then `bash scripts/ci/run-backend.sh <architecture|h2|postgres>`.
The Unix wrapper is tracked without execute permission; invoke it with Bash.
Do not replace it with a runner-installed Maven or use the Windows cmd script.

Every job creates an empty `$RUNNER_TEMP/d1-05-maven`. Both wrapper distribution
and Maven dependencies start uncached; a nonempty Maven home makes the job fail.
Explicit [settings](../../scripts/ci/maven-settings.xml) avoid developer/global user
mirrors, profiles or credentials. No Maven cache restore, SSL bypass or cache purge.
The script verifies Java 21 and Maven 3.10.0, then from `app/backend` invokes:

```bash
bash ./mvnw -B -ntp -Dstyle.color=never \
  --settings "$root/scripts/ci/maven-settings.xml" \
  "-Dmaven.repo.local=$MAVEN_USER_HOME/repository" -Ddebug=false -Dtrace=false \
  -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests clean verify
# H2/default: omit -Dtest. PostgreSQL: omit -Dtest, add -Ddemo.test.database=postgres.
```

Surefire inherited from Boot runs existing Jupiter `*Tests`; no Failsafe or new
engine is required. The report guard requires every named production architecture
test and every negative-fixture test, verifies counters, rejects duplicates,
malformed/unknown reports, missing tests and skipped architecture tests, and
propagates a nonzero Maven exit. Inventory changes need an explicit update to
`REQUIRED` in `verify-reports.py`; do not loosen discovery checks to permit zero
tests. A single exact alias handles Surefire's `CapturedOutput` parameter name.

| Mode | Required discovered / executed | Allowed skips |
|---|---|---|
| architecture | 23 / 23: 11 production/scope, 12 fixture | None |
| h2 | 48 / 40 | Exactly eight named PostgreSQL-only tests, conditional by existing test annotation |
| postgres | 48 / 48 | None |

Eleven negative fixtures evaluate the same production ArchUnit rules and assert
the actual forbidden dependency/cycle/direction result plus thrown assertion;
the twelfth is an allowed actor control. They are test scope only. A fixture test
passes when detection works; it does not itself make a valid production build
red. If production violates a rule, `ArchitectureTests.check` fails, Maven exits
nonzero and the job fails. A remote deliberate-red PR is a separate experiment
requiring approval; no intentional violation was added to production.
Six business modules without code remain NOT YET IMPLEMENTED / N/A for coverage.

## Disposable PostgreSQL

`python3 -B scripts/ci/prepare-postgres.py start --directory "$RUNNER_TEMP/d1-05-postgres"`
starts only a new labeled container, pinned to the same D1-03 image:
`postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c`.
Runner-only host binding is `127.0.0.1:5433`, server port 5433; data is tmpfs at
the PG18 data root, with no named or developer volume. This does not require
Docker Desktop on a developer's machine; the hosted Ubuntu runner supplies Docker.

The helper reuses the owner/migrator/runtime policy from
`infra/postgres/prepare-test.py`, but never targets its fixed development container.
The existing bootstrap SQL names `elogistics_dev`, so CI deliberately provisions
two newly named `elogistics_test_*`/`elogistics_test_upgrade_*` databases instead.
It creates a NOLOGIN owner, migration role with owner membership, DML runtime
role and owner-managed platform schema. Existing tests execute unchanged V1/V2,
permission, constraints, rollback/concurrency, validate/rerun and V1-to-V2 upgrade.
No `clean`, `repair`, Hibernate update or H2 substitution for PostgreSQL tests.

Random bootstrap/migrator/runtime passwords exist only in runner-private files
and process environment. Bootstrap uses `POSTGRES_PASSWORD_FILE`; SQL is passed
via stdin with captured output, never password-bearing command-line arguments.
GitHub masks are registered before test execution and values passed via GITHUB_ENV.
State files contain only container identity. Cleanup always runs, validates the
recorded name and ownership label and removes only that ephemeral container;
no `-v`, prune, development Compose down or database reset. Every retry requires
fresh databases; the complete suite is not a fixture reset mechanism.

## Evidence privacy and failures

Maven and raw Surefire diagnostics can include MockMvc tokens during failures.
They stay in `$RUNNER_TEMP/d1-05-private`/backend target and are never streamed or
uploaded. Published artifacts include **only** `summary.json` and `tests.xml`
from `$RUNNER_TEMP/d1-05-reports`, retained seven days. These contain allowlisted
class/method/status/counts, no properties/system output/failure bodies/passwords.
The private Maven log is also checked for the generated PG passwords; a match
fails the job without printing its value. Sanitizer tests verify secret removal.
Missing reports/artifacts fail; uploads and cleanup cannot hide a prior failure.

Use the sanitized failed method and stage to reproduce locally. Do not upload raw
Maven logs, Surefire XML/TXT, private env files, containers logs or settings to
debug a failed run. Wrapper/network failures may have no test artifact; this is
a failure, not a zero-test PASS. Hosted runner disposal removes remaining private
files; local simulations retain provisioning env/state only in ignored `.demo`.
Raw diagnostics are discarded when their disposable runner containers are removed.

## Remote completion procedure

After the user authorizes publishing, commit the reviewed diff on
`feature/d1-05-ci-verification`, push it and create a PR with base `develop`.
Observe all three real check runs and download only sanitized artifacts. Record
run/attempt URL, tested SHA, runner/toolchain, counts/skips and result in D1-05
evidence. Set and verify the `develop` required checks through an authorized
maintainer. Do not merge while any mandatory job is red or absent.

D1-05 remains PARTIAL until actual clean hosted execution and required-check
enforcement are evidenced. Local Linux PASS does not complete AC-001-01/05 for
web/mobile/full infrastructure or independent developer onboarding.
