# ADR-0012: Bounded Production Identity HTTP security

Status: ACCEPTED for the owner-approved security direction; implementation/CI acceptance is scoped below.
Owner: OWNER_T_002_06_SECURITY_DESIGN: CONDITIONAL_GO, 2026-10-24.
Authority: ADR-0009/0010/0011, accepted contracts/openapi/identity-access.yaml and Feature 002.
Original REQ/AC, V1-V5, Demo trust and GRANT_MUTATION_FUNCTIONALITY_LIMITED are unchanged.

## Approved decisions

Production access uses RS256 only, pinned in signing and verification, explicit configured kid,
issuer and single audience. Require RSA at least3072 bits, matching active private/public key,
up to16 explicitly named verification keys, no remote key discovery or startup key generation.
Reject none, HMAC/RSA confusion, unknown kid, unexpected JOSE headers/claims, duplicate JSON,
invalid signature, malformed identifiers, missing/fractional timing/version claims and expiry.
Access is at most900 seconds, additionally capped by authoritative Application deadlines.
This revision uses zero clock skew: issue/nbf cannot be in the future; expiry is exclusive.

Configuration is an external protected JWK set, issuer, audience, current-kid and exact HTTPS
origin. Retiring keys are public-only and never sign; retain verifiers through the last legitimate
expiry across ALL instances (900 seconds after last issuance plus deployment/clock uncertainty).
Rolling rotation and emergency key removal are operator-controlled, not a public management API.
See the [deployment/retention runbook](../../runbooks/production-identity-security.md).

CurrentSessionAdmission is created only inside the same validated request scope. Signature and
claims are necessary, never sufficient: the Application locks/rechecks current session owner,
account/status/verification, authVersion, revocation, idle/absolute deadlines and ACTIVE exact
Identity Realm authority. JWT NumericDate has integer seconds; PostgreSQL timestamps have
microseconds. A read-only runtime lookup binds session/account/version and resolves creation
precision ONLY within the identical issuing second. An earlier issuing second is rejected;
no Application/Domain deadline, transaction ownership or grant policy is weakened.

Meaningful activity is server-attested only after a NEW successful disable/reactivate/grant
replacement. Exact receipt replay, failed mutation, read/list/catalog and /me polling do not
qualify. Business mutation/audit/receipt remains one existing owned transaction; idle accounting
is a subsequent existing Session-owned transaction. A failure after business commit produces an
uncertain response; the accepted same-key receipt flow resolves the business outcome without
repeating it. There is no ambient/caller-owned commit or automatic retry.

Bearer-only administration does not authenticate from cookies. No CORS origin wildcard or
forwarded-header trust is installed. Direct HTTPS is required; supplied Origin must equal the
configured origin. Reverse-proxy TLS/address trust requires explicit operator-reviewed proxy
configuration, not X-Forwarded-* values accepted directly from clients.

## Approved limiter policy; persistence decision still required

- Login:5 failures/account/15min and30 attempts/IP/15min.
- Refresh:30 attempts/session/5min.
- Invitation activation:5 attempts/token/15min.
- Resend:3 requests/account/hour and20 requests/IP/hour.

These thresholds are ACCEPTED, NOT implemented or verified in this revision. Atomic enforcement
must work across instances, hash sensitive identifiers, distrust forwarded addresses, fail closed
on unavailable storage and preserve declared429/Retry-After behavior. In-flight login attempts
must reserve failure budget so concurrency cannot exceed the account budget; successful attempts
release only their own reservation and cannot erase concurrent failures.

Existing Redis foundation_probe has no write/script/key authority. Do not widen that credential
or modify developer Redis. A dedicated narrowly scoped Redis application ACL is one option.
An Identity-owned forward migration for limiter counters/reservations is another. No concrete
new table/privilege model has yet been approved under AGENTS.md's proposed-table gate; no V6 or
ACL mutation is performed here. Owner must select/approve that bounded deployment/model scope.

## Invitation compatibility qualification

Owner approves v1.<selector>.<verifier> inside the EXISTING opaque token field. Selector locates
one proof and is NEVER authority; retained HMAC, purpose, expiry and atomic consumption remain
mandatory. Existing issuance protects a canonical43-character verifier in V5; its HMAC binds the
proof/account/times/key, so the raw verifier cannot be indexed by hashing it alone. HTTP currently
has no accepted locator field/resolver. Existing invitations are NOT rewritten or invalidated.

Small compatible forward direction: future authorized delivery can form the v1 envelope from its
already known proof ID and existing protected verifier, preserving V5 crypto/storage. This does
not safely resolve already delivered bare verifiers. A separately approved transition/resolver
for those invitations is needed; no all-proof scan, delivery decryption at HTTP admission, legacy
unkeyed fallback, new wire field or silent expiry assumption is permitted. Activation HTTP stays
uninstalled. This does not change the accepted Application activation command or its regressions.

## Exact implementation boundary

Installed, opt-in identity,postgres profile (demo/migration combinations fail configuration):
getIdentityMe, listIdentityAccounts, getIdentityAccount, listIdentityRoles, listIdentityScopes,
replaceIdentityGrants, disableIdentityAccount and reactivateIdentityAccount.
Use exact accepted paths/DTOs/errors, no-store/request IDs/strong ETags and unchanged complete
current-target authority. Authorization precedes target precondition disclosure; supplied actor,
session, authVersion, activity flags or alternative credential channels cannot establish authority.

Not installed: login/refresh/logout/CSRF bootstrap, provisioning/resend/activation HTTP and both
Recovery operations. Browser authentication requires the unresolved shared limiter/admission
configuration. Provisioning/resend also retain ADR0011's production issuance/maintenance gate;
T-002-08 sender/maintenance is not implemented here. Recovery needs its own approved storage/crypto
forward decision. No fake endpoint, provider, secret or success response substitutes for these.

T-002-06 PARTIAL; full HTTP/task DoD and Feature002/AC-002-01..05 are NOT ACCEPTED. Tests are
actual servlet/filter-chain fixtures with synthetic TLS metadata and owned PostgreSQL; this is
not evidence of an operated TLS/reverse-proxy deployment or browser E2E.

## Implementation verification metadata ? 2026-10-24

Eight-operation source1962a839fce6e601d0f541e094a1dc9d2c663c05 is CI_VERIFIED by
[run40103151184](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40103151184):
Architecture23/0 skips, H2454/17, PostgreSQL698/5,125 report controls/job; exact inventories,
artifact digests, credential/privacy and owned cleanup PASS. This verifies the scoped implementation,
not an operational key rollout, complete TLS/proxy deployment, shared limiter, legacy invitation
transition or full T-002-06. Accepted policy and all open prerequisites above are unchanged.
