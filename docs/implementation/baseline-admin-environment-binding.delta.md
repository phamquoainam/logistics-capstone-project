# BRC-ACL-01 [PROPOSED] — portable isolated Local/Test environment binding

2026-11-27 · exact baseline blocker; no mutation applied by this proposal.

Cold Compose successfully migrated the48 approved migrations through V49.
Backend then failed CUSTOMER_RUNTIME_CONFIGURATION_INVALID. Source root cause:
`JdbcCustomerBusinessContacts.requireSchema` requires an actual
`identity.local_admin_environment_matches('LOCAL'|'TEST')` binding for V41
Business Contact; the schema alone is insufficient. This check must remain.

Existing `identity.bind_local_admin_environment(UUID,UUID,TEXT,TEXT)` requires
session_user membership in canonical `elogistics_local_admin_operator` and denies
superuser/CREATEROLE/CREATEDB/BYPASSRLS. The migrator correctly lacks this
membership. Neither SET ROLE owner nor temporary SQL may bypass the check.

## Exact proposed source/test bootstrap delta

Only in a NEW root-Compose-owned isolated PostgreSQL18 cluster, its deterministic
private bootstrap may create one PostgreSQL login `argopost_local_admin_operator`:

```sql
CREATE ROLE argopost_local_admin_operator LOGIN NOSUPERUSER NOCREATEDB
 NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD '<generated protected secret>';
GRANT elogistics_local_admin_operator TO argopost_local_admin_operator
 WITH INHERIT TRUE, SET FALSE;
GRANT CONNECT ON DATABASE <exact-generated-local-db> TO argopost_local_admin_operator;
```

No membership in identity_admin_writer, realm operator, Identity Management,
Staff/Network/Finance/Carrier/Merchant operator or schema owner. No grant option.
Preserve canonical role function EXECUTE allowlist; add no SQL function/API/DDL
migration or runtime permission. A separately isolated one-shot binding job
receives only this operator's generated password and non-secret installation
IDs, never runtime/Frontend credentials. Backend receives none of its credentials.

The job calls only existing canonical ENV_BIND with installation UUID/request,
the V4-generated SYSTEM realm ID, LOCAL and a fixed baseline reason. Replays use
the same request/operator/input and must create no additional receipt/audit.
Binding does NOT activate SYSTEM or provision Root/Admin/Staff/Customer identities,
issue application permissions or create synthetic business events.

Applies as a reproducible bootstrap template to future NEW isolated developer
Compose targets, not any existing demo/shared/Admin-UAT/Production cluster.
Existing named volumes lacking this authority must stop with a precise blocker;
never amend their roles implicitly. The failed first bootstrap is preserved.

Mandatory checks: actual role attributes/memberships and absence of writer SET;
runtime/operator separation; real physical DB/realm binding; same-key replay;
SYSTEM remains INACTIVE/no accounts or grants; role and48 migration owner/ACL/
checksum inventory; Backend startup and Business Contact path; no real SMTP/AWS.

Approval is required before implementing this persistent PostgreSQL operator
login/membership. Ordinary Docker/config work and independent S1 regressions
continue. This is not permission for actual user onboarding or SYSTEM activation.
