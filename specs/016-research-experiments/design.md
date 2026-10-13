# Technical Design 016: Thực nghiệm RQ1–RQ4 và tái lập bằng chứng

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `quality, scripts, infrastructure`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/research-experiments/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/research-experiments/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
quality/datasets/, quality/evidence/ (kết quả thực), quality/benchmarks/, scripts/fault/; không thêm bảng production nếu chỉ phục vụ research.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
Internal test-only endpoints nếu cần phải tắt ngoài test profile; health/metrics chỉ có quyền phù hợp; không dùng fake public APIs.

- `Authorization`: role + scope + ownership theo FR/BR; request owner ID không phải nguồn đáng tin.
- Mutation dùng `Idempotency-Key` ở điểm có tác động, `X-Request-Id` để correlate; DTO được validate, lỗi có stable `error.code`.
- Trạng thái gợi ý: `201` create, `200` query/commit, `202` deferred/unknown, `400/422` invalid input/business, `401/403/404` auth, `409` duplicate/state, `503` dependency; cụ thể hóa tại `contracts/openapi/`.
- Không trả toàn bộ persistence entity/JPA object; output use case DTO tối thiểu.

## 4. Data flow / transaction boundary
```mermaid
sequenceDiagram
  participant UI as Web/Mobile
  participant C as HTTP Adapter
  participant UC as Application/Workflow
  participant MOD as Domain module public API
  participant DB as PostgreSQL
  UI->>C: Authenticated command + Idempotency-Key
  C->>UC: Validated DTO + trusted identity/scope
  UC->>MOD: Check current state / call command
  MOD->>DB: Atomic writes + constraints + audit/outbox
  DB-->>MOD: Commit/rollback
  MOD-->>UC: Result/status
  UC-->>C: Result or typed error
  C-->>UI: Response + requestId
```

**Feature-specific atomicity:** Each scenario run isolation/seed reset, query invariant after each run; fault injection có rollback môi trường thử, không tác động dữ liệu thật.

## 5. Events, async / retry
Collect metrics and logs only, không phát business events khi tạo synthetic report.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Một run vi phạm invariant phải đánh fail mặc dù latency nhanh; thiếu matrix phải đánh invalid experiment; hỏng tool không được thay bằng số liệu ước đoán.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
JUnit/Testcontainers concur suites, k6 workloads, Redis cache hit counters, fault-compose scripts, route matrix fixtures and statistical summary.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-016-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-016-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-016-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-016-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-016-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 001, 004, 005, 006, 007, 008, 010, 011, 012. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-C01, NFR-P01, NFR-P02, NFR-P03, NFR-P04, NFR-O03 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Baseline load environment, datasets, target devices, fixed maps provider/matrix, optional multi-replica plan.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.
