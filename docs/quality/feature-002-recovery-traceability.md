# Feature 002 recovery — supplemental traceability

Checkpoint2026-10-21; owner baseline recovery approved2026-10-20.
This supplements, does not modify, [protected main traceability](traceability.md).
[Fresh evidence](../implementation/t-002-02-identity-contract-evidence.md);
[ADR-0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md);
[canonical spec](../../specs/002-identity-access/spec.md);
[design](../../specs/002-identity-access/design.md);
[tasks](../../specs/002-identity-access/tasks.md).
No new REQ/AC identifiers or amended original acceptance wording.

| Source → requirement | Decision/design → task | Contract/fixture/test IDs | Fresh scope/status | Mandatory runtime evidence still missing |
|---|---|---|---|---|
| FR-A01/A02/AD01 → REQ-002-01 | DEC-04/O5/O6, ADR-0009 → T-002-01/02 | Account/Grant/ProvisionRequest, listIdentityAccounts/provisionIdentityAccount/getIdentityAccount/replaceIdentityGrants; fixture.lifecycle-shape.*, document-mutant.target-any-match/remove-union-authority | Decision record accepted; schema/header/status/mutation checks PASS | Multi-role authorization and whole-target resource-provider enforcement |
| FR-A01/A02/AD01 → REQ-002-02 | DEC-03/O2/O5 → T-002-02 | getIdentityMe/CSRF security; contract.inventory.security.headers.status.policy.refs; document-mutant.missing-security/cookie-or-not-and/client-only-csrf | Wire/policy checks PASS; csrf.binding declaration only | JWT signature/session admission, CSRF rejection, object ownership/denial |
| FR-A01/A02/AD01 → REQ-002-03 | DEC-03/O4 → T-002-02 | LoginPassword/NewPassword/JwtClaims policy; credential.policy.*, credential.schema-byte-gap.explicit; negative.jwt.missing.* | Test-only schema/codepoint/byte/required-claim checks PASS | Java BCrypt preflight/hash checking, brute-force distribution, safe mobile storage, logs/keys |
| FR-A01/A02/AD01 → REQ-002-04 | DEC-04/O5/O6 → T-002-02 | replace/disable/reactivate; audit.transaction/owner.authority/last-admin.concurrent/self.* declarations; policy mutants | Contract policy deletion controls PASS, no runtime grant/audit proof | PostgreSQL atomic grants/audit/receipt/revocation, concurrent last-admin invariant, Finance segregation |
| FR-A01/A02/AD01 → REQ-002-05 | DEC-03/O3/O7 → T-002-02 | refreshIdentity/logoutIdentity/confirmIdentityRecovery; refresh.concurrent/lost-response, session.revoked, idle.polling, reset.sessions, version.replay/stale declarations | Headers/error bindings PASS, declared effects NOT executed | Real rotation/replay/family invalidation, deadline accounting, concurrency/lost-response recovery |
| NFR-S01/S02/S04 → REQ-002-01..05 | security/ownership/ADR-0009 → T-002-02 | error.* / negative.error-binding.* / negative.response-secret.*; selected-output disclosure checks | Contract examples and bounded publication checks only | Full application security/privacy and production configuration acceptance |

500 PASS includes compilation, fixture and control assertions, NOT500 executed
business scenarios.36 document mutations are included in that count; do not add
them again.21 behavioral declarations are included coverage/label checks only.
All operation success/header fixtures and every specified error example are
synthetic; none is a captured API/database response.

T-002-01 DONE for reaffirmed browser design decisions; T-002-02
CONTRACT_REVIEW_PENDING. Strict Redocly UNVERIFIED; reconstructed candidate
focused review remains pending. T-002-03..10 NOT STARTED.
AC-002-01..05 remain NOT ACCEPTED / runtime UNVERIFIED. Full feature DoD includes
CI and integration into main traceability at a separate authorized checkpoint.
Feature001, D1-08 and Day1 remain PARTIAL.

## Final task acceptance — 2026-10-21

T-002-02 DONE under OWNER_T_002_02_FINAL_VERIFICATION_APPROVAL, superseding the historical reconstruction REVIEW_PENDING/500-check checkpoint above. Fresh512 checks PASS/0 FAIL/0 skip, including43 document mutations and1 nonpolicy positive control; one strict Linux Redocly2.64.0 attempt PASS, exit0/zero errors/warnings/ignored. Exact OpenAPI SHA-256 acf92380c325698a1370a2ade6accff82bb338c4524f6328a9613d3e4b7b6128.

| Reviewed correction | Requirement/task trace | Fresh contract controls | Runtime classification |
|---|---|---|---|
| FINAL-01: repeat logout with missing refresh cookie still requires valid server-bound preauth CSRF and exact Origin | REQ-002-02/03/05, T-002-02, O2/O3 | document-mutant.logout-refresh-required/logout-optional-param-deleted/logout-csrf-disabled; logout.case-declarations-covered;4 logout.declaration-only.* checks |4 cases are DECLARATIVE_NOT_RUNTIME_EXECUTED; actual HTTP204/403 and session effects still UNVERIFIED |
| FINAL-02: recovery receipt scope independent of existence and delivery only to stored verified email | REQ-002-02/03/05, T-002-02, DEC-04/O5 | document-mutant.recovery-existence-scoped/recovery-arbitrary-email/recovery-receipt-only-existing/recovery-plaintext-receipt; declaration-only.recovery.enumeration | No real email, database receipt, timing or privacy enforcement test |

The21 original expectations remain future acceptance requirements: csrf.binding → HTTP/state binding; refresh.concurrent/lost-response → concurrency/fault; session.revoked/idle.polling → admission/deadline; grant.scope-union/account.target-wide/owner.authority → object authorization; last-admin.concurrent/self.disable/self.grants/reactivate.old-session/reactivate.pending → transaction/lifecycle; invitation.consumption/invitation.resend/recovery.enumeration/reset.sessions → proof/mail/recovery; version.replay/version.stale → idempotency/preconditions; audit.transaction/delivery.boundary → transaction/outbox. None is relabeled as executed runtime.
This authorized supplement satisfies current task-level traceability without writing main traceability.md. Feature-level CI/full integration and main-document reconciliation remain open. T-002-03..10 NOT STARTED; AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 remain PARTIAL.

## T-002-03 persistence checkpoint — 2026-10-21

Explicit OWNER_T_002_03_IMPLEMENTATION_APPROVAL: GO authorizes migration/schema
and verification sources; exact-revision runtime authorization remains pending.
T-002-03 is **IMPLEMENTED_VERIFICATION_PENDING**, superseding its NOT STARTED
state above; canonical checkbox stays open. [Current evidence, schema decisions,
test inventory and execution packet](../implementation/t-002-03-identity-migration-evidence.md).

| REQ / design / task | Migration support | PostgreSQL test method (IdentityMigrationTests) | Current evidence / AC limit |
|---|---|---|---|
| REQ-002-01, DEC-04/O4/O6, T-002-03 | user_account lifecycle/version/exact username; account_grant multi-role/public scope IDs | usernamesRemainExactCaseUnicodeAndRejectDuplicatesBlankAndNull; accountLifecycleRequiresVerifiedActivatedCredentialAndPositiveVersions; grantsAllowMultipleScopesButRejectDuplicatesInvalidScopesAndMissingAccounts | Source/static checks PASS; PostgreSQL UNVERIFIED. No last-admin, transition-authority or resource assignment acceptance |
| REQ-002-02/05, DEC-03/O2/O3, T-002-03 | auth_session family/revocation/deadlines; csrf_binding | sessionsRejectInvalidDeadlinesMissingAccountsAndNullExpiry; csrfSupportsPreauthAndSessionBindingButRejectsBadHashesAndReferences | Source only; actual admission/CSRF/idle semantics remain application/security obligations |
| REQ-002-03/05, DEC-03/04/O4, T-002-03 | BCrypt hash shape, refresh_token digest/lineage, action_proof purpose/expiry/consumption | refreshHashesMustBeUniqueCorrectSizeAndHaveExistingSession; refreshLineageCannotCrossFamiliesForkOrHaveTwoLiveMembers; proofPurposeLifetimeHashAndSingleOutstandingProofAreEnforced | PostgreSQL UNVERIFIED. No BCrypt execution, token randomness, replay/concurrent compare-and-consume or delivery proof |
| REQ-002-04/05, O5/O7, T-002-03 | typed command_receipt binding; runtime append-only audit/receipts | receiptsBindActorProofOrPreauthWithoutExistenceDisclosureOrDuplicateKey; runtimeCanAppendAuditButCannotDdlDeleteAccountsOrMutateAuditAndReceipts | PostgreSQL UNVERIFIED; no transaction/authorized replay/last-admin/concurrent revocation proof |
| REQ-002-01..05, ownership/Flyway rules, T-002-03 | V3 append-only, eight Identity tables, unchanged Demo tables/global V1/V2 and H2 migration | coldStartValidateAndRerunPreserveHistoryAndIdentityData; upgradeV2PreservesAllDemoTablesWithoutImportingAccounts; ownerOwnsEightTablesIndexesAndOnlyIdentityForeignKeys; preexistingSchemaDriftFailsWithoutBaselineOrRepair | Immutable-source checks PASS; actual cold-start, upgrade, permissions and compatibility UNVERIFIED |

15 Python tests PASS/0 failures;15 mutation subcontrols and1 positive comment
control are included, not extra executed PostgreSQL tests.14 Java test methods
are written, NOT COMPILED/NOT RUN. New class is excluded by the existing task-c tag
without explicit profile. Existing strict PostgreSQL CI report policy does not
yet admit this class; no workflow/report-policy change or GitHub PASS is claimed.
Main traceability and11 protected Foundation working files remain byte-identical.
T-002-04..10 remain NOT STARTED; production ACs remain open; Feature001/D1-08/Day1
remain PARTIAL. No original REQ/AC or accepted contract/decision is amended.

## T-002-03-PG conditional execution assessment — 2026-10-21

OWNER_T_002_03_ISOLATED_POSTGRES_VERIFICATION: CONDITIONAL_GO does not waive
mandatory runtime gates. [Fresh preflight/evidence](../implementation/t-002-03-identity-migration-evidence.md#t-002-03-pg-conditional-verification-preflight--2026-10-21)
records5 approved source hashes +11 protected Foundation hashes matched and
15 fresh static controls PASS (included15 mutation subcontrols/1 positive control).
REQ-002-01..05 schema/test mappings above remain source-level support only.

**T_002_03_ENVIRONMENT_BLOCKED**: no concrete Linux Java21/Maven3.10.0/process
containment path established; Windows-only H2/Wrapper harness does not authorize
PG. Existing strict CI collector also lacks the new class/method inventory.
Source discovery under the Testcontainers profile is not execution evidence.
No new security incident, SQL defect or compile failure is claimed.

PG-1..5 remain UNVERIFIED; runtime batches0, JVM/Maven launches0, containers/DBs0,
migrations0 and actual SQL assertions0. Runtime cleanup is NOT_APPLICABLE, not
PASS. No actual report/privacy guard or GitHub CI run occurred. Implementation
stays IMPLEMENTED_VERIFICATION_PENDING; T-002-04 is not started; production ACs
remain open. Protected main traceability/report policies and existing working
source remain unchanged. Feature001/D1-08/Day1 remain PARTIAL.

## T-002-03-RI strict inventory checkpoint — 2026-10-21

Narrow owner exception authorizes two report-policy files for exact14-method
inventory integration. [Current readiness packet](../implementation/t-002-03-identity-migration-evidence.md#t-002-03-ri-readiness-integration--2026-10-21)
records authorized before/after hashes; nine other Foundation paths, V3 and Java
test sources remain byte-identical. This supersedes the earlier inventory-absent
finding ONLY at source/policy level, not runtime acceptance.

REQ-002-01..05 / T-002-03 schema-to-method mapping now has a finite mandatory
PostgreSQL collector identity: all14 methods required PASS, no H2/architecture/
foundation acceptance.19 prior required class/method lists preserved; all four
collector function ASTs unchanged.67 Python policy controls PASS (56 existing,
11 new), including each missing/renamed/outcome case and sanitized publication;
15 migration source tests PASS. PostgreSQL expected inventory270 PASS/5 skips is
synthetic/source-derived, NOT an executed database result.

Selected existing hosted Ubuntu path is evidenced at baseline run39211616700,
not on current uncommitted changes. RI-LIFECYCLE-01 identifies shared runtime
proof overwrite/single-volume audit coverage for two class-scoped containers;
source-level gap, no observed leak. Correction outside inventory scope requires
owner approval. **T_002_03_CI_READY_RUNTIME_PENDING**, not VERIFICATION_READY.
Java/PostgreSQL/remote execution0; actual runtime privacy/cleanup UNVERIFIED.
T-002-03 stays IMPLEMENTED_VERIFICATION_PENDING; T-002-04 NOT STARTED;
Feature002 ACs remain NOT ACCEPTED, Feature001/D1-08/Day1 PARTIAL.

## RI-LIFECYCLE-01 source correction — 2026-10-21

[Current correction evidence and exact source hashes](../implementation/t-002-03-identity-migration-evidence.md#ri-lifecycle-01-source-correction--2026-10-21)
records OWNER_RI_LIFECYCLE_01_CORRECTION: GO. Dedicated known-suite records now
preserve observed/validated resource provenance; cleanup auditing requires both
suites, current execution/job/attempt, exact run/session/labels, distinct PG and
volume identities and explicitly shared Ryuk. Missing/failed/truncated/stale/
foreign/substituted/extra metadata or remaining resources cannot pass.

88 current process-free Python tests PASS:67 original report/Identity inventory
controls plus21 lifecycle controls;15 migration static tests freshly PASS. These
are synthetic parser/audit/source/wiring controls, not Java filesystem/SDK, Docker,
SQL constraint or actual cleanup execution. REQ-002-01..05 database/test mapping
and exact14-method required inventory are unchanged. Java/SQL suite sources,
contract, canonical REQ/AC and nine other Foundation paths remain byte-identical.

Existing private task-c-runtime.txt entrypoint now consumes both suite records;
workflow/launcher unchanged. **RI_LIFECYCLE_01_CORRECTED_CI_READY** means source
and wiring readiness, not runtime acceptance. Current code is uncommitted; future
authorized publication must include reviewed compile/test/SDD dependency closure
and use the resulting exact commit for hosted CI. No current GitHub run is claimed.
T-002-03 remains IMPLEMENTED_VERIFICATION_PENDING, T-002-04 NOT STARTED;
Feature002 ACs NOT ACCEPTED, Feature001/D1-08/Day1 PARTIAL. Main traceability stays
protected. No real cleanup PASS or observed resource leak is asserted.
