# P0 Dashboard metrics, resource scope and supporting readers

Status: **PROPOSED — new wire/source changes await the consolidated approval**.
Existing six Admin GET contracts and source/test evidence remain accepted. This
document does not turn an absent source into an empty business dataset.

## MET-P0-01: exact compatible negotiation

Keep all six paths under `/api/v1/admin/dashboard` unchanged. Default requests
continue returning the exact legacy JSON field set. The current frontend parser
rejects unknown fields, so adding metadata unconditionally would break it.

Proposed opt-in header:
`Accept: application/vnd.argopost.admin-dashboard.v2+json`. Response Content-Type
matches that media type; `Vary: Accept`, `Cache-Control: no-store`. Unsupported
versions return406 with `NOT_ACCEPTABLE`; no fallback to a partly parsed response.
The v2 frontend validates the exact version and shape. All endpoints still enforce
current Backend authority; negotiation never changes a permission.

V2 Dashboard has exactly the existing `query`, `generatedAt`, `snapshotId`,
`sections`, plus `schemaVersion` = `ADMIN_DASHBOARD_2`, `resourceScopeVersion` and
`metricMetadata`. Legacy Query and individual Metric fields stay unchanged; new
Merchant metrics use the v2 finite registry only. Legacy v1 Merchant section stays
unavailable until its source/mapping can be supplied without changing v1 semantics.
Do not put unrecognized new keys into the v1 section or old frontend enum.

Each `metricMetadata` entry is:

| Field | Exact meaning |
|---|---|
| metricKey | Finite metric ID, unique in response |
| definitionVersion | Registry version `ARGO_ADMIN_METRICS_2`, not a timestamp |
| cohortKind | BOOKED_SHIPMENT or FIRST_SUBMITTED_APPLICATION |
| timeZone | Asia/Ho_Chi_Minh |
| numerator | Exact nonnegative integer string for the defined population/count/sum |
| denominator | Exact integer string for a real rate or duration average sample count; null for count/sum/min/max |
| unit | SHIPMENT/PARCEL/APPLICATION/VND/MILLISECOND/RATIO |
| sampleCount | Integer string for duration/rate samples, null otherwise |
| lastSourceEventAt | Max persisted relevant source-event timestamp in this authorized cohort, nullable for legitimate empty source |
| sourceCheckedAt | Consistent transaction read time; never renamed lastSourceEventAt |
| freshness | CURRENT_TRANSACTION, UNAVAILABLE or DEGRADED; no invented lag SLA |
| availabilityReason | Finite code or null; no raw diagnostic/PII |

This is wire metadata and a code-owned definition registry; no MetricDefinition
table is necessary. Definitions are owner-reviewed functions, not arbitrary SQL
or report formulas submitted by the browser. A future materialized/remote source
needs an explicit observedAt/lag policy before claiming CURRENT_TRANSACTION.

## MET-P0-02: definitions and cohorts

| Metric | Cohort / numerator / denominator |
|---|---|
| BOOKINGS | Distinct confirmed Shipment IDs booked in Vietnam date range; no denominator |
| PARCELS | Distinct Parcel IDs belonging to exactly those Shipments; no denominator |
| WAITING_HANDOVER / ACCEPTED | Distinct same-cohort Shipments at legal captured state; accepted counted once despite multiple intake events |
| CONFIRMED_FEES / UNPAID_FEES | Exact VND sum over the same distinct Shipment IDs; no Parcel/event join multiplication, no paid/revenue/overdue inference |
| APPLICATIONS_SUBMITTED / UNDER_REVIEW / NEEDS_INFORMATION / APPROVED / REJECTED | Distinct application IDs whose **firstSubmittedAt** is in the date range; state at capture; all revisions of one application count once |
| APPLICATIONS_WITHDRAWN | Same submitted cohort only; a never-submitted withdrawn Draft is excluded |
| APPLICATION_PROCESSING_TOTAL_MS / MIN_MS / MAX_MS | firstSubmittedAt to final committed APPROVED/REJECTED event; sampleCount is number of those terminal decisions, not all open cases |
| APPLICATION_PROCESSING_AVG_MS | duration total / sampleCount; absent when no samples, never display zero elapsed time as measured performance |

The v2 application list accepts the corresponding finite APPLICATIONS_* metric
and returns existing safe application item fields plus v2 metadata; never names,
tax IDs, email or documents through Dashboard aggregate permission. A revision
resubmission is a real event and may have its own future event-based series; it
does not increase the submitted-application count. New/Resubmitted labels are not
extra state-machine states. No SLA completion ratio is inferred from processing
duration or the fact that an Admin clicked Approve.

Tabs use the relevant cohort labels: Shipment filters mean Booking time; Merchant
Queue means first submission time. A status filter from one domain cannot silently
filter another. The v2 query explicitly adds optional `applicationState`, applicable
only to Merchant section/list, while preserving existing shipmentStatus semantics;
cross-domain unsupported combinations reject400. The new field is not sent to v1.

Calendar buckets are Vietnam-local DAY, Monday-start WEEK or calendar MONTH,
clipped at request boundaries. Source series must partition the interval without
duplicates/gaps and reconcile exactly to metric totals. No event-time chart may
be labeled Booking-time or vice versa. No broadening to Production or official
network, delivery, SLA, COD/reconciliation or telemetry sources.

## SCOPE-P0-01: authoritative source scope

Current Shipment/Finance reads retain REALM_BOUND_LOCAL scope only, with the same
finite six READ grants and admission exclusions. It is not called a region/site
scope. Nonempty regionId still rejects; missing official operating geography is
not solved with SIM codes.

New Merchant governance uses current persisted `business_reviewer_scope` POLICY
or ORGANIZATION assignments. Application aggregate/list/detail scope must be the
same intersection of Admin MERCHANT READ, domain application.read, published
policy and current assignment; KYC disclosure additionally needs evidence.read.
No assignment returns POLICY_NOT_CONFIGURED with no metrics, not a count of0 and
not all applications. Scope revoked before a read denies; new scope filter values
never expand authority. Explicit policyId is a narrowing filter, not a grant.

Derive `resourceScopeVersion` from actor/realm/current assignment versions,
permissions, effective policy versions and environment. Both v1 and v2 snapshot
selectors must include the applicable effective scope fingerprint and definition
version. No new cache/table is needed. A changed grant, scope, policy, contributing
row or source fact makes old selectors409; do not silently return a newly filtered
list under an old count. Population-lock-before-RR semantics must include offline
scope changes through the Identity→Merchant lock order.

## READ-P0-01: Shipment Oversight minimum

Existing canonical row/page stays P0-ready. Proposed detail is a masked,
snapshot-bound reader via Orders' public port, not Customer endpoint enumeration.
Return real Shipment IDs/status/owner kind and Parcel IDs/codes/measurements,
bounded existing tracking/intake evidence. Validate the Shipment contributes to
the original snapshot/metric/query and current scope. No recipient/sender PII,
private Label/POD bytes, Carrier event editor or arbitrary status writer.

Absent later lifecycle facts remain unavailable. No per-region/site filter or
investigation command without an owning source/authority. Exact API/DTO appears
as P0-READ-01 in the manifest; no schema change is needed for these reads.

## READ-P0-02: IAM is separate current authority

Reusable accepted readers are `IdentityAdministrationCommands` and
`ProductionIdentityReads`: GET `/api/v1/admin/users`, `/users/{accountId}`, `/roles`,
`/scopes`, plus current `/api/v1/auth/me`. They enforce exact IDENTITY_MANAGEMENT
realm scope/keyset visibility; the roles catalog contains IDENTITY_MANAGEMENT,
not a general Carrier Staff directory. Admin READ is denied these endpoints.

Do not label that limited catalog as all Staff, expose all Customer accounts or
add IAM grants to Admin. No new IAM path/table/permission or management mutation
is proposed for P0. A future Root-only reader UI may reuse the exact accepted
contract in its own context; general Staff governance requires a separate typed
source design. P0 does not declare ADM-12 complete.

## READ-P0-03: Audit Explorer and Work Inbox minimum

Audit v1 reads only committed Merchant application/Organization governance
events, in assigned scope, through the Merchant owner. Public fields are IDs,
operation, versions, policy, timestamp/correlation and finite reason code; no
protected comments, raw evidence, passwords, proof, tax/contact or other-domain
audit SQL. `audit.read` is distinct from application/evidence permission. No export,
PII reveal or audit edit is opened. A source missing before V42 is unavailable;
an existing authorized source with no rows is a legitimate empty list.

Work v1 derives current work from persisted submitted revision, assignment and
READY_FOR_DECISION event. Stable work IDs bind application/revision/assignment
version; current eligibility filters are applied **before pagination**. Reviewer
work and independent-decider work are distinct. `dueAt=null` until a deadline
policy exists; no fabricated overdue, priority or ApprovalTask row. Actions call
the same reviewed Merchant commands and permissions, not a parallel task engine.
Acknowledge/mark-complete is not a side channel to approve an application.

Dashboard Decisions can use this exact source after its contract and runtime are
implemented. Until then, the existing unavailable panel remains truthful.

## Required acceptance after approval

V1 exact-field parsing and v2 negotiation; calendar boundaries; denominator0/no
samples; application revision dedup; first-submitted cohort vs Booking cohort;
current scope/revocation/policy change; hash-bound same-cohort list; no money-type
mixing; no KYC leak from aggregate grant; no privileged data for technical-only
principal; Work eligibility before LIMIT; no audit/assignment/task write from READ;
masked detail cross-source consistency and current source-backed Browser tests.
No Source/Test READY claim follows from this document alone.
