# T-002-05 Invitation activation application checkpoint ? 2026-10-23

Status: **IMPLEMENTED_VERIFICATION_PENDING** under
`OWNER_T_002_05_INVITATION_ACTIVATION: GO`.
T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED.
No new REQ/AC, SQL, accepted wire contract or Domain policy is introduced.

## Git baseline and ownership

PR22 is actually merged at `3c1fd042d388cbde9a3af228a60c55ff8a6ce276`.
Fetch and merge-ancestor check PASS. The isolated worktree branch
`feature/002-identity-activation` starts at that exact origin/develop SHA.
The original Application worktree remains at3d9281e with an empty index and
exactly its six Foundation changes, all prior SHA-256 fingerprints unchanged.
No dirty Foundation file was copied into the new worktree. No commit/push/PR.

This is canonical T-002-05 Application: backend command, inward ports, use case,
JDBC transaction and explicit crypto/password composition. HTTP admission,
Origin/CSRF, routing, throttling and response mapping remain T-002-06. Sender,
provider and scheduled maintenance remain T-002-08 after its canonical dependency.

## Flow and security boundaries

`IdentityInvitationCommands` is an internal backend caller contract, not HTTP.
Its proof ID is an untrusted locator, never sufficient authority; the token must
match the exact stored proof/account/purpose/timestamps/retained verifier key.
The accepted HTTP body still contains only token/password: no proofId wire field,
URL format, locator resolver or new transport envelope has been defined here.
The HTTP adapter must resolve the proof context safely before wiring this command;
that mapping remains an explicit T-002-06 integration prerequisite, not a working
end-to-end endpoint or an all-proof scan implemented by this slice.

1. Reuse Domain new-password validation:15 code points minimum,72 UTF-8 bytes
   maximum, lone-surrogate rejection, no trim/normalization/truncation.
2. Enter existing READ_COMMITTED/15s transaction; same Identity administration
   advisory lock and5s lock/10s statement budgets; row-lock proof and exact account.
   Sample the clock after locking; time is canonical PostgreSQL microseconds.
3. Verify HMAC with retained proof key ID. No legacy unkeyed fallback, outbox
   decryption, live credentials, environment/file reads, Demo principal or network.
   Reject wrong purpose, revoked/expired/future proofs and unknown keys. Expiry is
   exclusive at24h. Consumed-proof identity authentication exists ONLY to admit an
   exact authorized retained receipt replay; it never re-consumes a proof.
4. Recheck exact current management grant, ACTIVE Realm and issued destination/
   Realm metadata. An outbox row or SENT state is not proof of email delivery.
   The inactive system-Realm bootstrap flow is deliberately not bypassed: this
   composition neither activates that Realm nor invokes owner-only functions.
5. PROOF-bound receipt lookup follows authentication/current eligibility. Compare
   purpose-separated HMAC-SHA-256 command fingerprints with constant-time equality.
   Fingerprint binds proof/account/purpose/times/key ID, fixed-length token and
   exact password bytes; no raw password/token or unkeyed password digest is stored.
   Use the proof's retained key across rotation. A valid exact-key replay is a
   no-op only while the proof is unexpired, receipt retained and current ACTIVE
   account/version agrees. Different payload409; consumed proof with another key
   denies; no session, audit, outbox or BCrypt effect repeats.
6. Reuse `IdentityAccount.activate`; BCrypt12 only after validation/admission.
   Optimistic account version/authVersion predicates and compare-and-consume
   proof update must each affect exactly one row. Persist ACTIVE/verified email/
   verifier/activation timestamp and increment both versions. Revoke prior sessions
   and refresh tokens, clear only the matching still-live outbox payload/lease,
   append fixed audit and204 receipt with at least24h retention in the SAME tx.
   Any mandatory failure rolls back all writes. No grants, Realm, new accounts,
   sessions or credential response are created. Existing audits/receipts are never
   deleted or overwritten; a SENT row retains its own terminal delivery outcome.
7. Command owns a defensive token buffer and is AutoCloseable; caller must close.
   Application clones are wiped on success/failure; password UTF-8 fingerprint
   bytes are wiped. Java immutable password Strings cannot be promised securely
   erased from memory. Secret-bearing objects/errors are redacted; no logging added.

## Authored tests versus executed checks

12 unit identities in `IdentityInvitationActivationTests` and20 PostgreSQL
`activation*` identities in existing `IdentityGrantApplicationTests` authored.
The existing85 PG method bodies are unchanged and retained; suite now105 methods.
The three-suite Testcontainers lifecycle, cleanup/security helpers, pins, execution
selection, required job names and expected skips remain unchanged.

Tests cover success/BCrypt verification/version/authVersion/audit/receipt, Domain
password boundaries, wrong token/locator/account/purpose, exact expiry/future,
revoked/consumed proof, disabled pending/Realm/grant eligibility, exact replay and
conflict, five mandatory-write rollbacks, optimistic update rejection, two-key
and same-key concurrent calls, old-key rotation/missing key, redaction/session
revocation and outbox payload cleanup. Synthetic outbox token extraction in the
PG fixture is NOT actual email delivery; bootstrap fixture helpers are test-only.

| Command/check | Actual result |
| --- | --- |
| `python -B -m unittest discover -s scripts/ci -p test_verify_reports.py` |113/113 PASS on corrected inventory; includes3 new controls and220 missing/renamed/skipped/failure/error method mutation subcases |
| Initial report-policy run |112 PASS/1 FAIL: retained33-grant count did not yet subtract20 activation methods; corrected arithmetic, preserving all old identities |
| `python -B app/backend/test_identity_migration.py` |15/15 PASS |
| `python -B app/backend/test_identity_invitation_outbox.py` |9/10 test methods PASS; one method FAIL with2 V3/V4 raw-byte hash subfailures on Windows CRLF checkout |
| V1-V5 Git content/immutability check |PASS: no migration diff; each checkout equals committed bytes after LF/CRLF normalization; no SQL was edited |
| V3/V4 hash attribution |Committed LF blobs match pinned c870bc7f/c2254b9f; untouched CRLF checkout bytes are c900c0a4/f169c55d. V1/V2 pins instead describe their Windows checkout bytes. Existing guard mixes representations; no expected hash/assertion was changed or bypassed |
| `git diff --check` |PASS after retaining unchanged policy bytes and using LF for added/replaced policy lines; no Git settings changed |
| Existing85 PG test-body equality |PASS, text-identical; no old assertion/test removed |
| Basic brace/static dependency checks |PASS; NOT Java compilation or ArchUnit execution |
| Java compilation, BCrypt/JCE unit execution, PostgreSQL locking/rollback/races, actual report guard and actual runtime privacy/cleanup |UNVERIFIED: no approved JVM/PostgreSQL runtime invoked |

Report fixture XML is synthetic policy evidence only, never Java test results.
Expected FUTURE inventory: Architecture23/0skip; H2330 PASS/17skip;
PostgreSQL485 PASS/5skip, including32 new tests. These numbers are not observations.
Existing CI runs39896561084/39900467243 do not verify this uncommitted source.
No new runner, dependency, dispatch, Docker resource or mail sandbox was created.
The Windows raw-byte guard limitation must be carried into review/CI handoff;
it is not evidence of changed/applied SQL or a new database regression.

## Exact changed-file manifest

All paths are relative to the isolated worktree; no staged files. SHA-256 packet
is retained privately in ignored `.demo/activation-implementation-checkpoint/`.

- app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityInvitationCommands.java
- app/backend/src/main/java/com/elogistic/backend/identity/api/IdentityCommandFailure.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/application/AcceptIdentityInvitation.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/IdentityInvitationStore.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/application/port/InvitationCredentials.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/JdbcIdentityInvitationStore.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProtectedInvitationCredentials.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/ProductionIdentityActivationComposition.java
- app/backend/src/main/java/com/elogistic/backend/identity/internal/infrastructure/InvitationProtection.java
- app/backend/src/test/java/com/elogistic/backend/identity/internal/application/IdentityInvitationActivationTests.java
- app/backend/src/test/java/com/elogistic/backend/identity/internal/infrastructure/IdentityGrantApplicationTests.java
- scripts/ci/verify-reports.py
- scripts/ci/test_verify_reports.py
- docs/implementation/t-002-05-invitation-activation-evidence.md
- docs/implementation/t-002-05-identity-application-evidence.md
- docs/quality/feature-002-recovery-traceability.md
- specs/002-identity-access/design.md
- specs/002-identity-access/tasks.md

## Remaining dependency graph and review boundary

T-002-05 is still PARTIAL: this source needs current-revision runtime verification;
login/logout/refresh/current-session reads, recovery request/confirmation,
resend, disable/reactivate/account query/list and remaining authoritative grants/
owner integrations are not made complete by this slice.
`GRANT_MUTATION_FUNCTIONALITY_LIMITED` remains. Inactive bootstrap invitation
issuance/completion is not enabled by the ordinary ACTIVE-Realm-only composition.

Business-feature implementation is NOT ready now. Dependency order is001 ->002
->003 merchant/address, then the later business slices. T-003-01 review and
T-003-02 accepted contract precede T-003-03 migration (first canonical production
persistence implementation) and T-003-04/05 Domain/Application. None is authorized
by Identity provisioning. When those prerequisites are actually ready, emit
`BUSINESS_FEATURE_IMPLEMENTATION_REVIEW_REQUIRED` and obtain owner approval of
exact task IDs, roles/permissions, workflows, API/DB/frontend scope, reusable Demo
parts, decisions and tests BEFORE business implementation. No Business Admin,
Merchant or Shipper functionality was started here.
