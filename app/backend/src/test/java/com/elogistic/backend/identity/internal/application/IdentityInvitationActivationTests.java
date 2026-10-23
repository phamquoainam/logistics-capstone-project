package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static com.elogistic.backend.identity.api.IdentityCommandFailure.Code.*;

/** Application orchestration unit controls; mocks do not prove PostgreSQL atomicity or cryptography. */
class IdentityInvitationActivationTests {
    private static final Instant NOW=Instant.parse("2026-10-23T00:05:00Z");
    private static final UUID ACCOUNT=UUID.randomUUID(),PROOF=UUID.randomUUID(),REALM=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String PASSWORD="Synthetic-only-new-password";
    private IdentityInvitationStore store;
    private IdentityInvitationStore.Transaction tx;
    private IdentityGrantStore.Transaction admission;
    private InvitationCredentials credentials;
    private AcceptIdentityInvitation app;
    private IdentityAccount pending;
    private ActionProof proof;
    private byte[] fingerprint;
    @BeforeEach void fixture() {
        store=mock(IdentityInvitationStore.class);tx=mock(IdentityInvitationStore.Transaction.class);
        admission=mock(IdentityGrantStore.Transaction.class);credentials=mock(InvitationCredentials.class);
        pending=new IdentityAccount(ACCOUNT,new Username("synthetic.pending"),IdentityAccount.Status.PENDING_ACTIVATION,
            IdentityAccount.Activation.UNVERIFIED,new AccountVersion(1,1),Set.of(new Grant(new RoleCode("IDENTITY_MANAGEMENT"),new ResourceScope(ResourceScope.Type.IDENTITY,REALM))));
        proof=ActionProof.invitation(PROOF,pending,NOW.minusSeconds(1));fingerprint=new byte[32];Arrays.fill(fingerprint,(byte)7);
        when(store.transaction(any())).thenAnswer(invocation->{Function<IdentityInvitationStore.Transaction,Object> work=invocation.getArgument(0);return work.apply(tx);});
        when(tx.admission()).thenReturn(admission);when(tx.lockProof(PROOF)).thenReturn(Optional.of(new IdentityInvitationStore.StoredProof(proof,"v1",new byte[32])));
        when(admission.lockAccount(ACCOUNT)).thenReturn(Optional.of(pending));when(tx.eligibleRealm(ACCOUNT,PROOF)).thenReturn(true);
        when(tx.receipt(PROOF,KEY)).thenReturn(Optional.empty());when(credentials.matches(any(),any(),any())).thenReturn(true);
        when(credentials.fingerprint(any(),any(),any())).thenAnswer(i->fingerprint.clone());
        when(credentials.hashPassword(any())).thenReturn("$2b$12$"+".".repeat(53));
        app=new AcceptIdentityInvitation(store,credentials,Clock.fixed(NOW,ZoneOffset.UTC),Duration.ofHours(24));
    }
    private IdentityInvitationCommands.Command command(String password) {
        return new IdentityInvitationCommands.Command(PROOF,new byte[43],password,KEY,UUID.randomUUID());
    }
    private void reject(IdentityCommandFailure.Code code,String password) {
        try(var command=command(password)) {var error=assertThrows(IdentityCommandFailure.class,()->app.acceptIdentityInvitation(command));assertEquals(code,error.code());assertEquals(code.name(),error.getMessage());}
    }
    @Test void validActivationInvokesAllWritesInsideTheTransaction() {
        try(var c=command(PASSWORD)){app.acceptIdentityInvitation(c);}
        verify(admission).lockAdministration();verify(tx).activate(eq(pending),any(),anyString(),eq(NOW));
        verify(admission).revokeSessions(ACCOUNT,NOW);verify(tx).clearDelivery(PROOF,NOW);
        verify(tx).appendAudit(eq(ACCOUNT),any(),eq(NOW));verify(tx).appendReceipt(eq(PROOF),eq(KEY),any(),eq(1L),eq(NOW));
    }
    @Test void missingCommandIdentifiersFailBeforeTheStore() {
        assertThrows(IdentityCommandFailure.class,()->app.acceptIdentityInvitation(null));
        try(var c=new IdentityInvitationCommands.Command(null,null,PASSWORD,KEY,UUID.randomUUID())){assertThrows(IdentityCommandFailure.class,()->app.acceptIdentityInvitation(c));}
        verifyNoInteractions(store);
    }
    @Test void invalidNewPasswordsNeverReachPersistence() {
        for(String password:List.of("short","x".repeat(73),"\u20ac".repeat(25),"\ud800"+"x".repeat(20)))reject(VALIDATION_FAILED,password);
        verifyNoInteractions(store);
    }
    @Test void passwordUnicodeAndUtf8BoundsReuseTheDomainPolicy() {
        assertDoesNotThrow(()->PasswordPolicy.validateNew("x".repeat(15)));assertDoesNotThrow(()->PasswordPolicy.validateNew("x".repeat(72)));
        assertDoesNotThrow(()->PasswordPolicy.validateNew("\u20ac".repeat(24)));
        assertThrows(DomainViolation.class,()->PasswordPolicy.validateNew("x".repeat(14)));
        assertThrows(DomainViolation.class,()->PasswordPolicy.validateNew("\u20ac".repeat(25)));
        assertDoesNotThrow(()->PasswordPolicy.validateNew(" "+"x".repeat(14)));
    }
    @Test void invalidTokenCannotReachReceiptOrPasswordHashing() {
        when(credentials.matches(any(),any(),any())).thenReturn(false);reject(TOKEN_INVALID_OR_EXPIRED,PASSWORD);
        verify(tx,never()).receipt(any(),any());verify(credentials,never()).hashPassword(any());
    }
    @Test void unavailableRealmCannotReachPasswordHashing() {
        when(tx.eligibleRealm(ACCOUNT,PROOF)).thenReturn(false);reject(INVALID_ACCOUNT_STATE,PASSWORD);verify(credentials,never()).hashPassword(any());
    }
    @Test void consumedProofWithoutExactReceiptCannotActivate() {
        when(tx.lockProof(PROOF)).thenReturn(Optional.of(new IdentityInvitationStore.StoredProof(proof.consume(ActionProof.Purpose.INVITATION,ACCOUNT,NOW),"v1",new byte[32])));
        reject(TOKEN_INVALID_OR_EXPIRED,PASSWORD);verify(tx,never()).activate(any(),any(),any(),any());
    }
    private void replay() {
        var change=pending.activate(proof,PASSWORD,1,NOW);
        when(admission.lockAccount(ACCOUNT)).thenReturn(Optional.of(change.account()));
        when(tx.lockProof(PROOF)).thenReturn(Optional.of(new IdentityInvitationStore.StoredProof(change.consumedProof(),"v1",new byte[32])));
        when(tx.receipt(PROOF,KEY)).thenReturn(Optional.of(new IdentityInvitationStore.Receipt(ACCOUNT,2,fingerprint,NOW.plusSeconds(86400))));
    }
    @Test void exactConsumedProofReceiptReplayHasNoWrites() {
        replay();try(var c=command(PASSWORD)){app.acceptIdentityInvitation(c);}
        verify(tx,never()).activate(any(),any(),any(),any());verify(credentials,never()).hashPassword(any());verify(tx,never()).appendAudit(any(),any(),any());
    }
    @Test void changedReceiptFingerprintConflictsBeforeAnyWrite() {
        replay();when(credentials.fingerprint(any(),any(),any())).thenReturn(new byte[32]);reject(IDEMPOTENCY_CONFLICT,PASSWORD);
        verify(tx,never()).activate(any(),any(),any(),any());
    }
    @Test void replayRequiresCurrentActiveAccountAndRetainedVersion() {
        replay();when(admission.lockAccount(ACCOUNT)).thenReturn(Optional.of(pending));reject(INVALID_ACCOUNT_STATE,PASSWORD);
    }
    @Test void receiptRetentionCannotBeShortenedBelowTwentyFourHours() {
        assertThrows(IllegalArgumentException.class,()->new AcceptIdentityInvitation(store,credentials,Clock.systemUTC(),Duration.ofHours(23)));
    }
    @Test void commandBuffersAreCopiedRedactedAndClosed() {
        byte[] original=new byte[43];Arrays.fill(original,(byte)65);
        var c=new IdentityInvitationCommands.Command(PROOF,original,PASSWORD,KEY,UUID.randomUUID());byte[] copy=c.token();copy[0]=0;
        assertEquals(65,c.token()[0]);assertFalse(c.toString().contains(PASSWORD));c.close();
        assertThrows(IllegalStateException.class,c::token);assertEquals(65,original[0]);Arrays.fill(original,(byte)0);Arrays.fill(copy,(byte)0);
    }
}
