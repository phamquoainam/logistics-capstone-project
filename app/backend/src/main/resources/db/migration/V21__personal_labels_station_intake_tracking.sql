-- Owner-authorized Milestone B, LOCAL/TEST SIMULATION only. No Staff/Customer fixture seed.
GRANT USAGE ON SCHEMA dispatch TO "${runtimeRole}";
CREATE TABLE dispatch.pilot_station (
 code VARCHAR(32) PRIMARY KEY CHECK(code IN ('SIM-DROP-A1','SIM-DROP-A2','SIM-DROP-B1')),
 dataset_version VARCHAR(64) NOT NULL CHECK(dataset_version='ARGO-SIM-COVERAGE-1'),
 active BOOLEAN NOT NULL, drop_off_enabled BOOLEAN NOT NULL, pickup_enabled BOOLEAN NOT NULL CHECK(NOT pickup_enabled),
 provenance VARCHAR(16) NOT NULL CHECK(provenance='SIMULATION'), version BIGINT NOT NULL CHECK(version>0)
);
INSERT INTO dispatch.pilot_station VALUES
 ('SIM-DROP-A1','ARGO-SIM-COVERAGE-1',TRUE,TRUE,FALSE,'SIMULATION',1),
 ('SIM-DROP-A2','ARGO-SIM-COVERAGE-1',TRUE,TRUE,FALSE,'SIMULATION',1),
 ('SIM-DROP-B1','ARGO-SIM-COVERAGE-1',TRUE,TRUE,FALSE,'SIMULATION',1);
CREATE TABLE dispatch.pilot_station_assignment (
 id UUID PRIMARY KEY, account_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL REFERENCES dispatch.pilot_station(code),
 permission VARCHAR(32) NOT NULL CHECK(permission='STATION_INTAKE'), state VARCHAR(16) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')),
 effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ CHECK(effective_to>effective_from), version BIGINT NOT NULL CHECK(version>0),
 UNIQUE(id,account_id,station_code)
);
CREATE UNIQUE INDEX one_current_pilot_station_assignment ON dispatch.pilot_station_assignment(account_id,station_code) WHERE state='ACTIVE';
CREATE TABLE dispatch.pilot_station_assignment_audit (
 id UUID PRIMARY KEY, assignment_id UUID NOT NULL REFERENCES dispatch.pilot_station_assignment(id), account_id UUID NOT NULL,
 station_code VARCHAR(32) NOT NULL, operation VARCHAR(8) NOT NULL CHECK(operation IN ('GRANT','REVOKE')),
 operator_principal TEXT NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL CHECK(version>0)
);
-- Revocation/capability updates use the same lock as runtime admission, even for owner SQL.
CREATE FUNCTION dispatch.lock_pilot_authority_change() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730021,1);
 IF TG_OP='UPDATE' THEN
  IF NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'STATION_AUTHORITY_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
  IF TG_TABLE_NAME='pilot_station_assignment' THEN
   IF NEW.id<>OLD.id OR NEW.account_id<>OLD.account_id OR NEW.station_code<>OLD.station_code OR NEW.permission<>OLD.permission THEN
    RAISE EXCEPTION 'STATION_ASSIGNMENT_IDENTITY_IMMUTABLE' USING ERRCODE='23514';
   END IF;
  END IF;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION dispatch.lock_pilot_authority_change() FROM PUBLIC;
CREATE TRIGGER guard_pilot_station_authority BEFORE INSERT OR UPDATE OR DELETE ON dispatch.pilot_station FOR EACH ROW EXECUTE FUNCTION dispatch.lock_pilot_authority_change();
CREATE TRIGGER guard_pilot_staff_authority BEFORE INSERT OR UPDATE OR DELETE ON dispatch.pilot_station_assignment FOR EACH ROW EXECUTE FUNCTION dispatch.lock_pilot_authority_change();
-- Explicit owner/migrator action only. No runtime EXECUTE, self-grant, account creation or fixture seed.
CREATE FUNCTION dispatch.configure_pilot_station_staff(target UUID,station TEXT,action TEXT) RETURNS UUID LANGUAGE plpgsql AS $$
DECLARE assigned UUID; revision BIGINT;
BEGIN
 IF current_setting('argopost.operator_environment',TRUE) IS NULL OR current_setting('argopost.operator_environment',TRUE) NOT IN ('LOCAL','TEST') OR target IS NULL OR target='00000000-0000-0000-0000-000000000000'::uuid OR action IS NULL OR action NOT IN ('GRANT','REVOKE') THEN RAISE EXCEPTION 'EXPLICIT_LOCAL_STAFF_DISPOSITION_REQUIRED' USING ERRCODE='23514'; END IF;
 PERFORM pg_advisory_xact_lock(730021,1);
 IF NOT EXISTS(SELECT 1 FROM dispatch.pilot_station WHERE code=station AND active AND drop_off_enabled AND NOT pickup_enabled) THEN RAISE EXCEPTION 'APPROVED_STATION_REQUIRED' USING ERRCODE='23514'; END IF;
 SELECT id,version INTO assigned,revision FROM dispatch.pilot_station_assignment WHERE account_id=target AND station_code=station AND state='ACTIVE';
 IF action='GRANT' THEN
  IF assigned IS NOT NULL THEN RETURN assigned; END IF;
  assigned=gen_random_uuid();revision=1;
  INSERT INTO dispatch.pilot_station_assignment VALUES(assigned,target,station,'STATION_INTAKE','ACTIVE',clock_timestamp(),NULL,revision);
 ELSE
  IF assigned IS NULL THEN RETURN NULL; END IF;
  revision=revision+1;UPDATE dispatch.pilot_station_assignment SET state='REVOKED',version=revision WHERE id=assigned;
 END IF;
 INSERT INTO dispatch.pilot_station_assignment_audit VALUES(gen_random_uuid(),assigned,target,station,action,session_user,clock_timestamp(),revision);
 RETURN assigned;
END $$;
REVOKE ALL ON FUNCTION dispatch.configure_pilot_station_staff(UUID,TEXT,TEXT) FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON dispatch.pilot_station_assignment_audit FROM PUBLIC,"${runtimeRole}";
CREATE TABLE dispatch.pilot_intake_scan (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL, assignment_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL,
 shipment_id UUID NOT NULL, parcel_id UUID NOT NULL, scanned_at TIMESTAMPTZ NOT NULL, request_id UUID NOT NULL,
 environment VARCHAR(8) NOT NULL CHECK(environment IN ('LOCAL','TEST')), provenance VARCHAR(16) NOT NULL CHECK(provenance='SIMULATION'),
 FOREIGN KEY(assignment_id,actor_id,station_code) REFERENCES dispatch.pilot_station_assignment(id,account_id,station_code),
 UNIQUE(actor_id,station_code,parcel_id), UNIQUE(id,parcel_id,shipment_id,station_code,actor_id)
);
CREATE TABLE dispatch.pilot_intake_acceptance (
 id UUID PRIMARY KEY, shipment_id UUID NOT NULL UNIQUE, actor_id UUID NOT NULL, assignment_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL,
 parcel_count INTEGER NOT NULL CHECK(parcel_count BETWEEN 1 AND 5), occurred_at TIMESTAMPTZ NOT NULL, request_id UUID NOT NULL,
 expected_version BIGINT NOT NULL CHECK(expected_version=0), inspection VARCHAR(16) NOT NULL CHECK(inspection='MATCHED'),
 policy_version VARCHAR(64) NOT NULL CHECK(policy_version='BR13-LOCAL-INTAKE-01'), environment VARCHAR(8) NOT NULL CHECK(environment IN ('LOCAL','TEST')),
 provenance VARCHAR(16) NOT NULL CHECK(provenance='SIMULATION'),
 FOREIGN KEY(assignment_id,actor_id,station_code) REFERENCES dispatch.pilot_station_assignment(id,account_id,station_code), UNIQUE(id,shipment_id,station_code,actor_id)
);
CREATE TABLE dispatch.pilot_custody (
 acceptance_id UUID NOT NULL, parcel_id UUID PRIMARY KEY, scan_id UUID NOT NULL UNIQUE, shipment_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL,
 actor_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(acceptance_id,shipment_id,station_code,actor_id) REFERENCES dispatch.pilot_intake_acceptance(id,shipment_id,station_code,actor_id),
 FOREIGN KEY(scan_id,parcel_id,shipment_id,station_code,actor_id) REFERENCES dispatch.pilot_intake_scan(id,parcel_id,shipment_id,station_code,actor_id)
);
CREATE TABLE dispatch.pilot_intake_receipt (
 actor_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL REFERENCES dispatch.pilot_station(code), operation_key UUID NOT NULL,
 operation VARCHAR(8) NOT NULL CHECK(operation IN ('SCAN','ACCEPT')), fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 target_id UUID NOT NULL, shipment_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(actor_id,station_code,operation_key)
);
CREATE TABLE dispatch.pilot_intake_audit (
 id UUID PRIMARY KEY, actor_id UUID NOT NULL, station_code VARCHAR(32) NOT NULL REFERENCES dispatch.pilot_station(code),
 operation VARCHAR(8) NOT NULL CHECK(operation IN ('SCAN','ACCEPT')), target_id UUID NOT NULL, shipment_id UUID NOT NULL,
 request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL
);
CREATE FUNCTION dispatch.require_complete_pilot_custody() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM dispatch.pilot_custody WHERE acceptance_id=NEW.id)<>NEW.parcel_count THEN
  RAISE EXCEPTION 'ALL_PILOT_PARCEL_CUSTODY_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION dispatch.require_complete_pilot_custody() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION dispatch.require_complete_pilot_custody() TO "${runtimeRole}";
CREATE CONSTRAINT TRIGGER complete_pilot_custody AFTER INSERT ON dispatch.pilot_intake_acceptance DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION dispatch.require_complete_pilot_custody();
ALTER TABLE orders.pilot_parcel ADD CONSTRAINT pilot_parcel_shipment_binding UNIQUE(id,shipment_id);
CREATE TABLE orders.pilot_tracking_event (
 id UUID PRIMARY KEY, shipment_id UUID NOT NULL UNIQUE, owner_id UUID NOT NULL, source_acceptance_id UUID NOT NULL UNIQUE,
 event_type VARCHAR(40) NOT NULL CHECK(event_type='CARRIER_ACCEPTED_AT_STATION'), station_code VARCHAR(32) NOT NULL,
 actor_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL, version BIGINT NOT NULL CHECK(version=1),
 environment VARCHAR(8) NOT NULL CHECK(environment IN ('LOCAL','TEST')), provenance VARCHAR(16) NOT NULL CHECK(provenance='SIMULATION'),
 FOREIGN KEY(shipment_id,owner_id) REFERENCES orders.pilot_shipment(id,owner_id), UNIQUE(id,shipment_id)
);
CREATE TABLE orders.pilot_parcel_event (
 event_id UUID NOT NULL, shipment_id UUID NOT NULL, parcel_id UUID PRIMARY KEY,
 event_type VARCHAR(40) NOT NULL CHECK(event_type='CARRIER_ACCEPTED_AT_STATION'), occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(event_id,shipment_id) REFERENCES orders.pilot_tracking_event(id,shipment_id),
 FOREIGN KEY(parcel_id,shipment_id) REFERENCES orders.pilot_parcel(id,shipment_id)
);
CREATE FUNCTION orders.require_complete_pilot_tracking() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM orders.pilot_parcel_event WHERE event_id=NEW.id)<>(SELECT count(*) FROM orders.pilot_parcel WHERE shipment_id=NEW.shipment_id) THEN
  RAISE EXCEPTION 'ALL_PILOT_PARCEL_EVENTS_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION orders.require_complete_pilot_tracking() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.require_complete_pilot_tracking() TO "${runtimeRole}";
CREATE CONSTRAINT TRIGGER complete_pilot_tracking AFTER INSERT ON orders.pilot_tracking_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.require_complete_pilot_tracking();
REVOKE ALL ON dispatch.pilot_station,dispatch.pilot_station_assignment,dispatch.pilot_intake_scan,dispatch.pilot_intake_acceptance,dispatch.pilot_custody,dispatch.pilot_intake_receipt,dispatch.pilot_intake_audit,orders.pilot_tracking_event,orders.pilot_parcel_event FROM PUBLIC;
GRANT SELECT ON dispatch.pilot_station,dispatch.pilot_station_assignment TO "${runtimeRole}";
GRANT SELECT,INSERT ON dispatch.pilot_intake_scan,dispatch.pilot_intake_acceptance,dispatch.pilot_custody,dispatch.pilot_intake_receipt,dispatch.pilot_intake_audit,orders.pilot_tracking_event,orders.pilot_parcel_event TO "${runtimeRole}";
