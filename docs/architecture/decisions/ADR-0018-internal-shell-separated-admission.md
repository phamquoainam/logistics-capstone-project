# ADR0018 Internal web shell with separate admission

Status: ACCEPTED for Owner2026-11-23 frontend restructuring using existing
contracts; no Backend authority/schema/deployment approval is implied.

Administration Console is /admin within one InternalApp with /ops and /finance.
Reuse the existing React application, branding and nine-domain Spring artifact.
Shared InternalShell is used by Admin read surfaces and the existing SUPPORT
workbench; unavailable Operations remains denied, not a simulated business app.

SYSTEM dedicated Admin, IAM Root, STAFF/SUPPORT, Customer and Merchant admissions
remain separate. No common frontend role/session or portal switch can union their
grants. A context-discovery/switch contract and resource scopes require controlled
amendment. Existing proof capture, cookies, CSRF, current permission checks and
snapshot logic are preserved. Missing sources/authority are explicit states.

See [architecture](../../implementation/internal-web-admin-architecture.md),
[role reconciliation](../../implementation/admin-console-role-capability-mapping.md),
[unapproved deltas](../../implementation/admin-console-contract-amendment.proposed.md)
and [actual evidence](../../implementation/admin-console-implementation-execution.md).
