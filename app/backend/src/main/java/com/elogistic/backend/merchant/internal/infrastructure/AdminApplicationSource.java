package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** V1 remains unavailable; V2 captures only canonical first-submitted, currently authorized facts. */
@Component @Profile("identity")
public final class AdminApplicationSource implements AdminApplicationFacts {
    private static final Set<String> STATES=Set.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","APPROVED","REJECTED","WITHDRAWN");
    private static final Set<String> METRICS=Set.of("APPLICATIONS_SUBMITTED","APPLICATIONS_UNDER_REVIEW","APPLICATIONS_NEEDS_INFORMATION","APPLICATIONS_APPROVED","APPLICATIONS_REJECTED","APPLICATIONS_WITHDRAWN","APPLICATION_PROCESSING_TOTAL_MS","APPLICATION_PROCESSING_MIN_MS","APPLICATION_PROCESSING_MAX_MS","APPLICATION_PROCESSING_AVG_MS");
    private final DataSource source;private final AdminAdmission reads;private final Supplier<MerchantGovernance> governance;private final Clock clock;private final JdbcTemplate jdbc;
    public AdminApplicationSource(){source=null;reads=null;governance=()->null;clock=Clock.systemUTC();jdbc=null;}
    @Autowired public AdminApplicationSource(DataSource source,AdminAdmission reads,ObjectProvider<MerchantGovernance> governance,@Qualifier("identityHttpClock")Clock clock){this(source,reads,governance::getIfAvailable,clock);}
    public AdminApplicationSource(DataSource source,AdminAdmission reads,MerchantGovernance governance,Clock clock){this(source,reads,()->governance,clock);}
    private AdminApplicationSource(DataSource source,AdminAdmission reads,Supplier<MerchantGovernance> governance,Clock clock){this.source=Objects.requireNonNull(source);this.reads=Objects.requireNonNull(reads);this.governance=Objects.requireNonNull(governance);this.clock=Objects.requireNonNull(clock);jdbc=new JdbcTemplate(source);}
    public String availability(Access actor){actor.requireApplicationFacts();return "SOURCE_NOT_IMPLEMENTED";}
    private static GovernanceFailure fail(GovernanceFailure.Code code){return new GovernanceFailure(code);}
    private static String digest(Object value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.toString().getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException unavailable){throw fail(SERVICE_UNAVAILABLE);}}
    private AdminReadContext admit(Access access){
        if(reads==null||access==null)throw new AdminFailure("ACCESS_DENIED");
        var context=access.identityContext();reads.requireCurrent(context);
        if(!TransactionSynchronizationManager.hasResource(source)||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.isCurrentTransactionReadOnly())throw new AdminFailure("ACCESS_DENIED");
        context.require(AdminReadContext.ENTRY);context.require(AdminReadContext.MERCHANT);access.requireApplicationFacts();return context;
    }
    @Override public Capture capture(Access access,ApplicationCohort query,Instant cutoff){
        var actor=admit(access);
        if(query==null||query.from()==null||query.until()==null||!query.from().isBefore(query.until())||Duration.between(query.from(),query.until()).compareTo(Duration.ofDays(366))>0||query.state()!=null&&!STATES.contains(query.state())||query.policyId()!=null&&query.policyId().equals(new UUID(0,0))||cutoff==null||cutoff.isAfter(clock.instant()))throw fail(VALIDATION_FAILED);
        var owner=governance.get();String emptyVersion=digest(actor.accountId()+"|"+actor.realmId()+"|"+actor.environment()+"|"+actor.permissionVersion());
        if(owner==null||!Boolean.TRUE.equals(jdbc.queryForObject("SELECT to_regclass('merchant.application_policy') IS NOT NULL AND to_regclass('merchant.application_revision') IS NOT NULL",Boolean.class)))return new Capture("SOURCE_NOT_IMPLEMENTED","SUBMITTED_APPLICATION_SOURCE_REQUIRED",emptyVersion,digest("UNAVAILABLE"),List.of());
        var context=owner.context(actor);
        var policies=context.assignments().stream().filter(a->a.scopeType().equals("POLICY")&&a.permissions().contains("application.read")).map(GovernanceAssignment::policyId).distinct().sorted().toList();
        if(query.policyId()!=null&&!policies.contains(query.policyId()))throw fail(RESOURCE_NOT_FOUND);
        var versions=new ArrayList<String>();for(UUID policy:policies){var target=new Target(Kind.POLICY,policy);versions.add(owner.withScope(actor,target,scope->{scope.require("application.read",target);return scope.version();}));}
        String scopeVersion=digest(emptyVersion+"|"+versions);
        if(policies.isEmpty())return new Capture("POLICY_NOT_CONFIGURED","MERCHANT_APPLICATION_SCOPE_REQUIRED",scopeVersion,digest("NO_APPLICATION_SCOPE"),List.of());
        var selected=query.policyId()==null?policies:List.of(query.policyId());var args=new ArrayList<Object>();args.addAll(selected);args.add(java.sql.Timestamp.from(query.from()));args.add(java.sql.Timestamp.from(query.until()));args.add(java.sql.Timestamp.from(cutoff));
        String predicate="a.policy_id IN ("+String.join(",",Collections.nCopies(selected.size(),"?"))+") AND a.first_submitted_at>=? AND a.first_submitted_at<? AND a.first_submitted_at<=?";
        if(query.state()!=null){predicate+=" AND a.state=?";args.add(query.state());}
        var rows=jdbc.query("SELECT a.id,a.state,a.first_submitted_at,a.updated_at,a.terminal_at,(SELECT max(e.occurred_at) FROM merchant.governance_event e WHERE e.application_id=a.id) AS last_event,(SELECT max(e.occurred_at) FROM merchant.governance_event e WHERE e.application_id=a.id AND e.operation IN ('APPROVE','REJECT')) AS decision_event FROM merchant.application a WHERE "+predicate+" ORDER BY a.first_submitted_at DESC,a.id DESC LIMIT 100001",(r,n)->{
            String state=r.getString("state");Instant submitted=r.getTimestamp("first_submitted_at").toInstant(),terminal=r.getTimestamp("terminal_at")==null?null:r.getTimestamp("terminal_at").toInstant(),decision=r.getTimestamp("decision_event")==null?null:r.getTimestamp("decision_event").toInstant();
            if(!STATES.contains(state)||Set.of("APPROVED","REJECTED").contains(state)&&(terminal==null||!terminal.equals(decision)||terminal.isBefore(submitted)))throw fail(SERVICE_UNAVAILABLE);
            return new Row(r.getObject("id",UUID.class),state,submitted,r.getTimestamp("updated_at").toInstant(),Set.of("APPROVED","REJECTED").contains(state)?terminal:null,r.getTimestamp("last_event")==null?null:r.getTimestamp("last_event").toInstant());
        },args.toArray());
        if(rows.size()>100000||rows.stream().map(Row::id).distinct().count()!=rows.size())throw fail(SERVICE_UNAVAILABLE);
        reads.requireCurrent(actor);if(!context.authorityVersion().equals(owner.context(actor).authorityVersion()))throw fail(SNAPSHOT_EXPIRED);
        return new Capture("AVAILABLE","CANONICAL_FIRST_SUBMITTED_COHORT",scopeVersion,digest(query+"|"+scopeVersion+"|"+rows),rows);
    }
    @Override public Page page(Access access,ApplicationCohort query,Selector selector,int page,int size){
        admit(access);if(selector==null||!METRICS.contains(Objects.toString(selector.metric(),""))||page<0||page>10000||size<1||size>50)throw fail(VALIDATION_FAILED);
        var current=capture(access,query,selector.cutoff());if(!current.availability().equals("AVAILABLE"))throw fail(POLICY_UNAVAILABLE);
        if(!Objects.equals(current.resourceScopeVersion(),selector.resourceScopeVersion())||!Objects.equals(current.fingerprint(),selector.fingerprint()))throw fail(SNAPSHOT_EXPIRED);
        var rows=current.rows().stream().filter(r->selector.metric().equals("APPLICATIONS_SUBMITTED")||selector.metric().startsWith("APPLICATION_PROCESSING_")?selector.metric().equals("APPLICATIONS_SUBMITTED")||r.terminalAt()!=null:r.state().equals(selector.metric().substring("APPLICATIONS_".length()))).toList();
        long start=(long)page*size;return new Page(rows.stream().skip(start).limit(size).toList(),page,size,start+size<rows.size());
    }
}
