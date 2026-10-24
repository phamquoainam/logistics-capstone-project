# T-002-05 remaining Application delivery

Current checkpoint: **seven Application operations CI_VERIFIED; T-002-05 PARTIAL**.
See the verified source CI closure below; preceding pending states are historical.

Owner: OWNER_T_002_05_REMAINING_APPLICATION: CONDITIONAL_GO. Date: 2026-10-24.
Baseline: 899cf8f211c1a8b52441e01849bcfefbdbee6162 (PR25 merged).
Status: IMPLEMENTATION_IN_PROGRESS; T-002-05 PARTIAL, Feature002/AC-002-01..05 NOT ACCEPTED.

## Initial integration gate

[Develop push run40069216714](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40069216714),
attempt1, actual checkout899cf8f211c1a8b52441e01849bcfefbdbee6162 in all jobs:
Architecture23/0 skips; H2375/17 expected skips; PostgreSQL567/5 expected skips;
all zero FAIL/ERROR.119 report controls/job, exact reports, artifact digest/CRC,
privacy and owned cleanup PASS. Raw logs processed in memory only.

## Canonical dependency matrix and bounded plan

[Task ownership](../../specs/002-identity-access/tasks.md),
[accepted semantics](../../contracts/identity-access/README.md),
[ADR0009](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
[ADR0010](../architecture/decisions/ADR-0010-identity-system-realm.md),
[ADR0011](../architecture/decisions/ADR-0011-identity-invitation-outbox.md).
Original REQ/AC are unchanged. REQ-002-01/02/04/05 and AC-002-02/03/04/05 receive
supporting coverage only, never whole-AC acceptance.

| Capability | Ownership/support | Security/preconditions | Delivery classification |
| --- | --- | --- | --- |
| listIdentityAccounts/getIdentityAccount | T-002-05; V3 accounts/grants, accepted AccountPage cursor/limit and Account | authoritative actor/session/access facts, entire-target same-Realm authority before contacts/pagination, no global counts | Planned independent Application scope |
| listIdentityRoles/listIdentityScopes | T-002-05; registered ACTIVE SYSTEM Realm and dedicated approved Identity Management capability | only current authorized Identity pair; no external owner assumptions or new grant assignability | Planned bounded catalog projection |
| disableIdentityAccount/reactivateIdentityAccount | T-002-05; existing Domain transitions and V3 DML/audit/receipts | full-target authority, self/last-admin/version guards, owned transactions, atomic revocation/replay | Planned independent Application scope |
| resendIdentityInvitation | T-002-05; existing ActionProof/SecureInvitationIssuer/V5 outbox | same full-target authority; pending/unverified only, invalidate old proof/payload, atomic protected replacement/version/audit/receipt | Planned Application scope; no sender |
| requestIdentityRecovery/confirmIdentityRecovery | T-002-05; Domain/V3 proof/receipt exist, production recovery issuer/storage does not | V5 purpose=INVITATION and exact24h CHECK cannot store15min RECOVERY; ADR0011 crypto/worker eligibility is invitation-only | BLOCKED affected capability: reviewed forward migration and purpose-separated recovery crypto/storage decision required; no fake/legacy issuer |
| Realm administration/bootstrap | owner-only V4 operations, no accepted new runtime API | no runtime registry mutation/privilege expansion | OUT OF SCOPE |
| broader Grant/external owner authority | public resource-owner contracts unavailable | preserve GRANT_MUTATION_FUNCTIONALITY_LIMITED and existing replacement guards | DEPENDENCY BLOCKED; no new roles/grants |
| HTTP/JWT/CSRF/Origin/throttling | T-002-06; required trusted inward admission only | no transport adapter or rate threshold invented | EXCLUDED ownership |
| sender/provider/scheduler/retention worker | canonical T-002-08 | no external I/O or credentials | EXCLUDED ownership |

Implement one coherent seven-operation Application slice; do not modify V1-V5,
accepted wire contracts, existing Domain, shared Grant transaction propagation or
Session/Activation/Provisioning semantics. Required admission has no default adapter.
Existing same-Realm management covers only the exact current target grant set;
this slice never issues new grants or changes the replacement/provisioning policy.
Catalogs project that bounded capability, not general role assignment authority.

Expected files: new IdentityAdministrationCommands, Administration store/Application/
explicit composition; unit and existing PostgreSQL suite extensions; exact CI
inventory/negative controls; this evidence, design/tasks and supplemental trace.
Protected main traceability and all old worktrees remain untouched.

Verification: unit admission/validation/replay/denial; real PostgreSQL read isolation,
authorized-before-pagination, lifecycle/proof/version/audit/receipt atomicity,
concurrency/rollback, ambient rejection and existing regressions; strict finite
inventory, report privacy and owned cleanup. Source CI, docs closure and PR integration
must be recorded separately. No DONE from compilation/H2 alone.

## Implementation and local verification in progress

Authored five production Java files,36 unit cases and34 PostgreSQL cases in the
existing IdentityGrantApplicationTests (previous142 method bodies unchanged).
CI inventory is finite176 combined PG; predicted411 H2 and637 PG, with original
17/5 expected skips only.122 report-policy controls include missing/renamed/skipped/
failed/error negatives for every new identity plus diagnostic privacy controls.

Initial focused run: Java compilation and Architecture23 PASS; two new Mockito
restubbing setup errors were corrected using doReturn, with assertions unchanged.
The subsequent pre-final revision passed409 H2/17 skips and122 report controls.
A focused source review then added final deadline checking before return/commit,
accepted error mapping for unavailable listing authority, and two regressions.
Those earlier results do NOT verify this newer36-unit revision. Final verification
must execute again against unchanged fingerprints before publication.

## Source publication checkpoint

Current production/36 unit revision: direct Java21.0.10/Maven3.10.0 offline verify
PASS, exit0; strict H2 report guard PASS:428 discovered/411 PASS/17 exact expected
skips/0 failure/error; Architecture23 and new unit36 PASS.122 report-policy controls
PASS with no skip/identity/lifecycle/privacy relaxation. Static migration15/15 PASS.
No developer credentials, databases, services or local Docker resources were used.

The34 new PostgreSQL tests compile, runtime is UNVERIFIED until hosted execution.
Two new fixture assumptions were corrected after reviewing the existing bootstrap
helper: cursor traversal includes its authorized administrator; the last-admin
race first retires that third fixture administrator using the real lifecycle.
All original142 method bodies remain unchanged. Current production/unit bytes
match the H2-tested revision; final PG fixture source compiled separately, exit0.
Expected hosted totals: Architecture23; H2411/17 skips; PG637/5 skips; combined
IdentityGrantApplicationTests176 methods. These PG totals remain predictions.

No V1-V5/accepted contract/Domain/shared Grant propagation changes. Existing
Login/Refresh/Logout, Current Session/Activity, Activation, Provisioning and Outbox
implementations remain unchanged. Historical Maven bootstrap attribution remains
OPEN/NOT REPRODUCED. Python migration/outbox source suites remain outside current
workflow execution evidence; Windows outbox raw-byte source check remains9/10
methods with two historical V3/V4 CRLF subcases. No SQL/pin/security guard changed.
Source publication/CI and later docs closure/PR integration are separate revisions.
Recovery and full Production Identity acceptance remain blocked/pending as above.

## First source CI and bounded fixture correction

[Run40071846327](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40071846327),
attempt1/workflow_dispatch, sourcecbf9bf10d2a8246d2a15c1d338e8eeab2df4c47a:
Architecture23 PASS; H2411 PASS/17 expected skips; PostgreSQL636 PASS/1 ERROR/5 skips.
Exact failing method: IdentityGrantApplicationTests.
adminOppositeDisableConcurrencyKeepsOneActiveAdministrator. All142 previous PG
methods and the other33 new PG cases passed. Sanitized reports hide diagnostic
bodies; no raw SQLState/exception body was obtained or published.

Source attribution: bootstrap fixture uses database CURRENT_TIMESTAMP (run2026-10-24)
but the new test's Application clock was fixed2026-10-21. Retiring that bootstrap
account therefore requested disabled_at before created_at, violating the unchanged
V3 account-clock CHECK. The one authorized correction anchors this test's two
session fixtures/admission/Application clock to one database timestamp obtained
AFTER bootstrap. Bootstrap retirement still uses the real authorized lifecycle;
concurrent opposite-disable and exactly-one-active-admin assertions stay intact.
No production source, migration, permissions, policy, skip or test identity changed.
Corrected PostgreSQL runtime remains UNVERIFIED until a fresh exact-SHA CI passes.

## Verified source CI and Application slice closure - 2026-10-24

Current seven-operation slice: CI_VERIFIED / READY_FOR_PR_REVIEW.
[Backend verification run40072363351](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40072363351), attempt1/workflow_dispatch,
actual checkout **0f49ff8d0b5329437cf783b8f632d4a844ece58b** in ALL three jobs. This corrected source checkpoint
supersedes only the earlier new-source runtime pending/failing state. Initial
run40071846327 and its bounded fixture correction remain recorded above.

| Job | Job ID | Discovered | PASS | FAIL/ERROR | Expected skips | Policy controls |
| --- | --- | ---: | ---: | --- | ---: | ---: |
| Architecture gate | 120808369055 | 23 | 23 | 0/0 | 0 | 122 PASS |
| Backend default / H2 | 120808368897 | 428 | 411 | 0/0 | 17 | 122 PASS |
| Backend PostgreSQL 18 | 120808369092 | 642 | 637 | 0/0 | 5 | 122 PASS |

Exact finite class/method/outcome inventory PASS, no unexpected skips. New36 unit
and34 PostgreSQL cases all PASS. Previous142 PG methods/body identities remain
intact and all PASS; combined IdentityGrantApplicationTests176/176 PASS. These
subsets overlap overall637, never add them again. Existing Domain48/Migration25,
Session24 unit/23 PG, Current Session21 unit/14 PG, Activation, Realm/Grant,
Provisioning/Outbox and architecture/negative-fixture regressions all PASS.
The corrected adminOppositeDisableConcurrencyKeepsOneActiveAdministrator PASS.
H2 skips exactly8 PostgresFoundation+4 PostgresHealthSafety+5 FoundationDependency;
PG skips only5 Redis/Rabbit FoundationDependency cases, not remote infra PASS.

| Canonical operation | Implemented Application/Persistence evidence |
| --- | --- |
| listIdentityAccounts | complete exact-Realm target SQL filter BEFORE cursor/LIMIT; actor/Realm-bound untrusted keyset navigation, no global totals; adminPaginationFiltersUnauthorizedRowsBeforeLimit/adminCursorCannotBeTransferredBetweenActors |
| getIdentityAccount | accepted Account only after full-target authority/current session/access revalidation; absent/unauthorized share404; no activity/idle writes; adminReadFiltersWholeTargetAndDoesNotWriteActivity |
| listIdentityRoles/listIdentityScopes | dedicated current management capability in registered ACTIVE SYSTEM Realm only; no external assignability fabricated; adminCatalogsExposeOnlyCurrentRegisteredIdentityPair/adminInactiveRealmAndMixedActorRemainDefaultDeny |
| disableIdentityAccount/reactivateIdentityAccount | existing Domain self/last-admin/activation/version guards, atomic versions+all-session revocation+proof/outbox invalidation+audit+receipts; disabled-pending cannot reactivate; replay/conflict/operation namespaces, four write-fault rollbacks, ambient rejection and real last-admin concurrency |
| resendIdentityInvitation | pending/unverified only, missing crypto fails closed,24h single-use protected replacement, invalid old proof/payload, version/authVersion/audit/receipt in same owned transaction; real crypto, exact replay/conflict, four write-fault rollbacks and concurrent same-key dedupe |

Mandatory CurrentSessionAdmission is still an inward trust contract, not a JWT
implementation. Real transport/signature/issuer/audience/Origin/CSRF/throttling
and completed-action adapter remain T-002-06. Queries never advance idle; no new
HTTP/Spring registration, caller principal, activity flag or Demo fallback.
Existing Session-owned guard is reused unchanged; no shared Grant propagation,
Login/Refresh/Logout/strict-replay or old Current Session semantics changed.

| Sanitized artifact | ID | GitHub SHA-256 digest (matched ZIP) |
| --- | --- | --- |
| backend-architecture-1 | 12551852374 | 73366da0ddc8f465b71c34e8e38e1f9e2ab20be05462f555c4de521b7f8906d3 |
| backend-h2-1 | 12552081103 | b4ec20053047e8a8bdfdf9ad21098a54af9c129d773a1e951b03810654815274 |
| backend-postgres-1 | 12551349918 | 87c69353c4644be36934c10393a40e32f1bb71e72264f77ae16da897d1bee70b |

All ZIP CRC and allowlisted summary.json/tests.xml contents PASS. XML contains
only approved method/status/count projections and fixed redacted skip text.
Credential/privacy log audit, Testcontainers generated-credential audit, all-owned
resource cleanup audit and disposable PostgreSQL cleanup PASS. No raw Maven log,
SQL diagnostics, password/proof material or private artifact is published. Ignored
sanitized packet is retained under .demo/remaining-application/verified-source-ci/.

Source commits: cbf9bf10d2a8246d2a15c1d338e8eeab2df4c47a (implementation), then
0f49ff8d0b5329437cf783b8f632d4a844ece58b (one bounded test-fixture correction).
The subsequent evidence-closure commit modifies SDD only; source CI does NOT test
that later documentation revision or a future PR integration SHA. PR must obtain
its own three required integration checks; no automatic merge is authorized.

## Remaining dependencies and task acceptance

The coherent seven-operation Application slice has real PostgreSQL evidence;
T-002-05 remains PARTIAL because requestIdentityRecovery/confirmIdentityRecovery
still lack approved purpose-separated15min protected issuance/storage. V5 is
INVITATION-only/exact24h; a separately reviewed forward migration/protection design
is needed. No dummy issuer, plaintext intent, legacy proof fallback or unapproved
crypto generalization was installed. No accepted V1-V5/contract/Domain changes.
External owner authority remains unavailable/default deny; keep
GRANT_MUTATION_FUNCTIONALITY_LIMITED. Remaining HTTP/security/UI and provider/
maintenance integrations retain canonical T-002-06/07/08 ownership; no business-role
implementation or full end-to-end Production Identity readiness is claimed.
Feature002/AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1 remain PARTIAL.
Historical Maven bootstrap attribution and Python/outbox CRLF findings stay OPEN
as recorded; no new result relabels them PASS. Four protected worktrees and six
Foundation fingerprints remain unchanged. Business implementation still requires
BUSINESS_FEATURE_IMPLEMENTATION_REVIEW_REQUIRED and explicit owner approval.
