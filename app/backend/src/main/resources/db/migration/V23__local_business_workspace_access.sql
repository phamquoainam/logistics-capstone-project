-- Owner accepted BUSINESS-LOCAL-ACCESS-01; development access is not application approval.
CREATE TABLE merchant.organization (
 id UUID PRIMARY KEY, display_name VARCHAR(160) NOT NULL CHECK(length(btrim(display_name))>0 AND display_name !~ '[[:cntrl:]]'),
 state VARCHAR(12) NOT NULL CHECK(state IN ('ACTIVE','SUSPENDED')),
 admission_origin VARCHAR(32) NOT NULL CHECK(admission_origin='LOCAL_OPERATOR_PROVISIONED'),
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 initial_owner_membership_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at)
);
CREATE TABLE merchant.membership (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), account_id UUID NOT NULL,
 role VARCHAR(8) NOT NULL CHECK(role IN ('OWNER','MANAGER','OPERATOR')), state VARCHAR(7) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')),
 authority_version BIGINT NOT NULL CHECK(authority_version>0), version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 UNIQUE(organization_id,account_id), UNIQUE(id,organization_id)
);
ALTER TABLE merchant.organization ADD CONSTRAINT workspace_initial_owner_fk
 FOREIGN KEY(initial_owner_membership_id,id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE merchant.member_grant (
 id UUID PRIMARY KEY, membership_id UUID NOT NULL, organization_id UUID NOT NULL,
 permission VARCHAR(24) NOT NULL CHECK(permission IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read')),
 scope VARCHAR(12) NOT NULL CHECK(scope='ORGANIZATION'), UNIQUE(membership_id,permission),
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id)
);
CREATE TABLE merchant.workspace_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), membership_id UUID,
 operator_principal TEXT NOT NULL, operation VARCHAR(12) NOT NULL CHECK(operation IN ('PROVISION','GRANT','REVOKE','SUSPEND')),
 request_id UUID NOT NULL UNIQUE, reason VARCHAR(300) NOT NULL CHECK(length(btrim(reason))>0 AND reason !~ '[[:cntrl:]]'), occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id)
);
CREATE TABLE merchant.workspace_command_receipt (
 request_id UUID PRIMARY KEY, input_digest VARCHAR(64) NOT NULL CHECK(input_digest ~ '^[a-f0-9]{64}$'),
 organization_id UUID NOT NULL REFERENCES merchant.organization(id), membership_id UUID,
 operation VARCHAR(12) NOT NULL CHECK(operation IN ('PROVISION','GRANT','REVOKE','SUSPEND')), recorded_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id)
);
CREATE INDEX workspace_account_context ON merchant.membership(account_id,organization_id) WHERE state='ACTIVE';

-- Identity-owned offline port: no Merchant cross-schema Identity reads/writes.
CREATE FUNCTION identity.lock_local_business_subject(target UUID, require_eligible BOOLEAN)
RETURNS VOID LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,identity AS $$
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='identity'))
 OR COALESCE(current_setting('argopost.operator_environment',TRUE),'') NOT IN ('LOCAL','TEST')
 OR target IS NULL OR require_eligible IS NULL THEN RAISE EXCEPTION 'LOCAL_BUSINESS_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 PERFORM 1 FROM identity.user_account WHERE id=target FOR UPDATE;
 IF NOT FOUND OR (require_eligible AND NOT EXISTS(
   SELECT 1 FROM identity.user_account a JOIN identity.customer_identifier c ON c.account_id=a.id
   WHERE a.id=target AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL
   AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE "C"
 )) THEN RAISE EXCEPTION 'EXISTING_VERIFIED_CUSTOMER_REQUIRED' USING ERRCODE='23514'; END IF;
END $$;
CREATE FUNCTION merchant.guard_workspace_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN
   IF TG_TABLE_NAME<>'member_grant' THEN RAISE EXCEPTION 'WORKSPACE_DELETE_DENIED' USING ERRCODE='23514'; END IF; RETURN OLD;
 END IF;
 IF TG_OP='UPDATE' THEN
   IF TG_TABLE_NAME='member_grant' THEN RAISE EXCEPTION 'WORKSPACE_GRANT_IMMUTABLE' USING ERRCODE='23514'; END IF;
   IF NEW.id<>OLD.id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at THEN RAISE EXCEPTION 'WORKSPACE_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
   IF TG_TABLE_NAME='organization' THEN
     IF NEW.admission_origin<>OLD.admission_origin OR NEW.environment<>OLD.environment OR NEW.initial_owner_membership_id<>OLD.initial_owner_membership_id THEN RAISE EXCEPTION 'WORKSPACE_ORIGIN_IMMUTABLE' USING ERRCODE='23514'; END IF;
   ELSE
     IF NEW.organization_id<>OLD.organization_id OR NEW.account_id<>OLD.account_id OR NEW.role<>OLD.role OR OLD.state='REVOKED'
     OR NEW.authority_version<>OLD.authority_version+1 THEN RAISE EXCEPTION 'WORKSPACE_MEMBERSHIP_IMMUTABLE' USING ERRCODE='23514'; END IF;
   END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER workspace_org_identity BEFORE INSERT OR UPDATE OR DELETE ON merchant.organization FOR EACH ROW EXECUTE FUNCTION merchant.guard_workspace_identity();
CREATE TRIGGER workspace_member_identity BEFORE INSERT OR UPDATE OR DELETE ON merchant.membership FOR EACH ROW EXECUTE FUNCTION merchant.guard_workspace_identity();
CREATE TRIGGER workspace_grant_identity BEFORE INSERT OR UPDATE OR DELETE ON merchant.member_grant FOR EACH ROW EXECUTE FUNCTION merchant.guard_workspace_identity();
CREATE FUNCTION merchant.check_workspace_invariants() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE target UUID;
BEGIN
 IF TG_TABLE_NAME='organization' THEN target:=NEW.id;
 ELSIF TG_OP='DELETE' THEN target:=OLD.organization_id;
 ELSE target:=NEW.organization_id; END IF;
 IF EXISTS(SELECT 1 FROM merchant.organization o WHERE o.id=target AND o.state='ACTIVE' AND NOT EXISTS(
   SELECT 1 FROM merchant.membership m WHERE m.id=o.initial_owner_membership_id AND m.organization_id=o.id AND m.state='ACTIVE' AND m.role='OWNER'
 )) THEN RAISE EXCEPTION 'WORKSPACE_ACTIVE_INITIAL_OWNER_REQUIRED' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=target AND g.permission<>'dashboard.read' AND NOT EXISTS(
   SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='dashboard.read'
 )) THEN RAISE EXCEPTION 'WORKSPACE_BASE_PERMISSION_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER workspace_org_invariants AFTER INSERT OR UPDATE ON merchant.organization DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_workspace_invariants();
CREATE CONSTRAINT TRIGGER workspace_member_invariants AFTER INSERT OR UPDATE ON merchant.membership DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_workspace_invariants();
CREATE CONSTRAINT TRIGGER workspace_grant_invariants AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_workspace_invariants();
CREATE FUNCTION merchant.workspace_append_only() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'WORKSPACE_EVIDENCE_IMMUTABLE' USING ERRCODE='23514'; END $$;
CREATE TRIGGER workspace_audit_append BEFORE UPDATE OR DELETE ON merchant.workspace_audit FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER workspace_receipt_append BEFORE UPDATE OR DELETE ON merchant.workspace_command_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();

CREATE FUNCTION merchant.configure_local_workspace(
 p_request UUID,p_action TEXT,p_org UUID,p_name TEXT,p_account UUID,p_role TEXT,p_permissions TEXT[],p_reason TEXT
) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,merchant AS $$
DECLARE stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP; member UUID; owner_account UUID; subject UUID;
 prior merchant.workspace_command_receipt%ROWTYPE; org merchant.organization%ROWTYPE;
 binding merchant.membership%ROWTYPE; digest TEXT; permissions TEXT[]; mode TEXT;
BEGIN
 mode:=current_setting('argopost.operator_environment',TRUE);
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) OR COALESCE(mode,'') NOT IN ('LOCAL','TEST') THEN RAISE EXCEPTION 'LOCAL_BUSINESS_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 IF p_request IS NULL OR p_org IS NULL OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('PROVISION','GRANT','REVOKE','SUSPEND')
 OR p_role IS NULL OR p_role NOT IN ('OWNER','MANAGER','OPERATOR') OR p_permissions IS NULL
 OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]'
 OR cardinality(p_permissions)>3 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read'))
 OR p_org='00000000-0000-0000-0000-000000000000' OR p_account='00000000-0000-0000-0000-000000000000'
 OR p_request='00000000-0000-0000-0000-000000000000' THEN RAISE EXCEPTION 'WORKSPACE_COMMAND_INVALID' USING ERRCODE='23514'; END IF;
 permissions:=ARRAY(SELECT DISTINCT p FROM unnest(p_permissions) p ORDER BY p);
 IF p_action IN ('PROVISION','GRANT') AND (NOT 'dashboard.read'=ANY(permissions) OR cardinality(permissions)=0) THEN RAISE EXCEPTION 'WORKSPACE_BASE_PERMISSION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF p_action='PROVISION' AND (p_role<>'OWNER' OR p_name IS NULL OR length(btrim(p_name)) NOT BETWEEN 1 AND 160 OR p_name ~ '[[:cntrl:]]') THEN RAISE EXCEPTION 'WORKSPACE_INITIAL_OWNER_REQUIRED' USING ERRCODE='23514'; END IF;
 -- Immutable owner binding is discovered before sorted Identity locks; all writers
 -- take Identity administration, subject locks, then Merchant control in that order.
 SELECT m.account_id INTO owner_account FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id WHERE o.id=p_org;
 FOR subject IN SELECT DISTINCT v FROM unnest(ARRAY[p_account,owner_account]) v WHERE v IS NOT NULL ORDER BY v LOOP
   PERFORM identity.lock_local_business_subject(subject,p_action IN ('PROVISION','GRANT'));
 END LOOP;
 PERFORM pg_advisory_xact_lock(730023,1);
 digest:=encode(sha256(convert_to(jsonb_build_array(mode,p_action,p_org,p_name,p_account,p_role,permissions,p_reason)::TEXT,'UTF8')),'hex');
 SELECT * INTO prior FROM merchant.workspace_command_receipt WHERE request_id=p_request;
 IF FOUND THEN
   IF prior.input_digest<>digest THEN RAISE EXCEPTION 'WORKSPACE_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514'; END IF; RETURN prior.membership_id;
 END IF;
 SELECT * INTO org FROM merchant.organization WHERE id=p_org FOR UPDATE;
 IF p_action='PROVISION' THEN
   IF FOUND THEN RAISE EXCEPTION 'WORKSPACE_ALREADY_EXISTS' USING ERRCODE='23514'; END IF;
   member:=gen_random_uuid();
   INSERT INTO merchant.organization VALUES(p_org,btrim(p_name),'ACTIVE','LOCAL_OPERATOR_PROVISIONED',mode,member,1,stamp,stamp);
   INSERT INTO merchant.membership VALUES(member,p_org,p_account,'OWNER','ACTIVE',1,1,stamp,stamp);
 ELSE
   IF NOT FOUND OR org.environment<>mode THEN RAISE EXCEPTION 'WORKSPACE_CONTEXT_UNAVAILABLE' USING ERRCODE='23514'; END IF;
   SELECT * INTO binding FROM merchant.membership WHERE organization_id=p_org AND account_id=p_account FOR UPDATE;
   IF p_action='SUSPEND' THEN
     IF org.state<>'ACTIVE' THEN RAISE EXCEPTION 'WORKSPACE_STATE_CONFLICT' USING ERRCODE='23514'; END IF;
     UPDATE merchant.organization SET state='SUSPENDED',version=version+1,updated_at=stamp WHERE id=p_org; member:=binding.id;
   ELSIF p_action='GRANT' THEN
     IF org.state<>'ACTIVE' THEN RAISE EXCEPTION 'WORKSPACE_CONTEXT_UNAVAILABLE' USING ERRCODE='23514'; END IF;
     IF binding.id IS NULL THEN
       IF p_role='OWNER' THEN RAISE EXCEPTION 'WORKSPACE_INITIAL_OWNER_ONLY' USING ERRCODE='23514'; END IF;
       member:=gen_random_uuid(); INSERT INTO merchant.membership VALUES(member,p_org,p_account,p_role,'ACTIVE',1,1,stamp,stamp);
     ELSE
       IF binding.state<>'ACTIVE' OR binding.role<>p_role THEN RAISE EXCEPTION 'WORKSPACE_MEMBERSHIP_IMMUTABLE' USING ERRCODE='23514'; END IF;
       member:=binding.id; UPDATE merchant.membership SET version=version+1,authority_version=authority_version+1,updated_at=stamp WHERE id=member;
     END IF;
   ELSE
     IF binding.id IS NULL OR binding.state<>'ACTIVE' THEN RAISE EXCEPTION 'WORKSPACE_CONTEXT_UNAVAILABLE' USING ERRCODE='23514'; END IF; member:=binding.id;
     IF cardinality(permissions)=0 THEN
       UPDATE merchant.membership SET state='REVOKED',version=version+1,authority_version=authority_version+1,updated_at=stamp WHERE id=member;
       DELETE FROM merchant.member_grant WHERE membership_id=member;
     ELSE
       DELETE FROM merchant.member_grant WHERE membership_id=member AND permission=ANY(permissions);
       UPDATE merchant.membership SET version=version+1,authority_version=authority_version+1,updated_at=stamp WHERE id=member;
     END IF;
   END IF;
 END IF;
 IF p_action IN ('PROVISION','GRANT') THEN
   INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope)
   SELECT gen_random_uuid(),member,p_org,p,'ORGANIZATION' FROM unnest(permissions) p ON CONFLICT(membership_id,permission) DO NOTHING;
 END IF;
 INSERT INTO merchant.workspace_audit VALUES(gen_random_uuid(),p_org,member,session_user,p_action,p_request,p_reason,stamp);
 INSERT INTO merchant.workspace_command_receipt VALUES(p_request,digest,p_org,member,p_action,stamp); RETURN member;
END $$;
REVOKE ALL ON merchant.organization,merchant.membership,merchant.member_grant,merchant.workspace_audit,merchant.workspace_command_receipt FROM PUBLIC,"${runtimeRole}";
GRANT SELECT ON merchant.organization,merchant.membership,merchant.member_grant TO "${runtimeRole}";
REVOKE ALL ON FUNCTION identity.lock_local_business_subject(UUID,BOOLEAN),merchant.configure_local_workspace(UUID,TEXT,UUID,TEXT,UUID,TEXT,TEXT[],TEXT),merchant.guard_workspace_identity(),merchant.check_workspace_invariants(),merchant.workspace_append_only() FROM PUBLIC,"${runtimeRole}";
