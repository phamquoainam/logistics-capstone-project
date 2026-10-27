# Design 017 — cod-collection-cashier-remittance

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Collection contract trước L015; remittance/allocation/shift/variance.

FE: Courier COD due/submit; Cashier count/confirm/close; Manager giám sát; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Finance liability/receipt/allocation/ledger. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-014

REQ-V3-BC-014: Xem COD thuộc Merchant. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem COD thuộc Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem COD thuộc Merchant; action «Xem COD thuộc Merchant» có success/deny/recovery states. Authorization: Chỉ đọc theo quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-014-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-028

REQ-V3-BC-028: Xem COD của đơn được giao phụ trách. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem COD của đơn được giao phụ trách; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem COD của đơn được giao phụ trách; action «Xem COD của đơn được giao phụ trách» có success/deny/recovery states. Authorization: Không sửa COD đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-028-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-040

REQ-V3-BC-040: Thực hiện điều chỉnh COD/settlement. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thực hiện điều chỉnh COD/settlement; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thực hiện điều chỉnh COD/settlement; action «Thực hiện điều chỉnh COD/settlement» có success/deny/recovery states. Authorization: Không mặc định được phép; cần workflow tài chính riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-040-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-046

REQ-V3-BC-046: Sửa/duyệt COD. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Sửa/duyệt COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Sửa/duyệt COD; action «Sửa/duyệt COD» có success/deny/recovery states. Authorization: Ngoài quyền Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R05, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-046-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-075

REQ-V3-BC-075: Ghi nhận khoản COD đã thu. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận khoản COD đã thu; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận khoản COD đã thu; action «Ghi nhận khoản COD đã thu» có success/deny/recovery states. Authorization: Đúng số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-075-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-076

REQ-V3-BC-076: Yêu cầu nộp COD cho quỹ. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Yêu cầu nộp COD cho quỹ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Yêu cầu nộp COD cho quỹ; action «Yêu cầu nộp COD cho quỹ» có success/deny/recovery states. Authorization: Không tự xác nhận quỹ đã nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-076-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-082

REQ-V3-BC-082: Thanh toán COD đã xác nhận. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thanh toán COD đã xác nhận; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Verified Recipient channel — Thanh toán COD đã xác nhận; action «Thanh toán COD đã xác nhận» có success/deny/recovery states. Authorization: Không thay đổi cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R10, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-082-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-086

REQ-V3-BC-086: Xem COD dự kiến/đã thu/đang giữ/đã trả. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem COD dự kiến/đã thu/đang giữ/đã trả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem COD dự kiến/đã thu/đang giữ/đã trả; action «Xem COD dự kiến/đã thu/đang giữ/đã trả» có success/deny/recovery states. Authorization: Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-086-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-092

REQ-V3-BC-092: Tiếp nhận tiền mặt COD Courier nộp. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận tiền mặt COD Courier nộp; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận tiền mặt COD Courier nộp; action «Tiếp nhận tiền mặt COD Courier nộp» có success/deny/recovery states. Authorization: Hai phía, amount và timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-092-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-093

REQ-V3-BC-093: Xác nhận nộp và ghi thiếu/thừa. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận nộp và ghi thiếu/thừa; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận nộp và ghi thiếu/thừa; action «Xác nhận nộp và ghi thiếu/thừa» có success/deny/recovery states. Authorization: Không âm thầm sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-093-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-094

REQ-V3-BC-094: Quản lý đối chiếu nộp COD theo ca. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý đối chiếu nộp COD theo ca; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý đối chiếu nộp COD theo ca; action «Quản lý đối chiếu nộp COD theo ca» có success/deny/recovery states. Authorization: Không tự settle Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-094-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-095

REQ-V3-BC-095: Ghi audit tiền nộp và điều chỉnh. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi audit tiền nộp và điều chỉnh; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi audit tiền nộp và điều chỉnh; action «Ghi audit tiền nộp và điều chỉnh» có success/deny/recovery states. Authorization: Không xóa receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-095-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-096

REQ-V3-BC-096: Kiểm kê két quỹ/đóng ca nâng cao. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm kê két quỹ/đóng ca nâng cao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm kê két quỹ/đóng ca nâng cao; action «Kiểm kê két quỹ/đóng ca nâng cao» có success/deny/recovery states. Authorization: Có đối chiếu kế toán; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-096-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-097

REQ-V3-BC-097: Đối soát Courier thu và nộp COD. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối soát Courier thu và nộp COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối soát Courier thu và nộp COD; action «Đối soát Courier thu và nộp COD» có success/deny/recovery states. Authorization: Phải khớp custody tiền và shipment; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-097-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-098

REQ-V3-BC-098: Phát hiện COD thiếu/thừa/trùng. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phát hiện COD thiếu/thừa/trùng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phát hiện COD thiếu/thừa/trùng; action «Phát hiện COD thiếu/thừa/trùng» có success/deny/recovery states. Authorization: Ledger bất biến/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-098-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-121

REQ-V3-BC-121: Giám sát bàn giao COD tại Station. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát bàn giao COD tại Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát bàn giao COD tại Station; action «Giám sát bàn giao COD tại Station» có success/deny/recovery states. Authorization: Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-121-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-127

REQ-V3-BC-127: Đọc log đối soát COD. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc log đối soát COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc log đối soát COD; action «Đọc log đối soát COD» có success/deny/recovery states. Authorization: Không sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-127-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-161

REQ-V3-BC-161: Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ; action «Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ» có success/deny/recovery states. Authorization: Chỉ đọc tài chính thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-161-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-187

REQ-V3-BC-187: Xem COD dự kiến, đã thu, đang giữ và đã thanh toán. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem COD dự kiến, đã thu, đang giữ và đã thanh toán; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem COD dự kiến, đã thu, đang giữ và đã thanh toán; action «Xem COD dự kiến, đã thu, đang giữ và đã thanh toán» có success/deny/recovery states. Authorization: Không sửa dữ liệu thu COD của Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-187-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-225

REQ-V3-BC-225: Xác nhận số COD cần thu và phương thức thanh toán. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận số COD cần thu và phương thức thanh toán; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận số COD cần thu và phương thức thanh toán; action «Xác nhận số COD cần thu và phương thức thanh toán» có success/deny/recovery states. Authorization: Chỉ đọc giá trị COD đã chốt; không tự đổi tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-225-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-233

REQ-V3-BC-233: Giám sát Courier nộp COD tại Station. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát Courier nộp COD tại Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát Courier nộp COD tại Station; action «Giám sát Courier nộp COD tại Station» có success/deny/recovery states. Authorization: Cashier xác nhận tiền; manager không sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-233-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-280

REQ-V3-BC-280: Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật; action «Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật» có success/deny/recovery states. Authorization: Negative permission tests, separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-280-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-281

REQ-V3-BC-281: Kiểm đếm COD thực nhận theo ca và từng Courier. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm đếm COD thực nhận theo ca và từng Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm đếm COD thực nhận theo ca và từng Courier; action «Kiểm đếm COD thực nhận theo ca và từng Courier» có success/deny/recovery states. Authorization: Xác nhận song phương, denomination/count và số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-281-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-282

REQ-V3-BC-282: Đóng ca và đối chiếu quỹ COD thực tế. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đóng ca và đối chiếu quỹ COD thực tế; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đóng ca và đối chiếu quỹ COD thực tế; action «Đóng ca và đối chiếu quỹ COD thực tế» có success/deny/recovery states. Authorization: Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-282-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-283

REQ-V3-BC-283: Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát; action «Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát» có success/deny/recovery states. Authorization: Không tự ghi RECONCILED hay SETTLED; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-283-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-284

REQ-V3-BC-284: Ràng buộc điểm thu quỹ với Station/ca. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ràng buộc điểm thu quỹ với Station/ca; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ràng buộc điểm thu quỹ với Station/ca; action «Ràng buộc điểm thu quỹ với Station/ca» có success/deny/recovery states. Authorization: Chỉ Station được giao, actor và thời gian có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R12, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-284-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-285

REQ-V3-BC-285: Đối chiếu Delivered Shipment với COD thu thực tế. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu Delivered Shipment với COD thu thực tế; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối chiếu Delivered Shipment với COD thu thực tế; action «Đối chiếu Delivered Shipment với COD thu thực tế» có success/deny/recovery states. Authorization: Không suy luận COD từ DELIVERED; phát hiện ngoại lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-285-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-295

REQ-V3-BC-295: Tiếp nhận tra soát COD và chuyển Finance. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận tra soát COD và chuyển Finance; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận tra soát COD và chuyển Finance; action «Tiếp nhận tra soát COD và chuyển Finance» có success/deny/recovery states. Authorization: Không tự sửa cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-295-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-317

REQ-V3-BC-317: Đọc chuỗi thu-nộp-đối soát COD. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc chuỗi thu-nộp-đối soát COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc chuỗi thu-nộp-đối soát COD; action «Đọc chuỗi thu-nộp-đối soát COD» có success/deny/recovery states. Authorization: Không ghi remittance; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-317-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-354

REQ-V3-BC-354: Không được giả xác nhận thu/đối soát/thanh toán COD. Backend application boundary: finance xử lý COD liability/collection/remittance; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Không được giả xác nhận thu/đối soát/thanh toán COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: finance; resource COD liability/collection/remittance. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Không được giả xác nhận thu/đối soát/thanh toán COD; action «Không được giả xác nhận thu/đối soát/thanh toán COD» có success/deny/recovery states. Authorization: Negative permission tests; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-354-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-RECIPIENT-COD

Trace REQ-AB-RECIPIENT-COD. Verified collection receipt/channel; BE/application: Recipient pays locked COD via supported approved/simulated method; receipt ties amount/Shipment/attempt/Courier and collection proof, not remittance/settlement. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert COD immutable to recipient; duplicate collection/payment callback once; no DELIVERED→COLLECTED inference, no real money claim; current guard và failure/replay/recovery, no mock-only success.
