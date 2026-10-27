# Design 007 — shipment-booking-sender-payments

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Workflow validate quote, finance charge, warehouse reserve nếu WMS, orders confirm cùng transaction.

FE: Sender/Merchant booking/cancel/payment pending/receipt/retry; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Booking ở orders, money ở finance; không body balance. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-006

REQ-V3-BC-006: Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone; action «Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone» có success/deny/recovery states. Authorization: Không cấp API public tạo Shipment; ký gửi tại quầy do Station Operator thực hiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-006-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-011

REQ-V3-BC-011: Tạo, xem, xác nhận Shipment. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo, xem, xác nhận Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo, xem, xác nhận Shipment; action «Tạo, xem, xác nhận Shipment» có success/deny/recovery states. Authorization: Membership, valid state, idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-011-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-012

REQ-V3-BC-012: Yêu cầu hủy/sửa Shipment. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Yêu cầu hủy/sửa Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Yêu cầu hủy/sửa Shipment; action «Yêu cầu hủy/sửa Shipment» có success/deny/recovery states. Authorization: Chỉ trạng thái cho phép; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-012-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-024

REQ-V3-BC-024: Tạo/xác nhận Shipment. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo/xác nhận Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo/xác nhận Shipment; action «Tạo/xác nhận Shipment» có success/deny/recovery states. Authorization: Idempotency, đúng org; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-024-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-025

REQ-V3-BC-025: Xem và yêu cầu hủy/giao lại. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem và yêu cầu hủy/giao lại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem và yêu cầu hủy/giao lại; action «Xem và yêu cầu hủy/giao lại» có success/deny/recovery states. Authorization: Hủy/sửa phải đúng quyền và state; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-025-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-139

REQ-V3-BC-139: Tạo và xác nhận Shipment gửi hàng cá nhân. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo và xác nhận Shipment gửi hàng cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Tạo và xác nhận Shipment gửi hàng cá nhân; action «Tạo và xác nhận Shipment gửi hàng cá nhân» có success/deny/recovery states. Authorization: Shipment thuộc account; valid state, chống trùng/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-139-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-144

REQ-V3-BC-144: Hủy yêu cầu gửi khi trạng thái cho phép. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Hủy yêu cầu gửi khi trạng thái cho phép; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Hủy yêu cầu gửi khi trạng thái cho phép; action «Hủy yêu cầu gửi khi trạng thái cho phép» có success/deny/recovery states. Authorization: Đã pickup phải chuyển workflow thu hồi/hoàn; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-144-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-147

REQ-V3-BC-147: Xem lịch sử vận đơn đã gửi. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch sử vận đơn đã gửi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Xem lịch sử vận đơn đã gửi; action «Xem lịch sử vận đơn đã gửi» có success/deny/recovery states. Authorization: Giới hạn theo account và quyền riêng tư; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-147-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-160

REQ-V3-BC-160: Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant; action «Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant» có success/deny/recovery states. Authorization: Có kiểm tra dữ liệu và idempotency, không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-160-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-169

REQ-V3-BC-169: Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách; action «Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách» có success/deny/recovery states. Authorization: State guard; không ghi đè Shipment đã chốt trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-169-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-193

REQ-V3-BC-193: Tra cứu Order/Shipment của Merchant để hỗ trợ người mua. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu Order/Shipment của Merchant để hỗ trợ người mua; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tra cứu Order/Shipment của Merchant để hỗ trợ người mua; action «Tra cứu Order/Shipment của Merchant để hỗ trợ người mua» có success/deny/recovery states. Authorization: Không xem Shipment Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-193-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-243

REQ-V3-BC-243: Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier. Backend application boundary: orders xử lý owned Shipment/request; finance charge reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource owned Shipment/request; finance charge reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier; action «Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier» có success/deny/recovery states. Authorization: Negative authorization cho commercial state/POD; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-243-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-BOOKING-FORM

Trace REQ-AB-BOOKING-FORM. Single create/draft/confirm/reset/view; BE/application: Sender origins default/alternate eligible pickup or supported Station drop-off; recipient details/schedule; goods/parcels/service/terms; save draft/validate/confirm/reset/unsaved guard; no physical pickup on request acceptance. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Real browser personal WITHOUT Merchant and Merchant branch; confirm snapshots+single charge/reservation+state atomic; invalid/missing funds rollback; lost response status recovery; cancel cutoff enforced; current guard và failure/replay/recovery, no mock-only success.


## DES-AB-SHIPMENT-MANAGEMENT

Trace REQ-AB-SHIPMENT-MANAGEMENT. My/Merchant shipments list/detail; BE/application: Scoped identifier/date/status/service/facility/payment/COD filters where permitted, sort/paging/search, pending queue/cancel/change approvals; timeline via009, labels008, export028/import023. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Filter-before-paging isolation including counts; locked fields immutable, stale updates conflict; unsupported actions unavailable, history preserved; current guard và failure/replay/recovery, no mock-only success.
