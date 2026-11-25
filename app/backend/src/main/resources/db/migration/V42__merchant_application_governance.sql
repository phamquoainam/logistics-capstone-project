-- Owner-approved P0 + P0-OP-01. Source/test default-off; no publication, grants or seed rows.
CREATE TABLE merchant.application_policy (
 id UUID PRIMARY KEY, code VARCHAR(64) NOT NULL CHECK(code ~ '^[A-Z0-9_-]{1,64}$'),
 version BIGINT NOT NULL CHECK(version>0), environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 entity_types TEXT[] NOT NULL CHECK(cardinality(entity_types) BETWEEN 1 AND 2 AND entity_types <@ ARRAY['COMPANY','HOUSEHOLD_BUSINESS']::TEXT[] AND array_position(entity_types,NULL) IS NULL),
 jurisdiction_codes TEXT[] NOT NULL CHECK(cardinality(jurisdiction_codes) BETWEEN 1 AND 100 AND array_position(jurisdiction_codes,NULL) IS NULL),
 consent_version VARCHAR(64) NOT NULL CHECK(length(btrim(consent_version))>0),
 consent_statements JSONB NOT NULL CHECK(jsonb_typeof(consent_statements) IS NOT DISTINCT FROM 'object'
  AND consent_statements ?& ARRAY['accuracy','representation'] AND consent_statements-ARRAY['accuracy','representation']='{}'::JSONB
  AND jsonb_typeof(consent_statements->'accuracy') IS NOT DISTINCT FROM 'string'
  AND jsonb_typeof(consent_statements->'representation') IS NOT DISTINCT FROM 'string'
  AND length(consent_statements->>'accuracy') BETWEEN 1 AND 1000 AND length(consent_statements->>'representation') BETWEEN 1 AND 1000
  AND length(btrim(consent_statements->>'accuracy'))>0 AND length(btrim(consent_statements->>'representation'))>0
  AND consent_statements->>'accuracy'=normalize(consent_statements->>'accuracy',NFC) AND consent_statements->>'representation'=normalize(consent_statements->>'representation',NFC)
  AND (consent_statements->>'accuracy') !~ '[[:cntrl:]]' AND (consent_statements->>'representation') !~ '[[:cntrl:]]'),
 required_evidence TEXT[] NOT NULL CHECK(required_evidence @> ARRAY['REGISTRATION','REPRESENTATIVE_AUTHORIZATION']::TEXT[] AND required_evidence <@ ARRAY['REGISTRATION','REPRESENTATIVE_AUTHORIZATION']::TEXT[] AND cardinality(required_evidence)=2),
 review_mode VARCHAR(32) NOT NULL CHECK(review_mode='TWO_PERSON'), state VARCHAR(12) NOT NULL CHECK(state IN ('PUBLISHED','RETIRED')),
 valid_from TIMESTAMPTZ NOT NULL, valid_to TIMESTAMPTZ CHECK(valid_to>valid_from),
 published_digest BYTEA NOT NULL CHECK(octet_length(published_digest)=32), created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(code,version,environment)
);
CREATE TABLE merchant.business_reviewer_scope (
 id UUID PRIMARY KEY, account_id UUID NOT NULL, realm_id UUID NOT NULL, scope_type VARCHAR(16) NOT NULL CHECK(scope_type IN ('POLICY','ORGANIZATION')),
 policy_id UUID REFERENCES merchant.application_policy(id), organization_id UUID REFERENCES merchant.organization(id),
 permissions TEXT[] NOT NULL CHECK(cardinality(permissions) BETWEEN 1 AND 10 AND array_position(permissions,NULL) IS NULL
  AND permissions <@ ARRAY['application.read','application.evidence.read','application.assign','application.review','application.decide','org.read','org.suspend','org.reinstate','audit.read','task.read']::TEXT[]),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')), version BIGINT NOT NULL CHECK(version>0),
 expires_at TIMESTAMPTZ NOT NULL, operator_request_id UUID NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CHECK((scope_type='POLICY' AND policy_id IS NOT NULL AND organization_id IS NULL)
  OR (scope_type='ORGANIZATION' AND policy_id IS NULL AND organization_id IS NOT NULL AND permissions <@ ARRAY['org.read','org.suspend','org.reinstate','audit.read']::TEXT[]))
);
CREATE UNIQUE INDEX reviewer_scope_active_target ON merchant.business_reviewer_scope(account_id,realm_id,scope_type,(coalesce(policy_id,organization_id))) WHERE state='ACTIVE';
CREATE INDEX reviewer_scope_policy ON merchant.business_reviewer_scope(policy_id,account_id) WHERE state='ACTIVE';
CREATE TABLE merchant.governance_event (
 id UUID PRIMARY KEY, actor_kind VARCHAR(16) NOT NULL CHECK(actor_kind IN ('CUSTOMER','SYSTEM','OPERATOR')),
 actor_id UUID, operator_principal TEXT, realm_id UUID, target_type VARCHAR(16) NOT NULL CHECK(target_type IN ('APPLICATION','ORGANIZATION','SCOPE','POLICY')),
 target_id UUID NOT NULL, application_id UUID REFERENCES merchant.application(id), organization_id UUID REFERENCES merchant.organization(id), policy_id UUID REFERENCES merchant.application_policy(id),
 operation VARCHAR(40) NOT NULL CHECK(operation IN ('PUBLISH_POLICY','RETIRE_POLICY','ISSUE_SCOPE','REVOKE_SCOPE','SUBMIT','RESUBMIT','WITHDRAW','ASSIGN_REVIEWER','BEGIN_REVIEW','READY_FOR_DECISION','REQUEST_INFORMATION','APPROVE','REJECT','SUSPEND','REINSTATE','READ_DISCLOSURE','RESERVE_EVIDENCE','FINALIZE_EVIDENCE','ARCHIVE_EVIDENCE')),
 revision BIGINT CHECK(revision>0), prior_version BIGINT NOT NULL CHECK(prior_version>=0), new_version BIGINT NOT NULL,
 reason_code VARCHAR(40) NOT NULL CHECK(reason_code IN ('OPERATOR_POLICY','OPERATOR_SCOPE','APPLICANT_SUBMISSION','APPLICANT_WITHDRAWAL','REVIEWER_ASSIGNMENT','REVIEW_STARTED','CHECKLIST_COMPLETE','INFORMATION_REQUIRED','INDEPENDENT_APPROVAL','INDEPENDENT_REJECTION','ORGANIZATION_SUSPENDED','ORGANIZATION_REINSTATED','MERCHANT_REVIEW','EVIDENCE_RESERVED','EVIDENCE_FINALIZED','EVIDENCE_ARCHIVED')),
 protected_payload BYTEA, request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE(request_id,operation,target_id),
 CHECK((actor_kind='OPERATOR' AND actor_id IS NULL AND operator_principal IS NOT NULL AND octet_length(operator_principal) BETWEEN 1 AND 63)
   OR (actor_kind IN ('CUSTOMER','SYSTEM') AND actor_id IS NOT NULL AND operator_principal IS NULL)),
 CHECK((operation='READ_DISCLOSURE' AND new_version=prior_version) OR (operation<>'READ_DISCLOSURE' AND new_version=prior_version+1)),
 CHECK((target_type='APPLICATION' AND application_id=target_id AND (policy_id IS NOT NULL OR (actor_kind='CUSTOMER' AND operation='WITHDRAW')) AND organization_id IS NULL)
  OR (target_type='ORGANIZATION' AND organization_id=target_id AND application_id IS NULL)
  OR (target_type='POLICY' AND policy_id=target_id AND application_id IS NULL AND organization_id IS NULL)
  OR (target_type='SCOPE' AND application_id IS NULL AND ((policy_id IS NOT NULL AND organization_id IS NULL) OR (policy_id IS NULL AND organization_id IS NOT NULL))))
);
CREATE TABLE merchant.governance_receipt (
 actor_kind VARCHAR(16) NOT NULL CHECK(actor_kind IN ('CUSTOMER','SYSTEM','OPERATOR')), actor_binding TEXT NOT NULL,
 realm_id UUID, operation VARCHAR(40) NOT NULL, operation_key UUID NOT NULL CHECK(operation_key<>'00000000-0000-0000-0000-000000000000'),
 target_type VARCHAR(16) NOT NULL CHECK(target_type IN ('APPLICATION','ORGANIZATION','SCOPE','POLICY')), target_id UUID NOT NULL,
 original_version BIGINT NOT NULL CHECK(original_version>=0), result_version BIGINT NOT NULL CHECK(result_version=original_version+1),
 input_key_id VARCHAR(64) NOT NULL, input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 event_id UUID NOT NULL REFERENCES merchant.governance_event(id), outcome VARCHAR(12) NOT NULL CHECK(outcome='COMMITTED'),
 recorded_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at>=recorded_at+INTERVAL '7 days'),
 PRIMARY KEY(actor_kind,actor_binding,operation,operation_key),
 CHECK((actor_kind='OPERATOR' AND octet_length(actor_binding) BETWEEN 1 AND 63 AND input_key_id='SQL_SHA256_1')
   OR (actor_kind IN ('CUSTOMER','SYSTEM') AND actor_binding ~ '^[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}$' AND input_key_id<>'SQL_SHA256_1'))
);
CREATE TABLE merchant.application_revision (
 application_id UUID NOT NULL, revision BIGINT NOT NULL CHECK(revision>0), applicant_id UUID NOT NULL,
 application_version BIGINT NOT NULL CHECK(application_version>0), submitted_input JSONB NOT NULL CHECK(jsonb_typeof(submitted_input)='object' AND octet_length(submitted_input::TEXT)<=32768),
 activation_display_name VARCHAR(160) NOT NULL CHECK(length(btrim(activation_display_name))>0 AND activation_display_name !~ '[[:cntrl:]]' AND activation_display_name=normalize(activation_display_name,NFC)),
 content_digest BYTEA NOT NULL CHECK(octet_length(content_digest)=32), policy_id UUID NOT NULL REFERENCES merchant.application_policy(id), policy_version BIGINT NOT NULL CHECK(policy_version>0),
 consent_version VARCHAR(64) NOT NULL, contact_attestation_id UUID NOT NULL, submitted_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(application_id,revision), UNIQUE(application_id,application_version),
 FOREIGN KEY(application_id,applicant_id) REFERENCES merchant.application(id,applicant_account_id)
);
CREATE TABLE merchant.application_evidence_reservation (
 id UUID PRIMARY KEY, application_id UUID NOT NULL, applicant_id UUID NOT NULL, operation_key UUID NOT NULL,
 input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32), purpose VARCHAR(40) NOT NULL CHECK(purpose IN ('REGISTRATION','REPRESENTATIVE_AUTHORIZATION')),
 state VARCHAR(12) NOT NULL CHECK(state IN ('RESERVED','FINALIZED','FAILED','EXPIRED')), created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at=created_at+INTERVAL '15 minutes'), finalized_at TIMESTAMPTZ,
 UNIQUE(applicant_id,operation_key), FOREIGN KEY(application_id,applicant_id) REFERENCES merchant.application(id,applicant_account_id),
 CHECK((state='RESERVED' AND finalized_at IS NULL) OR (state<>'RESERVED' AND finalized_at IS NOT NULL AND finalized_at>=created_at))
);
CREATE TABLE merchant.application_evidence (
 id UUID PRIMARY KEY, application_id UUID NOT NULL REFERENCES merchant.application(id), reservation_id UUID NOT NULL UNIQUE REFERENCES merchant.application_evidence_reservation(id),
 blob_key UUID NOT NULL UNIQUE, purpose VARCHAR(40) NOT NULL CHECK(purpose IN ('REGISTRATION','REPRESENTATIVE_AUTHORIZATION')),
 state VARCHAR(16) NOT NULL CHECK(state IN ('CLEAN','QUARANTINED','ARCHIVED')), mime VARCHAR(80) NOT NULL CHECK(mime IN ('application/pdf','image/png','image/jpeg')),
 bytes BIGINT NOT NULL CHECK(bytes BETWEEN 1 AND 5242880), sha256 CHAR(64) NOT NULL CHECK(sha256 ~ '^[a-f0-9]{64}$'),
 scan_version VARCHAR(64), screened_at TIMESTAMPTZ, version BIGINT NOT NULL CHECK(version>0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 UNIQUE(id,application_id), CHECK(state<>'CLEAN' OR (scan_version IS NOT NULL AND length(btrim(scan_version))>0 AND screened_at IS NOT NULL))
);
CREATE TABLE merchant.application_revision_evidence (
 application_id UUID NOT NULL, revision BIGINT NOT NULL, evidence_id UUID NOT NULL, sha256 CHAR(64) NOT NULL CHECK(sha256 ~ '^[a-f0-9]{64}$'),
 PRIMARY KEY(application_id,revision,evidence_id),
 FOREIGN KEY(application_id,revision) REFERENCES merchant.application_revision(application_id,revision),
 FOREIGN KEY(evidence_id,application_id) REFERENCES merchant.application_evidence(id,application_id)
);
CREATE TABLE merchant.application_reviewer_assignment (
 application_id UUID PRIMARY KEY REFERENCES merchant.application(id), reviewer_account_id UUID NOT NULL,
 reviewer_scope_id UUID NOT NULL REFERENCES merchant.business_reviewer_scope(id), policy_id UUID NOT NULL REFERENCES merchant.application_policy(id),
 version BIGINT NOT NULL CHECK(version>0), event_id UUID NOT NULL REFERENCES merchant.governance_event(id), assigned_at TIMESTAMPTZ NOT NULL
);

-- P0-ALTER-01/02: no historical migration rewrite and no mixed-version DRAFT projection.
ALTER TABLE merchant.application DROP CONSTRAINT application_state_check;
ALTER TABLE merchant.application DROP CONSTRAINT application_current_revision_check;
ALTER TABLE merchant.application ADD CONSTRAINT application_workflow_state CHECK(state IN ('DRAFT','SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION','APPROVED','REJECTED','WITHDRAWN'));
ALTER TABLE merchant.application ADD CONSTRAINT application_workflow_revision CHECK(current_revision>=1);
ALTER TABLE merchant.application ADD COLUMN policy_id UUID REFERENCES merchant.application_policy(id);
ALTER TABLE merchant.application ADD COLUMN first_submitted_at TIMESTAMPTZ;
ALTER TABLE merchant.application ADD COLUMN terminal_at TIMESTAMPTZ;
ALTER TABLE merchant.application ADD COLUMN submitted_revision BIGINT;
ALTER TABLE merchant.application ADD CONSTRAINT application_submitted_revision FOREIGN KEY(id,submitted_revision)
 REFERENCES merchant.application_revision(application_id,revision) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE merchant.application ADD CONSTRAINT application_submission_binding CHECK(
 (submitted_revision IS NULL AND current_revision=1 AND policy_id IS NULL AND first_submitted_at IS NULL AND state IN ('DRAFT','WITHDRAWN'))
 OR (submitted_revision IS NOT NULL AND submitted_revision=current_revision AND policy_id IS NOT NULL AND first_submitted_at IS NOT NULL AND state<>'DRAFT'));
ALTER TABLE merchant.application ADD CONSTRAINT application_terminal_binding CHECK((state IN ('APPROVED','REJECTED','WITHDRAWN'))=(terminal_at IS NOT NULL));
CREATE INDEX application_submission_time ON merchant.application(first_submitted_at DESC,id DESC) WHERE first_submitted_at IS NOT NULL;
CREATE INDEX application_policy_state_time ON merchant.application(policy_id,state,first_submitted_at DESC,id DESC) WHERE first_submitted_at IS NOT NULL;
CREATE INDEX application_reviewer ON merchant.application_reviewer_assignment(reviewer_account_id,application_id);
CREATE INDEX reviewer_current_authority ON merchant.business_reviewer_scope(account_id,realm_id,state,expires_at);
CREATE INDEX application_governance_history ON merchant.governance_event(application_id,occurred_at DESC,id DESC);
CREATE INDEX organization_governance_history ON merchant.governance_event(organization_id,occurred_at DESC,id DESC);

CREATE FUNCTION merchant.guard_application_workflow_v42() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'APPLICATION_DELETE_DENIED' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'DRAFT' OR NEW.current_revision<>1 OR NEW.version<>1 OR NEW.policy_id IS NOT NULL OR NEW.submitted_revision IS NOT NULL THEN RAISE EXCEPTION 'APPLICATION_MUST_BEGIN_AS_DRAFT' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF NEW.id<>OLD.id OR NEW.applicant_account_id<>OLD.applicant_account_id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1
  OR NEW.updated_at<OLD.updated_at OR OLD.state IN ('APPROVED','REJECTED','WITHDRAWN')
  OR (OLD.first_submitted_at IS NOT NULL AND NEW.first_submitted_at IS DISTINCT FROM OLD.first_submitted_at) THEN RAISE EXCEPTION 'APPLICATION_IMMUTABLE_BINDING_OR_VERSION' USING ERRCODE='23514';END IF;
 IF (OLD.state='DRAFT' AND NEW.state='DRAFT') OR (OLD.state='NEEDS_INFORMATION' AND NEW.state='NEEDS_INFORMATION') THEN
  IF NEW.current_revision<>OLD.current_revision OR NEW.submitted_revision IS DISTINCT FROM OLD.submitted_revision OR NEW.policy_id IS DISTINCT FROM OLD.policy_id THEN RAISE EXCEPTION 'DRAFT_CANNOT_REWRITE_SUBMISSION' USING ERRCODE='23514';END IF;
 ELSIF OLD.state IN ('DRAFT','NEEDS_INFORMATION') AND NEW.state='SUBMITTED' THEN
  IF NEW.draft<>OLD.draft OR NEW.current_revision<>(CASE WHEN OLD.state='DRAFT' THEN 1 ELSE OLD.current_revision+1 END) THEN RAISE EXCEPTION 'APPLICATION_SUBMISSION_REVISION_INVALID' USING ERRCODE='23514';END IF;
 ELSIF (OLD.state='SUBMITTED' AND NEW.state IN ('SUBMITTED','UNDER_REVIEW')) OR (OLD.state='UNDER_REVIEW' AND NEW.state IN ('UNDER_REVIEW','NEEDS_INFORMATION','REJECTED'))
   OR (OLD.state IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION') AND NEW.state='WITHDRAWN') THEN
  IF NEW.draft<>OLD.draft OR NEW.current_revision<>OLD.current_revision OR NEW.submitted_revision IS DISTINCT FROM OLD.submitted_revision OR NEW.policy_id IS DISTINCT FROM OLD.policy_id THEN RAISE EXCEPTION 'APPLICATION_REVIEW_CANNOT_EDIT_SNAPSHOT' USING ERRCODE='23514';END IF;
 ELSE
  -- V43 alone introduces the receipt-backed atomic APPROVED/Organization activation branch.
  RAISE EXCEPTION 'APPLICATION_TRANSITION_DENIED' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
DROP TRIGGER guard_application_draft_identity ON merchant.application;
CREATE TRIGGER guard_application_workflow_v42 BEFORE INSERT OR UPDATE OR DELETE ON merchant.application FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_workflow_v42();
REVOKE ALL ON FUNCTION merchant.guard_application_workflow_v42() FROM PUBLIC;

CREATE FUNCTION merchant.guard_submitted_application_rows() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE a merchant.application; p merchant.application_policy; r merchant.application_revision; ev merchant.application_evidence;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'SUBMITTED_CONTENT_IMMUTABLE' USING ERRCODE='23514';END IF;
 SELECT * INTO a FROM merchant.application WHERE id=NEW.application_id FOR UPDATE;
 IF NOT FOUND OR a.state<>'SUBMITTED' THEN RAISE EXCEPTION 'SUBMISSION_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='application_revision' THEN
  SELECT * INTO p FROM merchant.application_policy WHERE id=NEW.policy_id;
  IF NEW.applicant_id<>a.applicant_account_id OR NEW.application_version<>a.version OR NEW.revision<>a.current_revision OR NEW.policy_id<>a.policy_id
   OR NEW.submitted_input<>a.draft OR NEW.contact_attestation_id IS DISTINCT FROM (NEW.submitted_input->>'businessContactAttestationId')::UUID
   OR p.state<>'PUBLISHED' OR p.version<>NEW.policy_version OR p.consent_version<>NEW.consent_version OR NEW.consent_version IS DISTINCT FROM NEW.submitted_input#>>'{declarations,consentVersion}'
   OR NOT(p.entity_types @> ARRAY[NEW.submitted_input->>'entityType']) OR NOT(p.jurisdiction_codes @> ARRAY[normalize(btrim(NEW.submitted_input->>'jurisdiction'),NFC)])
   OR NEW.submitted_at<p.valid_from OR (p.valid_to IS NOT NULL AND NEW.submitted_at>=p.valid_to)
   OR NEW.submitted_input#>>'{declarations,accuracy}' IS DISTINCT FROM 'true' OR NEW.submitted_input#>>'{declarations,representation}' IS DISTINCT FROM 'true'
   THEN RAISE EXCEPTION 'SUBMITTED_POLICY_BINDING_INVALID' USING ERRCODE='23514';END IF;
  IF NOT NEW.submitted_input ?& ARRAY['entityType','jurisdiction','registeredName','legalIdentifier','registeredAddress','operatingAddress','representativeName','representativeEmail','representativeRelationship','businessContactAttestationId','evidenceIds','declarations']
   OR EXISTS(SELECT 1 FROM unnest(ARRAY['entityType','jurisdiction','registeredName','legalIdentifier','representativeName','representativeEmail','representativeRelationship']) k WHERE jsonb_typeof(NEW.submitted_input->k) IS DISTINCT FROM 'string' OR length(btrim(NEW.submitted_input->>k))=0)
   OR jsonb_typeof(NEW.submitted_input->'registeredAddress') IS DISTINCT FROM 'object' OR jsonb_typeof(NEW.submitted_input->'operatingAddress') IS DISTINCT FROM 'object'
   OR jsonb_typeof(NEW.submitted_input->'evidenceIds') IS DISTINCT FROM 'array' OR jsonb_array_length(NEW.submitted_input->'evidenceIds') NOT BETWEEN 2 AND 5 THEN RAISE EXCEPTION 'SUBMISSION_INCOMPLETE' USING ERRCODE='23514';END IF;
 ELSE
  SELECT * INTO r FROM merchant.application_revision WHERE application_id=NEW.application_id AND revision=NEW.revision;
  SELECT * INTO ev FROM merchant.application_evidence WHERE id=NEW.evidence_id AND application_id=NEW.application_id;
  IF r.revision IS NULL OR ev.id IS NULL OR ev.state<>'CLEAN' OR ev.sha256<>NEW.sha256
   OR NOT(r.submitted_input->'evidenceIds' @> to_jsonb(NEW.evidence_id::TEXT)) THEN RAISE EXCEPTION 'SUBMITTED_EVIDENCE_BINDING_INVALID' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_application_revision BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_revision FOR EACH ROW EXECUTE FUNCTION merchant.guard_submitted_application_rows();
CREATE TRIGGER guard_application_revision_evidence BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_revision_evidence FOR EACH ROW EXECUTE FUNCTION merchant.guard_submitted_application_rows();
REVOKE ALL ON FUNCTION merchant.guard_submitted_application_rows() FROM PUBLIC;

-- Uses only the caller's already approved Merchant reads, never a privileged PII reader.
CREATE FUNCTION merchant.require_application_workflow_commit() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE e merchant.governance_event; p merchant.application_policy; r merchant.application_revision; old_state TEXT;
BEGIN
 IF NOT EXISTS(SELECT 1 FROM merchant.application_draft_version d WHERE d.application_id=NEW.id AND d.application_version=NEW.version
  AND d.applicant_account_id=NEW.applicant_account_id AND d.draft=NEW.draft AND d.created_at=NEW.created_at AND d.updated_at=NEW.updated_at)
  THEN RAISE EXCEPTION 'EXACT_APPLICATION_WORKING_HISTORY_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' OR (TG_OP='UPDATE' AND OLD.state=NEW.state AND NEW.state IN ('DRAFT','NEEDS_INFORMATION') AND NOT EXISTS(SELECT 1 FROM merchant.governance_event ev WHERE ev.application_id=NEW.id AND ev.new_version=NEW.version AND ev.operation='ASSIGN_REVIEWER')) THEN
  IF NOT EXISTS(SELECT 1 FROM merchant.command_receipt c JOIN merchant.business_audit b ON b.target_id=c.target_id AND b.new_version=c.result_version
    AND b.actor_account_id=c.subject_account_id AND b.operation=c.operation
    WHERE c.target_type='APPLICATION' AND c.target_id=NEW.id AND c.result_version=NEW.version AND c.subject_account_id=NEW.applicant_account_id
     AND c.operation=CASE WHEN TG_OP='INSERT' THEN 'createApplicationDraft' ELSE 'updateApplicationDraft' END)
   THEN RAISE EXCEPTION 'APPLICATION_DRAFT_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
  RETURN NULL;
 END IF;
 SELECT ev.* INTO e FROM merchant.governance_event ev JOIN merchant.governance_receipt c ON c.event_id=ev.id
  WHERE ev.target_type='APPLICATION' AND ev.application_id=NEW.id AND ev.new_version=NEW.version AND ev.prior_version=OLD.version
   AND c.target_id=NEW.id AND c.actor_kind=ev.actor_kind AND c.actor_binding=ev.actor_id::TEXT;
 IF NOT FOUND OR e.policy_id IS DISTINCT FROM NEW.policy_id THEN RAISE EXCEPTION 'APPLICATION_GOVERNANCE_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO p FROM merchant.application_policy WHERE id=NEW.policy_id;
 IF NOT(OLD.state='DRAFT' AND NEW.state='WITHDRAWN' AND OLD.policy_id IS NULL AND NEW.policy_id IS NULL AND e.actor_kind='CUSTOMER' AND e.operation='WITHDRAW')
  AND (p.id IS NULL OR p.state<>'PUBLISHED' OR clock_timestamp()<p.valid_from OR (p.valid_to IS NOT NULL AND clock_timestamp()>=p.valid_to))
   THEN RAISE EXCEPTION 'APPLICATION_CURRENT_POLICY_REQUIRED' USING ERRCODE='23514';END IF;
 IF e.actor_kind='CUSTOMER' THEN
  IF e.actor_id<>NEW.applicant_account_id OR e.realm_id IS NOT NULL OR e.operation NOT IN ('SUBMIT','RESUBMIT','WITHDRAW')
   OR (e.operation='WITHDRAW' AND (NEW.state<>'WITHDRAWN' OR OLD.state NOT IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION')))
   OR (e.operation='SUBMIT' AND (OLD.state<>'DRAFT' OR NEW.state<>'SUBMITTED'))
   OR (e.operation='RESUBMIT' AND (OLD.state<>'NEEDS_INFORMATION' OR NEW.state<>'SUBMITTED')) THEN RAISE EXCEPTION 'APPLICANT_TRANSITION_DENIED' USING ERRCODE='23514';END IF;
 ELSIF e.actor_kind='SYSTEM' THEN
  IF e.actor_id=NEW.applicant_account_id OR e.revision IS DISTINCT FROM NEW.current_revision
   OR NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.account_id=e.actor_id AND s.realm_id=e.realm_id AND s.scope_type='POLICY'
    AND s.policy_id=NEW.policy_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp()
    AND s.permissions @> ARRAY[CASE e.operation WHEN 'ASSIGN_REVIEWER' THEN 'application.assign' WHEN 'REJECT' THEN 'application.decide' ELSE 'application.review' END])
   OR EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.membership applicant ON applicant.organization_id=m.organization_id
    WHERE m.account_id=e.actor_id AND applicant.account_id=NEW.applicant_account_id AND m.state<>'REVOKED' AND applicant.state<>'REVOKED') THEN RAISE EXCEPTION 'CURRENT_REVIEWER_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
  IF (e.operation='ASSIGN_REVIEWER' AND (NEW.state<>OLD.state OR NEW.state NOT IN ('SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION')))
   OR (e.operation='BEGIN_REVIEW' AND (OLD.state NOT IN ('SUBMITTED','UNDER_REVIEW') OR NEW.state<>'UNDER_REVIEW'))
   OR (e.operation='READY_FOR_DECISION' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'UNDER_REVIEW'))
   OR (e.operation='REQUEST_INFORMATION' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'NEEDS_INFORMATION'))
   OR (e.operation='REJECT' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'REJECTED'))
   OR e.operation NOT IN ('ASSIGN_REVIEWER','BEGIN_REVIEW','READY_FOR_DECISION','REQUEST_INFORMATION','REJECT') THEN RAISE EXCEPTION 'REVIEW_TRANSITION_DENIED' USING ERRCODE='23514';END IF;
  IF e.operation<>'ASSIGN_REVIEWER' AND NOT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment a JOIN merchant.business_reviewer_scope s ON s.id=a.reviewer_scope_id
   WHERE a.application_id=NEW.id AND a.policy_id=NEW.policy_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['application.review']
    AND ((e.operation<>'REJECT' AND a.reviewer_account_id=e.actor_id) OR (e.operation='REJECT' AND a.reviewer_account_id<>e.actor_id)))
   THEN RAISE EXCEPTION 'INDEPENDENT_CURRENT_REVIEW_ASSIGNMENT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE RAISE EXCEPTION 'APPLICATION_OPERATOR_MUTATION_DENIED' USING ERRCODE='42501';END IF;
 IF e.operation='BEGIN_REVIEW' AND OLD.state='UNDER_REVIEW' AND EXISTS(
  SELECT 1 FROM merchant.governance_event prior JOIN merchant.application_reviewer_assignment a ON a.application_id=prior.application_id
  WHERE prior.application_id=NEW.id AND prior.revision=NEW.current_revision AND prior.operation='BEGIN_REVIEW'
   AND prior.actor_id=a.reviewer_account_id AND prior.occurred_at>=a.assigned_at AND prior.new_version<=e.prior_version)
  THEN RAISE EXCEPTION 'CURRENT_ASSIGNMENT_ALREADY_BEGAN_REVIEW' USING ERRCODE='23514';END IF;
 IF e.operation='REJECT' AND NOT EXISTS(
  SELECT 1 FROM merchant.governance_event ready JOIN merchant.application_reviewer_assignment a ON a.application_id=ready.application_id
  WHERE ready.application_id=NEW.id AND ready.revision=NEW.current_revision AND ready.operation='READY_FOR_DECISION'
   AND ready.actor_id=a.reviewer_account_id AND ready.actor_id<>e.actor_id AND ready.occurred_at>=a.assigned_at
   AND ready.new_version=e.prior_version AND ready.reason_code='CHECKLIST_COMPLETE')
  THEN RAISE EXCEPTION 'CURRENT_INDEPENDENT_COMPLETE_REVIEW_REQUIRED' USING ERRCODE='23514';END IF;
 IF e.operation IN ('SUBMIT','RESUBMIT') THEN
  SELECT * INTO r FROM merchant.application_revision WHERE application_id=NEW.id AND revision=NEW.current_revision;
  IF r.application_id IS NULL OR r.application_version<>NEW.version OR r.submitted_input<>NEW.draft OR r.policy_id<>NEW.policy_id
   OR e.revision IS DISTINCT FROM NEW.current_revision OR r.policy_version<>p.version THEN RAISE EXCEPTION 'EXACT_SUBMISSION_REVISION_REQUIRED' USING ERRCODE='23514';END IF;
  IF (SELECT count(*) FROM merchant.application_revision_evidence x WHERE x.application_id=NEW.id AND x.revision=NEW.current_revision)<>jsonb_array_length(r.submitted_input->'evidenceIds')
   OR NOT EXISTS(SELECT 1 FROM merchant.application_revision_evidence x JOIN merchant.application_evidence f ON f.id=x.evidence_id WHERE x.application_id=NEW.id AND x.revision=NEW.current_revision AND f.purpose='REGISTRATION' AND f.state='CLEAN' AND f.sha256=x.sha256)
   OR NOT EXISTS(SELECT 1 FROM merchant.application_revision_evidence x JOIN merchant.application_evidence f ON f.id=x.evidence_id WHERE x.application_id=NEW.id AND x.revision=NEW.current_revision AND f.purpose='REPRESENTATIVE_AUTHORIZATION' AND f.state='CLEAN' AND f.sha256=x.sha256)
   THEN RAISE EXCEPTION 'COMPLETE_SCREENED_REVISION_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER require_application_workflow_commit AFTER INSERT OR UPDATE ON merchant.application DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_application_workflow_commit();
REVOKE ALL ON FUNCTION merchant.require_application_workflow_commit() FROM PUBLIC;

CREATE FUNCTION merchant.guard_application_evidence_rows() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE a merchant.application; reservation merchant.application_evidence_reservation;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'EVIDENCE_RETENTION_REQUIRED' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM merchant.application WHERE id=NEW.application_id FOR UPDATE;
 IF a.id IS NULL OR a.state NOT IN ('DRAFT','NEEDS_INFORMATION') THEN RAISE EXCEPTION 'EDITABLE_APPLICATION_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='application_evidence_reservation' THEN
  IF NEW.applicant_id<>a.applicant_account_id THEN RAISE EXCEPTION 'EVIDENCE_OWNER_REQUIRED' USING ERRCODE='23514';END IF;
  IF TG_OP='INSERT' THEN
   IF NEW.state<>'RESERVED' OR (SELECT count(*) FROM merchant.application_evidence WHERE application_id=a.id)
     +(SELECT count(*) FROM merchant.application_evidence_reservation WHERE application_id=a.id AND state<>'FINALIZED')>=5
     THEN RAISE EXCEPTION 'EVIDENCE_QUOTA_OR_RESERVATION_INVALID' USING ERRCODE='23514';END IF;
  ELSIF to_jsonb(NEW)-ARRAY['state','finalized_at']<>to_jsonb(OLD)-ARRAY['state','finalized_at'] OR OLD.state<>'RESERVED' OR NEW.state NOT IN ('FINALIZED','FAILED','EXPIRED')
   OR (NEW.state='FINALIZED' AND clock_timestamp()>=OLD.expires_at) THEN RAISE EXCEPTION 'EVIDENCE_RESERVATION_IMMUTABLE' USING ERRCODE='23514';END IF;
 ELSE
  SELECT * INTO reservation FROM merchant.application_evidence_reservation WHERE id=NEW.reservation_id;
  IF reservation.id IS NULL OR reservation.application_id<>a.id OR reservation.purpose<>NEW.purpose OR reservation.applicant_id<>a.applicant_account_id
   OR (TG_OP='INSERT' AND (reservation.state<>'RESERVED' OR clock_timestamp()>=reservation.expires_at OR NEW.version<>1)) THEN RAISE EXCEPTION 'EVIDENCE_RESERVATION_REQUIRED' USING ERRCODE='23514';END IF;
  IF TG_OP='UPDATE' AND (NEW.state<>'ARCHIVED' OR OLD.state='ARCHIVED' OR NEW.version<>OLD.version+1 OR to_jsonb(NEW)-ARRAY['state','version','updated_at']<>to_jsonb(OLD)-ARRAY['state','version','updated_at']
    OR EXISTS(SELECT 1 FROM merchant.application_revision_evidence x WHERE x.evidence_id=OLD.id)) THEN RAISE EXCEPTION 'SUBMITTED_EVIDENCE_IMMUTABLE' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_application_evidence_reservation BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_evidence_reservation FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_evidence_rows();
CREATE TRIGGER guard_application_evidence BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_evidence FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_evidence_rows();
REVOKE ALL ON FUNCTION merchant.guard_application_evidence_rows() FROM PUBLIC;

CREATE FUNCTION merchant.guard_application_reviewer_assignment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE a merchant.application; s merchant.business_reviewer_scope; e merchant.governance_event;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'REVIEW_ASSIGNMENT_HISTORY_REQUIRED' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM merchant.application WHERE id=NEW.application_id FOR UPDATE;
 SELECT * INTO s FROM merchant.business_reviewer_scope WHERE id=NEW.reviewer_scope_id;
 SELECT * INTO e FROM merchant.governance_event WHERE id=NEW.event_id;
 IF a.id IS NULL OR s.id IS NULL OR e.id IS NULL OR a.state NOT IN ('SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION')
  OR NEW.policy_id IS DISTINCT FROM a.policy_id OR s.scope_type<>'POLICY' OR s.policy_id<>NEW.policy_id OR s.account_id<>NEW.reviewer_account_id
  OR s.state<>'ACTIVE' OR s.expires_at<=clock_timestamp() OR NOT(s.permissions @> ARRAY['application.review']) OR NEW.reviewer_account_id=a.applicant_account_id
  OR e.actor_kind<>'SYSTEM' OR e.operation<>'ASSIGN_REVIEWER' OR e.application_id<>a.id OR e.policy_id<>a.policy_id OR e.realm_id IS DISTINCT FROM s.realm_id
  OR e.prior_version<>a.version OR e.new_version<>a.version+1 OR e.revision<>a.current_revision OR NEW.assigned_at<>e.occurred_at
  THEN RAISE EXCEPTION 'CURRENT_REVISION_REVIEW_ASSIGNMENT_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' AND NEW.version<>1 OR TG_OP='UPDATE' AND (NEW.application_id<>OLD.application_id OR NEW.version<>OLD.version+1 OR NEW.assigned_at<OLD.assigned_at)
  THEN RAISE EXCEPTION 'REVIEW_ASSIGNMENT_VERSION_REQUIRED' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.membership applicant ON applicant.organization_id=m.organization_id
  WHERE m.account_id=NEW.reviewer_account_id AND applicant.account_id=a.applicant_account_id AND m.state<>'REVOKED' AND applicant.state<>'REVOKED')
  THEN RAISE EXCEPTION 'REVIEW_ASSIGNMENT_KNOWN_CONFLICT' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_application_reviewer_assignment BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_reviewer_assignment FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_reviewer_assignment();
REVOKE ALL ON FUNCTION merchant.guard_application_reviewer_assignment() FROM PUBLIC;

CREATE FUNCTION merchant.require_reviewer_assignment_commit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.governance_receipt r ON r.event_id=e.id
  JOIN merchant.application_draft_version d ON d.application_id=e.application_id AND d.application_version=e.new_version
  JOIN merchant.application a ON a.id=e.application_id
  WHERE e.id=NEW.event_id AND e.operation='ASSIGN_REVIEWER' AND e.application_id=NEW.application_id AND e.new_version<=a.version
   AND r.actor_kind='SYSTEM' AND r.actor_binding=e.actor_id::TEXT AND r.realm_id=e.realm_id)
  THEN RAISE EXCEPTION 'REVIEW_ASSIGNMENT_RECEIPT_AND_WORKING_HISTORY_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER require_reviewer_assignment_commit AFTER INSERT OR UPDATE ON merchant.application_reviewer_assignment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_reviewer_assignment_commit();
REVOKE ALL ON FUNCTION merchant.require_reviewer_assignment_commit() FROM PUBLIC;

CREATE FUNCTION merchant.guard_assignment_identity_eligibility() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE realm UUID;
BEGIN
 SELECT realm_id INTO realm FROM merchant.business_reviewer_scope WHERE id=NEW.reviewer_scope_id;
 IF realm IS NULL OR NOT identity.local_admin_eligible(NEW.reviewer_account_id,realm) THEN RAISE EXCEPTION 'CURRENT_ELIGIBLE_REVIEWER_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER assignment_identity_eligibility BEFORE INSERT OR UPDATE ON merchant.application_reviewer_assignment FOR EACH ROW EXECUTE FUNCTION merchant.guard_assignment_identity_eligibility();
REVOKE ALL ON FUNCTION merchant.guard_assignment_identity_eligibility() FROM PUBLIC,"${runtimeRole}";

-- Private structural guards; current_user is a genuine SECURITY DEFINER owner, not a caller GUC.
CREATE FUNCTION merchant.guard_governance_authority() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR current_user<>'merchant_governance_writer' THEN RAISE EXCEPTION 'GOVERNANCE_OPERATOR_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF TG_TABLE_NAME='application_policy' AND TG_OP='UPDATE' AND
  (OLD.state<>'PUBLISHED' OR NEW.state<>'RETIRED' OR NEW.version<>OLD.version
   OR to_jsonb(NEW)-ARRAY['state']<>to_jsonb(OLD)-ARRAY['state']) THEN RAISE EXCEPTION 'PUBLISHED_POLICY_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='business_reviewer_scope' AND TG_OP='UPDATE' AND
  (OLD.state<>'ACTIVE' OR NEW.state<>'REVOKED' OR NEW.version<>OLD.version+1
   OR to_jsonb(NEW)-ARRAY['state','version','operator_request_id','updated_at']<>to_jsonb(OLD)-ARRAY['state','version','operator_request_id','updated_at']) THEN RAISE EXCEPTION 'REVIEWER_SCOPE_IMMUTABLE' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_application_policy BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_policy FOR EACH ROW EXECUTE FUNCTION merchant.guard_governance_authority();
CREATE TRIGGER guard_business_reviewer_scope BEFORE INSERT OR UPDATE OR DELETE ON merchant.business_reviewer_scope FOR EACH ROW EXECUTE FUNCTION merchant.guard_governance_authority();
CREATE FUNCTION merchant.guard_governance_record() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE e merchant.governance_event;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'GOVERNANCE_RECORD_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF NEW.actor_kind='OPERATOR' AND current_user<>'merchant_governance_writer' THEN RAISE EXCEPTION 'GOVERNANCE_OPERATOR_EVENT_DENIED' USING ERRCODE='42501';END IF;
 IF TG_TABLE_NAME='governance_event' THEN
  IF NEW.actor_kind='OPERATOR' AND NEW.operator_principal<>session_user::TEXT THEN RAISE EXCEPTION 'GOVERNANCE_OPERATOR_EVENT_DENIED' USING ERRCODE='42501';END IF;
  IF NEW.actor_kind='CUSTOMER' AND (NEW.realm_id IS NOT NULL OR NEW.target_type<>'APPLICATION'
   OR NEW.operation NOT IN ('SUBMIT','RESUBMIT','WITHDRAW','RESERVE_EVIDENCE','FINALIZE_EVIDENCE','ARCHIVE_EVIDENCE')
   OR NOT EXISTS(SELECT 1 FROM merchant.application a WHERE a.id=NEW.application_id AND a.applicant_account_id=NEW.actor_id)) THEN RAISE EXCEPTION 'GOVERNANCE_APPLICANT_EVENT_DENIED' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_TABLE_NAME='governance_receipt' THEN
  SELECT * INTO e FROM merchant.governance_event WHERE id=NEW.event_id;
  IF NOT FOUND OR e.actor_kind<>NEW.actor_kind OR e.target_type<>NEW.target_type OR e.target_id<>NEW.target_id OR e.operation<>NEW.operation
   OR e.prior_version<>NEW.original_version OR e.new_version<>NEW.result_version OR e.realm_id IS DISTINCT FROM NEW.realm_id
   OR (NEW.actor_kind='OPERATOR' AND NEW.actor_binding<>e.operator_principal) OR (NEW.actor_kind<>'OPERATOR' AND NEW.actor_binding<>e.actor_id::TEXT)
   THEN RAISE EXCEPTION 'GOVERNANCE_RECEIPT_EVENT_BINDING' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_governance_event BEFORE INSERT OR UPDATE OR DELETE ON merchant.governance_event FOR EACH ROW EXECUTE FUNCTION merchant.guard_governance_record();
CREATE TRIGGER guard_governance_receipt BEFORE INSERT OR UPDATE OR DELETE ON merchant.governance_receipt FOR EACH ROW EXECUTE FUNCTION merchant.guard_governance_record();
-- Narrow trigger only: existing boolean Identity predicate, no new data privileges and no raw Identity SQL.
-- It supplies no write path or output to runtime. Operator guards remain SECURITY INVOKER above.
CREATE FUNCTION merchant.guard_governance_system_eligibility() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE binding JSONB;
BEGIN
 IF NEW.actor_kind='SYSTEM' AND (NEW.realm_id IS NULL OR NOT identity.local_admin_eligible(NEW.actor_id,NEW.realm_id))
  THEN RAISE EXCEPTION 'CURRENT_DEDICATED_ADMIN_REQUIRED' USING ERRCODE='42501';END IF;
 IF NEW.actor_kind='SYSTEM' AND NEW.operation='REJECT' THEN
  -- A caller-set descriptor is not authority: only the existing Identity-owned,
  -- consumed and request/action/target/version-bound canonical proof can satisfy it.
  -- No extra Identity SELECT, runtime EXECUTE, role membership or SQL signature.
  BEGIN
   binding:=nullif(current_setting('argopost.governance_stepup',true),'')::JSONB;
   IF binding IS NULL OR NOT coalesce(identity.admin_governance_command_bound((binding->>'proofId')::UUID,NEW.request_id,NEW.actor_id,binding->>'authorityVersion','REJECT',NEW.target_id,NEW.prior_version,decode(binding->>'inputDigest','hex')),false)
    THEN RAISE EXCEPTION 'CURRENT_BOUND_DECISION_PROOF_REQUIRED' USING ERRCODE='42501';END IF;
  EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'CURRENT_BOUND_DECISION_PROOF_REQUIRED' USING ERRCODE='42501';END;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER governance_system_eligibility BEFORE INSERT ON merchant.governance_event FOR EACH ROW EXECUTE FUNCTION merchant.guard_governance_system_eligibility();
REVOKE ALL ON FUNCTION merchant.guard_governance_system_eligibility() FROM PUBLIC,"${runtimeRole}";
CREATE FUNCTION merchant.require_governance_operator_receipt() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='application_policy' THEN
  IF NOT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.governance_receipt r ON r.event_id=e.id
   WHERE e.actor_kind='OPERATOR' AND e.target_type='POLICY' AND e.target_id=NEW.id AND ((NEW.state='PUBLISHED' AND e.new_version=NEW.version) OR (NEW.state='RETIRED' AND e.prior_version=NEW.version AND e.new_version=NEW.version+1))
    AND e.operation=CASE NEW.state WHEN 'PUBLISHED' THEN 'PUBLISH_POLICY' ELSE 'RETIRE_POLICY' END) THEN RAISE EXCEPTION 'POLICY_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.governance_receipt r ON r.event_id=e.id
   WHERE e.actor_kind='OPERATOR' AND e.target_type='SCOPE' AND e.target_id=NEW.id AND e.new_version=NEW.version
    AND e.request_id=NEW.operator_request_id AND e.operation=CASE NEW.state WHEN 'ACTIVE' THEN 'ISSUE_SCOPE' ELSE 'REVOKE_SCOPE' END) THEN RAISE EXCEPTION 'SCOPE_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER require_application_policy_receipt AFTER INSERT OR UPDATE ON merchant.application_policy DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_governance_operator_receipt();
CREATE CONSTRAINT TRIGGER require_business_scope_receipt AFTER INSERT OR UPDATE ON merchant.business_reviewer_scope DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_governance_operator_receipt();

CREATE FUNCTION merchant.publish_local_application_policy(p_request UUID,p_policy UUID,p_version BIGINT,p_body JSONB,p_reason TEXT) RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE stamp TIMESTAMPTZ:=clock_timestamp(); v_environment TEXT; v_state TEXT; v_from TIMESTAMPTZ; v_until TIMESTAMPTZ; v_entities TEXT[]; v_jurisdictions TEXT[]; v_evidence TEXT[]; digest BYTEA; prior merchant.governance_receipt; existing merchant.application_policy; event UUID:=gen_random_uuid(); op TEXT; base BIGINT; canonical JSONB;
BEGIN
 IF NOT pg_has_role(session_user,'merchant_governance_operator','MEMBER') OR session_user IN ('merchant_governance_operator','merchant_governance_writer')
  OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_policy IS NULL OR p_version<1 OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 THEN RAISE EXCEPTION 'GOVERNANCE_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(('x4d45524348414e54'::BIT(64))::BIGINT);
 IF jsonb_typeof(p_body)<>'object' OR NOT p_body ?& ARRAY['code','environment','entityTypes','jurisdictionCodes','consentVersion','consentText','consentStatements','requiredEvidence','reviewMode','state','validFrom','validTo']
  OR p_body-ARRAY['code','environment','entityTypes','jurisdictionCodes','consentVersion','consentText','consentStatements','requiredEvidence','reviewMode','state','validFrom','validTo']<>'{}'::JSONB THEN RAISE EXCEPTION 'POLICY_INPUT_INVALID' USING ERRCODE='22023';END IF;
 v_environment:=p_body->>'environment';v_state:=p_body->>'state';
 IF v_environment NOT IN ('LOCAL','TEST') OR NOT identity.local_admin_environment_matches(v_environment) OR v_state NOT IN ('PUBLISHED','RETIRED') THEN RAISE EXCEPTION 'GOVERNANCE_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 IF jsonb_typeof(p_body->'entityTypes')<>'array' OR jsonb_typeof(p_body->'jurisdictionCodes')<>'array' OR jsonb_typeof(p_body->'requiredEvidence')<>'array'
  OR EXISTS(SELECT 1 FROM jsonb_array_elements((p_body->'entityTypes')||(p_body->'jurisdictionCodes')||(p_body->'requiredEvidence')) x WHERE jsonb_typeof(x)<>'string')
  OR p_body->>'reviewMode'<>'TWO_PERSON' OR length(btrim(p_body->>'consentText')) NOT BETWEEN 1 AND 8192
  OR length(btrim(p_body->>'consentVersion')) NOT BETWEEN 1 AND 64 THEN RAISE EXCEPTION 'POLICY_INPUT_INVALID' USING ERRCODE='22023';END IF;
 SELECT array_agg(v ORDER BY v COLLATE "C") INTO v_entities FROM jsonb_array_elements_text(p_body->'entityTypes') v;
 SELECT array_agg(v ORDER BY v COLLATE "C") INTO v_jurisdictions FROM jsonb_array_elements_text(p_body->'jurisdictionCodes') v;
 SELECT array_agg(v ORDER BY v COLLATE "C") INTO v_evidence FROM jsonb_array_elements_text(p_body->'requiredEvidence') v;
 IF v_entities IS NULL OR v_jurisdictions IS NULL OR v_evidence IS NULL OR cardinality(v_entities)<>(SELECT count(DISTINCT v) FROM unnest(v_entities) v)
  OR cardinality(v_jurisdictions)<>(SELECT count(DISTINCT v) FROM unnest(v_jurisdictions) v) OR cardinality(v_evidence)<>(SELECT count(DISTINCT v) FROM unnest(v_evidence) v)
  OR EXISTS(SELECT 1 FROM unnest(v_jurisdictions) v WHERE v='' OR length(v)>64 OR v<>normalize(btrim(v),NFC) OR v ~ '[[:cntrl:]]') THEN RAISE EXCEPTION 'POLICY_INPUT_INVALID' USING ERRCODE='22023';END IF;
 v_from:=(p_body->>'validFrom')::TIMESTAMPTZ;v_until:=(p_body->>'validTo')::TIMESTAMPTZ;
 IF v_from IS NULL OR (v_until IS NOT NULL AND v_until<=v_from) THEN RAISE EXCEPTION 'POLICY_WINDOW_INVALID' USING ERRCODE='22023';END IF;
 op:=CASE v_state WHEN 'PUBLISHED' THEN 'PUBLISH_POLICY' ELSE 'RETIRE_POLICY' END;base:=CASE v_state WHEN 'PUBLISHED' THEN p_version-1 ELSE p_version END;
 canonical:=jsonb_build_object('format','ARGO_OPERATOR_RECEIPT_1','operator',session_user,'request',p_request,'operation',op,'policy',p_policy,'version',p_version,'reason',p_reason,
  'body',p_body||jsonb_build_object('entityTypes',v_entities,'jurisdictionCodes',v_jurisdictions,'requiredEvidence',v_evidence,'validFrom',extract(epoch FROM v_from)*1000000,'validTo',extract(epoch FROM v_until)*1000000));
 digest:=sha256(convert_to(canonical::TEXT,'UTF8'));
 SELECT * INTO prior FROM merchant.governance_receipt WHERE actor_kind='OPERATOR' AND actor_binding=session_user::TEXT AND operation=op AND operation_key=p_request;
 IF FOUND THEN IF prior.input_digest<>digest OR prior.target_id<>p_policy THEN RAISE EXCEPTION 'GOVERNANCE_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;RETURN prior.target_id;END IF;
 IF v_state='PUBLISHED' THEN
  IF EXISTS(SELECT 1 FROM merchant.application_policy WHERE environment=v_environment AND state='PUBLISHED' AND entity_types && v_entities AND jurisdiction_codes && v_jurisdictions
    AND tstzrange(valid_from,valid_to,'[)') && tstzrange(v_from,v_until,'[)')) THEN RAISE EXCEPTION 'POLICY_WINDOW_OVERLAP' USING ERRCODE='23514';END IF;
  INSERT INTO merchant.application_policy VALUES(p_policy,p_body->>'code',p_version,v_environment,v_entities,v_jurisdictions,p_body->>'consentVersion',p_body->'consentStatements',v_evidence,'TWO_PERSON','PUBLISHED',v_from,v_until,sha256(convert_to((canonical->'body')::TEXT,'UTF8')),stamp);
 ELSE
  SELECT * INTO existing FROM merchant.application_policy WHERE id=p_policy FOR UPDATE;
  IF NOT FOUND OR existing.version<>p_version OR existing.state<>'PUBLISHED' THEN RAISE EXCEPTION 'POLICY_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
  IF existing.code<>p_body->>'code' OR existing.environment<>v_environment OR existing.entity_types<>v_entities OR existing.jurisdiction_codes<>v_jurisdictions OR existing.consent_version<>p_body->>'consentVersion' OR existing.valid_from<>v_from OR existing.valid_to IS DISTINCT FROM v_until THEN RAISE EXCEPTION 'POLICY_CONTENT_IMMUTABLE' USING ERRCODE='23514';END IF;
  IF existing.published_digest<>sha256(convert_to(((canonical->'body')||jsonb_build_object('state','PUBLISHED'))::TEXT,'UTF8')) THEN RAISE EXCEPTION 'POLICY_CONTENT_IMMUTABLE' USING ERRCODE='23514';END IF;
  UPDATE merchant.application_policy SET state='RETIRED' WHERE id=p_policy;
 END IF;
 -- Policy text is public synthetic policy content, not applicant PII or an operator reason.
 -- Keep the published legal/consent artifact immutable in its original event; review comments use protected application payloads.
 INSERT INTO merchant.governance_event VALUES(event,'OPERATOR',NULL,session_user::TEXT,NULL,'POLICY',p_policy,NULL,NULL,p_policy,op,NULL,base,CASE v_state WHEN 'PUBLISHED' THEN p_version ELSE p_version+1 END,'OPERATOR_POLICY',convert_to((canonical->'body')::TEXT,'UTF8'),p_request,stamp);
 INSERT INTO merchant.governance_receipt VALUES('OPERATOR',session_user::TEXT,NULL,op,p_request,'POLICY',p_policy,base,CASE v_state WHEN 'PUBLISHED' THEN p_version ELSE p_version+1 END,'SQL_SHA256_1',digest,event,'COMMITTED',stamp,stamp+INTERVAL '7 days');
 RETURN p_policy;
END $$;

CREATE FUNCTION merchant.configure_local_reviewer_scope(p_request UUID,p_actor UUID,p_realm UUID,p_type TEXT,p_scope UUID,p_permissions TEXT[],p_action TEXT,p_expected BIGINT,p_expires TIMESTAMPTZ,p_reason TEXT) RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE stamp TIMESTAMPTZ:=clock_timestamp(); perms TEXT[]; digest BYTEA; prior merchant.governance_receipt; existing merchant.business_reviewer_scope; result UUID; event UUID:=gen_random_uuid(); v_policy UUID; v_org UUID; op TEXT; target_environment TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'merchant_governance_operator','MEMBER') OR session_user IN ('merchant_governance_operator','merchant_governance_writer')
  OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_actor IS NULL OR p_realm IS NULL OR p_scope IS NULL OR p_expected IS NULL OR p_expected<0
  OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_action NOT IN ('ISSUE','REVOKE') OR p_type NOT IN ('POLICY','ORGANIZATION') THEN RAISE EXCEPTION 'GOVERNANCE_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(('x4d45524348414e54'::BIT(64))::BIGINT);
 IF p_type='POLICY' THEN v_policy:=p_scope;SELECT environment INTO target_environment FROM merchant.application_policy WHERE id=p_scope;
 ELSE v_org:=p_scope;SELECT environment INTO target_environment FROM merchant.organization WHERE id=p_scope;END IF;
 IF target_environment IS NULL OR NOT identity.local_admin_environment_matches(target_environment) THEN RAISE EXCEPTION 'GOVERNANCE_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 IF p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 10 OR array_position(p_permissions,NULL) IS NOT NULL
  OR cardinality(p_permissions)<>(SELECT count(DISTINCT v) FROM unnest(p_permissions) v)
  OR NOT p_permissions <@ ARRAY['application.read','application.evidence.read','application.assign','application.review','application.decide','org.read','org.suspend','org.reinstate','audit.read','task.read']::TEXT[]
  OR (p_type='ORGANIZATION' AND NOT p_permissions <@ ARRAY['org.read','org.suspend','org.reinstate','audit.read']::TEXT[]) OR p_expires IS NULL THEN RAISE EXCEPTION 'REVIEWER_PERMISSIONS_INVALID' USING ERRCODE='22023';END IF;
 SELECT array_agg(v ORDER BY v COLLATE "C") INTO perms FROM unnest(p_permissions) v;
 op:=CASE p_action WHEN 'ISSUE' THEN 'ISSUE_SCOPE' ELSE 'REVOKE_SCOPE' END;
 digest:=sha256(convert_to(jsonb_build_object('format','ARGO_OPERATOR_RECEIPT_1','operator',session_user,'request',p_request,'operation',op,'actor',p_actor,'realm',p_realm,'type',p_type,'scope',p_scope,'permissions',perms,'version',p_expected,'expiresAt',extract(epoch FROM p_expires)*1000000,'reason',p_reason)::TEXT,'UTF8'));
 SELECT * INTO prior FROM merchant.governance_receipt WHERE actor_kind='OPERATOR' AND actor_binding=session_user::TEXT AND operation=op AND operation_key=p_request;
 IF FOUND THEN IF prior.input_digest<>digest THEN RAISE EXCEPTION 'GOVERNANCE_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;RETURN prior.target_id;END IF;
 SELECT * INTO existing FROM merchant.business_reviewer_scope WHERE account_id=p_actor AND realm_id=p_realm AND scope_type=p_type AND coalesce(policy_id,organization_id)=p_scope AND state='ACTIVE' FOR UPDATE;
 IF p_action='ISSUE' THEN
  IF FOUND OR p_expected<>0 OR p_expires<=stamp OR NOT identity.local_admin_eligible(p_actor,p_realm)
   OR (v_policy IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.application_policy WHERE id=v_policy AND state='PUBLISHED' AND valid_from<=stamp AND (valid_to IS NULL OR valid_to>stamp))) THEN RAISE EXCEPTION 'REVIEWER_SCOPE_ADMISSION_DENIED' USING ERRCODE='42501';END IF;
  IF v_policy IS NOT NULL AND (SELECT count(*) FROM merchant.business_reviewer_scope WHERE policy_id=v_policy AND state='ACTIVE' AND expires_at>stamp)>=100 THEN RAISE EXCEPTION 'REVIEWER_SCOPE_CAPACITY' USING ERRCODE='23514';END IF;
  result:=gen_random_uuid();INSERT INTO merchant.business_reviewer_scope VALUES(result,p_actor,p_realm,p_type,v_policy,v_org,perms,'ACTIVE',1,p_expires,p_request,stamp);
 ELSE
  IF NOT FOUND OR existing.version<>p_expected OR existing.permissions<>perms OR existing.expires_at<>p_expires THEN RAISE EXCEPTION 'REVIEWER_SCOPE_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
  result:=existing.id;UPDATE merchant.business_reviewer_scope SET state='REVOKED',version=version+1,operator_request_id=p_request,updated_at=stamp WHERE id=result;
 END IF;
 INSERT INTO merchant.governance_event VALUES(event,'OPERATOR',NULL,session_user::TEXT,p_realm,'SCOPE',result,NULL,v_org,v_policy,op,NULL,p_expected,p_expected+1,'OPERATOR_SCOPE',NULL,p_request,stamp);
 INSERT INTO merchant.governance_receipt VALUES('OPERATOR',session_user::TEXT,p_realm,op,p_request,'SCOPE',result,p_expected,p_expected+1,'SQL_SHA256_1',digest,event,'COMMITTED',stamp,stamp+INTERVAL '7 days');
 RETURN result;
END $$;

REVOKE ALL ON merchant.application_policy,merchant.business_reviewer_scope,merchant.governance_event,merchant.governance_receipt,merchant.application_revision,merchant.application_evidence_reservation,merchant.application_evidence,merchant.application_revision_evidence,merchant.application_reviewer_assignment FROM PUBLIC;
GRANT SELECT ON merchant.application_policy,merchant.business_reviewer_scope,merchant.governance_event,merchant.governance_receipt,merchant.application_revision,merchant.application_evidence_reservation,merchant.application_evidence,merchant.application_revision_evidence,merchant.application_reviewer_assignment TO "${runtimeRole}";
-- Explicit V42 workflow writes only. Policy/scope remain offline-only; audit/history remain append-only.
GRANT INSERT ON merchant.governance_event,merchant.governance_receipt,merchant.application_revision,merchant.application_revision_evidence,merchant.application_evidence_reservation,merchant.application_evidence TO "${runtimeRole}";
GRANT UPDATE(state,finalized_at) ON merchant.application_evidence_reservation TO "${runtimeRole}";
GRANT UPDATE(state,version,updated_at) ON merchant.application_evidence TO "${runtimeRole}";
GRANT INSERT ON merchant.application_reviewer_assignment TO "${runtimeRole}";
GRANT UPDATE(reviewer_account_id,reviewer_scope_id,policy_id,version,event_id,assigned_at) ON merchant.application_reviewer_assignment TO "${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON merchant.application_policy,merchant.business_reviewer_scope TO merchant_governance_writer;
GRANT SELECT,INSERT ON merchant.governance_event,merchant.governance_receipt TO merchant_governance_writer;
GRANT SELECT ON merchant.organization TO merchant_governance_writer;
GRANT USAGE ON SCHEMA merchant,identity TO merchant_governance_writer;
GRANT USAGE ON SCHEMA merchant TO merchant_governance_operator;
DO $$ DECLARE migration_owner NAME:=current_user;BEGIN
 EXECUTE 'SET LOCAL ROLE identity_admin_writer';
 EXECUTE 'GRANT EXECUTE ON FUNCTION identity.local_admin_environment_matches(TEXT),identity.local_admin_eligible(UUID,UUID) TO merchant_governance_writer';
 EXECUTE format('SET LOCAL ROLE %I',migration_owner);
END $$;
REVOKE ALL ON FUNCTION merchant.guard_governance_authority(),merchant.guard_governance_record(),merchant.require_governance_operator_receipt(),merchant.publish_local_application_policy(UUID,UUID,BIGINT,JSONB,TEXT),merchant.configure_local_reviewer_scope(UUID,UUID,UUID,TEXT,UUID,TEXT[],TEXT,BIGINT,TIMESTAMPTZ,TEXT) FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION merchant.publish_local_application_policy(UUID,UUID,BIGINT,JSONB,TEXT),merchant.configure_local_reviewer_scope(UUID,UUID,UUID,TEXT,UUID,TEXT[],TEXT,BIGINT,TIMESTAMPTZ,TEXT) TO merchant_governance_operator;
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
ALTER FUNCTION merchant.publish_local_application_policy(UUID,UUID,BIGINT,JSONB,TEXT) OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.configure_local_reviewer_scope(UUID,UUID,UUID,TEXT,UUID,TEXT[],TEXT,BIGINT,TIMESTAMPTZ,TEXT) OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.guard_governance_system_eligibility() OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.guard_assignment_identity_eligibility() OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;
