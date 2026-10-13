# Implementation Tasks 009: Shipper Mobile: lấy/giao/hoàn, POD và COD collection

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-009-01 → T-009-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-009-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Allowed POD form photo/signature/OTP and fields, file provider, max size, retry/delivery attempt limits, offline-safe mutations..  
  - Phụ thuộc: specs/001, specs/002, specs/005, specs/008, specs/006.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `GET /api/v1/shipper/tasks; POST /api/v1/shipper/tasks/{id}/start; POST /api/v1/shipper/scans; POST /api/v1/deliveries/{attemptId}/result; POST /api/v1/pods/uploads [policy-specific].`.  
  - Phụ thuộc: T-009-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `order.delivery_attempt, delivery_record, pod_metadata, parcel scan refs; dispatch.assignment; finance.cod_item / collection record; mobile offline queue.`.  
  - Phụ thuộc: T-009-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-01, BR-04, BR-05, BR-09, BR-16.  
  - Phụ thuộc: T-009-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `After external file upload, atomic server commit delivery result + tracking + COD actual collected (if applicable) through workflow; orphan POD objects cleanup by policy.`.  
  - Phụ thuộc: T-009-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-009-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-009-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: DeliverySucceeded, DeliveryFailed, CODCollected with source IDs after commit for notification..  
  - Phụ thuộc: T-009-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-09 — Quality**  
  - Thực hiện: Chạy: Real-device permission, POD file validation/ACL, revoked assignment, duplicate scan and delivery result, cross-module COD atomicity, app restart/offline.  
  - Phụ thuộc: T-009-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-009-01..05; AC-009-01..05.
- [ ] **T-009-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-009-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-009-01..05; AC-009-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
