-- Spec003 Identity-owned pending enrollment, proof, private delivery and receipts.
-- Additive only: accepted V1-V6 and privileged invitation outbox are immutable.
CREATE TABLE identity.customer_enrollment (
 id UUID PRIMARY KEY,
 intended_account_id UUID NOT NULL UNIQUE,
 exact_email VARCHAR(254) COLLATE "C" NOT NULL CHECK (char_length(exact_email)>0),
 display_name VARCHAR(160) NOT NULL CHECK (char_length(display_name)>0),
 protected_password BYTEA,
 consent_version VARCHAR(64) NOT NULL CHECK (char_length(consent_version)>0),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>created_at),
 status VARCHAR(8) NOT NULL CHECK (status IN ('PENDING','CONSUMED','EXPIRED','REVOKED')),
 version BIGINT NOT NULL CHECK (version>0),
 UNIQUE(id,intended_account_id),
 CHECK ((status='PENDING' AND protected_password IS NOT NULL AND octet_length(protected_password)=60
    AND convert_from(protected_password,'UTF8') ~ '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$')
   OR (status<>'PENDING' AND protected_password IS NULL))
);
CREATE UNIQUE INDEX uq_customer_enrollment_pending ON identity.customer_enrollment(exact_email) WHERE status='PENDING';
CREATE INDEX idx_customer_enrollment_expiry ON identity.customer_enrollment(expires_at) WHERE status='PENDING';

-- Composite binding prevents a proof for one account naming another account's session.
ALTER TABLE identity.customer_auth_session ADD UNIQUE(id,account_id);
ALTER TABLE identity.auth_session ADD UNIQUE(id,account_id);
CREATE TABLE identity.customer_proof (
 id UUID PRIMARY KEY,
 subject_id UUID NOT NULL,
 enrollment_id UUID,
 account_id UUID REFERENCES identity.user_account(id),
 purpose VARCHAR(24) NOT NULL CHECK (purpose IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY','SECURITY_EMAIL_CHANGE',
  'BUSINESS_CONTACT','REAUTHENTICATION','OWNER_RECOVERY_CONSENT','LEGACY_CUSTOMER_LINK')),
 verifier BYTEA,
 verifier_key_id VARCHAR(64) CHECK (verifier_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 bound_session_id UUID,
 bound_auth_version BIGINT CHECK (bound_auth_version>0),
 customer_session_id UUID GENERATED ALWAYS AS (CASE WHEN purpose<>'LEGACY_CUSTOMER_LINK' THEN bound_session_id END) STORED,
 system_session_id UUID GENERATED ALWAYS AS (CASE WHEN purpose='LEGACY_CUSTOMER_LINK' THEN bound_session_id END) STORED,
 organization_id UUID,
 policy_version VARCHAR(64) NOT NULL CHECK (policy_version='S003-2026-10-27'),
 consumed_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 FOREIGN KEY(enrollment_id,subject_id) REFERENCES identity.customer_enrollment(id,intended_account_id),
 FOREIGN KEY(customer_session_id,account_id) REFERENCES identity.customer_auth_session(id,account_id),
 FOREIGN KEY(system_session_id,account_id) REFERENCES identity.auth_session(id,account_id),
 UNIQUE(id,subject_id,purpose,created_at,expires_at,verifier_key_id),
 CHECK ((purpose='CUSTOMER_ENROLLMENT' AND enrollment_id IS NOT NULL AND account_id IS NULL)
  OR (purpose<>'CUSTOMER_ENROLLMENT' AND enrollment_id IS NULL AND account_id IS NOT NULL AND subject_id=account_id)),
 CHECK (expires_at=created_at+CASE WHEN purpose='CUSTOMER_ENROLLMENT' THEN INTERVAL '24 hours' ELSE INTERVAL '15 minutes' END),
 CHECK ((consumed_at IS NULL OR (consumed_at>=created_at AND consumed_at<expires_at)) AND (revoked_at IS NULL OR revoked_at>=created_at)),
 CHECK ((purpose IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY') AND bound_session_id IS NULL AND bound_auth_version IS NULL)
  OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY') AND bound_session_id IS NOT NULL AND bound_auth_version IS NOT NULL)),
 CHECK ((purpose='OWNER_RECOVERY_CONSENT')=(organization_id IS NOT NULL)),
 CHECK ((purpose IN ('REAUTHENTICATION','OWNER_RECOVERY_CONSENT') AND verifier IS NULL AND verifier_key_id IS NULL)
  OR (purpose NOT IN ('REAUTHENTICATION','OWNER_RECOVERY_CONSENT') AND verifier IS NOT NULL AND octet_length(verifier)=32 AND verifier_key_id IS NOT NULL))
);
CREATE UNIQUE INDEX uq_customer_proof_live ON identity.customer_proof(subject_id,purpose,COALESCE(organization_id,'00000000-0000-0000-0000-000000000000'::UUID))
 WHERE consumed_at IS NULL AND revoked_at IS NULL;
CREATE INDEX idx_customer_proof_expiry ON identity.customer_proof(expires_at);

CREATE TABLE identity.customer_delivery (
 id UUID PRIMARY KEY,
 proof_id UUID NOT NULL UNIQUE,
 subject_id UUID NOT NULL,
 purpose VARCHAR(24) NOT NULL,
 verifier_key_id VARCHAR(64) NOT NULL,
 encryption_key_id VARCHAR(64) NOT NULL CHECK (encryption_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 template_version VARCHAR(64) NOT NULL CHECK (template_version='S003-EMAIL-1'),
 configuration_version VARCHAR(64) NOT NULL CHECK (configuration_version='S003-SMTP-1'),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL,
 status VARCHAR(20) NOT NULL CHECK (status IN ('QUEUED','SENDING','RETRY_WAIT','ACCEPTED_BY_PROVIDER','DELIVERED','FAILED','EXPIRED','BLOCKED')),
 attempts SMALLINT NOT NULL DEFAULT 0 CHECK (attempts BETWEEN 0 AND 5),
 due_at TIMESTAMPTZ NOT NULL CHECK (due_at>=created_at AND due_at<=expires_at),
 lease_id UUID,
 lease_until TIMESTAMPTZ,
 nonce BYTEA,
 ciphertext BYTEA,
 recipient_nonce BYTEA,
 recipient_ciphertext BYTEA,
 provider_reference VARCHAR(160),
 accepted_at TIMESTAMPTZ,
 delivered_at TIMESTAMPTZ,
 delivery_evidence BYTEA CHECK (delivery_evidence IS NULL OR octet_length(delivery_evidence)=32),
 finished_at TIMESTAMPTZ,
 version BIGINT NOT NULL CHECK (version>0),
 FOREIGN KEY(proof_id,subject_id,purpose,created_at,expires_at,verifier_key_id)
  REFERENCES identity.customer_proof(id,subject_id,purpose,created_at,expires_at,verifier_key_id),
 CHECK ((status='SENDING' AND lease_id IS NOT NULL AND lease_until IS NOT NULL AND attempts>0
   AND lease_until>created_at AND lease_until<=expires_at)
  OR (status<>'SENDING' AND lease_id IS NULL AND lease_until IS NULL)),
 CHECK ((status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED') AND nonce IS NOT NULL AND octet_length(nonce)=12
   AND ciphertext IS NOT NULL AND octet_length(ciphertext)=59 AND recipient_nonce IS NOT NULL AND octet_length(recipient_nonce)=12
   AND recipient_ciphertext IS NOT NULL AND octet_length(recipient_ciphertext) BETWEEN 19 AND 270 AND finished_at IS NULL)
  OR (status IN ('ACCEPTED_BY_PROVIDER','DELIVERED','FAILED','EXPIRED') AND nonce IS NULL AND ciphertext IS NULL
   AND recipient_nonce IS NULL AND recipient_ciphertext IS NULL AND finished_at IS NOT NULL AND finished_at>=created_at)),
 CHECK (accepted_at IS NULL OR (accepted_at>=created_at AND provider_reference IS NOT NULL)),
 CHECK ((status='DELIVERED' AND delivered_at IS NOT NULL AND delivered_at>=created_at AND delivery_evidence IS NOT NULL)
  OR (status<>'DELIVERED' AND delivered_at IS NULL AND delivery_evidence IS NULL)),
 CHECK (status<>'ACCEPTED_BY_PROVIDER' OR accepted_at IS NOT NULL)
);
CREATE INDEX idx_customer_delivery_ready ON identity.customer_delivery(due_at,id) WHERE status IN ('QUEUED','SENDING','RETRY_WAIT','BLOCKED');

CREATE TABLE identity.customer_command_receipt (
 preauth_binding_id UUID NOT NULL REFERENCES identity.customer_csrf_binding(id),
 operation_key UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation IN ('enrollCustomer','resendCustomerVerification','verifyCustomerEmail')),
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK (fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 request_fingerprint BYTEA NOT NULL CHECK (octet_length(request_fingerprint)=32),
 request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>=created_at+INTERVAL '7 days'),
 PRIMARY KEY(preauth_binding_id,operation_key)
);
REVOKE ALL ON identity.customer_enrollment,identity.customer_proof,identity.customer_delivery,identity.customer_command_receipt FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON identity.customer_enrollment,identity.customer_proof,identity.customer_delivery TO "${runtimeRole}";
GRANT SELECT,INSERT ON identity.customer_command_receipt TO "${runtimeRole}";
-- No runtime deletion, audit/receipt mutation, secrets, plaintext proofs or plaintext delivery recipients.
