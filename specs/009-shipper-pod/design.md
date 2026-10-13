# Technical Design 009: Shipper Mobile: lấy/giao/hoàn, POD và COD collection

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `order, dispatch, finance, shipper-mobile`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/shipper-pod/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/shipper-pod/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
order.delivery_attempt, delivery_record, pod_metadata, parcel scan refs; dispatch.assignment; finance.cod_item / collection record; mobile offline queue.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
GET /api/v1/shipper/tasks; POST /api/v1/shipper/tasks/{id}/start; POST /api/v1/shipper/scans; POST /api/v1/deliveries/{attemptId}/result; POST /api/v1/pods/uploads [policy-specific].

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

**Feature-specific atomicity:** After external file upload, atomic server commit delivery result + tracking + COD actual collected (if applicable) through workflow; orphan POD objects cleanup by policy.

## 5. Events, async / retry
DeliverySucceeded, DeliveryFailed, CODCollected with source IDs after commit for notification.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** POD của attempt khác, assignment bị thu hồi, sai parcel, duplicate scan, mất mạng khi upload, COD mismatch → conflict/pending không thành công giả.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
Real-device permission, POD file validation/ACL, revoked assignment, duplicate scan and delivery result, cross-module COD atomicity, app restart/offline.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-009-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-009-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-009-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-009-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-009-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 001, 002, 005, 008, 006. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-R03, NFR-S03, NFR-S05 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Allowed POD form photo/signature/OTP and fields, file provider, max size, retry/delivery attempt limits, offline-safe mutations.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.
