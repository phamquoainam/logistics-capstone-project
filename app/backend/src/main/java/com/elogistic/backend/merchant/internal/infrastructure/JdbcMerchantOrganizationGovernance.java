package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** Scoped Organization governance; never grants Merchant/Carrier/Finance authority. */
final class JdbcMerchantOrganizationGovernance {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final JdbcTemplate jdbc;private final JdbcMerchantGovernance scopes;private final MerchantFingerprints fingerprints;
    private final ProtectedGovernancePayloads payloads;private final AdminGovernanceReauthentication proofs;private final Clock clock;
    JdbcMerchantOrganizationGovernance(DataSource source,JdbcMerchantGovernance scopes,MerchantFingerprints fingerprints,ProtectedGovernancePayloads payloads,AdminGovernanceReauthentication proofs,Clock clock){jdbc=new JdbcTemplate(source);this.scopes=scopes;this.fingerprints=fingerprints;this.payloads=payloads;this.proofs=proofs;this.clock=clock;}
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    private static Target target(UUID id){if(id==null||id.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);return new Target(Kind.ORGANIZATION,id);}
    OrganizationSummaryPage organizations(AdminReadContext actor,OrganizationQuery query){
        var context=scopes.context(actor);actor.require(AdminReadContext.MERCHANT);
        if(query==null||query.page()<0||query.page()>10000||query.size()<1||query.size()>50||query.state()!=null&&!Set.of("ACTIVE","SUSPENDED").contains(query.state()))throw fail(VALIDATION_FAILED);
        var assignments=context.assignments().stream().filter(s->s.permissions().contains("org.read")).toList();
        var policies=assignments.stream().filter(s->s.scopeType().equals("POLICY")).map(GovernanceAssignment::policyId).distinct().toList();
        var organizations=assignments.stream().filter(s->s.scopeType().equals("ORGANIZATION")).map(GovernanceAssignment::organizationId).distinct().toList();
        if(query.policyId()!=null&&!policies.contains(query.policyId())&&!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_activation WHERE policy_id=? AND organization_id=ANY(?::uuid[]))",Boolean.class,query.policyId(),organizations.toArray(UUID[]::new))))throw fail(RESOURCE_NOT_FOUND);
        if(policies.isEmpty()&&organizations.isEmpty())return new OrganizationSummaryPage(List.of(),query.page(),query.size(),false);
        String where="o.environment=? AND (o.id=ANY(?::uuid[]) OR (x.policy_id=ANY(?::uuid[]) AND merchant.application_origin_admitted(o.id)))";
        List<Object> args=new ArrayList<>();args.add(actor.environment());args.add(organizations.toArray(UUID[]::new));args.add(policies.toArray(UUID[]::new));
        if(query.state()!=null){where+=" AND o.state=?";args.add(query.state());}if(query.policyId()!=null){where+=" AND x.policy_id=?";args.add(query.policyId());}
        args.add(query.size()+1);args.add((long)query.page()*query.size());
        var ids=jdbc.query("SELECT o.id FROM merchant.organization o LEFT JOIN merchant.application_activation x ON x.organization_id=o.id WHERE "+where+" ORDER BY o.id LIMIT ? OFFSET ?",(r,n)->r.getObject(1,UUID.class),args.toArray());
        List<OrganizationSummary> items=new ArrayList<>();for(UUID id:ids.stream().limit(query.size()).toList())items.add(organization(actor,id));return new OrganizationSummaryPage(items,query.page(),query.size(),ids.size()>query.size());
    }
    OrganizationSummary organization(AdminReadContext actor,UUID id){Target target=target(id);return scopes.withScope(actor,target,scope->{scope.require("org.read",target);return summary(id,scope,false);});}
    AdminCommandAdmission.ReferencedSubjects references(AdminReadContext actor,UUID id){Target target=target(id);return scopes.withScope(actor,target,scope->{scope.require("org.read",target);return new AdminCommandAdmission.ReferencedSubjects(Set.of(owner(id)),Set.of());});}
    private UUID owner(UUID id){return jdbc.query("SELECT m.account_id FROM merchant.organization o JOIN merchant.membership m ON m.id=o.initial_owner_membership_id AND m.organization_id=o.id WHERE o.id=?",(r,n)->r.getObject(1,UUID.class),id).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));}
    private ActivationOutcome activation(UUID org){return jdbc.query("SELECT * FROM merchant.application_activation WHERE organization_id=?",(r,n)->new ActivationOutcome(r.getObject("application_id",UUID.class),r.getLong("revision"),r.getObject("decision_event_id",UUID.class),org,r.getObject("owner_membership_id",UUID.class),"ACTIVE",r.getTimestamp("activated_at").toInstant()),org).stream().findFirst().orElse(null);}
    private OrganizationSummary summary(UUID id,GovernanceScope scope,boolean frozen){
        var values=jdbc.queryForList("SELECT * FROM merchant.organization WHERE id=?",id);if(values.size()!=1)throw fail(RESOURCE_NOT_FOUND);var row=values.getFirst();
        var counts=new TreeMap<String,Map<String,Long>>();jdbc.query("SELECT role,state,count(*) AS amount FROM merchant.membership WHERE organization_id=? GROUP BY role,state ORDER BY role,state",r->{counts.computeIfAbsent(r.getString("role"),ignored->new TreeMap<>()).put(r.getString("state"),r.getLong("amount"));},id);
        var actions=new ArrayList<String>();if(!frozen&&"APPLICATION_APPROVED".equals(row.get("admission_origin"))){String action="ACTIVE".equals(row.get("state"))?"SUSPEND":"REINSTATE";try{scope.require(action.equals("SUSPEND")?"org.suspend":"org.reinstate",target(id));actions.add(action);}catch(GovernanceFailure denied){if(denied.code()!=ACCESS_DENIED)throw denied;}}
        return new OrganizationSummary(id,(String)row.get("display_name"),(String)row.get("state"),(String)row.get("admission_origin"),((Number)row.get("version")).longValue(),(UUID)row.get("initial_owner_membership_id"),activation(id),counts,actions);
    }
    private String body(Object value){return JSON.writeValueAsString(value);}
    private String canonical(LifecycleInput input){return body(new TreeMap<>(Map.of("reason",input.reason(),"reauthenticationProofId",input.reauthenticationProofId())));}
    private byte[] digest(String kid,AdminCommandContext actor,String op,UUID id,long expected,UUID key,String input){return fingerprints.hash(kid,"merchantGovernanceCommand",id,expected,actor.accountId().toString(),actor.realmId().toString(),op,key.toString(),input);}
    OrganizationSummary transition(AdminCommandContext actor,UUID id,LifecycleInput input,long expected,UUID key,boolean suspend){
        Target target=target(id);String op=suspend?"SUSPEND":"REINSTATE";
        if(input==null||input.reason()==null||input.reason().isBlank()||input.reason().codePointCount(0,input.reason().length())>300||input.reason().codePoints().anyMatch(c->Character.isISOControl(c)||c==0x2028||c==0x2029)||input.reauthenticationProofId()==null||key==null||key.equals(new UUID(0,0))||expected<1)throw fail(VALIDATION_FAILED);
        return scopes.withScope(actor,target,scope->{
            scope.require(suspend?"org.suspend":"org.reinstate",target);String canonical=canonical(input);
            var saved=jdbc.queryForList("SELECT r.*,e.protected_payload FROM merchant.governance_receipt r JOIN merchant.governance_event e ON e.id=r.event_id WHERE r.actor_kind='SYSTEM' AND r.actor_binding=? AND r.realm_id=? AND r.operation=? AND r.operation_key=? AND r.target_type='ORGANIZATION'",actor.accountId().toString(),actor.realmId(),op,key);
            if(!saved.isEmpty()){
                var r=saved.getFirst();byte[] hash=digest((String)r.get("input_key_id"),actor,op,id,expected,key,canonical);
                try{if(!id.equals(r.get("target_id"))||((Number)r.get("original_version")).longValue()!=expected||!MessageDigest.isEqual(hash,(byte[])r.get("input_digest")))throw fail(IDEMPOTENCY_CONFLICT);}finally{Arrays.fill(hash,(byte)0);}
                var frozen=JSON.readTree(payloads.open((UUID)r.get("event_id"),id,0,op,(byte[])r.get("protected_payload"))).get("outcome");return JSON.readValue(frozen.toString(),OrganizationSummary.class);
            }
            var rows=jdbc.queryForList("SELECT o.*,x.policy_id FROM merchant.organization o LEFT JOIN merchant.application_activation x ON x.organization_id=o.id WHERE o.id=? FOR UPDATE OF o",id);
            if(rows.size()!=1)throw fail(RESOURCE_NOT_FOUND);var row=rows.getFirst();if(!"APPLICATION_APPROVED".equals(row.get("admission_origin"))||row.get("policy_id")==null)throw fail(STATE_CONFLICT);
            if(((Number)row.get("version")).longValue()!=expected)throw fail(PRECONDITION_STALE);if(!(suspend?"ACTIVE":"SUSPENDED").equals(row.get("state")))throw fail(STATE_CONFLICT);
            UUID owner=owner(id);if(owner.equals(actor.accountId())||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.membership WHERE organization_id=? AND account_id=? AND state<>'REVOKED')",Boolean.class,id,actor.accountId())))throw fail(ACCESS_DENIED);
            if(!suspend&&(!actor.customer(owner).eligible()||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.membership WHERE id=? AND organization_id=? AND role='OWNER' AND state='ACTIVE')",Boolean.class,row.get("initial_owner_membership_id"),id))))throw fail(STATE_CONFLICT);
            byte[] commandHash;try{commandHash=MessageDigest.getInstance("SHA-256").digest(body(new TreeMap<>(Map.of("operation",op,"originalVersion",expected,"reason",input.reason(),"target",id.toString()))).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.security.NoSuchAlgorithmException unavailable){throw fail(SERVICE_UNAVAILABLE);}
            try{
                proofs.consume(actor,input.reauthenticationProofId(),scope.version(),op,id,expected,commandHash);
                String previous=jdbc.queryForObject("SELECT current_setting('argopost.governance_stepup',true)",String.class);
                jdbc.queryForObject("SELECT set_config('argopost.governance_stepup',?,true)",String.class,body(Map.of("proofId",input.reauthenticationProofId().toString(),"authorityVersion",scope.version(),"inputDigest",HexFormat.of().formatHex(commandHash))));
                RuntimeException primary=null;
                try{
                    UUID event=UUID.randomUUID();Instant stamp=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                    var before=summary(id,scope,true);var outcome=new OrganizationSummary(before.id(),before.displayName(),suspend?"SUSPENDED":"ACTIVE",before.admissionOrigin(),expected+1,before.initialOwnerMembershipId(),before.activation(),before.membershipCounts(),List.of());
                    byte[] encrypted=payloads.seal(event,id,0,op,body(Map.of("command",JSON.readTree(canonical),"outcome",outcome)));
                    try{jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,realm_id,target_type,target_id,organization_id,policy_id,operation,prior_version,new_version,reason_code,protected_payload,request_id,occurred_at) VALUES (?,'SYSTEM',?,?,'ORGANIZATION',?,?,?,?,?,?,?,?,?,?)",event,actor.accountId(),actor.realmId(),id,id,row.get("policy_id"),op,expected,expected+1,suspend?"ORGANIZATION_SUSPENDED":"ORGANIZATION_REINSTATED",encrypted,actor.requestId(),Timestamp.from(stamp));}finally{Arrays.fill(encrypted,(byte)0);}
                    byte[] hash=digest(fingerprints.activeKey(),actor,op,id,expected,key,canonical);try{jdbc.update("INSERT INTO merchant.governance_receipt VALUES ('SYSTEM',?,?,?,?, 'ORGANIZATION',?,?,?,?,?,?,'COMMITTED',?,?)",actor.accountId().toString(),actor.realmId(),op,key,id,expected,expected+1,fingerprints.activeKey(),hash,event,Timestamp.from(stamp),Timestamp.from(stamp.plus(Duration.ofDays(7))));}finally{Arrays.fill(hash,(byte)0);}
                    if(jdbc.update("UPDATE merchant.organization SET state=?,version=version+1,updated_at=? WHERE id=? AND version=?",outcome.state(),Timestamp.from(stamp),id,expected)!=1)throw fail(PRECONDITION_STALE);
                    scope.require(suspend?"org.suspend":"org.reinstate",target);return outcome;
                }catch(RuntimeException failure){primary=failure;throw failure;}finally{try{jdbc.queryForObject("SELECT set_config('argopost.governance_stepup',?,true)",String.class,previous==null?"":previous);}catch(org.springframework.dao.DataAccessException cleanup){if(primary==null)throw cleanup;primary.addSuppressed(cleanup);}}
            }finally{Arrays.fill(commandHash,(byte)0);}
        });
    }
}
