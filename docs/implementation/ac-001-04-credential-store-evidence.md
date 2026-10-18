# AC-001-04 protected credential store — Phase 1 evidence

Latest checkpoint2026-10-19: **REVIEW_PASS_READY_FOR_PR_PREPARATION** for the
approved Phase 1 contract, after final independent review and Linux fixture repair.
Real migration remains BLOCKED; AC-001-04 remains NOT ACCEPTED.

## Final review and publication regression — 2026-10-19

The independent source/native-API/test/log review resolved PCS-01, PCS-02,
PCS-03, PCS-04 and PCS-RR-01 for Phase 1. No remaining confirmed Phase 1 PR
security blocker was found. This does not accept the full security requirement.

| Finding | Resolved Phase 1 protection | Remaining boundary |
| --- | --- | --- |
| PCS-01 | Latest revision, stored receipt, target/backup identity and digest; rollback creates a new revision | Trusted same-account/admin boundary remains |
| PCS-02 | Public control/credential names and Windows aliases rejected before mutation | Internal callers remain trusted |
| PCS-03 | Independent inspection/owner/namespace/collision/eligibility/action; uncertainty blocks | Actual legacy namespaces unsafe, root OWNER_ACTION_REQUIRED |
| PCS-04 | Flushed/read-back intent before replace, immutable events, replay barrier, exact explicit accept/abort | Ambiguous/corrupt evidence blocks; no automatic restore or power-loss proof |
| PCS-RR-01 | PRESENT/ABSENT/UNKNOWN; narrow not-found proof; UNKNOWN blocks native replace or successful resolution | Snapshot only, not full TOCTOU freedom |

The first publication attempt stopped before staging: Ubuntu reported58 PASS,
1 FAIL and13 expected Windows-only skips. The failing existing-object namespace
test expected UNSAFE but observed SAFE_EXISTING_ANCESTORS. Its POSIX owner
injection intercepted _presence_stat only; real pin used direct Path.lstat and
observed the safe filesystem owner. This was a fixture mismatch, not evidence
of a corresponding Windows production-policy failure. The original failure is
retained; no assertion was weakened or test disabled.

The approved correction changes only test_credential_store.py among source/test
files: a context-local Path subclass routes pin's lstat through the same synthetic
owner snapshot for the selected paths/backend. It still executes the original
pin and namespace_assessment logic, patches no global Path behavior and changes
no actual owner/ACL. Existing/missing unsafe paths, the actual owner-rejection
reason, unaffected safe paths and restoration after the context are asserted.

| Gate/command | Actual result | Coverage |
| --- | --- | --- |
| Exact original Ubuntu test before correction |1 FAIL, exit1 | Reproduced RED retained separately from original publication failure |
| Exact original Ubuntu test after correction |1 PASS | Original UNSAFE assertion preserved |
| Adjacent Ubuntu owner/namespace/collision controls |4 PASS | Safe/unsafe and uncertainty decisions |
| python -B -m unittest discover -s infra/security -p "test_*.py" -v |72 PASS/0 FAIL/0 skips on Windows | Current corrected source; all native/launcher controls |
| Same discovery via existing Ubuntu/python3 |59 PASS/0 FAIL/13 skips;72 discovered |8 native Windows and5 PowerShell tests intentionally skipped; no other skips |
| Web lint/source typecheck/unit/build | Prior publication execution: PASS/PASS/28 PASS/PASS | Not rerun for the fixture-only correction; normal source typing, not fresh standalone test-source typing |
| Hosted backend workflow | Pending publication of this checkpoint | Does not run Windows-store, portable-store or Web suites |

Private logs retain RED, focused GREEN, adjacent controls and both full runs.
Only sanitized results are published; no absolute private evidence paths or raw
payloads. Linux import/execution succeeded for portable modules with WinDLL
initialization isolated to Windows construction; Linux protected-store runtime
is still unsupported. Counts across separate runs are never added together.

Legacy ACL exposure, sandbox-owned root settings, Foundation/smoke consumer
migration and PostgreSQL bind cutover UNVERIFIED remain OPEN. The historical
raw-cache scanner FAIL/npm attribution and privacy review2026-10-25 are preserved.
No real credential migration/provisioning/retirement, live owner/ACL change,
Docker/DB mutation or dependency/workflow change was performed. S2/S3/S4 have
not resumed. AC-001-04/05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## Historical review checkpoints

Prior checkpoint2026-10-19: **PCS_RR01_PATCH_READY_FOR_REVIEW**. The2026-10-18
corrective verdict was **CORRECTIVE_PATCH_READY_FOR_REVIEW** within
approved source/tooling/synthetic/no-write-dry-run scope. Not READY_FOR_PR or real
migration acceptance. Initial IMPLEMENTATION_READY_FOR_REVIEW was followed by
security review FIXES_REQUIRED_BEFORE_PR; initial results below are retained.
[ADR-0008](../architecture/decisions/ADR-0008-protected-credential-store.md).

## PCS-RR-01 strict presence correction — 2026-10-19

Independent read-only re-review: PCS-01/02 RESOLVED; PCS-03/04 PARTIAL because
lexists false was used as proof of absence in namespace/collision and recovery.
That API can hide query failures on Python3.14.2/Windows. No actual credential
leak or observed natural I/O incident was claimed. This checkpoint implements
the approved narrow correction, pending another security review; not READY_FOR_PR.

Same feature branch and HEAD e5994b08. Actual remote develop and all19 incoming
source hashes were verified. No staged/private artifact. Only the same3 security
helper/test files and6 directly related docs change;10 Phase1 files remain at
their pre-task hashes. No launcher, Web, business, workflow, dependency or migration
edits were necessary.

| Decision point | Corrected behavior |
| --- | --- |
| Windows.presence / PosixFixture.presence | PRESENT positively observed; ABSENT only justified not-found under verified ancestors; UNKNOWN for query/security/type uncertainty. Windows retains ancestor handles during the query; no content read |
| namespace_assessment | UNKNOWN error becomes UNVERIFIED; known unsafe namespace/object becomes UNSAFE; missing ancestors are allowed only after strict verified-absence proof |
| Target collision | true for observed target/parent; false only for both verified ABSENT; otherwise UNKNOWN. Presence categories/codes are separate fields and blockers |
| Native/POSIX backup guard | UNKNOWN prevents native replace/rename; existing backup is never silently accepted |
| Post-replace / accept / abort | Absence must be verified; UNKNOWN leaves intent/backup/pending evidence unresolved and writes no successful terminal/resolution |
| Object matching / intent / recovery lock | UNKNOWN cannot become missing intent, absent target or permission to create a replacement lock |

Public source/control/credential-name and revision/identity protections remain
unchanged. Dry-run uses an explicit legacy metadata-only audit in known unsafe
namespaces for owner reporting; this cannot upgrade sourcePresence or eligibility.
Query-error UNKNOWN stops before downstream target inspection. The remaining
is_file on the static consumer-reference list is advisory code discovery, not
absence/collision authorization; actual Docker consumers remain UNVERIFIED.

### Executed verification for PCS-RR-01

| Command/gate | Result | Boundary |
| --- | --- | --- |
| python -B -m unittest discover -s infra/security -p "test_*.py" -v | **72 PASS /0 FAIL /0 skips** |57 prior tests +15 new tests. Includes5 PowerShell,4 prepare and8 Windows-native tests. Subcases are not counted as additional tests |
| Raw existence-query injections | PASS | Access5, sharing32, I/O1117/device21, unexpected87; missing root and unsafe ancestor; actual helper/consumer decisions execute |
| Unknown target/source queries | PASS | No false collision-free/absence result, no downstream inspection/content/write; eligibility remains BLOCKED |
| Unknown temporary on accept / backup on abort | PASS | No replace or resolution; artifact identities preserved, writer barrier persists. Actual positive accept/abort works after uncertainty is removed |
| Unknown backup collision | PASS | Native call denied before mutation; intent/pending retained and writer blocked when reached through update |
| Unknown post-replace absence | PASS | Completed native replace is retained as unresolved, without terminal event; explicit positive reconciliation remains required |
| Native metadata/security verification failures | PASS | UNKNOWN with sanitized category; not treated as a known-unsafe audit fallback |
| Actual CLI main(['dry-run']) with content-open/read/create/mkdir/replace/remove guards | PASS |12 records,5 active/7 historical;0 content reads/0 destination writes. Target and parent ABSENT/VERIFIED_NOT_FOUND/code2, collision false. Source namespace UNSAFE; root incompatible/OWNER_ACTION_REQUIRED; migration BLOCKED |
| Web regressions | NOT RERUN, not a new PASS | No Web/shared behavior change; prior28 unit tests and lint/src typecheck/build remain historical evidence |

First scoped run70/0 and final run72/0 are separate successful runs, not cumulative
counts. Two existing dry-run fault fixtures were strengthened to inject at the
new query/security boundary; reparse/hardlink checks now also assert UNKNOWN.
Faults are synthetic; no real developer filesystem failure or secret mutation
was induced. Historical failed attempts and scanner FAIL remain retained.

Private evidence: original Phase1 private evidence root,
rr01-5076ee601c3c484aa201bd8b40626972. It retains before/after metadata and source
hashes, scoped test logs, guarded dry-run and bounded publication results. Raw
metadata/logs stay private. No real credential content is used for comparison.

PCS-01/02 remain resolved according to the independent review. PCS-03/04 and
PCS-RR-01 have implemented/tested corrections but await final independent
disposition. Real migration remains BLOCKED: legacy ACL exposure, root owner
action, PG bind cutover, incomplete foundation/smoke migration, Linux/hosted CI,
raw-cache scanner FAIL/npm-source attribution and power-loss durability are OPEN.
No end-to-end TOCTOU freedom, same-user isolation or AC-001-04 acceptance claimed.

Final RR-01 scope/privacy checks:19 paths,11 modified/8 untracked, index empty;
9 corrective files versus10 preserved incoming hashes. UTF-8 PASS;5 Python ASTs
parse;207 relative links resolve; bounded PEM-header/AWS-ID scan0 hits with2
positive controls. This does not certify a complete secret audit. Fourteen
legacy metadata records match the pre-task snapshot after JSON normalization;
no remaining owned synthetic fixtures, actual target strictly ABSENT/code2.
No credential-byte hash or database integrity inference is made. No live
credential/ACL/owner/Docker/DB/dependency changes, commit, push or PR occurred.
git diff --check PASS; HEAD remains e5994b08. No unsuccessful test runs in this
checkpoint; earlier failure records and raw-cache scanner FAIL are untouched.

## Corrective security checkpoint — owner-approved PCS-01..04

Same feature branch/HEAD e5994b08; remote develop was verified at the same full
SHA. Preserve the original19-file inventory, unstaged. Corrective edits are only
credential_store.py, windows_store.py, test_credential_store.py and directly
related ADR/evidence/traceability/plan/task/runbook documentation. No business,
launcher/consumer semantics, dependencies, migration or workflow changes here.

| Finding | Correction | Focused evidence |
| --- | --- | --- |
| PCS-01 ABA/stale rollback | Append-only global revisions plus per-target priorRevision chain; exact stored receipt/store identity; target and backup volume/file/type identities plus digests; rollback is a new revision | A->B->C->B rejected after reopening store; same-byte target/backup substitution rejected; legitimate rollback/new revision PASS |
| PCS-02 metadata lock collision | Public metadata_leaf reserves operation.lock, transactions, internal artifact prefixes and credential/marker names; rejects case aliases, trailing dots/spaces, streams/traversal/devices before mutation | Invalid-name matrix asserts create/mkdir/replace never invoked; ordinary mixed-case filename control PASS |
| PCS-03 dry-run eligibility | Independent inspectionStatus, ownerCompatibility, namespaceSafety, targetCollision, migrationEligibility, requiredOwnerAction and blockers; reviewed owner expectations can be supplied without machine SID hardcoding | Expected/unexpected/explicitly authorized owners, unsafe source/target, collision and inaccessible target tests; actual guarded12-file metadata audit |
| PCS-04 partial replace | Flushed immutable intent before replace; exclusive live OS lock; append-only terminal/recovery/resolution events; replay barrier and explicit bounded accept/abort | Failures before intent, before/during/after replace, postvalidation and terminal persistence; process termination before/after replace; unresolved writer denial and explicit reconciliation |

The transaction protocol and deliberately bounded durability guarantee are in
ADR-0008. Missing/incomplete intent and corrupt terminal evidence cannot be
automatically reconciled by this patch. A partial rename with missing target
retains backup/pending/journal and stays BLOCKED. No Windows partial-replace
error was observed naturally: those native-failure outcomes are injected using
synthetic objects. Two subprocess tests actually use os._exit before/after a
normal native replace, proving observed process termination/restart behavior,
not power loss. Ordinary mutation and recovery are both denied under a live lock.

Current commands (repository root unless specified):

| Command/gate | Actual result | Limit |
| --- | --- | --- |
| python -B -m unittest discover -s infra/security -p "test_*.py" -v | **57 PASS,0 failures,0 skips** |36 baseline +21 corrective tests; includes5 PowerShell,4 prepare tests,6 native Windows; process faults and mocks distinguished above |
| npm.cmd run lint; npm.cmd run typecheck (app/frontend/web) | PASS / PASS | Existing src typing scope; no dependency installation |
| npm.cmd test (app/frontend/web) | **28 PASS** |24 existing adapter +4 source tests; no browser/backend/services |
| npm.cmd run build (app/frontend/web) | PASS,39 modules | Generated dist remains ignored |
| CLI main(['dry-run']) with builtins.open/io.open and backend content/write operations forbidden | PASS,12 metadata rows;5 active/7 historical;0 content reads/0 destination writes | Source namespaces UNSAFE; root owner UNEXPECTED_OWNER against current developer, OWNER_ACTION_REQUIRED; safe existing target ancestors, target absent/no collision; overall migration BLOCKED |

Owner expectations are not inferred from whichever owner was observed. Without
an explicitly reviewed exception, expected owner is the current developer.
Thus the known sandbox-owned root is still incompatible and needs owner action;
recording this fact is not an owner/ACL change. Missing/access-denied evidence is
UNVERIFIED. The no-write audit is a snapshot, not a namespace lease, Docker mount
verification or authorization to transfer credentials. Historical rows stay
retention-only, never permanent active generation candidates.

Private evidence is retained under the original Phase1 private evidence root,
corrective-c80f312210e14d209bef8e7d0e677a30. It contains before/after metadata,
bounded publication manifest, Python attempt logs, Web command logs and corrected
dry-run output; do not publish raw files. First corrective run55/0 and subsequent
57/0 are separate successful checkpoints, not cumulative counts. Original
unsuccessful Phase1 attempts and the original privacy finding remain preserved.

The corrective checkpoint does not re-run Maven, Docker, real protected-store
launch, PostgreSQL, native build or hosted CI. Legacy ACL exposure, real migration,
root owner action, PG bind cutover, foundation/smoke wiring, Linux/hosted coverage
and original raw-cache scanner FAIL/npm-source attribution remain OPEN. File
flushes alone do not establish power-loss crash consistency; migration readiness
stays BLOCKED. A subsequent security review must confirm PR blockers are resolved.

Final corrective safety checks: the19-file inventory is unchanged; exactly9 of
those files differ from the Phase1 starting hashes (3 helper/test +6 docs).
All19 decode as UTF-8;5 Python files parse;203 relative Markdown links resolve;
bounded PEM-header/AWS-ID marker scan has0 hits with2 positive controls. This is
not a general secret audit or resolution of the retained raw-cache scanner FAIL.
git diff --check PASS, index empty, HEAD unchanged. Fourteen allowlisted legacy
file/directory metadata records (owner/DACL/identity/size/write time) match before
and after; no synthetic leftovers and the real project store remains absent.
No credential-content hash comparison or database integrity inference was made.

The first preservation comparator reported14 deltas because Python ACL ACE
tuples had been serialized into JSON lists. Field-level diagnostics found only
that representation mismatch; JSON-normalized comparison found0 actual field
deltas. Both the failed comparator output and correction are retained privately.
The final focused run after I/O-error sanitization again passed57/0; attempts are
not added together. Web NO_COLOR/FORCE_COLOR notice was nonfatal. No skipped,
suppressed or weakened security assertion was used to obtain PASS.

## Baseline and implementation inventory

Clean develop/origin-develop `e5994b08aecea9f049a9ffd2b3d7bd8dad5ab540`;
local/cached/actual remote target branch absent before safe creation of
`feature/ac-001-04-protected-credential-store`. HEAD unchanged; no commit/push/PR.

The prior20 files were candidates, not a blanket change instruction. Phase 1
contains **19 files**,12 source/test +7 documentation:

| Files | Purpose |
| --- | --- |
| infra/security/credential_store.py; windows_store.py | Shared policy/lifecycle/read-only CLI; stdlib Win32 backend separated for maintainability |
| infra/security/test_credential_store.py; test_launchers.py | Native, portable policy, session, dry-run and mock-backed launcher tests |
| app/backend/run-demo.ps1; run-postgres-demo.ps1; verify-postgres.ps1 | Explicit protected selector, legacy default; validated path/session lookup |
| infra/postgres/start-local.ps1; prepare-test.py | Protected validation/cutover guard; private test record before mocked SQL provisioning |
| app/frontend/web/tests/support/settings.ts; settings-source.ts; unit-tests/settings-source.spec.ts | Explicit isolated/synthetic settings, no developer fallback; tests |
| docs/architecture/decisions/ADR-0008-protected-credential-store.md | Scoped approved design/compatibility boundary |
| docs/implementation/ac-001-04-credential-store-evidence.md | This report |
| docs/runbooks/local-development.md; clean-clone-verification.md | Commands and scope restrictions |
| docs/quality/traceability.md; docs/implementation/day-1-plan.md; specs/001-platform-foundation/tasks.md | Evidence/status reconciliation; original acceptance unchanged |

Unchanged: business code, PostgreSQL/Flyway/roles, API/SDK, manifests/lockfiles,
workflow/ruleset, mobile/native, foundation/verification bootstrap implementations.
No dependencies were added or upgraded. The unchanged legacy consumers remain
explicit Phase 2 migration prerequisites.

## Helper contract

Known Folder comes from SHGetKnownFolderPath, SID from the process token; no
personal SID hardcoding or inherited root override. Ancestors are pinned and
checked before create/read. Native protected descriptors are supplied at
directory/file creation and validated before data writing; no create-then-lock.
Objects/collisions, unsafe ACEs, reparse redirects/hardlinks, incomplete manifests,
schema/hash/purpose mismatches fail closed with fixed sanitized reason codes.

`Store.synthetic()` creates only a uniquely named fixture in KnownFolder (or a
private POSIX fixture); publish is synthetic-only. No real provisioning CLI.
`Store.open_development().active_paths()` validates existing development markers,
committed generation, JSON fields and PostgreSQL password-file consistency.
Metadata create/update/rollback has a separate policy, exclusive live operation
lock and append-only revision/identity-bound transaction records. Unresolved
locks/journals stop normal writers; reconciliation is explicit and bounded.
Session test records are immutable and complete before SQL provisioning; failure
retains the record/resources for explicit recovery. No database drop was added.

Cleanup is fixture-only, prefix/root-ID/owner/link checked, inspects before
descending and deletes exact objects bottom-up. Temporary cleanup grants only
read+DELETE, not metadata write access to credentials. Junction/hardlink negative
fixtures are nonsecret and removed explicitly. No developer resource deletion.

## Initial Phase 1 executed verification — historical checkpoint

| Command/gate | Actual result | Boundary |
| --- | --- | --- |
| `python -B -m unittest discover -s infra/security -p "test_*.py" -v` | **36 tests PASS,0 skips** on Windows | Real Win32 ACLs/handles, collisions, immutable write/delete/native replace denial, broad ACE rejection, KnownFolder override, junction/hardlink, metadata backup/rollback including changed-backup/poststate negatives, sessions and no-write dry-run |
| Same suite: PowerShell controls | PASS within36 | Local process RemoteSigned only; no global policy change. Synthetic paths, Docker/Java mocks; missing protected config cannot read legacy; current migration/runtime role/seed env preserved |
| `npm.cmd run lint` (Web) | PASS | Existing ESLint, no rules disabled |
| `npm.cmd run typecheck` | PASS | Existing Web src scope |
| `npm.cmd test` | **28 PASS** |24 existing API/SDK adapter regressions +4 explicit-source tests; no browser/service connection |
| `npm.cmd run build` | PASS | Existing TSC/Vite production build; generated dist ignored |
| Auxiliary strict TSC for changed test sources | PASS | Existing mobile @types/node26.6.5 supplied via --typeRoots; no dependency/config edits, not clean-Web-install evidence |
| Actual `python -B infra/security/credential_store.py dry-run` | PASS metadata collection,12/12 METADATA_ONLY_NO_MIGRATION | Five active/seven historical;0 credential content reads/0 destination writes; target absent/no collision |
| Diff/publication checks | PASS at final checkpoint | Intended source/docs only, no private evidence staged; bounded scan, not all disk/history |

Auxiliary command (from app/frontend/web):

```powershell
.\node_modules\.bin\tsc.cmd --ignoreConfig --noEmit --strict --target ES2023 --module ESNext --moduleResolution bundler --types node --typeRoots ..\shipper-mobile\node_modules\@types --skipLibCheck tests/support/settings-source.ts unit-tests/settings-source.spec.ts
```

Web does not install @types/node directly. Initial standalone test-source TSC
without a type root failed TS2688; the auxiliary check reused an already installed
workspace package. Normal Web lint/src typecheck/build/unit tests passed with
their existing dependencies. Do not infer a clean-install test typing gate.

Other retained attempts: first hardlink fixture on an FR credential failed
AccessDenied; fixture was changed to writable synthetic metadata to exercise the
link-count rejection. No permissions were loosened on credential-under-test.
Initial `npm` selected policy-blocked npm.ps1; using npm.cmd required no policy
change. One PowerShell capture treated normal unittest stderr as NativeCommandError;
final subprocess capture obtained actual exit0/36 tests. Patch-context retries
were reconciled against actual diff. Earlier failures are not counted as PASS.

No Maven/backend DB/native build, Docker execution, full browser runtime, real
credential launch or remote CI was run. No Java/business behavior changed;
focused mocked/native and existing Web adapter regressions were appropriate.
Previous Docker bind-read evidence is retained, not rerun or called new evidence.
Portable branches are available for Linux CI, but Linux execution is UNVERIFIED
in this Windows-only checkpoint; Windows-specific skips elsewhere are not PASS.

## Dry-run, compatibility and open findings

Dry-run inspects only the exact5+7 legacy paths and their metadata ancestors.
It never reads/hashes credential contents, copies, creates a project store,
modifies ACLs/owners, runs Docker, changes binds or prints credential values.
Root owner suffix1005 is recorded; retirement **OWNER_ACTION_REQUIRED**.
Seven records map to owner retention decision, not active permanent credentials.
Consumer files are an explicit review list; actual Docker consumer/mount checks
remain **UNVERIFIED_NO_DOCKER_EXECUTION**. Metadata eligibility is not permission
or proof of safe migration.

Legacy remains the normal backend/PG mode before cutover; protected source never
falls back. Protected start-local InitializeOnly validates; non-initialize
container operations are intentionally blocked pending approval. Web manual E2E
now requires C04_CONTEXT or a labelled synthetic ELOGISTICS_TEST_SETTINGS_FILE;
unconfigured collection intentionally fails instead of reading developer secrets.
The existing isolated C04 source continues to work; no actual harness/browser was
launched this phase.

Legacy ACL exposure remains CONFIRMED/no proven exfiltration. No credential
bytes, live owner/DACL, PostgreSQL/Docker/AVD/global settings were changed.
Historical raw-cache scanner FAIL/attribution, privacy review2026-10-25 and
Node/Rabbit/native findings remain OPEN; no broad security certification.

## Phase 2 prerequisites

Owner review of this source checkpoint first. Then separately authorize real
store provisioning/transfer API and transaction, exact5-file byte-preserving
transfer,7-record archive/retention, owner1005 legacy retirement, remaining
legacy consumer wiring (foundation/smoke/verification), private rollback and
bounded publication checks. Container downtime/recreation/rebind requires its
own explicit approval with unchanged image/volume/roles/data. Never restore an
insecure .demo copy or reactivate a legacy-hardcoded launcher as automatic rollback.

**AC-001-04 NOT ACCEPTED; S1 remediation pending. AC-001-05 unaccepted;
Feature001/D1-08/Day1 PARTIAL. S2/S3/S4 not resumed.** No commit/push/PR.
