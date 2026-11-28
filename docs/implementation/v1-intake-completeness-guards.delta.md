# BIA-DB-GUARD-01 [PROPOSED] — exact deferred cross-domain completeness reads

2026-11-27 · isolated SOURCE/TEST only · not approved or applied.

Automatic approval review rejected an attempted V48 guard patch: cross-private
Orders/Dispatch plus receipt/audit SELECT exceeds the approved private-read
boundary. Do not retry or apply through another tool without explicit approval.
Existing BIA ownership FKs/CHECKs and NWS authority guards remain in place.

## Exact additional boundary requested

Replace only the two existing private deferred trigger function bodies below
in **unapplied V48**, keeping their signatures, SECURITY INVOKER, owners, ACLs,
trigger timing and existing runtime SELECT/INSERT privileges unchanged.

1. `dispatch.require_complete_pilot_custody() RETURNS trigger`:
   - Existing own-domain custody count must equal acceptance.parcel_count.
   - Read Orders `pilot_parcel` by exact NEW.shipment_id to require that count
     equals the complete canonical booked Parcel set.
   - Read Orders `pilot_shipment.created_at` by custody.shipment_id to reject
     scans before Booking; own scan/custody times must not exceed acceptance.
   - Read Orders `pilot_tracking_event` only for NEW.id / NEW.shipment_id /
     NEW.owner_type / NEW.occurred_at, requiring the corresponding event.
   - Require own Dispatch ACCEPT audit and receipt with the same actor/site/
     Shipment/acceptance/time; audit must also match request_id.
2. `orders.require_complete_pilot_tracking() RETURNS trigger`:
   - Retain own canonical Parcel/event set count and require equal event times.
   - Verify own immutable Shipment owner type/account/Org/facility and Booking
     timestamp against NEW; use null-safe owner comparisons.
   - Read Dispatch `pilot_intake_acceptance` only by NEW.source_acceptance_id /
     Shipment/owner type/site/actor/time to verify the same source event.

These are validation-only reads with SQLSTATE 23514 on mismatch. They return
NULL as deferred trigger functions, not private domain rows to callers. No
additional SELECT grant, role, membership, SECURITY DEFINER, PUBLIC EXECUTE,
operator or HTTP API. No Finance read/write and no historical migration rewrite.
The existing runtime already has SELECT on these intake/tracking/Shipment/Parcel
tables; approval is requested for the **cross-domain guard dependency**, not a
new runtime privilege or a second acceptance engine.

All lookups are bounded to the transaction's exact Shipment and authoritative
acceptance; no unscoped reader or client-controlled Organization. Require both
groups to commit together, unchanged snapshots/UNPAID and complete rollback.

## Required gates after approval

PG18 positive PERSONAL/BUSINESS all-Parcel acceptance; wrong owner/Org/site/
actor/Parcel/time/source-event denial; missing tracking/custody/audit/receipt
denial at commit; concurrency/replay; injected late failure rollback; catalog
function owners/prosecdef/ACL unchanged; architecture review of these exact
dependencies. The rejected implementation is not counted as source/test PASS.

Until approved and tested, BIA-T-01/04 DB completeness acceptance remains
incomplete even if application-path HTTP/Browser scenarios pass.
