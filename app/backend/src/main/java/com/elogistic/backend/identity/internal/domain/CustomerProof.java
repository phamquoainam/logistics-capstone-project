package com.elogistic.backend.identity.internal.domain;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Customer-only proof lifecycle. UUID locators never establish possession or business authority. */
public record CustomerProof(UUID id, UUID subjectId, Purpose purpose, Instant createdAt, Instant expiresAt,
                            UUID sessionId, Long authVersion, UUID organizationId,
                            Instant consumedAt, Instant revokedAt) {
    public enum Purpose {
        CUSTOMER_ENROLLMENT(86400), CUSTOMER_RECOVERY(900), SECURITY_EMAIL_CHANGE(900),
        BUSINESS_CONTACT(900), REAUTHENTICATION(900), OWNER_RECOVERY_CONSENT(900), LEGACY_CUSTOMER_LINK(900);
        public final long seconds;
        Purpose(long seconds){this.seconds=seconds;}
        public boolean possessionRequired(){return this!=REAUTHENTICATION && this!=OWNER_RECOVERY_CONSENT;}
        public boolean customerSessionRequired(){return Set.of(SECURITY_EMAIL_CHANGE,BUSINESS_CONTACT,REAUTHENTICATION,OWNER_RECOVERY_CONSENT).contains(this);}
    }
    public CustomerProof {
        if(id==null || subjectId==null || purpose==null || createdAt==null || expiresAt==null
            || !createdAt.equals(createdAt.truncatedTo(ChronoUnit.MICROS)) || !expiresAt.equals(createdAt.plusSeconds(purpose.seconds))
            || (purpose!=Purpose.CUSTOMER_RECOVERY && (sessionId==null)!=(authVersion==null)) || (authVersion!=null && authVersion<1)
            || (purpose.customerSessionRequired() && sessionId==null)
            || ((purpose==Purpose.OWNER_RECOVERY_CONSENT)!=(organizationId!=null))
            || (consumedAt!=null && (consumedAt.isBefore(createdAt) || !consumedAt.isBefore(expiresAt)))
            || (revokedAt!=null && revokedAt.isBefore(createdAt)))throw rejected();
        if((purpose==Purpose.CUSTOMER_ENROLLMENT || purpose==Purpose.CUSTOMER_RECOVERY) && sessionId!=null)throw rejected();
        // Recovery has no session, but a live proof must bind the current security version.
        // Closed pre-amendment rows remain readable as unusable historical evidence.
        if(purpose==Purpose.CUSTOMER_RECOVERY && authVersion==null && consumedAt==null && revokedAt==null)throw rejected();
        if(purpose==Purpose.LEGACY_CUSTOMER_LINK && sessionId==null)throw rejected();
    }
    public static CustomerProof issue(UUID id,UUID subject,Purpose purpose,Instant time,UUID session,Long version,UUID organization) {
        return new CustomerProof(id,subject,purpose,time,time.plusSeconds(purpose.seconds),session,version,organization,null,null);
    }
    public void requireAvailable(Instant time) {
        if(time==null || time.isBefore(createdAt) || !time.isBefore(expiresAt) || consumedAt!=null || revokedAt!=null)throw rejected();
    }
    public CustomerProof consume(Purpose expected,UUID subject,UUID session,Long version,UUID organization,Instant time) {
        requireAvailable(time);
        if(purpose!=expected || !subjectId.equals(subject) || !Objects.equals(sessionId,session)
            || !Objects.equals(authVersion,version) || !Objects.equals(organizationId,organization))throw rejected();
        return new CustomerProof(id,subjectId,purpose,createdAt,expiresAt,sessionId,authVersion,organizationId,time,null);
    }
    public static DomainViolation rejected(){return new DomainViolation(DomainViolation.Reason.PROOF_INVALID);}
    @Override public String toString(){return "CustomerProof[redacted]";}
}
