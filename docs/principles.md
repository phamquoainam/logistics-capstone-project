# Engineering & Product Principles

**Nguồn [REPORT]:** Phần A §2, §5; Chương 2 §2.4–2.7; Chương 4 §4.1–4.3.1. Các quy ước CI/API bên dưới là [PROPOSED] nếu báo cáo chưa ấn định.

1. **Tính đúng đắn trước hiệu năng:** từ chối/tạm hoãn khi không bảo vệ được bất biến; không báo thành công giả.
2. **Authorization ở server, theo đối tượng:** role + `merchantId`/`warehouseId`/`hubId`/`assignmentId` + trạng thái hiện hành; Admin không mặc nhiên có quyền Finance.
3. **Một module là chủ dữ liệu:** chỉ sở hữu module được ghi DML nghiệp vụ; giao tiếp qua public command/query/event, không import `internal` module khác.
4. **Transaction cục bộ bảo vệ nguyên tử:** Order confirmation/cancellation và giao có COD được phối hợp qua workflow trong backend; Redis lock và queue không thay ACID + DB constraints.
5. **Idempotency theo nghiệp vụ:** khóa gồm scope + operation + actor/subject + request key; cùng key/cùng payload trả kết quả cũ, cùng key/khác payload báo xung đột; áp dụng vào callback và scanner.
6. **Audit append-only cho thao tác nhạy cảm:** timestamp, actor, target, source, reason, correlation ID; sửa sai qua nghiệp vụ bù chứ không UPDATE ledger/movement/kỳ đóng.
7. **Giá có phiên bản:** báo giá lưu input, break-down, tariffVersion và expiry; quote được chốt thì snapshot không thay dù publish giá mới.
8. **Migration một nguồn:** Flyway quản lý DDL; Hibernate `validate` hoặc tương đương, không tự tạo/cập nhật schema ở runtime.
9. **Outbox/Inbox khi bất đồng bộ:** chỉ phát sau commit; retry hữu hạn, dead-letter, dedupe và runbook cho xử lý ngoại lệ.
10. **Offline minh bạch:** tác vụ nào offline được phải lưu cục bộ, có idempotency key, trạng thái chờ/đồng bộ/xung đột; quyền và trạng thái được kiểm tra lại tại server.
11. **Thời gian và tiền chính xác:** lưu `timestamptz` theo UTC và hiển thị theo timezone nghiệp vụ; tiền VND không dùng floating point; units và rounding cần quyết định được lưu lại.
12. **Dữ liệu vị trí và POD có giới hạn:** kiểm soát phạm vi quyền, chỉ ghi GPS khi phiên hợp lệ; tệp private, truy cập bằng quyền; không ghi token/mật khẩu hoặc dữ liệu nhạy cảm vào log.
13. **Contract-first:** HTTP contract tại `contracts/openapi/`; event contract ở `contracts/events/`; thay đổi public contract phải tương thích hoặc có migration/versioning plan.
14. **Test-first ở biên và bất biến:** mỗi BR/AC cần kiểm thử phù hợp (unit, integration, concurrency, E2E, fault); test không được chỉ mock hành vi tạo tiền/tồn.
15. **Spec trước code:** mọi user-visible change bắt đầu từ spec/design/tasks; cập nhật traceability và kiểm thử trước khi merge.
16. **Incremental delivery:** thực hiện một feature end-to-end thay vì tạo 9 module với stub vô nghĩa; tối giản Clean Architecture theo độ phức tạp từng use case.
17. **Không tự chốt điều chưa quyết định:** `[PROPOSED]`/`[BLOCKING]` không được biến thành hành vi production bằng phỏng đoán của Codex.
18. **Không phá code nhóm:** quan sát `git status`, thao tác trên branch riêng, không force-push hoặc reset/hủy thay đổi chưa lưu, không commit secrets.
