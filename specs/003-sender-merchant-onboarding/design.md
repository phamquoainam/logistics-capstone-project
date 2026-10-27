# Design 003 — sender-merchant-onboarding

OWNER_REVIEWED_PLANNING_BASELINE / runtime NOT_IMPLEMENTED. See [focused technical review](contracts/technical-review.md). [Spec](spec.md), [tasks](tasks.md).

## Construction

BE: Customer onboarding phối hợp L002; Merchant application/legal evidence/manual review; controlled org/Owner activation; membership/delegation; pickup management chỉ qua L004.

FE: Personal default/profile/application/status; Merchant context/profile/facilities/staff; pickup navigation L004; no Guest type chooser; source app/frontend/web, native app/frontend/shipper-mobile chỉ khi applicable.

Data: merchant application/org/Merchant business profile/membership/delegation; Retail Sender profile authority customer_profile (Owner DATA-01), independent of org/membership; physical candidate merchant module; pickup data L004; Identity verified contacts/credentials; exact schema pending. Một artifact/chín domain; domain api/internal, workflow chỉ public API. Identity không phụ thuộc compile-time vòng sang Merchant; authority bridge phải được review trước code; không sửa ArchUnit để cho import nội bộ.

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION. Exact route/DTO/error/permission/schema follow the reviewed planning contracts and technical-review.md; no runtime acceptance, DDL or production authorization from documentation.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Responsibility từng capability

### DES-BC-007

REQ-V3-BC-007: Dùng tài khoản với Merchant membership ACTIVE. Backend application boundary: merchant xử lý current organization membership/context; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Dùng tài khoản với Merchant membership ACTIVE; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource current organization membership/context. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Dùng tài khoản với Merchant membership ACTIVE; action «Dùng tài khoản với Merchant membership ACTIVE» có success/deny/recovery states. Authorization: Đúng org và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-007-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-008

REQ-V3-BC-008: Quản lý hồ sơ và nhân viên Merchant. Backend application boundary: merchant xử lý organization business profile and scoped staff directory; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý hồ sơ và nhân viên Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource organization business profile and scoped staff directory. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý hồ sơ và nhân viên Merchant; action «Quản lý hồ sơ và nhân viên Merchant» có success/deny/recovery states. Authorization: Đúng quyền quản trị tổ chức; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-008-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-020

REQ-V3-BC-020: Truy cập qua membership Merchant. Backend application boundary: merchant xử lý current delegated business membership/context; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Truy cập qua membership Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource current delegated business membership/context. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Truy cập qua membership Merchant; action «Truy cập qua membership Merchant» có success/deny/recovery states. Authorization: Không tự nâng quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R03, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-020-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-032

REQ-V3-BC-032: Duyệt/giám sát Merchant theo policy. Backend application boundary: merchant xử lý application review decision/org lifecycle; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Duyệt/giám sát Merchant theo policy; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource application review decision/org lifecycle. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Duyệt/giám sát Merchant theo policy; action «Duyệt/giám sát Merchant theo policy» có success/deny/recovery states. Authorization: Phạm vi org/region; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R04, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-032-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-045

REQ-V3-BC-045: Sửa nội dung nghiệp vụ Merchant. Backend application boundary: merchant xử lý Merchant business records denied to Identity-only authority; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Sửa nội dung nghiệp vụ Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource Merchant business records denied to Identity-only authority. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Internal scoped channel — Sửa nội dung nghiệp vụ Merchant; action «Sửa nội dung nghiệp vụ Merchant» có success/deny/recovery states. Authorization: Không tự có quyền Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion); ASSIGNMENT_STATION_HUB_REGION_CASE. Test fixtures riêng: valid R05, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-045-SUCCESS/DENY/RECOVERY và no PII leak. Không tạo positive endpoint/role grant cho prohibition.

### DES-BC-132

REQ-V3-BC-132: Đi đến đăng ký một tài khoản khách hàng cá nhân. Backend application boundary: merchant xử lý public personal-registration entry; no business record from navigation; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Đi đến đăng ký một tài khoản khách hàng cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource public personal-registration entry; no business record from navigation. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Public — personal registration/login entry only; action «Đi đến đăng ký một tài khoản khách hàng cá nhân» có success/deny/recovery states. Authorization: Public entry; xác minh Identity; không chọn loại tài khoản, không tự cấp role/Owner; permission code DECISION_REQUIRED; PUBLIC_REDACTED. Test fixtures riêng: valid R01, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-132-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-133

REQ-V3-BC-133: Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập. Backend application boundary: merchant xử lý owned application/evidence revisions/review status, no Owner before approval; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource owned application/evidence revisions/review status, no Owner before approval. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Personal authenticated — Merchant application and application status; action «Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập» có success/deny/recovery states. Authorization: Account verified và eligible hiện hành; không Guest submit, không Owner trước approval; permission code DECISION_REQUIRED; PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-133-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-134

REQ-V3-BC-134: Đăng ký/đăng nhập tài khoản cá nhân. Backend application boundary: Spec003 điều phối onboarding/UI qua public API Identity 002 đã được duyệt; Identity sở hữu account/authoritative ID, credentials/passwords, verification/activation/recovery, verified security contacts, sessions/refresh/revocation và security lifecycle. Merchant không lưu bản sao security resources, không cross-module SQL/repository. Query lọc trước paging; command recheck current authority tại commit.

API cần: Đăng ký/đăng nhập tài khoản cá nhân; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Resource authority theo khai báo dưới đây và [policy](../../docs/reconciliation/v3/resource-authority-policy.json); đây là stable resource IDs cho tài liệu, không phải tên bảng/wire fields. Identity 002 review mọi persistence/security delta qua controlled amendment; Spec003 chỉ gọi contract, không tạo account/verification/session tables. Merchant giữ application/org/business profile/membership/delegation; orchestration không cấp data authority.

UI/channel: Public registration/verification/login → Personal default; action «Đăng ký/đăng nhập tài khoản cá nhân» có success/deny/recovery states. Authorization: Không buộc Merchant membership; xác minh/chống giả mạo; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-134-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

```ownership-json
{
  "scope": "BC-134", "orchestrator": "spec003",
  "resources": [
    {"resource_id": "identity.account", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.credentials", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.verification", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.security_contact", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null},
    {"resource_id": "identity.session", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null}
  ]
}
```

### DES-BC-135

REQ-V3-BC-135: Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản. Backend application boundary: Spec003 điều phối self business profile/contact preferences; customer_profile sở hữu Retail Sender business profile độc lập org/membership theo Owner DATA-01; Identity security contacts authoritative; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Resource authority theo khai báo dưới đây; Retail Sender business profile authority customer_profile đã được Owner DATA-01 duyệt, không là Merchant tenant resource; physical candidate module merchant chỉ là vị trí kỹ thuật. Identity giữ verified security contacts/account status; không suy ra Identity sở hữu business profile. Schema/API candidate tại contracts/data-model.md và contracts/openapi.candidate.json cần acceptance trước code.

UI/channel: Personal — Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản; action «Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản» có success/deny/recovery states. Authorization: Chỉ dữ liệu bản thân; thay đổi nhạy cảm cần xác minh; permission code DECISION_REQUIRED (no runtime allowlist expansion); PERSONAL_OWNER. Test fixtures riêng: valid R18, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-135-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

```ownership-json
{
  "scope": "BC-135", "orchestrator": "spec003",
  "resources": [
    {"resource_id": "retail_sender.business_profile", "authoritative_owner": "customer_profile", "resolution": "OWNER_POLICY_ACCEPTED", "decision": "DATA-01"},
    {"resource_id": "identity.security_contact", "authoritative_owner": "identity", "resolution": "DECLARED", "decision": null}
  ]
}
```

### DES-BC-157

REQ-V3-BC-157: Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant. Backend application boundary: merchant xử lý organization legal/business profile; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource organization legal/business profile. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant; action «Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant» có success/deny/recovery states. Authorization: Chỉ Merchant sở hữu; kiểm soát thay đổi nhạy cảm; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-157-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-158

REQ-V3-BC-158: Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức. Backend application boundary: merchant xử lý membership invitation/current member status; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource membership invitation/current member status. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức; action «Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức» có success/deny/recovery states. Authorization: Không tự cấp quyền nền tảng; phân quyền trong giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-158-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-159

REQ-V3-BC-159: Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định. Backend application boundary: merchant xử lý branch/facility metadata and versioned defaults referencing supported catalog; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource branch/facility metadata and versioned defaults referencing supported catalog. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định; action «Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định» có success/deny/recovery states. Authorization: Merchant scope; chính sách phiên bản; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R02, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-159-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-166

REQ-V3-BC-166: Quản lý nhân sự và quyền hạn được Owner ủy quyền. Backend application boundary: merchant xử lý current delegated staff/permission assignment; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Quản lý nhân sự và quyền hạn được Owner ủy quyền; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource current delegated staff/permission assignment. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Quản lý nhân sự và quyền hạn được Owner ủy quyền; action «Quản lý nhân sự và quyền hạn được Owner ủy quyền» có success/deny/recovery states. Authorization: Không vượt delegated scope; audit thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-166-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

### DES-BC-167

REQ-V3-BC-167: Điều hành cửa hàng/kho và cấu hình vận hành được giao. Backend application boundary: merchant xử lý delegated branch/facility/defaults, no Owner/bank edits; HTTP/machine/channel adapter gọi public API, không cross-module repository. Query lọc trước paging; command check current state tại commit.

API cần: Điều hành cửa hàng/kho và cấu hình vận hành được giao; input validation, result tối thiểu, lỗi/conditional/recovery được duyệt trước code. Data owner: merchant; resource delegated branch/facility/defaults, no Owner/bank edits. DB schema/unique business key/version/transition exact cần contract review; không auto tạo bảng theo label.

UI/channel: Merchant current context — Điều hành cửa hàng/kho và cấu hình vận hành được giao; action «Điều hành cửa hàng/kho và cấu hình vận hành được giao» có success/deny/recovery states. Authorization: Không đổi Merchant owner hoặc ngân hàng COD; permission code DECISION_REQUIRED (no runtime allowlist expansion); MERCHANT_ORG_BRANCH. Test fixtures riêng: valid R19, wrong personal/tenant, revoked scope, invalid state, duplicate/conflict/race nếu mutation, unavailable dependency; assert AC-BC-167-SUCCESS/DENY/RECOVERY và no PII leak. Consumer responsibilities giữ source owner; state change chỉ khi operation đã duyệt.

## Legacy integration

Các obligation ở spec phải có contract tests/DB assertions tương ứng; bảng ánh xạ lịch sử không tự tạo API/DDL. [Retirement](../../docs/reconciliation/v3/legacy-retirement.md).


## DES-003-ACCOUNT/APPLICATION/ACTIVATION/MEMBERSHIP/CONTEXT/FACILITIES

Business source of truth Merchant: personal business profile, application/evidence metadata/revisions/review decisions, org/facilities/defaults, membership/invitation/delegation/audit receipts. Identity: verified security contacts, authoritative account/current session, credentials/proofs/recovery. Secure file bytes qua approved private storage adapter; không chọn provider hoặc public evidence URL. Exact names/fields/indexes/contracts DECISION_REQUIRED. Branch metadata không sở hữu warehouse stock; registered application address không serviceability proof.

Operation contracts cần được duyệt: self-profile query/update; own application create/save/submit/resubmit/status/withdraw; scoped review list/detail/request-info/reject/approve; current contexts; org profile/facilities/defaults; invite/accept/reject/revoke/suspend/reinstate/delegate/leave; suspension/closure. Không đặt fabricated routes/permission codes. Contract phải có input allowlist, no actor/owner/verified/status patch, field errors, safe pagination, revision/conditional token, scoped idempotency/recovery, minimal read DTO, redaction và audit reason.

Approval atomic Merchant transaction: current subject/eligibility protocol được review, application/reviewer/business-key/last-owner locks → org+initial member+application outcome+audit+receipt/outbox cùng commit. No identity DML, no service call external trong TX; không bypass Identity session-owned transaction. Review action carries reason/source/correlation; eligibility/revoke interleaving phải có proven locking/version protocol, không check-then-write bằng cached JWT.

Evidence: type-specific required categories/issuer/operator assertion, digest/version/upload status; no malware/formula execution, file MIME/signature/size/count/retention config reviewed; verify file ready before submission. Manual sandbox verification ghi reviewer/process/reason/time, không false authority integration claim. Missing audit, evidence read denied, provider timeout, encryption unavailable đều fail closed/pending đúng contract.

FE screens: public registration/verification/login/Recovery consume002; personal profile/security and own application/status; Merchant context switch/profile/facilities/staff/delegation; Business Admin review queue/detail/decision; permission-aware navigation, no Guest Merchant form submit. Confirmations/unsaved changes, field errors, accessible focus/keyboard, no secrets in URL/storage/logs. Cache partition by authoritative subject/context, generation guard ignores stale previous-context responses; logout/revoke clears sensitive cached state.

Fixtures: customer A/B active/unverified/disabled; org A/B ACTIVE/SUSPENDED; pending/rejected/needs-information applications with immutable revisions; valid/invalid representative proof; current/expired/revoked invitation; delegated Manager/Operator; Identity-only admin; Business Admin same/outside scope/self-review. Tests include exact state matrix, race approval/duplicate/last Owner, rollback audit/write/outbox, revoke/account disable before commit, stale context response, contact reverify, evidence unauthorized/retention, empty+upgrade roles/indexes/constraints and previous application compatibility. Native camera/GPS/offline không003 scope.


## DES-AB-ENTRY

Trace REQ-AB-ENTRY. Public/personal entry; BE/application: Public landing/register/login/recovery navigation; one personal registration, personal default; Merchant application only authenticated; no dead-end Guest Merchant form. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert Guest cannot submit application; verified personal login default; deep links rechecked; no role grants; current guard và failure/replay/recovery, no mock-only success.


## DES-AB-CONTEXT

Trace REQ-AB-CONTEXT. Context switch/profile/staff; BE/application: Accessible organizations only; active context indicator; partition/clear cache, cancel or ignore stale previous-context responses; preserve personal access after org rejection/revoke. Data DML giữ domain owner theo từng BC và cross-module contract, không duplicate master/projection authority. Contract result/errors/paging/version/key/permission reviewed before implement; wire/DB names not invented. Tests assert A→B switch cannot show A response in B; revoked org cleared and current request denied; personal still valid; current guard và failure/replay/recovery, no mock-only success.

## Controlled technical design for final DoR review

Owner six policies are recorded at [decision](../../docs/decisions/spec003-six-decisions.md). Use [exact API/DTO/errors](contracts/openapi.candidate.json), [authority/state/config](contracts/policy.json), [physical data candidates](contracts/data-model.md), [TX/recovery/privacy/UI contract](contracts/README.md) and [minimum owner contracts](contracts/minimum-dependencies.md). These detailed candidates supersede earlier per-capability generic “exact contract pending” placeholders; approval is still required, not another general design cycle.

Physical proposal: distinct customer-profile and tenant aggregates within existing merchant.api/internal application/domain/infrastructure layers. No organization/membership FK or guard on customer_profile. Identity remains sole security master. New customer admission owns the coordinated transaction and rejects ambient callers; accepted existing session store/HTTP SYSTEM paths stay unchanged. Workflow composition only public APIs; direct Merchant→Pricing dependency and Identity→Merchant imports remain forbidden. Prototype/Demo source is not production acceptance.

Typed per-capability declarations above use stable logical authority IDs. `authority_modules.customer_profile=merchant` is a physical placement candidate, not Merchant tenant authority. The exact13 CORE+DENY045 frontend/API/DB/test/task mappings are [coverage](contracts/coverage.csv). All production operation names/DTO/table/permission symbols in the candidate package await explicit acceptance; no SQL or code changed here.
