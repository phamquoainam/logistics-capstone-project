-- Accepted bounded CSV preparation workflow. No automatic Quote/Booking.
CREATE TABLE orders.organization_import_job (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID, creator_account_id UUID NOT NULL,
 template_version VARCHAR(20) NOT NULL CHECK(template_version='BUSINESS_CSV_V1'),
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), preview_snapshot JSONB NOT NULL,
 state VARCHAR(10) NOT NULL CHECK(state IN ('PREVIEW','PARTIAL','COMPLETED')), version BIGINT NOT NULL CHECK(version>0),
 total_rows INT NOT NULL CHECK(total_rows BETWEEN 1 AND 100), valid_rows INT NOT NULL CHECK(valid_rows>=0), invalid_rows INT NOT NULL CHECK(invalid_rows>=0),
 created_rows INT NOT NULL CHECK(created_rows>=0), conflict_rows INT NOT NULL CHECK(conflict_rows>=0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), UNIQUE(id,organization_id),
 CHECK(total_rows=valid_rows+invalid_rows+created_rows+conflict_rows),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TABLE orders.organization_import_row (
 job_id UUID NOT NULL, row_number INT NOT NULL CHECK(row_number BETWEEN 1 AND 100), organization_id UUID NOT NULL,
 proposed_input JSONB, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 source_channel VARCHAR(40), source_order_reference VARCHAR(80),
 state VARCHAR(8) NOT NULL CHECK(state IN ('VALID','INVALID','CREATED','CONFLICT')), errors JSONB NOT NULL CHECK(jsonb_typeof(errors)='array' AND jsonb_array_length(errors)<=40),
 preparation_id UUID, version BIGINT NOT NULL CHECK(version>0), PRIMARY KEY(job_id,row_number),
 FOREIGN KEY(job_id,organization_id) REFERENCES orders.organization_import_job(id,organization_id),
 FOREIGN KEY(preparation_id,organization_id) REFERENCES orders.organization_preparation(id,organization_id),
 CHECK((state='CREATED')=(preparation_id IS NOT NULL)),
 CHECK((state='INVALID' AND proposed_input IS NULL AND jsonb_array_length(errors)>0)
 OR (state IN ('VALID','CREATED','CONFLICT') AND proposed_input IS NOT NULL AND source_channel IS NOT NULL AND source_order_reference IS NOT NULL)),
 CHECK(state NOT IN ('VALID','CREATED') OR jsonb_array_length(errors)=0),
 CHECK(state='INVALID' OR (orders.valid_shipping_preparation(proposed_input-'facilityId'-'sourceChannel'-'sourceOrderReference') IS TRUE
 AND proposed_input->>'sourceChannel'=source_channel AND proposed_input->>'sourceOrderReference'=source_order_reference))
);
CREATE UNIQUE INDEX organization_import_row_source ON orders.organization_import_row(job_id,source_channel,source_order_reference) WHERE state<>'INVALID';
CREATE INDEX organization_import_list ON orders.organization_import_job(organization_id,updated_at DESC,id);
-- Keep preparation version foreign keys for old operations; import operations bind
-- real import jobs, without inventing a fake Draft as an evidence target.
DO $$ DECLARE c RECORD;
BEGIN
 FOR c IN SELECT conname,conrelid::regclass rel FROM pg_constraint
 WHERE contype='f' AND conrelid IN ('orders.organization_preparation_receipt'::regclass,'orders.organization_preparation_audit'::regclass)
 AND confrelid='orders.organization_preparation_version'::regclass LOOP
 EXECUTE format('ALTER TABLE %s DROP CONSTRAINT %I',c.rel,c.conname); END LOOP;
END $$;
ALTER TABLE orders.organization_preparation_receipt
 ADD COLUMN result_snapshot JSONB,
 ADD COLUMN preparation_target UUID GENERATED ALWAYS AS (CASE WHEN operation IN ('CREATE','UPDATE','ARCHIVE') THEN target_id END) STORED,
 ADD COLUMN preparation_version BIGINT GENERATED ALWAYS AS (CASE WHEN operation IN ('CREATE','UPDATE','ARCHIVE') THEN result_version END) STORED,
 ADD COLUMN import_target UUID GENERATED ALWAYS AS (CASE WHEN operation IN ('IMPORT_PREVIEW','IMPORT_COMMIT') THEN target_id END) STORED,
 ADD CHECK(operation IN ('CREATE','UPDATE','ARCHIVE','IMPORT_PREVIEW','IMPORT_COMMIT')),
 ADD CHECK(result_version=original_version+1),
 ADD CHECK((operation IN ('CREATE','UPDATE','ARCHIVE') AND result_snapshot IS NULL) OR
 (operation IN ('IMPORT_PREVIEW','IMPORT_COMMIT') AND result_snapshot IS NOT NULL AND jsonb_typeof(result_snapshot)='object'
 AND result_snapshot ?& ARRAY['id','organizationId','version'] AND jsonb_typeof(result_snapshot->'id')='string' AND jsonb_typeof(result_snapshot->'organizationId')='string' AND jsonb_typeof(result_snapshot->'version')='number'
 AND (result_snapshot->>'id')::uuid=target_id AND (result_snapshot->>'organizationId')::uuid=organization_id AND (result_snapshot->>'version')::bigint=result_version)),
 ADD FOREIGN KEY(preparation_target,organization_id,preparation_version) REFERENCES orders.organization_preparation_version(id,organization_id,version),
 ADD FOREIGN KEY(import_target,organization_id) REFERENCES orders.organization_import_job(id,organization_id);
ALTER TABLE orders.organization_preparation_audit
 ADD COLUMN preparation_target UUID GENERATED ALWAYS AS (CASE WHEN operation IN ('CREATE','UPDATE','ARCHIVE') THEN target_id END) STORED,
 ADD COLUMN preparation_version BIGINT GENERATED ALWAYS AS (CASE WHEN operation IN ('CREATE','UPDATE','ARCHIVE') THEN new_version END) STORED,
 ADD COLUMN import_target UUID GENERATED ALWAYS AS (CASE WHEN operation IN ('IMPORT_PREVIEW','IMPORT_COMMIT') THEN target_id END) STORED,
 ADD CHECK(operation IN ('CREATE','UPDATE','ARCHIVE','IMPORT_PREVIEW','IMPORT_COMMIT')),
 ADD FOREIGN KEY(preparation_target,organization_id,preparation_version) REFERENCES orders.organization_preparation_version(id,organization_id,version),
 ADD FOREIGN KEY(import_target,organization_id) REFERENCES orders.organization_import_job(id,organization_id);
CREATE FUNCTION orders.guard_organization_import() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'IMPORT_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF TG_OP='UPDATE' THEN
 IF TG_TABLE_NAME='organization_import_job' THEN
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.creator_account_id<>OLD.creator_account_id
  OR NEW.template_version<>OLD.template_version OR NEW.fingerprint_key_id<>OLD.fingerprint_key_id OR NEW.fingerprint<>OLD.fingerprint OR NEW.preview_snapshot<>OLD.preview_snapshot
  OR NEW.total_rows<>OLD.total_rows OR NEW.invalid_rows<>OLD.invalid_rows OR NEW.created_at<>OLD.created_at OR NEW.updated_at<OLD.updated_at OR NEW.version<>OLD.version+1
  THEN RAISE EXCEPTION 'IMPORT_BINDING_IMMUTABLE' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.job_id<>OLD.job_id OR NEW.row_number<>OLD.row_number OR NEW.organization_id<>OLD.organization_id OR NEW.fingerprint<>OLD.fingerprint
  OR NEW.proposed_input IS DISTINCT FROM OLD.proposed_input OR NEW.source_channel IS DISTINCT FROM OLD.source_channel OR NEW.source_order_reference IS DISTINCT FROM OLD.source_order_reference
  OR OLD.state<>'VALID' OR NEW.state NOT IN ('CREATED','CONFLICT') OR NEW.version<>OLD.version+1
  THEN RAISE EXCEPTION 'IMPORT_ROW_RESULT_IMMUTABLE' USING ERRCODE='23514'; END IF;
 END IF; END IF; RETURN NEW;
END $$;
CREATE TRIGGER organization_import_job_guard BEFORE UPDATE OR DELETE ON orders.organization_import_job FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_import();
CREATE TRIGGER organization_import_row_guard BEFORE UPDATE OR DELETE ON orders.organization_import_row FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_import();
CREATE FUNCTION orders.check_organization_import() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE target UUID; j orders.organization_import_job; counts RECORD;
BEGIN
 IF TG_TABLE_NAME='organization_import_job' THEN target:=NEW.id; ELSE target:=NEW.job_id; END IF;
 SELECT * INTO j FROM orders.organization_import_job WHERE id=target;
 SELECT count(*) total,count(*) FILTER(WHERE state='VALID') valid,count(*) FILTER(WHERE state='INVALID') invalid,
 count(*) FILTER(WHERE state='CREATED') created,count(*) FILTER(WHERE state='CONFLICT') conflict INTO counts FROM orders.organization_import_row WHERE job_id=target;
 IF j.total_rows<>counts.total OR j.valid_rows<>counts.valid OR j.invalid_rows<>counts.invalid OR j.created_rows<>counts.created OR j.conflict_rows<>counts.conflict
 OR (j.state='COMPLETED' AND j.valid_rows<>0) OR (j.version=1 AND (j.state<>'PREVIEW' OR j.created_rows<>0 OR j.conflict_rows<>0))
 OR EXISTS(SELECT 1 FROM orders.organization_import_row ir WHERE ir.job_id=target AND ir.state<>'INVALID' AND ((ir.proposed_input->>'facilityId')::uuid IS DISTINCT FROM j.facility_id
 OR (ir.state='CREATED' AND NOT EXISTS(SELECT 1 FROM orders.organization_preparation_version v WHERE v.id=ir.preparation_id AND v.organization_id=j.organization_id AND v.version=1
 AND v.snapshot->'input'=ir.proposed_input-'facilityId'-'sourceChannel'-'sourceOrderReference' AND v.snapshot->>'sourceChannel'=ir.source_channel AND v.snapshot->>'sourceOrderReference'=ir.source_order_reference))))
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_receipt r WHERE r.import_target=target AND r.organization_id=j.organization_id AND r.result_version=j.version)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_audit a WHERE a.import_target=target AND a.organization_id=j.organization_id AND a.new_version=j.version)
 THEN RAISE EXCEPTION 'IMPORT_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_import_job_evidence AFTER INSERT OR UPDATE ON orders.organization_import_job DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_import();
CREATE CONSTRAINT TRIGGER organization_import_row_evidence AFTER INSERT OR UPDATE ON orders.organization_import_row DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_import();
REVOKE ALL ON orders.organization_import_job,orders.organization_import_row FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.organization_import_job,orders.organization_import_row TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_organization_import(),orders.check_organization_import() FROM PUBLIC,"${runtimeRole}";

-- Retain purpose-bound mandatory preparation evidence after generalizing the receipt targets.
CREATE OR REPLACE FUNCTION orders.check_organization_preparation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM orders.organization_preparation_version v WHERE v.id=NEW.id AND v.organization_id=NEW.organization_id
 AND v.version=NEW.version AND (v.snapshot->>'id')::uuid=NEW.id AND (v.snapshot->>'organizationId')::uuid=NEW.organization_id
 AND (v.snapshot->>'version')::bigint=NEW.version AND (v.snapshot->>'createdAt')::timestamptz=NEW.created_at AND (v.snapshot->>'updatedAt')::timestamptz=NEW.updated_at
 AND v.snapshot->>'state'=NEW.state AND v.snapshot->>'preparationState'=NEW.preparation_state
 AND v.snapshot->'input'=NEW.payload AND (v.snapshot->>'facilityId')::uuid IS NOT DISTINCT FROM NEW.facility_id
 AND (v.snapshot->>'creatorAccountId')::uuid=NEW.creator_account_id
 AND v.snapshot->>'sourceChannel' IS NOT DISTINCT FROM NEW.source_channel
 AND v.snapshot->>'sourceOrderReference' IS NOT DISTINCT FROM NEW.source_order_reference)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_audit a WHERE a.operation IN ('CREATE','UPDATE','ARCHIVE') AND a.target_id=NEW.id AND a.organization_id=NEW.organization_id AND a.new_version=NEW.version)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_receipt r WHERE r.operation IN ('CREATE','UPDATE','ARCHIVE') AND r.target_id=NEW.id AND r.organization_id=NEW.organization_id AND r.result_version=NEW.version)
 THEN RAISE EXCEPTION 'PREPARATION_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
