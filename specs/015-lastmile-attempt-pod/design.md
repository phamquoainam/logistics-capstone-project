# Design 015 — lastmile-attempt-pod

DESIGN_CANDIDATE / runtime NOT_IMPLEMENTED. [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Orders attempt/POD/result, dispatch authority, finance collection cùng workflow.

FE: Courier scan/POD/failure/retry; Recipient đưa evidence ngoài account; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: Orders attempt/POD metadata; finance collection; storage chưa chọn. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/BLOCKED_FOR_IMPLEMENTATION. Không acceptance do promotion. Exact route/DTO/error/permission/schema: DECISION_REQUIRED trong contracts sau review; các khái niệm dưới đây không phải DDL hay wire field được tự duyệt.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-073

REQ-V3-BC-073: Ghi DELIVERED kèm POD. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi DELIVERED kèm POD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi DELIVERED kèm POD; action «Ghi DELIVERED kèm POD» có success/deny/recovery states. Authorization: Bằng chứng hợp lệ; chống trùng; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-073-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-074

REQ-V3-BC-074: Ghi DELIVERY_FAILED kèm lý do/attempt. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi DELIVERY_FAILED kèm lý do/attempt; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi DELIVERY_FAILED kèm lý do/attempt; action «Ghi DELIVERY_FAILED kèm lý do/attempt» có success/deny/recovery states. Authorization: Không xóa thất bại; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-074-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-081

REQ-V3-BC-081: Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Verified Recipient channel — Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có; action «Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có» có success/deny/recovery states. Authorization: Courier ghi nhận; không cấp role IAM; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R10, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-081-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-084

REQ-V3-BC-084: Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Verified Recipient channel — Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier; action «Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier» có success/deny/recovery states. Authorization: Nhân sự được quyền xử lý theo state; Recipient không thao tác backend; permission code DECISION_REQUIRED (no runtime allowlist expansion); PUBLIC_REDACTED. Test fixtures riêng: valid R10, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-084-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-227

REQ-V3-BC-227: Ghi nhận bằng chứng và tăng số lần giao thất bại. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Ghi nhận bằng chứng và tăng số lần giao thất bại; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Ghi nhận bằng chứng và tăng số lần giao thất bại; action «Ghi nhận bằng chứng và tăng số lần giao thất bại» có success/deny/recovery states. Authorization: Lý do, timestamp, evidence, attempt ID độc lập; không xóa history; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R09, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-227-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-299

REQ-V3-BC-299: Đối chiếu delivery attempts và bằng chứng POD. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đối chiếu delivery attempts và bằng chứng POD; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Đối chiếu delivery attempts và bằng chứng POD; action «Đối chiếu delivery attempts và bằng chứng POD» có success/deny/recovery states. Authorization: Chứng cứ nguồn gốc; xử lý tranh chấp; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R15, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-299-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-355

REQ-V3-BC-355: Không được giả POD/DELIVERED. Backend application boundary: orders xử lý delivery attempt/POD/instructions; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Không được giả POD/DELIVERED; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: orders; resource delivery attempt/POD/instructions. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Không được giả POD/DELIVERED; action «Không được giả POD/DELIVERED» có success/deny/recovery states. Authorization: Không bypass chứng cứ dù là Super Admin; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R31, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-355-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-AB-RECIPIENT

Trace REQ-AB-RECIPIENT. Verified scoped delivery response/Courier/CS; BE/application: Notify/limited tracking, POD/OTP when policy, requested reschedule/instructions/alternate receiver/refusal/address change request; no forced IAM; authorized workflow owns final change. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Token cannot cancel/alter COD/final shipment facts or grant Merchant authority; expired/replayed verification denied; approved request produces separate attempt/event with evidence; current guard và failure/replay/recovery, no mock-only success.
