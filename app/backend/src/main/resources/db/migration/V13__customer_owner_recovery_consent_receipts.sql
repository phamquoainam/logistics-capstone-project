-- Add Owner consent without changing V12 receipt shape; deployed V12 positional inserts remain valid.
ALTER TABLE identity.customer_proof ADD UNIQUE(id,subject_id,purpose,bound_session_id,bound_auth_version,organization_id);
CREATE TABLE identity.customer_owner_consent_receipt (
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 operation_key UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation='createOwnerRecoveryConsent'),
 bound_session_id UUID NOT NULL,
 bound_auth_version BIGINT NOT NULL CHECK (bound_auth_version>0),
 proof_id UUID NOT NULL,
 proof_purpose VARCHAR(24) NOT NULL DEFAULT 'OWNER_RECOVERY_CONSENT' CHECK (proof_purpose='OWNER_RECOVERY_CONSENT'),
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK (fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint)=32),
 request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>=created_at+INTERVAL '7 days'),
 bound_organization_id UUID NOT NULL,
 source_reauthentication_proof_id UUID NOT NULL,
 source_proof_purpose VARCHAR(24) NOT NULL DEFAULT 'REAUTHENTICATION' CHECK (source_proof_purpose='REAUTHENTICATION'),
 PRIMARY KEY(account_id,operation_key),
 CONSTRAINT ck_customer_owner_consent_binding CHECK (proof_id<>source_reauthentication_proof_id),
 FOREIGN KEY(bound_session_id,account_id) REFERENCES identity.customer_auth_session(id,account_id),
 FOREIGN KEY(proof_id,account_id,proof_purpose,bound_session_id,bound_auth_version,bound_organization_id)
  REFERENCES identity.customer_proof(id,subject_id,purpose,bound_session_id,bound_auth_version,organization_id),
 FOREIGN KEY(source_reauthentication_proof_id,account_id,source_proof_purpose,bound_session_id,bound_auth_version)
  REFERENCES identity.customer_proof(id,subject_id,purpose,bound_session_id,bound_auth_version)
);
REVOKE ALL ON identity.customer_owner_consent_receipt FROM PUBLIC;
GRANT SELECT,INSERT ON identity.customer_owner_consent_receipt TO "${runtimeRole}";
-- No V12 DDL, runtime update/delete, new grant or Merchant SQL.
