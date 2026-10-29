package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.internal.domain.*;
import com.elogistic.backend.identity.internal.application.port.*;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerEnrollmentTests {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    private static final String HASH="$2b$12$"+".".repeat(53);
    private static CustomerEnrollment pending(){return new CustomerEnrollment(UUID.randomUUID(),UUID.randomUUID(),new CustomerEmail("fixture@example.invalid"),"Synthetic Customer",HASH,"S003-2026-10-27",NOW,NOW.plusSeconds(86400),CustomerEnrollment.Status.PENDING,1);}
    @Test void enrollmentCannotHoldPlaintextPasswordOrInvalidPendingState(){
        var e=pending();
        assertThrows(DomainViolation.class,()->new CustomerEnrollment(e.id(),e.intendedAccountId(),e.email(),e.displayName(),"plaintext-never-stored",e.consentVersion(),NOW,e.expiresAt(),e.status(),1));
        assertThrows(DomainViolation.class,()->new CustomerEnrollment(e.id(),e.intendedAccountId(),e.email(),e.displayName(),null,e.consentVersion(),NOW,e.expiresAt(),e.status(),1));
        assertThrows(DomainViolation.class,()->new CustomerEnrollment(e.id(),e.intendedAccountId(),e.email(),e.displayName(),HASH,e.consentVersion(),NOW,e.expiresAt(),CustomerEnrollment.Status.CONSUMED,2));
    }
    @Test void onlyCanonicalUnverifiedInternationalPhoneCanEnterEnrollment(){
        var e=pending();var valid=new CustomerEnrollment(e.id(),e.intendedAccountId(),e.email(),e.displayName(),"+84901234567",HASH,e.consentVersion(),NOW,e.expiresAt(),e.status(),1);
        assertEquals("+84901234567",valid.resend(NOW.plusSeconds(1)).unverifiedPhone());
        assertThrows(DomainViolation.class,()->new CustomerEnrollment(e.id(),e.intendedAccountId(),e.email(),e.displayName(),"0901234567",HASH,e.consentVersion(),NOW,e.expiresAt(),e.status(),1));
    }
    @Test void consumingEnrollmentDestroysProtectedMaterialAndCannotReplay(){
        var e=pending();var consumed=e.consume(NOW);assertNull(consumed.protectedPassword());assertEquals(2,consumed.version());assertEquals(CustomerEnrollment.Status.CONSUMED,consumed.status());
        assertEquals(e.intendedAccountId(),consumed.intendedAccountId());assertThrows(DomainViolation.class,()->consumed.consume(NOW));
    }
    @Test void exactDeadlineAndFutureTimeDenyActivation(){
        var e=pending();e.requirePending(e.expiresAt().minusNanos(1));
        assertThrows(DomainViolation.class,()->e.consume(e.expiresAt()));assertThrows(DomainViolation.class,()->e.consume(NOW.minusNanos(1)));
    }
    @Test void resendPreservesIdentityAndCredentialButRenewsProofWindow(){
        var e=pending();var renewed=e.resend(NOW.plusSeconds(60));
        assertEquals(e.intendedAccountId(),renewed.intendedAccountId());assertEquals(e.protectedPassword(),renewed.protectedPassword());assertEquals(NOW.plusSeconds(86460),renewed.expiresAt());assertEquals(2,renewed.version());
        assertThrows(DomainViolation.class,()->e.resend(e.expiresAt()));
    }
    @Test void expiryDestroysProtectedMaterialAndPreservesHistoricalEnrollment(){
        var e=pending();assertThrows(DomainViolation.class,()->e.expire(NOW));var expired=e.expire(e.expiresAt());
        assertNull(expired.protectedPassword());assertEquals(CustomerEnrollment.Status.EXPIRED,expired.status());assertEquals(e.id(),expired.id());assertEquals(2,expired.version());
    }
    @Test void enrollmentAndCommandsNeverExposeCredentialsOrContactInDiagnostics(){
        var e=pending();assertEquals("CustomerEnrollment[redacted]",e.toString());
        var c=new com.elogistic.backend.identity.api.customer.CustomerEnrollmentCommands.Enrollment(e.email().value(),"synthetic-password",e.displayName(),e.consentVersion(),e.id(),e.intendedAccountId());
        assertEquals("CustomerEnrollmentCommand[redacted]",c.toString());
    }
    @Test void applicationHasNoPermissiveAdmissionOrProtectionDefault(){
        assertThrows(NullPointerException.class,()->new CustomerEnrollments(null,null,null,Clock.systemUTC()));
        assertThrows(IllegalArgumentException.class,()->new CustomerEnrollmentAdmission.Binding(null));
    }
}
