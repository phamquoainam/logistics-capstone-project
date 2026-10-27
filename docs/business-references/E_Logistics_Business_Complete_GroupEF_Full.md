> Current canonical target follows [Owner account-first decision](../decisions/customer-account-merchant-admission.md). Historical statements below are source context where superseded; accepted runtime/security unchanged.

# E-Logistics Capstone — Tài liệu nghiệp vụ toàn văn

> **Nguồn:** `business_logistoc.docx` — chuyển đổi nguyên văn sang Markdown để hỗ trợ AI review nghiệp vụ. Nội dung và thứ tự của tài liệu gốc được giữ nguyên; các tiêu đề dưới đây chỉ hỗ trợ điều hướng. Các tuyên bố trong tài liệu là nội dung nguồn, không mặc nhiên là thiết kế đã được phê duyệt.
>
> **Tài nguyên:** hình ảnh đi kèm trong thư mục `assets/` (đường dẫn tương đối). Để AI xem đủ cả hình ảnh, hãy cung cấp toàn bộ gói ZIP hoặc thư mục này, không chỉ tệp `.md`.

---

Để xây dựng E-Logistics Capstone thành một hệ thống có nghiệp vụ thực tế như Viettel Post, Giao Hàng Tiết Kiệm (GHTK) hay Giao Hàng Nhanh (GHN), chúng ta cần hiểu toàn bộ những người tham gia hệ thống, quyền hạn của họ và cách một đơn hàng đi từ lúc tạo đến lúc giao thành công, đối soát COD hoặc hoàn trả.

Tôi sẽ trình bày theo hướng Business Analysis + System Architecture, đủ làm nền tảng cho việc review nghiệp vụ, thiết kế Role-Permission Matrix, Database, API, Frontend và Acceptance Criteria của dự án.

Lưu ý: Đây là mô hình nghiệp vụ chuẩn tham khảo từ ngành chuyển phát và logistics thương mại điện tử Việt Nam, không phải tài liệu nội bộ hay khẳng định chính xác mọi quyền và quy trình đang áp dụng tại từng hãng trong năm 2026.

## I. Tổng quan các vai trò trong hệ thống Logistics

Một hệ thống chuyển phát chuyên nghiệp không chỉ có CUSTOMER, MERCHANT, SHIPPER và ADMIN.

Thông thường, có thể phân thành 6 nhóm chủ thể nghiệp vụ.

LOGISTICS MANAGEMENT PLATFORM

Đơn hàng · Vận chuyển · Kho vận · Tài chính · Chăm sóc khách hàng

1\. Khách hàng

Guest, Retail Sender, Recipient

2\. Merchant

Owner, Manager, Operator, Finance

3\. Giao nhận

Pickup Courier, Delivery Courier, Driver

4\. Vận hành

Dispatcher, Station, Hub, Sorting

5\. Kinh doanh

CS, Claims, COD, Accounting, Sales

6\. Quản trị

Business Admin, System Admin, Audit

Một điểm cực kỳ quan trọng: Role không nhất thiết tương ứng một loại tài khoản độc lập.

Ví dụ, một người có thể là chủ Merchant A, nhân viên Merchant B và đồng thời là người nhận hàng ở một đơn khác. Quyền phải được tính theo người dùng + tổ chức + vai trò + phạm vi dữ liệu + trạng thái hiện tại, không chỉ theo tên Role.

## II. Nghiệp vụ đầy đủ theo từng Role

### Nhóm A — Khách hàng và người sử dụng dịch vụ

Đây là các chủ thể bên ngoài doanh nghiệp vận chuyển.

### 1. Guest — Khách vãng lai

![Vận chuyển SPX Hải Phòng dưới góc nhìn chuyên gia 10 năm](assets/image1.jpeg)

Vai trò: Người chưa đăng nhập hoặc chưa có tài khoản Logistics.

Guest cần sử dụng được một số tiện ích công khai mà không bị buộc tạo tài khoản ngay.

| **Nghiệp vụ**           | **Chi tiết**                                                                            |
|-------------------------|-----------------------------------------------------------------------------------------|
| Tra cứu vận đơn         | Nhập mã vận đơn, chỉ xem trạng thái và thông tin đã được giới hạn                       |
| Ước tính phí vận chuyển | Nhập nơi gửi, nơi nhận, khối lượng, kích thước, dịch vụ                                 |
| Kiểm tra vùng phục vụ   | Xem tuyến giao nhận có được hỗ trợ hay không                                            |
| Tra cứu bưu cục         | Xem địa chỉ, giờ phục vụ, vị trí                                                        |
| Tra cứu dịch vụ         | Xem các loại hình vận chuyển, điều kiện gửi hàng                                        |
| Liên hệ hỗ trợ          | Gửi yêu cầu tư vấn hoặc phản ánh                                                        |
| Đăng ký tài khoản       | Đăng ký một tài khoản cá nhân; Merchant application chỉ sau đăng nhập                                      |
| Tạo yêu cầu gửi lẻ      | Có thể hỗ trợ hình thức gửi không đăng ký, nhưng cần xác minh qua OTP/quy trình bưu cục |

Giới hạn bảo mật: Guest không được xem đầy đủ số điện thoại, địa chỉ riêng tư, số tiền COD nhạy cảm hoặc lịch sử vận đơn của người khác chỉ bằng cách đoán mã vận đơn. Các thao tác nhạy cảm phải có thêm bằng chứng sở hữu.

### 2. Retail Sender — Khách gửi hàng cá nhân

Đây là khách hàng gửi hàng không thường xuyên, khác với Merchant kinh doanh chuyên nghiệp.

| **Nhóm nghiệp vụ** | **Chức năng**                                           |
|--------------------|---------------------------------------------------------|
| Hồ sơ              | Quản lý thông tin người gửi và phương thức liên hệ      |
| Tạo vận đơn        | Gửi một kiện hoặc yêu cầu lấy hàng tại nhà              |
| Địa chỉ            | Lưu các địa chỉ gửi, nhận thường dùng                   |
| Báo giá            | Xem phí tạm tính và dịch vụ khả dụng                    |
| Thanh toán         | Trả trước, trả tại bưu cục hoặc phương thức được hỗ trợ |
| Theo dõi           | Xem toàn bộ vận đơn do mình tạo                         |
| Hủy yêu cầu        | Hủy trong trạng thái và điều kiện cho phép              |
| Khiếu nại          | Yêu cầu tra soát, báo mất/hỏng, đề nghị hỗ trợ          |
| Hoàn hàng          | Xem và quản lý các kiện bị hoàn trả                     |
| Lịch sử            | Xem các lần gửi trước, hóa đơn hoặc biên nhận           |

Không nên ép khách gửi lẻ sử dụng mô hình Merchant nếu họ không có tổ chức kinh doanh.

### 3. Recipient — Người nhận hàng

Người nhận là một chủ thể nghiệp vụ riêng, dù không có tài khoản đăng nhập.

| **Nhóm nghiệp vụ** | **Chức năng**                                                                              |
|--------------------|--------------------------------------------------------------------------------------------|
| Nhận thông báo     | Được thông báo đơn sắp giao, giao thất bại, đổi lịch                                       |
| Theo dõi đơn       | Xem trạng thái vận chuyển với mức xác minh phù hợp                                         |
| Xác nhận giao hàng | Cung cấp OTP hoặc phương thức chứng minh nhận hàng theo chính sách                         |
| Thay đổi lịch      | Yêu cầu hẹn giờ giao lại trong giới hạn cho phép                                           |
| Chỉ dẫn giao hàng  | Cung cấp hướng dẫn giao, người nhận thay hoặc thông tin liên hệ                            |
| Thanh toán COD     | Thanh toán số tiền thu hộ đã được xác nhận                                                 |
| Từ chối nhận       | Từ chối theo lý do và chính sách cho phép                                                  |
| Khiếu nại          | Phản ánh giao thiếu, hỏng, sai kiện hoặc thái độ giao hàng                                 |
| Đổi địa chỉ        | Yêu cầu chuyển địa chỉ theo quy trình xét duyệt, không được tự sửa dữ liệu vận đơn đã chốt |

Recipient không được mặc định có quyền sửa vận đơn, thay đổi tiền COD hoặc hủy đơn của Merchant.

### Nhóm B — Merchant / Shop / Doanh nghiệp gửi hàng

Merchant là một tổ chức kinh doanh sử dụng dịch vụ vận chuyển. Một Merchant có thể có nhiều nhân viên, kho, cửa hàng, địa chỉ lấy hàng và hợp đồng dịch vụ.

![Vietnam Post cải tiến vì khách hàng | Báo Dân trí](assets/image2.jpeg)

![Tìm kiếm &quot;Kho 20-HNI Hoàng Mai SOC ở đâu?&quot; Xem ngay thông tin!](assets/image3.jpeg)

![Kho hàng Lazada ở đâu? Danh sách địa chỉ kho hàng Lazada cập nhật toàn quốc 2025](assets/image4.jpeg)

9

### 4. Merchant Owner — Chủ cửa hàng / Chủ doanh nghiệp

Đây là người có quyền quản trị cao nhất bên trong tổ chức Merchant, không phải quản trị viên toàn hệ thống Logistics.

| **Nhóm nghiệp vụ**  | **Chức năng**                                                              |
|---------------------|----------------------------------------------------------------------------|
| Merchant Profile    | Quản lý pháp nhân, tên cửa hàng, địa chỉ, thông tin thuế                   |
| Nhân sự             | Mời, thêm, khóa nhân viên và phân quyền trong tổ chức                      |
| Điểm lấy hàng       | Tạo, cập nhật, ngừng sử dụng địa chỉ lấy hàng                              |
| Quản lý vận đơn     | Xem toàn bộ đơn của tổ chức                                                |
| Tạo đơn             | Tạo đơn hàng hoặc vận đơn thủ công/hàng loạt                               |
| Cấu hình vận chuyển | Chọn dịch vụ, kho, quy tắc mặc định                                        |
| COD                 | Xem số tiền thu hộ, số tiền đã đối soát, công nợ                           |
| Tài chính           | Xem cước phí, bảng kê, hóa đơn, lịch thanh toán COD                        |
| Hợp đồng            | Xem mức giá được áp dụng, chính sách dịch vụ                               |
| Khiếu nại           | Theo dõi bồi thường, xử lý đơn bất thường                                  |
| Tích hợp            | Quản lý API integrations/webhooks của Merchant trong phạm vi được cấp phép |
| Báo cáo             | Doanh thu COD, tỷ lệ giao thành công, đơn hoàn, chi phí vận chuyển         |

Giới hạn: Merchant Owner chỉ có toàn quyền trong tổ chức của mình. Không được xem doanh thu, khách hàng hoặc đơn hàng của Merchant khác.

### 5. Merchant Manager — Quản lý cửa hàng / Quản lý vận hành

Được Merchant Owner ủy quyền để điều hành hoạt động hằng ngày.

Nghiệp vụ gồm quản lý nhân viên theo phạm vi được giao, quản lý các điểm lấy hàng, kiểm tra đơn cần xử lý, duyệt một số thao tác hủy hoặc sửa đơn, theo dõi hiệu suất giao hàng, xử lý giao thất bại và liên hệ đơn vị vận chuyển.

Merchant Manager không mặc định được đổi tài khoản ngân hàng nhận COD, quản lý mọi khóa API hoặc thay đổi chủ sở hữu Merchant.

### 6. Merchant Operator — Nhân viên xử lý đơn

Đây là Role được sử dụng rất thường xuyên trong cửa hàng thương mại điện tử.

| **Nghiệp vụ**      | **Chi tiết**                                        |
|--------------------|-----------------------------------------------------|
| Tạo đơn            | Tạo vận đơn từ thông tin khách mua hàng             |
| Nhập đơn hàng loạt | Import CSV/Excel hoặc đồng bộ hệ thống bán hàng     |
| Kiểm tra địa chỉ   | Chuẩn hóa địa chỉ, chọn địa chỉ lấy hàng            |
| Chọn dịch vụ       | Tính cước, chọn gói và phí                          |
| Xác nhận đơn       | Xác nhận yêu cầu giao cho hãng vận chuyển           |
| In nhãn            | In mã vận đơn, barcode, thông tin giao nhận         |
| Bàn giao           | Chuẩn bị kiện, quét và lập danh sách bàn giao       |
| Theo dõi đơn       | Xem các vận đơn đã tạo                              |
| Xử lý ngoại lệ     | Yêu cầu giao lại, hủy hoặc hoàn theo quyền được cấp |
| Kiểm tra COD       | Xem tiền thu hộ của các đơn thuộc phạm vi vận hành  |

Không được tự duyệt bồi thường, sửa số tiền COD sau khi quy trình đã khóa hoặc xóa lịch sử đơn hàng.

### 7. Merchant Warehouse Staff — Nhân viên kho của Merchant

Role này làm việc tại kho của người bán, không phải kho của hãng chuyển phát.

Nghiệp vụ bao gồm tiếp nhận yêu cầu đóng gói, lấy hàng, kiểm tra số lượng, đóng kiện, cân và đo kích thước, dán nhãn, quét mã, lập danh sách kiện bàn giao, ghi nhận thiếu hàng hoặc sai hàng và xác nhận nhân viên vận chuyển đã đến lấy.

Tại đây cần phân biệt:

- Order: Đơn mua bán của khách hàng.

- Shipment: Yêu cầu vận chuyển.

- Parcel: Kiện hàng vật lý.

Một order có thể được chia thành nhiều kiện hoặc nhiều shipment tùy thiết kế nghiệp vụ.

### 8. Merchant Finance — Kế toán Merchant

Chịu trách nhiệm đối soát tiền và chi phí từ phía người bán.

| **Nghiệp vụ**       | **Chi tiết**                                            |
|---------------------|---------------------------------------------------------|
| Theo dõi COD        | Xem COD dự kiến, đã thu, đang giữ, đã thanh toán        |
| Đối soát            | So sánh các khoản hãng vận chuyển phải trả              |
| Phí vận chuyển      | Xem phí gốc, phụ phí, điều chỉnh                        |
| Công nợ             | Xem bảng kê phải thu/phải trả                           |
| Thanh toán          | Kiểm tra các lần hãng chuyển COD cho Merchant           |
| Hóa đơn             | Xem và tải hóa đơn, chứng từ                            |
| Khiếu nại tài chính | Mở tra soát COD thiếu hoặc phí sai                      |
| Báo cáo             | Xuất báo cáo theo thời gian, kho hoặc đơn vị kinh doanh |

Merchant Finance không cần quyền thay đổi trạng thái giao hàng hoặc phân công Shipper.

### 9. Merchant Support — Nhân viên CSKH của Merchant

Đây là người hỗ trợ khách mua hàng của cửa hàng, không phải nhân viên CSKH của hãng vận chuyển.

Họ có thể tra cứu đơn thuộc Merchant, kiểm tra giao chậm, tiếp nhận phản ánh từ người mua, yêu cầu giao lại, làm việc với hãng chuyển phát và theo dõi hoàn hàng.

Quyền đọc dữ liệu phải được giới hạn theo tổ chức và phạm vi nhân sự. Không có quyền đọc lịch sử tài chính toàn nền tảng.

### 10. Enterprise/API Client — Doanh nghiệp tích hợp hệ thống

Đây là chủ thể sử dụng API để đồng bộ đơn từ ERP, OMS, WMS, website hoặc nền tảng bán hàng.

Nghiệp vụ gồm tạo và đồng bộ shipment, báo giá, lấy nhãn, nhận webhook trạng thái, tra cứu đơn, gửi idempotency key và đối chiếu thất bại đồng bộ.

Đây thường là integration principal hoặc service account, không phải một Role con người. Credential của tích hợp phải ràng buộc với đúng Merchant và scope API.

### Nhóm C — Shipper và đội ngũ giao nhận

Trong logistics thực tế, Pickup Courier, Delivery Courier và Line-haul Driver có thể là ba chức trách khác nhau. Một người có thể kiêm nhiều chức trách, nhưng hệ thống vẫn nên phân biệt quyền theo nhiệm vụ.

![Vietnam&#39;s e-commerce sector set to hit nearly $4 bln in Q4 - VnEconomy](assets/image5.jpeg)

![Vietnam’s e-commerce market records strong growth in first half of 2026 - VnEconomy](assets/image6.jpeg)

![ERP Logistics: Giải pháp tối ưu vận hành hậu cần doanh nghiệp | GHTK - Nền tảng hậu cần chuyên nghiệp](assets/image7.jpeg)

11

### 11. Pickup Courier — Nhân viên lấy hàng

| **Nghiệp vụ**    | **Chi tiết**                                                |
|------------------|-------------------------------------------------------------|
| Nhận nhiệm vụ    | Xem danh sách pickup được Dispatcher phân công              |
| Xem tuyến        | Xem địa chỉ Merchant và thứ tự lấy hàng                     |
| Liên hệ Merchant | Gọi hoặc nhắn trong phạm vi nhiệm vụ                        |
| Check-in         | Ghi nhận đến điểm lấy nếu quy trình yêu cầu                 |
| Xác nhận kiện    | Quét barcode, đối chiếu mã và số lượng kiện                 |
| Kiểm tra ban đầu | Ghi nhận kiện hỏng bao bì, sai kích thước, sai thông tin    |
| Nhận hàng        | Ghi nhận thời điểm và bằng chứng bàn giao vật lý            |
| Pickup thất bại  | Ghi lý do: cửa hàng đóng cửa, không có hàng, sai địa chỉ... |
| Bàn giao kho     | Quét hoặc xác nhận chuyển kiện cho Station/Hub              |
| Báo cáo          | Số điểm đã ghé, số kiện lấy được, số nhiệm vụ thất bại      |

Nguyên tắc: Không được đánh dấu PICKED_UP chỉ vì Shipper bấm nút. Phải có bằng chứng nhận hàng phù hợp, và chỉ người được phân công mới được thực hiện.

### 12. Delivery Courier — Nhân viên giao hàng

Đây là Role tiếp xúc trực tiếp với người nhận.

| **Nghiệp vụ**      | **Chi tiết**                                               |
|--------------------|------------------------------------------------------------|
| Nhận đơn giao      | Xem các vận đơn được phân công                             |
| Xác nhận nhận tải  | Quét kiện được bàn giao từ bưu cục                         |
| Xem tuyến giao     | Danh sách điểm dừng, bản đồ, địa chỉ                       |
| Liên hệ người nhận | Liên hệ theo chính sách bảo vệ số điện thoại               |
| Bắt đầu giao       | Ghi nhận OUT_FOR_DELIVERY khi đủ điều kiện                 |
| Thu COD            | Xác nhận khoản tiền phải thu, phương thức thanh toán       |
| Giao thành công    | Ghi nhận bằng chứng giao hàng (POD)                        |
| Giao thất bại      | Ghi lý do và bằng chứng, cập nhật số lần giao              |
| Giao lại           | Xử lý nhiệm vụ theo phân công mới                          |
| Trả kiện           | Bàn giao kiện chưa giao về Station/Hub                     |
| Nộp COD            | Nộp tiền thu hộ theo quy trình đối soát                    |
| Hiệu suất          | Xem số kiện giao, tỷ lệ thành công, khoản COD còn phải nộp |

Điểm cực kỳ quan trọng: Shipper không được tự sửa giá trị COD, tự xóa nhiệm vụ giao thất bại hoặc tự xác nhận đã nộp tiền nếu phía nhận tiền chưa xác nhận.

### 13. Line-haul Driver — Tài xế trung chuyển

Khác với Shipper giao tận nhà. Tài xế trung chuyển vận chuyển kiện giữa các Hub, trung tâm phân loại hoặc bưu cục.

Nghiệp vụ: nhận chuyến xe, nhận manifest kiện/bao tải, quét bàn giao, xác nhận seal, kiểm tra tải trọng, ghi nhận hành trình, báo trễ hoặc sự cố, xác nhận đến Hub đích và bàn giao số kiện thực nhận.

Không cần có quyền sửa đơn hàng của Merchant hoặc xác nhận giao tận tay người nhận.

### 14. Courier Team Leader — Trưởng nhóm giao nhận

Quản lý năng suất và các trường hợp bất thường trong đội giao nhận.

Có thể xem nhiệm vụ của cả đội, đề xuất phân công lại, hỗ trợ sự cố, kiểm tra số kiện tồn và xác nhận chênh lệch khi bàn giao. Những quyền như thay đổi assignment hoặc chấp nhận thất thoát phải được cấp riêng và ghi audit.

### Nhóm D — Điều phối, bưu cục và kho vận

![深圳倍通智慧供应链有限公司](assets/image8.jpeg)

![AI менеджер доставки: оптимизация последней мили — mypl.pro](assets/image9.jpeg)

![Chuyên trang Chuyển đổi số Sở thông tin và truyền thông Thanh Hóa](assets/image10.jpeg)

7

### 15. Dispatcher — Nhân viên điều phối

Dispatcher là một trong những Role nghiệp vụ quan trọng nhất của hệ thống chuyển phát.

| **Nghiệp vụ**          | **Chi tiết**                                          |
|------------------------|-------------------------------------------------------|
| Quản lý pickup pool    | Xem các đơn chờ lấy hàng                              |
| Gán nhân viên lấy hàng | Phân công Pickup Courier theo khu vực/năng lực        |
| Quản lý delivery pool  | Xem kiện đã sẵn sàng giao                             |
| Phân công giao hàng    | Gán Delivery Courier, khu vực và tuyến                |
| Tối ưu tuyến           | Gom điểm, phân nhóm đơn, tính tuyến                   |
| Tái phân công          | Chuyển đơn khi nhân viên vắng, quá tải hoặc gặp sự cố |
| Theo dõi SLA           | Phát hiện đơn chờ lâu, quá hạn                        |
| Xử lý ngoại lệ         | Điều phối giao lại, chuyển bưu cục hoặc escalations   |
| Theo dõi hiệu suất     | Tải công việc, tỷ lệ pickup/delivery thành công       |

Dispatcher không được tùy ý đổi COD hoặc tạo sự kiện DELIVERED thay Shipper.

### 16. Station Clerk — Nhân viên bưu cục

Bưu cục là điểm nghiệp vụ thực tế: nơi nhận hàng, kiểm tra, nhập kho và bàn giao.

Nghiệp vụ gồm nhận kiện từ khách hoặc Shipper, quét barcode, cân/đo, ghi nhận kiện thực nhận, kiểm tra hàng gửi, tạo manifest, xuất kiện cho tuyến, nhận kiện trả lại, ghi nhận tồn kho và xử lý sai lệch.

Hệ thống phải biết kiện đang nằm ở bưu cục nào, ai nhận, lúc nào, từ ai.

### 17. Sorting Staff — Nhân viên phân loại

Thực hiện scan kiện, phân tuyến, phân bao/túi, xác nhận kiện đã qua dây chuyền và ghi nhận sai tuyến, kiện không đọc được barcode hoặc hư hỏng.

Thông thường, nhân viên phân loại không cần xem dữ liệu tài chính Merchant hoặc toàn bộ thông tin cá nhân của người nhận.

### 18. Hub Supervisor / Warehouse Manager — Quản lý Hub

Quản lý hoạt động trung tâm phân loại, tồn kiện, năng lực xử lý, tình trạng chuyến xe, chênh lệch manifest, báo cáo mất/hỏng và phối hợp điều chuyển hàng.

Các quyết định điều chỉnh tồn kho hoặc xác nhận thiếu hàng nên có lý do, evidence và quy trình phê duyệt.

### 19. Station Manager — Trưởng bưu cục

| **Nghiệp vụ** | **Chi tiết**                                           |
|---------------|--------------------------------------------------------|
| Nhân sự       | Quản lý các ca và nhiệm vụ trong bưu cục               |
| Tồn kho kiện  | Theo dõi kiện vào/ra/tồn                               |
| Điều phối     | Giám sát lấy và giao hàng tại khu vực                  |
| Chuyến xe     | Theo dõi bàn giao và tiếp nhận                         |
| COD           | Kiểm soát việc Shipper nộp COD, không tự ý thay sổ cái |
| Ngoại lệ      | Kiểm tra kiện tồn lâu, kiện sai tuyến, mất/hỏng        |
| KPI           | Theo dõi sản lượng và SLA                              |
| Audit         | Xem nhật ký thao tác thuộc phạm vi bưu cục             |

### 20. Network Operations Manager — Quản lý vận hành vùng/toàn mạng

Quản lý mạng lưới bưu cục, Hub, vùng giao nhận, năng lực tuyến, điều chuyển liên vùng, tỷ lệ giao thành công và SLA toàn mạng.

Có thể định tuyến ngoại lệ hoặc phê duyệt chuyển tuyến theo chính sách, nhưng không tự động có quyền thay đổi tiền COD, lệnh thanh toán hay cấu hình bảo mật hệ thống.

### Nhóm E — Chăm sóc khách hàng, COD, tài chính và kinh doanh

Đây là nhóm dễ bị bỏ sót trong các đồ án Logistics, nhưng nếu thiếu thì hệ thống chỉ là ứng dụng tạo và theo dõi vận đơn, chưa phải một hệ thống vận hành chuyển phát hoàn chỉnh.

### 21. Customer Service Agent — Nhân viên CSKH hãng vận chuyển

| **Nghiệp vụ**      | **Chi tiết**                                               |
|--------------------|------------------------------------------------------------|
| Tra cứu vận đơn    | Tìm kiếm đơn trong phạm vi phân công                       |
| Tiếp nhận hỗ trợ   | Tạo ticket từ Merchant, Sender hoặc Recipient              |
| Xử lý giao chậm    | Tra cứu vị trí cuối cùng và liên hệ bộ phận liên quan      |
| Yêu cầu giao lại   | Gửi yêu cầu và theo dõi kết quả                            |
| Thay đổi thông tin | Tiếp nhận yêu cầu đổi số điện thoại/địa chỉ theo quy trình |
| Xử lý COD          | Tiếp nhận yêu cầu tra soát, không được tự thay số tiền     |
| Xử lý hoàn hàng    | Kiểm tra lý do và thông báo kết quả                        |
| Escalation         | Chuyển vụ việc nghiêm trọng đến bộ phận có thẩm quyền      |
| SLA hỗ trợ         | Quản lý hạn trả lời và xử lý ticket                        |

CSKH cần nhìn thấy dữ liệu để giải quyết vụ việc, nhưng không nên có quyền tùy ý sửa lịch sử tracking hay trạng thái giao nhận.

### 22. Claims & Exception Officer — Nhân viên xử lý khiếu nại, sự cố

Xử lý những trường hợp giao thiếu, hàng hỏng, mất hàng, thu sai COD, thất lạc, giao nhầm và tranh chấp bằng chứng giao hàng.

Quy trình bao gồm mở hồ sơ khiếu nại, thu thập tracking/POD/ảnh/biên bản, xác định trách nhiệm, đánh giá chính sách bồi thường, đề xuất số tiền, chuyển phê duyệt và ghi nhận kết quả.

Người đề xuất bồi thường và người phê duyệt thanh toán không nên mặc định là cùng một người.

### 23. COD Cashier — Nhân viên thu quỹ COD

Khi Delivery Courier thu tiền mặt từ người nhận, tiền đó chưa lập tức trở thành tiền đã đối soát hoặc đã chuyển cho Merchant.

COD Cashier thực hiện tiếp nhận tiền từ Shipper, kiểm đếm, xác nhận khoản nộp, ghi nhận thiếu/thừa, đóng ca và chuyển số liệu cho bộ phận kế toán.

Các trạng thái nghiệp vụ cần phân biệt:

COLLECTED_BY_COURIER → REMITTED_TO_STATION → RECONCILED → SETTLED_TO_MERCHANT

Đây là mô hình luồng tiền minh họa. Tên trạng thái chính thức phải được chốt trong hợp đồng nghiệp vụ của E-Logistics.

### 24. Finance / COD Reconciliation — Kế toán đối soát

| **Nghiệp vụ**       | **Chi tiết**                                           |
|---------------------|--------------------------------------------------------|
| Đối soát Shipper    | So sánh COD phải nộp và thực nộp                       |
| Đối soát đơn        | So sánh delivered shipments và khoản thu thực tế       |
| Thanh toán Merchant | Tính COD đủ điều kiện chuyển trả                       |
| Phí                 | Tính phí vận chuyển, phụ phí, phí hoàn hoặc điều chỉnh |
| Công nợ             | Quản lý phải thu/phải trả                              |
| Bảng kê             | Tạo kỳ đối soát, phiếu và báo cáo                      |
| Hoàn tiền           | Thực hiện refund đã được phê duyệt                     |
| Sai lệch            | Phát hiện COD thiếu/thừa, thanh toán trùng             |
| Audit               | Truy vết các giao dịch và điều chỉnh tài chính         |

Hệ thống cần có ledger (sổ ghi nhận bút toán) và idempotency cho thanh toán. Không được cộng trừ tiền chỉ bằng cách ghi đè một trường cod_amount hoặc merchant_balance.

### 25. Pricing / Billing Administrator — Quản trị bảng giá và cước phí

Chịu trách nhiệm cấu hình biểu phí vận chuyển theo vùng, tuyến, trọng lượng tính cước, loại dịch vụ, COD, phụ phí, hợp đồng Merchant, chiết khấu và thời gian hiệu lực.

Ví dụ, cùng một kiện hàng có thể phát sinh:

\\ \text{Tổng phí}=\text{Cước cơ bản}+\text{Phụ phí}-\text{Ưu đãi} \\

Phải phân biệt phí ước tính khi báo giá, phí chốt theo hợp đồng, phí điều chỉnh sau cân đo và số tiền thực tế thanh toán.

Một đơn hàng đã xác nhận cần lưu snapshot giá áp dụng, không phụ thuộc hoàn toàn vào bảng giá đang có hiệu lực ở thời điểm tra cứu sau này.

### 26. Sales / Key Account Manager — Nhân viên kinh doanh

Chức năng gồm quản lý khách hàng doanh nghiệp, tư vấn dịch vụ, theo dõi hợp đồng, đề xuất bảng giá riêng, quản lý account được giao, xem KPI và hỗ trợ Merchant lớn.

Nhân viên Sales không nên tự duyệt mức giá đặc biệt, tự đổi thông tin ngân hàng Merchant hoặc truy cập dữ liệu tất cả Merchant không thuộc phạm vi quản lý.

### 27. Risk / Fraud / Compliance Officer — Kiểm soát rủi ro

Xử lý các vấn đề gian lận COD, sử dụng tài khoản trái phép, spam vận đơn, thông tin người nhận giả, kiện có dấu hiệu bất thường, truy cập dữ liệu không hợp lệ và các yêu cầu tuân thủ.

Các hành động có thể gồm gắn cờ rủi ro, đề nghị tạm giữ shipment, chuyển thẩm định, xem audit log, kiểm tra giao dịch bất thường và phê duyệt giải phóng theo chính sách.

Role này cần quyền truy cập nhạy cảm nhưng phải bị kiểm soát và ghi audit chặt chẽ.

### Nhóm F — Quản trị nghiệp vụ và quản trị hệ thống

Đặc biệt quan trọng đối với E-Logistics: Business Admin không phải System Admin và cũng không phải Merchant Owner.

### 28. Business Admin — Quản trị nghiệp vụ Logistics

Business Admin quản lý các chính sách và hoạt động của doanh nghiệp chuyển phát, trong phạm vi tổ chức hoặc khu vực được cấp quyền.

| **Nghiệp vụ**         | **Chi tiết**                                          |
|-----------------------|-------------------------------------------------------|
| Merchant management   | Xem, duyệt hoặc quản lý hồ sơ Merchant theo quy trình |
| Service configuration | Quản lý dịch vụ vận chuyển và coverage                |
| Branch management     | Quản lý tổ chức bưu cục, khu vực và Hub               |
| Operational policies  | Cấu hình quy tắc vận hành được phép                   |
| Staff assignment      | Quản lý các vai trò nghiệp vụ trong phạm vi           |
| Logistics monitoring  | Theo dõi shipment, ngoại lệ, tỷ lệ giao hàng          |
| SLA management        | Theo dõi tiêu chuẩn dịch vụ                           |
| Reports               | Tổng hợp doanh thu, sản lượng, chất lượng vận hành    |
| Escalation            | Phê duyệt những ngoại lệ được chính sách cho phép     |

Business Admin không nên có quyền xem private key JWT, sửa trực tiếp database, xóa audit log hoặc tự thao tác thanh toán vượt thẩm quyền.

### 29. Platform Super Admin — Quản trị nền tảng cấp cao

Quản lý cấu hình hệ thống đa tổ chức, tenant, phân vùng, quyền quản trị cấp cao, feature flags và một số chính sách nền tảng.

Đây là vai trò có quyền rất lớn nhưng không nên là quyền vượt qua tất cả quy tắc nghiệp vụ.

Ví dụ: Super Admin không được mặc nhiên ghi nhận giả một đơn đã giao thành công hoặc tự xác nhận một khoản COD đã được Merchant nhận tiền.

Các tác vụ đặc biệt cần quy trình có kiểm soát, lý do, audit và trong một số trường hợp là phê duyệt hai người.

### 30. Identity / Security Administrator — Quản trị danh tính và bảo mật

Quản lý hoặc giám sát provisioning, account status, Realm, quyền Identity được phép, cấu hình MFA, session revocation, credential incidents và các chính sách bảo mật.

Có quyền quản trị danh tính không đồng nghĩa có quyền sở hữu nghiệp vụ Merchant, xem COD hoặc quản lý vận đơn.

Điều này liên quan trực tiếp đến giới hạn IDENTITY_MANAGEMENT/IDENTITY mà dự án bạn đang có.

### 31. IT / Integration Operations — Quản trị kỹ thuật

Nghiệp vụ gồm kiểm tra sức khỏe hệ thống, xử lý webhook thất bại, quản lý cấu hình tích hợp, giám sát hàng đợi, đối chiếu lỗi đồng bộ và vận hành hệ thống.

Quyền Production đặc biệt phải được giới hạn. Không cho phép nhân sự kỹ thuật tự xem toàn bộ thông tin người nhận hoặc impersonate Merchant tùy ý.

### 32. Auditor — Kiểm toán / Giám sát

Role thường chỉ đọc dữ liệu trong phạm vi được phê duyệt.

Có thể xem lịch sử thay đổi, event giao nhận, lịch sử phân quyền, bút toán tài chính và bằng chứng xử lý sự cố. Auditor không được sửa dữ liệu mà mình đang kiểm toán.

### 33. Partner / Franchise Operator — Đối tác nhượng quyền, điểm gửi nhận

Nhiều mạng lưới chuyển phát có các điểm nhận hàng, đại lý hoặc đối tác vận hành.

Đối tác có thể tiếp nhận kiện tại quầy, bàn giao cho hãng, xác nhận số lượng, quản lý kiện tại điểm và xem báo cáo doanh thu dịch vụ của chính điểm đó.

Quyền được giới hạn theo Partner Organization và Station, không mở cho toàn bộ mạng lưới.

## III. Vòng đời của một đơn vận chuyển ngoài thực tế

Đây là phần quan trọng nhất: các Role phía trên phải phối hợp với nhau để hoàn thành một Shipment.

**Luồng giao hàng tiêu chuẩn**

1.  Merchant / Sender

Tạo đơn và yêu cầu vận chuyển

2.  Logistics System

Báo giá, kiểm tra vùng phục vụ, xác nhận đơn

3.  Dispatcher

Phân công nhân viên lấy hàng

4.  Pickup Courier

Đến điểm lấy, quét mã, nhận kiện

5.  Station / Sorting Hub

Nhập kho, cân đo, phân loại, lập manifest

6.  Line-haul Driver

Trung chuyển đến khu vực nhận

7.  Destination Station

Nhận kiện và chuẩn bị phát

8.  Dispatcher

Phân công nhân viên giao

9.  Delivery Courier

Giao hàng, thu COD, ghi nhận POD

10. Recipient

Nhận hàng, xác nhận giao

11. COD / Finance

Nộp tiền, đối soát, thanh toán Merchant

12. Merchant

Theo dõi giao thành công và quyết toán

**Trạng thái Shipment cần quản lý**

Một mô hình tham khảo:

| **Giai đoạn**    | **Trạng thái nghiệp vụ** | **Người/hệ thống tạo sự kiện**  |
|------------------|--------------------------|---------------------------------|
| Chuẩn bị         | DRAFT                    | Merchant                        |
| Xác nhận         | CONFIRMED                | Merchant / System               |
| Chờ lấy          | PICKUP_REQUESTED         | System                          |
| Đã phân công     | PICKUP_ASSIGNED          | Dispatcher                      |
| Đã nhận hàng     | PICKED_UP                | Pickup Courier                  |
| Vào bưu cục      | ORIGIN_RECEIVED          | Station                         |
| Phân loại        | SORTED                   | Sorting Hub                     |
| Trung chuyển     | IN_TRANSIT               | Hub / Driver                    |
| Đến bưu cục phát | DESTINATION_RECEIVED     | Destination Station             |
| Đang giao        | OUT_FOR_DELIVERY         | Delivery Courier                |
| Giao thành công  | DELIVERED                | Courier + bằng chứng            |
| Giao thất bại    | DELIVERY_FAILED          | Courier + lý do                 |
| Yêu cầu hoàn     | RETURN_REQUESTED         | Actor được phép                 |
| Đang hoàn        | RETURNING                | Network Operations              |
| Hoàn thành       | RETURNED                 | Return handover                 |
| Hủy              | CANCELLED                | Actor được phép theo trạng thái |
| Ngoại lệ         | ON_HOLD, LOST, DAMAGED   | Bộ phận có thẩm quyền           |

Đây không phải một chuỗi tuyến tính cố định: đơn có thể giao thất bại rồi quay lại phân công giao, chuyển tuyến, giữ để kiểm tra hoặc bắt đầu hoàn hàng.

Vì vậy, khi thiết kế database, tôi khuyến nghị tách trạng thái hiện tại khỏi lịch sử Tracking Events và Delivery Attempts. Không dùng một trường status duy nhất để thay thế toàn bộ bằng chứng vận hành.

**Những luồng ngoại lệ bắt buộc có**

| **Tình huống**                       | **Quy trình thực tế nên hỗ trợ**                                 |
|--------------------------------------|------------------------------------------------------------------|
| Merchant không có hàng khi đến lấy   | Pickup thất bại, ghi lý do, hẹn lấy lại hoặc hủy                 |
| Sai địa chỉ người nhận               | CS/điều phối xác minh, duyệt chuyển địa chỉ hoặc chuyển xử lý    |
| Người nhận không nghe máy            | Ghi một delivery attempt thất bại, lên lịch lại nếu được phép    |
| Người nhận từ chối                   | Ghi lý do, xác định điều kiện giao lại hay hoàn hàng             |
| Kiện rách/hỏng                       | Ghi nhận biên bản, ảnh, trạng thái kiểm tra và trách nhiệm       |
| Mất kiện                             | Mở điều tra, xác minh điểm quét cuối, xử lý khiếu nại/bồi thường |
| Sai tuyến                            | Quét ngoại lệ, chuyển lại tuyến, cập nhật SLA                    |
| Thu COD thiếu                        | Ghi sai lệch, xử lý theo quỹ và đối soát; không sửa âm thầm      |
| Đã giao nhưng Merchant chưa nhận COD | Mở tra soát tài chính; không sửa trạng thái giao hàng            |
| Đơn hoàn                             | Điều phối hoàn, nhận lại kiện, bàn giao Merchant, đối soát phí   |
| Duplicate request/webhook            | Idempotency, không tạo vận đơn hoặc bút toán trùng               |
| Hủy sau khi đã lấy hàng              | Không xử lý như hủy draft; phải chuyển quy trình thu hồi/hoàn    |

Một hệ thống chỉ có PENDING → SHIPPING → DELIVERED sẽ không phản ánh được phần lớn hoạt động vận hành thực tế.

## IV. Ma trận quyền hạn nghiệp vụ — Role-Permission Matrix

Khi triển khai hệ thống, chúng ta không nên thiết kế một enum duy nhất như:

GUEST \| MERCHANT \| SHIPPER \| ADMIN

Mô hình đó quá đơn giản để xử lý bưu cục, đội vận chuyển, nhiều tổ chức Merchant và phân tách trách nhiệm tài chính.

### 1. Nguyên tắc phân quyền

Một thao tác chỉ được cho phép khi đồng thời đáp ứng:

\\ \text{Allow} = \text{Role Permission} \land \text{Organization Scope} \land \text{Resource Scope} \land \text{Current State} \land \text{Security Conditions} \\

Ví dụ: Delivery Courier có quyền ghi nhận giao thành công, nhưng chỉ đối với kiện được phân công cho chính họ, chưa giao trước đó và đã có bằng chứng giao theo chính sách.

| **Thao tác**             | **Người có thể thực hiện**             | **Điều kiện bắt buộc**                       |
|--------------------------|----------------------------------------|----------------------------------------------|
| Tạo pickup point         | Merchant Owner/Operator được cấp quyền | Membership ACTIVE trong Merchant             |
| Xem pickup point         | Merchant thành viên hợp lệ             | Đúng Merchant Organization                   |
| Tạo shipment             | Sender/Merchant Operator               | Dịch vụ được phép, dữ liệu hợp lệ            |
| Phân công Pickup         | Dispatcher                             | Đúng khu vực, nhiệm vụ đủ điều kiện          |
| Xác nhận đã lấy hàng     | Pickup Courier                         | Đúng assignment, có bằng chứng               |
| Nhập kho                 | Station Clerk                          | Đúng station và quyền custody                |
| Phân loại kiện           | Sorting Staff                          | Kiện hợp lệ trong scope Hub                  |
| Phân công Delivery       | Dispatcher                             | Kiện đủ điều kiện giao                       |
| Xác nhận DELIVERED       | Delivery Courier                       | Assignment hợp lệ, có POD                    |
| Ghi nhận thu COD         | Delivery Courier                       | Đúng kiện, số tiền đã chốt                   |
| Xác nhận Shipper nộp COD | COD Cashier                            | Đúng ca/quỹ, giao dịch được đối chiếu        |
| Duyệt đối soát Merchant  | Finance có thẩm quyền                  | Kỳ đối soát hợp lệ, không trùng              |
| Phê duyệt bồi thường     | Claims Approver                        | Chính sách, hạn mức và phân tách trách nhiệm |
| Cấu hình cước phí        | Pricing Admin                          | Phạm vi hợp đồng và thời gian hiệu lực       |
| Quản lý Merchant staff   | Merchant Owner/Manager được ủy quyền   | Chỉ trong tổ chức hiện hành                  |
| Quản lý quyền Identity   | Identity Administrator                 | Không mặc nhiên có quyền Logistics           |
| Xem audit toàn nền tảng  | Auditor được cấp phép                  | Đúng phạm vi kiểm toán, chỉ đọc              |

**Một số nguyên tắc không được phá vỡ**

Merchant A không được xem đơn của Merchant B. Ngay cả khi họ biết chính xác UUID của đơn.

Business Admin không mặc định được thay đổi COD. Quyền vận hành và quyền tài chính phải tách rời.

Shipper không được tự tạo assignment rồi xác nhận DELIVERED. Tránh tự cấp quyền để giả lập hoàn thành nhiệm vụ.

System Admin không được sửa audit history. Tác vụ hỗ trợ đặc biệt phải có cơ chế kiểm soát, không phải bypass tất cả nghiệp vụ.

Recipient không được tự sửa shipment đã chốt. Mọi yêu cầu đổi địa chỉ/giao lại cần đi qua workflow được phép.

## V. Các module nghiệp vụ mà hệ thống Logistics hoàn chỉnh cần có

Đây là danh sách module ở cấp hệ thống, không đồng nghĩa E-Logistics phải phát triển tất cả trong phiên bản đầu tiên.

| **Module**                  | **Nghiệp vụ chính**                      | **Nhóm Role**                |
|-----------------------------|------------------------------------------|------------------------------|
| Identity & Access           | Account, session, membership, permission | Tất cả                       |
| Merchant Management         | Tổ chức, nhân viên, điểm lấy, cấu hình   | Merchant, Business Admin     |
| Address & Coverage          | Địa chỉ, vùng phục vụ, geocoding         | Guest, Merchant, Operations  |
| Pricing & Quotation         | Báo giá, bảng giá, hợp đồng              | Merchant, Pricing            |
| Order & Shipment Management | Tạo, xác nhận, hủy, sửa theo quyền       | Merchant, Retail Sender      |
| Parcel & Label              | Kiện hàng, barcode, cân đo, tem nhãn     | Merchant Warehouse, Station  |
| Pickup Management           | Yêu cầu lấy, phân công, bàn giao         | Dispatcher, Pickup Courier   |
| Station & Hub Operations    | Nhập/xuất kho, phân loại, manifest       | Station, Sorting             |
| Transportation Management   | Chuyến xe, trung chuyển, tuyến           | Driver, Network Operations   |
| Last-mile Delivery          | Phân công, giao lại, POD                 | Delivery Courier, Dispatcher |
| Tracking & Notifications    | Tracking events, thông báo, ETA          | Guest, Merchant, Recipient   |
| COD Management              | Thu tiền, nộp tiền, sai lệch             | Courier, Cashier, Finance    |
| Billing & Settlement        | Phí, công nợ, thanh toán Merchant        | Finance, Merchant Finance    |
| Returns & Reverse Logistics | Hoàn hàng, giao lại người gửi            | Merchant, Operations         |
| Customer Support & Claims   | Ticket, tra soát, bồi thường             | CS, Claims                   |
| Analytics & SLA             | KPI, tồn kho, giao thất bại, báo cáo     | Managers, Admin              |
| Partner & Integrations      | API, webhook, đại lý, marketplace        | Enterprise, Partner          |
| Security & Audit            | Truy vết thay đổi, kiểm soát rủi ro      | Security, Auditor            |

**Những module nâng cao**

Một doanh nghiệp lớn có thể còn có WMS chuyên sâu, quản lý kho fulfillment, gom/chia đơn tự động, routing optimization, GPS tracking, dự báo năng lực mạng lưới, vận tải quốc tế, bảo hiểm hàng hóa, quản lý đối tác 3PL, quản trị đội xe, phân tích gian lận và ứng dụng Shipper offline-first.

Những thứ này nên được thiết kế theo khả năng mở rộng, không nhất thiết phải hoàn thiện hết trước khi có một luồng giao nhận cơ bản hoạt động tốt.

## VI. Database: Các thực thể nghiệp vụ quan trọng

Để triển khai đúng, chúng ta cần tách biệt danh tính, tổ chức, vận hành, hàng hóa và tiền.

| **Nhóm**     | **Entity dự kiến**                                        | **Ý nghĩa**                                  |
|--------------|-----------------------------------------------------------|----------------------------------------------|
| Identity     | user_account, auth_session                                | Danh tính và phiên đăng nhập                 |
| Organization | merchant_profile, merchant_membership                     | Merchant và người được cấp quyền             |
| Location     | address, pickup_point, admin_catalog_version              | Địa chỉ và điểm lấy hàng                     |
| Commercial   | customer_order, shipment, parcel                          | Đơn mua bán, yêu cầu vận chuyển, kiện vật lý |
| Pricing      | rate_card, service_zone, shipment_quote                   | Cước phí và snapshot báo giá                 |
| Operations   | station, hub, route, assignment                           | Cơ sở vận hành và nhiệm vụ                   |
| Tracking     | tracking_event, delivery_attempt, proof_of_delivery       | Lịch sử vận chuyển và chứng cứ               |
| Handover     | manifest, handover_event                                  | Bàn giao giữa người và điểm vận hành         |
| Returns      | return_case, return_event                                 | Quy trình hoàn hàng                          |
| Finance      | cod_collection, cash_remittance, settlement, ledger_entry | Dòng tiền và đối soát                        |
| Support      | support_ticket, claim_case                                | Hỗ trợ và khiếu nại                          |
| Governance   | audit_event, approval_request                             | Truy vết và phê duyệt                        |

Đây là mô hình conceptual, chưa phải danh sách bảng được phép tạo ngay. Phải kiểm tra canonical SDD để tránh trùng trách nhiệm giữa Merchant, Identity, Orders, Shipment, Finance và các feature khác.

**Phân biệt ba thực thể hay bị thiết kế sai**

Order — Đơn mua bán

Khách mua hai sản phẩm từ cửa hàng A

Shipment — Yêu cầu vận chuyển

Cửa hàng giao hai sản phẩm đó đến người nhận

Parcel 01

Kiện hàng vật lý số 1

Parcel 02

Kiện hàng vật lý số 2

Không nên đồng nhất order ID với tracking ID hay parcel ID.

## VII. Minh họa nghiệp vụ thực tế: Một đơn COD giao thất bại rồi giao lại

Giả sử Merchant A bán một món hàng với COD 350.000đ. Đây là số tiền minh họa, không phải mức phí hay chính sách của một hãng cụ thể.

| **Bước** | **Role**          | **Hoạt động**                                      |
|----------|-------------------|----------------------------------------------------|
| 1        | Merchant Operator | Tạo yêu cầu giao hàng, COD 350.000đ                |
| 2        | Pricing System    | Xác nhận dịch vụ, tính cước theo chính sách        |
| 3        | Merchant          | Xác nhận shipment và in nhãn                       |
| 4        | Dispatcher        | Phân công Courier đến lấy                          |
| 5        | Pickup Courier    | Đến lấy và quét nhận kiện                          |
| 6        | Station Clerk     | Nhận kiện, xác nhận số lượng                       |
| 7        | Sorting Hub       | Phân tuyến đến khu vực người nhận                  |
| 8        | Delivery Courier  | Nhận kiện giao                                     |
| 9        | Recipient         | Không có mặt tại địa chỉ                           |
| 10       | Delivery Courier  | Ghi DELIVERY_FAILED với lý do phù hợp              |
| 11       | Dispatcher        | Đánh giá điều kiện và lên lịch giao lại            |
| 12       | Delivery Courier  | Giao thành công lần thứ hai, thu 350.000đ          |
| 13       | Recipient         | Xác nhận nhận hàng theo chính sách POD             |
| 14       | COD Cashier       | Nhận khoản tiền Shipper nộp                        |
| 15       | Finance           | Đối soát COD và các khoản phí                      |
| 16       | Merchant Finance  | Xem và xác nhận kỳ đối soát thuộc phạm vi của mình |

Có ít nhất ba loại sự kiện độc lập:

- Delivery Event: Giao thành công hay thất bại.

- Custody Event: Kiện đang do ai hoặc bưu cục nào giữ.

- Financial Event: Tiền COD đang do ai giữ và đã đối soát hay chưa.

Một kiện có thể DELIVERED nhưng COD vẫn chưa được nộp hoặc chưa thanh toán cho Merchant. Vì vậy không được gộp ba tiến trình này thành một trạng thái duy nhất.

> **Phạm vi cập nhật 2026-10-26:** Mục 8.9 (Nhóm A), 8.10 (Nhóm B) và 8.11 (Nhóm C/D) quy định Target Scope mới nhất. Các danh sách P0/P1/P2 trước đó chỉ là baseline lịch sử và phân kỳ, không cắt bỏ nghiệp vụ FULL.

## VIII. Đối với E-Logistics Capstone, nên triển khai các Role nào?

Để đạt mức thực tế mà vẫn phù hợp đồ án, tôi đề xuất chia thành hai phạm vi: Role cần làm trước và Role chuyên sâu mở rộng sau.

**Nhóm Role ưu tiên cho bản Logistics**

hãy tiếp tục với mục "VIII. Đối với E-Logistics Capstone, nên triển khai các Role nào?" đang triển khai dở dang đi

## VIII. Đối với E-Logistics Capstone, nên triển khai các Role nào?

Sau khi phân tích hơn 30 vai trò có thể xuất hiện trong một doanh nghiệp chuyển phát thực tế, tôi đề xuất E-Logistics không triển khai toàn bộ các Role ngay từ đầu, nhưng cũng không nên đơn giản hóa hệ thống chỉ còn MERCHANT, SHIPPER và ADMIN.

Mục tiêu là xây dựng nền tảng Logistics có độ bao phủ nghiệp vụ thực tế cao, phục vụ **cả Retail Sender đăng ký tài khoản cá nhân lẫn Merchant đăng ký theo tổ chức**, hỗ trợ đầy đủ vòng đời vận chuyển và các luồng ngoại lệ. P0/P1/P2 là **thứ tự ưu tiên triển khai**, không phải quyền được lược bỏ các capability đã xác định bắt buộc. Các quyết định dưới đây là định hướng nghiệp vụ do Project Owner xác lập, phải đối chiếu canonical SDD trước khi thay đổi code, API, schema hoặc Identity contract.

### 8.1. Danh sách Role đề xuất cho E-Logistics

Tôi chia các Role thành ba mức ưu tiên.

**Nhóm P0 — Vai trò cốt lõi (mở rộng đầy đủ Nhóm B theo target)**

Đây là nhóm cần có để phát triển những chức năng đầu tiên, đặc biệt là Feature 003 — Merchant & Address.

| **Role**          | **Trách nhiệm chính**                                            | **Portal**     |
|-------------------|------------------------------------------------------------------|----------------|
| Guest             | Tracking giới hạn, ước tính phí, coverage, bưu cục, dịch vụ, liên hệ hỗ trợ, đăng ký tài khoản cá nhân; application Merchant sau đăng nhập; **không tự tạo Shipment** | Public |
| Retail Sender     | Đăng ký cá nhân; hồ sơ, địa chỉ, gửi lẻ, pickup, báo giá, thanh toán, theo dõi, hủy, tra soát, hoàn và chứng từ | Sender |
| Merchant Owner    | Quản lý tổ chức, nhân viên, điểm lấy, toàn bộ đơn thuộc Merchant | Merchant       |
| Merchant Operator | Tạo/import/sync đơn, báo giá, xác nhận, in nhãn, bàn giao, tracking, ngoại lệ và COD trong phạm vi | Merchant |
| Merchant Manager | Quản lý nhân sự được ủy quyền, pickup point, phê duyệt sửa/hủy, hiệu suất và phối hợp giao lại | Merchant |
| Business Admin    | Quản trị và giám sát nghiệp vụ Logistics theo quyền được cấp     | Admin          |
| Identity Admin    | Quản trị tài khoản, phiên, phân quyền Identity được phép         | Internal Admin |

Lưu ý: Có một Role trong danh sách không có nghĩa chúng ta phải triển khai ngay mọi chức năng của Role đó.

Ví dụ: Trong Feature 003, Merchant Owner và Merchant Operator có thể quản lý pickup point, nhưng các chức năng tạo shipment chỉ triển khai khi đến Feature tương ứng.

**Nhóm P1 — Vai trò vận hành Logistics bắt buộc cho luồng giao nhận hoàn chỉnh**

**Merchant Warehouse Staff (Nhóm B)** — Role nhân sự kho Merchant: nhận danh sách chuẩn bị, lấy/kiểm hàng, đóng gói, cân đo, dán/quét nhãn, ghi sai lệch, lập manifest và bàn giao cho Courier. Không phải Station Clerk và không tự ghi sự kiện pickup của Courier.

**Merchant Support (Nhóm B)** — Role CSKH Merchant: tiếp nhận Recipient, kiểm tra Shipment, tạo/theo dõi ticket với CSKH E-Logistics, yêu cầu giao lại và theo dõi hoàn hàng. Không được sửa COD/POD hoặc duyệt bồi thường.


![Đưa công nghệ vào vận hành - Tối ưu thời gian chia chọn hàng hoá tại kho | GHTK - Nền tảng hậu cần chuyên nghiệp](assets/image11.jpeg)

Dispatcher — Nhân viên điều phối

Phân công lấy hàng, giao hàng, theo dõi nhiệm vụ và xử lý điều phối lại.

![Về chúng tôi | 247Express - Nhanh hơn bạn nghĩ](assets/image12.jpeg)

Pickup Courier — Nhân viên lấy hàng

Nhận nhiệm vụ, đến Merchant lấy kiện, quét mã, ghi nhận bằng chứng và bàn giao.

![Efforts to innovate and improve quality will create momentum for Vietnam Post to grow strongly.](assets/image13.jpeg)

Station Operator — Nhân viên bưu cục

Nhận kiện, kiểm tra, nhập/xuất, quét barcode và xác nhận bàn giao.

![Giao hàng toàn quốc ĐỒNG GIÁ 18K cùng Giaohangnhanh trên Haravan](assets/image14.jpeg)

Delivery Courier — Nhân viên giao hàng

Nhận đơn giao, giao hàng, thu COD, ghi nhận POD và xử lý giao thất bại.

![Hành trình nỗ lực nâng tầm chất lượng dịch vụ và trải nghiệm khách hàng của J&amp;T Express | Báo điện tử Tiền Phong](assets/image15.jpeg)

Recipient — Người nhận hàng

Theo dõi đơn, nhận hàng, thanh toán COD và yêu cầu hỗ trợ trong phạm vi được phép.

Recipient là **External Business Actor**: chỉ tra cứu trạng thái công khai giới hạn như Guest; nhận hàng/thu COD/POD do Courier thực hiện và ghi nhận; phản ánh đơn của Merchant đi qua **Merchant Support**, không cần Recipient Account hay Recipient Portal. Trường hợp đơn do Retail Sender gửi không có Merchant, kênh hỗ trợ trực tiếp của Logistics phải được quy định riêng.

Đối với đồ án, một tài khoản nhân viên có thể đồng thời mang quyền PICKUP_COURIER và DELIVERY_COURIER. Không cần xây dựng hai ứng dụng Shipper riêng biệt.

Tuy nhiên, hệ thống phải phân biệt hai quyền lấy hàng và giao hàng, cũng như assignment tương ứng.

**Nhóm P2 — Vai trò hoàn thiện vận hành, tài chính và tích hợp**

**Enterprise/API Client (Nhóm B, integration principal)** — Năng lực tích hợp API đầy đủ là Target CORE: tạo/đồng bộ Shipment, quote, label, tracking/webhook, idempotency và đối chiếu lỗi. Đây không phải tài khoản đăng nhập của nhân viên; triển khai API theo dependency, kiểm soát credential và scope Merchant.


| **Role**               | **Nghiệp vụ**                                                      |
|------------------------|--------------------------------------------------------------------|
| Merchant Finance       | Đầy đủ COD, phí, đối soát, công nợ, hóa đơn/chứng từ, tra soát và báo cáo Merchant |
| COD Cashier            | Nhận tiền COD từ Shipper, ghi nhận nộp tiền và sai lệch            |
| Finance Operator       | Đối soát COD, công nợ, khoản thanh toán Merchant                   |
| Customer Service Agent | Hỗ trợ Merchant/Recipient, xử lý ticket và yêu cầu giao lại        |
| Claims Officer         | Xử lý thất lạc, hư hỏng, khiếu nại và bồi thường                   |
| Station Manager        | Giám sát tồn kiện, vận hành và nhân viên tại bưu cục               |
| Auditor                | Tra cứu lịch sử hoạt động và audit theo phạm vi, không sửa dữ liệu |

Những Role như Hub Supervisor, Line-haul Driver, Network Manager, Pricing Admin, Risk Officer và Partner Operator có thể được bổ sung khi chúng ta triển khai các module vận hành nâng cao.

Khuyến nghị quy mô cho đồ án

**11**

Vai trò/chủ thể ưu tiên P0 + P1

**7**

Vai trò mở rộng P2

Đây là phân nhóm để lập kế hoạch, chưa phải danh sách Role đã được phê duyệt trong canonical SDD. Guest và Recipient **không có tài khoản nội bộ theo phạm vi hiện tại**; Retail Sender **bắt buộc là tài khoản cá nhân đăng ký**. Khách walk-in không có tài khoản được nhân viên Station tiếp nhận trực tiếp tại quầy theo quy trình có xác minh, **không** thông qua API Guest tự tạo Shipment.

### 8.2. Thiết kế Role không nên phụ thuộc trực tiếp vào giao diện

Tôi đề xuất phân biệt ba khái niệm:

Account (Tài khoản): Danh tính của một người hoặc service.

Role (Vai trò): Nhóm quyền mà người đó có thể được cấp.

Membership/Assignment (Phạm vi): Xác định người đó có quyền trong tổ chức, bưu cục hoặc nhiệm vụ nào.

Ví dụ:

User Account A

Một danh tính đăng nhập duy nhất

Merchant A

MERCHANT_OWNER

ACTIVE

Merchant B

MERCHANT_OPERATOR

ACTIVE

Authorization theo Membership đang hoạt động

Cùng một người có thể xem tất cả đơn Merchant A nhưng chỉ được tạo đơn ở Merchant B.

Điều này giúp xử lý các tình huống thực tế như một người quản lý nhiều cửa hàng hoặc một nhân viên làm việc cho nhiều tổ chức.

Đối với nhân sự Logistics, phạm vi tương tự có thể là Station, Hub, Region, Team hoặc Assignment.

Không nên gắn merchant_id cứng vào toàn bộ user account rồi coi đó là nguồn phân quyền duy nhất.

**Bổ sung mô hình loại khách đăng ký:** Retail Sender sử dụng `user_account` cá nhân với phạm vi sở hữu Shipment của chính họ, không bắt buộc `merchant_membership`. Merchant là tổ chức có membership/permission theo tenant. Một cá nhân có thể đồng thời là Retail Sender và thành viên một/nhiều Merchant nếu được cấp quyền, nhưng hai bối cảnh vận hành không được trộn dữ liệu. Tên entity, cột hoặc API cụ thể phải do canonical SDD quyết định.

### 8.3. Ma trận quyền cốt lõi dành cho E-Logistics

Ma trận dưới đây là đề xuất nghiệp vụ, chưa phải các mã permission đã được chấp nhận trong Feature 002 hoặc 003.

Ký hiệu: ✓ = được phép khi thỏa mãn các điều kiện về phạm vi/trạng thái; — = không được phép mặc định; \* = tùy quyền được cấp tường minh.

| **Nghiệp vụ**          | **Merchant Owner** | **Merchant Operator** | **Business Admin** | **Dispatcher** | **Courier** |
|------------------------|--------------------|-----------------------|--------------------|----------------|-------------|
| Quản lý pickup point   | ✓                  | ✓                     | \*                 | —              | —           |
| Tạo shipment           | ✓                  | ✓                     | \*                 | —              | —           |
| Xem shipment           | ✓                  | ✓                     | \*                 | ✓              | \*          |
| Hủy shipment           | ✓                  | \*                    | \*                 | —              | —           |
| Phân công lấy hàng     | —                  | —                     | \*                 | ✓              | —           |
| Xác nhận PICKED_UP     | —                  | —                     | —                  | —              | ✓           |
| Phân công giao hàng    | —                  | —                     | \*                 | ✓              | —           |
| Xác nhận DELIVERED     | —                  | —                     | —                  | —              | ✓           |
| Ghi nhận giao thất bại | —                  | —                     | —                  | —              | ✓           |
| Xem COD                | ✓                  | \*                    | \*                 | \*             | \*          |
| Điều chỉnh COD         | —                  | —                     | \*                 | —              | —           |
| Quản lý Merchant staff | ✓                  | —                     | \*                 | —              | —           |

**Bổ sung quyền Nhóm A và tiếp nhận tại quầy (đề xuất):**

| Nghiệp vụ | Guest | Retail Sender | Merchant Owner / Operator | Station Operator | Recipient |
|---|---|---|---|---|---|
| Tra tracking công khai giới hạn | ✓ | ✓ | ✓ | ✓ | ✓ |
| Đăng ký tài khoản cá nhân | ✓ | — (đã có account) | — | — | — |
| Đăng ký/onboard Merchant | ✓ | * (qua quy trình) | * | — | — |
| Tự tạo Shipment qua nền tảng | — | ✓ (của mình) | ✓ (theo Merchant scope) | * (walk-in tại quầy) | — |
| Quản lý địa chỉ gửi thường dùng | — | ✓ (của mình) | * (theo Merchant) | — | — |
| Xem/hủy Shipment đã tạo | — | ✓ (theo state) | ✓ (theo scope/state) | * (theo tác vụ) | — |
| Tạo ticket tra soát | * (liên hệ công khai) | ✓ (đơn của mình) | * (người được cấp quyền) | * (tại quầy) | — (qua Merchant Support) |

`✓` chỉ có hiệu lực khi đúng resource/state/security; `*` đòi hỏi workflow/quyền được cấp. **Guest không có quyền tạo vận đơn trực tuyến kể cả bằng OTP.** Walk-in là thao tác của nhân viên được ủy quyền, có xác minh người gửi, biên nhận, thanh toán và custody; không cấp Role đăng nhập cho walk-in Sender.

Một số thao tác như điều chỉnh COD có thể còn yêu cầu phê duyệt tài chính và phân tách trách nhiệm; quyền Business Admin đơn thuần không đủ.

Đối với Courier, quyền chỉ có hiệu lực trên nhiệm vụ đã được phân công và còn hợp lệ. Đối với Merchant, quyền chỉ có hiệu lực trong đúng tổ chức.

### 8.4. Cần có những Portal nào?

Không nên tạo một website riêng cho mọi Role. Thay vào đó, có thể tổ chức giao diện theo nhóm người dùng và quy trình nghiệp vụ.

Public Portal

Guest và Recipient có thể tra cứu vận đơn công khai ở mức thông tin giới hạn. **Guest** được báo giá, kiểm tra vùng phục vụ, tra cứu Station/Hub và dịch vụ, liên hệ hỗ trợ, đăng ký tài khoản cá nhân; nộp đơn Merchant sau đăng nhập. **Recipient** không có account/Portal riêng; khiếu nại Shipment của Merchant đi qua Merchant Support; không được tự tạo/sửa Shipment.

Sender Portal

Retail Sender đã đăng ký: hồ sơ cá nhân, địa chỉ thường dùng, báo giá, tạo Shipment, yêu cầu lấy hàng, thanh toán, tracking, hủy theo trạng thái, hỗ trợ/tra soát, hoàn hàng, lịch sử và biên nhận. Có thể chung frontend với Public/Merchant nhưng phải cách ly quyền theo principal và resource owner.

Merchant Portal

Owner, Manager, Operator, Warehouse Staff, Finance, Support: hồ sơ tổ chức, phân quyền/nhân sự, chi nhánh, điểm lấy, đơn hàng/kiện, bàn giao, COD, báo cáo, khiếu nại và cấu hình API theo quyền.

Operations Portal

Dispatcher, Station Operator, Station Manager: nhiệm vụ lấy/giao, quét kiện, bưu cục, tồn kho, tuyến và **nhận ký gửi walk-in tại quầy** (Station staff tạo theo quy trình nội bộ).

Courier Interface

Pickup/Delivery Courier: nhiệm vụ cá nhân, quét mã, cập nhật lấy/giao hàng, POD và COD. Có thể bắt đầu bằng responsive web/PWA.

Administration Portal

Business Admin, Identity Admin, Finance, CS, Auditor: sử dụng các phân hệ riêng, hiển thị theo quyền.

Các Portal này có thể cùng nằm trong một frontend project, sử dụng routing, layouts và authorization chung. Không cần tạo repository riêng cho mỗi Portal; Sender Portal là bối cảnh mới trong cùng giải pháp. Không coi số lượng Portal là số lượng repository.

Điều quan trọng là backend vẫn xác minh quyền ở từng API; không được coi việc ẩn một nút trên giao diện là biện pháp phân quyền.

### 8.5. Lộ trình triển khai nghiệp vụ thực tế cho đồ án

Tôi đề xuất tổ chức thành bốn mốc sản phẩm.

| **Mốc**                  | **Phạm vi nghiệp vụ**                                      | **Kết quả cần đạt**                             |
|--------------------------|------------------------------------------------------------|-------------------------------------------------|
| M1 — Identity & Sender/Merchant Foundation | Tài khoản cá nhân Retail Sender; đăng ký Merchant, membership, điểm lấy, địa chỉ, phân quyền tổ chức; public Guest | Retail Sender và Merchant đăng ký/đăng nhập đúng scope; Merchant Owner/Manager phân quyền theo tổ chức và kho/điểm lấy; Guest xem tiện ích công khai |
| M2 — Shipment Creation   | Báo giá, gửi lẻ Retail Sender, tạo Shipment Merchant, Parcel, xác nhận, in nhãn, thanh toán/cước và biên nhận; walk-in tại Station | Retail Sender và Merchant tạo Shipment hợp lệ; Merchant Operator import/sync, Warehouse chuẩn bị và bàn giao kiện; Station nhận ký gửi walk-in có custody; tích hợp API theo dependency |
| M3 — Pickup & Delivery   | Dispatcher, Pickup/Delivery Courier, tracking, POD         | Đơn đi từ lấy hàng đến giao thành công          |
| M4 — COD & Exceptions    | Thu COD, bàn giao tiền, đối soát, giao thất bại, hoàn hàng | Có luồng tài chính và xử lý ngoại lệ, Merchant Finance đối chiếu, Merchant Support theo dõi ticket/return    |

Các mốc này là nhóm năng lực sản phẩm; thứ tự task bên trong vẫn phải tuân theo dependency và contract của SDD.

**M1 — Identity & Sender/Merchant Foundation**

Retail Sender đăng ký/đăng nhập tài khoản cá nhân, quản lý hồ sơ và địa chỉ thường dùng. Guest xem toàn bộ tiện ích công khai đã chốt nhưng không tạo Shipment trực tuyến. Merchant đăng ký theo tổ chức và có membership riêng. Cần giữ phân tách rõ principal/organization scope và personal shipment ownership.

Demo mong muốn

Merchant A đăng nhập → tạo điểm lấy hàng → nhập địa chỉ → xác nhận kết quả kiểm tra → lưu → reload → thấy điểm lấy.

Merchant B đăng nhập → cố truy cập pickup point của A → bị từ chối.

Điều kiện: Merchant membership, authoritative eligibility và các API liên quan đã được triển khai đúng. Không dùng Demo principal để vượt qua giới hạn Identity hiện tại.

**M2 — Shipment Creation**

Retail Sender chọn địa chỉ cá nhân, dịch vụ, nhận báo giá, tạo Shipment gửi lẻ, yêu cầu pickup hoặc chọn ký gửi bưu cục, theo dõi và quản lý thanh toán theo phương thức hỗ trợ; lưu chứng từ. Người không đăng ký chỉ được ký gửi trực tiếp tại Station/Hub có điểm giao dịch và nhân viên thẩm quyền, nhận biên nhận.

Merchant chọn điểm lấy đã lưu, nhập thông tin người nhận, hàng hóa và COD, nhận báo giá, xác nhận vận chuyển rồi in nhãn.

Khi xác nhận, hệ thống phải kiểm soát trạng thái, idempotency và lưu các snapshot nghiệp vụ cần thiết.

**M3 — Pickup & Delivery**

Dispatcher phân công Courier lấy hàng; Courier quét nhận; bưu cục tiếp nhận; Dispatcher phân công giao; Courier ghi nhận giao thành công có POD.

Một người được cấp cả hai vai trò Courier có thể thực hiện nhiều loại nhiệm vụ, nhưng các bước bàn giao vẫn phải có bằng chứng.

**M4 — COD & Exceptions**

Courier thu tiền, nộp về điểm vận hành, Finance đối soát, Merchant xem trạng thái thanh toán.

Song song, hệ thống hỗ trợ giao thất bại, giao lại, hoàn hàng và tra soát để sản phẩm không chỉ chạy được happy path.

### 8.6. Danh sách mở rộng lịch sử (xem Target FULL mới nhất tại 8.9–8.12)

Đối với E-Logistics, tôi khuyến nghị trì hoãn các vai trò chuyên sâu sau cho đến khi chúng thực sự cần thiết:

| **Role**                    | **Vì sao có thể triển khai sau?**                                                                 |
|-----------------------------|---------------------------------------------------------------------------------------------------|
| Line-haul Driver            | Có thể mô phỏng custody/transfer trong luồng đầu tiên trước khi xây dựng quản lý chuyến xe đầy đủ |
| Hub Supervisor              | Chưa cần dashboard Hub chuyên sâu khi quy mô ban đầu nhỏ                                          |
| Fleet Manager               | Chưa cần quản lý bảo dưỡng, nhiên liệu, phương tiện                                               |
| Pricing Administrator riêng | Có thể dùng cấu hình giá được quản trị có kiểm soát trong giai đoạn đầu                           |
| Claims Approver riêng       | Chỉ bắt buộc khi triển khai phê duyệt bồi thường thực tế                                          |
| Risk/Fraud Officer          | Có thể bổ sung khi đã có dữ liệu giao dịch và quy trình xử lý rủi ro                              |
| Partner/Franchise Operator  | Chưa cần nếu phạm vi ban đầu không bao gồm đại lý hoặc nhượng quyền                               |

Trì hoãn một Role không đồng nghĩa cho phép bỏ qua các quy tắc nghiệp vụ tương ứng. Ví dụ, khi chưa xây dựng Finance Portal đầy đủ, cũng không được để Dispatcher tự ý xác nhận thanh toán COD.

### 8.7. Áp dụng trực tiếp vào Feature 003 hiện tại

Chúng ta đã đến bước T-003-01 — Merchant/Address Readiness & Authority Decision.

Tôi đề xuất giới hạn review đầu tiên vào các vai trò thực sự tham gia Feature 003:

| **Role**          | **Quyền đề xuất trong Feature 003**                                                   |
|-------------------|---------------------------------------------------------------------------------------|
| Guest             | Tra cứu catalog/vùng phục vụ công khai nếu contract cho phép; không CRUD pickup point, không tự tạo Shipment |
| Retail Sender     | Quyền Identity cá nhân và địa chỉ cá nhân khi feature tương ứng được phê duyệt; không có quyền CRUD pickup point của Merchant |
| Merchant Owner    | Tạo, xem, cập nhật và quản lý pickup point của tổ chức                                |
| Merchant Operator | Tạo, xem, sửa pickup point nếu được tổ chức cấp quyền                                 |
| Business Admin    | Chỉ quản lý Merchant/pickup point khi có business permission được cấp tường minh      |
| Identity Admin    | Quản lý danh tính theo phạm vi Identity; không tự có quyền sửa pickup point           |
| Shipper           | Không có quyền quản trị pickup point ở Feature 003                                    |
| Recipient         | Không có quyền CRUD pickup point của Merchant                                         |

**Những quyết định cần chốt trước khi Codex code Feature 003**

Một là: Merchant Authority. Cần xác định nguồn dữ liệu membership, ai được thêm thành viên, cách thu hồi quyền và cách Identity cho phép tài khoản Merchant đăng nhập mà không mở rộng IDENTITY_MANAGEMENT tùy tiện.

Hai là: Business Admin. Cần xác định Role này được quản lý những Merchant nào và hành động nào thực sự có quyền.

Ba là: Địa chỉ Việt Nam. Cần chốt phiên bản dữ liệu hành chính, cấu trúc địa chỉ, cách lưu địa chỉ lịch sử và quy tắc chuẩn hóa.

Bốn là: Geocoding. Cần chốt provider, độ tin cậy, xử lý địa chỉ mơ hồ, timeout và trường hợp người dùng xác nhận thủ công.

Năm là: Contract/API/Database. Cần thống nhất create/read/update, archive nếu có, idempotency, optimistic concurrency, tenant isolation và acceptance criteria.

Sau khi chốt, mới đủ căn cứ để thiết kế và triển khai M1 một cách an toàn.

### 8.8. Đề xuất cuối cùng của tôi cho E-Logistics

Tôi không đề xuất xây dựng 33 loại tài khoản ngay từ đầu. Tôi đề xuất một kiến trúc mà Role, Permission, Organization Membership và Resource Assignment là các khái niệm độc lập.

Điều đó cho phép hệ thống có số lượng Role ban đầu vừa đủ, nhưng không phải viết lại toàn bộ phân quyền khi sau này bổ sung Dispatcher, Station Manager, Finance, Auditor hoặc Partner.

Về mức độ hoàn thiện sản phẩm, chúng ta nên ưu tiên:

Public Guest + Identity Retail Sender/Merchant → Shipment Creation (Sender/Merchant/walk-in) → Pickup/Delivery → Tracking/POD → COD/Settlement → Exceptions/Returns → Reporting.

Khi hoàn thành các mốc này cùng với các luồng phân quyền và kiểm thử thực tế, E-Logistics sẽ có nền tảng của một hệ thống vận hành chuyển phát chứ không chỉ là website quản lý đơn hàng.

**Kết luận cho checkpoint review nghiệp vụ**

Chúng ta nên chốt mô hình Role toàn hệ thống ở cấp kiến trúc trước, nhưng chỉ triển khai các Role và Permission cần thiết cho từng Feature.

Đối với T-003-01, quyền Merchant vẫn phải bám contract đã duyệt; **Retail Sender là bắt buộc trong mục tiêu sản phẩm nhưng không được tự thêm quyền/bảng/API ngoài phạm vi T-003-01 nếu chưa được chốt SDD**. Guest chỉ dùng tính năng công khai. Những Role liên quan đến Dispatcher, Shipper, Station và Finance sẽ được đưa vào review sâu khi các nghiệp vụ tương ứng bắt đầu.

Đây sẽ là nền tảng để chúng ta xây dựng Role-Permission Matrix chính thức cho E-Logistics, sau đó đối chiếu với canonical SDD, quyết định những gì cần bổ sung và những gì chưa được phép thay đổi.

### 8.9. Quyết định bổ sung phạm vi Nhóm A (Project Owner, 2026-10-26)

1. **Guest — P0, External:** phải có tracking giới hạn; ước tính cước; coverage; tra cứu Station/Hub và giờ phục vụ; danh mục dịch vụ/điều kiện gửi; liên hệ tư vấn/phản ánh; đăng ký một tài khoản cá nhân; Merchant application chỉ sau đăng nhập. **Loại bỏ khả năng Guest gửi hàng trực tuyến không đăng ký**.
2. **Retail Sender — P0, Account cá nhân:** đăng ký/đăng nhập; hồ sơ và liên hệ; tạo Shipment gửi lẻ; yêu cầu pickup tại nhà; lưu địa chỉ; báo giá/dịch vụ; thanh toán; theo dõi tất cả đơn sở hữu; hủy khi được phép; mở tra soát/khiếu nại; quản lý hoàn hàng; lịch sử và hóa đơn/biên nhận. Không ép tạo tổ chức Merchant. Đây là **capability mục tiêu bắt buộc**, có thể chia Feature/mốc triển khai có dependency.
3. **Merchant — Account gắn Organization:** khác Retail Sender ở membership, nhân sự, pickup point, bảng giá hợp đồng, COD/đối soát, tích hợp và báo cáo theo quyền. Cùng dùng Logistics Core nhưng policy và resource scope khác nhau.
4. **Recipient — P1, External:** không đăng ký, không đăng nhập, không Portal riêng; chỉ tra cứu công khai như Guest (bảo vệ PII). Các tương tác giao nhận do Courier ghi nhận. Khiếu nại đơn Merchant thông qua Merchant Support, Merchant chuyển ticket lên Logistics Customer Service/Claims. Đơn của Retail Sender phải có kênh Logistics Support tương ứng; không giả định tồn tại Merchant.
5. **Walk-in Sender tại điểm giao dịch:** khách chưa đăng ký có thể ký gửi trực tiếp tại Station/Hub được hỗ trợ, do **Station Operator** tạo vận đơn trong kênh nhân viên có xác minh, phí/biên nhận và custody rõ ràng. Không phải Guest online Shipment Creation và không mặc nhiên là `user_account` hay một Role đăng nhập mới.
6. **Phạm vi tài liệu:** các quyết định trên thay thế những dòng trước đó trong mục VIII mô tả Retail Sender là ngoài ưu tiên, Guest gửi lẻ online hoặc Recipient cần Portal. Mục II vẫn lưu mô hình toàn ngành để tham chiếu; khi có khác biệt, **mục 8.9 là quyết định phạm vi Capstone**, không sửa âm thầm phần tham khảo nguyên bản. Mọi mã permission, table, API, state vẫn chờ đối chiếu canonical SDD.

### 8.10. Quyết định mở rộng đầy đủ Nhóm B (Project Owner, 2026-10-26)

**Phạm vi mục tiêu Nhóm B = FULL.** Đưa đầy đủ 7 chủ thể trong mục II, Nhóm B vào thiết kế nghiệp vụ E-Logistics. Đây là **Target Scope** chứ không phải chứng nhận triển khai xong, cũng không mở rộng quyền Identity/SDD khi chưa duyệt. Trong mục VIII, P0/P1/P2 là thứ tự triển khai các nhóm capability; **không phải lý do loại một nghiệp vụ của Nhóm B ra khỏi mục tiêu cuối**.

| ID ma trận | Role / Principal | Nhóm | Đợt ưu tiên gợi ý | Nghiệp vụ phải phủ |
|---|---|---|---|---|
| R02 | Merchant Owner | B | P0 | Hồ sơ pháp nhân, nhân sự/quyền, pickup point, đơn thủ công/hàng loạt, cấu hình vận chuyển, COD, billing, hợp đồng/giá, claim, integrations, báo cáo |
| R19 | Merchant Manager | B | P0 | Nhân sự được ủy quyền, pickup point, xử lý/duyệt sửa-hủy, hiệu suất, giao thất bại, làm việc với hãng |
| R03 | Merchant Operator | B | P0 | Tạo/import/sync, chuẩn hóa địa chỉ, báo giá, xác nhận, in nhãn, bàn giao, tracking, giao lại/hủy/hoàn, xem COD theo scope |
| R20 | Merchant Warehouse Staff | B | P1 | Pick/pack/count, cân đo, dán/quét nhãn, manifest, ghi sai/thiếu, xác nhận bàn giao |
| R11 | Merchant Finance | B | P2 | COD, đối soát, cước/phụ phí, công nợ, thanh toán, chứng từ, khiếu nại phí/COD, báo cáo |
| R21 | Merchant Support | B | P1 | Tra cứu Merchant Shipment, tiếp nhận Recipient, yêu cầu giao lại, tạo/theo dõi ticket, phối hợp Logistics CS, theo dõi hoàn |
| R22 | Enterprise/API Client | B | P2 | Merchant-bound API principal; quote, tạo/sync Shipment, label, tracking, webhook, idempotency, lỗi đồng bộ |

**Tổng danh mục mục tiêu:** 22 Role/chủ thể/principal khi giữ nguyên 18 mục sau mở rộng Nhóm A và thêm 4 mục Nhóm B. Bao gồm **Guest + Recipient là external actors** và **Enterprise/API Client là integration principal**; đây không phải 22 loại user-account.

**Các module phủ Nhóm B:** M01 Identity & Access; M02 Merchant Management; M03 Address & Coverage; M04 Pricing & Quotation; M05 Order & Shipment Management; M06 Parcel & Label; M07 Pickup Management; M10 Last-mile Delivery; M11 Tracking & Notifications; M12 COD Management; M13 Billing & Settlement; M14 Returns & Reverse Logistics; M15 Customer Support & Claims; M16 Analytics & SLA; M17 Partner & Integrations; M18 Security & Audit (xuyên suốt). M08 Station & Hub Operations và M09 Transportation Management phục vụ hành trình logistics phối hợp, không tự động chuyển nhân sự kho Merchant thành nhân sự hãng.

**Merchant Support nhận phản ánh Recipient:** Recipient không có portal/account. Merchant Support kiểm tra Shipment và lập ticket → E-Logistics Customer Service → Claims/Operations/Finance có thẩm quyền → Merchant Support phản hồi Recipient. Với kiện của Retail Sender, kênh Customer Service Logistics tiếp nhận không thông qua Merchant.

**Ranh giới bắt buộc:** Owner chỉ kiểm soát Merchant mình; Manager theo ủy quyền (không mặc nhiên thay ngân hàng hoặc owner); Operator không tự duyệt bồi thường/sửa COD đã khóa; Warehouse không giả mạo sự kiện nhận hàng của Courier; Finance không tự ghi settlement đã thanh toán; Support không sửa POD/tracking; API principal không được sử dụng chung secret của human user hoặc truy cập sai tenant.

**Tương thích tài liệu lịch sử:** Phần II Nhóm B là Business Reference toàn ngành. Các mô tả cũ trong 8.1–8.8 về chưa ưu tiên Manager/Warehouse/Support/API được hiểu là baseline giai đoạn trước, **được mục 8.10 thay thế về phạm vi đích**. `R01–R18`, `BC-001–BC-156` được giữ để bảo toàn tham chiếu; tham khảo ma trận cập nhật `R19–R22` và các BC mới. Khi thay đổi SDD, phải review Identity/Merchant/Orders/Shipment/Claims/Finance/Integrations contract và không sửa source code khi chưa được phép.

### 8.11. Quyết định mở rộng đầy đủ Nhóm C và D (Project Owner, 2026-10-26)

**Nhóm C — Shipper/giao nhận và Nhóm D — Điều phối, bưu cục, kho vận: Target Scope = FULL**, gồm đầy đủ **4 + 6 = 10 Role** và tất cả các hoạt động liệt kê trong Phần II. `FULL` có nghĩa phạm vi nghiệp vụ sản phẩm phải bao phủ, **không có nghĩa source đã triển khai hoặc SDD đã phê duyệt**. P0/P1/P2 chỉ là thứ tự thực hiện. Nội dung mục 8.11 **thay thế** những đoạn lịch sử 8.1–8.8 ngụ ý Line-haul, Sorting, Hub, Network, Courier Team Leader không cần trong sản phẩm đích.

| ID ma trận | Nhóm | Role | Giai đoạn ưu tiên gợi ý | Nghiệp vụ Target FULL | Module chủ đạo |
|---|---|---|---|---|---|
| R07 | C | Pickup Courier | P1 | Assignment, tuyến/liên hệ, check-in, scan/đối chiếu, kiểm tra, bằng chứng pickup, thất bại, bàn giao, báo cáo | M06, M07, M08, M11, M16 |
| R09 | C | Delivery Courier | P1 | Nhận tải/scan, tuyến/liên hệ, OFD, COD, POD, giao thất bại, giao lại, trả kiện, nộp COD, báo cáo | M06, M08, M10, M11, M12, M14, M16 |
| R23 | C | Line-haul Driver | P1 | Chuyến xe, manifest, scan, seal/tải trọng, hành trình, báo trễ/sự cố, tới Hub, đối chiếu bàn giao | M06, M08, M09, M11, M18 |
| R24 | C | Courier Team Leader | P1 | Nhiệm vụ đội, năng suất, đề xuất phân công lại, sự cố, tồn kiện, chênh lệch bàn giao | M07, M08, M10, M15, M16, M18 |
| R06 | D | Dispatcher | P1 | Pickup/delivery pool, phân công, gom điểm/tối ưu tuyến, tái phân công, SLA, giao lại/chuyển tuyến, năng suất | M07, M08, M10, M11, M14, M16 |
| R08 | D | Station Clerk / Station Operator | P1 | Tiếp nhận, scan, cân đo, kiểm kiện, manifest, xuất/nhập, nhận trả, tồn kho, chênh lệch, walk-in | M05, M06, M08, M09, M11, M14 |
| R25 | D | Sorting Staff | P1 | Scan, phân tuyến, gom bao/túi, xác nhận qua phân loại, sai tuyến, barcode lỗi, hư hỏng | M06, M08, M09, M18 |
| R26 | D | Hub Supervisor / Warehouse Manager | P1 | Năng lực xử lý, tồn kiện, chuyến xe, lệch manifest, mất/hỏng, điều chuyển, phê duyệt ngoại lệ | M08, M09, M15, M16, M18 |
| R16 | D | Station Manager | P2 | Nhân sự/ca, kiện vào/ra/tồn, pickup/delivery, chuyến, nộp COD, ngoại lệ, KPI/SLA, audit | M01, M07, M08, M09, M10, M12, M16, M18 |
| R27 | D | Network Operations Manager | P2 | Station/Hub/coverage, năng lực tuyến, điều chuyển liên vùng, giao thành công, SLA, chuyển tuyến ngoại lệ | M03, M07, M08, M09, M10, M12, M16, M18 |

**Danh mục mục tiêu tổng:** **27 Role/chủ thể/principal** sau khi bảo toàn 22 mục của bản mở rộng Nhóm A/B và thêm 5 Role (R23–R27). Đây không phải 27 loại account: Guest và Recipient vẫn là external actors, Enterprise/API Client vẫn là integration principal.

**Các ranh giới bắt buộc:**

1. **Assignment:** Courier chỉ thao tác trên pickup/delivery task hợp lệ của mình. Một người kiêm hai chức trách vẫn có assignment và permission độc lập.
2. **Custody:** Hàng đi qua Merchant/Retail Sender → Pickup Courier → Station → Sorting Hub → Line-haul → Hub/Station đích → Delivery Courier; mỗi lượt bàn giao cần actor, địa điểm, số lượng, thời gian và bằng chứng theo chính sách. Đây là luồng minh họa, tuyến cụ thể có thể có ít điểm trung gian hơn.
3. **POD / Attempt:** Không được ghi `DELIVERED` khi thiếu bằng chứng giao hoặc sửa/xóa lịch sử thất bại; Dispatcher/Manager không tạo POD thay Courier.
4. **Line-haul / Station separation:** Driver quản lý trip/seal/manifest/chuyến; Station xác nhận kiện xuất/nhập. Không dùng quyền Station để giả mạo sự kiện hành trình.
5. **COD separation:** Courier thu và yêu cầu nộp, Cashier xác nhận đã nhận; Manager/Network không tự sửa ledger hay thanh toán.
6. **Exception & audit:** Sai tuyến, mất/hỏng, lệch manifest, tồn kho và chuyển vùng cần event, lý do, evidence, quyền phê duyệt và audit.
7. **Khách gửi:** Giữ nguyên Nhóm A/B: Guest không gửi online khi chưa đăng ký; Retail Sender account cá nhân đủ nghiệp vụ; khách walk-in ký gửi tại Station được nhân viên tạo Shipment; Recipient không tài khoản/Portal và phản ánh đơn Merchant qua Merchant Support.

**Phân kỳ thực hiện:** M3 được mở rộng thành **Pickup → Station → Sorting → Line-haul → Destination Station → Delivery**, hỗ trợ return/exception qua M4. Toàn bộ năng lực đã liệt kê là `CORE` trong **Target FULL**, dù có thể triển khai theo nhiều Feature. Để công nhận hoàn thành, cần đối chiếu `Role → Module → Business Capability → SDD → API → DB → UI → Test` và chứng cứ vận hành/financial/custody. Không phát minh quyền, endpoint hoặc bảng mới trước khi review canonical SDD.

**Bảo toàn quyết định cũ:** Mục 8.9 (Nhóm A) và 8.10 (Nhóm B) vẫn có hiệu lực; mục 8.11 chỉ mở rộng Nhóm C/D. Các mô tả 8.1–8.8 là baseline lịch sử và bị 8.9–8.11 thay thế khi mâu thuẫn. Ma trận giữ nguyên `R01–R22`, `BC-001–BC-206`; bổ sung `R23–R27`, `BC-207` trở đi.


### 8.12. Quyết định mở rộng đầy đủ Nhóm E và F (Project Owner, 2026-10-26)

**Nhóm E (CSKH, COD, tài chính, kinh doanh): 7/7 Role — Target FULL. Nhóm F (quản trị nghiệp vụ, bảo mật và đối tác): 6/6 Role — Target FULL.** Các nghiệp vụ được liệt kê ở mục II là **Business Target Scope bắt buộc**, có thể phân kỳ theo Feature. `FULL` không khẳng định sản phẩm đã hiện thực hoặc canonical SDD đã phê duyệt thay đổi.

| Mã Role | Nhóm | Role/Principal | Ưu tiên gợi ý | Nghiệp vụ Target FULL | Module liên quan |
|---|---|---|---|---|---|
| R14 | E | Customer Service Agent | P2 | Tra cứu theo ticket, tiếp nhận, giao chậm, yêu cầu giao lại, đổi thông tin theo workflow, COD inquiry, returns, escalation, SLA | M03, M05, M10, M11, M12, M14, M15, M16, M18 |
| R15 | E | Claims & Exception Officer | P2 | Case/điều tra/evidence/trách nhiệm/chính sách/hạn mức/đề xuất/chuyển phê duyệt/outcome | M08, M10, M11, M12, M13, M15, M18 |
| R12 | E | COD Cashier | P2 | Nhận tiền, kiểm đếm, xác nhận nộp, chênh lệch, đóng ca, chuyển đối soát | M08, M12, M13, M16, M18 |
| R13 | E | Finance / COD Reconciliation | P2 | Courier/Shipment đối soát, eligible settlement, phí/công nợ, bảng kê, refund đã duyệt, sai lệch/trùng, audit | M04, M05, M12, M13, M16, M18 |
| R28 | E | Pricing / Billing Administrator | P1 | Rate card, vùng/tuyến, trọng lượng, dịch vụ, phí COD/phụ phí, hợp đồng Merchant, chiết khấu, hiệu lực, snapshot giá | M02, M03, M04, M13, M18 |
| R29 | E | Sales / Key Account Manager | P2 | Quản lý Merchant, tư vấn, hợp đồng, đề xuất giá, account scope, KPI, hỗ trợ Merchant lớn | M02, M04, M15, M16, M17, M18 |
| R30 | E | Risk / Fraud / Compliance Officer | P1 | Gian lận COD, account abuse, spam Shipment, recipient giả, kiện bất thường, truy cập sai, hold/escalation/release, audit | M01, M05, M08, M12, M13, M15, M18 |
| R04 | F | Business Admin | P0 | Merchant, dịch vụ, coverage, Station/Hub, chính sách, nhân sự theo quyền, Shipment monitoring, SLA/report, escalation | M01, M02, M03, M04, M05, M08, M15, M16, M18 |
| R31 | F | Platform Super Admin | P2 | Tenant/multi-organization, partition, privileged admin, feature flags, platform policy, exceptional workflow | M01, M02, M12, M18 |
| R05 | F | Identity / Security Administrator | P0 | Provisioning, account status, Realm, Identity rights, MFA, session revocation, credential incidents, security policy | M01, M18 |
| R32 | F | IT / Integration Operations | P2 | Health checks, webhook failure, integration config, queues, sync error reconciliation, controlled production operation | M01, M05, M11, M16, M17, M18 |
| R17 | F | Auditor | P2 | Read-only changes/tracking/identity permission history/ledger/claims evidence | M11, M12, M13, M15, M18 |
| R33 | F | Partner / Franchise Operator | P2 | Nhận kiện tại quầy, bàn giao, kiểm đếm, tồn tại điểm, báo cáo doanh thu chính điểm | M05, M06, M08, M13, M17, M18 |

**Tổng danh mục target:** **33 Role/chủ thể/principal** trong sáu nhóm A–F, với 18 module nghiệp vụ cấp hệ thống; `Guest` và `Recipient` vẫn là External Actors không có account; `Enterprise/API Client` là Integration Principal không phải human account; Partner Operator là account có Partner Organization/Station scope. Con số 33 **không phải 33 Role đăng nhập**.

**Ranh giới nghiệp vụ không được phá:**

1. **Ticket và Claim:** CSKH tiếp nhận, Claims điều tra và đề xuất; người phê duyệt bồi thường/chi trả có quyền và hạn mức riêng; không đồng nhất người đề xuất–phê duyệt–thanh toán.
2. **COD & Ledger:** Thu tiền Courier → Cashier nhận → Finance đối soát → Merchant settlement là các sự kiện riêng. Không sửa trực tiếp `cod_amount`/số dư thay cho ledger, phải chống duplicate/refund và có bằng chứng.
3. **Pricing:** Bảng giá có version/effective date, Shipment có snapshot giá chốt, điều chỉnh sau cân đo có dấu vết và phê duyệt; Sales đề xuất giá khác với Pricing Admin phê duyệt/cấu hình được phép.
4. **Security segregation:** Business Admin không tự có Identity security powers; Identity Admin không có COD/Shipment powers; Platform Super Admin không giả lập DELIVERED/POD/COD settlement; IT không tùy ý impersonate; Auditor chỉ đọc.
5. **Fraud:** Risk Officer chỉ được hold/release Shipment theo quy trình, có hồ sơ/bằng chứng và audit; không sửa tracking/custody/ledger không có workflow.
6. **Partner:** Chỉ có scope theo Partner Organization và Station; ký gửi tại điểm có biên nhận/custody, không truy cập toàn mạng.
7. **Giữ nguyên A–D:** Guest có tiện ích công khai trừ tạo Shipment online; Retail Sender account cá nhân với mọi nghiệp vụ đã liệt kê; Recipient không có Portal, tra cứu công khai và khiếu nại Merchant thông qua Merchant Support; walk-in do nhân viên Station thực hiện; Nhóm B và C/D Target FULL giữ nguyên.

**Thứ tự triển khai không làm mất phạm vi:** P0/P1/P2 dùng để sắp xếp dependency; mỗi capability E/F là `CORE` trong Target FULL trừ các hàng `DEFER` chỉ ra **hành vi bị cấm** ở ma trận. Phạm vi kỹ thuật chi tiết phải reconcile với canonical SDD, không tự thêm endpoint/schema/permission hay coi đây là bằng chứng đã code.

**Tính kế thừa:** Các mục 8.9 (A), 8.10 (B), 8.11 (C/D) vẫn có hiệu lực; khi 8.1–8.8 mô tả phạm vi hẹp hơn, các quyết định 8.9–8.12 được ưu tiên về Target Scope. Ma trận giữ `R01–R27`, `BC-001–BC-280`; bổ sung `R28–R33`, `BC-281` trở đi.
