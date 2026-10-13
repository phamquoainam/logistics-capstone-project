# Feature 004: Pricing Engine và bảng giá theo phiên bản

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `pricing`  
**Actors:** Guest, Merchant, Price Admin  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Pricing Engine và bảng giá theo phiên bản là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Guest, Merchant, Price Admin**, tôi muốn thực hiện nghiệp vụ **Pricing Engine và bảng giá theo phiên bản** để đạt: **Với cùng input và tariff version, tính phí và chi tiết nhất quán; quote persisted; publish v2 không sửa quote v1 đã chốt.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Admin tạo tariff DRAFT → validate → publish version không sửa → merchant gửi vùng/dịch vụ/kích thước/khối lượng → quote breakdown+expiry → Order kiểm tra lại quote khi confirm.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-004-01 | Khối lượng tính phí là max(real, volumetric), unit và rounding theo version; mọi input bắt buộc được validate. | FR-M03 FR-P01 FR-G01 |
| REQ-004-02 | Giá gồm zone/service/tier/surcharge/discount với thứ tự xác định; không đoán giá khi thiếu rule. | FR-M03 FR-P01 FR-G01 |
| REQ-004-03 | Mỗi quote lưu version, canonical input, breakdown, currency, total, issuedAt, expiresAt. | FR-M03 FR-P01 FR-G01 |
| REQ-004-04 | Published tariff/version không update in-place; bảng giá mới tạo version mới. | FR-M03 FR-P01 FR-G01 |
| REQ-004-05 | Cache theo key gồm tariffVersion và canonical price dimensions; Redis fail fallback DB không sai cước. | FR-M03 FR-P01 FR-G01 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-M03 | Tạo báo giá có phiên bản, đầu vào, chi tiết phí và thời hạn. | M03 || FR-P01 | Tạo, kiểm tra, công bố/ngừng phiên bản giá; giữ lịch sử và phiên bản đã dùng. | F04 || FR-G01 | Ước tính cước công khai từ đầu vào hợp lệ, không tạo vận đơn. | G01 |

### Business invariants / NFR cần giữ
- **BR:** BR-10, BR-11, BR-12, BR-19.
- **NFR:** NFR-P02, NFR-C03.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-004-01: Đúng thời điểm effectiveFrom/effectiveTo
- EC-004-02: Sai quy tắc làm tròn
- EC-004-03: Cache cũ khi publish v2
- EC-004-04: Concurrent publish
- EC-004-05: Replay với cùng version
- EC-004-06: Payment exact amount

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-004-01 — happy path / REQ-004-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 003 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Admin tạo tariff DRAFT → validate → publish version không sửa → merchant gửi vùng/dịch vụ/kích thước/khối lượng → quote breakdown+expiry → Order kiểm tra lại quote khi confirm.
- **Then** Với cùng input và tariff version, tính phí và chi tiết nhất quán; quote persisted; publish v2 không sửa quote v1 đã chốt.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-004-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Thiếu zone/tier, kích thước âm, quote expired, tariff inactive, inconsistent breakdown hoặc Redis không hoạt động → lỗi xác định/fallback an toàn.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-004-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-004-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-004-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `003`.
- **Ngoài phạm vi:** AI demand pricing, dynamic pricing theo ML, thay đổi báo giá quá khứ.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Đơn vị kg/g/cm, divisor cân nặng quy đổi, vùng giá, thứ tự phụ phí/chiết khấu, rounding, thời hạn quote, tariff sample.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
