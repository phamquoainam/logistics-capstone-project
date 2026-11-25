# Admin isolated Local/Test — consolidated execution approval record

Ngày Owner: 2026-11-22, Asia/Saigon.
Checkpoint: **OWNER END-TO-END EXECUTION APPROVED**.
Trạng thái hiện tại: xem [execution evidence](admin-uat-deployment-execution.md).

Đây là một checkpoint triển khai theo bước5 của chỉ thị Owner mới nhất, không
phải xin duyệt lại V39/V40, sáu READ permissions hay AON-ACL-01/02.
Owner đã phê duyệt exact manifest bên dưới để thực thi end-to-end. Nội dung
inventory/preparation phía dưới là trạng thái lịch sử trước triển khai; không
phải yêu cầu duyệt lại. Bytes của JSON được phê duyệt giữ nguyên.

## Record cần duyệt

[Exact deployment manifest](admin-uat-deployment-manifest.json), SHA-256:

`9e0773d7d18bb281e82c06d7efe28cd03d457018561f708dcf1971210ddf916e`

Approval chỉ có hiệu lực với exact bytes/hash này và target bên dưới. Đổi target,
operator membership, mailbox hoặc authority phải dừng phần bị ảnh hưởng. Không
sửa artifact đã được duyệt để tự hợp thức hóa thay đổi. Approved parent manifest
giữ nguyên SHA256 `025ae708cb97c01cf966a695402f730d5073364d7b0352298b7a27a644c9f0bc`.

## Target đã tạo và quan sát

| Thuộc tính | Giá trị |
|---|---|
| Deployment ID | `e9e6c0e4-8266-4c54-88b8-1bfc7ed3f3d0` |
| Container | `argopost-admin-uat-pg18-15f3f2a22479` |
| Container ID | `c1151578cda4e5df4758d9b6e855548c08d976ccefd90fc7f4e91506fef4ad34` |
| Volume mới | `argopost_admin_uat_15f3f2a22479_pgdata` |
| PostgreSQL |18.6, image digest `sha256:afc7e2d441324c0388fa80c3d24f733b4194a4eb7f47dd8ee2b08eb1a24a647c` |
| Connection | `127.0.0.1:5543/argopost_admin_uat_15f3f2a22479` |
| Database OID | `16384` |
| System identifier | `7711028187818553388` |
| Owner hiện tại | `postgres`, chỉ là bootstrap của cluster UAT mới |
| Health | HEALTHY; cổng host trả SCRAM-SHA-256 challenge |
| Application data/schema |0 application tables;0 custom roles; chưa có Flyway history |
| SYSTEM realm dự kiến | `394516b0-a637-4238-b55f-498e0f9d14d3`, chưa tạo; truyền qua placeholder V4 |
| Recovery DB dự kiến | `argopost_admin_uat_15f3f2a22479_recovery`, chưa tạo |

Container/volume có label purpose `admin-isolated-uat` và đúng deployment UUID;
volume PG18 mount `/var/lib/postgresql`, không dùng volume demo. Không tự restart
container demo đang dừng. Target mới chỉ có hạ tầng PostgreSQL rỗng được tạo theo
bước3; chưa cài privileged prerequisites, migration, Identity hoặc operator.

## Exact operator scope chờ duyệt

Tất cả login bên dưới chưa tồn tại, dùng secret mới riêng trong thư mục private;
đều NOSUPERUSER/NOCREATEDB/NOCREATEROLE/NOREPLICATION/NOBYPASSRLS.
Operator secrets không được nạp vào runtime config/Backend DataSource hoặc môi
trường Frontend; SMTP/JWK/DB runtime secrets cũng không truyền sang Frontend.
Đây là tách biệt credential/config và DB authority; không tuyên bố process chạy
cùng Windows user đã có isolation ở mức OS service account.

| Principal dự kiến | Quyền duy nhất/phạm vi |
|---|---|
| `argopost_admin_owner_15f3f2a22479` | NOLOGIN database/schema owner; được SET các writer prerequisite đã duyệt; không kế thừa mặc định |
| `argopost_admin_migrator_15f3f2a22479` | LOGIN; SET owner để Flyway; không CREATE ROLE |
| `argopost_admin_runtime_15f3f2a22479` | LOGIN; chỉ grants runtime do migrations cấp; không membership owner/writer/operator |
| `argopost_admin_op_15f3f2a22479` | LOGIN; INHERIT đúng `elogistics_local_admin_operator`, SET FALSE; không writer/Realm/IAM |
| `argopost_realm_op_15f3f2a22479` | LOGIN; INHERIT đúng `elogistics_local_realm_operator`, SET FALSE; không Admin/Staff/Finance |

Trusted bootstrap `postgres` chỉ dùng offline trên cluster mới này để tạo role,
owner/schema/connect ACL và prerequisite đã duyệt; không đưa credential bootstrap
vào Backend. Các prerequisite NOLOGIN gồm ba role Staff/Support hiện hữu mà stream
V36/V37 yêu cầu và ba role AON; không tạo Staff operator LOGIN hoặc Support grants.

SQL chuẩn bị nhưng **chưa chạy**:
`.demo/admin-uat-15f3f2a22479/privileged-prerequisites.PENDING.sql`, SHA256
`bb9792c0c0250e23772e040b4b276daa2f0f39cbe91e7b0d79ca3e18a26f9d9d`.
File không chứa password literal. Privileged session không log statement/password;
audit nghiệp vụ vẫn do canonical functions/receipts thực hiện. Hardening PUBLIC
CONNECT của các DB mặc định chỉ nằm trong cluster mới; raw `pg_control_system()`
ACL thu hẹp tại đúng UAT DB theo AON-ACL-02. Không áp dụng vào demo/shared.

## Mailbox, artifact và runtime

Owner đã chọn hai mailbox khác nhau cho Initial Root và dedicated Admin. Exact
selection, nonce và checksum binding nằm trong file private được manifest tham
chiếu; không đưa địa chỉ email, password, token/proof/private key vào Git/report.
Không tự lấy Customer/Merchant account làm Admin. Hai Account UUID sẽ do canonical
provisioning thực tế sinh ra sau approval; chưa invent/seed UUID account.

- Dedicated Admin được cấp chính xác sáu `ADMIN_*_READ` đã liệt kê trong manifest.
  Initial Root chỉ có canonical `IDENTITY_MANAGEMENT` pair riêng qua V40/V4.
- Source/JAR khớp checkpoint: JAR SHA256
  `7415fb2995073b3ee52c060c2e69a45fa6dd17c58b016c19de2137193c5bed80`;
  cả39 migration files đóng gói khớp source, V33 vẫn vắng mặt.
- 51 WIP entries gốc và37 migration lịch sử kiểm chứng nguyên vẹn. Current source
  fingerprint575 files được lưu riêng, không sửa code app/CI trong preparation.
- Config/keyring mới chỉ cho UAT; không dùng khóa ký/proof/encryption của demo.
  Chứng chỉ localhost hiện hữu được reuse read-only, thumbprint
  `7E6B44BD8772608CB2230A96864B9A706F486173`, SAN localhost/127.0.0.1 và Current User trust PASS.
- Frontend dự kiến `https://localhost:5273/admin/login`; Backend
  `https://localhost:9443`; bind127.0.0.1. Dùng browser profile riêng vì cookie không
  được cô lập chỉ bằng port. Backend proxy dùng HTTPS127.0.0.1 để tránh IPv6 fallback.
- Gmail STARTTLS handshake/certificate verification PASS, TLS1.3; **chưa AUTH,
  chưa gửi thư**. DPAPI App Password hiện hữu chưa giải mã. Không cần nhập lại secret.
- Các Admin/first-bootstrap/delivery flags hiện FALSE; Backend/Frontend chưa chạy.
  Pool dự kiến4 connections, timeout2000ms; không trust-all TLS/cookie/CSRF bypass.

## Backup và trình tự sau approval

1. Revalidate manifest/source/config/physical binding và labels. Restore dump DB
   rỗng vào đúng recovery DB mới ở trên, so sánh schema/data trước privileged setup.
   Dump981 bytes, archive-list PASS; SHA256
   `a75e12bf1f95d744b0ea3a86ecef444405aaa73f3c992504a2fdb335e3f26ecc`.
   **Restore rehearsal chưa chạy**, không gọi đây là restore-verified backup.
2. Chạy exact prerequisite bằng trusted bootstrap; inventory before/after roles,
   ACL, membership. Migrate stream39 files đến V40, validate/checksum; không sửa
   history, không clean/repair/baseline hoặc chèn V33.
3. Bind physical target/realm bằng canonical operator; khởi động đúng source-built
   Backend/Frontend HTTPS với config target, không dùng launcher demo hardwired.
   Record PID/start-time; chỉ READY khi HTTPS/DB/UI thực sự sẵn sàng.
4. Initial Root nhận protected invitation thật, Owner đặt password/verify trên UI;
   chỉ sau genuine proof mới dùng V40 activation wrapper. Không raw verification DML.
5. Provision dedicated Admin với exact six READ set, protected email thật và Owner
   verification; login bình thường, không auto-session từ acceptance.
6. UAT six APIs/KPI/filter/drill-through, 401/403, refresh/logout, giảm một READ grant
   bằng canonical operator để kiểm chứng revocation rồi restore six bằng request/version
   mới có audit. Không terminally revoke sole Admin hoặc disable vĩnh viễn sole realm.
7. Rehearse rollback bằng target-only flag-off/owned-process stop, xác minh deny rồi
   resume có kiểm soát. Không down migration, reset, xóa volume hoặc thay Root.

## UAT/security coverage thực tế tại checkpoint này

Preparation verification: **15/15 PASS**, evidence private
`.demo/admin-uat-15f3f2a22479/preparation-verification.json`. Bao gồm record/hash,
mailbox binding, setup SQL, JAR/source không drift, flags tắt, không có operator
credential trong runtime, backup hash, không lộ mailbox trong public artifacts,
config/cert demo không đổi, exact planned memberships và target vẫn rỗng/healthy.
Implementation validator PASS, ownership CONSISTENT; giữ10 preexisting link findings.
Không lặp full source suites đã PASS vì app/CI source không thay đổi.

| Capability | Source/test | Real target hiện tại |
|---|---|---|
| V39/V40/ACL/replay/concurrency | PASS, evidence đã được nghiệm thu | Chưa chạy migration |
| Container/volume/physical identity | Chuẩn bị độc lập | PASS, empty target |
| Backup archive | Không thay source acceptance | Dump/list PASS; restore PENDING |
| Operator membership/grants | PASS trên fixture cô lập | Chưa tạo/cấp |
| Mailbox verification/Root activation | PASS với SMTP loopback ở source/test | NOT RUN |
| Admin login/Dashboard/Browser | PASS trên fixture HTTPS | NOT RUN; app chưa start |
| Gmail | Transport hiện hữu | TLS PASS; AUTH/delivery NOT RUN |
| Revocation/rollback | PASS trên fixture | NOT RUN |

Target mới không có Shipment. Không copy PII demo hoặc seed KPI bằng SQL.
Live empty cohort0 có thể nghiệm thu đúng semantics, nhưng không thay bằng chứng
positive aggregation/drill-through. Muốn kiểm chứng positive data cần canonical
source facts được phép tạo riêng; không tự mở thêm account/Pilot/Carrier authority.

Merchant Approval, official Network, advanced Carrier lifecycle, SLA/Finance
settlement/telemetry vẫn UNAVAILABLE theo source coverage; không suy ra revenue
từ UNPAID. Chưa công bố `ADMIN_DASHBOARD_LOCAL_UAT_READY` hoặc GitHub CI PASS.

## Một quyết định Owner cần thiết

Duyệt exact deployment manifest/hash ở đầu trang để cho phép recovery rehearsal,
privileged prerequisites/operator memberships, V1–V40 migration trên target mới,
canonical Root/Admin real onboarding và controlled local deployment/UAT theo đúng
record. Đây là approval gate Owner yêu cầu ở bước5; không phải duyệt lại technical
contract. Không tự thực hiện những mutation này trước quyết định đó.
