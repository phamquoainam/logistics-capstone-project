# Feature 002 recovery — supplemental traceability

Current bounded Session checkpoint:
**T_002_05_SESSION_CI_VERIFIED**, sourcecaa150fb/run39932569608.
[Latest verified evidence and limitations](#session-ci-verified-trace---2026-10-23).
Previous Activation checkpoint remains CI_VERIFIED on081c14c/run39920826371.
[Activation evidence](#activation-ci-verified-trace---2026-10-23).
T-002-05 remains PARTIAL; Feature002 and AC-002-01..05 NOT ACCEPTED.

Migration checkpoint: **T-002-03 DONE** on run39575568268 attempt1, using verified
sanitized artifacts and exact source revision. T-002-04 is now
DONE under actual CI; T-002-05 Application PARTIAL, first grant slice CI_VERIFIED on1698940/run39641670408.
[Domain source/test acceptance](../implementation/t-002-04-identity-domain-evidence.md#t-002-04-actual-ci-acceptance--2026-10-21).
[Final migration acceptance and handoff](../implementation/t-002-03-identity-migration-evidence.md#final-migration-acceptance--2026-10-21).
Earlier checkpoints below are historical and superseded only within that
migration scope. Production AC-002-01..05 remain NOT ACCEPTED.

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

## T-002-03 final migration acceptance — 2026-10-21

Owner-delegated Migration DoD is **DONE**. Actual
[run39575568268 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39575568268)
tests feature head bf80d84a459e2b6c8f8431c47dcbbe9f0e84c817 through synthetic
checkout f426491f9b02c3045333f8296f6b1e320e4b7339, verified in all three consoles.
[Artifact identities/digests, exact14 method outcomes and canonical DoD](../implementation/t-002-03-identity-migration-evidence.md#final-migration-acceptance--2026-10-21)
now supersede the earlier source-only/pending migration results above.

| Requirement / task | Actual PostgreSQL migration evidence | Remaining runtime acceptance |
|---|---|---|
| REQ-002-01 / T-002-03 | Username/lifecycle/grant constraint cases PASS | Production multi-role/object authority and lifecycle service guards not yet accepted |
| REQ-002-02/05 / T-002-03 | Session deadline/FK and CSRF storage cases PASS | Actual request admission, CSRF and meaningful activity accounting remain future tasks |
| REQ-002-03/05 / T-002-03 | Refresh hash/lineage/proof cases PASS | Crypto/password execution, rotation/replay concurrency and delivery remain unaccepted |
| REQ-002-04/05 / T-002-03 | Receipt binding and runtime append-only/permission cases PASS | Atomic grants/audit/revocation, authorized replay and last-admin serialization remain unaccepted |
| REQ-002-01..05 / Migration DoD | Cold V1/V2/V3, V2 upgrade/Demo preservation, indexes/ownership, drift rejection PASS;14/14 Identity tests, corrected Foundation rerun case and retained regressions PASS | No develop post-merge or full-feature acceptance inferred |

Verified artifact summaries: Architecture23 PASS/0 skip; H2231 PASS/17 expected
skips; PostgreSQL270 PASS/5 expected skips;0 failures/errors and no prohibited
skips. SHA-256 and exact sanitized inventories match.88 report-policy controls
PASS per job; actual credential/privacy and PostgreSQL owned cleanup audits PASS.
Scope remains migration plus existing backend regression, not general security
certification or Redis/RabbitMQ GitHub runtime coverage.

T-002-04 Domain READY / NOT STARTED; bounded handoff and required unit cases are
in final evidence. AC-002-01..05 remain NOT ACCEPTED; Feature002 incomplete;
Feature001/D1-08/Day1 PARTIAL. Main traceability reconciliation, documentation
commit/push and PR20 review/merge remain separate authorized integration work;
none is performed here and no canonical REQ/AC or accepted contract is changed.

## T-002-04 Domain source checkpoint — 2026-10-21

Owner-authorized pure-Java implementation, **IMPLEMENTED_VERIFICATION_PENDING**.
[Exact classes,48 unit methods, fingerprints and execution handoff](../implementation/t-002-04-identity-domain-evidence.md).
This supersedes Domain READY / NOT STARTED only; T-002-03 DONE is preserved.

| Requirement / accepted rule | Domain source and required unit cases | Actual evidence / remaining limit |
|---|---|---|
| REQ-002-01/04, O5/O6 | IdentityAccount, Grant/RoleCode/ResourceScope, ManagementAuthority/AccountVersion; IdentityAccountTests10 + IdentityAuthorizationTests10 | Written lifecycle/full-target union/assignability/self/last-admin/default-deny assertions; Java UNVERIFIED; current facts/serialization require Application |
| REQ-002-01/03, O4 | Username/PasswordPolicy, IdentityValueTests9 | Exact Unicode/case, new/legacy/UTF-8/malformed/immutability/redaction assertions written; no BCrypt execution |
| REQ-002-02/05, DEC-03/O3 | IdentitySession, IdentitySessionTests9 | Deadline/access cap, meaningful activity, revocation/account/authVersion checks written; no actual HTTP admission/security test |
| REQ-002-03/05, DEC-04/O3/O4 | ActionProof/RefreshFamily, IdentityProofAndRefreshTests10 plus Account lifecycle tests | Purpose/subject/stored-email-match eligibility, TTL/consumption/replay revocation results written; no atomic persistence, delivery or concurrent replay proof |

Strict CI adds all48 finite Domain identities as required PASS in H2 and
PostgreSQL, not Testcontainers-only. All20 previous class lists and skip sets
preserved; collector function ASTs unchanged.93 Python controls PASS (88 retained
+5 new); final five focused controls PASS. Source-derived future expectations:
Architecture23/0 skips, H2279/17, PostgreSQL318/5; these are NOT executed Java
counts. Current Domain sources are uncommitted and not covered by prior PR20 CI.
No approved current-revision contained JVM path is established; no local Java /
Maven/DB execution or CI trigger here. Domain canonical checkbox remains open.
T-002-05 not started; production AC-002-01..05 NOT ACCEPTED, Feature002 incomplete,
Feature001/D1-08/Day1 PARTIAL. Main traceability/T-002-03 evidence and six
preexisting Foundation paths are preserved; no original REQ/AC changes.

## T-002-04 actual CI acceptance — 2026-10-21

**T-002-04 DONE** under OWNER_T_002_04_CI_VERIFICATION_APPROVAL, replacing the
Domain source-only verification-pending classification above.
[Run39626192090 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39626192090),
event workflow_dispatch, actual source/checkout bbb10987faa8dbcf7a85ccffe72c7c299e8e38b0.
[Exact class/method outcomes, artifacts/digests and bounded handoff](../implementation/t-002-04-identity-domain-evidence.md#t-002-04-actual-ci-acceptance--2026-10-21).

| Requirement / task | Actual Domain evidence in BOTH backend jobs | Scope limit |
|---|---|---|
| REQ-002-01/04/05 / T-002-04 | IdentityAccountTests10 + IdentityAuthorizationTests10 PASS: lifecycle/version, full-target union, assignability, self/last-admin/default-deny/ownership scope decisions | No concurrent database transaction or real owner-provider enforcement proven |
| REQ-002-01/03 / T-002-04 | IdentityValueTests9 PASS: exact username, password/codepoint/UTF-8/malformed input, version/immutability/redaction | No BCrypt adapter or runtime credential storage acceptance |
| REQ-002-02/05 / T-002-04 | IdentitySessionTests9 PASS: absolute/idle/access caps, meaningful activity, account/authVersion/revocation | No production JWT/HTTP/CSRF/Origin/cookie admission acceptance |
| REQ-002-03/05 / T-002-04 | IdentityProofAndRefreshTests10 PASS: purpose/subject/lifetime/consumption and strict refresh-family replay/rejection results | No atomic rotation, replay concurrency, delivery or external side-effect proof |

Actual totals: Architecture23 PASS/0 skips; H2279 PASS/17 expected skips;
PostgreSQL318 PASS/5 expected skips;0 failures/errors and exact finite inventories.
PostgreSQL migration14/14 and Foundation rerun PASS. Three artifact ZIP digests
verified;93 report-policy controls per job, credential/privacy and PostgreSQL
owned cleanup PASS. Redis/RabbitMQ remote runtime is not claimed.
Canonical Domain DoD satisfied without changing REQ/AC. T-002-05 READY / NOT
STARTED; AC-002-01..05 NOT ACCEPTED; Feature002 incomplete; Feature001/D1-08/Day1
PARTIAL. Only three post-CI SDD files change locally; separate documentation
commit/publication and PR integration remain unperformed. The successful run
tests bbb1098, not a later documentation revision. Main traceability and six
Foundation changes remain protected; no next-task implementation.

## T-002-05 first Application slice — 2026-10-21

OWNER_T_002_05_APPLICATION_IMPLEMENTATION: GO.
[Exact source/data/test mapping and runtime handoff](../implementation/t-002-05-identity-application-evidence.md).
Source implemented, not runtime accepted. T-002-05 remains PARTIAL and unchecked.

| Trace | Application/database effect | Authored evidence | Actual runtime |
|---|---|---|---|
| REQ-002-01/04, AC-002-01/02 | Complete grant replacement, current/requested-union authority, account/auth version and append-only audit | IdentityGrantApplicationTests positive/hidden-scope/default-deny/self cases | UNVERIFIED |
| REQ-002-02/05, AC-002-04/05 | Trusted session/account lookup and current admission, target session/refresh revocation | Current/revoked/expired/authVersion and disabled account cases | UNVERIFIED |
| REQ-002-04/05, AC-002-03 | Bound canonical command receipt before stale-version check, serial population/row locks | Authorized replay/conflicts, concurrent same-key/stale/opposite-admin cases | UNVERIFIED |
| REQ-002-04/05, AC-002-02/05 | Grants, versions, revocation, receipt and audit in one JDBC transaction | Real PostgreSQL fault triggers and six-table rollback snapshots | UNVERIFIED |
| REQ-002-02/03, AC-002-04 | Safe public Account fields and redacted representations, finite sanitized reports | Result field test;97 process-safe Python policy controls PASS | Java privacy/cleanup UNVERIFIED |

33 Java integration methods authored and required only in the PostgreSQL job;
no allowed new skips. Java lifecycle manifest/Python strict collector both
require the exact new owned identity-grants suite; no weakening of image,
ownership, cleanup or privacy checks. Existing workflow/check names, contract,
V1/V2/V3 and protected main traceability remain unchanged.
No current Java/Maven/Docker/CI execution or publication; historical Domain
run39626192090 is not evidence for this working tree. Missing owner provider
denies; synthetic test authority is not a production integration.
T-002-01..04 DONE; Feature002 and AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. Other Application operations remain pending.

## T-002-05 first slice actual CI verification — 2026-10-22

**T_002_05_FIRST_SLICE_CI_VERIFIED** supersedes source-only pending for this
slice, not the full Application task.
[Exact33 outcomes, transaction/authority/cleanup evidence and remaining scope](../implementation/t-002-05-identity-application-evidence.md#t-002-05-first-slice-actual-ci-verification--2026-10-22).
Run39641670408 attempt1 workflow_dispatch, exact checkout/source
1698940e4d4d888993daa2e38e995014c4f3282b on feature/002-identity-application-ci.

| Trace | Actual tested evidence | Limit |
|---|---|---|
| REQ-002-01/04, AC-002-01/02/04 | PostgreSQL stored full grant replacement, current/requested-union/default-deny/self guards, account/auth versions, audit/receipt PASS | Synthetic trusted authority port; real owner composition pending |
| REQ-002-02/05, AC-002-04/05 | Current account/session admission, revoked/expired/authVersion/disabled checks and target session/refresh invalidation PASS | Not production JWT/HTTP/CSRF/cookie admission |
| REQ-002-04/05, AC-002-03/05 | Same-key replay/conflicts, stale versions, real concurrent same-key/competing-version/opposite-admin tests PASS | Bounded grant lock protocol; other lifecycle commands pending |
| REQ-002-04/05, AC-002-02/05 | Real-DB audit/receipt/refresh-revocation fault triggers with six-table rollback snapshots PASS | Not general failure/availability certification |
| REQ-002-02/03, AC-002-04 | Safe account field/redacted representations, finite sanitized reports and credential/privacy/owned-cleanup guards PASS | No full security or production AC acceptance |

Actual totals: Architecture23 PASS/0 skips; H2279 PASS/17 expected skips;
PostgreSQL351 PASS/5 expected skips;33/33 grant integration,48/48 Domain in
BOTH backend jobs and14/14 PostgreSQL migration methods PASS.97 policy
controls per job PASS. Three ZIP SHA-256 digests and complete finite XML
inventories verified; all registered Testcontainers suites accounted for.
No Redis/RabbitMQ remote test claim. Minimum24h retention source guard kept.

Exactly14 Application paths committed/pushed once; no Foundation/private
artifacts. PR20 already MERGED at preflight, no change by this task; Domain/
Migration/develop refs preserved. Three SDD closure files change after CI and
remain uncommitted, not part of tested1698940. T-002-01..04 DONE;
T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. Other Application use cases and real production
authority integration are next bounded dependencies; no automatic execution.

## T-002-05-AUTH authority composition — 2026-10-22

[Bounded source, trust boundaries, exact dependency and test evidence](../implementation/t-002-05-identity-application-evidence.md#t-002-05-auth--bounded-production-composition--2026-10-22).
**T_002_05_AUTHORITY_DEPENDENCY_BLOCKED** for full production ownership integration.
New independently valid source implemented, NEW Java/PostgreSQL UNVERIFIED.

| Trace | New composition/test coverage | Current evidence / limitation |
|---|---|---|
| REQ-002-02/05, AC-002-04/05 | Existing persisted session/account admission wired through production factory; provider failure/default-deny wrapper |8 pure unit and current-state PostgreSQL tests authored; no new runtime PASS |
| REQ-002-01/04, AC-002-01/02/04 | Existing full-target/union/self/version Domain checks retained after owner lookup |11 added PostgreSQL denial/context/concurrency methods; original33 bodies unchanged |
| REQ-002-04/05, AC-002-03/05 | Existing atomic transaction, last-admin protocol, receipts/audit/revocation and24h protection retained | Core source unchanged; prior33 CI PASS remains1698940 evidence only |
| O5 / owner boundary | Require approved actor-bound target/resource assignability and complete context | No suitable owner public contract; default DENY even for IDENTITY/empty target; no resource-existence-to-permission shortcut |

Production source adds FailClosedGrantAuthority and ProductionIdentityGrantComposition;
no new ownership abstraction, global role catalog, cross-module dependency,
network call, REST endpoint or synthetic production authority. MerchantAccess
require(UUID) is insufficient for actor-bound management; other owner APIs and
Identity scope/delegation semantics are absent. Module Catalog forbids reverse
Identity-to-Merchant/business imports, so a compatible approved public-contract
bridge is a dependency, not an implicit architecture exception.

101 process-safe report-policy controls PASS, not Java tests. Exact8 unit +
11 PostgreSQL cases added; no existing requirements/skips/lifecycle registrations
removed. Java/PostgreSQL/ArchUnit/privacy/cleanup for new source remain
UNVERIFIED pending separate authorization. All earlier CI closure content is
retained, not repurposed as verification of these additions.

T-002-01..04 DONE; T-002-05 PARTIAL/unchecked; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. Main traceability, six Foundation files,
T-002-03 evidence, V1/V2/V3, accepted contracts/ADRs and publication refs preserved.
No commit/push/PR/merge, runtime, workflow dispatch or next-slice execution.

## T-002-05-AUTH-CONTRACT authority decision packet — 2026-10-22

**T_002_05_AUTHORITY_CONTRACT_READY_FOR_OWNER_APPROVAL**.
[Policy/protocol/transaction decision packet](../implementation/t-002-05-identity-application-evidence.md#t-002-05-auth-contract--owner-decision-packet--2026-10-22).
This is PROPOSED / OWNER_DECISION_REQUIRED, not accepted owner authorization.

| Requirement | Accepted safeguard | Missing decision / future evidence |
|---|---|---|
| REQ-002-01/02; AC-002-01/04 | Exact role/resource checks, current actor/session and full-target union | AUTHC-D1 Identity realm/delegation policy; AUTHC-D2 public owner SPI/bridge; AUTHC-01..07 planned |
| REQ-002-04; AC-002-02/04 | No Admin-to-Finance implication, self/last-admin guards, audit atomicity | Owner-specific management/assignability implementation; unknown/existence-only cases must deny |
| REQ-002-05; EC-002-01/03/05; AC-002-03/05 | Revocation/authVersion, transactional replay and concurrency guards | AUTHC-D3/D4 direction/owner-lock policy; real authority-change races and transaction-participation proofs AUTHC-08..11 |
| REQ-002-03 | No secret/principal/proof material in authority protocol or logs | Future production HTTP/session/security verification remains separate |

No positive Identity-local provider is invented: accepted managementAdmin and
ManagementAuthority define entry/consumption, not delegation. Proposed initial
exact-pair realm policy (including unresolved-empty-target denial) needs explicit
owner approval; it is not a change to accepted Domain tests or canonical REQ/AC.
External resource ownership/assignability remains DEPENDENCY BLOCKED/default-deny.
Narrow owner APIs must not infer permission from MerchantAccess.require(UUID).

Only three SDD supplements appended; all previous source/tests/closure evidence,
six Foundation paths and T-002-03 record preserved. Existing19 AUTH Java cases
remain UNVERIFIED; earlier run39641670408 covers1698940 first slice only.
No runtime/publication or new production source. T-002-05 PARTIAL; T-002-01..04
DONE; Feature002/AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## T-002-05-AUTHC-RESOLUTION restricted decision — 2026-10-22

**T_002_05_IDENTITY_SCOPE_MAPPING_BLOCKED**.
[Conditional owner decision and exact evidence](../implementation/t-002-05-identity-application-evidence.md#t-002-05-authc-resolution--restricted-owner-decision--2026-10-22).

| Requirement / edge | Current checkpoint | Verification limitation |
|---|---|---|
| REQ-002-01/02; AC-002-01/04 | D1 explicit authority/full union; no wildcard or ordinary-grant delegation. scopeId authoritative resource + assignability mapping still missing | No allowing provider; one owner model decision remains |
| REQ-002-02/04; AC-002-02/04 | Owner-approved empty-target replacement denial added before trusted delegate; provisioning remains separate; external scopes stay denied | Two new unit/PG negative cases authored, UNVERIFIED |
| REQ-002-05; EC-002-01/03/05; AC-002-03/05 | Existing Identity lock/version/revocation/audit/receipt protocol retained; D4 approves no independent owner transaction assumption | Current Java/PG/concurrency runtime still UNVERIFIED |
| REQ-002-03 | Safe denial, no private provider diagnostics or credential reads; exact report/privacy guards retained |102 synthetic Python policy controls PASS; not a new runtime or HTTP security acceptance |

D2 is approved in principle within existing directions, not acceptance of every
proposed DTO or external dependency; no SPI implementation/new module imports.
Previous configured realm/actor-held delegation/owner-lock proposals are not
silently accepted. D3 keeps all unsupported external authority denied.
Original33 grant/48 Domain/14 migration and earlier19 AUTH methods preserved;
one unit +one PG case added (authority total21 unverified). Collector functions,
privacy/cleanup logic and lifecycle registry unchanged. Only the documented
five source/test/policy paths plus three SDD supplements changed here.
Main traceability, six Foundation changes, T-002-03 record and prior closure
content preserved; no runtime/publication. T-002-05 PARTIAL;
Feature002/AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## T-002-05-AUTHC-REALM architecture checkpoint — 2026-10-22

**T_002_05_REALM_MIGRATION_APPROVAL_REQUIRED**.
[Authoritative Realm decision and proposed V4/authority gates](../implementation/t-002-05-identity-application-evidence.md#t-002-05-authc-realm--registered-identity-realm-decision--2026-10-22).

| Requirement / guard | Decision or proposal | Evidence boundary |
|---|---|---|
| REQ-002-01/02; AC-002-01/04 | Owner establishes registered Realm UUID semantics for IDENTITY.scopeId; one system Realm proposed, no Merchant/tenant identity inference | Persistence missing in V3; explicit V4 registry/conditional FK/legacy-data review remains OWNER_DECISION_REQUIRED |
| REQ-002-02/04; AC-002-02/04 | Registered Realm != actor authority; ordinary grant != delegation; full-target union, self/last-admin and empty-target denial retained | Same-Realm IDENTITY_MANAGEMENT assign/revoke policy proposed, not enabled/accepted |
| REQ-002-05; AC-002-03/05 | Existing local lock/version/revocation/audit/receipt protocol preserved; Realm state changes proposed to join its locking protocol | REALM-08 two-transaction proof pending; no cached/cross-module consistency assumed |
| REQ-002-03 | No credential access, runtime fallback registration or authority from client claims | Current102 synthetic report-policy controls PASS; future real-source privacy/cleanup still required |

Architectural meaning is conditionally approved; registration/activation and
delegation implementation need REALM-P1..P3. REALM-01..09 are planned gates,
not authored/executed Java methods. Source count remains9 authority unit /
45 grant PG with21 authority additions UNVERIFIED; original33/48/14 retained.
Only three SDD append-only updates in this turn; all previous source, empty-target
restriction, strict policy/cleanup, V1/V2/V3, acceptance evidence and six Foundation
changes preserved. T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. No SQL/runtime/Git publication or new feature.

## T-002-05-REALM-IMPLEMENTATION checkpoint — 2026-10-22

**T_002_05_REALM_PROVIDER_IMPLEMENTED_VERIFICATION_PENDING**.
[Source, tests, actual process-safe outcomes and runtime dependency closure](../implementation/t-002-05-identity-application-evidence.md#t-002-05-realm-implementation--approved-local-realm-provider--2026-10-22).
[Accepted bounded Realm ADR](../architecture/decisions/ADR-0010-identity-system-realm.md).

| Requirement / acceptance | Implemented source | Actual evidence limitation |
|---|---|---|
| REQ-002-01/02; AC-002-01/04 | V4 authoritative registration/conditional FK, actual locked account-grant target context, exact same-Realm delegated management pair, real default JDBC provider | Unit8 + provider7 + migration/bootstrap8 new methods authored; Java/SQL UNVERIFIED |
| REQ-002-02/04; AC-002-02/04 | Owner-only pending-only bootstrap/one-time prerequisite activation, no existing-account takeover/global/ordinary-grant/external fallback; Domain self/last-admin retained | Bootstrap privilege/rollback/runtime proof pending; accepted invitation workflow still separate |
| REQ-002-05; AC-002-03/05 | Existing lock/version/session/refresh/audit/receipt/replay/24h protocol reused; Realm state changes join its Identity lock protocol | New real-DB deactivation race + audit rollback tests authored, not executed |
| REQ-002-03 | No credential bootstrap input, secrets/PII publication or runtime registry write; safe provider failure and exact privacy/report guards |104 synthetic report-policy and15 source controls PASS; real process/log/cleanup acceptance pending |

Current source has9 retained authority unit,8 new Realm policy unit,52 grant PG
(33 original +12 retained AUTH +7 Realm),22 migration (14 original +8 Realm) and
unchanged48 Domain. Future totals Architecture23/0 skips, H2296/17 skips,
PostgreSQL395/5 skips are required inventory expectations, not actual CI outcomes.
Earlier39641670408 remains historical1698940 evidence only. OpenAPI bytes and
V1/V2/V3 unchanged; README clarification explicitly records owner-approved meaning.
Previous SDD content and six Foundation changes preserved; no source dispatch/
publication/runtime/credential access. T-002-05 PARTIAL; Feature002 and AC-002-01..05
NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## T-002-05-PROVISION bounded source checkpoint — 2026-10-22

[Detailed implementation/evidence/limits](../implementation/t-002-05-identity-application-evidence.md#t-002-05-provision--bounded-invitation-provisioning-implementation--2026-10-22).
[Owner-approved initial-pair scope](../architecture/decisions/ADR-0010-identity-system-realm.md#bounded-ordinary-provisioning-extension--owner-approved-2026-10-22).

Prior Realm source4a90baf/run39795074146 attempt1 SUCCESS is reconciled:
23 Architecture,296 H2/17 skips,395 PostgreSQL/5 skips,104 policy controls/job,
exact artifact digests/inventories and credential/privacy/owned cleanup PASS.
This supersedes old Realm pending states, not new provisioning verification.

| Trace | Current implementation/authored evidence | Actual status / limit |
|---|---|---|
| REQ-002-01/04; AC-002-01/02/04 | Owner-approved explicit same-Realm initial management pair; pending/unverified new account; full grant-set/default-deny policy |12 unit methods authored; Java UNVERIFIED; external scopes denied |
| REQ-002-02/05; AC-002-02/04/05 | Current stored actor/session/authVersion and ACTIVE Realm, same DataSource/admin lock, replay target union | New real-provider PostgreSQL denial/concurrency methods authored, not executed |
| REQ-002-04/05; AC-002-03/05 | Atomic account/grants/proof/audit/receipt/intent port; username race; exact replay/conflict; minimum24h receipts |18 PostgreSQL methods appended, old52 bodies retained; synthetic test delivery only |
| REQ-002-03; AC-002-02/04 | No password/session issue or plaintext proof in API/audit/receipt; metadata validation and material cleanup | Privacy107 policy controls PASS; production cryptographic issuer/durable protected delivery missing |

Three Task-C suite resource registries, expected skip categories, V1..V4,
accepted contracts and original Domain48 are unchanged. New future totals:
Architecture23/0, H2308/17, PostgreSQL425/5; these are NOT current runtime PASS.
107 process-safe report controls and15 static migration checks PASS. Production
composition without a reviewed invitation adapter fails before any account write.
No production bootstrap, new migration, provider, credentials, JVM/Docker runtime,
Git publication or CI dispatch. Main traceability remains protected.

T_002_05_PROVISIONING_IMPLEMENTED_VERIFICATION_PENDING for bounded components;
production delivery dependency BLOCKED. GRANT_MUTATION_FUNCTIONALITY_LIMITED
remains for replacement; first-pair policy approval is not full lifecycle/runtime
acceptance. T-002-01..04 DONE; T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED.


## Invitation Outbox source checkpoint — 2026-10-23

T-002-05 PARTIAL; secure issuer and durable encrypted V5 invitation outbox are
IMPLEMENTED_VERIFICATION_PENDING under the owner's bounded approval.
[Evidence and exact 30-path manifest](../implementation/t-002-05-identity-application-evidence.md#t-002-05-invitation-outbox-checkpoint--2026-10-23),
[ADR-0011](../architecture/decisions/ADR-0011-identity-invitation-outbox.md).
REQ-002-01/04 -> pending controlled initial-pair provisioning, audit and transactional
intent; REQ-002-02/05 -> current persisted actor/session/Realm and replay admission;
REQ-002-03 -> separate-key HMAC/AEAD protected proof/delivery material.
AC-002-02/03/04/05 now have 10 crypto + 15 PostgreSQL outbox + 3 V5 migration
methods AUTHORED, not executed. Python source/report controls PASS25/110 do not
prove Java compilation, PostgreSQL enforcement or HTTP/runtime AC. All original
REQ/AC text and main traceability remain unchanged; AC-002-01..05 NOT ACCEPTED.
Production sender/maintenance schedule/activation remain dependencies; physical
purge is invocation-driven. GRANT_MUTATION_FUNCTIONALITY_LIMITED remains.


## Provisioning and Secure Outbox CI evidence - 2026-10-23

Bounded checkpoint **T_002_05_PROVISION_OUTBOX_CI_VERIFIED** for source
0098461f2c4b242956c401430e5d5ca304521eb8; [run39896561084 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39896561084),
workflow_dispatch on feature/002-identity-application-ci.
[Evidence, exact inventories and digests](../implementation/t-002-05-identity-application-evidence.md#provisioning-and-secure-outbox-ci-closure---2026-10-23).

| Requirement / acceptance coverage | Actual evidence | Acceptance limitation |
|---|---|---|
| REQ-002-01/04 controlled pending provisioning, grant and audit ownership | Provision unit12/12; PG18/18; Grant/Application85/85; migration25/25 | Bounded initial Identity pair only; no general delegation |
| REQ-002-02/05 current session/authVersion/Realm admission | Realm unit8/8, authority unit9/9, Realm PG7/7; repaired concurrent actor/Realm admission method PASS | Production HTTP admission remains a later task |
| REQ-002-03 protected invitation material | Crypto10/10 and real secure outbox PG15/15, credential/privacy guards PASS | No actual email, production key-store deployment or sender proof |
| AC-002-02/03/04/05 supporting transaction, replay, race and authorization tests | Architecture23; H2318/17 expected skips; PG453/5 expected skips; zero FAIL/ERROR; all3 artifact digests verified | Supporting evidence only; no full runtime AC acceptance |

These group counts overlap. Full Domain48 and V1-V5 cold/upgrade/ownership/
constraint/permission checks passed. Exact report inventory, 110 report-policy
controls/job, credential privacy and owned Testcontainers cleanup PASS.
The main Foundation traceability is protected and unchanged. This append occurs
after tested SHA0098461f and is not part of its CI-tested source.
T-002-05 remains PARTIAL. Feature002/AC-002-01..05 NOT ACCEPTED.
Pending: sender/worker, maintenance/scheduled purge, invitation consumption,
password setup/account activation, remaining Application use cases and subsequent
HTTP/integration acceptance. Preserve GRANT_MUTATION_FUNCTIONALITY_LIMITED.


## Invitation activation source trace ? 2026-10-23

T-002-05 bounded source IMPLEMENTED_VERIFICATION_PENDING under explicit owner
approval; original REQ/AC and canonical DoD remain unchanged.

| Trace | Authored source/tests | Actual evidence |
| --- | --- | --- |
| REQ-002-01; DEC-04; ADR-0009/0010 |AcceptIdentityInvitation, Domain activate;20 activation PG methods |Source only; real activation/Realm/concurrency UNVERIFIED |
| REQ-002-03; O4; ADR-0011 |ProtectedInvitationCredentials;12 Application unit methods; password/token/key negatives |No new JVM/BCrypt/JCE runtime; policy controls are not crypto proof |
| REQ-002-04/05; transaction/idempotency |Proof receipt, audit, version/authVersion, revoke/rollback/race cases |Existing85 PG bodies preserved; new runtime UNVERIFIED |
| CI inventory/privacy |Exact12 unit +20 PG identities; same skips and owned lifecycle |113 Python policy PASS;15 migration-source PASS; Outbox source9/10 methods with V3/V4 CRLF hash limitation |

[Full sanitized evidence and exact manifest](../implementation/t-002-05-invitation-activation-evidence.md).
T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED; grant limitation retained.
Main traceability and original six Foundation files unchanged. No business-feature
readiness/approval is inferred from Identity Admin provisioning or activation.


## Activation CI outcome ? 2026-10-23

[Actual run39916095981 attempt1 and verified sanitized artifacts](../implementation/t-002-05-invitation-activation-evidence.md#activation-ci-checkpoint--2026-10-23)
test source888da7e9d8004cbb887157b5d9e4a87d0c4b48db on
feature/002-identity-activation. Architecture23 PASS/0 skips; H2330 PASS/17 exact
expected skips, all12 Activation unit cases PASS, Domain48 and crypto10 retained.
PostgreSQL job FAILURE at Maven Wrapper bootstrap before clean verify;20 new
Activation PG and85 preserved integration identities remain runtime UNVERIFIED.
No PostgreSQL counts/artifact, migration/concurrency or Testcontainers audit PASS
is inferred. Only the separate disposable PostgreSQL cleanup step passed.
113 Python policy controls PASS per job; two artifact ZIP digests and sanitized
inventories/privacy allowlists verified. Required migration/outbox Python source
suites are not invoked by existing workflow; CI coverage remains UNVERIFIED.
V1-V5 publication blobs unchanged; V3/V4 canonical LF hashes match accepted pins;
Windows CRLF source-guard limitation is recorded without weakening checks.

T_002_05_ACTIVATION_CI_FAILED, T-002-05 PARTIAL; Feature002/ACs NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. These are LOCAL post-CI docs edits,
not CI-tested source or an amended commit; docs-only closure publication remains
separate. No PR/rerun/merge/repair. Original Foundation and main traceability
untouched; business-feature owner-review boundary remains in force.


## Activation CI verified trace - 2026-10-23

**T_002_05_INVITATION_ACTIVATION_CI_VERIFIED**, authorized by
`OWNER_ACTIVATION_CI_EVIDENCE_CLOSURE: CONDITIONAL_GO`.
[Exact evidence, inventories, artifact digests and historical gaps](../implementation/t-002-05-invitation-activation-evidence.md#activation-ci-verified-closure---2026-10-23).
[Actual run39920826371 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39920826371):
SUCCESS, `workflow_dispatch`, `feature/002-identity-activation`, tested/checkout
SHA `081c14cc059b61ddfabc2b1f4d1dbf7d682a70f7` in all three jobs, without a PR
synthetic merge revision. This supersedes prior runtime UNVERIFIED/CI_FAILED
statements only for that tested Activation checkpoint; history is preserved.

| Existing requirement support | Actual evidence | Remaining acceptance boundary |
| --- | --- | --- |
| REQ-002-01; DEC-04; ADR-0009/0010 | Main/test compilation;12/12 Activation unit cases in H2 and PG;20/20 Activation PG cases with actual proof/password/account/version effects | Application-only; no production HTTP, bootstrap Realm activation or login |
| REQ-002-03; O4; ADR-0011 | Crypto10, secret-negative/redaction cases, retained-key rotation; credential/privacy audits PASS | No email/provider deployment or full secret-store acceptance |
| REQ-002-04/05; transaction/idempotency | Activation rollback/replay/concurrency PASS;85/85 retained Grant/Application PG cases, total105/105; migration25/25 | Remaining session/recovery/resend/lifecycle/query/authority use cases pending |
| Exact CI/report/privacy inventory | Architecture23 PASS/0 skips; H2330 PASS/17 expected skips; PG485 PASS/5 expected skips; zero FAIL/ERROR;113 report-policy controls/job | Three ZIP SHA-256 match GitHub metadata; these are source081c14c results, not later docs-only CI |

PostgreSQL generated credential privacy, owned Testcontainers cleanup and separate
disposable PostgreSQL cleanup PASS. H2/PG exact inventories include all12 unit
identities; PG includes all20 new and85 retained method identities. Counts in
supporting groups overlap. Redis/RabbitMQ FoundationDependencyTests5 are expected
skips, not verified remote infrastructure tests. Source guard/V1-V5 Git blob
integrity was rechecked separately without changing SQL or accepted pins.

Historical run39916095981 failed at `bash ./mvnw --version` before clean verify/
PostgreSQL tests. The bounded diagnostic-only patch preserves exit status,
allowlisted output/UNKNOWN fallback and private cleanup;21 synthetic controls
PASS. Bootstrap now succeeded; original root cause remains OPEN/NOT REPRODUCED,
not permanently fixed. The workflow still does not invoke either migration or
outbox Python source suite. Their CI coverage is UNVERIFIED; preserve Windows
CRLF/LF9-of10 outbox-source finding and unchanged V1-V5 canonical blobs.

T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED;
Feature001/D1-08/Day1 PARTIAL. GRANT_MUTATION_FUNCTIONALITY_LIMITED remains.
Session/recovery/resend/lifecycle/query and remaining authority/security work are
pending. HTTP secure mapping belongs to later canonical T-002-06; sender/provider/
maintenance belongs to T-002-08 after dependencies. No deployment or business-module
readiness is implied; owner business-feature review boundary remains unchanged.

This record and linked evidence are the only two paths in the later documentation
publication. Source CI does not test that docs-only commit. Protected main
traceability and six original Foundation changes remain untouched. No source,
SQL, contract, workflow, policy, credentials or runtime behavior changes here.


## Identity Session Application source trace - 2026-10-23

[Bounded Session source and actual evidence](../implementation/t-002-05-identity-session-evidence.md).
T_002_05_SESSION_APPLICATION_IMPLEMENTED_VERIFICATION_PENDING; source baseline
merged PR23/d0615d2. Prior Activation CI proves only its historical revision.

| Trace | Implemented support | Actual evidence / limit |
| --- | --- | --- |
| REQ-002-01/02; ADR-0009/0010 | Authoritative exact credentials/account/ACTIVE Realm, no pending/disabled/Demo fallback | New20 unit cases PASS; real session PostgreSQL admission UNVERIFIED |
| REQ-002-03 | Real BCrypt verification,256-bit refresh material/digest-only persistence, secret-safe owned buffers and mandatory security admission port | Crypto/unit negatives PASS; full HTTP/Origin/CSRF/throttle/JWT signing remain T-002-06 |
| REQ-002-05; AC-002-02/03/04/05 support | Session deadlines/authVersion, atomic rotation, durable strict replay revocation, scoped idempotent logout |18 PostgreSQL cases AUTHORED, not executed; transaction/concurrency acceptance pending |
| CI inventory/privacy | Every new20+18 method mandatory; preserve123 Grant/Application identities and three lifecycle suites |116 Python report-policy controls PASS, including290 method mutations; no skip relaxation |

Focused Java55/55 (Session20/Activation12/Architecture23) and migration static15/15
PASS. Full H2 result and current-source PostgreSQL/CI coverage are separately
recorded in linked evidence. Future PG523/5 skips is not an executed result.
V1-V5/Domain/contracts/Demo, main traceability and six original Foundation paths
preserved. T-002-05 remains PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains; no business-feature readiness.

Session local full default/H2 clean verify:350 PASS/0 FAIL/0 ERROR/17 expected
skips; actual strict report guard exit0. This does not prove PostgreSQL or new
GitHub CI. Current-source Session PostgreSQL18 cases remain UNVERIFIED.


## Session CI verified trace - 2026-10-23

**T_002_05_SESSION_CI_VERIFIED**, under
`OWNER_SESSION_CI_EVIDENCE_CLOSURE: CONDITIONAL_GO`.
[Exact Session evidence, inventories and digests](../implementation/t-002-05-identity-session-evidence.md#session-ci-verified-closure---2026-10-23).
[Run39932569608 attempt1](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39932569608)
SUCCESS, `workflow_dispatch`, `feature/002-identity-session-application`;
actual tested/checkout SHA in all three jobs:
`caa150fb3c1a2c4ac1a60d27566f07f6a3c3c479`.
Earlier Session runtime-pending statements are historical and superseded only
for this source and bounded Application scope, not full feature acceptance.

| Existing trace | Actual CI evidence | Remaining boundary |
| --- | --- | --- |
| REQ-002-01/02; ADR-0009/0010 | Authoritative account/Realm eligibility;20/20 Session unit cases in H2 and PG;18/18 Session PG cases | Bounded Identity Management pair, unsupported owners denied; no production HTTP admission |
| REQ-002-03 | BCrypt/refresh digest-only persistence, privacy and secret-negative cases PASS | AccessGrant is signing facts, not JWT/principal; actual JWT/Origin/CSRF/cookie/throttle adapters pending |
| REQ-002-05; AC-002-02/03/04/05 supporting evidence | Real PostgreSQL authVersion/deadlines, rotation, cross-account logout, rollback and concurrency PASS | No full runtime AC acceptance; current-session/activity and remaining use cases pending |
| Exact CI inventory | Architecture23 PASS/0 skips; H2350 PASS/17 expected skips; PG523 PASS/5 expected skips; zero FAIL/ERROR | Session20+18 and retained105 PG cases overlap totals; combined Grant/Application123/123 PASS |

`sessionReplayCommitsFamilyRevocationBeforeDenial` and
`sessionConcurrentRefreshHasOneRotationThenReplayRevocation` PASS on real PG:
replay denial follows durable session/family revocation; concurrent reuse yields
one rotation then replay/family revocation, without two usable successors.
Mandatory-write rollback cases also PASS. Prior105 PG methods include20 Activation;
Domain48/migration25/provision/Realm/Grant/outbox inventories all retained and ran.

116 report-policy controls/job, exact identities/expected skips, credential/privacy
and Testcontainers owned cleanup PASS. Three sanitized ZIP digests match fresh
GitHub metadata, with only summary/XML allowlisted content and fixed skip text.
The five Redis/RabbitMQ foundation skips are not remote infrastructure PASS.
Migration/outbox Python source suites remain not invoked in CI; their CI coverage
is UNVERIFIED. Keep historical Windows CRLF/LF and bootstrap findings unchanged.

This trace and linked evidence form a separate later docs-only publication.
Run39932569608 verifies sourcecaa150fb, not that documentation commit. No raw
logs/reports/credentials enter tracked docs. Protected main traceability, six
original Foundation fingerprints, Activation worktree and immutable V1-V5 remain
unchanged. No new source/contract/security/CI policy modification is authorized.

T-002-05 remains PARTIAL/unchecked; Feature002 and AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED remains. Pending: current-session/activity,
recovery/resend/lifecycle/query, remaining authority/security and HTTP/JWT/CSRF/
cookie adapters; provider/delivery integration stays T-002-08 after dependencies.
Feature001/D1-08/Day1 remain PARTIAL. No deployment/end-to-end or business-module
readiness is implied; separate business-feature owner approval remains required.


## Session transaction ownership correction trace - 2026-10-23

[Scoped finding, guard, exact tests and local evidence](../implementation/t-002-05-identity-session-evidence.md#session-transaction-ownership-correction---2026-10-23).
REQ-002-02/03/05 and AC-002-02/03/05 gain corrective verification coverage only;
no requirement text or production feature AC is accepted by this checkpoint.

Session-only JDBC boundary rejects actual ambient transactions, synchronization
or any bound resource before shared REQUIRED participation; fixed internal failure,
no public contract change or caller transaction manipulation. Four guard unit tests
PASS within24 Session unit cases; five PG regressions authored/compiled, execution
UNVERIFIED (Docker CLI unavailable). Existing direct replay and concurrent refresh
remain mandatory. Full local Java21/Maven3.10.0 offline verify and strict H2 guard:
354 PASS/17 expected skips/0 failure/0 error; Architecture23, Activation unit12,
Domain48 and report-policy116 PASS. Migration Python source15 PASS. Outbox Python
source9/10 methods PASS, with2 pre-existing V3/V4 CRLF byte-pin subcase failures;
unchanged pre-edit bytes/HEAD canonical pins verified, no repair or assertion bypass.

Historical sourcecaa150fb/run39932569608 and later docs0551b279 are preserved.
Neither verifies this uncommitted correction. Current-source PostgreSQL (including
ownership/durable replay/concurrency and privacy/owned cleanup) plus new CI/PR
integration remain pending; synthetic inventory expectations are not runtime PASS.
T-002-05 PARTIAL, Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Protected main traceability,
Foundation changes, Activation, SQL V1-V5 and accepted contracts are unchanged.
No commit/push/dispatch/PR/merge or further-feature implementation authorized here.


## Session transaction ownership CI verified trace - 2026-10-23

[Exact provenance, security cases, artifact hashes and finding disposition](../implementation/t-002-05-identity-session-evidence.md#session-transaction-ownership-ci-verified-closure---2026-10-23).
Owner: OWNER_SESSION_SECURITY_CI_CLOSURE: CONDITIONAL_GO. This supersedes only the
preceding correction's PG-pending checkpoint; historical evidence is preserved.

REQ-002-02/03/05 and AC-002-02/03/05 have bounded runtime verification coverage:
[Backend verification run39986298097](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/39986298097),
attempt1, workflow_dispatch, feature/002-identity-session-application,
actual checkout SHA c851f5dc73809dafd3b22c4bf3cd81ccdcc1de17 in all three jobs.
Architecture: 23 PASS/0 skip; H2: 354 PASS/17 expected skips; PostgreSQL 18: 532 PASS/5
expected skips; all0 failure/error. Exact method inventories PASS;5 Redis/RabbitMQ
foundation skips are NOT remote infrastructure PASS.

Session unit 24/24 (new guards4/4); PG Session 23/23 (new methods5/5);
previous PG123/123 and combined128/128 PASS. These counts overlap overall 532.
The five exact new identities and independent connection checks are in the linked
evidence. They verify same runtime DataSource/real JdbcTransactionManager,
READ_COMMITTED caller commit/rollback rejection without Session mutation, normal
Session-owned operations and durable direct replay/audit with successor denial.
Existing direct replay and concurrent refresh methods remain PASS. Domain,
Migration, Grant/Realm, Provisioning, Crypto, Outbox and Activation inventories PASS.

Report-policy116 controls/job PASS. Actual checkout and privacy/credential/owned
Testcontainers cleanup projections PASS. Sanitized ZIPs3/3 SHA-256 match GitHub
metadata; entries only summary.json/tests.xml. No raw diagnostics or credentials
in public artifacts/SDD. Original Session run39932569608/sourcecaa150fb remains
historical; run39986298097 verifies only security sourcec851f5dc, not the later
published docs-only closure revision. No CI rerun/dispatch, PR creation or merge.

Specific ambient-transaction replay-durability finding behind
T_002_05_SESSION_PR_SECURITY_BLOCKED now has passing PG evidence:
T_002_05_SESSION_TRANSACTION_OWNERSHIP_CI_VERIFIED. Ready for final PR-readiness
reassessment ONLY; neither final PR integration nor all Session security is accepted.

Keep Maven bootstrap root cause OPEN / NOT REPRODUCED. Python migration/outbox
source suites remain outside current CI; Windows outbox source9/10 methods PASS
with2 V3/V4 CRLF raw-byte subcase failures remains documented. Canonical V1-V5 Git
blobs/pins and accepted contracts unchanged; no SQL/hash patch or source-policy
relaxation. Protected main traceability and Foundation/Activation remain untouched.

T-002-05 PARTIAL/unchecked; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Current-session/activity, recovery,
resend, lifecycle/query, remaining authority/security, HTTP/JWT/cookie/CSRF/throttling
and T-002-08 delivery/provider integration remain pending. Feature001/D1-08/Day1
PARTIAL. No full Production Identity or business-feature readiness claim.


## Current-session and meaningful activity Application - 2026-10-24

[Ownership, inward admission contract, exact methods and local results](../implementation/t-002-05-current-session-activity-evidence.md).
Baseline PR24 merge f935c8e9fdfac5dd0fd97e1a5d2a07fa2e238ede; isolated
feature/002-identity-current-session-activity. REQ-002-02/03/05 and AC-002-02/03/04/05
receive supporting implementation/test coverage, not acceptance.
getIdentityMe returns approved Account without idle writes. recordMeaningfulActivity
requires distinct trusted server admission and updates only Domain-approved session
idle/last-activity fields. No client identity/activity flags, JWT/HTTP adapter, raw
audit feed, pagination endpoint, secrets, migration or business-role scope added.
Existing guarded owned transaction, authoritative account/authVersion/Realm and
session deadlines/strict replay remain; no shared Grant propagation change.

Actual local Java21/Maven3.10.0 offline verify PASS:375 PASS/17 expected skips;
Current Session unit21, prior Session unit24, Architecture23, Domain48 PASS;
strict H2 report guard PASS. Python report-policy119/119; migration source15/15.
Outbox source9/10 methods PASS with2 unchanged historical V3/V4 CRLF subcase
failures. New14 PG cases compile, runtime UNVERIFIED: Docker CLI exists but
desktop-linux Engine endpoint unavailable. No Docker/service mutation or CI run.
142 combined PG methods are mandatory inventory, not execution evidence.
Original128 PG methods, five ownership regressions and replay/concurrency cases
are preserved. Existing CI runs do not verify this uncommitted current revision.

T_002_05_CURRENT_SESSION_ACTIVITY_IMPLEMENTED_VERIFICATION_PENDING.
T-002-05 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. Current-source PG/CI plus actual
T-002-06 production access/meaningful-action adapter integration remain open;
recovery/resend/lifecycle/query/authority and T-002-08 delivery/provider work stay
out of this slice. Foundation/Day1 PARTIAL. Protected main traceability, six original
Foundation paths, old Session/Activation worktrees and accepted SQL/contracts intact.


## Current Session/Activity verified source CI closure - 2026-10-24

Owner: OWNER_CURRENT_SESSION_ACTIVITY_CI_CLOSURE: CONDITIONAL_GO.
**T_002_05_CURRENT_SESSION_ACTIVITY_CI_VERIFIED**.
[Exact provenance, methods, artifacts, security evidence and limitations](../implementation/t-002-05-current-session-activity-evidence.md#verified-current-sessionactivity-ci-checkpoint---2026-10-24).
This supersedes ONLY the preceding implementation checkpoint's PG/CI-pending state;
its historical local results and findings remain intact.

REQ-002-02/03/05 and AC-002-02/03/04/05 receive bounded Application/Persistence runtime
verification coverage, not acceptance. [Backend verification run 40062627539](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40062627539),
attempt 1, workflow_dispatch, feature/002-identity-current-session-activity, completed
SUCCESS. All three jobs actually checkout source SHA
231ce63fa6ca5d12aae57e8ad2069d95ada59da9.

Architecture 23 PASS/0 skip; H2: 375 PASS/17 expected skips; PostgreSQL 18: 567 PASS/5
expected skips; all 0 FAIL/ERROR. 119 report-policy controls per job PASS. Exact
class/method/outcome inventory and expected skips PASS; the five Redis/RabbitMQ
skips do not establish remote infrastructure PASS. Current Session unit 21/21 PASS
in both full jobs; new PG 14/14, previous PG 128/128 and combined 142/142 PASS.
These subsets overlap full job totals and must not be double-counted. Prior
Session 24 unit/23 PG, ownership/replay/concurrency, Activation, Provisioning/Outbox,
Realm/Grant, Domain and Migration inventories remain present and PASS.

Real PostgreSQL cases verify authoritative owner/account/authVersion/Realm checks,
read-only /me, allowed activity fields, exact expiry/revocation denial, absolute
cap, rollback, ambient-transaction rejection and concurrent revocation/Realm/idle
monotonicity. Unit tests also exercise post-lock access expiry and distinct trusted
meaningful-action admission; no client identity/activity flag or Demo fallback.
Access 15m/absolute 8h/idle 30m, durable refresh-replay revocation and owned transaction
guards are retained; shared Grant propagation is unchanged. Synthetic admission
fixtures are not production JWT/HTTP/Spring Security verification.

Three sanitized artifacts match authoritative GitHub SHA-256 metadata; CRC and
summary.json/tests.xml-only contents PASS. Credential/privacy guards, Testcontainers
owned-resource audit and disposable PostgreSQL cleanup PASS. Raw logs were handled
in memory; no raw diagnostics/credentials/ZIPs/private evidence are tracked here.
This later two-document closure SHA is separate from the tested source SHA above;
the existing CI does not test the docs-only revision. No rerun/dispatch/PR/merge.

Keep historical Maven bootstrap attribution OPEN / NOT REPRODUCED. Python migration/
outbox source suites remain outside current workflow evidence; Windows outbox 9/10
methods with two V3/V4 CRLF subcases is unchanged, not relabelled PASS. Accepted
V1-V5 Git blobs/contracts, original six Foundation fingerprints, protected main
traceability and previous Activation/Session worktrees are unchanged.

T-002-05 PARTIAL; Feature 002/AC-002-01..05 NOT ACCEPTED;
GRANT_MUTATION_FUNCTIONALITY_LIMITED retained. T-002-06 trusted admission and
HTTP/JWT/cookie/CSRF/throttling adapters, Recovery/Resend, remaining lifecycle/query/
authority and canonical T-002-08 delivery/provider integration remain open. Full
Production Identity end-to-end readiness is not claimed. Feature 001/D1-08/Day 1
PARTIAL. Ready only for a bounded PR-readiness review. Business modules require
BUSINESS_FEATURE_IMPLEMENTATION_REVIEW_REQUIRED and explicit owner approval.
