# Spec 023 — bulk-shipment-import

Alias lịch sử logical `L023`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Import CSV/Excel với row outcomes. Validate file/preview, row dedup, commit draft qua canonical booking.

Workflow: Mixed valid/invalid→preview→commit theo policy→sửa/retry; không auto-confirm/charge. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L007. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-22. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-029 — Nhập đơn hàng loạt CSV/Excel

Source: BC-029/R03/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Kiểm tra bản ghi trùng và tính hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-029-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Nhập đơn hàng loạt CSV/Excel; Then outcome lưu/đọc đúng import job/row/source-to-Shipment reference, giữ guard «Kiểm tra bản ghi trùng và tính hợp lệ»; reload/API/PG assertion; TEST-V3-BC-029-FLOW.
- **AC-BC-029-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-029-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-174 — Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi

Source: BC-174/R03/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Idempotency, validation, partial failure report; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-174-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi; Then outcome lưu/đọc đúng import job/row/source-to-Shipment reference, giữ guard «Idempotency, validation, partial failure report»; reload/API/PG assertion; TEST-V3-BC-174-FLOW.
- **AC-BC-174-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-174-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-014-01 | Preview không tạo shipment hoặc thu cước; import xác nhận chỉ tạo draft hợp lệ. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import/spec.md) |
| REQ-014-02 | Mỗi row có số dòng, mã tham chiếu, lỗi cấu trúc/ngữ nghĩa rõ; chủ sở hữu lấy từ phiên, không từ Excel. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import/spec.md) |
| REQ-014-03 | File/header/size/count limits config; không execute formula/macro; không log PII từ Excel. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import/spec.md) |
| REQ-014-04 | Import batch có job ID + row fingerprint/merchant reference; replay không tạo trùng; semantics partial success vs atomic import phải chốt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import/spec.md) |
| REQ-014-05 | Quy trình xác nhận phí/WMS vẫn qua feature 005, không auto-confirm đơn import. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/014-bulk-shipment-import/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-BULK

Owner target: L023; source BC-029;BC-160;BC-174; latest Owner portal scope, no new BC ID. UI/channel: Upload/template/tabular preview/correction/history/progress/results.

Required behavior: Versioned CSV/Excel template and schema; reviewed file/row/size/resource limits; safe parsing no macro/formula execution, signature/MIME/security policy, tenant from current context; preview no shipment/charge; row-specific validation/correction/results. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-BULK:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Proposed per-row atomic DRAFT creation after explicit commit, visible partial outcomes (DEC22 review required); stable job+row revision/source fingerprint/business ref; replay/reorder/retry no duplicate; export formula injection protected; no auto confirm/charge. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-BULK PLANNED_NOT_EXECUTED. Dependencies: L003;L004;L007.
