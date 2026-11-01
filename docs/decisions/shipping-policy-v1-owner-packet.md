# Shipping Policy V1 — Owner Decision Packet

Status: **APPROVED FOR BOUNDED LOCAL/TEST PILOT; technical activation gated**. Latest authority:
Owner activation decision e5ac3e8a-e10f-43ef-9745-30a9a48b92ab, 2026-11-01.
Earlier proposals and responses below are preserved as historical checkpoints.
Original authority: Owner directive attachment
cb874be9-2dc4-4108-8fbc-e5dc8365db69, 2026-11-01.

**Current final disposition:** Owner subsequently explicitly approved
ARGO-SIM-COVERAGE-1 (exact proposed3 synthetic areas,4 directed routes and
SIM-DROP drop-off only) and [BR13-LOCAL-PILOT-01](br13-local-pilot-01.md).
All required business values for the bounded LOCAL/TEST pilot are now approved;
activation is still conditional on implementation/verification, not more approval.
Production/Business/collection/Pickup/Carrier events remain excluded. Historical
PENDING/PROPOSED descriptions below describe their earlier checkpoint only.
Baseline: develop fecd646fcf2e3ee9fae0d2bc598cc57623e1d95f (PR37 merged).

The final Owner response approves the exact simulation values and bounded unpaid
booking exception below. It does not approve commercial operation, real carrier
capability, verified addresses, collection or acceptance of real shipments.

## Current activation matrix — final Owner disposition

| Dependency | Approved scope / technical gate |
| --- | --- |
| Service / tariff / tax | ROAD_STANDARD;1..5 parcels;20kg actual/parcel; each dimension<=100cm; girth<=200cm; divisor5000; per-parcel ceil500g; INTRA25000+3000, INTER38000+5000;15m TTL; simulation fee only, no invoice or extra charge |
| Coverage | ARGO-SIM-COVERAGE-1 / ARGO_SIMULATED_ADMIN_1; SIM-A1/A2 in region A, SIM-B1 in B. Exactly A1→A2,A2→A1 INTRA and A1→B1,B1→A1 INTER; no inferred/free-text/national coverage |
| Handover | SIM-DROP-A1/A2/B1 are simulated drop-off points only. Pickup unavailable; no real Station or handover fact created |
| BR13 | BR13-LOCAL-PILOT-01 ACCEPTED within verified PERSONAL Local/Test. Atomic CONFIRMED booking creates immutable UNPAID obligation, not PAID or Carrier Acceptance |
| Quote gate | Explicit LOCAL/TEST plus Customer SANDBOX rollout, non-production profiles, approved effective dataset/tariff, actual owned draft/measurements and ordinary-goods declaration; required positive/negative tests |
| Booking gate | Quote gate plus current owner/draft version, unexpired unconsumed quote, exact snapshot binding and one-transaction Shipment/Parcel/receipt/audit/UNPAID persistence; races/rollback/replay tests |
| Collection / Carrier / Production / Merchant | BLOCKED / excluded. No runtime collection or PAID permission; no Carrier Accepted/Delivered, COD, operational pickup or commercial document |
| Evidence / PR38 | See [delivery tracker](../implementation/shipping-personal-v1.md). PR38 remains an unmerged integration proposal; exact published source/integration SHA and hosted checks belong to its PR metadata |

V20 records approval effective time2026-11-01T15:20:48Z, immutable synthetic master
rows and tariff ARGO-ROAD-TARIFF-1. Runtime defaults DISABLED; the existing local
launcher explicitly selects LOCAL. No additional business approval is required
for implementation within these limits.

## Historical decision record — superseded statuses below

Earlier PENDING/CANDIDATE/NOT ACCEPTED labels are retained to explain the sequence;
the current matrix and bounded amendment above control the approved pilot.

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

## D1–D7 activation decision — current authority

| Decision | Actual approval | Implementation effect |
| --- | --- | --- |
| D1 service | APPROVED FOR LOCAL/CAPSTONE PILOT | ROAD_STANDARD only;1..5 parcels;20kg actual/parcel; each dimension<=100cm, L+2(W+H)<=200cm. Prohibited/dangerous/perishable/unsupported special handling rejected. Draft syntax remains preparation; quote applies service rules |
| D3 pricing | APPROVED FOR PILOT, canonical BR10–12 compatible | Per-parcel max(actual,volumetric), ceil500g; divisor5000 with cm/kg;25000+3000 or38000+5000 per additional500g; sum parcels; no extra surcharge;15m TTL, immutable version/input/breakdown |
| D4 tax/documents | APPROVED SIMULATION ONLY | Display SIMULATED_SHIPPING_FEE, never VAT0%, payment receipt or real tax invoice. BR10–12 do not mandate inventing tax data. No tax-policy approval request repeated |
| D2 coverage | CONDITIONAL / DATASET NOT APPROVED | Need exact dataset/administrative identifiers/directed routes/service eligibility/effective dates and explicitly simulated capability before use. No national fallback |
| D5 payment | REQUESTED LIMITED EXCEPTION, not yet an accepted BR13 amendment | BR13/T1 still require successful fee charge before confirmation. A preference for cash does not authorize bypass. Collection has no runtime authority |
| D6 gates | APPROVED | Quote readiness independent of BR13/collection; booking needs quote readiness plus the explicit booking/payment exception and verified implementation |
| PR38 | OPEN at1eeb8c3, not integrated into develop | Continue scoped incremental work on the same branch/PR; do not auto-merge or claim merged baseline |

### Concrete coverage table for Owner disposition — PROPOSED, no runtime seed

Dataset `ARGO-SIM-COVERAGE-1`, administrative reference namespace
`ARGO_SIMULATED_ADMIN_1`. These are **synthetic identifiers**, not official VN
administrative codes and not actual service coverage. Labels must say mô phỏng.
They cannot be matched from free-text province/ward or reused as real postal codes.
The alternative is Owner-provided official identifiers/dataset; no guessed mapping.

| Administrative identifier | Region | Explicit simulated handover capability |
| --- | --- | --- |
| SIM-A1 | SIM-REGION-A | SIM-DROP-A1, drop-off simulation only |
| SIM-A2 | SIM-REGION-A | SIM-DROP-A2, drop-off simulation only |
| SIM-B1 | SIM-REGION-B | SIM-DROP-B1, drop-off simulation only |

Proposed ROAD_STANDARD directed route allowlist: SIM-A1→SIM-A2 and SIM-A2→SIM-A1
INTRA; SIM-A1→SIM-B1 and SIM-B1→SIM-A1 INTER. All other pairs unsupported, including
self routes. Pickup unsupported. Effective start would be the explicitly recorded
Owner approval time; no end (until a new version closes it). Environments LOCAL/TEST
only, no real address geocoding or inferred geographic coverage. No endpoint may
claim an actual Station exists or that handover occurred.

### Concrete BR13 amendment for Owner disposition — NOT ACCEPTED YET

Proposed `BR13-LOCAL-PILOT-01`: Only verified PERSONAL Customer, ROAD_STANDARD,
approved Local/Test simulation route, valid unexpired quote and no WMS/COD may
confirm with sender CASH_ON_HANDOVER. Confirmation atomically records immutable
Shipment/Parcel/address/quote snapshots and an UNPAID shipping-fee obligation;
it does not require a successful collection event at this stage. It creates no
PAID state, payment receipt, financial posting or Carrier Accepted/Delivered event.
Canonical prepaid/WMS/Business behavior and nonnegative-wallet/ledger invariants
remain unchanged. No runtime actor currently has collection authority; collection
stays disabled until a separately authorized Carrier/Finance evidence contract.

Explicitly accepting this text would permit **unpaid pilot booking while collection
remains BLOCKED**. Without acceptance, booking remains blocked by BR13; quote can
proceed once D2 and its own implementation/verification gates are satisfied.
