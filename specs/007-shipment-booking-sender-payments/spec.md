# Spec 007 — shipment-booking-sender-payments

Alias lịch sử logical `L007`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Booking Sender/Merchant và thu cước đúng. Workflow validate quote, finance charge, warehouse reserve nếu WMS, orders confirm cùng transaction.

Workflow: Confirm→một charge/hold/snapshot; thiếu tiền/tồn rollback; timeout tra cứu cùng key. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L005, L006, L018. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-04; DEC-09; DEC-10; DEC-11; DEC-12. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-006 — Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone

Source: BC-006/R01/M05, DEFER; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không cấp API public tạo Shipment; ký gửi tại quầy do Station Operator thực hiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-006-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-006-DENY.
- **AC-BC-006-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-006-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

### REQ-V3-BC-011 — Tạo, xem, xác nhận Shipment

Source: BC-011/R02/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Membership, valid state, idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-011-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo, xem, xác nhận Shipment; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Membership, valid state, idempotency»; reload/API/PG assertion; TEST-V3-BC-011-FLOW.
- **AC-BC-011-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-011-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-012 — Yêu cầu hủy/sửa Shipment

Source: BC-012/R02/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ trạng thái cho phép; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-012-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Yêu cầu hủy/sửa Shipment; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Chỉ trạng thái cho phép»; reload/API/PG assertion; TEST-V3-BC-012-FLOW.
- **AC-BC-012-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-012-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-024 — Tạo/xác nhận Shipment

Source: BC-024/R03/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Idempotency, đúng org; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-024-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo/xác nhận Shipment; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Idempotency, đúng org»; reload/API/PG assertion; TEST-V3-BC-024-FLOW.
- **AC-BC-024-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-024-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-025 — Xem và yêu cầu hủy/giao lại

Source: BC-025/R03/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hủy/sửa phải đúng quyền và state; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-025-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem và yêu cầu hủy/giao lại; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Hủy/sửa phải đúng quyền và state»; reload/API/PG assertion; TEST-V3-BC-025-FLOW.
- **AC-BC-025-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-025-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-139 — Tạo và xác nhận Shipment gửi hàng cá nhân

Source: BC-139/R18/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Shipment thuộc account; valid state, chống trùng/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-139-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Tạo và xác nhận Shipment gửi hàng cá nhân; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Shipment thuộc account»; reload/API/PG assertion; TEST-V3-BC-139-FLOW.
- **AC-BC-139-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-139-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-144 — Hủy yêu cầu gửi khi trạng thái cho phép

Source: BC-144/R18/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đã pickup phải chuyển workflow thu hồi/hoàn; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-144-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Hủy yêu cầu gửi khi trạng thái cho phép; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Đã pickup phải chuyển workflow thu hồi/hoàn»; reload/API/PG assertion; TEST-V3-BC-144-FLOW.
- **AC-BC-144-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-144-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-147 — Xem lịch sử vận đơn đã gửi

Source: BC-147/R18/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Giới hạn theo account và quyền riêng tư; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-147-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Xem lịch sử vận đơn đã gửi; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Giới hạn theo account và quyền riêng tư»; reload/API/PG assertion; TEST-V3-BC-147-FLOW.
- **AC-BC-147-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-147-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-160 — Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant

Source: BC-160/R02/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có kiểm tra dữ liệu và idempotency, không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-160-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Có kiểm tra dữ liệu và idempotency, không sửa lịch sử»; reload/API/PG assertion; TEST-V3-BC-160-FLOW.
- **AC-BC-160-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-160-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-169 — Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách

Source: BC-169/R19/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: State guard; không ghi đè Shipment đã chốt trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-169-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «State guard»; reload/API/PG assertion; TEST-V3-BC-169-FLOW.
- **AC-BC-169-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-169-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-193 — Tra cứu Order/Shipment của Merchant để hỗ trợ người mua

Source: BC-193/R21/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xem Shipment Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-193-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tra cứu Order/Shipment của Merchant để hỗ trợ người mua; Then outcome lưu/đọc đúng owned Shipment/request; finance charge reference, giữ guard «Không xem Shipment Merchant khác»; reload/API/PG assertion; TEST-V3-BC-193-FLOW.
- **AC-BC-193-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-193-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-243 — Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier

Source: BC-243/R23/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Negative authorization cho commercial state/POD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-243-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-243-DENY.
- **AC-BC-243-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-243-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-005-01 | DRAFT tạo được từ input hợp lệ và merchantId trusted; mã shipment ref unique theo owner; bắt buộc Idempotency-Key. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |
| REQ-005-02 | Xác nhận dùng quote hợp lệ, address confirmed; WMS reserve nếu có, Finance fee debit, Order confirm + snapshot trong cùng DB transaction. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |
| REQ-005-04 | Cancel chỉ trước mốc picked-up trực tiếp hoặc WMS picking; release unused reserved; refund theo policy bằng nghiệp vụ riêng, không xóa ledger. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/005-shipment-lifecycle/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-BOOKING-FORM

Owner target: L007; source BC-011;BC-012;BC-024;BC-139;BC-144;BC-160; latest Owner portal scope, no new BC ID. UI/channel: Single create/draft/confirm/reset/view.

Required behavior: Sender origins default/alternate eligible pickup or supported Station drop-off; recipient details/schedule; goods/parcels/service/terms; save draft/validate/confirm/reset/unsaved guard; no physical pickup on request acceptance. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-BOOKING-FORM:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Real browser personal WITHOUT Merchant and Merchant branch; confirm snapshots+single charge/reservation+state atomic; invalid/missing funds rollback; lost response status recovery; cancel cutoff enforced. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-BOOKING-FORM PLANNED_NOT_EXECUTED. Dependencies: L003;L004;L005;L006;L018.


## REQ-AB-SHIPMENT-MANAGEMENT

Owner target: L007; source BC-011;BC-012;BC-139;BC-147;BC-160;BC-169;BC-193; latest Owner portal scope, no new BC ID. UI/channel: My/Merchant shipments list/detail.

Required behavior: Scoped identifier/date/status/service/facility/payment/COD filters where permitted, sort/paging/search, pending queue/cancel/change approvals; timeline via009, labels008, export028/import023. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-SHIPMENT-MANAGEMENT:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Filter-before-paging isolation including counts; locked fields immutable, stale updates conflict; unsupported actions unavailable, history preserved. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-SHIPMENT-MANAGEMENT PLANNED_NOT_EXECUTED. Dependencies: L006;L009;L008;L023;L028.
