# Spec 019 — cod-reconciliation-merchant-settlement

Alias lịch sử logical `L019`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `finance`; từng BC giữ DML owner riêng.

## Outcome và boundary

Reconciliation và settlement có điều kiện. Eligibility recheck/unique closed membership/cutoff/dispute/payout evidence.

Workflow: Delivered+collected+confirmed remitted→reconcile→simulated payout; late data kỳ sau. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L017, L018. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: DEC-19; D-V3-04. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-015 — Xem phí, bảng kê và settlement

Source: BC-015/R02/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-015-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem phí, bảng kê và settlement; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Chỉ thuộc Merchant»; reload/API/PG assertion; TEST-V3-BC-015-FLOW.
- **AC-BC-015-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-015-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-087 — Xem bảng kê đối soát và thanh toán

Source: BC-087/R11/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa sổ cái; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-087-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem bảng kê đối soát và thanh toán; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Không sửa sổ cái»; reload/API/PG assertion; TEST-V3-BC-087-FLOW.
- **AC-BC-087-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-087-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-091 — Đối chiếu/ghi nhận xác nhận bảng kê

Source: BC-091/R11/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự quyết toán phía Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-091-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Đối chiếu/ghi nhận xác nhận bảng kê; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Không tự quyết toán phía Logistics»; reload/API/PG assertion; TEST-V3-BC-091-FLOW.
- **AC-BC-091-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-091-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-099 — Tạo bảng kê và kỳ thanh toán Merchant

Source: BC-099/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có quyền phê duyệt phù hợp; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-099-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Tạo bảng kê và kỳ thanh toán Merchant; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Có quyền phê duyệt phù hợp»; reload/API/PG assertion; TEST-V3-BC-099-FLOW.
- **AC-BC-099-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-099-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-100 — Ghi nhận settlement đã thực hiện

Source: BC-100/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả định đã chuyển tiền khi chưa có bằng chứng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-100-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Ghi nhận settlement đã thực hiện; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Không giả định đã chuyển tiền khi chưa có bằng chứng»; reload/API/PG assertion; TEST-V3-BC-100-FLOW.
- **AC-BC-100-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-100-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-162 — Xem lịch thanh toán, hóa đơn và chứng từ COD/cước

Source: BC-162/R02/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ Merchant được sở hữu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-162-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem lịch thanh toán, hóa đơn và chứng từ COD/cước; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Chỉ Merchant được sở hữu»; reload/API/PG assertion; TEST-V3-BC-162-FLOW.
- **AC-BC-162-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-162-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-188 — Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả

Source: BC-188/R11/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Ledger/statement không được sửa âm thầm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-188-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Ledger/statement không được sửa âm thầm»; reload/API/PG assertion; TEST-V3-BC-188-FLOW.
- **AC-BC-188-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-188-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-190 — Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ

Source: BC-190/R11/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự xác nhận hãng đã trả; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `finance`.

- **AC-BC-190-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Không tự xác nhận hãng đã trả»; reload/API/PG assertion; TEST-V3-BC-190-FLOW.
- **AC-BC-190-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-190-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-287 — Xác định COD đủ điều kiện trả Merchant

Source: BC-287/R13/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Theo hold, fees, disputes, policy, approvals; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `finance`.

- **AC-BC-287-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Xác định COD đủ điều kiện trả Merchant; Then outcome lưu/đọc đúng statement/closed-period snapshot/payout evidence, giữ guard «Theo hold, fees, disputes, policy, approvals»; reload/API/PG assertion; TEST-V3-BC-287-FLOW.
- **AC-BC-287-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-287-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-006-05 | Refund/adjustment tạo giao dịch và postings mới link original; quyền Finance và reason bắt buộc. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/006-wallet-payments/spec.md) |
| REQ-011-01 | Phân biệt COLLECTED/REMITTED/RECONCILED, không coi shipper submit là xác nhận đã nộp. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |
| REQ-011-04 | Chỉ COD delivered+collected+fully confirmed remitted+no dispute+eligible cutoff mới đóng kỳ; mỗi COD chỉ một closed period. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |
| REQ-011-05 | Closed period/items/totals immutable; adjustment và simulated payout có source ref, quyền, idempotency. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/011-cod-reconciliation/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-FINANCIAL-PORTAL

Owner target: L019; source BC-015;BC-087;BC-091;BC-162;BC-188;BC-190; latest Owner portal scope, no new BC ID. UI/channel: COD statements/payout schedules/discrepancy.

Required behavior: Expected/collected/held/remitted/reconciled/settled distinct; fees/receivables/payables/payout references, statement confirmation and dispute links; invoice source018. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-FINANCIAL-PORTAL:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Statement totals tie to immutable ledger/allocation/eligible closed snapshots; Merchant acknowledgment not carrier settlement; late data new adjustment; sandbox payout accurately labeled. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-FINANCIAL-PORTAL PLANNED_NOT_EXECUTED. Dependencies: L017;L018;L020.
