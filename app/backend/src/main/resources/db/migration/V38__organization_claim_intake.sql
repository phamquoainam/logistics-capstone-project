-- Owner standing source/test authority; document intake only, compensation remains BLOCKED.
-- No seed/grant, commercial eligibility policy, financial write or Carrier transition.
ALTER TABLE orders.organization_support_ticket ADD CONSTRAINT organization_support_claim_source UNIQUE(id,organization_id,shipment_id);

CREATE TABLE orders.organization_claim_intake (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL,facility_id UUID,ticket_id UUID NOT NULL,shipment_id UUID NOT NULL,
 creator_account_id UUID NOT NULL,category VARCHAR(20) NOT NULL CHECK(category IN ('LOSS','DAMAGE','DELAY','SERVICE_DISPUTE')),
 state VARCHAR(24) NOT NULL CHECK(state IN ('RECEIVED','UNDER_REVIEW','NEEDS_INFORMATION','CLOSED_WITHOUT_DECISION')),
 version BIGINT NOT NULL CHECK(version>0),created_at TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 UNIQUE(id,organization_id),UNIQUE(id,organization_id,ticket_id),
 FOREIGN KEY(ticket_id,organization_id,shipment_id) REFERENCES orders.organization_support_ticket(id,organization_id,shipment_id),
 FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 FOREIGN KEY(organization_id,creator_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id)
);
CREATE UNIQUE INDEX organization_claim_one_open ON orders.organization_claim_intake(organization_id,shipment_id,category) WHERE state<>'CLOSED_WITHOUT_DECISION';
CREATE INDEX organization_claim_page ON orders.organization_claim_intake(organization_id,updated_at DESC,id);

CREATE TABLE orders.organization_claim_intake_event (
 id UUID PRIMARY KEY,intake_id UUID NOT NULL,organization_id UUID NOT NULL,actor_account_id UUID NOT NULL REFERENCES identity.user_account(id),
 author_kind VARCHAR(8) NOT NULL CHECK(author_kind IN ('MERCHANT','SUPPORT')),
 operation VARCHAR(24) NOT NULL CHECK(operation IN ('CREATE','SUPPLEMENT','BEGIN_REVIEW','REQUEST_INFORMATION','CLOSE_WITHOUT_DECISION')),
 message VARCHAR(4000) NOT NULL CHECK(length(btrim(message))>0 AND regexp_replace(message,E'[\n\t]','','g') !~ '[[:cntrl:]]'),
 prior_version BIGINT NOT NULL CHECK(prior_version>=0),new_version BIGINT NOT NULL CHECK(new_version=prior_version+1),
 prior_state VARCHAR(24),new_state VARCHAR(24) NOT NULL,
 CHECK((prior_version=0)=(prior_state IS NULL)),occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE(intake_id,new_version),UNIQUE(intake_id,new_version,new_state),UNIQUE(intake_id,organization_id,new_version,actor_account_id,operation),
 FOREIGN KEY(intake_id,organization_id) REFERENCES orders.organization_claim_intake(id,organization_id),
 FOREIGN KEY(intake_id,prior_version,prior_state) REFERENCES orders.organization_claim_intake_event(intake_id,new_version,new_state) DEFERRABLE INITIALLY DEFERRED,
 CHECK(
  (operation='CREATE' AND author_kind='MERCHANT' AND prior_version=0 AND prior_state IS NULL AND new_state='RECEIVED') OR
  (operation='SUPPLEMENT' AND author_kind='MERCHANT' AND prior_version>0 AND prior_state IN ('RECEIVED','UNDER_REVIEW','NEEDS_INFORMATION') AND new_state=CASE prior_state WHEN 'NEEDS_INFORMATION' THEN 'UNDER_REVIEW' ELSE prior_state END) OR
  (operation='BEGIN_REVIEW' AND author_kind='SUPPORT' AND prior_state='RECEIVED' AND new_state='UNDER_REVIEW') OR
  (operation='REQUEST_INFORMATION' AND author_kind='SUPPORT' AND prior_state IN ('RECEIVED','UNDER_REVIEW') AND new_state='NEEDS_INFORMATION') OR
  (operation='CLOSE_WITHOUT_DECISION' AND author_kind='SUPPORT' AND prior_state IN ('RECEIVED','UNDER_REVIEW','NEEDS_INFORMATION') AND new_state='CLOSED_WITHOUT_DECISION')
 )
);
CREATE TABLE orders.organization_claim_intake_receipt (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL,actor_account_id UUID NOT NULL REFERENCES identity.user_account(id),
 operation VARCHAR(24) NOT NULL CHECK(operation IN ('CREATE','SUPPLEMENT','BEGIN_REVIEW','REQUEST_INFORMATION','CLOSE_WITHOUT_DECISION')),
 operation_key UUID NOT NULL CHECK(operation_key<>'00000000-0000-0000-0000-000000000000'),intake_id UUID NOT NULL,
 original_version BIGINT NOT NULL CHECK(original_version>=0),result_version BIGINT NOT NULL CHECK(result_version=original_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK(fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 request_id UUID NOT NULL,created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(organization_id,actor_account_id,operation,operation_key),UNIQUE(intake_id,result_version),
 FOREIGN KEY(intake_id,organization_id,result_version,actor_account_id,operation) REFERENCES orders.organization_claim_intake_event(intake_id,organization_id,new_version,actor_account_id,operation) DEFERRABLE INITIALLY DEFERRED,
 CHECK((operation='CREATE')=(original_version=0))
);
CREATE FUNCTION orders.guard_claim_intake() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'CLAIM_INTAKE_IMMUTABLE' USING ERRCODE='42501';END IF;
 IF NOT EXISTS(SELECT 1 FROM orders.organization_support_ticket t JOIN merchant.organization o ON o.id=t.organization_id
  WHERE (t.id,t.organization_id,t.shipment_id)=(NEW.ticket_id,NEW.organization_id,NEW.shipment_id) AND t.kind='SHIPMENT'
  AND t.facility_id IS NOT DISTINCT FROM NEW.facility_id AND o.environment IN ('LOCAL','TEST')) THEN
  RAISE EXCEPTION 'CLAIM_TICKET_SHIPMENT_BINDING_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'RECEIVED' OR NEW.version<>1 OR NEW.created_at<>NEW.updated_at THEN RAISE EXCEPTION 'CLAIM_INITIAL_STATE_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF (NEW.id,NEW.organization_id,NEW.ticket_id,NEW.shipment_id,NEW.creator_account_id,NEW.category,NEW.created_at)
   IS DISTINCT FROM (OLD.id,OLD.organization_id,OLD.ticket_id,OLD.shipment_id,OLD.creator_account_id,OLD.category,OLD.created_at)
   OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at OR OLD.state='CLOSED_WITHOUT_DECISION' THEN
   RAISE EXCEPTION 'CLAIM_IDENTITY_OR_VERSION_IMMUTABLE' USING ERRCODE='23514';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER organization_claim_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_claim_intake FOR EACH ROW EXECUTE FUNCTION orders.guard_claim_intake();

CREATE FUNCTION orders.guard_claim_event() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c orders.organization_claim_intake;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'CLAIM_HISTORY_IMMUTABLE' USING ERRCODE='42501';END IF;
 SELECT * INTO c FROM orders.organization_claim_intake WHERE id=NEW.intake_id AND organization_id=NEW.organization_id;
 IF NEW.author_kind='MERCHANT' THEN
  IF NOT EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
   JOIN merchant.member_grant g ON g.membership_id=m.id AND g.organization_id=m.organization_id
   JOIN identity.user_account a ON a.id=m.account_id JOIN identity.customer_identifier ci ON ci.account_id=a.id
   WHERE m.organization_id=c.organization_id AND m.account_id=NEW.actor_account_id AND m.state='ACTIVE' AND o.state='ACTIVE'
   AND a.status='ACTIVE' AND a.email_verified AND ci.status='ACTIVE' AND ci.exact_email=a.email COLLATE "C"
   AND g.permission='support.write' AND (g.scope='ORGANIZATION' OR g.facility_id=c.facility_id)) THEN
   RAISE EXCEPTION 'CLAIM_CURRENT_MERCHANT_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
  IF NEW.operation='CREATE' AND c.creator_account_id<>NEW.actor_account_id THEN RAISE EXCEPTION 'CLAIM_CREATOR_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT orders.organization_support_staff(c.organization_id,NEW.actor_account_id,'SUPPORT_HANDLE') OR NOT EXISTS(
   SELECT 1 FROM orders.organization_support_case_assignment x WHERE x.ticket_id=c.ticket_id AND x.organization_id=c.organization_id AND x.assignee_account_id=NEW.actor_account_id AND x.state='ACTIVE') THEN
   RAISE EXCEPTION 'CLAIM_ASSIGNED_SUPPORT_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER organization_claim_event_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_claim_intake_event FOR EACH ROW EXECUTE FUNCTION orders.guard_claim_event();
CREATE FUNCTION orders.check_claim_evidence() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE target UUID;c orders.organization_claim_intake;
BEGIN
 IF TG_TABLE_NAME='organization_claim_intake' THEN target:=NEW.id;ELSE target:=NEW.intake_id;END IF;
 SELECT * INTO c FROM orders.organization_claim_intake WHERE id=target;
 IF c.id IS NULL OR (SELECT count(*) FROM orders.organization_claim_intake_event WHERE intake_id=target)<>c.version
  OR NOT EXISTS(SELECT 1 FROM orders.organization_claim_intake_event e WHERE e.intake_id=target AND e.new_version=c.version AND e.new_state=c.state AND e.occurred_at=c.updated_at)
  OR EXISTS(SELECT 1 FROM orders.organization_claim_intake_event e WHERE e.intake_id=target AND NOT EXISTS(
    SELECT 1 FROM orders.organization_claim_intake_receipt r WHERE r.intake_id=e.intake_id AND r.result_version=e.new_version AND r.actor_account_id=e.actor_account_id AND r.operation=e.operation)) THEN
  RAISE EXCEPTION 'CLAIM_ATOMIC_HISTORY_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_claim_evidence AFTER INSERT OR UPDATE ON orders.organization_claim_intake DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_claim_evidence();
CREATE CONSTRAINT TRIGGER organization_claim_event_evidence AFTER INSERT ON orders.organization_claim_intake_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_claim_evidence();
CREATE CONSTRAINT TRIGGER organization_claim_receipt_evidence AFTER INSERT ON orders.organization_claim_intake_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_claim_evidence();
REVOKE ALL ON orders.organization_claim_intake,orders.organization_claim_intake_event,orders.organization_claim_intake_receipt FROM PUBLIC;
GRANT SELECT,INSERT ON orders.organization_claim_intake,orders.organization_claim_intake_event,orders.organization_claim_intake_receipt TO "${runtimeRole}";
GRANT UPDATE(state,version,updated_at) ON orders.organization_claim_intake TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_claim_intake(),orders.guard_claim_event(),orders.check_claim_evidence() FROM PUBLIC;

CREATE FUNCTION orders.guard_claim_receipt() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'CLAIM_RECEIPT_IMMUTABLE' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER organization_claim_receipt_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_claim_intake_receipt FOR EACH ROW EXECUTE FUNCTION orders.guard_claim_receipt();
REVOKE ALL ON FUNCTION orders.guard_claim_receipt() FROM PUBLIC;

-- A reservation exists before external I/O. Quarantine objects never become readable
-- until the current domain transaction records CLEAN and its own supplement event.
CREATE TABLE orders.organization_claim_evidence (
 id UUID PRIMARY KEY,intake_id UUID NOT NULL,organization_id UUID NOT NULL,actor_account_id UUID NOT NULL,
 operation_key UUID NOT NULL,object_key UUID NOT NULL UNIQUE,
 mime VARCHAR(32) NOT NULL CHECK(mime IN ('image/png','image/jpeg','application/pdf')),
 bytes BIGINT NOT NULL CHECK(bytes BETWEEN 1 AND 5242880),sha256 VARCHAR(64) NOT NULL CHECK(sha256 ~ '^[0-9a-f]{64}$'),
 state VARCHAR(8) NOT NULL CHECK(state IN ('RESERVED','CLEAN')),
 original_version BIGINT NOT NULL CHECK(original_version>0),reserved_version BIGINT NOT NULL CHECK(reserved_version=original_version+1),attached_version BIGINT,
 fingerprint_key_id VARCHAR(64) NOT NULL CHECK(fingerprint_key_id ~ '^[A-Za-z0-9_-]{1,64}$'),fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 created_at TIMESTAMPTZ NOT NULL,expires_at TIMESTAMPTZ NOT NULL CHECK(expires_at=created_at+INTERVAL '15 minutes'),attached_at TIMESTAMPTZ,
 UNIQUE(organization_id,actor_account_id,operation_key),
 UNIQUE(intake_id,reserved_version),UNIQUE(intake_id,attached_version),
 FOREIGN KEY(intake_id,organization_id) REFERENCES orders.organization_claim_intake(id,organization_id),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(intake_id,reserved_version) REFERENCES orders.organization_claim_intake_event(intake_id,new_version) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(intake_id,attached_version) REFERENCES orders.organization_claim_intake_event(intake_id,new_version) DEFERRABLE INITIALLY DEFERRED,
 CHECK((state='CLEAN')=(attached_version IS NOT NULL AND attached_at IS NOT NULL)),
 CHECK(attached_version IS NULL OR attached_version>reserved_version),CHECK(attached_at IS NULL OR attached_at>=created_at)
);
CREATE FUNCTION orders.guard_claim_file() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c orders.organization_claim_intake;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'CLAIM_FILE_IMMUTABLE' USING ERRCODE='42501';END IF;
 SELECT * INTO c FROM orders.organization_claim_intake WHERE id=NEW.intake_id AND organization_id=NEW.organization_id;
 IF c.id IS NULL OR c.state='CLOSED_WITHOUT_DECISION' OR NOT orders.organization_support_permission(c.organization_id,NEW.actor_account_id,c.facility_id,'support.write') THEN
  RAISE EXCEPTION 'CLAIM_FILE_CURRENT_AUTHORITY_REQUIRED' USING ERRCODE='42501';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'RESERVED' OR NEW.attached_at IS NOT NULL OR NEW.attached_version IS NOT NULL OR NEW.reserved_version<>c.version OR NEW.created_at>clock_timestamp()+INTERVAL '1 minute'
   OR (SELECT count(*) FROM orders.organization_claim_evidence WHERE intake_id=c.id)>=10
   OR (SELECT coalesce(sum(bytes),0) FROM orders.organization_claim_evidence WHERE intake_id=c.id)+NEW.bytes>26214400 THEN
   RAISE EXCEPTION 'CLAIM_FILE_RESERVATION_OR_QUOTA_INVALID' USING ERRCODE='23514';END IF;
 ELSE
  IF (NEW.id,NEW.intake_id,NEW.organization_id,NEW.actor_account_id,NEW.operation_key,NEW.object_key,NEW.mime,NEW.bytes,NEW.sha256,NEW.original_version,NEW.reserved_version,NEW.fingerprint_key_id,NEW.fingerprint,NEW.created_at,NEW.expires_at)
   IS DISTINCT FROM (OLD.id,OLD.intake_id,OLD.organization_id,OLD.actor_account_id,OLD.operation_key,OLD.object_key,OLD.mime,OLD.bytes,OLD.sha256,OLD.original_version,OLD.reserved_version,OLD.fingerprint_key_id,OLD.fingerprint,OLD.created_at,OLD.expires_at)
   OR OLD.state<>'RESERVED' OR NEW.state<>'CLEAN' OR NEW.attached_version<>c.version OR clock_timestamp()>=OLD.expires_at THEN
   RAISE EXCEPTION 'CLAIM_FILE_BINDING_OR_STATE_INVALID' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_claim_file_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_claim_evidence FOR EACH ROW EXECUTE FUNCTION orders.guard_claim_file();
CREATE FUNCTION orders.check_claim_file_event() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE v BIGINT;stamp TIMESTAMPTZ;
BEGIN
 IF TG_OP='INSERT' THEN v:=NEW.reserved_version;stamp:=NEW.created_at;ELSE v:=NEW.attached_version;stamp:=NEW.attached_at;END IF;
 IF NOT EXISTS(SELECT 1 FROM orders.organization_claim_intake_event e JOIN orders.organization_claim_intake_receipt r ON r.intake_id=e.intake_id AND r.result_version=e.new_version
  WHERE e.intake_id=NEW.intake_id AND e.organization_id=NEW.organization_id AND e.new_version=v AND e.actor_account_id=NEW.actor_account_id
  AND e.author_kind='MERCHANT' AND e.operation='SUPPLEMENT' AND e.occurred_at=stamp AND e.xmin::TEXT=pg_current_xact_id()::TEXT AND r.xmin::TEXT=pg_current_xact_id()::TEXT) THEN
  RAISE EXCEPTION 'CLAIM_FILE_ATOMIC_EVENT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_claim_file_event AFTER INSERT OR UPDATE ON orders.organization_claim_evidence DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_claim_file_event();
REVOKE ALL ON orders.organization_claim_evidence FROM PUBLIC;
GRANT SELECT,INSERT ON orders.organization_claim_evidence TO "${runtimeRole}";
GRANT UPDATE(state,attached_version,attached_at) ON orders.organization_claim_evidence TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_claim_file(),orders.check_claim_file_event() FROM PUBLIC;

-- Event-sourced in-app feed only. No email channel, fictitious SLA or financial decision.
CREATE TABLE orders.organization_claim_notice (
 id UUID PRIMARY KEY,intake_id UUID NOT NULL,organization_id UUID NOT NULL,event_id UUID NOT NULL,
 recipient_account_id UUID NOT NULL,type VARCHAR(20) NOT NULL CHECK(type IN ('REVIEW','NEEDS_INFORMATION','REPLY')),created_at TIMESTAMPTZ NOT NULL,
 UNIQUE(event_id,recipient_account_id),
 FOREIGN KEY(intake_id,organization_id) REFERENCES orders.organization_claim_intake(id,organization_id),
 FOREIGN KEY(event_id) REFERENCES orders.organization_claim_intake_event(id),
 FOREIGN KEY(organization_id,recipient_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE FUNCTION orders.guard_claim_notice() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c orders.organization_claim_intake;e orders.organization_claim_intake_event;expected TEXT;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'CLAIM_NOTICE_IMMUTABLE' USING ERRCODE='42501';END IF;
 SELECT * INTO c FROM orders.organization_claim_intake WHERE id=NEW.intake_id AND organization_id=NEW.organization_id;
 SELECT * INTO e FROM orders.organization_claim_intake_event WHERE id=NEW.event_id AND intake_id=c.id AND organization_id=c.organization_id;
 expected:=CASE WHEN e.operation='REQUEST_INFORMATION' THEN 'NEEDS_INFORMATION' WHEN e.author_kind='SUPPORT' THEN 'REVIEW' ELSE 'REPLY' END;
 IF e.id IS NULL OR e.operation='CREATE' OR NOT EXISTS(SELECT 1 FROM orders.organization_claim_intake_event src WHERE src.id=e.id AND src.xmin::TEXT=pg_current_xact_id()::TEXT) OR NEW.type<>expected OR NEW.created_at<>e.occurred_at OR NEW.recipient_account_id=e.actor_account_id
  OR NOT orders.organization_support_permission(c.organization_id,NEW.recipient_account_id,c.facility_id,'support.read')
  OR NOT EXISTS(SELECT 1 FROM orders.organization_support_notification_preference p WHERE p.organization_id=c.organization_id AND p.account_id=NEW.recipient_account_id AND p.enabled AND NEW.type=ANY(p.event_types))
  OR (NEW.recipient_account_id<>c.creator_account_id AND NOT EXISTS(SELECT 1 FROM orders.organization_support_assignment a WHERE a.ticket_id=c.ticket_id AND a.organization_id=c.organization_id AND a.assignee_account_id=NEW.recipient_account_id)) THEN
  RAISE EXCEPTION 'CLAIM_NOTICE_CURRENT_EVENT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_claim_notice_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_claim_notice FOR EACH ROW EXECUTE FUNCTION orders.guard_claim_notice();
REVOKE ALL ON orders.organization_claim_notice FROM PUBLIC;
GRANT SELECT,INSERT ON orders.organization_claim_notice TO "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_claim_notice() FROM PUBLIC;
