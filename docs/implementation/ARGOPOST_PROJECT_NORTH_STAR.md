# ARGOPOST — PROJECT NORTH STAR

> **OWNER DIRECTIVE · PROJECT-WIDE · PORTAL-NEUTRAL · READ FIRST**  
> **Tôn chỉ:** **E2E LOGISTICS FIRST — COMPLETE THE REAL SHIPMENT FLOW BEFORE NONCRITICAL FEATURES.**  
> **Đối tượng áp dụng:** mọi task/code change tại Public Website, Customer Portal, Carrier Operations, Courier Mobile, Carrier Business & Finance, Administration Console, Partner Workspace (nếu dùng), Backend, Identity, Infrastructure, DB, CI và tích hợp liên quan.  
> **Đường dẫn chuẩn đề xuất:** `docs/implementation/ARGOPOST_PROJECT_NORTH_STAR.md`.  
> **Trạng thái:** nguyên tắc ưu tiên cấp dự án; **không tự tạo quyền triển khai, quyền DB, quyền tài khoản hoặc quyền deploy**.

## 0. READ-FIRST CONTRACT — BẮT BUỘC TRƯỚC MỌI NHIỆM VỤ

Trước **mỗi nhiệm vụ mới**, kể cả khi đổi portal, đổi domain hoặc bắt đầu phiên Codex mới:

1. Đọc toàn bộ file này tại đường dẫn hiện hành trong repository, sau đó đọc `AGENTS.md` có hiệu lực tại root và trong các thư mục liên quan, cùng các ADR, contracts, approved manifests/policies thuộc phạm vi được giao.
2. Xác định **nhiệm vụ hiện tại theo yêu cầu rõ ràng mới nhất của Owner** (Mục 5). Không để ví dụ, checkpoint cũ hoặc tên module trong tài liệu này tự trở thành task mới.
3. Kiểm kê chức năng đã có, trạng thái code/DB/API/UI, canonical data sources, permissions và PASS/FAIL evidence trước khi thay đổi.
4. Phân tích tác động đến toàn bộ **Quote → Booking → Pickup → Network → Delivery/Return → Finance → Support/Claim** rồi mới chọn giải pháp.
5. Thực hiện đúng phạm vi và quyền đã được phê duyệt; kiểm thử dựa trên rủi ro; ghi rõ kết quả và dependency chưa được giải quyết.

**Quy tắc phiên mới:** không mặc định Codex nhớ các chỉ thị trong cuộc chat trước. `AGENTS.md` phải neo đường dẫn file này và yêu cầu READ FIRST. Nếu file không đọc được, phải báo rõ, không đoán nội dung.

**Thứ tự áp dụng:** yêu cầu Owner mới nhất xác định *task*, tài liệu này định hướng *mục tiêu và ưu tiên*; `AGENTS.md`, ranh giới bảo mật, quyền được cấp và approved technical/business contracts kiểm soát *những gì được phép làm*. Khi xung đột, không tự bỏ qua kiểm soát; nêu rõ xung đột và tiếp tục phần độc lập an toàn.

---

## 1. MỤC TIÊU TỐI THƯỢNG CỦA TOÀN DỰ ÁN

ArgoPost phải vận hành được **một vòng đời vận đơn thực tế, xuyên portal, trên dữ liệu canonical**, chứ không chỉ là tập hợp giao diện, module, API hay test PASS rời rạc.

### 1.1. Hành trình E2E đích

1. **Public Website → Customer Portal:** khám phá dịch vụ → đăng ký/xác minh/đăng nhập → kiểm tra vùng phục vụ → quote → booking → quản lý Shipment/Parcel → theo dõi hành trình, thanh toán và hỗ trợ.
2. **Carrier Operations:** tiếp nhận yêu cầu → pickup/acceptance handoff → Station/Hub intake → scan/inventory/custody → sort → manifest/dispatch → line-haul/handover → xử lý exception và return.
3. **Courier Mobile:** nhận nhiệm vụ pickup/delivery → thu/nhận hàng và xác nhận thực tế → delivery attempts → Delivered + POD hoặc Failed Delivery → retry/return → xác nhận bàn giao; COD collection proof nếu được hỗ trợ.
4. **Carrier Business & Finance:** confirmed fees/payment, COD obligations nếu có → thu/nhận tiền → remittance → reconciliation → settlement/exception; CSKH tiếp nhận Ticket/Claim → điều tra bằng sự kiện gốc → giải quyết và thông báo.
5. **Administration Console — Control Plane:** quản lý Merchant/Organization, dịch vụ, coverage, pricing, Station/Hub, Identity/RBAC/scope; oversight Shipment/SLA/Finance/Claims; approval theo ủy quyền, audit, monitoring và health. Admin **không làm thay** scan/custody/POD/ledger của domain owner.

Các nhánh trên **không phải chuỗi tuần tự theo thứ tự triển khai**: Admin và Identity hỗ trợ xuyên suốt; Ops, Courier, Finance và Customer có các đường phụ thuộc hai chiều. Customer **cá nhân** có thể gửi hàng không cần Merchant Approval; Business Customer cần Organization được kích hoạt khi dùng năng lực Business.

### 1.2. Định nghĩa thành công

**Success ≠ hoàn thành 23 Admin modules hay toàn bộ menu của một portal.**  
**Success = vận đơn hợp lệ đi qua mọi bước bắt buộc với trạng thái nhất quán, phân quyền đúng, evidence nguồn và kết quả tiền/ticket có thể kiểm chứng.**

---

## 2. NGUYÊN TẮC CHỌN VIỆC: E2E FIRST, KHÔNG PORTAL FIRST

Trước mỗi feature, tự trả lời:

1. Có trực tiếp mở khóa một bước trong E2E hoặc bảo vệ tính đúng đắn/an toàn bắt buộc không?
2. Bước này đã có implementation và **PASS evidence còn hợp lệ** chưa?
3. Đây là **canonical owner** hay chỉ là UI/consumer/oversight đọc dữ liệu của owner khác?
4. Thiếu contract, dữ liệu thật, scope, nhân sự, station, pricing hay event nào khiến bước E2E chưa chạy được?
5. Có giải pháp tối thiểu, backward-compatible, đúng authority giúp chốt luồng nhanh hơn không?

### 2.1. Nhãn ưu tiên và trạng thái thống nhất

- `P0_E2E_BLOCKER`: thiếu hoặc sai khiến hành trình E2E không chạy an toàn/đúng nghiệp vụ.
- `P0_REQUIRED`: điều kiện nền tảng bắt buộc (Identity, service/coverage, pricing, station, custody, audit/security, finance integrity...).
- `P0_DEPENDENT`: cần nhưng domain owner khác chưa có source/contract hoặc phải triển khai cùng domain đó.
- `P1_ENHANCEMENT`: tăng mức độ vận hành, tối ưu và quản trị sâu nhưng không cản luồng E2E được chọn.
- `EXISTING_VERIFIED`: đã có, evidence hợp lệ, không cần code/test lại nếu không bị tác động.
- `MISSING_APPROVED`: được duyệt nhưng chưa triển khai.
- `BLOCKED_CONTRACT`: chưa có authority/API/schema/policy được duyệt; không tự mở quyền hay dựng nguồn giả.
- `UNAVAILABLE`: UI hiển thị rõ khi canonical source chưa có; **không** coi là dữ liệu bằng 0.

**Ưu tiên:** `P0_E2E_BLOCKER` > `P0_REQUIRED` > `P0_DEPENDENT` có thể unblock > `P1_ENHANCEMENT`, trong khuôn khổ nhiệm vụ Owner giao và quyền đã được phê duyệt. Không tự chuyển sang portal khác nếu Owner đã giới hạn task chỉ ở một hạng mục; ghi rõ dependency và đề xuất bước tiếp theo.

### 2.2. Tối ưu thực chất

- Không xây lại các luồng đã PASS; không tạo duplicate workflow, database facts, approval engine, mail sender, state machine hay KPI source.
- Không thêm UI nâng cao chỉ để làm đầy menu khi nghiệp vụ gửi hàng vẫn bị chặn.
- Không tự mở rộng `P0` để bao gồm mọi chức năng trong đặc tả; P0 là **phần nhỏ nhất đủ đúng để E2E chạy và an toàn**.
- Không tạo Shipment/POD/custody/finance/claim state từ mock/hardcode trong môi trường nghiệp vụ. Synthetic fixtures chỉ dùng trong test cô lập.

---

## 3. KIẾN TRÚC, DOMAIN OWNERSHIP & RANH GIỚI PORTAL

| Khu vực | Domain responsibility | Không được giả quyền/domain khác |
|---|---|---|
| **Public Website** | giới thiệu dịch vụ, CTA/điểm truy cập Customer | không tự xác thực hoặc lưu quyết định vận đơn |
| **Customer Portal** | Identity self-service, quote, booking, tracking, ticket, Business Workspace | không tự xác nhận Carrier Accepted/POD hoặc Merchant approval |
| **Carrier Operations (`/ops`)** | Station/Hub, inbound, scan, custody, sorting, dispatch, manifest, transfer/return | không tạo ledger, COD settlement hoặc Admin grant |
| **Courier Mobile** | pickup, attempt, delivery/POD, failed delivery, return handover và cash collection evidence khi được phép | không tự sửa bảng giá, ledger hoặc approval policies |
| **Carrier Business & Finance (`/finance`)** | CSKH/Claims, fee collection, COD, reconciliation, payout/settlement | không giả event tracking/custody/delivery |
| **Administration Console (`/admin`)** | governance, approval, configuration, IAM oversight, cross-domain reads, audit | không tạo scan/POD, chỉnh trực tiếp ledger hoặc gộp quyền các realm |
| **Partner Workspace (nếu triển khai)** | tác nghiệp được ủy quyền tại site/scope riêng | không mặc nhiên có Carrier/Admin authority |

- Giữ kiến trúc **Internal Web Application** gồm `/admin`, `/ops`, `/finance` nhưng **không** tự union permissions hoặc login contexts. SYSTEM/STAFF/Customer identities và sự cấp quyền phải tuân thủ approved contracts.
- Admin giữ nguyên **9 menu cha** và **ADM-01–ADM-23** theo full specification; không tự đổi menu/module IDs. Điều này là **quy tắc riêng cho Admin**, không biến Admin thành ưu tiên mặc định của toàn dự án.
- Các portal khác giữ nguyên menu/modules theo **đặc tả riêng tương ứng**; không áp đặt taxonomy Admin sang `/ops`, `/finance`, Customer hay Mobile.
- **Source of truth theo domain:** mỗi mutation và event phải do domain được giao sở hữu. Consumer dùng approved public ports/contracts; không đọc bảng private chéo domain hay nhân bản dữ liệu canonical chỉ để làm UI.
- **State/finance invariants:** Booking ≠ Accepted; Parcel ≠ Shipment; Delivery Attempt ≠ Delivered; COD expected ≠ collected ≠ remitted ≠ reconciled ≠ settled; UNPAID ≠ revenue. Mỗi thao tác cần actor, scope, event/version, audit và giới hạn idempotency/concurrency phù hợp.
- Thay đổi service, route, tariff và station phải versioned/effective-dated, không hồi tố làm sai quote/booking snapshot hoặc làm mất custody đang mở.

---

## 4. E2E CRITICAL PATH — ƯU TIÊN XUYÊN MỌI PORTAL

Bảng này là **thứ tự dependency**, không phải yêu cầu làm lại code hoặc tuần tự cứng nhắc. Nếu một ô đã PASS, bỏ qua và đi tới blocker tiếp theo.

| Cổng nghiệp vụ / kết quả cần chạy được | Nguồn sở hữu / nhóm portal chính | Các yếu tố bắt buộc |
|---|---|---|
| **A. Identity & access** | Identity; Customer; Admin/Staff IAM | đăng ký/login, session, role/permission/resource scope, cấp và thu hồi quyền đúng người |
| **B. Service → Quote** | Service/Coverage + Pricing; Customer | service eligibility, supported routes, pricing version, quote TTL và fee snapshot |
| **C. Booking → Pickup request** | Orders/Customer; Ops | Shipment/Parcel IDs, address/service validation, route, dispatch request, event origin |
| **D. Pickup → Carrier Accepted** | Courier + Operations | authorized assignment, physical handover evidence, intake/acceptance, không tự nhận hàng từ UI |
| **E. Station/Hub → Line-haul** | Carrier Operations | site registry, scan/custody inventory, sorting, manifest, transfer, receiver confirmation |
| **F. Last-mile & return** | Courier + Ops | dispatch, delivery attempt, genuine POD, failed delivery, retry/return handover |
| **G. Money lifecycle** | Finance (và Courier collection evidence khi có COD) | fee/payment/receivable; COD expected/collected/remitted/reconciled/settled đúng nguồn và transaction |
| **H. Support/Claims** | CSKH/Claims; Customer; Operations evidence | Ticket/Claim tied to shipment, ownership, investigation, outcome, notification |
| **I. Oversight & audit** | Admin, domain audit & telemetry | correct scoped reads, exception escalation, audit trail, health/queue visibility |

### Hành vi phụ thuộc bắt buộc

- Khi một bước thiếu canonical source/approved interface: ghi rõ **blocker + domain owner + consumer bị ảnh hưởng + next smallest unblocker**; tiếp tục việc độc lập trong phạm vi được phép.
- UI Admin có thể hiển thị `UNAVAILABLE` cho metric chưa có nguồn; nhưng **E2E acceptance không thể PASS** nếu một bước bắt buộc như Carrier Accepted, POD hay reconciliation thực tế chưa tồn tại.
- Có thể song song hóa việc độc lập giữa portal, nhưng không dùng fallback giả để tạo PASS.
- Không xây full governance/policy editor/enterprise telemetry trước khi có tối thiểu end-to-end path, trừ khi chúng là yêu cầu kỹ thuật hoặc an toàn thực sự.

---

## 5. CURRENT TASK SLOT — THAY ĐỔI DUY NHẤT THEO TỪNG NHIỆM VỤ

**Mục 5 là phần DUY NHẤT có thể cập nhật thường xuyên theo Owner; Mục 0–4 và 6–9 là baseline chiến lược dài hạn.**

**Nguồn nhiệm vụ ưu tiên:** yêu cầu rõ ràng **mới nhất** của Owner. Nếu Owner giao nhiệm vụ trong chat, **không bắt buộc sửa file**; tác vụ mới nhất thay thế slot cũ trong việc thực thi. Chỉ sửa khối giữa các markers dưới đây nếu Owner muốn ghi nhiệm vụ đang làm vào file. Không tự sửa các mục chiến lược.

<!-- BEGIN OWNER_CURRENT_TASK -->
**Chưa pin nhiệm vụ cụ thể trong file này.** Hãy lấy nhiệm vụ hiện tại từ yêu cầu rõ ràng mới nhất của Owner. Không mặc định bắt đầu ở ADM-01 hoặc ở bất kỳ portal nào.
<!-- END OWNER_CURRENT_TASK -->

### Mẫu nhiệm vụ Owner có thể đưa trong prompt hoặc pin vào slot

```md
MỤC 5 — NHIỆM VỤ HIỆN TẠI
Portal / domain / module: ...
Business outcome & E2E step unlocked: ...
Relevant source specification / approved contracts: ...
Current state & existing PASS evidence (verify independently): ...
Exact scope / constraints / exclusions: ...
Definition of Done (observable outcome): ...
```

### Giao thức thực hiện một task

1. **READ**: North Star, `AGENTS.md`, chính nhiệm vụ Owner, domain spec và approved contracts.
2. **DISCOVER**: kiểm kê repo/API/schema/UI/evidence; xác định canonical owner và dependency.
3. **CLASSIFY**: `EXISTING_VERIFIED`, `MISSING_APPROVED`, `P0_E2E_BLOCKER`, `P0_DEPENDENT`, `BLOCKED_CONTRACT`, `P1_ENHANCEMENT`.
4. **EXECUTE**: thực hiện phần thiếu nhỏ nhất đủ tạo outcome; tránh scope creep và lặp code.
5. **VERIFY**: chỉ impacted tests và security gates cần thiết, cùng một kịch bản tích hợp liên quan khi cần.
6. **REPORT/HANDOFF**: kết quả thật, chưa hoàn thành gì, next highest-value dependency. Không tự nhận nhiệm vụ không được giao.

---

## 6. TESTING STRATEGY — NHANH NHƯNG KHÔNG THIẾU AN TOÀN

Áp dụng **change-based + risk-based testing**:

- Tái sử dụng PASS evidence có fingerprint và dependencies còn nguyên; không mặc định rerun mọi thứ chỉ vì chuyển task.
- Chạy focused unit/integration/DB/UI tests liên quan phần thay đổi; gom các sửa lỗi cùng domain thành batch; không tạo test trùng assertions.
- Khi sửa fixture, rerun test bị ảnh hưởng và regressions liên quan; giữ nguyên FAIL evidence lịch sử, không hạ assertion để tạo PASS.
- **Security/transaction gates bắt buộc khi bị tác động:** authentication/admission, actor/tenant/resource scope, current grants, proof/SoD, idempotency/replay, concurrency, atomicity/rollback, privacy/PII, COD/ledger/custody/POD integrity, event/outbox binding, audit.
- Chạy **consolidated HTTPS + Backend + PostgreSQL + UI** E2E khi các dependency cần thiết đã tích hợp; tránh chạy nhiều browser scenarios tương đương trước đó.
- Không tuyên bố thành công dựa vào mocks, intercepted UI fixtures, stale evidence hoặc test chưa thực sự chạy.

---

## 7. PLATFORM ACCEPTANCE — BẰNG CHỨNG VẬN ĐƠN THẬT TRONG TEST CÔ LẬP

Tối thiểu phải có các scenario **genuine UI + canonical Backend + isolated PostgreSQL** (synthetic accounts/business data, nhưng các transaction/event thực sự đi qua code production-path):

| Acceptance | Hành trình cần chứng minh |
|---|---|
| **E2E-01 — Successful Delivery** | Customer login → quote → booking → pickup → carrier accepted → Station/Hub custody/sort/dispatch → Courier POD → Delivered → fee/payment status đúng |
| **E2E-02 — Failed Delivery & Return** | Booking → acceptance → failed attempt + reason → authorized return workflow/handover → Customer tracking + finance consistency |
| **E2E-03 — Support & Claim** | Customer Ticket/Claim gắn Shipment → CSKH nhận/điều tra từ evidence → resolution/notification → audit |
| **E2E-04 — Business Merchant** | Merchant submission → independent review → decision → atomic Organization/Owner activation → Business Workspace → booking đúng Organization scope |
| **E2E-05 — Finance/COD** | Khi hỗ trợ COD: shipment obligation → authorized collection evidence → remittance → reconciliation/settlement; sai lệch và duplicate attempt bị từ chối |
| **E2E-06 — Role & Audit** | Separate Customer/Staff/Admin/Courier scopes, cross-site/tenant denial, authorized actions traced with domain events |

**Không bắt buộc COD nếu scope dịch vụ chưa hỗ trợ COD**, nhưng phải chốt rõ loại thanh toán nào đang được nghiệm thu và không tuyên bố COD READY. Không coi bước `UNAVAILABLE` là hoàn tất E2E.

Readiness tách biệt: `COMPONENT_SOURCE_TEST_READY` → `PORTAL_CORE_READY` → `CROSS_PORTAL_E2E_READY` → `LOCAL_UAT_READY` → `PRODUCTION_READY` (mỗi cấp cần evidence riêng). Một Admin Dashboard PASS không kéo theo platform PASS.

---

## 8. EXECUTION POLICY — ÁP DỤNG NGANG TẤT CẢ PORTAL

1. **Một task, một outcome đo được:** hoàn thiện phần quan trọng nhất của task được giao, không tự chuyển sang tính năng khác theo thói quen.
2. **Gắn đúng owner:** ưu tiên sửa domain tạo dữ liệu trước khi bổ sung read UI; consumer không tạo fake state.
3. **Không chặn toàn dự án vì một module:** ghi dependency cho các nguồn chưa sẵn sàng, tiếp tục phần độc lập đã được phép.
4. **Không làm lại:** giữ stable code/tests/DB migrations đã PASS khi không bị tác động.
5. **Giữ product specifications:** 9 menu Admin và specs của các portal khác tiếp tục là target; thứ tự thực hiện lấy theo critical path, không theo thứ tự trang trong file.
6. **Tối thiểu đủ đúng:** dùng existing services & canonical APIs khi có; hạn chế mở rộng API, roles, DB, screens khi không cần.
7. **Bàn giao sau mỗi milestone nghiệp vụ:** nêu bước đã chạy được từ portal nào sang portal nào, còn thiếu bằng chứng gì để đi tới next hop.

### Báo cáo ngắn sau nhiệm vụ

- `TASK`: phạm vi hiện tại / E2E step.
- `EXISTING`: hạng mục giữ nguyên do đã PASS.
- `IMPLEMENTED`: source/DB/UI thực sự thay đổi.
- `VERIFIED`: focused PASS, failed tests và genuine E2E coverage (nếu có).
- `BLOCKED`: nguyên nhân, domain owner, contract/authority thiếu.
- `NEXT`: đúng một next blocker/critical-path task có giá trị cao nhất.

---

## 9. AUTHORITY, PRESERVATION & CHANGE CONTROL

- File North Star chỉ là **định hướng sản phẩm và ưu tiên**, không phê duyệt thay những thay đổi API/schema/permission/identity linking/financial policy/production rollout.
- Tuân thủ `AGENTS.md`, module ownership, approved contracts, migrations, ACL, resource scope, separation-of-duties và security reviews. Đề xuất bounded amendment khi thật sự cần; đừng hỏi phê duyệt lại phần đã được duyệt.
- Không sửa lịch sử migrations, override canonical records, tự cấp real grants, tự gửi mail thật, hoặc bypass auth/CSRF/step-up để test PASS.
- Chỉ chạy trên owned disposable test targets trong quyền được cấp. Không tự thay demo/shared/UAT/Production hoặc deploy, commit/push/merge nếu chưa được Owner cho phép.
- Không xóa/stash/reset/clean WIP hoặc thay đổi artifacts đã phê duyệt mà không có quyền và preservation evidence.
- Chỉ công bố trạng thái đã quan sát, không suy diễn CI, full regression, Local UAT hoặc Production READY.

---

## PHỤ LỤC — THIẾT LẬP CODEX TỰ NHẮC READ FIRST

**Chỉ lưu `.md` sẽ không khiến Codex tự đọc.** Trong `AGENTS.md` tại repository root, **thêm** đoạn sau (giữ nguyên các rules đang có; không tạo một `AGENTS.md` cạnh tranh nếu đã có root file):

```md
### ArgoPost project-wide North Star — mandatory read first
For every new task in any ArgoPost area (public/customer/ops/courier/finance/admin,
backend, DB or infrastructure), read the entire
`docs/implementation/ARGOPOST_PROJECT_NORTH_STAR.md` before planning, editing,
testing, or declaring completion. It is portal-neutral: do not default to Admin
or repeat a prior ADM-01 task. Use the Owner's latest explicit task as Section 5;
only the Owner may request edits to the current-task slot. Prioritize genuine
cross-portal Quote→Booking→Pickup→Station/Hub→Delivery/Return→Finance→Support E2E.
Inspect existing source and PASS evidence; avoid redundant implementation/tests.
Keep domain boundaries and security/transaction gates. This file does not expand
authority or permit deployments. If unreadable, report this before code changes.
```

**Khi thay tên file:** tìm và cập nhật tất cả references cũ đến `ARGOPOST_E2E_NORTH_STAR.md` trong `AGENTS.md` và docs để chỉ còn **một canonical North Star**; không giữ hai chỉ thị mâu thuẫn. Việc thiết lập phải được Codex xác nhận bằng diff thực tế trong repository.

**Câu khởi tạo nhiệm vụ ngắn dành cho Owner:**

> Đọc `docs/implementation/ARGOPOST_PROJECT_NORTH_STAR.md` theo `AGENTS.md`. Nhiệm vụ hiện tại: **[portal/domain/module + outcome]**. Rà soát code/evidence đã có, chỉ triển khai phần thiếu quan trọng cho E2E, kiểm thử theo thay đổi và báo blocker/next dependency.
