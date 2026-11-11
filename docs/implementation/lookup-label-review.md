# Personal Tracking, Estimates and Labels —2026-11-09

Owner request authorizes dedicated peer-level menu pages, reusing existing own
Shipment/handling/catalog/private PDF/Quote/Draft contracts. Preserve59 WIP paths;
no new worktree, migration, provider, Carrier/Payment mutation or Guest API.

- [x] TL-01 Audit contracts and preserve existing source/archive fingerprints.
- [x] TL-02 Purposeful own-code lookup, bounded multiple codes, exact match,
  safe missing/foreign/unknown states and current permitted tracking.
- [x] TL-03 Catalog-bound estimate and dimensional eligibility, approved routes/
  services/drop-off information; contact-free facts handoff into new draft and
  fresh authoritative Quote/TTL checks before Booking.
- [x] TL-04 Labels workspace: own list/search/paging/current-page selection,
  existing protected PDF/ZIP, actual per-Parcel pages, supported paper and guides.
- [x] TL-05 Unit/UI/responsive/build/security/browser/PG evidence and traceability.

## Controlled presentation amendment and dependencies

Owner explicitly asks for cost estimation. A deterministic catalog-based UI
estimate is an indicative calculation, NOT an issued Quote or a new Pricing API.
It supersedes the earlier presentation prohibition on client cost estimates only
for this informational calculator. No quoteId, expiry, money, payer, authority or
service grant is transferred to Booking. Backend still reads persisted Draft
facts, reissues exact Quote with15-minute TTL and rechecks it on confirmation.
No schema/API field is added. User can transfer only raw Parcel facts and route
selection into a new draft; cannot overwrite an existing draft/contact selection.

Tracking remains authenticated/current-self. At most10 unique valid Shipment
Codes per explicit request; exact-code binding after existing paged own search.
Bounded source scans must report unknown on exhaustion/error, never false absence.
No automatic Guest exposure, contacts in results, external tracking URL, created
Carrier event or printing-as-handover. Guest tracking needs a separate reviewed
redacted public contract and is not inferred from this screen request.

Only ROAD_STANDARD/four directed SIM routes are approved. There are no real
Station coordinates, opening hours, cutoffs, nearest-point/distance search, ETA,
multi-service comparison, COD, insurance or VAS sources. Show their availability
accurately; no fake address/map, fee, schedule or commercial capability.

Existing private PDF contains100x150mm per-Parcel Code128 pages and simulation/
UNPAID provenance. Reuse those exact bytes; no browser-generated Parcel codes,
QR format, receipt/POD or unsupported paper conversion. Per-Parcel page selection
uses the PDF print dialog, including ALL continuation pages for that Parcel.
Current accepted P2 renderer can emit several pages for one Parcel when contact
content is long; no UI page-count inference from Parcel count is allowed.
Existing multi-Shipment ZIP remains max20/32MB and
requires authorization for every PDF; no partial bundle or persistent PII cache.

## Observed local evidence

TL-01–05 LOCAL DONE within the bounded supported scope, not full commercial
Tracking/Quotation/Document acceptance. No new mutation API or production Backend
implementation change; Backend test host adds the Labels SPA forward only.

- `npm.cmd test`:100 units PASS, including6 new exact-code/scan-bound/cancellation/
  integer price/volumetric/eligibility/raw-handoff negatives. Source limits and
  true per-Parcel rounding are checked independently of UI rendering.
- Typecheck/lint/default+Customer build PASS. Full UI run:152 PASS/2 failures;
  old deferred-module assertion still expected Labels to be unavailable, and a
  new Draft echo fixture returned noncanonical contact phones. Corrected fixture/
  expected screen only, no production validator relaxation.49 affected UI cases
  then PASS. Final continuation-page wording and return context are covered by
  54 affected UI cases PASS, including Customer/Tools/Station label navigation.
  All154 distinct UI cases pass across these runs; no false single full-run PASS.
  Synthetic1440/390 estimate/coverage/labels renders reviewed with no body overflow.
- `mvnw.cmd -q -Ptestcontainers-postgres
  -Dtest=IdentityGrantApplicationTests#pilot*+stationLabels*+stationCustomerMissingRevokedWrongStationAndUnknownBarcodeAreDenied+customerEntryBrowserUsesActualHttpsPostgresAndSmtp test`:
  13/13 PASS,0 failures/errors/skips. Existing tests cover real tariff/coverage,
 15-minute expiry/replay/version guards, ownership/isolation, concurrent Booking,
  atomic failure rollback, disabled environment/payment denial and private stable
  multi-Parcel PDF/Draft/wrong-Station denial. No old migration changed.
- Real HTTPS/PG18/test-SMTP Browser13/13 PASS. Actual sequence: two-Parcel53000VND
  indicative calculation -> raw-fact handoff -> explicit Draft -> fresh server
  Quote -> single Booking -> exact own/missing code lookup -> permitted tracking
  -> authorized PDF and actual one-member ZIP. Existing postconditions verify
  two Parcels/one UNPAID obligation/one later Staff acceptance; printing does not
  create payment/custody/tracking. No Owner application accounts/data are seeded.
- After correcting continuation-page guidance to match existing P2 renderer,
  final focused Browser parent1/1 PASS and all13 browser cases PASS again. Safe
  receipt PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED. Both focused runs have credential
  privacy and owned-resource cleanup PASS. Raw evidence is ACL-protected/ignored.
- Source/manifest validation errors[] and semantic ownership CONSISTENT; whitespace
  PASS. Unchanged H2/CI collectors, architecture and report policies are not
  claimed rerun. Hosted exact-commit CI/publication remains pending; no commit,
  push, merge, new worktree, reset/clean or discarded WIP.

Existing localhost remains LOCAL_FULL_STACK_HTTP_READY. HTTPS `/personal/lookup`
and `/personal/labels` return200 under the existing Customer auth application.
PostgreSQL and V1–V21/old domain data/secrets/certificates retained. No Gmail/AWS
live test is claimed by this UI checkpoint. `.demo/lookup-label-review/` preserves
the original59-path source snapshot/fingerprints and final source evidence.

## Explicit remaining capabilities

Guest redacted tracking, genuine Station locations/hours/nearest lookup and ETA,
additional service comparisons, special goods/Pickup/COD/VAS, QR changes, issued
handover receipts/POD/payment documents and unsupported paper sizes still require
their accepted sources/authority/contracts. They are not fabricated here. A PDF
contains all Parcel labels/continuations; selecting one Parcel for printing means
all of that Parcel's pages, not blindly selecting a single ordinal page.
