# P0 Identity, internal context and role resolution

Status: **PROPOSED FOR CONSOLIDATED REVIEW**, 2026-11-23. This design does not
authorize code, grants, migration, principal linking or deployment. Existing
six-READ Admin admission and the accepted shell remain in force.

## ID-P0-01: P0 has no principal linking

One browser application is not one security principal. Keep dedicated verified
SYSTEM Admin, SYSTEM IAM Root/Management, STAFF/SUPPORT and Customer identities
separate. Email/name equality, simultaneous cookies or Organization membership
never link accounts. No shared access token, grant union, common refresh family,
cookie relabeling or cross-purpose bearer acceptance.

For P0, **defer** the previously suggested GET `/api/v1/internal/contexts` and
`InternalContexts.current(AuthenticatedPrincipal)`. There is no canonical common
principal or linking authority to populate that response safely. Do not implement
an endpoint returning three invented contexts. Current Admin context discovery
already exists at GET `/api/v1/admin/dashboard/context`; Staff discovery is its
own `/api/v1/staff-auth/me`; Merchant contexts belong to Customer admission.

Current-context details and navigation may reuse those existing responses.
Cross-portal links may lead to the appropriate independent login; they do not
activate authority. Target APIs always enforce their own live admission. The
cross-context switcher remains disabled. A future linking policy needs explicit
proof of both accounts, revocation semantics and a separate manifest; it is not a
dependency for Merchant review by a dedicated SYSTEM Admin.

## ID-P0-02: actual versions and scopes

| Source | Actual authority/version | P0 treatment |
|---|---|---|
| AdminSessionPolicy | Dedicated ACTIVE/verified Admin; exactly six possible READ codes, ENTRY mandatory; same ACTIVE SYSTEM realm; SHA256 of current authVersion/grants | Preserve unchanged; Root, Customer and SUPPORT exclusions remain |
| JdbcAdminAdmission | Trusted CurrentSessionAdmission; population lock before read-only repeatable-read snapshot; callback lifetime and access deadlines | Reuse for reads; never call a mutator inside its read-only callback |
| Admin frontend authorizationVersion | Local increment when a fresh `/me` projection changes | UI invalidation only, not persisted authority or a grant version |
| Staff client/admission | STAFF principal, SUPPORT purpose, separate tokens/cookies/session tables and canonical Support grants/bindings | No Merchant governance or Finance authority |
| CustomerAdmission | Current Customer security subject/session plus sorted referenced subjects, under Identity-owned transaction | Reuse for applicant submission and contact proof; no SYSTEM replacement |
| Merchant BusinessWorkspaces | Current ACTIVE Organization/Membership/permission/scope and authorityVersion | Organization business authorization only; not Admin authority |

A SYSTEM realm UUID is an admission boundary, not an operating region. P0 introduces
no fictional region registry. Proposed Merchant review assignments use a published
**review-policy scope** and explicit Organization scope, owned by Merchant. Their
resource IDs are checked against the target, never supplied as authority by UI.
Official regional/site assignments remain a later source dependency.

## ID-P0-03 [PROPOSED]: command admission, not another authentication engine

New public Identity port:

```java
interface AdminCommandAdmission {
  <T> T execute(UUID requestId, ReferencedSubjects references,
      Function<AdminCommandContext,T> work);
}
record ReferencedSubjects(Set<UUID> customers, Set<UUID> admins) {}
interface AdminCommandContext {
  UUID accountId(); UUID realmId(); UUID sessionId(); UUID requestId();
  long authVersion(); String permissionVersion(); String environment();
  void requireCurrent();
  CustomerAdmission.Subject customer(UUID referencedAccountId);
  AdminEligibility admin(UUID referencedAccountId);
}
record AdminEligibility(UUID accountId, UUID realmId, String displayName,
    String permissionVersion, boolean eligible) {}
```

These contexts are server-created callback capabilities, never request DTOs. The
Identity implementation admits the existing SYSTEM session and unchanged six-READ
dedicated Admin policy, rejects ambient transactions, locks population → sorted
referenced accounts → sessions, and owns one READ_COMMITTED local transaction.
Merchant obtains its separate current governance assignment under its control
lock. No Identity code imports Merchant types or knows review decisions. Reads
remain on existing read-only admission. No network/SMTP/storage I/O inside TX.

Reuse the existing `CustomerAdmission.Subject` account/displayName/verifiedEmail/
securityVersion/eligible projection for locked applicants. A second Identity
recipient-discovery interface is unnecessary. Notification enqueue revalidates
that recipient against the actual callback subject; constructing the record is
not authority. Protect/redact the destination; never expose it in Admin DTOs or
persist plaintext mail recipients in receipts.

Recent step-up is a new purpose-bound public Identity port, reusing credential
verification/limiting, not the Customer proof namespace:

```java
interface AdminGovernanceReauthentication {
  StepUpResult request(AdminCommandContext actor, AdminStepUpInput input,
      String authorityVersion, UUID key);
  void consume(AdminCommandContext actor, UUID proofId,
      String authorityVersion, String operation, UUID targetId,
      long expectedVersion, byte[] inputDigest);
}
record AdminStepUpInput(String operation, UUID targetId,
    long expectedVersion, String commandDigest, String password) {}
record StepUpResult(UUID proofId, Instant expiresAt) {}
```

Workflow first acquires the current Merchant scope in AdminCommandAdmission,
then calls this joining Identity port with its server-derived authorityVersion;
Identity never resolves Merchant assignments or starts a nested admission.
Server recomputes the canonical command digest before consume; the client digest
is not authority. Proof binds actor/session/authVersion/realm/authorityVersion/
operation/target/version/input, expires after15 minutes, single use. Password is transport-only,
never persisted/logged/echoed. Consume rolls back with business failure. Exact
same-key replay first checks current admission and target authority, then the
committed receipt; it does not need an unconsumed proof or fresh If-Match. A
different input/key binding conflicts. No proof is accepted across session/purpose.

## ID-P0-04: reviewer delegation amendment

Spec003 planning describes an eligible verified human with Merchant-owned reviewer
scope and no SYSTEM-role shortcut. New product specifications place the UI in
SYSTEM Administration Console. Resolve explicitly: a dedicated SYSTEM Admin may
review only with a separately issued Merchant-owned governance assignment. Its
six READ grants remain unchanged; Root/IAM or a Merchant Owner never becomes a
reviewer. This is a **new authority amendment awaiting approval**, not a label fix.

The offline operator must be independent, explicitly provisioned for a physical
Local/Test binding, with request/digest/operator-bound audit and durable receipt.
No API self-grant, grant via Merchant role, default reviewer seed or runtime/public
EXECUTE of the offline procedure. Revoke/policy changes participate in the same
locks; they invalidate an in-flight later decision and affected snapshots.

## ROLE-P0-01: canonical vocabulary proposal

Keep Business Matrix R28–R32 byte-for-byte. Use these stable **documentary semantic
keys** for A/B instead of reusing numeric role IDs:

| Semantic key | A/B alias | Existing matrix comparison | Runtime effect |
|---|---|---|---|
| ADMIN_BUSINESS_GOVERNANCE | ADMIN_SPEC.R28 | BUSINESS_MATRIX.R28 is Pricing/Billing | None; explicit Merchant assignments required |
| ADMIN_PLATFORM_GOVERNANCE | ADMIN_SPEC.R29 | BUSINESS_MATRIX.R29 is Sales/KAM | None; no Finance/PII/IAM implication |
| ADMIN_IDENTITY_SECURITY | ADMIN_SPEC.R30 | BUSINESS_MATRIX.R30 is Risk/Compliance | Existing IAM Root/management only when actually granted |
| ADMIN_TECHNICAL_OPERATIONS | ADMIN_SPEC.R31 | BUSINESS_MATRIX.R31 is Platform Super Admin | Existing TECHNICAL READ only when granted |
| ADMIN_SCOPED_AUDITOR | ADMIN_SPEC.R32 | BUSINESS_MATRIX.R32 is IT/Integration | Read-only explicit assignments; no export or write by title |

No new `identity.account_grant.role_code`, DB role template or role-based grant
copy is proposed. Numeric IDs in historical audit remain historical; exports must
carry vocabulary/version, never reinterpret an old ID using the new labels. New
UI strings use semantic descriptions; authorization continues using permissions.

An Auditor disposition may request only `application.read`, `org.read`, `audit.read`
and `task.read`; KYC evidence disclosure needs a separate explicit approval. The
exact operator approval record must name this permission set; a title cannot
authorize another grant. No runtime Auditor role/template is introduced, so there
is no claim that a label magically blocks a separately issued write grant. Backend
denies writes because the actual current assignment lacks the required capability.
No extension of IAM, Carrier or Finance.

## Required denial evidence

Customer/Staff/Root cannot enter Merchant Admin command admission; same-email
accounts stay separate; no unknown/mixed READ code; no grant union; revoked,
expired or wrong-policy assignment denies before target/version disclosure;
reauth reuse across targets/sessions fails; current authority required on replay;
population-lock ordering defeats revoke races. Existing SYSTEM/Customer/Staff
regression and old six-READ tests must continue passing.

See [exact manifest](admin-p0-implementation-manifest.proposed.json) and
[Merchant design](admin-p0-merchant-approval-contract.proposed.md). No new linking
or generalized context-switch API is included in the implementation proposal.
