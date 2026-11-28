-- NWS-V1 approved 8adc4229ff00e728f322dd9341cb146a82e2dc16e1470dc077ec05eb55ac4ce5; no account/grant seed.
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='elogistics_local_operations_operator' AND NOT rolcanlogin AND NOT rolsuper AND NOT rolcreaterole AND NOT rolcreatedb AND NOT rolreplication AND NOT rolbypassrls)
 OR pg_has_role('${runtimeRole}','elogistics_local_operations_operator','MEMBER') THEN RAISE EXCEPTION 'NWS_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501'; END IF;
END $$;
ALTER TABLE identity.local_staff_principal ALTER COLUMN purpose TYPE VARCHAR(10);
ALTER TABLE identity.local_staff_principal DROP CONSTRAINT local_staff_principal_purpose_check;
ALTER TABLE identity.local_staff_principal ADD CONSTRAINT local_staff_principal_purpose_check CHECK(purpose IN ('SUPPORT','OPERATIONS'));
ALTER TABLE identity.local_staff_operator_receipt DROP CONSTRAINT local_staff_operator_receipt_namespace_check;
ALTER TABLE identity.local_staff_operator_receipt ADD CONSTRAINT local_staff_operator_receipt_namespace_check CHECK(namespace IN ('CSKH_IDENTITY_V1','OPS_IDENTITY_V1'));
ALTER TABLE identity.auth_session DROP CONSTRAINT auth_session_audience_check;
ALTER TABLE identity.auth_session ADD CONSTRAINT auth_session_audience_check CHECK(audience IN ('SYSTEM','STAFF','OPS'));
ALTER TABLE identity.csrf_binding DROP CONSTRAINT csrf_binding_audience_check;
ALTER TABLE identity.csrf_binding ADD CONSTRAINT csrf_binding_audience_check CHECK(audience IN ('SYSTEM','STAFF','OPS'));
GRANT USAGE ON SCHEMA identity TO elogistics_local_operations_operator;
GRANT CREATE ON SCHEMA identity TO identity_staff_writer;
CREATE FUNCTION identity.operations_staff_registered(p_account UUID) RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=p_account AND purpose='OPERATIONS')
$$;
CREATE FUNCTION identity.operations_staff_eligible(p_account UUID) RETURNS BOOLEAN LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT identity.local_staff_environment_matches() AND EXISTS(SELECT 1 FROM identity.local_staff_principal s JOIN identity.user_account a ON a.id=s.account_id
 JOIN identity.local_staff_environment e ON e.singleton AND s.environment=e.environment
 WHERE s.account_id=p_account AND s.purpose='OPERATIONS' AND s.state='ACTIVE' AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL)
$$;
CREATE FUNCTION identity.lock_local_operations_staff_subject(p_account UUID,p_require_eligible BOOLEAN) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$ BEGIN
 PERFORM pg_advisory_xact_lock(5279420865908921433); PERFORM 1 FROM identity.user_account WHERE id=p_account FOR UPDATE;
 IF NOT identity.operations_staff_registered(p_account) OR (p_require_eligible AND NOT identity.operations_staff_eligible(p_account)) THEN RAISE EXCEPTION 'OPERATIONS_STAFF_UNAVAILABLE' USING ERRCODE='42501'; END IF;
END $$;


CREATE OR REPLACE FUNCTION identity.guard_identity_session_audience() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='UPDATE' AND NEW.audience<>OLD.audience THEN RAISE EXCEPTION 'SESSION_AUDIENCE_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='auth_session' AND TG_OP='INSERT' THEN
  IF (NEW.audience='STAFF' AND NOT identity.support_staff_eligible(NEW.account_id)) OR
   (NEW.audience='OPS' AND NOT identity.operations_staff_eligible(NEW.account_id)) OR
   (NEW.audience='SYSTEM' AND identity.support_staff_registered(NEW.account_id)) THEN
   RAISE EXCEPTION 'SESSION_PRINCIPAL_DENIED' USING ERRCODE='23514';END IF;
 END IF;
 IF NEW.audience IN ('STAFF','OPS') AND NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_UNAVAILABLE' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;

CREATE OR REPLACE FUNCTION identity.check_local_staff_evidence() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE target UUID;s identity.local_staff_principal;a identity.user_account;r identity.local_staff_operator_receipt;
BEGIN
 IF TG_TABLE_NAME='user_account' THEN target:=NEW.id;
 ELSIF TG_TABLE_NAME IN ('action_proof','invitation_outbox') THEN
  IF NEW.purpose<>'STAFF_INV' THEN RETURN NULL;END IF;target:=NEW.account_id;
 ELSE target:=NEW.account_id;END IF;
 SELECT * INTO s FROM identity.local_staff_principal WHERE account_id=target;
 IF s.account_id IS NULL THEN
  IF TG_TABLE_NAME IN ('action_proof','invitation_outbox','local_staff_operator_receipt') THEN
   RAISE EXCEPTION 'STAFF_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
 END IF;
 SELECT * INTO a FROM identity.user_account WHERE id=target;
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=s.provision_request_id;
 IF r.namespace IS DISTINCT FROM (CASE s.purpose WHEN 'SUPPORT' THEN 'CSKH_IDENTITY_V1' WHEN 'OPERATIONS' THEN 'OPS_IDENTITY_V1' END) THEN RAISE EXCEPTION 'STAFF_PURPOSE_RECEIPT_REQUIRED' USING ERRCODE='23514'; END IF;
 IF a.id IS NULL OR r.operation<>'PROVISION' OR r.account_id<>target OR r.created_at<>s.created_at
 OR r.environment<>s.environment OR a.email COLLATE "C"<>s.exact_email OR
 EXISTS(SELECT 1 FROM identity.customer_identifier WHERE account_id=target) OR
 EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=target) OR
 EXISTS(SELECT 1 FROM merchant.membership WHERE account_id=target) OR
 EXISTS(SELECT 1 FROM identity.customer_enrollment WHERE exact_email COLLATE "C"=s.exact_email AND status='PENDING') OR
 EXISTS(SELECT 1 FROM identity.user_account WHERE email COLLATE "C"=s.exact_email AND id<>target) THEN
  RAISE EXCEPTION 'STAFF_PURPOSE_EVIDENCE_INVALID' USING ERRCODE='23514';END IF;
 IF (s.state='PENDING' AND (a.status<>'PENDING_ACTIVATION' OR a.password_hash IS NOT NULL OR a.email_verified)) OR
 (s.state='ACTIVE' AND (a.status<>'ACTIVE' OR NOT a.email_verified OR a.password_hash IS NULL OR NOT EXISTS(
  SELECT 1 FROM identity.action_proof p JOIN identity.command_receipt c ON c.proof_id=p.id
  JOIN identity.audit_entry u ON u.target_id=p.account_id AND u.event_type='STAFF_INVITATION_ACCEPTED' AND u.source='IDENTITY_STAFF'
  WHERE p.account_id=target AND p.purpose='STAFF_INV' AND p.consumed_at IS NOT NULL AND p.revoked_at IS NULL
  AND c.operation='acceptStaffInvitation' AND c.binding_kind='PROOF' AND c.target_id=target AND c.result_version=a.version))) OR
 (s.state='CANCELLED' AND (a.status<>'DISABLED' OR a.activated_at IS NOT NULL OR NOT EXISTS(
  SELECT 1 FROM identity.local_staff_operator_receipt WHERE account_id=target AND operation='CANCEL'))) THEN
  RAISE EXCEPTION 'STAFF_ACTIVATION_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM identity.local_staff_operator_receipt x WHERE x.account_id=target AND (
  NOT EXISTS(SELECT 1 FROM identity.audit_entry u WHERE u.id=x.audit_id AND u.target_id=target AND u.source='IDENTITY_STAFF' AND u.request_id=x.request_id::text
   AND u.event_type=CASE x.operation WHEN 'PROVISION' THEN 'STAFF_PROVISIONED' WHEN 'RESEND' THEN 'STAFF_INVITATION_RESENT' ELSE 'STAFF_INVITATION_CANCELLED' END)
  OR (x.operation IN ('PROVISION','RESEND') AND NOT EXISTS(
   SELECT 1 FROM identity.action_proof p JOIN identity.invitation_outbox o ON o.proof_id=p.id
   WHERE p.id=x.proof_id AND p.account_id=target AND p.purpose='STAFF_INV' AND p.created_at=x.created_at
   AND p.expires_at=p.created_at+INTERVAL '24 hours' AND o.id=x.message_id AND o.destination COLLATE "C"=s.exact_email AND o.format_version=2 AND o.realm_id IS NULL))
 )) THEN RAISE EXCEPTION 'STAFF_SEALED_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM identity.action_proof p WHERE p.account_id=target AND p.purpose='STAFF_INV' AND NOT EXISTS(
  SELECT 1 FROM identity.local_staff_operator_receipt x WHERE x.account_id=target AND x.proof_id=p.id)) THEN
  RAISE EXCEPTION 'STAFF_PROOF_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;

REVOKE ALL ON FUNCTION identity.operations_staff_registered(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.operations_staff_registered(UUID) TO CURRENT_USER;
ALTER FUNCTION identity.operations_staff_registered(UUID) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.operations_staff_eligible(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.operations_staff_eligible(UUID) TO CURRENT_USER;
ALTER FUNCTION identity.operations_staff_eligible(UUID) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.lock_local_operations_staff_subject(UUID,BOOLEAN) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.lock_local_operations_staff_subject(UUID,BOOLEAN) TO CURRENT_USER;
ALTER FUNCTION identity.lock_local_operations_staff_subject(UUID,BOOLEAN) OWNER TO identity_staff_writer;


SET ROLE identity_staff_writer;
CREATE FUNCTION identity.provision_local_operations_staff(p_request UUID,p_display_name TEXT,p_email TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment; r identity.local_staff_operator_receipt; h BYTEA;
 target UUID; proof UUID; message UUID; audit UUID; stamp TIMESTAMPTZ; principal_state TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';
 END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_display_name IS NULL
 OR length(btrim(p_display_name)) NOT BETWEEN 1 AND 160 OR p_display_name ~ '[[:cntrl:]]'
 OR p_email IS NULL OR length(p_email) NOT BETWEEN 3 AND 254 OR p_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
 OR p_reason IS NULL OR p_reason<>btrim(p_reason) OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';
 END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO e FROM identity.local_staff_environment WHERE singleton;
 h:=sha256(convert_to(jsonb_build_array('OPS_IDENTITY_V1',session_user,e.system_identifier,e.database_oid,e.database_name,e.environment,'PROVISION',p_display_name,p_email,p_reason)::text,'UTF8'));
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 IF FOUND THEN
  IF r.operator_principal<>session_user OR r.payload_digest<>h THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  SELECT state INTO principal_state FROM identity.local_staff_principal WHERE identity.local_staff_principal.account_id=r.account_id;
  RETURN QUERY SELECT r.account_id,r.proof_id,r.message_id,r.created_at,r.created_at+INTERVAL '24 hours',TRUE,principal_state;RETURN;
 END IF;
 IF EXISTS(SELECT 1 FROM identity.user_account WHERE email COLLATE "C"=p_email COLLATE "C") THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';
 END IF;
 target:=gen_random_uuid();proof:=gen_random_uuid();message:=gen_random_uuid();audit:=gen_random_uuid();stamp:=clock_timestamp();
 INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at)
 VALUES(target,'ops-'||target,p_display_name,p_email,'PENDING_ACTIVATION',stamp,stamp);
 INSERT INTO identity.local_staff_principal VALUES(target,p_email,'OPERATIONS',e.environment,'PENDING',1,p_request,stamp,stamp);
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(audit,target,'STAFF_PROVISIONED','IDENTITY_STAFF',p_reason,p_request::text,stamp);
 INSERT INTO identity.local_staff_operator_receipt VALUES(p_request,'OPS_IDENTITY_V1',session_user,e.environment,e.system_identifier,e.database_oid,e.database_name,
 'PROVISION',h,target,proof,message,audit,'PENDING_VERIFICATION',p_reason,stamp);
 RETURN QUERY SELECT target,proof,message,stamp,stamp+INTERVAL '24 hours',FALSE,'PENDING'::text;
END $$;

CREATE OR REPLACE FUNCTION identity.seal_local_support_staff_invitation(p_request UUID,p_verifier_key_id TEXT,p_encryption_key_id TEXT,p_token_hash BYTEA,p_nonce BYTEA,p_ciphertext BYTEA) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r identity.local_staff_operator_receipt; dest TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 IF NOT FOUND OR r.namespace<>'CSKH_IDENTITY_V1' OR r.operator_principal<>session_user OR r.operation NOT IN ('PROVISION','RESEND')
 OR r.database_name<>current_database() OR r.created_at+INTERVAL '24 hours'<=clock_timestamp()
 OR p_verifier_key_id IS NULL OR p_verifier_key_id !~ '^[A-Za-z0-9_-]{1,64}$'
 OR p_encryption_key_id IS NULL OR p_encryption_key_id !~ '^[A-Za-z0-9_-]{1,64}$'
 OR p_token_hash IS NULL OR octet_length(p_token_hash)<>32 OR p_nonce IS NULL OR octet_length(p_nonce)<>12
 OR p_ciphertext IS NULL OR octet_length(p_ciphertext)<>59 THEN
  RAISE EXCEPTION 'STAFF_SEAL_INVALID' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM identity.action_proof WHERE id=r.proof_id) THEN
  IF NOT EXISTS(SELECT 1 FROM identity.action_proof p JOIN identity.invitation_outbox o ON o.proof_id=p.id
   WHERE p.id=r.proof_id AND p.account_id=r.account_id AND p.purpose='STAFF_INV' AND p.token_hash=p_token_hash
   AND p.verifier_key_id=p_verifier_key_id AND o.id=r.message_id AND o.encryption_key_id=p_encryption_key_id
   AND (o.status IN ('SENT','FAILED','EXPIRED') OR (o.nonce=p_nonce AND o.ciphertext=p_ciphertext))) THEN
   RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN;
 END IF;
 SELECT exact_email INTO dest FROM identity.local_staff_principal WHERE account_id=r.account_id AND state='PENDING';
 IF dest IS NULL THEN RAISE EXCEPTION 'STAFF_SEAL_INVALID' USING ERRCODE='23514';END IF;
 INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,verifier_key_id)
 VALUES(r.proof_id,r.account_id,'STAFF_INV',p_token_hash,r.created_at,r.created_at+INTERVAL '24 hours',p_verifier_key_id);
 INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext)
 VALUES(r.message_id,r.proof_id,r.account_id,NULL,'STAFF_INV',dest,2,p_verifier_key_id,p_encryption_key_id,r.created_at,r.created_at+INTERVAL '24 hours','PENDING',r.created_at,p_nonce,p_ciphertext);
END $$;

CREATE FUNCTION identity.seal_local_operations_staff_invitation(p_request UUID,p_verifier_key_id TEXT,p_encryption_key_id TEXT,p_token_hash BYTEA,p_nonce BYTEA,p_ciphertext BYTEA) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r identity.local_staff_operator_receipt; dest TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 IF NOT FOUND OR r.namespace<>'OPS_IDENTITY_V1' OR r.operator_principal<>session_user OR r.operation NOT IN ('PROVISION','RESEND')
 OR r.database_name<>current_database() OR r.created_at+INTERVAL '24 hours'<=clock_timestamp()
 OR p_verifier_key_id IS NULL OR p_verifier_key_id !~ '^[A-Za-z0-9_-]{1,64}$'
 OR p_encryption_key_id IS NULL OR p_encryption_key_id !~ '^[A-Za-z0-9_-]{1,64}$'
 OR p_token_hash IS NULL OR octet_length(p_token_hash)<>32 OR p_nonce IS NULL OR octet_length(p_nonce)<>12
 OR p_ciphertext IS NULL OR octet_length(p_ciphertext)<>59 THEN
  RAISE EXCEPTION 'STAFF_SEAL_INVALID' USING ERRCODE='23514';END IF;
 IF EXISTS(SELECT 1 FROM identity.action_proof WHERE id=r.proof_id) THEN
  IF NOT EXISTS(SELECT 1 FROM identity.action_proof p JOIN identity.invitation_outbox o ON o.proof_id=p.id
   WHERE p.id=r.proof_id AND p.account_id=r.account_id AND p.purpose='STAFF_INV' AND p.token_hash=p_token_hash
   AND p.verifier_key_id=p_verifier_key_id AND o.id=r.message_id AND o.encryption_key_id=p_encryption_key_id
   AND (o.status IN ('SENT','FAILED','EXPIRED') OR (o.nonce=p_nonce AND o.ciphertext=p_ciphertext))) THEN
   RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN;
 END IF;
 SELECT exact_email INTO dest FROM identity.local_staff_principal WHERE account_id=r.account_id AND state='PENDING';
 IF dest IS NULL THEN RAISE EXCEPTION 'STAFF_SEAL_INVALID' USING ERRCODE='23514';END IF;
 INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,created_at,expires_at,verifier_key_id)
 VALUES(r.proof_id,r.account_id,'STAFF_INV',p_token_hash,r.created_at,r.created_at+INTERVAL '24 hours',p_verifier_key_id);
 INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext)
 VALUES(r.message_id,r.proof_id,r.account_id,NULL,'STAFF_INV',dest,2,p_verifier_key_id,p_encryption_key_id,r.created_at,r.created_at+INTERVAL '24 hours','PENDING',r.created_at,p_nonce,p_ciphertext);
END $$;

CREATE OR REPLACE FUNCTION identity.disposition_local_support_staff_invitation(p_request UUID,p_account UUID,p_action TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment;r identity.local_staff_operator_receipt;s identity.local_staff_principal;
 h BYTEA;proof UUID;message UUID;audit UUID;stamp TIMESTAMPTZ;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('RESEND','CANCEL')
 OR p_reason IS NULL OR p_reason<>btrim(p_reason) OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO e FROM identity.local_staff_environment WHERE singleton;
 h:=sha256(convert_to(jsonb_build_array('CSKH_IDENTITY_V1',session_user,e.system_identifier,e.database_oid,e.database_name,e.environment,p_action,p_account,p_reason)::text,'UTF8'));
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 SELECT * INTO s FROM identity.local_staff_principal WHERE identity.local_staff_principal.account_id=p_account FOR UPDATE;
 IF s.purpose IS DISTINCT FROM 'SUPPORT' THEN RAISE EXCEPTION 'STAFF_PURPOSE_REQUIRED' USING ERRCODE='42501'; END IF;
 IF r.request_id IS NOT NULL THEN
  IF r.operator_principal<>session_user OR r.payload_digest<>h THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT r.account_id,r.proof_id,r.message_id,r.created_at,CASE WHEN r.proof_id IS NOT NULL THEN r.created_at+INTERVAL '24 hours' END,TRUE,s.state::text;RETURN;
 END IF;
 IF s.account_id IS NULL OR s.state<>'PENDING' THEN RAISE EXCEPTION 'STAFF_INVITATION_NOT_PENDING' USING ERRCODE='23514';END IF;
 stamp:=clock_timestamp();audit:=gen_random_uuid();
 -- Offline requests have no trustworthy browser IP. Bound existing 3/account/hour and
 -- 20/operator/hour thresholds to persisted successful RESEND receipts under the same lock.
 IF p_action='RESEND' AND (
  (SELECT count(*) FROM identity.local_staff_operator_receipt q WHERE q.account_id=p_account AND q.operation='RESEND' AND q.created_at>stamp-INTERVAL '1 hour')>=3 OR
  (SELECT count(*) FROM identity.local_staff_operator_receipt q WHERE q.operator_principal=session_user AND q.operation='RESEND' AND q.created_at>stamp-INTERVAL '1 hour')>=20
 ) THEN RAISE EXCEPTION 'RATE_LIMITED' USING ERRCODE='23514';END IF;
 UPDATE identity.action_proof SET revoked_at=stamp WHERE identity.action_proof.account_id=p_account AND purpose='STAFF_INV' AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp,last_outcome='NOT_ELIGIBLE'
 WHERE identity.invitation_outbox.account_id=p_account AND purpose='STAFF_INV' AND status IN ('PENDING','LEASED');
 IF p_action='CANCEL' THEN
  UPDATE identity.user_account SET status='DISABLED',disabled_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=p_account;
  UPDATE identity.local_staff_principal SET state='CANCELLED',version=version+1,updated_at=stamp WHERE identity.local_staff_principal.account_id=p_account;
 ELSE proof:=gen_random_uuid();message:=gen_random_uuid();END IF;
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(audit,p_account,CASE WHEN p_action='CANCEL' THEN 'STAFF_INVITATION_CANCELLED' ELSE 'STAFF_INVITATION_RESENT' END,'IDENTITY_STAFF',p_reason,p_request::text,stamp);
 INSERT INTO identity.local_staff_operator_receipt VALUES(p_request,'CSKH_IDENTITY_V1',session_user,e.environment,e.system_identifier,e.database_oid,e.database_name,
 p_action,h,p_account,proof,message,audit,CASE WHEN p_action='CANCEL' THEN 'CANCELLED' ELSE 'RESENT' END,p_reason,stamp);
 RETURN QUERY SELECT p_account,proof,message,stamp,CASE WHEN proof IS NOT NULL THEN stamp+INTERVAL '24 hours' END,FALSE,CASE WHEN p_action='CANCEL' THEN 'CANCELLED' ELSE 'PENDING' END;
END $$;

CREATE FUNCTION identity.disposition_local_operations_staff_invitation(p_request UUID,p_account UUID,p_action TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment;r identity.local_staff_operator_receipt;s identity.local_staff_principal;
 h BYTEA;proof UUID;message UUID;audit UUID;stamp TIMESTAMPTZ;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('RESEND','CANCEL')
 OR p_reason IS NULL OR p_reason<>btrim(p_reason) OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO e FROM identity.local_staff_environment WHERE singleton;
 h:=sha256(convert_to(jsonb_build_array('OPS_IDENTITY_V1',session_user,e.system_identifier,e.database_oid,e.database_name,e.environment,p_action,p_account,p_reason)::text,'UTF8'));
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 SELECT * INTO s FROM identity.local_staff_principal WHERE identity.local_staff_principal.account_id=p_account FOR UPDATE;
 IF s.purpose IS DISTINCT FROM 'OPERATIONS' THEN RAISE EXCEPTION 'STAFF_PURPOSE_REQUIRED' USING ERRCODE='42501'; END IF;
 IF r.request_id IS NOT NULL THEN
  IF r.operator_principal<>session_user OR r.payload_digest<>h THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT r.account_id,r.proof_id,r.message_id,r.created_at,CASE WHEN r.proof_id IS NOT NULL THEN r.created_at+INTERVAL '24 hours' END,TRUE,s.state::text;RETURN;
 END IF;
 IF s.account_id IS NULL OR s.state<>'PENDING' THEN RAISE EXCEPTION 'STAFF_INVITATION_NOT_PENDING' USING ERRCODE='23514';END IF;
 stamp:=clock_timestamp();audit:=gen_random_uuid();
 -- Offline requests have no trustworthy browser IP. Bound existing 3/account/hour and
 -- 20/operator/hour thresholds to persisted successful RESEND receipts under the same lock.
 IF p_action='RESEND' AND (
  (SELECT count(*) FROM identity.local_staff_operator_receipt q WHERE q.account_id=p_account AND q.operation='RESEND' AND q.created_at>stamp-INTERVAL '1 hour')>=3 OR
  (SELECT count(*) FROM identity.local_staff_operator_receipt q WHERE q.operator_principal=session_user AND q.operation='RESEND' AND q.created_at>stamp-INTERVAL '1 hour')>=20
 ) THEN RAISE EXCEPTION 'RATE_LIMITED' USING ERRCODE='23514';END IF;
 UPDATE identity.action_proof SET revoked_at=stamp WHERE identity.action_proof.account_id=p_account AND purpose='STAFF_INV' AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp,last_outcome='NOT_ELIGIBLE'
 WHERE identity.invitation_outbox.account_id=p_account AND purpose='STAFF_INV' AND status IN ('PENDING','LEASED');
 IF p_action='CANCEL' THEN
  UPDATE identity.user_account SET status='DISABLED',disabled_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=p_account;
  UPDATE identity.local_staff_principal SET state='CANCELLED',version=version+1,updated_at=stamp WHERE identity.local_staff_principal.account_id=p_account;
 ELSE proof:=gen_random_uuid();message:=gen_random_uuid();END IF;
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(audit,p_account,CASE WHEN p_action='CANCEL' THEN 'STAFF_INVITATION_CANCELLED' ELSE 'STAFF_INVITATION_RESENT' END,'IDENTITY_STAFF',p_reason,p_request::text,stamp);
 INSERT INTO identity.local_staff_operator_receipt VALUES(p_request,'OPS_IDENTITY_V1',session_user,e.environment,e.system_identifier,e.database_oid,e.database_name,
 p_action,h,p_account,proof,message,audit,CASE WHEN p_action='CANCEL' THEN 'CANCELLED' ELSE 'RESENT' END,p_reason,stamp);
 RETURN QUERY SELECT p_account,proof,message,stamp,CASE WHEN proof IS NOT NULL THEN stamp+INTERVAL '24 hours' END,FALSE,CASE WHEN p_action='CANCEL' THEN 'CANCELLED' ELSE 'PENDING' END;
END $$;

CREATE OR REPLACE FUNCTION identity.accept_local_support_staff_invitation(p_proof UUID,p_key UUID,p_fingerprint BYTEA,p_password_verifier TEXT,p_request UUID) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE p identity.action_proof;s identity.local_staff_principal;a identity.user_account;r identity.command_receipt;stamp TIMESTAMPTZ;
BEGIN
 IF NOT identity.local_staff_environment_matches() THEN RAISE EXCEPTION 'STAFF_UNAVAILABLE' USING ERRCODE='42501';END IF;
 IF p_key IS NULL OR p_key='00000000-0000-0000-0000-000000000000' OR p_request IS NULL OR p_fingerprint IS NULL OR octet_length(p_fingerprint)<>32
 OR p_password_verifier IS NULL OR p_password_verifier !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO p FROM identity.action_proof WHERE id=p_proof AND purpose='STAFF_INV' FOR UPDATE;
 SELECT * INTO s FROM identity.local_staff_principal WHERE account_id=p.account_id FOR UPDATE;
 SELECT * INTO a FROM identity.user_account WHERE id=p.account_id FOR UPDATE;
 stamp:=clock_timestamp();
 IF s.purpose IS DISTINCT FROM 'SUPPORT' OR p.id IS NULL OR p.revoked_at IS NOT NULL OR p.expires_at<=stamp OR NOT EXISTS(
  SELECT 1 FROM identity.invitation_outbox WHERE proof_id=p.id AND destination COLLATE "C"=s.exact_email AND purpose='STAFF_INV' AND format_version=2) THEN
  RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO r FROM identity.command_receipt WHERE binding_kind='PROOF' AND binding_id=p_proof AND operation='acceptStaffInvitation' AND operation_key=p_key;
 IF FOUND THEN
  IF r.request_fingerprint<>p_fingerprint THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  IF p.consumed_at IS NULL OR NOT identity.support_staff_eligible(p.account_id) OR r.result_version<>a.version THEN
   RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;RETURN;
 END IF;
 IF s.state<>'PENDING' OR a.status<>'PENDING_ACTIVATION' OR p.consumed_at IS NOT NULL THEN
  RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;
 UPDATE identity.action_proof SET consumed_at=stamp WHERE id=p.id;
 UPDATE identity.user_account SET status='ACTIVE',email_verified=TRUE,password_hash=p_password_verifier,activated_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=p.account_id;
 UPDATE identity.local_staff_principal SET state='ACTIVE',version=version+1,updated_at=stamp WHERE account_id=p.account_id;
 INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at)
 VALUES('PROOF',p.id,'acceptStaffInvitation',p_key,p_fingerprint,p.account_id,a.version,a.version+1,204,stamp,stamp+INTERVAL '24 hours');
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,request_id,occurred_at)
 VALUES(gen_random_uuid(),p.account_id,'STAFF_INVITATION_ACCEPTED','IDENTITY_STAFF',p_request::text,stamp);
 UPDATE identity.invitation_outbox SET status='EXPIRED',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp,last_outcome='NOT_ELIGIBLE'
 WHERE proof_id=p.id AND status IN ('PENDING','LEASED');
END $$;

CREATE FUNCTION identity.accept_local_operations_staff_invitation(p_proof UUID,p_key UUID,p_fingerprint BYTEA,p_password_verifier TEXT,p_request UUID) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE p identity.action_proof;s identity.local_staff_principal;a identity.user_account;r identity.command_receipt;stamp TIMESTAMPTZ;
BEGIN
 IF NOT identity.local_staff_environment_matches() THEN RAISE EXCEPTION 'STAFF_UNAVAILABLE' USING ERRCODE='42501';END IF;
 IF p_key IS NULL OR p_key='00000000-0000-0000-0000-000000000000' OR p_request IS NULL OR p_fingerprint IS NULL OR octet_length(p_fingerprint)<>32
 OR p_password_verifier IS NULL OR p_password_verifier !~ '^\$2[aby]\$[0-9]{2}\$[./A-Za-z0-9]{53}$' THEN
  RAISE EXCEPTION 'STAFF_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO p FROM identity.action_proof WHERE id=p_proof AND purpose='STAFF_INV' FOR UPDATE;
 SELECT * INTO s FROM identity.local_staff_principal WHERE account_id=p.account_id FOR UPDATE;
 SELECT * INTO a FROM identity.user_account WHERE id=p.account_id FOR UPDATE;
 stamp:=clock_timestamp();
 IF s.purpose IS DISTINCT FROM 'OPERATIONS' OR p.id IS NULL OR p.revoked_at IS NOT NULL OR p.expires_at<=stamp OR NOT EXISTS(
  SELECT 1 FROM identity.invitation_outbox WHERE proof_id=p.id AND destination COLLATE "C"=s.exact_email AND purpose='STAFF_INV' AND format_version=2) THEN
  RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO r FROM identity.command_receipt WHERE binding_kind='PROOF' AND binding_id=p_proof AND operation='acceptStaffInvitation' AND operation_key=p_key;
 IF FOUND THEN
  IF r.request_fingerprint<>p_fingerprint THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  IF p.consumed_at IS NULL OR NOT identity.operations_staff_eligible(p.account_id) OR r.result_version<>a.version THEN
   RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;RETURN;
 END IF;
 IF s.state<>'PENDING' OR a.status<>'PENDING_ACTIVATION' OR p.consumed_at IS NOT NULL THEN
  RAISE EXCEPTION 'TOKEN_INVALID_OR_EXPIRED' USING ERRCODE='23514';END IF;
 UPDATE identity.action_proof SET consumed_at=stamp WHERE id=p.id;
 UPDATE identity.user_account SET status='ACTIVE',email_verified=TRUE,password_hash=p_password_verifier,activated_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=p.account_id;
 UPDATE identity.local_staff_principal SET state='ACTIVE',version=version+1,updated_at=stamp WHERE account_id=p.account_id;
 INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at)
 VALUES('PROOF',p.id,'acceptStaffInvitation',p_key,p_fingerprint,p.account_id,a.version,a.version+1,204,stamp,stamp+INTERVAL '24 hours');
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,request_id,occurred_at)
 VALUES(gen_random_uuid(),p.account_id,'STAFF_INVITATION_ACCEPTED','IDENTITY_STAFF',p_request::text,stamp);
 UPDATE identity.invitation_outbox SET status='EXPIRED',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp,last_outcome='NOT_ELIGIBLE'
 WHERE proof_id=p.id AND status IN ('PENDING','LEASED');
END $$;

CREATE OR REPLACE FUNCTION identity.local_support_staff_invitation_destination(p_request UUID) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT s.exact_email INTO destination FROM identity.local_staff_operator_receipt r JOIN identity.local_staff_principal s ON s.account_id=r.account_id
 WHERE s.purpose='SUPPORT' AND r.namespace='CSKH_IDENTITY_V1' AND r.request_id=p_request AND r.operator_principal=session_user AND r.operation IN ('PROVISION','RESEND') AND s.state='PENDING'
 AND r.xmin::text=pg_current_xact_id()::text;
 IF destination IS NULL THEN RAISE EXCEPTION 'STAFF_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN destination;
END $$;

CREATE FUNCTION identity.local_operations_staff_invitation_destination(p_request UUID) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT s.exact_email INTO destination FROM identity.local_staff_operator_receipt r JOIN identity.local_staff_principal s ON s.account_id=r.account_id
 WHERE s.purpose='OPERATIONS' AND r.namespace='OPS_IDENTITY_V1' AND r.request_id=p_request AND r.operator_principal=session_user AND r.operation IN ('PROVISION','RESEND') AND s.state='PENDING'
 AND r.xmin::text=pg_current_xact_id()::text;
 IF destination IS NULL THEN RAISE EXCEPTION 'STAFF_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN destination;
END $$;

REVOKE ALL ON FUNCTION identity.provision_local_operations_staff(UUID,TEXT,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.provision_local_operations_staff(UUID,TEXT,TEXT,TEXT) TO CURRENT_USER;
ALTER FUNCTION identity.provision_local_operations_staff(UUID,TEXT,TEXT,TEXT) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.seal_local_operations_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.seal_local_operations_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) TO CURRENT_USER;
ALTER FUNCTION identity.seal_local_operations_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.disposition_local_operations_staff_invitation(UUID,UUID,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.disposition_local_operations_staff_invitation(UUID,UUID,TEXT,TEXT) TO CURRENT_USER;
ALTER FUNCTION identity.disposition_local_operations_staff_invitation(UUID,UUID,TEXT,TEXT) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.accept_local_operations_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.accept_local_operations_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) TO CURRENT_USER;
ALTER FUNCTION identity.accept_local_operations_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) OWNER TO identity_staff_writer;

REVOKE ALL ON FUNCTION identity.local_operations_staff_invitation_destination(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_operations_staff_invitation_destination(UUID) TO CURRENT_USER;
ALTER FUNCTION identity.local_operations_staff_invitation_destination(UUID) OWNER TO identity_staff_writer;

GRANT EXECUTE ON FUNCTION identity.operations_staff_registered(UUID),identity.operations_staff_eligible(UUID) TO "${runtimeRole}";
GRANT EXECUTE ON FUNCTION identity.provision_local_operations_staff(UUID,TEXT,TEXT,TEXT),identity.seal_local_operations_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA),identity.disposition_local_operations_staff_invitation(UUID,UUID,TEXT,TEXT),identity.local_operations_staff_invitation_destination(UUID) TO elogistics_local_operations_operator;
GRANT EXECUTE ON FUNCTION identity.accept_local_operations_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) TO "${runtimeRole}";
-- Restore the canonical schema owner after the private writer block, not session_user.
DO $$ BEGIN EXECUTE format('SET LOCAL ROLE %I',(SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname='identity')); END $$;
REVOKE CREATE ON SCHEMA identity FROM identity_staff_writer;
