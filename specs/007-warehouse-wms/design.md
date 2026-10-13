# Technical Design 007: WMS: nhận hàng, tồn, giữ, pick-pack-xuất, hoàn

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `warehouse`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/warehouse-wms/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/warehouse-wms/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
warehouse.owner_sku, warehouse, location, inventory_balance, stock_movement, reservation, outbound, return_receipt, asn, qc_record.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
POST /api/v1/warehouses/{id}/receipts; GET /api/v1/warehouses/{id}/stock; POST /api/v1/reservations [internal workflow]; POST /api/v1/outbounds/{id}/pick, /pack, /dispatch; POST /api/v1/returns.

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

**Feature-specific atomicity:** Conditional update/row-lock transaction balance+movement+reservation; confirm shipment coordinator uses warehouse public reserve API in same transaction.

## 5. Events, async / retry
StockReserved, OutboundReady, StockAdjusted after commit, no HubReceived inference.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Giữ đồng thời vượt 10, pick 8 khi chỉ giữ 7, nhập lặp, QC hàng cách ly, cross-merchant SKU, cancel sau outbound → từ chối/luồng return.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
Stock conservation, concurrent reservation, duplicate operation, private SKU ownership, pick/pack guards, return idempotency, real DB tests.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-007-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-007-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-007-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-007-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-007-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 001, 002, 003. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-C01, NFR-C02, NFR-S01 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] SKU measurement units, QC eligibility, warehouse picking statuses, scan label format, fulfillment vs direct pickup policy.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.
