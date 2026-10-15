# D1-02 — Toolchain and mobile foundation execution evidence

## Execution checkpoint — 2026-10-15

User approved Phases B–F and reuse of existing branch
`feature/d1-02-toolchain-closure`. Baseline and HEAD
`2dcffd5cb4dbaea42473ba133bdc391b0723f390`: PR #6 merged, local develop,
origin/develop and live remote equal, clean tree, branch zero ahead/behind.
No branch recreation/reset/rebase, commit/push/PR or changes to backend,
architecture rules, workflow, PostgreSQL configuration/migrations/data.
Results below apply to uncommitted working-tree changes on that baseline.

**D1-02 DONE for the approved local toolchain, quality tooling and Android native
starter-build feasibility scope.** Required execution gates below PASS, including
an installed APK and observed starter render, not just Metro or a JS bundle.
Online patch recommendations and dependency advisories remain explicitly OPEN;
they are not reported as clean audits or full Feature001 acceptance. The approved
task does not require chasing the latest framework patch, zero upstream warning
lines or native business features that do not yet exist. No mandatory gate or
original REQ/AC has been removed. Full Feature001/Day1 remain PARTIAL.

## Toolchain and pinning

Observed Node24.13.0/npm11.6.2 and Java21.0.10, unchanged. Root `.nvmrc`
contains 24.13.0. Both frontend manifests declare exact engines node24.13.0/
npm11.6.2 and packageManager npm@11.6.2; project `.npmrc` engine-strict=true
is effective in both directories (`npm config get engine-strict`). No global
settings/version-manager installation or framework upgrade performed.

Lockfile v3 comparison against HEAD verifies unchanged resolved React19.2.3,
ReactDOM19.2.3, RN0.86.3, Expo57.0.27, TS6.0.3 and web Vite8.3.4.
Expo SDK57 reference and installed RN engines support Node24.13.0.

## Actual commands and results

Commands run in the corresponding `app/frontend/web` or `shipper-mobile` directory.

| Gate / command | Result | Evidence / limit |
|---|---|---|
| Web `npm ci --no-audit --no-fund` | PASS | Lockfile clean install; no cache deletion |
| Web `npm run lint` | PASS | ESLint9.39.5/typescript-eslint8.71.2, hook order/dependencies, max-warnings0 |
| Web `npm run typecheck` | PASS | Existing strict TypeScript configuration |
| Web `npm test` | PASS | Five real API-client tests with existing Playwright runner, no browser/backend requirement |
| Web `npm run build` | PASS | Vite8.3.4, 25 modules; same JS asset index-EpOxFtPx.js, 253.67kB/gzip79.87kB |
| Web `npm run test:e2e` | PASS | Three existing Edge browser regressions, 7.7s; isolated H2 in-memory backend, existing developer databases untouched |
| Backend `mvnw.cmd -o -B -ntp -Dmaven.repo.local=<repo>/.demo/maven-repository -Ddebug=false -Dtrace=false verify` | PASS final retry | 48 discovered,40 executed/pass,eight PG-only skips,zero failure/error; includes23 architecture tests. No new PostgreSQL execution claim |
| Mobile `npm ci --no-audit --no-fund` | PASS | 987 packages from lockfile; package manager integrity/engines retained |
| Mobile `npm run lint` | PASS | SDK57 eslint-config-expo57.0.2, max-warnings0; generated native folders excluded |
| Mobile `npm run typecheck` | PASS | Added RN/Jest types; no assertion suppression |
| Mobile `npm test` | PASS | Jest29.7/jest-expo57.0.6, RNTL14.1.0; one starter mount/remount test, actual rendered text, no snapshots |
| Mobile `npm ls react react-dom react-native test-renderer react-reconciler --all --depth=3` | PASS | test-renderer1.2.0/reconciler0.33.0 match React19.2.3; no invalid peer |
| Mobile `expo export --platform android --output-dir ../../../.demo/d1-02/android-bundle` | PASS | Hermes bundle1.4MB, 580 modules; not an APK/native-build claim |
| Online `expo install --check` | FAIL advisory version drift | Remote recommendation Expo~57.0.29 and @expo/metro-runtime~57.0.17 differs from retained approved57.0.27/57.0.16. No automatic --fix/upgrade |
| `EXPO_OFFLINE=1 expo install --check` | PASS with explicit reliability warning | Bundled/offline matrix only; does not supersede the online advisory or prove runtime |
| Native Gradle :app:assembleDebug | PASS | 6m58s, 304 tasks:272 executed/32 up-to-date, real x86_64 APK |
| APK signature/package/install | PASS | v2 signature verified, package absent before install, adb install Success; com.elogistic.shipper.dev1.0.0/code1/min24/target36/x86_64 |
| Native starter render | PASS | UI hierarchy and visually inspected screenshot; main JS running in the separate package, developer onboarding/menu dismissed before counting render |
| Native Metro/observation | PASS | Two cold launches; first PID32452 stable95s after first captured render, six ESTABLISHED sockets to10.0.2.2:8081; second PID2799 render/network pass |
| Native crash/ANR/JS-fatal checks | PASS within bounded windows | No matching fatal markers in either PID log; exit-info only intentional USER REQUESTED force-stop used for second launch |

Web tests assert request method/body/token/idempotency preservation, anonymous
header omission, typed backend409, network failure and safe non-JSON503 handling.
Existing browser tests exercise merchant isolation/create/read/cancel, invalid
login/admin visibility and form retention/retry. No production web/App/API source
was modified; backend was started only against new `jdbc:h2:mem:d102_regression`.
Credentials remain in ignored settings/process environment and private server log.
First backend verify ran all40 tests/pass but repackage failed because that
temporary Java server held target/backend-0.0.1-SNAPSHOT.jar open on Windows.
After stopping only the identified regression server, the exact verify command
passed including repackage; no POM/assertion/config workaround. Both logs retained
as backend-verify.log and backend-verify-final.log under .demo/d1-02.

Mobile test tooling was installed through Expo CLI. The first typecheck failed
on missing Jest globals; explicit TS types fixed it. Initial auto-selected
test-renderer1.3.1 required React19.3 through reconciler0.34; selecting its
supported1.2.0 peer fixed the graph without changing application React. Tooling
is devDependencies; expo-dev-client57.0.20 is the SDK-compatible native addition.
No legacy-peer-deps, audit-fix, dependency exclusion or skipTests was used.

## Android / Expo Go and native targets

Current AVD: **Pixel_7**, Android API37 (AVD target android-37.2), emulator-5554
online/boot complete. Expo Go host.exp.exponent57.0.9/versionCode444 present.
SDK has platform36, Build Tools36.0.0, NDK27.1.12297006 and CMake3.22.1;
these match generated build configuration. Java21 unchanged.
Initial free C/D/E measured 3.89/93.97/37.86GiB; no storage error observed.

No native directory existed. Generated Android once using
`expo prebuild --platform android --no-install --no-clean --skip-dependency-update react,react-native`.
Native directories stay ignored under the original mobile .gitignore; none were
edited by hand. app.json adds only development package com.elogistic.shipper.dev
and scheme elogistics-shipper-dev. Existing starter UI/name/slug stay unchanged.
Existing Expo Go commands are retained; explicit start:go/start:dev/android:dev
scripts distinguish targets. No EAS account/credentials, release signing or AVD
reset/wipe/uninstall was needed.

Native command: `android/gradlew.bat -p android :app:assembleDebug --no-daemon
--console=plain -PreactNativeArchitectures=x86_64 --max-workers=2` with
GRADLE_USER_HOME at repository `.demo/d1-02/gradle-home`. Generated Gradle9.3.1,
Android SDK36/NDK27.1/Kotlin2.1.20; APK targets Pixel7 x86_64 only.
Initial attempt failed because PowerShell split an unquoted -Dorg.gradle argument;
correct --max-workers syntax fixed the invocation, with no source change.
The corrected build completed successfully. APK is generated/ignored at
`android/app/build/outputs/apk/debug/app-debug.apk`, 63,488,363 bytes,
SHA256 **3f13b04a28b35d493eb29c71d607c0bcb27b0c6d1449cac1ae7255b61082f3d6**.
`aapt dump badging`, `apksigner verify --verbose` and installed package metadata
confirm identity/SDK/ABI/signature. `adb install` installed a previously absent
package; no replace/uninstall/data reset. Dev-launch URL:
`elogistics-shipper-dev://expo-development-client/?url=http%3A%2F%2F10.0.2.2%3A8081`.

First cold launch MainActivity at about07:20:54Z; JS Running main07:20:57.961Z.
Captured starter07:21:58.596634Z, same PID32452 and starter still rendered at
07:23:33.599913Z (95s after captured render). Force-stop07:23:35 was intentional,
exit-info reason10/USER REQUESTED, not crash. Second COLD launch succeeded in
1.398s; PID2799, starter/network observed again at07:25:01.998919Z. Six sockets
ESTABLISHED to10.0.2.2:8081; UI TextView belongs to com.elogistic.shipper.dev,
bounds[165,1174][915,1225]. No FATAL EXCEPTION/Fatal signal/ANR/JavascriptException
marker in collected app PID logs. These are bounded observations, not a claim
of release reliability or all permissions/background behavior.
System `dumpsys activity lastanr` additionally reported no ANR since boot;
events/crash buffers showed UWB vendor failures, not this application's crash.

Ignored evidence: pixel7-native-render.png/xml/log, native-observation.json,
pixel7-native-observed.png/xml/log, pixel7-native-second.png/xml,
pixel7-native-second-runtime.log and native-second-observation.json.
Second screenshot SHA256 **35aadd6e34b7c9eceddb2b96bca93a9b30ef9a73bff73f23be62d6fc405067e6**.

Final native observation at **2026-10-15T07:36:27.097799Z**: same second-launch
PID2799, starter still rendered, six Metro8081 sockets ESTABLISHED and no fatal
markers in captured PID log, over12 minutes after the second cold launch.
Final ignored records: native-final-observation.json, pixel7-native-final.xml/log.

CNG also warned that applying userInterfaceStyle requires optional expo-system-ui.
No module was added solely to silence that warning. The existing starter's white
view renders as tested; full theme/system-bar behavior remains outside this smoke.

Metro --localhost bound only ::1; IPv4/emulator10.0.2.2 could not connect.
That initial Expo Go attempt showed its error screen, not starter PASS. Correct
launch uses process-only REACT_NATIVE_PACKAGER_HOSTNAME=10.0.2.2 and --lan.
Dev client server8081 and separate Go server8082; manifests/bundle URLs now point
to10.0.2.2, host/status and actual Go render/socket evidence PASS. Existing adb
reverse host-19 tcp:8081 was preserved; no firewall/system configuration changed.

Expo Go on corrected endpoint: PID27782, starter TextView in host.exp.exponent,
bounds[165,1174][915,1225]; same PID and five ESTABLISHED sockets to10.0.2.2:8082
in later observation. No app fatal/ANR/JS-fatal markers in collected PID log.
Artifacts in ignored `.demo/d1-02`: pixel7-go-lan.png, pixel7-go-lan-ui.xml,
pixel7-go-lan-runtime.log, pixel7-go-observed-runtime.log. Initial failed screen/
logs are retained separately; HTTP200 alone was not counted as render evidence.

## Runtime findings and dependency advisory disposition

| Finding | Actual evidence | Disposition / limit |
|---|---|---|
| Expo update fallback | Go PID27782 logs one embedded/launchable UpdateFailedToLoad before Running main and actual render | Reproduced in Expo Go; nonfatal for observed starter; initial remote-download failure was independently explained by ::1 binding |
| Headless loader | Go stack names host.exp.exponent.taskManager.ExpoHeadlessAppLoader constructor/NoSuchMethodException | Expo Go initialization finding, no task/background app feature implemented; no claim background readiness |
| UIManager/ReactHost soft exception | Go has no-active-context UIManager exception; native first launch has ReactHost onWindowFocusChange before context ready, absent on second native launch | RN ReactHostImpl explicitly raises a soft exception when currentReactContext is null. Startup timing finding; no app JS fatal, both renders pass; not fixed by suppressing logs |
| Other initialization exceptions | Go RecentTaskInfo.numActivities reflection and TaskDescription opaque-color exception | Go/platform integration evidence, not application JS stack; root cause upstream fix UNVERIFIED, no version/config workaround applied |
| Native DevLauncher colorScheme | NoSuchFieldException in both native cold launches; not an App.tsx stack | SDK DevLauncherExpoAppLoader.kt reflectively sets AppearanceModule.colorScheme and catches Exception with Log.w at lines51–57. Field absent in this RN build; nonfatal for starter, upstream compatibility/color-style follow-up owned by mobile maintainer |
| UWB service | Reproduces before launch: /vendor/bin/hw/android.hardware.uwb-service /dev/uwb0, Rust panic failed serial open, SIGABRT | Android emulator vendor process/device issue; not attributed to app; no disabling service/wipe/system-image change |

ESLint9 and several inherited Jest/SDK packages emit deprecation warnings. npm
audit --omit=dev reports 22 baseline findings (7 moderate/15 high) versus35 in
the current lock (12 moderate/23 high), zero critical. npm graph classification
is not proof of APK runtime reachability: newly reported names include Jest/
RN test-preset transform dependencies. Raw reports and baseline manifest-only
comparison are ignored under .demo/d1-02; no original cache or lock was reset.
Remediation is not silently claimed: suggested RN downgrade0.72.17/major fixes
are incompatible with preserved baseline and were not applied. Security/SDK patch
advisory review remains open; these diagnostics are not waived by smoke PASS.

Disposition for this task: actual starter-build/quality/runtime gates PASS; no
source exception or fatal app failure remains unresolved. Retaining the tested
Expo57.0.27/RN0.86.3 baseline follows user constraints. Online latest-patch drift
is an advisory diagnostic FAIL, not a failed compile/engine/peer/runtime gate.
Do not use the offline check to hide it. Package deprecation/audit and upstream
SDK/Go/Android findings are tracked maintenance items, not silently marked fixed
or declared DEFERRED mandatory work. No framework/library patch was invented.

## Scope and references

REQ-001-01/05, AC-001-01 native-feasibility subset and AC-001-05 reproducibility
subset; local native feasibility now verified. Another developer's
full onboarding, release/iOS builds, scan/GPS/SQLite/offline/background features,
full infrastructure/health/contracts and remaining Feature001 AC remain separate.
Original REQ/AC text is unchanged. D1-03/04/05 completed scopes are preserved.

Own H2 in-memory regression server, Vite5173 and Go Metro8082 were stopped after
verification; only dev-client Metro8081 is kept for user inspection. Emulator,
installed apps, original adb reverse mapping, developer databases/volumes/caches
remain intact. No globally installed tools or settings were changed. Native output,
APK, logs, screenshots, Gradle cache and npm audit snapshots remain ignored.

Official references read before Expo/API changes:
[SDK57](https://docs.expo.dev/versions/v57.0.0/),
[Jest setup](https://docs.expo.dev/develop/unit-testing/),
[ESLint](https://docs.expo.dev/guides/using-eslint/),
[local development builds](https://docs.expo.dev/guides/local-app-development/),
[DevClient57](https://docs.expo.dev/versions/v57.0.0/sdk/dev-client/).

## Changed files and final checks

Implementation checkpoint before publication: 23 files (13 tracked modifications,
10 new), all unstaged/uncommitted; HEAD2dcffd5. Publication review follows below;
the PR/commit identifies the published revision of this checkpoint.

```text
.nvmrc
app/frontend/shipper-mobile/.npmrc
app/frontend/shipper-mobile/__tests__/App.test.tsx
app/frontend/shipper-mobile/app.json
app/frontend/shipper-mobile/eslint.config.js
app/frontend/shipper-mobile/jest.config.cjs
app/frontend/shipper-mobile/package-lock.json
app/frontend/shipper-mobile/package.json
app/frontend/shipper-mobile/tsconfig.json
app/frontend/web/.npmrc
app/frontend/web/eslint.config.js
app/frontend/web/package-lock.json
app/frontend/web/package.json
app/frontend/web/playwright-unit.config.ts
app/frontend/web/unit-tests/api.spec.ts
docs/decisions/open-questions.md
docs/implementation/d1-02-toolchain-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/design.md
specs/001-platform-foundation/spec.md
specs/001-platform-foundation/tasks.md
```

Final verification: git diff --check PASS; original full-feature REQ/AC and T-001
task criteria unchanged; protected backend/infra/workflow/business-source and
original mobile ignore rules unchanged. UTF8/relative file links and bounded
secret-pattern/10 generated-private-value audit PASS. APK/native build output,
Gradle cache, diagnostics/settings, screenshots and logs remain ignored.

## Final publication safety review — 2026-10-15

Live remote develop still equals baseline2dcffd5; branch is
feature/d1-02-toolchain-closure, with no upstream or duplicate PR before publish.
The 23-file inventory above exactly matches Git-visible changes. No backend,
workflow, architecture-rule, PostgreSQL/Flyway, web production source or mobile
App.tsx/index.ts changes. Original Feature001 requirement/AC text and full task
criteria are byte-equivalent after line-ending normalization. Relative document
file links resolve; bounded private-key/token patterns and ten private values
from ignored local settings are absent from changes. This is a bounded check,
not a universal absence-of-secrets guarantee.

Mobile `.gitignore` is identical to HEAD, not a pending large rewrite. Actual
native Android/iOS directories, Gradle output/cache, APK/AAB build paths,
local.properties/debug.keystore, node_modules, Expo/dist artifacts, local env,
logs and backup paths were verified with `git check-ignore -v --no-index`.
Root `.demo/` protects APK evidence, screenshots and the dedicated Gradle cache.
No forbidden generated/private artifact is tracked or in the 23-file inventory;
no ignore-rule change was needed. Standalone artifacts copied outside these
ignored output directories are not a publication convention of this repository.

Fresh audit commands: `npm.cmd audit --json` and
`npm.cmd audit --omit=dev --json`, in each frontend directory and against baseline
manifests/locks copied from Git HEAD to ignored folders, without installing or
fixing them. Reports: `.demo/d1-02/publish-review/*-{full,prod}.json`.

| npm audit graph | Baseline total (moderate/high) | Current total (moderate/high) |
|---|---|---|
| Web full and omit-dev | 0 (0/0) | 0 (0/0) |
| Mobile full | 22 (7/15) | 58 (12/46) |
| Mobile omit-dev | 22 (7/15) | 35 (12/23) |

All snapshots have zero low/critical findings. Audit exit1 for mobile is retained,
not labeled PASS. The current full graph includes five direct affected package
entries: expo/react-native (dependencies) and @types/jest/jest/jest-expo
(devDependencies); their severity propagates through transitive dependencies,
not five independently demonstrated application exploits. The other53 affected
entries are transitive. Full versus omit-dev differs by23 high-severity entries
at this checkpoint; omit-dev still includes RN/Jest/CLI peer/transitive paths.

There are four unique underlying advisories in the current graph (three in
baseline), rather than58 distinct advisories:

- [braces nested-pattern denial of service](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm),
  through micromatch in Metro/Jest tooling; already in baseline.
- [node-forge signature verification](https://github.com/advisories/GHSA-86w9-cpqp-85rv),
  through Expo CLI/code-signing-certificates; already in baseline.
- [uuid buffer checks](https://github.com/advisories/GHSA-w5hq-g745-h8pq),
  through xcode native-project tooling; already in baseline.
- [sprintf-js precision denial of service](https://github.com/advisories/GHSA-hp3w-g68c-fv3c),
  new transitive path through Istanbul config/argparse from test tooling.

Actual starter source does not accept user-controlled brace patterns/format
strings or perform these signature/buffer operations. Build/dev-server/test
tooling exposure deserves separate remediation; npm omit-dev membership alone
does not prove these paths execute in the APK. Full exploit reachability is
UNVERIFIED, not declared safe. Existing debug starter smoke is not a production
security acceptance or a clean dependency audit.

Six affected entries have `fixAvailable: true`: @expo/code-signing-certificates,
@expo/prebuild-config, @jest/reporters, @react-native/virtualized-lists,
jest-resolve-dependencies and metro-transform-worker. These are candidates for
a separate lockfile/SDK compatibility spike, not verified complete non-breaking
remediation. Other suggestions change Jest/@types majors, SDK57 to58, RN0.86 to
0.87, or even downgrade Expo to44. Such suggestions are not compatible fixes
approved for this task; none were applied. Retain the online Expo patch advisory
FAIL and upstream runtime findings above. Follow-up owner: frontend/mobile
maintainer; assess SDK-compatible fixes and rerun quality/native gates before
shipping features that expose these paths. No `audit fix`/force/override used.

Final review reran web lint/typecheck/five tests/build and mobile
lint/typecheck/one test: all PASS. Web output still index-EpOxFtPx.js. Both
manifest/lock roots match, engine-strict remains configured, core locked
framework versions are unchanged, and prior successful clean installs stand.
Windows instructions now explicitly require selecting/verifying the active
Node/npm versions; `.nvmrc` is not automatic version switching.

Only documentation changed during this review. Existing native APK still hashes
to3f13b04a28b35d493eb29c71d607c0bcb27b0c6d1449cac1ae7255b61082f3d6;
native build/runtime evidence above remains valid without rebuilding. Existing
final backend log confirms BUILD SUCCESS/48 discovered/40 passed/eight PG-only
skips; backend was not rerun locally in this publication review because its
inputs are unchanged. Git diff whitespace check passes. GitHub backend checks
must be observed on the published PR revision separately; no frontend/mobile
or native GitHub Runner execution is claimed by this local evidence.
