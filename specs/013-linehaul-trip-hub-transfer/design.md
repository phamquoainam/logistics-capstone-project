# Design 013 — linehaul-trip-hub-transfer

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Trip/leg/driver/seal/checklist/departure/arrival/delay.

FE: Driver trip/events; Station xuất/nhận; Network giám sát; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Dispatch trip/manifest/custody; routing chỉ đề xuất. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-070

REQ-V3-BC-070: Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub; action «Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub» có success/deny/recovery states. Authorization: Station xác nhận nhận/xuất manifest, không vận hành lái xe thay Line-haul Driver; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-070-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-221

REQ-V3-BC-221: Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub; action «Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub» có success/deny/recovery states. Authorization: Vai trò Station chỉ xác nhận custody đầu/cuối; Driver quản lý hành trình; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-221-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-231

REQ-V3-BC-231: Giám sát các chuyến xe đi/đến và bàn giao kiện. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát các chuyến xe đi/đến và bàn giao kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát các chuyến xe đi/đến và bàn giao kiện; action «Giám sát các chuyến xe đi/đến và bàn giao kiện» có success/deny/recovery states. Authorization: Không làm Driver giả, giám sát manifest/custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-231-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-234

REQ-V3-BC-234: Nhận chuyến xe được phân công. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận chuyến xe được phân công; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận chuyến xe được phân công; action «Nhận chuyến xe được phân công» có success/deny/recovery states. Authorization: Chỉ trip hợp lệ theo tuyến/driver, thời hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-234-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-235

REQ-V3-BC-235: Kiểm tra seal, tải trọng và trạng thái phương tiện. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra seal, tải trọng và trạng thái phương tiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra seal, tải trọng và trạng thái phương tiện; action «Kiểm tra seal, tải trọng và trạng thái phương tiện» có success/deny/recovery states. Authorization: Ghi checklist/evidence; bất thường phải xử lý trước departure; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-235-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-236

REQ-V3-BC-236: Nhận manifest kiện, bao tải và thông tin số lượng. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận manifest kiện, bao tải và thông tin số lượng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận manifest kiện, bao tải và thông tin số lượng; action «Nhận manifest kiện, bao tải và thông tin số lượng» có success/deny/recovery states. Authorization: Manifest phiên bản, cargo/trip relation rõ ràng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-236-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-237

REQ-V3-BC-237: Quét barcode và đối chiếu kiện/bao khi nhận tải. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quét barcode và đối chiếu kiện/bao khi nhận tải; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quét barcode và đối chiếu kiện/bao khi nhận tải; action «Quét barcode và đối chiếu kiện/bao khi nhận tải» có success/deny/recovery states. Authorization: Chặn kiện trùng/sai manifest; ghi discrepancy; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-237-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-238

REQ-V3-BC-238: Xác nhận bàn giao kiện tại điểm xuất phát. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận bàn giao kiện tại điểm xuất phát; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận bàn giao kiện tại điểm xuất phát; action «Xác nhận bàn giao kiện tại điểm xuất phát» có success/deny/recovery states. Authorization: Hai phía custody; timestamp, station/hub, actor; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-238-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-239

REQ-V3-BC-239: Ghi nhận hành trình và các mốc vận tải. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận hành trình và các mốc vận tải; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận hành trình và các mốc vận tải; action «Ghi nhận hành trình và các mốc vận tải» có success/deny/recovery states. Authorization: Trip events độc lập với shipment delivered; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-239-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-240

REQ-V3-BC-240: Báo trễ, sự cố hoặc thay đổi hành trình. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Báo trễ, sự cố hoặc thay đổi hành trình; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Báo trễ, sự cố hoặc thay đổi hành trình; action «Báo trễ, sự cố hoặc thay đổi hành trình» có success/deny/recovery states. Authorization: Lý do/evidence, escalation, SLA; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-240-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-241

REQ-V3-BC-241: Xác nhận đến Hub/Station đích. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận đến Hub/Station đích; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận đến Hub/Station đích; action «Xác nhận đến Hub/Station đích» có success/deny/recovery states. Authorization: Đúng trip/destination, thời gian và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-241-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-242

REQ-V3-BC-242: Bàn giao và đối chiếu số kiện thực nhận ở đích. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Bàn giao và đối chiếu số kiện thực nhận ở đích; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Bàn giao và đối chiếu số kiện thực nhận ở đích; action «Bàn giao và đối chiếu số kiện thực nhận ở đích» có success/deny/recovery states. Authorization: Đối chiếu manifest và bên nhận, tạo discrepancy khi lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R23, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-242-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-264

REQ-V3-BC-264: Theo dõi chuyến xe đến/đi Hub. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Theo dõi chuyến xe đến/đi Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Theo dõi chuyến xe đến/đi Hub; action «Theo dõi chuyến xe đến/đi Hub» có success/deny/recovery states. Authorization: Trip + manifest + ETA/delay consistency; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-264-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-267

REQ-V3-BC-267: Phối hợp điều chuyển hàng và ngoại lệ vận tải. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Phối hợp điều chuyển hàng và ngoại lệ vận tải; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Phối hợp điều chuyển hàng và ngoại lệ vận tải; action «Phối hợp điều chuyển hàng và ngoại lệ vận tải» có success/deny/recovery states. Authorization: Theo policy/coverage/capacity, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R26, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-267-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-270

REQ-V3-BC-270: Quản lý mạng lưới Station và Hub theo vùng/toàn mạng. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý mạng lưới Station và Hub theo vùng/toàn mạng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý mạng lưới Station và Hub theo vùng/toàn mạng; action «Quản lý mạng lưới Station và Hub theo vùng/toàn mạng» có success/deny/recovery states. Authorization: Đúng scope, không tự vượt quyền identity; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-270-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-272

REQ-V3-BC-272: Giám sát năng lực tuyến và vận tải. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát năng lực tuyến và vận tải; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát năng lực tuyến và vận tải; action «Giám sát năng lực tuyến và vận tải» có success/deny/recovery states. Authorization: Dữ liệu theo network region; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-272-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-273

REQ-V3-BC-273: Điều chuyển hàng liên vùng. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều chuyển hàng liên vùng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều chuyển hàng liên vùng; action «Điều chuyển hàng liên vùng» có success/deny/recovery states. Authorization: Phê duyệt theo policy, manifest/custody liên tục; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-273-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-276

REQ-V3-BC-276: Điều chỉnh tuyến ngoại lệ theo chính sách. Backend application boundary: dispatch xử lý trip/seal/manifest/custody; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều chỉnh tuyến ngoại lệ theo chính sách; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource trip/seal/manifest/custody. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều chỉnh tuyến ngoại lệ theo chính sách; action «Điều chỉnh tuyến ngoại lệ theo chính sách» có success/deny/recovery states. Authorization: Reason/evidence/approval/audit; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R27, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-276-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).
