# Traceability Matrix (living document)

## Foundation pending changes — reconciliation 2026-10-27

| Trace / gate | Evidence | Disposition |
|---|---|---|
| T-001-10; REQ-001-05 / AC-001-04 subset; TEST-001-S3A-SEED-01 | Production preflight +74 tests already integrated by c59a3f0/PR20; actual develop84e72ac CI: Arch23, H2 483 PASS/17 skip, PG18 758 PASS/5 skip;74 preflight PASS in both DB jobs | Do not duplicate Java/Identity changes; scoped test evidence, not full AC acceptance |
| Historical six-file debt / preservation | Root has1 modified +8 untracked paths (six Foundation +three V3 sources); all9 bytes/hash preserved; two docs ADAPT, four checkpoint tools OBSOLETE for this integration, three sources ALREADY_INTEGRATED | Docs-only reconciliation proposed; no deletion/restore/cleanup authorization implied |
| Current policy / historical harness compatibility | Current report-policy128, isolation43, historical runner24 and observer310 controls PASS; current500-identity H2 artifact rejected by old248 contract; legacy H2 launch remains blocked | No weakened guard, unreviewed harness integration or runtime acceptance claim |
| AC-001-04/05 / security boundaries | Historical35 telemetry violations, raw-cache/legacy-store findings, S3AR1-07 LOW/open, Wrapper/observer and S3-B/S4 remain outstanding | NOT ACCEPTED; Feature001/D1-08/Day1 remain PARTIAL |

[Inventory, per-file decisions, exact tests/CI and safe consolidation proposal](../implementation/foundation-pending-changes-resolution.md).
Existing Identity/V3/earlier evidence below is retained. A scoped business task
uses its actual dependencies; historical file debt alone adds no new feature gate.

## S2-R1 nonblank configuration and CI inventory checkpoint — 2026-10-19

| Trace/gate | Evidence | Disposition |
| --- | --- | --- |
| REQ-001-05 / AC-001-04 subset; TEST-001-CONFIG-01 | Token: original 3 RED assertions preserved, byte/blank/raw-padding/JWT controls; 20 local PASS | S2-R1-01 RESOLVED for approved blank/length contract |
| TEST-001-CONFIG-02 | Seed: original 5 RED assertions preserved; conditional, bounds, padding/idempotency and pre-encode checks; 38 local PASS | S2-R1-02 RESOLVED; no full process/transaction acceptance claim |
| TEST-001-CONFIG-03/04 | Foundation context17 and PostgreSQL properties34 local PASS | Context/property evidence only; real startup/authentication matrix UNVERIFIED |
| TEST-001-CI-CONFIG-01 | Explicit109-case S2 inventory; 37 report/privacy controls PASS; actual clean H2 guard174 identities PASS | Local integration PASS; hosted evidence requires actual PR/run review |
| Regression | ArchUnit23 PASS; full H2 clean verify157 PASS/17 expected skips | No local PostgreSQL/Docker run; architecture and existing semantics preserved |

[Sanitized S2 evidence](../implementation/ac-001-04-s2-evidence.md).
S1 assessment complete; AC-001-04/05 remain NOT ACCEPTED. Real credential
migration BLOCKED; legacy ACL/root owner and historical raw-cache FAIL/npm
attribution remain OPEN. Feature001/D1-08/Day1 PARTIAL; original REQ/AC unchanged.

## Protected-store final review and portable fixture regression — 2026-10-19

| Trace/gate | Actual evidence | Disposition |
| --- | --- | --- |
| REQ-001-05 / AC-001-04 subset; TEST-001-STORE-04/05 | Independent review: all five PCS findings RESOLVED for Phase1; current Windows72 PASS/0 skips | REVIEW_PASS_READY_FOR_PR_PREPARATION, not full AC acceptance |
| TEST-001-STORE-05 portable negatives | Original Ubuntu58 PASS/1 FAIL/13 skips preserved; scoped fixture-only correction; exact GREEN1, adjacent4, full59 PASS/13 Windows-only skips | Portable fixture execution PASS; Linux production protected mode unsupported |
| TEST-001-STORE-03 | Prior publication Web28 unit PASS and lint/source typecheck/build PASS | Retained, not rerun for fixture correction; standalone test-source typing not newly proven |
| Publication/hosted boundary | Existing backend jobs do not execute protected-store or Web suites | Actual remote checks pending checkpoint publication; no hosted store/Web claim |

[Evidence](../implementation/ac-001-04-credential-store-evidence.md).
Literal REQ/AC unchanged; legacy ACL/root action, consumer cutover, raw-cache
scanner FAIL/npm attribution and privacy review2026-10-25 remain OPEN.
AC-001-04/05 NOT ACCEPTED; Feature001/D1-08/Day1 PARTIAL.

## PCS-RR-01 strict presence — 2026-10-19

| Trace/gate | Actual evidence | Disposition |
| --- | --- | --- |
| REQ-001-05 / AC-001-04 subset; TEST-001-STORE-05 |72 focused tests PASS/0 skips:57 regressions +15 query/namespace/uncertainty controls; actual native replacement denied on unknown backup presence | PCS_RR01_PATCH_READY_FOR_REVIEW; independent final disposition pending |
| TEST-001-STORE-02 | Guarded12-row actual dry-run; strict target/parent ABSENT proof; source unsafe/root owner action;0 content reads/writes | Migration BLOCKED, not eligible through UNKNOWN |
| TEST-001-STORE-03 | No Web edits; prior28 unit/lint/src typing/build retained | Historical only; no newly executed Web/hosted/Linux/runtime claim |

[Evidence](../implementation/ac-001-04-credential-store-evidence.md),
[contract](../architecture/decisions/ADR-0008-protected-credential-store.md).
No original REQ/AC changes or acceptance; raw-cache scanner FAIL/npm attribution
and legacy exposure remain OPEN. Feature001/D1-08/Day1 PARTIAL.

## AC-001-04 corrective checkpoint — PCS-01..04, 2026-10-18

| Trace/gate | Evidence | Disposition |
| --- | --- | --- |
| REQ-001-05 / AC-001-04 subset; TEST-001-STORE-04 |57 focused tests PASS/0 skips:21 corrective controls for reserved names, ABA/object substitution, actual process exit/concurrency, injected partial failure and bounded reconciliation | CORRECTIVE_PATCH_READY_FOR_REVIEW; subsequent security review required before PR |
| TEST-001-STORE-02 revised preflight |12 actual metadata rows; content-open/mutation guards; explicit incompatible-owner/unsafe-namespace/blocker classifications | Migration BLOCKED; root OWNER_ACTION_REQUIRED; no real credential access/cutover |
| TEST-001-STORE-03 regression |28 Web unit PASS; lint/src typecheck/build PASS | Local only; no clean-install typing, Docker/backend/browser/native or hosted claims |

[Updated evidence](../implementation/ac-001-04-credential-store-evidence.md) and
[ADR-0008](../architecture/decisions/ADR-0008-protected-credential-store.md).
No literal REQ/AC changes. Prior findings and original raw scanner FAIL/npm-source
attribution remain OPEN; AC-001-04/05 unaccepted, Feature001/D1-08/Day1 PARTIAL.

## AC-001-04 protected-store Phase 1 — 2026-10-18

[Evidence](../implementation/ac-001-04-credential-store-evidence.md),
[ADR-0008](../architecture/decisions/ADR-0008-protected-credential-store.md).

| Trace/gate | Executed evidence | Disposition |
| --- | --- | --- |
| REQ-001-05 / AC-001-04 security subset; TEST-001-STORE-01 |36 local Python/Win32/mock-launcher tests PASS, no skips; immutable vs mutable policy, exclusive creation, fail-closed source/purpose/hash/namespace and rollback negatives | IMPLEMENTATION_READY_FOR_REVIEW; not full config/secret audit acceptance |
| AC-001-04 migration preflight; TEST-001-STORE-02 |12 actual allowlisted metadata rows,5 active/7 historical,0 credential reads/0 destination writes; root owner1005 | OWNER_ACTION_REQUIRED; migration/legacy retirement not authorized; Docker consumers UNVERIFIED |
| Demo001/Web regression; TEST-001-STORE-03 |28 Web unit PASS, lint/src typecheck/build PASS; explicit C04/synthetic fixture settings, no developer fallback | Local only; browser/CI/real developer runtime not executed |
| Legacy/raw/privacy boundary | Legacy ACL exposure still confirmed; original scanner FAIL/attribution and evidence preserved | AC-001-04/05 unaccepted; S2/S3/S4 not resumed; Feature001/D1-08/Day1 PARTIAL |

**Formal owner decision — 2026-10-18: AC-001-01 DONE / PASS_WITH_DOCUMENTED_FINDINGS.**
[Acceptance record, E01–E08 and follow-up register](../implementation/ac-001-01-final-acceptance.md).
Original wording in Feature001 spec is unchanged; integrated SHA
`242e3d31192fb493938b09b398b4cd1ddd29a83b`. Earlier checkpoints below are historical;
their AC-001-01 blocked/pending labels are superseded only by this scoped approval.

| Trace / gate | Accepted evidence | Current disposition |
| --- | --- | --- |
| AC-001-01; REQ-001-01/02/04 cold-start clauses | E01/E02/E05/E06: new clone, approved tools, three isolated dependencies, V1/V2/roles, full-profile health/API, web build and new native APK/install/render | DONE / PASS_WITH_DOCUMENTED_FINDINGS; literal clause matrix in acceptance record |
| AC-001-01; original25 gates | E02 original22 PASS/1 FAIL/1 PARTIAL/1 UNVERIFIED; E03–E07 reconciliation24 PASS/0 FAIL/1 UNVERIFIED | Broad raw-artifact privacy assurance remains UNVERIFIED; historical failures retained |
| SDK/native verification deviations | E03/E04 complete SDK aggregates incl uninstrumented PASS; E06 new APK/runtime and independent review of wrapper exit1 | Accepted scope, not a Node/root-cause/parser fix; F-AC01-02/04/05/06/07 OPEN |
| Privacy/preservation; AC-001-04 boundary | E07 raw-cache scan FAIL/header origin UNDETERMINED; seven summaries PASS; E08 owned cleanup/foreign metadata preserved | F-AC01-01 OPEN; raw PRIVATE / NOT APPROVED FOR PUBLICATION; no comprehensive content-integrity claim |
| AC-001-04 / AC-001-05 / parent scope | Independent configuration/security and other-developer reproduction still require acceptance | Independently unaccepted; F-AC01-08/09 OPEN; Feature001/D1-08/Day1 PARTIAL |

**Historical checkpoint — 2026-10-18: AC-001-01 Windows blocker remediation.**
[Evidence](../implementation/ac-001-01-windows-remediation.md).

| Trace / gate | Actual current evidence | Status |
|---|---|---|
| AC-001-01; isolated resource ownership | Approved failed-session recovery complete; fresh a677c73052e94396b1cd8501127335c8 cleanup/idempotence PASS, foreign4 containers/5 volumes/4 networks equal; stable bridge/Engine | PASS local recovery/lifecycle subset; no owned resources remain |
| AC-001-01 SDK generation/build subset | Narrow LF policy, actual Git checkout2/2; strict16 outputs, C01 41 + SDK44 PASS | PASS local Windows; Linux/hosted fixes UNVERIFIED |
| AC-001-01 Rabbit cookie safety subset | Fresh cold-start/restart/recreate: actual server999:999, cookie999:999/0400/readable; non-root diagnostics, no EACCES, real AMQP auth/denials and persistent metadata; safety43 PASS | LOCAL_DONE / READY_FOR_REVIEW; not full AC acceptance, exact old cookie creator unproven |
| AC-001-01 runtime/native acceptance | Libuv not reproduced in40 probes/current SDK run; old new-APK timeout | OPEN; no Node root-cause fix/new APK claim |
| AC-001-01 / AC-001-05 / parent tasks | Full new-clone and independent developer acceptance not complete | AC01 BLOCKED; AC05 not complete; Feature001/D1-08/Day1 PARTIAL |

Original REQ/AC text and historical evidence are unchanged. Later local PASS does
not erase the failed fresh-clone run. No fixes were published in this iteration.

**AC-001-01 isolated-bootstrap implementation — 2026-10-17, local evidence only.**
[Evidence](../implementation/ac-001-01-isolated-bootstrap-evidence.md),
[runbook](../runbooks/clean-clone-verification.md).

| Trace / test ID | Scope | Current result |
|---|---|---|
| REQ-001-05; TEST-001-CCB-01 | Isolation/collision/inherited environment/secret privacy/owned cleanup controls | 40 focused tests PASS; existing bootstrap7/guards25 PASS |
| REQ-001-02/04; TEST-001-CCB-02 | Empty owned PG DB, V1/V2, schema/table ownership, runtime no-DDL/history, rerun/checksums | Final controlled local PASS |
| REQ-001-04; TEST-001-CCB-03 | Authenticated Redis/active AMQP, least privilege, full readiness/liveness/Demo auth | Final controlled local PASS9-gate scenario |
| AC-001-02 subset; TEST-001-CCB-04 | Owned PG restart/marker/history and live binding refresh/new backend | PASS local; not full cold-clone acceptance |
| REQ-001-01/04; TEST-001-CCB-05 | Temporary Vite proxy targets owned backend on dynamic ports | HTTP/auth smoke PASS; browser UI/native UNVERIFIED |
| REQ-001-05; TEST-001-CCB-06 | Final captured-resource cleanup/idempotence and foreign inventory equality | PASS final session; earlier bridge drift/manual recovery OPEN and preserved |
| REQ-001-02/03/04; TEST-001-CCB-REG | Existing architecture/H2/PG/Task A/C and Task B tests | Arch23, H2 48/17, PG73/5 exact reports; Task B5/5+16 gates via disclosed same-byte image/cache adapters PASS |
| AC-001-01/05 | Official cold-clone and independent developer reproduction | UNVERIFIED; must publish/integrate then use NEW clone; parent Feature001/Day1 PARTIAL |

**D1-08 Task D —2026-10-17: scoped DONE / AC-001-03 PASS.**
[Evidence](../implementation/d1-08-ci-red-green-evidence.md), Draft PR #13 (unmerged).

| Trace | Actual verification | Status |
|---|---|---|
| REQ-001-03; AC-001-03; TEST-001-CI-RED-GREEN-01 | Same production MODULE_BOUNDARIES rejects compiled Finance fixture -> Orders internal repository. RED73dac74/run39006911313 fails exactly financeCannotUseOrderInternalRepository, Maven exit1; correction c52819f removes fixture/restores implementation; GREEN39007163856 passes exact inventories | PASS hosted paired experiment; no rule/guard bypass |
| CI/privacy regression | GREEN architecture23/0, H2 48/17, PG73/5; guards25 each; sanitized reports/digests, credential/owned cleanup checks | PASS within backend coverage; Redis/RabbitMQ5 remain skipped |
| Integration/full acceptance | Experiment Draft PR remains unmerged; evidence transfer needs separate documentation approval; AC-001-01/04/05 and security/coverage disposition remain open | Feature001/Day1 PARTIAL |

This dated Task D entry supersedes earlier red-experiment UNVERIFIED snapshots
only; it does not rewrite original REQ/AC, other historical results or Task A/B/C.

**D1-08 Task C —2026-10-17: LOCAL_DONE / READY_FOR_REVIEW.**
[Decision](../architecture/decisions/ADR-0006-testcontainers-verification.md),
[exact method/command evidence](../implementation/d1-08-testcontainers-evidence.md).

| Requirement / test | Verified scope | Status |
|---|---|---|
| REQ-001-02; TEST-001-TC-01/02/05/06 | Actual pinned PostgreSQL18, empty V1/V2, schema/object ownership, independent restricted roles | PASS local Testcontainers |
| AC-001-02 subset; TEST-001-TC-03/04/07 | Validate/rerun checksums/data, V1->V2 marker, same-container restart with live random-port rediscovery | PASS local |
| REQ-001-05 / AC-001-04 subset; TEST-001-TC-POLICY | Immutable allowlist, non-privileged Ryuk/socket/loopback, no reuse/check bypass, credentials and owned cleanup guards | PASS6 Java/25 Python tests plus actual Docker assertions/audits |
| REQ-001-03 and Task A regression | Architecture23 incl12 fixture tests; PostgreSQL73 incl4 JDBC safety cases,5 named foundation skips; H2 unchanged48/17 | PASS local; exact report inventory |
| REQ-001-04 contract regression | OAS41, actual wire27/108 responses/11 operations/no mismatch | PASS local |
| Task C hosted CI / full T-001-09 / AC-001-01/03/04/05 | New profile configured in existing PG job; remote execution, full onboarding, literal red CI and broader security gates | UNVERIFIED/PARTIAL; no parent DONE claim |

Task A/B and D1-07 scoped DONE are approved; older entries below retain historical
states. Task C does not waive any original requirement, AC or remaining full-quality gate.

**D1-08 Task B — 2026-10-17, LOCAL_DONE / READY_FOR_REVIEW for approved scope.**
[Approved ADR](../architecture/decisions/ADR-0005-foundation-dependencies.md),
[exact results](../implementation/d1-08-foundation-evidence.md).

| Requirement / test | Verification scope | Status |
|---|---|---|
| REQ-001-04, AC-001-01 subset, NFR-O02; TEST-001-FOUNDATION-01 | Pinned real Redis/Rabbit, real PostgreSQL, full readiness and independent liveness; each pause/stop/recovery same backend | PASS final runtime; detection12s/recovery30s targets met, not an SLA |
| REQ-001-05, AC-001-04 subset; TEST-001-FOUNDATION-02 | Least privilege, incorrect credentials/vhost, restricted endpoints, actual missing config/profile startup fail-fast, Windows private ACL, bounded logs/reports | PASS; JUnit5/bootstrap7/report guard17; no full security certification |
| EC-001-02, AC-001-01 subset; TEST-001-FOUNDATION-03 | Redis loses fixture key, Rabbit retains test-created metadata across restart/recreate; owned cleanup | PASS; baseline containers/volumes/networks preserved |
| REQ-001-01/02/03/04; TEST-001-FOUNDATION-04 | Default/demo/demo,postgres isolation, unchanged V1/V2 and JDBC safety, real architecture/H2/PG/OAS/wire regressions | Arch23/H2 48+17skips/PG60+5skips/OAS41/wire27 PASS; exact5 foundation cases run separately; prior Node/libuv wire failure disclosed |

Task A actual post-merge develop run38959330146/f20c8c6 PASS23/44+12skips/56,
guards15 per job; previous local-only snapshots below retain their dated scope.
No full-feature, independent onboarding, Testcontainers, deliberate red CI,
business messaging or remote full-infrastructure coverage claim is made.

**D1-08 Task A - 2026-10-17 / baseline26afba7, local DONE.**
[Exact policy, commands, runtime/cleanup/privacy evidence](../implementation/d1-08-health-readiness-evidence.md).

| Requirement / new test ID | Actual evidence | Status / scope |
|---|---|---|
| REQ-001-04, AC-001-01 subset, NFR-O02; TEST-001-HEALTH-01 | FoundationHealthTests: real H2 and PostgreSQL DataSource, status-only UP, readiness OUT_OF_SERVICE503, real pool exhaustion DOWN503 and recovery; liveness stays200 | PASS local PostgreSQL-aware health subset; Redis/Rabbit/full cold-start still missing |
| REQ-001-05, AC-001-04 subset; TEST-001-HEALTH-02 | Anonymous exact GET probes; other methods/management paths rejected, even ADMIN cannot access denied health details; strict response JSON and bounded secret audits | PASS tested exposure/privacy, not comprehensive security certification |
| NFR-O02; TEST-001-HEALTH-03 | Real owned PG18.6/tmpfs paused/unpaused; HTTP200 ->503 ->200 on same backend, two outage observations with bounded JDBC/pool waits | PASS observed runtime failure/recovery; no Flyway startup-recovery or arbitrary-network SLA claim |
| REQ-001-02/03 and DEC-29 regressions; TEST-001-HEALTH-04 | Arch23, H2 44+12 named PG-only skips, PG56/no skips; guards15, contract41, wire27/108 responses/11 operations | PASS local; V1/V2/OAS/generated SDK/business behavior unchanged; new remote CI UNVERIFIED |

TEST-001-HEALTH-05 adds four mandatory real PostgreSQL safety tests under the
actual demo,postgres profile: long reads before/after probe and same physical
reuse, concurrent query/probe on a one-slot runtime test pool, terminated physical
connection replacement, socket-stall timeout/recovery and zero leaked pool workers.
No global socket read deadline remains; native isValid(1) restoration/eviction is
verified. These four tests are explicitly PG-only skips in H2 and mandatory PASS
in PostgreSQL mode; no arbitrary skip allowance was added. This addresses
REQ-001-04/05, AC-001-04 subset and NFR-O02/M02; not an HTTP SLA.

Task A does not close Feature001/Day1 or perform D1-07 post-merge documentation
closure. Earlier dated D1-07 execution statements below remain historical snapshots.

**Current C-06 final combined verification - 2026-10-17 /1f26854.**
[Final evidence/commands/security/CI coverage](../implementation/d1-07-c06-final-evidence.md).
C-05 committed; C-06 LOCAL_DONE, D1-07 PARTIAL / READY_FOR_PR, no publish.

| Requirement/test subset | Fresh actual evidence | Status/limit |
|---|---|---|
| SC-01/02; TEST01/02; SC-AC-01 | Strict lint/source11/41 tests, actual two generations and independent16-file raw-byte/source comparison,44 SDK tests | PASS local; no schema/generated/pin changes |
| SC-03/04; TEST04-08; SC-AC-02/03/04 | Web24 tests, real wire27/108 responses and browser7 twice on fresh PG; SQL ownership/replay/concurrency, loss/retry/session/error fixtures | PASS scoped; explicit intercepted branches distinguished from actual backend responses |
| SC-05; TEST03/09; SC-AC-05 | All three lockfile clean installs; web/mobile quality, Jest1/compiler4/actual SDK Hermes23 modules/14 SDK +starter576/0 SDK | PASS local compile/bundle, no device/business network claim |
| TEST10/SC-AC-06 local; BR-01/02/NFR-S01/S02/C02 subsets | Arch23 incl11 negative/1 positive; H2 40+8 named skips; real PG48+0; fail-closed guard14,15-value/30-safe-file bounded audit/cleanup | PASS local; not the literal CI red-then-fixed parent experiment |
| Dependency findings | Web0/0; SDK4 high/0; mobile58 full/35 omit-dev,7 distinct root advisories across scopes | Audit FAIL/OPEN, impact/path/owner disposition recorded; no remediation/risk waiver |
| D1-07 official DoD / remote integration | Existing three required backend checks not run on unpublished HEAD; no new frontend CI | PARTIAL / READY_FOR_PR; remote UNVERIFIED, merge NO-GO |
| Full001/Day1 | Readiness/full infra/Testcontainers/independent onboarding/native business/security maintenance/D1-08 | PARTIAL; original REQ/AC unchanged |

Older dated checkpoints below retain historical scope.

**Historical C-05 mobile consumer — 2026-10-17 /6656753.**
[Evidence/inventory](../implementation/d1-07-c05-mobile-evidence.md).
C-04 reviewed/committed23 files; C-05 DONE local/uncommitted, no publish.

| Requirement/test subset | Actual evidence | Status/limit |
|---|---|---|
| SC-05; TEST-001-CONTRACT-03/09; SC-AC-05 mobile subset; REQ-001-05/NFR-M01 subset | Local SDK public exports, actual Expo Bundler/react-native compiler,9 type assertions/four negative controls, clean install/lint/typecheck/Jest1 | PASS compile-only, no mobile business requests |
| SC-05 portability/import packaging | Actual SDK-entry Metro/Hermes23 modules/14 SDK, no Node-only tooling dependency; separate unchanged starter export576 modules/zero SDK | PASS bundle scope; not native execution/network/device integration |
| SC-02 package regression |41 contract+44 SDK/no skips, exact pins/build/typecheck/two-directory16-file deterministic output | PASS local |
| Dependency security review | Mobile before58 full/35 omit-dev, after59/35; additional Testing Library meta-finding via unchanged RN, same underlying advisory URLs; SDK4 high/runtime0 | Audits FAIL/OPEN, not remediation/risk acceptance; propagation difference root cause UNVERIFIED |
| TEST09/10 combined, SC-AC-06, D1-07/full001/Day1 | C-06 not started; remote after new checkpoints NOT RUN | PARTIAL / UNVERIFIED; no original REQ/AC edits |

Prior dated checkpoint statements below are historical.

**Historical C-04 web adoption — 2026-10-16 /a2609f9.**
[Evidence/mapping/inventory](../implementation/d1-07-c04-web-evidence.md).
C-03 checkpoint committed with approval; C-04 DONE local/uncommitted, no publish.

| Requirement/test subset | Executed evidence | Status/limit |
|---|---|---|
| SC-03; TEST-001-CONTRACT-04/08; SC-AC-02 web; DEMO-01..04/NFR-U02 subsets |11 generated operations/public exports;24 adapter +7 actual browser tests including original5/3, merchant/admin journeys | PASS local, isolated PG/backend; no handwritten wire DTOs |
| SC-04; SC-AC-03/04 web; BR-01/02/NFR-S01/S02/C02 subsets | Actual403/401 invalid/expired/missing/404 owner/409/429; committed create/cancel response loss/replay, retained input/key; explicit simulated nonJSON503/poll403 fixtures distinguished | PASS scoped; no new backend behavior or client authorization |
| SC-05; TEST03/09 web compiler/package subset | Local file dependency, clean install176 packages, lint/typecheck/build, existing versions unchanged | PASS web; mobile consumer NOT RUN/C-05 |
| TEST01/02/05/06/07 regressions; TEST10 H2/privacy subset |41+44 package tests,27 real PG wire tests/108 responses, Maven40 incl23 architecture/eight named PG-only skips, cleanup/privacy | PASS scoped; combined C-06 and48-test PG backend suite not newly claimed |
| Dependency findings | Web full/runtime0; SDK dev4 high OPEN/runtime0, generator mitigation intact | No remediation/implicit risk acceptance; pins retained |
| Full SC-AC-05/06, TEST09/10 combined, D1-07/full001/Day1 | C-05/06 not started; remote on C-04 NOT RUN | PARTIAL / UNVERIFIED; original REQ/AC unchanged |

Prior dated checkpoint statements below are historical.

**Historical C-03 actual HTTP/PostgreSQL checkpoint — 2026-10-15 /b385244.**
[Evidence](../implementation/d1-07-c03-wire-evidence.md). C-01/C-02 checkpoint
43 files committed under user authorization; C-03 changes uncommitted, no push.

| Requirement/test subset | Actual evidence | Status/limit |
|---|---|---|
| SC-01/04; REQ-001-04 contract subset; TEST-001-CONTRACT-05 |11 successful operations via real generated SDK/Fetch/backend/PG18.6, response/header/schema validation, required/nullability;27 tests per run | PASS two final runs; canonical input caveats retained |
| SC-04; NFR-S01/S02; TEST-001-CONTRACT-06 | Missing/invalid/expired/tampered JWT, wrong issuer, untrusted role claim, live role/enabled/scope, RBAC/owner404, invalid inputs, login429 | PASS actual HTTP; no developer account/data modified |
| BR-01/02; NFR-C02; TEST-001-CONTRACT-07 | Merchant-scoped keys/references; create/cancel replay/conflicts; two-way concurrent requests plus actual aggregate/per-resource SQL event/receipt assertions | PASS real PostgreSQL, not mocked DB invariants |
| TEST05/06/07 repeatability/isolation | Two fresh final databases, each34 success/74 negative responses, zero mismatches; safe reports same SHA256; owned cleanup/privacy PASS all six attempts | PASS local; initial libuv and SQL-guard FAIL attempts retained, not hidden |
| TEST01–04 regression / backend baseline | C-0141 + SDK44 PASS/no skips; Maven40 PASS incl23 architecture/eight named PG-only skips | PASS scoped; not a new48/48 PG backend-suite claim |
| Security/advisories | Refreshed dev npm audit FAIL4 high/runtime0, no graph/pin change; bounded loader mitigation unchanged | OPEN maintenance finding, not remediation/risk acceptance |
| Full SC-AC-02/04 UI/05/06 and TEST08–10 | Web/mobile adoption/browser/combined final gates still C-04–06 | UNVERIFIED / NOT RUN; D1-07/full001/Day1 PARTIAL |

Older wire NOT RUN/no-commit statements below are prior dated checkpoints.

**Historical C-01 acceptance / C-02 checkpoint — 2026-10-15 /20072b6.**
[Evidence and advisory disposition](../implementation/d1-07-c02-sdk-evidence.md).
C-01 final technical acceptance DONE; conditional user authorization satisfied;
C-02 local DONE, C-03–06 NOT STARTED; D1-07/full001/Day1 PARTIAL.

| Requirement/test subset | Actual execution | Status/limit |
|---|---|---|
| SC-01/02; REQ-001-04 subset; SC-AC-01; TEST01/02 | C-0141 pass;11 generated operations/16 files; two independent generations equal source bytes; stale/missing/extra controls fail | PASS local validation/generation/determinism |
| SC-02/05; TEST03 package portion | Strict TS6 package build, NodeNext/Bundler self-consumer declarations; ten diagnostic-checked compile negatives | PASS package; actual mobile/web consumers UNVERIFIED until C-04/05 |
| SC-03/04; TEST04 transport portion | Eleven actual SDK functions/injected Fetch, body/path/header/auth, raw status/error/network/retry-key controls; SDK44 pass/no skips total | PASS transport; real HTTP truth C-03, ApiError adapter/browser C-04 UNVERIFIED |
| REQ-001-05 subset; dependency review | Dev graph59 locked entries; npm audit FAIL4 high via js-yaml; runtime0; actual loader-trap/local-ref mitigation verified | OPEN dev maintenance finding; no zero-vulnerability/risk-acceptance claim |
| SC-AC-02–06; TEST05–10 | No real wire/consumer/browser/backend regression execution in this C-02 task | UNVERIFIED / NOT RUN; no full001 or Day1 closure |

Older C-01 audit0/no-SDK notes below retain the earlier checkpoint scope.

**Current D1-07 C-01 checkpoint — 2026-10-15 /20072b6.**
[C-01 evidence](../implementation/d1-07-c01-contract-evidence.md): local contract
foundation DONE; owner review pending before C-02. D1-07/slice IN_PROGRESS /
PARTIAL; C-02–06 NOT STARTED. This supersedes earlier implementation NOT STARTED
notes below without changing their historical evidence or original REQ/AC.

| Requirement / test mapping | Actual evidence | Status / limit |
|---|---|---|
| SC-01; REQ-001-04 contract subset; NFR-M02; TEST-001-CONTRACT-01 |11 source/OAS operations, strict Redocly lint0 errors/warnings/ignored;41 tests pass,37 negatives detected,31 documented JSON examples validate | PASS local C-01; source/schema fixtures, not HTTP conformance |
| SC-AC-01 validation portion | Clean lockfile install, approved validator pins, unique operationIds/local refs/auth/status/header/schema guards | PASS portion; generation/determinism portion UNVERIFIED, not full AC complete |
| SC-02–05; TEST-001-CONTRACT-02–10 | SDK not generated; consumer files unchanged; no wire/browser/backend execution for this slice | UNVERIFIED / NOT RUN, C-02–06 |
| REQ-001-05 security subset | Private dev-only tooling; npm audit new graph0; protected source/config diff empty | PASS bounded C-01; bundled-code audit limit and existing consumer advisories remain open |
| Full Feature001 / Day1 | Other infrastructure/readiness/onboarding/quality obligations retained | PARTIAL; no acceptance criteria changed |

The following D1-06 entry records its earlier documentation-only closure.

**D1-06 official DoR closure — 2026-10-15, baseline develop3186f01 / PR#7 merged.**
[Assessment and DoR](../implementation/d1-06-slice-readiness.md),
[subordinate spec](../../specs/001-platform-foundation/slices/demo-contract-client/spec.md),
[design/tests](../../specs/001-platform-foundation/slices/demo-contract-client/design.md),
[D1-07 tasks](../../specs/001-platform-foundation/slices/demo-contract-client/tasks.md).
**DEC-29 ACCEPTED** by explicit user approval, not metadata inference.
**D1-06 DONE** for assessment/selection/DoR; chosen slice READY_FOR_IMPLEMENTATION.
Source drift PASS: HEAD3186f01, eleven source-operation hashes/protected paths
unchanged; accepted contract/tooling scope and all mandatory planning gates recorded.
Consumer TS6 stays unchanged; test-only Ajv purpose, source/wire/drift/determinism
and application-only rollback preserved. **D1-07 NOT STARTED**; all implementation
test IDs below remain PLANNED/NOT RUN. Full Feature001/Day1 remain PARTIAL.

| Source/requirement subset | Accepted slice / planned test mapping | Current evidence/status |
|---|---|---|
| REQ-001-04 contract/client; NFR-M02/O03 | SC-01/02; TEST-001-CONTRACT-01/02/03/05 | Source11-operation inventory and generator metadata reviewed; actual OpenAPI/generated client absent |
| DEMO-01..04; FR-A01/A02/M01/M04/M05/AD01 subsets; NFR-U02 | SC-03; TEST-001-CONTRACT-04/05/08 | Existing web/API implementation confirmed; generated-client journey NOT RUN |
| BR-01/02 demo subset; NFR-S01/S02/C02 | SC-04; TEST-001-CONTRACT-06/07/10 | Preserve existing RBAC/owner/replay/concurrency; new wire-conformance tests NOT RUN |
| REQ-001-05; NFR-M01/S02; AC-001-04 subset | SC-05 / SC-AC-06; TEST-001-CONTRACT-09/10 | No new dependencies/source; documentation-only safety checks, not an installed-tool security claim |
| AC-001-01/05; REQ-001-04 infrastructure; Testcontainers full tasks | GAP-C03/04/05/06/09 | OPEN full001 obligations, outside this independent slice; no implicit DEFERRED |
| FR-S01..05 /009/010/015 | GAP-C02/07 | Mobile starter verified previously; business/native/API networking still NOT IMPLEMENTED/UNVERIFIED |

Full Feature001/Day1 remain PARTIAL. D1-01..05 completed task scopes are retained;
new gaps/status above distinguish literal full AC from those scopes. Prior D1-02
and older evidence below are dated checkpoints, not new execution claims.

| Closure evidence ID | Verified documentation gate | Status |
|---|---|---|
| DOC-001-D106-APPROVAL | Explicit user “DEC-29 — APPROVED / D1-06 Definition of Ready Closure”; scope/pins recorded in decision register/ADR/slice | PASS acceptance; no runtime compatibility claim |
| DOC-001-D106-DOR | Required DoR categories, source drift, source-to-contract inventory, fixtures/tasks/gates/rollback and links/protected REQ/AC checks | PASS documentation; D1-06 DONE, unpublished |

**D1-02 execution — 2026-10-15: DONE for approved local toolchain/native starter scope.**
Baseline2dcffd5, uncommitted feature/d1-02-toolchain-closure.
[Detailed commands/hashes/disposition](../implementation/d1-02-toolchain-evidence.md).

| Trace/test ID | Executed evidence | Status/limit |
|---|---|---|
| REQ-001-01/05; TEST-001-D102-PIN-01 | Node24.13.0/npm11.6.2, .nvmrc/exact engines/packageManager, engine-strict=true, lockfile clean installs | PASS; no framework major/patch upgrade |
| TEST-001-D102-WEB-01 | ESLint/typecheck/Vite build;5 API-client assertions and3 existing Edge regressions | PASS; isolated H2 in-memory backend, existing data untouched |
| TEST-001-D102-MOBILE-01 | SDK57 ESLint/Jest/RNTL, strict types, starter mount/remount, Android Hermes export | PASS; no real native behavior claimed from mocks/bundle |
| AC-001-01 native subset; TEST-001-D102-NATIVE-01 | Gradle304 tasks/APK SHA256/signature, install previously absent com.elogistic.shipper.dev on Pixel7/API37 | PASS debug/x86_64; not release/iOS/all-device coverage |
| TEST-001-D102-RUNTIME-01 | Two native cold launches, real starter UI/screenshot, PID/socket/exit-info evidence, no fatal/ANR/JS-fatal in bounded windows | PASS; upstream nonfatal colorScheme/ReactHost warnings recorded |
| TEST-001-D102-LOG-01 | Go and native PID comparison plus pre-launch UWB vendor-process evidence | PASS source/impact classification; upstream fixes/background readiness UNVERIFIED |
| TEST-001-D102-SDK-ADVISORY-01 | Online expo install --check recommends57.0.29/Metro57.0.17; frozen baseline57.0.27/57.0.16 | FAIL advisory recorded, no --fix or offline substitution |
| Dependency advisory review | npm audit graph22 baseline vs35 current omit-dev findings, zero critical; SDK/Jest deprecations | OPEN maintenance/security review; not APK reachability proof or a clean security audit |
| Full Feature001/Day1 | Full other-developer onboarding/health/contracts/infrastructure/native business and remaining DoR/quality tasks | PARTIAL, original REQ/AC unchanged |

D1-01/02/03/04/05 task scopes DONE; D1-06–08 remain open. Older task statuses
below are historical checkpoints, not an assertion of current missing tooling.

**Official post-merge closure — 2026-10-14:** D1-05 DONE for approved backend CI;
D1-03 DONE for PostgreSQL Phases A–E. PR #5 merged as
`f50c8972a3a4d0a9c877d6e7e99d2a918cd866fd`; develop push
[run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
/ attempt 1, three jobs success. [Detailed evidence](../implementation/d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).

| Trace / test ID | Verified evidence | Status / limit |
|---|---|---|
| REQ-001-03; TEST-001-CI-REMOTE-01 / ARCH-01 | Architecture 23 executed/pass, zero skip; exact production/fixture inventory | PASS for implemented code; six absent business modules N/A |
| EC/AC-001-03; TEST-001-CI-NEG-01 | 11 fixtures detect dependency/cycle/direction and assert production rule throws; positive actor control PASS | PASS detection on CI; deliberately red-then-fixed production workflow experiment UNVERIFIED, not performed/claimed |
| TEST-001-CI-DISC-01 / REG-01 | Guard 14 per job; H2 48 discovered/40 pass/eight named PG-only skips; PG48/no skip | PASS exact inventory and fail-closed report policy |
| REQ-001-02; TEST-001-CI-PG-01 | Isolated PG18 V1/V2, ownership/permission/validate/rerun/upgrade/rollback/concurrency | PASS; historical named-volume persistence/web/H2 proof retained; not Testcontainers implementation |
| REQ-001-05 subset; TEST-001-CI-SEC-01 | Three ZIP SHA256 digests, JSON/XML allowlists, credential audit and selected log token patterns | PASS bounded scope; no full-history/provider audit claim |
| TEST-001-CI-ENFORCE-01 | Active ruleset25049940; effective rules/develop; three contexts/app15368; strict policy/no bypass | PASS configuration enforcement; no deliberate blocked-merge experiment |
| TEST-001-CI-MERGE-01 | PR API merged=true; f50c897; push run matches merge/current develop | PASS integration; new docs branch uncommitted/unpublished |
| D1-03 approved task DoD | Original Phases A–E, local persistence/web/H2 proof, hosted backend run | DONE; native/tooling/full onboarding belong to D1-02/Feature001 |
| Full Feature001 / Day1; AC-001-01/05 | Contracts/client/health/full infrastructure/native/onboarding and broader quality still open | PARTIAL; no criteria reduced or implicitly deferred |

Earlier entries below retain historical scope. The table above defines current
CI/enforcement/merge/task status; old UNVERIFIED/PARTIAL notes are not current blockers.

**Nguồn:** [REPORT] Chương 3 §3.7. Tạo ma trận trong triển khai, trạng thái ban đầu tất cả `NOT_IMPLEMENTED`.

| Level | Mapping bắt buộc |
| --- | --- |
| Business source | Chương/mục báo cáo và mã BR, FR, NFR, UC |
| Feature | `specs/<id>/spec.md` `REQ-<id>-NN` |
| Acceptance | `AC-<id>-NN` mô tả Given/When/Then |
| Design | `specs/<id>/design.md` API/table/event/transaction |
| Task | `T-<id>-NN` trong `tasks.md` |
| Test | `TEST-<id>-UNIT/INT/E2E/CONC/FAULT-NN` |
| Evidence | Result + commit + environment + dataset + timestamp |

## Regeln / consistency
- Không đánh dấu DONE bằng nhận xét tay nếu chưa có test hoặc bằng chứng kiểm tra thủ công được ghi rõ.
- Không khai số liệu performance trước benchmark thật.
- BR-01–21 và toàn bộ NFR phải có coverage ít nhất một dòng trước nghiệm thu P0.
- Bất kỳ change tác động đến contract phải được liên kết đến OpenAPI/event schema và web/mobile generated client.

## Trạng thái khởi tạo
- Toàn bộ feature: `DRAFT_SPEC` / `NOT_IMPLEMENTED`.
- Tất cả test/evidence: `NOT_RUN`.

## Demo 001 — evidence bổ sung 13/10/2026

Mốc khởi tạo trên được giữ làm lịch sử; bảng này chỉ chứng minh demo scope do người dùng duyệt, không hoàn thành feature 001/002/003/005 hoặc thay AC gốc.

| Demo REQ / source subset | Implementation | Test / evidence | Status |
|---|---|---|---|
| DEMO-01; REQ-002-01/02/03/05, FR-A01/A02 | identity signed short-lived token, BCrypt, account status/role lookup, throttling | DemoAuthenticationTests (7); browser invalid login/admin | PASS demo; session/revocation/multi-scope P0 pending |
| DEMO-02; REQ-003-01 profile subset; FR-M01 | merchant read-only own organization | DemoShipmentTests.ownDraftAndCrossMerchantIsolation | PASS demo; pickup CRUD/catalog/geocoding pending |
| DEMO-02; REQ-005-01/03/04/05 subsets; BR-01/02/03 | owned DRAFT create/list/detail/cancel, durable operation receipts, events | DemoShipmentTests (6), concurrentCreateHasOneEffect; Edge A/B isolation | PASS demo; CONFIRMED/Finance/WMS/shipping pending |
| DEMO-03; REQ-002-04 subset | sanitized admin overview, no Finance mutation | anonymousUnauthorizedAndWrongRoleForbidden, admin browser test | PASS demo; role administration/audit pending |
| DEMO-04; FR-M04/M05 | React portal with real APIs, retained failed forms | 3 Playwright tests + typecheck/build | PASS demo |
| DEMO-05; REQ-001-02 persistence subset | isolated H2 file, Flyway demo V1 | smoke-demo.py create → backend restart → verify | PASS H2 demo; PostgreSQL foundation UNVERIFIED |

Environment/commands/artifacts: [demo evidence](../implementation/demo-001-evidence.md). No commit yet. Source contract: [demo design](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/demo-001-auth-merchant-shipment/design.md). Original feature tasks remain unchanged.

## D1-03 PostgreSQL milestone — 2026-10-13

Historical D1-03 records below retain their execution scope. Latest architecture
and closure assessment: [D1-04 evidence](../implementation/d1-04-architecture-evidence.md).

Demo-001 baseline was merged in 08c0c34 before this work. DEC-26 approved for this scope; current branch changes uncommitted. Original AC remain intact. [Execution evidence](../implementation/d1-03-postgresql-evidence.md).

| Trace / test ID | Executable evidence | Status / scope |
|---|---|---|
| REQ-001-02; TEST-001-INT-PG-01 | Migration job cold-start; PostgresFoundationTests.nineSchemasAndTablesBelongToOwner / actualPostgresAndSeparateMigrationPrincipal | PASS nine schemas/role separation on PG18; only approved demo tables |
| REQ-001-02; AC-001-02 subset; TEST-001-INT-PG-02 | validateAndRerunPreserveDataAndChecksums; versionOneToTwoUpgradePreservesExistingTechnicalData | PASS checksum/validate/rerun/V1→V2 technical-data upgrade; full P0 upgrade not claimed |
| REQ-001-02; TEST-001-INT-PG-03 | runtimeCannotDdlAssumeOwnerOrReadHistory; eventsAndReceiptsCannotBeChangedByRuntime | PASS runtime no DDL/history/owner and event/receipt immutability |
| BR-01/02; DEMO-01/02/03 | DemoAuthenticationTests (7), DemoShipmentTests (6) on PostgreSQL test DB | PASS demo auth/ownership/guards/conflicts/admin; P0 feature gates stay open |
| BR-02; TEST-001-CONC-PG-01 | concurrentCreateHasOneEffect; concurrentCancelHasOneEventAndReceipt | PASS one effect/event/receipt under two concurrent requests |
| Local ACID; TEST-001-INT-PG-04 | failedForeignKeyRollsBackWholeTransaction | PASS invalid FK rolls back earlier write on PostgreSQL |
| DEMO-05; AC-001-02 restart subset; TEST-001-E2E-PG-01 | smoke-demo.py create/verify --database postgres + container/backend restart + direct SQL | PASS named-volume persistence, same shipment/timestamp/event/receipt |
| DEMO-04; TEST-001-E2E-PG-02 | Web typecheck/build + three Edge Playwright tests before/after restart | PASS actual PostgreSQL APIs/render/negative/retry |
| Existing H2 demo compatibility | 14 original tests; original H2 runtime/proof at 8082 | PASS unchanged applied V1 and original persistent shipment |
| REQ-001-03; AC-001-03; CI; clean-machine AC-001-05 | No execution in the D1-03 slice; local architecture subsequently verified in D1-04 below | CI/reproduction UNVERIFIED; D1-03 PARTIAL, feature 001 not DONE |

## D1-04 — Architecture verification, 2026-10-13/14

Clean merged baseline `ccafeb4c339ffdfeb23181c245440b61176c2c31`; branch
`feature/d1-04-architecture-verification`, uncommitted changes. User-approved
minimal trusted actor and ArchUnit integration: [ADR-0004](../architecture/decisions/ADR-0004-architecture-verification.md).
[Actual scopes, commands, fixtures and reports](../implementation/d1-04-architecture-evidence.md).
Original REQ/AC and unchecked full-feature tasks are unchanged.

| Trace / test ID | Executable test/evidence | Status / limit |
|---|---|---|
| REQ-001-03; AC-001-03 local subset; TEST-001-ARCH-01 | productionScopeIsRealAndExcludesNegativeFixtures, packagesMatchApprovedModuleStructure, modulesOnlyUsePublicApis | PASS 37 real classes/four packages; six missing business modules N/A, not verified |
| REQ-001-03; EC-001-03; TEST-001-ARCH-02 | financeCannotUseOrderInternalRepository: same MODULE_BOUNDARIES evaluate + check throws AssertionError | PASS forbidden finance→Orders repository detected, exact dependency in failure report; no violation in production |
| DDD ownership; TEST-001-ARCH-03 | publicApisDoNotLeakInternals, modulesFollowApprovedDependencyDirection + API/direction fixtures | PASS approved collaborators only; no Orders AccountView/broad Identity API or domain actor dependence |
| Modular monolith; TEST-001-ARCH-04 | moduleGraphHasNoCycles + moduleCycleIsDetected | PASS real graph acyclic; isolated finance/pricing cycle detected |
| Selective Clean Architecture; TEST-001-ARCH-05 | domain/application/web/infrastructure direction tests + matching negative fixtures; controllersDoNotOwnTransactions | PASS nonempty actual layer scopes and wrong-direction/controller-transaction detection |
| Trusted principal; BR-01; TEST-001-SEC-01 | actorMerchantOwnershipIsRecheckedFromDatabase; existing live-role/tampered/expired/disabled/forged-owner tests | PASS H2 and real PG; existing token uses DB-derived merchant scope, old owner refused |
| REQ-001-05; AC-001-04 demo subset; TEST-001-SEC-02 | credentialsAndBearerTokenDoNotAppearInApplicationLogs; signingConfigurationFailsFastWhenMissingOrWeak; bounded Git-visible/artifact audit | PASS tested credentials/log/config/artifact scope; not a full history/provider/P0 security audit |
| D1-03/Demo-001 regression; TEST-001-REG-01 | Final Maven H2 and PostgreSQL verify, browser/typecheck/build | PASS H2 40 executed/eight PG-only skip; PG 48/no skip; three Edge tests; unchanged V1/V2/Compose/business semantics |
| REQ-001-03/AC-001-03 CI requirement; AC-001-01/05 | No executable GitHub workflow or independent clean-runner execution | UNVERIFIED; local tests do not close CI/reproducibility/full feature 001 |

D1-04 local verification satisfies the approved execution checklist; **propose DONE**.
D1-03 DB milestone is complete locally; overall D1-03 stays PARTIAL due to documented
environment/reproducibility prerequisites. No implicit DEFERRED or AC reduction.

## D1-05 — CI gate, 2026-10-14

Latest baseline `develop/2c609c42863505d0e28400eaa26f36c72b062769`, clean and
remote-matched; D1-03/04 merged. User confirms D1-04 reviewer closure; approved
local D1-04 task is DONE, with earlier execution scope preserved above.
Current branch `feature/d1-05-ci-verification`, HEAD unchanged, no publish.
[Evidence/commands/hashes](../implementation/d1-05-ci-evidence.md),
[workflow](../../.github/workflows/backend-verification.yml),
[operations](ci-verification.md). Original feature REQ/AC are unchanged.

| Trace / test ID | Evidence | Current status / limit |
|---|---|---|
| REQ-001-03, EC-001-03; TEST-001-CI-ARCH-01 | Clean Linux Wrapper architecture verify: 11 production/scope + 12 isolated fixture tests | PASS local 23/no skips; 37 classes/four implemented groups; missing six business modules N/A |
| AC-001-03; TEST-001-CI-NEG-01 | Same production MODULE_BOUNDARIES detects Finance→Orders repository; fixture asserts detail/throw; cycle/direction controls execute | PASS detection locally; actual remote red-PR/enforcement UNVERIFIED |
| REQ-001-03; TEST-001-CI-DISC-01 | 14 report-guard tests plus actual Surefire reports | PASS missing/zero/skipped/duplicate/malformed/count mismatch/nonzero Maven/conflicting PG failure+skip fail closed; known parameter-name alias covered |
| REQ-001-01 / EC-001-04 backend subset; TEST-001-CI-REG-01 | Uncached Linux Wrapper default/H2 clean verify | PASS 48 discovered/40 executed/eight named PG-only conditional skips; not full PostgreSQL or web/mobile verification |
| REQ-001-02 / AC-001-02 subset; TEST-001-CI-PG-01 | Fresh pinned PG18 tmpfs container, isolated DB/upgrade DB/roles; existing V1/V2 suite | PASS local 48/zero skip; ownership/permission, migration validate/rerun/upgrade, rollback/concurrency. Restart/persistence remain original D1-03 evidence |
| REQ-001-05, EC-001-05 / AC-001-04 evidence subset; TEST-001-CI-SEC-01 | Public artifact allowlist, synthetic secret stripping test, generated PG password log/artifact audit | PASS bounded tested outputs; no real repo credentials or raw diagnostic upload, no full-history/provider audit claim |
| REQ-001-03 / AC-001-03 actual CI; TEST-001-CI-REMOTE-01 | Workflow PR→develop/push develop, actionlint/ShellCheck and SHA provenance | Configuration PASS / READY FOR REMOTE VERIFICATION; no Actions run URL/tested remote SHA, required-check settings UNVERIFIED |
| AC-001-01/05 full cold-start/reproducibility | Local fresh Linux backend only | PARTIAL; full web/mobile/Redis/RabbitMQ/health and independent developer onboarding not verified |

D1-03 local DB milestone remains PASS, overall task PARTIAL for D1-02/full
onboarding prerequisites. D1-04 approved local task DONE; D1-05 overall PARTIAL
until real hosted runner execution and required-check enforcement. A successful
fixture test proves detection, not that a deliberately violating remote PR has
been blocked. No production violation, acceptance change or implicit deferral.

## V3 business reconciliation — planning only, 2026-10-26

[371 BC](../reconciliation/v3/capability-crosswalk.csv), [legacy IDs](../reconciliation/v3/legacy-crosswalk.csv), [roadmap](../implementation/roadmap-v3.md), [validation](../reconciliation/v3/validation.md). Mapping PROPOSED, target test IDs là kế hoạch. Không thay REQ/AC acceptance/auto VERIFIED. Feature001/D1-08/Day1 PARTIAL; T-002-05/06 PARTIAL; Feature002/AC-002-01..05 NOT ACCEPTED; GRANT_MUTATION_FUNCTIONALITY_LIMITED. Sáu Foundation files gốc nguyên vẹn; append chỉ worktree mới từ origin/develop.


## Canonical V3 rebuild — target traceability, not runtime acceptance

[371 BC](../reconciliation/v3/capability-crosswalk.csv) → canonical003–030 per-BC REQ/AC/DES/T-BC tasks;002 target delta separate,001/002 actual statuses preserved. [112 A/B coverage](../portal/group-ab-coverage.csv) provides UI/API behavior/data owner/permission/dependencies/planned tests/status/gap; [23 supplemental requirements](../portal/supplemental-requirements.csv) close Owner portal scope beyond old BC labels; [J-AB-01..10](../portal/journeys.csv) → [product gates](../portal/group-ab-acceptance.md). All target evidence PLANNED_NOT_EXECUTED. History preserved in retirement/legacy crosswalk/promotion-history; no current runtime PASS inferred from old ID match or moving folders.

## Spec003 controlled decisions and candidate readiness — 2026-10-27

[Owner six decisions](../decisions/spec003-six-decisions.md) → [exact13 CORE+DENY045 coverage](../../specs/003-sender-merchant-onboarding/contracts/coverage.csv) → reviewed77 operations/55 HTTP+3 dependency schemas, business rules, data ownership, frontend routes, per-BC layer tasks and declared security/concurrency/fault scenarios. [DoR](../../specs/003-sender-merchant-onboarding/readiness.md) records completed conditional technical review, SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION and the still-pending single production authorization. REQ-AB-ENTRY/CONTEXT mapped in003 tasks; legacy pickup consumer obligation stays004 command-owned. All business/runtime evidence remains PLANNED_NOT_EXECUTED; no prior Foundation/Identity acceptance evidence changed.


## Spec003 authorized implementation — customer admission kernel,2026-10-27

[Full Owner authorization and execution record](../../specs/003-sender-merchant-onboarding/implementation.md): all T003-01..12 authorized; current T003-01/02 IN_PROGRESS. Customer purpose storage and exact verified-email eligibility, owned current-subject transaction with final request/deadline checks and post-commit meaningful activity implemented. Existing SYSTEM defaults/username/session domain and V1–V6 preserved.

Actual local `mvnw -o -B ... clean verify`: H2 report527 discovered/510 PASS/17 policy skips/0 failure/error; new27 customer session/admission unit cases and original regressions included. Focused Architecture23 PASS including negative fixtures. Twelve new PG migration/customer cases authored and exact inventory extended; PG runtime and current PR CI pending, no older run substituted. No customer enrollment/HTTP/browser/full journey acceptance claimed. All61 business scenarios remain PLANNED_NOT_EXECUTED until complete runtime evidence. Source manifest explicitly scopes implementation changes; `validate.py --implementation` retains canonical371-BC gates and immutable migrations/contracts/runtime cleanup/report collector protection.

### Spec003 kernel CI and enrollment application increment

Kernel source5a10187 / integrationd02157c verified [CI40522928949](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40522928949): Architecture23 PASS, H2510 PASS/17 expected skips, PG18 797 PASS/5 expected skips,0 failure/error; exact checkout, artifact digests and owned cleanup in [receipt](../../specs/003-sender-merchant-onboarding/evidence/customer-kernel-ci.json). Earlier83ce24f PG failure retained in execution record, not reported PASS.

BC134/T003-01/02/03: pending enrollment,24h proof/resend/single-use atomic verified personal account, encrypted recipient+proof delivery intent, uniform/keyed receipts and no public grants implemented at application/JDBC boundary. Forward V8 only; prior migrations unchanged. Local H2 build550 discovered/533 PASS/17 expected skips;23 new domain/crypto cases included. Eleven new PG cases authored pending current CI. HTTP admission/durable quota, actual SMTP, recovery/profile/Merchant/UI/browser and all61 full behavioral scenarios remain pending; T003-01..12 are not marked DONE or ACCEPTED.

Enrollment source3b1677f / integration719279d: [CI40526551675](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40526551675) verified Architecture23/H2533/PG18 831 PASS with expected17/5 skips, no failures/errors; [exact receipt](../../specs/003-sender-merchant-onboarding/evidence/customer-enrollment-ci.json). The previous63c6a14 overall PG FAIL remains recorded, not silently rewritten.

BC134/IAM-CUSTOMER-01/S003-DELIVERY T003-01/02/03/11: actual customer HTTPS/Origin/CSRF Spring chain, purpose-disjoint RS256/cookies/quotas and real configured SMTP/fenced delivery worker implemented. Local H2 build587 discovered/570 PASS/17 expected skips; real loopback SMTP/TLS9 and HTTP negatives15 included, report/privacy130 PASS. Thirteen new PG cases are authored pending current revision CI; [execution record](../../specs/003-sender-merchant-onboarding/implementation.md) and [sandbox runbook](../runbooks/customer-onboarding-sandbox.md) distinguish supported enrollment/session delivery from remaining full Spec003 and browser gates. No full behavioral scenario is marked PASS solely from these component cases.

HTTP/SMTP sourceb113464 / integration6bf7ef2: [CI40534890983](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40534890983) verified Architecture23/H2570/PG18 881 PASS,17/5 expected skips,0 failures/errors; [receipt](../../specs/003-sender-merchant-onboarding/evidence/customer-http-delivery-ci.json). Current Spring customer+SYSTEM chain/SMTP/session and all13 PG increment cases have actual evidence, distinct from still-pending browser acceptance.

BC134/S003-RECOVERY/S003-PROOF-REPLAY: version-bound15min recovery, exact verified destination, uniform202, atomic password/version/session invalidation, HMAC-rechecked recipient evidence, V10 purpose-scoped receipts and CSRF-rotation replay implemented. Local clean H2 599 discovered/582 PASS/17 expected skips,12 additional unit/HTTP cases included; report/privacy130 PASS. Twelve new PG cases authored pending current CI. Privileged invitations/grants/contact remain unchanged. Full61 behavioral scenarios and complete Spec003 remain pending full runtime/browser/business evidence.

BC135/S003-PROFILE/S003-PROFILE-INJECTION - independent Merchant-owned personal profile and public workflow GET/PUT implemented with current Identity self-admission, mandatory versions/keyed original-response receipts, immutable profile history and redacted atomic audit. No organization/member or security-contact authority in profile input. V11 forward migration only. Actual local clean backend595 PASS/17 expected skips, Architecture23 included;13 new unit cases PASS. Ten new PG18 HTTP/race/migration cases authored/compiled pending current CI; profile browser and sensitive-contact operations remain. Completed internal unit/local tasks checked in [task checklist](../../specs/003-sender-merchant-onboarding/tasks.md), without promoting full T003-04/Spec003 acceptance.

Profile sourceede1e50 / integrationab498d6: verified [CI40622633558](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40622633558) Architecture23/H2595/PG18 928 PASS,17/5 expected skips,0 failures/errors; [receipt](../../specs/003-sender-merchant-onboarding/evidence/customer-profile-ci.json). Ten new profile/V11 cases and recovery population correction now have runtime evidence and their internal task checkboxes are DONE. Additional receipt-lookup/Customer UI increment is tracked separately; no full003/browser/BC acceptance inferred.

BC134/IAM-CUSTOMER-01/T003-03 - reviewed current-self password reauthentication implemented with durable existing customer budgets, session/authVersion-bound15min reference, V12 immutable keyed receipts, no idle extension or SYSTEM authority. Identity public consumption joins current admission; final earliest-proof-deadline check protects long business transactions. Nine new unit cases plus one admission deadline case PASS in clean backend606 PASS/17 expected skips; H2 report guard PASS. Five PG/HTTP and two migration/privilege cases remain pending current integration CI. [Execution/task record](../../specs/003-sender-merchant-onboarding/implementation.md) distinguishes completed bounded unit gate from incomplete sensitive-contact/link and full Spec003 acceptance.

BC133/PRIVACY-01/T003-04 - Owner-approved private filesystem quarantine byte adapter implemented;10 real Windows filesystem component tests PASS (focused21 including architecture11). Strict owner permissions, outside-web-root/server UUID naming, exact5MiB boundary, bounded digest reads and owned failure cleanup verified. POSIX link tests await Linux CI. Scanner/parser/application scope/quota/submitted immutability and full upload/download runtime gates remain open; no CLEAN or complete BC133 acceptance.

BC133/T003-04 - applicant application draft aggregate, public owner API, strict HTTP adapters and additive V14 authored. Ten actual unit cases PASS in focused44; no organization/Owner authority or submission success. BC032/T003-03: nine Owner-consent unit cases PASS with V13 pending PG. Implementation follows Owner acceleration directive7a138b96: focused development checks, early FE and real journey verification, full mandatory CI at integration checkpoint; full003 remains IN_PROGRESS/NOT_ACCEPTED.

## Owner-directed public website and customer entry, 2026-10-29

BC132/public website UI and Customer Auth entry: DONE for the reviewed increment on source 30893ade3628d55c99f3a312a8e5c296b3895234. Public pages, responsive navigation, honest utility placeholders, original hero illustration, auth visual shell, registration with unverified E.164 phone contact (forward V15), verification, login/session/logout and recovery reuse existing Identity. Registration confirmation stays client-only; phone grants no login/recovery/role authority. [CI 40837207925](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/40837207925) on exact source: Architecture PASS; H2 639/639 executed PASS, 17 policy skips; PostgreSQL18 991/991 executed PASS, 5 policy skips; zero failures/errors. Its integration revision 1b6be1518d79e59e78761617dc187cb5ea17acc9 records REAL_HTTPS_POSTGRES18_SMTP_BROWSER 9/9 PASS with CLEANUP PASS. Local frontend unit 40/40, customer UI fixture 9/9, contract validator 530/530 and local/S3-mock media 2/2 PASS. Local PG18 Task C was blocked before tests by missing Docker, so CI supplies the PG/browser evidence. `/merchant` remains PENDING for positive ACTIVE membership/organization-context shell; the route denies private access. Tracking, quote, coverage and stations remain explicit pending utilities. Owner-approved legal policy text and live S3 credentials/bucket are unavailable, so policy pages and live S3 evidence remain PENDING. This increment does not claim full Spec003 acceptance. See [sandbox runbook](../runbooks/customer-onboarding-sandbox.md).

## Owner-directed local S3/Gmail runtime — 2026-10-30 checkpoint

The historical BC132 paragraph above describes its exact earlier revision. In this new unmerged workspace: the backend AWS SDK probe has actually uploaded/read/validated private access/removed only a synthetic `dev-smoke/` object, and the fixed original SVG was uploaded to the private development bucket and read back by `S3PublicMediaStore` with matching SHA-256. The existing PostgreSQL18.6 container was reused, Flyway migration profile succeeded, backend HTTPS+PG18+S3 media readiness passed in a local test-transport smoke, and Vite HTTPS/CA validation passed. Focused SMTP/S3 tests and frontend typecheck/build passed. **Gmail live delivery, Owner browser journey and trusted browser certificate remain pending** because the Owner has not yet stored the App Password/trusted the self-signed certificate. The private evidence provider remains the approved filesystem sandbox; no S3 business upload/metadata flow or positive Merchant membership is claimed. See the local launcher procedure in [customer onboarding sandbox runbook](../runbooks/customer-onboarding-sandbox.md).

## Owner-directed customer entry and OTP amendment — 2026-10-30 working tree

IAM-CUSTOMER-01 enrollment now uses the Owner-approved six-digit OTP/120-second amendment; V16 preserves already-issued 24-hour links and keeps account creation atomic after verification. New registration accepts a domestic Vietnamese mobile number and stores its canonical unverified `+84` contact; phone login/recovery remains unavailable. Recovery still uses the 15-minute email link. The E-Logistics branded login/register layouts follow the supplied compact visual reference. Local frontend typecheck/lint/customer build, 41/41 unit cases and 10/10 isolated UI cases PASS. Backend H2 and Architecture Maven commands exit 0; PostgreSQL18 Testcontainers Identity 282/282, migration 45/45 and container safety 7/7 PASS, with real HTTPS/PostgreSQL/loopback SMTP browser 9/9 PASS. The broader local PostgreSQL command exits 1 because 30 Demo/Health context tests lack `PG_TEST_DB_URL`; it is not reported as full-suite PASS. Contract-only validator 530/530 PASS. Local launcher applied the forward migration and reported `LOCAL_FULL_STACK_HTTP_READY` using the existing trusted certificate, PostgreSQL18.6 and configured Gmail transport. Real Gmail inbox delivery and Owner-controlled registration/recovery remain **PENDING**; no live email PASS is claimed. Full Spec003 acceptance is not inferred.
