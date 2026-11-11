-- Accepted Business preparation only. No Shipment, rate, payment or Carrier effects.
CREATE FUNCTION merchant.valid_facility_contact(c JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE a JSONB; field TEXT;
BEGIN
 IF c IS NULL OR jsonb_typeof(c)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(c))<>3 OR NOT c ?& ARRAY['name','phone','address']
 OR jsonb_typeof(c->'name')<>'string' OR length(btrim(c->>'name')) NOT BETWEEN 1 AND 160 OR c->>'name' ~ '[[:cntrl:]]'
 OR jsonb_typeof(c->'phone')<>'string' OR c->>'phone' !~ '^\+84[35789][0-9]{8}$' THEN RETURN FALSE; END IF;
 a:=c->'address';
 IF jsonb_typeof(a)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(a))<>4 OR NOT a ?& ARRAY['country','administrativeArea','locality','addressLine'] OR a->>'country'<>'VN' THEN RETURN FALSE; END IF;
 FOREACH field IN ARRAY ARRAY['administrativeArea','locality','addressLine'] LOOP
 IF jsonb_typeof(a->field)<>'string' OR length(btrim(a->>field)) NOT BETWEEN 1 AND (CASE WHEN field='addressLine' THEN 500 ELSE 160 END) OR a->>field ~ '[[:cntrl:]]' THEN RETURN FALSE; END IF;
 END LOOP; RETURN TRUE;
END $$;
CREATE TABLE merchant.facility (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 name VARCHAR(160) NOT NULL CHECK(length(btrim(name))>0 AND name !~ '[[:cntrl:]]'),
 contact JSONB NOT NULL CHECK(merchant.valid_facility_contact(contact) IS TRUE),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','ARCHIVED')), version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), UNIQUE(id,organization_id)
);
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage'));
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_scope_check;
ALTER TABLE merchant.member_grant ADD COLUMN facility_id UUID;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK(
 (scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND permission LIKE 'shipment.%'));
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_facility_fk FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id);
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_membership_id_permission_key;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_scope_key UNIQUE NULLS NOT DISTINCT(membership_id,permission,facility_id);
CREATE FUNCTION merchant.check_preparation_grants() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID;
BEGIN
 org:=CASE WHEN TG_OP='DELETE' THEN OLD.organization_id ELSE NEW.organization_id END;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission LIKE 'shipment.%'
 AND g.permission<>'shipment.draft.read' AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r
 WHERE r.membership_id=g.membership_id AND r.permission='shipment.draft.read'
 AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id)))
 THEN RAISE EXCEPTION 'PREPARATION_READ_PERMISSION_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER preparation_grant_invariants AFTER INSERT OR DELETE ON merchant.member_grant
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_preparation_grants();
CREATE TABLE orders.organization_preparation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, facility_id UUID, creator_account_id UUID NOT NULL,
 state VARCHAR(8) NOT NULL CHECK(state IN ('DRAFT','ARCHIVED','BOOKED')),
 preparation_state VARCHAR(18) NOT NULL CHECK(preparation_state IN ('DRAFT','READY_TO_PACK','PACKED','READY_TO_HANDOVER')),
 version BIGINT NOT NULL CHECK(version>0), source_channel VARCHAR(40), source_order_reference VARCHAR(80),
 payload JSONB NOT NULL CHECK(orders.valid_shipping_preparation(payload)),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), UNIQUE(id,organization_id),
 CHECK((source_channel IS NULL)=(source_order_reference IS NULL)),
 CHECK(source_channel IS NULL OR (length(btrim(source_channel))>0 AND source_channel !~ '[[:cntrl:]]')),
 CHECK(source_order_reference IS NULL OR (length(btrim(source_order_reference))>0 AND source_order_reference !~ '[[:cntrl:]]'))
);
-- Organization and facility are admitted through Merchant public API in the same transaction.
-- PostgreSQL also binds preparation references to authoritative Merchant keys.
ALTER TABLE orders.organization_preparation ADD CONSTRAINT organization_preparation_org_fk FOREIGN KEY(organization_id) REFERENCES merchant.organization(id);
ALTER TABLE orders.organization_preparation ADD CONSTRAINT organization_preparation_facility_fk FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id);
CREATE UNIQUE INDEX organization_preparation_source_key ON orders.organization_preparation(organization_id,source_channel,source_order_reference) WHERE source_channel IS NOT NULL;
CREATE INDEX organization_preparation_listing ON orders.organization_preparation(organization_id,updated_at DESC,id) WHERE state='DRAFT';
CREATE TABLE orders.organization_preparation_version (
 id UUID NOT NULL, organization_id UUID NOT NULL, version BIGINT NOT NULL, snapshot JSONB NOT NULL,
 recorded_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(id,version), UNIQUE(id,organization_id,version),
 FOREIGN KEY(id,organization_id) REFERENCES orders.organization_preparation(id,organization_id)
);
ALTER TABLE orders.organization_preparation ADD CONSTRAINT organization_preparation_current_fk FOREIGN KEY(id,organization_id,version)
 REFERENCES orders.organization_preparation_version(id,organization_id,version) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE orders.organization_preparation_receipt (
 organization_id UUID NOT NULL, operation VARCHAR(40) NOT NULL, operation_key UUID NOT NULL, actor_account_id UUID NOT NULL,
 facility_id UUID, target_id UUID NOT NULL, original_version BIGINT NOT NULL CHECK(original_version>=0), result_version BIGINT NOT NULL,
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), recorded_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,operation,operation_key),
 FOREIGN KEY(target_id,organization_id,result_version) REFERENCES orders.organization_preparation_version(id,organization_id,version)
);
CREATE TABLE orders.organization_preparation_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, target_id UUID NOT NULL, operation VARCHAR(40) NOT NULL,
 prior_version BIGINT NOT NULL CHECK(prior_version>=0), new_version BIGINT NOT NULL CHECK(new_version=prior_version+1), prior_state VARCHAR(18), new_state VARCHAR(18) NOT NULL,
 request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, UNIQUE(target_id,new_version),
 FOREIGN KEY(target_id,organization_id,new_version) REFERENCES orders.organization_preparation_version(id,organization_id,version)
);
CREATE FUNCTION orders.guard_organization_preparation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'PREPARATION_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.state<>'DRAFT' OR NEW.preparation_state<>'DRAFT' THEN RAISE EXCEPTION 'PREPARATION_INITIAL_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.creator_account_id<>OLD.creator_account_id
  OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
  OR NEW.source_channel IS DISTINCT FROM OLD.source_channel OR NEW.source_order_reference IS DISTINCT FROM OLD.source_order_reference
  OR OLD.state<>'DRAFT' OR NEW.state='BOOKED'
  THEN RAISE EXCEPTION 'PREPARATION_IMMUTABLE_BINDING' USING ERRCODE='23514'; END IF;
  IF (NEW.payload<>OLD.payload OR NEW.facility_id IS DISTINCT FROM OLD.facility_id) AND NEW.preparation_state<>'DRAFT'
  THEN RAISE EXCEPTION 'PREPARATION_PACKING_INVALIDATED' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_preparation_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_preparation FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_preparation();
CREATE FUNCTION orders.check_organization_preparation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM orders.organization_preparation_version v WHERE v.id=NEW.id AND v.organization_id=NEW.organization_id
 AND v.version=NEW.version AND (v.snapshot->>'id')::uuid=NEW.id AND (v.snapshot->>'organizationId')::uuid=NEW.organization_id
 AND (v.snapshot->>'version')::bigint=NEW.version AND (v.snapshot->>'createdAt')::timestamptz=NEW.created_at AND (v.snapshot->>'updatedAt')::timestamptz=NEW.updated_at
 AND v.snapshot->>'state'=NEW.state AND v.snapshot->>'preparationState'=NEW.preparation_state
 AND v.snapshot->'input'=NEW.payload AND (v.snapshot->>'facilityId')::uuid IS NOT DISTINCT FROM NEW.facility_id
 AND (v.snapshot->>'creatorAccountId')::uuid=NEW.creator_account_id
 AND v.snapshot->>'sourceChannel' IS NOT DISTINCT FROM NEW.source_channel
 AND v.snapshot->>'sourceOrderReference' IS NOT DISTINCT FROM NEW.source_order_reference)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_audit a WHERE a.target_id=NEW.id AND a.organization_id=NEW.organization_id AND a.new_version=NEW.version)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_receipt r WHERE r.target_id=NEW.id AND r.organization_id=NEW.organization_id AND r.result_version=NEW.version)
 THEN RAISE EXCEPTION 'PREPARATION_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_preparation_evidence AFTER INSERT OR UPDATE ON orders.organization_preparation
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_preparation();
CREATE TRIGGER organization_preparation_version_append BEFORE UPDATE OR DELETE ON orders.organization_preparation_version FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER organization_preparation_receipt_append BEFORE UPDATE OR DELETE ON orders.organization_preparation_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER organization_preparation_audit_append BEFORE UPDATE OR DELETE ON orders.organization_preparation_audit FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
REVOKE ALL ON merchant.facility,orders.organization_preparation,orders.organization_preparation_version,orders.organization_preparation_receipt,orders.organization_preparation_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT ON merchant.facility TO "${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.organization_preparation TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.organization_preparation_version,orders.organization_preparation_receipt,orders.organization_preparation_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.valid_facility_contact(JSONB),merchant.check_preparation_grants(),orders.guard_organization_preparation(),orders.check_organization_preparation() FROM PUBLIC,"${runtimeRole}";

-- Extend the original audited operator command without changing its digest/signature.
CREATE OR REPLACE FUNCTION merchant.configure_local_workspace(
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
 OR cardinality(p_permissions)>13 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage'))
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
   SELECT gen_random_uuid(),member,p_org,p,'ORGANIZATION' FROM unnest(permissions) p ON CONFLICT DO NOTHING;
 END IF;
 INSERT INTO merchant.workspace_audit VALUES(gen_random_uuid(),p_org,member,session_user,p_action,p_request,p_reason,stamp);
 INSERT INTO merchant.workspace_command_receipt VALUES(p_request,digest,p_org,member,p_action,stamp); RETURN member;
END $$;

GRANT EXECUTE ON FUNCTION merchant.valid_facility_contact(JSONB) TO "${runtimeRole}";
