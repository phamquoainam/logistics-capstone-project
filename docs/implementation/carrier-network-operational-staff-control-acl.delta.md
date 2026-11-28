# NWS-ACL-02 — private Dispatch physical stamp [OWNER APPROVED]

Parent approved SHA-256: `8adc4229ff00e728f322dd9341cb146a82e2dc16e1470dc077ec05eb55ac4ce5`.
The manifest requires durable Dispatch receipts to bind the real PostgreSQL
system_identifier, database OID/name and authenticated operator. Identity-owned
eligibility predicates return only boolean, not that system identifier.

Exact proposed additional privileged prerequisite on NEW isolated test clusters:

```sql
GRANT EXECUTE ON FUNCTION pg_catalog.pg_control_system()
TO dispatch_network_writer;
```

No PUBLIC/runtime/offline-operator EXECUTE or writer membership. The role remains
NOLOGIN/NOSUPERUSER/NOCREATEROLE/NOCREATEDB/NOREPLICATION/NOBYPASSRLS. Functions use
the stamp internally in receipts; HTTP/operator result DTOs are unchanged. Existing
database/Identity environment binding checks still apply. No new SQL signature,
schema, accounts, demo/UAT changes or historical migration edits.

Owner approved the exact grant for isolated SOURCE/TEST only (2026-11-25).
Implemented in the owned test prerequisites, after the pre-existing AON-ACL-02
PUBLIC revoke. NWS adds no PUBLIC revoke. An explicit catalog check fails closed
if PUBLIC retains EXECUTE; runtime/operator effective access is also asserted.
No writer login, runtime/operator membership, SET/INHERIT change or grant option.

Verification: run `cskh-focused-5a0df106f2724477a84fe0681df25596` passed the
physical-stamp/ACL and operator receipt tests. Dispatch writer can read the real
system identifier; runtime and offline Operations operator cannot. Receipt joins
match request/audit/operator plus actual system identifier, database OID and name;
same-key replay has one site/evidence set, changed input and raw runtime DML deny.
This removes only NWS-ACL-02. Other foundation gates remain independently tracked.
The approved parent manifest bytes remain unchanged; execution composition is
recorded in `carrier-network-operational-staff-execution-manifest.json`.
No deployment or shared/demo/UAT database authorization is implied.
