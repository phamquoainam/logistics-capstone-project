# Local Development Runbook — repository hiện hữu

## D1-08 Task A - PostgreSQL-aware probes

Supported HTTP runtime profiles: demo (H2) and demo,postgres. Boot-managed
Actuator is explicitly enabled by demo; no Kubernetes detection is needed.
Start the existing runtime using its approved launcher/configuration, then:

```powershell
curl.exe -i http://127.0.0.1:8080/actuator/health/liveness
curl.exe -i http://127.0.0.1:8080/actuator/health/readiness
```

Use the actual configured port. GET needs no token. A healthy response is exactly
{"status":"UP"}; DOWN and OUT_OF_SERVICE map to503, without details/components.
Only those exact GET routes are public: /actuator, aggregate health, db/nested
health, env/beans/metrics and non-GET probe requests are denied. Demo001's existing
/api/demo/v1/health stays constant UP/local-demo and is not dependency readiness.
Default non-demo context does not opt into management/probes; migration job is
non-web. A Flyway startup failure is distinct from runtime DB loss/recovery.

Configured waits: Hikari acquisition2000ms/validation1000ms and PostgreSQL
connectTimeout2s. There is no repository-imposed global socket read timeout.
The platform dbHealthIndicator borrows from the runtime pool and uses JDBC
isValid(1): pinned pgJDBC applies/restores a validation-only socket timeout.
Invalid/unrestored connections are evicted, all borrows closed. No extra pool,
probe executor or business query deadline. H2 uses its native JDBC validation.
These are component timeouts, not a universal HTTP SLA. A readiness503
must not trigger a DB reset/migration repair or make liveness depend on DB.

From repository root after Wrapper builds the current backend JAR:

```powershell
python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
python -B packages/api-client/scripts/check-backend-baselines.py
python -B app/backend/verify-health.py
npm.cmd --prefix packages/api-client run verify:c01
npm.cmd --prefix packages/api-client run test:wire
```

Run Docker harnesses sequentially. Backend regression helper needs free5433;
the health/HTTP harness uses random loopback ports and only a newly owned pinned
PG18.6 tmpfs container. It pauses/unpauses its captured ID, never elogistics_dev
or a developer container, and checks all prior containers/volumes on cleanup.
Do not manually pause a developer database to reproduce this smoke test.
Inherited PG/DEMO/SPRING configuration is cleared by the provisioning harness.
Safe runtime evidence goes under ignored .demo/d1-08-health; reused wire
provisioning keeps private contexts/logs in new ignored .demo/d1-07-c04/run-*.
Share only sanitized evidence, never raw application/Surefire logs or contexts.

Fresh results: architecture23; H2 44 PASS/12 named PG skips; PostgreSQL56/no
skips; four PG safety tests validate long SQL/concurrency/reuse/eviction and
pool-thread cleanup. Guards15; contract41; wire27/108 actual responses. Runtime PG unavailable
returns503, liveness remains200, unpause recovers without backend restart.
[Full Task A evidence/limitations](../implementation/d1-08-health-readiness-evidence.md).
No Redis/RabbitMQ, Testcontainers, CI expansion or full Feature001 closure claim.

## D1-07 C-06 - combined local verification, 2026-10-17

Retain Java21/Boot4.1.1/Maven3.10.0 and Node24.13.0/npm11.6.2 pins. Clean-install
the SDK first and build it before consumer gates. From repository root:

```powershell
npm.cmd --prefix packages/api-client ci --no-audit --no-fund
npm.cmd --prefix packages/api-client run verify:c02
npm.cmd --prefix app/frontend/web ci --ignore-scripts --no-audit
npm.cmd --prefix app/frontend/web run lint
npm.cmd --prefix app/frontend/web run typecheck
npm.cmd --prefix app/frontend/web test
npm.cmd --prefix app/frontend/web run build
npm.cmd --prefix app/frontend/shipper-mobile ci --ignore-scripts --no-audit
npm.cmd --prefix app/frontend/shipper-mobile run lint
npm.cmd --prefix app/frontend/shipper-mobile run typecheck
npm.cmd --prefix app/frontend/shipper-mobile test
node packages/api-client/scripts/check-mobile-consumer.mjs
python -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
python -B packages/api-client/scripts/check-backend-baselines.py
npm.cmd --prefix packages/api-client run test:wire
npm.cmd --prefix packages/api-client run test:web
```

Use the published C-05 starter export command for the unchanged real entry too.
Run Docker harnesses sequentially; their inventory checks include all existing
containers/volumes. Backend helper reuses CI provisioning/report guards and the
existing offline .demo/maven-repository; it requires Docker Engine, approved PG
image locally and free127.0.0.1:5433. If busy, FAIL/BLOCKED: never stop/retarget
existing services or change test URL guards. It creates two owned tmpfs test DBs,
runs architecture/H2/postgres clean verify and removes only its captured ID.
Wire/browser harnesses use random loopback ports. No -v/prune/cache delete/data
reset/Flyway clean/repair. Canonical postgres@sha256 reference retains exactly
the approved18.6-bookworm digest even when the local image loses its tag.

Outputs/context/logs/raw reports stay ignored; share only sanitized allowlisted
evidence. Fresh C-06: package85, web24, mobile1/compiler4/Hermes, arch23,
H2 40+8 skips, PG48+0 and guard14 PASS; wire27/browser7 PASS twice. Audits remain
FAIL/OPEN for SDK/mobile; inspect full and omit-dev separately, no force fix.
[Final evidence](../implementation/d1-07-c06-final-evidence.md). This is cached local
repeatability, not independent clean-machine onboarding. No APK rebuild or mobile
business network execution; remote CI requires separately authorized publication.

## D1-07 C-05 — compile-only mobile SDK / real Hermes bundle, 2026-10-17

Build SDK before the local mobile link is consumed. From repository root:

```powershell
npm.cmd --prefix packages/api-client run verify:c02
Set-Location app/frontend/shipper-mobile
npm.cmd ci --ignore-scripts --no-audit --cache ../../../.demo/npm-cache
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
Set-Location ../../../
node packages/api-client/scripts/check-mobile-consumer.mjs
```

The final script uses actual mobile TypeScript/Expo config and four isolated
negative compiler controls, then installed Expo export:embed on the value-import
fixture type-tests/api-client.ts. It checks actual Hermes header, source-map SDK
modules/public entry and exclusion of generator/validator/Node-only tooling.
It writes a new .demo/d1-07-c05/sdk-<uuid> directory, no cache reset or API request.
Factory requires caller configuration; it is never invoked by starter App/index.
PASS is compile/bundle compatibility only, not mobile networking/device runtime.

Separately verify the unchanged starter using installed Expo CLI:
`expo export --platform android --output-dir <new ignored directory> --source-maps`.
The executed result is576 modules/zero SDK modules; SDK fixture result is23/14.
No App/index/tsconfig/Metro/app.json/native edit, prebuild or APK rebuild needed.

For a new local dependency, use installed Expo install with cache in
npm_config_cache; don't pass `--cache` and its value through Expo CLI extras.
In this execution that produced a mistaken undefined dependency, corrected before
final clean install. Final manifest/lock add only the approved file dependency.
Offline install validation is unreliable and is not Expo Doctor/latest-patch PASS.

Audit full and omit=dev separately; final mobile counts59/35, SDK4-high/0 remain
disclosed. No audit fix/major downgrade. [C-05 evidence](../implementation/d1-07-c05-mobile-evidence.md)
records before/after, current advisories and limits. HBC/maps/logs/reports stay
ignored. C-06 combined/remote gates are not run or closed by this verification.

## D1-07 C-04 — web SDK consumer and isolated browser gate, 2026-10-16

Build the private SDK before web install/build. Node24.13.0/npm11.6.2 remain
required; no version upgrade. From repository root:

```powershell
npm.cmd --prefix packages/api-client ci --no-audit --no-fund
npm.cmd --prefix packages/api-client run verify:c02
Set-Location app/frontend/web
npm.cmd ci --ignore-scripts --no-audit --cache ../../../.demo/npm-cache
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
npm.cmd run build
Set-Location ../../../
npm.cmd --prefix packages/api-client run test:web
```

Browser gate also requires current Maven-verified backend JAR, running Docker,
approved PG18.6 image already available, Python and installed Edge. It creates
fresh owned PostgreSQL tmpfs/backend/Vite with random loopback ports/synthetic
credentials, then seven browser tests (original three +four SDK cases). Only
its own children/container are stopped; existing DB/volumes are never targeted.
Normal Vite proxy/dev config is unchanged; vite.c04.config.ts is test-only.
Private/safe reports and synthetic screenshots stay ignored under
.demo/d1-07-c04/run-*; no traces/raw error values in isolated reporter output.
All cleanup flags must PASS. Do not copy private context or credentials to Git.

The original e2e settings fallback remains for existing manual regressions;
new C-04 runtime tests refuse execution without the isolated harness. Use
test:web for the complete seven-test suite, rather than targeting developer data.
Final web24/browser7 PASS/no skips; package85/wire27 PASS.
[C-04 evidence](../implementation/d1-07-c04-web-evidence.md) distinguishes actual
HTTP/replay from explicit network/nonJSON503/poll403 branch fixtures.
SDK dev audit4 high remains OPEN; web audit0 does not close that finding.
C-05/mobile compile-consumer and final C-06 gates remain unexecuted.

## D1-07 C-03 — real HTTP with owned PostgreSQL, 2026-10-15

Prerequisites: Java21/current Maven-verified backend JAR; Node24.13.0/npm11.6.2,
Python on PATH and running Docker Engine with approved PG18.6 digest available.
Build backend with the existing Wrapper/cache invocation, then from packages/api-client:

```powershell
npm.cmd run verify:c02
npm.cmd run test:wire
```

The harness creates a new labeled PostgreSQL tmpfs container/database/three-role
setup, random loopback DB/backend ports and synthetic passwords, then runs the
real JAR with demo,postgres. It never targets elogistics_dev or native/developer
ports5432/5433, mounts existing data, or invokes Flyway clean/repair. All11 APIs
are called with real HTTP. New process/owned-container cleanup runs in finally;
container IDs/volume names before/after must match. No -v/prune/down is used.
Credentials/context/raw backend log stay ignored under .demo/d1-07-c03/run-*;
do not publish/copy them. Sanitized safe-wire-results/environment/cleanup JSON
are separate. Missing tools/access are failures, never mocked fallback.

Final two runs PASS27/27/no skips,108 HTTP responses each, zero mismatches;
both cleanup and log privacy PASS. Read [C-03 evidence](../implementation/d1-07-c03-wire-evidence.md)
for exact ports/digests, earlier failed attempts, canonical request limitations
and per-operation coverage. Dev audit4 high remains OPEN; C-04–06 not executed.

## D1-07 C-02 — generated package verification, 2026-10-15

From packages/api-client with Node24.13.0/npm11.6.2:

```powershell
npm.cmd ci --no-audit --no-fund
npm.cmd run verify:c02
npm.cmd audit --json
npm.cmd audit --omit=dev --json
```

PASS: strict contract gates, source/two-directory16-file determinism, package
typecheck/build,41+44 tests/no skips. Full dev audit remains FAIL4 high findings;
runtime0, supported checked-object generation avoids installed js-yaml loader.
See [C-02 evidence](../implementation/d1-07-c02-sdk-evidence.md) and
[package commands/policies](../../packages/api-client/README.md). npm audit is
reported separately, never suppressed by verify:c02. After reviewed source/config
changes only, npm.cmd run generate recreates generated source; verification itself
does not overwrite it. C-03 actual HTTP and C-04/05 consumer adoption remain NOT
RUN, no DB/native action required for these commands. Older C-01 notes below
record its earlier minimal graph with audit0 and no generated SDK.

## D1-07 C-01 — contract-only verification, 2026-10-15

From repository root, with the pinned Node24.13.0/npm11.6.2:

```powershell
Set-Location packages/api-client
npm.cmd ci --no-audit --no-fund
npm.cmd run verify:c01
npm.cmd audit --json
npm.cmd audit --omit=dev --json
```

C-01 local result: Redocly2.64.0 errors/warnings/ignored0;11 exact source/OAS
operations;41 tests PASS/no skips,37 negative fixtures; new reported graph audit0.
See [C-01 evidence and limits](../implementation/d1-07-c01-contract-evidence.md)
and [contract notes](../../contracts/openapi/README.md). No backend/database,
Docker, native or frontend change is needed for these commands. Validation
scratch files are ignored under .demo/d1-07-c01; this is not SDK generation.
Owner contract review remains before C-02. C-02–06, actual wire/build/consumer
compatibility and determinism remain NOT RUN/UNVERIFIED; D1-07/Day1 PARTIAL.

The following checkpoints retain their historical execution dates.

**D1-02 latest execution — 2026-10-15:** local toolchain/native starter scope DONE;
Pixel_7/API37 is the current emulator target. See [L.10](#l10-d1-02-toolchain-and-pixel-7-native-execution--2026-10-15)
and [execution evidence](../implementation/d1-02-toolchain-evidence.md) for commands,
23 changed/new files, actual APK/runtime proof and open advisory diagnostics.
Older no-tooling/Pixel7Pro/PARTIAL statuses retain their dated checkpoint scope.
Full Feature001/Day1 are not DONE. No new commit/push/PR performed.

**D1-05 Java repair verified (2026-10-14):** PR #5/fix 6b8f97a có actual GitHub
run 38506000079 PASS: architecture 23, H2 40/tám PG-only skip, PostgreSQL 48/no
skip; artifacts/digest/counts/privacy đã kiểm tra. Setup Java dùng Temurin major
`21`, ghi `java -version`; runner thực tế 21.0.12.1+1, local validation 21.0.10+7.
Các SHAs/Boot/Maven/test criteria/PG setup không đổi. Develop chưa required ba
checks, nên D1-05 PARTIAL. [Exact results và UI next step](../implementation/d1-05-ci-evidence.md#d1-05-b--java-setup-fix-and-actual-hosted-verification).
Các mốc PR/CI bị chặn dưới đây giữ lịch sử; không tự merge hoặc sửa settings.

**D1-05-B hiện hành (2026-10-14):** commit 9194451 đã push feature branch theo
authorization mới; actual CI chưa chạy vì API tạo PR lỗi kết nối. [Manual PR link,
protection audit và completion steps](../implementation/d1-05-ci-evidence.md#d1-05-b--publication-and-acceptance-review).
Develop require PR nhưng chưa required ba CI checks; không tự sửa settings/merge.
Các câu cấm publish của D1-05 ban đầu dưới đây giữ lịch sử trước authorization.

**D1-05 hiện hành (2026-10-14):** D1-03/04 đã merge tại develop/2c609c4.
CI workflow và local Linux verification đã sẵn sàng; actual GitHub Actions chưa
chạy vì task không cho commit/push/PR. [CI guide](../quality/ci-verification.md),
[D1-05 evidence](../implementation/d1-05-ci-evidence.md), mục L.9 bên dưới.
Các câu “chưa có CI/architecture” ở audit cũ là lịch sử, không phải hiện trạng.

**Cập nhật sau Demo-001 / D1-03 (2026-10-13):** PostgreSQL 18.6 dev riêng trên Docker/5433 đã chạy thật; Flyway V1/V2, chín ownership schemas, owner/migrator/runtime và Demo-001 regression PASS. H2 fallback/migration/dữ liệu giữ nguyên. Hướng dẫn hiện hành: [PostgreSQL foundation](postgresql-foundation.md), [Demo local](demo-local.md), [evidence](../implementation/d1-03-postgresql-evidence.md). Các mục baseline bên dưới là lịch sử audit, không phản ánh persistence/POM hiện tại. D1-02/D1-03 và feature 001 chưa DONE.

**Baseline ngày 13/10/2026:** có backend Spring Boot và Expo starter; web độc lập, database/migration và hạ tầng chưa triển khai. Runbook ưu tiên Windows 11 + PowerShell, phân biệt lệnh được repository hỗ trợ với lệnh đã chạy. Chưa có bằng chứng hệ thống end-to-end khởi chạy. Xem [kế hoạch và gap](../implementation/day-1-plan.md).

**Sau toolchain spike T-001-01 / D1-02:** Wrapper Windows/online và offline backend verify PASS, contextLoads chạy thật; versions giữ nguyên theo phê duyệt người dùng. Xem [L.4](#l4-toolchain-spike-t-001-01--d1-02). L.1–L.2 lưu lịch sử audit trước sửa, không mô tả trạng thái hiện tại.

**Environment verification kế tiếp:** Node/npm, mobile typecheck, Metro/status/Android bundle PASS; render Android BLOCKED vì không có device/AVD, dev-build/lint chưa có setup được duyệt. Modulith metadata đã đối chiếu, chưa thêm dependency/chốt architecture tooling. Xem [L.5](#l5-d1-02-nodenpm-expoandroid-và-modulith-verification); D1-02 chưa DONE.

**Android runtime & closure review mới nhất:** AVD `Pixel_7_Pro` và `emulator-5554` đã online/boot hoàn tất. Emulator gọi Metro `/status` HTTP 200 PASS; render BLOCKED vì chưa có Expo Go, không tự cài client. L.5 là lịch sử; [L.6](#l6-d1-02-android-runtime--closure-review) ghi trạng thái mới và từng điều kiện closure. D1-02 vẫn PARTIAL, không gate nào được duyệt DEFERRED.

**Expo Go/runtime smoke sau phê duyệt cài client:** client 57.0.9 đã có sẵn đầu phiên, provenance/signature PASS; screenshot/UI hierarchy xác nhận starter render thật và client kết nối Metro. Log review có non-fatal init findings/emulator UWB-service crash, chưa root cause. [L.7](#l7-d1-02-expo-go-provenance--android-starter-smoke) là trạng thái mới nhất; L.4–L.6 lưu lịch sử. D1-02 vẫn PARTIAL; approval cài/smoke không tự chốt tooling/pin hoặc hoãn gate.

## A. Điều kiện tiên quyết

| Công cụ/thành phần | Căn cứ repository | Yêu cầu và trạng thái |
| --- | --- | --- |
| JDK | [POM](../../app/backend/pom.xml): `java.version=21` | JDK 21; vendor/patch chưa chốt theo DEC-01. Audit dùng Oracle JDK **21.0.10+8-LTS-217**. |
| Maven | [wrapper properties](../../app/backend/.mvn/wrapper/maven-wrapper.properties) | Wrapper **3.3.4**, distribution **3.10.0** giữ nguyên theo phê duyệt. Windows Wrapper và verify PASS tại L.4. Maven hệ thống **3.9.12** chỉ chẩn đoán, không thay gate Wrapper. |
| Spring Boot | POM parent **4.1.1** | Giữ nguyên theo phê duyệt; dependency resolution, package và context smoke PASS với repository/quyền tại L.4. Không suy ra HTTP/DB/business runtime PASS. |
| Node.js | Manifest không `engines`; RN range trong lock; [SDK 57 reference](https://docs.expo.dev/versions/v57.0.0/) yêu cầu Node tối thiểu 22.13.x | **24.13.0** PASS npm/RN/TS và mọi Node engine range trong 499 lock entries. RN range riêng cho phép các nhánh khác nhưng không thay minimum Expo SDK. Project chưa pin Node; DEC-02 còn mở. |
| Package manager | [package-lock.json](../../app/frontend/shipper-mobile/package-lock.json), v3 | npm **11.6.2**, engines `^20.17.0` hoặc `>=22.9.0`, Node hiện tại đáp ứng. Không Bun/yarn/pnpm lock hoặc packageManager pin. `npm.cmd ls --depth=0` PASS; không giữ dependency changes từ lint auto-setup, không chạy npm ci. |
| Mobile stack | [package.json](../../app/frontend/shipper-mobile/package.json), lockfile | Locked Expo **57.0.27**, React/React DOM **19.2.3**, RN **0.86.3**, TS **6.0.3**, RN Web **0.21.4** (manifest `^0.21.2`), `@types/react` **19.2.18**. |
| Docker/Compose | `infra/` chỉ `.gitkeep` | Repo chưa Compose/service/profile/version. Docker không tìm thấy trên PATH máy audit. Chưa cần cho starter; cần khi task hạ tầng được duyệt. |
| PostgreSQL/Redis/RabbitMQ | Không client/adapter/config hoặc Compose | `NOT CONFIGURED`, version chưa chốt. URL template không chứng minh service đang chạy. |
| Android Studio/SDK/emulator | Script `android` = `expo start --android`; không native/Gradle project | SDK/adb/emulator đã kiểm tra tại L.5. Hiện AVD `Pixel_7_Pro`, `emulator-5554` online, boot completed, Android 17/API 37; WHPX usable tại L.6. SDK 57 compile/target API 36, Android 7+ theo reference; native compile chưa chạy. API emulator không phải compile/target SDK của app. |
| Git/PowerShell | Git repository, Windows wrapper/npm scripts | Audit PowerShell **5.1.26100.9549**, Windows 11 amd64. Dùng `npm.cmd` tránh npm.ps1 bị ExecutionPolicy chặn; không cần đổi policy toàn máy. |

## B. Chuẩn bị repository

Mở PowerShell tại checkout của bạn. Ví dụ máy hiện tại:

```powershell
Set-Location -LiteralPath 'D:\TLCN\e-logistic'
$repoRoot = (Get-Location).Path
git status --short
git branch --show-current
git rev-parse HEAD
java -version
node --version
npm.cmd --version
```

`$repoRoot` dùng trong cùng cửa sổ cho các bước sau. Máy mới thay đường dẫn bằng checkout thực tế; repo chưa README clone URL/bootstrap script để suy đoán. Giữ working tree đang thay đổi, không tự stash/reset/checkout đè; chọn branch theo quy trình nhóm, không commit thẳng main.

| Working directory từ root | Nội dung có thể sử dụng |
| --- | --- |
| `app/backend` | POM, `mvnw.cmd`/`mvnw`, application class, context-load test. |
| `app/frontend/shipper-mobile` | npm manifest/lock, Expo scripts, `App.tsx`, TypeScript strict. |
| `app/frontend/web` | Chỉ `.gitkeep`, chưa app executable. |
| `package/api-client`, `infra/compose`, `quality`, `scripts` | Chỉ `.gitkeep`, chưa client/Compose/tooling executable. |

**Dependencies mobile trên máy mới** — chạy khi thiết lập local, chưa chạy trong audit chỉ đọc vì đã có `node_modules`:

```powershell
Set-Location -LiteralPath (Join-Path $repoRoot 'app/frontend/shipper-mobile')
npm.cmd ci
node .\node_modules\typescript\bin\tsc --noEmit --incremental false
```

`npm.cmd ci` cài theo lock và thay thư mục dependencies hiện có. Không dùng `npm install`/`expo install --fix` để tự đổi manifest/lock khi lỗi. Backend resolve dependencies qua Wrapper khi wrapper/network/cache đủ điều kiện; không cài npm packages tại repository root.

## C. Cấu hình môi trường

### C.1. Cấu hình có hiệu lực và template

[application.properties](../../app/backend/src/main/resources/application.properties) chỉ đặt `spring.application.name=backend`. Không `application-local.*`, datasource, server port, Flyway, Redis/RabbitMQ hoặc security config. Không API URL, `process.env`/`fetch`/axios client trong mobile starter. `.env` root không tồn tại lúc audit; không đọc/in/ghi secret thật vào tài liệu.

[.env.example](../../.env.example) là template chưa được backend/mobile/Compose tiêu thụ. Sao chép `.env` không tự cấu hình Spring/service; chưa cần điền credential để chạy starter. Nếu task cấu hình được duyệt cần template, chỉ tạo khi chưa có file:

```powershell
Set-Location -LiteralPath $repoRoot
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
git check-ignore .env
```

Thao tác tùy chọn này chưa chạy trong audit. `.gitignore` có `.env`, `.env.*`, ngoại lệ `.env.example`; file commit chỉ placeholder, không in secret vào console/log/evidence.

### C.2. Biến khai báo trong template

Tất cả biến dưới ở `.env.example` root. **Consumer runtime hiện tại: không có — `NOT CONFIGURED`**. Chưa binding/validation nên không thể khẳng định bắt buộc/tùy chọn ở runtime; cột điều kiện mô tả nhu cầu mục tiêu sau review.

| Biến | Mục đích/thành phần mục tiêu | Điều kiện sử dụng | Mẫu an toàn trong template |
| --- | --- | --- | --- |
| `APP_PROFILE` | Profile backend | Chưa binding; không thay `SPRING_PROFILES_ACTIVE` | `local` |
| `DB_URL` | PostgreSQL backend | Khi có persistence/Flyway config | `jdbc:postgresql://localhost:5432/elogistics` — template, chưa DB thực |
| `DB_USERNAME` | DB user | Khi có DB; runtime/migration quyền riêng cần review | `REPLACE_ME` |
| `DB_PASSWORD` | DB secret | Khi có DB, cấp local | `REPLACE_WITH_LOCAL_SECRET` |
| `REDIS_URL` | Cache | Khi slice dùng Redis | `redis://localhost:6379` — template |
| `RABBITMQ_URL` | Broker | Khi slice có async delivery | `amqp://localhost:5672` — template |
| `RABBITMQ_USERNAME` | Broker user | Khi broker cấu hình | `REPLACE_ME` |
| `RABBITMQ_PASSWORD` | Broker secret | Khi broker cấu hình | `REPLACE_WITH_LOCAL_SECRET` |
| `VNPAY_ENV` | Payment adapter | Chỉ Sandbox P0, adapter chưa có | `sandbox` |
| `VNPAY_TMNCODE` | Merchant code Sandbox | Feature 006, DEC-12 | `REPLACE_WITH_SANDBOX_VALUE` |
| `VNPAY_HASH_SECRET` | Signing secret Sandbox | Feature 006, DEC-12 | `REPLACE_WITH_SANDBOX_SECRET` |
| `MAP_PROVIDER` | Geocoding/matrix adapter | DEC-06/17 `[BLOCKING]` | `UNDECIDED` |
| `POD_STORAGE_PROVIDER` | Private POD storage | DEC-15 `[BLOCKING]` | `UNDECIDED` |
| `TZ` | Timezone môi trường mục tiêu | Chưa loader/wiring, không chứng minh timezone nghiệp vụ | `Asia/Ho_Chi_Minh` |

Backend URL cho web/mobile, auth/session secret, storage credentials và migration-account config: **`NOT CONFIGURED`**, không tự đặt tên biến. Port template không phải port service đã quan sát; backend/Expo URL phải đọc log khi chạy thật.

## D. Khởi động hạ tầng

Không Compose YAML trong repo. `infra/compose/`, `infra/postgres/`, `infra/rabbitmq/`, `infra/observability/` chỉ giữ chỗ; chưa services/profiles/container names/image versions/volumes/healthchecks. Không dùng lệnh cũ trỏ `infra/compose.yaml` hoặc `docker compose up` không file.

Backend chỉ Web MVC nên chưa dependency DB/cache/broker phải chạy trước. Sau task hạ tầng duyệt, bổ sung file Compose thực, service/profile/version/dependency order và health/readiness commands có evidence. PostgreSQL/migration DEC-26 cần review; Redis/RabbitMQ chỉ chạy khi slice dùng cache/async. Repo chưa phương án PostgreSQL native/seed local executable.

## E. Database và Flyway

| Hạng mục | Hiện trạng | Điều kiện có hướng dẫn chạy |
| --- | --- | --- |
| Kết nối local | `NOT CONFIGURED`, không datasource/JDBC/persistence | Binding review, DB/version riêng, credential quyền phù hợp. |
| SQL migration | `app/backend/src/main/resources/db/migration/` chỉ `.gitkeep` | Review/commit version/schema/constraints. |
| Migration execution | `NOT_IMPLEMENTED`, không Flyway dependency/plugin/CLI script | Chốt startup migration hoặc tool thật, cập nhật lệnh theo config. |
| History/status | Chưa DB/history table/công cụ | Migrate thật rồi kiểm tra log/history với schema thực; không giả định bảng có sẵn. |
| DB thử nghiệm | Chưa Testcontainers/test datasource | Test isolation trong task riêng; empty/upgrade/re-run an toàn. |
| Seed | Không SQL/loader/script hoặc fixture accounts | Synthetic seed tách DDL, idempotency/định danh và môi trường cho phép rõ. |

Không dùng `flyway:migrate`/`flyway:info` như goals đang có: POM chưa plugin/config. Không Hibernate `update/create`, sửa migration đã áp dụng, repair mặc định, reset DB/xóa volume. Khi thêm ORM, Flyway quản lý schema và runtime validate theo [ownership strategy](../architecture/data-ownership.md), [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md). Audit không kết nối/thay đổi DB nào.

## F. Phát triển backend

### F.1. Lệnh được cấu trúc Maven hỗ trợ

Tại `app/backend`, terminal riêng khi chạy server:

```powershell
Set-Location -LiteralPath (Join-Path $repoRoot 'app/backend')
.\mvnw.cmd --version
.\mvnw.cmd verify
```

Chỉ tiếp tục khi lệnh trước thành công. `verify` compile/test/package theo Maven/POM; không tự là architecture/integration/migration gate khi tests chưa có. Wrapper Windows đã sửa và kiểm chứng tại L.4.

| Mục đích | Lệnh tại `app/backend` | Phạm vi |
| --- | --- | --- |
| Compile | `.\mvnw.cmd compile` | Không chạy test. |
| Test | `.\mvnw.cmd test` | Chỉ context-load test hiện có. |
| Build đầy đủ | `.\mvnw.cmd verify` | PASS trong spike với explicit local repository; chưa clean clone/máy khác. |
| Chạy starter | `.\mvnw.cmd spring-boot:run` | Spring Boot Maven plugin đã có; audit chưa chạy. |

Fallback chẩn đoán **khi đã có Maven hệ thống**, ghi rõ khác distribution Wrapper:

```powershell
mvn.cmd --version
mvn.cmd -o verify
```

Audit ban đầu dùng Maven hệ thống 3.9.12, offline FAIL do effective cache không có parent. Spike sau sửa dùng Wrapper 3.10.0 và đã verify thành công; không hạ Boot hoặc đổi distribution.

**Cache và quyền truy cập:** `MAVEN_USER_HOME` trong script điều khiển cache distribution của Wrapper; không được coi là đã đổi artifact repository của JVM Maven. Khi debug log cho thấy repository khác cache dự định dùng, xác định đường dẫn từ môi trường local rồi chỉ định cho invocation:

```powershell
# Trong app/backend; biến chỉ áp dụng cho lệnh, không sửa settings.xml.
$taskRepository = Join-Path $env:USERPROFILE '.m2/repository'
.\mvnw.cmd "-Dmaven.repo.local=$taskRepository" verify
# Chỉ dùng offline khi dependencies và plugins đã resolve đầy đủ.
.\mvnw.cmd -o "-Dmaven.repo.local=$taskRepository" verify
```

Đường dẫn trên chọn cache của người dùng hiện tại, không hardcode tên máy/user. Nếu dùng cache khác được phép, đặt `$taskRepository` tương ứng. Cache Maven cần quyền ghi file lock kể cả khi offline; sandbox chặn lock phải được giải quyết bằng quyền/cache phù hợp, không tắt lock hoặc security. Khi cần chẩn đoán dùng `-X ... validate`, chỉ đọc các dòng settings/local repository cần thiết, không công bố toàn bộ debug log có thể chứa cấu hình nhạy cảm.

### F.2. Profile, URL và smoke

Chưa profile `local`, `APP_PROFILE` không consumer. API base URL/port thực `[UNVERIFIED]`; không ghi default URL như đã đo. Sau `spring-boot:run` thành công, đọc server port/context và `Started` trong log, xác nhận đúng process. Không controller nghiệp vụ/static UI; truy cập root chưa chứng minh business API.

Chưa Actuator dependency: `/actuator/health`/readiness **`NOT_IMPLEMENTED`**, không yêu cầu “health xanh” tại baseline. Ghi startup log/URL thực vào evidence, dừng server của mình bằng `Ctrl+C` khi xong.

## G. Phát triển web frontend

`app/frontend/web/` chỉ `.gitkeep`, chưa manifest/lock/Vite/React config/entry/backend URL/dev port. **`NOT_IMPLEMENTED`**; không có lệnh npm ci/dev/build/typecheck/lint/test hợp lệ ở đây.

Script `web` của `shipper-mobile` preview cùng `App.tsx`, không thay web Merchant/Dispatcher/Finance. Sau task web baseline duyệt theo DEC-02/feature 001 mới bổ sung scripts/URL thực. Không tạo web skeleton trong nhiệm vụ tài liệu.

## H. Phát triển ứng dụng shipper mobile

### H.1. Dependencies, typecheck và dev server

Working directory `app/frontend/shipper-mobile`; cài `npm.cmd ci` theo mục B trên máy mới. Với dependencies có sẵn:

```powershell
Set-Location -LiteralPath (Join-Path $repoRoot 'app/frontend/shipper-mobile')
node .\node_modules\typescript\bin\tsc --noEmit --incremental false
npm.cmd run start
```

`start` gọi Expo CLI local. L.5 quan sát Metro/bundle; L.7 đã xác nhận starter render qua Expo Go và client tới `10.0.2.2:8081` trong CI/offline. Máy khác đọc URL/port từ terminal. Để chạy biến thể, dừng server trước rồi chọn **một** script:

| Lệnh manifest | Tác dụng/điều kiện |
| --- | --- |
| `npm.cmd run android` | `expo start --android`, cần emulator/device phù hợp được Expo nhận diện. Không native APK build. |
| `npm.cmd run web` | `expo start --web`, browser preview starter, URL theo terminal. |
| `npm.cmd run ios` | `expo start --ios`, chưa kiểm chứng; không quy trình iOS simulator trên Windows. |

Không script build/lint/typecheck/test, lint config/test framework, `eas.json`, native `android/ios/`. Tsc trực tiếp đã PASS lại tại L.5. Không dùng `npx` tải CLI mới/`expo-doctor` chưa cài như gate hiện hữu. **Không gọi `expo lint` khi thiếu ESLint/config trong scope cấm dependency changes:** CLI có thể tự setup/install ngay cả CI/offline; lần thử ở L.5 đã dừng và khôi phục manifest. Lint vẫn BLOCKED. Khi thay mobile code đọc [AGENTS mobile](../../app/frontend/shipper-mobile/AGENTS.md): Router/EAS trong hướng dẫn chưa phải thành phần đã implement.

### H.2. Emulator, thiết bị và native capabilities

Sau khi Android SDK/AVD máy được chuẩn bị theo target nhóm chốt, mở emulator hoặc kết nối thiết bị thử nghiệm, kiểm tra platform-tools:

```powershell
adb version
adb devices
```

L.5 ghi lần chưa có device/AVD; L.6 kiểm tra lại có emulator nhưng thiếu client. L.7, sau approval cài/smoke, xác minh client Expo Go 57.0.9 đã có sẵn từ trước và khớp release chính thức; starter render/client-to-Metro PASS. Log findings ghi riêng, không tuyên bố runtime sạch hoàn toàn. Không tạo AVD/cài SDK hoặc cài lại client. Đường dẫn SDK/AVD là quan sát, không bắt buộc trên máy khác; thao tác smoke thật nằm tại L.7.

| Khả năng | Bằng chứng/giới hạn |
| --- | --- |
| Backend local | Không API client/base URL; không hướng dẫn localhost/emulator alias như wiring đang có. Cần contract/auth/backend runtime và config thật. |
| Navigation | `index.ts` → `registerRootComponent(App)`, không Expo Router dependency; `src/app/(auth)`/`(work)` chỉ `.gitkeep`. |
| GPS/background | Không location package/call/permission/service; feature location rỗng, chưa latency/device test. |
| Camera/barcode/POD | Không camera/scanner/storage package, permission hoặc upload; scanning/delivery rỗng. |
| SQLite/offline/secure storage | Platform folders chỉ `.gitkeep`, chưa queue/storage/restart/sync test. |
| Expo Go/dev build | Expo Go SDK 57 được duyệt cho starter smoke này, client 57.0.9 PASS tại L.7. Development build khi cần native modules/config ngoài Expo Go còn PROPOSED; chưa dev-client/EAS config/app identifier/native project. Chưa duyệt hoãn foundation development-build feasibility; starter không chứng minh APK/scan/GPS/offline. |

DEC-02/15/16/18/23 còn mở. Chỉ bổ sung permission/config plugins/native build sau review/spike thiết bị; không cài thư viện để “chạy đủ” trong audit.

Native build readiness: SDK API 36/acceleration có sẵn; JDK máy là 21.0.10. [Hướng dẫn Expo Windows](https://docs.expo.dev/workflow/android-studio-emulator/) nêu JDK 17 cho setup native; chưa có Gradle project để kiểm chứng Java 21 với build toolchain thực. Không tự đổi Java của backend hoặc cài JDK. [Development build docs](https://docs.expo.dev/develop/development-builds/introduction/) giải thích `run:android` sẽ prebuild khi thiếu native directories; vì vậy không chạy trong scope này. EAS/dev-client/app ID/build profile và credentials, nếu cần, phải được duyệt riêng.

## I. Kiểm thử và xác minh

`PASS` chỉ đúng phạm vi lệnh; `NOT_RUN` là test có nhưng chưa chạy; `NOT_IMPLEMENTED` là chưa có test/tool.

| Nhóm | Đường dẫn/bằng chứng | Lệnh/điều kiện/thành công | Audit |
| --- | --- | --- | --- |
| Domain unit | Chưa test nghiệp vụ backend/mobile/web | Chưa suite; Maven test/verify chỉ chứng minh BR khi có assertion/test thật và report. | `NOT_IMPLEMENTED` |
| Spring context smoke | `app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java`, `@SpringBootTest contextLoads()` | Backend `.\mvnw.cmd test`/`verify`, cần resolve dependencies; exit 0 và Surefire report test thực chạy. | **PASS** tại L.4: 1 test, 0 failures/errors/skipped; chỉ context bootstrap. |
| DB/API integration | Test domain/support chỉ `.gitkeep`, không Testcontainers | Chưa suite/command riêng; cần PostgreSQL isolation/config/migration và assertion transaction/constraint/auth. | `NOT_IMPLEMENTED` |
| Architecture | Backend test `architecture/.gitkeep` | Chưa ArchUnit/Modulith/tests; compile không thay boundary verification. | `NOT_IMPLEMENTED` |
| Contract/client | Không schema/controller, `package/api-client/.gitkeep` | Chưa validator/generator/contract test command. | `NOT_IMPLEMENTED` |
| Web E2E | `quality/e2e-web/.gitkeep` | Không Playwright config/package/suite, chưa web app. | `NOT_IMPLEMENTED` |
| Migration | Backend `db/migration/.gitkeep` | Chưa Flyway/test DB/SQL, không goal giả định. | `NOT_IMPLEMENTED` |
| Mobile typecheck | `App.tsx`, `index.ts`, `tsconfig.json`, local TS | Mobile `node .\node_modules\typescript\bin\tsc --noEmit --incremental false`, đúng dependencies, exit 0. | **PASS**, không diagnostic; chưa runtime/native proof. |
| Mobile unit/device/offline | `quality/mobile/.gitkeep`, features/platform rỗng | Chưa test framework/harness; startup/permissions/offline cần smoke/evidence thật sau triển khai. | Starter render/client-to-Metro PASS tại L.7; logs PARTIAL với init findings, permissions/offline/dev-build UNVERIFIED. |
| Concurrency/fault/load/RQ | `quality/load/.gitkeep`, `scripts/.gitkeep` | Chưa k6/harness/fault scripts; không benchmark/invariant result giả. | `NOT_IMPLEMENTED` |
| Frontend lint/build | Không web manifest; mobile không lint/build scripts/config | Lint setup chưa được duyệt; tsc/bundle kiểm chứng riêng, không thay APK build. | Lint BLOCKED, native build UNVERIFIED; Android JS bundle PASS. |

CI/CD `.github/workflows/` chỉ `.gitkeep`. Khi thêm suite/CI theo task, ghi evidence/mapping vào [traceability](../quality/traceability.md); phiên này không sửa trạng thái ở file đó.

## J. Xử lý lỗi và phục hồi an toàn

| Triệu chứng | Chẩn đoán/cách xử lý |
| --- | --- |
| `npm.ps1 cannot be loaded` | Dùng `npm.cmd` cùng tham số; đã chạy version thành công. Không đổi ExecutionPolicy toàn máy. |
| Wrapper null-array / `Cannot start maven from wrapper` | Lỗi cũ do `.Target` null ở thư mục cache thường trên PowerShell 5.1. Đã sửa: đọc target đầu tiên qua pipeline, kiểm tra null, fallback cache gốc; thường/khoảng trắng/junction PASS tại L.4. Nếu còn lỗi, xác định checkout/cache thực; không xóa cache/đổi distribution. |
| Offline parent POM `4.1.1` không resolve | Kiểm tra effective repository: sandbox dùng `CodexSandboxOffline`, còn user cache có parent/BOM. Chỉ định cache phù hợp cho invocation; `-o` không tải phần còn thiếu. Không suy ra version sai từ cache miss. |
| Maven không mở được `.locks/...lock` | Cache cần quyền ghi dù offline. Spike default sandbox bị chặn user-cache lock; invocation có quyền phù hợp đã chạy. Không xóa lock của tiến trình khác, sửa global settings hoặc tắt lock để vượt gate. |
| Surefire thiếu POM / `SurefireReportParameters` | Spike offline trước resolve thiếu plugin POM/dependencies, chưa chạy tests. Online verify qua HTTPS tải phần thiếu; offline verify sau đó PASS. Không đổi plugin version hoặc skip tests để che lỗi cache. |
| Java/build dependency lỗi | So JDK 21/Maven version; dừng trước startup nếu build fail. `target/` có sẵn không chứng minh build mới. |
| npm ci/dependency lỗi | So Node range/lock/npm/network/cache. Giữ manifest/lock, không regenerate để che mismatch. Tên package `web` là metadata hiện tại của mobile. |
| Port conflict | Đọc port trong lỗi/log, kiểm tra process LISTEN. Không kill process người khác; dừng server của mình bằng `Ctrl+C`, không tự thêm port config như có sẵn. |
| Health/login/API 404, web không chạy | Chưa Actuator/controller/auth/web manifest; xác định feature chưa implement thay vì debug URL bịa. Startup chỉ chứng minh starter. |
| DB connection/migration lỗi sau khi thêm persistence | Baseline chưa datasource/Flyway. Môi trường đã có dữ liệu: dừng ghi, giữ log/version/schema, backup; không repair/reset/drop/đổi migration đã chạy. Xem [failure recovery](failure-recovery.md). |
| Redis/RabbitMQ connectivity | Chưa client/service/fallback runtime. Khi có config thật, kiểm tra health/credential/outbox; không bỏ DB invariant hoặc xóa message/event để che lỗi. |
| Backend–web/mobile không kết nối | Chưa API client/base URL/web app. Chốt contract/config rồi kiểm tra backend runtime/host/mạng từ thiết bị; không mặc định điện thoại dùng URL laptop. |
| Android không nhận device | Kiểm tra SDK/platform-tools PATH, `adb devices`; giải quyết unauthorized/offline trên thiết bị thử trước chạy script. Không reset emulator data mặc định. |
| Expo CLI báo incompatible host flags | `--offline` và `--localhost` không được dùng cùng nhau trong CLI đã cài. Invocation đầu FAIL do flags; sửa invocation thành `--offline --port 8081`, không sửa build config. |
| Expo lint tự cài dependencies | Thiếu ESLint/config làm CLI tự setup. Không coi CI/offline là bảo đảm read-only. Dừng riêng tiến trình task, khôi phục thay đổi của chính invocation theo snapshot; cần approval setup lint riêng. |
| Scan/GPS/POD/offline chưa hoạt động | Chưa code/dependency/permission; policy/native spike còn `[BLOCKING]`. Preview chạy không chứng minh các khả năng này. |

Xem process đang lắng nghe mà không dừng dịch vụ:

```powershell
Get-NetTCPConnection -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

Lệnh chẩn đoán này chưa chạy trong audit. Mất dữ liệu, payment UNKNOWN, replay/outbox hoặc phục hồi phức tạp theo [failure recovery](failure-recovery.md) và quyền/audit duyệt. Runbook đó là yêu cầu phục hồi mục tiêu, chưa recovery executable. Không reset DB/xóa volume/sửa kỳ CLOSED để setup local.

## K. Checklist local development

Checklist để từng thành viên xác nhận máy của mình; để trống vì chưa có cold-start hoàn chỉnh trên máy mới.

- [ ] Ghi branch/HEAD/dirty state; giữ thay đổi người khác, không commit thẳng main.
- [ ] JDK/Node/npm đáp ứng manifest/lock; version/network/cache được ghi.
- [ ] Wrapper chạy distribution review; backend verify chạy context test thật có report.
- [ ] Mobile dependencies theo lock, typecheck exit 0.
- [ ] Expo render trên target công bố có log/URL thật; Android/device nhận diện nếu dùng native.
- [ ] Phân biệt web placeholder, Expo web preview và shipper workflow chưa implement.
- [ ] Secret local không commit/in log; template env chưa binding vào starter.
- [ ] Nếu slice dùng DB: config/migration/privileges/seed review, DB thử riêng, cold-start/upgrade/re-run PASS.
- [ ] Nếu dùng cache/broker: Compose/services thật, health/fallback/retry evidence; DB bảo vệ invariants.
- [ ] Trước business code: DoR đạt, blocker/contract/auth scope/fixtures chốt, tests cần thiết tồn tại.
- [ ] Ghi PASS/FAIL/NOT_RUN và giới hạn; không khai P0 sẵn sàng từ typecheck/startup.

## L. Verification metadata

### L.1. Mốc và môi trường audit trước spike

| Thuộc tính | Giá trị quan sát |
| --- | --- |
| Ngày kiểm tra | **13/10/2026**, timezone trao đổi Asia/Saigon. |
| Repository/branch/HEAD | `D:\TLCN\e-logistic`, `develop`, `7d0a7fa35d3c60c9fdfeb38fec4d588ff414fdee`. |
| Working tree đầu phiên | Nhiều docs SDD, AGENTS.md, .env.example chưa tracked; bảy `.gitkeep` bị xóa sẵn. Hai file plan/runbook cũng chưa tracked; không diff với HEAD cho chúng. |
| OS/shell | Maven nhận diện Windows 11 amd64, OS build `10.0.26300.0`, PowerShell `5.1.26100.9549`. |
| Tools | Oracle JDK 21.0.10, Node 24.13.0, npm 11.6.2, Maven hệ thống 3.9.12, adb 36.0.2. Docker/psql không trên PATH; không kết luận mọi service ngoài repo không tồn tại. |
| Local sẵn có | Mobile node_modules/backend target có trước audit, `.env` chưa có. Installed Expo/React/RN/TS khớp locked versions nêu trên. Artifact sẵn có không được dùng để khai test PASS. |

### L.2. Lệnh và kết quả audit trước spike

Các bước đọc/audit không in secret, cài dependencies, khởi động services hoặc thay đổi DB.

| Working directory | Lệnh/kiểm tra đã chạy | Kết quả |
| --- | --- | --- |
| Root | `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git ls-files` | **PASS**: dirty tree/branch/HEAD và file tracked; không commit/push. |
| Root | `rg --files --hidden --no-ignore` với loại trừ dependency/build/Git; `rg -n` kiểm tra source; đọc UTF-8 docs/manifests/config | **PASS**: executable/placeholder như baseline, chưa Compose/SQL/contracts/CI executable hoặc DOCX tại repo. |
| Root | `$PSVersionTable.PSVersion.ToString()`, `[System.Environment]::OSVersion.VersionString`, `Get-Command` tools; đọc SDK directory names | **PASS** cho metadata; Docker/psql không trên PATH. |
| Root | `java -version`, `node --version` | **PASS**, version tại L.1. |
| Root | `npm --version` | **FAIL**: npm.ps1 bị ExecutionPolicy chặn, không phải dependency test fail. |
| Root | `npm.cmd --version`, `adb version` | **PASS**, npm 11.6.2/adb 36.0.2. |
| `app/backend` | `.\mvnw.cmd --version` | **FAIL**, exit 1: null-array trong Wrapper, chưa chạy Maven distribution. |
| `app/backend` | `mvn.cmd --version` | **PASS**, Maven 3.9.12/JDK 21.0.10, khác Wrapper distribution 3.10.0. |
| `app/backend` | `mvn.cmd -o verify` | **FAIL**, exit 1: parent `spring-boot-starter-parent:4.1.1` thiếu trong cache, offline không resolve. Chưa compile/package/test. |
| `app/frontend/shipper-mobile` | `node .\node_modules\typescript\bin\tsc --noEmit --incremental false` | **PASS**, exit 0, không diagnostic. |
| Root | Python đọc JSON và SHA-256 snapshot | Đối chiếu manifest/lock/nội dung file. PowerShell ConvertFrom-Json không xử lý key rỗng `packages[""]` trong lockfile trên máy này nên dùng Python. |
| Root | Python kiểm tra headings A–H/A–L, fenced blocks, số cột bảng, whitespace, link/anchor tương đối; đối chiếu SHA-256 trước/sau | **PASS** sau sửa: 69 links/anchors hợp lệ; chỉ hai Markdown được chỉ định đổi trong snapshot 220 file không thuộc Git/dependency/build artifacts. |
| Root | `git diff --check`, `git diff --stat`, `git status --short` | **PASS** cho kiểm tra tracked; bảy `.gitkeep` xóa sẵn vẫn nguyên trạng. Hai Markdown chưa tracked nên được kiểm tra thêm qua snapshot và whitespace validation. |

Vì hai file chưa tracked, `git diff` thông thường không hiển thị nội dung của chúng; validation dùng nội dung file và snapshot SHA-256 trước/sau cùng Git status. Không có Markdown linter được repository cấu hình; kiểm tra tài liệu bằng script chỉ đọc không được coi là lint/build/test feature.

### L.3. Chưa chạy và điều kiện kiểm chứng tiếp

| Bước | Trạng thái/lý do | Điều kiện tiếp tục |
| --- | --- | --- |
| npm ci / clean clone build | `NOT_RUN`: mobile dependencies sẵn; spike verify incremental, chưa thử máy mới | Thiết lập máy mới với artifact access, giữ lock/version và ghi result. Online Wrapper verify đã PASS tại L.4. |
| Backend startup/HTTP/API/health smoke | `[UNVERIFIED]`: verify/context smoke đã PASS; chưa chạy server HTTP, health/business API chưa có | Startup log/endpoint thật trong task runtime được giao. |
| Expo/Android render, web/iOS preview | Metro/status/Android bundle PASS tại L.5; Android render BLOCKED không device/AVD, web/iOS preview chưa chạy | Client SDK 57 và target được chuẩn bị/duyệt; native smoke DEC-02. |
| DB/Flyway/seed/Compose/health | `NOT_IMPLEMENTED`, runtime `[BLOCKING]` bởi config/version/hạ tầng thiếu | Foundation có tool/config thật, DB thử riêng, DEC-26 review. |
| Lint/architecture/contract/integration/E2E/concurrency/native/fault | `NOT_IMPLEMENTED` ngoài context-load; context-load PASS tại L.4 | Suite/tool trong task được giao, chạy/mapping evidence theo DoD. |
| Auth/business/providers | `[BLOCKING]` theo [open questions](../decisions/open-questions.md) | T-001-01 review trước, sau đó T-002-01 auth; contract/domain/provider chốt theo slice. |

Phiên audit ban đầu chỉ cập nhật hai Markdown. Spike sau đó sửa Wrapper theo phạm vi được duyệt, không sửa source nghiệp vụ/manifest/migration/infra/ADR/traceability hoặc database; không commit/push. Phần D1-02 còn lại là native/mobile toolchain evidence, không tái tạo skeleton hoặc tuyên bố P0 sẵn sàng.

### L.4. Toolchain spike T-001-01 / D1-02

**Authorization:** người dùng duyệt giữ Java 21, Boot 4.1.1, Maven distribution 3.10.0; sửa Windows Wrapper và verify backend. Chỉ chỉnh `mvnw.cmd`, day-1 plan và runbook. Milestone backend build/PostgreSQL/Flyway được ưu tiên trước Redis/RabbitMQ; giữ toàn bộ REQ/AC hiện tại. Không ghi decisions là ACCEPTED trong ADR ngoài phạm vi cho phép.

**Snapshot:** 13/10/2026, branch `docs/sdd-baseline-and-runbooks`, HEAD `495813dbd318142f30855451546d313190f11656`, working tree sạch trước sửa. Windows 11/PowerShell 5.1, Oracle Java 21.0.10; Wrapper 3.3.4 chạy Maven 3.10.0. Backend POM và wrapper properties không đổi.

**Root cause/fix:** `.Target[0]` trên cache thường nhận null trước khi gọi Maven. Sửa tối thiểu đọc `(Get-Item -LiteralPath $MAVEN_M2_PATH).Target | Select-Object -First 1`, kiểm tra null rồi chọn cache gốc hoặc junction target. Không regenerate Wrapper/hardcode path. Cache root thông thường có khoảng trắng và cache root junction thật đều chạy Wrapper thành công.

| Evidence ID | Working directory / lệnh hoặc thao tác thực tế | Kết quả |
| --- | --- | --- |
| SPIKE-001-SAFETY | Root: `git status --short`, `git branch --show-current`; SHA-256 snapshot 220 file nguồn/tài liệu ngoài build/dependencies | Tree sạch trước sửa; branch nêu trên; đối chiếu cuối phiên chỉ ba file được phép đổi. |
| SPIKE-001-WRAPPER-BEFORE | Backend: `.\mvnw.cmd --version` trước sửa | FAIL exit 1, `Cannot index into a null array`, chưa gọi Maven. |
| SPIKE-001-WRAPPER-AFTER | Backend: `.\mvnw.cmd --version` sau sửa | PASS exit 0; Maven 3.10.0, Java 21.0.10. Distribution đã có trong cache; chưa thử tải distribution từ cache trống. |
| SPIKE-001-CACHE-NORMAL | Tạo thư mục cache root thường `ordinary cache with spaces` trong temp; junction con `wrapper` trỏ distribution cache sẵn; đặt `MAVEN_USER_HOME` cho invocation; chạy `.\mvnw.cmd --version` | PASS exit 0, root `ReparsePoint=False`, đúng Maven 3.10.0. Kiểm chứng root cache thường, không phải cold download. |
| SPIKE-001-CACHE-JUNCTION | `New-Item -ItemType Junction` tạo cache root `linked cache with spaces` trỏ cache sẵn; đặt `MAVEN_USER_HOME`; chạy `.\mvnw.cmd --version` | PASS exit 0, root `ReparsePoint=True`, Maven home resolve về target. Temp junctions/thư mục rỗng được gỡ riêng, không recursive delete target; env được khôi phục. |
| SPIKE-001-REPOSITORY | Backend: Wrapper `-o -X validate`, rồi `-o -X "-Dmaven.repo.local=$taskRepository" validate` | Default FAIL vì parent không nằm trong effective `C:\Users\CodexSandboxOffline\.m2\repository`. Override chọn `C:\Users\HOAI NAM\.m2\repository`, nhưng sandbox không mở được named-lock file. Các path là quan sát, không pin vào config. |
| SPIKE-001-OFFLINE-BEFORE | Backend: `.\mvnw.cmd -o "-Dmaven.repo.local=$taskRepository" verify` | BLOCKED trong sandbox bởi cache lock; invocation có quyền phù hợp đi tới Surefire nhưng FAIL vì thiếu plugin POM/dependencies, `SurefireReportParameters`; chưa có test result. |
| SPIKE-001-ONLINE | Backend: `.\mvnw.cmd "-Dmaven.repo.local=$taskRepository" verify`, quyền phù hợp cho cache/network | PASS exit 0; bắt đầu `2026-10-13T09:53:22.5748035Z`, kết thúc 16:54:16 +07:00. Tải phần thiếu qua HTTPS Central; không đổi settings/versions/bypass SSL. Warning metadata apache.snapshots timeout, không làm fail build. |
| SPIKE-001-OFFLINE-AFTER | Backend: `.\mvnw.cmd -o "-Dmaven.repo.local=$taskRepository" verify`, quyền phù hợp cho cache | PASS exit 0; bắt đầu `2026-10-13T09:54:48.1515101Z`, kết thúc 16:54:54 +07:00; không cần tải dependencies lần này. |
| SPIKE-001-CONTEXT | Đọc XML/text tại `app/backend/target/surefire-reports/`; so mtime report với thời điểm bắt đầu invocation cuối | PASS: `BackendApplicationTests.contextLoads`, 1 test, 0 failures/errors/skipped. XML mtime `2026-10-13T09:54:53.216811Z`, suite 1.706 s, testcase 0.397 s. Có artifact `target/backend-0.0.1-SNAPSHOT.jar`. |

`$taskRepository` được tính bằng `Join-Path $env:USERPROFILE '.m2/repository'`. Global settings không sửa; Maven chỉ bổ sung cache/metadata/locks và output build. `verify` incremental báo classes up to date rồi chạy tests thật/repackage; chưa phải clean build hoặc cold-start trên máy mới. Mockito/Byte Buddy dynamic-agent warnings xuất hiện với JDK 21, tests vẫn PASS; không đổi JVM flags/dependencies để ẩn warning.

**Surefire evidence:** XML `target/surefire-reports/TEST-com.elogistic.backend.BackendApplicationTests.xml`, text `target/surefire-reports/com.elogistic.backend.BackendApplicationTests.txt`; đây là output sinh bởi Maven, không commit. Test source không đổi. Context-load không chứng minh server HTTP, health/readiness endpoint, DB hoặc business invariants.

| Trace REQ/AC | Đóng góp của spike | Trạng thái sau spike |
| --- | --- | --- |
| REQ-001-01; AC-001-01 | Backend Wrapper build/package/context smoke có bằng chứng | PARTIAL; web/native/Compose/DB/cache/broker/health còn thiếu. |
| REQ-001-05; AC-001-04 | Giữ versions người dùng duyệt; không thêm credential/config hoặc debug endpoint | PARTIAL; chưa secret/config fail-fast gate đầy đủ. |
| AC-001-05 | Ghi versions, effective cache, permissions, commands và report mới | PARTIAL; chưa developer khác/clean clone repeatability. |
| REQ-001-02/03/04; AC-001-02/03 | Không triển khai migration/boundary/contracts | NOT_IMPLEMENTED; không nhận gate từ backend verify. |

**D1-02:** backend toolchain spike hoàn tất, toàn checklist vẫn PARTIAL vì Modulith/Node/npm/native target còn cần review/evidence. D1-03 chỉ phần backend verify đạt; migration vẫn BLOCKED. Không tick T-001-01 hoặc toàn feature DONE; traceability của spike ghi trong hai tài liệu được phép, chưa sửa `docs/quality/traceability.md`/ADR/specs. **Next task:** phần còn lại D1-02 — xác minh Node/npm và Expo/Android runtime/dev-build target; DEC-26 được review trước khi triển khai PostgreSQL/Flyway.

### L.5. D1-02 Node/npm, Expo/Android và Modulith verification

**Snapshot/scope:** 13/10/2026, branch/HEAD như L.4; đầu phiên đã có `mvnw.cmd` và hai Markdown modified từ spike trước. Phiên này chỉ cập nhật hai Markdown, giữ Wrapper/source/build config/dependencies theo SHA-256 đầu phiên. Không npm ci, SDK installation, AVD creation, EAS/prebuild, migration, reset, commit hoặc push. Backend PASS tại L.4 được giữ làm bằng chứng, không chạy lại khi không có backend change.

**Working directory mobile:** `app/frontend/shipper-mobile`, dù package/name/slug là `web`. `index.ts` đăng ký `App`, không Expo Router dependency. CLI thông báo `Using src/app as the root directory for Expo Router` vì có layout directory; thông báo đó không chứng minh navigation đã implement.

| Gate / command thực tế | Kết quả và bằng chứng | Điều kiện còn lại |
| --- | --- | --- |
| Root: `node --version`, `npm.cmd --version` | PASS: 24.13.0 / 11.6.2. Manifest không engines/packageManager; npm lock v3 là package-manager evidence | Pin/accepted configuration DEC-02 còn cần reviewer; không tự thêm config. |
| Mobile: `npm.cmd ls --depth=0`; đọc manifest/lock/installed package metadata | PASS exit 0; toàn bộ 9 direct dependencies/devDependencies khớp lock; Expo 57.0.27, React 19.2.3, RN 0.86.3, RN Web 0.21.4, TS 6.0.3 | Không thay dependencies; clean install trên máy khác chưa kiểm chứng. |
| Mobile: Node stdin script dùng local `semver.satisfies` với npm engines và `packages[*].engines.node` trong lock | PASS: Node đáp ứng npm `^20.17.0` hoặc `>=22.9.0`; 499 lock entries, 0 Node engine mismatch. RN range và TS >=14.17 đáp ứng | Không coi engine check là runtime/native test. |
| Mobile: `node .\node_modules\typescript\bin\tsc --noEmit --incremental false` | PASS exit 0, không diagnostic; chạy lại sau lint cancellation/manifest restore | Không thay lint/unit/device tests. |
| Mobile: local `node .\node_modules\expo\bin\cli --version`; `config --type public --json` | PASS: CLI báo 57.0.28; resolved SDK 57.0.0, platforms ios/android/web; package Expo vẫn 57.0.27. Không android.package/plugins | App identifier/build configuration cần review cho native build. |
| Mobile: local `expo install --check`, CI=1/EXPO_OFFLINE=1 | PASS exit 0, `Dependencies are up to date`; CLI cảnh báo offline dependency validation unreliable. Bundled metadata SDK 57 khớp direct Expo/RN/React packages | Chỉ xác minh offline/local metadata, không khai online validation PASS. |
| Mobile: local `expo lint`, CI=1/EXPO_OFFLINE=1 | BLOCKED: chưa ESLint/config; CLI tự thử setup/install. Đã Ctrl+C, xác nhận không còn owned lint process; manifest phục hồi đúng hash đầu phiên, lock không đổi, ESLint không được cài | Cần duyệt dependency/config lint ở task riêng; không chạy lại auto-setup. |
| Mobile/web lint/test/build scripts | BLOCKED/NOT_IMPLEMENTED: mobile chỉ start/android/ios/web, không test/lint/build; web không manifest | Thêm tooling/web implementation phải được duyệt; không tạo skeleton trong verification. |
| `adb version`, `emulator -version` | PASS: adb 36.0.2, emulator 36.3.10.0; SDK platform 36 và build-tools hiện hữu | Version Studio/AVD/native target chưa pin. |
| `emulator -accel-check` | PASS exit 0: WHPX installed and usable | Chưa chứng minh emulator boot; không có AVD để chạy. |
| `adb devices -l`; `emulator -list-avds`; đọc ANDROID_AVD_HOME | Commands PASS exit 0 nhưng danh sách rỗng; `E:\Android\avd` rỗng | Android runtime gate BLOCKED; kết nối device hoặc chuẩn bị AVD riêng, không reset dữ liệu. |
| Mobile: `npm.cmd run android -- --offline --localhost --port 8081` | FAIL exit 1 do mutually exclusive CLI flags, chưa thử app | Invocation được sửa bên dưới, không đổi config. |
| Mobile: `npm.cmd run android -- --offline --port 8081`, CI=1/EXPO_OFFLINE=1 | BLOCKED, exit 1: `No Android connected device found, and no emulators could be started automatically` | Device/AVD và client phù hợp; chưa kiểm tra Expo Go installed version. |
| Mobile: `npm.cmd run start -- --offline --port 8081`, CI=1/EXPO_OFFLINE=1 | PASS startup, `Waiting on http://localhost:8081`; CI tắt watch/reload | Chưa device-to-host connection; không tự mở firewall/tunnel. |
| Python urllib: GET `http://127.0.0.1:8081/status`; Android manifest GET `/` với headers `expo-platform: android`, `accept: application/expo+json`, `expo-protocol-version: 1`; GET launchAsset URL trả về | PASS HTTP 200; status `packager-status:running`; manifest 200; Android JS bundle 200, chứa starter text; CLI báo 708 modules | Không phải APK/native render; icons/fonts vẫn UNVERIFIED do manifest-assets warning/aborted operation trong offline. |
| Native development build | UNVERIFIED/BLOCKED: chưa expo-dev-client, EAS config, app identifier hoặc native directories; không run:android/prebuild | Duyệt build target/config/dependencies; thực hiện native build smoke với JDK/SDK được chọn. |
| Cleanup/snapshot | PASS: dừng riêng Metro PID 2292/workers sau kiểm tra command line; dừng ADB daemon do task khởi chạy. SHA-256 source/config/lock khớp snapshot trước cập nhật docs | Không dừng process người khác, không xóa SDK/cache/user data; `.expo`/Metro cache là runtime output ignored. |

**Bundle evidence:** tại `2026-10-13T10:20:20.296206Z`, launchAsset là `http://127.0.0.1:8081/index.ts.bundle?platform=android&dev=true&hot=false&lazy=true&transform.engine=hermes&transform.bytecode=1&transform.routerRoot=src%2Fapp&unstable_transformProfile=hermes-stable`; response `application/javascript; charset=UTF-8`, 3,815,166 bytes, SHA-256 `b91487bceea7832ab60f1e06950af8f1e449936150afcadb211794d2ad9dff0c`. Python đọc response trong bộ nhớ, không tạo build artifact/source. Metro/status/manifest/bundle là host-side evidence, không device render evidence.

**Native modules/target:** inventory `expo-module.config.json` có Expo core/status-bar và transitive modules như constants/asset/font/file-system/keep-awake/log-box/dom-webview. Không direct camera/location/SQLite/secure-storage/dev-client/Router; chưa code permissions/native shipper capabilities. Đề xuất Expo Go đúng SDK 57 để smoke starter, development build cho P0 cần cấu hình/native capabilities riêng; không khẳng định mọi SDK library đều cần dev build. Expo Go chỉ kiểm chứng được phần native libraries nó đóng gói; xem [Expo development builds](https://docs.expo.dev/develop/development-builds/introduction/). Cả hai target chưa được chạy trên Android trong phiên này.

**Modulith compatibility assessment — metadata review, không dependency installation/test:** appendix online còn table cũ đến 2.0 snapshot, nên không chọn version chỉ từ page title. Đối chiếu tag POM chính thức:

| Candidate | Metadata chính thức | Kết luận cho Boot 4.1.1 / Java 21 |
| --- | --- | --- |
| [Modulith 2.1.1](https://raw.githubusercontent.com/spring-projects/spring-modulith/2.1.1/pom.xml) | spring-boot.version=4.1.1; compiler source/target=17; archunit.version=1.4.2 | Phù hợp ở mức baseline dependency/bytecode; PROPOSED candidate, repo integration UNVERIFIED. |
| [Modulith 2.0.8](https://raw.githubusercontent.com/spring-projects/spring-modulith/2.0.8/pom.xml) | Boot 4.0.8, source/target 17 | Không phải exact Boot baseline; không chọn thay 2.1 line nếu không spike. Không kết luận chắc chắn incompatible. |
| [Modulith 1.4.13](https://raw.githubusercontent.com/spring-projects/spring-modulith/1.4.13/pom.xml) | Boot 3.5.16/Framework 6.2.19 | Không đề xuất cho baseline Boot 4.1.1/Framework 7. |

Modulith core đã phụ thuộc ArchUnit theo [core POM](https://raw.githubusercontent.com/spring-projects/spring-modulith/2.1.1/spring-modulith-core/pom.xml). [ArchUnit 1.4.2 release](https://github.com/TNG/ArchUnit/releases/tag/v1.4.2) hỗ trợ đến Java 26 class files; Java 21 không có bytecode gap rõ ràng, nhưng chưa chạy architecture tests trong repo.

| Tiêu chí | ArchUnit only | Modulith kết hợp ArchUnit |
| --- | --- | --- |
| Compatibility | Candidate 1.4.2, không gắn Boot runtime; verification trong repo vẫn cần test | Candidate Modulith 2.1.1 + ArchUnit 1.4.2 khớp metadata Boot 4.1.1; cần resolve/integration gate sau duyệt |
| Dependencies/boundaries | Custom package/internal-import/layer/cycle rules; phù hợp [ArchUnit guide](https://www.archunit.org/userguide/html/000_Index.html) | Module model, cycle/internal access/allowed dependencies theo [Modulith verification](https://docs.spring.io/spring-modulith/reference/verification.html), cộng rules riêng |
| Code impact | Test dependency/tests; mapping packages do nhóm định nghĩa | Có thể bắt đầu test scope; cần module detection và named interfaces cho domain.api, có thể thêm package metadata/API dependency sau review |
| Complexity | Ít dependency, phải tự duy trì rules | Thêm BOM/transitives/module conventions; có module integration/docs support |
| Day 1 | PROPOSED để làm architecture gate nhỏ trước; phải có negative fixture và non-vacuous assertions | Có thể triển khai sau mapping; không cần runtime events/actuator/persistence để chỉ verify boundaries |

Không tool nào tự chứng minh database ownership hoặc transaction correctness. Packages rỗng hiện tại không đủ chứng minh nine modules; không coi test xanh trên empty classes là gate. **Đề xuất chưa duyệt:** ArchUnit-only trước, giữ Modulith 2.1.1 là phương án tương thích khi cần module model/integration support. Không thêm BOM/dependencies/runtime starter hoặc tự thay kiến trúc.

**Checklist/trace:** backend PASS từ L.4; Node/npm/dependency engines/typecheck/Expo config/Metro/bundle PASS từ L.5. REQ-001-01 và AC-001-01/05 chỉ PARTIAL vì web/native/cold-start chưa đạt; REQ-001-03 chưa có architecture tests. D1-02 vẫn PARTIAL/BLOCKED ở native/lint/accepted tooling; không tick DONE hoặc sửa feature tasks/ADR/traceability ngoài scope. Để đóng: target Android/client phù hợp, render/dev-build smoke và approval DEC-01/02/tooling, hoặc split gate được duyệt rõ. Để bắt đầu **D1-03** độc lập: review DEC-26 và Docker/PostgreSQL/Flyway/test-DB readiness; chưa chạy migration trước approval.

### L.6. D1-02 Android runtime & closure review

**Snapshot/scope:** 13/10/2026, branch `docs/sdd-baseline-and-runbooks`, HEAD như L.4; đầu phiên đã có Wrapper/hai Markdown modified. Đọc AGENTS root/mobile, DoR, day plan, runbook và feature 001 spec/design/tasks. Chỉ cập nhật hai Markdown; không đổi source/dependencies/lock/config, cài SDK/client, tạo AVD/credentials, prebuild, migrate, reset, commit hoặc push. Backend evidence L.4 và compatibility review L.5 được kế thừa, không báo là chạy lại. L.6 thay trạng thái môi trường mới nhất; L.5 giữ lịch sử lần chưa có target.

#### Android execution evidence

| Lệnh thực tế / working directory | Kết quả thực thi | Gate / điều kiện còn lại |
| --- | --- | --- |
| Root: `git status --short`; `git branch --show-current` | Đúng branch trên, ba file modified từ phiên trước | PASS safety; bảo toàn thay đổi hiện hữu. |
| `adb devices -l`; `emulator -list-avds` | `emulator-5554 device`, model `sdk_gphone16k_x86_64`; AVD `Pixel_7_Pro` | PASS target discovery; emulator có sẵn, không khởi tạo/boot lại. |
| `adb -s emulator-5554 get-state`; `shell getprop sys.boot_completed`; `shell getprop ro.build.version.release`; `shell getprop ro.build.version.sdk` | `device`, `1`, `17`, `37` | PASS boot; Android 17/API 37 chỉ là runtime emulator, không đổi SDK 57 compile/target 36. |
| `emulator -accel-check` | Exit 0, WHPX installed and usable | PASS acceleration check. |
| `adb -s emulator-5554 shell pm list packages host.exp.exponent`; `shell pm path host.exp.exponent`; `shell dumpsys package host.exp.exponent` | Không package/path; dumpsys: `Unable to find package: host.exp.exponent` | BLOCKED client readiness, chưa có installed version để kiểm tra. |
| `adb -s emulator-5554 shell cmd package resolve-activity --brief -a android.intent.action.VIEW -d exp://127.0.0.1:8081` | `No activity found` | Không client xử lý deep link. |
| `adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d exp://127.0.0.1:8081 -p host.exp.exponent` | Exit 1: `Activity not started, unable to resolve Intent` | Launch FAIL do thiếu client; render gate BLOCKED, chưa phải lỗi source render. |
| Mobile: `node .\node_modules\expo\bin\cli config --type public --json`, CI=1/EXPO_OFFLINE=1 | SDK `57.0.0`, entry/manifest giữ nguyên | PASS config resolution. |
| Mobile: `node .\node_modules\typescript\bin\tsc --noEmit --incremental false` | Chạy riêng, exit 0, không diagnostic | PASS typecheck; không thay render/lint/tests. |
| Mobile: `npm.cmd ls --depth=0`; root `node --version`; `npm.cmd --version` | Dependencies như L.5, exit 0; Node 24.13.0/npm 11.6.2 | PASS installed baseline; pin vẫn PROPOSED. |
| Mobile: `npm.cmd run start -- --offline --go --port 8081`, CI=1/EXPO_OFFLINE=1 | Metro chạy, `Waiting on http://localhost:8081`; CI tắt reload/watch | PASS server startup, explicit Expo Go server target; không gọi auto-install Android launcher. |
| Host Python urllib: GET `/status`, Android manifest `/` và launchAsset URL tại `http://127.0.0.1:8081` | HTTP 200, `packager-status:running`, SDK 57.0.0, bundle chứa starter text; Metro log `Android Bundled 3409ms index.ts (706 modules)` | PASS host bundle, không native render. Manifest-assets warning offline còn nguyên, icons/fonts UNVERIFIED. |
| Emulator shell: `printf 'GET /status HTTP/1.1\r\nHost: 10.0.2.2:8081\r\nConnection: close\r\n\r\n'` pipe sang `toybox nc -w 5 -W 10 10.0.2.2 8081` qua ADB shell stdin | Device shell exit 0, `HTTP/1.1 200 OK`, body `packager-status:running` | PASS emulator-to-Metro TCP/HTTP; chưa Expo Go manifest/bundle consumption hoặc UI. Không firewall/tunnel/ADB reverse change. |
| `adb -s emulator-5554 shell pidof host.exp.exponent` | Không PID | Không client process/render log để kết luận UI PASS. |
| Cleanup: Ctrl+C riêng server của phiên; socket probe port 8081; `adb devices -l`; `adb -s emulator-5554 reverse --list` | Metro không còn listen; emulator vẫn online; reverse list rỗng như đầu phiên | PASS cleanup; không dừng emulator/ADB có sẵn, không reset/wipe data. |

**Thời điểm network/bundle evidence:** `2026-10-13T10:52:08.999060+00:00` (17:52:08 +07:00); response từ emulator có Date `Tue, 13 Oct 2026 10:52:09 GMT`. Host Android bundle 3,815,166 bytes, SHA-256 `b91487bceea7832ab60f1e06950af8f1e449936150afcadb211794d2ad9dff0c`, launchAsset URL như L.5. Các response đọc trong memory; không tạo APK/source artifact. Chưa có screenshot/UI hierarchy hoặc React runtime log chứng minh starter render.

#### Expo Go readiness và cách tháo blocker

[Trang Expo Go chính thức](https://expo.dev/go) hiện có SDK 57 cho Android/emulator; [SDK 57 reference](https://docs.expo.dev/versions/v57.0.0/) khớp RN/React baseline tại L.5. Starter chỉ dùng RN View/Text/StyleSheet và Expo StatusBar; Expo Go SDK 57 là target smoke phù hợp ở mức source/docs, **PROPOSED** để chốt baseline. Chưa có client trên emulator nên device compatibility/render vẫn BLOCKED. Không tự cài client; không chạy launcher online có thể download/install hoặc update client.

Hướng dẫn cho người dùng chuẩn bị client, **chưa thực hiện trong phiên này**:

1. Giữ AVD hiện có; không tạo lại hoặc wipe. Cài Expo Go hỗ trợ **SDK 57** từ `expo.dev/go` (chọn Android Emulator) hoặc nguồn chính thức liên kết ở đó. Nếu muốn Codex thực hiện cài, cần phê duyệt riêng thao tác cài client.
2. Kiểm tra `adb -s emulator-5554 shell pm path host.exp.exponent` và `adb -s emulator-5554 shell dumpsys package host.exp.exponent` để ghi versionName/versionCode. Đối chiếu bản client với SDK 57; offline CLI không xác minh version online.
3. Tại `app/frontend/shipper-mobile`, chạy server và mở ứng dụng bằng deep link tới host alias đã đo hoạt động. Serial cần thay theo `adb devices -l` trên máy khác:

```powershell
# Terminal 1, trong working directory mobile; không cài dependencies.
$env:CI = '1'
$env:EXPO_OFFLINE = '1'
npm.cmd run start -- --offline --go --port 8081
# Terminal 2, sau khi client đã được chuẩn bị riêng:
adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d exp://10.0.2.2:8081 -p host.exp.exponent
```

4. Quan sát và chụp màn hình starter có đúng text `Open up App.tsx to start working on your app!`; ghi Metro bundle request, package/client version, foreground activity, logcat theo PID client sau khi chạy. UI không có error screen và log không có lỗi runtime liên quan. Ghi bằng chứng client nhận manifest/bundle thành công; HTTP `/status` một mình không đủ. Chưa chạy các bước này sau cài client.
5. Nếu lỗi mạng, kiểm tra URL mà manifest/launchAsset trả về từ client: địa chỉ phải truy cập được từ emulator. Không suy ra host `127.0.0.1` là loopback của thiết bị. Không tự mở firewall/tunnel hay sửa hệ thống; ghi blocker cụ thể để xử lý. Dừng riêng Metro của phiên khi xong.

#### Remaining decisions — approval review

| Quyết định / phương án | Đã duyệt hay đề xuất | Tác động / điều kiện xác nhận |
| --- | --- | --- |
| Giữ Java 21, Boot 4.1.1, Maven distribution 3.10.0; giữ Modular Monolith; milestones backend/DB trước Redis/RabbitMQ | CONFIRMED theo phê duyệt người dùng ở spike; không sửa acceptance criteria | Không đổi versions/kiến trúc; thứ tự triển khai hạ tầng không miễn AC toàn feature. |
| ArchUnit only cho foundation, candidate 1.4.2; Modulith 2.1.1 giữ làm lựa chọn sau | PROPOSED, chưa duyệt dependencies/config | Ít tác động test scope; phải có negative fixture thật bắt internal import/cycle và rule không pass trên module rỗng. Metadata compatibility review L.5 PASS trong phạm vi phân tích, repo integration UNVERIFIED. Owner/reviewer cần chọn tooling. |
| Expo Go SDK 57 cho starter smoke | PROPOSED baseline target; runtime verification được phép, client installation chưa được phép | Có thể kiểm chứng starter sau chuẩn bị client. Không chứng minh development build/scan/GPS/SQLite/offline. Người dùng chốt target và cách cài. |
| Development build khi cần native modules/config ngoài khả năng Expo Go | PROPOSED hướng feature native | Chưa native build/config/dev-client/JDK-Gradle smoke. Không tự DEFERRED foundation development-build feasibility của design 001; muốn tách milestone phải duyệt scope/task/evidence rõ. Modules đã bundled trong Go không tự động đòi build riêng. Xem [development builds](https://docs.expo.dev/develop/development-builds/introduction/). |
| Pin Node 24.13.0/npm 11.6.2 theo baseline đã chạy | PROPOSED accepted configuration, chưa engines/packageManager/version file | Phù hợp SDK/engines đã đo, không đổi package versions/lock. Owner duyệt pin và cơ chế áp dụng ở task riêng; không lấy trạng thái installed làm ACCEPTED. |
| Hoàn thiện mobile lint và test suite ở task riêng | PROPOSED split, **không DEFERRED** | Cần owner duyệt task/scope/dependencies/test gates; chưa quyết định miễn hoặc hoãn gate D1-02. AGENTS mobile yêu cầu lint/typecheck trước DONE; user scope hiện cấm thêm dependency nên lint BLOCKED. |

#### Closure checklist

| Gate | Phân loại hiện tại | Điều kiện để hoàn tất |
| --- | --- | --- |
| Git safety; Java/Wrapper version/cache paths; online/offline backend verify/contextLoads | DONE/PASS trong scope L.4; safety kiểm tra lại L.6 | Giữ evidence; clean build máy khác vẫn là feature reproducibility/D1-03 gate riêng. |
| Node/npm compatibility, installed/lock, Expo config/dependencies, mobile typecheck | DONE/PASS cho verification commands L.5–L.6 | Accepted stack/pin là quyết định riêng chưa chốt. Offline dependency check không phải online validation. |
| SDK/WHPX, AVD/device discovery và boot | DONE/PASS L.6 | Đã có target, không còn blocker thiếu AVD. |
| Metro Android bundle và emulator-to-development-server HTTP | DONE/PASS L.6 trong phạm vi bundle/network | Chưa chứng minh client tải/chạy bundle. |
| Android starter render và client SDK compatibility thực tế | BLOCKED | Chuẩn bị Expo Go SDK 57, chạy và thu bằng chứng màn hình/runtime/client requests. |
| Node/npm/native target và architecture-tooling decision | PARTIAL / PROPOSED | Người dùng/owner phê duyệt và ghi accepted configuration trong scope được phép. |
| Native development-build feasibility | UNVERIFIED, setup BLOCKED | Duyệt scope/setup hoặc split chính thức; chạy native build smoke, không thay bằng Expo Go render. |
| Mobile lint/test suite, web verification | BLOCKED do tooling/implementation chưa có | Task riêng được duyệt rồi triển khai/kiểm chứng; chưa có PASS và không tự miễn gate. |
| DEFERRED gates | Không có | Chỉ ghi DEFERRED khi người dùng phê duyệt cụ thể gate, milestone thay thế và điều kiện nghiệm thu. |
| **D1-02 closure** | **PARTIAL — chưa đủ DONE** | Gate bắt buộc còn thiếu phải đạt hoặc scope change được duyệt chính thức; user yêu cầu review không phải phê duyệt các đề xuất trên. |

**Trace và D1-03:** REQ-001-01, AC-001-01/05 vẫn PARTIAL; module tests REQ-001-03/AC-001-03 chưa triển khai. Feature 001 vẫn chưa DONE, không sửa spec/design/tasks/ADR/traceability ngoài hai file được phép. D1-03 tổng thể chưa mở như task đã đủ mọi dependency; có thể làm phần chuẩn bị độc lập: review DEC-26 (PostgreSQL version, migration stream/location/execution, owner/runtime accounts, isolated test DB) và Docker/Compose readiness. `Get-Command docker -ErrorAction SilentlyContinue` không tìm executable trên PATH, không chứng minh Docker chưa cài; cần người dùng chuẩn bị/cho phép xử lý trước khi smoke infrastructure. Không tự cài Docker, thêm Flyway/driver, chạy migration hoặc triển khai business feature trong closure review.

### L.7. D1-02 Expo Go provenance & Android starter smoke

**Scope/authorization:** người dùng cho phép cài Expo Go chính thức tương thích SDK 57 và smoke trên emulator hiện tại. Không cho phép thay source/dependencies/lock/config hoặc cài công cụ khác. Branch/HEAD không đổi; đầu phiên ba file modified như L.6, Wrapper được giữ nguyên. Chỉ hai Markdown cập nhật trong repository; screenshot/XML/log/APK pull là evidence tạm ngoài repository, không commit. L.7 là trạng thái mới nhất, L.4–L.6 giữ lịch sử. Không clear logcat/app data, reset/wipe AVD, prebuild, migrate hoặc push.

#### Client provenance / installation verification

Đầu phiên `adb devices -l` thấy `emulator-5554 device`, AVD `Pixel_7_Pro`, boot completed=1. **Expo Go đã có sẵn**, `pm path host.exp.exponent` trả APK; dumpsys versionName **57.0.9**, versionCode **444**, targetSdk=36, firstInstallTime/lastUpdateTime **2026-10-13 18:36:51**. Không gán việc cài trước phiên cho Codex; không cần chạy lại `adb install` hoặc uninstall.

Nguồn xác minh: [Expo Go](https://expo.dev/go), [Expo versions API](https://exp.host/--/api/v2/versions), [release chính thức Expo-Go-57.0.9](https://github.com/expo/expo-go-releases/releases/tag/Expo-Go-57.0.9). API `sdkVersions['57.0.0']` chỉ định `androidClientVersion=57.0.9`, RN 0.86.3/React 19.2.3 và [APK chính thức](https://github.com/expo/expo-go-releases/releases/download/Expo-Go-57.0.9/Expo-Go-57.0.9.apk). Metadata GitHub asset có size **208,916,626 bytes**, digest **sha256:14c18828a3d04e43922245920bd941b978fd1c7a5aec88a0188032f0bb4972ce**. Không lấy trường androidVersion tổng quát của API làm version SDK 57.

| Lệnh/kiểm tra thực tế | Kết quả |
| --- | --- |
| `adb -s emulator-5554 shell pm path host.exp.exponent`; `shell dumpsys package host.exp.exponent` | PASS package đã cài, version như trên. |
| Python urllib GET versions API và GitHub release API `https://api.github.com/repos/expo/expo-go-releases/releases/tags/Expo-Go-57.0.9` | Default sandbox WinError 10013; retry có quyền network được duyệt PASS qua HTTPS, không bypass TLS/security. |
| `adb -s emulator-5554 pull <APK path từ pm path> <temporary installed-expo-go.apk>`; Python hashlib SHA-256 | PASS: file pull đủ 208,916,626 bytes và hash khớp chính xác digest release. Có bằng chứng APK đã cài là artifact chính thức, không chỉ trùng versionName. |
| SDK build-tools 36.1.0 `apksigner.bat verify --verbose --print-certs <temporary installed-expo-go.apk>` | PASS exit 0, `Verifies`, signature v2=true; certificate SHA-256 `9eb6b9631cecbbd864ef607826e0a30f673dc64cf8fb5753263ecaae4bc50fae`. Dùng công cụ có sẵn, không cài signer/SDK. |
| Cài mới/cài lại Expo Go | NOT_REQUIRED: client có sẵn, artifact/version/signature phù hợp. Installation readiness PASS; không báo đã thực hiện installation mới. |

#### Android render / connection evidence

Working directory `app/frontend/shipper-mobile`; script thực tế và launch:

```powershell
$env:CI = '1'
$env:EXPO_OFFLINE = '1'
npm.cmd run start -- --offline --go --port 8081
# Sau Metro startup, mở client đã được xác minh; không cài package:
adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d exp://10.0.2.2:8081 -p host.exp.exponent
```

| Verification command / bằng chứng | Trạng thái và phạm vi |
| --- | --- |
| Host GET `http://127.0.0.1:8081/status` trước launch và lúc kiểm tra cuối | PASS, `packager-status:running`; chỉ server status. |
| Deep-link command trên | PASS exit 0, Status=ok, LaunchState=WARM, ExperienceActivity, TotalTime=596ms. Launch UTC `2026-10-13T11:41:56.295245+00:00`. |
| Metro terminal sau client launch | PASS: `Android Bundled 657ms index.ts (708 modules)`; không fetch Android bundle từ host để thay client smoke trong phiên này. |
| PID logcat `ReactNativeJS: Running "main"` | PASS: initialUri `exp://10.0.2.2:8081`, runtimeVersion `exposdk:57.0.0`, launchAsset `http://10.0.2.2:8081/index.ts.bundle?...`, hostUri/debuggerHost 10.0.2.2:8081, fabric=true. Chứng minh client nhận manifest và chạy JS. |
| `adb -s emulator-5554 exec-out screencap -p`; `shell uiautomator dump /data/local/tmp/elogistic-d1-02-go-ui.xml`; đọc XML | PASS render: screenshot quan sát trực tiếp và TextView thuộc `host.exp.exponent` đều có `Open up App.tsx to start working on your app!`, bounds `[221,1527][1219,1593]`. Không chỉ splash hoặc HTTP 200. |
| `shell pidof host.exp.exponent`; `shell dumpsys activity activities`; `shell toybox netstat -tn` | PASS duy trì smoke: PID **13000** giữ nguyên, ExperienceActivity foreground; 5 sockets ESTABLISHED tới 10.0.2.2:8081 lúc UTC **11:45:52.564657**, gần 4 phút sau launch. Không suy ra scan/GPS/offline từ test này. |
| `node .\node_modules\typescript\bin\tsc --noEmit --incremental false` | PASS exit 0, không diagnostic; không chạy `expo lint` vì tooling còn thiếu, tránh auto-install ngoài scope. |
| PID-filtered logcat và crash buffer, từ device time `10-13 18:41:55.000` | Đã kiểm tra; findings bên dưới. Không tuyên bố runtime hoàn toàn sạch exceptions. |

**Evidence artifacts tạm:** `C:\Users\HOAI NAM\AppData\Local\Temp\elogistic-d1-02-expo-go-8y43zubz\`: `installed-expo-go.apk`, `starter-rendered.png`, `starter-rendered.xml`, `expo-go-runtime.log`. Screenshot final SHA-256 **43e90b5795bf767f126670568ae256d7ab385c7714e4bc1d497d42c5b61985df**, UI/log snapshot UTC **11:45:11.016763**. Screenshot đầu `starter-initial.png` là splash, không dùng làm bằng chứng render; screenshot final và UI hierarchy mới xác nhận gate. Temp có thể bị OS cleanup; command/hash/text/time trong runbook là record bền vững. Không tạo file evidence mới trong tracked repository.

#### Runtime log findings / giới hạn

Lệnh thực tế: `adb -s emulator-5554 logcat -d -T '10-13 18:41:55.000' --pid=13000` và `logcat -b crash -d -T '10-13 18:41:55.000'`, không xóa buffer. PID log snapshot có 337 lines. Không thấy Expo Go FATAL/ANR hoặc JS fatal trong observation window; process vẫn ở foreground và text starter render. Tuy nhiên có các dòng error/exception trong quá trình init:

- `UpdateFailedToLoad`: failed to launch embedded/launchable update, trước log Running main.
- `NoSuchFieldException` với RecentTaskInfo.numActivities; TaskDescription primary color should be opaque.
- ReactNoCrashSoftException/UIManager chưa có active React instance trong native-module init.
- Hai dòng `Cannot initialize app loader` cho ExpoHeadlessAppLoader.

Các finding không chặn starter render trong window này; **root cause và tác động native/background feature UNVERIFIED**, không tự gán lỗi cho App.tsx hoặc tự sửa versions/config. **Log health PARTIAL**, không có kết luận zero-runtime-exception PASS. Crash buffer chỉ thấy `/vendor/bin/hw/android.hardware.uwb-service /dev/uwb0` lặp SIGABRT với missing serial device; không có host.exp.exponent trong crash record. Đây là process hệ thống emulator, cần review môi trường riêng nếu yêu cầu emulator sạch lỗi; không tắt service/reset/cài SDK để che log.

#### Proposed decisions / remaining tasks

| Nội dung đề nghị owner phê duyệt | Trạng thái | Task/gate sau duyệt |
| --- | --- | --- |
| ArchUnit-only, test-scope candidate 1.4.2 cho foundation; chưa thêm Modulith runtime | PROPOSED; compatibility analysis L.5 có evidence, integration chưa chạy | Review package mapping, thêm rules internal-import/cycle và negative fixture khiến verify fail khi vi phạm, pass khi bỏ vi phạm; không test rỗng. Tooling selection thuộc D1-02, automated architecture gate thuộc D1-04/REQ-001-03/AC-001-03. |
| Expo Go SDK 57 cho starter smoke; client 57.0.9 đã kiểm chứng | CONFIRMED trong phạm vi authorization cài/smoke SDK 57 của người dùng; không suy ra đã duyệt native-build milestone hoặc toàn DEC-02 | Accepted spike target/evidence ghi tại L.7, không yêu cầu duyệt lại cùng thao tác. Không đổi app Expo 57.0.27/RN/React/lock hoặc tự pin các version khác. |
| Development build bắt buộc trước nghiệm thu feature cần native modules/config không có trong Expo Go; foundation build-feasibility vẫn giữ gate | PROPOSED, chưa DEFERRED | Task native spike được duyệt: app ID/config/dev-client hoặc build mode, JDK/Gradle compatibility và APK smoke. Chưa run:android/prebuild; muốn tách milestone cần chỉ định owner, task và gate thay thế, không miễn feature 001 native feasibility. |
| Pin Node 24.13.0/npm 11.6.2 theo baseline đã chạy | PROPOSED, chưa accepted configuration | Task pin qua version file và manifest metadata phù hợp, kiểm chứng clean install/build trên môi trường công bố; source/config edits cần approval riêng. Không tự chọn phiên bản mới. |
| Mobile lint/test suite thành task tooling riêng; web baseline/build thành task riêng | PROPOSED split, chưa DEFERRED | Duyệt scope/dependencies; ESLint phù hợp Expo SDK 57, test harness có assertions cần thiết theo slice, chạy lint/test thật; web chưa manifest. Không tạo skeleton hoặc fake business tests trong smoke. |
| Xử lý/review client-init và emulator UWB findings | OPEN / UNVERIFIED root cause | Ghi disposition có evidence/owner; nếu cần đổi SDK/AVD/client/config phải duyệt riêng. Không lấy UI PASS để xóa findings. |

#### D1-02 closure status

- **DONE/PASS trong scope:** safety, backend toolchain/context từ L.4; Node/npm/dependencies/typecheck, Expo config/SDK/Metro/bundle; AVD boot; client package/provenance/signature; Android starter render và client-to-Metro trong observation window.
- **PARTIAL:** log health với init/emulator findings; DEC-01/02 toolchain/accepted configuration chưa chốt hết. Maven/Java/Boot giữ nguyên đã được duyệt, không suy ra architecture/mobile choices tự CONFIRMED.
- **UNVERIFIED:** native development-build feasibility, clean reproducibility trên máy khác, feature native permissions/background/offline. Các feature capabilities chưa triển khai không phải business smoke đã đạt.
- **BLOCKED:** tooling/config cho lint/mobile tests, web implementation, architecture-tooling integration sau lựa chọn. Không thể triển khai khi scope hiện chỉ cho docs và Expo Go runtime.
- **DEFERRED:** chưa có; đề xuất tách milestone không phải quyết định hoãn đã được duyệt.

**D1-02 vẫn PARTIAL, không tick DONE.** Để đóng cần: owner chốt baseline/tooling/pin và disposition log findings; các gate bắt buộc thiếu phải được kiểm chứng hoặc split milestone được duyệt chính thức với task/owner/gate cụ thể. Starter render không thay AC-001-01 cold start/web/native spike, AC-001-03 boundary tests hay AC-001-05 reproducibility. Không sửa acceptance criteria/spec/tasks/ADR/traceability ngoài scope; feature 001/T-001-01 chưa DONE.

**D1-03 tiếp theo:** phần chuẩn bị độc lập vẫn là review DEC-26 + Docker/Compose/PostgreSQL/Flyway/test-DB readiness (migration stream/location/execution, ownership/accounts, isolated DB). Backend verify đã có PASS; migration chưa triển khai/chạy. D1-03 tổng thể vẫn phụ thuộc D1-02 và DB decisions; không tự cài Docker/Flyway hoặc chạy migration trên dữ liệu hiện hữu. Sau review/approval mới đề xuất diff hạ tầng tối thiểu.

**Cleanup:** Ctrl+C dừng riêng Metro do phiên khởi chạy, xác nhận port 8081 không còn listen. Emulator/ADB/client có sẵn được giữ; reverse `host-15 tcp:8081 tcp:8081` có từ đầu phiên vẫn nguyên, không tạo/xóa mapping. Bằng chứng kết nối PASS áp dụng lúc Metro đang chạy; không tuyên bố kết nối còn hoạt động sau cleanup. Không xóa cache/log/app data hoặc temporary evidence.

## L.8. D1-04 architecture verification — 2026-10-13/14

Current branch `feature/d1-04-architecture-verification`, baseline merged develop
`ccafeb4`. [Mapping/evidence](../implementation/d1-04-architecture-evidence.md),
[approved decision](../architecture/decisions/ADR-0004-architecture-verification.md).
Earlier L.4–L.7 “architecture not implemented/web absent” statements are historical.

From `app/backend`, use the existing project-local Maven cache:

```powershell
$architectureRoot = (Resolve-Path ../..).Path
$architectureCache = Join-Path $architectureRoot '.demo/maven-repository'
.\mvnw.cmd -B -ntp "-Dmaven.repo.local=$architectureCache" -Ddebug=false -Dtrace=false clean verify
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$architectureCache" '-Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests' test
.\mvnw.cmd -o -B -ntp "-Dmaven.repo.local=$architectureCache" -Ddebug=false -Dtrace=false verify
```

Online clean verify resolves the clean plugin if absent; offline failure due to
uncached plugin is a cache/resolution issue. No cache deletion/global settings or
security bypass. Core ArchUnit 1.5.1 is test scope, plain Jupiter uses existing
Surefire. Full local tests need no new JUnit engine or Spring Modulith.
Final H2 verification: 48 discovered, 40 executed/pass, eight PG-only skips.

PostgreSQL regression from repository root, with the existing D1-03 container
healthy (do not reset/recreate/prune volume):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File app/backend/verify-postgres.ps1 -Offline
```

Script provisions fresh isolated test/upgrade databases, never dev/native DB;
retains them. Final PG: 48 PASS, zero fail/error/skip. Architecture: 11 production
and 12 fixture tests; 37 production classes, exact four implemented package groups,
nonempty layer/controller scopes. Eleven fixture violations detected plus one
allowed actor dependency. Fixtures are excluded from production importer/artifact;
no suppression. Missing six business modules are not boundary-verified.

Trusted actor has only id/enabled/role/merchantId. Existing security context validates
signed token and current account status/DB-derived role/merchant. Orders receives
AuthenticatedActor via AuthenticationPrincipal and cannot use AccountView/other
Identity APIs or internals. Domain remains Identity-independent. Tests verify
live merchant ownership, forged-owner rejection, existing RBAC and unchanged JSON.

Security evidence: captured demo application output excludes credentials/bearer;
active demo TokenService fails missing/weak config and starts with valid config.
Bounded secret audit checks Git-visible files/artifact against generated private
values and selected key/token patterns; no full-history/provider security claim.

Browser regression used `run-postgres-demo.ps1 -RuntimeOnly`, existing Vite script,
then `npm.cmd run test:e2e` in `app/frontend/web`: three installed Edge tests PASS;
web typecheck/build PASS. Runtime-only does not migrate/reseed dev data. Browser
fixtures add synthetic demo drafts; existing data/receipts are preserved.

D1-04 local verification: propose DONE; D1-03 local DB milestone PASS/overall PARTIAL;
D1-02/native/lint/pin/log disposition, independent reproduction, CI/OpenAPI/full
foundation still open. No AC change or implicit deferral. Next independent slice:
D1-05 executable CI architecture gate with actual clean-runner evidence.

## L.9. D1-05 — Linux CI gate và remote verification

**Official closure, 2026-10-14:** D1-05 DONE. User merged PR #5 into
`develop/f50c897`; actual [push run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
PASS: Architecture gate 23/no skip, Backend default / H2 40/eight PG-only skips,
Backend PostgreSQL 18 48/no skip; guard 14 per job. Sanitized artifacts/digests
and effective required checks verified read-only. Active ruleset25049940/develop,
three GitHub Actions contexts, strict policy/no bypass actors.
[D1-05 evidence](../implementation/d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).
D1-03 approved PostgreSQL task DONE; D1-02/full foundation still PARTIAL.
Earlier L.4–L.8 states and initial local simulation results retain history.

Lệnh CI chạy trong runner/container Linux riêng từ repository root:

```bash
python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v
bash scripts/ci/run-backend.sh architecture
# Các mode dùng runner/temp directory riêng, không chia sẻ Maven home:
bash scripts/ci/run-backend.sh h2
python3 -B scripts/ci/prepare-postgres.py start --directory "$RUNNER_TEMP/d1-05-postgres"
bash scripts/ci/run-backend.sh postgres
python3 -B scripts/ci/prepare-postgres.py cleanup --directory "$RUNNER_TEMP/d1-05-postgres"
```

Chỉ chạy provisioning CI trên runner test riêng có port 5433 trống; không chạy
host mode này trên máy dev đang có PostgreSQL development/5433. Local Windows
simulation dùng `--network-container` trỏ tới **container runner mới có CI label**,
PG chạy trong loopback riêng của runner, không bind host port. Mỗi PG test attempt
phải tạo DB mới: rerun toàn suite trên fixture cũ gây failure, không reset DB để
che lỗi. Helper cleanup chỉ xóa đúng container tạm có name/ownership label đã
ghi; không xóa volume, dev data, H2, native PostgreSQL hoặc Maven cache máy dev.

Maven Wrapper Unix gọi qua Bash; chưa dùng Windows PowerShell/cmd scripts trên
Linux. Không copy `.demo`, `.m2`, target, private settings/env hay volumes vào
runner. Java21/Boot4.1.1/Maven3.10.0/FlywayV1V2 không đổi. CI settings tối thiểu
và repository mới được truyền explicit; thiếu reports/test hoặc Maven failure
đều làm job FAIL. Không có skipTests, suppression hay continue-on-error.

Artifacts chỉ `summary.json` và `tests.xml` đã lọc theo inventory test được duyệt;
không properties/system output/failure bodies. Raw Maven/Surefire output có thể
chứa token khi MockMvc test thất bại: không upload hoặc paste lên GitHub.
Local evidence giữ tại `.demo/d1-05/evidence/`; provisioning env/state private
trong `.demo/d1-05/postgres-private*`, không commit.

**Closure:** D1-05 DONE for approved backend CI, actual PR/develop verification
and required-check enforcement. Documentation-only closure branch starts from
clean develop/f50c897; no new commit/push/PR/merge authorized.
Next: D1-02 Closure decision gate for pin/tooling/native strategy; mobile/native/
lint/log/onboarding requirements remain open, not DEFERRED.
[Closure plan](../implementation/day-1-plan.md#i-d1-02-closure-plan-sau-d1-05--proposed).
Không tự triển khai D1-06 hoặc thay đổi full feature AC để bỏ qua gate này.

## L.10. D1-02 toolchain and Pixel 7 native execution — 2026-10-15

**Current D1-02: DONE for approved local tooling/native starter feasibility.**
Earlier L.4–L.9 pending tooling/Pixel7Pro statements retain their dated scope.
Current target is **Pixel_7/API37**, emulator-5554, not Pixel7Pro.
[Commands/results, APK/screenshot hashes and finding disposition](../implementation/d1-02-toolchain-evidence.md).
Full Feature001/Day1 remain PARTIAL; native starter is not scan/GPS/offline,
production release or other-developer onboarding acceptance.

### Version pins and quality commands

Use Node24.13.0/npm11.6.2. Root .nvmrc, exact engines/packageManager in both
frontend manifests and project engine-strict .npmrc enforce the declared baseline
for npm operations. No global settings/version changes were made. Existing Expo
57.0.27/React19.2.3/RN0.86.3, Vite8.3.4, TS6.0.3, Java21/Boot4.1.1/Maven3.10.0
and database foundation remain unchanged.

`.nvmrc` declares a version; it does not automatically switch the active Node
installation on Windows. If NVM for Windows is already installed and this Node
version is available, explicitly run `nvm use 24.13.0`, then check `node --version`
and `npm.cmd --version` in the shell used for the project. Otherwise select the
approved existing installation. Stop on a version mismatch; do not assume
`packageManager` installs npm or changes PATH. No version manager/global npm
installation is part of these verification commands.

From app/frontend/web, then separately app/frontend/shipper-mobile:

```powershell
node --version
npm.cmd --version
npm.cmd config get engine-strict
npm.cmd ci --no-audit --no-fund
npm.cmd run lint
npm.cmd run typecheck
npm.cmd test
# Web only:
npm.cmd run build
# Mobile Android JS export only; this does not build an APK:
npm.cmd run build:android:bundle
```

Web tests use existing Playwright as a browserless HTTP-client test runner:
five meaningful cases, no developer credentials/backend requirement. Mobile uses
SDK57 ESLint/Jest preset and RNTL14, one real starter mount/remount test. Native
behavior is verified separately below, not by Jest's mocked native modules.
Both clean install/lint/typecheck/test gates PASS. Web build PASS; existing three
Edge browser regressions PASS against a separate new in-memory H2 backend.
Run test:e2e only when the appropriate test backend/Vite and ignored credentials
are prepared; do not run regression provisioning/reset against developer data.

### Native development build versus Expo Go

The project now includes SDK-compatible expo-dev-client57.0.20. app.json defines
development-only package com.elogistic.shipper.dev and scheme elogistics-shipper-dev.
Generated native folders remain ignored under the original .gitignore. No manual
native edits, EAS login/credentials, release keys or AVD recreation are required.

Actual Windows first-generation/build commands from shipper-mobile:

```powershell
adb devices -l
emulator -list-avds
adb -s emulator-5554 emu avd name
# Only for a clone with no android directory; preserve existing native files.
node node_modules/expo/bin/cli prebuild --platform android --no-install --no-clean --skip-dependency-update 'react,react-native'
$toolchainRepoRoot = (Resolve-Path ../../..).Path
$env:GRADLE_USER_HOME = Join-Path $toolchainRepoRoot '.demo/d1-02/gradle-home'
$env:NODE_ENV = 'development'
.\android\gradlew.bat -p android :app:assembleDebug --no-daemon --console=plain -PreactNativeArchitectures=x86_64 --max-workers=2
```

Installed SDK36/BuildTools36.0.0/NDK27.1/CMake3.22.1 and existing JDK21 were used.
Generated Gradle9.3.1 build PASS in6m58s, 304 tasks. x86_64 targets Pixel7 emulator;
this is not ARM/release/iOS coverage. Set NODE_ENV for subsequent direct Gradle
calls; the first successful run warned when it was absent. No cache was deleted.
Use quoted JVM properties or --max-workers: PowerShell splits unquoted dotted
-D arguments. The initial invocation failure is retained in evidence.

APK android/app/build/outputs/apk/debug/app-debug.apk:63,488,363 bytes,
debug signature verified. Package was absent before `adb install`; install and
MainActivity cold launch PASS. Existing installations/data were not replaced.
`npm run android:dev -- --device emulator-5554` is the explicit Expo native-build
target; start:go/start:dev separately choose the JavaScript development client.

For this emulator, actual working Metro setup is:

```powershell
$env:REACT_NATIVE_PACKAGER_HOSTNAME = '10.0.2.2'
npm.cmd run start:dev -- --lan --port 8081
# In another shell after installation:
adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d 'elogistics-shipper-dev://expo-development-client/?url=http%3A%2F%2F10.0.2.2%3A8081' com.elogistic.shipper.dev
# Separate Expo Go comparison, not the native APK target:
npm.cmd run start:go -- --lan --port 8082
adb -s emulator-5554 shell am start -W -a android.intent.action.VIEW -d exp://10.0.2.2:8082 host.exp.exponent
```

--localhost bound only ::1 on this host; 127.0.0.1/10.0.2.2 connections failed.
LAN binding and process-only advertised hostname fixed manifest/asset routing.
No firewall/global configuration change or adb-reverse replacement was made.
Do not count HTTP200, APK installation or developer menu as starter render.
Dismiss first-run dev-menu onboarding; UI hierarchy/screenshot must show starter
text in com.elogistic.shipper.dev. Both native cold launches PASS with actual
starter, Metro sockets and bounded no-app-fatal/ANR/JS-fatal evidence.

### Findings and remaining advisory work

Go57.0.9 starter comparison PASS on Pixel7 after routing fix. Its embedded-update
fallback/headless-loader/UIManager/RecentTaskInfo/TaskDescription errors reproduce
in the Go process. Native APK does not reproduce update/headless errors. It has
a caught DevLauncher colorScheme reflection warning in both launches and one
RN ReactHost focus-before-context soft exception on the first launch; second
launch renders with no such soft exception. SDK/RN source identifies both paths;
no app JS stack/fatal. UWB panic concerns Android vendor service /dev/uwb0 before
app launch; do not attribute it to App.tsx or disable/wipe the emulator to hide it.
System lastanr reports no ANR since boot; native exit-info only records intentional
force-stop between cold launches.

Online expo install --check still FAILS its latest-patch recommendation
(57.0.29/Metro57.0.17 versus retained57.0.27/57.0.16). Offline check warns it is
unreliable and does not supersede this result. Baseline engine/peer/compile/
quality/runtime gates PASS; no approved requirement demanded automatic upgrades
to the latest patch. npm graph audits/deprecations and upstream warning fixes
remain OPEN maintenance/security review, not a clean audit or full native-feature
readiness claim. Do not run audit-fix/force or framework downgrade to hide findings.

Verification backend/Vite/Go Metro were stopped after use, preserving original
databases, caches, emulator and existing adb reverse mapping. Native Metro8081
remains running for inspection of the installed development app; observation
results are bounded by recorded timestamps, not perpetual connectivity promises.
