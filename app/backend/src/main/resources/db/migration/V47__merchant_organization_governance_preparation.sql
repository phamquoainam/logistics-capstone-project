-- Owner Policy V1 / Step A only. V1-V46 immutable. No grants/policies/roles seeded.
DO $$ DECLARE c RECORD;BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='merchant.business_reviewer_scope'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%permissions%' LOOP
  EXECUTE format('ALTER TABLE merchant.business_reviewer_scope DROP CONSTRAINT %I',c.conname);
 END LOOP;
END $$;
ALTER TABLE merchant.business_reviewer_scope ADD CONSTRAINT m2l_permissions CHECK(
 cardinality(permissions) BETWEEN 1 AND 15 AND array_position(permissions,NULL) IS NULL
 AND permissions <@ ARRAY['application.read','application.evidence.read','application.assign','application.review','application.decide','org.read','org.suspend','org.reinstate','audit.read','task.read','application.comment.write','org.profile.read','org.members.read','org.lifecycle.propose','org.lifecycle.review']::TEXT[]);
ALTER TABLE merchant.business_reviewer_scope ADD CONSTRAINT m2l_scope CHECK(
 (scope_type='POLICY' AND policy_id IS NOT NULL AND organization_id IS NULL AND NOT permissions && ARRAY['org.profile.read','org.members.read','org.lifecycle.propose','org.lifecycle.review']::TEXT[])
 OR (scope_type='ORGANIZATION' AND policy_id IS NULL AND organization_id IS NOT NULL AND permissions <@ ARRAY['org.read','org.suspend','org.reinstate','audit.read','org.profile.read','org.members.read','org.lifecycle.propose','org.lifecycle.review']::TEXT[]));
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
DO $$ DECLARE definition TEXT; migration_owner NAME:=current_user;BEGIN
 EXECUTE 'SET LOCAL ROLE merchant_governance_writer';
 SELECT pg_get_functiondef('merchant.configure_local_reviewer_scope(uuid,uuid,uuid,text,uuid,text[],text,bigint,timestamptz,text)'::regprocedure) INTO definition;
 IF position('cardinality(p_permissions) NOT BETWEEN 1 AND 13' IN definition)=0 THEN RAISE EXCEPTION 'M2L_OPERATOR_SOURCE_DRIFT';END IF;
 definition:=replace(definition,'cardinality(p_permissions) NOT BETWEEN 1 AND 13','cardinality(p_permissions) NOT BETWEEN 1 AND 15');
 definition:=replace(definition,'''org.profile.read'',''org.members.read'']::TEXT[]','''org.profile.read'',''org.members.read'',''org.lifecycle.propose'',''org.lifecycle.review'']::TEXT[]');
 EXECUTE definition;
 EXECUTE format('SET LOCAL ROLE %I',migration_owner);
END $$;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;

CREATE TABLE merchant.organization_governance_policy (
 id UUID PRIMARY KEY,code VARCHAR(64) NOT NULL,version BIGINT NOT NULL CHECK(version>0),
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 state VARCHAR(12) NOT NULL CHECK(state IN ('PUBLISHED','RETIRED')),body JSONB NOT NULL,
 digest BYTEA NOT NULL CHECK(octet_length(digest)=32),valid_from TIMESTAMPTZ NOT NULL,
 valid_to TIMESTAMPTZ CHECK(valid_to>valid_from),published_event_id UUID NOT NULL,
 UNIQUE(code,version,environment),UNIQUE(id,version)
);
CREATE TABLE merchant.organization_governance_case (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 type VARCHAR(24) NOT NULL CHECK(type IN ('RESTRICT','RELEASE_RESTRICTION','CLOSE','RISK_HOLD','RELEASE_HOLD','DISPUTE','REVERIFY','OWNER_TRANSFER')),
 state VARCHAR(24) NOT NULL CHECK(state IN ('DRAFT','SUBMITTED','UNDER_REVIEW','NEEDS_INFORMATION','READY_FOR_DECISION','APPROVED','REJECTED','EXECUTED','CANCELLED','EXPIRED')),
 version BIGINT NOT NULL CHECK(version>0),revision BIGINT NOT NULL CHECK(revision>0),
 policy_id UUID NOT NULL,policy_version BIGINT NOT NULL,expected_org_version BIGINT NOT NULL CHECK(expected_org_version>0),
 expected_ownership_version BIGINT NOT NULL CHECK(expected_ownership_version>0),
 maker_kind VARCHAR(8) NOT NULL CHECK(maker_kind IN ('SYSTEM','CUSTOMER')),maker_account_id UUID NOT NULL,
 maker_realm_id UUID,protected_payload BYTEA NOT NULL CHECK(octet_length(protected_payload) BETWEEN 32 AND 33000),
 impact_digest BYTEA NOT NULL CHECK(octet_length(impact_digest)=32),
 expires_at TIMESTAMPTZ NOT NULL,effective_at TIMESTAMPTZ,created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL,
 last_event_id UUID NOT NULL,UNIQUE(id,organization_id),
 FOREIGN KEY(policy_id,policy_version) REFERENCES merchant.organization_governance_policy(id,version),
 CHECK((maker_kind='SYSTEM')=(maker_realm_id IS NOT NULL)),CHECK(expires_at=created_at+INTERVAL '7 days'),CHECK(updated_at>=created_at),
 CHECK(effective_at IS NULL OR effective_at<=created_at)
);
CREATE UNIQUE INDEX one_open_owner_case ON merchant.organization_governance_case(organization_id) WHERE type='OWNER_TRANSFER' AND state NOT IN ('CANCELLED','EXPIRED','REJECTED','EXECUTED');
CREATE UNIQUE INDEX one_open_closure_case ON merchant.organization_governance_case(organization_id) WHERE type='CLOSE' AND state NOT IN ('CANCELLED','EXPIRED','REJECTED','EXECUTED');
CREATE INDEX org_cases_scope ON merchant.organization_governance_case(organization_id,created_at DESC,id DESC);
CREATE TABLE merchant.organization_governance_event (
 id UUID PRIMARY KEY,case_id UUID,organization_id UUID REFERENCES merchant.organization(id),actor_kind VARCHAR(8) NOT NULL CHECK(actor_kind IN ('SYSTEM','CUSTOMER','OPERATOR')),
 actor_account_id UUID,realm_id UUID,operator_principal TEXT,
 operation VARCHAR(40) NOT NULL CHECK(operation IN ('PUBLISH_ORG_POLICY','RETIRE_ORG_POLICY','CREATE_CASE','SUBMIT_CASE','ASSIGN_CASE_REVIEWER','BEGIN_CASE_REVIEW','REQUEST_CASE_INFORMATION','RESUBMIT_CASE','READY_CASE_DECISION','CANCEL_CASE','EXPIRE_CASE','READ_CASE_DISCLOSURE')),
 revision BIGINT NOT NULL CHECK(revision>=0),prior_version BIGINT NOT NULL CHECK(prior_version>=0),new_version BIGINT NOT NULL,
 policy_id UUID REFERENCES merchant.organization_governance_policy(id) DEFERRABLE INITIALLY DEFERRED,policy_version BIGINT NOT NULL,
 reason_code VARCHAR(40) NOT NULL CHECK(reason_code IN ('ORG_POLICY_PUBLISHED','ORG_POLICY_RETIRED','CASE_CREATED','CASE_SUBMITTED','CASE_REVIEWER_ASSIGNED','CASE_REVIEW_STARTED','CASE_INFORMATION_REQUIRED','CASE_RESUBMITTED','CASE_CHECKLIST_COMPLETE','CASE_CANCELLED','CASE_EXPIRED','CASE_DISCLOSURE')),
 protected_payload BYTEA CHECK(protected_payload IS NULL OR octet_length(protected_payload) BETWEEN 32 AND 33000),request_id UUID NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE NULLS NOT DISTINCT(request_id,operation,organization_id,case_id),
 FOREIGN KEY(case_id,organization_id) REFERENCES merchant.organization_governance_case(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 CHECK((operation='READ_CASE_DISCLOSURE' AND new_version=prior_version) OR (operation<>'READ_CASE_DISCLOSURE' AND new_version=prior_version+1)),
 CHECK((actor_kind='OPERATOR' AND actor_account_id IS NULL AND realm_id IS NULL AND operator_principal IS NOT NULL AND organization_id IS NULL AND case_id IS NULL AND operation IN ('PUBLISH_ORG_POLICY','RETIRE_ORG_POLICY'))
  OR (actor_kind='SYSTEM' AND actor_account_id IS NOT NULL AND realm_id IS NOT NULL AND operator_principal IS NULL AND organization_id IS NOT NULL AND case_id IS NOT NULL AND operation NOT IN ('PUBLISH_ORG_POLICY','RETIRE_ORG_POLICY')))
);
ALTER TABLE merchant.organization_governance_policy ADD FOREIGN KEY(published_event_id) REFERENCES merchant.organization_governance_event(id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE merchant.organization_governance_case ADD FOREIGN KEY(last_event_id) REFERENCES merchant.organization_governance_event(id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE merchant.organization_governance_receipt (
 actor_kind VARCHAR(8) NOT NULL,actor_binding TEXT NOT NULL,realm_id UUID,operation VARCHAR(40) NOT NULL,operation_key UUID NOT NULL CHECK(operation_key<>'00000000-0000-0000-0000-000000000000'),
 organization_id UUID REFERENCES merchant.organization(id),case_id UUID,original_version BIGINT NOT NULL,result_version BIGINT NOT NULL,
 input_key_id VARCHAR(64) NOT NULL,input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 event_id UUID NOT NULL UNIQUE REFERENCES merchant.organization_governance_event(id),protected_outcome BYTEA NOT NULL CHECK(octet_length(protected_outcome) BETWEEN 32 AND 33000),
 recorded_at TIMESTAMPTZ NOT NULL,expires_at TIMESTAMPTZ NOT NULL,policy_id UUID REFERENCES merchant.organization_governance_policy(id),
 PRIMARY KEY(actor_kind,actor_binding,operation,operation_key),
 FOREIGN KEY(case_id,organization_id) REFERENCES merchant.organization_governance_case(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 CHECK(result_version=original_version+1),CHECK(expires_at>=recorded_at+INTERVAL '7 days'),
 CHECK((actor_kind='OPERATOR' AND realm_id IS NULL AND organization_id IS NULL AND case_id IS NULL AND policy_id IS NOT NULL AND input_key_id='SQL_SHA256_1') OR (actor_kind='SYSTEM' AND realm_id IS NOT NULL AND organization_id IS NOT NULL AND case_id IS NOT NULL AND policy_id IS NOT NULL AND input_key_id<>'SQL_SHA256_1'))
);
CREATE TABLE merchant.organization_case_review (
 case_id UUID PRIMARY KEY,organization_id UUID NOT NULL,reviewer_account_id UUID NOT NULL,reviewer_realm_id UUID NOT NULL,
 authority_version TEXT NOT NULL CHECK(length(authority_version) BETWEEN 1 AND 1024),case_revision BIGINT NOT NULL CHECK(case_revision>0),version BIGINT NOT NULL CHECK(version>0),
 checklist JSONB,ready_event_id UUID REFERENCES merchant.organization_governance_event(id) DEFERRABLE INITIALLY DEFERRED,
 assigned_event_id UUID NOT NULL REFERENCES merchant.organization_governance_event(id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(case_id,organization_id) REFERENCES merchant.organization_governance_case(id,organization_id)
);
CREATE TABLE merchant.organization_current_owner (
 organization_id UUID PRIMARY KEY REFERENCES merchant.organization(id),
 membership_id UUID NOT NULL UNIQUE,version BIGINT NOT NULL CHECK(version=1),
 last_transfer_case_id UUID REFERENCES merchant.organization_governance_case(id),updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 CHECK(last_transfer_case_id IS NULL)
);
-- Genuine existing offline Identity eligibility check at migration time, never a runtime workaround.
DO $$ DECLARE r RECORD; migration_owner NAME:=current_user; admitted BOOLEAN; previous_environment TEXT:=current_setting('argopost.operator_environment',TRUE);BEGIN
 FOR r IN SELECT o.id,o.initial_owner_membership_id,m.account_id,m.role,m.state,o.environment FROM merchant.organization o LEFT JOIN merchant.membership m ON m.id=o.initial_owner_membership_id AND m.organization_id=o.id LOOP
  EXECUTE 'SET LOCAL ROLE merchant_governance_writer';
  admitted:=merchant.application_origin_admitted(r.id);
  EXECUTE format('SET LOCAL ROLE %I',migration_owner);
  IF r.account_id IS NULL OR r.role<>'OWNER' OR r.state<>'ACTIVE' OR r.environment NOT IN ('LOCAL','TEST') OR NOT coalesce(admitted,FALSE) THEN RAISE EXCEPTION 'CURRENT_OWNER_BACKFILL_INCONSISTENT' USING ERRCODE='23514';END IF;
  -- Transaction-local offline invocation under the unchanged schema-owner guard.
  -- This is sourced from the existing admitted Organization, never runtime/client input.
  PERFORM set_config('argopost.operator_environment',r.environment,TRUE);
  PERFORM identity.lock_local_business_subject(r.account_id,TRUE);
  INSERT INTO merchant.organization_current_owner VALUES(r.id,r.initial_owner_membership_id,1,NULL,clock_timestamp());
 END LOOP;
 PERFORM set_config('argopost.operator_environment',coalesce(previous_environment,''),TRUE);
END $$;

CREATE FUNCTION merchant.guard_organization_governance_rows() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e merchant.organization_governance_event;c merchant.organization_governance_case;required TEXT;p merchant.organization_governance_policy;m UUID;
BEGIN
 IF TG_TABLE_NAME='organization' THEN
  SELECT x.id INTO m FROM merchant.membership x WHERE x.id=NEW.initial_owner_membership_id AND x.organization_id=NEW.id AND x.role='OWNER' AND x.state='ACTIVE';
  IF m IS NULL OR NOT merchant.application_origin_admitted(NEW.id) THEN RAISE EXCEPTION 'CURRENT_OWNER_SOURCE_REQUIRED' USING ERRCODE='23514';END IF;
  INSERT INTO merchant.organization_current_owner VALUES(NEW.id,m,1,NULL,clock_timestamp()) ON CONFLICT(organization_id) DO NOTHING;
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_current_owner WHERE organization_id=NEW.id AND membership_id=m AND version=1 AND last_transfer_case_id IS NULL) THEN RAISE EXCEPTION 'CURRENT_OWNER_BRIDGE_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ORG_GOVERNANCE_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='organization_current_owner' THEN
  IF TG_OP<>'INSERT' OR NOT EXISTS(SELECT 1 FROM merchant.organization o JOIN merchant.membership x ON x.id=o.initial_owner_membership_id AND x.organization_id=o.id WHERE o.id=NEW.organization_id AND x.id=NEW.membership_id AND x.role='OWNER' AND x.state='ACTIVE' AND NEW.version=1 AND NEW.last_transfer_case_id IS NULL) THEN RAISE EXCEPTION 'CURRENT_OWNER_IMMUTABLE_STEP_A' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='organization_governance_policy' THEN
  IF session_user IN ('merchant_governance_writer','merchant_governance_operator') OR NOT pg_has_role(session_user,'merchant_governance_operator','MEMBER') THEN RAISE EXCEPTION 'ORG_POLICY_OFFLINE_ONLY' USING ERRCODE='42501';END IF;
  IF TG_OP='UPDATE' AND (OLD.state<>'PUBLISHED' OR NEW.state<>'RETIRED' OR to_jsonb(NEW)-'state'<>to_jsonb(OLD)-'state') THEN RAISE EXCEPTION 'ORG_POLICY_IMMUTABLE' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='organization_governance_event' THEN
  IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'ORG_EVENT_IMMUTABLE' USING ERRCODE='23514';END IF;
  IF NEW.actor_kind='OPERATOR' THEN
   IF NEW.operator_principal IS DISTINCT FROM session_user::TEXT OR NOT pg_has_role(session_user,'merchant_governance_operator','MEMBER') THEN RAISE EXCEPTION 'ORG_OPERATOR_REQUIRED' USING ERRCODE='42501';END IF;
  ELSE
   IF NOT identity.local_admin_eligible(NEW.actor_account_id,NEW.realm_id) OR EXISTS(SELECT 1 FROM merchant.membership WHERE organization_id=NEW.organization_id AND account_id=NEW.actor_account_id AND state<>'REVOKED') THEN RAISE EXCEPTION 'ORG_CURRENT_INDEPENDENT_ADMIN_REQUIRED' USING ERRCODE='42501';END IF;
   SELECT * INTO p FROM merchant.organization_governance_policy WHERE id=NEW.policy_id AND version=NEW.policy_version AND state='PUBLISHED' AND valid_from<=clock_timestamp() AND (valid_to IS NULL OR valid_to>clock_timestamp());
   IF NOT FOUND OR NOT identity.local_admin_environment_matches(p.environment) THEN RAISE EXCEPTION 'ORG_CURRENT_POLICY_REQUIRED' USING ERRCODE='42501';END IF;
   required:=CASE WHEN NEW.operation IN ('ASSIGN_CASE_REVIEWER','CREATE_CASE','SUBMIT_CASE','RESUBMIT_CASE','CANCEL_CASE','EXPIRE_CASE') THEN 'org.lifecycle.propose' WHEN NEW.operation='READ_CASE_DISCLOSURE' THEN 'org.read' ELSE 'org.lifecycle.review' END;
   IF NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.account_id=NEW.actor_account_id AND s.realm_id=NEW.realm_id AND s.organization_id=NEW.organization_id AND s.scope_type='ORGANIZATION' AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['org.read',required]) THEN RAISE EXCEPTION 'ORG_CASE_SCOPE_REQUIRED' USING ERRCODE='42501';END IF;
   IF NEW.operation='READ_CASE_DISCLOSURE' AND NOT EXISTS(SELECT 1 FROM merchant.organization_governance_case case_row WHERE case_row.id=NEW.case_id AND case_row.organization_id=NEW.organization_id AND case_row.version=NEW.prior_version AND case_row.version=NEW.new_version AND case_row.revision=NEW.revision AND case_row.policy_id=NEW.policy_id AND case_row.policy_version=NEW.policy_version AND (
    (case_row.maker_account_id=NEW.actor_account_id AND EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.account_id=NEW.actor_account_id AND s.realm_id=NEW.realm_id AND s.organization_id=NEW.organization_id AND s.scope_type='ORGANIZATION' AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['org.read','org.lifecycle.propose']))
    OR EXISTS(SELECT 1 FROM merchant.organization_case_review r JOIN merchant.business_reviewer_scope s ON s.account_id=r.reviewer_account_id AND s.realm_id=r.reviewer_realm_id AND s.organization_id=r.organization_id WHERE r.case_id=case_row.id AND r.case_revision=case_row.revision AND r.reviewer_account_id=NEW.actor_account_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.scope_type='ORGANIZATION' AND s.permissions @> ARRAY['org.read','org.lifecycle.review'])
   )) THEN RAISE EXCEPTION 'ORG_CURRENT_DISCLOSURE_PARTY_REQUIRED' USING ERRCODE='42501';END IF;
  END IF;
  RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='organization_governance_receipt' THEN
  IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'ORG_RECEIPT_IMMUTABLE' USING ERRCODE='23514';END IF;
  SELECT * INTO e FROM merchant.organization_governance_event WHERE id=NEW.event_id;
  IF NOT FOUND OR e.operation<>NEW.operation OR e.actor_kind<>NEW.actor_kind OR e.realm_id IS DISTINCT FROM NEW.realm_id OR e.organization_id IS DISTINCT FROM NEW.organization_id OR e.case_id IS DISTINCT FROM NEW.case_id OR e.policy_id IS DISTINCT FROM NEW.policy_id OR e.prior_version<>NEW.original_version OR e.new_version<>NEW.result_version OR (NEW.actor_kind='SYSTEM' AND e.actor_account_id::TEXT<>NEW.actor_binding) OR (NEW.actor_kind='OPERATOR' AND e.operator_principal<>NEW.actor_binding) THEN RAISE EXCEPTION 'ORG_RECEIPT_BINDING' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='organization_governance_case' THEN
  SELECT * INTO e FROM merchant.organization_governance_event WHERE id=NEW.last_event_id;
  IF NOT FOUND OR e.case_id IS DISTINCT FROM NEW.id OR e.organization_id IS DISTINCT FROM NEW.organization_id OR e.policy_id IS DISTINCT FROM NEW.policy_id OR e.policy_version<>NEW.policy_version OR e.new_version<>NEW.version OR e.revision<>NEW.revision OR e.protected_payload IS DISTINCT FROM NEW.protected_payload THEN RAISE EXCEPTION 'ORG_CASE_EVENT_BINDING' USING ERRCODE='23514';END IF;
  IF NEW.maker_kind<>'SYSTEM' OR NEW.type IN ('OWNER_TRANSFER','DISPUTE','REVERIFY','RELEASE_RESTRICTION','RELEASE_HOLD') OR NEW.state IN ('APPROVED','REJECTED','EXECUTED') THEN RAISE EXCEPTION 'ORG_CASE_STEP_A_BOUNDARY' USING ERRCODE='42501';END IF;
  IF TG_OP='INSERT' THEN
   IF NEW.version<>1 OR NEW.revision<>1 OR NEW.state<>'DRAFT' OR e.operation<>'CREATE_CASE' OR e.prior_version<>0 OR NEW.maker_account_id<>e.actor_account_id OR NEW.maker_realm_id<>e.realm_id THEN RAISE EXCEPTION 'ORG_CASE_CREATE_BINDING' USING ERRCODE='23514';END IF;
  ELSE
   IF NEW.version<>OLD.version+1 OR NEW.organization_id<>OLD.organization_id OR NEW.type<>OLD.type OR NEW.policy_id<>OLD.policy_id OR NEW.policy_version<>OLD.policy_version OR NEW.maker_kind<>OLD.maker_kind OR NEW.maker_account_id<>OLD.maker_account_id OR NEW.maker_realm_id IS DISTINCT FROM OLD.maker_realm_id OR NEW.created_at<>OLD.created_at OR NEW.expires_at<>OLD.expires_at OR NEW.expected_org_version<>OLD.expected_org_version OR NEW.expected_ownership_version<>OLD.expected_ownership_version OR NEW.effective_at IS DISTINCT FROM OLD.effective_at OR e.prior_version<>OLD.version THEN RAISE EXCEPTION 'ORG_CASE_IDENTITY_IMMUTABLE' USING ERRCODE='23514';END IF;
   IF (e.operation='SUBMIT_CASE' AND OLD.state='DRAFT' AND NEW.state='SUBMITTED')
    OR (e.operation='ASSIGN_CASE_REVIEWER' AND OLD.state IN ('SUBMITTED','UNDER_REVIEW') AND NEW.state=OLD.state)
    OR (e.operation='BEGIN_CASE_REVIEW' AND OLD.state IN ('SUBMITTED','UNDER_REVIEW') AND NEW.state='UNDER_REVIEW')
    OR (e.operation='REQUEST_CASE_INFORMATION' AND OLD.state IN ('UNDER_REVIEW','READY_FOR_DECISION') AND NEW.state='NEEDS_INFORMATION')
    OR (e.operation='RESUBMIT_CASE' AND OLD.state='NEEDS_INFORMATION' AND NEW.state='SUBMITTED')
    OR (e.operation='READY_CASE_DECISION' AND OLD.state='UNDER_REVIEW' AND NEW.state='READY_FOR_DECISION')
    OR (e.operation='CANCEL_CASE' AND OLD.state IN ('DRAFT','SUBMITTED','NEEDS_INFORMATION') AND NEW.state='CANCELLED')
    OR (e.operation='EXPIRE_CASE' AND OLD.state NOT IN ('EXECUTED','REJECTED','CANCELLED','EXPIRED') AND NEW.state='EXPIRED' AND OLD.expires_at<=clock_timestamp()) THEN NULL;
   ELSE RAISE EXCEPTION 'ORG_CASE_TRANSITION_DENIED' USING ERRCODE='23514';END IF;
   IF NEW.revision<>OLD.revision+(CASE WHEN e.operation='RESUBMIT_CASE' THEN 1 ELSE 0 END) THEN RAISE EXCEPTION 'ORG_CASE_REVISION_BINDING' USING ERRCODE='23514';END IF;
   IF e.operation IN ('SUBMIT_CASE','RESUBMIT_CASE','CANCEL_CASE') AND e.actor_account_id<>OLD.maker_account_id THEN RAISE EXCEPTION 'ORG_CASE_MAKER_REQUIRED' USING ERRCODE='42501';END IF;
  END IF;
  IF NOT EXISTS(SELECT 1 FROM merchant.organization o JOIN merchant.organization_current_owner x ON x.organization_id=o.id JOIN merchant.membership m ON m.id=x.membership_id AND m.organization_id=o.id WHERE o.id=NEW.organization_id AND o.environment IN ('LOCAL','TEST') AND o.version=NEW.expected_org_version AND x.version=NEW.expected_ownership_version AND m.role='OWNER' AND m.state='ACTIVE') THEN RAISE EXCEPTION 'ORG_CASE_CURRENT_STATE_REQUIRED' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF TG_TABLE_NAME='organization_case_review' THEN
  SELECT * INTO c FROM merchant.organization_governance_case WHERE id=NEW.case_id AND organization_id=NEW.organization_id;
  IF NOT FOUND OR NEW.reviewer_account_id=c.maker_account_id OR NOT identity.local_admin_eligible(NEW.reviewer_account_id,NEW.reviewer_realm_id) OR EXISTS(SELECT 1 FROM merchant.membership WHERE organization_id=NEW.organization_id AND account_id=NEW.reviewer_account_id AND state<>'REVOKED')
   OR NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s WHERE s.organization_id=NEW.organization_id AND s.account_id=NEW.reviewer_account_id AND s.realm_id=NEW.reviewer_realm_id AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['org.read','org.lifecycle.review']) THEN RAISE EXCEPTION 'ORG_CASE_REVIEWER_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
  IF TG_OP='UPDATE' AND (NEW.case_id<>OLD.case_id OR NEW.organization_id<>OLD.organization_id OR NEW.version<>OLD.version+1) THEN RAISE EXCEPTION 'ORG_CASE_REVIEW_IDENTITY' USING ERRCODE='23514';END IF;
  IF NEW.case_revision<>c.revision THEN RAISE EXCEPTION 'ORG_CASE_REVIEW_REVISION' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 RAISE EXCEPTION 'ORG_GOVERNANCE_TABLE_DENIED' USING ERRCODE='42501';
END $$;

CREATE FUNCTION merchant.require_organization_governance_commit() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e merchant.organization_governance_event;c merchant.organization_governance_case;r merchant.organization_governance_receipt;
BEGIN
 IF TG_TABLE_NAME='organization_governance_policy' THEN
  SELECT * INTO e FROM merchant.organization_governance_event WHERE id=NEW.published_event_id;
  IF e.id IS NULL OR e.policy_id IS DISTINCT FROM NEW.id OR e.operation<>'PUBLISH_ORG_POLICY' OR e.new_version<>NEW.version THEN RAISE EXCEPTION 'ORG_POLICY_PUBLICATION_BINDING' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='organization_governance_case' THEN
  SELECT * INTO e FROM merchant.organization_governance_event WHERE id=NEW.last_event_id;
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_governance_event WHERE id=e.id AND xmin::TEXT=pg_current_xact_id()::TEXT) THEN RAISE EXCEPTION 'ORG_CASE_FRESH_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='organization_case_review' THEN
  SELECT * INTO c FROM merchant.organization_governance_case WHERE id=NEW.case_id;
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_governance_case WHERE id=c.id AND xmin::TEXT=pg_current_xact_id()::TEXT) THEN RAISE EXCEPTION 'ORG_REVIEW_CASE_TRANSACTION_REQUIRED' USING ERRCODE='23514';END IF;
  SELECT * INTO e FROM merchant.organization_governance_event WHERE id=c.last_event_id;
  IF e.operation NOT IN ('ASSIGN_CASE_REVIEWER','BEGIN_CASE_REVIEW','REQUEST_CASE_INFORMATION','RESUBMIT_CASE','READY_CASE_DECISION') THEN RAISE EXCEPTION 'ORG_REVIEW_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
  IF e.operation IN ('BEGIN_CASE_REVIEW','REQUEST_CASE_INFORMATION','READY_CASE_DECISION') AND e.actor_account_id<>NEW.reviewer_account_id THEN RAISE EXCEPTION 'ORG_ASSIGNED_REVIEWER_REQUIRED' USING ERRCODE='42501';END IF;
  IF e.operation='READY_CASE_DECISION' AND (NEW.ready_event_id IS DISTINCT FROM e.id OR NEW.checklist IS DISTINCT FROM '{"authority":true,"conflictClear":true,"evidenceComplete":true,"impactReviewed":true}'::JSONB) THEN RAISE EXCEPTION 'ORG_REVIEW_CHECKLIST_REQUIRED' USING ERRCODE='23514';END IF;
  IF e.operation IN ('REQUEST_CASE_INFORMATION','READY_CASE_DECISION') AND NOT EXISTS(SELECT 1 FROM merchant.organization_governance_event b JOIN merchant.organization_governance_event a ON a.id=NEW.assigned_event_id WHERE b.case_id=NEW.case_id AND b.actor_account_id=NEW.reviewer_account_id AND b.operation='BEGIN_CASE_REVIEW' AND b.revision=NEW.case_revision AND b.new_version>a.new_version AND b.new_version<e.new_version) THEN RAISE EXCEPTION 'ORG_CURRENT_REVIEW_BEGIN_REQUIRED' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='organization_governance_event' THEN e:=NEW;
 ELSE RAISE EXCEPTION 'ORG_COMMIT_TARGET_DENIED';END IF;
 IF e.id IS NULL THEN RAISE EXCEPTION 'ORG_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
 IF e.operation<>'READ_CASE_DISCLOSURE' THEN
  SELECT * INTO r FROM merchant.organization_governance_receipt WHERE event_id=e.id;
  IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM merchant.organization_governance_receipt WHERE event_id=e.id AND xmin::TEXT=pg_current_xact_id()::TEXT) THEN RAISE EXCEPTION 'ORG_ATOMIC_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;
 IF e.actor_kind='SYSTEM' THEN
  SELECT * INTO c FROM merchant.organization_governance_case WHERE id=e.case_id;
  IF c.last_event_id IS DISTINCT FROM e.id AND e.operation<>'READ_CASE_DISCLOSURE' THEN RAISE EXCEPTION 'ORG_ATOMIC_CASE_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
  IF e.operation IN ('ASSIGN_CASE_REVIEWER','BEGIN_CASE_REVIEW','REQUEST_CASE_INFORMATION','READY_CASE_DECISION') AND NOT EXISTS(SELECT 1 FROM merchant.organization_case_review WHERE case_id=c.id AND case_revision=c.revision) THEN RAISE EXCEPTION 'ORG_ATOMIC_REVIEW_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;RETURN NULL;
END $$;

DO $$ DECLARE t TEXT;BEGIN
 FOREACH t IN ARRAY ARRAY['organization_governance_policy','organization_governance_case','organization_governance_event','organization_governance_receipt','organization_case_review','organization_current_owner'] LOOP
  EXECUTE format('CREATE TRIGGER organization_governance_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.%I FOR EACH ROW EXECUTE FUNCTION merchant.guard_organization_governance_rows()',t);
  IF t NOT IN ('organization_governance_receipt','organization_current_owner') THEN EXECUTE format('CREATE CONSTRAINT TRIGGER organization_governance_commit AFTER INSERT OR UPDATE ON merchant.%I DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.require_organization_governance_commit()',t);END IF;
 END LOOP;
END $$;
CREATE CONSTRAINT TRIGGER organization_owner_bridge AFTER INSERT OR UPDATE ON merchant.organization DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.guard_organization_governance_rows();

CREATE FUNCTION merchant.publish_local_organization_governance_policy(p_request UUID,p_policy UUID,p_version BIGINT,p_body JSONB,p_reason TEXT) RETURNS UUID LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE stamp TIMESTAMPTZ:=clock_timestamp();e UUID:=gen_random_uuid();d BYTEA;prior merchant.organization_governance_receipt;expected JSONB:='{"schema_version":"ORG_GOVERNANCE_1","environment":"TEST","allowed_case_types":["RESTRICT","RELEASE_RESTRICTION","CLOSE","RISK_HOLD","RELEASE_HOLD","DISPUTE","REVERIFY"],"capability_policy":{"RESTRICTED":{"readSupportAllowlist":["HISTORY","TRACKING","SUPPORT","EXISTING_SHIPMENT_HANDLING","EXISTING_FINANCIAL_OBLIGATIONS"],"deniedBusinessOperations":["QUOTE_NEW","BOOKING_CONFIRM","TEAM_DELEGATION","CONTRACT_CHANGE"]},"CLOSED":{"retainedReadSupportAllowlist":["HISTORY","SUPPORT","EXISTING_FINANCIAL_OBLIGATIONS"],"terminal":true},"hold_effects":{"BOOKING_HOLD":["QUOTE_NEW","BOOKING_CONFIRM"],"CONTRACT_CHANGE_HOLD":["CONTRACT_CHANGE"]}},"separation":{"maker_reviewer_distinct":true,"reviewer_decider_distinct":true,"maker_decider_distinct":true,"executor_may_equal_decider":true},"case_ttl_seconds":604800,"consent_ttl_seconds":86400,"max_future_effect_seconds":0,"required_checklist_codes":["authority","conflictClear","evidenceComplete","impactReviewed"],"close_requires":["ORDERS_CLEAR","FINANCE_CLEAR"],"transfer_eligibility":{"organizationState":"ACTIVE","admissionOrigin":"APPLICATION_APPROVED","sameOrganization":true,"verifiedParties":true,"activeNonOwnerRecipient":true,"openOwnershipDispute":false,"blockingMaterialReverification":false,"ordinaryUnpaidBlocks":false},"post_transfer_grants":{"outgoingRole":"OPERATOR","outgoingState":"ACTIVE","outgoingPermissions":["dashboard.read"],"incomingPermissions":["dashboard.read","ownership.transfer.request"]},"retention_policy_reference":null}'::JSONB;
BEGIN
 IF NOT pg_has_role(session_user,'merchant_governance_operator','MEMBER') OR session_user IN ('merchant_governance_operator','merchant_governance_writer') OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_policy IS NULL OR p_policy='00000000-0000-0000-0000-000000000000' OR p_version IS DISTINCT FROM 1 OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 THEN RAISE EXCEPTION 'ORG_POLICY_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(('x4d45524348414e54'::BIT(64))::BIGINT);
 IF p_body IS NULL OR jsonb_typeof(p_body)<>'object' OR p_body->>'environment' NOT IN ('LOCAL','TEST') OR (p_body-'environment') IS DISTINCT FROM (expected-'environment') OR NOT coalesce(identity.local_admin_environment_matches(p_body->>'environment'),FALSE) THEN RAISE EXCEPTION 'ORG_POLICY_V1_EXACT_APPROVAL_REQUIRED' USING ERRCODE='22023';END IF;
 d:=sha256(convert_to(jsonb_build_object('format','ARGO_OPERATOR_RECEIPT_1','operator',session_user,'request',p_request,'policy',p_policy,'version',p_version,'body',p_body,'reason',p_reason)::TEXT,'UTF8'));
 SELECT * INTO prior FROM merchant.organization_governance_receipt WHERE actor_kind='OPERATOR' AND actor_binding=session_user::TEXT AND operation='PUBLISH_ORG_POLICY' AND operation_key=p_request;
 IF FOUND THEN IF prior.policy_id IS DISTINCT FROM p_policy OR prior.input_digest<>d THEN RAISE EXCEPTION 'ORG_POLICY_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;RETURN prior.policy_id;END IF;
 INSERT INTO merchant.organization_governance_policy VALUES(p_policy,p_body->>'schema_version',p_version,p_body->>'environment','PUBLISHED',p_body,sha256(convert_to(p_body::TEXT,'UTF8')),stamp,NULL,e);
 INSERT INTO merchant.organization_governance_event VALUES(e,NULL,NULL,'OPERATOR',NULL,NULL,session_user::TEXT,'PUBLISH_ORG_POLICY',0,0,1,p_policy,p_version,'ORG_POLICY_PUBLISHED',NULL,p_request,stamp);
 INSERT INTO merchant.organization_governance_receipt VALUES('OPERATOR',session_user::TEXT,NULL,'PUBLISH_ORG_POLICY',p_request,NULL,NULL,0,1,'SQL_SHA256_1',d,e,d,stamp,stamp+INTERVAL '7 days',p_policy);
 RETURN p_policy;
END $$;

REVOKE ALL ON merchant.organization_governance_policy,merchant.organization_governance_case,merchant.organization_governance_event,merchant.organization_governance_receipt,merchant.organization_case_review,merchant.organization_current_owner FROM PUBLIC;
GRANT SELECT ON merchant.organization_governance_policy,merchant.organization_governance_case,merchant.organization_governance_event,merchant.organization_governance_receipt,merchant.organization_case_review,merchant.organization_current_owner TO "${runtimeRole}";
GRANT INSERT ON merchant.organization_governance_case,merchant.organization_governance_event,merchant.organization_governance_receipt,merchant.organization_case_review TO "${runtimeRole}";
GRANT UPDATE(state,version,revision,protected_payload,impact_digest,updated_at,last_event_id) ON merchant.organization_governance_case TO "${runtimeRole}";
GRANT UPDATE(reviewer_account_id,reviewer_realm_id,authority_version,case_revision,version,checklist,ready_event_id,assigned_event_id) ON merchant.organization_case_review TO "${runtimeRole}";
GRANT SELECT ON merchant.organization_governance_policy,merchant.organization_governance_case,merchant.organization_governance_event,merchant.organization_governance_receipt,merchant.organization_case_review,merchant.organization_current_owner,merchant.organization,merchant.membership,merchant.business_reviewer_scope TO merchant_governance_writer;
GRANT INSERT ON merchant.organization_governance_policy,merchant.organization_governance_event,merchant.organization_governance_receipt,merchant.organization_current_owner TO merchant_governance_writer;
GRANT UPDATE(state) ON merchant.organization_governance_policy TO merchant_governance_writer;
REVOKE ALL ON FUNCTION merchant.guard_organization_governance_rows(),merchant.require_organization_governance_commit(),merchant.publish_local_organization_governance_policy(UUID,UUID,BIGINT,JSONB,TEXT) FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION merchant.publish_local_organization_governance_policy(UUID,UUID,BIGINT,JSONB,TEXT) TO merchant_governance_operator;
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
ALTER FUNCTION merchant.guard_organization_governance_rows() OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.require_organization_governance_commit() OWNER TO merchant_governance_writer;
ALTER FUNCTION merchant.publish_local_organization_governance_policy(UUID,UUID,BIGINT,JSONB,TEXT) OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;

