# Design 026 — risk-fraud-compliance

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Guard trước L007; full rules/case/approval đọc finance/identity qua API duyệt.

FE: Risk case/evidence/propose hold/release approval, minimal PII; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Orders hold/case proposal; không sửa source finance/custody/identity. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-337

REQ-V3-BC-337: Điều tra dấu hiệu gian lận COD. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra dấu hiệu gian lận COD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra dấu hiệu gian lận COD; action «Điều tra dấu hiệu gian lận COD» có success/deny/recovery states. Authorization: Read theo case và dấu vết tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-337-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-338

REQ-V3-BC-338: Phát hiện và điều tra tài khoản sử dụng trái phép. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phát hiện và điều tra tài khoản sử dụng trái phép; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phát hiện và điều tra tài khoản sử dụng trái phép; action «Phát hiện và điều tra tài khoản sử dụng trái phép» có success/deny/recovery states. Authorization: Không tự cấp quyền mới; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-338-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-339

REQ-V3-BC-339: Phát hiện spam hoặc tạo Shipment hàng loạt bất thường. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phát hiện spam hoặc tạo Shipment hàng loạt bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phát hiện spam hoặc tạo Shipment hàng loạt bất thường; action «Phát hiện spam hoặc tạo Shipment hàng loạt bất thường» có success/deny/recovery states. Authorization: Rate, fingerprint, scope, false positives; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-339-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-340

REQ-V3-BC-340: Kiểm tra thông tin Recipient có dấu hiệu giả. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra thông tin Recipient có dấu hiệu giả; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra thông tin Recipient có dấu hiệu giả; action «Kiểm tra thông tin Recipient có dấu hiệu giả» có success/deny/recovery states. Authorization: PII minimization, authorization; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-340-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-341

REQ-V3-BC-341: Đánh dấu kiện có dấu hiệu rủi ro. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đánh dấu kiện có dấu hiệu rủi ro; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đánh dấu kiện có dấu hiệu rủi ro; action «Đánh dấu kiện có dấu hiệu rủi ro» có success/deny/recovery states. Authorization: Case, evidence và custody không bị sửa; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-341-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-342

REQ-V3-BC-342: Điều tra truy cập dữ liệu trái phép. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra truy cập dữ liệu trái phép; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra truy cập dữ liệu trái phép; action «Điều tra truy cập dữ liệu trái phép» có success/deny/recovery states. Authorization: Audit bất biến, nguyên tắc tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-342-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-343

REQ-V3-BC-343: Đề xuất tạm giữ Shipment theo chính sách. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đề xuất tạm giữ Shipment theo chính sách; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đề xuất tạm giữ Shipment theo chính sách; action «Đề xuất tạm giữ Shipment theo chính sách» có success/deny/recovery states. Authorization: Không thay đổi silent status; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-343-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-344

REQ-V3-BC-344: Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt; action «Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt» có success/deny/recovery states. Authorization: Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-344-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-345

REQ-V3-BC-345: Phê duyệt giải phóng Shipment theo thẩm quyền. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phê duyệt giải phóng Shipment theo thẩm quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phê duyệt giải phóng Shipment theo thẩm quyền; action «Phê duyệt giải phóng Shipment theo thẩm quyền» có success/deny/recovery states. Authorization: Dual control/hạn mức khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-345-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-346

REQ-V3-BC-346: Xem audit và giao dịch bất thường theo case. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem audit và giao dịch bất thường theo case; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem audit và giao dịch bất thường theo case; action «Xem audit và giao dịch bất thường theo case» có success/deny/recovery states. Authorization: Mọi lần truy cập nhạy cảm có log; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-346-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-347

REQ-V3-BC-347: Điều tra giao dịch tài chính bất thường. Backend application boundary: orders xử lý risk/hold case; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều tra giao dịch tài chính bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource risk/hold case. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều tra giao dịch tài chính bất thường; action «Điều tra giao dịch tài chính bất thường» có success/deny/recovery states. Authorization: Không tự sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R30, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-347-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
