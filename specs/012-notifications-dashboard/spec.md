# Feature 012: Thông báo nội bộ và dashboard theo vai trò

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `notification, order/finance/read projections`  
**Actors:** Merchant, Dispatcher, Finance, Admin, Shipper  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Thông báo nội bộ và dashboard theo vai trò là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Merchant, Dispatcher, Finance, Admin, Shipper**, tôi muốn thực hiện nghiệp vụ **Thông báo nội bộ và dashboard theo vai trò** để đạt: **ShipmentConfirmed phát đúng một thông báo cho recipient phù hợp; user chỉ thấy bản của mình; dashboard tổng khớp DB projection.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Transaction nghiệp vụ commit cùng outbox → publish → notification inbox dedupe → người nhận mở bảng thông báo/đánh dấu đã đọc; dashboard query dựa trên data đã commit, hiển thị thời điểm cập nhật.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-012-01 | Chỉ tạo notification sau source transaction commit; mỗi (sourceEventId, recipientId, purpose) chỉ một bản. | FR-C01 FR-C02 FR-M08 |
| REQ-012-02 | Notification không tự quyết định trạng thái vận đơn/tài chính; dữ liệu nhạy cảm được redact. | FR-C01 FR-C02 FR-M08 |
| REQ-012-03 | Người dùng chỉ đọc notification của mình; `markAsRead` chỉ cập nhật target thuộc account. | FR-C01 FR-C02 FR-M08 |
| REQ-012-04 | Dashboard filter theo tenant/role/warehouse/hub; định nghĩa KPI và thời điểm cập nhật được công bố. | FR-C01 FR-C02 FR-M08 |
| REQ-012-05 | RabbitMQ unavailable vẫn giữ outbox; retry bounded + failed job readable theo quyền. | FR-C01 FR-C02 FR-M08 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-C01 | Tạo/gửi thông báo từ nghiệp vụ nguồn; người nhận xem/đánh dấu đọc; kiểm tra quyền và chống trùng theo nguồn, người nhận, mục đích. | C01; hành vi tự động trong các luồng nguồn || FR-C02 | Dashboard theo vai trò/phạm vi; công bố định nghĩa chỉ số và thời điểm cập nhật. | M12 || FR-M08 | Tra cứu COD, bảng kê/chi trả; truy cập thông báo và dashboard theo quyền. | M09, M12, C01 |

### Business invariants / NFR cần giữ
- **BR:** BR-01, BR-02.
- **NFR:** NFR-R01, NFR-R02, NFR-S01, NFR-O01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-012-01: Notification read repeatedly
- EC-012-02: Same event different recipient
- EC-012-03: Message delivered twice
- EC-012-04: Consumer crash after DB commit
- EC-012-05: Dashboard lag
- EC-012-06: PII in payload
- EC-012-07: Pagination cursor

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-012-01 — happy path / REQ-012-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 001, 002, 005, 008, 011 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Transaction nghiệp vụ commit cùng outbox → publish → notification inbox dedupe → người nhận mở bảng thông báo/đánh dấu đã đọc; dashboard query dựa trên data đã commit, hiển thị thời điểm cập nhật.
- **Then** ShipmentConfirmed phát đúng một thông báo cho recipient phù hợp; user chỉ thấy bản của mình; dashboard tổng khớp DB projection.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-012-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Event replay/out-of-order, deleted user, recipient access revoked, MQ down, stale projection và cross-tenant dashboard → không lộ dữ liệu/sai số vô giải thích.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-012-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-012-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-012-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `001`, `002`, `005`, `008`, `011`.
- **Ngoài phạm vi:** Email/SMS/push và phân tích predictive dashboards thuộc P1.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Danh sách thông báo P0 thiết yếu và người nhận từng loại; KPI dashboard, lịch sync và retention.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L009 tracking-event-core-public-lookup](../../specs/planning/v3/L009-tracking-event-core-public-lookup/spec.md); [L020 customer-support-tickets-sla](../../specs/planning/v3/L020-customer-support-tickets-sla/spec.md); [L028 notifications-analytics-sla](../../specs/planning/v3/L028-notifications-analytics-sla/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Tracking an toàn và có nguồn — Source commit→timeline; đoán code/UUID không lộ PII; late/duplicate không sửa lịch sử; Ticket có nguồn/scope/SLA — Tiếp nhận→verify→phân công→phối hợp/escalate→phản hồi; không sửa POD/COD; Notifications/KPI/SLA từ committed facts — Commit→dedup notification→read; late/missing event đối chiếu; không success inflation.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
