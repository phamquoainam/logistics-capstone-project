# Design 020 — customer-support-tickets-sla

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Public contact core F1; Sender direct/Merchant Support handoff; approved source-domain command; Merchant-owned assigned-customer relationship/interactions qua merchant API, không account/password master.

FE: Public contact/Sender support/Merchant inbox/Logistics CS queue; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Ticket đề xuất dưới orders, không sửa Finance/Dispatch; D-V3-05. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-004

REQ-V3-BC-004: Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập; action «Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập» có success/deny/recovery states. Authorization: Rate limit và kiểm soát spam; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-004-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-016

REQ-V3-BC-016: Gửi và theo dõi support ticket. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gửi và theo dõi support ticket; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Gửi và theo dõi support ticket; action «Gửi và theo dõi support ticket» có success/deny/recovery states. Authorization: Không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-016-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-030

REQ-V3-BC-030: Tạo ticket hỗ trợ Logistics nếu được cấp quyền. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo ticket hỗ trợ Logistics nếu được cấp quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo ticket hỗ trợ Logistics nếu được cấp quyền; action «Tạo ticket hỗ trợ Logistics nếu được cấp quyền» có success/deny/recovery states. Authorization: Không duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-030-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-038

REQ-V3-BC-038: Phê duyệt escalation theo quyền. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt escalation theo quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt escalation theo quyền; action «Phê duyệt escalation theo quyền» có success/deny/recovery states. Authorization: Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-038-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-083

REQ-V3-BC-083: Phản ánh đơn Merchant qua Merchant Support. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phản ánh đơn Merchant qua Merchant Support; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Verified Recipient channel — Phản ánh đơn Merchant qua Merchant Support; action «Phản ánh đơn Merchant qua Merchant Support» có success/deny/recovery states. Authorization: Merchant lập ticket với Logistics CSKH; không có Recipient Portal; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R10, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-083-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-089

REQ-V3-BC-089: Mở tra soát COD thiếu/phí sai. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Mở tra soát COD thiếu/phí sai; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Mở tra soát COD thiếu/phí sai; action «Mở tra soát COD thiếu/phí sai» có success/deny/recovery states. Authorization: Không tự duyệt hoàn tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-089-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-105

REQ-V3-BC-105: Nhận/tạo ticket từ Merchant hoặc người liên quan. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận/tạo ticket từ Merchant hoặc người liên quan; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận/tạo ticket từ Merchant hoặc người liên quan; action «Nhận/tạo ticket từ Merchant hoặc người liên quan» có success/deny/recovery states. Authorization: Phân loại nguồn; xác minh quyền thông tin; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-105-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-107

REQ-V3-BC-107: Phân loại, phân công và cập nhật SLA ticket. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phân loại, phân công và cập nhật SLA ticket; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phân loại, phân công và cập nhật SLA ticket; action «Phân loại, phân công và cập nhật SLA ticket» có success/deny/recovery states. Authorization: Lưu lịch sử thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-107-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-108

REQ-V3-BC-108: Gửi yêu cầu giao lại/đổi thông tin. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gửi yêu cầu giao lại/đổi thông tin; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Gửi yêu cầu giao lại/đổi thông tin; action «Gửi yêu cầu giao lại/đổi thông tin» có success/deny/recovery states. Authorization: Workflow phê duyệt; không tự sửa Shipment đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-108-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-109

REQ-V3-BC-109: Escalate vụ nghiêm trọng. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Escalate vụ nghiêm trọng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Escalate vụ nghiêm trọng; action «Escalate vụ nghiêm trọng» có success/deny/recovery states. Authorization: Không tự phê duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-109-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-110

REQ-V3-BC-110: Đa kênh hotline/email/chat tích hợp. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đa kênh hotline/email/chat tích hợp; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đa kênh hotline/email/chat tích hợp; action «Đa kênh hotline/email/chat tích hợp» có success/deny/recovery states. Authorization: Không cần xây portal Recipient; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-110-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-145

REQ-V3-BC-145: Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal — Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng; action «Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng» có success/deny/recovery states. Authorization: Chỉ Shipment thuộc mình, có ticket/evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-145-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-153

REQ-V3-BC-153: Nhận và xử lý ticket trực tiếp từ Retail Sender. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận và xử lý ticket trực tiếp từ Retail Sender; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận và xử lý ticket trực tiếp từ Retail Sender; action «Nhận và xử lý ticket trực tiếp từ Retail Sender» có success/deny/recovery states. Authorization: Xác minh Shipment thuộc Sender, tracking, timeline, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-153-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-154

REQ-V3-BC-154: Tiếp nhận phản ánh Recipient cho đơn Retail Sender. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận phản ánh Recipient cho đơn Retail Sender; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận phản ánh Recipient cho đơn Retail Sender; action «Tiếp nhận phản ánh Recipient cho đơn Retail Sender» có success/deny/recovery states. Authorization: Không giả định Merchant Support; xác minh tối thiểu; bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-154-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-155

REQ-V3-BC-155: Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant; action «Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant» có success/deny/recovery states. Authorization: Có thể ủy quyền Merchant Support khi Role được bổ sung; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-155-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-156

REQ-V3-BC-156: Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền; action «Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền» có success/deny/recovery states. Authorization: Merchant scope, không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-156-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-173

REQ-V3-BC-173: Trao đổi với hãng chuyển phát, mở/escalate ticket. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Trao đổi với hãng chuyển phát, mở/escalate ticket; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Trao đổi với hãng chuyển phát, mở/escalate ticket; action «Trao đổi với hãng chuyển phát, mở/escalate ticket» có success/deny/recovery states. Authorization: Không tự duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-173-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-191

REQ-V3-BC-191: Mở tra soát COD thiếu, phí sai và theo dõi kết quả. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Mở tra soát COD thiếu, phí sai và theo dõi kết quả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Mở tra soát COD thiếu, phí sai và theo dõi kết quả; action «Mở tra soát COD thiếu, phí sai và theo dõi kết quả» có success/deny/recovery states. Authorization: Không tự phê duyệt khoản hoàn/bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-191-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-195

REQ-V3-BC-195: Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment; action «Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment» có success/deny/recovery states. Authorization: Xác minh đơn/nguồn phản ánh; chống ticket trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-195-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-196

REQ-V3-BC-196: Gửi ticket sang E-Logistics Customer Service và theo dõi timeline. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Gửi ticket sang E-Logistics Customer Service và theo dõi timeline; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Gửi ticket sang E-Logistics Customer Service và theo dõi timeline; action «Gửi ticket sang E-Logistics Customer Service và theo dõi timeline» có success/deny/recovery states. Authorization: Có phân công, SLA, audit và bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-196-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-199

REQ-V3-BC-199: Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket; action «Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket» có success/deny/recovery states. Authorization: Không tự đóng ticket Logistics trái quy trình; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R21, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-199-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-249

REQ-V3-BC-249: Hỗ trợ sự cố giao nhận và escalations. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Hỗ trợ sự cố giao nhận và escalations; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Hỗ trợ sự cố giao nhận và escalations; action «Hỗ trợ sự cố giao nhận và escalations» có success/deny/recovery states. Authorization: Ghi lý do, liên kết ticket/incident; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-249-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-266

REQ-V3-BC-266: Tạo/điều phối báo cáo mất hoặc hỏng kiện. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tạo/điều phối báo cáo mất hoặc hỏng kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tạo/điều phối báo cáo mất hoặc hỏng kiện; action «Tạo/điều phối báo cáo mất hoặc hỏng kiện» có success/deny/recovery states. Authorization: Liên kết kiện và vị trí cuối, ticket/claim; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-266-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-292

REQ-V3-BC-292: Tra cứu Shipment theo ticket/phạm vi phân công. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tra cứu Shipment theo ticket/phạm vi phân công; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tra cứu Shipment theo ticket/phạm vi phân công; action «Tra cứu Shipment theo ticket/phạm vi phân công» có success/deny/recovery states. Authorization: PII tối thiểu, reason và access audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-292-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-294

REQ-V3-BC-294: Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc; action «Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc» có success/deny/recovery states. Authorization: Xác minh sở hữu; phê duyệt thay đổi theo state; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-294-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-296

REQ-V3-BC-296: Giám sát deadline phản hồi và giải quyết ticket. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát deadline phản hồi và giải quyết ticket; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát deadline phản hồi và giải quyết ticket; action «Giám sát deadline phản hồi và giải quyết ticket» có success/deny/recovery states. Authorization: SLA clock, escalation, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R14, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-296-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-307

REQ-V3-BC-307: Phê duyệt escalation nghiệp vụ theo hạn mức. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt escalation nghiệp vụ theo hạn mức; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt escalation nghiệp vụ theo hạn mức; action «Phê duyệt escalation nghiệp vụ theo hạn mức» có success/deny/recovery states. Authorization: Separation of duties, lý do và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-307-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-329

REQ-V3-BC-329: Quản lý Merchant/account được giao. Backend application boundary: merchant xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý Merchant/account được giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý Merchant/account được giao; action «Quản lý Merchant/account được giao» có success/deny/recovery states. Authorization: Không truy cập Merchant ngoài danh sách; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-329-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-330

REQ-V3-BC-330: Tư vấn dịch vụ và ghi nhận tương tác khách hàng. Backend application boundary: merchant xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tư vấn dịch vụ và ghi nhận tương tác khách hàng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tư vấn dịch vụ và ghi nhận tương tác khách hàng; action «Tư vấn dịch vụ và ghi nhận tương tác khách hàng» có success/deny/recovery states. Authorization: Không đọc PII Shipment không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-330-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-334

REQ-V3-BC-334: Phối hợp hỗ trợ Merchant lớn. Backend application boundary: orders xử lý ticket/assigned customer relationship/interaction; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phối hợp hỗ trợ Merchant lớn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource ticket/assigned customer relationship/interaction. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phối hợp hỗ trợ Merchant lớn; action «Phối hợp hỗ trợ Merchant lớn» có success/deny/recovery states. Authorization: Ticket ownership và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-334-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-SUPPORT

Trace REQ-AB-SUPPORT. Guest contact; personal/Merchant tickets; verified Recipient channel; BE/application: Guest anti-spam contact, customer-owned ticket history/SLA/escalation; Merchant shipment recipient via Merchant Support or authorized carrier CS, Retail recipient direct carrier CS; address/contact change request not direct state patch. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Identity/Shipment ownership verified; cross-tenant/case denied; external support channel no mandatory Recipient account; no ticket auto refunds/POD mutation; current guard và failure/replay/recovery, no mock-only success.
