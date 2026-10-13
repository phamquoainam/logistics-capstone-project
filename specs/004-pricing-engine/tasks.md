# Implementation Tasks 004: Pricing Engine và bảng giá theo phiên bản

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-004-01 → T-004-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-004-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Đơn vị kg/g/cm, divisor cân nặng quy đổi, vùng giá, thứ tự phụ phí/chiết khấu, rounding, thời hạn quote, tariff sample..  
  - Phụ thuộc: specs/001, specs/002, specs/003.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `POST /api/v1/quotes; GET /api/v1/quotes/{quoteId}; GET/POST /api/v1/admin/tariffs; POST /api/v1/admin/tariffs/{id}/publish.`.  
  - Phụ thuộc: T-004-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `pricing.tariff, tariff_version, price_rule, quote, breakdown lines/cache meta; order.quote_snapshot riêng module.`.  
  - Phụ thuộc: T-004-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-10, BR-11, BR-12, BR-19.  
  - Phụ thuộc: T-004-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Publish version và active-interval guard một transaction; quote snapshot chỉ do Order ghi ở confirm; tính cước không trừ tiền.`.  
  - Phụ thuộc: T-004-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-004-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-004-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: TariffPublished để invalidate/rotate cache; event không thay source-of-truth PostgreSQL..  
  - Phụ thuộc: T-004-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-09 — Quality**  
  - Thực hiện: Chạy: Golden case rule/rounding, codepoint? numeric boundaries, tariff overlap, concurrent publish, no/cold/hot cache, outage fallback, p95 workload measured.  
  - Phụ thuộc: T-004-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-004-01..05; AC-004-01..05.
- [ ] **T-004-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-004-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-004-01..05; AC-004-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
