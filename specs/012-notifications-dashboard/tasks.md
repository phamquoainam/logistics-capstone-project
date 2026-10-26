# Implementation Tasks 012: Thông báo nội bộ và dashboard theo vai trò

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-012-01 → T-012-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-012-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Danh sách thông báo P0 thiết yếu và người nhận từng loại; KPI dashboard, lịch sync và retention..  
  - Phụ thuộc: specs/001, specs/002, specs/005, specs/008, specs/011.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `GET /api/v1/notifications; POST /api/v1/notifications/{id}/read; GET /api/v1/dashboard/overview; GET /api/v1/admin/failed-jobs [theo role].`.  
  - Phụ thuộc: T-012-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `notification.notification, notification_receipt, inbox, delivery_attempt, read projections owned by query consumer; event IDs stable.`.  
  - Phụ thuộc: T-012-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-01, BR-02.  
  - Phụ thuộc: T-012-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Source event outbox committed with source use case; consumer inbox mark + notification insert in one transaction.`.  
  - Phụ thuộc: T-012-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-012-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-012-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: Consumable public events ShipmentConfirmed, HubBacklogDetected, CODRemittanceConfirmed, ReconciliationClosed; schema versioned..  
  - Phụ thuộc: T-012-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-09 — Quality**  
  - Thực hiện: Chạy: Outbox recovery/duplicates, notification dedup, private ACL, dashboard aggregate ownership, lag metrics, no success inflation.  
  - Phụ thuộc: T-012-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-012-01..05; AC-012-01..05.
- [ ] **T-012-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-012-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-012-01..05; AC-012-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L009 tracking-event-core-public-lookup](../../specs/planning/v3/L009-tracking-event-core-public-lookup/spec.md); [L020 customer-support-tickets-sla](../../specs/planning/v3/L020-customer-support-tickets-sla/spec.md); [L028 notifications-analytics-sla](../../specs/planning/v3/L028-notifications-analytics-sla/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Review task đầu tiên chưa tick giữ mở, chốt D-V3-05; D-V3-06; DEC-20; D-V3-06; DEC-20; DEC-20; D-V3-06. Một authorization capability gồm contract+migration+BE/HTTP+FE+tests/CI/evidence, không checkpoints routine theo tầng; dependencies đối chiếu DAG trước code.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
