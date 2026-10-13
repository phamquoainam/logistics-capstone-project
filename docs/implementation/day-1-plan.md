# Day 1 — Existing Codebase Assessment, Baseline Verification & SDD Implementation Plan

**Đối chiếu ngày 13/10/2026:** branch `develop`, HEAD `7d0a7fa35d3c60c9fdfeb38fec4d588ff414fdee`, có thay đổi chưa commit. Baseline gồm source tại HEAD và tài liệu SDD trong working tree; HEAD riêng lẻ chưa chứa toàn bộ tài liệu này. Kết quả lệnh và giới hạn môi trường nằm tại [runbook local, mục L](../runbooks/local-development.md#l-verification-metadata).

## A. Mục tiêu và phạm vi

“Day 1” là phiên làm việc tiếp theo trên repository hiện hữu: kiểm kê code, xác minh baseline, xác định phần còn thiếu và chọn lát cắt triển khai theo SDD. Giữ backend Spring Boot và Expo app hiện có; chỉ bổ sung foundation khi có gap được chứng minh. Không cam kết hoàn thành P0 trong một ngày.

Phiên cập nhật tài liệu này chỉ sửa `docs/implementation/day-1-plan.md` và `docs/runbooks/local-development.md`. Không triển khai feature, tái cấu trúc package, đổi phiên bản, sửa migration/manifest/hạ tầng, tạo DB/seed, commit hoặc push. Các task triển khai bên dưới dành cho phiên được giao phạm vi tương ứng; không được hiểu là đã thực hiện.

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
| Backend | [pom.xml](../../app/backend/pom.xml), [BackendApplication.java](../../app/backend/src/main/java/com/elogistic/backend/BackendApplication.java) | `PARTIAL`: một artifact `com.elogistic:backend:0.0.1-SNAPSHOT`, Java 21, Spring Boot parent **4.1.1**; chỉ Web MVC và Web MVC test starters. Build/runtime `UNVERIFIED`. |
| Build backend | [wrapper properties](../../app/backend/.mvn/wrapper/maven-wrapper.properties) | Wrapper **3.3.4**, distribution Maven **3.10.0** khai báo. `mvnw.cmd --version` FAIL trước Maven do null-array; Maven hệ thống 3.9.12 nhận diện được nhưng `mvn.cmd -o verify` FAIL vì parent thiếu trong cache. |
| Backend config | [application.properties](../../app/backend/src/main/resources/application.properties) | Chỉ `spring.application.name=backend`. Không profile local, datasource, security, port hoặc health/readiness tùy chỉnh. |
| Web độc lập | [app/frontend/web](../../app/frontend/web) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không manifest, lockfile, entry point hoặc build tooling. |
| Mobile | [package.json](../../app/frontend/shipper-mobile/package.json), [App.tsx](../../app/frontend/shipper-mobile/App.tsx), [index.ts](../../app/frontend/shipper-mobile/index.ts), [app.json](../../app/frontend/shipper-mobile/app.json) | `PARTIAL`: Expo starter đăng ký `App`, màn hình mẫu; package/name/slug hiện là `web` dù nằm ở `shipper-mobile`. Chưa có luồng shipper. |
| Mobile stack | [package-lock.json](../../app/frontend/shipper-mobile/package-lock.json), [tsconfig.json](../../app/frontend/shipper-mobile/tsconfig.json) | npm lockfile v3; Expo **57.0.27**, React/React DOM **19.2.3**, React Native **0.86.3**, TypeScript **6.0.3**, `@types/react` **19.2.18** theo lock. Typecheck local PASS; Expo/Android runtime `UNVERIFIED`. |
| Mobile scripts | `start`, `android`, `ios`, `web` trong manifest | Gọi `expo start` với tùy chọn tương ứng. Không script build/lint/typecheck/test, Expo Router dependency, native project hoặc EAS config. Expo web preview không thay web nghiệp vụ độc lập. |
| PostgreSQL/Flyway/seed | [db/migration](../../app/backend/src/main/resources/db/migration), [infra/postgres](../../infra/postgres) | `NOT_IMPLEMENTED`: chỉ `.gitkeep`; không SQL, JDBC driver, persistence/Flyway dependency, migration plugin, datasource hoặc seed script. Chưa migration strategy hoạt động. |
| Compose/cache/broker/storage | [infra](../../infra), POM và source | `NOT_IMPLEMENTED`: không Compose YAML, service/profile/image/healthcheck hay adapter Redis/RabbitMQ/POD. `.env.example` chưa nối vào ứng dụng. |
| Contracts/client | [package/api-client](../../package/api-client), `specs/openapi/`, `specs/events/` | Client chỉ `.gitkeep`, thư mục contracts cũ không có schema; `contracts/` chưa có. Không controller/API nghiệp vụ, schema hoặc generated client. |
| Backend tests | [BackendApplicationTests.java](../../app/backend/src/test/java/com/elogistic/backend/BackendApplicationTests.java) | `IMPLEMENTED` ở phạm vi test context-load duy nhất; `NOT_RUN` vì build chặn trước Surefire. Không assertion nghiệp vụ/DB integration/architecture test. |
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
| GAP-03 | High | Wrapper lỗi; offline verify không resolve parent 4.1.1 | DEC-01; REQ-001-01/05 | Xác minh wrapper/dependency resolution với cache/network phù hợp; không tự hạ version hoặc coi cache miss là test fail. |
| GAP-04 | High | Không Compose/DB/health/seed; template env không consumer | AC-001-01/02/04/05, AC-06; NFR-O02/O03 | Chốt version/wiring cần cho slice, thêm hạ tầng có smoke/migration evidence. Không cài broker/cache chỉ để đủ sơ đồ. |
| GAP-05 | High | Mapping chưa đủ nine domains; architecture test chỉ `.gitkeep` | ADR-0001; REQ-001-03; NFR-M01 | Review mapping/public API; test phải fail khi tạo boundary violation. Giữ backend hiện có. |
| GAP-06 | High | Web chỉ thư mục; mobile starter thiếu native scan/POD/GPS/offline | AC-01/02; FR-S01–05; DEC-02/15/16/18/23 | Xác minh starter, native spike sau quyết định; tạo web khi task/baseline được duyệt. |
| GAP-07 | High | Không approved contracts/client, CI, integration/concurrency/E2E/fault tests | REQ-001-04; AC-04/07; DoR/DoD | Review DEC-25/27, ADR-0003; test cùng slice và lưu evidence thật. Context-load không chứng minh invariants. |
| GAP-08 | Medium | Blueprint dùng `apps/`, `packages/`; code dùng `app/`, `package/` | NFR-O03; kế hoạch/runbook cũ | Dùng đường dẫn thực trong hai file; review docs/spec khác ở task riêng, không ép chuyển thư mục. |
| GAP-09 | Medium | Không DOCX tại repo, runtime/evidence chưa có | Thứ tự thẩm quyền, AC-07; DoD | Cần bản báo cáo khi review nghiệp vụ; cập nhật traceability theo evidence trong phiên có phạm vi cho phép. |
| GAP-10 | Low | Mobile package/name/slug là `web`, routes rỗng chưa Router | Khả năng hiểu cấu trúc local | Ghi rõ runbook; đổi metadata/navigation khi có task, không viết lại starter để sửa tên. |

## D. Ưu tiên Day 1 và checklist thực thi

Checklist assessment này không thay checklist feature. Task đầu tiên chưa tick là **D1-02**; task đầu tiên của feature 001 vẫn **T-001-01 — Review**, chưa đạt điều kiện hoàn thành.

- [x] **D1-01 — An toàn repository và kiểm kê baseline** — ưu tiên Critical; phụ thuộc: không. File: `AGENTS.md`, manifests, source/config, docs/specs. Gate: ghi branch/HEAD/dirty state, executable/placeholder mà không hủy thay đổi. Bằng chứng: `git status --short`, `git branch --show-current`, `git rev-parse HEAD`, `git ls-files`, `rg --files --hidden --no-ignore` với loại trừ dependency/build/Git; bảng B đã lập. Chỉ audit hoàn thành, không phải foundation.

- [ ] **D1-02 — Xác minh môi trường/toolchain** — ưu tiên High; phụ thuộc D1-01. File: wrapper properties/POM, mobile manifest/lock/app.json. Gate: wrapper chạy distribution đã review; JDK/Node/npm/native target có version và spike evidence DEC-01/02. Ban đầu `PARTIAL/BLOCKED`: tools nhận diện được, mobile typecheck PASS; wrapper lỗi, Docker không trên PATH, Expo/Android runtime chưa chạy.

- [ ] **D1-03 — Xác minh build/test/migration** — ưu tiên High; phụ thuộc D1-02, quyết định DB cho migration. File: POM, backend `src/test`, `src/main/resources/db/migration`, `infra/`. Gate: `verify` chạy test thật và report; migration cold-start/upgrade/re-run trên DB thử có evidence sau khi có SQL/config. Ban đầu `BLOCKED`: parent offline thiếu, context test `NOT_RUN`, Flyway chưa có. Không migrate vào DB dùng chung.

- [ ] **D1-04 — Review kiến trúc/bảo mật** — ưu tiên Critical; phụ thuộc D1-01, automated gate cần D1-03. File: packages backend, test `architecture/`, `.env.example`. Gate: mapping nine domains review, test bắt internal import/cycle, auth/object scope và secrets/log checks có evidence. Ban đầu `PARTIAL`: audit source đã có; module/security/tests chưa có, chưa secret-scanner executable, không tuyên bố security PASS.

- [ ] **D1-05 — Bổ sung foundation theo gap duyệt** — ưu tiên High; phụ thuộc D1-02/03/04 và T-001-01; có thể tách spike độc lập để tháo blocker. File: backend hiện có, migration/config, `infra/compose/`, web/mobile và tests liên quan; chỉ lập diff cụ thể sau review. Gate: thay đổi nhỏ bảo vệ REQ-001/AC liên quan với test/runtime evidence, không tái sinh project/chín nghiệp vụ stub. Ban đầu `[PROPOSED]/BLOCKED`: DEC-01/02/26 và contract review. Redis/RabbitMQ chỉ thêm khi cần cache/async; vẫn cần đủ hạ tầng trước nghiệm thu toàn feature 001/P0.

- [ ] **D1-06 — Chọn slice và kiểm tra DoR** — ưu tiên High; phụ thuộc foundation cần thiết VERIFIED, D1-04/05. File: `specs/001-platform-foundation/{spec,design,tasks}.md`, sau đó `specs/002-identity-access/{spec,design,tasks}.md`. Gate: reviewer chốt blocker/contract/fixture, public dependency rõ. Ban đầu `BLOCKED`; Identity chưa implement nên là business slice ứng viên đầu tiên sau foundation, không phải triển khai lại auth đã có.

- [ ] **D1-07 — Test-first và gate slice** — ưu tiên High; phụ thuộc D1-06. File: test/source owner được chọn, contract đã review, UI phù hợp. Gate: happy/negative/auth/replay/concurrency/failure tests cần thiết PASS trên môi trường công bố; build/migration/architecture/API đạt. Ban đầu `NOT_IMPLEMENTED` cho business slice; typecheck starter không thay gate này.

- [ ] **D1-08 — Chốt kết quả/evidence/blocker** — ưu tiên Medium; phụ thuộc task đã thực hiện. File: hai tài liệu này; phiên triển khai sau còn feature tasks, traceability/ADR nếu design change. Gate: command/result/commit/environment cho từng gate, task chưa đạt vẫn mở, dependency/next task rõ. Ban đầu `PARTIAL`: audit/metadata đã ghi, runtime baseline chưa đạt. Không sửa traceability/ADR/specs trong phiên giới hạn hai file.

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
| Build | JDK/Wrapper/dependencies truy cập được | Backend `.\mvnw.cmd verify`; mobile `node .\node_modules\typescript\bin\tsc --noEmit --incremental false`; web theo scripts thật sau manifest | Wrapper FAIL; offline Maven FAIL trước test; mobile typecheck PASS; web chưa có. |
| Unit/context/integration | Test tồn tại; DB thử riêng khi persistence | Exit 0/report thật, assertions theo BR/transaction/constraint | Context-load `NOT_RUN`; domain/DB tests chưa có. |
| Architecture | Mapping/boundary policy review | Test bắt internal import/cycle/cross-owner write, PASS khi code hợp lệ | Chưa test/dependency; rà starter không chứng minh nine domains. |
| Migration | Flyway/driver/config/SQL/DB/version/credentials chốt | Empty/upgrade/re-run an toàn; Hibernate validate nếu ORM dùng; runtime không DDL | `NOT_IMPLEMENTED`, không CLI/plugin command hiện hữu. |
| API/authz | Approved schema/auth decision/account-scope fixtures | Contract/unauthorized/cross-tenant/revocation tests PASS, không lộ dữ liệu | Chưa có; `/api/v1`, `/actuator/health` vẫn đề xuất. |
| Business invariants | Owner/transaction/lock/unique/check/idempotency thiết kế | Happy/negative/duplicate/concurrent/timeout/restart/rollback PASS, không vi phạm BR trong kịch bản công bố | `NOT_IMPLEMENTED`, không mock tiền/tồn để thay gate. |
| Native/E2E/CI | Device/browser target công bố, harness có | UI đúng server state, permission/offline/restart thật, CI/evidence | Chưa harness/workflow; runtime `UNVERIFIED`. |

## G. Rủi ro, blocker và quyết định

| Loại | Nội dung | Điều kiện tiếp tục |
| --- | --- | --- |
| Chấp thuận theo báo cáo | [ADR-0001](../architecture/decisions/ADR-0001-modular-monolith.md): một backend, Package by Domain, local ACID, một PostgreSQL với ownership | Giữ định hướng; boundary/atomicity tests chứng minh. |
| Chấp thuận một phần | [ADR-0002](../architecture/decisions/ADR-0002-database-ownership.md): Flyway/ownership theo báo cáo, migration details PROPOSED | Review DEC-26, DB version/stream/order và runtime/migration accounts trước DDL. |
| Đề xuất chưa duyệt | [ADR-0003](../architecture/decisions/ADR-0003-contract-and-idempotency.md), DEC-25/27/28 | Review API/error/idempotency/event/workflow; không chọn wire format/provider bằng phỏng đoán. |
| `[BLOCKING]` gần nhất | DEC-01/02: wrapper/build/native baseline; DEC-03/04: auth; package mapping chưa rõ | Owner/reviewer và evidence trong [decision register](../decisions/open-questions.md); source version không tự đóng decision. |
| `[BLOCKING]` theo domain | DEC-05–07/09–13/15–17/19/22–24, CONFIG chưa được duyệt | Chốt theo slice, provider theo decision; không bắt mọi provider chốt trước spike độc lập. |
| Giới hạn môi trường | Windows 11/PowerShell 5.1; wrapper null-array, cache parent thiếu, Docker không trên PATH; native/runtime chưa quan sát | Môi trường đủ tool/cache/network/device, command/report thật. Chưa kết luận version không tồn tại hoặc feature test lỗi. |
| Giới hạn nguồn | Chưa DOCX tại repo; docs mới và `.gitkeep` bị xóa sẵn | Đối chiếu báo cáo khi review nghiệp vụ; giữ toàn bộ thay đổi, không checkout/reset để làm sạch. |

## H. Đầu ra kỳ vọng và điều kiện tiếp tục

Phiên tài liệu này cung cấp baseline có version/path/evidence, gap có mức độ, task/dependency/gate và runbook PowerShell có PASS/FAIL/NOT_RUN. Không tự tick feature DONE hoặc sửa ADR/traceability ngoài hai file.

Phiên triển khai tiếp theo lập plan nhỏ cho **T-001-01** và D1-02, ghi owner/reviewer/decision, kiểm chứng wrapper/build/native và duyệt phạm vi gap cần sửa. Khi baseline/DoR đủ, thực hiện một vertical slice thật với test trước hoặc cùng logic; ghi REQ → AC → task → test/evidence vào [traceability](../quality/traceability.md), cập nhật ADR nếu design change, báo lệnh pass/fail và next task. Nếu blocker còn mở, báo evidence spike độc lập và giữ business task chưa hoàn thành.
