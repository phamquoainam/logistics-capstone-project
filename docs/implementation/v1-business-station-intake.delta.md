# BIA-V1 — Merchant Booking → canonical Station Intake [PROPOSED]

**2026-11-26 · one consolidated SOURCE/TEST approval checkpoint.**
Status: **BLOCKED_CONTRACT — not implemented or approved**.
Dependency: [V1 critical-path audit](v1-operational-e2e-critical-path.md).
This is not approval of deferred Menu2 Step B or downstream Spec012–015.

## BIA-POL-01 — exact business scope requiring Owner approval

Extend the existing unpaid-intake exception to **BUSINESS Local/Test only**:
an ACTIVE, validly activated/provisioned Merchant Organization has a confirmed
ROAD_STANDARD Shipment on one of the four approved ARGO-SIM-COVERAGE-1 routes.
Dedicated OPERATIONS Staff with current SITE_READ + STATION_INTAKE at its booked
SIM-DROP origin may inspect MATCHED and accept **all 1–5 Parcels together** even
though its exact `BR13-BUSINESS-LOCAL-01` obligation remains UNPAID.

Use intake policy identifier `BR13-BUSINESS-INTAKE-01`. No inherited PERSONAL
exception: the BUSINESS branch has its own explicit policy/ownership checks.
No Pickup, collection, PAID, COD, sorting, movement, In Transit or Delivered.
No Admin/Customer/Support principal becomes Operations Staff. Legacy Customer-
admitted Station paths remain PERSONAL-only. No new global roles or permissions.
New property `shipping.pilot.business-intake-enabled` defaults false and is
effective only with all existing LOCAL/TEST + SANDBOX + non-Production gates.
It is enabled only in owned isolated tests after approval; this proposal does
not activate any local/shared/demo runtime or weaken the PERSONAL gates.
Non-ACTIVE Merchant admission is denied in this bounded pilot; broader handling
policy for suspended/restricted/closed Organizations is not implemented here.

## BIA-PORT-01 — Merchant eligibility through existing owning port

Add to existing `merchant.api.customer.PilotCoverage`, default fail-closed:

```java
<T> T withIntakeOrganizationLock(OperationalStaffAdmission.Context actor,
                                 java.util.function.Supplier<T> work);
void requireIntakeOrganization(OperationalStaffAdmission.Context actor,
                               UUID organizationId, UUID facilityId);
```

Merchant verifies genuine current Identity-owned Ops context/physical transaction,
current LOCAL/TEST binding, actual ACTIVE Organization, valid existing origin,
current owner/Membership invariants and same-Org active facility when non-null.
This is not Business membership for Staff or authority to browse an Organization.
Workflow supplies IDs from the Orders-owned Shipment, never a request-body Org.
No cross-private SQL or fabricated Customer context. Reuse current PilotCoverage
Ops-context owner checks and persisted coverage; do not add routes or tariffs.
The wrapper verifies genuine context and physical transaction before taking the
existing Merchant workspace admission lock `(730023,1)`. Workflow constructs its
OPS/Dispatch site scope inside that callback, then obtains the existing Dispatch
authority lock `(730021,1)`, then Shipment/intake locks. No new lock namespace,
Workflow private SQL or reverse Dispatch→Merchant lock acquisition. The lock
persists through transaction completion; current Organization eligibility is
rechecked before commit. Include intake versus concurrent Organization/facility/
assignment change and Booking in the focused concurrency gate. Do not change
deferred Menu2 Lifecycle/Team lock or authority implementations.

## BIA-PORT-02 — owner-neutral Orders carrier facts

Preserve existing `PilotHandling.Cargo`, all existing methods and personal JSON.
Add these records/methods on the **same Orders-owned PilotHandling port**; default
methods fail closed until implemented:

```java
record CarrierCargo(UUID shipmentId, String shipmentCode, String ownerType,
                    UUID accountId, UUID organizationId, UUID facilityId,
                    UUID quoteId, long totalVnd, ShippingDrafts.Input input,
                    PilotQuotes.Route route, List<PilotShipments.Parcel> parcels,
                    Handling handling) {}
CarrierCargo carrierLookup(CarrierAccess actor, String barcode);
CarrierCargo carrierAtStation(CarrierAccess actor, UUID shipmentId);
void carrierAccept(CarrierAccess actor, UUID shipmentId, long expectedVersion,
                   UUID acceptanceId, Instant occurredAt);
byte[] carrierLabel(CarrierAccess actor, UUID shipmentId);
Handling businessSelf(BusinessPreparations.Access actor, UUID shipmentId);
```

`ownerType` is exactly PERSONAL or BUSINESS, derived from the canonical row.
PERSONAL requires accountId and null Organization/facility; BUSINESS requires
Organization, null accountId, optional same-Org facility. Never put an Org UUID
in accountId or treat creatorAccountId as shipment owner. No client-owned DTO is
authority. Orders re-reads confirmed immutable Shipment/Quote/Parcel references
at the booked origin under current CarrierAccess and its transaction. No quote
recalculation, new IDs or snapshot edits. `CarrierCargo.toString` must redact.

`carrierAccept` appends the same canonical acceptance/Parcel events; it does not
UPDATE the immutable Booking carrier_state or create a second tracking stream.
`businessSelf` uses actual current Organization/Facility permission through the
existing Business access, not creator-only ownership. Label renderer is reused
with actual stored code/input/route/Parcels; simulation marking stays on PDF.

## BIA-PORT-03 — reuse the same Dispatch intake algorithm

Add two overloads to existing `OperationalStationIntake` only:

```java
StationIntake.Scan scan(OperationalStaffAdmission.Context actor, UUID siteId,
                       PilotHandling.CarrierCargo cargo, String barcode, UUID key);
StationIntake.Acceptance accept(OperationalStaffAdmission.Context actor, UUID siteId,
                       PilotHandling.CarrierCargo cargo, List<UUID> scanIds,
                       long expectedVersion, String inspection, UUID key);
```

Both delegate to the existing exact-set/idempotency/locking/receipt/custody core
after normalizing canonical facts. Inject the existing Orders public reader to
re-read/compare cargo before trusting a constructed CarrierCargo. Current issued
Ops context/site/assignment, route, parcel set, inspection and version are checked
under the same authority and shipment locks through commit. No second acceptance
or inventory engine. Historical PERSONAL receipt fingerprints remain unchanged;
BUSINESS fingerprints are domain-separated by BUSINESS/Org/Shipment/Parcel set.

## BIA-PORT-04 — Finance read binding, no new Finance authority

Add to `BusinessObligations`, default fail-closed:

```java
void requireUnpaidHandover(PilotObligations.HandoverAccess actor,
                          UUID organizationId, UUID facilityId, UUID shipmentId,
                          UUID quoteId, long amountVnd);
```

Reuse the existing HandoverAccess composition pattern. Its private Workflow
adapter checks genuine Ops admission + current site authority + Merchant intake
eligibility. Finance reads only its own canonical row and requires exact BUSINESS/
Org/facility/Shipment/Quote/amount, VND/UNPAID/SENDER/CASH_ON_HANDOVER and existing
BR13-BUSINESS-LOCAL-01 binding. It returns no financial data and writes nothing.
No Merchant Finance grant or synthetic BusinessAccess is issued to Staff.
Existing atomic Booking/obligation/FK/immutability guards stay mandatory.

## BIA-HTTP-01 / BIA-DTO-01 — bounded UI/API integration

Existing OPS URLs, credentials/cookies/CSRF, request bodies, idempotency and
If-Match remain. No owner/Org field is accepted in scan/acceptance bodies.

- Existing GET `/api/v1/ops/sites/{siteId}/parcels/{barcode}`, GET
  `/api/v1/ops/sites/{siteId}/shipments/{id}`, POST `/scans` and POST
  `/shipments/{id}/acceptance` support opt-in Accept
  `application/vnd.argopost.station-cargo.v2+json` on the same site prefix.
  V2 view is `{cargo: CarrierCargo, scans: StationIntake.Scan[], paymentState:
  "UNPAID", simulation: true, policyVersion:
  "BR13-LOCAL-INTAKE-01"|"BR13-BUSINESS-INTAKE-01"}`;
  scan response remains existing Scan; acceptance response remains the refreshed
  view/ETag. V1 retains PERSONAL-only behavior. Codes are selected server-side.
- Existing OPS private PDF URL resolves owner-neutral canonical cargo under the
  same site/financial policy. PERSONAL output bytes remain compatible; Business
  label bytes derive from its immutable Booking, no client-supplied fields.
- Add **GET** `/api/v1/merchant-organizations/{organizationId}/shipments/{id}/handling`
  returning existing `PilotHandling.Handling`, requiring current membership,
  `shipment.draft.read` at the actual facility and existing Organization admission.
  No new permission or public tracking/PII endpoint.
- Existing Business management GET supports opt-in Accept
  `application/vnd.argopost.business-management.v2+json` using
  `BusinessShipmentManagement.PageV2` with the existing page fields and
  `ItemV2 { shipment: BusinessShipments.Shipment, source: Source,
  assignment: Assignment, allowedActions: string[], handling: PilotHandling.Handling }`.
  Add default-deny `PageV2 readV2(Access, Map<String,String>, Consumer<List<FeeBinding>>)`
  on the existing port. Batch-capture current events within the same scoped cohort;
  no per-row unscoped fetch or count inflation from joins. Summary shape unchanged,
  waitingForHandover counts actual no-acceptance rows. Filter adds only AT_STATION
  alongside existing WAITING_FOR_HANDOVER. V1 DTO unchanged and its waiting count
  remains correct; immutable Shipment NOT_ACCEPTED remains a Booking snapshot,
  not a false assertion that an accepted shipment is still waiting.
- Orders-owned report/Admin readers consuming these facts must derive acceptance
  from events or remain explicitly unavailable. Do not infer Delivered/COD/revenue.
  No Admin/shared Menu6–9 API, policy or permission is added.

Business Detail/Management and OPS UI consume these exact reads and show real
Station event/UNPAID. Existing labels/quotes/booking and legacy Customer Station
flows stay intact. Empty/error/denial/pending-receipt states remain explicit.

## BIA-DB-01 — forward-only schema envelope (candidate not reserved)

No new table, operator role, application permission, provisioning path or SQL
public function. One forward migration candidate, **number unreserved**; recheck
remote branches/PRs/registry before creation. Never edit V1–V47.

1. `dispatch.pilot_intake_acceptance`: add `owner_type VARCHAR(8) NOT NULL DEFAULT
   'PERSONAL'`, allow exactly PERSONAL/BUSINESS; add parent composite Shipment
   FK `(shipment_id,owner_type)` to Orders' immutable `(id,owner_type)` key.
   PERSONAL must keep BR13-LOCAL-INTAKE-01; BUSINESS requires
   BR13-BUSINESS-INTAKE-01 and the operational-assignment branch, never legacy
   assignment. Add the exact composite acceptance key including owner_type.
2. `orders.pilot_tracking_event`: add the same owner_type tag/default plus
   `organization_id UUID NULL`, `facility_id UUID NULL`. owner_id may be null
   **only for BUSINESS**; PERSONAL keeps its existing FK/non-null CHECK. BUSINESS
   requires non-null Organization; composite Shipment/Org FK and optional
   Shipment/Org/facility FK bind actual immutable owner (never a creator alias).
   Add missing parent unique keys only where required for these exact FKs.
3. Add tracking source FK `(source_acceptance_id,shipment_id,station_code,actor_id,
   owner_type)` to the matching acceptance. Add scan Parcel/Shipment FK to actual
   Orders Parcel and acceptance Shipment FK, preserving existing unique rules.
   No orphan/fake Parcel, cross-owner or different-actor acceptance may commit.
4. Narrow forward replacements of existing `dispatch.require_operational_intake_authority`,
   `dispatch.require_complete_pilot_custody`, `orders.require_complete_pilot_tracking`
   only as necessary for these ownership/exact-set/same-event guards. Keep existing
   genuine Ops/site/assignment/expiry/revocation/current-context checks and all
   legacy branches. Do not replace writer guards with schema-owner bypass.
5. Historical rows proven PERSONAL retain all original values and references;
   new ownership tags supply that known fact, no event/timestamp rewrite. Validate
   before applying constraints. All three event/custody/receipt groups commit or
   roll back together. Runtime still has no UPDATE/DELETE of immutable facts.

ACL: retain existing role memberships/owners/NOLOGIN and runtime SELECT/INSERT
rights on the existing intake/tracking tables. New columns on those same tables
are subject to the existing whole-table grants and strengthened guards; add no
cross-domain private SELECT, Finance DML, operator EXECUTE or pg_control_system
grant. If more privilege is actually required, stop that delta for review.

## BIA-T-01–05 — required implementation gates, all NOT RUN

1. PG18 forward migration + historical checksum/data preservation; exact FK/
   CHECK/owner/ACL inventory; wrong owner, orphan Parcel and fabricated direct DML deny.
2. Genuine Ops at exact origin: Merchant two-Parcel Booking → two scans → one
   acceptance, two custody rows, one Shipment event, two Parcel events, audit/
   receipt; immutable snapshots and UNPAID unchanged. No collection/movement.
3. Wrong/inactive site, missing/expired/revoked Staff/assignment, wrong audience,
   wrong Org/facility, finance-binding mismatch, missing/duplicate/foreign Parcel,
   constructed cargo and stale versions deny; Customer cannot use Business intake.
4. Same-key lost-response replay and concurrent staff races produce exactly one
   effect; revoke race serializes; late custody/tracking/audit/receipt failure rolls
   everything back. No DML/test shortcut to make acceptance pass.
5. One consolidated genuine HTTPS+PG18 Browser flow from application approval
   through explicitly recorded operator shipping setup, Merchant Quote/Booking/
   label and OPS intake back to Merchant current tracking. Targeted affected
   PERSONAL/NWS/management/Finance/architecture/UI regression only, not full suite.

Shared owners: Merchant eligibility, Orders carrier/read projections, Dispatch
acceptance, Finance binding, Workflow composition and portal consumers. Coordinate
exact revisions before modifying shared WIP; no new Identity/Team implementation.
This checkpoint does not select real users/operators/sites or approve deployment,
real grants, emails, commit/push/merge, compensation, payment or Spec012 execution.
