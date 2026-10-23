package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.api.IdentitySessionCommands.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import com.elogistic.backend.identity.internal.infrastructure.ProtectedSessionCredentials;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Mock orchestration assertions do not claim PostgreSQL atomicity or HTTP/JWT admission. */
class IdentitySessionApplicationTests {
    private static final Instant NOW=Instant.parse("2026-10-23T00:05:00Z");
    private static final UUID A=UUID.randomUUID(),SID=UUID.randomUUID(),TID=UUID.randomUUID(),R=UUID.randomUUID(),REQUEST=UUID.randomUUID();
    private IdentitySessionStore store;
    private IdentitySessionStore.Transaction tx;
    private IdentityGrantStore.Transaction accounts;
    private SessionCredentials crypto;
    private SessionAdmission admission;
    private IdentitySessions app;
    private IdentityAccount account;
    private IdentitySession session;
    @BeforeEach void fixture() {
        store=mock(IdentitySessionStore.class);tx=mock(IdentitySessionStore.Transaction.class);accounts=mock(IdentityGrantStore.Transaction.class);
        crypto=mock(SessionCredentials.class);admission=mock(SessionAdmission.class);
        account=new IdentityAccount(A,new Username("synthetic.session"),IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED,new AccountVersion(1,1),Set.of(new Grant(new RoleCode("IDENTITY_MANAGEMENT"),new ResourceScope(ResourceScope.Type.IDENTITY,R))));
        session=IdentitySession.open(SID,account,NOW);
        when(store.transaction(any())).thenAnswer(i->{Function<IdentitySessionStore.Transaction,Object> f=i.getArgument(0);return f.apply(tx);});
        when(tx.accounts()).thenReturn(accounts);when(tx.findUsername("synthetic.session")).thenReturn(Optional.of(A));when(accounts.lockAccount(A)).thenReturn(Optional.of(account));when(tx.passwordVerifier(A)).thenReturn("synthetic-verifier");
        when(tx.eligibleRealm(A)).thenReturn(true);when(crypto.verify(anyString(),any())).thenReturn(true);
        when(crypto.newRefreshToken()).thenAnswer(i->new byte[43]);when(crypto.digest(any())).thenAnswer(i->new byte[32]);
        when(accounts.accountView(A)).thenReturn(new IdentityGrantCommands.Account(A,"synthetic.session","Synthetic","session@example.invalid","ACTIVE",List.of(),1,true));
        when(tx.findRefresh(any())).thenReturn(Optional.of(new IdentitySessionStore.Refresh(TID,SID,NOW.plusSeconds(28800),null,null)));
        when(accounts.sessionAccount(SID)).thenReturn(Optional.of(A));when(accounts.lockSession(SID)).thenReturn(Optional.of(session));
        when(tx.family(session)).thenReturn(Optional.of(RefreshFamily.issue(session,account,TID,NOW)));
        app=new IdentitySessions(store,crypto,admission,Clock.fixed(NOW,ZoneOffset.UTC));
    }
    private Login login(String password) {return new Login("synthetic.session",password,REQUEST);}
    private Token token() {return new Token(new byte[43],REQUEST);}
    private void rejected(IdentityCommandFailure.Code code,Runnable work) {assertEquals(code,assertThrows(IdentityCommandFailure.class,work::run).code());}
    @Test void loginCreatesSessionRefreshAndAuditInOneTransaction() {
        try(var r=app.loginIdentity(login("legacy"))) {assertEquals(A,r.access().accountId());assertEquals(NOW.plusSeconds(900),r.access().expiresAt());assertEquals(NOW.plusSeconds(28800),r.absoluteExpiresAt());}
        verify(tx).insertSession(any());verify(tx).insertRefresh(any(),isNull(),any(),any(),eq(NOW));verify(tx).audit(A,REQUEST,"IDENTITY_LOGIN",NOW);
    }
    @Test void wrongPasswordHasNoWrites() {
        when(crypto.verify(anyString(),any())).thenReturn(false);rejected(INVALID_CREDENTIALS,()->app.loginIdentity(login("legacy")));verify(tx,never()).insertSession(any());
    }
    @Test void unknownAccountStillPerformsCredentialComparison() {
        when(tx.findUsername(any())).thenReturn(Optional.empty());rejected(INVALID_CREDENTIALS,()->app.loginIdentity(login("legacy")));verify(crypto).verify("legacy",null);verify(tx,never()).insertSession(any());
    }
    @Test void pendingAndDisabledAccountsCannotLogin() {
        for(var state:List.of(IdentityAccount.Status.PENDING_ACTIVATION,IdentityAccount.Status.DISABLED)) {
            var blocked=new IdentityAccount(A,account.username(),state,IdentityAccount.Activation.UNVERIFIED,account.version(),account.grants());
            when(accounts.lockAccount(A)).thenReturn(Optional.of(blocked));rejected(INVALID_CREDENTIALS,()->app.loginIdentity(login("legacy")));
        }verify(tx,never()).insertSession(any());
    }
    @Test void inactiveRealmDeniesLogin() {
        when(tx.eligibleRealm(A)).thenReturn(false);rejected(INVALID_CREDENTIALS,()->app.loginIdentity(login("legacy")));verify(tx,never()).insertSession(any());
    }
    @Test void missingAdmissionCannotCompose() {assertThrows(NullPointerException.class,()->new IdentitySessions(store,crypto,null,Clock.systemUTC()));}
    @Test void deniedAdmissionPrecedesCredentialLookup() {
        doThrow(new IdentityCommandFailure(CSRF_REJECTED)).when(admission).authorize(any(),any(),any(),anyBoolean());
        rejected(CSRF_REJECTED,()->app.loginIdentity(login("legacy")));verifyNoInteractions(store,crypto);
    }
    @Test void invalidUsernameAndOversizedLoginPasswordFailWithoutStore() {
        rejected(VALIDATION_FAILED,()->app.loginIdentity(new Login(" ","legacy",REQUEST)));
        rejected(VALIDATION_FAILED,()->app.loginIdentity(login("x".repeat(73))));verifyNoInteractions(store);
    }
    @Test void refreshRotatesWithoutExtendingSessionDeadlines() {
        try(var c=token();var r=app.refreshIdentity(c)){assertEquals(NOW.plusSeconds(900),r.access().expiresAt());assertEquals(session.absoluteExpiresAt(),r.absoluteExpiresAt());}
        verify(tx).consumeRefresh(TID,NOW);verify(tx).insertRefresh(any(),eq(TID),eq(session),any(),eq(NOW));verify(tx,never()).insertSession(any());
    }
    @Test void consumedRefreshCommitsRevocationBeforeReplayDenial() {
        UUID next=UUID.randomUUID();when(tx.findRefresh(any())).thenReturn(Optional.of(new IdentitySessionStore.Refresh(TID,SID,session.absoluteExpiresAt(),NOW,null)));
        when(tx.family(session)).thenReturn(Optional.of(new RefreshFamily(SID,next,session.absoluteExpiresAt(),Set.of(TID),RefreshFamily.State.ACTIVE)));
        try(var c=token()){rejected(REFRESH_REPLAY,()->app.refreshIdentity(c));}
        verify(tx).revokeFamily(SID,NOW);verify(tx).audit(A,REQUEST,"IDENTITY_REFRESH_REPLAY",NOW);verify(tx,never()).insertRefresh(any(),any(),any(),any(),any());
    }
    @Test void unknownRefreshDoesNotResolveAnAccount() {
        when(tx.findRefresh(any())).thenReturn(Optional.empty());try(var c=token()){rejected(SESSION_INVALID,()->app.refreshIdentity(c));}verify(accounts,never()).sessionAccount(any());
    }
    @Test void staleAuthorizationVersionDeniesRefresh() {
        when(accounts.lockAccount(A)).thenReturn(Optional.of(new IdentityAccount(A,account.username(),account.status(),account.activation(),new AccountVersion(2,2),account.grants())));
        try(var c=token()){rejected(SESSION_INVALID,()->app.refreshIdentity(c));}verify(tx,never()).consumeRefresh(any(),any());
    }
    @Test void expiredAndRevokedSessionsCannotRefresh() {
        when(accounts.lockSession(SID)).thenReturn(Optional.of(session.revoke(NOW)));try(var c=token()){rejected(SESSION_INVALID,()->app.refreshIdentity(c));}
        app=new IdentitySessions(store,crypto,admission,Clock.fixed(NOW.plusSeconds(1800),ZoneOffset.UTC));when(accounts.lockSession(SID)).thenReturn(Optional.of(session));
        try(var c=token()){rejected(SESSION_INVALID,()->app.refreshIdentity(c));}verify(tx,never()).consumeRefresh(any(),any());
    }
    @Test void sessionAccountSubstitutionCannotRefresh() {
        when(accounts.lockSession(SID)).thenReturn(Optional.of(new IdentitySession(SID,UUID.randomUUID(),1,NOW,NOW.plusSeconds(28800),NOW,NOW.plusSeconds(1800),null)));
        try(var c=token()){rejected(SESSION_INVALID,()->app.refreshIdentity(c));}verify(tx,never()).consumeRefresh(any(),any());
    }
    @Test void logoutWithoutCookieStillRequiresAdmission() {
        try(var c=new Token(null,REQUEST)){app.logoutIdentity(c);}verify(admission).authorize(SessionAdmission.Operation.LOGOUT,REQUEST,null,false);verifyNoInteractions(store);
        when(crypto.digest(any())).thenThrow(new IdentityCommandFailure(SESSION_INVALID));
        try(var c=token()){rejected(CSRF_REJECTED,()->app.logoutIdentity(c));}
        verifyNoInteractions(store);
    }
    @Test void logoutRevokesOnlyResolvedSessionAndIsIdempotent() {
        when(tx.revokeFamily(SID,NOW)).thenReturn(true,false);try(var c=token()){app.logoutIdentity(c);app.logoutIdentity(c);}
        verify(tx,times(2)).revokeFamily(SID,NOW);verify(tx,times(1)).audit(A,REQUEST,"IDENTITY_LOGOUT",NOW);
    }
    @Test void expiredLogoutCanRevokeWithoutRestoringSession() {
        when(accounts.lockSession(SID)).thenReturn(Optional.of(session.revoke(NOW)));try(var c=token()){app.logoutIdentity(c);}verify(tx).revokeFamily(SID,NOW);verify(tx,never()).insertSession(any());
    }
    @Test void tokenBuffersAndResultsAreCopiedRedactedAndClosed() {
        byte[] raw=new byte[43];Arrays.fill(raw,(byte)65);var c=new Token(raw,REQUEST);byte[] copy=c.value();copy[0]=0;assertEquals(65,c.value()[0]);assertFalse(c.toString().contains("AAAA"));c.close();assertThrows(IllegalStateException.class,c::value);
        var r=app.loginIdentity(login("legacy"));assertEquals("IdentitySessionResult[redacted]",r.toString());r.close();assertThrows(IllegalStateException.class,r::refreshToken);
    }
    @Test void refreshMaterialIsRandomCanonicalAndDigested() {
        var real=new ProtectedSessionCredentials();byte[] a=real.newRefreshToken(),b=real.newRefreshToken();
        try {assertEquals(43,a.length);assertFalse(Arrays.equals(a,b));assertEquals(32,real.digest(a).length);assertArrayEquals(real.digest(a),real.digest(a));assertFalse(Arrays.equals(a,real.digest(a)));assertThrows(IdentityCommandFailure.class,()->real.digest(new byte[43]));}
        finally {Arrays.fill(a,(byte)0);Arrays.fill(b,(byte)0);}
    }
    @Test void legacyPasswordUsesBcryptWithoutNewPasswordMinimum() {
        var real=new ProtectedSessionCredentials();String hash=new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode("legacy");
        assertTrue(real.verify("legacy",hash));assertFalse(real.verify("wrong",hash));assertFalse(real.verify("legacy",null));assertFalse(real.verify("legacy","invalid"));
    }

    private void rejectsUnownedStoreTransaction(javax.sql.DataSource source) {
        var entered=new java.util.concurrent.atomic.AtomicBoolean();
        var store=new com.elogistic.backend.identity.internal.infrastructure.JdbcIdentitySessionStore(source);
        var failure=assertThrows(IllegalStateException.class,()->store.transaction(tx->{entered.set(true);return null;}));
        assertEquals("SESSION_TRANSACTION_OWNERSHIP_REQUIRED",failure.getMessage());
        assertFalse(entered.get());verifyNoInteractions(source);
    }
    @Test void storeRejectsActualAmbientTransactionBeforeJdbcAccess() {
        var source=mock(javax.sql.DataSource.class);
        assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {rejectsUnownedStoreTransaction(source);}
        finally {org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void storeRejectsAmbientSynchronizationBeforeJdbcAccess() {
        var source=mock(javax.sql.DataSource.class);
        assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive());
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {rejectsUnownedStoreTransaction(source);}
        finally {org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void storeRejectsBoundDataSourceBeforeJdbcAccess() {
        var source=mock(javax.sql.DataSource.class);
        org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(source,new Object());
        try {rejectsUnownedStoreTransaction(source);}
        finally {org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(source);}
    }
    @Test void storeRejectsUnknownBoundResourceBeforeJdbcAccess() {
        var source=mock(javax.sql.DataSource.class);var key=new Object();
        org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(key,new Object());
        try {rejectsUnownedStoreTransaction(source);}
        finally {org.springframework.transaction.support.TransactionSynchronizationManager.unbindResource(key);}
    }
}
