-- Owner accepted Organization Shipment Management V1. LOCAL/TEST source + test DB first.
-- No existing Shipment/Quote/Parcel/Finance/migration rewrite, grants or seed data.
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign'));

CREATE FUNCTION merchant.check_export_grants() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID;
BEGIN
 org:=CASE WHEN TG_OP='DELETE' THEN OLD.organization_id ELSE NEW.organization_id END;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission='shipment.export.pii'
   AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='shipment.export'
     AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id)))
 THEN RAISE EXCEPTION 'EXPORT_PERMISSION_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_export_grants AFTER INSERT OR DELETE ON merchant.member_grant
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_export_grants();
REVOKE ALL ON FUNCTION merchant.check_export_grants() FROM PUBLIC,"${runtimeRole}";

CREATE TABLE orders.organization_shipment_assignment (
 shipment_id UUID PRIMARY KEY, organization_id UUID NOT NULL, facility_id UUID,
 assignee_account_id UUID, version BIGINT NOT NULL CHECK(version>0), updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(organization_id,assignee_account_id) REFERENCES merchant.membership(organization_id,account_id),
 UNIQUE(shipment_id,organization_id)
);
CREATE TABLE orders.organization_shipment_assignment_event (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, shipment_id UUID NOT NULL, facility_id UUID,
 operation VARCHAR(12) NOT NULL CHECK(operation IN ('ASSIGNED','REASSIGNED','UNASSIGNED')),
 actor_account_id UUID NOT NULL, prior_assignee_account_id UUID, assignee_account_id UUID,
 prior_version BIGINT NOT NULL CHECK(prior_version>=0), new_version BIGINT NOT NULL CHECK(new_version=prior_version+1),
 reason_code VARCHAR(24) NOT NULL CHECK(reason_code IN ('INITIAL_ASSIGNMENT','REASSIGNMENT','UNASSIGNMENT')),
 request_id UUID NOT NULL, operation_key UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 CHECK(prior_assignee_account_id IS DISTINCT FROM assignee_account_id),
 CHECK((prior_assignee_account_id IS NULL AND assignee_account_id IS NOT NULL AND operation='ASSIGNED' AND reason_code='INITIAL_ASSIGNMENT')
   OR (prior_assignee_account_id IS NOT NULL AND assignee_account_id IS NOT NULL AND operation='REASSIGNED' AND reason_code='REASSIGNMENT')
   OR (prior_assignee_account_id IS NOT NULL AND assignee_account_id IS NULL AND operation='UNASSIGNED' AND reason_code='UNASSIGNMENT')),
 FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(organization_id,prior_assignee_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(organization_id,assignee_account_id) REFERENCES merchant.membership(organization_id,account_id),
 UNIQUE(shipment_id,new_version), UNIQUE(organization_id,actor_account_id,operation_key)
);
CREATE TABLE orders.organization_shipment_assignment_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation_key UUID NOT NULL,
 shipment_id UUID NOT NULL, facility_id UUID, expected_version BIGINT NOT NULL CHECK(expected_version>=0),
 result_version BIGINT NOT NULL CHECK(result_version=expected_version+1), fingerprint_key_id VARCHAR(64) NOT NULL,
 fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), occurred_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,operation_key), UNIQUE(shipment_id,result_version),
 FOREIGN KEY(shipment_id,result_version) REFERENCES orders.organization_shipment_assignment_event(shipment_id,new_version) DEFERRABLE INITIALLY DEFERRED
);
ALTER TABLE orders.organization_shipment_assignment ADD CONSTRAINT assignment_current_event
 FOREIGN KEY(shipment_id,version) REFERENCES orders.organization_shipment_assignment_event(shipment_id,new_version) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE orders.organization_shipment_assignment_event ADD CONSTRAINT assignment_event_receipt
 FOREIGN KEY(organization_id,actor_account_id,operation_key) REFERENCES orders.organization_shipment_assignment_receipt(organization_id,actor_account_id,operation_key) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE orders.organization_shipment_export_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), actor_account_id UUID NOT NULL,
 request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, filter_digest BYTEA NOT NULL CHECK(octet_length(filter_digest)=32),
 shipment_ids UUID[] NOT NULL CHECK(cardinality(shipment_ids)<=200 AND array_position(shipment_ids,NULL) IS NULL),
 row_count INTEGER NOT NULL CHECK(row_count BETWEEN 0 AND 200 AND row_count=cardinality(shipment_ids)),
 pii_included BOOLEAN NOT NULL, FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE INDEX organization_assignment_filter ON orders.organization_shipment_assignment(organization_id,assignee_account_id,shipment_id);
CREATE INDEX organization_assignment_history ON orders.organization_shipment_assignment_event(organization_id,shipment_id,new_version DESC);
CREATE INDEX organization_export_audit_scope ON orders.organization_shipment_export_audit(organization_id,occurred_at DESC);

CREATE FUNCTION orders.organization_management_append_only() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'ORGANIZATION_MANAGEMENT_APPEND_ONLY' USING ERRCODE='23514'; END $$;
CREATE TRIGGER assignment_event_append BEFORE UPDATE OR DELETE ON orders.organization_shipment_assignment_event FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();
CREATE TRIGGER assignment_receipt_append BEFORE UPDATE OR DELETE ON orders.organization_shipment_assignment_receipt FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();
CREATE TRIGGER organization_export_audit_append BEFORE UPDATE OR DELETE ON orders.organization_shipment_export_audit FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();

CREATE FUNCTION orders.guard_organization_assignment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ASSIGNMENT_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF NOT EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=NEW.shipment_id AND s.organization_id=NEW.organization_id
   AND s.owner_type='BUSINESS' AND s.booking_state='CONFIRMED' AND s.facility_id IS NOT DISTINCT FROM NEW.facility_id)
 THEN RAISE EXCEPTION 'ASSIGNMENT_SHIPMENT_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' AND NEW.version<>1 THEN RAISE EXCEPTION 'ASSIGNMENT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' AND (NEW.shipment_id<>OLD.shipment_id OR NEW.organization_id<>OLD.organization_id
   OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.version<>OLD.version+1
   OR NEW.updated_at<OLD.updated_at OR NEW.assignee_account_id IS NOT DISTINCT FROM OLD.assignee_account_id)
 THEN RAISE EXCEPTION 'ASSIGNMENT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER organization_assignment_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_shipment_assignment FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_assignment();

CREATE FUNCTION orders.check_organization_assignment_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE s orders.organization_shipment_assignment%ROWTYPE; e orders.organization_shipment_assignment_event%ROWTYPE;
 r orders.organization_shipment_assignment_receipt%ROWTYPE; prior orders.organization_shipment_assignment_event%ROWTYPE;
BEGIN
 SELECT * INTO s FROM orders.organization_shipment_assignment WHERE shipment_id=NEW.shipment_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'ASSIGNMENT_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 SELECT * INTO e FROM orders.organization_shipment_assignment_event WHERE shipment_id=s.shipment_id AND new_version=s.version;
 IF NOT FOUND OR e.organization_id<>s.organization_id OR e.facility_id IS DISTINCT FROM s.facility_id
   OR e.assignee_account_id IS DISTINCT FROM s.assignee_account_id OR e.occurred_at<>s.updated_at
 THEN RAISE EXCEPTION 'ASSIGNMENT_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='organization_shipment_assignment_event' THEN
   IF NEW.new_version<>s.version THEN RAISE EXCEPTION 'ASSIGNMENT_EVENT_CURRENT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_TABLE_NAME='organization_shipment_assignment_receipt' THEN
   IF NEW.result_version<>s.version THEN RAISE EXCEPTION 'ASSIGNMENT_RECEIPT_CURRENT_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 SELECT * INTO r FROM orders.organization_shipment_assignment_receipt WHERE organization_id=e.organization_id AND actor_account_id=e.actor_account_id AND operation_key=e.operation_key;
 IF NOT FOUND OR r.shipment_id<>s.shipment_id OR r.facility_id IS DISTINCT FROM s.facility_id
   OR r.expected_version<>e.prior_version OR r.result_version<>e.new_version OR r.occurred_at<>e.occurred_at
 THEN RAISE EXCEPTION 'ASSIGNMENT_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF e.prior_version=0 THEN
   IF e.prior_assignee_account_id IS NOT NULL THEN RAISE EXCEPTION 'ASSIGNMENT_HISTORY_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
   SELECT * INTO prior FROM orders.organization_shipment_assignment_event WHERE shipment_id=s.shipment_id AND new_version=e.prior_version;
   IF NOT FOUND OR prior.assignee_account_id IS DISTINCT FROM e.prior_assignee_account_id OR prior.occurred_at>e.occurred_at
   THEN RAISE EXCEPTION 'ASSIGNMENT_HISTORY_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.organization_id=s.organization_id AND m.account_id=e.actor_account_id AND m.state='ACTIVE'
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=m.id AND g.permission='shipment.assign' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id))
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=m.id AND g.permission='shipment.draft.read' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id)))
 THEN RAISE EXCEPTION 'ASSIGNMENT_ACTOR_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF s.assignee_account_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.organization_id=s.organization_id AND m.account_id=s.assignee_account_id AND m.state='ACTIVE'
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=m.id AND g.permission='shipment.draft.read' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id)))
 THEN RAISE EXCEPTION 'ASSIGNMENT_TARGET_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_assignment_evidence AFTER INSERT OR UPDATE ON orders.organization_shipment_assignment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_assignment_evidence();
CREATE CONSTRAINT TRIGGER organization_assignment_event_evidence AFTER INSERT ON orders.organization_shipment_assignment_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_assignment_evidence();
CREATE CONSTRAINT TRIGGER organization_assignment_receipt_evidence AFTER INSERT ON orders.organization_shipment_assignment_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_assignment_evidence();

CREATE FUNCTION orders.guard_organization_export_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE member UUID;
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT id INTO member FROM merchant.membership WHERE organization_id=NEW.organization_id AND account_id=NEW.actor_account_id AND state='ACTIVE';
 IF member IS NULL OR NOT EXISTS(SELECT 1 FROM merchant.member_grant WHERE membership_id=member AND permission='shipment.export')
   OR (SELECT count(DISTINCT id) FROM unnest(NEW.shipment_ids) id)<>NEW.row_count
   OR (SELECT count(*) FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.shipment_ids) AND s.organization_id=NEW.organization_id AND s.owner_type='BUSINESS')<>NEW.row_count
   OR EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.shipment_ids) AND (
     NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission='shipment.draft.read' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id))
     OR NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission='shipment.export' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id))
     OR (NEW.pii_included AND NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission='shipment.export.pii' AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id)))))
 THEN RAISE EXCEPTION 'EXPORT_AUDIT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER organization_export_audit_guard BEFORE INSERT ON orders.organization_shipment_export_audit FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_export_audit();

REVOKE ALL ON orders.organization_shipment_assignment,orders.organization_shipment_assignment_event,orders.organization_shipment_assignment_receipt,orders.organization_shipment_export_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.organization_shipment_assignment TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.organization_shipment_assignment_event,orders.organization_shipment_assignment_receipt TO "${runtimeRole}";
GRANT INSERT ON orders.organization_shipment_export_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.organization_management_append_only(),orders.guard_organization_assignment(),orders.check_organization_assignment_evidence() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_organization_export_audit() FROM PUBLIC,"${runtimeRole}";

-- Preserve the existing audited operator signature/receipt digest and old permissions.
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
 OR cardinality(p_permissions)>16 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign'))
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
