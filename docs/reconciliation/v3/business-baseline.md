> Current canonical target follows [Owner account-first decision](../../decisions/customer-account-merchant-admission.md). Historical statements below are source context where superseded; accepted runtime/security unchanged.

# Business scope V3 — Reconciled Candidate

PROPOSED / Owner review required. Owner VIII.8.9–8.12 định nghĩa target; accepted SDD/contracts và verified source định nghĩa hành vi hiện tại. [Conflict register](decisions.md) ghi khác biệt; chưa chốt thì dừng implementation phụ thuộc, tiếp tục docs-only. Không sửa nghĩa REPORT FR/BR/NFR.

## Sáu nhóm FULL

| Nhóm | Roles/actors/principals | Outcome bắt buộc |
|---|---|---|
| A | R01 Guest, R10 Recipient, R18 Retail Sender | Guest tracking/quote/coverage/Station/Hub/service/contact/đăng ký cá nhân và application Merchant sau đăng nhập; Sender account cá nhân profile/address/booking/pickup/payment/tracking/cancel/return/support/receipts; Recipient external không login/portal; walk-in do staff tại quầy |
| B | R02 Owner, R03 Operator, R11 Finance, R19 Manager, R20 Warehouse, R21 Support, R22 API Client | Org/membership/delegation, pickup, shipment/import/fulfillment, contracts/fees/COD/settlement, ticket/claim/report, integration; Merchant A/B isolation |
| C | R07 Pickup, R09 Delivery, R23 Line-haul, R24 Team Leader | Nhiệm vụ/scan/count/contact/evidence/attempts/POD/cash submit; trip/seal/delay/handover; leader đề xuất theo team |
| D | R06 Dispatcher, R08 Station Operator, R16 Station Manager, R25 Sorting, R26 Hub Supervisor, R27 Network Manager | Assignment/custody/manifest/sorting/bag/trips/route/region/capacity; tồn kiện/variance/approval; staffed walk-in |
| E | R12 Cashier, R13 Finance, R14 CS, R15 Claims, R28 Pricing, R29 Sales, R30 Risk | Cash count/shift/receipt; ledger/debt/reconciliation/payout; tickets/claims/SoD; versioned pricing/contracts/sales approval; fraud hold/release |
| F | R04 Business Admin, R05 Identity Admin, R17 Auditor, R31 Super Admin, R32 IT Ops, R33 Partner | Business/security/platform privileges riêng; read-only audit; controlled retry/production ops; Partner Organization/Station isolation |

33 R IDs không là33 account types/portals/repos. Human Account khác Integration Principal. Recipient đơn Merchant qua Merchant Support→Logistics CS→domain/Claims/Finance; đơn Sender có Logistics Support trực tiếp. Guest contact có abuse controls, không shipment API.

## Capability và invariants

[371 rows](capability-crosswalk.csv) giữ ID/role/module/operation/guard:366 CORE và đúng5 DEFER BC-006/040/045/046/085. DEFER là cấm/không có quyền mặc định, không positive backlog. CORE cũng có denial: Driver243, Sorting261, Network280, Super Admin354/355, IT362. Mỗi BC một primary spec/domain owner đề xuất; mapping PROPOSED độc lập implementation status.

Order ≠ Shipment ≠ Parcel; Shipment Status ≠ Tracking Event; Assignment ≠ Custody; Tracking Event ≠ Delivery Attempt; Delivery Attempt ≠ POD; Quote ≠ Price Snapshot; Fee Adjustment ≠ original price; COD Collected ≠ Remitted ≠ Reconciled ≠ Settled; Ticket ≠ Claim; Business Admin ≠ Identity Admin ≠ Super Admin; Portal visibility ≠ backend authorization.

Allow=current principal/session+explicit operation permission+authoritative membership/delegation+org/resource/Station/Hub/region/team/assignment scope+current state+evidence/security. Recheck mutation/replay/recovery/async; không tin body owner/role/actor hay UUID. Identity Management chỉ scope đã duyệt, không widen bằng matrix.

## 18 business modules ánh xạ chín domains

| Taxonomy | Write owner/collaborator |
|---|---|
| M01 | identity credential/session/grant; merchant business membership qua authority API |
| M02/M03 | merchant org/profile/address/catalog proposal; routing adapter theo contract; orders snapshot |
| M04 | pricing quote/rate/contract policy; finance adjustment/posting |
| M05/M06 | orders Shipment/Parcel/label; warehouse SKU/stock/fulfillment; không duplicate Parcel |
| M07/M08/M09/M10 | dispatch assignment/custody/Station/Hub/trip; routing route/location; orders attempt/POD/state |
| M11/M16 | orders canonical tracking; notification delivery/projections; KPI không ghi source/ledger |
| M12/M13 | finance duy nhất money/ledger/receipt/reconciliation/settlement |
| M14/M15 | orders return/ticket/claim proposal; dispatch custody; warehouse QC; finance compensation; D-V3-05 trước schema |
| M17 | merchant integration/partner-org; identity machine credential; dispatch point operations |
| M18 | identity security; audit ở domain nguồn; platform technical primitives/ops |

Một Spring Boot artifact; workflow/public API cùng local DB transaction; không othermodule.internal, không18 services/table từ conceptual label. Bảng/path/DTO mới chờ review.

## Financial/NFR/release boundaries

Giữ BR-01–21/NFR/AC-01–08 REPORT. Fee/WMS confirm cùng transaction; DB constraints/locking chống âm tiền/tồn/double assignment; movement/ledger/custody/evidence append-only, closed recon bất biến. External I/O ngoài transaction, idempotency scope/key/canonical payload; source/outbox atomic. Refresh giữ strict single-flight/no blind retry, không áp receipt chung vào token rotation.

Tiền thật/chi trả thật vẫn OUT_OF_SCOPE REPORT tới D-V3-04; sandbox/simulation có nhãn, không proof settlement thật. Không tự chọn map/geocoding/payment/object provider.

Giữ ngưỡng và ranh giới đo REPORT: API100VU p95≤750ms/p99≤1.5s/error<1%; pricing p95≤300ms; route≤10s/50điểm; GPS server nhận→UI≤5s; zero observed invariant violations trong workload công bố. Chưa benchmark mới. Retention/access/abuse/restore/device phải chốt trước first use. [J1–J9](journeys.md), [delivery](delivery-plan.md) yêu cầu BE+FE+PG+browser/native evidence, không synthetic UI proof.
