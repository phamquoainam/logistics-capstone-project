# E-Logistics Management Platform — Spec Roadmap V2.1 (Optimized Proposal)

> **Phiên bản:** `V2.1 / 2026-10-26` · **Trạng thái:** `PROPOSED — RECONCILIATION REQUIRED` · **Loại tài liệu:** Roadmap / đề xuất quản trị SDD, **KHÔNG PHẢI** canonical spec hoặc bằng chứng runtime acceptance.\
> **Phạm vi mục tiêu:** 33 Role/Actor/Principal (`R01–R33`), 18 module (`M01–M18`), 371 capability entries (`BC-001–BC-371`). Có 366 mục `CORE` và 5 mục `DEFER` trong matrix hiện tại; `DEFER` ở đây có thể là **hành vi bị cấm**, không được biến thành backlog chức năng.\
> **Nguồn chính:** `E_Logistics_Business_Complete_GroupEF_Full.md` (đặc biệt VIII.8.9–8.12); `E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md`. **Nguồn hiện trạng Spec:** danh sách folder 001–016 và demo do Owner cung cấp qua ảnh; **chưa đọc nội dung từng folder hay source repository**.\
> **Nguyên tắc chứng cứ:** Owner báo cáo đã thực hiện `001`, `002`, `demo-001`, nhưng `implemented/reported ≠ fully accepted`. Chỉ kết luận `VERIFIED`/`RELEASED` từ test/runtime/CI receipts hợp lệ.

## 0. Quyết định điều hành: dùng V2.1 thế nào

**Phán quyết đề xuất: `GO WITH CONDITIONS` đối với lộ trình; `NO AUTOMATIC CODE CHANGE`.**

- Giữ nguyên **30 logical Spec** (001–030) từ V2 ở vòng hoạch định này; 001–002 là baseline, 003–030 là plan. Con số 30 **không phải bất biến**: thay đổi chỉ khi kiểm kê thực tế chứng minh Spec quá lớn/quá nhỏ hoặc có xung đột trách nhiệm.
- Giữ `demo-001-auth-merchant-shipment` như **demo/regression artifact** riêng, không biến thành spec #31 và không coi là bằng chứng mọi nghiệp vụ đã đạt.
- Không `mv`, `rename`, xóa hoặc làm lại `specs/003–016` chỉ dựa trên tài liệu này; tên V2.1 trước hết là **logical names**, chưa phải folder canonical.
- Mọi `CORE` cần có **Spec owner + Acceptance + Evidence plan**, nhưng **không** phải toàn bộ 366 CORE phải hoàn thành trước Spec 003.
- Spec đã triển khai cần được **reuse/extend qua change control**; không viết lại Foundation/Identity để chạy theo sơ đồ mới. Bất kỳ change nào va chạm Identity migration/auth/session/CI phải review tác động riêng.
- Ưu tiên **đi đến một Shipment thực chạy được** qua từng milestone, đồng thời đóng các security/data gates đúng lúc; không để `audit`, `COD`, `custody` thành vấn đề “để cuối”.

### 0.1. Tiêu chí tối ưu hóa (weighted decision rubric)

| Tiêu chí | Trọng số | Điều kiện đánh giá |
|---|---:|---|
| Coverage và traceability | 20% | 371 BC entries có mapping; mỗi mục có owner/consumer/test status; không để orphan |
| Dependency và ownership | 15% | Có hard prerequisites, không circular dependency; một domain có một source of truth |
| E2E incremental delivery | 15% | Các mốc F1–F5 có hành trình test được và rollback/recovery rõ |
| Security và data/financial integrity | 15% | Tenant/assignment/custody/COD/ledger/SoD/audit fail-closed |
| Testability / acceptance | 10% | Positive + negative + concurrency + evidence mỗi Feature |
| Scope feasibility | 10% | Các Spec bounded, không vượt sức đồ án, không đòi mọi module xong trước khi demo |
| Compatibility/reuse | 10% | Không hỏng canonical IDs, migration, lịch sử Foundation/Identity/demo |
| Reliability/operations | 5% | Retry/idempotency, health/metrics, backup/recovery, release receipts |

**Chưa chấm điểm định lượng Roadmap** vì thiếu inventory repo, effort estimation và evidence. Những trọng số trên là khung lựa chọn đề xuất, không phải benchmark được kiểm chứng.

## 1. Các quyết định nghiệp vụ bất biến (Business Baseline)

1. `Guest` có public lookup, quote, coverage, station/service catalog, liên hệ hỗ trợ và đăng ký; **không được tạo Shipment trực tuyến khi chưa có account**.
2. `Retail Sender` là user cá nhân đăng ký, sở hữu Shipment của mình; có đầy đủ gửi lẻ, báo giá, thanh toán cước, pickup, tracking, hủy hợp lệ, hoàn, hỗ trợ, lịch sử/chứng từ.
3. `Merchant` là tổ chức có membership, Owner/Manager/Operator/Warehouse/Finance/Support và integration principal; không rò dữ liệu giữa Merchant A/B.
4. `Recipient` là **external actor**, không đăng ký/login/Portal riêng; được tra cứu công khai hạn chế và nhận hàng/COD/POD qua Courier; đơn Merchant phản ánh qua Merchant Support; đơn Retail Sender có luồng hỗ trợ trực tiếp E-Logistics phù hợp.
5. `Walk-in Sender` không cần tài khoản nhưng chỉ tạo Shipment qua **Station/Partner staff được cấp quyền** tại quầy, có xác minh/biên nhận/custody/thu phí theo chính sách. Không có public anonymous-create API.
6. `Order ≠ Shipment ≠ Parcel`; `Shipment Status ≠ Tracking Event ≠ Delivery Attempt ≠ Custody Event ≠ Financial Event`; `Quote ≠ Price Snapshot ≠ Fee Adjustment`; `COD Collected ≠ Remitted ≠ Reconciled ≠ Settled`.
7. Quyền = role permission + principal/org membership + resource scope + assignment/station/partner scope + current state + evidence/security condition. **Không** suy quyền từ việc có giao diện.
8. `POD`, cash receipt, approval, ledger, audit phải có actor, provenance và idempotency. Không sửa âm thầm sự kiện/bút toán; cấm self-assignment để tự giao, giả POD, tự xác nhận cash remittance hay cross-tenant access.
9. Toàn bộ 33 Role/Actor/Principal là **target business scope**; không đồng nghĩa 33 portal hoặc 33 user-account types. Các vai trò và quyền đặc thù có thể cùng trên một ứng dụng nhưng phải enforcement tại backend.

## 2. Cấu trúc điều hành: F0–F5 và các gate

| Wave | Spec logic | Kết quả sản phẩm | Gate bắt buộc để chuyển wave |
|---|---|---|---|
| **F0 — Baseline & Reconciliation** | 001–002 + demo; G0 docs-only | Giữ Foundation/Identity, crosswalk legacy, chốt owner/dependency và rủi ro | **G0:** inventory + scope delta + data/security contracts sớm + Owner sign-off; không có thay đổi runtime trái phép |
| **F1 — Customer to Booking** | 003–009 | Guest → đăng ký Sender/Merchant → coverage/quote → Shipment/Parcel/Label → tracking an toàn | **G1:** Sender và Merchant happy/negative path; quote snapshot, idempotency, privacy, membership được kiểm chứng |
| **F2 — Physical Logistics** | 010–016 | Pickup → Station → Sorting → Line-haul → Destination Station → Delivery/POD hoặc Return | **G2:** tracking/custody/attempt nhất quán, không có lost/duplicate custody; walk-in đúng quyền |
| **F3 — Money & Resolution** | 017–021 | COD remittance → ledger/settlement; ticket/claims và bồi thường có phê duyệt | **G3:** reconciliation không trùng, SoD, refund/claim evidence, mismatch có xử lý |
| **F4 — Enterprise & Governance** | 022–028 | Enterprise API, bulk import, partner, network, risk, platform admin, analytics/notifications | **G4:** full 33 role target coverage ở mức phù hợp; external integration, tenant/risk/IT/admin guard và SLA |
| **F5 — Reliability & Release** | 029–030 | Courier offline sync, toàn bộ regression và release/demonstration | **G5:** cross-module E2E, quality/security/NFR, deployment/recovery và provenance receipts |

**Gates dùng để phát hành tích lũy**, không buộc chờ G5 mới chạy E2E. Nếu một milestone không có test evidence, ghi `NOT_VERIFIED` chứ không gọi `PASS`.

## 3. Danh sách 30 Logical Spec — owners, prerequisite và acceptance

> `Hard deps` là **phụ thuộc hợp đồng/chức năng thiết yếu trước khi bắt đầu triển khai phần liên quan**, không nhất thiết phải đợi *mọi acceptance của Spec trước* mới viết tài liệu Spec sau. `Cross-cutting` ở mục 4 phải được chốt theo thời điểm sử dụng. Mã `Mxx` là module phân tích, chưa phải tên bounded context đã phê duyệt.

| ID | Logical Spec (giữ từ V2) | Module dẫn dắt / liên quan | Hard deps | Owner của kết quả | Exit acceptance tối thiểu |
|---|---|---|---|---|---|
| **001** | `platform-foundation` | M01, M18 | — | Runtime/CI/platform constraints | Baseline không hồi quy; runtime acceptance đánh giá theo evidence riêng |
| **002** | `identity-access` | M01, M18 | 001 | Identity/session/permission contracts | Xác thực, session, membership/permission scope theo canonical; không tuyên bố pass khi thiếu receipt |
| **003** | `sender-merchant-onboarding` | **M02**, M01 | 002 | Retail Sender profile, Merchant org/membership, staff delegation | Hai luồng đăng ký khác biệt; Merchant isolation; không tự cấp privileged internal role |
| **004** | `address-coverage-service-catalog` | **M03**, M04, M08 | 002 (public); 003 (merchant address flows) | Address, coverage, service/station catalog | Public coverage/station lookup; địa chỉ hợp lệ/phiên bản; station chỉ hiện khi phục vụ |
| **005** | `pricing-contracts-quotation` | **M04**, M02, M13 | 004; 003 (Merchant contract) | Rate cards, quotation, Merchant pricing, Sales proposal lifecycle | Quote Guest/Sender/Merchant đúng giá, date/version; không rò giá hợp đồng, price snapshot contract |
| **006** | `order-shipment-parcel-contract` | **M05**, M06, M18 | 003, 004; 005 (pricing contract) | Domain identity/state/transaction ownership & event contracts | Order/Shipment/Parcel riêng; status guard, duplicate protection, privacy/hold/cancel constraints |
| **007** | `shipment-booking-sender-payments` | **M05**, M13 | 005, 006 | Sender/Merchant booking, sender payment initiation & status | Hai kiểu người gửi; quote acceptance; payment retry/failure; không đúp Shipment/charge |
| **008** | `parcel-label-merchant-fulfillment` | **M06**, M02, M07 | 006, 007 | Parcel details/label, merchant pack & pre-handover manifest | Nhãn/scan/weight; kho Merchant không giả custody Logistics |
| **009** | `tracking-event-core-public-lookup` | **M11**, M18 | 006 | Tracking event stream, privacy-safe public read model | Tracking append-only; provenance; random code guessing không lộ PII/COD |
| **010** | `pickup-dispatch-execution` | **M07**, M11, M16 | 007, 008, 009 | Pickup pool/assignment/check-in/evidence/failure | `PICKED_UP` đúng assignment + evidence; no double assignment/scan |
| **011** | `station-intake-walkin-custody` | **M08**, M05, M13 | 007, 008, 009; 010 (courier handover flow) | Station custody, receiving/inventory, staffed walk-in booking | Custody 2 chiều; walk-in receipt & fee; staff/station scope |
| **012** | `sorting-hub-manifests` | **M08**, M06 | 011 | Sorting, bag/manifest, hub variance | Scan/bag/misroute/damage; manifest diff có evidence và approval |
| **013** | `linehaul-trip-hub-transfer` | **M09**, M08 | 011, 012 | Trip/driver/seal/transfers | Source–destination handover, quantity reconciliation, late/damage evidence |
| **014** | `delivery-dispatch-and-route` | **M10**, M09, M16 | 011, 013 | Delivery pool/assignment/route, leader oversight | Chỉ kiện ở đích hợp lệ mới OFD; reassignment & double loading bị chặn |
| **015** | `lastmile-attempt-pod` | **M10**, M11, M12 | 009, 014 | Delivery attempts/POD + COD collection event contract | DELIVERED/POD hoặc FAILED có attempt; payable COD phải thu/timestamp; không giả giao |
| **016** | `returns-reverse-logistics` | **M14**, M08, M10 | 011, 015 | Return case/custody/merchant receipt | Failed → retry/return; trả kiện có tracking & custody; cấm cancel như draft sau pickup |
| **017** | `cod-collection-cashier-remittance` | **M12**, M08, M18 | 015; financial interface baseline ở 006/007 | Courier cash collection, station cashier receipt/shift | Cashier kiểm nhận, thiếu/thừa và ca; courier không tự mark `REMITTED` |
| **018** | `billing-ledger-fees-invoices` | **M13**, M04, M18 | 005, 007, 017 (COD posting); invoice/fee phần không COD có thể trước | Invoice/fee adjustment, ledger/journal semantics | Immutable postings, idempotent charge/refund, snapshot giá chốt |
| **019** | `cod-reconciliation-merchant-settlement` | **M12**, M13 | 017, 018 | Period settlement/payout/reconciliation | Delivered ≠ settled; tie out tiền/fee, no duplicate payout, approval |
| **020** | `customer-support-tickets-sla` | **M15**, M11, M16 | 009; 015 (delivery escalations) | Case/ticket ownership, SLA, guest contact, Merchant Support handoff | Retail Sender tự ticket; Recipient Merchant qua Merchant Support; ticket không trực tiếp sửa POD/COD |
| **021** | `claims-investigation-compensation` | **M15**, M18, M13 | 016, 018, 020 | Claim/evidence/approval/compensation | Investigator ≠ approver ≠ payer by policy; immutable claim decisions/evidence |
| **022** | `enterprise-api-webhooks` | **M17**, M01, M05, M11 | 002, 005, 006, 007, 009 | Merchant-bound API credential, shipment integration/webhook | Scope, idempotency, webhook signature/retry, error reconciliation |
| **023** | `bulk-shipment-import` | **M05**, M17 | 006, 007; 022 (API sync only) | CSV/Excel/async bulk import and row exceptions | Mỗi dòng có trạng thái; replay-safe, lỗi một dòng không âm thầm hỏng toàn batch |
| **024** | `partner-franchise-station-ops` | **M17**, M08, M05 | 003 (org model), 011 | Partner organization & assigned station custody | Partner chỉ station/org scope, biên nhận & tiền/doanh thu điểm đúng quyền |
| **025** | `network-ops-routing-optimization` | **M09**, M03, M16 | 012–014 | Regional capacity/routes/exception reroute | Sự kiện điều chuyển có approval; route optimization không sửa tracking giả |
| **026** | `risk-fraud-compliance` | **M18**, M05, M12 | 006, 009, 015, 017, 019 | Fraud detection, hold/release & compliance case | Risk actor không sửa COD/POD/ledger; case, evidence, audit; hold enforcement có từ 006 |
| **027** | `platform-admin-audit-governance` | **M18**, M01, M02 | 002, 003; tiêu thụ contract 019, 026 khi có | Admin policy, privileged workflow, Auditor/IT Ops boundaries | Business Admin ≠ Identity Admin ≠ Super Admin ≠ IT ≠ Auditor; deny tests |
| **028** | `notifications-analytics-sla` | **M11**, M16 | 009, 019, 020; network analytics tiêu thụ 025 | Notifications, reporting, SLA dashboards | Event/outbox/retry, privacy scope, KPIs reconciliation with events/ledger |
| **029** | `courier-offline-sync` | **M07**, M10, M11 | 010, 015, 017 | Offline queue, revalidation, replay & conflict | Không bypass stale assignment/authorization, không duplicate POD/COD |
| **030** | `capstone-e2e-hardening-release` | **M01–M18** | 003–029 (theo scope release) | Cross-spec regression, deploy/ops/security readiness | Journeys + negative/evidence, restore/health checks, approvals phù hợp |

### 3.1. Không biến spec 027/028 thành “bảo mật/thông báo chỉ làm sau”

- `M18` authorization/audit ở mọi spec ghi dữ liệu nhạy cảm; 027 chỉ bổ sung quản trị và kiểm toán sâu.
- `M11` sự kiện hành trình và publish contract thuộc 009; thông báo tối thiểu theo yêu cầu của luồng được tích hợp ngay nơi cần (ví dụ pickup/delivery). 028 hoàn thiện template, kênh, escalation, dashboards.
- `M13` payment evidence và snapshot price tối thiểu phải tồn tại trong 005–007/011; 018–019 mới hoàn thiện ledger/invoice/settlement. **Nếu** nghiệp vụ cước gửi lẻ cần ledger posting tức thì, phải mở rộng giao diện/tối thiểu posting trước G1; không được nói “đợi 018” trong khi đã thu tiền thật.
- `M15` Guest hỗ trợ công khai cần entrypoint an toàn ở 004 hoặc 003/F1, có delivery mechanism tối thiểu (ví dụ contact request) và không đánh dấu ticket lifecycle hoàn tất trước 020.

## 4. Bốn hợp đồng xuyên module phải chốt sớm (không là feature mới)

| Contract ID đề xuất | Chủ hợp đồng / thời điểm | Bất biến được chốt | Consumer chính | Negative/consistency tests |
|---|---|---|---|---|
| **XC-IDENTITY-SCOPE** | 002 canonical + delta 003 trước code | Account vs Retail Sender vs Merchant membership vs Partner/staff vs API principal; session/MFA/privileged roles | 003, 007, 010, 011, 022, 024, 027 | Cross-tenant UUID, expired membership, self-elevation, impersonation bị từ chối |
| **XC-SHIPMENT-CUSTODY** | 006 domain + 009 events; trước pickup/Station | Order/Shipment/Parcel identity; shipment/status guards; custody transfers; attempts; evidence, station ownership | 008–016, 020, 021, 025 | Duplicate/foreign scan, reassign race, delivery without custody/POD, missing handover |
| **XC-PRICE-MONEY** | 005 price contract + 006/007 financial hooks; trước first real payment/COD | Quote/snapshot/charge vs COD; collection/remittance/ledger/reconciliation/settlement distinct; currency/rounding/fee version/idempotency | 007, 011, 015, 017–019, 021, 024 | Double charge, duplicate remittance, mispriced COD, payout before reconciliation |
| **XC-EVENT-AUDIT** | 001/002 hooks + 006/009 shared envelope; trước first state-changing workflow | Actor/principal, org/scope, reason, event time/received time, correlation/idempotency, immutable evidence, audit retention | 003–030 | Forged event, missing audit, replay, out-of-order, spoofed callback |

**Không tự tạo schema/table/service từ bảng này.** Mỗi contract cần **canonical owner**, approved ADR/schema/API nếu có thay đổi, consumer contract tests và traceability. Contract nhóm G0 có thể `DECISION_REQUIRED` thay vì kéo dài review vô hạn nếu change không blocking Feature 003.

### 4.1. Ma trận nguồn chân lý cho data ownership

| Domain / nguồn chân lý | Logical owner | Được dùng bởi | Nguyên tắc |
|---|---|---|---|
| Identity/session/permission | 002 (delta qua change control) | 003, mọi Role/Portal | Không duplicate IAM roles/credential store ở Merchant |
| Merchant org/membership & Retail Sender profile | 003 (gắn Identity contract) | 004, 007, 022, 027 | Ownership/membership nhất quán; đừng đồng nhất cá nhân và tổ chức |
| Address/service/coverage | 004 | 005, 007, 010, 025 | Snapshot/version cho shipment đã xác nhận |
| Rate card/quote/snapshot policy | 005 | 007, 018, 019 | Không hồi tố giá do sửa bảng giá |
| Order/Shipment/Parcel IDs & state constraints | 006 | 007–030 | Mỗi trạng thái có guard/actor/evidence |
| Booking/payment attempt | 007 | 011, 018, 022–023 | Idempotent booking, không charge lặp |
| Parcel/label physical measures | 008 | 010–016, 022, 024 | Dimension adjustments không giả sửa kiện lịch sử |
| Tracking event read model | 009 | 010–030 | Derived từ canonical event, không tái ghi state lịch sử |
| Pickup assignments & attempts | 010 | 011, 029 | Only assigned courier, assignment version |
| Station custody/handover/manifest primitive | 011 | 012–016, 021, 024 | Custody append-only evidence, no silent stock write |
| Sorting/Hub & linehaul manifest/trip | 012 + 013 | 014, 025 | Scan origin + destination reconcile |
| Delivery assignments & attempts/POD | 014 + 015 | 016–021, 029 | Dispatcher assign, Courier proves; không tự assign + complete |
| Returns/reverse custody | 016 | 020, 021 | Không cancel như draft sau pickup |
| COD cash remittance | 017 | 018, 019, 021 | Courier collected vs cashier received độc lập |
| Ledger/billing/invoice | 018 | 019, 021, 028 | Append-only posting, immutable financial audit |
| Settlement/payout | 019 | 021, 028 | Payout idempotent, reconciliation proof |
| Ticket/claims | 020 + 021 | 027, 028 | Ticket ≠ Claim; SoD cho bồi thường |
| API credentials/webhook | 022 | 023, 028 | Scoped Merchant principal; signed webhooks |
| Risk holds/admin policies | 026 + 027 | 006–030 | Enforce guard sớm; policy admin không bypass domain proof |

## 5. Chuyển đổi từ 16 Legacy Spec + demo (Crosswalk logic, KHÔNG di trú tự động)

| Folder hiện có do Owner cung cấp | Logical V2.1 target | Action tối thiểu | Bảo toàn |
|---|---|---|---|
| `001-platform-foundation` | 001 | `KEEP` | Baseline source, ADR, CI/runtime decisions |
| `002-identity-access` | 002 | `KEEP / CONTROLLED DELTA` | Contract, migration, security evidence |
| `003-merchant-address` | 003, 004 | `SPLIT-LOGICAL` | Merchant + address tasks/IDs, existing decision logs |
| `004-pricing-engine` | 005, 018 | `SPLIT-LOGICAL` | Rate, quote & billing obligations |
| `005-shipment-lifecycle` | 006, 007, 009, 015, 016 | `SPLIT-LOGICAL` | State machine, flows, events, API acceptance |
| `006-wallet-payments` | 007, 017, 018, 019 | `SPLIT-LOGICAL` | Financial decisions, payment adapters, reconciliation |
| `007-warehouse-wms` | 008, 011, 012, 024 | `SPLIT-LOGICAL` | Merchant WMS vs Station custody vs Hub sorting |
| `008-dispatch-hub` | 010, 011–014, 025 | `SPLIT-LOGICAL` | Dispatch/trip/station evidence |
| `009-shipper-pod` | 010, 015, 017, 029 | `SPLIT-LOGICAL` | POD/attempt/cash/offline policy |
| `010-routing-location` | 004, 014, 025 | `SPLIT-LOGICAL` | Coverage/address, operational/optimized routing |
| `011-cod-reconciliation` | 017, 018, 019 | `SPLIT-LOGICAL` | Cash receipt vs ledger vs payout |
| `012-notifications-dashboard` | 009, 020, 028 | `SPLIT-LOGICAL` | Tracking/notification/SLA/data read models |
| `013-admin-audit-configuration` | 026, 027 | `SPLIT-LOGICAL` | Audit/privileged controls/risk |
| `014-bulk-shipment-import` | 023 | `REUSE` | Import/async/error contract |
| `015-shipper-offline-sync` | 029 | `REUSE` | Offline/cache/sync policy |
| `016-research-experiments` | R&D track ngoài release critical path | `PRESERVE` | Research artifacts, accepted ADR outputs |
| `demo-001-auth-merchant-shipment` | Demo/regression baseline | `PRESERVE` | Demo scope, actual receipts; không suy luận production readiness |

**Reconciliation output bắt buộc:** một `legacy-to-v2-crosswalk.md` chứa mỗi `old_requirement_id/task/ADR/migration → logical owner → keep/extend/split/supersede → evidence`. Không rename trước khi Owner review; file trên chỉ là proposed mapping từ **tên folder**, chưa xác nhận các nội dung bên trong.

## 6. BC-001–BC-371 coverage: quy trình ánh xạ và chống orphan

> **Dữ liệu nguồn:** Ma trận GroupEF Full có **371 rows**, gồm **366 CORE + 5 DEFER**. Bộ file kèm `E_Logistics_Roadmap_V2_1_BC_Ownership_Draft.csv` là **draft assignment từ nội dung từng BC**, không phải hợp đồng đã phê duyệt hay bằng chứng triển khai. Mỗi hàng trỏ đến `primary_spec` và `module`/`role` từ nguồn; những điểm còn tranh cãi cần Owner/Tech Lead xử lý bằng decision log. **Không sử dụng draft mapping để tự sửa source code.**

### 6.1. Chuẩn hóa tracking cho từng BC

| Field | Quy tắc |
|---|---|
| `capability_id` | Giữ nguyên BC-xxx, không renumber |
| `role_id` | Giữ nguyên Rxx, external vs account vs integration principal |
| `module_id` | Giữ M01–M18 từ matrix |
| `primary_spec` | **Một** logical owner mặc định; consumers có thể nhiều |
| `mapping_status` | `PROPOSED` → `REVIEWED` → `APPROVED`; không tự nhảy `APPROVED` |
| `implementation_status` | `NOT_STARTED / PARTIAL / IMPLEMENTED / VERIFIED / BLOCKED / NOT_VERIFIED` |
| `sdd_reference`, `api_db_ui_test_evidence` | Điền sau khi kiểm kê repository; **chưa biết** không suy đoán |
| `acceptance_kind` | `POSITIVE`, `NEGATIVE/DENY`, `STATE_GUARD`, `FINANCIAL`, `AUDIT` hoặc hỗn hợp |

**Chú ý:** 5 mục `DEFER` có thể là **explicit deny assertions**; phải có negative test khi module tương ứng tồn tại. Không xóa `BC-006` (Guest anonymous booking), `BC-085` (Recipient account), hoặc các hành vi vượt quyền chỉ vì không phải chức năng UI.

### 6.2. Module → Spec owner(s) dự kiến

| Module | Logical owner dẫn dắt | Consumers / expansion | Điều kiện độ bao phủ |
|---|---|---|---|
| M01 Identity & Access | 002, 003 | 022, 027 | Account/org/permission guards; controlled delta |
| M02 Merchant Management | 003 | 005, 008, 027 | Tổ chức, nhân sự và scope quản lý |
| M03 Address & Coverage | 004 | 020, 025 | Catalog version/coverage/đổi địa chỉ có phê duyệt |
| M04 Pricing & Quotation | 005 | 007, 018 | Price snapshot, contract/effective dating |
| M05 Order & Shipment Management | 006, 007 | 011, 022, 023, 026 | Entity identity/state owner, cancellation/hold |
| M06 Parcel & Label | 008 | 010–013, 022, 024 | Parcels, labeling, scan evidence |
| M07 Pickup Management | 010 | 008, 029 | Assignment/attempt; duplicate prevention |
| M08 Station & Hub Operations | 011, 012 | 013, 016, 024 | Custody, inventory, manifest and exceptions |
| M09 Transportation Management | 013, 014 | 025 | Trip, linehaul and routing |
| M10 Last-mile Delivery | 014, 015 | 016, 020, 029 | Delivery assignment vs attempt/POD |
| M11 Tracking & Notifications | 009 | 020, 022, 028 | Append-only events, privacy and notification |
| M12 COD Management | 015, 017 | 019, 026 | Collected / remitted / reconciled / settled distinct |
| M13 Billing & Settlement | 007, 018, 019 | 021, 024 | Sender payment vs ledger vs settlement |
| M14 Returns & Reverse Logistics | 016 | 020, 021 | Return custody và phí hoàn |
| M15 Customer Support & Claims | 020, 021 | 026, 027 | Ticket vs claim vs approval |
| M16 Analytics & SLA | 028 | 010, 020, 025 | KPI/SLA đáng tin cậy, đúng scope |
| M17 Partner & Integrations | 022, 024 | 023, 028 | Tenant-bound API/partner scope |
| M18 Security & Audit | 001, 002, 027 | Tất cả Spec, 026 | Không trì hoãn security enforcement |

## 7. Acceptance journeys (release regression, không phải mock demo)

| Journey | Role/Actor | Spec trọng tâm | Phải chứng minh |
|---|---|---|---|
| **J1 Retail Sender** | R01, R18, R07, R08, R09 | 003–015, 018 | Cá nhân đăng ký→quote→shipment→cước→pickup→POD→tracking/chứng từ |
| **J2 Merchant** | R02/R03/R19/R20/R11 | 003, 005–010, 019, 023 | Tổ chức, membership, nhân sự/kho, hàng loạt, settlement đúng tenant |
| **J3 Walk-in** | R01, R08, R33 khi có | 011, 024 | Staff tạo tại quầy, phí + biên nhận + custody; Guest online bị cấm |
| **J4 Last-mile exception** | R06, R09, R08, R21 | 014–016, 020 | Fail attempt, giao lại/hoàn và handover không mất dấu |
| **J5 COD/Finance** | R09, R12, R13, R11 | 015, 017–019 | Money custody, ledger, recon, payout với idempotency |
| **J6 Claims** | R10, R21, R14, R15, R13 | 020–021 | Recipient Merchant qua Support, claimant evidence và approval/pay SoD |
| **J7 Integration/Partner** | R22, R33, R32 | 022–024 | API scope/retry/webhook, partner point, không cross-tenant |
| **J8 Fraud/Governance** | R30, R04, R05, R31, R17 | 026–027 | Risk hold/release, privileged roles separated, auditor read-only |
| **J9 Offline courier** | R07, R09 | 029 | Replay duplicate/stale auth/assignment không tạo COD/POD/custody sai |

**Luôn test thêm:** sender↔merchant isolation, Station/Hub cross-scope, direct-URL/UUID enumeration, stale assignments, duplicate COD/refund, race conditions, missing/late event, request retry, fail/restore, tampered evidence, unexpected privileged escalation.

## 8. Spec Ready/Done, security gates và bằng chứng

### 8.1. Definition of Ready — trước code của từng Spec

1. Scope rõ: In/Out và actor R IDs; full target vs wave delivery subset; capability BC IDs ánh xạ đúng.
2. Canonical owner cho table/state/event/ledger; không có hai aggregate cùng ghi một sự thật.
3. Preconditions, postconditions, transitions, failure/cancellation/compensation paths; đầu vào xác minh đúng quyền.
4. Contract đã được duyệt: API schema, DB changes, data migration, event payload/outbox, idempotency semantics, error format.
5. Acceptance: positive, denial, concurrency, recovery, full or partial E2E; test data/fixtures, evidence location.
6. Dependencies cụ thể; threat/PII/COD risk; known undecided items có `decision owner` và hard blocker rõ.
7. Scope bounded: nếu Spec có nhiều domain owner độc lập, tách thành deliverable nhỏ trong Spec hoặc đề xuất split qua reconciliation.

### 8.2. Definition of Done — trước tuyên bố một Spec/Feature accepted

1. Approved requirement/ADR/traceability và change control đối với Foundation/Identity.
2. Backend/API/migrations/UI (nếu thuộc scope) + business state guards thực thi; không chỉ mock/placeholder.
3. Positive/negative/integration/E2E tests cần thiết pass ở môi trường phù hợp, log được provenance.
4. Audit, privacy, tenant/station/assignment controls, financial invariants và idempotency được kiểm tra.
5. CI/runtime evidence đúng phiên, không reuse receipts lỗi/stale; known blocker được đánh giá rõ.
6. Cập nhật BC statuses và SDD evidence; chỉ đánh `VERIFIED` khi có thực chứng, `BLOCKED` khi chưa được phép hoặc failed gate.
7. Có điểm rollback/compensation cho migration/event/cash không thể đảo ngược tùy ý.

### 8.3. Non-functional baseline phải có từ sớm

- Observability: correlation/request ID, structured logs không lộ secrets/PII, audit log immutable theo policy.
- Reliability: retry/backoff, outbox/inbox theo nhu cầu, de-duplication, dead-letter và reconciliation jobs.
- Security: rate limiting public lookups, enrollment/invite protection, staff provisioning, secret rotation, least privilege, approvals.
- Data lifecycle: retention, access limitation, backups/restore, history reconciliation, administrative geographic catalog versioning.
- Performance: đo latency/throughput theo workload Capstone thật; **ngưỡng số cụ thể chốt trong NFR/acceptance**, không tự đặt số tùy ý.
- Payment safety: không giả lập “paid” khi gateway/cashier chưa xác nhận; khi chưa có provider thật có thể dùng sandbox/mocks rõ nhãn và không coi đó là production settlement.

## 9. Governance khi migrate SDD thực tế (bounded reconciliation, tránh verification vô hạn)

1. **Inventory docs-only:** đọc toàn bộ `specs/001–016`, demo, requirement IDs/tasks/ADRs, migration histories, release/CI workflows; không chạy runtime nếu chưa được phê duyệt.
2. **Crosswalk:** `old_id → V2.1 logical owner → Mxx/Rxx/BCxxx → KEEP/EXTEND/SPLIT/SUPERSEDE → risks`. Mỗi ID cũ có trạng thái tồn tại hoặc tham chiếu; tuyệt đối không drop acceptance/history.
3. **Resolve only blocking conflicts** cho triển khai 003: đăng ký Sender/Merchant, merchant membership, address ownership, IAM auth contract. Những vấn đề COD/partner sâu có thể ra decision log, không cần chặn 003 trừ khi schema/event contract hiện tại xung đột trực tiếp.
4. **Review change delta:** xác định khác biệt giữa hợp đồng canonical đang có và target Business (không coi business reference thắng tự động); ADR/owner approval khi thay quyền/data/state.
5. **Approval & implementation:** Owner duyệt mapping, tên thực tế folder và patch plan; sau đó tiến hành từng Spec/slice, đánh evidence. Nếu folder cũ đã có phần đã code, reuse và chỉ bổ sung phần thiếu.
6. **Exit G0 có giới hạn:** `GO` nếu 003 Ready, không có security/schema blocker trực tiếp; `GO WITH CONDITIONS` nếu non-blocking gap có owner/milestone; `FIXES REQUIRED` khi contract xung đột nhưng sửa được; `SECURITY BLOCKED` khi nguy cơ vượt scope hoặc runtime gate chưa được phép.

### 9.1. Cần thu thập để chốt implementation-ready

- Nội dung repo `specs/001–016` + demo, `README`, dependency graph, ADR/canonical decisions.
- DB schema/migrations, Identity policy, Merchant/org model, existing API/event contracts, tests và evidence.
- Chỉ báo tiến độ thật cho từng Spec: `NOT_STARTED / IN_PROGRESS / IMPLEMENTED / VERIFIED / BLOCKED`.
- NFR định lượng và phạm vi demo release theo nguồn lực thực tế.

**Không thể xác nhận “tối ưu tuyệt đối” khi chưa có bốn nhóm chứng cứ này.** Roadmap V2.1 có cấu trúc để giảm rework và giúp bắt đầu Feature 003 an toàn, chứ không tự tạo trạng thái nghiệm thu.

## 10. Next action — đúng một bước ưu tiên

**Chạy `Roadmap Reconciliation Gate` chỉ tài liệu (docs-only), không sửa code:** đưa bản V2.1 và BC crosswalk draft vào Codex; đọc các Spec 001–016 + demo để kiểm kê hiện trạng, đối chiếu 371 BC, lập crosswalk legacy IDs và chỉ chốt những khác biệt blocking Feature 003. Yêu cầu đầu ra `GO / GO WITH CONDITIONS / FIXES REQUIRED / SECURITY BLOCKED` cùng **một** bước tiếp theo có mức ưu tiên cao nhất.

---

### Appendix A — Tài liệu dùng kèm

- `E_Logistics_Business_Complete_GroupEF_Full.md` — Business Reference và các quyết định mục VIII.8.9–8.12.
- `E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md` — 33 Role/Actor/Principal, 18 module, 371 capability entries và constraints.
- `E_Logistics_Roadmap_V2_1_BC_Ownership_Draft.csv` — mapping từng BC sang logical owner, **PROPOSED**, chưa có source SDD/test evidence.
- `E_Logistics_Spec_Roadmap_V2_Original_Backup.md` — lịch sử V2 không chỉnh sửa.

### Appendix B — Những giả định chưa thể xác minh

- Tên thư mục legacy là đúng như ảnh Owner cung cấp; **chưa xem** từng file.
- Completion của 001/002/demo dựa trên báo cáo Owner; acceptance/runtime phải xác minh riêng.
- Có thể tiếp tục dùng 30 logical Spec và ghép vào lịch sử canonical mà không đổi ID; cần chứng minh khi reconciliation.
- Không tự đặt tên database/table/microservice mới từ sơ đồ này; chỉ ghi contract needs.

### Appendix C — Prompt Codex cho Roadmap Reconciliation Gate (copy nguyên khối)

```text
ROLE: Technical Project Lead + Security/Architecture Reviewer for E-Logistics Capstone.
MODE: DOCS-ONLY / READ-ONLY. No code, schema, folder rename, permission change,
CI/runtime execution, or fabricated test receipt. No runtime approval implied.

INPUTS:
- E_Logistics_Spec_Roadmap_V2_Proposed.md (V2.1)
- E_Logistics_Roadmap_V2_1_BC_Ownership_Draft.csv (all rows PROPOSED)
- E_Logistics_Business_Complete_GroupEF_Full.md
- E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md
- Canonical repository: specs/001–016, demo-001, ADRs, requirements, tasks,
  schemas, APIs, migrations, tests and existing evidence.

TASK:
1) Inventory real files and authoritative decisions; distinguish REPORTED vs VERIFIED.
2) Preserve 001/002/demo and legacy ID history; never infer acceptance from a folder name.
3) Reconcile proposed logical Spec 003–030 with existing canonical folders.
4) For all BC-001–BC-371, review/confirm primary Spec owner, dependencies,
   permission/guard, acceptance, implementation status, evidence; mark disagreements.
5) Identify one source of truth for Identity, Merchant membership, Shipment/Parcel,
   Pricing snapshot, Custody, Tracking, Delivery/POD, COD ledger and Settlement.
6) Classify gaps as BLOCKING_FOR_003, DEFERRED_WITH_OWNER, or NO_GAP.
7) Produce docs-only crosswalk, dependency graph, Feature 003 readiness and
   bounded proposal for later Spec split/rename if actually necessary.

OUTPUT:
- Verdict: GO / GO WITH CONDITIONS / FIXES REQUIRED / SECURITY BLOCKED.
- Source-evidenced gap table with file/line/requirement references, no inventions.
- Old → V2.1 → BC → tests/evidence traceability and ownership decisions.
- Exactly ONE highest-priority next step.
DO NOT silently modify canonical SDD or perform runtime execution.
```
