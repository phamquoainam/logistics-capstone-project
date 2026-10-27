# Design 027 — platform-admin-audit-governance

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Identity policy, platform technical ops; business admin qua owner APIs.

FE: Workspaces theo permission, Auditor read-only, IT scoped retry; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Domain giữ audit nguồn; platform không business aggregate chung. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-031

REQ-V3-BC-031: Truy cập admin bằng quyền nghiệp vụ. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Truy cập admin bằng quyền nghiệp vụ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Truy cập admin bằng quyền nghiệp vụ; action «Truy cập admin bằng quyền nghiệp vụ» có success/deny/recovery states. Authorization: Không mặc nhiên có Identity quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-031-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-036

REQ-V3-BC-036: Tra cứu audit nghiệp vụ liên quan. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu audit nghiệp vụ liên quan; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tra cứu audit nghiệp vụ liên quan; action «Tra cứu audit nghiệp vụ liên quan» có success/deny/recovery states. Authorization: Không xóa/sửa audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-036-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-043

REQ-V3-BC-043: Xem sự kiện bảo mật Identity. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem sự kiện bảo mật Identity; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem sự kiện bảo mật Identity; action «Xem sự kiện bảo mật Identity» có success/deny/recovery states. Authorization: Least privilege; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R05, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-043-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-124

REQ-V3-BC-124: Xem audit vận hành của Station. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem audit vận hành của Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem audit vận hành của Station; action «Xem audit vận hành của Station» có success/deny/recovery states. Authorization: Chỉ đọc; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-124-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-125

REQ-V3-BC-125: Đọc audit events đúng phạm vi. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc audit events đúng phạm vi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc audit events đúng phạm vi; action «Đọc audit events đúng phạm vi» có success/deny/recovery states. Authorization: Read only, không sửa/xóa; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-125-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-128

REQ-V3-BC-128: Xem lịch sử phân quyền và thu hồi quyền. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch sử phân quyền và thu hồi quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem lịch sử phân quyền và thu hồi quyền; action «Xem lịch sử phân quyền và thu hồi quyền» có success/deny/recovery states. Authorization: Không quản lý tài khoản; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-128-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-252

REQ-V3-BC-252: Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng; action «Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng» có success/deny/recovery states. Authorization: Không mặc định có privilege đặc biệt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-252-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-279

REQ-V3-BC-279: Audit quyết định chuyển tuyến và ngoại lệ. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Audit quyết định chuyển tuyến và ngoại lệ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Audit quyết định chuyển tuyến và ngoại lệ; action «Audit quyết định chuyển tuyến và ngoại lệ» có success/deny/recovery states. Authorization: Chỉ quyền được ủy quyền, không xóa log; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-279-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-306

REQ-V3-BC-306: Phân công vai trò nghiệp vụ nhân sự theo ủy quyền. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân công vai trò nghiệp vụ nhân sự theo ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân công vai trò nghiệp vụ nhân sự theo ủy quyền; action «Phân công vai trò nghiệp vụ nhân sự theo ủy quyền» có success/deny/recovery states. Authorization: Không tự cấp admin bảo mật/Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-306-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-308

REQ-V3-BC-308: Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ; action «Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ» có success/deny/recovery states. Authorization: Theo tổ chức/vùng được cấp phép; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-308-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-313

REQ-V3-BC-313: Giám sát thay đổi quyền và sự kiện Identity. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát thay đổi quyền và sự kiện Identity; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát thay đổi quyền và sự kiện Identity; action «Giám sát thay đổi quyền và sự kiện Identity» có success/deny/recovery states. Authorization: Chỉ phạm vi bảo mật, append-only audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R05, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-313-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-314

REQ-V3-BC-314: Xem lịch sử thay đổi quyền và quản trị. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem lịch sử thay đổi quyền và quản trị; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem lịch sử thay đổi quyền và quản trị; action «Xem lịch sử thay đổi quyền và quản trị» có success/deny/recovery states. Authorization: Read-only, scoped, immutable source; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-314-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-348

REQ-V3-BC-348: Quản lý tenant và quyền quản trị nền tảng cấp cao. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý tenant và quyền quản trị nền tảng cấp cao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý tenant và quyền quản trị nền tảng cấp cao; action «Quản lý tenant và quyền quản trị nền tảng cấp cao» có success/deny/recovery states. Authorization: Privilege boundary, approval và session assurance; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-348-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-349

REQ-V3-BC-349: Cấu hình phân vùng tổ chức ở cấp nền tảng. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cấu hình phân vùng tổ chức ở cấp nền tảng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Cấu hình phân vùng tổ chức ở cấp nền tảng; action «Cấu hình phân vùng tổ chức ở cấp nền tảng» có success/deny/recovery states. Authorization: Không tự có quyền sở hữu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-349-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-350

REQ-V3-BC-350: Quản lý feature flags theo kiểm soát thay đổi. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý feature flags theo kiểm soát thay đổi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý feature flags theo kiểm soát thay đổi; action «Quản lý feature flags theo kiểm soát thay đổi» có success/deny/recovery states. Authorization: Audit, rollback và rollout scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-350-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-351

REQ-V3-BC-351: Quản lý chính sách nền tảng được phân quyền. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý chính sách nền tảng được phân quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý chính sách nền tảng được phân quyền; action «Quản lý chính sách nền tảng được phân quyền» có success/deny/recovery states. Authorization: Policy versioning và phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-351-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-352

REQ-V3-BC-352: Phê duyệt tác vụ quản trị đặc biệt theo quy trình. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt tác vụ quản trị đặc biệt theo quy trình; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt tác vụ quản trị đặc biệt theo quy trình; action «Phê duyệt tác vụ quản trị đặc biệt theo quy trình» có success/deny/recovery states. Authorization: Dual approval khi policy yêu cầu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-352-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-353

REQ-V3-BC-353: Giám sát thao tác đặc quyền và break-glass. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát thao tác đặc quyền và break-glass; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát thao tác đặc quyền và break-glass; action «Giám sát thao tác đặc quyền và break-glass» có success/deny/recovery states. Authorization: Reason, expiry, immutable audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-353-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-361

REQ-V3-BC-361: Vận hành production có kiểm soát. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Vận hành production có kiểm soát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Vận hành production có kiểm soát; action «Vận hành production có kiểm soát» có success/deny/recovery states. Authorization: Approval, least privilege, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-361-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-362

REQ-V3-BC-362: Không impersonate Merchant tùy ý. Backend application boundary: identity xử lý policy/audit/read projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Không impersonate Merchant tùy ý; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: identity; resource policy/audit/read projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Không impersonate Merchant tùy ý; action «Không impersonate Merchant tùy ý» có success/deny/recovery states. Authorization: Session/account controls và evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-362-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
