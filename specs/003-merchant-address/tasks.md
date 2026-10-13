# Implementation Tasks 003: Hồ sơ merchant, địa chỉ và tọa độ

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-003-01 → T-003-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-003-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Dataset danh mục hành chính (phiên bản áp dụng), provider geocoding/matrix, phạm vi hỗ trợ tỉnh/thành và confidence threshold..  
  - Phụ thuộc: specs/001, specs/002.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `GET/POST/PATCH /api/v1/merchants/me/pickup-points; POST /api/v1/addresses/validate; POST /api/v1/addresses/resolve [đề xuất endpoint].`.  
  - Phụ thuộc: T-003-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `merchant.pickup_point; catalog/version và address candidate theo owner đã chốt; order.shipment_address_snapshot thuộc order khi confirm.`.  
  - Phụ thuộc: T-003-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-19, BR-10.  
  - Phụ thuộc: T-003-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Upsert điểm lấy trong merchant; snapshot chỉ tạo ở transaction Order confirmation; geocoder call ngoài transaction.`.  
  - Phụ thuộc: T-003-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-003-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-003-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: Không cần domain event khi address lookup; PickupPointUpdated dùng khi subscriber thật sự cần và có outbox..  
  - Phụ thuộc: T-003-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-09 — Quality**  
  - Thực hiện: Chạy: Unit address validation, provider timeouts and ambiguous responses, cross-owner API, snapshot immutability, Vietnamese administrative data version.  
  - Phụ thuộc: T-003-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-003-01..05; AC-003-01..05.
- [ ] **T-003-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-003-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-003-01..05; AC-003-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
