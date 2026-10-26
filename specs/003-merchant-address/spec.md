# Feature 003: Hồ sơ merchant, địa chỉ và tọa độ

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `merchant, order(address snapshot), routing(adapter)`  
**Actors:** Merchant, Guest, Admin  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Hồ sơ merchant, địa chỉ và tọa độ là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Merchant, Guest, Admin**, tôi muốn thực hiện nghiệp vụ **Hồ sơ merchant, địa chỉ và tọa độ** để đạt: **Merchant A lưu điểm lấy hợp lệ; địa chỉ giao có tọa độ rõ → có thể yêu cầu báo giá, địa chỉ đã xác nhận thể hiện trong snapshot.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Merchant tạo điểm lấy → nhập địa chỉ có cấu trúc → kiểm tra danh mục hành chính → lấy/điều chỉnh tọa độ → xác nhận thủ công khi mơ hồ → dùng cho quote và shipment.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-003-01 | Pickup point thuộc merchant; user chỉ CRUD điểm được cấp quyền. | FR-M01 FR-M02 FR-G01 |
| REQ-003-02 | Lưu địa chỉ raw, phần có cấu trúc, admin-catalog version, coordinate source/confidence và thời điểm xác nhận. | FR-M01 FR-M02 FR-G01 |
| REQ-003-03 | Không mặc định geocoding thành công khi provider trả thiếu/mơ hồ; có nhánh chọn tay xác nhận. | FR-M01 FR-M02 FR-G01 |
| REQ-003-04 | Order lưu Address Snapshot lúc xác nhận; thay địa chỉ/danh mục sau đó không sửa snapshot. | FR-M01 FR-M02 FR-G01 |
| REQ-003-05 | Không dùng NLP để sửa địa chỉ phi cấu trúc trong P0. | FR-M01 FR-M02 FR-G01 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-M01 | Quản lý hồ sơ merchant và điểm lấy thuộc tổ chức. | M01 || FR-M02 | Nhập/kiểm tra địa chỉ, xác định hoặc chọn tọa độ và xác nhận thủ công khi cần. | M02 || FR-G01 | Ước tính cước công khai từ đầu vào hợp lệ, không tạo vận đơn. | G01 |

### Business invariants / NFR cần giữ
- **BR:** BR-19, BR-10.
- **NFR:** NFR-S01, NFR-U02.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-003-01: Danh mục hành chính có version mới
- EC-003-02: Provider trả điểm khác hành chính
- EC-003-03: Tọa độ lat/lon sai range
- EC-003-04: User edit pickup point đang được đơn cũ tham chiếu
- EC-003-05: Guest quote không tạo shipment

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-003-01 — happy path / REQ-003-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Merchant tạo điểm lấy → nhập địa chỉ có cấu trúc → kiểm tra danh mục hành chính → lấy/điều chỉnh tọa độ → xác nhận thủ công khi mơ hồ → dùng cho quote và shipment.
- **Then** Merchant A lưu điểm lấy hợp lệ; địa chỉ giao có tọa độ rõ → có thể yêu cầu báo giá, địa chỉ đã xác nhận thể hiện trong snapshot.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-003-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Thiếu quận/phường bắt buộc, coordinate ngoài phạm vi, geocoder timeout, bản đồ trả nhiều kết quả → chưa thể xác nhận.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-003-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-003-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-003-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`.
- **Ngoài phạm vi:** NLP/fuzzy free-text normalization và tự suy diễn tọa độ khi mơ hồ thuộc P1.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Dataset danh mục hành chính (phiên bản áp dụng), provider geocoding/matrix, phạm vi hỗ trợ tỉnh/thành và confidence threshold.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L003 sender-merchant-onboarding](../../specs/planning/v3/L003-sender-merchant-onboarding/spec.md); [L004 address-coverage-service-catalog](../../specs/planning/v3/L004-address-coverage-service-catalog/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Khách gửi cá nhân và Merchant tổ chức — Guest chọn loại→xác minh→đúng profile/org; revoke giữa form và submit phải deny; Địa chỉ/điểm lấy và catalog public — Validate→xác nhận tọa độ→lưu/reload; timeout/mơ hồ chưa confirmed.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
