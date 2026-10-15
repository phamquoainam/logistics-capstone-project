# Traceability Matrix (living document)

**D1-06 official DoR closure — 2026-10-15, baseline develop3186f01 / PR#7 merged.**
[Assessment and DoR](../implementation/d1-06-slice-readiness.md),
[subordinate spec](../../specs/001-platform-foundation/slices/demo-contract-client/spec.md),
[design/tests](../../specs/001-platform-foundation/slices/demo-contract-client/design.md),
[D1-07 tasks](../../specs/001-platform-foundation/slices/demo-contract-client/tasks.md).
**DEC-29 ACCEPTED** by explicit user approval, not metadata inference.
**D1-06 DONE** for assessment/selection/DoR; chosen slice READY_FOR_IMPLEMENTATION.
Source drift PASS: HEAD3186f01, eleven source-operation hashes/protected paths
unchanged; accepted contract/tooling scope and all mandatory planning gates recorded.
Consumer TS6 stays unchanged; test-only Ajv purpose, source/wire/drift/determinism
and application-only rollback preserved. **D1-07 NOT STARTED**; all implementation
test IDs below remain PLANNED/NOT RUN. Full Feature001/Day1 remain PARTIAL.

| Source/requirement subset | Accepted slice / planned test mapping | Current evidence/status |
|---|---|---|
| REQ-001-04 contract/client; NFR-M02/O03 | SC-01/02; TEST-001-CONTRACT-01/02/03/05 | Source11-operation inventory and generator metadata reviewed; actual OpenAPI/generated client absent |
| DEMO-01..04; FR-A01/A02/M01/M04/M05/AD01 subsets; NFR-U02 | SC-03; TEST-001-CONTRACT-04/05/08 | Existing web/API implementation confirmed; generated-client journey NOT RUN |
| BR-01/02 demo subset; NFR-S01/S02/C02 | SC-04; TEST-001-CONTRACT-06/07/10 | Preserve existing RBAC/owner/replay/concurrency; new wire-conformance tests NOT RUN |
| REQ-001-05; NFR-M01/S02; AC-001-04 subset | SC-05 / SC-AC-06; TEST-001-CONTRACT-09/10 | No new dependencies/source; documentation-only safety checks, not an installed-tool security claim |
| AC-001-01/05; REQ-001-04 infrastructure; Testcontainers full tasks | GAP-C03/04/05/06/09 | OPEN full001 obligations, outside this independent slice; no implicit DEFERRED |
| FR-S01..05 /009/010/015 | GAP-C02/07 | Mobile starter verified previously; business/native/API networking still NOT IMPLEMENTED/UNVERIFIED |

Full Feature001/Day1 remain PARTIAL. D1-01..05 completed task scopes are retained;
new gaps/status above distinguish literal full AC from those scopes. Prior D1-02
and older evidence below are dated checkpoints, not new execution claims.

| Closure evidence ID | Verified documentation gate | Status |
|---|---|---|
| DOC-001-D106-APPROVAL | Explicit user “DEC-29 — APPROVED / D1-06 Definition of Ready Closure”; scope/pins recorded in decision register/ADR/slice | PASS acceptance; no runtime compatibility claim |
| DOC-001-D106-DOR | Required DoR categories, source drift, source-to-contract inventory, fixtures/tasks/gates/rollback and links/protected REQ/AC checks | PASS documentation; D1-06 DONE, unpublished |

**D1-02 execution — 2026-10-15: DONE for approved local toolchain/native starter scope.**
Baseline2dcffd5, uncommitted feature/d1-02-toolchain-closure.
[Detailed commands/hashes/disposition](../implementation/d1-02-toolchain-evidence.md).

| Trace/test ID | Executed evidence | Status/limit |
|---|---|---|
| REQ-001-01/05; TEST-001-D102-PIN-01 | Node24.13.0/npm11.6.2, .nvmrc/exact engines/packageManager, engine-strict=true, lockfile clean installs | PASS; no framework major/patch upgrade |
| TEST-001-D102-WEB-01 | ESLint/typecheck/Vite build;5 API-client assertions and3 existing Edge regressions | PASS; isolated H2 in-memory backend, existing data untouched |
| TEST-001-D102-MOBILE-01 | SDK57 ESLint/Jest/RNTL, strict types, starter mount/remount, Android Hermes export | PASS; no real native behavior claimed from mocks/bundle |
| AC-001-01 native subset; TEST-001-D102-NATIVE-01 | Gradle304 tasks/APK SHA256/signature, install previously absent com.elogistic.shipper.dev on Pixel7/API37 | PASS debug/x86_64; not release/iOS/all-device coverage |
| TEST-001-D102-RUNTIME-01 | Two native cold launches, real starter UI/screenshot, PID/socket/exit-info evidence, no fatal/ANR/JS-fatal in bounded windows | PASS; upstream nonfatal colorScheme/ReactHost warnings recorded |
| TEST-001-D102-LOG-01 | Go and native PID comparison plus pre-launch UWB vendor-process evidence | PASS source/impact classification; upstream fixes/background readiness UNVERIFIED |
| TEST-001-D102-SDK-ADVISORY-01 | Online expo install --check recommends57.0.29/Metro57.0.17; frozen baseline57.0.27/57.0.16 | FAIL advisory recorded, no --fix or offline substitution |
| Dependency advisory review | npm audit graph22 baseline vs35 current omit-dev findings, zero critical; SDK/Jest deprecations | OPEN maintenance/security review; not APK reachability proof or a clean security audit |
| Full Feature001/Day1 | Full other-developer onboarding/health/contracts/infrastructure/native business and remaining DoR/quality tasks | PARTIAL, original REQ/AC unchanged |

D1-01/02/03/04/05 task scopes DONE; D1-06–08 remain open. Older task statuses
below are historical checkpoints, not an assertion of current missing tooling.

**Official post-merge closure — 2026-10-14:** D1-05 DONE for approved backend CI;
D1-03 DONE for PostgreSQL Phases A–E. PR #5 merged as
`f50c8972a3a4d0a9c877d6e7e99d2a918cd866fd`; develop push
[run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
/ attempt 1, three jobs success. [Detailed evidence](../implementation/d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).

| Trace / test ID | Verified evidence | Status / limit |
|---|---|---|
| REQ-001-03; TEST-001-CI-REMOTE-01 / ARCH-01 | Architecture 23 executed/pass, zero skip; exact production/fixture inventory | PASS for implemented code; six absent business modules N/A |
| EC/AC-001-03; TEST-001-CI-NEG-01 | 11 fixtures detect dependency/cycle/direction and assert production rule throws; positive actor control PASS | PASS detection on CI; deliberately red-then-fixed production workflow experiment UNVERIFIED, not performed/claimed |
| TEST-001-CI-DISC-01 / REG-01 | Guard 14 per job; H2 48 discovered/40 pass/eight named PG-only skips; PG48/no skip | PASS exact inventory and fail-closed report policy |
| REQ-001-02; TEST-001-CI-PG-01 | Isolated PG18 V1/V2, ownership/permission/validate/rerun/upgrade/rollback/concurrency | PASS; historical named-volume persistence/web/H2 proof retained; not Testcontainers implementation |
| REQ-001-05 subset; TEST-001-CI-SEC-01 | Three ZIP SHA256 digests, JSON/XML allowlists, credential audit and selected log token patterns | PASS bounded scope; no full-history/provider audit claim |
| TEST-001-CI-ENFORCE-01 | Active ruleset25049940; effective rules/develop; three contexts/app15368; strict policy/no bypass | PASS configuration enforcement; no deliberate blocked-merge experiment |
| TEST-001-CI-MERGE-01 | PR API merged=true; f50c897; push run matches merge/current develop | PASS integration; new docs branch uncommitted/unpublished |
| D1-03 approved task DoD | Original Phases A–E, local persistence/web/H2 proof, hosted backend run | DONE; native/tooling/full onboarding belong to D1-02/Feature001 |
| Full Feature001 / Day1; AC-001-01/05 | Contracts/client/health/full infrastructure/native/onboarding and broader quality still open | PARTIAL; no criteria reduced or implicitly deferred |

Earlier entries below retain historical scope. The table above defines current
CI/enforcement/merge/task status; old UNVERIFIED/PARTIAL notes are not current blockers.

**Nguồn:** [REPORT] Chương 3 §3.7. Tạo ma trận trong triển khai, trạng thái ban đầu tất cả `NOT_IMPLEMENTED`.

| Level | Mapping bắt buộc |
| --- | --- |
| Business source | Chương/mục báo cáo và mã BR, FR, NFR, UC |
| Feature | `specs/<id>/spec.md` `REQ-<id>-NN` |
| Acceptance | `AC-<id>-NN` mô tả Given/When/Then |
| Design | `specs/<id>/design.md` API/table/event/transaction |
| Task | `T-<id>-NN` trong `tasks.md` |
| Test | `TEST-<id>-UNIT/INT/E2E/CONC/FAULT-NN` |
| Evidence | Result + commit + environment + dataset + timestamp |

## Regeln / consistency
- Không đánh dấu DONE bằng nhận xét tay nếu chưa có test hoặc bằng chứng kiểm tra thủ công được ghi rõ.
- Không khai số liệu performance trước benchmark thật.
- BR-01–21 và toàn bộ NFR phải có coverage ít nhất một dòng trước nghiệm thu P0.
- Bất kỳ change tác động đến contract phải được liên kết đến OpenAPI/event schema và web/mobile generated client.

## Trạng thái khởi tạo
- Toàn bộ feature: `DRAFT_SPEC` / `NOT_IMPLEMENTED`.
- Tất cả test/evidence: `NOT_RUN`.

## Demo 001 — evidence bổ sung 13/10/2026

Mốc khởi tạo trên được giữ làm lịch sử; bảng này chỉ chứng minh demo scope do người dùng duyệt, không hoàn thành feature 001/002/003/005 hoặc thay AC gốc.

| Demo REQ / source subset | Implementation | Test / evidence | Status |
|---|---|---|---|
| DEMO-01; REQ-002-01/02/03/05, FR-A01/A02 | identity signed short-lived token, BCrypt, account status/role lookup, throttling | DemoAuthenticationTests (7); browser invalid login/admin | PASS demo; session/revocation/multi-scope P0 pending |
| DEMO-02; REQ-003-01 profile subset; FR-M01 | merchant read-only own organization | DemoShipmentTests.ownDraftAndCrossMerchantIsolation | PASS demo; pickup CRUD/catalog/geocoding pending |
| DEMO-02; REQ-005-01/03/04/05 subsets; BR-01/02/03 | owned DRAFT create/list/detail/cancel, durable operation receipts, events | DemoShipmentTests (6), concurrentCreateHasOneEffect; Edge A/B isolation | PASS demo; CONFIRMED/Finance/WMS/shipping pending |
| DEMO-03; REQ-002-04 subset | sanitized admin overview, no Finance mutation | anonymousUnauthorizedAndWrongRoleForbidden, admin browser test | PASS demo; role administration/audit pending |
| DEMO-04; FR-M04/M05 | React portal with real APIs, retained failed forms | 3 Playwright tests + typecheck/build | PASS demo |
| DEMO-05; REQ-001-02 persistence subset | isolated H2 file, Flyway demo V1 | smoke-demo.py create → backend restart → verify | PASS H2 demo; PostgreSQL foundation UNVERIFIED |

Environment/commands/artifacts: [demo evidence](../implementation/demo-001-evidence.md). No commit yet. Source contract: [demo design](../../specs/demo-001-auth-merchant-shipment/design.md). Original feature tasks remain unchanged.

## D1-03 PostgreSQL milestone — 2026-10-13

Historical D1-03 records below retain their execution scope. Latest architecture
and closure assessment: [D1-04 evidence](../implementation/d1-04-architecture-evidence.md).

Demo-001 baseline was merged in 08c0c34 before this work. DEC-26 approved for this scope; current branch changes uncommitted. Original AC remain intact. [Execution evidence](../implementation/d1-03-postgresql-evidence.md).

| Trace / test ID | Executable evidence | Status / scope |
|---|---|---|
| REQ-001-02; TEST-001-INT-PG-01 | Migration job cold-start; PostgresFoundationTests.nineSchemasAndTablesBelongToOwner / actualPostgresAndSeparateMigrationPrincipal | PASS nine schemas/role separation on PG18; only approved demo tables |
| REQ-001-02; AC-001-02 subset; TEST-001-INT-PG-02 | validateAndRerunPreserveDataAndChecksums; versionOneToTwoUpgradePreservesExistingTechnicalData | PASS checksum/validate/rerun/V1→V2 technical-data upgrade; full P0 upgrade not claimed |
| REQ-001-02; TEST-001-INT-PG-03 | runtimeCannotDdlAssumeOwnerOrReadHistory; eventsAndReceiptsCannotBeChangedByRuntime | PASS runtime no DDL/history/owner and event/receipt immutability |
| BR-01/02; DEMO-01/02/03 | DemoAuthenticationTests (7), DemoShipmentTests (6) on PostgreSQL test DB | PASS demo auth/ownership/guards/conflicts/admin; P0 feature gates stay open |
| BR-02; TEST-001-CONC-PG-01 | concurrentCreateHasOneEffect; concurrentCancelHasOneEventAndReceipt | PASS one effect/event/receipt under two concurrent requests |
| Local ACID; TEST-001-INT-PG-04 | failedForeignKeyRollsBackWholeTransaction | PASS invalid FK rolls back earlier write on PostgreSQL |
| DEMO-05; AC-001-02 restart subset; TEST-001-E2E-PG-01 | smoke-demo.py create/verify --database postgres + container/backend restart + direct SQL | PASS named-volume persistence, same shipment/timestamp/event/receipt |
| DEMO-04; TEST-001-E2E-PG-02 | Web typecheck/build + three Edge Playwright tests before/after restart | PASS actual PostgreSQL APIs/render/negative/retry |
| Existing H2 demo compatibility | 14 original tests; original H2 runtime/proof at 8082 | PASS unchanged applied V1 and original persistent shipment |
| REQ-001-03; AC-001-03; CI; clean-machine AC-001-05 | No execution in the D1-03 slice; local architecture subsequently verified in D1-04 below | CI/reproduction UNVERIFIED; D1-03 PARTIAL, feature 001 not DONE |

## D1-04 — Architecture verification, 2026-10-13/14

Clean merged baseline `ccafeb4c339ffdfeb23181c245440b61176c2c31`; branch
`feature/d1-04-architecture-verification`, uncommitted changes. User-approved
minimal trusted actor and ArchUnit integration: [ADR-0004](../architecture/decisions/ADR-0004-architecture-verification.md).
[Actual scopes, commands, fixtures and reports](../implementation/d1-04-architecture-evidence.md).
Original REQ/AC and unchecked full-feature tasks are unchanged.

| Trace / test ID | Executable test/evidence | Status / limit |
|---|---|---|
| REQ-001-03; AC-001-03 local subset; TEST-001-ARCH-01 | productionScopeIsRealAndExcludesNegativeFixtures, packagesMatchApprovedModuleStructure, modulesOnlyUsePublicApis | PASS 37 real classes/four packages; six missing business modules N/A, not verified |
| REQ-001-03; EC-001-03; TEST-001-ARCH-02 | financeCannotUseOrderInternalRepository: same MODULE_BOUNDARIES evaluate + check throws AssertionError | PASS forbidden finance→Orders repository detected, exact dependency in failure report; no violation in production |
| DDD ownership; TEST-001-ARCH-03 | publicApisDoNotLeakInternals, modulesFollowApprovedDependencyDirection + API/direction fixtures | PASS approved collaborators only; no Orders AccountView/broad Identity API or domain actor dependence |
| Modular monolith; TEST-001-ARCH-04 | moduleGraphHasNoCycles + moduleCycleIsDetected | PASS real graph acyclic; isolated finance/pricing cycle detected |
| Selective Clean Architecture; TEST-001-ARCH-05 | domain/application/web/infrastructure direction tests + matching negative fixtures; controllersDoNotOwnTransactions | PASS nonempty actual layer scopes and wrong-direction/controller-transaction detection |
| Trusted principal; BR-01; TEST-001-SEC-01 | actorMerchantOwnershipIsRecheckedFromDatabase; existing live-role/tampered/expired/disabled/forged-owner tests | PASS H2 and real PG; existing token uses DB-derived merchant scope, old owner refused |
| REQ-001-05; AC-001-04 demo subset; TEST-001-SEC-02 | credentialsAndBearerTokenDoNotAppearInApplicationLogs; signingConfigurationFailsFastWhenMissingOrWeak; bounded Git-visible/artifact audit | PASS tested credentials/log/config/artifact scope; not a full history/provider/P0 security audit |
| D1-03/Demo-001 regression; TEST-001-REG-01 | Final Maven H2 and PostgreSQL verify, browser/typecheck/build | PASS H2 40 executed/eight PG-only skip; PG 48/no skip; three Edge tests; unchanged V1/V2/Compose/business semantics |
| REQ-001-03/AC-001-03 CI requirement; AC-001-01/05 | No executable GitHub workflow or independent clean-runner execution | UNVERIFIED; local tests do not close CI/reproducibility/full feature 001 |

D1-04 local verification satisfies the approved execution checklist; **propose DONE**.
D1-03 DB milestone is complete locally; overall D1-03 stays PARTIAL due to documented
environment/reproducibility prerequisites. No implicit DEFERRED or AC reduction.

## D1-05 — CI gate, 2026-10-14

Latest baseline `develop/2c609c42863505d0e28400eaa26f36c72b062769`, clean and
remote-matched; D1-03/04 merged. User confirms D1-04 reviewer closure; approved
local D1-04 task is DONE, with earlier execution scope preserved above.
Current branch `feature/d1-05-ci-verification`, HEAD unchanged, no publish.
[Evidence/commands/hashes](../implementation/d1-05-ci-evidence.md),
[workflow](../../.github/workflows/backend-verification.yml),
[operations](ci-verification.md). Original feature REQ/AC are unchanged.

| Trace / test ID | Evidence | Current status / limit |
|---|---|---|
| REQ-001-03, EC-001-03; TEST-001-CI-ARCH-01 | Clean Linux Wrapper architecture verify: 11 production/scope + 12 isolated fixture tests | PASS local 23/no skips; 37 classes/four implemented groups; missing six business modules N/A |
| AC-001-03; TEST-001-CI-NEG-01 | Same production MODULE_BOUNDARIES detects Finance→Orders repository; fixture asserts detail/throw; cycle/direction controls execute | PASS detection locally; actual remote red-PR/enforcement UNVERIFIED |
| REQ-001-03; TEST-001-CI-DISC-01 | 14 report-guard tests plus actual Surefire reports | PASS missing/zero/skipped/duplicate/malformed/count mismatch/nonzero Maven/conflicting PG failure+skip fail closed; known parameter-name alias covered |
| REQ-001-01 / EC-001-04 backend subset; TEST-001-CI-REG-01 | Uncached Linux Wrapper default/H2 clean verify | PASS 48 discovered/40 executed/eight named PG-only conditional skips; not full PostgreSQL or web/mobile verification |
| REQ-001-02 / AC-001-02 subset; TEST-001-CI-PG-01 | Fresh pinned PG18 tmpfs container, isolated DB/upgrade DB/roles; existing V1/V2 suite | PASS local 48/zero skip; ownership/permission, migration validate/rerun/upgrade, rollback/concurrency. Restart/persistence remain original D1-03 evidence |
| REQ-001-05, EC-001-05 / AC-001-04 evidence subset; TEST-001-CI-SEC-01 | Public artifact allowlist, synthetic secret stripping test, generated PG password log/artifact audit | PASS bounded tested outputs; no real repo credentials or raw diagnostic upload, no full-history/provider audit claim |
| REQ-001-03 / AC-001-03 actual CI; TEST-001-CI-REMOTE-01 | Workflow PR→develop/push develop, actionlint/ShellCheck and SHA provenance | Configuration PASS / READY FOR REMOTE VERIFICATION; no Actions run URL/tested remote SHA, required-check settings UNVERIFIED |
| AC-001-01/05 full cold-start/reproducibility | Local fresh Linux backend only | PARTIAL; full web/mobile/Redis/RabbitMQ/health and independent developer onboarding not verified |

D1-03 local DB milestone remains PASS, overall task PARTIAL for D1-02/full
onboarding prerequisites. D1-04 approved local task DONE; D1-05 overall PARTIAL
until real hosted runner execution and required-check enforcement. A successful
fixture test proves detection, not that a deliberately violating remote PR has
been blocked. No production violation, acceptance change or implicit deferral.
