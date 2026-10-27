# Spec 003 — sender-merchant-onboarding

Alias lịch sử logical `L003`. CANONICAL_TARGET / NOT_STARTED / SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION. Owner accountable: `merchant`; từng BC giữ DML owner riêng.

## Outcome và boundary

Hồ sơ cá nhân, Merchant application/approval và organization/membership. Customer onboarding phối hợp L002; Merchant application/legal evidence/manual review; controlled org/Owner activation; membership/delegation; pickup management chỉ qua L004.

Workflow: Guest đăng ký cá nhân→Identity verify/login→personal workspace→Merchant apply→manual sandbox review→approval+Owner activation→context; rejection/revocation giữ personal access. Ngoài scope: Không cấp quyền từ role label; không ghi dữ liệu owner khác;5 DEFER chỉ denial. CORE wave sau vẫn trong target.; downstream thuộc owner register, không duplicate mutation.

Hard minimum contracts: L002. Consumer/soft: L004, L005. Hard là dependency chức năng tối thiểu đã chứng minh, không buộc cả spec upstream accepted. Soft không cho phép fake missing facts.

Blockers trước first use: IAM-CUSTOMER-01; IAM-CONTEXT-02; IAM-INVITE-03; DATA-01; PRIVACY-01; POLICY-003-01. [Decision register](../../docs/reconciliation/v3/decisions.md).

## Requirements và acceptance từng BC

### REQ-V3-BC-007 — Dùng tài khoản với Merchant membership ACTIVE

Source: BC-007/R02/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng org và trạng thái; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-007-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Dùng tài khoản với Merchant membership ACTIVE; Then outcome lưu/đọc đúng current organization membership/context, giữ guard «Đúng org và trạng thái»; reload/API/PG assertion; TEST-V3-BC-007-FLOW.
- **AC-BC-007-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-007-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-008 — Quản lý hồ sơ và nhân viên Merchant

Source: BC-008/R02/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Đúng quyền quản trị tổ chức; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-008-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý hồ sơ và nhân viên Merchant; Then outcome lưu/đọc đúng organization business profile and scoped staff directory, giữ guard «Đúng quyền quản trị tổ chức»; reload/API/PG assertion; TEST-V3-BC-008-FLOW.
- **AC-BC-008-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-008-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-020 — Truy cập qua membership Merchant

Source: BC-020/R03/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự nâng quyền; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-020-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Truy cập qua membership Merchant; Then outcome lưu/đọc đúng current delegated business membership/context, giữ guard «Không tự nâng quyền»; reload/API/PG assertion; TEST-V3-BC-020-FLOW.
- **AC-BC-020-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-020-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-032 — Duyệt/giám sát Merchant theo policy

Source: BC-032/R04/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Phạm vi org/region; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-032-SUCCESS:** Given current subject và resource đúng ASSIGNMENT_STATION_HUB_REGION_CASE; When Duyệt/giám sát Merchant theo policy; Then outcome lưu/đọc đúng application review decision/org lifecycle, giữ guard «Phạm vi org/region»; reload/API/PG assertion; TEST-V3-BC-032-FLOW.
- **AC-BC-032-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-032-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-045 — Sửa nội dung nghiệp vụ Merchant

Source: BC-045/R05/M02, DEFER; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự có quyền Logistics; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: ASSIGNMENT_STATION_HUB_REGION_CASE. Owner `merchant`.

- **AC-BC-045-SUCCESS:** Given actor chỉ có authority nguồn; When gọi trực tiếp hành vi Sửa nội dung nghiệp vụ Merchant; Then server deny, không đổi dữ liệu nghiệp vụ/không lộ private data; TEST-V3-BC-045-DENY.
- **AC-BC-045-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-045-RECOVERY:** Deny lặp vẫn không tạo positive effect; kiểm tra DB trước/sau và không có UI action cấp quyền.

### REQ-V3-BC-132 — Đi đến đăng ký một tài khoản khách hàng cá nhân

Source: BC-132/R01/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Public entry; xác minh Identity; không chọn loại tài khoản, không tự cấp role/Owner; permission code DECISION_REQUIRED. Scope: PUBLIC_REDACTED. Owner `merchant`.

- **AC-BC-132-SUCCESS:** Given current subject và resource đúng PUBLIC_REDACTED; When Đi đến đăng ký một tài khoản khách hàng cá nhân; Then outcome lưu/đọc đúng public personal-registration entry; no business record from navigation, giữ guard «Public entry»; reload/API/PG assertion; TEST-V3-BC-132-FLOW.
- **AC-BC-132-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-132-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-133 — Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập

Source: BC-133/R18/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Account verified và eligible hiện hành; không Guest submit, không Owner trước approval; permission code DECISION_REQUIRED. Scope: PERSONAL_OWNER. Owner `merchant`.

- **AC-BC-133-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập; Then outcome lưu/đọc đúng owned application/evidence revisions/review status, no Owner before approval, giữ guard «Account verified và eligible hiện hành»; reload/API/PG assertion; TEST-V3-BC-133-FLOW.
- **AC-BC-133-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-133-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-134 — Đăng ký/đăng nhập tài khoản cá nhân

Source: BC-134/R18/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không buộc Merchant membership; xác minh/chống giả mạo; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Identity `identity` sở hữu authoritative security resources; Spec003/Merchant điều phối workflow/UI qua approved public API, không sở hữu persistence Identity.

- **AC-BC-134-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Đăng ký/đăng nhập tài khoản cá nhân; Then outcome lưu/đọc đúng Identity customer verification/account/session; Merchant orchestrates personal onboarding, no credential copy, giữ guard «Không buộc Merchant membership»; reload/API/PG assertion; TEST-V3-BC-134-FLOW.
- **AC-BC-134-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-134-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-135 — Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản

Source: BC-135/R18/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ dữ liệu bản thân; thay đổi nhạy cảm cần xác minh; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: PERSONAL_OWNER. Retail Sender business-data authority `customer_profile` được Owner DATA-01 duyệt, self scope độc lập org/member; physical module candidate `merchant` không là tenant authority; Identity giữ verified security contacts/account security state.

- **AC-BC-135-SUCCESS:** Given current subject và resource đúng PERSONAL_OWNER; When Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản; Then outcome lưu/đọc đúng self business profile/contact preferences; Identity security contacts authoritative, giữ guard «Chỉ dữ liệu bản thân»; reload/API/PG assertion; TEST-V3-BC-135-FLOW.
- **AC-BC-135-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-135-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-157 — Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant

Source: BC-157/R02/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Chỉ Merchant sở hữu; kiểm soát thay đổi nhạy cảm; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-157-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant; Then outcome lưu/đọc đúng organization legal/business profile, giữ guard «Chỉ Merchant sở hữu»; reload/API/PG assertion; TEST-V3-BC-157-FLOW.
- **AC-BC-157-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-157-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-158 — Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức

Source: BC-158/R02/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không tự cấp quyền nền tảng; phân quyền trong giới hạn; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-158-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức; Then outcome lưu/đọc đúng membership invitation/current member status, giữ guard «Không tự cấp quyền nền tảng»; reload/API/PG assertion; TEST-V3-BC-158-FLOW.
- **AC-BC-158-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-158-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-159 — Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định

Source: BC-159/R02/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Merchant scope; chính sách phiên bản; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-159-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định; Then outcome lưu/đọc đúng branch/facility metadata and versioned defaults referencing supported catalog, giữ guard «Merchant scope»; reload/API/PG assertion; TEST-V3-BC-159-FLOW.
- **AC-BC-159-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-159-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-166 — Quản lý nhân sự và quyền hạn được Owner ủy quyền

Source: BC-166/R19/M01, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không vượt delegated scope; audit thay đổi; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-166-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Quản lý nhân sự và quyền hạn được Owner ủy quyền; Then outcome lưu/đọc đúng current delegated staff/permission assignment, giữ guard «Không vượt delegated scope»; reload/API/PG assertion; TEST-V3-BC-166-FLOW.
- **AC-BC-166-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-166-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

### REQ-V3-BC-167 — Điều hành cửa hàng/kho và cấu hình vận hành được giao

Source: BC-167/R19/M02, CORE; [matrix](../../docs/business-references/E_Logistics_Role_Module_Business_Capability_Matrix_GroupEF_Full.md). Guard: Không đổi Merchant owner hoặc ngân hàng COD; permission code DECISION_REQUIRED (no runtime allowlist expansion). Scope: MERCHANT_ORG_BRANCH. Owner `merchant`.

- **AC-BC-167-SUCCESS:** Given current subject và resource đúng MERCHANT_ORG_BRANCH; When Điều hành cửa hàng/kho và cấu hình vận hành được giao; Then outcome lưu/đọc đúng delegated branch/facility/defaults, no Owner/bank edits, giữ guard «Không đổi Merchant owner hoặc ngân hàng COD»; reload/API/PG assertion; TEST-V3-BC-167-FLOW.
- **AC-BC-167-DENY:** Given wrong subject/tenant/assignment, expired/revoked authority hoặc illegal state; When direct API/query/submit/replay; Then no unauthorized effect, no private data/receipt disclosure, safe error và UI không báo success.
- **AC-BC-167-RECOVERY:** Given lỗi dependency hoặc lost response/concurrent request; When retry/recovery theo operation contract; Then read giữ đúng snapshot/scope, mutation chỉ một effect hoặc rollback/pending được định nghĩa; payload/version conflict không ghi đè. Bắt buộc fixture expected DB before/after cho operation này.

## Nghĩa vụ legacy được tiếp nhận

Các ID cũ là aliases nguồn, không đổi nghĩa hoặc coi mapping bằng acceptance. Khi một yêu cầu phân bổ nhiều spec, primary mutation/data owner theo cross-module contract; consumer chứng minh phần mình.

| ID lịch sử | Nghĩa vụ giữ lại | Nguồn |
|---|---|---|
| REQ-003-01 | Pickup point thuộc merchant; user chỉ CRUD điểm được cấp quyền. | [pinned history](https://github.com/phamquoainam/logistics-capstone-project/blob/eeb7833ab678240581c407ccd167d7e61ea873c6/specs/003-merchant-address/spec.md) |

## Quy tắc chung và evidence

Canonical target documentation; NOT_STARTED/SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION. Không acceptance do promotion. Exact candidate route/DTO/error/permission/schema: contracts/openapi.candidate.json + policy.json + data-model.md; reviewed planning baseline under contracts/technical-review.md, không là runtime authority; production implementation NOT_AUTHORIZED.

Authority = current account/session + explicit permission + personal/org/branch/assignment/resource scope + current state/evidence; server recheck cả list, pagination, submit, replay và async recovery. Deny trước khi tiết lộ existence/receipt; không body actor/role/owner authority. Machine principal tách human; Recipient external; Guest không tạo online Shipment.

Mutation hợp lệ: scoped key + subject/operation/target + canonical payload + original version; same binding một effect, changed binding conflict, current authority recheck; read không ghi business effect. Unique/check/FK/locking theo invariant đã review, rollback khi audit bắt buộc lỗi. Source+audit/receipt/outbox atomic; external I/O ngoài TX; consumer inbox/dedup. Không đổi strict refresh thành retry/idempotency grace.

UI có loading/empty/field validation/error/unauthorized/pending/conflict/unknown/revoked; giữ input an toàn, không success trước server. Search/filter/sort/paging scoped; keyboard/focus/labels/contrast/responsive; timezone/currency/unit nhất quán, private data mask. Feature chưa có không hiện nút chết/mock counter.

Unit/ArchUnit/contract/HTTP real filter chain/H2 regression/PG18 empty+upgrade+privilege+constraint+rollback+races/browser E2E; native device chỉ nơi thật sự có native flow. Exact SHA/env/command/exit/pass/fail/named skips/report/artifact digest; planned IDs không receipt. V1–V6 immutable; no repair/ORM update/security allowlist bypass. Owner merge và full acceptance riêng; TASK DONE != PR MERGED != SPEC ACCEPTED != PORTAL ACCEPTED.

## Full acceptance

Mọi assigned CORE/negative/legacy obligation và supplemental portal requirement phải có real BE/API/PG/UI-channel evidence; minimum dependencies verified; mandatory CI, traceability và Owner feature acceptance. [Portal milestone](../../docs/portal/group-ab-acceptance.md) là cross-Spec, không tự PASS khi spec này hoàn tất.


## S003-ACCOUNT — account-first và personal profile

REQ-003-ACCOUNT: BC132/134 dùng normal personal enrollment qua Identity002; không account-type chooser. Verified current customer login vào personal dashboard shell/profile, Merchant application entry chỉ sau eligibility. Guest deep-link application phải login rồi server recheck; no anonymous submit. Account có từ trước chỉ reuse sau authenticated proof, không match email để takeover. Duplicate registration trả non-enumerating outcome theo Identity amendment; không tạo identity thứ hai hoặc thêm role.

REQ-003-PROFILE: BC135 business display/contact preferences thuộc customer_profile boundary độc lập Merchant organization/membership theo Owner DATA-01, gắn personal account bằng authoritative Identity ID qua approved contract; Identity sở hữu verified security email/phone/password/session/account security status. Thay business contact không tự đổi recovery destination; thay security contact phải qua Identity reverify/session policy. Người dùng không patch activation/status/verified flags. Full name/contact/status hiển thị theo purpose/minimal DTO; không tự copy credential fields.

AC-003-ACCOUNT: Given Guest; When đăng ký; Then chỉ có personal account/proof theo Identity, zero org/member/SYSTEM grants. Given eligible verified account; When login; Then personal default. Given suspended/rejected Merchant nhưng account còn valid; When mở personal profile; Then hợp lệ. Given unverified/disabled account; When submit Merchant application; Then deny, no org/Owner.

## S003-APPLICATION — manual/sandbox admission, không giả legal verification

REQ-003-APPLICATION (BC133/032/157): application gồm business entity type, registered name, registration/tax identifier nếu entity policy yêu cầu, registered address, operational/contact information, representative và applicant authority, business contact channels, supporting evidence, consent/policy version. Đây là field categories để review, không official API/schema hoặc tuyên bố tuân thủ pháp luật. Owner POLICY-003-01 đã duyệt manual/sandbox COMPANY/HOUSEHOLD_BUSINESS và explicit multi-org; exact candidate entity fields/evidence/business key tại contracts/README.md và contracts/policy.json cần review trước code. Registered address của application là evidence nghiệp vụ; không tự coi serviceable/confirmed pickup address. Pickup/service validation thuộc Spec004.

Business transition policy được Owner duyệt; exact wire enums/schema vẫn candidate tại contracts/policy.json và OpenAPI, chưa được tự triển khai migration:

| Application state | Actor/action/guard | Next state và effect |
|---|---|---|
| DRAFT | Applicant verified/eligible sửa draft thuộc mình | DRAFT; không org/member authority |
| DRAFT / NEEDS_INFORMATION | Applicant submit/resubmit đủ entity-policy evidence; version và duplicate check | SUBMITTED; đóng revision evidence, không Owner |
| SUBMITTED | Scoped Business Admin nhận review, không tự duyệt application của chính mình | UNDER_REVIEW; audit reviewer/reason |
| UNDER_REVIEW | Reviewer yêu cầu bổ sung, nêu thiếu gì qua safe fields | NEEDS_INFORMATION; không sửa evidence đã nộp |
| UNDER_REVIEW | Reviewer xác minh representative authority/manual sandbox proof và đủ policy | APPROVED chỉ cùng controlled activation transaction dưới đây |
| UNDER_REVIEW | Reviewer reject có reason | REJECTED; preserve history, personal account không bị disable |
| DRAFT / SUBMITTED / NEEDS_INFORMATION | Applicant withdraw trước approval theo policy/current version | WITHDRAWN; không xóa audit/evidence |
| REJECTED / WITHDRAWN | Applicant nộp lại theo policy | New linked application/revision; không rewrite quyết định cũ |

Duplicate-business detection: xác định normalized identifiers theo entity type và policy; không đoán chỉ tên. DB scoped uniqueness/active-application ownership binding khi phù hợp; ambiguity/manual review không tự merge org hoặc đổi Owner. Existing org → deny takeover hoặc authorized invitation/ownership process; không tạo duplicate ACTIVE org bằng retry. Error không tiết lộ evidence/account của applicant khác.

## S003-ACTIVATION — tổ chức, initial Owner và recovery

REQ-003-ACTIVATION: Merchant owner TX recheck applicant current identity eligibility qua approved protocol + current reviewer scope + application revision/state + representative evidence. Lock application/business uniqueness/ownership population; atomically tạo tổ chức ACTIVE, exactly one initial-owner binding ACTIVE cho applicant, mark application APPROVED, audit/receipt/outbox. Server cấp IDs và quyền theo policy; role/owner/org ID request không authority. Unique initial-owner binding/business key ngăn duplicate activation; audit failure rollback toàn bộ; mất response replay một activation. Không cần đổi credentials hoặc SYSTEM grants của applicant trong approval TX.

Owner-approved org lifecycle direction (candidate wire labels): ACTIVE → SUSPENDED bởi authorized Business Admin có reason/evidence → ACTIVE sau reviewed reinstatement; ACTIVE/SUSPENDED → CLOSURE_REQUESTED → CLOSED chỉ khi downstream obligations có clearance theo contract Finance/Orders/claims. CLOSED không hard-delete history hoặc tự đóng shipments/ledger; pending closure không được giả hoàn tất nếu dependencies chưa có. Initial application approval không tự tái kích hoạt org cũ. Membership Owner cuối cùng không bị revoke/suspend/transfer nếu tạo orphan org; controlled replacement/succession policy và DB lock/constraint cần review, không mượn invariant last Identity Admin làm proof.

Suspended org chặn business mutations/context theo policy, cho phép scoped read/history nếu được duyệt; carrier tiếp tục xử lý custody/financial obligations bằng workflow owner, không xóa lịch sử. Account cá nhân còn hợp lệ vẫn truy cập personal workspace. Account bị Identity disable thì cả personal/business requests phải deny.

AC-003-ACTIVATION: two concurrent approvals/replay → one org/initial Owner/audit effect; forged Owner flag → deny; approve own application/outside region → deny; current eligibility revoked before commit → deny theo protocol; simulated write/audit failure → zero partial ACTIVE authority; interrupted draft/review resume theo persisted state, không approve lại mặc định.

## S003-MEMBERSHIP — invitation, delegation và context

REQ-003-MEMBERSHIP (BC007/008/020/158/166): Owner/Manager có management AND assignability chỉ mời đúng org/branch và business permissions được phép. Invitation candidate PENDING → ACCEPTED / REJECTED / EXPIRED / REVOKED; 72h configurable, recipient/purpose-bound theo Owner policy; không dùng Identity invitation24h; inviter revoke/suspend hoặc policy/delegation version change invalidates pending invitation, resend revokes prior proof. Existing verified customer accept bằng account của chính họ; new recipient hoàn tất normal personal registration/login trước accept; không tạo SYSTEM account. Recheck inviter current delegation, org ACTIVE, recipient binding và allowed rights tại acceptance. Duplicate/replay một membership; changed binding conflict; expired/revoked proof không authority.

Membership candidate PENDING → ACTIVE khi accepted hợp lệ; ACTIVE → SUSPENDED / REVOKED do authorized org manager trong bounds; SUSPENDED → ACTIVE chỉ qua reviewed reinstatement; REVOKED muốn tham gia lại cần new admission, không revive revoked proof. Person self-leave có last-Owner safeguard. Không global-disable account khi khóa membership. Effective business permission không vượt current delegation/resource scope; replacement kiểm cả rights bị remove/add theo management contract, không xóa quyền org B từ org A.

REQ-003-CONTEXT: personal default; list chỉ current accessible org; indicator org rõ. Switch revalidate server, cancel/ignore stale in-flight responses, partition/clear org caches/forms/notifications và không flash dữ liệu org trước. Revoked membership/suspended org → clear stale business context, safe error rồi personal access nếu còn valid; URL/body context ID không authority. Machine API principal không hiện như human context.

## S003-FACILITIES — đúng boundary

BC159/167 quản lý metadata cửa hàng/kho/branch và versioned organization defaults trong scope; không SKU stock/custody/carrier Station master. Service preferences tham chiếu published supported catalog/policy qua Spec004/005 contracts; unknown/unsupported service không được bật. Reference có version và validation; chưa có catalog tối thiểu thì capability này BLOCKED, không báo full003 PASS. Pickup list/form/commands BC009/021/168 thuộc canonical004 duy nhất;003 chỉ context/navigation khi004 delivered. Không dependency compile-time vòng; minimum catalog contract/fixtures có thể chuẩn bị trước, không coi một phần004 là full004 accepted.

## Full Spec003 acceptance và demonstration

REQ-003-FULL: đủ mọi assigned BC, S003-ACCOUNT/APPLICATION/ACTIVATION/MEMBERSHIP/CONTEXT/FACILITIES và historical obligations của003 trong scope. Demo thật: Guest personal register→verify/login→self profile; customer application→request-info/resubmit→independent scoped review→approval→same identity personal+Merchant context; Owner invite Manager/Operator→recipient verified accept→delegated staff access→revoke giữa form/submit; rejected/suspended Merchant vẫn personal; duplicate/race/fault/no orphan; cross-org UUID và Identity-only admin deny. Real PG persisted rows, HTTP filter chain, browser, accepted technical amendments, migrations/compatibility, CI/evidence, no unresolved blockers và Owner feature acceptance riêng. Không cần shipping/COD/labels để giả demo003 đầy đủ; các chức năng đó thuộc specs sau.


## REQ-AB-ENTRY

Owner target: L003; source BC-132;BC-133;BC-134; latest Owner portal scope, no new BC ID. UI/channel: Public/personal entry.

Required behavior: Public landing/register/login/recovery navigation; one personal registration, personal default; Merchant application only authenticated; no dead-end Guest Merchant form. Exact candidate routes/DTO/permissions/schema đã định nghĩa tại contracts/openapi.candidate.json, contracts/policy.json và contracts/data-model.md; Owner acceptance pending before code.

**AC-ENTRY:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then Guest cannot submit application; verified personal login default; deep links rechecked; no role grants. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-ENTRY PLANNED_NOT_EXECUTED. Dependencies: L002.


## REQ-AB-CONTEXT

Owner target: L003; source BC-007;BC-020;BC-158;BC-166; latest Owner portal scope, no new BC ID. UI/channel: Context switch/profile/staff.

Required behavior: Accessible organizations only; active context indicator; partition/clear cache, cancel or ignore stale previous-context responses; preserve personal access after org rejection/revoke. Exact candidate routes/DTO/permissions/schema đã định nghĩa tại contracts/openapi.candidate.json, contracts/policy.json và contracts/data-model.md; Owner acceptance pending before code.

**AC-CONTEXT:** Given valid current actor/resource/state; When thực hiện workflow nguồn; Then A→B switch cannot show A response in B; revoked org cleared and current request denied; personal still valid. Negative: wrong owner/revoked/expired/illegal state → no unauthorized effect/private disclosure. Evidence: real source-owner HTTP/PG + browser/channel; TEST-AB-CONTEXT PLANNED_NOT_EXECUTED. Dependencies: L002.

## Owner six-decision closure and exact contract package

[OWNER-S003-SIX-2026-10-27](../../docs/decisions/spec003-six-decisions.md) approves all six policy directions with refinements. [Contract package](contracts/README.md), [data model](contracts/data-model.md), [minimum dependencies](contracts/minimum-dependencies.md), [coverage](contracts/coverage.csv) and [DoR](readiness.md) are the reviewed planning baseline under [focused technical review](contracts/technical-review.md). Checkpoint SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION; production implementation remains NOT_AUTHORIZED and runtime NOT_IMPLEMENTED/NOT_ACCEPTED.

- BR-S003-AUTH: verified email customer admission distinct SYSTEM; unverified phone never login/recovery authority; no public business/admin grants. Register→verify→activate→login→refresh→logout→recovery must be real.
- BR-S003-SELF: customer_profile self scope is independent of organization/membership/approval; business contact updates cannot alter Identity verified security contacts/password/status/recovery destination.
- BR-S003-STATE: approved business policy transitions only; submitted revisions/evidence immutable; resubmission creates new revision; approval+org+initial Owner atomic; pending activation/transfer/recovery grants no active authority; independent Business Admin, no self-review.
- BR-S003-REPLAY: current authority and version/assignability at final locked decision point; revoke-first denies, earlier valid commit survives. Same-key replay rechecks authority; changed binding conflicts; no duplicate initial Owner/membership or orphan last Owner.
- BR-S003-EVIDENCE: five retained/reserved private files/application,5MiB/file, PDF/PNG/JPEG signature/parse screening; unavailable scanner denies access. Delivery queued/provider-accepted != delivered. Sandbox30/90/365d retention with holds/references, purge disabled pending separate approval.

Additional acceptance: legal-profile changes stage independent-review revisions; target-consented two-person Owner recovery can admit an eligible new personal customer without trusting supplied account IDs. Reviewer bootstrap is offline Owner-controlled and audited, not a public privilege endpoint. Closure with missing/active obligations stays pending; successful closure requires real owner clearances. See exact schema/scenario mappings, no new BC IDs.
