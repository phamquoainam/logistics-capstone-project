# E-Logistics — canonical SDD V3

First-party Logistics Management Platform on existing modular monolith/React/RN. **Spec-first → Spec-by-Spec → Full-stack End-to-end Delivery → Full Spec Acceptance.** Canonical target planning, no production coding authorized by this docs rebuild.

1. Read AGENTS.md, [PRD](prd.md), [principles](principles.md), accepted architecture/security and [latest account-first decision](decisions/customer-account-merchant-admission.md).
2. Use [canonical index](../specs/INDEX.md), [roadmap](implementation/roadmap-v3.md), [delivery principles](implementation/delivery-principles-v3.md), [DoR](codex/definition-of-ready.md).001/002 retained,003–030 one active hierarchy; no duplicate planning/legacy trees.
3. Read own spec/design/tasks,371-BC crosswalk and material blockers; accepted contracts only in contracts/. Actual source app/backend, app/frontend/web, app/frontend/shipper-mobile. Proposed names are not official wire/schema.
4. [Portal coverage](portal/group-ab-coverage.md), [navigation](portal/navigation-permissions.md), [screenshot benchmark](portal/screenshot-benchmark.md), [journeys](portal/journeys.md), [product acceptance](portal/group-ab-acceptance.md) cover Group A/B across specs, not everything in003.
5. [Retirement and source provenance](reconciliation/v3/legacy-retirement.md) preserve old REQ/AC/tasks/RQ/Demo history. Demo code/contracts/migrations/tests/evidence remain;001/002 accepted bounded implementations/status/findings unchanged.

Registration: one personal account→verify/login→personal workspace→Merchant application→review→controlled organization/Owner activation. Personal remains usable after unrelated Merchant rejection/revoke. No Guest online shipment, Recipient IAM requirement, human/API credential sharing or implicit admin privileges.

Status vocabulary: CANONICAL_TARGET documentation may remain DESIGN_CANDIDATE/BLOCKED/NOT_STARTED. PLANNED != IMPLEMENTED; UI CREATED != INTEGRATED; PR MERGED != SPEC ACCEPTED != PORTAL/FULL PLATFORM ACCEPTED. No runtime/benchmark evidence is created by rebuilding docs.
