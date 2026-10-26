# Technical Design 004: Pricing Engine và bảng giá theo phiên bản

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `pricing`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/pricing-engine/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/pricing-engine/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
pricing.tariff, tariff_version, price_rule, quote, breakdown lines/cache meta; order.quote_snapshot riêng module.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
POST /api/v1/quotes; GET /api/v1/quotes/{quoteId}; GET/POST /api/v1/admin/tariffs; POST /api/v1/admin/tariffs/{id}/publish.

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

**Feature-specific atomicity:** Publish version và active-interval guard một transaction; quote snapshot chỉ do Order ghi ở confirm; tính cước không trừ tiền.

## 5. Events, async / retry
TariffPublished để invalidate/rotate cache; event không thay source-of-truth PostgreSQL.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Thiếu zone/tier, kích thước âm, quote expired, tariff inactive, inconsistent breakdown hoặc Redis không hoạt động → lỗi xác định/fallback an toàn.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
Golden case rule/rounding, codepoint? numeric boundaries, tariff overlap, concurrent publish, no/cold/hot cache, outage fallback, p95 workload measured.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-004-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-004-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-004-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-004-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-004-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 001, 002, 003. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-P02, NFR-C03 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Đơn vị kg/g/cm, divisor cân nặng quy đổi, vùng giá, thứ tự phụ phí/chiết khấu, rounding, thời hạn quote, tariff sample.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L005 pricing-contracts-quotation](../../specs/planning/v3/L005-pricing-contracts-quotation/spec.md); [L018 billing-ledger-fees-invoices](../../specs/planning/v3/L018-billing-ledger-fees-invoices/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Source thật app/backend/src/main/java/com/elogistic/backend, app/frontend/web, app/frontend/shipper-mobile; apps/com.example cũ chỉ blueprint.9 domains giữ DML ownership, không internals import. L005: BE Rate/effective interval/rounding/phụ phí/Sales proposal/approval; FE Public/Sender/Merchant quote; Pricing publish; Sales chỉ đề xuất; L018: BE Minimal balanced posting/fee/refund API trước L007; full invoice/debt/adjustment; FE Sender receipt; Merchant Finance fee/debt; Finance ledger; Pricing adjustment. Schema/API mới PROPOSED trước review.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
