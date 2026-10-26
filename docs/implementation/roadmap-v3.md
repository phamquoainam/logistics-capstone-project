# E-Logistics Spec Roadmap V3 — Reconciled Candidate

PROPOSED/Owner review/2026-10-26. Baseline ff3b296; [inventory](../reconciliation/v3/inventory.md), [business](../reconciliation/v3/business-baseline.md), [decisions](../reconciliation/v3/decisions.md). V2.1 nguyên source history, không canonical numbering.

## 30 logical specs và ID strategy

Giữ30 boundaries L001–L030: onboarding khác credential; state contract khác booking; fulfillment khác custody; pickup khác sorting/trip/delivery; receipt khác ledger/payout; ticket khác claim. Không spec theo CRUD/role, không thêm bounded context/service. L001/002 trỏ baseline,28 planning triples tại specs/planning/v3/Lxxx-name; [legacy aliases](../reconciliation/v3/legacy-crosswalk.csv) giữ physical001–016. Demo không spec31; Research016 là RQ track liên kết L030.

Namespace PROPOSED chờ D-V3-08; chưa thay numeric canonical governance. Promotion: Owner duyệt mapping/aliases→contracts→implement slice trong code hiện hữu→update evidence/navigation. Không move lịch sử. Nếu cần physical IDs mới sau này, reserve range qua decision và full alias/link migration+validators trước rename; không tự dùng numeric017–044.

## Spec cards và DAG

| ID | Full spec/name | Domain accountable | Wave | Hard prerequisites | Readiness |
|---|---|---|---|---|---|
| L001 | [platform-foundation](../../specs/001-platform-foundation/spec.md) | platform | F0 |  | PROTECTED_PARTIAL |
| L002 | [identity-access](../../specs/002-identity-access/spec.md) | identity | F0 | L001 | PROTECTED_PARTIAL |
| L003 | [sender-merchant-onboarding](../../specs/planning/v3/L003-sender-merchant-onboarding/spec.md) | merchant | F1 | L002 | BLOCKED |
| L004 | [address-coverage-service-catalog](../../specs/planning/v3/L004-address-coverage-service-catalog/spec.md) | merchant | F1 | L001 | BLOCKED |
| L005 | [pricing-contracts-quotation](../../specs/planning/v3/L005-pricing-contracts-quotation/spec.md) | pricing | F1 | L004 | BLOCKED |
| L006 | [order-shipment-parcel-contract](../../specs/planning/v3/L006-order-shipment-parcel-contract/spec.md) | orders | F1 | L003,L004,L005 | BLOCKED |
| L007 | [shipment-booking-sender-payments](../../specs/planning/v3/L007-shipment-booking-sender-payments/spec.md) | orders | F1 | L005,L006,L018 | BLOCKED |
| L008 | [parcel-label-merchant-fulfillment](../../specs/planning/v3/L008-parcel-label-merchant-fulfillment/spec.md) | warehouse | F1 | L006,L007 | BLOCKED |
| L009 | [tracking-event-core-public-lookup](../../specs/planning/v3/L009-tracking-event-core-public-lookup/spec.md) | orders | F1 | L006 | BLOCKED |
| L010 | [pickup-dispatch-execution](../../specs/planning/v3/L010-pickup-dispatch-execution/spec.md) | dispatch | F2 | L007,L008,L009 | BLOCKED |
| L011 | [station-intake-walkin-custody](../../specs/planning/v3/L011-station-intake-walkin-custody/spec.md) | dispatch | F2 | L007,L008,L009,L010,L018 | BLOCKED |
| L012 | [sorting-hub-manifests](../../specs/planning/v3/L012-sorting-hub-manifests/spec.md) | dispatch | F2 | L011 | BLOCKED |
| L013 | [linehaul-trip-hub-transfer](../../specs/planning/v3/L013-linehaul-trip-hub-transfer/spec.md) | dispatch | F2 | L011,L012 | BLOCKED |
| L014 | [delivery-dispatch-and-route](../../specs/planning/v3/L014-delivery-dispatch-and-route/spec.md) | dispatch | F2 | L011,L013 | BLOCKED |
| L015 | [lastmile-attempt-pod](../../specs/planning/v3/L015-lastmile-attempt-pod/spec.md) | orders | F2 | L009,L014,L017 | BLOCKED |
| L016 | [returns-reverse-logistics](../../specs/planning/v3/L016-returns-reverse-logistics/spec.md) | orders | F2 | L011,L015 | BLOCKED |
| L017 | [cod-collection-cashier-remittance](../../specs/planning/v3/L017-cod-collection-cashier-remittance/spec.md) | finance | F3 | L011,L018 | BLOCKED |
| L018 | [billing-ledger-fees-invoices](../../specs/planning/v3/L018-billing-ledger-fees-invoices/spec.md) | finance | F3 | L005,L006 | BLOCKED |
| L019 | [cod-reconciliation-merchant-settlement](../../specs/planning/v3/L019-cod-reconciliation-merchant-settlement/spec.md) | finance | F3 | L017,L018 | BLOCKED |
| L020 | [customer-support-tickets-sla](../../specs/planning/v3/L020-customer-support-tickets-sla/spec.md) | orders | F3 | L002 | BLOCKED |
| L021 | [claims-investigation-compensation](../../specs/planning/v3/L021-claims-investigation-compensation/spec.md) | orders | F3 | L016,L018,L020 | BLOCKED |
| L022 | [enterprise-api-webhooks](../../specs/planning/v3/L022-enterprise-api-webhooks/spec.md) | merchant | F4 | L002,L003,L005,L007,L008,L009 | BLOCKED |
| L023 | [bulk-shipment-import](../../specs/planning/v3/L023-bulk-shipment-import/spec.md) | orders | F4 | L007 | BLOCKED |
| L024 | [partner-franchise-station-ops](../../specs/planning/v3/L024-partner-franchise-station-ops/spec.md) | dispatch | F4 | L003,L011 | BLOCKED |
| L025 | [network-ops-routing-optimization](../../specs/planning/v3/L025-network-ops-routing-optimization/spec.md) | routing | F4 | L012,L013,L014 | BLOCKED |
| L026 | [risk-fraud-compliance](../../specs/planning/v3/L026-risk-fraud-compliance/spec.md) | orders | F4 | L006,L009 | BLOCKED |
| L027 | [platform-admin-audit-governance](../../specs/planning/v3/L027-platform-admin-audit-governance/spec.md) | identity | F4 | L002,L003 | BLOCKED |
| L028 | [notifications-analytics-sla](../../specs/planning/v3/L028-notifications-analytics-sla/spec.md) | notification | F4 | L009,L019,L020 | BLOCKED |
| L029 | [courier-offline-sync](../../specs/planning/v3/L029-courier-offline-sync/spec.md) | dispatch | F5 | L010,L015,L017 | BLOCKED |
| L030 | [capstone-e2e-hardening-release](../../specs/planning/v3/L030-capstone-e2e-hardening-release/spec.md) | platform | F5 | L003,L004,L005,L006,L007,L008,L009,L010,L011,L012,L013,L014,L015,L016,L017,L018,L019,L020,L021,L022,L023,L024,L025,L026,L027,L028,L029 | BLOCKED |

[Structured register](../reconciliation/v3/spec-register.json) chứa đủ ID/name/domain/purpose/in-out/actors/BC/FR-in-spec/invariants/hard-soft/BE/FE/DB/contracts/auth/workflows/idempotency/audit/acceptance/tests/DoR/DoD/order/blockers+owner. Requirement cụ thể ở từng spec; CSV là một nguồn BC/guard/status. Permission/schema/path chưa duyệt là DECISION_REQUIRED. Primary spec accountability không cấp DML mọi consumer; primary domain từng BC trong CSV là write owner cụ thể.

## F0–F5, gates tích lũy

| Wave | Packages/outcome | Exit evidence |
|---|---|---|
| F0 Baseline & Reconciliation | Preserve001/002/demo; inventory/371 mappings/conflicts/owners/docs review | G0 Owner duyệt V3 và first-use decisions; docs validators/diff pass, không full001/002 acceptance |
| F1 Customer to Booking | L003–009, L018 minimum ledger trước L007, L020 contact core; hold hook/M18 tại first use | G1 Guest utilities+Sender/Merchant scoped flows; snapshot/charge/reservation một lần; masked tracking/receipt; PG/browser/denial receipts |
| F2 Physical Logistics | L010–016, L017 collection contract trước L015; Station walk-in fee/receipt/custody | G2 Pickup→Station→Sorting→Line-haul→Destination→POD hoặc return; assignment/custody/tracking/attempt riêng; stale/tamper/replay denial |
| F3 Money & Resolution | Full L017–021, ledger/invoice/settlement simulation/ticket/claim/approval | G3 J5/J6 money tie-out/SoD/closed-period/refund/payout duplicates/recovery |
| F4 Enterprise & Governance | L022–028 integration/import/partner/network/risk/admin/analytics | G4 J7/J8 tenant-bound machine credential/Partner point scope; FULL target groups, không postpone security |
| F5 Reliability & Release | L029–030+Research016 RQ1–4 | G5 J1–J9/native offline/device/declared NFR load/restore/deploy/exact CI và Owner release |

Hard dependencies là contract/minimum functional slice được chứng minh, không cần toàn spec accepted. L018 core phụ thuộc L005/L006, không L007; full billing tiêu thụ booking/COD soft integration. L017 core phụ thuộc Station/L018, không L015; L015 dùng collection contract; full remittance E2E cần delivery thật. L020 contact core không delivery dependency; full escalation dùng L009/L015. L026 guard từ L006/007, full investigation F4. Audit/notification trước first effect.

Topological execution: L001→L002; L004 public độc lập L003; L003→L005→L006→L018-core→L007→L008/L009→L010→L011→L012→L013→L014; L017-core sau L011+L018→L015→L016→L017-full→L018-full/L019. L020-core từ F1; support/claims full theo data; L022–028 theo DAG→L029→L030. Không sort numeric để code; implementation_order trong register là topological ordinal, wave là full capability milestone.

## Feasibility và authorization

30 boundaries reuse monolith/React/RN, không30 lần xin phép/PR bắt buộc. Một coherent capability authorization gồm BE+FE+test/CI/evidence; đủ nhỏ review/rollback. Chưa estimate366 CORE với nhóm3/14tuần REPORT, không hứa deadline/tối ưu tuyệt đối. D08 chốt nguồn lực/milestone, không bỏ CORE cho vừa lịch.

[First packet](../../specs/planning/v3/first-business-slice/spec.md) BLOCKED D01/02/03/05 trước code; [delivery](../reconciliation/v3/delivery-plan.md), [J1–J9](../reconciliation/v3/journeys.md) định nghĩa closure. PR docs không production code authorization.
