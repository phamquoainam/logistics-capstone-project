package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

/** Population lock precedes MVCC snapshot acquisition, so a waited-for revocation cannot be stale. */
public final class JdbcAdminAdmission implements AdminAdmission {
    private static final long LOCK=0x4944454e54495459L;
    private final DataSource source;private final CurrentSessionAdmission admission;private final Supplier<UUID> requestId;
    private final AdminSessionPolicy policy;private final Clock clock;private final JdbcTemplate jdbc;private final TransactionTemplate read;
    private final ThreadLocal<Object> current=new ThreadLocal<>();
    private record Issued(DataSource source,AdminReadContext context){}
    private static final ThreadLocal<Issued> ISSUED=new ThreadLocal<>();
    /** Preserve domain-owned failures without importing another module's exception types. */
    private static final class CallbackFailure extends RuntimeException {
        private final RuntimeException original;
        private CallbackFailure(RuntimeException original){super("ADMIN_CALLBACK_FAILURE");this.original=original;}
    }
    @Override public void requireCurrent(AdminReadContext context){
        var issued=ISSUED.get();
        if(issued==null||issued.source()!=source||issued.context()!=context)throw new AdminFailure("ACCESS_DENIED");
        context.requireCurrent();
    }
    public JdbcAdminAdmission(DataSource source,CurrentSessionAdmission admission,Supplier<UUID> requestId,AdminSessionPolicy policy,Clock clock){
        this.source=source;this.admission=admission;this.requestId=requestId;this.policy=policy;this.clock=clock;jdbc=new JdbcTemplate(source);
        read=new TransactionTemplate(new JdbcTransactionManager(source));read.setReadOnly(true);read.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);read.setTimeout(12);
    }
    @Override public <T>T execute(Function<AdminReadContext,T> work){
        if(current.get()!=null||TransactionSynchronizationManager.isActualTransactionActive()||TransactionSynchronizationManager.isSynchronizationActive()||!TransactionSynchronizationManager.getResourceMap().isEmpty())throw new AdminFailure("ACCESS_DENIED");
        var claim=admission.admit(CurrentSessionAdmission.Operation.CURRENT_SESSION,requestId.get());
        // A dedicated, bounded connection holds the *existing* population lock before starting RR.
        // The callback's regular DataSource transaction owns every domain read. Never lock after its snapshot.
        try(Connection lock=source.getConnection()){
            boolean held=false;
            try(var s=lock.prepareStatement("SELECT pg_try_advisory_lock(?)")){
                s.setQueryTimeout(3);s.setLong(1,LOCK);try(var rows=s.executeQuery()){held=rows.next()&&rows.getBoolean(1);}
            }
            if(!held)throw new AdminFailure("SERVICE_UNAVAILABLE");
            try{return read.execute(status->{
                jdbc.execute("SET LOCAL statement_timeout='5s'");
                var authority=policy.current(claim.accountId()).orElseThrow(()->new AdminFailure("ACCESS_DENIED"));
                var expiry=jdbc.query("""
                    SELECT s.created_at,s.absolute_expires_at,s.idle_expires_at
                    FROM identity.auth_session s JOIN identity.user_account a ON a.id=s.account_id
                    WHERE s.id=? AND s.account_id=? AND s.audience='SYSTEM' AND s.revoked_at IS NULL
                     AND s.auth_version=? AND a.auth_version=s.auth_version AND a.status='ACTIVE' AND a.email_verified
                    """,(r,n)->new Instant[]{r.getTimestamp(1).toInstant(),r.getTimestamp(2).toInstant(),r.getTimestamp(3).toInstant()},claim.sessionId(),claim.accountId(),claim.authVersion());
                if(expiry.size()!=1)throw new AdminFailure("SESSION_INVALID");
                var limits=expiry.getFirst();Object identity=new Object(),physical=TransactionSynchronizationManager.getResource(source);current.set(identity);
                AdminReadContext context=new AdminReadContext(){
                    public UUID accountId(){return authority.accountId();}public UUID realmId(){return authority.realmId();}public String displayName(){return authority.displayName();}public String environment(){return policy.environment();}public String permissionVersion(){return authority.version();}public Set<String> permissions(){return authority.permissions();}
                    public void requireCurrent(){
                        Instant now=clock.instant();
                        if(current.get()!=identity||physical==null||TransactionSynchronizationManager.getResource(source)!=physical||!TransactionSynchronizationManager.isActualTransactionActive()||!TransactionSynchronizationManager.isCurrentTransactionReadOnly()||!Objects.equals(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel(),Connection.TRANSACTION_REPEATABLE_READ))throw new AdminFailure("ACCESS_DENIED");
                        if(claim.issuedAt()==null||claim.expiresAt()==null||claim.issuedAt().isBefore(limits[0])||now.isBefore(claim.issuedAt())||!now.isBefore(claim.expiresAt())||claim.expiresAt().isAfter(limits[1])||claim.expiresAt().isAfter(limits[2])||!claim.expiresAt().isAfter(claim.issuedAt())||Duration.between(claim.issuedAt(),claim.expiresAt()).compareTo(Duration.ofMinutes(15))>0)throw new AdminFailure("SESSION_INVALID");
                    }
                };
                ISSUED.set(new Issued(source,context));
                try{
                    context.require(AdminReadContext.ENTRY);T result;
                    try{result=work.apply(context);}
                    catch(org.springframework.dao.DataAccessException unavailable){throw new AdminFailure("SERVICE_UNAVAILABLE");}
                    catch(RuntimeException domainFailure){throw new CallbackFailure(domainFailure);}
                    context.requireCurrent();return result;
                }finally{ISSUED.remove();current.remove();}
            });}finally{
                try(var s=lock.prepareStatement("SELECT pg_advisory_unlock(?)")){s.setLong(1,LOCK);try(var rows=s.executeQuery()){if(!rows.next()||!rows.getBoolean(1))throw new AdminFailure("SERVICE_UNAVAILABLE");}}
            }
        }catch(CallbackFailure domainFailure){throw domainFailure.original;}
        catch(AdminFailure known){throw known;}catch(Exception unavailable){throw new AdminFailure("SERVICE_UNAVAILABLE");}
    }
}
