package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;
import static com.elogistic.backend.identity.internal.application.port.SessionAdmission.Operation.*;

/** Application-only session transitions. No JWT, cookies, Demo fallback, network or automatic registration. */
public final class IdentitySessions implements IdentitySessionCommands {
    private final IdentitySessionStore store;
    private final SessionCredentials credentials;
    private final SessionAdmission admission;
    private final Clock clock;
    private final boolean customerIdentifier;
    public IdentitySessions(IdentitySessionStore store, SessionCredentials credentials, SessionAdmission admission, Clock clock) {
        this(store, credentials, admission, clock, false);
    }
    private IdentitySessions(IdentitySessionStore store, SessionCredentials credentials, SessionAdmission admission, Clock clock, boolean customerIdentifier) {
        this.store=Objects.requireNonNull(store);this.credentials=Objects.requireNonNull(credentials);
        this.admission=Objects.requireNonNull(admission);this.clock=Objects.requireNonNull(clock);
        this.customerIdentifier=customerIdentifier;
    }
    /** Same session state machine; composition MUST supply the isolated customer store/admission. */
    public static IdentitySessions customer(IdentitySessionStore store, SessionCredentials credentials, SessionAdmission admission, Clock clock) {
        return new IdentitySessions(store, credentials, admission, clock, true);
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static IdentityCommandFailure fail(IdentityCommandFailure.Code code) { return new IdentityCommandFailure(code); }
    @Override public Result loginIdentity(Login c) {
        if(c==null || c.requestId()==null)throw fail(VALIDATION_FAILED);
        admission.authorize(LOGIN,c.requestId(),null,false);
        try {if(customerIdentifier)new CustomerEmail(c.username());else new Username(c.username());PasswordPolicy.validateLogin(c.password());}
        catch(DomainViolation invalid) {throw fail(VALIDATION_FAILED);}
        return safely(tx->{
            var accounts=tx.accounts();accounts.lockAdministration();
            var id=tx.findUsername(c.username());
            var account=id.flatMap(accounts::lockAccount);
            String verifier=id.map(tx::passwordVerifier).orElse(null);
            boolean matches=credentials.verify(c.password(),verifier);
            if(!matches || account.isEmpty() || !account.get().active()
                || account.get().activation()!=IdentityAccount.Activation.VERIFIED || !tx.eligibleRealm(account.get().id()))throw fail(INVALID_CREDENTIALS);
            Instant time=now();var session=IdentitySession.open(UUID.randomUUID(),account.get(),time);
            byte[] token=credentials.newRefreshToken(),digest=null;
            try {
                digest=credentials.digest(token);
                tx.insertSession(session);tx.insertRefresh(UUID.randomUUID(),null,session,digest,time);
                tx.audit(account.get().id(),c.requestId(),"IDENTITY_LOGIN",time);
                return result(tx,session,account.get(),token,time);
            } finally {wipe(token);wipe(digest);}
        });
    }
    @Override public Result refreshIdentity(Token c) {
        require(c);byte[] raw=c.value(),digest=null;
        try {
            digest=credentials.digest(raw);final byte[] lookup=digest;
            // Replay revocation MUST commit before the outward denial is raised.
            Result result=safely(tx->{
                var a=tx.accounts();a.lockAdministration();
                var old=tx.findRefresh(lookup).orElseThrow(()->fail(SESSION_INVALID));
                admission.authorize(REFRESH,c.requestId(),old.sessionId(),true);
                var account=a.sessionAccount(old.sessionId()).flatMap(a::lockAccount).orElseThrow(()->fail(SESSION_INVALID));
                var session=a.lockSession(old.sessionId()).orElseThrow(()->fail(SESSION_INVALID));
                Instant time=now();admit(session,account,time,tx);
                if(old.revokedAt()!=null || !time.isBefore(old.expiresAt()))throw fail(SESSION_INVALID);
                var family=tx.family(session).orElseThrow(()->fail(SESSION_INVALID));
                UUID next=UUID.randomUUID();RefreshFamily.Rotation rotation;
                try {rotation=family.rotate(session,account,old.id(),next,time);}
                catch(DomainViolation invalid) {throw fail(SESSION_INVALID);}
                if(rotation.outcome()==RefreshFamily.Outcome.REPLAY_REVOKED) {
                    tx.revokeFamily(session.id(),time);tx.audit(account.id(),c.requestId(),"IDENTITY_REFRESH_REPLAY",time);return null;
                }
                if(rotation.outcome()!=RefreshFamily.Outcome.ROTATED)throw fail(SESSION_INVALID);
                byte[] token=credentials.newRefreshToken(),hash=null;
                try {
                    hash=credentials.digest(token);tx.consumeRefresh(old.id(),time);
                    tx.insertRefresh(next,old.id(),session,hash,time);tx.audit(account.id(),c.requestId(),"IDENTITY_REFRESH",time);
                    return result(tx,session,account,token,time);
                } finally {wipe(token);wipe(hash);}
            });
            if(result==null)throw fail(REFRESH_REPLAY);
            return result;
        } finally {wipe(raw);wipe(digest);}
    }
    @Override public void logoutIdentity(Token c) {
        require(c);byte[] raw=c.value(),digest=null;
        try {
            if(raw==null) {admission.authorize(LOGOUT,c.requestId(),null,false);return;}
            try {digest=credentials.digest(raw);}
            catch(IdentityCommandFailure invalid) {throw fail(CSRF_REJECTED);}
            final byte[] lookup=digest;
            store.transaction(tx->{
                var a=tx.accounts();a.lockAdministration();
                var token=tx.findRefresh(lookup).orElseThrow(()->fail(CSRF_REJECTED));
                admission.authorize(LOGOUT,c.requestId(),token.sessionId(),true);
                var account=a.sessionAccount(token.sessionId()).flatMap(a::lockAccount).orElseThrow(()->fail(CSRF_REJECTED));
                var session=a.lockSession(token.sessionId()).orElseThrow(()->fail(CSRF_REJECTED));
                if(!session.accountId().equals(account.id()))throw fail(CSRF_REJECTED);
                Instant time=now();if(time.isBefore(session.createdAt()))throw fail(CSRF_REJECTED);
                if(tx.revokeFamily(session.id(),time))tx.audit(account.id(),c.requestId(),"IDENTITY_LOGOUT",time);
                return null;
            });
        } finally {wipe(raw);wipe(digest);}
    }
    private void admit(IdentitySession s,IdentityAccount a,Instant time,IdentitySessionStore.Transaction tx) {
        try {s.admit(a,time);}
        catch(DomainViolation invalid) {throw fail(SESSION_INVALID);}
        if(a.activation()!=IdentityAccount.Activation.VERIFIED || !tx.eligibleRealm(a.id()))throw fail(SESSION_INVALID);
    }
    private Result result(IdentitySessionStore.Transaction tx,IdentitySession s,IdentityAccount a,byte[] raw,Instant time) {
        return new Result(new AccessGrant(s.id(),a.id(),a.version().authVersion(),time,s.accessExpiresAt(a,time)),
            tx.accounts().accountView(a.id()),s.absoluteExpiresAt(),raw);
    }
    private Result safely(java.util.function.Function<IdentitySessionStore.Transaction,Result> work) {
        Result[] produced={null};
        try {return store.transaction(tx->produced[0]=work.apply(tx));}
        catch(RuntimeException | Error failure) {if(produced[0]!=null)produced[0].close();throw failure;}
    }
    private static void require(Token c) {if(c==null || c.requestId()==null)throw fail(VALIDATION_FAILED);}
    private static void wipe(byte[] b) {if(b!=null)Arrays.fill(b,(byte)0);}
}
