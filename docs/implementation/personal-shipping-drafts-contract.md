# Personal shipping preparation — bounded implementation contract

Authority: Owner shipping directive and subsequent pilot decision, 2026-11-01.
Implementation of independent drafts is authorized; policy activation is not.
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
storage safety bounds, **not accepted service limits**. Commercial20kg/100cm/girth
and supported commodities remain candidate service rules pending activation.
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
