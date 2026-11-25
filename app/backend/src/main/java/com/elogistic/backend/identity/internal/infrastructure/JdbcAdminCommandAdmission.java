package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import java.sql.Connection;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

/** Existing Identity population protocol, then subjects and session; no nested/read-only mutation. */
public final class JdbcAdminCommandAdmission implements AdminCommandAdmission {
    private static final long POPULATION_LOCK=0x4944454e54495459L;
    private final JdbcTemplate jdbc;
    private final DataSource source;
    private final TransactionTemplate write;
    private final CurrentSessionAdmission admission;
    private final AdminSessionPolicy policy;
    private final Clock clock;
    private final ThreadLocal<Object> current=new ThreadLocal<>();
    private record Issued(DataSource source,AdminCommandContext context){}
    private static final ThreadLocal<Issued> ISSUED=new ThreadLocal<>();
    static void requireOwned(DataSource source,AdminCommandContext context){var issued=ISSUED.get();if(issued==null||issued.source()!=source||issued.context()!=context)throw denied();context.requireCurrent();}
    @Override public void requireCurrent(AdminCommandContext context){requireOwned(source,context);}

    public JdbcAdminCommandAdmission(DataSource source,CurrentSessionAdmission admission,AdminSessionPolicy policy,Clock clock){
        this.source=Objects.requireNonNull(source);jdbc=new JdbcTemplate(source);this.admission=Objects.requireNonNull(admission);
        this.policy=Objects.requireNonNull(policy);this.clock=Objects.requireNonNull(clock);
        var manager=new JdbcTransactionManager(source);manager.setValidateExistingTransaction(true);
        write=new TransactionTemplate(manager);write.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);write.setTimeout(15);
    }
    private static AdminFailure denied(){return new AdminFailure("ACCESS_DENIED");}
    private static AdminFailure invalid(){return new AdminFailure("SESSION_INVALID");}
    @Override public <T>T execute(UUID request,ReferencedSubjects references,Function<AdminCommandContext,T> work){
        if(request==null || references==null || work==null || references.customers().size()+references.admins().size()>100
            || !Collections.disjoint(references.customers(),references.admins()))throw new AdminFailure("VALIDATION_FAILED");
        if(current.get()!=null || TransactionSynchronizationManager.isActualTransactionActive()
            || TransactionSynchronizationManager.isSynchronizationActive() || !TransactionSynchronizationManager.getResourceMap().isEmpty())throw denied();
        var claim=admission.admit(CurrentSessionAdmission.Operation.CURRENT_SESSION,request);
        if(claim==null || claim.accountId()==null || claim.sessionId()==null || claim.authVersion()<1)throw invalid();
        try{return write.execute(status->{
            jdbc.execute("SET LOCAL lock_timeout='5s'");jdbc.execute("SET LOCAL statement_timeout='10s'");
            jdbc.query("SELECT pg_advisory_xact_lock(?)",(r,n)->0,POPULATION_LOCK);
            Set<UUID> ids=new TreeSet<>(references.customers());ids.addAll(references.admins());ids.add(claim.accountId());
            Set<UUID> locked=new HashSet<>();
            for(UUID id:ids)if(!jdbc.query("SELECT id FROM identity.user_account WHERE id=? FOR UPDATE",(r,n)->r.getObject(1,UUID.class),id).isEmpty())locked.add(id);
            if(!locked.contains(claim.accountId()))throw invalid();
            var authority=policy.current(claim.accountId()).orElseThrow(JdbcAdminCommandAdmission::denied);
            // This command port is bounded to Merchant governance, unlike generic six-READ admission.
            if(!authority.permissions().containsAll(Set.of(AdminReadContext.ENTRY,AdminReadContext.MERCHANT)))throw denied();
            if(jdbc.query("SELECT id FROM identity.auth_session WHERE id=? AND account_id=? FOR UPDATE",(r,n)->r.getObject(1,UUID.class),claim.sessionId(),claim.accountId()).size()!=1)throw invalid();
            Map<UUID,CustomerAdmission.Subject> customers=new HashMap<>();
            for(UUID id:references.customers()){
                if(!locked.contains(id))continue;
                jdbc.query("""
                    SELECT a.id,a.display_name,a.email,a.auth_version,
                     (a.status='ACTIVE' AND a.email_verified AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE "C") AS eligible
                    FROM identity.user_account a LEFT JOIN identity.customer_identifier c ON c.account_id=a.id AND c.status='ACTIVE' WHERE a.id=?
                    """,r->{boolean eligible=r.getBoolean("eligible");customers.put(id,new CustomerAdmission.Subject(id,r.getString("display_name"),eligible?r.getString("email"):null,r.getLong("auth_version"),eligible));},id);
            }
            Map<UUID,AdminCommandContext.AdminEligibility> admins=new HashMap<>();
            for(UUID id:references.admins()){
                var target=locked.contains(id)?policy.current(id):Optional.<AdminSessionPolicy.Authority>empty();
                admins.put(id,target.filter(a->a.realmId().equals(authority.realmId())&&a.permissions().contains(AdminReadContext.MERCHANT)).map(a->new AdminCommandContext.AdminEligibility(id,a.realmId(),a.displayName(),a.version(),true))
                    .orElseGet(()->new AdminCommandContext.AdminEligibility(id,null,"",null,false)));
            }
            Object token=new Object(),physicalTransaction=TransactionSynchronizationManager.getResource(source);current.set(token);
            AdminCommandContext context=new AdminCommandContext(){
                public UUID accountId(){return authority.accountId();} public UUID realmId(){return authority.realmId();}
                public UUID sessionId(){return claim.sessionId();} public UUID requestId(){return request;}
                public long authVersion(){return claim.authVersion();} public String permissionVersion(){return authority.version();}
                public String environment(){return policy.environment();}
                public void requireCurrent(){
                    if(current.get()!=token || TransactionSynchronizationManager.getResource(source)!=physicalTransaction || !TransactionSynchronizationManager.isActualTransactionActive()
                        || TransactionSynchronizationManager.isCurrentTransactionReadOnly()
                        || !Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),Connection.TRANSACTION_READ_COMMITTED))throw denied();
                    validateSession(claim);
                    if(!policy.current(accountId()).map(a->a.version().equals(authority.version())).orElse(false))throw denied();
                }
                public CustomerAdmission.Subject customer(UUID id){requireCurrent();var value=customers.get(id);if(value==null)throw denied();return value;}
                public AdminEligibility admin(UUID id){requireCurrent();var value=admins.get(id);if(value==null)throw denied();return value;}
                @Override public String toString(){return "AdminCommandContext[redacted]";}
            };
            ISSUED.set(new Issued(source,context));try{context.requireCurrent();T result=work.apply(context);context.requireCurrent();return result;}finally{ISSUED.remove();current.remove();}
        });}catch(DataAccessException unavailable){throw new AdminFailure("SERVICE_UNAVAILABLE");}
    }
    private void validateSession(CurrentSessionAdmission.Context claim){
        Instant now=clock.instant();
        if(claim.issuedAt()==null || claim.expiresAt()==null || now.isBefore(claim.issuedAt()) || !now.isBefore(claim.expiresAt())
            || !claim.expiresAt().isAfter(claim.issuedAt()) || Duration.between(claim.issuedAt(),claim.expiresAt()).compareTo(Duration.ofMinutes(15))>0)throw invalid();
        boolean valid=Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT EXISTS(SELECT 1 FROM identity.auth_session s JOIN identity.user_account a ON a.id=s.account_id
             WHERE s.id=? AND s.account_id=? AND s.audience='SYSTEM' AND s.revoked_at IS NULL
              AND s.auth_version=? AND a.auth_version=s.auth_version AND a.status='ACTIVE' AND a.email_verified
              AND s.created_at<=? AND s.absolute_expires_at>=? AND s.idle_expires_at>=?)
            """,Boolean.class,claim.sessionId(),claim.accountId(),claim.authVersion(),java.sql.Timestamp.from(claim.issuedAt()),java.sql.Timestamp.from(claim.expiresAt()),java.sql.Timestamp.from(claim.expiresAt())));
        if(!valid)throw invalid();
    }
}
