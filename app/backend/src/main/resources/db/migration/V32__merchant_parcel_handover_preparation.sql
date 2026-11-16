-- WFP V1 internal Merchant preparation only. No Carrier, Finance, pickup or inventory writes.
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','shipment.handover.read','shipment.handover.manage'));
ALTER TABLE merchant.member_grant DROP CONSTRAINT preparation_scope_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK((scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND permission IN (
 'shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','facility.read','facility.manage','shipment.handover.read','shipment.handover.manage')));
ALTER TABLE orders.pilot_shipment ADD CONSTRAINT merchant_work_shipment_site UNIQUE(id,organization_id,facility_id);
ALTER TABLE orders.pilot_parcel ADD CONSTRAINT merchant_work_parcel_shipment UNIQUE(id,shipment_id);
CREATE FUNCTION orders.merchant_parcel_fingerprint(shipment UUID) RETURNS BYTEA LANGUAGE sql STABLE AS $$
 SELECT sha256(convert_to(COALESCE(jsonb_agg(to_jsonb(p) ORDER BY parcel_number),'[]'::JSONB)::TEXT,'UTF8')) FROM orders.pilot_parcel p WHERE shipment_id=shipment
$$;
CREATE TABLE orders.merchant_parcel_preparation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, facility_id UUID NOT NULL, shipment_id UUID NOT NULL UNIQUE,
 state VARCHAR(18) NOT NULL CHECK(state IN ('READY_TO_PACK','PACKED','READY_TO_HANDOVER')), version BIGINT NOT NULL CHECK(version>0), cycle BIGINT NOT NULL CHECK(cycle>0),
 source_fingerprint BYTEA NOT NULL CHECK(octet_length(source_fingerprint)=32), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 UNIQUE(id,organization_id,facility_id), UNIQUE(id,shipment_id),
 FOREIGN KEY(shipment_id,organization_id,facility_id) REFERENCES orders.pilot_shipment(id,organization_id,facility_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE orders.merchant_preparation_check (
 preparation_id UUID NOT NULL, shipment_id UUID NOT NULL, parcel_id UUID NOT NULL, result VARCHAR(6) NOT NULL CHECK(result IN ('OK','VERIFY')), note VARCHAR(300), cycle BIGINT NOT NULL CHECK(cycle>0),
 PRIMARY KEY(preparation_id,parcel_id), FOREIGN KEY(preparation_id,shipment_id) REFERENCES orders.merchant_parcel_preparation(id,shipment_id),
 FOREIGN KEY(parcel_id,shipment_id) REFERENCES orders.pilot_parcel(id,shipment_id), CHECK(note IS NULL OR (length(btrim(note))>0 AND note !~ '[[:cntrl:]]'))
);
CREATE TABLE orders.merchant_handover_preparation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, facility_id UUID NOT NULL, state VARCHAR(9) NOT NULL CHECK(state IN ('DRAFT','READY','CANCELLED')),
 version BIGINT NOT NULL CHECK(version>0), cycle BIGINT NOT NULL CHECK(cycle>0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 UNIQUE(id,organization_id,facility_id), FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE orders.merchant_handover_shipment (
 batch_id UUID NOT NULL, organization_id UUID NOT NULL, facility_id UUID NOT NULL, shipment_id UUID NOT NULL, active BOOLEAN NOT NULL,
 PRIMARY KEY(batch_id,shipment_id), FOREIGN KEY(batch_id,organization_id,facility_id) REFERENCES orders.merchant_handover_preparation(id,organization_id,facility_id),
 FOREIGN KEY(shipment_id,organization_id,facility_id) REFERENCES orders.pilot_shipment(id,organization_id,facility_id)
);
CREATE UNIQUE INDEX merchant_one_live_handover ON orders.merchant_handover_shipment(shipment_id) WHERE active;
CREATE TABLE orders.merchant_handover_check (
 batch_id UUID NOT NULL, shipment_id UUID NOT NULL, parcel_id UUID NOT NULL, result VARCHAR(7) NOT NULL CHECK(result IN ('PRESENT','MISSING','VERIFY')), note VARCHAR(300), cycle BIGINT NOT NULL CHECK(cycle>0),
 PRIMARY KEY(batch_id,parcel_id), FOREIGN KEY(batch_id,shipment_id) REFERENCES orders.merchant_handover_shipment(batch_id,shipment_id),
 FOREIGN KEY(parcel_id,shipment_id) REFERENCES orders.pilot_parcel(id,shipment_id), CHECK(note IS NULL OR (length(btrim(note))>0 AND note !~ '[[:cntrl:]]'))
);
CREATE TABLE orders.merchant_facility_work_event (
 resource_kind VARCHAR(11) NOT NULL CHECK(resource_kind IN ('PREPARATION','HANDOVER')), target_id UUID NOT NULL, organization_id UUID NOT NULL, facility_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0),
 actor_account_id UUID NOT NULL, operation VARCHAR(24) NOT NULL, operation_key UUID NOT NULL, parcel_id UUID, reason VARCHAR(300), snapshot JSONB NOT NULL, request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(resource_kind,target_id,version), UNIQUE(organization_id,actor_account_id,operation,operation_key),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id), FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 CHECK(reason IS NULL OR (length(btrim(reason))>0 AND reason !~ '[[:cntrl:]]'))
);
CREATE TABLE orders.merchant_facility_work_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation VARCHAR(24) NOT NULL, operation_key UUID NOT NULL,
 resource_kind VARCHAR(11) NOT NULL, target_id UUID NOT NULL, facility_id UUID NOT NULL, expected_version BIGINT NOT NULL CHECK(expected_version>=0), result_version BIGINT NOT NULL CHECK(result_version=expected_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), occurred_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,operation,operation_key), UNIQUE(resource_kind,target_id,result_version),
 FOREIGN KEY(resource_kind,target_id,result_version) REFERENCES orders.merchant_facility_work_event(resource_kind,target_id,version) DEFERRABLE INITIALLY DEFERRED
);
CREATE FUNCTION orders.merchant_work_snapshot(kind TEXT,target UUID) RETURNS JSONB LANGUAGE plpgsql STABLE AS $$
DECLARE v JSONB;
BEGIN
 IF kind='PREPARATION' THEN
  SELECT to_jsonb(w)||jsonb_build_object('checks',COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY c.parcel_id) FROM orders.merchant_preparation_check c WHERE c.preparation_id=w.id),'[]'::JSONB)) INTO v FROM orders.merchant_parcel_preparation w WHERE w.id=target;
 ELSE
  SELECT to_jsonb(w)||jsonb_build_object('items',COALESCE((SELECT jsonb_agg(to_jsonb(i) ORDER BY i.shipment_id) FROM orders.merchant_handover_shipment i WHERE i.batch_id=w.id),'[]'::JSONB),
   'checks',COALESCE((SELECT jsonb_agg(to_jsonb(c) ORDER BY c.parcel_id) FROM orders.merchant_handover_check c WHERE c.batch_id=w.id),'[]'::JSONB)) INTO v FROM orders.merchant_handover_preparation w WHERE w.id=target;
 END IF;RETURN v;
END $$;
CREATE FUNCTION orders.merchant_work_permission(org UUID,actor UUID,site UUID,kind TEXT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT merchant.contact_scope(org,actor,site,'shipment.draft.read') AND merchant.contact_scope(org,actor,site,CASE WHEN kind='PREPARATION' THEN 'shipment.prepare' ELSE 'shipment.handover.manage' END)
 AND (kind='PREPARATION' OR merchant.contact_scope(org,actor,site,'shipment.handover.read'))
 AND ((merchant.contact_scope(org,actor,site,'facility.manage') AND merchant.contact_scope(org,actor,site,'facility.read')) OR NOT EXISTS(SELECT 1 FROM merchant.facility_responsibility WHERE organization_id=org AND facility_id=site AND responsibility=CASE WHEN kind='PREPARATION' THEN 'PREPARATION' ELSE 'HANDOVER' END)
  OR EXISTS(SELECT 1 FROM merchant.facility_responsibility WHERE organization_id=org AND facility_id=site AND account_id=actor AND responsibility=CASE WHEN kind='PREPARATION' THEN 'PREPARATION' ELSE 'HANDOVER' END))
$$;
CREATE FUNCTION orders.guard_merchant_work() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'MERCHANT_WORK_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=NEW.facility_id AND f.organization_id=NEW.organization_id AND f.state='ACTIVE') THEN RAISE EXCEPTION 'FACILITY_ACTIVE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.cycle<>1 OR NEW.created_at<>NEW.updated_at OR NEW.state<>(CASE WHEN TG_TABLE_NAME='merchant_parcel_preparation' THEN 'READY_TO_PACK' ELSE 'DRAFT' END) THEN RAISE EXCEPTION 'MERCHANT_WORK_INITIAL_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.facility_id<>OLD.facility_id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at OR NEW.cycle NOT BETWEEN OLD.cycle AND OLD.cycle+1 THEN RAISE EXCEPTION 'MERCHANT_WORK_BINDING_IMMUTABLE' USING ERRCODE='23514'; END IF;
 END IF;
 IF TG_TABLE_NAME='merchant_parcel_preparation' THEN
  IF TG_OP='UPDATE' AND (NEW.shipment_id<>OLD.shipment_id OR NEW.source_fingerprint<>OLD.source_fingerprint) THEN RAISE EXCEPTION 'MERCHANT_SOURCE_IMMUTABLE' USING ERRCODE='23514'; END IF;
  IF NOT EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=NEW.shipment_id AND s.organization_id=NEW.organization_id AND s.facility_id=NEW.facility_id AND s.owner_type='BUSINESS' AND s.booking_state='CONFIRMED' AND s.carrier_state='NOT_ACCEPTED') OR NEW.source_fingerprint<>orders.merchant_parcel_fingerprint(NEW.shipment_id) THEN RAISE EXCEPTION 'MERCHANT_SOURCE_NOT_ELIGIBLE' USING ERRCODE='23514'; END IF;
  IF TG_OP='UPDATE' AND NEW.state<>'READY_TO_HANDOVER' AND EXISTS(SELECT 1 FROM orders.merchant_handover_shipment i JOIN orders.merchant_handover_preparation b ON b.id=i.batch_id WHERE i.shipment_id=NEW.shipment_id AND i.active AND b.state='READY') THEN RAISE EXCEPTION 'HANDOVER_REOPEN_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF TG_OP='UPDATE' AND OLD.state='CANCELLED' THEN RAISE EXCEPTION 'HANDOVER_CANCELLED_IMMUTABLE' USING ERRCODE='23514'; END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER merchant_preparation_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.merchant_parcel_preparation FOR EACH ROW EXECUTE FUNCTION orders.guard_merchant_work();
CREATE TRIGGER merchant_handover_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.merchant_handover_preparation FOR EACH ROW EXECUTE FUNCTION orders.guard_merchant_work();
CREATE FUNCTION orders.check_merchant_work() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE kind TEXT; target UUID; current_snapshot JSONB; e orders.merchant_facility_work_event%ROWTYPE; r orders.merchant_facility_work_receipt%ROWTYPE; prior JSONB; action TEXT; p UUID; changed INT;
BEGIN
 IF TG_TABLE_NAME IN ('merchant_facility_work_event','merchant_facility_work_receipt') THEN kind:=NEW.resource_kind;target:=NEW.target_id;
 ELSIF TG_TABLE_NAME='merchant_parcel_preparation' THEN kind:='PREPARATION';target:=NEW.id;
 ELSIF TG_TABLE_NAME='merchant_handover_preparation' THEN kind:='HANDOVER';target:=NEW.id;
 ELSIF TG_TABLE_NAME='merchant_preparation_check' THEN kind:='PREPARATION';IF TG_OP='DELETE' THEN target:=OLD.preparation_id;ELSE target:=NEW.preparation_id;END IF;
 ELSE kind:='HANDOVER';IF TG_OP='DELETE' THEN target:=OLD.batch_id;ELSE target:=NEW.batch_id;END IF;END IF;
 current_snapshot:=orders.merchant_work_snapshot(kind,target);
 IF TG_TABLE_NAME='merchant_facility_work_event' THEN IF NEW.version<>(current_snapshot->>'version')::BIGINT THEN RAISE EXCEPTION 'MERCHANT_EVENT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSIF TG_TABLE_NAME='merchant_facility_work_receipt' THEN IF NEW.result_version<>(current_snapshot->>'version')::BIGINT THEN RAISE EXCEPTION 'MERCHANT_RECEIPT_CURRENT_REQUIRED' USING ERRCODE='23514'; END IF;END IF;
 SELECT * INTO e FROM orders.merchant_facility_work_event WHERE resource_kind=kind AND target_id=target AND version=(current_snapshot->>'version')::BIGINT;
 SELECT * INTO r FROM orders.merchant_facility_work_receipt WHERE resource_kind=kind AND target_id=target AND result_version=e.version;
 IF e.target_id IS NULL OR r.target_id IS NULL OR e.snapshot IS DISTINCT FROM current_snapshot OR e.organization_id IS DISTINCT FROM (current_snapshot->>'organization_id')::UUID OR e.facility_id IS DISTINCT FROM (current_snapshot->>'facility_id')::UUID OR e.occurred_at IS DISTINCT FROM (current_snapshot->>'updated_at')::TIMESTAMPTZ
 OR r.organization_id<>e.organization_id OR r.facility_id<>e.facility_id OR r.actor_account_id<>e.actor_account_id OR r.operation<>e.operation OR r.operation_key<>e.operation_key OR r.occurred_at<>e.occurred_at
 OR NOT orders.merchant_work_permission(e.organization_id,e.actor_account_id,e.facility_id,kind) THEN RAISE EXCEPTION 'MERCHANT_WORK_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 action:=substring(e.operation FROM length(kind)+2);
 IF e.operation<>kind||'_'||action THEN RAISE EXCEPTION 'MERCHANT_WORK_OPERATION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF e.version=1 THEN IF action<>'CREATE' OR current_snapshot->'checks'<>'[]'::JSONB THEN RAISE EXCEPTION 'MERCHANT_WORK_CREATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  SELECT snapshot INTO prior FROM orders.merchant_facility_work_event WHERE resource_kind=kind AND target_id=target AND version=e.version-1;
  IF prior IS NULL THEN RAISE EXCEPTION 'MERCHANT_WORK_PRIOR_REQUIRED' USING ERRCODE='23514'; END IF;
  IF action='CHECK' THEN
   IF e.parcel_id IS NULL OR current_snapshot->>'state'<>prior->>'state' OR current_snapshot->>'state'<>(CASE WHEN kind='PREPARATION' THEN 'READY_TO_PACK' ELSE 'DRAFT' END)
    OR (current_snapshot-ARRAY['checks','version','updated_at'])<>(prior-ARRAY['checks','version','updated_at'])
    OR NOT EXISTS(SELECT 1 FROM jsonb_array_elements(current_snapshot->'checks') c WHERE (c->>'parcel_id')::UUID=e.parcel_id)
   THEN RAISE EXCEPTION 'MERCHANT_CHECK_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
   IF (SELECT COALESCE(jsonb_agg(c ORDER BY c->>'parcel_id'),'[]'::JSONB) FROM jsonb_array_elements(current_snapshot->'checks') c WHERE (c->>'parcel_id')::UUID<>e.parcel_id) IS DISTINCT FROM
      (SELECT COALESCE(jsonb_agg(c ORDER BY c->>'parcel_id'),'[]'::JSONB) FROM jsonb_array_elements(prior->'checks') c WHERE (c->>'parcel_id')::UUID<>e.parcel_id) THEN RAISE EXCEPTION 'MERCHANT_CHECK_SINGLE_PARCEL_REQUIRED' USING ERRCODE='23514'; END IF;
  ELSIF kind='PREPARATION' THEN
   IF NOT ((action='PACK' AND prior->>'state'='READY_TO_PACK' AND current_snapshot->>'state'='PACKED' AND current_snapshot->>'cycle'=prior->>'cycle')
    OR (action='READY' AND prior->>'state'='PACKED' AND current_snapshot->>'state'='READY_TO_HANDOVER' AND current_snapshot->>'cycle'=prior->>'cycle')
    OR (action='REOPEN' AND prior->>'state' IN ('PACKED','READY_TO_HANDOVER') AND current_snapshot->>'state'='READY_TO_PACK' AND (current_snapshot->>'cycle')::BIGINT=(prior->>'cycle')::BIGINT+1 AND current_snapshot->'checks'='[]'::JSONB AND e.reason IS NOT NULL))
    OR (current_snapshot-ARRAY['checks','state','cycle','version','updated_at'])<>(prior-ARRAY['checks','state','cycle','version','updated_at']) OR (action<>'REOPEN' AND current_snapshot->'checks'<>prior->'checks') THEN RAISE EXCEPTION 'MERCHANT_PREPARATION_TRANSITION_DENIED' USING ERRCODE='23514'; END IF;
  ELSE
   IF action='CANCEL' AND (SELECT jsonb_agg(i-'active' ORDER BY i->>'shipment_id') FROM jsonb_array_elements(current_snapshot->'items') i) IS DISTINCT FROM (SELECT jsonb_agg(i-'active' ORDER BY i->>'shipment_id') FROM jsonb_array_elements(prior->'items') i) THEN RAISE EXCEPTION 'MERCHANT_CANCEL_ITEMS_IMMUTABLE' USING ERRCODE='23514'; END IF;
   IF NOT ((action='READY' AND prior->>'state'='DRAFT' AND current_snapshot->>'state'='READY' AND current_snapshot->>'cycle'=prior->>'cycle' AND current_snapshot->'checks'=prior->'checks' AND current_snapshot->'items'=prior->'items')
    OR (action='REOPEN' AND prior->>'state'='READY' AND current_snapshot->>'state'='DRAFT' AND e.reason IS NOT NULL AND (current_snapshot->>'cycle')::BIGINT=(prior->>'cycle')::BIGINT+1 AND current_snapshot->'checks'='[]'::JSONB AND current_snapshot->'items'=prior->'items')
    OR (action='ITEMS' AND prior->>'state' IN ('DRAFT','READY') AND current_snapshot->>'state'='DRAFT' AND (current_snapshot->>'cycle')::BIGINT=(prior->>'cycle')::BIGINT+1 AND current_snapshot->'checks'='[]'::JSONB)
    OR (action='CANCEL' AND prior->>'state' IN ('DRAFT','READY') AND current_snapshot->>'state'='CANCELLED' AND e.reason IS NOT NULL AND current_snapshot->>'cycle'=prior->>'cycle' AND current_snapshot->'checks'=prior->'checks')) THEN RAISE EXCEPTION 'MERCHANT_HANDOVER_TRANSITION_DENIED' USING ERRCODE='23514'; END IF;
  END IF;
 END IF;
 IF kind='PREPARATION' THEN
  IF EXISTS(SELECT 1 FROM orders.merchant_preparation_check c WHERE c.preparation_id=target AND c.cycle<>(current_snapshot->>'cycle')::BIGINT) THEN RAISE EXCEPTION 'MERCHANT_CHECK_CYCLE_REQUIRED' USING ERRCODE='23514'; END IF;
  IF current_snapshot->>'state' IN ('PACKED','READY_TO_HANDOVER') AND EXISTS(SELECT 1 FROM orders.pilot_parcel p LEFT JOIN orders.merchant_preparation_check c ON c.parcel_id=p.id AND c.preparation_id=target WHERE p.shipment_id=(current_snapshot->>'shipment_id')::UUID AND c.result IS DISTINCT FROM 'OK') THEN RAISE EXCEPTION 'MERCHANT_ALL_PARCELS_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF jsonb_array_length(current_snapshot->'items') NOT BETWEEN 1 AND 20 OR EXISTS(SELECT 1 FROM orders.merchant_handover_shipment i WHERE i.batch_id=target AND i.active<>(current_snapshot->>'state'<>'CANCELLED')) THEN RAISE EXCEPTION 'MERCHANT_BATCH_ITEMS_REQUIRED' USING ERRCODE='23514'; END IF;
  IF EXISTS(SELECT 1 FROM orders.merchant_handover_shipment i JOIN orders.pilot_shipment s ON s.id=i.shipment_id WHERE i.batch_id=target AND (s.owner_type<>'BUSINESS' OR s.booking_state<>'CONFIRMED' OR s.carrier_state<>'NOT_ACCEPTED')) THEN RAISE EXCEPTION 'MERCHANT_SOURCE_NOT_ELIGIBLE' USING ERRCODE='23514'; END IF;
  IF (SELECT count(*) FROM orders.merchant_handover_shipment i JOIN orders.pilot_parcel p ON p.shipment_id=i.shipment_id WHERE i.batch_id=target)>100 OR EXISTS(SELECT 1 FROM orders.merchant_handover_check c WHERE c.batch_id=target AND c.cycle<>(current_snapshot->>'cycle')::BIGINT) THEN RAISE EXCEPTION 'MERCHANT_BATCH_LIMIT_OR_CYCLE' USING ERRCODE='23514'; END IF;
  IF current_snapshot->>'state'='READY' AND (EXISTS(SELECT 1 FROM orders.merchant_handover_shipment i LEFT JOIN orders.merchant_parcel_preparation w ON w.shipment_id=i.shipment_id WHERE i.batch_id=target AND (w.state IS DISTINCT FROM 'READY_TO_HANDOVER' OR w.source_fingerprint IS DISTINCT FROM orders.merchant_parcel_fingerprint(i.shipment_id)))
   OR EXISTS(SELECT 1 FROM orders.merchant_handover_shipment i JOIN orders.pilot_parcel p ON p.shipment_id=i.shipment_id LEFT JOIN orders.merchant_handover_check c ON c.batch_id=target AND c.parcel_id=p.id WHERE i.batch_id=target AND c.result IS DISTINCT FROM 'PRESENT')) THEN RAISE EXCEPTION 'MERCHANT_HANDOVER_ALL_PARCELS_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER merchant_preparation_evidence AFTER INSERT OR UPDATE ON orders.merchant_parcel_preparation DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_preparation_check_evidence AFTER INSERT OR UPDATE OR DELETE ON orders.merchant_preparation_check DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_handover_evidence AFTER INSERT OR UPDATE ON orders.merchant_handover_preparation DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_handover_item_evidence AFTER INSERT OR UPDATE OR DELETE ON orders.merchant_handover_shipment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_handover_check_evidence AFTER INSERT OR UPDATE OR DELETE ON orders.merchant_handover_check DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_work_event_evidence AFTER INSERT ON orders.merchant_facility_work_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE CONSTRAINT TRIGGER merchant_work_receipt_evidence AFTER INSERT ON orders.merchant_facility_work_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_merchant_work();
CREATE TRIGGER merchant_work_event_append BEFORE UPDATE OR DELETE ON orders.merchant_facility_work_event FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
CREATE TRIGGER merchant_work_receipt_append BEFORE UPDATE OR DELETE ON orders.merchant_facility_work_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
REVOKE ALL ON orders.merchant_parcel_preparation,orders.merchant_preparation_check,orders.merchant_handover_preparation,orders.merchant_handover_shipment,orders.merchant_handover_check,orders.merchant_facility_work_event,orders.merchant_facility_work_receipt FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.merchant_parcel_preparation,orders.merchant_handover_preparation TO "${runtimeRole}";
GRANT SELECT,INSERT,UPDATE,DELETE ON orders.merchant_preparation_check,orders.merchant_handover_shipment,orders.merchant_handover_check TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.merchant_facility_work_event,orders.merchant_facility_work_receipt TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.merchant_parcel_fingerprint(UUID),orders.merchant_work_snapshot(TEXT,UUID),orders.merchant_work_permission(UUID,UUID,UUID,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.merchant_parcel_fingerprint(UUID),orders.merchant_work_snapshot(TEXT,UUID),orders.merchant_work_permission(UUID,UUID,UUID,TEXT) TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_merchant_work(),orders.check_merchant_work() FROM PUBLIC,"${runtimeRole}";

CREATE OR REPLACE FUNCTION merchant.configure_local_facility_grants(p_request UUID,p_action TEXT,p_org UUID,p_account UUID,p_facility UUID,p_permissions TEXT[],p_reason TEXT) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,merchant AS $$
DECLARE mode TEXT:=current_setting('argopost.operator_environment',TRUE); owner_account UUID; subject UUID; member UUID; digest TEXT; prior merchant.facility_grant_audit%ROWTYPE; permissions TEXT[]; stamp TIMESTAMPTZ:=clock_timestamp();
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) OR COALESCE(mode,'') NOT IN ('LOCAL','TEST') THEN RAISE EXCEPTION 'LOCAL_FACILITY_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_org IS NULL OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE')
  OR p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 8 OR array_position(p_permissions,NULL) IS NOT NULL
  OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('facility.read','facility.manage','defaults.manage','shipment.prepare','shipment.draft.read','shipment.handover.read','shipment.handover.manage')) OR (p_facility IS NOT NULL AND 'defaults.manage'=ANY(p_permissions))
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
CREATE FUNCTION merchant.check_handover_permissions() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID;
BEGIN
 IF TG_OP='DELETE' THEN org:=OLD.organization_id;ELSE org:=NEW.organization_id;END IF;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission='shipment.handover.manage'
  AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='shipment.handover.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id))) THEN RAISE EXCEPTION 'HANDOVER_READ_PERMISSION_REQUIRED' USING ERRCODE='23514'; END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER handover_permission_dependencies AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_handover_permissions();
REVOKE ALL ON FUNCTION merchant.check_handover_permissions() FROM PUBLIC,"${runtimeRole}";

CREATE FUNCTION orders.guard_merchant_work_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE v JSONB;
BEGIN
 v:=orders.merchant_work_snapshot(NEW.resource_kind,NEW.target_id);
 IF NEW.snapshot IS DISTINCT FROM v OR NEW.version IS DISTINCT FROM (v->>'version')::BIGINT OR NEW.organization_id IS DISTINCT FROM (v->>'organization_id')::UUID OR NEW.facility_id IS DISTINCT FROM (v->>'facility_id')::UUID
 THEN RAISE EXCEPTION 'MERCHANT_EVENT_SNAPSHOT_REQUIRED' USING ERRCODE='23514'; END IF;RETURN NEW;
END $$;
CREATE TRIGGER merchant_work_event_guard BEFORE INSERT ON orders.merchant_facility_work_event FOR EACH ROW EXECUTE FUNCTION orders.guard_merchant_work_event();
REVOKE ALL ON FUNCTION orders.guard_merchant_work_event() FROM PUBLIC,"${runtimeRole}";
