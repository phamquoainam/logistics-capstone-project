-- NWS-V1 / NWS-ACL-02. No site, assignment, account or runtime operator membership seed.
DO $$ DECLARE n TEXT; BEGIN
 FOREACH n IN ARRAY ARRAY['dispatch_network_writer','merchant_coverage_reader','elogistics_local_operations_operator'] LOOP
  IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname=n AND NOT rolcanlogin AND NOT rolsuper AND NOT rolcreaterole AND NOT rolcreatedb AND NOT rolreplication AND NOT rolbypassrls)
  OR pg_has_role('${runtimeRole}',n,'MEMBER') THEN RAISE EXCEPTION 'NWS_ROLE_PREREQUISITE_REQUIRED' USING ERRCODE='42501';END IF;
 END LOOP;
 IF NOT has_function_privilege('dispatch_network_writer','pg_catalog.pg_control_system()','EXECUTE')
 OR has_function_privilege('${runtimeRole}','pg_catalog.pg_control_system()','EXECUTE')
 OR has_function_privilege('elogistics_local_operations_operator','pg_catalog.pg_control_system()','EXECUTE') THEN
 RAISE EXCEPTION 'NWS_CONTROL_ACL_REQUIRED' USING ERRCODE='42501'; END IF;
END $$;
GRANT USAGE ON SCHEMA dispatch,identity,merchant TO dispatch_network_writer;
GRANT USAGE ON SCHEMA dispatch TO elogistics_local_operations_operator;
GRANT USAGE ON SCHEMA merchant TO merchant_coverage_reader;
GRANT SELECT ON merchant.pilot_coverage_dataset,merchant.pilot_area,merchant.pilot_route TO merchant_coverage_reader;
GRANT CREATE ON SCHEMA merchant TO merchant_coverage_reader;
CREATE FUNCTION merchant.pilot_drop_off_bound(p_dataset TEXT,p_service TEXT,p_area TEXT,p_drop TEXT) RETURNS BOOLEAN
LANGUAGE sql SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT EXISTS(SELECT 1 FROM merchant.pilot_coverage_dataset d JOIN merchant.pilot_area a ON a.dataset_version=d.version
 WHERE d.version=p_dataset AND d.version='ARGO-SIM-COVERAGE-1' AND d.provenance='SIMULATION' AND p_service='ROAD_STANDARD'
 AND d.effective_from<=clock_timestamp() AND (d.effective_to IS NULL OR d.effective_to>clock_timestamp())
 AND a.code=p_area AND a.drop_off_code=p_drop AND EXISTS(SELECT 1 FROM merchant.pilot_route r WHERE r.dataset_version=d.version AND r.origin_code=a.code AND r.service_code=p_service))
$$;
REVOKE ALL ON FUNCTION merchant.pilot_drop_off_bound(TEXT,TEXT,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.pilot_drop_off_bound(TEXT,TEXT,TEXT,TEXT) TO dispatch_network_writer;
ALTER FUNCTION merchant.pilot_drop_off_bound(TEXT,TEXT,TEXT,TEXT) OWNER TO merchant_coverage_reader;
REVOKE CREATE ON SCHEMA merchant FROM merchant_coverage_reader;
SET ROLE identity_staff_writer;
GRANT EXECUTE ON FUNCTION identity.operations_staff_eligible(UUID),identity.lock_local_operations_staff_subject(UUID,BOOLEAN),identity.local_staff_environment_matches(TEXT) TO dispatch_network_writer;
-- Restore the canonical schema owner after the private writer block, not session_user.
DO $$ BEGIN EXECUTE format('SET LOCAL ROLE %I',(SELECT pg_get_userbyid(nspowner) FROM pg_namespace WHERE nspname='dispatch')); END $$;

CREATE TABLE dispatch.carrier_site (
 id UUID PRIMARY KEY, code VARCHAR(32) NOT NULL UNIQUE CHECK(code ~ '^[A-Z0-9][A-Z0-9-]{0,31}$'),
 type VARCHAR(7) NOT NULL CHECK(type IN ('STATION','HUB')),created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE dispatch.carrier_site_version (
 id UUID PRIMARY KEY,site_id UUID NOT NULL REFERENCES dispatch.carrier_site(id) ON DELETE RESTRICT,
 version BIGINT NOT NULL CHECK(version>0),name VARCHAR(120) NOT NULL CHECK(length(btrim(name))>0),
 location_text VARCHAR(500) NOT NULL CHECK(length(btrim(location_text))>0),area_code VARCHAR(32) NOT NULL,
 latitude NUMERIC(9,6) CHECK(latitude BETWEEN -90 AND 90),longitude NUMERIC(10,6) CHECK(longitude BETWEEN -180 AND 180),
 timezone VARCHAR(64) NOT NULL,operating_hours JSONB NOT NULL CHECK(jsonb_typeof(operating_hours)='object' AND octet_length(operating_hours::text)<=2048),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','INACTIVE')),capabilities TEXT[] NOT NULL CHECK(capabilities<@ARRAY['SITE_READ','STATION_INTAKE']::text[]),
 effective_from TIMESTAMPTZ NOT NULL,effective_to TIMESTAMPTZ CHECK(effective_to>effective_from),
 environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),provenance VARCHAR(10) NOT NULL CHECK(provenance='SIMULATION'),
 UNIQUE(site_id,version),UNIQUE(site_id,effective_from),UNIQUE(id,site_id)
);
CREATE TABLE dispatch.carrier_site_link (
 id UUID PRIMARY KEY,link_id UUID NOT NULL,from_site_id UUID NOT NULL REFERENCES dispatch.carrier_site(id),to_site_id UUID NOT NULL REFERENCES dispatch.carrier_site(id),
 service_code VARCHAR(32) NOT NULL,dataset_version VARCHAR(64) NOT NULL,area_code VARCHAR(32) NOT NULL,drop_off_code VARCHAR(32) NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),effective_from TIMESTAMPTZ NOT NULL,effective_to TIMESTAMPTZ CHECK(effective_to>effective_from),
 CHECK(from_site_id<>to_site_id),UNIQUE(link_id,version),UNIQUE(link_id,effective_from)
);
CREATE TABLE dispatch.operational_site_assignment (
 id UUID PRIMARY KEY,account_id UUID NOT NULL REFERENCES identity.user_account(id) ON DELETE RESTRICT,
 site_id UUID NOT NULL REFERENCES dispatch.carrier_site(id) ON DELETE RESTRICT,
 permission VARCHAR(32) NOT NULL CHECK(permission IN ('SITE_READ','STATION_INTAKE')),state VARCHAR(7) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')),
 effective_from TIMESTAMPTZ NOT NULL,effective_to TIMESTAMPTZ NOT NULL CHECK(effective_to>effective_from),version BIGINT NOT NULL CHECK(version>0),UNIQUE(id,account_id,site_id)
);
CREATE UNIQUE INDEX uq_operational_site_active ON dispatch.operational_site_assignment(account_id,site_id,permission) WHERE state='ACTIVE';
CREATE TABLE dispatch.network_operator_audit (
 id UUID PRIMARY KEY,request_id UUID NOT NULL UNIQUE,operator_principal NAME NOT NULL,operation VARCHAR(24) NOT NULL,
 target_id UUID NOT NULL,before_version BIGINT,after_version BIGINT NOT NULL CHECK(after_version>0),reason VARCHAR(300) NOT NULL,occurred_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE dispatch.network_operator_receipt (
 request_id UUID PRIMARY KEY,operator_principal NAME NOT NULL,operation VARCHAR(24) NOT NULL CHECK(operation IN ('SITE','LINK','GRANT','REVOKE')),target_id UUID NOT NULL,
 payload_digest BYTEA NOT NULL CHECK(octet_length(payload_digest)=32),environment VARCHAR(5) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 system_identifier NUMERIC(20,0) NOT NULL,database_oid OID NOT NULL,database_name NAME NOT NULL,result_version BIGINT NOT NULL CHECK(result_version>0),
 audit_id UUID NOT NULL UNIQUE REFERENCES dispatch.network_operator_audit(id) DEFERRABLE INITIALLY DEFERRED,created_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(request_id) REFERENCES dispatch.network_operator_audit(request_id) DEFERRABLE INITIALLY DEFERRED
);
ALTER TABLE dispatch.network_operator_audit ADD CONSTRAINT fk_network_audit_receipt FOREIGN KEY(request_id) REFERENCES dispatch.network_operator_receipt(request_id) DEFERRABLE INITIALLY DEFERRED;
REVOKE ALL ON dispatch.carrier_site,dispatch.carrier_site_version,dispatch.carrier_site_link,dispatch.operational_site_assignment,dispatch.network_operator_audit,dispatch.network_operator_receipt FROM PUBLIC;
GRANT SELECT ON dispatch.carrier_site,dispatch.carrier_site_version,dispatch.carrier_site_link,dispatch.operational_site_assignment TO "${runtimeRole}";
GRANT SELECT,INSERT ON dispatch.carrier_site,dispatch.carrier_site_version,dispatch.carrier_site_link,dispatch.operational_site_assignment,dispatch.network_operator_audit,dispatch.network_operator_receipt TO dispatch_network_writer;
GRANT UPDATE(state,version) ON dispatch.operational_site_assignment TO dispatch_network_writer;
GRANT CREATE ON SCHEMA dispatch TO dispatch_network_writer;

CREATE FUNCTION dispatch.guard_carrier_site_identity() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$ BEGIN
 IF current_user<>'dispatch_network_writer' OR TG_OP<>'INSERT' THEN RAISE EXCEPTION 'SITE_IDENTITY_IMMUTABLE' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(730021,1);RETURN NEW;
END $$;
CREATE TRIGGER carrier_site_identity BEFORE INSERT OR UPDATE OR DELETE ON dispatch.carrier_site FOR EACH ROW EXECUTE FUNCTION dispatch.guard_carrier_site_identity();
CREATE FUNCTION dispatch.guard_carrier_site_version() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
DECLARE previous dispatch.carrier_site_version; kind TEXT; h RECORD; i JSONB; last_end TEXT; start_time TEXT; finish TEXT;
BEGIN
 IF current_user<>'dispatch_network_writer' OR TG_OP<>'INSERT' THEN RAISE EXCEPTION 'SITE_VERSION_IMMUTABLE' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(730021,1);
 SELECT * INTO previous FROM dispatch.carrier_site_version WHERE site_id=NEW.site_id ORDER BY version DESC LIMIT 1;
 IF NEW.version<>COALESCE(previous.version,0)+1 OR (previous.id IS NOT NULL AND NEW.effective_from<=previous.effective_from)
 OR NEW.effective_from<transaction_timestamp() OR NOT identity.local_staff_environment_matches(NEW.environment)
 OR NOT EXISTS(SELECT 1 FROM pg_timezone_names WHERE name=NEW.timezone) THEN RAISE EXCEPTION 'SITE_VERSION_INVALID' USING ERRCODE='23514';END IF;
 SELECT type INTO kind FROM dispatch.carrier_site WHERE id=NEW.site_id;
 IF kind<>'STATION' AND 'STATION_INTAKE'=ANY(NEW.capabilities) THEN RAISE EXCEPTION 'SITE_CAPABILITY_INVALID' USING ERRCODE='23514';END IF;
 IF cardinality(NEW.capabilities)<>(SELECT count(DISTINCT c) FROM unnest(NEW.capabilities) c) OR NEW.capabilities IS NULL THEN RAISE EXCEPTION 'SITE_CAPABILITY_INVALID' USING ERRCODE='23514';END IF;
 FOR h IN SELECT * FROM jsonb_each(NEW.operating_hours) LOOP
  IF h.key NOT IN ('MON','TUE','WED','THU','FRI','SAT','SUN') OR jsonb_typeof(h.value)<>'array' OR jsonb_array_length(h.value)>2 THEN RAISE EXCEPTION 'SITE_HOURS_INVALID' USING ERRCODE='23514';END IF;
  last_end:=NULL;
  FOR i IN SELECT * FROM jsonb_array_elements(h.value) LOOP
   IF jsonb_typeof(i)<>'string' OR (i#>>'{}') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]-([01][0-9]|2[0-3]):[0-5][0-9]$' THEN RAISE EXCEPTION 'SITE_HOURS_INVALID' USING ERRCODE='23514';END IF;
   start_time:=substring(i#>>'{}',1,5);finish:=substring(i#>>'{}',7,5);
   IF start_time>=finish OR (last_end IS NOT NULL AND start_time<last_end) THEN RAISE EXCEPTION 'SITE_HOURS_INVALID' USING ERRCODE='23514';END IF;last_end:=finish;
  END LOOP;
 END LOOP;RETURN NEW;
END $$;
CREATE TRIGGER carrier_site_version BEFORE INSERT OR UPDATE OR DELETE ON dispatch.carrier_site_version FOR EACH ROW EXECUTE FUNCTION dispatch.guard_carrier_site_version();
CREATE FUNCTION dispatch.guard_carrier_site_link() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
DECLARE previous dispatch.carrier_site_link; BEGIN
 IF current_user<>'dispatch_network_writer' OR TG_OP<>'INSERT' THEN RAISE EXCEPTION 'SITE_LINK_IMMUTABLE' USING ERRCODE='42501';END IF;
 PERFORM pg_advisory_xact_lock(730021,1);
 SELECT * INTO previous FROM dispatch.carrier_site_link WHERE link_id=NEW.link_id ORDER BY version DESC LIMIT 1;
 IF NEW.version<>COALESCE(previous.version,0)+1 OR NEW.effective_from<transaction_timestamp()
 OR (previous.id IS NOT NULL AND (NEW.effective_from<=previous.effective_from OR ROW(NEW.from_site_id,NEW.to_site_id) IS DISTINCT FROM ROW(previous.from_site_id,previous.to_site_id)))
 OR NOT merchant.pilot_drop_off_bound(NEW.dataset_version,NEW.service_code,NEW.area_code,NEW.drop_off_code)
 OR NOT EXISTS(SELECT 1 FROM dispatch.carrier_site s JOIN dispatch.carrier_site_version v ON v.site_id=s.id WHERE s.id=NEW.from_site_id AND s.code=NEW.drop_off_code AND v.area_code=NEW.area_code AND v.state='ACTIVE' AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=s.id AND x.effective_from<=NEW.effective_from))
 OR NOT EXISTS(SELECT 1 FROM dispatch.carrier_site_version v WHERE v.site_id=NEW.to_site_id AND v.state='ACTIVE' AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=v.site_id AND x.effective_from<=NEW.effective_from)) THEN RAISE EXCEPTION 'SITE_LINK_INVALID' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER carrier_site_link BEFORE INSERT OR UPDATE OR DELETE ON dispatch.carrier_site_link FOR EACH ROW EXECUTE FUNCTION dispatch.guard_carrier_site_link();
CREATE FUNCTION dispatch.guard_operational_site_assignment() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$ BEGIN
 IF current_user<>'dispatch_network_writer' OR TG_OP='DELETE' THEN RAISE EXCEPTION 'SITE_ASSIGNMENT_WRITE_DENIED' USING ERRCODE='42501';END IF;
 PERFORM identity.lock_local_operations_staff_subject(NEW.account_id,TG_OP='INSERT');PERFORM pg_advisory_xact_lock(730021,1);
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'ACTIVE' OR NEW.version<>1 OR NOT EXISTS(SELECT 1 FROM dispatch.carrier_site_version v WHERE v.site_id=NEW.site_id AND v.state='ACTIVE' AND NEW.permission=ANY(v.capabilities)
   AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=v.site_id AND x.effective_from<=NEW.effective_from) AND (v.effective_to IS NULL OR v.effective_to>NEW.effective_from)) THEN RAISE EXCEPTION 'SITE_ASSIGNMENT_INVALID' USING ERRCODE='23514';END IF;
 ELSE
  IF OLD.state<>'ACTIVE' OR NEW.state<>'REVOKED' OR NEW.version<>OLD.version+1
  OR ROW(NEW.id,NEW.account_id,NEW.site_id,NEW.permission,NEW.effective_from,NEW.effective_to) IS DISTINCT FROM ROW(OLD.id,OLD.account_id,OLD.site_id,OLD.permission,OLD.effective_from,OLD.effective_to) THEN RAISE EXCEPTION 'SITE_ASSIGNMENT_IMMUTABLE' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER operational_site_assignment BEFORE INSERT OR UPDATE OR DELETE ON dispatch.operational_site_assignment FOR EACH ROW EXECUTE FUNCTION dispatch.guard_operational_site_assignment();
CREATE FUNCTION dispatch.guard_network_operator_evidence() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE target UUID; revision BIGINT; r dispatch.network_operator_receipt; a dispatch.network_operator_audit;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'NETWORK_EVIDENCE_IMMUTABLE' USING ERRCODE='42501';END IF;
 IF TG_TABLE_NAME='network_operator_receipt' THEN r:=NEW;
 ELSIF TG_TABLE_NAME='network_operator_audit' THEN SELECT * INTO r FROM dispatch.network_operator_receipt WHERE request_id=NEW.request_id;
 ELSE
  IF TG_TABLE_NAME='carrier_site' THEN target:=NEW.id;revision:=1;
  ELSIF TG_TABLE_NAME='carrier_site_version' THEN target:=NEW.site_id;revision:=NEW.version;
  ELSIF TG_TABLE_NAME='carrier_site_link' THEN target:=NEW.link_id;revision:=NEW.version;
  ELSE target:=NEW.id;revision:=NEW.version;END IF;
  SELECT * INTO r FROM dispatch.network_operator_receipt WHERE target_id=target AND result_version=revision AND xmin::text=pg_current_xact_id()::text;
 END IF;
 SELECT * INTO a FROM dispatch.network_operator_audit WHERE id=r.audit_id;
 IF r.request_id IS NULL OR r.operator_principal<>session_user OR NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') OR a.id IS NULL OR a.request_id<>r.request_id OR a.operator_principal<>r.operator_principal OR a.operation<>r.operation OR a.target_id<>r.target_id OR a.after_version<>r.result_version OR r.database_name<>current_database()
 OR r.database_oid<>(SELECT oid FROM pg_database WHERE datname=current_database()) OR r.system_identifier<>(SELECT system_identifier FROM pg_control_system())
 OR NOT identity.local_staff_environment_matches(r.environment) THEN RAISE EXCEPTION 'NETWORK_RECEIPT_AUDIT_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER network_identity_evidence AFTER INSERT ON dispatch.carrier_site DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();
CREATE CONSTRAINT TRIGGER network_version_evidence AFTER INSERT ON dispatch.carrier_site_version DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();
CREATE CONSTRAINT TRIGGER network_link_evidence AFTER INSERT ON dispatch.carrier_site_link DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();
CREATE CONSTRAINT TRIGGER network_assignment_evidence AFTER INSERT OR UPDATE ON dispatch.operational_site_assignment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();
CREATE CONSTRAINT TRIGGER network_receipt_evidence AFTER INSERT OR UPDATE OR DELETE ON dispatch.network_operator_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();
CREATE CONSTRAINT TRIGGER network_audit_evidence AFTER INSERT OR UPDATE OR DELETE ON dispatch.network_operator_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.guard_network_operator_evidence();

ALTER TABLE dispatch.pilot_intake_scan ALTER COLUMN assignment_id DROP NOT NULL;
ALTER TABLE dispatch.pilot_intake_acceptance ALTER COLUMN assignment_id DROP NOT NULL;
ALTER TABLE dispatch.pilot_intake_scan ADD COLUMN operational_assignment_id UUID,ADD COLUMN site_version_id UUID,ADD COLUMN site_id UUID,
 ADD CONSTRAINT ck_scan_authority_branch CHECK((assignment_id IS NOT NULL AND operational_assignment_id IS NULL AND site_version_id IS NULL AND site_id IS NULL) OR (assignment_id IS NULL AND operational_assignment_id IS NOT NULL AND site_version_id IS NOT NULL AND site_id IS NOT NULL)),
 ADD CONSTRAINT fk_scan_ops_assignment FOREIGN KEY(operational_assignment_id,actor_id,site_id) REFERENCES dispatch.operational_site_assignment(id,account_id,site_id),
 ADD CONSTRAINT fk_scan_ops_site_version FOREIGN KEY(site_version_id,site_id) REFERENCES dispatch.carrier_site_version(id,site_id);
ALTER TABLE dispatch.pilot_intake_acceptance ADD COLUMN operational_assignment_id UUID,ADD COLUMN site_version_id UUID,ADD COLUMN site_id UUID,
 ADD CONSTRAINT ck_accept_authority_branch CHECK((assignment_id IS NOT NULL AND operational_assignment_id IS NULL AND site_version_id IS NULL AND site_id IS NULL) OR (assignment_id IS NULL AND operational_assignment_id IS NOT NULL AND site_version_id IS NOT NULL AND site_id IS NOT NULL)),
 ADD CONSTRAINT fk_accept_ops_assignment FOREIGN KEY(operational_assignment_id,actor_id,site_id) REFERENCES dispatch.operational_site_assignment(id,account_id,site_id),
 ADD CONSTRAINT fk_accept_ops_site_version FOREIGN KEY(site_version_id,site_id) REFERENCES dispatch.carrier_site_version(id,site_id);
GRANT SELECT ON dispatch.pilot_intake_scan,dispatch.pilot_intake_acceptance TO dispatch_network_writer;
CREATE FUNCTION dispatch.require_operational_intake_authority() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE stamp TIMESTAMPTZ; BEGIN
 IF NEW.operational_assignment_id IS NULL THEN RETURN NEW;END IF;
 PERFORM identity.lock_local_operations_staff_subject(NEW.actor_id,TRUE);PERFORM pg_advisory_xact_lock(730021,1);stamp:=clock_timestamp();
 IF NOT EXISTS(SELECT 1 FROM dispatch.operational_site_assignment a JOIN dispatch.carrier_site s ON s.id=a.site_id JOIN dispatch.carrier_site_version v ON v.site_id=s.id
 WHERE a.id=NEW.operational_assignment_id AND a.account_id=NEW.actor_id AND a.site_id=NEW.site_id AND a.permission='STATION_INTAKE' AND a.state='ACTIVE' AND a.effective_from<=stamp AND a.effective_to>stamp
 AND v.id=NEW.site_version_id AND v.environment=NEW.environment AND v.state='ACTIVE' AND s.type='STATION' AND s.code=NEW.station_code AND 'STATION_INTAKE'=ANY(v.capabilities)
 AND v.version=(SELECT max(x.version) FROM dispatch.carrier_site_version x WHERE x.site_id=s.id AND x.effective_from<=stamp) AND (v.effective_to IS NULL OR v.effective_to>stamp)
 AND EXISTS(SELECT 1 FROM dispatch.operational_site_assignment ar WHERE ar.account_id=a.account_id AND ar.site_id=a.site_id AND ar.permission='SITE_READ' AND ar.state='ACTIVE' AND ar.effective_from<=stamp AND ar.effective_to>stamp)
 AND EXISTS(SELECT 1 FROM dispatch.carrier_site_link l WHERE l.from_site_id=s.id AND l.drop_off_code=s.code AND l.effective_from<=stamp AND (l.effective_to IS NULL OR l.effective_to>stamp)
 AND l.version=(SELECT max(x.version) FROM dispatch.carrier_site_link x WHERE x.link_id=l.link_id AND x.effective_from<=stamp) AND merchant.pilot_drop_off_bound(l.dataset_version,l.service_code,l.area_code,l.drop_off_code))) THEN RAISE EXCEPTION 'OPS_INTAKE_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER scan_operational_authority BEFORE INSERT ON dispatch.pilot_intake_scan FOR EACH ROW EXECUTE FUNCTION dispatch.require_operational_intake_authority();
CREATE TRIGGER acceptance_operational_authority BEFORE INSERT ON dispatch.pilot_intake_acceptance FOR EACH ROW EXECUTE FUNCTION dispatch.require_operational_intake_authority();
CREATE CONSTRAINT TRIGGER scan_operational_commit_authority AFTER INSERT ON dispatch.pilot_intake_scan DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.require_operational_intake_authority();
CREATE CONSTRAINT TRIGGER acceptance_operational_commit_authority AFTER INSERT ON dispatch.pilot_intake_acceptance DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.require_operational_intake_authority();

CREATE FUNCTION dispatch.configure_local_carrier_site(p_request UUID,p_site UUID,p_expected BIGINT,p_config JSONB,p_reason TEXT) RETURNS TABLE(site_id UUID,version BIGINT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE old dispatch.network_operator_receipt; digest BYTEA; env TEXT; sys NUMERIC(20,0); db OID; target UUID; revision BIGINT; audit UUID; stamp TIMESTAMPTZ; previous BIGINT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') THEN RAISE EXCEPTION 'NETWORK_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 env:=CASE WHEN identity.local_staff_environment_matches('LOCAL') THEN 'LOCAL' WHEN identity.local_staff_environment_matches('TEST') THEN 'TEST' END;
 IF env IS NULL OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_expected IS NULL OR p_expected<0 OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason<>btrim(p_reason) OR p_reason ~ '[[:cntrl:]]' THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 SELECT system_identifier INTO sys FROM pg_control_system();SELECT oid INTO db FROM pg_database WHERE datname=current_database();
 PERFORM pg_advisory_xact_lock(730021,1);stamp:=clock_timestamp();
 digest:=sha256(convert_to(jsonb_build_array(session_user,sys,db,current_database(),env,'SITE',p_site,p_expected,p_config,p_reason)::text,'UTF8'));
 SELECT * INTO old FROM dispatch.network_operator_receipt WHERE request_id=p_request;
 IF FOUND THEN
  IF old.operator_principal<>session_user OR old.payload_digest<>digest THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT old.target_id,old.result_version,TRUE;RETURN;
 END IF;

 IF p_config IS NULL OR jsonb_typeof(p_config)<>'object' OR (SELECT array_agg(k ORDER BY k) FROM jsonb_object_keys(p_config) k)<>ARRAY['areaCode','capabilities','code','effectiveFrom','effectiveTo','environment','latitude','locationText','longitude','name','operatingHours','provenance','state','timezone','type'] OR p_config->>'environment' IS DISTINCT FROM env THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 target:=p_site;
 IF target IS NULL THEN
  IF p_expected<>0 THEN RAISE EXCEPTION 'NETWORK_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
  target:=gen_random_uuid();INSERT INTO dispatch.carrier_site VALUES(target,p_config->>'code',p_config->>'type',stamp);previous:=0;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM dispatch.carrier_site WHERE id=target AND code=p_config->>'code' AND type=p_config->>'type') THEN RAISE EXCEPTION 'SITE_NOT_FOUND' USING ERRCODE='23514';END IF;
  SELECT max(v.version) INTO previous FROM dispatch.carrier_site_version v WHERE v.site_id=target;
 END IF;
 IF previous IS DISTINCT FROM p_expected THEN RAISE EXCEPTION 'NETWORK_VERSION_CONFLICT' USING ERRCODE='23514';END IF;revision:=previous+1;
 INSERT INTO dispatch.carrier_site_version VALUES(gen_random_uuid(),target,revision,p_config->>'name',p_config->>'locationText',p_config->>'areaCode',(p_config->>'latitude')::NUMERIC,(p_config->>'longitude')::NUMERIC,p_config->>'timezone',p_config->'operatingHours',p_config->>'state',ARRAY(SELECT jsonb_array_elements_text(p_config->'capabilities')),COALESCE((p_config->>'effectiveFrom')::TIMESTAMPTZ,stamp),(p_config->>'effectiveTo')::TIMESTAMPTZ,env,p_config->>'provenance');
audit:=gen_random_uuid();
 INSERT INTO dispatch.network_operator_audit VALUES(audit,p_request,session_user,'SITE',target,previous,revision,p_reason,stamp);
 INSERT INTO dispatch.network_operator_receipt VALUES(p_request,session_user,'SITE',target,digest,env,sys,db,current_database(),revision,audit,stamp);
 RETURN QUERY SELECT target,revision,FALSE;
END $$;

CREATE FUNCTION dispatch.configure_local_site_link(p_request UUID,p_link UUID,p_expected BIGINT,p_config JSONB,p_reason TEXT) RETURNS TABLE(link_id UUID,version BIGINT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE old dispatch.network_operator_receipt; digest BYTEA; env TEXT; sys NUMERIC(20,0); db OID; target UUID; revision BIGINT; audit UUID; stamp TIMESTAMPTZ; previous BIGINT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') THEN RAISE EXCEPTION 'NETWORK_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 env:=CASE WHEN identity.local_staff_environment_matches('LOCAL') THEN 'LOCAL' WHEN identity.local_staff_environment_matches('TEST') THEN 'TEST' END;
 IF env IS NULL OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_expected IS NULL OR p_expected<0 OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason<>btrim(p_reason) OR p_reason ~ '[[:cntrl:]]' THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 SELECT system_identifier INTO sys FROM pg_control_system();SELECT oid INTO db FROM pg_database WHERE datname=current_database();
 PERFORM pg_advisory_xact_lock(730021,1);stamp:=clock_timestamp();
 digest:=sha256(convert_to(jsonb_build_array(session_user,sys,db,current_database(),env,'LINK',p_link,p_expected,p_config,p_reason)::text,'UTF8'));
 SELECT * INTO old FROM dispatch.network_operator_receipt WHERE request_id=p_request;
 IF FOUND THEN
  IF old.operator_principal<>session_user OR old.payload_digest<>digest THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT old.target_id,old.result_version,TRUE;RETURN;
 END IF;

 IF p_config IS NULL OR jsonb_typeof(p_config)<>'object' OR (SELECT array_agg(k ORDER BY k) FROM jsonb_object_keys(p_config) k)<>ARRAY['areaCode','datasetVersion','dropOffCode','effectiveFrom','effectiveTo','fromSiteId','serviceCode','toSiteId'] THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 target:=COALESCE(p_link,gen_random_uuid());SELECT COALESCE(max(v.version),0) INTO previous FROM dispatch.carrier_site_link v WHERE v.link_id=target;
 IF previous<>p_expected OR (p_link IS NULL AND p_expected<>0) THEN RAISE EXCEPTION 'NETWORK_VERSION_CONFLICT' USING ERRCODE='23514';END IF;revision:=previous+1;
 INSERT INTO dispatch.carrier_site_link VALUES(gen_random_uuid(),target,(p_config->>'fromSiteId')::UUID,(p_config->>'toSiteId')::UUID,p_config->>'serviceCode',p_config->>'datasetVersion',p_config->>'areaCode',p_config->>'dropOffCode',revision,COALESCE((p_config->>'effectiveFrom')::TIMESTAMPTZ,stamp),(p_config->>'effectiveTo')::TIMESTAMPTZ);
audit:=gen_random_uuid();
 INSERT INTO dispatch.network_operator_audit VALUES(audit,p_request,session_user,'LINK',target,previous,revision,p_reason,stamp);
 INSERT INTO dispatch.network_operator_receipt VALUES(p_request,session_user,'LINK',target,digest,env,sys,db,current_database(),revision,audit,stamp);
 RETURN QUERY SELECT target,revision,FALSE;
END $$;

CREATE FUNCTION dispatch.configure_local_operational_assignment(p_request UUID,p_account UUID,p_site UUID,p_permission TEXT,p_action TEXT,p_from TIMESTAMPTZ,p_to TIMESTAMPTZ,p_expected BIGINT,p_reason TEXT) RETURNS TABLE(assignment_id UUID,version BIGINT,replayed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE old dispatch.network_operator_receipt; digest BYTEA; env TEXT; sys NUMERIC(20,0); db OID; target UUID; revision BIGINT; audit UUID; stamp TIMESTAMPTZ; previous BIGINT;
BEGIN
 IF NOT pg_has_role(session_user,'elogistics_local_operations_operator','MEMBER') THEN RAISE EXCEPTION 'NETWORK_OPERATOR_DENIED' USING ERRCODE='42501';END IF;
 env:=CASE WHEN identity.local_staff_environment_matches('LOCAL') THEN 'LOCAL' WHEN identity.local_staff_environment_matches('TEST') THEN 'TEST' END;
 IF env IS NULL OR p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_expected IS NULL OR p_expected<0 OR p_reason IS NULL OR length(p_reason) NOT BETWEEN 1 AND 300 OR p_reason<>btrim(p_reason) OR p_reason ~ '[[:cntrl:]]' THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 SELECT system_identifier INTO sys FROM pg_control_system();SELECT oid INTO db FROM pg_database WHERE datname=current_database();
 IF p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE') OR p_permission IS NULL OR p_permission NOT IN ('SITE_READ','STATION_INTAKE') OR p_from IS NULL OR p_to IS NULL OR p_to<=p_from THEN RAISE EXCEPTION 'NETWORK_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM identity.lock_local_operations_staff_subject(p_account,p_action='GRANT');
 PERFORM pg_advisory_xact_lock(730021,1);stamp:=clock_timestamp();
 digest:=sha256(convert_to(jsonb_build_array(session_user,sys,db,current_database(),env,p_action,p_account,p_site,p_permission,p_from,p_to,p_expected,p_reason)::text,'UTF8'));
 SELECT * INTO old FROM dispatch.network_operator_receipt WHERE request_id=p_request;
 IF FOUND THEN
  IF old.operator_principal<>session_user OR old.payload_digest<>digest THEN RAISE EXCEPTION 'IDEMPOTENCY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT old.target_id,old.result_version,TRUE;RETURN;
 END IF;

 IF p_action='GRANT' THEN
  IF p_expected<>0 THEN RAISE EXCEPTION 'NETWORK_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
  target:=gen_random_uuid();previous:=0;revision:=1;
  INSERT INTO dispatch.operational_site_assignment VALUES(target,p_account,p_site,p_permission,'ACTIVE',p_from,p_to,revision);
 ELSE
  SELECT a.id,a.version INTO target,previous FROM dispatch.operational_site_assignment a WHERE a.account_id=p_account AND a.site_id=p_site AND a.permission=p_permission AND a.state='ACTIVE' AND a.effective_from=p_from AND a.effective_to=p_to;
  IF target IS NULL OR previous<>p_expected THEN RAISE EXCEPTION 'NETWORK_VERSION_CONFLICT' USING ERRCODE='23514';END IF;
  revision:=previous+1;UPDATE dispatch.operational_site_assignment SET state='REVOKED',version=revision WHERE id=target;
 END IF;
audit:=gen_random_uuid();
 INSERT INTO dispatch.network_operator_audit VALUES(audit,p_request,session_user,p_action,target,previous,revision,p_reason,stamp);
 INSERT INTO dispatch.network_operator_receipt VALUES(p_request,session_user,p_action,target,digest,env,sys,db,current_database(),revision,audit,stamp);
 RETURN QUERY SELECT target,revision,FALSE;
END $$;

REVOKE ALL ON FUNCTION dispatch.configure_local_carrier_site(UUID,UUID,BIGINT,JSONB,TEXT),dispatch.configure_local_site_link(UUID,UUID,BIGINT,JSONB,TEXT),dispatch.configure_local_operational_assignment(UUID,UUID,UUID,TEXT,TEXT,TIMESTAMPTZ,TIMESTAMPTZ,BIGINT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dispatch.configure_local_carrier_site(UUID,UUID,BIGINT,JSONB,TEXT),dispatch.configure_local_site_link(UUID,UUID,BIGINT,JSONB,TEXT),dispatch.configure_local_operational_assignment(UUID,UUID,UUID,TEXT,TEXT,TIMESTAMPTZ,TIMESTAMPTZ,BIGINT,TEXT) TO elogistics_local_operations_operator;
REVOKE ALL ON FUNCTION dispatch.configure_local_carrier_site(UUID,UUID,BIGINT,JSONB,TEXT) FROM PUBLIC;
ALTER FUNCTION dispatch.configure_local_carrier_site(UUID,UUID,BIGINT,JSONB,TEXT) OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.configure_local_site_link(UUID,UUID,BIGINT,JSONB,TEXT) FROM PUBLIC;
ALTER FUNCTION dispatch.configure_local_site_link(UUID,UUID,BIGINT,JSONB,TEXT) OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.configure_local_operational_assignment(UUID,UUID,UUID,TEXT,TEXT,TIMESTAMPTZ,TIMESTAMPTZ,BIGINT,TEXT) FROM PUBLIC;
ALTER FUNCTION dispatch.configure_local_operational_assignment(UUID,UUID,UUID,TEXT,TEXT,TIMESTAMPTZ,TIMESTAMPTZ,BIGINT,TEXT) OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.guard_carrier_site_identity() FROM PUBLIC;
ALTER FUNCTION dispatch.guard_carrier_site_identity() OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.guard_carrier_site_version() FROM PUBLIC;
ALTER FUNCTION dispatch.guard_carrier_site_version() OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.guard_carrier_site_link() FROM PUBLIC;
ALTER FUNCTION dispatch.guard_carrier_site_link() OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.guard_operational_site_assignment() FROM PUBLIC;
ALTER FUNCTION dispatch.guard_operational_site_assignment() OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.guard_network_operator_evidence() FROM PUBLIC;
ALTER FUNCTION dispatch.guard_network_operator_evidence() OWNER TO dispatch_network_writer;
REVOKE ALL ON FUNCTION dispatch.require_operational_intake_authority() FROM PUBLIC;
ALTER FUNCTION dispatch.require_operational_intake_authority() OWNER TO dispatch_network_writer;
REVOKE CREATE ON SCHEMA dispatch FROM dispatch_network_writer;
