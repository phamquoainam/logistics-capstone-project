# ADR-0002: Database Ownership and Flyway

- **Status:** ACCEPTED_FOR_D1_03 (2026-10-13, explicit user approval / DEC-26). P0 domain table contracts outside Demo-001 remain subject to their own review.
- **Source:** Chương 4 §4.3.1 proposes schema-per-module and single PostgreSQL; Flyway selected in §4.1.3. The report did not fix migration location/numbering; explicit DEC-26 approval below settles D1-03 details.
- **Accepted decision:** one PostgreSQL database for the modular monolith, nine ownership schemas, one runtime DataSource and separate owner/migrator/runtime roles. PostgreSQL 18.6 official bookworm image pinned by verified OCI digest. Flyway and JDBC versions come from Spring Boot 4.1.1 BOM; no framework version changes.
- **Risks:** FK coupling, migration coordination, technical transaction boundaries, replay/upgrade from existing environments.
- **Validation:** migrate empty+populated test DB; cross-owner query prohibited by architecture convention; no Hibernate auto-update.

## D1-03 implementation

- Local Compose project `elogistics-d1`, container `elogistics-d1-postgres18`, database `elogistics_dev`; bind `127.0.0.1:5433:5432`. New volume `elogistics_d1_pg18_dev_data` mounts `/var/lib/postgresql`; native PostgreSQL on 5432 and H2 data are independent.
- Image `postgres:18.6-bookworm@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c`; manifest reverified before pull, linux/amd64 available.
- Bootstrap administrator initializes an empty volume only. `elogistics_owner` is NOLOGIN and owns database/schemas/tables. `elogistics_migrator` may SET ROLE owner; neither migrator nor runtime is superuser/CREATEDB/CREATEROLE. Runtime is not an owner member.
- Global PostgreSQL stream: `app/backend/src/main/resources/db/migration/V1__ownership_schemas.sql`, then `V2__demo_identity_merchant_drafts.sql`. History `platform.flyway_schema_history`. Bootstrap creates only database/roles/platform prerequisite; Flyway owns domain DDL.
- Identity, merchant, orders, pricing, warehouse, dispatch, finance, routing, notification schemas exist. Only the five approved Demo-001 tables exist in identity/merchant/orders; no speculative P0 tables, cross-schema FKs or new business behavior.
- Runtime gets USAGE in implemented schemas and only required DML. Events/receipts permit SELECT/INSERT, no UPDATE/DELETE. No runtime CREATE/ALTER/DROP/TRUNCATE or history access. One runtime DB role is not a DB firewall between modules: application boundaries and architecture tests remain required.
- Activate `demo,postgres` for local demo behavior with PostgreSQL configuration last. H2 `demo` profile/location/public history and its applied V1 remain intact.
- Local/test Flyway uses separate migration credentials. Shared environments run one controlled `migration` profile job before application deployment; runtime disables Flyway and does not receive migration credentials. The one-shot job starts no HTTP server or seed and exits after successful validation.
- Clean disabled; no automatic repair/baseline/out-of-order; applied scripts immutable. No Hibernate DDL generation. No H2-to-PostgreSQL data conversion was authorized or performed.

Evidence: [D1-03 execution](../../implementation/d1-03-postgresql-evidence.md), [runbook](../../runbooks/postgresql-foundation.md). Full feature 001, architecture/CI and clean-machine reproducibility are not closed by this ADR.
