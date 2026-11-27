-- BIA-V1, Owner-approved isolated SOURCE/TEST. No roles/grants or historical edits.
ALTER TABLE orders.pilot_shipment
 ADD CONSTRAINT pilot_shipment_owner_kind_key UNIQUE(id,owner_type),
 ADD CONSTRAINT pilot_shipment_org_facility_key UNIQUE(id,organization_id,facility_id);

ALTER TABLE dispatch.pilot_intake_acceptance
 ADD COLUMN owner_type VARCHAR(8) NOT NULL DEFAULT 'PERSONAL',
 DROP CONSTRAINT pilot_intake_acceptance_policy_version_check,
 ADD CONSTRAINT intake_owner_policy CHECK(
  (owner_type='PERSONAL' AND policy_version='BR13-LOCAL-INTAKE-01') OR
  (owner_type='BUSINESS' AND policy_version='BR13-BUSINESS-INTAKE-01'
   AND operational_assignment_id IS NOT NULL AND assignment_id IS NULL)),
 ADD CONSTRAINT intake_shipment_owner_fk FOREIGN KEY(shipment_id,owner_type)
  REFERENCES orders.pilot_shipment(id,owner_type),
 ADD CONSTRAINT intake_owner_event_key UNIQUE(id,shipment_id,station_code,actor_id,owner_type);

ALTER TABLE dispatch.pilot_intake_scan
 ADD CONSTRAINT intake_scan_parcel_fk FOREIGN KEY(parcel_id,shipment_id)
  REFERENCES orders.pilot_parcel(id,shipment_id);

ALTER TABLE orders.pilot_tracking_event
 ADD COLUMN owner_type VARCHAR(8) NOT NULL DEFAULT 'PERSONAL',
 ADD COLUMN organization_id UUID,
 ADD COLUMN facility_id UUID,
 ALTER COLUMN owner_id DROP NOT NULL,
 ADD CONSTRAINT tracking_owner_variant CHECK(
  (owner_type='PERSONAL' AND owner_id IS NOT NULL AND organization_id IS NULL AND facility_id IS NULL) OR
  (owner_type='BUSINESS' AND owner_id IS NULL AND organization_id IS NOT NULL)),
 ADD CONSTRAINT tracking_shipment_owner_fk FOREIGN KEY(shipment_id,owner_type)
  REFERENCES orders.pilot_shipment(id,owner_type),
 ADD CONSTRAINT tracking_shipment_org_fk FOREIGN KEY(shipment_id,organization_id)
  REFERENCES orders.pilot_shipment(id,organization_id),
 ADD CONSTRAINT tracking_shipment_facility_fk FOREIGN KEY(shipment_id,organization_id,facility_id)
  REFERENCES orders.pilot_shipment(id,organization_id,facility_id),
 ADD CONSTRAINT tracking_acceptance_owner_fk
  FOREIGN KEY(source_acceptance_id,shipment_id,station_code,actor_id,owner_type)
  REFERENCES dispatch.pilot_intake_acceptance(id,shipment_id,station_code,actor_id,owner_type);

-- Keep the original PERSONAL FK, append-only grants, actual Ops authority guard,
-- unique acceptance/Parcel events and deferred custody/tracking completeness.
-- New owner tags project the known historical PERSONAL owner; no event is rewritten.

-- Owner-approved BIA-DB-GUARD-01 (6950d960...): bounded validation reads only.
-- Preserve SECURITY INVOKER, owner, ACL and the existing deferred triggers.
CREATE OR REPLACE FUNCTION dispatch.require_complete_pilot_custody() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.parcel_count<>(SELECT count(*) FROM orders.pilot_parcel WHERE shipment_id=NEW.shipment_id)
 OR NEW.parcel_count<>(SELECT count(*) FROM dispatch.pilot_custody WHERE acceptance_id=NEW.id)
 OR EXISTS(SELECT 1 FROM dispatch.pilot_custody c JOIN dispatch.pilot_intake_scan s ON s.id=c.scan_id
   JOIN orders.pilot_shipment b ON b.id=c.shipment_id WHERE c.acceptance_id=NEW.id
   AND (c.occurred_at<>NEW.occurred_at OR s.scanned_at<b.created_at OR s.scanned_at>NEW.occurred_at))
 OR NOT EXISTS(SELECT 1 FROM orders.pilot_tracking_event e WHERE e.source_acceptance_id=NEW.id
   AND e.shipment_id=NEW.shipment_id AND e.owner_type=NEW.owner_type AND e.occurred_at=NEW.occurred_at)
 OR NOT EXISTS(SELECT 1 FROM dispatch.pilot_intake_audit a WHERE a.target_id=NEW.id AND a.shipment_id=NEW.shipment_id
   AND a.actor_id=NEW.actor_id AND a.station_code=NEW.station_code AND a.request_id=NEW.request_id AND a.operation='ACCEPT' AND a.occurred_at=NEW.occurred_at)
 OR NOT EXISTS(SELECT 1 FROM dispatch.pilot_intake_receipt r WHERE r.target_id=NEW.id AND r.shipment_id=NEW.shipment_id
   AND r.actor_id=NEW.actor_id AND r.station_code=NEW.station_code AND r.operation='ACCEPT' AND r.created_at=NEW.occurred_at)
 THEN RAISE EXCEPTION 'ALL_PILOT_PARCEL_CUSTODY_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE OR REPLACE FUNCTION orders.require_complete_pilot_tracking() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM orders.pilot_parcel_event WHERE event_id=NEW.id)<>(SELECT count(*) FROM orders.pilot_parcel WHERE shipment_id=NEW.shipment_id)
 OR EXISTS(SELECT 1 FROM orders.pilot_parcel_event p WHERE p.event_id=NEW.id AND p.occurred_at<>NEW.occurred_at)
 OR NOT EXISTS(SELECT 1 FROM orders.pilot_shipment s WHERE s.id=NEW.shipment_id AND s.owner_type=NEW.owner_type
   AND s.owner_id IS NOT DISTINCT FROM NEW.owner_id AND s.organization_id IS NOT DISTINCT FROM NEW.organization_id
   AND s.facility_id IS NOT DISTINCT FROM NEW.facility_id AND s.created_at<=NEW.occurred_at)
 OR NOT EXISTS(SELECT 1 FROM dispatch.pilot_intake_acceptance a WHERE a.id=NEW.source_acceptance_id AND a.shipment_id=NEW.shipment_id
   AND a.owner_type=NEW.owner_type AND a.station_code=NEW.station_code AND a.actor_id=NEW.actor_id AND a.occurred_at=NEW.occurred_at)
 THEN RAISE EXCEPTION 'ALL_PILOT_PARCEL_EVENTS_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
