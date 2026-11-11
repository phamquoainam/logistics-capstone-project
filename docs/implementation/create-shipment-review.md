# Create Shipment — Owner review checkpoint, 2026-11-09

Authority: latest Owner request to audit and complete Create Shipment. Reuses the
accepted preparation/booking contracts, final shipping policy and bounded BR13
amendment. No new DTO/schema/provider or authority is proposed or activated.
Original17-path Overview WIP preserved with SHA-256 before edits in ignored
.demo/create-shipment-review. Single worktree and branch retained; no publication.

## Execution checklist

- [x] CS-01 Audit actual UI/API/policy and preserve uncommitted work.
- [x] CS-02 Remove assumed measurements; explicit default address copy through
  existing paged self-only list; complete saved per-Parcel/reference/note review.
- [x] CS-03 Catalog-derived per-Parcel eligibility messages, authoritative actual/
  chargeable weights and fee breakdown, clear UNPAID/handover/capability boundaries.
- [x] CS-04 Unit/UI/real HTTPS-PostgreSQL browser, responsive visuals, source and
  contract validators; record actual evidence before marking local completion.

Planned files: ShippingDrafts.tsx, shipping-entry.ts, PilotShipping.tsx,
workspace.css; corresponding existing unit/UI/runtime tests and source manifest.
No Backend/migration/data changes. First uncompleted task CS-02 is a presentation/
validation slice on accepted endpoints; authoritative validation stays server-side.

## Actual capability disposition

| Requested capability | Existing source / bounded disposition |
|---|---|
| Sender/recipient | Self-owned saved addresses or new contact snapshot; local0/+84 phone; no recipient Identity requirement |
| Default address | Explicit copy from current self-owned list, including later pages; never asynchronous auto-overwrite |
| Goods/Parcels | Description, contents quantity, physical weight/dimensions/value;1..5 Parcels; measured facts required |
| Service/goods checks | ROAD_STANDARD only; approved catalog limits/ordinary-goods attestation; Backend authority |
| Handover | Four approved directed SIM routes and SIM-DROP only; free text never proves coverage |
| Pickup/calendar | BLOCKED: no approved capability/capacity/appointment contract |
| VAS/insurance | BLOCKED: no service/tariff policy; declared value is not purchased insurance |
| COD/recipient or third-party payer | BLOCKED: no Finance/collection authority; accepted sender-only UNPAID |
| Quote | Server snapshot, actual/chargeable weights/fee lines/total,15m TTL; no tax invoice or invented surcharge |
| ETA | BLOCKED: no valid service calendar/commitment source |
| Draft/review | Save/resume/edit/archive, ETag/idempotency/receipt recovery; full saved snapshot before Quote |
| Booking/after | Atomic Shipment and Parcel identities/UNPAID; private detail/labels and current authorized handling reused |
| Terms | No approved carriage legal text to fabricate; ordinary-goods declaration retained; no invented legal consent |

Booking does not create Carrier Acceptance, payment collection or Delivered.
Commercial/nationwide completeness is not claimed by a bounded Local/Test flow.


## Observed local outcome

CS-01–04 LOCAL DONE for the accepted bounded core. Completed explicit default
address lookup/copy (all existing self-list pages; no auto-overwrite), genuine
required measurements on new Parcels, all saved per-Parcel/contact/reference/note
facts before confirmation, and catalog-based actionable eligibility preflight.
An in-progress default lookup locks entry/save/reset; missing/default network
failure preserves the current input. Existing save/review/ETag/unknown receipt
recovery remains. No new endpoint or persisted field.

Quote displays authoritative actual/chargeable grams and per-Parcel fees/total.
First-step/additional-weight explanation appears only when immutable tariff version
and recomposed amount match the issued server snapshot. It never issues/replaces
server prices. Declared value does not buy insurance; no ETA/VAT/payment is inferred.
Original Backend guards remain the authority even if UI checks are bypassed.

Observed gates:
- Final typecheck/lint/default build PASS; Customer build PASS as UI prerequisite.
-81 frontend unit tests PASS, including exact dimensions/quantity semantics,
  blank measurements, service boundaries and fail-closed fee explanation.
- Full UI105/105 PASS; subsequent final17/17 affected UI checks PASS after the
  fee-explanation presentation addition and default-network negative test. Includes
  lost save/booking, stale draft, expiry, reset, paged default/missing/default-failure,
  blocked save during lookup, blank measurements, oversize denial and responsive
  review/quote. Synthetic1440/390 images inspected; no body horizontal overflow.
- Actual HTTPS/PostgreSQL18/test-SMTP Browser13/13 PASS; Maven parent1 test,
  0 failure/error/skipped, exit0, PASS/CLEANUP/NONE/LOCAL_UNPUBLISHED receipt.
  Verified real multi-Parcel Shipment persistence,53000VND UNPAID, ownership/private
  label and separate Station workflow with the existing test harness. This run
  preceded the last presentation-only fee explanation; final unit/UI/build gates
  cover that explanation. No second full-stack execution claimed.
- Contract validator530 PASS; implementation validator errors[]/CONSISTENT.
  Final whitespace/source validation recorded after documentation updates.
- Initial lint rejected action function named useDefault as a Hook; renamed the
  action applyDefaultAddress, preserving rules. One npm invocation from repository
  root failed ENOENT (no root package.json); commands subsequently run in web.
  No failing business/security gate suppressed or bypassed.

No Backend source/schema/migration changes, no app DB reset/seed or key/certificate
change, no real email/S3 operation. Original application PostgreSQL is preserved;
Browser uses the existing isolated disposable-test policy. Local launcher status
is checked separately. Source uncommitted on feature/argopost-overview-completion;
no current hosted exact-revision CI, commit/push/merge or production readiness claim.
