# Design 018 — billing-ledger-fees-invoices

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Minimal balanced posting/fee/refund API trước L007; full invoice/debt/adjustment.

FE: Sender receipt; Merchant Finance fee/debt; Finance ledger; Pricing adjustment; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Finance wallet/payment/ledger append-only; tổng Nợ=Có, balance không âm. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-088

REQ-V3-BC-088: Tra cứu phí và phụ phí theo Shipment. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu phí và phụ phí theo Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tra cứu phí và phụ phí theo Shipment; action «Tra cứu phí và phụ phí theo Shipment» có success/deny/recovery states. Authorization: Tôn trọng snapshot giá; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-088-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-101

REQ-V3-BC-101: Tính phí và công nợ phải thu/phải trả. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tính phí và công nợ phải thu/phải trả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tính phí và công nợ phải thu/phải trả; action «Tính phí và công nợ phải thu/phải trả» có success/deny/recovery states. Authorization: Snapshot giá + audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-101-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-102

REQ-V3-BC-102: Audit điều chỉnh tài chính. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Audit điều chỉnh tài chính; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Audit điều chỉnh tài chính; action «Audit điều chỉnh tài chính» có success/deny/recovery states. Authorization: Không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-102-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-103

REQ-V3-BC-103: Refund và xử lý khiếu nại tài chính. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Refund và xử lý khiếu nại tài chính; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Refund và xử lý khiếu nại tài chính; action «Refund và xử lý khiếu nại tài chính» có success/deny/recovery states. Authorization: Phải được phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-103-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-104

REQ-V3-BC-104: Tích hợp cổng thanh toán/ngân hàng. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tích hợp cổng thanh toán/ngân hàng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tích hợp cổng thanh toán/ngân hàng; action «Tích hợp cổng thanh toán/ngân hàng» có success/deny/recovery states. Authorization: Không yêu cầu thanh toán thật trong demo; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-104-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-116

REQ-V3-BC-116: Đề xuất bồi thường/hoàn tiền. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đề xuất bồi thường/hoàn tiền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đề xuất bồi thường/hoàn tiền; action «Đề xuất bồi thường/hoàn tiền» có success/deny/recovery states. Authorization: Không tự thực hiện settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-116-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-142

REQ-V3-BC-142: Thanh toán phí vận chuyển theo phương thức hỗ trợ. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thanh toán phí vận chuyển theo phương thức hỗ trợ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Thanh toán phí vận chuyển theo phương thức hỗ trợ; action «Thanh toán phí vận chuyển theo phương thức hỗ trợ» có success/deny/recovery states. Authorization: Receipt, price snapshot và chống thu trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-142-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-148

REQ-V3-BC-148: Xem/tải hóa đơn và biên nhận. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem/tải hóa đơn và biên nhận; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Xem/tải hóa đơn và biên nhận; action «Xem/tải hóa đơn và biên nhận» có success/deny/recovery states. Authorization: Hợp lệ với hình thức thanh toán, không rò dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-148-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-151

REQ-V3-BC-151: Ghi nhận phí/thanh toán ký gửi tại quầy theo policy. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận phí/thanh toán ký gửi tại quầy theo policy; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận phí/thanh toán ký gửi tại quầy theo policy; action «Ghi nhận phí/thanh toán ký gửi tại quầy theo policy» có success/deny/recovery states. Authorization: Không tự xác nhận thanh toán thiếu bằng chứng; sổ thu/receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-151-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-286

REQ-V3-BC-286: Quản lý công nợ phải thu và phải trả. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý công nợ phải thu và phải trả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý công nợ phải thu và phải trả; action «Quản lý công nợ phải thu và phải trả» có success/deny/recovery states. Authorization: Ledger entries append-only; tính toán có căn cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-286-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-288

REQ-V3-BC-288: Thực hiện hoàn tiền đã được phê duyệt. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thực hiện hoàn tiền đã được phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thực hiện hoàn tiền đã được phê duyệt; action «Thực hiện hoàn tiền đã được phê duyệt» có success/deny/recovery states. Authorization: Idempotency, kiểm soát hai người khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-288-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-289

REQ-V3-BC-289: Phát hiện và từ chối thanh toán hoặc refund trùng. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phát hiện và từ chối thanh toán hoặc refund trùng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phát hiện và từ chối thanh toán hoặc refund trùng; action «Phát hiện và từ chối thanh toán hoặc refund trùng» có success/deny/recovery states. Authorization: Idempotency key, unique reference và reconciliation; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-289-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-303

REQ-V3-BC-303: Theo dõi thanh toán bồi thường/refund đã phê duyệt. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi thanh toán bồi thường/refund đã phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi thanh toán bồi thường/refund đã phê duyệt; action «Theo dõi thanh toán bồi thường/refund đã phê duyệt» có success/deny/recovery states. Authorization: Không giả lập payment confirmation; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-303-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-316

REQ-V3-BC-316: Đọc ledger và lịch sử điều chỉnh tài chính. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc ledger và lịch sử điều chỉnh tài chính; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc ledger và lịch sử điều chỉnh tài chính; action «Đọc ledger và lịch sử điều chỉnh tài chính» có success/deny/recovery states. Authorization: PII redaction và scope; read-only; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-316-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-327

REQ-V3-BC-327: Cung cấp dữ liệu cước cho hóa đơn và đối soát. Backend application boundary: finance xử lý ledger/payment/charge/invoice/receipt; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cung cấp dữ liệu cước cho hóa đơn và đối soát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource ledger/payment/charge/invoice/receipt. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Cung cấp dữ liệu cước cho hóa đơn và đối soát; action «Cung cấp dữ liệu cước cho hóa đơn và đối soát» có success/deny/recovery states. Authorization: Không trực tiếp sửa ledger/số dư; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-327-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-PAYMENTS-RECEIPTS

Trace REQ-AB-PAYMENTS-RECEIPTS. Payments/receipts/invoices/adjustment/refund; BE/application: Supported freight payer/options, exact charge/surcharge/declared fee evidence; receipt vs applicable invoice distinction; authorized dispute/refund approval/reference. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert No real bank/payment execution implied; callback verified/unknown handling, duplicate refund/charge denied; personal/tenant invoice access scoped; invoice policy before issue; current guard và failure/replay/recovery, no mock-only success.
