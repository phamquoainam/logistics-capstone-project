# Day 1 — Existing Codebase Assessment, Baseline Verification & SDD Implementation Plan

**Hiện hành sau merge D1-05 — 2026-10-14:** D1-05 **DONE** cho scope backend CI đã duyệt.
PR #5 merge vào `develop/f50c8972a3a4d0a9c877d6e7e99d2a918cd866fd`;
[push run 38512345091](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/38512345091)
PASS: architecture 23/no skip, H2 40/tám PG-only skip, PG18 48/no skip;
guard 14 mỗi job, artifacts/digests và required checks enforced PASS.
[D1-05 evidence](d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14).
D1-03 **DONE cho PostgreSQL Phases A–E đã duyệt**; mobile tooling và full onboarding
thuộc D1-02/Feature 001, không giữ task PostgreSQL PARTIAL vì các gate khác.
[D1-03 reconciliation](d1-03-postgresql-evidence.md). D1-02, D1-06–08 và toàn
Feature 001/Day 1 còn mở; không giảm AC hoặc ngầm DEFERRED.
Nhánh tài liệu `feature/d1-05-post-merge-closure` tạo từ develop sạch; chưa commit/
push/PR/merge thay đổi mới. Checklist D/evidence hiện hành thay trạng thái audit
cũ bên dưới; các mốc cũ được giữ làm lịch sử.

**Lịch sử sau Demo-001 / D1-03 (2026-10-13):** baseline develop/08c0c34; branch `feature/d1-03-postgresql-foundation`. DEC-26 đã được duyệt; PostgreSQL/Flyway DB milestone PASS, H2 giữ nguyên; assessment khi đó D1-03 PARTIAL. [Evidence và reconciliation hiện hành](d1-03-postgresql-evidence.md), [runbook](../runbooks/postgresql-foundation.md). Các bảng baseline/gap dưới đây lưu lịch sử trước Demo-001; không dùng placeholder cũ để kết luận chưa có Flyway/dependencies.

**Cập nhật spike ngày 13/10/2026:** branch `docs/sdd-baseline-and-runbooks`, HEAD `495813dbd318142f30855451546d313190f11656`, working tree sạch trước khi sửa. Wrapper Windows đã sửa null-array; Maven 3.10.0/Java 21.0.10 chạy `verify` online và offline PASS, Surefire xác nhận `contextLoads` chạy thật. Chỉ backend toolchain slice đạt gate; foundation chưa DONE. Kết quả chi tiết nằm tại [runbook, mục L.4](../runbooks/local-development.md#l4-toolchain-spike-t-001-01--d1-02).

**Verification D1-02 tiếp theo, cùng ngày:** Node 24.13.0/npm 11.6.2, lock/dependencies, mobile typecheck, Metro và Android bundle PASS. Android render BLOCKED vì không có device/AVD; lint và development build chưa có tooling/config được duyệt. Spring Modulith 2.1.1 có metadata khớp Boot 4.1.1/Java 21 nhưng chưa integration test hoặc quyết định chọn tooling. D1-02 vẫn PARTIAL; [bằng chứng L.5](../runbooks/local-development.md#l5-d1-02-nodenpm-expoandroid-và-modulith-verification) thay trạng thái chưa kiểm tra ở mốc trước. Working tree đầu phiên có sẵn ba file từ spike; phiên này chỉ đổi hai Markdown, giữ nguyên Wrapper.

Baseline audit trước spike: branch `develop`, HEAD `7d0a7fa35d3c60c9fdfeb38fec4d588ff414fdee`, có tài liệu chưa commit. Metadata/lệnh FAIL của mốc này được giữ riêng tại runbook L.1–L.2.

**Android runtime & closure review, 13/10/2026:** môi trường đã thay đổi: AVD `Pixel_7_Pro`, `emulator-5554` online, Android 17/API 37, boot completed. Metro và kết nối trực tiếp từ emulator tới `/status` PASS; render vẫn BLOCKED vì chưa cài Expo Go (`host.exp.exponent`), lệnh mở client không resolve được Intent. Không cài client hoặc đổi hệ thống. [Bằng chứng và closure matrix L.6](../runbooks/local-development.md#l6-d1-02-android-runtime--closure-review) là trạng thái mới nhất; L.5 giữ lịch sử. ArchUnit-only, Expo Go smoke target, Node/npm pin và tách lint/test/native build vẫn PROPOSED; không có gate nào được DEFERRED hoặc miễn theo phê duyệt. D1-02 vẫn PARTIAL, chưa DONE.

**Expo Go/runtime smoke mới nhất, 13/10/2026:** người dùng đã cho phép cài client chính thức SDK 57 và smoke. Đầu phiên client 57.0.9/versionCode 444 đã có sẵn; APK pull từ emulator khớp SHA-256 release chính thức và signature verify PASS, không cần cài lại. Mở `exp://10.0.2.2:8081` thành công; screenshot/UI hierarchy xác nhận text starter, React chạy main bằng manifest SDK 57 và kết nối Metro còn ESTABLISHED sau gần 4 phút. Render/client/network PASS. Log review PARTIAL: có lỗi init của Expo Go và UWB-service crash thuộc emulator, chưa xác định root cause; không thấy client crash/ANR/JS fatal trong observation window. [L.7](../runbooks/local-development.md#l7-d1-02-expo-go-provenance--android-starter-smoke) thay trạng thái mới nhất, L.4–L.6 giữ lịch sử. D1-02 vẫn PARTIAL vì accepted tooling/pin, native build, lint/test/web và các phát hiện còn mở; chưa có DEFERRED hoặc scope change được duyệt.

## A. Mục tiêu và phạm vi

“Day 1” là phiên làm việc tiếp theo trên repository hiện hữu: kiểm kê code, xác minh baseline, xác định phần còn thiếu và chọn lát cắt triển khai theo SDD. Giữ backend Spring Boot và Expo app hiện có; chỉ bổ sung foundation khi có gap được chứng minh. Không cam kết hoàn thành P0 trong một ngày.

Phiên baseline ban đầu chỉ cập nhật hai Markdown. Phiên spike hiện tại được người dùng duyệt sửa thêm `app/backend/mvnw.cmd`, chạy backend baseline và cập nhật bằng chứng trong hai Markdown này. Giữ Java 21, Spring Boot 4.1.1, Maven distribution 3.10.0; không thêm dependencies, đổi global settings, triển khai business feature/migration/database, commit hoặc push. Foundation được duyệt triển khai theo milestone, ưu tiên backend build và PostgreSQL/Flyway trước Redis/RabbitMQ; acceptance criteria hiện có giữ nguyên.

Phiên environment verification kế tiếp chỉ cập nhật hai Markdown; không đổi source/dependencies/build config, không prebuild/cài SDK/tạo credentials. Expo lint đã tự thử setup dependency ngoài dự kiến; tiến trình được dừng và manifest khôi phục byte-for-byte theo snapshot, lockfile/dependencies không đổi. Lint vẫn BLOCKED, không nhận kết quả từ auto-setup.

Chuyển sang business code khi [Definition of Ready](../codex/definition-of-ready.md) đạt cho slice: dependency và hợp đồng công khai đã review, blocker liên quan đã chốt hoặc phần độc lập được tách rõ, fixture/test/gate xác định, baseline chạy được. Hoàn tất feature phải đạt [Definition of Done trong test strategy](../quality/test-strategy.md#definition-of-done-per-feature), không chỉ có controller hoặc UI.

## B. Baseline repository hiện tại

### B.1. Quy ước bằng chứng

| Trạng thái | Cách dùng trong tài liệu này |
| --- | --- |
| `IMPLEMENTED` | Có hiện thực và bằng chứng phù hợp với phạm vi nhỏ được nêu; không suy ra feature P0 hoàn thành. |
| `PARTIAL` | Có một phần hiện thực, chưa đủ yêu cầu. |
| `NOT_IMPLEMENTED` | Không tìm thấy hiện thực sau kiểm tra source/config, kể cả file ẩn ngoài dependency/build. |
| `UNVERIFIED` / `[UNVERIFIED]` | Có code/cấu hình hoặc lệnh được hỗ trợ nhưng hoạt động chưa được chứng minh. |
| `BLOCKED` / `[BLOCKING]` | Gate/business code chưa thể tiếp tục vì điều kiện/quyết định cụ thể còn thiếu. |

`[PROPOSED]` là phương án để review. Phiên bản trong manifest không tự thành quyết định `ACCEPTED`. Thư mục `.gitkeep`, dependency đã cài và `target/` có sẵn không phải bằng chứng runtime thành công.

### B.2. Ứng dụng, công cụ và hạ tầng

| Thành phần | Bằng chứng thực tế | Hiện trạng và kiểm chứng |
| --- | --- | --- |
| Backend | [pom.xml](../../app/backend/pom.xml), [BackendApplication.java](../../app/backend/src/main/java/com/elogistic/backend/BackendApplication.java) | `PARTIAL`: một artifact `com.elogistic:backend:0.0.1-SNAPSHOT`, Java 21, Spring Boot parent **4.1.1**; chỉ Web MVC và Web MVC test starters. Wrapper `verify`/context smoke PASS; HTTP server/runtime `UNVERIFIED`. |
| Build backend | [wrapper properties](../../app/backend/.mvn/wrapper/maven-wrapper.properties), [mvnw.cmd](../../app/backend/mvnw.cmd) | Wrapper **3.3.4**, distribution Maven **3.10.0** giữ nguyên. Null-safe cache target sửa tối thiểu; `--version`, cache thường có khoảng trắng và junction thật PASS. `verify` online rồi offline PASS với explicit user repository và quyền tạo cache lock. Xem L.4; không dùng Maven hệ thống để thay gate Wrapper. |
| Backend config | [application.properties](../../app/backend/src/main/resources/application.properties) | Chỉ `spring.application.name=backend`. Không profile local, datasource, security, port hoặc health/readiness tùy chỉnh. |
| Web độc lập | [app/frontend/web](../../app/frontend/web) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không manifest, lockfile, entry point hoặc build tooling. |
| Mobile | [package.json](../../app/frontend/shipper-mobile/package.json), [App.tsx](../../app/frontend/shipper-mobile/App.tsx), [index.ts](../../app/frontend/shipper-mobile/index.ts), [app.json](../../app/frontend/shipper-mobile/app.json) | `PARTIAL`: Expo starter đăng ký `App`, màn hình mẫu; package/name/slug hiện là `web` dù nằm ở `shipper-mobile`. Chưa có luồng shipper. |
| Mobile stack | [package-lock.json](../../app/frontend/shipper-mobile/package-lock.json), [tsconfig.json](../../app/frontend/shipper-mobile/tsconfig.json) | npm lockfile v3; Expo **57.0.27**, React/React DOM **19.2.3**, RN **0.86.3**, TS **6.0.3**, `@types/react` **19.2.18**; installed direct dependencies khớp lock. Node **24.13.0**/npm **11.6.2** tương thích engines; project chưa pin engines/packageManager. Typecheck/Metro/bundle/starter render PASS tại L.7; logs PARTIAL, dev build UNVERIFIED. |
| Mobile scripts | `start`, `android`, `ios`, `web` trong manifest | Gọi `expo start` với tùy chọn tương ứng. Không script build/lint/typecheck/test, Expo Router dependency, native project hoặc EAS config. Expo web preview không thay web nghiệp vụ độc lập. |
| PostgreSQL/Flyway/seed | [db/migration](../../app/backend/src/main/resources/db/migration), [infra/postgres](../../infra/postgres) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không SQL, JDBC driver, persistence/Flyway dependency, migration plugin, datasource hoặc seed script. Chưa migration strategy hoạt động. |
| Compose/cache/broker/storage | [infra](../../infra), POM và source | `NOT_IMPLEMENTED`: không Compose YAML, service/profile/image/healthcheck hay adapter Redis/RabbitMQ/POD. `.env.example` chưa nối vào ứng dụng. |
| Contracts/client | [package/api-client](../../package/api-client), `specs/openapi/`, `specs/events/` | Client chỉ `.gitkeep`, thư mục contracts cũ không có schema; `contracts/` chưa có. Không controller/API nghiệp vụ, schema hoặc generated client. |
| Backend tests | [BackendApplicationTests.java](../../app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java) | Context-load **PASS**: Surefire 3.5.6 ghi 1 test, 0 failures/errors/skipped; report mới sau thời điểm chạy. Không assertion nghiệp vụ/DB integration/architecture test. |
| CI/quality/scripts | [.github/workflows](../../.github/workflows), [quality](../../quality), [scripts](../../scripts) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; chưa CI YAML, E2E/load/mobile harness, fixtures hoặc verify/seed/recovery script. |

### B.3. Nine domain modules: mục tiêu và package thực tế

Base package là `com.elogistic.backend`, khác package ví dụ `com.example.elogistics`. Source main chỉ có `BackendApplication.java`. **Cả chín domain đều `NOT_IMPLEMENTED` về nghiệp vụ**; các thư mục dưới chỉ `.gitkeep`, chưa được kiểm chứng boundary.

| Domain theo [Module Catalog](../architecture/module-catalog.md) | Package dưới `app/backend/src/main/java/com/elogistic/backend/` | Nhận định |
| --- | --- | --- |
| Identity | `identity/api`, `identity/internal` | Có bố trí thư mục; chưa authentication/RBAC/object scope. |
| Merchant | Không `merchant/` | Chưa profile, pickup point/address flow. |
| Order | `orders/api`, `orders/internal` | Tên số nhiều; chưa shipment/state/snapshot. |
| Pricing | Không `pricing/` | Chưa tariff/version/quote/cache. |
| Warehouse | `wms/api`, `wms/internal` | Tên khác catalog; chưa stock/reservation/fulfillment. |
| Finance | `finance/api`, `finance/internal` | Chưa ví/ledger/VNPay/COD. |
| Dispatch | `dispatch/api`, `dispatch/internal` | Chưa assignment/manifest/custody. |
| Routing | Không `routing/`; có `shipping/api`, `shipping/internal` | Không đủ code xác định `shipping` thuộc domain nào; không tự đồng nhất với Routing/Order. |
| Notification | `notifications/api`, `notifications/internal` | Tên số nhiều; chưa notification/outbox/inbox. |

`platform/` có thư mục kỹ thuật rỗng; `workflow/` chưa có. Chưa Spring Modulith, ArchUnit, `package-info.java` hoặc test phát hiện import `othermodule.internal.*`. Review mapping trước, không đổi tên hàng loạt theo blueprint.

### B.4. Nguồn và giới hạn audit

Đã đối chiếu [AGENTS.md](../../AGENTS.md), [PRD](../prd.md), [principles](../principles.md), toàn bộ [domain](../domain), [acceptance](../quality/acceptance.md), [test strategy](../quality/test-strategy.md), DoR, kiến trúc/ADR, [dependency order](dependency-order.md), [open questions](../decisions/open-questions.md), [failure recovery](../runbooks/failure-recovery.md) và spec/design/tasks liên quan. Bộ 16 feature vẫn ghi `DRAFT_SPEC/NOT_IMPLEMENTED`; feature 001 thực tế có một phần bootstrap, chưa đạt AC-001.

Không tìm thấy DOCX báo cáo trong repository; chưa đối chiếu trực tiếp bản gốc có thẩm quyền cao nhất. Các Markdown bắt buộc đều có; không file DoD riêng, DoD nằm trong test strategy. Chưa `README.md`, Compose executable, web manifest, contract schema hoặc seed executable; không dùng nội dung giả định thay bằng chứng.

## C. Gap analysis và mức độ ưu tiên

Mức độ phản ánh ảnh hưởng tới phát triển/nghiệm thu, không phải kết luận lỗi của nghiệp vụ đã chạy.

| ID | Mức | Gap và bằng chứng | Yêu cầu/gate ảnh hưởng | Hướng xử lý |
| --- | --- | --- | --- | --- |
| GAP-01 | Critical | Chưa persistence, migration, transaction/constraint, idempotency, ledger/reservation/workflow; POM chỉ Web MVC | BR-02/06–08/13–18; NFR-C01/C02; `005/006/007/011` | Chốt ownership/money/lock/migration contracts, test PostgreSQL thật. Chặn confirm/thu tiền/giữ tồn; không mock nghiệp vụ để vượt gate. |
| GAP-02 | Critical | Identity rỗng, không security dependency/config | FR-A01/A02, NFR-S01–05; `002` | Review DEC-03/04 và role/object scope trước API nghiệp vụ; frontend không thay authz server. |
| GAP-03 | High | Wrapper/verify và Node/mobile typecheck/Metro/bundle/starter render PASS; logs PARTIAL với init findings. Modulith 2.1.1 metadata phù hợp stack, chưa được chọn hoặc chạy trong repo | DEC-01/02; REQ-001-01/05 | Giữ versions; cần review log findings, dev-build, tooling/pin decision và clean build trên máy khác. Không suy ra library integration từ metadata hoặc native feature readiness từ starter. |
| GAP-04 | High | Không Compose/DB/health/seed; template env không consumer | AC-001-01/02/04/05, AC-06; NFR-O02/O03 | Chốt version/wiring cần cho slice, thêm hạ tầng có smoke/migration evidence. Không cài broker/cache chỉ để đủ sơ đồ. |
| GAP-05 | High | Mapping chưa đủ nine domains; architecture test chỉ `.gitkeep` | ADR-0001; REQ-001-03; NFR-M01 | Review mapping/public API; test phải fail khi tạo boundary violation. Giữ backend hiện có. |
| GAP-06 | High | Web chỉ thư mục; mobile starter thiếu native scan/POD/GPS/offline | AC-01/02; FR-S01–05; DEC-02/15/16/18/23 | Xác minh starter, native spike sau quyết định; tạo web khi task/baseline được duyệt. |
| GAP-07 | High | Không approved contracts/client, CI, integration/concurrency/E2E/fault tests | REQ-001-04; AC-04/07; DoR/DoD | Review DEC-25/27, ADR-0003; test cùng slice và lưu evidence thật. Context-load không chứng minh invariants. |
| GAP-08 | Medium | Blueprint dùng `apps/`, `packages/`; code dùng `app/`, `package/` | NFR-O03; kế hoạch/runbook cũ | Dùng đường dẫn thực trong hai file; review docs/spec khác ở task riêng, không ép chuyển thư mục. |
| GAP-09 | Medium | Không DOCX tại repo, runtime/evidence chưa có | Thứ tự thẩm quyền, AC-07; DoD | Cần bản báo cáo khi review nghiệp vụ; cập nhật traceability theo evidence trong phiên có phạm vi cho phép. |
| GAP-10 | Low | Mobile package/name/slug là `web`, routes rỗng chưa Router | Khả năng hiểu cấu trúc local | Ghi rõ runbook; đổi metadata/navigation khi có task, không viết lại starter để sửa tên. |

## D. Ưu tiên Day 1 và checklist thực thi

Checklist assessment này không thay checklist feature. D1-02 là task tổng thể đầu tiên chưa tick; D1-03/04/05 đã DONE theo scope được duyệt và có evidence. Full feature 001 vẫn bắt đầu ở T-001-01 — Review, chưa đạt toàn bộ điều kiện hoàn thành.

- [x] **D1-01 — An toàn repository và kiểm kê baseline** — ưu tiên Critical; phụ thuộc: không. File: `AGENTS.md`, manifests, source/config, docs/specs. Gate: ghi branch/HEAD/dirty state, executable/placeholder mà không hủy thay đổi. Bằng chứng: `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git ls-files`, `rg --files --hidden --no-ignore` với loại trừ dependency/build/Git; bảng B đã lập. Chỉ audit hoàn thành, không phải foundation.

- [ ] **D1-02 — Xác minh môi trường/toolchain** — ưu tiên High; phụ thuộc D1-01. **PARTIAL**: backend spike PASS tại L.4; Node/npm/lock/typecheck/SDK/Metro/bundle PASS tại L.5–L.7. AVD boot, Expo Go 57.0.9 provenance/signature, starter render và client-to-Metro PASS tại L.7. Log review PARTIAL với non-fatal init findings và emulator UWB-service crash; chưa root cause. Native development build UNVERIFIED; mobile lint/test BLOCKED; web Demo-001 typecheck/build/browser PASS, web lint chưa có. ArchUnit-only D1-04 được duyệt/verify PASS; DEC-02 accepted stack/Node/npm pin chưa chốt; tách milestone chưa duyệt, không có DEFERRED. Không tick DONE hoặc tự miễn gate; closure matrix/điều kiện trong L.7.

- [x] **D1-03 — Xác minh build/test/migration** — **DONE cho PostgreSQL Phases A–E được duyệt**: DEC-26, infrastructure/profile/JDBC/Flyway V1/V2, ownership/no-DDL, cold-start/validate/rerun/upgrade/restart persistence, concurrency/idempotency/rollback và PostgreSQL-backed Demo-001 web/H2 regression PASS. Hosted develop/f50c897 verify PG18 48/no skip, H2 40/tám PG-only skip, architecture 23 PASS. Backend toolchain prerequisite đã PASS; mobile/pin/lint và full onboarding thuộc D1-02/Feature 001. [Scope/evidence](d1-03-postgresql-evidence.md). Không sửa AC hoặc miễn Testcontainers/full cold-start.

- [x] **D1-04 — Review kiến trúc/bảo mật** — **DONE cho scope local đã duyệt**: reviewer chấp thuận và merge vào develop/2c609c4 theo xác nhận người dùng, đã kiểm chứng ancestry/source. Mapping nine domains, 37 production classes thuộc identity/merchant/orders/platform; sáu business modules chưa implement không tính PASS. 23 architecture tests, 11 negative violations/one positive fixture; adapter direction và minimal trusted actor theo ADR-0004. Auth/object scope, log/fail-fast/bounded secret audit và H2/PG/browser regressions PASS. [Evidence gốc](d1-04-architecture-evidence.md), [merge/CI reconciliation](d1-05-ci-evidence.md). CI/full security/provider/native và full feature AC vẫn là gates riêng, không miễn hoặc hạ tiêu chí.

- [x] **D1-05 — CI Architecture Gate & Clean Runner Verification** — **DONE**: người dùng review/merge PR #5; merge f50c897, actual push develop run 38512345091/attempt 1 PASS. Architecture 23/23; H2 40 PASS/tám named PG-only skips; PG18 48/48, zero skips; guard 14 mỗi job. Artifacts digests/named inventory/privacy PASS. Ruleset 25049940 Active, target develop, ba required GitHub Actions checks/strict policy/no bypass, effective rules verified. [Official closure](d1-05-ci-evidence.md#post-merge-verification-and-official-closure--2026-10-14). Không tuyên bố deliberate-red workflow experiment hoặc sáu absent modules đã kiểm chứng; original Feature 001 AC còn nguyên.

- [ ] **D1-06 — Chọn slice và kiểm tra DoR** — **PARTIAL / chưa READY**: D1-04/05 dependencies đã đạt. Review code hiện có, chọn đúng gap chưa hoàn thiện, chốt BLOCKING trong scope, approved public contracts/owner/fixtures/gates. Demo-001 đã có Identity/auth/merchant/DRAFT; không khởi tạo lại hoặc suy ra full feature 002 DONE. Chỉ BLOCKED nếu có dependency thực sự chưa duyệt của slice được chọn; không bắt toàn D1-02 đóng cho mọi phần độc lập.

- [ ] **D1-07 — Test-first và gate slice** — **BLOCKED bởi D1-06 chưa chọn/duyệt slice**: thiếu test inventory/execution của slice được chọn, happy/negative/auth/replay/concurrency/failure theo requirement, API/E2E/build/migration/architecture. Demo-001 regressions PASS không tự hoàn thành gate của feature mới. Không triển khai business code trong closure tài liệu này.

- [ ] **D1-08 — Chốt kết quả/evidence/blocker** — **PARTIAL**: evidence/traceability cho D1-03/04/05 đã ghi; task mở có scope/next action rõ. Còn nghiệm thu D1-02, D1-06/07, full REQ/AC/test-ID và runbook reproduction trước final Day 1 review. Thay đổi closure hiện tại chỉ local, chưa commit/publish; không tick toàn Day 1 DONE.

## E. Chiến lược triển khai feature

Theo [dependency order](dependency-order.md), [feature register](../../specs/INDEX.md) và audit, việc tiếp theo là **review T-001-01, spike tháo blocker của foundation hiện có**, rồi bổ sung enabler còn thiếu. Không mặc định hoàn thành/xây lại `001/002` theo tên thư mục.

| Bước | Slice sau khi đủ điều kiện | Dependency/quyết định | Bất biến và test |
| --- | --- | --- | --- |
| 1 | [001 foundation](../../specs/001-platform-foundation/spec.md): kiểm chứng/bổ sung build, DB migration, boundary, health/runtime | [Design](../../specs/001-platform-foundation/design.md), [tasks](../../specs/001-platform-foundation/tasks.md); DEC-01/02/25/26, mapping packages | Một artifact, runtime không DDL, migration không phá dữ liệu, boundary test thật; giữ backend/mobile hiện có. |
| 2 | [002 Identity](../../specs/002-identity-access/spec.md): phiên/quyền đối tượng trong slice review | Foundation cần thiết; [design](../../specs/002-identity-access/design.md), [tasks](../../specs/002-identity-access/tasks.md); DEC-03/04 | Unauthorized/expired/revoked/cross-scope bị từ chối; Admin không mặc nhiên Finance. |
| 3 | [003 Merchant/address](../../specs/003-merchant-address/spec.md) → [004 Pricing](../../specs/004-pricing-engine/spec.md) | Identity; DEC-05/06/07/08; dữ liệu địa chỉ/vùng/giá chuẩn | BR-19, BR-10–12: địa chỉ gốc/version, quote expiry/input/breakdown/version, golden rounding; cache không thay DB. |
| 4 | [005 DRAFT](../../specs/005-shipment-lifecycle/spec.md) phần độc lập sau contract; [006 Finance](../../specs/006-wallet-payments/spec.md), [007 WMS](../../specs/007-warehouse-wms/spec.md) chuẩn bị confirm | DEC-09/10/11/12/13/28; chưa confirm khi Finance/WMS cần thiết chưa ready | Owner trusted, replay/state guard; exact money, ledger append-only, tồn khả dụng không âm, reservation conservation. |
| 5 | `005` confirm/cancel xuyên module | Pricing + Finance + Warehouse public API; workflow cùng transaction manager, lock order review | Quote + debit + hold nếu WMS + shipment/snapshot/idempotency cùng local DB transaction; thiếu tiền/tồn rollback; retry/concurrent không trừ/hold hai lần; hủy phần chưa dùng. |
| 6 | [008 Dispatch/Hub](../../specs/008-dispatch-hub/spec.md) → [009 Shipper/POD](../../specs/009-shipper-pod/spec.md) → [010 Routing/GPS](../../specs/010-routing-location/spec.md) → [011 COD](../../specs/011-cod-reconciliation/spec.md) | Order/Finance/WMS; Routing cần Dispatch; DEC-14–19, native/provider decisions | Một assignment active/chặng; WMS outbound ≠ hub inbound; delivery/POD/COD nhất quán; GPS/route không sửa assignment; COD đủ điều kiện, kỳ CLOSED bất biến. |
| 7 | `012` notification/dashboard, `013` admin, `014` import, `015` offline; chuẩn bị `016` evidence | Prerequisites từng spec; DEC-20–24/27, core contracts | Sau commit + dedupe/inbox; scope dashboard/recovery; import chỉ draft; offline giữ operation ID/revalidate; RQ dataset cố định; P1 sau P0. |

Không triển khai API/DTO/table `[PROPOSED]` như contract đã duyệt. Không tính số dư từ body, thay DB constraint bằng Redis lock/queue/frontend, hoặc gọi provider mạng trong transaction tiền/tồn. Draft/starter chạy được chưa phải AC-02 end-to-end.

## F. Verification gates

Các lệnh hiện có nằm trong [runbook](../runbooks/local-development.md); lệnh được hỗ trợ khác kết quả đã chạy. Gate chưa có công cụ/test báo `NOT_IMPLEMENTED`, không đặt lệnh giả.

| Gate | Bắt đầu khi | Hoàn tất khi | Hiện trạng |
| --- | --- | --- | --- |
| Ready/contract | Spec → design → tasks review; blocker chốt; contract/fixture rõ | REQ/AC/test/evidence truy vết, contract đúng schema approved | `BLOCKED`: DRAFT specs, schema/client chưa có. |
| Build | JDK/Wrapper/dependencies truy cập được | Backend `.\mvnw.cmd verify`; mobile `node .\node_modules\typescript\bin\tsc --noEmit --incremental false`; web theo scripts thật sau manifest | Wrapper verify PASS tại L.4; mobile typecheck và Android JS bundle PASS tại L.5, không phải APK build; web chưa có. |
| Unit/context/integration | Test tồn tại; DB thử riêng khi persistence | Exit 0/report thật, assertions theo BR/transaction/constraint | Context-load PASS (1 test); domain/DB tests chưa có. |
| Architecture | Mapping/boundary policy review | Test bắt internal import/cycle/cross-owner write, PASS khi code hợp lệ | Chưa test/dependency; rà starter không chứng minh nine domains. |
| Migration | Flyway/driver/config/SQL/DB/version/credentials chốt | Empty/upgrade/re-run an toàn; Hibernate validate nếu ORM dùng; runtime không DDL | `NOT_IMPLEMENTED`, không CLI/plugin command hiện hữu. |
| API/authz | Approved schema/auth decision/account-scope fixtures | Contract/unauthorized/cross-tenant/revocation tests PASS, không lộ dữ liệu | Chưa có; `/api/v1`, `/actuator/health` vẫn đề xuất. |
| Business invariants | Owner/transaction/lock/unique/check/idempotency thiết kế | Happy/negative/duplicate/concurrent/timeout/restart/rollback PASS, không vi phạm BR trong kịch bản công bố | `NOT_IMPLEMENTED`, không mock tiền/tồn để thay gate. |
| Native/E2E/CI | Device/browser target công bố, harness có | UI đúng server state, permission/offline/restart thật, CI/evidence | Expo Go starter render/client-to-Metro PASS tại L.7; logs PARTIAL/init findings. Dev-build/permissions/offline UNVERIFIED; chưa harness/workflow, không thay P0 feature E2E. |

## G. Rủi ro, blocker và quyết định

| Loại | Nội dung | Điều kiện tiếp tục |
| --- | --- | --- |
| Chấp thuận theo báo cáo | [ADR-0001](../architecture/decisions/ADR-0001-modular-monolith.md): một backend, Package by Domain, local ACID, một PostgreSQL với ownership | Giữ định hướng; boundary/atomicity tests chứng minh. |
| Chấp thuận một phần | [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md): Flyway/ownership theo báo cáo, migration details PROPOSED | Review DEC-26, DB version/stream/order và runtime/migration accounts trước DDL. |
| Đề xuất chưa duyệt | [ADR-0003](../architecture/decisions/ADR-0003-contract-and-idempotency.md), DEC-25/27/28 | Review API/error/idempotency/event/workflow; không chọn wire format/provider bằng phỏng đoán. |
| `[BLOCKING]` gần nhất | DEC-01/02: wrapper/build/native baseline; DEC-03/04: auth; package mapping chưa rõ | Owner/reviewer và evidence trong [decision register](../decisions/open-questions.md); source version không tự đóng decision. |
| `[BLOCKING]` theo domain | DEC-05–07/09–13/15–17/19/22–24, CONFIG chưa được duyệt | Chốt theo slice, provider theo decision; không bắt mọi provider chốt trước spike độc lập. |
| Giới hạn môi trường | Client Expo Go SDK 57 đã có, APK chính thức và starter render/network PASS. Expo Go init exceptions và emulator UWB-service crash được ghi riêng, root cause UNVERIFIED | Không sửa client/emulator/system/source để che log. Cần review findings và approval tooling/dev-build/pin/milestone; Docker chưa trên PATH. |
| Giới hạn nguồn | Chưa DOCX tại repo; docs mới và `.gitkeep` bị xóa sẵn | Đối chiếu báo cáo khi review nghiệp vụ; giữ toàn bộ thay đổi, không checkout/reset để làm sạch. |

## H. Đầu ra kỳ vọng và điều kiện tiếp tục

Baseline và spike cung cấp version/path/evidence, gap, task/dependency/gate và runbook PowerShell có PASS/FAIL/NOT_RUN. Không tự tick feature DONE hoặc sửa ADR/traceability ngoài phạm vi ba file đã duyệt.

Backend toolchain spike của **T-001-01 / D1-02** đã thực hiện; bằng chứng REQ/AC nằm tại runbook L.4. Không sửa feature tasks, ADR hoặc [traceability](../quality/traceability.md) vì ngoài ba file được phép. T-001-01 và feature 001 chưa DONE.

**Nhiệm vụ kế tiếp để đóng D1-02:** Expo Go SDK 57 smoke và ArchUnit-only đã được duyệt/kiểm chứng, không yêu cầu duyệt lại. Chốt DEC-02 Node/npm pin, lint/test tooling và native build target theo kế hoạch I; review init/emulator findings. Gate còn thiếu phải đạt hoặc split được duyệt chính thức. Không thay native development-build feasibility/feature 001 AC bằng starter render; chưa tick DONE.

**D1-03 hiện hành:** PostgreSQL Phases A–E DONE; build/DB/architecture/hosted CI PASS. Review DEC-26/chưa migration ở các mốc cũ là lịch sử. D1-02 và full onboarding/native/contract gates vẫn giữ trong đúng task; không tự chuyển business slice sang READY.

## I. D1-02 Closure plan sau D1-05 — PROPOSED

Không triển khai tooling hoặc cài SDK trong phiên closure tài liệu này.

| Thứ tự | Gate / hành động | Hiện trạng | Điều kiện hoàn thành |
|---|---|---|---|
| 1 | Chốt DEC-02: Node 24.13.0/npm 11.6.2 đã chạy, pin mechanism, lint/test tooling scope và native build target | BLOCKED cho implementation: chưa duyệt dependencies/config/metadata | Owner duyệt đúng versions/phạm vi; Java21/Boot4.1.1/Maven3.10.0 và ArchUnit-only đã accepted, không yêu cầu duyệt lại |
| 2 | Pin đã duyệt; clean npm install theo lock; lint/typecheck/build/test web và mobile với assertions phù hợp slice | Mobile lint/test BLOCKED; web lint thiếu; typecheck/web build/browser baseline PASS | Command/version/exit/report thật; không tạo lại skeleton, tests rỗng hoặc đổi versions để né compatibility |
| 3 | Isolated Android development-build feasibility / APK starter smoke | UNVERIFIED; config/tooling cần duyệt | Kiểm tra Gradle/JDK/SDK, build/app ID/dev-client strategy; native build và render/Metro có logs; không tự prebuild source/tạo credentials |
| 4 | Review Expo Go initialization/emulator UWB findings | PARTIAL; root cause UNVERIFIED | Reproduce/attribute, owner/disposition có evidence; không xóa findings bằng UI smoke PASS hoặc tự đổi hệ thống |
| 5 | D1-02 closure review và reproducibility theo target công bố | PARTIAL | Mandatory gates đạt; split/DEFERRED chỉ khi duyệt cụ thể owner/task/gate. Full other-developer onboarding AC-001-05 vẫn là foundation gate riêng |

**Ưu tiên duy nhất kế tiếp:** D1-02 Closure decision gate (hàng 1), để mở
implementation tooling/native đang chặn tiến độ. Native business capabilities
chưa liên quan không phải điều kiện cho starter build feasibility.
