# Organization Shipment Management V1 [OWNER ACCEPTED]

Date: 2026-11-13. Owner requests Business Management with Organization ownership,
current Membership + RBAC + facility scope. Baseline develop 5172dee / merged PR46.
Owner accepted this exact API/data/permission amendment: “Duyệt Management V1 theo contract”.
BSC-06 pricing/coverage/UNPAID authority is retained, not submitted for reapproval.

## Delivery and boundaries

- [x] OSM-01 Inventory current domain/API/authority and preserve clean baseline.
- [x] OSM-02 Existing-contract UI: Management parent, nine categories, actual list/
  Draft navigation, eight detail tabs and scoped private labels; no fake status.
- [x] OSM-03 Owner accepts the exact read/export/internal-assignment contract below.
- [x] OSM-04 Full-cohort server filters/counts/pagination, source metadata, deep links.
- [x] OSM-05 Bounded authorized CSV/Excel and selected private PDF bundles.
- [x] OSM-06 Internal assignment/reassignment/history/receipts and concurrency.
- [x] OSM-07 PG18/security/upgrade/Browser/Frontend gates and traceability.

Observed gate results, evidence boundaries and deployment status:
[implementation checkpoint](organization-shipment-management-checkpoint.md).

Scope: existing LOCAL/TEST sandbox only, no new worktree. Never modify V1–V26,
reset/seed existing databases, activate a new Carrier or financial authority, or
turn member role names into permissions. New schema uses forward V27 after checking
the current migration stream. Source and disposable DB tests first; no localhost
migration or operator grants without an explicit deployment disposition.

The nine screen categories are ALL, DRAFT, WAITING, TRANSIT, ACTION, COMPLETED,
RETURNED, CANCELLED, SUPPORT. Only ALL/DRAFT/WAITING currently have Business data
sources. Booking is NOT_ACCEPTED/UNPAID. No invented transit/delivery/return/cancel/
Ticket/Carrier rejection counts or zero-success placeholders. Unavailable groups
show their specific dependency; Draft archive is not Shipment cancellation.

Warehouse/Pickup Point means an optional Merchant facility binding. It is not a
Carrier Station or evidence of pickup capability. Store/partner master data are
absent: show only existing sourceChannel/sourceOrderReference, not invented stores
or partner mappings. Batch means the persisted CSV/XLSX import job where applicable,
not a new Carrier handover batch. Internal assignee does not become a Courier.

## OSM-03a — management query / unchanged legacy API

Prefix `/api/v1/merchant-organizations/{organizationId}`. Keep existing GET
`/shipments` response/parameters unchanged when `view` is omitted. Add
`GET /shipments` with `view=management` and the following bounded query:

| Parameter | Meaning |
| --- | --- |
| page, size | page >=0; size=20; deterministic server pagination |
| search | literal substring 1–160: Shipment/Parcel code, reference, immutable source-order ref, recipient name/phone; normalize the supported 0/+84 phone representation; escape SQL LIKE metacharacters |
| from, until | optional date pair YYYY-MM-DD, Vietnam midnight start-inclusive/end-exclusive, maximum366 days, until no later than tomorrow; omitted = all Booking dates |
| status | omitted or WAITING_FOR_HANDOVER only while no Business Carrier lifecycle exists |
| service | omitted or ROAD_STANDARD |
| origin, destination | both omitted or one approved directed SIM route |
| obligation, handover | omitted or UNPAID / DROP_OFF respectively |
| facilityId, creatorId | optional exact UUID; AND with actual authorization scope |
| assignee | omitted, NONE or exact account UUID; internal assignment only |
| sourceChannel | optional literal 1–40, matching immutable preparation source |
| batchId | optional existing import-job UUID whose CREATED row produced this preparation |
| sort | NEWEST default, OLDEST, FEE_ASC, FEE_DESC; stable UUID tie-break |

Reject duplicate/unknown/malformed fields400; foreign/scoped-out resource/filter
404; disabled503. Never widen a facility grant through a filter, totals or export.
Identity/current Organization/Membership admission is reused for every request.

Response: `{schemaVersion:ORGANIZATION_MANAGEMENT_1,generatedAt,timeZone:
Asia/Ho_Chi_Minh,summary,total,page,size:20,hasNext,items}`. Counts and VND totals
are decimal integer strings. `summary` = `{bookedShipments,waitingForHandover,
unassigned,confirmedFeesVnd,unpaidFeesVnd}` over the same captured SQL cohort before
the status selection; total/page then apply the selected status. Label that scope.
No Draft/import-error count is summed into booked Shipments. One statement/captured
cohort prevents count/page disagreement or Parcel-join multiplication.

Item: `{record,status:WAITING_FOR_HANDOVER,source,assignment,allowedActions}`.
`allowedActions` is a server-derived subset of READ_LABEL, EXPORT, EXPORT_PII,
ASSIGN for that exact row/facility, never caller-supplied permission evidence.
`record` is the
unchanged BSC-06 Shipment/Parcel/Quote + independently verified Finance obligation.
`source` = `{sourceChannel,sourceOrderReference,facilityId,importJobIds:[UUID]}`
from immutable Booking preparation and actual import linkage. `assignment` =
`{assigneeAccountId:null|UUID,version:0|positive,updatedAt:null|instant}`.
No Personal data source, caller totals or client-only whole-organization filters.

## OSM-03b — bounded export and labels

Add explicit `shipment.export` and `shipment.export.pii` permissions, each supporting
ORGANIZATION or same-Organization FACILITY scope. Read permission is still required.
No automatic grants/backfill to current members. Existing read exposes BSC-06
contacts/fee facts; this amendment separately controls product export actions.

`GET /shipment-exports` accepts the management filter fields except view, plus
`ids` (optional <=20 distinct UUIDs from the selected page) and `includePii`
(false default). No page means this is a bounded cohort export, not a hidden page
truncation: <=200 records; larger cohort422 EXPORT_LIMIT_EXCEEDED, ask to narrow.
All selected IDs must be authorized and match filters, otherwise deny the whole
download; no partial silent export. Export uses NEWEST/other accepted sort.

Response JSON: `{schemaVersion:ORGANIZATION_EXPORT_1,exportId,organizationId,
generatedAt,timeZone,piiIncluded,rowCount,columns:[string],rows:[[string]]}`.
Only deterministic shipment/source/route/service/Parcel-count/fee/state/creator/
facility/import-job/internal-assignee fields. Recipient name/phone/full address
are omitted unless includePii=true AND every row has current export.pii scope.
No sender/recipient email, Identity credentials, private links or internal errors.
UI converts these exact strings to CSV or existing SheetJS XLSX, including the
bounded scope/Local-Test identity in independently saved files. Escape CSV formula/
DDE prefixes; XLSX cells are literal strings, no formulas/external links/macros.

Export writes an append-only access audit containing IDs, request/actor/Org/scope,
timestamp, filter digest, selected IDs/count and PII flag, never raw contacts/rows.
Audit failure denies output; no meaningful-action/session-idle extension for reads.
This is a synchronous read/access audit, no Booking, payment or command retry job.

Selected PDF bundles reuse existing GET private label per Shipment, max20 and32MiB
total. Check each current Org/facility/label permission server-side; abort all output
on a denial or scope/session change. Retain full original PDFs/continuation pages,
do not fabricate Parcel IDs or Carrier receipts. No new bulk Carrier mutation.

## OSM-03c — internal assignment, separate from Tracking

New explicit `shipment.assign` permission with Org/facility scope; existing read
required. No implicit Owner/Manager role grant. Targets must have current ACTIVE
membership in the same Organization, eligible verified Identity, and read access
to the Shipment's exact facility. A facility-scoped actor cannot assign outside it.
Historical revoked assignees remain auditable but do not gain access or appear as
eligible choices. Assignment never transfers Shipment ownership or Carrier custody.

Add routes:

| Route | Exact request / response |
| --- | --- |
| GET `/shipments/{id}/assignment-candidates` | page>=0,size20; only current eligible same-scope members. `{items:[{accountId,membershipId,displayName,role}],page,size:20,hasNext}`. Identity public admission supplies displayName only; no email/phone/security fields. No broad Identity table read by Orders/Merchant. |
| GET `/shipments/{id}/assignment` | `{shipmentId,organizationId,assigneeAccountId:null|UUID,version,updatedAt}`; virtual initial unassigned version0, no write on GET; ETag `"aN"` |
| PUT `/shipments/{id}/assignment` | `{assigneeAccountId:null|UUID,reasonCode:INITIAL_ASSIGNMENT|REASSIGNMENT|UNASSIGNMENT}`; UUID Idempotency-Key and If-Match `"aN"`; response same assignment with version+1, current timestamp and ETag. Stale409; no-op rejected409; invalid/currently revoked target404. |
| GET `/shipment-assignment-receipts/{key}` | actor + Org + facility + purpose bound `{targetId,version,outcome:COMMITTED}`; current permission required; unknown write resolution, no blind retry |
| GET `/shipments/{id}/internal-history` | page,size20; `{items:[{id,operation:ASSIGNED|REASSIGNED|UNASSIGNED,actorAccountId,priorAssigneeAccountId,assigneeAccountId,priorVersion,newVersion,occurredAt}],page,size:20,hasNext}`. No PII/free-text, filter/group never called Carrier Tracking. |

V27 adds four Orders-owned tables: organization_shipment_assignment (Shipment PK,
Org/facility/current assignee/version/time), organization_shipment_assignment_event
(append-only transitions/actor/request/time), organization_shipment_assignment_receipt
(Org/actor/purpose/key/HMAC/key ID/target/result version/time), and
organization_shipment_export_audit (bounded non-PII access evidence above).
Same-Org Shipment and membership composite FKs, monotonic version, immutable
owner/facility, unique command receipt, mandatory deferred event/receipt evidence.
No trigger or API may edit Quote/Parcel/Booking/Finance or Carrier tables.

Reuse Identity subject locking/public subject projection, current Merchant scope
lock and one physical transaction. Discover referenced target/candidate IDs through
a Merchant public API; lock those Identity subjects before final Merchant admission.
Recheck current membership/read scope at final command decision. Concurrent revoke,
reassign/same-key retry have one authorized, versioned outcome; audit/receipt failure
rolls back all changes. No runtime grant or role management endpoint is introduced.

## Acceptance / dependencies

Actual PG18 fixtures must prove Org/facility isolation on list/detail/count/search/
label/export/directory/history, revocation/Production denial, fee/cohort integrity,
date boundaries, Parcel/source/batch search, export PII and formula safety, stale/
concurrent assignment, actor-bound receipts and atomic rollback. Populated V26→V27
upgrade preserves V1–V26 data/checksums. UI/Browser cover filters, back/refresh,
eight tabs, nine categories, selected labels/exports and current-authority changes.

Carrier Lifecycle, handover manifests/acceptance/rejection, Pickup scheduling,
cancel/change requests, Organization Support/Claims, COD/Payment and missing
Store/Partner/Warehouse masters remain explicit dependencies. Their controls must
not claim successful workflow merely to make this screen look complete.
