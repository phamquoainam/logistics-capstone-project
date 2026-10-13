# Feature 002: Xác thực và phân quyền theo phạm vi

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `identity`  
**Actors:** Guest, Merchant, Operators, Dispatcher, Shipper, Finance, Admin  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Xác thực và phân quyền theo phạm vi là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Guest, Merchant, Operators, Dispatcher, Shipper, Finance, Admin**, tôi muốn thực hiện nghiệp vụ **Xác thực và phân quyền theo phạm vi** để đạt: **Merchant A đăng nhập, chỉ đọc shipment thuộc A; Admin cấp scope kho X để operator thao tác X, không thao tác Y.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Admin tạo/cấp account và scope → người dùng đăng nhập → client nhận phiên → request đọc/ghi → server kiểm tra role, owner/scope và trạng thái → logout/thu hồi.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-002-01 | Account có trạng thái và role/scope, cùng user có nhiều role nhưng quyền phải được xét trên tài nguyên. | FR-A01 FR-A02 FR-AD01 |
| REQ-002-02 | Mọi request ngoài public yêu cầu phiên hợp lệ và quyền chức năng; mọi thao tác đọc/ghi kiểm tra object ownership ở server. | FR-A01 FR-A02 FR-AD01 |
| REQ-002-03 | Đăng nhập giới hạn brute-force; password hashed; session/token không log, không lưu không an toàn trên mobile. | FR-A01 FR-A02 FR-AD01 |
| REQ-002-04 | Tách quyền Admin và Finance; việc cấp/thu hồi quyền có audit. | FR-A01 FR-A02 FR-AD01 |
| REQ-002-05 | Thu hồi phiên/role có hiệu lực khi các nghiệp vụ nhạy cảm được xác nhận. | FR-A01 FR-A02 FR-AD01 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-A01 | Đăng nhập, đăng xuất, quản lý phiên và khôi phục tài khoản. | A01–A03 || FR-A02 | Kiểm tra quyền theo vai trò, tổ chức, kho/hub và đối tượng tại thời điểm thao tác. | AD02; áp dụng trong các UC không công khai || FR-AD01 | Quản lý tài khoản, trạng thái, vai trò và phạm vi quyền. | AD01, AD02 |

### Business invariants / NFR cần giữ
- **BR:** Áp dụng nguyên tắc correctness, audit, ownership trong docs/principles.md.
- **NFR:** NFR-S01, NFR-S02, NFR-S04.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-002-01: Role bị thu hồi giữa form và submit
- EC-002-02: Một account có hai merchant scope
- EC-002-03: Tài khoản bị khóa khi đang có phiên
- EC-002-04: Reset credentials/invalid refresh
- EC-002-05: Parallel requests với phiên thu hồi

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-002-01 — happy path / REQ-002-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Admin tạo/cấp account và scope → người dùng đăng nhập → client nhận phiên → request đọc/ghi → server kiểm tra role, owner/scope và trạng thái → logout/thu hồi.
- **Then** Merchant A đăng nhập, chỉ đọc shipment thuộc A; Admin cấp scope kho X để operator thao tác X, không thao tác Y.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-002-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** User không đăng nhập/phiên hết hạn bị từ chối; merchant A không đọc B; shipper không thể tự nâng quyền; admin không tự duyệt nộp COD.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-002-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-002-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-002-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`.
- **Ngoài phạm vi:** SSO/OIDC enterprise và multi-tenant billing ngoài P0.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Session cookie hay access+refresh token, vòng đời token, reset password qua mail sandbox, đăng ký account công khai hay chỉ admin cấp?

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.
