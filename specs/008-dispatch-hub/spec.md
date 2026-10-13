# Feature 008: Hub Operations, manifest, custody và assignment

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `dispatch`  
**Actors:** Dispatcher, Hub Operator, Shipper  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Hub Operations, manifest, custody và assignment là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Dispatcher, Hub Operator, Shipper**, tôi muốn thực hiện nghiệp vụ **Hub Operations, manifest, custody và assignment** để đạt: **Hub quét một kiện thuộc manifest hợp lệ, custody chuyển đúng bên nhận; dispatcher reassign không tạo 2 active assignments.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Dispatcher lập hub/trip/leg/manifest → kiểm Shipment sẵn sàng → assign shipper active → operator scan handover/in/out hub → custody được ghi riêng → giám sát backlog và reassign có reason.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-008-01 | Một leg tối đa 1 ASSIGNED/IN_PROGRESS active; reassign đóng old + tạo new atomically, reason/audit. | FR-D01 FR-D02 FR-D05 FR-W04 FR-W05 |
| REQ-008-02 | Manifest và scan đối chiếu parcel, hub, leg, chain-of-custody và idempotency key. | FR-D01 FR-D02 FR-D05 FR-W04 FR-W05 |
| REQ-008-03 | WMS outbound không đồng nghĩa hub inbound; mốc bàn giao cần chứng từ riêng. | FR-D01 FR-D02 FR-D05 FR-W04 FR-W05 |
| REQ-008-04 | Shipper chỉ scan task được phân công; các kiện sai hub/sai manifest vào exception có người phụ trách. | FR-D01 FR-D02 FR-D05 FR-W04 FR-W05 |
| REQ-008-05 | Hub backlog threshold config và cảnh báo, không tự đổi trạng thái shipment ngoài Order public contract. | FR-D01 FR-D02 FR-D05 FR-W04 FR-W05 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-D01 | Quản lý hub/nguồn lực, tổ chức chuyến và manifest theo chặng. | D01, D02 || FR-D02 | Phân công/gán lại shipper theo chặng, lưu lý do và lịch sử. | D03 || FR-D05 | Giám sát chuyến, vị trí có độ mới, ngoại lệ và cách xử lý. | D06 || FR-W04 | Quét nhập–xuất hub, đối chiếu manifest và xác nhận bàn giao. | D04, S03 || FR-W05 | Theo dõi lưu hub và phát cảnh báo vượt ngưỡng cấu hình. | D06 |

### Business invariants / NFR cần giữ
- **BR:** BR-04, BR-09, BR-01.
- **NFR:** NFR-C01, NFR-C02, NFR-S01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-008-01: Trip leg thay đổi sau plan
- EC-008-02: Late scan đến sau reassign
- EC-008-03: Cùng kiện quét ở hai hub
- EC-008-04: Out of order scan
- EC-008-05: Delay/backlog threshold
- EC-008-06: Reassign không tự bàn giao custody

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-008-01 — happy path / REQ-008-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 005, 007 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Dispatcher lập hub/trip/leg/manifest → kiểm Shipment sẵn sàng → assign shipper active → operator scan handover/in/out hub → custody được ghi riêng → giám sát backlog và reassign có reason.
- **Then** Hub quét một kiện thuộc manifest hợp lệ, custody chuyển đúng bên nhận; dispatcher reassign không tạo 2 active assignments.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-008-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Scan trùng, thiếu kiện, sai hub, giao nhầm, reassign đồng thời, shipper hết hiệu lực → không nhân đôi custody/assignment.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-008-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-008-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-008-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `005`, `007`.
- **Ngoài phạm vi:** Advanced dynamic vehicle rebalancing/automatic carrier assignment outside P0.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Chặng nào nhận/giao trách nhiệm, hub holding threshold, manifest lifecycle, scan format and custody handover confirmation.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
