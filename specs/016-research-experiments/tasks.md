# Implementation Tasks 016: Thực nghiệm RQ1–RQ4 và tái lập bằng chứng

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-016-01 → T-016-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-016-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Baseline load environment, datasets, target devices, fixed maps provider/matrix, optional multi-replica plan..  
  - Phụ thuộc: specs/001, specs/004, specs/005, specs/006, specs/007, specs/008, specs/010, specs/011, specs/012.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `Internal test-only endpoints nếu cần phải tắt ngoài test profile; health/metrics chỉ có quyền phù hợp; không dùng fake public APIs.`.  
  - Phụ thuộc: T-016-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `quality/datasets/, quality/evidence/ (kết quả thực), quality/benchmarks/, scripts/fault/; không thêm bảng production nếu chỉ phục vụ research.`.  
  - Phụ thuộc: T-016-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-02, BR-06, BR-11, BR-13, BR-18, BR-20, BR-21.  
  - Phụ thuộc: T-016-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Each scenario run isolation/seed reset, query invariant after each run; fault injection có rollback môi trường thử, không tác động dữ liệu thật.`.  
  - Phụ thuộc: T-016-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-016-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-016-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: Collect metrics and logs only, không phát business events khi tạo synthetic report..  
  - Phụ thuộc: T-016-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-09 — Quality**  
  - Thực hiện: Chạy: JUnit/Testcontainers concur suites, k6 workloads, Redis cache hit counters, fault-compose scripts, route matrix fixtures and statistical summary.  
  - Phụ thuộc: T-016-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-016-01..05; AC-016-01..05.
- [ ] **T-016-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-016-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-016-01..05; AC-016-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
