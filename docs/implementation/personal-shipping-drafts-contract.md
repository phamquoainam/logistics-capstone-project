# Personal shipping preparation — bounded implementation contract

Authority: Owner shipping directive and subsequent pilot decision, 2026-11-01.
Initial preparation checkpoint; subsequently the Owner approved the bounded Local/Test pilot in [BR13-LOCAL-PILOT-01](../decisions/br13-local-pilot-01.md). The separate [pilot booking contract](personal-pilot-booking-contract.md) governs Quote/Booking; draft syntax itself remains preparation.
Maps REQ-AB-SHIPPING-FACTS, BC139/140/160/181 and BR01/02/12/19, specs006/007.

Orders owns one shared shipping preparation aggregate. PERSONAL ownership is
derived by workflow from current CustomerAdmission. Business ownership is not
enabled; the API never accepts account/merchant/role/state/price/payment fields.
Orders uses a server-only Access capability delegating current admission checks,
not an Identity dependency or request-bound actor. Every operation checks it,
joins the same datasource transaction and rechecks before return/commit.

GET/POST `/api/v1/customer/shipment-drafts`; GET/PUT `/{id}`; POST `/{id}/archive`;
GET `/api/v1/customer/shipment-draft-receipts/{operationId}/{key}`. Existing
HTTPS/Bearer/Origin/CSRF applies. Writes require UUID Idempotency-Key; edit/archive
require If-Match `"vN"`. Responses include id/ownerType=PERSONAL/accountId,
state=DRAFT/ARCHIVED, input/version/createdAt/updatedAt, ETag. Archive204;
missing/foreign/archived reads404. Missing version428/stale409/changed-key409;
validation400/session401/unavailable503. Receipt contains metadata only.
List page>=0/size20, optional search<=160; updatedAt DESC/id ASC, owner/filter
before paging. No browser retry of an unknown write; lookup the scoped receipt.

Input: optional reference1..80 and instructions1..1000, required sender/recipient
contact snapshots `{name,phone,address:{country:VN,administrativeArea,locality,
addressLine}}`, required parcels1..5 with `{description,quantity,weightGrams,
lengthMm,widthMm,heightMm,declaredValueVnd}`. Contacts are unverified, phone0/+84
canonicalized, name/description<=160, area/locality<=160,line<=500. Quantity1..1e6,
weightGrams1..1e9, dimensionsMm1..1e6, valueVnd0..1e12 are integer serialization/
storage safety bounds, **not accepted service limits**. Approved commercial-style20kg/100cm/girth simulation limits and ordinary-goods
attestation are enforced at the separate pilot quote/booking boundary; draft syntax
alone still does not confer eligibility.
No floating-point monetary data. No COD field/service/pickup/quote in draft input.
Passing draft validation does not confirm restricted goods or service eligibility.

Forward V19 only: orders.shipping_draft current metadata plus immutable
shipping_draft_version JSONB preparation snapshot, shipping_draft_audit and
shipping_draft_receipt. JSON snapshots include parcel facts; no operational Parcel
identity/label is issued before booking. Deferred current-history FK binds owner,
version and state. Check constraints validate full snapshot shape/limits; trigger
prevents owner/time/history/state changes outside versioned archive/edit. Runtime
has no delete or history/audit/receipt update. Audit/receipt failure rolls back all.
Retained-key HMAC binds owner/operation/key payload/original version; source IDs
are server-generated. No key rotation or new credentials: reuse configured
retained key material with a distinct orders shipping-draft purpose namespace.

UI integrates existing shell/PW03; snapshots copied into a draft never change
implicitly when source contact changes. No official shipment code, quote, label,
carrier acceptance/delivery, payment, tracking or statistics fabricated. Drafts
have a separate list; do not label them accepted shipments. Preserve unknown
write input/key in memory; no PII/token storage or public logs.

Verify real PG migration/upgrade/privilege/constraint/immutable owner, self isolation,
replay/stale/archive/races/audit rollback, actual HTTPS browser save/reload/edit/
archive and receipt recovery, existing required security/architecture/CI gates.

## Owner-directed domestic entry presentation — 2026-11-03

The Owner's ViettelPost screen reference authorizes a domestic order-entry UX
refresh using these existing APIs, not new commercial policies or request fields.
Contacts occupy the left column; physical parcels, payer information and notes
the right. Shared ArgoPost Navy/Orange styles and a sticky action summary apply.

- UI dimensions use centimetres with one decimal place; convert exactly into the
  existing integer millimetre fields. Backend contracts, stored facts and service
  limits are unchanged. Values with unsupported precision are not silently rounded.
- Aggregate physical Parcel weight/value without multiplying by contents quantity.
  Missing measurements render absent, not as a valid zero or free shipping quote.
- Save draft retains the existing detail route. Continue saves via the same
  ETag/idempotent API, then reviews the authoritative returned snapshot before
  reusing the existing Quote/Booking panel. An unchanged saved draft needs no extra
  write. Returning to edit discards the displayed quote; edited facts require a
  new saved version and quote. No auto-booking or client-calculated fee is added.
- Unknown save retains intent/key/input; resolve the scoped receipt, then read its
  target before entering review. A newer version opens authoritative detail rather
  than quoting it as the original lost snapshot. Unknown Quote/Booking locks the
  edit/back action; scoped receipt recovery remains mandatory.
- New/edit draft query identities isolate form state. Reset only clears unsaved
  local form changes with confirmation; it never deletes or resets database data.
- Free-text contacts never prove coverage. Use the separate approved SIM route
  selection; no guessed VN administrative dropdown or phone-based customer lookup.
  No unapproved terms checkbox, pickup appointment, COD, recipient payer, promotion,
  Smart Box, international/instant service or Excel mutation is introduced.

Backend eligibility, quote TTL/consumption, immutable Shipment/Parcel/UNPAID,
ownership and Local/Test isolation remain governed by the existing booking contract.


## Owner Create Shipment audit refinement — 2026-11-09

Presentation on existing accepted operations only. New physical Parcels have no
assumed weight/dimensions; Customer measures the packed Parcel. Quantity defaults1
and declared value0 remain explicit facts, never multiply physical weight/value.
An explicit default-contact action traverses current self-owned address pages,
locks entry while copying and preserves current values on no default/failure.
It never auto-fills after user edits. Saved contact snapshots stay independent.
Review shows every saved Parcel and reference/instructions before Quote.
Catalog-driven per-Parcel limit messages are client guidance; Backend is always
mandatory authority. No new field/table/price or capability grant.

[Observed outcome and boundaries](create-shipment-review.md).

## Owner estimator handoff —2026-11-09

The dedicated Customer estimator can populate only measured Parcel facts and a
preferred route in a NEW draft through transient navigation state. No contacts,
money or issued Quote is accepted from this context. Reject invalid/unknown fields
and any attempt to combine preparation with an existing draft/address selection.
The raw contact/goods completion, explicit save, backend-normalized response,
current catalog read, fresh Quote and Booking checks remain mandatory. No write
occurs on navigation alone. No change to this Draft API/schema/state machine.
[Tools contract/evidence](lookup-label-review.md).
