# Design 014 — delivery-dispatch-and-route

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Delivery pool/assignment; routing proposal/confirm giữ completed stops.

FE: Dispatcher pool/route/reassign; Team Leader đề xuất; Courier nhận tải; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Dispatch assignment duy nhất; routing route version riêng. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-048

REQ-V3-BC-048: Xem delivery pool và phân công giao. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem delivery pool và phân công giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem delivery pool và phân công giao; action «Xem delivery pool và phân công giao» có success/deny/recovery states. Authorization: Kiện đủ điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-048-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-050

REQ-V3-BC-050: Điều phối giao lại. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều phối giao lại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều phối giao lại; action «Điều phối giao lại» có success/deny/recovery states. Authorization: Delivery attempt và chính sách; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-050-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-054

REQ-V3-BC-054: Gom điểm/tối ưu tuyến. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gom điểm/tối ưu tuyến; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Gom điểm/tối ưu tuyến; action «Gom điểm/tối ưu tuyến» có success/deny/recovery states. Authorization: Chưa yêu cầu routing optimization chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-054-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-071

REQ-V3-BC-071: Xem và nhận kiện giao theo assignment. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem và nhận kiện giao theo assignment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem và nhận kiện giao theo assignment; action «Xem và nhận kiện giao theo assignment» có success/deny/recovery states. Authorization: Chỉ nhiệm vụ cá nhân; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-071-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-072

REQ-V3-BC-072: Ghi OUT_FOR_DELIVERY. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi OUT_FOR_DELIVERY; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi OUT_FOR_DELIVERY; action «Ghi OUT_FOR_DELIVERY» có success/deny/recovery states. Authorization: Chỉ khi kiện đã được bàn giao hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-072-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-079

REQ-V3-BC-079: Tuyến giao hàng tối ưu/GPS real-time. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tuyến giao hàng tối ưu/GPS real-time; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tuyến giao hàng tối ưu/GPS real-time; action «Tuyến giao hàng tối ưu/GPS real-time» có success/deny/recovery states. Authorization: Không bắt buộc với MVP; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-079-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-119

REQ-V3-BC-119: Giám sát pickup/courier thuộc Station. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát pickup/courier thuộc Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát pickup/courier thuộc Station; action «Giám sát pickup/courier thuộc Station» có success/deny/recovery states. Authorization: Không xác nhận PICKED_UP thay courier; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-119-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-120

REQ-V3-BC-120: Giám sát giao thất bại và tồn phát. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát giao thất bại và tồn phát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát giao thất bại và tồn phát; action «Giám sát giao thất bại và tồn phát» có success/deny/recovery states. Authorization: Không tạo POD giả; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-120-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-171

REQ-V3-BC-171: Yêu cầu xử lý giao thất bại và giao lại. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Yêu cầu xử lý giao thất bại và giao lại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Yêu cầu xử lý giao thất bại và giao lại; action «Yêu cầu xử lý giao thất bại và giao lại» có success/deny/recovery states. Authorization: Không tạo DELIVERED/POD thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-171-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-177

REQ-V3-BC-177: Gửi yêu cầu giao lại qua workflow theo quyền. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gửi yêu cầu giao lại qua workflow theo quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Gửi yêu cầu giao lại qua workflow theo quyền; action «Gửi yêu cầu giao lại qua workflow theo quyền» có success/deny/recovery states. Authorization: Không tự gán Courier hoặc POD; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-177-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-197

REQ-V3-BC-197: Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát; action «Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát» có success/deny/recovery states. Authorization: Không trực tiếp ghi trạng thái giao; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-197-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-208

REQ-V3-BC-208: Phân công Delivery Courier theo khu vực, tuyến và năng lực. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân công Delivery Courier theo khu vực, tuyến và năng lực; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân công Delivery Courier theo khu vực, tuyến và năng lực; action «Phân công Delivery Courier theo khu vực, tuyến và năng lực» có success/deny/recovery states. Authorization: Kiện sẵn sàng giao, custody/scan và assignment hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-208-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-222

REQ-V3-BC-222: Quét barcode xác nhận nhận tải kiện từ bưu cục. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quét barcode xác nhận nhận tải kiện từ bưu cục; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quét barcode xác nhận nhận tải kiện từ bưu cục; action «Quét barcode xác nhận nhận tải kiện từ bưu cục» có success/deny/recovery states. Authorization: Handover hai phía, đúng assignment, không nhận trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-222-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-223

REQ-V3-BC-223: Xem tuyến giao, bản đồ và điểm dừng. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem tuyến giao, bản đồ và điểm dừng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem tuyến giao, bản đồ và điểm dừng; action «Xem tuyến giao, bản đồ và điểm dừng» có success/deny/recovery states. Authorization: Chỉ nhiệm vụ được giao; địa chỉ và PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-223-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-224

REQ-V3-BC-224: Liên hệ Recipient theo chính sách bảo vệ số điện thoại. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Liên hệ Recipient theo chính sách bảo vệ số điện thoại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Liên hệ Recipient theo chính sách bảo vệ số điện thoại; action «Liên hệ Recipient theo chính sách bảo vệ số điện thoại» có success/deny/recovery states. Authorization: Chỉ khi assignment hợp lệ; bảo vệ thông tin liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-224-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-226

REQ-V3-BC-226: Thực hiện giao lại theo assignment mới. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thực hiện giao lại theo assignment mới; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thực hiện giao lại theo assignment mới; action «Thực hiện giao lại theo assignment mới» có success/deny/recovery states. Authorization: Delivery attempt riêng; không tái sử dụng assignment hết hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-226-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-245

REQ-V3-BC-245: Xem nhiệm vụ Delivery Courier toàn đội. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem nhiệm vụ Delivery Courier toàn đội; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem nhiệm vụ Delivery Courier toàn đội; action «Xem nhiệm vụ Delivery Courier toàn đội» có success/deny/recovery states. Authorization: Không tự có quyền giao thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-245-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-248

REQ-V3-BC-248: Đề xuất tái phân công delivery. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đề xuất tái phân công delivery; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đề xuất tái phân công delivery; action «Đề xuất tái phân công delivery» có success/deny/recovery states. Authorization: Không tự cấp assignment rồi tự ghi POD; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-248-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-274

REQ-V3-BC-274: Theo dõi tỷ lệ giao thành công toàn mạng. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi tỷ lệ giao thành công toàn mạng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi tỷ lệ giao thành công toàn mạng; action «Theo dõi tỷ lệ giao thành công toàn mạng» có success/deny/recovery states. Authorization: KPI từ attempts/tracking, không sửa POD; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-274-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-278

REQ-V3-BC-278: Điều phối chuyển vùng phát trong ngoại lệ. Backend application boundary: dispatch xử lý delivery request/assignment/route; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều phối chuyển vùng phát trong ngoại lệ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource delivery request/assignment/route. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều phối chuyển vùng phát trong ngoại lệ; action «Điều phối chuyển vùng phát trong ngoại lệ» có success/deny/recovery states. Authorization: Tôn trọng state, active assignment và custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-278-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
