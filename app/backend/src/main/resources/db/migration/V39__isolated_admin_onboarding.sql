-- AON Option B / exact Owner manifest 025ae708...: no CREATE ROLE in Flyway.
-- Dedicated isolated DB binding is an OPERATOR action, never an installation default.
DO $$
DECLARE r RECORD;
BEGIN
 FOR r IN SELECT required.name,p.* FROM
  (VALUES ('elogistics_local_admin_operator'),('identity_admin_writer'),('elogistics_local_realm_operator')) required(name)
  LEFT JOIN pg_catalog.pg_roles p ON p.rolname=required.name LOOP
  IF r.oid IS NULL OR r.rolcanlogin OR r.rolsuper OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication OR r.rolbypassrls THEN
   RAISE EXCEPTION 'AON_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';
  END IF;
 END LOOP;
 IF NOT pg_has_role(current_user,'identity_admin_writer','SET') OR
  pg_has_role('${runtimeRole}','identity_admin_writer','SET') OR
  pg_has_role('${runtimeRole}','elogistics_local_admin_operator','SET') OR
  pg_has_role('${runtimeRole}','elogistics_local_realm_operator','SET') OR
  pg_has_role('elogistics_local_admin_operator','identity_admin_writer','SET') OR
  pg_has_role('elogistics_local_realm_operator','identity_admin_writer','SET') OR
  NOT has_function_privilege('identity_admin_writer','pg_catalog.pg_control_system()','EXECUTE') THEN
  RAISE EXCEPTION 'AON_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';
 END IF;
END $$;

CREATE TABLE identity.local_admin_environment (
 singleton BOOLEAN PRIMARY KEY CHECK(singleton),
 deployment_id UUID NOT NULL UNIQUE,
 realm_id UUID NOT NULL UNIQUE REFERENCES identity.realm(id),
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 purpose VARCHAR(24) NOT NULL CHECK(purpose='ADMIN_ISOLATED_UAT'),
 database_name TEXT NOT NULL, database_oid OID NOT NULL, system_identifier TEXT NOT NULL,
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','DISABLED')),
 bound_request_id UUID NOT NULL UNIQUE, bound_at TIMESTAMPTZ NOT NULL, disabled_at TIMESTAMPTZ,
 CHECK(database_name ~ '^argopost_admin_uat_[a-f0-9]{12}$'),
 CHECK((state='ACTIVE' AND disabled_at IS NULL) OR (state='DISABLED' AND disabled_at>=bound_at))
);
CREATE TABLE identity.local_admin_principal (
 account_id UUID PRIMARY KEY REFERENCES identity.user_account(id),
 realm_id UUID NOT NULL REFERENCES identity.realm(id),
 exact_email VARCHAR(254) COLLATE "C" NOT NULL UNIQUE,
 state VARCHAR(8) NOT NULL CHECK(state IN ('PENDING','ACTIVE','REVOKED')),
 version BIGINT NOT NULL CHECK(version>0),
 provision_request_id UUID NOT NULL UNIQUE, last_request_id UUID NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 FOREIGN KEY(account_id,exact_email) REFERENCES identity.user_account(id,email),
 UNIQUE(account_id,realm_id)
);
CREATE TABLE identity.local_admin_operator_receipt (
 request_id UUID PRIMARY KEY CHECK(request_id<>'00000000-0000-0000-0000-000000000000'),
 deployment_id UUID NOT NULL REFERENCES identity.local_admin_environment(deployment_id) DEFERRABLE INITIALLY DEFERRED,
 realm_id UUID NOT NULL REFERENCES identity.realm(id),
 operator_principal TEXT NOT NULL, operator_oid OID NOT NULL, database_name TEXT NOT NULL,
 operation VARCHAR(24) NOT NULL CHECK(operation IN ('ENV_BIND','ENV_DISABLE','PROVISION','RESEND','CANCEL','SET_PERMISSIONS','REVOKE')),
 input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 account_id UUID REFERENCES identity.user_account(id),
 proof_id UUID REFERENCES identity.action_proof(id) DEFERRABLE INITIALLY DEFERRED,
 message_id UUID REFERENCES identity.invitation_outbox(id) DEFERRABLE INITIALLY DEFERRED,
 original_version BIGINT, committed_version BIGINT,
 committed_outcome VARCHAR(32) NOT NULL CHECK(committed_outcome IN ('BOUND','DISABLED','PENDING_VERIFICATION','RESENT','CANCELLED','PERMISSIONS_UPDATED','REVOKED')),
 audit_id UUID NOT NULL UNIQUE REFERENCES identity.audit_entry(id) DEFERRABLE INITIALLY DEFERRED,
 created_at TIMESTAMPTZ NOT NULL,
 CHECK((operation IN ('ENV_BIND','ENV_DISABLE') AND account_id IS NULL AND proof_id IS NULL AND message_id IS NULL AND original_version IS NULL AND committed_version IS NULL)
  OR (operation IN ('PROVISION','RESEND') AND account_id IS NOT NULL AND proof_id IS NOT NULL AND message_id IS NOT NULL
   AND original_version>=0 AND committed_version=original_version+1)
  OR (operation IN ('CANCEL','SET_PERMISSIONS','REVOKE') AND account_id IS NOT NULL AND proof_id IS NULL AND message_id IS NULL
   AND original_version>0 AND committed_version=original_version+1))
);
ALTER TABLE identity.local_admin_environment ADD FOREIGN KEY(bound_request_id)
 REFERENCES identity.local_admin_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE identity.local_admin_principal ADD FOREIGN KEY(provision_request_id)
 REFERENCES identity.local_admin_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE identity.local_admin_principal ADD FOREIGN KEY(last_request_id)
 REFERENCES identity.local_admin_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;

REVOKE ALL ON identity.local_admin_environment,identity.local_admin_principal,identity.local_admin_operator_receipt
 FROM PUBLIC,"${runtimeRole}",elogistics_local_admin_operator,elogistics_local_realm_operator;
GRANT USAGE ON SCHEMA identity TO identity_admin_writer,elogistics_local_admin_operator,elogistics_local_realm_operator;
-- Needed only for ownership transfer during this transaction, revoked before commit.
GRANT CREATE ON SCHEMA identity TO identity_admin_writer;
GRANT SELECT ON identity.local_admin_environment,identity.local_admin_principal,identity.local_admin_operator_receipt,
 identity.realm,identity.user_account,identity.account_grant,identity.action_proof,identity.invitation_outbox,
 identity.audit_entry,identity.command_receipt,identity.auth_session,identity.refresh_token,
 identity.customer_identifier,identity.customer_enrollment,identity.local_staff_principal TO identity_admin_writer;
GRANT USAGE ON SCHEMA merchant TO identity_admin_writer;
GRANT SELECT ON merchant.membership TO identity_admin_writer;
GRANT INSERT ON identity.local_admin_environment,identity.local_admin_principal,identity.local_admin_operator_receipt,
 identity.user_account,identity.account_grant,identity.action_proof,identity.invitation_outbox,
 identity.audit_entry,identity.command_receipt TO identity_admin_writer;
GRANT UPDATE(state,disabled_at) ON identity.local_admin_environment TO identity_admin_writer;
GRANT UPDATE(state,version,last_request_id,updated_at) ON identity.local_admin_principal TO identity_admin_writer;
GRANT UPDATE(password_hash,status,email_verified,version,auth_version,updated_at,activated_at,disabled_at) ON identity.user_account TO identity_admin_writer;
GRANT UPDATE(consumed_at,revoked_at) ON identity.action_proof TO identity_admin_writer;
GRANT UPDATE(status,nonce,ciphertext,finished_at,lease_id,lease_until,last_outcome) ON identity.invitation_outbox TO identity_admin_writer;
GRANT UPDATE(revoked_at) ON identity.auth_session,identity.refresh_token TO identity_admin_writer;
GRANT DELETE ON identity.account_grant TO identity_admin_writer;

CREATE FUNCTION identity.local_admin_environment_matches(p_environment TEXT) RETURNS BOOLEAN
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 RETURN p_environment IN ('LOCAL','TEST') AND EXISTS(
  SELECT 1 FROM identity.local_admin_environment e,pg_database d,pg_control_system() s
  WHERE e.singleton AND e.state='ACTIVE' AND e.environment=p_environment AND e.purpose='ADMIN_ISOLATED_UAT'
   AND d.datname=current_database() AND d.datname ~ '^argopost_admin_uat_[a-f0-9]{12}$'
   AND e.database_name=d.datname AND e.database_oid=d.oid AND e.system_identifier=s.system_identifier::text);
EXCEPTION WHEN insufficient_privilege THEN RETURN FALSE;
END $$;
CREATE FUNCTION identity.local_admin_registered(p_account UUID) RETURNS BOOLEAN
LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM identity.local_admin_principal WHERE account_id=p_account)
$$;
CREATE FUNCTION identity.local_admin_eligible(p_account UUID,p_realm UUID) RETURNS BOOLEAN
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM identity.local_admin_principal s JOIN identity.local_admin_environment e ON e.realm_id=s.realm_id
  JOIN identity.user_account a ON a.id=s.account_id JOIN identity.realm r ON r.id=s.realm_id
  WHERE s.account_id=p_account AND s.realm_id=p_realm AND s.state='ACTIVE' AND r.status='ACTIVE'
   AND a.status='ACTIVE' AND a.email_verified AND a.password_hash IS NOT NULL AND a.email COLLATE "C"=s.exact_email
   AND r.bootstrap_account_id<>a.id AND identity.local_admin_environment_matches(e.environment))
$$;
-- Apply the approved ACL while the migration role is still effective owner;
-- SET-only NOINHERIT membership cannot grant/revoke after ownership transfer.
REVOKE ALL ON FUNCTION identity.local_admin_environment_matches(TEXT),identity.local_admin_registered(UUID),identity.local_admin_eligible(UUID,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_admin_environment_matches(TEXT) TO "${runtimeRole}",elogistics_local_admin_operator,elogistics_local_realm_operator;
GRANT EXECUTE ON FUNCTION identity.local_admin_registered(UUID),identity.local_admin_eligible(UUID,UUID) TO "${runtimeRole}";
-- AON-ACL-01: boolean purpose exclusion only; no private data or Admin authority.
GRANT EXECUTE ON FUNCTION identity.local_admin_registered(UUID) TO identity_staff_writer;
ALTER FUNCTION identity.local_admin_environment_matches(TEXT) OWNER TO identity_admin_writer;
ALTER FUNCTION identity.local_admin_registered(UUID) OWNER TO identity_admin_writer;
ALTER FUNCTION identity.local_admin_eligible(UUID,UUID) OWNER TO identity_admin_writer;
-- AON-SQL-19 already approves the constrained Merchant writer. ALTER OWNER
-- remaps the old owner's ACL entry, so grant this caller as the actual new owner.
DO $$
DECLARE migration_owner NAME:=current_user; merchant_writer NAME;
BEGIN
 SELECT pg_get_userbyid(nspowner) INTO merchant_writer FROM pg_namespace WHERE nspname='merchant';
 EXECUTE 'SET LOCAL ROLE identity_admin_writer';
 EXECUTE format('GRANT EXECUTE ON FUNCTION identity.local_admin_registered(UUID) TO %I',merchant_writer);
 EXECUTE format('SET LOCAL ROLE %I',migration_owner);
END $$;

CREATE FUNCTION identity.guard_local_admin_environment() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
 IF current_user<>'identity_admin_writer' OR TG_OP='DELETE' THEN RAISE EXCEPTION 'AON_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'ACTIVE' THEN RAISE EXCEPTION 'AON_STATE_INVALID';END IF;
 ELSE
  IF OLD.state<>'ACTIVE' OR NEW.state<>'DISABLED' OR
   ROW(NEW.singleton,NEW.deployment_id,NEW.realm_id,NEW.environment,NEW.purpose,NEW.database_name,NEW.database_oid,NEW.system_identifier,NEW.bound_request_id,NEW.bound_at)
   IS DISTINCT FROM ROW(OLD.singleton,OLD.deployment_id,OLD.realm_id,OLD.environment,OLD.purpose,OLD.database_name,OLD.database_oid,OLD.system_identifier,OLD.bound_request_id,OLD.bound_at)
  THEN RAISE EXCEPTION 'AON_ENVIRONMENT_IMMUTABLE' USING ERRCODE='42501';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_admin_principal() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
 IF current_user<>'identity_admin_writer' OR TG_OP='DELETE' THEN RAISE EXCEPTION 'AON_WRITE_DENIED' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'PENDING' OR NEW.version<>1 THEN RAISE EXCEPTION 'AON_STATE_INVALID';END IF;
 ELSE
  IF OLD.state='REVOKED' OR (OLD.state='ACTIVE' AND NEW.state='PENDING') OR NEW.version<>OLD.version+1 OR
   ROW(NEW.account_id,NEW.realm_id,NEW.exact_email,NEW.provision_request_id,NEW.created_at)
   IS DISTINCT FROM ROW(OLD.account_id,OLD.realm_id,OLD.exact_email,OLD.provision_request_id,OLD.created_at)
  THEN RAISE EXCEPTION 'AON_PRINCIPAL_IMMUTABLE' USING ERRCODE='42501';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_admin_account() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
 IF identity.local_admin_registered(OLD.id) THEN
  IF TG_OP='DELETE' OR current_user<>'identity_admin_writer' THEN RAISE EXCEPTION 'AON_ACCOUNT_WRITE_DENIED' USING ERRCODE='42501';END IF;
  IF ROW(NEW.id,NEW.username,NEW.display_name,NEW.email,NEW.created_at) IS DISTINCT FROM ROW(OLD.id,OLD.username,OLD.display_name,OLD.email,OLD.created_at)
   OR NEW.version<>OLD.version+1 OR NEW.auth_version<>OLD.auth_version+1 THEN RAISE EXCEPTION 'AON_ACCOUNT_IMMUTABLE' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_admin_grant() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE target UUID;
BEGIN
 target:=CASE WHEN TG_OP='DELETE' THEN OLD.account_id ELSE NEW.account_id END;
 IF identity.local_admin_registered(target) OR (TG_OP='UPDATE' AND identity.local_admin_registered(OLD.account_id)) THEN
  IF current_user<>'identity_admin_writer' THEN RAISE EXCEPTION 'AON_GRANT_WRITE_DENIED' USING ERRCODE='42501';END IF;
  IF TG_OP<>'DELETE' AND (NEW.account_id<>target OR NEW.scope_type<>'IDENTITY' OR NEW.role_code NOT IN
   ('ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ')) THEN
   RAISE EXCEPTION 'AON_GRANT_INVALID' USING ERRCODE='42501';END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD;END IF;RETURN NEW;
END $$;
-- Pure exclusion reader: no caller authorization is inferred from its definer.
CREATE FUNCTION identity.guard_local_admin_exclusion() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF identity.local_admin_registered(NEW.account_id) THEN RAISE EXCEPTION 'AON_PURPOSE_EXCLUSION' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_admin_delivery() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
DECLARE target UUID;
BEGIN
 target:=CASE WHEN TG_OP='DELETE' THEN OLD.account_id ELSE NEW.account_id END;
 IF identity.local_admin_registered(target) OR (TG_OP='UPDATE' AND identity.local_admin_registered(OLD.account_id)) THEN
  IF TG_OP='DELETE' THEN RAISE EXCEPTION 'AON_DELIVERY_IMMUTABLE' USING ERRCODE='42501';END IF;
  IF current_user<>'identity_admin_writer' THEN
   IF TG_TABLE_NAME<>'invitation_outbox' OR TG_OP<>'UPDATE' THEN RAISE EXCEPTION 'AON_PROOF_WRITE_DENIED' USING ERRCODE='42501';END IF;
   IF ROW(NEW.id,NEW.proof_id,NEW.account_id,NEW.realm_id,NEW.purpose,NEW.destination,NEW.format_version,NEW.verifier_key_id,NEW.encryption_key_id,NEW.created_at,NEW.expires_at)
    IS DISTINCT FROM ROW(OLD.id,OLD.proof_id,OLD.account_id,OLD.realm_id,OLD.purpose,OLD.destination,OLD.format_version,OLD.verifier_key_id,OLD.encryption_key_id,OLD.created_at,OLD.expires_at)
    OR NEW.nonce IS DISTINCT FROM OLD.nonce AND NEW.nonce IS NOT NULL
    OR NEW.ciphertext IS DISTINCT FROM OLD.ciphertext AND NEW.ciphertext IS NOT NULL THEN
    RAISE EXCEPTION 'AON_DELIVERY_IMMUTABLE' USING ERRCODE='42501';END IF;
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.guard_local_admin_operator_receipt() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP<>'INSERT' OR current_user<>'identity_admin_writer' OR NEW.operator_principal<>session_user OR
  NEW.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) OR NEW.database_name<>current_database() THEN
  RAISE EXCEPTION 'AON_RECEIPT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE FUNCTION identity.check_local_admin_evidence() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE target UUID; p identity.local_admin_principal; a identity.user_account; r identity.local_admin_operator_receipt; e identity.local_admin_environment;
BEGIN
 IF TG_TABLE_NAME='local_admin_environment' THEN
  SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
  IF NOT EXISTS(SELECT 1 FROM identity.local_admin_operator_receipt x JOIN identity.audit_entry u ON u.id=x.audit_id
   WHERE x.request_id=e.bound_request_id AND x.operation='ENV_BIND' AND x.deployment_id=e.deployment_id AND x.realm_id=e.realm_id
    AND x.created_at=e.bound_at AND u.event_type='ADMIN_ENVIRONMENT_BOUND' AND u.source='LOCAL_ADMIN_OPERATOR') THEN
   RAISE EXCEPTION 'AON_ENVIRONMENT_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
  RETURN NULL;
 END IF;
 -- PL/pgSQL resolves record fields while preparing an expression; CASE is not
 -- sufficient when the trigger tables have different record shapes.
 IF TG_TABLE_NAME='user_account' THEN
  target:=NEW.id;
 ELSIF TG_OP='DELETE' THEN
  target:=OLD.account_id;
 ELSE
  target:=NEW.account_id;
 END IF;
 SELECT * INTO p FROM identity.local_admin_principal WHERE account_id=target;
 IF NOT FOUND THEN RETURN NULL;END IF;
 SELECT * INTO a FROM identity.user_account WHERE id=target;
 SELECT * INTO r FROM identity.local_admin_operator_receipt WHERE request_id=p.provision_request_id;
 IF r.account_id IS DISTINCT FROM target OR r.operation<>'PROVISION' OR r.realm_id<>p.realm_id OR r.created_at<>p.created_at
  OR a.created_at<>p.created_at OR a.email COLLATE "C"<>p.exact_email OR a.version<>p.version OR a.auth_version<>p.version
  OR EXISTS(SELECT 1 FROM identity.realm WHERE bootstrap_account_id=target)
  OR EXISTS(SELECT 1 FROM identity.customer_identifier WHERE account_id=target)
  OR EXISTS(SELECT 1 FROM identity.local_staff_principal WHERE account_id=target)
  OR EXISTS(SELECT 1 FROM merchant.membership WHERE account_id=target) THEN
  RAISE EXCEPTION 'AON_PRINCIPAL_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
 IF NOT EXISTS(SELECT 1 FROM identity.audit_entry u WHERE u.id=r.audit_id AND u.target_id=target AND u.source='LOCAL_ADMIN_OPERATOR') THEN
  RAISE EXCEPTION 'AON_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO r FROM identity.local_admin_operator_receipt x WHERE x.request_id=p.last_request_id;
 IF NOT FOUND OR r.account_id IS DISTINCT FROM target OR r.realm_id<>p.realm_id OR NOT EXISTS(
  SELECT 1 FROM identity.audit_entry u WHERE u.id=r.audit_id AND u.target_id=target AND u.source='LOCAL_ADMIN_OPERATOR'
   AND u.request_id=r.request_id::text AND u.occurred_at=r.created_at)
  OR (p.state='PENDING' AND (r.operation NOT IN ('PROVISION','RESEND') OR r.committed_version<>p.version))
  OR (p.state='REVOKED' AND (r.operation NOT IN ('CANCEL','REVOKE') OR r.committed_version<>p.version))
  OR (p.state='ACTIVE' AND NOT ((r.operation='SET_PERMISSIONS' AND r.committed_version=p.version)
    OR (r.operation IN ('PROVISION','RESEND') AND r.committed_version+1=p.version))) THEN
  RAISE EXCEPTION 'AON_CURRENT_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 IF p.state='REVOKED' THEN
  IF a.status<>'DISABLED' OR EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=target) THEN
   RAISE EXCEPTION 'AON_REVOKE_INCOMPLETE' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=target AND role_code='ADMIN_DASHBOARD_READ' AND scope_type='IDENTITY' AND scope_id=p.realm_id)
   OR EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=target AND (scope_type<>'IDENTITY' OR scope_id<>p.realm_id OR role_code NOT IN
    ('ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ'))) THEN
   RAISE EXCEPTION 'AON_EXACT_READ_SET_REQUIRED' USING ERRCODE='23514';END IF;
  IF p.state='PENDING' THEN
   SELECT * INTO r FROM identity.local_admin_operator_receipt WHERE request_id=p.last_request_id;
   IF a.status<>'PENDING_ACTIVATION' OR r.operation NOT IN ('PROVISION','RESEND') OR NOT EXISTS(
    SELECT 1 FROM identity.action_proof f JOIN identity.invitation_outbox o ON o.proof_id=f.id
    WHERE f.id=r.proof_id AND f.account_id=target AND f.purpose='INVITATION' AND f.consumed_at IS NULL AND f.revoked_at IS NULL
     AND f.created_at=r.created_at AND f.expires_at=f.created_at+INTERVAL '24 hours' AND o.id=r.message_id
     AND o.realm_id=p.realm_id AND o.destination COLLATE "C"=p.exact_email AND o.format_version=1) THEN
    RAISE EXCEPTION 'AON_SEALED_INVITATION_REQUIRED' USING ERRCODE='23514';END IF;
  ELSIF p.state='ACTIVE' THEN
   IF a.status<>'ACTIVE' OR NOT a.email_verified OR a.password_hash IS NULL OR NOT EXISTS(
    SELECT 1 FROM identity.command_receipt c JOIN identity.action_proof f ON f.id=c.proof_id
    JOIN identity.audit_entry u ON u.target_id=target AND u.request_id IS NOT NULL
    WHERE c.binding_kind='PROOF' AND c.operation='acceptIdentityInvitation' AND c.target_id=target
     AND f.purpose='INVITATION' AND f.consumed_at IS NOT NULL AND f.revoked_at IS NULL
     AND u.event_type='ADMIN_INVITATION_ACCEPTED' AND u.source='IDENTITY_ADMIN') THEN
    RAISE EXCEPTION 'AON_VERIFICATION_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;
  END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE FUNCTION identity.guard_local_admin_truncate() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'AON_TRUNCATE_DENIED' USING ERRCODE='42501';END $$;
CREATE FUNCTION merchant.reject_local_admin_membership() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF identity.local_admin_registered(NEW.account_id) THEN RAISE EXCEPTION 'AON_MEMBERSHIP_DENIED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;

CREATE TRIGGER local_admin_environment_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.local_admin_environment FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_environment();
CREATE TRIGGER local_admin_principal_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.local_admin_principal FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_principal();
CREATE TRIGGER local_admin_account_guard BEFORE UPDATE OR DELETE ON identity.user_account FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_account();
CREATE TRIGGER local_admin_grant_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.account_grant FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_grant();
CREATE TRIGGER local_admin_customer_exclusion BEFORE INSERT OR UPDATE ON identity.customer_identifier FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_exclusion();
CREATE TRIGGER local_admin_staff_exclusion BEFORE INSERT OR UPDATE ON identity.local_staff_principal FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_exclusion();
CREATE TRIGGER local_admin_membership_exclusion BEFORE INSERT OR UPDATE ON merchant.membership FOR EACH ROW EXECUTE FUNCTION merchant.reject_local_admin_membership();
CREATE TRIGGER local_admin_proof_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.action_proof FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_delivery();
CREATE TRIGGER local_admin_delivery_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.invitation_outbox FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_delivery();
CREATE TRIGGER local_admin_receipt_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.local_admin_operator_receipt FOR EACH ROW EXECUTE FUNCTION identity.guard_local_admin_operator_receipt();
CREATE TRIGGER local_admin_environment_truncate BEFORE TRUNCATE ON identity.local_admin_environment EXECUTE FUNCTION identity.guard_local_admin_truncate();
CREATE TRIGGER local_admin_principal_truncate BEFORE TRUNCATE ON identity.local_admin_principal EXECUTE FUNCTION identity.guard_local_admin_truncate();
CREATE TRIGGER local_admin_receipt_truncate BEFORE TRUNCATE ON identity.local_admin_operator_receipt EXECUTE FUNCTION identity.guard_local_admin_truncate();
CREATE CONSTRAINT TRIGGER local_admin_environment_evidence AFTER INSERT OR UPDATE ON identity.local_admin_environment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_principal_evidence AFTER INSERT OR UPDATE ON identity.local_admin_principal DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_account_evidence AFTER INSERT OR UPDATE ON identity.user_account DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_grant_evidence AFTER INSERT OR UPDATE OR DELETE ON identity.account_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_receipt_evidence AFTER INSERT ON identity.local_admin_operator_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_proof_evidence AFTER INSERT OR UPDATE ON identity.action_proof DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();
CREATE CONSTRAINT TRIGGER local_admin_delivery_evidence AFTER INSERT OR UPDATE ON identity.invitation_outbox DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.check_local_admin_evidence();

REVOKE ALL ON FUNCTION identity.guard_local_admin_environment(),identity.guard_local_admin_principal(),identity.guard_local_admin_account(),
 identity.guard_local_admin_grant(),identity.guard_local_admin_exclusion(),identity.guard_local_admin_delivery(),
 identity.guard_local_admin_operator_receipt(),identity.check_local_admin_evidence(),identity.guard_local_admin_truncate(),merchant.reject_local_admin_membership() FROM PUBLIC;
ALTER FUNCTION identity.guard_local_admin_exclusion() OWNER TO identity_admin_writer;
ALTER FUNCTION identity.check_local_admin_evidence() OWNER TO identity_admin_writer;


-- AON-SQL-01: immutable physical registration; no implicit SYSTEM activation.
CREATE FUNCTION identity.bind_local_admin_environment(p_request UUID,p_realm UUID,p_environment TEXT,p_reason TEXT)
RETURNS TABLE(request_id UUID,realm_id UUID,environment TEXT,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; prior identity.local_admin_operator_receipt; a UUID; stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP; db OID; sys TEXT; h BYTEA;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN
  RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN
  RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));

 IF p_environment IS NULL OR p_environment NOT IN ('LOCAL','TEST') OR current_database() !~ '^argopost_admin_uat_[a-f0-9]{12}$' THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(p_realm::text,40));
 PERFORM 1 FROM identity.realm r WHERE r.id=p_realm AND r.code='SYSTEM';
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;
 SELECT oid INTO db FROM pg_database WHERE datname=current_database();SELECT system_identifier::text INTO sys FROM pg_control_system();
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton FOR UPDATE;
 IF FOUND THEN
  IF e.realm_id<>p_realm OR e.environment<>p_environment OR NOT identity.local_admin_environment_matches(p_environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 ELSE e.deployment_id:=p_request;END IF;
 h:=sha256(convert_to(jsonb_build_object('v',1,'operation','ENV_BIND','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',NULL,'email',NULL,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT p_request,p_realm,e.environment::text,e.state::text,TRUE;RETURN;
 END IF;

 IF EXISTS(SELECT 1 FROM identity.local_admin_environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_ALREADY_BOUND' USING ERRCODE='23514';END IF;
 a:=gen_random_uuid();
 INSERT INTO identity.local_admin_environment VALUES(TRUE,e.deployment_id,p_realm,p_environment,'ADMIN_ISOLATED_UAT',current_database(),db,sys,'ACTIVE',p_request,stamp,NULL);
 INSERT INTO identity.audit_entry(id,event_type,source,reason,request_id,occurred_at) VALUES(a,'ADMIN_ENVIRONMENT_BOUND','LOCAL_ADMIN_OPERATOR','Dedicated isolated environment bound',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),'ENV_BIND',h,'BOUND',a,stamp);
 RETURN QUERY SELECT p_request,p_realm,p_environment,'ACTIVE'::text,FALSE;
END $$;
CREATE FUNCTION identity.disable_local_admin_environment(p_request UUID,p_realm UUID,p_reason TEXT)
RETURNS TABLE(request_id UUID,realm_id UUID,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; prior identity.local_admin_operator_receipt; h BYTEA;a UUID;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN
  RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN
  RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));

 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton AND local_admin_environment.realm_id=p_realm FOR UPDATE;
 IF NOT FOUND OR e.database_name<>current_database() OR e.database_oid<>(SELECT oid FROM pg_database WHERE datname=current_database()) OR e.system_identifier<>(SELECT system_identifier::text FROM pg_control_system()) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 h:=sha256(convert_to(jsonb_build_object('v',1,'operation','ENV_DISABLE','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',p_realm,'targetId',NULL,'expectedVersion',NULL,'displayName',NULL,'email',NULL,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT p_request,p_realm,e.state::text,TRUE;RETURN;
 END IF;
 IF e.state<>'ACTIVE' THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 UPDATE identity.local_admin_environment SET state='DISABLED',disabled_at=stamp WHERE singleton;
 a:=gen_random_uuid();
 INSERT INTO identity.audit_entry(id,event_type,source,reason,request_id,occurred_at) VALUES(a,'ADMIN_ENVIRONMENT_DISABLED','LOCAL_ADMIN_OPERATOR','Dedicated isolated admission disabled',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,p_realm,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),'ENV_DISABLE',h,'DISABLED',a,stamp);
 RETURN QUERY SELECT p_request,p_realm,'DISABLED'::text,FALSE;
END $$;
REVOKE ALL ON FUNCTION identity.bind_local_admin_environment(UUID,UUID,TEXT,TEXT),identity.disable_local_admin_environment(UUID,UUID,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.bind_local_admin_environment(UUID,UUID,TEXT,TEXT),identity.disable_local_admin_environment(UUID,UUID,TEXT) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.bind_local_admin_environment(UUID,UUID,TEXT,TEXT) OWNER TO identity_admin_writer;
ALTER FUNCTION identity.disable_local_admin_environment(UUID,UUID,TEXT) OWNER TO identity_admin_writer;


-- AON-SQL-03/04: reserve then protected seal in one physical transaction.
CREATE FUNCTION identity.provision_local_admin(p_request UUID,p_realm UUID,p_display_name TEXT,p_email TEXT,p_permissions TEXT[],p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; realm_row identity.realm; prior identity.local_admin_operator_receipt; h BYTEA; selected TEXT[]; target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;original BIGINT;op TEXT;event TEXT;outcome TEXT;subject identity.local_admin_principal;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(e.realm_id::text,40));
 SELECT * INTO realm_row FROM identity.realm r WHERE r.id=e.realm_id;
 IF realm_row.status<>'ACTIVE' THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;

 IF p_display_name IS NULL OR length(p_display_name) NOT BETWEEN 1 AND 160 OR p_email IS NULL OR length(p_email) NOT BETWEEN 1 AND 254 OR p_email ~ '[[:space:][:cntrl:]]' OR p_email !~ '^[A-Za-z0-9.!#$%&''*+/=?^_`{|}~-]+@[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?([.][A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?)*$' OR split_part(p_email,'@',1) LIKE '.%' OR split_part(p_email,'@',1) LIKE '%.' OR split_part(p_email,'@',1) LIKE '%..%' THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;

 IF p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 6 OR array_position(p_permissions,NULL) IS NOT NULL THEN RAISE EXCEPTION 'AON_READ_SET_INVALID' USING ERRCODE='22023';END IF;
 SELECT array_agg(x COLLATE "C" ORDER BY x COLLATE "C") INTO selected FROM (SELECT DISTINCT unnest(p_permissions) x) q;
 IF cardinality(selected)<>cardinality(p_permissions) OR NOT ('ADMIN_DASHBOARD_READ'=ANY(selected)) OR NOT selected<@ARRAY['ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ']::text[] THEN RAISE EXCEPTION 'AON_READ_SET_INVALID' USING ERRCODE='22023';END IF;

 IF e.realm_id IS DISTINCT FROM p_realm THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;
 h:=sha256(convert_to(jsonb_build_object('v',1,'operation','PROVISION','deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',e.realm_id,'targetId',NULL,'expectedVersion',NULL,'displayName',p_display_name,'email',p_email,'permissions',selected,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.deployment_id<>e.deployment_id OR prior.realm_id<>e.realm_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;

  RETURN QUERY SELECT prior.account_id,prior.proof_id,prior.message_id,prior.created_at,CASE WHEN prior.proof_id IS NULL THEN NULL::timestamptz ELSE prior.created_at+INTERVAL '24 hours' END,TRUE,(SELECT q.state::text FROM identity.local_admin_principal q WHERE q.account_id=prior.account_id);RETURN;
 END IF;

 IF EXISTS(SELECT 1 FROM identity.user_account WHERE email COLLATE "C"=p_email) OR EXISTS(SELECT 1 FROM identity.customer_enrollment WHERE exact_email COLLATE "C"=p_email AND status='PENDING') THEN RAISE EXCEPTION 'AON_CONTACT_UNAVAILABLE' USING ERRCODE='23514';END IF;
 target:=gen_random_uuid();proof:=gen_random_uuid();message:=gen_random_uuid();original:=0;op:='PROVISION';event:='ADMIN_PROVISIONED_PENDING';outcome:='PENDING_VERIFICATION';
 INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at) VALUES(target,'admin_'||replace(target::text,'-',''),p_display_name,p_email,'PENDING_ACTIVATION',stamp,stamp);
 INSERT INTO identity.local_admin_principal VALUES(target,e.realm_id,p_email,'PENDING',1,p_request,p_request,stamp,stamp);
 INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) SELECT target,x,'IDENTITY',e.realm_id,stamp FROM unnest(selected) x;
 
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,event,'LOCAL_ADMIN_OPERATOR','Request-bound offline Admin operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,e.realm_id,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,original+1,outcome,a,stamp);

 RETURN QUERY SELECT target,proof,message,stamp,stamp+INTERVAL '24 hours',FALSE,'PENDING'::text;
END $$;
CREATE FUNCTION identity.seal_local_admin_invitation(p_request UUID,p_verifier_key_id TEXT,p_encryption_key_id TEXT,p_token_hash BYTEA,p_nonce BYTEA,p_ciphertext BYTEA) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE r identity.local_admin_operator_receipt;subject identity.local_admin_principal;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request AND x.xmin::text=pg_current_xact_id()::text AND x.operation IN ('PROVISION','RESEND') AND x.operator_principal=session_user AND x.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user);
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 SELECT * INTO subject FROM identity.local_admin_principal p WHERE p.account_id=r.account_id AND p.state='PENDING' AND p.last_request_id=p_request;
 IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM identity.local_admin_environment e WHERE e.realm_id=r.realm_id AND identity.local_admin_environment_matches(e.environment)) OR NOT EXISTS(SELECT 1 FROM identity.realm WHERE id=r.realm_id AND status='ACTIVE') THEN RAISE EXCEPTION 'AON_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;
 IF p_verifier_key_id IS NULL OR p_verifier_key_id !~ '^[A-Za-z0-9_-]{1,64}$' OR p_encryption_key_id IS NULL OR p_encryption_key_id !~ '^[A-Za-z0-9_-]{1,64}$' OR p_token_hash IS NULL OR octet_length(p_token_hash)<>32 OR p_nonce IS NULL OR octet_length(p_nonce)<>12 OR p_ciphertext IS NULL OR octet_length(p_ciphertext)<>59 THEN RAISE EXCEPTION 'AON_SEAL_INVALID' USING ERRCODE='22023';END IF;
 INSERT INTO identity.action_proof(id,account_id,purpose,token_hash,verifier_key_id,created_at,expires_at) VALUES(r.proof_id,r.account_id,'INVITATION',p_token_hash,p_verifier_key_id,r.created_at,r.created_at+INTERVAL '24 hours');
 INSERT INTO identity.invitation_outbox(id,proof_id,account_id,realm_id,purpose,destination,format_version,verifier_key_id,encryption_key_id,created_at,expires_at,status,next_attempt_at,nonce,ciphertext)
 VALUES(r.message_id,r.proof_id,r.account_id,r.realm_id,'INVITATION',subject.exact_email,1,p_verifier_key_id,p_encryption_key_id,r.created_at,r.created_at+INTERVAL '24 hours','PENDING',r.created_at,p_nonce,p_ciphertext);
END $$;
CREATE FUNCTION identity.local_admin_invitation_destination(p_request UUID) RETURNS TEXT
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE destination TEXT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT s.exact_email INTO destination FROM identity.local_admin_operator_receipt r JOIN identity.local_admin_principal s ON s.account_id=r.account_id JOIN identity.local_admin_environment e ON e.deployment_id=r.deployment_id
 WHERE r.request_id=p_request AND r.operator_principal=session_user AND r.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user) AND r.operation IN ('PROVISION','RESEND') AND r.xmin::text=pg_current_xact_id()::text AND s.state='PENDING' AND s.last_request_id=p_request AND identity.local_admin_environment_matches(e.environment);
 IF destination IS NULL THEN RAISE EXCEPTION 'AON_RESERVATION_REQUIRED' USING ERRCODE='42501';END IF;RETURN destination;
END $$;

CREATE FUNCTION identity.disposition_local_admin_invitation(p_request UUID,p_account UUID,p_expected_version BIGINT,p_action TEXT,p_reason TEXT)
RETURNS TABLE(account_id UUID,proof_id UUID,message_id UUID,created_at TIMESTAMPTZ,expires_at TIMESTAMPTZ,replayed BOOLEAN,current_principal_state TEXT)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; realm_row identity.realm; prior identity.local_admin_operator_receipt; h BYTEA; selected TEXT[]; target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;original BIGINT;op TEXT;event TEXT;outcome TEXT;subject identity.local_admin_principal;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(e.realm_id::text,40));
 SELECT * INTO realm_row FROM identity.realm r WHERE r.id=e.realm_id;
 IF realm_row.status<>'ACTIVE' THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;

 SELECT * INTO subject FROM identity.local_admin_principal p WHERE p.account_id=p_account AND p.realm_id=e.realm_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_TARGET_DENIED' USING ERRCODE='42501';END IF;
 PERFORM 1 FROM identity.user_account WHERE id=p_account FOR UPDATE;target:=p_account;
IF p_action IS NULL OR p_action NOT IN ('RESEND','CANCEL') THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;op:=p_action;h:=sha256(convert_to(jsonb_build_object('v',1,'operation',op,'deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',e.realm_id,'targetId',p_account,'expectedVersion',p_expected_version,'displayName',NULL,'email',NULL,'permissions',NULL,'action',p_action,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.deployment_id<>e.deployment_id OR prior.realm_id<>e.realm_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;

  RETURN QUERY SELECT prior.account_id,prior.proof_id,prior.message_id,prior.created_at,CASE WHEN prior.proof_id IS NULL THEN NULL::timestamptz ELSE prior.created_at+INTERVAL '24 hours' END,TRUE,(SELECT q.state::text FROM identity.local_admin_principal q WHERE q.account_id=prior.account_id);RETURN;
 END IF;

 IF p_expected_version IS NULL OR p_expected_version<1 OR subject.version<>p_expected_version THEN RAISE EXCEPTION 'AON_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
 
 IF subject.state<>'PENDING' THEN RAISE EXCEPTION 'AON_TARGET_STATE_INVALID' USING ERRCODE='23514';END IF;
 IF op='RESEND' AND ((SELECT count(*) FROM identity.local_admin_operator_receipt r WHERE r.account_id=target AND r.operation='RESEND' AND r.created_at>stamp-INTERVAL '1 hour')>=3 OR (SELECT count(*) FROM identity.local_admin_operator_receipt r WHERE r.operator_oid=(SELECT oid FROM pg_roles WHERE rolname=session_user) AND r.operation='RESEND' AND r.created_at>stamp-INTERVAL '1 hour')>=20) THEN RAISE EXCEPTION 'AON_RESEND_THROTTLED' USING ERRCODE='23514';END IF;
 original:=subject.version;

 UPDATE identity.action_proof SET revoked_at=stamp WHERE action_proof.account_id=target AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE invitation_outbox.account_id=target AND status IN ('PENDING','LEASED');
 UPDATE identity.auth_session SET revoked_at=stamp WHERE auth_session.account_id=target AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT id FROM identity.auth_session WHERE auth_session.account_id=target);

 IF op='RESEND' THEN
  proof:=gen_random_uuid();message:=gen_random_uuid();event:='ADMIN_INVITATION_RESENT';outcome:='RESENT';
 ELSE
  DELETE FROM identity.account_grant WHERE account_grant.account_id=target;
  event:='ADMIN_INVITATION_CANCELLED';outcome:='CANCELLED';
 END IF;
 UPDATE identity.user_account SET status=CASE WHEN op='CANCEL' THEN 'DISABLED' ELSE status END,disabled_at=CASE WHEN op='CANCEL' THEN stamp ELSE disabled_at END,version=version+1,auth_version=auth_version+1,updated_at=stamp WHERE id=target;
 UPDATE identity.local_admin_principal SET state=CASE WHEN op='CANCEL' THEN 'REVOKED' ELSE state END,version=version+1,last_request_id=p_request,updated_at=stamp WHERE local_admin_principal.account_id=target;
 
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,event,'LOCAL_ADMIN_OPERATOR','Request-bound offline Admin operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,e.realm_id,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,original+1,outcome,a,stamp);
RETURN QUERY SELECT target,proof,message,stamp,CASE WHEN proof IS NULL THEN NULL::timestamptz ELSE stamp+INTERVAL '24 hours' END,FALSE,CASE WHEN op='CANCEL' THEN 'REVOKED'::text ELSE 'PENDING'::text END;
END $$;

CREATE FUNCTION identity.configure_local_admin_permissions(p_request UUID,p_account UUID,p_expected_version BIGINT,p_permissions TEXT[],p_reason TEXT)
RETURNS TABLE(request_id UUID,account_id UUID,committed_version BIGINT,current_version BIGINT,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; realm_row identity.realm; prior identity.local_admin_operator_receipt; h BYTEA; selected TEXT[]; target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;original BIGINT;op TEXT;event TEXT;outcome TEXT;subject identity.local_admin_principal;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(e.realm_id::text,40));
 SELECT * INTO realm_row FROM identity.realm r WHERE r.id=e.realm_id;
 IF realm_row.status<>'ACTIVE' THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;

 SELECT * INTO subject FROM identity.local_admin_principal p WHERE p.account_id=p_account AND p.realm_id=e.realm_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_TARGET_DENIED' USING ERRCODE='42501';END IF;
 PERFORM 1 FROM identity.user_account WHERE id=p_account FOR UPDATE;target:=p_account;

 IF p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 6 OR array_position(p_permissions,NULL) IS NOT NULL THEN RAISE EXCEPTION 'AON_READ_SET_INVALID' USING ERRCODE='22023';END IF;
 SELECT array_agg(x COLLATE "C" ORDER BY x COLLATE "C") INTO selected FROM (SELECT DISTINCT unnest(p_permissions) x) q;
 IF cardinality(selected)<>cardinality(p_permissions) OR NOT ('ADMIN_DASHBOARD_READ'=ANY(selected)) OR NOT selected<@ARRAY['ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ']::text[] THEN RAISE EXCEPTION 'AON_READ_SET_INVALID' USING ERRCODE='22023';END IF;
op:='SET_PERMISSIONS';h:=sha256(convert_to(jsonb_build_object('v',1,'operation',op,'deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',e.realm_id,'targetId',p_account,'expectedVersion',p_expected_version,'displayName',NULL,'email',NULL,'permissions',selected,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.deployment_id<>e.deployment_id OR prior.realm_id<>e.realm_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
RETURN QUERY SELECT p_request,target,prior.committed_version,subject.version,subject.state::text,TRUE;RETURN;END IF;
 IF p_expected_version IS NULL OR p_expected_version<1 OR subject.version<>p_expected_version THEN RAISE EXCEPTION 'AON_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
 IF subject.state<>'ACTIVE' THEN RAISE EXCEPTION 'AON_TARGET_STATE_INVALID' USING ERRCODE='23514';END IF;
original:=subject.version;

 UPDATE identity.action_proof SET revoked_at=stamp WHERE action_proof.account_id=target AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE invitation_outbox.account_id=target AND status IN ('PENDING','LEASED');
 UPDATE identity.auth_session SET revoked_at=stamp WHERE auth_session.account_id=target AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT id FROM identity.auth_session WHERE auth_session.account_id=target);

 DELETE FROM identity.account_grant WHERE account_grant.account_id=target;
 INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at) SELECT target,x,'IDENTITY',e.realm_id,stamp FROM unnest(selected) x;
 UPDATE identity.user_account SET version=version+1,auth_version=auth_version+1,updated_at=stamp WHERE id=target;
 UPDATE identity.local_admin_principal SET version=version+1,last_request_id=p_request,updated_at=stamp WHERE local_admin_principal.account_id=target;
 event:='ADMIN_PERMISSIONS_UPDATED';outcome:='PERMISSIONS_UPDATED';
 
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,event,'LOCAL_ADMIN_OPERATOR','Request-bound offline Admin operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,e.realm_id,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,original+1,outcome,a,stamp);
RETURN QUERY SELECT p_request,target,original+1,original+1,CASE WHEN op='REVOKE' THEN 'REVOKED'::text ELSE 'ACTIVE'::text END,FALSE;
END $$;

CREATE FUNCTION identity.revoke_local_admin(p_request UUID,p_account UUID,p_expected_version BIGINT,p_reason TEXT)
RETURNS TABLE(request_id UUID,account_id UUID,committed_version BIGINT,current_version BIGINT,current_state TEXT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment; realm_row identity.realm; prior identity.local_admin_operator_receipt; h BYTEA; selected TEXT[]; target UUID;proof UUID;message UUID;a UUID:=gen_random_uuid();stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;original BIGINT;op TEXT;event TEXT;outcome TEXT;subject identity.local_admin_principal;
BEGIN

 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 10 AND 500 THEN RAISE EXCEPTION 'AON_INPUT_INVALID' USING ERRCODE='22023';END IF;
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreatedb OR rolcreaterole OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_request::text,39));
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(e.realm_id::text,40));
 SELECT * INTO realm_row FROM identity.realm r WHERE r.id=e.realm_id;
 IF realm_row.status<>'ACTIVE' THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;

 SELECT * INTO subject FROM identity.local_admin_principal p WHERE p.account_id=p_account AND p.realm_id=e.realm_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_TARGET_DENIED' USING ERRCODE='42501';END IF;
 PERFORM 1 FROM identity.user_account WHERE id=p_account FOR UPDATE;target:=p_account;
op:='REVOKE';h:=sha256(convert_to(jsonb_build_object('v',1,'operation',op,'deploymentId',e.deployment_id,'databaseName',current_database(),'realmId',e.realm_id,'targetId',p_account,'expectedVersion',p_expected_version,'displayName',NULL,'email',NULL,'permissions',NULL,'action',NULL,'reason',p_reason)::text,'UTF8'));
 SELECT * INTO prior FROM identity.local_admin_operator_receipt x WHERE x.request_id=p_request;
 IF FOUND THEN
  IF prior.operator_principal<>session_user OR prior.operator_oid<>(SELECT oid FROM pg_roles WHERE rolname=session_user) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
  IF prior.input_digest<>h OR prior.deployment_id<>e.deployment_id OR prior.realm_id<>e.realm_id THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
RETURN QUERY SELECT p_request,target,prior.committed_version,subject.version,subject.state::text,TRUE;RETURN;END IF;
 IF p_expected_version IS NULL OR p_expected_version<1 OR subject.version<>p_expected_version THEN RAISE EXCEPTION 'AON_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
 IF subject.state='REVOKED' THEN RAISE EXCEPTION 'AON_TARGET_STATE_INVALID' USING ERRCODE='23514';END IF;
original:=subject.version;

 UPDATE identity.action_proof SET revoked_at=stamp WHERE action_proof.account_id=target AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE invitation_outbox.account_id=target AND status IN ('PENDING','LEASED');
 UPDATE identity.auth_session SET revoked_at=stamp WHERE auth_session.account_id=target AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT id FROM identity.auth_session WHERE auth_session.account_id=target);

 DELETE FROM identity.account_grant WHERE account_grant.account_id=target;
 UPDATE identity.user_account SET status='DISABLED',disabled_at=stamp,version=version+1,auth_version=auth_version+1,updated_at=stamp WHERE id=target;
 UPDATE identity.local_admin_principal SET state='REVOKED',version=version+1,last_request_id=p_request,updated_at=stamp WHERE local_admin_principal.account_id=target;
 event:='ADMIN_REVOKED';outcome:='REVOKED';
 
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(a,target,event,'LOCAL_ADMIN_OPERATOR','Request-bound offline Admin operation',p_request::text,stamp);
 INSERT INTO identity.local_admin_operator_receipt(request_id,deployment_id,realm_id,operator_principal,operator_oid,database_name,operation,input_digest,account_id,proof_id,message_id,original_version,committed_version,committed_outcome,audit_id,created_at)
 VALUES(p_request,e.deployment_id,e.realm_id,session_user,(SELECT oid FROM pg_roles WHERE rolname=session_user),current_database(),op,h,target,proof,message,original,original+1,outcome,a,stamp);
RETURN QUERY SELECT p_request,target,original+1,original+1,CASE WHEN op='REVOKE' THEN 'REVOKED'::text ELSE 'ACTIVE'::text END,FALSE;
END $$;

CREATE FUNCTION identity.local_admin_operator_status(p_account UUID)
RETURNS TABLE(account_id UUID,realm_id UUID,current_state TEXT,current_version BIGINT,current_permissions TEXT[])
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE e identity.local_admin_environment;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_admin_operator','MEMBER') OR EXISTS(SELECT 1 FROM pg_roles WHERE rolname=session_user AND (rolsuper OR rolcreaterole OR rolcreatedb OR rolbypassrls)) THEN RAISE EXCEPTION 'AON_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 RETURN QUERY SELECT p.account_id,p.realm_id,p.state::text,p.version,ARRAY(SELECT g.role_code::text FROM identity.account_grant g WHERE g.account_id=p.account_id ORDER BY g.role_code COLLATE "C") FROM identity.local_admin_principal p WHERE p.account_id=p_account AND p.realm_id=e.realm_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_TARGET_DENIED' USING ERRCODE='42501';END IF;
END $$;
REVOKE ALL ON FUNCTION identity.provision_local_admin(UUID,UUID,TEXT,TEXT,TEXT[],TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.provision_local_admin(UUID,UUID,TEXT,TEXT,TEXT[],TEXT) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.provision_local_admin(UUID,UUID,TEXT,TEXT,TEXT[],TEXT) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.seal_local_admin_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.seal_local_admin_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.seal_local_admin_invitation(UUID,TEXT,TEXT,BYTEA,BYTEA,BYTEA) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.local_admin_invitation_destination(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_admin_invitation_destination(UUID) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.local_admin_invitation_destination(UUID) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.disposition_local_admin_invitation(UUID,UUID,BIGINT,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.disposition_local_admin_invitation(UUID,UUID,BIGINT,TEXT,TEXT) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.disposition_local_admin_invitation(UUID,UUID,BIGINT,TEXT,TEXT) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.configure_local_admin_permissions(UUID,UUID,BIGINT,TEXT[],TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.configure_local_admin_permissions(UUID,UUID,BIGINT,TEXT[],TEXT) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.configure_local_admin_permissions(UUID,UUID,BIGINT,TEXT[],TEXT) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.revoke_local_admin(UUID,UUID,BIGINT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.revoke_local_admin(UUID,UUID,BIGINT,TEXT) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.revoke_local_admin(UUID,UUID,BIGINT,TEXT) OWNER TO identity_admin_writer;
REVOKE ALL ON FUNCTION identity.local_admin_operator_status(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.local_admin_operator_status(UUID) TO elogistics_local_admin_operator;
ALTER FUNCTION identity.local_admin_operator_status(UUID) OWNER TO identity_admin_writer;




-- AON-SQL-09: possession/password verification is performed by the canonical
-- Identity application; this function owns the relational atomic transition.
CREATE FUNCTION identity.accept_local_admin_invitation(p_proof UUID,p_key UUID,p_fingerprint BYTEA,p_password_verifier TEXT,p_request UUID) RETURNS VOID
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE subject identity.local_admin_principal;proof identity.action_proof;a identity.user_account;e identity.local_admin_environment;prior identity.command_receipt;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;
BEGIN
 IF p_proof IS NULL OR p_key IS NULL OR p_key='00000000-0000-0000-0000-000000000000' OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_fingerprint IS NULL OR octet_length(p_fingerprint)<>32 OR p_password_verifier IS NULL OR p_password_verifier !~ '^\$2[aby]\$12\$[./A-Za-z0-9]{53}$' THEN RAISE EXCEPTION 'AON_ACCEPT_INVALID' USING ERRCODE='22023';END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);PERFORM pg_advisory_xact_lock(hashtextextended(p_key::text,39));
 SELECT s.* INTO subject FROM identity.local_admin_principal s JOIN identity.action_proof f ON f.account_id=s.account_id WHERE f.id=p_proof;
 IF NOT FOUND THEN RAISE EXCEPTION 'AON_PROOF_INVALID' USING ERRCODE='42501';END IF;
 SELECT * INTO e FROM identity.local_admin_environment WHERE singleton AND realm_id=subject.realm_id;
 IF NOT FOUND OR NOT identity.local_admin_environment_matches(e.environment) THEN RAISE EXCEPTION 'AON_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(subject.realm_id::text,40));
 IF NOT EXISTS(SELECT 1 FROM identity.realm WHERE id=subject.realm_id AND status='ACTIVE' AND bootstrap_account_id<>subject.account_id) THEN RAISE EXCEPTION 'AON_REALM_DENIED' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM identity.user_account WHERE id=subject.account_id FOR UPDATE;
 SELECT * INTO proof FROM identity.action_proof WHERE id=p_proof FOR UPDATE;
 SELECT * INTO prior FROM identity.command_receipt c WHERE c.binding_kind='PROOF' AND c.proof_id=p_proof AND c.operation='acceptIdentityInvitation' AND c.operation_key=p_key;
 IF FOUND THEN
  IF prior.request_fingerprint<>p_fingerprint THEN RAISE EXCEPTION 'AON_REQUEST_CONFLICT' USING ERRCODE='23514';END IF;
  IF prior.target_id<>a.id OR prior.result_version<>a.version OR prior.expires_at<=stamp OR proof.consumed_at IS NULL OR proof.revoked_at IS NOT NULL OR NOT identity.local_admin_eligible(a.id,subject.realm_id) THEN RAISE EXCEPTION 'AON_ACCEPT_STATE_INVALID' USING ERRCODE='42501';END IF;
  RETURN;
 END IF;
 IF subject.state<>'PENDING' OR a.status<>'PENDING_ACTIVATION' OR a.email_verified OR a.password_hash IS NOT NULL OR proof.purpose<>'INVITATION' OR proof.consumed_at IS NOT NULL OR proof.revoked_at IS NOT NULL OR stamp<proof.created_at OR stamp>=proof.expires_at OR proof.expires_at<>proof.created_at+INTERVAL '24 hours' OR NOT EXISTS(SELECT 1 FROM identity.invitation_outbox o WHERE o.proof_id=proof.id AND o.account_id=a.id AND o.realm_id=subject.realm_id AND o.destination COLLATE "C"=subject.exact_email AND o.format_version=1) THEN RAISE EXCEPTION 'AON_PROOF_INVALID' USING ERRCODE='42501';END IF;
 UPDATE identity.user_account SET status='ACTIVE',email_verified=TRUE,password_hash=p_password_verifier,activated_at=stamp,updated_at=stamp,version=version+1,auth_version=auth_version+1 WHERE id=a.id;
 UPDATE identity.local_admin_principal SET state='ACTIVE',version=version+1,updated_at=stamp WHERE account_id=a.id;
 UPDATE identity.action_proof SET consumed_at=stamp WHERE id=proof.id;
 UPDATE identity.auth_session SET revoked_at=stamp WHERE account_id=a.id AND revoked_at IS NULL;
 UPDATE identity.refresh_token SET revoked_at=stamp WHERE revoked_at IS NULL AND session_id IN(SELECT id FROM identity.auth_session WHERE account_id=a.id);
 UPDATE identity.invitation_outbox SET status='EXPIRED',last_outcome='NOT_ELIGIBLE',nonce=NULL,ciphertext=NULL,lease_id=NULL,lease_until=NULL,finished_at=stamp WHERE proof_id=proof.id AND status IN ('PENDING','LEASED');
 INSERT INTO identity.audit_entry(id,target_id,event_type,source,reason,request_id,occurred_at) VALUES(gen_random_uuid(),a.id,'ADMIN_INVITATION_ACCEPTED','IDENTITY_ADMIN','Verified dedicated Admin invitation',p_request::text,stamp);
 INSERT INTO identity.command_receipt(binding_kind,proof_id,operation,operation_key,request_fingerprint,target_id,original_version,result_version,result_status,created_at,expires_at)
 VALUES('PROOF',proof.id,'acceptIdentityInvitation',p_key,p_fingerprint,a.id,a.version,a.version+1,204,stamp,stamp+INTERVAL '24 hours');
END $$;
REVOKE ALL ON FUNCTION identity.accept_local_admin_invitation(UUID,UUID,BYTEA,TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.accept_local_admin_invitation(UUID,UUID,BYTEA,TEXT,UUID) TO "${runtimeRole}";
ALTER FUNCTION identity.accept_local_admin_invitation(UUID,UUID,BYTEA,TEXT,UUID) OWNER TO identity_admin_writer;

REVOKE CREATE ON SCHEMA identity FROM identity_admin_writer;
