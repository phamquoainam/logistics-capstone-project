# Design 021 — claims-investigation-compensation

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Case/evidence/responsibility/proposal/approval ở orders; finance refund đã duyệt.

FE: Claims investigate/propose; approver riêng; CS/Merchant outcome; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Orders claim decision/evidence; finance payment refs/private evidence. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-112

REQ-V3-BC-112: Mở claim case gắn ticket/Shipment. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Mở claim case gắn ticket/Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Mở claim case gắn ticket/Shipment; action «Mở claim case gắn ticket/Shipment» có success/deny/recovery states. Authorization: Không trùng vụ việc; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-112-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-113

REQ-V3-BC-113: Thu thập POD/tracking/ảnh/biên bản. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thu thập POD/tracking/ảnh/biên bản; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thu thập POD/tracking/ảnh/biên bản; action «Thu thập POD/tracking/ảnh/biên bản» có success/deny/recovery states. Authorization: Bằng chứng có nguồn gốc; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-113-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-114

REQ-V3-BC-114: Xác định trách nhiệm và đề xuất phương án. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác định trách nhiệm và đề xuất phương án; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác định trách nhiệm và đề xuất phương án; action «Xác định trách nhiệm và đề xuất phương án» có success/deny/recovery states. Authorization: Người đề xuất khác người duyệt nếu cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-114-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-115

REQ-V3-BC-115: Ghi nhận outcome và thông báo CS/Merchant. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận outcome và thông báo CS/Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận outcome và thông báo CS/Merchant; action «Ghi nhận outcome và thông báo CS/Merchant» có success/deny/recovery states. Authorization: Audit đầy đủ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-115-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-117

REQ-V3-BC-117: Truy vết lịch sử xử lý claim. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Truy vết lịch sử xử lý claim; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Truy vết lịch sử xử lý claim; action «Truy vết lịch sử xử lý claim» có success/deny/recovery states. Authorization: Không xóa evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-117-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-129

REQ-V3-BC-129: Đọc bằng chứng claim và phê duyệt. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc bằng chứng claim và phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc bằng chứng claim và phê duyệt; action «Đọc bằng chứng claim và phê duyệt» có success/deny/recovery states. Authorization: Đúng phạm vi kiểm toán; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-129-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-163

REQ-V3-BC-163: Theo dõi claim, yêu cầu bồi thường và đơn bất thường. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi claim, yêu cầu bồi thường và đơn bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi claim, yêu cầu bồi thường và đơn bất thường; action «Theo dõi claim, yêu cầu bồi thường và đơn bất thường» có success/deny/recovery states. Authorization: Không tự phê duyệt bồi thường của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-163-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-297

REQ-V3-BC-297: Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD; action «Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD» có success/deny/recovery states. Authorization: Đối chiếu evidence, tracking, custody và trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-297-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-298

REQ-V3-BC-298: Truy xuất custody và biên bản bàn giao phục vụ claim. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Truy xuất custody và biên bản bàn giao phục vụ claim; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Truy xuất custody và biên bản bàn giao phục vụ claim; action «Truy xuất custody và biên bản bàn giao phục vụ claim» có success/deny/recovery states. Authorization: Chỉ case scope; không sửa sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-298-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-300

REQ-V3-BC-300: Điều tra thu sai COD liên quan claim. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra thu sai COD liên quan claim; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra thu sai COD liên quan claim; action «Điều tra thu sai COD liên quan claim» có success/deny/recovery states. Authorization: Không tự tạo điều chỉnh tài chính; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-300-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-301

REQ-V3-BC-301: Đánh giá chính sách, hạn mức và đề xuất số bồi thường. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đánh giá chính sách, hạn mức và đề xuất số bồi thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đánh giá chính sách, hạn mức và đề xuất số bồi thường; action «Đánh giá chính sách, hạn mức và đề xuất số bồi thường» có success/deny/recovery states. Authorization: Separation of duties và reason; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-301-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-302

REQ-V3-BC-302: Chuyển hồ sơ đề xuất sang người phê duyệt. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuyển hồ sơ đề xuất sang người phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Chuyển hồ sơ đề xuất sang người phê duyệt; action «Chuyển hồ sơ đề xuất sang người phê duyệt» có success/deny/recovery states. Authorization: Không tự phê duyệt chi trả khi không có quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-302-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-318

REQ-V3-BC-318: Đọc bằng chứng claim và phê duyệt. Backend application boundary: orders xử lý claim/evidence/proposal/approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đọc bằng chứng claim và phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource claim/evidence/proposal/approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đọc bằng chứng claim và phê duyệt; action «Đọc bằng chứng claim và phê duyệt» có success/deny/recovery states. Authorization: Không thay đổi outcome; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-318-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.


## DES-AB-CLAIMS

Trace REQ-AB-CLAIMS. Personal/Merchant claim case/evidence/outcome; BE/application: Retail may escalate own lost/damaged/delayed inquiry from020 to claim; Merchant Support routes recipient complaints; independent proposal/approval, finance payment refs. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Personal owner verified without Merchant; wrong case/evidence denied; no self-approved compensation or fake paid status; duplicate case policy; current guard và failure/replay/recovery, no mock-only success.
