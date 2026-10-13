# Non-functional Requirements

**Nguồn:** [REPORT] Chương 2 §2.6; ngưỡng chỉ áp dụng prototype và phép đo được công bố.

| ID | Yêu cầu | Tiêu chí | Cách kiểm chứng |
| --- | --- | --- | --- |
| NFR-C01 | Bảo vệ bất biến tiền, tồn, trạng thái, phân công, giá và đối soát. | Không phát hiện vi phạm trong kịch bản công bố; kiểm tra cả liên aggregate. | Đồng thời, tải, mô phỏng sự cố và truy vấn bất biến. |
| NFR-C02 | Thao tác lặp không tạo nhiều tác động nghiệp vụ. | Cùng định danh/phạm vi/nội dung có một kết quả; khác nội dung báo xung đột; nghiệp vụ mới không bị chặn nhầm. | Gửi lặp, đồng thời, dữ liệu muộn, thử lại sau mất phản hồi. |
| NFR-C03 | Thay đổi quan trọng truy nguyên được chủ thể, thời điểm và nguồn. | Truy từ đơn/giao dịch đến trạng thái, phiên bản giá, biến động và audit. | Kiểm tra tham chiếu và lịch sử. |
| NFR-P01 | API nghiệp vụ thông thường ổn định với 100 người dùng ảo. | p95 ≤ 750 ms, p99 ≤ 1,5 giây; lỗi hệ thống < 1%. Đo riêng tải tệp, tính tuyến và phụ thuộc ngoài. | Cố định thành phần tải, thời gian, khoảng nghỉ và ranh giới đo; từ chối nghiệp vụ đúng quy tắc được báo riêng. |
| NFR-P02 | Pricing nhanh và đúng phiên bản. | p95 ≤ 300 ms ở tải công bố; toàn bộ dữ liệu giá chuẩn đúng, kể cả đổi giá và cache lỗi. | Không cache/cache lạnh/cache nóng trên cùng dữ liệu, tài nguyên; đối chiếu kết quả và truy vấn. |
| NFR-P03 | Tạo tuyến trong giới hạn quy mô thử nghiệm. | ≤ 10 giây cho tối đa 50 điểm; không áp dụng tuyến vi phạm ràng buộc. Vô nghiệm phải báo rõ. | Cố định điểm, mạng đường và ràng buộc; đo riêng tính toán và dịch vụ ngoài. |
| NFR-P04 | Vị trí được cập nhật gần thời gian thực khi kết nối ổn định. | ≤ 5 giây từ máy chủ nhận bản tin hợp lệ đến giao diện hiển thị; tuổi mẫu tính riêng từ thời điểm thiết bị ghi nhận. | Công bố chu kỳ gửi, mạng và ranh giới đo; đồng bộ muộn không tính là vị trí thời gian thực. |
| NFR-P05 | Quá tải không phá bất biến hoặc mất nghiệp vụ đã xác nhận. | Kiểm tra dữ liệu sau mỗi bài tải; từ chối quá tải có kiểm soát, không báo thành công giả. | Tăng tải theo mức, phân loại lỗi và truy vấn bất biến. |
| NFR-R01 | Lỗi tạm thời hoặc khởi động lại không mất nghiệp vụ đã xác nhận hoặc tạo tác động trùng. | Sau phụ thuộc phục hồi, nghiệp vụ hoàn thành hợp lệ hoặc có trạng thái chờ/ngoại lệ truy vết; không treo âm thầm. | Timeout, dừng/khởi động lại, thử lại; kiểm tra trạng thái và tác động. |
| NFR-R02 | Tác vụ không tự phục hồi được xử lý có kiểm soát. | Có trạng thái, nguyên nhân, số lần thử, người/định danh phục hồi và kết quả. | Vượt giới hạn thử lại, lỗi kéo dài, phát lại và phát lại trái quyền. |
| NFR-R03 | Thao tác shipper mất mạng không mất âm thầm. | Thao tác hỗ trợ offline lưu bền trên thiết bị; mở lại thấy chờ/đã ghi nhận/xung đột. Gửi lại giữ định danh và kiểm tra quyền/trạng thái hiện tại. | Mất mạng, đóng/mở app, gửi lại, gán lại trước đồng bộ; chức năng không hỗ trợ offline phải báo rõ. |
| NFR-R04 | Sao lưu và khôi phục dữ liệu quan trọng. | Ít nhất một backup–restore thành công; công bố điểm khôi phục, kiểm tra toàn vẹn và bất biến của dữ liệu trong bản sao. | Nhật ký khôi phục, đối chiếu số lượng/tổng và truy vấn bất biến; không cam kết HA. |
| NFR-S01 | Xác thực và phân quyền phía máy chủ, gồm quyền trên đối tượng. | Từ chối ngoài vai trò/tổ chức/kho/hub; không trả dữ liệu nhạy cảm; kiểm tra lại khi ghi nhận. | Ma trận quyền, truy cập chéo tổ chức/kho/hub và phân công hết hiệu lực. |
| NFR-S02 | Bảo vệ mật khẩu, dữ liệu cá nhân, thanh toán và bí mật. | Mật khẩu được băm; không có secrets trong mã nguồn/tài nguyên công khai; log không chứa secrets hoặc dữ liệu cá nhân không cần thiết. | Kiểm tra lưu mật khẩu, cấu hình, secrets và log. |
| NFR-S03 | Kiểm tra và giới hạn đầu vào, POD, callback, API công khai. | Từ chối đầu vào/tệp/chữ ký sai; kiểm tra nội dung tệp và quyền; API công khai có giới hạn cấu hình. | Validation, upload sai/vượt giới hạn, rate limiting, gửi lại và callback giả. |
| NFR-S04 | Audit các thao tác nhạy cảm. | Có chủ thể, hành động, đối tượng, thời điểm, kết quả; không sửa/xóa qua chức năng người dùng. | Kiểm tra audit thành công/từ chối theo chính sách và quyền đọc. |
| NFR-S05 | GPS/POD đúng quyền, mục đích và thời hạn lưu. | Chỉ người có nhiệm vụ liên quan được xem; dừng thu vị trí khi phiên/quyền kết thúc; chính sách lưu/xóa được kiểm chứng. | Truy cập trái quyền, thu ngoài phiên, dừng quyền và thời hạn lưu. |
| NFR-U01 | Web và ứng dụng hoạt động trên mục tiêu công bố. | Luồng chính đạt trên trình duyệt, hệ điều hành và thiết bị thử nghiệm. | Bảng tương thích có phiên bản và kết quả. |
| NFR-U02 | Lỗi và trạng thái chờ rõ ràng. | Giữ dữ liệu nhập trong phạm vi cho phép; phân biệt chưa gửi/chờ/đã ghi nhận/xung đột và hướng xử lý. | Dữ liệu sai, lỗi mạng, thay đổi quyền và kết quả chưa xác định. |
| NFR-O01 | Log có cấu trúc, metric và mã liên kết. | Truy được thao tác qua module/phụ thuộc; không lộ dữ liệu nhạy cảm. | Log, metric và truy vết theo định danh. |
| NFR-O02 | Health/readiness và chỉ số vận hành. | Theo dõi lỗi, độ trễ, lưu lượng, tồn đọng; phản ánh đúng phụ thuộc trong thử nghiệm. | Dashboard, kiểm tra hoạt động và lỗi phụ thuộc. |
| NFR-O03 | Triển khai tái lập từ tài liệu và phiên bản. | Người khác khởi chạy Docker Compose, migration, dữ liệu mẫu và smoke test với cấu hình an toàn. | Biên bản triển khai độc lập, phiên bản mã và cấu hình. |
| NFR-M01 | Giao tiếp qua giao diện công khai; không phụ thuộc vòng hoặc truy cập nội bộ trái phép. | Kiểm tra kiến trúc phát hiện vi phạm ranh giới; ngoại lệ thiết kế được ghi rõ. | Kiểm tra phụ thuộc và ranh giới tại Chương 4–5. |
| NFR-M02 | Luồng và quy tắc trọng yếu có kiểm thử tự động. | Bộ kiểm thử truy vết UC/BR, chạy trong CI và lưu báo cáo; không chỉ nêu tỷ lệ bao phủ mã. | Báo cáo CI và liên kết kiểm thử–yêu cầu. |
