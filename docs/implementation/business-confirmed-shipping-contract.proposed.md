# Business confirmed Shipping — BSC-06 exact amendment [OWNER ACCEPTED]

Date: 2026-11-13. Owner explicitly approved this exact contract on 2026-11-13:
“Duyệt hợp đồng và triển khai Business Quote/Booking”. This is the
missing implementation amendment identified by the already accepted Business
preparation contract, not a request to approve tariff/coverage/UNPAID policy again.

## Accepted policy retained

BUSINESS-LOCAL-SHIPPING-01 and BR13-BUSINESS-LOCAL-01: Local/Test sandbox only,
ROAD_STANDARD, four ARGO-SIM-COVERAGE-1 directed routes, simulated drop-off,
versioned per-Organization pricing contract with existing ARGO-ROAD-TARIFF-1
arithmetic and limits, Quote TTL15min, sender pays, initial UNPAID. No COD,
collection, Pickup, production activation or Merchant Carrier acceptance.
Current Identity + active Organization/Membership + explicit permission/facility
scope on every read/write/replay; role names never grant authority.

## Wire — same organization prefix, existing guard/receipt conventions

Prefix `/api/v1/merchant-organizations/{organizationId}`. JSON unknown/duplicate
fields rejected; TLS/Bearer/current admission, Origin/CSRF for writes, no-store.
UUID Idempotency-Key on writes; If-Match current preparation version for Quote
and Booking. Stale409, absent/foreign/scoped-out404, invalid400, expired or
consumed Quote409, unsupported route422, disabled503. Replays require current
authorization and exact binding; no silent repricing or blind POST retry.

| Route | Exact request / response |
| --- | --- |
| GET `/shipping-catalog` | {simulation:true,serviceCode:ROAD_STANDARD,quoteEnabled,bookingEnabled,coverage,tariff,contract,paymentMethod:CASH_ON_HANDOVER,payer:SENDER,paymentCollectionEnabled:false}. Coverage/tariff reuse existing shapes. contract {id,organizationId,version,tariffVersion,state:ACTIVE/SUSPENDED,effectiveFrom,effectiveTo}; server sourced, never caller supplied. No active contract => unavailable capability, no fabricated price. |
| POST `/shipment-preparations/{id}/quotes` | {originCode,destinationCode,ordinaryGoodsConfirmed}; response201 {quote,usable,serverTime}. Quote {id,organizationId,facilityId,creatorAccountId,preparationId,preparationVersion,contractId,contractVersion,tariffVersion,route,parcels,feeLines,totalVnd,taxTreatment,createdAt,expiresAt,consumedBy}; existing route/measured Parcel/fee-line shapes. |
| GET `/shipping-quotes/{id}` | Same Quote envelope; usability rechecks current preparation/contract/coverage/time/permission, not only browser countdown. |
| GET `/shipping-quote-receipts/{key}` | {targetId,outcome:COMMITTED}; actor + organization + facility bound. |
| POST `/shipment-preparations/{id}/bookings` | {quoteId};201 + ETag v1 + Location. {simulation:true,shipment,obligation}. Snapshot fields below; atomic Quote consumption + BOOKED preparation/version + common Shipment/Parcels + UNPAID + audit/receipt. |
| GET `/shipments` | page>=0,size20, search<=160 optional, status empty/WAITING_FOR_HANDOVER. {items:[same Shipment envelope],page,size:20,hasNext}. Only current Org + permitted facility rows; do not aggregate Personal data. |
| GET `/shipments/{id}` | Same envelope + ETag v1; immutable booking snapshot. |
| GET `/shipping-booking-receipts/{key}` | {targetId,outcome:COMMITTED}; same current actor/scope binding. |
| GET `/shipments/{id}/labels` | Existing100x150mm Code128 PDF, one Parcel per page/explicit continuation for long content; current shipment.label.read + Org/facility authorization. Private/no-store; simulation identity retained in printable document. No acceptance/payment side effect. |

Shipment {id,organizationId,facilityId,creatorAccountId,code,preparationId,
preparationVersion,bookingState:CONFIRMED,carrierState:NOT_ACCEPTED,input,quote,
totalVnd,createdAt,parcels:[{id,parcelNumber,facts}]}. Input/Parcel facts reuse
existing shared Shipping preparation contract. Code/Parcel IDs server-generated;
no codes from imported source references. Source channel/reference are bound in
the immutable preparation/booking snapshot, not editable confirmed Shipment data.

Obligation {id,organizationId,facilityId,shipmentId,quoteId,amountVnd,currency:VND,
state:UNPAID,payer:SENDER,method:CASH_ON_HANDOVER,
policyVersion:BR13-BUSINESS-LOCAL-01,createdAt}. Amount from consumed Quote only.
No runtime path to PAID, COD, refunds or fabricated collection evidence.

## Data — forward migration only, shared Shipping master

Do not edit V1–V25 or create a second confirmed Shipment/Parcel master. Extend
existing `pricing.pilot_quote`, `orders.pilot_shipment` and
`finance.pilot_shipping_obligation` with explicit owner_type PERSONAL/BUSINESS
and organization/facility/creator/preparation bindings. Personal owner_id and
draft_id remain exact Personal-account/Draft references; they become null only
for BUSINESS rows. Never put an Organization UUID into accountId/owner_id.

For BUSINESS add organization_id, facility_id nullable, creator_account_id,
organization_preparation_id/version and quote contract_id/version. CHECKs enforce
mutually exclusive owner variants. Composite same-org/facility and immutable
preparation-version/Quote/Shipment bindings at DB level; all historic Personal
FKs, constraints and responses retain their behavior. Existing Personal queries
explicitly restrict PERSONAL to avoid disclosing Business rows to their creator.
`orders.pilot_parcel` remains the single Parcel table with current immutable facts.

New Pricing-owned `pricing.organization_pilot_contract`:
(id,version) PK; organization_id FK; tariff_version FK; state ACTIVE/SUSPENDED;
effective_from/to; approval_ref; operator principal/time. Immutable terms; versioned
supersession/suspension. Only audited Local/Test operator configuration for an
explicitly approved Organization; no runtime auto-provision or all-org seed.

New purpose-bound `orders.organization_booking_receipt` and Pricing quote receipt
store organization/actor/facility/operation/key, request HMAC/key ID, target,
expected/result versions and committed snapshot/time; append-only business audit
contains no raw contact/token. Preparation accepts BOOKED via an additive current-
version/receipt/audit amendment; no direct edit/archive after confirmed Booking.

Preserve Personal DTOs and endpoints unchanged. Shared pricing arithmetic,
Shipping validation, codes, label renderer and transactional workflow remain
reused via public module APIs. No cross-module internal imports or table writes.
Merchant scope lock -> preparation/source -> contract/coverage/tariff -> Quote ->
Shipping/Finance final checks in one physical transaction; failed evidence rolls
back every effect. Actor revocation/contract suspension races must have one
authorized outcome, not a UI-only guard.

## Required proof before activation

Populated upgrade preserves V1–V25 checksums and existing rows/receipts; exact table
inventory updated; Personal regression; org/facility/actor isolation, revoked
membership/grant/contract denial; unsupported routes/limits, expired/stale Quote;
single-use/concurrent Booking, HMAC/idempotency/unknown-write recovery; atomic
audit/Finance rollback; private PDF and no PAID/Accepted; explicit Production
denial; real HTTPS/PG18 Frontend → Quote → Booking → detail/label Browser journey.

This amendment covers the confirmed shipping gap in Create Shipment. Organization
contacts, facility/default/template masters and internal packing remain the
separate accepted preparation tasks, not implicitly completed by Booking.
