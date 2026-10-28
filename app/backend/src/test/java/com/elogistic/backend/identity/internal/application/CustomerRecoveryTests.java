package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerRecoveryTests {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private static final UUID ACCOUNT=UUID.randomUUID(),REALM=UUID.randomUUID();
    private static final String PASSWORD="synthetic-new-customer-password";
    private IdentityAccount account(){return new IdentityAccount(ACCOUNT,new Username("Exact.Legacy.Username"),IdentityAccount.Status.ACTIVE,IdentityAccount.Activation.VERIFIED,new AccountVersion(5,7),Set.of(new Grant(new RoleCode("IDENTITY_MANAGEMENT"),new ResourceScope(ResourceScope.Type.IDENTITY,REALM))));}
    private CustomerProof recovery(){return CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,null,7L,null);}
    @Test void liveRecoveryHasNoSessionButMustBindCurrentSecurityVersion(){
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,null,null,null));
        assertThrows(DomainViolation.class,()->CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,UUID.randomUUID(),7L,null));
        var proof=recovery();assertNull(proof.sessionId());assertEquals(7L,proof.authVersion());assertEquals(NOW.plusSeconds(900),proof.expiresAt());
    }
    @Test void resetPreservesExactUsernameGrantsAndEligibilityWhileAdvancingBothVersions(){
        var before=account();var change=before.resetCustomerPassword(recovery(),PASSWORD,5,NOW);var after=change.account();
        assertEquals(before.id(),after.id());assertEquals(before.username(),after.username());assertEquals(before.grants(),after.grants());assertEquals(before.status(),after.status());assertEquals(before.activation(),after.activation());
        assertEquals(new AccountVersion(6,8),after.version());assertNotNull(change.consumedProof().consumedAt());assertEquals("CustomerProofChange[redacted]",change.toString());
    }
    @Test void staleProofSecurityVersionAndWrongAccountVersionDenyReset(){
        var stale=CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,null,6L,null);
        assertThrows(DomainViolation.class,()->account().resetCustomerPassword(stale,PASSWORD,5,NOW));assertThrows(DomainViolation.class,()->account().resetCustomerPassword(recovery(),PASSWORD,4,NOW));
    }
    @Test void enrollmentAnotherSubjectAndConsumedProofCannotResetCredential(){
        var enrollment=CustomerProof.issue(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_ENROLLMENT,NOW,null,null,null);
        var other=CustomerProof.issue(UUID.randomUUID(),UUID.randomUUID(),CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,null,7L,null);
        var proof=recovery();var consumed=proof.consume(proof.purpose(),ACCOUNT,null,7L,null,NOW);
        for(var invalid:List.of(enrollment,other,consumed))assertThrows(DomainViolation.class,()->account().resetCustomerPassword(invalid,PASSWORD,5,NOW));
    }
    @Test void disabledOrUnverifiedAccountCannotRecoverEvenWithProof(){
        for(var before:List.of(new IdentityAccount(ACCOUNT,account().username(),IdentityAccount.Status.DISABLED,IdentityAccount.Activation.VERIFIED,account().version(),account().grants()),new IdentityAccount(ACCOUNT,account().username(),IdentityAccount.Status.PENDING_ACTIVATION,IdentityAccount.Activation.UNVERIFIED,account().version(),Set.of())))
            assertThrows(DomainViolation.class,()->before.resetCustomerPassword(recovery(),PASSWORD,5,NOW));
    }
    @Test void exactFifteenMinuteDeadlineAndFutureIssuanceDenyReset(){
        var proof=recovery();account().resetCustomerPassword(proof,PASSWORD,5,proof.expiresAt().minusNanos(1));
        for(Instant time:List.of(NOW.minusNanos(1),proof.expiresAt(),proof.expiresAt().plusSeconds(1)))assertThrows(DomainViolation.class,()->account().resetCustomerPassword(proof,PASSWORD,5,time));
    }
    @Test void acceptedPasswordPolicyAndVersionExhaustionHaveNoMutablePartialState(){
        var before=account();var proof=recovery();for(String invalid:List.of("short","x".repeat(73),"\u00e9".repeat(37)))assertThrows(DomainViolation.class,()->before.resetCustomerPassword(proof,invalid,5,NOW));
        assertEquals(new AccountVersion(5,7),before.version());assertNull(proof.consumedAt());
        var exhausted=new IdentityAccount(ACCOUNT,before.username(),before.status(),before.activation(),new AccountVersion(Long.MAX_VALUE,7),before.grants());assertThrows(DomainViolation.class,()->exhausted.resetCustomerPassword(proof,PASSWORD,Long.MAX_VALUE,NOW));
    }
    @Test void closedUnboundHistoricalProofStaysReadableButNeverUsable(){
        var closed=new CustomerProof(UUID.randomUUID(),ACCOUNT,CustomerProof.Purpose.CUSTOMER_RECOVERY,NOW,NOW.plusSeconds(900),null,null,null,null,NOW);
        assertThrows(DomainViolation.class,()->closed.requireAvailable(NOW));assertThrows(DomainViolation.class,()->account().resetCustomerPassword(closed,PASSWORD,5,NOW));
    }
}
