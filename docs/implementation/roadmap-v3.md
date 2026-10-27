# Canonical SDD V3 roadmap — full first-party logistics platform

Owner docs/spec rebuild authorization2026-10-27; baseline eeb7833ab678240581c407ccd167d7e61ea873c6. E-Logistics owns logistics network, not multi-carrier marketplace. All371 BC preserved:366 CORE+5 DEFER/deny,33 actor/principal IDs,18 business module labels within existing nine domain modules. No production code/accepted-contract/migration approval from docs.

**Spec-first → Spec-by-Spec → Full-stack End-to-end Delivery → Full Spec Acceptance.** [Principles](delivery-principles-v3.md), [index](../../specs/INDEX.md), [canonical register](../reconciliation/v3/spec-register.json).

| ID/alias | Canonical spec | Purpose | Hard minimum dependency | Full wave | Status |
|---|---|---|---|---|---|
| 001 / L001 | [platform-foundation](../../specs/001-platform-foundation/spec.md) | Runtime/build/CI bảo vệ baseline | none | F0 | PROTECTED_PARTIAL |
| 002 / L002 | [identity-access](../../specs/002-identity-access/spec.md) | Identity account/Realm/grant/session | L001 | F0 | PROTECTED_PARTIAL |
| 003 / L003 | [sender-merchant-onboarding](../../specs/003-sender-merchant-onboarding/spec.md) | Hồ sơ cá nhân, Merchant application/approval và organization/membership | L002 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 004 / L004 | [address-coverage-service-catalog](../../specs/004-address-coverage-service-catalog/spec.md) | Địa chỉ/điểm lấy và catalog public | L001, L003 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 005 / L005 | [pricing-contracts-quotation](../../specs/005-pricing-contracts-quotation/spec.md) | Báo giá theo khách gửi/hợp đồng/thời điểm | L004 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 006 / L006 | [order-shipment-parcel-contract](../../specs/006-order-shipment-parcel-contract/spec.md) | Identity và guard Order/Shipment/Parcel | L003, L004, L005 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 007 / L007 | [shipment-booking-sender-payments](../../specs/007-shipment-booking-sender-payments/spec.md) | Booking Sender/Merchant và thu cước đúng | L005, L006, L018 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 008 / L008 | [parcel-label-merchant-fulfillment](../../specs/008-parcel-label-merchant-fulfillment/spec.md) | Chuẩn bị SKU/kiện và nhãn có kiểm đếm | L006, L007 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 009 / L009 | [tracking-event-core-public-lookup](../../specs/009-tracking-event-core-public-lookup/spec.md) | Tracking an toàn và có nguồn | L006 | F1 | BLOCKED_FOR_IMPLEMENTATION |
| 010 / L010 | [pickup-dispatch-execution](../../specs/010-pickup-dispatch-execution/spec.md) | Dispatcher giao việc, Courier nhận kiện | L007, L008, L009 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 011 / L011 | [station-intake-walkin-custody](../../specs/011-station-intake-walkin-custody/spec.md) | Station intake và staffed walk-in | L007, L008, L009, L010, L018 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 012 / L012 | [sorting-hub-manifests](../../specs/012-sorting-hub-manifests/spec.md) | Sorting và Hub manifests | L011 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 013 / L013 | [linehaul-trip-hub-transfer](../../specs/013-linehaul-trip-hub-transfer/spec.md) | Line-haul với đối chiếu hai đầu | L011, L012 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 014 / L014 | [delivery-dispatch-and-route](../../specs/014-delivery-dispatch-and-route/spec.md) | Phân công giao và route có custody | L011, L013 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 015 / L015 | [lastmile-attempt-pod](../../specs/015-lastmile-attempt-pod/spec.md) | Delivery attempt/POD và COD thực thu | L009, L014, L017 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 016 / L016 | [returns-reverse-logistics](../../specs/016-returns-reverse-logistics/spec.md) | Returns và bằng chứng gửi lại | L011, L015 | F2 | BLOCKED_FOR_IMPLEMENTATION |
| 017 / L017 | [cod-collection-cashier-remittance](../../specs/017-cod-collection-cashier-remittance/spec.md) | COD collection và Cashier remittance | L011, L018 | F3 | BLOCKED_FOR_IMPLEMENTATION |
| 018 / L018 | [billing-ledger-fees-invoices](../../specs/018-billing-ledger-fees-invoices/spec.md) | Ledger, billing, phí và chứng từ | L005, L006 | F3 | BLOCKED_FOR_IMPLEMENTATION |
| 019 / L019 | [cod-reconciliation-merchant-settlement](../../specs/019-cod-reconciliation-merchant-settlement/spec.md) | Reconciliation và settlement có điều kiện | L017, L018 | F3 | BLOCKED_FOR_IMPLEMENTATION |
| 020 / L020 | [customer-support-tickets-sla](../../specs/020-customer-support-tickets-sla/spec.md) | Ticket có nguồn/scope/SLA | L002 | F3 | BLOCKED_FOR_IMPLEMENTATION |
| 021 / L021 | [claims-investigation-compensation](../../specs/021-claims-investigation-compensation/spec.md) | Claims và compensation tách ticket | L016, L018, L020 | F3 | BLOCKED_FOR_IMPLEMENTATION |
| 022 / L022 | [enterprise-api-webhooks](../../specs/022-enterprise-api-webhooks/spec.md) | Enterprise API principal và webhook | L002, L003, L005, L007, L008, L009 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 023 / L023 | [bulk-shipment-import](../../specs/023-bulk-shipment-import/spec.md) | Import CSV/Excel với row outcomes | L007 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 024 / L024 | [partner-franchise-station-ops](../../specs/024-partner-franchise-station-ops/spec.md) | Partner chỉ vận hành điểm được giao | L003, L011 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 025 / L025 | [network-ops-routing-optimization](../../specs/025-network-ops-routing-optimization/spec.md) | Network capacity và routing có phiên bản | L012, L013, L014 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 026 / L026 | [risk-fraud-compliance](../../specs/026-risk-fraud-compliance/spec.md) | Risk case/hold/release đúng quyền | L006, L009 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 027 / L027 | [platform-admin-audit-governance](../../specs/027-platform-admin-audit-governance/spec.md) | Governance phân tách admin/IT/Auditor | L002, L003 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 028 / L028 | [notifications-analytics-sla](../../specs/028-notifications-analytics-sla/spec.md) | Notifications/KPI/SLA từ committed facts | L009, L019, L020 | F4 | BLOCKED_FOR_IMPLEMENTATION |
| 029 / L029 | [courier-offline-sync](../../specs/029-courier-offline-sync/spec.md) | Offline Courier durable queue/revalidation | L010, L015, L017 | F5 | BLOCKED_FOR_IMPLEMENTATION |
| 030 / L030 | [capstone-e2e-hardening-release](../../specs/030-capstone-e2e-hardening-release/spec.md) | Cross-module E2E/release/RQ | L003, L004, L005, L006, L007, L008, L009, L010, L011, L012, L013, L014, L015, L016, L017, L018, L019, L020, L021, L022, L023, L024, L025, L026, L027, L028, L029 | F5 | BLOCKED_FOR_IMPLEMENTATION |

## Execution và acceptance

Contract topology uses implementation_order, not blind numeric sort.003 customer membership before004 authenticated pickup;004/005 published minimum service catalog contract can be prepared independently for003 preferences but missing catalog blocks affected BC/full003 acceptance.004 public Station lookup projection requires real published dispatch011 master contract before declaring lookup complete, not fabricated seed authority. These are explicit minimum dependencies, not a claim full003/004/011 can each be accepted with missing consumer contracts.

018 ledger core after005/006 before007 charge;017 collection core after011/018 before015 delivery; full remittance/settlement later requires actual delivery/finance evidence.020 contact core early;028 minimum notifications/audit at source first use, full dashboards later. Hard prerequisites denote tested minimum contracts; reverse consumers can need early approved source-core agreements, not compile-time cycles or duplicate masters. [Cross-module contracts](../reconciliation/v3/cross-module-contracts.md).

F1 customer→booking003–009 +018 minimum finance; F2 physical010–016 +017 minimum collection; F3 full money/support017–021; F4 enterprise/import/Partner/network/risk/admin/analytics022–028; F5 native offline029/full E2E/RQ/release030.003 does not implement the entire customer/Merchant portal. Bulk023 can start once owning upstream contracts ready, not numerical completion of022. C–F all retained.

[GROUP_A_B_PORTAL_FULL_ACCEPTANCE](../portal/group-ab-acceptance.md) requires all110 A/B CORE,2 denials, supplemental Owner requirements, J-AB-01..10 and real carrier/network dependencies; no acceptance from navigation/mock UI. All future implementations NOT_STARTED.001/002 remain partial with documented findings, not reopened redesign.

## Decisions and bounded changes

Account-first business direction resolved; technical Identity amendment remains blocked. BC133 now authenticated R18. Pickup009/021/168 sole command in004/Merchant; Station230/305 in011/Dispatch; Sales329/330 in020/Merchant relationship only,331 in005/Pricing contract,335 in022/Merchant coordination; KPI source projections in028. [Exact corrections](../reconciliation/v3/ownership-corrections.csv). No extra domains/services or new BC IDs.

[Remaining blockers](../reconciliation/v3/decisions.md) close before first use; unrelated later policy does not block safe independent work. Full366 CORE effort/resource dates still unestimated; no14-week promise or CORE reduction. RQ1–4 retained from retired legacy016 in030, distinct Returns016 canonical. No automatic merge/implementation authorization.
