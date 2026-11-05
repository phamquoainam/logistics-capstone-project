# ArgoPost — Kịch bản demo và kiểm thử Personal end-to-end

Ngày đối chiếu source:03/11/2026. Dùng UI local hiện tại, gồm navigation polish
được chuẩn bị tích hợp ngày05/11/2026 trên branch `feature/argopost-navigation-polish`.
Đây là hướng dẫn thao tác, KHÔNG phải biên bản các bước đã được thực hiện.
Không seed, cấp Staff, đổi mật khẩu hay tạo đơn trong lúc soạn tài liệu này.

## 1. Câu chuyện demo

Một khách hàng đăng ký/xác minh email, lưu liên hệ, chuẩn bị nhiều kiện, nhận
báo giá và đặt vận đơn thật trong PostgreSQL. Khách tải nhãn; nhân viên được
phân công scan đủ kiện và tiếp nhận tại Station. Khách thấy trạng thái, lịch sử
và Analytics cập nhật từ dữ liệu đó. Cuối cùng kiểm tra khôi phục mật khẩu và
cách ly dữ liệu giữa hai tài khoản.

Thời lượng:35–45 phút cho luồng chính; các ca lỗi mở rộng tập trước buổi demo.
Nếu chỉ có15 phút: Public → tài khoản đã chuẩn bị → D1 → PDF → Station → KPI.
Ghi rõ khi bỏ qua đăng ký trực tiếp và dùng tài khoản chuẩn bị trước.

Phạm vi vận hành:Local/Test Capstone ROAD_STANDARD, dataset mô phỏng được duyệt.
Địa chỉ/tuyến SIM không phải mạng lưới chuyển phát thương mại thực tế. Có dữ liệu
nghiệp vụ thật được lưu, nhưng chưa có Pickup, thu tiền, In Transit hay Delivered.
PDF vẫn phải giữ nhận diện mô phỏng/UNPAID theo contract.

## 2. Chuẩn bị trước buổi trình bày

Từ PowerShell tại `D:\TLCN\e-logistic`:

```powershell
.\scripts\dev\local.cmd doctor
.\scripts\dev\local.cmd start
.\scripts\dev\local.cmd status
```

Nếu đã READY, không cần stop/start để demo. Chỉ mở web khi thấy
`LOCAL_FULL_STACK_HTTP_READY`. Không chạy init, reset database, Flyway repair,
docker compose down -v hoặc xóa volumes. Giữ container PostgreSQL hiện có.

| Chuẩn bị | Cách sử dụng |
| --- | --- |
| HTTPS certificate đã trust; Docker/PG18 healthy | Không bỏ TLS validation để trình diễn |
| Internet/Gmail SMTP đã cấu hình | Mở sẵn inbox thử nghiệm do mình kiểm soát; kiểm tra Spam |
| Customer A | Email CHƯA từng đăng ký nếu muốn demo đăng ký mới; dùng A cho cả ba đơn bên dưới |
| Customer B | Account đã verified, trình duyệt/profile riêng, dùng kiểm tra ownership |
| Station Staff | Account verified riêng, đã được operator phân công SIM-DROP-A1 |
| Hai cửa sổ browser/profile độc lập | A ở cửa sổ1, Staff ở cửa sổ2; B dùng profile/phiên riêng khác |
| PDF viewer | Chuẩn bị download/print, chọn100×150mm/100% khi in |

Không dùng credentials `browser-customer@example.invalid` hoặc `browser-staff`
trong source: đó là fixtures ở DB test tạm, không phải tài khoản local được seed.
Mật khẩu tự chuẩn bị, tối thiểu15 ký tự và không quá72 byte UTF-8; không đưa vào
slide/log. Không chiếu App Password, token, OTP thật hoặc DevTools Authorization.
Điện thoại liên hệ chấp nhận dạng0…; không phải credential đăng nhập đã xác minh.

Các Terms/Privacy còn chờ Owner duyệt nội dung. Có thể trình diễn cơ chế consent
ở Local/Test hiện có, nhưng phải nói rõ chưa nghiệm thu pháp lý/production.
Nếu form bị policy guard chặn, không bỏ guard hay tự tạo văn bản để vượt qua.

### Staff là điều kiện chuẩn bị riêng

V21 không tự seed tài khoản hay assignment Staff. Khi Owner/operator chủ động
chọn đúng account verified dành cho demo, có thể cấp đúng một điểm bằng runbook
[Station operations](station-pilot-operations.md):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev\station-staff.ps1 -AccountId '<UUID_ACCOUNT_STAFF_VERIFIED>' -Station SIM-DROP-A1 -Action GRANT
```

UUID lấy từ phản hồi customer-auth/me của chính Staff sau login. Không dùng
account Customer A cho cả hai vai trò. Đây là thao tác cấp quyền có chủ đích,
được audit; KHÔNG tự chạy hoặc mặc định đã được thực hiện bởi tài liệu này.
Chưa chuẩn bị Staff thì phần Station chưa sẵn sàng demo; không giả lập thành công.

## 3. Bộ dữ liệu nhất quán để đối chiếu

Dùng tên/liên hệ thử nghiệm, không dùng PII người nhận thật. Ví dụ:
- Người gửi:Người gửi Demo; điện thoại0900000001; nhãn `DEMO-SENDER-A1`.
- Người nhận:Người nhận Demo; điện thoại0900000002; nhãn `DEMO-RECIPIENT-A2`.
- Tỉnh/địa phương/địa chỉ:nhập rõ là dữ liệu demo, ví dụ `Vùng A mô phỏng`,
  `Điểm A1`, `Địa chỉ thử nghiệm A1`. Đây là contact tự nhập, UNVERIFIED.
- Chọn coverage SIM riêng trong màn báo giá; địa chỉ tự nhập không tự suy ra vùng.
- Mỗi Parcel có quantity1, giá trị khai báo0, hàng thông thường không nguy hiểm.

| Mã tham chiếu tự nhập | Tuyến chọn | Parcel: trọng lượng / dài×rộng×cao | Cước kỳ vọng |
| --- | --- | --- | --- |
| DEMO-D1-NOIVUNG | SIM-A1 → SIM-A2 | Kiện1:500g /10×10×10cm; kiện2:501g /10×10×10cm |25.000+28.000=53.000đ |
| DEMO-D2-LIENVUNG | SIM-A1 → SIM-B1 | Một kiện1000g /10×10×10cm |38.000+5.000=43.000đ |
| DEMO-D3-QUYDOI | SIM-A1 → SIM-A2 | Một kiện500g /30×20×20cm |2,4kg thể tích →2,5kg tính cước →37.000đ |

Form nhập trọng lượng g, kích thước cm. Không nhập100 vào ô cm để biểu diễn100mm.
Form chuyển đổi theo contract; không nhân khối lượng thêm lần nữa theo quantity.
Cước tổng ba đơn:133.000đ. Ba Shipment, bốn Parcel, mọi nghĩa vụ vẫn UNPAID.
Chỉ so sánh số tuyệt đối này nếu A không có Shipment cũ và bộ lọc bao gồm cả ba đơn.
Nếu dùng account có lịch sử, ghi baseline và so chênh lệch +3/+4/+133.000đ.

## 4. Luồng trình diễn chính

### TC01 — Public Website và truy cập workspace

1. Mở `https://127.0.0.1:5173/`; xem logo, banner, màu thương hiệu, cuộn trang.
2. Xem các nhóm dịch vụ; không giới thiệu Air/Sea như dịch vụ đã vận hành.
3. Thu nhỏ viewport/tablet/mobile, thử menu và hover/focus.
4. Chọn ĐĂNG NHẬP / ĐĂNG KÝ để vào luồng tài khoản.

PASS:ảnh/logo tải đúng, không tràn ngang, điều hướng đúng. Các tiện ích public
chưa có nguồn nghiệp vụ phải nói đúng trạng thái, không trả tracking/cước giả.

### TC02 — Đăng ký và Gmail OTP thực

1. Vào `/register`, dùng email thử nghiệm mới do mình kiểm soát.
2. Nhập họ tên, email, điện thoại dạng0…, mật khẩu và xác nhận giống nhau.
3. Thử mật khẩu nhập lại sai để cho thấy validation, rồi sửa; consent theo trạng
   thái Local/Test đang áp dụng, không tuyên bố đã có Terms/Privacy production.
4. Đăng ký:phải mở ngay trang nhập OTP; không phải bấm link trong email.
5. Mở inbox, lấy mã6 số, nhập trong2 phút. Không đọc/chiếu mã lên slide/video lưu.
6. Sau xác minh:popup đăng ký thành công/chào mừng và tự chuyển `/login`.

PASS:trước OTP đúng chưa có account ACTIVE được tạo; pending enrollment có thể
được lưu riêng. Đúng OTP mới tạo account. Không suy luận gửi/nhận thành công chỉ
từ thông báo nhận yêu cầu; phải quan sát email thật. Email cũ có thể nhận phản
hồi chung mà không có OTP mới để chống dò tài khoản.

### TC03 — Đăng nhập, navigation và hồ sơ

1. Login bằng email đã verified và mật khẩu; refresh trang.
2. Mở dropdown ArgoPost Personal:Personal đang chọn, Business xám Chưa kích hoạt,
   bấm Business không điều hướng, không cấp quyền.
3. Thử Quản lý đóng/mở, Enter/Space/Escape, chuyển trang; không tự bật lại do remount.
4. Menu tài khoản ở header → Hồ sơ cá nhân; sửa tên trong hồ sơ, lưu và refresh.
5. Menu tài khoản → Bảo mật; giới thiệu liên hệ hồ sơ không đổi Identity credential.

PASS:phiên tồn tại qua refresh; dữ liệu hồ sơ lưu thật. Không yêu cầu tên hồ sơ
phải đổi ngay greeting Identity nếu đây là hai trường có chủ đích tách biệt.
Không còn nhóm Tài khoản lặp ở sidebar hoặc ngữ cảnh thừa bên trái header.

### TC04 — Sổ địa chỉ CRUD, mặc định và snapshot

1. Mở Sổ địa chỉ; tạo sender `DEMO-SENDER-A1`, chọn mặc định.
2. Tạo sender phụ làm mặc định:chỉ một sender đang active có cờ mặc định.
3. Tạo recipient `DEMO-RECIPIENT-A2`, chọn mặc định; sender/recipient độc lập.
4. Tìm theo nhãn/tên/điện thoại, lọc loại; sửa một liên hệ và refresh để kiểm tra.
5. Lưu trữ liên hệ phụ, xác nhận;nó biến mất khỏi danh sách active, không phải xóa
   lịch sử vận đơn. Giữ sender/recipient dùng cho demo.

PASS:canonical phone có thể hiển thị +84 dù nhập0…; liên hệ vẫn UNVERIFIED,
không trở thành email/phone credential hoặc bằng chứng coverage.

### TC05 — Chuẩn bị/lưu/chỉnh sửa/lưu trữ Draft

1. Tạo vận đơn; chọn người gửi/người nhận từ sổ địa chỉ.
2. Thêm hai kiện, nhập mô tả và số đo; tham chiếu `DEMO-DRAFT-EDIT`.
3. Lưu bản nháp; mở lại từ Quản lý vận đơn → Đơn nháp, refresh.
4. Tiếp tục chỉnh sửa:đổi tham chiếu/mô tả, lưu và kiểm tra thay đổi.
5. Lưu trữ bản nháp thử sau khi xác nhận, không ảnh hưởng các Booking.

PASS:Draft có dữ liệu thật nhưng chưa có Shipment/UNPAID/Carrier Acceptance.
Màn chi tiết có8 tab; những tab thiếu nghiệp vụ không tạo sự kiện/chứng từ giả.

### TC06 — Quote → Confirm Booking (D1, D2, D3)

Với từng dòng của bảng dữ liệu:
1. Tạo đơn mới, chọn liên hệ đã lưu, điền chính xác từng kiện và mã tham chiếu.
2. Tiếp tục báo giá → xem snapshot tại Xác nhận thông tin & báo giá.
3. Chọn origin/destination SIM đúng bảng; ROAD_STANDARD; drop-off, không Pickup.
4. Xác nhận chỉ khai hàng thông thường → Lấy báo giá mô phỏng.
5. Đối chiếu phí từng kiện/tổng và thời hạn15 phút; không tự sửa giá từ browser.
6. Xác nhận đặt vận đơn mô phỏng trước khi hết hạn.
7. Ghi lại mã Shipment và các AP-P Parcel; refresh để chứng minh persistence.

PASS:D1=53.000đ, D2=43.000đ, D3=37.000đ; mỗi thao tác thành công tạo đúng một
Shipment với đủ Parcel. Draft chuyển sang Booked, không còn là Draft active.
Trạng thái Chờ bàn giao; nghĩa vụ UNPAID. Booking không phải đã thu tiền/đã tiếp nhận.

### TC07 — Quản lý, tìm kiếm, bộ lọc, chi tiết8 tab

1. Quản lý → Quản lý vận đơn; tìm `DEMO-D1-NOIVUNG`, tìm tên/phone người nhận.
2. Chọn ngày hôm nay, ROAD_STANDARD, SIM-A1→SIM-A2, UNPAID; bấm Tìm vận đơn.
3. Kỳ vọng D1 và D3, rồi bỏ lọc để thấy ba đơn; đổi sort cước tăng/giảm.
4. Mở D1, lần lượt xem Tổng quan, Kiện hàng, Hành trình, Lấy hàng & Giao hàng,
   Cước & COD, Chứng từ, Yêu cầu & Hỗ trợ, Lịch sử.
5. Thử category chưa có nghiệp vụ như Đang vận chuyển/Hoàn hàng/Hỗ trợ liên quan:
   phải hiển thị dependency, không coi bảng rỗng/zero là chức năng đã hoàn chỉnh.

PASS:dữ liệu theo server filter, URL giữ lựa chọn, Back/Forward đúng. Bảng ba đơn
chưa đủ để chứng minh nhiều trang; ca paging riêng cần hơn20 Shipment thật hoặc
dùng báo cáo test tương ứng, không tự nhân bản data để đẹp screenshot.

### TC08 — Nhãn PDF, Code128 và snapshot bất biến

1. D1 → Chứng từ hoặc Kiện hàng → chuẩn bị/tải nhãn và Lưu PDF/mở chế độ in.
2. PDF có hai trang100×150mm, mỗi trang gắn đúng một Parcel AP-P/Code128.
3. So nội dung người gửi/nhận, tuyến, cân nặng và thứ tự kiện với Booking.
4. Sau Booking, sửa liên hệ gốc trong Sổ địa chỉ; mở lại D1/PDF.

PASS:Shipment/PDF vẫn giữ snapshot đã chốt; dữ liệu liên hệ mới không âm thầm sửa
đơn cũ. Nhãn có UNPAID và nhận diện mô phỏng, không phải hóa đơn/biên nhận thu tiền.
PDF riêng tư, không phải URL public hay upload S3 do Customer thực hiện ở đây.

### TC09 — Station intake đủ kiện, vai trò riêng

Điều kiện:Staff đã được phân công SIM-DROP-A1 đúng runbook; cửa sổ A giữ phiên.
1. Ở cửa sổ Staff, mở `https://127.0.0.1:5173/operations/station`, đăng nhập Staff.
2. Chọn SIM-DROP-A1; nhập/quét AP-P kiện1 của D1, bấm Scan & đối chiếu kiện.
3. Thấy1/2 đã scan; Xác nhận tiếp nhận đủ kiện phải bị chặn.
4. Nhập/quét kiện2; thấy2/2. Đối chiếu hàng/bao bì/cân đo với snapshot.
5. Tích xác nhận khớp đầy đủ rồi Xác nhận tiếp nhận đủ kiện.
6. Refresh Staff; quay cửa sổ A, mở Hành trình và Lấy hàng & Giao hàng của D1.

PASS:AT_STATION/Carrier tiếp nhận tại SIM-DROP-A1 được ghi thật; đủ hai kiện,
custody/tracking/audit theo transaction. D2/D3 vẫn chờ bàn giao. UNPAID không đổi.
Không diễn tiếp In Transit/Delivered hoặc ghi đã thu tiền khi chưa có nghiệp vụ.
Không có máy scan vẫn có thể nhập mã đầy đủ; không gọi đó là camera scan đã triển khai.

### TC10 — Dashboard3 tầng và Cước phí & Thống kê

1. A → Tổng quan, chọn kỳ bao gồm ba Booking, không lọc ngày/tuyến cục bộ còn sót.
2. Sau khi chỉ D1 được Staff tiếp nhận, đối chiếu:

| Chỉ số | Kỳ vọng nếu cohort chỉ gồm D1–D3 |
| --- | --- |
| Vận đơn |3 |
| Chờ bàn giao |2 |
| Đã tiếp nhận |1 |
| Cước chưa trả |133.000đ |
| Kiện hàng (Deep Dive) |4 |
| Cước xác nhận (Deep Dive) |133.000đ; không cộng với UNPAID thành266.000đ |
| Phân bổ trạng thái |Chờ66,6%; tiếp nhận33,3%, hiển thị một chữ số theo phép tính hiện tại |
| Nội vùng A1→A2 |2 đơn,90.000đ |
| Liên vùng A1→B1 |1 đơn,43.000đ |

3. Click KPI Đã tiếp nhận hoặc legend trạng thái → chỉ có D1.
4. Click cột thời gian → xem đúng các đơn thuộc bucket; Back/Forward giữ URL.
5. Xem phân tích → tuyến/kiện/dịch vụ/cước; click nhóm để drill-through source.
6. Thử7/30/90 ngày, khoảng tùy chọn dd/MM/yyyy và filter nâng cao.
7. Quản lý → Cước phí & Thống kê; đối chiếu cước/UNPAID cùng cohort.

PASS:không có giả Delivery/SLA/doanh thu/COD. Nếu kỳ trước zero, không bịa phần
trăm tăng trưởng. Không có ví/rút tiền/thanh toán giả. Trang Đơn hàng cần xử lý
giải thích dependency Carrier Exception/Support, không phải module exception hoàn chỉnh.

### TC11 — Hồ sơ Merchant đang có, không phải Business Workspace

1. Menu tài khoản ở header → Hồ sơ Merchant.
2. Tạo bản nháp Merchant, nhập tên pháp nhân thử nghiệm, lưu; refresh/mở lại.
3. Chỉ trình diễn các trường và thao tác thực sự đang có trên màn hình Draft.
4. Mở workspace chooser:Business vẫn disabled. Customer tự mở `/merchant` cũng
   không được vào private workspace hoặc tự nhận Merchant Owner.

PASS:bản nháp hồ sơ lưu thật; không suy ra nộp hồ sơ, xét duyệt, upload evidence
hay ACTIVE membership đã được triển khai đầy đủ.

### TC12 — Quên mật khẩu, đăng xuất và phiên khác

1. Mở thêm phiên A độc lập trước reset nếu muốn chứng minh thu hồi session.
2. Ở phiên chính, đăng xuất → Quên mật khẩu, nhập email verified của A.
3. Hệ thống mở ngay OTP, nhận Gmail thật, nhập mã6 số trong2 phút.
4. Đúng OTP mới đến màn mật khẩu mới; nhập và hoàn tất trước deadline gốc.
5. Popup đổi mật khẩu thành công và tự về login.
6. Thử mật khẩu cũ:bị từ chối; mật khẩu mới:thành công.
7. Phiên A cũ sau refresh/yêu cầu protected mới không còn được sử dụng.
8. Logout; truy cập lại `/personal/shipments` phải về login.

PASS:OTP/recovery single-use; không để lại quyền truy cập từ session đã bị thu hồi.
Không dùng hard refresh trang OTP giữa chừng vì challenge được giữ trong bộ nhớ;
nếu mất challenge thì yêu cầu mã mới, không nhờ đọc token từ DB/log.

## 5. Các ca QA bổ sung, không nhất thiết làm hết trước lớp

| ID | Thao tác | Kết quả cần có |
| --- | --- | --- |
| N01 | Gửi lại OTP sau cooldown30s; dùng mã cũ rồi mã mới |Mã cũ bị thu hồi, mã mới dùng trong deadline mới |
| N02 | Không nhập OTP quá2 phút; hoặc thử sai đến giới hạn5 lần |Không tạo account thành công; không chấp nhận proof bị hết hiệu lực |
| N03 | Đăng ký email đã tồn tại; recovery email không tồn tại |Phản hồi chung; không tiết lộ sự tồn tại, không tạo account/đổi mật khẩu giả |
| N04 | A mở hai tab cùng sửa một address/draft |Bản cũ gặp conflict; giữ nội dung, tải bản mới; không ghi đè im lặng |
| N05 | B mở URL Shipment/Draft của A hoặc PDF không có quyền |Không trả dữ liệu/PDF của A; không suy từ việc B thấy một trang trống |
| N06 | Customer không có assignment mở Operations |Bị từ chối, không thấy scan/acceptance data |
| N07 | Staff chỉ A2 hoặc Staff bị REVOKE thao tác D1 originA1 |Bị từ chối trước dữ liệu/scan/PDF/acceptance; chuẩn bị quyền bằng operator riêng |
| N08 | SIM-A2→SIM-B1 hoặc SIM-A1→SIM-A1 |Không báo giá/Booking; không fallback thành tuyến toàn quốc |
| N09 | Kiện vượt20kg, chiều>100cm hoặc girth>200cm |Quote từ chối theo policy; lưu Draft không đồng nghĩa service chấp nhận |
| N10 | Không xác nhận hàng thông thường / hàng điều kiện đặc biệt |Không phát hành quote hợp lệ |
| N11 | Để quote15 phút thật rồi Confirm |Không Booking từ quote hết hạn; không sửa đồng hồ hoặc DB để giả expiry |
| N12 | Sửa thông tin sau màn review |Quote cũ không được áp vào dữ liệu đã đổi; phải review/báo giá lại |
| N13 | Click Booking nhanh hai lần |Không tạo trùng Shipment/obligation; nút đang chạy bị khóa |
| N14 | Scan lặp, sai định dạng, sai Station, thiếu/ngoại lai Parcel |Không tăng đủ kiện giả/không partial acceptance |
| N15 | Lặp/đua Acceptance |Một kết quả hợp lệ; không nhân đôi custody/tracking/UNPAID |
| N16 | Đổi filter Dashboard hoặc Management sang khoảng không có đơn |Zero thật và empty state gọn; lỗi503/401 không thành zero giả |
| N17 | Nhập ngày31/02, ngày kết thúc trước bắt đầu |Báo validation, không gửi thành một query ngày khác |
| N18 | Tab/Enter/Space/Escape, resize360/390/768/1440, hover dropdown |Business disabled, chooser trong viewport; focus/menus không bị che |
| N19 | Backend không có response xác nhận mutation |UI chưa xác định, khóa gửi lại; kiểm tra receipt đúng thao tác |

N19/races/rollback nên dùng các integration/browser fault-injection tests hiện có,
không chủ động tắt DB, dừng Docker, xóa dữ liệu hay tạo proxy phá mạng giữa buổi demo.
Chuẩn bị quote expiry riêng trong rehearsal; không chờ15 phút đứng trước lớp.
Paging cần dữ liệu tự tạo hợp lệ, không hứa thấy Trang sau chỉ với3 đơn.

## 6. Phần kỹ thuật tùy thời gian

- Public media:GET `https://127.0.0.1:8443/api/v1/public-media/hero-network.svg`
  trả đúng ảnh. Đây là read Backend; chỉ nhìn ảnh200 chưa tự chứng minh upload
  AWS SDK/S3 live hoặc private evidence authorization.
- S3:trình bày đúng evidence app smoke đã chạy nếu có, tách khỏi AWS CLI smoke.
  Không upload/xóa file nghiệp vụ để làm hiệu ứng; probe chính thức chỉ dùng
  dev-smoke của nó. Chưa có public UI đầy đủ cho POD/Claims/private documents.
- PostgreSQL:refresh và mở lại record chứng minh persistence. Nếu trình DB, dùng
  truy vấn read-only vào đúng record, không SELECT * Identity/proofs/outbox hoặc
  chiếu password hashes, email người thật, keys/credentials.
- Các lệnh frontend hiện có: `npm.cmd --prefix app/frontend/web run typecheck`,
  `run lint`, `run test`, `run test:customer-ui`, `run build`, `run build:customer`
  (mỗi `run` giữ cùng prefix). Fixtures UI không phải các Shipment trong DB local.
- Latest local UI checkpoint:81 UI và68 unit PASS. Đó không phải full Backend CI
  của phần UI chưa commit. Dùng link/artifact CI đúng revision khi trình test evidence.
- Các locator Hồ sơ/Merchant của harness real-browser đã được đồng bộ với
  menu header khi chuẩn bị tích hợp ngày05/11/2026. Kiểm tra phiên bị thu hồi
  vẫn truy cập protected Profile và yêu cầu bị từ chối. Kết quả hosted E2E
  của revision mới phải được đối chiếu riêng; guide không tự chứng minh PASS.

## 7. Cách chốt phần trình bày

“ArgoPost hiện có luồng Personal từ tài khoản đã xác minh đến Booking, lưu Shipment/
Parcel, nhãn riêng tư và tiếp nhận đủ kiện ở Station có phân quyền; Dashboard đọc
cùng nguồn dữ liệu. Booking và tiếp nhận không tự biến thành đã thu tiền hay đã
giao. Business/Carrier lifecycle tiếp theo, thu tiền/COD/settlement, support workflow
và production legal chưa được nghiệm thu trong phạm vi này.”

Ghi PASS/FAIL theo thao tác đã quan sát, mã đơn và giờ thực hiện; không điền PASS
trước buổi chạy thử. Không dùng số ví dụ133.000đ như số hệ thống đã có sẵn.
