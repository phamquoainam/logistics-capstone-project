# Business Workflows — nguồn Chương 2 §2.2

Bảy quy trình là dữ liệu nguồn [REPORT], không thể hiện tất cả lựa chọn API và transaction đã chốt.


## 2.2.1 — Tạo đơn và thu cước

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Tạo vận đơn hợp lệ, xác định và thu cước từ ví merchant. |
| Tác nhân | Merchant và hệ thống. |
| Điều kiện bắt đầu | Merchant, ví và điểm lấy hàng đang hoạt động; dịch vụ và bảng giá khả dụng. |
| Đầu vào | Điểm lấy hàng, người nhận, địa chỉ, kiện hàng, dịch vụ, COD hoặc tệp import. |
| Luồng chính | Kiểm tra dữ liệu và địa chỉ → tạo báo giá → merchant xác nhận → kiểm tra số dư → thu cước → phát hành vận đơn. |
| Kết quả | Vận đơn có mã theo dõi, báo giá có phiên bản và giao dịch thu cước có thể truy nguyên. |
| Ngoại lệ | Dữ liệu sai, địa chỉ chưa xác nhận, dịch vụ không hỗ trợ, báo giá hết hạn, thiếu số dư hoặc yêu cầu trùng. |
| Quy tắc chính | Số dư không âm; một yêu cầu không tạo nhiều vận đơn hoặc lần ghi nợ; vận đơn chỉ được xác nhận sau khi thu cước thành công. |

## 2.2.2 — Xử lý kho

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Quản lý hàng hóa từ tiếp nhận, lưu trữ, giữ hàng đến lấy, đóng gói và xuất kho. |
| Tác nhân | Merchant, nhân viên kho và hệ thống. |
| Điều kiện bắt đầu | Kho, vị trí lưu trữ và hàng hóa đã được khai báo; vận đơn yêu cầu xử lý qua kho. |
| Đầu vào | Sản phẩm, số lượng, kho, vị trí, phiếu nhập, yêu cầu giữ hàng và vận đơn. |
| Luồng chính | Tiếp nhận hàng → ghi nhận vị trí → giữ hàng → lấy hàng → đóng gói → xác nhận xuất kho. |
| Kết quả | Kiện hàng sẵn sàng bàn giao; tồn kho và lịch sử biến động được cập nhật. |
| Ngoại lệ | Thiếu tồn, sai vị trí, giữ hàng trùng, chênh lệch kiểm đếm hoặc thao tác không đúng trạng thái. |
| Quy tắc chính | Tồn khả dụng không âm; mọi biến động có nguồn nghiệp vụ; không xuất vượt số lượng đã giữ; điều chỉnh phải có lý do. |

## 2.2.3 — Điều phối và giao nhận

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Tổ chức lấy hàng, trung chuyển, phân tuyến, giao hàng và xử lý giao thất bại. |
| Tác nhân | Điều phối viên, nhân viên kho/hub, shipper, người nhận và hệ thống. |
| Điều kiện bắt đầu | Vận đơn đã xác nhận, kiện hàng sẵn sàng và có nguồn lực vận chuyển phù hợp. |
| Đầu vào | Vận đơn, hub, chuyến, manifest, shipper, tọa độ, mạng đường và ràng buộc giao hàng. |
| Luồng chính | Lập chuyến và manifest → phân công → lấy hàng → nhập–xuất hub → gợi ý tuyến → giao hàng → ghi nhận POD. |
| Kết quả | Vận đơn được giao thành công hoặc chuyển sang giao lại/hoàn hàng; tracking và lịch sử bàn giao được cập nhật. |
| Ngoại lệ | Phân công không hợp lệ, quét trùng, sai hub hoặc manifest, giao thất bại, POD không hợp lệ hay mất kết nối. |
| Quy tắc chính | Một chặng chỉ có một phân công hoạt động; trạng thái chuyển đúng trình tự; giao thành công phải có POD; vị trí GPS quá cũ không được xem là vị trí hiện tại. |

## 2.2.4 — Nạp tiền

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Bổ sung số dư ví merchant qua VNPay Sandbox. |
| Tác nhân | Merchant, hệ thống và VNPay Sandbox. |
| Điều kiện bắt đầu | Merchant và ví đang hoạt động; số tiền nằm trong giới hạn cho phép. |
| Đầu vào | Số tiền, mã thanh toán và kết quả trả về từ VNPay Sandbox. |
| Luồng chính | Tạo yêu cầu thanh toán → chuyển sang VNPay Sandbox → nhận và xác minh kết quả → ghi có vào ví. |
| Kết quả | Ví được ghi có đúng một lần hoặc giao dịch được ghi nhận thất bại/chưa xác định. |
| Ngoại lệ | Người dùng hủy, chữ ký hoặc số tiền sai, kết quả trùng, quá thời gian hoặc trạng thái không rõ ràng. |
| Quy tắc chính | Không ghi có chỉ dựa trên kết quả từ trình duyệt; một yêu cầu thanh toán chỉ tạo một bút toán; mọi thay đổi số dư phải có nguồn nghiệp vụ. |

## 2.2.5 — Thu nộp và đối soát COD

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Theo dõi COD từ khi phát sinh nghĩa vụ thu đến khi hoàn tất đối soát với merchant. |
| Tác nhân | Shipper, Finance và merchant. |
| Điều kiện bắt đầu | Vận đơn có COD; việc ghi nhận đã thu yêu cầu giao hàng thành công. |
| Đầu vào | Số tiền COD, kết quả giao hàng, khai báo thu–nộp, xác nhận của Finance và kỳ đối soát. |
| Luồng chính | Shipper thu tiền → khai báo nộp → Finance xác nhận → lập kỳ → kiểm tra và đóng kỳ → ghi nhận chi trả mô phỏng. |
| Kết quả | COD được theo dõi qua các mốc phải thu, đã thu, đã nộp, đã xác nhận và đã đối soát. |
| Ngoại lệ | Chênh lệch tiền, khoản nộp chưa xác nhận, tranh chấp, dữ liệu đến muộn hoặc phát hiện sai lệch sau đóng kỳ. |
| Quy tắc chính | Shipper không tự xác nhận khoản mình nộp; một khoản COD không thuộc nhiều kỳ đã đóng; kỳ đã đóng chỉ được điều chỉnh bằng nghiệp vụ mới. |

## 2.2.6 — Quản lý bảng giá

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Quản lý quy tắc tính cước theo vùng, dịch vụ, khối lượng và thời gian hiệu lực. |
| Tác nhân | Quản trị viên hoặc người được cấp quyền quản lý bảng giá. |
| Điều kiện bắt đầu | Danh mục vùng, dịch vụ và quy tắc tính khối lượng đã được cấu hình. |
| Đầu vào | Vùng gửi–nhận, dịch vụ, bậc khối lượng, phí cơ bản, phụ phí, giảm giá và thời gian hiệu lực. |
| Luồng chính | Tạo phiên bản nháp → kiểm tra quy tắc → công bố → áp dụng theo thời gian hiệu lực. |
| Kết quả | Phiên bản bảng giá khả dụng; báo giá lưu phiên bản và chi tiết cấu thành cước. |
| Ngoại lệ | Quy tắc thiếu, bậc khối lượng chồng lấn, thời gian hiệu lực xung đột hoặc không có mức giá phù hợp. |
| Quy tắc chính | Không sửa phiên bản đã được sử dụng; mỗi báo giá tham chiếu một phiên bản; không tự suy đoán phí khi thiếu quy tắc. |

## 2.2.7 — Kiểm tra địa chỉ và xác định tọa độ

| Nội dung | Mô tả từ báo cáo |
| --- | --- |
| Mục đích | Bảo đảm địa chỉ đủ cấu trúc và có tọa độ phù hợp để tính cước, điều phối và lập tuyến. |
| Tác nhân | Merchant, nhân viên nhập liệu, hệ thống và dịch vụ bản đồ. |
| Điều kiện bắt đầu | Danh mục hành chính và dịch vụ bản đồ khả dụng hoặc có phương án xác nhận thủ công. |
| Đầu vào | Địa chỉ chi tiết, tỉnh/thành phố, phường/xã và thông tin tọa độ nếu có. |
| Luồng chính | Kiểm tra trường bắt buộc → đối chiếu danh mục → xác định tọa độ → hiển thị kết quả → người dùng xác nhận khi cần. |
| Kết quả | Địa chỉ có cấu trúc, tọa độ và trạng thái xác nhận; địa chỉ gốc vẫn được lưu để truy vết. |
| Ngoại lệ | Không tìm thấy, nhiều kết quả tương đương, phân cấp không hợp lệ, tọa độ nằm ngoài vùng hoặc dịch vụ bản đồ không khả dụng. |
| Quy tắc chính | Không ghi đè địa chỉ gốc; kết quả phải gắn với phiên bản danh mục; trường hợp mơ hồ phải được xác nhận thủ công. |

## Chia trách nhiệm
- Luồng xác nhận/hủy và giao COD cần workflow coordinator với transaction cục bộ được kiểm tra.
- VNPay callback, notification và background worker không thay thế transaction tiền/tồn.
- Trạng thái nghiệp vụ chính thức xem `state-machines.md` và BR-01–BR-21.
