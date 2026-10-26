# Feature 014: Import Excel tạo đơn nháp có preview và báo lỗi từng dòng

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `order`  
**Actors:** Merchant  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Import Excel tạo đơn nháp có preview và báo lỗi từng dòng là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Merchant**, tôi muốn thực hiện nghiệp vụ **Import Excel tạo đơn nháp có preview và báo lỗi từng dòng** để đạt: **File hợp lệ 3 dòng, một dòng lỗi: preview 2 valid/1 invalid, confirm tạo đúng các draft theo policy chốt; retry không sinh draft trùng.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Merchant upload file mẫu Excel → backend validate template, loại tệp, owner, dữ liệu từng dòng → preview valid/error → xác nhận import → tạo draft với idempotency và mã đối tác unique → trả summary để sửa dòng lỗi.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-014-01 | Preview không tạo shipment hoặc thu cước; import xác nhận chỉ tạo draft hợp lệ. | FR-M04 |
| REQ-014-02 | Mỗi row có số dòng, mã tham chiếu, lỗi cấu trúc/ngữ nghĩa rõ; chủ sở hữu lấy từ phiên, không từ Excel. | FR-M04 |
| REQ-014-03 | File/header/size/count limits config; không execute formula/macro; không log PII từ Excel. | FR-M04 |
| REQ-014-04 | Import batch có job ID + row fingerprint/merchant reference; replay không tạo trùng; semantics partial success vs atomic import phải chốt. | FR-M04 |
| REQ-014-05 | Quy trình xác nhận phí/WMS vẫn qua feature 005, không auto-confirm đơn import. | FR-M04 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-M04 | Tạo đơn nháp đơn lẻ hoặc import Excel; xem trước, báo lỗi từng dòng và chống tạo trùng. | M04, M10 |

### Business invariants / NFR cần giữ
- **BR:** BR-02, BR-19, BR-01.
- **NFR:** NFR-C02, NFR-S03, NFR-U02.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-014-01: Empty template
- EC-014-02: 10k rows resource exhaustion
- EC-014-03: UTF-8 Vietnamese text
- EC-014-04: Same file repeat
- EC-014-05: Row reordered
- EC-014-06: Mixed owner identifiers
- EC-014-07: Repeated external reference with different payload

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-014-01 — happy path / REQ-014-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 002, 003, 005 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Merchant upload file mẫu Excel → backend validate template, loại tệp, owner, dữ liệu từng dòng → preview valid/error → xác nhận import → tạo draft với idempotency và mã đối tác unique → trả summary để sửa dòng lỗi.
- **Then** File hợp lệ 3 dòng, một dòng lỗi: preview 2 valid/1 invalid, confirm tạo đúng các draft theo policy chốt; retry không sinh draft trùng.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-014-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Excel sai template, formula injection, duplicate merchant reference trong cùng file hoặc DB, mất mạng khi import, role bị thu hồi → không tạo ngoài quyền.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-014-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-014-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-014-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `002`, `003`, `005`.
- **Ngoài phạm vi:** Import PDF/CSV/other providers ngoài phạm vi P0 nếu không xác nhận.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Excel template columns, max file/rows, partial success vs all-or-nothing, retry correction policy.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L023 bulk-shipment-import](../../specs/planning/v3/L023-bulk-shipment-import/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Import CSV/Excel với row outcomes — Mixed valid/invalid→preview→commit theo policy→sửa/retry; không auto-confirm/charge.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
