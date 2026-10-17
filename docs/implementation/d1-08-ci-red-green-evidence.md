# D1-08 Task D — controlled architecture CI red-then-fixed

Execution: 2026-10-17, Asia/Saigon. Status: DONE for the approved Task D experiment.
AC-001-03: PASS, actual paired hosted RED and GREEN verified.
Feature 001, D1-08 and Day 1 remain PARTIAL; original REQ/AC are unchanged.

## Authorization and baseline

The owner explicitly authorized an isolated experiment branch, one Draft PR,
temporary test-only violation, normal commits/pushes and sanitized evidence.
No merge, ready-for-review transition, rule/config bypass or production change.

- Clean local/live develop baseline: `f8ba871c4f0abd13c67687ec82e36583ab589e03`.
- Branch: `experiment/ac-001-03-architecture-red-green`.
- [Draft PR #13](https://github.com/phamquoainam/logistics-capstone-project/pull/13): DO NOT MERGE.
- Task A/B/C and D1-07 are scoped DONE per approved integrated context.
- This test-scope fixture matches the literal AC-001-03 test-class requirement.

## Actual RED

- Commit: `73dac74df8736fd48ce43c18cc61b63c40658448`.
- [Hosted run 39006911313](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39006911313), attempt 1, pull_request, FAILURE.
- Synthetic PR checkout: `b11f0e41765c9d178e8afa97f13578a321d54e4e`; base is the baseline above, head is the red commit.
- Test: `ArchitectureNegativeFixtureTests.financeCannotUseOrderInternalRepository`.
- Temporary Finance fixture: `finance.internal.application.ActualCiForbiddenDependency`.
- Forbidden field type: `orders.internal.infrastructure.ShipmentRepository`.
- The existing method directly invokes unchanged production `ArchitectureRules.MODULE_BOUNDARIES.check(...)`.
- No manual assertion failure, simulated exit code, compilation failure or rule change.

Actual allowlisted diagnostic, observed in all three jobs:

```text
Forbidden cross-module implementation dependency: com.elogistic.backend.finance.internal.application.ActualCiForbiddenDependency -> com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository; Field <com.elogistic.backend.finance.internal.application.ActualCiForbiddenDependency.forbidden> has type <com.elogistic.backend.orders.internal.infrastructure.ShipmentRepository> in (ActualCiForbiddenDependency.java:0)
```

| Job | PASS | FAIL | SKIP | Guards | Artifact ID |
|---|---:|---:|---:|---:|---|
| Architecture gate | 22 | 1 | 0 | 25 PASS | 12090764716 |
| Backend default / H2 | 47 | 1 | 17 | 25 PASS | 12090884612 |
| Backend PostgreSQL 18 | 72 | 1 | 5 | 25 PASS | 12091526140 |

Each actual Maven exit and workflow command exit was 1. Java/bootstrap,
compilation, test discovery, artifact upload and PG preparation/cleanup worked.
Each report has only the intended failing method, no errors or missing tests.
Guard problems are the failed required test and nonzero Maven exit; no parser
error. PostgreSQL credential and owned Testcontainers cleanup audits passed.

## Correction and GREEN verification

The corrective commit deletes the temporary fixture and restores the original
negative-test method and CI reporter implementation exactly to baseline. It does
not amend, remove or rewrite the RED commit. Only this new evidence file remains
different from baseline at the corrective checkpoint.

Corrective commit: `c52819fe839634ee755fb37d8633542ae01eb6f2`.
[Hosted GREEN run 39007163856](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39007163856),
attempt 1, pull_request, SUCCESS. Synthetic checkout:
`97b9b70ae502f6f336b047d0015380013f3868a3`.

| Required job | PASS | FAIL/ERROR | SKIP | Guards | Artifact ID |
|---|---:|---:|---:|---:|---|
| Architecture gate | 23 | 0 | 0 | 25 PASS | 12091407845 |
| Backend default / H2 | 48 | 0 | 17 | 25 PASS | 12091054519 |
| Backend PostgreSQL 18 | 73 | 0 | 5 | 25 PASS | 12090469435 |

Exact class/method/status inventory matched committed expected suites, with no
missing/extra/duplicate tests. The corrected method passed. PostgreSQL includes
7 real Testcontainers/Flyway tests, 6 policy tests, 4 Task A JDBC safety tests and
8 existing PostgreSQL foundation tests. The 12 architecture fixtures comprise
11 forbidden cases and 1 positive control. H2 skips 8 PG foundation + 4 PG safety
+ 5 Redis/RabbitMQ tests; PostgreSQL skips only those 5 Redis/RabbitMQ tests.
Task C is excluded before H2 discovery, not counted as new skips.

All uploads succeeded; PG prepare/cleanup, credential and owned cleanup audits
passed. No runtime/config/network/compiler/parser failure was used to create RED.
Five Redis/RabbitMQ tests remain local-only evidence, not hosted PASS.

Local checks: focused Wrapper clean verify exited 1 with 22 PASS/1 intended FAIL
before correction, then exited 0 with 23/23 PASS after correction; matching report
guard rejected RED and accepted GREEN. Python guard suite 25/25, Bash syntax and
git diff --check passed. Initial sandbox cache access failed before tests; rerun
with permitted cache access produced the actual results above. No cache deleted.

Byte comparisons and Git baseline diff confirm original test/reporter restored,
temporary fixture absent, and no production/test/script/workflow/config/pin changes
remain. Task D metadata/evidence updates are documentation only. RED history is
retained; develop is unchanged and no experiment merge is authorized.

## Ruleset and privacy

Read-only API verification: ruleset 25049940, Protect develop - Backend CI,
enforcement active, target refs/heads/develop, no bypass actors. Requires PR,
strict up-to-date checks Architecture gate, Backend default / H2 and Backend
PostgreSQL 18 (GitHub Actions app 15368), with force-push/deletion protection.
Red checks match these required contexts. Draft/blocked state alone does not
prove a merge attempt was rejected; no merge or ready-for-review attempt was made.

Temporary reporting parses only the exact failing class/method and allowlisted
dependency line from private Surefire XML. It does not alter Maven status, test
selection, report verification, workflow, artifacts or required checks. No raw
Maven logs/XML, credentials or arbitrary failure bodies are published.
Downloaded ZIP SHA-256 values matched GitHub metadata; each ZIP contained exactly
summary.json and tests.xml, with sanitized failure/skip placeholders and no
properties/system output. Bounded credential scans passed.

Sanitized remote reports, job witnesses and ruleset snapshots are retained under
ignored `.demo/d1-08-task-d/`; private local diagnostics are not staged/published.

## Limits and documentation integration

ShellCheck and deliberate JVM-kill cleanup remain UNVERIFIED; Docker default
bridge investigation and dependency/security findings remain OPEN. Ryuk socket
access remains high trust. SDK dev audit 4 HIGH, mobile full 58 / omit-dev 35,
web 0 are existing snapshots, not rescanned or fixed here. No helper-image/Maven
vulnerability certification or full onboarding/config acceptance is claimed.

Keep this PR Draft and unmerged. After separate approval, transfer only final
Markdown evidence/Task D metadata from a clean develop branch into a documentation
PR. Do not merge/cherry-pick experimental RED history into develop.

## Artifact integrity manifest

Archive SHA-256 values below were checked against GitHub artifact metadata.

| Phase | Artifact | SHA-256 |
|---|---|---|
| RED | [backend-postgres-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39006911313/artifacts/12091526140) | `4a53aa156873db3a31e2e9cd10a35eb5510623f78622ebfdc35f5b3dfe7cf7f9` |
| RED | [backend-h2-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39006911313/artifacts/12090884612) | `b10d41abd4e7f874b5112dd5db842bd443e43b04c1f819641da41ec59b39510f` |
| RED | [backend-architecture-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39006911313/artifacts/12090764716) | `fc9a5d89e1bac479e82c86d33098eb5030c27b0a6f1137294f4d113b0fb5587f` |
| GREEN | [backend-architecture-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39007163856/artifacts/12091407845) | `6b6b8ab1f6908072508f6f19b6aec2a7277ca26cea81276bda0875b3ce4c5b35` |
| GREEN | [backend-h2-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39007163856/artifacts/12091054519) | `6e628a41febecb5596f09d4e7a6aff54a5053d0f9ed86a088cc79fb37e0c2d03` |
| GREEN | [backend-postgres-1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39007163856/artifacts/12090469435) | `3fff3a2d972936f854e88ef207210488bf26fcbb54bf00026c07bc1234f382d2` |
