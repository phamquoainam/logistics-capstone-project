# D1-07 / C-06 — Final combined regression and acceptance evidence

Execution2026-10-17, Asia/Saigon. **C-01–06 local deliverables VERIFIED; C-06
LOCAL_DONE. D1-07 remains PARTIAL / READY_FOR_PR**, with publication, actual
remote checks and integration pending. **GO to prepare/publish a review PR after
separate authorization; NO-GO to merge now.** Feature001/Day1 remain PARTIAL.
Security dependency audits remain FAIL/OPEN, not silently accepted or fixed.

## Git safety and C-05 checkpoint

Started on feature/d1-07-demo-contract-client at665675317dc86b88864f01d0904bbe7683082f21.
Read AGENTS including mobile instructions, DoR, DEC-29, bounded ADR-0003, Day1,
slice spec/design/tasks and security/acceptance/test/CI policies. Exact13 C-05
files matched [its evidence](d1-07-c05-mobile-evidence.md):10 modified/3 new,
index empty. Reviewed public imports/consumer factory, actual compiler/Hermes
reports, dependency/lock diff, artifact/secret checks and preserved starter/config.
No backend/web/OAS/generated/migration/workflow change in that checkpoint.
No mobile operation is invoked by the fixture or starter; no credential/config
literal or native change. C-05 acceptance PASS for compile/bundle scope only;
audit findings and unexecuted device/business workflows remain disclosed.

The user authorized stage/commit only that hash-checked13-file allowlist after
review PASS. Staged diff-check PASS; committed
**1f268545ef35921462177f8516f928b57016c84a**,
`build(mobile): verify generated SDK consumer compatibility`.
Working tree clean immediately after commit. Same branch; no push/PR/merge.
C-06 verification below runs on that baseline plus the two reviewable test-helper
changes described below. C-06 files remain unstaged/uncommitted.

Actual environment: Windows11/amd64, Oracle Java21.0.10+8, Maven Wrapper3.10.0,
Boot4.1.1, Node24.13.0/npm11.6.2, TS6.0.3, Expo57.0.27/RN0.86.3, Python3.14,
Docker29.6.2 and PostgreSQL18.6. Approved package pins and all manifests/locks
remain unchanged. Maven uses the existing .demo/maven-repository offline; npm
clean installs reuse cache. This is not independent cold-machine onboarding.

## Verification plan, changes and results

Required: all accepted contract/generation/drift, consumer quality/import/build,
real wire/browser, backend architecture/H2/PG and privacy/cleanup gates. Already
passing C-01–05 gates were freshly rerun rather than copied as current PASS.
Full001 readiness/infra/onboarding and native business capabilities remain outside
the slice. Advisory review is required; a clean security audit is not claimed.

Only executable changes for C-06:

1. wire-harness.py uses postgres@sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c,
   the **same approved18.6-bookworm immutable bytes**. Local store retained the
   digest but no tag: image inspect tag@digest failed while repo@digest passed.
   No pull/retag/version change. Original first backend preflight attempt
   backend-fcc66d459174 FAILed before any Maven/container creation; not counted PASS.
2. New scripts/check-backend-baselines.py reuses CI provisioning/report guard in
   a local wrapper. It resolves the same canonical digest only in the imported
   function's local invocation; CI source/workflow is untouched. It runs three
   clean Maven modes, snapshots container/volume inventory, verifies captured ID,
   label/tmpfs/no volume, and removes only its owned test ID. Test URL guards
   require5433, so preflight checks that port and fails rather than stop/retarget
   a developer listener. No production/controller/test/assertion change.

| C task | Fresh gate / exact observed counts | Assessment |
|---|---|---|
| C-01 | Strict Redocly lint0 errors/warnings/ignored; exact11-operation source inventory;41 tests,0 skips including37 negative controls | PASS |
| C-02 | SDK clean install59 packages, actual generation twice,16-file raw-byte/source match; strict typecheck/build;44 SDK tests,0 skips, public exports/loader trap | PASS scoped; dev audit4 high OPEN |
| C-03 | Two owned real JAR/PG18.6 runs,27/27 tests/no skips each;108 responses34 success/74 negative,11 successful operations,0 mismatches | PASS |
| C-04 | Web clean install176 packages; lint/typecheck/build;24 adapter tests/no skips,7 actual browser tests/no skips twice, original5 API/3 browser cases retained | PASS |
| C-05 | Mobile clean install988 packages; lint/typecheck/Jest1 suite/1 test/no skips; actual Expo compiler/four negative controls, SDK-entry Hermes23 modules/14 SDK, separate starter Hermes576/zero SDK | PASS compile/bundle; no device/network claim |
| C-06 | Architecture23/23; H248 discovered/40 PASS/8 named PG-only skips; PostgreSQL48/48/no skips; report guard14/14; actual repeatability/privacy/cleanup | LOCAL_DONE; remote integration UNVERIFIED |

Modes overlap (architecture is included in full backend suites); these are exact
per-run counts, not an inflated unique-test total. No required local test fails
in final runs. npm audit exit1 remains a separate disclosed security result.

Commands executed; unless indicated otherwise exits are0:

| Working directory / command | Exit / outcome |
|---|---|
| Client `npm.cmd ci --no-audit --no-fund --cache ../../.demo/npm-cache` |0;59 packages |
| Each frontend `npm.cmd ci --ignore-scripts --no-audit --cache ../../../.demo/npm-cache` |0; web176/mobile988 |
| Client `npm.cmd run verify:c02` |0; lint/inventory/41 tests/two real generations/typecheck/build/44 tests |
| Web `npm.cmd run lint`, `run typecheck`, `test`, `run build` |0 each;24 tests, Vite39 modules/264.62kB JS/83.62kB gzip |
| Mobile `npm.cmd run lint`, `run typecheck`, `test` |0 each; actual unchanged Expo config/Jest1 |
| Root `node packages/api-client/scripts/check-mobile-consumer.mjs` |0; actual compiler/four exactly expected diagnostics +real SDK-entry Metro/Hermes |
| Mobile installed Expo CLI `export --platform android --output-dir <new ignored C06 starter run>/export --source-maps` |0; actual real starter entry/default Hermes |
| Root `python -B packages/api-client/scripts/check-backend-baselines.py` |0 final; three Maven modes/guards/privacy and owned PG cleanup |
| Root `python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v` |0;14 report/privacy/fail-closed tests |
| Client `npm.cmd run test:wire` twice |0 each;27 tests/108 HTTP responses each |
| Client `npm.cmd run test:web` twice |0 each;7 actual browser tests each |
| Each project `npm.cmd audit --json`, `--omit=dev --json` | Web0/0, SDK1/0, mobile1/1; findings below |
| `java -version`, backend `mvnw.cmd --version`; Git diff/whitespace/source/artifact checks |0, retained baseline |

Backend wrapper invocation per mode from app/backend:
`mvnw.cmd -o -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -Ddebug=false -Dtrace=false [mode flag] clean verify`.
Architecture flag -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests;
postgres flag -Ddemo.test.database=postgres; H2 no mode flag. Each runs the existing
verify-reports.py with its exact mode/Maven exit; all guard exits0. Actual
sanitized architecture report contains11 production-rule tests +12 fixture tests
(11 deliberate violations detected/one positive control), none skipped.

## Actual generation, compiler and HTTP/browser evidence

Fresh SDK scratch directories sdk-jhqscZ/generated and sdk-7fgyPI/generated
were independently reread after the command. Their16 relative-file inventories,
raw bytes/lengths/per-file SHA256 and source src/generated match, without stripping
content or normalizing line endings. Ignored safe-determinism.json records this.
Combined SDK digest e3ce1e183afbeb7f9cb14f70674c5a475adafb069a125fe9bf969fdcfe0ecfe6.
OAS SHA2567944466e247e85069ba747ed4d62dac2f2891dd8fff64d8fa160f6ed22d44b82.
No hand edits to generated source, schema or generator configuration/pins.
Actual generation receives the checked local Redocly object; the installed
js-yaml loader trap and source/ref/drift/compile negative controls still PASS.

Mobile SDK run sdk-091d7f5e-7183-460e-bc95-51bda42aab3e: actual Expo Bundler/
react-native config; TS2345 missing key, TS2322 string weight, TS2345 numeric path,
TS2307 unexported subpath all detected as the sole expected error. Real SDK-entry
HBC139704 bytes/SHA25603e7c11e30e7dda8ce8527dd17e00ff2d248337ae4c844785f9446cfca752fab,
23 modules/14 SDK with no generator/validation tooling. Starter C06 export
starter-b263d93b5f92:576 modules/0 SDK/HBC1148288 bytes,
SHA2562eea23c8bcc2f7791df16523107195249c798ee39f126f944b4626f1ee58e054.
Both match prior scoped hashes. App/index/native/config unchanged; SDK fixture
is not imported by the real starter. No APK rebuild/device/business API request.

Real HTTP success covers health/login/me/merchant, list/create/detail/cancel and
three admin reads with operation-specific Ajv schemas/status/Location. Actual
400/401/403/404/409/429 groups cover JWT expiry/tamper/issuer/live account trust,
RBAC/owner hiding, invalid input/key, throttling, merchant-scoped replay/conflict
and concurrent create/cancel. Actual PG aggregate/per-resource SQL proves one
effect/event/receipt and denied writes; schema validation alone is not that proof.
Padded-input normalization/current-representation replay nuances remain intact.
Arbitrary framework failures/all Jackson coercions and forced DB-outage500 are
not exhaustively executed or silently accepted through a generic default schema.

Browser7 includes real generated-client React login/current-user/merchant views,
create/list/detail/cancel, cross-owner denial/admin views, memory-only logout/
next session, actual invalid/expired bearer and safe HTTP errors. Lost committed
responses replay with retained inputs/keys and one creation/cancel event. Explicit
network abort/nonJSON503 and polling403 are branch fixtures, not actual backend
outage/normal auth-me403 claims; subsequent actual401 logs out. No mocked SDK
stands in for real HTTP. Vite dev-server journeys and production build are
separate evidence, not a claim of testing a production deployment.

## Isolation, repeatability and artifact privacy

Same approved PG digest throughout. Newly generated synthetic credentials/roles/
data only, inherited PG/DEMO/SPRING settings removed. No native5432/developer DB,
existing data mounts/named volumes or migration file modification. Temporary test
DDL/rollback/upgrade probes run only inside the newly owned databases.

| Current run | Owned data/ports | Result / cleanup |
|---|---|---|
| Backend backend-526d284d8774 | elogistics_test_464cdb7cc572 +elogistics_test_upgrade_464cdb7cc572, preflight-free127.0.0.1:5433 | PG18.6; cold+upgrade history1,2;48 PASS; ID/label/tmpfs/port/inventory cleanup PASS |
| Wire9998444da780 and94453c250138 | Two new elogistics_wire_<suffix> DBs, random loopback DB/backend ports |27/27 each;108 each; log privacy/owned cleanup/prior IDs/volumes PASS |
| Browserf246bd629956 | Owned PG60479/backend60484/Vite63706 |7/7; Java/Vite/container cleanup/privacy PASS |
| Browsere38cbcc77cd3 | Owned PG52608/backend52613/Vite55766 |7/7; same cleanup/privacy PASS |

Wire safe reports are byte-identical SHA256076527a15993c1396b1510819d2a4af48ce4c124819495adcc6f63e597be7b7a.
Browser safe reports are byte-identical SHA256e16d326932f5ec3706601569d247c7d7f138e4838fbd581ecfdb893c36d6cd28.
Fresh DBs/ports differ, sanitized case outcomes match. No benchmark or independent
machine claim. Docker harnesses ran sequentially so inventory checks do not
mistake another concurrent test container for developer state.
Final read-only sweep:0 remaining containers with either test ownership label,
all11 recorded test ports no longer accept TCP connections. No cleanup action
was taken against any pre-existing process/container/volume. The auxiliary
PowerShell named-process query returned1 when Java was absent; the successful
port/container audit and subsequent process inventory confirm no backend remains,
not a failed Java test or ignored harness exit.

Reports/raw credentials/logs/HBC/maps/screenshots remain ignored. Fail-closed
report guard14 tests confirm missing/failed/skipped/duplicate/bad-count/Maven-fail
results cannot be reported green and raw secret output is not published.
Bounded scan:15 generated private values against30 current safe artifact files,
changed executable source and production web JS,0 credential/JWT matches. All
private backend log scans PASS. UI screenshots contain only synthetic demo data;
no request trace/token/body is emitted by the safe reporter. This is not a full
Git-history, supply-chain signature or production security audit.

## Dependency security disposition — OPEN, not audit PASS

| Scope | D1-02 post-tooling snapshot | C-05 snapshot | Fresh C-06 | Exit |
|---|---|---|---|---|
| Web full / omit-dev |0 /0 |0 /0 |0 /0 |0 /0 |
| Mobile full |58:46 high/12 moderate |59:47 high/12 moderate |58:46 high/12 moderate |1 |
| Mobile omit-dev |35:23 high/12 moderate |35 same |35 same finding objects |1 |
| SDK full / omit-dev | Not present then |4 high /0 |4 high /0 |1 /0 |

No critical findings in these snapshots. Seven distinct root advisory URLs
across scopes: three SDK YAML roots and four mobile roots; counts4/58/35 are
affected-package/metavulnerability entries, not independent exploits. Mobile
C06 dropped the C05 Testing Library propagated entry; underlying four URLs and
locked versions did not change. Exact npm propagation/cache reason remains
UNVERIFIED; no remediation claim. D1-02's earlier pre-tooling22/22 snapshot is
separate from its approved post-tooling58/35 baseline.

Installed versions/paths below are verified with npm explain JSON. All remain
OPEN. Future fixes require a scoped reviewed compatibility/lockfile change;
no audit fix, override, major upgrade/downgrade or DEC-29 pin change here.

| Root advisory / severity | Affected range / actual dependency path | Evidence of exposure/mitigation, remaining risk and action |
|---|---|---|
| [YAML merge CPU DoS](https://github.com/advisories/GHSA-52cp-r559-cp3m), HIGH | SDK js-yaml4.2.0 within >=4.0.0 <4.3.0; openapi-ts0.99.0→ref-parser1.4.4 (also shared0.5.0)→js-yaml | Checked-object generation/local refs/loader-trap PASS avoid this installed YAML loader in the supported generator path. Not a library fix or proof all tooling parsers safe. Platform owner: test compatible transitive remediation/generator options under approval. |
| [YAML ordered-map CPU DoS](https://github.com/advisories/GHSA-5p4m-2wfm-xmqj), HIGH | Same installed4.2.0, >=4.0.0 <4.3.1 | Same bounded mitigation. External/unreviewed input and other parser paths remain risk; retain pin disclosure and remediation follow-up. |
| [YAML empty-merge CPU DoS](https://github.com/advisories/GHSA-2883-xcg3-v3hh), HIGH | Same installed4.2.0, >=4.0.0 <4.3.2 | Same supported-path controls; no claim global parser safety or resolved advisory. |
| [braces stack exhaustion](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm), HIGH | Installed3.0.3, affected <=3.0.3; Jest/jest-config→micromatch4.0.8→braces (also tooling graph) | Build/test glob-pattern path exists. Current starter/SDK maps contain0 braces modules; current starter does not accept external patterns. Tooling/untrusted repository inputs still need review. Mobile/tooling owner: compatible transitive/SDK remediation spike. |
| [node-forge RSA signature verification](https://github.com/advisories/GHSA-86w9-cpqp-85rv), HIGH | Installed1.4.0, affected <=1.4.0; Expo57.0.27→CLI57.0.28→code-signing-certificates0.0.6→forge | Certificate/signing tooling path, not the Java backend HS256 implementation.0 starter/SDK bundle modules; no signing flow/exploitability test here. Mobile owner: review certificate/tooling consumers and compatible update; keep HIGH. |
| [sprintf-js precision DoS](https://github.com/advisories/GHSA-hp3w-g68c-fv3c), MODERATE | Installed1.0.3, affected <=1.1.3; Jest/Babel Istanbul→load-nyc-config→js-yaml3.15.2→argparse1.0.10→sprintf | Formatter/tooling chain.0 current bundle modules; no user-controlled format string in starter. Not a general tooling safety proof. Test/mobile owner: compatible formatter/dependency repair with quality regression. |
| [uuid buffer bounds](https://github.com/advisories/GHSA-w5hq-g745-h8pq), MODERATE | Installed7.0.3, affected <11.1.1; Expo config-plugins→xcode3.0.1→uuid | Advisory targets v3/v5/v6 with caller buffer. Inspected xcode pbxProject.js currently calls v4() without buffer;0 current starter/SDK modules. Other consumers/exploitability not exhaustively checked. Mobile owner: compatible xcode/uuid update review, no blind major replacement. |

Mobile's own YAML versions4.3.2/3.15.2 differ from SDK's linked dev4.2.0;
seeing js-yaml in a propagated mobile audit does not alone imply the three YAML
DoS roots affect those mobile versions. npm explain can traverse the linked
package's development tools; that is not a mobile runtime bundle import.

Fresh starter source-map inspection finds0 modules for each root package above,
generator/Redocly/Ajv/TS. SDK-entry bundle guard likewise excludes Node-only
tooling. Those are current-entry reachability observations, not proof all future
Expo/native/update/provider capabilities or developer tooling are safe. Redocly
bundled internals are not exhaustively enumerated by npm audit; loader mitigation
does not establish their security. No exploit payload/DoS benchmark is claimed.

Read repository security/quality/acceptance policies and accepted slice DoD:
mandatory contract/security failures cannot be waived; advisory findings require
impact/owner disclosure. No additional zero-advisory/separate risk-approval gate
was found in those documents. This review adds no exemption or implicit risk
acceptance. Findings stay visible for maintainer review; no merge approval here.

## CI coverage, DoD and publication readiness

Current workflow is unchanged Backend verification, triggered by PR to develop,
push to develop or workflow_dispatch. Three actual configured check names:

| Check | Configured coverage | Current D1-07 remote status |
|---|---|---|
| Architecture gate |23 architecture/negative tests +report guard | UNVERIFIED, not pushed/PR'd |
| Backend default / H2 |40 backend tests +8 explicitly allowed PG skips +guard | UNVERIFIED |
| Backend PostgreSQL 18 |48/no skips on owned cold/upgrade PG18 +guard | UNVERIFIED |

These cover backend rules/behavior/DB regressions, not OpenAPI/Ajv drift,
deterministic SDK, npm audit, web/browser or mobile compiler/Hermes/Jest. Those
have local evidence only. Accepted design explicitly puts new frontend/CI
workflow outside this slice unless separately approved. No mandatory new job
was inferred; future CI expansion is a disclosed automation gap, not implemented
or labeled DEFERRED. Existing three checks must actually PASS on the published
PR HEAD before merge. Current ruleset enforcement/conflicts are not newly
verified remotely here; no settings change. Project-wide CI-pass DoD therefore
still prevents an official D1-07 integrated DONE claim.

| Slice acceptance | Current evidence | Limit |
|---|---|---|
| SC-AC-01 | TEST01/02:41 schema controls +two real generations/independent bytes | PASS local |
| SC-AC-02 | TEST04/05/08:24 adapter +27 wire +7 browser,11-operation real web SDK | PASS scoped journeys |
| SC-AC-03 | TEST06/wire/browser actual auth/RBAC/owner/input/error cases | PASS published fixtures, not all security/advisories fixed |
| SC-AC-04 | TEST07/08 SQL replay/concurrency and lost-response UI/key recovery | PASS actual PG/HTTP and explicitly identified branch fixtures |
| SC-AC-05 | TEST03/09 both consumer clean installs/compilers/quality/builds | PASS; mobile compile-only |
| SC-AC-06 | TEST10 backend23/40+8/48+0, privacy14/changed-file checks | Local PASS; actual remote required checks/integration pending |

**C-06 LOCAL_DONE, D1-07 PARTIAL / READY_FOR_PR.** No unresolved in-scope mandatory
contract/build/runtime-fixture error in final local runs. Advisories disclosed
with owners; remote CI is not PASS. GO for separately authorized publish/review,
NO-GO to merge until actual three checks, conflict/ruleset/integration review and
maintainer acceptance. All original REQ/AC remain verbatim.

Parent gates remain open: full-stack clean-clone onboarding with PostgreSQL/
Redis/RabbitMQ, dependency-aware health/readiness, Testcontainers-specific tests,
independent developer/machine reproduction, native shipper business scope and
production security/maintenance; literal AC-001-03 red-then-fixed CI experiment
and D1-08 final Day1 review. Existing D1-02/03 scoped DONE is not reopened by these
parent gates. D1-07 alone cannot close full001/Day1.

Next authorized-by-user workflow, not executed: review this inventory, commit
only valid C-06 files, normal push current feature branch, create one PR to
develop, wait/inspect all three exact HEAD checks and sanitized artifacts,
verify conflicts/effective rules, then request maintainer review. No force/reset,
direct develop push, PR creation or merge performed by this task.

## Exact C-06 file inventory

Sixteen files relative to1f26854:14 modified/2 new, unstaged.

```text
packages/api-client/scripts/check-backend-baselines.py
packages/api-client/scripts/wire-harness.py
packages/api-client/README.md
docs/implementation/d1-07-c01-contract-evidence.md
docs/implementation/d1-07-c02-sdk-evidence.md
docs/implementation/d1-07-c03-wire-evidence.md
docs/implementation/d1-07-c04-web-evidence.md
docs/implementation/d1-07-c05-mobile-evidence.md
docs/implementation/d1-07-c06-final-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
specs/001-platform-foundation/tasks.md
```

No manifests/locks/approved pins, business/backend source/tests/POM, OAS,
generator/generated source, web/mobile UI/entry/native/config, migration,
workflow/report guards or existing data changed. Outputs/private reports remain
ignored. C-05 checkpoint is the only new commit; no C-06 stage/commit/push/PR/merge.
