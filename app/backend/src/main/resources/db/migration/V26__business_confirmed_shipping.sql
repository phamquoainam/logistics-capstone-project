-- BSC-06 OWNER ACCEPTED 2026-11-13. New source only; apply first to disposable test DB.
-- No V1-V25 edits, data updates, role provisioning, seeds or live migration execution.
CREATE TABLE pricing.organization_pilot_contract (
 id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0), organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 tariff_version VARCHAR(64) NOT NULL REFERENCES pricing.pilot_tariff_version(version), state VARCHAR(16) NOT NULL CHECK(state IN ('ACTIVE','SUSPENDED')),
 effective_from TIMESTAMPTZ NOT NULL, effective_to TIMESTAMPTZ CHECK(effective_to>effective_from), approval_ref VARCHAR(160) NOT NULL CHECK(length(btrim(approval_ref))>0),
 operator_request_id UUID NOT NULL UNIQUE, operator_principal TEXT NOT NULL, recorded_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(id,version), UNIQUE(organization_id,version), UNIQUE(id,version,organization_id)
);
CREATE TRIGGER organization_contract_append BEFORE UPDATE OR DELETE ON pricing.organization_pilot_contract FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
REVOKE ALL ON pricing.organization_pilot_contract FROM PUBLIC,"${runtimeRole}";
GRANT SELECT ON pricing.organization_pilot_contract TO "${runtimeRole}";

-- Owner separately confirmed the nullable owner-variant risk on2026-11-13 after automated review.
-- PERSONAL retains required account/draft bindings; BUSINESS cannot impersonate a Personal owner.
ALTER TABLE pricing.pilot_quote ALTER COLUMN owner_id DROP NOT NULL, ALTER COLUMN draft_id DROP NOT NULL,
 ADD COLUMN owner_type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL', ADD COLUMN organization_id UUID REFERENCES merchant.organization(id),
 ADD COLUMN facility_id UUID, ADD COLUMN creator_account_id UUID, ADD COLUMN organization_preparation_id UUID,
 ADD COLUMN organization_preparation_version BIGINT, ADD COLUMN contract_id UUID, ADD COLUMN contract_version BIGINT,
 ADD CONSTRAINT quote_facility_org_fk FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 ADD CONSTRAINT quote_preparation_org_fk FOREIGN KEY(organization_preparation_id,organization_id,organization_preparation_version) REFERENCES orders.organization_preparation_version(id,organization_id,version),
 ADD CONSTRAINT quote_contract_org_fk FOREIGN KEY(contract_id,contract_version,organization_id) REFERENCES pricing.organization_pilot_contract(id,version,organization_id),
 ADD CONSTRAINT quote_owner_variant CHECK(
  (owner_type='PERSONAL' AND owner_id IS NOT NULL AND draft_id IS NOT NULL AND organization_id IS NULL AND facility_id IS NULL AND creator_account_id IS NULL AND organization_preparation_id IS NULL AND organization_preparation_version IS NULL AND contract_id IS NULL AND contract_version IS NULL)
  OR (owner_type='BUSINESS' AND owner_id IS NULL AND draft_id IS NULL AND organization_id IS NOT NULL AND creator_account_id IS NOT NULL AND organization_preparation_id IS NOT NULL AND organization_preparation_version IS NOT NULL AND draft_version=organization_preparation_version AND contract_id IS NOT NULL AND contract_version IS NOT NULL)),
 ADD UNIQUE(id,organization_id);
CREATE UNIQUE INDEX business_quote_operation ON pricing.pilot_quote(organization_id,creator_account_id,operation_key) WHERE owner_type='BUSINESS';
CREATE TABLE pricing.organization_quote_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation_key UUID NOT NULL, facility_id UUID, target_id UUID NOT NULL,
 expected_version BIGINT NOT NULL CHECK(expected_version>0), fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 recorded_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(organization_id,actor_account_id,operation_key),
 FOREIGN KEY(target_id,organization_id) REFERENCES pricing.pilot_quote(id,organization_id), FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TRIGGER organization_quote_receipt_append BEFORE UPDATE OR DELETE ON pricing.organization_quote_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
REVOKE ALL ON pricing.organization_quote_receipt FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT ON pricing.organization_quote_receipt TO "${runtimeRole}";

CREATE FUNCTION pricing.configure_local_business_contract(request UUID,org UUID,action TEXT,expected BIGINT,approval TEXT) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER AS $$
DECLARE prior pricing.organization_pilot_contract; last pricing.organization_pilot_contract; target UUID; now_at TIMESTAMPTZ:=clock_timestamp();
BEGIN
 IF current_user<>(SELECT pg_get_userbyid(relowner) FROM pg_class WHERE oid='pricing.organization_pilot_contract'::regclass)
 OR coalesce(current_setting('argopost.operator_environment',true),'') NOT IN ('LOCAL','TEST')
 THEN RAISE EXCEPTION 'BUSINESS_CONTRACT_OPERATOR_DENIED' USING ERRCODE='42501'; END IF;
 PERFORM pg_advisory_xact_lock(730023,1);
 IF request IS NULL OR org IS NULL OR expected IS NULL OR expected<0 OR action IS NULL OR action NOT IN ('ACTIVATE','SUSPEND') OR approval IS NULL OR length(btrim(approval)) NOT BETWEEN 1 AND 160
 THEN RAISE EXCEPTION 'BUSINESS_CONTRACT_INPUT_INVALID' USING ERRCODE='23514'; END IF;
 SELECT * INTO prior FROM pricing.organization_pilot_contract WHERE operator_request_id=request;
 IF FOUND THEN
  IF prior.organization_id<>org OR prior.version<>expected+1 OR prior.state<>(CASE action WHEN 'ACTIVATE' THEN 'ACTIVE' ELSE 'SUSPENDED' END) OR prior.approval_ref<>approval
  THEN RAISE EXCEPTION 'BUSINESS_CONTRACT_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514'; END IF; RETURN prior.id;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.organization WHERE id=org AND state='ACTIVE' AND environment=current_setting('argopost.operator_environment'))
 THEN RAISE EXCEPTION 'BUSINESS_CONTRACT_ORGANIZATION_UNAVAILABLE' USING ERRCODE='23514'; END IF;
 SELECT * INTO last FROM pricing.organization_pilot_contract WHERE organization_id=org ORDER BY version DESC LIMIT 1;
 IF coalesce(last.version,0)<>expected OR (expected=0 AND action<>'ACTIVATE') THEN RAISE EXCEPTION 'BUSINESS_CONTRACT_STALE' USING ERRCODE='23514'; END IF;
 target:=coalesce(last.id,gen_random_uuid());
 INSERT INTO pricing.organization_pilot_contract VALUES(target,expected+1,org,'ARGO-ROAD-TARIFF-1',CASE action WHEN 'ACTIVATE' THEN 'ACTIVE' ELSE 'SUSPENDED' END,now_at,NULL,approval,request,current_user,now_at);
 RETURN target;
END $$;
REVOKE ALL ON FUNCTION pricing.configure_local_business_contract(UUID,UUID,TEXT,BIGINT,TEXT) FROM PUBLIC,"${runtimeRole}";

ALTER TABLE orders.pilot_shipment ALTER COLUMN owner_id DROP NOT NULL, ALTER COLUMN draft_id DROP NOT NULL,
 DROP CONSTRAINT pilot_shipment_owner_type_check, DROP CONSTRAINT pilot_shipment_quote_binding,
 ADD COLUMN organization_id UUID REFERENCES merchant.organization(id), ADD COLUMN facility_id UUID, ADD COLUMN creator_account_id UUID,
 ADD COLUMN organization_preparation_id UUID, ADD COLUMN organization_preparation_version BIGINT,
 ADD CONSTRAINT shipment_facility_org_fk FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 ADD CONSTRAINT shipment_preparation_org_fk FOREIGN KEY(organization_preparation_id,organization_id,organization_preparation_version) REFERENCES orders.organization_preparation_version(id,organization_id,version),
 ADD CONSTRAINT shipment_quote_org_fk FOREIGN KEY(quote_id,organization_id) REFERENCES pricing.pilot_quote(id,organization_id),
 ADD CONSTRAINT shipment_owner_variant CHECK(
  (owner_type='PERSONAL' AND owner_id IS NOT NULL AND draft_id IS NOT NULL AND organization_id IS NULL AND facility_id IS NULL AND creator_account_id IS NULL AND organization_preparation_id IS NULL AND organization_preparation_version IS NULL)
  OR (owner_type='BUSINESS' AND owner_id IS NULL AND draft_id IS NULL AND organization_id IS NOT NULL AND creator_account_id IS NOT NULL AND organization_preparation_id IS NOT NULL AND organization_preparation_version IS NOT NULL AND draft_version=organization_preparation_version)),
 ADD CONSTRAINT pilot_shipment_quote_binding CHECK((quote_snapshot->>'id'=quote_id::text AND (quote_snapshot->>'totalVnd')::bigint=total_vnd AND
 CASE owner_type WHEN 'PERSONAL' THEN quote_snapshot->>'accountId'=owner_id::text AND quote_snapshot->>'draftId'=draft_id::text AND (quote_snapshot->>'draftVersion')::bigint=draft_version
 ELSE quote_snapshot->>'organizationId'=organization_id::text AND quote_snapshot->>'preparationId'=organization_preparation_id::text AND (quote_snapshot->>'preparationVersion')::bigint=organization_preparation_version END) IS TRUE),
 ADD UNIQUE(id,organization_id);
CREATE UNIQUE INDEX business_shipment_preparation ON orders.pilot_shipment(organization_id,organization_preparation_id) WHERE owner_type='BUSINESS';
ALTER TABLE finance.pilot_shipping_obligation ALTER COLUMN owner_id DROP NOT NULL, DROP CONSTRAINT pilot_shipping_obligation_policy_version_check,
 ADD COLUMN owner_type VARCHAR(16) NOT NULL DEFAULT 'PERSONAL', ADD COLUMN organization_id UUID, ADD COLUMN facility_id UUID, ADD COLUMN creator_account_id UUID,
 ADD CONSTRAINT obligation_shipment_org_fk FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 ADD CONSTRAINT obligation_facility_org_fk FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 ADD CONSTRAINT obligation_owner_variant CHECK(
 (owner_type='PERSONAL' AND owner_id IS NOT NULL AND organization_id IS NULL AND facility_id IS NULL AND creator_account_id IS NULL AND policy_version='BR13-LOCAL-PILOT-01')
 OR (owner_type='BUSINESS' AND owner_id IS NULL AND organization_id IS NOT NULL AND creator_account_id IS NOT NULL AND policy_version='BR13-BUSINESS-LOCAL-01'));
CREATE TABLE orders.organization_booking_receipt (
 organization_id UUID NOT NULL, actor_account_id UUID NOT NULL, operation_key UUID NOT NULL, facility_id UUID,
 preparation_id UUID NOT NULL, expected_version BIGINT NOT NULL CHECK(expected_version>0), result_version BIGINT NOT NULL CHECK(result_version=expected_version+1),
 quote_id UUID NOT NULL, target_id UUID NOT NULL, fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 recorded_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(organization_id,actor_account_id,operation_key), UNIQUE(target_id),
 FOREIGN KEY(preparation_id,organization_id,result_version) REFERENCES orders.organization_preparation_version(id,organization_id,version),
 FOREIGN KEY(target_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id), FOREIGN KEY(quote_id,organization_id) REFERENCES pricing.pilot_quote(id,organization_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE TRIGGER organization_booking_receipt_append BEFORE UPDATE OR DELETE ON orders.organization_booking_receipt FOR EACH ROW EXECUTE FUNCTION merchant.workspace_append_only();
REVOKE ALL ON orders.organization_booking_receipt FROM PUBLIC,"${runtimeRole}";
GRANT SELECT,INSERT ON orders.organization_booking_receipt TO "${runtimeRole}";

-- Owner explicitly confirmed guard/audit amendment after automated review,2026-11-13.
-- Keep original generated preparation/import foreign keys; BOOK has its own actual version FK.
DO $$ DECLARE c RECORD; BEGIN
 FOR c IN SELECT conname FROM pg_constraint WHERE conrelid='orders.organization_preparation_audit'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%operation%'
 LOOP EXECUTE format('ALTER TABLE orders.organization_preparation_audit DROP CONSTRAINT %I',c.conname); END LOOP;
END $$;
ALTER TABLE orders.organization_preparation_audit ADD CHECK(operation IN ('CREATE','UPDATE','ARCHIVE','IMPORT_PREVIEW','IMPORT_COMMIT','BOOK')),
 ADD COLUMN booking_target UUID GENERATED ALWAYS AS (CASE WHEN operation='BOOK' THEN target_id END) STORED,
 ADD COLUMN booking_version BIGINT GENERATED ALWAYS AS (CASE WHEN operation='BOOK' THEN new_version END) STORED,
 ADD FOREIGN KEY(booking_target,organization_id,booking_version) REFERENCES orders.organization_preparation_version(id,organization_id,version);
CREATE OR REPLACE FUNCTION orders.guard_organization_preparation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'PREPARATION_DELETE_DENIED' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.state<>'DRAFT' OR NEW.preparation_state<>'DRAFT' THEN RAISE EXCEPTION 'PREPARATION_INITIAL_STATE_REQUIRED' USING ERRCODE='23514'; END IF;
 ELSE
  IF NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.creator_account_id<>OLD.creator_account_id OR NEW.created_at<>OLD.created_at
  OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at OR NEW.source_channel IS DISTINCT FROM OLD.source_channel OR NEW.source_order_reference IS DISTINCT FROM OLD.source_order_reference OR OLD.state<>'DRAFT'
  OR (NEW.state='BOOKED' AND (NEW.payload<>OLD.payload OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.preparation_state<>OLD.preparation_state))
  THEN RAISE EXCEPTION 'PREPARATION_IMMUTABLE_BINDING' USING ERRCODE='23514'; END IF;
  IF (NEW.payload<>OLD.payload OR NEW.facility_id IS DISTINCT FROM OLD.facility_id) AND NEW.preparation_state<>'DRAFT' THEN RAISE EXCEPTION 'PREPARATION_PACKING_INVALIDATED' USING ERRCODE='23514'; END IF;
 END IF; RETURN NEW;
END $$;
CREATE OR REPLACE FUNCTION orders.check_organization_preparation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM orders.organization_preparation_version v WHERE v.id=NEW.id AND v.organization_id=NEW.organization_id AND v.version=NEW.version
 AND (v.snapshot->>'id')::uuid=NEW.id AND (v.snapshot->>'organizationId')::uuid=NEW.organization_id AND (v.snapshot->>'version')::bigint=NEW.version
 AND (v.snapshot->>'createdAt')::timestamptz=NEW.created_at AND (v.snapshot->>'updatedAt')::timestamptz=NEW.updated_at
 AND v.snapshot->>'state'=NEW.state AND v.snapshot->>'preparationState'=NEW.preparation_state AND v.snapshot->'input'=NEW.payload
 AND (v.snapshot->>'facilityId')::uuid IS NOT DISTINCT FROM NEW.facility_id AND (v.snapshot->>'creatorAccountId')::uuid=NEW.creator_account_id
 AND v.snapshot->>'sourceChannel' IS NOT DISTINCT FROM NEW.source_channel AND v.snapshot->>'sourceOrderReference' IS NOT DISTINCT FROM NEW.source_order_reference)
 OR NOT EXISTS(SELECT 1 FROM orders.organization_preparation_audit a WHERE a.operation=CASE WHEN NEW.state='BOOKED' THEN 'BOOK' WHEN NEW.state='ARCHIVED' THEN 'ARCHIVE' WHEN NEW.version=1 THEN 'CREATE' ELSE 'UPDATE' END AND a.target_id=NEW.id AND a.organization_id=NEW.organization_id AND a.new_version=NEW.version)
 OR (NEW.state<>'BOOKED' AND NOT EXISTS(SELECT 1 FROM orders.organization_preparation_receipt r WHERE r.operation IN ('CREATE','UPDATE','ARCHIVE') AND r.target_id=NEW.id AND r.organization_id=NEW.organization_id AND r.result_version=NEW.version))
 OR (NEW.state='BOOKED' AND NOT EXISTS(SELECT 1 FROM orders.organization_booking_receipt r JOIN orders.pilot_shipment s ON s.id=r.target_id AND s.organization_id=r.organization_id WHERE r.preparation_id=NEW.id AND r.organization_id=NEW.organization_id AND r.result_version=NEW.version AND s.organization_preparation_id=NEW.id AND s.organization_preparation_version=NEW.version-1))
 THEN RAISE EXCEPTION 'PREPARATION_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE FUNCTION orders.check_business_booking() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE q pricing.pilot_quote; d orders.organization_preparation_version; b orders.organization_booking_receipt;
BEGIN
 IF NEW.owner_type<>'BUSINESS' THEN RETURN NULL; END IF;
 SELECT * INTO q FROM pricing.pilot_quote WHERE id=NEW.quote_id AND owner_type='BUSINESS';
 SELECT * INTO d FROM orders.organization_preparation_version WHERE id=NEW.organization_preparation_id AND organization_id=NEW.organization_id AND version=NEW.organization_preparation_version;
 SELECT * INTO b FROM orders.organization_booking_receipt WHERE target_id=NEW.id AND organization_id=NEW.organization_id;
 IF q.id IS NULL OR d.id IS NULL OR b.target_id IS NULL OR q.consumed_by IS DISTINCT FROM NEW.id OR q.organization_id<>NEW.organization_id
 OR q.facility_id IS DISTINCT FROM NEW.facility_id OR q.creator_account_id<>NEW.creator_account_id OR q.organization_preparation_id<>d.id OR q.organization_preparation_version<>d.version
 -- Keep this inside the already Owner-approved Business-only booking guard.
 -- No additional trigger/function or Personal execution path is introduced.
 OR q.total_vnd<>NEW.total_vnd OR NEW.draft_snapshot IS DISTINCT FROM d.snapshot->'input'
 OR NEW.quote_snapshot->'feeLines' IS DISTINCT FROM q.fee_lines OR NEW.quote_snapshot->'parcels' IS DISTINCT FROM q.parcel_facts
 OR (NEW.quote_snapshot->>'contractId')::uuid IS DISTINCT FROM q.contract_id OR (NEW.quote_snapshot->>'contractVersion')::bigint IS DISTINCT FROM q.contract_version
 OR (NEW.quote_snapshot->>'creatorAccountId')::uuid IS DISTINCT FROM q.creator_account_id OR (NEW.quote_snapshot->>'facilityId')::uuid IS DISTINCT FROM q.facility_id
 OR NEW.quote_snapshot->>'tariffVersion' IS DISTINCT FROM q.tariff_version OR NEW.quote_snapshot->>'taxTreatment' IS DISTINCT FROM 'SIMULATION_NOT_TAX_INVOICE'
 OR (NEW.quote_snapshot->>'createdAt')::timestamptz IS DISTINCT FROM q.created_at OR (NEW.quote_snapshot->>'expiresAt')::timestamptz IS DISTINCT FROM q.expires_at OR (NEW.quote_snapshot->>'consumedBy')::uuid IS DISTINCT FROM NEW.id
 OR NEW.quote_snapshot->'route' IS DISTINCT FROM jsonb_build_object('datasetVersion',q.coverage_version,'originCode',q.origin_code,'destinationCode',q.destination_code,'classification',q.classification,'dropOffCode',q.drop_off_code)
 OR q.parcel_facts IS DISTINCT FROM (SELECT jsonb_agg(jsonb_build_object('weightGrams',p->'weightGrams','lengthMm',p->'lengthMm','widthMm',p->'widthMm','heightMm',p->'heightMm') ORDER BY n) FROM jsonb_array_elements(NEW.draft_snapshot->'parcels') WITH ORDINALITY a(p,n))
 OR NOT EXISTS(SELECT 1 FROM pricing.organization_pilot_contract c WHERE c.id=q.contract_id AND c.version=q.contract_version AND c.organization_id=NEW.organization_id AND c.state='ACTIVE' AND c.tariff_version=q.tariff_version AND c.effective_from<=NEW.created_at AND (c.effective_to IS NULL OR NEW.created_at<c.effective_to) AND NOT EXISTS(SELECT 1 FROM pricing.organization_pilot_contract newer WHERE newer.organization_id=c.organization_id AND newer.version>c.version))
 OR NOT EXISTS(SELECT 1 FROM merchant.pilot_route r JOIN merchant.pilot_area a ON (a.dataset_version,a.code)=(r.dataset_version,r.origin_code) WHERE r.dataset_version=q.coverage_version AND r.origin_code=q.origin_code AND r.destination_code=q.destination_code AND r.classification=q.classification AND r.service_code='ROAD_STANDARD' AND a.drop_off_code=q.drop_off_code)
 OR b.actor_account_id<>NEW.creator_account_id OR b.facility_id IS DISTINCT FROM NEW.facility_id OR b.quote_id<>q.id OR b.preparation_id<>d.id OR b.expected_version<>d.version
 OR NOT EXISTS(SELECT 1 FROM finance.pilot_shipping_obligation f WHERE f.owner_type='BUSINESS' AND f.shipment_id=NEW.id AND f.organization_id=NEW.organization_id AND f.facility_id IS NOT DISTINCT FROM NEW.facility_id AND f.creator_account_id=NEW.creator_account_id AND f.quote_id=q.id AND f.amount_vnd=q.total_vnd)
 OR EXISTS(SELECT 1 FROM orders.pilot_parcel p WHERE p.shipment_id=NEW.id AND jsonb_build_object('description',p.description,'quantity',p.quantity,'weightGrams',p.weight_grams,'lengthMm',p.length_mm,'widthMm',p.width_mm,'heightMm',p.height_mm,'declaredValueVnd',p.declared_value_vnd) IS DISTINCT FROM NEW.draft_snapshot->'parcels'->(p.parcel_number-1))
 THEN RAISE EXCEPTION 'BUSINESS_BOOKING_ATOMIC_BINDING_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER business_booking_evidence AFTER INSERT ON orders.pilot_shipment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_business_booking();
REVOKE ALL ON FUNCTION orders.check_business_booking() FROM PUBLIC,"${runtimeRole}";
