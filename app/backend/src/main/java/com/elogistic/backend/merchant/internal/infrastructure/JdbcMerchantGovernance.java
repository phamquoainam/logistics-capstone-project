package com.elogistic.backend.merchant.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.merchant.api.*;
import com.elogistic.backend.merchant.api.MerchantGovernance.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.elogistic.backend.merchant.api.GovernanceFailure.Code.*;

/** Joins the current Identity physical transaction; never manufactures a command actor or scope. */
public final class JdbcMerchantGovernance implements MerchantGovernance {
    private static final Set<String> CODES=Set.of("application.read","application.evidence.read","application.assign",
        "application.review","application.decide","org.read","org.suspend","org.reinstate","audit.read","task.read");
    private final DataSource source;private final JdbcTemplate jdbc;private final Clock clock;
    private final AdminAdmission reads;private final AdminCommandAdmission commands;
    private final ThreadLocal<Object> active=new ThreadLocal<>();
    public JdbcMerchantGovernance(DataSource source,Clock clock,AdminAdmission reads,AdminCommandAdmission commands){this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.clock=Objects.requireNonNull(clock);this.reads=Objects.requireNonNull(reads);this.commands=Objects.requireNonNull(commands);}
    private static GovernanceFailure denied(){return new GovernanceFailure(ACCESS_DENIED);}
    @Override public <T>T withScope(AdminReadContext actor,Target target,Function<GovernanceScope,T> work){
        reads.requireCurrent(actor);actor.require(AdminReadContext.ENTRY);actor.require(AdminReadContext.MERCHANT);
        if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly())throw denied();
        return scoped(actor.accountId(),actor.realmId(),actor.environment(),actor.permissionVersion(),()->reads.requireCurrent(actor),target,work,false);
    }
    /** Approved audit/task-only projections do not inherit Merchant/KYC READ. */
    <T>T withProjectionScope(AdminReadContext actor,Target target,Function<GovernanceScope,T> work){
        reads.requireCurrent(actor);actor.require(AdminReadContext.ENTRY);
        if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly())throw denied();
        return scoped(actor.accountId(),actor.realmId(),actor.environment(),actor.permissionVersion(),()->reads.requireCurrent(actor),target,work,false);
    }
    @Override public <T>T withScope(AdminCommandContext actor,Target target,Function<GovernanceScope,T> work){
        commands.requireCurrent(actor);
        if(TransactionSynchronizationManager.isCurrentTransactionReadOnly())throw denied();
        return scoped(actor.accountId(),actor.realmId(),actor.environment(),actor.permissionVersion(),()->commands.requireCurrent(actor),target,work,true);
    }
    @Override public GovernanceContext context(AdminReadContext actor){
        reads.requireCurrent(actor);actor.require(AdminReadContext.ENTRY);
        if(!TransactionSynchronizationManager.hasResource(source)||!TransactionSynchronizationManager.isCurrentTransactionReadOnly()||!Set.of("LOCAL","TEST").contains(actor.environment()))throw denied();
        var items=jdbc.query("SELECT s.* FROM merchant.business_reviewer_scope s LEFT JOIN merchant.application_policy p ON p.id=s.policy_id WHERE s.account_id=? AND s.realm_id=? AND s.state='ACTIVE' AND s.expires_at>? AND (s.scope_type='ORGANIZATION' OR (p.state='PUBLISHED' AND p.environment=? AND p.valid_from<=? AND (p.valid_to IS NULL OR p.valid_to>?))) ORDER BY s.id LIMIT 101",(r,n)->new GovernanceAssignment(r.getObject("id",UUID.class),r.getString("scope_type"),r.getObject("policy_id",UUID.class),r.getObject("organization_id",UUID.class),Arrays.asList((String[])r.getArray("permissions").getArray()),r.getLong("version"),r.getTimestamp("expires_at").toInstant()),actor.accountId(),actor.realmId(),java.sql.Timestamp.from(clock.instant()),actor.environment(),java.sql.Timestamp.from(clock.instant()),java.sql.Timestamp.from(clock.instant()));
        if(items.size()>100)throw new GovernanceFailure(SERVICE_UNAVAILABLE);
        String version;try{version=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((actor.permissionVersion()+"|"+items).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException unavailable){throw new GovernanceFailure(SERVICE_UNAVAILABLE);}
        reads.requireCurrent(actor);return new GovernanceContext(actor.accountId(),actor.realmId(),version,items);
    }
    private record Assignment(UUID id,long version,UUID policy,String permissions,String publishedDigest,String policyState,long policyVersion,Instant validFrom,Instant validTo,Instant expiresAt){}
    private List<Assignment> assignments(UUID actor,UUID realm,String environment,Target target){
        String predicate=switch(target.kind()){
            case POLICY->"s.scope_type='POLICY' AND s.policy_id=?";
            case APPLICATION->"s.scope_type='POLICY' AND s.policy_id=(SELECT a.policy_id FROM merchant.application a WHERE a.id=?)";
            // Only an immutable, authoritative activation can provide a policy scope.
            case ORGANIZATION->"((s.scope_type='ORGANIZATION' AND s.organization_id=?) OR (s.scope_type='POLICY' AND s.policy_id=(SELECT x.policy_id FROM merchant.application_activation x WHERE x.organization_id=? AND merchant.application_origin_admitted(x.organization_id))))";
        };
        List<Object> args=new ArrayList<>(List.of(actor,realm));args.add(target.id());if(target.kind()==Kind.ORGANIZATION)args.add(target.id());args.add(environment);
        return jdbc.query("SELECT s.id,s.version,s.policy_id,array_to_string(s.permissions,',') AS permissions,encode(p.published_digest,'hex') AS digest,p.state,p.version AS policy_version,p.valid_from,p.valid_to,s.expires_at FROM merchant.business_reviewer_scope s LEFT JOIN merchant.application_policy p ON p.id=s.policy_id WHERE s.account_id=? AND s.realm_id=? AND s.state='ACTIVE' AND "+predicate+" AND (s.scope_type='ORGANIZATION' OR p.environment=?) ORDER BY s.id",
            (r,n)->new Assignment(r.getObject("id",UUID.class),r.getLong("version"),r.getObject("policy_id",UUID.class),r.getString("permissions"),r.getString("digest"),r.getString("state"),r.getLong("policy_version"),r.getTimestamp("valid_from")==null?null:r.getTimestamp("valid_from").toInstant(),r.getTimestamp("valid_to")==null?null:r.getTimestamp("valid_to").toInstant(),r.getTimestamp("expires_at").toInstant()),args.toArray());
    }
    private List<Assignment> current(UUID actor,UUID realm,String environment,Target target){
        Instant now=clock.instant();return assignments(actor,realm,environment,target).stream().filter(s->now.isBefore(s.expiresAt())&&(s.policy()==null||"PUBLISHED".equals(s.policyState())&&!now.isBefore(s.validFrom())&&(s.validTo()==null||now.isBefore(s.validTo())))).toList();
    }
    private static String fingerprint(String identity,List<Assignment> rows){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((identity+"|"+rows).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.GeneralSecurityException unavailable){throw new GovernanceFailure(SERVICE_UNAVAILABLE);}
    }
    private <T>T scoped(UUID actor,UUID realm,String environment,String identityVersion,Runnable admission,Target target,Function<GovernanceScope,T> work,boolean write){
        if(active.get()!=null||!Set.of("LOCAL","TEST").contains(environment)||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.hasResource(source))throw denied();
        Objects.requireNonNull(target);Objects.requireNonNull(work);Object physical=TransactionSynchronizationManager.getResource(source);
        if(write)jdbc.query("SELECT pg_advisory_xact_lock(?)",(r,n)->0,JdbcCustomerProfileStore.CONTROL_LOCK);
        var rows=current(actor,realm,environment,target);if(rows.isEmpty())throw new GovernanceFailure(RESOURCE_NOT_FOUND);
        String version=fingerprint(identityVersion,rows);Object token=new Object();active.set(token);
        Runnable validate=()->{
            admission.run();if(active.get()!=token||TransactionSynchronizationManager.getResource(source)!=physical)throw denied();
            if(!version.equals(fingerprint(identityVersion,current(actor,realm,environment,target))))throw denied();
        };
        GovernanceScope scope=new GovernanceScope(){
            public void require(String permission,Target requested){
                validate.run();if(!target.equals(requested)||!CODES.contains(permission)||rows.stream().noneMatch(s->Arrays.asList(s.permissions().split(",")).contains(permission)))throw denied();
            }
            public String version(){validate.run();return version;}
            @Override public String toString(){return "GovernanceScope[redacted]";}
        };
        try{validate.run();T result=work.apply(scope);validate.run();return result;}finally{active.remove();}
    }
}
