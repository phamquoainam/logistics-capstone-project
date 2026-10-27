package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import com.elogistic.backend.identity.internal.infrastructure.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Unit orchestration only. PostgreSQL purpose/FK/race evidence is a separate gate. */
class CustomerSessionApplicationTests {
    private final Instant now=Instant.parse("2026-10-27T00:00:00Z");
    private final UUID accountId=UUID.randomUUID(), requestId=UUID.randomUUID();
    private IdentitySessionStore store;
    private IdentitySessionStore.Transaction tx;
    private IdentityGrantStore.Transaction accounts;
    private SessionCredentials credentials;
    private SessionAdmission admission;
    private IdentitySessions customer;

    @BeforeEach void prepare() {
        store=mock(IdentitySessionStore.class);tx=mock(IdentitySessionStore.Transaction.class);
        accounts=mock(IdentityGrantStore.Transaction.class);credentials=mock(SessionCredentials.class);admission=mock(SessionAdmission.class);
        when(store.transaction(any())).thenAnswer(i->{Function<IdentitySessionStore.Transaction,Object> f=i.getArgument(0);return f.apply(tx);});
        when(tx.accounts()).thenReturn(accounts);when(tx.findUsername(anyString())).thenReturn(Optional.of(accountId));
        when(accounts.lockAccount(accountId)).thenReturn(Optional.of(new IdentityAccount(accountId,new Username("customer.opaque"),
            IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED,new AccountVersion(1,1),Set.of())));
        when(tx.passwordVerifier(accountId)).thenReturn("synthetic-verifier");when(credentials.verify(anyString(),any())).thenReturn(true);
        when(tx.eligibleRealm(accountId)).thenReturn(true);when(credentials.newRefreshToken()).thenReturn(new byte[43]);
        when(credentials.digest(any())).thenReturn(new byte[32]);
        when(accounts.accountView(accountId)).thenReturn(new IdentityGrantCommands.Account(accountId,"customer.opaque","Synthetic","person@example.invalid","ACTIVE",List.of(),1,true));
        customer=IdentitySessions.customer(store,credentials,admission,Clock.fixed(now,ZoneOffset.UTC));
    }
    private IdentitySessionCommands.Login login(String email) {return new IdentitySessionCommands.Login(email,"legacy",requestId);}
    @Test void verifiedCustomerWithNoSystemOrMerchantGrantCanOpenOwnSession() {
        try(var result=customer.loginIdentity(login("Person@example.invalid"))) {
            assertEquals(accountId,result.access().accountId());assertEquals(now.plusSeconds(900),result.access().expiresAt());
            assertTrue(result.user().grants().isEmpty());
        }
        verify(tx).findUsername("Person@example.invalid");verify(tx).insertSession(any());
    }
    @Test void emailLongerThanLegacyUsernameLimitUsesCustomerContract() {
        String email="x".repeat(60)+"@"+"y".repeat(40)+".example.invalid";
        try(var result=customer.loginIdentity(login(email))) {assertEquals(accountId,result.access().accountId());}
        verify(tx).findUsername(email);
    }
    @Test void systemLoginRetainsItsExactUsernameLimit() {
        var system=new IdentitySessions(store,credentials,admission,Clock.fixed(now,ZoneOffset.UTC));
        assertThrows(IdentityCommandFailure.class,()->system.loginIdentity(login("x".repeat(81)+"@example.invalid")));
        verifyNoInteractions(store,credentials);
    }
    @Test void phoneAndWhitespaceCannotEstablishCustomerIdentifier() {
        for(String invalid:List.of("0900000000"," person@example.invalid","person@example.invalid ","person\u00a0@example.invalid","person@example.invalid\n"))
            assertThrows(IdentityCommandFailure.class,()->customer.loginIdentity(login(invalid)));
        verifyNoInteractions(store,credentials);
    }
    @Test void exactEmailDoesNotCaseFoldTrimOrNormalize() {
        assertNotEquals(new CustomerEmail("Person@example.invalid"),new CustomerEmail("person@example.invalid"));
        assertEquals("Person@example.invalid",new CustomerEmail("Person@example.invalid").value());
    }
    @Test void identifierAndCredentialDiagnosticsAreRedacted() {
        assertFalse(new CustomerEmail("person@example.invalid").toString().contains("person"));
        assertFalse(login("person@example.invalid").toString().contains("legacy"));
    }
    @Test void staleOrMissingDerivedIdentifierDeniesEvenActiveAccount() {
        when(tx.eligibleRealm(accountId)).thenReturn(false);
        assertEquals(IdentityCommandFailure.Code.INVALID_CREDENTIALS,assertThrows(IdentityCommandFailure.class,
            ()->customer.loginIdentity(login("person@example.invalid"))).code());
        verify(tx,never()).insertSession(any());
    }
    @Test void unknownCustomerStillComparesPasswordWithoutCreatingAccount() {
        when(tx.findUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(IdentityCommandFailure.class,()->customer.loginIdentity(login("person@example.invalid")));
        verify(credentials).verify("legacy",null);verify(tx,never()).insertSession(any());
    }
    @Test void disabledIdentityAlwaysDeniesCustomerSession() {
        when(accounts.lockAccount(accountId)).thenReturn(Optional.of(new IdentityAccount(accountId,new Username("customer.opaque"),
            IdentityAccount.Status.DISABLED,IdentityAccount.Activation.VERIFIED,new AccountVersion(2,2),Set.of())));
        assertThrows(IdentityCommandFailure.class,()->customer.loginIdentity(login("person@example.invalid")));
        verify(tx,never()).insertSession(any());
    }
    private void rejectsAmbient() {
        DataSource source=mock(DataSource.class);var isolated=new JdbcIdentitySessionStore(source,IdentitySessionStorage.CUSTOMER);
        assertEquals("SESSION_TRANSACTION_OWNERSHIP_REQUIRED",assertThrows(IllegalStateException.class,
            ()->isolated.transaction(tx->{fail("must not enter caller-owned transaction");return null;})).getMessage());
        verifyNoInteractions(source);
    }
    @Test void customerStoreRejectsAmbientPhysicalTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {rejectsAmbient();} finally {TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void customerStoreRejectsAmbientSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        try {rejectsAmbient();} finally {TransactionSynchronizationManager.clearSynchronization();}
    }
    @Test void customerStoreRejectsPreboundCallerResources() {
        Object key=new Object();TransactionSynchronizationManager.bindResource(key,new Object());
        try {rejectsAmbient();} finally {TransactionSynchronizationManager.unbindResource(key);}
    }
}
