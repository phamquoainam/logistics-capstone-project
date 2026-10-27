> **Canonical V3 target:** [Roadmap](implementation/roadmap-v3.md). Giữ accepted/runtime baseline; các mục REPORT/legacy bên dưới là lịch sử, không dùng để loại target FULL hoặc suy acceptance.

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
Nhóm ba thành viên, khoảng 14 tuần trong báo cáo (không phải estimate FULL V3); triển khai theo từng spec end-to-end; vertical slice chỉ decomposition nội bộ. Các thành phần chưa chốt ở `docs/decisions/open-questions.md` phải ghi ADR trước khi phụ thuộc triển khai.

<a id="v3-target-business-baseline"></a>
## V3 target business baseline — canonical target, 2026-10-27

Latest [Owner account-first decision](decisions/customer-account-merchant-admission.md) và [delivery principles](implementation/delivery-principles-v3.md) supersede conflicting historical REPORT/proposed account choices. Existing accepted security/architecture only changes through controlled amendment. First-party own logistics network, not aggregator;33 actors/principals,18 conceptual business modules,371 BC=366 CORE+5 DEFER/deny in nine domain modules.

Guest public tracking/quote/coverage/Station/catalog/contact và unified personal registration/login/recovery entry; NO anonymous online Shipment. Retail Sender full eligible shipping journey without Merchant. Same verified human can apply Merchant after login; organization becomes active/initial Owner only after scoped evidence review/controlled activation. Staff personal accounts/current delegated memberships; personal remains valid after unrelated org rejection/revoke. Recipient external channels/no mandatory IAM. Machine API principal separate human credentials.

FULL Group A/B target: personal/Merchant dashboard/search/stats/notifications; single/bulk shipping, versioned addresses/service/quote/Parcel/labels/handover/tracking/return; supported payment/receipt/invoice/COD statements/disputes/claims; Merchant staff/facilities/pickup/contracts/reporting/integration. Correct owning specs in [coverage](portal/group-ab-coverage.md); unsupported international/immediate modes not promised. [Benchmark](portal/screenshot-benchmark.md) uses Owner descriptions only, not proprietary external behavior verification.

Retain full C–F: pickup assignment/evidence, Station/Hub custody/sort/manifests, linehaul trip/seal/arrival, delivery attempts/POD/returns, COD collection/cashier/remittance/reconciliation/simulation, CS/claims/risk/governance/audit/Partner/network/native offline/RQ/release. State vs event vs attempt vs custody vs money facts separate; Admin != Finance; business != Identity; SKU inventory != carrier custody; closed snapshots immutable. No real money/provider/security-policy expansion from docs.

[Roadmap](implementation/roadmap-v3.md), [371 BC](reconciliation/v3/capability-crosswalk.csv), [J-AB-01..10](portal/journeys.md), [GROUP_A_B_PORTAL_FULL_ACCEPTANCE](portal/group-ab-acceptance.md) supplement preserved FR/BR/NFR/AC-01..08, not replace them. Every CORE has owning requirement/design/task/AC; portal milestone cross-Spec,003 only customer admission/org/member/profile. Exact API/schema/permission/retention/provider pending review; no future functionality implemented claim.001/002 partial statuses and existing findings retained.
