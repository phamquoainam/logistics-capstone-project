# Implementation Tasks 013: Admin RBAC, cấu hình hệ thống, audit và recovery

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-013-01 → T-013-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-013-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Quyền quản trị cụ thể; cho phép hot-reload config nào; retention logs và quy trình duyệt thủ công..  
  - Phụ thuộc: specs/001, specs/002, specs/004, specs/012.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `GET/PATCH /api/v1/admin/config; GET /api/v1/admin/audit; GET /api/v1/admin/jobs/failed; POST /api/v1/admin/jobs/{id}/retry.`.  
  - Phụ thuộc: T-013-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `platform.config_definition, config_version, audit_entry, failed_job metadata; owner business data vẫn nằm ở module gốc.`.  
  - Phụ thuộc: T-013-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-02, BR-09, BR-12, BR-18, BR-20.  
  - Phụ thuộc: T-013-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Atomic config version increment+audit; recovery request queued with audit and idempotency, downstream domain applies own transaction.`.  
  - Phụ thuộc: T-013-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-013-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-013-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: ConfigurationChanged for consumers; JobRecoveryRequested only after admin authorization..  
  - Phụ thuộc: T-013-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-09 — Quality**  
  - Thực hiện: Chạy: Config validation, audit append-only, admin/finance segregation, job retry with duplicate and privilege checks, secrets scanner.  
  - Phụ thuộc: T-013-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-013-01..05; AC-013-01..05.
- [ ] **T-013-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-013-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-013-01..05; AC-013-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
