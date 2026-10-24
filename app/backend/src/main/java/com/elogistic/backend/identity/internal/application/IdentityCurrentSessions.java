package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.IdentityCurrentSessionCommands;
import com.elogistic.backend.identity.api.IdentityGrantCommands.Account;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context;
import com.elogistic.backend.identity.internal.application.port.IdentitySessionStore;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.SESSION_INVALID;
import static com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Operation.*;

/** Authoritative current account and idle accounting only; no HTTP, JWT signing or audit feed. */
public final class IdentityCurrentSessions implements IdentityCurrentSessionCommands {
    private final IdentitySessionStore store;
    private final CurrentSessionAdmission admission;
    private final Clock clock;
    public IdentityCurrentSessions(IdentitySessionStore store, CurrentSessionAdmission admission, Clock clock) {
        this.store=Objects.requireNonNull(store);this.admission=Objects.requireNonNull(admission);this.clock=Objects.requireNonNull(clock);
    }
    @Override public Account getIdentityMe(UUID requestId) { return execute(requestId,false); }
    @Override public void recordMeaningfulActivity(UUID requestId) { execute(requestId,true); }

    private Account execute(UUID requestId,boolean meaningful) {
        if(requestId==null)throw invalid();
        Context context=admission.admit(meaningful?MEANINGFUL_ACTIVITY:CURRENT_SESSION,requestId);
        validate(context,now());
        return store.transaction(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var owner=accounts.sessionAccount(context.sessionId()).orElseThrow(IdentityCurrentSessions::invalid);
            if(!owner.equals(context.accountId()))throw invalid();
            var account=accounts.lockAccount(owner).orElseThrow(IdentityCurrentSessions::invalid);
            var session=accounts.lockSession(context.sessionId()).orElseThrow(IdentityCurrentSessions::invalid);
            // Recheck time AFTER locks: a token/session can expire while admission is waiting.
            Instant time=now();validate(context,time);
            if(!session.id().equals(context.sessionId()) || !session.accountId().equals(owner)
                || context.authVersion()!=session.authVersion() || context.authVersion()!=account.version().authVersion()
                || context.issuedAt().isBefore(session.createdAt()) || context.expiresAt().isAfter(session.absoluteExpiresAt())
                || context.expiresAt().isAfter(session.idleExpiresAt())
                || account.activation()!=IdentityAccount.Activation.VERIFIED || !tx.eligibleRealm(owner))throw invalid();
            try {
                session.admit(account,time);
                if(meaningful) {
                    var updated=session.activity(account,IdentitySession.Activity.USER_ACTION,time);
                    if(!updated.equals(session))tx.recordActivity(session,updated);
                    return null;
                }
            } catch(DomainViolation rejected) { throw invalid(); }
            return accounts.accountView(owner);
        });
    }
    private void validate(Context c,Instant time) {
        if(c==null || c.sessionId()==null || c.accountId()==null || c.authVersion()<1 || c.issuedAt()==null || c.expiresAt()==null
            || !c.expiresAt().isAfter(c.issuedAt()) || java.time.Duration.between(c.issuedAt(),c.expiresAt()).compareTo(IdentitySession.ACCESS)>0
            || time.isBefore(c.issuedAt()) || !time.isBefore(c.expiresAt()))throw invalid();
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static IdentityCommandFailure invalid() {return new IdentityCommandFailure(SESSION_INVALID);}
}
