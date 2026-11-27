package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.staff.OperationalStaffAdmission;
import com.elogistic.backend.identity.internal.application.IdentityAccessRules;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Same Identity locks/session engine; issued-object and physical resource lifetime are mandatory. */
public final class JdbcOperationalStaffAdmission implements OperationalStaffAdmission {
    private final DataSource source;
    private final JdbcIdentitySessionStore store;
    private final JdbcTemplate jdbc;
    private final CustomerAccessVerifier verifier;
    private final CustomerRequestAdmission requests;
    private final Clock clock;
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private static final class State {
        final Context context; final Object resource; final CustomerAccessVerifier.Claim claim;
        final IdentityAccount account; final IdentitySession session; boolean meaningful;
        State(Context c,Object r,CustomerAccessVerifier.Claim claim,IdentityAccount a,IdentitySession s) {
            context=c;resource=r;this.claim=claim;account=a;session=s;
        }
    }
    public JdbcOperationalStaffAdmission(DataSource source,CustomerAccessVerifier verifier,CustomerRequestAdmission requests,Clock clock) {
        this.source=Objects.requireNonNull(source);store=new JdbcIdentitySessionStore(source,IdentitySessionStorage.OPS);
        jdbc=new JdbcTemplate(source);this.verifier=Objects.requireNonNull(verifier);this.requests=Objects.requireNonNull(requests);this.clock=Objects.requireNonNull(clock);
    }
    @Override public <T>T execute(String token,UUID request,Function<Context,T> work) {
        if(current.get()!=null||request==null||work==null)throw invalid();
        var claim=verifier.verify(token);if(claim==null||claim.accountId()==null||claim.sessionId()==null)throw invalid();
        requests.authorize(request,claim.sessionId());boolean[] meaningful={false};
        T result=store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var account=accounts.lockAccount(claim.accountId()).orElseThrow(JdbcOperationalStaffAdmission::invalid);
            if(!tx.eligibleRealm(account.id()))throw invalid();
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(JdbcOperationalStaffAdmission::invalid);
            IdentityAccessRules.admit(claim,account,session,clock.instant());
            var context=new Context(request,session.id(),new Subject(account.id(),accounts.accountView(account.id()).displayName(),account.version().authVersion()));
            var state=new State(context,TransactionSynchronizationManager.getResource(source),claim,account,session);current.set(state);
            TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization(){
                @Override public void beforeCommit(boolean readOnly){verify(state);}
            });
            try {requireCurrent(context);T value=work.apply(context);requireCurrent(context);meaningful[0]=state.meaningful;return value;}
            finally {current.remove();}
        });
        if(meaningful[0])store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var account=accounts.lockAccount(claim.accountId()).orElseThrow(JdbcOperationalStaffAdmission::invalid);
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(JdbcOperationalStaffAdmission::invalid);
            if(!tx.eligibleRealm(account.id()))throw invalid();IdentityAccessRules.admit(claim,account,session,clock.instant());
            tx.recordActivity(session,session.activity(account,IdentitySession.Activity.USER_ACTION,clock.instant()));return null;
        });
        return result;
    }
    @Override public void requireCurrent(Context context) {
        var s=current.get();if(s==null||s.context!=context)throw invalid();verify(s);
    }
    private void verify(State s) {
        var context=s.context;if(s.resource==null||!TransactionSynchronizationManager.isActualTransactionActive()
            ||TransactionSynchronizationManager.getResource(source)!=s.resource)throw invalid();
        IdentityAccessRules.admit(s.claim,s.account,s.session,clock.instant());requests.authorize(context.requestId(),context.sessionId());
        if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.operations_staff_eligible(?) AND EXISTS(SELECT 1 FROM identity.auth_session s JOIN identity.user_account a ON a.id=s.account_id WHERE s.id=? AND s.account_id=? AND s.audience='OPS' AND s.revoked_at IS NULL AND a.auth_version=? AND s.auth_version=a.auth_version)",Boolean.class,context.actor().accountId(),context.sessionId(),context.actor().accountId(),context.actor().securityVersion())))throw invalid();
    }
    @Override public void meaningfulAction(Context context) {requireCurrent(context);current.get().meaningful=true;}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
}
