# Merchant Operations Reports & Finance V1 [OWNER ACCEPTED]

Owner accepted: "Duyệt Reports & Finance V1 theo contract".
Date: 2026-11-15. Owner-directed follow-up to Organization Management V1.
Dependencies: accepted BSC-06 and OSM-01–07 working source, including V26/V27;
these remain uncommitted on `feature/argopost-organization-management`, not assumed
merged or deployed. All39 pre-existing changed source paths were preserved with
SHA-256 and copies before this task. No new worktree or database reset.

## Delivery tasks

- [x] MRF-01 Preserve source and inventory actual data/authority.
- [x] MRF-02 Owner accepts exact report/Finance read, export/history and permission amendment below.
- [x] MRF-03 Existing-contract UI: two Management children, scoped Shipment fee snapshots,
  useful source/dependency explanations and real drill-through; no page-only aggregate claim.
- [x] MRF-04 Full-cohort server Operations report, internal creation/Booking activity,
  exact period/dimension grouping and source-reconciled charts.
- [x] MRF-05 Permission-isolated Finance report, fee statement and immutable contract comparison.
- [x] MRF-06 Bounded report export, immutable access evidence and own export history.
- [x] MRF-07 Focused PG18/security/Browser/FE tests, upgrade/backward compatibility,
  required checkpoint gates, traceability and accurate remaining dependencies.

V1 is LOCAL/TEST + Customer SANDBOX only; default disabled, deny prod/production
profiles and disallowed Organization provenance, retain Identity/CSRF/TLS/current
Membership locking. Never modify V1–V27, grant permissions automatically, migrate
localhost or activate COD/collection/settlement/Carrier authority. Source and
disposable test DB first. This does not authorize deployment into the demo DB.

Subsequent Owner authorization, 2026-11-15: "Cho phép kích hoạt đúng phạm vi trên".
This specifically permits deploying the verified build and granting Organization
scope `shipment.reports.read` and `shipment.finance.read` through the audited
operator to Customer `5d206ef3-350e-42b7-af8a-4c54b8d43fe2` in NamShop Demo
`782c4766-8bdd-435b-9961-512c831922d6` on localhost. V28 was already present when
read-only preflight ran; no additional migration is authorized or needed for this
activation. Existing permissions remain intact. No other account, COD, collection,
payout, Carrier or Production authority is included.

## Data and metric semantics

Booking cohort: confirmed BUSINESS Shipments in the admitted Organization and
current authorized facilities, filtered by Booking time (Vietnam timezone), service,
route, facility, creator, source channel, actual import batch and internal assignee.
No Personal source or member-Personal sum. One Shipment counted once, Parcel facts
summed once; immutable Booking Quote supplies chargeable weight. Finance independently
validates every captured Shipment/Quote/facility/amount binding before exposing totals.

Counts, grams and VND totals are nonnegative decimal integer strings, not JS floats.
Average fee is `{totalVnd,shipmentCount}`; UI rounds display to nearest VND, HALF_UP,
and retains numerator/denominator. Empty supported cohort is real zero; absent source
or missing permission is explicit availability/null, never fabricated zero.

Internal preparation and activity have separate grains: current DRAFT/BOOKED/ARCHIVED
state of preparations created during the period; CREATE and BOOK audit events by
their actual actor/time and facility from the recorded version. They are not added
to booked Shipment totals. Route/service/status/assignee filters cannot apply to an
unbooked preparation: internal section then returns NOT_COMPARABLE with that reason,
instead of silently ignoring filters. Existing code has no actual PACKED transition,
print confirmation or Carrier handover event: those productivity metrics stay absent.

Facility is a Merchant warehouse binding, not a Carrier Station or pickup capability.
Source channel is a recorded code, not an invented Store/OMS master. Creator is a
recorded account UUID, not a Courier. Default labels may shorten UUIDs with full
accessible text; no broad Identity contact directory query.

Supported financial facts: Quote consumed at Booking, initial confirmed fee snapshot,
UNPAID obligation, service/route/facility/contract ID+version/tariff, Parcel actual and
chargeable grams, per-Parcel fee lines. Quote-to-initial-obligation difference is a
comparison of these two immutable sources, not a Carrier reweigh/adjustment workflow.
UNPAID is not automatically overdue, commercial credit debt or revenue.

COD requested/collected/reconciled/paid-out, payment history, credit limit/due date,
fee adjustment/refund, commercial statements/invoices, Finance dispute workflow,
delivery/return/SLA and pickup outcomes have no accepted source. Return explicit
SOURCE_NOT_CONNECTED / NOT_SUPPORTED availability and no amount/rate/false receipt.
No action lets Merchant write a ledger, collect money, approve a statement or payout.

## Proposed current permissions

Add explicit `shipment.reports.read` and `shipment.finance.read`, each ORGANIZATION
or same-Organization FACILITY scope. Existing `shipment.draft.read` remains required.
Role names do not confer either right; no backfill or runtime grant API.

Operations uses the intersection of draft.read and reports.read. Financial columns
inside Operations are visible only when finance.read covers the complete selected
cohort/scope; otherwise financial state FORBIDDEN and every monetary value null.
Finance uses the intersection of draft.read and finance.read, independently of
reports.read; a warehouse grant never becomes organization-wide authority.
Empty-cohort financial visibility still requires a real permitted scope, not vacuous
`every([])`. Server-derived capabilities control UI; client permissions are hints.

Exports additionally require existing `shipment.export` over every contributing
Shipment and selected scope. New reports contain no sender/recipient contacts;
`shipment.export.pii` does not make contacts appear in aggregate reports.
Old OSM/BSC wire and permission meanings are unchanged.

## Exact read/query contract [ACCEPTED]

Prefix `/api/v1/merchant-organizations/{organizationId}`. Every response no-store.

1. GET `/operations-reports`.
2. GET `/shipping-finance`.

Both accept strict unique query fields: `from`, `until` (ISO date pair, Vietnam
start-inclusive/end-exclusive, <=366 days, until<=tomorrow), `interval`
DAY/WEEK/MONTH/QUARTER/YEAR (default DAY), `service` ROAD_STANDARD, paired supported
`origin`/`destination`, `facilityId`, `creatorId`, `assignee` NONE/UUID,
`sourceChannel` (literal<=40), `batchId`, `status` WAITING_FOR_HANDOVER,
`obligation` UNPAID, `handover` DROP_OFF. Finance also accepts `page`>=0,size20,
literal `search`<=160 and sort NEWEST/OLDEST/FEE_ASC/FEE_DESC using OSM semantics.
Default period is last30 Vietnam dates including today. Reject unknown/duplicate/
malformed400, foreign resource or missing scope404, disabled503; no partial success
if a mandatory source validation fails. Bound report capture at10000 Shipments and
each dimension at1000 groups; overflow422 REPORT_LIMIT_EXCEEDED, never truncate.

Operations response `ORGANIZATION_OPERATIONS_1`:
`{schemaVersion,organizationId,generatedAt,timeZone,period,financialState,summary,
trend,breakdowns,preparations,activity,capabilities}`.

`period={from,until,interval,startInclusive,endExclusive}`.
`summary={shipments,parcels,actualGrams,chargeableGrams,waitingForHandover,
unassigned,confirmedFeesVnd,unpaidFeesVnd,averageFee}`.
`financialState=AVAILABLE|FORBIDDEN`; money/averageFee null when forbidden.
`trend` array of `{from,until,partial,shipments,parcels,actualGrams,chargeableGrams,
confirmedFeesVnd,unpaidFeesVnd}` clipped to the requested period, including zero
Booking buckets. Calendar weeks start Monday; quarter/year boundaries are real.
`breakdowns={facility,creator,channel,service,route}`, each row
`{key,label,shipments,parcels,actualGrams,chargeableGrams,confirmedFeesVnd,unpaidFeesVnd}`.
Null facility/channel has an explicit unbound key; no fictitious warehouse/store.
`preparations={state,reasonCode,total,draft,booked,archived}`; counts null when not
comparable. `activity={state,reasonCode,created,booked,byActor}`; byActor contains
`{accountId,created,booked}`, actual audit events only, null when not comparable.
`capabilities=[{id,state,reasonCode}]` names unsupported source categories above.

Finance response `ORGANIZATION_FINANCE_1`:
`{schemaVersion,organizationId,generatedAt,timeZone,period,summary,trend,breakdowns,
total,page,size:20,hasNext,items,capabilities}`.
Summary `{shipments,confirmedFeesVnd,unpaidFeesVnd,quoteFeesVnd,differenceVnd,averageFee}`.
Items `{shipmentId,code,createdAt,facilityId,creatorAccountId,sourceChannel,
sourceOrderReference,service,origin,destination,contractId,contractVersion,
tariffVersion,quoteId,quoteVnd,confirmedVnd,differenceVnd,paymentState:UNPAID,
actualGrams,chargeableGrams,feeLines}`; lines reuse accepted Quote line fields.
No recipient/sender PII. Trend and dimension rows reuse Operations monetary shapes.
Summary/trend/breakdowns use the complete filtered cohort, never the current page.

## Drill-through, exports and history [ACCEPTED]

UI routes `/merchant/{org}/reports` and `/merchant/{org}/finance` are Management
children. Charts use the exact report cohort/date/dimension. A chart click opens
existing Management filters; group period endpoints are carried intact. Unsupported
metrics have no fake drill target. Null-facility/channel and internal audit groups
must not broaden a filter: use a bounded contribution list within report UI when
the old Management query cannot represent the exact selection. Distinct own-Org
detail links retain a safe same-Org report return context.

GET `/shipping-report-exports`: same filters, plus `kind=OPERATIONS|FINANCE` and
`section=trend|facility|creator|channel|service|route`; no page/size/search/sort for
aggregate exports. Output `{schemaVersion:ORGANIZATION_REPORT_EXPORT_1,exportId,
organizationId,generatedAt,timeZone,kind,section,rowCount,columns,rows}`. Row cap1000;
literal string cells; no PII columns, formulas, macros or external links. CSV escapes
formula/DDE prefixes; XLSX uses existing library and literal strings. Files identify
LOCAL/TEST simulation and are not commercial statements or invoices.

V28 adds only `orders.organization_report_export_audit`: id, organization_id,
actor_account_id, request_id, kind, section, canonical_query JSONB (only allowed
non-contact report filters), query_digest32 bytes, contributing_shipment_ids UUID[]
<=10000, row_count<=1000, occurred_at. Same-Org membership binding, append-only,
runtime SELECT/INSERT only; audit failure rolls back/denies output. No monetary
snapshot, raw contact/search, token, generated file or credentials stored in audit.
V28 also extends explicit grant allowlists and preserves old operator signature,
receipt semantics and all existing grants. No source or financial table rewrite.

GET `/shipping-report-history` accepts `page` and `size=20`: own actor's export records only,
current Org/Membership/report-or-Finance/export permissions rechecked against all
contributing Shipments before disclosure. Response `{items:[{exportId,kind,section,
query,rowCount,occurredAt}],page,size:20,hasNext}`. Unauthorized old entries are not
exposed. This is export/query history, not archived financial evidence; reopening
filters recomputes current data, clearly labeled. No fake historical download URL.

Scheduled delivery is NOT_SUPPORTED in V1: no recipient/channel consent, frequency,
revocation-at-send, retention or failure/retry policy is approved. UI may explain
this dependency but must not show a successful schedule, enable SMTP or email staff
from inferred membership. A later controlled amendment can activate it; no new
provider or actual email send is authorized by this read-only checkpoint.

## Acceptance

Test real PG18: full-cohort vs page, multi-Parcel no double count, route/date/timezone
boundaries (week/quarter/year), empty/huge integer totals, internal versus Booking
cohorts, partial period, Org/facility/finance isolation, zero-cohort rights,
revocation and Production denial, export limits/audit rollback/own history and
negative selectors. Populated V27→V28 upgrade preserves data/checksums/grants;
V1–V27 immutable. FE unit/UI and actual HTTPS/PG/test-SMTP Browser verify filters,
charts/drill-back, scope switch, Finance denial, files and no fake COD/payment/receipt.
