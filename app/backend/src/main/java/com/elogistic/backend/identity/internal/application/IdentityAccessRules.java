package com.elogistic.backend.identity.internal.application;

import com.elogistic.backend.identity.api.IdentityCommandFailure;
import com.elogistic.backend.identity.internal.application.port.CustomerAccessVerifier;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Shared claim/session timing checks; fixed composition determines the principal audience. */
public final class IdentityAccessRules {
    private IdentityAccessRules() {}
    public static void admit(CustomerAccessVerifier.Claim claim,IdentityAccount account,IdentitySession session,Instant time) {
        try {session.admit(account,time);}catch(DomainViolation rejected){throw invalid();}
        if(claim.authVersion()!=account.version().authVersion() || !claim.accountId().equals(session.accountId())
            || !claim.sessionId().equals(session.id()) || claim.issuedAt()==null || claim.expiresAt()==null
            || time.isBefore(claim.issuedAt()) || !time.isBefore(claim.expiresAt())
            || claim.issuedAt().isBefore(session.createdAt().truncatedTo(ChronoUnit.SECONDS))
            || !claim.expiresAt().isAfter(claim.issuedAt()) || claim.expiresAt().isAfter(claim.issuedAt().plusSeconds(900))
            || claim.expiresAt().isAfter(session.absoluteExpiresAt()) || claim.expiresAt().isAfter(session.idleExpiresAt()))throw invalid();
    }
    private static IdentityCommandFailure invalid(){return new IdentityCommandFailure(IdentityCommandFailure.Code.SESSION_INVALID);}
}
