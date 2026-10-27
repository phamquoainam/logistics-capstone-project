# Spec 014 — delivery-dispatch-and-route

Alias lịch sử logical `L014`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `dispatch`; từng BC giữ DML owner riêng.

## Outcome và boundary

Phân công giao và route có custody. Delivery pool/assignment; routing proposal/confirm giữ completed stops.

Workflow: Kiện đích hợp lệ→assign/load→route; race/revoked không tạo hai active. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L011, L013. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-14; DEC-17; DEC-18. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-048 — Xem delivery pool và phân công giao

Source: BC-048/R06/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Kiện đủ điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-048-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem delivery pool và phân công giao; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Kiện đủ điều kiện»; reload/API/PG assertion; TEST-V3-BC-048-FLOW.
- **AC-BC-048-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-048-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-050 — Điều phối giao lại

Source: BC-050/R06/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Delivery attempt và chính sách; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-050-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều phối giao lại; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Delivery attempt và chính sách»; reload/API/PG assertion; TEST-V3-BC-050-FLOW.
- **AC-BC-050-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-050-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-054 — Gom điểm/tối ưu tuyến

Source: BC-054/R06/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chưa yêu cầu routing optimization chuyên sâu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-054-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Gom điểm/tối ưu tuyến; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Chưa yêu cầu routing optimization chuyên sâu»; reload/API/PG assertion; TEST-V3-BC-054-FLOW.
- **AC-BC-054-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-054-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-071 — Xem và nhận kiện giao theo assignment

Source: BC-071/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ nhiệm vụ cá nhân; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-071-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem và nhận kiện giao theo assignment; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Chỉ nhiệm vụ cá nhân»; reload/API/PG assertion; TEST-V3-BC-071-FLOW.
- **AC-BC-071-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-071-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-072 — Ghi OUT_FOR_DELIVERY

Source: BC-072/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ khi kiện đã được bàn giao hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-072-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi OUT_FOR_DELIVERY; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Chỉ khi kiện đã được bàn giao hợp lệ»; reload/API/PG assertion; TEST-V3-BC-072-FLOW.
- **AC-BC-072-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-072-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-079 — Tuyến giao hàng tối ưu/GPS real-time

Source: BC-079/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không bắt buộc với MVP; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-079-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tuyến giao hàng tối ưu/GPS real-time; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không bắt buộc với MVP»; reload/API/PG assertion; TEST-V3-BC-079-FLOW.
- **AC-BC-079-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-079-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-119 — Giám sát pickup/courier thuộc Station

Source: BC-119/R16/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xác nhận PICKED_UP thay courier; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-119-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát pickup/courier thuộc Station; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không xác nhận PICKED_UP thay courier»; reload/API/PG assertion; TEST-V3-BC-119-FLOW.
- **AC-BC-119-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-119-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-120 — Giám sát giao thất bại và tồn phát

Source: BC-120/R16/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tạo POD giả; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-120-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát giao thất bại và tồn phát; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không tạo POD giả»; reload/API/PG assertion; TEST-V3-BC-120-FLOW.
- **AC-BC-120-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-120-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-171 — Yêu cầu xử lý giao thất bại và giao lại

Source: BC-171/R19/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tạo DELIVERED/POD thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `dispatch`.

- **AC-BC-171-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Yêu cầu xử lý giao thất bại và giao lại; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không tạo DELIVERED/POD thay Courier»; reload/API/PG assertion; TEST-V3-BC-171-FLOW.
- **AC-BC-171-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-171-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-177 — Gửi yêu cầu giao lại qua workflow theo quyền

Source: BC-177/R03/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự gán Courier hoặc POD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `dispatch`.

- **AC-BC-177-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Gửi yêu cầu giao lại qua workflow theo quyền; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không tự gán Courier hoặc POD»; reload/API/PG assertion; TEST-V3-BC-177-FLOW.
- **AC-BC-177-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-177-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-197 — Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát

Source: BC-197/R21/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không trực tiếp ghi trạng thái giao; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `dispatch`.

- **AC-BC-197-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không trực tiếp ghi trạng thái giao»; reload/API/PG assertion; TEST-V3-BC-197-FLOW.
- **AC-BC-197-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-197-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-208 — Phân công Delivery Courier theo khu vực, tuyến và năng lực

Source: BC-208/R06/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Kiện sẵn sàng giao, custody/scan và assignment hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-208-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phân công Delivery Courier theo khu vực, tuyến và năng lực; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Kiện sẵn sàng giao, custody/scan và assignment hợp lệ»; reload/API/PG assertion; TEST-V3-BC-208-FLOW.
- **AC-BC-208-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-208-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-222 — Quét barcode xác nhận nhận tải kiện từ bưu cục

Source: BC-222/R09/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Handover hai phía, đúng assignment, không nhận trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-222-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quét barcode xác nhận nhận tải kiện từ bưu cục; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Handover hai phía, đúng assignment, không nhận trùng»; reload/API/PG assertion; TEST-V3-BC-222-FLOW.
- **AC-BC-222-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-222-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-223 — Xem tuyến giao, bản đồ và điểm dừng

Source: BC-223/R09/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ nhiệm vụ được giao; địa chỉ và PII tối thiểu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-223-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem tuyến giao, bản đồ và điểm dừng; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Chỉ nhiệm vụ được giao»; reload/API/PG assertion; TEST-V3-BC-223-FLOW.
- **AC-BC-223-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-223-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-224 — Liên hệ Recipient theo chính sách bảo vệ số điện thoại

Source: BC-224/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ khi assignment hợp lệ; bảo vệ thông tin liên hệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-224-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Liên hệ Recipient theo chính sách bảo vệ số điện thoại; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Chỉ khi assignment hợp lệ»; reload/API/PG assertion; TEST-V3-BC-224-FLOW.
- **AC-BC-224-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-224-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-226 — Thực hiện giao lại theo assignment mới

Source: BC-226/R09/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Delivery attempt riêng; không tái sử dụng assignment hết hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-226-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Thực hiện giao lại theo assignment mới; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Delivery attempt riêng»; reload/API/PG assertion; TEST-V3-BC-226-FLOW.
- **AC-BC-226-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-226-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-245 — Xem nhiệm vụ Delivery Courier toàn đội

Source: BC-245/R24/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự có quyền giao thay Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-245-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem nhiệm vụ Delivery Courier toàn đội; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không tự có quyền giao thay Courier»; reload/API/PG assertion; TEST-V3-BC-245-FLOW.
- **AC-BC-245-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-245-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-248 — Đề xuất tái phân công delivery

Source: BC-248/R24/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự cấp assignment rồi tự ghi POD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-248-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đề xuất tái phân công delivery; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Không tự cấp assignment rồi tự ghi POD»; reload/API/PG assertion; TEST-V3-BC-248-FLOW.
- **AC-BC-248-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-248-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-274 — Theo dõi tỷ lệ giao thành công toàn mạng

Source: BC-274/R27/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: KPI từ attempts/tracking, không sửa POD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-274-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi tỷ lệ giao thành công toàn mạng; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «KPI từ attempts/tracking, không sửa POD»; reload/API/PG assertion; TEST-V3-BC-274-FLOW.
- **AC-BC-274-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-274-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-278 — Điều phối chuyển vùng phát trong ngoại lệ

Source: BC-278/R27/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tôn trọng state, active assignment và custody; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `dispatch`.

- **AC-BC-278-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều phối chuyển vùng phát trong ngoại lệ; Then outcome lưu/đọc đúng delivery request/assignment/route, giữ guard «Tôn trọng state, active assignment và custody»; reload/API/PG assertion; TEST-V3-BC-278-FLOW.
- **AC-BC-278-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-278-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-008-01 | Một leg tối đa 1 ASSIGNED/IN_PROGRESS active; reassign đóng old + tạo new atomically, reason/audit. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/008-dispatch-hub/spec.md) |
| REQ-009-01 | Chỉ xử lý đúng shipment/leg trong assignment active của shipper, revalidate tại server lúc ghi nhận. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |
| REQ-010-01 | Tuyến chỉ gồm nhiệm vụ đang hợp lệ, ràng buộc capacity/time windows/stop count được kiểm trước khi áp dụng. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) |
| REQ-010-02 | Routing không tự thay assignment hay trạng thái Shipment; dispatcher confirm route version và có history. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) |
| REQ-010-03 | GPS có recordedAt, receivedAt, accuracy, source, sessionId; chỉ ghi trong phiên và role hợp lệ. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) |
| REQ-010-04 | Mẫu vị trí stale/out-of-order không được trình bày là latest; ngừng nhận khi hết phiên/quyền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) |
| REQ-010-05 | So sánh baseline vs candidate trên cùng matrix/ràng buộc; infeasible trả nguyên nhân. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/010-routing-location/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.
