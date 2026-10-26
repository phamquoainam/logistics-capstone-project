# Implementation Tasks 010: Gợi ý tuyến, phê duyệt tuyến và GPS

**Tình trạng hiện tại:** Chưa triển khai. **Rule:** Không tick DONE trước khi có code+test+evidence.  
**Ordering:** T-010-01 → T-010-02…; các phần web/mobile có thể song song sau khi contract+API ổn định, không merge thiếu tests.

- [ ] **T-010-01 — Review**  
  - Thực hiện: Đọc source trace FR/BR/NFR và xác nhận [BLOCKING] Map tiles/geocoding/matrix provider, coverage, license and credentials; route baseline algorithm; optimization constraints; GPS stale/accuracy thresholds and location transport..  
  - Phụ thuộc: specs/001, specs/002, specs/008.  
  - Hoàn thành khi: Owner và reviewer đánh dấu PROPOSED/ACCEPTED; không còn BLOCKING của vertical slice.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-02 — Contract**  
  - Thực hiện: Tạo hoặc cập nhật OpenAPI/event DTO cho `POST /api/v1/routes/proposals; POST /api/v1/routes/{id}/confirm; GET /api/v1/shipper/routes/current; POST /api/v1/location-sessions; POST /api/v1/locations/batch; GET /api/v1/dispatch/locations.`.  
  - Phụ thuộc: T-010-01.  
  - Hoàn thành khi: Schema build/validate được, mẫu request/response và error cases xác định.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-03 — Migration**  
  - Thực hiện: Xác định data model + Flyway migration cho `routing.route_plan, route_stop, route_version, location_session, location_update, latest_location_projection; references to dispatch assignment.`.  
  - Phụ thuộc: T-010-02.  
  - Hoàn thành khi: Migration apply từ DB trống + nâng cấp, kiểm tra indexes/constraints ownership và không phá schema hiện có.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-04 — Domain**  
  - Thực hiện: Viết domain types/guards bảo vệ BR-20, BR-21.  
  - Phụ thuộc: T-010-03.  
  - Hoàn thành khi: Unit test happy path, negative, bound và transition xanh.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-05 — Application**  
  - Thực hiện: Implement use cases, port, public module API và workflow `Matrix external outside DB transaction; route confirmation checks assignment current + sets single effective version atomically; location updates independent, bounded writes.`.  
  - Phụ thuộc: T-010-04.  
  - Hoàn thành khi: Transaction consistent; không import internals module khác; integration test pass.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-06 — HTTP/security**  
  - Thực hiện: Lắp controller, validation, role+object authZ, error mapping, idempotency.  
  - Phụ thuộc: T-010-05.  
  - Hoàn thành khi: Contract tests và cross-tenant/expired-scope tests pass.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-07 — Frontend**  
  - Thực hiện: Lắp web/mobile feature UI cần thiết theo user journey, gồm loading/pending/error/conflict.  
  - Phụ thuộc: T-010-06.  
  - Hoàn thành khi: Hành vi form/state có test; không báo success trước server.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-08 — Async/integrations**  
  - Thực hiện: Kết nối provider/outbox/inbox/events nếu feature cần: RouteConfirmed/LocationSessionStopped; location stream realtime implementation [PROPOSED] polling vs websocket needs ADR..  
  - Phụ thuộc: T-010-07.  
  - Hoàn thành khi: Duplicate/retry/crash không gây tác động trùng, failure có trạng thái rõ.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-09 — Quality**  
  - Thực hiện: Chạy: Route constraints, stale data, parallel versions, matrix missing, p95 compute/matrix separated, <=10s 50 points, GPS <=5s server-to-UI under declared conditions.  
  - Phụ thuộc: T-010-08.  
  - Hoàn thành khi: Unit/integration/concurrency/E2E/fault test và audit/metrics đạt, có run log.  
  - Trace: REQ-010-01..05; AC-010-01..05.
- [ ] **T-010-10 — Review & trace**  
  - Thực hiện: Cập nhật traceability + runbook + README/decision status, review PR an toàn.  
  - Phụ thuộc: T-010-09.  
  - Hoàn thành khi: Tất cả REQ/AC có test IDs và evidence; DoD trong docs/quality/test-strategy.md đầy đủ.  
  - Trace: REQ-010-01..05; AC-010-01..05.

## Verification gate (trước khi tích hợp)
- [ ] `mvnw verify` (backend) và architecture/integration tests.
- [ ] `typecheck`, lint, build web; mobile smoke/development build nếu liên quan.
- [ ] Flyway upgrade/test trên PostgreSQL sạch; không phát sinh file secrets.
- [ ] Thử replay key giống/khác payload, unauthorized object và concurrency nếu module có stateful changes.
- [ ] Chạy E2E happy và negative scenario; cập nhật evidence thật, traceability và ADR.

## Prompt cho Codex sau khi duyệt thiết kế
Đọc `AGENTS.md`, `docs/principles.md`, `docs/architecture/*`, các quyết định ACCEPTED và ba file feature này. Chỉ làm task đầu tiên chưa tick; không đổi tài liệu ngoài phạm vi nếu không có lý do và diff; chạy test, trình bày file đã chỉnh, kết quả, giả định và blocker. Không tự đánh dấu đã test khi chưa chạy.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L004 address-coverage-service-catalog](../../specs/planning/v3/L004-address-coverage-service-catalog/spec.md); [L014 delivery-dispatch-and-route](../../specs/planning/v3/L014-delivery-dispatch-and-route/spec.md); [L025 network-ops-routing-optimization](../../specs/planning/v3/L025-network-ops-routing-optimization/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Review task đầu tiên chưa tick giữ mở, chốt D-V3-03; DEC-05; DEC-06; DEC-06; DEC-17; DEC-18; DEC-14; DEC-17; DEC-18. Một authorization capability gồm contract+migration+BE/HTTP+FE+tests/CI/evidence, không checkpoints routine theo tầng; dependencies đối chiếu DAG trước code.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
