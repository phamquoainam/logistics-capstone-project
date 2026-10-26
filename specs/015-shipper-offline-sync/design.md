# Technical Design 015: Hàng đợi offline và đồng bộ an toàn trên mobile

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `shipper-mobile, order, dispatch`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/shipper-offline-sync/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/shipper-offline-sync/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
SQLite offline_command, offline_media_ref, sync_attempt; server idempotency/inbox and authoritative shipment/assignment.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
POST /api/v1/mobile/sync/actions (optional batch) hoặc tái sử dụng command endpoints kèm Idempotency-Key; GET current task for conflict resolution.

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

**Feature-specific atomicity:** SQLite transaction queue enqueue/ack; server handles each business action with own domain transaction; no distributed transaction phone↔server.

## 5. Events, async / retry
Không phát event trực tiếp từ offline device; server phát event sau commit hợp lệ.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Assignment reassigned trước sync, token expired, duplicate operation, insufficient media storage, upload POD dang dở → trạng thái yêu cầu xử lý chứ không xóa âm thầm.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
App force kill/restart, airplane mode, assignment revoked, double sync workers, file missing, queue size/storage exhaustion, privacy after logout.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-015-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-015-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-015-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-015-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-015-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 002, 008, 009. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-R03, NFR-U02, NFR-S01 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Danh sách offline action; sync conflict UX, queue TTL/limits, encryption/storage, POD file upload strategy.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L029 courier-offline-sync](../../specs/planning/v3/L029-courier-offline-sync/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Source thật app/backend/src/main/java/com/elogistic/backend, app/frontend/web, app/frontend/shipper-mobile; apps/com.example cũ chỉ blueprint.9 domains giữ DML ownership, không internals import. L029: BE RN SQLite queue; server reuse commands/current assignment/resource guards; FE Existing shipper-mobile scan/POD/COD pending/ACK/conflict/re-auth. Schema/API mới PROPOSED trước review.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
