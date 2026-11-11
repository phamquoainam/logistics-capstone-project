# Personal Address Book — approved bounded contract

Status: **IMPLEMENTED / VERIFIED_LOCAL**, 2026-11-01; hosted integration CI pending publication.
Authority: Owner reply “OWNER APPROVAL — PERSONAL ADDRESS BOOK / GO WITH CONDITIONS”.
Self-owned CRUD/archive, UNVERIFIED contacts, at most one active default per kind,
ETag/idempotency/audit/PII/real PG are approved; no Shipping/Pricing/Business expansion.

## Manual address boundary

Store customer-entered sender/recipient contacts with status `UNVERIFIED` only.
No geocoder, coordinates, confirmed administrative catalog, coverage or pricing
is inferred. A saved/default address never authorizes booking or physical pickup.
Shipping remains closed under the Owner's latest instruction.

## Approved HTTP contract

All paths use the existing Customer TLS/Bearer/current admission chain. Writes
also require existing exact Origin, customer CSRF and UUID Idempotency-Key.
The server derives self ownership; no input accountId, merchantId, role or status.

| Operation | Route | Success / concurrency |
| --- | --- | --- |
| listPersonalAddresses | GET `/api/v1/customer/addresses` |200; `page` default0, `size`20 only, `search`≤160, optional `kind`; owner/filter before paging, order updatedAt DESC/id ASC |
| getPersonalAddress | GET `/api/v1/customer/addresses/{id}` |200, ETag `"vN"`; foreign/archived/missing all404 |
| createPersonalAddress | POST `/api/v1/customer/addresses` |201 + Location + ETag; one effect per key/payload |
| updatePersonalAddress | PUT `/api/v1/customer/addresses/{id}` |200 + ETag; If-Match required, stale409 |
| archivePersonalAddress | POST `/api/v1/customer/addresses/{id}/archive` |204; no body, If-Match required; remove from active book and clear default atomically |
| personalAddressReceipt | GET `/api/v1/customer/address-receipts/{operationId}/{key}` |200 current self-authorized receipt; no address PII; never extends idle |

Create/replace input (all required except label):

```json
{
  "kind": "SENDER",
  "label": "Nhà riêng",
  "contactName": "Người gửi thử",
  "contactPhone": "0901234567",
  "address": {
    "country": "VN",
    "administrativeArea": "Tên tỉnh/thành do người dùng nhập",
    "locality": "Tên phường/xã do người dùng nhập",
    "addressLine": "Địa chỉ chi tiết do người dùng nhập"
  },
  "isDefault": false
}
```

`kind`: SENDER/RECIPIENT. `label`: trimmed1–80 if present; contactName1–160;
phone accepts Vietnamese domestic0 or +84, canonicalizes to +84 using the
enrollment contact rule, never a verified/login contact. Address reuses the
existing ApplicationAddress shape/limits (area/locality1–160, line1–500), with
country VN for this bounded slice. Reject unknown/duplicate JSON fields,
null required fields, control characters, blank values and oversized bodies.

Response: input fields plus server UUID `id`, self UUID `accountId`,
`verificationStatus: UNVERIFIED`, positive `version`, UTC `createdAt`/`updatedAt`.
List: `{items,page,size,hasNext}`. Receipt: `{operationId,targetId,outcome:
COMMITTED,version}`. No hidden totals, coordinates, eligibility, rate or PII in
receipts/errors. Preserve existing error vocabulary: VALIDATION_FAILED400,
SESSION_INVALID401, RESOURCE_NOT_FOUND404, PRECONDITION_REQUIRED428,
PRECONDITION_STALE409, IDEMPOTENCY_CONFLICT409, SERVICE_UNAVAILABLE503.

## Approved data / transaction binding

Owner: `merchant`, independent PERSONAL_OWNER scope as BC136, same placement
reason as the existing CustomerProfiles aggregate. `workflow` HTTP coordinates
public merchant API within the existing CustomerAdmission transaction. No Orders
→ Identity dependency expansion and no new domain module.

Forward-only new migration after V17; never edit deployed migrations. Candidate
`merchant.customer_address`: id/account_id UUID, kind VARCHAR(9), label VARCHAR(80),
contact_name VARCHAR(160), contact_phone VARCHAR(16), country VARCHAR(2),
administrative_area/locality VARCHAR(160), address_line VARCHAR(500), is_default
BOOLEAN, archived_at TIMESTAMPTZ nullable, version BIGINT, created_at/updated_at
TIMESTAMPTZ. PK id; UNIQUE(id,account_id); checks for valid kind/VN/canonical phone,
trimmed lengths/version/time ordering; partial UNIQUE(account_id,kind) WHERE
is_default AND archived_at IS NULL. Identity remains the security master.

Candidate immutable `merchant.customer_address_version` holds versioned address
snapshots keyed(id,version) with matching account binding. Reuse/extend the
existing merchant audit and scoped command-receipt contracts for target type
CUSTOMER_ADDRESS and the three named write operations. Runtime gets only required
DML; no DDL/history deletion. Archiving preserves address history, does not purge
PII or change shipping snapshots. Retention/purge is a separate policy dependency.

Serialize writes by self account within current CustomerAdmission ordering;
same key/canonical payload/original version replays the original result after
current authorization, different binding conflicts. Target mismatch returns404
before checking/disclosing target receipts. Default replacement bumps/audits the
previous default's version in the same transaction; its stale form then conflicts.
Source/default/history/audit/receipt commit or roll back together. No POST retry
on unknown result until an authorized receipt lookup establishes outcome.

Archived contacts cannot be restored or edited. Same-key archive replay remains
204; current-self receipt metadata remains readable for unknown-outcome recovery.
Active GET and non-archive replay reject archived targets with404 rather than
returning their historical PII. Implicit default displacement has its own version
snapshot/audit in the parent command transaction; the primary command receipt
binds its actual target/version. Existing profile/application status mappings are
unchanged; the400/409 mappings above are scoped to the new address API.

## Required evidence for the next slice

Real PG18 migration/upgrade/constraints/runtime privilege, self isolation including
search/paging/receipt, default-switch race, update/archive/stale/replay/revoke,
mandatory audit rollback. Real HTTPS UI create/edit/archive/refresh plus
unknown-outcome receipt recovery and Customer A/B denial. Preserve existing
architecture, Identity, H2/PG, report-policy and browser gates. No claim that a
saved raw address is serviceable, verified or accepted by the carrier.

## Owner Address Book refinement —2026-11-09

Latest Shipping Pilot decisions supersede the historical statement that Shipping
is closed; saving a raw address still grants no serviceability or Carrier authority.
Sổ địa chỉ is now under Management with the same existing authenticated route.

UI reuses existing reads/writes for explicit whitespace/NFC/phone normalization,
advisory duplicate comparison, direct default updates and empty-page/filter
recovery. Duplicate comparison includes kind/name/canonical phone/full location;
labels/default flags do not define contact identity. Exact active self contacts
are searched by phone/kind over successive pages, up to100 pages; an interrupted,
changing or larger scan is unknown, never a false no-duplicates result. This is
not a uniqueness constraint and does not silently merge data. No PII is persisted
in browser storage/logs. Backend remains authoritative for validation/invariants.

Frontend path `/personal/create` accepts selection query `senderAddressId=<uuid>`
and/or `recipientAddressId=<uuid>` only as UI context. New draft entry re-reads the current self-owned
active contact, validates its kind and copies a snapshot. A foreign/archived/wrong
kind/malformed selection is denied; selection cannot overwrite an existing draft.
No PII in the URL; no write until Customer explicitly saves. Existing Shipment
snapshots, Identity credentials and address serviceability remain independent.

Default shortcut uses the visible version's If-Match and existing UUID command
receipt flow; no optimistic default, blind retry or new endpoint. Archive copy
explains the accepted no-restore policy and preservation of booked snapshots.
Restore/list-archived, official administrative dictionary, geocoding, multi-label
groups and coverage inference remain unimplemented dependencies. Existing label
is sufficient for user-entered place names; Recipient remains only a self contact.

[Delivery evidence and dependency disposition](action-address-review.md).
