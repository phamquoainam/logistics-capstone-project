# AC-001-01 — Formal cold-start acceptance and open findings

## Owner decision and scope

- Acceptance ID: **AC-001-01**.
- Formal decision: **DONE / PASS_WITH_DOCUMENTED_FINDINGS**.
- Owner approval date: **2026-10-18**.
- Authority: the project owner's explicit instruction, **Record Formal Owner Acceptance and Open Findings**, following the final evidence review recommendation `RECOMMEND_PASS_WITH_DOCUMENTED_FINDINGS`.
- Integrated develop SHA: `242e3d31192fb493938b09b398b4cd1ddd29a83b`.
- Reviewed gate reconciliation: **24 PASS / 0 FAIL / 1 UNVERIFIED** out of the original 25 gates. Broad raw-artifact privacy assurance remains UNVERIFIED.
- Feature 001, D1-08 and Day 1 remain **PARTIAL**. **AC-001-04 and AC-001-05 remain independently unaccepted**.

This record accepts only the literal cold-start scope. It does not declare historical failures, security findings or intermittent reliability risks FIXED. Acceptance of the disclosed verification deviations below is not an amendment to any original requirement or acceptance criterion. The current documentation checkpoint introduces no new test execution or production change.

## Literal acceptance criterion

The following is copied verbatim from [Feature 001 specification](../../specs/001-platform-foundation/spec.md#6-tiêu-chí-nghiệm-thu-kiểm-chứng-được):

**AC-001-01 — cold start:** Given máy dev có Docker/JDK/Node baseline đã chốt, When setup từ clean clone theo runbook, Then PostgreSQL/Redis/RabbitMQ lên, migration chạy, backend health/readiness đúng, web build được và native mobile spike có log kết quả.

The [design](../../specs/001-platform-foundation/design.md), [tasks](../../specs/001-platform-foundation/tasks.md), [ADR-0007](../architecture/decisions/ADR-0007-isolated-verification-bootstrap.md) and [clean-clone runbook](../runbooks/clean-clone-verification.md) supply execution and isolation requirements. The SDK sequence and newly built APK/device proof required by that runbook are included below. No requirement demands a single failure-free attempt, empty dependency caches, empty application data, offline standalone operation, or zero historical health warnings. This conclusion does not waive future task-specific requirements.

## Integrated baseline and provenance

[PR #16](https://github.com/phamquoainam/logistics-capstone-project/pull/16) merged at `2026-10-17T20:39:23Z` as the integrated SHA above. Parents are `82c2ddcf21363b95efeff49f62cd60fa11e03c0a` and `091b458964358176d82b5f9c731c4d591efb79a7`. The reviewed merge tree equals the feature tree; the merge inventory contains the 12 authorized remediation files.

Actual develop **push** [run 39067632758, attempt 1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39067632758) completed SUCCESS with all three jobs checking out that merge SHA. This is post-merge evidence, not substituted PR CI.

| Required check | Tests passed | Expected skips | Report/privacy guard tests |
| --- | ---: | ---: | ---: |
| Architecture gate | 23 | 0 | 25 PASS |
| Backend default / H2 | 48 | 17 | 25 PASS |
| Backend PostgreSQL 18 | 73 | 5 | 25 PASS |

Hosted coverage is backend-only. It does not claim hosted SDK/web/mobile verification or execution of the five Redis/RabbitMQ foundation tests skipped in the PostgreSQL job.

Fresh acceptance clone identity: `ac-001-01-fresh-20261018-8bc27791`, direct remote clone at the integrated SHA, 420 tracked files, no copied ignored outputs. Both repositories were clean on develop before this documentation branch. All 420 tracked byte hashes matched the native pre-execution snapshot when rechecked for this record. Existing machine tools and caches were used; this is not independent-developer or empty-cache evidence.

## Evidence register

E01–E08 below refer to retained local evidence, not public GitHub artifacts. The roots and relative suffixes are local locators only; private paths must not be converted into public links or uploaded with this document.

- Root R: `D:\TLCN\e-logistic`.
- Root C: `D:\TLCN\verification\ac-001-01-fresh-20261018-8bc27791\repo`.
- Root N: `R\.demo\native-controlled-20261018`.

| Evidence ID | Local locator / source | Purpose |
| --- | --- | --- |
| E01 | `R\.demo\ac-001-01-postmerge-acceptance-20261018\`: `merge.json`, `location.json`, `run-39067632758.json`, `postmerge-verdict.json` and retained CI artifacts | Merge/clone provenance, actual push CI SHA, inventories and guards |
| E02 | `C\.demo\acceptance\`: `acceptance-matrix-resumed-final.json`, `assessment-resumed.md`, individual command results, sanitized architecture/H2/PostgreSQL reports, `infrastructure-results.json` | Original 25-gate checkpoint, command/exit evidence, clean installs, migrations/roles/probes and regressions |
| E03 | `R\.demo\node24-launcher-evidence-20261018\`: `report.md`, `C-result.json`, `E-result.json`, `verified-test-totals.json`, `preservation-final.json` | Uninstrumented and traced complete SDK aggregates, independent totals and preservation |
| E04 | `R\.demo\node24-final-aggregate-20261018\`: `report.md`, `summary.json`, `independent-integrity.json`, `attempt-*/findings.json`, sanitized process trees and privacy audit | Two bounded instrumented aggregates, subprocess exit propagation and limitations |
| E05 | `C\.demo\acceptance\rabbit-resume-results.json` and retained private health snapshots | Current restart/recreate/authentication/persistence and historical health-history finding |
| E06 | Root N: `report.md`, `summary.json`, `apk-verification.json`, `installation.json`, `runtime-reviewed.json`, `runtime-wrapper-result.json`, `cng-integrity.json`, `tracked-before.json`, `manifest.json` | New APK provenance, install/render/Metro, preserved wrapper failure and source integrity |
| E07 | Root N: `privacy-audit.json`, `privacy-reviewed.json` | Raw scan FAIL versus seven shareable summaries PASS; raw origin unknown |
| E08 | `C\.demo\acceptance\`: `final-owned-cleanup.json`, cleanup operations and foreign before/after inventories | Owned absence/idempotence and foreign metadata preservation |

The earlier [Windows remediation record](ac-001-01-windows-remediation.md) documents the approved correction and earlier Rabbit lifecycle session; it is historical evidence, not a replacement for this integrated-clone run. The final owner-review package is the preceding review response; this document records its owner-approved result. Original raw logs, failed results and earlier matrices were not overwritten. Selected native record hashes match their retained manifest; SDK failure log/result and the original matrix/assessment retain their recorded hashes and mtimes. No raw diagnostic payload is reproduced here.

## Requirement-to-evidence reconciliation

All rows refer to AC-001-01 at the integrated SHA and fresh clone above. The clauses below are not new requirement IDs.

| Literal clause | Required proof and actual execution | Result | Evidence / limits |
| --- | --- | --- | --- |
| máy dev có Docker/JDK/Node baseline đã chốt | Java21.0.10, Wrapper3.10.0, Node24.13.0/npm11.6.2; Docker Linux engine/Compose and recorded preflight | PASS | E02; existing host tools/caches |
| setup từ clean clone theo runbook | Direct remote clone, correct SHA, 420 tracked files, no copied outputs; isolated synthetic settings and session | PASS | E01/E02; not AC-001-05 |
| PostgreSQL lên | Owned PostgreSQL18 with random loopback binding, isolated database/roles and authenticated TCP | PASS | E02; no developer database used |
| Redis lên | Owned Redis with authenticated PING and anonymous/write denial | PASS | E02; approved ephemeral policy |
| RabbitMQ lên | Owned broker, authenticated AMQP, dedicated vhost/non-admin permissions, current healthy state | PASS | E02/E05; historical health-history finding OPEN |
| migration chạy | Empty-database V1/V2, validate/rerun/checksums, owner/migrator/runtime boundaries and runtime DDL denial | PASS | E02; migrations unchanged |
| backend health/readiness đúng | Full demo,postgres,foundation startup; liveness/readiness, active AMQP, API/auth; re-inspected bindings after restart/recreate | PASS | E02/E05; no universal latency SLA claim |
| web build được | Lockfile clean install, lint/typecheck, 24 tests and build; seven real Edge journeys additionally verified | PASS | E02; initial discovery failure retained |
| native mobile spike có log kết quả | Newly built APK, verified installed hash, Pixel7 starter render and Metro-connected bounded runtime | PASS | E06; data-preserving update, parser deviation below |

## Original 25-gate reconciliation

Original snapshot E02: **22 PASS / 1 FAIL / 1 PARTIAL / 1 UNVERIFIED**. Its results remain historical facts. The current review is **24 PASS / 0 FAIL / 1 UNVERIFIED / 0 NOT_APPLICABLE**. These are gate counts, not summed test counts. A successful later execution is recorded separately from the earlier failure; no historical exit is rewritten.

| # | Original gate ID | Original status | Reviewed status | Evidence / actual scope |
| ---: | --- | --- | --- | --- |
| 1 | merge | PASS | PASS | E01; PR16, parents and inventory |
| 2 | develop-ci | PASS | PASS | E01; actual develop push run |
| 3 | clone/sync | PASS | PASS | E01/E02; remote clone at merge SHA |
| 4 | environment | PASS | PASS | E02; approved tools and preflight |
| 5 | safety | PASS | PASS | E02; 43+7+25+2 = 77 tests, no failure/skip |
| 6 | backend-architecture | PASS | PASS | E02; 23 PASS/0 skips |
| 7 | backend-h2 | PASS | PASS | E02; 48 PASS/17 expected skips |
| 8 | backend-postgres | PASS | PASS | E02; 73 PASS/5 expected skips |
| 9 | PostgreSQL/Flyway/full-profile/API | PASS | PASS | E02; nine controlled runtime gates |
| 10 | SDK-install | PASS | PASS | E02; lockfile clean install |
| 11 | SDK-aggregate | FAIL | PASS | E03/E04; complete current aggregates; original libuv FAIL retained |
| 12 | contract-inventory | PASS | PASS | E02/E03/E04; exactly 11 operations |
| 13 | contract-tests | PASS | PASS | E02/E03/E04; 41 PASS/0 skips |
| 14 | determinism | PASS | PASS | E02/E03/E04; two generations/source byte-exact, 16 files |
| 15 | SDK-typecheck | PASS | PASS | E02/E03/E04; exit0 |
| 16 | SDK-build | PASS | PASS | E02/E03/E04; exit0 |
| 17 | SDK-tests | PASS | PASS | E02/E03/E04; 44 PASS/0 skips |
| 18 | web | PASS | PASS | E02; install/quality/build, 24 tests |
| 19 | HTTP-wire | PASS | PASS | E02; 27 tests, no failure/skip |
| 20 | browser | PASS | PASS | E02; seven actual Edge journeys |
| 21 | mobile | PASS | PASS | E02; clean tooling/Jest1/Hermes580 modules, 1,431,441 bytes |
| 22 | Rabbit-lifecycle | PARTIAL | PASS | E05; current restart/recreate/auth/persistence; old health-history cause OPEN |
| 23 | cleanup | PASS | PASS | E08; all captured resources absent, idempotence and foreign metadata |
| 24 | native | UNVERIFIED | PASS | E06; actual new APK/device/runtime evidence, wrapper deviation accepted |
| 25 | privacy/preservation | PASS | UNVERIFIED | E06/E07; source/preservation and scoped summaries PASS; broad raw-artifact privacy assurance UNVERIFIED |

The PostgreSQL report inventory includes seven `PostgresTestcontainersTests`, six `TestcontainersRuntimePolicyTests` and four `PostgresHealthSafetyTests`, all executed without skips. Testcontainers uses real PostgreSQL; H2 is not substituted for those gates. Expected profile skips are not missing mandatory tests.

Gate25's broader assurance is not established and is not silently promoted to PASS. This does not negate the proven literal cold-start clauses: the affected raw artifacts remain private and unapproved for publication. Independent AC-001-04 security/config acceptance remains open.

## Accepted deviations and unresolved reliability findings

The owner accepted the following disclosed deviations for this scope; no historical failure is erased and no underlying reliability defect is declared fixed.

1. **Node/libuv:** the original `npm run verify:c02` exited1 at contract lint with a Windows closing-handle assertion. Root cause remains **UNDETERMINED / OPEN**. Later complete aggregates passed: spike C uninstrumented22.052s; spike E traced15.313s; final two instrumented18.604s/17.952s. Each complete run included contract41, SDK44, inventory11, deterministic16 files and typecheck/build. Instrumentation adds preload/report options and synchronous trace writes, so timing is not automatically equivalent. The uninstrumented complete PASS supports execution acceptance; warm-cache/order/scheduling differences remain unisolated. Failed diagnostic preload/smoke setup attempts are retained separately and are not evidence of a libuv recurrence. No indefinite reproduction or runtime/version workaround is approved by this record. See F-AC01-02.
2. **RabbitMQ:** a stricter historical-health assertion failed after restart; the exact nonzero entry rotated before capture. Its cause remains **OPEN**, with no invented benign-startup/EACCES explanation. Current healthy/authentication, restart/recreate, same owned volume/cookie and persisted metadata passed. Cookie/server identity999:999, mode0400, no observed EACCES in the current evidence. No authentication/permission relaxation. See F-AC01-03.
3. **Native wrapper:** original verification wrapper **exit1** remains a failed execution. Its IPv4 substring comparison missed IPv4-mapped IPv6 endpoints. Separate review of existing captured logs normalized the address and found five established connections to the expected Metro host/port; hierarchy/screenshot/Running main and stable PID independently support startup/render. No runtime rerun was used to manufacture a green wrapper result. See F-AC01-04.
4. **Installation scope:** a newly built binary was installed with `adb install -r`, preserving existing application data. It is not an empty-data fresh install. Offline standalone operation was not verified and was not required by literal AC-001-01. No AVD/app-data wipe was performed.
5. **PostgreSQL harness:** the initial private-runner port mismatch failed; the corrected invocation used the unchanged committed CI fixture and passed. Tests, ownership guards and migration behavior were not altered. See F-AC01-06.
6. **Edge discovery:** initial browser launch failures are retained; the corrected child-runner environment restored the Windows known-folder `PROGRAMFILES(X86)` value. Browser channel, tests and installed browser were unchanged. See F-AC01-05.
7. **Native warnings:** ReactHost focus-before-context soft exception and DevLauncher colorScheme reflection warning remain nonfatal observations, not resolved defects. See F-AC01-07.

## Native artifact and observation boundary

Controlled prebuild exited0 in16.410s; Gradle debug build exited0 in383.759s. The APK was absent before this build and produced from the integrated fresh clone; existing Gradle cache was reused in place. Expected CNG script side effects were checked and restored to exact source bytes without Git reset/checkout or dependency changes.

- Package: `com.elogistic.shipper.dev`; debug/x86_64, version1.0.0/code1.
- APK size: **63,488,363 bytes**.
- SHA-256: `8538dc18f2ae67c600bcaa98604cc0543366dada7aa0e274a5fb939decf94855`.
- Signature verified; installed device APK hash equals this new artifact.
- Pixel_7/API37, actual starter render, fresh-clone Metro708 modules; main process stable30s with no observed app fatal/ANR/JS fatal in the captured PID-filtered window.

This is a Metro-connected native development-client spike. It is not release/offline/iOS/all-device verification, proof of indefinite crash freedom, or exhaustive application-data integrity. Owned Metro was stopped afterward; emulator/data were preserved. The original wrapper failure remains in E06.

## Security, privacy and preservation

The exact distinction approved by the owner is retained:

- **Raw compile-cache privacy scan: FAIL**, due to a private-key header finding (E07; 3,201 scanned payloads).
- **Complete PEM private key: not established.**
- **Known credential match: not detected in the reported scoped scan.**
- **Seven sanitized shareable summaries: PASS.**
- **Origin of the private-key header: UNDETERMINED.**
- **Raw diagnostic artifacts: PRIVATE / NOT APPROVED FOR PUBLICATION.**

The seven scoped summaries are `report.md`, `summary.json`, `acceptance-matrix.json`, `runtime-reviewed.json`, `runtime-wrapper-result.json`, `apk-verification.json` and `installation.json` under root N. Their scan result is not a claim that all project artifacts are free of secrets. No raw-cache content, credential, cookie, dump, screenshot, APK or raw log is included in this tracked record. Source attribution and disposition are required under F-AC01-01; that finding is not FIXED. Sensitive Node-report retention review remains **PROPOSED for2026-10-25, subject to owner approval**; no automatic evidence deletion.

Recorded owned cleanup/idempotence passed. Foreign Docker metadata matched **4 containers / 5 volumes / 4 networks** before/after; no developer resources were targeted. AVD was not wiped, foreign package inventory was preserved, and the binary update retained application data. Matching metadata does not prove complete database/application-data content integrity. No evidence deletion or unauthorized package/data removal is part of this checkpoint. Private `.demo` artifacts remain ignored and must not be staged or published.

## Follow-up register

These are local documentation finding IDs, **not externally created issues**. Responsible areas and priorities are recommendations; no individual is assigned. P1 means prioritize the next relevant closure/security work; P2 means bounded follow-up; P3 means monitor recurrence. None is an unresolved blocker of the owner-accepted literal AC-001-01. AC-001-04/05 retain their own acceptance obligations.

| ID | Status / classification | Recommended priority and responsible area | Required follow-up / closure proof |
| --- | --- | --- | --- |
| F-AC01-01 | OPEN / UNVERIFIED_REQUIRING_MORE_EVIDENCE | P1; security + Node/build tooling | Attribute the raw compile-cache header to its source, assess actual credential/key material and record disposition. Keep raw evidence private; decide retention separately. No blanket clean-artifact claim. E07. |
| F-AC01-02 | OPEN / NON_BLOCKING_OPEN_RISK | P2; platform/tooling | Keep intermittent Node/libuv assertion and unknown root cause tracked. On recurrence, capture authorized bounded process/exit/native diagnostics; require cause-specific regression before claiming FIXED. E03/E04. |
| F-AC01-03 | OPEN / NON_BLOCKING_OPEN_RISK | P2; infrastructure/verification | Preserve the missing-entry limitation; capture nonzero Rabbit health output before rotation in a future authorized run and disposition the actual cause. No speculative root cause. E05. |
| F-AC01-04 | OPEN; acceptance deviation APPROVED | P2; verification tooling | Correct endpoint parser for IPv4 and IPv4-mapped IPv6; positive/negative tests must reject wrong host/port. Retain original exit1 and independent runtime review. E06. |
| F-AC01-05 | OPEN; acceptance deviation APPROVED | P2; Windows/browser tooling | Document/reproduce the minimal child-only Edge known-folder environment; reject inherited connection/config redirects and keep existing browser assertions. E02. |
| F-AC01-06 | OPEN; acceptance deviation APPROVED | P2; database test tooling/docs | Document exact committed PostgreSQL harness invocation/port contract and cleanup prerequisites; prove the documented invocation preserves ownership guards and exact test inventory. E02. |
| F-AC01-07 | OPEN / NON_BLOCKING_OPEN_RISK | P3; mobile/Expo integration | If ReactHost/DevLauncher warnings recur, record process/time/impact and bounded reproduction before proposing a compatible fix. Do not suppress warnings or upgrade automatically. E06. |
| F-AC01-08 | OPEN; AC-001-04 independently unaccepted | P1; security/configuration | Complete scoped secret/config/fail-fast acceptance with explicit command/output coverage and dependency-advisory disposition. Include F-AC01-01; cold-start approval is not security acceptance. |
| F-AC01-09 | OPEN; AC-001-05 independently unaccepted | P2; developer experience + independent reviewer | Another developer follows the published environment/runbook and records commands/versions/smoke outcomes; no reuse of this operator's clone as independent proof. |

## Remaining Feature 001 work and documentation boundary

AC-001-01 is DONE with the documented findings above. **AC-001-04, AC-001-05, Feature 001, D1-08 and Day 1 are not made DONE by this decision.** Full-feature quality/task reconciliation, security/dependency disposition and contract/SDK/web/mobile/Redis-Rabbit hosted coverage assessment remain separate work; configured backend checks are not evidence of unexecuted hosted suites. AC-001-02/03 and other task statuses are not changed by this record.

The [Day 1 plan](day-1-plan.md), [traceability](../quality/traceability.md), [Feature 001 tasks](../../specs/001-platform-foundation/tasks.md) and runbook point to this current owner decision. Earlier failure/blocked checkpoint text remains historical, including the original 25-gate file. This documentation-only checkpoint runs documentation validation only: no application test rerun, native build, Docker/device mutation, dependency/source/workflow change, evidence deletion, commit, push or PR is authorized here.
