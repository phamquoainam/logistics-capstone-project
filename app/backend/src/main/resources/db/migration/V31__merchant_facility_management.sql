-- Owner accepted WFP V1. Forward-only; no grants, fixtures or runtime activation.
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read',
 'contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond'));
ALTER TABLE merchant.member_grant DROP CONSTRAINT preparation_scope_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK(
 (scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND
 permission IN ('shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare',
 'shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign',
 'shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export',
 'shipment.actions.manage','shipment.actions.respond','facility.read','facility.manage')));

CREATE FUNCTION merchant.valid_facility_hours(h JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE d JSONB; i JSONB; days TEXT[]:=ARRAY[]::TEXT[]; prior INT; opens INT; closes INT;
BEGIN
 IF h IS NULL THEN RETURN TRUE; END IF;
 IF jsonb_typeof(h)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(h))<>2 OR h->>'timeZone' IS DISTINCT FROM 'Asia/Ho_Chi_Minh'
 OR NOT h ?& ARRAY['timeZone','days'] OR jsonb_typeof(h->'days')<>'array' OR jsonb_array_length(h->'days')<>7 THEN RETURN FALSE; END IF;
 FOR d IN SELECT value FROM jsonb_array_elements(h->'days') LOOP
  IF jsonb_typeof(d)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(d))<>2 OR NOT d ?& ARRAY['day','intervals']
   OR jsonb_typeof(d->'day')<>'string' OR NOT (d->>'day'=ANY(ARRAY['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']))
   OR d->>'day'=ANY(days) OR jsonb_typeof(d->'intervals')<>'array' OR jsonb_array_length(d->'intervals')>2 THEN RETURN FALSE; END IF;
  days:=array_append(days,d->>'day');prior:=0;
  FOR i IN SELECT value FROM jsonb_array_elements(d->'intervals') LOOP
   IF jsonb_typeof(i)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(i))<>2 OR NOT i ?& ARRAY['opensAt','closesAt']
    OR jsonb_typeof(i->'opensAt')<>'string' OR jsonb_typeof(i->'closesAt')<>'string'
    OR i->>'opensAt' !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$' OR i->>'closesAt' !~ '^(([01][0-9]|2[0-3]):[0-5][0-9]|24:00)$' THEN RETURN FALSE; END IF;
   opens:=split_part(i->>'opensAt',':',1)::INT*60+split_part(i->>'opensAt',':',2)::INT;
   closes:=split_part(i->>'closesAt',':',1)::INT*60+split_part(i->>'closesAt',':',2)::INT;
   IF opens>=closes OR opens<prior THEN RETURN FALSE; END IF;prior:=closes;
  END LOOP;
 END LOOP;RETURN TRUE;
END $$;
ALTER TABLE merchant.facility ADD COLUMN code VARCHAR(40), ADD COLUMN kind VARCHAR(16), ADD COLUMN sender_contact JSONB,
 ADD COLUMN return_contact JSONB, ADD COLUMN operating_hours JSONB,
 ADD CONSTRAINT facility_code_shape CHECK(code IS NULL OR code ~ '^[A-Z0-9_-]{1,40}$'),
 ADD CONSTRAINT facility_kind_shape CHECK(kind IS NULL OR kind IN ('WAREHOUSE','STORE','PICKUP_ADDRESS','SENDER_POINT')),
 ADD CONSTRAINT facility_metadata_pair CHECK((code IS NULL)=(kind IS NULL)),
 ADD CONSTRAINT facility_sender_contact_shape CHECK(sender_contact IS NULL OR merchant.valid_facility_contact(sender_contact) IS TRUE),
 ADD CONSTRAINT facility_return_contact_shape CHECK(return_contact IS NULL OR merchant.valid_facility_contact(return_contact) IS TRUE),
 ADD CONSTRAINT facility_hours_shape CHECK(merchant.valid_facility_hours(operating_hours) IS TRUE),
 ADD CONSTRAINT facility_code_unique UNIQUE(organization_id,code);
CREATE INDEX facility_directory ON merchant.facility(organization_id,state,updated_at DESC,id);

CREATE TABLE merchant.facility_default (
 id UUID PRIMARY KEY REFERENCES merchant.organization(id), facility_id UUID, version BIGINT NOT NULL CHECK(version>0), updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(facility_id,id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE merchant.facility_team (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0), updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE merchant.facility_responsibility (
 facility_id UUID NOT NULL, organization_id UUID NOT NULL, account_id UUID NOT NULL, responsibility VARCHAR(12) NOT NULL CHECK(responsibility IN ('PREPARATION','HANDOVER','RETURNS')),
 PRIMARY KEY(facility_id,account_id,responsibility), FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(organization_id,account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id) REFERENCES merchant.facility_team(id) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE merchant.facility_version (
 resource_kind VARCHAR(7) NOT NULL CHECK(resource_kind IN ('SITE','DEFAULT','TEAM')), target_id UUID NOT NULL, organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 version BIGINT NOT NULL CHECK(version>0), snapshot JSONB NOT NULL CHECK(jsonb_typeof(snapshot)='object'), baseline BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(resource_kind,target_id,version)
);
-- Explicit baseline only; retain IDs, timestamps and pre-existing versions without inventing actors.
INSERT INTO merchant.facility_version SELECT 'SITE',id,organization_id,version,to_jsonb(f),TRUE FROM merchant.facility f;
CREATE TABLE merchant.facility_command_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(16) NOT NULL CHECK(operation IN ('CREATE','UPDATE','ARCHIVE','DEFAULT','RESPONSIBILITY')),
 operation_key UUID NOT NULL, resource_kind VARCHAR(7) NOT NULL CHECK(resource_kind IN ('SITE','DEFAULT','TEAM')), target_id UUID NOT NULL, facility_id UUID,
 expected_version BIGINT NOT NULL CHECK(expected_version>=0), result_version BIGINT NOT NULL CHECK(result_version=expected_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), occurred_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,operation,operation_key), UNIQUE(resource_kind,target_id,result_version),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(resource_kind,target_id,result_version) REFERENCES merchant.facility_version(resource_kind,target_id,version) DEFERRABLE INITIALLY DEFERRED,
 CHECK((resource_kind='SITE' AND operation IN ('CREATE','UPDATE','ARCHIVE') AND facility_id=target_id)
  OR (resource_kind='DEFAULT' AND operation='DEFAULT' AND target_id=organization_id) OR (resource_kind='TEAM' AND operation='RESPONSIBILITY' AND facility_id=target_id))
);
CREATE TABLE merchant.facility_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(16) NOT NULL, operation_key UUID NOT NULL,
 resource_kind VARCHAR(7) NOT NULL, target_id UUID NOT NULL, prior_version BIGINT NOT NULL, new_version BIGINT NOT NULL CHECK(new_version=prior_version+1), request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE(resource_kind,target_id,new_version),
 FOREIGN KEY(organization_id,actor_account_id,operation,operation_key) REFERENCES merchant.facility_command_receipt(organization_id,actor_account_id,operation,operation_key) DEFERRABLE INITIALLY DEFERRED
);
CREATE FUNCTION merchant.facility_has_work(org UUID,site UUID) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM merchant.facility_default WHERE id=org AND facility_id=site)
 OR EXISTS(SELECT 1 FROM merchant.member_grant WHERE organization_id=org AND facility_id=site)
 OR EXISTS(SELECT 1 FROM merchant.facility_responsibility WHERE organization_id=org AND facility_id=site)
 OR EXISTS(SELECT 1 FROM orders.organization_preparation WHERE organization_id=org AND facility_id=site AND state='DRAFT')
 OR EXISTS(SELECT 1 FROM orders.pilot_shipment WHERE organization_id=org AND facility_id=site AND owner_type='BUSINESS')
 OR EXISTS(SELECT 1 FROM orders.organization_action_task WHERE organization_id=org AND facility_id=site AND state NOT IN ('COMPLETED','CANCELLED'))
$$;
CREATE FUNCTION merchant.guard_facility_master() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'FACILITY_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.created_at<>NEW.updated_at OR NEW.state<>'ACTIVE' OR NEW.code IS NULL OR NEW.kind IS NULL THEN RAISE EXCEPTION 'FACILITY_INITIAL_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
   OR OLD.state<>'ACTIVE' OR (OLD.code IS NOT NULL AND NEW.code IS DISTINCT FROM OLD.code) OR (NEW.state='ACTIVE' AND (NEW.code IS NULL OR NEW.kind IS NULL))
  THEN RAISE EXCEPTION 'FACILITY_IMMUTABLE_BINDING' USING ERRCODE='23514'; END IF;
  IF NEW.state='ARCHIVED' AND ((to_jsonb(NEW)-ARRAY['state','version','updated_at'])<>(to_jsonb(OLD)-ARRAY['state','version','updated_at']) OR merchant.facility_has_work(NEW.organization_id,NEW.id))
  THEN RAISE EXCEPTION 'FACILITY_REFERENCED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER facility_master_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.facility FOR EACH ROW EXECUTE FUNCTION merchant.guard_facility_master();
CREATE FUNCTION merchant.guard_facility_binding() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'FACILITY_BINDING_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF (TG_OP='INSERT' AND NEW.version<>1) OR (TG_OP='UPDATE' AND (NEW.id<>OLD.id OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at)) THEN RAISE EXCEPTION 'FACILITY_BINDING_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='facility_default' THEN
  IF NEW.facility_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=NEW.facility_id AND f.organization_id=NEW.id AND f.state='ACTIVE') THEN RAISE EXCEPTION 'FACILITY_ACTIVE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF TG_OP='UPDATE' AND NEW.organization_id<>OLD.organization_id THEN RAISE EXCEPTION 'FACILITY_TEAM_ORG_IMMUTABLE' USING ERRCODE='23514'; END IF;
  IF NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=NEW.id AND f.organization_id=NEW.organization_id AND f.state='ACTIVE') THEN RAISE EXCEPTION 'FACILITY_ACTIVE_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER facility_default_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.facility_default FOR EACH ROW EXECUTE FUNCTION merchant.guard_facility_binding();
CREATE TRIGGER facility_team_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.facility_team FOR EACH ROW EXECUTE FUNCTION merchant.guard_facility_binding();
CREATE FUNCTION merchant.facility_team_snapshot(site UUID) RETURNS JSONB LANGUAGE sql STABLE AS $$
 SELECT to_jsonb(t)||jsonb_build_object('items',COALESCE((SELECT jsonb_agg(jsonb_build_object('accountId',r.account_id,'responsibility',r.responsibility) ORDER BY r.account_id,r.responsibility) FROM merchant.facility_responsibility r WHERE r.facility_id=t.id),'[]'::JSONB)) FROM merchant.facility_team t WHERE t.id=site
$$;
CREATE FUNCTION merchant.facility_snapshot(kind TEXT,target UUID) RETURNS JSONB LANGUAGE plpgsql STABLE AS $$
DECLARE v JSONB;
BEGIN
 IF kind='SITE' THEN SELECT to_jsonb(f) INTO v FROM merchant.facility f WHERE id=target;
 ELSIF kind='DEFAULT' THEN SELECT to_jsonb(d) INTO v FROM merchant.facility_default d WHERE id=target;
 ELSIF kind='TEAM' THEN v:=merchant.facility_team_snapshot(target); END IF;RETURN v;
END $$;
CREATE FUNCTION merchant.guard_facility_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.baseline OR NEW.snapshot IS DISTINCT FROM merchant.facility_snapshot(NEW.resource_kind,NEW.target_id)
 OR (NEW.snapshot->>'version')::BIGINT<>NEW.version OR (CASE WHEN NEW.resource_kind='DEFAULT' THEN (NEW.snapshot->>'id')::UUID ELSE (NEW.snapshot->>'organization_id')::UUID END) IS DISTINCT FROM NEW.organization_id
 THEN RAISE EXCEPTION 'FACILITY_SNAPSHOT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF;RETURN NEW;
END $$;
CREATE TRIGGER facility_version_guard BEFORE INSERT ON merchant.facility_version FOR EACH ROW EXECUTE FUNCTION merchant.guard_facility_version();
CREATE FUNCTION merchant.check_facility_command() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE r merchant.facility_command_receipt%ROWTYPE; v JSONB; prior JSONB;
BEGIN
 IF TG_TABLE_NAME='facility_command_receipt' THEN SELECT * INTO r FROM merchant.facility_command_receipt WHERE resource_kind=NEW.resource_kind AND target_id=NEW.target_id AND result_version=NEW.result_version;
 ELSE SELECT * INTO r FROM merchant.facility_command_receipt WHERE resource_kind=NEW.resource_kind AND target_id=NEW.target_id AND result_version=NEW.new_version; END IF;
 SELECT snapshot INTO v FROM merchant.facility_version WHERE resource_kind=r.resource_kind AND target_id=r.target_id AND version=r.result_version AND NOT baseline;
 IF r.target_id IS NULL OR v IS NULL OR r.occurred_at IS DISTINCT FROM (v->>'updated_at')::TIMESTAMPTZ
 OR NOT EXISTS(SELECT 1 FROM merchant.facility_audit a WHERE a.resource_kind=r.resource_kind AND a.target_id=r.target_id AND a.new_version=r.result_version AND a.prior_version=r.expected_version
  AND a.organization_id=r.organization_id AND a.actor_account_id=r.actor_account_id AND a.operation=r.operation AND a.operation_key=r.operation_key AND a.occurred_at=r.occurred_at)
 THEN RAISE EXCEPTION 'FACILITY_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF r.resource_kind='DEFAULT' THEN
  IF NOT merchant.contact_scope(r.organization_id,r.actor_account_id,NULL,'defaults.manage') OR (r.facility_id IS NOT NULL AND NOT merchant.contact_scope(r.organization_id,r.actor_account_id,r.facility_id,'facility.read'))
    OR r.facility_id IS DISTINCT FROM (v->>'facility_id')::UUID THEN RAISE EXCEPTION 'FACILITY_DEFAULT_AUTHORITY_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NOT merchant.contact_scope(r.organization_id,r.actor_account_id,r.facility_id,'facility.read') OR NOT merchant.contact_scope(r.organization_id,r.actor_account_id,CASE WHEN r.operation='CREATE' THEN NULL ELSE r.facility_id END,'facility.manage')
   OR (r.operation='CREATE' AND NOT merchant.contact_scope(r.organization_id,r.actor_account_id,NULL,'facility.read')) THEN RAISE EXCEPTION 'FACILITY_AUTHORITY_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF r.expected_version>0 THEN SELECT snapshot INTO prior FROM merchant.facility_version WHERE resource_kind=r.resource_kind AND target_id=r.target_id AND version=r.expected_version;IF prior IS NULL THEN RAISE EXCEPTION 'FACILITY_PRIOR_VERSION_REQUIRED' USING ERRCODE='23514'; END IF; END IF;
 IF r.resource_kind='SITE' AND r.operation<>(CASE WHEN r.expected_version=0 THEN 'CREATE' WHEN v->>'state'='ARCHIVED' THEN 'ARCHIVE' ELSE 'UPDATE' END) THEN RAISE EXCEPTION 'FACILITY_OPERATION_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER facility_receipt_evidence AFTER INSERT ON merchant.facility_command_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_command();
CREATE CONSTRAINT TRIGGER facility_audit_evidence AFTER INSERT ON merchant.facility_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_command();
CREATE FUNCTION merchant.check_facility_current() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE kind TEXT; target UUID; v JSONB;
BEGIN
 kind:=CASE TG_TABLE_NAME WHEN 'facility' THEN 'SITE' WHEN 'facility_default' THEN 'DEFAULT' ELSE 'TEAM' END;
 IF TG_TABLE_NAME='facility_responsibility' THEN
  IF TG_OP='DELETE' THEN target:=OLD.facility_id;ELSE target:=NEW.facility_id;END IF;
 ELSE target:=NEW.id;END IF;
 v:=merchant.facility_snapshot(kind,target);
 IF v IS NULL OR NOT EXISTS(SELECT 1 FROM merchant.facility_version h JOIN merchant.facility_command_receipt r ON r.resource_kind=h.resource_kind AND r.target_id=h.target_id AND r.result_version=h.version
  WHERE h.resource_kind=kind AND h.target_id=target AND h.version=(v->>'version')::BIGINT AND h.snapshot=v AND NOT h.baseline)
 THEN RAISE EXCEPTION 'FACILITY_CURRENT_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF kind='SITE' AND v->>'state'='ARCHIVED' AND merchant.facility_has_work((v->>'organization_id')::UUID,target) THEN RAISE EXCEPTION 'FACILITY_REFERENCED' USING ERRCODE='23514'; END IF;
 IF kind='TEAM' AND (jsonb_array_length(v->'items')>30 OR EXISTS(SELECT 1 FROM merchant.facility_responsibility x WHERE x.facility_id=target AND NOT merchant.contact_scope(x.organization_id,x.account_id,x.facility_id,'facility.read'))) THEN RAISE EXCEPTION 'FACILITY_TEAM_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER facility_current_evidence AFTER INSERT OR UPDATE ON merchant.facility DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_current();
CREATE CONSTRAINT TRIGGER facility_default_evidence AFTER INSERT OR UPDATE ON merchant.facility_default DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_current();
CREATE CONSTRAINT TRIGGER facility_team_evidence AFTER INSERT OR UPDATE ON merchant.facility_team DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_current();
CREATE CONSTRAINT TRIGGER facility_responsibility_evidence AFTER INSERT OR UPDATE OR DELETE ON merchant.facility_responsibility DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_facility_current();
CREATE TRIGGER facility_version_append BEFORE UPDATE OR DELETE ON merchant.facility_version FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER facility_receipt_append BEFORE UPDATE OR DELETE ON merchant.facility_command_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER facility_audit_append BEFORE UPDATE OR DELETE ON merchant.facility_audit FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();

CREATE TABLE merchant.facility_grant_audit (
 request_id UUID PRIMARY KEY, input_digest TEXT NOT NULL CHECK(length(input_digest)=64), organization_id UUID NOT NULL, membership_id UUID NOT NULL,
 facility_id UUID, action VARCHAR(6) NOT NULL CHECK(action IN ('GRANT','REVOKE')), permissions TEXT[] NOT NULL, operator_principal TEXT NOT NULL, reason VARCHAR(300) NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id), FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TRIGGER facility_grant_audit_append BEFORE UPDATE OR DELETE ON merchant.facility_grant_audit FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE FUNCTION merchant.configure_local_facility_grants(p_request UUID,p_action TEXT,p_org UUID,p_account UUID,p_facility UUID,p_permissions TEXT[],p_reason TEXT) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,merchant AS $$
DECLARE mode TEXT:=current_setting('argopost.operator_environment',TRUE); owner_account UUID; subject UUID; member UUID; digest TEXT; prior merchant.facility_grant_audit%ROWTYPE; permissions TEXT[]; stamp TIMESTAMPTZ:=clock_timestamp();
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) OR COALESCE(mode,'') NOT IN ('LOCAL','TEST') THEN RAISE EXCEPTION 'LOCAL_FACILITY_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_org IS NULL OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE')
  OR p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 8 OR array_position(p_permissions,NULL) IS NOT NULL
  OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('facility.read','facility.manage','defaults.manage')) OR (p_facility IS NOT NULL AND 'defaults.manage'=ANY(p_permissions))
  OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN RAISE EXCEPTION 'FACILITY_GRANT_COMMAND_INVALID' USING ERRCODE='23514'; END IF;
 SELECT m.account_id INTO owner_account FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id WHERE o.id=p_org;
 FOR subject IN SELECT DISTINCT v FROM unnest(ARRAY[p_account,owner_account]) v WHERE v IS NOT NULL ORDER BY v LOOP PERFORM identity.lock_local_business_subject(subject,p_action='GRANT');END LOOP;
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT m.id INTO member FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id
  WHERE m.organization_id=p_org AND m.account_id=p_account AND m.state='ACTIVE' AND o.state='ACTIVE' AND o.environment=mode AND owner.state='ACTIVE' AND owner.role='OWNER'
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=m.id AND g.permission='dashboard.read');
 IF member IS NULL OR (p_facility IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility WHERE id=p_facility AND organization_id=p_org AND state='ACTIVE')) THEN RAISE EXCEPTION 'FACILITY_GRANT_CONTEXT_REQUIRED' USING ERRCODE='23514'; END IF;
 permissions:=ARRAY(SELECT DISTINCT p FROM unnest(p_permissions) p ORDER BY p);digest:=encode(sha256(convert_to(jsonb_build_array(mode,p_action,p_org,p_account,p_facility,permissions,p_reason)::TEXT,'UTF8')),'hex');
 SELECT * INTO prior FROM merchant.facility_grant_audit WHERE request_id=p_request;
 IF FOUND THEN IF prior.input_digest<>digest THEN RAISE EXCEPTION 'FACILITY_GRANT_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514'; END IF;RETURN member;END IF;
 IF p_action='GRANT' THEN INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope,facility_id) SELECT gen_random_uuid(),member,p_org,p,CASE WHEN p_facility IS NULL THEN 'ORGANIZATION' ELSE 'FACILITY' END,p_facility FROM unnest(permissions) p ON CONFLICT DO NOTHING;
 ELSE DELETE FROM merchant.member_grant WHERE membership_id=member AND permission=ANY(permissions) AND facility_id IS NOT DISTINCT FROM p_facility;END IF;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission='facility.manage' AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=member AND r.permission='facility.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id))) THEN RAISE EXCEPTION 'FACILITY_READ_DEPENDENCY_REQUIRED' USING ERRCODE='23514'; END IF;
 UPDATE merchant.membership SET version=version+1,authority_version=authority_version+1,updated_at=stamp WHERE id=member;
 INSERT INTO merchant.facility_grant_audit VALUES(p_request,digest,p_org,member,p_facility,p_action,permissions,session_user,p_reason,stamp);RETURN member;
END $$;
REVOKE ALL ON merchant.facility,merchant.facility_default,merchant.facility_team,merchant.facility_responsibility,merchant.facility_version,merchant.facility_command_receipt,merchant.facility_audit,merchant.facility_grant_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON merchant.facility,merchant.facility_default,merchant.facility_team TO "${runtimeRole}";
GRANT SELECT,INSERT,DELETE ON merchant.facility_responsibility TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.facility_version,merchant.facility_command_receipt,merchant.facility_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.configure_local_facility_grants(UUID,TEXT,UUID,UUID,UUID,TEXT[],TEXT),merchant.guard_facility_master(),merchant.guard_facility_binding(),merchant.guard_facility_version(),merchant.check_facility_command(),merchant.check_facility_current() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.valid_facility_hours(JSONB),merchant.facility_has_work(UUID,UUID),merchant.facility_team_snapshot(UUID),merchant.facility_snapshot(TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.valid_facility_hours(JSONB),merchant.facility_has_work(UUID,UUID),merchant.facility_team_snapshot(UUID),merchant.facility_snapshot(TEXT,UUID) TO "${runtimeRole}";

-- Serialize every new active reference with archive, including direct runtime DML.
CREATE FUNCTION merchant.guard_active_facility_reference() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF NEW.facility_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=NEW.facility_id AND f.organization_id=NEW.organization_id AND f.state='ACTIVE') THEN RAISE EXCEPTION 'FACILITY_ACTIVE_REQUIRED' USING ERRCODE='23514'; END IF;RETURN NEW;
END $$;
CREATE TRIGGER facility_grant_active_reference BEFORE INSERT ON merchant.member_grant FOR EACH ROW EXECUTE FUNCTION merchant.guard_active_facility_reference();
CREATE TRIGGER facility_preparation_active_reference BEFORE INSERT OR UPDATE ON orders.organization_preparation FOR EACH ROW EXECUTE FUNCTION merchant.guard_active_facility_reference();
REVOKE ALL ON FUNCTION merchant.guard_active_facility_reference() FROM PUBLIC,"${runtimeRole}";
