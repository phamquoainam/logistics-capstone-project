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
| REQ-001-03; AC-001-03; CI; clean-machine AC-001-05 | No execution in this slice | UNVERIFIED; D1-03 PARTIAL, feature 001 not DONE |
