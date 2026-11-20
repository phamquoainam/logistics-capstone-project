# CSKH Staff Authority — hợp đồng amendment đề xuất

## Current Owner-authorized source/test amendment — 2026-11-19

The Owner's Organization Support & Claims directive authorizes source/test and
the exact ACL-05 resolution: privileged bootstrap of the three approved NOLOGIN
roles on an isolated PG18 test cluster, outside Flyway. Owner/migrator stay
NOCREATEROLE; runtime/operator cannot SET ROLE to either writer. Temporary writer
schema CREATE permits only function ownership transfer and is revoked in the
migration transaction. V36/V37 validate prerequisites, never CREATE ROLE.

This supersedes only the prior role-creation placement and implementation-pending
boundary below. Original approved manifest `acf43f4e…71ebb4` is preserved outside
the repository; updated manifest SHA-256 is
`ad6af1eb97fa453419c962255db0a84b5f478a470441704929cc166a6dc8a15c`.
The original review content below remains historical. Demo/Production deployment,
provisioning, grants, binding, runtime activation and Git publication remain excluded.
Actual progress and failures belong in [execution evidence](cskh-staff-authority-execution.md).

Ngày: 2026-11-18. Trạng thái: **[PROPOSED] — TECHNICAL DESIGN ONLY**.
Checkpoint: `CSKH_STAFF_AUTHORITY_TECHNICAL_REVIEW_READY`.

Đây là gói duyệt kỹ thuật cho `CSKH-IDENTITY-01` và `CSKH-GRANT-02`, không phải
ủy quyền triển khai. Danh mục chính xác, signature, field, constraint và quyền nằm
trong [technical manifest](cskh-staff-authority-technical-manifest.json).
[Technical review](cskh-staff-authority-technical-review.md) ghi bằng chứng reuse,
rủi ro và phạm vi Owner cần quyết định. Mọi ID mới dưới đây đều **[PROPOSED]**.

## 1. Baseline và giới hạn

Branch `feature/argopost-business-shipping-tools`, HEAD
`c8cdfa335e31596116cf0d7626b81ef0d40dee78`, có WIP Team/Support chưa commit.
Không coi các phần này đã merge hoặc có GitHub CI PASS. Giữ nguyên mọi WIP.

Bằng chứng catalog read-only đã được ghi trong
[execution log](organization-support-execution.md): PostgreSQL Local/Test đã ở V35,
SYSTEM INACTIVE, ba Identity accounts hiện hữu, không có Staff/canonical Support
grant/Organization Staff binding. Năm Merchant Support grants đã hoàn thành.
Checkpoint thiết kế này sử dụng lại evidence đó, không lặp discovery, restore,
migration, grant hoặc source/test OSC-04/05/06.

Không sửa source/migration V1–V35, không provision, mutate DB, bind Staff, bật
feature, restart runtime, đổi credentials/certificates, commit/push/merge. Chỉ ba
artifact review và mục traceability thiết kế được bổ sung.

## 2. Authority và reuse

| Yêu cầu | Cơ chế có sẵn | Amendment tối thiểu |
| --- | --- | --- |
| Account/credential | `user_account`, Identity aggregate, PasswordPolicy và BCrypt hiện hữu | Marker purpose Staff riêng; không tạo Customer identifier hoặc grant quản trị |
| Xác minh thực | `action_proof`, `InvitationProtection`, `invitation_outbox`, activation và proof receipt | Purpose `STAFF_INV`, 24 giờ; context format 2 không phụ thuộc SYSTEM realm |
| Phiên | `IdentitySessions`, refresh rotation, CSRF, RS256 issuer/audience | Composition STAFF cố định; discriminator trên session/CSRF SYSTEM, không thêm ba bảng session |
| Canonical Support | `support_staff_grant`, hai permission, `support_business_audit`, khóa 730022 | Command request-bound và receipt lâu bền; không có grant engine thứ hai |
| Organization Support | Grant + Organization binding + case assignment hiện hữu | Chỉ đổi Staff admission và eligibility target của operator binding; Merchant branch giữ nguyên |

Ordinary provisioning hiện yêu cầu management actor và ACTIVE SYSTEM realm.
Không truyền grant rỗng, giả session/management actor, dùng Customer signup rồi
đổi nhãn, hoặc ghi trực tiếp bằng schema owner để né giới hạn đó. Amendment này
bổ sung nhánh Staff rõ ràng; không sửa nghĩa nhánh SYSTEM được duyệt tại ADR-0010.

## 3. CSKH-IDENTITY-01: dedicated Staff

### 3.1. Principal và lifecycle — CSKH-POL-01/02

Staff vẫn là một con người với `identity.user_account` và credential riêng.
`identity.local_staff_principal` xác định purpose `SUPPORT`, environment LOCAL/TEST
và lifecycle; không thêm Realm, không có IDENTITY_MANAGEMENT.

- Chỉ tạo account mới, UUID do hệ thống sinh. Username tự sinh `staff-<UUID>`.
- Email phải qua syntax hiện hữu, không trùng exact email của account/Customer
  identifier hoặc Customer enrollment đang pending. Không gộp account theo email.
- Khi tạo: account PENDING_ACTIVATION, chưa verified, password NULL; principal
  PENDING, version 1. Chưa đăng nhập hoặc có bất kỳ Support grant nào.
- Sau proof đúng, còn hạn, password hợp lệ: account ACTIVE/verified và principal
  ACTIVE cùng transaction. Không tạo session tự động.
- RESEND chỉ cho pending, thu hồi proof trước và xóa protected delivery cũ; proof
  mới có hạn 24 giờ. CANCEL pending là terminal, không xóa account/evidence.
- Expired invitation không tự kích hoạt account. Không có auto-reactivation,
  account recovery/email change mới hoặc chuyển purpose trong amendment này.
- Staff marker tồn tại cả khi cancelled; không chuyển thành Customer/Merchant,
  không cấp account_grant quản trị. Marker creation cũng từ chối account đã có
  Membership/identifier/grant, kể cả Membership không còn ACTIVE.

### 3.2. Trusted operator — CSKH-ACL-01/02/05

Offline operator là DB login được nền tảng cấp membership vào NOLOGIN group
`elogistics_local_staff_operator` trong checkpoint deployment riêng. Không phải
Merchant Owner hoặc một Identity account giả để thỏa ACTOR receipt.

`session_user` thật, current role membership và physical DB binding được kiểm tra
trước cả receipt lookup. Caller không được truyền operator/verified flag/target
UUID/password/grants cho lệnh provision. GUC môi trường không đủ làm authority.

Hai hàm provision/seal thực hiện trong cùng physical transaction do Identity
offline composition sở hữu:

1. Validate current operator/environment; khóa Identity administration; đối chiếu
   request/digest. Replay hoàn tất chỉ trả lại định danh cũ và current state.
2. Hệ thống sinh account/proof/message UUID, tạo pending account, purpose marker,
   audit và receipt reservation. Các FK/evidence checks được deferred tới commit.
3. Dùng crypto hiện hữu tạo secret và sealed material trong bộ nhớ; chỉ sealed
   bytes/verifier vào SQL. `seal_local_support_staff_invitation` ghi proof/outbox
   đúng reservation của chính operator/request đó.
4. Commit chỉ thành công khi đầy đủ account/marker/proof/outbox/audit/receipt.
   Exception hoặc thiếu seal phải rollback tất cả; không có reservation dở đã commit.
5. SMTP worker gửi sau commit, không gửi trong DB transaction. Receipt/CLI không
   chứa token, email destination, password hoặc ciphertext. Buffer secret được xóa.

V5 hiện có outbox nhưng không có network sender trong class đó. Phần nối sender
vào JavaMailSender đã được chọn phải nằm trong implementation/gates sau duyệt;
không coi việc có outbox là đã gửi email thật. Provider acceptance có thể lặp sau
crash; cùng invitation có thể được gửi lại, không tạo account/proof/grant mới.

### 3.3. Proof và delivery — CSKH-DB-05, CSKH-PORT-05

`STAFF_INV` vừa existing VARCHAR(10); token 32 byte random, base64url không padding
43 ký tự, hạn 24 giờ. `invitation_outbox` STAFF có realm_id NULL và format_version 2;
INVITATION cũ bắt buộc realm_id không NULL, format 1 và điều kiện SYSTEM cũ.

HMAC/AES-GCM dùng engine/keyring hiện hữu với domain Staff riêng, binding đủ
purpose, account/proof/message, destination, timestamps và key IDs. Context v2
encode null realm có tag rõ, không gọi `toString()` trên null hoặc dùng UUID Realm
giả. Không reinterpret format 1. Retain nonce/ciphertext lengths, lease/retry bounds,
expiry, terminal payload erasure và key-retirement rules hiện hữu.

Worker kiểm tra current Staff pending, proof, destination, environment trước claim,
open và ngay trước send. Resend/cancel/expiry ngăn gửi tiếp; lỗi SMTP không kích hoạt
account. Secret chỉ qua email thử nghiệm được kiểm soát trong checkpoint provision
sau này. Không log token/link hoặc xuất chúng trong evidence.

### 3.4. HTTP/session — CSKH-API-01–06, CSKH-PORT-01/03/04

| Method | Path mới [PROPOSED] | Mục đích |
| --- | --- | --- |
| GET | `/api/v1/staff-auth/csrf` | Preauth CSRF cho đúng Staff transport |
| POST | `/api/v1/staff-auth/login` | Email/password của Staff đủ điều kiện |
| POST | `/api/v1/staff-auth/refresh` | Rotation Staff refresh family |
| POST | `/api/v1/staff-auth/logout` | Thu hồi đúng Staff family |
| GET | `/api/v1/staff-auth/me` | Current Staff, không suy ra quyền Organization |
| POST | `/api/v1/staff-auth/invitations/accept` | Real possession + initial password, không public Staff signup |

Field/response/error chính xác ở CSKH-DTO-01–13 và từng API trong manifest. Reject
unknown/duplicate fields, duplicate cookies/headers, invalid UUID/content type và
oversize body. Dùng error envelope/no-store/HTTPS/Origin/CSRF và rate limiter hiện
hữu; không để endpoint enrollment trở thành public provision.

Token audience cố định `argopost-local-support-staff`; reuse RS256 và keyring đã
cấu hình. Cookie riêng `__Host-argopost.staff.csrf` / `__Host-argopost.staff.refresh`,
Secure/HttpOnly/SameSite Strict/Path=/, không Domain. Giữ 15 phút access, 30 phút
idle, 8 giờ absolute. Poll/refresh/receipt replay không gia hạn meaningful activity.

`auth_session`/`csrf_binding` thêm audience SYSTEM/STAFF, default SYSTEM cho
compatibility; refresh không thêm schema, join qua session. **Mọi** query
sessionAccount/lockSession/family/refresh/CSRF/access timing phải lọc audience cố định.
Không chỉ dựa vào JWT audience. Customer dùng bộ bảng riêng như trước.

`StaffAdmission` là typed Identity API dùng lại transaction/lock/security engine,
không broaden CustomerAdmission. Context không dùng lại ngoài callback; current
account/marker/session/version/deadline được kiểm tra mỗi request. Chỉ Staff branch
của `OrganizationSupportHttp` đổi adapter. Paths/DTO Ticket và Merchant branch giữ nguyên.

`CustomerRequestContext` hiện không phải bearer parser trung lập: authorize dùng
cookie CSRF của Customer. Vì vậy CSKH-PORT-06 và CSKH-CONFIG-02 bổ sung typed Staff
request context/filter trước Customer chain, chỉ match Staff auth và ba nhóm path
Organization Staff hiện hữu. Không thay toàn bộ `/support-operations/**` hoặc dùng
Customer cookie/ThreadLocal cho Staff. Sáu operation cũ đổi admission, không đổi
path/DTO hoặc authority xử lý Ticket; danh sách chính xác ở CSKH-HTTP-AMEND-01.

Actor luôn phải là Staff. Tuy nhiên, `staffReferences` hiện chứa cả Merchant creator/
người phụ trách nội bộ để gửi notification: referenced subjects phải hỗ trợ eligibility
Customer hiện hữu bên cạnh Staff, có `principalType` rõ ràng. Reference eligibility
không phải quyền đăng nhập. Assignee CSKH vẫn phải qua Staff eligibility + HANDLE +
Organization binding độc lập; không biến Merchant recipient thành handler. Đây là
dependency quan sát trực tiếp từ `JdbcOrganizationSupportTickets`, không thêm workflow.

UI chỉ bổ sung `/staff/activate` và `/staff/login`, không có Staff signup/link ở
Customer menu. Invitation proof đi bằng fragment, đọc vào memory rồi xóa fragment
ngay; không localStorage/query/referrer. Existing Support desk dùng Staff auth client
riêng. Không có binding thì trả denial rõ ràng, không lấy quyền Merchant làm dự phòng.

### 3.5. Staff eligibility và V35 binding dependency

`identity.lock_local_business_subject` hiện **bắt buộc Customer identifier**.
Dedicated Staff không thỏa điều kiện đó. CSKH-SQL-06 thêm Staff-specific predicate/
lock; không widen helper Customer này.

CSKH-DB-10 chỉ đổi target eligibility call trong
`orders.configure_local_organization_support_staff(UUID,TEXT,UUID,UUID,TEXT,TEXT)`;
giữ signature, Organization checks, grants, request/audit/locks và privilege cũ.
`orders.organization_support_staff` thêm current Staff eligibility để disabled
Staff bị từ chối ngay. Đây là dependency kỹ thuật được trình minh bạch, không phải
ủy quyền tạo binding hoặc bật Organization Support.

## 4. CSKH-GRANT-02: request-bound canonical grants

### 4.1. Signature và compatibility — CSKH-SQL-07/08

Signature mới [PROPOSED]:

```text
orders.configure_local_support_staff(
  p_request UUID, target UUID, permission_name TEXT, action TEXT, p_reason TEXT
) RETURNS TABLE(
  request_id UUID, account_id UUID, permission TEXT, committed_outcome TEXT,
  grant_id UUID, committed_version BIGINT, current_grant_state TEXT,
  effective_grant_id UUID, effective_permission_active BOOLEAN, replayed BOOLEAN
)
```

Vẫn chỉ `SUPPORT_TRIAGE` và `SUPPORT_HANDLE`, lưu đúng bảng canonical hiện hữu.
GRANT cần ACTIVE verified Staff/purpose; REVOKE được xử lý cho Staff đã mất Identity
eligibility để không kẹt offboarding. Không cấp Merchant/Carrier/Finance/Admin.

Legacy `(UUID,TEXT,TEXT)` được thay bằng deny-only body và revoke EXECUTE. Ngay cả
caller từng có owner privilege cũng chỉ nhận `STAFF_REQUEST_ID_REQUIRED` từ hàm
này, không có mutation. Không wrapper sinh request key ngẫu nhiên. Script hiện hữu
phải yêu cầu `RequestId`/`Reason` rồi gọi signature mới; không tạo script grant song song.

### 4.2. Replay, concurrency và audit — CSKH-REPLAY-01

Digest được tính tại authority bằng SHA-256 của canonical JSON array, có namespace
version, system/database/environment, `session_user`, action, exact target UUID,
permission và reason đã normalize. Không tin digest hoặc actor do caller tự khai.
Request UUID unique toàn bảng, không đổi chủ sang operator khác.

| Tình huống | Kết quả |
| --- | --- |
| Key/input/operator/DB trùng, command đã commit | Trả original receipt + current grant state, không thêm mutation/audit/receipt |
| Key trùng, input/operator/DB khác | Reject, không lộ receipt của caller khác |
| Response mất sau commit | Retry key cũ lấy đúng Grant UUID/version cũ |
| Grant cũ đã REVOKED rồi replay GRANT | Giữ REVOKED, không cấp lại; receipt chỉ là evidence lịch sử |
| Target không còn eligible hoặc operator bị thu quyền | Current admission từ chối GRANT replay; receipt không bypass authority |
| Key mới GRANT khi đã ACTIVE | NO_CHANGE, một receipt mới; không tăng version hoặc tạo mutation audit giả |
| Key mới REVOKE khi không có active grant | NO_CHANGE; không tạo Grant UUID giả/audit tăng version |
| Hai request đồng thời | Identity/account lock → canonical lock → receipt/grant; PK/unique active grant là chốt DB |
| Key mới sau revoke, đủ authority | Có thể cấp grant ID mới theo command mới; binding tham chiếu grant cũ vẫn vô hiệu |

Response phân biệt `grantId/currentGrantState` của kết quả gốc với
`effectiveGrantId/effectivePermissionActive` hiện tại. Cả hai không chứng minh quyền
Organization. Không dùng receipt toàn cục để vào Organization khác hoặc bỏ qua
case assignment.

Mutation thật phải có canonical grant delta + existing Support audit + receipt
cùng transaction. Preserve `new_version=prior_version+1` trong audit cũ. NO_CHANGE
chỉ có receipt command với operator/reason/time, không sửa business version.

V22 audit cũ request_id NULL được giữ nguyên. New operator audit request_id có FK
qua generated `operator_request_id` tới receipt; Ticket request UUID không bị ép
tham chiếu operator receipt. Deferred checks xác nhận hai chiều target/permission/
grant/version/operator/request, không chỉ tồn tại một UUID bất kỳ.

## 5. Schema tối thiểu và privileges

| ID [PROPOSED] | Bảng mới | Lý do không dùng bảng cũ |
| --- | --- | --- |
| CSKH-DB-01 | `identity.local_staff_environment` | Physical DB/environment binding disabled mặc định; GUC có thể tự set nên không đủ guard |
| CSKH-DB-02 | `identity.local_staff_principal` | Immutable purpose/admission, không biến Customer identifier hoặc account_grant thành nhãn Staff |
| CSKH-DB-03 | `identity.local_staff_operator_receipt` | Existing Identity ACTOR receipt bắt buộc Identity actor; offline operator không có actor như vậy |
| CSKH-DB-04 | `orders.support_grant_operator_receipt` | Existing V35 operator audit bắt buộc Organization và BIND/UNBIND; không dùng làm receipt GRANT/REVOKE toàn cục |

Không thêm account/credential/session/delivery engine, bảng permission, bảng grant,
queue hay SMTP provider. Proof acceptance reuse `identity.command_receipt` sau exact
CHECK amendment; audits reuse bảng cũ. PK/FK/unique/column/guard và lifecycle từng
bảng được liệt kê đầy đủ tại CSKH-DB-01–10, CSKH-GUARD-01–04 trong manifest.

Ba NOLOGIN roles được đề xuất: operator group và hai writer roles sở hữu function
giới hạn Identity/Orders. Runtime/operator không là member writer roles, không
SET ROLE sang writer. SECURITY DEFINER có search_path cố định và tên bảng qualify
đầy đủ; không dynamic SQL hoặc caller-controlled identifiers. Writer không có quyền
Realm, Membership, Ticket/Finance/Carrier DML hoặc schema-wide privileges.

Runtime giữ application trust boundary hiện hữu cho credential/HMAC verification;
SQL guards kiểm soát direct DML, lifecycle, provenance và atomic evidence. Không
tuyên bố SQL tự xác minh HMAC nếu kẻ tấn công đã lấy runtime DB credentials. API
authentication, least privilege, secret isolation và negative tests vẫn bắt buộc.

## 6. Forward migrations và deployment

- **CSKH-MIG-01 [PROPOSED]**: `V36__local_support_staff_identity.sql`.
- **CSKH-MIG-02 [PROPOSED]**: `V37__request_bound_support_staff_grants.sql`.

Source hiện có V1–V32, V34, V35; V36/V37 chưa bị chiếm. Không chèn V33 placeholder,
không sửa V22/V34/V35. Số phiên bản phải kiểm tra lại trước implementation; collision
phải báo exact delta, không tự viết lại manifest đã được duyệt.

V36 chỉ seed singleton DISABLED. `identity.bind_local_staff_environment` là thao tác
deployment riêng, derive DB identity thực, ghi audit/request/digest/operator trong
singleton; không truyền physical ID từ caller. Chỉ deployment principal có thẩm
quyền, không runtime/local operator, được bind một lần. Membership operator login
không được auto-grant. Missing privilege đọc DB identity phải dừng, không fallback.

V37 dừng deployment nếu còn ACTIVE canonical grants cho principal không phải Staff;
không tự convert/revoke. Lịch sử revoked legacy grant/audit giữ nguyên. Inventory đã
review hiện có zero canonical grants, nhưng deployment sau này phải kiểm tra lại.

Thứ tự checkpoint: duyệt manifest → source/test DB riêng → duyệt Local/Test deployment
và recovery point → enable environment/operator có giới hạn → provision/verify →
canonical grants → exact Organization binding → feature/runtime/UAT. Gói này không
ủy quyền bất kỳ bước runtime nào. `support.tickets.organization-enabled` giữ disabled.

Rollback transaction phải không để account/grant/outbox/audit/receipt dở dang. Với
DDL đã commit, dùng disable gates + forward correction/recovery theo approval riêng,
không down migration/repair/clean/reset hoặc phục hồi writable legacy path.

## 7. Gates và trạng thái task

| Task | Trạng thái checkpoint này | Bằng chứng cần có |
| --- | --- | --- |
| CSKH-DESIGN-01 | DONE — design only | Source evidence và inventory API/DTO/schema/ACL/replay chính xác |
| CSKH-DESIGN-02 | DONE — design only | Ba artifact nhất quán, JSON/ID/link/hash checks và docs validator |
| CSKH-IMPLEMENT-01 | NOT STARTED — chờ technical approval | V36/V37 source và focused tests CSKH-T-01–10 |
| CSKH-DEPLOY-01 | NOT STARTED — checkpoint riêng | Preflight/backup/permission delta/real verification evidence |

CSKH-T-01–10 chỉ là **test plan [PROPOSED], chưa chạy**: PG18 migration/FK/CHECK/direct
DML, rollback; invitation/lost-response/concurrency; cryptography/delivery/privacy;
Staff HTTP/session; canonical replay/revoke; Organization isolation/current scope;
Production denial; affected Browser/Frontend/Architecture/regressions. Không lặp
OSC/full suite không liên quan hoặc ghi planned tests là PASS.

Owner cần duyệt đúng CSKH-DEC-01–04 trong manifest, bằng hash review duy nhất. Nếu
duyệt source/test, vẫn không đồng nghĩa cho phép migrate/provision/grant/bind/enable
localhost. Không yêu cầu duyệt lại Organization Support business/API scope đã chốt.
