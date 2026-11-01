-- Preparation only. No active carrier service, quote, booking, payment or tracking is created.
CREATE FUNCTION orders.valid_shipping_preparation(p JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE c JSONB; a JSONB; item JSONB; k TEXT;
BEGIN
 IF jsonb_typeof(p)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(p))<>5
 OR NOT p ?& ARRAY['reference','instructions','sender','recipient','parcels'] THEN RETURN FALSE; END IF;
 FOREACH k IN ARRAY ARRAY['reference','instructions'] LOOP
  IF p->k<>'null'::jsonb AND (jsonb_typeof(p->k)<>'string' OR length(btrim(p->>k))<1 OR length(p->>k)>(CASE WHEN k='reference' THEN 80 ELSE 1000 END) OR (p->>k)~'[[:cntrl:]]') THEN RETURN FALSE; END IF;
 END LOOP;
 FOREACH k IN ARRAY ARRAY['sender','recipient'] LOOP
  c=p->k; a=c->'address';
  IF jsonb_typeof(c)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(c))<>3 OR NOT c ?& ARRAY['name','phone','address']
  OR jsonb_typeof(c->'name')<>'string' OR length(btrim(c->>'name')) NOT BETWEEN 1 AND 160 OR (c->>'name')~'[[:cntrl:]]'
  OR jsonb_typeof(c->'phone')<>'string' OR (c->>'phone')!~'^\+84[35789][0-9]{8}$'
  OR jsonb_typeof(a)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(a))<>4 OR NOT a ?& ARRAY['country','administrativeArea','locality','addressLine'] OR a->>'country'<>'VN' THEN RETURN FALSE; END IF;
  IF jsonb_typeof(a->'country')<>'string' THEN RETURN FALSE; END IF;
  FOR item IN SELECT value FROM jsonb_each(a-'country') LOOP
   IF jsonb_typeof(item)<>'string' OR length(btrim(item#>>'{}'))<1 OR length(item#>>'{}')>500 OR (item#>>'{}')~'[[:cntrl:]]' THEN RETURN FALSE; END IF;
  END LOOP;
  IF length(a->>'administrativeArea')>160 OR length(a->>'locality')>160 THEN RETURN FALSE; END IF;
 END LOOP;
 IF jsonb_typeof(p->'parcels')<>'array' OR jsonb_array_length(p->'parcels') NOT BETWEEN 1 AND 5 THEN RETURN FALSE; END IF;
 FOR item IN SELECT value FROM jsonb_array_elements(p->'parcels') LOOP
  IF jsonb_typeof(item)<>'object' OR (SELECT count(*) FROM jsonb_object_keys(item))<>7 OR NOT item ?& ARRAY['description','quantity','weightGrams','lengthMm','widthMm','heightMm','declaredValueVnd']
  OR jsonb_typeof(item->'description')<>'string' OR length(btrim(item->>'description')) NOT BETWEEN 1 AND 160 OR (item->>'description')~'[[:cntrl:]]' THEN RETURN FALSE; END IF;
  FOREACH k IN ARRAY ARRAY['quantity','weightGrams','lengthMm','widthMm','heightMm','declaredValueVnd'] LOOP
   IF jsonb_typeof(item->k)<>'number' OR (item->>k)!~'^[0-9]+$' THEN RETURN FALSE; END IF;
   IF (item->>k)::numeric<(CASE WHEN k='declaredValueVnd' THEN 0 ELSE 1 END) OR (item->>k)::numeric>(CASE WHEN k='declaredValueVnd' THEN 1000000000000 WHEN k='weightGrams' THEN 1000000000 ELSE 1000000 END) THEN RETURN FALSE; END IF;
  END LOOP;
 END LOOP;
 RETURN TRUE;
EXCEPTION WHEN OTHERS THEN RETURN FALSE;
END $$;
REVOKE ALL ON FUNCTION orders.valid_shipping_preparation(JSONB) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.valid_shipping_preparation(JSONB) TO "${runtimeRole}";
CREATE TABLE orders.shipping_draft (
 id UUID PRIMARY KEY, owner_type VARCHAR(16) NOT NULL CHECK(owner_type='PERSONAL'), owner_id UUID NOT NULL,
 state VARCHAR(16) NOT NULL CHECK(state IN ('DRAFT','ARCHIVED')), version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at), UNIQUE(id,owner_id)
);
CREATE TABLE orders.shipping_draft_version (
 id UUID NOT NULL, owner_id UUID NOT NULL, version BIGINT NOT NULL CHECK(version>0), state VARCHAR(16) NOT NULL CHECK(state IN ('DRAFT','ARCHIVED')),
 payload JSONB NOT NULL CHECK(orders.valid_shipping_preparation(payload) IS TRUE), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 PRIMARY KEY(id,version), UNIQUE(id,owner_id,version,state), UNIQUE(id,owner_id,version), FOREIGN KEY(id,owner_id) REFERENCES orders.shipping_draft(id,owner_id)
);
ALTER TABLE orders.shipping_draft ADD CONSTRAINT fk_shipping_draft_current FOREIGN KEY(id,owner_id,version,state) REFERENCES orders.shipping_draft_version(id,owner_id,version,state) DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX idx_shipping_draft_owner_page ON orders.shipping_draft(owner_id,updated_at DESC,id ASC) WHERE state='DRAFT';
CREATE FUNCTION orders.guard_shipping_draft() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id<>OLD.id OR NEW.owner_type<>OLD.owner_type OR NEW.owner_id<>OLD.owner_id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR OLD.state<>'DRAFT' OR NEW.updated_at<OLD.updated_at THEN
  RAISE EXCEPTION 'SHIPPING_DRAFT_IMMUTABLE_BINDING_OR_VERSION' USING ERRCODE='23514';
 END IF; RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION orders.guard_shipping_draft() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.guard_shipping_draft() TO "${runtimeRole}";
CREATE TRIGGER guard_shipping_draft BEFORE UPDATE ON orders.shipping_draft FOR EACH ROW EXECUTE FUNCTION orders.guard_shipping_draft();
CREATE TABLE orders.shipping_draft_audit (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, target_id UUID NOT NULL, operation VARCHAR(32) NOT NULL CHECK(operation IN ('createShippingDraft','updateShippingDraft','archiveShippingDraft')),
 prior_version BIGINT NOT NULL CHECK(prior_version>=0), new_version BIGINT NOT NULL CHECK(new_version=prior_version+1), request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(target_id,owner_id,new_version) REFERENCES orders.shipping_draft_version(id,owner_id,version),
 CHECK((operation='createShippingDraft' AND prior_version=0) OR (operation<>'createShippingDraft' AND prior_version>0))
);
CREATE TABLE orders.shipping_draft_receipt (
 owner_id UUID NOT NULL, operation VARCHAR(32) NOT NULL CHECK(operation IN ('createShippingDraft','updateShippingDraft','archiveShippingDraft')), operation_key UUID NOT NULL,
 target_id UUID NOT NULL, original_version BIGINT NOT NULL CHECK(original_version>=0), result_version BIGINT NOT NULL CHECK(result_version=original_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32), created_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(owner_id,operation,operation_key), FOREIGN KEY(target_id,owner_id,result_version) REFERENCES orders.shipping_draft_version(id,owner_id,version),
 CHECK((operation='createShippingDraft' AND original_version=0) OR (operation<>'createShippingDraft' AND original_version>0))
);
REVOKE ALL ON orders.shipping_draft,orders.shipping_draft_version,orders.shipping_draft_audit,orders.shipping_draft_receipt FROM PUBLIC;
GRANT SELECT,INSERT,UPDATE ON orders.shipping_draft TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.shipping_draft_version,orders.shipping_draft_audit,orders.shipping_draft_receipt TO "${runtimeRole}";
