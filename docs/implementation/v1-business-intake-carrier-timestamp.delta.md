# BIA-TIMESTAMP-01 [PROPOSED] — preserve canonical scan-time validation

SOURCE/TEST isolated only; no deployment or historical migration change.
Parent BIA-V1 SHA-256 remains
`ba0812bc41572e0870cba0b7a1572426321430530698e9b5aa697f9205aa06ea`.

The approved CarrierCargo DTO omits Shipment.createdAt. Existing shared intake
rejects scans earlier than the actual Booking timestamp; it must not substitute
a clock value, fake PERSONAL Shipment, cross-read Orders private SQL in Dispatch,
or drop that validation for BUSINESS.

Exact proposed delta: add **one `Instant createdAt` field** to the new Orders-owned
`PilotHandling.CarrierCargo` record, immediately after `long totalVnd`. Populate
only from the immutable canonical `orders.pilot_shipment.created_at` for either
owner type. Serialize it only in the already approved opt-in OPS V2 cargo view;
require a valid Instant in its client parser. Dispatch re-reads canonical cargo
and retains `booking.createdAt <= scan.scannedAt <= operationClock` for both
audiences. No original PERSONAL DTO/HTTP behavior changes.

No new HTTP path, method, port signature, schema/column, SQL function, permission,
role, grant, state transition or Identity/Finance authority. Targeted tests:
actual timestamp fidelity, pre-Booking/future scan denial and unchanged legacy
timestamp validation. Pending Owner approval; unrelated BIA work may continue.
