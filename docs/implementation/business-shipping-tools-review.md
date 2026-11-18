# Business Tracking / Quotation and Labels / Utilities

Owner-directed 2026-11-16. Baseline c8cdfa3 / PR48; its post-merge CI passed.
Branch feature/argopost-business-shipping-tools; existing working directory only.

## Tasks / existing contract mapping

- [x] BTU-01 Inventory BSC-06, OSM-03, WFP-02/06, current APIs and authority.
- [x] BTU-02 Organization-only exact multi-code/reference lookup and authorized export.
- [x] BTU-03 Current Merchant contract, saved-Draft single/bounded batch quotes,
  expiry/receipt recovery and explicit Booking review, never Personal fallback.
- [x] BTU-04 Filtered confirmed labels, private preview/PDF/ZIP and source-bound
  Parcel checklist / existing internal handover navigation.
- [x] BTU-05 Catalog/coverage, packaging and measured-weight utilities; truthful
  unsupported service/ETA/Pickup/COD/Carrier document states.
- [x] BTU-06 Focused unit/UI, actual HTTPS/PG Browser, architecture/source gates;
  record evidence and remaining dependencies before marking delivery DONE.

No new API, DTO, permission or table is proposed in this slice. UI routes
`/merchant/{org}/lookup` and `/merchant/{org}/labels` orchestrate the already
accepted BSC/OSM/WFP APIs. Every resource request retains current Backend
Organization/Membership/facility/permission checks. No localhost migration or
new member grant is necessary or authorized by this implementation checkpoint.

Lookup uses OSM management literal search then exact Shipment/Parcel/reference
binding, up to ten inputs and ten pages per input; exhaustion/errors are unknown,
not not-found. Export reuses audited OSM export, at most20 selected IDs and explicit
PII permission. No contacts in lookup cards or new anonymous tracking endpoint.

Batch pricing is explicit UI orchestration of at most10 saved Draft versions,
each with its own approved Quote command/UUID/receipt. No atomic batch API or
automatic Booking is implied. Stop on an unknown write and resolve its receipt
before any subsequent write. No blind POST retry. Reopening a Quote reads it and
revalidates Organization/Draft/version/facility/current server usability before
existing Booking. Quote TTL is derived from serverTime, not a browser clock.

Labels retain original authorized100x150mm Code128 PDF bytes and continuation
pages. ZIP bounds remain20 Shipments/32MiB; any authorization/read failure denies
the whole download. A preview object URL is memory-only and revoked on close,
navigation or session change. No invented Parcel QR, printer integration, Carrier
receipt/POD or print-history authority. Internal WFP lists remain explicitly
NOT_CARRIER_ACCEPTANCE. Packing checklist uses confirmed immutable Parcel facts.

Only accepted current Merchant tariff/scope is used. Existing contract projection
contains version/effectivity metadata, not a full commercial agreement; do not
infer detailed contract-document access from OWNER role. No commercial service,
scheduled Pickup, ETA, COD, price tier, external printer/API principal or Carrier
lifecycle is added. V1–V32 and all existing application data remain unchanged.

## Observed checkpoint — LOCAL DONE, supported scope only

Implementation reuses Business Shipping, management/export and facility workflows;
Backend production classes, SQL migrations, Identity and authorization contracts
are unchanged. Backend test host adds only the two SPA forwards.

- `npm.cmd --prefix app/frontend/web test`: **169/169 PASS** on final source.
  Six new units cover identifiers/references, exact binding, scan cancellation/
  exhaustion/repetition,20-result bound and Quote ownership/version/Parcel binding.
- `npm.cmd --prefix app/frontend/web run typecheck`, `run lint`, `run build`:
  PASS. `run test:customer-ui -- business-shipping-tools.spec.ts`: **9 cases PASS across focused runs**;
  this includes its Customer build. Earlier unaffected Business shell9 and
  Merchant preparation3 UI regressions also PASS; do not describe these separate
  focused runs as one full UI run. Customer build retains the existing advisory
  chunk-size warning, not a failed gate.
- New UI cases cover exact Shipment/Parcel lookups, no Personal source or contact
  projection, denied refresh clearing old data, audited no-PII lookup export,
  lost Quote receipt before the next POST, suspended-contract denial, fresh
  read/scope validation for local non-contact Quote CSV, private label preview,
  selected PDF ZIP and denied output, measurements and390/1440px layouts.
  The quote CSV is a local copy of current readable Quote facts, not a new
  server-audited Quote export job or commercial document. No contact/reference
  columns are exported; current organization-wide export permission is required.
- Focused Maven command using an ACL-protected separate build directory:
  `mvnw.cmd -Ptestcontainers-postgres -Dtest=ArchitectureTests,ArchitectureNegativeFixtureTests,IdentityGrantApplicationTests#businessQuote*+businessBooking*+businessConfirmedShipping*+businessManagement*+customerEntryBrowserUsesActualHttpsPostgresAndSmtp test`:
  **39/39 PASS,0 skipped** (23 Architecture;16 PostgreSQL application tests,
  including the Browser parent). No full H2/PG suite rerun is claimed.
- That parent ran **18/18 real HTTPS/PostgreSQL18/test-SMTP Browser journeys**.
  The existing Merchant facility journey now saves a two-Parcel Draft, obtains
  its50000VND Quote through the new Business tool, reopens the actual Quote in
  Booking review, confirms one Shipment/UNPAID, looks up its code, opens actual
  Carrier dependency state and downloads its real private label. Existing
  preparation/checklist/handover PDF/reload steps remain successful afterward.
  No fulfilled API fixtures, production data seed, live Gmail or AWS claim.
- Focused-test credential privacy PASS. Full-suite lifecycle collector correctly
  refused the partial one-suite inventory; an independent read-only check then
  verified the observed/verified/stopped Identity suite bindings and absence of
  every recorded container/volume/reaper and run-labelled resource. No deletion
  performed. Existing developer PostgreSQL remains healthy.
- Implementation/source manifest validator errors[] and whitespace check PASS.
  No source/CI test-count guard was weakened; Browser case inventory unchanged.
- Desktop/mobile synthetic screenshots reviewed. Fixed inherited global label
  flex-direction, explicit search-control accessible name and initial Quote
  countdown rounding. Confirmed no horizontal body overflow.
- Official `scripts/dev/local.cmd status`: **LOCAL_FULL_STACK_HTTP_READY**.
  Strict trusted-CA HTTPS checks: both new localhost SPA routes and their Vite
  source module return200. No restart, secret read/rotation, migration, account
  grant or developer database write was needed. Owner session UI is not claimed
  manually accepted by this checkpoint.

Raw/private test output, lifecycle records and synthetic visual artifacts remain
ignored under `.demo/business-shipping-tools/`. Source is on the feature branch;
no commit/push/PR/new hosted CI or merge is claimed for this implementation turn.

## Still dependent on separate sources/authority

Commercial coverage/Station hours/nearest search, additional service comparisons,
ETA, Pickup and COD; full negotiated-contract documents/field-level disclosure;
other label sizes/templates/QR and printer/API-client integration; persistent
print-history; actual Carrier intake receipt/POD and Business tracking lifecycle
are not fabricated here. Existing WFP internal packing and handover lists remain
Merchant evidence only, never proof of Carrier acceptance. These task checkmarks
are bounded tool delivery, not full commercial Business Workspace acceptance.
