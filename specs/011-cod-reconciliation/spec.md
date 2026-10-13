# Feature 011: COD nộp tiền, xác nhận, đối soát và payout mô phỏng

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `finance`  
**Actors:** Shipper, Finance, Merchant (read)  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
COD nộp tiền, xác nhận, đối soát và payout mô phỏng là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Shipper, Finance, Merchant (read)**, tôi muốn thực hiện nghiệp vụ **COD nộp tiền, xác nhận, đối soát và payout mô phỏng** để đạt: **Finance xác nhận remittance đủ, close kỳ một lần; merchant nhìn kỳ đóng và payout mô phỏng khớp tổng; double-close không nhân đôi khoản.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Delivery tạo nghĩa vụ COD → shipper khai collected và lập remittance SUBMITTED → Finance kiểm chứng tiền thực nhận/phân bổ → phân loại CONFIRMED/DISCREPANCY → preview kỳ → recheck và CLOSE → payout mô phỏng hoặc adjustment riêng.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-011-01 | Phân biệt COLLECTED/REMITTED/RECONCILED, không coi shipper submit là xác nhận đã nộp. | FR-F02 FR-F03 FR-F04 FR-M08 FR-S05 |
| REQ-011-02 | Remittance có allocation per COD item; tổng phân bổ ≤ remittance confirmed amount; không vượt COD liability. | FR-F02 FR-F03 FR-F04 FR-M08 FR-S05 |
| REQ-011-03 | Finance là người xác nhận, khác shipper khai báo; reason + evidence bắt buộc khi discrepancy/reject. | FR-F02 FR-F03 FR-F04 FR-M08 FR-S05 |
| REQ-011-04 | Chỉ COD delivered+collected+fully confirmed remitted+no dispute+eligible cutoff mới đóng kỳ; mỗi COD chỉ một closed period. | FR-F02 FR-F03 FR-F04 FR-M08 FR-S05 |
| REQ-011-05 | Closed period/items/totals immutable; adjustment và simulated payout có source ref, quyền, idempotency. | FR-F02 FR-F03 FR-F04 FR-M08 FR-S05 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-F02 | Đối chiếu, xác nhận COD thực nhận, phân bổ và xử lý chênh lệch/tranh chấp. | F01 || FR-F03 | Lập, xem trước, kiểm tra và đóng kỳ; kiểm tra lại điều kiện khi chốt. | F02, F05 || FR-F04 | Chi trả mô phỏng, hoàn cước và điều chỉnh bằng nghiệp vụ riêng có tham chiếu, quyền và lý do. | F03, F07, F08 || FR-M08 | Tra cứu COD, bảng kê/chi trả; truy cập thông báo và dashboard theo quyền. | M09, M12, C01 || FR-S05 | Ghi COD thực thu, khai báo nộp và bằng chứng để Finance xác nhận. | S04, S08 |

### Business invariants / NFR cần giữ
- **BR:** BR-15, BR-16, BR-17, BR-18.
- **NFR:** NFR-C01, NFR-C02, NFR-S01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-011-01: Partial COD collection
- EC-011-02: One remittance allocated to many COD
- EC-011-03: One COD receives multiple remittances
- EC-011-04: Finance vs shipper separation
- EC-011-05: Race two closing periods
- EC-011-06: Late event after cutoff
- EC-011-07: Reverse charge via new adjustment

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-011-01 — happy path / REQ-011-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 006, 009 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Delivery tạo nghĩa vụ COD → shipper khai collected và lập remittance SUBMITTED → Finance kiểm chứng tiền thực nhận/phân bổ → phân loại CONFIRMED/DISCREPANCY → preview kỳ → recheck và CLOSE → payout mô phỏng hoặc adjustment riêng.
- **Then** Finance xác nhận remittance đủ, close kỳ một lần; merchant nhìn kỳ đóng và payout mô phỏng khớp tổng; double-close không nhân đôi khoản.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-011-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Submit twice, partial shortage, merchant cross-tenant, COD dispute, late remittance, duplicate close/payout, change after CLOSED → tách trạng thái rõ và không sửa kỳ.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-011-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-011-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-011-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `006`, `009`.
- **Ngoài phạm vi:** Real transfer to bank account and settlement of real funds.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Cutoff timezone and schedule, dispute policy, partial remittance rules, simulated payout deduction formula, adjustment authority and accounting examples.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
