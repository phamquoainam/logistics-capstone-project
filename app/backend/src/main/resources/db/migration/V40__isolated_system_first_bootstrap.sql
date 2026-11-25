
-- AON Option B. New isolated realm only; V4 bootstrap/activation stay unchanged.
ALTER TABLE identity.local_admin_operator_receipt DROP CONSTRAINT local_admin_operator_receipt_operation_check;
ALTER TABLE identity.local_admin_operator_receipt DROP CONSTRAINT local_admin_operator_receipt_committed_outcome_check;
ALTER TABLE identity.local_admin_operator_receipt DROP CONSTRAINT local_admin_operator_receipt_check;
ALTER TABLE identity.local_admin_operator_receipt ADD CONSTRAINT aon_operator_operation_v40 CHECK(operation IN ('ENV_BIND','ENV_DISABLE','PROVISION','RESEND','CANCEL','SET_PERMISSIONS','REVOKE','REALM_BOOTSTRAP','REALM_RESEND','REALM_CANCEL','REALM_ACTIVATE','REALM_DISABLE'));
ALTER TABLE identity.local_admin_operator_receipt ADD CONSTRAINT aon_operator_outcome_v40 CHECK(committed_outcome IN ('BOUND','DISABLED','PENDING_VERIFICATION','RESENT','CANCELLED','PERMISSIONS_UPDATED','REVOKED','REALM_PENDING','REALM_RESENT','REALM_CANCELLED','REALM_ACTIVE','REALM_INACTIVE'));
ALTER TABLE identity.local_admin_operator_receipt ADD CONSTRAINT aon_operator_binding_v40 CHECK(
 (operation IN ('ENV_BIND','ENV_DISABLE','REALM_ACTIVATE','REALM_DISABLE') AND account_id IS NULL AND proof_id IS NULL AND message_id IS NULL AND original_version IS NULL AND committed_version IS NULL)
 OR(operation IN ('PROVISION','RESEND','REALM_BOOTSTRAP','REALM_RESEND') AND account_id IS NOT NULL AND proof_id IS NOT NULL AND message_id IS NOT NULL AND original_version>=0 AND committed_version=original_version+1)
 OR(operation IN ('CANCEL','SET_PERMISSIONS','REVOKE','REALM_CANCEL') AND account_id IS NOT NULL AND proof_id IS NULL AND message_id IS NULL AND original_version>0 AND committed_version=original_version+1));
CREATE OR REPLACE FUNCTION identity.guard_local_admin_operator_receipt() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'AON_RECEIPT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF NEW.operator_principal<>session_user OR NEW.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) OR NEW.database_name<>current_database() THEN RAISE EXCEPTION 'AON_RECEIPT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF NEW.operation LIKE 'REALM_%' THEN
  IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='identity')) OR NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 ELSIF current_user<>'identity_admin_writer' THEN RAISE EXCEPTION 'AON_RECEIPT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;

CREATE FUNCTION identity.provision_local_realm_bootstrap(p_request UUID,p_realm UUID,p_display_name TEXT,p_email TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment;realm_row identity.realm;prior identity.local_admin_operator_receipt;h BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();op TEXT;outcome TEXT;original BIGINT;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment env WHERE env.singleton AND env.realm_id=p_realm;
 IF NOT FOUND OR NOT (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system())) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO realm_row FROM identity.realm WHERE id=p_realm FOR UPDATE;

 IF p_display_name IS NULL OR length(p_display_name) NOT BETWEEN 1 AND 160 OR p_email IS NULL OR length(p_email) NOT BETWEEN 1 AND 254 OR p_email ~ '[[:space:][:cntrl:]]' OR p_email !~ '^[A-Za-z0-9.!#$%&''*+/=?^_`{|}~-]+@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?([.][A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*$' OR split_part(p_email,'@',1) LIKE '.%' OR split_part(p_email,'@',1) LIKE '%.' OR split_part(p_email,'@',1) LIKE '%..%' THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
h:=sha256(convert_to(jsonb_build_object('v',1,'operation','REALM_BOOTSTRAP','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',p_display_name,'email',p_email,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.realm_id<>p_realm OR prior.deployment_id<>e.deployment_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;

  RETURN QUERY SELECT prior.account_id,prior.proof_id,prior.message_id,prior.created_at,CASE WHEN prior.proof_id IS NULL THEN NULL::timestamptz ELSE prior.created_at+INTERVAL '24 hours' END,TRUE,(SELECT CASE u.status WHEN 'ACTIVE' THEN 'ACTIVE' WHEN 'PENDING_ACTIVATION' THEN 'PENDING' ELSE 'REVOKED' END FROM identity.user_account u WHERE u.id=prior.account_id);RETURN;
 END IF;

 IF realm_row.status<>'INACTIVE' OR realm_row.activated_at IS NOT NULL OR realm_row.bootstrap_account_id IS NOT NULL THEN RAISE EXCEPTION 'AON_FRESH_REALM_REQUIRED' USING ERRCODE='42501';END IF;
 IF EXISTS(SELECT 1 FROM identity.user_account WHERE email COLLATE "C"=p_email) OR EXISTS(SELECT 1 FROM identity.customer_enrollment WHERE exact_email COLLATE "C"=p_email AND status='PENDING') THEN RAISE EXCEPTION 'AON_CONTACT_UNAVAILABLE' USING ERRCODE='23514';END IF;
 target:=gen_random_uuid();proof:=gen_random_uuid();message:=gen_random_uuid();original:=0;op:='REALM_BOOTSTRAP';outcome:='REALM_PENDING';
 PERFORM identity.bootstrap_system_realm_admin(p_realm,target,'root_'||replace(target::text,'-',''),p_display_name,p_email,p_request);

 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,op,'LOCAL_REALM_OPERATOR','Isolated request-bound first bootstrap operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,CASE WHEN original IS NULL THEN NULL ELSE original+1 END,outcome,a,stamp);

 RETURN QUERY SELECT target,proof,message,stamp,stamp+INTERVAL '24 hours',FALSE,'PENDING'::text;
END $$;

CREATE FUNCTION identity.seal_local_realm_bootstrap_invitation(p_request UUID,p_verifier_key_id TEXT,p_encryption_key_id TEXT,p_token_hash BYTEA,p_nonce BYTEA,p_ciphertext BYTEA) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r identity.local_admin_operator_receipt;destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request AND x.xmin::text=pg_current_xact_id()::text AND x.operation IN ('REALM_BOOTSTRAP','REALM_RESEND') AND x.operator_principal=session_user AND x.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user);
 IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM identity.local_admin_environment e JOIN identity.realm q ON q.id=e.realm_id WHERE e.realm_id=r.realm_id AND q.status='INACTIVE' AND q.activated_at IS NULL AND q.bootstrap_account_id=r.account_id AND (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system()))) THEN RAISE EXCEPTION 'AON_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 SELECT email INTO destination FROM identity.user_account WHERE id=r.account_id AND status='PENDING_ACTIVATION';
 IF destination IS NULL OR p_verifier_key_id IS NULL OR p_verifier_key_id !~ '^[A-Za-z0-9_-]{1,64}$' OR p_encryption_key_id IS NULL OR p_encryption_key_id !~ '^[A-Za-z0-9_-]{1,64}$' OR p_token_hash IS NULL OR octet_length(p_token_hash)<>32 OR p_nonce IS NULL OR octet_length(p_nonce)<>12 OR p_ciphertext IS NULL OR octet_length(p_ciphertext)<>59 THEN RAISE EXCEPTION 'AON_SEAL_INVALID' USING ERRCODE='22023';END IF;
 INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,verifier_key_id,created_at,expires_at) VALUES(r.proof_id,r.account_id,'INVITATION',p_token_hash,p_verifier_key_id,r.created_at,r.created_at+INTERVAL '24 hours');
 INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext)
 VALUES(r.message_id,r.proof_id,r.account_id,r.realm_id,'INVITATION',destination,1,p_verifier_key_id,p_encryption_key_id,r.created_at,r.created_at+INTERVAL '24 hours','PENDING',r.created_at,p_nonce,p_ciphertext);
END $$;
CREATE FUNCTION identity.local_realm_bootstrap_invitation_destination(p_request UUID) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT u.email INTO destination FROM identity.local_admin_operator_receipt r JOIN identity.user_account u ON u.id=r.account_id JOIN identity.local_admin_environment e ON e.realm_id=r.realm_id JOIN identity.realm q ON q.id=r.realm_id
 WHERE r.request_id=p_request AND r.operator_principal=session_user AND r.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user) AND r.operation IN ('REALM_BOOTSTRAP','REALM_RESEND') AND r.xmin::text=pg_current_xact_id()::text AND u.status='PENDING_ACTIVATION' AND q.bootstrap_account_id=u.id AND q.activated_at IS NULL AND q.status='INACTIVE' AND (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system()));
 IF destination IS NULL THEN RAISE EXCEPTION 'AON_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;RETURN destination;
END $$;

CREATE FUNCTION identity.activate_local_system_realm(p_request UUID,p_realm UUID,p_reason TEXT)
RETURNS TABLE(request_id UUID,realm_id UUID,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment;realm_row identity.realm;prior identity.local_admin_operator_receipt;h BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();op TEXT;outcome TEXT;original BIGINT;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment env WHERE env.singleton AND env.realm_id=p_realm;
 IF NOT FOUND OR NOT (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system())) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO realm_row FROM identity.realm WHERE id=p_realm FOR UPDATE;
h:=sha256(convert_to(jsonb_build_object('v',1,'operation','REALM_ACTIVATE','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',NULL,'email',NULL,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.realm_id<>p_realm OR prior.deployment_id<>e.deployment_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
RETURN QUERY SELECT p_request,p_realm,realm_row.status::text,TRUE;RETURN;END IF;
IF NOT EXISTS(SELECT 1 FROM identity.local_admin_operator_receipt x WHERE x.realm_id=p_realm AND x.account_id=realm_row.bootstrap_account_id AND x.operation='REALM_BOOTSTRAP') THEN RAISE EXCEPTION 'AON_BOOTSTRAP_PROVENANCE_REQUIRED' USING ERRCODE='42501';END IF;

 IF realm_row.status<>'INACTIVE' OR realm_row.activated_at IS NOT NULL THEN RAISE EXCEPTION 'AON_FRESH_REALM_REQUIRED' USING ERRCODE='42501';END IF;
 PERFORM identity.activate_system_realm(p_realm,realm_row.bootstrap_account_id,p_request);
 op:='REALM_ACTIVATE';outcome:='REALM_ACTIVE';

 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,op,'LOCAL_REALM_OPERATOR','Isolated request-bound first bootstrap operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,CASE WHEN original IS NULL THEN NULL ELSE original+1 END,outcome,a,stamp);
RETURN QUERY SELECT p_request,p_realm,'ACTIVE'::text,FALSE;
END $$;

CREATE FUNCTION identity.disable_local_system_realm(p_request UUID,p_realm UUID,p_reason TEXT)
RETURNS TABLE(request_id UUID,realm_id UUID,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment;realm_row identity.realm;prior identity.local_admin_operator_receipt;h BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();op TEXT;outcome TEXT;original BIGINT;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment env WHERE env.singleton AND env.realm_id=p_realm;
 IF NOT FOUND OR NOT (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system())) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO realm_row FROM identity.realm WHERE id=p_realm FOR UPDATE;
h:=sha256(convert_to(jsonb_build_object('v',1,'operation','REALM_DISABLE','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',NULL,'email',NULL,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.realm_id<>p_realm OR prior.deployment_id<>e.deployment_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
RETURN QUERY SELECT p_request,p_realm,realm_row.status::text,TRUE;RETURN;END IF;
IF NOT EXISTS(SELECT 1 FROM identity.local_admin_operator_receipt x WHERE x.realm_id=p_realm AND x.account_id=realm_row.bootstrap_account_id AND x.operation='REALM_BOOTSTRAP') THEN RAISE EXCEPTION 'AON_BOOTSTRAP_PROVENANCE_REQUIRED' USING ERRCODE='42501';END IF;

 IF realm_row.status<>'ACTIVE' OR realm_row.activated_at IS NULL THEN RAISE EXCEPTION 'AON_ACTIVE_REALM_REQUIRED' USING ERRCODE='42501';END IF;
 UPDATE identity.realm SET status='INACTIVE',updated_at=stamp WHERE id=p_realm;
 UPDATE identity.auth_session SET revoked_at=stamp WHERE account_id IN(SELECT g.account_id FROM identity.account_grant g WHERE g.scope_type='IDENTITY' AND g.scope_id=p_realm) AND audience='SYSTEM' AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT z.id FROM identity.auth_session z JOIN identity.account_grant g ON g.account_id=z.account_id WHERE z.audience='SYSTEM' AND g.scope_type='IDENTITY' AND g.scope_id=p_realm);
 op:='REALM_DISABLE';outcome:='REALM_INACTIVE';

 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,op,'LOCAL_REALM_OPERATOR','Isolated request-bound first bootstrap operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,CASE WHEN original IS NULL THEN NULL ELSE original+1 END,outcome,a,stamp);
RETURN QUERY SELECT p_request,p_realm,'INACTIVE'::text,FALSE;
END $$;

CREATE FUNCTION identity.disposition_local_realm_bootstrap_invitation(p_request UUID,p_realm UUID,p_action TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment;realm_row identity.realm;prior identity.local_admin_operator_receipt;h BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();op TEXT;outcome TEXT;original BIGINT;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_realm_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_REALM_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment env WHERE env.singleton AND env.realm_id=p_realm;
 IF NOT FOUND OR NOT (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system())) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO realm_row FROM identity.realm WHERE id=p_realm FOR UPDATE;
IF p_action IS NULL OR p_action NOT IN ('RESEND','CANCEL') THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;op:='REALM_'||p_action;
h:=sha256(convert_to(jsonb_build_object('v',1,'operation',op,'deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',NULL,'email',NULL,'permissions',NULL,'action',p_action,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.realm_id<>p_realm OR prior.deployment_id<>e.deployment_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;

  RETURN QUERY SELECT prior.account_id,prior.proof_id,prior.message_id,prior.created_at,CASE WHEN prior.proof_id IS NULL THEN NULL::timestamptz ELSE prior.created_at+INTERVAL '24 hours' END,TRUE,(SELECT CASE u.status WHEN 'ACTIVE' THEN 'ACTIVE' WHEN 'PENDING_ACTIVATION' THEN 'PENDING' ELSE 'REVOKED' END FROM identity.user_account u WHERE u.id=prior.account_id);RETURN;
 END IF;

 IF realm_row.status<>'INACTIVE' OR realm_row.activated_at IS NOT NULL OR NOT EXISTS(SELECT 1 FROM identity.local_admin_operator_receipt x WHERE x.operation='REALM_BOOTSTRAP' AND x.realm_id=p_realm AND x.account_id=realm_row.bootstrap_account_id) THEN RAISE EXCEPTION 'AON_FRESH_REALM_REQUIRED' USING ERRCODE='42501';END IF;
 target:=realm_row.bootstrap_account_id;
 SELECT version INTO original FROM identity.user_account WHERE id=target AND status='PENDING_ACTIVATION' FOR UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_PENDING_ROOT_REQUIRED' USING ERRCODE='42501';END IF;
 IF p_action='RESEND' AND ((SELECT count(*) FROM identity.local_admin_operator_receipt x WHERE x.account_id=target AND x.operation='REALM_RESEND' AND x.created_at>stamp-INTERVAL '1 hour')>=3 OR(SELECT count(*) FROM identity.local_admin_operator_receipt x WHERE x.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user) AND x.operation='REALM_RESEND' AND x.created_at>stamp-INTERVAL '1 hour')>=20) THEN RAISE EXCEPTION 'AON_RESEND_THROTTLED' USING ERRCODE='23514';END IF;
 UPDATE identity.action_proof SET revoked_at=stamp WHERE action_proof.account_id=target AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE invitation_outbox.account_id=target AND status IN ('PENDING','LEASED');
 UPDATE identity.user_account SET status=CASE WHEN p_action='CANCEL' THEN 'DISABLED' ELSE status END,disabled_at=CASE WHEN p_action='CANCEL' THEN stamp ELSE disabled_at END,version=version+1,auth_version=auth_version+1,updated_at=stamp WHERE id=target;
 IF p_action='RESEND' THEN proof:=gen_random_uuid();message:=gen_random_uuid();outcome:='REALM_RESENT';ELSE outcome:='REALM_CANCELLED';END IF;

 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,op,'LOCAL_REALM_OPERATOR','Isolated request-bound first bootstrap operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,CASE WHEN original IS NULL THEN NULL ELSE original+1 END,outcome,a,stamp);

 RETURN QUERY SELECT target,proof,message,stamp,CASE WHEN proof IS NULL THEN NULL::timestamptz ELSE stamp+INTERVAL '24 hours' END,FALSE,CASE WHEN p_action='CANCEL' THEN 'REVOKED'::text ELSE 'PENDING'::text END;
END $$;

CREATE FUNCTION identity.accept_local_realm_bootstrap_invitation(p_proof UUID,p_key UUID,p_fingerprint BYTEA,p_password_verifier TEXT,p_request UUID) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE subject identity.local_admin_operator_receipt;proof identity.action_proof;a identity.user_account;e identity.local_admin_environment;prior identity.command_receipt;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;
BEGIN
 IF p_proof IS NULL OR p_key IS NULL OR p_key='00000000-0000-0000-0000-000000000000' OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_fingerprint IS NULL OR octet_length(p_fingerprint)<>32 OR p_password_verifier IS NULL OR p_password_verifier !~ '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$' THEN RAISE EXCEPTION 'AON_ACCEPT_INVALID' USING ERRCODE='22023';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_key::text,39));
 SELECT s.* INTO subject FROM identity.local_admin_operator_receipt s JOIN identity.action_proof f ON f.account_id=s.account_id WHERE f.id=p_proof AND s.operation='REALM_BOOTSTRAP';
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_PROOF_INVALID' USING ERRCODE='42501';END IF;
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton AND realm_id=subject.realm_id;
 IF NOT FOUND OR NOT (e.state='ACTIVE' AND e.environment IN ('LOCAL','TEST') AND e.purpose='ADMIN_ISOLATED_UAT' AND e.database_name=current_database() AND e.database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$' AND e.database_oid=(SELECT oid FROM pg_database WHERE datname=current_database()) AND e.system_identifier=(SELECT system_identifier::text FROM pg_control_system())) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(subject.realm_id::text,40));
 IF NOT EXISTS(SELECT 1 FROM identity.realm WHERE id=subject.realm_id AND status='INACTIVE' AND activated_at IS NULL AND bootstrap_account_id=subject.account_id) THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM identity.user_account WHERE id=subject.account_id FOR UPDATE;
 SELECT * INTO proof FROM identity.action_proof WHERE id=p_proof FOR UPDATE;
 SELECT * INTO prior FROM identity.command_receipt c WHERE c.binding_kind='PROOF' AND c.proof_id=p_proof AND c.operation='acceptIdentityInvitation' AND c.operation_key=p_key;
 IF FOUND THEN
  IF prior.request_fingerprint<>p_fingerprint THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
  IF prior.target_id<>a.id OR prior.result_version<>a.version OR prior.expires_at<=stamp OR proof.consumed_at IS NULL OR proof.revoked_at IS NOT NULL OR a.status<>'ACTIVE' THEN RAISE EXCEPTION 'AON_ACCEPT_STATE_INVALID' USING ERRCODE='42501';END IF;
  RETURN;
 END IF;
 IF a.status<>'PENDING_ACTIVATION' OR a.email_verified OR a.password_hash IS NOT NULL OR proof.purpose<>'INVITATION' OR proof.consumed_at IS NOT NULL OR proof.revoked_at IS NOT NULL OR stamp<proof.created_at OR stamp>=proof.expires_at OR proof.expires_at<>proof.created_at+INTERVAL '24 hours' OR NOT EXISTS(SELECT 1 FROM identity.invitation_outbox o WHERE o.proof_id=proof.id AND o.account_id=a.id AND o.realm_id=subject.realm_id AND o.destination COLLATE "C"=a.email AND o.format_version=1) THEN RAISE EXCEPTION 'AON_PROOF_INVALID' USING ERRCODE='42501';END IF;
 UPDATE identity.user_account SET status='ACTIVE',email_verified=TRUE,password_hash=p_password_verifier,activated_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=a.id;

 UPDATE identity.action_proof SET consumed_at=stamp WHERE id=proof.id;
 UPDATE identity.auth_session SET revoked_at=stamp WHERE account_id=a.id AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT id FROM identity.auth_session WHERE account_id=a.id);
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE proof_id=proof.id AND status IN ('PENDING','LEASED');
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(gen_random_uuid(),a.id,'LOCAL_REALM_INVITATION_ACCEPTED','IDENTITY_LOCAL_REALM','Verified isolated Initial Root invitation',p_request::text,stamp);
 INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at)
 VALUES('PROOF',proof.id,'acceptIdentityInvitation',p_key,p_fingerprint,a.id,a.version,a.version+1,204,stamp,stamp+INTERVAL '24 hours');
END $$;

CREATE FUNCTION identity.check_local_realm_bootstrap_evidence() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE target UUID;original identity.local_admin_operator_receipt;latest identity.local_admin_operator_receipt;subject identity.user_account;realm_row identity.realm;
BEGIN
 IF TG_TABLE_NAME='realm' THEN target:=NEW.bootstrap_account_id;
 ELSIF TG_TABLE_NAME='user_account' THEN target:=NEW.id;
 ELSE target:=NEW.account_id;END IF;
 SELECT * INTO original FROM identity.local_admin_operator_receipt x WHERE x.account_id=target AND x.operation='REALM_BOOTSTRAP';
 IF NOT FOUND THEN RETURN NULL;END IF;
 SELECT * INTO realm_row FROM identity.realm WHERE id=original.realm_id;
 SELECT * INTO subject FROM identity.user_account WHERE id=target;
 IF realm_row.bootstrap_account_id IS DISTINCT FROM target OR EXISTS(SELECT 1 FROM identity.local_admin_principal WHERE account_id=target) OR EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=target) OR EXISTS(SELECT 1 FROM identity.customer_identifier WHERE account_id=target) OR EXISTS(SELECT 1 FROM merchant.membership WHERE account_id=target) OR (SELECT count(*) FROM identity.account_grant WHERE account_id=target)<>1 OR NOT EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=target AND role_code='IDENTITY_MANAGEMENT' AND scope_type='IDENTITY' AND scope_id=original.realm_id) THEN RAISE EXCEPTION 'AON_ROOT_BINDING_INVALID' USING ERRCODE='23514';END IF;
 IF NOT EXISTS(SELECT 1 FROM identity.audit_entry x WHERE x.id=original.audit_id AND x.target_id=target AND x.event_type='REALM_BOOTSTRAP' AND x.source='LOCAL_REALM_OPERATOR' AND x.request_id=original.request_id::text) THEN RAISE EXCEPTION 'AON_ROOT_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 IF subject.status='PENDING_ACTIVATION' THEN
  SELECT * INTO latest FROM identity.local_admin_operator_receipt x WHERE x.account_id=target AND x.operation IN ('REALM_BOOTSTRAP','REALM_RESEND') ORDER BY x.committed_version DESC LIMIT 1;
  IF subject.version<>latest.committed_version OR NOT EXISTS(SELECT 1 FROM identity.action_proof f JOIN identity.invitation_outbox o ON o.proof_id=f.id WHERE f.id=latest.proof_id AND f.account_id=target AND f.purpose='INVITATION' AND f.verifier_key_id IS NOT NULL AND f.revoked_at IS NULL AND f.consumed_at IS NULL AND f.created_at=latest.created_at AND f.expires_at=f.created_at+INTERVAL '24 hours' AND o.id=latest.message_id AND o.realm_id=original.realm_id AND o.destination COLLATE "C"=subject.email AND o.format_version=1) THEN RAISE EXCEPTION 'AON_ROOT_SEAL_REQUIRED' USING ERRCODE='23514';END IF;
 ELSIF subject.status='ACTIVE' THEN
  IF NOT EXISTS(SELECT 1 FROM identity.action_proof f JOIN identity.command_receipt c ON c.proof_id=f.id JOIN identity.audit_entry u ON u.target_id=target WHERE f.account_id=target AND f.purpose='INVITATION' AND f.consumed_at IS NOT NULL AND f.revoked_at IS NULL AND c.binding_kind='PROOF' AND c.operation='acceptIdentityInvitation' AND c.target_id=target AND u.event_type='LOCAL_REALM_INVITATION_ACCEPTED' AND u.source='IDENTITY_LOCAL_REALM') THEN RAISE EXCEPTION 'AON_ROOT_VERIFICATION_REQUIRED' USING ERRCODE='23514';END IF;
 ELSIF NOT EXISTS(SELECT 1 FROM identity.local_admin_operator_receipt x WHERE x.account_id=target AND x.operation='REALM_CANCEL' AND x.committed_version=subject.version) THEN RAISE EXCEPTION 'AON_ROOT_CANCEL_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER aon_root_account_evidence AFTER INSERT OR UPDATE ON identity.user_account DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_realm_bootstrap_evidence();
CREATE CONSTRAINT TRIGGER aon_root_receipt_evidence AFTER INSERT ON identity.local_admin_operator_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_realm_bootstrap_evidence();
CREATE CONSTRAINT TRIGGER aon_root_proof_evidence AFTER INSERT OR UPDATE ON identity.action_proof DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_realm_bootstrap_evidence();
CREATE CONSTRAINT TRIGGER aon_root_delivery_evidence AFTER INSERT OR UPDATE ON identity.invitation_outbox DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_realm_bootstrap_evidence();
CREATE CONSTRAINT TRIGGER aon_root_realm_evidence AFTER UPDATE ON identity.realm DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_realm_bootstrap_evidence();
REVOKE ALL ON FUNCTION identity.check_local_realm_bootstrap_evidence() FROM PUBLIC;
REVOKE ALL ON FUNCTION identity.provision_local_realm_bootstrap(UUID,UUID,TEXT,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.provision_local_realm_bootstrap(UUID,UUID,TEXT,TEXT,TEXT) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.seal_local_realm_bootstrap_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.seal_local_realm_bootstrap_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.local_realm_bootstrap_invitation_destination(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_realm_bootstrap_invitation_destination(UUID) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.activate_local_system_realm(UUID,UUID,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.activate_local_system_realm(UUID,UUID,TEXT) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.disable_local_system_realm(UUID,UUID,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.disable_local_system_realm(UUID,UUID,TEXT) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.disposition_local_realm_bootstrap_invitation(UUID,UUID,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.disposition_local_realm_bootstrap_invitation(UUID,UUID,TEXT,TEXT) TO elogistics_local_realm_operator;
REVOKE ALL ON FUNCTION identity.accept_local_realm_bootstrap_invitation(UUID,UUID,BYTEA,TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.accept_local_realm_bootstrap_invitation(UUID,UUID,BYTEA,TEXT,UUID) TO "${runtimeRole}";
