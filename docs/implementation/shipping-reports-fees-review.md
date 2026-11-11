# Shipping Reports and Fees — Owner-directed review,2026-11-09

Reuse accepted Personal Analytics, Management and immutable Quote/Shipment/UNPAID
reads. Latest Owner explicitly requests a purposeful Shipping Reports entry in
Management, superseding its earlier removal as a duplicate of Overview. New UI
must be analysis by dimension with one same-cohort source drill, not cloned KPIs.
Keep Overview and all existing working paths; no publication/new worktree.

## Delivery tasks

- [x] RF-01 Audit accepted source contracts and preserve original34 working paths.
- [x] RF-02 Separate Reports navigation; day/week/month and calendar-quarter/year
  rollup over the accepted bounded period, status/service/route/Parcel analysis,
  full-cohort aggregate CSV and source-list drill with report return context.
- [x] RF-03 Dedicated Fees page: exact confirmed/UNPAID cohort totals, paged
  charges, lazy owned immutable charge snapshot detail and bounded CSV. Honest
  unavailable Payment/COD/refund/overdue/legal-document capabilities, no fake zeros.
- [x] RF-04 Source/precision/date/denominator/return/export/ownership negative UI
  and unit tests, actual HTTPS-PG browser, responsive review and validators.
- [x] RF-05 Owner-approved read-contract extension below, with actual PostgreSQL
  authorization/cohort/weight/timing and production-denial verification.

Quarter/year are calendar regrouping of returned daily source buckets within
max366 days; partial periods labelled. No multi-year unlimited query or guessed
prior-quarter comparison. Shipments and Parcel quantities remain distinct; reports
use exact integer arithmetic and only the Customer's captured Booking cohort.
Saved CSV includes period/filter/source timestamp and simulation provenance;
Excel/PDF report generation is not falsely offered. Private label PDF stays separate.

## RF-05 — ACCEPTED exact additive read extension,2026-11-09

Reason: Orders already captures acceptedAt, but current Analytics DTO omits timing
statistics and chargeable Parcel weight aggregates. Deriving them from20 source rows
would be incorrect for the full cohort. No shipment/quote/migration mutation needed.

Accepted GET /api/v1/customer/personal-shipping-reports, same exact query allowlist,
limits, current-self admission, transaction, no-store and Local/Test guards as the
accepted personal-analytics GET. Existing endpoint/DTO untouched.
Exact response: {schemaVersion:PERSONAL_REPORTS_1, report: existing PERSONAL_ANALYTICS_1,
acceptanceTiming:{sampleShipments:unsignedIntegerString,totalDurationMillis:unsignedIntegerString,
minimumMillis:null|unsignedIntegerString,maximumMillis:null|unsignedIntegerString},
parcelWeights:{actualGrams:unsignedIntegerString,chargeableGrams:unsignedIntegerString}}.
All objects additionalProperties:false. No contacts, IDs, extra Carrier authority.
Same captured filtered Booking cohort before source pagination. Sample is only
committed accepted Shipments; duration=acceptedAt-bookedAt, never a delivery SLA.
Zero sample =>sum0,min/maxnull, displayed N/A. Average derives from exact sum/sample
with presentation rounding, not an unqualified performance score. Reject negative
or corrupt source data. Chargeable sums come from immutable issued feeLines, not
client dimensions or current tariffs. All Parcels in contributing Shipments count.
Retain max10000 per cohort, no request owner/env/source override. Same denial/error
contracts and read-only behavior; cross-owner/production/malformed-source tests.
Delivery/failure/redelivery/return/exception/ETA/COD/payment collections remain absent.

Owner explicitly replied “Duyệt RF-05 và triển khai”. RF-05 is authorized within
this exact read-only scope; no other approval loop is required. Existing API shape
is preserved and both reads share one captured cohort implementation.

## Local delivery evidence

RF-01–05 LOCAL DONE, not hosted CI or commercial/full Personal acceptance.

- `npm.cmd test`:92 unit cases PASS. `npm.cmd run test:customer-ui`:128 UI cases
  PASS; final13 affected UI cases PASS after final copy refinement. Typecheck,
  lint, default build and Customer build PASS. Desktop1440/tablet768/mobile390
  synthetic renders reviewed; chart axes/focus context and scroll containment
  checked. Fixtures are isolated UI evidence, not application database evidence.
- `mvnw.cmd -q clean verify` H2:683 discovered,666 executed/PASS,17 intentional
  profile skips; existing report validator PASS. Final focused
  `-Dtest=PersonalAnalyticsTests,ArchitectureTests test` PASS, including rejecting
  negative submillisecond source time before integer duration conversion.
- `mvnw.cmd -q -Ptestcontainers-postgres
  -Dtest=PostgresTestcontainersTests,IdentityMigrationTests,IdentityGrantApplicationTests test`:
  7+54+326=387 PASS,0 errors/failures/skips. All3 lifecycle/credential privacy
  audits PASS. This is the existing Testcontainers scope, not the entire hosted
  PostgreSQL matrix or legacy fixed5433 Foundation suite; original DB is retained.
- Final focused `-Dtest=IdentityGrantApplicationTests#personalAnalytics*+customerEntryBrowserUsesActualHttpsPostgresAndSmtp`:
  5/5 PASS after final source guard and runtime test updates. Real HTTPS/PG18/
  test-SMTP Browser13/13 PASS; safe receipt PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED.
  Tests include actual1001g vs1500g full-cohort weights, empty/accepted timing,
  quarter source drill/report return, CSV, immutable53000VND fee detail, own-only
  isolation, revoked sessions, invalid query and disabled-environment denial.
  Final focused test resources absent and credential privacy audit PASS.
- `python -B -m unittest discover -s scripts/ci -p 'test_*.py'`:130 PASS.
  `node contracts/identity-access/validate.mjs`:530 PASS (Identity declarations,
  not RF-05 runtime evidence). Independent RF-05 AJV2020 schema/local references,
  malformed payloads and unchanged legacy path/schema compatibility PASS.
  `python -B docs/reconciliation/v3/validate.py --implementation`:errors[],
  semantic ownership CONSISTENT. `git diff --check`:PASS.

First PostgreSQL attempt failed before resource creation because Docker Engine
was offline. Started existing Docker Desktop and reran in fresh owned test
resources; no application DB reset. New Browser assertions then exposed missing
`/personal/reports` SPA forward in the test host; added that route and corrected
test-source Unicode encoding before the final5/13-case PASS. No assertion disabled
or skipped to close either failure. Earlier failures remain in ignored evidence.

Source groups: Notifications API/service and Orders captured facts/JDBC; Workflow
RF-05 GET; additive OpenAPI; Customer report/fees pages, exact aggregation helpers,
styles/navigation/client/source-return handling; corresponding unit/UI/HTTP/browser
tests and runtime test-host route. No production mutation endpoint, new migration,
Identity policy change or database cleanup. V1–V21 remain unchanged.

Ignored local evidence: `.demo/reports-fees-review/`, with original34-path source
archive, fingerprints, full/focused logs, synthetic screenshots and ACL-protected
PostgreSQL/browser evidence directories. Raw evidence is not for Git publication.
Current branch stays `feature/argopost-overview-completion`; changes remain
uncommitted, no push/merge or exact-commit hosted CI claimed.

Final local runtime: reused `elogistics-d1-postgres18`/existing volume and launcher;
POSTGRES_HEALTHY_FLYWAY_MIGRATE_PASS, trusted HTTPS and LOCAL_FULL_STACK_HTTP_READY.
`https://127.0.0.1:5173/personal/reports` and `/personal/billing` return200;
unauthenticated Backend RF-05 returns401. Existing local secrets/certificates reused,
no init/key rotation/account seed/application data reset. No new live Gmail test
is claimed by this reporting checkpoint. Original34 archived source hashes verified;
final53 working paths archived with SHA-256, retaining earlier checkpoints.

## Remaining capability dependencies

- Accepted/At Station is the latest supported Carrier state; In Transit, delivery,
  attempts, failure, return, exception reasons and ETA/SLA need their own accepted
  sources/authority. Those reports stay unavailable instead of fabricated0%.
- Payment collection/history, due dates, refunds/adjustments, COD reconciliation/
  payout, legal tax documents and online inquiry workflows are not activated.
  Confirmed charges are not collected cash or revenue; UNPAID is not overdue.
- CSV aggregates cover the entire bounded report; Fees CSV is explicitly current
  page only. Excel/PDF report generation is not offered. Existing private Shipping
  Label is still separately authorized and retains Local/Test provenance.
- Production/Merchant activation and unlimited/multi-year reporting remain out of
  scope. Local/Test simulation guards and required CI gates are unchanged.
