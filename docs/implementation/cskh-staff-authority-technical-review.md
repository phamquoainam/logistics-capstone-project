# CSKH Staff Authority — technical review checkpoint

Ngày: 2026-11-18.
Verdict: **CSKH_STAFF_AUTHORITY_TECHNICAL_REVIEW_READY** — design only.
Technical approval/implementation/deployment: **chưa được cấp bởi checkpoint này**.

Hai artifact cần đọc cùng review này:

- [Contract [PROPOSED]](cskh-staff-authority-contract.proposed.md).
- [Exact technical manifest [PROPOSED]](cskh-staff-authority-technical-manifest.json).

SHA-256 của **bytes file JSON**, không phải JSON serialize lại:

```text
370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb
```

## 1. Kết luận kỹ thuật

Canonical Identity/Support hiện có phần lớn engine cần thiết; chưa có workflow
được phép tạo dedicated Staff không thuộc SYSTEM management. Operator canonical
Support tồn tại nhưng mới state-idempotent, chưa request-bound. Không có cơ sở
provision/grant bằng các cơ chế hiện hành mà vẫn đáp ứng Owner conditions.

Gói này đề xuất đúng hai amendment, giữ existing account/credential/proof/session/
canonical grant engines. Không thiết kế lại Organization Support hoặc tạo authority
song song. Các delta admission phụ thuộc được chỉ rõ, không ẩn trong implementation.

| Manifest [PROPOSED] | Số lượng / phạm vi |
| --- | --- |
| CSKH-API-01–06 | 6 operation Staff auth/activation mới, không public Staff provisioning |
| CSKH-HTTP-AMEND-01 | 6 operation Organization Staff hiện hữu chỉ đổi admission; paths/DTO nghiệp vụ giữ nguyên |
| CSKH-DTO-01–13 | 10 boundary DTO và 3 typed server request/admission DTO, field đầy đủ |
| CSKH-PORT-01–06 | Staff admission/context, offline composition, fixed session/delivery composition và adapter workflow |
| CSKH-DB-01–04 | 4 bảng mới có lý do không reuse, PK/FK/unique/lifecycle rõ ràng |
| CSKH-DB-05–10 | 6 nhóm delta additive trên storage/constraint/eligibility hiện hữu |
| CSKH-SQL-01–10 | 10 mục signature, trong đó SQL-06 có predicate và locking function; SQL-08 là legacy deny-only |
| CSKH-GUARD-01–04 | 4 nhóm trigger/constraint bảo vệ direct DML, evidence và audience |
| CSKH-ACL-01–05 | 3 NOLOGIN roles và giới hạn runtime/deployment, không auto-grant login |
| CSKH-MIG-01/02 | V36/V37 được đề xuất, chưa tạo file hoặc chạy migration |
| CSKH-T-01–10 | Focused test plan, **chưa thực thi** |

Mọi item mới/changed có ID ổn định và status `[PROPOSED]`. Evidence CSKH-EV-01–15
là quan sát source hiện hữu, không phải nội dung xin phê duyệt triển khai.

## 2. Source/catalog evidence và phần được tối giản

| Evidence | Source quan trọng | Kết luận |
| --- | --- | --- |
| EV-01/02/03 | `ProvisionIdentityAccount`, `IdentityRealmGrantPolicy`, `JdbcIdentityInvitationStore` | UUID/activation/receipt có thể reuse; management actor + ACTIVE SYSTEM yêu cầu nhánh Staff explicit |
| EV-04 | `JdbcInvitationOutbox` | Có lease/retry/erase protected payload; không có network sender trong class. Không suy diễn mail live PASS |
| EV-05/06 | `IdentitySessions`, `ProductionAccessTokens` | Giữ 8h/30m/15m và RS256 audience engine; không thêm JWT claims hoặc auth engine |
| EV-08 | V22 `configure_local_support_staff` / audit CHECK | Hàm cũ tồn tại, operator-only, audit session_user; same ACTIVE grant không ghi trùng audit. Không có request receipt; replay sau revoke có thể cấp mới |
| EV-09 | V35 binding operator / `organization_support_staff` | Org binding và assignment đã có, giữ lại. Binding operator đang gọi Customer-only Identity lock: cần Staff-specific dependency |
| EV-11 | `JdbcCustomerEnrollmentStore.emailOccupied` | Đã kiểm tra mọi `user_account.email` bằng exact C equality. Không cần thêm Customer API/DTO để ngăn signup trùng Staff |
| EV-12/13 | V3 command_receipt, V35 support_operator_audit | Offline actor không phải Identity actor; global grant không phải Organization binding. Không thể reuse hai receipt shapes này bằng UUID/Organization giả |
| EV-14 | `JdbcOrganizationSupportTickets.staffReferences/notify` | Staff callback còn cần referenced Customer eligibility để thông báo Merchant. Actor phải Staff, reference không cấp admission |
| EV-15 | `ProductionCustomerRequestAdmission` | CSRF/ThreadLocal là Customer-specific. Dùng Staff context/filter riêng, không chỉ đổi JWT verifier trong controller |

Đường dẫn exact của từng source ở manifest; chúng đã được kiểm tra tồn tại. Catalog
evidence đã được lưu ở execution log từ checkpoint trước: database/container duy nhất,
V35 latest checksum `-599636176`, SYSTEM INACTIVE, ba accounts, zero canonical Support
grants và zero Organization bindings. Không chạy lại live discovery để thiết kế.

Frozen source fingerprints đã đối chiếu trong checkpoint:

| File | SHA-256 |
| --- | --- |
| V34 source | `07ebf9fd74aa31c178ab4e849761c4f3b513fdee7c8ad46269713661de6f0f4c` |
| V35 source | `33e7e8b532de598627c321a454fd293157fb6c3a365cede946a03c3267d4844b` |
| Organization Support approved manifest | `9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940` |

## 3. Review các bất biến

| Bất biến | Thiết kế kiểm soát | Gate implementation bắt buộc |
| --- | --- | --- |
| Không Customer relabel/implicit admin | Immutable STAFF/SUPPORT marker, exclusive identifiers/roles/memberships; system UUID, pending unverified | T-02/03/05/08 |
| SYSTEM INACTIVE, không IDENTITY_MANAGEMENT | Staff proof không có Realm; format-v1/management policy cũ giữ nguyên | T-01/04/05/08 |
| Chỉ trusted operator | session_user + current operator role + bound DB/environment; writer roles không cho runtime/operator SET ROLE | T-02/06/08 |
| Verification thực | HMAC possession + password policy; account/proof/marker/audit/receipt atomic; không SMTP-success activation | T-03/04/05 |
| Session purpose tách biệt | Fixed JWT audience + DB audience + Staff CSRF/context/filter; không token fallback | T-02/05/09 |
| Replay không cấp lại quyền | Durable receipt, actor/payload/DB binding, current admission trước replay; original vs effective grant phân biệt | T-06/07 |
| Không cross-Org | Canonical receipt scope CANONICAL_STAFF; exact binding + case assignment xét hiện hành | T-07 |
| Không mất notification | Customer reference eligibility đọc-only, Merchant permission/preference vẫn kiểm tra, unique event/recipient | T-07 |
| Không writable legacy bypass | Legacy body chỉ reject, update existing script bắt buộc request/reason | T-02/06 |
| Không sửa lịch sử/data cũ | Additive V36/V37, giữ legacy audit; migration chưa chạy, no V33 placeholder | T-01/08/10 |

Đây là **design review**, không phải test evidence rằng các guard đề xuất đã hoạt động.
Database guards bảo vệ DML/lifecycle/provenance; crypto/password vẫn thuộc trusted
Identity application. Không tuyên bố SQL tự chống được mọi hành vi của kẻ đã chiếm
runtime DB credentials hoặc superuser/schema-owner DDL authority.

## 4. Compatibility và rủi ro còn lại

1. Shared SYSTEM session storage giảm ba bảng trùng lặp nhưng buộc thêm audience
   predicate tại mọi session/refresh/CSRF/timing lookup. Một lookup thiếu predicate
   là blocking defect, không thể thay bằng frontend guard hoặc unit test riêng lẻ.
2. V5 invitation format 1 phải giữ nguyên crypto fixtures; Staff format 2 là purpose
   mới, null realm có encoding rõ. Không đổi key/certificate đang dùng.
3. V35 binding operator cần đổi đúng target-admission call. Không broaden
   `identity.lock_local_business_subject`, không tự cấp/bind Staff trong migration.
4. Nếu deployment tương lai có active canonical grant cho non-Staff, dừng để Owner
   quyết định. Không migrate lịch sử thành Staff hoặc tự revoke account khác.
5. Physical DB binding ngăn activate nhầm bản sao có identity khác; clone cố ý giữ
   nguyên identity và privileged roles không thể tự phân biệt hoàn toàn. Production
   profile denial và deployment control vẫn là lớp độc lập bắt buộc.
6. Nếu migrator không đủ quyền cài NOLOGIN writers/đọc physical DB identity an toàn,
   dừng checkpoint deployment; không dùng owner DML như lối tắt.
7. V34/V35 và source Team/Support đang có WIP. Gói này không chứng minh integration
   hoặc required CI của chúng. Bảo toàn tất cả, không commit/push/merge ở đây.
8. SMTP và real mailbox verification chỉ được nghiệm thu khi có checkpoint runtime
   hợp lệ; hiện chưa có Staff UUID, verification proof hoặc grant UUID mới.

## 5. Kiểm chứng tài liệu và trạng thái thực thi

Các phép kiểm chứng ở checkpoint này chỉ gồm JSON parse, unique IDs/status,
cross-reference/source-path/hash consistency, migration-number availability trong
source, local Markdown links, Git whitespace và repository docs validator ở chế độ
implementation để nhận diện WIP hiện hữu. Kết quả chi tiết được ghi trong traceability
và báo cáo checkpoint; không chạy application/DB tests trong một task design-only.

Không có V36/V37 SQL file, API implementation, new role, new account, grant,
binding, feature flag, runtime artifact hoặc process nào được thay đổi. Không đọc
credentials/verification proofs. Không lặp backup/restore/source-test hoặc Ticket UAT.

Evidence cũ vẫn giữ nguyên: original full Team PostgreSQL run có **1 FAIL**, focused
rerun **10/10 PASS**. Không chuyển thành full-suite/GitHub CI PASS; test plan CSKH
không được dùng để thay evidence thực thi.

## 6. Một checkpoint Owner cần duyệt

Owner duyệt hoặc nêu delta cho **đúng hash manifest trên**, gồm CSKH-DEC-01–04:

- Staff purpose/verification/admission, API/DTO và existing Staff route admission.
- Exact schema/guard/function/privilege/operator isolation và environment disabled.
- Request-bound grants, current-authority replay, legacy retirement, V35 eligibility dependency.
- Forward-only V36/V37 và source/test **trên isolated PostgreSQL18**, sau khi xác nhận
  hai số vẫn còn trống; không tự triển khai Local/Test demo.

Không cần duyệt lại Organization Support nghiệp vụ/API đã được chấp thuận. Việc
technical review READY không gỡ execution block: provisioning, grants, Organization
binding, activation và UAT vẫn chưa được thực hiện hoặc tuyên bố PASS.

### K?t qu? ki?m ch?ng thi?t k? ?? ch?y

- JSON parse, stable unique IDs, [PROPOSED] status, API uniqueness, cross-reference
  v? source-path/hash consistency: PASS.
- `python -B docs/reconciliation/v3/validate.py --implementation`: exit 0,
  `errors=[]`, semantic ownership CONSISTENT, 182 changed Markdown ???c ki?m tra.
  Gi? nguy?n 10 pre-existing broken links ???c validator ph?n lo?i; kh?ng tuy?n b?
  ?? s?a c?c li?n k?t l?ch s? ??. Ch? ?? implementation ch? nh?n di?n WIP ngu?n
  ???c Owner cho ph?p tr??c ??y, kh?ng c?p th?m implementation authority ? task n?y.
- `git diff --check`: exit 0; ch? c? Git line-ending notices tr?n WIP hi?n h?u.
- Local links/hash review v? preservation fingerprints ???c ??i chi?u cu?i checkpoint.
- Kh?ng ch?y PostgreSQL/HTTP/Browser/full suite/CI m?i: design-only scope, kh?ng c?
  application source thay ??i. CSKH-T-01?10 v?n ch?a th?c thi.
