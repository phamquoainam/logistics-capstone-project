# ADR-0002: Database Ownership and Flyway

- **Status:** PARTIALLY_ACCEPTED_BY_REPORT + PROPOSED_MIGRATION_DETAILS
- **Source:** Chương 4 §4.3.1 explicitly proposes schema-per-module and single PostgreSQL; Flyway selected in §4.1.3. Exact migration location/numbering remains unconfirmed.
- **Decision proposal:** one unified Flyway version stream; app DB runtime without DDL; `validate` for ORM; owner reviews cross-schema DDL and FK.
- **Risks:** FK coupling, migration coordination, technical transaction boundaries, replay/upgrade from existing environments.
- **Validation:** migrate empty+populated test DB; cross-owner query prohibited by architecture convention; no Hibernate auto-update.
