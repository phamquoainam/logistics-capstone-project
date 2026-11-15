# Organization Shipment Management V1 — implementation checkpoint

Owner accepted the exact Local/Test contract in
[Organization Shipment Management V1](organization-shipment-management-contract.proposed.md).
Branch: `feature/argopost-organization-management`; baseline `5172dee570df26c593c15662b1318166cf64323f`.

## Implemented scope

- Business **Quản lý → Quản lý vận đơn**: nine navigation groups, real booked
  records and existing Draft workflow. Groups without Carrier/Support sources
  explain their dependency instead of returning synthetic success or zero counts.
- A server-side captured Organization/facility cohort powers literal search,
  Vietnam date boundaries, route/service/payment/source/creator/import/assignee
  filters, deterministic pagination, counts and exact VND totals. Finance validates
  captured obligations independently. Legacy list API stays unchanged.
- Eight distinct Shipment tabs retain immutable Booking/Parcel/Quote facts,
  Merchant source metadata and filter-preserving return links. Internal assignment
  history is separate from Carrier Tracking.
- Explicit scoped export and PII permissions; bounded 200-record JSON projection
  becomes CSV or literal-string XLSX. Access audit must commit before output.
  Selected label ZIPs contain all authorized original PDFs or produce no file;
  limits are 20 Shipments and 32 MiB. Documents retain simulation identification.
- Internal assignment uses current verified Identity + same-Organization active
  Membership/read scope, explicit `shipment.assign`, ETag `aN`, idempotency and
  actor/purpose-bound receipts. Database event/receipt constraints and transactions
  make changes atomic. Unknown outcomes use receipt lookup, not blind mutation retry.

V27 introduces four Orders-owned assignment/export evidence tables and extends
the audited operator permission allowlist. No grants or source data are seeded.
V1–V26 remain unchanged. No Carrier acceptance, payment collection or lifecycle
transition is introduced; Business shipments remain NOT_ACCEPTED / UNPAID.

## Observed verification

- Frontend unit tests: **136 PASS**. Typecheck, lint and both builds PASS.
- Affected UI suites: **28 PASS**, including facility-only admission and the
  existing Business screens. Synthetic desktop/mobile images checked, long codes wrap
  inside their cell and PII checkbox aligns with its label.
- Actual HTTPS/PostgreSQL18/test-SMTP Browser suite: **16 PASS**, parent JVM test
  **1 PASS**. Includes lost assignment response/receipt resolution, eight tabs,
  literal export, selected private labels, filter-preserving return, and existing
  Personal/Identity/Support journeys. This is not a live Gmail delivery claim.
- Focused PostgreSQL: seven Management cases and populated V26→V27 upgrade PASS;
  subsequent **201 real test Bookings** verify full-cohort totals and fail-closed
  export limit, with no truncated file or access audit on denied output.
- Backend default/H2: **674 PASS**, **17 policy-expected skips**; report guard PASS.
- Full PostgreSQL18 regression/build: **1,115 PASS**, **5 policy-expected skips**,
  **0 failures/errors**; fail-closed report collection PASS. This includes all
  **356 Identity/application integration tests**, **60 migration tests**, scoped
  assignment versus concurrent target revocation and the 201-Booking export bound.
  Architecture **23 PASS** is included in the required regression.
- Final report-policy suite: **130 PASS**. Testcontainers privacy and cleanup
  audits both PASS; only the recorded disposable CI container was removed.
- Canonical validator `python -B docs/reconciliation/v3/validate.py --implementation`:
  PASS; default docs-only mode is inapplicable to this Owner-authorized source task.

Commands exercised: frontend `npm.cmd run test`, `typecheck`, `lint`, `build`,
`build:customer` and `test:customer-ui -- shipment-entry-parity.spec.ts business-ui.spec.ts`;
backend `mvnw.cmd -o -B -ntp clean verify` and the owned disposable-PostgreSQL
checkpoint with `-Ptestcontainers-postgres -Ddemo.test.database=postgres`.
PostgreSQL credentials are generated inside private test evidence, never copied
from the developer database. The accepted application validator and
`python -B -m unittest discover -s scripts/ci -p test_verify_reports.py` cover
source inventory and fail-closed report collection (**130 PASS**).

## Deployment and remaining dependencies

**V27 is tested on disposable databases only.** No localhost migration, actual
account grants, production enablement, commit/push or merge is part of this checkpoint.
The developer database container was observed `exited` before the final PG gate;
the harness verified and preserved that exact state and container identity.
No developer database start/stop, migration, data reset or volume removal occurred.

OSM-01–07 are **LOCAL IMPLEMENTATION DONE** with the evidence above. No hosted
GitHub CI run or PR has been created for this uncommitted checkpoint; local gate
results are not represented as remote integration checks or deployment acceptance.

Current grants do not acquire export/assignment implicitly. An explicitly approved
operator deployment must migrate the intended environment and grant only required
permissions before those controls can be used by a real demo account.

Carrier lifecycle/acceptance/rejection, handover manifests, Pickup, cancellation
and change workflows, Organization Support/Claims, COD/collection and absent
Store/Partner masters remain unavailable. Merchant facility, import job and source
channel must not be relabeled as those operational resources. No Customer or
Merchant action can mark delivery, payment or Carrier custody through this feature.

## Subsequent Local deployment and integration preparation - 2026-11-16

The disposable-only deployment statement above records the original checkpoint.
Owner later approved exact-target runtime deployment; the existing demo database
now has forward V27-V30. Management/Reports permissions remain separate from
Contact/Internal Work permissions. OAA-08 records the verified Local deployment,
data/checksum preservation and Owner-confirmed screens in the
[combined checkpoint](organization-action-address-checkpoint.md).
Owner now authorized commit/push and normal PR merge after current required CI.
OSM, MRF and OAA share the accepted source dependencies and are prepared together
against develop5172dee. No Carrier, collection, COD or Production expansion.
