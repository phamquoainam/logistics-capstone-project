-- Identity-owned current-session reauthentication receipts; V1-V11 remain immutable.
ALTER TABLE identity.customer_proof ADD UNIQUE(id,subject_id,purpose,bound_session_id,bound_auth_version);
CREATE TABLE identity.customer_security_receipt (
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 operation_key UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation='requestCustomerReauthentication'),
 bound_session_id UUID NOT NULL,
 bound_auth_version BIGINT NOT NULL CHECK (bound_auth_version>0),
 proof_id UUID NOT NULL,
 proof_purpose VARCHAR(24) NOT NULL CHECK (proof_purpose='REAUTHENTICATION'),
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK (fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint)=32),
 request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>=created_at+INTERVAL '7 days'),
 PRIMARY KEY(account_id,operation_key),
 FOREIGN KEY(bound_session_id,account_id) REFERENCES identity.customer_auth_session(id,account_id),
 FOREIGN KEY(proof_id,account_id,proof_purpose,bound_session_id,bound_auth_version)
  REFERENCES identity.customer_proof(id,subject_id,purpose,bound_session_id,bound_auth_version)
);
REVOKE ALL ON identity.customer_security_receipt FROM PUBLIC;
GRANT SELECT,INSERT ON identity.customer_security_receipt TO "${runtimeRole}";
-- No plaintext password/email, bearer material, runtime DDL/delete/receipt mutation or new SYSTEM authority.
