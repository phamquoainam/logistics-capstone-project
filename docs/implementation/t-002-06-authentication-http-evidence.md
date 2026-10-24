# T-002-06 Authentication HTTP and shared limiter evidence

Owner: OWNER_AUTH_HTTP_503_CONTRACT_CHANGE APPROVED and
OWNER_T_002_06_AUTH_HTTP_CONTINUATION CONDITIONAL_GO, 2026-10-24.
State: IMPLEMENTATION_VERIFICATION_PENDING. No current PostgreSQL/CI PASS claimed.

Baseline46920c040b9cd2e78f99efee7718015dff69dc89; PR27 merge;
develop push40105487979 attempt1 verified23 Architecture,454 H2/17 skips,
698 PostgreSQL/5 skips;125 controls/job, artifact digest/privacy/owned cleanup PASS.
Reuse clean HTTP worktree on feature/002-identity-authentication-http; prior branch
and ignored evidence retained. Six original Foundation fingerprints and four prior
Activation/Session/Current Session/Remaining Application worktrees remain protected.

Scope: exactly CSRF/Login/Refresh/Logout, V6 limiter, dedicated external HMAC,
secure cookies, authoritative Session/replay/rollback and strict report inventory.
[Accepted design](../architecture/decisions/ADR-0013-identity-browser-authentication-limiter.md).
Four operations add503/SERVICE_UNAVAILABLE only; no original REQ/AC change.

Verification and actual source SHA/run/artifact references will be appended after
execution. Auth servlet/filter-chain tests use synthetic TLS metadata; no production
TLS/key rollout, browser E2E, invitation delivery, Recovery or business readiness.
T-002-05/T-002-06 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Main traceability remains protected.
