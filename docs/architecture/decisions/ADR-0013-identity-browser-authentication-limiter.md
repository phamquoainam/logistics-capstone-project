# ADR-0013: PostgreSQL shared limiter and browser authentication

Status: ACCEPTED design under OWNER_IDENTITY_SHARED_LIMITER_DESIGN and
OWNER_AUTH_HTTP_503_CONTRACT_CHANGE, 2026-10-24. Implementation verification pending.
Authority: ADR-0009, ADR-0012, accepted Identity contract and owner continuous-delivery task.
This supersedes ADR-0012's pending limiter-persistence decision only. No Recovery,
invitation compatibility, external provider, new authority or original REQ/AC approval.

## Contract and ownership

Exactly getIdentityCsrf/loginIdentity/refreshIdentity/logoutIdentity add 503 with
SERVICE_UNAVAILABLE and existing flat code/message/requestId. All previous statuses,
request/success schemas and other operations are unchanged. No default response;
strict exact-status/schema validation remains enforced. 429 is verified quota only.
503 means an indispensable security/storage operation cannot safely complete; it
never means invalid credentials or confirmed quota exhaustion. Logout 204 requires
confirmed authoritative processing; local cookie clearing alone is insufficient.

Identity owns V6__identity_shared_rate_limiter.sql. V1-V5 immutable. Two physical
tables: rate_limit_subject(policy,subject_hash,touched_at), and rate_limit_attempt
(reservation_id,policy,subject_hash,created_at,expires_at,lease_expires_at,state).
Composite subject PK/FK and reservation PK protect identity; finite policy/state,
32-byte identifier, expiry and lease constraints enforce bounds. No raw subject,
IP, secret, credential, cookie or token is persisted in the limiter.

Existing owner/migrator/runtime role conventions continue. Runtime gets only
SELECT/INSERT/UPDATE/DELETE on these two new tables; PUBLIC has no rights. No DDL,
TRUNCATE, ownership, superuser or unrelated object grants. No runtime migrator.

## Reservations and transaction state

Exact policies: LOGIN_ACCOUNT 5 failures/900s; LOGIN_IP 30 attempts/900s;
REFRESH_SESSION 30 attempts/300s; INVITATION_TOKEN 5 attempts/900s;
RESEND_ACCOUNT 3 attempts/3600s; RESEND_IP 20 attempts/3600s.
Only Login/Refresh are wired. Invitation HTTP is not completed by these constants.
CSRF GET has no newly invented quota; Logout is not rate limited.

HMAC-SHA256 over version/domain/policy/exact subject uses its own external 32-byte
key. Username is IDENTITY_EXACT_NO_TRANSFORM. Numeric servlet remote addresses are
canonicalized; no Forwarded/X-Forwarded-* or client identity establishes trust.
Direct HTTPS and exact configured Origin are required for every auth POST.

All requested subject rows are locked in deterministic policy/hash order. Database
clock_timestamp AFTER acquiring locks controls rolling windows and reservations.
All dimensions admit or roll back together. Login IP attempts count immediately;
account slots enter PENDING. A confirmed invalid credential becomes FAILED, with
15 minutes measured from DB finalization. Success releases only that request's
pending account slot; it never clears another failure. The 60-second completion
lease is bounded. A missed/crashed completion remains conservatively counted until
15 minutes after lease expiry, then ceases to block; it cannot later refund quota.
No fixed-window reset, in-memory fallback or JWT-based authority shortcut exists.

Limiter opens and commits its own physical JDBC transactions, never an ambient
Spring authentication transaction. Failed business writes cannot undo quota. No
limiter transaction/row lock remains open during BCrypt or any external network I/O.
Refresh admission may run while Session owns its transaction, but uses a separate
limiter connection and never suspends/commits caller resources. Local lock timeout
750ms and statement timeout2500ms; at most one retry, only deadlock/serialization.
Uncertain/failed commits fail closed. No blind retry of Login/Refresh or token replay.

Cleanup before reservation removes at most100 expired attempt rows plus100 old
empty subject rows, using SKIP LOCKED and FK protection. Live quotas are retained.
The explicit cleanup method can also be invoked by approved maintenance. It has no
scheduler or wall-clock deletion SLA during inactivity; no T-002-08 worker is implied.

## CSRF, cookies and lifecycle

Reuse existing V3 csrf_binding hash-only storage. Fresh independent 256-bit cookie
and challenge; only SHA256 of canonical encoded secrets enters DB. Fifteen-minute
bindings use DB time; login/logout revoke old state and rebind. CSRF GET never
rotates refresh or extends idle; a retained refresh digest locates a family only
for CSRF binding, never authenticates an account or asserts current eligibility.
This permits a fresh family-bound challenge for idempotent logout of an expired/
revoked family. Application still checks all authoritative state for refresh.

Browser obtains GET /auth/csrf before Login and again after Login/Logout rebinding;
AccessSession response is unchanged and does not invent a csrfToken field. Refresh
keeps the current challenge and replaces the opaque refresh credential; both host
cookies are returned. __Host cookies: Secure, HttpOnly, SameSite=Strict, Path=/,
no Domain. Refresh Max-Age never exceeds remaining absolute8h deadline. Access
remains RS256 at most15min, idle30min, and absolute8h, with DB revocation/Realm/
authVersion checks. /me and refresh never update idle. No Demo/cookie fallback on
the eight existing Bearer operations. Missing/duplicate headers/cookies deny.

Reuse Session Application owned transactions, strict single-flight rotation,
replay family revocation and audit rollback. A committed rotation followed by an
unavailable response is an uncertain outcome: reauthenticate, never blind retry.
No credentials are published before limiter finalization and response signing.

## Operational key and connection boundaries

identity.security.limiter-hmac is required, canonical Base64 of independently
generated32 bytes; absent/invalid/zero fails configuration with a redacted fixed
code. Never reuse JWT/invitation keys. Protected config injection only, no key
generation, developer store lookup, command-line secret, raw logging or fallback.
All instances MUST use the same key. Rotation is coordinated, never rolling mixed
keys: stop protected admissions across instances, drain completions, wait at least
the longest quota window plus lease (3660s) since last old-key reservation, then
switch all instances together. Preserve historical identifiers for normal cleanup;
do not silently reset live quotas. Emergency rotation requires operator review.

Each admitted auth request needs independent limiter/CSRF connections in addition
to Session's owned connection; pool sizing/timeout rollout must account for this.
No TLS/proxy/browser production rollout or throughput claim is made by mock tests.

T-002-05/T-002-06 PARTIAL; Feature002 and AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED unchanged. Invitation/Recovery/T-002-07/08
and business features remain excluded. Actual source/CI evidence is recorded separately.

## Implementation verification — 2026-10-24

Sourcef96df93c49f98bd098d02889de8943e6816b3509, [run40110885806](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40110885806) attempt1:
23 Architecture/0 skips,483 H2/17,758 PostgreSQL/5;0 failures/errors,128 controls/job.
Exact29 new unit and31 PostgreSQL cases, combined225 Application methods,530
contract checks/45 mutations and strict lint PASS. Artifact SHA-256/CRC/privacy/
owned cleanup PASS. [Evidence and limitations](../../implementation/t-002-06-authentication-http-evidence.md).
This later documentation revision requires separate PR integration checks. No
operational rollout, full T-002-06/Feature acceptance or new authority is implied.
