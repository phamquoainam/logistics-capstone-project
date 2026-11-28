# ARGOPOST V1 — TEAM DAILY EXECUTION & INTEGRATION CALENDAR

> **Calendar version:** `V1-TEAM-CALENDAR-v1.3 — TRI-CO-OWNER GOVERNANCE` · Ngày lập 27/11/2026 · Lịch theo **ngày Việt Nam**.  
> **Ngày thực thi:** **28/11 → 05/12/2026 (8 ngày lịch)**.  
> **OWNER DECISION `OWNER-V1-DEADLINE-2026-11-27`:** Deadline chính thức **05/12/2026 — `TEAM-V1-GO-NOGO`**, do Nam (Co-Owner) phê duyệt thay cho 04/12. **04/12 là ngày integration regression/revalidation**, không còn là deadline GO/NO-GO. **05/12 là ngày final sign-off**, không phải buffer. Thay đổi ngày không cấp thêm quyền code/approval hoặc bảo đảm E2E PASS.  
> **Team:** `V1-A = Nam (Co-Owner/Customer & Commercial)`; `V1-B = Hoàng (Co-Owner/Carrier Network & Custody)`; `V1-C = Triết (Co-Owner/Dispatch & Courier Delivery)`.


**Quy tắc linh hoạt (Owner xác nhận):** Không bắt buộc giờ họp, khung giờ bàn giao hoặc giờ ra quyết định cố định. Mỗi người **kiểm tra lịch khi bắt đầu ngày làm việc**, ưu tiên giao ngay output P0 khi đủ điều kiện; integration smoke test khi có packet/source sẵn sàng; báo cáo trước khi kết thúc ngày làm việc. Các ngày trong bảng là **mục tiêu phối hợp**, không cho phép Codex bỏ qua approved authority, bảo mật, test hoặc tạo evidence giả. Trong **ngày 05/12**, Hoàng và Triết bàn giao báo cáo trước để Nam xem xét và ra GO/NO-GO sau khi đã kiểm chứng.

## 0.A. Tri-Co-Owner decision rights — thay cho mọi bước phê duyệt Nam đơn lẻ

**Quyết định điều hành mới `TEAM-COOWNER-DELEGATION-2026-11-27`: Nam, Hoàng và Triết đồng cấp Co-Owner.** Không có việc Hoàng/Triết phải chờ Nam duyệt mỗi bước. Lịch là công cụ điều phối, không phải quyền bỏ qua automated review, ACL hoặc thay đổi production.

| Quyết định | Người có quyền | Quy tắc |
|---|---|---|
| Task đã giao, source và contract trong phạm vi được duyệt | **Người phụ trách domain tự quyết** (Nam: Customer/Commercial; Hoàng: Network; Triết: Courier) | Audit → kiểm tra technical authority, sole writer → implement/test/handoff; **không chia sẻ với ba Co-Owners approval** |
| Handoff đã approved, ACK, thứ tự task độc lập, UI/Backend fixes trong cùng contract | Provider + consumer / mỗi domain tự quyết | Bàn giao sớm và làm song song |
| Shared file, interface mới, thay đổi chạm hai domain | **Hai Co-Owners chịu ảnh hưởng**, writer khác reviewer | `COOWNER_DECISION_RECORD` + one `CODE_WRITER`; domain thứ ba chỉ cần được thông báo, trừ khi cũng bị ảnh hưởng |
| Shared 3-domain / thay đổi scope, deadline, release | **Cả Nam, Hoàng, Triết đồng thuận** | Không người nào tự thông qua thay toàn đội |
| Protected security/ACL/migration/identity/payment/production | Review hợp lệ theo repo + owners liên quan | **Không được self-approve thay cho automatic/security review hoặc vượt quyền thực tế** |
| S8 GO/NO-GO ngày 05/12 | **Ba Co-Owners cùng ký**; dashboard do steward luân phiên tổng hợp | Một người thiếu ACK/evidence hoặc hard gate FAIL = `PARTIAL/NO-GO` |

`COOWNER_DECISION_RECORD`: `DECISION_ID | TASK_ID | AFFECTED_DOMAINS | REQUESTER | CODE_WRITER | INDEPENDENT_REVIEWER | AUTHORITY_REF | CONTRACT_VERSION | REQUIRED_EVIDENCE | ACKS | STATUS`. Trường hợp cần L2 mà chưa ACK: `WAITING_CROSS_DOMAIN_DECISION` + `NEXT_INDEPENDENT_TASK`, không chuyển mặc định sang Nam. Đây là cơ chế điều hành nội bộ **không tự cho phép chạy Git push/merge, deployment, sửa applied migration, AWS IAM/S3 hay vượt AGENTS.md**.

## 0. Cách Codex sử dụng lịch này — bắt buộc

Đọc **đồng thời**, nhưng không coi lịch là technical authority:

- V1-A Nam: `ArgoPost_V1_Member_01_Owner_Customer_Commercial.md` (TEAM-ASSIGNMENT-v1.5).
- V1-B Hoàng: `ArgoPost_V1_Member_02_Carrier_Network_Custody.md` (TEAM-ASSIGNMENT-v1.5).
- V1-C Triết: `ArgoPost_V1_Member_03_Dispatch_Courier_Delivery.md` (TEAM-ASSIGNMENT-v1.5).
- `AGENTS.md`, North Stars, critical-path, approvals/ADRs/specs, latest repository source, source/evidence register; actual decisions/approvals supersede planning targets.

**Phân định quyền:** Work assignment định nghĩa *ai chịu trách nhiệm*; lịch này định nghĩa *khi nào ưu tiên*; approved contract/technical authority + CODE_WRITER quyết định *được sửa gì*. Lịch không cấp quyền migration, DB ACL, Identity, payment, deployment, Git commit/push/merge hoặc thay đổi service boundaries. Không mặc định source được triển khai giống báo cáo 27/11; mỗi buổi phải inspect revision, worktree, evidence và current approval.

**Mỗi ngày Codex phải tuân thủ:**
1. **Đầu ngày:** đọc dòng kế hoạch của đúng Member và xem các handoff đến hạn **hôm nay hoặc 1–2 ngày tới**; xác nhận `ROLE_ID`, `git status`, locks, source fingerprint, approval và evidence. Nếu `TEAM-D0-BASELINE` 27/11 chưa đủ, làm preflight catch-up trong ngày 28/11 **không bịa D0 PASS**.
2. Ưu tiên **P0-UNBLOCK**: deliverable nhỏ nhất *đang khóa người khác* (contract/version/reader/approved command/test evidence) và gửi `DEPENDENCY_NOTICE` ngay khi phát hiện cần input. Không đợi cả Slice hoàn thành để bàn giao.
3. Tiếp **P1-VERTICAL**: task của chính Member mà có thể đi trọn actor UI → HTTPS Backend → PostgreSQL18 + negative tests, theo approved authority.
4. Nếu P0 chưa có input/approval, ghi `WAITING_FOR_HANDOFF` hoặc `BLOCKED_APPROVAL`, gửi Co-Owner provider/consumer đúng kênh rồi **chuyển P2-INDEPENDENT** trong cùng ngày. `CONTRACT_READY` cho phép consume/implement interface đã duyệt; `EVIDENCE_READY` chỉ khi upstream đã tạo genuine actor/custody/events; `INTEGRATION_READY` yêu cầu ghép source đồng revision và smoke test + ACK.
5. **Khi có source sẵn sàng:** thử **daily integration smoke** trên source/revision được tích hợp hợp lệ, càng sớm càng tốt, không đợi đến ngày 03/12. **Cuối ngày:** báo cáo `DONE|PARTIAL|BLOCKED|HANDOFF|UI_GATE|BACKEND_GATE|PG18_GATE|INTEGRATION_GATE|NEXT_INDEPENDENT_TASK|OWNER_DECISION`.

**WIP giới hạn:** 1 runtime/code task chính + 1 read-only audit/contract/test-design task độc lập trên mỗi Codex. Khi P0 bị blocker, không sửa source người khác; chuyển P2. Những contract/policy chưa được duyệt chỉ được soạn/kiểm thử ở mức proposal; không tự nâng thành runtime authority.

**Bắt buộc về UX:** Một Slice có actor-facing UI chỉ PASS khi giao diện Web/Mobile thật thao tác được vào Backend HTTPS và PG18, có authorization, loading/empty/error, stale/duplicate denial và read-back. Backend-only `BACKEND_READY`; UI fixture-only `UI_PROTOTYPE_READY`; giao source chưa được consume là `INTEGRATION_PENDING`; không gán `SLICE_E2E_PASS` từ bất kỳ trạng thái nào trong ba loại này.

## 1. Critical dependency graph — liên hệ trước 1–2 ngày

```text
NAM (V1-A) canonical Booking/Parcel/CarrierCargo
  ├─ A2B-S1-BOOKED-CARGO ──> HOÀNG (V1-B) Station acceptance + custody
  │                            └─ Hoàng S2 Inventory/Sort/Manifest
  │                                 └─ Hoàng S3 Transfer/Hub Receive
  │                                      ├─ B2A-S2/S3-EVENTS ──> NAM tracking
  │                                      └─ B2C-S4-DISPATCH-READY ──> TRIẾT
  │                                             └─ Triết S4 Dispatch Web/Courier Mobile/POD
  │                                                     └─ C2A-S4-POD-FACTS ──> NAM tracking/Claims
  └─ A2C-S5-PICKUP-REQUEST ──> TRIẾT S5 collect from sender
                                           └─ C2B-S5-PICKUP-HANDOVER ──> HOÀNG Station receive
                                                                                  └─ same Hoàng S2→S3→Triết S4 pipeline
TRIẾT S6 failure/return ── C2B-S6-RETURN-HANDOVER ──> HOÀNG reverse receipt
Hoàng/Triết immutable actual facts ──> NAM S7 Finance/Claims (never fake PAID/COD)
```

**Không đợi “cả Slice”**: Nam giao Orders reader cho Hoàng khi contract+targeted tests sẵn sàng, không chờ Customer UI hoàn hảo; Hoàng giao DispatchEligibility DTO cho Triết trước S3 E2E; Nam giao Pickup DTO trước Pickup E2E; Triết giao POD reader cho Nam khi scoped contract được duyệt, evidence đến sau. **Chỉ nhận event nghiệp vụ thật để kết luận E2E PASS**.

### 1.1. Handoff registry — ngày mục tiêu đường găng, chốt GO/NO-GO 05/12

| Ngày mục tiêu | ID / Level | Người gửi → người nhận | Input phải đủ trước khi consumer nghiệm thu |
|---|---|---|---|
| 27/11 (carry-over nếu thiếu) | `TEAM-D0-BASELINE` | Nam+Hoàng+Triết → Nam | Source audit, worktree/HEAD, locks, UI/backend gaps, approvals |
| **28/11** | `A2B-S1-BOOKED-CARGO` | Nam → Hoàng | Owner-neutral approved Orders contract; genuine Booking/Parcel/createdAt/ownerType/UNPAID + exact source hoặc blocker |
| **29/11** | `B2A-S1-ACCEPTED-EVENT` | Hoàng → Nam | Personal/Business valid Station acceptance + custody/tracking/audit/receipt, HTTPS+PG18+negative |
| **29/11** | `A2C-S5-PICKUP-REQUEST` — CONTRACT | Nam → Triết | Approved Pickup API/DTO owner/Org/address/window/coverage/cancel/idempotency |
| **30/11** | `B2C-S4-DISPATCH-READY` — CONTRACT | Hoàng → Triết | Approved eligibility/current-holder/version/exact Parcel set/handover errors; no Hub PASS implied |
| **30/11** | `A2C-S5-PICKUP-REQUEST` — EVIDENCE | Nam → Triết | Genuine owner-authorized Pickup Request from Customer UI/Backend+PG18 + targeted negatives |
| **01/12** | `B2A-S2/S3-EVENTS` and `B2C-S4-DISPATCH-READY` — EVIDENCE | Hoàng → Nam/Triết | Genuine Manifest→Transfer→Hub received, holder, version, PG18/security evidence |
| **01/12** | `C2A-S4-POD-FACTS` | Triết → Nam | Genuine courier attempt/POD/Delivered proof & scoped privacy-safe read |
| **02/12** | `C2B-S5-PICKUP-HANDOVER` | Triết → Hoàng | Actual Sender→Courier collection + exact Parcel set; Station receive/ACK by Hoàng |
| **02/12** | `B2A-S7-NETWORK-FACTS` and `C2A-S7-FINANCE-EVIDENCE` | Hoàng/Triết → Nam | True immutable custody/return/attempt/proof fee-relevant refs; not PAID/COD |
| **03/12** | `C2B-S6-RETURN-HANDOVER` | Triết → Hoàng | Authorized failed/retry/return policy + real reverse custody events |
| **03/12** | `TEAM-S8-REHEARSAL` | Nam+Hoàng+Triết → Nam | 4 full-stack journeys + negative/security/finance, with exact revision |
| **04/12** | `TEAM-S8-REVALIDATION` | Nam+Hoàng+Triết → Nam | Final integrated source revision, four Web/Mobile→HTTPS→PG18 E2E regressions, S6/S7/security evidence, unresolved P0/P1 risk register; **chưa GO** |
| **05/12** | `TEAM-S8-SIGNOFF` | Nam+Hoàng+Triết → Nam | Signed role evidence/ACK, all UI/backend/PG18/security gates, residual blockers/risks and matching revisions |
| **05/12** | `TEAM-V1-GO-NOGO` | Nam (Owner) | Evidence-based GO only if all mandatory gates PASS; otherwise PARTIAL/NO-GO with exact recovery plan |

**Nguyên tắc thông báo:** gửi `DEPENDENCY_NOTICE` cho provider ngay khi biết input cần trong 1–2 ngày tới; nếu nguy cơ trễ, `RISK_NOTICE` ngay; nếu quá hạn, `DEADLINE_MISSED` và escalation ngay khi phát hiện. Contract, source fingerprint, consumer ACK, tests phải được lưu trong một `HANDOFF_PACKET` có thể audit. Codex không được nói “đã gửi” khi chỉ mới tạo draft thông báo nhưng người khác chưa nhận.

## 2. Lịch công việc từng ngày — NAM (V1-A)

**Vùng code:** Customer/Business Portal, Merchant/Admin Business approval, Orders/Booking/Pickup Request, Finance/Claims và joint co-owner coordination. Không thực hiện physical Station scan, Inventory custody, Courier POD, hoặc financial fact giả. **BIA-DB-GUARD-01/V48 còn WIP**: hoàn thiện scope hiện tại, review approval, migration preflight, release sole-writer lock; nếu chưa được duyệt, không tự sửa guard.

| Ngày | P0 — việc phải mở khóa / kết nối thành viên khác trước | P1 — runtime + UI Full-Stack của Nam (có authority) | P2 — nếu P0 blocked, chuyển ngay sang | Output / check cuối ngày |
|---|---|---|---|---|
| **28/11 (T7)** | **A-S1-04/05**, `A2B-S1-BOOKED-CARGO` tới **Hoàng trong ngày**; BIA guards/V48 exact state, release/transfer lock cho Hoàng nếu đạt gate; xác nhận BIA source đã được tích hợp hay chưa. Gửi **Triết** sớm bản nháp Pickup DTO `A-S5-02/03` để design độc lập (không coi proposal approved). | **A-S1-01/02/03/06** audit/regression: Customer/Business quote→Booking/label, Merchant review/activation UI, `WAITING_FOR_HANDOVER`/Station reader; genuine HTTPS+PG18 và deny cases. Chỉ phần Nam sở hữu. | **A-S7-01** audit XLSX/CSV pipeline; **A-S7-02** immutable fee/obligation checks; **A-S8-01** acceptance matrix. | `A2B` packet/blocked reason; V48 single writer released hay locked; Customer/Admin S1 UI/backend evidence, `A-D1-STATUS`. |
| **29/11 (CN)** | Nhận `B2A-S1-ACCEPTED-EVENT` từ **Hoàng trong ngày**; ACK và ghép `A-S1-06`. **Chốt approved `A2C-S5-PICKUP-REQUEST` CONTRACT cho Triết trong ngày**: DTO/permission/price/coverage/cancel rules. | **A-S7-01** Business XLSX/CSV upload→row errors→draft→quote→genuine booking/label Web UI; **A-S5-01/02/03** Customer Pickup mode/form + Backend contract khi approved. | **A-S3-02** pricing snapshot regression; **A-S6-02/03** Claims intake/lifecycle, **A-S8-02** customer test plan. | `S1-P/Hoàng` only if Hoàng actual acceptance+UI PASS; `A2C` CONTRACT packet; XLSX valid row→real Booking status. |
| **30/11 (T2)** | Trong ngày gửi **Triết** `A2C-S5-PICKUP-REQUEST` **EVIDENCE** của authorized Personal và Business Pickup Request, hoặc exact blocker; báo **Hoàng** các Booking/Parcel/route read gaps còn ảnh hưởng Manifest. | **A-S5-01/04** Pickup Request Customer UI→HTTPS→PG18 (coverage, wrong-owner, duplicate, cancel/race); **A-S7-01** Bulk booking negative and idempotent confirm; **A-S7-03** payment contract proposal. | **A-S7-02** obligations, **A-S7-05** Claims reuse/privacy; **A-S2-01** Orders reader regression. | Personal/Business Pickup Request `CONTRACT/EVIDENCE` đúng level; Bulk UI/backend proof; no synthetic pickup collected. |
| **01/12 (T3)** | Nhận `B2A-S2/S3-EVENTS` từ **Hoàng trong ngày**, nhận `C2A-S4-POD-FACTS` từ **Triết trong ngày** nếu thực tế sẵn sàng; consumer ACK only after revision & read contract. | **A-S2-02**, **A-S3-01**, **A-S4-01/02** cập nhật Customer/Business Tracking UI bằng real Manifest/Hub/Attempt/POD; Admin V1 oversight; authorization/PII tests. | **A-S7-03/04** payment approval/verified-method preparation; **A-S6-02/03** claims UI; **A-S8-01** E2E test harness. | Real Customer view Station→Hub→Delivered if events genuinely exist, otherwise `INTEGRATION_PENDING`; Finance approval queue. |
| **02/12 (T4)** | Xác nhận **Triết→Hoàng Pickup** đang được nhận và ACK; trong ngày nhận `B2A-S7-NETWORK-FACTS`/`C2A-S7-FINANCE-EVIDENCE`. Không tự tăng trạng thái `PAID`. | **A-S5-01/04** hoàn thiện Customer Pickup status/cancel UI trên real request; **A-S7-04/06** approved payment/reconciliation Finance Web UI; **A-S6-01/02/03**, **A-S7-05** Claims/CSKH UI và event binding. | **A-S7-01** Bulk invalid row UX + retry, **A-S8-02/03** actor/finance security tests. | Personal/Business Pickup Customer UI + claims/Finance states có nguồn; S5 end-to-end chỉ khi Hoàng/Triết custody thật. |
| **03/12 (T5)** | Trong ngày xử lý mọi Nam↔Hoàng/Triết handoff còn thiếu; phối hợp `TEAM-S8-REHEARSAL` trong ngày. Ưu tiên **blocking gaps của bốn journeys** trước feature ngoài critical path. | **A-S8-01/02/03/04** điều phối 4 browser/mobile journeys; thử Bulk Booking, Finance supported settlement, claim lifecycle; kiểm thử UI error/permission/rollback. | **A-S7-02/05**, **A-S6-01** targeted regression, evidence sanitizer/report; không thêm spec chưa duyệt. | 4 E2E truth table, UI/backend/PG18/integration gate từng Slice, source fingerprint & blockers, GO/NO-GO candidate. |
| **04/12 (T6) — FINAL REGRESSION** | Trong ngày tổng hợp mọi blocker còn khóa 4 journeys; điều phối Nam/Hoàng/Triết cùng một source revision và `TEAM-S8-REVALIDATION` **cuối ngày**. | **A-S8-01/02/03/04** Browser/Admin/Customer/Finance/Claims regressions + actor auth/negative/rollback, kiểm tra handoff ACK, critical bugfix được duyệt. | **A-S7-01/02/05**, **A-S6-01** targeted tests/evidence nếu hard blockers đang chờ Hoàng/Triết; không bổ sung scope ngoài V1. | Four-journey evidence register, signed-report draft, exact P0/P1 blockers và action owner; **chưa có Owner GO/NO-GO**. |
| **05/12 (T7) — OFFICIAL GATE** | Cả **Nam + Hoàng + Triết** cùng đọc signed reports và ra quyết định GO/NO-GO chung; kiểm tra canonical source revision, four journeys và hard gates; **trong ngày** cả ba đồng thuận `TEAM-V1-GO-NOGO`. | **A-S8-04/05** final full-stack acceptance, audit evidence UI→HTTPS→PG18/finance/security/claims; Owner ký `GO` hoặc `PARTIAL/NO-GO`. | Nếu vẫn thiếu evidence, chỉ chạy targeted authorized retests trước giờ gate; không giả PASS hoặc nới scope. | **Formal Owner decision 05/12**, decision log + evidence index, nếu NO-GO có blocker owner/recovery plan. |

**Ba Co-Owners chủ động báo nhau trước:** Hoàng `A2B` (28/11), `B2A` (29/11 và 01/12), Triết `A2C` (29–30/11), `C2A` (01–02/12). Cần duyệt ngay các contract ảnh hưởng đường găng; không trì hoãn domain Co-Owner / independent technical approvals đến lúc Hoàng/Triết xong UI.

## 3. Lịch công việc từng ngày — HOÀNG (V1-B)

**Vùng code:** Station Intake, Inventory, Sorting, Manifest, Network Transfer/Hub Receiving, Hub release/Station Pickup receiving, reverse network custody. Không tự sửa Orders private, Finance payment, Courier DeliveryRun/Mobile. Riêng `BIA-DB-GUARD-01/V48` chỉ sửa phần Dispatch-owned khi nhận **CODE_WRITER RELEASE + approval**; Nam đang xử lý WIP có thể chưa bàn giao.

| Ngày | P0 — việc phải mở khóa / kết nối thành viên khác trước | P1 — runtime + Operations Web Full-Stack của Hoàng (có authority) | P2 — nếu P0 blocked, chuyển ngay sang | Output / check cuối ngày |
|---|---|---|---|---|
| **28/11 (T7)** | Nhận `A2B-S1-BOOKED-CARGO` từ **Nam trong ngày**; phối hợp exact BIA guard/V48 source writer release; lập ngay `DEPENDENCY_NOTICE` cho Nam nếu thiếu Orders read/approval. Gửi **Triết** sớm draft `DispatchEligibility` fields/custody handover cho Triết thiết kế. | **B-S1-01/02/03/04/05/06/07** hoàn thiện Business Station Intake được duyệt: Ops Web thật scan→accept; PG18 custody/tracking/receipt/audit, negatives/security. Không viết lại Personal PASS. | **B-S2-01/02/03/04/05/06** inventory/manifest audit + Spec012 contract/test/UI plan; **B-S3-01/04** Spec013 draft. | BIA S1 gaps/errors; `B2A` planned proof trace; Spec012 owner decision request; no conflicting V48 edit. |
| **29/11 (CN)** | Trong ngày gửi **Nam** `B2A-S1-ACCEPTED-EVENT` (Personal+Business actual) hoặc blocker. Chốt Spec012 approval; báo **Triết** sớm expected DispatchEligibility data fields/error cases. | **B-S2-01/02/03/05/06** Ops Inventory site list/search, valid sorting, Manifest Draft UI/backend/PG18; tiếp **B-S2-04** Seal version/idempotency nếu approved. | **B-S3-01/02/03/04** Spec013 API + hub UI screen design; **B-S4-01** eligibility contract; **B-S7-01** factual reader design. | S1 evidence+ACK; S2 Inventory/Manifest vertical checks (không giả custody từ Seal). |
| **30/11 (T2)** | Trong ngày gửi **Triết** `B2C-S4-DISPATCH-READY` **CONTRACT**; thống nhất Hoàng/Triết current-holder, network release vs courier acceptance, retry/error/version. Chốt Spec013 approval. | **B-S2-03/04/05/06** Manifest Draft→Seal Operations Web→PG18 tests; **B-S3-01/02/03/04/05/06** triển khai Transfer/Hub Receiving (sender/receiver/hub UI) theo approved spec. | **B-S5-01** Station Pickup Receiving contract với Triết; **B-S6-01** discrepancy/hold UI design; **B-S7-01/02** event reader. | `B2C` contract approved+ACK; S2 Full-Stack status; S3 early handover/receiver proof. |
| **01/12 (T3)** | Trong ngày gửi **Nam** `B2A-S2/S3-EVENTS` và **Triết** `B2C-S4-DISPATCH-READY` **EVIDENCE**, hoặc blocker ngay. Đây là handoff đường găng cao nhất. | **B-S3-01/02/03/04/05/06** Ops sender handover→Transport→Hub receipt/discrepancy; **B-S4-01/02/03** Hub eligibility/release UI/backend and custody tests. | **B-S5-01/02** Station receiving implementation theo approved Triết handshake; **B-S6-01** discrepancy UX; **B-S8-02/03** negative/catalog tests. | Real Manifest→Hub + Hoàng→Triết handover, exact parcel/current holder revision; nếu Hub chưa PASS, Triết không assign real parcels. |
| **02/12 (T4)** | Trong ngày nhận `C2B-S5-PICKUP-HANDOVER` từ **Triết**; Station phải xác nhận custody receipt/ACK. Trong ngày gửi **Nam** `B2A-S7-NETWORK-FACTS`. | **B-S5-01/02/03** Courier Pickup→Station Receiving Operations Web→PG18, reuse cùng S1 intake and S2 inventory; **B-S6-01/04** exception/hold UI + reader. | **B-S3-03/04/06** Hub discrepancy regression, **B-S4-03** wrong courier/replay tests, **B-S7-02** network evidence tests. | Pickup handoff consumer ACK + Personal/Business Station receipt or exact blocker; Finance facts (no cash status). |
| **03/12 (T5)** | Trong ngày nhận `C2B-S6-RETURN-HANDOVER` từ **Triết**; integrate network return. Hoàn thiện `TEAM-S8-REHEARSAL` trong ngày cùng Nam/Triết. | **B-S6-02/03**, **B-S8-01/02/03** real return custody, 4 network journeys, negative/rollback/site/race/concurrency; Operations S1–S6 UI. | **B-S2-06**, **B-S3-06**, **B-S7-01/02** targeted regression/repair; không tạo new engine. | Network 4 flows evidence/source revision, unresolved gaps ranked, unsigned signoff draft. |
| **04/12 (T6) — FINAL REGRESSION** | Cùng Nam/Triết giải quyết critical Hub→Courier/Pickup Station/Return blockers và tham gia `TEAM-S8-REVALIDATION` **cuối ngày**. | **B-S2/S3/S4/S5/S6/S8** Operations Web + HTTPS/PG18 regression, migration/security/site/return, final source revision và 4 journeys. | Kiểm tra source/test independent cho network discrepancy, ACL, replay nếu đang đợi Triết handover. | B-S8 evidence draft, exact missing gates/source fingerprint cho Nam, chưa final sign-off. |
| **05/12 (T7) — OFFICIAL GATE** | Hoàng công bố `TEAM-S8-SIGNOFF` Network cho **cả ba Co-Owners trước quyết định GO/NO-GO chung**; consumer ACK, current revision và all hard gates. | **B-S8-01/02/03/04** verify Network Ops Web→HTTPS→PG18, custody/Hub/Station/return, migration/ACL tests, 4 journeys. | Chạy targeted regressions được duyệt nếu còn thiếu, không tạo fake receipt hoặc sửa domain Triết. | Signed Network role evidence + exact PASS/PARTIAL + blockers trước khi ba Co-Owners cùng quyết định GO/NO-GO; Nam quyết định trong ngày. |

**Hoàng không đợi Nam hoàn tất tất cả Merchant/Finance:** chỉ cần canonical Booking/Parcel + approved reader cho các bước B-owned. **Hoàng cũng không đợi Triết hoàn tất Mobile** để viết S2/S3/Station UI đã được duyệt; bàn giao DTO sớm trước bằng chứng courier thực.

## 4. Lịch công việc từng ngày — TRIẾT (V1-C)

**Vùng code:** Carrier Operations **Dispatch Web** và Courier Mobile/DeliveryRun/Assignment/Attempt/POD/Pickup/Return Courier. Không sửa Orders `CarrierCargo`, Station Intake, Dispatch pilot custody guard, B-owned Hub receiving. Identity/Courier audience, POD proof policy/storage, Spec014/015/029 phải có approved authority trước runtime, không bypass security review.

| Ngày | P0 — việc phải mở khóa / kết nối thành viên khác trước | P1 — runtime + Dispatcher Web/Courier Mobile Full-Stack của Triết (có authority) | P2 — nếu P0 blocked, chuyển ngay sang | Output / check cuối ngày |
|---|---|---|---|---|
| **28/11 (T7)** | Gửi **Hoàng** request sớm về DispatchEligibility/Hub→Courier contract `C-S2-01`, `C-S3-01/02`; gửi **Nam** request Pickup DTO `C-S5-01`. Hoàn thiện proposals Spec014/015/029 để các Co-Owners liên quan và mandatory technical reviewers xử lý ngay. | **C-S1-01/02/03**, **C-S4-03** Courier Mobile build/login/navigation/task list/error scaffolding theo approved existing auth; bootstrap Dispatch UI nếu writer rõ. | **C-S4-01/02** DeliveryRun task lifecycle/assignment contract; **C-S4-06** POD proof/privacy policy; **C-S4-09** test harness. | Mobile build/emulator proof; contract/approval queues cho Hoàng→Triết, Nam→Triết; không claimed Courier auth nếu chưa verified. |
| **29/11 (CN)** | Chủ động ACK **Nam** `A2C-S5-PICKUP-REQUEST` **CONTRACT** trong ngày nếu approved; nhắc **Hoàng** `B2C` contract deadline 30/11; không đợi Hub event để viết consumer adapter. | **C-S4-01/02/03/09**, **C-S2-02** DeliveryRun/Assignment Backend + **Dispatch Web** create/list/assign UI, Courier Mobile assigned tasks UI; focused auth/idempotency tests trong approved scope. | **C-S4-05/06/08** attempt/POD/failure DTO + privacy/negative test design; **C-S5-03** Pickup task UI preparation. | Dispatch + Courier auth/task vertical features by approved contract; no real parcel assigned until Hoàng eligibility evidence. |
| **30/11 (T2)** | Trong ngày nhận `B2C-S4-DISPATCH-READY` CONTRACT từ **Hoàng**; trong ngày nhận `A2C-S5-PICKUP-REQUEST` **EVIDENCE** từ **Nam**. Tiếp tục P1 ngay phần nào đã nhận đúng level. | **C-S4-03/04/05/06/09** Courier Mobile scan/accept, attempt/POD UX+Backend commands (real execution only after Hoàng handover); **C-S5-01/02/03** Pickup Queue + Dispatcher Web/assigned Courier pickup task. | **C-S4-08**, **C-S5-06** failures/cancel policy design; **C-S6-01/04** race/rollback tests. | Hoàng/Triết approved port connected at CONTRACT level; Nam pickup request consumer integrated where possible; no POD/Delivered E2E claim. |
| **01/12 (T3)** | Trong ngày nhận **Hoàng** `B2C-S4-DISPATCH-READY` **EVIDENCE**; khi Hub release available execute C-S4-04. Trong ngày gửi `C2A-S4-POD-FACTS` cho **Nam** hoặc exact blocker. | **C-S4-01/02/04/05/06/07/09** Dispatcher Web create/assign→Courier Mobile accept/scan/attempt/POD→Delivered→PG18, negative/privacy/duplicate tests. | **C-S5-01/02/03/04** Customer Pickup consumer + courier UI/workflow preparation; **C-S7-01** evidence reader design. | First real DROP_OFF→Delivered on matching integrated revision if possible; Triết→Nam proof; NOT PASS if Hoàng Hub release missing. |
| **02/12 (T4)** | Trong ngày gửi `C2B-S5-PICKUP-HANDOVER` tới **Hoàng**: Personal + Business actual Sender→Courier collection, correct Parcels; Hoàng Station ACK. Trong ngày gửi `C2A-S7-FINANCE-EVIDENCE` tới **Nam**. | **C-S5-01/02/03/04/05/07** Dispatcher Pickup Web + Courier Mobile accept/scan/sender handover→Hoàng Station, Personal/Business; **C-S5-06** failed/cancel/reassign UI. | **C-S6-01/02/04/05** failed delivery/retry/privacy test design, **C-S4-09** regressions. | Pickup→Station EVIDENCE with Hoàng ACK; queue/assignment Mobile full-stack trace; no phantom pickup success. |
| **03/12 (T5)** | Trong ngày gửi `C2B-S6-RETURN-HANDOVER` tới **Hoàng**; phối hợp rehearsal 4 journeys cùng Nam/Hoàng trong ngày. | **C-S6-01/02/03/04/05**, **C-S8-01/02/03/04** Failed Pickup/Delivery→approved retry/return, Courier Mobile/Dispatcher Web exceptions, proofs and 4 integrated journeys. | **C-S4-09**, **C-S5-07** wrong courier, replay, duplicate POD, offline retry/regression tests. | Genuine return evidence/ACK, Courier S4/S5/S6 UI+Backend proof, rehearsal status exact revision. |
| **04/12 (T6) — FINAL REGRESSION** | Ưu tiên đóng Hoàng↔Triết Hub handover/Pickup/Return blockers, cung cấp lại POD fact reader nếu source revision đổi; tham gia `TEAM-S8-REVALIDATION` **cuối ngày**. | **C-S4/S5/S6/S8** Dispatch Browser/Courier Mobile→HTTPS→PG18 full-stack regressions, privacy/POD, retry/race and 4 journeys. | Đóng independent UI error-state, duplicate/replay/offline retry tests trong approved code paths. | C-S8 evidence draft, exact missing gates/source fingerprint cho Nam, chưa final sign-off. |
| **05/12 (T7) — OFFICIAL GATE** | Triết công bố `TEAM-S8-SIGNOFF` Courier cho **cả ba Co-Owners trước quyết định GO/NO-GO chung**, kèm mobile/emulator+Dispatch Web proof trên source revision dùng nghiệm thu. | **C-S8-01/02/03/04/05** final POD/Delivered, Pickup, Return, privacy/race, 4 journeys trên HTTPS+PG18 thật. | Targeted approved retest hoặc exact blocker notice, không claim fake POD/Delivered. | Signed Courier role evidence + exact PASS/PARTIAL trước khi ba Co-Owners cùng quyết định GO/NO-GO; Nam quyết định trong ngày. |

**Triết không chờ Hoàng hoàn thành toàn bộ S3** để triển khai DeliveryRun/Mobile theo approved DTO, nhưng **phải chờ genuine Hub custody/handover** để nghiệm thu S4 Delivery/POD. **Triết không chờ Nam hoàn tất toàn bộ S5** để soạn Pickup UI, nhưng phải có approved Customer Request + actual Parcel trước khi ghi genuine collection.

## 5. Thứ tự phát triển theo ngày — chống chờ và conflict

| Ngày | First unblock của nhóm (điểm khóa cần làm trước) | Full-stack vertical demo nên có cuối ngày | Nếu bị chặn bởi người khác |
|---|---|---|---|
| 28/11 | Nam `A2B`; Nam→Hoàng V48 handback; Nam/Triết Pickup DTO & Hoàng/Triết dispatch DTO early notices | Personal/Business Booking/Station smoke; Courier auth bootstrap | Hoàng làm Spec012/013; Triết làm Mobile auth/test; Nam làm XLSX audit |
| 29/11 | Hoàng `B2A` S1 acceptance; Nam `A2C` CONTRACT | S1 Customer+Ops UI proof; Bulk Booking Web→PG18; Inventory UI first path | Nam làm Claims/Bulk; Hoàng chuẩn bị Inventory UI; Triết làm Dispatch UI |
| 30/11 | Hoàng `B2C` CONTRACT; Nam `A2C` EVIDENCE | Manifest Draft/Seal UI→PG18; Pickup Request UI→PG18; Dispatch Web/Mobile local wiring | Hoàng làm Spec013/Hub UI; Triết làm POD/attempt command/test; Nam làm Finance contract |
| 01/12 | Hoàng `B2A/B2C` EVIDENCE; Triết `C2A` POD | Ops Hub Receive → Dispatcher Web → Courier Mobile → POD; Customer Tracking | Nam làm Finance/Claims; Hoàng làm S5 Station receiver; Triết làm Pickup consumer |
| 02/12 | Triết→Hoàng actual Pickup; Hoàng/Triết→Nam Finance facts | Two owner types Pickup→Station UI; Claims/Finance integrated | Nam Bulk tests, Hoàng discrepancy/return, Triết retry/failed Pickup |
| 03/12 | Triết→Hoàng return; team full 4-journey rehearsal | Four E2E + negative/rollback/PII; no new speculative scope | Fix top blocker only, daily cross-role re-test |
| 04/12 | Nam+Hoàng+Triết final integration regression và `TEAM-S8-REVALIDATION` cuối ngày | Same-revision 4 E2E, security/finance/UX evidence drafts | Fix only approved critical blocker; không tuyên bố GO trước 05/12 |
| 05/12 | Nam+Hoàng+Triết signed evidence đầu ngày; Nam `TEAM-V1-GO-NOGO` trong ngày | Formal GO hoặc PARTIAL/NO-GO với evidence & recovery plan | Chưa đủ hard gate → NO-GO, không giả PASS |

### 5.1. Shared-source lock rules — bắt buộc

| Nhóm source/change-set | Sole CODE_WRITER theo approved domain/current handback | Ai chỉ làm consumer/contract trong khi locked |
|---|---|---|
| Orders `CarrierCargo`, Booking owner/fee, Merchant Approval/Claims/Finance | **Nam** hoặc exact canonical domain owner khác được Co-Owners có thẩm quyền quyết định | Hoàng/Triết không viết private Orders/Finance |
| Existing V48/`BIA-DB-GUARD-01`/shared migration registry | **Chỉ writer hiện được Nam xác nhận** theo exact approved change-set; tách Orders-vs-Dispatch guard theo authority, tránh hai writer cùng một migration | Hoàng không đồng thời sửa khi Nam chưa release; Triết không sửa |
| Station acceptance/custody, Inventory/Manifest, Hub receiving/Network return | **Hoàng** sau chính thức source handback | Nam/Triết consume qua approved ports, không ghi bảng Hoàng private |
| Dispatch Web DeliveryRun/Assignment, Courier Mobile, Attempt/POD/Pickup execution | **Triết** | Nam/Hoàng chỉ share approved source/events/requirements |
| Common shell, migration numbering, cross-domain DTO, Workflow, Role/ACL | **Nam chỉ định một CODE_WRITER mỗi change-set** + mandatory review/approval | Người khác tạo change request + consumer tests, không patch đồng thời |

- Mỗi người nên có **worktree/clone riêng** và branch riêng; không dùng cùng thư mục với Codex khác. Tách worktree **không tự ngăn semantic conflicts**; cần owner/path lock, explicit source transfer, same integration revision và integration tests.
- Trước khi sửa shared source, đưa `LOCK_REQUEST(path/module, task, role, change-set, authority, expected return, reviewer)` vào nhóm. Khi complete, đưa `LOCK_RELEASE` kèm diff/files/PG18 checksum/source fingerprint. Không tự merge/cherry-pick/commit/push/deploy khi chưa có approval; Các Co-Owners của domain bị ảnh hưởng thống nhất integration writer và thao tác được phép; Git operations cần đúng AGENTS/CI/quyền thực tế.
- Có thể có nhiều tests khác nhau trên một shared contract nhưng **một source writer**. Nếu hai task cùng đụng một file config/route, gom thành **một change-set** và owner cho consumer test; không chia cả hai cùng patch.
- Dùng **isolated PG18 schema/database** khi chạy destructive/regression tests, không đồng thời mutate cùng shared test DB dẫn đến false failures/false PASS. Integration run sử dụng một environment với revision và fixtures được pin rõ, reset/seed theo approved test procedure; không reset môi trường người khác.

## 6. Nhịp phối hợp linh hoạt — không đặt giờ cố định

| Thời điểm linh hoạt | Ai tham dự / làm gì | Điều kiện đầu ra |
|---|---|---|
| **Khi bắt đầu làm việc** | Integration steward luân phiên điều phối 3 người, mỗi người nêu 1 P0 handoff cần trong 1–2 ngày tới, 1 P1 vertical, 1 fallback P2; status source lock, approval, affected paths | Bảng `TASK_ID / BLOCKER / PROVIDER / DUE / WRITER / NEXT_INDEPENDENT_TASK` được cập nhật |
| **Trong ngày** | Gửi packet đến hạn, consumer review contract/signature/security. Nếu chỉ có CONTRACT, có thể code consumer đã duyệt; không fake physical evidence | `HANDOFF_PACKET` + rõ level/ACK hoặc `RISK_NOTICE`; missed deadline escalate ngay khi phát hiện |
| **Khi có bản tích hợp (linh hoạt)** | Integration steward/sole writer được giao ghép phần nguồn được phép; các thành viên thực hiện Browser/Emulator→HTTPS→PG18 smoke | `INTEGRATION_READY`/`FAILED_VERIFICATION` với source fingerprint, regression links; không claim merge khi chưa thực hiện |
| **Cuối ngày (linh hoạt)** | Review những test thực PASS, mọi vấn đề trễ và kế hoạch sáng hôm sau; gửi notice cho dependencies trong 2 ngày tới | Integration steward được cả ba phân công duy trì dashboard S1–S8; 3 daily reports có `NEXT_INDEPENDENT_TASK` |

**Bất cứ lúc nào:** hard blocker có thể làm lỡ critical path → `RISK_NOTICE`/`BLOCKER_NOTICE` đến đúng provider + Nam ngay khi phát hiện, không chờ check-in; ưu tiên work nhỏ nhất để người khác tiếp tục, không cần hoàn thiện tất cả màn hình mới bàn giao API contract.

## 7. Standard `DEPENDENCY_NOTICE` / task switching format

```text
DEPENDENCY_NOTICE
DATE_VN:
REQUESTER: Nam(V1-A) | Hoàng(V1-B) | Triết(V1-C)
PROVIDER:
NEEDED_FOR: exact TASK_ID + Slice + actor-facing UI/backend action
HANDOFF_ID: exact VI.1 ID, or PROPOSED_NEEDS_CROSS_DOMAIN_REGISTRY
NEEDED_LEVEL: CONTRACT_READY | INTEGRATION_READY | EVIDENCE_READY
EXACT_INPUT: approved DTO/API/event, source revision, code owner lock, proof/receipt, authorization, PG18/HTTPS test
NEEDED_BY_DATE:
CURRENT_STATUS: CONTRACT_PENDING | BLOCKED_APPROVAL | WAITING_FOR_HANDOFF | CONSUMER_VERIFYING
POTENTIAL_CONFLICT_PATHS:
P1_PART_NOT_BLOCKED: part of this task safe to implement under approved authority
NEXT_INDEPENDENT_TASK: exact own Task ID, smallest executable step
CROSS_DOMAIN_DECISION_OR_REVIEW_REQUIRED: YES/NO + affected Co-Owners/reviewer
DELIVERY_CHANNEL_AND_ACK: only fill after actual transmission/receipt
```

**Không tự mở khóa bằng dữ liệu giả:** Mock/fixtures chỉ dùng cho unit/contract/UI prototype tests và phải ghi rõ; không dùng để báo Station Accepted, Manifest Hub Received, Courier delivered/POD, Pickup collected hoặc Finance PAID. Không gửi thông báo giả; Codex chỉ tạo bản nháp nếu không có connector/kênh gửi thật.

## 8. Shared dashboard S1–S8 & Joint GO/NO-GO

Tối thiểu 8 hàng, một hàng cho S1–S8:

```text
DATE | SLICE | SOURCE_REVISION | UI_GATE | BACKEND_GATE | PG18_GATE | SECURITY_GATE
INTEGRATION_GATE | PERSON_BUSINESS | DROP_PICK (where relevant)
LAST_HANDOFF_ACK | BLOCKER_ID | BLOCKER_OWNER | NEXT_INDEPENDENT_TASK
TARGET_DATE | ACTUAL_PASS_EVIDENCE | STATUS (PASS/PARTIAL/NO-GO)
```

**Định nghĩa 4 mandatory journeys:** `V1-E2E-P-DROP`, `V1-E2E-P-PICK`, `V1-E2E-B-DROP`, `V1-E2E-B-PICK`. Tất cả phải qua correct Customer/Business admission→Quote/Booking/Parcel/Label→Drop-off hay genuine Courier Pickup→Station custody→Inventory/Manifest→Transfer/Hub→Dispatch/Courier Mobile→Attempt/POD→Delivered→Tracking/Finance/CSKH supported gates; security/negative/rollback và payment policy được duyệt. **Không rút phần UI để đủ tiến độ.**

**Three Co-Three Co-Owner Decision Gate — 05/12:**
- **GO / `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST`:** tất cả hard gates và bốn journeys đã có current matching proof.
- **PARTIAL / NO-GO:** chỉ rõ đúng Slice/Task, missing approval/contract/ui/backend/integration/evidence và date/risk. Lập recovery backlog sau quyết định 05/12; không hợp thức hóa chưa PASS.
- **Nếu 05/12 NO-GO:** ghi chính xác blockers + owner + recovery plan; mọi patch sau gate giữ source preservation, authority và signed re-evidence. Nếu cần deadline mới sau 05/12, phải có quyết định chung của ba Co-Owners. Không hồi tố GO khi chỉ hoàn thành các tests về sau.

## 9. Prompt khởi động mỗi ngày — copy cho Codex, điền ROLE đúng file

```text
MỤC 5 — NHIỆM VỤ HIỆN TẠI: ARGOPOST V1 DAILY EXECUTION

Bạn đang giữ ROLE_ID xác định trong file assignment v1.4 của mình: V1-A/Nam, V1-B/Hoàng hoặc V1-C/Triết.
Đọc đầy đủ file assignment của đúng ROLE_ID, rồi đọc:
docs/implementation/v1-team/ArgoPost_V1_Team_Execution_Calendar_2026-11-28_to_2026-12-05.md
(nếu file nằm ở nơi khác, dùng đường dẫn thật; không tự tạo file giả).

Hôm nay: [YYYY-MM-DD ICT]. Trước khi làm, đối chiếu date/status với source, approvals,
D0 baseline, handoff registry, current Git/worktree và evidence; không lặp lại code PASS.

1. Đọc P0/P1/P2 trong hàng của hôm nay và P0 1–2 ngày tới.
2. Nếu P0 phụ thuộc Member khác: xuất DEPENDENCY_NOTICE chính xác, chỉ claim đã gửi nếu kênh thật xác nhận.
3. Nếu P0 đủ approved authority, đúng CODE_WRITER và input: triển khai smallest unblocker và tạo handoff packet ngay, không đợi full Slice.
4. Nếu P0 blocked: WAITING_FOR_HANDOFF hoặc BLOCKED_APPROVAL, báo Co-Owners liên quan/theo escalation, chuyển NGAY sang P2 independent task và triển khai phần được phép.
5. Sau mỗi feature, kiểm chứng actor Web/Mobile→HTTPS Backend→PostgreSQL18 + denial/rollback/idempotency; không báo FULL_STACK_PASS từ API-only/UI fixture.
6. Tích hợp theo approved source/revision, cùng test environment, consumer ACK; Git operations phải đúng repository access/AGENTS/CI; không deploy, reset shared DB, sửa source/member khác hay đổi approval.
7. Kết thúc bằng báo cáo: EXACT TASK_IDS, DONE/PARTIAL/BLOCKED, APPROVAL, SOURCE_REVISION,
   UI_GATE, BACKEND_GATE, PG18_GATE, INTEGRATION_GATE, HANDOFF packet+ACK,
   blocker/provider/deadline, NEXT_INDEPENDENT_TASK, 24h risk.

Bắt đầu với read-only preflight; sau đó thực hiện ngay phần có approved authority.
```

---

**Ghi chú điều phối:** Tài liệu này là **operational companion schedule**, không thay thế `TEAM-ASSIGNMENT-v1.5`. Khi source thực tế hoặc gate ngày trước khác dự kiến, **giữ nguyên các hard business/security gates**, điều chỉnh thứ tự task *trong scope* theo P0→P1→P2; không tự đổi CODE_WRITER, deadline **05/12/2026** hay V1 acceptance. Nếu lịch mục tiêu không khả thi, báo NO-GO risk sớm thay vì khai báo giả đã PASS.
