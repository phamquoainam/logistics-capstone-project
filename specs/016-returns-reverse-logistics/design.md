# Design 016 — returns-reverse-logistics

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Orders return/state; dispatch reverse custody; warehouse QC khi WMS.

FE: Sender/Merchant yêu cầu/theo dõi; Operations tasks; Courier handover; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Return refs ở orders, custody ở dispatch, return movement ở warehouse. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-052

REQ-V3-BC-052: Điều phối hoàn hàng theo workflow. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều phối hoàn hàng theo workflow; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều phối hoàn hàng theo workflow; action «Điều phối hoàn hàng theo workflow» có success/deny/recovery states. Authorization: Không xóa lịch sử delivery; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-052-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-067

REQ-V3-BC-067: Nhận kiện giao thất bại/đang hoàn. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận kiện giao thất bại/đang hoàn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận kiện giao thất bại/đang hoàn; action «Nhận kiện giao thất bại/đang hoàn» có success/deny/recovery states. Authorization: Đúng trạng thái custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-067-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-077

REQ-V3-BC-077: Trả kiện giao thất bại về Station. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Trả kiện giao thất bại về Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Trả kiện giao thất bại về Station; action «Trả kiện giao thất bại về Station» có success/deny/recovery states. Authorization: Có custody handover; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-077-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-111

REQ-V3-BC-111: Theo dõi ticket liên quan hoàn hàng. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi ticket liên quan hoàn hàng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi ticket liên quan hoàn hàng; action «Theo dõi ticket liên quan hoàn hàng» có success/deny/recovery states. Authorization: Không tự sửa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-111-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-146

REQ-V3-BC-146: Theo dõi và quản lý hoàn trả kiện gửi lẻ. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi và quản lý hoàn trả kiện gửi lẻ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Theo dõi và quản lý hoàn trả kiện gửi lẻ; action «Theo dõi và quản lý hoàn trả kiện gửi lẻ» có success/deny/recovery states. Authorization: Return events/custody rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-146-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-178

REQ-V3-BC-178: Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái; action «Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái» có success/deny/recovery states. Authorization: Hàng đã pickup phải đi reverse logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-178-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-198

REQ-V3-BC-198: Theo dõi hoàn hàng và trao đổi kết quả với Recipient. Backend application boundary: orders xử lý return authorization/events; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi hoàn hàng và trao đổi kết quả với Recipient; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource return authorization/events. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi hoàn hàng và trao đổi kết quả với Recipient; action «Theo dõi hoàn hàng và trao đổi kết quả với Recipient» có success/deny/recovery states. Authorization: Không thao tác custody của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-198-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-RETURNS

Trace REQ-AB-RETURNS. Personal/Merchant return visibility and authorized request; BE/application: After pickup use reverse logistics, never ordinary cancel; return authorization/source/evidence/status/custody visible by ownership. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Failure/return/retry request cannot silently set custody or refund; historical delivery attempts preserved; rejected request reason safe; current guard và failure/replay/recovery, no mock-only success.
