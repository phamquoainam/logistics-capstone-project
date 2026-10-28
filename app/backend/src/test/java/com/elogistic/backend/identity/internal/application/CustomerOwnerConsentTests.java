package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.api.customer.*;
import com.elogistic.backend.identity.internal.application.port.*;
import com.elogistic.backend.identity.internal.domain.CustomerProof;
import com.elogistic.backend.identity.internal.infrastructure.*;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerOwnerConsentTests {
    private static final Instant NOW=Instant.parse("2026-10-28T00:00:00Z");
    private static final UUID ACCOUNT=UUID.randomUUID(),SESSION=UUID.randomUUID(),REQUEST=UUID.randomUUID(),KEY=UUID.randomUUID(),ORG=UUID.randomUUID(),REAUTH=UUID.randomUUID();
    private final CustomerAdmission.Context context=new CustomerAdmission.Context(REQUEST,SESSION,new CustomerAdmission.Subject(ACCOUNT,"Synthetic","self@example.invalid",3,true),Map.of());
    private final class Admission implements CustomerAdmission {
        boolean active,disabled;
        public <T>T execute(String access,UUID request,Set<UUID> references,Function<Context,T> work){assertEquals("access",access);assertEquals(REQUEST,request);assertTrue(references.isEmpty());if(disabled)throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);active=true;try{return work.apply(context);}finally{active=false;}}
        public void requireCurrent(Context value){if(!active || context!=value)throw new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
        public void meaningfulAction(Context value){fail("Consent issuance must not extend idle time");}
        public void requireDeadline(Context value,Instant deadline){requireCurrent(value);assertEquals(NOW.plusSeconds(900),deadline);}
    }
    private final Admission admission=new Admission();
    private final CustomerReauthentication reauthentication=mock(CustomerReauthentication.class);
    private final CustomerOwnerConsentStore store=mock(CustomerOwnerConsentStore.class);
    private final CustomerRequestContext requests=mock(CustomerRequestContext.class);
    private final CustomerProofProtection protection=protection();
    private CustomerOwnerConsents app;
    private static CustomerProofProtection protection(){byte[] a=new byte[32],b=new byte[32];Arrays.fill(a,(byte)94);Arrays.fill(b,(byte)95);return new ProtectedCustomerProofs(Map.of("v1",a),"v1",Map.of("e1",b),"e1");}
    @BeforeEach void setup(){when(requests.current()).thenReturn(new CustomerRequestContext.Request(REQUEST,"access"));app=new CustomerOwnerConsents(admission,requests,reauthentication,store,protection,Clock.fixed(NOW,ZoneOffset.UTC));}
    private CustomerOwnerRecoveryConsents.Command command(){return new CustomerOwnerRecoveryConsents.Command(KEY,ORG,REAUTH);}
    private CustomerProof proof(Instant issued,UUID session,long version){return CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.OWNER_RECOVERY_CONSENT,issued,session,version,ORG);}
    private CustomerOwnerConsentStore.Receipt receipt(CustomerProof proof){return new CustomerOwnerConsentStore.Receipt(CustomerReceipts.prepare(protection,REQUEST,"createOwnerRecoveryConsent",ACCOUNT.toString(),SESSION.toString(),"3",ORG.toString(),REAUTH.toString()),proof,REAUTH);}
    private void code(IdentityCommandFailure.Code code,Runnable work){assertEquals(code,assertThrows(IdentityCommandFailure.class,work::run).code());}
    @Test void consentUsesCurrentHumanAndConsumesRecentProofBeforeAnyWrite(){
        var result=app.create(command());var proof=ArgumentCaptor.forClass(CustomerProof.class);verify(store).replace(proof.capture(),eq(NOW));assertEquals(ACCOUNT,proof.getValue().subjectId());assertEquals(SESSION,proof.getValue().sessionId());assertEquals(3L,proof.getValue().authVersion());assertEquals(ORG,proof.getValue().organizationId());assertEquals(CustomerProof.Purpose.OWNER_RECOVERY_CONSENT,proof.getValue().purpose());assertEquals(proof.getValue().id(),result.id());assertEquals(ORG,result.organizationId());assertEquals(NOW.plusSeconds(900),result.expiresAt());
        var order=inOrder(store,reauthentication);order.verify(store).receipt(ACCOUNT,KEY);order.verify(reauthentication).consume(context,REAUTH);order.verify(store).replace(any(),eq(NOW));order.verify(store).audit(ACCOUNT,REQUEST,NOW);order.verify(store).receipt(eq(ACCOUNT),eq(KEY),any(),eq(NOW));
    }
    @Test void sameKeyReturnsOriginalExpiredOutcomeWithoutRenewingOrConsumingAgain(){
        var proof=proof(NOW.minusSeconds(1000),SESSION,3);when(store.receipt(ACCOUNT,KEY)).thenReturn(Optional.of(receipt(proof)));var result=app.create(command());assertEquals(proof.id(),result.id());assertEquals(proof.expiresAt(),result.expiresAt());verifyNoInteractions(reauthentication);verify(store,never()).replace(any(),any());verify(store,never()).audit(any(),any(),any());
    }
    @Test void changedOrganizationOrRecentProofCannotReuseCommittedKey(){
        when(store.receipt(ACCOUNT,KEY)).thenReturn(Optional.of(receipt(proof(NOW,SESSION,3))));code(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT,()->app.create(new CustomerOwnerRecoveryConsents.Command(KEY,UUID.randomUUID(),REAUTH)));code(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT,()->app.create(new CustomerOwnerRecoveryConsents.Command(KEY,ORG,UUID.randomUUID())));verifyNoInteractions(reauthentication);verify(store,never()).replace(any(),any());
    }
    @Test void changedSessionVersionOrProofPurposeCannotBeReplayedAsConsent(){
        for(var proof:List.of(proof(NOW,UUID.randomUUID(),3),proof(NOW,SESSION,2),CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.REAUTHENTICATION,NOW,SESSION,3L,null))){when(store.receipt(ACCOUNT,KEY)).thenReturn(Optional.of(receipt(proof)));code(IdentityCommandFailure.Code.IDEMPOTENCY_CONFLICT,()->app.create(command()));}verifyNoInteractions(reauthentication);verify(store,never()).replace(any(),any());
    }
    @Test void unavailableExpiredOrForeignRecentProofPreventsEveryConsentWrite(){
        doThrow(new IdentityCommandFailure(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED)).when(reauthentication).consume(context,REAUTH);code(IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED,()->app.create(command()));verify(store,never()).replace(any(),any());verify(store,never()).audit(any(),any(),any());verify(store,never()).receipt(any(),any(),any(),any());
    }
    @Test void mandatoryConsentAuditFailureCannotReturnSuccessfulOutcome(){
        doThrow(new IllegalStateException("SYNTHETIC_AUDIT_FAILURE")).when(store).audit(ACCOUNT,REQUEST,NOW);assertThrows(IllegalStateException.class,()->app.create(command()));verify(reauthentication).consume(context,REAUTH);verify(store,never()).receipt(any(),any(),any(),any());
    }
    @Test void missingCommandBindingsFailBeforeRequestOrPersistence(){
        code(IdentityCommandFailure.Code.VALIDATION_FAILED,()->app.create(null));for(var invalid:List.of(new CustomerOwnerRecoveryConsents.Command(null,ORG,REAUTH),new CustomerOwnerRecoveryConsents.Command(KEY,null,REAUTH),new CustomerOwnerRecoveryConsents.Command(KEY,ORG,null)))code(IdentityCommandFailure.Code.VALIDATION_FAILED,()->app.create(invalid));verifyNoInteractions(requests,store,reauthentication);
    }
    @Test void revokedCurrentAdmissionCannotIssueConsentFromPreviouslyCapturedSubject(){
        admission.disabled=true;code(IdentityCommandFailure.Code.SESSION_INVALID,()->app.create(command()));verifyNoInteractions(store,reauthentication);
    }
    @Test void consentStoreRequiresExactBoundDataSourceAndDiagnosticsStayRedacted(){
        var jdbc=new JdbcCustomerOwnerConsentStore(new org.springframework.jdbc.datasource.DriverManagerDataSource());assertThrows(IllegalStateException.class,()->jdbc.receipt(ACCOUNT,KEY));boolean previous=TransactionSynchronizationManager.isActualTransactionActive();
        try{TransactionSynchronizationManager.setActualTransactionActive(true);assertThrows(IllegalStateException.class,()->jdbc.replace(proof(NOW,SESSION,3),NOW));assertThrows(IllegalStateException.class,()->jdbc.audit(ACCOUNT,REQUEST,NOW));}finally{TransactionSynchronizationManager.setActualTransactionActive(previous);}
        assertEquals("OwnerRecoveryConsentCommand[redacted]",command().toString());assertEquals("OwnerConsentReceipt[redacted]",receipt(proof(NOW,SESSION,3)).toString());
    }
}
