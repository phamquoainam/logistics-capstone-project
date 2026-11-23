# Admin onboarding and isolated Local/Test activation delta

Status: **[PROPOSED] - consolidated technical approval required**. 2026-11-22.
This document does not authorize implementation, migration, role creation,
provisioning or activation. Previous ADM-01-08 source/test acceptance is retained.

## Current authority and first incomplete task

Owner directs completion of real onboarding/UAT, but explicitly requires approval
before adding provisioning authority/API/schema. AGENTS.md also requires approval
of proposed paths/DTOs/tables. First incomplete task is **AON-01: close that exact
authority gap**, not repeat the completed Dashboard implementation or its suites.

Plan: inspect/preserve -> exact consolidated manifest -> deployment/rollback/UAT
runbook and coverage -> verify document integrity -> one Owner technical decision.
Only after that decision implement the approved delta with TDD and isolated PG18.
No privileged source or migration is included in the current documentation diff.

## Evidence for the gap

1. `IdentityRealmGrantPolicy.resolveProvisioning` permits only a new pending target
   with the exact `IDENTITY_MANAGEMENT/IDENTITY/ACTIVE_SYSTEM_REALM` pair. It cannot
   issue the six approved Dashboard read codes; changing the requested array or
   using a management actor does not authorize them.
2. `JdbcIdentityInvitationStore.eligibleRealm` and `JdbcInvitationOutbox.ELIGIBLE`
   require the current exact management pair and ACTIVE realm. Admin read grants
   cannot currently receive/accept a valid invitation through those adapters.
3. `LocalSupportStaffProvisioner` is dedicated to STAFF/SUPPORT. Its separate
   marker, purpose and grant authority must not be relabeled as SYSTEM Admin.
4. The accepted V4 `bootstrap_system_realm_admin`/`activate_system_realm` functions
   exist and remain unchanged. They create a new pending Initial Root and later
   require genuine verification/credential activation. The runbook explicitly
   stops before activation if that real initial verification flow is missing.
   Current ordinary invitation eligibility requires ACTIVE realm, so there is
   no proven real pre-activation bootstrap invitation path. Test helpers that
   mark an account verified cannot close this gap.
5. Admin source admission defaults off and has no grant-issuance authority.
   Existing six GET APIs are not onboarding endpoints.

## Smallest bounded core: AON-CORE / proposed V39

The exact signatures, columns, guards, roles, DTOs and tests are in
[the manifest](admin-onboarding-technical-manifest.proposed.json).

- Reuse Identity user_account, account_grant, INVITATION24h/format1, HMAC/AES-GCM,
  PasswordPolicy, credential hashing, command receipts, audit, SYSTEM session,
  browser CSRF and the approved JavaMailSender transport. No new auth engine,
  proof purpose, permission code, session table or storage/mail provider.
- Add three Identity tables: dedicated isolated environment binding, dedicated
  Admin principal marker and request-bound offline operator receipts. Existing
  ACTOR receipts require a human Identity actor and cannot impersonate session_user.
- New offline operator can provision ONLY a fresh pending Admin, explicitly
  selected READ codes including ADMIN_DASHBOARD_READ, in an ACTIVE registered
  SYSTEM realm. IDs/login username are generated. No conversion, default all-six,
  IDENTITY_MANAGEMENT, Merchant, Support, Carrier write or Finance write grants.
- New public verification POST and `/admin/activate` are invitation acceptance,
  not registration. Existing SYSTEM preauth CSRF/Origin/TLS, opaque proof and
  idempotency apply. Activation creates no session and redirects to login.
- Exact operator status, resend/cancel, permission replacement and terminal revoke
  are audited/idempotent. Current admission and current version are enforced;
  receipt replay cannot revive disabled authority, account or proof.
- Marked targets have writer/provenance/deferred consistency guards. Current
  grant storage remains canonical; the marker is not a second permissions engine.
- Dashboard admission additionally checks the current ACTIVE marker and dedicated
  physical binding. This is explicit proposed hardening; old IAM/Root/Staff/
  Customer behavior and the six approved GET wire contracts remain unchanged.
- Core Admin operator/writer cannot activate Realm or write management grants.

V39 is currently unused in working files (37 historical migrations through V38,
V33 absent). It must be rechecked immediately before future implementation. No
V39 file, placeholder or database change was created in this checkpoint.

## SYSTEM prerequisite: one explicit choice in the same review

**Option A:** use an already genuinely verified/ACTIVE, separately approved isolated
SYSTEM realm. Existing Root and management grants are untouched. If no such
environment exists, do not claim readiness or restore a fixture as a real realm.

**Option B, separately sensitive:** approve optional **AON-REALM / proposed V40**
within the same manifest for a brand-new dedicated isolated database only. A
distinct offline Realm operator issues a real verification invitation for the new
V4-bound Initial Root, completes the existing credential/proof engine, and invokes
the unchanged V4 activation checks. Bounded schema-owner definer wrappers are
explicitly listed; they are not granted to the Admin operator or runtime.

This is an explicit exception for a NEW isolated Initial Root's canonical
management pair, NOT permission to grant management authority to a Dashboard
Admin. It needs its own affirmative decision in this single technical review.
Existing Root records, management grants and shared/demo realm remain untouched.
No relaxed global INACTIVE-realm eligibility, rebootstrap or reactivation.
Terminal realm disable is included for rollback, retaining its first-activation
metadata and bootstrap binding. A cancelled initial bootstrap is quarantined,
never silently reassigned to another account.

If neither option is authorized/available, AON core source/test may proceed after
technical approval, but real deployment remains BLOCKED_SYSTEM_PREREQUISITE.
No direct DML, broad schema-owner CLI or synthetic verification is a fallback.

## Environment, operator and selected READ set

Only a dedicated database named `argopost_admin_uat_<12 lower hex>` is eligible;
the execution record must pin actual cluster/container/volume/database/OID/system
identifier/realm. `elogistics_dev`, shared services and Production are excluded.
No new worktree or repository is needed. Temporary Docker resources are not
created until exact new authority and isolated deployment prerequisites pass.

Operator DB login membership must be independently authorized. No automatic grant
to developer/migrator/runtime, no public EXECUTE, no writer SET/INHERIT, no GUC-only
authority. Existing pinned CI ownership/privacy/bootstrap protocol is preserved.

Owner must select the READ set at provisioning; do not assume all six. For an
initial shipment/fee demonstration the minimal candidate is ENTRY + SHIPMENT_READ
+ FINANCE_READ. TECHNICAL_READ is separate; MERCHANT_READ/NETWORK_READ currently
only expose honest source availability, not implemented business review/network.
This is a recommendation, not a permission grant or a selected target account.

## Scope and proof of completion

All new objects are PROPOSED. Exact implementation tests AON-T-01-10 and real UAT
are NOT RUN. Existing ADM PASS evidence remains historical source/test evidence.
Required real email receipt, genuine verification, SYSTEM login, live cohort
comparison, negative authority and rollback must be observed on the approved
environment before `ADMIN_DASHBOARD_LOCAL_UAT_READY`.

See [technical review](admin-onboarding-technical-review.md),
[deployment/UAT runbook](admin-local-readiness-runbook.md) and
[coverage matrix](admin-local-functional-coverage.csv).
