# Ubiquitous Language

**Nguồn [REPORT]:** Chương 2 §2.3; Chương 3 §3.6; Chương 4 §4.2.2.

| Tên thống nhất | Ý nghĩa / phân biệt |
| --- | --- |
| Merchant | Tổ chức/chủ hàng; khác với user Account đang thao tác. |
| Shipment | Vận đơn và vòng đời giao nhận. Dữ liệu có thể đi qua nhiều chặng. |
| Parcel | Kiện vật lý thuộc một Shipment; không đồng nghĩa SKU hoặc stock. |
| SKU | Danh mục hàng hóa do Warehouse quản lý, có tồn theo owner/location/condition. |
| Quote | Kết quả tính cước có tariff version, input, breakdown và thời hạn. |
| Quote Snapshot | Bản chụp quote được Order cố định khi xác nhận vận đơn. |
| Reservation | Lượng tồn đã giữ cho nhu cầu cụ thể, không đồng nghĩa đã pick/xuất. |
| Inventory Balance | Số dư tồn tại thời điểm; phải đối chiếu Stock Movement. |
| Stock Movement | Chứng từ biến động tồn append-only. |
| Trip/Leg/Manifest | Chuyến/chặng/bảng kê kiện vận hành bởi Dispatch & Hub. |
| Assignment | Phân công hợp lệ cho một chặng; chỉ một active assignment trên chặng. |
| Custody | Bằng chứng ai giữ kiện tại mốc giao–nhận, khác với trạng thái tồn kho. |
| POD | Proof of Delivery gắn đúng lượt giao và Shipment, có metadata/ảnh/OTP theo chính sách. |
| Wallet | Số dư tác nghiệp của merchant; Ledger là nguồn bút toán đối soát. |
| COD Item | Nghĩa vụ thu hộ gắn vận đơn; phân biệt collected, remitted, reconciled. |
| Reconciliation Period | Kỳ đối soát, đã đóng thì bất biến. |
| Route Plan | Kế hoạch tuyến versioned/được dispatcher xác nhận; không tự gán shipper. |
| Location Session | Phiên được phép chia sẻ vị trí; mẫu GPS phải có recordedAt và accuracy. |
| Idempotency Key | Định danh yêu cầu trong một scope/operation nhằm tránh tác động trùng. |
| Outbox / Inbox | Lưu tin cần phát / tin đã xử lý để phục hồi khi giao nhận message at-least-once. |
