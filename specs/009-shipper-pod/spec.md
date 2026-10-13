# Feature 009: Shipper Mobile: lấy/giao/hoàn, POD và COD collection

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `order, dispatch, finance, shipper-mobile`  
**Actors:** Shipper, Dispatcher, Recipient (proof only)  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Shipper Mobile: lấy/giao/hoàn, POD và COD collection là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Shipper, Dispatcher, Recipient (proof only)**, tôi muốn thực hiện nghiệp vụ **Shipper Mobile: lấy/giao/hoàn, POD và COD collection** để đạt: **Shipper được giao ghi DELIVERED đúng một lần kèm POD hợp lệ; Order delivery record và Finance COD collected (nếu có) nhất quán.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Shipper đăng nhập mobile → xem assignment active → bắt đầu nhiệm vụ → scan parcel → xác nhận lấy/bàn giao → giao thành công có POD đúng attempt hoặc thất bại có reason → ghi COD thu thực tế → sync và xem kết quả.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-009-01 | Chỉ xử lý đúng shipment/leg trong assignment active của shipper, revalidate tại server lúc ghi nhận. | FR-S01 FR-S02 FR-S04 FR-S05 |
| REQ-009-02 | Mỗi delivery attempt có ID; giao thành công bắt buộc POD tối thiểu theo policy; POD gắn đúng attempt. | FR-S01 FR-S02 FR-S04 FR-S05 |
| REQ-009-03 | Failed delivery có reason và attempt ID; giao lại/hoàn theo policy, không nhảy trạng thái trực tiếp. | FR-S01 FR-S02 FR-S04 FR-S05 |
| REQ-009-04 | COD collected là nghĩa vụ shipper; không tự chuyển remittance thành CONFIRMED. | FR-S01 FR-S02 FR-S04 FR-S05 |
| REQ-009-05 | POD metadata ở DB và file private object storage, giới hạn upload/permission; offline action chỉ khi policy cho phép. | FR-S01 FR-S02 FR-S04 FR-S05 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-S01 | Cung cấp ứng dụng React Native để xem, bắt đầu và thực hiện nhiệm vụ được giao. | S01 || FR-S02 | Quét/nhập mã khi lấy, giao–nhận kiện và giao/hoàn. | S02–S05 || FR-S04 | Ghi kết quả giao, thất bại, giao lại hoặc hoàn; thành công có POD, thất bại có lý do. | S04, S05 || FR-S05 | Ghi COD thực thu, khai báo nộp và bằng chứng để Finance xác nhận. | S04, S08 |

### Business invariants / NFR cần giữ
- **BR:** BR-01, BR-04, BR-05, BR-09, BR-16.
- **NFR:** NFR-R03, NFR-S03, NFR-S05.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-009-01: Đúng hạn/qua hạn nhiệm vụ
- EC-009-02: POD upload success nhưng command fail
- EC-009-03: Hai thiết bị cùng shipper
- EC-009-04: Double submit
- EC-009-05: Partial COD collection
- EC-009-06: Return after failed retries

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-009-01 — happy path / REQ-009-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 005, 008, 006 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Shipper đăng nhập mobile → xem assignment active → bắt đầu nhiệm vụ → scan parcel → xác nhận lấy/bàn giao → giao thành công có POD đúng attempt hoặc thất bại có reason → ghi COD thu thực tế → sync và xem kết quả.
- **Then** Shipper được giao ghi DELIVERED đúng một lần kèm POD hợp lệ; Order delivery record và Finance COD collected (nếu có) nhất quán.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-009-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** POD của attempt khác, assignment bị thu hồi, sai parcel, duplicate scan, mất mạng khi upload, COD mismatch → conflict/pending không thành công giả.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-009-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-009-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-009-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `005`, `008`, `006`.
- **Ngoài phạm vi:** Advanced OTP/POD biometrics, production-grade camera scanning analytics.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Allowed POD form photo/signature/OTP and fields, file provider, max size, retry/delivery attempt limits, offline-safe mutations.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
