# Spec 021 — claims-investigation-compensation

Alias lịch sử logical `L021`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `orders`; từng BC giữ DML owner riêng.

## Outcome và boundary

Claims và compensation tách ticket. Case/evidence/responsibility/proposal/approval ở orders; finance refund đã duyệt.

Workflow: Ticket→claim→evidence→đề xuất→người khác duyệt→Finance pay; chống self-approval/trùng. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L016, L018, L020. Consumer/soft: L019. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-05; D-V3-07; DEC-15. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-112 — Mở claim case gắn ticket/Shipment

Source: BC-112/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không trùng vụ việc; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-112-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Mở claim case gắn ticket/Shipment; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không trùng vụ việc»; reload/API/PG assertion; TEST-V3-BC-112-FLOW.
- **AC-BC-112-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-112-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-113 — Thu thập POD/tracking/ảnh/biên bản

Source: BC-113/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Bằng chứng có nguồn gốc; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-113-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Thu thập POD/tracking/ảnh/biên bản; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Bằng chứng có nguồn gốc»; reload/API/PG assertion; TEST-V3-BC-113-FLOW.
- **AC-BC-113-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-113-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-114 — Xác định trách nhiệm và đề xuất phương án

Source: BC-114/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Người đề xuất khác người duyệt nếu cần; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-114-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác định trách nhiệm và đề xuất phương án; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Người đề xuất khác người duyệt nếu cần»; reload/API/PG assertion; TEST-V3-BC-114-FLOW.
- **AC-BC-114-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-114-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-115 — Ghi nhận outcome và thông báo CS/Merchant

Source: BC-115/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Audit đầy đủ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-115-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận outcome và thông báo CS/Merchant; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Audit đầy đủ»; reload/API/PG assertion; TEST-V3-BC-115-FLOW.
- **AC-BC-115-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-115-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-117 — Truy vết lịch sử xử lý claim

Source: BC-117/R15/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa evidence; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-117-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Truy vết lịch sử xử lý claim; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không xóa evidence»; reload/API/PG assertion; TEST-V3-BC-117-FLOW.
- **AC-BC-117-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-117-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-129 — Đọc bằng chứng claim và phê duyệt

Source: BC-129/R17/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng phạm vi kiểm toán; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-129-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc bằng chứng claim và phê duyệt; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Đúng phạm vi kiểm toán»; reload/API/PG assertion; TEST-V3-BC-129-FLOW.
- **AC-BC-129-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-129-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-163 — Theo dõi claim, yêu cầu bồi thường và đơn bất thường

Source: BC-163/R02/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự phê duyệt bồi thường của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `orders`.

- **AC-BC-163-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Theo dõi claim, yêu cầu bồi thường và đơn bất thường; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không tự phê duyệt bồi thường của hãng»; reload/API/PG assertion; TEST-V3-BC-163-FLOW.
- **AC-BC-163-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-163-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-297 — Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD

Source: BC-297/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đối chiếu evidence, tracking, custody và trách nhiệm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-297-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Đối chiếu evidence, tracking, custody và trách nhiệm»; reload/API/PG assertion; TEST-V3-BC-297-FLOW.
- **AC-BC-297-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-297-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-298 — Truy xuất custody và biên bản bàn giao phục vụ claim

Source: BC-298/R15/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ case scope; không sửa sự kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-298-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Truy xuất custody và biên bản bàn giao phục vụ claim; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Chỉ case scope»; reload/API/PG assertion; TEST-V3-BC-298-FLOW.
- **AC-BC-298-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-298-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-300 — Điều tra thu sai COD liên quan claim

Source: BC-300/R15/M12, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự tạo điều chỉnh tài chính; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-300-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều tra thu sai COD liên quan claim; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không tự tạo điều chỉnh tài chính»; reload/API/PG assertion; TEST-V3-BC-300-FLOW.
- **AC-BC-300-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-300-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-301 — Đánh giá chính sách, hạn mức và đề xuất số bồi thường

Source: BC-301/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Separation of duties và reason; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-301-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đánh giá chính sách, hạn mức và đề xuất số bồi thường; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Separation of duties và reason»; reload/API/PG assertion; TEST-V3-BC-301-FLOW.
- **AC-BC-301-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-301-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-302 — Chuyển hồ sơ đề xuất sang người phê duyệt

Source: BC-302/R15/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự phê duyệt chi trả khi không có quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-302-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Chuyển hồ sơ đề xuất sang người phê duyệt; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không tự phê duyệt chi trả khi không có quyền»; reload/API/PG assertion; TEST-V3-BC-302-FLOW.
- **AC-BC-302-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-302-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-318 — Đọc bằng chứng claim và phê duyệt

Source: BC-318/R17/M15, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không thay đổi outcome; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `orders`.

- **AC-BC-318-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc bằng chứng claim và phê duyệt; Then outcome lưu/đọc đúng claim/evidence/proposal/approval, giữ guard «Không thay đổi outcome»; reload/API/PG assertion; TEST-V3-BC-318-FLOW.
- **AC-BC-318-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-318-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-CLAIMS

Owner target: L021; source BC-145;BC-163;BC-195; latest Owner portal scope, no new BC ID. UI/channel: Personal/Merchant claim case/evidence/outcome.

Required behavior: Retail may escalate own lost/damaged/delayed inquiry from020 to claim; Merchant Support routes recipient complaints; independent proposal/approval, finance payment refs. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-CLAIMS:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Personal owner verified without Merchant; wrong case/evidence denied; no self-approved compensation or fake paid status; duplicate case policy. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-CLAIMS PLANNED_NOT_EXECUTED. Dependencies: L020;L016;L018.
