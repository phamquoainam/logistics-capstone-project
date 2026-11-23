# ArgoPost — OWNER TECHNICAL APPROVAL (ADM-02–08)

**Decision:** APPROVED — SOURCE/TEST ONLY  
**Date:** 2026-11-21  
**Target branch:** `feature/argopost-admin-dashboard`  
**Contract:** `docs/implementation/admin-dashboard-contract.proposed.md`  
**Contract SHA-256 (exact uploaded bytes):** `69d1ac81606199a2db644d8038553e19fd78bfddb2608e92713b2fa1396e2289`  
**Execution baseline in submitted evidence:** `d7329a84822f88a1c01e70a0e23b065123860410`

## 1. Approved implementation scope

Approve the bounded technical scope of **ADM-02, ADM-03, ADM-04, ADM-05, ADM-06, ADM-07, and ADM-08**, without changing the existing accepted ADM-01 authentication wire contract.

- **ADM-02:** Current, persisted, independently checked SYSTEM admission for dedicated verified ACTIVE Admin principal; explicit `ADMIN_DASHBOARD_READ`, `ADMIN_MERCHANT_READ`, `ADMIN_NETWORK_READ`, `ADMIN_SHIPMENT_READ`, `ADMIN_FINANCE_READ`, `ADMIN_TECHNICAL_READ` codes, with per-section least privilege. No implicit Carrier/Finance access for Identity Root, Customer, Merchant, or CSKH Staff; preserve current Identity-management admission and initial-Root safeguards.
- **ADM-03/04:** Exactly six specified GET endpoints and their bounded DTOs/filters; enforce no-store, parameter validation, status/scope restrictions, PII minimization, current permissions, 15-minute actor-bound snapshots and 409 `SNAPSHOT_EXPIRED` on unjustifiable cohort drift. The actor, scope and snapshot must never be trusted from browser input.
- **ADM-05:** Typed owner read ports and a single read-only repeatable-read PostgreSQL transaction for consistent cross-domain aggregates; no direct other-module internal imports or shadow business data.
- **ADM-06:** Real shipment/parcel/acceptance facts and confirmed fee/UNPAID obligation facts only. Correct distinct-count, time-zone cutoff and exact arithmetic; preserve the precise cohort for authorized drill-through. Explicitly mark unimplemented Merchant application review, official network, advanced lifecycle, SLA/priority and unsourced technical monitoring as unavailable, not zero or synthetic KPIs.
- **ADM-07:** Responsive navy/orange Admin shell, date filters, only authorized and sourced widgets, meaningful drill-through, loading/error/empty/expired-snapshot states; no fake approval actions.
- **ADM-08:** TDD and focused integration, HTTP/browser, negative authorization, regression, and PostgreSQL 18 isolated tests; evidence-based final report.

## 2. Mandatory boundaries

1. Source/test authorization **only**. Do not provision Admin accounts, create/alter live grants, activate SYSTEM realm, deploy to Local/Test demo or Production, alter credentials/certificates, or commit/push/merge under this approval.
2. No new migration, SQL privilege expansion, role template, operator bootstrap, production enablement or Admin onboarding is authorized. If an actual schema/constraint mismatch requires any of them, halt only that dependency and report the exact minimal delta for approval. Do not bypass a CHECK or migration rule.
3. Keep any new read admission **default-off and Local/Test-only**, including a separate test fixture/protocol. Do not relax production or existing Identity-management authorization to manufacture tests.
4. Do not treat a registered SYSTEM realm as a business region. Unknown/mixed realm/permission sets deny. Read grants never authorize ledger writes, payout, compensation, Carrier operational mutations or access to PII fields excluded by the contract.
5. Deny unauthorized sections at the backend **before fetching**, not by returning zero, masking as source-unavailable, or frontend-only hiding. Explicit drill-through requires its corresponding granular permission.
6. Preserve WIP, current migrations and `AGENTS.md`/ADR constraints. Existing ADM-01 frontend fixture PASS is not Backend, live SYSTEM, CI or full-suite evidence.

## 3. Execution and acceptance

Preflight: read `AGENTS.md`, contract, execution report, current Git/WIP, Identity account-grant constraints, active migration stream, source owners and current tests. Verify the contract SHA-256 before implementation. Where repo text differs from the uploaded contract, do not silently approve a different contract.

Implement and verify continuously: **inspect -> design details within approved scope -> TDD -> source -> isolated PostgreSQL 18 -> HTTP/browser -> security regression -> report**. Fix ordinary in-scope defects without repeat Owner reviews. Fail-closed and provide an exact delta only for material contract, migration, security, privacy or financial-authority changes.

Acceptance requires actual evidence of ADM-02–08 against approved source/read domains and negative tests: Customer/Staff/demo/Identity Root denial, revocation and stale-permission denial, unauthorized section filtering, malicious/duplicate query rejection, PII absence, correct shipment dedup/finance obligation arithmetic, snapshot tamper/expiry/change, and exact list-to-metric drill-through. Report which source statuses are intentionally unavailable. Do not assert complete Admin business operations while data sources or approved workflows are missing.

**Proposed final checkpoint (only after gates PASS):** `ADMIN_DASHBOARD_SOURCE_TEST_READY`.

**Not covered:** Real Admin onboarding; SYSTEM activation; demo/live UAT; production rollout; CI without an actual run; any additional business-write workflows.

This decision is based on the two supplied documentation artifacts, not an independent local-repository audit or a claim that implementation tests were executed in this review.
