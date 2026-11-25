# P0-BC-03 — submitted Business Contact fact read

Status: **PROPOSED — NOT IMPLEMENTED; OWNER TECHNICAL APPROVAL REQUIRED**.
Parent manifest remains byte-identical:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
P0-BC-01/02 and P0-OP-01 remain approved; this does not reopen them.

## Exact gap and evidence

- P0-PORT-03 / `CustomerBusinessContacts.consumeAttestation(...)` returns void.
  The existing request/confirm API exposes a fact only during genuine confirmation.
- P0-DTO-18 ReviewData and P0-DTO-24 ApplicationRevision require the authoritative
  `contactVerifiedAt`, not the submission timestamp. P0-DB-08 stores only the
  attestation UUID, not a duplicate Identity verification master.
- MA-EVID-01 requires review/approval to recheck current applicant eligibility,
  unchanged contact binding and security revocation, while ordinary proof/session
  expiry after submission must not invalidate an immutable submitted fact.
- The current public port has no submitted-fact reader. Re-consuming it is forbidden;
  asking for the old email proof, trusting client `verifiedAt`, caching a confirmation
  in memory, or reading Identity private tables from Merchant is not an alternative.

Customer upload/contact/Submit can be tested independently. Review-data contact
metadata and final decision/activation cannot be accepted until this gap is closed.
No fabricated timestamp or weakened revocation check is implemented.

## P0-BC-03-A [PROPOSED] — two read-only overloads on the existing port

Keep every existing method/signature, including void consumeAttestation, unchanged.
Add only:

```java
CustomerBusinessContacts.BusinessContactAttestation submittedAttestation(
    CustomerAdmission.Context actor, UUID attestationId, UUID applicationId,
    long revision, String expectedEmail);

CustomerBusinessContacts.BusinessContactAttestation submittedAttestation(
    AdminCommandContext actor, UUID attestationId, UUID applicationId,
    UUID applicantId, long revision, String expectedEmail);
```

Return the existing approved BusinessContactAttestation record
`{id, applicationId, verifiedAt, expiresAt}`. No email, proof, recipient envelope,
credential, new DTO, HTTP path, permission, table, column, SQL signature or ACL.

### Admission and invariants

1. Customer overload requires the exact current CustomerAdmission context and
   caller-owned applicant. It is called immediately after successful consume in
   the same physical transaction; consumed revision must match.
2. Admin overload requires a genuine current Identity-owned AdminCommandContext
   on the same DataSource, existing ENTRY + MERCHANT admission, and an applicant
   already discovered/locked in `context.customer(applicantId)` and still eligible.
   It neither discovers arbitrary accounts nor grants Merchant reviewer scope.
3. Identity alone reads its existing attestation/proof/receipt tables. Verify exact
   account/application/revision, consumed state, protected expected-email binding,
   no attestation/proof revocation, and current applicant security version matching
   the verified binding. A security-version change requires a fresh contact proof.
4. Ordinary elapsed proof TTL or expiry/logout of the original Customer session
   after submission does not reject the historical fact. Current Customer/Admin
   authentication and current applicant eligibility are still mandatory.
5. Merchant must separately enforce current policy/scope and evidence authority
   before review disclosure or decisions. This Identity reader conveys no Merchant,
   Platform, Finance, Customer-credential or cross-Organization authority.
6. No consume/update/audit side effect or repeated email. Generic proof rejection,
   without sensitive existence or field diagnostics. Constructed, captured,
   suspended-transaction or unknown referenced-subject contexts deny.

## P0-BC-03-B [PROPOSED] — use without schema expansion

After consume, read the fact in the same Customer transaction. Freeze its genuine
verifiedAt, attestation/application/revision binding in the already approved
submission event protected_payload using configured retained-key protection.
Include it in the submitted content digest. No extra field in MerchantApplications.Input.
Revision projections read the immutable protected submission fact. Review/decision
calls the Admin overload and compares the same authoritative binding; it never
silently turns an expired proof into a new attestation.

Required isolated PG18 tests: exact positive consumed fact; wrong owner/application/
revision/email; no unconsumed/revoked access; current security-version change;
ordinary post-submission TTL/session expiry retained; fake/captured/suspended
contexts; unknown applicant reference; rollback and replay without second consume,
audit, email or receipt; public response privacy. Preserve V41 bytes.

## Boundary

Only source/test on disposable PostgreSQL18 after exact approval. No demo/shared/
Admin-UAT/Production mutation, new grant/operator, migration repair or historical
SQL edit. No commit/push/merge. This is a missing public Identity fact contract,
not a request to approve V42 business scope again.

AGENTS.md: “Các path/DTO/bảng ghi [PROPOSED] phải được duyệt trước khi làm production
feature.” The Owner's current fail-closed direction also requires exact authority/
API deltas to be surfaced before implementation; P0-PORT-03 is an approved exact
public interface, so these new overloads are held pending.
