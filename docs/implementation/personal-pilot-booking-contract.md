# Personal pilot Quote → Booking — authorized contract

Authority: Owner final pilot activation, [bounded BR13 amendment](../decisions/br13-local-pilot-01.md).
Delivered on PR38, subsequently merged by Owner at0704a6bfb42455a89bcffb16eb731108d0b3e121.
The original delivery baseline was develop fecd646, before that merge. PW03/V18,
PW04/V19 and V20 remain intact. [Milestone B](milestone-b-station-acceptance.md)
adds private labels and separate current operational tracking with forward V21;
the immutable V20 carrierState NOT_ACCEPTED remains its booking-time snapshot.

Runtime must explicitly select shipping.pilot.environment LOCAL or TEST; default
DISABLED and any Production/unknown mode deny issuance/booking even if rows exist.
Activation additionally checks current approved immutable service/tariff/coverage
versions/effective periods. No request-controlled environment/region/amount/status.

merchant owns the approved simulation dataset,3 area/drop-off records and exactly4
directed routes A1↔A2 INTRA/A1↔B1 INTER. Pricing owns immutable approved tariff,
persisted own quotes and consumption. Orders owns confirmed pilot Shipment/Parcel,
address/draft/quote snapshots and booking receipts/audit. finance owns the immutable
UNPAID sender obligation. workflow coordinates only public APIs in the existing
CustomerAdmission transaction; no domain imports another module's internal types.

Endpoints under existing /api/v1/customer guard:
- GET /shipping-catalog: actual gate/versions, explicit simulation areas/routes/
  drop-off capability and approved service limits. No physical Station claim.
- POST /shipment-drafts/{id}/quotes: If-Match and UUID Idempotency-Key; input
  {originCode,destinationCode,ordinaryGoodsConfirmed:true}. Draft version/parcel
  facts are read from Orders; classify route from the approved dataset only.
- GET /shipping-quotes/{id}: self-only original snapshot with expiry and current
  usable flag; replay does not reset15m TTL or extend idle.
- POST /shipment-drafts/{id}/bookings: If-Match/UUID Idempotency-Key; {quoteId}.
  Payer/method are the accepted sender CASH_ON_HANDOVER policy, never client price.
- GET /booking-receipts/{key}: scoped non-PII result metadata for unknown writes.
- GET /quote-receipts/{key}: scoped quote identity metadata for unknown issuance.
- GET /shipments: own search/reference/code/contact, fixed deterministic paging20,
  optional status WAITING_FOR_HANDOVER; GET /shipments/{id}: owned immutable detail plus its
  actual finance UNPAID obligation. Current Carrier facts use the separately
  authorized handling projection; no paid/change/cancel endpoint is added.

400 input errors,401 session,404 foreign/missing resources/receipts,409 stale/key/
expired/consumed/unsupported/policy-disabled conflict,428 missing version,503 source
unavailable. Quote201 replay same original identity/expiry; booking201 replay same
Shipment after current authorization even after original quote expiry. Different
binding conflicts. No blind write retry; scoped receipt recovery.

Quote snapshot binds owner/draft/version, exact parcel facts, route/capability,
coverage/service/tariff versions, approval/effective interval, per-parcel integer
fees, simulation tax treatment, createdAt/expiresAt. Generic drafts may exceed
service bounds; quote rejects them. Non-ordinary/special/dangerous/perishable goods
are not supported; required sender attestation never implies Carrier acceptance.

Booking serializes owner/draft, rechecks version/eligibility/expiry and consumes the
quote once; creates one immutable Shipment and relational Parcel rows, exact quote/
address snapshots, UNPAID obligation, audit/receipt, and appends BOOKED draft
version. Any source/audit/obligation/receipt failure rolls back everything, including
quote consumption. No code path may silently save only a subset. Same-quote or
same-draft concurrent booking has one winner; replay safe after expiry. Cross-owner
denial before exposing source/quote/receipt/obligation. UI labels all pilot records
as simulation, waiting for handover/NOT_ACCEPTED and UNPAID, never delivered/paid.

V20 extends V19 state checks forward to BOOKED; prior snapshots remain readable
internally and immutable. Old APIs still exclude converted drafts. No old migration
edit or history cleanup. Runtime cannot alter catalog/tariff/Shipment/Parcel/
obligation/history/audit/receipts; the quote update path can only consume once.


## Owner Create Shipment audit presentation — 2026-11-09

Quote UI retains issued fee/weight/total/expiry as authority. It explains first
step and additional weight steps only when the immutable catalog tariff version
and calculated explanatory sum agree with the server fee snapshot; otherwise
omits the decomposition, never overwrites issued cước. No client quote engine,
new surcharge or request field. Explicit sender-only UNPAID, unsupported ETA,
insurance/VAS/Pickup/COD and non-tax-invoice treatment remain accurate.
Full source review and catalog-limit guidance precede the existing Booking action.
[Checkpoint](create-shipment-review.md) records source and actual validation.

## Owner Tracking / quotation tools amendment —2026-11-09

Latest Owner explicitly requests a dedicated cost estimator and preparation
handoff. [Tools checkpoint](lookup-label-review.md) narrows the presentation
amendment: client calculation from the accepted catalog is indicative only,
not issuance of a Pricing Quote. BigInt per-Parcel/volumetric/step rounding and
exact approved routes/limits apply. No new price, surcharge, insurance, COD,
service, payer, source override or backend endpoint. Failed/disabled catalog
cannot become a successful0-price estimate. Backend environment guards remain.

Only raw measured Parcel facts and route selection transfer in transient browser
navigation state to a new draft. Unknown fields, injected money/quoteId, invalid
facts or conflicting existing draft/address selection are rejected. No contacts
or credentials in that state; no automatic write. Goods descriptions and contacts
still require Customer completion. Existing Draft save and fresh authoritative
Quote precede Booking; the estimate carries no quoteId/TTL or payment meaning.
Chosen route preselects review only; backend current coverage is still authority.
Old approved Quote expiry/replay/version/ownership/atomicity/security rules stay.
