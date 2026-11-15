-- Owner accepted Reports/Finance V1. No source, ledger, grant or historical migration rewrite.
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission IN (
 'dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write',
 'shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign',
 'shipment.reports.read','shipment.finance.read'));

CREATE FUNCTION orders.valid_report_export_query(v JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE entry RECORD; first_date DATE; last_date DATE;
BEGIN
 IF v IS NULL OR jsonb_typeof(v)<>'object' OR NOT v ?& ARRAY['from','until','interval'] THEN RETURN FALSE; END IF;
 FOR entry IN SELECT * FROM jsonb_each(v) LOOP
  IF entry.key NOT IN ('from','until','interval','service','origin','destination','facilityId','creatorId','assignee','sourceChannel','batchId','status','obligation','handover')
   OR jsonb_typeof(entry.value)<>'string' OR length(entry.value#>>'{}') NOT BETWEEN 1 AND 160 OR entry.value#>>'{}' ~ '[[:cntrl:]]' THEN RETURN FALSE; END IF;
 END LOOP;
 IF v->>'interval' NOT IN ('DAY','WEEK','MONTH','QUARTER','YEAR') OR v->>'from' !~ '^\d{4}-\d{2}-\d{2}$' OR v->>'until' !~ '^\d{4}-\d{2}-\d{2}$' THEN RETURN FALSE; END IF;
 first_date:=(v->>'from')::date;last_date:=(v->>'until')::date;
 RETURN last_date>first_date AND last_date-first_date<=366;
EXCEPTION WHEN invalid_datetime_format OR datetime_field_overflow THEN RETURN FALSE;
END $$;
CREATE TABLE orders.organization_report_export_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 actor_account_id UUID NOT NULL, request_id UUID NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK(kind IN ('OPERATIONS','FINANCE')),
 section VARCHAR(10) NOT NULL CHECK(section IN ('trend','facility','creator','channel','service','route')),
 canonical_query JSONB NOT NULL CHECK(orders.valid_report_export_query(canonical_query) IS TRUE),
 query_digest BYTEA NOT NULL CHECK(octet_length(query_digest)=32),
 contributing_shipment_ids UUID[] NOT NULL CHECK(cardinality(contributing_shipment_ids)<=10000 AND array_position(contributing_shipment_ids,NULL) IS NULL),
 row_count INTEGER NOT NULL CHECK(row_count BETWEEN 0 AND 1000), occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE INDEX organization_report_history_actor ON orders.organization_report_export_audit(organization_id,actor_account_id,occurred_at DESC,id DESC);
CREATE FUNCTION orders.guard_report_export_audit() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE member UUID; permission_name TEXT; selected_site UUID;
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT m.id INTO member FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
  WHERE m.organization_id=NEW.organization_id AND m.account_id=NEW.actor_account_id AND m.state='ACTIVE'
   AND o.state='ACTIVE' AND o.environment IN ('LOCAL','TEST') AND o.admission_origin='LOCAL_OPERATOR_PROVISIONED';
 permission_name:=CASE WHEN NEW.kind='FINANCE' THEN 'shipment.finance.read' ELSE 'shipment.reports.read' END;
 IF member IS NULL OR (SELECT count(DISTINCT v) FROM unnest(NEW.contributing_shipment_ids) v)<>cardinality(NEW.contributing_shipment_ids)
  OR (SELECT count(*) FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.contributing_shipment_ids) AND s.organization_id=NEW.organization_id AND s.owner_type='BUSINESS')<>cardinality(NEW.contributing_shipment_ids)
 THEN RAISE EXCEPTION 'REPORT_EXPORT_BINDING_REQUIRED' USING ERRCODE='23514'; END IF;
 IF EXISTS(SELECT 1 FROM orders.pilot_shipment s CROSS JOIN unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p
  WHERE s.id=ANY(NEW.contributing_shipment_ids) AND NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=s.facility_id)))
 THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF NEW.canonical_query ? 'facilityId' THEN
  selected_site:=(NEW.canonical_query->>'facilityId')::uuid;
  IF NOT EXISTS(SELECT 1 FROM merchant.facility WHERE id=selected_site AND organization_id=NEW.organization_id)
   OR EXISTS(SELECT 1 FROM unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p WHERE NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=selected_site)))
   OR EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=ANY(NEW.contributing_shipment_ids) AND s.facility_id IS DISTINCT FROM selected_site)
  THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;
 IF cardinality(NEW.contributing_shipment_ids)=0 AND NOT (
  (SELECT count(DISTINCT permission) FROM merchant.member_grant WHERE membership_id=member AND scope='ORGANIZATION' AND permission=ANY(ARRAY['shipment.draft.read',permission_name,'shipment.export']))=3
  OR EXISTS(SELECT 1 FROM merchant.facility f WHERE f.organization_id=NEW.organization_id AND (selected_site IS NULL OR f.id=selected_site)
   AND NOT EXISTS(SELECT 1 FROM unnest(ARRAY['shipment.draft.read',permission_name,'shipment.export']) p WHERE NOT EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission=p AND (g.scope='ORGANIZATION' OR g.facility_id=f.id)))))
 THEN RAISE EXCEPTION 'REPORT_EXPORT_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF NEW.occurred_at<(NEW.canonical_query->>'from')::date::timestamp AT TIME ZONE 'Asia/Ho_Chi_Minh'
  OR (NEW.canonical_query->>'until')::date>(NEW.occurred_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::date+1
 THEN RAISE EXCEPTION 'REPORT_EXPORT_PERIOD_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER report_export_guard BEFORE INSERT ON orders.organization_report_export_audit FOR EACH ROW EXECUTE FUNCTION orders.guard_report_export_audit();
CREATE TRIGGER report_export_append BEFORE UPDATE OR DELETE ON orders.organization_report_export_audit FOR EACH ROW EXECUTE FUNCTION orders.organization_management_append_only();
REVOKE ALL ON orders.organization_report_export_audit FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT ON orders.organization_report_export_audit TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.valid_report_export_query(JSONB),orders.guard_report_export_audit() FROM PUBLIC,"${runtimeRole}";
GRANT EXECUTE ON FUNCTION orders.valid_report_export_query(JSONB) TO "${runtimeRole}";

-- Preserve operator signature, old receipts and explicit grants; no automatic grant.

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
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read'))
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
