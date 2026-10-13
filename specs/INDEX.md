# Feature Register — SDD

**[REPORT]** Mã FR/BR từ báo cáo, tính năng grouping riêng để Codex phát triển theo vertical slice. Tất cả 16 mục đang `DRAFT_SPEC/NOT_IMPLEMENTED`.

| ID | Feature | Source FR | Prerequisites | Read |
| --- | --- | --- | --- | --- |
| 001 | Nền tảng repository, deploy và kiến trúc | RQ/NFR | — | `001-platform-foundation/spec.md` |
| 002 | Xác thực và phân quyền theo phạm vi | FR-A01, FR-A02, FR-AD01 | 001 | `002-identity-access/spec.md` |
| 003 | Hồ sơ merchant, địa chỉ và tọa độ | FR-M01, FR-M02, FR-G01 | 001, 002 | `003-merchant-address/spec.md` |
| 004 | Pricing Engine và bảng giá theo phiên bản | FR-M03, FR-P01, FR-G01 | 001, 002, 003 | `004-pricing-engine/spec.md` |
| 005 | Tạo, xác nhận, hủy và tracking vận đơn | FR-M04, FR-M05, FR-M09, FR-G02 | 001, 002, 003, 004, 006, 007 | `005-shipment-lifecycle/spec.md` |
| 006 | Ví, ledger, thu cước và VNPay Sandbox | FR-M07, FR-F01, FR-F04 | 001, 002 | `006-wallet-payments/spec.md` |
| 007 | WMS: nhận hàng, tồn, giữ, pick-pack-xuất, hoàn | FR-W01, FR-W02, FR-W03, FR-W06, FR-W07, FR-W08, FR-M06 | 001, 002, 003 | `007-warehouse-wms/spec.md` |
| 008 | Hub Operations, manifest, custody và assignment | FR-D01, FR-D02, FR-D05, FR-W04, FR-W05 | 001, 002, 005, 007 | `008-dispatch-hub/spec.md` |
| 009 | Shipper Mobile: lấy/giao/hoàn, POD và COD collection | FR-S01, FR-S02, FR-S04, FR-S05 | 001, 002, 005, 008, 006 | `009-shipper-pod/spec.md` |
| 010 | Gợi ý tuyến, phê duyệt tuyến và GPS | FR-D03, FR-D04, FR-S03, FR-D05 | 001, 002, 008 | `010-routing-location/spec.md` |
| 011 | COD nộp tiền, xác nhận, đối soát và payout mô phỏng | FR-F02, FR-F03, FR-F04, FR-M08, FR-S05 | 001, 002, 006, 009 | `011-cod-reconciliation/spec.md` |
| 012 | Thông báo nội bộ và dashboard theo vai trò | FR-C01, FR-C02, FR-M08 | 001, 002, 005, 008, 011 | `012-notifications-dashboard/spec.md` |
| 013 | Admin RBAC, cấu hình hệ thống, audit và recovery | FR-AD01, FR-AD02, FR-AD03, FR-P01 | 001, 002, 004, 012 | `013-admin-audit-configuration/spec.md` |
| 014 | Import Excel tạo đơn nháp có preview và báo lỗi từng dòng | FR-M04 | 002, 003, 005 | `014-bulk-shipment-import/spec.md` |
| 015 | Hàng đợi offline và đồng bộ an toàn trên mobile | FR-S02, FR-S04, FR-S05 | 002, 008, 009 | `015-shipper-offline-sync/spec.md` |
| 016 | Thực nghiệm RQ1–RQ4 và tái lập bằng chứng | RQ/NFR | 001, 004, 005, 006, 007, 008, 010, 011, 012 | `016-research-experiments/spec.md` |

## Life cycle
`DRAFT_SPEC` → `REVIEWED` → `READY_FOR_IMPLEMENTATION` → `IN_PROGRESS` → `IMPLEMENTED` → `VERIFIED`.

Mỗi feature phải có đủ `spec.md`, `design.md`, `tasks.md`; xem `docs/codex/definition-of-ready.md`, `docs/quality/traceability.md`.
