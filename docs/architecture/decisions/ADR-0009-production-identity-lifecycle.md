# ADR-0009: Production Identity lifecycle — reconstructed decision record

- Status: ACCEPTED for DEC-03/DEC-04 design direction and bounded O1–O7, by explicit owner authorization on 2026-10-20.
- Provenance: task “FEATURE-002 — RECOVER SDD BASELINE & REBUILD PRODUCTION IDENTITY CONTRACT”, OWNER_FEATURE_002_BASELINE_RECOVERY_APPROVAL: GO. This is a new record, not byte-identical restoration of the deleted worktree.
- Authority: [Feature002](../../../specs/002-identity-access/spec.md), [security](../security.md), [ownership](../data-ownership.md), [principles](../../principles.md).
- Scope: browser Production Identity contract only. No Java/runtime, SQL, provider, frontend, SDK or Foundation implementation is approved here. Demo-001 trust boundary and eleven operations remain unchanged.

## Context and alternatives
DEC-03 formerly blocked server session cookie versus access/refresh JWT. Owner selects JWT_ACCESS_15M_WITH_ROTATING_REFRESH_AND_BACKEND_REVOCATION: short browser in-memory access JWT with opaque refresh cookie and backend-controlled sessions, rather than a long-lived stateless bearer or a single session-cookie architecture. This requires revocation lookups, CSRF state and transactional rotation; a JWT signature alone never establishes current authority.

DEC-04 selects ADMIN_CONTROLLED_PROVISIONING_WITH_VERIFIED_EMAIL_INVITATION_AND_RECOVERY, rather than public registration or password sharing. Admin bootstrap is controlled, no public registration/bootstrap endpoint, no default Finance or operational authority. Development/test delivery uses a sandbox; production mail provider remains unapproved.

## Accepted policy
- Access at most900 seconds; absolute session cap28800 seconds; meaningful-activity idle1800 seconds. JWT expiry cannot exceed session deadlines; backend enforces current session/account/grants even before JWT expiry.
- Opaque refresh hash storage, atomic rotation, strict replay revokes family; no replay grace/blind retry. Client single-flight; unrecoverable lost response requires reauthentication.
- Logout, account disable, password reset and grant changes revoke affected sessions. Refresh, CSRF, health and auth/me polling do not extend idle.
- Invitation single-use24h; recovery single-use15min via verified email. Proof consumption, credential update, session invalidation, audit and command receipt are atomic; delivery is outside transaction through durable sandbox intent/outbox.
- O1: /api/v1 resource-object responses and stable flat errors for Identity only. Global DEC-25 stays PROPOSED; ADR-0003 is not edited or globally promoted.
- O2: server-bound preauth/session CSRF state, cookie binding plus explicit challenge header, exact-Origin checks on browser auth POST. Host-only Secure/HttpOnly/SameSite=Strict/Path=/ cookies; login/logout rebind state. CSRF GET never rotates refresh.
- O3: strict refresh and idle rules above; no refresh idempotency cache/grace.
- O4: passwords unchanged, no trim/normalize/truncate. New minimum15 Unicode code points AND maximum72 UTF-8 bytes before BCrypt; login does not enforce the new minimum on legacy credentials.
- O5: entire-target authority, including union of current/requested grants for full replacement; no scope-A manager removing scope-B grants. Identity owns transaction/audit/session invalidation; public resource-owner APIs establish assignability, absent provider denies.
- O6: no self-disable/self-change-grants; concurrent last-active Identity Management Admin protection. Reactivation only previously activated verified DISABLED accounts; no old sessions restored, disabled-pending never automatically activated.
- O7: malformed precondition400, required missing428, stale non-replay412, business/idempotency conflict409. Current authority precedes disclosure/replay; same-key bound receipt precedes stale-version rejection.

## Contract details and approval boundary
The [contract README](../../../contracts/identity-access/README.md) proposes exact routes, DTOs, errors, ETags, cookie names and exact no-transform username identity. These are newly reconstructed implementation candidates for focused review, not facts recovered from missing source or new global policy. Username consistency preserves exact case/Unicode sequence and never rewrites existing Demo accounts.
Mobile secure storage/refresh transport, production mail provider, detailed bootstrap deployment, signing-key operation/retention, rate-limit thresholds and domain authority integrations remain separately specified before affected implementation. They do not permit a public registration path or an authority fallback.

## Verification and consequences
T-002-01 design-decision DoD is reestablished by this owner approval and record; runtime smoke is NOT claimed. T-002-02 has fresh schema/mutation/fixture and strict Redocly gates, plus candidate review. Production AC-002-01–05 remain unaccepted. See [fresh evidence](../../implementation/t-002-02-identity-contract-evidence.md) and [supplemental trace](../../quality/feature-002-recovery-traceability.md). The protected main traceability record is unchanged.
