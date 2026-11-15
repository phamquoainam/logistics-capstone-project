-- Owner accepted OAA V1. Test databases first. No grants/seed or historical rewrite.
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read',
 'contacts.read','contacts.write','contacts.import','contacts.export'));
ALTER TABLE merchant.member_grant DROP CONSTRAINT preparation_scope_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK(
 (scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND
 permission IN ('shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare',
 'shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign',
 'shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export')));

CREATE FUNCTION merchant.contact_scope(p_org UUID,p_actor UUID,p_facility UUID,p_permission TEXT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
  JOIN merchant.member_grant g ON g.membership_id=m.id AND g.organization_id=o.id
  WHERE m.organization_id=p_org AND m.account_id=p_actor AND m.state='ACTIVE' AND o.state='ACTIVE'
   AND o.environment IN ('LOCAL','TEST') AND o.admission_origin='LOCAL_OPERATOR_PROVISIONED'
   AND g.permission=p_permission AND (g.scope='ORGANIZATION' OR g.facility_id=p_facility))
$$;
CREATE FUNCTION merchant.check_contact_grants() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID:=CASE WHEN TG_OP='DELETE' THEN OLD.organization_id ELSE NEW.organization_id END;
BEGIN
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission IN ('contacts.write','contacts.import','contacts.export')
  AND (NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='contacts.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id))
   OR (g.permission='contacts.import' AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='contacts.write' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id)))))
 THEN RAISE EXCEPTION 'CONTACT_PERMISSION_DEPENDENCY_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER contact_grant_dependencies AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_grants();

CREATE TABLE merchant.organization_contact (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID,
 kind VARCHAR(9) NOT NULL CHECK(kind IN ('SENDER','RECIPIENT')), label VARCHAR(80), reference VARCHAR(80), group_name VARCHAR(80),
 contact_name VARCHAR(160) NOT NULL CHECK(length(btrim(contact_name))>0 AND contact_name !~ '[[:cntrl:]]'),
 contact_phone VARCHAR(13) NOT NULL CHECK(contact_phone ~ '^\+84[35789][0-9]{8}$'), country VARCHAR(2) NOT NULL CHECK(country='VN'),
 administrative_area VARCHAR(160) NOT NULL CHECK(length(btrim(administrative_area))>0 AND administrative_area !~ '[[:cntrl:]]'),
 locality VARCHAR(160) NOT NULL CHECK(length(btrim(locality))>0 AND locality !~ '[[:cntrl:]]'),
 address_line VARCHAR(500) NOT NULL CHECK(length(btrim(address_line))>0 AND address_line !~ '[[:cntrl:]]'),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','ARCHIVED')), version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), archived_at TIMESTAMPTZ,
 CHECK((state='ACTIVE' AND archived_at IS NULL) OR (state='ARCHIVED' AND archived_at=updated_at)),
 CHECK(label IS NULL OR (length(btrim(label))>0 AND label !~ '[[:cntrl:]]')),
 CHECK(reference IS NULL OR (length(btrim(reference))>0 AND reference !~ '[[:cntrl:]]')),
 CHECK(group_name IS NULL OR (length(btrim(group_name))>0 AND group_name !~ '[[:cntrl:]]')),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id), UNIQUE(id,organization_id)
);
CREATE UNIQUE INDEX organization_contact_reference ON merchant.organization_contact(organization_id,kind,facility_id,reference) NULLS NOT DISTINCT WHERE reference IS NOT NULL;
CREATE INDEX organization_contact_list ON merchant.organization_contact(organization_id,updated_at DESC,id);
CREATE TABLE merchant.organization_contact_version (
 contact_id UUID NOT NULL, organization_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0), snapshot JSONB NOT NULL CHECK(jsonb_typeof(snapshot)='object'),
 PRIMARY KEY(contact_id,version), FOREIGN KEY(contact_id,organization_id) REFERENCES merchant.organization_contact(id,organization_id)
);
ALTER TABLE merchant.organization_contact ADD CONSTRAINT organization_contact_current_version FOREIGN KEY(id,version) REFERENCES merchant.organization_contact_version(contact_id,version) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE merchant.organization_contact_default (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID,
 kind VARCHAR(9) NOT NULL CHECK(kind IN ('SENDER','RECIPIENT')), contact_id UUID, version BIGINT NOT NULL CHECK(version>0), updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(contact_id,organization_id) REFERENCES merchant.organization_contact(id,organization_id), UNIQUE NULLS NOT DISTINCT(organization_id,facility_id,kind)
);
CREATE TABLE merchant.organization_contact_command_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(16) NOT NULL CHECK(operation IN ('CREATE','UPDATE','ARCHIVE','RESTORE','DEFAULT','IMPORT_PREVIEW','IMPORT_COMMIT')),
 operation_key UUID NOT NULL, target_id UUID NOT NULL, facility_id UUID, expected_version BIGINT NOT NULL CHECK(expected_version>=0),
 result_version BIGINT NOT NULL CHECK(result_version=expected_version+1), fingerprint_key_id VARCHAR(64) NOT NULL,
 fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), default_result JSONB, occurred_at TIMESTAMPTZ NOT NULL,
 CHECK((operation='DEFAULT' AND jsonb_typeof(default_result)='object') OR (operation<>'DEFAULT' AND default_result IS NULL)),
 PRIMARY KEY(organization_id,actor_account_id,operation,operation_key), UNIQUE(target_id,result_version),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE merchant.organization_contact_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(16) NOT NULL,
 operation_key UUID NOT NULL, target_id UUID NOT NULL, prior_version BIGINT NOT NULL, new_version BIGINT NOT NULL,
 request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, UNIQUE(target_id,new_version),
 FOREIGN KEY(organization_id,actor_account_id,operation,operation_key) REFERENCES merchant.organization_contact_command_receipt(organization_id,actor_account_id,operation,operation_key) DEFERRABLE INITIALLY DEFERRED
);
CREATE FUNCTION merchant.organization_contact_append_only() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'CONTACT_HISTORY_APPEND_ONLY' USING ERRCODE='23514'; END $$;
CREATE TRIGGER contact_version_append BEFORE UPDATE OR DELETE ON merchant.organization_contact_version FOR EACH ROW EXECUTE FUNCTION merchant.organization_contact_append_only();
CREATE TRIGGER contact_receipt_append BEFORE UPDATE OR DELETE ON merchant.organization_contact_command_receipt FOR EACH ROW EXECUTE FUNCTION merchant.organization_contact_append_only();
CREATE TRIGGER contact_audit_append BEFORE UPDATE OR DELETE ON merchant.organization_contact_audit FOR EACH ROW EXECUTE FUNCTION merchant.organization_contact_append_only();

CREATE FUNCTION merchant.guard_organization_contact() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'CONTACT_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.state<>'ACTIVE' OR NEW.created_at<>NEW.updated_at THEN RAISE EXCEPTION 'CONTACT_INITIAL_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.kind<>OLD.kind
   OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
   OR (OLD.state='ARCHIVED' AND NEW.state<>'ACTIVE')
  THEN RAISE EXCEPTION 'CONTACT_VERSION_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
  IF NEW.state<>OLD.state AND (to_jsonb(NEW)-ARRAY['state','version','updated_at','archived_at'])<>(to_jsonb(OLD)-ARRAY['state','version','updated_at','archived_at'])
  THEN RAISE EXCEPTION 'CONTACT_LIFECYCLE_CONTENT_IMMUTABLE' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_contact_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.organization_contact FOR EACH ROW EXECUTE FUNCTION merchant.guard_organization_contact();

CREATE FUNCTION merchant.check_organization_contact_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE c merchant.organization_contact%ROWTYPE; r merchant.organization_contact_command_receipt%ROWTYPE; v JSONB; prior JSONB;
BEGIN
 IF TG_TABLE_NAME='organization_contact' THEN SELECT * INTO c FROM merchant.organization_contact WHERE id=NEW.id;
 ELSE SELECT * INTO c FROM merchant.organization_contact WHERE id=NEW.contact_id; END IF;
 IF NOT FOUND THEN RAISE EXCEPTION 'CONTACT_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='organization_contact_version' AND NEW.version<>c.version THEN RAISE EXCEPTION 'CONTACT_SNAPSHOT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF;
 SELECT snapshot INTO v FROM merchant.organization_contact_version WHERE contact_id=c.id AND version=c.version;
 SELECT * INTO r FROM merchant.organization_contact_command_receipt WHERE target_id=c.id AND result_version=c.version;
 IF v IS DISTINCT FROM to_jsonb(c) OR r.organization_id IS DISTINCT FROM c.organization_id OR r.facility_id IS DISTINCT FROM c.facility_id OR r.occurred_at IS DISTINCT FROM c.updated_at
  OR NOT merchant.contact_scope(c.organization_id,r.actor_account_id,c.facility_id,'contacts.read') OR NOT merchant.contact_scope(c.organization_id,r.actor_account_id,c.facility_id,'contacts.write')
  OR NOT EXISTS(SELECT 1 FROM merchant.organization_contact_audit a WHERE a.target_id=c.id AND a.new_version=c.version AND a.organization_id=r.organization_id
   AND a.actor_account_id=r.actor_account_id AND a.operation=r.operation AND a.operation_key=r.operation_key AND a.prior_version=r.expected_version AND a.occurred_at=r.occurred_at)
 THEN RAISE EXCEPTION 'CONTACT_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF c.version=1 THEN
  IF r.operation<>'CREATE' THEN RAISE EXCEPTION 'CONTACT_CREATE_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  SELECT snapshot INTO prior FROM merchant.organization_contact_version WHERE contact_id=c.id AND version=c.version-1;
  IF prior IS NULL OR r.operation<>(CASE WHEN prior->>'state'='ARCHIVED' THEN 'RESTORE' WHEN c.state='ARCHIVED' THEN 'ARCHIVE' ELSE 'UPDATE' END)
   THEN RAISE EXCEPTION 'CONTACT_OPERATION_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF c.state='ARCHIVED' AND EXISTS(SELECT 1 FROM merchant.organization_contact_default WHERE contact_id=c.id) THEN RAISE EXCEPTION 'ARCHIVED_CONTACT_DEFAULT_DENIED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_contact_evidence AFTER INSERT OR UPDATE ON merchant.organization_contact DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_organization_contact_evidence();
CREATE CONSTRAINT TRIGGER organization_contact_snapshot_evidence AFTER INSERT ON merchant.organization_contact_version DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_organization_contact_evidence();

CREATE FUNCTION merchant.guard_contact_default() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'CONTACT_DEFAULT_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF (TG_OP='INSERT' AND NEW.version<>1) OR (TG_OP='UPDATE' AND (NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id
  OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.kind<>OLD.kind OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at))
 THEN RAISE EXCEPTION 'CONTACT_DEFAULT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF NEW.contact_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.organization_contact c WHERE c.id=NEW.contact_id AND c.organization_id=NEW.organization_id
  AND c.facility_id IS NOT DISTINCT FROM NEW.facility_id AND c.kind=NEW.kind AND c.state='ACTIVE')
 THEN RAISE EXCEPTION 'CONTACT_DEFAULT_BINDING_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_contact_default_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.organization_contact_default FOR EACH ROW EXECUTE FUNCTION merchant.guard_contact_default();
CREATE FUNCTION merchant.check_contact_default_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE binding merchant.organization_contact_default%ROWTYPE;
BEGIN
 SELECT * INTO binding FROM merchant.organization_contact_default WHERE id=NEW.id;
 IF NOT EXISTS(SELECT 1 FROM merchant.organization_contact_command_receipt r JOIN merchant.organization_contact_audit a ON a.target_id=r.target_id AND a.new_version=r.result_version
   WHERE r.organization_id=binding.organization_id AND r.facility_id IS NOT DISTINCT FROM binding.facility_id AND r.occurred_at=binding.updated_at
   AND ((r.operation='DEFAULT' AND r.target_id=binding.id AND r.result_version=binding.version)
    OR (TG_OP='UPDATE' AND r.operation='ARCHIVE' AND binding.contact_id IS NULL AND r.target_id=OLD.contact_id
     AND EXISTS(SELECT 1 FROM merchant.organization_contact c WHERE c.id=r.target_id AND c.state='ARCHIVED' AND c.kind=binding.kind)))
   AND merchant.contact_scope(r.organization_id,r.actor_account_id,r.facility_id,'contacts.read') AND merchant.contact_scope(r.organization_id,r.actor_account_id,r.facility_id,'contacts.write'))
 THEN RAISE EXCEPTION 'CONTACT_DEFAULT_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_contact_default_evidence AFTER INSERT OR UPDATE ON merchant.organization_contact_default DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_default_evidence();

CREATE FUNCTION merchant.check_contact_command_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE r merchant.organization_contact_command_receipt%ROWTYPE;
BEGIN
 SELECT * INTO r FROM merchant.organization_contact_command_receipt WHERE target_id=NEW.target_id AND result_version=NEW.new_version;
 IF NOT FOUND OR NOT EXISTS(SELECT 1 FROM merchant.organization_contact_audit a WHERE a.target_id=r.target_id AND a.new_version=r.result_version
  AND a.organization_id=r.organization_id AND a.actor_account_id=r.actor_account_id AND a.operation=r.operation AND a.operation_key=r.operation_key
  AND a.prior_version=r.expected_version AND a.occurred_at=r.occurred_at)
 THEN RAISE EXCEPTION 'CONTACT_COMMAND_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF r.operation='DEFAULT' THEN
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_contact_default d WHERE d.id=r.target_id AND d.organization_id=r.organization_id
   AND d.facility_id IS NOT DISTINCT FROM r.facility_id AND d.version=r.result_version AND to_jsonb(d)=r.default_result)
  THEN RAISE EXCEPTION 'CONTACT_COMMAND_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSIF r.operation IN ('IMPORT_PREVIEW','IMPORT_COMMIT') THEN
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_contact_import_job j WHERE j.id=r.target_id AND j.organization_id=r.organization_id AND j.facility_id IS NOT DISTINCT FROM r.facility_id AND j.version=r.result_version AND j.creator_account_id=r.actor_account_id) THEN RAISE EXCEPTION 'CONTACT_IMPORT_COMMAND_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM merchant.organization_contact c WHERE c.id=r.target_id AND c.organization_id=r.organization_id AND c.facility_id IS NOT DISTINCT FROM r.facility_id AND c.version=r.result_version)
  THEN RAISE EXCEPTION 'CONTACT_COMMAND_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_contact_command_evidence AFTER INSERT ON merchant.organization_contact_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_command_evidence();
CREATE FUNCTION merchant.check_contact_receipt_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM merchant.organization_contact_audit a WHERE a.target_id=NEW.target_id AND a.new_version=NEW.result_version AND a.organization_id=NEW.organization_id
  AND a.actor_account_id=NEW.actor_account_id AND a.operation=NEW.operation AND a.operation_key=NEW.operation_key)
 THEN RAISE EXCEPTION 'CONTACT_RECEIPT_AUDIT_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_contact_receipt_evidence AFTER INSERT ON merchant.organization_contact_command_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_receipt_audit();

REVOKE ALL ON merchant.organization_contact,merchant.organization_contact_version,merchant.organization_contact_default,merchant.organization_contact_command_receipt,merchant.organization_contact_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON merchant.organization_contact,merchant.organization_contact_default TO "${runtimeRole}";
GRANT SELECT,INSERT ON merchant.organization_contact_version,merchant.organization_contact_command_receipt,merchant.organization_contact_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.check_contact_grants(),merchant.organization_contact_append_only(),merchant.guard_organization_contact(),merchant.check_organization_contact_evidence(),merchant.guard_contact_default(),merchant.check_contact_default_evidence() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.contact_scope(UUID,UUID,UUID,TEXT) FROM PUBLIC;
REVOKE ALL ON FUNCTION merchant.check_contact_command_evidence(),merchant.check_contact_receipt_audit() FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION merchant.contact_scope(UUID,UUID,UUID,TEXT) TO "${runtimeRole}";

CREATE TABLE merchant.organization_contact_import_job (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID, creator_account_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0), total_rows INTEGER NOT NULL CHECK(total_rows BETWEEN 1 AND 100),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(organization_id,creator_account_id) REFERENCES merchant.membership(organization_id,account_id), UNIQUE(id,organization_id)
);
CREATE FUNCTION merchant.valid_contact_import_input(v JSONB,facility UUID) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE k TEXT;
BEGIN
 IF v IS NULL OR jsonb_typeof(v)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(v))<>8
  OR NOT v ?& ARRAY['kind','facilityId','label','reference','group','contactName','contactPhone','address']
  OR v->>'kind' NOT IN ('SENDER','RECIPIENT') OR jsonb_typeof(v->'kind')<>'string'
  OR v->'facilityId' IS DISTINCT FROM COALESCE(to_jsonb(facility),'null'::JSONB)
  OR jsonb_typeof(v->'contactName')<>'string' OR length(btrim(v->>'contactName')) NOT BETWEEN 1 AND 160 OR v->>'contactName' ~ '[[:cntrl:]]'
  OR jsonb_typeof(v->'contactPhone')<>'string' OR v->>'contactPhone' !~ '^\+84[35789][0-9]{8}$'
  OR jsonb_typeof(v->'address')<>'object' OR (SELECT count(*) FROM jsonb_object_keys(v->'address'))<>4
  OR NOT (v->'address') ?& ARRAY['country','administrativeArea','locality','addressLine'] OR v->'address'->>'country'<>'VN'
 THEN RETURN FALSE; END IF;
 FOREACH k IN ARRAY ARRAY['label','reference','group'] LOOP
  IF jsonb_typeof(v->k)<>'null' AND (jsonb_typeof(v->k)<>'string' OR length(btrim(v->>k)) NOT BETWEEN 1 AND 80 OR v->>k ~ '[[:cntrl:]]') THEN RETURN FALSE; END IF;
 END LOOP;
 FOREACH k IN ARRAY ARRAY['administrativeArea','locality','addressLine'] LOOP
  IF jsonb_typeof(v->'address'->k)<>'string' OR length(btrim(v->'address'->>k)) NOT BETWEEN 1 AND (CASE WHEN k='addressLine' THEN 500 ELSE 160 END) OR v->'address'->>k ~ '[[:cntrl:]]' THEN RETURN FALSE; END IF;
 END LOOP; RETURN TRUE;
EXCEPTION WHEN OTHERS THEN RETURN FALSE;
END $$;
CREATE TABLE merchant.organization_contact_import_row (
 job_id UUID NOT NULL, organization_id UUID NOT NULL, row_number INTEGER NOT NULL CHECK(row_number BETWEEN 1 AND 100),
 proposed_input JSONB, errors JSONB NOT NULL CHECK(jsonb_typeof(errors)='array'), contact_id UUID, created_job_version BIGINT,
 PRIMARY KEY(job_id,row_number), FOREIGN KEY(job_id,organization_id) REFERENCES merchant.organization_contact_import_job(id,organization_id),
 FOREIGN KEY(contact_id,organization_id) REFERENCES merchant.organization_contact(id,organization_id), UNIQUE(contact_id),
 CHECK((proposed_input IS NULL AND jsonb_array_length(errors)>0 AND contact_id IS NULL) OR (proposed_input IS NOT NULL AND jsonb_typeof(proposed_input)='object' AND errors='[]'::jsonb)),
 CHECK((contact_id IS NULL AND created_job_version IS NULL) OR (contact_id IS NOT NULL AND created_job_version>1))
);
CREATE FUNCTION merchant.guard_contact_import() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE j merchant.organization_contact_import_job%ROWTYPE;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'CONTACT_IMPORT_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_TABLE_NAME='organization_contact_import_job' THEN
  IF (TG_OP='INSERT' AND (NEW.version<>1 OR NEW.created_at<>NEW.updated_at)) OR (TG_OP='UPDATE' AND (NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
   OR (to_jsonb(NEW)-ARRAY['version','updated_at'])<>(to_jsonb(OLD)-ARRAY['version','updated_at'])))
  THEN RAISE EXCEPTION 'CONTACT_IMPORT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  SELECT * INTO j FROM merchant.organization_contact_import_job WHERE id=NEW.job_id;
  IF (NEW.proposed_input IS NOT NULL AND merchant.valid_contact_import_input(NEW.proposed_input,j.facility_id) IS NOT TRUE)
   OR NEW.row_number>j.total_rows OR (TG_OP='INSERT' AND (j.version<>1 OR NEW.contact_id IS NOT NULL))
   OR (TG_OP='UPDATE' AND (OLD.contact_id IS NOT NULL OR NEW.contact_id IS NULL OR NEW.created_job_version<>j.version
    OR (to_jsonb(NEW)-ARRAY['contact_id','created_job_version'])<>(to_jsonb(OLD)-ARRAY['contact_id','created_job_version'])))
  THEN RAISE EXCEPTION 'CONTACT_IMPORT_ROW_IMMUTABLE' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_contact_import_job_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.organization_contact_import_job FOR EACH ROW EXECUTE FUNCTION merchant.guard_contact_import();
CREATE TRIGGER organization_contact_import_row_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.organization_contact_import_row FOR EACH ROW EXECUTE FUNCTION merchant.guard_contact_import();
CREATE FUNCTION merchant.check_contact_import_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE j merchant.organization_contact_import_job%ROWTYPE;
BEGIN
 IF TG_TABLE_NAME='organization_contact_import_job' THEN SELECT * INTO j FROM merchant.organization_contact_import_job WHERE id=NEW.id;
 ELSE SELECT * INTO j FROM merchant.organization_contact_import_job WHERE id=NEW.job_id; END IF;
 IF (SELECT count(*) FROM merchant.organization_contact_import_row WHERE job_id=j.id)<>j.total_rows
  OR NOT merchant.contact_scope(j.organization_id,j.creator_account_id,j.facility_id,'contacts.read')
  OR NOT merchant.contact_scope(j.organization_id,j.creator_account_id,j.facility_id,'contacts.write')
  OR NOT merchant.contact_scope(j.organization_id,j.creator_account_id,j.facility_id,'contacts.import')
  OR NOT EXISTS(SELECT 1 FROM merchant.organization_contact_command_receipt r WHERE r.target_id=j.id AND r.result_version=j.version
    AND r.organization_id=j.organization_id AND r.actor_account_id=j.creator_account_id AND r.facility_id IS NOT DISTINCT FROM j.facility_id
    AND r.occurred_at=j.updated_at AND r.operation=CASE WHEN j.version=1 THEN 'IMPORT_PREVIEW' ELSE 'IMPORT_COMMIT' END)
  OR EXISTS(SELECT 1 FROM merchant.organization_contact_import_row r JOIN merchant.organization_contact c ON c.id=r.contact_id WHERE r.job_id=j.id
    AND (c.organization_id<>j.organization_id OR c.facility_id IS DISTINCT FROM j.facility_id
      OR NOT EXISTS(SELECT 1 FROM merchant.organization_contact_version v WHERE v.contact_id=c.id AND v.version=1
        AND v.snapshot->>'contact_name'=r.proposed_input->>'contactName' AND v.snapshot->>'contact_phone'=r.proposed_input->>'contactPhone'
        AND v.snapshot->>'kind'=r.proposed_input->>'kind' AND v.snapshot->>'address_line'=r.proposed_input->'address'->>'addressLine')))
 THEN RAISE EXCEPTION 'CONTACT_IMPORT_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_contact_import_job_evidence AFTER INSERT OR UPDATE ON merchant.organization_contact_import_job DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_import_evidence();
CREATE CONSTRAINT TRIGGER organization_contact_import_row_evidence AFTER INSERT OR UPDATE ON merchant.organization_contact_import_row DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_contact_import_evidence();
CREATE TABLE merchant.organization_contact_export_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), actor_account_id UUID NOT NULL, request_id UUID NOT NULL,
 contact_ids UUID[] NOT NULL CHECK(cardinality(contact_ids)<=200 AND array_position(contact_ids,NULL) IS NULL),
 row_count INTEGER NOT NULL CHECK(row_count=cardinality(contact_ids)), fingerprint_key_id VARCHAR(64) NOT NULL,
 query_digest BYTEA NOT NULL CHECK(octet_length(query_digest)=32), occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE TRIGGER organization_contact_export_append BEFORE UPDATE OR DELETE ON merchant.organization_contact_export_audit FOR EACH ROW EXECUTE FUNCTION merchant.organization_contact_append_only();
CREATE FUNCTION merchant.guard_contact_export() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF (SELECT count(DISTINCT id) FROM unnest(NEW.contact_ids) id)<>NEW.row_count
  OR (SELECT count(*) FROM merchant.organization_contact c WHERE c.id=ANY(NEW.contact_ids) AND c.organization_id=NEW.organization_id)<>NEW.row_count
  OR EXISTS(SELECT 1 FROM merchant.organization_contact c WHERE c.id=ANY(NEW.contact_ids)
    AND (NOT merchant.contact_scope(NEW.organization_id,NEW.actor_account_id,c.facility_id,'contacts.read') OR NOT merchant.contact_scope(NEW.organization_id,NEW.actor_account_id,c.facility_id,'contacts.export')))
  OR NOT EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.member_grant g ON g.membership_id=m.id
    WHERE m.organization_id=NEW.organization_id AND m.account_id=NEW.actor_account_id AND m.state='ACTIVE' AND g.permission='contacts.export')
 THEN RAISE EXCEPTION 'CONTACT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_contact_export_guard BEFORE INSERT ON merchant.organization_contact_export_audit FOR EACH ROW EXECUTE FUNCTION merchant.guard_contact_export();
REVOKE ALL ON merchant.organization_contact_import_job,merchant.organization_contact_import_row,merchant.organization_contact_export_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON merchant.organization_contact_import_job,merchant.organization_contact_import_row TO "${runtimeRole}";
GRANT INSERT ON merchant.organization_contact_export_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.guard_contact_import(),merchant.check_contact_import_evidence(),merchant.guard_contact_export() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.valid_contact_import_input(JSONB,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.valid_contact_import_input(JSONB,UUID) TO "${runtimeRole}";

-- Same audited operator, additional explicitly admitted permissions; no grants.
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
 OR cardinality(p_permissions)>18 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export'))
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
