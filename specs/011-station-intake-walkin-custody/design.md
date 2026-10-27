# Design 011 — station-intake-walkin-custody

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Station custody/manifest/tồn kiện; counter booking/receipt/fee qua workflow; Station master và staff/shift ownership dispatch; Identity chỉ credential/security; minimum authority contract trước staff use.

FE: Station intake/counter/variance; Manager tồn/ca; receipt cho khách; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Dispatch custody, orders shipment, finance receipt; không SKU balance thay custody. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-055

REQ-V3-BC-055: Điều phối qua nhiều Station. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều phối qua nhiều Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Điều phối qua nhiều Station; action «Điều phối qua nhiều Station» có success/deny/recovery states. Authorization: Policy phạm vi bưu cục; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R06, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-055-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-059

REQ-V3-BC-059: Bàn giao kiện cho Station. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Bàn giao kiện cho Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Bàn giao kiện cho Station; action «Bàn giao kiện cho Station» có success/deny/recovery states. Authorization: Bên nhận xác nhận custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R07, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-059-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-063

REQ-V3-BC-063: Nhận kiện và xác nhận custody tại Station. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận kiện và xác nhận custody tại Station; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận kiện và xác nhận custody tại Station; action «Nhận kiện và xác nhận custody tại Station» có success/deny/recovery states. Authorization: Scan, người nhận, timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-063-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-065

REQ-V3-BC-065: Nhập/xuất kiện và lập manifest. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhập/xuất kiện và lập manifest; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhập/xuất kiện và lập manifest; action «Nhập/xuất kiện và lập manifest» có success/deny/recovery states. Authorization: Không ghi đè lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-065-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-066

REQ-V3-BC-066: Bàn giao kiện cho courier/tuyến. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Bàn giao kiện cho courier/tuyến; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Bàn giao kiện cho courier/tuyến; action «Bàn giao kiện cho courier/tuyến» có success/deny/recovery states. Authorization: Hai phía xác nhận khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-066-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-069

REQ-V3-BC-069: Kiểm kê tồn chi tiết, phân loại tự động. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm kê tồn chi tiết, phân loại tự động; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm kê tồn chi tiết, phân loại tự động; action «Kiểm kê tồn chi tiết, phân loại tự động» có success/deny/recovery states. Authorization: Có quy trình chênh lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-069-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-118

REQ-V3-BC-118: Giám sát tồn kiện, nhập/xuất, manifest. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Giám sát tồn kiện, nhập/xuất, manifest; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Giám sát tồn kiện, nhập/xuất, manifest; action «Giám sát tồn kiện, nhập/xuất, manifest» có success/deny/recovery states. Authorization: Đúng Station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-118-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-123

REQ-V3-BC-123: Duyệt sai lệch kho có bằng chứng. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Duyệt sai lệch kho có bằng chứng; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Duyệt sai lệch kho có bằng chứng; action «Duyệt sai lệch kho có bằng chứng» có success/deny/recovery states. Authorization: Có audit/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-123-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-149

REQ-V3-BC-149: Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender; action «Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender» có success/deny/recovery states. Authorization: Chỉ nhân viên được phân công, xác minh người gửi; KHÔNG phải Guest API; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-149-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-152

REQ-V3-BC-152: Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch; action «Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch» có success/deny/recovery states. Authorization: Đúng Station, nguồn walk-in, manifest/handover; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-152-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-216

REQ-V3-BC-216: Nhận kiện trực tiếp từ khách walk-in hoặc Courier. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận kiện trực tiếp từ khách walk-in hoặc Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Nhận kiện trực tiếp từ khách walk-in hoặc Courier; action «Nhận kiện trực tiếp từ khách walk-in hoặc Courier» có success/deny/recovery states. Authorization: Phân biệt walk-in/assigned handover; actor, thời điểm, số lượng, chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-216-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-217

REQ-V3-BC-217: Xác nhận kiện thực nhận theo manifest và kiểm đếm. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận kiện thực nhận theo manifest và kiểm đếm; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận kiện thực nhận theo manifest và kiểm đếm; action «Xác nhận kiện thực nhận theo manifest và kiểm đếm» có success/deny/recovery states. Authorization: Lệch manifest mở exception, không mặc định khớp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-217-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-219

REQ-V3-BC-219: Ghi nhận tồn kho và vị trí vật lý của kiện. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận tồn kho và vị trí vật lý của kiện; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận tồn kho và vị trí vật lý của kiện; action «Ghi nhận tồn kho và vị trí vật lý của kiện» có success/deny/recovery states. Authorization: Station custody và location event bất biến; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-219-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-220

REQ-V3-BC-220: Xử lý sai lệch, tồn lâu và kiện không xác định. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xử lý sai lệch, tồn lâu và kiện không xác định; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xử lý sai lệch, tồn lâu và kiện không xác định; action «Xử lý sai lệch, tồn lâu và kiện không xác định» có success/deny/recovery states. Authorization: Lý do, evidence và escalation; không âm thầm sửa kho; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R08, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-220-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-228

REQ-V3-BC-228: Bàn giao kiện không giao được về Station/Hub. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Bàn giao kiện không giao được về Station/Hub; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Bàn giao kiện không giao được về Station/Hub; action «Bàn giao kiện không giao được về Station/Hub» có success/deny/recovery states. Authorization: Custody giao hai phía, xác nhận thực nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-228-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-230

REQ-V3-BC-230: Quản lý ca làm và nhân sự Station trong quyền được ủy quyền. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý ca làm và nhân sự Station trong quyền được ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản lý ca làm và nhân sự Station trong quyền được ủy quyền; action «Quản lý ca làm và nhân sự Station trong quyền được ủy quyền» có success/deny/recovery states. Authorization: Không tự cấp global/system admin hoặc quyền ngoài Station; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-230-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-232

REQ-V3-BC-232: Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow; action «Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow» có success/deny/recovery states. Authorization: Case, bằng chứng, phê duyệt khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R16, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-232-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-250

REQ-V3-BC-250: Kiểm tra kiện tồn và bàn giao bất thường. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Kiểm tra kiện tồn và bàn giao bất thường; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Kiểm tra kiện tồn và bàn giao bất thường; action «Kiểm tra kiện tồn và bàn giao bất thường» có success/deny/recovery states. Authorization: Chỉ vị trí/đội được phân công, có đối chiếu custody; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-250-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-251

REQ-V3-BC-251: Xác nhận chênh lệch bàn giao theo quyền. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Xác nhận chênh lệch bàn giao theo quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Xác nhận chênh lệch bàn giao theo quyền; action «Xác nhận chênh lệch bàn giao theo quyền» có success/deny/recovery states. Authorization: Evidence, audit, phê duyệt riêng với mất mát; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R24, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-251-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-305

REQ-V3-BC-305: Quản trị cơ cấu Station/Hub/khu vực theo phạm vi. Backend application boundary: dispatch xử lý Station/shift/staff/custody/manifest; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản trị cơ cấu Station/Hub/khu vực theo phạm vi; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: dispatch; resource Station/shift/staff/custody/manifest. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Quản trị cơ cấu Station/Hub/khu vực theo phạm vi; action «Quản trị cơ cấu Station/Hub/khu vực theo phạm vi» có success/deny/recovery states. Authorization: Không sửa custody đã xảy ra; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-305-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-STATION-STAFF

Trace REQ-AB-STATION-STAFF. Station admin/staff/shift and published service points; BE/application: dispatch owns Station master/shift/staff assignments; Identity credential/security unchanged; address004 reads published projection; last staff/shift conflicts policy reviewed. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Merchant cannot DML Station by Owner role; wrong Station/delegation/revoke deny; shift changes audit/version and do not change historical custody; current guard và failure/replay/recovery, no mock-only success.
