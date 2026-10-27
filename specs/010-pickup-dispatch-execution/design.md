# Design 010 — pickup-dispatch-execution

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Pickup pool/assignment/version/attempt/scan/custody API.

FE: Operations assignment; Courier check-in/scan/failure; Sender request; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Dispatch assignment/custody; orders state; workflow cùng DB khi cần. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-037

REQ-V3-BC-037: Phê duyệt ngoại lệ điều phối theo thẩm quyền. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt ngoại lệ điều phối theo thẩm quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt ngoại lệ điều phối theo thẩm quyền; action «Phê duyệt ngoại lệ điều phối theo thẩm quyền» có success/deny/recovery states. Authorization: Không tự tạo bằng chứng pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-037-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-047

REQ-V3-BC-047: Xem pickup pool và phân công pickup. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem pickup pool và phân công pickup; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem pickup pool và phân công pickup; action «Xem pickup pool và phân công pickup» có success/deny/recovery states. Authorization: Theo vùng, tải và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-047-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-049

REQ-V3-BC-049: Tái phân công pickup có lý do. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tái phân công pickup có lý do; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tái phân công pickup có lý do; action «Tái phân công pickup có lý do» có success/deny/recovery states. Authorization: Audit, không tự xác nhận pickup; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-049-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-056

REQ-V3-BC-056: Xem pickup assignment cá nhân. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem pickup assignment cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem pickup assignment cá nhân; action «Xem pickup assignment cá nhân» có success/deny/recovery states. Authorization: Chỉ task được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-056-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-057

REQ-V3-BC-057: Check-in/scan và ghi nhận PICKED_UP. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Check-in/scan và ghi nhận PICKED_UP; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Check-in/scan và ghi nhận PICKED_UP; action «Check-in/scan và ghi nhận PICKED_UP» có success/deny/recovery states. Authorization: Có bằng chứng, đúng trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-057-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-058

REQ-V3-BC-058: Ghi pickup thất bại và lý do. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi pickup thất bại và lý do; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi pickup thất bại và lý do; action «Ghi pickup thất bại và lý do» có success/deny/recovery states. Authorization: Attempt có timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-058-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-062

REQ-V3-BC-062: Tối ưu thứ tự tuyến pickup. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tối ưu thứ tự tuyến pickup; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tối ưu thứ tự tuyến pickup; action «Tối ưu thứ tự tuyến pickup» có success/deny/recovery states. Authorization: Không bắt buộc GPS/routing chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-062-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-141

REQ-V3-BC-141: Đặt yêu cầu Pickup tại nhà. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đặt yêu cầu Pickup tại nhà; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Đặt yêu cầu Pickup tại nhà; action «Đặt yêu cầu Pickup tại nhà» có success/deny/recovery states. Authorization: Dịch vụ/địa chỉ đủ điều kiện; phân công do Operations; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-141-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-207

REQ-V3-BC-207: Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại; action «Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại» có success/deny/recovery states. Authorization: Station/region scope; chỉ assignment đủ điều kiện; chống phân công trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-207-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-210

REQ-V3-BC-210: Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender; action «Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender» có success/deny/recovery states. Authorization: Chỉ nhiệm vụ đang được giao; PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-210-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-211

REQ-V3-BC-211: Liên hệ Merchant/Retail Sender phục vụ pickup. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Liên hệ Merchant/Retail Sender phục vụ pickup; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Liên hệ Merchant/Retail Sender phục vụ pickup; action «Liên hệ Merchant/Retail Sender phục vụ pickup» có success/deny/recovery states. Authorization: Chỉ trong phạm vi nhiệm vụ; log/quy tắc bảo vệ liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-211-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-214

REQ-V3-BC-214: Xác nhận thời gian và bằng chứng bàn giao nhận kiện. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận thời gian và bằng chứng bàn giao nhận kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận thời gian và bằng chứng bàn giao nhận kiện; action «Xác nhận thời gian và bằng chứng bàn giao nhận kiện» có success/deny/recovery states. Authorization: Assignment hợp lệ; timestamp, actor, custody; không button-only PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-214-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-244

REQ-V3-BC-244: Xem nhiệm vụ Pickup Courier toàn đội. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem nhiệm vụ Pickup Courier toàn đội; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem nhiệm vụ Pickup Courier toàn đội; action «Xem nhiệm vụ Pickup Courier toàn đội» có success/deny/recovery states. Authorization: Chỉ đội/ca được giao; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-244-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-247

REQ-V3-BC-247: Đề xuất tái phân công pickup. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đề xuất tái phân công pickup; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đề xuất tái phân công pickup; action «Đề xuất tái phân công pickup» có success/deny/recovery states. Authorization: Approval/authorization tách biệt với hành động reassignment; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-247-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-277

REQ-V3-BC-277: Giám sát năng lực pickup giữa các khu vực. Backend application boundary: dispatch xử lý pickup request/assignment/evidence; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát năng lực pickup giữa các khu vực; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource pickup request/assignment/evidence. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát năng lực pickup giữa các khu vực; action «Giám sát năng lực pickup giữa các khu vực» có success/deny/recovery states. Authorization: Không tự tạo chứng cứ PICKED_UP; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-277-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
