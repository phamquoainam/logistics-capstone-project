# Production Identity contract — reconstructed candidate

Current technical acceptance (2026-10-21): T-002-02 DONE under OWNER_T_002_02_FINAL_VERIFICATION_APPROVAL. The final review corrected logout/recovery and verified the exact contract SHA-256 acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128 with512 contract checks and strict Redocly2.64.0 on Linux: exit0,0 errors/0 warnings/0 ignored. Earlier candidate/review-pending wording below records reconstruction context; it is superseded for this exact technical contract revision. No backend/API or production acceptance is implied. [Final evidence and T-002-03 handoff](../../docs/implementation/t-002-02-identity-contract-evidence.md#t-002-02-final-review-and-acceptance-2026-10-21).

This is a fresh T-002-02 candidate under [ADR-0009](../../docs/architecture/decisions/ADR-0009-production-identity-lifecycle.md), not the deleted historical artifact. No 187-test historical result is reused. The [OpenAPI3.1 document](../openapi/identity-access.yaml) uses JSON syntax, a valid YAML1.2 subset; it describes proposed production routes, not existing controllers.

## Scope and source of truth
Identity owns accounts, credential/session/token-family hashes, role grants, receipt and append-only audit. Merchant/warehouse/hub/dispatch/finance remain resource owners. Access to public authority APIs only; unavailable authority fails closed. No cross-module SQL or automatic Finance/Operations grant. Demo-001 /api/demo/v1 and generated SDK remain untouched. This task does not generate a production SDK or add runtime endpoints.

Current owner accepted DEC-03/04 and O1–O7; exact DTO/route/error choices below remain a focused-review candidate. Global envelope/event policy DEC-25 remains PROPOSED. [Canonical REQ/AC](../../specs/002-identity-access/spec.md) are unchanged.

## Session/CSRF handshake
1. Same-origin browser GET /auth/csrf returns no-store challenge and server-bound __Host-elogistics.csrf cookie. HttpOnly cookie is not read by JS; JS keeps the returned challenge in memory. No wildcard CORS; reject untrusted/null supplied Origin.
2. Browser authentication POST requires exact configured Origin, X-CSRF-Token and cookie-state proof together. Login/invitation/recovery use preauth binding; refresh also binds current refresh family. Logout binds the family when the optional refresh cookie is present and requires valid preauth CSRF when it is absent. A mere arbitrary cookie/header equality is insufficient.
3. Login rebinds CSRF to a new session and sets __Host-elogistics.refresh. Refresh strictly rotates opaque token, not through GET CSRF. Logout always requires exact Origin and valid server-bound CSRF. With a refresh cookie it also validates family binding before revocation/clearing; after the cookie is gone, valid preauth CSRF still permits an idempotent204. Logout rebinds fresh preauth CSRF; repeat calls never restore a session. Cookie absence is not a CSRF exemption.
4. Both cookies: host-only (no Domain), Secure, HttpOnly, SameSite=Strict, Path=/; distinct Set-Cookie fields, never comma-fold. Access JWT and CSRF challenge stay in memory. Production TLS required. No refresh token in JSON/URL/log.
5. JWT maximum15min (less near deadline); absolute8h/idle30min backend enforcement. Only accepted meaningful user activity advances idle; refresh/CSRF/health/me polling never does. Client single-flight refresh; strict replay revokes family, no grace or blind retry after lost response. Reauthenticate instead.
6. Validate production signature/issuer/audience/sub/jti/sid/iat/nbf/exp/authVersion AND current session/account/grants. Key/issuer/audience domain must be separate from Demo-001; no demo-token fallback. Algorithm/key deployment config needs implementation review; no cryptographic runtime proof in this contract.

## Credentials and lifecycle
New passwords: at least15 Unicode code points, at most72 UTF-8 bytes before BCrypt; reject malformed surrogate sequences. No trim/normalize/truncate. JSON Schema minLength counts code points, not UTF-8 bytes; credential fixtures add a contract-policy byte checker. This is NOT Java BCrypt verification.
Login accepts shorter legacy passwords within byte bound; disabled/unknown/wrong-password share INVALID_CREDENTIALS. Exact username sequence is identity: no trim/casefold/Unicode normalization; nonblank1..80 code points for this candidate, consistently used at provision/login/recovery. No implicit migration, renaming or import of Demo accounts; collision/legacy-data analysis belongs to T-002-03 review.
Provision creates PENDING_ACTIVATION;24h one-use verified-email invitation; consumption alone can activate eligible pending account. Recovery15min one-use to already verified email; known/unknown/disabled/unverified requests share202 shape. Timing/non-enumeration/throttling are future runtime gates.
Recovery request idempotency is scoped to server preauth binding + operation + key before any account lookup, with a username/email fingerprint and equivalent receipts for eligible/ineligible inputs. Changed payload409 applies uniformly. Submitted email is only a matcher: deliver solely to the stored verified address when it matches; otherwise same202/no delivery. Receipts/audit never store plaintext password or proof. These are contract requirements, not executed HTTP/DB outcomes.
Disable preserves whether account had ever activated. Reactivation only for previously activated verified DISABLED account; disabled-pending cannot activate or receive a resend. Old sessions/invitations never revive. No self-disable/grant-edit and no concurrent loss of last active Identity Management Admin.

## Authorization / transaction / replay
Admin account read requires authority over ALL target grants before exposing contact, ETag or membership. Listings filter before pagination; no leaked global counts. Full replacement checks union of existing/requested scopes/roles; global disable/reactivation/resend check whole target. Absent/unauthorized target uses same404. No any-match scope shortcut.
Grant/credential/lifecycle changes, version, ALL affected-session revocations, proof consumption where applicable, receipt and audit commit together inside Identity. Last-admin invariant must withstand concurrent transactions. Delivery intent/outbox is committed with command; real email is outside transaction. Domain public events may be designed later only with real subscribers; this task introduces no event schema/provider.
Idempotency-Key UUID required on provisioning/grants/lifecycle/invitation/recovery commands. Scope binds authenticated actor or verified proof subject, operation, target and canonical input including original If-Match; changed binding409. Recheck current authority and proof identity without requiring an unconsumed proof for a valid receipt replay. Same key replay must not repeat credential/outbox/audit effect. No raw secret-bearing response cache. Retention at least applicable proof lifetime is a candidate; detailed retention/storage policy remains an implementation-review dependency.
If-Match strong ETag format "vN": malformed400, absent428, stale non-replay412, business/idempotency409. Authorization precedes existence/precondition disclosure; valid authorized receipt replay precedes stale current version. No refresh idempotency exception that weakens strict replay.

## Operation inventory / stable errors
Paths below are relative to /api/v1. Success fields are resource objects; errors are flat code/message/requestId with optional field/code entries, never submitted values.400 identifies safe invalid fields;401 never reveals password/account status;404 masks unauthorized target.429 carries Retry-After. Every modeled response has Cache-Control:no-store and X-Request-Id. Account reads/commands return strong ETag; create also Location; session transitions carry specified Set-Cookie.

| operationId | Method/path | Request → success | Required conditional/key | Errors |
|---|---|---|---|---|
| getIdentityCsrf | GET /auth/csrf | none → 200 CsrfChallenge | none | 403:ORIGIN_REJECTED; 429:RATE_LIMITED |
| loginIdentity | POST /auth/login | LoginRequest → 200 AccessSession | none | 400:VALIDATION_FAILED; 401:INVALID_CREDENTIALS; 403:CSRF_REJECTED/ORIGIN_REJECTED; 429:RATE_LIMITED |
| refreshIdentity | POST /auth/refresh | none → 200 AccessSession | none | 401:SESSION_INVALID/REFRESH_REPLAY; 403:CSRF_REJECTED/ORIGIN_REJECTED; 429:RATE_LIMITED |
| logoutIdentity | POST /auth/logout | none → 204 empty | none | 403:CSRF_REJECTED/ORIGIN_REJECTED |
| getIdentityMe | GET /auth/me | none → 200 Account | none | 401:SESSION_INVALID |
| requestIdentityRecovery | POST /auth/recovery | RecoveryRequest → 202 Accepted | Idempotency-Key | 400:VALIDATION_FAILED; 403:CSRF_REJECTED/ORIGIN_REJECTED; 409:IDEMPOTENCY_CONFLICT; 429:RATE_LIMITED |
| confirmIdentityRecovery | POST /auth/recovery/confirm | RecoveryConfirmRequest → 204 empty | Idempotency-Key | 400:VALIDATION_FAILED; 403:CSRF_REJECTED/ORIGIN_REJECTED; 409:IDEMPOTENCY_CONFLICT/TOKEN_INVALID_OR_EXPIRED; 429:RATE_LIMITED |
| acceptIdentityInvitation | POST /auth/invitations/accept | InvitationAcceptRequest → 204 empty | Idempotency-Key | 400:VALIDATION_FAILED; 403:CSRF_REJECTED/ORIGIN_REJECTED; 409:IDEMPOTENCY_CONFLICT/TOKEN_INVALID_OR_EXPIRED/INVALID_ACCOUNT_STATE; 429:RATE_LIMITED |
| listIdentityAccounts | GET /admin/users | none → 200 AccountPage | none | 400:VALIDATION_FAILED; 401:SESSION_INVALID; 403:AUTHORITY_REQUIRED |
| provisionIdentityAccount | POST /admin/users | ProvisionRequest → 201 Account | Idempotency-Key | 400:VALIDATION_FAILED; 401:SESSION_INVALID; 403:AUTHORITY_REQUIRED; 409:USERNAME_CONFLICT/IDEMPOTENCY_CONFLICT |
| getIdentityAccount | GET /admin/users/{accountId} | none → 200 Account | none | 400:VALIDATION_FAILED; 401:SESSION_INVALID; 404:ACCOUNT_NOT_ACCESSIBLE |
| replaceIdentityGrants | PUT /admin/users/{accountId}/grants | ReplaceGrantsRequest → 200 Account | Idempotency-Key, If-Match | 400:VALIDATION_FAILED/PRECONDITION_MALFORMED; 401:SESSION_INVALID; 403:SELF_MANAGEMENT_FORBIDDEN; 404:ACCOUNT_NOT_ACCESSIBLE; 409:IDEMPOTENCY_CONFLICT/LAST_ADMIN_PROTECTED; 412:PRECONDITION_STALE; 428:PRECONDITION_REQUIRED |
| disableIdentityAccount | POST /admin/users/{accountId}/disable | LifecycleRequest → 200 Account | Idempotency-Key, If-Match | 400:VALIDATION_FAILED/PRECONDITION_MALFORMED; 401:SESSION_INVALID; 403:SELF_MANAGEMENT_FORBIDDEN; 404:ACCOUNT_NOT_ACCESSIBLE; 409:IDEMPOTENCY_CONFLICT/LAST_ADMIN_PROTECTED/INVALID_ACCOUNT_STATE; 412:PRECONDITION_STALE; 428:PRECONDITION_REQUIRED |
| reactivateIdentityAccount | POST /admin/users/{accountId}/reactivate | LifecycleRequest → 200 Account | Idempotency-Key, If-Match | 400:VALIDATION_FAILED/PRECONDITION_MALFORMED; 401:SESSION_INVALID; 404:ACCOUNT_NOT_ACCESSIBLE; 409:IDEMPOTENCY_CONFLICT/INVALID_ACCOUNT_STATE; 412:PRECONDITION_STALE; 428:PRECONDITION_REQUIRED |
| resendIdentityInvitation | POST /admin/users/{accountId}/invitation | LifecycleRequest → 202 Accepted | Idempotency-Key, If-Match | 400:VALIDATION_FAILED/PRECONDITION_MALFORMED; 401:SESSION_INVALID; 404:ACCOUNT_NOT_ACCESSIBLE; 409:IDEMPOTENCY_CONFLICT/INVALID_ACCOUNT_STATE; 412:PRECONDITION_STALE; 428:PRECONDITION_REQUIRED; 429:RATE_LIMITED |
| listIdentityRoles | GET /admin/roles | none → 200 RoleCatalog | none | 401:SESSION_INVALID; 403:AUTHORITY_REQUIRED |
| listIdentityScopes | GET /admin/scopes | none → 200 ScopeCatalog | none | 401:SESSION_INVALID; 403:AUTHORITY_REQUIRED |

After login/refresh/logout rebinding, the browser requests GET /auth/csrf to obtain the challenge for the current cookie/family before its next auth POST. GET retains valid existing state or issues bound replacement; it never rotates refresh, extends idle or authenticates an invalid family.

## Fixtures and verification
[fixtures.json](fixtures.json) contains synthetic request/success/header examples for every operation, policy boundary cases and explicitly DECLARATIVE_NOT_RUNTIME_EXECUTED behavioral expectations. Validator checks error examples against each actual operation/status/schema, required header examples and missing-header negatives; all schema fixtures reject unexpected properties/secret-shaped output.
[validate.mjs](validate.mjs) is process-free: fixed local reads and pinned existing Ajv8.20.0/formats3.0.1; no network, subprocess, output file, credentials or dependency installation. Independent operation/policy/DTO constants live in validator source, not inferred from tested document. Mutation tests check exact rejection categories; a nonpolicy description positive control must still pass.
Run at repository root:
~~~powershell
$env:NODE_DISABLE_COMPILE_CACHE='1'
Remove-Item Env:NODE_OPTIONS -ErrorAction SilentlyContinue
node contracts/identity-access/validate.mjs
~~~
Environment changes affect only that shell process. Do not copy developer secrets into examples.
Strict lint uses existing Redocly2.64.0 recommended-strict, inherited info-license exception only. No new rule suppression. The rebuilt document still requires one authorized compatible Linux invocation with exit0 and zero errors/warnings/ignored; no Windows retry of historical libuv failure. [Current evidence](../../docs/implementation/t-002-02-identity-contract-evidence.md) distinguishes results.
No fixture proves JWT cryptography, CSRF HTTP rejection, revocation/authority, DB transaction, concurrency, delivery timing or rate limiting. Those remain future Java/PostgreSQL/API tests after contract acceptance.

## Review/handoff
T-002-01 DONE for reaffirmed design directions, not runtime. T-002-02 stays REVIEW_PENDING if strict lint or focused review incomplete. Exact username/DTO/status/role vocabulary and command retention are candidate details; review before T-002-03 migration. Mobile transport, production delivery, bootstrap deployment and secret-operation configuration remain separate affected-scope prerequisites.
[Supplemental trace](../../docs/quality/feature-002-recovery-traceability.md) preserves the protected main Foundation traceability file. No canonical REQ/AC, Demo contract, SDK, Java/migration, manifest or CI changes are made.

## Registered system Realm clarification — 2026-10-22

Owner REALM-P1/P2/P3 approval establishes IDENTITY.scopeId as the UUID of an
authoritative registered Identity Realm. This changes no OpenAPI JSON field,
UUID syntax, endpoint/status, cookie/session or O1–O7 behavior. Earlier arbitrary
example UUIDs are synthetic, not automatically registered/global scopes.

[ADR-0010](../../docs/architecture/decisions/ADR-0010-identity-system-realm.md)
records the exact bounded single-SYSTEM-Realm delegation: current admitted
Identity Management actor, ACTIVE registered Realm, different existing target
with nonempty authoritative current grant context, full current/requested exact
IDENTITY_MANAGEMENT pair coverage in that same Realm, then existing Domain
self/last-admin/version guards. Ordinary grant possession, resource existence or
FK alone is insufficient. All external/mixed roles/scopes remain denied.

V4 registration begins INACTIVE; owner-only controlled bootstrap creates a new
pending initial administrator and requires separately completed accepted
verified-email invitation activation before enabling the Realm. No automatic
credentials/provisioning bypass, HTTP bootstrap or complete Feature002 acceptance.
[Procedure](../../docs/runbooks/identity-realm-bootstrap.md).
Current-source SQL/provider/runtime and HTTP security evidence are still pending;
synthetic fixtures and earlier CI are not those proofs.
