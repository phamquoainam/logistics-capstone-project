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
