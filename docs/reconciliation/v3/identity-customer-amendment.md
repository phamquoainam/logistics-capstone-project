# CC-V3-IDENTITY-01 — controlled customer Identity amendment

Status: BUSINESS_DIRECTION_OWNER_DECIDED / TECHNICAL_AMENDMENT_REVIEW_REQUIRED / NOT_IMPLEMENTED. [Business decision](../../decisions/customer-account-merchant-admission.md). Giữ nguyên accepted Feature002/ADR0009–0013/OpenAPI và migrations V1–V6; proposed delta dưới đây không có hiệu lực runtime.

| ID | Before hiện hành | Required target / thiết kế cần review | Owner và gate |
|---|---|---|---|
| IAM-CUSTOMER-01 | ADR0009 DEC04 admin-controlled provisioning; login eligibility exact active SYSTEM Realm management pair | Customer enrollment riêng, verified account ID, consent, duplicate-safe anti-enumeration, Recovery và customer session admission riêng; không bỏ eligibleRealm globally, không gắn khách vào SYSTEM Realm | Identity+Security; accepted amendment/contract/model/tests trước code |
| IAM-CONTEXT-02 | Demo AuthenticatedActor bốn fields; bounded full-target Realm grants; không customer/org authority provider | Trusted customer-session subject; personal default; resource-owner membership/assignability/revocation/version APIs; business context không là principal/Realm, no request actor flag | Identity+Merchant+Architecture; reviewed dependency direction và revoke races |
| IAM-INVITE-03 | V5 invitation-only24h secure outbox; proof locator compatibility, delivery worker/maintenance và Recovery chưa full | Phân biệt account security invitation với org membership invitation; verified existing-account accept; new account phải hoàn tất normal personal enrollment; invitation recipient binding, expiry/revoke/replay/current inviter delegation; approved channel/retention | Identity+Merchant+Security; no reusing SYSTEM provisioning to create Merchant staff |
| DATA-01 | V1–V6 applied immutable; security contacts do Identity sở hữu | Forward schema/backfill/model cho customer admission/proofs/contacts nếu cần; personal business profile không duplicate password/session/security verified flags | Identity+Merchant; cold/upgrade/permissions/old-build compatibility |
| PRIVACY-01 | Provider/retention/evidence policies chưa toàn bộ approved | Exact verification channels, abuse/size/file/retention/consent and manual-review access; secret protection, sandbox label; production provider không tự chọn | Security+Owner/Ops; first public exposure/first evidence upload |

## Hợp đồng cần đóng, chưa đặt tên endpoint/permission/table

- Customer registration/verify/activate/login/me/logout/refresh/recovery behaviors; schema/errors/status/CSRF/Origin/limiter/idempotency và non-enumeration fixtures. Giữ exact username/password policy đã duyệt trừ amendment explicit; không tự normalize dữ liệu cũ.
- Identity authoritative subject/current-session API; business authority thuộc resource owner. Không compile-time cycle identity→merchant→identity; bridge/composition/workflow direction cần review, không mở internals/allowlist để xanh.
- Merchant membership revocation chặn quyền org tại submit/replay ngay khi revoke commit; unrelated personal/other-org access còn hợp lệ. Account disable/security reset do Identity thu hồi sessions theo accepted contract. JWT signature/claim chỉ là một điều kiện, không current membership cache authority.
- Giữ session-owned transaction/admission; Merchant approval không cần sửa credentials/Realm: applicant đã active verified, approval tạo org+initial membership+application outcome+audit/receipt atomic trong Merchant owner TX. Cross-owner effects khác chỉ qua accepted protocol, không ambient TX wrapper tùy tiện.
- Refresh vẫn single-flight, strict replay family revoke, lost rotation response re-auth; không blind retry. Keep RS256 external protected keys, exact Origin/CSRF/cookies, backend revocation/deadlines/shared limiter, full-target union/self/last-admin protections.

## Required evidence trước feature acceptance

Contract positive/negative/mutation fixtures; internal Realm regression unchanged; customer cannot acquire SYSTEM; A/B+personal isolation; disabled account, suspended org, expired/revoked member và grant races; duplicate enrollment/invite accept; safe contact reverify; proof/credential/receipt/audit rollback; PG18 empty+upgrade/runtime roles; real HTTP filter chain; real browser register→verify→login→personal→Merchant application→review→context→revoke. Verified delivery/maintenance cho journey sử dụng; không QUEUED=ACTIVE. Recovery chưa có implementation thì không ghi hoàn tất.

Readiness: BLOCKED cho account-enabled production implementation tới khi technical amendments/fixtures approved. Không yêu cầu mở lại mọi Foundation finding hoặc full unrelated Identity feature; preserved findings vẫn OPEN đúng phạm vi.
