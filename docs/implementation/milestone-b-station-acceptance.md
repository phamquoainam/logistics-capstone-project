# Milestone B — Personal labels and simulated Station intake

Authority: Owner execution directive15983bb2-16f5-458a-bbeb-54a671716d8d,
2026-11-02. Baseline PR38 merge0704a6bfb42455a89bcffb16eb731108d0b3e121.
One existing working directory; branch feature/station-dropoff-acceptance.
Scope is LOCAL/TEST simulation only, not commercial Carrier operation.

## Execution tasks

- [x] B-01 BASELINE — DONE: clean root, fetched actual PR38 merge, V20 implementation
  and canonical ownership inspected; new branch created from actual origin/develop.
- [x] B-02 CONTRACT — DONE: Owner explicitly approved the bounded unpaid/all-Parcels
  exception; exact technical ownership, authority, transaction and API contract below.
- [x] B-03 LABEL — DONE / VERIFIED_LOCAL: Orders-owned stable Parcel Code128 labels
  from immutable snapshots; private PDF download/print view, actual render/decode.
- [x] B-04 STATION — DONE / VERIFIED_LOCAL: approved SIM-DROP capability, explicit
  current assignment/permission, separate real Operations UI and persisted scan evidence.
- [x] B-05 ACCEPTANCE — DONE / VERIFIED_LOCAL: complete all-Parcels command, scoped
  receipt/replay/concurrency, custody/audit and Orders events in one admission transaction.
- [x] B-06 PERSONAL — DONE / VERIFIED_LOCAL: actual operational model and labels in
  own list/eight detail tabs; acceptance never implies In Transit/Delivered or PAID.
- [ ] B-07 VERIFY — LOCAL DONE: PG upgrade/data preservation, authorization/races/faults,
  PDF/barcode, frontend and actual HTTPS/PG browser PASS. Exact-source hosted closure
  is recorded in the new PR checklist after publication, without another docs-only revision.
- [ ] B-08 PUBLISH — scoped commit/push/PR with exact source/integration evidence;
  no automatic merge and no local production staff fixture.

## Ownership and authorized technical contract

Exact [OpenAPI](../../contracts/openapi/personal-station-intake.yaml) and
[operator/runtime runbook](../runbooks/station-pilot-operations.md).

Orders owns booked Shipment/Parcel snapshots, stable label projection and tracking/
Parcel business events (BC140/182; spec008 does not transfer these to WMS).
Dispatch owns Station capability, staff assignments, scan/acceptance/custody and
operational audit. Identity owns the existing verified human account/session and
CSRF/TLS; Finance retains the immutable UNPAID obligation and no collection API.
Workflow calls public APIs in one CustomerAdmission transaction; no cross-module
SQL or new authentication stack. Staff personal Identity is not a Carrier grant.

Minimum Staff contract: current eligible verified Identity plus an explicit ACTIVE,
effective Dispatch assignment for that exact Station with STATION_INTAKE permission.
Assignment rows have no runtime create/update permission or public self-grant route.
Controlled test fixtures only; runtime migration seeds no Staff account/assignment.
An owner-only audited Dispatch function and explicit local operator script can
grant/revoke one selected existing verified Identity UUID at one approved Station;
no automatic execution or account creation. Runtime cannot call that function.
No broad SYSTEM/Identity allowlist expansion, customer/merchant role inference or
request-provided actor authority. A missing/revoked/out-of-scope assignment denies.

Dispatch registers only SIM-DROP-A1/A2/B1 with dataset ARGO-SIM-COVERAGE-1,
simulation provenance and drop-off capability; no operational network expansion.
Workflow revalidates the booked route through the existing Merchant coverage API.
The station must match the quote's origin drop-off code. Pickup remains unavailable.

Label identifiers use AP-P- plus the existing immutable Parcel UUID without hyphens;
no canonical UUID/Shipment Code changes. Code128 contains only that opaque code,
never PII/authentication material. One100x150mm PDF page per Parcel includes branding,
snapshot sender/recipient, route/service/weight/index and conspicuous LOCAL/TEST
SIMULATION and UNPAID statements. Generate in memory; no public file/S3 dependency.
GET requires current self ownership or current matching Station permission; deny
Drafts, foreign owners and wrong Station before rendering. No duplicate label row.

Scans persist actual server-observed actor/station/Parcel/Shipment/time and immutable
evidence; device/manual keyboard input uses the same strict barcode parser. Backend
resolves identifiers; client cannot override price/measurement/owner/state. A scan
is not custody or acceptance. All-Parcels acceptance requires exactly the current
Shipment's Parcel scan evidence for the same authenticated actor and Station plus
MATCHED inspection declaration; missing/duplicate/foreign scans or discrepancy deny.
No partial acceptance or invented hold lifecycle. Staff must resolve discrepancies
outside this scope before retry; no snapshot/price rewrite.

Carrier acceptance locks the shipment/intake scope, rechecks live assignment, route,
state/version and policy, then atomically persists Dispatch acceptance, per-Parcel
custody, operational audit/receipt and Orders shipment/Parcel tracking events.
The immutable V20 booking carrier_state remains its booking-time NOT_ACCEPTED fact;
a separate authoritative read model returns current AT_STATION/version1 from events.
No in-place rewrite of accepted booking or financial snapshots.

Receipt keys are scoped to actor+Station+operation. Same payload replay returns the
original outcome after current authorization; different binding conflicts. Lost
write outcomes recover through GET receipt, never blind POST. Reads/replays do not
extend idle; only newly committed meaningful commands do. Current permission stays
locked through commit; audit/tracking/custody/receipt failure rolls back all effects.

## Accepted bounded payment disposition

BR13-LOCAL-PILOT-01 permits unpaid booking only. Owner subsequently explicitly
approved [BR13-LOCAL-INTAKE-01](../decisions/br13-local-intake-01.md) for the exact
LOCAL/TEST PERSONAL all-Parcels unpaid Station intake boundary described above.
Activation still requires the implementation/security/PG/browser gates.
No payment success, collection authority, COD, Delivered or Production/Merchant
permission follows from this milestone.

## API surface under the existing authenticated Customer Identity chain

Customer: GET /api/v1/customer/shipments/{id}/labels.pdf and /handling.
Operations: /api/v1/station-operations, separately presented from Personal:
GET /stations; GET /stations/{code}/parcels/{barcode}; POST /stations/{code}/scans;
GET /stations/{code}/shipments/{id}; GET /stations/{code}/shipments/{id}/labels.pdf;
POST /stations/{code}/shipments/{id}/acceptance; GET /stations/{code}/receipts/{key}.
Mutations require existing same-session CSRF/origin and UUID Idempotency-Key;
acceptance additionally requires If-Match operational version. Exact schemas are
published in the corresponding OpenAPI file with implementation before publication.
401 invalid session;403 no current assignment;404 missing/foreign source;400 invalid
syntax;409 state/key/policy conflict;428 missing version;503 unavailable dependency.
Every result is private/no-store and carries simulation provenance. Unknown outcomes
stay unresolved until authoritative receipt/source read, never optimistic success.

## Capability checkpoint

| Capability | UI | Backend | PostgreSQL | Authorization | Tests | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Shipping Label | Personal/Station label actions | In-memory snapshot PDF | Existing immutable V20 source, no redundant label rows | Current self or matching Staff | Actual HTTP/private PDF + rendering | DONE / VERIFIED_LOCAL |
| Barcode | Parcel code + printed Code128 | Strict opaque stable UUID mapping | Existing Parcel IDs retained | No credentials/PII encoded | Decode every synthetic PDF page + parser negatives | DONE / VERIFIED_LOCAL |
| Print/Download | Save/open prepared100x150 PDF print view | Private PDF response/no-store | No public file/S3 dependency | Foreign owner/Station denied | Actual browser downloads PDF and opens print controls | DONE / VERIFIED_LOCAL |
| Staff Access | Separate Operations route/login | Existing Identity/CSRF/TLS plus Dispatch guard | No automatic Staff seed | Current verified account, explicit permission | Customer deny; actual Staff login; revoked session | DONE / VERIFIED_LOCAL |
| Station Assignment | Actually assigned Station selector | Live scope/capability admission | V21 assignment/audit; owner-only grant/revoke | Runtime cannot self-grant/update/execute | Wrong/revoked/inactive and operator audit tests | DONE / VERIFIED_LOCAL |
| Drop-off Verification | Scan every Parcel and matched inspection | Source route/set/state revalidation | Immutable scan facts | Same current Staff and Station | Foreign/duplicate/missing/unknown scans, mismatch | DONE / VERIFIED_LOCAL |
| Carrier Acceptance | Explicit all-Parcels confirmation | Bounded BR13-LOCAL-INTAKE-01 | One immutable acceptance | Expected version, current assignment | Replay/key conflict/two-Staff race/rollback | DONE / VERIFIED_LOCAL |
| Custody Evidence | Actual accepted result | Dispatch owns per-Parcel custody | FK/uniqueness/deferred completeness | No Customer custody authority | Custody/audit/receipt fault rollback | DONE / VERIFIED_LOCAL |
| Tracking Events | Actual Station status | Orders owns Shipment/Parcel events | Append-only and complete at commit | Workflow uses public APIs | Tracking/Parcel-event fault rollback | DONE / VERIFIED_LOCAL |
| Personal Integration | Real filter/state/time/barcodes/eight tabs | Self-only handling projection | Immutable booking + current event source | Current own scope, no internal Staff audit | Actual HTTPS/PG browser refresh and UNPAID | DONE / VERIFIED_LOCAL |

## Observed evidence — pre-publication checkpoint

Initial focused31 PASS:6 new Station HTTP/PG cases,2 PDF cases and23 architecture
cases. Subsequent extended runs cover all10 Station cases and3 V21 migration cases;
the final whole browser method PASS executes13 actual HTTPS/PostgreSQL18/test-SMTP
cases, including Staff login,2-Parcel scan, acceptance, original receipt recovery
after forwarding+losing real responses, own tracking refresh and UNPAID retention.
No API fulfilment or test transport substitutes for Station/Carrier effects.
Final browser receipt is under .demo/milestone-b-browser-final-*; previous failures
remain in ignored diagnostics. Backend source/PG fixtures, not local Customer data,
produce the synthetic Staff account/assignment. No local operator grant executed.

Frontend55 unit cases PASS. Full frontend typecheck/lint/default+customer builds and
37 isolated UI cases PASS, including separate no-authority context, fixed Operations
login return, unknown Acceptance recovery and invalidation of a prepared private PDF
when switching Shipment/Station. Desktop/mobile and rendered PDF pages
visually inspected; Code128 decoded from actual rendered pages. Physical printer/
scanner hardware is not claimed tested. New OpenAPI validates.

Accurate resolved findings: initial Java Timestamp import ambiguity; a forbidden
Dispatch→Pricing access fixed by the Orders public projection (no architecture gate
relaxed); V21 shared trigger accessed assignment-only fields on Station updates;
browser read-before-load and missing test-host Operations deep-link route; fixed
login destination captured from Router state rather than changing global location.
The regression/hosted gates below still govern integration readiness.

Final local regression: PostgreSQL18 clean verify1055 PASS/5 policy skips, zero
failure/error; all13 actual browser cases PASS. Disposable-container cleanup and
credential/privacy audits PASS; original PostgreSQL container restored healthy.
The three stale migration-list expectations now include V21; checksum and historical
upgrade assertions were retained. Latest full frontend UI37 PASS covers the additional
prepared-PDF binding defect; typecheck/lint/default and customer builds PASS.

Original local database: official migration launcher PASS through V21. Aggregate
fingerprints of every prior domain table and all V1–V20 checksums match the pre-run
snapshot; no local Staff account/assignment seeded. Existing trusted certificates,
Gmail/AWS settings and persistent volume retained. Launcher start/status reports
LOCAL_FULL_STACK_HTTP_READY. Local private diagnostic evidence remains ignored under
.demo/milestone-b-*; no raw PII, credentials, runtime files or browser artifacts belong
in this PR. Final exact commit, hosted integration SHA/run/artifact and B-07/B-08
closure belong to PR metadata, preventing a documentation-only verification loop.
