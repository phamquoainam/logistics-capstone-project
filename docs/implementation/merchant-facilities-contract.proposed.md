# Merchant Warehouses & Pickup Points V1 [OWNER ACCEPTED]

Owner explicitly approved V1 in the conversation on 2026-11-16. The proposal
filename is retained for traceability; [PROPOSED] section markers below refer to
the exact amendment accepted by this decision, not an outstanding approval.

Date: 2026-11-16. Owner-directed feature on develop10a6a6c / merged PR47.
Branch: feature/argopost-merchant-facilities; same working directory, no worktree.
This amendment completes accepted BSC-05b facility master/defaults and bounded
Merchant Parcel preparation/handover lists (FR-M01/BR-19, BC-026). It does not
start SKU inventory, reservations, Carrier Station/Hub management or Dispatch.

## Tasks and order

- [x] WFP-01 Clean baseline, source/authority/DoR inventory; PR47 post-merge CI PASS.
- [x] WFP-01a Existing-report site activity UI (not a Facility master), exact cohort
  drill, scope/financial denial, mobile; targeted UI fixture gates documented.
- [x] WFP-02 Owner accepts only the new wire/schema/permission details below.
- [x] WFP-03 Real Facility CRUD/archive, metadata/hours, defaults and versioned audit.
- [x] WFP-04 Current member/site responsibility links, permission isolation and no self-grant.
- [x] WFP-05 Site selection/contact snapshot use in manual/import preparation; true
  scoped Shipment/work/report lists using accepted APIs and current source facts.
- [x] WFP-06 Confirmed-Parcel internal preparation and handover reconciliation.
- [x] WFP-07 PG18 upgrade/concurrency/security/FE/HTTP/Browser/report/source gates.
- [x] WFP-08 Owner separately approved Local deployment after verified source;
  V31/V32, six explicit rights, data preservation and HTTPS runtime verified.
  Owner confirmed directory/Add Site visible without permission/loading errors;
  this is manual Owner evidence, not automated live GUI evidence.
  WFP-02 source/test approval itself did not authorize this deployment.

Do not reapprove accepted tariff, SIM coverage, UNPAID, Identity or master CRUD
purpose. New paths/DTO/tables below need the root AGENTS.md exact-contract gate.
V1 remains LOCAL/TEST + Customer SANDBOX, default disabled, deny prod/production.
Preserve V1-V30 and all existing records, memberships and confirmed snapshots.

## Existing authority reused

BSC preparation contract already accepts Merchant-owned Facility name/contact,
ACTIVE/ARCHIVED/version/timestamps, /facilities CRUD/archive, facility.manage,
organization shipping defaults, scoped grants and preparation-only transitions.
OSM/MRF/OAA supply real facility filters, reports, contacts, internal assignment
and work. Reuse current Identity -> Merchant -> business resource transaction,
HMAC receipts, scoped ETag, validation, source parsers, private labels and UI.
No new service, map/storage/payment provider or second Shipping/Identity domain.

## Facility model amendment [PROPOSED]

Merchant owns site configuration, not Carrier infrastructure or stock balance.
A site is a Merchant WAREHOUSE, STORE, PICKUP_ADDRESS or SENDER_POINT.
These are classifications entered by Merchant; none proves pickup/route capability.

Facility input:
{name,code,kind,contact,senderContact:null|Contact,returnContact:null|Contact,
 operatingHours:null|{timeZone:Asia/Ho_Chi_Minh,days:[{day,intervals:[{opensAt,closesAt}]}]}}.
- name1..160; code1..40 ASCII letters/digits/hyphen/underscore, canonical uppercase,
  unique per Organization including archived; code immutable after creation.
- Contact reuses accepted ShippingDrafts.Contact name/phone/VN address, domestic
  phone normalization, NFC/trim/control/length checks; no verified Identity link.
- contact is the site representative and physical address. Optional sender/return
  contacts are explicit snapshots, not security-contact edits or a Return request.
- Hours: either unknown(null), or exactly seven unique weekdays;0..2 nonoverlapping
  same-day intervals each; HH:mm, close24:00 allowed, open<close. Empty intervals
  mean closed that day. They are Merchant opening hours, never Carrier pickup SLA.
- Existing facilities without new metadata remain readable with null code/kind/
  hours (configuration incomplete). Do not fabricate codes, hours or old history.
- Output retains accepted id/organizationId/name/contact/state/version/timestamps,
  adds the above metadata plus allowedActions. All PII is private/no-store.

Facility scope uses explicit facility.read and accepted facility.manage. Creating
sites needs organization-wide manage+read; update/archive can use matching finite
facility scope. Read is required for manage. Extend exact FACILITY permission
allowlists only. Defaults use existing organization-wide defaults.manage plus
current read on the selected site. No role-name bypass or automatic grants.

Default site: one versioned binding per Organization, nullable active same-Org
Facility. UI calls it the Merchant default sending/pickup address; selecting it
never creates a Pickup request. Changing it cannot rebind an existing Draft or
Booking. Sender snapshot copied only after explicit selection/current validation;
confirm before replacing unsaved edits. Contact changes do not alter booked data.

Archive must reject when the site is the default, has current scoped grants,
active internal responsibilities, unbooked active Drafts, unfinished preparation/
handover work or confirmed not-yet-handed-over Business Shipments. Return clear
conflict reasons without disclosing inaccessible records. Historical references
remain queryable; no cascading delete. No restore until a separate policy exists.

## Facility/staff HTTP [PROPOSED]

Prefix /api/v1/merchant-organizations/{organizationId}; existing TLS/Bearer and
Origin/CSRF guard. Writes UUID Idempotency-Key + If-Match vN, except create.
Strict/duplicate/unknown JSON/query rejection, page>=0,size20, no-store; ownership
and current rights before read/write/replay. Stale/conflicting key409; foreign404;
unsupported422/503; no automatic POST retry. Actor/purpose-bound receipt recovery.

- GET/POST /facilities; GET/PUT /facilities/{id}; POST /facilities/{id}/archive.
  GET query page,size,state ACTIVE/ARCHIVED,kind,code optional; exact scoped paging.
- POST /facilities/search: read-only {page,size:20,search:1..160,state?,kind?};
  literal name/code/contact/address search in body, not URL/logs/browser storage.
- GET/PUT /facility-default: {facilityId:null|UUID,version,updatedAt}; virtual d0,
  If-Match dN; PUT body {facilityId}. Separate default version/concurrent binding.
- GET /facility-receipts/{operation}/{key}: operationId,targetId,outcome,version;
  no PII/body/HMAC. Operations CREATE,UPDATE,ARCHIVE,DEFAULT,RESPONSIBILITY.
- GET /facilities/{id}/members: current eligible member candidates20, safe display
  identity through Identity public API and current site scope; not all-account data.
- GET/PUT /facilities/{id}/responsibilities: versioned explicit responsible members
  for PREPARATION,HANDOVER,RETURNS. Body {items:[{accountId,responsibility}]}, max30.
  facility.manage required; candidates must be active verified same-Org members
  already authorized for that site. Linking responsibility NEVER grants RBAC,
  Carrier authority, membership, or changes an existing Shipment assignee.

Finite-scope operator tooling may use an audited offline
merchant.configure_local_facility_grants(request,action GRANT|REVOKE,org,account,
facility,permissions[],reason). Require existing active member/site and current
verified Identity, LOCAL/TEST, owner role, no runtime EXECUTE. Keep old operator
signature/receipts intact, no role or Membership creation, exact permission list,
read dependency and request digest/receipt/audit; never choose a real target.

## Confirmed-Parcel preparation and handover [PROPOSED]

Orders owns bounded Merchant parcel preparation (BC-026), separate from SKU stock
fulfillment and every Carrier state. Existing Draft workflow stays unchanged.
No new Booking state, Quote recomputation, fee/payment or shipment owner mutation.

Only confirmed BUSINESS Shipments already bound to one active same-Org Facility
and in current NOT_ACCEPTED state are eligible. The same current site scope is
required for every Shipment/Parcel; never reassign a confirmed Shipment's facility.
Existing unbound Bookings remain visible but cannot be silently assigned to a site.

- Explicit preparation record per Shipment, optimistic version and immutable source
  Parcel-set fingerprint. READY_TO_PACK -> PACKED -> READY_TO_HANDOVER, REOPEN
  invalidates checks/readiness with reason. All confirmed Parcel IDs must be
  reconciled, no caller-created Parcel code or quantity/weight snapshot override.
- Existing shipment.prepare plus shipment.draft.read at the exact site required;
  current responsible member checks where assigned. Manager site authority governs
  reassignment through existing permitted task/shipment assignment workflows.
- GET /facilities/{id}/preparation-work: paged real eligible Shipments and optional
  preparationState. Missing work record is labelled not started, not packing failure.
- POST /shipment-preparation-work {shipmentId}; GET /shipment-preparation-work/{id};
  POST /shipment-preparation-work/{id}/checks {parcelId,condition:OK|VERIFY,note?};
  POST /shipment-preparation-work/{id}/transitions {action,reason}.
- Handover batch is a Merchant preparation/reconciliation list, NOT a Dispatch
  manifest/custody receipt or Carrier acceptance. Max20 Shipments/100 real Parcels,
  same active site; unique live batch membership. DRAFT -> READY, CANCEL with reason;
  editing/removing items returns DRAFT and invalidates old reconciliation.
- Permissions shipment.handover.read/manage (manage requires read+draft.read;
  current organization or exact site). No implicit rights from roles or print action.
- GET/POST /handover-preparations; GET /handover-preparations/{id}; PUT /{id}/items
  {shipmentIds}; POST /{id}/checks {parcelId,result:PRESENT|MISSING|VERIFY,note?};
  POST /{id}/transitions {action:READY|REOPEN|CANCEL,reason}.
- READY requires every expected real Parcel PRESENT, eligible current preparation,
  current source fingerprints, no unresolved discrepancies and all current rights.
  Concurrent membership/parcel/source changes must fail atomically or serialize.
- GET /handover-preparations/{id}/document: private downloadable preparation list,
  actual Shipment/Parcel identifiers/counts, printable Local/Test simulation and
  clear NOT_CARRIER_ACCEPTANCE designation; no fabricated receiving signature.
- GET /facility-work-receipts/{operation}/{key}: current actor/scope receipts for
  preparation/batch/check/state commands. ETag/idempotency/audit same as above.

Missing/unrecognized Parcel input is an explicit error; do not reveal another
Organization's Parcel. VERIFY/MISSING are Merchant-reported discrepancies, not
Carrier rejection, loss, POD, compensation or transport exception facts.

## Schema/upgrade [PROPOSED]

V31 forward only: extend merchant.facility with nullable legacy-compatible code,
kind/hours/sender/return metadata; immutable Facility versions, command receipts,
metadata-only audit, organization default binding, responsibility current/version
records and scoped-operator audit. Preserve old fields/IDs/FKs; code unique even
archived, same-Org defaults/members/sites, current-version/history/audit/receipt
constraints and archive blockers at DB layer. Migration captures explicit legacy
baseline versions, never fabricated historical actor actions. Update synthetic
legacy test fixtures to satisfy new invariants rather than bypass them.

V32 forward only: Orders internal preparation/check/event/receipt and Merchant
handover-preparation/item/check/event/receipt tables with same-Org/site/Shipment/
Parcel composite FKs, constrained legal states, one live batch binding, immutable
source/actor receipts and append-only evidence. Shared physical transaction; no
Carrier tables, Finance DML or accepted-contract/migration rewrite.

## Screens/source-dependent capabilities

Menu Kho & Diem lay hang is Business root-level alongside Quan ly. Site directory
and detail tabs: Overview, Contacts & hours, Team, Preparing shipments, Handover
preparation lists, Location reports, Pickup. Reuse orange Business tokens and
existing empty/loading/revocation/unknown-outcome/accessibility/responsive patterns.
Use actual OSM/MRF site filters and same-cohort drill-through; current Financial
permission still controls monetary columns. No client-only total or fake events.

Pickup scheduling remains NOT_SUPPORTED: existing approved service has no Pickup
capacity/window/request/assignment contract. Opening hours are not pickup windows.
Carrier receipt/acceptance/rejection/custody/tracking likewise need the separate
Business Carrier workflow; never mark them successful from Merchant UI. No new
Station/Hub, route, rate, COD, collection, return flow or stock inventory is created.

## Acceptance gates

Test real PG18 populated upgrade, V1-V30 checksums/data/grants, legacy-site reads,
code uniqueness, strict normalization/hours, default and archive races, inactive/
foreign sites, finite scope and revoked identities/memberships. Assignment must
not grant rights. ETag/idempotent receipt replay, atomic audit/check failure,
concurrent batch binding, source drift and wrong/missing/duplicate Parcels.
Frontend -> real HTTP -> PostgreSQL CRUD/default/site selection/work/batch/check/
reload and unknown-write recovery; private document authorization; no Carrier or
Finance effect; Production deny. Focused tests then existing applicable gates;
DONE only with observed evidence. No new worktree or automatic PR merge.
