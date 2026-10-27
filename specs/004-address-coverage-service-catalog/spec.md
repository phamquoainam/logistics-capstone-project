# Spec 004 — address-coverage-service-catalog

Alias lịch sử logical `L004`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `merchant`; từng BC giữ DML owner riêng.

## Outcome và boundary

Địa chỉ/điểm lấy và catalog public. Catalog phiên bản, địa chỉ/coverage/điểm phục vụ; routing adapter theo contract.

Workflow: Validate→xác nhận tọa độ→lưu/reload; timeout/mơ hồ chưa confirmed. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L001, L003. Consumer/soft: L011. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-03; DEC-05; DEC-06. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-001 — Tra cứu vùng phục vụ công khai

Source: BC-001/R01/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `merchant`.

- **AC-BC-001-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Tra cứu vùng phục vụ công khai; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Chỉ dữ liệu công khai»; reload/API/PG assertion; TEST-V3-BC-001-FLOW.
- **AC-BC-001-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-001-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-005 — Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động

Source: BC-005/R01/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dữ liệu công khai; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PUBLIC_REDACTED. Owner `merchant`.

- **AC-BC-005-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Dữ liệu công khai»; reload/API/PG assertion; TEST-V3-BC-005-FLOW.
- **AC-BC-005-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-005-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-010 — Quản lý địa chỉ lấy hàng

Source: BC-010/R02/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Catalog phiên bản và kiểm tra dữ liệu; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-010-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý địa chỉ lấy hàng; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Catalog phiên bản và kiểm tra dữ liệu»; reload/API/PG assertion; TEST-V3-BC-010-FLOW.
- **AC-BC-010-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-010-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-022 — Chuẩn hóa địa chỉ nhận/gửi

Source: BC-022/R03/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Dữ liệu hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-022-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Chuẩn hóa địa chỉ nhận/gửi; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Dữ liệu hợp lệ»; reload/API/PG assertion; TEST-V3-BC-022-FLOW.
- **AC-BC-022-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-022-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-033 — Quản lý vùng phục vụ theo policy

Source: BC-033/R04/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Có phiên bản và audit; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-033-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý vùng phục vụ theo policy; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Có phiên bản và audit»; reload/API/PG assertion; TEST-V3-BC-033-FLOW.
- **AC-BC-033-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-033-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-136 — Lưu và quản lý địa chỉ gửi/nhận thường dùng

Source: BC-136/R18/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ sở hữu cá nhân; chuẩn hóa địa chỉ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `merchant`.

- **AC-BC-136-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Lưu và quản lý địa chỉ gửi/nhận thường dùng; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Chỉ sở hữu cá nhân»; reload/API/PG assertion; TEST-V3-BC-136-FLOW.
- **AC-BC-136-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-136-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-137 — Kiểm tra vùng phục vụ theo tuyến gửi lẻ

Source: BC-137/R18/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Coverage snapshot/quy tắc hợp lệ; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Owner `merchant`.

- **AC-BC-137-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Kiểm tra vùng phục vụ theo tuyến gửi lẻ; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Coverage snapshot/quy tắc hợp lệ»; reload/API/PG assertion; TEST-V3-BC-137-FLOW.
- **AC-BC-137-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-137-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-271 — Quản lý vùng giao nhận và coverage

Source: BC-271/R27/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Versioning/policy, không hồi tố đơn đã chốt tùy ý; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-271-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Quản lý vùng giao nhận và coverage; Then outcome lưu/đọc đúng versioned address/catalog/coverage, giữ guard «Versioning/policy, không hồi tố đơn đã chốt tùy ý»; reload/API/PG assertion; TEST-V3-BC-271-FLOW.
- **AC-BC-271-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-271-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-003-01 | Pickup point thuộc merchant; user chỉ CRUD điểm được cấp quyền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) |
| REQ-003-02 | Lưu địa chỉ raw, phần có cấu trúc, admin-catalog version, coordinate source/confidence và thời điểm xác nhận. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) |
| REQ-003-03 | Không mặc định geocoding thành công khi provider trả thiếu/mơ hồ; có nhánh chọn tay xác nhận. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) |
| REQ-003-05 | Không dùng NLP để sửa địa chỉ phi cấu trúc trong P0. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) |
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


### REQ-V3-BC-009 — Quản lý pickup point

Source: BC-009/R02/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Tenant isolation; archive/sửa có điều kiện; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-009-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý pickup point; Then outcome lưu/đọc đúng Merchant pickup point/address validation reference, giữ guard «Tenant isolation»; reload/API/PG assertion; TEST-V3-BC-009-FLOW.
- **AC-BC-009-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-009-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.


### REQ-V3-BC-021 — Xem và sửa pickup point được ủy quyền

Source: BC-021/R03/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không quản lý nhân viên mặc định; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-021-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Xem và sửa pickup point được ủy quyền; Then outcome lưu/đọc đúng delegated Merchant pickup point/address reference, giữ guard «Không quản lý nhân viên mặc định»; reload/API/PG assertion; TEST-V3-BC-021-FLOW.
- **AC-BC-021-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-021-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.


### REQ-V3-BC-168 — Quản lý pickup point trong phạm vi ủy quyền

Source: BC-168/R19/M03, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant/branch scope; không truy cập tenant khác; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-168-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý pickup point trong phạm vi ủy quyền; Then outcome lưu/đọc đúng delegated Manager pickup point/address reference, giữ guard «Merchant/branch scope»; reload/API/PG assertion; TEST-V3-BC-168-FLOW.
- **AC-BC-168-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-168-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.
