# Shipping Policy V1 — Owner Decision Packet

Status: **PROPOSED / NOT ACTIVE**. Authority: Owner directive attachment
cb874be9-2dc4-4108-8fbc-e5dc8365db69, 2026-11-01.
Baseline: develop fecd646fcf2e3ee9fae0d2bc598cc57623e1d95f (PR37 merged).

Implementation is authorized; the directive expressly withholds approval of the
candidate commercial/operational values. Nothing below is a tariff publication,
carrier capability, confirmed address or acceptance of real shipments.

## Repository findings and reuse

Identity/current CustomerAdmission, profile, PW03/V18, HTTPS/CSRF/session lifecycle,
private storage and email adapters exist. Reuse them. orders.demo_shipment is an
isolated legacy demo contract and must remain unchanged. There is no deployed
production service/tariff/coverage/Station/Pickup/Tracking/Finance/Support authority.
No administrative reference dataset/version/provenance or operational pilot route
is present. Free-text PW03 addresses and Merchant application snapshots are not
such a dataset. An empty source must not be advertised as national coverage.

## Decisions requiring Owner response

| ID | Proposal, expressly not approved | Missing evidence / effect until decided |
| --- | --- | --- |
| SPV1-SERVICE | ROAD_STANDARD, Vietnam domestic, multiple parcels; maximum actual and chargeable weight 20kg per parcel | Parcel count, per-dimension/girth limits, supported commodities, exclusions and dangerous/restricted goods need approval. No service activated by marketing |
| SPV1-COVERAGE | Start with one controlled origin/destination area and one actual handover location, then expand only by version | Repository cannot justify a named operational pilot. Owner supplies approved administrative reference/version/provenance, exact area IDs, route matrix, real station/pickup capabilities/hours. Alternative: explicitly approved simulation-only route data, never represented as operating coverage |
| SPV1-PRICE | Per parcel: intraregion25000VND first500g +3000 per extra500g; interregion38000 +5000. Dimensions in cm; volumetric kg=L×W×H/5000; billable=max(actual,volumetric), ceil500g. Shipment fee=sum parcel fees. No floating-point money | Approve aggregation, region mapping, dimensions, goods and fees/tax treatment. Proposed simulation has no discounts, pickup fee or surcharges; must explicitly reject unsupported services/handling rather than guess charges. No tax invoice generated |
| SPV1-QUOTE |15 minutes, UTC effective intervals, immutable published versions, exact input/breakdown snapshot | Quote unavailable until service+coverage+tariff+payment decisions and golden cases approved; expired quote cannot confirm |
| SPV1-PAYMENT | Sender owes shipping fee; cash collected only by authorized Carrier at handover; booking is not PAID or Carrier Accepted; COD/online payment disabled | **Controlled amendment required:** BR13 and state-machine T1 currently require successful fee charge at confirmation. Owner must approve a bounded Personal cash-on-handover exception with separate payable/payment state, or retain prepaid confirmation and defer booking until Finance funding exists. No implicit exception |
| SPV1-CHANGE | Draft edit/archive only, no fee/payment/custody effect; booked cancellation/change/refund follows the accepted source lifecycle | Booked cancellation charge/refund, pickup window/reschedule, labels and carrier actions remain closed without their authority and policy |

## Independent implementation boundary

Owner authorized create/resume/edit/archive drafts and real self-scoped list/detail.
Persist only preparation data, never CONFIRMED/ACCEPTED/DELIVERED/payment/quote.
Draft snapshots are customer-entered UNVERIFIED contacts; changing PW03 later does
not silently rewrite saved drafts. Multi-parcel measurements are preparation
facts; passing input validation never implies a service accepts those parcels.
Business owner type is reserved, not enabled without current membership provider.
Server derives personal owner; writes are versioned/idempotent/audited, fail closed
on unknown outcomes. No monetary, service-weight or geographic rules are activated
while the decisions above are pending.

## Acceptance before activation

Signed-off golden price cases at500g/501g, dimensional boundaries and multi-parcel
aggregation; effective/expired version checks; supported/unsupported route cases;
real carrier capability evidence; explicit BR13 amendment or funded-charge gate.
Real PG atomicity/uniqueness/ownership/races/replay/audit rollback; immutable address
and accepted quote snapshots; browser deny/recovery; required CI on exact revision.
No PAID/COD/custody/delivery event from Customer input; no official label from draft.

Without those decisions, deliver and verify independent capabilities, mark affected
modules BLOCKED and keep quote/booking disabled on both Backend and Frontend.

## Subsequent Owner response — controlled pilot approved in principle

Owner permits Local/Test simulation, not commercial operation or immediate policy
activation. Candidate details: ROAD_STANDARD, at most5 parcels, actual20kg/parcel,
each dimension100cm, L+2(W+H)<=200cm, per-parcel500g rounding and15m TTL; proposed
fees/formula above retained. COD disabled; no unapproved surcharges, special/dangerous
goods or real tax invoice. These are candidate values pending complete confirmation.
The inactive calculator takes explicit INTRA/INTER classification for golden tests;
it never infers real regions or issues quotes. The20kg maximum is tested as actual
weight; Owner must resolve any additional chargeable-weight ceiling before activation.

| Final activation decision | Current status | Exact prerequisite |
| --- | --- | --- |
| Pilot administrative dataset/areas/routes | BLOCKED | Approve exact version/provenance/IDs and simulation labels; none in repo. Free-text contacts never prove eligibility |
| Origin/destination price regions | BLOCKED | Dataset-backed region assignments and allowed direction pairs; no nationwide default |
| Handover/Pickup/Station | BLOCKED | Explicit Local/Test capability and authorized operational actor/evidence contract; no invented real facility |
| Tax/fees/rounding | CANDIDATE | Confirm whether candidate base fees are final simulation charges, any tax-inclusive/exempt presentation, approved pickup/other fees and integer rounding; no invoice assumption |
| Goods and weight ceilings | CANDIDATE | Approve allowed ordinary goods classification, refusal/handling rules and actual versus chargeable20kg ceiling |
| BR13/T1 amendment | CONFLICT / PENDING | Approve explicit Personal LOCAL_PILOT cash-on-handover exception to successful fee-charge-at-confirmation. Booking must leave an unpaid payable and no Carrier Accepted event; canonical prepaid/WMS unchanged |
| Collection authority | BLOCKED | Carrier actor/permission/assignment plus immutable evidence and Finance source contract. No collector implemented or auto-PAID |

The new Owner response explicitly says to continue independent drafts and not mark
Quote/Booking ACTIVE while any of these prerequisites is missing. No canonical
BR13 text, transition table, financial ledger or membership contract is amended
by this packet or by the independent preparation implementation.
