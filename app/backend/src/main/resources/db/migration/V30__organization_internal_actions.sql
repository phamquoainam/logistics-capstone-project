-- Owner accepted OAA V1 internal Merchant work. No Carrier/Finance authority.
GRANT USAGE ON SCHEMA notification TO "${runtimeRole}";
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read',
 'contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond'));
ALTER TABLE merchant.member_grant DROP CONSTRAINT preparation_scope_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK(
 (scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND
 permission IN ('shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare',
 'shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign',
 'shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export',
 'shipment.actions.manage','shipment.actions.respond')));
CREATE FUNCTION merchant.check_action_grants() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID:=CASE WHEN TG_OP='DELETE' THEN OLD.organization_id ELSE NEW.organization_id END;
BEGIN
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission IN ('shipment.actions.manage','shipment.actions.respond')
   AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='shipment.draft.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id)))
 THEN RAISE EXCEPTION 'ACTION_READ_DEPENDENCY_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER action_grant_dependencies AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_action_grants();
REVOKE ALL ON FUNCTION merchant.check_action_grants() FROM PUBLIC,"${runtimeRole}";

CREATE TABLE orders.organization_action_task (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID,
 preparation_id UUID, shipment_id UUID, type VARCHAR(24) NOT NULL,
 title VARCHAR(160) NOT NULL CHECK(length(btrim(title))>0 AND title !~ '[[:cntrl:]]'),
 details VARCHAR(2000) NOT NULL CHECK(length(btrim(details))>0 AND details !~ '[[:cntrl:]]'),
 state VARCHAR(16) NOT NULL CHECK(state IN ('OPEN','IN_PROGRESS','AWAITING_REVIEW','COMPLETED','CANCELLED')),
 priority VARCHAR(6) NOT NULL CHECK(priority IN ('LOW','NORMAL','HIGH')), assignee_account_id UUID,
 due_at TIMESTAMPTZ, initial_due_at TIMESTAMPTZ, version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), completed_at TIMESTAMPTZ,
 CHECK((state='COMPLETED' AND completed_at=updated_at) OR (state<>'COMPLETED' AND completed_at IS NULL)),
 CHECK((preparation_id IS NOT NULL AND shipment_id IS NULL AND type='PREPARATION_REVIEW') OR (preparation_id IS NULL AND shipment_id IS NOT NULL AND type='SHIPMENT_FOLLOW_UP')),
 FOREIGN KEY(preparation_id,organization_id) REFERENCES orders.organization_preparation(id,organization_id),
 FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(organization_id,assignee_account_id) REFERENCES merchant.membership(organization_id,account_id), UNIQUE(id,organization_id)
);
CREATE TABLE orders.organization_action_event (
 id UUID PRIMARY KEY, task_id UUID NOT NULL, organization_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0),
 operation VARCHAR(16) NOT NULL CHECK(operation IN ('CREATE','ASSIGNMENT','COMMENT','START','REQUEST_REVIEW','COMPLETE','REOPEN','CANCEL','POLICY')),
 actor_account_id UUID NOT NULL, operation_key UUID NOT NULL, request_id UUID NOT NULL,
 message VARCHAR(2000) CHECK(message IS NULL OR (length(btrim(message))>0 AND message !~ '[[:cntrl:]]')),
 snapshot JSONB NOT NULL CHECK(jsonb_typeof(snapshot)='object'), occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(task_id,organization_id) REFERENCES orders.organization_action_task(id,organization_id),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 UNIQUE(task_id,version), UNIQUE(id,organization_id)
);
CREATE TABLE orders.organization_action_command_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(16) NOT NULL, operation_key UUID NOT NULL,
 task_id UUID NOT NULL, facility_id UUID, expected_version BIGINT NOT NULL CHECK(expected_version>=0),
 result_version BIGINT NOT NULL CHECK(result_version=expected_version+1), event_id UUID NOT NULL,
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), occurred_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,operation,operation_key), UNIQUE(task_id,result_version),
 FOREIGN KEY(event_id,organization_id) REFERENCES orders.organization_action_event(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(task_id,result_version) REFERENCES orders.organization_action_event(task_id,version) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
ALTER TABLE orders.organization_action_task ADD CONSTRAINT action_current_event FOREIGN KEY(id,version) REFERENCES orders.organization_action_event(task_id,version) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE orders.organization_action_event ADD CONSTRAINT action_event_receipt FOREIGN KEY(organization_id,actor_account_id,operation,operation_key) REFERENCES orders.organization_action_command_receipt(organization_id,actor_account_id,operation,operation_key) DEFERRABLE INITIALLY DEFERRED;

CREATE TABLE notification.organization_action_notice (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, task_id UUID NOT NULL, event_id UUID NOT NULL, recipient_account_id UUID NOT NULL, facility_id UUID,
 type VARCHAR(16) NOT NULL CHECK(type IN ('ASSIGNMENT','COMMENT','POLICY','REOPEN')), created_at TIMESTAMPTZ NOT NULL, read_at TIMESTAMPTZ CHECK(read_at>=created_at),
 FOREIGN KEY(task_id,organization_id) REFERENCES orders.organization_action_task(id,organization_id),
 FOREIGN KEY(event_id,organization_id) REFERENCES orders.organization_action_event(id,organization_id),
 FOREIGN KEY(organization_id,recipient_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id), UNIQUE(event_id,recipient_account_id)
);
CREATE INDEX organization_action_queue ON orders.organization_action_task(organization_id,facility_id,state,updated_at DESC,id);
CREATE INDEX organization_action_assignee ON orders.organization_action_task(organization_id,assignee_account_id,state,due_at);
CREATE INDEX organization_action_notice_own ON notification.organization_action_notice(organization_id,recipient_account_id,created_at DESC,id);
CREATE TRIGGER organization_action_event_append BEFORE UPDATE OR DELETE ON orders.organization_action_event FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();
CREATE TRIGGER organization_action_receipt_append BEFORE UPDATE OR DELETE ON orders.organization_action_command_receipt FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();

CREATE FUNCTION orders.guard_organization_action() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ACTION_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.state<>'OPEN' OR NEW.assignee_account_id IS NOT NULL OR NEW.created_at<>NEW.updated_at OR NEW.due_at IS DISTINCT FROM NEW.initial_due_at
   OR (NEW.due_at IS NOT NULL AND NEW.due_at<=NEW.created_at)
   OR (NEW.preparation_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM orders.organization_preparation d WHERE d.id=NEW.preparation_id AND d.organization_id=NEW.organization_id AND d.state='DRAFT' AND d.facility_id IS NOT DISTINCT FROM NEW.facility_id))
   OR (NEW.shipment_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=NEW.shipment_id AND s.organization_id=NEW.organization_id AND s.owner_type='BUSINESS' AND s.booking_state='CONFIRMED' AND s.facility_id IS NOT DISTINCT FROM NEW.facility_id))
  THEN RAISE EXCEPTION 'ACTION_INITIAL_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at OR
   (to_jsonb(NEW)-ARRAY['version','updated_at','state','priority','assignee_account_id','due_at','completed_at'])<>(to_jsonb(OLD)-ARRAY['version','updated_at','state','priority','assignee_account_id','due_at','completed_at'])
  THEN RAISE EXCEPTION 'ACTION_IMMUTABLE_TARGET_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_action_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_action_task FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_action();

CREATE FUNCTION orders.check_organization_action_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE t orders.organization_action_task%ROWTYPE; e orders.organization_action_event%ROWTYPE; r orders.organization_action_command_receipt%ROWTYPE;
 prior JSONB; managed BOOLEAN; responded BOOLEAN; before_state TEXT; permitted TEXT[]; recipient UUID; current_facility UUID;
BEGIN
 IF TG_TABLE_NAME='organization_action_task' THEN SELECT * INTO t FROM orders.organization_action_task WHERE id=NEW.id;
 ELSE SELECT * INTO t FROM orders.organization_action_task WHERE id=NEW.task_id; END IF;
 SELECT * INTO e FROM orders.organization_action_event WHERE task_id=t.id AND version=t.version;
 SELECT * INTO r FROM orders.organization_action_command_receipt WHERE task_id=t.id AND result_version=t.version;
 IF e.snapshot IS DISTINCT FROM to_jsonb(t) OR e.organization_id IS DISTINCT FROM t.organization_id OR e.occurred_at IS DISTINCT FROM t.updated_at
  OR r.organization_id IS DISTINCT FROM t.organization_id OR r.facility_id IS DISTINCT FROM t.facility_id OR r.event_id IS DISTINCT FROM e.id
  OR r.actor_account_id IS DISTINCT FROM e.actor_account_id OR r.operation IS DISTINCT FROM e.operation OR r.operation_key IS DISTINCT FROM e.operation_key OR r.occurred_at IS DISTINCT FROM e.occurred_at
 THEN RAISE EXCEPTION 'ACTION_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME<>'organization_action_task' THEN
  IF TG_TABLE_NAME='organization_action_event' THEN
   IF NEW.version<>t.version THEN RAISE EXCEPTION 'ACTION_EVENT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF;
  ELSE IF NEW.result_version<>t.version THEN RAISE EXCEPTION 'ACTION_RECEIPT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF; END IF;
 END IF;
 SELECT CASE WHEN t.preparation_id IS NOT NULL THEN (SELECT facility_id FROM orders.organization_preparation WHERE id=t.preparation_id) ELSE (SELECT facility_id FROM orders.pilot_shipment WHERE id=t.shipment_id) END INTO current_facility;
 IF NOT merchant.contact_scope(t.organization_id,e.actor_account_id,t.facility_id,'shipment.draft.read') OR NOT merchant.contact_scope(t.organization_id,e.actor_account_id,current_facility,'shipment.draft.read')
 THEN RAISE EXCEPTION 'ACTION_CURRENT_READ_REQUIRED' USING ERRCODE='23514'; END IF;
 managed:=merchant.contact_scope(t.organization_id,e.actor_account_id,t.facility_id,'shipment.actions.manage') AND merchant.contact_scope(t.organization_id,e.actor_account_id,current_facility,'shipment.actions.manage');
 responded:=t.assignee_account_id=e.actor_account_id AND merchant.contact_scope(t.organization_id,e.actor_account_id,t.facility_id,'shipment.actions.respond') AND merchant.contact_scope(t.organization_id,e.actor_account_id,current_facility,'shipment.actions.respond');
 IF t.version=1 THEN
  IF e.operation<>'CREATE' OR NOT managed OR e.message IS NOT NULL THEN RAISE EXCEPTION 'ACTION_CREATE_AUTHORITY_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  SELECT snapshot INTO prior FROM orders.organization_action_event WHERE task_id=t.id AND version=t.version-1;
  IF prior IS NULL THEN RAISE EXCEPTION 'ACTION_HISTORY_REQUIRED' USING ERRCODE='23514'; END IF;before_state:=prior->>'state';
  IF e.operation='ASSIGNMENT' THEN
   IF NOT managed OR before_state IN ('COMPLETED','CANCELLED') OR t.state<>before_state OR (prior->>'assignee_account_id') IS NOT DISTINCT FROM (to_jsonb(t)->>'assignee_account_id') THEN RAISE EXCEPTION 'ACTION_ASSIGNMENT_DENIED' USING ERRCODE='23514'; END IF;
   permitted:=ARRAY['assignee_account_id'];
   IF t.assignee_account_id IS NOT NULL AND (NOT merchant.contact_scope(t.organization_id,t.assignee_account_id,t.facility_id,'shipment.draft.read') OR NOT merchant.contact_scope(t.organization_id,t.assignee_account_id,t.facility_id,'shipment.actions.respond')
     OR NOT merchant.contact_scope(t.organization_id,t.assignee_account_id,current_facility,'shipment.draft.read') OR NOT merchant.contact_scope(t.organization_id,t.assignee_account_id,current_facility,'shipment.actions.respond'))
   THEN RAISE EXCEPTION 'ACTION_ASSIGNEE_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
  ELSIF e.operation='POLICY' THEN
   IF NOT managed OR before_state IN ('COMPLETED','CANCELLED') OR (t.due_at IS NOT NULL AND t.due_at<=t.updated_at) THEN RAISE EXCEPTION 'ACTION_POLICY_DENIED' USING ERRCODE='23514'; END IF;permitted:=ARRAY['priority','due_at'];
  ELSIF e.operation='COMMENT' THEN
   IF (NOT managed AND NOT responded) OR before_state IN ('COMPLETED','CANCELLED') THEN RAISE EXCEPTION 'ACTION_COMMENT_DENIED' USING ERRCODE='23514'; END IF;permitted:=ARRAY[]::TEXT[];
  ELSE
   IF NOT ((e.operation='START' AND responded AND before_state='OPEN' AND t.state='IN_PROGRESS')
    OR (e.operation='REQUEST_REVIEW' AND responded AND before_state='IN_PROGRESS' AND t.state='AWAITING_REVIEW')
    OR (e.operation='COMPLETE' AND managed AND before_state='AWAITING_REVIEW' AND t.state='COMPLETED')
    OR (e.operation='REOPEN' AND managed AND before_state IN ('COMPLETED','AWAITING_REVIEW') AND t.state='OPEN')
    OR (e.operation='CANCEL' AND managed AND before_state NOT IN ('COMPLETED','CANCELLED') AND t.state='CANCELLED'))
   THEN RAISE EXCEPTION 'ACTION_TRANSITION_DENIED' USING ERRCODE='23514'; END IF;permitted:=ARRAY['state','completed_at'];
  END IF;
  IF e.message IS NULL OR length(btrim(e.message))<1 OR (to_jsonb(t)-(permitted||ARRAY['version','updated_at']))<>(prior-(permitted||ARRAY['version','updated_at']))
  THEN RAISE EXCEPTION 'ACTION_EVENT_CONTENT_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF e.operation IN ('ASSIGNMENT','COMMENT','POLICY','REOPEN') AND t.assignee_account_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM notification.organization_action_notice n WHERE n.event_id=e.id AND n.task_id=t.id AND n.organization_id=t.organization_id
    AND n.recipient_account_id=t.assignee_account_id AND n.facility_id IS NOT DISTINCT FROM t.facility_id AND n.type=e.operation AND n.created_at=e.occurred_at)
 THEN RAISE EXCEPTION 'ACTION_NOTICE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_action_evidence AFTER INSERT OR UPDATE ON orders.organization_action_task DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_action_evidence();
CREATE CONSTRAINT TRIGGER organization_action_event_evidence AFTER INSERT ON orders.organization_action_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_action_evidence();
CREATE CONSTRAINT TRIGGER organization_action_receipt_evidence AFTER INSERT ON orders.organization_action_command_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_action_evidence();

CREATE FUNCTION notification.guard_organization_action_notice() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE e orders.organization_action_event%ROWTYPE;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ACTION_NOTICE_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.read_at IS NOT NULL OR NEW.read_at IS NULL OR (to_jsonb(NEW)-'read_at')<>(to_jsonb(OLD)-'read_at') THEN RAISE EXCEPTION 'ACTION_NOTICE_IMMUTABLE' USING ERRCODE='23514'; END IF;
 ELSE
  SELECT * INTO e FROM orders.organization_action_event WHERE id=NEW.event_id AND organization_id=NEW.organization_id;
  IF e.id IS NULL OR e.task_id<>NEW.task_id OR e.operation<>NEW.type OR e.snapshot->>'assignee_account_id' IS DISTINCT FROM NEW.recipient_account_id::TEXT
   OR (e.snapshot->>'facility_id')::UUID IS DISTINCT FROM NEW.facility_id OR NEW.created_at<>e.occurred_at OR NEW.read_at IS NOT NULL
  THEN RAISE EXCEPTION 'ACTION_NOTICE_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_action_notice_guard BEFORE INSERT OR UPDATE OR DELETE ON notification.organization_action_notice FOR EACH ROW EXECUTE FUNCTION notification.guard_organization_action_notice();
REVOKE ALL ON orders.organization_action_task,orders.organization_action_event,orders.organization_action_command_receipt,notification.organization_action_notice FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.organization_action_task,notification.organization_action_notice TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.organization_action_event,orders.organization_action_command_receipt TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_organization_action(),orders.check_organization_action_evidence(),notification.guard_organization_action_notice() FROM PUBLIC,"${runtimeRole}";

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
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond'))
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
