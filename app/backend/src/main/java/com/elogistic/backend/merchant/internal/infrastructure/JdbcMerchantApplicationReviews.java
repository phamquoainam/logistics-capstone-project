package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.identity.api.customer.CustomerBusinessContacts;
import com.elogistic.backend.merchant.api.customer.MerchantApplications;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import com.elogistic.backend.merchant.internal.application.port.MerchantFingerprints;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** Genuine revision-bound assignment/review. No decision/activation or Carrier writer. */
public final class JdbcMerchantApplicationReviews implements MerchantGovernance {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    private final JdbcMerchantOrganizationGovernance organizations;private final JdbcMerchantGovernance scopes;private final JdbcTemplate jdbc;private final MerchantFingerprints fingerprints;private final ProtectedGovernancePayloads payloads;private final Clock clock;private final CustomerBusinessContacts contacts;private final AdminGovernanceReauthentication reauthentication;
    private final AdminAdmission reads;private final AdminCommandAdmission commands;private final com.elogistic.backend.platform.api.PrivateEvidenceStore blobs;
    public JdbcMerchantApplicationReviews(DataSource source,MerchantFingerprints fingerprints,ProtectedGovernancePayloads payloads,Clock clock,AdminAdmission reads,AdminCommandAdmission commands,CustomerBusinessContacts contacts,AdminGovernanceReauthentication reauthentication){this(source,fingerprints,payloads,clock,reads,commands,contacts,reauthentication,null);}
    public JdbcMerchantApplicationReviews(DataSource source,MerchantFingerprints fingerprints,ProtectedGovernancePayloads payloads,Clock clock,AdminAdmission reads,AdminCommandAdmission commands,CustomerBusinessContacts contacts,AdminGovernanceReauthentication reauthentication,com.elogistic.backend.platform.api.PrivateEvidenceStore blobs){this.scopes=new JdbcMerchantGovernance(source,clock,reads,commands);jdbc=new JdbcTemplate(source);this.fingerprints=Objects.requireNonNull(fingerprints);this.payloads=Objects.requireNonNull(payloads);this.clock=Objects.requireNonNull(clock);this.contacts=Objects.requireNonNull(contacts);this.reauthentication=Objects.requireNonNull(reauthentication);this.reads=Objects.requireNonNull(reads);this.commands=Objects.requireNonNull(commands);this.blobs=blobs;this.organizations=new JdbcMerchantOrganizationGovernance(source,this.scopes,fingerprints,payloads,reauthentication,clock);}
    @Override public <T>T withScope(AdminReadContext actor,Target target,Function<GovernanceScope,T> work){return scopes.withScope(actor,target,work);}
    @Override public <T>T withScope(AdminCommandContext actor,Target target,Function<GovernanceScope,T> work){return scopes.withScope(actor,target,work);}
    @Override public GovernanceContext context(AdminReadContext actor){return scopes.context(actor);}
    private static void page(int page,int size){if(page<0||page>10000||size<1||size>50)throw fail(VALIDATION_FAILED);}
    @Override public ApplicationSummaryPage applications(AdminReadContext actor,ApplicationQuery query){
        var context=scopes.context(actor);actor.require(AdminReadContext.MERCHANT);
        if(query==null)throw fail(VALIDATION_FAILED);page(query.page(),query.size());
        var today=clock.instant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        if(query.from()==null||query.to()==null||query.from().isAfter(query.to())||query.to().isAfter(today)||java.time.temporal.ChronoUnit.DAYS.between(query.from(),query.to())>=366||query.state()!=null&&!Set.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","APPROVED","REJECTED","WITHDRAWN").contains(query.state()))throw fail(VALIDATION_FAILED);
        var policies=context.assignments().stream().filter(a->a.scopeType().equals("POLICY")&&a.permissions().contains("application.read")).map(GovernanceAssignment::policyId).distinct().toList();
        if(query.policyId()!=null&&!policies.contains(query.policyId()))throw fail(RESOURCE_NOT_FOUND);
        if(policies.isEmpty())return new ApplicationSummaryPage(List.of(),query.page(),query.size(),false);
        List<Object> args=new ArrayList<>(policies);String where="policy_id IN ("+String.join(",",Collections.nCopies(policies.size(),"?"))+") AND first_submitted_at>=? AND first_submitted_at<?";
        ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");args.add(at(query.from().atStartOfDay(zone).toInstant()));args.add(at(query.to().plusDays(1).atStartOfDay(zone).toInstant()));
        if(query.policyId()!=null){where+=" AND policy_id=?";args.add(query.policyId());}
        if(query.state()!=null){where+=" AND state=?";args.add(query.state());}
        if(query.reviewerId()!=null){
            var visible=jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment x JOIN merchant.application a ON a.id=x.application_id WHERE x.reviewer_account_id=? AND a.policy_id IN ("+String.join(",",Collections.nCopies(policies.size(),"?"))+"))",Boolean.class,java.util.stream.Stream.concat(java.util.stream.Stream.of(query.reviewerId()),policies.stream()).toArray());
            if(!Boolean.TRUE.equals(visible))throw fail(RESOURCE_NOT_FOUND);where+=" AND id IN (SELECT application_id FROM merchant.application_reviewer_assignment WHERE reviewer_account_id=?)";args.add(query.reviewerId());
        }
        args.add(query.size()+1);args.add((long)query.page()*query.size());
        var ids=jdbc.query("SELECT id FROM merchant.application WHERE "+where+" ORDER BY first_submitted_at DESC,id DESC LIMIT ? OFFSET ?",(r,n)->r.getObject(1,UUID.class),args.toArray());
        List<ApplicationSummary> items=new ArrayList<>();for(UUID id:ids.stream().limit(query.size()).toList())items.add(application(actor,id));
        return new ApplicationSummaryPage(items,query.page(),query.size(),ids.size()>query.size());
    }
    @Override public ApplicationRevisionPage revisions(AdminReadContext actor,UUID id,int page,int size){
        page(page,size);Target t=target(id);return withScope(actor,t,scope->{
            scope.require("application.read",t);
            var items=jdbc.query("SELECT * FROM merchant.application_revision WHERE application_id=? ORDER BY revision DESC LIMIT ? OFFSET ?",(r,n)->{
                long revision=r.getLong("revision");String operation=revision==1?"SUBMIT":"RESUBMIT";
                var e=jdbc.queryForMap("SELECT id,protected_payload FROM merchant.governance_event WHERE application_id=? AND revision=? AND operation=? AND new_version=?",id,revision,operation,r.getLong("application_version"));
                var fact=JSON.readTree(payloads.open((UUID)e.get("id"),id,revision,operation,(byte[])e.get("protected_payload"))).get("contactFact");if(fact==null)throw fail(SERVICE_UNAVAILABLE);
                var ids=jdbc.query("SELECT evidence_id FROM merchant.application_revision_evidence WHERE application_id=? AND revision=? ORDER BY evidence_id",(x,index)->x.getObject(1,UUID.class),id,revision);
                return new ApplicationRevision(id,revision,r.getTimestamp("submitted_at").toInstant(),Long.toString(r.getLong("policy_version")),r.getString("activation_display_name"),HexFormat.of().formatHex(r.getBytes("content_digest")),ids,Instant.parse(fact.get("verifiedAt").asString()));
            },id,size+1,(long)page*size);
            return new ApplicationRevisionPage(items.stream().limit(size).toList(),page,size,items.size()>size);
        });
    }
    @Override public List<UUID> reviewerCandidateIds(AdminReadContext actor,UUID policy,UUID after){
        Target target=new Target(Kind.POLICY,Objects.requireNonNull(policy));return withScope(actor,target,scope->{
            scope.require("application.assign",target);
            return jdbc.query("SELECT account_id FROM merchant.business_reviewer_scope WHERE policy_id=? AND realm_id=? AND scope_type='POLICY' AND state='ACTIVE' AND expires_at>? AND permissions @> ARRAY['application.review'] AND (?::uuid IS NULL OR account_id>?::uuid) ORDER BY account_id LIMIT 100",(r,n)->r.getObject(1,UUID.class),policy,actor.realmId(),at(now()),after,after);
        });
    }
    @Override public List<ReviewerCandidate> eligibleReviewers(AdminCommandContext actor,UUID policy,List<UUID> candidates){
        Target target=new Target(Kind.POLICY,Objects.requireNonNull(policy));return withScope(actor,target,scope->{
            scope.require("application.assign",target);if(candidates==null||candidates.size()>100||candidates.stream().anyMatch(Objects::isNull)||new HashSet<>(candidates).size()!=candidates.size())throw fail(VALIDATION_FAILED);
            List<ReviewerCandidate> result=new ArrayList<>();for(UUID id:candidates){var eligible=actor.admin(id);
                if(eligible.eligible()&&actor.realmId().equals(eligible.realmId())&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.business_reviewer_scope WHERE account_id=? AND realm_id=? AND policy_id=? AND scope_type='POLICY' AND state='ACTIVE' AND expires_at>? AND permissions @> ARRAY['application.review'])",Boolean.class,id,actor.realmId(),policy,at(now()))))result.add(new ReviewerCandidate(id,eligible.displayName()));
            }return List.copyOf(result);
        });
    }
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    /** Translate only canonical activation conflicts; unexpected SQL/security failures remain failures. */
    static RuntimeException activationFailure(org.springframework.dao.DataAccessException failure){
        for(Throwable cause=failure;cause!=null;cause=cause.getCause()){
            if(cause instanceof SQLException sql){
                String first=Objects.toString(sql.getMessage(),"").lines().findFirst().orElse("").strip();
                if("23514".equals(sql.getSQLState())&&first.equals("ERROR: LEGACY_BUSINESS_KEY_CLEARANCE_REQUIRED"))return fail(LEGACY_KEY_CLEARANCE_REQUIRED);
                if("23505".equals(sql.getSQLState())&&(first.equals("ERROR: BUSINESS_KEY_ALREADY_ACTIVATED")||first.equals("ERROR: duplicate key value violates unique constraint \"business_key_registry_pkey\"")))return fail(BUSINESS_CONFLICT);
            }
        }
        return failure;
    }
    private static Target target(UUID id){if(id==null||id.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);return new Target(Kind.APPLICATION,id);}
    private static void reason(String reason){if(reason==null||reason.isBlank()||reason.codePointCount(0,reason.length())>300||reason.codePoints().anyMatch(c->Character.isISOControl(c)||c==0x2028||c==0x2029||c>=0xd800&&c<=0xdfff))throw fail(VALIDATION_FAILED);}
    private static void command(long version,UUID key){if(version<1||key==null||key.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);}
    private record ApplicationRow(UUID id,UUID applicant,UUID policy,String state,long revision,long version,Instant created,Instant first,Instant updated,String draft){}
    private ApplicationRow row(UUID id){return jdbc.query("SELECT * FROM merchant.application WHERE id=?",(r,n)->new ApplicationRow(id,r.getObject("applicant_account_id",UUID.class),r.getObject("policy_id",UUID.class),r.getString("state"),r.getLong("current_revision"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("first_submitted_at")==null?null:r.getTimestamp("first_submitted_at").toInstant(),r.getTimestamp("updated_at").toInstant(),r.getString("draft")),id).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));}
    private UUID reviewer(UUID application){return jdbc.query("SELECT reviewer_account_id FROM merchant.application_reviewer_assignment WHERE application_id=?",(r,n)->r.getObject(1,UUID.class),application).stream().findFirst().orElse(null);}
    @Override public AdminCommandAdmission.ReferencedSubjects subjects(AdminReadContext actor,UUID application,UUID candidate){
        Target t=target(application);return withScope(actor,t,s->{s.require(candidate==null?"application.read":"application.assign",t);var a=row(application);Set<UUID> admins=new HashSet<>();UUID current=reviewer(application);if(current!=null)admins.add(current);if(candidate!=null)admins.add(candidate);if(admins.contains(a.applicant()))throw fail(ACCESS_DENIED);return new AdminCommandAdmission.ReferencedSubjects(Set.of(a.applicant()),admins);});
    }
    private static String maskedName(String name){return name==null||name.isEmpty()?"***":new String(Character.toChars(name.codePointAt(0)))+"***";}
    private ApplicationSummary summary(UUID actor,ApplicationRow a,GovernanceScope scope){
        var draft=JSON.readTree(a.draft());String legal=draft.path("legalIdentifier").asString("");int from=legal.offsetByCodePoints(0,Math.max(0,legal.codePointCount(0,legal.length())-4));
        List<String> allowed=new ArrayList<>();Target t=target(a.id());
        if(Set.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION").contains(a.state()))try{scope.require("application.assign",t);allowed.add("ASSIGN_REVIEWER");}catch(GovernanceFailure denied){if(denied.code()!=ACCESS_DENIED)throw denied;}
        UUID reviewer=reviewer(a.id());boolean currentReviewer=actor.equals(reviewer)&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment x JOIN merchant.business_reviewer_scope s ON s.id=x.reviewer_scope_id WHERE x.application_id=? AND s.account_id=? AND s.state='ACTIVE' AND s.expires_at>? AND s.permissions @> ARRAY['application.review'])",Boolean.class,a.id(),actor,at(now())));
        if(currentReviewer&&permitted(scope,"application.review",t)){
            if(a.state().equals("SUBMITTED")||a.state().equals("UNDER_REVIEW")&&!reviewBegan(a,actor))allowed.add("BEGIN");
            if(a.state().equals("UNDER_REVIEW")&&reviewBegan(a,actor)){allowed.add("REQUEST_INFORMATION");if(!currentReady(a))allowed.add("READY_FOR_DECISION");}
        }
        if(a.state().equals("UNDER_REVIEW")&&reviewer!=null&&!actor.equals(reviewer)&&currentReady(a)&&permitted(scope,"application.decide",t)){allowed.add("REJECT");if(Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regprocedure('merchant.activate_approved_application(uuid,uuid,bigint,bigint,uuid,uuid)') IS NOT NULL",Boolean.class)))allowed.add("APPROVE");}
        return new ApplicationSummary(a.id(),a.state(),a.revision(),a.version(),maskedName(draft.path("registeredName").asString("")),"***"+legal.substring(from),a.policy(),reviewer(a.id()),a.first(),a.updated(),activation(a.id()),allowed);
    }
    private ActivationOutcome activation(UUID app){
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('merchant.application_activation') IS NOT NULL",Boolean.class)))return null;
        return jdbc.query("SELECT * FROM merchant.application_activation WHERE application_id=?",(r,n)->new ActivationOutcome(app,r.getLong("revision"),r.getObject("decision_event_id",UUID.class),r.getObject("organization_id",UUID.class),r.getObject("owner_membership_id",UUID.class),"ACTIVE",r.getTimestamp("activated_at").toInstant()),app).stream().findFirst().orElse(null);
    }
    private boolean permitted(GovernanceScope scope,String permission,Target target){try{scope.require(permission,target);return true;}catch(GovernanceFailure failure){if(failure.code()!=ACCESS_DENIED)throw failure;return false;}}
    private boolean reviewBegan(ApplicationRow a,UUID actor){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.application_reviewer_assignment x ON x.application_id=e.application_id WHERE e.application_id=? AND e.revision=? AND e.operation='BEGIN_REVIEW' AND e.actor_id=? AND e.occurred_at>=x.assigned_at)",Boolean.class,a.id(),a.revision(),actor));}
    private boolean currentReady(ApplicationRow a){return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.application_reviewer_assignment x ON x.application_id=e.application_id WHERE e.application_id=? AND e.revision=? AND e.operation='READY_FOR_DECISION' AND e.actor_id=x.reviewer_account_id AND e.occurred_at>=x.assigned_at AND e.new_version=?)",Boolean.class,a.id(),a.revision(),a.version()));}
    @Override public ApplicationSummary application(AdminReadContext actor,UUID id){Target t=target(id);return withScope(actor,t,s->{s.require("application.read",t);return summary(actor.accountId(),row(id),s);});}
    @Override public AuditPage events(AdminReadContext actor,UUID application,int page,int size){
        page(page,size);Target target=target(application);return withScope(actor,target,s->{
            s.require("audit.read",target);
            var items=jdbc.query("SELECT e.id,e.actor_id,e.operation,e.target_type,e.target_id,e.prior_version,e.new_version,e.revision,p.version AS policy_version,e.reason_code,e.occurred_at,e.request_id FROM merchant.governance_event e JOIN merchant.application_policy p ON p.id=e.policy_id WHERE e.application_id=? AND e.target_type='APPLICATION' AND e.target_id=? ORDER BY e.occurred_at DESC,e.id DESC LIMIT ? OFFSET ?",(r,n)->new AuditItem(r.getObject("id",UUID.class),r.getObject("actor_id",UUID.class),r.getString("operation"),r.getString("target_type"),r.getObject("target_id",UUID.class),r.getLong("prior_version"),r.getLong("new_version"),r.getObject("revision",Long.class),Long.toString(r.getLong("policy_version")),r.getString("reason_code"),r.getTimestamp("occurred_at").toInstant(),r.getObject("request_id",UUID.class)),application,application,size+1,(long)page*size);
            s.require("audit.read",target);return new AuditPage(items.stream().limit(size).toList(),page,size,items.size()>size);
        });
    }
    private record WorkCandidate(UUID application,UUID applicant,UUID reviewer,long revision,long assignmentVersion,long applicationVersion,Instant assignedAt){}
    @Override public WorkPage work(UUID request,WorkQuery query){
        if(request==null||query==null||query.kind()!=null&&!Set.of("MERCHANT_REVIEW","MERCHANT_DECISION").contains(query.kind())||query.policyId()!=null&&query.policyId().equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);page(query.page(),query.size());
        var authority=reads.execute(actor->{
            var context=scopes.context(actor);
            var policies=context.assignments().stream().filter(a->a.scopeType().equals("POLICY")&&a.permissions().containsAll(Set.of("task.read","application.read"))).map(GovernanceAssignment::policyId).distinct().sorted().toList();
            if(query.policyId()!=null&&!policies.contains(query.policyId()))throw fail(RESOURCE_NOT_FOUND);
            if(policies.isEmpty())throw fail(ACCESS_DENIED);
            // This source is actionable work only. Read-only principals cannot acquire command authority.
            return new Object[]{context,query.policyId()==null?policies:List.of(query.policyId()),actor.permissions().contains(AdminReadContext.MERCHANT)};
        });
        var original=(GovernanceContext)authority[0];if(!Boolean.TRUE.equals(authority[2]))return new WorkPage(List.of(),query.page(),query.size(),false);
        @SuppressWarnings("unchecked") var policies=(List<UUID>)authority[1];
        var visible=new ArrayList<WorkItem>();UUID cursor=null;long needed=(long)(query.page()+1)*query.size()+1,deadline=System.nanoTime()+Duration.ofSeconds(10).toNanos();
        while(visible.size()<needed){
            if(System.nanoTime()>deadline)throw fail(SERVICE_UNAVAILABLE);UUID after=cursor;
            var candidates=reads.execute(actor->{
                if(!original.authorityVersion().equals(scopes.context(actor).authorityVersion()))throw fail(SNAPSHOT_EXPIRED);
                var args=new ArrayList<Object>(policies);String where="a.policy_id IN ("+String.join(",",Collections.nCopies(policies.size(),"?"))+") AND a.state IN ('SUBMITTED','UNDER_REVIEW') AND a.submitted_revision=a.current_revision";
                if(after!=null){where+=" AND a.id>?";args.add(after);}
                return jdbc.query("SELECT a.id,a.applicant_account_id,a.current_revision,a.version,x.reviewer_account_id,x.version AS assignment_version,x.assigned_at FROM merchant.application a JOIN merchant.application_reviewer_assignment x ON x.application_id=a.id WHERE "+where+" ORDER BY a.id LIMIT 50",(r,n)->new WorkCandidate(r.getObject("id",UUID.class),r.getObject("applicant_account_id",UUID.class),r.getObject("reviewer_account_id",UUID.class),r.getLong("current_revision"),r.getLong("assignment_version"),r.getLong("version"),r.getTimestamp("assigned_at").toInstant()),args.toArray());
            });
            if(candidates.isEmpty())break;
            var references=new AdminCommandAdmission.ReferencedSubjects(new HashSet<>(candidates.stream().map(WorkCandidate::applicant).toList()),new HashSet<>(candidates.stream().map(WorkCandidate::reviewer).toList()));
            visible.addAll(commands.execute(request,references,actor->{
                if(!actor.accountId().equals(original.accountId())||!actor.realmId().equals(original.realmId()))throw fail(ACCESS_DENIED);
                var result=new ArrayList<WorkItem>();
                for(var candidate:candidates){
                    if(!actor.customer(candidate.applicant()).eligible())continue;
                    var eligible=actor.admin(candidate.reviewer());if(!eligible.eligible()||!actor.realmId().equals(eligible.realmId()))continue;
                    var target=target(candidate.application());
                    scopes.withScope(actor,target,scope->{
                        scope.require("task.read",target);scope.require("application.read",target);var current=row(candidate.application());
                        var assignment=jdbc.query("SELECT reviewer_account_id,version,assigned_at FROM merchant.application_reviewer_assignment WHERE application_id=?",(r,n)->new WorkCandidate(current.id(),current.applicant(),r.getObject(1,UUID.class),current.revision(),r.getLong(2),current.version(),r.getTimestamp(3).toInstant()),current.id()).stream().findFirst().orElse(null);
                        if(!candidate.equals(assignment)||!Set.of("SUBMITTED","UNDER_REVIEW").contains(current.state()))throw fail(SNAPSHOT_EXPIRED);
                        var reviewerActive=Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment x JOIN merchant.business_reviewer_scope s ON s.id=x.reviewer_scope_id WHERE x.application_id=? AND s.account_id=? AND s.state='ACTIVE' AND s.expires_at>? AND s.permissions @> ARRAY['application.review'])",Boolean.class,current.id(),candidate.reviewer(),at(now())));
                        if(!reviewerActive||actor.accountId().equals(current.applicant()))return null;
                        var summary=summary(actor.accountId(),current,scope);String kind;List<String> actions;
                        if(actor.accountId().equals(candidate.reviewer())&&!currentReady(current)){kind="MERCHANT_REVIEW";actions=summary.allowedActions().stream().filter(a->Set.of("BEGIN","REQUEST_INFORMATION","READY_FOR_DECISION").contains(a)).toList();}
                        else if(!actor.accountId().equals(candidate.reviewer())&&currentReady(current)){kind="MERCHANT_DECISION";actions=summary.allowedActions().stream().filter(a->Set.of("APPROVE","REJECT").contains(a)).toList();}
                        else return null;
                        if(actions.isEmpty()||query.kind()!=null&&!query.kind().equals(kind))return null;
                        UUID id=UUID.nameUUIDFromBytes((kind+"|"+current.id()+"|"+current.revision()+"|"+candidate.assignmentVersion()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        result.add(new WorkItem(id,kind,current.id(),current.revision(),candidate.assignmentVersion(),candidate.assignedAt(),null,"/admin/merchants/applications?applicationId="+current.id(),actions));return null;
                    });
                }return result;
            }));
            cursor=candidates.getLast().application();if(candidates.size()<50)break;
        }
        reads.execute(actor->{if(!original.authorityVersion().equals(scopes.context(actor).authorityVersion()))throw fail(SNAPSHOT_EXPIRED);return null;});
        long skip=(long)query.page()*query.size();return new WorkPage(visible.stream().skip(skip).limit(query.size()).toList(),query.page(),query.size(),visible.size()>skip+query.size());
    }
    @Override public AuditPage audit(AdminReadContext actor,AuditQuery query){return new JdbcMerchantAuditReads(jdbc,scopes,clock).read(actor,query);}
    @Override public CommandReceipt receipt(AdminReadContext actor,String operation,UUID key){
        scopes.context(actor);actor.require(AdminReadContext.MERCHANT);
        String permission=switch(operation==null?"":operation){case "ASSIGN_REVIEWER"->"application.assign";case "BEGIN_REVIEW","READY_FOR_DECISION","REQUEST_INFORMATION"->"application.review";case "REJECT","APPROVE"->"application.decide";default->throw fail(VALIDATION_FAILED);};
        if(key==null||key.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);
        var values=jdbc.query("SELECT target_id,result_version FROM merchant.governance_receipt WHERE actor_kind='SYSTEM' AND actor_binding=? AND realm_id=? AND operation=? AND operation_key=? AND target_type='APPLICATION' AND outcome='COMMITTED'",(r,n)->new CommandReceipt(operation,r.getObject(1,UUID.class),"COMMITTED",r.getLong(2),null),actor.accountId().toString(),actor.realmId(),operation,key);
        if(values.size()!=1)throw fail(RESOURCE_NOT_FOUND);var receipt=values.getFirst();Target target=target(receipt.targetId());
        return withScope(actor,target,s->{s.require(permission,target);return receipt;});
    }
    private record AuthorizedEvidence(UUID actor,UUID realm,String authority,long revision,com.elogistic.backend.platform.api.PrivateEvidenceStore.Blob blob,String mime){}
    private AuthorizedEvidence authorizedEvidence(AdminCommandContext actor,UUID app,UUID evidence,String purpose,boolean disclose){
        Target target=target(app);return withScope(actor,target,s->{
            s.require("application.read",target);s.require("application.evidence.read",target);if(!"MERCHANT_REVIEW".equals(purpose))throw fail(VALIDATION_FAILED);
            var a=row(app);applicant(actor,a);verifySubmission(actor,a);
            var value=jdbc.query("SELECT e.blob_key,e.bytes,e.sha256,e.mime FROM merchant.application_revision_evidence x JOIN merchant.application_evidence e ON e.id=x.evidence_id AND e.application_id=x.application_id AND e.sha256=x.sha256 WHERE x.application_id=? AND x.revision=? AND e.id=? AND e.state='CLEAN'",(r,n)->new AuthorizedEvidence(actor.accountId(),actor.realmId(),s.version(),a.revision(),new com.elogistic.backend.platform.api.PrivateEvidenceStore.Blob(r.getObject(1,UUID.class),r.getLong(2),r.getString(3)),r.getString(4)),app,a.revision(),evidence).stream().findFirst().orElseThrow(()->fail(RESOURCE_NOT_FOUND));
            if(disclose){UUID event=UUID.randomUUID();byte[] payload=payloads.seal(event,app,a.revision(),"READ_DISCLOSURE",body(Map.of("purpose",purpose,"evidenceId",evidence)));
                try{jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,realm_id,target_type,target_id,application_id,policy_id,operation,revision,prior_version,new_version,reason_code,protected_payload,request_id,occurred_at) VALUES (?,'SYSTEM',?,?,'APPLICATION',?,?,?,'READ_DISCLOSURE',?,?,?,'MERCHANT_REVIEW',?,?,?)",event,actor.accountId(),actor.realmId(),app,app,a.policy(),a.revision(),a.version(),a.version(),payload,actor.requestId(),at(now()));}finally{Arrays.fill(payload,(byte)0);}}
            return value;
        });
    }
    @Override public EvidenceContent evidenceContent(UUID app,UUID evidence,String purpose,UUID request){
        if(blobs==null)throw fail(SERVICE_UNAVAILABLE);if(evidence==null||evidence.equals(new UUID(0,0)))throw fail(VALIDATION_FAILED);
        if(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())throw fail(ACCESS_DENIED);
        var references=reads.execute(c->subjects(c,app,null));var expected=commands.execute(request,references,c->authorizedEvidence(c,app,evidence,purpose,true));byte[] bytes=null;
        try(var content=blobs.read(expected.blob())){
            bytes=content.bytes();byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes);
            try{if(bytes.length!=expected.blob().bytes()||!MessageDigest.isEqual(hash,HexFormat.of().parseHex(expected.blob().sha256())))throw fail(EVIDENCE_NOT_READY);}finally{Arrays.fill(hash,(byte)0);}
            references=reads.execute(c->subjects(c,app,null));var current=commands.execute(request,references,c->authorizedEvidence(c,app,evidence,purpose,false));if(!current.equals(expected))throw fail(ACCESS_DENIED);
            return new EvidenceContent(bytes,current.mime());
        }catch(GovernanceFailure known){throw known;}catch(com.elogistic.backend.platform.api.PrivateEvidenceStore.Rejected rejected){throw fail(EVIDENCE_NOT_READY);}catch(java.security.GeneralSecurityException|com.elogistic.backend.platform.api.PrivateEvidenceStore.Unavailable unavailable){throw fail(SERVICE_UNAVAILABLE);}finally{if(bytes!=null)Arrays.fill(bytes,(byte)0);}
    }
    private void applicant(AdminCommandContext actor,ApplicationRow a){if(!actor.customer(a.applicant()).eligible()||actor.accountId().equals(a.applicant()))throw fail(ACCESS_DENIED);}
    private void independent(UUID actor,UUID applicant){
        if(actor.equals(applicant)||Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.membership a JOIN merchant.membership b ON b.organization_id=a.organization_id WHERE a.account_id=? AND b.account_id=? AND a.state<>'REVOKED' AND b.state<>'REVOKED')",Boolean.class,actor,applicant)))throw fail(ACCESS_DENIED);
    }
    private String body(Map<String,Object> fields){return JSON.writeValueAsString(new TreeMap<>(fields));}
    private byte[] digest(String keyId,AdminCommandContext actor,String operation,UUID app,long expected,UUID key,String body){return fingerprints.hash(keyId,"merchantGovernanceCommand",app,expected,actor.accountId().toString(),actor.realmId().toString(),operation,key.toString(),body);}
    private record Saved(UUID target,String keyId,byte[] digest,long original,long result,UUID event,long revision,UUID policy,Instant occurred,byte[] protectedValue){}
    private Optional<Saved> saved(AdminCommandContext actor,String operation,UUID key){return jdbc.query("SELECT r.target_id,r.input_key_id,r.input_digest,r.original_version,r.result_version,e.id,e.revision,e.policy_id,e.occurred_at,e.protected_payload FROM merchant.governance_receipt r JOIN merchant.governance_event e ON e.id=r.event_id WHERE r.actor_kind='SYSTEM' AND r.actor_binding=? AND r.realm_id=? AND r.operation=? AND r.operation_key=?",(r,n)->new Saved(r.getObject(1,UUID.class),r.getString(2),r.getBytes(3),r.getLong(4),r.getLong(5),r.getObject(6,UUID.class),r.getLong(7),r.getObject(8,UUID.class),r.getTimestamp(9).toInstant(),r.getBytes(10)),actor.accountId().toString(),actor.realmId(),operation,key).stream().findFirst();}
    private Optional<ApplicationSummary> replay(AdminCommandContext actor,String op,UUID id,long expected,UUID key,String body){
        var saved=saved(actor,op,key);if(saved.isEmpty())return Optional.empty();var r=saved.get();byte[] actual=digest(r.keyId(),actor,op,id,expected,key,body);
        try{if(!r.target().equals(id)||r.original()!=expected||!MessageDigest.isEqual(actual,r.digest()))throw fail(IDEMPOTENCY_CONFLICT);return Optional.of(outcome(r,op));}finally{Arrays.fill(actual,(byte)0);Arrays.fill(r.digest(),(byte)0);}
    }
    private ApplicationSummary outcome(Saved receipt,String operation){
        var a=row(receipt.target());var value=JSON.readTree(payloads.open(receipt.event(),receipt.target(),receipt.revision(),operation,receipt.protectedValue()));
        var frozen=value.get("outcome");if(frozen==null||!frozen.isObject())throw fail(SERVICE_UNAVAILABLE);
        String draft=jdbc.queryForObject("SELECT draft FROM merchant.application_draft_version WHERE application_id=? AND application_version=?",String.class,receipt.target(),receipt.result());var fields=JSON.readTree(draft);String legal=fields.path("legalIdentifier").asString("");int offset=legal.offsetByCodePoints(0,Math.max(0,legal.codePointCount(0,legal.length())-4));
        var reviewer=frozen.get("reviewerId");return new ApplicationSummary(receipt.target(),frozen.get("state").asString(),receipt.revision(),receipt.result(),maskedName(fields.path("registeredName").asString("")),"***"+legal.substring(offset),receipt.policy(),reviewer==null||reviewer.isNull()?null:UUID.fromString(reviewer.asString()),a.first(),receipt.occurred(),operation.equals("APPROVE")?activation(receipt.target()):null,List.of());
    }
    private ApplicationSummary committed(AdminCommandContext actor,String operation,UUID key){return outcome(saved(actor,operation,key).orElseThrow(()->fail(SERVICE_UNAVAILABLE)),operation);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MICROS);}
    private static Timestamp at(Instant t){return Timestamp.from(t);}
    private UUID record(AdminCommandContext actor,ApplicationRow a,String operation,String reasonCode,UUID key,String canonical,Instant at){
        UUID event=UUID.randomUUID();var frozen=new TreeMap<String,Object>();frozen.put("state",switch(operation){case "BEGIN_REVIEW"->"UNDER_REVIEW";case "REQUEST_INFORMATION"->"NEEDS_INFORMATION";case "REJECT"->"REJECTED";case "APPROVE"->"APPROVED";default->a.state();});frozen.put("reviewerId",operation.equals("ASSIGN_REVIEWER")?JSON.readTree(canonical).get("assigneeAccountId").asString():reviewer(a.id()));
        byte[] protectedBody=payloads.seal(event,a.id(),a.revision(),operation,JSON.writeValueAsString(Map.of("command",JSON.readTree(canonical),"outcome",frozen)));
        try{jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,realm_id,target_type,target_id,application_id,policy_id,operation,revision,prior_version,new_version,reason_code,protected_payload,request_id,occurred_at) VALUES (?,'SYSTEM',?,?,'APPLICATION',?,?,?,?,?,?,?,?,?,?,?)",event,actor.accountId(),actor.realmId(),a.id(),a.id(),a.policy(),operation,a.revision(),a.version(),a.version()+1,reasonCode,protectedBody,actor.requestId(),at(at));}finally{Arrays.fill(protectedBody,(byte)0);}
        byte[] hash=digest(fingerprints.activeKey(),actor,operation,a.id(),a.version(),key,canonical);
        try{jdbc.update("INSERT INTO merchant.governance_receipt VALUES ('SYSTEM',?,?,?,?, 'APPLICATION',?,?,?,?,?,?,'COMMITTED',?,?)",actor.accountId().toString(),actor.realmId(),operation,key,a.id(),a.version(),a.version()+1,fingerprints.activeKey(),hash,event,at(at),at(at.plus(Duration.ofDays(7))));}finally{Arrays.fill(hash,(byte)0);}
        return event;
    }
    private void update(ApplicationRow before,String state,Instant stamp){
        if(jdbc.update("UPDATE merchant.application SET state=?,version=version+1,updated_at=?,terminal_at=CASE WHEN ?='REJECTED' THEN ? ELSE terminal_at END WHERE id=? AND version=?",state,at(stamp),state,at(stamp),before.id(),before.version())!=1)throw fail(PRECONDITION_STALE);
        jdbc.update("INSERT INTO merchant.application_draft_version VALUES (?,?,?,?::jsonb,?,?)",before.id(),before.version()+1,before.applicant(),before.draft(),at(before.created()),at(stamp));
    }
    @Override public ApplicationSummary assign(AdminCommandContext actor,UUID id,ReviewerAssignmentInput input,long expected,UUID key){
        command(expected,key);if(input==null||input.assigneeAccountId()==null)throw fail(VALIDATION_FAILED);reason(input.reason());Target t=target(id);
        return withScope(actor,t,s->{
            s.require("application.assign",t);ApplicationRow a=row(id);applicant(actor,a);independent(actor.accountId(),a.applicant());independent(input.assigneeAccountId(),a.applicant());
            var assignee=actor.admin(input.assigneeAccountId());if(!assignee.eligible()||!actor.realmId().equals(assignee.realmId()))throw fail(ACCESS_DENIED);
            var candidates=jdbc.query("SELECT id FROM merchant.business_reviewer_scope WHERE account_id=? AND realm_id=? AND scope_type='POLICY' AND policy_id=? AND state='ACTIVE' AND expires_at>? AND permissions @> ARRAY['application.review']",(r,n)->r.getObject(1,UUID.class),input.assigneeAccountId(),actor.realmId(),a.policy(),at(now()));if(candidates.size()!=1)throw fail(ACCESS_DENIED);
            String body=body(Map.of("assigneeAccountId",input.assigneeAccountId(),"reason",input.reason()));var replay=replay(actor,"ASSIGN_REVIEWER",id,expected,key,body);if(replay.isPresent())return replay.get();
            if(a.version()!=expected)throw fail(PRECONDITION_STALE);if(!Set.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION").contains(a.state()))throw fail(STATE_CONFLICT);
            Instant at=now();UUID event=record(actor,a,"ASSIGN_REVIEWER","REVIEWER_ASSIGNMENT",key,body,at);
            jdbc.update("INSERT INTO merchant.application_reviewer_assignment VALUES (?,?,?,?,1,?,?) ON CONFLICT(application_id) DO UPDATE SET reviewer_account_id=EXCLUDED.reviewer_account_id,reviewer_scope_id=EXCLUDED.reviewer_scope_id,policy_id=EXCLUDED.policy_id,version=merchant.application_reviewer_assignment.version+1,event_id=EXCLUDED.event_id,assigned_at=EXCLUDED.assigned_at",id,input.assigneeAccountId(),candidates.getFirst(),a.policy(),event,at(at));
            update(a,a.state(),at);s.require("application.assign",t);return committed(actor,"ASSIGN_REVIEWER",key);
        });
    }
    private void assigned(AdminCommandContext actor,ApplicationRow a){
        applicant(actor,a);independent(actor.accountId(),a.applicant());
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.application_reviewer_assignment a JOIN merchant.business_reviewer_scope s ON s.id=a.reviewer_scope_id WHERE a.application_id=? AND a.reviewer_account_id=? AND a.policy_id=? AND s.account_id=a.reviewer_account_id AND s.realm_id=? AND s.policy_id=a.policy_id AND s.state='ACTIVE' AND s.expires_at>? AND s.permissions @> ARRAY['application.review'])",Boolean.class,a.id(),actor.accountId(),a.policy(),actor.realmId(),at(now()))))throw fail(ACCESS_DENIED);
    }
    @Override public ApplicationSummary review(AdminCommandContext actor,UUID id,ReviewInput input,long expected,UUID key){
        command(expected,key);if(input==null||!Set.of("BEGIN","READY_FOR_DECISION").contains(Objects.toString(input.action(),""))||input.revision()<1||input.policyVersion()==null||!input.policyVersion().matches("[1-9][0-9]{0,18}"))throw fail(VALIDATION_FAILED);reason(input.reason());Target t=target(id);
        return withScope(actor,t,s->{
            s.require("application.review",t);ApplicationRow a=row(id);assigned(actor,a);
            String body=body(Map.of("action",input.action(),"revision",input.revision(),"policyVersion",input.policyVersion(),"checklist",input.checklist()==null?Map.of():input.checklist(),"reason",input.reason()));String operation=input.action().equals("BEGIN")?"BEGIN_REVIEW":"READY_FOR_DECISION";
            var replay=replay(actor,operation,id,expected,key,body);if(replay.isPresent())return replay.get();
            if(a.version()!=expected)throw fail(PRECONDITION_STALE);long policy=jdbc.queryForObject("SELECT version FROM merchant.application_policy WHERE id=?",Long.class,a.policy());
            if(a.revision()!=input.revision()||!Long.toString(policy).equals(input.policyVersion()))throw fail(STATE_CONFLICT);
            boolean ready=input.action().equals("READY_FOR_DECISION");
            if(ready?!a.state().equals("UNDER_REVIEW")||currentReady(a):!a.state().equals("SUBMITTED")&&!(a.state().equals("UNDER_REVIEW")&&!reviewBegan(a,actor.accountId())))throw fail(STATE_CONFLICT);
            if(ready){
                var check=input.checklist();if(check==null||!check.registrationValid()||!check.representationValid()||!check.contactValid()||!check.duplicatesClear()||!check.conflictsClear())throw fail(VALIDATION_FAILED);
                verifySubmission(actor,a);
                if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM merchant.governance_event e JOIN merchant.application_reviewer_assignment x ON x.application_id=e.application_id WHERE e.application_id=? AND e.revision=? AND e.operation='BEGIN_REVIEW' AND e.actor_id=? AND e.occurred_at>=x.assigned_at)",Boolean.class,id,a.revision(),actor.accountId())))throw fail(STATE_CONFLICT);
            }
            Instant stamp=now();record(actor,a,operation,ready?"CHECKLIST_COMPLETE":"REVIEW_STARTED",key,body,stamp);update(a,"UNDER_REVIEW",stamp);s.require("application.review",t);return committed(actor,operation,key);
        });
    }
    private CustomerBusinessContacts.BusinessContactAttestation verifySubmission(AdminCommandContext actor,ApplicationRow a){
        var revision=jdbc.queryForMap("SELECT * FROM merchant.application_revision WHERE application_id=? AND revision=?",a.id(),a.revision());
        var input=JSON.readValue(revision.get("submitted_input").toString(),MerchantApplications.Input.class);
        CustomerBusinessContacts.BusinessContactAttestation fact;
        try{fact=contacts.submittedAttestation(actor,(UUID)revision.get("contact_attestation_id"),a.id(),a.applicant(),a.revision(),input.representativeEmail());}
        catch(com.elogistic.backend.identity.api.IdentityCommandFailure invalid){if(invalid.code()==com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED)throw fail(CONTACT_PROOF_REQUIRED);throw invalid;}
        String operation=a.revision()==1?"SUBMIT":"RESUBMIT";
        var submission=jdbc.queryForMap("SELECT id,protected_payload FROM merchant.governance_event WHERE application_id=? AND revision=? AND operation=? AND new_version=?",a.id(),a.revision(),operation,revision.get("application_version"));
        var frozen=JSON.readTree(payloads.open((UUID)submission.get("id"),a.id(),a.revision(),operation,(byte[])submission.get("protected_payload"))).get("contactFact");
        var expected=Map.of("id",fact.id().toString(),"applicationId",a.id().toString(),"revision",a.revision(),"verifiedAt",fact.verifiedAt().toString());
        if(frozen==null||!frozen.equals(JSON.readTree(JSON.writeValueAsString(expected))))throw fail(CONTACT_PROOF_REQUIRED);
        List<Map<String,Object>> evidence=new ArrayList<>();Set<String> purposes=new HashSet<>();
        for(UUID id:input.evidenceIds()){
            var rows=jdbc.queryForList("SELECT e.id,e.sha256,e.purpose FROM merchant.application_revision_evidence x JOIN merchant.application_evidence e ON e.id=x.evidence_id AND e.application_id=x.application_id WHERE x.application_id=? AND x.revision=? AND x.evidence_id=? AND e.sha256=x.sha256 AND e.state='CLEAN' AND e.screened_at IS NOT NULL AND e.scan_version IS NOT NULL",a.id(),a.revision(),id);
            if(rows.size()!=1)throw fail(EVIDENCE_NOT_READY);var row=rows.getFirst();evidence.add(Map.of("id",id,"hash",row.get("sha256")));purposes.add((String)row.get("purpose"));
        }
        if(!purposes.containsAll(Set.of("REGISTRATION","REPRESENTATIVE_AUTHORIZATION")))throw fail(EVIDENCE_NOT_READY);
        try{
            byte[] actual=MessageDigest.getInstance("SHA-256").digest(body(Map.of("input",input,"displayName",revision.get("activation_display_name"),"policy",a.policy(),"policyVersion",revision.get("policy_version"),"contactFact",expected,"evidence",evidence)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            try{if(!MessageDigest.isEqual(actual,(byte[])revision.get("content_digest")))throw fail(STATE_CONFLICT);}finally{Arrays.fill(actual,(byte)0);}
        }catch(java.security.NoSuchAlgorithmException unavailable){throw fail(SERVICE_UNAVAILABLE);}
        return fact;
    }
    @Override public ReviewData reviewData(AdminCommandContext actor,UUID id,String purpose){
        Target t=target(id);return withScope(actor,t,scope->{
            scope.require("application.read",t);scope.require("application.evidence.read",t);
            if(!"MERCHANT_REVIEW".equals(purpose))throw fail(VALIDATION_FAILED);
            var a=row(id);applicant(actor,a);var fact=verifySubmission(actor,a);
            var revision=jdbc.queryForMap("SELECT * FROM merchant.application_revision WHERE application_id=? AND revision=?",id,a.revision());
            var input=JSON.readValue(revision.get("submitted_input").toString(),MerchantApplications.Input.class);
            var evidence=jdbc.query("SELECT e.* FROM merchant.application_revision_evidence x JOIN merchant.application_evidence e ON e.id=x.evidence_id AND e.application_id=x.application_id WHERE x.application_id=? AND x.revision=? ORDER BY e.id",(r,n)->new MerchantApplications.Evidence(r.getObject("id",UUID.class),id,r.getString("purpose"),r.getString("state"),r.getString("mime"),r.getLong("bytes"),r.getString("sha256"),r.getLong("version"),r.getTimestamp("screened_at").toInstant()),id,a.revision());
            List<Map<String,Object>> events=new ArrayList<>();
            jdbc.query("SELECT id,operation,revision,actor_id,occurred_at,protected_payload FROM merchant.governance_event WHERE application_id=? AND operation IN ('BEGIN_REVIEW','READY_FOR_DECISION','REQUEST_INFORMATION') ORDER BY occurred_at DESC,id DESC LIMIT 50",r->{
                String op=r.getString("operation");var value=JSON.readTree(payloads.open(r.getObject("id",UUID.class),id,r.getLong("revision"),op,r.getBytes("protected_payload")));
                events.add(Map.of("id",r.getObject("id",UUID.class),"operation",op,"actorId",r.getObject("actor_id",UUID.class),"occurredAt",r.getTimestamp("occurred_at").toInstant(),"command",value.get("command")));
            },id);
            UUID event=UUID.randomUUID();byte[] protectedPurpose=payloads.seal(event,id,a.revision(),"READ_DISCLOSURE",body(Map.of("purpose",purpose)));
            try{jdbc.update("INSERT INTO merchant.governance_event(id,actor_kind,actor_id,realm_id,target_type,target_id,application_id,policy_id,operation,revision,prior_version,new_version,reason_code,protected_payload,request_id,occurred_at) VALUES (?,'SYSTEM',?,?,'APPLICATION',?,?,?,'READ_DISCLOSURE',?,?,?,'MERCHANT_REVIEW',?,?,?)",event,actor.accountId(),actor.realmId(),id,id,a.policy(),a.revision(),a.version(),a.version(),protectedPurpose,actor.requestId(),at(now()));}finally{Arrays.fill(protectedPurpose,(byte)0);}
            scope.require("application.evidence.read",t);
            return new ReviewData(summary(actor.accountId(),a,scope),input,evidence,fact.verifiedAt(),revision.get("policy_version").toString(),events,(String)revision.get("activation_display_name"));
        });
    }
    @Override public ApplicationSummary reject(AdminCommandContext actor,UUID id,DecisionInput input,long expected,UUID key){return decide(actor,id,input,expected,key,false);}
    @Override public ApplicationSummary approve(AdminCommandContext actor,UUID id,DecisionInput input,long expected,UUID key){return decide(actor,id,input,expected,key,true);}
    private ApplicationSummary decide(AdminCommandContext actor,UUID id,DecisionInput input,long expected,UUID key,boolean approval){
        final String operation=approval?"APPROVE":"REJECT";
        command(expected,key);if(input==null||input.revision()<1||input.reauthenticationProofId()==null||input.policyVersion()==null||!input.policyVersion().matches("[1-9][0-9]{0,18}"))throw fail(VALIDATION_FAILED);reason(input.reason());Target t=target(id);
        return withScope(actor,t,scope->{
            scope.require("application.decide",t);var a=row(id);applicant(actor,a);independent(actor.accountId(),a.applicant());
            UUID assigned=reviewer(id);if(assigned==null||assigned.equals(actor.accountId())||!actor.admin(assigned).eligible())throw fail(ACCESS_DENIED);
            String canonical=body(Map.of("revision",input.revision(),"policyVersion",input.policyVersion(),"reauthenticationProofId",input.reauthenticationProofId(),"reason",input.reason()));
            var old=replay(actor,operation,id,expected,key,canonical);if(old.isPresent())return old.get();
            if(a.version()!=expected)throw fail(PRECONDITION_STALE);if(!a.state().equals("UNDER_REVIEW")||a.revision()!=input.revision())throw fail(STATE_CONFLICT);
            var ready=jdbc.queryForList("SELECT e.id,e.new_version FROM merchant.governance_event e JOIN merchant.application_reviewer_assignment x ON x.application_id=e.application_id JOIN merchant.business_reviewer_scope s ON s.id=x.reviewer_scope_id WHERE e.application_id=? AND e.revision=? AND e.operation='READY_FOR_DECISION' AND e.actor_id=x.reviewer_account_id AND e.occurred_at>=x.assigned_at AND s.state='ACTIVE' AND s.expires_at>? AND s.permissions @> ARRAY['application.review'] ORDER BY e.new_version DESC LIMIT 1",id,a.revision(),at(now()));
            if(ready.size()!=1||((Number)ready.getFirst().get("new_version")).longValue()!=a.version()||!input.policyVersion().equals(jdbc.queryForObject("SELECT policy_version::text FROM merchant.application_revision WHERE application_id=? AND revision=?",String.class,id,a.revision())))throw fail(STATE_CONFLICT);
            verifySubmission(actor,a);byte[] commandDigest;
            try{commandDigest=MessageDigest.getInstance("SHA-256").digest(body(Map.of("operation",operation,"target",id.toString(),"originalVersion",expected,"revision",input.revision(),"policy",input.policyVersion(),"reason",input.reason())).getBytes(java.nio.charset.StandardCharsets.UTF_8));}catch(java.security.NoSuchAlgorithmException unavailable){throw fail(SERVICE_UNAVAILABLE);}
            try{
                reauthentication.consume(actor,input.reauthenticationProofId(),scope.version(),operation,id,expected,commandDigest);
                String proofBinding=body(Map.of("proofId",input.reauthenticationProofId().toString(),"authorityVersion",scope.version(),"inputDigest",HexFormat.of().formatHex(commandDigest)));
                String previous=jdbc.queryForObject("SELECT current_setting('argopost.governance_stepup',true)",String.class);
                jdbc.queryForObject("SELECT set_config('argopost.governance_stepup',?,true)",String.class,proofBinding);
                RuntimeException primary=null;try{Instant stamp=now();UUID event=record(actor,a,operation,approval?"INDEPENDENT_APPROVAL":"INDEPENDENT_REJECTION",key,canonical,stamp);
                    if(approval){try{jdbc.queryForObject("SELECT merchant.activate_approved_application(?,?,?,?,?,?)",UUID.class,actor.requestId(),id,a.revision(),expected,event,input.reauthenticationProofId());}catch(org.springframework.dao.DataAccessException failure){throw activationFailure(failure);}}else update(a,"REJECTED",stamp);
}
                catch(RuntimeException failure){primary=failure;throw failure;}finally{try{jdbc.queryForObject("SELECT set_config('argopost.governance_stepup',?,true)",String.class,previous==null?"":previous);}catch(org.springframework.dao.DataAccessException cleanup){if(primary==null)throw cleanup;primary.addSuppressed(cleanup);}}
            }finally{Arrays.fill(commandDigest,(byte)0);}
            scope.require("application.decide",t);return committed(actor,operation,key);
        });
    }
    @Override public ApplicationSummary requestInformation(AdminCommandContext actor,UUID id,InformationRequest input,long expected,UUID key){
        command(expected,key);Set<String> fields=Set.of("entityType","jurisdiction","registeredName","legalIdentifier","registeredAddress","operatingAddress","representativeName","representativeEmail","representativeRelationship","businessContactAttestationId","evidenceIds","declarations","REGISTRATION","REPRESENTATIVE_AUTHORIZATION");
        if(input==null||input.revision()<1||input.fieldCodes()==null||input.fieldCodes().isEmpty()||input.fieldCodes().size()>16||new HashSet<>(input.fieldCodes()).size()!=input.fieldCodes().size()||input.fieldCodes().stream().anyMatch(code->code==null||!fields.contains(code)))throw fail(VALIDATION_FAILED);reason(input.reason());Target t=target(id);
        return withScope(actor,t,s->{
            s.require("application.review",t);var a=row(id);assigned(actor,a);String body=body(Map.of("revision",input.revision(),"fieldCodes",input.fieldCodes().stream().sorted().toList(),"reason",input.reason()));var replay=replay(actor,"REQUEST_INFORMATION",id,expected,key,body);if(replay.isPresent())return replay.get();
            if(a.version()!=expected)throw fail(PRECONDITION_STALE);if(!a.state().equals("UNDER_REVIEW")||a.revision()!=input.revision())throw fail(STATE_CONFLICT);
            Instant stamp=now();UUID event=record(actor,a,"REQUEST_INFORMATION","INFORMATION_REQUIRED",key,body,stamp);update(a,"NEEDS_INFORMATION",stamp);s.require("application.review",t);return committed(actor,"REQUEST_INFORMATION",key);
        });
    }
    @Override public OrganizationSummaryPage organizations(AdminReadContext actor,OrganizationQuery query){return organizations.organizations(actor,query);}
    @Override public OrganizationSummary organization(AdminReadContext actor,UUID id){return organizations.organization(actor,id);}
    @Override public AdminCommandAdmission.ReferencedSubjects organizationSubjects(AdminReadContext actor,UUID id){return organizations.references(actor,id);}
    @Override public OrganizationSummary suspend(AdminCommandContext actor,UUID id,LifecycleInput input,long expectedVersion,UUID key){return organizations.transition(actor,id,input,expectedVersion,key,true);}
    @Override public OrganizationSummary reinstate(AdminCommandContext actor,UUID id,LifecycleInput input,long expectedVersion,UUID key){return organizations.transition(actor,id,input,expectedVersion,key,false);}

}
