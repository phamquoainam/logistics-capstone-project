# SPM-V1 [PROPOSED] — shared Station Inventory → Sorting → Manifest

2026-11-27 · **BLOCKED_CONTRACT — review proposal only, no runtime implementation**.
Dependency: [Slice1 acceptance](v1-dual-audience-source-test-execution.md).
Owning specification: [Spec012](../../specs/012-sorting-hub-manifests/spec.md).
This proposal neither reserves a migration number nor approves API/DDL/ACL.
All new items identified below are **[PROPOSED]**.

## 1. Source inventory and bounded outcome

Existing and reused: Orders canonical Shipment/Parcel/Booking snapshots and
`PilotHandling.carrierAtStation`; Dispatch `pilot_custody`, intake acceptance,
scans, NWS carrier_site/version/link/assignment; genuine Identity OPS admission.
PERSONAL and BUSINESS share these facts. No new inventory balance/table, Staff
identity, admission purpose, tariff, Booking, intake or tracking engine.

Missing in production: a site-scoped custody list, sort decision, destination
routing rule and sealed Carrier manifest. Merchant WFP handover documents are
not Carrier manifests. Current NWS links bind origin/service/coverage but do
not select a next Hub for each Shipment destination; never infer that mapping.

First bounded outcome: two real accepted Parcels at the booked origin Station
→ canonical custody list → explicit rule-bound sort decisions → exact-set
Manifest Draft → Seal. Same implementation for both owner types and mixed
customers at the same Carrier site. No Transfer, receiving at another Hub,
trip, Dispatch, Courier, In Transit, Delivered, payment or inventory adjustment.
Station-only first use is an explicit Owner-requested decomposition of Spec012;
it is not full Hub/bag/capacity/variance/full-spec acceptance.

## 2. Owner policy decisions required together

| ID | Proposed bounded choice | Why not inherited from intake |
|---|---|---|
| SPM-POL-01 | After valid acceptance, authorized Carrier Staff may read/prepare existing custody even if the sender Organization is later SUSPENDED; no new intake/Booking and no bypass of a future physical/legal hold. When a required hold source exists but cannot be verified, mutations fail closed. | BIA's ACTIVE Organization gate governs intake, not already-held physical Parcels. Reusing it would hide existing custody. RESTRICTED/CLOSED/hold execution is not implemented by this proposal. |
| SPM-POL-02 | A manifest contains1–100 distinct Parcels, one origin Station, one service and one next-hop Hub under explicit currently effective rules. For this first pilot, include the entire still-held booked Parcel set of every included Shipment; do not split a Shipment. Mixed PERSONAL/BUSINESS is permitted without exposing sender PII. | Capacity and split-consignment policy have no existing approval. |
| SPM-POL-03 | Draft may replace its complete set or cancel with reason; Seal is immutable. No unseal, sealed cancellation or reservation release after Seal until a separately approved correction/Transfer workflow. | Avoid silent alteration of handed-over evidence. A sealed manifest is preparation only. |
| SPM-POL-04 | Trusted existing Local/Test network operator publishes explicit destination→next-hop rules using actual approved NWS links and four SIM route tuples; no automatic route choice or new coverage. Only isolated source/test first. | An outgoing link alone does not prove a destination routing plan. |

No retention duration, damages/compensation policy, operational SLA or money
authority is invented. Keep immutable preparation history under existing test
preservation; any future deletion/retention workflow requires its own policy.

## 3. Permissions and context [PROPOSED SPM-PERM-01–03]

Add exactly site-bound `PARCEL_INVENTORY_READ`, `PARCEL_SORT`,
`MANIFEST_PREPARE` to existing NWS capability and assignment allowlists.
Every action also requires existing SITE_READ, effective ACTIVE site version,
current assigned Staff and genuine OPS context. No implicit grant from role
R25/R26, STATION_INTAKE, Admin READ, Merchant Owner or SYSTEM Root.

Extend only existing NWS site/assignment validation and operator allowlists;
keep operator signatures/audit/idempotency and NOLOGIN roles. No new operator
login/membership or real grant. Existing intake requirements stay unchanged.
Inventory permission alone cannot sort, print private labels or mutate manifests.
Manifest mutation requires MANIFEST_PREPARE and PARCEL_INVENTORY_READ; sort
requires PARCEL_SORT and PARCEL_INVENTORY_READ. All permission checks are Backend.

## 4. HTTP and DTO manifest [PROPOSED SPM-API-01–11]

Root `/api/v1/ops/sites/{siteId}`; reuse current OPS HTTPS/cookie/CSRF transport.
No actor, owner, Organization, custody holder or timestamp authority in bodies.
All reads recheck permission before existence, pagination or receipt disclosure.

| ID | Method/path relative to root | Input/result |
|---|---|---|
|01| GET `/inventory` | InventoryQuery → InventoryPage |
|02| GET `/inventory/{parcelId}` | ParcelDetail |
|03| POST `/sorting-decisions` | SortInput → SortDecision |
|04| GET `/manifests` | ManifestQuery → ManifestPage |
|05| POST `/manifests` | ManifestInput → ManifestDetail |
|06| GET `/manifests/{manifestId}` | ManifestDetail |
|07| PUT `/manifests/{manifestId}/parcels` | ManifestInput → ManifestDetail |
|08| POST `/manifests/{manifestId}/seal` | SealInput → ManifestDetail |
|09| POST `/manifests/{manifestId}/cancellation` | ReasonInput → ManifestDetail |
|10| GET `/manifests/{manifestId}/events` | page,size → ManifestEventPage |
|11| GET `/preparation-receipts/{key}` | PreparationReceipt; same authenticated actor/site only |

Exact proposed wire DTO fields (IDs SPM-DTO-01–15, immutable copies/redacted logs):

1. InventoryQuery `{query?,serviceCode?,destinationArea?,preparationState?,page,size}`;
   query is a Parcel/Shipment code only, max80 chars; size1–50, page>=0.
2. InventoryPage `{items,total,page,size,generatedAt}`; no fictitious source version.
3. ParcelDetail `{parcelId,barcode,shipmentId,shipmentCode,ownerType,parcelNumber,
   weightGrams,lengthMm,widthMm,heightMm,serviceCode,destinationArea,siteId,
   acceptanceId,acceptedAt,preparationVersion,preparationState,sortDecision,
   manifestId}`. Source custody remains Dispatch, not a client-supplied balance.
4. SortInput `{parcelId,expectedPreparationVersion,routeRuleId,routeRuleVersion,
   scannedBarcode}`; literal barcode must match the canonical Parcel.
5. SortDecision `{id,parcelId,siteId,nextSiteId,routeRuleId,routeRuleVersion,
   preparationVersion,actorId,occurredAt}`.
6. ManifestQuery `{state?,nextSiteId?,page,size}`; size1–50.
7. ManifestInput `{nextSiteId,serviceCode,parcelIds}`; exact distinct set1–100.
8. ManifestDetail `{id,siteId,nextSiteId,serviceCode,state,version,parcels,
   setDigest,createdAt,sealedAt,cancelledAt}`; parcels contain ParcelDetail's
   operational fields only, no contact/address/fee snapshots.
9. ManifestPage `{items,total,page,size}`; list items exclude full Parcel arrays.
10. SealInput `{expectedSetDigest,parcelIds}`; repeat exact reviewed set, never
    treat a button click as physical transfer or receiving evidence.
11. ReasonInput `{reason}`; trimmed1–500 chars, no private supporting attachments.
12. ManifestEvent `{id,manifestId,version,operation,actorId,requestId,occurredAt,
    setDigest,reason}`; reason allowlisted, no editable/deletable event UI.
13. ManifestEventPage `{items,page,size,hasNext}`.
14. PreparationReceipt `{operation,targetId,version,status}`; status COMMITTED.
15. PreparationFailure `{code}`; allowlisted codes only.

All mutations require UUID Idempotency-Key. Manifest replace/seal/cancel require
If-Match `"vN"`; create starts version1. Sort uses its explicit preparation
version, initially0 for accepted custody without a sort decision. Response
ETag is the committed version. Successful create201, other success200.
Error contract:400 VALIDATION_FAILED;401 SESSION_INVALID;403 AUTHORITY_REQUIRED;
404 RESOURCE_NOT_FOUND (foreign resources);409 STATE_CONFLICT/KEY_CONFLICT;
412 PRECONDITION_STALE;422 ROUTE_UNAVAILABLE/PARCEL_SET_INCOMPLETE;
503 SOURCE_UNAVAILABLE. No private response body/SQL detail. Same-key replay
checks current authority and full original digest before returning a receipt,
even after original ETag is stale. Different input or actor/site is rejected.

## 5. Java/domain composition [PROPOSED SPM-PORT-01]

One new Dispatch-owned public interface `dispatch.api.StationParcelPreparation`;
methods correspond exactly to11 operations above:

```java
InventoryPage inventory(OperationalStaffAdmission.Context actor, UUID siteId, InventoryQuery query);
ParcelDetail parcel(OperationalStaffAdmission.Context actor, UUID siteId, UUID parcelId);
SortDecision sort(OperationalStaffAdmission.Context actor, UUID siteId, SortInput input, UUID key);
ManifestPage manifests(OperationalStaffAdmission.Context actor, UUID siteId, ManifestQuery query);
ManifestDetail create(OperationalStaffAdmission.Context actor, UUID siteId, ManifestInput input, UUID key);
ManifestDetail manifest(OperationalStaffAdmission.Context actor, UUID siteId, UUID id);
ManifestDetail replace(OperationalStaffAdmission.Context actor, UUID siteId, UUID id, long version, ManifestInput input, UUID key);
ManifestDetail seal(OperationalStaffAdmission.Context actor, UUID siteId, UUID id, long version, SealInput input, UUID key);
ManifestDetail cancel(OperationalStaffAdmission.Context actor, UUID siteId, UUID id, long version, ReasonInput input, UUID key);
ManifestEventPage events(OperationalStaffAdmission.Context actor, UUID siteId, UUID id, int page, int size);
PreparationReceipt receipt(OperationalStaffAdmission.Context actor, UUID siteId, UUID key);
```

Reuse genuine `OperationalStaffAdmission.requireCurrent`, existing Orders
`PilotHandling.carrierAtStation` and site/network authority. Do not broaden the
existing intake-only `OperationalStationIntake.require` to imply these rights.
Dispatch reads its own custody source and projects only operational Orders
fields; private contacts and Finance fields never cross these HTTP DTOs. No
new Orders/Finance/Identity API, private-table read or compile-time dependency.
Workflow remains the HTTP composition root and transaction owner through current
Identity admission. SPM uses the existing Identity→Dispatch authority lock order,
then sorted Parcel IDs and manifest lock; never call the BIA Merchant-active
intake wrapper after acquiring Dispatch locks. Revalidate before commit.

## 6. Forward schema proposal [PROPOSED SPM-DB-01–07]

Seven Dispatch-owned tables; no inventory table and no edits to accepted custody:

| ID/table | Essential keys, facts and lifecycle |
|---|---|
|01 `station_sort_rule` | UUID row PK; logical rule_id/version UNIQUE; exact from_site_id/to_site_id and NWS link-version FK, dataset/service/origin/destination, effective interval and PUBLISHED/RETIRED; actor/operator receipt binding. Append-only versions, no overlapping applicable rule for a tuple. |
|02 `parcel_sort_decision` | UUID PK; Parcel/custody/Shipment composite binding, origin site, rule version, next site, actor/assignment/site-version/request/time and positive preparation_version; UNIQUE(parcel_id,preparation_version), immutable decision history. No decision while reserved; correction requires a later explicit new decision/version. |
|03 `station_manifest` | UUID PK; origin/next site/service, DRAFT/SEALED/CANCELLED, positive version, created/sealed/cancelled times and exact-set digest; version increments by1, only allowed transitions. Sealed/cancelled header immutable. |
|04 `station_manifest_parcel` | PK(manifest_id,manifest_version,parcel_id); exact sort-decision/custody FK and immutable operational snapshot; retains each reviewed revision. No duplicate Parcel per version; no FK to Merchant WFP documents. |
|05 `parcel_manifest_reservation` | parcel_id PK, FK exact manifest revision/item; prevents incompatible active membership across manifests. Replace/cancel releases only the appropriate Draft reservations under ordered locks; Seal freezes them. This is grouping exclusivity, not another custody/inventory source. |
|06 `station_preparation_audit` | UUID PK; actor/site/request/operation/target/version/time/set_digest/reason; immutable, mandatory command evidence, no plaintext PII. |
|07 `station_preparation_receipt` | PK(actor_id,site_id,operation_key); operation/target/original_version/canonical SHA256 digest/result_version/audit_id/time; FK audit and exact domain result, immutable. |

Define missing composite UNIQUEs only on existing Dispatch-owned custody/link
columns needed by these FKs; no broad Orders/Identity table access. Before SQL
implementation, reconcile exact column types against current catalog. A change
to public port/schema/authority beyond these items requires a bounded amendment.

DB checks/guards must deny direct-DML fabrication, wrong holder/site/assignment,
expired/revoked Staff, mismatched route/set/digest/version/audit/receipt, sealed
mutation and two concurrent reservations. Business writes/audit/receipt/required
reservation and complete item set commit or roll back together. No SMTP/IO in TX.

## 7. Operator/ACL manifest [PROPOSED SPM-SQL-01 / SPM-ACL-01]

One new trusted offline rule function:

```sql
dispatch.configure_local_station_sort_rule(
  p_request UUID, p_rule UUID, p_expected BIGINT, p_config JSONB, p_reason TEXT
) RETURNS TABLE(rule_id UUID, version BIGINT, replayed BOOLEAN)
```

Exact config keys: `fromSiteId`, `toSiteId`, `linkVersionId`, `datasetVersion`,
`serviceCode`, `originArea`, `destinationArea`, `state`, `effectiveFrom`,
`effectiveTo`, `environment`. All bindings use actual approved PilotCoverage/
NWS sources; operator input cannot create a route, site or coverage capability.
Reuse `dispatch_network_writer` NOLOGIN and existing network operator login
procedure, physical environment binding, request/operator/input digest receipts
and network audit. Extend existing audit/receipt operation allowlist only for
SORT_RULE; no writable legacy bypass, new operator membership or auto-grant.

New private trigger functions (not public ports):
`dispatch.guard_station_preparation() RETURNS trigger`,
`dispatch.require_complete_station_preparation() RETURNS trigger`, and
`dispatch.require_station_preparation_authority() RETURNS trigger`.
First two SECURITY INVOKER under existing canonical schema owner, authority guard
follows existing NWS SECURITY DEFINER/NOLOGIN writer pattern with fixed search_path,
current subject predicate and site assignment rechecks. No PUBLIC EXECUTE; no
direct runtime/operator EXECUTE on private guards. Deferred complete/authority
checks at commit, not frontend-only invariants. Existing intake guard unchanged.

Runtime grants limited to SELECT/INSERT on new immutable facts/audit/receipts,
SELECT/INSERT plus column-bounded UPDATE on Draft header/reservations and DELETE
only Draft reservation release guarded by matching command. Runtime cannot write
route rules or assignments. Network writer gets only its own new rule table and
existing bounded sources; operator gets EXECUTE only on new operator function.
No new Identity/Finance/Merchant SELECT, PUBLIC privilege, CREATEROLE, role
membership, pg_control_system grant or SECURITY DEFINER owner bypass.
Final generated catalog ACL inventory is a mandatory implementation gate.

Migration candidate: **next free forward version after V49, unreserved**. Recheck
develop/branches/open PRs before naming a file. Preserve V1–V49 bytes and existing
rows; no reset/repair/outOfOrder. Proposed changes are isolated source/test only.

## 8. Targeted acceptance [PROPOSED SPM-T-01–06]

1. PG18 cold/populated upgrade, owner/ACL/FK/CHECK/unique and direct-DML negative
   tests; no second inventory source; existing Slice1 rows/checksums survive.
2. Both PERSONAL and BUSINESS two-Parcel Booking→real Intake→Inventory; current
   Station custody exactly2, wrong-site/revoked/expired/forged-context denial.
3. Valid barcode + explicit route version → immutable sort evidence; wrong
   destination/service/rule/link/holder/stale version deny with zero side effects.
4. Exact-set Draft/replace/cancel/seal; missing/duplicate/foreign/wrong-next-hop
   denial; sealed data immutable; two racing manifests cannot reserve one Parcel.
5. Same-key lost response/replay, input conflict, revocation race and late
   audit/receipt/item/reservation failure roll back; snapshots/UNPAID/custody/
   tracking unchanged by sort/seal. Reader page total and filters use one cohort.
6. Consolidated genuine HTTPS+PG18 Ops UI for both audiences, correct empty/error/
   pending/conflict/denial states and desktop/mobile; affected lint/typecheck/build/
   architecture checks only. Reuse Slice1 evidence when source is unchanged.

## 9. Coverage and next approval checkpoint

Maps bounded source requirements BC-263 inventory, BC-254/256 sort provenance,
BC-261 privacy denial and legacy REQ-008-02 exact manifest/scan/chain of custody.
BC-255 bag hierarchy, BC-257 redirect workflow, BC-259 damage evidence, BC-262
capacity, BC-265 variance approvals and BC-269 inventory adjustment remain
separate dependencies; no FULL Spec012 READY claim for this foundation.

**One consolidated review:** SPM-POL-01–04 plus SPM-PERM-01–03, API/DTO/port,
DB/SQL/ACL items above. Open policy choices must be resolved before runtime code.
No implementation, reservation of V50, real grants, deployment or Git publication
has been performed. Following this bounded acceptance, Spec013 owns sender/
receiver transfer and Hub custody; Spec014 then owns Dispatch.
