# ARGOPOST V1 — LỊCH ĐIỀU PHỐI CHUNG 29/11–07/12/2026

> **Version:** `TEAM-CALENDAR-v1.6 — 29NOV–07DEC-2026` · ngày lập 29/11/2026.  
> **Active timeline:** 29/11–07/12 (9 ngày lịch; Việt Nam; **không đặt giờ cố định**).  
> **3 Co-Owners ngang quyền:** **Nam** (Customer/Commercial, V1-A), **Hoàng** (Network/Custody, V1-B), **Triết** (Dispatch/Courier, V1-C).  
> **Team release target:** 07/12/2026, `TEAM-V1-GO-NOGO`. Đây là lịch thay cho toàn bộ lịch cũ 28/11–05/12; không song song sử dụng hai lịch. Đề xuất đổi ngày do Nam đưa ra phải được Hoàng và Triết ACK theo quyết định chung ba Co-Owners trước khi coi là release commitment.  
> **S1 source fact:** báo cáo 27/11 xác nhận `V1_SLICE_1_DUAL_AUDIENCE_READY — SOURCE/TEST`. **Không** mặc nhiên khẳng định `develop` đã có commit này hoặc Docker clean clone đã PASS. **Spec012** vẫn PROPOSED trừ khi có technical review mới.

## A. Quy tắc Codex chọn việc mỗi ngày

1. Đọc file assignment v1.6 của **đúng người**, lịch hôm nay, source/`git status`, branch/merge SHA, approvals, handoffs and tests. Nếu ngày đang đọc sau ngày mục tiêu, ghi actual status trước khi tiếp tục; không tự đóng task theo lịch.
2. **P0 — unblock others:** tạo/nhận approved contract, minimal reader/DTO, handoff source hoặc real evidence; gửi `DEPENDENCY_NOTICE` khi sẽ cần trong 1–2 ngày; không chờ cả Slice.
3. **P1 — own Full-Stack:** theo domain writer, hoàn thiện actor Web/Mobile → HTTPS Backend → PostgreSQL18 + auth/negative/idempotency/rollback; làm UI cùng lúc với backend, không bỏ UI đến ngày cuối.
4. **P2 — independent fallback:** khi contract, shared CODE_WRITER, source hoặc physical event chưa có, chuyển sang task độc lập có authority; `WAITING_FOR_HANDOFF` hoặc `BLOCKED_APPROVAL` phải ghi exact blocker.
5. Mỗi phiên chỉ **1 runtime task + 1 audit/contract/test-plan task**. Source shared/migration/Workflow chỉ có một CODE_WRITER; các Co-Owners của affected domains chọn reviewer và tích hợp đúng quyền thực tế. Không tự ghi event/custody/PAID/POD giả.
6. Mỗi ngày giao source/contract/evidence ngay khi đủ level `CONTRACT_READY → INTEGRATION_READY → EVIDENCE_READY` và consumer ACK. Cuối ngày báo cáo run IDs, revision, current status, next independent task. Git push/merge chỉ khi có authorization, CI và branch protections PASS.

**File cần đọc:**
- Nam: `ArgoPost_V1_Member_01_Owner_Customer_Commercial.md`
- Hoàng: `ArgoPost_V1_Member_02_Carrier_Network_Custody.md`
- Triết: `ArgoPost_V1_Member_03_Dispatch_Courier_Delivery.md`

## B. Mốc dự kiến HOÀN CHỈNH của từng Slice (không phải ngày bắt đầu code)

| Slice | Ngày dự kiến `SLICE_FULL_STACK_PASS` | Chứng cứ tối thiểu / điều kiện |
|---|---|---|
| **S1** | **Đã PASS SOURCE/TEST 27/11**; target **30/11** xác nhận trên baseline mới | Reuse BIA V48/V49/Business-Personal E2E; `develop` merge + Compose clean clone + S1 regression kiểm chứng riêng |
| **S2** | **03/12** | Station Inventory/Sort/Manifest Draft→Seal Ops Web+API+PG18, scoped ACL/security/concurrency; Spec012 approved |
| **S3** | **04/12** | Sender handover → Transfer → Hub receive/reconcile/variance; real custody/tracking + Ops Web + Spec013 approved |
| **S4** | **05/12** | Dispatcher Web → Courier Mobile → Attempt/POD → Delivered, actual Hub→Courier custody, approved proof policies |
| **S5** | **06/12** | Personal & Business Pickup Request→Courier→Station→shared delivery pipeline with UI/Mobile/PG18 |
| **S6** | **06/12** | Failed pickup/delivery, retry/return and reverse custody; Ops/Courier/Customer/CSKH affected UI and negatives |
| **S7** | **06/12** | Business XLSX/CSV→actual Booking; one supported/approved payment reconciliation; Claims/CSKH UI + real facts |
| **S8** | **07/12** | All four integrated journeys (Personal/Business × Drop-off/Pickup), same revision full-stack and signed security/finance tests |

**Forecast caveat:** Với S2–S8, đây là kế hoạch tham vọng, **không đảm bảo** chắc chắn. Đặc biệt Spec012, Transfer/Dispatch/POD approvals, mobile runtime và settlement policy phải đạt review thì mới bắt đầu những phần runtime được bảo vệ. Nếu trễ một hard dependency, báo risk/NO-GO ngay, không giảm tiêu chí Full-Stack hoặc âm thầm đổi ngày.

## C. Lịch 9 ngày — Nam, Hoàng, Triết triển khai song song

| Ngày | Nam — Customer & Commercial | Hoàng — Network/Custody | Triết — Dispatch/Courier | Gate/handoff ưu tiên |
|---|---|---|---|---|
| **T1 — 29/11/2026** | `A-S1-04/05/06, A-S8-01`: Verify S1 27/11 evidence, V48/V49 and baseline Docker/develop state; handoff canonical Booking/Parcel/custody readers to Hoàng; send early Pickup DTO notice Triết | `B-S1-01/05/06/07, B-S2-01`: Receive S1 source/handback and writer lock; verify Spec012 proposal vs current schema, send consolidated policy/ACL review; baseline/Compose audit | `C-S1-01/02/03, C-S4-01`: Audit Mobile, Courier identity/auth, Spec014/015/029 approval and current Dispatch; send Hoàng handover/eligibility interface need | S1 SOURCE handoff & portable baseline audit; Spec012 review requested |
| **T2 — 30/11/2026** | `A-S5-01/02/03, A-S7-01`: Approve/chốt Pickup Request contract with Triết; reconcile UI mode/coverage/cancel/idempotency; S1 clean clone regression when baseline ready | `B-S2-01/02/05, B-S4-01`: Finalize Spec012 minimum approved bounded scope; send Triết B2C Dispatch contract draft for review; build allowed Inventory read and Operations UI | `C-S4-01/02/03, C-S2-01/02`: Agree B2C DispatchEligibility/Hub handover CONTRACT with Hoàng, build approved DeliveryRun/Assignment core + Courier Mobile foundation | Pickup & Dispatch contract sent; baseline regression |
| **T3 — 01/12/2026** | `A-S5-01/02/03/04, A-S7-01`: Implement approved Personal/Business Pickup Request Web + Backend/PG18; complete valid Excel/CSV→actual Booking integration | `B-S2-02/03/04/05/06`: Implement Sorting→Manifest Draft/Seal with canonical accepted Parcels, scoped Ops Web, PG18 negative/idempotency if Spec012 approved | `C-S4-01/02/03/09, C-S5-01/03`: Implement Dispatcher Web task list/create/assign, Mobile login/task detail, version/stale/deny tests under approved spec | First Inventory UI/API + Mobile/Dispatch foundation |
| **T4 — 02/12/2026** | `A-S5-01/04, A-S2-01, A-S7-02`: Deliver A2C Pickup SOURCE/EVIDENCE level available; real scoped owner/Org admission and denial; ensure Hoàng sees Parcel reader contract | `B-S2-01/02/03/04/05/06, B-S3-01`: Complete Manifest exact-set, Seal/immutability, roles/ACL/negative and Ops Web; start Transfer implementation after approved Spec013 | `C-S4-03/05/06/08, C-S5-01/02`: Consume Nam Pickup Request source, build Pickup assignment and Mobile task; build Attempt/POD validated flow with privacy | Pickup Request implementation source, Manifest Seal/Hub prep |
| **T5 — 03/12/2026** | `A-S2-02, A-S7-01/05, A-S8-01`: Consume S2 sorting/manifest reader; Customer/Business Operations timeline Web; Bulk negative/duplicate; CSKH claims UI | `B-S2-01/06, B-S3-01/02/03`: S2 FULL-STACK PASS target; deliver inventory/manifest events Nam; build signed Transfer/Handover, Hub reception first vertical | `C-S4-01/02/04/05/06/09, C-S3-01/02`: Finish Dispatcher Web + Courier Mobile full-stack core; contract tests Hub-to-Courier while Hoàng completes S3; start exception UI | S2 full-stack goal + S3 first vertical |
| **T6 — 04/12/2026** | `A-S3-01, A-S7-03/04/06, A-S8-02`: Consume actual Hub/Transfer events, complete customer tracking; Finance supported payment + reconciliation implementation only under approved contract | `B-S3-01/02/03/04/05/06, B-S4-01/02/03`: S3 sender handover→transfer→Hub receipt/reconcile full-stack; emit B2A/B2C Hub evidence and dispatch eligibility | `C-S3-01/02, C-S4-04/05/06/07, C-S5-01`: With Hoàng real Hub Receiving handover, courier accept scan, Attempt/POD actual E2E first pass; Nam POD reader contract | S3 full-stack goal; actual Hub-to-Courier handoff |
| **T7 — 05/12/2026** | `A-S4-01/02, A-S6-01/02/03, A-S7-04/05`: Receive genuine Courier POD; customer Delivered tracking, claim-linked attempt; Finance event binding | `B-S4-02/03, B-S5-01/02, B-S7-01/02`: Hub→Courier release with Triết, Pickup Station Receiving real UI/PG18, network facts to Nam; return contract | `C-S4-01/02/03/04/05/06/07/08/09, C-S7-01/02`: S4 FULL-STACK PASS target: Dispatcher Web→Courier Mobile→Attempt/POD/Delivered with real custody; deliver actual POD/fee facts Nam | S4 full-stack goal + POD/finance facts |
| **T8 — 06/12/2026** | `A-S5-01/04, A-S7-01/06, A-S8-01/03`: Personal/Business Pickup end-to-end customer UI; finish supported payment/reconciliation, import booking, CSKH/Claims evidence; S8 rehearsal | `B-S5-02/03, B-S6-01/02/03/04, B-S8-01/02`: Pickup→Station actual acceptance for Personal/Business; return custody/Inventory Hold/exception UI; four journey rehearsal | `C-S5-01/02/03/04/05/06/07, C-S6-01/02/03/04/05, C-S8-01`: Personal/Business Pickup→Station→same downstream delivery; Failed Pickup/Delivery Retry/Return with Hoàng; joint 4-journey rehearsal | S5/S6/S7 full-stack goals; 4-journey rehearsal |
| **T9 — 07/12/2026** | `A-S8-01/02/03/04/05`: Joint 4 journey final Web/Backend/PG18 regression, compare all role evidence and sign decision | `B-S8-01/02/03/04`: Run final Station/Inventory/Manifest/Hub/Return and security/ACL regression on joint revision; co-owner signoff | `C-S8-01/02/03/04/05`: Final Courier Mobile/Dispatcher Web, POD privacy/auth and pickup/return regression on integrated revision; signoff with Nam/Hoàng | S8 final joint signoff and GO/NO-GO |

## D. Ngày mục tiêu bàn giao giữa ba người — không chờ trọn Slice

| Ngày | Người giao → nhận | Handoff ID và điều kiện |
|---|---|---|
| **29/11** | Nam ↔ Hoàng | `A2B-S1-BOOKED-CARGO`, `B2A-S1-ACCEPTED-EVENT`: reuse real S1 proofs + current source fingerprint; **không xây lại S1** |
| **29/11** | Nam + Hoàng + Triết | `TEAM-BASELINE-29NOV`: inventory Git/develop, Docker clean-clone/DB/credentials policy, CODE_WRITER locks; report BLOCKED if release unfinished |
| **30/11** | Nam → Triết | `A2C-S5-PICKUP-REQUEST` CONTRACT approved (Personal/Business, owner/scope, time/coverage, cancellation) |
| **30/11** | Hoàng → Triết | `B2C-S4-DISPATCH-READY` CONTRACT approved (holder/site/Parcel/eligibility/version) |
| **01/12** | Hoàng → team technical reviewers | `SPM-S2-CONTRACT` Spec012 policy/API/ACL/DB disposition; PROPOSED không cho runtime write |
| **02/12** | Nam → Triết | `A2C-S5-PICKUP-REQUEST` actual source/HTTPS+PG18 ready for Pickup consumer, hoặc exact blocked reason |
| **03/12** | Hoàng → Nam | `B2A-S2/S3-EVENTS` S2 inventory/manifest subset accepted; S3 still pending |
| **04/12** | Hoàng → Nam và Triết | `B2A-S2/S3-EVENTS` S3 actual Hub; `B2C-S4-DISPATCH-READY` EVIDENCE, exact hub custody |
| **05/12** | Triết → Nam, Hoàng → Nam | `C2A-S4-POD-FACTS`, `B2A-S7-NETWORK-FACTS`, `C2A-S7-FINANCE-EVIDENCE` real refs; not proof PAID |
| **05–06/12** | Triết → Hoàng | `C2B-S5-PICKUP-HANDOVER`: Courier pickup→Station acceptance, real Parcel and actor binding |
| **06/12** | Triết → Hoàng; toàn đội | `C2B-S6-RETURN-HANDOVER`; `TEAM-S8-REHEARSAL` full-stack 4 journey attempt |
| **07/12** | Nam + Hoàng + Triết | `TEAM-S8-REVALIDATION` → `TEAM-S8-SIGNOFF` → `TEAM-V1-GO-NOGO`, source/evidence same revision |

## E. Chống chờ, chống conflict, chống false PASS

- **Publisher baseline tạm thời:** Nam tiếp tục phát hành portable `develop`/Docker Compose chỉ trong exact release authorization. Báo cáo S1 PASS không chứng minh publish đã hoàn thành. Không để Hoàng/Triết cùng sửa Workflow/Docker/db migrations trước khi source lock được release; họ được audit, soạn contracts, Mobile tasks độc lập.
- **Nam mở khóa sớm:** Orders/Parcel/S1 reuse ngay; Pickup Request contract trước, UI/API evidence sau. Hoàng/Triết không phải đợi Nam xong Finance/Claims.
- **Hoàng mở khóa sớm:** chốt Spec012/013 và DispatchEligibility contract, rồi gửi genuine Manifest/Hub facts sau; Triết không phải đợi S3 E2E mới làm DeliveryRun/Mobile.
- **Triết mở khóa sớm:** Mobile/Dispatcher Web/POD contract và actor tests độc lập; không claim Delivered khi chưa nhận Hub→Courier handover; báo POD reader cho Nam đúng source.
- **S5 hội tụ đúng canonical custody:** Nam Request → Triết Collection → Hoàng Station Receiving → Hoàng S2/S3 → Triết S4; không có engine Pickup/Parcel khác.
- **Finance không bỏ qua security:** `UNPAID` không suy ra revenue, `PAID`/COD chỉ từ approved verified settlement; nếu chưa có approved method thì S7 FAIL/PARTIAL.
- **Definition of Done:** `UI_ACTION + BACKEND_API + PG18_TRANSACTIONS + AUTH + NEGATIVE/RACE/REPLAY + READBACK + INTEGRATION_ACK + EVIDENCE_FINGERPRINT`; Backend-only, mock-only và contract-only không là Slice PASS.
- **Không ép giờ:** mỗi người tự kiểm tra lịch khi bắt đầu làm việc; bàn giao ngay khi đủ điều kiện; cùng đánh giá trước khi kết thúc ngày. Nếu mốc trễ, thông báo ngay khi phát hiện, chuyển sang `NEXT_INDEPENDENT_TASK`.
- **TEAM GOVERNANCE:** ba Co-Owners đồng cấp; domain task approved tự thực hiện; shared changes được affected domain owners thống nhất, review độc lập khi cần; protected ACL/migration/security still gated. Chỉ ký GO khi cả ba có PASS trên một revision.

## F. Daily Codex prompt mẫu

```text
MỤC 5 — ARGOPOST V1.6 DAILY EXECUTION

Tôi là [Nam / Hoàng / Triết], Co-Owner; ROLE_ID tương ứng V1-A/V1-B/V1-C.
Đọc toàn bộ assignment v1.6 đúng ROLE_ID và lịch duy nhất:
docs/implementation/v1-team/ArgoPost_V1_Team_Execution_Calendar_2026-11-29_to_2026-12-07.md

Xác nhận ngày Việt Nam hiện tại, exact Git/source revision, status S1 đã PASS trong report 27/11 nhưng baseline develop/Compose phải xác minh riêng, review/approval mới nhất, CODE_WRITER locks và handoff packet.

Thực hiện hàng kế hoạch hôm nay + handoffs cần trong 1–2 ngày. Ưu tiên:
P0 — smallest approved unblocker cho người khác;
P1 — own Full-Stack UI→HTTPS Backend→PG18 implementation/tests;
P2 — nếu P0/P1 blocked, DEPENDENCY_NOTICE rồi NEXT_INDEPENDENT_TASK.

Không làm lại S1 đã PASS, không giả event/PAID/POD, không sửa shared source chưa có writer lock, không biến proposal thành approval. Git operations theo exact release rights/CI.

Báo TASK_ID | status | UI/API/PG18/security/integration evidence | source hash | handoff/ACK | blocker | next independent task | risk to 07/12.
Bắt đầu bằng read-only preflight rồi thực thi phần có authority, không dừng ở việc chỉ lập kế hoạch.
```

---

**Final release:** `V1_OPERATIONAL_E2E_ACCEPTED — SOURCE/TEST` chỉ khi Nam, Hoàng, Triết đều sign-off bốn journeys và mọi mandatory hard gates; nếu không, `PARTIAL/NO-GO` và recovery backlog. Lịch mới không phải Production/UAT authority.
