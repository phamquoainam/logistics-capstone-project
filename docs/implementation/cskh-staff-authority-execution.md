# CSKH-IMPLEMENT-01 — source/test execution evidence

## Current source/test checkpoint - 2026-11-20

**CSKH_STAFF_AUTHORITY_SOURCE_TEST_READY** for the bounded Staff authority
implementation. Claims intake/evidence source/test also verified; commercial
compensation remains BLOCKED. This is not a live activation or full module acceptance.
No demo or Production database/account/grant/binding/feature/runtime was changed.
No credential, certificate trust, Git staging, commit, push or merge was performed.

Original Owner-approved manifest SHA-256:
`acf43f4ed06ac77bf3c405376adc2d7d9e0aa7db2598782b59eef35f4971ebb4`.
The original bytes and WIP remain in verified private archives recorded below.
Current manifest includes the subsequent standing-authorized bootstrap, precise
source/runtime deltas and mandatory fixed report inventory; its SHA-256 is
`c6a784d48e827330fee46e09175c27939238c259680c2f54487ab1ef1ec3409c`.

### Source diff and boundaries

- V36/V37 implement dedicated Staff identity, pending/sealed invitation, real
  protected possession activation, fixed STAFF session/CSRF/audience/admission,
  request-bound canonical grants and revoke-safe durable receipts.
- Exactly three NOLOGIN roles are bootstrapped outside Flyway on owned isolated
  PG18 by the trusted test administrator. Migrator/owner remain NOCREATEROLE;
  runtime/operator cannot SET ROLE writer. No local operator membership is granted.
- Existing Identity session/verification/outbox/SMTP engines are reused. Customer
  self APIs and SYSTEM invitation format1 remain compatible. Personal and
  Organization CSKH use the same dedicated Staff purpose, with their existing
  current canonical grant and assignment policies; no Customer relabelling.
- Workflow consumes public Identity/Orders APIs; Platform owns byte-only private
  storage/scanner, Orders owns Claim intake/evidence/events/receipts. V38 is the
  bounded Owner-authorized intake/evidence amendment; compensation remains BLOCKED.
- Dedicated Staff UI activation/login/desk uses memory-only proofs/session and
  immediately scrubs invitation fragments. Merchant intake UI supports real private
  evidence, current-authority retries, history, reports and committed-event notices.
- Bootstrap helpers change only two pinned prerequisite hooks per original test/CI
  protocol. All seven accepted report collector function ASTs are unchanged. Fixed
  inventory additions enforce the new tests; synthetic reference totals are not
  claimed full-suite results.

### Required test matrix CSKH-T-01 through CSKH-T-10

| Gate | Actual evidence and result |
| --- | --- |
| CSKH-T-01 migration/integrity | DONE: 72/72 migration tests PASS, V35 upgrade through V38, old hashes/data preserved; late V37 injected DDL rollback and normal subsequent migration PASS without repair. |
| CSKH-T-02 direct DML/ACL | DONE: exact role bootstrap, ownership/EXECUTE/SET ROLE denial, unsealed/forged authority and direct-DML negatives PASS. |
| CSKH-T-03 provisioning/verification | DONE: real crypto/SMTP test inbox/activation, resend/quota/cancel/expiry, concurrent consumption and late audit rollback PASS; no real mailbox provisioning. |
| CSKH-T-04 protected delivery/privacy | DONE: format1/2 purpose isolation, outbox lease recovery/exhaustion/cancellation/terminal wipe and SMTP regressions PASS. |
| CSKH-T-05 HTTP/session | DONE: fixed Staff audience, TLS/Origin/CSRF, cookie/refresh/logout, wrong-purpose/Customer/admin denial, bounded body and negative config PASS. |
| CSKH-T-06 grant receipts | DONE: exact replay/lost response, mismatch, concurrency, absent revoke, revoke-safe old receipt, authorized fresh grant and late failure PASS. |
| CSKH-T-07 Organization authority | DONE: current eligible Staff + canonical grant + exact binding/assignment, tenant/facility denial, revocation, Merchant notice preferences/no duplicates, Personal Support regressions PASS. |
| CSKH-T-08 environment | DONE: disabled sentinel/default, physical DB binding/mismatch, denied profiles/missing prerequisites and no automatic local grants PASS. No existing demo mutation. |
| CSKH-T-09 UI/Browser | DONE: Staff real HTTPS/PG/SMTP Browser PASS; existing 19 Customer/Personal/Business/Team/Support Browser cases PASS after updating obsolete Claims navigation assertion; frontend typecheck/lint/build and 13 focused unit tests PASS. |
| CSKH-T-10 architecture/privacy/regression | DONE: affected architecture, security, H2-independent unit and PG regression tests PASS; 187 Python report/bootstrap/validator tests PASS; collector functions preserved. Implementation source validator: zero errors; historical broken links retained. |

DONE refers to the bounded source/test gates, not live deployment, commercial Claim
acceptance or full Spec 021 acceptance. No full PostgreSQL suite/GitHub CI PASS.

### Commands and retained evidence

Runner: `powershell.exe -NoProfile -ExecutionPolicy Bypass -File
scripts/ci/run-cskh-focused.ps1 -Tests <selectors> -Offline`.
It creates a fresh private evidence directory, copies only fresh Surefire results,
records per-source SHA-256 and checks source did not change during the run.

Final affected selector:
`IdentityMigrationTests,IdentityGrantApplicationTests#staff*+claim*+supportTicket*+organizationSupport*+team*+customerEntryBrowserUsesActualHttpsPostgresAndSmtp,Architecture*Tests,*Invitation*Tests,*Evidence*Tests,Production*Tests,SmtpCustomerDeliveryTests,ClaimIntakePolicyTests`.

| Evidence suffix | Observed result |
| --- | --- |
| `ca0eb775bf534a1a909f2e03e066e420` | Resend/quota/concurrent activation: 2/2 PASS |
| `ca95369a708f40498635dbbd09fee521` | Claims Browser/evidence HTTP/V38/config/architecture: 17/17 PASS |
| `9b72471f538b45b7aabc2e5fe0e3ee8a` | Outbox/guard/evidence/S3 controlled HTTP: 9/9 PASS |
| `b82792e3bb3940169b13a36a37cd469d` | DDL rollback and scoped reports/notices/late failure: 2/2 PASS |
| `5c66fad7892d42699e7d67086446b4b0` | Final affected regression: 382 PASS, 1 FAIL / 383, zero skips, source unchanged. All 72 migration cases, Staff Browser and Claims Browser PASS. Customer Browser failed only obsolete Claims-tab lookup. |
| `0c3e80f833d6428e8cfefcc86a7be1fd` | Corrected affected Customer Browser: 1/1 JUnit PASS with all 19 Browser cases PASS, zero skips, source unchanged. |
| `89d066b6d88d4c75b6692642f870025e` | Claims Browser plus final 390px mobile/evidence readiness capture: 1/1 PASS, zero skips, source unchanged. Synthetic image visually inspected: no overlay, loaded CLEAN evidence and no horizontal page overflow. |

The 383-case run snapshot SHA-256 is
`8d3477d7c6a57941ca417c8071172a0432d8030c33a67cfa74bcaf8550db13c9`;
the corrected Customer Browser snapshot is
`aa9f5090af4875e8dafed2fb64d0d9660c1bd68427a3748d0c6d55600b8ff716`.
Only the affected Browser assertion changed between the first two runs. Final
Claims Browser source snapshot: `42cd98f8ebc25984229cdcc52960702347a8d5a31c9ced7fa05f5d70375430d6`;
its only subsequent test delta waits for loaded evidence and dismisses the menu
before the mobile image. Production source
was unchanged; no redundant full-suite rerun is inferred or required.

Frontend commands in `app/frontend/web`: `npm.cmd run typecheck`, `npm.cmd run lint`,
`npm.cmd run build:customer` PASS; build retains the existing large-bundle warning.
`npm.cmd test -- staff-authority.spec.ts claim-intake.spec.ts claim-evidence.spec.ts organization-support.spec.ts`:
13/13 PASS. These unit tests are distinct from real Browser evidence.
`python -m unittest scripts.ci.test_verify_reports scripts.ci.test_staff_role_prerequisites docs.reconciliation.v3.test_validate`:
187/187 PASS. Fixture totals H2=721/PG=1226 are synthetic collector golden-report
counts, NOT executed full backend suite totals.

### Resolved failures retained accurately

Private earlier results remain available: migration 68 PASS/1 FAIL/1 ERROR;
architecture/private-storage 32 PASS/1 FAIL/1 ERROR; later migration/affected run
89 PASS/1 ERROR; intermediate Browser failures and final obsolete-navigation
failure above. Fixed causes: old session-row comparison after SYSTEM audience
addition; expected writable retired legacy operator; audit CHECK selection;
operator schema USAGE; web-to-JDBC architecture dependency; ambiguous SQL quota
columns; new Staff sessions included in SYSTEM snapshot; old fixture counting SYSTEM
grants; direct-DML column/rowtype mistakes; base permission revoke fixture; stale
UI detail while loading; encoded Vietnamese test locator; renamed Claims navigation.
No skip, weakened invariant, trust-all TLS, Flyway repair or false success was used.
Historical Team full PG run 1 FAIL/focused 10/10 PASS remains unchanged; it is not
converted to a full-suite or GitHub CI success by this checkpoint.

Architecture decision: [ADR-0015](../architecture/decisions/ADR-0015-local-staff-and-private-claim-evidence.md).

### Remaining scope and next checkpoint

[Claims execution](organization-claims-intake-execution.md) records the separate V38
source/test slice. Commercial eligibility, deadlines/appeals, liability/SoD,
compensation decisions and payout/refund remain BLOCKED. Actual AWS/ClamAV live
classification/delivery and existing-demo activation are NOT VERIFIED/NOT RUN.
Source/test success does not authorize real provisioning/grants/bindings/activation.
Next action: Owner reviews this bounded evidence before any separate Local/Test
deployment decision; no deployment approval is requested or assumed by this report.

The original preflight report below is retained verbatim as history.

Date: 2026-11-19. Checkpoint: **BLOCKED — PREFLIGHT DEPENDENCY**.

Owner approved source/test only for exact manifest SHA-256:

```text
acf43f4ed06ac77bf3c405376adc2d7d9e0aa7db2598782b59eef35f4971ebb4
```

Approval supersedes the historical `CHANGES_REQUESTED` / approval-pending lines in
the supplied [implementation handoff](cskh-staff-authority-implementation-handoff.md).
The manifest is preserved byte-for-byte; its `[PROPOSED]` metadata is a historical
review artifact, not grounds to ask for the same approval again.

## Artifact receipt and preservation

The named `argopost-cskh-source-test-unblock.zip` is absent from repository root.
Therefore ZIP CRC/archive integrity has **not** been verified. The extracted
`README-unblock.md`, approved manifest and handoff are present. Direct hashing of
the actual manifest bytes matches the exact Owner-approved hash above.

JSON comparison with the original confirms the only semantic delta is
`CSKH-ACL-02.grants`: Identity writer owns SECURITY DEFINER SQL-01..06 and SQL-10;
SQL-09 remains SECURITY INVOKER under the authorized deployment principal.
No replacement or reserialization of the already-correct artifact was necessary.

The original committed Git blob uses LF and hashes to
`8b1d5c8cb1383d76967182c1b4d4af0b5d68661abeb930ae8f88480fd22a4047`.
Its CRLF worktree reconstruction matches the original recorded SHA-256
`370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb`.
Both representations were preserved, explicitly distinguishing reconstruction
from a preexisting original worktree backup.

Before creating this report, all four WIP files were archived and CRC/read-back
SHA-256 verified in an OS temporary directory outside the repository:

```text
C:\Users\HOAINA~1\AppData\Local\Temp\argopost-cskh-preflight-vjcu2azw
```

That directory contains the original LF blob, CRLF reconstruction, WIP ZIP,
Git-status record and per-file fingerprints. The four preserved paths are:

- `docs/implementation/cskh-staff-authority-technical-manifest.json`
- `README-unblock.md`
- `docs/implementation/cskh-staff-authority-implementation-handoff.md`
- `docs/implementation/owner-handoff-cskh-staff-authority-review-ready.md`

No secret, credential, local runtime artifact or database content was read for
this source-preservation step. An OS temporary backup is not a durable replacement
for the existing project checkpoints and is not scheduled for deletion here.

## Preflight results

| Gate | Observed result |
| --- | --- |
| Applicable instructions | Root `AGENTS.md` and Definition of Ready read; no additional Backend/web AGENTS files found in the filename inventory |
| Branch / HEAD | `feature/argopost-business-shipping-tools` / `58f54a2010f8fd5ed911556be7b8ea41d3ba22d7` |
| Local origin/develop ref | `420e948beeffe7d1e596e3a004174b3b4ed3d4c6`; HEAD tree equals this local ref; no new remote fetch claimed |
| Worktrees | One existing root worktree only |
| Approved manifest | PASS: exact bytes/hash and ACL-02-only semantic delta |
| Frozen V34 | PASS: `07ebf9fd74aa31c178ab4e849761c4f3b513fdee7c8ad46269713661de6f0f4c` |
| Frozen V35 | PASS: `33e7e8b532de598627c321a454fd293157fb6c3a365cede946a03c3267d4844b` |
| Approved Organization Support manifest | PASS: `9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940` |
| Migration source stream | 34 files: V1–V32, V34, V35; no V33, V36 or V37 |
| Isolated PostgreSQL execution | BLOCKED: Docker CLI 29.6.2 found through launcher fallback; Linux Engine pipe absent |
| Migration authority compatibility | BLOCKED: ACL-05 requires role creation during migration; accepted Flyway principals are NOCREATEROLE |

Frozen hashes are source evidence only. No live database identity/history or
current role catalog was queried; no live authority or schema PASS is inferred.

## Exact migration-authority delta

Evidence:

1. [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md) fixes the
   owner/migrator/runtime split. Owner and migrator are not superuser or CREATEROLE.
2. [Existing bootstrap](../../infra/postgres/bootstrap.sql) lines 4–7 create the
   owner/migrator/runtime with `NOSUPERUSER ... NOCREATEROLE` and grant only the
   owner membership to migrator.
3. [Existing isolated test support](../../app/backend/src/test/java/com/elogistic/backend/testsupport/PostgresTestcontainersSupport.java)
   uses the same NOCREATEROLE policy and Flyway `SET ROLE <owner>`.
4. The approved manifest `CSKH-ACL-05` explicitly says
   `Create roles/functions/constraints in migration transaction`, while
   ACL-01/02/03 require three new cluster roles. Existing prerequisites create
   none of those roles or the function-ownership transfer authority.

Under those accepted principals, creating the three roles during V36/V37 is not
supported by current migration authority. This is a source-established dependency
conflict, **not** an executed PostgreSQL failure. No attempt was made to run
Flyway as bootstrap/superuser, grant CREATEROLE, or inject privileged helper SQL.

### Minimum resolution proposed, not implemented

Use a separately bounded cluster-bootstrap prerequisite for only the approved
three NOLOGIN roles, together with exact migration ownership-transfer authority
and required physical-identity inspection privilege. Keep runtime/operator unable
to become writer roles; retain NOCREATEROLE/NOSUPERUSER on existing principals.
Test fixtures would establish the same prerequisite through their existing owned
container bootstrap authority, then run Flyway under the ordinary migrator.

V36/V37 would validate those prerequisites and fail closed when absent rather
than silently gain role-management authority. This changes the placement of role
creation stated in ACL-05, so the exact prerequisite/ownership delta must be
resolved before implementing it. It does not require reopening Identity/Support
APIs, tariffs, permission codes or the approved ACL-02 correction.

No future demo bootstrap, role membership or grant is authorized by this proposal.

## Execution slices and test results

First pending slice is handoff **A — Identity DDL**. Its PG18/ACL prerequisite gate
has not passed. No application source or migration has been written, and no slice
is marked DONE. Planned affected files remain V36, isolated migration/security
fixtures and, only after slice A passes, the approved Identity/Orders/workflow/UI
compositions listed in the handoff.

| Required gate | Actual result |
| --- | --- |
| CSKH-T-01 migration/integrity | NOT RUN — no isolated Engine; migration-authority conflict |
| CSKH-T-02 direct DML/ACL | NOT RUN — same prerequisite |
| CSKH-T-03 provisioning/verification | NOT RUN — depends on slice A |
| CSKH-T-04 protected delivery/privacy | NOT RUN — depends on slice A/B |
| CSKH-T-05 Staff HTTP/session/security | NOT RUN — depends on slice A/B |
| CSKH-T-06 grant receipt/concurrency | NOT RUN — depends on slice A/C |
| CSKH-T-07 Organization isolation/authority | NOT RUN — depends on approved implementations |
| CSKH-T-08 environment/negative deployment | NOT RUN — isolated execution unavailable |
| CSKH-T-09 Frontend/HTTP Browser | NOT RUN — no implementation available |
| CSKH-T-10 architecture/regression/report policy | NOT RUN — no application source change; no old-suite rerun |

Read-only/static checks actually executed: Git status/HEAD/tree comparison,
worktree inventory, SHA-256 verification, JSON semantic comparison, source migration
inventory, source authority inspection and Docker `version`.
Preservation execution wrote/read only an isolated temporary source archive.
These checks are not substitutes for CSKH-T-01–10.

Documentation verification actually executed:

- `git diff --check`: exit 0, with a CRLF notice only.
- Execution-report local Markdown links: PASS.
- Recheck of four preserved incoming WIP file fingerprints: 4/4 unchanged.
- `python -B docs/reconciliation/v3/validate.py --implementation`: **exit 1**,
  semantic ownership status ERROR. Exact diagnostics are
  `corrupt UTF-8 authored Markdown docs/implementation/cskh-staff-authority-technical-review.md`
  and `undeclared implementation source README-unblock.md`.
  The former is an unchanged historical tracked review; the latter is the
  preserved incoming package README at repository root. Neither file was edited
  in this execution. The validator also retains its ten classified preexisting
  broken links. No validator rule or source manifest was weakened to report PASS.

## Boundary and next checkpoint

- Application source/migrations changed: **no**.
- Demo database queried/migrated or reset: **no**.
- Staff provisioned / canonical grants / Organization bindings: **none**.
- Runtime restart / feature activation / credential or certificate change: **no**.
- Commit / push / merge: **no**.
- `CSKH_STAFF_AUTHORITY_SOURCE_TEST_READY`: **not achieved**.

Next: resolve the exact ACL-05/bootstrap prerequisite delta and make the isolated
Docker Engine available, then resume slice A under the already-approved source/test
scope. No duplicate approval for the existing manifest's API/DTO/business scope is
requested.

## Subsequent Docker readiness verification — 2026-11-19

Owner reported starting Docker Desktop. Read-only Docker `version` and `info`
both exited 0: Client/Server 29.6.2, Linux, amd64/x86_64. The Engine-availability
blocker above is resolved; those earlier failed queries remain historical evidence.
No container was started, stopped, recreated or modified by this verification.

The ACL-05/bootstrap authority dependency is still unresolved. No new approval
of that delta is implied by Docker availability. Slice A and CSKH-T-01–10 remain
unexecuted; SOURCE_TEST_READY is not claimed. The two recorded documentation
validator findings also remain open.

## Current WIP preservation

Source/document WIP checkpoint (private, ignored): `.demo/cskh-source-checkpoint-0f7f5bc24216452aad58cedbe9769eb7`.
Original incoming archives remain untouched. ZIP CRC and every source SHA-256 were
verified by read-back; deleted tracked paths are recorded, not discarded.
No credentials, runtime evidence, build output or database backup is in this source ZIP.

## Owner-authorized Git integration checkpoint - 2026-11-20

The Owner subsequently requested commit/push and merge into develop. This overrides
only the earlier Git publication restriction. Demo migration, actual provisioning,
grants/bindings, feature activation and Production remain outside this checkpoint.
Remote preflight: `origin/develop` is `420e948beeffe7d1e596e3a004174b3b4ed3d4c6`
(PR #49 merge); its tree equals the current committed branch baseline `58f54a2`.
Current source/doc WIP is the bounded Staff + Claim intake/evidence implementation
above. It will be integrated through a PR and current revision CI, without force
push, squash/rebase, branch deletion or branch-protection bypass. No CI outcome or
merge is claimed in this pre-publication record.

### PR #50 fixed-inventory CI correction

First PR integration run `44748213433`, head `223d1508aa8c7085ac0d8d2fc9befa82bde650b1`,
synthetic merge `318e6d506794cba0b865ca17fe5369169282f2e0`:
Architecture PASS; H2 collector FAIL with 718 recorded passed cases, zero test
failures/errors, 17 allowed skips. The sixth Customer configuration case
`staffUsesFixedAudienceAndRejectsCustomerAndSystemTokens` was omitted from the
finite required inventory, causing the report parser to reject that suite.
This is an inventory defect, not an H2 application assertion failure.

TDD: two new report-policy regressions failed before the correction; both plus
the exact reference-report totals test PASS after appending the omitted case.
No collector/privacy function changed, no test was skipped or weakened. Golden
fixture totals become H2=722/PG=1227; these remain synthetic policy-fixture counts,
not observed full backend execution totals. Full current-revision CI is pending.

### Exact-head PostgreSQL CI findings after inventory correction

Run `44750213467`, head `fd58c11011385ed50f6fa6eeef22a7f073ca6376`:
Architecture PASS; H2 actual 722 PASS/17 allowed skips, no failure/error.
PostgreSQL job was cancelled by the 30-minute execution limit. Its retained
sanitized report records 1187 PASS, 5 FAIL, 35 ERROR and 5 allowed skips; this is
not a PostgreSQL PASS and PR #50 is not merged.

Two Foundation suites still expected the V35 stream/131 tables. Reproduction
`e47adf6e57434add81a10737a5302814` failed both targeted assertions. Corrected exact
stream V1-V32/V34-V38 (37 migrations) and 140 owner-owned tables, explicitly adding
all nine Staff/Claim tables while retaining the old object/FK/privilege checks.
The complete Testcontainers foundation suite then PASS 7/7 in
`f2f9ce9c865e42809a5599cab11303d8`. No migration or runtime invariant was changed.

Four representative CI error methods PASS 4/4 when run on isolated PG18 in
`9fa82033d9b44a04907e44a577aeb799`. This does not establish the cause of the other
CI errors or turn the full run into PASS. Existing artifacts deliberately omit
private stacks. A separate bounded diagnostic helper now classifies known error
kinds from private Surefire XML in memory and emits only fixed enums plus exact
already-approved test identifiers. Unknown messages/identifiers, stack contents,
properties and credentials are never emitted. Four synthetic privacy/bounds tests
PASS. The helper is diagnostic only, cannot change gate outcome, and leaves the
accepted collector, private-log policy and runtime ownership protocol unchanged.
The existing 30-minute limit is unchanged until there is causal evidence.

### Confirmed runner storage exhaustion and bounded fixture release

Run `44756480373`, head `3fb22952384e7920e57e9bf94b3c7745c591a42c`, completed
normally in about 18 minutes: 1191 PASS, 1 FAIL, 35 ERROR, 5 allowed skips. All
Foundation assertions and all 19 Customer Browser journeys PASS. The safe classifier
identifies all 35 errors as DISK_FULL, zero unknown/unavailable diagnostics. The
single assertion is Draft isolation at the boundary before the 35 late fixture
errors; it PASS unchanged when reproduced separately below.

The fixture factory creates a new database per Identity test but previously kept
all 408 until container shutdown. V36-V38 increased the per-database footprint.
Add a separate test-only release helper and AfterEach hook: reuse the complete
accepted owned container/volume/run/session proof and current loopback mapping;
require exact task_c/owner/migrator/runtime UUID suffix bindings and current catalog
owner; refuse live connections or changed ownership. DROP DATABASE is non-FORCE;
no session termination, role/container/volume deletion, developer URL or demo access.
The original factory/ownership/privacy protocol is unchanged. No migration/runtime
SQL, assertions, test coverage, timeout or fsync policy was weakened.

Real isolated PG18 checks in `06f3f673e1a8479eb5c0d6763785976c`: 3/3 PASS, including
foreign/invalid/mismatched target denial, neighboring data preservation, replay,
live-connection/changed-owner denial and the unchanged Draft isolation test.
Two cleanup negative/positive cases become mandatory finite CI inventory; synthetic
PG reference total 1229, H2 722. Full exact-head CI still required before merge.
Owner machine free space was read-only verified at about 0.52 GiB on C; no full
local stress run or filesystem/Docker-volume cleanup was attempted.

### Follow-up cleanup connection-close race

Run `44761278906`, head `e02388af1847bfec5d5bdd2dcc08565ab707000d`: actual
1222 PASS, zero assertion failures, 7 errors, 5 allowed skips; no DISK_FULL remains.
The seven session/concurrency cases PASS 7/7 in isolated reproduction
`999fbf9c0e43439baf855ab318b93753`. They are not claimed resolved from that alone.

A controlled owner-closes-connection regression then reproduces premature cleanup
rejection: `b37ee5e9d57d42858542f8ad18e991c4`, 1 ERROR, exact safe root marker
TEST_DATABASE_STILL_CONNECTED. Cleanup now waits at most three seconds for the
connection owner to close normally, then rechecks catalog ownership. A persistent
connection still denies deletion; there is no FORCE, signal, session termination,
runtime privilege or test assertion relaxation. The new regression additionally
asserts cleanup waited for the scheduled owner closure rather than killing it.
See [PostgreSQL DROP DATABASE semantics](https://www.postgresql.org/docs/18/sql-dropdatabase.html).

`3c95994a10764650ab4fb16b9ed4d8a8`: 10/10 real PG18 cases PASS, comprising all
seven CI-error methods and all three release safety/replay/wait cases. Mandatory
inventory adds the third release case (synthetic PG total 1230, H2 unchanged 722).
Finite safe diagnostic codes now distinguish cleanup-in-use/owner/target failures;
raw messages and credentials remain private. Full exact-head CI is still required
before merge; no full-suite PASS is inferred from focused reproduction.
