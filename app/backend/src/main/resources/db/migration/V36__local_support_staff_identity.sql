-- CSKH-IDENTITY-01 / Owner ACL-05 amendment: cluster roles are bootstrapped
-- separately on an owned isolated test cluster. Never CREATE ROLE in Flyway.
DO $$
DECLARE r RECORD;
BEGIN
 FOR r IN SELECT required.name,p.* FROM
  (VALUES ('elogistics_local_staff_operator'),('identity_staff_writer'),('support_grant_writer')) required(name)
  LEFT JOIN pg_catalog.pg_roles p ON p.rolname=required.name LOOP
  IF r.oid IS NULL OR r.rolcanlogin OR r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls THEN
   RAISE EXCEPTION 'CSKH_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';
  END IF;
 END LOOP;
 IF NOT pg_catalog.pg_has_role(current_user,'identity_staff_writer','SET') OR
    NOT pg_catalog.pg_has_role(current_user,'support_grant_writer','SET') OR
    pg_catalog.pg_has_role('${runtimeRole}','identity_staff_writer','SET') OR
    pg_catalog.pg_has_role('${runtimeRole}','support_grant_writer','SET') OR
    pg_catalog.pg_has_role('${runtimeRole}','elogistics_local_staff_operator','SET') OR
    pg_catalog.pg_has_role('elogistics_local_staff_operator','identity_staff_writer','SET') OR
    pg_catalog.pg_has_role('elogistics_local_staff_operator','support_grant_writer','SET') OR
    NOT pg_catalog.has_function_privilege('identity_staff_writer','pg_catalog.pg_control_system()','EXECUTE') THEN
  RAISE EXCEPTION 'CSKH_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';
 END IF;
END $$;

CREATE TABLE identity.local_staff_environment (
 singleton BOOLEAN PRIMARY KEY CHECK(singleton),
 environment VARCHAR(8) NOT NULL CHECK(environment IN ('DISABLED','LOCAL','TEST')),
 enabled BOOLEAN NOT NULL DEFAULT FALSE,
 system_identifier NUMERIC(20,0), database_oid OID, database_name NAME,
 audit_id UUID UNIQUE REFERENCES identity.audit_entry(id), bound_by NAME,
 binding_request_id UUID UNIQUE,
 binding_digest BYTEA CHECK(binding_digest IS NULL OR octet_length(binding_digest)=32),
 CHECK ((NOT enabled AND environment='DISABLED' AND system_identifier IS NULL AND database_oid IS NULL AND database_name IS NULL AND audit_id IS NULL AND bound_by IS NULL AND binding_request_id IS NULL AND binding_digest IS NULL)
 OR (enabled AND environment IN ('LOCAL','TEST') AND system_identifier IS NOT NULL AND database_oid IS NOT NULL AND database_name IS NOT NULL AND audit_id IS NOT NULL AND bound_by IS NOT NULL AND binding_request_id IS NOT NULL AND binding_digest IS NOT NULL))
);
INSERT INTO identity.local_staff_environment(singleton,environment) VALUES(TRUE,'DISABLED');

ALTER TABLE identity.user_account ADD CONSTRAINT uq_identity_staff_email_binding UNIQUE(id,email);
CREATE TABLE identity.local_staff_principal (
 account_id UUID PRIMARY KEY REFERENCES identity.user_account(id) ON DELETE RESTRICT,
 exact_email TEXT COLLATE "C" NOT NULL UNIQUE,
 purpose VARCHAR(7) NOT NULL CHECK(purpose='SUPPORT'),
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 state VARCHAR(9) NOT NULL CHECK(state IN ('PENDING','ACTIVE','CANCELLED')),
 version BIGINT NOT NULL CHECK(version>0), provision_request_id UUID NOT NULL UNIQUE,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 FOREIGN KEY(account_id,exact_email) REFERENCES identity.user_account(id,email) ON DELETE RESTRICT
);
CREATE TABLE identity.local_staff_operator_receipt (
 request_id UUID PRIMARY KEY CHECK(request_id<>'00000000-0000-0000-0000-000000000000'),
 namespace VARCHAR(24) NOT NULL CHECK(namespace='CSKH_IDENTITY_V1'), operator_principal NAME NOT NULL,
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 system_identifier NUMERIC(20,0) NOT NULL, database_oid OID NOT NULL, database_name NAME NOT NULL,
 operation VARCHAR(9) NOT NULL CHECK(operation IN ('PROVISION','RESEND','CANCEL')),
 payload_digest BYTEA NOT NULL CHECK(octet_length(payload_digest)=32),
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 proof_id UUID REFERENCES identity.action_proof(id) DEFERRABLE INITIALLY DEFERRED,
 message_id UUID REFERENCES identity.invitation_outbox(id) DEFERRABLE INITIALLY DEFERRED,
 audit_id UUID NOT NULL UNIQUE REFERENCES identity.audit_entry(id) DEFERRABLE INITIALLY DEFERRED,
 committed_outcome VARCHAR(20) NOT NULL CHECK(committed_outcome IN ('PENDING_VERIFICATION','RESENT','CANCELLED')),
 reason VARCHAR(300) NOT NULL CHECK(reason=btrim(reason) AND length(reason)>0 AND reason !~ '[[:cntrl:]]'),
 created_at TIMESTAMPTZ NOT NULL,
 CHECK((operation='PROVISION' AND committed_outcome='PENDING_VERIFICATION' AND proof_id IS NOT NULL AND message_id IS NOT NULL)
 OR (operation='RESEND' AND committed_outcome='RESENT' AND proof_id IS NOT NULL AND message_id IS NOT NULL)
 OR (operation='CANCEL' AND committed_outcome='CANCELLED' AND proof_id IS NULL AND message_id IS NULL))
);
ALTER TABLE identity.local_staff_principal ADD FOREIGN KEY(provision_request_id)
 REFERENCES identity.local_staff_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE identity.action_proof DROP CONSTRAINT action_proof_purpose_check;
ALTER TABLE identity.action_proof ADD CONSTRAINT action_proof_purpose_check CHECK(purpose IN ('INVITATION','RECOVERY','STAFF_INV'));
ALTER TABLE identity.action_proof DROP CONSTRAINT ck_identity_proof_clock;
ALTER TABLE identity.action_proof ADD CONSTRAINT ck_identity_proof_clock CHECK(expires_at>created_at AND
 expires_at<=created_at+CASE WHEN purpose IN ('INVITATION','STAFF_INV') THEN INTERVAL '24 hours' ELSE INTERVAL '15 minutes' END AND
 (purpose<>'STAFF_INV' OR expires_at=created_at+INTERVAL '24 hours') AND
 (consumed_at IS NULL OR consumed_at BETWEEN created_at AND expires_at) AND (revoked_at IS NULL OR revoked_at>=created_at));
ALTER TABLE identity.invitation_outbox ALTER COLUMN realm_id DROP NOT NULL;
ALTER TABLE identity.invitation_outbox DROP CONSTRAINT invitation_outbox_purpose_check;
ALTER TABLE identity.invitation_outbox DROP CONSTRAINT invitation_outbox_format_version_check;
ALTER TABLE identity.invitation_outbox ADD CONSTRAINT ck_identity_outbox_purpose_version CHECK(
 (purpose='INVITATION' AND realm_id IS NOT NULL AND format_version=1) OR
 (purpose='STAFF_INV' AND realm_id IS NULL AND format_version=2));

ALTER TABLE identity.auth_session ADD COLUMN audience VARCHAR(6) NOT NULL DEFAULT 'SYSTEM' CHECK(audience IN ('SYSTEM','STAFF'));
ALTER TABLE identity.csrf_binding ADD COLUMN audience VARCHAR(6) NOT NULL DEFAULT 'SYSTEM' CHECK(audience IN ('SYSTEM','STAFF'));
ALTER TABLE identity.auth_session ADD CONSTRAINT uq_identity_session_audience UNIQUE(id,audience);
ALTER TABLE identity.csrf_binding ADD CONSTRAINT fk_identity_csrf_audience FOREIGN KEY(session_id,audience) REFERENCES identity.auth_session(id,audience);
ALTER TABLE identity.command_receipt DROP CONSTRAINT command_receipt_operation_check;
ALTER TABLE identity.command_receipt ADD CONSTRAINT command_receipt_operation_check CHECK(operation IN (
 'provisionIdentityAccount','replaceIdentityGrants','disableIdentityAccount','reactivateIdentityAccount',
 'resendIdentityInvitation','requestIdentityRecovery','confirmIdentityRecovery','acceptIdentityInvitation','acceptStaffInvitation'));
ALTER TABLE identity.command_receipt DROP CONSTRAINT ck_identity_receipt_binding;
ALTER TABLE identity.command_receipt ADD CONSTRAINT ck_identity_receipt_binding CHECK(
 (binding_kind='ACTOR' AND actor_id IS NOT NULL AND proof_id IS NULL AND preauth_binding_id IS NULL AND target_id IS NOT NULL AND operation IN ('provisionIdentityAccount','replaceIdentityGrants','disableIdentityAccount','reactivateIdentityAccount','resendIdentityInvitation')) OR
 (binding_kind='PROOF' AND actor_id IS NULL AND proof_id IS NOT NULL AND preauth_binding_id IS NULL AND target_id IS NOT NULL AND operation IN ('confirmIdentityRecovery','acceptIdentityInvitation','acceptStaffInvitation')) OR
 (binding_kind='PREAUTH' AND actor_id IS NULL AND proof_id IS NULL AND preauth_binding_id IS NOT NULL AND target_id IS NULL AND operation='requestIdentityRecovery'));

REVOKE ALL ON identity.local_staff_environment,identity.local_staff_principal,identity.local_staff_operator_receipt FROM PUBLIC,"${runtimeRole}",elogistics_local_staff_operator;
GRANT USAGE ON SCHEMA identity TO identity_staff_writer,elogistics_local_staff_operator,support_grant_writer;
GRANT CREATE ON SCHEMA identity TO identity_staff_writer;
GRANT SELECT ON identity.local_staff_environment,identity.local_staff_principal,identity.local_staff_operator_receipt,
 identity.user_account,identity.action_proof,identity.invitation_outbox,identity.audit_entry,identity.command_receipt TO identity_staff_writer;
GRANT INSERT ON identity.user_account,identity.local_staff_principal,identity.local_staff_operator_receipt,
 identity.action_proof,identity.invitation_outbox,identity.audit_entry,identity.command_receipt TO identity_staff_writer;
GRANT UPDATE(state,version,updated_at) ON identity.local_staff_principal TO identity_staff_writer;
GRANT UPDATE(password_hash,status,email_verified,version,auth_version,updated_at,activated_at,disabled_at) ON identity.user_account TO identity_staff_writer;
GRANT UPDATE(consumed_at,revoked_at) ON identity.action_proof TO identity_staff_writer;
GRANT UPDATE(status,nonce,ciphertext,finished_at,lease_id,lease_until,last_outcome) ON identity.invitation_outbox TO identity_staff_writer;
GRANT SELECT ON identity.local_staff_principal TO "${runtimeRole}";

CREATE FUNCTION identity.local_staff_environment_matches() RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 RETURN EXISTS(SELECT 1 FROM identity.local_staff_environment e,pg_catalog.pg_database d,pg_catalog.pg_control_system() p
  WHERE e.singleton AND e.enabled AND e.environment IN ('LOCAL','TEST') AND d.datname=current_database()
  AND e.database_name=d.datname AND e.database_oid=d.oid AND e.system_identifier=p.system_identifier);
EXCEPTION WHEN insufficient_privilege THEN RETURN FALSE;
END $$;
CREATE FUNCTION identity.support_staff_registered(p_account UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=p_account)
$$;
CREATE FUNCTION identity.support_staff_eligible(p_account UUID) RETURNS BOOLEAN
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT identity.local_staff_environment_matches() AND EXISTS(
  SELECT 1 FROM identity.local_staff_principal s JOIN identity.user_account a ON a.id=s.account_id
  JOIN identity.local_staff_environment e ON e.singleton AND s.environment=e.environment
  WHERE s.account_id=p_account AND s.purpose='SUPPORT' AND s.state='ACTIVE'
  AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL)
$$;
CREATE FUNCTION identity.lock_local_support_staff_subject(p_account UUID,p_require_eligible BOOLEAN) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 PERFORM pg_catalog.pg_advisory_xact_lock(5279420865908921433);
 PERFORM 1 FROM identity.user_account WHERE id=p_account FOR UPDATE;
 IF NOT identity.support_staff_registered(p_account) OR (p_require_eligible AND NOT identity.support_staff_eligible(p_account)) THEN
  RAISE EXCEPTION 'STAFF_UNAVAILABLE' USING ERRCODE='42501';
 END IF;
END $$;

CREATE FUNCTION identity.bind_local_staff_environment(p_request UUID,p_environment TEXT,p_reason TEXT)
RETURNS TABLE(environment TEXT,system_identifier NUMERIC,database_oid OID,database_name NAME,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment; a UUID; h BYTEA; sys NUMERIC; db OID;
BEGIN
 IF current_user<>pg_catalog.pg_get_userbyid((SELECT relowner FROM pg_catalog.pg_class WHERE oid='identity.local_staff_environment'::regclass))
 OR p_environment NOT IN ('LOCAL','TEST') OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000'
 OR p_reason IS NULL OR p_reason<>btrim(p_reason) OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN
  RAISE EXCEPTION 'STAFF_ENVIRONMENT_BINDING_DENIED' USING ERRCODE='42501';
 END IF;
 SELECT p.system_identifier INTO sys FROM pg_catalog.pg_control_system() p;
 SELECT oid INTO db FROM pg_catalog.pg_database WHERE datname=current_database();
 h:=sha256(convert_to(jsonb_build_array('CSKH_ENVIRONMENT_V1',session_user,sys,db,current_database(),p_environment,p_reason)::text,'UTF8'));
 SELECT * INTO e FROM identity.local_staff_environment WHERE singleton FOR UPDATE;
 IF e.enabled THEN
  IF e.binding_request_id<>p_request OR e.binding_digest<>h OR e.bound_by<>session_user THEN
   RAISE EXCEPTION 'STAFF_ENVIRONMENT_BINDING_CONFLICT' USING ERRCODE='23514';
  END IF;
  RETURN QUERY SELECT e.environment::text,e.system_identifier,e.database_oid,e.database_name,TRUE; RETURN;
 END IF;
 a:=gen_random_uuid();
 INSERT INTO identity.audit_entry(id,event_type,source,reason,request_id,occurred_at)
 VALUES(a,'STAFF_ENVIRONMENT_BOUND','IDENTITY_STAFF',p_reason,p_request::text,clock_timestamp());
 UPDATE identity.local_staff_environment SET environment=p_environment,enabled=TRUE,system_identifier=sys,database_oid=db,
 database_name=current_database(),audit_id=a,bound_by=session_user,binding_request_id=p_request,binding_digest=h WHERE singleton;
 RETURN QUERY SELECT p_environment,sys,db,current_database()::name,FALSE;
END $$;

CREATE FUNCTION identity.guard_local_staff_environment() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP<>'UPDATE' OR OLD.enabled OR NEW.singleton<>OLD.singleton OR NOT NEW.enabled OR
 current_user<>pg_catalog.pg_get_userbyid((SELECT relowner FROM pg_catalog.pg_class WHERE oid=TG_RELID)) OR
 NOT EXISTS(SELECT 1 FROM identity.audit_entry WHERE id=NEW.audit_id AND event_type='STAFF_ENVIRONMENT_BOUND'
 AND source='IDENTITY_STAFF' AND request_id=NEW.binding_request_id::text) THEN
  RAISE EXCEPTION 'STAFF_ENVIRONMENT_IMMUTABLE' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER local_staff_environment_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.local_staff_environment
 FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_environment();

-- Privileged predicates expose booleans/locks only; no public operator route.
ALTER FUNCTION identity.local_staff_environment_matches() OWNER TO identity_staff_writer;
ALTER FUNCTION identity.support_staff_registered(UUID) OWNER TO identity_staff_writer;
ALTER FUNCTION identity.support_staff_eligible(UUID) OWNER TO identity_staff_writer;
ALTER FUNCTION identity.lock_local_support_staff_subject(UUID,BOOLEAN) OWNER TO identity_staff_writer;
REVOKE ALL ON FUNCTION identity.local_staff_environment_matches(),identity.support_staff_registered(UUID),
 identity.support_staff_eligible(UUID),identity.lock_local_support_staff_subject(UUID,BOOLEAN),
 identity.bind_local_staff_environment(UUID,TEXT,TEXT),identity.guard_local_staff_environment() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_staff_environment_matches(),identity.support_staff_registered(UUID),identity.support_staff_eligible(UUID)
 TO "${runtimeRole}",elogistics_local_staff_operator,support_grant_writer;
GRANT EXECUTE ON FUNCTION identity.lock_local_support_staff_subject(UUID,BOOLEAN) TO support_grant_writer;
-- Read only the environment admission result; never expose physical binding identifiers to runtime.
CREATE FUNCTION identity.local_staff_environment_matches(p_environment TEXT) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT p_environment IN ('LOCAL','TEST') AND identity.local_staff_environment_matches()
 AND EXISTS(SELECT 1 FROM identity.local_staff_environment WHERE singleton AND environment=p_environment)
$$;
ALTER FUNCTION identity.local_staff_environment_matches(TEXT) OWNER TO identity_staff_writer;
REVOKE ALL ON FUNCTION identity.local_staff_environment_matches(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_staff_environment_matches(TEXT) TO "${runtimeRole}";
REVOKE CREATE ON SCHEMA identity FROM identity_staff_writer;

-- Standing Owner authority: bounded offline-only read required to seal RESEND
-- without granting direct contact-table SELECT or accepting a caller destination.
GRANT CREATE ON SCHEMA identity TO identity_staff_writer;
CREATE FUNCTION identity.local_support_staff_invitation_destination(p_request UUID) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT s.exact_email INTO destination FROM identity.local_staff_operator_receipt r JOIN identity.local_staff_principal s ON s.account_id=r.account_id
 WHERE r.request_id=p_request AND r.operator_principal=session_user AND r.operation IN ('PROVISION','RESEND') AND s.state='PENDING'
 AND r.xmin::text=pg_current_xact_id()::text;
 IF destination IS NULL THEN RAISE EXCEPTION 'STAFF_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN destination;
END $$;
ALTER FUNCTION identity.local_support_staff_invitation_destination(UUID) OWNER TO identity_staff_writer;
REVOKE ALL ON FUNCTION identity.local_support_staff_invitation_destination(UUID) FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION identity.local_support_staff_invitation_destination(UUID) TO elogistics_local_staff_operator;
REVOKE CREATE ON SCHEMA identity FROM identity_staff_writer;

CREATE FUNCTION identity.guard_local_staff_principal() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF current_user<>'identity_staff_writer' OR TG_OP='DELETE' THEN
  RAISE EXCEPTION 'STAFF_PRINCIPAL_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'PENDING' OR NEW.version<>1 OR NEW.updated_at<>NEW.created_at THEN
   RAISE EXCEPTION 'STAFF_PRINCIPAL_INVALID' USING ERRCODE='23514';END IF;
 ELSE
  IF ROW(NEW.account_id,NEW.exact_email,NEW.purpose,NEW.environment,NEW.provision_request_id,NEW.created_at)
   IS DISTINCT FROM ROW(OLD.account_id,OLD.exact_email,OLD.purpose,OLD.environment,OLD.provision_request_id,OLD.created_at)
   OR OLD.state<>'PENDING' OR NEW.state NOT IN ('ACTIVE','CANCELLED') OR NEW.version<>OLD.version+1 THEN
   RAISE EXCEPTION 'STAFF_PRINCIPAL_IMMUTABLE' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_staff_account() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=OLD.id) THEN
  IF current_user<>'identity_staff_writer' OR TG_OP='DELETE' OR
   ROW(NEW.id,NEW.username,NEW.display_name,NEW.email,NEW.created_at) IS DISTINCT FROM ROW(OLD.id,OLD.username,OLD.display_name,OLD.email,OLD.created_at)
   OR OLD.status<>'PENDING_ACTIVATION' OR NEW.status NOT IN ('ACTIVE','DISABLED') OR NEW.version<>OLD.version+1 OR NEW.auth_version<>OLD.auth_version+1 THEN
   RAISE EXCEPTION 'STAFF_ACCOUNT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_staff_exclusion() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 IF EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=NEW.account_id) THEN
  RAISE EXCEPTION 'STAFF_PURPOSE_EXCLUSIVE' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION merchant.reject_staff_membership() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 IF EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=NEW.account_id) THEN
  RAISE EXCEPTION 'STAFF_PURPOSE_EXCLUSIVE' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.local_staff_evidence_append_only() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP<>'INSERT' OR current_user<>'identity_staff_writer' THEN
  RAISE EXCEPTION 'STAFF_RECEIPT_IMMUTABLE' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_staff_delivery() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.purpose='STAFF_INV' THEN RAISE EXCEPTION 'STAFF_PROOF_IMMUTABLE' USING ERRCODE='42501';END IF;RETURN OLD;
 END IF;
 IF NEW.purpose='STAFF_INV' OR (TG_OP='UPDATE' AND OLD.purpose='STAFF_INV') THEN
  IF TG_TABLE_NAME='action_proof' THEN
   IF current_user<>'identity_staff_writer' OR (TG_OP='UPDATE' AND
    ROW(NEW.id,NEW.account_id,NEW.purpose,NEW.token_hash,NEW.created_at,NEW.expires_at,NEW.verifier_key_id)
    IS DISTINCT FROM ROW(OLD.id,OLD.account_id,OLD.purpose,OLD.token_hash,OLD.created_at,OLD.expires_at,OLD.verifier_key_id)) THEN
    RAISE EXCEPTION 'STAFF_PROOF_WRITE_DENIED' USING ERRCODE='42501';END IF;
   IF TG_OP='UPDATE' AND ((OLD.consumed_at IS NOT NULL AND NEW.consumed_at IS DISTINCT FROM OLD.consumed_at)
    OR (OLD.revoked_at IS NOT NULL AND NEW.revoked_at IS DISTINCT FROM OLD.revoked_at)) THEN
    RAISE EXCEPTION 'STAFF_PROOF_IMMUTABLE' USING ERRCODE='23514';END IF;
  ELSE
   IF (TG_OP='INSERT' AND current_user<>'identity_staff_writer') OR (TG_OP='UPDATE' AND (
    ROW(NEW.id,NEW.proof_id,NEW.account_id,NEW.realm_id,NEW.purpose,NEW.destination,NEW.format_version,NEW.verifier_key_id,NEW.encryption_key_id,NEW.created_at,NEW.expires_at)
    IS DISTINCT FROM ROW(OLD.id,OLD.proof_id,OLD.account_id,OLD.realm_id,OLD.purpose,OLD.destination,OLD.format_version,OLD.verifier_key_id,OLD.encryption_key_id,OLD.created_at,OLD.expires_at)
    OR OLD.status IN ('SENT','FAILED','EXPIRED') OR (NEW.status IN ('PENDING','LEASED') AND ROW(NEW.nonce,NEW.ciphertext) IS DISTINCT FROM ROW(OLD.nonce,OLD.ciphertext)))) THEN
    RAISE EXCEPTION 'STAFF_DELIVERY_WRITE_DENIED' USING ERRCODE='42501';END IF;
  END IF;
 END IF;RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_identity_session_audience() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='UPDATE' AND NEW.audience<>OLD.audience THEN RAISE EXCEPTION 'SESSION_AUDIENCE_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_TABLE_NAME='auth_session' AND TG_OP='INSERT' THEN
  IF (NEW.audience='STAFF' AND NOT identity.support_staff_eligible(NEW.account_id)) OR
   (NEW.audience='SYSTEM' AND identity.support_staff_registered(NEW.account_id)) THEN
   RAISE EXCEPTION 'SESSION_PRINCIPAL_DENIED' USING ERRCODE='23514';END IF;
 END IF;
 IF NEW.audience='STAFF' AND NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_UNAVAILABLE' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.check_local_staff_evidence() RETURNS TRIGGER
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

CREATE TRIGGER local_staff_principal_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.local_staff_principal FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_principal();
CREATE TRIGGER local_staff_account_guard BEFORE UPDATE OR DELETE ON identity.user_account FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_account();
CREATE TRIGGER local_staff_identifier_exclusion BEFORE INSERT OR UPDATE ON identity.customer_identifier FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_exclusion();
CREATE TRIGGER local_staff_grant_exclusion BEFORE INSERT OR UPDATE ON identity.account_grant FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_exclusion();
CREATE TRIGGER local_staff_membership_exclusion BEFORE INSERT OR UPDATE ON merchant.membership FOR EACH ROW EXECUTE FUNCTION merchant.reject_staff_membership();
CREATE TRIGGER local_staff_receipt_immutable BEFORE INSERT OR UPDATE OR DELETE ON identity.local_staff_operator_receipt FOR EACH ROW EXECUTE FUNCTION identity.local_staff_evidence_append_only();
CREATE TRIGGER local_staff_proof_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.action_proof FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_delivery();
CREATE TRIGGER local_staff_delivery_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.invitation_outbox FOR EACH ROW EXECUTE FUNCTION identity.guard_local_staff_delivery();
CREATE TRIGGER identity_session_audience_guard BEFORE INSERT OR UPDATE ON identity.auth_session FOR EACH ROW EXECUTE FUNCTION identity.guard_identity_session_audience();
CREATE TRIGGER identity_csrf_audience_guard BEFORE INSERT OR UPDATE ON identity.csrf_binding FOR EACH ROW EXECUTE FUNCTION identity.guard_identity_session_audience();
CREATE CONSTRAINT TRIGGER staff_principal_evidence AFTER INSERT OR UPDATE ON identity.local_staff_principal DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_staff_evidence();
CREATE CONSTRAINT TRIGGER staff_receipt_evidence AFTER INSERT ON identity.local_staff_operator_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_staff_evidence();
CREATE CONSTRAINT TRIGGER staff_account_evidence AFTER INSERT OR UPDATE ON identity.user_account DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_staff_evidence();
CREATE CONSTRAINT TRIGGER staff_proof_evidence AFTER INSERT OR UPDATE ON identity.action_proof DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_staff_evidence();
CREATE CONSTRAINT TRIGGER staff_delivery_evidence AFTER INSERT OR UPDATE ON identity.invitation_outbox DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_staff_evidence();
REVOKE ALL ON FUNCTION identity.guard_local_staff_principal(),identity.guard_local_staff_account(),identity.guard_local_staff_exclusion(),
 merchant.reject_staff_membership(),identity.local_staff_evidence_append_only(),identity.guard_local_staff_delivery(),
 identity.guard_identity_session_audience(),identity.check_local_staff_evidence() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_staff_environment_matches(),identity.support_staff_registered(UUID),identity.support_staff_eligible(UUID) TO CURRENT_USER;

-- Offline issuance reserves identifiers, then sealing must complete in the same transaction.
GRANT CREATE ON SCHEMA identity TO identity_staff_writer;
CREATE FUNCTION identity.provision_local_support_staff(p_request UUID,p_display_name TEXT,p_email TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_staff_environment; r identity.local_staff_operator_receipt; h BYTEA;
 target UUID; proof UUID; message UUID; audit UUID; stamp TIMESTAMPTZ; principal_state TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
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
 h:=sha256(convert_to(jsonb_build_array('CSKH_IDENTITY_V1',session_user,e.system_identifier,e.database_oid,e.database_name,e.environment,'PROVISION',p_display_name,p_email,p_reason)::text,'UTF8'));
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
 VALUES(target,'staff-'||target,p_display_name,p_email,'PENDING_ACTIVATION',stamp,stamp);
 INSERT INTO identity.local_staff_principal VALUES(target,p_email,'SUPPORT',e.environment,'PENDING',1,p_request,stamp,stamp);
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(audit,target,'STAFF_PROVISIONED','IDENTITY_STAFF',p_reason,p_request::text,stamp);
 INSERT INTO identity.local_staff_operator_receipt VALUES(p_request,'CSKH_IDENTITY_V1',session_user,e.environment,e.system_identifier,e.database_oid,e.database_name,
 'PROVISION',h,target,proof,message,audit,'PENDING_VERIFICATION',p_reason,stamp);
 RETURN QUERY SELECT target,proof,message,stamp,stamp+INTERVAL '24 hours',FALSE,'PENDING'::text;
END $$;

CREATE FUNCTION identity.seal_local_support_staff_invitation(p_request UUID,p_verifier_key_id TEXT,p_encryption_key_id TEXT,p_token_hash BYTEA,p_nonce BYTEA,p_ciphertext BYTEA) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r identity.local_staff_operator_receipt; dest TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_staff_operator','MEMBER') OR NOT identity.local_staff_environment_matches() THEN
  RAISE EXCEPTION 'STAFF_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.local_staff_operator_receipt WHERE request_id=p_request;
 IF NOT FOUND OR r.operator_principal<>session_user OR r.operation NOT IN ('PROVISION','RESEND')
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

CREATE FUNCTION identity.disposition_local_support_staff_invitation(p_request UUID,p_account UUID,p_action TEXT,p_reason TEXT)
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

CREATE FUNCTION identity.accept_local_support_staff_invitation(p_proof UUID,p_key UUID,p_fingerprint BYTEA,p_password_verifier TEXT,p_request UUID) RETURNS VOID
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
 IF p.id IS NULL OR p.revoked_at IS NOT NULL OR p.expires_at<=stamp OR NOT EXISTS(
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

ALTER FUNCTION identity.provision_local_support_staff(UUID,TEXT,TEXT,TEXT) OWNER TO identity_staff_writer;
ALTER FUNCTION identity.seal_local_support_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) OWNER TO identity_staff_writer;
ALTER FUNCTION identity.disposition_local_support_staff_invitation(UUID,UUID,TEXT,TEXT) OWNER TO identity_staff_writer;
ALTER FUNCTION identity.accept_local_support_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) OWNER TO identity_staff_writer;
REVOKE ALL ON FUNCTION identity.provision_local_support_staff(UUID,TEXT,TEXT,TEXT),
 identity.seal_local_support_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA),
 identity.disposition_local_support_staff_invitation(UUID,UUID,TEXT,TEXT),
 identity.accept_local_support_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.provision_local_support_staff(UUID,TEXT,TEXT,TEXT),
 identity.seal_local_support_staff_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA),
 identity.disposition_local_support_staff_invitation(UUID,UUID,TEXT,TEXT) TO elogistics_local_staff_operator;
GRANT EXECUTE ON FUNCTION identity.accept_local_support_staff_invitation(UUID,UUID,BYTEA,TEXT,UUID) TO "${runtimeRole}";
REVOKE CREATE ON SCHEMA identity FROM identity_staff_writer;
