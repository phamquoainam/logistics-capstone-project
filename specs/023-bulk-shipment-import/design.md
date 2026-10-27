# Design 023 — bulk-shipment-import

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Validate file/preview, row dedup, commit draft qua canonical booking.

FE: Merchant upload/preview/correct/retry/status/error download; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Orders import job/row refs; private temp file retention. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-029

REQ-V3-BC-029: Nhập đơn hàng loạt CSV/Excel. Backend application boundary: orders xử lý import job/row/source-to-Shipment reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhập đơn hàng loạt CSV/Excel; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource import job/row/source-to-Shipment reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Nhập đơn hàng loạt CSV/Excel; action «Nhập đơn hàng loạt CSV/Excel» có success/deny/recovery states. Authorization: Kiểm tra bản ghi trùng và tính hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-029-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-174

REQ-V3-BC-174: Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi. Backend application boundary: orders xử lý import job/row/source-to-Shipment reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource import job/row/source-to-Shipment reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi; action «Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi» có success/deny/recovery states. Authorization: Idempotency, validation, partial failure report; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-174-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-BULK

Trace REQ-AB-BULK. Upload/template/tabular preview/correction/history/progress/results; BE/application: Versioned CSV/Excel template and schema; reviewed file/row/size/resource limits; safe parsing no macro/formula execution, signature/MIME/security policy, tenant from current context; preview no shipment/charge; row-specific validation/correction/results. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Proposed per-row atomic DRAFT creation after explicit commit, visible partial outcomes (DEC22 review required); stable job+row revision/source fingerprint/business ref; replay/reorder/retry no duplicate; export formula injection protected; no auto confirm/charge; current guard và failure/replay/recovery, no mock-only success.
