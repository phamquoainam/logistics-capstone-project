# Spec 011 — station-intake-walkin-custody

Alias lịch sử logical `L011`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `dispatch`; từng BC giữ DML owner riêng.

## Outcome và boundary

Station intake và staffed walk-in. Station custody/manifest/tồn kiện; counter booking/receipt/fee qua workflow; Station master và staff/shift ownership dispatch; Identity chỉ credential/security; minimum authority contract trước staff use.

Workflow: Staff verify→kiểm kiện→quote/pay/receipt→intake; lệch manifest mở exception. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L007, L008, L009, L010, L018. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-05; D-V3-04; DEC-14. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-055 — Điều phối qua nhiều Station

Source: BC-055/R06/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Policy phạm vi bưu cục; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-055-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều phối qua nhiều Station; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Policy phạm vi bưu cục»; reload/API/PG assertion; TEST-V3-BC-055-FLOW.
- **AC-BC-055-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-055-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-059 — Bàn giao kiện cho Station

Source: BC-059/R07/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Bên nhận xác nhận custody; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-059-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Bàn giao kiện cho Station; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Bên nhận xác nhận custody»; reload/API/PG assertion; TEST-V3-BC-059-FLOW.
- **AC-BC-059-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-059-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-063 — Nhận kiện và xác nhận custody tại Station

Source: BC-063/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Scan, người nhận, timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-063-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhận kiện và xác nhận custody tại Station; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Scan, người nhận, timestamp»; reload/API/PG assertion; TEST-V3-BC-063-FLOW.
- **AC-BC-063-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-063-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-065 — Nhập/xuất kiện và lập manifest

Source: BC-065/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không ghi đè lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-065-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhập/xuất kiện và lập manifest; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Không ghi đè lịch sử»; reload/API/PG assertion; TEST-V3-BC-065-FLOW.
- **AC-BC-065-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-065-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-066 — Bàn giao kiện cho courier/tuyến

Source: BC-066/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hai phía xác nhận khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-066-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Bàn giao kiện cho courier/tuyến; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Hai phía xác nhận khi cần»; reload/API/PG assertion; TEST-V3-BC-066-FLOW.
- **AC-BC-066-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-066-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-069 — Kiểm kê tồn chi tiết, phân loại tự động

Source: BC-069/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có quy trình chênh lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-069-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm kê tồn chi tiết, phân loại tự động; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Có quy trình chênh lệch»; reload/API/PG assertion; TEST-V3-BC-069-FLOW.
- **AC-BC-069-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-069-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-118 — Giám sát tồn kiện, nhập/xuất, manifest

Source: BC-118/R16/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng Station scope; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-118-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát tồn kiện, nhập/xuất, manifest; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Đúng Station scope»; reload/API/PG assertion; TEST-V3-BC-118-FLOW.
- **AC-BC-118-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-118-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-123 — Duyệt sai lệch kho có bằng chứng

Source: BC-123/R16/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có audit/approval; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-123-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Duyệt sai lệch kho có bằng chứng; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Có audit/approval»; reload/API/PG assertion; TEST-V3-BC-123-FLOW.
- **AC-BC-123-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-123-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-149 — Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender

Source: BC-149/R08/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ nhân viên được phân công, xác minh người gửi; KHÔNG phải Guest API; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-149-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Chỉ nhân viên được phân công, xác minh người gửi»; reload/API/PG assertion; TEST-V3-BC-149-FLOW.
- **AC-BC-149-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-149-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-152 — Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch

Source: BC-152/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng Station, nguồn walk-in, manifest/handover; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-152-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Đúng Station, nguồn walk-in, manifest/handover»; reload/API/PG assertion; TEST-V3-BC-152-FLOW.
- **AC-BC-152-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-152-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-216 — Nhận kiện trực tiếp từ khách walk-in hoặc Courier

Source: BC-216/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Phân biệt walk-in/assigned handover; actor, thời điểm, số lượng, chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-216-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhận kiện trực tiếp từ khách walk-in hoặc Courier; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Phân biệt walk-in/assigned handover»; reload/API/PG assertion; TEST-V3-BC-216-FLOW.
- **AC-BC-216-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-216-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-217 — Xác nhận kiện thực nhận theo manifest và kiểm đếm

Source: BC-217/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Lệch manifest mở exception, không mặc định khớp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-217-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác nhận kiện thực nhận theo manifest và kiểm đếm; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Lệch manifest mở exception, không mặc định khớp»; reload/API/PG assertion; TEST-V3-BC-217-FLOW.
- **AC-BC-217-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-217-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-219 — Ghi nhận tồn kho và vị trí vật lý của kiện

Source: BC-219/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Station custody và location event bất biến; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-219-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận tồn kho và vị trí vật lý của kiện; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Station custody và location event bất biến»; reload/API/PG assertion; TEST-V3-BC-219-FLOW.
- **AC-BC-219-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-219-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-220 — Xử lý sai lệch, tồn lâu và kiện không xác định

Source: BC-220/R08/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Lý do, evidence và escalation; không âm thầm sửa kho; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-220-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xử lý sai lệch, tồn lâu và kiện không xác định; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Lý do, evidence và escalation»; reload/API/PG assertion; TEST-V3-BC-220-FLOW.
- **AC-BC-220-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-220-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-228 — Bàn giao kiện không giao được về Station/Hub

Source: BC-228/R09/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Custody giao hai phía, xác nhận thực nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-228-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Bàn giao kiện không giao được về Station/Hub; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Custody giao hai phía, xác nhận thực nhận»; reload/API/PG assertion; TEST-V3-BC-228-FLOW.
- **AC-BC-228-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-228-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-230 — Quản lý ca làm và nhân sự Station trong quyền được ủy quyền

Source: BC-230/R16/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự cấp global/system admin hoặc quyền ngoài Station; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-230-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý ca làm và nhân sự Station trong quyền được ủy quyền; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Không tự cấp global/system admin hoặc quyền ngoài Station»; reload/API/PG assertion; TEST-V3-BC-230-FLOW.
- **AC-BC-230-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-230-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-232 — Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow

Source: BC-232/R16/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Case, bằng chứng, phê duyệt khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-232-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Case, bằng chứng, phê duyệt khi cần»; reload/API/PG assertion; TEST-V3-BC-232-FLOW.
- **AC-BC-232-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-232-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-250 — Kiểm tra kiện tồn và bàn giao bất thường

Source: BC-250/R24/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ vị trí/đội được phân công, có đối chiếu custody; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-250-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm tra kiện tồn và bàn giao bất thường; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Chỉ vị trí/đội được phân công, có đối chiếu custody»; reload/API/PG assertion; TEST-V3-BC-250-FLOW.
- **AC-BC-250-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-250-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-251 — Xác nhận chênh lệch bàn giao theo quyền

Source: BC-251/R24/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Evidence, audit, phê duyệt riêng với mất mát; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-251-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác nhận chênh lệch bàn giao theo quyền; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Evidence, audit, phê duyệt riêng với mất mát»; reload/API/PG assertion; TEST-V3-BC-251-FLOW.
- **AC-BC-251-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-251-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-305 — Quản trị cơ cấu Station/Hub/khu vực theo phạm vi

Source: BC-305/R04/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa custody đã xảy ra; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-305-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản trị cơ cấu Station/Hub/khu vực theo phạm vi; Then outcome lưu/đọc đúng Station/shift/staff/custody/manifest, giữ guard «Không sửa custody đã xảy ra»; reload/API/PG assertion; TEST-V3-BC-305-FLOW.
- **AC-BC-305-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-305-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-008-02 | Manifest và scan đối chiếu parcel, hub, leg, chain-of-custody và idempotency key. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-008-03 | WMS outbound không đồng nghĩa hub inbound; mốc bàn giao cần chứng từ riêng. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-008-04 | Shipper chỉ scan task được phân công; các kiện sai hub/sai manifest vào exception có người phụ trách. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-008-05 | Hub backlog threshold config và cảnh báo, không tự đổi trạng thái shipment ngoài Order public contract. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-STATION-STAFF

Owner target: L011; source BC-230;BC-305; latest Owner portal scope, no new BC ID. UI/channel: Station admin/staff/shift and published service points.

Required behavior: dispatch owns Station master/shift/staff assignments; Identity credential/security unchanged; address004 reads published projection; last staff/shift conflicts policy reviewed. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-STATION-STAFF:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Merchant cannot DML Station by Owner role; wrong Station/delegation/revoke deny; shift changes audit/version and do not change historical custody. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-STATION-STAFF PLANNED_NOT_EXECUTED. Dependencies: L002;L004;L010.
