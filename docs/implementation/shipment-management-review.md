# Shipment Management audit — Owner-directed,2026-11-09

Authority: current Owner Shipment Management request. Existing approved Personal
Management query/DTO, Milestone B label/handling and immutable Booking contracts
remain. No new Backend field/API/schema/grant/policy. Single worktree; original25
working paths preserved with SHA-256 in ignored .demo/shipment-management-review.

## Tasks and bounded plan

- [x] SM-01 Actual list/eight-tab/API/policy audit; preserve current WIP.
- [x] SM-02 Repair canonical category/filter/empty-page behavior and detail return
  context, URL tabs/keyboard; one current handling read shared by badge/detail,
  exact Parcel identity binding and stale-result suppression.
- [x] SM-03 Current-page optional columns/CSV and selected private-label ZIP,
  maximum20 currently returned records, existing self-authorized PDF GET per row,
  no new broad data query or mutation. Final artifact only after every read passes;
  no partial success, auth/query change abort; memory URLs revoked on disposal.
- [x] SM-04 Tests/visuals/actual HTTPS-PG browser/validators; document accurate
  completion and unsupported workflow dependencies. No automatic publication.

First unfinished task SM-02: ShipmentManagement.tsx, PilotShipping.tsx,
ShipmentHandling.tsx and targeted tests. SM-03 adds a small presentation/export
helper and tests, management styles and source manifest. No Backend changes.

CSV includes current visible columns only, current page (or selection), exact
values/status/time, simulation provenance/filter metadata; no extra addresses,
internal actor/audit or hidden pages. Quoted cells neutralize spreadsheet formula
prefixes. Selection does not survive source changes and never grants Carrier rights.
ZIP stores original validated private PDF bytes, retaining100x150mm/Code128 and
simulation/UNPAID markings; not a merged financial/custody document. Limit32MiB.

Current Owner request authorizes these bounded download presentation actions on
already-owned reads, superseding the earlier presentation exclusion of export;
no unreviewed API/DTO is introduced. Real lifecycle/collection/cancellation/change/
redelivery/return/support workflows remain excluded until their accepted contracts.


## Observed outcome — LOCAL DONE for approved capabilities

| Requested group | Actual result / remaining dependency |
|---|---|
| All / Draft / Waiting | Real self-owned list; Draft continuation/archive retains history; WAITING bookmark canonicalized to actual Backend status |
| Current accepted state | AT_STATION from committed handling, never In Transit/Delivered; immutable booking carrierState not reused as current status |
| Search / filters / sort | Existing server code/reference/contact/recipient/date/service/route/UNPAID and four sorts; client max366-day guard; no local row filtering disguised as a server result |
| Source context | Detail/back keeps exact source filters/page/column choices; empty page removes page only, not all conditions |
| Columns / output | Four optional column groups; fixed identifiers/status/actions; CSV current page or selected current rows; formula-safe, exact source values and simulation provenance |
| Multiple labels | Up to20 selected current-page Shipments; existing authorized private GET per Shipment; bounded ZIP retains full per-Shipment PDF, fails before download on any denial |
| Eight detail tabs | URL-named tabs/reload/keyboard; actual source snapshot/reference/notes/Booking date/service/handover/ETA availability; Parcel identifiers/facts; current committed events; UNPAID; private labels; honest support dependency; public business history |
| Operational read consistency | Badge and all tabs share one handling read; reload masks stale state; exact Parcel set must match immutable Shipment before rendering operational content |
| Future filters/sorts | Independent destination/drop-off selectors, Pickup, third-party payer, updatedAt/status sorts lack accepted query/source; not invented or silently mapped to Booking dates |
| Carrier lifecycle / change workflows | Transit/Delivered/POD/Returns/Cancel/Redelivery/Support and related commands remain BLOCKED pending accepted sources/authority; not shown as successful zero lists |
| Money | UNPAID only; no PAID/COD/collection/finance transition or commercial receipt |

No Customer action can alter Tracking/custody or cancel a booked/accepted Shipment.
The new output actions are bounded representations of existing authorized reads,
not new mutation/bulk authority. Shipping PDFs keep original simulation markings.

## Verification actually executed

-85 frontend units PASS; typecheck/lint/default+Customer build PASS.
- Full UI run114 PASS and1 failed fixture case; corrected the fixture to return
  actual contract400 VALIDATION_FAILED for unsupported sort instead of accepting
  it as valid synthetic data. Focused1/1 PASS. Thus115 distinct UI cases passed
  across these runs, not a claim that the initial full run was entirely green.
- Final responsive-only adjustment anchors the column menu inside mobile viewport
  and scales table minimum width to visible columns; focused5/5 responsive/CSV/ZIP
  cases PASS at1440/768/390, final build/lint PASS. Images inspected.
- Actual HTTPS/PostgreSQL18/test-SMTP Browser13/13 PASS, parent Maven1 test,
  0 failures/errors/skips, exit0; safe receipt PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED.
  Journey verified actual Booking/53000VND UNPAID, filtered list/detail return,
  real CSV and authorized private label ZIP; existing Station and PDFBox/Code128
  gates remained. Final responsive CSS/table-width adjustment followed this run
  and is covered by focused UI/build; no redundant full-stack rerun claimed.
- Independent Python zipfile CRC/member/magic checks PASS:2 synthetic PDF members
  and1 real browser-downloaded PDF member. Individual real PDF page/policy/barcode
  validation remains in the parent PDFBox gate; Python pypdf is unavailable, so no
  additional Python PDF-page verification is claimed.
- Contract validator530 PASS; implementation validator errors[]/CONSISTENT; final
  documentation/whitespace/source validation follows below through retained logs.

Initial focused execution uncovered an actual selection-initialization null guard:
undefined selection and undefined data were compared equal, then ids was read.
Fixed explicit selection presence. The interrupted initial run is not PASS evidence.
Column disclosure covered the target link in one test; added Escape/outside/blur
behavior and explicit keyboard dismissal, targeted return test PASS. No assertion
relaxation, forced click, retry, test disable or business/security gate bypass.

No Backend/schema/migration/original app-data/credential or provider change. Existing original25
WIP paths preserved by SHA-256; final snapshot archived locally, no source discard.
Same feature/argopost-overview-completion branch; no commit/push/merge/new worktree.
Hosted exact-revision CI and commercial/full Carrier/Finance acceptance are not
claimed. Local readiness checked separately without init/reset or app DB seeding.
