# Spec 010 — pickup-dispatch-execution

Alias lịch sử logical `L010`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `dispatch`; từng BC giữ DML owner riêng.

## Outcome và boundary

Dispatcher giao việc, Courier nhận kiện. Pickup pool/assignment/version/attempt/scan/custody API.

Workflow: Assign→scan/count→nhận→handover; stale/reassign/double scan deny. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L007, L008, L009. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-14; D-V3-05. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-037 — Phê duyệt ngoại lệ điều phối theo thẩm quyền

Source: BC-037/R04/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự tạo bằng chứng pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-037-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phê duyệt ngoại lệ điều phối theo thẩm quyền; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Không tự tạo bằng chứng pickup»; reload/API/PG assertion; TEST-V3-BC-037-FLOW.
- **AC-BC-037-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-037-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-047 — Xem pickup pool và phân công pickup

Source: BC-047/R06/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Theo vùng, tải và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-047-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem pickup pool và phân công pickup; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Theo vùng, tải và trạng thái»; reload/API/PG assertion; TEST-V3-BC-047-FLOW.
- **AC-BC-047-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-047-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-049 — Tái phân công pickup có lý do

Source: BC-049/R06/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Audit, không tự xác nhận pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-049-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tái phân công pickup có lý do; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Audit, không tự xác nhận pickup»; reload/API/PG assertion; TEST-V3-BC-049-FLOW.
- **AC-BC-049-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-049-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-056 — Xem pickup assignment cá nhân

Source: BC-056/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ task được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-056-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem pickup assignment cá nhân; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Chỉ task được phân công»; reload/API/PG assertion; TEST-V3-BC-056-FLOW.
- **AC-BC-056-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-056-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-057 — Check-in/scan và ghi nhận PICKED_UP

Source: BC-057/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có bằng chứng, đúng trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-057-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Check-in/scan và ghi nhận PICKED_UP; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Có bằng chứng, đúng trạng thái»; reload/API/PG assertion; TEST-V3-BC-057-FLOW.
- **AC-BC-057-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-057-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-058 — Ghi pickup thất bại và lý do

Source: BC-058/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Attempt có timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-058-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi pickup thất bại và lý do; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Attempt có timestamp»; reload/API/PG assertion; TEST-V3-BC-058-FLOW.
- **AC-BC-058-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-058-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-062 — Tối ưu thứ tự tuyến pickup

Source: BC-062/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không bắt buộc GPS/routing chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-062-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tối ưu thứ tự tuyến pickup; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Không bắt buộc GPS/routing chuyên sâu»; reload/API/PG assertion; TEST-V3-BC-062-FLOW.
- **AC-BC-062-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-062-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-141 — Đặt yêu cầu Pickup tại nhà

Source: BC-141/R18/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dịch vụ/địa chỉ đủ điều kiện; phân công do Operations; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `dispatch`.

- **AC-BC-141-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Đặt yêu cầu Pickup tại nhà; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Dịch vụ/địa chỉ đủ điều kiện»; reload/API/PG assertion; TEST-V3-BC-141-FLOW.
- **AC-BC-141-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-141-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-207 — Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại

Source: BC-207/R06/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Station/region scope; chỉ assignment đủ điều kiện; chống phân công trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-207-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Station/region scope»; reload/API/PG assertion; TEST-V3-BC-207-FLOW.
- **AC-BC-207-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-207-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-210 — Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender

Source: BC-210/R07/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ nhiệm vụ đang được giao; PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-210-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Chỉ nhiệm vụ đang được giao»; reload/API/PG assertion; TEST-V3-BC-210-FLOW.
- **AC-BC-210-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-210-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-211 — Liên hệ Merchant/Retail Sender phục vụ pickup

Source: BC-211/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ trong phạm vi nhiệm vụ; log/quy tắc bảo vệ liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-211-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Liên hệ Merchant/Retail Sender phục vụ pickup; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Chỉ trong phạm vi nhiệm vụ»; reload/API/PG assertion; TEST-V3-BC-211-FLOW.
- **AC-BC-211-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-211-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-214 — Xác nhận thời gian và bằng chứng bàn giao nhận kiện

Source: BC-214/R07/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Assignment hợp lệ; timestamp, actor, custody; không button-only PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-214-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác nhận thời gian và bằng chứng bàn giao nhận kiện; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Assignment hợp lệ»; reload/API/PG assertion; TEST-V3-BC-214-FLOW.
- **AC-BC-214-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-214-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-244 — Xem nhiệm vụ Pickup Courier toàn đội

Source: BC-244/R24/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ đội/ca được giao; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-244-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem nhiệm vụ Pickup Courier toàn đội; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Chỉ đội/ca được giao»; reload/API/PG assertion; TEST-V3-BC-244-FLOW.
- **AC-BC-244-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-244-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-247 — Đề xuất tái phân công pickup

Source: BC-247/R24/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Approval/authorization tách biệt với hành động reassignment; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-247-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đề xuất tái phân công pickup; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Approval/authorization tách biệt với hành động reassignment»; reload/API/PG assertion; TEST-V3-BC-247-FLOW.
- **AC-BC-247-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-247-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-277 — Giám sát năng lực pickup giữa các khu vực

Source: BC-277/R27/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự tạo chứng cứ PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-277-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát năng lực pickup giữa các khu vực; Then outcome lưu/đọc đúng pickup request/assignment/evidence, giữ guard «Không tự tạo chứng cứ PICKED_UP»; reload/API/PG assertion; TEST-V3-BC-277-FLOW.
- **AC-BC-277-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-277-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-008-01 | Một leg tối đa 1 ASSIGNED/IN_PROGRESS active; reassign đóng old + tạo new atomically, reason/audit. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-008-04 | Shipper chỉ scan task được phân công; các kiện sai hub/sai manifest vào exception có người phụ trách. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-009-01 | Chỉ xử lý đúng shipment/leg trong assignment active của shipper, revalidate tại server lúc ghi nhận. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.
