# Spec 018 — billing-ledger-fees-invoices

Alias lịch sử logical `L018`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `finance`; từng BC giữ DML owner riêng.

## Outcome và boundary

Ledger, billing, phí và chứng từ. Minimal balanced posting/fee/refund API trước L007; full invoice/debt/adjustment.

Workflow: Verified charge→balanced posting; duplicate callback/refund không thêm tiền; correction bằng entry mới. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L005, L006. Consumer/soft: L007, L017. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-11; DEC-12; D-V3-04. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-088 — Tra cứu phí và phụ phí theo Shipment

Source: BC-088/R11/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tôn trọng snapshot giá; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-088-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tra cứu phí và phụ phí theo Shipment; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Tôn trọng snapshot giá»; reload/API/PG assertion; TEST-V3-BC-088-FLOW.
- **AC-BC-088-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-088-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-101 — Tính phí và công nợ phải thu/phải trả

Source: BC-101/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Snapshot giá + audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-101-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tính phí và công nợ phải thu/phải trả; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Snapshot giá + audit»; reload/API/PG assertion; TEST-V3-BC-101-FLOW.
- **AC-BC-101-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-101-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-102 — Audit điều chỉnh tài chính

Source: BC-102/R13/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa lịch sử; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-102-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Audit điều chỉnh tài chính; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không sửa lịch sử»; reload/API/PG assertion; TEST-V3-BC-102-FLOW.
- **AC-BC-102-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-102-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-103 — Refund và xử lý khiếu nại tài chính

Source: BC-103/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Phải được phê duyệt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-103-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Refund và xử lý khiếu nại tài chính; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Phải được phê duyệt»; reload/API/PG assertion; TEST-V3-BC-103-FLOW.
- **AC-BC-103-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-103-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-104 — Tích hợp cổng thanh toán/ngân hàng

Source: BC-104/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không yêu cầu thanh toán thật trong demo; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-104-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tích hợp cổng thanh toán/ngân hàng; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không yêu cầu thanh toán thật trong demo»; reload/API/PG assertion; TEST-V3-BC-104-FLOW.
- **AC-BC-104-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-104-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-116 — Đề xuất bồi thường/hoàn tiền

Source: BC-116/R15/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự thực hiện settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-116-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đề xuất bồi thường/hoàn tiền; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không tự thực hiện settlement»; reload/API/PG assertion; TEST-V3-BC-116-FLOW.
- **AC-BC-116-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-116-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-142 — Thanh toán phí vận chuyển theo phương thức hỗ trợ

Source: BC-142/R18/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Receipt, price snapshot và chống thu trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `finance`.

- **AC-BC-142-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Thanh toán phí vận chuyển theo phương thức hỗ trợ; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Receipt, price snapshot và chống thu trùng»; reload/API/PG assertion; TEST-V3-BC-142-FLOW.
- **AC-BC-142-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-142-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-148 — Xem/tải hóa đơn và biên nhận

Source: BC-148/R18/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hợp lệ với hình thức thanh toán, không rò dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `finance`.

- **AC-BC-148-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Xem/tải hóa đơn và biên nhận; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Hợp lệ với hình thức thanh toán, không rò dữ liệu»; reload/API/PG assertion; TEST-V3-BC-148-FLOW.
- **AC-BC-148-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-148-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-151 — Ghi nhận phí/thanh toán ký gửi tại quầy theo policy

Source: BC-151/R08/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự xác nhận thanh toán thiếu bằng chứng; sổ thu/receipt; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-151-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận phí/thanh toán ký gửi tại quầy theo policy; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không tự xác nhận thanh toán thiếu bằng chứng»; reload/API/PG assertion; TEST-V3-BC-151-FLOW.
- **AC-BC-151-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-151-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-286 — Quản lý công nợ phải thu và phải trả

Source: BC-286/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ledger entries append-only; tính toán có căn cứ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-286-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý công nợ phải thu và phải trả; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Ledger entries append-only»; reload/API/PG assertion; TEST-V3-BC-286-FLOW.
- **AC-BC-286-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-286-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-288 — Thực hiện hoàn tiền đã được phê duyệt

Source: BC-288/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Idempotency, kiểm soát hai người khi cần; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-288-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Thực hiện hoàn tiền đã được phê duyệt; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Idempotency, kiểm soát hai người khi cần»; reload/API/PG assertion; TEST-V3-BC-288-FLOW.
- **AC-BC-288-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-288-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-289 — Phát hiện và từ chối thanh toán hoặc refund trùng

Source: BC-289/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Idempotency key, unique reference và reconciliation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-289-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phát hiện và từ chối thanh toán hoặc refund trùng; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Idempotency key, unique reference và reconciliation»; reload/API/PG assertion; TEST-V3-BC-289-FLOW.
- **AC-BC-289-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-289-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-303 — Theo dõi thanh toán bồi thường/refund đã phê duyệt

Source: BC-303/R15/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả lập payment confirmation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-303-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Theo dõi thanh toán bồi thường/refund đã phê duyệt; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không giả lập payment confirmation»; reload/API/PG assertion; TEST-V3-BC-303-FLOW.
- **AC-BC-303-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-303-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-316 — Đọc ledger và lịch sử điều chỉnh tài chính

Source: BC-316/R17/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: PII redaction và scope; read-only; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-316-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đọc ledger và lịch sử điều chỉnh tài chính; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «PII redaction và scope»; reload/API/PG assertion; TEST-V3-BC-316-FLOW.
- **AC-BC-316-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-316-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-327 — Cung cấp dữ liệu cước cho hóa đơn và đối soát

Source: BC-327/R28/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không trực tiếp sửa ledger/số dư; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-327-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Cung cấp dữ liệu cước cho hóa đơn và đối soát; Then outcome lưu/đọc đúng ledger/payment/charge/invoice/receipt, giữ guard «Không trực tiếp sửa ledger/số dư»; reload/API/PG assertion; TEST-V3-BC-327-FLOW.
- **AC-BC-327-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-327-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-004-01 | Khối lượng tính phí là max(real, volumetric), unit và rounding theo version; mọi input bắt buộc được validate. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-02 | Giá gồm zone/service/tier/surcharge/discount với thứ tự xác định; không đoán giá khi thiếu rule. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-03 | Mỗi quote lưu version, canonical input, breakdown, currency, total, issuedAt, expiresAt. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-04 | Published tariff/version không update in-place; bảng giá mới tạo version mới. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-004-05 | Cache theo key gồm tariffVersion và canonical price dimensions; Redis fail fallback DB không sai cước. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/004-pricing-engine/spec.md) |
| REQ-006-01 | Wallet available không âm dưới concurrent debit; tiền biểu diễn chính xác, ownerId từ trusted context. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |
| REQ-006-02 | Ledger postings append-only, bút toán cân, balance update và ledger cùng local transaction. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |
| REQ-006-03 | Payment intent có CREATED/PENDING/UNKNOWN/SUCCEEDED/FAILED và external reference; chỉ VERIFIED SUCCEEDED được credit. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |
| REQ-006-04 | Callback xác minh chữ ký, amount, reference, state và replay; browser redirect không tự cấp tiền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |
| REQ-006-05 | Refund/adjustment tạo giao dịch và postings mới link original; quyền Finance và reason bắt buộc. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-PAYMENTS-RECEIPTS

Owner target: L018; source BC-142;BC-148;BC-088;BC-151;BC-288; latest Owner portal scope, no new BC ID. UI/channel: Payments/receipts/invoices/adjustment/refund.

Required behavior: Supported freight payer/options, exact charge/surcharge/declared fee evidence; receipt vs applicable invoice distinction; authorized dispute/refund approval/reference. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-PAYMENTS-RECEIPTS:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then No real bank/payment execution implied; callback verified/unknown handling, duplicate refund/charge denied; personal/tenant invoice access scoped; invoice policy before issue. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-PAYMENTS-RECEIPTS PLANNED_NOT_EXECUTED. Dependencies: L005;L006;L007.
