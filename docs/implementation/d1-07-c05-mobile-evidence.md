# D1-07 / C-05 — Mobile compiler and actual Metro/Hermes SDK consumer

**C-06 cross-reference - 2026-10-17 /1f26854:** this is the original dated
checkpoint evidence, not a claim all gates below ran again at its original date.
[C-06 final combined evidence](d1-07-c06-final-evidence.md) freshly verifies
C-01-06 local gates, actual PG48/no skips and HTTP/browser repeatability;
C-05 accepted/committed as1f26854. D1-07 PARTIAL / READY_FOR_PR; remote CI/integration
UNVERIFIED. Current audits web0/0, SDK4-high/0, mobile58/35; prior snapshot59 is
retained historically, not silently rewritten as remediation. C-06 uncommitted.

Execution2026-10-16–17 (Asia/Saigon). **C-05 DONE for the approved local compile/bundle scope**.
C-01–04 remain DONE locally. C-06 NOT STARTED; D1-07/Feature001/Day1 PARTIAL.
Mobile business UI/network/device integration is not implemented or claimed.
Dependency security findings remain OPEN; the audit is not PASS.

## C-04 final acceptance and authorized checkpoint

Initial branch feature/d1-07-demo-contract-client, HEAD
a2609f995d871608e4ae35c831976413bd360bf9. Read AGENTS including nested mobile
instructions, DoR, accepted DEC-29/bounded ADR-0003 and slice spec/design/tasks.
Exactly23 files matched the [C-04 inventory](d1-07-c04-web-evidence.md#exact-c-04-file-inventory),
17 modified/6 new, index empty. Reviewed generated-operation mapping, ApiError,
per-request auth, memory-only session and unchanged form/key/replay call sites.
Diff/whitespace/UTF-8/relative links/bounded secrets/artifact review PASS;
backend/OAS/generated source/migrations/workflow/mobile were unchanged.

Rechecked C-04 final safe run31898e5d9991:7 browser PASS, cleanup/privacy PASS;
the detailed41+44/wire27/108 responses/Maven40+8 evidence is retained in C-04,
not presented as newly rerun here. Refreshed web adapter tests24/24 PASS before
checkpoint. Four HIGH SDK dev findings remain OPEN; no remote CI claim.

User authorized only the reviewed23-file C-04 checkpoint after acceptance PASS.
Hash-checked the allowlist, staged exactly those23, staged diff-check PASS,
then committed **665675317dc86b88864f01d0904bbe7683082f21**:
`feat(web): adopt generated Demo-001 API SDK`.
Working tree clean immediately after commit. Same branch retained, no push/PR/merge.
C-05 below remains unstaged/uncommitted at that HEAD.

## Baseline and minimum integration

Actual Node24.13.0/npm11.6.2 retained. Mobile manifest still Expo~57.0.27/locked
57.0.27, RN0.86.3, React19.2.3 and TS~6.0.3/locked6.0.3. Native dev client and
all existing package versions/resolved/integrity remain unchanged. Only two lock
package entries are added: ../../../packages/api-client metadata and its local
node_modules link. Package name/main/scripts and exact toolchain pins retained.

Mobile adds file:../../../packages/api-client through the installed Expo CLI,
using the existing npm lock convention. No workspace manager/peer override/new
native package. SDK public exports remain `.` and `./client`; declarations/ESM
come from built dist. SDK dev tooling is not a mobile runtime dependency.

type-tests/api-client.ts imports all11 real operation values/types from the root
and createClient from /client. Its factory requires caller-supplied baseUrl and
Fetch; bearer/body/path/key are typed call parameters. No hardcoded endpoint,
token/account/merchant value, storage or module-import operation call. Returned
closures are compiler fixtures, never invoked by the starter or verification.
No hand-copied wire DTO, generated edit, manual HTTP stand-in or suppression.

The existing Expo tsconfig already supplies moduleResolution=Bundler,
customConditions=[react-native], strict=true and includes the fixture. No
tsconfig/Metro/Babel/app config change was needed. Starter App.tsx/index.ts and
native settings remain Git-normalized-content identical to HEAD; no APK rebuild,
Pixel7/device/backend business request or new mobile capability is claimed.

Official references read before implementation:
[Expo57](https://docs.expo.dev/versions/v57.0.0/),
[Expo agent index](https://docs.expo.dev/llms.txt),
[Metro symlink/file-map behavior](https://docs.expo.dev/guides/customizing-metro/#file-watching-and-crawling),
[versioned Metro reference](https://docs.expo.dev/versions/v57.0.0/config/metro/).
Actual installed defaults and successful bundle, rather than assumed monorepo
behavior, establish compatibility for this repository's local link.

## Compiler effectiveness and actual bundle evidence

packages/api-client/scripts/check-mobile-consumer.mjs is verification-only Node
tooling outside runtime exports. It uses the mobile-installed TypeScript and
actual Expo project/config, including normal fixture inclusion, then four
isolated in-memory negative compiler controls. No invalid production fixture,
ignored diagnostics, ts-expect-error or arbitrary success cast.

| Compiler fixture | Actual result |
|---|---|
| Nine assertions: writable password/string, weight/number, required key/string, path/string, reason/string, nullable account/event fields, shipment status union and inferred creation response | PASS using existing Expo/RN config |
| Missing Idempotency-Key | Detected TS2345 |
| String weight | Detected TS2322 |
| Numeric shipment path | Detected TS2345 |
| Named import from unexported generated internal subpath | Detected TS2307 |

The same script invokes the installed Expo CLI's actual export:embed on the
**value-importing SDK fixture entry**, not the starter or a types-only/mock entry:

```text
expo export:embed --platform android --entry-file type-tests/api-client.ts --dev false --bytecode --max-workers 2 --bundle-output <new ignored run>/sdk.android.hbc --sourcemap-output <new ignored run>/sdk.android.hbc.map
```

Final run sdk-39b87415-c28b-45e9-94ff-46f8d908387f: actual Hermes bytecode139704 bytes,
SHA25603e7c11e30e7dda8ce8527dd17e00ff2d248337ae4c844785f9446cfca752fab.
Header c61fbc03c103191f matches the installed Expo CLI's HBC check. Source map
contains23 modules including14 actual SDK JS modules, root barrel, generated
operation SDK and Fetch client implementation; full allowlisted SDK source list
is in safe-mobile-results.json. No HeyAPI/Redocly/Ajv/formats/TypeScript/js-yaml
or SDK scripts/tests in that bundle graph. Included SDK runtime modules import
only bundled relative code; package has zero runtime npm dependencies.
This proves transformation/packaging, not execution of Fetch/native networking.
Earlier successful run sdk-717d9b0e-55a4-4eda-a463-9474f943015d has identical
bytecode hash/counts. Final helper additionally requires exactly one expected
diagnostic per negative control, so no unrelated compiler errors are ignored.

Separately exported the **unchanged real starter entry** with normal Expo export,
Android/source maps/default Hermes, into a new ignored starter-e6369c070899
directory. PASS:576 source modules,0 SDK modules, HBC1148288 bytes,
SHA2562eea23c8bcc2f7791df16523107195249c798ee39f126f944b4626f1ee58e054.
This separate result protects the starter baseline; alone it would not prove SDK
bundling. The explicit SDK-entry result above supplies that missing evidence.
No bundle/cache clearing or overwriting previous evidence was required.

## Commands and gate matrix

| Actual command / gate | Result |
|---|---|
| Mobile installed Expo CLI: `expo install @elogistics/api-client@file:../../../packages/api-client --npm -- --package-lock-only --ignore-scripts --no-audit` | PASS corrected invocation, only local link/metadata added; EXPO_OFFLINE=1 and cache via npm_config_cache |
| Mobile `npm.cmd ci --ignore-scripts --no-audit --cache ../../../.demo/npm-cache` | PASS988 packages on corrected lock |
| Mobile `npm.cmd run lint` | PASS, zero warnings/errors |
| Mobile `npm.cmd run typecheck` | PASS, unchanged actual Expo/RN config |
| Mobile `npm.cmd test` | PASS1 suite/1 test/no skips: starter mount/unmount/remount assertions |
| Root `node packages/api-client/scripts/check-mobile-consumer.mjs` | PASS real compiler +4 negative controls +actual SDK-entry Metro/Hermes,23 modules/14 SDK |
| Mobile installed Expo CLI `expo export --platform android --output-dir <new ignored starter run>/export --source-maps` | PASS actual starter/Hermes576 modules, no SDK fixture imported |
| Client `npm.cmd run verify:c02` | PASS41 contract+44 SDK/no skips, lint/inventory, strict build/typecheck, two-directory/source16-file byte determinism |
| Lock/dependency/export/UTF-8/link/diff/bounded secrets/artifact checks | PASS; no versions/approved pins changed, runtime exports only public paths |
| Mobile/SDK security audit | FAIL/OPEN; exact before/after below, no force fix |
| APK/device/mobile business network, C-06 combined gates, remote CI on these commits | NOT RUN / UNVERIFIED, outside C-05 |

EXPO_OFFLINE's dependency-validation warning is retained. It is not Expo Doctor
PASS or a latest-patch compatibility claim; actual compiler/Metro gates above
verify the selected local SDK. Existing Expo diagnostic/advisory limitations
remain open. No native package, provider/credential or APK change.

Earlier attempts preserved: first Expo install passed `--cache` with a separate
value through CLI extras; it was misparsed as a dependency named undefined.
Detected in diff, removed only that newly introduced manifest/lock entry and
link metadata; reran Expo install with cache in environment, then fresh npm ci.
The actual cache data was not deleted. No malformed entry remains in the final
manifest/lock/installed graph. The earlier989-package install is not the final
clean-install gate. First compiler helper attempt expected2307 from a side-effect
unexported import and FAILed its diagnostic check; replaced the fixture with a
named operation import from the same forbidden path. Actual2307 then detected;
exports/types/compiler assertions were not relaxed to pass it.

## Dependency/security findings and limits

All audits use npm audit --json, optionally --omit=dev, with no fix/override.
Raw advisory JSON stays ignored. These counts are affected package findings,
not counts of independent underlying advisories or shipped exploit paths.

| Graph | Before C-05 | Final after C-05 | Disposition |
|---|---|---|---|
| Mobile full |58:46 high/12 moderate/0 critical |59:47 high/12 moderate/0 critical | FAIL exit1, OPEN |
| Mobile omit=dev |35:23 high/12 moderate/0 critical |35, exact finding objects unchanged | FAIL exit1, OPEN |
| SDK full |4 high/0 critical |4 high; approved graph/pins unchanged | FAIL exit1, OPEN |
| SDK omit=dev |0 |0, no runtime npm deps | PASS scoped; not a mobile audit PASS |

Additional full-graph finding is existing @testing-library/react-native14.1.0,
`via: [react-native]`; its locked version did not change. Underlying advisory URL
set is identical before/after, with no new SDK runtime package. Refreshed original
HEAD manifest/lock in an ignored control directory still reports58; final graph
reports59. Therefore the extra reported HIGH is explicitly retained, not erased
as noise or described as a newly fixed issue. The exact reason npm's propagation
differs is UNVERIFIED. See [npm metavulnerability documentation](https://docs.npmjs.com/cli/v11/commands/npm-audit/#calculating-meta-vulnerabilities-and-remediations).

Baseline Expo/RN/Jest/Metro dependency findings and the additional propagated
Testing Library finding remain project/mobile maintenance work; SDK generator
four HIGH findings remain platform maintenance. Checked-object generation and
the js-yaml-loader negative/trap controls remain intact, not remediation or
implicit risk acceptance. npm's suggested major downgrade/fixes were not applied.
No zero-advisory requirement was invented/removed; technical compatibility DONE
does not close these security findings. Device/runtime exploitability and full
dependency remediation are not established by an HBC build.

## Traceability, next gates and exact file inventory

SC-05/TEST-001-CONTRACT-03/09 mobile compile/package portions PASS; SC-AC-05
consumer compatibility now has both web and mobile local evidence. C-05 doesn't
complete the C-06 combined invocation/SC-AC-06 or parent001 onboarding/native
business/Redis/RabbitMQ/Testcontainers requirements. Original REQ/AC text retained.

C-06 next, only when instructed: run final package generation/validation/build/
tests, both frontend lockfile/quality gates including mobile consumer; refresh
web browser/HTTP regressions, backend architecture/H2 and isolated PostgreSQL
regressions/report privacy guards. Record exact counts/skips and protected-file
hashes. Remote three required checks remain unverified until separately approved
publish; no C-06 implementation or publishing was performed in this task.

Thirteen C-05 files (10 modified/3 new), unstaged relative to6656753:

```text
app/frontend/shipper-mobile/package.json
app/frontend/shipper-mobile/package-lock.json
app/frontend/shipper-mobile/type-tests/api-client.ts
packages/api-client/scripts/check-mobile-consumer.mjs
packages/api-client/README.md
docs/implementation/d1-07-c05-mobile-evidence.md
docs/implementation/day-1-plan.md
docs/quality/traceability.md
docs/runbooks/local-development.md
specs/001-platform-foundation/slices/demo-contract-client/spec.md
specs/001-platform-foundation/slices/demo-contract-client/design.md
specs/001-platform-foundation/slices/demo-contract-client/tasks.md
specs/001-platform-foundation/tasks.md
```

No web/backend/OAS/generated/SDK lock/config/workflow/architecture-rule or
migration change. No app/index/tsconfig/Metro/native settings changed. No backend,
Docker or emulator process/data operation needed. Dist/HBC/maps/node_modules/logs/
private reports remain ignored; no credentials/PII or artifacts staged.
Rollback is consumer-only manifest/lock/fixture/helper patch reversal after review,
not a data/native/schema/cache reset. No rollback/reset executed. No C-05
commit/push/PR/merge; C-04 checkpoint is the only new commit this task.
