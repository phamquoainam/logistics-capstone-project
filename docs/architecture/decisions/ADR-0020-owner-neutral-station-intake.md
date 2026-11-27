# ADR-0020 — Owner-neutral Station Intake

Status: **ACCEPTED DESIGN — isolated SOURCE/TEST only**, Owner BIA-V1 directive
and bounded BIA-TIMESTAMP-01 / BIA-OWNER-01 / BIA-DB-GUARD-01 approvals.
Implementation readiness is separate and recorded in the execution report.

## Decision

PERSONAL and BUSINESS keep their own admission, immutable Booking/pricing and
UNPAID obligation policies. Both use the same Dispatch scan/acceptance/custody
algorithm and Orders tracking stream. No second inventory or acceptance engine.

Orders' CarrierCargo projects canonical owner type/account/Organization/facility,
actual Booking.createdAt, route, Parcels and current events. Dedicated OPS callers
use opt-in V2; legacy PERSONAL DTOs and receipt digests remain unchanged. Dispatch
re-reads canonical cargo through the Orders public port under current Ops site
authority, rather than trusting constructed facts or importing Pricing services.

Workflow locks genuine Identity admission, Merchant workspace, Dispatch authority,
then intake facts in that order. Merchant checks ACTIVE Organization/current
Owner/optional facility; Finance validates the existing exact BUSINESS UNPAID
obligation. Domain-separated Business receipts include actual Organization and
Parcel set. Replays still require current site and Organization admission.

V48 adds owner FKs/CHECKs and the two existing deferred completeness guards.
BIA-DB-GUARD-01 permits only their explicitly listed validation reads, retaining
SECURITY INVOKER, function owners/ACL and existing runtime privileges. No new
private SELECT grant or SECURITY DEFINER bridge is introduced.

V49 repairs only the V46 review-comment table's LOGIN-owner defect using a
catalog-verified canonical NOLOGIN owner. Historical migrations are immutable.

## Boundaries

Business intake uses its own BR13-BUSINESS-INTAKE-01 policy and default-off flag,
only LOCAL/TEST + SANDBOX + non-Production. Booking/intake do not collect money,
imply Delivered, enable Pickup or authorize downstream Sorting/Manifest/Transfer.
Printing labels does not establish custody. Current events, not immutable
Booking.carrier_state, determine the displayed AT_STATION state.

Original approved BIA artifact SHA-256:
`ba0812bc41572e0870cba0b7a1572426321430530698e9b5aa697f9205aa06ea`.
Guard amendment SHA-256:
`6950d9601e81b5a6da3589d492cae5f990db64b82fa254b50172da5fc42ed240`.
This ADR grants no deployment, real operator or publication authority.

Evidence: [dual-audience execution](../../implementation/v1-dual-audience-source-test-execution.md).
