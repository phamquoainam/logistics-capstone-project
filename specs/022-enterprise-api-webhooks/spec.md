# Spec 022 — enterprise-api-webhooks

Alias lịch sử logical `L022`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `merchant`; từng BC giữ DML owner riêng.

## Outcome và boundary

Enterprise API principal và webhook. Merchant integration/delivery; Identity credential; gọi owner APIs; Sales coordination metadata thuộc merchant; no credential disclosure; status không fake integration.

Workflow: Scoped credential→quote/create/label/tracking→signed webhook/retry; revoke/cross-tenant deny. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L002, L003, L005, L007, L008, L009. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-02; D-V3-06; D-V3-08. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-018 — Quản lý API integration/webhook Merchant

Source: BC-018/R02/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Key và scope tách riêng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-018-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý API integration/webhook Merchant; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Key và scope tách riêng»; reload/API/PG assertion; TEST-V3-BC-018-FLOW.
- **AC-BC-018-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-018-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-164 — Quản lý cấu hình tích hợp và webhook trong scope Merchant

Source: BC-164/R02/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Credential secret bảo vệ; không tự cấp scope cao hơn; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-164-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý cấu hình tích hợp và webhook trong scope Merchant; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Credential secret bảo vệ»; reload/API/PG assertion; TEST-V3-BC-164-FLOW.
- **AC-BC-164-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-164-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-175 — Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp

Source: BC-175/R03/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Credential dùng integration principal, không chia sẻ human secret; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-175-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Credential dùng integration principal, không chia sẻ human secret»; reload/API/PG assertion; TEST-V3-BC-175-FLOW.
- **AC-BC-175-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-175-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-200 — Xác thực integration principal và quản lý scope API gắn Merchant

Source: BC-200/R22/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Credential tách human account; rotate, revoke, audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `identity`.

- **AC-BC-200-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xác thực integration principal và quản lý scope API gắn Merchant; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Credential tách human account»; reload/API/PG assertion; TEST-V3-BC-200-FLOW.
- **AC-BC-200-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-200-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-201 — Thiết lập endpoint và subscription webhook trạng thái

Source: BC-201/R22/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tenant bound, signature, retry, duplicate handling; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-201-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Thiết lập endpoint và subscription webhook trạng thái; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Tenant bound, signature, retry, duplicate handling»; reload/API/PG assertion; TEST-V3-BC-201-FLOW.
- **AC-BC-201-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-201-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-202 — Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website

Source: BC-202/R22/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: API idempotency key; đúng Merchant; validation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-202-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «API idempotency key»; reload/API/PG assertion; TEST-V3-BC-202-FLOW.
- **AC-BC-202-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-202-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-203 — Lấy báo giá và danh sách dịch vụ được phép qua API

Source: BC-203/R22/M04, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Áp dụng đúng hợp đồng Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-203-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Lấy báo giá và danh sách dịch vụ được phép qua API; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Áp dụng đúng hợp đồng Merchant»; reload/API/PG assertion; TEST-V3-BC-203-FLOW.
- **AC-BC-203-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-203-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-204 — Lấy nhãn và barcode của Shipment/Parcel qua API

Source: BC-204/R22/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ tài nguyên thuộc Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-204-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Lấy nhãn và barcode của Shipment/Parcel qua API; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Chỉ tài nguyên thuộc Merchant»; reload/API/PG assertion; TEST-V3-BC-204-FLOW.
- **AC-BC-204-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-204-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-205 — Tra cứu Shipment và nhận tracking events

Source: BC-205/R22/M11, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Hạn chế PII; filtering theo Merchant; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-205-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Tra cứu Shipment và nhận tracking events; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Hạn chế PII»; reload/API/PG assertion; TEST-V3-BC-205-FLOW.
- **AC-BC-205-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-205-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-206 — Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries

Source: BC-206/R22/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Quan sát idempotency; không tạo bút toán/đơn trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-206-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Quan sát idempotency»; reload/API/PG assertion; TEST-V3-BC-206-FLOW.
- **AC-BC-206-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-206-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-335 — Phối hợp onboarding và integration Merchant

Source: BC-335/R29/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự xem/cấp mọi API secrets; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-335-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Phối hợp onboarding và integration Merchant; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Không tự xem/cấp mọi API secrets»; reload/API/PG assertion; TEST-V3-BC-335-FLOW.
- **AC-BC-335-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-335-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-357 — Điều tra lỗi webhook và lịch sử retry

Source: BC-357/R32/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Idempotent retries; không tạo duplicate state; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-357-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Điều tra lỗi webhook và lịch sử retry; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Idempotent retries»; reload/API/PG assertion; TEST-V3-BC-357-FLOW.
- **AC-BC-357-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-357-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-358 — Quản lý cấu hình tích hợp theo quyền kỹ thuật

Source: BC-358/R32/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tách secret và business action; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-358-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý cấu hình tích hợp theo quyền kỹ thuật; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Tách secret và business action»; reload/API/PG assertion; TEST-V3-BC-358-FLOW.
- **AC-BC-358-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-358-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-360 — Đối chiếu lỗi đồng bộ Shipment

Source: BC-360/R32/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không sửa nghiệp vụ vượt quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-360-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Đối chiếu lỗi đồng bộ Shipment; Then outcome lưu/đọc đúng integration config/scoped credential reference/webhook delivery, giữ guard «Không sửa nghiệp vụ vượt quyền»; reload/API/PG assertion; TEST-V3-BC-360-FLOW.
- **AC-BC-360-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-360-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## REQ-AB-ENTERPRISE

Owner target: L022; source BC-018;BC-164;BC-175;BC-200;BC-201;BC-202;BC-203;BC-204;BC-205;BC-206;BC-335; latest Owner portal scope, no new BC ID. UI/channel: Scoped integration settings and machine API/webhooks.

Required behavior: Separate machine credential issue/rotate/revoke/scope/lifetime; API quote/create/label/status; signed webhooks retry/backoff/failure/reconciliation; Sales coordination only minimal status. Exact routes/DTO/permission/schema CONFIG/DECISION_REQUIRED before code.

**AC-ENTERPRISE:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Cross-tenant/credential replay/revoked machine deny; no human secret reuse; key disclosure once only per approved contract; duplicate webhook no business duplicate; sync errors reconcile source job. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-ENTERPRISE PLANNED_NOT_EXECUTED. Dependencies: L002;L003;L005;L007;L008;L009.
