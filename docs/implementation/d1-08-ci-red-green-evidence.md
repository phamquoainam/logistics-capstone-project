# D1-08 Task D — controlled architecture CI red-then-fixed

Execution: 2026-10-17, Asia/Saigon. Status: RED_VERIFIED / GREEN_PENDING.
AC-001-03 remains PARTIAL until actual hosted GREEN is verified.
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
different from baseline. Hosted GREEN is pending at this checkpoint.

Expected baseline: Architecture 23/0 skips, H2 48/17 skips, PostgreSQL 73/5 skips
and 25 guard tests per job. Five Redis/RabbitMQ tests remain expected skips; full
infrastructure CI is not claimed. A documentation checkpoint will record actual
corrective SHA, GREEN run, exact inventory and artifact digests after verification.

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
