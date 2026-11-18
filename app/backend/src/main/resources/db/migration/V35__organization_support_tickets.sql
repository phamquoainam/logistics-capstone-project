-- OSC manifest 9484c415; source/test authorization only. V34 is already deployed.
-- No grants are seeded. Team role ceilings and historical migrations are unchanged.
ALTER TABLE orders.support_staff_grant ADD CONSTRAINT organization_support_canonical_grant UNIQUE(id,account_id,permission);

CREATE TABLE orders.organization_support_ticket (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), facility_id UUID,
 creator_account_id UUID NOT NULL, code VARCHAR(39) NOT NULL UNIQUE CHECK(code='AP-OST-'||replace(id::TEXT,'-','')),
 kind VARCHAR(8) NOT NULL CHECK(kind IN ('GENERAL','SHIPMENT')), shipment_id UUID,
 category VARCHAR(24) NOT NULL CHECK(category IN ('ACCOUNT_HELP','SHIPMENT_TRACE','DAMAGE_REPORT','LOSS_REPORT','FEE_REVIEW','OTHER')),
 subject VARCHAR(160) NOT NULL CHECK(length(btrim(subject))>0 AND subject !~ '[[:cntrl:]]'),
 state VARCHAR(20) NOT NULL CHECK(state IN ('NEW','IN_PROGRESS','NEEDS_INFORMATION','RESOLVED','CLOSED')),
 version BIGINT NOT NULL CHECK(version>0), created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=created_at),
 requested_priority VARCHAR(6) NOT NULL CHECK(requested_priority IN ('LOW','NORMAL','HIGH')),
 resolution_version BIGINT, review_pending BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE(id,organization_id),
 FOREIGN KEY(organization_id,creator_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 FOREIGN KEY(shipment_id,organization_id) REFERENCES orders.pilot_shipment(id,organization_id),
 FOREIGN KEY(shipment_id,organization_id,facility_id) REFERENCES orders.pilot_shipment(id,organization_id,facility_id),
 CHECK((kind='GENERAL' AND shipment_id IS NULL) OR (kind='SHIPMENT' AND shipment_id IS NOT NULL)),
 CHECK(category<>'SHIPMENT_TRACE' OR kind='SHIPMENT'),
 CHECK((state IN ('RESOLVED','CLOSED'))=(resolution_version IS NOT NULL)),
 CHECK(resolution_version IS NULL OR resolution_version BETWEEN 1 AND version),
 CHECK(NOT review_pending OR state='RESOLVED')
);
CREATE INDEX organization_support_page ON orders.organization_support_ticket(organization_id,updated_at DESC,id);
CREATE TABLE orders.organization_support_message (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, ticket_id UUID NOT NULL, ticket_version BIGINT NOT NULL CHECK(ticket_version>0),
 actor_account_id UUID NOT NULL, author_kind VARCHAR(8) NOT NULL CHECK(author_kind IN ('MERCHANT','SUPPORT')),
 event_type VARCHAR(24) NOT NULL CHECK(event_type IN ('CREATE','REPLY','STAFF_ASSIGNED','START','STAFF_REPLY','REQUEST_INFORMATION','RESOLVE','REQUEST_REVIEW','REOPEN','ACKNOWLEDGE')),
 message VARCHAR(4000) NOT NULL CHECK(length(btrim(message))>0 AND regexp_replace(message,E'[\n\t]','','g') !~ '[[:cntrl:]]'),
 created_at TIMESTAMPTZ NOT NULL, UNIQUE(ticket_id,ticket_version), UNIQUE(id,organization_id,ticket_id),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id)
);
CREATE TABLE orders.organization_support_assignment (
 ticket_id UUID PRIMARY KEY, organization_id UUID NOT NULL, assignee_account_id UUID,
 version BIGINT NOT NULL CHECK(version>0), updated_at TIMESTAMPTZ NOT NULL,
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id),
 FOREIGN KEY(organization_id,assignee_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE TABLE orders.organization_support_assignment_event (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, ticket_id UUID NOT NULL,
 operation VARCHAR(12) NOT NULL CHECK(operation IN ('ASSIGNED','REASSIGNED','UNASSIGNED')),actor_account_id UUID NOT NULL,
 prior_assignee_account_id UUID, assignee_account_id UUID, prior_version BIGINT NOT NULL CHECK(prior_version>=0),
 new_version BIGINT NOT NULL CHECK(new_version=prior_version+1),occurred_at TIMESTAMPTZ NOT NULL,
 CHECK(prior_assignee_account_id IS DISTINCT FROM assignee_account_id),
 CHECK(operation=CASE WHEN prior_assignee_account_id IS NULL THEN 'ASSIGNED' WHEN assignee_account_id IS NULL THEN 'UNASSIGNED' ELSE 'REASSIGNED' END),
 UNIQUE(ticket_id,new_version),UNIQUE(id,organization_id,ticket_id),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id),
 FOREIGN KEY(organization_id,assignee_account_id) REFERENCES merchant.membership(organization_id,account_id),
 FOREIGN KEY(organization_id,prior_assignee_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE TABLE orders.organization_support_case_assignment (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL, ticket_id UUID NOT NULL,assignee_account_id UUID NOT NULL,
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')),version BIGINT NOT NULL CHECK(version>0),
 assigned_by UUID NOT NULL,assigned_at TIMESTAMPTZ NOT NULL,revoked_at TIMESTAMPTZ,
 CHECK((state='REVOKED')=(revoked_at IS NOT NULL)),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id)
);
CREATE UNIQUE INDEX organization_support_current_handler ON orders.organization_support_case_assignment(ticket_id) WHERE state='ACTIVE';
CREATE TABLE orders.organization_support_staff_scope (
 organization_id UUID NOT NULL REFERENCES merchant.organization(id),grant_id UUID NOT NULL,account_id UUID NOT NULL,
 permission VARCHAR(20) NOT NULL CHECK(permission IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')),
 state VARCHAR(8) NOT NULL CHECK(state IN ('ACTIVE','REVOKED')),version BIGINT NOT NULL CHECK(version>0),
 effective_from TIMESTAMPTZ NOT NULL,updated_at TIMESTAMPTZ NOT NULL CHECK(updated_at>=effective_from),
 PRIMARY KEY(organization_id,account_id,permission),FOREIGN KEY(grant_id,account_id,permission) REFERENCES orders.support_staff_grant(id,account_id,permission)
);
CREATE TABLE orders.organization_support_notification_preference (
 organization_id UUID NOT NULL,account_id UUID NOT NULL,enabled BOOLEAN NOT NULL,event_types TEXT[] NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),updated_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,account_id),FOREIGN KEY(organization_id,account_id) REFERENCES merchant.membership(organization_id,account_id),
 CHECK(cardinality(event_types)<=5 AND array_position(event_types,NULL) IS NULL AND event_types<@ARRAY['ASSIGNMENT','REPLY','NEEDS_INFORMATION','RESOLUTION','REVIEW']::TEXT[])
);
CREATE TABLE orders.organization_support_notice (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL,ticket_id UUID NOT NULL,event_id UUID NOT NULL,
 message_event_id UUID,assignment_event_id UUID,recipient_account_id UUID NOT NULL,
 type VARCHAR(20) NOT NULL CHECK(type IN ('ASSIGNMENT','REPLY','NEEDS_INFORMATION','RESOLUTION','REVIEW')),
 created_at TIMESTAMPTZ NOT NULL,read_at TIMESTAMPTZ,version BIGINT NOT NULL CHECK(version IN (1,2)),
 CHECK((version=2)=(read_at IS NOT NULL)),CHECK(read_at IS NULL OR read_at>=created_at),
 CHECK((message_event_id IS NOT NULL AND assignment_event_id IS NULL AND event_id=message_event_id) OR (assignment_event_id IS NOT NULL AND message_event_id IS NULL AND event_id=assignment_event_id)),
 UNIQUE(event_id,recipient_account_id),UNIQUE(id,organization_id,recipient_account_id),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id),
 FOREIGN KEY(message_event_id,organization_id,ticket_id) REFERENCES orders.organization_support_message(id,organization_id,ticket_id),
 FOREIGN KEY(assignment_event_id,organization_id,ticket_id) REFERENCES orders.organization_support_assignment_event(id,organization_id,ticket_id),
 FOREIGN KEY(organization_id,recipient_account_id) REFERENCES merchant.membership(organization_id,account_id)
);
CREATE TABLE orders.organization_support_receipt (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL,actor_account_id UUID NOT NULL,
 operation VARCHAR(24) NOT NULL CHECK(operation IN ('CREATE','REPLY','REQUEST_REVIEW','ACKNOWLEDGE','ASSIGN','PREFERENCES','READ_NOTICE','CARRIER_ASSIGN','CARRIER_REPLY','CARRIER_TRANSITION')),
 operation_key UUID NOT NULL,target_kind VARCHAR(12) NOT NULL CHECK(target_kind IN ('TICKET','PREFERENCES','NOTICE')),
 ticket_id UUID,preference_account_id UUID,notice_id UUID,target_id UUID NOT NULL,
 original_version BIGINT NOT NULL CHECK(original_version>=0),result_version BIGINT NOT NULL CHECK(result_version=original_version+1),
 fingerprint_key_id VARCHAR(64) NOT NULL,fingerprint BYTEA NOT NULL CHECK(octet_length(fingerprint)=32),committed_at TIMESTAMPTZ NOT NULL,
 UNIQUE(organization_id,actor_account_id,operation,operation_key),UNIQUE(id,organization_id,actor_account_id,operation,target_id,original_version,result_version),
 CHECK((target_kind='TICKET' AND ticket_id=target_id AND ticket_id IS NOT NULL AND preference_account_id IS NULL AND notice_id IS NULL AND operation NOT IN ('PREFERENCES','READ_NOTICE'))
 OR (target_kind='PREFERENCES' AND preference_account_id=target_id AND target_id=actor_account_id AND preference_account_id IS NOT NULL AND ticket_id IS NULL AND notice_id IS NULL AND operation='PREFERENCES')
 OR (target_kind='NOTICE' AND notice_id=target_id AND notice_id IS NOT NULL AND ticket_id IS NULL AND preference_account_id IS NULL AND operation='READ_NOTICE')),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id),
 FOREIGN KEY(organization_id,preference_account_id) REFERENCES orders.organization_support_notification_preference(organization_id,account_id),
 FOREIGN KEY(notice_id,organization_id,actor_account_id) REFERENCES orders.organization_support_notice(id,organization_id,recipient_account_id)
);
CREATE TABLE orders.organization_support_audit (
 id UUID PRIMARY KEY,organization_id UUID NOT NULL,ticket_id UUID,target_kind VARCHAR(12) NOT NULL,target_id UUID NOT NULL,
 preference_account_id UUID,notice_id UUID,actor_account_id UUID NOT NULL,operation VARCHAR(24) NOT NULL,
 request_id UUID NOT NULL,receipt_id UUID NOT NULL UNIQUE,prior_version BIGINT NOT NULL CHECK(prior_version>=0),
 new_version BIGINT NOT NULL CHECK(new_version=prior_version+1),occurred_at TIMESTAMPTZ NOT NULL,
 CHECK((target_kind='TICKET' AND ticket_id IS NOT NULL AND ticket_id=target_id AND preference_account_id IS NULL AND notice_id IS NULL)
 OR (target_kind='PREFERENCES' AND preference_account_id IS NOT NULL AND preference_account_id=target_id AND target_id=actor_account_id AND ticket_id IS NULL AND notice_id IS NULL)
 OR (target_kind='NOTICE' AND notice_id IS NOT NULL AND notice_id=target_id AND ticket_id IS NULL AND preference_account_id IS NULL)),
 FOREIGN KEY(receipt_id,organization_id,actor_account_id,operation,target_id,prior_version,new_version) REFERENCES orders.organization_support_receipt(id,organization_id,actor_account_id,operation,target_id,original_version,result_version),
 FOREIGN KEY(ticket_id,organization_id) REFERENCES orders.organization_support_ticket(id,organization_id),
 FOREIGN KEY(organization_id,preference_account_id) REFERENCES orders.organization_support_notification_preference(organization_id,account_id),
 FOREIGN KEY(notice_id,organization_id,actor_account_id) REFERENCES orders.organization_support_notice(id,organization_id,recipient_account_id)
);
-- Staff receipt route has no Organization selector: its actor/purpose/key must
-- identify exactly one bound Organization, never an arbitrary first matching row.
CREATE UNIQUE INDEX organization_support_staff_receipt_key ON orders.organization_support_receipt(actor_account_id,operation,operation_key)
 WHERE operation IN ('CARRIER_ASSIGN','CARRIER_REPLY','CARRIER_TRANSITION');
-- Ticket lifecycle and Merchant assignment have independent version streams.
-- Each actual version mutation has one receipt, including preferences/notice read.
CREATE UNIQUE INDEX organization_support_receipt_mutation ON orders.organization_support_receipt
 (organization_id,target_kind,target_id,(operation='ASSIGN'),result_version);
ALTER TABLE orders.organization_support_audit ADD CONSTRAINT organization_support_audit_variant_operation CHECK(
 (target_kind='TICKET' AND operation IN ('CREATE','REPLY','REQUEST_REVIEW','ACKNOWLEDGE','ASSIGN','CARRIER_ASSIGN','CARRIER_REPLY','CARRIER_TRANSITION'))
 OR (target_kind='PREFERENCES' AND operation='PREFERENCES') OR (target_kind='NOTICE' AND operation='READ_NOTICE'));
CREATE TABLE orders.organization_support_window (
 organization_id UUID NOT NULL,actor_account_id UUID NOT NULL,window_start TIMESTAMPTZ NOT NULL,
 kind VARCHAR(12) NOT NULL CHECK(kind IN ('CREATE','CONVERSATION')),count INTEGER NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,window_start,kind),
 FOREIGN KEY(organization_id,actor_account_id) REFERENCES merchant.membership(organization_id,account_id),
 CHECK(count BETWEEN 1 AND CASE kind WHEN 'CREATE' THEN 5 ELSE 20 END)
);
CREATE TABLE orders.support_operator_audit (
 id UUID PRIMARY KEY,request_id UUID NOT NULL UNIQUE,organization_id UUID NOT NULL REFERENCES merchant.organization(id),
 target_account_id UUID NOT NULL,permission VARCHAR(20) NOT NULL CHECK(permission IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')),
 action VARCHAR(6) NOT NULL CHECK(action IN ('BIND','UNBIND')),reason VARCHAR(300) NOT NULL CHECK(length(btrim(reason))>0 AND reason !~ '[[:cntrl:]]'),
 operator_principal TEXT NOT NULL,occurred_at TIMESTAMPTZ NOT NULL,request_digest BYTEA NOT NULL CHECK(octet_length(request_digest)=32),
 prior_grant_id UUID,target_grant_id UUID NOT NULL,prior_version BIGINT NOT NULL CHECK(prior_version>=0),result_version BIGINT NOT NULL CHECK(result_version=prior_version+1),
 FOREIGN KEY(prior_grant_id,target_account_id,permission) REFERENCES orders.support_staff_grant(id,account_id,permission),
 FOREIGN KEY(target_grant_id,target_account_id,permission) REFERENCES orders.support_staff_grant(id,account_id,permission)
);

-- Typed evidence is append-only, including sources of notices. No unchecked UUID target/event.
CREATE TRIGGER organization_support_message_append BEFORE UPDATE OR DELETE ON orders.organization_support_message FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER organization_support_assignment_event_append BEFORE UPDATE OR DELETE ON orders.organization_support_assignment_event FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER organization_support_receipt_append BEFORE UPDATE OR DELETE ON orders.organization_support_receipt FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER organization_support_audit_append BEFORE UPDATE OR DELETE ON orders.organization_support_audit FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();
CREATE TRIGGER organization_support_operator_append BEFORE UPDATE OR DELETE ON orders.support_operator_audit FOR EACH ROW EXECUTE FUNCTION orders.support_append_only();

CREATE FUNCTION orders.organization_support_staff(org UUID,actor UUID,required TEXT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM orders.organization_support_staff_scope s
 JOIN orders.support_staff_grant g ON (g.id,g.account_id,g.permission)=(s.grant_id,s.account_id,s.permission)
 JOIN merchant.organization o ON o.id=s.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id
 WHERE s.organization_id=org AND s.account_id=actor AND s.permission=required AND s.state='ACTIVE' AND g.state='ACTIVE'
 AND s.effective_from<=CURRENT_TIMESTAMP AND g.effective_from<=CURRENT_TIMESTAMP
 AND o.state='ACTIVE' AND o.environment IN ('LOCAL','TEST') AND owner.state='ACTIVE' AND owner.role='OWNER')
$$;
CREATE FUNCTION orders.organization_support_permission(org UUID,actor UUID,site UUID,required TEXT) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT merchant.team_permission(org,actor,'support.read',site) AND merchant.team_permission(org,actor,required,site)
$$;
CREATE FUNCTION merchant.check_organization_support_permissions() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID;
BEGIN
 IF TG_OP='DELETE' THEN org:=OLD.organization_id;ELSE org:=NEW.organization_id;END IF;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.organization_id=org AND g.permission IN ('support.write','support.assign','support.reports.read','support.notifications.manage')
 AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=g.membership_id AND r.permission='support.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id)))
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_READ_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_permission_dependencies AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_organization_support_permissions();

CREATE FUNCTION orders.guard_organization_support_ticket() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);PERFORM pg_advisory_xact_lock(730022,1);PERFORM pg_advisory_xact_lock(730035,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_DELETE_DENIED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'NEW' OR NEW.version<>1 OR NEW.created_at<>NEW.updated_at OR NEW.review_pending OR NEW.resolution_version IS NOT NULL
   OR NOT orders.organization_support_permission(NEW.organization_id,NEW.creator_account_id,NEW.facility_id,'support.write')
   OR (NEW.facility_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility WHERE id=NEW.facility_id AND organization_id=NEW.organization_id AND state='ACTIVE'))
   OR (NEW.shipment_id IS NOT NULL AND (NOT merchant.team_permission(NEW.organization_id,NEW.creator_account_id,'shipment.draft.read',NEW.facility_id)
    OR NOT EXISTS(SELECT 1 FROM orders.pilot_shipment WHERE id=NEW.shipment_id AND organization_id=NEW.organization_id AND facility_id IS NOT DISTINCT FROM NEW.facility_id AND owner_type='BUSINESS')))
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SOURCE_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF (NEW.id,NEW.organization_id,NEW.creator_account_id,NEW.code,NEW.kind,NEW.category,NEW.subject,NEW.created_at,NEW.requested_priority)
    IS DISTINCT FROM (OLD.id,OLD.organization_id,OLD.creator_account_id,OLD.code,OLD.kind,OLD.category,OLD.subject,OLD.created_at,OLD.requested_priority)
    OR NEW.shipment_id IS DISTINCT FROM OLD.shipment_id OR NEW.facility_id IS DISTINCT FROM OLD.facility_id OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_IMMUTABLE_BINDING' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_support_ticket_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_ticket FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_ticket();

CREATE FUNCTION orders.check_organization_support_command() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE t orders.organization_support_ticket%ROWTYPE;r orders.organization_support_receipt%ROWTYPE;m orders.organization_support_message%ROWTYPE;site UUID;required TEXT;
BEGIN
 IF TG_TABLE_NAME='organization_support_receipt' THEN r:=NEW;
 ELSE SELECT * INTO r FROM orders.organization_support_receipt WHERE id=NEW.receipt_id;END IF;
 IF NOT EXISTS(SELECT 1 FROM orders.organization_support_audit a WHERE a.receipt_id=r.id AND a.xmin::TEXT=pg_current_xact_id()::TEXT)
  OR NOT EXISTS(SELECT 1 FROM orders.organization_support_receipt x WHERE x.id=r.id AND x.xmin::TEXT=pg_current_xact_id()::TEXT)
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ATOMIC_AUDIT_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 IF r.target_kind='TICKET' THEN
  SELECT * INTO t FROM orders.organization_support_ticket WHERE id=r.ticket_id AND organization_id=r.organization_id;site:=t.facility_id;
  IF r.operation LIKE 'CARRIER_%' THEN
   required:=CASE r.operation WHEN 'CARRIER_ASSIGN' THEN 'SUPPORT_TRIAGE' ELSE 'SUPPORT_HANDLE' END;
   IF NOT orders.organization_support_staff(r.organization_id,r.actor_account_id,required)
    OR (r.operation<>'CARRIER_ASSIGN' AND NOT EXISTS(SELECT 1 FROM orders.organization_support_case_assignment WHERE ticket_id=t.id AND assignee_account_id=r.actor_account_id AND state='ACTIVE'))
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CARRIER_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
  ELSE
   required:=CASE r.operation WHEN 'ASSIGN' THEN 'support.assign' ELSE 'support.write' END;
   IF NOT orders.organization_support_permission(r.organization_id,r.actor_account_id,site,required)
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_MERCHANT_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
  END IF;
  IF r.operation='ASSIGN' THEN
   IF NOT EXISTS(SELECT 1 FROM orders.organization_support_assignment a WHERE a.ticket_id=t.id AND a.organization_id=r.organization_id AND a.version>=r.result_version AND a.xmin::TEXT=pg_current_xact_id()::TEXT)
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_WRITE_REQUIRED' USING ERRCODE='23514';END IF;
   IF NOT EXISTS(SELECT 1 FROM orders.organization_support_assignment_event e WHERE e.ticket_id=t.id AND e.new_version=r.result_version
     AND e.prior_version=r.original_version AND e.actor_account_id=r.actor_account_id AND e.xmin::TEXT=pg_current_xact_id()::TEXT)
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
  ELSE
   IF t.version<r.result_version OR NOT EXISTS(SELECT 1 FROM orders.organization_support_ticket current_ticket WHERE current_ticket.id=t.id AND current_ticket.xmin::TEXT=pg_current_xact_id()::TEXT)
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_TICKET_WRITE_REQUIRED' USING ERRCODE='23514';END IF;
   SELECT * INTO m FROM orders.organization_support_message WHERE ticket_id=t.id AND ticket_version=r.result_version AND xmin::TEXT=pg_current_xact_id()::TEXT;
   IF m.id IS NULL OR m.actor_account_id<>r.actor_account_id OR m.organization_id<>r.organization_id OR m.author_kind<>(CASE WHEN r.operation LIKE 'CARRIER_%' THEN 'SUPPORT' ELSE 'MERCHANT' END)
    OR m.event_type<>(CASE r.operation WHEN 'CARRIER_ASSIGN' THEN 'STAFF_ASSIGNED' WHEN 'CARRIER_REPLY' THEN 'STAFF_REPLY' WHEN 'CARRIER_TRANSITION' THEN m.event_type ELSE r.operation END)
    OR (r.operation='CARRIER_TRANSITION' AND m.event_type NOT IN ('START','REQUEST_INFORMATION','RESOLVE','REOPEN'))
   THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_PUBLIC_EVENT_REQUIRED' USING ERRCODE='23514';END IF;
  END IF;
 ELSE
  IF (r.target_kind='PREFERENCES' AND NOT EXISTS(SELECT 1 FROM orders.organization_support_notification_preference p WHERE p.organization_id=r.organization_id AND p.account_id=r.actor_account_id AND p.version>=r.result_version AND p.xmin::TEXT=pg_current_xact_id()::TEXT))
   OR (r.target_kind='NOTICE' AND NOT EXISTS(SELECT 1 FROM orders.organization_support_notice n WHERE n.id=r.notice_id AND n.version=r.result_version AND n.xmin::TEXT=pg_current_xact_id()::TEXT))
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_TYPED_TARGET_WRITE_REQUIRED' USING ERRCODE='23514';END IF;
  IF r.target_kind='NOTICE' THEN SELECT t0.* INTO t FROM orders.organization_support_notice n JOIN orders.organization_support_ticket t0 ON t0.id=n.ticket_id WHERE n.id=r.notice_id;site:=t.facility_id;
  END IF;
  IF NOT orders.organization_support_permission(r.organization_id,r.actor_account_id,site,CASE r.target_kind WHEN 'PREFERENCES' THEN 'support.notifications.manage' ELSE 'support.read' END)
    AND NOT (r.target_kind='PREFERENCES' AND EXISTS(SELECT 1 FROM merchant.member_grant g JOIN merchant.membership mb ON mb.id=g.membership_id WHERE mb.organization_id=r.organization_id AND mb.account_id=r.actor_account_id AND g.permission='support.notifications.manage' AND orders.organization_support_permission(r.organization_id,r.actor_account_id,g.facility_id,'support.notifications.manage')))
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SELF_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_receipt_atomic AFTER INSERT ON orders.organization_support_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_command();
CREATE CONSTRAINT TRIGGER organization_support_audit_atomic AFTER INSERT ON orders.organization_support_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_command();

CREATE FUNCTION orders.check_organization_support_ticket_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE m orders.organization_support_message%ROWTYPE;r orders.organization_support_receipt%ROWTYPE;prior_state TEXT;prior_review BOOLEAN;
BEGIN
 SELECT * INTO m FROM orders.organization_support_message WHERE ticket_id=NEW.id AND ticket_version=NEW.version AND xmin::TEXT=pg_current_xact_id()::TEXT;
 SELECT * INTO r FROM orders.organization_support_receipt WHERE ticket_id=NEW.id AND result_version=NEW.version AND operation<>'ASSIGN' AND xmin::TEXT=pg_current_xact_id()::TEXT;
 IF m.id IS NULL OR r.id IS NULL OR r.original_version<>NEW.version-1 OR m.created_at<>NEW.updated_at
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_COMPLETE_TRANSACTION_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN
  IF m.event_type<>'CREATE' OR m.actor_account_id<>NEW.creator_account_id THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CREATE_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT (
   (m.event_type='REPLY' AND OLD.state IN ('NEW','IN_PROGRESS','NEEDS_INFORMATION') AND NEW.state=CASE OLD.state WHEN 'NEEDS_INFORMATION' THEN 'IN_PROGRESS' ELSE OLD.state END AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='STAFF_REPLY' AND OLD.state='IN_PROGRESS' AND NEW.state=OLD.state AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='STAFF_ASSIGNED' AND OLD.state='NEW' AND NEW.state='NEW' AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='START' AND OLD.state='NEW' AND NEW.state='IN_PROGRESS' AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='REQUEST_INFORMATION' AND OLD.state='IN_PROGRESS' AND NEW.state='NEEDS_INFORMATION' AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='RESOLVE' AND OLD.state='IN_PROGRESS' AND NEW.state='RESOLVED' AND NOT NEW.review_pending AND NEW.resolution_version=NEW.version)
   OR (m.event_type='REQUEST_REVIEW' AND OLD.state='RESOLVED' AND NOT OLD.review_pending AND NEW.state=OLD.state AND NEW.review_pending AND NEW.resolution_version=OLD.resolution_version)
   OR (m.event_type='REOPEN' AND OLD.state='RESOLVED' AND OLD.review_pending AND NEW.state='IN_PROGRESS' AND NOT NEW.review_pending AND NEW.resolution_version IS NULL)
   OR (m.event_type='ACKNOWLEDGE' AND OLD.state='RESOLVED' AND NOT OLD.review_pending AND NEW.state='CLOSED' AND NOT NEW.review_pending AND NEW.resolution_version=OLD.resolution_version))
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_STATE_EVENT_CONFLICT' USING ERRCODE='23514';END IF;
 END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_ticket_atomic AFTER INSERT OR UPDATE ON orders.organization_support_ticket DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_ticket_evidence();

CREATE FUNCTION orders.guard_organization_support_version() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730035,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_DELETE_DENIED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN IF NEW.version<>1 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_INITIAL_VERSION' USING ERRCODE='23514';END IF;
 ELSIF NEW.version<>OLD.version+1 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_VERSION_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='UPDATE' THEN
  IF TG_TABLE_NAME='organization_support_assignment' THEN
   IF (NEW.ticket_id,NEW.organization_id) IS DISTINCT FROM (OLD.ticket_id,OLD.organization_id) OR NEW.assignee_account_id IS NOT DISTINCT FROM OLD.assignee_account_id OR NEW.updated_at<OLD.updated_at THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_BINDING' USING ERRCODE='23514';END IF;
  ELSIF TG_TABLE_NAME='organization_support_notification_preference' THEN
   IF (NEW.organization_id,NEW.account_id) IS DISTINCT FROM (OLD.organization_id,OLD.account_id) OR NEW.updated_at<OLD.updated_at THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_PREFERENCE_BINDING' USING ERRCODE='23514';END IF;
  ELSIF TG_TABLE_NAME='organization_support_notice' THEN
   IF (NEW.id,NEW.organization_id,NEW.ticket_id,NEW.event_id,NEW.recipient_account_id,NEW.type,NEW.created_at) IS DISTINCT FROM (OLD.id,OLD.organization_id,OLD.ticket_id,OLD.event_id,OLD.recipient_account_id,OLD.type,OLD.created_at)
    OR NEW.message_event_id IS DISTINCT FROM OLD.message_event_id OR NEW.assignment_event_id IS DISTINCT FROM OLD.assignment_event_id OR OLD.version<>1 OR NEW.version<>2 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_NOTICE_BINDING' USING ERRCODE='23514';END IF;
  END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_support_assignment_version BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_assignment FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_version();
CREATE TRIGGER organization_support_preferences_version BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_notification_preference FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_version();
CREATE TRIGGER organization_support_notice_version BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_notice FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_version();

CREATE FUNCTION orders.check_organization_support_auxiliary() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE t orders.organization_support_ticket%ROWTYPE;op TEXT;target UUID;org UUID;actor UUID;prior BIGINT;e orders.organization_support_assignment_event%ROWTYPE;
BEGIN
 org:=NEW.organization_id;
 IF TG_TABLE_NAME='organization_support_assignment' THEN
  target:=NEW.ticket_id;op:='ASSIGN';SELECT * INTO t FROM orders.organization_support_ticket WHERE id=target;
  SELECT * INTO e FROM orders.organization_support_assignment_event WHERE ticket_id=target AND new_version=NEW.version AND xmin::TEXT=pg_current_xact_id()::TEXT;
  IF e.id IS NULL OR e.assignee_account_id IS DISTINCT FROM NEW.assignee_account_id OR e.occurred_at<>NEW.updated_at
   OR (TG_OP='UPDATE' AND e.prior_assignee_account_id IS DISTINCT FROM OLD.assignee_account_id)
   OR (NEW.assignee_account_id IS NOT NULL AND NOT orders.organization_support_permission(org,NEW.assignee_account_id,t.facility_id,'support.write'))
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_CURRENT_AUTHORITY' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='organization_support_notification_preference' THEN
  target:=NEW.account_id;op:='PREFERENCES';actor:=NEW.account_id;
  IF cardinality(NEW.event_types)<>(SELECT count(DISTINCT v) FROM unnest(NEW.event_types) v) THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_EVENTS_DUPLICATED' USING ERRCODE='23514';END IF;
 ELSE
  IF TG_OP='INSERT' THEN RETURN NULL;END IF;
  target:=NEW.id;op:='READ_NOTICE';actor:=NEW.recipient_account_id;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM orders.organization_support_receipt r WHERE r.organization_id=org AND r.operation=op AND r.target_id=target AND r.result_version=NEW.version
  AND (actor IS NULL OR r.actor_account_id=actor) AND r.xmin::TEXT=pg_current_xact_id()::TEXT)
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_AUXILIARY_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_assignment_atomic AFTER INSERT OR UPDATE ON orders.organization_support_assignment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_auxiliary();
CREATE CONSTRAINT TRIGGER organization_support_preferences_atomic AFTER INSERT OR UPDATE ON orders.organization_support_notification_preference DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_auxiliary();
CREATE CONSTRAINT TRIGGER organization_support_notice_atomic AFTER INSERT OR UPDATE ON orders.organization_support_notice DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_auxiliary();

CREATE FUNCTION orders.guard_organization_support_notice() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE t orders.organization_support_ticket%ROWTYPE;source_type TEXT;
BEGIN
 SELECT * INTO t FROM orders.organization_support_ticket WHERE id=NEW.ticket_id AND organization_id=NEW.organization_id;
 IF NEW.assignment_event_id IS NOT NULL THEN
  SELECT 'ASSIGNMENT' INTO source_type FROM orders.organization_support_assignment_event WHERE id=NEW.assignment_event_id AND xmin::TEXT=pg_current_xact_id()::TEXT;
 ELSE
  SELECT CASE event_type WHEN 'REQUEST_INFORMATION' THEN 'NEEDS_INFORMATION' WHEN 'RESOLVE' THEN 'RESOLUTION' WHEN 'REQUEST_REVIEW' THEN 'REVIEW' WHEN 'REOPEN' THEN 'REVIEW' WHEN 'STAFF_ASSIGNED' THEN 'ASSIGNMENT' ELSE 'REPLY' END INTO source_type
   FROM orders.organization_support_message WHERE id=NEW.message_event_id AND event_type<>'CREATE' AND xmin::TEXT=pg_current_xact_id()::TEXT;
 END IF;
 IF NEW.type IS DISTINCT FROM source_type OR NEW.version<>1 OR NOT orders.organization_support_permission(NEW.organization_id,NEW.recipient_account_id,t.facility_id,'support.read')
 OR NOT EXISTS(SELECT 1 FROM orders.organization_support_notification_preference WHERE organization_id=NEW.organization_id AND account_id=NEW.recipient_account_id AND enabled AND NEW.type=ANY(event_types))
 OR (NEW.recipient_account_id<>t.creator_account_id AND NOT EXISTS(SELECT 1 FROM orders.organization_support_assignment WHERE ticket_id=t.id AND assignee_account_id=NEW.recipient_account_id))
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_NOTICE_SOURCE_RECIPIENT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_support_notice_source BEFORE INSERT ON orders.organization_support_notice FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_notice();

CREATE FUNCTION orders.guard_organization_support_case_assignment() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_DELETE_DENIED' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'ACTIVE' OR NEW.version<>1 OR NOT orders.organization_support_staff(NEW.organization_id,NEW.assignee_account_id,'SUPPORT_HANDLE')
   OR NOT orders.organization_support_staff(NEW.organization_id,NEW.assigned_by,'SUPPORT_TRIAGE')
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SCOPED_STAFF_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF (NEW.id,NEW.organization_id,NEW.ticket_id,NEW.assignee_account_id,NEW.assigned_by,NEW.assigned_at) IS DISTINCT FROM (OLD.id,OLD.organization_id,OLD.ticket_id,OLD.assignee_account_id,OLD.assigned_by,OLD.assigned_at)
   OR OLD.state<>'ACTIVE' OR NEW.state<>'REVOKED' OR NEW.version<>OLD.version+1 OR NEW.revoked_at<OLD.assigned_at
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CASE_BINDING_IMMUTABLE' USING ERRCODE='23514';END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_support_case_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_case_assignment FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_case_assignment();
CREATE FUNCTION orders.check_organization_support_event() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF TG_TABLE_NAME='organization_support_message' THEN
  IF NOT EXISTS(SELECT 1 FROM orders.organization_support_receipt r WHERE r.ticket_id=NEW.ticket_id AND r.organization_id=NEW.organization_id
   AND r.operation<>'ASSIGN' AND r.result_version=NEW.ticket_version AND r.actor_account_id=NEW.actor_account_id AND r.xmin::TEXT=pg_current_xact_id()::TEXT)
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_MESSAGE_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSIF TG_TABLE_NAME='organization_support_assignment_event' THEN
  IF NOT EXISTS(SELECT 1 FROM orders.organization_support_receipt r WHERE r.ticket_id=NEW.ticket_id AND r.organization_id=NEW.organization_id
   AND r.operation='ASSIGN' AND r.result_version=NEW.new_version AND r.original_version=NEW.prior_version AND r.actor_account_id=NEW.actor_account_id AND r.xmin::TEXT=pg_current_xact_id()::TEXT)
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ASSIGNMENT_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 ELSE
  IF NOT EXISTS(SELECT 1 FROM orders.organization_support_receipt r JOIN orders.organization_support_message m ON m.ticket_id=r.ticket_id AND m.ticket_version=r.result_version
   WHERE r.ticket_id=NEW.ticket_id AND r.operation='CARRIER_ASSIGN' AND m.event_type='STAFF_ASSIGNED' AND r.xmin::TEXT=pg_current_xact_id()::TEXT)
  THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CASE_RECEIPT_REQUIRED' USING ERRCODE='23514';END IF;
 END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_message_atomic AFTER INSERT ON orders.organization_support_message DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_event();
CREATE CONSTRAINT TRIGGER organization_support_internal_event_atomic AFTER INSERT ON orders.organization_support_assignment_event DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_event();
CREATE CONSTRAINT TRIGGER organization_support_case_atomic AFTER INSERT OR UPDATE ON orders.organization_support_case_assignment DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_event();

CREATE FUNCTION orders.guard_organization_support_scope() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='orders')) OR COALESCE(current_setting('argopost.operator_environment',TRUE),'') NOT IN ('LOCAL','TEST')
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OFFLINE_OPERATOR_REQUIRED' USING ERRCODE='42501';END IF;
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SCOPE_HISTORY_REQUIRED' USING ERRCODE='23514';END IF;
 IF TG_OP='UPDATE' AND ((NEW.organization_id,NEW.account_id,NEW.permission) IS DISTINCT FROM (OLD.organization_id,OLD.account_id,OLD.permission) OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at)
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SCOPE_BINDING_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' AND NEW.version<>1 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_SCOPE_INITIAL_VERSION' USING ERRCODE='23514';END IF;
 IF NEW.state='ACTIVE' AND NOT EXISTS(SELECT 1 FROM orders.support_staff_grant WHERE id=NEW.grant_id AND account_id=NEW.account_id AND permission=NEW.permission AND state='ACTIVE' AND effective_from<=CURRENT_TIMESTAMP)
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CANONICAL_GRANT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER organization_support_scope_guard BEFORE INSERT OR UPDATE OR DELETE ON orders.organization_support_staff_scope FOR EACH ROW EXECUTE FUNCTION orders.guard_organization_support_scope();
CREATE FUNCTION orders.check_organization_support_scope_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM orders.support_operator_audit a WHERE a.organization_id=NEW.organization_id AND a.target_account_id=NEW.account_id AND a.permission=NEW.permission
  AND a.target_grant_id=NEW.grant_id AND a.result_version=NEW.version AND a.action=CASE NEW.state WHEN 'ACTIVE' THEN 'BIND' ELSE 'UNBIND' END
  AND a.xmin::TEXT=pg_current_xact_id()::TEXT)
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER organization_support_scope_atomic AFTER INSERT OR UPDATE ON orders.organization_support_staff_scope DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION orders.check_organization_support_scope_evidence();
CREATE FUNCTION orders.configure_local_organization_support_staff(p_request UUID,p_action TEXT,p_org UUID,p_account UUID,p_permission TEXT,p_reason TEXT)
 RETURNS TABLE(request_id UUID,outcome TEXT,version BIGINT,organization_id UUID,account_id UUID,permission TEXT)
 LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,orders AS $$
DECLARE prior orders.support_operator_audit%ROWTYPE;s orders.organization_support_staff_scope%ROWTYPE;g UUID;input_digest BYTEA;stamp TIMESTAMPTZ:=CURRENT_TIMESTAMP;
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='orders')) OR COALESCE(current_setting('argopost.operator_environment',TRUE),'') NOT IN ('LOCAL','TEST')
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OFFLINE_OPERATOR_REQUIRED' USING ERRCODE='42501';END IF;
 IF p_request IS NULL OR p_org IS NULL OR p_account IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_account='00000000-0000-0000-0000-000000000000'
  OR p_action IS NULL OR p_action NOT IN ('BIND','UNBIND') OR p_permission IS NULL OR p_permission NOT IN ('SUPPORT_TRIAGE','SUPPORT_HANDLE')
  OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]'
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_INPUT_INVALID' USING ERRCODE='23514';END IF;
 PERFORM identity.lock_local_business_subject(p_account,p_action='BIND');
 PERFORM pg_advisory_xact_lock(730023,1);PERFORM pg_advisory_xact_lock(730022,1);PERFORM pg_advisory_xact_lock(730035,1);
 input_digest:=sha256(convert_to(jsonb_build_array(current_setting('argopost.operator_environment'),p_action,p_org,p_account,p_permission,p_reason)::TEXT,'UTF8'));
 SELECT * INTO prior FROM orders.support_operator_audit WHERE support_operator_audit.request_id=p_request;
 IF FOUND THEN
  IF prior.request_digest<>input_digest THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_KEY_CONFLICT' USING ERRCODE='23514';END IF;
  RETURN QUERY SELECT prior.request_id,CASE prior.action WHEN 'BIND' THEN 'BOUND' ELSE 'UNBOUND' END,prior.result_version,prior.organization_id,prior.target_account_id,prior.permission::TEXT;RETURN;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id WHERE o.id=p_org AND o.environment=current_setting('argopost.operator_environment') AND o.state='ACTIVE' AND m.state='ACTIVE' AND m.role='OWNER')
 THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_OPERATOR_ORGANIZATION_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO s FROM orders.organization_support_staff_scope WHERE organization_support_staff_scope.organization_id=p_org AND organization_support_staff_scope.account_id=p_account AND organization_support_staff_scope.permission=p_permission FOR UPDATE;
 IF p_action='BIND' THEN
  SELECT id INTO g FROM orders.support_staff_grant WHERE support_staff_grant.account_id=p_account AND support_staff_grant.permission=p_permission AND state='ACTIVE' AND effective_from<=stamp;
  IF g IS NULL THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_CANONICAL_GRANT_REQUIRED' USING ERRCODE='23514';END IF;
  IF s.state='ACTIVE' AND s.grant_id=g THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_BINDING_NO_CHANGE' USING ERRCODE='23514';END IF;
 ELSE
  IF s.state IS DISTINCT FROM 'ACTIVE' THEN RAISE EXCEPTION 'ORGANIZATION_SUPPORT_ACTIVE_BINDING_REQUIRED' USING ERRCODE='23514';END IF;g:=s.grant_id;
 END IF;
 IF s.organization_id IS NULL THEN
  INSERT INTO orders.organization_support_staff_scope VALUES(p_org,g,p_account,p_permission,'ACTIVE',1,stamp,stamp);
 ELSE
  UPDATE orders.organization_support_staff_scope SET grant_id=g,state=CASE p_action WHEN 'BIND' THEN 'ACTIVE' ELSE 'REVOKED' END,version=s.version+1,effective_from=stamp,updated_at=stamp
   WHERE organization_support_staff_scope.organization_id=p_org AND organization_support_staff_scope.account_id=p_account AND organization_support_staff_scope.permission=p_permission;
 END IF;
 INSERT INTO orders.support_operator_audit VALUES(gen_random_uuid(),p_request,p_org,p_account,p_permission,p_action,p_reason,session_user,stamp,input_digest,s.grant_id,g,COALESCE(s.version,0),COALESCE(s.version,0)+1);
 RETURN QUERY SELECT p_request,CASE p_action WHEN 'BIND' THEN 'BOUND' ELSE 'UNBOUND' END,COALESCE(s.version,0)+1,p_org,p_account,p_permission;
END $$;

REVOKE ALL ON orders.organization_support_ticket,orders.organization_support_message,orders.organization_support_assignment,orders.organization_support_assignment_event,orders.organization_support_case_assignment,orders.organization_support_staff_scope,orders.organization_support_receipt,orders.organization_support_audit,orders.organization_support_window,orders.organization_support_notification_preference,orders.organization_support_notice,orders.support_operator_audit FROM PUBLIC;
GRANT SELECT,INSERT ON orders.organization_support_message,orders.organization_support_assignment_event,orders.organization_support_receipt,orders.organization_support_audit TO "${runtimeRole}";
GRANT SELECT,INSERT,UPDATE ON orders.organization_support_ticket,orders.organization_support_assignment,orders.organization_support_case_assignment,orders.organization_support_window,orders.organization_support_notification_preference,orders.organization_support_notice TO "${runtimeRole}";
GRANT SELECT ON orders.organization_support_staff_scope TO "${runtimeRole}";
REVOKE ALL ON orders.support_operator_audit FROM "${runtimeRole}";
REVOKE ALL ON FUNCTION orders.configure_local_organization_support_staff(UUID,TEXT,UUID,UUID,TEXT,TEXT) FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION orders.organization_support_staff(UUID,UUID,TEXT),orders.organization_support_permission(UUID,UUID,UUID,TEXT),merchant.check_organization_support_permissions(),orders.guard_organization_support_ticket(),orders.check_organization_support_command(),orders.check_organization_support_ticket_evidence(),orders.guard_organization_support_version(),orders.check_organization_support_auxiliary(),orders.guard_organization_support_notice(),orders.guard_organization_support_case_assignment(),orders.check_organization_support_event(),orders.guard_organization_support_scope(),orders.check_organization_support_scope_evidence() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION orders.organization_support_staff(UUID,UUID,TEXT),orders.organization_support_permission(UUID,UUID,UUID,TEXT),merchant.check_organization_support_permissions(),orders.guard_organization_support_ticket(),orders.check_organization_support_command(),orders.check_organization_support_ticket_evidence(),orders.guard_organization_support_version(),orders.check_organization_support_auxiliary(),orders.guard_organization_support_notice(),orders.guard_organization_support_case_assignment(),orders.check_organization_support_event() TO "${runtimeRole}";

-- Owner-approved compatibility corrections: actual operator name and permission width.
ALTER TABLE merchant.member_grant ALTER COLUMN permission TYPE VARCHAR(32);
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check CHECK(permission=ANY(merchant.team_role_ceiling('OWNER')||ARRAY['team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read','support.read','support.write','support.assign','support.reports.read','support.notifications.manage']));
ALTER TABLE merchant.member_grant DROP CONSTRAINT preparation_scope_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT preparation_scope_check CHECK((scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL AND permission IN (
 'shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','facility.read','facility.manage','shipment.handover.read','shipment.handover.manage','support.read','support.write','support.assign','support.reports.read','support.notifications.manage')));

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
 OR cardinality(p_permissions)>37 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read','support.read','support.write','support.assign','support.reports.read','support.notifications.manage'))
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


CREATE OR REPLACE FUNCTION merchant.configure_local_facility_grants(p_request UUID,p_action TEXT,p_org UUID,p_account UUID,p_facility UUID,p_permissions TEXT[],p_reason TEXT) RETURNS UUID LANGUAGE plpgsql SECURITY INVOKER SET search_path=pg_catalog,merchant AS $$
DECLARE mode TEXT:=current_setting('argopost.operator_environment',TRUE); owner_account UUID; subject UUID; member UUID; digest TEXT; prior merchant.facility_grant_audit%ROWTYPE; permissions TEXT[]; stamp TIMESTAMPTZ:=clock_timestamp();
BEGIN
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) OR COALESCE(mode,'') NOT IN ('LOCAL','TEST') THEN RAISE EXCEPTION 'LOCAL_FACILITY_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 IF p_request IS NULL OR p_request='00000000-0000-0000-0000-000000000000' OR p_org IS NULL OR p_account IS NULL OR p_action IS NULL OR p_action NOT IN ('GRANT','REVOKE')
  OR p_permissions IS NULL OR cardinality(p_permissions) NOT BETWEEN 1 AND 13 OR array_position(p_permissions,NULL) IS NOT NULL
  OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('facility.read','facility.manage','defaults.manage','shipment.prepare','shipment.draft.read','shipment.handover.read','shipment.handover.manage','support.read','support.write','support.assign','support.reports.read','support.notifications.manage')) OR (p_facility IS NOT NULL AND 'defaults.manage'=ANY(p_permissions))
  OR p_reason IS NULL OR length(btrim(p_reason)) NOT BETWEEN 1 AND 300 OR p_reason ~ '[[:cntrl:]]' THEN RAISE EXCEPTION 'FACILITY_GRANT_COMMAND_INVALID' USING ERRCODE='23514'; END IF;
 SELECT m.account_id INTO owner_account FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id WHERE o.id=p_org;
 FOR subject IN SELECT DISTINCT v FROM unnest(ARRAY[p_account,owner_account]) v WHERE v IS NOT NULL ORDER BY v LOOP PERFORM identity.lock_local_business_subject(subject,p_action='GRANT');END LOOP;
 PERFORM pg_advisory_xact_lock(730023,1);
 SELECT m.id INTO member FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id
  WHERE m.organization_id=p_org AND m.account_id=p_account AND m.state='ACTIVE' AND o.state='ACTIVE' AND o.environment=mode AND owner.state='ACTIVE' AND owner.role='OWNER'
   AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=m.id AND g.permission='dashboard.read');
 IF member IS NULL OR (p_facility IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility WHERE id=p_facility AND organization_id=p_org AND state='ACTIVE')) THEN RAISE EXCEPTION 'FACILITY_GRANT_CONTEXT_REQUIRED' USING ERRCODE='23514'; END IF;
 permissions:=ARRAY(SELECT DISTINCT p FROM unnest(p_permissions) p ORDER BY p);digest:=encode(sha256(convert_to(jsonb_build_array(mode,p_action,p_org,p_account,p_facility,permissions,p_reason)::TEXT,'UTF8')),'hex');
 SELECT * INTO prior FROM merchant.facility_grant_audit WHERE request_id=p_request;
 IF FOUND THEN IF prior.input_digest<>digest THEN RAISE EXCEPTION 'FACILITY_GRANT_IDEMPOTENCY_CONFLICT' USING ERRCODE='23514'; END IF;RETURN member;END IF;
 IF p_action='GRANT' THEN INSERT INTO merchant.member_grant(id,membership_id,organization_id,permission,scope,facility_id) SELECT gen_random_uuid(),member,p_org,p,CASE WHEN p_facility IS NULL THEN 'ORGANIZATION' ELSE 'FACILITY' END,p_facility FROM unnest(permissions) p ON CONFLICT DO NOTHING;
 ELSE DELETE FROM merchant.member_grant WHERE membership_id=member AND permission=ANY(permissions) AND facility_id IS NOT DISTINCT FROM p_facility;END IF;
 IF EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=member AND g.permission='facility.manage' AND NOT EXISTS(SELECT 1 FROM merchant.member_grant r WHERE r.membership_id=member AND r.permission='facility.read' AND (r.scope='ORGANIZATION' OR r.facility_id=g.facility_id))) THEN RAISE EXCEPTION 'FACILITY_READ_DEPENDENCY_REQUIRED' USING ERRCODE='23514'; END IF;
 UPDATE merchant.membership SET version=version+1,authority_version=authority_version+1,updated_at=stamp WHERE id=member;
 INSERT INTO merchant.facility_grant_audit VALUES(p_request,digest,p_org,member,p_facility,p_action,permissions,session_user,p_reason,stamp);RETURN member;
END $$;
