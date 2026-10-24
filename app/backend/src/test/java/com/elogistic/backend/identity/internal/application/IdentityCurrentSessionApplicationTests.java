package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Context;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;
import static com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission.Operation.*;

/** Orchestration/denial controls; PostgreSQL tests separately prove locking and persistence. */
class IdentityCurrentSessionApplicationTests {
    private static final Instant NOW=Instant.parse("2026-10-24T00:00:00Z");
    private static final UUID A=UUID.randomUUID(),SID=UUID.randomUUID(),R=UUID.randomUUID(),REQUEST=UUID.randomUUID();
    private IdentitySessionStore store;
    private IdentitySessionStore.Transaction tx;
    private IdentityGrantStore.Transaction accounts;
    private CurrentSessionAdmission admission;
    private Clock clock;
    private IdentityCurrentSessions app;
    private IdentityAccount account;
    private IdentitySession session;
    private IdentityGrantCommands.Account view;
    @BeforeEach void fixture() {
        store=mock(IdentitySessionStore.class);tx=mock(IdentitySessionStore.Transaction.class);accounts=mock(IdentityGrantStore.Transaction.class);
        admission=mock(CurrentSessionAdmission.class);clock=mock(Clock.class);when(clock.instant()).thenReturn(NOW);
        account=new IdentityAccount(A,new Username("synthetic.current"),IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED,new AccountVersion(1,1),Set.of(new Grant(new RoleCode("IDENTITY_MANAGEMENT"),new ResourceScope(ResourceScope.Type.IDENTITY,R))));
        session=IdentitySession.open(SID,account,NOW);
        view=new IdentityGrantCommands.Account(A,"synthetic.current","Synthetic","current@example.invalid","ACTIVE",List.of(new IdentityGrantCommands.GrantData("IDENTITY_MANAGEMENT","IDENTITY",R)),1,true);
        when(admission.admit(any(),eq(REQUEST))).thenReturn(context(SID,A,1,NOW,NOW.plusSeconds(900)));
        when(store.transaction(any())).thenAnswer(i->{Function<IdentitySessionStore.Transaction,Object> f=i.getArgument(0);return f.apply(tx);});
        when(tx.accounts()).thenReturn(accounts);when(accounts.sessionAccount(SID)).thenReturn(Optional.of(A));when(accounts.lockAccount(A)).thenReturn(Optional.of(account));when(accounts.lockSession(SID)).thenReturn(Optional.of(session));when(tx.eligibleRealm(A)).thenReturn(true);when(accounts.accountView(A)).thenReturn(view);
        app=new IdentityCurrentSessions(store,admission,clock);
    }
    private Context context(UUID sid,UUID id,long version,Instant issued,Instant expires) {return new Context(sid,id,version,issued,expires);}
    private void denies(Runnable operation) {var failure=assertThrows(IdentityCommandFailure.class,operation::run);assertEquals(SESSION_INVALID,failure.code());assertEquals("SESSION_INVALID",failure.getMessage());}
    private void deniesBoth() {denies(()->app.getIdentityMe(REQUEST));denies(()->app.recordMeaningfulActivity(REQUEST));verify(tx,never()).recordActivity(any(),any());verify(accounts,never()).accountView(any());}

    @Test void currentMeReturnsApprovedAccountWithoutActivityMutation() {
        assertEquals(view,app.getIdentityMe(REQUEST));verify(admission).admit(CURRENT_SESSION,REQUEST);verify(accounts).lockAdministration();verify(tx,never()).recordActivity(any(),any());verify(tx,never()).audit(any(),any(),any(),any());
    }
    @Test void meaningfulActivityUsesDistinctTrustedAdmissionAndDomainTransition() {
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));app.recordMeaningfulActivity(REQUEST);
        verify(admission).admit(MEANINGFUL_ACTIVITY,REQUEST);verify(tx).recordActivity(session,session.activity(account,IdentitySession.Activity.USER_ACTION,NOW.plusSeconds(60)));verify(accounts,never()).accountView(any());verify(tx,never()).audit(any(),any(),any(),any());
    }
    @Test void repeatedActivityAtSameInstantDoesNotWrite() {app.recordMeaningfulActivity(REQUEST);verify(tx,never()).recordActivity(any(),any());}
    @Test void missingAdmissionCannotCompose() {assertThrows(NullPointerException.class,()->new IdentityCurrentSessions(store,null,clock));}
    @Test void missingRequestIdCannotReachAdmissionOrStore() {denies(()->app.getIdentityMe(null));denies(()->app.recordMeaningfulActivity(null));verifyNoInteractions(admission,store);}
    @Test void rejectedTrustedAdmissionNeverReachesStore() {
        when(admission.admit(any(),eq(REQUEST))).thenThrow(new IdentityCommandFailure(SESSION_INVALID));deniesBoth();verifyNoInteractions(store);
    }
    @Test void authenticatedReadDoesNotAuthorizeMeaningfulActivity() {
        when(admission.admit(eq(MEANINGFUL_ACTIVITY),eq(REQUEST))).thenThrow(new IdentityCommandFailure(SESSION_INVALID));assertEquals(view,app.getIdentityMe(REQUEST));denies(()->app.recordMeaningfulActivity(REQUEST));verify(tx,never()).recordActivity(any(),any());
    }
    @Test void malformedTrustedContextsFailWithoutDatabaseAccess() {
        for(Context c:Arrays.asList(null,context(null,A,1,NOW,NOW.plusSeconds(900)),context(SID,null,1,NOW,NOW.plusSeconds(900)),context(SID,A,0,NOW,NOW.plusSeconds(900)),context(SID,A,1,null,NOW.plusSeconds(900)),context(SID,A,1,NOW,null))) {
            when(admission.admit(any(),eq(REQUEST))).thenReturn(c);deniesBoth();
        }verifyNoInteractions(store);
    }
    @Test void expiredFutureOrOverlongAccessFactsFailClosed() {
        for(Context c:List.of(context(SID,A,1,NOW.minusSeconds(900),NOW),context(SID,A,1,NOW.plusSeconds(1),NOW.plusSeconds(900)),context(SID,A,1,NOW,NOW.plusSeconds(901)),context(SID,A,1,NOW,NOW))) {
            when(admission.admit(any(),eq(REQUEST))).thenReturn(c);deniesBoth();
        }verifyNoInteractions(store);
    }
    @Test void accessExpiryIsRecheckedAfterWaitingForLocks() {
        when(clock.instant()).thenReturn(NOW,NOW.plusSeconds(900));denies(()->app.getIdentityMe(REQUEST));verify(accounts).lockSession(SID);verify(accounts,never()).accountView(any());
    }
    @Test void crossAccountSessionSubstitutionFailsBeforeAccountDisclosure() {
        when(accounts.sessionAccount(SID)).thenReturn(Optional.of(UUID.randomUUID()));deniesBoth();verify(accounts,never()).lockAccount(any());
    }
    @Test void missingPersistedSessionFailsClosed() {when(accounts.sessionAccount(SID)).thenReturn(Optional.empty());deniesBoth();}
    @Test void staleTrustedOrPersistedAuthorizationVersionFailsClosed() {
        when(admission.admit(any(),eq(REQUEST))).thenReturn(context(SID,A,2,NOW,NOW.plusSeconds(900)));deniesBoth();
        when(admission.admit(any(),eq(REQUEST))).thenReturn(context(SID,A,1,NOW,NOW.plusSeconds(900)));when(accounts.lockAccount(A)).thenReturn(Optional.of(new IdentityAccount(A,account.username(),account.status(),account.activation(),new AccountVersion(2,2),account.grants())));deniesBoth();
    }
    @Test void revokedSessionFailsClosed() {when(accounts.lockSession(SID)).thenReturn(Optional.of(session.revoke(NOW)));deniesBoth();}
    @Test void exactIdleOrAbsoluteExpiryCannotBeExtended() {
        for(long elapsed:new long[]{1800,28800}) {Instant time=NOW.plusSeconds(elapsed);when(clock.instant()).thenReturn(time);when(admission.admit(any(),eq(REQUEST))).thenReturn(context(SID,A,1,time,time.plusSeconds(900)));deniesBoth();}
    }
    @Test void disabledPendingAndUnverifiedAccountsFailClosed() {
        for(var status:List.of(IdentityAccount.Status.DISABLED,IdentityAccount.Status.PENDING_ACTIVATION)) {when(accounts.lockAccount(A)).thenReturn(Optional.of(new IdentityAccount(A,account.username(),status,IdentityAccount.Activation.UNVERIFIED,account.version(),account.grants())));deniesBoth();}
        assertThrows(DomainViolation.class,()->new IdentityAccount(A,account.username(),IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.UNVERIFIED,account.version(),account.grants()));
    }
    @Test void inactiveRealmOrUnsupportedGrantContextFailsClosed() {when(tx.eligibleRealm(A)).thenReturn(false);deniesBoth();}
    @Test void meaningfulActivityIsCappedByAbsoluteDeadline() {
        Instant time=NOW.plusSeconds(28740);var near=new IdentitySession(SID,A,1,NOW,NOW.plusSeconds(28800),NOW.plusSeconds(28200),NOW.plusSeconds(28800),null);
        when(accounts.lockSession(SID)).thenReturn(Optional.of(near));when(clock.instant()).thenReturn(time);when(admission.admit(any(),eq(REQUEST))).thenReturn(context(SID,A,1,time,time.plusSeconds(60)));
        app.recordMeaningfulActivity(REQUEST);verify(tx).recordActivity(near,near.activity(account,IdentitySession.Activity.USER_ACTION,time));
    }
    @Test void backwardClockCannotAdvanceOrRewindActivity() {
        var newer=session.activity(account,IdentitySession.Activity.USER_ACTION,NOW.plusSeconds(60));when(accounts.lockSession(SID)).thenReturn(Optional.of(newer));deniesBoth();
    }
    @Test void activityPersistenceFailureIsPropagatedForTransactionRollback() {
        when(clock.instant()).thenReturn(NOW.plusSeconds(60));doThrow(new IllegalStateException("SYNTHETIC_WRITE_FAILURE")).when(tx).recordActivity(any(),any());assertThrows(IllegalStateException.class,()->app.recordMeaningfulActivity(REQUEST));verify(accounts,never()).accountView(any());
    }
    @Test void currentSessionBoundaryExposesNoClientIdentityActivityFlagOrSecrets() {
        for(var method:IdentityCurrentSessionCommands.class.getDeclaredMethods())assertArrayEquals(new Class<?>[]{UUID.class},method.getParameterTypes());
        assertEquals("CurrentSessionContext[redacted]",context(SID,A,1,NOW,NOW.plusSeconds(900)).toString());assertEquals("IdentityAccountResult[redacted]",app.getIdentityMe(REQUEST).toString());
        assertEquals(Set.of("id","username","displayName","email","status","grants","version","emailVerified"),Arrays.stream(IdentityGrantCommands.Account.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
    }
}
