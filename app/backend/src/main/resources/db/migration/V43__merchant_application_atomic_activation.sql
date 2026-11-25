-- Approved P0-MA / P0-NOTICE-BIND-01. Isolated source/test; no policy, principal or seed data.
CREATE TABLE merchant.business_key_registry (
 jurisdiction_key TEXT NOT NULL CHECK(length(jurisdiction_key) BETWEEN 1 AND 64 AND jurisdiction_key=normalize(btrim(jurisdiction_key),NFC)),
 entity_type VARCHAR(32) NOT NULL CHECK(entity_type IN ('COMPANY','HOUSEHOLD_BUSINESS')),
 legal_key TEXT NOT NULL CHECK(length(legal_key) BETWEEN 1 AND 80 AND legal_key=normalize(regexp_replace(legal_key,'[- ./_]','','g'),NFC)),
 organization_id UUID NOT NULL UNIQUE, application_id UUID NOT NULL UNIQUE, revision BIGINT NOT NULL CHECK(revision>0), decision_event_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(jurisdiction_key,entity_type,legal_key),
 FOREIGN KEY(organization_id) REFERENCES merchant.organization(id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(application_id,revision) REFERENCES merchant.application_revision(application_id,revision),
 FOREIGN KEY(decision_event_id) REFERENCES merchant.governance_event(id)
);
CREATE TABLE merchant.application_activation (
 application_id UUID PRIMARY KEY, revision BIGINT NOT NULL CHECK(revision>0), decision_event_id UUID NOT NULL UNIQUE,
 organization_id UUID NOT NULL UNIQUE, owner_membership_id UUID NOT NULL UNIQUE, applicant_id UUID NOT NULL,
 policy_id UUID NOT NULL REFERENCES merchant.application_policy(id), request_id UUID NOT NULL UNIQUE, activated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(application_id,revision) REFERENCES merchant.application_revision(application_id,revision),
 FOREIGN KEY(owner_membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(organization_id) REFERENCES merchant.organization(id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(decision_event_id) REFERENCES merchant.governance_event(id)
);
CREATE TABLE notification.merchant_application_notice (
 id UUID PRIMARY KEY, event_id UUID NOT NULL, application_id UUID NOT NULL, applicant_id UUID NOT NULL,
 protected_recipient BYTEA NOT NULL CHECK(octet_length(protected_recipient) BETWEEN 32 AND 2048),
 recipient_key_id VARCHAR(64) NOT NULL CHECK(recipient_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 template VARCHAR(64) NOT NULL CHECK(template IN ('REQUEST_INFORMATION','REJECTED','APPROVED','WITHDRAWN')),
 state VARCHAR(24) NOT NULL CHECK(state IN ('QUEUED','SENDING','RETRY_WAIT','ACCEPTED_BY_PROVIDER','FAILED','BLOCKED')),
 attempts INTEGER NOT NULL CHECK(attempts>=0), next_attempt_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL, accepted_at TIMESTAMPTZ,
 UNIQUE(event_id,template), CHECK((state='ACCEPTED_BY_PROVIDER')=(accepted_at IS NOT NULL)),
 CHECK(next_attempt_at IS NULL OR next_attempt_at>=created_at), CHECK(accepted_at IS NULL OR accepted_at>=created_at)
);

CREATE FUNCTION merchant.guard_application_activation_rows() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE a merchant.application; e merchant.governance_event; binding JSONB;
BEGIN
 IF TG_OP<>'INSERT' OR current_user<>'merchant_governance_writer' THEN RAISE EXCEPTION 'IMMUTABLE_CANONICAL_ACTIVATION_REQUIRED' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM merchant.application WHERE id=NEW.application_id FOR UPDATE;
 SELECT * INTO e FROM merchant.governance_event WHERE id=NEW.decision_event_id;
 IF a.id IS NULL OR e.id IS NULL OR e.operation<>'APPROVE' OR e.actor_kind<>'SYSTEM' OR e.target_type<>'APPLICATION' OR e.application_id<>a.id OR e.target_id<>a.id
  OR e.policy_id<>a.policy_id OR e.revision<>NEW.revision OR e.revision<>a.current_revision OR e.actor_id=a.applicant_account_id OR NOT EXISTS(SELECT 1 FROM merchant.governance_event ev WHERE ev.id=e.id AND ev.xmin::text=pg_current_xact_id()::text)
  OR NOT EXISTS(SELECT 1 FROM merchant.governance_receipt r WHERE r.event_id=e.id AND r.operation='APPROVE' AND r.actor_kind='SYSTEM' AND r.actor_binding=e.actor_id::text
   AND r.realm_id=e.realm_id AND r.target_id=a.id AND r.original_version=e.prior_version AND r.result_version=e.new_version AND r.xmin::text=pg_current_xact_id()::text)
 THEN RAISE EXCEPTION 'BOUND_APPROVAL_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
 BEGIN
  binding:=nullif(current_setting('argopost.governance_stepup',true),'')::JSONB;
  IF binding IS NULL OR NOT coalesce(identity.admin_governance_command_bound((binding->>'proofId')::UUID,e.request_id,e.actor_id,binding->>'authorityVersion','APPROVE',a.id,e.prior_version,decode(binding->>'inputDigest','hex')),false)
   THEN RAISE EXCEPTION 'BOUND_APPROVAL_PROOF_REQUIRED' USING ERRCODE='42501';END IF;
 EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'BOUND_APPROVAL_PROOF_REQUIRED' USING ERRCODE='42501';END;
 IF TG_TABLE_NAME='application_activation' THEN
  IF NEW.applicant_id<>a.applicant_account_id OR NEW.policy_id<>a.policy_id OR NEW.request_id<>e.request_id OR NEW.activated_at<>e.occurred_at THEN RAISE EXCEPTION 'ACTIVATION_IDENTITY_BINDING_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NEW.jurisdiction_key<>normalize(btrim(a.draft->>'jurisdiction'),NFC) OR NEW.entity_type<>a.draft->>'entityType'
   OR NEW.legal_key<>normalize(regexp_replace(a.draft->>'legalIdentifier','[- ./_]','','g'),NFC) OR NEW.created_at<>e.occurred_at
   OR NOT EXISTS(SELECT 1 FROM merchant.application_activation x WHERE x.application_id=a.id AND x.revision=NEW.revision AND x.organization_id=NEW.organization_id AND x.decision_event_id=e.id AND x.xmin::text=pg_current_xact_id()::text)
   THEN RAISE EXCEPTION 'ACTIVATED_BUSINESS_KEY_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER guard_application_activation BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_activation FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_activation_rows();
CREATE TRIGGER guard_business_key_registry BEFORE INSERT OR UPDATE OR DELETE ON merchant.business_key_registry FOR EACH ROW EXECUTE FUNCTION merchant.guard_application_activation_rows();

CREATE FUNCTION merchant.application_origin_admitted(p_organization UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT coalesce(EXISTS(SELECT 1 FROM merchant.organization o WHERE o.id=p_organization AND o.environment IN ('LOCAL','TEST') AND
  (o.admission_origin='LOCAL_OPERATOR_PROVISIONED' OR (o.admission_origin='APPLICATION_APPROVED' AND EXISTS(
   SELECT 1 FROM merchant.application_activation x JOIN merchant.business_key_registry k ON k.application_id=x.application_id AND k.organization_id=x.organization_id AND k.decision_event_id=x.decision_event_id
    JOIN merchant.application a ON a.id=x.application_id AND a.state='APPROVED' AND a.applicant_account_id=x.applicant_id
    JOIN merchant.membership m ON m.id=x.owner_membership_id AND m.organization_id=x.organization_id AND m.account_id=x.applicant_id AND m.role='OWNER'
    JOIN merchant.governance_event e ON e.id=x.decision_event_id AND e.operation='APPROVE' AND e.application_id=x.application_id AND e.revision=x.revision
   WHERE x.organization_id=o.id AND x.owner_membership_id=o.initial_owner_membership_id)))),false)
$$;

-- P0-NOTICE-BIND-01: only a boolean across the owner boundary, no recipient/PII disclosure.
CREATE FUNCTION merchant.application_notice_bound(p_event UUID,p_application UUID,p_applicant UUID,p_template TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT coalesce(EXISTS(SELECT 1 FROM merchant.governance_event e
  JOIN merchant.application a ON a.id=e.application_id AND a.applicant_account_id=p_applicant
  JOIN merchant.governance_receipt r ON r.event_id=e.id AND r.actor_kind=e.actor_kind AND r.actor_binding=e.actor_id::text AND r.realm_id IS NOT DISTINCT FROM e.realm_id
   AND r.operation=e.operation AND r.target_type='APPLICATION' AND r.target_id=a.id AND r.original_version=e.prior_version AND r.result_version=e.new_version AND r.outcome='COMMITTED' AND r.input_key_id<>'SQL_SHA256_1'
  WHERE e.id=p_event AND e.application_id=p_application AND e.target_id=p_application AND e.target_type='APPLICATION' AND e.new_version=a.version
   AND e.xmin::text=pg_current_xact_id()::text AND r.xmin::text=pg_current_xact_id()::text
   AND ((p_template='REQUEST_INFORMATION' AND e.operation='REQUEST_INFORMATION' AND a.state='NEEDS_INFORMATION')
    OR (p_template='REJECTED' AND e.operation='REJECT' AND a.state='REJECTED') OR (p_template='WITHDRAWN' AND e.operation='WITHDRAW' AND a.state='WITHDRAWN')
    OR (p_template='APPROVED' AND e.operation='APPROVE' AND a.state='APPROVED' AND EXISTS(SELECT 1 FROM merchant.application_activation x WHERE x.application_id=a.id AND x.decision_event_id=e.id AND x.applicant_id=p_applicant AND x.xmin::text=pg_current_xact_id()::text)))) ,false)
$$;
CREATE FUNCTION notification.guard_merchant_application_notice() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'APPLICATION_NOTICE_IMMUTABLE' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'QUEUED' OR NEW.attempts<>0 OR NEW.accepted_at IS NOT NULL OR NOT merchant.application_notice_bound(NEW.event_id,NEW.application_id,NEW.applicant_id,NEW.template)
   THEN RAISE EXCEPTION 'AUTHORITATIVE_APPLICATION_NOTICE_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF to_jsonb(NEW)-ARRAY['state','attempts','next_attempt_at','accepted_at']<>to_jsonb(OLD)-ARRAY['state','attempts','next_attempt_at','accepted_at']
   OR NOT ((OLD.state IN ('QUEUED','RETRY_WAIT') AND NEW.state='SENDING' AND NEW.attempts=OLD.attempts+1)
    OR (OLD.state='SENDING' AND NEW.state IN ('RETRY_WAIT','ACCEPTED_BY_PROVIDER','FAILED','BLOCKED') AND NEW.attempts=OLD.attempts))
   THEN RAISE EXCEPTION 'APPLICATION_NOTICE_TRANSITION_DENIED' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER guard_merchant_application_notice BEFORE INSERT OR UPDATE OR DELETE ON notification.merchant_application_notice FOR EACH ROW EXECUTE FUNCTION notification.guard_merchant_application_notice();

REVOKE ALL ON merchant.business_key_registry,merchant.application_activation,notification.merchant_application_notice FROM PUBLIC,"${runtimeRole}";
GRANT SELECT ON merchant.business_key_registry,merchant.application_activation,notification.merchant_application_notice TO "${runtimeRole}";
GRANT INSERT ON notification.merchant_application_notice TO "${runtimeRole}";
GRANT UPDATE(state,attempts,next_attempt_at,accepted_at) ON notification.merchant_application_notice TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.business_key_registry,merchant.application_activation TO merchant_governance_writer;
GRANT SELECT ON merchant.application,merchant.application_revision,merchant.membership,merchant.member_grant,merchant.application_reviewer_assignment TO merchant_governance_writer;
GRANT USAGE ON SCHEMA notification TO merchant_governance_writer;
REVOKE ALL ON FUNCTION merchant.guard_application_activation_rows(),merchant.application_origin_admitted(UUID),merchant.application_notice_bound(UUID,UUID,UUID,TEXT),notification.guard_merchant_application_notice() FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION merchant.application_origin_admitted(UUID) TO "${runtimeRole}";
-- Privileges are fixed before ownership transfer. No PUBLIC/runtime predicate execution.
GRANT CREATE ON SCHEMA merchant,notification TO merchant_governance_writer;
ALTER FUNCTION merchant.application_origin_admitted(UUID) OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.application_notice_bound(UUID,UUID,UUID,TEXT) OWNER TO merchant_governance_writer;
ALTER FUNCTION notification.guard_merchant_application_notice() OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant,notification FROM merchant_governance_writer;

-- Preserve all previous workflow branches; only add the guarded terminal approval.
CREATE OR REPLACE FUNCTION merchant.guard_application_workflow_v42() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
 ELSIF (OLD.state='SUBMITTED' AND NEW.state IN ('SUBMITTED','UNDER_REVIEW')) OR (OLD.state='UNDER_REVIEW' AND NEW.state IN ('UNDER_REVIEW','NEEDS_INFORMATION','REJECTED','APPROVED'))
   OR (OLD.state IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION') AND NEW.state='WITHDRAWN') THEN
  IF NEW.draft<>OLD.draft OR NEW.current_revision<>OLD.current_revision OR NEW.submitted_revision IS DISTINCT FROM OLD.submitted_revision OR NEW.policy_id IS DISTINCT FROM OLD.policy_id THEN RAISE EXCEPTION 'APPLICATION_REVIEW_CANNOT_EDIT_SNAPSHOT' USING ERRCODE='23514';END IF;
 ELSE
  -- V43 alone introduces the receipt-backed atomic APPROVED/Organization activation branch.
  RAISE EXCEPTION 'APPLICATION_TRANSITION_DENIED' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION merchant.require_application_workflow_commit() RETURNS TRIGGER
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
    AND s.permissions @> ARRAY[CASE e.operation WHEN 'ASSIGN_REVIEWER' THEN 'application.assign' WHEN 'REJECT' THEN 'application.decide' WHEN 'APPROVE' THEN 'application.decide' ELSE 'application.review' END])
   OR EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.membership applicant ON applicant.organization_id=m.organization_id
    WHERE m.account_id=e.actor_id AND applicant.account_id=NEW.applicant_account_id AND m.state<>'REVOKED' AND applicant.state<>'REVOKED') THEN RAISE EXCEPTION 'CURRENT_REVIEWER_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
  IF (e.operation='ASSIGN_REVIEWER' AND (NEW.state<>OLD.state OR NEW.state NOT IN ('SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION')))
   OR (e.operation='BEGIN_REVIEW' AND (OLD.state NOT IN ('SUBMITTED','UNDER_REVIEW') OR NEW.state<>'UNDER_REVIEW'))
   OR (e.operation='READY_FOR_DECISION' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'UNDER_REVIEW'))
   OR (e.operation='REQUEST_INFORMATION' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'NEEDS_INFORMATION'))
   OR (e.operation='REJECT' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'REJECTED'))
   OR (e.operation='APPROVE' AND (OLD.state<>'UNDER_REVIEW' OR NEW.state<>'APPROVED'))
   OR e.operation NOT IN ('ASSIGN_REVIEWER','BEGIN_REVIEW','READY_FOR_DECISION','REQUEST_INFORMATION','REJECT','APPROVE') THEN RAISE EXCEPTION 'REVIEW_TRANSITION_DENIED' USING ERRCODE='23514';END IF;
  IF e.operation<>'ASSIGN_REVIEWER' AND NOT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment a JOIN merchant.business_reviewer_scope s ON s.id=a.reviewer_scope_id
   WHERE a.application_id=NEW.id AND a.policy_id=NEW.policy_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['application.review']
    AND ((e.operation NOT IN ('REJECT','APPROVE') AND a.reviewer_account_id=e.actor_id) OR (e.operation IN ('REJECT','APPROVE') AND a.reviewer_account_id<>e.actor_id)))
   THEN RAISE EXCEPTION 'INDEPENDENT_CURRENT_REVIEW_ASSIGNMENT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE RAISE EXCEPTION 'APPLICATION_OPERATOR_MUTATION_DENIED' USING ERRCODE='42501';END IF;
 IF e.operation='BEGIN_REVIEW' AND OLD.state='UNDER_REVIEW' AND EXISTS(
  SELECT 1 FROM merchant.governance_event prior JOIN merchant.application_reviewer_assignment a ON a.application_id=prior.application_id
  WHERE prior.application_id=NEW.id AND prior.revision=NEW.current_revision AND prior.operation='BEGIN_REVIEW'
   AND prior.actor_id=a.reviewer_account_id AND prior.occurred_at>=a.assigned_at AND prior.new_version<=e.prior_version)
  THEN RAISE EXCEPTION 'CURRENT_ASSIGNMENT_ALREADY_BEGAN_REVIEW' USING ERRCODE='23514';END IF;
 IF e.operation IN ('REJECT','APPROVE') AND NOT EXISTS(
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
 IF NEW.state='APPROVED' AND NOT EXISTS(SELECT 1 FROM merchant.application_activation x WHERE x.application_id=NEW.id AND x.decision_event_id=e.id AND x.revision=NEW.current_revision AND merchant.application_origin_admitted(x.organization_id)) THEN RAISE EXCEPTION 'ATOMIC_APPLICATION_ACTIVATION_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
SET LOCAL ROLE merchant_governance_writer;
CREATE OR REPLACE FUNCTION merchant.guard_governance_system_eligibility() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE binding JSONB;
BEGIN
 IF NEW.actor_kind='SYSTEM' AND (NEW.realm_id IS NULL OR NOT identity.local_admin_eligible(NEW.actor_id,NEW.realm_id))
  THEN RAISE EXCEPTION 'CURRENT_DEDICATED_ADMIN_REQUIRED' USING ERRCODE='42501';END IF;
 IF NEW.actor_kind='SYSTEM' AND NEW.operation IN ('REJECT','APPROVE','SUSPEND','REINSTATE') THEN
  -- A caller-set descriptor is not authority: only the existing Identity-owned,
  -- consumed and request/action/target/version-bound canonical proof can satisfy it.
  -- No extra Identity SELECT, runtime EXECUTE, role membership or SQL signature.
  BEGIN
   binding:=nullif(current_setting('argopost.governance_stepup',true),'')::JSONB;
   IF binding IS NULL OR NOT coalesce(identity.admin_governance_command_bound((binding->>'proofId')::UUID,NEW.request_id,NEW.actor_id,binding->>'authorityVersion',NEW.operation,NEW.target_id,NEW.prior_version,decode(binding->>'inputDigest','hex')),false)
    THEN RAISE EXCEPTION 'CURRENT_BOUND_DECISION_PROOF_REQUIRED' USING ERRCODE='42501';END IF;
  EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'CURRENT_BOUND_DECISION_PROOF_REQUIRED' USING ERRCODE='42501';END;
 END IF;
 RETURN NEW;
END $$;
RESET ROLE;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;
CREATE OR REPLACE FUNCTION merchant.check_team_member_insert() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE i merchant.team_invitation%ROWTYPE;
BEGIN
 IF merchant.team_offline_operator() THEN RETURN NULL;END IF;
 IF NEW.role='OWNER' AND NEW.state='ACTIVE' AND NEW.version=1 AND NEW.authority_version=1 AND NEW.created_at=NEW.updated_at AND EXISTS(
  SELECT 1 FROM merchant.application_activation x JOIN merchant.organization o ON o.id=x.organization_id JOIN merchant.application a ON a.id=x.application_id
  WHERE x.owner_membership_id=NEW.id AND x.organization_id=NEW.organization_id AND x.applicant_id=NEW.account_id AND x.activated_at=NEW.created_at
   AND o.initial_owner_membership_id=NEW.id AND o.admission_origin='APPLICATION_APPROVED' AND o.state='ACTIVE' AND a.state='APPROVED'
   AND x.xmin::text=pg_current_xact_id()::text AND o.xmin::text=pg_current_xact_id()::text AND merchant.application_origin_admitted(o.id)) THEN RETURN NULL;END IF;
 SELECT * INTO i FROM merchant.team_invitation WHERE accepted_membership_id=NEW.id AND organization_id=NEW.organization_id AND state='ACCEPTED';
 IF i.id IS NULL OR NEW.role<>i.role OR NEW.state<>'ACTIVE' OR NEW.version<>1 OR NEW.authority_version<>1 OR NEW.created_at<>NEW.updated_at
  OR NOT EXISTS(SELECT 1 FROM merchant.team_command_receipt r WHERE r.invitation_id=i.id AND r.organization_id=i.organization_id
   AND r.operation='ACCEPT' AND r.actor_account_id=NEW.account_id AND r.recorded_at=NEW.created_at AND r.version=i.version AND r.xmin::text=pg_current_xact_id()::text)
  OR NOT identity.membership_decision_consumed(i.id,i.organization_id,i.proof_version,NEW.account_id,NEW.created_at)
 THEN RAISE EXCEPTION 'TEAM_ACCEPTANCE_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE OR REPLACE FUNCTION merchant.check_team_grant_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE g merchant.member_grant%ROWTYPE; m merchant.membership%ROWTYPE; r merchant.team_command_receipt%ROWTYPE; i merchant.team_invitation%ROWTYPE;
BEGIN
 IF merchant.team_offline_operator() THEN RETURN NULL;END IF;
 IF TG_OP='DELETE' THEN g:=OLD;ELSE g:=NEW;END IF;
 SELECT * INTO m FROM merchant.membership WHERE id=g.membership_id AND organization_id=g.organization_id;
 IF TG_OP='INSERT' AND g.permission='dashboard.read' AND g.scope='ORGANIZATION' AND g.facility_id IS NULL AND m.role='OWNER'
  AND EXISTS(SELECT 1 FROM merchant.application_activation x WHERE x.organization_id=g.organization_id AND x.owner_membership_id=g.membership_id
   AND x.applicant_id=m.account_id AND x.xmin::text=pg_current_xact_id()::text AND merchant.application_origin_admitted(x.organization_id)) THEN RETURN NULL;END IF;
 IF m.id IS NULL OR m.role='OWNER' OR NOT EXISTS(SELECT 1 FROM merchant.membership WHERE id=m.id AND xmin::text=pg_current_xact_id()::text)
  THEN RAISE EXCEPTION 'TEAM_GRANT_MEMBERSHIP_COMMAND_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO i FROM merchant.team_invitation WHERE accepted_membership_id=m.id AND organization_id=m.organization_id AND state='ACCEPTED';
 IF TG_OP='INSERT' AND m.version=1 AND i.id IS NOT NULL AND EXISTS(SELECT 1 FROM merchant.team_command_receipt receipt WHERE receipt.invitation_id=i.id
  AND receipt.organization_id=i.organization_id AND receipt.operation='ACCEPT' AND receipt.actor_account_id=m.account_id AND receipt.xmin::text=pg_current_xact_id()::text)
  AND EXISTS(SELECT 1 FROM merchant.team_invitation_grant intent WHERE intent.invitation_id=i.id AND intent.permission=g.permission AND intent.scope=g.scope AND intent.facility_id IS NOT DISTINCT FROM g.facility_id)
 THEN RETURN NULL;END IF;
 SELECT * INTO r FROM merchant.team_command_receipt receipt WHERE receipt.membership_id=m.id AND receipt.organization_id=m.organization_id
  AND receipt.version=m.version AND receipt.recorded_at=m.updated_at AND receipt.operation IN ('ACCESS','REVOKE_MEMBER') AND receipt.xmin::text=pg_current_xact_id()::text;
 IF r.membership_id IS NULL OR NOT identity.team_reauthenticated_in_transaction(r.actor_account_id,m.updated_at)
  OR NOT merchant.team_permission(m.organization_id,r.actor_account_id,CASE r.operation WHEN 'ACCESS' THEN 'team.access.manage' ELSE 'team.membership.manage' END)
  OR NOT EXISTS(SELECT 1 FROM merchant.membership actor WHERE actor.organization_id=m.organization_id AND actor.account_id=r.actor_account_id AND actor.role='OWNER' AND actor.state='ACTIVE' AND actor.id<>m.id)
  OR (r.operation='REVOKE_MEMBER' AND (TG_OP<>'DELETE' OR m.state<>'REVOKED'))
  OR (r.operation='ACCESS' AND (m.state<>'ACTIVE' OR NOT merchant.team_permission(g.organization_id,r.actor_account_id,g.permission,g.facility_id)))
  OR (TG_OP='INSERT' AND NOT merchant.team_can_delegate(g.organization_id,r.actor_account_id,m.role,g.permission,g.facility_id))
  OR (TG_OP='INSERT' AND g.facility_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=g.facility_id AND f.organization_id=g.organization_id AND f.state='ACTIVE'))
 THEN RAISE EXCEPTION 'TEAM_GRANT_DELEGATION_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;

ALTER TABLE merchant.organization DROP CONSTRAINT organization_admission_origin_check;
ALTER TABLE merchant.organization ADD CONSTRAINT organization_admission_origin_check CHECK(admission_origin IN ('LOCAL_OPERATOR_PROVISIONED','APPLICATION_APPROVED'));
CREATE FUNCTION merchant.require_atomic_application_activation() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE org UUID;
BEGIN
 IF TG_TABLE_NAME='organization' THEN
  IF NEW.admission_origin<>'APPLICATION_APPROVED' THEN RETURN NULL;END IF;org:=NEW.id;
 ELSE org:=NEW.organization_id;END IF;
 IF NOT merchant.application_origin_admitted(org) OR NOT EXISTS(
  SELECT 1 FROM merchant.application_activation x JOIN merchant.organization o ON o.id=x.organization_id
   JOIN merchant.membership m ON m.id=x.owner_membership_id AND m.organization_id=x.organization_id
   JOIN merchant.application a ON a.id=x.application_id JOIN merchant.governance_event e ON e.id=x.decision_event_id
   JOIN merchant.governance_receipt r ON r.event_id=e.id AND r.operation='APPROVE' AND r.target_id=a.id AND r.actor_binding=e.actor_id::text AND r.realm_id=e.realm_id
   JOIN merchant.team_policy tp ON tp.organization_id=o.id
  WHERE x.organization_id=org AND x.xmin::text=pg_current_xact_id()::text AND o.xmin::text=pg_current_xact_id()::text
   AND m.xmin::text=pg_current_xact_id()::text AND a.xmin::text=pg_current_xact_id()::text
   AND e.xmin::text=pg_current_xact_id()::text AND r.xmin::text=pg_current_xact_id()::text
   AND o.state='ACTIVE' AND o.version=1 AND m.state='ACTIVE' AND m.role='OWNER' AND m.version=1 AND m.authority_version=1
   AND m.created_at=x.activated_at AND o.created_at=x.activated_at AND tp.version=1 AND tp.template_version=1
   AND a.state='APPROVED' AND a.version=e.new_version AND a.terminal_at=x.activated_at
   AND (SELECT count(*) FROM merchant.membership mm WHERE mm.organization_id=org)=1
   AND (SELECT count(*) FROM merchant.member_grant g WHERE g.organization_id=org)=1
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.membership_id=m.id AND g.permission='dashboard.read' AND g.scope='ORGANIZATION' AND g.facility_id IS NULL)
 ) THEN RAISE EXCEPTION 'EXACT_INITIAL_OWNER_ACTIVATION_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER approved_organization_activation AFTER INSERT ON merchant.organization DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_atomic_application_activation();
CREATE CONSTRAINT TRIGGER application_activation_complete AFTER INSERT ON merchant.application_activation DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_atomic_application_activation();
REVOKE ALL ON FUNCTION merchant.require_atomic_application_activation(),merchant.guard_application_activation_rows() FROM PUBLIC,"${runtimeRole}";

CREATE FUNCTION merchant.activate_approved_application(p_request UUID,p_application UUID,p_revision BIGINT,p_expected_version BIGINT,p_decision_event UUID,p_reauth_proof UUID) RETURNS UUID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE a merchant.application; e merchant.governance_event; p merchant.application_policy; rev merchant.application_revision; binding JSONB;
 org UUID:=gen_random_uuid(); member UUID:=gen_random_uuid();
BEGIN
 IF p_request IS NULL OR p_application IS NULL OR p_revision IS NULL OR p_revision<1 OR p_expected_version IS NULL OR p_expected_version<1 OR p_decision_event IS NULL OR p_reauth_proof IS NULL THEN RAISE EXCEPTION 'ACTIVATION_INPUT_REQUIRED' USING ERRCODE='23514';END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT * INTO a FROM merchant.application WHERE id=p_application FOR UPDATE;
 SELECT * INTO e FROM merchant.governance_event WHERE id=p_decision_event;
 SELECT * INTO p FROM merchant.application_policy WHERE id=a.policy_id FOR SHARE;
 SELECT * INTO rev FROM merchant.application_revision WHERE application_id=a.id AND revision=p_revision;
 IF a.id IS NULL OR a.state<>'UNDER_REVIEW' OR a.version<>p_expected_version OR a.current_revision<>p_revision OR a.submitted_revision<>p_revision
  OR p.id IS NULL OR p.state<>'PUBLISHED' OR NOT identity.local_admin_environment_matches(p.environment) OR p.environment NOT IN ('LOCAL','TEST')
  OR clock_timestamp()<p.valid_from OR p.valid_to IS NOT NULL AND clock_timestamp()>=p.valid_to
  OR rev.policy_id<>p.id OR rev.policy_version<>p.version OR rev.submitted_input<>a.draft
  OR e.id IS NULL OR e.operation<>'APPROVE' OR e.actor_kind<>'SYSTEM' OR e.request_id<>p_request OR e.application_id<>a.id OR e.target_id<>a.id OR e.target_type<>'APPLICATION'
  OR e.policy_id<>p.id OR e.revision<>p_revision OR e.prior_version<>p_expected_version OR e.new_version<>p_expected_version+1 OR e.actor_id=a.applicant_account_id
  OR NOT EXISTS(SELECT 1 FROM merchant.governance_event ev WHERE ev.id=e.id AND ev.xmin::text=pg_current_xact_id()::text)
  OR NOT identity.local_admin_eligible(e.actor_id,e.realm_id)
  OR NOT EXISTS(SELECT 1 FROM merchant.governance_receipt r WHERE r.event_id=e.id AND r.actor_kind='SYSTEM' AND r.actor_binding=e.actor_id::text AND r.realm_id=e.realm_id AND r.operation='APPROVE' AND r.target_id=a.id AND r.original_version=a.version AND r.result_version=e.new_version AND r.outcome='COMMITTED' AND r.input_key_id<>'SQL_SHA256_1' AND r.xmin::text=pg_current_xact_id()::text)
 THEN RAISE EXCEPTION 'CURRENT_BOUND_APPROVAL_REQUIRED' USING ERRCODE='42501';END IF;
 BEGIN
  binding:=nullif(current_setting('argopost.governance_stepup',true),'')::JSONB;
  IF binding IS NULL OR (binding->>'proofId')::UUID<>p_reauth_proof OR NOT coalesce(identity.admin_governance_command_bound(p_reauth_proof,p_request,e.actor_id,binding->>'authorityVersion','APPROVE',a.id,a.version,decode(binding->>'inputDigest','hex')),false)
   THEN RAISE EXCEPTION 'BOUND_APPROVAL_PROOF_REQUIRED' USING ERRCODE='42501';END IF;
 EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'BOUND_APPROVAL_PROOF_REQUIRED' USING ERRCODE='42501';END;
 IF NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.account_id=e.actor_id AND s.realm_id=e.realm_id AND s.policy_id=p.id AND s.scope_type='POLICY' AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['application.decide'])
 OR NOT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment x JOIN merchant.governance_event ready ON ready.application_id=x.application_id AND ready.actor_id=x.reviewer_account_id JOIN merchant.business_reviewer_scope rs ON rs.id=x.reviewer_scope_id
  WHERE x.application_id=a.id AND x.policy_id=p.id AND x.reviewer_account_id<>e.actor_id AND x.reviewer_account_id<>a.applicant_account_id
   AND rs.state='ACTIVE' AND rs.expires_at>clock_timestamp() AND rs.permissions @> ARRAY['application.review']
   AND identity.local_admin_eligible(x.reviewer_account_id,e.realm_id) AND ready.operation='READY_FOR_DECISION' AND ready.reason_code='CHECKLIST_COMPLETE' AND ready.revision=p_revision AND ready.new_version=a.version AND ready.occurred_at>=x.assigned_at)
 OR EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.membership applicant ON applicant.organization_id=m.organization_id WHERE m.account_id=e.actor_id AND applicant.account_id=a.applicant_account_id AND m.state<>'REVOKED' AND applicant.state<>'REVOKED')
 THEN RAISE EXCEPTION 'INDEPENDENT_CURRENT_REVIEW_REQUIRED' USING ERRCODE='42501';END IF;
 -- Legacy operator Organizations lack authoritative legal-key data; do not invent clearance or backfill.
 IF EXISTS(SELECT 1 FROM merchant.organization o WHERE o.admission_origin='LOCAL_OPERATOR_PROVISIONED' AND NOT EXISTS(SELECT 1 FROM merchant.business_key_registry k WHERE k.organization_id=o.id)) THEN RAISE EXCEPTION 'LEGACY_BUSINESS_KEY_CLEARANCE_REQUIRED' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM merchant.business_key_registry k WHERE k.jurisdiction_key=normalize(btrim(a.draft->>'jurisdiction'),NFC) AND k.entity_type=a.draft->>'entityType' AND k.legal_key=normalize(regexp_replace(a.draft->>'legalIdentifier','[- ./_]','','g'),NFC)) THEN RAISE EXCEPTION 'BUSINESS_KEY_ALREADY_ACTIVATED' USING ERRCODE='23505';END IF;
 INSERT INTO merchant.application_activation VALUES(a.id,p_revision,e.id,org,member,a.applicant_account_id,p.id,p_request,e.occurred_at);
 INSERT INTO merchant.business_key_registry VALUES(normalize(btrim(a.draft->>'jurisdiction'),NFC),a.draft->>'entityType',normalize(regexp_replace(a.draft->>'legalIdentifier','[- ./_]','','g'),NFC),org,a.id,p_revision,e.id,e.occurred_at);
 INSERT INTO merchant.organization VALUES(org,rev.activation_display_name,'ACTIVE','APPLICATION_APPROVED',p.environment,member,1,e.occurred_at,e.occurred_at);
 INSERT INTO merchant.membership VALUES(member,org,a.applicant_account_id,'OWNER','ACTIVE',1,1,e.occurred_at,e.occurred_at);
 INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope,facility_id) VALUES(gen_random_uuid(),member,org,'dashboard.read','ORGANIZATION',NULL);
 UPDATE merchant.application SET state='APPROVED',version=version+1,updated_at=e.occurred_at,terminal_at=e.occurred_at WHERE id=a.id AND version=p_expected_version;
 INSERT INTO merchant.application_draft_version VALUES(a.id,p_expected_version+1,a.applicant_account_id,a.draft,a.created_at,e.occurred_at);
 RETURN org;
END $$;
GRANT SELECT,INSERT ON merchant.organization,merchant.membership,merchant.member_grant,merchant.team_policy,merchant.application_draft_version TO merchant_governance_writer;
-- Required by the unchanged member_grant CHECK; pure immutable role-name allowlist, no grant/authority mutation.
GRANT EXECUTE ON FUNCTION merchant.team_role_ceiling(TEXT),merchant.team_offline_operator() TO merchant_governance_writer;
-- The unchanged operator predicate remains false for this non-schema-owner writer.
GRANT UPDATE(state,version,updated_at,terminal_at) ON merchant.application TO merchant_governance_writer;
REVOKE ALL ON FUNCTION merchant.activate_approved_application(UUID,UUID,BIGINT,BIGINT,UUID,UUID) FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION merchant.activate_approved_application(UUID,UUID,BIGINT,BIGINT,UUID,UUID) TO "${runtimeRole}";
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
ALTER FUNCTION merchant.activate_approved_application(UUID,UUID,BIGINT,BIGINT,UUID,UUID) OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;
CREATE OR REPLACE FUNCTION orders.guard_report_export_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE member UUID; permission_name TEXT; selected_site UUID;
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT m.id INTO member FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
  WHERE m.organization_id=NEW.organization_id AND m.account_id=NEW.actor_account_id AND m.state='ACTIVE'
   AND o.state='ACTIVE' AND o.environment IN ('LOCAL','TEST') AND merchant.application_origin_admitted(o.id);
 permission_name:=CASE WHEN NEW.kind='FINANCE' THEN 'shipment.finance.read' ELSE 'shipment.reports.read' END;
 IF member IS NULL OR (SELECT count(DISTINCT v) FROM unnest(NEW.contributing_shipment_ids) v)<>cardinality(NEW.contributing_shipment_ids)
  OR (SELECT count(*) FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.contributing_shipment_ids) AND s.organization_id=NEW.organization_id AND s.owner_type='BUSINESS')<>cardinality(NEW.contributing_shipment_ids)
 THEN RAISE EXCEPTION 'REPORT_EXPORT_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM orders.pilot_shipment s CROSS JOIN unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p
  WHERE s.id=ANY(NEW.contributing_shipment_ids) AND NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id)))
 THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF NEW.canonical_query ? 'facilityId' THEN
  selected_site:=(NEW.canonical_query->>'facilityId')::uuid;
  IF NOT EXISTS(SELECT 1 FROM merchant.facility WHERE id=selected_site AND organization_id=NEW.organization_id)
   OR EXISTS(SELECT 1 FROM unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p WHERE NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=selected_site)))
   OR EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.contributing_shipment_ids) AND s.facility_id IS DISTINCT FROM selected_site)
  THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF cardinality(NEW.contributing_shipment_ids)=0 AND NOT (
  (SELECT count(DISTINCT permission) FROM merchant.member_grant WHERE membership_id=member AND scope='ORGANIZATION' AND permission=ANY(ARRAY['shipment.draft.read',permission_name,'shipment.export']))=3
  OR EXISTS(SELECT 1 FROM merchant.facility f WHERE f.organization_id=NEW.organization_id AND (selected_site IS NULL OR f.id=selected_site)
   AND NOT EXISTS(SELECT 1 FROM unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p WHERE NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=f.id)))))
 THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF NEW.occurred_at<(NEW.canonical_query->>'from')::date::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh'
  OR (NEW.canonical_query->>'until')::date>(NEW.occurred_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+1
 THEN RAISE EXCEPTION 'REPORT_EXPORT_PERIOD_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION merchant.contact_scope(p_org UUID,p_actor UUID,p_facility UUID,p_permission TEXT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
  JOIN merchant.member_grant g ON g.membership_id=m.id AND g.organization_id=o.id
  WHERE m.organization_id=p_org AND m.account_id=p_actor AND m.state='ACTIVE' AND o.state='ACTIVE'
   AND o.environment IN ('LOCAL','TEST') AND merchant.application_origin_admitted(o.id)
   AND g.permission=p_permission AND (g.scope='ORGANIZATION' OR g.facility_id=p_facility))
$$;

-- Organization lifecycle remains separate from Carrier/Finance and operator-origin workspaces.
CREATE FUNCTION merchant.guard_approved_organization_lifecycle() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e merchant.governance_event; policy UUID; required TEXT; binding JSONB;
BEGIN
 IF OLD.admission_origin='LOCAL_OPERATOR_PROVISIONED' THEN
  IF coalesce(nullif(current_setting('role',true),'none'),session_user::text)<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) OR coalesce(current_setting('argopost.operator_environment',true),'') NOT IN ('LOCAL','TEST')
   THEN RAISE EXCEPTION 'LEGACY_ORGANIZATION_OPERATOR_REQUIRED' USING ERRCODE='42501';END IF;RETURN NEW;
 END IF;
 SELECT x.policy_id INTO policy FROM merchant.application_activation x WHERE x.organization_id=OLD.id;
 SELECT ev.* INTO e FROM merchant.governance_event ev JOIN merchant.governance_receipt r ON r.event_id=ev.id
  WHERE ev.target_type='ORGANIZATION' AND ev.organization_id=OLD.id AND ev.target_id=OLD.id AND ev.actor_kind='SYSTEM'
   AND ev.prior_version=OLD.version AND ev.new_version=NEW.version AND ev.occurred_at=NEW.updated_at
   AND ev.xmin::text=pg_current_xact_id()::text AND r.xmin::text=pg_current_xact_id()::text
   AND r.actor_kind='SYSTEM' AND r.actor_binding=ev.actor_id::text AND r.realm_id=ev.realm_id AND r.target_type='ORGANIZATION' AND r.target_id=OLD.id
   AND r.operation=ev.operation AND r.original_version=OLD.version AND r.result_version=NEW.version AND r.outcome='COMMITTED' AND r.input_key_id<>'SQL_SHA256_1';
 IF policy IS NULL OR e.id IS NULL OR e.policy_id IS DISTINCT FROM policy OR NEW.version<>OLD.version+1 OR NOT merchant.application_origin_admitted(OLD.id)
  OR NOT identity.local_admin_eligible(e.actor_id,e.realm_id)
  OR NOT ((OLD.state='ACTIVE' AND NEW.state='SUSPENDED' AND e.operation='SUSPEND') OR (OLD.state='SUSPENDED' AND NEW.state='ACTIVE' AND e.operation='REINSTATE'))
 THEN RAISE EXCEPTION 'BOUND_ORGANIZATION_GOVERNANCE_REQUIRED' USING ERRCODE='42501';END IF;
 required:=CASE e.operation WHEN 'SUSPEND' THEN 'org.suspend' ELSE 'org.reinstate' END;
 IF NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.account_id=e.actor_id AND s.realm_id=e.realm_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp()
   AND s.permissions @> ARRAY[required] AND ((s.scope_type='ORGANIZATION' AND s.organization_id=OLD.id) OR (s.scope_type='POLICY' AND s.policy_id=policy AND EXISTS(SELECT 1 FROM merchant.application_policy p WHERE p.id=policy AND p.state='PUBLISHED' AND p.valid_from<=clock_timestamp() AND (p.valid_to IS NULL OR p.valid_to>clock_timestamp())))))
  OR EXISTS(SELECT 1 FROM merchant.membership m WHERE m.organization_id=OLD.id AND m.account_id=e.actor_id AND m.state<>'REVOKED')
  OR (e.operation='REINSTATE' AND NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.id=OLD.initial_owner_membership_id AND m.organization_id=OLD.id AND m.role='OWNER' AND m.state='ACTIVE'))
 THEN RAISE EXCEPTION 'CURRENT_INDEPENDENT_ORGANIZATION_SCOPE_REQUIRED' USING ERRCODE='42501';END IF;
 BEGIN
  binding:=nullif(current_setting('argopost.governance_stepup',true),'')::JSONB;
  IF binding IS NULL OR NOT coalesce(identity.admin_governance_command_bound((binding->>'proofId')::UUID,e.request_id,e.actor_id,binding->>'authorityVersion',e.operation,OLD.id,OLD.version,decode(binding->>'inputDigest','hex')),false)
   THEN RAISE EXCEPTION 'CURRENT_BOUND_ORGANIZATION_PROOF_REQUIRED' USING ERRCODE='42501';END IF;
 EXCEPTION WHEN OTHERS THEN RAISE EXCEPTION 'CURRENT_BOUND_ORGANIZATION_PROOF_REQUIRED' USING ERRCODE='42501';END;
 RETURN NEW;
END $$;
CREATE TRIGGER approved_organization_lifecycle BEFORE UPDATE ON merchant.organization FOR EACH ROW EXECUTE FUNCTION merchant.guard_approved_organization_lifecycle();
REVOKE ALL ON FUNCTION merchant.guard_approved_organization_lifecycle() FROM PUBLIC,"${runtimeRole}";
GRANT UPDATE(state,version,updated_at) ON merchant.organization TO "${runtimeRole}";
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
ALTER FUNCTION merchant.guard_approved_organization_lifecycle() OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;
