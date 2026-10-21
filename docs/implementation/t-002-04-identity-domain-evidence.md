# T-002-04 — Production Identity Domain implementation

Checkpoint: 2026-10-21. Authority: OWNER_T_002_04_DOMAIN_IMPLEMENTATION: GO.
**T_002_04_IMPLEMENTED_VERIFICATION_PENDING**. Domain source and48 JUnit cases
are implemented; Java compilation/JUnit/ArchUnit/backend runtime are UNVERIFIED.
No Domain DONE checkbox or full Feature002/AC acceptance is inferred from
Python policy controls or the previous migration CI.

## SDD authority and DoR

Read [AGENTS](../../AGENTS.md), [SDD workflow](../README-SDD.md), [PRD](../prd.md),
[principles](../principles.md), [DoR](../codex/definition-of-ready.md),
[feature DoD](../quality/test-strategy.md), [dependency order](dependency-order.md),
[spec](../../specs/002-identity-access/spec.md), [design](../../specs/002-identity-access/design.md),
[tasks](../../specs/002-identity-access/tasks.md),
[ADR-0009 / DEC-03/04 and O1–O7](../architecture/decisions/ADR-0009-production-identity-lifecycle.md),
[accepted OpenAPI](../../contracts/openapi/identity-access.yaml),
[contract semantics](../../contracts/identity-access/README.md),
[V3](../../app/backend/src/main/resources/db/migration/V3__production_identity.sql),
[T-002-03 acceptance/handoff](t-002-03-identity-migration-evidence.md#final-migration-acceptance--2026-10-21),
[code boundaries](../architecture/code-architecture.md),
[security](../architecture/security.md), [ownership](../architecture/data-ownership.md),
[module catalog](../architecture/module-catalog.md), [transactions](../architecture/transactions-events.md).
No original DOCX is newly available; no invented report quotation or new REQ/AC.

| Ready gate | Result / scope |
|---|---|
| Canonical dependency T-002-03 | DONE, accepted migration packet; not Domain execution evidence |
| Accepted account/auth policy and contract vocabulary | PASS for bounded Domain slice; DEC-03/04/O1–O7 not reopened |
| Internal ownership, inputs, fixtures and test boundary | Defined below; pure Java with deterministic time, no external service calls |
| Unresolved deployment/mail/mobile/provider decisions | Outside this Domain slice; no authority fallback or unapproved provider implemented |
| Canonical T-002-04 DoD | Unchanged: "Unit test happy path, negative, bound và transition xanh." Pending actual JUnit execution |

## Git checkpoint and bounded scope

Branch feature/002-identity-migration-ci; HEAD
bf80d84a459e2b6c8f8431c47dcbbe9f0e84c817; index empty. Preflight records453
public/working files and9 preexisting dirty paths. Six Foundation paths and the
T-002-03 acceptance evidence are protected byte-for-byte. Existing tasks and
supplemental trace receive only authorized progress updates. No branch switch,
worktree, stage, commit, push, PR modification or merge. PR20 remains a migration
publication; this uncommitted Domain source is not tested by its previous run.
Main traceability.md, existing SQL/contracts/ADR, POM, workflow, Demo code and
Foundation source are unchanged. No credential access, ACL/global environment
change, Docker/developer-service mutation or new dependency.

## Domain implementation and invariants

Production root: app/backend/src/main/java/com/elogistic/backend/identity/internal/domain/.

| Files / responsibility | Implemented behavior | REQ trace |
|---|---|---|
| DomainViolation, DomainValues | Fixed internal reasons without submitted values/causes; Unicode and defensive-collection guards | REQ-002-01..05, NFR-S01/S02/S04 |
| Username, PasswordPolicy | Exact case/Unicode username1..80 codepoints with V3/ECMAScript nonblank set; new password15 codepoints/72 UTF-8 bytes, legacy login1 codepoint/72 bytes; malformed surrogate rejection; no transformation/hash/storage | REQ-002-01/03 |
| RoleCode, ResourceScope, Grant | Open exact1..80-codepoint role codes from contract; five approved scope types; exact role+scope pair, immutable values; no invented operational permission catalog | REQ-002-01/02/04 |
| AccountVersion, IdentityAccount | Positive versions, stale/overflow rejection; pending/active/disabled and preserved activation history; explicit activation/reset account+consumed-proof result; disable/reactivate/grant/reset invalidate prior authVersion | REQ-002-01/03/05 |
| ManagementAuthority | Trusted actor/authVersion/target/version binding, full current/requested grant union, explicit assignability, no self-management, missing authority denies; empty target still requires bound authority | REQ-002-01/02/04 |
| IdentityAccount admin/grant guards | Exact role/scope access; no implicit Finance right; protect last active Identity Management admin from loss using supplied authoritative population; reject unavailable/inconsistent counts | REQ-002-01/02/04/05 |
| IdentitySession |8h absolute/30min meaningful idle,15min access cap bounded by both deadlines; at-expiry denied, current account/authVersion recheck, revocation, no idle extension for refresh/CSRF/health/me/background polling | REQ-002-02/05 |
| ActionProof | Explicit invitation/recovery factories;24h/15min bounds, recovery requires active verified account and trusted stored-email match; subject/purpose binding, one-time consumption/revocation, exact-expiry denial | REQ-002-03/05 |
| RefreshFamily | Server-resolved token/member IDs only; rotation consumes prior member, replay returns family+session revocation, revoked family rejects; unknown token cannot revoke a legitimate family; no grace, idle extension or raw token storage | REQ-002-03/05 |

Activation.VERIFIED represents V3's coupled verified-email/activated-credential
state, not a second credential store. Aggregate construction supports trusted
rehydration, not a public account-provisioning endpoint. Password hashing and
persistence of its result remain application/infrastructure obligations.
ManagementAuthority and population/email-match facts MUST come from current
backend state/public owner contracts, never client claims. RoleCode validates
contract shape; supported-role/resource assignability still requires the trusted
authority provider. Domain does not query or own merchant/warehouse/finance data.

## Domain unit tests — written, execution UNVERIFIED

Test root: app/backend/src/test/java/com/elogistic/backend/identity/internal/domain/.
DomainFixtures is synthetic test-only state, not a production repository or
provider. Five plain Jupiter suites contain these exact finite inventories:

| Suite | Methods written | Key fixtures | Actual Java result |
|---|---|---|---|
| IdentityValueTests |9 | Case/Unicode/whitespace, UTF-8/codepoint bounds, malformed input, versions, immutability/redaction | UNVERIFIED |
| IdentityAccountTests |10 | Provision, activation, disable/reactivate, reset, recovery eligibility, state/version negatives, old-session rejection | UNVERIFIED |
| IdentityAuthorizationTests |10 | Multi-scope union/removal, assignability, binding/default deny, self/last-admin, empty/duplicate grants, exact access | UNVERIFIED |
| IdentitySessionTests |9 | Absolute/idle/access before-at-after boundaries, polling, revocation, current account/version, invalid restored clocks | UNVERIFIED |
| IdentityProofAndRefreshTests |10 | Purpose/subject/TTL/consumption/revocation, rotation/replay/rejection, family binding, unknown-token/expiry cases | UNVERIFIED |

All48 exact method identities are listed in IDENTITY_DOMAIN_REQUIRED in the
existing strict report policy. They are required PASS in H2 AND PostgreSQL,
excluded from architecture/foundation-only selection and NOT Testcontainers-only.
All20 previous required class/method lists, category/skip sets and collector
function ASTs remain unchanged. The existing aggregate expectation increases by
exactly48; no old test or expected skip is removed and no redaction/failure guard
is loosened. Five new Python controls cover source inventory/category wiring,
each missing suite/method, nonpass outcomes, unknown invocation and raw failure
publication. Only two policy files are modified under explicit owner permission.

Source-derived FUTURE expectations, not executed Java results:
Architecture23 PASS/0 skips; H2279 PASS/17 expected skips (296 discovered);
PostgreSQL318 PASS/5 expected skips (323 discovered). The last accepted migration
run's231/270 totals do not test these new sources.

## Actual verification and limits

| Check / command | Actual result |
|---|---|
| python -B -m unittest discover -s scripts/ci -p 'test_*.py' -q, first pass |94 tests,2 failures: old aggregate expectations231/270 vs expanded279/318; no Java/test failure inferred |
| Same command after exact aggregate update/removal of duplicate new control |93 PASS,0 failures/errors/skips,24.547s, exit0; all88 original methods retained +5 new controls |
| python -B -m unittest discover -s scripts/ci -p 'test_verify_reports.py' -k IdentityDomainInventoryTests -q after final Java fixture updates |5 PASS,1.214s, exit0; source/inventory and synthetic reports only |
| Exact prior inventory/category comparison and collector AST comparison | PASS;20 old required class lists and all collector functions unchanged |
| UTF-8 / Java import boundary / lexical delimiter scan | PASS on19 Java source/test files; production imports only java.lang/time/util and this Domain package; NOT compilation/ArchUnit |
| Protected fingerprints, documentation links, disclosure and whitespace checks | Final checkpoint results below; no raw evidence publication |
| Java compile /48 JUnit Domain tests / Identity regression / ArchUnit / backend verify | UNVERIFIED / NOT RUN |

No approved current-revision local contained JVM/Maven path is established.
Existing Windows Foundation containment work remains separate and protected;
this task neither bypasses it nor launches an ad-hoc javac/Java/Maven runner.
No Java parser/compiler dependency is installed. Existing hosted Ubuntu Java21 /
Maven Wrapper3.10.0 CI path can verify the exact source after separately
authorized publication. Python controls and lexical checks do not substitute
for required happy/negative/boundary/transition JUnit results.

## Application/infrastructure handoff and acceptance limit

T-002-05 must load current authoritative account/session/authority facts,
serialize last-admin and refresh/proof compare-and-consume operations, and commit
credential/grants/version/session revocation/receipt/audit together. It must
revoke superseded proofs before issuing replacements and deliver only to stored
verified email outside the transaction via approved durable intent. Persist a
REPLAY_REVOKED rotation result before returning a rejection; do not throw away
its required revocation effect. Domain results alone prove none of these effects.
HTTP admission/JWT signatures/CSRF/Origin/cookies/error statuses belong to later
application/security/API tasks, as do replay receipts and resource-owner adapters.
Demo LoginService/TokenService/AccountQueries/AuthenticatedActor stay unchanged
and are not promoted to production multi-role behavior.

T-002-04 remains IMPLEMENTED_VERIFICATION_PENDING with its canonical checkbox
open. T-002-05 Application is the next canonical task, blocked on actual T-002-04
DoD execution. Feature002 and AC-002-01..05 NOT ACCEPTED; Feature001/D1-08/Day1
PARTIAL. T-002-03 acceptance is not reopened or rewritten.

The bounded verification handoff is to publish only the reviewed Domain/policy/
SDD dependency closure under separate owner authorization, then run the existing
required CI path on that exact commit. Require48/48 Domain cases PASS in H2 and
PostgreSQL, the inventories above,93 policy controls per job, real ArchUnit and
existing regressions/privacy/cleanup. Existing three required job names remain
unchanged. No workflow rerun/publication is authorized or performed here.

## Final source/test SHA-256

Production/test filenames below are relative to their respective roots above;
policy paths are repository-relative. These are source fingerprints, not runtime
attestations. Evidence/tasks/trace are not included in this source hash table.

Final integrity checkpoint: exactly20 new files (13 Domain,6 test/fixture,1
evidence) and4 permitted existing-path updates (2 policy, tasks, supplemental
trace).449 other baseline files byte-identical, including all six Foundation
paths and T-002-03 acceptance evidence.45 relative file/anchor links PASS;
canonical task bodies/DoD unchanged, T-002-03 checked and T-002-04/05 unchecked.
UTF-8, new Java whitespace/disclosure/import/delimiter checks PASS; CRLF-aware
git diff --check exit0. HEAD unchanged, index empty. No new runtime or remote CI
result is claimed. These are bounded source/document checks, not Java execution.

| File | SHA-256 |
|---|---|
| main/AccountVersion.java | `742cefbd567293fa3d2509887f96e9273e7660af1fa15614dd4ed31a043d3e50` |
| main/ActionProof.java | `941fc870a6625ff0a3b8f1cfef721d4034e0d918e8b90a9319966ab39ec9c325` |
| main/DomainValues.java | `a5e65d3c2bff300480b5126dce9d6d998160b9c67843fd54cff6d7f20a55c18a` |
| main/DomainViolation.java | `095a387f33c45abdb9c9f192bf6c5c441b893f8a851711b1441d07f9019867c6` |
| main/Grant.java | `b7c959374d025fe9150a795c7653a0a34b75283768722adc806aeae4b92af3d2` |
| main/IdentityAccount.java | `bde60d1000736a75d10fb7e12dbf1bbf7342244b5505af5ce13d48c6dc6a1dc7` |
| main/IdentitySession.java | `14358a2cb3b15eb603a948d921ed53e8b99bbfc08992dad8b6d890ea5bcf35c5` |
| main/ManagementAuthority.java | `cd342476a207b8a26eb0969f121deb4ed3245e5733923a85a8bbbc29608f6811` |
| main/PasswordPolicy.java | `d71eebdf98e86ed89283302f98399e6433ef1202177a853b33f558e139088867` |
| main/RefreshFamily.java | `a5c7eda0e2955ef11bc087462fab241c72dc8aeea2edbff7357ef9a330e76388` |
| main/ResourceScope.java | `a8882495261b0c1a7d24b3956af471b0dffa7801328cbe242bec17677d57301f` |
| main/RoleCode.java | `07e6d39d88876ed97d49bb545ad31037bf81de9b83a50529c73d1031b163615f` |
| main/Username.java | `39534d899d9483f37457fcdca9921cd9d4da373da0a0e919d1c12d80bceb7ba6` |
| test/DomainFixtures.java | `03c3120402b18aa7672af397fe9f4f8785756be6ef83d03a2de538af79dd4f51` |
| test/IdentityAccountTests.java | `0ac518f9905ab932a322384986b5653b4857ed5e4ebcf307388ab6386ccba4cc` |
| test/IdentityAuthorizationTests.java | `05b35233a9a1f453ffdeaeec608f8cc2c71d03bbae611b8d0b96af838a39bd16` |
| test/IdentityProofAndRefreshTests.java | `cf828ac500115281ce2c5010529a29e128cdd134e46c47c3d446633f4bf887fa` |
| test/IdentitySessionTests.java | `4058e60b0b35356e75e27eed8ee5855a21b3a13ca4c8c1de92ca024bcd866fe4` |
| test/IdentityValueTests.java | `44829705072f60127019a1badf0c8995e0e1c6f0c88b598c42326c775ebd95fb` |
| scripts/ci/test_verify_reports.py | `6fab1b0b1b2e1e5fa26e6aa2b7b091d5ce63745af4782c8bb1f416947b4d19cb` |
| scripts/ci/verify-reports.py | `73143b26574ec68f9312649b3ce4aecad5b33a9ce79eb8f635f4a811c0b7a3f2` |

## T-002-04-CI publication preflight — 2026-10-21

OWNER_T_002_04_CI_VERIFICATION_APPROVAL: CONDITIONAL_GO permits one scoped
Domain branch publication and at most one existing-workflow dispatch, not a PR
or merge. Fresh Python command
`python -B -m unittest discover -s scripts/ci -p 'test_*.py' -q`:
93 PASS,0 failures/errors/skips,25.373s, exit0. This is policy evidence only.
All21 source/test/policy hashes above still match;13 Domain classes and48 finite
Jupiter method identities are unchanged from the reviewed implementation packet.

Publication manifest has exactly25 paths: the13 production Java files and6
test/fixture files listed above, scripts/ci/verify-reports.py and
scripts/ci/test_verify_reports.py, this evidence, T-002-03 migration acceptance
evidence, specs/002-identity-access/tasks.md and supplemental Feature002 trace.
The earlier uncommitted T-002-03 completion record is included explicitly as SDD
dependency evidence; its content is not rewritten. Six Foundation dirty paths,
including protected main traceability.md, and all private artifacts are excluded.

Parent is bf80d84a459e2b6c8f8431c47dcbbe9f0e84c817; target branch is
feature/002-identity-domain-ci, verified absent locally/remotely before mutation.
Origin/account/repository match; authenticated phamquoainam has push permission.
PR20 is OPEN on the unchanged Migration branch/head. Read-only Domain effective
rules are empty; classic protections target main/develop, not this new branch;
active Protect develop ruleset is not changed. No protection bypass is proposed.

Existing workflow is Backend verification, ID383888734, with unchanged matrix
checks Architecture gate / Backend default / H2 / Backend PostgreSQL18 and
sanitized reporting, isolated database and owned-cleanup guards. It supports
workflow_dispatch on the proposed source. Default branch main lacks this file;
no UI-dispatch/default-branch change is assumed. The workflow is registered and
has already run (accepted run39575568268). Current
[GitHub workflow_dispatch documentation](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#workflow_dispatch)
states that after a workflow has run once it can be dispatched against any
branch/tag through the API/CLI. Use that native registered-ID path only, after
verifying the new remote branch's exact committed SHA. If dispatch is rejected,
STOP without PR fallback or a second dispatch. No dispatch outcome, Java test
result, publication success or new task acceptance is claimed by this preflight.
