# Design 019 — cod-reconciliation-merchant-settlement

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Eligibility recheck/unique closed membership/cutoff/dispute/payout evidence.

FE: Finance preview/close/approve; Merchant đối chiếu/chứng từ/dispute; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Closed snapshot bất biến; payout/adjustment mới tham chiếu gốc. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-015

REQ-V3-BC-015: Xem phí, bảng kê và settlement. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem phí, bảng kê và settlement; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem phí, bảng kê và settlement; action «Xem phí, bảng kê và settlement» có success/deny/recovery states. Authorization: Chỉ thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-015-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-087

REQ-V3-BC-087: Xem bảng kê đối soát và thanh toán. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem bảng kê đối soát và thanh toán; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem bảng kê đối soát và thanh toán; action «Xem bảng kê đối soát và thanh toán» có success/deny/recovery states. Authorization: Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-087-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-091

REQ-V3-BC-091: Đối chiếu/ghi nhận xác nhận bảng kê. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu/ghi nhận xác nhận bảng kê; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Đối chiếu/ghi nhận xác nhận bảng kê; action «Đối chiếu/ghi nhận xác nhận bảng kê» có success/deny/recovery states. Authorization: Không tự quyết toán phía Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-091-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-099

REQ-V3-BC-099: Tạo bảng kê và kỳ thanh toán Merchant. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo bảng kê và kỳ thanh toán Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tạo bảng kê và kỳ thanh toán Merchant; action «Tạo bảng kê và kỳ thanh toán Merchant» có success/deny/recovery states. Authorization: Có quyền phê duyệt phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-099-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-100

REQ-V3-BC-100: Ghi nhận settlement đã thực hiện. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận settlement đã thực hiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận settlement đã thực hiện; action «Ghi nhận settlement đã thực hiện» có success/deny/recovery states. Authorization: Không giả định đã chuyển tiền khi chưa có bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-100-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-162

REQ-V3-BC-162: Xem lịch thanh toán, hóa đơn và chứng từ COD/cước. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch thanh toán, hóa đơn và chứng từ COD/cước; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem lịch thanh toán, hóa đơn và chứng từ COD/cước; action «Xem lịch thanh toán, hóa đơn và chứng từ COD/cước» có success/deny/recovery states. Authorization: Chỉ Merchant được sở hữu; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-162-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-188

REQ-V3-BC-188: Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả; action «Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả» có success/deny/recovery states. Authorization: Ledger/statement không được sửa âm thầm; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-188-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-190

REQ-V3-BC-190: Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ; action «Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ» có success/deny/recovery states. Authorization: Không tự xác nhận hãng đã trả; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-190-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-287

REQ-V3-BC-287: Xác định COD đủ điều kiện trả Merchant. Backend application boundary: finance xử lý statement/closed-period snapshot/payout evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác định COD đủ điều kiện trả Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource statement/closed-period snapshot/payout evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác định COD đủ điều kiện trả Merchant; action «Xác định COD đủ điều kiện trả Merchant» có success/deny/recovery states. Authorization: Theo hold, fees, disputes, policy, approvals; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-287-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-FINANCIAL-PORTAL

Trace REQ-AB-FINANCIAL-PORTAL. COD statements/payout schedules/discrepancy; BE/application: Expected/collected/held/remitted/reconciled/settled distinct; fees/receivables/payables/payout references, statement confirmation and dispute links; invoice source018. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Statement totals tie to immutable ledger/allocation/eligible closed snapshots; Merchant acknowledgment not carrier settlement; late data new adjustment; sandbox payout accurately labeled; current guard và failure/replay/recovery, no mock-only success.
