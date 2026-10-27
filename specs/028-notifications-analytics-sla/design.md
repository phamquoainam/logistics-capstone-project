# Design 028 — notifications-analytics-sla

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Outbox/inbox/templates/scoped projection/retry/DLQ; minimum notification first use; personal/Merchant/team dashboards và notifications; KPI source facts, declared freshness, no fabricated counts.

FE: Role dashboards/inbox/SLA freshness/lag/export theo scope; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Notification/inbox ở notification; không duplicate source ledger/event. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-017

REQ-V3-BC-017: Xem KPI, báo cáo chi phí, tỷ lệ giao. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem KPI, báo cáo chi phí, tỷ lệ giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem KPI, báo cáo chi phí, tỷ lệ giao; action «Xem KPI, báo cáo chi phí, tỷ lệ giao» có success/deny/recovery states. Authorization: Chỉ dữ liệu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-017-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-035

REQ-V3-BC-035: Theo dõi SLA và báo cáo vận hành. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi SLA và báo cáo vận hành; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi SLA và báo cáo vận hành; action «Theo dõi SLA và báo cáo vận hành» có success/deny/recovery states. Authorization: Chỉ phạm vi được phép; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-035-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-053

REQ-V3-BC-053: Theo dõi SLA, cảnh báo quá hạn. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi SLA, cảnh báo quá hạn; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi SLA, cảnh báo quá hạn; action «Theo dõi SLA, cảnh báo quá hạn» có success/deny/recovery states. Authorization: Không tự sửa tracking; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-053-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-061

REQ-V3-BC-061: Xem KPI cá nhân. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem KPI cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem KPI cá nhân; action «Xem KPI cá nhân» có success/deny/recovery states. Authorization: Không có quyền giám sát toàn đội; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-061-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-090

REQ-V3-BC-090: Xuất báo cáo COD, phí. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xuất báo cáo COD, phí; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xuất báo cáo COD, phí; action «Xuất báo cáo COD, phí» có success/deny/recovery states. Authorization: Đúng phạm vi Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-090-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-122

REQ-V3-BC-122: Theo dõi KPI/SLA và năng lực ca. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi KPI/SLA và năng lực ca; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi KPI/SLA và năng lực ca; action «Theo dõi KPI/SLA và năng lực ca» có success/deny/recovery states. Authorization: Đúng trạm; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-122-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-130

REQ-V3-BC-130: Xuất báo cáo audit. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xuất báo cáo audit; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xuất báo cáo audit; action «Xuất báo cáo audit» có success/deny/recovery states. Authorization: Bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R17, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-130-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-165

REQ-V3-BC-165: Xem báo cáo COD, giao thành công, đơn hoàn, chi phí. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem báo cáo COD, giao thành công, đơn hoàn, chi phí; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xem báo cáo COD, giao thành công, đơn hoàn, chi phí; action «Xem báo cáo COD, giao thành công, đơn hoàn, chi phí» có success/deny/recovery states. Authorization: Dữ liệu Merchant; không lộ tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-165-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-172

REQ-V3-BC-172: Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao; action «Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao» có success/deny/recovery states. Authorization: Không xem KPI Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-172-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-192

REQ-V3-BC-192: Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh; action «Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh» có success/deny/recovery states. Authorization: Giới hạn Merchant/branch scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R11, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-192-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-209

REQ-V3-BC-209: Giám sát tỷ lệ pickup/delivery thành công và tải công việc. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát tỷ lệ pickup/delivery thành công và tải công việc; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát tỷ lệ pickup/delivery thành công và tải công việc; action «Giám sát tỷ lệ pickup/delivery thành công và tải công việc» có success/deny/recovery states. Authorization: KPI có định nghĩa, dữ liệu trong vùng được phân quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-209-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-215

REQ-V3-BC-215: Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại; action «Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại» có success/deny/recovery states. Authorization: Chỉ hiệu suất cá nhân; dựa trên event và attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-215-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-229

REQ-V3-BC-229: Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp; action «Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp» có success/deny/recovery states. Authorization: Chỉ dữ liệu cá nhân; COD chưa nộp không được coi là remitted; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-229-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-246

REQ-V3-BC-246: Theo dõi hiệu suất và năng suất đội giao nhận. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi hiệu suất và năng suất đội giao nhận; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi hiệu suất và năng suất đội giao nhận; action «Theo dõi hiệu suất và năng suất đội giao nhận» có success/deny/recovery states. Authorization: Dữ liệu đúng team/scope và nguồn sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-246-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-268

REQ-V3-BC-268: Theo dõi KPI, throughput và thời gian tồn Hub. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi KPI, throughput và thời gian tồn Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi KPI, throughput và thời gian tồn Hub; action «Theo dõi KPI, throughput và thời gian tồn Hub» có success/deny/recovery states. Authorization: Chỉ số định nghĩa và provenance; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-268-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-275

REQ-V3-BC-275: Theo dõi SLA, cảnh báo và năng suất mạng lưới. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi SLA, cảnh báo và năng suất mạng lưới; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi SLA, cảnh báo và năng suất mạng lưới; action «Theo dõi SLA, cảnh báo và năng suất mạng lưới» có success/deny/recovery states. Authorization: Định nghĩa thống nhất, phân quyền dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-275-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-291

REQ-V3-BC-291: Xuất báo cáo công nợ và COD tồn chưa đối soát. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xuất báo cáo công nợ và COD tồn chưa đối soát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xuất báo cáo công nợ và COD tồn chưa đối soát; action «Xuất báo cáo công nợ và COD tồn chưa đối soát» có success/deny/recovery states. Authorization: Theo kỳ, có nguồn bút toán; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R13, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-291-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-333

REQ-V3-BC-333: Theo dõi KPI và chỉ tiêu kinh doanh. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi KPI và chỉ tiêu kinh doanh; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi KPI và chỉ tiêu kinh doanh; action «Theo dõi KPI và chỉ tiêu kinh doanh» có success/deny/recovery states. Authorization: Dữ liệu theo account/region scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R29, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-333-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-356

REQ-V3-BC-356: Kiểm tra sức khỏe hệ thống và tín hiệu vận hành. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra sức khỏe hệ thống và tín hiệu vận hành; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra sức khỏe hệ thống và tín hiệu vận hành; action «Kiểm tra sức khỏe hệ thống và tín hiệu vận hành» có success/deny/recovery states. Authorization: Chỉ telemetry cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-356-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-359

REQ-V3-BC-359: Giám sát hàng đợi, dead-letter và backlog. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát hàng đợi, dead-letter và backlog; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát hàng đợi, dead-letter và backlog; action «Giám sát hàng đợi, dead-letter và backlog» có success/deny/recovery states. Authorization: Có hạn mức retry và cảnh báo; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-359-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-363

REQ-V3-BC-363: Khắc phục lỗi phát thông báo/tracking event. Backend application boundary: notification xử lý notification/inbox/scoped KPI projection; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Khắc phục lỗi phát thông báo/tracking event; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: notification; resource notification/inbox/scoped KPI projection. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Khắc phục lỗi phát thông báo/tracking event; action «Khắc phục lỗi phát thông báo/tracking event» có success/deny/recovery states. Authorization: Không giả mạo sự kiện giao hàng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R32, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-363-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-PERSONAL-DASH

Trace REQ-AB-PERSONAL-DASH. Personal dashboard; BE/application: Owned shipment stats/date range/status summaries/trend/top-volume if meaningful, safe notifications, global scoped search; metric definitions/source/as-of/lag explicit. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Counters and chart tie to eligible owned source rows and timestamps; empty data zero/empty not fabricated; foreign shipment search denied; current guard và failure/replay/recovery, no mock-only success.


## DES-AB-NOTIFICATIONS

Trace REQ-AB-NOTIFICATIONS. Personal/Merchant inbox; verified Recipient channel; BE/application: Post-commit source events, dedup per subject/purpose; safe content and authorized deep links; recipients scoped external channel not IAM. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Duplicate/reorder/revoked recipient no leak/double effect; mark-read only owned; failure retained with bounded retry; current guard và failure/replay/recovery, no mock-only success.


## DES-AB-PORTAL-SHELL

Trace REQ-AB-PORTAL-SHELL. Shared customer/Merchant shell; BE/application: Persistent sidebar/topbar/breadcrumb/context; responsive and keyboard; menu capability/spec/delivery status mapping; actual backend gates. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Each visible interactive action calls supported API or deliberate disabled availability explanation; no mock-only dashboards; no privileged menu by role label alone; current guard và failure/replay/recovery, no mock-only success.
