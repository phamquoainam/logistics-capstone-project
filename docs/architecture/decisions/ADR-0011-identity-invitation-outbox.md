# ADR-0011: Protected Identity invitations and transactional outbox

- Status: ACCEPTED for the bounded implementation direction under
  `OWNER_T_002_05_SECURE_INVITATION_STORAGE: GO` and
  `OWNER_T_002_05_TRANSACTIONAL_OUTBOX: CONDITIONAL_GO`.
- Implementation/runtime acceptance: IMPLEMENTED_VERIFICATION_PENDING.
- Authority: [ADR-0009](ADR-0009-production-identity-lifecycle.md),
  [ADR-0010](ADR-0010-identity-system-realm.md),
  [Feature 002](../../../specs/002-identity-access/spec.md).
- This records the owner's invitation/outbox scope, not a production sender,
  global key-management provider, general event bus or Feature 002 acceptance.

## Decision and alternatives

V3 has protected proof hashes, audit and receipts; V4 supplies Realm authority.
Neither can hold a recoverable protected delivery intent. The synthetic
`SYNTHETIC_NO_TOKEN` test table is not a deployed delivery mechanism.
Use one forward-only V5, one Identity-owned invitation outbox and JDK JCE;
do not modify V1-V4 or add Redis, RabbitMQ or a messaging framework.
Hash-only delivery cannot recover a link after commit; plaintext outbox storage
is rejected. HMAC verifier plus independently keyed authenticated encryption
protects against a database-only disclosure. Runtime/key-holder compromise is
outside that protection and still requires normal host/secret-store controls.

## Cryptographic and configuration boundary

- Fresh 32-byte `SecureRandom` token; unpadded canonical Base64url, 43 bytes.
  Raw token is never a public application result or JDBC parameter.
- HMAC-SHA-256 binds versioned purpose, proof ID, account ID, creation/expiry,
  verifier key ID and token. Compare digests with `MessageDigest.isEqual`.
  Read the authoritative proof and atomically consume it in the later invitation
  activation use case; cryptographic verification alone never consumes a proof.
- AES-256-GCM, fresh random 96-bit nonce, 128-bit authentication tag. AAD binds
  format v1, stable message/proof/account/Realm IDs, destination, INVITATION,
  timestamps and both key IDs. Timestamps are canonical PostgreSQL microseconds.
  The protected 43-byte token occupies 59 ciphertext/tag bytes. No URL is stored.
- `InvitationProtection` accepts two independent external key rings, active IDs
  and exactly 32-byte keys. Invalid/missing/zero/shared-purpose key material is
  rejected; no repository key, environment/file read or default/fallback exists.
  `ProductionIdentityProvisionComposition.secure` also requires an explicit
  delivery policy. Its unconfigured alternative remains fail-closed before writes.
- Up to 16 keys per ring; key IDs are 1-64 ASCII letters/digits/underscore/hyphen.
  Rotation issues with active IDs and reads with retained old IDs. Retain verifier
  keys through every corresponding live proof; retain encryption keys through
  all associated live payloads. Lost/unknown keys deny decryption/verification.
  Provision high-entropy keys outside this repository and clear caller-owned
  input buffers. Do not recycle key IDs for different material. Bound use of each
  GCM key operationally and rotate well before 2^32 encryptions; nonce uniqueness
  is probabilistic, not a promise that a database enforces an infinite counter.

JCE references: [Cipher/AEAD](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/Cipher.html),
[MessageDigest comparison](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/security/MessageDigest.html).

## Data, transaction and compatibility

V5 adds nullable `action_proof.verifier_key_id`: NULL retains the V3 legacy
representation and is never silently interpreted as HMAC. No legacy proof is
rewritten. An outbox row requires a non-null verifier ID and a composite FK to
the exact proof/account/purpose/times/key ID, one row per proof, plus Realm FK.
The table holds contact PII, protected payload, explicit states, bounded attempts,
lease ID/deadline, retry time, fixed outcome codes and terminal timestamps.
Only runtime SELECT/INSERT/UPDATE are granted; no new global/owner privileges.

Provisioning keeps the existing current actor/session/authVersion, ACTIVE Realm,
initial exact Identity Management pair, administration lock, replay/username
uniqueness and 24h receipt rules. Prepare crypto before account writes; persist
account, grants, proof, encrypted intent, audit and receipt in the same runtime
DataSource transaction. Any mandatory write failure rolls all effects back.
Exact authorized replay returns before generation, so it never reissues a token.
Resend/reissue remains separate. No accepted HTTP payload/status is changed.

## Internal delivery and retention

No public delivery endpoint/module API, scheduled sender or SMTP integration is
installed. `JdbcInvitationOutbox` is a trusted Identity-internal boundary for an
explicitly authorized future worker:

- Claim with the existing administration lock, row lock/SKIP LOCKED, random lease
  capability, explicit 1-10 attempts, 5-300s lease and 1-300s retry delay. This
  serializes small Identity delivery transactions; it is not a throughput claim.
- Before claim/open, recheck current proof, pending/unverified/no-password account,
  exact stored destination, ACTIVE Realm and exact current same-Realm grant pair.
  AAD and HMAC checks reject tampering even after a fresh database read.
- Decrypt only inside an unexpired current lease; close/wipe the worker-owned
  token buffer on every path. Provider I/O belongs outside the DB transaction.
  Use stable message identity for provider deduplication. A crash after external
  sending but before acknowledgement can cause transport duplication: no
  exactly-once email guarantee is claimed.
- SENT/permanent failure/exhaustion clears ciphertext/nonce immediately when
  outcome is recorded. Revocation/consumption/ineligibility/exact 24h expiry make
  payload unusable and eligible for purge. Purge touches at most 100 outbox rows,
  never proof/account/audit/receipt or unrelated data. Stale lease outcomes deny.
- **Physical purge is invocation-driven.** A future maintenance worker must run
  purge even with sending disabled, drain bounded batches and monitor backlog.
  No wall-clock physical deletion SLA or automatic scheduler is claimed in this
  checkpoint. Database MVCC/backups also require separately operated retention;
  clearing a column is not secure erasure of every historical storage copy.

Production issuance/delivery remains unregistered/disabled until external secret
configuration, a maintenance schedule, approved worker/sender and operational
retention are integrated and verified. QUEUED, SENT, CONSUMED and ACTIVE remain
distinct. This checkpoint implements no activation/password/session consumer.

## Verification and remaining gates

[Checkpoint evidence](../../implementation/t-002-05-identity-application-evidence.md#t-002-05-invitation-outbox-checkpoint--2026-10-23)
separates authored JUnit/PostgreSQL tests from executed Python source/report-policy
controls. No previous CI run validates this V5/issuer/outbox revision. Require
current-source compilation, real crypto/unit, V1-V5 cold start/V4 upgrade,
transaction/race/lease/failure/rotation/purge and unchanged Realm/Domain/Demo
regressions plus exact inventory/privacy/owned cleanup before runtime acceptance.
T-002-05 remains PARTIAL; Feature 002 and AC-002-01..05 remain NOT ACCEPTED.
