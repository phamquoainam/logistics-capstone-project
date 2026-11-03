# Personal Management — bounded read contract and delivery

Status: **ACCEPTED exact query/DTO extension by Owner,2026-11-03**.
Authority: Owner directive803ce74e-f3d4-495a-9163-a8ef96d6b69b,2026-11-03.
Owner explicitly approved this exact read extension after the initial audit;
implementation may proceed within the boundaries below.

## Observed baseline and empty-state finding

One working directory, branch `feature/argopost-customer-portal-ui`, HEAD
`e7de0c50157913b19da1d8ef4cf0e04709ce5ac6`. PR41 remains OPEN, not in develop.
Existing18 modified/untracked source paths preserved in ignored checkpoint before
edits (patch SHA2569548c697049c81d512bb7daa0297c4812f4762625368f768c63434123388e21e).

Original PostgreSQL `elogistics_dev` audited READ ONLY:0 Draft,0 Quote,0 Shipment,
0 Parcel,0 obligation and0 Orders tracking events at this checkpoint. Thus the
existing Shipment empty state is consistent with the database for every Customer;
it cannot be explained by another owner or an active filter hiding existing rows.
CI/Testcontainers browser records are separate owned temporary databases; they
never populate this original local database. No demo insert/seed is authorized here.

The existing GET list supports only page/size/search/status, and its SQL searches
codes/reference/contact names but not telephone numbers. Date/service/route/payment
and sort controls require a reviewed backend read extension, not client filtering.

## Exact proposed backwards-compatible HTTP extension

Reuse `GET /api/v1/customer/shipments`; no second Shipment table/engine or mutation.
Existing requests without `view=management` keep their exact original shape and
parameter allowlist. New parameters require `view=management`; no silent fallback.
Both modes retain CustomerAdmission, HTTPS, Bearer, no-store and LOCAL/TEST guards.
Unknown/duplicate query parameters, owner/org/context selectors reject400.

| Parameter | Exact meaning |
| --- | --- |
| view | `management` selects the response below |
| page, size | zero-based0..500; fixed20; existing exact deterministic paging |
| search | optional literal case-insensitive substring1..160 of code/reference/sender or recipient name/phone; self-owned rows only |
| recipient | optional literal substring1..160 of recipient name/phone; AND with search |
| from, until | optional YYYY-MM-DD pair, start inclusive/end exclusive Vietnam midnight, maximum366 days; omitted means all Booking dates; no future end beyond tomorrow |
| status | omitted=all; WAITING_FOR_HANDOVER or AT_STATION only |
| service | omitted=all supported observed service; ROAD_STANDARD only |
| origin, destination | supplied together; exactly one of four approved directed SIM routes |
| obligation | omitted=all; UNPAID only; no PAID inference |
| sort | NEWEST default, OLDEST, FEE_DESC, FEE_ASC; tie-break Shipment UUID ascending |

Response exact fields:
`schemaVersion: PERSONAL_MANAGEMENT_1`, `generatedAt` UTC instant,
`timeZone: Asia/Ho_Chi_Minh`, `summary`, `total`, `page`, `size`, `hasNext`, `items`.
All counts and VND sums in summary/total use decimal integer strings; no float money.
`summary` exact fields: `totalShipments`, `waitingForHandover`, `atStation`.
It uses every non-status filter before applying the selected status; the two
exclusive status counts sum to totalShipments. `total` and page items apply the
selected status too. UI names this summary scope; do not label it unfiltered global
totals or sum Draft counts into booked Shipment totals.

Each item exact fields: `record`, `status`. `record` is the existing, unchanged
owned Shipment/Parcel snapshot + Finance obligation + simulation response object.
`status` is WAITING_FOR_HANDOVER or AT_STATION from committed Orders acceptance
events, never V20 immutable carrier_state or inferred physical delivery. No PII in
aggregate KPIs; item contacts are the same self-owned data already returned by list.

## Domain and transaction implementation boundary

Orders owns management query/filter/summary/page and immutable snapshot reads.
Apply owner/type/CONFIRMED and all filters server-side before counting/paging.
One captured SQL cohort supplies pre-status counts and selected page so a concurrent
acceptance cannot disagree between count and row status. Avoid join multiplication
of Parcel/event rows. Bounded page hydration uses the existing immutable Shipment
projection; no contact/owner mutation or new domain storage.

Workflow composes only Orders and Finance public APIs in the existing admission
transaction. Finance independently verifies actual obligation owner/quote/amount/
currency/state before returning records; missing/mismatched facts fail closed.
Payment filters must not invent payment state from UI data. Current pilot requires
one immutable UNPAID obligation per Shipment and cannot be collected/changed.

Fees/statistics reuse the accepted PersonalAnalytics API/service, exact VND/cohort
definitions and URL drill-through. Do not create another analytics calculator.
Its period is explicitly labelled; management defaults to all Booking dates.
No cross-module internal import, schema migration, grant, seed or policy change.

## Presentation and canonical categories

Parent Quản lý contains existing Shipment route, new bounded dependency screen
`/personal/action-required`, and existing `/personal/billing` as the primary fees/
statistics entry. Preserve old links, Draft URLs, eight Shipment detail tabs and
private PDF generation. Documents remain accessed from the corresponding Shipment;
no duplicated finance/document implementation or invented invoice.

Nine category tabs share existing resources: all, Draft, waiting, transit,
requires-action, completed, returned, cancelled, related-support. Only all/Draft/
waiting have executable V1 data sources. Transit/Delivery/Return/Cancel/Support
categories show a specific dependency view, not a fabricated zero table. AT_STATION
is supported as a status filter/KPI; it never maps to In Transit or Delivered.
No bulk cancellation, import/export, call history, COD edit, partial delivery or
compensation action without its accepted workflow. Existing per-Shipment PDF/detail
actions remain; no fake bulk-success button.

Action-required screen has no exception/ticket source today; show this dependency
without an empty-success claim, unsupported POST or fake rows. No total exception
KPI until owning Carrier/Support workflows exist. Collapse/expand sidebar supports
keyboard, current route, deep linking and narrow viewports.

## Execution checklist

- [x] PM-01 read-only source/database audit and preserve WIP.
- [x] PM-02 exact extension approved by Owner; no schema or mutation expansion.
- [x] PM-03 DONE locally: parent navigation, owned table, nine source-aware categories.
- [x] PM-04 DONE locally: approved server filters, captured KPI/status page and stable sort.
- [x] PM-05 DONE locally: existing Analytics fee view and honest action/dependency screen.
- [x] PM-06 VERIFIED_LOCAL:2 real PG read-contract tests,68 frontend units,63 UI cases,
  actual HTTPS/PG18/test-SMTP browser13/13 PASS. Existing Station race/atomicity gates
  remain required in the full CI; do not infer a full PostgreSQL suite from these runs.
- [ ] PM-07 source validator, scoped commit/PR, exact-revision CI; no auto merge.

Positive browser: actual booking -> owned list/status -> detail, Draft separation,
search/contact/date/route/payment/sort/page and Back/Forward. Negative: A/B isolation,
invalid/duplicate selectors, unsupported lifecycle, expired/revoked access,
Production deny, unavailable data never converted to empty success. No new demo
data in original PostgreSQL without explicit separate Owner seed disposition.

## Final local evidence and demo boundary

Original database configuration verified separately from Customer records:
merchant.pilot_area3, merchant.pilot_route4, pricing.pilot_tariff_version1;
21 successful Flyway migrations, latest V21. Original Draft/Shipment/Parcel/Quote/
UNPAID/tracking tables still empty after focused tests. The local launcher rebuilt
and restarted only its own Backend/Frontend processes using the existing database,
certificate and secrets; no init/reset, migration edit, Staff grant or dataset seed.

The real browser journey used the actual new management GET with recipient/date/
service/route/UNPAID/sort and verified the53000VND obligation, fee analytics, actual
Station acceptance and private labels. Its DB was an owned temporary test database,
receipt `LOCAL_UNPUBLISHED`, `PASS/CLEANUP/NONE`; never the local customer database.
Images under ignored `.demo/management-*.png` use explicit synthetic UI fixtures.
No real contact details, credentials, SQL data dump or raw diagnostic is published.

The first new PG failure was a double-encoded URI in a new MockMvc test; use its
URI overload and preserve assertions. The observed UI failures were a missing exact
accessible select name and an existing broad status assertion racing a loading
status; both fixed specifically. Report-policy initially rejected stale expected
inventory counts324/1069 after two tests were added; exact expectations are now
326/1071, keeping all fail-closed rules and three-suite ownership audit unchanged.

For an Owner demo, use the existing signed-in Customer create -> review -> quote ->
confirm workflow on an approved SIM route; that produces legitimate local UNPAID
records. Do not auto-populate another account or fake Station/Transit/Delivery facts.
A scripted local seed remains NOT EXECUTED and requires its own bounded approval.
Until then, empty local tables and nonzero temporary test receipts are both correct.
Required hosted evidence belongs in the PR on its exact source/integration SHA,
not retroactively inferred from the earlier PR41 checks.
