# Personal / Business Create Shipment alignment

Owner direction 2026-11-13. Baseline 814370a, clean develop; post-merge CI 42993777682
SUCCESS. Work uses the same working directory and a new feature branch.

- [x] SEP-01 Inventory current journeys/contracts and actual parity gaps.
- [x] SEP-02 Share contact/Parcel/notes/review/presentation components and units.
- [x] SEP-03 Align both entry layouts; retain Merchant ownership/source metadata,
  immutable source keys, real save/receipts and add saved Business review.
- [x] SEP-04 Focused unit/UI/responsive and real HTTPS/PG Browser regression;
  source manifest/traceability and accurate remaining-capability report.

First implementation task SEP-02: extract shared pure entry fields from
ShippingDrafts.tsx, reuse in BusinessPreparations.tsx. Keep transport/admission
separate. No new API/DTO/schema or migration for this slice. Personal address book
continues to use self-owned data; Business must not read an employee's Personal
address book as an Organization master. Changing a form or opening review does
not confer quote/book/label/Carrier/Finance rights.

| Capability | Personal current | Business current / remaining |
| --- | --- | --- |
| Sender/recipient, measured1–5 Parcels, notes | Saved Personal Draft | Saved Org Draft; align same fields/components |
| Address book/default | Self-owned contacts | No Org-scoped contact master; do not leak Personal contacts |
| Source channel + source order ref | Optional user reference | Accepted immutable per-Org source key; keep separate from reference |
| Facility/defaults/templates/packing | Not Merchant workflow | Accepted BSC-05b/c contracts; runtime not yet implemented |
| CSV/XLSX preview/row commit/history | Not exposed | Implemented Org Draft import |
| Quote/Booking/UNPAID/labels | Accepted PERSONAL Local/Test pilot | BSC-06 exact amendment accepted2026-11-13; implementation and test gates below, no localhost activation yet |
| COD/Pickup/insurance/collection/ETA | Unsupported in pilot | No additional commercial authority from this UI request |

Do not claim Business creation fully supersedes Personal while Organization contact,
facility/default/template masters, internal packing and batch confirmed Booking
remain incomplete. Common UI parity is separate from domain capability parity.
New exact wire/data proposals must
be explicitly accepted under AGENTS.md before that dependent implementation.

## BSC-06 implementation checkpoint

- [x] BSC06-01 Exact wire/data and owner-variant/guard amendments Owner accepted.
- [x] BSC06-02 V26 populated upgrade, owner/facility/snapshot/receipt invariants.
- [x] BSC06-03 Actual contract-based Quote/TTL/current authorization and receipts.
- [x] BSC06-04 Atomic Booking/shared Shipment/Parcel/UNPAID and private labels.
- [x] BSC06-05 Real Business review → Quote → Booking → detail/list/label UI.
- [ ] BSC06-06 Final required regression, source manifest and traceability evidence.

Observed development gates: shared presentation21 cases PASS, then Business
Quote/Booking/expiry and Personal UI24/24 PASS; existing Business/import UI15/15;
targeted transport/measurement25/25. Architecture23/23 and label renderer2/2 PASS.
Initial four PG Business cases PASS. Subsequent regression identified and corrected
new test fixture facility-column order, explicit UTC timestamps and current26
migration inventory. Production denial, scoped receipts and contract-suspension
serialization use actual PostgreSQL, not UI-only guards.

Actual Browser test-host initially lacked the newly added controller; it now imports
BusinessShippingHttp and its direct-refresh paths. The expanded existing Browser
journey passed all 16 cases, with actual HTTPS/PG18/test-SMTP and real Quote/Booking
receipts and PDF, without timeout increases. Final rerun after remaining source
changes also passed all 16 Browser cases and its parent in the Testcontainers
regression execution. Gmail live delivery is not
part of this checkpoint. No localhost migration, contract activation, grant,
commit/push/merge or new worktree is claimed.

## Final source/test checkpoint — 2026-11-13

SEP-01–04 and BSC06-01–05 are DONE for source/disposable-test scope. BSC06-06 is
not DONE: the remaining Foundation health gate below is not replaced by Browser
or unit PASS. None of these checks authorizes localhost V26 migration/activation.

| Gate / exact command | Observed result |
| --- | --- |
| `npm --prefix app/frontend/web test` | 126 unit cases PASS |
| Affected `test:customer-ui` Personal/parity/Business/import cases | 39 distinct cases PASS; final parity/Business rerun 10/10 PASS |
| `npm --prefix app/frontend/web run lint`, `run build`, `run build:customer` | PASS, including TypeScript checks; bundle-size warning remains nonblocking |
| `mvnw.cmd -o -B -ntp -Ptestcontainers-postgres -Dtest=* -Ddebug=false -Dtrace=false test` | 1,103 discovered: 1,086 PASS, 17 SKIP, zero assertion failures/errors. Includes 348 Identity application, 59 migration, 23 architecture, 2 PDF renderer and real Browser parent PASS. Twelve legacy Foundation/health tests were skipped because `demo.test.database` was not set; this invocation alone is not the required complete PG gate. |
| `mvnw.cmd -o -B -ntp -Ddebug=false -Dtrace=false test` | H2 report-policy PASS: 666 PASS, 17 expected SKIP. Four stale PostgreSQL-only XML files from the prior invocation were excluded from the H2 report set, with original reports preserved. |
| Isolated Linux runner, `-Ddemo.test.database=postgres -Dtest=PostgresFoundationTests,PostgresHealthSafetyTests test` | 8 Foundation + 3 health PASS; remaining `stalledSocketProbeIsBoundedEvictsAndRecoversWithoutTimeoutLeak` errored before its scenario because that runner lacks Docker CLI. Initial Maven read-only cache lock error corrected only in runner configuration. No test assertion relaxed or disabled. |
| `python -B scripts/ci/test_verify_reports.py` | 128/130 PASS initially; exact 2 inventory-order failures corrected and rerun PASS. 130 distinct cases verified; not a single full 130/130 run claim. |
| `python -B docs/reconciliation/v3/validate.py --implementation`, `git diff --check` | PASS; V1–V25 source diff empty |
| Credential privacy / Testcontainers owned-resource absence audit | PASS after supplying original execution identity and Docker CLI PATH to the audit process |

Actual Browser result: 16/16 PASS on final Frontend/source, HTTPS + PostgreSQL 18
and test SMTP. Visual review of the synthetic Business booked detail confirms
source-order metadata, saved contacts/Parcels, exact fee, UNPAID and private-label
action. Independent 1440/768/390-width form checks cover both workspace themes.
Private reports, synthetic screenshots and changed-source SHA-256 manifest remain
under ignored `.demo/shipment-entry-parity` and `.demo/business-shipping-review`;
no raw private evidence or runtime output is added to the source manifest.

The remaining health test requires an owned disposable CI PostgreSQL container on
port 5433 plus Docker pause/unpause. The existing localhost database occupies that
port. Automated approval review rejected temporarily stopping/restarting that
local container because explicit interruption approval is absent. The rejected
command did not execute; localhost was not stopped/migrated. Disposable Linux test
containers were removed only after their recorded ownership checks. No workaround
was used to bypass that rejection. Completion requires explicit Owner permission
for the short local DB interruption, or a suitable independent CI runner.

No current hosted CI, commit, push, PR or merge is claimed. V26 source remains
unapplied to localhost. Organization contacts/facilities/defaults/templates,
internal packing and batch confirmed Booking remain separate unfinished tasks;
Business Dashboard aggregation is not connected by this Create Shipment slice.

## Owner UI follow-up — creation modes, 2026-11-13

- [x] CM-01 Remove the standalone Business import sidebar item; keep Shipment
  and Draft navigation because confirmed and editable preparation records differ.
- [x] CM-02 Present one shared, branded route-aware “Tạo thủ công / Nhập CSV/Excel”
  switch inside both creation screens. The sidebar highlights Create Shipment in
  either mode. Existing `/imports` bookmarks, including the job query parameter,
  and import history still work.
- [x] CM-03 Confirm before abandoning unsaved manual/worksheet data. Busy or
  unknown-write outcomes block switching; missing import permission exposes no
  import link. Switching does not issue save/Booking commands or extend authority.
- [x] CM-04 Targeted UI 30/30 PASS (five new navigation/dirty/permission/unknown-
  outcome cases included), mobile/desktop snapshots visually reviewed; lint and
  normal build PASS, Customer typechecked build PASS within the UI runner.
- [x] CM-05 Actual HTTPS/PG18 Browser regression through the new entry path:
  `IdentityGrantApplicationTests#customerEntryBrowserUsesActualHttpsPostgresAndSmtp`
  PASS, Browser 16/16 PASS, including manual creation, the nested import entry,
  preview/explicit Draft commit, Quote/Booking and labels. SMTP is test transport.

Commands: `npm --prefix app/frontend/web run test:customer-ui --
shipment-entry-parity.spec.ts business-grid-ui.spec.ts business-import-ui.spec.ts
business-ui.spec.ts`, `npm --prefix app/frontend/web run lint`,
`npm --prefix app/frontend/web run build`. Production source changes in this
follow-up are Frontend-only. API, database, privileges and business rules retain
their earlier scope; no local DB interruption/migration or commit/push is performed.
The earlier Foundation health blocker is not resolved by these UI checks.

## Owner integration follow-up — consolidated Business navigation

Owner subsequently requested removing Shipment and Draft as separate sidebar
entries and authorized commit/push/merge to develop, subject to mandatory gates.

- [x] CM-06 Business sidebar now contains Overview and Create Shipment only.
  Confirmed shipments and Drafts remain real records accessible from within the
  creation flow, not deleted business capabilities. Create stays highlighted for
  import, preparation list and confirmed Shipment routes. Read-only Draft members
  retain a navigation entry to their permitted read screen.
- [x] CM-07 Updated the affected sidebar expectations; targeted UI 30/30 PASS,
  lint and normal typechecked build PASS, Customer typechecked build PASS in the
  UI runner. No new Backend/schema change in this navigation follow-up.

Required hosted CI on the pushed PR revision must close the remaining integration
gate. Earlier local-only test results are preserved above and do not substitute
for that gate. Merge does not authorize applying V26 to the localhost database or
activating an Organization contract/grant there; that source/test-only boundary
remains in effect. Full Business Workspace is not claimed.
