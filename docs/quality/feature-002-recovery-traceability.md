# Feature 002 recovery — supplemental traceability

Latest checkpoint: **T-002-03 DONE** on run39575568268 attempt1, using verified
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
