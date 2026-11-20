-- CSKH-GRANT-02: reuse canonical storage, retire the writable legacy operator.
DO $$ BEGIN
 IF NOT pg_has_role(current_user,'support_grant_writer','SET') OR
  pg_has_role('${runtimeRole}','support_grant_writer','SET') OR
  pg_has_role('elogistics_local_staff_operator','support_grant_writer','SET') THEN
  RAISE EXCEPTION 'CSKH_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';END IF;
 IF EXISTS(SELECT 1 FROM orders.support_staff_grant g WHERE g.state='ACTIVE' AND NOT identity.support_staff_registered(g.account_id)) THEN
  RAISE EXCEPTION 'EXISTING_NON_STAFF_AUTHORITY_REQUIRES_DISPOSITION' USING ERRCODE='23514';END IF;
END $$;

CREATE TABLE orders.support_grant_operator_receipt (
 request_id UUID PRIMARY KEY CHECK(request_id<>'00000000-0000-0000-0000-000000000000'),
 namespace VARCHAR(24) NOT NULL CHECK(namespace='CSKH_GRANT_V1'), scope VARCHAR(16) NOT NULL CHECK(scope='CANONICAL_STAFF'),
 operator_principal NAME NOT NULL, environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 system_identifier NUMERIC(20,0) NOT NULL,database_oid OID NOT NULL,database_name NAME NOT NULL,
 account_id UUID NOT NULL REFERENCES identity.local_staff_principal(account_id),
 permission VARCHAR(20) NOT NULL CHECK(permission IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')),
 action VARCHAR(6) NOT NULL CHECK(action IN ('GRANT','REVOKE')),
 reason VARCHAR(300) NOT NULL CHECK(reason=btrim(reason) AND length(reason)>0 AND reason !~ '[[:cntrl:]]'),
 payload_digest BYTEA NOT NULL CHECK(octet_length(payload_digest)=32), grant_id UUID,
 prior_version BIGINT NOT NULL CHECK(prior_version>=0),result_version BIGINT NOT NULL CHECK(result_version>=0),
 committed_outcome VARCHAR(9) NOT NULL CHECK(committed_outcome IN ('GRANTED','REVOKED','NO_CHANGE')),
 audit_id UUID UNIQUE REFERENCES orders.support_business_audit(id) DEFERRABLE INITIALLY DEFERRED,
 created_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(grant_id,account_id,permission) REFERENCES orders.support_staff_grant(id,account_id,permission),
 CHECK((committed_outcome='NO_CHANGE' AND prior_version=result_version AND audit_id IS NULL AND (grant_id IS NOT NULL OR prior_version=0)) OR
 (committed_outcome IN ('GRANTED','REVOKED') AND grant_id IS NOT NULL AND audit_id IS NOT NULL AND result_version=prior_version+1
 AND ((committed_outcome='GRANTED' AND action='GRANT') OR (committed_outcome='REVOKED' AND action='REVOKE'))))
);
-- Preserve support_business_audit_check (new_version = prior_version + 1).
ALTER TABLE orders.support_business_audit DROP CONSTRAINT support_business_audit_check1;
ALTER TABLE orders.support_business_audit ADD CONSTRAINT support_business_audit_check1 CHECK(
 (ticket_id IS NOT NULL AND owner_id IS NOT NULL AND actor_id IS NOT NULL AND request_id IS NOT NULL AND operator_principal IS NULL AND authority_grant_id IS NULL) OR
 (ticket_id IS NULL AND owner_id IS NULL AND actor_id IS NOT NULL AND operator_principal IS NOT NULL AND authority_grant_id IS NOT NULL AND operation IN ('GRANT','REVOKE')));
ALTER TABLE orders.support_business_audit ADD COLUMN operator_request_id UUID GENERATED ALWAYS AS
 (CASE WHEN operator_principal IS NOT NULL AND request_id IS NOT NULL THEN request_id ELSE NULL END) STORED;
ALTER TABLE orders.support_business_audit ADD FOREIGN KEY(operator_request_id)
 REFERENCES orders.support_grant_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;
REVOKE ALL ON orders.support_grant_operator_receipt FROM PUBLIC,"${runtimeRole}",elogistics_local_staff_operator;
GRANT USAGE,CREATE ON SCHEMA orders TO support_grant_writer;
GRANT USAGE ON SCHEMA orders TO elogistics_local_staff_operator;
GRANT SELECT,INSERT ON orders.support_staff_grant,orders.support_business_audit,orders.support_grant_operator_receipt TO support_grant_writer;
GRANT UPDATE(state,version) ON orders.support_staff_grant TO support_grant_writer;
GRANT SELECT ON identity.local_staff_environment TO support_grant_writer;

CREATE FUNCTION orders.configure_local_support_staff(p_request UUID,target UUID,permission_name TEXT,action TEXT,p_reason TEXT)
RETURNS TABLE(request_id UUID,account_id UUID,permission TEXT,committed_outcome TEXT,grant_id UUID,committed_version BIGINT,current_grant_state TEXT,effective_grant_id UUID,effective_permission_active BOOLEAN,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment;r orders.support_grant_operator_receipt;g orders.support_staff_grant;
 h BYTEA;stamp TIMESTAMPTZ;assigned UUID;prior BIGINT;revision BIGINT;audit UUID;outcome TEXT;effective UUID;original_state TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR target IS NULL OR
 permission_name IS NULL OR permission_name NOT IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE') OR action IS NULL OR action NOT IN ('GRANT','REVOKE')
 OR p_reason IS NULL OR p_reason<>btrim(p_reason) OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN
  RAISE EXCEPTION 'SUPPORT_OPERATOR_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM identity.lock_local_support_staff_subject(target,action='GRANT');
 PERFORM pg_advisory_xact_lock(730022,1);
 SELECT * INTO e FROM identity.local_staff_environment WHERE singleton;
 h:=sha256(convert_to(jsonb_build_array('CSKH_GRANT_V1',session_user,e.system_identifier,e.database_oid,e.database_name,e.environment,action,target,permission_name,p_reason)::text,'UTF8'));
 SELECT * INTO r FROM orders.support_grant_operator_receipt WHERE orders.support_grant_operator_receipt.request_id=p_request;
 IF FOUND THEN
  IF r.operator_principal<>session_user OR r.payload_digest<>h THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  SELECT state INTO original_state FROM orders.support_staff_grant WHERE id=r.grant_id;
  SELECT id INTO effective FROM orders.support_staff_grant WHERE orders.support_staff_grant.account_id=target AND orders.support_staff_grant.permission=permission_name AND state='ACTIVE' AND effective_from<=clock_timestamp();
  RETURN QUERY SELECT r.request_id,r.account_id,r.permission::text,r.committed_outcome::text,r.grant_id,r.result_version,COALESCE(original_state,'ABSENT'),effective,effective IS NOT NULL,TRUE;RETURN;
 END IF;
 SELECT * INTO g FROM orders.support_staff_grant WHERE orders.support_staff_grant.account_id=target AND orders.support_staff_grant.permission=permission_name AND state='ACTIVE' FOR UPDATE;
 assigned:=g.id;prior:=COALESCE(g.version,0);revision:=prior;outcome:='NO_CHANGE';stamp:=clock_timestamp();
 IF action='GRANT' AND assigned IS NULL THEN
  assigned:=gen_random_uuid();revision:=1;outcome:='GRANTED';
  INSERT INTO orders.support_staff_grant VALUES(assigned,target,permission_name,'ACTIVE',revision,stamp);
 ELSIF action='REVOKE' AND assigned IS NOT NULL THEN
  revision:=prior+1;outcome:='REVOKED';
  UPDATE orders.support_staff_grant SET state='REVOKED',version=revision WHERE id=assigned;
 END IF;
 IF outcome<>'NO_CHANGE' THEN
  audit:=gen_random_uuid();
  INSERT INTO orders.support_business_audit(id,actor_id,operation,prior_version,new_version,request_id,occurred_at,operator_principal,authority_grant_id)
  VALUES(audit,target,action,prior,revision,p_request,stamp,session_user,assigned);
 END IF;
 INSERT INTO orders.support_grant_operator_receipt VALUES(p_request,'CSKH_GRANT_V1','CANONICAL_STAFF',session_user,e.environment,e.system_identifier,e.database_oid,e.database_name,
 target,permission_name,action,p_reason,h,assigned,prior,revision,outcome,audit,stamp);
 SELECT state INTO original_state FROM orders.support_staff_grant WHERE id=assigned;
 SELECT id INTO effective FROM orders.support_staff_grant WHERE orders.support_staff_grant.account_id=target AND orders.support_staff_grant.permission=permission_name AND state='ACTIVE' AND effective_from<=stamp;
 RETURN QUERY SELECT p_request,target,permission_name,outcome,assigned,revision,COALESCE(original_state,'ABSENT'),effective,effective IS NOT NULL,FALSE;
END $$;
ALTER FUNCTION orders.configure_local_support_staff(UUID,UUID,TEXT,TEXT,TEXT) OWNER TO support_grant_writer;
REVOKE ALL ON FUNCTION orders.configure_local_support_staff(UUID,UUID,TEXT,TEXT,TEXT) FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION orders.configure_local_support_staff(UUID,UUID,TEXT,TEXT,TEXT) TO elogistics_local_staff_operator;
REVOKE CREATE ON SCHEMA orders FROM support_grant_writer;
CREATE OR REPLACE FUNCTION orders.configure_local_support_staff(target UUID,permission_name TEXT,action TEXT) RETURNS UUID
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'REQUEST_BOUND_SUPPORT_OPERATOR_REQUIRED' USING ERRCODE='42501';END $$;
REVOKE ALL ON FUNCTION orders.configure_local_support_staff(UUID,TEXT,TEXT) FROM PUBLIC,"${runtimeRole}",elogistics_local_staff_operator;

CREATE FUNCTION orders.guard_support_operator_request() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_TABLE_NAME='support_staff_grant' THEN
  IF current_user<>'support_grant_writer' OR TG_OP='DELETE' THEN RAISE EXCEPTION 'SUPPORT_GRANT_WRITE_DENIED' USING ERRCODE='42501';END IF;
  IF TG_OP='UPDATE' AND (OLD.state<>'ACTIVE' OR NEW.state<>'REVOKED') THEN RAISE EXCEPTION 'SUPPORT_GRANT_TERMINAL' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='support_grant_operator_receipt' THEN
  IF TG_OP<>'INSERT' OR current_user<>'support_grant_writer' THEN RAISE EXCEPTION 'SUPPORT_RECEIPT_IMMUTABLE' USING ERRCODE='42501';END IF;
 ELSE
  IF NEW.operator_principal IS NOT NULL AND (current_user<>'support_grant_writer' OR NEW.request_id IS NULL OR NEW.operator_principal<>session_user) THEN
   RAISE EXCEPTION 'SUPPORT_OPERATOR_AUDIT_DENIED' USING ERRCODE='42501';END IF;
 END IF;RETURN NEW;
END $$;
CREATE FUNCTION orders.check_support_operator_request() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r orders.support_grant_operator_receipt;g orders.support_staff_grant;
BEGIN
 IF TG_TABLE_NAME='support_grant_operator_receipt' THEN
  r:=NEW;
 ELSIF TG_TABLE_NAME='support_staff_grant' THEN
  SELECT * INTO r FROM orders.support_grant_operator_receipt WHERE grant_id=NEW.id AND result_version=NEW.version AND committed_outcome<>'NO_CHANGE' AND xmin::text=pg_current_xact_id()::text;
 ELSE
  IF NEW.operator_principal IS NULL THEN RETURN NULL;END IF;
  SELECT * INTO r FROM orders.support_grant_operator_receipt WHERE request_id=NEW.request_id AND audit_id=NEW.id;
 END IF;
 IF r.request_id IS NULL THEN RAISE EXCEPTION 'SUPPORT_REQUEST_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 IF r.grant_id IS NOT NULL THEN
  SELECT * INTO g FROM orders.support_staff_grant WHERE id=r.grant_id;
  IF g.id IS NULL OR g.account_id<>r.account_id OR g.permission<>r.permission OR g.version<>r.result_version OR
   (r.committed_outcome='GRANTED' AND g.state<>'ACTIVE') OR (r.committed_outcome='REVOKED' AND g.state<>'REVOKED') THEN
   RAISE EXCEPTION 'SUPPORT_REQUEST_GRANT_MISMATCH' USING ERRCODE='23514';END IF;
 END IF;
 IF r.committed_outcome<>'NO_CHANGE' AND NOT EXISTS(SELECT 1 FROM orders.support_business_audit a
  WHERE a.id=r.audit_id AND a.actor_id=r.account_id AND a.authority_grant_id=r.grant_id AND a.request_id=r.request_id
  AND a.operator_principal=r.operator_principal AND a.operation=r.action AND a.prior_version=r.prior_version AND a.new_version=r.result_version
  AND a.xmin::text=pg_current_xact_id()::text) THEN
  RAISE EXCEPTION 'SUPPORT_REQUEST_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE TRIGGER support_operator_grant_provenance BEFORE INSERT OR UPDATE OR DELETE ON orders.support_staff_grant FOR EACH ROW EXECUTE FUNCTION orders.guard_support_operator_request();
CREATE TRIGGER support_operator_receipt_immutable BEFORE INSERT OR UPDATE OR DELETE ON orders.support_grant_operator_receipt FOR EACH ROW EXECUTE FUNCTION orders.guard_support_operator_request();
CREATE TRIGGER support_operator_audit_provenance BEFORE INSERT ON orders.support_business_audit FOR EACH ROW EXECUTE FUNCTION orders.guard_support_operator_request();
CREATE CONSTRAINT TRIGGER support_operator_receipt_atomic AFTER INSERT ON orders.support_grant_operator_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_support_operator_request();
CREATE CONSTRAINT TRIGGER support_operator_grant_atomic AFTER INSERT OR UPDATE ON orders.support_staff_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_support_operator_request();
CREATE CONSTRAINT TRIGGER support_operator_audit_atomic AFTER INSERT ON orders.support_business_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_support_operator_request();
REVOKE ALL ON FUNCTION orders.guard_support_operator_request(),orders.check_support_operator_request() FROM PUBLIC;

CREATE OR REPLACE FUNCTION orders.organization_support_staff(org UUID,actor UUID,required TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE AS $$
 SELECT identity.support_staff_eligible(actor) AND EXISTS(SELECT 1 FROM orders.organization_support_staff_scope s
 JOIN orders.support_staff_grant g ON (g.id,g.account_id,g.permission)=(s.grant_id,s.account_id,s.permission)
 JOIN merchant.organization o ON o.id=s.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id
 WHERE s.organization_id=org AND s.account_id=actor AND s.permission=required AND s.state='ACTIVE' AND g.state='ACTIVE'
 AND s.effective_from<=CURRENT_TIMESTAMP AND g.effective_from<=CURRENT_TIMESTAMP
 AND o.state='ACTIVE' AND o.environment IN ('LOCAL','TEST') AND owner.state='ACTIVE' AND owner.role='OWNER')
$$;
GRANT EXECUTE ON FUNCTION identity.lock_local_support_staff_subject(UUID,BOOLEAN) TO CURRENT_USER;

-- Preserve V35 operator signature/digest/audit; change only Staff target admission.
CREATE OR REPLACE FUNCTION orders.configure_local_organization_support_staff(p_request UUID,p_action TEXT,p_org UUID,p_account UUID,p_permission TEXT,p_reason TEXT)
 RETURNS TABLE(request_id UUID,outcome TEXT,version BIGINT,organization_id UUID,account_id UUID,permission TEXT)
 LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,orders AS $$
DECLARE prior orders.support_operator_audit%ROWTYPE;s orders.organization_support_staff_scope%ROWTYPE;g UUID;input_digest BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='orders')) OR COALESCE(current_setting('argopost.operator_environment',TRUE),'') NOT IN ('LOCAL','TEST')
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OFFLINE_OPERATOR_REQUIRED' USING ERRCODE='42501';END IF;
 IF p_request IS NULL OR p_org IS NULL OR p_account IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_account='00000000-0000-0000-0000-000000000000'
  OR p_action IS NULL OR p_action NOT IN ('BIND','UNBIND') OR p_permission IS NULL OR p_permission NOT IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')
  OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]'
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM identity.lock_local_support_staff_subject(p_account,p_action='BIND');
 PERFORM pg_advisory_xact_lock(730023,1);PERFORM pg_advisory_xact_lock(730022,1);PERFORM pg_advisory_xact_lock(730035,1);
 input_digest:=sha256(convert_to(jsonb_build_array(current_setting('argopost.operator_environment'),p_action,p_org,p_account,p_permission,p_reason)::TEXT,'UTF8'));
 SELECT * INTO prior FROM orders.support_operator_audit WHERE support_operator_audit.request_id=p_request;
 IF FOUND THEN
  IF prior.request_digest<>input_digest THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_KEY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT prior.request_id,CASE prior.action WHEN 'BIND' THEN 'BOUND' ELSE 'UNBOUND' END,prior.result_version,prior.organization_id,prior.target_account_id,prior.permission::TEXT;RETURN;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id WHERE o.id=p_org AND o.environment=current_setting('argopost.operator_environment') AND o.state='ACTIVE' AND m.state='ACTIVE' AND m.role='OWNER')
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_ORGANIZATION_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO s FROM orders.organization_support_staff_scope WHERE organization_support_staff_scope.organization_id=p_org AND organization_support_staff_scope.account_id=p_account AND organization_support_staff_scope.permission=p_permission FOR UPDATE;
 IF p_action='BIND' THEN
  SELECT id INTO g FROM orders.support_staff_grant WHERE support_staff_grant.account_id=p_account AND support_staff_grant.permission=p_permission AND state='ACTIVE' AND effective_from<=stamp;
  IF g IS NULL THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CANONICAL_GRANT_REQUIRED' USING ERRCODE='23514';END IF;
  IF s.state='ACTIVE' AND s.grant_id=g THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_BINDING_NO_CHANGE' USING ERRCODE='23514';END IF;
 ELSE
  IF s.state IS DISTINCT FROM 'ACTIVE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ACTIVE_BINDING_REQUIRED' USING ERRCODE='23514';END IF;g:=s.grant_id;
 END IF;
 IF s.organization_id IS NULL THEN
  INSERT INTO orders.organization_support_staff_scope VALUES(p_org,g,p_account,p_permission,'ACTIVE',1,stamp,stamp);
 ELSE
  UPDATE orders.organization_support_staff_scope SET grant_id=g,state=CASE p_action WHEN 'BIND' THEN 'ACTIVE' ELSE 'REVOKED' END,version=s.version+1,effective_from=stamp,updated_at=stamp
   WHERE organization_support_staff_scope.organization_id=p_org AND organization_support_staff_scope.account_id=p_account AND organization_support_staff_scope.permission=p_permission;
 END IF;
 INSERT INTO orders.support_operator_audit VALUES(gen_random_uuid(),p_request,p_org,p_account,p_permission,p_action,p_reason,session_user,stamp,input_digest,s.grant_id,g,COALESCE(s.version,0),COALESCE(s.version,0)+1);
 RETURN QUERY SELECT p_request,CASE p_action WHEN 'BIND' THEN 'BOUND' ELSE 'UNBOUND' END,COALESCE(s.version,0)+1,p_org,p_account,p_permission;
END $$;
