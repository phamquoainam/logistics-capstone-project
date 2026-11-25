# P0 BUSINESS_CONTACT — bounded security correction for Owner review

Status: PROPOSED, NOT IMPLEMENTED. Parent approved manifest remains byte-identical:
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.
SOURCE/TEST only. This is one consolidated correction to two omissions found while
implementing the approved Identity proof workflow; no broader P0 reapproval.

## Evidence and affected gate

1. Existing `JdbcCustomerDeliveryStore.update()` intentionally clears encrypted
   recipient fields after SMTP acceptance. A later confirmation therefore cannot
   recover the original BUSINESS_CONTACT destination from that delivery row.
   Approved P0-DB-02 can store a verified destination only after genuine proof;
   its verified_at is NOT NULL and must match consumed_at. Creating an already
   verified attestation at request time would fabricate verification.
2. P0-PORT-03 consumeAttestation currently takes context/id/application/revision,
   without the submitted representativeEmail. Identity cannot compare its durable
   verified-address digest to that submitted contact. Reading Merchant private SQL,
   accepting a client flag or restricting business contact to personal email is
   not an authorized fix.

BUSINESS_CONTACT completion and dependent submit/activation acceptance remain
blocked on this correction. Existing command admission, governance step-up,
independent readers and scoped-authority work can continue. No placeholder proof
or fake attestation is inserted to pass tests.

## P0-BC-01 — encrypted pending destination, exact schema delta

Add one nullable column to the already proposed new receipt table (not a16th table):

```sql
ALTER TABLE identity.governance_proof_receipt
 ADD COLUMN protected_address BYTEA,
 ADD CONSTRAINT ck_governance_pending_address CHECK (
   (operation='REQUEST_BUSINESS_CONTACT' AND protected_address IS NOT NULL
      AND octet_length(protected_address) BETWEEN 34 AND 512)
   OR (operation<>'REQUEST_BUSINESS_CONTACT' AND protected_address IS NULL));
```

The opaque envelope contains a finite format version, existing configured
encryption-key ID,12-byte nonce and existing BUSINESS_CONTACT purpose/proof-bound
recipient ciphertext. It is neither a plaintext address nor a verification token.
Reuse current CustomerProofProtection and configured key ring; no new provider,
algorithm, generated runtime key or fallback key. Wrong key/context/tamper denies.

Persist it in the same transaction as proof/delivery/request receipt/audit. Receipt
remains immutable INSERT/SELECT only; it never appears in HTTP DTOs/logs/export.
After possession validation, copy the protected envelope into the verified
attestation; retain existing delivery clearing behavior. No deletion/retention
cleanup authority is added. Existing privacy/purge policy remains a dependency.

## P0-BC-02 — exact port delta, no HTTP/API/DTO addition

Replace only this method within the already approved CustomerBusinessContacts:

```java
void consumeAttestation(CustomerAdmission.Context actor, UUID attestationId,
    UUID applicationId, long revision, String expectedEmail);
```

Merchant passes representativeEmail from its locked candidate submission, never
an actor/authority flag. Identity recomputes the same exact-address fingerprint
using the retained verifier-key ID and checks actor/application/session/authVersion,
expiry/revocation, single-use revision and constant-time address-digest equality.
Mismatch fails closed; no attestation/evidence/consent shortcut. Identical retry
is resolved by the enclosing business receipt before a second consume.

No new HTTP path, request field, permission, Identity grant, operator role or
principal linking is introduced. No correction to historical V1–V40. V41 is still
an unreleased source/test migration; edit that source only after approval and
validate on fresh owned disposable targets. If it ever reaches a durable approved
deployment first, use a separately numbered forward migration instead.

## Required acceptance for the correction

- Controlled SMTP accepts mail and clears delivery ciphertext; later genuine
  confirmation still succeeds from durable protected metadata.
- Restart/recomposition and same-key request/confirmation replay preserve outcome.
- Wrong application, actor, session, purpose, address, key, nonce/ciphertext,
  expiry or revoked subject denies; no duplicate proof/audit/delivery.
- Pending recipient metadata is never represented as verified, nor returned in
  Customer/Admin aggregate DTOs or diagnostics.
- Submitted contact changed after verification is rejected; unchanged exact
  contact consumes once in the same atomic submission transaction.
- V40 compatibility, V41 migration/ACL/direct-DML and private delivery regression.

Approval requested only for P0-BC-01 and P0-BC-02 together, source/test isolation
unchanged. No demo/shared/UAT deployment, real mail, grant or activation authorized.
