# Design 012 — sorting-hub-manifests

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Scan provenance/bag relation/manifest version/variance approval.

FE: Sorting scan/misroute/unreadable; Hub capacity/variance approval; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Dispatch manifest/custody/handling units; orders Parcel tham chiếu. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-254

REQ-V3-BC-254: Phân tuyến theo đích và quy tắc vận hành. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân tuyến theo đích và quy tắc vận hành; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân tuyến theo đích và quy tắc vận hành; action «Phân tuyến theo đích và quy tắc vận hành» có success/deny/recovery states. Authorization: Đúng scope Hub, route plan hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-254-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-255

REQ-V3-BC-255: Gom kiện vào bao/túi/đơn vị xử lý. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gom kiện vào bao/túi/đơn vị xử lý; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Gom kiện vào bao/túi/đơn vị xử lý; action «Gom kiện vào bao/túi/đơn vị xử lý» có success/deny/recovery states. Authorization: Parent-child manifest và số lượng kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-255-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-256

REQ-V3-BC-256: Xác nhận kiện đã qua dây chuyền phân loại. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận kiện đã qua dây chuyền phân loại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận kiện đã qua dây chuyền phân loại; action «Xác nhận kiện đã qua dây chuyền phân loại» có success/deny/recovery states. Authorization: Không giả lập scan; có evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-256-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-257

REQ-V3-BC-257: Ghi nhận kiện sai tuyến. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận kiện sai tuyến; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận kiện sai tuyến; action «Ghi nhận kiện sai tuyến» có success/deny/recovery states. Authorization: Exception, redirect plan, không xóa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-257-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-259

REQ-V3-BC-259: Ghi nhận kiện hư hỏng khi phân loại. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận kiện hư hỏng khi phân loại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận kiện hư hỏng khi phân loại; action «Ghi nhận kiện hư hỏng khi phân loại» có success/deny/recovery states. Authorization: Ảnh/biên bản, custody, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-259-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-260

REQ-V3-BC-260: Chuyển kiện/bao vào tuyến vận tải phù hợp. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuyển kiện/bao vào tuyến vận tải phù hợp; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Chuyển kiện/bao vào tuyến vận tải phù hợp; action «Chuyển kiện/bao vào tuyến vận tải phù hợp» có success/deny/recovery states. Authorization: Manifest/tuyến/đích khớp, không tự tạo trip quyền driver; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-260-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-261

REQ-V3-BC-261: Hạn chế PII và dữ liệu tài chính Merchant. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Hạn chế PII và dữ liệu tài chính Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Hạn chế PII và dữ liệu tài chính Merchant; action «Hạn chế PII và dữ liệu tài chính Merchant» có success/deny/recovery states. Authorization: Least privilege, test deny access; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R25, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-261-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-262

REQ-V3-BC-262: Giám sát năng lực xử lý của Hub. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát năng lực xử lý của Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát năng lực xử lý của Hub; action «Giám sát năng lực xử lý của Hub» có success/deny/recovery states. Authorization: Chỉ hub thuộc quyền; capacity measurable; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-262-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-263

REQ-V3-BC-263: Theo dõi tồn kiện theo vị trí/thời gian. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi tồn kiện theo vị trí/thời gian; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi tồn kiện theo vị trí/thời gian; action «Theo dõi tồn kiện theo vị trí/thời gian» có success/deny/recovery states. Authorization: Event custody và inventory reconcile; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-263-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-265

REQ-V3-BC-265: Đối chiếu manifest và xử lý chênh lệch. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu manifest và xử lý chênh lệch; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối chiếu manifest và xử lý chênh lệch; action «Đối chiếu manifest và xử lý chênh lệch» có success/deny/recovery states. Authorization: Không sửa âm thầm, chứng cứ/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-265-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-269

REQ-V3-BC-269: Phê duyệt điều chỉnh tồn theo thẩm quyền. Backend application boundary: dispatch xử lý sort/bag/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt điều chỉnh tồn theo thẩm quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource sort/bag/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt điều chỉnh tồn theo thẩm quyền; action «Phê duyệt điều chỉnh tồn theo thẩm quyền» có success/deny/recovery states. Authorization: Reason/evidence/separation of duties, không xóa custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-269-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
