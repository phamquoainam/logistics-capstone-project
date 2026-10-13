# Feature 006: Ví, ledger, thu cước và VNPay Sandbox

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `finance`  
**Actors:** Merchant, Finance  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Ví, ledger, thu cước và VNPay Sandbox là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Merchant, Finance**, tôi muốn thực hiện nghiệp vụ **Ví, ledger, thu cước và VNPay Sandbox** để đạt: **Topup Sandbox verified tạo một payment success, một credit operation; double callback hoặc retry không tăng tiền; hai concurrent charges không đưa balance âm.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Merchant xem wallet → tạo topup intent → redirect Sandbox → callback/query verified → Finance ghi credit exactly-once; confirm shipment gọi Finance fee charge nội bộ → merchant xem ledger; timeout = UNKNOWN.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-006-01 | Wallet available không âm dưới concurrent debit; tiền biểu diễn chính xác, ownerId từ trusted context. | FR-M07 FR-F01 FR-F04 |
| REQ-006-02 | Ledger postings append-only, bút toán cân, balance update và ledger cùng local transaction. | FR-M07 FR-F01 FR-F04 |
| REQ-006-03 | Payment intent có CREATED/PENDING/UNKNOWN/SUCCEEDED/FAILED và external reference; chỉ VERIFIED SUCCEEDED được credit. | FR-M07 FR-F01 FR-F04 |
| REQ-006-04 | Callback xác minh chữ ký, amount, reference, state và replay; browser redirect không tự cấp tiền. | FR-M07 FR-F01 FR-F04 |
| REQ-006-05 | Refund/adjustment tạo giao dịch và postings mới link original; quyền Finance và reason bắt buộc. | FR-M07 FR-F01 FR-F04 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-M07 | Tra cứu số dư/lịch sử và nạp tiền qua VNPay Sandbox. | M08, M11 || FR-F01 | Tra cứu thanh toán/giao dịch gốc trong phạm vi quyền để xác minh và xử lý kết quả chưa xác định. | F06–F08 || FR-F04 | Chi trả mô phỏng, hoàn cước và điều chỉnh bằng nghiệp vụ riêng có tham chiếu, quyền và lý do. | F03, F07, F08 |

### Business invariants / NFR cần giữ
- **BR:** BR-13, BR-14, BR-15.
- **NFR:** NFR-C01, NFR-C02, NFR-R01, NFR-S03.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-006-01: Payment callback before redirect
- EC-006-02: Provider timeout after payment success
- EC-006-03: Repeated topup reference
- EC-006-04: Decimal currency overflow
- EC-006-05: Different payload same key
- EC-006-06: Refund twice

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-006-01 — happy path / REQ-006-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Merchant xem wallet → tạo topup intent → redirect Sandbox → callback/query verified → Finance ghi credit exactly-once; confirm shipment gọi Finance fee charge nội bộ → merchant xem ledger; timeout = UNKNOWN.
- **Then** Topup Sandbox verified tạo một payment success, một credit operation; double callback hoặc retry không tăng tiền; hai concurrent charges không đưa balance âm.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-006-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Fake signature, amount mismatch, race credit/debit, timeout, duplicate callback, unknown outcome, underfunded fee → không ghi sai/đòi tra soát.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-006-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-006-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-006-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`.
- **Ngoài phạm vi:** Real money transfer/payment hoặc payout thật.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Currency exact representation policy, accounting account mapping, VNPay Sandbox parameters/signature contract, reimbursement/fee policy.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
