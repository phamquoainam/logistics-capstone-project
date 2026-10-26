# Implementation Tasks 005: Tạo, xác nhận, hủy và tracking vận đơn

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-005-01 → T-005-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-005-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Chính sách fee refund khi hủy, allowed cancel state chính xác theo flow; shipment code format; các bắt buộc tối thiểu khi tạo draft..  
  - Phụ thuộc: specs/001, specs/002, specs/003, specs/004, specs/006, specs/007.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `POST /api/v1/shipments (DRAFT); POST /api/v1/shipments/{id}/confirm; POST /api/v1/shipments/{id}/cancel; GET /api/v1/shipments/{id}; GET /api/v1/public/tracking/{code}.`.  
  - Phụ thuộc: T-005-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `order.shipment, parcel, addresses, quote_snapshot, tracking_event, idempotency record; dependencies referenced via public IDs.`.  
  - Phụ thuộc: T-005-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-01, BR-02, BR-03, BR-05, BR-12, BR-13, BR-19.  
  - Phụ thuộc: T-005-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Single local transaction across workflow→Pricing(validate), Warehouse(reserve if WMS), Finance(charge), Order(confirm+snapshot+tracking); outside integrations pre-resolved.`.  
  - Phụ thuộc: T-005-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-005-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-005-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: ShipmentConfirmed/ShipmentCancelled/ShipmentStatusChanged in source outbox; notification asynchronously..  
  - Phụ thuộc: T-005-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-09 — Quality**  
  - Thực hiện: Chạy: State-transition matrix, Testcontainers confirm insufficient funds/stock rollback, concurrent same shipment, replay, tenant auth, E2E with pricing and finance.  
  - Phụ thuộc: T-005-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-005-01..05; AC-005-01..05.
- [ ] **T-005-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-005-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-005-01..05; AC-005-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L006 order-shipment-parcel-contract](../../specs/planning/v3/L006-order-shipment-parcel-contract/spec.md); [L007 shipment-booking-sender-payments](../../specs/planning/v3/L007-shipment-booking-sender-payments/spec.md); [L009 tracking-event-core-public-lookup](../../specs/planning/v3/L009-tracking-event-core-public-lookup/spec.md); [L015 lastmile-attempt-pod](../../specs/planning/v3/L015-lastmile-attempt-pod/spec.md); [L016 returns-reverse-logistics](../../specs/planning/v3/L016-returns-reverse-logistics/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Review task đầu tiên chưa tick giữ mở, chốt D-V3-04; DEC-09; DEC-10; D-V3-04; DEC-09; DEC-10; DEC-11; DEC-12; D-V3-06; DEC-20; DEC-09; DEC-16; D-V3-05; DEC-15; DEC-16; D-V3-04. Một authorization capability gồm contract+migration+BE/HTTP+FE+tests/CI/evidence, không checkpoints routine theo tầng; dependencies đối chiếu DAG trước code.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
