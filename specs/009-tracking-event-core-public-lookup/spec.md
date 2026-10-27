# Spec 009 — tracking-event-core-public-lookup

Alias lịch sử logical `L009`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Tracking an toàn và có nguồn. Canonical append-only events, privacy projection/rate limit.

Workflow: Source commit→timeline; đoán code/UUID không lộ PII; late/duplicate không sửa lịch sử. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L006. Consumer/soft: L028. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-06; DEC-20. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-003 — Tra cứu tracking giới hạn

Source: BC-003/R01/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không lộ PII, COD nhạy cảm; tăng xác minh khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-003-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Tra cứu tracking giới hạn; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không lộ PII, COD nhạy cảm»; reload/API/PG assertion; TEST-V3-BC-003-FLOW.
- **AC-BC-003-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-003-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-013 — Theo dõi mọi Shipment thuộc Merchant

Source: BC-013/R02/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không cross-tenant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-013-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi mọi Shipment thuộc Merchant; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không cross-tenant»; reload/API/PG assertion; TEST-V3-BC-013-FLOW.
- **AC-BC-013-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-013-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-027 — Tra cứu trạng thái Shipment thuộc scope

Source: BC-027/R03/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-027-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tra cứu trạng thái Shipment thuộc scope; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Merchant isolation»; reload/API/PG assertion; TEST-V3-BC-027-FLOW.
- **AC-BC-027-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-027-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-051 — Theo dõi sự kiện và nhiệm vụ vận chuyển

Source: BC-051/R06/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không truy cập ngoài region; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-051-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi sự kiện và nhiệm vụ vận chuyển; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không truy cập ngoài region»; reload/API/PG assertion; TEST-V3-BC-051-FLOW.
- **AC-BC-051-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-051-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-060 — Xem lịch sử nhiệm vụ cá nhân

Source: BC-060/R07/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-060-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem lịch sử nhiệm vụ cá nhân; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «PII tối thiểu»; reload/API/PG assertion; TEST-V3-BC-060-FLOW.
- **AC-BC-060-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-060-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-068 — Xem trạng thái kiện tại Station

Source: BC-068/R08/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-068-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem trạng thái kiện tại Station; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Đúng station scope»; reload/API/PG assertion; TEST-V3-BC-068-FLOW.
- **AC-BC-068-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-068-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-078 — Xem thông tin liên hệ người nhận tối thiểu

Source: BC-078/R09/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ trong assignment; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-078-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem thông tin liên hệ người nhận tối thiểu; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Chỉ trong assignment»; reload/API/PG assertion; TEST-V3-BC-078-FLOW.
- **AC-BC-078-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-078-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-080 — Nhận/tra cứu thông tin trạng thái giới hạn

Source: BC-080/R10/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không login; xác minh phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-080-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Nhận/tra cứu thông tin trạng thái giới hạn; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không login»; reload/API/PG assertion; TEST-V3-BC-080-FLOW.
- **AC-BC-080-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-080-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-106 — Tra tracking, POD và điểm quét cuối

Source: BC-106/R14/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ ticket/scope được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-106-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tra tracking, POD và điểm quét cuối; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Chỉ ticket/scope được phân công»; reload/API/PG assertion; TEST-V3-BC-106-FLOW.
- **AC-BC-106-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-106-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-126 — Xem lịch sử tracking/custody có chứng cứ

Source: BC-126/R17/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu được cấp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-126-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem lịch sử tracking/custody có chứng cứ; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Chỉ dữ liệu được cấp»; reload/API/PG assertion; TEST-V3-BC-126-FLOW.
- **AC-BC-126-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-126-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-143 — Tra cứu tất cả Shipment do mình tạo

Source: BC-143/R18/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Resource owner isolation; không cross-account; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-143-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Tra cứu tất cả Shipment do mình tạo; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Resource owner isolation»; reload/API/PG assertion; TEST-V3-BC-143-FLOW.
- **AC-BC-143-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-143-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-170 — Theo dõi giao chậm, vận đơn bất thường và tiến độ

Source: BC-170/R19/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng scope Merchant/branch; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-170-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi giao chậm, vận đơn bất thường và tiến độ; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Đúng scope Merchant/branch»; reload/API/PG assertion; TEST-V3-BC-170-FLOW.
- **AC-BC-170-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-170-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-186 — Theo dõi trạng thái kiện đã bàn giao cho hãng

Source: BC-186/R20/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ kho/đơn thuộc scope; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-186-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi trạng thái kiện đã bàn giao cho hãng; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Chỉ kho/đơn thuộc scope»; reload/API/PG assertion; TEST-V3-BC-186-FLOW.
- **AC-BC-186-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-186-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-194 — Theo dõi giao chậm, giao thất bại và thông báo bất thường

Source: BC-194/R21/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu cần cho CSKH; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-194-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi giao chậm, giao thất bại và thông báo bất thường; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Chỉ dữ liệu cần cho CSKH»; reload/API/PG assertion; TEST-V3-BC-194-FLOW.
- **AC-BC-194-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-194-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-293 — Xử lý yêu cầu giao chậm qua điểm quét cuối

Source: BC-293/R14/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả lập tracking event; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-293-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xử lý yêu cầu giao chậm qua điểm quét cuối; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không giả lập tracking event»; reload/API/PG assertion; TEST-V3-BC-293-FLOW.
- **AC-BC-293-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-293-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-315 — Đọc tracking events để kiểm toán vận chuyển

Source: BC-315/R17/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa trạng thái/attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-315-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc tracking events để kiểm toán vận chuyển; Then outcome lưu/đọc đúng source tracking event/scoped projection, giữ guard «Không sửa trạng thái/attempt»; reload/API/PG assertion; TEST-V3-BC-315-FLOW.
- **AC-BC-315-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-315-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-005-03 | State guard theo bảng `docs/domain/state-machines.md`; không cập nhật lịch sử tracking đã ghi. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |
| REQ-005-05 | Tracking công khai chỉ tối thiểu, có rate limit; merchant đọc toàn bộ đơn chỉ trong merchant scope. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |
| REQ-012-01 | Chỉ tạo notification sau source transaction commit; mỗi (sourceEventId, recipientId, purpose) chỉ một bản. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-02 | Notification không tự quyết định trạng thái vận đơn/tài chính; dữ liệu nhạy cảm được redact. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-03 | Người dùng chỉ đọc notification của mình; `markAsRead` chỉ cập nhật target thuộc account. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-04 | Dashboard filter theo tenant/role/warehouse/hub; định nghĩa KPI và thời điểm cập nhật được công bố. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-05 | RabbitMQ unavailable vẫn giữ outbox; retry bounded + failed job readable theo quyền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-TRACKING-PRIVACY

Owner target: L009; source BC-003;BC-013;BC-027;BC-080;BC-143;BC-170;BC-186;BC-194; latest Owner portal scope, no new BC ID. UI/channel: Public limited lookup; private detail/timeline.

Required behavior: Shipment status != tracking event != delivery attempt != custody event != financial event; current owners get permitted detail, public code alone redacts identities/full contact/address/COD. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-TRACKING-PRIVACY:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Direct code/UUID enumeration, token misuse, delayed/reordered duplicate events and revoked scope tests; trace sources; no event retroactively fakes POD. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-TRACKING-PRIVACY PLANNED_NOT_EXECUTED. Dependencies: L006.
