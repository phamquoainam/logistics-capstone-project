# Feature 010: Gợi ý tuyến, phê duyệt tuyến và GPS

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `routing`  
**Actors:** Dispatcher, Shipper  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Gợi ý tuyến, phê duyệt tuyến và GPS là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Dispatcher, Shipper**, tôi muốn thực hiện nghiệp vụ **Gợi ý tuyến, phê duyệt tuyến và GPS** để đạt: **Dispatcher tạo route hợp lệ cho 10 điểm, confirm; shipper thấy route đúng version và GPS hiển thị độ mới rõ ràng.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Dispatcher lấy tập nhiệm vụ active + điểm xuất phát + map matrix → tạo Route Plan DRAFT → xem/điều chỉnh → confirm sau khi revalidate → shipper bắt đầu → ghi GPS trong phiên và hiển thị vị trí có freshness.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-010-01 | Tuyến chỉ gồm nhiệm vụ đang hợp lệ, ràng buộc capacity/time windows/stop count được kiểm trước khi áp dụng. | FR-D03 FR-D04 FR-S03 FR-D05 |
| REQ-010-02 | Routing không tự thay assignment hay trạng thái Shipment; dispatcher confirm route version và có history. | FR-D03 FR-D04 FR-S03 FR-D05 |
| REQ-010-03 | GPS có recordedAt, receivedAt, accuracy, source, sessionId; chỉ ghi trong phiên và role hợp lệ. | FR-D03 FR-D04 FR-S03 FR-D05 |
| REQ-010-04 | Mẫu vị trí stale/out-of-order không được trình bày là latest; ngừng nhận khi hết phiên/quyền. | FR-D03 FR-D04 FR-S03 FR-D05 |
| REQ-010-05 | So sánh baseline vs candidate trên cùng matrix/ràng buộc; infeasible trả nguyên nhân. | FR-D03 FR-D04 FR-S03 FR-D05 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-D03 | Tạo tuyến dự thảo từ nhiệm vụ, mạng đường và ràng buộc vận hành. | D05 || FR-D04 | Xem, chỉnh, kiểm tra, xác nhận tuyến và lưu phiên bản. | D07 || FR-S03 | Xem/thực hiện tuyến đã xác nhận, bật/tắt chia sẻ vị trí trong phiên và cập nhật tiến độ. | S06, S07 || FR-D05 | Giám sát chuyến, vị trí có độ mới, ngoại lệ và cách xử lý. | D06 |

### Business invariants / NFR cần giữ
- **BR:** BR-20, BR-21.
- **NFR:** NFR-P03, NFR-P04, NFR-S05.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-010-01: Hai route cùng confirm cho shipper
- EC-010-02: Re-plan sau khi stop completed
- EC-010-03: GPS drift, accuracy tệ
- EC-010-04: Device timestamp in future
- EC-010-05: No tiles/matrix but cached route
- EC-010-06: Native background permission denied

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-010-01 — happy path / REQ-010-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 008 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Dispatcher lấy tập nhiệm vụ active + điểm xuất phát + map matrix → tạo Route Plan DRAFT → xem/điều chỉnh → confirm sau khi revalidate → shipper bắt đầu → ghi GPS trong phiên và hiển thị vị trí có freshness.
- **Then** Dispatcher tạo route hợp lệ cho 10 điểm, confirm; shipper thấy route đúng version và GPS hiển thị độ mới rõ ràng.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-010-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Provider matrix lỗi, vô nghiệm, stale assignment lúc confirm, GPS timestamp sai/out-of-order, session tắt → từ chối hoặc fallback được đánh dấu.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-010-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-010-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-010-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `008`.
- **Ngoài phạm vi:** Commercial ETA guarantees, real-time traffic integration and advanced reroute P1.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Map tiles/geocoding/matrix provider, coverage, license and credentials; route baseline algorithm; optimization constraints; GPS stale/accuracy thresholds and location transport.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
