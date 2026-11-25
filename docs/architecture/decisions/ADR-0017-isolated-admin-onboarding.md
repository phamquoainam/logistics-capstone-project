# ADR-0017 — Isolated Admin onboarding and genuine first bootstrap

Status: ACCEPTED for exact Owner-approved V39/V40 Option B SOURCE/TEST only,
2026-11-22. Acceptance is not a test/deployment readiness claim.

Authority: [immutable technical manifest](../../implementation/admin-onboarding-technical-manifest.proposed.json),
SHA-256 `025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`,
plus [approved AON-ACL-01](../../implementation/admin-onboarding-staff-predicate-acl.delta.md)
and separately Owner-approved [AON-ACL-02](../../implementation/admin-onboarding-control-function-acl.delta.md).

Dedicated Admin is a new SYSTEM principal, not a converted Customer, Staff,
Merchant Owner or Initial Root. Existing user, credentials, INVITATION24h format1,
HMAC/AES-GCM, command receipts, BCrypt12, sessions, CSRF and SMTP engines remain
canonical. Admin grants use the existing account_grant table and exact six READ
codes, mandatory ADMIN_DASHBOARD_READ; the marker is purpose/provenance, not a
parallel grant engine. Root retains only its canonical Identity Management pair.

V39 adds exactly three Identity tables and finite request-bound offline functions.
Bind the physical database name/OID/system identifier and one SYSTEM realm.
Only a fresh `argopost_admin_uat_<12 lower hex>` database can be bound; restored,
copied, disabled, shared/demo or Production admission fails closed. No automatic
binding, account, grant or realm activation occurs on migration/runtime startup.

Independent NOLOGIN Admin/Realm operators and a constrained writer are prepared
outside Flyway by trusted isolated setup. Runtime/Flyway stay NOCREATEROLE;
runtime/operator have no SET/INHERIT of the writer. Offline adapter owns one
physical transaction, reserves and seals proof/outbox before commit, and prints
only finite receipt metadata. session_user membership/name/OID, current environment
and target are checked before replay; same input replays before stale versions,
different input conflicts, and replay cannot resurrect revoked authority.

V40's separately approved schema-owner wrappers apply only to a new bound
never-activated realm. They invoke unchanged V4 bootstrap/activation checks.
Protected genuine invitation acceptance activates its account, not its realm.
Cancelled bootstrap keeps the original binding and quarantines the realm;
terminal disable preserves first activation metadata and does not permit
reactivation. Admin operator never gains Realm operator or management authority.

Marked Admin account/grant/delivery provenance guards remain SECURITY INVOKER
with current_user checks. Deferred consistency/pure exclusion readers may be
definers with fixed search paths. No rejected definer/xmin guard rewrite is used.
AON-ACL-01 grants Staff writer only the boolean registration predicate.
AON-ACL-02 removes the pinned PostgreSQL raw control-function PUBLIC default
only in newly owned test databases, retaining explicit approved writer/owner
grants. It does not authorize modifying any real/shared catalog ACL.

Verification POST uses SYSTEM preauth CSRF, strict HTTPS Origin, exact body,
bounded input/rate limits, proof possession and idempotency. No session is
automatically created. The activation browser captures and scrubs the fragment
before session restore, retains it only in memory, then returns to login.
Admin/Root invitation delivery uses the existing protected outbox/SMTP engine;
worker partitions prevent ordinary IAM or Staff workers from consuming it.

ADM-02–08 six GET contracts and read-only snapshot/cohort analytics remain
unchanged. Admission now additionally requires current ACTIVE Admin marker and
dedicated physical binding. Identity Root does not acquire Carrier read rights.

All historical migrations remain byte-identical. Real deployment, operator login
membership, controlled mailbox, selected READ set, real SYSTEM activation and
Local UAT require exact target authorization. Current evidence, including closure
of both approved ACL deltas, is tracked in [execution](../../implementation/admin-onboarding-execution.md).
