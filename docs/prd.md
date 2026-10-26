> **V3 candidate để Owner review:** [Roadmap](implementation/roadmap-v3.md). Giữ accepted/runtime baseline; các mục REPORT/legacy bên dưới là lịch sử, không dùng để loại target FULL hoặc suy acceptance.

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

<a id="v3-target-business-baseline"></a>
## V3 target business baseline — PROPOSED, 2026-10-26

Mục1–9 giữ nguyên REPORT/prototype. Owner VIII.8.9–8.12 bổ sung target; [conflict register](reconciliation/v3/decisions.md) ghi khác biệt. Không dùng P1/exclusion cũ để âm thầm loại CORE, không dùng proposal mới thay accepted security/cho phép tiền thật.

Vision mở rộng: codebase Modular Monolith hiện hữu phục vụ Guest, Retail Sender cá nhân, FULL Merchant, physical logistics, Finance/Support/Governance/Partner:33 roles/actors/principals,18 modules,371 BC=366 CORE+5 DEFER. [Business baseline](reconciliation/v3/business-baseline.md), [crosswalk](reconciliation/v3/capability-crosswalk.csv) là target FR bổ sung với source/guard/owner/API/DB/UI/test status; không renumber FR/BR/NFR.

[30 specs/F0–F5](implementation/roadmap-v3.md) là full inventory. Guest public tracking/quote/coverage/Station/Hub/service/contact/hai nhánh đăng ký, cấm anonymous online Shipment. Sender profile/address/quote/booking/pickup/payment/tracking/cancel/return/support/history/receipt trong account cá nhân, không ép Merchant membership. Merchant Owner/Manager/Operator/Warehouse/Finance/Support và integration principal có org/delegation/fulfillment/contracts/COD/claims/report/API đầy đủ. Recipient external không IAM/portal, đơn Merchant qua Merchant Support, đơn Sender qua Logistics CS. Walk-in staff Station/Partner có quyền tại quầy, verification/fee/receipt/custody.

Physical: pickup pool→assignment→evidence→Station→sorting/bag/manifest→line-haul/seal/trip→destination→delivery assignment→attempt/POD hoặc retry/return. Assignment/custody/tracking/money custody riêng. Finance: collected→Cashier received→reconciled→settlement proof; immutable ledger/price snapshot/adjustment. Ticket khác claim; proposer/approver/payer riêng. Business Admin/Identity Admin/Super Admin/IT/Auditor khác quyền; Partner org+Station; cấm fake POD/COD/sửa audit/closed recon.

[J1–J9](reconciliation/v3/journeys.md) bổ sung AC-01–08; denial qua direct API/browser. Complete capability: accepted contract→migration/domain→application/HTTP auth→FE integrated→PG/security/concurrency→browser/native→source+PR CI→evidence. NFR giữ ngưỡng REPORT mục7, privacy/retention/abuse trước first use; không benchmark mới.

[First BE+FE packet](../specs/planning/v3/first-business-slice/spec.md) gồm Guest paths/Sender cá nhân/Merchant membership/pickup/address; BLOCKED tới approved D01/02/03/05 và API/data contract. Không coi001/002/Demo fully accepted.
