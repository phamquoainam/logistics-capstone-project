# Database Ownership & Migration Strategy

**Nguồn:** [REPORT] Chương 4 §4.3.1. **[PROPOSED]** chi tiết cấu hình Flyway theo schema và bố trí script vì phần 4.3.4 chưa hiện diện trong file nguồn.

## Ownership mapping

| Owner | Schema | Nhóm bảng | Cách truy cập của module khác |
| --- | --- | --- | --- |
| Identity | identity | user_account, role, permission, user_role, role_permission, access_scope, dữ liệu phiên. | Public API xác thực và truy vấn phạm vi quyền; không đọc thông tin xác thực nội bộ. |
| Merchant | merchant | merchant_profile, pickup_point. | Query hồ sơ, trạng thái và điểm lấy hàng; cập nhật qua command của Merchant. |
| Order | orders | shipment, parcel, address_snapshot, quote_snapshot, tracking_event, delivery_attempt, pod_metadata. | Query vận đơn; command chuyển trạng thái và ghi nhận kết quả giao nhận. |
| Pricing | pricing | tariff, price_rule_version, price_rule, quote. | API tính cước và kiểm tra báo giá; không sửa trực tiếp bảng giá. |
| Warehouse | warehouse | sku, warehouse, storage_location, chứng từ nhận/QC/putaway, stock_balance, stock_movement, reservation, chứng từ xuất, kiểm kê và hàng hoàn. | Query tồn và command giữ, giải phóng hoặc xuất hàng. |
| Finance | finance | Ví, tài khoản sổ cái, nghiệp vụ tiền, bút toán, payment intent, COD, nộp tiền, phân bổ, kỳ đối soát, payout và adjustment. | Query tài chính theo quyền; command thu cước, hoàn tiền hoặc ghi nhận COD. |
| Dispatch | dispatch | hub, trip, leg, manifest, manifest_item, assignment, custody_record. | Query nhiệm vụ; command phân công và bàn giao. |
| Routing | routing | route_plan, route_stop, location_session, location_update. | Query tuyến và vị trí theo quyền; cập nhật qua API của Routing. |
| Notification | notification | notification, trạng thái đã đọc và lịch sử xử lý. | Query/thao tác trên thông báo của người dùng được cấp quyền. |

## Quy tắc PostgreSQL
- Một Postgres instance, một database, schema theo module. DML của bảng do module owner chịu trách nhiệm.
- Runtime account có DML thích hợp, không được DDL; migration account riêng cho Flyway.
- PK định danh ổn định; UUID được **đề xuất**, mã nghiệp vụ và reference bảo vệ bằng UNIQUE theo scope.
- Composite reference dùng owner/merchant ID để tránh cross-tenant.
- Nội module: FK phù hợp. Cross-schema FK có thể dùng có kiểm soát nhưng phải ghi nhận coupling.
- Tiền: biểu diễn chính xác và contract JSON an toàn; không `float`/`double` hoặc `Number` dễ mất chính xác.
- UTC thời điểm, timezone hiển thị cấu hình; tracking/ledger/movement được bảo quản lịch sử.

## [PROPOSED] Flyway baseline
1. Nơi đặt: `apps/backend/src/main/resources/db/migration/` với một chuỗi version duy nhất `V1__...sql`, `V2__...sql`, `V3__...sql`…; hoặc `db/migration/<module>` với **một owner kiểm soát thứ tự**, không để 9 nhóm tự chọn V1 va chạm.
2. V1 tạo schemas và extensions được duyệt; V2+ tạo identity/merchant; các migration tiếp theo theo phụ thuộc của vertical slice. Định danh version bất biến khi đã dùng chung.
3. Không sửa migration đã chạy trên môi trường chia sẻ: viết migration mới, có backfill/compatibility nếu cần.
4. CI chạy migrate trên Postgres thật (Testcontainers), kiểm tra migrate từ DB trống, upgrade từ snapshot thử nghiệm và rollback app về build trước nếu tương thích.
5. Hibernate chỉ `validate`; cấm `create`/`update` schema.
6. Test DB constraint: money >= 0, reserved <= allocatable, unique active assignment (partial index), duplicate operation, FK ownership; quy tắc liên dòng cần transaction + code và integration tests.
7. Log owner/reviewer trong PR migration; dữ liệu mẫu tách riêng với production DDL, không gắn dữ liệu nhạy cảm.

## Các bất biến xuyên bảng
- Ledger transaction: ít nhất hai posting, cùng currency, tổng Nợ = tổng Có; cập nhật balance và postings cùng transaction; refund/adjustment thêm bút toán tham chiếu.
- Stock balance + movement + reservation cùng transaction; inventory available = đủ điều kiện − remaining reservation, không âm.
- Shipment confirm: quote hợp lệ + fee charge + reservation (nếu WMS) + shipment state cùng ACID transaction; idempotency cùng phạm vi.
- COD allocation không vượt khoản nộp và nghĩa vụ; đóng reconciliation recheck và snapshot trong transaction; kỳ đã đóng bất biến.

## Danh mục test bắt buộc
Cross-tenant FK, concurrent reservation, concurrent wallet debit, duplicate callback, active assignment conflict, balance/movement mismatch, double-posted payout, edit closed reconciliation, migrate empty DB và migration upgrade.
