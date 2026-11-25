-- Approved P0-ID, source/test default-off. No historical rows, flags or grants are activated.
CREATE TABLE identity.admin_governance_reauthentication (
 id UUID PRIMARY KEY,
 account_id UUID NOT NULL,
 realm_id UUID NOT NULL REFERENCES identity.realm(id),
 session_id UUID NOT NULL,
 auth_version BIGINT NOT NULL CHECK(auth_version>0),
 authority_version CHAR(64) NOT NULL CHECK(authority_version ~ '^[0-9a-f]{64}$'),
 operation VARCHAR(32) NOT NULL CHECK(operation IN ('APPROVE','REJECT','SUSPEND','REINSTATE')),
 target_id UUID NOT NULL,
 expected_version BIGINT NOT NULL CHECK(expected_version>0),
 command_digest BYTEA NOT NULL CHECK(octet_length(command_digest)=32),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at=created_at+INTERVAL '15 minutes'),
 consumed_at TIMESTAMPTZ,
 consumed_request_id UUID,
 FOREIGN KEY(session_id,account_id) REFERENCES identity.auth_session(id,account_id),
 CHECK((consumed_at IS NULL)=(consumed_request_id IS NULL)),
 CHECK(consumed_at IS NULL OR (consumed_at>=created_at AND consumed_at<expires_at))
);
CREATE TABLE identity.business_contact_attestation (
 id UUID PRIMARY KEY,
 proof_id UUID NOT NULL UNIQUE REFERENCES identity.customer_proof(id),
 account_id UUID NOT NULL REFERENCES identity.user_account(id),
 application_binding UUID NOT NULL,
 address_digest BYTEA NOT NULL CHECK(octet_length(address_digest)=32),
 protected_address BYTEA NOT NULL CHECK(octet_length(protected_address) BETWEEN 31 AND 1024),
 key_id VARCHAR(64) NOT NULL CHECK(key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 verified_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at>verified_at),
 used_revision BIGINT CHECK(used_revision>0),
 used_at TIMESTAMPTZ,
 revoked_at TIMESTAMPTZ,
 CHECK((used_revision IS NULL)=(used_at IS NULL)),
 CHECK(used_at IS NULL OR (used_at>=verified_at AND used_at<expires_at)),
 CHECK(revoked_at IS NULL OR revoked_at>=verified_at)
);
CREATE TABLE identity.governance_proof_receipt (
 actor_id UUID NOT NULL,
 operation VARCHAR(40) NOT NULL CHECK(operation IN ('REQUEST_ADMIN_STEPUP','REQUEST_BUSINESS_CONTACT','CONFIRM_BUSINESS_CONTACT')),
 operation_key UUID NOT NULL CHECK(operation_key<>'00000000-0000-0000-0000-000000000000'),
 system_session_id UUID,
 customer_session_id UUID,
 auth_version BIGINT NOT NULL CHECK(auth_version>0),
 target_id UUID NOT NULL,
 input_key_id VARCHAR(64) NOT NULL CHECK(input_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),
 input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 protected_address BYTEA,
 CONSTRAINT ck_governance_pending_address CHECK((operation='REQUEST_BUSINESS_CONTACT' AND protected_address IS NOT NULL AND octet_length(protected_address) BETWEEN 34 AND 512) OR (operation<>'REQUEST_BUSINESS_CONTACT' AND protected_address IS NULL)),
 system_proof_id UUID REFERENCES identity.admin_governance_reauthentication(id),
 customer_proof_id UUID REFERENCES identity.customer_proof(id),
 attestation_id UUID REFERENCES identity.business_contact_attestation(id),
 created_at TIMESTAMPTZ NOT NULL,
 expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at>=created_at+INTERVAL '7 days'),
 PRIMARY KEY(actor_id,operation,operation_key),
 FOREIGN KEY(system_session_id,actor_id) REFERENCES identity.auth_session(id,account_id),
 FOREIGN KEY(customer_session_id,actor_id) REFERENCES identity.customer_auth_session(id,account_id),
 CHECK((operation='REQUEST_ADMIN_STEPUP' AND system_session_id IS NOT NULL AND system_proof_id IS NOT NULL
         AND customer_session_id IS NULL AND customer_proof_id IS NULL AND attestation_id IS NULL)
    OR (operation='REQUEST_BUSINESS_CONTACT' AND system_session_id IS NULL AND system_proof_id IS NULL
         AND customer_session_id IS NOT NULL AND customer_proof_id IS NOT NULL AND attestation_id IS NULL)
    OR (operation='CONFIRM_BUSINESS_CONTACT' AND system_session_id IS NULL AND system_proof_id IS NULL
         AND customer_session_id IS NOT NULL AND customer_proof_id IS NOT NULL AND attestation_id IS NOT NULL))
);
-- A purpose proof has one durable request binding; another key cannot rebind it to another application.
CREATE UNIQUE INDEX governance_admin_proof_request ON identity.governance_proof_receipt(system_proof_id) WHERE operation='REQUEST_ADMIN_STEPUP';
CREATE UNIQUE INDEX governance_contact_proof_request ON identity.governance_proof_receipt(customer_proof_id) WHERE operation='REQUEST_BUSINESS_CONTACT';
CREATE UNIQUE INDEX governance_contact_confirmation ON identity.governance_proof_receipt(attestation_id) WHERE operation='CONFIRM_BUSINESS_CONTACT';
CREATE INDEX admin_governance_proof_session ON identity.admin_governance_reauthentication(session_id,id);
CREATE INDEX business_contact_application ON identity.business_contact_attestation(account_id,application_binding);

-- Private trigger implementation: application verifies credentials/possession; DB owns structural binding.
CREATE FUNCTION identity.guard_governance_proof_rows() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE p identity.customer_proof; s identity.admin_governance_reauthentication; r identity.governance_proof_receipt; stamp TIMESTAMPTZ:=clock_timestamp();
BEGIN
 IF TG_OP IN ('DELETE','TRUNCATE') THEN RAISE EXCEPTION 'GOVERNANCE_PROOF_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF NOT identity.local_admin_environment_matches('LOCAL') AND NOT identity.local_admin_environment_matches('TEST') THEN RAISE EXCEPTION 'GOVERNANCE_PHYSICAL_ENVIRONMENT_DENIED' USING ERRCODE='42501';END IF;
 IF TG_TABLE_NAME='admin_governance_reauthentication' THEN
  IF TG_OP='UPDATE' AND (to_jsonb(NEW)-ARRAY['consumed_at','consumed_request_id']<>to_jsonb(OLD)-ARRAY['consumed_at','consumed_request_id']
    OR OLD.consumed_at IS NOT NULL OR NEW.consumed_at IS NULL OR NEW.consumed_request_id IS NULL) THEN
   RAISE EXCEPTION 'GOVERNANCE_PROOF_IMMUTABLE' USING ERRCODE='23514';END IF;
  IF NOT identity.local_admin_eligible(NEW.account_id,NEW.realm_id)
   OR EXISTS(SELECT 1 FROM identity.customer_identifier WHERE account_id=NEW.account_id)
   OR identity.support_staff_registered(NEW.account_id)
   OR NOT EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=NEW.account_id AND role_code='ADMIN_DASHBOARD_READ' AND scope_type='IDENTITY' AND scope_id=NEW.realm_id)
   OR NOT EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=NEW.account_id AND role_code='ADMIN_MERCHANT_READ' AND scope_type='IDENTITY' AND scope_id=NEW.realm_id)
   OR EXISTS(SELECT 1 FROM identity.account_grant WHERE account_id=NEW.account_id AND (role_code NOT IN ('ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ') OR scope_type<>'IDENTITY' OR scope_id<>NEW.realm_id))
   OR NOT EXISTS(SELECT 1 FROM identity.auth_session a JOIN identity.user_account u ON u.id=a.account_id
    WHERE a.id=NEW.session_id AND a.account_id=NEW.account_id AND a.audience='SYSTEM' AND a.revoked_at IS NULL
     AND a.auth_version=NEW.auth_version AND u.auth_version=a.auth_version AND u.status='ACTIVE' AND u.email_verified
     AND a.created_at<=NEW.created_at AND a.absolute_expires_at>stamp AND a.idle_expires_at>stamp)
   OR NEW.created_at>stamp OR NEW.expires_at<=stamp THEN RAISE EXCEPTION 'GOVERNANCE_PROOF_ADMISSION_DENIED' USING ERRCODE='42501';END IF;
 ELSIF TG_TABLE_NAME='business_contact_attestation' THEN
  IF TG_OP='UPDATE' AND (to_jsonb(NEW)-ARRAY['used_revision','used_at','revoked_at']<>to_jsonb(OLD)-ARRAY['used_revision','used_at','revoked_at']
    OR OLD.revoked_at IS NOT NULL OR (OLD.used_at IS NOT NULL AND (NEW.used_at IS DISTINCT FROM OLD.used_at OR NEW.used_revision IS DISTINCT FROM OLD.used_revision))
    OR (NEW.used_at IS NOT NULL AND OLD.used_at IS NULL AND NEW.used_at>=NEW.expires_at)) THEN
   RAISE EXCEPTION 'BUSINESS_CONTACT_ATTESTATION_IMMUTABLE' USING ERRCODE='23514';END IF;
  SELECT * INTO p FROM identity.customer_proof WHERE id=NEW.proof_id;
  IF NOT FOUND OR p.purpose<>'BUSINESS_CONTACT' OR p.subject_id<>NEW.account_id OR p.consumed_at IS NULL OR p.revoked_at IS NOT NULL
   OR NEW.verified_at<>p.consumed_at OR NEW.expires_at<>p.expires_at
   OR NOT EXISTS(SELECT 1 FROM identity.governance_proof_receipt WHERE actor_id=NEW.account_id AND operation='REQUEST_BUSINESS_CONTACT'
     AND customer_proof_id=p.id AND target_id=NEW.application_binding AND customer_session_id=p.bound_session_id AND auth_version=p.bound_auth_version)
   THEN RAISE EXCEPTION 'BUSINESS_CONTACT_PROOF_BINDING_DENIED' USING ERRCODE='42501';END IF;
  -- Historical verified facts remain readable after TTL. A new use still requires live current security/session.
  IF TG_OP='INSERT' OR (OLD.used_at IS NULL AND NEW.used_at IS NOT NULL) THEN
   IF stamp>=NEW.expires_at OR NOT EXISTS(SELECT 1 FROM identity.customer_auth_session c JOIN identity.user_account u ON u.id=c.account_id
     JOIN identity.customer_identifier i ON i.account_id=u.id AND i.status='ACTIVE' AND i.exact_email=u.email COLLATE "C"
     WHERE c.id=p.bound_session_id AND c.account_id=NEW.account_id AND c.auth_version=p.bound_auth_version AND c.auth_version=u.auth_version
      AND c.revoked_at IS NULL AND c.absolute_expires_at>stamp AND c.idle_expires_at>stamp AND u.status='ACTIVE' AND u.email_verified)
    THEN RAISE EXCEPTION 'BUSINESS_CONTACT_ADMISSION_DENIED' USING ERRCODE='42501';END IF;
  END IF;
 ELSE
  IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'GOVERNANCE_RECEIPT_IMMUTABLE' USING ERRCODE='23514';END IF;
  IF NEW.operation='REQUEST_ADMIN_STEPUP' THEN
   SELECT * INTO s FROM identity.admin_governance_reauthentication WHERE id=NEW.system_proof_id;
   IF NOT FOUND OR s.account_id<>NEW.actor_id OR s.session_id<>NEW.system_session_id OR s.auth_version<>NEW.auth_version OR s.target_id<>NEW.target_id
    THEN RAISE EXCEPTION 'GOVERNANCE_RECEIPT_BINDING_DENIED' USING ERRCODE='23514';END IF;
  ELSE
   SELECT * INTO p FROM identity.customer_proof WHERE id=NEW.customer_proof_id;
   IF NOT FOUND OR p.purpose<>'BUSINESS_CONTACT' OR p.subject_id<>NEW.actor_id OR p.bound_session_id<>NEW.customer_session_id OR p.bound_auth_version<>NEW.auth_version
    THEN RAISE EXCEPTION 'GOVERNANCE_RECEIPT_BINDING_DENIED' USING ERRCODE='23514';END IF;
   IF NEW.operation='CONFIRM_BUSINESS_CONTACT' AND NOT EXISTS(SELECT 1 FROM identity.business_contact_attestation a
     WHERE a.id=NEW.attestation_id AND a.proof_id=p.id AND a.account_id=NEW.actor_id AND a.application_binding=NEW.target_id)
    THEN RAISE EXCEPTION 'GOVERNANCE_RECEIPT_ATTESTATION_DENIED' USING ERRCODE='23514';END IF;
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER admin_governance_proof_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.admin_governance_reauthentication FOR EACH ROW EXECUTE FUNCTION identity.guard_governance_proof_rows();
CREATE TRIGGER business_contact_attestation_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.business_contact_attestation FOR EACH ROW EXECUTE FUNCTION identity.guard_governance_proof_rows();
CREATE TRIGGER governance_proof_receipt_guard BEFORE INSERT OR UPDATE OR DELETE ON identity.governance_proof_receipt FOR EACH ROW EXECUTE FUNCTION identity.guard_governance_proof_rows();

-- A proof/attestation cannot commit without its exact immutable request receipt.
CREATE FUNCTION identity.require_governance_proof_receipt() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='admin_governance_reauthentication' THEN
  IF NOT EXISTS(SELECT 1 FROM identity.governance_proof_receipt r WHERE r.operation='REQUEST_ADMIN_STEPUP' AND r.system_proof_id=NEW.id
    AND r.actor_id=NEW.account_id AND r.system_session_id=NEW.session_id AND r.auth_version=NEW.auth_version AND r.target_id=NEW.target_id)
   THEN RAISE EXCEPTION 'GOVERNANCE_PROOF_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
  IF NOT EXISTS(SELECT 1 FROM identity.audit_entry a WHERE a.id=NEW.id AND a.actor_id=NEW.account_id AND a.target_id=NEW.account_id AND a.event_type='ADMIN_GOVERNANCE_REAUTHENTICATED')
   OR (NEW.consumed_at IS NOT NULL AND NOT EXISTS(SELECT 1 FROM identity.audit_entry a WHERE a.id=NEW.consumed_request_id
    AND a.actor_id=NEW.account_id AND a.target_id=NEW.account_id AND a.event_type='ADMIN_GOVERNANCE_PROOF_CONSUMED'))
   THEN RAISE EXCEPTION 'GOVERNANCE_PROOF_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM identity.governance_proof_receipt r WHERE r.operation='CONFIRM_BUSINESS_CONTACT' AND r.attestation_id=NEW.id
    AND r.actor_id=NEW.account_id AND r.customer_proof_id=NEW.proof_id AND r.target_id=NEW.application_binding)
   THEN RAISE EXCEPTION 'CONTACT_ATTESTATION_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
  IF NOT EXISTS(SELECT 1 FROM identity.audit_entry a WHERE a.id=NEW.id AND a.actor_id=NEW.account_id AND a.target_id=NEW.account_id AND a.event_type='BUSINESS_CONTACT_VERIFIED')
   THEN RAISE EXCEPTION 'CONTACT_ATTESTATION_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER admin_governance_proof_receipt_required AFTER INSERT OR UPDATE ON identity.admin_governance_reauthentication
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.require_governance_proof_receipt();
CREATE CONSTRAINT TRIGGER business_contact_receipt_required AFTER INSERT ON identity.business_contact_attestation
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION identity.require_governance_proof_receipt();

CREATE FUNCTION identity.admin_governance_command_bound(p_proof UUID,p_request UUID,p_actor UUID,p_authority_version TEXT,p_operation TEXT,p_target UUID,p_expected_version BIGINT,p_digest BYTEA)
RETURNS BOOLEAN LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM identity.admin_governance_reauthentication p
  JOIN identity.auth_session s ON s.id=p.session_id AND s.account_id=p.account_id
  JOIN identity.user_account a ON a.id=p.account_id
  WHERE p.id=p_proof AND p.consumed_request_id=p_request AND p.consumed_at IS NOT NULL AND p.account_id=p_actor
   AND p.authority_version=p_authority_version AND p.operation=p_operation AND p.target_id=p_target
   AND p.expected_version=p_expected_version AND p.command_digest=p_digest AND p.expires_at>clock_timestamp()
   AND s.audience='SYSTEM' AND s.revoked_at IS NULL AND s.auth_version=p.auth_version AND a.auth_version=p.auth_version
   AND s.absolute_expires_at>clock_timestamp() AND s.idle_expires_at>clock_timestamp()
   AND identity.local_admin_eligible(p_actor,p.realm_id)
   AND NOT EXISTS(SELECT 1 FROM identity.customer_identifier WHERE account_id=p_actor)
   AND NOT identity.support_staff_registered(p_actor)
   AND EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=p_actor AND g.role_code='ADMIN_DASHBOARD_READ' AND g.scope_type='IDENTITY' AND g.scope_id=p.realm_id)
   AND EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=p_actor AND g.role_code='ADMIN_MERCHANT_READ' AND g.scope_type='IDENTITY' AND g.scope_id=p.realm_id)
   AND NOT EXISTS(SELECT 1 FROM identity.account_grant g WHERE g.account_id=p_actor AND (g.role_code NOT IN ('ADMIN_DASHBOARD_READ','ADMIN_MERCHANT_READ','ADMIN_NETWORK_READ','ADMIN_SHIPMENT_READ','ADMIN_FINANCE_READ','ADMIN_TECHNICAL_READ') OR g.scope_type<>'IDENTITY' OR g.scope_id<>p.realm_id)))
$$;
REVOKE ALL ON identity.admin_governance_reauthentication,identity.business_contact_attestation,identity.governance_proof_receipt FROM PUBLIC;
GRANT SELECT,INSERT ON identity.admin_governance_reauthentication,identity.business_contact_attestation,identity.governance_proof_receipt TO "${runtimeRole}";
GRANT UPDATE(consumed_at,consumed_request_id) ON identity.admin_governance_reauthentication TO "${runtimeRole}";
GRANT UPDATE(used_revision,used_at,revoked_at) ON identity.business_contact_attestation TO "${runtimeRole}";
GRANT SELECT ON identity.admin_governance_reauthentication TO identity_admin_writer;
-- Existing NOINHERIT owner-transfer protocol; only a boolean purpose predicate,
-- no Staff table SELECT, membership or Staff execution authority.
DO $$
DECLARE migration_owner NAME:=current_user;
BEGIN
 EXECUTE 'SET LOCAL ROLE identity_staff_writer';
 EXECUTE 'GRANT EXECUTE ON FUNCTION identity.support_staff_registered(UUID) TO identity_admin_writer';
 EXECUTE format('SET LOCAL ROLE %I',migration_owner);
END $$;
REVOKE ALL ON FUNCTION identity.guard_governance_proof_rows(),identity.require_governance_proof_receipt(),identity.admin_governance_command_bound(UUID,UUID,UUID,TEXT,TEXT,UUID,BIGINT,BYTEA) FROM PUBLIC,"${runtimeRole}";
GRANT USAGE ON SCHEMA identity TO merchant_governance_writer;
GRANT EXECUTE ON FUNCTION identity.admin_governance_command_bound(UUID,UUID,UUID,TEXT,TEXT,UUID,BIGINT,BYTEA) TO merchant_governance_writer;
GRANT CREATE ON SCHEMA identity TO identity_admin_writer;
ALTER FUNCTION identity.admin_governance_command_bound(UUID,UUID,UUID,TEXT,TEXT,UUID,BIGINT,BYTEA) OWNER TO identity_admin_writer;
REVOKE CREATE ON SCHEMA identity FROM identity_admin_writer;
