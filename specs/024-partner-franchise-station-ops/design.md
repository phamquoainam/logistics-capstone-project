# Design 024 — partner-franchise-station-ops

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Partner binding qua merchant/identity API; dispatch custody/counter.

FE: Partner counter/stock/handover/revenue scoped; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Partner org ở merchant, custody ở dispatch, revenue ở finance. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-364

REQ-V3-BC-364: Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định; action «Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định» có success/deny/recovery states. Authorization: Partner+Station scope, xác minh walk-in; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-364-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-365

REQ-V3-BC-365: Bàn giao kiện cho hãng vận chuyển. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Bàn giao kiện cho hãng vận chuyển; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Bàn giao kiện cho hãng vận chuyển; action «Bàn giao kiện cho hãng vận chuyển» có success/deny/recovery states. Authorization: Actor, count, manifest, timestamp, custody proof; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-365-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-366

REQ-V3-BC-366: Xác nhận số lượng thực giao/nhận. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận số lượng thực giao/nhận; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận số lượng thực giao/nhận; action «Xác nhận số lượng thực giao/nhận» có success/deny/recovery states. Authorization: Sai lệch phải ghi event và investigation; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-366-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-367

REQ-V3-BC-367: Quản lý tồn kiện ở điểm đại lý. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý tồn kiện ở điểm đại lý; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý tồn kiện ở điểm đại lý; action «Quản lý tồn kiện ở điểm đại lý» có success/deny/recovery states. Authorization: Không đọc tồn nơi khác; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-367-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-368

REQ-V3-BC-368: Xem doanh thu dịch vụ thuộc điểm được giao. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem doanh thu dịch vụ thuộc điểm được giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem doanh thu dịch vụ thuộc điểm được giao; action «Xem doanh thu dịch vụ thuộc điểm được giao» có success/deny/recovery states. Authorization: Chỉ báo cáo được phép; không sửa settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-368-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-369

REQ-V3-BC-369: Ràng buộc Partner Organization với Station. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ràng buộc Partner Organization với Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ràng buộc Partner Organization với Station; action «Ràng buộc Partner Organization với Station» có success/deny/recovery states. Authorization: Explicit membership/scope; no network-wide access; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-369-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-370

REQ-V3-BC-370: Quét/kiểm tra nhãn và tình trạng kiện. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quét/kiểm tra nhãn và tình trạng kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quét/kiểm tra nhãn và tình trạng kiện; action «Quét/kiểm tra nhãn và tình trạng kiện» có success/deny/recovery states. Authorization: Không giả mạo scan của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-370-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-371

REQ-V3-BC-371: Truy vết nghiệp vụ điểm đối tác. Backend application boundary: dispatch xử lý Partner point/assignment/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Truy vết nghiệp vụ điểm đối tác; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Partner point/assignment/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Truy vết nghiệp vụ điểm đối tác; action «Truy vết nghiệp vụ điểm đối tác» có success/deny/recovery states. Authorization: Không xóa handover history; permission code DECISION_REQUIRED (no runtime allowlist expansion); PARTNER_ORG_STATION. Test fixtures riêng: valid R33, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-371-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.
