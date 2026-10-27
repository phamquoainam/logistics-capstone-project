# Spec 015 — lastmile-attempt-pod

Alias lịch sử logical `L015`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Delivery attempt/POD và COD thực thu. Orders attempt/POD/result, dispatch authority, finance collection cùng workflow.

Workflow: OFD→POD hoặc failed reason; COD theo giá trị chốt; orphan upload có cleanup. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L009, L014, L017. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-15; DEC-16; D-V3-04. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-073 — Ghi DELIVERED kèm POD

Source: BC-073/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Bằng chứng hợp lệ; chống trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-073-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi DELIVERED kèm POD; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Bằng chứng hợp lệ»; reload/API/PG assertion; TEST-V3-BC-073-FLOW.
- **AC-BC-073-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-073-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-074 — Ghi DELIVERY_FAILED kèm lý do/attempt

Source: BC-074/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa thất bại; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-074-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi DELIVERY_FAILED kèm lý do/attempt; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Không xóa thất bại»; reload/API/PG assertion; TEST-V3-BC-074-FLOW.
- **AC-BC-074-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-074-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-081 — Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có

Source: BC-081/R10/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Courier ghi nhận; không cấp role IAM; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-081-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Courier ghi nhận»; reload/API/PG assertion; TEST-V3-BC-081-FLOW.
- **AC-BC-081-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-081-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-084 — Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier

Source: BC-084/R10/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Nhân sự được quyền xử lý theo state; Recipient không thao tác backend; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-084-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Nhân sự được quyền xử lý theo state»; reload/API/PG assertion; TEST-V3-BC-084-FLOW.
- **AC-BC-084-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-084-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-227 — Ghi nhận bằng chứng và tăng số lần giao thất bại

Source: BC-227/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Lý do, timestamp, evidence, attempt ID độc lập; không xóa history; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-227-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận bằng chứng và tăng số lần giao thất bại; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Lý do, timestamp, evidence, attempt ID độc lập»; reload/API/PG assertion; TEST-V3-BC-227-FLOW.
- **AC-BC-227-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-227-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-299 — Đối chiếu delivery attempts và bằng chứng POD

Source: BC-299/R15/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chứng cứ nguồn gốc; xử lý tranh chấp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-299-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đối chiếu delivery attempts và bằng chứng POD; Then outcome lưu/đọc đúng delivery attempt/POD/instructions, giữ guard «Chứng cứ nguồn gốc»; reload/API/PG assertion; TEST-V3-BC-299-FLOW.
- **AC-BC-299-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-299-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-355 — Không được giả POD/DELIVERED

Source: BC-355/R31/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không bypass chứng cứ dù là Super Admin; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-355-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Không được giả POD/DELIVERED; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-355-DENY.
- **AC-BC-355-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-355-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-009-02 | Mỗi delivery attempt có ID; giao thành công bắt buộc POD tối thiểu theo policy; POD gắn đúng attempt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |
| REQ-009-03 | Failed delivery có reason và attempt ID; giao lại/hoàn theo policy, không nhảy trạng thái trực tiếp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |
| REQ-009-05 | POD metadata ở DB và file private object storage, giới hạn upload/permission; offline action chỉ khi policy cho phép. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-RECIPIENT

Owner target: L015; source BC-080;BC-081;BC-084; latest Owner portal scope, no new BC ID. UI/channel: Verified scoped delivery response/Courier/CS.

Required behavior: Notify/limited tracking, POD/OTP when policy, requested reschedule/instructions/alternate receiver/refusal/address change request; no forced IAM; authorized workflow owns final change. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-RECIPIENT:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Token cannot cancel/alter COD/final shipment facts or grant Merchant authority; expired/replayed verification denied; approved request produces separate attempt/event with evidence. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-RECIPIENT PLANNED_NOT_EXECUTED. Dependencies: L009;L014;L020.
