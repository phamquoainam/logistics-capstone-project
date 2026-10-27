# Spec 024 — partner-franchise-station-ops

Alias lịch sử logical `L024`. CANONICAL_TARGET / NOT_STARTED / BLOCKED_FOR_IMPLEMENTATION. Owner accountable: `dispatch`; từng BC giữ DML owner riêng.

## Outcome và boundary

Partner chỉ vận hành điểm được giao. Partner binding qua merchant/identity API; dispatch custody/counter.

Workflow: Assigned staff→counter/scan/count/handover; foreign Station deny. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L003, L011. Consumer/soft: none. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: D-V3-02; D-V3-05; D-V3-08. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-364 — Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định

Source: BC-364/R33/M05, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Partner+Station scope, xác minh walk-in; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-364-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Partner+Station scope, xác minh walk-in»; reload/API/PG assertion; TEST-V3-BC-364-FLOW.
- **AC-BC-364-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-364-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-365 — Bàn giao kiện cho hãng vận chuyển

Source: BC-365/R33/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Actor, count, manifest, timestamp, custody proof; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-365-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Bàn giao kiện cho hãng vận chuyển; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Actor, count, manifest, timestamp, custody proof»; reload/API/PG assertion; TEST-V3-BC-365-FLOW.
- **AC-BC-365-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-365-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-366 — Xác nhận số lượng thực giao/nhận

Source: BC-366/R33/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Sai lệch phải ghi event và investigation; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-366-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Xác nhận số lượng thực giao/nhận; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Sai lệch phải ghi event và investigation»; reload/API/PG assertion; TEST-V3-BC-366-FLOW.
- **AC-BC-366-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-366-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-367 — Quản lý tồn kiện ở điểm đại lý

Source: BC-367/R33/M08, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không đọc tồn nơi khác; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-367-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Quản lý tồn kiện ở điểm đại lý; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Không đọc tồn nơi khác»; reload/API/PG assertion; TEST-V3-BC-367-FLOW.
- **AC-BC-367-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-367-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-368 — Xem doanh thu dịch vụ thuộc điểm được giao

Source: BC-368/R33/M13, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ báo cáo được phép; không sửa settlement; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-368-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Xem doanh thu dịch vụ thuộc điểm được giao; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Chỉ báo cáo được phép»; reload/API/PG assertion; TEST-V3-BC-368-FLOW.
- **AC-BC-368-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-368-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-369 — Ràng buộc Partner Organization với Station

Source: BC-369/R33/M17, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Explicit membership/scope; no network-wide access; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-369-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Ràng buộc Partner Organization với Station; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Explicit membership/scope»; reload/API/PG assertion; TEST-V3-BC-369-FLOW.
- **AC-BC-369-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-369-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-370 — Quét/kiểm tra nhãn và tình trạng kiện

Source: BC-370/R33/M06, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không giả mạo scan của hãng; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-370-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Quét/kiểm tra nhãn và tình trạng kiện; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Không giả mạo scan của hãng»; reload/API/PG assertion; TEST-V3-BC-370-FLOW.
- **AC-BC-370-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-370-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-371 — Truy vết nghiệp vụ điểm đối tác

Source: BC-371/R33/M18, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không xóa handover history; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PARTNER_ORG_STATION. Owner `dispatch`.

- **AC-BC-371-SUCCESS:** Given current subject và resource đúng PARTNER_ORG_STATION; When Truy vết nghiệp vụ điểm đối tác; Then outcome lưu/đọc đúng Partner point/assignment/custody, giữ guard «Không xóa handover history»; reload/API/PG assertion; TEST-V3-BC-371-FLOW.
- **AC-BC-371-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-371-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.
