# Canonical SDD V3 — authoritative target register

Documentation baseline promoted by Owner authorization2026-10-27. Canonical target != accepted wire/schema != implemented != feature accepted.001/002 protected partial status remains; current runtime Demo is bounded regression only. Logical L aliases retained, numeric003–030 are one active hierarchy.

[Delivery principles](../docs/implementation/delivery-principles-v3.md) · [Roadmap](../docs/implementation/roadmap-v3.md) · [Retirement/history](../docs/reconciliation/v3/legacy-retirement.md) · [371 BC](../docs/reconciliation/v3/capability-crosswalk.csv) · [Portal](../docs/portal/group-ab-coverage.md).

| Canonical ID / alias | Spec | Domain accountable (BC DML owner separate) | Minimum hard contracts | Status |
|---|---|---|---|---|
| 001 / L001 | [platform-foundation](001-platform-foundation/spec.md) | platform | none | PROTECTED_PARTIAL |
| 002 / L002 | [identity-access](002-identity-access/spec.md) | identity | L001 | PROTECTED_PARTIAL |
| 003 / L003 | [sender-merchant-onboarding](003-sender-merchant-onboarding/spec.md) | merchant | L002 | SPEC003_READY_FOR_OWNER_IMPLEMENTATION_AUTHORIZATION |
| 004 / L004 | [address-coverage-service-catalog](004-address-coverage-service-catalog/spec.md) | merchant | L001, L003 | BLOCKED_FOR_IMPLEMENTATION |
| 005 / L005 | [pricing-contracts-quotation](005-pricing-contracts-quotation/spec.md) | pricing | L004 | BLOCKED_FOR_IMPLEMENTATION |
| 006 / L006 | [order-shipment-parcel-contract](006-order-shipment-parcel-contract/spec.md) | orders | L003, L004, L005 | BLOCKED_FOR_IMPLEMENTATION |
| 007 / L007 | [shipment-booking-sender-payments](007-shipment-booking-sender-payments/spec.md) | orders | L005, L006, L018 | BLOCKED_FOR_IMPLEMENTATION |
| 008 / L008 | [parcel-label-merchant-fulfillment](008-parcel-label-merchant-fulfillment/spec.md) | warehouse | L006, L007 | BLOCKED_FOR_IMPLEMENTATION |
| 009 / L009 | [tracking-event-core-public-lookup](009-tracking-event-core-public-lookup/spec.md) | orders | L006 | BLOCKED_FOR_IMPLEMENTATION |
| 010 / L010 | [pickup-dispatch-execution](010-pickup-dispatch-execution/spec.md) | dispatch | L007, L008, L009 | BLOCKED_FOR_IMPLEMENTATION |
| 011 / L011 | [station-intake-walkin-custody](011-station-intake-walkin-custody/spec.md) | dispatch | L007, L008, L009, L010, L018 | BLOCKED_FOR_IMPLEMENTATION |
| 012 / L012 | [sorting-hub-manifests](012-sorting-hub-manifests/spec.md) | dispatch | L011 | BLOCKED_FOR_IMPLEMENTATION |
| 013 / L013 | [linehaul-trip-hub-transfer](013-linehaul-trip-hub-transfer/spec.md) | dispatch | L011, L012 | BLOCKED_FOR_IMPLEMENTATION |
| 014 / L014 | [delivery-dispatch-and-route](014-delivery-dispatch-and-route/spec.md) | dispatch | L011, L013 | BLOCKED_FOR_IMPLEMENTATION |
| 015 / L015 | [lastmile-attempt-pod](015-lastmile-attempt-pod/spec.md) | orders | L009, L014, L017 | BLOCKED_FOR_IMPLEMENTATION |
| 016 / L016 | [returns-reverse-logistics](016-returns-reverse-logistics/spec.md) | orders | L011, L015 | BLOCKED_FOR_IMPLEMENTATION |
| 017 / L017 | [cod-collection-cashier-remittance](017-cod-collection-cashier-remittance/spec.md) | finance | L011, L018 | BLOCKED_FOR_IMPLEMENTATION |
| 018 / L018 | [billing-ledger-fees-invoices](018-billing-ledger-fees-invoices/spec.md) | finance | L005, L006 | BLOCKED_FOR_IMPLEMENTATION |
| 019 / L019 | [cod-reconciliation-merchant-settlement](019-cod-reconciliation-merchant-settlement/spec.md) | finance | L017, L018 | BLOCKED_FOR_IMPLEMENTATION |
| 020 / L020 | [customer-support-tickets-sla](020-customer-support-tickets-sla/spec.md) | orders | L002 | BLOCKED_FOR_IMPLEMENTATION |
| 021 / L021 | [claims-investigation-compensation](021-claims-investigation-compensation/spec.md) | orders | L016, L018, L020 | BLOCKED_FOR_IMPLEMENTATION |
| 022 / L022 | [enterprise-api-webhooks](022-enterprise-api-webhooks/spec.md) | merchant | L002, L003, L005, L007, L008, L009 | BLOCKED_FOR_IMPLEMENTATION |
| 023 / L023 | [bulk-shipment-import](023-bulk-shipment-import/spec.md) | orders | L007 | BLOCKED_FOR_IMPLEMENTATION |
| 024 / L024 | [partner-franchise-station-ops](024-partner-franchise-station-ops/spec.md) | dispatch | L003, L011 | BLOCKED_FOR_IMPLEMENTATION |
| 025 / L025 | [network-ops-routing-optimization](025-network-ops-routing-optimization/spec.md) | routing | L012, L013, L014 | BLOCKED_FOR_IMPLEMENTATION |
| 026 / L026 | [risk-fraud-compliance](026-risk-fraud-compliance/spec.md) | orders | L006, L009 | BLOCKED_FOR_IMPLEMENTATION |
| 027 / L027 | [platform-admin-audit-governance](027-platform-admin-audit-governance/spec.md) | identity | L002, L003 | BLOCKED_FOR_IMPLEMENTATION |
| 028 / L028 | [notifications-analytics-sla](028-notifications-analytics-sla/spec.md) | notification | L009, L019, L020 | BLOCKED_FOR_IMPLEMENTATION |
| 029 / L029 | [courier-offline-sync](029-courier-offline-sync/spec.md) | dispatch | L010, L015, L017 | BLOCKED_FOR_IMPLEMENTATION |
| 030 / L030 | [capstone-e2e-hardening-release](030-capstone-e2e-hardening-release/spec.md) | platform | L003, L004, L005, L006, L007, L008, L009, L010, L011, L012, L013, L014, L015, L016, L017, L018, L019, L020, L021, L022, L023, L024, L025, L026, L027, L028, L029 | BLOCKED_FOR_IMPLEMENTATION |

DoR: docs/codex/definition-of-ready.md. Full acceptance includes all assigned BC and Owner supplemental requirements, legacy obligations, real layers/evidence and Owner record. No new spec31; product milestone lives docs/portal.
