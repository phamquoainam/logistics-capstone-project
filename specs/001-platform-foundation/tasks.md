# Implementation Tasks 001: Nền tảng repository, deploy và kiến trúc

**D1-05 independent CI task slice (2026-10-14):** user confirms reviewer-approved
D1-04 merged; develop baseline 2c609c4 verified. This implements only CI quality /
trace portions of T-001-09/10 and REQ-001-03, not all tasks below.
[Commands/results](../../docs/implementation/d1-05-ci-evidence.md).

- [x] Workflow PR→develop/push develop, least privilege/full-SHA Actions.
- [x] Local fresh Linux architecture 23/no skips, negative fixture detection,
  default/H2 40/eight named PG-only skips and fresh PG18 48/no skips.
- [x] Discovery fail-closed/privacy guards (14 tests), YAML/Bash lint, evidence/trace.
- [ ] Actual hosted Actions run URL/tested SHA and three checks/artifacts PASS.
- [ ] Develop required checks configured and enforcement verified by maintainer.

**D1-05 PARTIAL / READY FOR REMOTE VERIFICATION.** No commit/push/PR or settings
change authorized in this execution. Original full feature tasks/AC remain open;
no Testcontainers, native/mobile or full cold-start claim from these Linux tests.

**Tình trạng hiện tại:** PARTIAL — D1-03 PostgreSQL/Demo-001 DB milestone được duyệt và kiểm chứng ([evidence](../../docs/implementation/d1-03-postgresql-evidence.md)); các task/AC đầy đủ dưới đây chưa DONE. **Rule:** Không tick DONE trước khi có code+test+evidence.
**Ordering:** T-001-01 → T-001-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

**D1-04 verified task slice (2026-10-14):** local architecture coverage and negative
fixtures PASS; minimal trusted actor and adapter-direction repair approved.
PostgreSQL verify 48 PASS; H2 40 PASS/eight PG-only skip; web regression PASS.
[Evidence](../../docs/implementation/d1-04-architecture-evidence.md) and
[traceability](../../docs/quality/traceability.md). This completes only the approved
local verification slice of T-001-01/05/09/10. Full tasks below remain unchecked:
CI, contracts, independent reproduction and native/toolchain gates are still open.

- [ ] **T-001-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Khóa phiên bản Spring/Modulith/Node/Expo, auth approach, provider storage/map; phụ thuộc nền tảng nào chưa chốt cần spike và ADR..  
  - Phụ thuộc: Không.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `GET /actuator/health (hoặc endpoint tương đương); GET /api/v1/meta nếu cần; **không** công khai endpoint debug chứa config.`.  
  - Phụ thuộc: T-001-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `Schema nine ownership + audit/technical helpers khi cần; migration table; health/readiness; demo seed tách khỏi migration.`.  
  - Phụ thuộc: T-001-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ module/ownership rules.  
  - Phụ thuộc: T-001-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Không transaction nghiệp vụ; migration atomic theo hỗ trợ PostgreSQL; seed idempotent theo định danh.`.  
  - Phụ thuộc: T-001-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-001-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-001-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: Không phát event từ skeleton; chuẩn bị Outbox/Inbox contract chưa triển khai consumer nếu chưa cần..  
  - Phụ thuộc: T-001-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-09 — Quality**  
  - Thực hiện: Chạy: Maven clean verify, module boundaries, Testcontainers Flyway cold-start, Compose smoke, web build/typecheck, mobile development build feasibility.  
  - Phụ thuộc: T-001-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-001-01..05; AC-001-01..05.
- [ ] **T-001-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-001-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-001-01..05; AC-001-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.
