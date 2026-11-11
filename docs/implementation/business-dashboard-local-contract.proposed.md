# ArgoPost Business Dashboard — controlled Local/Test opening

Status: OWNER ACCEPTED — Local/Test only. Date: 2026-11-10.
Owner answer: “Duyệt Business Local/Test theo contract”. Filename retained for provenance.
Scope: requested Business Overview first; no Admin approval UI in this slice.
Authority: current Owner request; AGENTS.md exact API/DTO/table gate still applies.
Maps to BC-017 / BC-172, Spec028 analytics and Spec003 current organization access.

## Observed baseline and first task

- Branch `feature/argopost-overview-completion`, HEAD
  `59648b4bf4ba02ca069bd0fbf9529889134f3d59`, 87 pre-existing working source paths.
  SHA-256 backup verified under `.demo/business-dashboard-review/before-source`.
- Customer Identity/current admission exists. Merchant V14 is an application DRAFT
  aggregate; no runtime Organization/Membership/grant master or accepted context API.
  Historical `/api/demo/v1/merchant/me` is not Customer Business authorization.
- V19/V20 Draft/Shipment owner_type is constrained to PERSONAL. Existing Pricing,
  Booking, UNPAID and Acceptance pilot exceptions explicitly exclude Merchant.
  V22 is already deployed; never edit V1–V22.
- Personal analytics is self-only. Summing members' Personal orders, copying Demo
  orders or interpreting a missing Merchant source as zero is not permitted.

- [x] BD-01 Inventory and preserve WIP; identify concrete gates.
- [x] BD-02 Owner accepts exact bounded amendment/API/data contract below.
- [x] BD-03 Forward migration, operator setup, current membership/RBAC admission (PG18 verified; exact Owner-confirmed local target provisioned).
- [x] BD-04 Business shell/switcher/Overview connected to permitted Backend sources.
- [x] BD-05 PG18 isolation/revocation/upgrade and HTTP/browser/FE regression.

Before BD-02, only isolated UI/test spike is allowed; no active Business route,
credentials, grant, migration execution, seed or source reassignment.

BD-02 was accepted by Owner. BD-03–05 implementation gates are verified; exact
Owner-confirmed local provisioning is recorded in the checkpoint. This is not
full Business Shipping/Finance acceptance or permission to pick another account.
See [implementation checkpoint](business-dashboard-checkpoint.md).

## Amendment BUSINESS-LOCAL-ACCESS-01 [PROPOSED]

Defer Admin approval workflow development. Allow an explicit offline Owner/operator
command in Local/Test to provision a DEVELOPMENT organization and initial ACTIVE
membership for an existing ACTIVE, verified, eligible Customer. The operator must
choose the account UUID and organization name/UUID; no first-login/public-registration
self-grant, no default privileged account and no email/domain-derived membership.

This is development access, not representative/legal verification or application
approval. Keep any submitted application unchanged; record provenance
LOCAL_OPERATOR_PROVISIONED and operator/request/reason/time. Never create an
APPROVED application or use MANUAL_SANDBOX_REVIEWED falsely. Admin approval later
needs its own independent authority/workflow; this origin cannot become Production
eligible by toggling a profile or environment variable.

Enforce LOCAL/TEST + Customer SANDBOX and deny active production/prod profiles.
Organization admission provenance remains Local/Test-only in DB and every read.
Default feature disabled. Operator function runtime permissions denied; no HTTP
grant endpoint. Do not execute real provision/grant without explicit target account.

Keep existing Identity verified session, TLS, origin/CSRF, lock order and transaction.
Merchant admission calls public CustomerAdmission, then Merchant control and org/
membership/grant locks; every permission mutation and read uses the same protocol.
Current member revocation/org suspension/account disable must deny subsequent
requests and in-flight reads serialize consistently. An org ID or frontend switch
is context selection, never authority. No imported othermodule.internal classes.

## Minimal data contract [PROPOSED]

Allocate V23 only after fresh migration inventory; additive, no applied migration edit.
Merchant-owned names reuse canonical concepts, not Demo tables:

| Table | Exact bounded fields / invariants |
| --- | --- |
| merchant.organization | id UUID PK; display_name varchar(160); state ACTIVE/SUSPENDED; admission_origin fixed LOCAL_OPERATOR_PROVISIONED; environment LOCAL/TEST; initial_owner_membership_id UUID; version bigint; created_at/updated_at timestamptz. No legal verification claims. |
| merchant.membership | id UUID PK; organization_id FK; account_id immutable Identity UUID; role OWNER/MANAGER/OPERATOR; state ACTIVE/REVOKED; authority_version/version bigint; created_at/updated_at. UNIQUE(org,account); composite(id,org) key. No runtime account creation/Identity DML. Revoked membership not resurrected. |
| merchant.member_grant | id UUID PK; membership_id + organization_id composite FK; permission; scope fixed ORGANIZATION for this slice; UNIQUE(membership,permission). No wildcard/client supplied permission. |
| merchant.workspace_audit | id UUID PK; organization_id/member_id refs; operator_principal; operation PROVISION/GRANT/REVOKE/SUSPEND; request_id; reason varchar(300); occurred_at. Append-only. No passwords/email/token/contact payload. |
| merchant.workspace_command_receipt | request_id UUID PK; canonical input digest; organization_id; membership_id; operation; recorded_at. Same transaction as effect+audit; conflict rejected, same command replay has one effect. Current authority required before disclosure. |

DB deferred initial Owner membership same-org/ACTIVE/OWNER check for active org,
immutable org/member identity/provenance, explicit grant allowlist, no delete/audit
update/DDL granted to runtime. Account references validated through public Identity
admission, not Merchant cross-schema SELECT or invented Identity foreign keys.
Offline provisioning must attest existing account eligibility through the existing
controlled Identity/operator boundary, not synthesize verification.

Only explicit ORGANIZATION grants in V1. A facility-limited member cannot receive
organization totals from these endpoints. Facility grants/warehouses are deferred
until an accepted same-org Facility master and source lineage exist; requests for
such scope fail closed, not silently widen to all warehouses.

Permissions proposed: `dashboard.read`, `dashboard.finance.read`,
`dashboard.staff.read`. Role label does not grant permissions. Initial development
Owner receives only these explicitly recorded grants. A Manager/Operator may get
a chosen subset through the operator, never by self-promotion. Financial/team
permissions require dashboard.read; they still cannot activate missing sources.

## HTTP / DTO [PROPOSED]

Customer authentication chain; all responses Cache-Control:no-store; no PII.

1. `GET /api/v1/customer/business-contexts`, no query/body.
   `schemaVersion: BUSINESS_CONTEXTS_V1`, `items` array of
   `{organizationId, displayName, membershipId, role, permissions, authorityVersion}`.
   Only current ACTIVE Local/Test org + ACTIVE own membership + dashboard.read.
   No membership -> empty list, not fabricated membership. Request cannot select
   another Customer. Feature unavailable -> 503 BUSINESS_DISABLED.
2. `GET /api/v1/merchant-organizations/{organizationId}/dashboard`
   allowed query: `from`, `to` ISO dates inclusive, max366 days; `interval`
   DAY/WEEK/MONTH. Default last30 days, Asia/Ho_Chi_Minh; strict query/duplicate
   rejection. No owner/account/facility override. Existing Customer TLS/current
   auth before current Merchant admission; nonmember/suspended/revoked/missing
   permission -> indistinguishable404 BUSINESS_CONTEXT_UNAVAILABLE. Bad query400.
   Read never extends meaningful session activity.

Dashboard exact response:

```text
schemaVersion: BUSINESS_DASHBOARD_V1
context: { organizationId, displayName, membershipId, authorityVersion }
generatedAt: UTC instant
timeZone: Asia/Ho_Chi_Minh
period: { from, to, startInclusive, endExclusive, interval }
capabilities: [{ id, state, reasonCode }]
metrics: [{ id, unit, state, value, points }]
charts: [{ id, state, series }]
recentShipments: { state, items }
attention: { state, items }
quickActions: [{ id, state, reasonCode }]
```

State enum AVAILABLE / SOURCE_NOT_CONNECTED / NOT_SUPPORTED; omit an unauthorized
metric/chart/action entirely. Unavailable metric value=null and points=[]; an
available numeric value is an integer decimal string, never JS float currency.
Unavailable lists have items=[] AND state, not a false “no problems/no shipments”.
All source-unavailable chart series=[] (no demo/random/Personal data).

Metric IDs/unit: BOOKED_SHIPMENTS, AWAITING_HANDOVER, ACCEPTED_SHIPMENTS,
IN_TRANSIT, DELIVERED, DELIVERY_FAILED, RETURNED, ACTION_REQUIRED (SHIPMENT);
PARCELS (PARCEL); ACTUAL_WEIGHT_GRAMS (GRAM); CONFIRMED_FEES, OUTSTANDING_FEES,
COD_EXPECTED, COD_COLLECTED, COD_RECONCILED, COD_REMITTED (VND);
COST_PER_SHIPMENT (VND_PER_SHIPMENT); DELIVERY_SUCCESS_RATE, RETURN_RATE,
PICKUP_SUCCESS_RATE (PERCENT); BOOKING_TO_ACCEPTANCE_MS (MILLISECOND).
Finance metrics require dashboard.finance.read. No unverified debt/overdue/revenue.
Ratios/averages require declared valid denominator, never divide by zero or call
missing outcomes failed. No rates are AVAILABLE in this first source-limited slice.

Chart IDs: VOLUME_TREND, STATUS_DISTRIBUTION, SERVICE_BREAKDOWN, ROUTE_BREAKDOWN,
FEE_TREND (finance), FACILITY_BREAKDOWN (unavailable without facility source),
STAFF_BREAKDOWN (dashboard.staff.read), CHANNEL_BREAKDOWN (channel source absent).
Available series future elements must use cohort-bound dates/categories and exact
integer values. No available series schema is invented/activated in this slice.

Quick action IDs CREATE, IMPORT, LABELS, MANIFEST, PICKUP, ACTION_CENTER; all
NOT_SUPPORTED until actual Merchant contracts/authority exist. No links to a
Personal screen pretending to be Merchant, no enabled dead buttons.

## Source and UI acceptance boundaries

This slice can deliver real PostgreSQL organization access, revocation and a
Backend-connected Dashboard shell with honest capability states. It CANNOT yet
deliver live Merchant shipping/finance KPI: approved data currently only Personal.
No new Shipment table, copies, seed, source import or Merchant booking is approved
by this packet. Merchant Shipping Domain binding/pricing/payment authority is a
separate concrete dependency, not an excuse to reopen the Personal architecture.

Compact org greeting/date toolbar; source-based KPI cards; trend and status regions;
permission-filtered financial/warehouse/team/channel panels. Missing sources use
one concise source-state explanation and purposeful empty region, not repeated
warnings, fabricated zeros/sparklines or dozens of inactive cards. Deep links only
when a real permitted Merchant list/route exists; never pretend drill-through works.
ArgoPost primary logo/navy #0B2D5B/orange #FF7A00; keyboard, mobile, reduced motion.
Switcher loads actual contexts, allows Personal return, distinguishes selected org;
no static ACTIVE badge. Clear old org data immediately on context/session change;
ignore late responses after switch/revocation/logout. Network failure distinct from
no-membership/unsupported-source. No browser persistent private dashboard payload.

## Required evidence before DONE

PG18: upgrade from populated V22; unchanged old checksums/data; same-key provision
one effect/audit; rollback on audit failure; runtime no grants/DDL; org A/B isolation;
member revoke/permission revoke/org suspend/account disable; concurrent revoke/read;
fabricated org/stale context; branch-limited rejection; production/test mixed profile
denial. No real privileged account seed; synthetic isolated fixtures only.
HTTP:401 anonymous, strict query/role/tenant/scope denial, finance/team redaction,
no-store and existing TLS/Origin/CSRF preserved. Browser: actual two-org context,
switch/race/logout, keyboard/mobile, unavailable sources do not chart zeros, no
enabled out-of-scope actions. Typecheck/lint/build, affected tests, architecture,
source manifest/report/privacy gates. Do not claim full Business acceptance.

No commit/push/merge/worktree or real privilege activation in this checkpoint
without separate instruction identifying targets. Existing87 working paths retained.
