-- Owner-approved M2-R01 subset only. No notice/timing/lifecycle/ownership grants.
DO $$ DECLARE c RECORD;BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='merchant.business_reviewer_scope'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%permissions%' LOOP
  EXECUTE format('ALTER TABLE merchant.business_reviewer_scope DROP CONSTRAINT %I',c.conname);
 END LOOP;
END $$;
ALTER TABLE merchant.business_reviewer_scope ADD CONSTRAINT m2r_permissions CHECK(
 cardinality(permissions) BETWEEN 1 AND 13 AND array_position(permissions,NULL) IS NULL
 AND permissions <@ ARRAY['application.read','application.evidence.read','application.assign','application.review','application.decide','org.read','org.suspend','org.reinstate','audit.read','task.read','application.comment.write','org.profile.read','org.members.read']::TEXT[]);
ALTER TABLE merchant.business_reviewer_scope ADD CONSTRAINT m2r_scope CHECK(
 (scope_type='POLICY' AND policy_id IS NOT NULL AND organization_id IS NULL AND NOT permissions && ARRAY['org.profile.read','org.members.read']::TEXT[])
 OR (scope_type='ORGANIZATION' AND policy_id IS NULL AND organization_id IS NOT NULL AND permissions <@ ARRAY['org.read','org.suspend','org.reinstate','audit.read','org.profile.read','org.members.read']::TEXT[]));

-- Keep the original signature, owner, SECURITY DEFINER search_path, receipt and ACL.
-- Amend only the exact permission allowlists in the existing canonical operator.
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
SET LOCAL ROLE merchant_governance_writer;
DO $$ DECLARE definition TEXT;BEGIN
 SELECT pg_get_functiondef('merchant.configure_local_reviewer_scope(uuid,uuid,uuid,text,uuid,text[],text,bigint,timestamptz,text)'::regprocedure) INTO definition;
 IF position('cardinality(p_permissions) NOT BETWEEN 1 AND 10' IN definition)=0
  OR position('''audit.read'',''task.read'']::TEXT[]' IN definition)=0 THEN
  RAISE EXCEPTION 'M2R_OPERATOR_SOURCE_DRIFT';
 END IF;
 definition:=replace(definition,'cardinality(p_permissions) NOT BETWEEN 1 AND 10','cardinality(p_permissions) NOT BETWEEN 1 AND 13');
 definition:=replace(definition,'''audit.read'',''task.read'']::TEXT[]','''audit.read'',''task.read'',''application.comment.write'',''org.profile.read'',''org.members.read'']::TEXT[]');
 definition:=replace(definition,'ARRAY[''org.read'',''org.suspend'',''org.reinstate'',''audit.read'']::TEXT[]','ARRAY[''org.read'',''org.suspend'',''org.reinstate'',''audit.read'',''org.profile.read'',''org.members.read'']::TEXT[]');
 definition:=replace(definition,'OR p_expires IS NULL THEN','OR (p_type=''POLICY'' AND p_permissions && ARRAY[''org.profile.read'',''org.members.read'']::TEXT[]) OR p_expires IS NULL THEN');
 EXECUTE definition;
END $$;
RESET ROLE;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;

CREATE TABLE merchant.application_review_comment (
 id UUID PRIMARY KEY,
 application_id UUID NOT NULL,
 revision BIGINT NOT NULL CHECK(revision>0),
 actor_id UUID NOT NULL,
 realm_id UUID NOT NULL,
 protected_text BYTEA NOT NULL CHECK(octet_length(protected_text) BETWEEN 32 AND 33000),
 event_id UUID NOT NULL UNIQUE REFERENCES merchant.governance_event(id),
 request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 operation_key UUID NOT NULL CHECK(operation_key<>'00000000-0000-0000-0000-000000000000'),
 input_key_id VARCHAR(64) NOT NULL CHECK(input_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 FOREIGN KEY(application_id,revision) REFERENCES merchant.application_revision(application_id,revision),
 UNIQUE(actor_id,application_id,operation_key)
);
CREATE INDEX review_comments_application ON merchant.application_review_comment(application_id,created_at,id);
CREATE FUNCTION merchant.guard_review_comment() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e merchant.governance_event; a merchant.application;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'REVIEW_COMMENT_IMMUTABLE' USING ERRCODE='23514';END IF;
 SELECT * INTO e FROM merchant.governance_event WHERE id=NEW.event_id;
 IF NOT FOUND OR e.actor_kind<>'SYSTEM' OR e.operation<>'READ_DISCLOSURE' OR e.target_type<>'APPLICATION'
  OR e.application_id IS DISTINCT FROM NEW.application_id OR e.target_id<>NEW.application_id OR e.revision IS DISTINCT FROM NEW.revision
  OR e.actor_id<>NEW.actor_id OR e.realm_id IS DISTINCT FROM NEW.realm_id OR e.request_id<>NEW.request_id
  OR e.occurred_at<>NEW.created_at OR e.protected_payload IS DISTINCT FROM NEW.protected_text OR e.prior_version<>e.new_version
  OR NOT EXISTS(SELECT 1 FROM merchant.governance_event WHERE id=NEW.event_id AND xmin::TEXT=pg_current_xact_id()::TEXT)
 THEN RAISE EXCEPTION 'REVIEW_COMMENT_EVENT_BINDING' USING ERRCODE='23514';END IF;
 SELECT * INTO a FROM merchant.application WHERE id=NEW.application_id;
 IF NOT FOUND OR a.submitted_revision IS DISTINCT FROM NEW.revision OR a.version<>e.new_version OR a.policy_id IS DISTINCT FROM e.policy_id
  OR NOT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope s JOIN merchant.application_policy p ON p.id=s.policy_id
   WHERE s.account_id=NEW.actor_id AND s.realm_id=NEW.realm_id AND s.scope_type='POLICY' AND s.policy_id=a.policy_id
    AND s.state='ACTIVE' AND s.expires_at>clock_timestamp() AND s.permissions @> ARRAY['application.read','application.comment.write']::TEXT[]
    AND p.state='PUBLISHED' AND p.valid_from<=clock_timestamp() AND (p.valid_to IS NULL OR p.valid_to>clock_timestamp()))
 THEN RAISE EXCEPTION 'REVIEW_COMMENT_CURRENT_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER guard_review_comment BEFORE INSERT OR UPDATE OR DELETE ON merchant.application_review_comment FOR EACH ROW EXECUTE FUNCTION merchant.guard_review_comment();
REVOKE ALL ON TABLE merchant.application_review_comment FROM PUBLIC;
GRANT SELECT,INSERT ON merchant.application_review_comment TO "${runtimeRole}";
GRANT SELECT ON merchant.application_review_comment,merchant.application TO merchant_governance_writer;
REVOKE ALL ON FUNCTION merchant.guard_review_comment() FROM PUBLIC,"${runtimeRole}";
GRANT CREATE ON SCHEMA merchant TO merchant_governance_writer;
ALTER FUNCTION merchant.guard_review_comment() OWNER TO merchant_governance_writer;
REVOKE CREATE ON SCHEMA merchant FROM merchant_governance_writer;
