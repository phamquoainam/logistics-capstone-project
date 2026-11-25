# P0-OP-01 — offline operator receipt canonicalization

Status: PROPOSED, NOT IMPLEMENTED. Source/test isolated only. Parent approved
manifest remains byte-identical with SHA256
`90c795bd7e773b6a58d0d220056cfbc5e9a5ee95b38ae2493d5b9f75a372291f`.

## Exact conflict

The manifest `canonicalization.receipt_digest` requires HMAC using the existing
configured application key ring. This includes operator receipts unless amended.
The two approved offline SQL signatures are:

```sql
merchant.configure_local_reviewer_scope(
 UUID,UUID,UUID,TEXT,UUID,TEXT[],TEXT,BIGINT,TIMESTAMPTZ,TEXT) RETURNS UUID
merchant.publish_local_application_policy(
 UUID,UUID,BIGINT,JSONB,TEXT) RETURNS UUID
```

Neither supplies a key ID/digest nor a database key capability. The existing
CustomerProofProtection keys are application configuration, not SQL-accessible
data. Existing operator receipt patterns use SQL SHA256; their existence does
not authorize silently replacing the newly approved HMAC rule. Do not store the
application secret in SQL, add a secret GUC, add table SELECT over private key
material or accept an unverified caller digest to appear compliant.

## Proposed bounded resolution

Change only the offline OPERATOR receipt canonicalization. Keep both signatures,
roles, privileges, ownership and all fifteen approved tables unchanged.

- `input_key_id = 'SQL_SHA256_1'`, explicitly an algorithm/version identifier,
  not a claim that a secret key was used.
- `input_digest = sha256(convert_to(canonical_payload::text,'UTF8'))`, computed
  inside each fixed-search-path operator function from a typed `jsonb` object.
- Canonical payload includes format/version, exact `session_user`, request UUID,
  operation, target, original version and every typed input, including the reason.
  Sort/deduplicate permission sets only after rejecting invalid/duplicate values;
  no normalization of arbitrary strings or credentials. Timestamps use epoch
  microseconds, not session timezone-dependent text.
- Replay still checks current physical Local/Test binding and authorized operator
  membership first, then the durable receipt. Same key and canonical payload
  returns the original committed result. Different input rejects; revoke never
  reactivates an old grant. Locks serialize same-key/concurrent commands.
- Receipt and event remain immutable and commit atomically with the policy/scope
  change. Runtime/PUBLIC cannot execute either operator function, write policy or
  scope, or view the private receipt payload. No new cleanup authority.
- Do not put raw request JSON/reason, keys or credentials into a report or public
  response. A digest is not encryption; keep the existing private storage and
  disclosure restrictions.

CUSTOMER and SYSTEM application receipts, Business Contact receipts and Admin
step-up receipts retain their approved existing-key-ring HMAC. This does not
change any authentication proof or business/decision authority.

## Focused acceptance

PostgreSQL18 isolated: same-key replay, payload/actor/target/version changes,
timezone-independent canonicalization, concurrent requests, revoke replay,
operator/runtime ACL denial, immutable audit/receipt and rollback. Source/test
only; no real operator membership, policy publication or demo mutation.

Until Owner accepts this exact delta, the affected V42 operator receipt paths
remain unimplemented. Other approved source/test work can continue.
