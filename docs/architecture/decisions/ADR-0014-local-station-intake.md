# ADR-0014 — Bounded Local/Test Station intake and private labels

Status: ACCEPTED within Owner Milestone B authorization and subsequent explicit
[unpaid all-Parcels exception](../../decisions/br13-local-intake-01.md),2026-11-02.

Orders owns labels/Parcel identifiers and customer-visible tracking. Dispatch owns
Station/capability/assignment, scans, acceptance/custody/audit. The existing verified
human Customer Identity is reused; Dispatch permission/assignment is additional
authority, never inferred from an Identity/Merchant role or client body.

One existing CustomerAdmission transaction coordinates only public APIs; no new
authentication service, cross-module SQL, queue/outbox dependency or architecture
collaborator expansion. Dispatch receives source facts from Orders' public Cargo
projection, not Pricing internals/API. Current admission plus the Dispatch lock
protect permission through commit; owner capability/revocation triggers take the
same lock. Runtime assignments are read-only. Explicit owner-only provisioning is
audited and requires LOCAL/TEST operator context; no original-DB Staff fixture seed.

Forward V21 keeps V20 booking snapshots immutable. Current operational state comes
from append-only Orders Shipment/Parcel events. Dispatch and Orders deferred
completeness/FKs/uniqueness complement one physical transaction; lost-response
recovery uses original actor/Station/operation receipts. No Customer actor/timestamp/
payment/measurement override is accepted. Event history hides internal Staff audit.

Private100x150mm PDF generation uses bundled font/Owner logo and Code128 from the
existing Parcel UUID. No redundant label table, public file or S3 dependency.
Current self ownership or current matching Staff permission is required before
rendering. Identifiers remain stable; PDF/Barcode contains no credentials.

LOCAL/TEST plus SANDBOX/non-production gates remain mandatory. Finance performs a
read-only binding check of the actual immutable UNPAID obligation. Collection/PAID,
Pickup, In Transit/Delivered, partial intake, Production/Merchant remain excluded.
See [contract and evidence](../../implementation/milestone-b-station-acceptance.md).
