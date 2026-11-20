package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.staff.StaffAdmission;
import com.elogistic.backend.identity.internal.application.IdentityAccessRules;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import java.time.Clock;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/** Same Identity population/account/session lock protocol, fixed Staff actor and typed read references. */
public final class JdbcStaffAdmission implements StaffAdmission {
    private final JdbcIdentitySessionStore store;
    private final JdbcTemplate jdbc;
    private final CustomerAccessVerifier verifier;
    private final CustomerRequestAdmission requests;
    private final Clock clock;
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private static final class State {final Context context;boolean meaningful;State(Context context){this.context=context;}}

    public JdbcStaffAdmission(DataSource runtime,CustomerAccessVerifier verifier,CustomerRequestAdmission requests,Clock clock) {
        store=new JdbcIdentitySessionStore(runtime,IdentitySessionStorage.STAFF);jdbc=new JdbcTemplate(runtime);
        this.verifier=Objects.requireNonNull(verifier);this.requests=Objects.requireNonNull(requests);this.clock=Objects.requireNonNull(clock);
    }
    @Override public <T>T execute(String encodedAccess,UUID requestId,Set<UUID> referencedAccounts,Function<Context,T> work) {
        if(current.get()!=null)throw new IllegalStateException("STAFF_ADMISSION_REENTRY_REJECTED");
        if(requestId==null || referencedAccounts==null || referencedAccounts.size()>100 || referencedAccounts.stream().anyMatch(Objects::isNull) || work==null)throw invalid();
        var claim=verifier.verify(encodedAccess);
        if(claim==null || claim.sessionId()==null || claim.accountId()==null)throw invalid();
        requests.authorize(requestId,claim.sessionId());boolean[] meaningful={false};
        T result=store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var ids=new TreeSet<>(referencedAccounts);ids.add(claim.accountId());
            Map<UUID,Subject> subjects=new HashMap<>();Map<UUID,IdentityAccount> locked=new HashMap<>();
            for(UUID id:ids) {
                var account=accounts.lockAccount(id);
                if(account.isEmpty()){subjects.put(id,new Subject(id,"",PrincipalType.NONE,0,false));continue;}
                var a=account.get();locked.put(id,a);var view=accounts.accountView(id);
                boolean staff=Boolean.TRUE.equals(jdbc.queryForObject("SELECT identity.support_staff_registered(?)",Boolean.class,id));
                boolean eligibleStaff=staff&&tx.eligibleRealm(id);
                boolean customer=!staff&&Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM identity.customer_identifier c JOIN identity.user_account a ON a.id=c.account_id WHERE c.account_id=? AND c.status='ACTIVE' AND c.exact_email=a.email COLLATE \"C\" AND a.status='ACTIVE' AND a.email_verified)",Boolean.class,id));
                subjects.put(id,new Subject(id,view.displayName(),staff?PrincipalType.STAFF:customer?PrincipalType.CUSTOMER:PrincipalType.NONE,a.version().authVersion(),a.active()&&a.activation()==IdentityAccount.Activation.VERIFIED&&(eligibleStaff||customer)));
            }
            var actor=subjects.get(claim.accountId());var account=locked.get(claim.accountId());
            if(actor==null || account==null || actor.principalType()!=PrincipalType.STAFF || !actor.eligible())throw invalid();
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(JdbcStaffAdmission::invalid);
            IdentityAccessRules.admit(claim,account,session,clock.instant());
            var context=new Context(requestId,session.id(),actor,subjects);var state=new State(context);current.set(state);
            try {
                T value=work.apply(context);
                if(!tx.eligibleRealm(account.id()))throw invalid();
                IdentityAccessRules.admit(claim,account,session,clock.instant());requests.authorize(requestId,session.id());
                meaningful[0]=state.meaningful;return value;
            } finally {current.remove();}
        });
        if(meaningful[0])store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var account=accounts.lockAccount(claim.accountId()).orElseThrow(JdbcStaffAdmission::invalid);
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(JdbcStaffAdmission::invalid);
            if(!tx.eligibleRealm(account.id()))throw invalid();IdentityAccessRules.admit(claim,account,session,clock.instant());
            tx.recordActivity(session,session.activity(account,com.elogistic.backend.identity.internal.domain.IdentitySession.Activity.USER_ACTION,clock.instant()));return null;
        });
        return result;
    }
    @Override public void requireCurrent(Context context){var state=current.get();if(state==null || state.context!=context)throw invalid();}
    @Override public void meaningfulAction(Context context){requireCurrent(context);current.get().meaningful=true;}
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
}
