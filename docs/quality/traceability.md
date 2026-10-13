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
