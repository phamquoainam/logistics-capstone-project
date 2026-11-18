-- OTA V1, Owner technical GO f6e21ee3...2026-11-17. Test-only execution authorization.
-- V33 is not present: its unapproved Support proposal is not a migration dependency.
CREATE TABLE merchant.team_policy (
 organization_id UUID PRIMARY KEY REFERENCES merchant.organization(id),
 version BIGINT NOT NULL CHECK(version>0), template_version BIGINT NOT NULL CHECK(template_version=1),
 updated_at TIMESTAMPTZ NOT NULL
);
INSERT INTO merchant.team_policy SELECT id,1,1,updated_at FROM merchant.organization;

CREATE TABLE merchant.team_invitation (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.team_policy(organization_id),
 inviter_membership_id UUID NOT NULL, inviter_authority_version BIGINT NOT NULL CHECK(inviter_authority_version>0),
 policy_version BIGINT NOT NULL CHECK(policy_version>0),
 recipient_digest BYTEA NOT NULL CHECK(octet_length(recipient_digest)=32),
 protected_recipient BYTEA NOT NULL CHECK(octet_length(protected_recipient) BETWEEN 32 AND 2048),
 role VARCHAR(8) NOT NULL CHECK(role IN ('MANAGER','OPERATOR')),
 state VARCHAR(12) NOT NULL CHECK(state IN ('PENDING','ACCEPTED','DECLINED','REVOKED','EXPIRED','INVALIDATED')),
 proof_digest BYTEA NOT NULL UNIQUE CHECK(octet_length(proof_digest)=32), proof_version BIGINT NOT NULL CHECK(proof_version>0),
 expires_at TIMESTAMPTZ NOT NULL, accepted_membership_id UUID, version BIGINT NOT NULL CHECK(version>0),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 UNIQUE(id,organization_id),
 FOREIGN KEY(inviter_membership_id,organization_id) REFERENCES merchant.membership(id,organization_id),
 FOREIGN KEY(accepted_membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 CHECK(expires_at>created_at AND updated_at>=created_at),
 CHECK((state='ACCEPTED')=(accepted_membership_id IS NOT NULL))
);
CREATE UNIQUE INDEX team_live_recipient ON merchant.team_invitation(organization_id,recipient_digest) WHERE state='PENDING';
CREATE INDEX team_invitation_owner ON merchant.team_invitation(organization_id,inviter_membership_id,created_at,id);
CREATE TABLE merchant.team_invitation_grant (
 id UUID PRIMARY KEY, invitation_id UUID NOT NULL, organization_id UUID NOT NULL,
 permission VARCHAR(32) NOT NULL, scope VARCHAR(12) NOT NULL, facility_id UUID,
 FOREIGN KEY(invitation_id,organization_id) REFERENCES merchant.team_invitation(id,organization_id),
 FOREIGN KEY(facility_id,organization_id) REFERENCES merchant.facility(id,organization_id),
 UNIQUE NULLS NOT DISTINCT(invitation_id,permission,scope,facility_id),
 CHECK((scope='ORGANIZATION' AND facility_id IS NULL) OR (scope='FACILITY' AND facility_id IS NOT NULL))
);
CREATE TABLE merchant.team_command_receipt (
 organization_id UUID NOT NULL REFERENCES merchant.organization(id), actor_account_id UUID NOT NULL,
 operation VARCHAR(20) NOT NULL CHECK(operation IN ('INVITE','RESEND','REVOKE_INVITATION','ACCESS','SUSPEND','RESUME','REVOKE_MEMBER','ACCEPT','DECLINE')),
 key UUID NOT NULL, input_digest BYTEA NOT NULL CHECK(octet_length(input_digest)=32),
 membership_id UUID, invitation_id UUID,
 outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('COMMITTED','ACCEPTED','DECLINED')),
 version BIGINT NOT NULL CHECK(version>0), recorded_at TIMESTAMPTZ NOT NULL,
 PRIMARY KEY(organization_id,actor_account_id,operation,key),
 FOREIGN KEY(membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(invitation_id,organization_id) REFERENCES merchant.team_invitation(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 CHECK((operation IN ('ACCESS','SUSPEND','RESUME','REVOKE_MEMBER') AND membership_id IS NOT NULL AND invitation_id IS NULL)
   OR (operation IN ('INVITE','RESEND','REVOKE_INVITATION','ACCEPT','DECLINE') AND membership_id IS NULL AND invitation_id IS NOT NULL)),
 CHECK(outcome=CASE operation WHEN 'ACCEPT' THEN 'ACCEPTED' WHEN 'DECLINE' THEN 'DECLINED' ELSE 'COMMITTED' END)
);
CREATE TABLE merchant.team_audit (
 id UUID PRIMARY KEY, organization_id UUID NOT NULL REFERENCES merchant.organization(id), actor_account_id UUID NOT NULL,
 actor_membership_id UUID, target_membership_id UUID, invitation_id UUID,
 operation VARCHAR(20) NOT NULL, reason VARCHAR(300) NOT NULL CHECK(length(btrim(reason)) BETWEEN 1 AND 300 AND reason !~ '[[:cntrl:]]'),
 request_id UUID NOT NULL, occurred_at TIMESTAMPTZ NOT NULL,
 UNIQUE(organization_id,actor_account_id,operation,request_id),
 FOREIGN KEY(actor_membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(target_membership_id,organization_id) REFERENCES merchant.membership(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(invitation_id,organization_id) REFERENCES merchant.team_invitation(id,organization_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(organization_id,actor_account_id,operation,request_id)
   REFERENCES merchant.team_command_receipt(organization_id,actor_account_id,operation,key) DEFERRABLE INITIALLY DEFERRED,
 CHECK(actor_membership_id IS NOT NULL OR operation='DECLINE'),
 CHECK((operation IN ('ACCESS','SUSPEND','RESUME','REVOKE_MEMBER') AND target_membership_id IS NOT NULL AND invitation_id IS NULL)
    OR (operation IN ('INVITE','RESEND','REVOKE_INVITATION','ACCEPT','DECLINE') AND invitation_id IS NOT NULL))
);
ALTER TABLE merchant.membership ALTER COLUMN state TYPE VARCHAR(9);
ALTER TABLE merchant.membership DROP CONSTRAINT membership_state_check;
ALTER TABLE merchant.membership ADD CONSTRAINT membership_state_check CHECK(state IN ('ACTIVE','SUSPENDED','REVOKED'));

CREATE FUNCTION merchant.team_role_ceiling(role_name TEXT) RETURNS TEXT[] LANGUAGE sql IMMUTABLE AS $$
 SELECT CASE role_name WHEN 'OWNER' THEN ARRAY['dashboard.read','dashboard.finance.read','dashboard.staff.read',
 'shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read',
 'shipment.handover.read','shipment.handover.manage','facility.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign',
 'shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond']
 WHEN 'MANAGER' THEN ARRAY['dashboard.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage',
 'shipment.prepare','shipment.quote','shipment.book','shipment.label.read','shipment.handover.read','shipment.handover.manage','facility.read','facility.manage',
 'defaults.manage','shipment.export','shipment.assign','shipment.reports.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond']
 WHEN 'OPERATOR' THEN ARRAY['dashboard.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.prepare','shipment.quote','shipment.book',
 'shipment.label.read','shipment.handover.read','facility.read','shipment.export','shipment.reports.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.respond']
 ELSE ARRAY[]::TEXT[] END
$$;
ALTER TABLE merchant.member_grant DROP CONSTRAINT member_grant_permission_check;
ALTER TABLE merchant.member_grant ADD CONSTRAINT member_grant_permission_check
 CHECK(permission=ANY(merchant.team_role_ceiling('OWNER')||ARRAY['team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read']));
-- preparation_scope_check from V32 is intentionally unchanged: team.* cannot be FACILITY grants.

CREATE FUNCTION merchant.team_permission(org UUID,actor UUID,required TEXT,site UUID DEFAULT NULL) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM merchant.membership m JOIN merchant.organization o ON o.id=m.organization_id
 JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id AND owner.organization_id=o.id
 JOIN merchant.member_grant g ON g.membership_id=m.id AND g.organization_id=m.organization_id
 WHERE m.organization_id=org AND m.account_id=actor AND m.state='ACTIVE' AND o.state='ACTIVE'
 AND o.environment IN ('LOCAL','TEST') AND owner.state='ACTIVE' AND owner.role='OWNER'
 AND g.permission=required AND (g.scope='ORGANIZATION' OR (site IS NOT NULL AND g.facility_id=site)))
$$;
CREATE FUNCTION merchant.team_can_delegate(org UUID,actor UUID,target_role TEXT,required TEXT,site UUID) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT target_role IN ('MANAGER','OPERATOR') AND required=ANY(merchant.team_role_ceiling(target_role))
 AND merchant.team_permission(org,actor,required,site)
 AND EXISTS(SELECT 1 FROM merchant.membership m WHERE m.organization_id=org AND m.account_id=actor AND m.state='ACTIVE'
 AND (m.role='OWNER' OR (m.role='MANAGER' AND target_role='OPERATOR')))
$$;
CREATE FUNCTION merchant.team_append_only() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'TEAM_HISTORY_APPEND_ONLY' USING ERRCODE='23514'; END $$;
CREATE TRIGGER team_audit_append BEFORE UPDATE OR DELETE ON merchant.team_audit FOR EACH ROW EXECUTE FUNCTION merchant.team_append_only();
CREATE TRIGGER team_receipt_append BEFORE UPDATE OR DELETE ON merchant.team_command_receipt FOR EACH ROW EXECUTE FUNCTION merchant.team_append_only();
CREATE TRIGGER team_invitation_grant_append BEFORE UPDATE OR DELETE ON merchant.team_invitation_grant FOR EACH ROW EXECUTE FUNCTION merchant.team_append_only();

CREATE FUNCTION merchant.guard_team_invitation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'TEAM_HISTORY_APPEND_ONLY' USING ERRCODE='23514'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'PENDING' OR NEW.version<>1 OR NEW.proof_version<>1 OR NEW.created_at<>NEW.updated_at
    OR NEW.expires_at<>NEW.created_at+INTERVAL '72 hours' THEN RAISE EXCEPTION 'TEAM_INVITATION_INITIAL_STATE' USING ERRCODE='23514'; END IF;
 ELSE
  IF OLD.state<>'PENDING' OR NEW.id<>OLD.id OR NEW.organization_id<>OLD.organization_id OR NEW.inviter_membership_id<>OLD.inviter_membership_id
   OR NEW.recipient_digest<>OLD.recipient_digest OR NEW.protected_recipient<>OLD.protected_recipient OR NEW.role<>OLD.role
   OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at
   OR NEW.inviter_authority_version<>OLD.inviter_authority_version OR NEW.policy_version<>OLD.policy_version
  THEN RAISE EXCEPTION 'TEAM_INVITATION_BINDING_IMMUTABLE' USING ERRCODE='23514'; END IF;
  IF NEW.state='PENDING' THEN
   IF NEW.proof_version<>OLD.proof_version+1 OR NEW.proof_digest=OLD.proof_digest OR NEW.expires_at<>NEW.updated_at+INTERVAL '72 hours'
    THEN RAISE EXCEPTION 'TEAM_RESEND_PROOF_REQUIRED' USING ERRCODE='23514'; END IF;
  ELSIF NEW.proof_digest<>OLD.proof_digest OR NEW.proof_version<>OLD.proof_version OR NEW.expires_at<>OLD.expires_at
   THEN RAISE EXCEPTION 'TEAM_TERMINAL_PROOF_IMMUTABLE' USING ERRCODE='23514'; END IF;
  IF NEW.state='EXPIRED' AND NEW.updated_at<OLD.expires_at THEN RAISE EXCEPTION 'TEAM_NOT_EXPIRED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NEW;
END $$;
CREATE TRIGGER team_invitation_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.team_invitation FOR EACH ROW EXECUTE FUNCTION merchant.guard_team_invitation();

CREATE FUNCTION merchant.check_team_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE r merchant.team_command_receipt%ROWTYPE; a merchant.team_audit%ROWTYPE; i merchant.team_invitation%ROWTYPE; m merchant.membership%ROWTYPE; inviter merchant.membership%ROWTYPE;
BEGIN
 IF TG_TABLE_NAME='team_audit' THEN
  SELECT * INTO r FROM merchant.team_command_receipt WHERE organization_id=NEW.organization_id AND actor_account_id=NEW.actor_account_id AND operation=NEW.operation AND key=NEW.request_id;
 ELSE
  r:=NEW;
 END IF;
 SELECT * INTO a FROM merchant.team_audit WHERE organization_id=r.organization_id AND actor_account_id=r.actor_account_id AND operation=r.operation AND request_id=r.key;
 IF a.id IS NULL OR a.actor_account_id IS DISTINCT FROM r.actor_account_id OR a.occurred_at IS DISTINCT FROM r.recorded_at OR a.target_membership_id IS DISTINCT FROM r.membership_id AND r.membership_id IS NOT NULL
   OR a.invitation_id IS DISTINCT FROM r.invitation_id
 THEN RAISE EXCEPTION 'TEAM_ATOMIC_AUDIT_RECEIPT_REQUIRED' USING ERRCODE='23514'; END IF;
 IF r.invitation_id IS NOT NULL THEN
  SELECT * INTO i FROM merchant.team_invitation WHERE id=r.invitation_id AND organization_id=r.organization_id;
  IF i.id IS NULL OR i.version<>r.version OR i.updated_at<>r.recorded_at OR i.state<>(CASE r.operation WHEN 'INVITE' THEN 'PENDING' WHEN 'RESEND' THEN 'PENDING'
    WHEN 'REVOKE_INVITATION' THEN 'REVOKED' WHEN 'ACCEPT' THEN 'ACCEPTED' WHEN 'DECLINE' THEN 'DECLINED' END)
   THEN RAISE EXCEPTION 'TEAM_INVITATION_RECEIPT_REQUIRED' USING ERRCODE='23514'; END IF;
  SELECT * INTO inviter FROM merchant.membership WHERE id=i.inviter_membership_id AND organization_id=i.organization_id;
  IF NOT EXISTS(SELECT 1 FROM merchant.team_invitation WHERE id=i.id AND xmin::text=pg_current_xact_id()::text)
   OR NOT EXISTS(SELECT 1 FROM merchant.team_command_receipt WHERE organization_id=r.organization_id AND actor_account_id=r.actor_account_id AND operation=r.operation AND key=r.key AND xmin::text=pg_current_xact_id()::text)
   THEN RAISE EXCEPTION 'TEAM_CURRENT_TRANSACTION_REQUIRED' USING ERRCODE='23514';END IF;
  IF inviter.state<>'ACTIVE' OR inviter.authority_version<>i.inviter_authority_version
    OR NOT EXISTS(SELECT 1 FROM merchant.team_policy p WHERE p.organization_id=i.organization_id AND p.version=i.policy_version)
    OR NOT merchant.team_permission(i.organization_id,inviter.account_id,'team.invite')
   THEN RAISE EXCEPTION 'TEAM_CURRENT_INVITER_REQUIRED' USING ERRCODE='23514'; END IF;
  IF r.operation IN ('INVITE','RESEND','REVOKE_INVITATION') AND (a.actor_membership_id<>i.inviter_membership_id AND NOT EXISTS(
    SELECT 1 FROM merchant.membership x WHERE x.id=a.actor_membership_id AND x.organization_id=i.organization_id AND x.role='OWNER' AND x.state='ACTIVE'))
   THEN RAISE EXCEPTION 'TEAM_INVITER_SCOPE_REQUIRED' USING ERRCODE='23514'; END IF;
  IF r.operation IN ('INVITE','RESEND','REVOKE_INVITATION') AND (NOT merchant.team_permission(i.organization_id,r.actor_account_id,'team.invite')
   OR NOT EXISTS(SELECT 1 FROM merchant.membership actor WHERE actor.id=a.actor_membership_id AND actor.organization_id=i.organization_id AND actor.account_id=r.actor_account_id AND actor.state='ACTIVE'))
   THEN RAISE EXCEPTION 'TEAM_INVITER_AUTHORITY_REQUIRED' USING ERRCODE='23514';END IF;
  IF r.operation IN ('INVITE','RESEND') AND NOT identity.membership_delivery_bound(i.id,i.organization_id,i.proof_version,i.proof_digest,inviter.account_id,i.expires_at)
   THEN RAISE EXCEPTION 'TEAM_PROTECTED_DELIVERY_REQUIRED' USING ERRCODE='23514';END IF;
  IF r.operation IN ('ACCEPT','DECLINE') AND NOT identity.membership_decision_consumed(i.id,i.organization_id,i.proof_version,r.actor_account_id,r.recorded_at)
   THEN RAISE EXCEPTION 'TEAM_RECIPIENT_PROOF_REQUIRED' USING ERRCODE='23514';END IF;
  IF r.operation IN ('INVITE','RESEND','ACCEPT') THEN
   IF (SELECT count(*) FROM merchant.team_invitation_grant WHERE invitation_id=i.id) NOT BETWEEN 1 AND 200
    OR NOT EXISTS(SELECT 1 FROM merchant.team_invitation_grant WHERE invitation_id=i.id AND permission='dashboard.read' AND scope='ORGANIZATION')
    OR EXISTS(SELECT 1 FROM merchant.team_invitation_grant g WHERE g.invitation_id=i.id AND NOT merchant.team_can_delegate(i.organization_id,inviter.account_id,i.role,g.permission,g.facility_id))
    THEN RAISE EXCEPTION 'TEAM_DELEGATION_DENIED' USING ERRCODE='23514'; END IF;
  END IF;
  IF r.operation='ACCEPT' THEN
   SELECT * INTO m FROM merchant.membership WHERE id=i.accepted_membership_id AND organization_id=i.organization_id;
   IF m.id IS NULL OR m.account_id<>r.actor_account_id OR m.role<>i.role OR m.state<>'ACTIVE' OR m.version<>1 OR a.target_membership_id<>m.id OR a.actor_membership_id<>m.id
    OR EXISTS((SELECT permission,scope,facility_id FROM merchant.team_invitation_grant WHERE invitation_id=i.id) EXCEPT (SELECT permission,scope,facility_id FROM merchant.member_grant WHERE membership_id=m.id))
    OR EXISTS((SELECT permission,scope,facility_id FROM merchant.member_grant WHERE membership_id=m.id) EXCEPT (SELECT permission,scope,facility_id FROM merchant.team_invitation_grant WHERE invitation_id=i.id))
    THEN RAISE EXCEPTION 'TEAM_ACCEPTANCE_MEMBERSHIP_REQUIRED' USING ERRCODE='23514'; END IF;
  END IF;
 ELSE
  SELECT * INTO m FROM merchant.membership WHERE id=r.membership_id AND organization_id=r.organization_id;
  IF m.id IS NULL OR m.role='OWNER' OR m.account_id=r.actor_account_id OR m.version<>r.version OR m.updated_at<>r.recorded_at
    OR NOT EXISTS(SELECT 1 FROM merchant.membership x WHERE x.id=a.actor_membership_id AND x.organization_id=r.organization_id AND x.account_id=r.actor_account_id AND x.role='OWNER' AND x.state='ACTIVE')
    OR NOT merchant.team_permission(r.organization_id,r.actor_account_id,CASE r.operation WHEN 'ACCESS' THEN 'team.access.manage' ELSE 'team.membership.manage' END)
    OR m.state<>(CASE r.operation WHEN 'SUSPEND' THEN 'SUSPENDED' WHEN 'REVOKE_MEMBER' THEN 'REVOKED' ELSE 'ACTIVE' END)
   THEN RAISE EXCEPTION 'TEAM_MEMBER_AUTHORITY_REQUIRED' USING ERRCODE='23514'; END IF;
 END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_receipt_evidence AFTER INSERT ON merchant.team_command_receipt DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_evidence();
CREATE CONSTRAINT TRIGGER team_audit_evidence AFTER INSERT ON merchant.team_audit DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_evidence();

CREATE FUNCTION merchant.check_team_invitation_evidence() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE i merchant.team_invitation%ROWTYPE;
BEGIN
 IF TG_TABLE_NAME='team_invitation' THEN SELECT * INTO i FROM merchant.team_invitation WHERE id=NEW.id;
 ELSE SELECT * INTO i FROM merchant.team_invitation WHERE id=NEW.invitation_id; END IF;
 IF i.id IS NULL THEN RAISE EXCEPTION 'TEAM_INVITATION_REQUIRED' USING ERRCODE='23514'; END IF;
 IF i.state='INVALIDATED' AND (NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.id=i.inviter_membership_id AND m.state='ACTIVE' AND m.authority_version=i.inviter_authority_version)
   OR NOT EXISTS(SELECT 1 FROM merchant.team_policy p WHERE p.organization_id=i.organization_id AND p.version=i.policy_version)
   OR NOT EXISTS(SELECT 1 FROM merchant.organization o WHERE o.id=i.organization_id AND o.state='ACTIVE')) THEN RETURN NULL; END IF;
 IF i.state='EXPIRED' AND i.updated_at>=i.expires_at THEN RETURN NULL; END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.team_command_receipt r WHERE r.invitation_id=i.id AND r.organization_id=i.organization_id AND r.version=i.version AND r.recorded_at=i.updated_at AND r.xmin::text=pg_current_xact_id()::text)
  THEN RAISE EXCEPTION 'TEAM_INVITATION_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_invitation_evidence AFTER INSERT OR UPDATE ON merchant.team_invitation DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_invitation_evidence();
CREATE CONSTRAINT TRIGGER team_invitation_grant_evidence AFTER INSERT ON merchant.team_invitation_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_invitation_evidence();

-- OTA-PERM-01..05 / AMEND-02: this additive trigger touches only NEW team.* grants.
-- Keep all V23/V32 guards and operator EXECUTE privileges unchanged here.
CREATE FUNCTION merchant.guard_team_operator_grant() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE member_role TEXT;
BEGIN
 IF NEW.permission NOT LIKE 'team.%' THEN RETURN NEW; END IF;
 SELECT role INTO member_role FROM merchant.membership WHERE id=NEW.membership_id AND organization_id=NEW.organization_id;
 IF current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant'))
   OR COALESCE(current_setting('argopost.operator_environment',TRUE),'') NOT IN ('LOCAL','TEST')
   OR NEW.scope<>'ORGANIZATION' OR member_role NOT IN ('OWNER','MANAGER')
   OR (member_role='MANAGER' AND NEW.permission IN ('team.access.manage','team.membership.manage'))
 THEN RAISE EXCEPTION 'TEAM_INDEPENDENT_OPERATOR_REQUIRED' USING ERRCODE='42501'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER team_operator_grant BEFORE INSERT ON merchant.member_grant FOR EACH ROW EXECUTE FUNCTION merchant.guard_team_operator_grant();
REVOKE ALL ON FUNCTION merchant.guard_team_operator_grant() FROM PUBLIC,"${runtimeRole}";

-- Approved OTA-AMEND-03, exact manifest f6e21ee3... confirmed by Owner.
-- Pre-fix PG18 test teamOperatorGrantRemainsIndependentBoundedAuditedAndRuntimeDenied
-- observed WORKSPACE_COMMAND_INVALID at the existing allowlist, function line14.
-- Baseline comparison: only TWO validation lines differ from the V30 definition:
-- capacity18->32 and the exact five team.* values; all command/lock/audit code is retained.
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
 OR cardinality(p_permissions)>32 OR array_position(p_permissions,NULL) IS NOT NULL
 OR EXISTS(SELECT 1 FROM unnest(p_permissions) p WHERE p NOT IN ('dashboard.read','dashboard.finance.read','dashboard.staff.read','shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','facility.manage','defaults.manage','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','team.read','team.invite','team.access.manage','team.membership.manage','team.audit.read'))
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

-- Add the member evidence guard without changing runtime privileges.
-- Runtime writes are granted below only after invitation/grant/delivery guards are installed.
CREATE FUNCTION merchant.check_team_member_update() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE r merchant.team_command_receipt%ROWTYPE; op TEXT;
BEGIN
 IF current_user=pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')) THEN RETURN NULL; END IF;
 op:=CASE WHEN NEW.state<>OLD.state THEN CASE NEW.state WHEN 'ACTIVE' THEN 'RESUME' WHEN 'SUSPENDED' THEN 'SUSPEND' ELSE 'REVOKE_MEMBER' END ELSE 'ACCESS' END;
 SELECT * INTO r FROM merchant.team_command_receipt WHERE membership_id=NEW.id AND organization_id=NEW.organization_id
  AND version=NEW.version AND operation=op AND xmin::text=pg_current_xact_id()::text;
 IF r.membership_id IS NULL OR OLD.role='OWNER' OR NEW.role='OWNER' OR NEW.account_id=r.actor_account_id
  OR NOT EXISTS(SELECT 1 FROM merchant.team_audit a WHERE a.organization_id=r.organization_id AND a.actor_account_id=r.actor_account_id
   AND a.operation=r.operation AND a.request_id=r.key AND a.target_membership_id=NEW.id AND a.occurred_at=r.recorded_at)
  THEN RAISE EXCEPTION 'TEAM_MEMBER_ATOMIC_EVIDENCE_REQUIRED' USING ERRCODE='23514'; END IF;
 IF (op='RESUME' AND OLD.state<>'SUSPENDED') OR (op='SUSPEND' AND OLD.state<>'ACTIVE')
  OR (op='REVOKE_MEMBER' AND OLD.state NOT IN ('ACTIVE','SUSPENDED')) THEN RAISE EXCEPTION 'TEAM_MEMBER_TRANSITION_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_member_update AFTER UPDATE ON merchant.membership DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_member_update();
REVOKE ALL ON FUNCTION merchant.check_team_member_update() FROM PUBLIC,"${runtimeRole}";

-- Owner separately confirmed this bounded guard patch after automatic review on2026-11-17.
-- Identity-owned boolean verification: Merchant does not read credential tables.
CREATE FUNCTION identity.team_reauthenticated_in_transaction(actor UUID,action_at TIMESTAMPTZ) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM identity.customer_proof p JOIN identity.user_account a ON a.id=p.account_id
  JOIN identity.customer_auth_session s ON s.id=p.bound_session_id AND s.account_id=p.account_id
  WHERE p.purpose='REAUTHENTICATION' AND p.account_id=actor AND p.subject_id=actor AND p.bound_auth_version=a.auth_version
   AND a.status='ACTIVE' AND a.email_verified AND p.revoked_at IS NULL AND p.consumed_at IS NOT NULL
   AND p.consumed_at BETWEEN action_at-INTERVAL '5 seconds' AND action_at AND p.expires_at>action_at
   AND p.xmin::text=pg_current_xact_id()::text AND s.revoked_at IS NULL AND s.absolute_expires_at>action_at AND s.idle_expires_at>action_at)
$$;
REVOKE ALL ON FUNCTION identity.team_reauthenticated_in_transaction(UUID,TIMESTAMPTZ) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.team_reauthenticated_in_transaction(UUID,TIMESTAMPTZ) TO "${runtimeRole}";

CREATE FUNCTION merchant.team_member_change_allowed(before_row merchant.membership,after_row merchant.membership) RETURNS BOOLEAN LANGUAGE plpgsql STABLE AS $$
DECLARE receipt merchant.team_command_receipt%ROWTYPE; required TEXT; op TEXT;
BEGIN
 IF before_row.role NOT IN ('MANAGER','OPERATOR') OR after_row.role NOT IN ('MANAGER','OPERATOR') OR before_row.state='REVOKED' THEN RETURN FALSE; END IF;
 op:=CASE WHEN before_row.state=after_row.state THEN 'ACCESS' WHEN after_row.state='SUSPENDED' THEN 'SUSPEND' WHEN after_row.state='ACTIVE' THEN 'RESUME' ELSE 'REVOKE_MEMBER' END;
 IF (op='ACCESS' AND before_row.state<>'ACTIVE') OR (op='SUSPEND' AND before_row.state<>'ACTIVE')
  OR (op='RESUME' AND before_row.state<>'SUSPENDED') OR (op<>'ACCESS' AND before_row.role<>after_row.role) THEN RETURN FALSE; END IF;
 SELECT * INTO receipt FROM merchant.team_command_receipt r WHERE r.membership_id=before_row.id AND r.organization_id=before_row.organization_id
  AND r.version=after_row.version AND r.operation=op AND r.recorded_at=after_row.updated_at AND r.xmin::text=pg_current_xact_id()::text;
 required:=CASE op WHEN 'ACCESS' THEN 'team.access.manage' ELSE 'team.membership.manage' END;
 IF receipt.membership_id IS NULL OR receipt.actor_account_id=before_row.account_id
  OR NOT merchant.team_permission(before_row.organization_id,receipt.actor_account_id,required)
  OR NOT identity.team_reauthenticated_in_transaction(receipt.actor_account_id,after_row.updated_at)
  OR NOT EXISTS(SELECT 1 FROM merchant.team_audit a JOIN merchant.membership actor ON actor.id=a.actor_membership_id AND actor.organization_id=a.organization_id
   WHERE a.organization_id=receipt.organization_id AND a.actor_account_id=receipt.actor_account_id AND actor.account_id=a.actor_account_id AND actor.state='ACTIVE' AND actor.role='OWNER'
    AND a.operation=receipt.operation AND a.request_id=receipt.key AND a.target_membership_id=before_row.id AND a.occurred_at=receipt.recorded_at)
 THEN RETURN FALSE; END IF;
 IF op='ACCESS' AND EXISTS(SELECT 1 FROM merchant.member_grant g WHERE g.membership_id=before_row.id AND
   (NOT g.permission=ANY(merchant.team_role_ceiling(before_row.role)) OR NOT merchant.team_permission(g.organization_id,receipt.actor_account_id,g.permission,g.facility_id))) THEN RETURN FALSE; END IF;
 RETURN TRUE;
END $$;
REVOKE ALL ON FUNCTION merchant.team_member_change_allowed(merchant.membership,merchant.membership) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.team_member_change_allowed(merchant.membership,merchant.membership) TO "${runtimeRole}";

-- V23 identity/version/provenance checks retained. A role change additionally
-- requires actual receipt/audit + Owner permission + consumed reauthentication.
-- This patch deliberately adds no runtime write ACL.
CREATE OR REPLACE FUNCTION merchant.guard_workspace_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN
   IF TG_TABLE_NAME<>'member_grant' THEN RAISE EXCEPTION 'WORKSPACE_DELETE_DENIED' USING ERRCODE='23514'; END IF; RETURN OLD;
 END IF;
 IF TG_OP='UPDATE' THEN
   IF TG_TABLE_NAME='member_grant' THEN RAISE EXCEPTION 'WORKSPACE_GRANT_IMMUTABLE' USING ERRCODE='23514'; END IF;
   IF NEW.id<>OLD.id OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at THEN RAISE EXCEPTION 'WORKSPACE_VERSION_REQUIRED' USING ERRCODE='23514'; END IF;
   IF TG_TABLE_NAME='organization' THEN
     IF NEW.admission_origin<>OLD.admission_origin OR NEW.environment<>OLD.environment OR NEW.initial_owner_membership_id<>OLD.initial_owner_membership_id THEN RAISE EXCEPTION 'WORKSPACE_ORIGIN_IMMUTABLE' USING ERRCODE='23514'; END IF;
   ELSE
     IF NEW.organization_id<>OLD.organization_id OR NEW.account_id<>OLD.account_id OR OLD.state='REVOKED'
      OR NEW.authority_version<>OLD.authority_version+1 THEN RAISE EXCEPTION 'WORKSPACE_MEMBERSHIP_IMMUTABLE' USING ERRCODE='23514'; END IF;
     IF (NEW.role<>OLD.role OR current_user<>pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant')))
       AND NOT merchant.team_member_change_allowed(OLD,NEW) THEN RAISE EXCEPTION 'TEAM_MEMBER_AUTHORITY_REQUIRED' USING ERRCODE='23514'; END IF;
   END IF;
 END IF; RETURN NEW;
END $$;

-- OTA-AMEND-04: new purpose only; retain old proof purposes and all old limits.
ALTER TABLE identity.customer_proof DROP CONSTRAINT customer_proof_purpose_check;
ALTER TABLE identity.customer_proof ADD CONSTRAINT customer_proof_purpose_check CHECK(purpose IN (
 'CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY','SECURITY_EMAIL_CHANGE','BUSINESS_CONTACT','REAUTHENTICATION','OWNER_RECOVERY_CONSENT','LEGACY_CUSTOMER_LINK','MERCHANT_MEMBERSHIP'));
ALTER TABLE identity.customer_proof DROP CONSTRAINT ck_customer_recovery_otp_clock;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_recovery_otp_clock CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND expires_at IN (created_at+INTERVAL '24 hours',created_at+INTERVAL '2 minutes'))
 OR (purpose='CUSTOMER_RECOVERY' AND expires_at IN (created_at+INTERVAL '15 minutes',created_at+INTERVAL '2 minutes'))
 OR (purpose='MERCHANT_MEMBERSHIP' AND expires_at=created_at+INTERVAL '72 hours')
 OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY','MERCHANT_MEMBERSHIP') AND expires_at=created_at+INTERVAL '15 minutes'));
DO $$ DECLARE names TEXT[];
BEGIN
 SELECT array_agg(conname::text) INTO names FROM pg_constraint WHERE conrelid='identity.customer_proof'::regclass AND contype='c'
  AND pg_get_constraintdef(oid) LIKE '%enrollment_id IS NOT NULL%' AND pg_get_constraintdef(oid) LIKE '%subject_id = account_id%';
 IF cardinality(names) IS DISTINCT FROM 1 THEN RAISE EXCEPTION 'TEAM_IDENTITY_SUBJECT_BASELINE_REQUIRED';END IF;
 EXECUTE format('ALTER TABLE identity.customer_proof DROP CONSTRAINT %I',names[1]);
 SELECT array_agg(conname::text) INTO names FROM pg_constraint WHERE conrelid='identity.customer_proof'::regclass AND contype='c'
  AND pg_get_constraintdef(oid) LIKE '%OWNER_RECOVERY_CONSENT%' AND pg_get_constraintdef(oid) LIKE '%organization_id IS NOT NULL%';
 IF cardinality(names) IS DISTINCT FROM 1 THEN RAISE EXCEPTION 'TEAM_IDENTITY_ORGANIZATION_BASELINE_REQUIRED';END IF;
 EXECUTE format('ALTER TABLE identity.customer_proof DROP CONSTRAINT %I',names[1]);
END $$;
ALTER TABLE identity.customer_proof ADD CONSTRAINT customer_proof_subject_binding CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND enrollment_id IS NOT NULL AND account_id IS NULL)
 OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','MERCHANT_MEMBERSHIP') AND enrollment_id IS NULL AND account_id IS NOT NULL AND subject_id=account_id)
 OR (purpose='MERCHANT_MEMBERSHIP' AND enrollment_id IS NULL AND account_id IS NOT NULL));
ALTER TABLE identity.customer_proof ADD CONSTRAINT customer_proof_organization_binding CHECK ((purpose IN ('OWNER_RECOVERY_CONSENT','MERCHANT_MEMBERSHIP'))=(organization_id IS NOT NULL));
ALTER TABLE identity.customer_proof DROP CONSTRAINT ck_customer_proof_subject_session_version;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_proof_subject_session_version CHECK (
 (purpose='CUSTOMER_ENROLLMENT' AND bound_session_id IS NULL AND bound_auth_version IS NULL)
 OR (purpose='CUSTOMER_RECOVERY' AND bound_session_id IS NULL AND (bound_auth_version IS NOT NULL OR consumed_at IS NOT NULL OR revoked_at IS NOT NULL))
 OR (purpose NOT IN ('CUSTOMER_ENROLLMENT','CUSTOMER_RECOVERY','MERCHANT_MEMBERSHIP') AND bound_session_id IS NOT NULL AND bound_auth_version IS NOT NULL)
 OR (purpose='MERCHANT_MEMBERSHIP' AND ((consumed_at IS NULL AND bound_session_id IS NULL AND bound_auth_version IS NULL)
    OR (consumed_at IS NOT NULL AND bound_session_id IS NOT NULL AND bound_auth_version IS NOT NULL))));
ALTER TABLE identity.customer_proof DROP CONSTRAINT ck_customer_proof_otp_policy;
ALTER TABLE identity.customer_proof ADD CONSTRAINT ck_customer_proof_otp_policy CHECK (
 (purpose='MERCHANT_MEMBERSHIP' AND policy_version='OTA-TEAM-1') OR (purpose<>'MERCHANT_MEMBERSHIP' AND policy_version IN ('S003-2026-10-27','S003-OTP-2026-10-30')));
ALTER TABLE identity.customer_delivery DROP CONSTRAINT ck_customer_delivery_otp_template;
ALTER TABLE identity.customer_delivery ADD CONSTRAINT ck_customer_delivery_otp_template CHECK (
 (purpose='MERCHANT_MEMBERSHIP' AND template_version='OTA-TEAM-EMAIL-1') OR (purpose<>'MERCHANT_MEMBERSHIP' AND template_version IN ('S003-EMAIL-1','S003-EMAIL-OTP-1')));

CREATE FUNCTION merchant.initialize_team_policy() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN INSERT INTO merchant.team_policy VALUES(NEW.id,1,1,NEW.updated_at);RETURN NULL;END $$;
CREATE TRIGGER team_policy_initial AFTER INSERT ON merchant.organization FOR EACH ROW EXECUTE FUNCTION merchant.initialize_team_policy();
CREATE FUNCTION merchant.guard_team_policy() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
 PERFORM pg_advisory_xact_lock(730023,1);
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'TEAM_POLICY_IMMUTABLE' USING ERRCODE='23514';END IF;
 IF TG_OP='INSERT' AND NEW.version<>1 THEN RAISE EXCEPTION 'TEAM_POLICY_INITIAL_VERSION' USING ERRCODE='23514';END IF;
 IF TG_OP='UPDATE' AND (NEW.organization_id<>OLD.organization_id OR NEW.version<>OLD.version+1 OR NEW.updated_at<OLD.updated_at) THEN RAISE EXCEPTION 'TEAM_POLICY_VERSION_REQUIRED' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER team_policy_guard BEFORE INSERT OR UPDATE OR DELETE ON merchant.team_policy FOR EACH ROW EXECUTE FUNCTION merchant.guard_team_policy();

CREATE FUNCTION identity.membership_proof_id(invitation UUID,version BIGINT) RETURNS UUID LANGUAGE sql IMMUTABLE AS $$
 WITH raw AS (SELECT decode(md5('merchant-membership-proof-v1:'||invitation::text||':'||version::text),'hex') AS b)
 SELECT encode(set_byte(set_byte(b,6,(get_byte(b,6)&15)|48),8,(get_byte(b,8)&63)|128),'hex')::uuid FROM raw
$$;
CREATE FUNCTION identity.membership_delivery_bound(invitation UUID,org UUID,proof_version BIGINT,verifier BYTEA,issuer UUID,expires TIMESTAMPTZ) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM identity.customer_proof p JOIN identity.customer_delivery d ON d.proof_id=p.id
  WHERE p.id=identity.membership_proof_id(invitation,proof_version) AND p.subject_id=invitation AND p.organization_id=org AND p.account_id=issuer
   AND p.purpose='MERCHANT_MEMBERSHIP' AND p.verifier=$4 AND p.expires_at=expires AND p.consumed_at IS NULL AND p.revoked_at IS NULL
   AND d.subject_id=invitation AND d.purpose=p.purpose AND d.expires_at=p.expires_at
   AND d.status IN ('QUEUED','SENDING','RETRY_WAIT','ACCEPTED_BY_PROVIDER','BLOCKED') AND p.xmin::text=pg_current_xact_id()::text AND d.xmin::text=pg_current_xact_id()::text)
$$;
CREATE FUNCTION identity.membership_decision_consumed(invitation UUID,org UUID,proof_version BIGINT,actor UUID,action_at TIMESTAMPTZ) RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT EXISTS(SELECT 1 FROM identity.customer_proof p JOIN identity.customer_auth_session s ON s.id=p.bound_session_id AND s.account_id=p.account_id
  JOIN identity.user_account a ON a.id=p.account_id JOIN identity.customer_delivery d ON d.proof_id=p.id
  WHERE p.id=identity.membership_proof_id(invitation,proof_version) AND p.subject_id=invitation AND p.organization_id=org AND p.purpose='MERCHANT_MEMBERSHIP'
   AND p.account_id=actor AND p.bound_auth_version=a.auth_version AND a.status='ACTIVE' AND a.email_verified
   AND p.revoked_at IS NULL AND p.consumed_at BETWEEN action_at-INTERVAL '5 seconds' AND action_at AND p.expires_at>action_at
   AND s.revoked_at IS NULL AND s.absolute_expires_at>action_at AND s.idle_expires_at>action_at
   AND p.xmin::text=pg_current_xact_id()::text AND d.status='DELIVERED' AND d.delivery_evidence IS NOT NULL)
$$;
-- SQL trigger-only revocation port: does not expose Identity table mutation to Merchant code.
CREATE FUNCTION identity.invalidate_membership_delivery(invitation UUID,org UUID,action_at TIMESTAMPTZ) RETURNS VOID LANGUAGE plpgsql AS $$
BEGIN
 IF pg_trigger_depth()=0 THEN RAISE EXCEPTION 'MEMBERSHIP_TRIGGER_REQUIRED' USING ERRCODE='42501';END IF;
 UPDATE identity.customer_proof SET revoked_at=GREATEST(action_at,created_at)
  WHERE subject_id=invitation AND organization_id=org AND purpose='MERCHANT_MEMBERSHIP' AND consumed_at IS NULL AND revoked_at IS NULL;
 UPDATE identity.customer_delivery SET status='EXPIRED',finished_at=GREATEST(action_at,created_at),lease_id=NULL,lease_until=NULL,
  nonce=NULL,ciphertext=NULL,recipient_nonce=NULL,recipient_ciphertext=NULL,version=version+1
  WHERE subject_id=invitation AND purpose='MERCHANT_MEMBERSHIP' AND status NOT IN ('EXPIRED','DELIVERED');
END $$;
CREATE FUNCTION merchant.invalidate_pending_team_invites() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE org UUID; invitation UUID; stamp TIMESTAMPTZ;
BEGIN
 IF TG_TABLE_NAME='membership' THEN org:=NEW.organization_id;stamp:=NEW.updated_at;
 ELSIF TG_TABLE_NAME='organization' THEN org:=NEW.id;stamp:=NEW.updated_at;
 ELSIF TG_TABLE_NAME='team_policy' THEN org:=NEW.organization_id;stamp:=NEW.updated_at;
 ELSIF TG_OP='DELETE' THEN org:=OLD.organization_id;stamp:=clock_timestamp();ELSE org:=NEW.organization_id;stamp:=clock_timestamp();END IF;
 FOR invitation IN UPDATE merchant.team_invitation i SET state='INVALIDATED',version=version+1,updated_at=GREATEST(stamp,i.updated_at)
  WHERE i.organization_id=org AND i.state='PENDING' AND (NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.id=i.inviter_membership_id AND m.state='ACTIVE' AND m.authority_version=i.inviter_authority_version)
   OR NOT EXISTS(SELECT 1 FROM merchant.team_policy p WHERE p.organization_id=org AND p.version=i.policy_version)
   OR NOT EXISTS(SELECT 1 FROM merchant.organization o WHERE o.id=org AND o.state='ACTIVE')) RETURNING id
 LOOP PERFORM identity.invalidate_membership_delivery(invitation,org,stamp);END LOOP;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_membership_invalidation AFTER UPDATE ON merchant.membership DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.invalidate_pending_team_invites();
CREATE CONSTRAINT TRIGGER team_policy_invalidation AFTER UPDATE ON merchant.team_policy DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.invalidate_pending_team_invites();
CREATE CONSTRAINT TRIGGER team_org_invalidation AFTER UPDATE ON merchant.organization DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.invalidate_pending_team_invites();

CREATE FUNCTION merchant.guard_invitation_grant_insert() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE i merchant.team_invitation%ROWTYPE;
BEGIN
 SELECT * INTO i FROM merchant.team_invitation WHERE id=NEW.invitation_id AND organization_id=NEW.organization_id;
 IF i.id IS NULL OR i.state<>'PENDING' OR i.version<>1 OR i.proof_version<>1
  OR NOT EXISTS(SELECT 1 FROM merchant.team_invitation WHERE id=i.id AND xmin::text=pg_current_xact_id()::text)
  OR NOT NEW.permission=ANY(merchant.team_role_ceiling(i.role))
  OR (NEW.facility_id IS NOT NULL AND (NEW.permission NOT IN ('shipment.draft.read','shipment.draft.write','shipment.import','shipment.template.manage','shipment.prepare','shipment.quote','shipment.book','shipment.label.read','shipment.export','shipment.export.pii','shipment.assign','shipment.reports.read','shipment.finance.read','contacts.read','contacts.write','contacts.import','contacts.export','shipment.actions.manage','shipment.actions.respond','facility.read','facility.manage','shipment.handover.read','shipment.handover.manage')
    OR NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=NEW.facility_id AND f.organization_id=NEW.organization_id AND f.state='ACTIVE')))
 THEN RAISE EXCEPTION 'TEAM_INVITATION_GRANT_INTENT_REQUIRED' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER team_invitation_grant_insert BEFORE INSERT ON merchant.team_invitation_grant FOR EACH ROW EXECUTE FUNCTION merchant.guard_invitation_grant_insert();

-- Additive guards for member/grant inserts and removals. The existing offline
-- operator lane remains schema-owner + explicit Local/Test configuration only.
CREATE FUNCTION merchant.team_offline_operator() RETURNS BOOLEAN LANGUAGE sql STABLE AS $$
 SELECT current_user=pg_get_userbyid((SELECT nspowner FROM pg_namespace WHERE nspname='merchant'))
  AND COALESCE(current_setting('argopost.operator_environment',true),'') IN ('LOCAL','TEST')
$$;
CREATE FUNCTION merchant.check_team_member_insert() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE i merchant.team_invitation%ROWTYPE;
BEGIN
 IF merchant.team_offline_operator() THEN RETURN NULL;END IF;
 SELECT * INTO i FROM merchant.team_invitation WHERE accepted_membership_id=NEW.id AND organization_id=NEW.organization_id AND state='ACCEPTED';
 IF i.id IS NULL OR NEW.role<>i.role OR NEW.state<>'ACTIVE' OR NEW.version<>1 OR NEW.authority_version<>1 OR NEW.created_at<>NEW.updated_at
  OR NOT EXISTS(SELECT 1 FROM merchant.team_command_receipt r WHERE r.invitation_id=i.id AND r.organization_id=i.organization_id
   AND r.operation='ACCEPT' AND r.actor_account_id=NEW.account_id AND r.recorded_at=NEW.created_at AND r.version=i.version AND r.xmin::text=pg_current_xact_id()::text)
  OR NOT identity.membership_decision_consumed(i.id,i.organization_id,i.proof_version,NEW.account_id,NEW.created_at)
 THEN RAISE EXCEPTION 'TEAM_ACCEPTANCE_EVIDENCE_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_member_insert AFTER INSERT ON merchant.membership DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_member_insert();

CREATE FUNCTION merchant.check_team_grant_mutation() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE g merchant.member_grant%ROWTYPE; m merchant.membership%ROWTYPE; r merchant.team_command_receipt%ROWTYPE; i merchant.team_invitation%ROWTYPE;
BEGIN
 IF merchant.team_offline_operator() THEN RETURN NULL;END IF;
 IF TG_OP='DELETE' THEN g:=OLD;ELSE g:=NEW;END IF;
 SELECT * INTO m FROM merchant.membership WHERE id=g.membership_id AND organization_id=g.organization_id;
 IF m.id IS NULL OR m.role='OWNER' OR NOT EXISTS(SELECT 1 FROM merchant.membership WHERE id=m.id AND xmin::text=pg_current_xact_id()::text)
  THEN RAISE EXCEPTION 'TEAM_GRANT_MEMBERSHIP_COMMAND_REQUIRED' USING ERRCODE='23514';END IF;
 SELECT * INTO i FROM merchant.team_invitation WHERE accepted_membership_id=m.id AND organization_id=m.organization_id AND state='ACCEPTED';
 IF TG_OP='INSERT' AND m.version=1 AND i.id IS NOT NULL AND EXISTS(SELECT 1 FROM merchant.team_command_receipt receipt WHERE receipt.invitation_id=i.id
  AND receipt.organization_id=i.organization_id AND receipt.operation='ACCEPT' AND receipt.actor_account_id=m.account_id AND receipt.xmin::text=pg_current_xact_id()::text)
  AND EXISTS(SELECT 1 FROM merchant.team_invitation_grant intent WHERE intent.invitation_id=i.id AND intent.permission=g.permission AND intent.scope=g.scope AND intent.facility_id IS NOT DISTINCT FROM g.facility_id)
 THEN RETURN NULL;END IF;
 SELECT * INTO r FROM merchant.team_command_receipt receipt WHERE receipt.membership_id=m.id AND receipt.organization_id=m.organization_id
  AND receipt.version=m.version AND receipt.recorded_at=m.updated_at AND receipt.operation IN ('ACCESS','REVOKE_MEMBER') AND receipt.xmin::text=pg_current_xact_id()::text;
 IF r.membership_id IS NULL OR NOT identity.team_reauthenticated_in_transaction(r.actor_account_id,m.updated_at)
  OR NOT merchant.team_permission(m.organization_id,r.actor_account_id,CASE r.operation WHEN 'ACCESS' THEN 'team.access.manage' ELSE 'team.membership.manage' END)
  OR NOT EXISTS(SELECT 1 FROM merchant.membership actor WHERE actor.organization_id=m.organization_id AND actor.account_id=r.actor_account_id AND actor.role='OWNER' AND actor.state='ACTIVE' AND actor.id<>m.id)
  OR (r.operation='REVOKE_MEMBER' AND (TG_OP<>'DELETE' OR m.state<>'REVOKED'))
  OR (r.operation='ACCESS' AND (m.state<>'ACTIVE' OR NOT merchant.team_permission(g.organization_id,r.actor_account_id,g.permission,g.facility_id)))
  OR (TG_OP='INSERT' AND NOT merchant.team_can_delegate(g.organization_id,r.actor_account_id,m.role,g.permission,g.facility_id))
  OR (TG_OP='INSERT' AND g.facility_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM merchant.facility f WHERE f.id=g.facility_id AND f.organization_id=g.organization_id AND f.state='ACTIVE'))
 THEN RAISE EXCEPTION 'TEAM_GRANT_DELEGATION_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_grant_mutation AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_grant_mutation();

-- A raw grant change cannot leave an old authority version and live invitation.
-- Operator and application commands must also update the canonical membership.
CREATE FUNCTION merchant.check_team_authority_change() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE member UUID; org UUID;
BEGIN
 IF TG_OP='DELETE' THEN member:=OLD.membership_id;org:=OLD.organization_id;ELSE member:=NEW.membership_id;org:=NEW.organization_id;END IF;
 IF NOT EXISTS(SELECT 1 FROM merchant.membership m WHERE m.id=member AND m.organization_id=org AND m.xmin::text=pg_current_xact_id()::text)
 THEN RAISE EXCEPTION 'TEAM_AUTHORITY_VERSION_REQUIRED' USING ERRCODE='23514';END IF;RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER team_authority_change AFTER INSERT OR DELETE ON merchant.member_grant DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION merchant.check_team_authority_change();

REVOKE ALL ON FUNCTION merchant.initialize_team_policy(),merchant.guard_team_policy(),merchant.invalidate_pending_team_invites(),merchant.guard_invitation_grant_insert(),merchant.check_team_member_insert(),merchant.check_team_grant_mutation(),merchant.check_team_authority_change() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.team_offline_operator() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.team_offline_operator() TO "${runtimeRole}";
REVOKE ALL ON FUNCTION identity.membership_proof_id(UUID,BIGINT),identity.membership_delivery_bound(UUID,UUID,BIGINT,BYTEA,UUID,TIMESTAMPTZ),identity.membership_decision_consumed(UUID,UUID,BIGINT,UUID,TIMESTAMPTZ),identity.invalidate_membership_delivery(UUID,UUID,TIMESTAMPTZ) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION identity.membership_proof_id(UUID,BIGINT),identity.membership_delivery_bound(UUID,UUID,BIGINT,BYTEA,UUID,TIMESTAMPTZ),identity.membership_decision_consumed(UUID,UUID,BIGINT,UUID,TIMESTAMPTZ),identity.invalidate_membership_delivery(UUID,UUID,TIMESTAMPTZ) TO "${runtimeRole}";

-- Owner explicitly approved this bounded ACL source patch after automatic review.
-- Execute/verify only on isolated test databases; no localhost deployment.
REVOKE ALL ON merchant.team_policy,merchant.team_invitation,merchant.team_invitation_grant,merchant.team_audit,merchant.team_command_receipt FROM PUBLIC,"${runtimeRole}";
GRANT SELECT ON merchant.team_policy,merchant.team_invitation,merchant.team_invitation_grant,merchant.team_audit,merchant.team_command_receipt TO "${runtimeRole}";
GRANT INSERT ON merchant.membership,merchant.member_grant,merchant.team_invitation,merchant.team_invitation_grant,merchant.team_audit,merchant.team_command_receipt TO "${runtimeRole}";
GRANT UPDATE(role,state,authority_version,version,updated_at) ON merchant.membership TO "${runtimeRole}";
GRANT UPDATE(state,proof_digest,proof_version,expires_at,accepted_membership_id,version,updated_at) ON merchant.team_invitation TO "${runtimeRole}";
GRANT DELETE ON merchant.member_grant TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.team_role_ceiling(TEXT),merchant.team_permission(UUID,UUID,TEXT,UUID),merchant.team_can_delegate(UUID,UUID,TEXT,TEXT,UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION merchant.team_role_ceiling(TEXT),merchant.team_permission(UUID,UUID,TEXT,UUID),merchant.team_can_delegate(UUID,UUID,TEXT,TEXT,UUID) TO "${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.team_append_only(),merchant.guard_team_invitation(),merchant.check_team_evidence() FROM PUBLIC,"${runtimeRole}";
REVOKE ALL ON FUNCTION merchant.check_team_invitation_evidence() FROM PUBLIC,"${runtimeRole}";
