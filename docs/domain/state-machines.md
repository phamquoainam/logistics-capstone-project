# State-machine Baseline — nguồn Chương 3 §3.5.2

**[REPORT]** Đây là chuyển trạng thái trích từ các bảng §3.5.2 (DOCX). Codex phải cài transition guard có kiểm thử; các transition không liệt kê không được mặc nhiên hợp lệ. Không đổi trạng thái trong database bằng câu UPDATE trực tiếp bỏ qua use case.


## State machine 1

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | DRAFT → CONFIRMED | Merchant | Quyền, địa chỉ, báo giá, số dư và hàng WMS hợp lệ; ghi nhận nhất quán thu cước, giữ hàng và xác nhận vận đơn. |
| T2–T3 | DRAFT/CONFIRMED → CANCELLED | Merchant | Chưa vượt mốc hủy; giải phóng phần giữ chưa sử dụng và xử lý hoàn cước theo chính sách. Hủy thành công không đồng nghĩa đã hoàn tiền. |
| T4 | CONFIRMED → IN_TRANSIT | Shipper | Đơn giao nhận trực tiếp, phân công lấy hợp lệ và đã nhận kiện; lưu bằng chứng, tracking. |
| T5 | CONFIRMED → FULFILLING | Warehouse Operator | Đơn WMS có phiếu xuất và phần giữ hợp lệ; ghi nhận bắt đầu lấy hàng, không còn cho phép hủy thông thường. |
| T6 | FULFILLING → READY_FOR_HANDOVER | Warehouse Operator | Đã kiểm đếm, đóng gói và xuất đúng phần phân bổ; lưu chứng từ xuất, chưa coi hub đã nhận. |
| T7 | READY_FOR_HANDOVER → IN_TRANSIT | Shipper/Hub Operator theo chặng | Có xác nhận giao–nhận đúng kiện; lưu bên đang giữ kiện và tracking. |
| T8 | IN_TRANSIT → OUT_FOR_DELIVERY | Shipper | Có phân công giao còn hiệu lực và đã nhận kiện; ghi nhận bắt đầu chặng giao. |
| T9 | OUT_FOR_DELIVERY → DELIVERED | Shipper; người nhận nếu xác nhận trực tiếp | POD hợp lệ và đáp ứng điều kiện COD nếu có; ghi nhận kết quả giao, POD và khoản thu liên quan. |
| T10 | OUT_FOR_DELIVERY → DELIVERY_FAILED | Shipper | Có lý do, định danh lần giao và dữ liệu hợp lệ; lưu thất bại, không mặc định đã thu COD. |
| T11 | DELIVERY_FAILED → OUT_FOR_DELIVERY | Shipper theo điều phối | Còn được giao lại và phân công hợp lệ; tạo lần giao mới, không dùng lại định danh lần thất bại. |
| T12 | DELIVERY_FAILED → RETURN_REQUESTED | Dispatcher/người có quyền | Hết điều kiện giao lại hoặc có quyết định hoàn; lưu lý do và lập nhiệm vụ hoàn. |
| T13 | RETURN_REQUESTED → RETURNING | Shipper | Nhiệm vụ hoàn hợp lệ; ghi nhận bắt đầu thực hiện. |
| T14 | RETURNING → RETURNED | Shipper và bên nhận lại hàng | Có bằng chứng bên gửi đã nhận lại; hàng hoàn về WMS phải được kiểm tra riêng trước khi cộng tồn khả dụng. |
| T15 | IN_TRANSIT → RETURN_REQUESTED | Dispatcher/người có quyền | Có quyết định chuyển hoàn trước giao, lý do và phương án bàn giao phù hợp; không xử lý như hủy trước lấy hàng. |

## State machine 2

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | CREATED → PENDING | Merchant/Hệ thống | Số tiền và tham chiếu hợp lệ; gửi yêu cầu sang VNPay Sandbox. |
| T2 | CREATED → FAILED | Merchant | Hủy trước khi gửi sang nhà cung cấp; không có thanh toán bên ngoài đang chờ. |
| T3 | PENDING → UNKNOWN | Hệ thống | Timeout hoặc chưa xác minh được kết quả; không ghi có và không suy đoán thất bại. |
| T4 | UNKNOWN → PENDING | Hệ thống/Finance qua UC-F06 | Bắt đầu xác minh lại cùng yêu cầu, không tạo khoản nạp mới. |
| T5–T6 | PENDING/UNKNOWN → SUCCEEDED | Hệ thống từ kết quả hợp lệ | Xác minh nguồn, chữ ký, số tiền, tham chiếu và trạng thái thành công; ghi có duy nhất cùng kết quả thanh toán. |
| T7–T8 | PENDING/UNKNOWN → FAILED | Hệ thống/Finance theo kết quả xác minh | Có căn cứ chắc chắn về kết quả không thành công; không ghi có. Đóng trình duyệt không phải căn cứ xác định thất bại. |
| Nội bộ | SUCCEEDED giữ nguyên | Hệ thống | Kết quả lặp trả về trạng thái đã có. Kết quả mâu thuẫn được chuyển sang UC-F06; không tự ghi có thêm hoặc đảo bút toán. |

## State machine 3

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → ACTIVE | Merchant qua UC-M05 hoặc Warehouse Operator được cấp quyền | reserved > 0, đúng chủ hàng và kho, đủ tồn hợp lệ; ghi phần giữ và giảm tồn khả dụng, không giảm tồn thực tế. |
| T2 | ACTIVE giữ nguyên | Warehouse Operator/luồng hủy đơn hợp lệ | Số lượng sử dụng hoặc giải phóng lớn hơn 0 và nhỏ hơn remaining; cập nhật consumed hoặc released, lưu lịch sử. Sử dụng phải gắn chứng từ xuất; giải phóng không tăng tồn thực tế. |
| T3 | ACTIVE → CONSUMED | Warehouse Operator qua UC-W05 | remaining = 0, consumed = reserved, released = 0; có đầy đủ chứng từ xuất liên quan. |
| T4 | ACTIVE → RELEASED | Merchant qua UC-M06 hoặc người có quyền | remaining = 0, released = reserved, consumed = 0; trả lại khả năng phân bổ. |
| T5 | ACTIVE → CLOSED | Hệ thống theo nghiệp vụ sử dụng/giải phóng hợp lệ | remaining = 0, consumed > 0 và released > 0; giữ lịch sử cả phần đã xuất và phần đã giải phóng. |

## State machine 4

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → READY | Hệ thống theo đơn WMS | Đơn đã xác nhận, phần giữ và kho hợp lệ; lập phiếu xuất. |
| T2 | READY → PICKING | Warehouse Operator | Có quyền và phần phân bổ còn hiệu lực; ghi nhận bắt đầu lấy hàng. |
| T3 | READY → CANCELLED | Merchant/người có quyền theo quy trình hủy | Chưa bắt đầu lấy; đóng phiếu chưa tác nghiệp và giải phóng phần giữ hợp lệ. |
| T4 | PICKING → PACKING | Warehouse Operator | SKU, vị trí và số lượng đã được đối chiếu, không vượt phần giữ; lưu kết quả lấy hàng. |
| T5 | PACKING → READY_TO_DISPATCH | Warehouse Operator | Đóng gói và mã kiện đúng vận đơn; lưu kết quả đóng gói. |
| T6 | READY_TO_DISPATCH → DISPATCHED | Warehouse Operator | Đủ điều kiện xuất và bàn giao; ghi chứng từ xuất, giảm tồn thực tế và ghi nhận lượng giữ đã sử dụng một lần. |
| Ngoại lệ | Giữ bước hiện tại | Warehouse Operator/người có quyền xử lý | Thiếu hoặc sai hàng, sai kiện phải lưu nguyên nhân, khắc phục và kiểm tra lại. Không chuyển sang DISPATCHED khi còn lỗi; dừng hoặc hoàn sau khi bắt đầu lấy phải được xử lý có thẩm quyền. |

## State machine 5

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → PENDING_COLLECTION | Hệ thống theo vận đơn | Có nghĩa vụ COD hợp lệ; lưu số tiền phải thu và vận đơn gốc. |
| T2 | PENDING_COLLECTION → COLLECTED | Shipper | Đã thu đủ khoản COD hợp lệ; ghi số tiền thực thu và người chịu trách nhiệm. Khoản thu một phần được theo dõi riêng, không làm mất nghĩa vụ còn lại. |
| T3 | PENDING_COLLECTION → CANCELLED | Người có quyền theo kết quả vận đơn | Vận đơn kết thúc không thu và không có tiền đã thu chưa xử lý; đóng nghĩa vụ chưa thu, giữ lịch sử. |
| T4 | COLLECTED → REMITTED | Finance | Xác nhận đã nộp đủ, không tự xác nhận khai báo của mình; lưu chứng từ và cập nhật nghĩa vụ nộp tiền. |
| T5 | REMITTED → RECONCILED | Finance qua UC-F05 | Đã giao, thu, nộp đủ, không tranh chấp và chưa thuộc kỳ đã đóng khác; chốt liên kết với kỳ đối soát. |
| Ngoại lệ | Không tự đổi trạng thái cuối | Finance/người có quyền | Chênh lệch hoặc tranh chấp được xử lý riêng và chặn đối soát. Sai lệch sau khi đóng kỳ tạo điều chỉnh mới, không đưa RECONCILED về trạng thái trước. |

## State machine 6

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → SUBMITTED | Shipper | Khoản COD thuộc trách nhiệm shipper, không khai báo xử lý trùng; lưu số tiền và bằng chứng. |
| T2 | SUBMITTED → CONFIRMED | Finance khác người khai báo | Tiền thực nhận và phân bổ khớp, đủ bằng chứng; ghi nhận xác nhận một lần. |
| T3 | SUBMITTED → DISCREPANCY | Finance | Có thiếu, thừa hoặc dữ liệu không khớp; lưu số thực nhận, chênh lệch và người phụ trách. |
| T4 | SUBMITTED → REJECTED | Finance | Có căn cứ từ chối; lưu lý do, không ghi nhận đã nộp đủ. |
| T5 | DISCREPANCY → CONFIRMED | Finance | Có căn cứ xử lý và phân bổ tiền; nếu chỉ xác nhận một phần thì tiếp tục giữ nghĩa vụ còn thiếu của từng khoản COD. |
| T6 | DISCREPANCY → REJECTED | Finance | Có quyết định từ chối và lý do; giữ lịch sử. Tiền đã thực nhận vẫn phải được truy vết và xử lý. |

## State machine 7

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → DRAFT | Finance | Có quyền, phạm vi merchant và thời điểm chốt hợp lệ; lập danh sách dự kiến. |
| T2 | DRAFT → READY_TO_CLOSE | Finance/Hệ thống | Kiểm tra điều kiện từng khoản, tổng tiền và điều chỉnh; tạo kết quả xem trước trên phiên bản dữ liệu xác định. |
| T3 | READY_TO_CLOSE → DRAFT | Finance/Hệ thống | Danh sách hoặc số liệu thay đổi; kết quả xem trước mất hiệu lực và phải kiểm tra lại. |
| T4 | READY_TO_CLOSE → CLOSED | Finance có quyền đóng kỳ | Kiểm tra lại điều kiện và xung đột kỳ tại thời điểm đóng; chốt nhất quán danh sách, tổng tiền, người đóng và thời điểm. |
| Sau đóng | CLOSED giữ nguyên | Finance qua UC-F08/UC-F03 | Sai lệch được xử lý bằng điều chỉnh riêng; chi trả qua nghiệp vụ riêng. Không mở lại hoặc sửa trực tiếp bảng kê đã chốt. |

## State machine 8

| Mã | Chuyển trạng thái | Nguồn | Guard / tác động |
| --- | --- | --- | --- |
| T1 | Khởi tạo → DRAFT | Dispatcher | Xác định nhiệm vụ, phân công và dữ liệu đầu vào; tạo tuyến gợi ý hoặc dự thảo thủ công có ghi rõ giới hạn. |
| T2 | DRAFT → CONFIRMED | Dispatcher | Đủ dữ liệu kiểm chứng, không vi phạm ràng buộc và nhiệm vụ còn hợp lệ; xác nhận phiên bản. |
| T3 | DRAFT → CANCELLED | Dispatcher | Có quyền; hủy dự thảo, không thay đổi phân công hoặc trạng thái vận đơn. |
| T4 | CONFIRMED → IN_PROGRESS | Shipper | Đúng shipper, phiên bản có hiệu lực và nhiệm vụ hợp lệ; ghi nhận bắt đầu tuyến. |
| T5 | CONFIRMED → SUPERSEDED | Dispatcher | Phiên bản thay thế đã được kiểm tra và xác nhận; thay đổi hiệu lực nhất quán, không để hai phiên bản đồng thời có hiệu lực. |
| T6 | IN_PROGRESS → SUPERSEDED | Dispatcher | Chỉ thay phần chưa hoàn tất theo chính sách; giữ lịch sử các điểm đã phục vụ và tiếp tục bằng phiên bản mới. |
| T7 | IN_PROGRESS → COMPLETED | Shipper/Hệ thống theo kết quả tác nghiệp | Các điểm trong kế hoạch có kết quả hợp lệ hoặc ngoại lệ đã được kết thúc theo chính sách; không đồng nghĩa mọi vận đơn đều giao thành công. |
