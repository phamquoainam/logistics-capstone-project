# ArgoPost — Bàn giao tiến độ cho trợ lý Owner

Ngày tổng hợp: **2026-11-19**. Checkpoint: **CSKH_STAFF_AUTHORITY_TECHNICAL_REVIEW_READY**.

Bạn đóng vai **trợ lý Owner của dự án ArgoPost**, hỗ trợ xét duyệt nghiệp vụ,
technical contracts, security và deployment checkpoints. Hãy dựa trên trạng thái
thực tế dưới đây, đọc các artifact được dẫn chiếu trước khi đưa ra quyết định kỹ thuật.

## 1. Bối cảnh và nguyên tắc làm việc

- Dự án: **ArgoPost — E-Logistics Capstone**.
- Repository: `phamquoainam/logistics-capstone-project`.
- Working directory duy nhất: `D:\TLCN\e-logistic`.
- Phương pháp hiện hành: **Owner-Directed, Feature-Driven Development**.
- Stack: React/Vite, Spring Boot modular monolith, PostgreSQL 18, Flyway;
  Local HTTPS, Amazon S3 và Gmail SMTP.
- Personal dùng navy; Business dùng orange, giữ nhận diện ArgoPost.

Các nguyên tắc bắt buộc:

- Không reset/clean/discard dữ liệu, không tạo thêm worktree.
- Không sửa migrations đã áp dụng hoặc dùng Flyway repair/clean để bỏ qua lỗi.
- API/DTO/schema/permission mới phải có technical approval theo `AGENTS.md`.
- Business luôn kiểm tra **Organization Ownership + Active Membership + Permission
  + Scope** tại Backend.
- Merchant Owner chỉ quản lý Organization của mình, không phải Platform Admin
  hoặc Carrier Staff.
- Phân biệt **source/test PASS**, **local deployment**, **runtime activation**,
  **UAT** và **GitHub CI**.
- Chỉ ghi DONE khi có evidence; không dùng mock/unit PASS thay cho required
  integration/security gates.

## 2. Những nền tảng và chức năng đã phát triển

| Nhóm | Kết quả chính |
| --- | --- |
| Public Website và Customer Identity | Giao diện ArgoPost; đăng ký OTP 6 số/2 phút, resend và giới hạn nhập sai; chỉ tạo account sau khi xác minh thành công; đăng nhập, logout, khôi phục mật khẩu bằng OTP rồi đặt mật khẩu mới |
| Local full stack | Launcher Windows, HTTPS, PostgreSQL 18, cơ chế local secrets; Customer đăng ký/đăng nhập/khôi phục bằng Gmail đã được Owner xác nhận hoạt động ở checkpoint trước |
| Personal Workspace | Dashboard/analytics và drill-through, sổ địa chỉ, Shipment Draft, Quote, Booking, quản lý Shipment, báo cáo/cước, tra cứu, nhãn và tiện ích, Support Ticket V1 |
| Shipping Pilot | ROAD_STANDARD, bốn tuyến SIM được duyệt, drop-off; Quote TTL 15 phút; Shipment/Parcel thật trong PostgreSQL và snapshot tại Booking |
| Mốc B | Shipping Label 100 × 150 mm, Code128; Station intake toàn bộ Parcel, authorization/assignment, custody/tracking/audit nguyên tử |
| Business Workspace | Organization/Membership/RBAC, dashboard, chuẩn bị đơn và CSV/Excel, Quote/Booking/nhãn theo Merchant contract, quản lý vận đơn, Reports/Finance đọc, Action Center, danh bạ Organization, Kho & Điểm lấy hàng, Tracking/Quotation và Labels/Utilities |

Các phần trên có phạm vi triển khai theo contract đã được duyệt; **không đồng
nghĩa toàn bộ nền tảng logistics thương mại đã hoàn chỉnh**.

Những giới hạn nghiệp vụ vẫn phải giữ:

- Booking không đồng nghĩa Carrier đã nhận hàng hoặc đã thu tiền.
- Pilot tạo nghĩa vụ `UNPAID`; không tự ghi `PAID`.
- Không tự mở COD, payout, reconciliation, Pickup hoặc các trạng thái Carrier
  chưa có workflow/authority.
- Không dùng dữ liệu Personal để làm KPI Business.
- Claim/bồi thường, evidence upload và Shipment mutation không thuộc Organization
  Support V1 đã triển khai.

## 3. Organization Team V1 — đã triển khai và nghiệm thu Local/Test

Technical manifest Team được Owner duyệt có SHA-256:

```text
f6e21ee3ec3f742035d37f2c10d624031cc450a9b590939df878e9397952f426
```

Đã hoàn thành:

- OTA-04/05 source/test.
- Migration **V34**.
- Thành viên Organization, lời mời 72 giờ, accept/decline/resend/revoke.
- Phân quyền theo ceiling và scope; suspend/resume/revoke có reauthentication.
- ETag/version, idempotency, receipt và audit.
- Bảo vệ Initial Owner, chống privilege escalation và cross-Organization.
- Local/Test deployment cùng đúng năm quyền Team cho Initial Owner NamShop Demo.
- Owner đã xác nhận UI và các bước UAT theo checkpoint; evidence chi tiết nằm
  trong execution report.

Không triển khai Owner Transfer hoặc các role amendments Warehouse/Finance/Support
chưa được duyệt.

Evidence lịch sử phải giữ chính xác: **original full Team PostgreSQL run có
1 FAIL; focused corrected rerun 10/10 PASS**. Không sửa lại kết luận của lần chạy
cũ thành PASS.

Artifact chính: [Organization Team execution](organization-team-execution.md).

## 4. Organization Support V1 — source/test hoàn thành, schema local đã sẵn sàng

Technical manifest Support được Owner duyệt có SHA-256:

```text
9484c415356e601ee8192e84a09af2e39f2f448d07244457de1ef79a09c3a940
```

Các task **OSC-04/05/06 DONE tại phạm vi source/test**, gồm:

- Ticket chung hoặc liên kết Shipment đúng Organization/Warehouse.
- Hội thoại Merchant–CSKH.
- Lifecycle, review/closure.
- Phân công nội bộ Merchant tách biệt Carrier CSKH assignment.
- Authorization, tenant isolation, ETag, idempotency, concurrency và rollback.
- Typed receipt/audit, notification từ sự kiện đã commit.
- Báo cáo phản hồi dựa trên timestamp thực.
- UI, HTTP/Browser và PostgreSQL tests trong môi trường thử nghiệm riêng.

Evidence tiêu biểu đã ghi nhận:

- 9 Organization Support cases PASS qua các focused runs.
- 5 Personal Support regression cases PASS.
- V35 upgrade/preservation test PASS.
- Browser integration cuối cùng **19/19 PASS**, dùng HTTPS + PostgreSQL thật
  + test SMTP.
- Frontend và Architecture checks PASS.

**V35 đã được Owner cho phép áp dụng lên Local/Test** và đạt:

```text
LOCAL_SUPPORT_V35_SCHEMA_READY
```

- V35 có 12 Support tables.
- Flyway latest V35, checksum đã ghi nhận: `-599636176`.
- Backup đã được restore thử và đối chiếu.
- Không tạo dữ liệu Ticket/audit/notification giả.
- V33 không tồn tại trong stream; không được thêm placeholder.

Artifact chính: [Organization Support execution](organization-support-execution.md).

## 5. Merchant Support grants đã hoàn thành; Staff activation vẫn bị chặn

Đích Local/Test đã được Owner xác nhận:

| Định danh | Giá trị |
| --- | --- |
| Database | `127.0.0.1:5433/elogistics_dev` |
| Organization | NamShop Demo |
| Organization ID | `782c4766-8bdd-435b-9961-512c831922d6` |
| Initial Owner Account ID | `5d206ef3-350e-42b7-af8a-4c54b8d43fe2` |
| Initial Owner Membership ID | `d34e1afe-4d3d-4e6e-bd63-449ddca5f92f` |

Đã cấp đúng năm quyền, đều `ORGANIZATION` scope:

```text
support.read
support.write
support.assign
support.reports.read
support.notifications.manage
```

Operator procedure có audit/receipt; same-key replay không tạo thêm grant/audit
hoặc tăng version lần nữa. Không cần cấp lại.

Tại lần discovery/preflight đã ghi nhận:

- 3 Identity accounts hiện hữu.
- Không có Staff candidate phù hợp.
- Canonical `SUPPORT_TRIAGE` và `SUPPORT_HANDLE`: **0**.
- Organization Staff bindings: **0**.
- SYSTEM realm: **INACTIVE**.
- `support.tickets.organization-enabled`: **default-off**.

Đây là evidence tại checkpoint đã lưu, không phải tuyên bố đã truy vấn lại database
ngày tổng hợp này.

## 6. Blocker thực sự của CSKH Staff Authority

Owner từng cho phép tạo một dedicated CSKH Staff và cấp hai canonical permissions,
**với điều kiện cơ chế canonical hiện hữu đã đáp ứng yêu cầu**. Preflight phát hiện
điều kiện này chưa đạt:

1. Provisioning/verification hiện hữu yêu cầu management actor và ACTIVE SYSTEM
   realm; chưa có nhánh dedicated non-admin Staff phù hợp.
2. Staff adapter Organization Support còn dùng Customer admission.
3. Hàm canonical hiện hữu `orders.configure_local_support_staff(uuid,text,text)`
   có operator audit và state-idempotency, nhưng thiếu request ID, payload digest
   và durable receipt. Replay sau revoke có thể tạo grant mới.

Vì vậy đã dừng trước mutation và báo:

```text
SECURITY_BLOCKED — CANONICAL_STAFF_PROVISIONING_REQUIRED
```

Không dùng Customer đổi nhãn thành Staff, không bật SYSTEM, không direct DML hoặc
tạo authority thay thế.

## 7. Checkpoint hiện tại: CSKH_STAFF_AUTHORITY_TECHNICAL_REVIEW_READY

Owner đã cho phép **thiết kế**, chưa cho phép triển khai hai amendments:

- `CSKH-IDENTITY-01`: dedicated Local/Test Staff provisioning, verification và admission.
- `CSKH-GRANT-02`: canonical Support grants có request-bound receipt/replay.

Ba artifact đã hoàn thành:

- [Contract đề xuất](cskh-staff-authority-contract.proposed.md).
- [Exact technical manifest](cskh-staff-authority-technical-manifest.json).
- [Technical review](cskh-staff-authority-technical-review.md).

SHA-256 của **bytes file manifest JSON**, đã được đối chiếu ngày tổng hợp:

```text
370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb
```

Manifest vẫn là **[PROPOSED]**, gồm:

| Thành phần | Phạm vi đề xuất |
| --- | --- |
| API | 6 Staff auth/activation operations |
| DTO | 13 DTO |
| Interfaces | 6 nhóm port/composition |
| Storage mới | 4 bảng |
| Storage hiện hữu | 6 nhóm delta additive |
| SQL | 10 mục signature |
| Guards | 4 nhóm |
| ACL | 3 NOLOGIN roles và các giới hạn privilege |
| Migration | V36/V37 đề xuất, chưa tạo SQL |
| Verification | CSKH-T-01–10 là test plan, chưa chạy |

Bốn bảng đề xuất:

```text
identity.local_staff_environment
identity.local_staff_principal
identity.local_staff_operator_receipt
orders.support_grant_operator_receipt
```

Thiết kế chính:

- Staff UUID do hệ thống sinh; pending account không có mật khẩu, chưa verified.
- Invitation `STAFF_INV` hiệu lực 24 giờ; activation thực qua proof và password policy.
- Tái sử dụng Identity/crypto/session/SMTP engines hiện hữu.
- Staff audience, cookies, CSRF và admission riêng; không mở SYSTEM management.
- Chỉ trusted offline operator được provisioning.
- Grant request-bound: cùng request/input trả receipt cũ; input khác reject;
  replay sau revoke không tự cấp lại.
- Legacy operator signature trở thành deny-only.
- Organization binding và assignment vẫn là điều kiện độc lập.
- Environment default disabled, Production denial.
- Không tự cấp quyền hoặc triển khai localhost qua migration.

**Đã kiểm chứng tài liệu:** JSON/IDs/cross-reference/hash/source paths và docs
validator PASS. Các kiểm thử PostgreSQL/HTTP/security/Browser của amendment
**chưa thực thi**.

| Task | Trạng thái |
| --- | --- |
| CSKH-DESIGN-01 | DONE — design only |
| CSKH-DESIGN-02 | DONE — design only |
| CSKH-IMPLEMENT-01 | NOT STARTED — awaiting technical approval |
| CSKH-DEPLOY-01 | NOT STARTED — separate checkpoint |

## 8. Git và CI tại thời điểm tổng hợp

Các ghi chú “WIP chưa commit” hoặc “HEAD c8cdfa3” trong artifact là bối cảnh lịch sử
lúc thiết kế. Sau đó Owner đã yêu cầu push/merge:

- [PR #49](https://github.com/phamquoainam/logistics-capstone-project/pull/49)
  đã **MERGED**.
- PR HEAD: `58f54a2010f8fd5ed911556be7b8ea41d3ba22d7`.
- Merge commit trên `develop`: `420e948beeffe7d1e596e3a004174b3b4ed3d4c6`.
- Architecture, H2 và PostgreSQL 18 trên PR HEAD đều PASS tại
  [PR CI run](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/44351063704).
- [CI hậu merge](https://github.com/phamquoainam/logistics-capstone-project/actions/runs/44355453227)
  đã **SUCCESS** trên đúng merge SHA, được kiểm tra khi lập bản bàn giao.
- Trước khi thêm file bàn giao này, local worktree sạch, vẫn ở
  `feature/argopost-business-shipping-tools`, HEAD `58f54a2`;
  ref `origin/develop` ở `420e948`.
- Bản sửa CI chỉ cập nhật 15 assertion đếm migration để tính thêm V35.

CI PASS này là evidence tích hợp Team/Support source hiện hữu; **không chứng minh
CSKH amendments đã được triển khai**. Việc merge tài liệu `[PROPOSED]` cũng không
biến chúng thành technical approval.

File bàn giao này là tài liệu tổng hợp mới; không thay đổi manifest hoặc cấp thêm
implementation/deployment authority.

## 9. Quyết định Owner cần xét tiếp

Chỉ cần một checkpoint duyệt technical manifest đúng hash trên, gồm:

- **CSKH-DEC-01:** Staff purpose, invitation/verification, API/DTO/admission.
- **CSKH-DEC-02:** Schema, guards, functions và privilege/operator isolation.
- **CSKH-DEC-03:** Request-bound canonical grants, replay và legacy retirement.
- **CSKH-DEC-04:** V36/V37 nếu còn trống, cùng source/test trên isolated PostgreSQL 18.

Nếu được duyệt, bước tiếp theo là **implementation và focused tests trên database
thử nghiệm riêng**. Chưa bao gồm migrate demo, cấp operator authority, provisioning
Staff, canonical grants, Organization binding, feature activation hoặc Ticket UAT
localhost.

**Hành động Owner tiếp theo:** Đọc ba artifact CSKH và đưa ra một quyết định kỹ thuật
cho manifest SHA-256
`370f965872c4bb3997cc868ebb6d89c945a88035eea484e07f3501c1e9b798cb`, nêu rõ phạm vi
source/test được phép hoặc các delta cần sửa.
