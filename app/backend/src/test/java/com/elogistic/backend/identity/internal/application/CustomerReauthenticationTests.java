package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.*;
import com.elogistic.backend.identity.internal.infrastructure.ProtectedCustomerProofs;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerReauthenticationTests {
    private static final Instant NOW=Instant.parse("2026-10-28T00:00:00Z");
    private static final UUID ACCOUNT=UUID.randomUUID(),SESSION=UUID.randomUUID(),REQUEST=UUID.randomUUID(),KEY=UUID.randomUUID();
    private static final String EMAIL="self@example.invalid",PASSWORD="synthetic-current-password";
    private final CustomerAdmission.Context context=new CustomerAdmission.Context(REQUEST,SESSION,new CustomerAdmission.Subject(ACCOUNT,"Synthetic",EMAIL,3,true),Map.of());
    private final class Admission implements CustomerAdmission {
        boolean active;boolean disabled;
        public <T>T execute(String access,UUID request,Set<UUID> refs,Function<Context,T> work){assertEquals("access",access);assertEquals(REQUEST,request);assertTrue(refs.isEmpty());if(disabled)throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);active=true;try{return work.apply(context);}finally{active=false;}}
        public void requireCurrent(Context value){if(!active || value!=context)throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
        public void meaningfulAction(Context value){fail("Reauthentication must not extend idle lifetime");}
        public void requireDeadline(Context value,Instant deadline){requireCurrent(value);assertTrue(NOW.isBefore(deadline));}
    }
    private final Admission admission=new Admission();
    private final CustomerReauthenticationStore store=mock(CustomerReauthenticationStore.class);
    private final SessionCredentials credentials=mock(SessionCredentials.class);
    private final CustomerPasswordAttemptAdmission attempts=mock(CustomerPasswordAttemptAdmission.class);
    private final IdentityRateLimiter.Reservation reservation=mock(IdentityRateLimiter.Reservation.class);
    private final CustomerProofProtection protection=protection();
    private CustomerReauthentications app;
    private static CustomerProofProtection protection(){byte[] a=new byte[32],b=new byte[32];Arrays.fill(a,(byte)91);Arrays.fill(b,(byte)92);return new ProtectedCustomerProofs(Map.of("v1",a),"v1",Map.of("e1",b),"e1");}
    @BeforeEach void setup(){
        when(attempts.reserve(EMAIL)).thenReturn(reservation);when(store.passwordVerifier(ACCOUNT)).thenReturn("stored-verifier");when(credentials.verify(PASSWORD,"stored-verifier")).thenReturn(true);
        app=new CustomerReauthentications(admission,()->new CustomerRequestContext.Request(REQUEST,"access"),attempts,store,protection,credentials,Clock.fixed(NOW,ZoneOffset.UTC));
    }
    private CustomerReauthentication.Command command(){return new CustomerReauthentication.Command(KEY,EMAIL,PASSWORD);}
    private CustomerProof proof(){return CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.REAUTHENTICATION,NOW,SESSION,3L,null);}
    private CustomerReauthenticationStore.Receipt receipt(CustomerProof proof){return new CustomerReauthenticationStore.Receipt(CustomerReceipts.prepare(protection,REQUEST,"requestCustomerReauthentication",ACCOUNT.toString(),SESSION.toString(),"3",EMAIL,PASSWORD),SESSION,3,proof);}
    private void code(IdentityCommandFailure.Code code,Runnable work){assertEquals(code,assertThrows(IdentityCommandFailure.class,work::run).code());}
    @Test void reauthenticationUsesOnlyCurrentSubjectAndExactFifteenMinuteSessionBinding(){
        var result=app.request(command());var proof=ArgumentCaptor.forClass(CustomerProof.class);verify(store).replace(proof.capture(),eq(NOW));assertEquals(ACCOUNT,proof.getValue().subjectId());assertEquals(SESSION,proof.getValue().sessionId());assertEquals(3,proof.getValue().authVersion());assertEquals(CustomerProof.Purpose.REAUTHENTICATION,proof.getValue().purpose());assertEquals(NOW.plusSeconds(900),result.expiresAt());assertEquals(proof.getValue().id(),result.proofId());verify(store).audit(ACCOUNT,REQUEST,"CUSTOMER_REAUTHENTICATED",NOW);verify(reservation).finishLogin(true);
        var order=inOrder(attempts,store);order.verify(attempts).reserve(EMAIL);order.verify(store).receipt(ACCOUNT,KEY);
    }
    @Test void wrongPasswordOrDifferentEmailCannotChooseAnotherActorAndConsumesAttempt(){
        when(credentials.verify(PASSWORD,"stored-verifier")).thenReturn(false);code(IdentityCommandFailure.Code.INVALID_CREDENTIALS,()->app.request(command()));verify(reservation).finishLogin(false);verify(store,never()).replace(any(),any());
        reset(credentials,reservation);when(attempts.reserve("other@example.invalid")).thenReturn(reservation);code(IdentityCommandFailure.Code.INVALID_CREDENTIALS,()->app.request(new CustomerReauthentication.Command(KEY,"other@example.invalid",PASSWORD)));verify(credentials).verify(PASSWORD,null);verify(store,never()).passwordVerifier(argThat(id->!id.equals(ACCOUNT)));verify(reservation).finishLogin(false);
    }
    @Test void sameKeyReturnsOriginalExpiredOrConsumedMetadataWithoutRenewingAuthority(){
        var proof=proof().consume(CustomerProof.Purpose.REAUTHENTICATION,ACCOUNT,SESSION,3L,null,NOW);when(store.receipt(ACCOUNT,KEY)).thenReturn(Optional.of(receipt(proof)));var result=app.request(command());assertEquals(proof.id(),result.proofId());assertEquals(proof.expiresAt(),result.expiresAt());verifyNoInteractions(credentials);verify(store,never()).replace(any(),any());verify(store,never()).audit(any(),any(),any(),any());
    }
    @Test void sameKeyChangedPasswordOrEmailConflictsBeforeReissuingProof(){
        when(store.receipt(ACCOUNT,KEY)).thenReturn(Optional.of(receipt(proof())));code(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT,()->app.request(new CustomerReauthentication.Command(KEY,EMAIL,"changed-password")));verifyNoInteractions(credentials);verify(store,never()).replace(any(),any());verify(reservation,never()).finishLogin(true);
    }
    @Test void consumeRequiresCurrentContextAndCannotUseCapturedOrConstructedAuthority(){
        when(store.proof(any())).thenReturn(Optional.of(proof()));code(IdentityCommandFailure.Code.SESSION_INVALID,()->app.consume(context,UUID.randomUUID()));admission.active=true;var forged=new CustomerAdmission.Context(REQUEST,SESSION,context.actor(),Map.of());code(IdentityCommandFailure.Code.SESSION_INVALID,()->app.consume(forged,UUID.randomUUID()));verify(store,never()).consume(any(),any());
    }
    @Test void consumptionIsSingleUseAndBoundToCurrentSessionVersionPurposeAndDeadline(){
        admission.active=true;var proof=proof();when(store.proof(proof.id())).thenReturn(Optional.of(proof));app.consume(context,proof.id());verify(store).consume(eq(proof),eq(proof.consume(CustomerProof.Purpose.REAUTHENTICATION,ACCOUNT,SESSION,3L,null,NOW)));
        for(var invalid:List.of(proof.consume(CustomerProof.Purpose.REAUTHENTICATION,ACCOUNT,SESSION,3L,null,NOW),CustomerProof.issue(proof.id(),ACCOUNT,CustomerProof.Purpose.REAUTHENTICATION,NOW.minusSeconds(900),SESSION,3L,null),CustomerProof.issue(proof.id(),ACCOUNT,CustomerProof.Purpose.REAUTHENTICATION,NOW,UUID.randomUUID(),3L,null),CustomerProof.issue(proof.id(),ACCOUNT,CustomerProof.Purpose.REAUTHENTICATION,NOW,SESSION,2L,null),CustomerProof.issue(proof.id(),ACCOUNT,CustomerProof.Purpose.OWNER_RECOVERY_CONSENT,NOW,SESSION,3L,UUID.randomUUID()))){when(store.proof(proof.id())).thenReturn(Optional.of(invalid));code(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED,()->app.consume(context,proof.id()));}
        verify(store,times(1)).consume(any(),any());
    }
    @Test void quotaOrCurrentAdmissionFailureCannotIssueProofOrReleaseSuccessfulAttempt(){
        when(attempts.reserve(EMAIL)).thenThrow(new IdentityRateLimiter.Unavailable());assertThrows(IdentityRateLimiter.Unavailable.class,()->app.request(command()));verifyNoInteractions(store,credentials,reservation);
        doReturn(reservation).when(attempts).reserve(EMAIL);admission.disabled=true;code(IdentityCommandFailure.Code.SESSION_INVALID,()->app.request(command()));verifyNoInteractions(store,credentials,reservation);
    }
    @Test void malformedCommandsAreRejectedBeforeAttemptOrPersistenceAndDiagnosticsAreRedacted(){
        for(var command:List.of(new CustomerReauthentication.Command(null,EMAIL,PASSWORD),new CustomerReauthentication.Command(KEY," "+EMAIL,PASSWORD),new CustomerReauthentication.Command(KEY,EMAIL,""),new CustomerReauthentication.Command(KEY,EMAIL,"x".repeat(257))))code(IdentityCommandFailure.Code.VALIDATION_FAILED,()->app.request(command));verifyNoInteractions(store,credentials,attempts);assertFalse(command().toString().contains(PASSWORD));assertFalse(command().toString().contains(EMAIL));assertFalse(receipt(proof()).toString().contains(EMAIL));
    }
    @Test void reauthenticationPersistenceCannotOpenAnUnownedOrUnboundTransaction(){
        var store=new com.elogistic.backend.identity.internal.infrastructure.JdbcCustomerReauthenticationStore(new org.springframework.jdbc.datasource.DriverManagerDataSource());
        assertThrows(IllegalStateException.class,()->store.passwordVerifier(ACCOUNT));assertThrows(IllegalStateException.class,()->store.proof(KEY));assertThrows(IllegalStateException.class,()->store.receipt(ACCOUNT,KEY));assertThrows(IllegalStateException.class,()->store.replace(proof(),NOW));
    }
}
