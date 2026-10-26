# Feature 015: Hàng đợi offline và đồng bộ an toàn trên mobile

**Status:** DRAFT_SPEC / NOT_IMPLEMENTED  
**Owners / Bounded Context:** `shipper-mobile, order, dispatch`  
**Actors:** Shipper  
**Priority:** P0  
**Source:** [REPORT] Chương 2 BR/FR/NFR; Chương 3 Use Case/State Machine; Chương 4 §4.2–4.3.1. API/UI cụ thể: [PROPOSED].

## 1. Bối cảnh / Problem statement
Hàng đợi offline và đồng bộ an toàn trên mobile là một lát cắt sản phẩm E-Logistics, triển khai sau/đồng thời các contract phụ thuộc. Người dùng cần giải quyết đúng nghiệp vụ đồng thời bảo vệ ownership, consistency và truy vết. Không được xây một CRUD demo bỏ qua trạng thái và tác động liên module.

## 2. User story / business outcome
Là **Shipper**, tôi muốn thực hiện nghiệp vụ **Hàng đợi offline và đồng bộ an toàn trên mobile** để đạt: **Tạo scan offline, kill app rồi mở lại còn pending; có mạng submit một lần; ACK hiển thị tác động server; replay không scan đôi.**.

## 3. Luồng người dùng và dữ liệu (happy path)
Shipper mất mạng → thực hiện tác vụ nằm trong allowlist offline → SQLite lưu durable command với operationId → app restart → reconnect → submit giữ ID → server kiểm trạng thái/quyền hiện tại → ACK hoặc CONFLICT → shipper xem và xử lý.

## 4. Yêu cầu có mã định danh
| Feature REQ | Hành vi cần xây dựng | Trace FR |
| --- | --- | --- |
| REQ-015-01 | Chỉ cho offline những thao tác có policy rõ, không tự coi thành công server; UI gắn `PENDING_SYNC`. | FR-S02 FR-S04 FR-S05 |
| REQ-015-02 | Queue item lưu operationId, actorId, assignment/version, type, canonical payload, recordedAt, retry state, attempts; sensitive payload bảo vệ/giới hạn. | FR-S02 FR-S04 FR-S05 |
| REQ-015-03 | Retry giữ nguyên operationId/key, bounded backoff; server dedupe và state guard trước khi commit. | FR-S02 FR-S04 FR-S05 |
| REQ-015-04 | Conflict khi assignment bị thay hay shipment hoàn tất bởi thiết bị khác; không tự bỏ queue item, không tự ép trạng thái. | FR-S02 FR-S04 FR-S05 |
| REQ-015-05 | Credential không lưu trong queue; logout/thu hồi account phải có chính sách bảo vệ/giải quyết dữ liệu tồn. | FR-S02 FR-S04 FR-S05 |


### Mapping FR gốc
| FR | Yêu cầu từ báo cáo | UC |
| --- | --- | --- |
| FR-S02 | Quét/nhập mã khi lấy, giao–nhận kiện và giao/hoàn. | S02–S05 || FR-S04 | Ghi kết quả giao, thất bại, giao lại hoặc hoàn; thành công có POD, thất bại có lý do. | S04, S05 || FR-S05 | Ghi COD thực thu, khai báo nộp và bằng chứng để Finance xác nhận. | S04, S08 |

### Business invariants / NFR cần giữ
- **BR:** BR-02, BR-04, BR-05, BR-09, BR-20.
- **NFR:** NFR-R03, NFR-U02, NFR-S01.
- Tham chiếu chi tiết tại `docs/domain/business-rules.md`, `docs/quality/non-functional-requirements.md`; không thay đổi nghĩa BR tại feature.

## 5. Lỗi, trường hợp biên và điều kiện thiếu
- EC-015-01: FIFO vs dependency ordering
- EC-015-02: POD file queued pending upload
- EC-015-03: Clock skew
- EC-015-04: Same operation from two devices
- EC-015-05: Retry long after TTL
- EC-015-06: Device storage full
- EC-015-07: Partial sync batch

- Từ chối quyền hoặc object cross-tenant trước khi tiết lộ dữ liệu. Input sai: xác định lỗi ở trường nào; partial commit chỉ được phép khi chính sách đã chốt.
- Mất mạng/timeout sau commit phải có đường `get status`/retry cùng idempotency key; không reset form và báo thành công giả.

## 6. Tiêu chí nghiệm thu kiểm chứng được

**AC-015-01 — happy path / REQ-015-01…**
- **Given** môi trường đã migration/seed hợp lệ, các phụ thuộc 002, 008, 009 đã sẵn sàng, người dùng có quyền tương ứng.
- **When** thực hiện luồng: Shipper mất mạng → thực hiện tác vụ nằm trong allowlist offline → SQLite lưu durable command với operationId → app restart → reconnect → submit giữ ID → server kiểm trạng thái/quyền hiện tại → ACK hoặc CONFLICT → shipper xem và xử lý.
- **Then** Tạo scan offline, kill app rồi mở lại còn pending; có mạng submit một lần; ACK hiển thị tác động server; replay không scan đôi.
- **And** các bảng authoritative/audit/event liên quan thống nhất, không sai quyền đối tượng.

**AC-015-02 — rollback/negative path**
- **Given** người dùng gửi yêu cầu có trạng thái, quyền hoặc dữ liệu không hợp lệ.
- **When** Assignment reassigned trước sync, token expired, duplicate operation, insufficient media storage, upload POD dang dở → trạng thái yêu cầu xử lý chứ không xóa âm thầm.
- **Then** không có tác động nghiệp vụ trái BR; API trả lỗi có mã ổn định; UI không hiển thị thành công giả và giữ dữ liệu hợp lý.

**AC-015-03 — replay/concurrency**
- **Given** cùng operation có request key và cùng input hợp lệ.
- **When** gửi lại, gửi đồng thời hoặc server mất kết nối sau commit.
- **Then** chỉ có một tác động, cùng key+payload khác trả xung đột; nghiệp vụ có mã mới và đủ điều kiện không bị chặn nhầm.

**AC-015-04 — authorization**
- **Given** người dùng ngoài role/merchant/warehouse/hub/assignment scope.
- **When** thử đọc hoặc cập nhật object.
- **Then** server từ chối, không rò thông tin object và có log/audit phù hợp.

**AC-015-05 — edge cases**
- **Given** các kịch bản biên ở mục 5.
- **When** thực thi bằng unit/integration/E2E/fault test phù hợp.
- **Then** tất cả kết quả khớp với guard hoặc fallback được phê duyệt và lưu evidence.

## 7. Phạm vi và phụ thuộc
- **Cần có trước / phải có hợp đồng mock được:** `002`, `008`, `009`.
- **Ngoài phạm vi:** Full offline mapping/geocoding, unattended background success guarantee.
- **Mốc P0:** chỉ complete khi code, migration, contract và kiểm thử thực hiện, không chỉ dựng giao diện.

## 8. Câu hỏi còn mở và giả định
[BLOCKING] Danh sách offline action; sync conflict UX, queue TTL/limits, encryption/storage, POD file upload strategy.

**Giả định:** bảng master/demo seeded, người dùng và phạm vi quyền hợp lệ, các phụ thuộc ngoài dùng Sandbox/mô phỏng, các ngưỡng được đặt tại cấu hình sau khi chốt. Mọi assumption phải được đánh dấu `PROPOSED` hoặc `BLOCKING` trong quyết định chung.

## V3 reconciliation overlay — PROPOSED, 2026-10-26

ID/nội dung/checkbox cũ giữ nguyên lịch sử; candidate không DONE/REPORT/accepted decision. Target: [L029 courier-offline-sync](../../specs/planning/v3/L029-courier-offline-sync/spec.md). [Crosswalk](../../docs/reconciliation/v3/legacy-crosswalk.csv) SPLIT/EXTEND/PRESERVE là scope relation, không automatic equivalence.

Actors/flow/guard bổ sung theo REQ-V3-BC tại linked specs; P1/exclusions cũ không loại CORE FULL.5 DEFER và CORE negative cần direct API denial, không xóa AC cũ. Outcomes: Offline Courier durable queue/revalidation — Offline enqueue→kill/restart→reconnect→ACK/conflict; double sync không double POD/COD.

[Delivery/DoR/DoD](../../docs/reconciliation/v3/delivery-plan.md). Research016 chỉ RQ/L030, không Returns. T-003-01 còn mở cho authority/address/enrollment; [first packet](../planning/v3/first-business-slice/spec.md).
