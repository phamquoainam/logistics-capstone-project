# Design 009 — tracking-event-core-public-lookup

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Canonical append-only events, privacy projection/rate limit.

FE: Public masked tracking; Sender owned/Merchant scoped timeline; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Orders event nguồn; projection không ghi lại state. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-003

REQ-V3-BC-003: Tra cứu tracking giới hạn. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu tracking giới hạn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Tra cứu tracking giới hạn; action «Tra cứu tracking giới hạn» có success/deny/recovery states. Authorization: Không lộ PII, COD nhạy cảm; tăng xác minh khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-003-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-013

REQ-V3-BC-013: Theo dõi mọi Shipment thuộc Merchant. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi mọi Shipment thuộc Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi mọi Shipment thuộc Merchant; action «Theo dõi mọi Shipment thuộc Merchant» có success/deny/recovery states. Authorization: Không cross-tenant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-013-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-027

REQ-V3-BC-027: Tra cứu trạng thái Shipment thuộc scope. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu trạng thái Shipment thuộc scope; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tra cứu trạng thái Shipment thuộc scope; action «Tra cứu trạng thái Shipment thuộc scope» có success/deny/recovery states. Authorization: Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-027-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-051

REQ-V3-BC-051: Theo dõi sự kiện và nhiệm vụ vận chuyển. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi sự kiện và nhiệm vụ vận chuyển; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi sự kiện và nhiệm vụ vận chuyển; action «Theo dõi sự kiện và nhiệm vụ vận chuyển» có success/deny/recovery states. Authorization: Không truy cập ngoài region; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-051-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-060

REQ-V3-BC-060: Xem lịch sử nhiệm vụ cá nhân. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch sử nhiệm vụ cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem lịch sử nhiệm vụ cá nhân; action «Xem lịch sử nhiệm vụ cá nhân» có success/deny/recovery states. Authorization: PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-060-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-068

REQ-V3-BC-068: Xem trạng thái kiện tại Station. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem trạng thái kiện tại Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem trạng thái kiện tại Station; action «Xem trạng thái kiện tại Station» có success/deny/recovery states. Authorization: Đúng station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-068-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-078

REQ-V3-BC-078: Xem thông tin liên hệ người nhận tối thiểu. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem thông tin liên hệ người nhận tối thiểu; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem thông tin liên hệ người nhận tối thiểu; action «Xem thông tin liên hệ người nhận tối thiểu» có success/deny/recovery states. Authorization: Chỉ trong assignment; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-078-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-080

REQ-V3-BC-080: Nhận/tra cứu thông tin trạng thái giới hạn. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận/tra cứu thông tin trạng thái giới hạn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Verified Recipient channel — Nhận/tra cứu thông tin trạng thái giới hạn; action «Nhận/tra cứu thông tin trạng thái giới hạn» có success/deny/recovery states. Authorization: Không login; xác minh phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R10, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-080-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-106

REQ-V3-BC-106: Tra tracking, POD và điểm quét cuối. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra tracking, POD và điểm quét cuối; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tra tracking, POD và điểm quét cuối; action «Tra tracking, POD và điểm quét cuối» có success/deny/recovery states. Authorization: Chỉ ticket/scope được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-106-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-126

REQ-V3-BC-126: Xem lịch sử tracking/custody có chứng cứ. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch sử tracking/custody có chứng cứ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem lịch sử tracking/custody có chứng cứ; action «Xem lịch sử tracking/custody có chứng cứ» có success/deny/recovery states. Authorization: Chỉ dữ liệu được cấp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-126-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-143

REQ-V3-BC-143: Tra cứu tất cả Shipment do mình tạo. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu tất cả Shipment do mình tạo; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Tra cứu tất cả Shipment do mình tạo; action «Tra cứu tất cả Shipment do mình tạo» có success/deny/recovery states. Authorization: Resource owner isolation; không cross-account; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-143-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-170

REQ-V3-BC-170: Theo dõi giao chậm, vận đơn bất thường và tiến độ. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi giao chậm, vận đơn bất thường và tiến độ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi giao chậm, vận đơn bất thường và tiến độ; action «Theo dõi giao chậm, vận đơn bất thường và tiến độ» có success/deny/recovery states. Authorization: Đúng scope Merchant/branch; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-170-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-186

REQ-V3-BC-186: Theo dõi trạng thái kiện đã bàn giao cho hãng. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi trạng thái kiện đã bàn giao cho hãng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi trạng thái kiện đã bàn giao cho hãng; action «Theo dõi trạng thái kiện đã bàn giao cho hãng» có success/deny/recovery states. Authorization: Chỉ kho/đơn thuộc scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R20, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-186-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-194

REQ-V3-BC-194: Theo dõi giao chậm, giao thất bại và thông báo bất thường. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi giao chậm, giao thất bại và thông báo bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi giao chậm, giao thất bại và thông báo bất thường; action «Theo dõi giao chậm, giao thất bại và thông báo bất thường» có success/deny/recovery states. Authorization: Chỉ dữ liệu cần cho CSKH; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-194-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-293

REQ-V3-BC-293: Xử lý yêu cầu giao chậm qua điểm quét cuối. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xử lý yêu cầu giao chậm qua điểm quét cuối; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xử lý yêu cầu giao chậm qua điểm quét cuối; action «Xử lý yêu cầu giao chậm qua điểm quét cuối» có success/deny/recovery states. Authorization: Không giả lập tracking event; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-293-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-315

REQ-V3-BC-315: Đọc tracking events để kiểm toán vận chuyển. Backend application boundary: orders xử lý source tracking event/scoped projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc tracking events để kiểm toán vận chuyển; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource source tracking event/scoped projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc tracking events để kiểm toán vận chuyển; action «Đọc tracking events để kiểm toán vận chuyển» có success/deny/recovery states. Authorization: Không sửa trạng thái/attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-315-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-TRACKING-PRIVACY

Trace REQ-AB-TRACKING-PRIVACY. Public limited lookup; private detail/timeline; BE/application: Shipment status != tracking event != delivery attempt != custody event != financial event; current owners get permitted detail, public code alone redacts identities/full contact/address/COD. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Direct code/UUID enumeration, token misuse, delayed/reordered duplicate events and revoked scope tests; trace sources; no event retroactively fakes POD; current guard và failure/replay/recovery, no mock-only success.
