# Design 022 — enterprise-api-webhooks

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Merchant integration/delivery; Identity credential; gọi owner APIs; Sales coordination metadata thuộc merchant; no credential disclosure; status không fake integration.

FE: Merchant configure/rotate/revoke; IT failed deliveries; machine không human portal; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Config ở merchant, credential ở identity; không duplicate shipment/quote. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-018

REQ-V3-BC-018: Quản lý API integration/webhook Merchant. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý API integration/webhook Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý API integration/webhook Merchant; action «Quản lý API integration/webhook Merchant» có success/deny/recovery states. Authorization: Key và scope tách riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-018-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-164

REQ-V3-BC-164: Quản lý cấu hình tích hợp và webhook trong scope Merchant. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý cấu hình tích hợp và webhook trong scope Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý cấu hình tích hợp và webhook trong scope Merchant; action «Quản lý cấu hình tích hợp và webhook trong scope Merchant» có success/deny/recovery states. Authorization: Credential secret bảo vệ; không tự cấp scope cao hơn; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-164-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-175

REQ-V3-BC-175: Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp; action «Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp» có success/deny/recovery states. Authorization: Credential dùng integration principal, không chia sẻ human secret; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-175-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-200

REQ-V3-BC-200: Xác thực integration principal và quản lý scope API gắn Merchant. Backend application boundary: identity xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác thực integration principal và quản lý scope API gắn Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Xác thực integration principal và quản lý scope API gắn Merchant; action «Xác thực integration principal và quản lý scope API gắn Merchant» có success/deny/recovery states. Authorization: Credential tách human account; rotate, revoke, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-200-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-201

REQ-V3-BC-201: Thiết lập endpoint và subscription webhook trạng thái. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thiết lập endpoint và subscription webhook trạng thái; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Thiết lập endpoint và subscription webhook trạng thái; action «Thiết lập endpoint và subscription webhook trạng thái» có success/deny/recovery states. Authorization: Tenant bound, signature, retry, duplicate handling; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-201-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-202

REQ-V3-BC-202: Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website; action «Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website» có success/deny/recovery states. Authorization: API idempotency key; đúng Merchant; validation; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-202-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-203

REQ-V3-BC-203: Lấy báo giá và danh sách dịch vụ được phép qua API. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Lấy báo giá và danh sách dịch vụ được phép qua API; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Lấy báo giá và danh sách dịch vụ được phép qua API; action «Lấy báo giá và danh sách dịch vụ được phép qua API» có success/deny/recovery states. Authorization: Áp dụng đúng hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-203-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-204

REQ-V3-BC-204: Lấy nhãn và barcode của Shipment/Parcel qua API. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Lấy nhãn và barcode của Shipment/Parcel qua API; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Lấy nhãn và barcode của Shipment/Parcel qua API; action «Lấy nhãn và barcode của Shipment/Parcel qua API» có success/deny/recovery states. Authorization: Chỉ tài nguyên thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-204-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-205

REQ-V3-BC-205: Tra cứu Shipment và nhận tracking events. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu Shipment và nhận tracking events; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Tra cứu Shipment và nhận tracking events; action «Tra cứu Shipment và nhận tracking events» có success/deny/recovery states. Authorization: Hạn chế PII; filtering theo Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-205-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-206

REQ-V3-BC-206: Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Machine API/webhook — Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries; action «Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries» có success/deny/recovery states. Authorization: Quan sát idempotency; không tạo bút toán/đơn trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R22, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-206-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-335

REQ-V3-BC-335: Phối hợp onboarding và integration Merchant. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phối hợp onboarding và integration Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phối hợp onboarding và integration Merchant; action «Phối hợp onboarding và integration Merchant» có success/deny/recovery states. Authorization: Không tự xem/cấp mọi API secrets; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-335-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-357

REQ-V3-BC-357: Điều tra lỗi webhook và lịch sử retry. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra lỗi webhook và lịch sử retry; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra lỗi webhook và lịch sử retry; action «Điều tra lỗi webhook và lịch sử retry» có success/deny/recovery states. Authorization: Idempotent retries; không tạo duplicate state; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-357-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-358

REQ-V3-BC-358: Quản lý cấu hình tích hợp theo quyền kỹ thuật. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý cấu hình tích hợp theo quyền kỹ thuật; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý cấu hình tích hợp theo quyền kỹ thuật; action «Quản lý cấu hình tích hợp theo quyền kỹ thuật» có success/deny/recovery states. Authorization: Tách secret và business action; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-358-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-360

REQ-V3-BC-360: Đối chiếu lỗi đồng bộ Shipment. Backend application boundary: merchant xử lý integration config/scoped credential reference/webhook delivery; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu lỗi đồng bộ Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource integration config/scoped credential reference/webhook delivery. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối chiếu lỗi đồng bộ Shipment; action «Đối chiếu lỗi đồng bộ Shipment» có success/deny/recovery states. Authorization: Không sửa nghiệp vụ vượt quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-360-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.


## DES-AB-ENTERPRISE

Trace REQ-AB-ENTERPRISE. Scoped integration settings and machine API/webhooks; BE/application: Separate machine credential issue/rotate/revoke/scope/lifetime; API quote/create/label/status; signed webhooks retry/backoff/failure/reconciliation; Sales coordination only minimal status. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Cross-tenant/credential replay/revoked machine deny; no human secret reuse; key disclosure once only per approved contract; duplicate webhook no business duplicate; sync errors reconcile source job; current guard và failure/replay/recovery, no mock-only success.
