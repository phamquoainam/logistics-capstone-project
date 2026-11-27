# BIA-OWNER-01 [PROPOSED] — repair pre-existing V46 table ownership

2026-11-27. Isolated SOURCE/TEST only. Not approved or implemented.

## Evidence and root cause

Focused PostgreSQL18 run `623c8b70941047b7ac0d2535e76f6ca6` passes the
complete 47-migration cold stream and V43→V48 data/checksum preservation, but
`schemasAndObjectsBelongToOwner` fails. The exact table
`merchant.application_review_comment` belongs to the disposable database's
LOGIN migrator, rather than its canonical NOLOGIN schema owner.

V46 switches to `merchant_governance_writer` to replace an existing operator
definition, then executes `RESET ROLE` before creating the table. Flyway's
connection had entered the canonical owner using `SET ROLE`; RESET restores
the session's migrator instead. V46 bytes and its accepted history remain
untouched. This is not an intake timestamp or Business ownership variant issue.

## Exact requested correction

One forward-only migration, number **unreserved** until fresh branch/PR/registry
preflight. Its only persistent mutation is:

```sql
ALTER TABLE merchant.application_review_comment OWNER TO <canonical NOLOGIN owner>;
```

Resolve that identifier from `pg_namespace.nspowner` for the existing `merchant`
schema, never from client input or a hardcoded developer role. Before mutation:

- Require the expected existing relation and canonical NOLOGIN schema owner.
- Require the table's current owner to be that canonical owner (idempotent
  no-op) or the current migration `session_user`; otherwise abort on drift.
- Require migration execution under the existing canonical owner and existing
  migrator membership. Do not grant membership, CREATE, SET ROLE or EXECUTE.
- Use identifier quoting for the catalog-derived owner. Preserve every table
  row, column, constraint, trigger, index and explicit ACL.

No new API, permission, role, table, function signature or runtime grant. No
shared Team/Identity/Notifications implementation change. No V1–V48 rewrite,
Flyway repair, demo/UAT deployment or real-account mutation.

## Focused acceptance after approval

Run populated V46/V48 forward-upgrade preservation and cold migration tests;
verify all table owners, existing SELECT/INSERT ACL and denied UPDATE/DELETE;
verify review-comment guard/function ownership and row preservation. Confirm
Flyway validation and replay apply zero additional migrations. Keep the recorded
ownership FAIL as historical evidence; do not whitelist the LOGIN owner.

This proposal does not approve BIA-TIMESTAMP-01, complete BIA-V1 security gates,
or resume Menu2 Step B. Until approved and tested, the ownership gate stays FAIL.
