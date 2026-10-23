# T-002-05 Invitation activation application checkpoint ? 2026-10-23

Status: **T_002_05_INVITATION_ACTIVATION_CI_VERIFIED**.
Current CI acceptance is recorded in the final section below; the original
implementation and failed-CI checkpoints are retained as historical evidence.
Initial implementation authorization:
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


## Activation CI checkpoint ? 2026-10-23

**T_002_05_ACTIVATION_CI_FAILED** under
`OWNER_ACTIVATION_CI_CHECKPOINT: CONDITIONAL_GO`.
Published/tested source: `888da7e9d8004cbb887157b5d9e4a87d0c4b48db`, parent
`3c1fd042d388cbde9a3af228a60c55ff8a6ce276`, exactly18 approved files in one commit,
one normal feature push. Branch `feature/002-identity-activation`; remote SHA
verified equal. No PR/merge/deploy/source repair or manual rerun.
This supersedes the initial no-publication/runtime-pending statements ONLY for
actual results below; earlier evidence remains historical.

[Backend verification run39916095981 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39916095981):
workflow383888734, `.github/workflows/backend-verification.yml`, native
workflow_dispatch, completed FAILURE. No appropriate push run existed; dispatch
was issued exactly once. All three actual job checkout consoles identify the
exact source SHA888da7e above, without a synthetic PR merge revision.

| Job | Actual runtime/report evidence |
| --- | --- |
| [Architecture gate](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39916095981/job/120289903833) |SUCCESS;23 PASS/0 FAIL/0 ERROR/0 skips; exact required inventory including negative fixtures |
| [Backend default / H2](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39916095981/job/120289903760) |SUCCESS;347 discovered,330 PASS/0 FAIL/0 ERROR/17 exact expected skips; all12 Activation unit methods PASS |
| [Backend PostgreSQL18](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39916095981/job/120289903533) |FAILURE before clean verify: `FAIL: Maven Wrapper bootstrap; private diagnostics retained`. No PostgreSQL report/artifact;20 Activation PG methods and85 retained PG methods NOT EXECUTED/UNVERIFIED, not failed test cases or approved skips |

Java/main/test compilation is actually proven by successful clean Maven verify
in H2/Architecture; H2 also executes Domain48, Provisioning-input12, authority9,
Realm-policy8 and crypto10 regressions. PostgreSQL transaction/concurrency,
Realm/Grant/Outbox integration, Testcontainers runtime/privacy/owned cleanup
are still UNVERIFIED on this source. The PG step fails at `mvnw --version`, before
`run-backend.sh` reaches clean verify/report generation or Testcontainers suite
execution. Missing summary/tests.xml upload is consequential, not the root gate.
Underlying bootstrap/download reason cannot be determined from the sanitized
message; no network/SQL/source cause is invented and no raw Maven diagnostics
were retrieved or persisted. The disposable prepare-postgres cleanup step itself
completed SUCCESS; this does not prove the unexecuted Testcontainers audit.

Each job's fail-closed Python report-policy step PASS,113 controls, including
strict new12+20 identities and unmodified skip/lifecycle policy. H2/Architecture
temporary DB credential log-audit messages PASS. Sanitized summary/XML exact
identity/status counts and allowlisted fields PASS, with no secret-bearing
failure messages, properties or raw logs included. PG Testcontainers credential
privacy/cleanup audit did not execute and is NOT claimed PASS.

Only two sanitized artifact ZIPs exist; both are authenticated to run39916095981,
source888da7e, attempt1; downloaded with GitHub CLI, CRC/file allowlist checked:

| Artifact | ID | GitHub digest = actual ZIP SHA-256 |
| --- | --- | --- |
| backend-architecture-1 |12484557968 |d85eb228bd4c6d0d990ee83005ee776b7ae6cfc81a88662aaaf886a3ad6538bd |
| backend-h2-1 |12484059526 |b3028386b05c555893e4af27084bffe5a6564e04e6162ba82b28dfcdf8c6f1ed |

Private ignored checkpoint `.demo/activation-ci-publication/` retains artifact
metadata, verified sanitized ZIPs/XML/summaries, checkout/guard projections and
publication/dispatch evidence. No raw credential-bearing Maven diagnostics,
GitHub token or signed artifact URL was written there. Public job consoles were
processed only in memory into fixed status/count/SHA projections.

### Source-integrity verification and remaining coverage

V3 `app/backend/src/main/resources/db/migration/V3__production_identity.sql`:
canonical LF SHA-256 c870bc7fa2225e7497534caf717ce6ad8d0f51c8fffbca272f8c5bdaf84985fb;
Windows CRLF checkout c900c0a4b26d1f1839b1c5cd33ab2c40aa06942db02e2f76338270b65632db01.
V4 `app/backend/src/main/resources/db/migration/V4__identity_system_realm.sql`:
canonical LF c2254b9fc8f292e171f4fde49cda15c38c63c940b1c74a2ccae780ec94643062;
Windows CRLF f169c55d60dbdce41d5cbc5a8b9e2826736563f7f98143453d6d95f399d3e1e3.
Both canonical blobs match accepted pins; normalized bytes prove checkout-only
line endings. V1-V5 Git blob OIDs are unchanged by publication; no SQL, expected
hash, test skip or source-integrity assertion was changed. The Windows source
check is still9/10 with2 failing subcontrols, never relabeled PASS.

Existing workflow runs scripts/ci report-policy discovery and run-backend.sh;
it does NOT invoke `app/backend/test_identity_migration.py` or
`app/backend/test_identity_invitation_outbox.py`. Thus separate migration/outbox
Python source controls are NOT EXECUTED on GitHub and cannot be claimed CI PASS.
Local15/15 migration controls and canonical blob verification are separate
prepublication evidence, not a substitute for requested Linux source-control
execution. No workflow was changed to hide or silently reinterpret that gap.

### Current state and handoff

T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED and all application/HTTP/delivery/bootstrap
limitations above remain. Not ready for final green-CI PR review. Next priority
is owner-authorized bounded attribution of the PostgreSQL Maven Wrapper bootstrap
failure; no retry/repair/extra dispatch is performed automatically. Source-control
CI coverage also remains an explicit acceptance gap before a VERIFIED verdict.

This section and supplemental trace are later LOCAL documentation edits, NOT
part of source commit888da7e tested by the run. They need a separate docs-only
closure publication after review; no amend or second commit was made. Six
original Foundation changes remain untouched; no business-role readiness is
inferred. Future Merchant/Business Admin/Shipper work still requires the owner
review checkpoint before the first eligible canonical business implementation.


## Activation CI verified closure - 2026-10-23

**T_002_05_INVITATION_ACTIVATION_CI_VERIFIED**, under
`OWNER_ACTIVATION_CI_EVIDENCE_CLOSURE: CONDITIONAL_GO`.
This supersedes earlier runtime-pending/failed-CI statements ONLY for the bounded
Activation source tested here. The failed run and original implementation checks
above remain historical; neither their results nor migration pins are rewritten.

### Provenance and actual runtime results

[Backend verification run 39920826371, attempt 1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39920826371)
completed SUCCESS, event `workflow_dispatch`, branch
`feature/002-identity-activation`, workflow `backend-verification.yml` /383888734.
Run metadata, artifact provenance and retained sanitized checkout projections
identify the same full tested SHA in all three jobs:
`081c14cc059b61ddfabc2b1f4d1dbf7d682a70f7`.
Its parent is Activation implementation commit
`888da7e9d8004cbb887157b5d9e4a87d0c4b48db`; the later source commit changes only
bounded bootstrap diagnostics in `scripts/ci/run-backend.sh`.

| Job | Job ID | Discovered | PASS / executed | FAIL | ERROR | Exact expected skips |
| --- | --- | --- | --- | --- | --- | --- |
| Architecture gate |120305361105 |23 |23 |0 |0 |0 |
| Backend default / H2 |120305360869 |347 |330 |0 |0 |17 |
| Backend PostgreSQL 18 |120305361309 |490 |485 |0 |0 |5 |

Architecture includes11 production/scope rules and12 negative-fixture/control
cases. H2 skips only FoundationDependencyTests5, PostgresFoundationTests8 and
PostgresHealthSafetyTests4. PostgreSQL skips only FoundationDependencyTests5:
Redis/RabbitMQ real foundation tests were not executed by this workflow.
Every job passed113 Python report-policy controls, the strict finite method/
status/skip inventory guard and temporary database credential log audit.
PostgreSQL also passed generated Testcontainers credential privacy and owned
resource cleanup audits; the separate disposable CI PostgreSQL cleanup passed.
No missing test, unexpected skip, raw failure body or relaxed assertion is used
to obtain these results.

Clean Maven verify actually compiled main/test Java. Both H2 and PostgreSQL ran
all12 `IdentityInvitationActivationTests` methods. PostgreSQL ran all20 new
`activation*` cases and all85 retained methods in `IdentityGrantApplicationTests`
(105/105 PASS), including actual PostgreSQL transaction/rollback/concurrency.
Retained regressions include Domain48, provisioning-input12, authority9,
Realm-policy8, crypto10, IdentityMigration25, PostgreSQL foundation8, JDBC safety4,
PostgresTestcontainers7 and runtime-policy6. Groups overlap; do not add these
counts to the job totals as separate tests.

Exact12 unit and20 PostgreSQL identities/statuses are in the sanitized JUnit
XML, cross-checked against the strict source inventory and the retained private
verified-test-evidence manifest. Coverage includes proof identity/purpose/time,
password bounds/BCrypt, consumed/revoked proofs, current Realm/grant admission,
retained-key rotation, exact/conflicting receipt replay, mandatory-write rollback,
optimistic versions, two-key/same-key concurrency, secret redaction, old-session
revocation and obsolete outbox payload clearing. No actual email delivery,
production bootstrap, HTTP admission or new session issuance is demonstrated.

### Sanitized artifact integrity and privacy

All three artifacts belong to run39920826371/source081c14c/attempt1. Fresh GitHub
metadata was compared with retained ZIP bytes during closure: SHA-256 matches,
CRC and exact two-entry allowlist (`summary.json`, `tests.xml`) PASS. Sanitized
XML and summaries agree on class/method/status/counts; no raw Maven diagnostics,
properties, system output, credentials or signed download URLs are published.

| Artifact | GitHub artifact ID | Verified ZIP SHA-256 = GitHub digest |
| --- | --- | --- |
| backend-architecture-1 |12486993872 |aa02b9c3cfeef44afd0fcd2b119284f0e5e4a72d4f9cc9edb8d907d96ec7bb4a |
| backend-h2-1 |12487207225 |6026cd7467703539161ed8a17f857ecf8ea4067efc38a31d4c2db503077dc7b5 |
| backend-postgres-1 |12486813751 |8bd0e43863440d8f563d6835e5cb937c5ada77e34622fd95ba36319dffdd1b59 |

Private sanitized evidence remains in ignored
`.demo/activation-bootstrap-diagnostic/`; this documentation publishes only
provenance, fixed status/counts and artifact digests. No raw diagnostics were read
or republished for this closure.

### Historical Maven incident and explicit source-suite gaps

Run39916095981 attempt1 failed in the PostgreSQL job at
`bash ./mvnw --version`, before Maven `clean verify` and PostgreSQL tests.
Missing report artifacts were a consequence. The later diagnostic-only patch
preserves the wrapper exit code, bounds private diagnostics, emits only allowlisted
categories/metadata, defaults to UNKNOWN for insufficient/ambiguous evidence and
passed21 synthetic/mock controls. Those controls are not Java runtime results.
In successful run39920826371 bootstrap exit code was0 in all jobs; the original
failure did not reproduce. **Original root cause remains OPEN / NOT REPRODUCED**;
no permanent repair, network attribution or SQL defect is claimed.

The workflow does NOT invoke `app/backend/test_identity_migration.py` or
`app/backend/test_identity_invitation_outbox.py`; neither Python source suite has
CI execution evidence from this run. Actual25 Java migration tests are separate
runtime evidence, not substitutes for those Python suites. Local migration15/15
and Outbox9/10 (two V3/V4 raw-byte hash subfailures) retain their historical
results. Windows CRLF versus canonical LF finding above remains OPEN; accepted
pins were not changed. V1-V5 Git blob OIDs and SHA-256 match the recorded immutable
identities during this closure. No SQL or expected hash was edited.

### SDD boundary and documentation publication

Only this bounded Activation CI checkpoint is VERIFIED. T-002-05 remains
**PARTIAL/unchecked**; Feature002 and AC-002-01..05 remain **NOT ACCEPTED**;
`GRANT_MUTATION_FUNCTIONALITY_LIMITED` remains. Feature001/D1-08/Day1 stay PARTIAL.
Pending: session use cases, recovery, resend, lifecycle/query, remaining authority/
security integration. HTTP adapter/proof resolution/secure request mapping remain
later canonical T-002-06 work; provider/delivery and scheduled maintenance remain
T-002-08 after its dependencies. This is not deployment readiness.

The closure publication contains exactly this evidence document and
[the supplemental traceability](../quality/feature-002-recovery-traceability.md#activation-ci-verified-trace---2026-10-23).
It is one later docs-only commit: run39920826371 tests source081c14c above, NOT
the documentation commit that records it. No manual dispatch/rerun/PR/merge is
part of closure. Original repository index stays empty and all six Foundation
fingerprints remain unchanged; no protected main traceability edit is included.
The existing owner-review boundary for future business modules remains in force;
this checkpoint does not authorize Merchant/Business Admin/Shipper implementation.
