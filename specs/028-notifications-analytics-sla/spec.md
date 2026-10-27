# Spec 028 — notifications-analytics-sla

Alias lịch sử logical `L028`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `notification`; từng BC giữ DML owner riêng.

## Outcome và boundary

Notifications/KPI/SLA từ committed facts. Outbox/inbox/templates/scoped projection/retry/DLQ; minimum notification first use; personal/Merchant/team dashboards và notifications; KPI source facts, declared freshness, no fabricated counts.

Workflow: Commit→dedup notification→read; late/missing event đối chiếu; không success inflation. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L009, L019, L020. Consumer/soft: L025. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-20; D-V3-06. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-017 — Xem KPI, báo cáo chi phí, tỷ lệ giao

Source: BC-017/R02/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `notification`.

- **AC-BC-017-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem KPI, báo cáo chi phí, tỷ lệ giao; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ dữ liệu Merchant»; reload/API/PG assertion; TEST-V3-BC-017-FLOW.
- **AC-BC-017-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-017-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-035 — Theo dõi SLA và báo cáo vận hành

Source: BC-035/R04/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ phạm vi được phép; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-035-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi SLA và báo cáo vận hành; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ phạm vi được phép»; reload/API/PG assertion; TEST-V3-BC-035-FLOW.
- **AC-BC-035-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-035-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-053 — Theo dõi SLA, cảnh báo quá hạn

Source: BC-053/R06/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự sửa tracking; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-053-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi SLA, cảnh báo quá hạn; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Không tự sửa tracking»; reload/API/PG assertion; TEST-V3-BC-053-FLOW.
- **AC-BC-053-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-053-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-061 — Xem KPI cá nhân

Source: BC-061/R07/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không có quyền giám sát toàn đội; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-061-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem KPI cá nhân; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Không có quyền giám sát toàn đội»; reload/API/PG assertion; TEST-V3-BC-061-FLOW.
- **AC-BC-061-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-061-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-090 — Xuất báo cáo COD, phí

Source: BC-090/R11/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng phạm vi Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `notification`.

- **AC-BC-090-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xuất báo cáo COD, phí; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Đúng phạm vi Merchant»; reload/API/PG assertion; TEST-V3-BC-090-FLOW.
- **AC-BC-090-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-090-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-122 — Theo dõi KPI/SLA và năng lực ca

Source: BC-122/R16/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng trạm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-122-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi KPI/SLA và năng lực ca; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Đúng trạm»; reload/API/PG assertion; TEST-V3-BC-122-FLOW.
- **AC-BC-122-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-122-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-130 — Xuất báo cáo audit

Source: BC-130/R17/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-130-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xuất báo cáo audit; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Bảo vệ PII»; reload/API/PG assertion; TEST-V3-BC-130-FLOW.
- **AC-BC-130-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-130-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-165 — Xem báo cáo COD, giao thành công, đơn hoàn, chi phí

Source: BC-165/R02/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dữ liệu Merchant; không lộ tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `notification`.

- **AC-BC-165-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem báo cáo COD, giao thành công, đơn hoàn, chi phí; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Dữ liệu Merchant»; reload/API/PG assertion; TEST-V3-BC-165-FLOW.
- **AC-BC-165-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-165-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-172 — Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao

Source: BC-172/R19/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xem KPI Merchant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `notification`.

- **AC-BC-172-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Không xem KPI Merchant khác»; reload/API/PG assertion; TEST-V3-BC-172-FLOW.
- **AC-BC-172-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-172-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-192 — Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh

Source: BC-192/R11/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Giới hạn Merchant/branch scope; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `notification`.

- **AC-BC-192-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Giới hạn Merchant/branch scope»; reload/API/PG assertion; TEST-V3-BC-192-FLOW.
- **AC-BC-192-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-192-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-209 — Giám sát tỷ lệ pickup/delivery thành công và tải công việc

Source: BC-209/R06/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: KPI có định nghĩa, dữ liệu trong vùng được phân quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-209-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát tỷ lệ pickup/delivery thành công và tải công việc; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «KPI có định nghĩa, dữ liệu trong vùng được phân quyền»; reload/API/PG assertion; TEST-V3-BC-209-FLOW.
- **AC-BC-209-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-209-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-215 — Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại

Source: BC-215/R07/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ hiệu suất cá nhân; dựa trên event và attempt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-215-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ hiệu suất cá nhân»; reload/API/PG assertion; TEST-V3-BC-215-FLOW.
- **AC-BC-215-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-215-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-229 — Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp

Source: BC-229/R09/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu cá nhân; COD chưa nộp không được coi là remitted; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-229-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ dữ liệu cá nhân»; reload/API/PG assertion; TEST-V3-BC-229-FLOW.
- **AC-BC-229-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-229-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-246 — Theo dõi hiệu suất và năng suất đội giao nhận

Source: BC-246/R24/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dữ liệu đúng team/scope và nguồn sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-246-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi hiệu suất và năng suất đội giao nhận; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Dữ liệu đúng team/scope và nguồn sự kiện»; reload/API/PG assertion; TEST-V3-BC-246-FLOW.
- **AC-BC-246-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-246-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-268 — Theo dõi KPI, throughput và thời gian tồn Hub

Source: BC-268/R26/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ số định nghĩa và provenance; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-268-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi KPI, throughput và thời gian tồn Hub; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ số định nghĩa và provenance»; reload/API/PG assertion; TEST-V3-BC-268-FLOW.
- **AC-BC-268-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-268-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-275 — Theo dõi SLA, cảnh báo và năng suất mạng lưới

Source: BC-275/R27/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Định nghĩa thống nhất, phân quyền dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-275-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi SLA, cảnh báo và năng suất mạng lưới; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Định nghĩa thống nhất, phân quyền dữ liệu»; reload/API/PG assertion; TEST-V3-BC-275-FLOW.
- **AC-BC-275-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-275-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-291 — Xuất báo cáo công nợ và COD tồn chưa đối soát

Source: BC-291/R13/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Theo kỳ, có nguồn bút toán; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-291-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xuất báo cáo công nợ và COD tồn chưa đối soát; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Theo kỳ, có nguồn bút toán»; reload/API/PG assertion; TEST-V3-BC-291-FLOW.
- **AC-BC-291-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-291-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-333 — Theo dõi KPI và chỉ tiêu kinh doanh

Source: BC-333/R29/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dữ liệu theo account/region scope; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-333-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi KPI và chỉ tiêu kinh doanh; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Dữ liệu theo account/region scope»; reload/API/PG assertion; TEST-V3-BC-333-FLOW.
- **AC-BC-333-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-333-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-356 — Kiểm tra sức khỏe hệ thống và tín hiệu vận hành

Source: BC-356/R32/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ telemetry cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-356-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Kiểm tra sức khỏe hệ thống và tín hiệu vận hành; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Chỉ telemetry cần thiết»; reload/API/PG assertion; TEST-V3-BC-356-FLOW.
- **AC-BC-356-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-356-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-359 — Giám sát hàng đợi, dead-letter và backlog

Source: BC-359/R32/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có hạn mức retry và cảnh báo; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-359-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát hàng đợi, dead-letter và backlog; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Có hạn mức retry và cảnh báo»; reload/API/PG assertion; TEST-V3-BC-359-FLOW.
- **AC-BC-359-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-359-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-363 — Khắc phục lỗi phát thông báo/tracking event

Source: BC-363/R32/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả mạo sự kiện giao hàng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `notification`.

- **AC-BC-363-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Khắc phục lỗi phát thông báo/tracking event; Then outcome lưu/đọc đúng notification/inbox/scoped KPI projection, giữ guard «Không giả mạo sự kiện giao hàng»; reload/API/PG assertion; TEST-V3-BC-363-FLOW.
- **AC-BC-363-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-363-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-012-01 | Chỉ tạo notification sau source transaction commit; mỗi (sourceEventId, recipientId, purpose) chỉ một bản. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-02 | Notification không tự quyết định trạng thái vận đơn/tài chính; dữ liệu nhạy cảm được redact. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-03 | Người dùng chỉ đọc notification của mình; `markAsRead` chỉ cập nhật target thuộc account. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-04 | Dashboard filter theo tenant/role/warehouse/hub; định nghĩa KPI và thời điểm cập nhật được công bố. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |
| REQ-012-05 | RabbitMQ unavailable vẫn giữ outbox; retry bounded + failed job readable theo quyền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/012-notifications-dashboard/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-PERSONAL-DASH

Owner target: L028; source BC-143;BC-147; latest Owner portal scope, no new BC ID. UI/channel: Personal dashboard.

Required behavior: Owned shipment stats/date range/status summaries/trend/top-volume if meaningful, safe notifications, global scoped search; metric definitions/source/as-of/lag explicit. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-PERSONAL-DASH:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Counters and chart tie to eligible owned source rows and timestamps; empty data zero/empty not fabricated; foreign shipment search denied. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-PERSONAL-DASH PLANNED_NOT_EXECUTED. Dependencies: L009;L007.


## REQ-AB-NOTIFICATIONS

Owner target: L028; source BC-080;BC-143;BC-194; latest Owner portal scope, no new BC ID. UI/channel: Personal/Merchant inbox; verified Recipient channel.

Required behavior: Post-commit source events, dedup per subject/purpose; safe content and authorized deep links; recipients scoped external channel not IAM. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-NOTIFICATIONS:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Duplicate/reorder/revoked recipient no leak/double effect; mark-read only owned; failure retained with bounded retry. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-NOTIFICATIONS PLANNED_NOT_EXECUTED. Dependencies: L009;L015;L020.


## REQ-AB-PORTAL-SHELL

Owner target: L028; source BC-017;BC-165;BC-172;BC-192; latest Owner portal scope, no new BC ID. UI/channel: Shared customer/Merchant shell.

Required behavior: Persistent sidebar/topbar/breadcrumb/context; responsive and keyboard; menu capability/spec/delivery status mapping; actual backend gates. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-PORTAL-SHELL:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Each visible interactive action calls supported API or deliberate disabled availability explanation; no mock-only dashboards; no privileged menu by role label alone. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-PORTAL-SHELL PLANNED_NOT_EXECUTED. Dependencies: L003;L009.
