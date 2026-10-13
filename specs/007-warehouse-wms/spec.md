# Feature 007: WMS: nhận hàng, tồn, giữ, pick-pack-xuất, hoàn

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `warehouse`  
**Actors:** Warehouse Operator, Merchant (read), Workflow  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
WMS: nhận hàng, tồn, giữ, pick-pack-xuất, hoàn là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Warehouse Operator, Merchant (read), Workflow**, tôi muốn thực hiện nghiệp vụ **WMS: nhận hàng, tồn, giữ, pick-pack-xuất, hoàn** để đạt: **Nhận 10 món đủ chuẩn, giữ 7 theo shipment; available còn 3; pick 5, release 2 có chứng từ, balance/movement/reservation đối chiếu được.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Warehouse tạo SKU/location → tiếp nhận ASN → kiểm/QC → putaway → stock movement → reservation theo shipment → picking/packing/outbound → chứng từ và đối chiếu → return/adjust nếu cần.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-007-01 | Stock theo owner, SKU, warehouse, location và condition; available = eligible − remaining holds ≥ 0. | FR-W01 FR-W02 FR-W03 FR-W06 FR-W07 FR-W08 FR-M06 |
| REQ-007-02 | Reservation consumed/released/remaining với invariant reserved = consumed + released + remaining. | FR-W01 FR-W02 FR-W03 FR-W06 FR-W07 FR-W08 FR-M06 |
| REQ-007-03 | Mọi stock move từ chứng từ, append-only history; transfer bảo toàn tổng; kiểm kê không trực tiếp ghi balance. | FR-W01 FR-W02 FR-W03 FR-W06 FR-W07 FR-W08 FR-M06 |
| REQ-007-04 | Pick/outbound không vượt reservation remaining; hủy chỉ giải phóng unused; hàng xuất phải qua return receipt. | FR-W01 FR-W02 FR-W03 FR-W06 FR-W07 FR-W08 FR-M06 |
| REQ-007-05 | Nhân viên chỉ thao tác kho được cấp; duplicate scan/command không tạo movement lặp. | FR-W01 FR-W02 FR-W03 FR-W06 FR-W07 FR-W08 FR-M06 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-W01 | Quản lý chủ hàng/SKU, kho, khu vực và vị trí theo phạm vi quyền. | W01 || FR-W02 | Nhận hàng theo ASN/phiếu, kiểm đếm, QC, putaway và ghi lịch sử biến động; điều chỉnh tồn bằng chứng từ có quyền và lý do. | W02, W09 || FR-W03 | Giữ/giải phóng, lấy–đóng gói–xuất theo vận đơn; đối chiếu phân bổ và kiện. | W04, W05 || FR-W06 | Chuyển vị trí hàng có nguồn–đích, số lượng và chứng từ; bảo toàn lượng và xử lý phần đã giữ theo chính sách. | W06 || FR-W07 | Ghi phạm vi kiểm kê, số tham chiếu tại thời điểm đối chiếu, số đếm và chênh lệch; điều chỉnh là bước có quyền riêng. | W07, W09 || FR-W08 | Tiếp nhận hàng hoàn liên kết vận đơn/kiện gốc; kiểm tra để nhập lại đủ điều kiện hoặc cách ly, chống nhập lặp. | W08 || FR-M06 | Tra cứu tồn, lượng giữ và tiến độ fulfillment thuộc merchant. | W03, M07 |

### Business invariants / NFR cần giữ
- **BR:** BR-06, BR-07, BR-08.
- **NFR:** NFR-C01, NFR-C02, NFR-S01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-007-01: Một SKU nhiều location
- EC-007-02: Release phần giữ chưa dùng
- EC-007-03: Concurrent reserve on same stock
- EC-007-04: Duplicate ASN/scan
- EC-007-05: Transfer source/destination conflict
- EC-007-06: Return QC quarantine
- EC-007-07: Stocktaking variance approval

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-007-01 — happy path / REQ-007-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 003 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Warehouse tạo SKU/location → tiếp nhận ASN → kiểm/QC → putaway → stock movement → reservation theo shipment → picking/packing/outbound → chứng từ và đối chiếu → return/adjust nếu cần.
- **Then** Nhận 10 món đủ chuẩn, giữ 7 theo shipment; available còn 3; pick 5, release 2 có chứng từ, balance/movement/reservation đối chiếu được.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-007-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Giữ đồng thời vượt 10, pick 8 khi chỉ giữ 7, nhập lặp, QC hàng cách ly, cross-merchant SKU, cancel sau outbound → từ chối/luồng return.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-007-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-007-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-007-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `003`.
- **Ngoài phạm vi:** WMS batch optimization/slotting advanced beyond P0.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] SKU measurement units, QC eligibility, warehouse picking statuses, scan label format, fulfillment vs direct pickup policy.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
