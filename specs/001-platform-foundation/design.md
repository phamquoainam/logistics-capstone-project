# Technical Design 001: Nền tảng repository, deploy và kiến trúc

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

Approved Temurin major `21` logs resolved hosted 21.0.12.1+1; Boot 4.1.1/
Maven Wrapper 3.10.0 and full-SHA Actions remain unchanged. This closure changes
no architecture/API/database design; isolated tmpfs PG roles and exact test
report inventory remain implemented as documented in the CI guide.

**State:** DESIGN_PROPOSED — không mô tả API/migration đã implement.  

**D1-03 DB milestone accepted/implemented 2026-10-13:** DEC-26 approved explicitly; PostgreSQL 18.6 isolated dev container/5433, nine ownership schemas, V1/V2 global stream, platform history, owner/migrator/runtime roles and local/test migrations/shared job implemented. Demo-001 real PostgreSQL tests/migration/permissions/restart/browser checks PASS; H2 remains intact. This subsection records only the approved DB slice; remaining proposed contracts/feature AC are unchanged. [ADR](../../docs/architecture/decisions/ADR-0002-database-ownership.md), [evidence](../../docs/implementation/d1-03-postgresql-evidence.md). D1-03 approved task DONE; full foundation remains PARTIAL.
**Source:** `spec.md`, `docs/architecture/*`, [REPORT] Chương 3–4.

**D1-04 approved local architecture slice, 2026-10-14:** ArchUnit core 1.5.1
test-scope/plain Jupiter, real root `com.elogistic.backend`, implemented packages
identity/merchant/orders/platform. Ten architecture rules plus scope guards and
isolated effectiveness fixtures PASS; six remaining business modules have no
implementation/verification claim. Internal application ports invert persistence
coupling; technical seeds stay in infrastructure. Orders accepts only minimal
`identity.api.AuthenticatedActor` at web/application boundary, populated from the
existing DB-validated security principal. No domain-to-Identity or broad AccountView
dependency. User approved this collaboration/refactor explicitly; original HTTP
JSON, RBAC, ownership, transactions and migrations unchanged.
[ADR-0004](../../docs/architecture/decisions/ADR-0004-architecture-verification.md),
[evidence](../../docs/implementation/d1-04-architecture-evidence.md).
Local verification does not complete CI/full REQ-001-03 or the full feature DoD.

## 1. Bounded context / code location
- Primary owner: `platform, workflow; all module skeletons`.
- Backend: `apps/backend/src/main/java/com/example/elogistics/<module>/api/` và `internal/{web,application,domain,infrastructure}/`.
- Application coordination xuyên module chỉ ở `workflow`, gọi public contracts; không truy cập trực tiếp `othermodule.internal`.
- Web: `apps/web/src/features/platform-foundation/` khi có màn hình liên quan. Mobile: `apps/shipper-mobile/src/features/platform-foundation/` nếu shipper thao tác.

## 2. Domain model & persistence boundary
Schema nine ownership + audit/technical helpers khi cần; migration table; health/readiness; demo seed tách khỏi migration.

**Database rules:** owner schema; foreign/unique/check/conditional update theo `docs/architecture/data-ownership.md`; append-only cho ledger/stock movement/tracking/audit; migration mới thay vì ORM auto-DDL. Không tạo bảng cho dữ liệu chưa có use case.

## 3. Public API proposal — cần đưa vào OpenAPI sau review
GET /actuator/health (hoặc endpoint tương đương); GET /api/v1/meta nếu cần; **không** công khai endpoint debug chứa config.

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

**Feature-specific atomicity:** Không transaction nghiệp vụ; migration atomic theo hỗ trợ PostgreSQL; seed idempotent theo định danh.

## 5. Events, async / retry
Không phát event từ skeleton; chuẩn bị Outbox/Inbox contract chưa triển khai consumer nếu chưa cần.

- Với event thật: versioned envelope `eventId`, `eventType`, `version`, `aggregateId`, `occurredAt`, `correlationId`, minimal payload; outbox commit cùng source; consumer inbox idempotent.
- Retry bounded/backoff và failed job state; callback/scan được dedupe theo business scope, không tin RabbitMQ bảo đảm exactly-once.

## 6. Lỗi / bảo mật / UI state
- **Failure semantics:** Thiếu biến môi trường bắt buộc phải fail-fast có hướng dẫn; Redis down không được báo quote sai; migration lỗi không khởi chạy API nhận ghi.
- **AuthZ:** enforce tại server theo actor và object, bao gồm read, update và async recovery.
- **UI:** loading/success/validation conflict/pending-verification/offline states (chỉ khi phù hợp); giữ input khi request lỗi; optimistic UI không thay business truth.
- **Audit & privacy:** lưu actor + target + trace + reason cho operation nhạy cảm; mask token, PII, GPS và payment key trong log.

## 7. Kiểm thử kỹ thuật (thiết kế trước khi code)
Maven clean verify, module boundaries, Testcontainers Flyway cold-start, Compose smoke, web build/typecheck, mobile development build feasibility.

| Loại test | Bảo đảm |
| --- | --- |
| Unit `TEST-001-UNIT-*` | REQ, guard, edge state |
| Integration `TEST-001-INT-*` | Postgres transaction/constraint, owner boundary, API |
| Concurrency `TEST-001-CONC-*` | Zero duplicate effects, invariant under interleaving |
| E2E `TEST-001-E2E-*` | User journey, pending/conflict/error UX |
| Fault `TEST-001-FAULT-*` | Timeout/retry/crash + recovery states |

## 8. Interfaces & dependencies
Feature prerequisites: foundation bootstrap. Không thêm call cycle; nếu thiếu contract, tạo/duyệt public interface trước khi implement service.

## 9. Security/performance/operability
- Đo metric theo NFR liên quan NFR-O01, NFR-O02, NFR-O03, NFR-M01, NFR-M02 và tên operation.
- Load benchmark không tạo số liệu giả; dùng cùng seed/fixture và môi trường được công bố.
- Test RBAC/tenant scoping và no token/log sensitive data.

## 10. Decisions to close before implementation
[BLOCKING] Khóa phiên bản Spring/Modulith/Node/Expo, auth approach, provider storage/map; phụ thuộc nền tảng nào chưa chốt cần spike và ADR.

Mọi endpoint/tên bảng/field mới trong tài liệu là **[PROPOSED]**; báo cáo không định nghĩa đầy đủ API/migration. Sau khi duyệt, khóa hợp đồng ở `contracts/openapi/` và ghi ADR khi quyết định mang tính kiến trúc.
