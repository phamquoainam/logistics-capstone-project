# Spec 016 — returns-reverse-logistics

Alias lịch sử logical `L016`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Returns và bằng chứng gửi lại. Orders return/state; dispatch reverse custody; warehouse QC khi WMS.

Workflow: Failed→retry hoặc approved return→returning→handed back; không cancel sau pickup. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L011, L015. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-09; DEC-16; D-V3-05. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-052 — Điều phối hoàn hàng theo workflow

Source: BC-052/R06/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa lịch sử delivery; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-052-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều phối hoàn hàng theo workflow; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Không xóa lịch sử delivery»; reload/API/PG assertion; TEST-V3-BC-052-FLOW.
- **AC-BC-052-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-052-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-067 — Nhận kiện giao thất bại/đang hoàn

Source: BC-067/R08/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng trạng thái custody; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-067-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhận kiện giao thất bại/đang hoàn; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Đúng trạng thái custody»; reload/API/PG assertion; TEST-V3-BC-067-FLOW.
- **AC-BC-067-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-067-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-077 — Trả kiện giao thất bại về Station

Source: BC-077/R09/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có custody handover; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-077-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Trả kiện giao thất bại về Station; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Có custody handover»; reload/API/PG assertion; TEST-V3-BC-077-FLOW.
- **AC-BC-077-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-077-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-111 — Theo dõi ticket liên quan hoàn hàng

Source: BC-111/R14/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự sửa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-111-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi ticket liên quan hoàn hàng; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Không tự sửa custody»; reload/API/PG assertion; TEST-V3-BC-111-FLOW.
- **AC-BC-111-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-111-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-146 — Theo dõi và quản lý hoàn trả kiện gửi lẻ

Source: BC-146/R18/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Return events/custody rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-146-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Theo dõi và quản lý hoàn trả kiện gửi lẻ; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Return events/custody rõ ràng»; reload/API/PG assertion; TEST-V3-BC-146-FLOW.
- **AC-BC-146-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-146-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-178 — Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái

Source: BC-178/R03/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hàng đã pickup phải đi reverse logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-178-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Hàng đã pickup phải đi reverse logistics»; reload/API/PG assertion; TEST-V3-BC-178-FLOW.
- **AC-BC-178-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-178-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-198 — Theo dõi hoàn hàng và trao đổi kết quả với Recipient

Source: BC-198/R21/M14, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không thao tác custody của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-198-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi hoàn hàng và trao đổi kết quả với Recipient; Then outcome lưu/đọc đúng return authorization/events, giữ guard «Không thao tác custody của hãng»; reload/API/PG assertion; TEST-V3-BC-198-FLOW.
- **AC-BC-198-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-198-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-005-04 | Cancel chỉ trước mốc picked-up trực tiếp hoặc WMS picking; release unused reserved; refund theo policy bằng nghiệp vụ riêng, không xóa ledger. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |
| REQ-007-01 | Stock theo owner, SKU, warehouse, location và condition; available = eligible − remaining holds ≥ 0. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-02 | Reservation consumed/released/remaining với invariant reserved = consumed + released + remaining. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-03 | Mọi stock move từ chứng từ, append-only history; transfer bảo toàn tổng; kiểm kê không trực tiếp ghi balance. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-04 | Pick/outbound không vượt reservation remaining; hủy chỉ giải phóng unused; hàng xuất phải qua return receipt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-05 | Nhân viên chỉ thao tác kho được cấp; duplicate scan/command không tạo movement lặp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-009-03 | Failed delivery có reason và attempt ID; giao lại/hoàn theo policy, không nhảy trạng thái trực tiếp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-RETURNS

Owner target: L016; source BC-146;BC-178;BC-198; latest Owner portal scope, no new BC ID. UI/channel: Personal/Merchant return visibility and authorized request.

Required behavior: After pickup use reverse logistics, never ordinary cancel; return authorization/source/evidence/status/custody visible by ownership. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-RETURNS:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Failure/return/retry request cannot silently set custody or refund; historical delivery attempts preserved; rejected request reason safe. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-RETURNS PLANNED_NOT_EXECUTED. Dependencies: L011;L015;L018.
