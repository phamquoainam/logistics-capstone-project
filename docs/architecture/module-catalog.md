# DDD Context Map & Module Catalog

**Nguồn:** [REPORT] Chương 4 §4.2.2, đặc biệt các bảng bounded context, module catalog và dependency.

## 1. Bounded Context (không đồng nhất module và process)

| Bounded Context | Phân loại DDD | Module | Lý do phân chia |
| --- | --- | --- | --- |
| Identity & Access | Generic | identity | Quản lý tài khoản, phiên, vai trò và phạm vi quyền; không sở hữu nghiệp vụ giao nhận hoặc tài chính. |
| Merchant Management | Supporting | merchant | Quản lý tổ chức merchant, hồ sơ và điểm lấy hàng; có vòng đời khác với tài khoản đăng nhập. |
| Shipment Management | Core | order | Quản lý vận đơn, kiện, trạng thái, lịch sử hành trình và kết quả giao nhận. |
| Pricing | Supporting | pricing | Quản lý bảng giá, phiên bản quy tắc và báo giá; vòng đời giá độc lập với vận đơn. |
| Dispatch & Hub Operations | Core | dispatch | Tổ chức chuyến, phân công và kiểm soát việc giữ–bàn giao kiện tại hub. |
| Warehouse Management | Supporting | warehouse | Quản lý hàng hóa theo SKU, tồn kho, giữ hàng và fulfillment; khác với trung chuyển kiện tại hub. |
| Finance | Supporting | finance | Quản lý ví, sổ cái, thanh toán và COD với các bất biến tài chính riêng. |
| Routing & Location | Core | routing | Hỗ trợ quyết định tuyến và quản lý vị trí tác nghiệp; không quyết định trạng thái giao hàng. |
| Notification | Generic | notification | Cung cấp thông báo trong ứng dụng từ kết quả nghiệp vụ đã ghi nhận. |

## 2. Module Catalog: ownership và public contract

| Module | Trách nhiệm và giới hạn | Dữ liệu thuộc sở hữu | Public command/query/event tiêu biểu |
| --- | --- | --- | --- |
| identity | Xác thực, tài khoản, vai trò và phạm vi quyền. Không quyết định điều kiện tài chính hoặc trạng thái giao nhận. | Account, Session, Role, Permission, Access Scope. | Quản lý tài khoản; truy vấn phạm vi quyền; kiểm tra quyền truy cập chức năng. |
| merchant | Hồ sơ tổ chức và điểm lấy hàng. Không sở hữu ví, tồn kho hoặc vận đơn. | Merchant Profile, Pickup Point. | GetMerchant, GetPickupPoint, cập nhật hồ sơ và điểm lấy. |
| order | Tạo/import, xác nhận, hủy và theo dõi vận đơn; ghi nhận giao nhận và POD. | Shipment, Parcel, Address Snapshot, Quote Snapshot, Tracking Event, Delivery Record, POD Metadata. | CreateShipment, ConfirmShipment, CancelShipment, RecordDelivery, GetShipment; sự kiện trạng thái vận đơn. |
| pricing | Quản lý phiên bản giá, tính và kiểm tra hiệu lực báo giá. Không thu tiền. | Tariff, Price Rule Version, Quote. | CalculateQuote, ValidateQuote, PublishTariff; kết quả có phiên bản và chi tiết phí. |
| warehouse | Nhập/QC/putaway, giữ hàng, pick–pack–ship, kiểm kê, điều chỉnh và hàng hoàn. | SKU, Warehouse, Storage Location, Inventory Balance, Stock Movement, Reservation, Outbound, Return Receipt. | ReserveStock, ReleaseReservation, ConfirmOutbound, ReceiveReturn, GetStock; dữ kiện xuất hàng. |
| finance | Ví, nạp tiền, ledger, COD, đối soát, hoàn cước và điều chỉnh. Không thay đổi trạng thái vận đơn. | Wallet, Money Operation, Ledger Transaction, Posting, Payment, COD Item, Remittance, Reconciliation Period, Payout, Adjustment. | ChargeShippingFee, RefundShippingFee, RecordCODCollection, ConfirmRemittance, CloseReconciliation; sự kiện tài chính. |
| dispatch | Hub, chuyến, manifest, phân công, custody và ngoại lệ vận hành. Không quản lý tồn SKU. | Hub, Trip, Leg, Manifest, Assignment, Custody Record. | AssignShipment, ReassignShipment, ConfirmHandover, GetDeliveryTasks; dữ kiện phân công và bàn giao. |
| routing | Tạo/kiểm tra/xác nhận tuyến; quản lý phiên và mẫu vị trí. Không tự phân công hoặc xác nhận giao hàng. | Route Plan, Route Stop, Location Session, Location Update. | ProposeRoute, ConfirmRoute, RecordLocation, GetLatestLocation; kết quả tuyến và độ mới vị trí. |
| notification | Tạo, cung cấp và đánh dấu đọc thông báo trong ứng dụng. | Notification, trạng thái đọc, lịch sử xử lý thông báo. | ListNotifications, MarkAsRead; tiếp nhận dữ kiện công khai từ miền nguồn. |

## 3. Allowed dependency (compile-time)

| Consumer | Chỉ được gọi public API của |
| --- | --- |
| identity | Không phụ thuộc module nghiệp vụ khác. |
| merchant | identity. |
| pricing | Không phụ thuộc module nghiệp vụ khác; nhận đầu vào theo hợp đồng Pricing. |
| order | merchant, pricing. |
| warehouse | Không gọi ngược Order/Finance; dữ liệu liên module được lớp điều phối cung cấp. |
| finance | Không gọi ngược Order/WMS; phối hợp liên miền qua lớp điều phối. |
| dispatch | order, để kiểm tra vận đơn và đề nghị ghi nhận chuyển trạng thái hợp lệ. |
| routing | dispatch, để kiểm tra nhiệm vụ và phân công hiện tại. |
| notification | Public event của order, warehouse, finance, dispatch, routing; API identity phục vụ xác định người nhận. |
| Application coordinator | API của các module cần thiết cho luồng đang điều phối; không được truy cập implementation nội bộ. |

### D1-04: trusted actor contract (ACCEPTED, 2026-10-13)

Ngoài các collaborator nghiệp vụ trên, `order` (package thực tế `orders`) được
phụ thuộc **duy nhất** `identity.api.AuthenticatedActor` tại `internal.web` và
`internal.application`: bốn thuộc tính `id`, `enabled`, `role`, `merchantId`.
Principal do backend xác thực và tra cứu account hiện hành trong security context;
không bind actor từ request. Orders domain không phụ thuộc Identity. Không cho phép
Orders dùng `AccountView`, API Identity khác, repository hoặc `identity.internal`.
Đây là dependency authentication/authorization có phạm vi hẹp được người dùng
phê duyệt, không mở rộng collaborator nghiệp vụ. [ADR-0004](decisions/ADR-0004-architecture-verification.md).

## 4. Các quan hệ upstream/downstream cần hợp đồng rõ

| Quan hệ | Contract | Không được làm |
| --- | --- | --- |
| Pricing → Order | Báo giá, phiên bản, đầu vào, chi tiết phí và thời hạn. | Order không tự tính lại hoặc sửa quy tắc giá đã áp dụng. |
| WMS → Order | Kết quả giữ hàng, lượng còn giữ và kết quả fulfillment. | Order không trực tiếp cập nhật tồn kho. |
| Finance → Order | Kết quả thu hoặc hoàn cước và tham chiếu giao dịch. | Order không sửa số dư hoặc tạo bút toán. |
| Order ↔ Dispatch | Vận đơn đủ điều kiện; dữ kiện lấy hàng và bàn giao. | Dispatch quyết định tác nghiệp; Order kiểm tra và ghi nhận chuyển trạng thái vận đơn. |
| WMS → Dispatch | Kiện, vận đơn và chứng từ xuất phục vụ bàn giao. | Xuất WMS không tự xác nhận hub đã nhận kiện. |
| Dispatch ↔ Routing | Tập nhiệm vụ, phân công và tuyến đề xuất. | Routing không tự tạo phân công hoặc thay đổi trạng thái giao nhận. |
| Miền nghiệp vụ → Notification | Dữ kiện đã ghi nhận, đối tượng nguồn và thông tin định tuyến người nhận. | Notification không quyết định lại kết quả nghiệp vụ nguồn. |
| VNPay / Maps → Adapter nội bộ | Dữ liệu thanh toán, tọa độ và dữ liệu di chuyển. | Adapter kiểm tra, chuyển đổi và xử lý lỗi theo hợp đồng nội bộ. |

## 5. Luồng xuyên miền

| Luồng | Module tham gia | Yêu cầu nhất quán |
| --- | --- | --- |
| Xác nhận vận đơn — UC-M05 | Pricing kiểm tra quote; WMS giữ hàng nếu áp dụng; Finance thu cước; Order xác nhận vận đơn. | Phần ghi dữ liệu nghiệp vụ phải hoàn tất hoặc rollback cùng nhau trong giao dịch cục bộ được thiết kế tại mục 4.6.1. |
| Hủy vận đơn — UC-M06 | Order kiểm tra mốc hủy; WMS giải phóng lượng còn giữ; Finance hoàn cước nếu chính sách cho phép. | Không giải phóng hàng đã xuất hoặc hoàn tiền lặp; các thay đổi tự động liên quan được phối hợp nhất quán. |
| Xuất WMS và nhận tại hub — UC-W05 / D04 | WMS ghi nhận xuất hàng; Dispatch ghi nhận việc tiếp nhận kiện khi bàn giao thực tế. | Hai mốc có chứng từ riêng; không dùng kết quả xuất kho để tự xác nhận nhập hub. |
| Giao hàng có COD — UC-S04 | Dispatch kiểm tra phân công; Order ghi nhận kết quả và POD; Finance ghi nhận COD thực thu. | Kết quả giao và ghi nhận thu COD liên quan phải nhất quán; trạng thái nộp và đối soát tiếp tục được quản lý riêng. |
| Xác nhận tuyến — UC-D07 | Routing kiểm tra tuyến; Dispatch cung cấp nhiệm vụ và phân công còn hiệu lực. | Kiểm tra lại trước khi áp dụng; tuyến không tự thay đổi phân công. |
| Thông báo nghiệp vụ | Module nguồn ghi nhận kết quả; Notification xử lý dữ kiện đã commit. | Cho phép trễ, chống tác động trùng; lỗi thông báo không đảo kết quả nghiệp vụ đã hoàn tất. |

## 6. Ràng buộc code

- Mỗi domain nằm trong `com.example.elogistics.<module>` và có `api/` công khai, `internal/` chỉ cho module đó.
- Module gọi qua **type/service trong `api/`** hoặc published event; không tham chiếu `othermodule.internal.*`, entity JPA hoặc repository của module khác.
- `workflow` được quyền gọi `api` của nhiều module để phối hợp transaction; không được sở hữu dữ liệu nghiệp vụ riêng của các module.
- `platform` chỉ chứa hạ tầng kỹ thuật dùng chung (security primitives, clock, idempotency kernel, error contract…), tránh trở thành domain service hỗn tạp.
- Không dùng `shared` để chứa entity nghiệp vụ xuyên module; không tạo repository tiện lợi bypass ownership.
- Kiểm chứng bằng Spring Modulith/ArchUnit thích hợp sau khi có skeleton thực.
