# Spec 017 — cod-collection-cashier-remittance

Alias lịch sử logical `L017`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `finance`; từng BC giữ DML owner riêng.

## Outcome và boundary

COD collection và Cashier remittance. Collection contract trước L015; remittance/allocation/shift/variance.

Workflow: Collected→submitted→Cashier kiểm nhận; shortage giữ nghĩa vụ; courier không tự confirm. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L011, L018. Consumer/soft: L015. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-19; D-V3-04. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-014 — Xem COD thuộc Merchant

Source: BC-014/R02/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ đọc theo quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-014-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem COD thuộc Merchant; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Chỉ đọc theo quyền»; reload/API/PG assertion; TEST-V3-BC-014-FLOW.
- **AC-BC-014-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-014-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-028 — Xem COD của đơn được giao phụ trách

Source: BC-028/R03/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa COD đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-028-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem COD của đơn được giao phụ trách; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không sửa COD đã chốt»; reload/API/PG assertion; TEST-V3-BC-028-FLOW.
- **AC-BC-028-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-028-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-040 — Thực hiện điều chỉnh COD/settlement

Source: BC-040/R04/M12, DEFER; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không mặc định được phép; cần workflow tài chính riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-040-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Thực hiện điều chỉnh COD/settlement; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-040-DENY.
- **AC-BC-040-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-040-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

### REQ-V3-BC-046 — Sửa/duyệt COD

Source: BC-046/R05/M12, DEFER; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ngoài quyền Identity; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-046-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Sửa/duyệt COD; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-046-DENY.
- **AC-BC-046-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-046-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

### REQ-V3-BC-075 — Ghi nhận khoản COD đã thu

Source: BC-075/R09/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-075-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận khoản COD đã thu; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Đúng số tiền đã chốt»; reload/API/PG assertion; TEST-V3-BC-075-FLOW.
- **AC-BC-075-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-075-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-076 — Yêu cầu nộp COD cho quỹ

Source: BC-076/R09/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự xác nhận quỹ đã nhận; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-076-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Yêu cầu nộp COD cho quỹ; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không tự xác nhận quỹ đã nhận»; reload/API/PG assertion; TEST-V3-BC-076-FLOW.
- **AC-BC-076-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-076-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-082 — Thanh toán COD đã xác nhận

Source: BC-082/R10/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không thay đổi cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `finance`.

- **AC-BC-082-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Thanh toán COD đã xác nhận; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không thay đổi cod_amount»; reload/API/PG assertion; TEST-V3-BC-082-FLOW.
- **AC-BC-082-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-082-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-086 — Xem COD dự kiến/đã thu/đang giữ/đã trả

Source: BC-086/R11/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant isolation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-086-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem COD dự kiến/đã thu/đang giữ/đã trả; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Merchant isolation»; reload/API/PG assertion; TEST-V3-BC-086-FLOW.
- **AC-BC-086-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-086-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-092 — Tiếp nhận tiền mặt COD Courier nộp

Source: BC-092/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hai phía, amount và timestamp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-092-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tiếp nhận tiền mặt COD Courier nộp; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Hai phía, amount và timestamp»; reload/API/PG assertion; TEST-V3-BC-092-FLOW.
- **AC-BC-092-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-092-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-093 — Xác nhận nộp và ghi thiếu/thừa

Source: BC-093/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không âm thầm sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-093-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác nhận nộp và ghi thiếu/thừa; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không âm thầm sửa tiền»; reload/API/PG assertion; TEST-V3-BC-093-FLOW.
- **AC-BC-093-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-093-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-094 — Quản lý đối chiếu nộp COD theo ca

Source: BC-094/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự settle Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-094-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý đối chiếu nộp COD theo ca; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không tự settle Merchant»; reload/API/PG assertion; TEST-V3-BC-094-FLOW.
- **AC-BC-094-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-094-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-095 — Ghi audit tiền nộp và điều chỉnh

Source: BC-095/R12/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-095-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi audit tiền nộp và điều chỉnh; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không xóa receipt»; reload/API/PG assertion; TEST-V3-BC-095-FLOW.
- **AC-BC-095-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-095-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-096 — Kiểm kê két quỹ/đóng ca nâng cao

Source: BC-096/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có đối chiếu kế toán; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-096-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm kê két quỹ/đóng ca nâng cao; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Có đối chiếu kế toán»; reload/API/PG assertion; TEST-V3-BC-096-FLOW.
- **AC-BC-096-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-096-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-097 — Đối soát Courier thu và nộp COD

Source: BC-097/R13/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Phải khớp custody tiền và shipment; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-097-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đối soát Courier thu và nộp COD; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Phải khớp custody tiền và shipment»; reload/API/PG assertion; TEST-V3-BC-097-FLOW.
- **AC-BC-097-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-097-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-098 — Phát hiện COD thiếu/thừa/trùng

Source: BC-098/R13/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ledger bất biến/idempotency; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-098-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phát hiện COD thiếu/thừa/trùng; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Ledger bất biến/idempotency»; reload/API/PG assertion; TEST-V3-BC-098-FLOW.
- **AC-BC-098-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-098-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-121 — Giám sát bàn giao COD tại Station

Source: BC-121/R16/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-121-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát bàn giao COD tại Station; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không sửa sổ cái»; reload/API/PG assertion; TEST-V3-BC-121-FLOW.
- **AC-BC-121-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-121-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-127 — Đọc log đối soát COD

Source: BC-127/R17/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-127-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc log đối soát COD; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không sửa tiền»; reload/API/PG assertion; TEST-V3-BC-127-FLOW.
- **AC-BC-127-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-127-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-161 — Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ

Source: BC-161/R02/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ đọc tài chính thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-161-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Chỉ đọc tài chính thuộc Merchant»; reload/API/PG assertion; TEST-V3-BC-161-FLOW.
- **AC-BC-161-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-161-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-187 — Xem COD dự kiến, đã thu, đang giữ và đã thanh toán

Source: BC-187/R11/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa dữ liệu thu COD của Courier; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-187-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem COD dự kiến, đã thu, đang giữ và đã thanh toán; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không sửa dữ liệu thu COD của Courier»; reload/API/PG assertion; TEST-V3-BC-187-FLOW.
- **AC-BC-187-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-187-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-225 — Xác nhận số COD cần thu và phương thức thanh toán

Source: BC-225/R09/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ đọc giá trị COD đã chốt; không tự đổi tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-225-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác nhận số COD cần thu và phương thức thanh toán; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Chỉ đọc giá trị COD đã chốt»; reload/API/PG assertion; TEST-V3-BC-225-FLOW.
- **AC-BC-225-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-225-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-233 — Giám sát Courier nộp COD tại Station

Source: BC-233/R16/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Cashier xác nhận tiền; manager không sửa ledger; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-233-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát Courier nộp COD tại Station; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Cashier xác nhận tiền»; reload/API/PG assertion; TEST-V3-BC-233-FLOW.
- **AC-BC-233-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-233-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-280 — Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật

Source: BC-280/R27/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Negative permission tests, separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-280-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-280-DENY.
- **AC-BC-280-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-280-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

### REQ-V3-BC-281 — Kiểm đếm COD thực nhận theo ca và từng Courier

Source: BC-281/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Xác nhận song phương, denomination/count và số tiền đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-281-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm đếm COD thực nhận theo ca và từng Courier; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Xác nhận song phương, denomination/count và số tiền đã chốt»; reload/API/PG assertion; TEST-V3-BC-281-FLOW.
- **AC-BC-281-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-281-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-282 — Đóng ca và đối chiếu quỹ COD thực tế

Source: BC-282/R12/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-282-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đóng ca và đối chiếu quỹ COD thực tế; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm»; reload/API/PG assertion; TEST-V3-BC-282-FLOW.
- **AC-BC-282-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-282-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-283 — Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát

Source: BC-283/R12/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự ghi RECONCILED hay SETTLED; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-283-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không tự ghi RECONCILED hay SETTLED»; reload/API/PG assertion; TEST-V3-BC-283-FLOW.
- **AC-BC-283-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-283-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-284 — Ràng buộc điểm thu quỹ với Station/ca

Source: BC-284/R12/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ Station được giao, actor và thời gian có chứng cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-284-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ràng buộc điểm thu quỹ với Station/ca; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Chỉ Station được giao, actor và thời gian có chứng cứ»; reload/API/PG assertion; TEST-V3-BC-284-FLOW.
- **AC-BC-284-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-284-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-285 — Đối chiếu Delivered Shipment với COD thu thực tế

Source: BC-285/R13/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không suy luận COD từ DELIVERED; phát hiện ngoại lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-285-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đối chiếu Delivered Shipment với COD thu thực tế; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không suy luận COD từ DELIVERED»; reload/API/PG assertion; TEST-V3-BC-285-FLOW.
- **AC-BC-285-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-285-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-295 — Tiếp nhận tra soát COD và chuyển Finance

Source: BC-295/R14/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự sửa cod_amount; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-295-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tiếp nhận tra soát COD và chuyển Finance; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không tự sửa cod_amount»; reload/API/PG assertion; TEST-V3-BC-295-FLOW.
- **AC-BC-295-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-295-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-317 — Đọc chuỗi thu-nộp-đối soát COD

Source: BC-317/R17/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không ghi remittance; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-317-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc chuỗi thu-nộp-đối soát COD; Then outcome lưu/đọc đúng COD liability/collection/remittance, giữ guard «Không ghi remittance»; reload/API/PG assertion; TEST-V3-BC-317-FLOW.
- **AC-BC-317-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-317-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-354 — Không được giả xác nhận thu/đối soát/thanh toán COD

Source: BC-354/R31/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Negative permission tests; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-354-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Không được giả xác nhận thu/đối soát/thanh toán COD; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-354-DENY.
- **AC-BC-354-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-354-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-009-04 | COD collected là nghĩa vụ shipper; không tự chuyển remittance thành CONFIRMED. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/009-shipper-pod/spec.md) |
| REQ-011-01 | Phân biệt COLLECTED/REMITTED/RECONCILED, không coi shipper submit là xác nhận đã nộp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |
| REQ-011-02 | Remittance có allocation per COD item; tổng phân bổ ≤ remittance confirmed amount; không vượt COD liability. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |
| REQ-011-03 | Finance là người xác nhận, khác shipper khai báo; reason + evidence bắt buộc khi discrepancy/reject. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-RECIPIENT-COD

Owner target: L017; source BC-082;BC-225; latest Owner portal scope, no new BC ID. UI/channel: Verified collection receipt/channel.

Required behavior: Recipient pays locked COD via supported approved/simulated method; receipt ties amount/Shipment/attempt/Courier and collection proof, not remittance/settlement. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-RECIPIENT-COD:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then COD immutable to recipient; duplicate collection/payment callback once; no DELIVERED→COLLECTED inference, no real money claim. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-RECIPIENT-COD PLANNED_NOT_EXECUTED. Dependencies: L015;L018.
