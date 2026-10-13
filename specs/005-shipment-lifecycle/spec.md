# Feature 005: Tạo, xác nhận, hủy và tracking vận đơn

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `order, workflow`  
**Actors:** Merchant, Guest, Dispatch, Warehouse, Finance  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Tạo, xác nhận, hủy và tracking vận đơn là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Merchant, Guest, Dispatch, Warehouse, Finance**, tôi muốn thực hiện nghiệp vụ **Tạo, xác nhận, hủy và tracking vận đơn** để đạt: **Merchant xác nhận đủ tiền và đủ tồn → shipment CONFIRMED, fee charged một lần, reservation đúng và quote snapshot bất biến.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Merchant tạo DRAFT → chọn pickup/delivery/address → tính quote → submit confirm → workflow revalidate quote + debit fee + reserve WMS nếu có + status CONFIRMED → tracking read; merchant cancel trước mốc → release/refund theo policy.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-005-01 | DRAFT tạo được từ input hợp lệ và merchantId trusted; mã shipment ref unique theo owner; bắt buộc Idempotency-Key. | FR-M04 FR-M05 FR-M09 FR-G02 |
| REQ-005-02 | Xác nhận dùng quote hợp lệ, address confirmed; WMS reserve nếu có, Finance fee debit, Order confirm + snapshot trong cùng DB transaction. | FR-M04 FR-M05 FR-M09 FR-G02 |
| REQ-005-03 | State guard theo bảng `docs/domain/state-machines.md`; không cập nhật lịch sử tracking đã ghi. | FR-M04 FR-M05 FR-M09 FR-G02 |
| REQ-005-04 | Cancel chỉ trước mốc picked-up trực tiếp hoặc WMS picking; release unused reserved; refund theo policy bằng nghiệp vụ riêng, không xóa ledger. | FR-M04 FR-M05 FR-M09 FR-G02 |
| REQ-005-05 | Tracking công khai chỉ tối thiểu, có rate limit; merchant đọc toàn bộ đơn chỉ trong merchant scope. | FR-M04 FR-M05 FR-M09 FR-G02 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-M04 | Tạo đơn nháp đơn lẻ hoặc import Excel; xem trước, báo lỗi từng dòng và chống tạo trùng. | M04, M10 || FR-M05 | Theo dõi và hủy vận đơn theo điều kiện; tra cứu kết quả hủy/hoàn liên quan. | M06, M07 || FR-M09 | Xác nhận vận đơn khi báo giá hợp lệ, thu cước thành công và giữ đủ hàng WMS nếu áp dụng; có nhánh thiếu tiền/tồn, yêu cầu trùng và chưa xác định. | M05, W04 || FR-G02 | Tra cứu hành trình công khai, giới hạn dữ liệu được phép. | G02 |

### Business invariants / NFR cần giữ
- **BR:** BR-01, BR-02, BR-03, BR-05, BR-12, BR-13, BR-19.
- **NFR:** NFR-C01, NFR-C02, NFR-S01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-005-01: Direct pickup vs WMS fulfillment
- EC-005-02: Hai concurrent confirmation same shipment
- EC-005-03: Idempotency same key/different payload
- EC-005-04: Cancel trước/đúng mốc
- EC-005-05: Snapshot sau publish giá mới
- EC-005-06: Unknown external provider result

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-005-01 — happy path / REQ-005-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 003, 004, 006, 007 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Merchant tạo DRAFT → chọn pickup/delivery/address → tính quote → submit confirm → workflow revalidate quote + debit fee + reserve WMS nếu có + status CONFIRMED → tracking read; merchant cancel trước mốc → release/refund theo policy.
- **Then** Merchant xác nhận đủ tiền và đủ tồn → shipment CONFIRMED, fee charged một lần, reservation đúng và quote snapshot bất biến.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-005-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Thiếu tiền, hết tồn, quote expired, outdated address, double confirm, mất kết nối sau commit, hủy sau picking → reject/pending mà không commit dang dở.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-005-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-005-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-005-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `003`, `004`, `006`, `007`.
- **Ngoài phạm vi:** Đổi danh mục quá khứ, xóa tracking/ledger, thay đổi giá sau confirm, thao tác ngầm vượt cancellation cutoff.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Chính sách fee refund khi hủy, allowed cancel state chính xác theo flow; shipment code format; các bắt buộc tối thiểu khi tạo draft.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
