# ArgoPost Admin Portal — bounded implementation contract

Status: **PROPOSED — Admin business authority/read API not approved**. 2026-11-21.
Baseline: `d7329a84822f88a1c01e70a0e23b065123860410`. No deployment authorization.

## ADM-01 — accepted authentication UI, independent work

Implement `/admin/login` and `/admin` browser routes in the existing HTTPS web
application. Reuse the accepted SYSTEM operations without changing their wire
contract: GET `/api/v1/auth/csrf`, POST `/api/v1/auth/login`, POST
`/api/v1/auth/refresh`, GET `/api/v1/auth/me`, POST `/api/v1/auth/logout`.
Username/password, secure HttpOnly cookies, Origin/CSRF verification and current
persisted admission remain authoritative. Do not use the demo ADMIN principal,
Customer transport or SUPPORT-purpose Staff session. No public registration.

Access token and account projection remain memory-only; serialize refresh across
tabs with a separate browser lock. Fail closed on malformed responses, mismatched
account IDs, unavailable lock, revoked/expired session or HTTP deployment. No
automatic retry of an ambiguous login/refresh/logout POST. Do not persist password,
token, proof or account data in browser storage. Logout failure must be reported as
unconfirmed server revocation, even after clearing browser memory.

An authenticated SYSTEM Identity administrator is **not** automatically a Carrier
business administrator. Until ADM-02–08 are approved/implemented, show a truthful
access-boundary screen; no fake dashboard, KPI placeholders presented as data or
calls to unapproved Admin endpoints. Existing SYSTEM realm stays unchanged.

## ADM-02 — [PROPOSED] current Admin admission amendment

Reuse Identity session, credential, verification and registered SYSTEM realm.
Extend current session eligibility only for a dedicated, verified ACTIVE account
that has no Customer identifier or SUPPORT Staff binding, and whose complete
current canonical `identity.account_grant` set consists of the following explicit
codes in the **same registered ACTIVE SYSTEM realm**:

| ID | Code | Meaning |
|---|---|---|
| ADM-P-01 | `ADMIN_DASHBOARD_READ` | Enter Carrier overview; no implicit data permissions |
| ADM-P-02 | `ADMIN_MERCHANT_READ` | Read aggregate Merchant application facts and bounded list |
| ADM-P-03 | `ADMIN_NETWORK_READ` | Read official active-network facts and bounded list |
| ADM-P-04 | `ADMIN_SHIPMENT_READ` | Read Carrier shipment/parcel counts and bounded list |
| ADM-P-05 | `ADMIN_FINANCE_READ` | Read confirmed fee/obligation aggregates, not ledger writes |
| ADM-P-06 | `ADMIN_TECHNICAL_READ` | Read allowlisted technical availability only |

The stored pair is `(role_code, scope_type=IDENTITY, scope_id=SYSTEM realm UUID)`;
here the realm is an **admission boundary**, not a Merchant, region or permission to
manage Identity. ADM-P-01 is mandatory. Mixed-realm, mixed Identity-management,
unknown-code, empty and inactive sets deny. Old exact
`IDENTITY_MANAGEMENT/IDENTITY/R` admission and initial-Root/last-admin protections
remain unchanged. Root has no implicit dashboard/Finance access. No role-name
wildcards or external token claims confer authority. Recheck current persisted
grants on every request/refresh, under existing Identity lock/version protocol.

This first slice is Local/Test source/test only, default disabled. Existing SYSTEM
realm must not be activated. Existing provisioning/delegation does **not** permit
these new codes: runtime grant replacement/provisioning must continue to deny them.
No new provisioning API/operator function, grant execution, bootstrap, new Staff
purpose, schema, role template or migration is proposed in this slice. A deployable
Admin onboarding procedure is a separate dependency; fixtures are not provisioning.
Production admission remains closed. Region/facility-scoped Admin delegation is
not implemented by treating a SYSTEM realm as a business region.

## ADM-03 — [PROPOSED] HTTP read manifest

Every operation requires an admitted SYSTEM session and ADM-P-01. No Customer,
Merchant or CSKH authority substitution. All responses `no-store`; query tokens
and anonymous access deny. Paths are under the existing protected Admin transport.

| ID | Method/path | Result DTO | Additional permission |
|---|---|---|---|
| ADM-API-01 | GET `/api/v1/admin/dashboard/context` | AdminDashboardContext | none |
| ADM-API-02 | GET `/api/v1/admin/dashboard` | AdminDashboard | only requested authorized sections |
| ADM-API-03 | GET `/api/v1/admin/dashboard/shipments` | AdminShipmentPage | ADM-P-04 |
| ADM-API-04 | GET `/api/v1/admin/dashboard/merchant-applications` | AdminApplicationPage | ADM-P-02 |
| ADM-API-05 | GET `/api/v1/admin/dashboard/network` | AdminNetworkPage | ADM-P-03 |
| ADM-API-06 | GET `/api/v1/admin/dashboard/platform` | AdminPlatformStatus | ADM-P-06 |

Errors: 400 VALIDATION_FAILED; 401 SESSION_INVALID; 403 ACCESS_DENIED;
409 SNAPSHOT_EXPIRED; 422 SOURCE_UNAVAILABLE; 503 SERVICE_UNAVAILABLE.
No raw database/exception/account data in errors. Forbidden sections are omitted,
not returned as zero or fetched then hidden by Frontend. Unknown query parameters,
duplicate parameters and unsupported status/dimension combinations reject.

## ADM-04 — [PROPOSED] exact DTO/filters

- AdminDashboardContext: `accountId`, `displayName`, `permissions` (approved codes),
  `environment` (LOCAL/TEST), `timeZone` (Asia/Ho_Chi_Minh), `regionFilterAvailable`
  (false until a canonical operational region source/assignment exists).
- AdminDashboardQuery: `from`, `to` (inclusive ISO local dates, maximum 366 days),
  `grain` (DAY/WEEK/MONTH), optional `shipmentStatus` (BOOKED/AT_STATION),
  optional `regionId`. Nonempty regionId rejects until authoritative scope exists;
  it must not expand a user's allowed data. Dates required, no browser-timezone
  inference. Clock/cutoff supplied by Backend.
- AdminDashboard: `query`, `generatedAt`, `snapshotId`, `sections`.
- AdminSection: `key` (MERCHANT/NETWORK/SHIPMENTS/FINANCE/SLA/PRIORITIES),
  `availability` (AVAILABLE/SOURCE_NOT_IMPLEMENTED/POLICY_NOT_CONFIGURED),
  `reasonCode`, `metrics`, `series`. Unavailable sections have empty metrics/series.
- AdminMetric: `key`, `value` (exact integer/decimal string), `unit`
  (SHIPMENT/PARCEL/VND/MILLISECOND), `drilldown` (nullable approved list/filter).
  Value is never an inferred currency payment or an unsupported zero.
- AdminSeriesPoint: `from`, `to` (exclusive), `metric`, `value`.
- AdminPageQuery: same cohort plus `snapshotId`, `page` (0..10000), `size`
  (1..50), `metric`. `metric` is a finite owner-defined allowlist, not SQL/URL.
- AdminShipmentPage: `items`, `page`, `size`, `hasNext`, `snapshotId`.
  Item: `id`, `code`, `ownerKind`, `bookedAt`, `status`, `parcelCount`.
  No sender/recipient name, phone, address, email or Merchant legal data.
- AdminApplicationPage: same page envelope. Item: `id`, `state`, `submittedAt`,
  `updatedAt`. Only real submitted workflow facts; Draft is not a submitted case.
- AdminNetworkPage: same page envelope. Item: `id`, `code`, `kind`, `status`.
  Canonical official sources only; SIM-DROP is not an official Station/Hub/Partner.
- AdminPlatformStatus: `checkedAt`, `apiAvailability` (UP/UNAVAILABLE),
  `queues`, `scheduledJobs`, `integrationFailures`, each represented by
  availability plus bounded aggregate facts only when backed by a source.
  No hostnames, connection strings, credentials, arbitrary actuator payload or stack.

Snapshot is a short-lived server-generated, authenticated selector binding actor,
realm, current permission version, normalized query, cutoff and 15-minute expiry.
Not a browser-supplied authority or persisted business entity. A click must retain
the cohort; current authorization still rechecked. If rows affecting the metric
changed since cutoff and historical reproduction is unavailable, reject with
SNAPSHOT_EXPIRED and offer refresh; never claim the new list equals the old count.
Do not introduce unbounded snapshot caches/tables. Offset pages use deterministic
`bookedAt DESC,id DESC`; no stable-history claim across refresh.

## ADM-05 — [PROPOSED] owner APIs and transaction

Identity exposes typed `AdminAdmission` / `AdminReadContext`, populated only from
current SYSTEM admission, not client input. Domain public read ports:
Merchant `AdminApplicationFacts`, Dispatch `AdminNetworkFacts`, Orders
`AdminShipmentFacts`, Finance `AdminObligationFacts`, Notifications
`AdminDashboardReads`. Each receives the admitted immutable context plus normalized
cohort; no `othermodule.internal` imports. Workflow composes reads in one read-only
repeatable-read local transaction; domain SQL remains with its owner. Empty/missing
source and unavailable infrastructure are different outcomes. No cross-tenant
Customer/Business API enumeration or independent shadow business tables.

## ADM-06 — actual source availability and metric semantics

| Requested group | Current source / correct first-slice behavior |
|---|---|
| Merchant review | V14 `merchant.application` CHECK state=DRAFT. No submitted/review source: unavailable; no invented processing time, no counting operator Organization creation as approval. |
| Official network | V21 `dispatch.pilot_station` is SIM. Official Station/Hub/Partner counts unavailable. |
| Booking / parcels | V20/V26 confirmed Shipment/Parcel facts, PERSONAL and BUSINESS Local/Test. Count distinct Shipment IDs; count Parcel IDs separately. |
| Acceptance | V21 intake acceptance plus immutable tracking; count accepted Shipments once, not both CARRIER_ACCEPTED and AT_STATION events. |
| In transit / delivered / failed / return | No accepted lifecycle source implemented: unavailable, not fabricated zero or percentage. |
| Finance | Confirmed snapshot fees and current UNPAID obligations only. Separate confirmed amount/unpaid amount; no revenue, paid/overdue/COD/reconciliation inference. |
| SLA / escalations / priority decisions | No approved SLA/decision queue source: unavailable. High-priority Support ticket is not an approval escalation. |
| Platform | Actual bounded API check for explicit technical grant; absent queue/job/integration telemetry remains unavailable. |

Booking cohort uses `bookedAt` in Vietnam [from 00:00, dayAfter(to) 00:00) converted
to UTC. Status is current legal status at consistent read cutoff; this is not an
event-occurrence report. Finance sums only the same distinct confirmed shipment
cohort, exact VND, one applicable obligation per Shipment; joins must not multiply
fees by Parcel/event count. Acceptance time only from real acceptance timestamps.
Never calculate a success rate when the denominator/lifecycle source is absent.

## ADM-07 — UX

Navy #0B2D5B, orange #FF7A00, existing primary ArgoPost logo. Separate Admin
shell and compact date filter, meaningful permitted KPI cards and real trends,
bounded priority list only when sourced. No Customer quick actions or shipping
workspace switcher. Unauthorized widgets are absent; unavailable sources are
explained once in a compact source-status area, not repeated warning banners.
Drill-through opens the exact filtered list, preserves selection/back navigation,
and has loading/empty/error/expired-snapshot states. No identical report pages
with different headings, decorative fake charts or nonfunctional approval buttons.

## ADM-08 — gates and sequence

1. ADM-01: existing-wire login client/UI; unit + intercepted-browser tests, typecheck,
   lint, build. This is frontend contract verification, not live SYSTEM acceptance.
2. Owner approves ADM-02–08 before new authority/API implementation.
3. TDD current admission, least privilege, Customer/Staff/demo denial, revocation,
   scope/PII isolation, unknown-filter denial, exact fee/dedup math, empty vs missing
   sources, snapshot tamper/expiry/change, cross-actor replay, real list contribution.
4. Isolated PostgreSQL18 read integration and existing Identity/Customer/Staff
   regressions. No migration unless a separately identified necessity is approved.
5. Frontend/HTTP/Browser gates on the implementation revision, source inventory and
   repository validators. Do not claim CI or full suite PASS without execution.
6. Separate deploy/provision/activation checkpoint. No demo account/grants/realm,
   credentials, certificates, Production, commit/push/merge in this source/test task.

Task state: ADM-01 IN_PROGRESS; ADM-02–08 AWAITING_TECHNICAL_APPROVAL.
No existing canonical task is marked DONE by this proposal.
