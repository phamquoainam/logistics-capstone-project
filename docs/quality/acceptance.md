# Project-wide Acceptance Criteria

**Nguồn:** [REPORT] Chương 2 §2.7.5.

| ID | Định nghĩa |
| --- | --- |
| AC-01 | Hoàn thành các yêu cầu P0 trên web, ứng dụng React Native và backend; nội dung chưa hoàn thành phải được công bố rõ ràng. |
| AC-02 | Trình diễn được luồng: tạo/import đơn → xác nhận địa chỉ → tính và thu cước → xử lý kho → điều phối và trung chuyển → gợi ý tuyến và theo dõi GPS → giao có POD → thông báo → thu, nộp và đối soát COD. |
| AC-03 | Không vi phạm các bất biến về số dư, bút toán, tồn kho, trạng thái, phân công, báo giá và kỳ đối soát trong các kiểm thử đã công bố. |
| AC-04 | Đạt các kiểm thử chức năng, tích hợp, end-to-end, bảo mật, đồng thời, tải và phục hồi cốt lõi; lỗi còn lại phải được ghi nhận và đánh giá ảnh hưởng. |
| AC-05 | Các mục tiêu hiệu năng được đánh giá theo môi trường, dữ liệu và tải thử nghiệm công bố tại Chương 6. |
| AC-06 | Hệ thống có thể triển khai lại bằng Docker Compose từ mã nguồn, migration, dữ liệu mẫu và tài liệu hướng dẫn. |
| AC-07 | Có đặc tả, mô hình, ma trận truy vết và bằng chứng kiểm thử tương ứng với các yêu cầu đã công bố. |
| AC-08 | Hoàn thành thực nghiệm cho RQ1–RQ4 và báo cáo cả kết quả đạt, không đạt hoặc bác bỏ giả thuyết. |

## Kiểm tra nghiệm thu tối thiểu
- Không mở rộng kết luận ngoài cấu hình tải/điều kiện thử nghiệm đã công bố.
- Mỗi AC phải có một hoặc nhiều test ID và bằng chứng tại `docs/quality/traceability.md`.
- Báo cáo danh sách tính năng chưa hoàn thành thay vì đánh dấu P0 đạt theo cảm tính.
