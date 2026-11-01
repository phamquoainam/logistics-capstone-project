-- Owner-approved Personal Address Book. Raw contacts never imply verified location or shipping eligibility.
CREATE TABLE merchant.customer_address (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL,
 kind VARCHAR(9) NOT NULL CHECK (kind IN ('SENDER','RECIPIENT')),
 label VARCHAR(80) CHECK (label IS NULL OR (char_length(btrim(label))>0 AND label !~ '[[:cntrl:]]')),
 contact_name VARCHAR(160) NOT NULL CHECK (char_length(btrim(contact_name))>0 AND contact_name !~ '[[:cntrl:]]'),
 contact_phone VARCHAR(16) NOT NULL CHECK (contact_phone ~ '^\+84[35789][0-9]{8}$'),
 country VARCHAR(2) NOT NULL CHECK (country='VN'),
 administrative_area VARCHAR(160) NOT NULL CHECK (char_length(btrim(administrative_area))>0 AND administrative_area !~ '[[:cntrl:]]'),
 locality VARCHAR(160) NOT NULL CHECK (char_length(btrim(locality))>0 AND locality !~ '[[:cntrl:]]'),
 address_line VARCHAR(500) NOT NULL CHECK (char_length(btrim(address_line))>0 AND address_line !~ '[[:cntrl:]]'),
 is_default BOOLEAN NOT NULL,
 archived_at TIMESTAMPTZ,
 version BIGINT NOT NULL CHECK (version>0),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 UNIQUE(id,account_id),
 CHECK (archived_at IS NULL OR (NOT is_default AND archived_at=updated_at))
);
CREATE UNIQUE INDEX uq_customer_address_active_default ON merchant.customer_address(account_id,kind)
 WHERE is_default AND archived_at IS NULL;
CREATE INDEX idx_customer_address_owner_page ON merchant.customer_address(account_id,updated_at DESC,id ASC)
 WHERE archived_at IS NULL;
CREATE TABLE merchant.customer_address_version (
 id UUID NOT NULL,
 account_id UUID NOT NULL,
 kind VARCHAR(9) NOT NULL CHECK (kind IN ('SENDER','RECIPIENT')),
 label VARCHAR(80) CHECK (label IS NULL OR (char_length(btrim(label))>0 AND label !~ '[[:cntrl:]]')),
 contact_name VARCHAR(160) NOT NULL CHECK (char_length(btrim(contact_name))>0 AND contact_name !~ '[[:cntrl:]]'),
 contact_phone VARCHAR(16) NOT NULL CHECK (contact_phone ~ '^\+84[35789][0-9]{8}$'),
 country VARCHAR(2) NOT NULL CHECK (country='VN'),
 administrative_area VARCHAR(160) NOT NULL CHECK (char_length(btrim(administrative_area))>0 AND administrative_area !~ '[[:cntrl:]]'),
 locality VARCHAR(160) NOT NULL CHECK (char_length(btrim(locality))>0 AND locality !~ '[[:cntrl:]]'),
 address_line VARCHAR(500) NOT NULL CHECK (char_length(btrim(address_line))>0 AND address_line !~ '[[:cntrl:]]'),
 is_default BOOLEAN NOT NULL,
 archived_at TIMESTAMPTZ,
 version BIGINT NOT NULL CHECK (version>0),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 PRIMARY KEY(id,version),
 UNIQUE(id,version,account_id),
 FOREIGN KEY(id,account_id) REFERENCES merchant.customer_address(id,account_id),
 CHECK (archived_at IS NULL OR (NOT is_default AND archived_at=updated_at))
);
ALTER TABLE merchant.customer_address ADD CONSTRAINT fk_customer_address_current_version
 FOREIGN KEY(id,version,account_id) REFERENCES merchant.customer_address_version(id,version,account_id)
 DEFERRABLE INITIALLY DEFERRED;
CREATE FUNCTION merchant.guard_customer_address_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id<>OLD.id OR NEW.account_id<>OLD.account_id OR NEW.created_at<>OLD.created_at
    OR NEW.version<>OLD.version+1 OR OLD.archived_at IS NOT NULL OR NEW.updated_at<OLD.updated_at THEN
  RAISE EXCEPTION 'CUSTOMER_ADDRESS_IMMUTABLE_BINDING_OR_VERSION' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_customer_address_identity BEFORE UPDATE ON merchant.customer_address
 FOR EACH ROW EXECUTE FUNCTION merchant.guard_customer_address_identity();
REVOKE ALL ON FUNCTION merchant.guard_customer_address_identity() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.guard_customer_address_identity() TO "${runtimeRole}";

ALTER TABLE merchant.business_audit DROP CONSTRAINT ck_business_audit_operation_binding;
ALTER TABLE merchant.business_audit DROP CONSTRAINT business_audit_policy_version_check;
ALTER TABLE merchant.business_audit ADD CONSTRAINT ck_business_audit_operation_binding CHECK (
 (target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile' AND prior_version>0)
 OR (target_type='APPLICATION' AND operation='createApplicationDraft' AND prior_version=0 AND new_version=1)
 OR (target_type='APPLICATION' AND operation='updateApplicationDraft' AND prior_version>0)
 OR (target_type='CUSTOMER_ADDRESS' AND operation='createPersonalAddress' AND prior_version=0 AND new_version=1)
 OR (target_type='CUSTOMER_ADDRESS' AND operation IN ('updatePersonalAddress','archivePersonalAddress') AND prior_version>0)
);
ALTER TABLE merchant.business_audit ADD CONSTRAINT ck_merchant_audit_policy_binding CHECK (
 (target_type='CUSTOMER_ADDRESS' AND policy_version='PERSONAL-ADDRESS-2026-11-01')
 OR (target_type IN ('CUSTOMER_PROFILE','APPLICATION') AND policy_version='S003-2026-10-27')
);
ALTER TABLE merchant.command_receipt DROP CONSTRAINT ck_merchant_receipt_operation_binding;
ALTER TABLE merchant.command_receipt ADD CONSTRAINT ck_merchant_receipt_operation_binding CHECK (
 (target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile' AND original_version>0)
 OR (target_type='APPLICATION' AND operation='createApplicationDraft' AND original_version=0 AND result_version=1)
 OR (target_type='APPLICATION' AND operation='updateApplicationDraft' AND original_version>0)
 OR (target_type='CUSTOMER_ADDRESS' AND operation='createPersonalAddress' AND original_version=0 AND result_version=1)
 OR (target_type='CUSTOMER_ADDRESS' AND operation IN ('updatePersonalAddress','archivePersonalAddress') AND original_version>0)
);
ALTER TABLE merchant.command_receipt ADD COLUMN customer_address_id UUID
 GENERATED ALWAYS AS (CASE WHEN target_type='CUSTOMER_ADDRESS' THEN target_id END) STORED;
ALTER TABLE merchant.command_receipt ADD COLUMN customer_address_version BIGINT
 GENERATED ALWAYS AS (CASE WHEN target_type='CUSTOMER_ADDRESS' THEN result_version END) STORED;
ALTER TABLE merchant.command_receipt ADD FOREIGN KEY(customer_address_id,customer_address_version,subject_account_id)
 REFERENCES merchant.customer_address_version(id,version,account_id);
CREATE UNIQUE INDEX uq_customer_address_receipt_subject_operation_key
 ON merchant.command_receipt(subject_account_id,operation,operation_key) WHERE target_type='CUSTOMER_ADDRESS';
REVOKE ALL ON merchant.customer_address,merchant.customer_address_version FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON merchant.customer_address TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.customer_address_version TO "${runtimeRole}";
-- History/audit/receipt have no runtime UPDATE/DELETE; no geocoding/Identity/Orders DML or security contact.
