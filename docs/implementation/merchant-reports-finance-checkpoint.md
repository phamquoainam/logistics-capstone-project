# Merchant Reports & Finance V1 — implementation checkpoint

## Subsequent Owner-approved localhost activation, 2026-11-15

Owner explicitly approved the verified runtime and two Organization-scoped read
grants for Customer `5d206ef3-350e-42b7-af8a-4c54b8d43fe2`, NamShop Demo
`782c4766-8bdd-435b-9961-512c831922d6`. This supersedes the deployment limitation
of the original implementation checkpoint below for this exact target only.

Observed: local database was already V28. Backend sources matched the verified
full-PG build fingerprints; the deployed JAR matched that artifact and contained
the Reports API and unchanged migration resources. The existing launcher lifecycle
stopped only its two owned processes and restarted Backend/Frontend with the same
local secrets/certificates. Its task-local adapter reused the verified artifact
and asserted V28 instead of building or executing Flyway. HTTPS readiness PASS at
`https://127.0.0.1:5173/` and Backend8443; no database reset or new container.

Audited operator request `e523cf7f-42c2-4ea6-acc3-46598f2863d4` added exactly
`shipment.reports.read` and `shipment.finance.read`, both Organization scope;
existing prerequisite grants were retained. Verified two target read grants,
one GRANT audit and one command receipt. Fingerprints of all other grants and
memberships, Shipments, Parcels, obligations, preparations and migration history
were unchanged. No export, COD, payment, payout or Carrier permission was added.

Live authenticated menu/API interaction remains **OWNER BROWSER CHECK PENDING**.
Computer Use could not determine the Windows browser URL confidently enough to
enforce its policy and stopped before browser input. This is not represented as
a successful live browser check. Prior disposable HTTPS/PG18 Browser16 PASS still
describes implementation verification. Owner can reload the existing NamShop Demo
session to receive the updated context/authority and open the two new menu items.

Owner accepted [Reports & Finance V1](merchant-reports-finance-contract.proposed.md)
on 2026-11-15. Branch `feature/argopost-organization-management`; unchanged HEAD
`5172dee570df26c593c15662b1318166cf64323f`. Existing Management V1 working source
is a dependency, not assumed merged. The 39 preceding source modifications were
copied with SHA-256 before this task and the preserved copies still match.

## Implemented behavior

- Business **Quản lý → Báo cáo vận hành Merchant** and **Cước, COD & Đối soát**
  are distinct screens. Time intervals support day/week/month/quarter/year with
  Vietnam date boundaries, clipped periods and explicit incomplete buckets.
- A captured Organization/facility cohort supplies Shipment/Parcel/actual and
  chargeable grams, route/service/warehouse/creator/source-channel breakdowns.
  Totals are not calculated from the 20-row page or member Personal data.
- Current preparation states and CREATE/BOOK audit activity have separate grains.
  Booking-only filters make these internal sections NOT_COMPARABLE rather than
  being silently ignored. Downloading a label is not counted as packing/printing
  or Carrier handover.
- Explicit `shipment.reports.read` and `shipment.finance.read` are independent;
  draft.read remains required. Operations money is null/FORBIDDEN unless Finance
  covers the whole scope, including empty-cohort checks. Finance independently
  validates the captured immutable Shipment/Quote/facility/obligation bindings.
- The fee statement uses initial confirmed UNPAID obligations, per-Parcel actual
  and chargeable weights, consumed Quote and contract/tariff versions. The initial
  Quote-to-obligation difference is not a Carrier adjustment or reconciliation.
  Integer VND/gram aggregation and rational averages avoid floating-point loss.
- Charts preserve exact cohort selectors in Management drill-through. Unbound
  warehouse/channel groups and scopes the legacy filter cannot represent use a
  bounded within-report contribution list; no broadened list is presented as an
  exact group. Same-Organization detail return links retain the report context.
- Aggregate CSV/XLSX exports have no contact columns, preserve filters, protect
  formula/DDE literals and require scoped export rights plus a committed access
  audit. A searched Finance view cannot silently export an unsearched cohort.
- Own export history is re-authorized on read and reopens current queries; it
  does not pretend to retain an archived invoice or downloadable historical file.

V28 adds an append-only report-access journal and extends explicit operator grant
allowlists without backfill, automatic grants, source or Finance table rewrites.
V1–V27 are unchanged; V27 and the preceding Finance/Management public contracts
still match the source checkpoint fingerprints. New sources remain composed
through module APIs and the existing admitted local database transaction.

## Observed checks

- Frontend unit suite: **145 PASS**; typecheck/lint and both builds PASS.
- Affected UI: **37 distinct PASS** across reporting9, entry/Management19 and
  existing Business9. The reporting/Business17 cases were rerun after report-view
  pagination/scope cleanup; reporting9 subsequently PASS after the 366-day mobile
  chart-overflow correction, including keyboard drill to the final bucket. Synthetic desktop/mobile artifacts were
  rendered; no real PII is used in these fixtures.
- Focused PG18: **7 PASS** (five Reports/Finance security/integration cases,
  populated V27→V28 upgrade and actual Browser parent). Actual Browser includes
  real scoped Operations/Finance reads, export/history, fee-to-detail return and
  prior Identity/Personal/Support/Business journeys, using test SMTP transport.
- Backend H2/build/report guard: **685 PASS / 17 expected policy skips**,
  including reporting unit11 and Architecture23. Report-policy tests **132 PASS**.
- Full PostgreSQL18 regression/build/report guard: **1,132 PASS / 5 expected
  policy skips**, including Identity/application361, migrations61 and
  Architecture23. Actual HTTPS/PG18/test-SMTP Browser **16 PASS** plus parent1.
  Testcontainers privacy/cleanup and canonical implementation source validator PASS.
  No remote GitHub CI result is claimed for this uncommitted working revision.

Local verification uses the same Maven POM with only build output redirected to
an owned ignored task directory retaining `target/test-classes`. Source POM is
checksum-verified and the temporary sibling POM is removed after the invocation.
This avoids stopping the running localhost JAR. Initial blocked clean and a
misnamed isolated output (ArchUnit included test fixtures) are retained as private
failed attempts, not counted as PASS; architecture assertions were not changed.

The owned PostgreSQL checkpoint temporarily stopped the verified existing local
container to use the official test port, then restored the same original container
to HEALTHY. Only the recorded disposable CI container was removed. No volume was
removed; no localhost database migration, SQL reset or Flyway repair was performed.
The running localhost Backend JAR and trusted certificates were not replaced.

MRF-01-07 are **LOCAL IMPLEMENTATION DONE**. Commands exercised: frontend
`npm.cmd run test`, `typecheck`, `lint`, `build`, `build:customer` (also by the UI
harness), and affected `test:customer-ui` suites; backend Maven Wrapper
`-o -B -ntp clean verify` (H2) and
`-Ptestcontainers-postgres -Ddemo.test.database=postgres clean verify` (disposable
PG18), with the isolated output POM described above. Report-policy command:
`python -B -m unittest discover -s scripts/ci -p test_verify_reports.py`.
Source gate: `python -B docs/reconciliation/v3/validate.py --implementation`.
Final diff whitespace check PASS. The last change after full Backend regression
was frontend-only chart containment; it passed the focused reporting9 UI/build/lint
checks and does not change backend source, migration or wire.

## Limits and deployment

**V28 has only been applied to disposable test databases.** No developer database
migration, Customer/Merchant grant, live Gmail send, commit, push, PR or merge has
been performed in this checkpoint. Runtime must receive the intended migration
and explicitly authorized scoped permissions before these screens are available
to a real local demo member. Role names alone do not grant report/Finance access.

COD, collection, payment history, reconciliation/payout, credit/due dates,
commercial invoices, adjustments/refunds and Finance disputes remain unavailable.
Pickup, Business Carrier handover/delivery/return/SLA, actual packing/print events
and independent Store masters are not fabricated. Scheduled emails remain
NOT_SUPPORTED pending an approved recipient/consent/delivery policy. No Merchant
control can write PAID, a ledger, COD collected, payout or Carrier Tracking.

## Subsequent Local deployment and integration preparation - 2026-11-16

The disposable-only statement above records the initial implementation checkpoint.
Owner subsequently authorized exact-target Reports/Finance grants and deployment,
then OAA-08 V29/V30 on the same database. The existing demo now runs V30 with
preserved prior data; no new financial fact or collection authority was created.
See the [combined checkpoint](organization-action-address-checkpoint.md) for current
runtime evidence. Owner has authorized commit/push and PR merge subject to the
current protected-develop checks; remote integration evidence is recorded in PR.
