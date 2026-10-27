> Current canonical target follows [Owner account-first decision](../decisions/customer-account-merchant-admission.md). Historical statements below are source context where superseded; accepted runtime/security unchanged.

# Ma trận Role × Module × Business Capability — E-Logistics Capstone

> **Trạng thái:** ĐỀ XUẤT PHÂN TÍCH — chưa phải Role/Permission/Feature contract được canonical SDD phê duyệt.
> **Nguồn:** `E_Logistics_Business_Complete_GroupEF_Full.md`, các mục II (33 Role), III (lifecycle), IV (permission), V (18 module), VIII (P0/P1/P2 và M1–M4); cùng quyết định trong cuộc trao đổi này: quyết định phạm vi Nhóm A, mở rộng FULL Nhóm B và mở rộng FULL Nhóm C–D và FULL Nhóm E–F (2026-10-26): Retail Sender là account cá nhân có đầy đủ nghiệp vụ gửi lẻ; Guest có đủ tiện ích công khai nhưng không tạo Shipment online; Recipient không có account/Portal và phản ánh đơn Merchant qua Merchant Support; khách không đăng ký chỉ có thể ký gửi tại điểm giao dịch qua Station Operator.
> **Phạm vi:** 33 Role/chủ thể/principal mục tiêu (bao gồm external actors và integration principal); không chứng nhận trạng thái đã code/test.

## 0. Cách đọc và nguyên tắc

- **CORE:** Năng lực bắt buộc trong **phạm vi mục tiêu sản phẩm toàn diện M1–M4**, không đồng nghĩa bắt buộc có ngay ở Feature 003. P2 có thể chứa CORE nếu không thể hoàn thành quyết toán hoặc hỗ trợ đúng cách khi đến mốc tương ứng.
- **EXPAND:** Năng lực có giá trị thực tế nhưng không bắt buộc để đạt end-to-end tối thiểu; có thể triển khai sau.
- **DEFER:** Không mặc định xây trong MVP hoặc quyền bị loại khỏi Role này; xem **Điều kiện/giới hạn** để tránh nhầm 'không có quyền' với 'chưa làm'.
- **Access ≠ ownership:** Role được đọc thông tin của một module không đồng nghĩa được quyền tạo/sửa/xóa đối tượng của module.
- **Dữ liệu thực tế:** Cần đối chiếu từng capability với SDD, backend API, DB schema, frontend, tests trước khi gắn `IMPLEMENTED`.
- **Recipient/Guest:** External Business Actors, **không tạo account IAM**; Guest có đủ tiện ích công khai, trừ tạo Shipment online; Recipient chỉ tra cứu công khai, phản ánh Merchant Support (đơn Merchant) và không có Portal riêng.
- **Retail Sender:** Bắt buộc là account cá nhân, không phụ thuộc Merchant membership; tự sở hữu Shipment và có đầy đủ chức năng gửi lẻ đã liệt kê trong Business Reference.
- **Walk-in:** Station Operator tạo Shipment tại điểm giao dịch sau xác minh và xuất biên nhận; Guest không thể dùng đường này qua public API.
- **Nhóm E–F — Target FULL:** 7 Role Nhóm E và 6 Role Nhóm F; phân tách rõ Claims approval/payment, Cashier/Finance, Business Admin/Identity Admin/Super Admin/IT/Auditor, Partner Station scope. CORE có nghĩa target chứ không phải đã triển khai.
- **Nhóm B — Target FULL:** 6 human roles (Owner, Manager, Operator, Warehouse Staff, Finance, Support) và 1 Enterprise/API integration principal. Các năng lực đã liệt kê là CORE thuộc target scope; triển khai theo dependency SDD. Owner/Operator có thể giữ quyền tạo ticket nếu được cấp rõ ràng, nhưng không thay thế Merchant Support trong thiết kế mục tiêu.

## 1. Danh mục Role

| ID | Role / Actor | Nhóm | Ưu tiên | Loại |
|---|---|---|---|---|
| R01 | Guest | A | P0 | External |
| R02 | Merchant Owner | B | P0 | Account |
| R03 | Merchant Operator | B | P0 | Account |
| R04 | Business Admin | F | P0 | Account |
| R05 | Identity Admin | F | P0 | Account |
| R06 | Dispatcher | D | P1 | Account |
| R07 | Pickup Courier | C | P1 | Account |
| R08 | Station Operator | D | P1 | Account |
| R09 | Delivery Courier | C | P1 | Account |
| R10 | Recipient | A | P1 | External |
| R11 | Merchant Finance | B | P2 | Account |
| R12 | COD Cashier | E | P2 | Account |
| R13 | Finance Operator | E | P2 | Account |
| R14 | Customer Service Agent | E | P2 | Account |
| R15 | Claims Officer | E | P2 | Account |
| R16 | Station Manager | D | P2 | Account |
| R17 | Auditor | F | P2 | Account |
| R18 | Retail Sender | A | P0 | Account cá nhân |

| R19 | Merchant Manager | B | P0 | Account |
| R20 | Merchant Warehouse Staff | B | P1 | Account |
| R21 | Merchant Support | B | P1 | Account |
| R22 | Enterprise/API Client | B | P2 | Integration principal |

| R23 | Line-haul Driver | C | P1 | Account |
| R24 | Courier Team Leader | C | P1 | Account |
| R25 | Sorting Staff | D | P1 | Account |
| R26 | Hub Supervisor / Warehouse Manager | D | P1 | Account |
| R27 | Network Operations Manager | D | P2 | Account |

| R28 | Pricing / Billing Administrator | E | P1 | Account |
| R29 | Sales / Key Account Manager | E | P2 | Account |
| R30 | Risk / Fraud / Compliance Officer | E | P1 | Account |
| R31 | Platform Super Admin | F | P2 | Account |
| R32 | IT / Integration Operations | F | P2 | Account |
| R33 | Partner / Franchise Operator | F | P2 | Account |

## 2. Danh mục module

| ID | Module |
|---|---|
| M01 | Identity & Access |
| M02 | Merchant Management |
| M03 | Address & Coverage |
| M04 | Pricing & Quotation |
| M05 | Order & Shipment Management |
| M06 | Parcel & Label |
| M07 | Pickup Management |
| M08 | Station & Hub Operations |
| M09 | Transportation Management |
| M10 | Last-mile Delivery |
| M11 | Tracking & Notifications |
| M12 | COD Management |
| M13 | Billing & Settlement |
| M14 | Returns & Reverse Logistics |
| M15 | Customer Support & Claims |
| M16 | Analytics & SLA |
| M17 | Partner & Integrations |
| M18 | Security & Audit |

## 3. Ma trận Role × Module (CORE/EXPAND/DEFER)

Một ô có thể chứa nhiều loại năng lực: **C/E** = CORE và EXPAND. `·` = chưa có capability được chỉ định; không có nghĩa module hoàn toàn không liên quan.

| Role | M01 | M02 | M03 | M04 | M05 | M06 | M07 | M08 | M09 | M10 | M11 | M12 | M13 | M14 | M15 | M16 | M17 | M18 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| R01 Guest | C | · | C | C | D | · | · | · | · | · | C | · | · | · | C | · | · | · |
| R02 Merchant Owner | C | C | C | C | C | · | · | · | · | · | C | C | C | · | C | C | C | · |
| R03 Merchant Operator | C | C | C | C | C | C | C | · | · | C | C | C | · | C | C | · | C | · |
| R04 Business Admin | C | C | C | C | C | · | C | C | · | · | · | D | · | · | C | C | · | C |
| R05 Identity Admin | C | D | · | · | · | · | · | · | · | · | · | D | · | · | · | · | · | C |
| R06 Dispatcher | · | · | · | · | · | · | C | C | · | C | C | · | · | C | · | C | · | · |
| R07 Pickup Courier | · | · | · | · | · | C | C | C | · | · | C | · | · | · | · | C | · | · |
| R08 Station Operator | · | · | · | · | C | C | · | C | C | · | C | · | C | C | · | · | · | · |
| R09 Delivery Courier | · | · | · | · | · | C | · | C | · | C | C | C | · | C | · | C | · | · |
| R10 Recipient | D | · | · | · | · | · | · | · | · | C | C | C | · | · | C | · | · | · |
| R11 Merchant Finance | · | · | · | C | · | · | · | · | · | · | · | C | C | · | C | C | · | · |
| R12 COD Cashier | · | · | · | · | · | · | · | C | · | · | · | C | C | · | · | · | · | C |
| R13 Finance Operator | · | · | · | C | · | · | · | · | · | · | · | C | C | · | · | C | · | C |
| R14 Customer Service Agent | · | · | C | · | C | · | · | · | · | C | C | C | · | C | C | C | · | · |
| R15 Claims Officer | · | · | · | · | · | · | · | C | · | C | · | C | C | · | C | · | · | C |
| R16 Station Manager | C | · | · | · | · | · | C | C | C | C | · | C | · | · | · | C | · | C |
| R17 Auditor | C | · | · | · | · | · | · | · | · | · | C | C | C | · | C | C | · | C |
| R18 Retail Sender | C | · | C | C | C | C | C | · | · | · | C | · | C | C | C | · | · | · |
| R19 Merchant Manager | C | C | C | · | C | · | · | · | · | C | C | · | · | · | C | C | · | · |
| R20 Merchant Warehouse Staff | · | · | · | · | C | C | C | · | · | · | C | · | · | · | · | · | · | · |
| R21 Merchant Support | · | · | · | · | C | · | · | · | · | C | C | · | · | C | C | · | · | · |
| R22 Enterprise/API Client | C | · | · | C | C | C | · | · | · | · | C | · | · | · | · | · | C | · |
| R23 Line-haul Driver | · | · | · | · | · | C | · | C | C | · | · | · | · | · | · | · | · | C |
| R24 Courier Team Leader | · | · | · | · | · | · | C | C | · | C | · | · | · | · | C | C | · | C |
| R25 Sorting Staff | · | · | · | · | · | C | · | C | C | · | · | · | · | · | · | · | · | C |
| R26 Hub Supervisor / Warehouse Manager | · | · | · | · | · | · | · | C | C | · | · | · | · | · | C | C | · | C |
| R27 Network Operations Manager | · | · | C | · | · | · | C | C | C | C | · | C | · | · | · | C | · | C |
| R28 Pricing / Billing Administrator | · | · | · | C | · | · | · | · | · | · | · | · | C | · | · | · | · | C |
| R29 Sales / Key Account Manager | · | C | · | C | · | · | · | · | · | · | · | · | · | · | C | C | C | C |
| R30 Risk / Fraud / Compliance Officer | C | · | · | · | C | · | · | C | · | · | · | C | C | · | C | · | · | C |
| R31 Platform Super Admin | C | C | · | · | · | · | · | · | · | C | · | C | · | · | · | · | · | C |
| R32 IT / Integration Operations | C | · | · | · | C | · | · | · | · | · | C | · | · | · | · | C | C | C |
| R33 Partner / Franchise Operator | · | · | · | · | C | C | · | C | · | · | · | · | C | · | · | · | C | C |

## 4. Ma trận cấp Business Capability (đối chiếu trực tiếp)

| ID | Role | Module | Phạm vi | Business Capability | Điều kiện và ranh giới quyền |
|---|---|---|---|---|---|
| BC-001 | R01 Guest | M03 | CORE | Tra cứu vùng phục vụ công khai | Chỉ dữ liệu công khai |
| BC-002 | R01 Guest | M04 | CORE | Ước tính cước vận chuyển | Không lộ giá hợp đồng Merchant |
| BC-003 | R01 Guest | M11 | CORE | Tra cứu tracking giới hạn | Không lộ PII, COD nhạy cảm; tăng xác minh khi cần |
| BC-004 | R01 Guest | M15 | CORE | Gửi yêu cầu tư vấn/hỗ trợ không đăng nhập | Rate limit và kiểm soát spam |
| BC-005 | R01 Guest | M03 | CORE | Tra cứu bưu cục/Hub có phục vụ, địa chỉ, giờ hoạt động | Dữ liệu công khai |
| BC-006 | R01 Guest | M05 | DEFER | Tự tạo Shipment online khi không có tài khoản — BỊ CẤM theo phạm vi Capstone | Không cấp API public tạo Shipment; ký gửi tại quầy do Station Operator thực hiện |
| BC-007 | R02 Merchant Owner | M01 | CORE | Dùng tài khoản với Merchant membership ACTIVE | Đúng org và trạng thái |
| BC-008 | R02 Merchant Owner | M02 | CORE | Quản lý hồ sơ và nhân viên Merchant | Đúng quyền quản trị tổ chức |
| BC-009 | R02 Merchant Owner | M02 | CORE | Quản lý pickup point | Tenant isolation; archive/sửa có điều kiện |
| BC-010 | R02 Merchant Owner | M03 | CORE | Quản lý địa chỉ lấy hàng | Catalog phiên bản và kiểm tra dữ liệu |
| BC-011 | R02 Merchant Owner | M05 | CORE | Tạo, xem, xác nhận Shipment | Membership, valid state, idempotency |
| BC-012 | R02 Merchant Owner | M05 | CORE | Yêu cầu hủy/sửa Shipment | Chỉ trạng thái cho phép |
| BC-013 | R02 Merchant Owner | M11 | CORE | Theo dõi mọi Shipment thuộc Merchant | Không cross-tenant |
| BC-014 | R02 Merchant Owner | M12 | CORE | Xem COD thuộc Merchant | Chỉ đọc theo quyền |
| BC-015 | R02 Merchant Owner | M13 | CORE | Xem phí, bảng kê và settlement | Chỉ thuộc Merchant |
| BC-016 | R02 Merchant Owner | M15 | CORE | Gửi và theo dõi support ticket | Không tự quyết định bồi thường |
| BC-017 | R02 Merchant Owner | M16 | CORE | Xem KPI, báo cáo chi phí, tỷ lệ giao | Chỉ dữ liệu Merchant |
| BC-018 | R02 Merchant Owner | M17 | CORE | Quản lý API integration/webhook Merchant | Key và scope tách riêng |
| BC-019 | R02 Merchant Owner | M04 | CORE | Xem hợp đồng và biểu phí áp dụng | Không tự sửa bảng giá |
| BC-020 | R03 Merchant Operator | M01 | CORE | Truy cập qua membership Merchant | Không tự nâng quyền |
| BC-021 | R03 Merchant Operator | M02 | CORE | Xem và sửa pickup point được ủy quyền | Không quản lý nhân viên mặc định |
| BC-022 | R03 Merchant Operator | M03 | CORE | Chuẩn hóa địa chỉ nhận/gửi | Dữ liệu hợp lệ |
| BC-023 | R03 Merchant Operator | M04 | CORE | Nhận báo giá và chọn dịch vụ | Snapshot giá khi chốt |
| BC-024 | R03 Merchant Operator | M05 | CORE | Tạo/xác nhận Shipment | Idempotency, đúng org |
| BC-025 | R03 Merchant Operator | M05 | CORE | Xem và yêu cầu hủy/giao lại | Hủy/sửa phải đúng quyền và state |
| BC-026 | R03 Merchant Operator | M06 | CORE | Tạo nhãn và danh sách bàn giao | Barcode liên kết kiện |
| BC-027 | R03 Merchant Operator | M11 | CORE | Tra cứu trạng thái Shipment thuộc scope | Merchant isolation |
| BC-028 | R03 Merchant Operator | M12 | CORE | Xem COD của đơn được giao phụ trách | Không sửa COD đã chốt |
| BC-029 | R03 Merchant Operator | M05 | CORE | Nhập đơn hàng loạt CSV/Excel | Kiểm tra bản ghi trùng và tính hợp lệ |
| BC-030 | R03 Merchant Operator | M15 | CORE | Tạo ticket hỗ trợ Logistics nếu được cấp quyền | Không duyệt bồi thường |
| BC-031 | R04 Business Admin | M01 | CORE | Truy cập admin bằng quyền nghiệp vụ | Không mặc nhiên có Identity quyền |
| BC-032 | R04 Business Admin | M02 | CORE | Duyệt/giám sát Merchant theo policy | Phạm vi org/region |
| BC-033 | R04 Business Admin | M03 | CORE | Quản lý vùng phục vụ theo policy | Có phiên bản và audit |
| BC-034 | R04 Business Admin | M05 | CORE | Giám sát Shipment và ngoại lệ | Không giả lập DELIVERED |
| BC-035 | R04 Business Admin | M16 | CORE | Theo dõi SLA và báo cáo vận hành | Chỉ phạm vi được phép |
| BC-036 | R04 Business Admin | M18 | CORE | Tra cứu audit nghiệp vụ liên quan | Không xóa/sửa audit |
| BC-037 | R04 Business Admin | M07 | CORE | Phê duyệt ngoại lệ điều phối theo thẩm quyền | Không tự tạo bằng chứng pickup |
| BC-038 | R04 Business Admin | M15 | CORE | Phê duyệt escalation theo quyền | Separation of duties |
| BC-039 | R04 Business Admin | M04 | CORE | Quản trị chính sách dịch vụ/bảng giá được ủy quyền | Không tự động có quyền sửa COD |
| BC-040 | R04 Business Admin | M12 | DEFER | Thực hiện điều chỉnh COD/settlement | Không mặc định được phép; cần workflow tài chính riêng |
| BC-041 | R05 Identity Admin | M01 | CORE | Quản lý trạng thái tài khoản và cấp quyền Identity | Đúng giới hạn hợp đồng Identity |
| BC-042 | R05 Identity Admin | M01 | CORE | Session revocation và MFA policy được phép | Audit, tránh quyền vượt scope |
| BC-043 | R05 Identity Admin | M18 | CORE | Xem sự kiện bảo mật Identity | Least privilege |
| BC-044 | R05 Identity Admin | M01 | CORE | Xử lý credential incident và provisioning nâng cao | Quy trình quản trị riêng |
| BC-045 | R05 Identity Admin | M02 | DEFER | Sửa nội dung nghiệp vụ Merchant | Không tự có quyền Logistics |
| BC-046 | R05 Identity Admin | M12 | DEFER | Sửa/duyệt COD | Ngoài quyền Identity |
| BC-047 | R06 Dispatcher | M07 | CORE | Xem pickup pool và phân công pickup | Theo vùng, tải và trạng thái |
| BC-048 | R06 Dispatcher | M10 | CORE | Xem delivery pool và phân công giao | Kiện đủ điều kiện |
| BC-049 | R06 Dispatcher | M07 | CORE | Tái phân công pickup có lý do | Audit, không tự xác nhận pickup |
| BC-050 | R06 Dispatcher | M10 | CORE | Điều phối giao lại | Delivery attempt và chính sách |
| BC-051 | R06 Dispatcher | M11 | CORE | Theo dõi sự kiện và nhiệm vụ vận chuyển | Không truy cập ngoài region |
| BC-052 | R06 Dispatcher | M14 | CORE | Điều phối hoàn hàng theo workflow | Không xóa lịch sử delivery |
| BC-053 | R06 Dispatcher | M16 | CORE | Theo dõi SLA, cảnh báo quá hạn | Không tự sửa tracking |
| BC-054 | R06 Dispatcher | M10 | CORE | Gom điểm/tối ưu tuyến | Chưa yêu cầu routing optimization chuyên sâu |
| BC-055 | R06 Dispatcher | M08 | CORE | Điều phối qua nhiều Station | Policy phạm vi bưu cục |
| BC-056 | R07 Pickup Courier | M07 | CORE | Xem pickup assignment cá nhân | Chỉ task được phân công |
| BC-057 | R07 Pickup Courier | M07 | CORE | Check-in/scan và ghi nhận PICKED_UP | Có bằng chứng, đúng trạng thái |
| BC-058 | R07 Pickup Courier | M07 | CORE | Ghi pickup thất bại và lý do | Attempt có timestamp |
| BC-059 | R07 Pickup Courier | M08 | CORE | Bàn giao kiện cho Station | Bên nhận xác nhận custody |
| BC-060 | R07 Pickup Courier | M11 | CORE | Xem lịch sử nhiệm vụ cá nhân | PII tối thiểu |
| BC-061 | R07 Pickup Courier | M16 | CORE | Xem KPI cá nhân | Không có quyền giám sát toàn đội |
| BC-062 | R07 Pickup Courier | M07 | CORE | Tối ưu thứ tự tuyến pickup | Không bắt buộc GPS/routing chuyên sâu |
| BC-063 | R08 Station Operator | M08 | CORE | Nhận kiện và xác nhận custody tại Station | Scan, người nhận, timestamp |
| BC-064 | R08 Station Operator | M06 | CORE | Kiểm tra barcode/cân đo kiện | Ghi nhận sai lệch có chứng cứ |
| BC-065 | R08 Station Operator | M08 | CORE | Nhập/xuất kiện và lập manifest | Không ghi đè lịch sử |
| BC-066 | R08 Station Operator | M08 | CORE | Bàn giao kiện cho courier/tuyến | Hai phía xác nhận khi cần |
| BC-067 | R08 Station Operator | M14 | CORE | Nhận kiện giao thất bại/đang hoàn | Đúng trạng thái custody |
| BC-068 | R08 Station Operator | M11 | CORE | Xem trạng thái kiện tại Station | Đúng station scope |
| BC-069 | R08 Station Operator | M08 | CORE | Kiểm kê tồn chi tiết, phân loại tự động | Có quy trình chênh lệch |
| BC-070 | R08 Station Operator | M09 | CORE | Thực hiện phần nghiệp vụ tại Station trong bàn giao chuyến xe liên Hub | Station xác nhận nhận/xuất manifest, không vận hành lái xe thay Line-haul Driver |
| BC-071 | R09 Delivery Courier | M10 | CORE | Xem và nhận kiện giao theo assignment | Chỉ nhiệm vụ cá nhân |
| BC-072 | R09 Delivery Courier | M10 | CORE | Ghi OUT_FOR_DELIVERY | Chỉ khi kiện đã được bàn giao hợp lệ |
| BC-073 | R09 Delivery Courier | M10 | CORE | Ghi DELIVERED kèm POD | Bằng chứng hợp lệ; chống trùng |
| BC-074 | R09 Delivery Courier | M10 | CORE | Ghi DELIVERY_FAILED kèm lý do/attempt | Không xóa thất bại |
| BC-075 | R09 Delivery Courier | M12 | CORE | Ghi nhận khoản COD đã thu | Đúng số tiền đã chốt |
| BC-076 | R09 Delivery Courier | M12 | CORE | Yêu cầu nộp COD cho quỹ | Không tự xác nhận quỹ đã nhận |
| BC-077 | R09 Delivery Courier | M14 | CORE | Trả kiện giao thất bại về Station | Có custody handover |
| BC-078 | R09 Delivery Courier | M11 | CORE | Xem thông tin liên hệ người nhận tối thiểu | Chỉ trong assignment |
| BC-079 | R09 Delivery Courier | M10 | CORE | Tuyến giao hàng tối ưu/GPS real-time | Không bắt buộc với MVP |
| BC-080 | R10 Recipient | M11 | CORE | Nhận/tra cứu thông tin trạng thái giới hạn | Không login; xác minh phù hợp |
| BC-081 | R10 Recipient | M10 | CORE | Nhận hoặc từ chối kiện; xác nhận POD/OTP khi có | Courier ghi nhận; không cấp role IAM |
| BC-082 | R10 Recipient | M12 | CORE | Thanh toán COD đã xác nhận | Không thay đổi cod_amount |
| BC-083 | R10 Recipient | M15 | CORE | Phản ánh đơn Merchant qua Merchant Support | Merchant lập ticket với Logistics CSKH; không có Recipient Portal |
| BC-084 | R10 Recipient | M10 | CORE | Thông tin giao lại/chỉ dẫn qua Merchant hoặc Courier | Nhân sự được quyền xử lý theo state; Recipient không thao tác backend |
| BC-085 | R10 Recipient | M01 | DEFER | Recipient account/login/Portal riêng — KHÔNG THUỘC PHẠM VI | Không cấp Identity role; chỉ public tracking giới hạn |
| BC-086 | R11 Merchant Finance | M12 | CORE | Xem COD dự kiến/đã thu/đang giữ/đã trả | Merchant isolation |
| BC-087 | R11 Merchant Finance | M13 | CORE | Xem bảng kê đối soát và thanh toán | Không sửa sổ cái |
| BC-088 | R11 Merchant Finance | M13 | CORE | Tra cứu phí và phụ phí theo Shipment | Tôn trọng snapshot giá |
| BC-089 | R11 Merchant Finance | M15 | CORE | Mở tra soát COD thiếu/phí sai | Không tự duyệt hoàn tiền |
| BC-090 | R11 Merchant Finance | M16 | CORE | Xuất báo cáo COD, phí | Đúng phạm vi Merchant |
| BC-091 | R11 Merchant Finance | M13 | CORE | Đối chiếu/ghi nhận xác nhận bảng kê | Không tự quyết toán phía Logistics |
| BC-092 | R12 COD Cashier | M12 | CORE | Tiếp nhận tiền mặt COD Courier nộp | Hai phía, amount và timestamp |
| BC-093 | R12 COD Cashier | M12 | CORE | Xác nhận nộp và ghi thiếu/thừa | Không âm thầm sửa tiền |
| BC-094 | R12 COD Cashier | M12 | CORE | Quản lý đối chiếu nộp COD theo ca | Không tự settle Merchant |
| BC-095 | R12 COD Cashier | M18 | CORE | Ghi audit tiền nộp và điều chỉnh | Không xóa receipt |
| BC-096 | R12 COD Cashier | M12 | CORE | Kiểm kê két quỹ/đóng ca nâng cao | Có đối chiếu kế toán |
| BC-097 | R13 Finance Operator | M12 | CORE | Đối soát Courier thu và nộp COD | Phải khớp custody tiền và shipment |
| BC-098 | R13 Finance Operator | M12 | CORE | Phát hiện COD thiếu/thừa/trùng | Ledger bất biến/idempotency |
| BC-099 | R13 Finance Operator | M13 | CORE | Tạo bảng kê và kỳ thanh toán Merchant | Có quyền phê duyệt phù hợp |
| BC-100 | R13 Finance Operator | M13 | CORE | Ghi nhận settlement đã thực hiện | Không giả định đã chuyển tiền khi chưa có bằng chứng |
| BC-101 | R13 Finance Operator | M13 | CORE | Tính phí và công nợ phải thu/phải trả | Snapshot giá + audit |
| BC-102 | R13 Finance Operator | M18 | CORE | Audit điều chỉnh tài chính | Không sửa lịch sử |
| BC-103 | R13 Finance Operator | M13 | CORE | Refund và xử lý khiếu nại tài chính | Phải được phê duyệt |
| BC-104 | R13 Finance Operator | M13 | CORE | Tích hợp cổng thanh toán/ngân hàng | Không yêu cầu thanh toán thật trong demo |
| BC-105 | R14 Customer Service Agent | M15 | CORE | Nhận/tạo ticket từ Merchant hoặc người liên quan | Phân loại nguồn; xác minh quyền thông tin |
| BC-106 | R14 Customer Service Agent | M11 | CORE | Tra tracking, POD và điểm quét cuối | Chỉ ticket/scope được phân công |
| BC-107 | R14 Customer Service Agent | M15 | CORE | Phân loại, phân công và cập nhật SLA ticket | Lưu lịch sử thay đổi |
| BC-108 | R14 Customer Service Agent | M10 | CORE | Gửi yêu cầu giao lại/đổi thông tin | Workflow phê duyệt; không tự sửa Shipment đã chốt |
| BC-109 | R14 Customer Service Agent | M15 | CORE | Escalate vụ nghiêm trọng | Không tự phê duyệt bồi thường |
| BC-110 | R14 Customer Service Agent | M15 | CORE | Đa kênh hotline/email/chat tích hợp | Không cần xây portal Recipient |
| BC-111 | R14 Customer Service Agent | M14 | CORE | Theo dõi ticket liên quan hoàn hàng | Không tự sửa custody |
| BC-112 | R15 Claims Officer | M15 | CORE | Mở claim case gắn ticket/Shipment | Không trùng vụ việc |
| BC-113 | R15 Claims Officer | M15 | CORE | Thu thập POD/tracking/ảnh/biên bản | Bằng chứng có nguồn gốc |
| BC-114 | R15 Claims Officer | M15 | CORE | Xác định trách nhiệm và đề xuất phương án | Người đề xuất khác người duyệt nếu cần |
| BC-115 | R15 Claims Officer | M15 | CORE | Ghi nhận outcome và thông báo CS/Merchant | Audit đầy đủ |
| BC-116 | R15 Claims Officer | M13 | CORE | Đề xuất bồi thường/hoàn tiền | Không tự thực hiện settlement |
| BC-117 | R15 Claims Officer | M18 | CORE | Truy vết lịch sử xử lý claim | Không xóa evidence |
| BC-118 | R16 Station Manager | M08 | CORE | Giám sát tồn kiện, nhập/xuất, manifest | Đúng Station scope |
| BC-119 | R16 Station Manager | M07 | CORE | Giám sát pickup/courier thuộc Station | Không xác nhận PICKED_UP thay courier |
| BC-120 | R16 Station Manager | M10 | CORE | Giám sát giao thất bại và tồn phát | Không tạo POD giả |
| BC-121 | R16 Station Manager | M12 | CORE | Giám sát bàn giao COD tại Station | Không sửa sổ cái |
| BC-122 | R16 Station Manager | M16 | CORE | Theo dõi KPI/SLA và năng lực ca | Đúng trạm |
| BC-123 | R16 Station Manager | M08 | CORE | Duyệt sai lệch kho có bằng chứng | Có audit/approval |
| BC-124 | R16 Station Manager | M18 | CORE | Xem audit vận hành của Station | Chỉ đọc |
| BC-125 | R17 Auditor | M18 | CORE | Đọc audit events đúng phạm vi | Read only, không sửa/xóa |
| BC-126 | R17 Auditor | M11 | CORE | Xem lịch sử tracking/custody có chứng cứ | Chỉ dữ liệu được cấp |
| BC-127 | R17 Auditor | M12 | CORE | Đọc log đối soát COD | Không sửa tiền |
| BC-128 | R17 Auditor | M01 | CORE | Xem lịch sử phân quyền và thu hồi quyền | Không quản lý tài khoản |
| BC-129 | R17 Auditor | M15 | CORE | Đọc bằng chứng claim và phê duyệt | Đúng phạm vi kiểm toán |
| BC-130 | R17 Auditor | M16 | CORE | Xuất báo cáo audit | Bảo vệ PII |

| BC-131 | R01 Guest | M04 | CORE | Tra cứu danh mục dịch vụ, điều kiện gửi hàng | Catalog công khai, không lộ hợp đồng riêng |
| BC-132 | R01 Guest | M01 | CORE | Đi đến đăng ký một tài khoản khách hàng cá nhân | Public entry; xác minh Identity; không chọn loại tài khoản, không tự cấp role/Owner |
| BC-133 | R18 Retail Sender | M01 | CORE | Nộp và theo dõi đơn trở thành Merchant từ workspace cá nhân sau đăng nhập | Account verified và eligible hiện hành; không Guest submit, không Owner trước approval |
| BC-134 | R18 Retail Sender | M01 | CORE | Đăng ký/đăng nhập tài khoản cá nhân | Không buộc Merchant membership; xác minh/chống giả mạo |
| BC-135 | R18 Retail Sender | M01 | CORE | Quản lý hồ sơ, tên, liên hệ, trạng thái tài khoản | Chỉ dữ liệu bản thân; thay đổi nhạy cảm cần xác minh |
| BC-136 | R18 Retail Sender | M03 | CORE | Lưu và quản lý địa chỉ gửi/nhận thường dùng | Chỉ sở hữu cá nhân; chuẩn hóa địa chỉ |
| BC-137 | R18 Retail Sender | M03 | CORE | Kiểm tra vùng phục vụ theo tuyến gửi lẻ | Coverage snapshot/quy tắc hợp lệ |
| BC-138 | R18 Retail Sender | M04 | CORE | Báo giá và chọn dịch vụ gửi lẻ | Không áp dụng giá hợp đồng Merchant trái quyền |
| BC-139 | R18 Retail Sender | M05 | CORE | Tạo và xác nhận Shipment gửi hàng cá nhân | Shipment thuộc account; valid state, chống trùng/idempotency |
| BC-140 | R18 Retail Sender | M06 | CORE | Khai báo kiện và nhận mã vận đơn/nhãn/biên nhận gửi | Parcel khác Shipment, thông tin hàng hóa hợp lệ |
| BC-141 | R18 Retail Sender | M07 | CORE | Đặt yêu cầu Pickup tại nhà | Dịch vụ/địa chỉ đủ điều kiện; phân công do Operations |
| BC-142 | R18 Retail Sender | M13 | CORE | Thanh toán phí vận chuyển theo phương thức hỗ trợ | Receipt, price snapshot và chống thu trùng |
| BC-143 | R18 Retail Sender | M11 | CORE | Tra cứu tất cả Shipment do mình tạo | Resource owner isolation; không cross-account |
| BC-144 | R18 Retail Sender | M05 | CORE | Hủy yêu cầu gửi khi trạng thái cho phép | Đã pickup phải chuyển workflow thu hồi/hoàn |
| BC-145 | R18 Retail Sender | M15 | CORE | Mở và theo dõi yêu cầu hỗ trợ/tra soát mất, hỏng | Chỉ Shipment thuộc mình, có ticket/evidence |
| BC-146 | R18 Retail Sender | M14 | CORE | Theo dõi và quản lý hoàn trả kiện gửi lẻ | Return events/custody rõ ràng |
| BC-147 | R18 Retail Sender | M05 | CORE | Xem lịch sử vận đơn đã gửi | Giới hạn theo account và quyền riêng tư |
| BC-148 | R18 Retail Sender | M13 | CORE | Xem/tải hóa đơn và biên nhận | Hợp lệ với hình thức thanh toán, không rò dữ liệu |
| BC-149 | R08 Station Operator | M05 | CORE | Tiếp nhận tại quầy và lập Shipment cho Walk-in Sender | Chỉ nhân viên được phân công, xác minh người gửi; KHÔNG phải Guest API |
| BC-150 | R08 Station Operator | M06 | CORE | Kiểm kiện, cân đo, in nhãn và cấp biên nhận walk-in | Tạo Parcel, timestamp, bằng chứng nhận kiện |
| BC-151 | R08 Station Operator | M13 | CORE | Ghi nhận phí/thanh toán ký gửi tại quầy theo policy | Không tự xác nhận thanh toán thiếu bằng chứng; sổ thu/receipt |
| BC-152 | R08 Station Operator | M08 | CORE | Nhập custody kiện walk-in tại Station/Hub phục vụ giao dịch | Đúng Station, nguồn walk-in, manifest/handover |
| BC-153 | R14 Customer Service Agent | M15 | CORE | Nhận và xử lý ticket trực tiếp từ Retail Sender | Xác minh Shipment thuộc Sender, tracking, timeline, escalation |
| BC-154 | R14 Customer Service Agent | M15 | CORE | Tiếp nhận phản ánh Recipient cho đơn Retail Sender | Không giả định Merchant Support; xác minh tối thiểu; bảo vệ PII |
| BC-155 | R02 Merchant Owner | M15 | CORE | Tạo và theo dõi ticket từ phản ánh Recipient đối với đơn Merchant | Có thể ủy quyền Merchant Support khi Role được bổ sung |
| BC-156 | R03 Merchant Operator | M15 | CORE | Tiếp nhận/chuyển phản ánh Recipient và theo dõi ticket theo ủy quyền | Merchant scope, không tự quyết định bồi thường |

| BC-157 | R02 Merchant Owner | M02 | CORE | Cập nhật thông tin pháp nhân, tên cửa hàng, mã số thuế và cấu hình Merchant | Chỉ Merchant sở hữu; kiểm soát thay đổi nhạy cảm |
| BC-158 | R02 Merchant Owner | M01 | CORE | Mời/thêm/khóa nhân viên, quản lý membership và quyền tổ chức | Không tự cấp quyền nền tảng; phân quyền trong giới hạn |
| BC-159 | R02 Merchant Owner | M02 | CORE | Quản lý nhiều cửa hàng/kho, cấu hình dịch vụ và quy tắc mặc định | Merchant scope; chính sách phiên bản |
| BC-160 | R02 Merchant Owner | M05 | CORE | Tạo Shipment thủ công và hàng loạt, quản lý toàn bộ Shipment Merchant | Có kiểm tra dữ liệu và idempotency, không sửa lịch sử |
| BC-161 | R02 Merchant Owner | M12 | CORE | Xem COD dự kiến, đã thu, đã nộp, đã đối soát và công nợ | Chỉ đọc tài chính thuộc Merchant |
| BC-162 | R02 Merchant Owner | M13 | CORE | Xem lịch thanh toán, hóa đơn và chứng từ COD/cước | Chỉ Merchant được sở hữu |
| BC-163 | R02 Merchant Owner | M15 | CORE | Theo dõi claim, yêu cầu bồi thường và đơn bất thường | Không tự phê duyệt bồi thường của hãng |
| BC-164 | R02 Merchant Owner | M17 | CORE | Quản lý cấu hình tích hợp và webhook trong scope Merchant | Credential secret bảo vệ; không tự cấp scope cao hơn |
| BC-165 | R02 Merchant Owner | M16 | CORE | Xem báo cáo COD, giao thành công, đơn hoàn, chi phí | Dữ liệu Merchant; không lộ tenant khác |
| BC-166 | R19 Merchant Manager | M01 | CORE | Quản lý nhân sự và quyền hạn được Owner ủy quyền | Không vượt delegated scope; audit thay đổi |
| BC-167 | R19 Merchant Manager | M02 | CORE | Điều hành cửa hàng/kho và cấu hình vận hành được giao | Không đổi Merchant owner hoặc ngân hàng COD |
| BC-168 | R19 Merchant Manager | M03 | CORE | Quản lý pickup point trong phạm vi ủy quyền | Merchant/branch scope; không truy cập tenant khác |
| BC-169 | R19 Merchant Manager | M05 | CORE | Kiểm tra Shipment cần xử lý và duyệt sửa/hủy theo chính sách | State guard; không ghi đè Shipment đã chốt trái quyền |
| BC-170 | R19 Merchant Manager | M11 | CORE | Theo dõi giao chậm, vận đơn bất thường và tiến độ | Đúng scope Merchant/branch |
| BC-171 | R19 Merchant Manager | M10 | CORE | Yêu cầu xử lý giao thất bại và giao lại | Không tạo DELIVERED/POD thay Courier |
| BC-172 | R19 Merchant Manager | M16 | CORE | Theo dõi hiệu suất giao hàng theo Merchant/đơn vị được giao | Không xem KPI Merchant khác |
| BC-173 | R19 Merchant Manager | M15 | CORE | Trao đổi với hãng chuyển phát, mở/escalate ticket | Không tự duyệt bồi thường |
| BC-174 | R03 Merchant Operator | M05 | CORE | Nhập Shipment hàng loạt từ CSV/Excel và xử lý từng dòng lỗi | Idempotency, validation, partial failure report |
| BC-175 | R03 Merchant Operator | M17 | CORE | Đồng bộ đơn từ hệ thống bán hàng trong phạm vi được cấp | Credential dùng integration principal, không chia sẻ human secret |
| BC-176 | R03 Merchant Operator | M07 | CORE | Chuẩn bị kiện và lập danh sách bàn giao cho Pickup Courier | Quét/đối chiếu kiện; không tự tạo pickup evidence |
| BC-177 | R03 Merchant Operator | M10 | CORE | Gửi yêu cầu giao lại qua workflow theo quyền | Không tự gán Courier hoặc POD |
| BC-178 | R03 Merchant Operator | M14 | CORE | Yêu cầu hoàn/thu hồi Shipment theo quyền và trạng thái | Hàng đã pickup phải đi reverse logistics |
| BC-179 | R20 Merchant Warehouse Staff | M05 | CORE | Nhận danh sách Shipment/đơn cần lấy hàng và chuẩn bị kiện | Chỉ đơn thuộc kho/nhân sự được phân công |
| BC-180 | R20 Merchant Warehouse Staff | M06 | CORE | Lấy hàng, kiểm đếm và ghi số lượng thực tế | Order, Shipment, Parcel tách biệt; có sai lệch |
| BC-181 | R20 Merchant Warehouse Staff | M06 | CORE | Đóng gói và cân đo kiện, ghi kích thước thực tế | Snapshot measurement; không tự xác nhận phí chốt |
| BC-182 | R20 Merchant Warehouse Staff | M06 | CORE | Dán nhãn, quét barcode và đối chiếu Parcel | Không tạo barcode trùng |
| BC-183 | R20 Merchant Warehouse Staff | M07 | CORE | Lập manifest/danh sách kiện bàn giao Pickup Courier | Bằng chứng số kiện, thời gian và người bàn giao |
| BC-184 | R20 Merchant Warehouse Staff | M06 | CORE | Ghi nhận thiếu/sai hàng hoặc bao bì bất thường | Evidence, lý do và escalation |
| BC-185 | R20 Merchant Warehouse Staff | M07 | CORE | Xác nhận Courier tới lấy và phối hợp bàn giao | Không tự ghi PICKED_UP thay Courier; hai phía đối chiếu |
| BC-186 | R20 Merchant Warehouse Staff | M11 | CORE | Theo dõi trạng thái kiện đã bàn giao cho hãng | Chỉ kho/đơn thuộc scope |
| BC-187 | R11 Merchant Finance | M12 | CORE | Xem COD dự kiến, đã thu, đang giữ và đã thanh toán | Không sửa dữ liệu thu COD của Courier |
| BC-188 | R11 Merchant Finance | M13 | CORE | Đối soát Merchant với bảng kê hãng và công nợ phải thu/phải trả | Ledger/statement không được sửa âm thầm |
| BC-189 | R11 Merchant Finance | M04 | CORE | Xem phí gốc, phụ phí và điều chỉnh theo Shipment/hợp đồng | Không tự thay bảng giá |
| BC-190 | R11 Merchant Finance | M13 | CORE | Kiểm tra chuyển COD thực nhận, lịch thanh toán và tải chứng từ | Không tự xác nhận hãng đã trả |
| BC-191 | R11 Merchant Finance | M15 | CORE | Mở tra soát COD thiếu, phí sai và theo dõi kết quả | Không tự phê duyệt khoản hoàn/bồi thường |
| BC-192 | R11 Merchant Finance | M16 | CORE | Xuất báo cáo tài chính theo thời gian, kho và đơn vị kinh doanh | Giới hạn Merchant/branch scope |
| BC-193 | R21 Merchant Support | M05 | CORE | Tra cứu Order/Shipment của Merchant để hỗ trợ người mua | Không xem Shipment Merchant khác |
| BC-194 | R21 Merchant Support | M11 | CORE | Theo dõi giao chậm, giao thất bại và thông báo bất thường | Chỉ dữ liệu cần cho CSKH |
| BC-195 | R21 Merchant Support | M15 | CORE | Tiếp nhận khiếu nại Recipient và tạo ticket liên kết Shipment | Xác minh đơn/nguồn phản ánh; chống ticket trùng |
| BC-196 | R21 Merchant Support | M15 | CORE | Gửi ticket sang E-Logistics Customer Service và theo dõi timeline | Có phân công, SLA, audit và bằng chứng |
| BC-197 | R21 Merchant Support | M10 | CORE | Yêu cầu giao lại và cung cấp chỉ dẫn giao có kiểm soát | Không trực tiếp ghi trạng thái giao |
| BC-198 | R21 Merchant Support | M14 | CORE | Theo dõi hoàn hàng và trao đổi kết quả với Recipient | Không thao tác custody của hãng |
| BC-199 | R21 Merchant Support | M15 | CORE | Cập nhật phản hồi Recipient và xác nhận hoàn tất phối hợp ticket | Không tự đóng ticket Logistics trái quy trình |
| BC-200 | R22 Enterprise/API Client | M01 | CORE | Xác thực integration principal và quản lý scope API gắn Merchant | Credential tách human account; rotate, revoke, audit |
| BC-201 | R22 Enterprise/API Client | M17 | CORE | Thiết lập endpoint và subscription webhook trạng thái | Tenant bound, signature, retry, duplicate handling |
| BC-202 | R22 Enterprise/API Client | M05 | CORE | Tạo và đồng bộ Shipment từ ERP/OMS/WMS/website | API idempotency key; đúng Merchant; validation |
| BC-203 | R22 Enterprise/API Client | M04 | CORE | Lấy báo giá và danh sách dịch vụ được phép qua API | Áp dụng đúng hợp đồng Merchant |
| BC-204 | R22 Enterprise/API Client | M06 | CORE | Lấy nhãn và barcode của Shipment/Parcel qua API | Chỉ tài nguyên thuộc Merchant |
| BC-205 | R22 Enterprise/API Client | M11 | CORE | Tra cứu Shipment và nhận tracking events | Hạn chế PII; filtering theo Merchant |
| BC-206 | R22 Enterprise/API Client | M17 | CORE | Đối chiếu lỗi đồng bộ, retry và failed webhook deliveries | Quan sát idempotency; không tạo bút toán/đơn trùng |

| BC-207 | R06 Dispatcher | M07 | CORE | Phân công Pickup Courier theo khu vực, năng lực và tải hiện tại | Station/region scope; chỉ assignment đủ điều kiện; chống phân công trùng |
| BC-208 | R06 Dispatcher | M10 | CORE | Phân công Delivery Courier theo khu vực, tuyến và năng lực | Kiện sẵn sàng giao, custody/scan và assignment hợp lệ |
| BC-209 | R06 Dispatcher | M16 | CORE | Giám sát tỷ lệ pickup/delivery thành công và tải công việc | KPI có định nghĩa, dữ liệu trong vùng được phân quyền |
| BC-210 | R07 Pickup Courier | M11 | CORE | Xem tuyến, thứ tự điểm lấy và địa chỉ Merchant/Retail Sender | Chỉ nhiệm vụ đang được giao; PII tối thiểu |
| BC-211 | R07 Pickup Courier | M07 | CORE | Liên hệ Merchant/Retail Sender phục vụ pickup | Chỉ trong phạm vi nhiệm vụ; log/quy tắc bảo vệ liên hệ |
| BC-212 | R07 Pickup Courier | M06 | CORE | Quét barcode và đối chiếu mã, số lượng kiện | Đúng parcel/manifest; phát hiện thiếu, trùng, lệch |
| BC-213 | R07 Pickup Courier | M06 | CORE | Kiểm tra bao bì, kích thước và thông tin kiện lúc pickup | Ghi sai lệch và evidence; không tự chốt lại phí |
| BC-214 | R07 Pickup Courier | M07 | CORE | Xác nhận thời gian và bằng chứng bàn giao nhận kiện | Assignment hợp lệ; timestamp, actor, custody; không button-only PICKED_UP |
| BC-215 | R07 Pickup Courier | M16 | CORE | Báo cáo số điểm đã ghé, số kiện lấy và nhiệm vụ thất bại | Chỉ hiệu suất cá nhân; dựa trên event và attempt |
| BC-216 | R08 Station Operator | M08 | CORE | Nhận kiện trực tiếp từ khách walk-in hoặc Courier | Phân biệt walk-in/assigned handover; actor, thời điểm, số lượng, chứng cứ |
| BC-217 | R08 Station Operator | M08 | CORE | Xác nhận kiện thực nhận theo manifest và kiểm đếm | Lệch manifest mở exception, không mặc định khớp |
| BC-218 | R08 Station Operator | M06 | CORE | Kiểm tra hàng gửi và điều kiện chấp nhận | Theo chính sách hàng cấm/hạn chế; không truy cập dữ liệu không cần thiết |
| BC-219 | R08 Station Operator | M08 | CORE | Ghi nhận tồn kho và vị trí vật lý của kiện | Station custody và location event bất biến |
| BC-220 | R08 Station Operator | M08 | CORE | Xử lý sai lệch, tồn lâu và kiện không xác định | Lý do, evidence và escalation; không âm thầm sửa kho |
| BC-221 | R08 Station Operator | M09 | CORE | Thực hiện scan bàn giao nhận/xuất kiện với chuyến xe liên Hub | Vai trò Station chỉ xác nhận custody đầu/cuối; Driver quản lý hành trình |
| BC-222 | R09 Delivery Courier | M06 | CORE | Quét barcode xác nhận nhận tải kiện từ bưu cục | Handover hai phía, đúng assignment, không nhận trùng |
| BC-223 | R09 Delivery Courier | M11 | CORE | Xem tuyến giao, bản đồ và điểm dừng | Chỉ nhiệm vụ được giao; địa chỉ và PII tối thiểu |
| BC-224 | R09 Delivery Courier | M10 | CORE | Liên hệ Recipient theo chính sách bảo vệ số điện thoại | Chỉ khi assignment hợp lệ; bảo vệ thông tin liên hệ |
| BC-225 | R09 Delivery Courier | M12 | CORE | Xác nhận số COD cần thu và phương thức thanh toán | Chỉ đọc giá trị COD đã chốt; không tự đổi tiền |
| BC-226 | R09 Delivery Courier | M10 | CORE | Thực hiện giao lại theo assignment mới | Delivery attempt riêng; không tái sử dụng assignment hết hiệu lực |
| BC-227 | R09 Delivery Courier | M10 | CORE | Ghi nhận bằng chứng và tăng số lần giao thất bại | Lý do, timestamp, evidence, attempt ID độc lập; không xóa history |
| BC-228 | R09 Delivery Courier | M08 | CORE | Bàn giao kiện không giao được về Station/Hub | Custody giao hai phía, xác nhận thực nhận |
| BC-229 | R09 Delivery Courier | M16 | CORE | Xem KPI số kiện giao, tỷ lệ thành công và COD còn phải nộp | Chỉ dữ liệu cá nhân; COD chưa nộp không được coi là remitted |
| BC-230 | R16 Station Manager | M01 | CORE | Quản lý ca làm và nhân sự Station trong quyền được ủy quyền | Không tự cấp global/system admin hoặc quyền ngoài Station |
| BC-231 | R16 Station Manager | M09 | CORE | Giám sát các chuyến xe đi/đến và bàn giao kiện | Không làm Driver giả, giám sát manifest/custody |
| BC-232 | R16 Station Manager | M08 | CORE | Xử lý kiện tồn lâu, sai tuyến, mất/hỏng qua workflow | Case, bằng chứng, phê duyệt khi cần |
| BC-233 | R16 Station Manager | M12 | CORE | Giám sát Courier nộp COD tại Station | Cashier xác nhận tiền; manager không sửa ledger |
| BC-234 | R23 Line-haul Driver | M09 | CORE | Nhận chuyến xe được phân công | Chỉ trip hợp lệ theo tuyến/driver, thời hạn |
| BC-235 | R23 Line-haul Driver | M09 | CORE | Kiểm tra seal, tải trọng và trạng thái phương tiện | Ghi checklist/evidence; bất thường phải xử lý trước departure |
| BC-236 | R23 Line-haul Driver | M08 | CORE | Nhận manifest kiện, bao tải và thông tin số lượng | Manifest phiên bản, cargo/trip relation rõ ràng |
| BC-237 | R23 Line-haul Driver | M06 | CORE | Quét barcode và đối chiếu kiện/bao khi nhận tải | Chặn kiện trùng/sai manifest; ghi discrepancy |
| BC-238 | R23 Line-haul Driver | M08 | CORE | Xác nhận bàn giao kiện tại điểm xuất phát | Hai phía custody; timestamp, station/hub, actor |
| BC-239 | R23 Line-haul Driver | M09 | CORE | Ghi nhận hành trình và các mốc vận tải | Trip events độc lập với shipment delivered |
| BC-240 | R23 Line-haul Driver | M09 | CORE | Báo trễ, sự cố hoặc thay đổi hành trình | Lý do/evidence, escalation, SLA |
| BC-241 | R23 Line-haul Driver | M09 | CORE | Xác nhận đến Hub/Station đích | Đúng trip/destination, thời gian và trạng thái |
| BC-242 | R23 Line-haul Driver | M08 | CORE | Bàn giao và đối chiếu số kiện thực nhận ở đích | Đối chiếu manifest và bên nhận, tạo discrepancy khi lệch |
| BC-243 | R23 Line-haul Driver | M18 | CORE | Không sửa đơn Merchant hoặc ghi DELIVERED thay Courier | Negative authorization cho commercial state/POD |
| BC-244 | R24 Courier Team Leader | M07 | CORE | Xem nhiệm vụ Pickup Courier toàn đội | Chỉ đội/ca được giao |
| BC-245 | R24 Courier Team Leader | M10 | CORE | Xem nhiệm vụ Delivery Courier toàn đội | Không tự có quyền giao thay Courier |
| BC-246 | R24 Courier Team Leader | M16 | CORE | Theo dõi hiệu suất và năng suất đội giao nhận | Dữ liệu đúng team/scope và nguồn sự kiện |
| BC-247 | R24 Courier Team Leader | M07 | CORE | Đề xuất tái phân công pickup | Approval/authorization tách biệt với hành động reassignment |
| BC-248 | R24 Courier Team Leader | M10 | CORE | Đề xuất tái phân công delivery | Không tự cấp assignment rồi tự ghi POD |
| BC-249 | R24 Courier Team Leader | M15 | CORE | Hỗ trợ sự cố giao nhận và escalations | Ghi lý do, liên kết ticket/incident |
| BC-250 | R24 Courier Team Leader | M08 | CORE | Kiểm tra kiện tồn và bàn giao bất thường | Chỉ vị trí/đội được phân công, có đối chiếu custody |
| BC-251 | R24 Courier Team Leader | M08 | CORE | Xác nhận chênh lệch bàn giao theo quyền | Evidence, audit, phê duyệt riêng với mất mát |
| BC-252 | R24 Courier Team Leader | M18 | CORE | Thao tác thay đổi assignment/chấp nhận tổn thất cần quyền riêng | Không mặc định có privilege đặc biệt |
| BC-253 | R25 Sorting Staff | M06 | CORE | Quét barcode kiện trong công đoạn phân loại | Scan event truy vết người/máy/thời điểm |
| BC-254 | R25 Sorting Staff | M08 | CORE | Phân tuyến theo đích và quy tắc vận hành | Đúng scope Hub, route plan hợp lệ |
| BC-255 | R25 Sorting Staff | M08 | CORE | Gom kiện vào bao/túi/đơn vị xử lý | Parent-child manifest và số lượng kiểm soát |
| BC-256 | R25 Sorting Staff | M08 | CORE | Xác nhận kiện đã qua dây chuyền phân loại | Không giả lập scan; có evidence |
| BC-257 | R25 Sorting Staff | M08 | CORE | Ghi nhận kiện sai tuyến | Exception, redirect plan, không xóa lịch sử |
| BC-258 | R25 Sorting Staff | M06 | CORE | Ghi nhận barcode không đọc được | Case xử lý định danh thay thế có kiểm soát |
| BC-259 | R25 Sorting Staff | M08 | CORE | Ghi nhận kiện hư hỏng khi phân loại | Ảnh/biên bản, custody, escalation |
| BC-260 | R25 Sorting Staff | M09 | CORE | Chuyển kiện/bao vào tuyến vận tải phù hợp | Manifest/tuyến/đích khớp, không tự tạo trip quyền driver |
| BC-261 | R25 Sorting Staff | M18 | CORE | Hạn chế PII và dữ liệu tài chính Merchant | Least privilege, test deny access |
| BC-262 | R26 Hub Supervisor / Warehouse Manager | M08 | CORE | Giám sát năng lực xử lý của Hub | Chỉ hub thuộc quyền; capacity measurable |
| BC-263 | R26 Hub Supervisor / Warehouse Manager | M08 | CORE | Theo dõi tồn kiện theo vị trí/thời gian | Event custody và inventory reconcile |
| BC-264 | R26 Hub Supervisor / Warehouse Manager | M09 | CORE | Theo dõi chuyến xe đến/đi Hub | Trip + manifest + ETA/delay consistency |
| BC-265 | R26 Hub Supervisor / Warehouse Manager | M08 | CORE | Đối chiếu manifest và xử lý chênh lệch | Không sửa âm thầm, chứng cứ/approval |
| BC-266 | R26 Hub Supervisor / Warehouse Manager | M15 | CORE | Tạo/điều phối báo cáo mất hoặc hỏng kiện | Liên kết kiện và vị trí cuối, ticket/claim |
| BC-267 | R26 Hub Supervisor / Warehouse Manager | M09 | CORE | Phối hợp điều chuyển hàng và ngoại lệ vận tải | Theo policy/coverage/capacity, audit |
| BC-268 | R26 Hub Supervisor / Warehouse Manager | M16 | CORE | Theo dõi KPI, throughput và thời gian tồn Hub | Chỉ số định nghĩa và provenance |
| BC-269 | R26 Hub Supervisor / Warehouse Manager | M18 | CORE | Phê duyệt điều chỉnh tồn theo thẩm quyền | Reason/evidence/separation of duties, không xóa custody |
| BC-270 | R27 Network Operations Manager | M08 | CORE | Quản lý mạng lưới Station và Hub theo vùng/toàn mạng | Đúng scope, không tự vượt quyền identity |
| BC-271 | R27 Network Operations Manager | M03 | CORE | Quản lý vùng giao nhận và coverage | Versioning/policy, không hồi tố đơn đã chốt tùy ý |
| BC-272 | R27 Network Operations Manager | M09 | CORE | Giám sát năng lực tuyến và vận tải | Dữ liệu theo network region |
| BC-273 | R27 Network Operations Manager | M09 | CORE | Điều chuyển hàng liên vùng | Phê duyệt theo policy, manifest/custody liên tục |
| BC-274 | R27 Network Operations Manager | M10 | CORE | Theo dõi tỷ lệ giao thành công toàn mạng | KPI từ attempts/tracking, không sửa POD |
| BC-275 | R27 Network Operations Manager | M16 | CORE | Theo dõi SLA, cảnh báo và năng suất mạng lưới | Định nghĩa thống nhất, phân quyền dữ liệu |
| BC-276 | R27 Network Operations Manager | M09 | CORE | Điều chỉnh tuyến ngoại lệ theo chính sách | Reason/evidence/approval/audit |
| BC-277 | R27 Network Operations Manager | M07 | CORE | Giám sát năng lực pickup giữa các khu vực | Không tự tạo chứng cứ PICKED_UP |
| BC-278 | R27 Network Operations Manager | M10 | CORE | Điều phối chuyển vùng phát trong ngoại lệ | Tôn trọng state, active assignment và custody |
| BC-279 | R27 Network Operations Manager | M18 | CORE | Audit quyết định chuyển tuyến và ngoại lệ | Chỉ quyền được ủy quyền, không xóa log |
| BC-280 | R27 Network Operations Manager | M12 | CORE | Không mặc nhiên được đổi COD, thanh toán hoặc cấu hình bảo mật | Negative permission tests, separation of duties |

| BC-281 | R12 COD Cashier | M12 | CORE | Kiểm đếm COD thực nhận theo ca và từng Courier | Xác nhận song phương, denomination/count và số tiền đã chốt |
| BC-282 | R12 COD Cashier | M12 | CORE | Đóng ca và đối chiếu quỹ COD thực tế | Biên bản đóng ca, thiếu/thừa và người chịu trách nhiệm |
| BC-283 | R12 COD Cashier | M13 | CORE | Chuyển dữ liệu nộp tiền đã xác nhận sang đối soát | Không tự ghi RECONCILED hay SETTLED |
| BC-284 | R12 COD Cashier | M08 | CORE | Ràng buộc điểm thu quỹ với Station/ca | Chỉ Station được giao, actor và thời gian có chứng cứ |
| BC-285 | R13 Finance Operator | M12 | CORE | Đối chiếu Delivered Shipment với COD thu thực tế | Không suy luận COD từ DELIVERED; phát hiện ngoại lệ |
| BC-286 | R13 Finance Operator | M13 | CORE | Quản lý công nợ phải thu và phải trả | Ledger entries append-only; tính toán có căn cứ |
| BC-287 | R13 Finance Operator | M13 | CORE | Xác định COD đủ điều kiện trả Merchant | Theo hold, fees, disputes, policy, approvals |
| BC-288 | R13 Finance Operator | M13 | CORE | Thực hiện hoàn tiền đã được phê duyệt | Idempotency, kiểm soát hai người khi cần |
| BC-289 | R13 Finance Operator | M13 | CORE | Phát hiện và từ chối thanh toán hoặc refund trùng | Idempotency key, unique reference và reconciliation |
| BC-290 | R13 Finance Operator | M04 | CORE | Đối chiếu giá chốt, phí điều chỉnh sau cân đo | Snapshot + phiên bản rate card; audit sửa phí |
| BC-291 | R13 Finance Operator | M16 | CORE | Xuất báo cáo công nợ và COD tồn chưa đối soát | Theo kỳ, có nguồn bút toán |
| BC-292 | R14 Customer Service Agent | M05 | CORE | Tra cứu Shipment theo ticket/phạm vi phân công | PII tối thiểu, reason và access audit |
| BC-293 | R14 Customer Service Agent | M11 | CORE | Xử lý yêu cầu giao chậm qua điểm quét cuối | Không giả lập tracking event |
| BC-294 | R14 Customer Service Agent | M03 | CORE | Tiếp nhận yêu cầu đổi địa chỉ hoặc số liên lạc | Xác minh sở hữu; phê duyệt thay đổi theo state |
| BC-295 | R14 Customer Service Agent | M12 | CORE | Tiếp nhận tra soát COD và chuyển Finance | Không tự sửa cod_amount |
| BC-296 | R14 Customer Service Agent | M16 | CORE | Giám sát deadline phản hồi và giải quyết ticket | SLA clock, escalation, audit |
| BC-297 | R15 Claims Officer | M15 | CORE | Điều tra mất/hỏng/thiếu/giao nhầm và tranh chấp POD | Đối chiếu evidence, tracking, custody và trách nhiệm |
| BC-298 | R15 Claims Officer | M08 | CORE | Truy xuất custody và biên bản bàn giao phục vụ claim | Chỉ case scope; không sửa sự kiện |
| BC-299 | R15 Claims Officer | M10 | CORE | Đối chiếu delivery attempts và bằng chứng POD | Chứng cứ nguồn gốc; xử lý tranh chấp |
| BC-300 | R15 Claims Officer | M12 | CORE | Điều tra thu sai COD liên quan claim | Không tự tạo điều chỉnh tài chính |
| BC-301 | R15 Claims Officer | M15 | CORE | Đánh giá chính sách, hạn mức và đề xuất số bồi thường | Separation of duties và reason |
| BC-302 | R15 Claims Officer | M15 | CORE | Chuyển hồ sơ đề xuất sang người phê duyệt | Không tự phê duyệt chi trả khi không có quyền |
| BC-303 | R15 Claims Officer | M13 | CORE | Theo dõi thanh toán bồi thường/refund đã phê duyệt | Không giả lập payment confirmation |
| BC-304 | R04 Business Admin | M04 | CORE | Quản lý dịch vụ và chính sách nghiệp vụ theo quyền | Giá đặc biệt cần approval riêng |
| BC-305 | R04 Business Admin | M08 | CORE | Quản trị cơ cấu Station/Hub/khu vực theo phạm vi | Không sửa custody đã xảy ra |
| BC-306 | R04 Business Admin | M01 | CORE | Phân công vai trò nghiệp vụ nhân sự theo ủy quyền | Không tự cấp admin bảo mật/Identity |
| BC-307 | R04 Business Admin | M15 | CORE | Phê duyệt escalation nghiệp vụ theo hạn mức | Separation of duties, lý do và audit |
| BC-308 | R04 Business Admin | M16 | CORE | Xem báo cáo doanh thu, sản lượng, chất lượng dịch vụ | Theo tổ chức/vùng được cấp phép |
| BC-309 | R05 Identity Admin | M01 | CORE | Provisioning tài khoản theo workflow được duyệt | Không tự cấp quyền nghiệp vụ Merchant/COD |
| BC-310 | R05 Identity Admin | M01 | CORE | Quản trị Realm và policy Identity trong phạm vi | Hạn chế superuser và audit config |
| BC-311 | R05 Identity Admin | M01 | CORE | Xử lý credential incident và thu hồi credential | Incident reference, forced rotation/revocation |
| BC-312 | R05 Identity Admin | M01 | CORE | Quản lý MFA và session revocation theo policy | Không bypass các security gates |
| BC-313 | R05 Identity Admin | M18 | CORE | Giám sát thay đổi quyền và sự kiện Identity | Chỉ phạm vi bảo mật, append-only audit |
| BC-314 | R17 Auditor | M18 | CORE | Xem lịch sử thay đổi quyền và quản trị | Read-only, scoped, immutable source |
| BC-315 | R17 Auditor | M11 | CORE | Đọc tracking events để kiểm toán vận chuyển | Không sửa trạng thái/attempt |
| BC-316 | R17 Auditor | M13 | CORE | Đọc ledger và lịch sử điều chỉnh tài chính | PII redaction và scope; read-only |
| BC-317 | R17 Auditor | M12 | CORE | Đọc chuỗi thu-nộp-đối soát COD | Không ghi remittance |
| BC-318 | R17 Auditor | M15 | CORE | Đọc bằng chứng claim và phê duyệt | Không thay đổi outcome |
| BC-319 | R28 Pricing / Billing Administrator | M04 | CORE | Cấu hình rate card theo vùng và tuyến | Version, effective date, kiểm soát hồi tố |
| BC-320 | R28 Pricing / Billing Administrator | M04 | CORE | Quy tắc trọng lượng tính cước và đo kích thước | Khối lượng quy đổi/cân thực tế theo policy |
| BC-321 | R28 Pricing / Billing Administrator | M04 | CORE | Quản trị loại dịch vụ vận chuyển | Service availability và vùng phục vụ |
| BC-322 | R28 Pricing / Billing Administrator | M04 | CORE | Cấu hình phí COD và các phụ phí | Min/max/tiers rõ ràng, audit |
| BC-323 | R28 Pricing / Billing Administrator | M04 | CORE | Áp dụng biểu phí riêng theo hợp đồng Merchant | Chỉ hợp đồng được phê duyệt và còn hiệu lực |
| BC-324 | R28 Pricing / Billing Administrator | M04 | CORE | Quản lý chiết khấu và khuyến mãi | Điều kiện áp dụng, phê duyệt và hạn sử dụng |
| BC-325 | R28 Pricing / Billing Administrator | M04 | CORE | Quản trị thời điểm hiệu lực bảng giá | Không tự hồi tố đơn đã chốt |
| BC-326 | R28 Pricing / Billing Administrator | M04 | CORE | Phân biệt giá ước tính, giá chốt và phí điều chỉnh | Lưu price snapshot + adjustment trail |
| BC-327 | R28 Pricing / Billing Administrator | M13 | CORE | Cung cấp dữ liệu cước cho hóa đơn và đối soát | Không trực tiếp sửa ledger/số dư |
| BC-328 | R28 Pricing / Billing Administrator | M18 | CORE | Audit sửa bảng giá, phê duyệt và phiên bản | Không xóa price history |
| BC-329 | R29 Sales / Key Account Manager | M02 | CORE | Quản lý Merchant/account được giao | Không truy cập Merchant ngoài danh sách |
| BC-330 | R29 Sales / Key Account Manager | M02 | CORE | Tư vấn dịch vụ và ghi nhận tương tác khách hàng | Không đọc PII Shipment không cần thiết |
| BC-331 | R29 Sales / Key Account Manager | M02 | CORE | Theo dõi hợp đồng Merchant và gia hạn | Quyền xem theo scope, tài liệu versioned |
| BC-332 | R29 Sales / Key Account Manager | M04 | CORE | Đề xuất bảng giá hoặc chiết khấu đặc biệt | Không tự phê duyệt hoặc áp dụng giá |
| BC-333 | R29 Sales / Key Account Manager | M16 | CORE | Theo dõi KPI và chỉ tiêu kinh doanh | Dữ liệu theo account/region scope |
| BC-334 | R29 Sales / Key Account Manager | M15 | CORE | Phối hợp hỗ trợ Merchant lớn | Ticket ownership và escalation |
| BC-335 | R29 Sales / Key Account Manager | M17 | CORE | Phối hợp onboarding và integration Merchant | Không tự xem/cấp mọi API secrets |
| BC-336 | R29 Sales / Key Account Manager | M18 | CORE | Audit đề xuất giá và thay đổi quan hệ hợp đồng | Approval trace |
| BC-337 | R30 Risk / Fraud / Compliance Officer | M12 | CORE | Điều tra dấu hiệu gian lận COD | Read theo case và dấu vết tiền |
| BC-338 | R30 Risk / Fraud / Compliance Officer | M01 | CORE | Phát hiện và điều tra tài khoản sử dụng trái phép | Không tự cấp quyền mới |
| BC-339 | R30 Risk / Fraud / Compliance Officer | M05 | CORE | Phát hiện spam hoặc tạo Shipment hàng loạt bất thường | Rate, fingerprint, scope, false positives |
| BC-340 | R30 Risk / Fraud / Compliance Officer | M05 | CORE | Kiểm tra thông tin Recipient có dấu hiệu giả | PII minimization, authorization |
| BC-341 | R30 Risk / Fraud / Compliance Officer | M08 | CORE | Đánh dấu kiện có dấu hiệu rủi ro | Case, evidence và custody không bị sửa |
| BC-342 | R30 Risk / Fraud / Compliance Officer | M18 | CORE | Điều tra truy cập dữ liệu trái phép | Audit bất biến, nguyên tắc tối thiểu |
| BC-343 | R30 Risk / Fraud / Compliance Officer | M05 | CORE | Đề xuất tạm giữ Shipment theo chính sách | Không thay đổi silent status |
| BC-344 | R30 Risk / Fraud / Compliance Officer | M15 | CORE | Chuyển hồ sơ rủi ro sang thẩm định/phê duyệt | Separation of duties |
| BC-345 | R30 Risk / Fraud / Compliance Officer | M05 | CORE | Phê duyệt giải phóng Shipment theo thẩm quyền | Dual control/hạn mức khi cần |
| BC-346 | R30 Risk / Fraud / Compliance Officer | M18 | CORE | Xem audit và giao dịch bất thường theo case | Mọi lần truy cập nhạy cảm có log |
| BC-347 | R30 Risk / Fraud / Compliance Officer | M13 | CORE | Điều tra giao dịch tài chính bất thường | Không tự sửa ledger |
| BC-348 | R31 Platform Super Admin | M01 | CORE | Quản lý tenant và quyền quản trị nền tảng cấp cao | Privilege boundary, approval và session assurance |
| BC-349 | R31 Platform Super Admin | M02 | CORE | Cấu hình phân vùng tổ chức ở cấp nền tảng | Không tự có quyền sở hữu Merchant |
| BC-350 | R31 Platform Super Admin | M18 | CORE | Quản lý feature flags theo kiểm soát thay đổi | Audit, rollback và rollout scope |
| BC-351 | R31 Platform Super Admin | M18 | CORE | Quản lý chính sách nền tảng được phân quyền | Policy versioning và phê duyệt |
| BC-352 | R31 Platform Super Admin | M18 | CORE | Phê duyệt tác vụ quản trị đặc biệt theo quy trình | Dual approval khi policy yêu cầu |
| BC-353 | R31 Platform Super Admin | M18 | CORE | Giám sát thao tác đặc quyền và break-glass | Reason, expiry, immutable audit |
| BC-354 | R31 Platform Super Admin | M12 | CORE | Không được giả xác nhận thu/đối soát/thanh toán COD | Negative permission tests |
| BC-355 | R31 Platform Super Admin | M10 | CORE | Không được giả POD/DELIVERED | Không bypass chứng cứ dù là Super Admin |
| BC-356 | R32 IT / Integration Operations | M16 | CORE | Kiểm tra sức khỏe hệ thống và tín hiệu vận hành | Chỉ telemetry cần thiết |
| BC-357 | R32 IT / Integration Operations | M17 | CORE | Điều tra lỗi webhook và lịch sử retry | Idempotent retries; không tạo duplicate state |
| BC-358 | R32 IT / Integration Operations | M17 | CORE | Quản lý cấu hình tích hợp theo quyền kỹ thuật | Tách secret và business action |
| BC-359 | R32 IT / Integration Operations | M16 | CORE | Giám sát hàng đợi, dead-letter và backlog | Có hạn mức retry và cảnh báo |
| BC-360 | R32 IT / Integration Operations | M05 | CORE | Đối chiếu lỗi đồng bộ Shipment | Không sửa nghiệp vụ vượt quyền |
| BC-361 | R32 IT / Integration Operations | M18 | CORE | Vận hành production có kiểm soát | Approval, least privilege, audit |
| BC-362 | R32 IT / Integration Operations | M01 | CORE | Không impersonate Merchant tùy ý | Session/account controls và evidence |
| BC-363 | R32 IT / Integration Operations | M11 | CORE | Khắc phục lỗi phát thông báo/tracking event | Không giả mạo sự kiện giao hàng |
| BC-364 | R33 Partner / Franchise Operator | M05 | CORE | Tiếp nhận/gửi Shipment tại quầy đối tác được chỉ định | Partner+Station scope, xác minh walk-in |
| BC-365 | R33 Partner / Franchise Operator | M08 | CORE | Bàn giao kiện cho hãng vận chuyển | Actor, count, manifest, timestamp, custody proof |
| BC-366 | R33 Partner / Franchise Operator | M08 | CORE | Xác nhận số lượng thực giao/nhận | Sai lệch phải ghi event và investigation |
| BC-367 | R33 Partner / Franchise Operator | M08 | CORE | Quản lý tồn kiện ở điểm đại lý | Không đọc tồn nơi khác |
| BC-368 | R33 Partner / Franchise Operator | M13 | CORE | Xem doanh thu dịch vụ thuộc điểm được giao | Chỉ báo cáo được phép; không sửa settlement |
| BC-369 | R33 Partner / Franchise Operator | M17 | CORE | Ràng buộc Partner Organization với Station | Explicit membership/scope; no network-wide access |
| BC-370 | R33 Partner / Franchise Operator | M06 | CORE | Quét/kiểm tra nhãn và tình trạng kiện | Không giả mạo scan của hãng |
| BC-371 | R33 Partner / Franchise Operator | M18 | CORE | Truy vết nghiệp vụ điểm đối tác | Không xóa handover history |

## 5. Phân công theo mốc sản phẩm

| Mốc | Năng lực phải chứng minh | Role/Actor điển hình |
|---|---|---|
| M1 Identity & Sender/Merchant Foundation | Đăng ký Retail Sender, hồ sơ/địa chỉ cá nhân; đăng ký Merchant, membership/scope, pickup point, public Guest capabilities | Retail Sender, Guest, Owner, Manager, Operator, Identity Admin, Business Admin |
| M2 Shipment Creation | Báo giá; Retail Sender và Merchant tạo Shipment; Station nhận walk-in có biên nhận/custody; thanh toán gửi lẻ | Retail Sender, Owner, Manager, Operator, Warehouse Staff, Enterprise/API Client, Station Operator, Guest (chỉ public quote) |
| M3 Pickup, Hub, Line-haul & Delivery | Assignment, pickup scan, Station custody, sorting, manifests, line-haul trips, Hub transfer, attempts, POD, tracking | Dispatcher, Pickup Courier, Courier Team Leader, Station Operator, Sorting Staff, Hub Supervisor, Line-haul Driver, Delivery Courier, Network Operations Manager, Station Manager, Recipient |
| M4 COD & Exceptions | Thu/nộp COD, reconciliation/ledger, returns, ticket/claim | Delivery Courier, COD Cashier, Finance Operator, Merchant Finance, Merchant Support, CS, Claims, Pricing Admin, Risk Officer, Auditor |

## 6. Acceptance gates tối thiểu dành cho AI reviewer

1. **Authorization:** Actor + role permission + organization + station/assignment + resource + state + security condition; kiểm thử cross-tenant/cross-assignment. Không dùng frontend hidden controls thay backend authorization.
2. **Shipment vs Parcel vs Order:** Có quan hệ, snapshot và trạng thái riêng; không đánh đồng Order ID với tracking ID.
3. **Custody:** Mọi nhận/bàn giao kiện phải truy vết ai, ở đâu, lúc nào, từ ai, bằng chứng gì.
4. **Delivery:** `PICKED_UP`, `DELIVERED`, `DELIVERY_FAILED` phải có assignment và evidence; delivery attempts không bị ghi đè.
5. **COD:** Collected → Remitted → Reconciled → Settled là các bước riêng; ledger/idempotency; không sửa âm thầm số dư/COD.
6. **Recipient privacy:** Không mở Portal/Identity; tracking công khai tối thiểu, xác minh thao tác nhạy cảm, bảo vệ PII.
7. **Tickets/Claims:** Ticket có nguồn (Merchant hoặc Retail Sender trực tiếp; Recipient đơn Merchant qua Merchant Support), Shipment, trạng thái, ownership, timeline và evidence; claim phê duyệt độc lập khi có bồi thường.
8. **No false completeness:** Mỗi hàng BC chỉ đổi trạng thái sau khi có evidence từ SDD + implementation + tests. Phân loại thiếu thành GAP, DEFERRED_BY_DESIGN, hoặc NOT_APPLICABLE.
9. **Guest/Walk-in barrier:** Guest không có public API tạo Shipment; khách không đăng ký chỉ gửi tại điểm giao dịch thông qua Station Operator và phải có biên nhận, custody, thu phí, xác minh.
10. **Retail Sender isolation:** Cá nhân chỉ truy cập Shipment/hồ sơ/địa chỉ/chứng từ của mình; khác Merchant organization membership. Phải có cả luồng đăng ký và vận chuyển từ đầu đến hoàn/hỗ trợ.
11. **Integration target:** API Client/Partner Integration là CORE trong target scope Nhóm B; có thể thực hiện sau M2 nhưng không đánh dấu hoàn chỉnh Nhóm B trước khi xác minh tenant-bound credentials, quote/shipment/labels/webhooks và idempotency. Transportation/Line-haul thuộc Target FULL Nhóm C–D và có thể triển khai theo giai đoạn, không được loại khỏi target.

12. **Merchant segregation of duties:** Manager chỉ quyền ủy quyền; Warehouse không tự ghi custody của Courier; Support không sửa POD/COD; Finance không ghi nhận hãng đã chuyển tiền; integration principal không tự cấp privilege.

13. **Nhóm C–D Target FULL:** mọi nhiệm vụ pickup/delivery, line-haul, phân loại, manifest, Hub/Station, giám sát mạng lưới là CORE ở target; giữ Assignment ≠ Custody ≠ Tracking ≠ COD. Đánh giá từng capability bằng SDD/API/DB/UI/tests, không gộp nhiều bằng chứng thành một nút trạng thái.

14. **Nhóm E–F FULL:** có ledger, idempotency, phê duyệt riêng, price snapshot, hợp đồng và scope, giám sát gian lận, IT Operations, audit read-only, franchise partner isolation; kiểm thử negative permission kể cả Super Admin.

## 7. Đề xuất các cột dùng khi review code/SDD

`Capability ID | SDD Feature/Requirement ID | Role | Module | Action | Data Scope | Allowed States | Preconditions | Evidence | API | DB entities | Frontend | Automated tests | Status (NOT_STARTED/PARTIAL/IMPLEMENTED/VERIFIED/BLOCKED) | Gap/Risk | Owner Decision`

Không điền các cột evidence bằng suy đoán. Đặc biệt, **mã permission, tên bảng, tuyến API và trạng thái chính thức** phải lấy từ canonical SDD, không tự đặt theo file này.

## 8. Change log phạm vi Nhóm A — 2026-10-26

- Giữ nguyên ID R01–R17 và BC-001–BC-130 để bảo toàn tham chiếu; thêm **R18 Retail Sender (P0)** và **BC-131–BC-156**.
- Nâng BC-004, BC-005 lên CORE; BC-006 trở thành **hành vi bị cấm** thay vì tính năng đợi triển khai; thêm Guest service catalog và đăng ký cá nhân và application Merchant sau đăng nhập.
- Recipient là external actor, không account/portal, khiếu nại đơn Merchant qua Merchant Support. Trường hợp đơn Retail Sender có CSKH hãng tiếp nhận, không buộc qua Merchant.
- Ký gửi walk-in tại quầy là quy trình do Station Operator thực hiện, không phải Guest online shipment.
- Các nhãn CORE là **phạm vi mục tiêu bắt buộc** mà Project Owner yêu cầu, không phải bằng chứng Feature đã build hoặc canonical SDD đã phê duyệt.

## 9. Change log phạm vi Nhóm B — 2026-10-26

- **Chủ trương:** toàn bộ nghiệp vụ tham khảo của 7 chủ thể Nhóm B trở thành Target FULL, không có nghĩa code hiện tại đã hoàn thành.
- Giữ nguyên `R01–R18`, `BC-001–BC-156`; thêm `R19 Merchant Manager`, `R20 Merchant Warehouse Staff`, `R21 Merchant Support`, `R22 Enterprise/API Client` (integration principal, không phải human login).
- Thêm `BC-157–BC-206`; các capability bổ sung bao gồm ủy quyền nhân sự/chi nhánh, kho Merchant, tiếp nhận khiếu nại Recipient, tài chính Merchant và tích hợp ERP/OMS/WMS.
- Chuyển các capability Owner/Operator/Finance liên quan từ EXPAND/DEFER thành CORE theo Target FULL; **không đồng nhất CORE với hoàn tất Feature hiện hành hoặc quyền đã cấp**.
- Khả năng tạo ticket tạm thời của Owner/Operator không xóa bỏ Role Merchant Support mục tiêu. Merchant Support chỉ xử lý Shipment thuộc Merchant; Recipient không có tài khoản E-Logistics.
- Các năng lực tích hợp phải có scope riêng và có thể triển khai theo đợt, nhưng không thể đánh dấu Nhóm B hoàn chỉnh nếu chưa có bằng chứng.

### Coverage và acceptance cho Nhóm B

| Role/principal | Mục tiêu FULL | Kiểm tra bắt buộc |
|---|---|---|
| Owner | Hồ sơ pháp nhân; nhân sự; pickup point; shipment đơn/hàng loạt; giá; COD/billing; claims; tích hợp; báo cáo | Isolation Merchant; ủy quyền; audit; secrets được bảo vệ |
| Manager | Nhân sự ủy quyền; điểm lấy; giám sát/duyệt sửa hủy; giao lại; hiệu suất; CS | Không đổi tài khoản ngân hàng COD hoặc chủ sở hữu mặc định |
| Operator | Tạo/import/sync; địa chỉ; báo giá; xác nhận; nhãn; bàn giao; tracking; ngoại lệ; xem COD | Không sửa COD đã khóa, xóa lịch sử hay duyệt bồi thường |
| Warehouse Staff | Pick/pack/count/measure/label; thiếu/sai hàng; manifest; bàn giao | Order ≠ Shipment ≠ Parcel; không giả mạo `PICKED_UP` |
| Merchant Finance | COD, cước, công nợ, báo cáo, chứng từ, tra soát và đối chiếu | Không tự chỉnh status delivery hoặc bút toán hãng |
| Merchant Support | Tracking, tiếp nhận Recipient, ticket/escalation, giao lại, hoàn | Scope Merchant, không sửa tracking/POD/COD |
| Enterprise/API Client | API create/sync/quote/label/tracking/webhooks/reconciliation | Merchant-bound credential/scope, idempotency, webhook signature/retry |

**Thực thi:** Mapping cụ thể Role→Permission→API→DB→UI→Test cần đối chiếu canonical SDD; mỗi capability phải có owner, trạng thái và bằng chứng độc lập.

## 10. Change log mở rộng FULL Nhóm C–D — 2026-10-26

- **Định hướng Project Owner:** 4 Role Nhóm C và 6 Role Nhóm D đều thuộc Target FULL, bảo toàn quyết định Nhóm A/B. P0/P1/P2 là thứ tự triển khai, không phải cắt bỏ trách nhiệm.
- **Giữ ID cũ:** `R01–R22`, `BC-001–BC-206`; bổ sung `R23 Line-haul Driver`, `R24 Courier Team Leader`, `R25 Sorting Staff`, `R26 Hub Supervisor / Warehouse Manager`, `R27 Network Operations Manager` và `BC-207–BC-280`.
- **Giữ vai trò hiện có:** Dispatcher `R06`, Pickup Courier `R07`, Station Operator/Station Clerk `R08`, Delivery Courier `R09`, Station Manager `R16`. Thêm capability cho các nghiệp vụ trước đây thiếu chi tiết; chuyển `BC-053/054/055/061/062/069/079/122/123` sang CORE (target) và cập nhật `BC-070` để phân định trách nhiệm bàn giao Station với nhiệm vụ Line-haul Driver.
- **Bảo toàn mục tiêu Nhóm A/B:** Guest public không tạo Shipment online; Retail Sender account cá nhân FULL; Recipient external không Portal; walk-in chỉ nhân viên Station tạo tại quầy; 7 Role/principal Merchant là FULL, isolation tenant, Merchant Support tiếp nhận phản ánh Recipient.
- **Chống nhập nhằng:** Một người có thể làm cả Pickup/Delivery, nhưng assignment và chứng cứ độc lập. Không ghi `PICKED_UP`/`DELIVERED` bằng nút bấm thiếu điều kiện; không gộp vận chuyển kiện với custody tiền COD; Line-haul Driver không có quyền delivery; Sorting không xem tài chính Merchant.
- **Phân kỳ:** Nhóm C/D có thể triển khai theo nhiều Feature, nhưng để xác nhận `FULL` phải kiểm thử cả happy path, failed pickup/delivery, sai tuyến, chênh lệch manifest, kiện hỏng/mất, return custody và phê duyệt ngoại lệ.
- **Cấp hợp đồng:** Đây vẫn là Business Target Proposal; Role ID, API, bảng DB, workflow trạng thái chỉ có hiệu lực triển khai sau khi đối chiếu canonical SDD. Không ghi `VERIFIED` khi chưa có evidence.

### Kiểm soát nghiệm thu Nhóm C–D

| Role | Bao phủ bắt buộc | Ngăn vượt quyền |
|---|---|---|
| Pickup Courier | Assignment, tuyến, liên hệ, check-in, scan, kiểm tra, pickup evidence/failure, handover, KPI | Không pickup không chứng cứ/không assignment |
| Delivery Courier | Nhận tải, tuyến/liên hệ, OFD, COD, POD, failure, retry, trả kiện, nộp COD, KPI | Không sửa COD/POD/tiền nộp trái quyền |
| Line-haul Driver | Trip, manifest, seal/tải trọng, scan, hành trình, trễ/sự cố, đến đích, đối chiếu bàn giao | Không sửa đơn Merchant hoặc giao tận tay |
| Courier Team Leader | Giám sát nhiệm vụ/hiệu suất, đề xuất phân công, sự cố, tồn kiện, chênh lệch | Không tự phê duyệt mọi reassignment/mất kiện |
| Dispatcher | Pickup/delivery pool, phân công, tối ưu tuyến, tái phân công, SLA, escalation, KPI | Không giả lập DELIVERED hoặc thay COD |
| Station Clerk/Operator | Nhận kiện, scan, cân đo, kiểm tra, manifest, xuất/nhập, hoàn, tồn, chênh lệch, walk-in | Custody phải truy vết, không tự làm Driver |
| Sorting Staff | Scan, tuyến, gom bao, xác nhận phân loại, sai tuyến, barcode lỗi, hư hỏng | Không xem đầy đủ PII/COD |
| Hub Supervisor | Capacity, tồn kiện, chuyến, manifest lệch, mất/hỏng, điều chuyển | Điều chỉnh cần lý do/bằng chứng/approval |
| Station Manager | Ca/nhân sự, kho, pickup/delivery, chuyến, COD, sự cố, KPI/SLA, audit | Không sửa ledger hoặc giả POD |
| Network Operations Manager | Mạng lưới, coverage, năng lực, điều chuyển vùng, thành công, SLA, ngoại lệ | Không sửa COD/settlement/bảo mật |


## 11. Change log mở rộng FULL Nhóm E–F — 2026-10-26

- **Quyết định mở rộng:** cả 7 Role Nhóm E và 6 Role Nhóm F thuộc Target FULL; CORE thể hiện mục tiêu nghiệp vụ, **không phải kết quả triển khai**.
- **ID ổn định:** giữ `R01–R27`, `BC-001–BC-280`; bổ sung `R28–R33` và `BC-281–BC-371`; không đổi số hoặc xóa hàng cũ. Những scope CORE được nâng từ EXPAND theo mục tiêu FULL, không chuyển hành vi bị cấm sang được phép.
- **R28 Pricing / Billing Administrator:** rate card, zones, weight, service, COD fee, discount, hợp đồng, effective dates, price snapshot và history.
- **R29 Sales / Key Account Manager:** Merchant account được giao, tư vấn, hợp đồng, đề xuất giá, KPI, Merchant lớn; không tự phê duyệt giá.
- **R30 Risk / Fraud / Compliance Officer:** COD fraud, account abuse, spam Shipment, PII/recipient anomaly, shipment hold/release theo approval, audit.
- **R31 Platform Super Admin:** tenant, partition, privilege, feature flags, governance; không bypass POD/financial settlement hoặc xóa audit.
- **R32 IT / Integration Operations:** health checks, webhook retries, queues, sync errors, production operational controls; không tùy ý impersonate/read recipient.
- **R33 Partner / Franchise Operator:** nhận/kiểm/đối chiếu/bàn giao kiện tại điểm, tồn điểm, doanh thu điểm, Partner Organization/Station isolation.
- **Đầy đủ Role E/F đã có:** R04 Business Admin, R05 Identity Admin, R12 COD Cashier, R13 Finance Operator, R14 Customer Service Agent, R15 Claims Officer và R17 Auditor đều được bổ sung các khả năng còn thiếu, có phân tách responsibilities.
- **Nguồn và precedence:** phần II là tài liệu nghiệp vụ tham khảo; mục VIII các quyết định phạm vi 8.9–8.12 là Target Business Scope mới. Không tự động sửa canonical SDD; mọi chi tiết API/DB/permission chính thức cần review traceability.

### Acceptance Gates riêng Nhóm E–F

| Nhóm năng lực | Acceptance trọng yếu |
|---|---|
| Customer Support & Claims | Ticket có nguồn, SLA, case evidence, claim approval độc lập với người đề xuất và người chi tiền |
| COD & Finance | COLLECTED/REMITTED/RECONCILED/SETTLED không đồng nhất; ledger bất biến, idempotency, chống duplicate payment/refund |
| Pricing | Snapshot giá chốt, rate card version/effective date, điều chỉnh có bút toán và audit |
| Sales | Account scope, phê duyệt giá đặc biệt, không tự thay ngân hàng Merchant |
| Risk | Shipment hold/release theo quyền và chứng cứ, không tạo/sửa lén tracking hoặc COD |
| Governance | Business Admin ≠ Identity Admin ≠ Super Admin ≠ IT ≠ Auditor; deny test với quyền vượt scope |
| Partner | Partner Organization và Station isolation; biên nhận/custody đối với kiện tại quầy |

### Công việc sau khi duyệt scope

Đối chiếu `Rxx → BC-xxx → Mxx → SDD requirement → permission → API → DB → UI → automated test`. Không điền VERIFIED khi thiếu evidence, không giả định đã có bảng hoặc service chỉ từ ma trận.
