package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

/** One new customer-owned transaction; never nests existing SYSTEM Session commands. */
public final class CustomerAdmissions implements CustomerAdmission {
    private final IdentitySessionStore store;
    private final CustomerAccessVerifier verifier;
    private final CustomerRequestAdmission requestAdmission;
    private final Clock clock;
    private final ThreadLocal<State> current=new ThreadLocal<>();
    private static final class State {
        final Context context;boolean meaningful;
        State(Context context){this.context=context;}
    }
    public CustomerAdmissions(IdentitySessionStore store,CustomerAccessVerifier verifier,CustomerRequestAdmission requestAdmission,Clock clock) {
        this.store=Objects.requireNonNull(store);this.verifier=Objects.requireNonNull(verifier);this.clock=Objects.requireNonNull(clock);
        this.requestAdmission=Objects.requireNonNull(requestAdmission);
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
    @Override public <T>T execute(String encodedAccess,UUID requestId,Set<UUID> referencedAccounts,Function<Context,T> work) {
        if(current.get()!=null)throw new IllegalStateException("CUSTOMER_ADMISSION_REENTRY_REJECTED");
        if(requestId==null || referencedAccounts==null || referencedAccounts.size()>100 || referencedAccounts.stream().anyMatch(Objects::isNull) || work==null)throw invalid();
        var claim=verifier.verify(encodedAccess);
        if(claim==null || claim.accountId()==null || claim.sessionId()==null)throw invalid();
        requestAdmission.authorize(requestId,claim.sessionId());boolean[] meaningful={false};
        T result=store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            // Population lock first, every affected account in UUID order, then actor session.
            var ids=new TreeSet<UUID>(referencedAccounts);ids.add(claim.accountId());
            Map<UUID,IdentityAccount> locked=new HashMap<>();Map<UUID,Subject> subjects=new HashMap<>();
            for(UUID id:ids) {
                var account=accounts.lockAccount(id);
                if(account.isPresent()) {
                    var a=account.get();locked.put(id,a);var view=accounts.accountView(id);
                    boolean eligible=a.active() && a.activation()==IdentityAccount.Activation.VERIFIED && tx.eligibleRealm(id);
                    subjects.put(id,new Subject(id,view.displayName(),eligible?view.email():null,a.version().authVersion(),eligible));
                }
            }
            var actor=locked.get(claim.accountId());var subject=subjects.get(claim.accountId());
            if(actor==null || subject==null || !subject.eligible())throw invalid();
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(CustomerAdmissions::invalid);
            admit(claim,actor,session);
            var context=new Context(requestId,session.id(),subject,subjects);var state=new State(context);
            current.set(state);
            try {
                T value=work.apply(context);
                // Locks remain held through physical commit; deadline checked after all business writes.
                admit(claim,actor,session);requestAdmission.authorize(requestId,session.id());meaningful[0]=state.meaningful;return value;
            }finally {current.remove();}
        });
        if(meaningful[0])recordActivity(claim);
        return result;
    }
    private void admit(CustomerAccessVerifier.Claim claim,IdentityAccount account,IdentitySession session) {
        Instant time=clock.instant();
        try {session.admit(account,time);}catch(DomainViolation rejected){throw invalid();}
        if(claim.authVersion()!=account.version().authVersion() || !claim.accountId().equals(session.accountId())
            || !claim.sessionId().equals(session.id()) || claim.issuedAt()==null || claim.expiresAt()==null
            || time.isBefore(claim.issuedAt()) || !time.isBefore(claim.expiresAt())
            || claim.issuedAt().isBefore(session.createdAt().truncatedTo(ChronoUnit.SECONDS))
            || !claim.expiresAt().isAfter(claim.issuedAt()) || claim.expiresAt().isAfter(claim.issuedAt().plusSeconds(900))
            || claim.expiresAt().isAfter(session.absoluteExpiresAt()) || claim.expiresAt().isAfter(session.idleExpiresAt()))throw invalid();
    }
    @Override public void requireCurrent(Context context) {
        State state=current.get();if(state==null || state.context!=context)throw invalid();
    }
    @Override public void meaningfulAction(Context context) {requireCurrent(context);current.get().meaningful=true;}
    private void recordActivity(CustomerAccessVerifier.Claim claim) {
        // Separate post-commit owned transaction. Failure cannot pretend the business write rolled back.
        store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var account=accounts.lockAccount(claim.accountId()).orElseThrow(CustomerAdmissions::invalid);
            var session=accounts.lockSession(claim.sessionId()).orElseThrow(CustomerAdmissions::invalid);
            if(!tx.eligibleRealm(account.id()))throw invalid();
            admit(claim,account,session);
            tx.recordActivity(session,session.activity(account,IdentitySession.Activity.USER_ACTION,clock.instant()));return null;
        });
    }
}
