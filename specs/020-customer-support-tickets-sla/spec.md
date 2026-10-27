# Spec 020 — customer-support-tickets-sla

Alias lịch sử logical `L020`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Ticket có nguồn/scope/SLA. Public contact core F1; Sender direct/Merchant Support handoff; approved source-domain command; Merchant-owned assigned-customer relationship/interactions qua merchant API, không account/password master.

Workflow: Tiếp nhận→verify→phân công→phối hợp/escalate→phản hồi; không sửa POD/COD. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L002. Consumer/soft: L009, L015. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-05; D-V3-06; DEC-20. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-004 — Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập

Source: BC-004/R01/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Rate limit và kiểm soát spam; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-004-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Rate limit và kiểm soát spam»; reload/API/PG assertion; TEST-V3-BC-004-FLOW.
- **AC-BC-004-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-004-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-016 — Gửi và theo dõi support ticket

Source: BC-016/R02/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-016-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Gửi và theo dõi support ticket; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự quyết định bồi thường»; reload/API/PG assertion; TEST-V3-BC-016-FLOW.
- **AC-BC-016-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-016-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-030 — Tạo ticket hỗ trợ Logistics nếu được cấp quyền

Source: BC-030/R03/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-030-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo ticket hỗ trợ Logistics nếu được cấp quyền; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không duyệt bồi thường»; reload/API/PG assertion; TEST-V3-BC-030-FLOW.
- **AC-BC-030-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-030-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-038 — Phê duyệt escalation theo quyền

Source: BC-038/R04/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Separation of duties; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-038-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phê duyệt escalation theo quyền; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Separation of duties»; reload/API/PG assertion; TEST-V3-BC-038-FLOW.
- **AC-BC-038-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-038-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-083 — Phản ánh đơn Merchant qua Merchant Support

Source: BC-083/R10/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant lập ticket với Logistics CSKH; không có Recipient Portal; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `orders`.

- **AC-BC-083-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Phản ánh đơn Merchant qua Merchant Support; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Merchant lập ticket với Logistics CSKH»; reload/API/PG assertion; TEST-V3-BC-083-FLOW.
- **AC-BC-083-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-083-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-089 — Mở tra soát COD thiếu/phí sai

Source: BC-089/R11/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự duyệt hoàn tiền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-089-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Mở tra soát COD thiếu/phí sai; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự duyệt hoàn tiền»; reload/API/PG assertion; TEST-V3-BC-089-FLOW.
- **AC-BC-089-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-089-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-105 — Nhận/tạo ticket từ Merchant hoặc người liên quan

Source: BC-105/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Phân loại nguồn; xác minh quyền thông tin; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-105-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhận/tạo ticket từ Merchant hoặc người liên quan; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Phân loại nguồn»; reload/API/PG assertion; TEST-V3-BC-105-FLOW.
- **AC-BC-105-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-105-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-107 — Phân loại, phân công và cập nhật SLA ticket

Source: BC-107/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Lưu lịch sử thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-107-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phân loại, phân công và cập nhật SLA ticket; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Lưu lịch sử thay đổi»; reload/API/PG assertion; TEST-V3-BC-107-FLOW.
- **AC-BC-107-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-107-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-108 — Gửi yêu cầu giao lại/đổi thông tin

Source: BC-108/R14/M10, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Workflow phê duyệt; không tự sửa Shipment đã chốt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-108-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Gửi yêu cầu giao lại/đổi thông tin; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Workflow phê duyệt»; reload/API/PG assertion; TEST-V3-BC-108-FLOW.
- **AC-BC-108-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-108-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-109 — Escalate vụ nghiêm trọng

Source: BC-109/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự phê duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-109-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Escalate vụ nghiêm trọng; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự phê duyệt bồi thường»; reload/API/PG assertion; TEST-V3-BC-109-FLOW.
- **AC-BC-109-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-109-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-110 — Đa kênh hotline/email/chat tích hợp

Source: BC-110/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không cần xây portal Recipient; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-110-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đa kênh hotline/email/chat tích hợp; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không cần xây portal Recipient»; reload/API/PG assertion; TEST-V3-BC-110-FLOW.
- **AC-BC-110-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-110-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-145 — Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng

Source: BC-145/R18/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ Shipment thuộc mình, có ticket/evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `orders`.

- **AC-BC-145-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Chỉ Shipment thuộc mình, có ticket/evidence»; reload/API/PG assertion; TEST-V3-BC-145-FLOW.
- **AC-BC-145-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-145-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-153 — Nhận và xử lý ticket trực tiếp từ Retail Sender

Source: BC-153/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Xác minh Shipment thuộc Sender, tracking, timeline, escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-153-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Nhận và xử lý ticket trực tiếp từ Retail Sender; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Xác minh Shipment thuộc Sender, tracking, timeline, escalation»; reload/API/PG assertion; TEST-V3-BC-153-FLOW.
- **AC-BC-153-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-153-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-154 — Tiếp nhận phản ánh Recipient cho đơn Retail Sender

Source: BC-154/R14/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả định Merchant Support; xác minh tối thiểu; bảo vệ PII; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-154-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tiếp nhận phản ánh Recipient cho đơn Retail Sender; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không giả định Merchant Support»; reload/API/PG assertion; TEST-V3-BC-154-FLOW.
- **AC-BC-154-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-154-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-155 — Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant

Source: BC-155/R02/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có thể ủy quyền Merchant Support khi Role được bổ sung; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-155-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Có thể ủy quyền Merchant Support khi Role được bổ sung»; reload/API/PG assertion; TEST-V3-BC-155-FLOW.
- **AC-BC-155-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-155-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-156 — Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền

Source: BC-156/R03/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant scope, không tự quyết định bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-156-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Merchant scope, không tự quyết định bồi thường»; reload/API/PG assertion; TEST-V3-BC-156-FLOW.
- **AC-BC-156-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-156-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-173 — Trao đổi với hãng chuyển phát, mở/escalate ticket

Source: BC-173/R19/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự duyệt bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-173-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Trao đổi với hãng chuyển phát, mở/escalate ticket; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự duyệt bồi thường»; reload/API/PG assertion; TEST-V3-BC-173-FLOW.
- **AC-BC-173-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-173-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-191 — Mở tra soát COD thiếu, phí sai và theo dõi kết quả

Source: BC-191/R11/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự phê duyệt khoản hoàn/bồi thường; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-191-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Mở tra soát COD thiếu, phí sai và theo dõi kết quả; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự phê duyệt khoản hoàn/bồi thường»; reload/API/PG assertion; TEST-V3-BC-191-FLOW.
- **AC-BC-191-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-191-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-195 — Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment

Source: BC-195/R21/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Xác minh đơn/nguồn phản ánh; chống ticket trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-195-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Xác minh đơn/nguồn phản ánh»; reload/API/PG assertion; TEST-V3-BC-195-FLOW.
- **AC-BC-195-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-195-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-196 — Gửi ticket sang E-Logistics Customer Service và theo dõi timeline

Source: BC-196/R21/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có phân công, SLA, audit và bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-196-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Gửi ticket sang E-Logistics Customer Service và theo dõi timeline; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Có phân công, SLA, audit và bằng chứng»; reload/API/PG assertion; TEST-V3-BC-196-FLOW.
- **AC-BC-196-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-196-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-199 — Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket

Source: BC-199/R21/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự đóng ticket Logistics trái quy trình; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-199-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không tự đóng ticket Logistics trái quy trình»; reload/API/PG assertion; TEST-V3-BC-199-FLOW.
- **AC-BC-199-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-199-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-249 — Hỗ trợ sự cố giao nhận và escalations

Source: BC-249/R24/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ghi lý do, liên kết ticket/incident; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-249-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Hỗ trợ sự cố giao nhận và escalations; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Ghi lý do, liên kết ticket/incident»; reload/API/PG assertion; TEST-V3-BC-249-FLOW.
- **AC-BC-249-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-249-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-266 — Tạo/điều phối báo cáo mất hoặc hỏng kiện

Source: BC-266/R26/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Liên kết kiện và vị trí cuối, ticket/claim; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-266-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tạo/điều phối báo cáo mất hoặc hỏng kiện; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Liên kết kiện và vị trí cuối, ticket/claim»; reload/API/PG assertion; TEST-V3-BC-266-FLOW.
- **AC-BC-266-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-266-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-292 — Tra cứu Shipment theo ticket/phạm vi phân công

Source: BC-292/R14/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: PII tối thiểu, reason và access audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-292-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tra cứu Shipment theo ticket/phạm vi phân công; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «PII tối thiểu, reason và access audit»; reload/API/PG assertion; TEST-V3-BC-292-FLOW.
- **AC-BC-292-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-292-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-294 — Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc

Source: BC-294/R14/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Xác minh sở hữu; phê duyệt thay đổi theo state; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-294-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Xác minh sở hữu»; reload/API/PG assertion; TEST-V3-BC-294-FLOW.
- **AC-BC-294-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-294-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-296 — Giám sát deadline phản hồi và giải quyết ticket

Source: BC-296/R14/M16, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: SLA clock, escalation, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-296-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Giám sát deadline phản hồi và giải quyết ticket; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «SLA clock, escalation, audit»; reload/API/PG assertion; TEST-V3-BC-296-FLOW.
- **AC-BC-296-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-296-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-307 — Phê duyệt escalation nghiệp vụ theo hạn mức

Source: BC-307/R04/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Separation of duties, lý do và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-307-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phê duyệt escalation nghiệp vụ theo hạn mức; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Separation of duties, lý do và audit»; reload/API/PG assertion; TEST-V3-BC-307-FLOW.
- **AC-BC-307-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-307-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-329 — Quản lý Merchant/account được giao

Source: BC-329/R29/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không truy cập Merchant ngoài danh sách; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-329-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý Merchant/account được giao; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không truy cập Merchant ngoài danh sách»; reload/API/PG assertion; TEST-V3-BC-329-FLOW.
- **AC-BC-329-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-329-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-330 — Tư vấn dịch vụ và ghi nhận tương tác khách hàng

Source: BC-330/R29/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không đọc PII Shipment không cần thiết; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-330-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tư vấn dịch vụ và ghi nhận tương tác khách hàng; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Không đọc PII Shipment không cần thiết»; reload/API/PG assertion; TEST-V3-BC-330-FLOW.
- **AC-BC-330-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-330-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-334 — Phối hợp hỗ trợ Merchant lớn

Source: BC-334/R29/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ticket ownership và escalation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-334-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phối hợp hỗ trợ Merchant lớn; Then outcome lưu/đọc đúng ticket/assigned customer relationship/interaction, giữ guard «Ticket ownership và escalation»; reload/API/PG assertion; TEST-V3-BC-334-FLOW.
- **AC-BC-334-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-334-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

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


## REQ-AB-SUPPORT

Owner target: L020; source BC-004;BC-083;BC-089;BC-145;BC-153;BC-154;BC-155;BC-195;BC-196;BC-199;BC-294; latest Owner portal scope, no new BC ID. UI/channel: Guest contact; personal/Merchant tickets; verified Recipient channel.

Required behavior: Guest anti-spam contact, customer-owned ticket history/SLA/escalation; Merchant shipment recipient via Merchant Support or authorized carrier CS, Retail recipient direct carrier CS; address/contact change request not direct state patch. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-SUPPORT:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Identity/Shipment ownership verified; cross-tenant/case denied; external support channel no mandatory Recipient account; no ticket auto refunds/POD mutation. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-SUPPORT PLANNED_NOT_EXECUTED. Dependencies: L002;L009.
