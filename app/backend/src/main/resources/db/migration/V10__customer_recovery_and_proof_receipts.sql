-- Version-bound customer recovery and proof-scoped confirmations surviving CSRF rotation.
-- No previously issued enrollment/SYSTEM proof or receipt is overwritten/reinterpreted.
WITH revoked AS (
 UPDATE identity.customer_proof SET revoked_at=GREATEST(clock_timestamp(),created_at)
 WHERE purpose='CUSTOMER_RECOVERY' AND bound_auth_version IS NULL AND consumed_at IS NULL AND revoked_at IS NULL
 RETURNING id,account_id
)
INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at)
 SELECT gen_random_uuid(),NULL,account_id,'CUSTOMER_UNBOUND_PROOF_REVOKED','IDENTITY_CUSTOMER_MIGRATION',
  'Unsupported unbound customer recovery proof revoked','V10:'||id::TEXT,clock_timestamp() FROM revoked;
UPDATE identity.customer_delivery d SET status='EXPIRED',lease_id=NULL,lease_until=NULL,nonce=NULL,ciphertext=NULL,
 recipient_nonce=NULL,recipient_ciphertext=NULL,finished_at=GREATEST(clock_timestamp(),d.created_at),version=d.version+1
 FROM identity.customer_proof p WHERE d.proof_id=p.id AND p.purpose='CUSTOMER_RECOVERY' AND p.bound_auth_version IS NULL
 AND d.status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED','ACCEPTED_BY_PROVIDER');

-- Locate exactly the reviewed V8 binding CHECK; stop on drift instead of guessing a generated name.
DO $$
DECLARE binding_check TEXT; matches INTEGER;
BEGIN
 SELECT min(conname::TEXT),count(*) INTO binding_check,matches FROM pg_constraint
 WHERE conrelid='identity.customer_proof'::REGCLASS AND contype='c'
  AND pg_get_constraintdef(oid) LIKE '%bound_session_id%' AND pg_get_constraintdef(oid) LIKE '%bound_auth_version%';
 IF matches<>1 THEN RAISE EXCEPTION 'CUSTOMER_PROOF_BINDING_SCHEMA_REQUIRED'; END IF;
 EXECUTE format('ALTER TABLE identity.customer_proof DROP CONSTRAINT %I',binding_check);
END $$;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_proof_subject_session_version CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND bound_session_id IS NULL AND bound_auth_version IS NULL)
 OR (purpose='CUSTOMER_RECOVERY' AND bound_session_id IS NULL AND (bound_auth_version IS NOT NULL OR consumed_at IS NOT NULL OR revoked_at IS NOT NULL))
 OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY') AND bound_session_id IS NOT NULL AND bound_auth_version IS NOT NULL)
);
ALTER TABLE identity.customer_command_receipt DROP CONSTRAINT customer_command_receipt_operation_check;
ALTER TABLE identity.customer_command_receipt ADD CONSTRAINT customer_command_receipt_operation_check CHECK (
 operation IN ('enrollCustomer','resendCustomerVerification','verifyCustomerEmail','requestCustomerRecovery'));
ALTER TABLE identity.customer_proof ADD UNIQUE(id,purpose);
CREATE TABLE identity.customer_proof_receipt (
 proof_id UUID NOT NULL,
 proof_purpose VARCHAR(24) NOT NULL,
 operation_key UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation IN ('verifyCustomerEmail','confirmCustomerRecovery')),
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK (fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint)=32),
 request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>=created_at+INTERVAL '7 days'),
 PRIMARY KEY(proof_id,operation_key),
 FOREIGN KEY(proof_id,proof_purpose) REFERENCES identity.customer_proof(id,purpose),
 CHECK ((operation='verifyCustomerEmail' AND proof_purpose='CUSTOMER_ENROLLMENT')
  OR (operation='confirmCustomerRecovery' AND proof_purpose='CUSTOMER_RECOVERY'))
);
REVOKE ALL ON identity.customer_proof_receipt FROM PUBLIC;
GRANT SELECT,INSERT ON identity.customer_proof_receipt TO "${runtimeRole}";
-- Existing preauth confirmations remain preserved for same-binding compatibility.
-- New proof-bound receipts contain no raw proof/password/contact and grant no repeat mutation.
