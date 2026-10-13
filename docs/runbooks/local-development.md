# Local Development Runbook — repository hiện hữu

**Baseline ngày 13/10/2026:** có backend Spring Boot và Expo starter; web độc lập, database/migration và hạ tầng chưa triển khai. Runbook ưu tiên Windows 11 + PowerShell, phân biệt lệnh được repository hỗ trợ với lệnh đã chạy. Chưa có bằng chứng hệ thống end-to-end khởi chạy. Xem [kế hoạch và gap](../implementation/day-1-plan.md).

## A. Điều kiện tiên quyết

| Công cụ/thành phần | Căn cứ repository | Yêu cầu và trạng thái |
| --- | --- | --- |
| JDK | [POM](../../app/backend/pom.xml): `java.version=21` | JDK 21; vendor/patch chưa chốt theo DEC-01. Audit dùng Oracle JDK **21.0.10+8-LTS-217**. |
| Maven | [wrapper properties](../../app/backend/.mvn/wrapper/maven-wrapper.properties) | Wrapper **3.3.4**, distribution **3.10.0**. Không Gradle. Wrapper hiện FAIL; Maven hệ thống **3.9.12** chỉ fallback chẩn đoán đã quan sát, không thay baseline khai báo. |
| Spring Boot | POM parent **4.1.1** | Phiên bản khai báo; dependency resolution/build còn `[UNVERIFIED]`. Cần cache hoặc quyền truy cập artifact repository phù hợp. |
| Node.js | Manifest mobile không `engines`; lockfile RN 0.86.3 có `engines.node` | Các nhánh range từ lock: `^20.19.4`, `^22.13.0`, `^24.3.0` hoặc `>=25.0.0`; project chưa pin Node. Audit **24.13.0**. DEC-02 còn mở. |
| Package manager | [package-lock.json](../../app/frontend/shipper-mobile/package-lock.json), v3 | Dùng npm; không root workspace manifest/lock hoặc Bun/yarn/pnpm lock. Audit npm **11.6.2**; project chưa pin npm. |
| Mobile stack | [package.json](../../app/frontend/shipper-mobile/package.json), lockfile | Locked Expo **57.0.27**, React/React DOM **19.2.3**, RN **0.86.3**, TS **6.0.3**, RN Web **0.21.4** (manifest `^0.21.2`), `@types/react` **19.2.18**. |
| Docker/Compose | `infra/` chỉ `.gitkeep` | Repo chưa Compose/service/profile/version. Docker không tìm thấy trên PATH máy audit. Chưa cần cho starter; cần khi task hạ tầng được duyệt. |
| PostgreSQL/Redis/RabbitMQ | Không client/adapter/config hoặc Compose | `NOT CONFIGURED`, version chưa chốt. URL template không chứng minh service đang chạy. |
| Android Studio/SDK/emulator | Script `android` = `expo start --android`; không native/Gradle project | Cần emulator/device đã chuẩn bị để dùng script. Studio/version/AVD chưa pin. Audit thấy SDK `E:\Android\Sdk`, platform `android-36`, build-tools `35.0.0/36.0.0/36.1.0`, adb **36.0.2**; chưa kiểm chứng device. |
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

Chỉ tiếp tục khi lệnh trước thành công. `verify` compile/test/package theo Maven/POM; không tự là architecture/integration/migration gate khi tests chưa có. Wrapper đang lỗi (mục J/L).

| Mục đích | Lệnh tại `app/backend` | Phạm vi |
| --- | --- | --- |
| Compile | `.\mvnw.cmd compile` | Không chạy test. |
| Test | `.\mvnw.cmd test` | Chỉ context-load test hiện có. |
| Build đầy đủ | `.\mvnw.cmd verify` | Compile/test/package, chưa PASS trong audit. |
| Chạy starter | `.\mvnw.cmd spring-boot:run` | Spring Boot Maven plugin đã có; audit chưa chạy. |

Fallback chẩn đoán **khi đã có Maven hệ thống**, ghi rõ khác distribution Wrapper:

```powershell
mvn.cmd --version
mvn.cmd -o verify
```

Audit dùng 3.9.12; offline FAIL vì parent 4.1.1 chưa cached. Khi môi trường có quyền artifact access, nhóm có thể kiểm chứng `mvn.cmd verify` hoặc Wrapper đã hoạt động; bước này chưa chạy. Không tự hạ Boot/sửa wrapper trong phiên tài liệu.

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

`start` gọi Expo CLI local; đọc URL/QR/port từ terminal, audit chưa quan sát startup. Để chạy biến thể, dừng server trước rồi chọn **một** script:

| Lệnh manifest | Tác dụng/điều kiện |
| --- | --- |
| `npm.cmd run android` | `expo start --android`, cần emulator/device phù hợp được Expo nhận diện. Không native APK build. |
| `npm.cmd run web` | `expo start --web`, browser preview starter, URL theo terminal. |
| `npm.cmd run ios` | `expo start --ios`, chưa kiểm chứng; không quy trình iOS simulator trên Windows. |

Không script build/lint/typecheck/test, lint config/test framework, `eas.json`, native `android/ios/`. Tsc trực tiếp dùng dependency hiện có đã PASS. Không dùng `npx` tải CLI mới/`expo-doctor` chưa cài như gate hiện hữu. Khi thay mobile code đọc [AGENTS mobile](../../app/frontend/shipper-mobile/AGENTS.md): Router/EAS trong hướng dẫn chưa phải thành phần đã implement.

### H.2. Emulator, thiết bị và native capabilities

Sau khi Android SDK/AVD máy được chuẩn bị theo target nhóm chốt, mở emulator hoặc kết nối thiết bị thử nghiệm, kiểm tra platform-tools:

```powershell
adb version
adb devices
```

`adb version` đã chạy; `adb devices`, mở AVD/điện thoại và script Android chưa chạy. Nếu thiếu PATH, dùng platform-tools của SDK máy; `E:\Android\Sdk` không đường dẫn bắt buộc. Repo chưa pin AVD/system image/Studio nên cần DEC-02/spike evidence trước khi công bố tương thích.

| Khả năng | Bằng chứng/giới hạn |
| --- | --- |
| Backend local | Không API client/base URL; không hướng dẫn localhost/emulator alias như wiring đang có. Cần contract/auth/backend runtime và config thật. |
| Navigation | `index.ts` → `registerRootComponent(App)`, không Expo Router dependency; `src/app/(auth)`/`(work)` chỉ `.gitkeep`. |
| GPS/background | Không location package/call/permission/service; feature location rỗng, chưa latency/device test. |
| Camera/barcode/POD | Không camera/scanner/storage package, permission hoặc upload; scanning/delivery rỗng. |
| SQLite/offline/secure storage | Platform folders chỉ `.gitkeep`, chưa queue/storage/restart/sync test. |
| Expo Go/dev build | Chưa xác nhận SDK client/native compatibility, chưa dev-build/EAS config để đưa build command cụ thể. Starter typecheck không chứng minh scan/GPS/offline. |

DEC-02/15/16/18/23 còn mở. Chỉ bổ sung permission/config plugins/native build sau review/spike thiết bị; không cài thư viện để “chạy đủ” trong audit.

## I. Kiểm thử và xác minh

`PASS` chỉ đúng phạm vi lệnh; `NOT_RUN` là test có nhưng chưa chạy; `NOT_IMPLEMENTED` là chưa có test/tool.

| Nhóm | Đường dẫn/bằng chứng | Lệnh/điều kiện/thành công | Audit |
| --- | --- | --- | --- |
| Domain unit | Chưa test nghiệp vụ backend/mobile/web | Chưa suite; Maven test/verify chỉ chứng minh BR khi có assertion/test thật và report. | `NOT_IMPLEMENTED` |
| Spring context smoke | `app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java`, `@SpringBootTest contextLoads()` | Backend `.\mvnw.cmd test`/`verify`, cần resolve dependencies; exit 0 và Surefire report test thực chạy. | `NOT_RUN`, wrapper/parent chặn trước test. |
| DB/API integration | Test domain/support chỉ `.gitkeep`, không Testcontainers | Chưa suite/command riêng; cần PostgreSQL isolation/config/migration và assertion transaction/constraint/auth. | `NOT_IMPLEMENTED` |
| Architecture | Backend test `architecture/.gitkeep` | Chưa ArchUnit/Modulith/tests; compile không thay boundary verification. | `NOT_IMPLEMENTED` |
| Contract/client | Không schema/controller, `package/api-client/.gitkeep` | Chưa validator/generator/contract test command. | `NOT_IMPLEMENTED` |
| Web E2E | `quality/e2e-web/.gitkeep` | Không Playwright config/package/suite, chưa web app. | `NOT_IMPLEMENTED` |
| Migration | Backend `db/migration/.gitkeep` | Chưa Flyway/test DB/SQL, không goal giả định. | `NOT_IMPLEMENTED` |
| Mobile typecheck | `App.tsx`, `index.ts`, `tsconfig.json`, local TS | Mobile `node .\node_modules\typescript\bin\tsc --noEmit --incremental false`, đúng dependencies, exit 0. | **PASS**, không diagnostic; chưa runtime/native proof. |
| Mobile unit/device/offline | `quality/mobile/.gitkeep`, features/platform rỗng | Chưa test framework/harness; startup/permissions/offline cần smoke/evidence thật sau triển khai. | Tests `NOT_IMPLEMENTED`, startup `NOT_RUN`. |
| Concurrency/fault/load/RQ | `quality/load/.gitkeep`, `scripts/.gitkeep` | Chưa k6/harness/fault scripts; không benchmark/invariant result giả. | `NOT_IMPLEMENTED` |
| Frontend lint/build | Không web manifest; mobile không lint/build scripts/config | Chưa lệnh lint/build cấu hình; tsc là static check riêng. | `NOT_IMPLEMENTED` |

CI/CD `.github/workflows/` chỉ `.gitkeep`. Khi thêm suite/CI theo task, ghi evidence/mapping vào [traceability](../quality/traceability.md); phiên này không sửa trạng thái ở file đó.

## J. Xử lý lỗi và phục hồi an toàn

| Triệu chứng | Chẩn đoán/cách xử lý |
| --- | --- |
| `npm.ps1 cannot be loaded` | Dùng `npm.cmd` cùng tham số; đã chạy version thành công. Không đổi ExecutionPolicy toàn máy. |
| Wrapper null-array / `Cannot start maven from wrapper` | Nhánh cache trong `mvnw.cmd` dùng `(Get-Item $MAVEN_M2_PATH).Target[0]`; audit lỗi tại đây trước Maven. Giữ wrapper, ghi version/environment; Maven hệ thống chẩn đoán riêng. Cần task fix/compatibility review, không xóa cache/đổi distribution tùy tiện. |
| Offline parent POM `4.1.1` không resolve | Parent chưa cached; `-o` không tải. Kiểm tra artifact repository/network/mirror khi được phép, verify lại. Chưa chứng minh version sai; không sửa POM cảm tính. |
| Java/build dependency lỗi | So JDK 21/Maven version; dừng trước startup nếu build fail. `target/` có sẵn không chứng minh build mới. |
| npm ci/dependency lỗi | So Node range/lock/npm/network/cache. Giữ manifest/lock, không regenerate để che mismatch. Tên package `web` là metadata hiện tại của mobile. |
| Port conflict | Đọc port trong lỗi/log, kiểm tra process LISTEN. Không kill process người khác; dừng server của mình bằng `Ctrl+C`, không tự thêm port config như có sẵn. |
| Health/login/API 404, web không chạy | Chưa Actuator/controller/auth/web manifest; xác định feature chưa implement thay vì debug URL bịa. Startup chỉ chứng minh starter. |
| DB connection/migration lỗi sau khi thêm persistence | Baseline chưa datasource/Flyway. Môi trường đã có dữ liệu: dừng ghi, giữ log/version/schema, backup; không repair/reset/drop/đổi migration đã chạy. Xem [failure recovery](failure-recovery.md). |
| Redis/RabbitMQ connectivity | Chưa client/service/fallback runtime. Khi có config thật, kiểm tra health/credential/outbox; không bỏ DB invariant hoặc xóa message/event để che lỗi. |
| Backend–web/mobile không kết nối | Chưa API client/base URL/web app. Chốt contract/config rồi kiểm tra backend runtime/host/mạng từ thiết bị; không mặc định điện thoại dùng URL laptop. |
| Android không nhận device | Kiểm tra SDK/platform-tools PATH, `adb devices`; giải quyết unauthorized/offline trên thiết bị thử trước chạy script. Không reset emulator data mặc định. |
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

### L.1. Mốc và môi trường

| Thuộc tính | Giá trị quan sát |
| --- | --- |
| Ngày kiểm tra | **13/10/2026**, timezone trao đổi Asia/Saigon. |
| Repository/branch/HEAD | `D:\TLCN\e-logistic`, `develop`, `7d0a7fa35d3c60c9fdfeb38fec4d588ff414fdee`. |
| Working tree đầu phiên | Nhiều docs SDD, AGENTS.md, .env.example chưa tracked; bảy `.gitkeep` bị xóa sẵn. Hai file plan/runbook cũng chưa tracked; không diff với HEAD cho chúng. |
| OS/shell | Maven nhận diện Windows 11 amd64, OS build `10.0.26300.0`, PowerShell `5.1.26100.9549`. |
| Tools | Oracle JDK 21.0.10, Node 24.13.0, npm 11.6.2, Maven hệ thống 3.9.12, adb 36.0.2. Docker/psql không trên PATH; không kết luận mọi service ngoài repo không tồn tại. |
| Local sẵn có | Mobile node_modules/backend target có trước audit, `.env` chưa có. Installed Expo/React/RN/TS khớp locked versions nêu trên. Artifact sẵn có không được dùng để khai test PASS. |

### L.2. Lệnh đã chạy và kết quả

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
| npm ci, online Maven verify | `NOT_RUN`: dùng dependencies sẵn và offline Maven, không cài/update cache/packages | Thiết lập máy mới với artifact access, giữ lock/version và ghi result. |
| Backend startup/HTTP/API/health smoke | `[UNVERIFIED]`: build chặn, health/business API chưa có | Wrapper/dependencies hoạt động, startup log/endpoint thật. |
| Expo start/web/android/iOS, emulator/phone render | `[UNVERIFIED]`: chỉ typecheck, không mở emulator/start app | Target/client SDK review, device smoke DEC-02. |
| DB/Flyway/seed/Compose/health | `NOT_IMPLEMENTED`, runtime `[BLOCKING]` bởi config/version/hạ tầng thiếu | Foundation có tool/config thật, DB thử riêng, DEC-26 review. |
| Lint/architecture/contract/integration/E2E/concurrency/native/fault | `NOT_IMPLEMENTED` ngoài context-load; context-load `NOT_RUN` | Suite/tool trong task được giao, chạy/mapping evidence theo DoD. |
| Auth/business/providers | `[BLOCKING]` theo [open questions](../decisions/open-questions.md) | T-001-01 review trước, sau đó T-002-01 auth; contract/domain/provider chốt theo slice. |

Phiên này chỉ cập nhật hai Markdown, không code/manifest/migration/infra/ADR/traceability hoặc lệnh thay đổi dữ liệu. Bước kế tiếp là review/tháo blocker baseline, không tái tạo skeleton hoặc tuyên bố local P0 đã sẵn sàng.
