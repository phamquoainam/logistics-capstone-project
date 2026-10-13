# Product Requirements Document — E-Logistics Prototype

**Nguồn:** [REPORT] Phần A §3–5; Chương 2 §2.1–2.7; Chương 3 §3.3–3.7.

## 1. Bối cảnh và vấn đề
Các hoạt động tạo vận đơn, xác nhận địa chỉ, tính cước, kho, điều phối/hub, shipper, ví và COD có thể sai lệch khi request trùng, thao tác đồng thời, sự kiện đến muộn hay gián đoạn mạng. Hệ thống cần một nguồn dữ liệu nghiệp vụ đáng tin cậy, kiểm soát quyền và lịch sử, xử lý nhất quán các mốc vận hành và tài chính.

## 2. Nhóm người dùng
- Guest/người nhận: chỉ tra cứu dữ liệu hành trình tối thiểu, ước tính cước.
- Merchant: quản lý điểm lấy, địa chỉ, quote, vận đơn, tồn/fulfillment, ví, COD.
- Warehouse Operator / Hub Operator: quản lý chứng từ, tồn kho và bàn giao theo kho/hub được cấp.
- Dispatcher: điều phối chuyến, manifest, phân công, tuyến và giám sát.
- Shipper: nhận nhiệm vụ, quét mã, giao/hoàn, POD, COD, chia sẻ vị trí theo phiên.
- Finance: xác minh giao dịch, xác nhận nộp COD, đối soát, điều chỉnh.
- Admin: RBAC, cấu hình, bảng giá, audit, xử lý lỗi; không mặc nhiên có quyền duyệt tài chính.

## 3. Mục tiêu và kết quả sản phẩm
1. Trình diễn được chuỗi end-to-end: tạo/import đơn → xác nhận địa chỉ → báo giá → xác nhận/thu cước → WMS → hub/dispatch → routing/GPS → POD → notification → COD/nộp/đối soát.
2. Bảo vệ bất biến dữ liệu: số dư/tồn khả dụng không âm, không tác động trùng, không assignment active trùng, workflow đúng trạng thái, quote có phiên bản, kỳ đối soát đóng bất biến.
3. Tái lập được toàn bộ demo và thử nghiệm với Docker Compose, migration và seed; đo được RQ1–RQ4.

## 4. Phân cấp phạm vi
### P0 bắt buộc
Authentication/RBAC; merchant/địa chỉ; pricing/versioning; shipment/import/tracking; warehouse; dispatch/hub; mobile shipper/POD/offline; routing/GPS; finance ví/VNPay sandbox/COD; notification/dashboard/audit; kiểm thử đồng thời, phục hồi, tải.

### P1 chỉ sau P0
Chuẩn hóa địa chỉ tiếng Việt phi cấu trúc (rule/fuzzy/NLP), WMS nâng cao, ETA/re-routing, POD nâng cao, thông báo email/SMS/push, analytics và tích hợp đối tác.

### Ngoài phạm vi
Tiền thật, thanh toán/chi trả thật, robot/IoT kho, HA/autoscaling/Kubernetes/multi-region, LLM/RAG/Text-to-SQL.

## 5. Những user journey chủ đạo
- J1 — Merchant có ví đủ tiền: tạo draft, xác nhận địa chỉ, nhận quote, xác nhận đơn, thấy số tiền bị trừ chính xác một lần.
- J2 — Merchant dùng WMS: xác nhận đơn, kho giữ đủ hàng, pick/pack/xuất, bàn giao riêng tại hub.
- J3 — Dispatcher–Shipper: lập manifest, phân công hợp lệ, đề xuất tuyến, duyệt tuyến, shipper nhận việc, quét và giao có POD.
- J4 — COD: shipper khai thu, nộp; Finance xác nhận; chỉ khoản đủ điều kiện mới vào kỳ đóng; payout mô phỏng theo dữ liệu đã chốt.
- J5 — Lỗi mạng/timeout/callback lặp: không làm mất tác động đã commit, không trừ/ghi có hai lần, hiển thị trạng thái chưa xác định và có đường phục hồi.

## 6. Yêu cầu chức năng và phi chức năng có thẩm quyền
Xem `docs/domain/requirements-catalog.md` để có đầy đủ các mã **FR-G/A/M/W/D/S/F/P/AD/C**, `docs/domain/business-rules.md` cho **BR-01–BR-21**, `docs/quality/non-functional-requirements.md` cho **NFR**. Không tạo bản sao sửa nghĩa tại tài liệu feature.

## 7. Điều kiện thành công và nghiệm thu
- [REPORT] AC-01–AC-08 tại `docs/quality/acceptance.md`.
- [REPORT] API thông thường với 100 VU: p95 ≤ 750 ms; p99 ≤ 1,5 s; lỗi hệ thống < 1%; Pricing p95 ≤ 300 ms với tải công bố; routing ≤ 10 s / tối đa 50 điểm; vị trí từ server nhận đến giao diện ≤ 5 s (đúng phạm vi phép đo).
- [REPORT] Số vi phạm bất biến trong kịch bản kiểm thử đã công bố: bằng 0. Không suy rộng thành SLA production.

## 8. Rủi ro / phụ thuộc
Tính đúng của dữ liệu hành chính và vùng giá, thanh toán sandbox, provider định tuyến, quyền GPS/camera, lưu POD, đồng bộ offline, idempotency, dữ liệu seed và khả năng tái lập thử nghiệm.

## 9. Ràng buộc thực thi
Nhóm ba thành viên, khoảng 14 tuần trong báo cáo; triển khai theo vertical slice ưu tiên P0. Các thành phần chưa chốt ở `docs/decisions/open-questions.md` phải ghi ADR trước khi phụ thuộc triển khai.
