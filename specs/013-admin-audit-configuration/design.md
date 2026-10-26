# Technical Design 013: Admin RBAC, cấu hình hệ thống, audit và recovery

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

## 1. Bounded context / code location
- Primary owner: `identity, platform, notification (read), other owner APIs`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/admin-audit-configuration/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/admin-audit-configuration/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
platform.config_definition, config_version, audit_entry, failed_job metadata; owner business data vẫn nằm ở module gốc.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
GET/PATCH /api/v1/admin/config; GET /api/v1/admin/audit; GET /api/v1/admin/jobs/failed; POST /api/v1/admin/jobs/{id}/retry.

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

**Feature-specific atomicity:** Atomic config version increment+audit; recovery request queued with audit and idempotency, downstream domain applies own transaction.

## 5. Events, async / retry
ConfigurationChanged for consumers; JobRecoveryRequested only after admin authorization.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Giá trị config âm/vượt range, admin tự duyệt COD, replay job đã xử lý, sửa audit hoặc kỳ đóng → từ chối.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
Config validation, audit append-only, admin/finance segregation, job retry with duplicate and privilege checks, secrets scanner.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-013-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-013-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-013-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-013-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-013-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: 001, 002, 004, 012. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-S04, NFR-R02, NFR-O01 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Quyền quản trị cụ thể; cho phép hot-reload config nào; retention logs và quy trình duyệt thủ công.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L026 risk-fraud-compliance](../../specs/planning/v3/L026-risk-fraud-compliance/spec.md); [L027 platform-admin-audit-governance](../../specs/planning/v3/L027-platform-admin-audit-governance/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Source thật app/backend/src/main/java/com/elogistic/backend, app/frontend/web, app/frontend/shipper-mobile; apps/com.example cũ chỉ blueprint.9 domains giữ DML ownership, không internals import. L026: BE Guard trước L007; full rules/case/approval đọc finance/identity qua API duyệt; FE Risk case/evidence/propose hold/release approval, minimal PII; L027: BE Identity policy, platform technical ops; business admin qua owner APIs; FE Workspaces theo permission, Auditor read-only, IT scoped retry. Schema/API mới PROPOSED trước review.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
