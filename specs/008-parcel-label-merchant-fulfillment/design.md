# Design 008 — parcel-label-merchant-fulfillment

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Warehouse stock/reservation/pick-pack; orders Parcel/label qua API.

FE: Merchant Warehouse count/measure/label/variance/handover list; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Stock/movement/reservation ở warehouse; Parcel identity ở orders. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-026

REQ-V3-BC-026: Tạo nhãn và danh sách bàn giao. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo nhãn và danh sách bàn giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo nhãn và danh sách bàn giao; action «Tạo nhãn và danh sách bàn giao» có success/deny/recovery states. Authorization: Barcode liên kết kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-026-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-064

REQ-V3-BC-064: Kiểm tra barcode/cân đo kiện. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra barcode/cân đo kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra barcode/cân đo kiện; action «Kiểm tra barcode/cân đo kiện» có success/deny/recovery states. Authorization: Ghi nhận sai lệch có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-064-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-140

REQ-V3-BC-140: Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi; action «Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi» có success/deny/recovery states. Authorization: Parcel khác Shipment, thông tin hàng hóa hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-140-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-150

REQ-V3-BC-150: Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in; action «Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in» có success/deny/recovery states. Authorization: Tạo Parcel, timestamp, bằng chứng nhận kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-150-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-176

REQ-V3-BC-176: Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier; action «Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier» có success/deny/recovery states. Authorization: Quét/đối chiếu kiện; không tự tạo pickup evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-176-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-179

REQ-V3-BC-179: Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện. Backend application boundary: warehouse xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: warehouse; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện; action «Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện» có success/deny/recovery states. Authorization: Chỉ đơn thuộc kho/nhân sự được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-179-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-180

REQ-V3-BC-180: Lấy hàng, kiểm đếm và ghi số lượng thực tế. Backend application boundary: warehouse xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Lấy hàng, kiểm đếm và ghi số lượng thực tế; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: warehouse; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Lấy hàng, kiểm đếm và ghi số lượng thực tế; action «Lấy hàng, kiểm đếm và ghi số lượng thực tế» có success/deny/recovery states. Authorization: Order, Shipment, Parcel tách biệt; có sai lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-180-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-181

REQ-V3-BC-181: Đóng gói và cân đo kiện, ghi kích thước thực tế. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đóng gói và cân đo kiện, ghi kích thước thực tế; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Đóng gói và cân đo kiện, ghi kích thước thực tế; action «Đóng gói và cân đo kiện, ghi kích thước thực tế» có success/deny/recovery states. Authorization: Snapshot measurement; không tự xác nhận phí chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-181-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-182

REQ-V3-BC-182: Dán nhãn, quét barcode và đối chiếu Parcel. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Dán nhãn, quét barcode và đối chiếu Parcel; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Dán nhãn, quét barcode và đối chiếu Parcel; action «Dán nhãn, quét barcode và đối chiếu Parcel» có success/deny/recovery states. Authorization: Không tạo barcode trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-182-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-183

REQ-V3-BC-183: Lập manifest/danh sách kiện bàn giao Pickup Courier. Backend application boundary: warehouse xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Lập manifest/danh sách kiện bàn giao Pickup Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: warehouse; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Lập manifest/danh sách kiện bàn giao Pickup Courier; action «Lập manifest/danh sách kiện bàn giao Pickup Courier» có success/deny/recovery states. Authorization: Bằng chứng số kiện, thời gian và người bàn giao; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-183-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-184

REQ-V3-BC-184: Ghi nhận thiếu/sai hàng hoặc bao bì bất thường. Backend application boundary: warehouse xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận thiếu/sai hàng hoặc bao bì bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: warehouse; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Ghi nhận thiếu/sai hàng hoặc bao bì bất thường; action «Ghi nhận thiếu/sai hàng hoặc bao bì bất thường» có success/deny/recovery states. Authorization: Evidence, lý do và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-184-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-185

REQ-V3-BC-185: Xác nhận Courier tới lấy và phối hợp bàn giao. Backend application boundary: warehouse xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận Courier tới lấy và phối hợp bàn giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: warehouse; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xác nhận Courier tới lấy và phối hợp bàn giao; action «Xác nhận Courier tới lấy và phối hợp bàn giao» có success/deny/recovery states. Authorization: Không tự ghi PICKED_UP thay Courier; hai phía đối chiếu; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-185-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-212

REQ-V3-BC-212: Quét barcode và đối chiếu mã, số lượng kiện. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quét barcode và đối chiếu mã, số lượng kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quét barcode và đối chiếu mã, số lượng kiện; action «Quét barcode và đối chiếu mã, số lượng kiện» có success/deny/recovery states. Authorization: Đúng parcel/manifest; phát hiện thiếu, trùng, lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-212-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-213

REQ-V3-BC-213: Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup; action «Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup» có success/deny/recovery states. Authorization: Ghi sai lệch và evidence; không tự chốt lại phí; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-213-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-218

REQ-V3-BC-218: Kiểm tra hàng gửi và điều kiện chấp nhận. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra hàng gửi và điều kiện chấp nhận; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra hàng gửi và điều kiện chấp nhận; action «Kiểm tra hàng gửi và điều kiện chấp nhận» có success/deny/recovery states. Authorization: Theo chính sách hàng cấm/hạn chế; không truy cập dữ liệu không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-218-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-253

REQ-V3-BC-253: Quét barcode kiện trong công đoạn phân loại. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quét barcode kiện trong công đoạn phân loại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quét barcode kiện trong công đoạn phân loại; action «Quét barcode kiện trong công đoạn phân loại» có success/deny/recovery states. Authorization: Scan event truy vết người/máy/thời điểm; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-253-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-258

REQ-V3-BC-258: Ghi nhận barcode không đọc được. Backend application boundary: orders xử lý Parcel/label/measurement or SKU pick-pack facts; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận barcode không đọc được; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource Parcel/label/measurement or SKU pick-pack facts. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận barcode không đọc được; action «Ghi nhận barcode không đọc được» có success/deny/recovery states. Authorization: Case xử lý định danh thay thế có kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-258-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-PARCEL-HANDOVER

Trace REQ-AB-PARCEL-HANDOVER. Pick/pack/measure/labels/handover list; BE/application: Pick queue/quantity variance/packing/measurement/barcode identity/reprint audit; label only valid tracking/Parcel identity; Merchant handover preparation not carrier custody acceptance. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Wrong/missing/duplicate Parcel barcode and unauthorized printing denied; measurement snapshot; pack completion no carrier PICKED_UP; two-party handover through010/011; current guard và failure/replay/recovery, no mock-only success.
