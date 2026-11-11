-- Owner-approved Ticket V1, PERSONAL Local/Test. No Staff seed, Claim or Shipment/Finance mutation.
CREATE TABLE orders.personal_support_ticket (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL, code VARCHAR(37) NOT NULL UNIQUE,
 kind VARCHAR(8) NOT NULL CHECK(kind IN ('GENERAL','SHIPMENT')), shipment_id UUID,
 category VARCHAR(24) NOT NULL CHECK(category IN ('ACCOUNT_HELP','SHIPMENT_TRACE','FEE_REVIEW','OTHER')),
 subject VARCHAR(160) NOT NULL CHECK(char_length(btrim(subject))>0 AND subject !~ '[[:cntrl:]]'),
 state VARCHAR(20) NOT NULL CHECK(state IN ('NEW','IN_PROGRESS','NEEDS_INFORMATION','RESOLVED','CLOSED')),
 version BIGINT NOT NULL CHECK(version>0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 resolution_version BIGINT, review_pending BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE(id,owner_id), FOREIGN KEY(shipment_id,owner_id) REFERENCES orders.pilot_shipment(id,owner_id),
 CHECK((kind='GENERAL' AND shipment_id IS NULL) OR (kind='SHIPMENT' AND shipment_id IS NOT NULL)),
 CHECK(category<>'SHIPMENT_TRACE' OR kind='SHIPMENT'),
 CHECK(resolution_version IS NULL OR resolution_version BETWEEN 1 AND version),
 CHECK(NOT review_pending OR (state='RESOLVED' AND resolution_version IS NOT NULL))
);
CREATE INDEX support_owner_page ON orders.personal_support_ticket(owner_id,updated_at DESC,id);
CREATE TABLE orders.personal_support_message (
 id UUID PRIMARY KEY, ticket_id UUID NOT NULL, owner_id UUID NOT NULL,
 ticket_version BIGINT NOT NULL CHECK(ticket_version>0),
 author_kind VARCHAR(8) NOT NULL CHECK(author_kind IN ('CUSTOMER','SUPPORT')),
 message VARCHAR(4000) NOT NULL CHECK(char_length(btrim(message))>0 AND regexp_replace(message,E'[\n\t]','','g') !~ '[[:cntrl:]]'),
 event_type VARCHAR(24) NOT NULL CHECK(event_type IN ('CREATED','REPLY','REQUEST_REVIEW','ACKNOWLEDGE','ASSIGN','START','REQUEST_INFORMATION','RESOLVE','REOPEN')),
 created_at TIMESTAMPTZ NOT NULL, UNIQUE(ticket_id,ticket_version),
 FOREIGN KEY(ticket_id,owner_id) REFERENCES orders.personal_support_ticket(id,owner_id)
);
CREATE TABLE orders.support_staff_grant (
 id UUID PRIMARY KEY, account_id UUID NOT NULL, permission VARCHAR(20) NOT NULL CHECK(permission IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')), version BIGINT NOT NULL CHECK(version>0), effective_from TIMESTAMPTZ NOT NULL
);
CREATE UNIQUE INDEX support_current_grant ON orders.support_staff_grant(account_id,permission) WHERE state='ACTIVE';
CREATE TABLE orders.personal_support_assignment (
 id UUID PRIMARY KEY, ticket_id UUID NOT NULL, owner_id UUID NOT NULL, assignee_id UUID NOT NULL,
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')), version BIGINT NOT NULL CHECK(version>0),
 assigned_at TIMESTAMPTZ NOT NULL, FOREIGN KEY(ticket_id,owner_id) REFERENCES orders.personal_support_ticket(id,owner_id)
);
CREATE UNIQUE INDEX support_current_case_assignment ON orders.personal_support_assignment(ticket_id) WHERE state='ACTIVE';
CREATE TABLE orders.support_command_receipt (
 actor_id UUID NOT NULL, operation VARCHAR(24) NOT NULL CHECK(operation IN ('CREATE','REPLY','REQUEST_REVIEW','ACKNOWLEDGE','ASSIGN','TRANSITION')),
 operation_key UUID NOT NULL, ticket_id UUID NOT NULL, owner_id UUID NOT NULL,
 original_version BIGINT NOT NULL CHECK(original_version>=0), result_version BIGINT NOT NULL CHECK(result_version=original_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL, fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),
 created_at TIMESTAMPTZ NOT NULL, PRIMARY KEY(actor_id,operation,operation_key),
 FOREIGN KEY(ticket_id,owner_id) REFERENCES orders.personal_support_ticket(id,owner_id)
);
CREATE TABLE orders.support_business_audit (
 id UUID PRIMARY KEY, actor_id UUID, ticket_id UUID, owner_id UUID, operation VARCHAR(24) NOT NULL,
 prior_version BIGINT NOT NULL CHECK(prior_version>=0), new_version BIGINT NOT NULL CHECK(new_version=prior_version+1),
 request_id UUID, occurred_at TIMESTAMPTZ NOT NULL, operator_principal TEXT, authority_grant_id UUID REFERENCES orders.support_staff_grant(id),
 FOREIGN KEY(ticket_id,owner_id) REFERENCES orders.personal_support_ticket(id,owner_id),
 CHECK((ticket_id IS NOT NULL AND owner_id IS NOT NULL AND actor_id IS NOT NULL AND request_id IS NOT NULL AND operator_principal IS NULL AND authority_grant_id IS NULL)
 OR (ticket_id IS NULL AND owner_id IS NULL AND actor_id IS NOT NULL AND request_id IS NULL AND operator_principal IS NOT NULL AND authority_grant_id IS NOT NULL AND operation IN ('GRANT','REVOKE')))
);
CREATE TABLE orders.support_request_window (
 owner_id UUID NOT NULL, window_start TIMESTAMPTZ NOT NULL, kind VARCHAR(12) NOT NULL CHECK(kind IN ('CREATE','CONVERSATION')),
 count INTEGER NOT NULL CHECK(count>0 AND ((kind='CREATE' AND count<=5) OR (kind='CONVERSATION' AND count<=20))), PRIMARY KEY(owner_id,window_start,kind)
);
CREATE FUNCTION orders.guard_support_authority() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730022,1);
 IF TG_OP='UPDATE' THEN
  IF NEW.id<>OLD.id OR NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'SUPPORT_AUTHORITY_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
  IF TG_TABLE_NAME='support_staff_grant' THEN
   IF NEW.account_id<>OLD.account_id OR NEW.permission<>OLD.permission OR NEW.effective_from<>OLD.effective_from THEN RAISE EXCEPTION 'SUPPORT_GRANT_IDENTITY_IMMUTABLE' USING ERRCODE='23514'; END IF;
  ELSE
   IF NEW.ticket_id<>OLD.ticket_id OR NEW.owner_id<>OLD.owner_id OR NEW.assignee_id<>OLD.assignee_id OR NEW.assigned_at<>OLD.assigned_at THEN RAISE EXCEPTION 'SUPPORT_ASSIGNMENT_IDENTITY_IMMUTABLE' USING ERRCODE='23514'; END IF;
  END IF;
 END IF;
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'SUPPORT_AUTHORITY_DELETE_DENIED' USING ERRCODE='23514'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER support_grant_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.support_staff_grant FOR EACH ROW EXECUTE FUNCTION orders.guard_support_authority();
CREATE TRIGGER support_assignment_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.personal_support_assignment FOR EACH ROW EXECUTE FUNCTION orders.guard_support_authority();
CREATE FUNCTION orders.configure_local_support_staff(target UUID,permission_name TEXT,action TEXT) RETURNS UUID LANGUAGE plpgsql AS $$
DECLARE assigned UUID; revision BIGINT;
BEGIN
 IF current_setting('argopost.operator_environment',TRUE) IS NULL OR current_setting('argopost.operator_environment',TRUE) NOT IN ('LOCAL','TEST')
 OR target IS NULL OR permission_name IS NULL OR permission_name NOT IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE') OR action IS NULL OR action NOT IN ('GRANT','REVOKE') THEN RAISE EXCEPTION 'EXPLICIT_LOCAL_SUPPORT_DISPOSITION_REQUIRED' USING ERRCODE='23514'; END IF;
 -- Serializes grant/revocation with the Support authority lock held by runtime admission.
 PERFORM pg_advisory_xact_lock(730022,1);
 IF NOT EXISTS(SELECT 1 FROM identity.user_account WHERE id=target) OR (action='GRANT' AND NOT EXISTS(SELECT 1 FROM identity.user_account WHERE id=target AND status='ACTIVE' AND email_verified)) THEN RAISE EXCEPTION 'EXISTING_VERIFIED_SUPPORT_ACCOUNT_REQUIRED' USING ERRCODE='23514'; END IF;
 SELECT id,version INTO assigned,revision FROM orders.support_staff_grant WHERE account_id=target AND permission=permission_name AND state='ACTIVE';
 IF action='GRANT' THEN
  IF assigned IS NOT NULL THEN RETURN assigned; END IF;
  assigned=gen_random_uuid();revision=1;
  INSERT INTO orders.support_staff_grant VALUES(assigned,target,permission_name,'ACTIVE',revision,clock_timestamp());
 ELSE
  IF assigned IS NULL THEN RETURN NULL; END IF;
  revision=revision+1;UPDATE orders.support_staff_grant SET state='REVOKED',version=revision WHERE id=assigned;
 END IF;
 INSERT INTO orders.support_business_audit VALUES(gen_random_uuid(),target,NULL,NULL,action,revision-1,revision,NULL,clock_timestamp(),session_user,assigned);
 RETURN assigned;
END $$;
CREATE FUNCTION orders.support_append_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'SUPPORT_EVIDENCE_APPEND_ONLY' USING ERRCODE='23514'; END $$;
CREATE TRIGGER support_message_immutable BEFORE UPDATE OR DELETE ON orders.personal_support_message FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER support_receipt_immutable BEFORE UPDATE OR DELETE ON orders.support_command_receipt FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER support_audit_immutable BEFORE UPDATE OR DELETE ON orders.support_business_audit FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE FUNCTION orders.guard_support_ticket() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.id<>OLD.id OR NEW.owner_id<>OLD.owner_id OR NEW.code<>OLD.code OR NEW.kind<>OLD.kind OR NEW.shipment_id IS DISTINCT FROM OLD.shipment_id
 OR NEW.category<>OLD.category OR NEW.subject<>OLD.subject OR NEW.created_at<>OLD.created_at OR NEW.updated_at<OLD.updated_at OR NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'SUPPORT_TICKET_IMMUTABLE_BINDING' USING ERRCODE='23514'; END IF;
 IF NOT ((OLD.state='NEW' AND NEW.state IN ('NEW','IN_PROGRESS')) OR (OLD.state='IN_PROGRESS' AND NEW.state IN ('IN_PROGRESS','NEEDS_INFORMATION','RESOLVED'))
 OR (OLD.state='NEEDS_INFORMATION' AND NEW.state='IN_PROGRESS') OR (OLD.state='RESOLVED' AND NEW.state IN ('RESOLVED','IN_PROGRESS','CLOSED'))) THEN RAISE EXCEPTION 'SUPPORT_INVALID_TRANSITION' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER support_ticket_guard BEFORE UPDATE ON orders.personal_support_ticket FOR EACH ROW EXECUTE FUNCTION orders.guard_support_ticket();
CREATE FUNCTION orders.require_support_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM orders.personal_support_message WHERE ticket_id=NEW.id AND owner_id=NEW.owner_id AND ticket_version=NEW.version)
 OR NOT EXISTS(SELECT 1 FROM orders.support_business_audit WHERE ticket_id=NEW.id AND owner_id=NEW.owner_id AND new_version=NEW.version)
 OR NOT EXISTS(SELECT 1 FROM orders.support_command_receipt WHERE ticket_id=NEW.id AND owner_id=NEW.owner_id AND result_version=NEW.version) THEN RAISE EXCEPTION 'SUPPORT_COMPLETE_TRANSACTION_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF; RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER support_complete_evidence AFTER INSERT OR UPDATE ON orders.personal_support_ticket DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.require_support_evidence();
REVOKE ALL ON orders.personal_support_ticket,orders.personal_support_message,orders.support_staff_grant,orders.personal_support_assignment,orders.support_command_receipt,orders.support_business_audit,orders.support_request_window FROM PUBLIC;
REVOKE ALL ON FUNCTION orders.configure_local_support_staff(UUID,TEXT,TEXT) FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION orders.guard_support_authority(),orders.support_append_only(),orders.guard_support_ticket(),orders.require_support_evidence() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.guard_support_authority(),orders.support_append_only(),orders.guard_support_ticket(),orders.require_support_evidence() TO "${runtimeRole}";
GRANT SELECT ON orders.support_staff_grant TO "${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.personal_support_ticket,orders.personal_support_assignment,orders.support_request_window TO "${runtimeRole}";
GRANT SELECT,INSERT ON orders.personal_support_message,orders.support_command_receipt,orders.support_business_audit TO "${runtimeRole}";
