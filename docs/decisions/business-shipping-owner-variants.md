# BSC-06 — Confirmed Shipping ownership

Status: ACCEPTED by Owner2026-11-13, Local/Test only.
Authority: [exact accepted amendment](../implementation/business-confirmed-shipping-contract.proposed.md),
including separate explicit confirmations of nullable owner variants and BOOK
guard/audit after automated approval review. These confirmations authorize source
and disposable test databases first; this checkpoint does not migrate localhost.

Business uses the existing Pricing Quote, Orders Shipment/Parcel, Finance UNPAID
and private100×150mm label infrastructure. PERSONAL records retain real account
and Draft references. BUSINESS records have null Personal owner/draft references
and mandatory Organization, creator, preparation/version and pricing-contract
bindings. Mutually exclusive CHECKs preserve Personal required values; composite
FKs prevent organization/facility/version mixing. No Organization UUID is used as
an account ID. V1–V25 remain immutable; all schema amendments are additive V26.

Workflow composes public module APIs inside current Identity → Merchant admission
and one physical transaction. Merchant's existing scope lock serializes membership,
grant, facility and contract decisions. Quote consumption, sealed preparation,
Shipment/Parcels, audit/receipt and UNPAID commit together. Deferred Business-only
booking evidence validates the price/Parcel snapshot and Finance binding. Historic
CREATE/UPDATE/ARCHIVE/IMPORT evidence and FKs remain enforced.

Pricing contracts are append-only versions with explicit operator principal,
request/approval reference and timestamp. Runtime cannot activate them. A newer
suspended version invalidates outstanding Quotes. Role names do not confer quote,
book or label permissions; current permission and facility scope govern every
read, mutation and receipt replay. The operation HMAC includes organization,
actor, purpose and exact version/request binding. No blind write retry.

Personal reads explicitly retain PERSONAL scope. Station lookup denies Business
Parcels: the existing unpaid Station acceptance exception is Personal-only.
Business Booking creates NOT_ACCEPTED and UNPAID, never custody, tracking,
collection, COD, Pickup, delivery or a commercial tax document. Production/prod
profiles and non-sandbox modes fail closed.

The form shares contact, measured Parcel, notes, review and quote presentation;
transport/ownership remain purpose-specific. Business never imports an employee's
Personal address book. Organization contact/facility/default/template masters,
internal packing, batch confirmed Booking and Merchant lifecycle/support remain
separate capabilities; BSC-06 is not a claim of full Business Workspace completion.
