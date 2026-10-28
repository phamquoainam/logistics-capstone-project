package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.customer.CustomerAdmission.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Boundary/ordering/final-deadline tests; actual PG lock interleavings are tested separately. */
class CustomerAdmissionTests {
    private static final UUID A=UUID.fromString("11111111-1111-4111-8111-111111111111"), S=UUID.randomUUID(), R=UUID.randomUUID();
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private IdentitySessionStore store;private IdentitySessionStore.Transaction tx;private IdentityGrantStore.Transaction accounts;
    private CustomerAccessVerifier verifier;private CustomerRequestAdmission request;private CustomerAdmissions service;
    private IdentityAccount actor;private IdentitySession session;private AtomicReference<Instant> time;
    private CustomerAccessVerifier.Claim claim;
    @BeforeEach void prepare() {
        store=mock(IdentitySessionStore.class);tx=mock(IdentitySessionStore.Transaction.class);accounts=mock(IdentityGrantStore.Transaction.class);
        verifier=mock(CustomerAccessVerifier.class);request=mock(CustomerRequestAdmission.class);time=new AtomicReference<>(NOW);
        Clock clock=new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){return this;}public Instant instant(){return time.get();}};
        actor=new IdentityAccount(A,new Username("customer.synthetic"),IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED,new AccountVersion(1,1),Set.of());
        session=IdentitySession.open(S,actor,NOW);claim=new CustomerAccessVerifier.Claim(S,A,1,NOW,NOW.plusSeconds(900));
        when(verifier.verify("signed-customer-token")).thenReturn(claim);
        when(store.transaction(any())).thenAnswer(i->{Function<IdentitySessionStore.Transaction,Object> f=i.getArgument(0);return f.apply(tx);});
        when(tx.accounts()).thenReturn(accounts);when(accounts.lockAccount(A)).thenReturn(Optional.of(actor));when(accounts.lockSession(S)).thenReturn(Optional.of(session));
        when(accounts.accountView(A)).thenReturn(new IdentityGrantCommands.Account(A,"customer.synthetic","Synthetic","person@example.invalid","ACTIVE",List.of(),1,true));when(tx.eligibleRealm(A)).thenReturn(true);
        service=new CustomerAdmissions(store,verifier,request,clock);
    }
    private <T>T run(Function<Context,T> work){return service.execute("signed-customer-token",R,Set.of(),work);}
    @Test void callbackGetsOnlyCurrentSubjectInsideOwnedTransaction() {
        assertEquals(A,run(c->{service.requireCurrent(c);assertEquals("person@example.invalid",c.actor().verifiedEmail());return c.actor().accountId();}));
        var ordered=inOrder(accounts);ordered.verify(accounts).lockAdministration();ordered.verify(accounts).lockAccount(A);ordered.verify(accounts).accountView(A);ordered.verify(accounts).lockSession(S);
        verify(request,times(2)).authorize(R,S);verify(store,times(1)).transaction(any());
    }
    @Test void suppliedOrCapturedContextCannotBecomeAuthority() {
        Context captured=run(c->c);assertThrows(IdentityCommandFailure.class,()->service.requireCurrent(captured));
        run(c->{assertThrows(IdentityCommandFailure.class,()->service.requireCurrent(new Context(c.requestId(),c.sessionId(),c.actor(),c.subjects())));return null;});
    }
    @Test void callbackCannotReenterOrReuseContextOnAnotherThread() throws Exception {
        run(c->{assertThrows(IllegalStateException.class,()->run(x->null));
            var rejected=new AtomicBoolean();Thread thread=new Thread(()->{try{service.requireCurrent(c);}catch(IdentityCommandFailure e){rejected.set(true);}});
            thread.start();try{thread.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}assertFalse(thread.isAlive());assertTrue(rejected.get());return null;});
    }
    @Test void missingRequestEvidenceDeniesBeforeDatabaseOrBusinessWork() {
        doThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED)).when(request).authorize(R,S);
        assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));verifyNoInteractions(store);
    }
    @Test void invalidSystemOrDemoTokenCannotReachDatabase() {
        when(verifier.verify(anyString())).thenThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID));
        assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));verifyNoInteractions(store,request);
    }
    @Test void staleIdentifierAndRevokedSessionDenyBeforeBusinessWork() {
        when(tx.eligibleRealm(A)).thenReturn(false);assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));
        when(tx.eligibleRealm(A)).thenReturn(true);when(accounts.lockSession(S)).thenReturn(Optional.of(session.revoke(NOW)));
        assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));
    }
    @Test void staleAccountVersionDeniesEvenSignedUnexpiredToken() {
        when(accounts.lockAccount(A)).thenReturn(Optional.of(new IdentityAccount(A,actor.username(),actor.status(),actor.activation(),new AccountVersion(2,2),Set.of())));
        assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));
    }
    @Test void substitutedSessionAccountCannotEstablishAuthority() {
        when(accounts.lockSession(S)).thenReturn(Optional.of(new IdentitySession(S,UUID.randomUUID(),1,NOW,NOW.plusSeconds(28800),NOW,NOW.plusSeconds(1800),null)));
        assertThrows(IdentityCommandFailure.class,()->run(c->{fail();return null;}));
    }
    @Test void expiryDuringCallbackRejectsTransactionBeforeCommit() {
        assertThrows(IdentityCommandFailure.class,()->run(c->{time.set(NOW.plusSeconds(900));return "staged-business-write";}));
        verify(tx,never()).recordActivity(any(),any());
    }
    @Test void earliestProofDeadlineCannotBeExtendedAndIsCheckedAfterBusinessWork(){
        var rejected=assertThrows(IdentityCommandFailure.class,()->run(c->{service.requireDeadline(c,NOW.plusSeconds(60));service.requireDeadline(c,NOW.plusSeconds(120));time.set(NOW.plusSeconds(60));return "staged";}));
        assertEquals(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED,rejected.code());verify(tx,never()).recordActivity(any(),any());
        Context captured=run(c->c);assertThrows(IdentityCommandFailure.class,()->service.requireDeadline(captured,NOW.plusSeconds(180)));
    }
    @Test void csrfRevocationDuringCallbackRejectsFinalDecision() {
        doNothing().doThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.CSRF_REJECTED)).when(request).authorize(R,S);
        assertThrows(IdentityCommandFailure.class,()->run(c->"staged-business-write"));
    }
    @Test void readOrReceiptDoesNotExtendIdle() {
        run(c->"read-only-or-authorized-receipt");verify(store,times(1)).transaction(any());verify(tx,never()).recordActivity(any(),any());
    }
    @Test void meaningfulNewCommandUpdatesActivityOnlyInSecondOwnedTransaction() {
        time.set(NOW.plusSeconds(5));run(c->{service.meaningfulAction(c);return "new-command";});
        verify(store,times(2)).transaction(any());verify(tx).recordActivity(eq(session),argThat(s->s.lastActivityAt().equals(NOW.plusSeconds(5))));
    }
    @Test void businessFailureClearsContextAndNeverUpdatesIdle() {
        var captured=new AtomicReference<Context>();assertThrows(IllegalStateException.class,()->run(c->{captured.set(c);service.meaningfulAction(c);throw new IllegalStateException("BUSINESS_FAILURE");}));
        assertThrows(IdentityCommandFailure.class,()->service.requireCurrent(captured.get()));verify(store,times(1)).transaction(any());verify(tx,never()).recordActivity(any(),any());
    }
    @Test void nullBindingHasNoPermissiveDefault() {
        assertThrows(NullPointerException.class,()->new CustomerAdmissions(store,verifier,null,Clock.systemUTC()));
    }
    @Test void subjectAndClaimDiagnosticsDoNotExposeEmailOrAccessValue() {
        run(c->{assertFalse(c.toString().contains("person"));assertFalse(c.actor().toString().contains("person"));return null;});
        assertFalse(claim.toString().contains(A.toString()));
    }
}
