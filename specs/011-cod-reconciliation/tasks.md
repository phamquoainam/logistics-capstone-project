# Implementation Tasks 011: COD nộp tiền, xác nhận, đối soát và payout mô phỏng

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-011-01 → T-011-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-011-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Cutoff timezone and schedule, dispute policy, partial remittance rules, simulated payout deduction formula, adjustment authority and accounting examples..  
  - Phụ thuộc: specs/001, specs/002, specs/006, specs/009.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `POST /api/v1/cod/remittances; POST /api/v1/cod/remittances/{id}/confirm; GET /api/v1/reconciliations/preview; POST /api/v1/reconciliations; POST /api/v1/reconciliations/{id}/close; POST /api/v1/payouts/simulate.`.  
  - Phụ thuộc: T-011-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `finance.cod_item, remittance, remittance_allocation, reconciliation_period, reconciliation_item, payout, finance_adjustment, ledger refs.`.  
  - Phụ thuộc: T-011-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-15, BR-16, BR-17, BR-18.  
  - Phụ thuộc: T-011-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Confirm remittance and allocation atomic. Close performs eligibility re-check and unique membership in one transaction; payout simulation idempotent, never real transfer.`.  
  - Phụ thuộc: T-011-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-011-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-011-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: CODRemittanceConfirmed, ReconciliationClosed, SimulatedPayoutRecorded after commit..  
  - Phụ thuộc: T-011-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-09 — Quality**  
  - Thực hiện: Chạy: Finance role and segregation, idempotency, multi-period concurrent close, cutoff boundary, allocation sum, immutable closed period, duplicate payout and ledger reconciliation.  
  - Phụ thuộc: T-011-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-011-01..05; AC-011-01..05.
- [ ] **T-011-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-011-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-011-01..05; AC-011-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
