# Spec 005 — pricing-contracts-quotation

Alias lịch sử logical `L005`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `pricing`; từng BC giữ DML owner riêng.

## Outcome và boundary

Báo giá theo khách gửi/hợp đồng/thời điểm. Rate/effective interval/rounding/phụ phí/Sales proposal/approval; authoritative Merchant contract versions/renewal decisions, Sales scoped proposal.

Workflow: Draft→duyệt/publish→quote→expiry/requote; không lộ giá tenant khác. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L004. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-07; DEC-08; D-V3-04. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-002 — Ước tính cước vận chuyển

Source: BC-002/R01/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không lộ giá hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `pricing`.

- **AC-BC-002-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Ước tính cước vận chuyển; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không lộ giá hợp đồng Merchant»; reload/API/PG assertion; TEST-V3-BC-002-FLOW.
- **AC-BC-002-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-002-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-019 — Xem hợp đồng và biểu phí áp dụng

Source: BC-019/R02/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự sửa bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `pricing`.

- **AC-BC-019-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem hợp đồng và biểu phí áp dụng; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không tự sửa bảng giá»; reload/API/PG assertion; TEST-V3-BC-019-FLOW.
- **AC-BC-019-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-019-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-023 — Nhận báo giá và chọn dịch vụ

Source: BC-023/R03/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Snapshot giá khi chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `pricing`.

- **AC-BC-023-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Nhận báo giá và chọn dịch vụ; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Snapshot giá khi chốt»; reload/API/PG assertion; TEST-V3-BC-023-FLOW.
- **AC-BC-023-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-023-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-039 — Quản trị chính sách dịch vụ/bảng giá được ủy quyền

Source: BC-039/R04/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự động có quyền sửa COD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-039-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản trị chính sách dịch vụ/bảng giá được ủy quyền; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không tự động có quyền sửa COD»; reload/API/PG assertion; TEST-V3-BC-039-FLOW.
- **AC-BC-039-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-039-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-131 — Tra cứu danh mục dịch vụ, điều kiện gửi hàng

Source: BC-131/R01/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Catalog công khai, không lộ hợp đồng riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `pricing`.

- **AC-BC-131-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Tra cứu danh mục dịch vụ, điều kiện gửi hàng; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Catalog công khai, không lộ hợp đồng riêng»; reload/API/PG assertion; TEST-V3-BC-131-FLOW.
- **AC-BC-131-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-131-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-138 — Báo giá và chọn dịch vụ gửi lẻ

Source: BC-138/R18/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không áp dụng giá hợp đồng Merchant trái quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `pricing`.

- **AC-BC-138-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Báo giá và chọn dịch vụ gửi lẻ; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không áp dụng giá hợp đồng Merchant trái quyền»; reload/API/PG assertion; TEST-V3-BC-138-FLOW.
- **AC-BC-138-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-138-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-189 — Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng

Source: BC-189/R11/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự thay bảng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `pricing`.

- **AC-BC-189-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không tự thay bảng giá»; reload/API/PG assertion; TEST-V3-BC-189-FLOW.
- **AC-BC-189-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-189-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-290 — Đối chiếu giá chốt, phí điều chỉnh sau cân đo

Source: BC-290/R13/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Snapshot + phiên bản rate card; audit sửa phí; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-290-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đối chiếu giá chốt, phí điều chỉnh sau cân đo; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Snapshot + phiên bản rate card»; reload/API/PG assertion; TEST-V3-BC-290-FLOW.
- **AC-BC-290-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-290-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-304 — Quản lý dịch vụ và chính sách nghiệp vụ theo quyền

Source: BC-304/R04/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Giá đặc biệt cần approval riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-304-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý dịch vụ và chính sách nghiệp vụ theo quyền; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Giá đặc biệt cần approval riêng»; reload/API/PG assertion; TEST-V3-BC-304-FLOW.
- **AC-BC-304-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-304-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-319 — Cấu hình rate card theo vùng và tuyến

Source: BC-319/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Version, effective date, kiểm soát hồi tố; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-319-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Cấu hình rate card theo vùng và tuyến; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Version, effective date, kiểm soát hồi tố»; reload/API/PG assertion; TEST-V3-BC-319-FLOW.
- **AC-BC-319-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-319-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-320 — Quy tắc trọng lượng tính cước và đo kích thước

Source: BC-320/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Khối lượng quy đổi/cân thực tế theo policy; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-320-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quy tắc trọng lượng tính cước và đo kích thước; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Khối lượng quy đổi/cân thực tế theo policy»; reload/API/PG assertion; TEST-V3-BC-320-FLOW.
- **AC-BC-320-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-320-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-321 — Quản trị loại dịch vụ vận chuyển

Source: BC-321/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Service availability và vùng phục vụ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-321-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản trị loại dịch vụ vận chuyển; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Service availability và vùng phục vụ»; reload/API/PG assertion; TEST-V3-BC-321-FLOW.
- **AC-BC-321-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-321-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-322 — Cấu hình phí COD và các phụ phí

Source: BC-322/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Min/max/tiers rõ ràng, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-322-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Cấu hình phí COD và các phụ phí; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Min/max/tiers rõ ràng, audit»; reload/API/PG assertion; TEST-V3-BC-322-FLOW.
- **AC-BC-322-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-322-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-323 — Áp dụng biểu phí riêng theo hợp đồng Merchant

Source: BC-323/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ hợp đồng được phê duyệt và còn hiệu lực; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-323-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Áp dụng biểu phí riêng theo hợp đồng Merchant; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Chỉ hợp đồng được phê duyệt và còn hiệu lực»; reload/API/PG assertion; TEST-V3-BC-323-FLOW.
- **AC-BC-323-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-323-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-324 — Quản lý chiết khấu và khuyến mãi

Source: BC-324/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Điều kiện áp dụng, phê duyệt và hạn sử dụng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-324-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý chiết khấu và khuyến mãi; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Điều kiện áp dụng, phê duyệt và hạn sử dụng»; reload/API/PG assertion; TEST-V3-BC-324-FLOW.
- **AC-BC-324-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-324-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-325 — Quản trị thời điểm hiệu lực bảng giá

Source: BC-325/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự hồi tố đơn đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-325-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản trị thời điểm hiệu lực bảng giá; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không tự hồi tố đơn đã chốt»; reload/API/PG assertion; TEST-V3-BC-325-FLOW.
- **AC-BC-325-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-325-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-326 — Phân biệt giá ước tính, giá chốt và phí điều chỉnh

Source: BC-326/R28/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Lưu price snapshot + adjustment trail; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-326-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phân biệt giá ước tính, giá chốt và phí điều chỉnh; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Lưu price snapshot + adjustment trail»; reload/API/PG assertion; TEST-V3-BC-326-FLOW.
- **AC-BC-326-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-326-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-328 — Audit sửa bảng giá, phê duyệt và phiên bản

Source: BC-328/R28/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa price history; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-328-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Audit sửa bảng giá, phê duyệt và phiên bản; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không xóa price history»; reload/API/PG assertion; TEST-V3-BC-328-FLOW.
- **AC-BC-328-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-328-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-331 — Theo dõi hợp đồng Merchant và gia hạn

Source: BC-331/R29/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Quyền xem theo scope, tài liệu versioned; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-331-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi hợp đồng Merchant và gia hạn; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Quyền xem theo scope, tài liệu versioned»; reload/API/PG assertion; TEST-V3-BC-331-FLOW.
- **AC-BC-331-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-331-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-332 — Đề xuất bảng giá hoặc chiết khấu đặc biệt

Source: BC-332/R29/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự phê duyệt hoặc áp dụng giá; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-332-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đề xuất bảng giá hoặc chiết khấu đặc biệt; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Không tự phê duyệt hoặc áp dụng giá»; reload/API/PG assertion; TEST-V3-BC-332-FLOW.
- **AC-BC-332-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-332-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-336 — Audit đề xuất giá và thay đổi quan hệ hợp đồng

Source: BC-336/R29/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Approval trace; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `pricing`.

- **AC-BC-336-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Audit đề xuất giá và thay đổi quan hệ hợp đồng; Then outcome lưu/đọc đúng service policy/quote/contract version, giữ guard «Approval trace»; reload/API/PG assertion; TEST-V3-BC-336-FLOW.
- **AC-BC-336-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-336-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-004-01 | Khối lượng tính phí là max(real, volumetric), unit và rounding theo version; mọi input bắt buộc được validate. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-02 | Giá gồm zone/service/tier/surcharge/discount với thứ tự xác định; không đoán giá khi thiếu rule. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-03 | Mỗi quote lưu version, canonical input, breakdown, currency, total, issuedAt, expiresAt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-04 | Published tariff/version không update in-place; bảng giá mới tạo version mới. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-05 | Cache theo key gồm tariffVersion và canonical price dimensions; Redis fail fallback DB không sai cước. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-QUOTE-FORMULA

Owner target: L005; source BC-002;BC-023;BC-138;BC-323; latest Owner portal scope, no new BC ID. UI/channel: Quote/service/promotion/price ETA summary.

Required behavior: Versioned volumetric formula/max(actual,volumetric), exact units/rounding/order of surcharges/discounts; freight payer/COD eligibility/route supported service/promotion; ETA only from actual policy. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-QUOTE-FORMULA:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Golden fixtures boundaries/expired quote/publish races/private rate isolation; missing rule rejects rather than guesses; unsupported international/immediate not advertised active. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-QUOTE-FORMULA PLANNED_NOT_EXECUTED. Dependencies: L004.
