package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.OrganizationGovernanceCases.*;
import com.elogistic.backend.merchant.api.OrganizationGovernanceCases.Case;
import com.elogistic.backend.merchant.api.MerchantGovernanceViews.PageQuery;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** Merchant-owned Step A. Canonical Identity contexts, existing scope engine; no state execution. */
public final class JdbcOrganizationGovernanceCases implements OrganizationGovernanceCases {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private static final Set<String> TERMINAL=Set.of("CANCELLED","EXPIRED","REJECTED","EXECUTED");
    private static final Set<String> TYPES=Set.of("RESTRICT","RELEASE_RESTRICTION","CLOSE","RISK_HOLD","RELEASE_HOLD","DISPUTE","REVERIFY");
    private static final Set<String> DENIED=Set.of("QUOTE_NEW","BOOKING_CONFIRM","TEAM_DELEGATION","CONTRACT_CHANGE");
    private static final UUID CURSOR_DOMAIN=UUID.fromString("d7d43b10-a5e2-4710-96e1-78093086a274");
    private final DataSource source;private final JdbcTemplate jdbc;private final MerchantGovernance governance;
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final MerchantFingerprints keys;
    private final ProtectedGovernancePayloads protection;private final Clock clock;private final MerchantViewCursor cursors;
    public JdbcOrganizationGovernanceCases(DataSource source,MerchantGovernance governance,AdminAdmission reads,AdminCommandAdmission commands,MerchantFingerprints keys,ProtectedGovernancePayloads protection,Clock clock){
        this.source=source;jdbc=new JdbcTemplate(source);this.governance=governance;this.reads=reads;this.commands=commands;this.keys=keys;this.protection=protection;this.clock=clock;cursors=new MerchantViewCursor(keys,clock);
    }
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    private static UUID id(UUID value){if(value==null||value.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);return value;}
    private static MerchantGovernance.Target target(UUID org){return new MerchantGovernance.Target(MerchantGovernance.Kind.ORGANIZATION,id(org));}
    private void bound(){if(!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw fail(ACCESS_DENIED);}
    private <T>T read(AdminReadContext actor,UUID org,String permission,Function<MerchantGovernance.GovernanceScope,T> work){reads.requireCurrent(actor);bound();return governance.withScope(actor,target(org),scope->{scope.require("org.read",target(org));scope.require(permission,target(org));T result=work.apply(scope);scope.require(permission,target(org));return result;});}
    private <T>T write(AdminCommandContext actor,UUID org,String permission,Function<MerchantGovernance.GovernanceScope,T> work){commands.requireCurrent(actor);bound();return governance.withScope(actor,target(org),scope->{scope.require("org.read",target(org));scope.require(permission,target(org));T result=work.apply(scope);scope.require(permission,target(org));return result;});}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static String json(Object value){return JSON.writeValueAsString(value);}
    private static long num(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static Instant time(Map<String,Object> row,String key){return row.get(key)==null?null:((Timestamp)row.get(key)).toInstant();}
    private static String reason(String text){if(text==null||text.isBlank()||text.codePoints().anyMatch(c->Character.isISOControl(c)||c>=0xd800&&c<=0xdfff||c==0x2028||c==0x2029))throw fail(VALIDATION_FAILED);String n=Normalizer.normalize(text,Normalizer.Form.NFC);if(n.codePointCount(0,n.length())>300)throw fail(VALIDATION_FAILED);return n;}
    private Map<String,Object> row(UUID org,UUID id){return jdbc.queryForList("SELECT * FROM merchant.organization_governance_case WHERE organization_id=? AND id=?",org,id(id)).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));}
    private Map<String,Object> organization(UUID org){return jdbc.queryForList("SELECT o.*,x.membership_id,x.version AS ownership_version,m.account_id AS owner_account FROM merchant.organization o JOIN merchant.organization_current_owner x ON x.organization_id=o.id JOIN merchant.membership m ON m.id=x.membership_id AND m.organization_id=o.id WHERE o.id=? AND m.role='OWNER' AND m.state='ACTIVE'",org).stream().findFirst().orElseThrow(()->fail(STATE_CONFLICT));}
    private Map<String,Object> policy(UUID policy,long version,String environment){return jdbc.queryForList("SELECT * FROM merchant.organization_governance_policy WHERE id=? AND version=? AND environment=? AND state='PUBLISHED' AND valid_from<=? AND (valid_to IS NULL OR valid_to>?)",id(policy),version,environment,Timestamp.from(now()),Timestamp.from(now())).stream().findFirst().orElseThrow(()->fail(POLICY_UNAVAILABLE));}
    private static boolean permits(MerchantGovernance.GovernanceScope scope,String permission,UUID org){try{scope.require(permission,target(org));return true;}catch(GovernanceFailure denied){if(denied.code()!=ACCESS_DENIED)throw denied;return false;}}
    private Map<String,Object> review(UUID caseId){return jdbc.queryForList("SELECT * FROM merchant.organization_case_review WHERE case_id=?",caseId).stream().findFirst().orElse(null);}
    private boolean reviewBegun(Map<String,Object> c,Map<String,Object> r){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.organization_governance_event b JOIN merchant.organization_governance_event a ON a.id=? WHERE b.case_id=? AND b.operation='BEGIN_CASE_REVIEW' AND b.actor_account_id=? AND b.revision=? AND b.new_version>a.new_version)",Boolean.class,r.get("assigned_event_id"),c.get("id"),r.get("reviewer_account_id"),c.get("revision")));}
    private Case view(Map<String,Object> row,UUID actor,MerchantGovernance.GovernanceScope scope){
        UUID org=(UUID)row.get("organization_id");String state=(String)row.get("state");boolean expired=!now().isBefore(time(row,"expires_at"));var review=review((UUID)row.get("id"));List<String> allowed=new ArrayList<>();
        if(!expired&&!TERMINAL.contains(state)){
            boolean maker=actor.equals(row.get("maker_account_id")),propose=permits(scope,"org.lifecycle.propose",org);
            if(maker&&propose){if(state.equals("DRAFT"))allowed.add("SUBMIT");if(state.equals("NEEDS_INFORMATION"))allowed.add("RESUBMIT");if(Set.of("DRAFT","SUBMITTED","NEEDS_INFORMATION").contains(state))allowed.add("CANCEL");}
            if(propose&&Set.of("SUBMITTED","UNDER_REVIEW").contains(state))allowed.add("ASSIGN");
            if(review!=null&&actor.equals(review.get("reviewer_account_id"))&&permits(scope,"org.lifecycle.review",org)&&num(review,"case_revision")==num(row,"revision")){
                if(state.equals("SUBMITTED")||state.equals("UNDER_REVIEW")&&!reviewBegun(row,review))allowed.add("BEGIN");if(state.equals("UNDER_REVIEW")&&reviewBegun(row,review)){allowed.add("REQUEST_INFORMATION");allowed.add("READY_FOR_DECISION");}
            }
        }
        return new Case((UUID)row.get("id"),org,(String)row.get("type"),state,num(row,"version"),(UUID)row.get("policy_id"),num(row,"policy_version"),num(row,"expected_org_version"),(String)row.get("maker_kind"),(UUID)row.get("maker_account_id"),review==null?null:(UUID)review.get("reviewer_account_id"),null,null,time(row,"created_at"),time(row,"expires_at"),time(row,"effective_at"),allowed,(UUID)row.get("last_event_id"),expired,now());
    }
    @Override public Case get(AdminReadContext actor,UUID org,UUID id){return read(actor,org,"org.read",s->view(row(org,id),actor.accountId(),s));}
    @Override public AdminCommandAdmission.ReferencedSubjects subjects(AdminReadContext actor,UUID org,UUID caseId){return read(actor,org,"org.read",s->{
        var o=organization(org);Set<UUID> admins=new HashSet<>();if(caseId!=null){var c=row(org,caseId);admins.add((UUID)c.get("maker_account_id"));var r=review(caseId);if(r!=null)admins.add((UUID)r.get("reviewer_account_id"));}return new AdminCommandAdmission.ReferencedSubjects(Set.of((UUID)o.get("owner_account")),admins);
    });}
    private String hash(String op,String... parts){return HexFormat.of().formatHex(keys.hash(keys.activeKey(),op,CURSOR_DOMAIN,1,parts));}
    private <T>Map.Entry<List<T>,String> page(UUID actor,UUID realm,String authority,List<T> values,PageQuery q,String query,String source){
        int size=q==null||q.size()==null?20:q.size();if(size<1||size>50)throw fail(VALIDATION_FAILED);
        String cursor=q==null?null:q.cursor();int offset=cursors.offset(cursor,actor,realm,authority,query,source,size);if(offset>values.size())throw fail(SNAPSHOT_EXPIRED);
        int end=Math.min(values.size(),offset+size);return new AbstractMap.SimpleImmutableEntry<>(List.copyOf(values.subList(offset,end)),end<values.size()?cursors.advance(cursor,actor,realm,authority,query,source,end,size):null);
    }
    @Override public CasePage list(AdminReadContext actor,UUID org,CaseQuery q){return read(actor,org,"org.read",s->{
        if(q==null||q.type()!=null&&!TYPES.contains(q.type())||q.state()!=null&&!Set.of("DRAFT","SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","READY_FOR_DECISION","APPROVED","REJECTED","EXECUTED","CANCELLED","EXPIRED").contains(q.state()))throw fail(VALIDATION_FAILED);
        var rows=jdbc.queryForList("SELECT c.* FROM merchant.organization_governance_case c LEFT JOIN merchant.organization_case_review r ON r.case_id=c.id WHERE c.organization_id=? AND (?::text IS NULL OR c.type=?) AND (?::text IS NULL OR c.state=?) AND (?=FALSE OR r.reviewer_account_id=?::uuid) ORDER BY c.created_at DESC,c.id DESC LIMIT 10001",org,q.type(),q.type(),q.state(),q.state(),q.assignedToMe(),actor.accountId());
        if(rows.size()>10000)throw fail(SERVICE_UNAVAILABLE);var values=rows.stream().map(c->view(c,actor.accountId(),s)).toList();String capture=hash("organizationCaseSource",s.version(),json(rows.stream().map(r->List.of(r.get("id"),r.get("version"),r.get("last_event_id"))).toList()));
        var page=page(actor.accountId(),actor.realmId(),actor.permissionVersion(),values,q.page(),hash("organizationCaseQuery",org.toString(),String.valueOf(q.type()),String.valueOf(q.state()),String.valueOf(q.assignedToMe())),capture);return new CasePage(page.getKey(),page.getValue());
    });}
    private void independent(AdminCommandContext actor,UUID org){if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.membership WHERE organization_id=? AND account_id=? AND state<>'REVOKED')",Boolean.class,org,actor.accountId())))throw fail(ACCESS_DENIED);}
    private void currentState(AdminCommandContext actor,Map<String,Object> c){var o=organization((UUID)c.get("organization_id"));if(num(o,"version")!=num(c,"expected_org_version")||num(o,"ownership_version")!=num(c,"expected_ownership_version"))throw fail(PRECONDITION_STALE);if(!actor.customer((UUID)o.get("owner_account")).eligible())throw fail(ACCESS_DENIED);}
    private CasePayload payload(String type,JsonNode n){return switch(type){case "RESTRICT"->JSON.treeToValue(n,Restriction.class);case "CLOSE"->JSON.treeToValue(n,Closure.class);case "RISK_HOLD"->JSON.treeToValue(n,RiskHold.class);case "RELEASE_RESTRICTION"->JSON.treeToValue(n,ReleaseRestriction.class);case "RELEASE_HOLD"->JSON.treeToValue(n,ReleaseHold.class);case "DISPUTE"->JSON.treeToValue(n,Dispute.class);case "REVERIFY"->JSON.treeToValue(n,Reverification.class);default->throw fail(VALIDATION_FAILED);};}
    private void validatePayload(String type,CasePayload input){
        if(!TYPES.contains(type)||input==null)throw fail(VALIDATION_FAILED);
        if(Set.of("DISPUTE","REVERIFY","RELEASE_RESTRICTION","RELEASE_HOLD").contains(type))throw fail(POLICY_UNAVAILABLE);
        boolean valid=switch(type){
            case "CLOSE"->input instanceof Closure;
            case "RESTRICT"->input instanceof Restriction r&&r.deniedCapabilities()!=null&&r.deniedCapabilities().size()==4&&new HashSet<>(r.deniedCapabilities()).equals(DENIED)&&r.effectiveAt()!=null&&!r.effectiveAt().isAfter(now())&&(r.expiresAt()==null||r.expiresAt().isAfter(now()));
            case "RISK_HOLD"->input instanceof RiskHold h&&Set.of("BOOKING_HOLD","CONTRACT_CHANGE_HOLD").contains(h.holdCode())&&(h.expiresAt()==null||h.expiresAt().isAfter(now()));
            default->false;
        };if(!valid)throw fail(VALIDATION_FAILED);
    }
    private Map<String,Object> decrypted(Map<String,Object> row){var e=jdbc.queryForMap("SELECT operation FROM merchant.organization_governance_event WHERE id=?",row.get("last_event_id"));return JSON.readValue(protection.open((UUID)row.get("last_event_id"),(UUID)row.get("id"),num(row,"revision"),e.get("operation").toString(),(byte[])row.get("protected_payload")),Map.class);}
    private byte[] digest(String kid,AdminCommandContext actor,String op,UUID org,UUID caseId,long expected,UUID key,Object input){return keys.hash(kid,"organizationGovernanceCommand",caseId==null?org:caseId,Math.max(1,expected),actor.accountId().toString(),actor.realmId().toString(),org.toString(),op,Long.toString(expected),key.toString(),json(input));}
    private Optional<Case> replay(AdminCommandContext actor,UUID org,UUID caseId,String op,long expected,UUID key,Object input){
        var rows=jdbc.queryForList("SELECT r.*,e.revision FROM merchant.organization_governance_receipt r JOIN merchant.organization_governance_event e ON e.id=r.event_id WHERE r.actor_kind='SYSTEM' AND r.actor_binding=? AND r.operation=? AND r.operation_key=?",actor.accountId().toString(),op,id(key));if(rows.isEmpty())return Optional.empty();var r=rows.getFirst();byte[] actual=digest(r.get("input_key_id").toString(),actor,op,org,caseId,expected,key,input);
        try{if(!org.equals(r.get("organization_id"))||!actor.realmId().equals(r.get("realm_id"))||caseId!=null&&!caseId.equals(r.get("case_id"))||!MessageDigest.isEqual(actual,(byte[])r.get("input_digest")))throw fail(IDEMPOTENCY_CONFLICT);
            return Optional.of(JSON.readValue(protection.open((UUID)r.get("event_id"),(UUID)r.get("case_id"),num(r,"revision"),"RECEIPT",(byte[])r.get("protected_outcome")),Case.class));
        }finally{Arrays.fill(actual,(byte)0);}
    }
    private void receipt(AdminCommandContext actor,UUID org,UUID caseId,String op,long expected,UUID key,Object input,UUID event,long revision,UUID policy,Case result){
        byte[] outcome=protection.seal(event,caseId,revision,"RECEIPT",json(result)),hash=digest(keys.activeKey(),actor,op,org,op.equals("CREATE_CASE")?null:caseId,expected,key,input);Instant stamp=now();
        try{jdbc.update("INSERT INTO merchant.organization_governance_receipt VALUES('SYSTEM',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",actor.accountId().toString(),actor.realmId(),op,key,org,caseId,expected,result.version(),keys.activeKey(),hash,event,outcome,Timestamp.from(stamp),Timestamp.from(stamp.plusSeconds(604800)),policy);}
        finally{Arrays.fill(outcome,(byte)0);Arrays.fill(hash,(byte)0);}
    }
    private byte[] event(AdminCommandContext actor,Map<String,Object> c,UUID event,String op,String reason,long prior,long revision,String from,String to,CasePayload payload,Object command){
        byte[] encoded=protection.seal(event,(UUID)c.get("id"),revision,op,json(Map.of("payload",payload,"command",command,"reason",reason,"fromState",from,"toState",to)));
        jdbc.update("INSERT INTO merchant.organization_governance_event VALUES(?,?,?,'SYSTEM',?,?,NULL,?,?,?,?,?,?,?, ?,?,?)",event,c.get("id"),c.get("organization_id"),actor.accountId(),actor.realmId(),op,revision,prior,prior+1,c.get("policy_id"),c.get("policy_version"),reasonCode(op),encoded,actor.requestId(),Timestamp.from(now()));return encoded;
    }
    private static String reasonCode(String op){return switch(op){case "CREATE_CASE"->"CASE_CREATED";case "SUBMIT_CASE"->"CASE_SUBMITTED";case "ASSIGN_CASE_REVIEWER"->"CASE_REVIEWER_ASSIGNED";case "BEGIN_CASE_REVIEW"->"CASE_REVIEW_STARTED";case "READY_CASE_DECISION"->"CASE_CHECKLIST_COMPLETE";case "REQUEST_CASE_INFORMATION"->"CASE_INFORMATION_REQUIRED";case "RESUBMIT_CASE"->"CASE_RESUBMITTED";case "CANCEL_CASE"->"CASE_CANCELLED";case "EXPIRE_CASE"->"CASE_EXPIRED";default->throw fail(VALIDATION_FAILED);};}
    @Override public Case create(AdminCommandContext actor,UUID org,CaseInput input,UUID key){return write(actor,org,"org.lifecycle.propose",s->{
        id(key);if(input==null||input.policyVersion()<1||input.expectedOrganizationVersion()<1)throw fail(VALIDATION_FAILED);reason(input.reason());independent(actor,org);
        policy(input.policyId(),input.policyVersion(),actor.environment());var saved=replay(actor,org,null,"CREATE_CASE",0,key,input);if(saved.isPresent())return saved.get();validatePayload(input.type(),input.payload());var o=organization(org);if(num(o,"version")!=input.expectedOrganizationVersion())throw fail(PRECONDITION_STALE);if(!actor.customer((UUID)o.get("owner_account")).eligible())throw fail(ACCESS_DENIED);
        expireOld(actor,org,s,key);
        Instant stamp=now();UUID caseId=UUID.randomUUID(),event=UUID.randomUUID();var c=new HashMap<String,Object>();c.put("id",caseId);c.put("organization_id",org);c.put("policy_id",input.policyId());c.put("policy_version",input.policyVersion());
        byte[] bytes=event(actor,c,event,"CREATE_CASE",input.reason(),0,1,"NONE","DRAFT",input.payload(),input);
        try{jdbc.update("INSERT INTO merchant.organization_governance_case VALUES(?,?,?,'DRAFT',1,1,?,?,?,?, 'SYSTEM',?,?,?, ?,?,?,?, ?,?)",caseId,org,input.type(),input.policyId(),input.policyVersion(),input.expectedOrganizationVersion(),num(o,"ownership_version"),actor.accountId(),actor.realmId(),bytes,MessageDigest.getInstance("SHA-256").digest(json(input.payload()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),Timestamp.from(stamp.plusSeconds(604800)),input.payload() instanceof Restriction r?Timestamp.from(r.effectiveAt()):null,Timestamp.from(stamp),Timestamp.from(stamp),event);}
        catch(java.security.NoSuchAlgorithmException impossible){throw fail(SERVICE_UNAVAILABLE);}finally{Arrays.fill(bytes,(byte)0);}
        Case result=view(row(org,caseId),actor.accountId(),s);receipt(actor,org,caseId,"CREATE_CASE",0,key,input,event,1,input.policyId(),result);return result;
    });}
    private String reviewerStamp(AdminCommandContext actor,UUID org,UUID reviewer){
        var eligible=actor.admin(reviewer);if(!eligible.eligible()||!actor.realmId().equals(eligible.realmId()))throw fail(ACCESS_DENIED);
        if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.membership WHERE organization_id=? AND account_id=? AND state<>'REVOKED')",Boolean.class,org,reviewer)))throw fail(ACCESS_DENIED);
        var scopes=jdbc.queryForList("SELECT id,version FROM merchant.business_reviewer_scope WHERE account_id=? AND realm_id=? AND organization_id=? AND scope_type='ORGANIZATION' AND state='ACTIVE' AND expires_at>? AND permissions @> ARRAY['org.read','org.lifecycle.review']::text[]",reviewer,actor.realmId(),org,Timestamp.from(now()));if(scopes.size()!=1)throw fail(ACCESS_DENIED);
        return scopes.getFirst().get("id")+":"+scopes.getFirst().get("version")+"|"+eligible.permissionVersion();
    }
    private void expireOld(AdminCommandContext actor,UUID org,MerchantGovernance.GovernanceScope scope,UUID requestKey){
        for(var c:jdbc.queryForList("SELECT * FROM merchant.organization_governance_case WHERE organization_id=? AND state NOT IN ('CANCELLED','EXPIRED','EXECUTED','REJECTED') AND expires_at<=? ORDER BY id",org,Timestamp.from(now()))){
            UUID caseId=(UUID)c.get("id"),key=UUID.nameUUIDFromBytes(("EXPIRE_CASE|"+requestKey+"|"+caseId).getBytes(java.nio.charset.StandardCharsets.UTF_8));var command=new ReasonAction(ReasonTag.CANCEL,new ReasonInput("Expired under authorized same-Organization command"));apply(actor,org,c,command,"EXPIRE_CASE","EXPIRED",key,scope);
        }
    }
    @Override public Case command(AdminCommandContext actor,UUID org,UUID caseId,CaseCommand input,long expected,UUID key){
        String op=switch(input){case AssignmentInput ignored->"ASSIGN_CASE_REVIEWER";case CaseReviewInput q->switch(q.action()){case "BEGIN"->"BEGIN_CASE_REVIEW";case "READY_FOR_DECISION"->"READY_CASE_DECISION";default->throw fail(VALIDATION_FAILED);};case InformationInput ignored->"REQUEST_CASE_INFORMATION";case ResponseInput ignored->"RESUBMIT_CASE";case ReasonAction q->q.action()==ReasonTag.SUBMIT?"SUBMIT_CASE":"CANCEL_CASE";case null->throw fail(VALIDATION_FAILED);};
        String permission=Set.of("BEGIN_CASE_REVIEW","READY_CASE_DECISION","REQUEST_CASE_INFORMATION").contains(op)?"org.lifecycle.review":"org.lifecycle.propose";
        return write(actor,org,permission,s->{
            if(expected<1)throw fail(VALIDATION_FAILED);id(key);independent(actor,org);var c=row(org,caseId);policy((UUID)c.get("policy_id"),num(c,"policy_version"),actor.environment());
            var saved=replay(actor,org,caseId,op,expected,key,input);if(saved.isPresent())return saved.get();if(num(c,"version")!=expected)throw fail(PRECONDITION_STALE);if(!now().isBefore(time(c,"expires_at")))throw fail(STATE_CONFLICT);currentState(actor,c);
            String state=c.get("state").toString(),next=state;boolean maker=actor.accountId().equals(c.get("maker_account_id"));
            if(Set.of("SUBMIT_CASE","CANCEL_CASE","RESUBMIT_CASE").contains(op)&&!maker)throw fail(ACCESS_DENIED);
            switch(op){
                case "SUBMIT_CASE"->{if(!state.equals("DRAFT"))throw fail(STATE_CONFLICT);next="SUBMITTED";}
                case "CANCEL_CASE"->{if(!Set.of("DRAFT","SUBMITTED","NEEDS_INFORMATION").contains(state))throw fail(STATE_CONFLICT);next="CANCELLED";}
                case "ASSIGN_CASE_REVIEWER"->{if(!Set.of("SUBMITTED","UNDER_REVIEW").contains(state))throw fail(STATE_CONFLICT);var q=(AssignmentInput)input;if(q.reviewerAccountId()==null||q.reviewerAccountId().equals(c.get("maker_account_id")))throw fail(ACCESS_DENIED);reviewerStamp(actor,org,q.reviewerAccountId());}
                case "BEGIN_CASE_REVIEW","READY_CASE_DECISION","REQUEST_CASE_INFORMATION"->{var r=review(caseId);if(r==null||!actor.accountId().equals(r.get("reviewer_account_id"))||num(r,"case_revision")!=num(c,"revision")||!reviewerStamp(actor,org,actor.accountId()).equals(r.get("authority_version")))throw fail(ACCESS_DENIED);if(op.equals("BEGIN_CASE_REVIEW")){if(!Set.of("SUBMITTED","UNDER_REVIEW").contains(state)||reviewBegun(c,r))throw fail(STATE_CONFLICT);next="UNDER_REVIEW";}else if(op.equals("READY_CASE_DECISION")){if(!state.equals("UNDER_REVIEW")||!reviewBegun(c,r)||!new Checklist(true,true,true,true).equals(((CaseReviewInput)input).checklist()))throw fail(STATE_CONFLICT);next="READY_FOR_DECISION";}else{if(!Set.of("UNDER_REVIEW","READY_FOR_DECISION").contains(state)||!reviewBegun(c,r))throw fail(STATE_CONFLICT);next="NEEDS_INFORMATION";}}
                case "RESUBMIT_CASE"->{if(!state.equals("NEEDS_INFORMATION"))throw fail(STATE_CONFLICT);var q=(ResponseInput)input;if(q.reauthenticationProofId()!=null||q.evidenceIds()==null||!q.evidenceIds().isEmpty())throw fail(POLICY_UNAVAILABLE);validatePayload(c.get("type").toString(),q.casePayload());next="SUBMITTED";}
                default->throw fail(VALIDATION_FAILED);
            }
            return apply(actor,org,c,input,op,next,key,s);
        });
    }
    private Case apply(AdminCommandContext actor,UUID org,Map<String,Object> c,CaseCommand input,String op,String next,UUID key,MerchantGovernance.GovernanceScope scope){
        String why=switch(input){case AssignmentInput q->q.reason();case CaseReviewInput q->q.reason();case InformationInput q->q.reason();case ResponseInput q->q.reason();case ReasonAction q->q.input().reason();};reason(why);
        if(input instanceof InformationInput q&&(q.fieldCodes()==null||q.fieldCodes().isEmpty()||q.fieldCodes().size()>16||new HashSet<>(q.fieldCodes()).size()!=q.fieldCodes().size()||!Set.of("reason","deniedCapabilities","effectiveAt","expiresAt","holdCode","statement","fieldCodes","evidenceIds").containsAll(q.fieldCodes())))throw fail(VALIDATION_FAILED);
        long prior=num(c,"version"),revision=num(c,"revision")+(op.equals("RESUBMIT_CASE")?1:0);UUID caseId=(UUID)c.get("id"),event=UUID.randomUUID();CasePayload payload=input instanceof ResponseInput q?q.casePayload():payload(c.get("type").toString(),JSON.valueToTree(decrypted(c).get("payload")));byte[] bytes=event(actor,c,event,op,why,prior,revision,c.get("state").toString(),next,payload,input);
        try{byte[] impact=MessageDigest.getInstance("SHA-256").digest(json(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8));jdbc.update("UPDATE merchant.organization_governance_case SET state=?,version=version+1,revision=?,protected_payload=?,impact_digest=?,updated_at=?,last_event_id=? WHERE id=? AND version=?",next,revision,bytes,impact,Timestamp.from(now()),event,caseId,prior);}
        catch(java.security.NoSuchAlgorithmException impossible){throw fail(SERVICE_UNAVAILABLE);}finally{Arrays.fill(bytes,(byte)0);}
        if(input instanceof AssignmentInput q){
            String stamp=reviewerStamp(actor,org,q.reviewerAccountId());jdbc.update("INSERT INTO merchant.organization_case_review VALUES(?,?,?,?,?,?,1,NULL,NULL,?) ON CONFLICT(case_id) DO UPDATE SET reviewer_account_id=EXCLUDED.reviewer_account_id,reviewer_realm_id=EXCLUDED.reviewer_realm_id,authority_version=EXCLUDED.authority_version,case_revision=EXCLUDED.case_revision,version=merchant.organization_case_review.version+1,checklist=NULL,ready_event_id=NULL,assigned_event_id=EXCLUDED.assigned_event_id",caseId,org,q.reviewerAccountId(),actor.realmId(),stamp,revision,event);
        }else if(Set.of("BEGIN_CASE_REVIEW","READY_CASE_DECISION","REQUEST_CASE_INFORMATION","RESUBMIT_CASE").contains(op)){
            Object checklist=op.equals("READY_CASE_DECISION")?json(((CaseReviewInput)input).checklist()):null;
            jdbc.update("UPDATE merchant.organization_case_review SET case_revision=?,version=version+1,checklist=?::jsonb,ready_event_id=? WHERE case_id=?",revision,checklist,op.equals("READY_CASE_DECISION")?event:null,caseId);
        }
        Case result=view(row(org,caseId),actor.accountId(),scope);receipt(actor,org,caseId,op,prior,key,input,event,revision,(UUID)c.get("policy_id"),result);return result;
    }
    @Override public CaseEventPage events(AdminReadContext actor,UUID org,UUID caseId,PageQuery query){return read(actor,org,"audit.read",s->{
        row(org,caseId);var rows=jdbc.queryForList("SELECT * FROM merchant.organization_governance_event WHERE organization_id=? AND case_id=? ORDER BY occurred_at,id LIMIT 10001",org,caseId);if(rows.size()>10000)throw fail(SERVICE_UNAVAILABLE);List<CaseEvent> values=new ArrayList<>();
        for(var e:rows){var p=JSON.readTree(protection.open((UUID)e.get("id"),caseId,num(e,"revision"),e.get("operation").toString(),(byte[])e.get("protected_payload")));values.add(new CaseEvent((UUID)e.get("id"),caseId,org,num(e,"revision"),e.get("actor_kind").toString(),(UUID)e.get("actor_account_id"),e.get("operation").toString(),p.path("fromState").asString(),p.path("toState").asString(),num(e,"prior_version"),num(e,"new_version"),num(e,"policy_version"),e.get("reason_code").toString(),time(e,"occurred_at"),(UUID)e.get("request_id")));}
        var result=page(actor.accountId(),actor.realmId(),actor.permissionVersion(),values,query,hash("organizationCaseQuery","EVENTS",org.toString(),caseId.toString()),hash("organizationCaseSource",s.version(),json(values)));return new CaseEventPage(result.getKey(),result.getValue());
    });}
    @Override public CasePolicies policies(AdminReadContext actor,UUID org){return read(actor,org,"org.read",scope->{
        List<Policy> values=new ArrayList<>();for(var p:jdbc.queryForList("SELECT * FROM merchant.organization_governance_policy WHERE environment=? AND state='PUBLISHED' AND valid_from<=? AND (valid_to IS NULL OR valid_to>?) ORDER BY code,version",actor.environment(),Timestamp.from(now()),Timestamp.from(now()))){
            var parsed=OrganizationCasePolicy.parse(p.get("body").toString());var body=JSON.readTree(p.get("body").toString());var limits=new LinkedHashMap<String,Object>();limits.put("case_ttl_seconds",parsed.caseTtl());limits.put("consent_ttl_seconds",parsed.consentTtl());limits.put("max_future_effect_seconds",parsed.maxFuture());limits.put("retention_policy_reference",null);
            values.add(new Policy((UUID)p.get("id"),p.get("code").toString(),num(p,"version"),parsed.types().stream().sorted().toList(),time(p,"valid_from"),time(p,"valid_to"),limits,List.of("authority","conflictClear","evidenceComplete","impactReviewed"),null,parsed.consentTtl(),JSON.readValue(body.get("capability_policy").toString(),Map.class)));
        }return new CasePolicies(values);
    });}
    @Override public CaseDisclosure disclose(AdminCommandContext actor,UUID org,UUID caseId,CaseDisclosureInput input){return write(actor,org,"org.read",scope->{
        if(input==null||input.expectedCaseVersion()<1||!"LIFECYCLE_REVIEW".equals(input.purpose()))throw fail(VALIDATION_FAILED);
        independent(actor,org);var c=row(org,caseId);if(num(c,"version")!=input.expectedCaseVersion())throw fail(PRECONDITION_STALE);
        boolean maker=actor.accountId().equals(c.get("maker_account_id"))&&permits(scope,"org.lifecycle.propose",org);
        var review=review(caseId);boolean assigned=review!=null&&actor.accountId().equals(review.get("reviewer_account_id"))&&permits(scope,"org.lifecycle.review",org)&&reviewerStamp(actor,org,actor.accountId()).equals(review.get("authority_version"));
        if(!maker&&!assigned)throw fail(ACCESS_DENIED);
        var clear=decrypted(c);CasePayload payload=payload(c.get("type").toString(),JSON.valueToTree(clear.get("payload")));
        String why=reason((String)clear.get("reason"));List<String> fields=List.of();
        if(c.get("state").equals("NEEDS_INFORMATION")){var command=JSON.valueToTree(clear.get("command"));var values=command.get("fieldCodes");if(values==null||!values.isArray()||values.isEmpty()||values.size()>16)throw fail(SERVICE_UNAVAILABLE);List<String> codes=new ArrayList<>();for(var code:values){if(!code.isString()||!Set.of("reason","deniedCapabilities","effectiveAt","expiresAt","holdCode","statement","fieldCodes","evidenceIds").contains(code.asString()))throw fail(SERVICE_UNAVAILABLE);codes.add(code.asString());}fields=List.copyOf(codes);}
        UUID sourceEvent=(UUID)c.get("last_event_id"),audit=UUID.randomUUID();Instant stamp=now();long version=num(c,"version"),revision=num(c,"revision");
        byte[] bytes=protection.seal(audit,caseId,revision,"READ_CASE_DISCLOSURE",json(Map.of("purpose",input.purpose(),"sourceEventId",sourceEvent,"revision",revision,"fromState",c.get("state"),"toState",c.get("state"))));
        try{jdbc.update("INSERT INTO merchant.organization_governance_event VALUES(?,?,?,'SYSTEM',?,?,NULL,'READ_CASE_DISCLOSURE',?,?,?,?,?,'CASE_DISCLOSURE',?,?,?)",audit,caseId,org,actor.accountId(),actor.realmId(),revision,version,version,c.get("policy_id"),c.get("policy_version"),bytes,actor.requestId(),Timestamp.from(stamp));}finally{Arrays.fill(bytes,(byte)0);}
        return new CaseDisclosure(caseId,org,version,revision,payload,why,fields,sourceEvent,stamp);
    });}
}
