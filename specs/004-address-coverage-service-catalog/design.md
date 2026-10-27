# Design 004 — address-coverage-service-catalog

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Catalog phiên bản, địa chỉ/coverage/điểm phục vụ; routing adapter theo contract.

FE: Public coverage/Station/Hub, Sender address book, Merchant pickup; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: merchant address/catalog/coverage; Station master dispatch L011 qua read projection; pickup mutations chỉ Spec004; orders snapshots immutable. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-001

REQ-V3-BC-001: Tra cứu vùng phục vụ công khai. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu vùng phục vụ công khai; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Tra cứu vùng phục vụ công khai; action «Tra cứu vùng phục vụ công khai» có success/deny/recovery states. Authorization: Chỉ dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-001-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-005

REQ-V3-BC-005: Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động; action «Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động» có success/deny/recovery states. Authorization: Dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-005-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-010

REQ-V3-BC-010: Quản lý địa chỉ lấy hàng. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý địa chỉ lấy hàng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý địa chỉ lấy hàng; action «Quản lý địa chỉ lấy hàng» có success/deny/recovery states. Authorization: Catalog phiên bản và kiểm tra dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-010-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-022

REQ-V3-BC-022: Chuẩn hóa địa chỉ nhận/gửi. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuẩn hóa địa chỉ nhận/gửi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Chuẩn hóa địa chỉ nhận/gửi; action «Chuẩn hóa địa chỉ nhận/gửi» có success/deny/recovery states. Authorization: Dữ liệu hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-022-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-033

REQ-V3-BC-033: Quản lý vùng phục vụ theo policy. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý vùng phục vụ theo policy; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý vùng phục vụ theo policy; action «Quản lý vùng phục vụ theo policy» có success/deny/recovery states. Authorization: Có phiên bản và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-033-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-136

REQ-V3-BC-136: Lưu và quản lý địa chỉ gửi/nhận thường dùng. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Lưu và quản lý địa chỉ gửi/nhận thường dùng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Lưu và quản lý địa chỉ gửi/nhận thường dùng; action «Lưu và quản lý địa chỉ gửi/nhận thường dùng» có success/deny/recovery states. Authorization: Chỉ sở hữu cá nhân; chuẩn hóa địa chỉ; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-136-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-137

REQ-V3-BC-137: Kiểm tra vùng phục vụ theo tuyến gửi lẻ. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra vùng phục vụ theo tuyến gửi lẻ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Kiểm tra vùng phục vụ theo tuyến gửi lẻ; action «Kiểm tra vùng phục vụ theo tuyến gửi lẻ» có success/deny/recovery states. Authorization: Coverage snapshot/quy tắc hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-137-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-271

REQ-V3-BC-271: Quản lý vùng giao nhận và coverage. Backend application boundary: merchant xử lý versioned address/catalog/coverage; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý vùng giao nhận và coverage; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource versioned address/catalog/coverage. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý vùng giao nhận và coverage; action «Quản lý vùng giao nhận và coverage» có success/deny/recovery states. Authorization: Versioning/policy, không hồi tố đơn đã chốt tùy ý; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-271-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


### DES-BC-009

REQ-V3-BC-009: Quản lý pickup point. Backend application boundary: merchant xử lý Merchant pickup point/address validation reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý pickup point; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource Merchant pickup point/address validation reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý pickup point; action «Quản lý pickup point» có success/deny/recovery states. Authorization: Tenant isolation; archive/sửa có điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-009-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.


### DES-BC-021

REQ-V3-BC-021: Xem và sửa pickup point được ủy quyền. Backend application boundary: merchant xử lý delegated Merchant pickup point/address reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem và sửa pickup point được ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource delegated Merchant pickup point/address reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem và sửa pickup point được ủy quyền; action «Xem và sửa pickup point được ủy quyền» có success/deny/recovery states. Authorization: Không quản lý nhân viên mặc định; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-021-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.


### DES-BC-168

REQ-V3-BC-168: Quản lý pickup point trong phạm vi ủy quyền. Backend application boundary: merchant xử lý delegated Manager pickup point/address reference; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý pickup point trong phạm vi ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource delegated Manager pickup point/address reference. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý pickup point trong phạm vi ủy quyền; action «Quản lý pickup point trong phạm vi ủy quyền» có success/deny/recovery states. Authorization: Merchant/branch scope; không truy cập tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-168-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.


## Sole pickup-point command và Station projection

BC009/021/168 cùng một Merchant-owned pickup aggregate/command API, permission khác theo current Owner/Operator/Manager delegation; BC010/022 dùng cùng address validation contract, không command owner thứ hai. Validate raw/structured administrative catalog version, lat/lon/source/confidence/confirmed time; geocoder timeout/ambiguous → unconfirmed/manual fallback approved, không đoán. Update/archive versioned, current owner+scope, historical Order snapshots không đổi. Station/Hub masters do dispatch trong011 sở hữu;004 public query chỉ published projection/minimal hours/location/serviceability, không master DML. Public catalog core có thể chuẩn bị sớm; authenticated address/pickup dùng003 membership contract thật.
