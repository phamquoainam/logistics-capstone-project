# Business Rules — trích nghĩa từ báo cáo §2.4

**Nguồn:** [REPORT] Chương 2 §2.4, bảng BR-01–BR-21. Mọi thay đổi nghĩa cần được xác nhận và đồng bộ với báo cáo.

| ID | Quy tắc | Ngoại lệ / kiểm chứng |
| --- | --- | --- |
| BR-01 | Vận đơn chỉ chuyển theo vòng đời hợp lệ; không sửa hoặc xóa lịch sử đã ghi. Cập nhật đến muộn phải kiểm tra trạng thái hiện tại. | Từ chối chuyển sai và lưu lý do. |
| BR-02 | Cùng mã thao tác, loại nghiệp vụ, chủ thể/phạm vi và nội dung chỉ tạo một kết quả; mã trùng nhưng khác nội dung phải báo xung đột. | Áp dụng cho tạo, xác nhận và hủy. Nghiệp vụ mới có định danh mới; không chống trùng chỉ bằng nội dung. |
| BR-03 | Đơn gửi trực tiếp được hủy trước xác nhận lấy hàng; đơn WMS trước khi bắt đầu picking. Giữ hàng chưa phải bắt đầu picking. | Sau mốc hủy, xử lý dừng hoặc hoàn có thẩm quyền theo tình trạng hàng thực tế; không tự hủy hoặc ghi nhận hoàn khi chưa đủ căn cứ. |
| BR-04 | Mỗi chặng tối đa một phân công hoạt động, gồm ASSIGNED hoặc IN_PROGRESS. Gán lại phải kết thúc phân công cũ và tạo mới nhất quán. | Có lý do và lịch sử; thay phân công không thay bên giữ kiện nếu chưa bàn giao. |
| BR-05 | Giao thành công phải có POD hợp lệ của đúng lần giao; thất bại có mã lý do và định danh lần giao. | POD thuộc đơn/lần giao được phép, đáp ứng các trường và giới hạn đã cấu hình. |
| BR-06 | Tồn khả dụng bằng tồn đủ điều kiện phân bổ trừ lượng còn giữ, theo chủ hàng, SKU, kho/vị trí; không được âm. | Hàng cách ly hoặc chưa đạt kiểm tra không thuộc tồn đủ điều kiện; từ chối giữ vượt tồn. |
| BR-07 | Không lấy hoặc xuất vượt phần phân bổ còn hợp lệ; không sử dụng hoặc giải phóng vượt lượng còn giữ. Lượng giữ bằng đã sử dụng cộng đã giải phóng cộng còn giữ. | Hủy chỉ giải phóng phần chưa dùng; hàng đã xuất cần chứng từ hoàn, không giải phóng như hàng chưa xuất. |
| BR-08 | Mọi biến động tồn có loại, thời điểm, chủ thể và nguồn; lịch sử không sửa hoặc xóa. Chuyển vị trí bảo toàn tổng lượng của nghiệp vụ. | Sai lệch xử lý bằng chứng từ điều chỉnh có quyền và lý do; số đếm kiểm kê không tự sửa tồn. |
| BR-09 | Nhập–xuất hub và bàn giao phải đối chiếu kiện, manifest/chặng, trạng thái và bên giao–nhận; cùng thao tác quét không tạo tác động lặp. | Sai hub, sai manifest, thiếu/thừa và tồn đọng được ghi nhận. Xuất WMS không tự xác nhận hub đã nhận. |
| BR-10 | Khối lượng tính cước là giá trị lớn hơn giữa khối lượng thực và quy đổi, làm tròn theo phiên bản giá. Đơn vị đo và hệ số quy đổi phải xác định. | Từ chối thiếu hoặc sai khối lượng, kích thước. |
| BR-11 | Cước được tính theo vùng, dịch vụ, bậc khối lượng, phụ phí/giảm giá và quy tắc làm tròn có thứ tự xác định. | Thiếu quy tắc không được suy đoán giá; chi tiết phí phải đối chiếu được với tổng. |
| BR-12 | Báo giá lưu đầu vào, phiên bản, chi tiết phí và thời hạn; xác nhận đơn lưu dữ liệu chụp của báo giá đã chấp nhận. | Hết hạn phải tính lại. Phiên bản đã công bố hoặc sử dụng không sửa trực tiếp; giá mới tạo phiên bản mới. |
| BR-13 | Ví không âm. Xác nhận đơn chỉ hoàn tất khi báo giá hợp lệ, thu cước thành công và giữ đủ hàng nếu dùng WMS, với kết quả nhất quán. | Thiếu tiền hoặc tồn không xác nhận. Kết quả chưa xác định phải tra cứu, không thu hoặc giữ thêm tùy tiện. |
| BR-14 | Yêu cầu nạp chỉ ghi có một lần sau khi xác minh tính xác thực, mã tham chiếu, số tiền và kết quả thành công của nhà cung cấp. | Không dùng chuyển hướng trình duyệt làm căn cứ duy nhất. Timeout chuyển chờ xác minh; dữ liệu sai bị từ chối; kết quả trùng không ghi thêm. |
| BR-15 | Mọi thay đổi số dư có bút toán và nguồn; bút toán không sửa hoặc xóa. Hoàn/điều chỉnh tạo nghiệp vụ mới liên kết dữ liệu gốc. | Một yêu cầu hoàn/điều chỉnh không tạo tác động lặp; số dư đối chiếu được với sổ cái. |
| BR-16 | Đã giao, đã thu, đã nộp và đã đối soát là các mốc riêng. Shipper không tự xác nhận tiền mình nộp. | Khai báo chưa được Finance xác nhận vẫn là nghĩa vụ chưa hoàn tất; lưu phân bổ tiền nộp và số còn thiếu. |
| BR-17 | COD đủ điều kiện chốt khi đã giao, thu đủ, được xác nhận nộp đủ, không tranh chấp và chưa thuộc kỳ đóng; các mốc cần thiết đáp ứng thời điểm chốt của kỳ. | Kiểm tra lại khi đóng. Dữ liệu đến muộn sau chốt được chuyển kỳ sau hoặc xử lý có quyền, không sửa kỳ cũ. |
| BR-18 | Kỳ đóng bảo toàn bảng kê và tổng tiền; một khoản COD tối đa thuộc một kỳ đã đóng. Chi trả là nghiệp vụ riêng, cùng yêu cầu không trả lặp. | Sai lệch tạo điều chỉnh có liên kết; không sửa kỳ gốc hoặc coi nghĩa vụ đã chi là chưa chi. |
| BR-19 | Lưu địa chỉ gốc/đã xác nhận, tọa độ, nguồn và phiên bản danh mục áp dụng. Thay danh mục không sửa vận đơn đã chốt. | Kết quả mơ hồ yêu cầu xác nhận hoặc chọn điểm thủ công; P0 không bắt buộc NLP. |
| BR-20 | GPS có shipper/phiên, thời điểm thiết bị ghi nhận và hệ thống tiếp nhận, độ chính xác, nguồn; chỉ thu trong phiên và quyền hợp lệ. | Mẫu sai hoặc quá cũ bị loại/đánh dấu; tuổi dữ liệu xét thời điểm ghi nhận, không chỉ thời điểm nhận lại. |
| BR-21 | Tuyến chỉ chứa nhiệm vụ hiện tại hợp lệ, tuân thủ ràng buộc; điều phối xác nhận trước áp dụng. Đổi tuyến đã xác nhận phải có phiên bản và lịch sử. | Không tự sửa phân công hoặc trạng thái từ tuyến/GPS; giữ kết quả điểm đã phục vụ khi thay tuyến. |
