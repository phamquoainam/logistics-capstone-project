-- Bounded Identity invitation delivery; V1-V4 and all existing rows remain intact.
-- NULL verifier_key_id identifies legacy V3 proofs; never reinterpret them as HMAC proofs.
ALTER TABLE identity.action_proof ADD COLUMN verifier_key_id VARCHAR(64)
 CHECK (verifier_key_id IS NULL OR verifier_key_id ~ '^[A-Za-z0-9_-]{1,64}$');
ALTER TABLE identity.action_proof ADD CONSTRAINT uq_identity_proof_delivery_binding
 UNIQUE (id,account_id,purpose,created_at,expires_at,verifier_key_id);

CREATE TABLE identity.invitation_outbox (
 id UUID PRIMARY KEY,
 proof_id UUID NOT NULL UNIQUE,
 account_id UUID NOT NULL,
 realm_id UUID NOT NULL REFERENCES identity.realm(id),
 purpose VARCHAR(10) NOT NULL CHECK (purpose='INVITATION'),
 destination TEXT NOT NULL CHECK (char_length(destination)>0),
 format_version SMALLINT NOT NULL CHECK (format_version=1),
 verifier_key_id VARCHAR(64) NOT NULL CHECK (verifier_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 encryption_key_id VARCHAR(64) NOT NULL CHECK (encryption_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at=created_at+INTERVAL '24 hours'),
 status VARCHAR(8) NOT NULL CHECK (status IN ('PENDING','LEASED','SENT','FAILED','EXPIRED')),
 attempts SMALLINT NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 10),
 next_attempt_at TIMESTAMPTZ NOT NULL,
 lease_id UUID,
 lease_until TIMESTAMPTZ,
 last_outcome VARCHAR(20) CHECK (last_outcome IN ('SENT','TRANSIENT_FAILURE','PERMANENT_FAILURE','NOT_ELIGIBLE','ATTEMPTS_EXHAUSTED')),
 finished_at TIMESTAMPTZ,
 nonce BYTEA,
 ciphertext BYTEA,
 CONSTRAINT fk_identity_outbox_proof FOREIGN KEY
  (proof_id,account_id,purpose,created_at,expires_at,verifier_key_id)
  REFERENCES identity.action_proof(id,account_id,purpose,created_at,expires_at,verifier_key_id),
 CONSTRAINT ck_identity_outbox_lease CHECK (
  (status='LEASED' AND lease_id IS NOT NULL AND lease_until IS NOT NULL AND
   lease_until>created_at AND lease_until<=expires_at AND attempts>0) OR
  (status<>'LEASED' AND lease_id IS NULL AND lease_until IS NULL)),
 CONSTRAINT ck_identity_outbox_payload CHECK (
  (status IN ('PENDING','LEASED') AND nonce IS NOT NULL AND octet_length(nonce)=12 AND
   ciphertext IS NOT NULL AND octet_length(ciphertext)=59 AND finished_at IS NULL) OR
  (status IN ('SENT','FAILED','EXPIRED') AND nonce IS NULL AND ciphertext IS NULL AND
   finished_at IS NOT NULL AND finished_at>=created_at)),
 CONSTRAINT ck_identity_outbox_retry CHECK (next_attempt_at>=created_at AND next_attempt_at<=expires_at)
);
CREATE INDEX idx_identity_outbox_ready ON identity.invitation_outbox(next_attempt_at,id)
 WHERE status IN ('PENDING','LEASED');
REVOKE ALL ON TABLE identity.invitation_outbox FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON identity.invitation_outbox TO "${runtimeRole}";
-- No DELETE/TRUNCATE/DDL, no sender credentials, no plaintext bearer, no cross-module data.
