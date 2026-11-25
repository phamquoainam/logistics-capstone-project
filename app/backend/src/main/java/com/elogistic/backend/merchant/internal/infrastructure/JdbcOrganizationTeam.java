package com.elogistic.backend.merchant.internal.infrastructure;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam;
import com.elogistic.backend.merchant.api.customer.OrganizationTeam.*;
import com.elogistic.backend.merchant.internal.domain.OrganizationTeamPolicy;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.merchant.internal.domain.OrganizationTeamPolicy.fail;
/** Current Merchant admission at every boundary; names resolved through the Identity public port. */
public final class JdbcOrganizationTeam implements OrganizationTeam {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final java.security.SecureRandom RANDOM=new java.security.SecureRandom();
    private String originPredicate(){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regprocedure('merchant.application_origin_admitted(uuid)') IS NOT NULL",Boolean.class))?"merchant.application_origin_admitted(o.id)":"o.admission_origin='LOCAL_OPERATOR_PROVISIONED'";}
    private final DataSource source;private final JdbcTemplate jdbc;private final boolean enabled;private final MerchantFingerprints fingerprints;private final Clock clock;
    public JdbcOrganizationTeam(DataSource source,boolean enabled,MerchantFingerprints fingerprints,Clock clock){this.source=source;this.jdbc=new JdbcTemplate(source);this.enabled=enabled;this.fingerprints=fingerprints;this.clock=clock;}
    private void permit(Access a,String permission){
        a.requireCurrent();if(!enabled)throw fail("ORGANIZATION_TEAM_DISABLED");
        if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("TEAM_ADMISSION_REQUIRED");
        if(!a.scope().permits(permission,null))throw fail("AUTHORITY_REQUIRED");
    }
    private record Binding(UUID id,UUID account,String role,String state,long authority,long version,Instant created,Instant updated){}
    @Override public boolean deliveryCurrent(UUID invitation,UUID proof,UUID organization,UUID issuer,Instant now,java.util.function.Predicate<UUID> eligible){
        if(!enabled||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))return false;
        jdbc.query("SELECT pg_advisory_xact_lock(730023,1)",(r,n)->0);
        var rows=jdbc.query("SELECT i.proof_version,owner.account_id FROM merchant.team_invitation i JOIN merchant.membership m ON m.id=i.inviter_membership_id AND m.organization_id=i.organization_id JOIN merchant.organization o ON o.id=i.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id AND owner.organization_id=o.id JOIN merchant.team_policy p ON p.organization_id=o.id WHERE i.id=? AND i.organization_id=? AND i.state='PENDING' AND i.expires_at>? AND m.account_id=? AND m.state='ACTIVE' AND m.authority_version=i.inviter_authority_version AND o.state='ACTIVE' AND owner.state='ACTIVE' AND owner.role='OWNER' AND p.version=i.policy_version AND merchant.team_permission(o.id,m.account_id,'team.invite')",
          (r,n)->Map.entry(r.getLong(1),r.getObject(2,UUID.class)),invitation,organization,Timestamp.from(now),issuer);
        return rows.size()==1&&proof.equals(UUID.nameUUIDFromBytes(("merchant-membership-proof-v1:"+invitation+":"+rows.getFirst().getKey()).getBytes(java.nio.charset.StandardCharsets.US_ASCII)))&&eligible.test(issuer)&&eligible.test(rows.getFirst().getValue());
    }
    private List<Binding> bindings(Access a){
        return jdbc.query("SELECT id,account_id,role,state,authority_version,version,created_at,updated_at FROM merchant.membership WHERE organization_id=? ORDER BY id LIMIT 50051",
          (r,n)->new Binding(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getTimestamp(7).toInstant(),r.getTimestamp(8).toInstant()),a.scope().context().organizationId());
    }
    private Binding binding(Access a,UUID id){return jdbc.query("SELECT id,account_id,role,state,authority_version,version,created_at,updated_at FROM merchant.membership WHERE organization_id=? AND id=?",
          (r,n)->new Binding(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getTimestamp(7).toInstant(),r.getTimestamp(8).toInstant()),a.scope().context().organizationId(),id).stream().findFirst().orElseThrow(()->fail("RESOURCE_NOT_FOUND"));}
    private List<Grant> grants(Access a,UUID id){return jdbc.query("SELECT permission,scope,facility_id FROM merchant.member_grant WHERE membership_id=? AND organization_id=? ORDER BY permission,scope,facility_id NULLS FIRST",(r,n)->new Grant(r.getString(1),r.getString(2),r.getObject(3,UUID.class)),id,a.scope().context().organizationId());}
    private static String name(Map<UUID,String> names,Binding b){String name=names.get(b.account);if(name==null)throw fail("SERVICE_UNAVAILABLE");return name;}
    @Override public MemberPage members(Access a,MemberSearch q){
        permit(a,"team.read");if(q==null||q.page()<0||q.page()>1000||q.size()<1||q.size()>50||q.search()==null||q.search().codePointCount(0,q.search().length())>120
          ||q.search().codePoints().anyMatch(Character::isISOControl)||q.role()!=null&&!Set.of("OWNER","MANAGER","OPERATOR").contains(q.role())
          ||q.state()!=null&&!Set.of("ACTIVE","SUSPENDED","REVOKED").contains(q.state()))throw fail("VALIDATION_FAILED");
        if(q.facilityId()!=null&&!a.scope().facilityExists(q.facilityId()))throw fail("RESOURCE_NOT_FOUND");
        var bindings=bindings(a);if(bindings.size()>50050)throw fail("SERVICE_UNAVAILABLE");
        var accounts=new HashSet<UUID>();bindings.forEach(b->accounts.add(b.account));var names=a.displayNames(Set.copyOf(accounts));
        Set<UUID> atFacility=q.facilityId()==null?null:Set.copyOf(jdbc.query("SELECT DISTINCT membership_id FROM merchant.member_grant WHERE organization_id=? AND (scope='ORGANIZATION' OR facility_id=?)",(r,n)->r.getObject(1,UUID.class),a.scope().context().organizationId(),q.facilityId()));
        String needle=q.search().trim().toLowerCase(Locale.ROOT);long offset=Math.multiplyExact((long)q.page(),q.size());
        var rows=bindings.stream().filter(b->q.role()==null||q.role().equals(b.role)).filter(b->q.state()==null||q.state().equals(b.state))
          .filter(b->atFacility==null||atFacility.contains(b.id)).filter(b->name(names,b).toLowerCase(Locale.ROOT).contains(needle)).skip(offset).limit(q.size()+1L)
          .map(b->new Member(b.id,name(names,b),b.role,b.state,b.authority,b.version,b.created,b.updated)).toList();
        a.requireCurrent();return new MemberPage(rows.stream().limit(q.size()).toList(),q.page(),q.size(),rows.size()>q.size());
    }
    private MemberDetail detail(Access a,UUID id){var b=binding(a,id);var names=a.displayNames(Set.of(b.account));return new MemberDetail(b.id,name(names,b),b.role,b.state,b.authority,b.version,b.created,b.updated,grants(a,id));}
    @Override public MemberDetail member(Access a,UUID id){permit(a,"team.read");return detail(a,id);}
    @Override public RoleTemplates roleTemplates(Access a){permit(a,"team.read");return new RoleTemplates(List.of("OWNER","MANAGER","OPERATOR").stream().map(r->new RoleTemplate(r,1,OrganizationTeamPolicy.allowed(r))).toList());}
    @Override public Set<UUID> references(Access a,UUID memberId){a.requireCurrent();if(!enabled)throw fail("ORGANIZATION_TEAM_DISABLED");if(!a.scope().permits("team.read",null)&&!a.scope().permits("team.access.manage",null)&&!a.scope().permits("team.membership.manage",null))throw fail("AUTHORITY_REQUIRED");return Set.of(binding(a,memberId).account);}
    private static String permission(String op){return switch(op){case "ACCESS"->"team.access.manage";case "SUSPEND","RESUME","REVOKE_MEMBER"->"team.membership.manage";case "INVITE","RESEND","REVOKE_INVITATION"->"team.invite";default->throw fail("VALIDATION_FAILED");};}
    private UUID org(Access a){return a.scope().context().organizationId();}
    private record Prior(Receipt receipt,byte[] digest){}
    private Optional<Prior> prior(Access a,String op,UUID key){return jdbc.query("SELECT membership_id,invitation_id,outcome,version,input_digest FROM merchant.team_command_receipt WHERE organization_id=? AND actor_account_id=? AND operation=? AND key=?",(r,n)->new Prior(new Receipt(op,key,r.getObject(1,UUID.class)==null?r.getObject(2,UUID.class):r.getObject(1,UUID.class),r.getString(3),r.getLong(4)),r.getBytes(5)),org(a),a.actorAccountId(),op,key).stream().findFirst();}
    private byte[] digest(Access a,String key,String op,UUID target,long version,String payload){return fingerprints.hash(key,"organizationTeam"+op.replace("_",""),org(a),1,a.actorAccountId().toString(),Objects.toString(op.equals("INVITE")?org(a):target,""),Long.toString(version),payload);}
    private void match(Access a,Prior prior,String op,UUID target,long version,String payload){boolean equal=false;for(String key:fingerprints.retainedKeys()){byte[] candidate=digest(a,key,op,target,version,payload);try{equal|=MessageDigest.isEqual(candidate,prior.digest);}finally{Arrays.fill(candidate,(byte)0);}}if(!equal)throw fail("IDEMPOTENCY_CONFLICT");}
    private void evidence(Access a,String op,UUID key,UUID target,boolean invitation,long version,String payload,long expected,String reason,Instant now){
        byte[] hash=digest(a,fingerprints.activeKey(),op,target,expected,payload);try{
            jdbc.update("INSERT INTO merchant.team_command_receipt VALUES(?,?,?,?,?,?,?,'COMMITTED',?,?)",org(a),a.actorAccountId(),op,key,hash,invitation?null:target,invitation?target:null,version,Timestamp.from(now));
            jdbc.update("INSERT INTO merchant.team_audit VALUES(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),org(a),a.actorAccountId(),a.scope().context().membershipId(),invitation?null:target,invitation?target:null,op,reason,key,Timestamp.from(now));
        }finally{Arrays.fill(hash,(byte)0);}a.meaningfulAction();
    }
    private Binding administrativeTarget(Access a,UUID target,String op){
        permit(a,permission(op));if(!a.scope().context().role().equals("OWNER"))throw fail("TEAM_DELEGATION_DENIED");var before=binding(a,target);
        if(before.role.equals("OWNER"))throw fail("LAST_OWNER_PROTECTED");if(before.account.equals(a.actorAccountId()))throw fail("TEAM_DELEGATION_DENIED");return before;
    }
    @Override public MemberDetail updateAccess(Access a,UUID id,AccessInput in,long expected,UUID key){
        var before=administrativeTarget(a,id,"ACCESS");if(in==null||in.reauthenticationProofId()==null||key==null)throw fail("VALIDATION_FAILED");String reason=OrganizationTeamPolicy.reason(in.reason());var grants=OrganizationTeamPolicy.grants(in.grants());
        String payload=JSON.writeValueAsString(Arrays.asList(in.role(),grants,reason,in.reauthenticationProofId()));var prior=prior(a,"ACCESS",key);
        if(prior.isPresent()){match(a,prior.get(),"ACCESS",id,expected,payload);return detail(a,id);}
        if(before.version!=expected)throw fail("VERSION_MISMATCH");OrganizationTeamPolicy.transition(before.role,before.state,"ACCESS");
        if(!a.eligibleAccount(before.account))throw fail("TEAM_STATE_CONFLICT");
        var previous=grants(a,id);if(previous.stream().anyMatch(g->!OrganizationTeamPolicy.delegable(before.role).contains(g.permission())))throw fail("TEAM_STATE_CONFLICT");
        OrganizationTeamPolicy.delegation(a.scope().context().role(),before.role,previous,(p,f)->a.scope().permits(p,f));
        OrganizationTeamPolicy.delegation(a.scope().context().role(),in.role(),grants,(p,f)->a.scope().permits(p,f));grants.forEach(g->a.scope().requireActiveFacility(g.facilityId()));
        a.reauthenticate(in.reauthenticationProofId());Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        evidence(a,"ACCESS",key,id,false,expected+1,payload,expected,reason,now);
        jdbc.update("UPDATE merchant.membership SET role=?,authority_version=authority_version+1,version=version+1,updated_at=? WHERE id=? AND organization_id=? AND version=?",in.role(),Timestamp.from(now),id,org(a),expected);
        jdbc.update("DELETE FROM merchant.member_grant WHERE membership_id=? AND organization_id=?",id,org(a));
        for(var g:grants)jdbc.update("INSERT INTO merchant.member_grant VALUES(?,?,?,?,?,?)",UUID.randomUUID(),id,org(a),g.permission(),g.scope(),g.facilityId());
        return detail(a,id);
    }
    @Override public MemberDetail lifecycle(Access a,UUID id,String op,SensitiveReason in,long expected,UUID key){
        var before=administrativeTarget(a,id,op);if(in==null||in.reauthenticationProofId()==null||key==null)throw fail("VALIDATION_FAILED");String reason=OrganizationTeamPolicy.reason(in.reason());String payload=JSON.writeValueAsString(Arrays.asList(reason,in.reauthenticationProofId()));var prior=prior(a,op,key);
        if(prior.isPresent()){match(a,prior.get(),op,id,expected,payload);return detail(a,id);}
        if(before.version!=expected)throw fail("VERSION_MISMATCH");OrganizationTeamPolicy.transition(before.role,before.state,op);
        if(op.equals("RESUME")&&!a.eligibleAccount(before.account))throw fail("TEAM_STATE_CONFLICT");
        a.reauthenticate(in.reauthenticationProofId());Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);String state=switch(op){case "SUSPEND"->"SUSPENDED";case "RESUME"->"ACTIVE";case "REVOKE_MEMBER"->"REVOKED";default->throw fail("VALIDATION_FAILED");};
        evidence(a,op,key,id,false,expected+1,payload,expected,reason,now);
        jdbc.update("UPDATE merchant.membership SET state=?,authority_version=authority_version+1,version=version+1,updated_at=? WHERE id=? AND organization_id=? AND version=?",state,Timestamp.from(now),id,org(a),expected);
        if(op.equals("REVOKE_MEMBER"))jdbc.update("DELETE FROM merchant.member_grant WHERE membership_id=? AND organization_id=?",id,org(a));
        return detail(a,id);
    }
    @Override public Receipt receipt(Access a,String op,UUID key){permit(a,permission(op));if(Set.of("ACCESS","SUSPEND","RESUME","REVOKE_MEMBER").contains(op)&&!a.scope().context().role().equals("OWNER"))throw fail("TEAM_DELEGATION_DENIED");return prior(a,op,key).map(Prior::receipt).orElseThrow(()->fail("RESOURCE_NOT_FOUND"));}
    @Override public AuditPage audit(Access a,AuditSearch q){
        permit(a,"team.audit.read");if(q==null||q.page()<0||q.page()>1000||q.size()<1||q.size()>50||q.from()==null||q.to()==null||!q.to().isAfter(q.from())||Duration.between(q.from(),q.to()).compareTo(Duration.ofDays(90))>0)throw fail("VALIDATION_FAILED");
        if(q.operation()!=null&&!Set.of("INVITE","RESEND","REVOKE_INVITATION","ACCESS","SUSPEND","RESUME","REVOKE_MEMBER","ACCEPT","DECLINE").contains(q.operation()))throw fail("VALIDATION_FAILED");if(q.membershipId()!=null)binding(a,q.membershipId());
        var rows=jdbc.query("SELECT id,actor_membership_id,target_membership_id,operation,reason,occurred_at FROM merchant.team_audit WHERE organization_id=? AND occurred_at>=? AND occurred_at<? AND (?::uuid IS NULL OR target_membership_id=?) AND (?::text IS NULL OR operation=?) ORDER BY occurred_at DESC,id LIMIT ? OFFSET ?",
          (r,n)->new Audit(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getString(4),r.getString(5),r.getTimestamp(6).toInstant()),org(a),Timestamp.from(q.from()),Timestamp.from(q.to()),q.membershipId(),q.membershipId(),q.operation(),q.operation(),q.size()+1,(long)q.page()*q.size());
        return new AuditPage(rows.stream().limit(q.size()).toList(),q.page(),q.size(),rows.size()>q.size());
    }
    private record InviteRow(UUID id,UUID inviter,long authority,long policy,byte[] contact,String role,String state,long version,long proofVersion,Instant created,Instant updated,Instant expires){}
    private List<InviteRow> invitationRows(Access a,UUID id){return jdbc.query("SELECT id,inviter_membership_id,inviter_authority_version,policy_version,protected_recipient,role,state,version,proof_version,created_at,updated_at,expires_at FROM merchant.team_invitation WHERE organization_id=? AND (?::uuid IS NULL OR id=?) ORDER BY created_at DESC,id LIMIT 50051",
      (r,n)->new InviteRow(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getLong(3),r.getLong(4),r.getBytes(5),r.getString(6),r.getString(7),r.getLong(8),r.getLong(9),r.getTimestamp(10).toInstant(),r.getTimestamp(11).toInstant(),r.getTimestamp(12).toInstant()),org(a),id,id);}
    private List<Grant> invitationGrants(UUID id){return jdbc.query("SELECT permission,scope,facility_id FROM merchant.team_invitation_grant WHERE invitation_id=? ORDER BY permission,scope,facility_id NULLS FIRST",(r,n)->new Grant(r.getString(1),r.getString(2),r.getObject(3,UUID.class)),id);}
    private void invitationAuthority(Access a,InviteRow row){permit(a,"team.invite");String role=a.scope().context().role();if(!role.equals("OWNER")&&!(role.equals("MANAGER")&&row.inviter.equals(a.scope().context().membershipId())&&row.role.equals("OPERATOR")))throw fail("RESOURCE_NOT_FOUND");}
    private InviteRow invitation(Access a,UUID id){var found=invitationRows(a,id);if(found.size()!=1)throw fail("RESOURCE_NOT_FOUND");var row=found.getFirst();invitationAuthority(a,row);return row;}
    @Override public Set<UUID> invitationReferences(Access a,UUID id){permit(a,"team.invite");var row=invitation(a,id);return Set.of(binding(a,row.inviter).account);}
    private Invitation view(Access a,InviteRow row){invitationAuthority(a,row);return new Invitation(row.id,a.maskedRecipient(row.id,row.contact),row.role,invitationGrants(row.id),row.state,row.version,row.expires,a.deliveryState(row.id,row.proofVersion),row.created);}
    private long policy(Access a){var rows=jdbc.query("SELECT version FROM merchant.team_policy WHERE organization_id=?",(r,n)->r.getLong(1),org(a));if(rows.size()!=1)throw fail("SERVICE_UNAVAILABLE");return rows.getFirst();}
    private byte[] token(){byte[] entropy=new byte[32];try{RANDOM.nextBytes(entropy);return Base64.getUrlEncoder().withoutPadding().encode(entropy);}finally{Arrays.fill(entropy,(byte)0);}}
    @Override public Invitation invite(Access a,InvitationInput in,UUID key){
        permit(a,"team.invite");if(in==null||key==null||in.email()==null)throw fail("VALIDATION_FAILED");String reason=OrganizationTeamPolicy.reason(in.reason());var grants=OrganizationTeamPolicy.grants(in.grants());
        String payload=JSON.writeValueAsString(Arrays.asList(in.email(),in.role(),grants,reason));var previous=prior(a,"INVITE",key);
        if(previous.isPresent()){match(a,previous.get(),"INVITE",org(a),0,payload);return view(a,invitation(a,previous.get().receipt.targetId()));}
        OrganizationTeamPolicy.delegation(a.scope().context().role(),in.role(),grants,(p,f)->a.scope().permits(p,f));grants.forEach(g->a.scope().requireActiveFacility(g.facilityId()));
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);byte[] digest=a.recipientDigest(in.email());
        try{var existing=jdbc.query("SELECT id,expires_at FROM merchant.team_invitation WHERE organization_id=? AND recipient_digest=? AND state='PENDING'",(r,n)->Map.entry(r.getObject(1,UUID.class),r.getTimestamp(2).toInstant()),org(a),digest);
            for(var old:existing){if(now.isBefore(old.getValue()))throw fail("TEAM_STATE_CONFLICT");a.revokeInvitationProof(old.getKey());jdbc.update("UPDATE merchant.team_invitation SET state='EXPIRED',version=version+1,updated_at=? WHERE id=?",Timestamp.from(now),old.getKey());}
        }finally{Arrays.fill(digest,(byte)0);}
        UUID id=UUID.randomUUID();Instant expires=now.plusSeconds(259200);byte[] proof=token();
        try{var sealed=a.enqueue(id,1,in.email(),proof,expires);
            jdbc.update("INSERT INTO merchant.team_invitation VALUES(?,?,?,?,?,?,?,?, 'PENDING',?,?,?,NULL,1,?,?)",id,org(a),a.scope().context().membershipId(),a.scope().context().authorityVersion(),policy(a),sealed.recipientDigest(),sealed.protectedRecipient(),in.role(),sealed.proofDigest(),1,Timestamp.from(expires),Timestamp.from(now),Timestamp.from(now));
            for(var g:grants)jdbc.update("INSERT INTO merchant.team_invitation_grant VALUES(?,?,?,?,?,?)",UUID.randomUUID(),id,org(a),g.permission(),g.scope(),g.facilityId());
            evidence(a,"INVITE",key,id,true,1,payload,0,reason,now);return view(a,invitation(a,id));
        }finally{Arrays.fill(proof,(byte)0);}
    }
    @Override public InvitationPage invitations(Access a,InvitationSearch q){
        permit(a,"team.invite");if(q==null||q.page()<0||q.page()>1000||q.size()<1||q.size()>50||q.state()!=null&&!Set.of("PENDING","ACCEPTED","DECLINED","REVOKED","EXPIRED","INVALIDATED").contains(q.state()))throw fail("VALIDATION_FAILED");
        var context=a.scope().context();if(!Set.of("OWNER","MANAGER").contains(context.role()))throw fail("AUTHORITY_REQUIRED");
        var rows=invitationRows(a,null);if(rows.size()>50050)throw fail("SERVICE_UNAVAILABLE");var visible=rows.stream().filter(i->context.role().equals("OWNER")||i.inviter.equals(context.membershipId())&&i.role.equals("OPERATOR"))
          .filter(i->q.state()==null||i.state.equals(q.state())).skip((long)q.page()*q.size()).limit(q.size()+1L).toList();
        return new InvitationPage(visible.stream().limit(q.size()).map(i->view(a,i)).toList(),q.page(),q.size(),visible.size()>q.size());
    }
    @Override public Invitation resend(Access a,UUID id,Reason in,long expected,UUID key){return invitationCommand(a,id,in,expected,key,true);}
    @Override public Invitation revoke(Access a,UUID id,Reason in,long expected,UUID key){return invitationCommand(a,id,in,expected,key,false);}
    private Invitation invitationCommand(Access a,UUID id,Reason in,long expected,UUID key,boolean resend){
        permit(a,"team.invite");if(in==null||key==null)throw fail("VALIDATION_FAILED");String reason=OrganizationTeamPolicy.reason(in.reason()),op=resend?"RESEND":"REVOKE_INVITATION";var row=invitation(a,id);String payload=JSON.writeValueAsString(List.of(reason));var previous=prior(a,op,key);
        if(previous.isPresent()){match(a,previous.get(),op,id,expected,payload);return view(a,row);}if(row.version!=expected)throw fail("VERSION_MISMATCH");
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);if(!row.state.equals("PENDING")||!now.isBefore(row.expires)||row.authority!=binding(a,row.inviter).authority||row.policy!=policy(a))throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");
        var grants=invitationGrants(id);OrganizationTeamPolicy.delegation(a.scope().context().role(),row.role,grants,(p,f)->a.scope().permits(p,f));
        if(resend){byte[] proof=token();try{
            // The sealed contact is never exposed to Merchant. Identity resolves it under current admission.
            var sealed=a.reenqueue(binding(a,row.inviter).account,id,row.proofVersion+1,row.contact,proof,now.plusSeconds(259200));
            jdbc.update("UPDATE merchant.team_invitation SET proof_digest=?,proof_version=proof_version+1,expires_at=?,version=version+1,updated_at=? WHERE id=? AND organization_id=?",sealed.proofDigest(),Timestamp.from(now.plusSeconds(259200)),Timestamp.from(now),id,org(a));
        }finally{Arrays.fill(proof,(byte)0);}}
        else{a.revokeInvitationProof(id);jdbc.update("UPDATE merchant.team_invitation SET state='REVOKED',version=version+1,updated_at=? WHERE id=? AND organization_id=?",Timestamp.from(now),id,org(a));}
        evidence(a,op,key,id,true,expected+1,payload,expected,reason,now);return view(a,invitation(a,id));
    }
    private void recipientAdmission(RecipientAccess a){a.requireCurrent();if(!enabled)throw fail("ORGANIZATION_TEAM_DISABLED");if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw new IllegalStateException("TEAM_ADMISSION_REQUIRED");jdbc.query("SELECT pg_advisory_xact_lock(730023,1)",(r,n)->0);}
    private record DecisionRow(UUID org,UUID inviter,UUID owner,long authority,long currentAuthority,long policy,long currentPolicy,String state,String role,long version,long proofVersion,Instant expires,byte[] contact,UUID accepted){}
    private DecisionRow decisionRow(RecipientAccess a,UUID id){
        recipientAdmission(a);var rows=jdbc.query("SELECT i.organization_id,m.account_id,owner.account_id,i.inviter_authority_version,m.authority_version,i.policy_version,p.version,i.state,i.role,i.version,i.proof_version,i.expires_at,i.protected_recipient,i.accepted_membership_id FROM merchant.team_invitation i JOIN merchant.organization o ON o.id=i.organization_id JOIN merchant.membership owner ON owner.id=o.initial_owner_membership_id AND owner.organization_id=o.id JOIN merchant.membership m ON m.id=i.inviter_membership_id AND m.organization_id=o.id JOIN merchant.team_policy p ON p.organization_id=o.id WHERE i.id=? AND o.state='ACTIVE' AND owner.state='ACTIVE' AND owner.role='OWNER' AND o.environment IN ('LOCAL','TEST') AND "+originPredicate()+"",
          (r,n)->new DecisionRow(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getObject(3,UUID.class),r.getLong(4),r.getLong(5),r.getLong(6),r.getLong(7),r.getString(8),r.getString(9),r.getLong(10),r.getLong(11),r.getTimestamp(12).toInstant(),r.getBytes(13),r.getObject(14,UUID.class)),id);
        if(rows.size()!=1||!a.matchesRecipient(rows.getFirst().org,id,rows.getFirst().contact))throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");return rows.getFirst();
    }
    @Override public RecipientReferences recipientReferences(RecipientAccess a,UUID id){var row=decisionRow(a,id);return new RecipientReferences(row.org,new HashSet<>(List.of(row.inviter,row.owner)));}
    private byte[] decisionDigest(RecipientAccess a,UUID org,String kid,String op,UUID id,String token){return fingerprints.hash(kid,"organizationTeam"+op,org,1,a.actorAccountId().toString(),id.toString(),"0",token);}
    @Override public DecisionReceipt decide(RecipientAccess a,UUID id,boolean accept,InvitationDecision input,UUID key){
        var row=decisionRow(a,id);if(input==null||input.proof()==null||!input.proof().matches("[A-Za-z0-9_-]{43}")||key==null)throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");
        if(!a.eligibleAccount(row.owner))throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");String op=accept?"ACCEPT":"DECLINE";
        var prior=jdbc.query("SELECT invitation_id,input_digest,outcome FROM merchant.team_command_receipt WHERE organization_id=? AND actor_account_id=? AND operation=? AND key=?",(r,n)->new Object[]{r.getObject(1,UUID.class),r.getBytes(2),r.getString(3)},row.org,a.actorAccountId(),op,key);
        if(!prior.isEmpty()){var previous=prior.getFirst();boolean equal=false;for(String kid:fingerprints.retainedKeys()){byte[] hash=decisionDigest(a,row.org,kid,op,id,input.proof());try{equal|=MessageDigest.isEqual(hash,(byte[])previous[1]);}finally{Arrays.fill(hash,(byte)0);}}if(!equal||!id.equals(previous[0]))throw fail("IDEMPOTENCY_CONFLICT");return new DecisionReceipt(id,(String)previous[2],accept?row.accepted:null);}
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        if(!row.state.equals("PENDING")||!now.isBefore(row.expires)||row.authority!=row.currentAuthority||row.policy!=row.currentPolicy||!a.eligibleAccount(row.inviter)
          ||jdbc.queryForObject("SELECT merchant.team_permission(?,?, 'team.invite')",Boolean.class,row.org,row.inviter)!=Boolean.TRUE)throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");
        var grants=invitationGrants(id);for(var g:grants){if(jdbc.queryForObject("SELECT merchant.team_can_delegate(?,?,?,?,?)",Boolean.class,row.org,row.inviter,row.role,g.permission(),g.facilityId())!=Boolean.TRUE
          ||g.facilityId()!=null&&jdbc.queryForObject("SELECT count(*) FROM merchant.facility WHERE id=? AND organization_id=? AND state='ACTIVE'",Integer.class,g.facilityId(),row.org)!=1)throw fail("TEAM_INVITATION_INVALID_OR_EXPIRED");}
        var current=jdbc.query("SELECT id FROM merchant.membership WHERE organization_id=? AND account_id=?",(r,n)->r.getObject(1,UUID.class),row.org,a.actorAccountId());
        if(accept&&!current.isEmpty())throw fail("MEMBERSHIP_ALREADY_EXISTS");
        byte[] token=input.proof().getBytes(java.nio.charset.StandardCharsets.US_ASCII);try{a.consume(row.org,id,row.proofVersion,token);}finally{Arrays.fill(token,(byte)0);}
        // Identity consumption happens first. With a real clock its timestamp
        // is later than the precondition check, unlike deterministic fixtures.
        // Keep the DB evidence ordering exact; never relax the proof guard.
        now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        UUID membership=accept?UUID.randomUUID():current.stream().findFirst().orElse(null);
        if(accept){jdbc.update("INSERT INTO merchant.membership VALUES(?,?,?,?,'ACTIVE',1,1,?,?)",membership,row.org,a.actorAccountId(),row.role,Timestamp.from(now),Timestamp.from(now));
            for(var g:grants)jdbc.update("INSERT INTO merchant.member_grant VALUES(?,?,?,?,?,?)",UUID.randomUUID(),membership,row.org,g.permission(),g.scope(),g.facilityId());}
        jdbc.update("UPDATE merchant.team_invitation SET state=?,accepted_membership_id=?,version=version+1,updated_at=? WHERE id=? AND organization_id=?",accept?"ACCEPTED":"DECLINED",accept?membership:null,Timestamp.from(now),id,row.org);
        byte[] digest=decisionDigest(a,row.org,fingerprints.activeKey(),op,id,input.proof());try{
            jdbc.update("INSERT INTO merchant.team_command_receipt VALUES(?,?,?,?,?,NULL,?,?,?,?)",row.org,a.actorAccountId(),op,key,digest,id,accept?"ACCEPTED":"DECLINED",row.version+1,Timestamp.from(now));
            jdbc.update("INSERT INTO merchant.team_audit VALUES(?,?,?,?,?,?,?, ?,?,?)",UUID.randomUUID(),row.org,a.actorAccountId(),membership,accept?membership:null,id,op,accept?"Recipient accepted invitation":"Recipient declined invitation",key,Timestamp.from(now));
        }finally{Arrays.fill(digest,(byte)0);}a.meaningfulAction();return new DecisionReceipt(id,accept?"ACCEPTED":"DECLINED",accept?membership:null);
    }
}
