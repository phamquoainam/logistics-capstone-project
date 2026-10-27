# Design 005 — pricing-contracts-quotation

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Rate/effective interval/rounding/phụ phí/Sales proposal/approval; authoritative Merchant contract versions/renewal decisions, Sales scoped proposal.

FE: Public/Sender/Merchant quote; Pricing publish; Sales chỉ đề xuất; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Pricing giữ quote/rate; orders snapshot; finance adjustment. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-002

REQ-V3-BC-002: Ước tính cước vận chuyển. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ước tính cước vận chuyển; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Ước tính cước vận chuyển; action «Ước tính cước vận chuyển» có success/deny/recovery states. Authorization: Không lộ giá hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-002-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-019

REQ-V3-BC-019: Xem hợp đồng và biểu phí áp dụng. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem hợp đồng và biểu phí áp dụng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem hợp đồng và biểu phí áp dụng; action «Xem hợp đồng và biểu phí áp dụng» có success/deny/recovery states. Authorization: Không tự sửa bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-019-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-023

REQ-V3-BC-023: Nhận báo giá và chọn dịch vụ. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận báo giá và chọn dịch vụ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Nhận báo giá và chọn dịch vụ; action «Nhận báo giá và chọn dịch vụ» có success/deny/recovery states. Authorization: Snapshot giá khi chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-023-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-039

REQ-V3-BC-039: Quản trị chính sách dịch vụ/bảng giá được ủy quyền. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản trị chính sách dịch vụ/bảng giá được ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản trị chính sách dịch vụ/bảng giá được ủy quyền; action «Quản trị chính sách dịch vụ/bảng giá được ủy quyền» có success/deny/recovery states. Authorization: Không tự động có quyền sửa COD; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-039-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-131

REQ-V3-BC-131: Tra cứu danh mục dịch vụ, điều kiện gửi hàng. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu danh mục dịch vụ, điều kiện gửi hàng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Tra cứu danh mục dịch vụ, điều kiện gửi hàng; action «Tra cứu danh mục dịch vụ, điều kiện gửi hàng» có success/deny/recovery states. Authorization: Catalog công khai, không lộ hợp đồng riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-131-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-138

REQ-V3-BC-138: Báo giá và chọn dịch vụ gửi lẻ. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Báo giá và chọn dịch vụ gửi lẻ; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Báo giá và chọn dịch vụ gửi lẻ; action «Báo giá và chọn dịch vụ gửi lẻ» có success/deny/recovery states. Authorization: Không áp dụng giá hợp đồng Merchant trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-138-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-189

REQ-V3-BC-189: Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng; action «Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng» có success/deny/recovery states. Authorization: Không tự thay bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-189-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-290

REQ-V3-BC-290: Đối chiếu giá chốt, phí điều chỉnh sau cân đo. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu giá chốt, phí điều chỉnh sau cân đo; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối chiếu giá chốt, phí điều chỉnh sau cân đo; action «Đối chiếu giá chốt, phí điều chỉnh sau cân đo» có success/deny/recovery states. Authorization: Snapshot + phiên bản rate card; audit sửa phí; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-290-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-304

REQ-V3-BC-304: Quản lý dịch vụ và chính sách nghiệp vụ theo quyền. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý dịch vụ và chính sách nghiệp vụ theo quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý dịch vụ và chính sách nghiệp vụ theo quyền; action «Quản lý dịch vụ và chính sách nghiệp vụ theo quyền» có success/deny/recovery states. Authorization: Giá đặc biệt cần approval riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-304-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-319

REQ-V3-BC-319: Cấu hình rate card theo vùng và tuyến. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cấu hình rate card theo vùng và tuyến; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Cấu hình rate card theo vùng và tuyến; action «Cấu hình rate card theo vùng và tuyến» có success/deny/recovery states. Authorization: Version, effective date, kiểm soát hồi tố; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-319-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-320

REQ-V3-BC-320: Quy tắc trọng lượng tính cước và đo kích thước. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quy tắc trọng lượng tính cước và đo kích thước; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quy tắc trọng lượng tính cước và đo kích thước; action «Quy tắc trọng lượng tính cước và đo kích thước» có success/deny/recovery states. Authorization: Khối lượng quy đổi/cân thực tế theo policy; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-320-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-321

REQ-V3-BC-321: Quản trị loại dịch vụ vận chuyển. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản trị loại dịch vụ vận chuyển; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản trị loại dịch vụ vận chuyển; action «Quản trị loại dịch vụ vận chuyển» có success/deny/recovery states. Authorization: Service availability và vùng phục vụ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-321-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-322

REQ-V3-BC-322: Cấu hình phí COD và các phụ phí. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cấu hình phí COD và các phụ phí; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Cấu hình phí COD và các phụ phí; action «Cấu hình phí COD và các phụ phí» có success/deny/recovery states. Authorization: Min/max/tiers rõ ràng, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-322-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-323

REQ-V3-BC-323: Áp dụng biểu phí riêng theo hợp đồng Merchant. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Áp dụng biểu phí riêng theo hợp đồng Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Áp dụng biểu phí riêng theo hợp đồng Merchant; action «Áp dụng biểu phí riêng theo hợp đồng Merchant» có success/deny/recovery states. Authorization: Chỉ hợp đồng được phê duyệt và còn hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-323-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-324

REQ-V3-BC-324: Quản lý chiết khấu và khuyến mãi. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý chiết khấu và khuyến mãi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý chiết khấu và khuyến mãi; action «Quản lý chiết khấu và khuyến mãi» có success/deny/recovery states. Authorization: Điều kiện áp dụng, phê duyệt và hạn sử dụng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-324-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-325

REQ-V3-BC-325: Quản trị thời điểm hiệu lực bảng giá. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản trị thời điểm hiệu lực bảng giá; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản trị thời điểm hiệu lực bảng giá; action «Quản trị thời điểm hiệu lực bảng giá» có success/deny/recovery states. Authorization: Không tự hồi tố đơn đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-325-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-326

REQ-V3-BC-326: Phân biệt giá ước tính, giá chốt và phí điều chỉnh. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân biệt giá ước tính, giá chốt và phí điều chỉnh; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân biệt giá ước tính, giá chốt và phí điều chỉnh; action «Phân biệt giá ước tính, giá chốt và phí điều chỉnh» có success/deny/recovery states. Authorization: Lưu price snapshot + adjustment trail; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-326-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-328

REQ-V3-BC-328: Audit sửa bảng giá, phê duyệt và phiên bản. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Audit sửa bảng giá, phê duyệt và phiên bản; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Audit sửa bảng giá, phê duyệt và phiên bản; action «Audit sửa bảng giá, phê duyệt và phiên bản» có success/deny/recovery states. Authorization: Không xóa price history; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R28, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-328-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-331

REQ-V3-BC-331: Theo dõi hợp đồng Merchant và gia hạn. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi hợp đồng Merchant và gia hạn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi hợp đồng Merchant và gia hạn; action «Theo dõi hợp đồng Merchant và gia hạn» có success/deny/recovery states. Authorization: Quyền xem theo scope, tài liệu versioned; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-331-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-332

REQ-V3-BC-332: Đề xuất bảng giá hoặc chiết khấu đặc biệt. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đề xuất bảng giá hoặc chiết khấu đặc biệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đề xuất bảng giá hoặc chiết khấu đặc biệt; action «Đề xuất bảng giá hoặc chiết khấu đặc biệt» có success/deny/recovery states. Authorization: Không tự phê duyệt hoặc áp dụng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-332-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-336

REQ-V3-BC-336: Audit đề xuất giá và thay đổi quan hệ hợp đồng. Backend application boundary: pricing xử lý service policy/quote/contract version; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Audit đề xuất giá và thay đổi quan hệ hợp đồng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: pricing; resource service policy/quote/contract version. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Audit đề xuất giá và thay đổi quan hệ hợp đồng; action «Audit đề xuất giá và thay đổi quan hệ hợp đồng» có success/deny/recovery states. Authorization: Approval trace; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-336-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-QUOTE-FORMULA

Trace REQ-AB-QUOTE-FORMULA. Quote/service/promotion/price ETA summary; BE/application: Versioned volumetric formula/max(actual,volumetric), exact units/rounding/order of surcharges/discounts; freight payer/COD eligibility/route supported service/promotion; ETA only from actual policy. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Golden fixtures boundaries/expired quote/publish races/private rate isolation; missing rule rejects rather than guesses; unsupported international/immediate not advertised active; current guard và failure/replay/recovery, no mock-only success.
