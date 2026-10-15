# Implementation Tasks 001: Nền tảng repository, deploy và kiến trúc

**Official closure — 2026-10-14:** D1-05 **DONE** for the approved backend CI task.
[PR #5](https://github.com/phamquoainam/logistics-capstone-project/pull/5) merged as
`f50c8972a3a4d0a9c877d6e7e99d2a918cd866fd`; actual develop push
[run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
PASS: architecture 23/no skip, H2 40/eight named PG-only skips, PG18 48/no skip;
guard 14 per job, sanitized artifacts/digests and required-check enforcement verified.
Active ruleset 25049940 targets develop with all three GitHub Actions checks,
strict up-to-date policy and no bypass actors.
[D1-05 evidence](../../docs/implementation/d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).

D1-03 **DONE for approved PostgreSQL Phases A–E**, not full Feature 001.
D1-02/native/lint/pin/log disposition, full onboarding and remaining contracts/
quality tasks remain open. REQ-001-03 is verified for implemented code; AC-001-03
fixture detection is PASS, but no deliberately red production workflow experiment
is claimed. Six absent business modules remain NOT YET IMPLEMENTED/N/A.
Original REQ/AC and full-feature task criteria remain unchanged; Feature 001/Day 1
remain PARTIAL. Earlier execution states remain in historical evidence documents.

## Completed independent Day 1 task scopes

- [x] D1-03 approved PostgreSQL Phases A–E: infrastructure, V1/V2/roles,
  migration lifecycle/persistence, H2/PostgreSQL/web regression and documentation.
- [x] D1-04 approved local architecture: 37 production classes, 23 tests,
  11 negative violations/one positive control; six absent modules N/A.
- [x] D1-05 PR→develop/push develop workflow; least privilege/full-SHA Actions.
- [x] D1-05 clean Linux architecture, H2 regression and isolated PG18 verify.
- [x] D1-05 exact discovery/privacy guard: 14 tests per hosted job.
- [x] D1-05 actual PR HEAD run 38508902499 / 8a25dd5 and post-merge push
  run 38512345091 / f50c897: three jobs/artifacts/exact counts PASS.
- [x] D1-05 effective required checks: Active ruleset 25049940/develop,
  three contexts/source GitHub Actions, strict policy/no bypass actors.
- [x] D1-05 accepted PR #5 merged; evidence/traceability reconciled locally.

## Original full-feature tasks — remain open

These broader tasks are not completed wholesale by independent D1-03/04/05
closure. Contract/health/helpers/native/Testcontainers/full onboarding and
remaining REQ/AC require their own evidence. Original task/AC criteria below
remain unchanged.

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
