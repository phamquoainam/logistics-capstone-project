# Technical Design 001: Nền tảng repository, deploy và kiến trúc

## Approved D1-08 Task B — full dependency foundation, 2026-10-17

Explicit user approval B-01..B-09 is recorded in
[ADR-0005](../../docs/architecture/decisions/ADR-0005-foundation-dependencies.md).
Additive Redis/Rabbit Compose and demo,postgres,foundation target; livenessState
independent, readinessState+db+redis+rabbit, authenticated probes and status-only
security. Redis AGPLv3/ephemeral/ACL and Rabbit persistent/non-admin/vhost policy
are accepted for local foundation, not business caching or messaging.

Boot-managed clients are opt-in only. Real native Rabbit cached-UP characterization
justifies the bounded health-only AMQP connection/channel RPC; application RPC
policy and Task A JDBC validation remain untouched. Faults, recovery, persistence,
profile isolation, timeout/privacy/resource tests are documented in
[Task B evidence](../../docs/implementation/d1-08-foundation-evidence.md).
No new database migration, domain contract or CI workflow is approved here.
Full feature001/Day1 remain PARTIAL; original REQ/AC below are unchanged.

## Approved D1-08 Task A - health/readiness runtime, 2026-10-17

The user's Task A explicitly approves Boot-managed Actuator4.1.1 and separate
GET /actuator/health/liveness and /actuator/health/readiness. This bounded
implementation does not accept other proposed API/envelope/event contracts.
One backend artifact; no new module or health framework. The explicitly approved
timeout-safety fix adds one small platform infrastructure Actuator contributor. Add the
starter without an explicit version, managed by the existing Boot4.1.1 parent.

Default/non-demo context opts management access out and probes off. The existing
demo profile opts health read-only/probes on explicitly for H2 and demo,postgres:
liveness group=livenessState, readiness group=readinessState,db. Membership
validation stays enabled. Runtime migration failures still prevent startup;
readiness is not a mechanism to bypass failed Flyway startup. One-shot migration
is non-web. PostgreSQL-only deployment without demo is not a new supported target.

Management web exposure includes health only; discovery off, JMX exposure excluded,
details/components never. DemoSecurity permits anonymous GET of only the two exact
probe paths; aggregate health, nested details and other management paths remain
denied, including for ADMIN. JWT/business rules are unchanged. Use Boot defaults
UP200 and DOWN/OUT_OF_SERVICE503; do not override their status mapping.

Timeout-safety fix approved after review reproduced the rejected global3s setting:
retain Hikari acquisition2000ms/validation1000ms and PG connectTimeout2s, remove
global socketTimeout. DatabaseReadinessConfiguration in platform/internal/
infrastructure contributes dbHealthIndicator using the existing runtime pool.
JDBC isValid(1) in pinned pgJDBC42.7.13 scopes/restores its own network timeout;
return UP only for a valid open connection with unchanged original timeout.
Explicitly evict failed/unrestored connections; close on all paths; no exception
details. Boot's conditional default db contributor backs off by bean name.
No executor/future-based JDBC cancellation, shared health pool or manual global
socket mutation. Runtime pool exhaustion remains DOWN. Long SQL is not assigned
a new application deadline. Real H2/PG/default context and PG safety/recovery
regressions verify behavior. This is no universal HTTP SLA or Flyway startup
recovery claim. H2 has no PG driver properties; transaction/pool-size/startup and
migration policy remain unchanged. Framework types are infrastructure dependencies,
not imports into domain or another module's internal packages.

Test IDs TEST-001-HEALTH-01..04 and exact runtime results are in
[Task A evidence](../../docs/implementation/d1-08-health-readiness-evidence.md).
Original REQ/AC and proposed sections below remain unchanged. Redis/RabbitMQ,
Testcontainers, broader observability and full independent onboarding remain open.

**D1-02 local execution — 2026-10-15: DONE for approved toolchain/native starter feasibility.**
Node24.13.0/npm11.6.2 pinned; both frontend clean installs/lint/typecheck/tests,
web build/browser regression and Android export PASS. Debug APK build/install/
real starter render on Pixel_7/API37 and two cold launches/Metro/no fatal windows
PASS; package com.elogistic.shipper.dev. Online SDK patch advisory remains FAIL
and dependency/upstream nonfatal findings OPEN; no framework upgrade, skipped
mandatory test or full Feature001 DONE claim.
[Evidence](../../docs/implementation/d1-02-toolchain-evidence.md).

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
D1-02 local tooling/native starter scope is now DONE; full onboarding, native
business features and remaining contracts/quality tasks remain open. REQ-001-03 is verified for implemented code; AC-001-03
fixture detection is PASS, but no deliberately red production workflow experiment
is claimed. Six absent business modules remain NOT YET IMPLEMENTED/N/A.
Original REQ/AC and full-feature task criteria remain unchanged; Feature 001/Day 1
remain PARTIAL. Earlier execution states remain in historical evidence documents.

Approved Temurin major `21` logs resolved hosted 21.0.12.1+1; Boot 4.1.1/
Maven Wrapper 3.10.0 and full-SHA Actions remain unchanged. This closure changes
no architecture/API/database design; isolated tmpfs PG roles and exact test
report inventory remain implemented as documented in the CI guide.

Development-only native configuration: app.json package com.elogistic.shipper.dev,
scheme elogistics-shipper-dev; expo-dev-client57.0.20 through SDK57 CLI. CNG output
is generated/ignored, not hand-maintained native architecture. Original Go
commands retained with explicit start:go/start:dev/android:dev targets. No business
API/RBAC/domain/database/architecture-rule design changes.

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
