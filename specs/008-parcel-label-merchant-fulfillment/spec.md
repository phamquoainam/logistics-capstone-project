# Spec 008 — parcel-label-merchant-fulfillment

Alias lịch sử logical `L008`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `warehouse`; từng BC giữ DML owner riêng.

## Outcome và boundary

Chuẩn bị SKU/kiện và nhãn có kiểm đếm. Warehouse stock/reservation/pick-pack; orders Parcel/label qua API.

Workflow: Reserve→pick/pack/count→label→handover; không giả Courier pickup. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L006, L007. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-05; DEC-13. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-026 — Tạo nhãn và danh sách bàn giao

Source: BC-026/R03/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Barcode liên kết kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-026-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo nhãn và danh sách bàn giao; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Barcode liên kết kiện»; reload/API/PG assertion; TEST-V3-BC-026-FLOW.
- **AC-BC-026-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-026-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-064 — Kiểm tra barcode/cân đo kiện

Source: BC-064/R08/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ghi nhận sai lệch có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-064-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm tra barcode/cân đo kiện; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Ghi nhận sai lệch có chứng cứ»; reload/API/PG assertion; TEST-V3-BC-064-FLOW.
- **AC-BC-064-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-064-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-140 — Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi

Source: BC-140/R18/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Parcel khác Shipment, thông tin hàng hóa hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-140-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Parcel khác Shipment, thông tin hàng hóa hợp lệ»; reload/API/PG assertion; TEST-V3-BC-140-FLOW.
- **AC-BC-140-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-140-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-150 — Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in

Source: BC-150/R08/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tạo Parcel, timestamp, bằng chứng nhận kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-150-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Tạo Parcel, timestamp, bằng chứng nhận kiện»; reload/API/PG assertion; TEST-V3-BC-150-FLOW.
- **AC-BC-150-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-150-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-176 — Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier

Source: BC-176/R03/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Quét/đối chiếu kiện; không tự tạo pickup evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-176-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Quét/đối chiếu kiện»; reload/API/PG assertion; TEST-V3-BC-176-FLOW.
- **AC-BC-176-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-176-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-179 — Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện

Source: BC-179/R20/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ đơn thuộc kho/nhân sự được phân công; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `warehouse`.

- **AC-BC-179-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Chỉ đơn thuộc kho/nhân sự được phân công»; reload/API/PG assertion; TEST-V3-BC-179-FLOW.
- **AC-BC-179-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-179-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-180 — Lấy hàng, kiểm đếm và ghi số lượng thực tế

Source: BC-180/R20/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Order, Shipment, Parcel tách biệt; có sai lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `warehouse`.

- **AC-BC-180-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Lấy hàng, kiểm đếm và ghi số lượng thực tế; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Order, Shipment, Parcel tách biệt»; reload/API/PG assertion; TEST-V3-BC-180-FLOW.
- **AC-BC-180-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-180-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-181 — Đóng gói và cân đo kiện, ghi kích thước thực tế

Source: BC-181/R20/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Snapshot measurement; không tự xác nhận phí chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-181-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Đóng gói và cân đo kiện, ghi kích thước thực tế; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Snapshot measurement»; reload/API/PG assertion; TEST-V3-BC-181-FLOW.
- **AC-BC-181-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-181-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-182 — Dán nhãn, quét barcode và đối chiếu Parcel

Source: BC-182/R20/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tạo barcode trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-182-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Dán nhãn, quét barcode và đối chiếu Parcel; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Không tạo barcode trùng»; reload/API/PG assertion; TEST-V3-BC-182-FLOW.
- **AC-BC-182-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-182-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-183 — Lập manifest/danh sách kiện bàn giao Pickup Courier

Source: BC-183/R20/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Bằng chứng số kiện, thời gian và người bàn giao; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `warehouse`.

- **AC-BC-183-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Lập manifest/danh sách kiện bàn giao Pickup Courier; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Bằng chứng số kiện, thời gian và người bàn giao»; reload/API/PG assertion; TEST-V3-BC-183-FLOW.
- **AC-BC-183-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-183-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-184 — Ghi nhận thiếu/sai hàng hoặc bao bì bất thường

Source: BC-184/R20/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Evidence, lý do và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `warehouse`.

- **AC-BC-184-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Ghi nhận thiếu/sai hàng hoặc bao bì bất thường; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Evidence, lý do và escalation»; reload/API/PG assertion; TEST-V3-BC-184-FLOW.
- **AC-BC-184-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-184-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-185 — Xác nhận Courier tới lấy và phối hợp bàn giao

Source: BC-185/R20/M07, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự ghi PICKED_UP thay Courier; hai phía đối chiếu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `warehouse`.

- **AC-BC-185-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xác nhận Courier tới lấy và phối hợp bàn giao; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Không tự ghi PICKED_UP thay Courier»; reload/API/PG assertion; TEST-V3-BC-185-FLOW.
- **AC-BC-185-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-185-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-212 — Quét barcode và đối chiếu mã, số lượng kiện

Source: BC-212/R07/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng parcel/manifest; phát hiện thiếu, trùng, lệch; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-212-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quét barcode và đối chiếu mã, số lượng kiện; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Đúng parcel/manifest»; reload/API/PG assertion; TEST-V3-BC-212-FLOW.
- **AC-BC-212-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-212-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-213 — Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup

Source: BC-213/R07/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ghi sai lệch và evidence; không tự chốt lại phí; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-213-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Ghi sai lệch và evidence»; reload/API/PG assertion; TEST-V3-BC-213-FLOW.
- **AC-BC-213-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-213-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-218 — Kiểm tra hàng gửi và điều kiện chấp nhận

Source: BC-218/R08/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Theo chính sách hàng cấm/hạn chế; không truy cập dữ liệu không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-218-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm tra hàng gửi và điều kiện chấp nhận; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Theo chính sách hàng cấm/hạn chế»; reload/API/PG assertion; TEST-V3-BC-218-FLOW.
- **AC-BC-218-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-218-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-253 — Quét barcode kiện trong công đoạn phân loại

Source: BC-253/R25/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Scan event truy vết người/máy/thời điểm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-253-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quét barcode kiện trong công đoạn phân loại; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Scan event truy vết người/máy/thời điểm»; reload/API/PG assertion; TEST-V3-BC-253-FLOW.
- **AC-BC-253-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-253-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-258 — Ghi nhận barcode không đọc được

Source: BC-258/R25/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Case xử lý định danh thay thế có kiểm soát; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-258-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận barcode không đọc được; Then outcome lưu/đọc đúng Parcel/label/measurement or SKU pick-pack facts, giữ guard «Case xử lý định danh thay thế có kiểm soát»; reload/API/PG assertion; TEST-V3-BC-258-FLOW.
- **AC-BC-258-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-258-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-007-01 | Stock theo owner, SKU, warehouse, location và condition; available = eligible − remaining holds ≥ 0. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-02 | Reservation consumed/released/remaining với invariant reserved = consumed + released + remaining. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-03 | Mọi stock move từ chứng từ, append-only history; transfer bảo toàn tổng; kiểm kê không trực tiếp ghi balance. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-04 | Pick/outbound không vượt reservation remaining; hủy chỉ giải phóng unused; hàng xuất phải qua return receipt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |
| REQ-007-05 | Nhân viên chỉ thao tác kho được cấp; duplicate scan/command không tạo movement lặp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/007-warehouse-wms/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-PARCEL-HANDOVER

Owner target: L008; source BC-026;BC-140;BC-176;BC-179;BC-180;BC-181;BC-182;BC-183;BC-184;BC-185; latest Owner portal scope, no new BC ID. UI/channel: Pick/pack/measure/labels/handover list.

Required behavior: Pick queue/quantity variance/packing/measurement/barcode identity/reprint audit; label only valid tracking/Parcel identity; Merchant handover preparation not carrier custody acceptance. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-PARCEL-HANDOVER:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Wrong/missing/duplicate Parcel barcode and unauthorized printing denied; measurement snapshot; pack completion no carrier PICKED_UP; two-party handover through010/011. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-PARCEL-HANDOVER PLANNED_NOT_EXECUTED. Dependencies: L006;L007;L010.
