# T-002-05 remaining Application delivery

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
