# Feature 013: Admin RBAC, cấu hình hệ thống, audit và recovery

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `identity, platform, notification (read), other owner APIs`  
**Actors:** Admin, Security Auditor, Finance (separate permission)  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Admin RBAC, cấu hình hệ thống, audit và recovery là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Admin, Security Auditor, Finance (separate permission)**, tôi muốn thực hiện nghiệp vụ **Admin RBAC, cấu hình hệ thống, audit và recovery** để đạt: **Admin thay TTL hợp lệ, hệ thống dùng version mới cho quote tương lai; failed job được retry có log và không gây tác động trùng.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Admin quản lý tài khoản/role → cập nhật ngưỡng cấu hình có reason → xem audit/failed job → thử lại job có quyền → quan sát health; Finance chỉ xử lý tài chính qua quyền chuyên biệt.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-013-01 | Mọi cấu hình quote TTL, delivery retries, hub backlog, GPS freshness có version/audit và validate range. | FR-AD01 FR-AD02 FR-AD03 FR-P01 |
| REQ-013-02 | Job recovery có reason, actor, attemptedAt và kết quả; không bỏ domain guard, không sửa ledger/kỳ CLOSED. | FR-AD01 FR-AD02 FR-AD03 FR-P01 |
| REQ-013-03 | Audit gồm who/what/when/target/outcome, không thể bị CRUD thường sửa hoặc xóa. | FR-AD01 FR-AD02 FR-AD03 FR-P01 |
| REQ-013-04 | Admin chỉ dùng public interfaces từng owner khi cần admin operation; không DML trực tiếp schema khác. | FR-AD01 FR-AD02 FR-AD03 FR-P01 |
| REQ-013-05 | Published tariff được tạo/duyệt qua Pricing API theo role chuyên biệt, không sửa version cũ. | FR-AD01 FR-AD02 FR-AD03 FR-P01 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-AD01 | Quản lý tài khoản, trạng thái, vai trò và phạm vi quyền. | AD01, AD02 || FR-AD02 | Quản lý cấu hình thời hạn báo giá, giao lại/hoàn, ngưỡng hub/GPS và thông báo; kiểm tra, lưu vết thay đổi. | AD03 || FR-AD03 | Tra cứu audit và tác vụ lỗi; thử lại/xử lý thủ công có quyền, nguyên nhân và lịch sử; không bỏ qua bất biến. | AD04, O01 || FR-P01 | Tạo, kiểm tra, công bố/ngừng phiên bản giá; giữ lịch sử và phiên bản đã dùng. | F04 |

### Business invariants / NFR cần giữ
- **BR:** BR-02, BR-09, BR-12, BR-18, BR-20.
- **NFR:** NFR-S04, NFR-R02, NFR-O01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-013-01: Concurrent config update optimistic version
- EC-013-02: Role revoked while recovery pending
- EC-013-03: Secret config leaked in UI
- EC-013-04: Retry failed event after state changed
- EC-013-05: Audit on denied action

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-013-01 — happy path / REQ-013-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 004, 012 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Admin quản lý tài khoản/role → cập nhật ngưỡng cấu hình có reason → xem audit/failed job → thử lại job có quyền → quan sát health; Finance chỉ xử lý tài chính qua quyền chuyên biệt.
- **Then** Admin thay TTL hợp lệ, hệ thống dùng version mới cho quote tương lai; failed job được retry có log và không gây tác động trùng.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-013-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Giá trị config âm/vượt range, admin tự duyệt COD, replay job đã xử lý, sửa audit hoặc kỳ đóng → từ chối.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-013-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-013-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-013-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `004`, `012`.
- **Ngoài phạm vi:** Centralized SIEM, auto production tuning, secret distribution service.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Quyền quản trị cụ thể; cho phép hot-reload config nào; retention logs và quy trình duyệt thủ công.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L026 risk-fraud-compliance](../../specs/planning/v3/L026-risk-fraud-compliance/spec.md); [L027 platform-admin-audit-governance](../../specs/planning/v3/L027-platform-admin-audit-governance/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Risk case/hold/release đúng quyền — Flag→investigate→propose→approve/release; replay/self-approval deny; Governance phân tách admin/IT/Auditor — Authorized change+reason/approval→audit; deny fake POD/COD/delete audit/impersonation.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
