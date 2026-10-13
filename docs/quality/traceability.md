# Traceability Matrix (living document)

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
