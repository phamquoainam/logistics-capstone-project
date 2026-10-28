-- Applicant drafts only. Submission/activation remain unavailable until their reviewed gates exist.
CREATE TABLE merchant.application (
 id UUID PRIMARY KEY,
 applicant_account_id UUID NOT NULL,
 state VARCHAR(24) NOT NULL DEFAULT 'DRAFT' CHECK (state='DRAFT'),
 current_revision BIGINT NOT NULL DEFAULT 1 CHECK (current_revision=1),
 version BIGINT NOT NULL CHECK (version>0),
 draft JSONB NOT NULL CHECK (jsonb_typeof(draft)='object'),
 normalized_jurisdiction TEXT GENERATED ALWAYS AS (normalize(btrim(draft->>'jurisdiction'),NFC)) STORED,
 entity_type TEXT GENERATED ALWAYS AS (draft->>'entityType') STORED,
 normalized_legal_identifier TEXT GENERATED ALWAYS AS (normalize(translate(draft->>'legalIdentifier',' -./_',''),NFC)) STORED,
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 UNIQUE(id,applicant_account_id),
 CHECK (entity_type IS NULL OR entity_type IN ('COMPANY','HOUSEHOLD_BUSINESS')),
 CHECK (normalized_legal_identifier IS NULL OR normalized_legal_identifier<>''),
 CHECK (octet_length(draft::text)<=32768)
);
CREATE UNIQUE INDEX uq_merchant_active_application_business_key
 ON merchant.application(normalized_jurisdiction,entity_type,normalized_legal_identifier)
 WHERE state IN ('DRAFT','SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION');
CREATE INDEX idx_merchant_application_applicant ON merchant.application(applicant_account_id,created_at DESC,id DESC);
-- Private history preserves exact same-key outcomes after subsequent edits; not a submitted snapshot.
CREATE TABLE merchant.application_draft_version (
 application_id UUID NOT NULL,
 application_version BIGINT NOT NULL CHECK (application_version>0),
 applicant_account_id UUID NOT NULL,
 draft JSONB NOT NULL CHECK (jsonb_typeof(draft)='object' AND octet_length(draft::text)<=32768),
 created_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL CHECK (updated_at>=created_at),
 PRIMARY KEY(application_id,application_version),
 UNIQUE(application_id,application_version,applicant_account_id),
 FOREIGN KEY(application_id,applicant_account_id) REFERENCES merchant.application(id,applicant_account_id)
);
ALTER TABLE merchant.application ADD CONSTRAINT fk_application_current_draft
 FOREIGN KEY(id,version,applicant_account_id)
 REFERENCES merchant.application_draft_version(application_id,application_version,applicant_account_id)
 DEFERRABLE INITIALLY DEFERRED;
CREATE FUNCTION merchant.guard_application_draft_identity() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id<>OLD.id OR NEW.applicant_account_id<>OLD.applicant_account_id OR NEW.created_at<>OLD.created_at
    OR NEW.version<>OLD.version+1 OR OLD.state<>'DRAFT' OR NEW.state<>'DRAFT' OR NEW.current_revision<>OLD.current_revision THEN
  RAISE EXCEPTION 'APPLICATION_IMMUTABLE_BINDING_OR_VERSION' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_application_draft_identity BEFORE UPDATE ON merchant.application
 FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_draft_identity();
REVOKE ALL ON FUNCTION merchant.guard_application_draft_identity() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.guard_application_draft_identity() TO "${runtimeRole}";
ALTER TABLE merchant.business_audit DROP CONSTRAINT business_audit_target_type_check;
ALTER TABLE merchant.business_audit DROP CONSTRAINT business_audit_operation_check;
ALTER TABLE merchant.business_audit DROP CONSTRAINT business_audit_prior_version_check;
ALTER TABLE merchant.business_audit ADD CONSTRAINT ck_business_audit_operation_binding CHECK (
 (target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile' AND prior_version>0)
 OR (target_type='APPLICATION' AND operation='createApplicationDraft' AND prior_version=0 AND new_version=1)
 OR (target_type='APPLICATION' AND operation='updateApplicationDraft' AND prior_version>0)
);
ALTER TABLE merchant.command_receipt DROP CONSTRAINT command_receipt_target_type_check;
ALTER TABLE merchant.command_receipt DROP CONSTRAINT command_receipt_operation_check;
ALTER TABLE merchant.command_receipt DROP CONSTRAINT command_receipt_original_version_check;
ALTER TABLE merchant.command_receipt ADD CONSTRAINT ck_merchant_receipt_operation_binding CHECK (
 (target_type='CUSTOMER_PROFILE' AND operation='updateCustomerProfile' AND original_version>0)
 OR (target_type='APPLICATION' AND operation='createApplicationDraft' AND original_version=0 AND result_version=1)
 OR (target_type='APPLICATION' AND operation='updateApplicationDraft' AND original_version>0)
);
ALTER TABLE merchant.command_receipt ADD COLUMN application_id UUID
 GENERATED ALWAYS AS (CASE WHEN target_type='APPLICATION' THEN target_id END) STORED;
ALTER TABLE merchant.command_receipt ADD COLUMN application_version BIGINT
 GENERATED ALWAYS AS (CASE WHEN target_type='APPLICATION' THEN result_version END) STORED;
ALTER TABLE merchant.command_receipt ADD FOREIGN KEY(application_id,application_version,subject_account_id)
 REFERENCES merchant.application_draft_version(application_id,application_version,applicant_account_id);
CREATE UNIQUE INDEX uq_application_receipt_subject_operation_key
 ON merchant.command_receipt(subject_account_id,operation,operation_key) WHERE target_type='APPLICATION';
REVOKE ALL ON merchant.application,merchant.application_draft_version FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON merchant.application TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.application_draft_version TO "${runtimeRole}";
-- No cross-owner FK/SQL, organization/membership creation, runtime delete or mutable audit/receipt.
