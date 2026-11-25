# ArgoPost Internal Web Application and Administration Console

Owner directive2026-11-23 authorizes the product restructuring and ordinary
implementation using existing approved contracts. This document records that
implementation; it grants no new Backend, database, deployment or role authority.

## Inputs and preservation

Both DOCX inputs were fully extracted and read in body order, including all table
cells and available headers/footers/notes. No source document was edited.

| Source | SHA256 | Extracted content |
|---|---|---|
| [Document A](../business-references/ArgoPost_Admin_Portal_Functional_Menu_Specification.docx) | ea739f8f09ccd65b6458e4bfcbfd290e76f6567ad5217753689148bfb3952505 |505 paragraphs;7 tables;36237 text characters |
| [Document B](../business-references/ArgoPost_Administration_Console_Admin_Portal_Functional_Menu_Specification_Full.docx) |58f43be7ba88255fb1483fe3c3222f02822e2a668c6079d785d168f4d7987a46 |1342 paragraphs;16 tables;90793 text characters |

The19/27 page descriptions are the supplied document identities, not newly rendered
pagination claims. The separate35-page Internal specification and74-page handbook
are referenced inside B; they are not additional inputs read in this checkpoint.
Use B section VIII's explicit mapping, including merged Internal ADM-08 and split
Internal ADM-11/12. Do not invent page locators from XML paragraph numbers.

Starting branch feature/argopost-admin-dashboard, HEAD248d651. Private preflight:
`.demo/admin-console-preflight-59c74427a4154ade8f0157f0e2797cd2/` contains HEAD archive,
the two input WIP files, complete text extractions and hashes of39 migration files.
The original51-path checkpoint and37 historical migrations remain untouched.
Read-only UAT process check found both recorded processes absent; no restart,
configuration, database, grants or UAT deployment is part of this change.

## One application with separate admission boundaries

```mermaid
flowchart TD
  Entry[Existing React web entry] --> Internal[InternalApp and shared InternalShell]
  Internal --> Ops[/ops Operational Execution]
  Internal --> Finance[/finance Business and Finance Execution]
  Internal --> Admin[/admin Administration Control Plane]
  Ops --> Closed[Unavailable until operational admission is approved]
  Finance --> Support[Existing SUPPORT Staff client and CSKH workflows]
  Admin --> System[Existing SYSTEM Admin admission and six READ APIs]
```

The existing customer-mode web artifact already hosts the internal routes. The
new InternalApp is the common application boundary; no new repository, website,
backend artifact, auth engine or credential store. Customer initialization does
not run for internal paths. Customer/Public routes remain outside InternalApp.
Invitation fragments are captured/scrubbed before session work, exactly as before.

InternalShell supplies shared header/sidebar, portal context, environment,
authentication status, breadcrumbs, account/logout controls, timezone, mobile
navigation and keyboard focus. Both the real Administration Dashboard and existing
Staff CSKH workbench use it. SUPPORT does not acquire Finance ledger authority.

The portal selector intentionally denies cross-purpose switching: the current
contracts cannot discover and activate all three contexts for one identity.
SYSTEM Admin credentials/cookies/tokens never become Staff credentials. Staff
entry still uses staff-auth/me and current domain permission/binding checks;
Customer and Merchant sessions cannot populate either internal client.
Cross-Portal Integration is therefore BLOCKED, not READY. See the single
[consolidated amendment](admin-console-contract-amendment.proposed.md).

## Implemented routing and navigation inventory

| UI route | Current behavior | Backend authority |
|---|---|---|
| /admin/login; /admin/activate | Existing real SYSTEM login/invitation; updated product naming | Existing auth/proof/CSRF engines |
| /admin; /admin/dashboard | Six permission-aware Dashboard tabs | Existing context/dashboard/platform reads |
| /admin/shipments | Real canonical contributing Shipment list with snapshot and preserved filters | ADMIN_SHIPMENT_READ |
| /admin/finance | Confirmed fees and UNPAID exposure; no execution | ADMIN_FINANCE_READ; Shipment read also required for drill |
| /admin/platform/health | Actual responding Admin API plus truthful missing telemetry | ADMIN_TECHNICAL_READ |
| /admin/account | Own current context/grants; canonical logout | ADMIN_DASHBOARD_READ |
| /admin/merchants/applications; /admin/merchants/organizations | Explicit dependency views, no fabricated directory/review records | Existing MERCHANT read needed to open view |
| /admin/network; /admin/partners; /admin/sla | Explicit source/dependency views, no simulated network/SLA records | Existing NETWORK or SHIPMENT read needed |
| Other registered /admin module paths | Forbidden until an exact capability is approved; disabled sidebar items | No authority inferred from knowing route |
| Unknown /admin paths | Safe forbidden boundary after authentication | No new resource APIs |
| /finance; /finance/support | Actual existing CSKH workbench through Staff admission | Canonical SUPPORT plus case/Organization binding |
| /staff/login; /staff/activate; /staff/cases | Compatible existing Staff entry/legacy deep links | Same existing Staff engine |
| /ops and descendants | Explicit unavailable admission boundary, no operational requests | No execution authority opened |

The nine parent groups and all23 target module IDs are centralized in
`app/frontend/web/src/internal/navigation.ts`, matching B section II. Disabled
items and dependency views are not implemented business modules. ADM-20 is linked
from both appropriate groups, using the same canonical target module identity.

## Dashboard behavior and metric limitations

Overview retains the accepted live KPI/chart/drill experience. Finance Exposure
shows only financial metrics/trends. Merchant Queue, Network & SLA and Decisions
state the exact missing source/workflow without fake counters or action buttons.
Platform Health is only visible/called with its granular technical grant.
Time/status filters reuse the existing query; tab changes do not refetch/redefine
the cohort. Unrecognized scope/region query parameters fail closed in UI; Backend
continues rejecting unsupported region filters. Realm is not an operating region.

Snapshot selectors remain server-generated/current-authority/HMAC/cohort bound.
The UI separately identifies expired15-minute snapshots and disables stale drill.
No silent refresh into a different list. Error/forbidden/unavailable/empty/degraded
states are distinct. Last source event and source freshness are explicitly unknown
where the wire contract does not supply them. Refresh time is never relabeled as
a source event. Metric version metadata remains an amendment, not an invented DTO.

Booking count, accepted count, Parcel count and VND sums retain their original
definitions. Parcel KPI contributes via Shipment rows and their Parcel counts;
it is not a Parcel-event search. Rates beyond supported state shares are absent.
UNPAID is neither overdue nor paid revenue, COD collected, reconciled or settled.

## Security and change boundaries

No Backend production source, API/DTO, permission, schema, migration or grants are
changed. Existing six READ APIs enforce authorization in Backend; sidebar checks
are additional UX only. The new route catalog exposes no business data itself.
No Admin-created custody/POD/Carrier events, ledger posting, claim payout,
impersonation, PII reveal, approval task or runtime feature mutation.

Role labels in the new documents remain documentary target roles until the
[role reconciliation](admin-console-role-capability-mapping.md) is approved.
They are not renamed current grants or role-based permission templates.

## Acceptance boundaries

See [execution](admin-console-implementation-execution.md) for actual commands and
evidence. Shared shell source/test acceptance is independent of complete ADM-01
business-source coverage. Full Administration Console, cross-portal context
switching, newly deployed Local UAT and Production readiness are not implied by
successful UI or fixture tests.
