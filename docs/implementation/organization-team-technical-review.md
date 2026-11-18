# Organization Team V1 — checkpoint đối chiếu kỹ thuật

2026-11-17 · `feature/argopost-business-shipping-tools` · baseline `c8cdfa3`.

Trạng thái hiện hành: **Owner đã duyệt Technical Core V1** theo manifest SHA-256
`f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`.
[Manifest JSON](organization-team-technical-manifest.json) được giữ nguyên byte
làm bằng chứng phê duyệt; các trường pending trong JSON là trạng thái lịch sử.
Phần đối chiếu dưới đây được lập trước technical GO. Tiến độ source/test hiện hành
nằm trong [execution log](organization-team-execution.md). Chưa có quyền triển khai
migration hoặc cấp quyền tài khoản localhost.

## 1. OTA-API-01–15

`B = /api/v1/merchant-organizations/{organizationId}/team`.
DTO đầy đủ20 định nghĩa nằm trong JSON `dtos`; mỗi API trong `apis` có status,
headers, operation, ownership, permission và error contract riêng.

| ID | Method/path | Request → Response | Permission / ownership |
| --- | --- | --- | --- |
| 01 | POST B/members/search | MemberSearch → MemberPage | team.read; current same-Org Membership; lọc không mở quyền |
| 02 | GET B/members/{membershipId} | — → MemberDetail + ETag | team.read; đích cùng Org; không email/phone/account UUID |
| 03 | GET B/role-templates | — → RoleTemplates | team.read; ceiling do nền tảng định nghĩa, không tự cấp grants |
| 04 | POST B/invitations | InvitationInput → Invitation + ETag | team.invite; Owner mời Manager/Operator; Manager chỉ mời Operator trong ceiling |
| 05 | POST B/invitations/search | InvitationSearch → InvitationPage | team.invite; Owner trong Org, Manager chỉ lời mời Operator của mình |
| 06 | POST B/invitations/{id}/resend | Reason → Invitation + ETag | team.invite; cùng giới hạn inviter, thu hồi proof cũ |
| 07 | POST B/invitations/{id}/revoke | Reason → Invitation + ETag | team.invite; cùng giới hạn inviter |
| 08 | PUT B/members/{id}/access | AccessInput → MemberDetail + ETag | team.access.manage **và actual Owner**; non-self/non-Owner; reauth |
| 09 | POST B/members/{id}/suspend | SensitiveReason → MemberDetail + ETag | team.membership.manage **và Owner**; đích non-Owner cùng Org; reauth |
| 10 | POST B/members/{id}/resume | SensitiveReason → MemberDetail + ETag | như09; kiểm tra lại Identity eligibility, không revive REVOKED |
| 11 | POST B/members/{id}/revoke | SensitiveReason → MemberDetail + ETag | như09; REVOKED terminal; không xóa lịch sử |
| 12 | POST B/audit/search | AuditSearch → AuditPage | team.audit.read; bounded same-Org; không log thiết bị/secret của Identity |
| 13 | GET B/receipts/{operation}/{key} | — → Receipt | actor+Org+operation+key và current quyền lệnh tương ứng |
| 14 | POST /api/v1/customer/organization-invitations/{id}/accept | InvitationDecision → DecisionReceipt | Customer đã xác minh đúng email nhận; chưa cần Membership Org |
| 15 | POST /api/v1/customer/organization-invitations/{id}/decline | InvitationDecision → DecisionReceipt | như14; không decline ẩn danh |

Error contract chung:400 VALIDATION_FAILED;401 guard Identity/session;403
AUTHORITY_REQUIRED/CSRF_REJECTED/TEAM_DELEGATION_DENIED;404 RESOURCE_NOT_FOUND che foreign/hidden/absent;
429 RATE_LIMITED + Retry-After;503 SERVICE_UNAVAILABLE/ORGANIZATION_TEAM_DISABLED.
Mutation thêm409 IDEMPOTENCY_CONFLICT/TEAM_STATE_CONFLICT/
LAST_OWNER_PROTECTED/MEMBERSHIP_ALREADY_EXISTS;422
TEAM_INVITATION_INVALID_OR_EXPIRED hoặc TOKEN_INVALID_OR_EXPIRED của reauth.
Lệnh06–11 yêu cầu If-Match:428 thiếu,412 VERSION_MISMATCH. Đọc search POST không
ghi receipt/quota hoặc kéo dài idle session; vẫn kiểm tra Origin/CSRF.
Mọi mutation có UUID Idempotency-Key. API04 trả201, các success còn lại200.
Tất cả no-store; không log body chứa email/proof hoặc đưa chúng vào query URL.
Error body giữ envelope `{code}` hiện có. Riêng14/15 trả422 thống nhất cho ID
không tồn tại, sai recipient/proof, hết hạn/thu hồi/invalidated, không dùng404 để
tiết lộ sự tồn tại của lời mời. Operation receipt không hợp lệ là400.

API13 chỉ tra cứu bảy operation quản trị. Customer chưa có Membership giải quyết
kết quả accept/decline chưa rõ bằng replay14/15 cùng key/proof và admission hiện
hành; không bị buộc có quyền quản trị Org để đọc receipt.

## 2. Năm permission mới — tất cả chờ duyệt

| Permission | Scope duy nhất | Vai trò đủ điều kiện, vẫn cần grant thực |
| --- | --- | --- |
| team.read | ORGANIZATION | Owner/Manager: xem nhân sự và ceiling quyền |
| team.invite | ORGANIZATION | Owner/Manager; giới hạn mục01, không invite Owner |
| team.access.manage | ORGANIZATION | Chỉ Owner, không self-promotion/Owner target |
| team.membership.manage | ORGANIZATION | Chỉ Owner, không khóa/thu hồi Owner |
| team.audit.read | ORGANIZATION | Owner/Manager, chỉ lịch sử Team cùng Org |

Không auto-grant theo role, không cấp cho NamShop Demo hoặc Customer khác.
Core runtime không được cấp/chuyển tiếp `team.*` qua lời mời hoặc access edit;
operator được ủy quyền riêng mới cấp các quyền này. MANAGER/OPERATOR mới chỉ nhận
operational subset từ accepted27; không thêm Finance/PII-export/admin bằng tên role.
`dashboard.staff.read` không phải `team.read`. FACILITY giữ chính xác allowlist
V32; không thêm team permission ở kho, không tự tạo scope Store.
JSON chứa đầy đủ27 permission hiện hữu,23 permission được phép có FACILITY scope
và ba role template dạng array cụ thể. `allowedPermissions` là ceiling nền tảng;
`runtime_delegable_ceiling` là ceiling thấp hơn cho thao tác cấp quyền, vẫn giao
với actual grants của actor. Năm quyền Team chỉ được operator cấp riêng.

## 3. Năm bảng — PK/FK/unique/lifecycle

Tất cả Merchant-owned và **chưa có DDL**. JSON `tables` liệt kê từng cột, SQL type,
nullability, khóa và check; bảng dưới là bản đối chiếu quan hệ.

| Bảng | PK, FK và unique chính | Lifecycle / ràng buộc bảo mật |
| --- | --- | --- |
| merchant.team_policy | PK organization_id → organization.id | version/template_version dương; baseline metadata không cấp quyền; thay version vô hiệu lời mời pending |
| merchant.team_invitation | PK id; UNIQUE(id,organization_id), UNIQUE(proof_digest); partial UNIQUE(organization_id,recipient_digest) khi PENDING; same-Org inviter/accepted member FKs; Org → team_policy | PENDING → ACCEPTED/DECLINED/REVOKED/EXPIRED/INVALIDATED; terminal immutable; email encrypted, keyed digests, không plaintext proof; accepted_member chỉ có ở ACCEPTED; expiry transition rõ, không dùng now() trong unique index |
| merchant.team_invitation_grant | PK id; same-Org invitation và facility FKs; UNIQUE NULLS NOT DISTINCT(invitation_id,permission,scope,facility_id) | grant intent immutable; tối đa200, dashboard.read dependency, scope/permission ceilings; resend không đổi grants |
| merchant.team_audit | PK id; same-Org actor/target Membership và invitation FKs; UNIQUE(org,actor_account,operation,request_id) | append-only; actor_account từ Identity admission; actor Membership chỉ nullable cho verified recipient DECLINE; không secret/email/IP/device log |
| merchant.team_command_receipt | composite PK(org,actor_account,operation,key); same-Org typed membership_id/invitation_id FKs, đúng một target | immutable; digest keyed, current replay admission; deferred receipt/audit/resource/grant consistency; không polymorphic targetId thiếu FK |

Actor account là external reference được xác nhận qua Identity public admission;
không thêm FK/read vào bảng internal của Identity. Mọi UUID resource/FK mang Org,
không chỉ kiểm tra frontend. Không runtime raw-write bypass, cascade/delete lịch
sử hoặc quyền chạy operator function. SMTP chạy qua protected outbox/worker
hiện có, không network I/O trong transaction. SMTP250 không chứng minh nhận email.

## 4. Tương thích và xung đột V23

| Nguồn hiện hành | Đánh giá / amendment đề xuất |
| --- | --- |
| membership UNIQUE(org,account), immutable account/org/id/createdAt | GIỮ; không tái tạo hoặc revive nhân viên REVOKED |
| membership chỉ ACTIVE/REVOKED; state VARCHAR7 | AMENDMENT CHỜ DUYỆT: VARCHAR9 + SUSPENDED; không sửa file V23 |
| guard_workspace_identity cấm thay role | AMENDMENT CHỜ DUYỆT: chỉ MANAGER↔OPERATOR qua lệnh đã kiểm chứng; version/authorityVersion tăng đúng1; không sửa Owner |
| organization.initial_owner_membership_id bất biến, deferred ACTIVE OWNER check | GIỮ NGUYÊN; mọi lệnh09–11 từ chối Owner; không thay pointer/last-owner rule |
| JdbcBusinessWorkspaces kiểm tra initial Owner ACTIVE và LOCAL_OPERATOR_PROVISIONED | GIỮ; không mở Production hoặc phê duyệt pháp lý Org |
| member_grant + V24–V32 scopes/read dependencies | Thêm đúng5 team rights ORG-only sau GO; accepted27 và facility allowlist/dependencies giữ nguyên |
| operator grant/revoke/suspend/version thay đổi quyền ngoài runtime | Phải invalidate pending Team proofs cùng transaction; không để bypass invitation policy bằng operator path |
| Quyền cũ do operator cấp ngoài MANAGER/OPERATOR ceiling mới | Giữ dữ liệu; core replacement trả TEAM_STATE_CONFLICT, không âm thầm strip/mở rộng; cần disposition/template hợp lệ trước edit |
| Customer reauthentication hiện hữu | Tái sử dụng session-bound single-use proof và cùng transaction; không viết lại mật khẩu/phiên |

Các khoảng trống của bản đề xuất ban đầu đã được làm rõ và **vẫn chờ duyệt**:
recipient chưa có ETag/Membership nên proof version là concurrency credential;
invitation-grant cần PK thực; DECLINE cần actor account được Identity xác minh dù
chưa có Membership; receipt cần typed target FKs. JSON có sáu mục
`OTA-CLARIFY-01–06`, không giả định chúng đã được Owner chấp thuận.

Owner Transfer vẫn ngoài core: dù policy hai bước15 phút đã accepted, V23 và
BusinessWorkspaces chưa có current-owner/provenance amendment. Không tự gỡ invariant.
WAREHOUSE/FINANCE/SUPPORT role templates tiếp tục chờ amendment riêng. Không
đưa pending Organization Support permissions vào Team; không mở Carrier/Finance.

## 5. Gate trước implementation / deployment

Một technical GO cho toàn gói core ở trên mới mở OTA-04/05. Khi đó phải chứng minh
PG18 transaction rollback/direct-DML, last-owner, scope union, tenant isolation,
reauth, duplicate accept, proof replay/resend, revoked inviter/member denial,
offboarding giữ Personal/Org khác và test SMTP/Browser. Hiện các gate mới là
NOT_RUN_AWAITING_TECHNICAL_CONFIRMATION, không PASS.

Migration mới forward-only, số thứ tự chốt trên cùng stream trước source;
V33 Support chỉ được đề xuất, chưa tạo/chạy. Không sửa V1–V32, migrate/grant demo,
reset DB, chọn Customer tự động hoặc merge. Local deployment cần disposition riêng
sau source/test; không bao gồm trong core technical GO này.

Trang quyền của chính người dùng OTA-02 vẫn dùng API đã accepted; kết quả kiểm thử
và bảo toàn WIP được ghi riêng tại cuối checkpoint, không suy diễn full Team DONE.

## 6. Kết quả thực thi độc lập — OTA-02 LOCAL DONE

- Menu cùng cấp “Nhân sự & Phân quyền”, route `/merchant/{org}/team`; Membership
  của chính Customer và các quyền ORGANIZATION từ GET business-contexts thật.
  Nhóm/tìm quyền tại client, không suy ra toàn bộ quyền theo Warehouse hoặc số
  nhân viên. Không có form mời/đổi quyền giả thành công.
- Kiểm tra lại quyền ẩn dữ liệu cũ trong lúc tải và khi revoke/lỗi/đổi Membership;
  đổi Organization không giữ lại dữ liệu tenant trước. Quyền ở trang này là thông
  tin, không thay thế authorization từng thao tác tại Backend.
- Source mới: OrganizationTeam.tsx, organization-team.ts/.css, hai file test Team;
  tích hợp BusinessWorkspace/WorkspaceShell, menu regression, test-host SPA forward
  và journey Browser. Backend production và migrations không thay đổi.
- `npm.cmd --prefix app/frontend/web test -- organization-team.spec.ts`: **2/2 PASS**.
- `npm.cmd --prefix app/frontend/web run test:customer-ui -- organization-team.spec.ts business-ui.spec.ts`:
  **19/19 PASS** (10 Team,9 hồi quy Business); visual390/1440px đã xem, không tràn.
- `npm.cmd --prefix app/frontend/web run typecheck`, `run lint`, `run build`:
  **PASS**. `build:customer` do UI test runner chạy: **PASS**; advisory bundle-size
  hiện hữu vẫn còn, không bị tắt hoặc nâng ngưỡng để che cảnh báo.
- Focused PG18 parent `IdentityGrantApplicationTests#customerEntryBrowserUsesActualHttpsPostgresAndSmtp`:
  **1/1 PASS,0 skip**; **18/18 Browser journeys PASS** trên HTTPS/PG18/test SMTP.
  Journey Merchant đọc trang Team và kiểm tra lại quyền từ Backend thật, rồi tiếp
  tục luồng nhãn/chuẩn bị/bàn giao hiện hữu. Không phải test15 API mới hoặc Gmail
  invitation thật; không dùng fixture HTTP làm full-stack evidence.
- Credential privacy PASS. Ownership bindings observed/created/verified/stopped
  khớp; read-only absence check xác nhận các tài nguyên Testcontainers đã hết;
  container PostgreSQL demo hiện hữu vẫn healthy. Full all-suite report-policy
  không được trình bày như đã chạy ở focused checkpoint này.
- Checkpoint29 WIP paths dưới `.demo/organization-team/`: **SHA-256 PASS** cho mọi
  bản sao trước chỉnh sửa. Private raw test evidence/synthetic visuals giữ ignored;
  không đưa secrets hoặc PII thật vào các source mới.
- Launcher `LOCAL_FULL_STACK_HTTP_READY`; anonymous route/module Team HTTPS200 với
  CA validation. Đây là smoke readiness, không phải Owner-session Team admin
  acceptance. Không restart/init, migrate/grant hoặc đổi certificate/credential.
- Source manifest/traceability và `validate.py --implementation` PASS; whitespace
  check PASS. Không commit/push/PR/CI mới hoặc merge trong checkpoint này.

Manifest SHA-256:
`f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426`.
Digest định danh bản đề xuất đã đối chiếu, không phải approval receipt.

**Một hành động Owner tiếp theo:** duyệt hoặc chỉ rõ chỉnh sửa cho toàn gói core
OTA-API-01–15 / DTO-01–20 / PERM-01–05 / DB-01–05 / AMEND-01–04 và các clarification
trong manifest này. Chỉ sau technical GO mới thực hiện OTA-04/05 trên DB test;
Owner Transfer, ba role mở rộng và triển khai localhost vẫn ngoài checkpoint đó.
