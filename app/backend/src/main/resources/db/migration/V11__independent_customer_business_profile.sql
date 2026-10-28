-- Independent personal business aggregate, never an Identity/security or tenant membership master.
CREATE TABLE merchant.customer_profile (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL UNIQUE,
 display_name VARCHAR(160) NOT NULL CHECK (char_length(display_name)>0),
 preferred_language VARCHAR(2) NOT NULL CHECK (preferred_language IN ('vi','en')),
 business_contact_email VARCHAR(254) CHECK (business_contact_email IS NULL OR char_length(business_contact_email)>0),
 business_contact_phone VARCHAR(32) CHECK (business_contact_phone IS NULL OR char_length(business_contact_phone)>0),
 version BIGINT NOT NULL CHECK (version>=2),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 UNIQUE(id,account_id)
);
CREATE TABLE merchant.customer_profile_version (
 profile_id UUID NOT NULL,
 profile_version BIGINT NOT NULL CHECK (profile_version>=2),
 account_id UUID NOT NULL,
 display_name VARCHAR(160) NOT NULL CHECK (char_length(display_name)>0),
 preferred_language VARCHAR(2) NOT NULL CHECK (preferred_language IN ('vi','en')),
 business_contact_email VARCHAR(254) CHECK (business_contact_email IS NULL OR char_length(business_contact_email)>0),
 business_contact_phone VARCHAR(32) CHECK (business_contact_phone IS NULL OR char_length(business_contact_phone)>0),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 PRIMARY KEY(profile_id,profile_version),
 UNIQUE(profile_id,profile_version,account_id),
 FOREIGN KEY(profile_id,account_id) REFERENCES merchant.customer_profile(id,account_id)
);
ALTER TABLE merchant.customer_profile ADD CONSTRAINT fk_customer_profile_current_version
 FOREIGN KEY(id,version,account_id) REFERENCES merchant.customer_profile_version(profile_id,profile_version,account_id)
 DEFERRABLE INITIALLY DEFERRED;
CREATE FUNCTION merchant.guard_customer_profile_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id<>OLD.id OR NEW.account_id<>OLD.account_id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 THEN
  RAISE EXCEPTION 'CUSTOMER_PROFILE_IMMUTABLE_BINDING_OR_VERSION' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_customer_profile_identity BEFORE UPDATE ON merchant.customer_profile
 FOR EACH ROW EXECUTE FUNCTION merchant.guard_customer_profile_identity();
REVOKE ALL ON FUNCTION merchant.guard_customer_profile_identity() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.guard_customer_profile_identity() TO "${runtimeRole}";

CREATE TABLE merchant.business_audit (
 id UUID PRIMARY KEY,
 actor_account_id UUID NOT NULL,
 target_type VARCHAR(32) NOT NULL CHECK (target_type='CUSTOMER_PROFILE'),
 target_id UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation='updateCustomerProfile'),
 prior_version BIGINT NOT NULL CHECK (prior_version>0),
 new_version BIGINT NOT NULL CHECK (new_version=prior_version+1),
 policy_version VARCHAR(64) NOT NULL CHECK (policy_version='S003-2026-10-27'),
 request_id UUID NOT NULL,
 occurred_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE merchant.command_receipt (
 subject_account_id UUID NOT NULL,
 target_type VARCHAR(32) NOT NULL CHECK (target_type='CUSTOMER_PROFILE'),
 target_id UUID NOT NULL,
 operation VARCHAR(80) NOT NULL CHECK (operation='updateCustomerProfile'),
 operation_key UUID NOT NULL,
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK (fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 canonical_request_hash BYTEA NOT NULL CHECK (octet_length(canonical_request_hash)=32),
 original_version BIGINT NOT NULL CHECK (original_version>0),
 result_version BIGINT NOT NULL CHECK (result_version=original_version+1),
 outcome VARCHAR(9) NOT NULL CHECK (outcome='COMMITTED'),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK (expires_at>=created_at+INTERVAL '7 days'),
 customer_profile_id UUID GENERATED ALWAYS AS (CASE WHEN target_type='CUSTOMER_PROFILE' THEN target_id END) STORED,
 customer_profile_version BIGINT GENERATED ALWAYS AS (CASE WHEN target_type='CUSTOMER_PROFILE' THEN result_version END) STORED,
 PRIMARY KEY(subject_account_id,target_type,target_id,operation,operation_key),
 FOREIGN KEY(customer_profile_id,customer_profile_version,subject_account_id)
  REFERENCES merchant.customer_profile_version(profile_id,profile_version,account_id)
);
CREATE INDEX idx_merchant_receipt_expiry ON merchant.command_receipt(expires_at);
REVOKE ALL ON merchant.customer_profile,merchant.customer_profile_version,merchant.business_audit,merchant.command_receipt FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON merchant.customer_profile TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.customer_profile_version,merchant.business_audit,merchant.command_receipt TO "${runtimeRole}";
-- No tenant FK, Identity SELECT/DML, authority flags, raw receipt PII or runtime delete/DDL/audit mutation.
