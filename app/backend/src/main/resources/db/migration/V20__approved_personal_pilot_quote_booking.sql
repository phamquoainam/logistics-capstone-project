-- Explicit Owner final activation: LOCAL/TEST simulation only. Application still requires an explicit environment gate.
GRANT USAGE ON SCHEMA pricing,finance TO "${runtimeRole}";
CREATE TABLE merchant.pilot_coverage_dataset (
 version VARCHAR(64) PRIMARY KEY, namespace VARCHAR(64) NOT NULL, provenance VARCHAR(16) NOT NULL CHECK(provenance='SIMULATION'),
 approval_ref VARCHAR(100) NOT NULL, effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ CHECK(effective_to>effective_from)
);
CREATE TABLE merchant.pilot_area (
 dataset_version VARCHAR(64) NOT NULL REFERENCES merchant.pilot_coverage_dataset(version), code VARCHAR(32) NOT NULL,
 region VARCHAR(32) NOT NULL, label VARCHAR(160) NOT NULL, drop_off_code VARCHAR(32) NOT NULL,
 pickup_enabled BOOLEAN NOT NULL CHECK(NOT pickup_enabled), PRIMARY KEY(dataset_version,code), UNIQUE(dataset_version,drop_off_code)
);
CREATE TABLE merchant.pilot_route (
 dataset_version VARCHAR(64) NOT NULL REFERENCES merchant.pilot_coverage_dataset(version), origin_code VARCHAR(32) NOT NULL, destination_code VARCHAR(32) NOT NULL,
 service_code VARCHAR(32) NOT NULL CHECK(service_code='ROAD_STANDARD'), classification VARCHAR(8) NOT NULL CHECK(classification IN ('INTRA','INTER')),
 PRIMARY KEY(dataset_version,origin_code,destination_code,service_code), CHECK(origin_code<>destination_code),
 FOREIGN KEY(dataset_version,origin_code) REFERENCES merchant.pilot_area(dataset_version,code), FOREIGN KEY(dataset_version,destination_code) REFERENCES merchant.pilot_area(dataset_version,code)
);
INSERT INTO merchant.pilot_coverage_dataset VALUES ('ARGO-SIM-COVERAGE-1','ARGO_SIMULATED_ADMIN_1','SIMULATION','OWNER-FINAL-PILOT-ACTIVATION-2026-11-01','2026-11-01T15:20:48Z',NULL);
INSERT INTO merchant.pilot_area VALUES
 ('ARGO-SIM-COVERAGE-1','SIM-A1','SIM-REGION-A','Khu mô phỏng A1 — không phải địa giới thật','SIM-DROP-A1',FALSE),
 ('ARGO-SIM-COVERAGE-1','SIM-A2','SIM-REGION-A','Khu mô phỏng A2 — không phải địa giới thật','SIM-DROP-A2',FALSE),
 ('ARGO-SIM-COVERAGE-1','SIM-B1','SIM-REGION-B','Khu mô phỏng B1 — không phải địa giới thật','SIM-DROP-B1',FALSE);
INSERT INTO merchant.pilot_route VALUES
 ('ARGO-SIM-COVERAGE-1','SIM-A1','SIM-A2','ROAD_STANDARD','INTRA'),('ARGO-SIM-COVERAGE-1','SIM-A2','SIM-A1','ROAD_STANDARD','INTRA'),
 ('ARGO-SIM-COVERAGE-1','SIM-A1','SIM-B1','ROAD_STANDARD','INTER'),('ARGO-SIM-COVERAGE-1','SIM-B1','SIM-A1','ROAD_STANDARD','INTER');
CREATE TABLE pricing.pilot_tariff_version (
 version VARCHAR(64) PRIMARY KEY, service_code VARCHAR(32) NOT NULL CHECK(service_code='ROAD_STANDARD'), approval_ref VARCHAR(100) NOT NULL,
 effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ CHECK(effective_to>effective_from),
 intra_first_vnd BIGINT NOT NULL CHECK(intra_first_vnd>0), intra_step_vnd BIGINT NOT NULL CHECK(intra_step_vnd>0),
 inter_first_vnd BIGINT NOT NULL CHECK(inter_first_vnd>0), inter_step_vnd BIGINT NOT NULL CHECK(inter_step_vnd>0),
 step_grams INTEGER NOT NULL CHECK(step_grams=500), volumetric_divisor INTEGER NOT NULL CHECK(volumetric_divisor=5000),
 max_parcels INTEGER NOT NULL CHECK(max_parcels=5), max_actual_grams INTEGER NOT NULL CHECK(max_actual_grams=20000),
 max_dimension_mm INTEGER NOT NULL CHECK(max_dimension_mm=1000), max_girth_mm INTEGER NOT NULL CHECK(max_girth_mm=2000),
 quote_ttl_seconds INTEGER NOT NULL CHECK(quote_ttl_seconds=900), tax_treatment VARCHAR(64) NOT NULL CHECK(tax_treatment='SIMULATION_NOT_TAX_INVOICE')
);
INSERT INTO pricing.pilot_tariff_version VALUES ('ARGO-ROAD-TARIFF-1','ROAD_STANDARD','OWNER-FINAL-PILOT-ACTIVATION-2026-11-01','2026-11-01T15:20:48Z',NULL,25000,3000,38000,5000,500,5000,5,20000,1000,2000,900,'SIMULATION_NOT_TAX_INVOICE');
CREATE TABLE pricing.pilot_quote (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, draft_id UUID NOT NULL, draft_version BIGINT NOT NULL CHECK(draft_version>0),
 tariff_version VARCHAR(64) NOT NULL REFERENCES pricing.pilot_tariff_version(version), coverage_version VARCHAR(64) NOT NULL,
 origin_code VARCHAR(32) NOT NULL, destination_code VARCHAR(32) NOT NULL, classification VARCHAR(8) NOT NULL CHECK(classification IN ('INTRA','INTER')),
 ordinary_goods_confirmed BOOLEAN NOT NULL CHECK(ordinary_goods_confirmed),
 drop_off_code VARCHAR(32) NOT NULL, parcel_facts JSONB NOT NULL CHECK(jsonb_typeof(parcel_facts)='array' AND jsonb_array_length(parcel_facts) BETWEEN 1 AND 5),
 fee_lines JSONB NOT NULL CHECK(jsonb_typeof(fee_lines)='array' AND jsonb_array_length(fee_lines)=jsonb_array_length(parcel_facts)),
 total_vnd BIGINT NOT NULL CHECK(total_vnd>0), created_at TIMESTAMPTZ NOT NULL, expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at=created_at+INTERVAL '15 minutes'),
 operation_key UUID NOT NULL, fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 consumed_by UUID, consumed_at TIMESTAMPTZ, UNIQUE(owner_id,operation_key), UNIQUE(consumed_by), CHECK((consumed_by IS NULL)=(consumed_at IS NULL)),
 CHECK(consumed_at IS NULL OR (consumed_at>=created_at AND consumed_at<expires_at))
);
CREATE FUNCTION pricing.guard_pilot_quote() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.consumed_by IS NOT NULL OR NEW.consumed_by IS NULL OR NEW.consumed_at IS NULL OR
 (to_jsonb(NEW)-'consumed_by'-'consumed_at') IS DISTINCT FROM (to_jsonb(OLD)-'consumed_by'-'consumed_at') THEN
  RAISE EXCEPTION 'PILOT_QUOTE_IMMUTABLE_OR_ALREADY_CONSUMED' USING ERRCODE='23514';
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER guard_pilot_quote BEFORE UPDATE ON pricing.pilot_quote FOR EACH ROW EXECUTE FUNCTION pricing.guard_pilot_quote();
REVOKE ALL ON FUNCTION pricing.guard_pilot_quote() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pricing.guard_pilot_quote() TO "${runtimeRole}";
ALTER TABLE orders.shipping_draft DROP CONSTRAINT shipping_draft_state_check;
ALTER TABLE orders.shipping_draft ADD CONSTRAINT shipping_draft_state_check CHECK(state IN ('DRAFT','ARCHIVED','BOOKED'));
ALTER TABLE orders.shipping_draft_version DROP CONSTRAINT shipping_draft_version_state_check;
ALTER TABLE orders.shipping_draft_version ADD CONSTRAINT shipping_draft_version_state_check CHECK(state IN ('DRAFT','ARCHIVED','BOOKED'));
ALTER TABLE orders.shipping_draft_audit DROP CONSTRAINT shipping_draft_audit_operation_check;
ALTER TABLE orders.shipping_draft_audit ADD CONSTRAINT shipping_draft_audit_operation_check CHECK(operation IN ('createShippingDraft','updateShippingDraft','archiveShippingDraft','confirmPilotBooking'));
CREATE TABLE orders.pilot_shipment (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, owner_type VARCHAR(16) NOT NULL CHECK(owner_type='PERSONAL'),
 code VARCHAR(64) NOT NULL UNIQUE CHECK(code~'^AP-SIM-[a-f0-9]{32}$'), draft_id UUID NOT NULL UNIQUE, draft_version BIGINT NOT NULL,
 booking_state VARCHAR(16) NOT NULL CHECK(booking_state='CONFIRMED'), carrier_state VARCHAR(16) NOT NULL CHECK(carrier_state='NOT_ACCEPTED'),
 draft_snapshot JSONB NOT NULL CHECK(orders.valid_shipping_preparation(draft_snapshot)), quote_snapshot JSONB NOT NULL,
 quote_id UUID NOT NULL UNIQUE, total_vnd BIGINT NOT NULL CHECK(total_vnd>0), created_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(draft_id,owner_id,draft_version) REFERENCES orders.shipping_draft_version(id,owner_id,version), UNIQUE(id,owner_id)
);
CREATE TABLE orders.pilot_parcel (
 id UUID PRIMARY KEY, shipment_id UUID NOT NULL REFERENCES orders.pilot_shipment(id), parcel_number INTEGER NOT NULL CHECK(parcel_number BETWEEN 1 AND 5),
 description VARCHAR(160) NOT NULL CHECK(length(btrim(description))>0 AND description!~'[[:cntrl:]]'), quantity BIGINT NOT NULL CHECK(quantity BETWEEN 1 AND 1000000),
 weight_grams BIGINT NOT NULL CHECK(weight_grams BETWEEN 1 AND 20000), length_mm BIGINT NOT NULL CHECK(length_mm BETWEEN 1 AND 1000), width_mm BIGINT NOT NULL CHECK(width_mm BETWEEN 1 AND 1000), height_mm BIGINT NOT NULL CHECK(height_mm BETWEEN 1 AND 1000),
 declared_value_vnd BIGINT NOT NULL CHECK(declared_value_vnd BETWEEN 0 AND 1000000000000), CHECK(length_mm+2*(width_mm+height_mm)<=2000), UNIQUE(shipment_id,parcel_number)
);
CREATE TABLE orders.pilot_booking_receipt (
 owner_id UUID NOT NULL, operation_key UUID NOT NULL, draft_id UUID NOT NULL, draft_version BIGINT NOT NULL CHECK(draft_version>0), quote_id UUID NOT NULL,
 shipment_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(owner_id,operation_key),
 FOREIGN KEY(shipment_id,owner_id) REFERENCES orders.pilot_shipment(id,owner_id), FOREIGN KEY(draft_id,owner_id,draft_version) REFERENCES orders.shipping_draft_version(id,owner_id,version)
);
CREATE TABLE finance.pilot_shipping_obligation (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, shipment_id UUID NOT NULL UNIQUE, quote_id UUID NOT NULL,
 amount_vnd BIGINT NOT NULL CHECK(amount_vnd>0), currency VARCHAR(3) NOT NULL CHECK(currency='VND'), state VARCHAR(8) NOT NULL CHECK(state='UNPAID'),
 payer VARCHAR(8) NOT NULL CHECK(payer='SENDER'), method VARCHAR(32) NOT NULL CHECK(method='CASH_ON_HANDOVER'),
 policy_version VARCHAR(64) NOT NULL CHECK(policy_version='BR13-LOCAL-PILOT-01'), request_id UUID NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
REVOKE ALL ON merchant.pilot_coverage_dataset,merchant.pilot_area,merchant.pilot_route,pricing.pilot_tariff_version,pricing.pilot_quote,orders.pilot_shipment,orders.pilot_parcel,orders.pilot_booking_receipt,finance.pilot_shipping_obligation FROM PUBLIC;
GRANT SELECT ON merchant.pilot_coverage_dataset,merchant.pilot_area,merchant.pilot_route,pricing.pilot_tariff_version TO "${runtimeRole}";
GRANT SELECT,INSERT ON pricing.pilot_quote,orders.pilot_shipment,orders.pilot_parcel,orders.pilot_booking_receipt,finance.pilot_shipping_obligation TO "${runtimeRole}";
GRANT UPDATE(consumed_by,consumed_at) ON pricing.pilot_quote TO "${runtimeRole}";

-- Published pilot facts are immutable; changing rules requires a new approved version/migration.
CREATE FUNCTION merchant.reject_pilot_catalog_rewrite() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'PUBLISHED_PILOT_CATALOG_IMMUTABLE' USING ERRCODE='23514'; END $$;
REVOKE ALL ON FUNCTION merchant.reject_pilot_catalog_rewrite() FROM PUBLIC;
CREATE TRIGGER immutable_pilot_dataset BEFORE UPDATE OR DELETE ON merchant.pilot_coverage_dataset FOR EACH ROW EXECUTE FUNCTION merchant.reject_pilot_catalog_rewrite();
CREATE TRIGGER immutable_pilot_area BEFORE UPDATE OR DELETE ON merchant.pilot_area FOR EACH ROW EXECUTE FUNCTION merchant.reject_pilot_catalog_rewrite();
CREATE TRIGGER immutable_pilot_route BEFORE UPDATE OR DELETE ON merchant.pilot_route FOR EACH ROW EXECUTE FUNCTION merchant.reject_pilot_catalog_rewrite();
CREATE FUNCTION pricing.reject_pilot_tariff_rewrite() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'PUBLISHED_PILOT_TARIFF_IMMUTABLE' USING ERRCODE='23514'; END $$;
REVOKE ALL ON FUNCTION pricing.reject_pilot_tariff_rewrite() FROM PUBLIC;
CREATE TRIGGER immutable_pilot_tariff BEFORE UPDATE OR DELETE ON pricing.pilot_tariff_version FOR EACH ROW EXECUTE FUNCTION pricing.reject_pilot_tariff_rewrite();
CREATE FUNCTION orders.require_pilot_booking_parcels() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (SELECT count(*) FROM orders.pilot_parcel WHERE shipment_id=NEW.id)<>jsonb_array_length(NEW.draft_snapshot->'parcels') THEN
  RAISE EXCEPTION 'PILOT_BOOKING_PARCELS_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION orders.require_pilot_booking_parcels() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.require_pilot_booking_parcels() TO "${runtimeRole}";
CREATE CONSTRAINT TRIGGER complete_pilot_booking AFTER INSERT ON orders.pilot_shipment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.require_pilot_booking_parcels();
ALTER TABLE orders.pilot_shipment ADD CONSTRAINT pilot_shipment_quote_binding CHECK(
 quote_snapshot->>'id'=quote_id::text AND quote_snapshot->>'accountId'=owner_id::text AND quote_snapshot->>'draftId'=draft_id::text AND (quote_snapshot->>'draftVersion')::bigint=draft_version AND (quote_snapshot->>'totalVnd')::bigint=total_vnd
);
CREATE FUNCTION pricing.valid_pilot_quote_breakdown(facts JSONB,lines JSONB,total BIGINT,rate_version TEXT,region TEXT) RETURNS BOOLEAN LANGUAGE plpgsql STABLE AS $$
DECLARE rule pricing.pilot_tariff_version%ROWTYPE; p JSONB; f JSONB; n INTEGER=0; steps BIGINT; amount BIGINT; summed BIGINT=0; k TEXT;
BEGIN
 SELECT * INTO STRICT rule FROM pricing.pilot_tariff_version WHERE version=rate_version;
 IF region NOT IN ('INTRA','INTER') OR jsonb_typeof(facts)<>'array' OR jsonb_typeof(lines)<>'array' OR jsonb_array_length(facts) NOT BETWEEN 1 AND rule.max_parcels OR jsonb_array_length(facts)<>jsonb_array_length(lines) THEN RETURN FALSE; END IF;
 FOR p IN SELECT value FROM jsonb_array_elements(facts) LOOP
  f=lines->n; n=n+1;
  IF jsonb_typeof(p)<>'object' OR jsonb_typeof(f)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(p))<>4 OR (SELECT count(*) FROM jsonb_object_keys(f))<>4 OR NOT p ?& ARRAY['weightGrams','lengthMm','widthMm','heightMm'] OR NOT f ?& ARRAY['parcelNumber','actualGrams','chargeableGrams','feeVnd'] THEN RETURN FALSE; END IF;
  FOREACH k IN ARRAY ARRAY['weightGrams','lengthMm','widthMm','heightMm'] LOOP
   IF jsonb_typeof(p->k)<>'number' OR (p->>k)!~'^[0-9]+$' OR (p->>k)::bigint<1 THEN RETURN FALSE; END IF;
  END LOOP;
  FOREACH k IN ARRAY ARRAY['parcelNumber','actualGrams','chargeableGrams','feeVnd'] LOOP
   IF jsonb_typeof(f->k)<>'number' OR (f->>k)!~'^[0-9]+$' THEN RETURN FALSE; END IF;
  END LOOP;
  IF (p->>'weightGrams')::bigint>rule.max_actual_grams OR (p->>'lengthMm')::bigint>rule.max_dimension_mm OR (p->>'widthMm')::bigint>rule.max_dimension_mm OR (p->>'heightMm')::bigint>rule.max_dimension_mm OR (p->>'lengthMm')::bigint+2*((p->>'widthMm')::bigint+(p->>'heightMm')::bigint)>rule.max_girth_mm THEN RETURN FALSE; END IF;
  steps=greatest(ceil((p->>'weightGrams')::numeric/rule.step_grams),ceil((p->>'lengthMm')::numeric*(p->>'widthMm')::numeric*(p->>'heightMm')::numeric/(rule.volumetric_divisor::numeric*rule.step_grams)))::bigint;
  amount=(CASE WHEN region='INTRA' THEN rule.intra_first_vnd+(steps-1)*rule.intra_step_vnd ELSE rule.inter_first_vnd+(steps-1)*rule.inter_step_vnd END);
  IF (f->>'parcelNumber')::bigint<>n OR (f->>'actualGrams')::bigint<>(p->>'weightGrams')::bigint OR (f->>'chargeableGrams')::bigint<>steps*rule.step_grams OR (f->>'feeVnd')::bigint<>amount THEN RETURN FALSE; END IF;
  summed=summed+amount;
 END LOOP; RETURN summed=total;
EXCEPTION WHEN OTHERS THEN RETURN FALSE;
END $$;
REVOKE ALL ON FUNCTION pricing.valid_pilot_quote_breakdown(JSONB,JSONB,BIGINT,TEXT,TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pricing.valid_pilot_quote_breakdown(JSONB,JSONB,BIGINT,TEXT,TEXT) TO "${runtimeRole}";
ALTER TABLE pricing.pilot_quote ADD CONSTRAINT pilot_quote_exact_breakdown CHECK(pricing.valid_pilot_quote_breakdown(parcel_facts,fee_lines,total_vnd,tariff_version,classification) IS TRUE);
