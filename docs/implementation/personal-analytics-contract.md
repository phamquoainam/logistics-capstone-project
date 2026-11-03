# Personal Analytics V1 — metric catalog, query contract and delivery gates

Status: **ACCEPTED exact read contract by Owner, 2026-11-03**. Authority: Owner
directive f22a7acf-edb1-4975-b402-0d46f6393498, 2026-11-03. Implements bounded
Personal portions of L028/REQ-012-04, REQ-AB-PERSONAL-DASH and BC143/147. It does
not accept all L028, open Carrier Lifecycle or grant Business authority.

## A. Observed baseline and preservation

- develop `7cdf9865bc17509b41b7e408b787f666906cb5f9`, PR39 and PR40 merged.
  Exact develop CI41499940590 SUCCESS; no open PR at audit time.
- Existing Dashboard reads only Customer Profile; its shipping placeholders are
  stale. No deployed Personal analytics aggregate or date-filtered query exists.
- Reuse CustomerAdmission/current-session checks, Customer client request lock,
  WorkspaceShell, existing Shipment detail, V20 Booking/Parcel/UNPAID snapshots
  and V21 committed Orders tracking events. Never use `/api/demo`.
- Banner removal preserved separately in commit `ea2d57a` and ignored SHA-256
  patch archive `.demo/analytics-checkpoint/`; no original data is discarded.

## B. Source and ownership design

No new table, migration, provider or chart dependency. Query paths below were reviewed and approved by Owner before runtime code.

`orders.api` exposes a reduced, self-owned read projection from
`orders.pilot_shipment`, `orders.pilot_parcel` and
`orders.pilot_tracking_event`; no contact/address/actor identity leaves it. One
SQL statement returns one Shipment row with independently aggregated Parcel
facts and its committed acceptance timestamp, avoiding join multiplication.
`finance.api` exposes a bounded owner-scoped batch read of immutable
`finance.pilot_shipping_obligation` for those IDs. Missing/mismatched amount,
quote, currency or owner is an integrity error, never an inferred zero.

The read service in the existing `notifications` module composes these public
APIs only; workflow HTTP derives the current CustomerAdmission context. Reads
join the existing physical transaction, recheck current admission before return
and never call meaningfulAction. No other-module internal import/cross-owner DML.

All source facts are read server-side. Date window max366 calendar days; each
current or comparison cohort max10000 Shipments (10001st row rejects the whole
query with ANALYTICS_RANGE_TOO_LARGE, never truncated metrics). Batch obligations
are read once per cohort, not N+1. No contacts, reference or address are needed
for analytics. Pagination20, bounded code search64, allowlisted sort. This bound
is a query safety limit, not a booking/business limit. No DB benchmark claimed.

Only existing explicit LOCAL/TEST + SANDBOX + non-production gates may enable
pilot analytics. Production/unknown mode denies with503 PILOT_DISABLED. No
Business endpoint or owner/organization/context query selector. Unknown or
duplicate query parameters reject400, including ownerId/organizationId.
No shared persistent or browser storage cache; existing session generation guard
rejects stale results on logout/account changes. Responses Cache-Control:no-store.

## Metric Catalog (before chart implementation)

Common definition: distinct self-owned CONFIRMED Booking cohort in requested
Booking period. Current status comes from committed CARRIER_ACCEPTED_AT_STATION
events, NOT the immutable Booking snapshot carrier_state. The captured projection
provides one authoritative cohort for KPI, dimensions and contributing rows.

| Metric ID | Business definition / aggregation | Unit | Previous / sparkline | Drill target | Availability |
|---|---|---|---|---|---|
| BOOKED_SHIPMENTS | count distinct Shipment.id | SHIPMENT | equal elapsed previous period / Booking buckets | shipments | DONE locally; hosted CI pending |
| AWAITING_HANDOVER | cohort Shipments without committed acceptance | SHIPMENT | previous Booking cohort at same read / Booking buckets | status (status=WAITING_FOR_HANDOVER) | DONE locally; hosted CI pending |
| ACCEPTED_SHIPMENTS | cohort Shipments with committed acceptance; not Delivered | SHIPMENT | previous Booking cohort at same read / Booking buckets | status (status=AT_STATION) | DONE locally; hosted CI pending |
| CONFIRMED_FEES | sum immutable Shipment.total_vnd once per Shipment | VND | previous cohort / Booking buckets | fees | DONE locally; hosted CI pending |
| UNPAID_OBLIGATIONS | sum finance UNPAID amount, bound to each cohort Shipment/quote | VND | previous cohort / Booking buckets | fees (obligation=UNPAID) | DONE locally; hosted CI pending |
| CREATED_PARCELS | count distinct Parcel.id in cohort; quantity is not Parcel count | PARCEL | previous cohort / Booking buckets | parcels | DONE locally; hosted CI pending |

All six metrics share date/status/service/route filters. Parcel weight bucket or
multi-parcel drill filters select contributing Shipments with EXISTS semantics;
Parcel metric still counts all Parcels in those Shipments and explains that unit.
Status percentages = status Shipment count / filtered distinct Shipment count;
service/route shares use the same denominator. Empty denominator => null/N/A.
Trend percentage =100*(current-previous)/previous; previous0 => null, never a
fabricated100% increase. When comparison=none, previousValue/changePercent=null.
All sums use exact integer/BigInteger arithmetic; VND is a decimal string in JSON.
Comparisons are volume/cost changes, not automatically good/bad performance.

Current-status metrics are explicitly **Booking-cohort status at read time**;
their series is attributed to Booking time, not acceptance-event volume. No
acceptance event-time trend is presented under a Booking-time caption. An
event-time operational trend is deferred until separately defined.

| Dimension | Source / grouping | Reconciliation / target | Availability |
|---|---|---|---|
| Status / operational position | committed Orders acceptance vs no event | two mutually exclusive Shipment groups; sum=BOOKED_SHIPMENTS; status drill | DONE locally; hosted CI pending |
| Booking volume | Shipment.created_at, day/week/month in selected zone | buckets sum=BOOKED_SHIPMENTS; bucket drill preserves global range | DONE locally; hosted CI pending |
| Services | approved immutable tariff version => ROAD_STANDARD; no inferred future services | observed source only; service drill | DONE locally; hosted CI pending |
| Route | quote snapshot datasetVersion/originCode/destinationCode | only existing approved SIM routes; no geographic map; route drill | DONE locally; hosted CI pending |
| Parcels | distinct IDs, multi-parcel Shipment count, actual weight buckets <=500g /501..5000g />5000g | weight distribution denominator Parcel count; drill lists contributing Shipments | DONE locally; hosted CI pending |
| Fees | confirmed and UNPAID per Booking bucket/route | overlapping measures, never a paid/unpaid pie or collected revenue | DONE locally; hosted CI pending |
| Delivered/return/exception/support/SLA/ETA/COD/revenue | no accepted authoritative source | no runtime chart or fake zero; dependency explanation | BLOCKED |
| Business metrics | org ownership, current membership/RBAC absent | definitions below only; no query/runtime workspace | BLOCKED |

Future Business catalog: organization Shipment volume/time/service/route/branch/
staff, actual financial obligations, and Delivery/SLA/COD/settlement only when
their owning domains and permissions exist. Each source must be organization-
owned; never sum Personal accounts of members. No org permission code invented.

## Exact proposed HTTP contract

`GET /api/v1/customer/personal-analytics` (Customer access bearer, existing TLS,
verified/eligible session and current admission). Read-only; no body.

Query allowlist (all optional except date pair must be supplied together):

| Parameter | Exact meaning / values |
|---|---|
| from, until | YYYY-MM-DD; calendar start inclusive/end exclusive; default30 days including today |
| compare | previous (default), none |
| bucket | day (default), week (Monday), month |
| status | WAITING_FOR_HANDOVER, AT_STATION; omitted=all |
| service | ROAD_STANDARD; omitted=all observed eligible services |
| origin, destination | approved SIM-A1/SIM-A2/SIM-B1; supplied together and one of four directed approved routes |
| day | optional YYYY-MM-DD start of a displayed Booking bucket within global range; point drill only |
| obligation | UNPAID; omitted=all |
| weight | UP_TO_500G, UP_TO_5KG, OVER_5KG; any matching Parcel selects Shipment |
| multi | true; any Shipment with >1 distinct Parcel |
| search | case-insensitive literal substring of Shipment code only; max64; no PII search |
| sort | NEWEST (default), OLDEST, FEE_DESC, FEE_ASC; tie-break id ascending |
| page | zero-based integer0..500; page size fixed20 |

Timezone fixed Asia/Ho_Chi_Minh (UTC+07); not client-controlled. Effective interval
is [from midnight, min(until midnight, server read time +1 microsecond)), expressed in UTC in
response. The microsecond exclusive successor includes facts stamped exactly at the read clock, matching PostgreSQL timestamp precision. Previous start/end shift by the same calendar-day span; a partial today
compares equal elapsed time. Bucket values include real zero days; a lack of
source/failed read is not a zero. Current status filters use status at read time
for both Booking cohorts. Selected day shifts by the same span for comparison.

One response contains overview, dimensions and selected contributing page from
the same captured cohort. New reads may observe newly committed facts; generatedAt
is the read freshness, not a durable historical snapshot or source version.
On page/filter navigation all visible metrics/list update together; stale requests
cannot overwrite a newer selection. No claim of immutable multi-request snapshot.

Response200 exact fields:

```text
schemaVersion: "PERSONAL_ANALYTICS_1"
context: "PERSONAL"
timeZone: "Asia/Ho_Chi_Minh"
generatedAt: UTC ISO instant
period: {startInclusive, endExclusive, previousStartInclusive:null|instant, previousEndExclusive:null|instant}
metrics: [{id, unit:SHIPMENT|PARCEL|VND, value:unsignedIntegerString,
           previousValue:null|unsignedIntegerString, changePercent:null|signedDecimalString,
           points:[{date:YYYY-MM-DD, value:unsignedIntegerString}]}]
status: [{key:WAITING_FOR_HANDOVER|AT_STATION, shipments:unsignedIntegerString}]
volume: [{date:YYYY-MM-DD, shipments:unsignedIntegerString, confirmedFeeVnd:unsignedIntegerString, unpaidVnd:unsignedIntegerString}]
services: [{code:ROAD_STANDARD, shipments:unsignedIntegerString, confirmedFeeVnd:unsignedIntegerString}]
routes: [{datasetVersion, origin, destination, shipments:unsignedIntegerString, confirmedFeeVnd:unsignedIntegerString}]
parcels: {count:unsignedIntegerString, multiParcelShipments:unsignedIntegerString,
          weight:[{key:UP_TO_500G|UP_TO_5KG|OVER_5KG, parcels:unsignedIntegerString}]}
shipments: {total:unsignedIntegerString, page:integer, size:20, hasNext:boolean,
            items:[{id:UUID, code, bookedAt:instant, status:WAITING_FOR_HANDOVER|AT_STATION,
                    service:ROAD_STANDARD, origin, destination, parcels:unsignedIntegerString,
                    weightGrams:unsignedIntegerString, confirmedFeeVnd:unsignedIntegerString,
                    unpaidVnd:unsignedIntegerString}]}
```

All objects additionalProperties:false. No contact, actor, assignment, custody,
proof, token or internal operational details. Monetary/count integer strings may
be zero; IDs opaque UUID; strings source-bound; max20 page rows,6 metrics,366
daily buckets,4 routes,1 current service and3 weight groups. Response bounded1MiB.
Errors reuse safe existing `{code,message,requestId}` envelope:400 VALIDATION_FAILED;401 existing
session denial;403 existing current-customer denial;422 ANALYTICS_RANGE_TOO_LARGE;
503 PILOT_DISABLED, ANALYTICS_UNAVAILABLE or existing SERVICE_UNAVAILABLE (no raw SQL/private diagnostic).
No Business fallback. Unknown selectors never change scope.

## C–F. UI and interaction contract

Replace the stale welcome/placeholder dashboard with compact header/date filters,
six KPI cards, responsive status/volume/route/service/parcel/fee cards and a source
Shipment table. Navy#0B2D5B, Orange#FF7A00, neutral surfaces. Native SVG/CSS charts,
no heavy dependencies. Values/context available on hover AND keyboard focus/tap;
visible textual summaries/tables, focus indicators and reduced motion.

`/personal` remains a compatible entry. Canonical routes inside Tổng quan:
`/personal/overview`, `/personal/overview/{shipments,status,services,routes,fees,parcels}`.
Filter/query string carries date/global filters, point/dimension selection, search,
sort/page. Click-through uses same tab and existing Shipment detail route. Back/
Forward restore URL; breadcrumb retains Dashboard's global filters. Clear filters
returns default30day range. No fake map, fleet, Delivery or Business card.
Do not add a replacement global Pilot banner. Printable labels remain unchanged.

## G. Task and acceptance gates

- [x] A — source/authority/baseline audit; preservation DONE.
- [ ] B — exact metric/read-contract Owner review; PROPOSED, not runtime-authorized yet.
- [ ] C — shared query service + six Overview metrics; real PG accuracy/zero/denominator tests.
- [ ] D — supported dimensions from same cohort; distinct Shipment/Parcel/money tests.
- [ ] E — URL drill-through/table/search/sort/page; chart-total reconciliation and current ownership denial.
- [ ] F — responsive screenshot review, keyboard/tap tooltips, loading/error/empty/stale-response tests.
- [ ] G — real HTTPS/PG18 browser, migrations unchanged, required CI exact SHA, scoped PR; no auto merge.

Security tests: A/B aggregate and source-list isolation; expired/revoked Customer;
ownerId/org selector/duplicate-param rejection; Business context denial; production
mode; no query extends meaningful session activity; no PII in analytics or artifacts.
Correctness tests: multiple Parcel/acceptance events never multiply Shipment/money,
date boundaries+partial previous period, status derived from committed events,
empty/current-only cohorts, exact VND beyond JS safe integer, integrity failure,
consistent sort/page and point/route/weight drill totals. Tests use synthetic data
only in disposable PG; original Windows PostgreSQL is read-only/unmodified.


## Observed implementation checkpoint

Local pure aggregation10/10 PASS, including prod/production overriding LOCAL;
real PostgreSQL18 Customer HTTP4/4 PASS (ownership, committed Station event,
unknown/duplicate/Business selectors, revocation, disabled mode). Frontend61/61
unit cases and47/47 UI cases PASS before final date/reset refinements; the final
focused Analytics suite and hosted exact-revision gates are recorded in the PR.
Architecture23/23 passed when the real notifications module was added; its
implemented-module inventory was expanded without changing dependency rules.
Default/customer frontend builds, typecheck/lint and new OpenAPI lint PASS.

The real HTTPS/PG18/test-SMTP browser receipt for the updated13-case journey
reached PASS/CLEANUP, including Analytics before/after actual Station acceptance
and private actual-file PDF checks. **The local Maven process has not completed**:
its subsequent database invariant checks are waiting on Docker InspectContainer
inside the unchanged Testcontainers ownership verifier. Windows C has0 free bytes;
temp-only work was redirected to an owned ignored directory on D. No ownership
checks were bypassed, no unrelated process stopped and no existing data/cache/
volume deleted. Treat this local run as partial evidence until its final exit and
cleanup are verified; hosted CI must independently cover the exact source.

Initial development failures were corrected specifically: one comparison fixture
was outside the clipped prior period; initial UI fixture omitted CSRF; old browser
assertions referenced the removed profile/welcome Dashboard; script output encoding
corrupted a few newly authored labels (corrected and UI regressions passed);
report-policy finite inventories needed4 added PG identities,10 unit identities,
source-order alignment and exact new totals. The complete130-case local policy
run had only3 remaining stale-inventory failures; both affected tests subsequently
PASS with corrected totals. Required CI reruns all130 checks on a fresh runner.
No false full-suite local PASS is claimed.

Observed existing PostgreSQL18.6: V21 successful,0 Shipments/Parcels/acceptances at
the read-only audit. No demo Shipments, synthetic PAID/Delivered, stations, routes
or customer data are inserted into that database. Local empty Analytics is the
correct state. Integration/visual fixtures are synthetic and isolated only.

## Owner UI refinement amendment — 2026-11-03

Authority: Owner directive `661f6fab-d341-4d74-b338-1016f35deb53`.
This presentation amendment supersedes the six-card main Dashboard layout in
section C–F only. Metric definitions, read DTO/query, ownership, comparison,
LOCAL/TEST guards and all domain/security contracts above remain unchanged.
Baseline: develop `f5e2c599e427818d97a825ede3104ec7fb12c521`, PR41/42 merged;
one working directory, no existing uncommitted source at checkpoint start.

- Main Overview: BOOKED_SHIPMENTS, AWAITING_HANDOVER, ACCEPTED_SHIPMENTS and
  UNPAID_OBLIGATIONS only; other metrics remain in existing deep-dive routes.
- Nonempty Overview: Booking volume, committed-event status distribution and
  at most five source rows from the same returned page. No extra query/engine.
  The preview is called recent only for page0/NEWEST without code search; other
  preserved URL sort/search/page states are labelled as a filtered preview.
- Empty cohort: four true-zero values and one concise empty state/CTA; no zero
  donut, flat sparkline, large blank chart or repeated empty tables. Errors and
  unavailable responses still show no metrics, never inferred zeroes.
- Xem phân tích and dimension/drill routes retain all six metrics, source
  search/sort/pagination, advanced filters and URL Back/Forward. Date presets
  7/30/90 and dd/MM/yyyy inputs translate to the same existing ISO query with
  exclusive `until` and fixed Vietnam timezone. No server parameter added.
- Null comparisons omit repeated trend copy; sparklines require multiple points
  and real nonzero values. No invented trend or financial result.
- Shared header uses normal document flow instead of a fixed overlay; sidebar
  remains independently scrollable. Keyboard focus, Escape-dismissed explanations,
  viewport-bounded tooltips, mobile tables and reduced-motion rules remain.
- Navy#0B2D5B/Orange#FF7A00, existing logo, compact type/spacing and white surfaces.
  No analytics cache, provider, Backend, migration or runtime seed.

Execution checklist for this amendment:
- [x] UX-01 DONE: audit, accepted presentation scope and clean baseline preserved.
- [x] UX-02 DONE: compact hierarchy, dates, disclosures, source preview and header.
- [x] UX-03 DONE:75 Customer UI regressions PASS, including9 empty/few/many
  synthetic visual cases at1440/768/390, keyboard/Escape/focus/viewport checks,
  date presets/custom dates, deep links and unavailable responses.
- [x] UX-04 LOCAL DONE: typecheck/lint,68 units, default/Customer builds and source
  validator PASS; final invalid-date negative check PASS. No Backend/API change.
  Hosted real API/HTTPS/PostgreSQL/browser and exact-revision CI remain required
  before review readiness; commit/PR and final hosted results are recorded on
  the refinement PR after push. No automatic merge.
