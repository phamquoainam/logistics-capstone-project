# Business shipment creation V1 — execution contract

Status: OWNER ACCEPTED (preparation contract and bounded Merchant pilot direction). Date: 2026-11-10.
Owner decisions: “Duyệt contract chuẩn bị Business” and “Duyệt hướng pilot Business này”.
The proposed filename is retained as the decision's original reference. Preparation
API/data/permissions below are accepted; the confirmed Shipment/financial wire
amendment described below remains a separate implementation dependency.
Owner requested an orange Business theme and full Business creation capabilities.
Existing Business access remains Local/Test only. Personal migrations/records and
the existing provisioned Organization must be preserved; V1–V23 are immutable.
Source checkpoint: `.demo/business-shipping-review/before-source` (104 paths).

## Tasks / smallest first deliverable

- [x] BSC-01 Inventory and preserve current source; inspect DoR and accepted policies.
- [x] BSC-02 Orange Business theme; retain Personal navy; responsive/contrast tests.
- [x] BSC-03 Owner accepts exact preparation API/data/permission contract below.
- [x] BSC-04 Real organization-owned manual Draft → PostgreSQL → reload/edit/archive.
- [ ] BSC-05 CSV validation/preview → explicit row confirmation → idempotent Drafts;
  real row outcomes, templates and preparation transitions; no automatic Booking.
- [x] BSC-05a CSV V1 template, bounded preview, persisted row outcomes, explicit
  chunk commit, source-key dedup and unknown-write recovery through real HTTP/PG.
- [ ] BSC-05b Merchant facility CRUD, defaults and reusable preparation templates.
- [ ] BSC-05c Version-bound internal packing transitions and handover preparation.
- [ ] BSC-06 Approved Merchant Quote → Booking/Parcel/UNPAID → labels; only after
  the explicit Merchant pricing/coverage/payment amendment below is accepted.
- [ ] BSC-07 Affected ownership/RBAC/revocation/concurrency/PG18/FE/Browser gates,
  source manifest/privacy/report validation, traceability and honest capability map.

Theme can proceed now. New runtime paths/DTOs/tables/permission values require
approval under root AGENTS.md. No independent security architecture redesign.

### Observed theme checkpoint

`BusinessWorkspace.tsx` adds a Business-only root class; `business-theme.css`
scopes orange primary #FF7A00, warm neutral surfaces, active navigation/filter,
buttons, header/avatar, borders and emphasis. Personal's shared navy tokens and
authorization are unchanged. Dark ink on orange retains normal-text contrast.

Test-first theme assertion initially failed against navy, then passed after patch.
Affected Business/switcher UI14/14 PASS, including returning to Personal navy,
contrast>=4.5:1 and360/768/1440px layouts. Lint PASS; Customer and default builds
with TypeScript checking PASS. Synthetic desktop/mobile artifacts inspected under
`.demo/business-shipping-review`. Only source/render styling changed; no migration,
operator grant, account creation, service/pricing/financial mutation or false Draft/
Booking success. Existing V23 source fingerprint remains unchanged.

BSC-03 and the bounded Merchant policy direction were explicitly accepted by Owner.
BSC-04 onward remain unchecked until their actual implementation gates pass.

## Common Shipping ownership and preparation [OWNER ACCEPTED]

Reuse Orders ShippingPreparation validation, ShippingDrafts contact/parcel models,
existing fingerprint/ETag/receipt patterns, pricing arithmetic and label renderer.
No second authentication/Shipping Domain and no Personal data import/reassignment.
Do not impersonate an account by passing Organization UUID as accountId.

Expose explicit Orders-owned organization preparation API alongside the unchanged
Personal API. A server-created scope has actorAccountId, organizationId and current
Merchant admission; payloads never select actor/owner or supply trusted permissions.
All Orders queries/mutations bind organizationId AND permitted facility scope.
Current Identity → Merchant → facility → preparation/business key lock ordering.
State-changing commands recheck current grant, account eligibility, membership,
org status and facility binding at the final locked decision. Audit/receipt failure
rolls back. No runtime seeding of accounts/orgs/facilities/rates/Carrier assignments.

New permissions (explicit grants, no role-label allow):
`shipment.draft.read`, `shipment.draft.write`, `shipment.import`,
`shipment.template.manage`, `shipment.prepare`, `shipment.quote`,
`shipment.book`, `shipment.label.read`, `facility.manage`, `defaults.manage`.
Read is required for dependent preparation rights; quote/book/label require the
corresponding workflow and pricing authority as well. No COD/Finance/Carrier grants.
Existing Dashboard grants do not gain write rights. NamShop Demo's confirmed Owner
may receive these accepted local preparation rights by an audited explicit command;
no rights or memberships for other actual accounts without a specified target.

Facility scope: ORGANIZATION or explicit FACILITY IDs of the same Organization.
Extend member_grant only through a new forward migration (never edit V23); scoped
grant validity has DB same-org constraints. Org read rights cannot be inferred from
one facility grant. Org-wide operations require org-wide permission; nonmatching
facility/data/receipt/template/row access gives404, not empty success/widened totals.

## Data contract — forward migration group [OWNER ACCEPTED]

Allocate actual V24+ after inventory; no old migration repair/update. Reuse shared
validation function and immutable snapshots. Tables below are preparation records,
not a parallel confirmed Shipment/Parcel/Tracking/Finance master.

| Table | Bounded fields / invariants |
| --- | --- |
| merchant.facility | id UUID; organization_id FK; name≤160; contact+address using existing ShippingDrafts.Contact; state ACTIVE/ARCHIVED; version; timestamps. Composite(id,org). User-entered Merchant site, never a Carrier Station, proof of coverage or pickup capability. |
| merchant.organization_shipping_defaults | organization_id PK; facility_id optional same-org; serviceCode optional; handover DROP_OFF/PICKUP; payer SENDER/RECIPIENT; codRequested bool; version/timestamps. Only supported accepted catalog values can be selected as effective defaults; unsupported options cannot become a silent Booking fallback. |
| orders.organization_preparation | id UUID; organization_id; facility_id optional; creator_account_id immutable; state DRAFT/ARCHIVED/BOOKED; preparation_state DRAFT/READY_TO_PACK/PACKED/READY_TO_HANDOVER; version; source_channel≤40 optional; source_order_reference≤80 optional; existing normalized input JSON; timestamps. No Shipment/Parcel codes before Booking. Composite(id,org); immutable owner/creator/source-key binding. |
| orders.organization_preparation_version | (id,version) PK; organization_id; immutable payload/state/preparation snapshot and recorded_at; deferred current-version FK. Per-org live source-channel+source-reference unique binding prevents duplicate ingestion. |
| orders.organization_preparation_template | id UUID; org/facility scope; name≤120; version; reusable sender/default service/parcel configuration JSON; ACTIVE/ARCHIVED; timestamps. Recipient PII not automatically copied into a reusable template. Source refs revalidated, never trusted stale IDs. |
| orders.organization_import_job | id UUID; org/facility; creator; templateVersion; request fingerprint; state PREVIEW/COMMITTING/COMPLETED/PARTIAL/ARCHIVED; version; totals; timestamps. Max100 rows /256KiB CSV per V1 job. PostgreSQL source of progress, no frontend fake timer. |
| orders.organization_import_row | (job_id,row_number) PK; org; normalized proposed input; row fingerprint; source key; VALID/INVALID/CREATED/CONFLICT state; bounded field-error codes; resulting preparation_id optional; version. CREATED immutable result; no automatic Quote/Booking/fees. |
| orders.organization_preparation_receipt | (org,operation,key) unique; actor/scope binding, request HMAC/key ID, target, original/result version, committed outcome/time. Purpose separation from existing Personal receipts; unknown write resolved by scoped GET receipt. |
| orders.organization_preparation_audit | id UUID; org/actor/target/operation; old/new version/state; request; time; append-only. No raw contact/document/token in audit. Same transaction as each effect/receipt. |

Organizations remain Merchant-owned, preparation Orders-owned. Cross-module
references validated through public APIs in the shared physical transaction, not
cross-module implementation imports or another module's direct table DML.
Extend the existing audited operator grant procedure in a new migration; preserve
its old signatures/receipt compatibility and current memberships. No runtime self-grant.

## Exact preparation HTTP / DTO [OWNER ACCEPTED]

All routes below use prefix `/api/v1/merchant-organizations/{organizationId}`,
existing Customer TLS/Bearer/Origin/CSRF guard plus current Merchant RBAC. no-store.
GET pagination page≥0,size20; duplicate/unknown query/body fields rejected.
POST/PUT require UUID Idempotency-Key; update/archive/transition If-Match `"vN"`;
same key+body replay returns original result, key/body mismatch409, stale version409,
unknown foreign target404, missing auth401, unavailable capability503/422.

| Routes | Request/response |
| --- | --- |
| GET/POST `/facilities`; GET/PUT `/facilities/{id}`; POST `/{id}/archive` | Contact/address and name; no operational Carrier fields. Versioned Facility {id,organizationId,name,contact,state,version,createdAt,updatedAt}. Referenced active sites cannot be archived without explicit disposition. |
| GET/PUT `/shipping-defaults` | Versioned defaults above plus server-derived supportedCapabilities; no invented pricing/capability flags from caller. |
| GET/POST `/shipment-preparations`; GET/PUT `/shipment-preparations/{id}`; POST `/{id}/archive` | Input {facilityId?,sourceChannel?,sourceOrderReference?,reference?,instructions?,sender,recipient,parcels}. Response {id,organizationId,facilityId,creatorAccountId,state,preparationState,version,input,sourceChannel,sourceOrderReference,createdAt,updatedAt,allowedActions}. Personal accountId is not owner. |
| POST `/shipment-preparations/{id}/transitions` | {action:READY_TO_PACK/PACK/READY_TO_HANDOVER/REOPEN,expectedParcelVersion}; preparation-only. READY_TO_PACK requires validated current content; PACK requires exact Parcel count and validated snapshot; later edits invalidate packing state. No Carrier Accepted event. |
| GET/POST `/shipment-templates`; GET/PUT `/shipment-templates/{id}`; POST `/{id}/archive` | {name,facilityId?,sender?,serviceCode?,parcels?}; normalized template snapshot, version/state/current allowedActions. Applying creates/edits a Draft under fresh validation, not Booking. |
| GET `/shipment-import-template` (query `format=csv`) | Supported UTF-8 CSV template and column/unit instructions, no real PII. |
| POST `/shipment-imports` | JSON {templateVersion:BUSINESS_CSV_V1,facilityId?,csv}; bounded UTF-8 text, exact headers, RFC4180 quoting, no executable formula fields. Returns versioned preview job and per-row field errors; no Draft effects. |
| GET `/shipment-imports`; GET `/shipment-imports/{id}` | Current server job totals and paginated row outcomes. No fabricated asynchronous progress or client-derived counts. |
| POST `/shipment-imports/{id}/commit` | {rows:[rowNumber,...]} after preview; only explicitly selected VALID rows; max20 rows/transaction. Each creates through the same preparation command, unique source key and deterministic job/row receipt. Retry does not recreate successful rows. Conflicting rows retain stable error/result; failed atomic transaction rolls back every effect in that chunk. |
| GET `/shipping-preparation-receipts/{operation}/{key}` | {operationId,targetId,outcome:COMMITTED,version}; current org/facility/permission before disclosure, no raw body/HMAC. |

CSV V1 represents one Shipment per row with parcels as a bounded JSON cell; avoids
silently treating repeated source-reference rows as different Shipments. Columns:
source_channel,source_order_reference,sender_name,sender_phone,sender_country,
sender_administrative_area,sender_locality,sender_address_line,recipient_name,
recipient_phone,recipient_country,recipient_administrative_area,recipient_locality,
recipient_address_line,parcels_json,reference,instructions. Parcel JSON uses the
existing description/quantity/weightGrams/lengthMm/widthMm/heightMm/declaredValueVnd.
Reject duplicate/missing headers, excess columns/rows, malformed quotes/JSON, control
characters, formulas and source-key duplicates. Error {rowNumber,field,code}; PII
rendered as text only and restricted to current authorized org. No arbitrary file
upload, public evidence storage, macros or third-party import SaaS.

XLSX is a later parser for an explicitly supported template, not a renamed CSV or
an enabled fake button. No external spreadsheet provider will be selected here.

## Merchant Quote/Booking policy direction [OWNER ACCEPTED]

BR-13 currently requires payment at Booking; the accepted UNPAID exception and
ROAD_STANDARD pricing/coverage use explicitly exclude Merchant. The new request
authorizes implementation, not invented Merchant commercial tariff/COD policy.

Proposed BUSINESS-LOCAL-SHIPPING-01: only existing Local/Test development orgs with
current explicit quote/book permissions, reuse precisely ARGO-SIM-COVERAGE-1's4
directed routes and simulated station drop-off; no Pickup. Create an explicit
versioned Pricing-owned contract per Organization using ARGO-ROAD-TARIFF-1 rules
(25,000/38,000VND first500g/Parcel, +3,000/+5,000 per next500g, max5 Parcels,
20kg/Parcel, max100cm side /200cm girth, divisor5000, round up500g, TTL15min,
accepted noncommercial tax simulation; no unapproved surcharge).

Quote must bind organization, current facility scope, preparation version, contract
version, coverage/service version and immutable per-Parcel fee lines. Contract
expiry/suspension/authority/route changes reject confirm; no silently repriced
Booking. Proposed BR13-BUSINESS-LOCAL-01 permits initial UNPAID obligation for this
bounded Merchant pilot only; no PAID/collection/COD/payment provider or Carrier
Acceptance. Carrier intake exception remains Personal-only until separately scoped.

Owner explicitly accepted this bounded extension on 2026-11-10. No real commercial
tariff, COD, collection, Pickup or Merchant Carrier intake is implied by that decision.
Do not store fake CONTRACT_ACTIVE or confirmed Shipment records from Drafts.

When accepted, next implementation contract must bind common confirmed Shipment/
Parcel/financial obligation persistence to explicit Organization owner, without
reinterpreting Personal accountId or altering applied migrations; provide actual
snapshot/label/query schemas for review before changing those wire contracts.

### BSC-04 observed checkpoint — 2026-11-10

V24 adds explicit scoped preparation grants, Merchant facility references and
Orders-owned preparation/current immutable versions, HMAC receipts and mandatory
audit. Existing Identity admission and Merchant lock are reused. New Customer
Business screens create, reload, edit and archive organization Drafts through real
HTTP APIs. A Draft never creates a Shipment, fee, payment or Carrier event.

Focused PG3/3 PASS (isolation, scoped rights, revocation, duplicate/source key,
concurrency, rollback and disabled/Production modes); populated V23 upgrade1/1
PASS; Business access regression5/5 PASS; architecture23/23 PASS. Real HTTPS/PG18/
test-SMTP Browser16/16 PASS, including lost Business create response resolved by
receipt, reload, edit and archive. Frontend unit9/9 and affected UI14/14 PASS;
typecheck, lint and both builds PASS. Report inventory3/3 and source validator PASS.
An initial Browser run used stale Customer assets; the following run found a missing
SPA route in the test host. Rebuilt assets and explicit new test-host routes fixed
these failures; final successful evidence is retained, not substituted with mocks.

Local V24 is now APPLIED/IMMUTABLE. All17 existing table fingerprints and V1–V23
checksums matched before any grant. The already confirmed NamShop Demo Owner then
received only draft.read/write via audited operator GRANT: no new Membership,
two grants, one audit/receipt; other13 table hashes unchanged. Local HTTPS frontend
and Backend READY; unauthenticated preparation GET401. Private evidence is under
`.demo/business-shipping-review`; no actual contact content is published.

Facility master CRUD, defaults/templates, CSV import and packing remain unfinished;
the optional facility reference currently supports backend scope enforcement only.
BSC-05/BSC-06/BSC-07 are not marked DONE. No hosted CI, commit, push or PR is claimed.

### BSC-05a observed checkpoint — 2026-11-10

CSV import is now implemented; the older BSC-04 remaining-work statement above
describes that earlier checkpoint. V25 adds Orders-owned import jobs/rows and
extends purpose-bound immutable receipts/audits with import targets, preserving
the preparation-version foreign keys for CREATE/UPDATE/ARCHIVE. No fake Draft is
used as an import job. Uploaded CSV text is parsed in memory, not retained as a
raw file. Only scoped normalized row proposals/error codes and committed outcomes
are persisted. CSV V1 requires source_channel and source_order_reference per row;
this deliberately prevents a second import job from duplicating the same order.

Preview max100 rows/256KiB, strict headers/quotes/JSON, shared contact/Parcel
validation and formula/control/duplicate rejection. Preview creates zero Drafts.
Commit explicitly selects up to20 VALID rows, requires If-Match/idempotency and
current import + draft.read/write permissions, and invokes the same Draft command.
Each row gets a deterministic command key. Known source conflicts become immutable
CONFLICT results; SQL/evidence/authorization failure rolls back the entire chunk.
The job counters, row results, Draft/history, audit and receipt are one transaction.
Success replay returns its original snapshot; receipt lookup is actor- and scope-
bound. Facility-limited import access never widens to organization-wide data.

Focused PG imports2/2 and manual Draft regression3/3 PASS; populated V24→V25
upgrade1/1 PASS; architecture23/23 PASS. Added scope/receipt isolation and explicit
import disabled/Production denial were rerun as focused individual cases: PASS.
Real HTTPS/PG18/test-SMTP Browser16/16 PASS now includes actual CSV download/upload,
VALID/INVALID preview, explicit commit, persistence/reload and lost responses at
both writes resolved without a second POST. PDF/Identity/Personal/UNPAID regression
journeys remain in that successful Browser execution. FE unit11/11, affected
Business UI11/11 and mobile refinements2/2 PASS; lint/typecheck/both builds PASS.
Report-policy130/130 PASS; source validator has errors[] and CONSISTENT ownership.
Visual artifacts use synthetic contacts only and remain ignored/private.

Local V25 is APPLIED/IMMUTABLE. Before granting import, all22 V24 table fingerprints
and V1–V24 checksums matched. The existing confirmed NamShop Demo Owner then gained
only shipment.import: one grant/audit/receipt, no new Membership, other18 table
hashes unchanged. HTTPS Frontend and Backend READY; unauthenticated import GET401.
No Quote/Booking, COD, collection, Pickup or Carrier permission has been granted.

Remaining: BSC-05b/BSC-05c, then the explicit confirmed Organization Shipment/
Parcel/contract-pricing/UNPAID/label wire amendment required by BSC-06. The bounded
Merchant pricing/payment direction is accepted; the remaining implementation
contract is not a request to reapprove those rates or business decisions.

## Capability completion / remaining authorities

| Requested capability | Concrete delivery / activation requirement |
| --- | --- |
| Manual organization creation | Real preparation CRUD first, then authorized Quote/Booking. Not labelled completed Shipment from a Draft. |
| CSV / validation / batch | V1 supported template, preview, explicit commit, per-row persisted results, source-key dedup and safe chunk retry. No automatic Booking. |
| Templates/defaults/source refs | Accepted scoped masters above; snapshots revalidated on apply; values cannot invent unsupported service/COD/payer. |
| Warehouse/pickup-point selection | Merchant Facility, scoped authorization; no inference of Carrier pickup capability. |
| Merchant contract prices | Explicit amendment/contract version above or actual Owner-supplied policy. |
| Internal preparation | Version-bound preparation state independent of Carrier state. No Accepted/Delivered/PAID effects. |
| Batch labels | Only confirmed org Shipment/Parcel IDs after accepted Booking/label contracts; preserve simulation identification and PDF privacy. |
| Multi-Shipment Pickup | Requires approved dispatch windows/capacity/assignments and Pickup contract; not created from saved Merchant site. |
| Handover checking | Packing validation is not custody. Parcel/code/label reconciliation only after actual confirmed Shipment. |
| API Booking | Requires accepted integration-principal/scopes/credential lifecycle/rate-limit contract. Never reuse a human session as a fabricated API Client. |
| COD/insurance/VAS/receiver payer | Still unavailable without accepted Merchant service/Finance authority; no implied collection/remittance from a form field. |

## Verification before marking DONE

PG18 forward upgrade with populated V23, all old checksums/data; org A/B and
Personal isolation, facility subset denial, stale/revoked membership, disable,
concurrent version/duplicate/source-key/import retry, receipt ownership, audit fault
rollback and runtime no operator grant authority. Import parser boundary/errors,
zero preview side effects, partial outcomes and safe CSV export. Real Browser
create/reload/edit/archive/template/import/preparation; appropriate unknown-write
recovery and session/context switching. FE typecheck/lint/build, focused unit/UI,
architecture/contract, affected PG/report/privacy/source gates. No mock success,
Production enablement, migrations reset, new worktree, commit/push/merge in this task.
