# Tổng quan — Owner completion checkpoint, 2026-11-09

Scope: presentation and drill-through using the ACCEPTED PERSONAL_ANALYTICS_1
read contract. No new DTO, API, migration, authority or Carrier/Finance workflow.
Latest Owner direction supersedes the previous four-card-only presentation;
retain four priority KPIs and add compact Parcel/fee summaries from the same read.

- [x] OV-01 Fix navigation context, removable selections, point drill, filters,
  Today preset and invalid custom range recovery; regression tests.
- [x] OV-02 Parcel/financial summaries, supported status distribution, recent
  Booking rows and direct private document-tab navigation; quick actions.
- [x] OV-03 Explicit unavailable Delivery/Return/Exception/Payment/COD/Support
  and notification sources; never infer zero, paid, success or customer actions.
- [x] OV-04 Typecheck/lint/unit/UI/build and synthetic visual verification;
  record observed evidence and limitations in traceability.

Overview → Breakdown → contributing Shipments → existing private detail/label.
Drills retain the selected cohort. Active selections are individually removable.
Breadcrumb restores the source Overview when reached through in-app links;
external deep links fall back to the global date/status/service/route filters.
Return context is session navigation state, never sent to the Backend or stored
persistently. Shipment detail still performs its existing authorization read.

Unavailable metrics are a compact capability explanation, not a warning banner
and not an assertion that there are no issues or no outstanding payments.
No Full Personal/Carrier/Finance acceptance is implied.


## Requirement coverage

| Owner request | Current delivery / prerequisite |
|---|---|
| Shipment KPIs | Confirmed Bookings, waiting for handover and committed Station acceptance; no invented pickup count |
| Parcel statistics | Total Parcels and multi-Parcel Shipments from the same cohort; product quantity is not Parcel count |
| Finance | Confirmed shipping fees and authoritative UNPAID, exact VND strings; not collected revenue or duplicated sums |
| Trends | Existing day/week/month Booking buckets, comparison and accessible point drill; no inferred delivery performance |
| Status distribution | Two supported committed states; KPI/dimension consistency validated before rendering |
| Recent shipments | Five rows by Booking date in selected period; non-default sort/search/page explicitly labelled as filtered preview |
| Drill-through | Source Overview retained in router state, visible/removable filters, exact report return from detail/documents, page-zero recovery |
| Quick actions | Create, existing lookup, action-required dependency screen and per-Shipment private document tab |
| Dates | Today/7/30/90/custom; Vietnam timezone, max366 days, no future end date |
| In Transit/Delivered/Failed/Return, Parcel exceptions | BLOCKED by absent Carrier Lifecycle/Exception source; never shown as zero |
| Paid/COD | BLOCKED by collection/COD authority and source; UNPAID unchanged, no payment control |
| Customer issues/important notifications | BLOCKED by missing Support/notification/fee-change read contracts; no fake events/counts |

## Observed verification — 2026-11-09

- `npm.cmd run typecheck`, `npm.cmd run lint`: PASS.
- `npm.cmd run test`:69/69 PASS; includes contradictory status/fee payload rejection.
- `npm.cmd run test:customer-ui`:87/87 PASS, including28 Analytics cases.
- `npm.cmd run build` and Customer build (UI suite prerequisite): PASS.
- Synthetic0/1/75-row screenshots at1440/768/390px reviewed; no page overflow.
- Contract validator530 PASS; implementation source validator errors[]/CONSISTENT.
- Existing aggregation tests10/10 PASS and four real PG18 Analytics HTTP tests
  PASS (ownership, committed event/status, revoked/foreign selectors and environment denial).
- Actual HTTPS + disposable PG18 + test SMTP browser13/13 PASS, parent Maven
  browser test PASS, receipt PASS/CLEANUP/NONE, revision LOCAL_UNPUBLISHED.
  This is working-source evidence, not hosted CI for a new commit or live Gmail delivery.
- Existing local launcher: PostgreSQL18.6 healthy, Flyway startup migration PASS,
  Backend/Frontend HTTPS readiness PASS. Existing database/keys/certificates kept.

Initial failures retained accurately: first integration invocation lacked the
mandatory local execution identity and failed closed before starting its fixture.
The next run passed aggregation/PG assertions but browser stopped on the old
`Danh sách vận đơn` locator after the UI changed it to `Quay lại báo cáo`.
The test now checks report return explicitly, then opens Management for its
unchanged assertions; final browser rerun passes all13 cases. An initial UI test
had an incorrect synthetic row-presence assumption; corrected fixture/ETag and
all final87 cases passed. No tests skipped, retries added or assertions weakened.

Original working directory only; feature branch `feature/argopost-overview-completion`
from develop59648b4. No commit/push/merge in this implementation checkpoint.
Required hosted CI remains a publication gate; full Carrier/Finance/Personal
acceptance is not claimed. Migrations and Backend source unchanged.


## Owner chart/UX refinement follow-up — 2026-11-09

- [x] OV-05 Clarify Overview as quick summary and Shipping Report as detailed
  analysis of the same cohort. Remove decorative link arrows. Build shared
  responsive SVG line/area and column plots, readable axes/units, accessible
  exact-value point navigation, supported status donut and Parcel weight bars.
- [x] OV-06 Regression/unit/visual/build verification and actual browser gate
  for changed chart interactions, retaining all existing source/security gates.

Planned files: PersonalAnalytics.tsx + analytics.css, reusable AnalyticsCharts.tsx
and chart-math.ts, focused UI/unit tests and traceability. No new dependencies,
API fields, schema or business authority. Current metric.points are current-period
series only; previousValue is an aggregate. Never fabricate a previous-period line
from that aggregate. Fees/UNPAID overlap: no paid/unpaid stacked chart. Delivery,
COD, routes outside the accepted dataset and Support remain source-bound.


### Chart follow-up verification and UX rules

LOCAL DONE for the supported read-only presentation. Overview answers "what is
happening in this period?" with four priority KPIs, compact Parcel/fee summaries,
four charts and five recent rows. Shipping Report answers "which group/time/
Shipment contributes?" using the same captured cohort, six metrics, dimensions
and paginated source records. It is not a second calculation engine or a second
shipment-management workflow. No duplicated data, fabricated delivery events or
new paid/collection controls.

- Navy line with restrained area fill: Booking volume. Orange columns: confirmed
  fees. Status ring: supported committed states. Horizontal bars: actual Parcel
  weight groups. Each view states its measure/time basis, exact unit and drill.
- Previous-period values remain aggregate text; no invented previous-series line.
  Confirmed and UNPAID fees overlap, so never stack them as separate cash flows.
- Integer-first BigInt axes/geometry, readable labels and exact hover/focus values
  preserve money beyond JavaScript safe integer precision. Plot positions and
  date tick labels align. No synthetic curve extrema or invented data points.
- Decorative navigation/trend arrows removed; directed-route arrows remain
  semantic direction indicators. Current navigation filters/return state retained.
- Desktop two-column chart grid; tablet/mobile readable single-column layout.
  Keyboard links, live exact-value context, bounded tooltips, reduced motion,
  honest empty/error/loading behavior and private Shipment/label access retained.

Observed:73 unit tests,92 full UI tests, typecheck/lint/default+Customer builds
PASS. Final tablet CSS adjustment separately rechecked33 Analytics cases PASS.
Synthetic0/1/75-row desktop/tablet/mobile screenshots viewed. Actual HTTPS/PG18/
test-SMTP browser13/13 PASS with parent Maven exit0 and PASS/CLEANUP/NONE receipt,
LOCAL_UNPUBLISHED; final tablet-only CSS checked by UI tests, not an additional
full-stack run. No claim of hosted exact-commit CI or new live Gmail verification.
Contract validator530 PASS. Source validator initially rejected three undeclared
new files; added their exact paths to the existing implementation manifest,
retaining all guards. Final errors[]/CONSISTENT PASS.

Visual QA found the shell icon-size selector shrinking the new plot SVG; corrected
chart-specific selector and added actual rendered plot/path dimension assertions.
No test retries, skips or assertion weakening. Existing WIP/checkpoints retained;
no Backend source, migration, application data, secret, provider or authority change.


## Owner simplification amendment — 2026-11-09 (latest presentation authority)

Supersedes the separate Shipping Report/dimension dashboard presentation above.
Retain accepted backend cohort/read contract, URLs as compatible entry points,
authorization, precision, bounded queries and domain invariants.

- [x] OV-07 One Overview, four priority KPI values, one switchable volume/fee
  chart, status distribution, Parcel breakdown and recent rows. Remove duplicated
  summaries, decorative sparklines, quick-link toolbar and report/dimension tabs.
  KPI/point/group opens one canonical list-only drill; old dimension URLs also
  render source results only. No repeated KPI/chart dashboard in the drill.
- [x] OV-08 Immediate presets/selects; one explicit custom-date confirmation.
  Global date/status/route context visible; list search/sort/pagination and
  removable drill chips preserve source context. Zero KPI/status groups cannot
  expand an incompatible current cohort. Verify UI, source validator and real
  HTTPS/PG18 browser. Preserve every existing uncommitted source/checkpoint.

Planned source: PersonalAnalytics.tsx and analytics.css; focused UI/runtime test
assertions follow new list behavior; no new API fields/table/permission/provider.
Overview answers the aggregate question. Drill answers which Shipments contributed,
with rows/count and their exact source filters; it is not a reporting homepage.


### Single Overview / purposeful drill outcome

LOCAL DONE under the [latest accepted presentation amendment](personal-analytics-contract.md).
The old Report entry and six dimension tabs are removed. All new drills point to
one source-list route; compatible old URLs render rows only. Main Overview keeps
four KPIs, one locally switchable volume/fee chart, status distribution, Parcel
breakdown and five concise recent rows. Removed duplicate summary cards, decorative
sparklines, repeated toolbars and retired filter/tab CSS; no new dependency.

Presets/status/route/grouping update immediately; custom dates use one confirmation
and cancel keeps applied values. Source list has contextual heading/date/chips,
explicit code search, immediate sort and pagination. No repeated charts/KPIs/global
filter form. Noninteractive zero KPI/status groups cannot expand a filtered cohort.
Return to Overview/list preserves date, selectors, chart mode and source page;
Personal alias return fixed. Existing private detail/label authorization unchanged.

Observed gates:
-73 frontend units PASS; typecheck/lint and default/Customer builds PASS.
-94 distinct UI cases ultimately PASS: full run92 PASS and2 failures confined to
  tests (a new alias test read rows before asynchronous load; old loading-copy
  assertion no longer matched). Added readiness assertion and checked the named
  loading region without dropping error/zero/privacy assertions; focused2/2 PASS.
- Final removal of nested vertical list scroll checked separately at1440/390px,
  focused2/2 PASS, final builds PASS. No whole-suite rerun claimed after that CSS.
- Synthetic root/list desktop/tablet/mobile visuals reviewed; recent mobile rows
  remain readable, full source table scroll is contained and there is no overflow.
- Actual HTTPS/PG18/test-SMTP browser13/13 PASS, parent Maven exit0, receipt
  PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED. Runtime assertions now verify the contributing
  row's actual2 Parcels/53000 VND rather than looking for removed duplicate KPI cards.
  Final list-only scroll CSS separately UI verified; no new live Gmail claim.
- Contract530 PASS; implementation validator recorded on final source. No Backend
  source/schema/migration/application data or accepted security control changed.

Original uncommitted work13 paths and prior checkpoints preserved by SHA-256 before
this amendment. No commit/push/merge or new worktree; current hosted CI not claimed.


## Owner compact / vivid charts follow-up — 2026-11-09

- [x] OV-09 Compact heading/date/status/route/action toolbar; calendar range
  popover with immediate presets and confirmed custom dates. Per-metric true
  sparklines including real zero; remove KPI info buttons, retain accessible
  definitions. Same source-list-only drill and zero-group safeguards.
- [x] OV-10 Add observed route volume, confirmed fees by route and Parcel
  timeline alongside existing volume/status/weight views; stronger Navy/Orange
  palette with restrained categorical accents. No fabricated prior series,
  paid/COD/Delivery or event-time claims. Frontend tests/visuals/real browser.

Planned: PersonalAnalytics, AnalyticsCharts and analytics.css; focused period
picker and date arithmetic tests, implementation manifest and traceability.
No new backend DTO, source query field, schema, provider or business authority.


### Compact / vivid Overview outcome

OV-09/10 LOCAL DONE. Header is compact; each of four KPI values has a source-backed
mini chart and accessible definition without an info button. Six complementary
main charts use actual source values, exact VND and clear time/unit labels.
Route/point/weight drills remain list-only and preserve applied context. Calendar
popover supports bounded dates, presets, keyboard arrows, Escape/focus return,
outside dismissal and viewport bounds on desktop/mobile. No fake comparison line.

Observed verification:
-77 frontend unit tests PASS;100 full Customer UI cases PASS. Typecheck/lint and
  default/Customer builds PASS. Synthetic0/1/75-row desktop/tablet/mobile visuals
  reviewed; all four mini plots and main chart dimensions have regression checks.
- Initial focused run had one test-selector mismatch: trigger includes a functional
  chevron; date assertion now checks the exact date span. Full100-case run passed.
- Final calendar artifact recheck2/2 PASS at1440/390px. Screenshot now captures the
  viewport, avoiding full-page resize dismissing the popup. Outside-dismissal test
  clicks the visible activity heading, because mobile calendar overlays Overview
  heading. No forced click, skipped case or production behavior suppression.
- Actual HTTPS/PG18/test-SMTP browser13/13 PASS; Maven parent1 test/0 failures/
  0 errors/0 skipped, exit0. Receipt PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED. No claim
  of hosted exact-commit CI, real Gmail delivery or full Carrier/Finance acceptance.
- Contract validator530 PASS; implementation validator errors[]/CONSISTENT before
  final evidence updates, revalidated after documentation. No Backend, migration,
  original application DB data, provider, credentials or authority changes.

Original15 uncommitted paths preserved byte-for-byte before edits in ignored
.demo/overview-vivid/before-source with SHA-256 manifest; prior archives retained.
Final source snapshot and manifest also preserved locally. Source remains on
feature/argopost-overview-completion, uncommitted; no push/merge/new worktree.
