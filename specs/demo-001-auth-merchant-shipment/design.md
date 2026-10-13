# Demo 001 — Technical design and temporary decisions

## Accepted PostgreSQL extension — D1-03, 2026-10-13

User approved DEC-26 and integration after Demo-001 merge 08c0c34. The H2 implementation below is preserved as its historical fallback design. PostgreSQL target uses `demo,postgres`, isolated Docker 18.6/5433/elogistics_dev, approved five demo tables and unchanged APIs/invariants. Global PostgreSQL V1 schemas/V2 tables live in `db/migration`; history `platform.flyway_schema_history`; H2 `db/demo-migration/V1` remains immutable. NOLOGIN owner, migrator SET ROLE owner and one runtime DataSource with DML only. Events/receipts also reject runtime UPDATE/DELETE. Six additional domain schemas contain no speculative tables. Local/test separate migration credentials; shared job/runtime separation. No H2 data conversion or new business feature. [ADR](../../docs/architecture/decisions/ADR-0002-database-ownership.md), [evidence](../../docs/implementation/d1-03-postgresql-evidence.md), [runbook](../../docs/runbooks/postgresql-foundation.md).

## Authorization and readiness
The user explicitly authorized demo endpoints/DTOs/tables, signed short-lived tokens, web scaffolding and an isolated H2 fallback. No P0 acceptance changes. PostgreSQL native is running but no connection credentials are configured; do not guess credentials or touch existing clusters. Use H2 file storage on D for this demo; keep PostgreSQL 18 as the foundation proposal.

## Ownership / flow
Single existing Spring Boot artifact, Java 21 / Boot 4.1.1 / Maven 3.10.0 unchanged. Packages: identity, merchant, orders, platform. Each domain owns application/HTTP/persistence adapters under internal; another module uses only api. Spring JDBC and Flyway; no ORM auto-DDL.
Browser → stateless bearer authentication → identity account lookup (fresh DB role/status) → domain use case → owner repository → own schema. Orders checks merchant membership through merchant.api, never merchant tables.

## Model (demo contract)
- identity.demo_account: UUID, username unique, BCrypt hash, display name, six-role enum, enabled, optional merchant UUID.
- merchant.demo_merchant: UUID, display name, synthetic reference; read-only local seed.
- orders.demo_shipment: UUID, trusted merchant UUID, reference, pickupAddress, recipientName, recipientPhone, deliveryStreet, deliveryWard, deliveryProvince, parcelDescription, weightKg decimal, status DRAFT/CANCELLED, creator UUID, createdAt/updatedAt.
- orders.demo_event: append-only created/cancelled event, shipment ID, actor ID and timestamp.
- orders.demo_operation: merchant+operation+key unique, canonical payload SHA-256, shipment ID. Persistent transaction receipt; replay returns current shipment representation.
No cross-schema SQL/FKs or arbitrary status update. Unique merchant/reference, positive weight, status checks and transaction atomicity enforced. Scoped row locking serializes cancellation; duplicate races use database uniqueness then replay after rollback.

## API contract
Prefix /api/demo/v1; JSON responses are resource objects/arrays, errors {code,message}. Validation errors contain no submitted password/token.
| Method | Path | Request | Response / permission |
|---|---|---|---|
| POST | /auth/login | username,password | accessToken,expiresIn,user; public, rate limited |
| GET | /auth/me | bearer | id,username,displayName,role,merchantId; enabled user |
| GET | /merchant/me | bearer | id,displayName,reference; MERCHANT |
| GET | /shipments | bearer | own shipment array (most recent 200); MERCHANT |
| POST | /shipments | draft fields above; Idempotency-Key | 201 shipment, Location; MERCHANT |
| GET | /shipments/{id} | bearer | {shipment,events,addressVerification}; MERCHANT; 404 cross-owner |
| POST | /shipments/{id}/cancel | reason; Idempotency-Key | cancelled shipment; MERCHANT |
| GET | /admin/users | bearer | sanitized account overview; ADMIN |
| GET | /admin/overview | bearer | total/draft/cancelled counts; ADMIN |
| GET | /admin/shipments | bearer | scoped permission permits summary only; ADMIN |
| GET | /health | none | status UP after startup migrations; public local demo |
Unsupported paths use deny-by-default authorization. No Finance privilege for ADMIN. No registration or confirmation endpoint.

## Database/configuration
Profile demo only, backend bound to 127.0.0.1:8080. H2 URL configured by launcher, file under repository .demo/data on D, credentials and token secret generated into ignored .demo/settings.json. Do not log credentials. Flyway location classpath:db/demo-migration, independent from future production migrations, global V1 stream for demo. Seed runner only when demo.seed.enabled=true; creates missing synthetic accounts using locally generated passwords, never overwrites existing accounts/hashes. H2 runtime/migration share local credentials: production DB role isolation remains deferred.
Credentials persist across restart; deleting settings is not a password-reset strategy. Token HS256 secret must have at least 32 bytes; issuer and expiry validated; role claims are not trusted. Tokens stored only in browser memory; page reload requires login. No cookie authentication; CSRF disabled only for this stateless bearer chain.
Web React/TypeScript + Vite; relative /api via local dev proxy; strict localhost dev/preview hosts. No production target implied. All labels expose draft address verification as pending.

## Migration / security follow-up
PostgreSQL migration ownership/roles/Testcontainers remain D1-03 work. No H2 compatibility-mode result is counted as PostgreSQL PASS. Expand auth via approved feature 002 decisions; replace process-local login throttle, add revocation/refresh and production TLS/storage strategy. Demo-only names isolate this provisional data model from unapproved P0 tables.
