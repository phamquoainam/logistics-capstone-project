# Functional Requirements Catalog

**Nguồn:** [REPORT] Chương 2 §2.5, dùng mã yêu cầu gốc. Cột Use Case mang mã rút gọn như trong báo cáo.

| ID | Nội dung | Use Case | BR liên quan |
| --- | --- | --- | --- |
| FR-G01 | Ước tính cước công khai từ đầu vào hợp lệ, không tạo vận đơn. | G01 | BR-10–BR-12, BR-19 |
| FR-G02 | Tra cứu hành trình công khai, giới hạn dữ liệu được phép. | G02 | BR-01 |
| FR-A01 | Đăng nhập, đăng xuất, quản lý phiên và khôi phục tài khoản. | A01–A03 | — |
| FR-A02 | Kiểm tra quyền theo vai trò, tổ chức, kho/hub và đối tượng tại thời điểm thao tác. | AD02; áp dụng trong các UC không công khai | — |
| FR-M01 | Quản lý hồ sơ merchant và điểm lấy thuộc tổ chức. | M01 | BR-19 |
| FR-M02 | Nhập/kiểm tra địa chỉ, xác định hoặc chọn tọa độ và xác nhận thủ công khi cần. | M02 | BR-19 |
| FR-M03 | Tạo báo giá có phiên bản, đầu vào, chi tiết phí và thời hạn. | M03 | BR-10–BR-12 |
| FR-M04 | Tạo đơn nháp đơn lẻ hoặc import Excel; xem trước, báo lỗi từng dòng và chống tạo trùng. | M04, M10 | BR-02, BR-19 |
| FR-M05 | Theo dõi và hủy vận đơn theo điều kiện; tra cứu kết quả hủy/hoàn liên quan. | M06, M07 | BR-01, BR-03, BR-07, BR-15 |
| FR-M06 | Tra cứu tồn, lượng giữ và tiến độ fulfillment thuộc merchant. | W03, M07 | BR-06–BR-08 |
| FR-M07 | Tra cứu số dư/lịch sử và nạp tiền qua VNPay Sandbox. | M08, M11 | BR-13–BR-15 |
| FR-M08 | Tra cứu COD, bảng kê/chi trả; truy cập thông báo và dashboard theo quyền. | M09, M12, C01 | BR-16–BR-18 |
| FR-M09 | Xác nhận vận đơn khi báo giá hợp lệ, thu cước thành công và giữ đủ hàng WMS nếu áp dụng; có nhánh thiếu tiền/tồn, yêu cầu trùng và chưa xác định. | M05, W04 | BR-01–BR-02, BR-06–BR-07, BR-12–BR-13, BR-15, BR-19 |
| FR-W01 | Quản lý chủ hàng/SKU, kho, khu vực và vị trí theo phạm vi quyền. | W01 | BR-06, BR-08 |
| FR-W02 | Nhận hàng theo ASN/phiếu, kiểm đếm, QC, putaway và ghi lịch sử biến động; điều chỉnh tồn bằng chứng từ có quyền và lý do. | W02, W09 | BR-06, BR-08 |
| FR-W03 | Giữ/giải phóng, lấy–đóng gói–xuất theo vận đơn; đối chiếu phân bổ và kiện. | W04, W05 | BR-06–BR-09 |
| FR-W04 | Quét nhập–xuất hub, đối chiếu manifest và xác nhận bàn giao. | D04, S03 | BR-09 |
| FR-W05 | Theo dõi lưu hub và phát cảnh báo vượt ngưỡng cấu hình. | D06 | BR-09 |
| FR-W06 | Chuyển vị trí hàng có nguồn–đích, số lượng và chứng từ; bảo toàn lượng và xử lý phần đã giữ theo chính sách. | W06 | BR-06, BR-08 |
| FR-W07 | Ghi phạm vi kiểm kê, số tham chiếu tại thời điểm đối chiếu, số đếm và chênh lệch; điều chỉnh là bước có quyền riêng. | W07, W09 | BR-06, BR-08 |
| FR-W08 | Tiếp nhận hàng hoàn liên kết vận đơn/kiện gốc; kiểm tra để nhập lại đủ điều kiện hoặc cách ly, chống nhập lặp. | W08 | BR-06, BR-08–BR-09 |
| FR-D01 | Quản lý hub/nguồn lực, tổ chức chuyến và manifest theo chặng. | D01, D02 | BR-04, BR-09 |
| FR-D02 | Phân công/gán lại shipper theo chặng, lưu lý do và lịch sử. | D03 | BR-04 |
| FR-D03 | Tạo tuyến dự thảo từ nhiệm vụ, mạng đường và ràng buộc vận hành. | D05 | BR-21 |
| FR-D04 | Xem, chỉnh, kiểm tra, xác nhận tuyến và lưu phiên bản. | D07 | BR-21 |
| FR-D05 | Giám sát chuyến, vị trí có độ mới, ngoại lệ và cách xử lý. | D06 | BR-01, BR-09, BR-20 |
| FR-S01 | Cung cấp ứng dụng React Native để xem, bắt đầu và thực hiện nhiệm vụ được giao. | S01 | BR-04 |
| FR-S02 | Quét/nhập mã khi lấy, giao–nhận kiện và giao/hoàn. | S02–S05 | BR-01, BR-09 |
| FR-S03 | Xem/thực hiện tuyến đã xác nhận, bật/tắt chia sẻ vị trí trong phiên và cập nhật tiến độ. | S06, S07 | BR-20–BR-21 |
| FR-S04 | Ghi kết quả giao, thất bại, giao lại hoặc hoàn; thành công có POD, thất bại có lý do. | S04, S05 | BR-01, BR-03, BR-05 |
| FR-S05 | Ghi COD thực thu, khai báo nộp và bằng chứng để Finance xác nhận. | S04, S08 | BR-16 |
| FR-F01 | Tra cứu thanh toán/giao dịch gốc trong phạm vi quyền để xác minh và xử lý kết quả chưa xác định. | F06–F08 | BR-14–BR-15 |
| FR-F02 | Đối chiếu, xác nhận COD thực nhận, phân bổ và xử lý chênh lệch/tranh chấp. | F01 | BR-16–BR-17 |
| FR-F03 | Lập, xem trước, kiểm tra và đóng kỳ; kiểm tra lại điều kiện khi chốt. | F02, F05 | BR-17–BR-18 |
| FR-F04 | Chi trả mô phỏng, hoàn cước và điều chỉnh bằng nghiệp vụ riêng có tham chiếu, quyền và lý do. | F03, F07, F08 | BR-15, BR-18 |
| FR-P01 | Tạo, kiểm tra, công bố/ngừng phiên bản giá; giữ lịch sử và phiên bản đã dùng. | F04 | BR-10–BR-12 |
| FR-AD01 | Quản lý tài khoản, trạng thái, vai trò và phạm vi quyền. | AD01, AD02 | — |
| FR-AD02 | Quản lý cấu hình thời hạn báo giá, giao lại/hoàn, ngưỡng hub/GPS và thông báo; kiểm tra, lưu vết thay đổi. | AD03 | BR-03, BR-09, BR-12, BR-20–BR-21 |
| FR-AD03 | Tra cứu audit và tác vụ lỗi; thử lại/xử lý thủ công có quyền, nguyên nhân và lịch sử; không bỏ qua bất biến. | AD04, O01 | BR-01–BR-02, BR-08, BR-15, BR-18 |
| FR-C01 | Tạo/gửi thông báo từ nghiệp vụ nguồn; người nhận xem/đánh dấu đọc; kiểm tra quyền và chống trùng theo nguồn, người nhận, mục đích. | C01; hành vi tự động trong các luồng nguồn | — |
| FR-C02 | Dashboard theo vai trò/phạm vi; công bố định nghĩa chỉ số và thời điểm cập nhật. | M12 | — |
