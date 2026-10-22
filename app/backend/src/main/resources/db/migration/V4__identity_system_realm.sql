-- Approved REALM-P1/P2. Explicit operator-selected UUID; never infer it from grants.
-- Runs transactionally as the existing NOLOGIN schema owner through Flyway.
CREATE TABLE identity.realm (
 id UUID PRIMARY KEY,
 code VARCHAR(16) NOT NULL UNIQUE CHECK (code = 'SYSTEM'),
 status VARCHAR(8) NOT NULL CHECK (status IN ('INACTIVE','ACTIVE')),
 registered_at TIMESTAMPTZ NOT NULL,
 updated_at TIMESTAMPTZ NOT NULL,
 bootstrap_account_id UUID UNIQUE REFERENCES identity.user_account(id),
 activated_at TIMESTAMPTZ,
 CONSTRAINT ck_identity_realm_clock CHECK (updated_at >= registered_at AND
   (activated_at IS NULL OR (activated_at >= registered_at AND activated_at <= updated_at))),
 CONSTRAINT ck_identity_realm_activation CHECK
   (status <> 'ACTIVE' OR (activated_at IS NOT NULL AND bootstrap_account_id IS NOT NULL))
);
INSERT INTO identity.realm(id,code,status,registered_at,updated_at)
VALUES ('${identitySystemRealmId}'::uuid,'SYSTEM','INACTIVE',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
DO $realm_preflight$
BEGIN
 IF EXISTS (SELECT 1 FROM identity.account_grant
   WHERE scope_type='IDENTITY' AND scope_id <> '${identitySystemRealmId}'::uuid) THEN
   RAISE EXCEPTION 'IDENTITY_REALM_MAPPING_REVIEW_REQUIRED';
 END IF;
END;
$realm_preflight$;
ALTER TABLE identity.account_grant ADD COLUMN identity_realm_id UUID
 GENERATED ALWAYS AS (CASE WHEN scope_type='IDENTITY' THEN scope_id ELSE NULL::uuid END) STORED;
ALTER TABLE identity.account_grant ADD CONSTRAINT fk_identity_grant_realm
 FOREIGN KEY (identity_realm_id) REFERENCES identity.realm(id) ON UPDATE RESTRICT ON DELETE RESTRICT;
REVOKE ALL ON TABLE identity.realm FROM PUBLIC;
GRANT SELECT ON TABLE identity.realm TO "${runtimeRole}";

-- SECURITY INVOKER, owner-only, explicit arguments, no credential or default administrator.
-- Creates ONLY a new pending account. Invitation delivery/consumption remains a separate flow.
CREATE FUNCTION identity.bootstrap_system_realm_admin(
 p_realm UUID, p_account UUID, p_username TEXT, p_display_name TEXT,
 p_email TEXT, p_request UUID
) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER SET search_path = pg_catalog, identity AS $bootstrap$
DECLARE r identity.realm%ROWTYPE; stamp TIMESTAMPTZ := CURRENT_TIMESTAMP;
BEGIN
 IF current_user <> pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='identity')) THEN
   RAISE EXCEPTION 'IDENTITY_BOOTSTRAP_OWNER_REQUIRED' USING ERRCODE='42501';
 END IF;
 IF p_realm IS NULL OR p_account IS NULL OR p_request IS NULL THEN
   RAISE EXCEPTION 'IDENTITY_BOOTSTRAP_INPUT_REQUIRED';
 END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.realm WHERE id=p_realm FOR UPDATE;
 IF NOT FOUND OR r.status <> 'INACTIVE' OR r.bootstrap_account_id IS NOT NULL OR r.activated_at IS NOT NULL THEN
   RAISE EXCEPTION 'IDENTITY_BOOTSTRAP_NOT_AVAILABLE';
 END IF;
 -- INSERT, never upsert/update an arbitrary existing account. V3 validates all account fields.
 INSERT INTO identity.user_account(id,username,display_name,email,status,created_at,updated_at)
 VALUES(p_account,p_username,p_display_name,p_email,'PENDING_ACTIVATION',stamp,stamp);
 INSERT INTO identity.account_grant(account_id,role_code,scope_type,scope_id,granted_at)
 VALUES(p_account,'IDENTITY_MANAGEMENT','IDENTITY',p_realm,stamp);
 UPDATE identity.realm SET bootstrap_account_id=p_account,updated_at=stamp WHERE id=p_realm;
 INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(p_request,NULL,p_account,'IDENTITY_REALM_BOOTSTRAP_PENDING','OWNER_BOOTSTRAP',
   'Operator principal: '||session_user,p_request::text,stamp);
 RETURN p_account;
END;
$bootstrap$;

-- Activation cannot verify email or set a password. It requires the separately completed
-- accepted invitation flow on the exact newly bootstrapped account, including consumed proof.
CREATE FUNCTION identity.activate_system_realm(p_realm UUID,p_account UUID,p_request UUID)
RETURNS VOID LANGUAGE plpgsql SECURITY INVOKER SET search_path = pg_catalog, identity AS $activation$
DECLARE r identity.realm%ROWTYPE; a identity.user_account%ROWTYPE; stamp TIMESTAMPTZ := CURRENT_TIMESTAMP;
BEGIN
 IF current_user <> pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='identity')) THEN
   RAISE EXCEPTION 'IDENTITY_BOOTSTRAP_OWNER_REQUIRED' USING ERRCODE='42501';
 END IF;
 IF p_realm IS NULL OR p_account IS NULL OR p_request IS NULL THEN
   RAISE EXCEPTION 'IDENTITY_BOOTSTRAP_INPUT_REQUIRED';
 END IF;
 PERFORM pg_advisory_xact_lock(5279420865908921433);
 SELECT * INTO r FROM identity.realm WHERE id=p_realm FOR UPDATE;
 IF NOT FOUND OR r.status <> 'INACTIVE' OR r.activated_at IS NOT NULL
   OR r.bootstrap_account_id IS DISTINCT FROM p_account THEN
   RAISE EXCEPTION 'IDENTITY_REALM_ACTIVATION_NOT_AVAILABLE';
 END IF;
 SELECT * INTO a FROM identity.user_account WHERE id=p_account FOR UPDATE;
 IF NOT FOUND OR a.status <> 'ACTIVE' OR NOT a.email_verified OR a.password_hash IS NULL
   OR a.activated_at IS NULL OR a.activated_at < r.registered_at
   OR NOT EXISTS (SELECT 1 FROM identity.action_proof WHERE account_id=p_account
     AND purpose='INVITATION' AND consumed_at IS NOT NULL AND revoked_at IS NULL
     AND created_at >= r.registered_at AND consumed_at <= stamp)
   OR NOT EXISTS (SELECT 1 FROM identity.account_grant WHERE account_id=p_account
     AND role_code='IDENTITY_MANAGEMENT' AND scope_type='IDENTITY' AND scope_id=p_realm)
   OR EXISTS (SELECT 1 FROM identity.account_grant WHERE account_id=p_account
     AND (role_code <> 'IDENTITY_MANAGEMENT' OR scope_type <> 'IDENTITY' OR scope_id <> p_realm)) THEN
   RAISE EXCEPTION 'IDENTITY_REALM_ACTIVATION_PREREQUISITES_REQUIRED';
 END IF;
 UPDATE identity.realm SET status='ACTIVE',activated_at=stamp,updated_at=stamp WHERE id=p_realm;
 INSERT INTO identity.audit_entry(id,actor_id,target_id,event_type,source,reason,request_id,occurred_at)
 VALUES(p_request,NULL,p_account,'IDENTITY_REALM_ACTIVATED','OWNER_BOOTSTRAP',
   'Operator principal: '||session_user,p_request::text,stamp);
END;
$activation$;
REVOKE ALL ON FUNCTION identity.bootstrap_system_realm_admin(UUID,UUID,TEXT,TEXT,TEXT,UUID) FROM PUBLIC;
REVOKE ALL ON FUNCTION identity.activate_system_realm(UUID,UUID,UUID) FROM PUBLIC;
