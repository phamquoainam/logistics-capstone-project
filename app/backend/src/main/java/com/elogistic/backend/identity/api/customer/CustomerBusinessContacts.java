package com.elogistic.backend.identity.api.customer;

import java.time.Instant;
import java.util.UUID;

/** Proof of contact possession only; Merchant owns application/representative eligibility. */
public interface CustomerBusinessContacts {
    ProofAccepted request(CustomerAdmission.Context actor,UUID applicationId,String email,UUID key);
    BusinessContactAttestation confirm(CustomerAdmission.Context actor,UUID applicationId,UUID proofId,byte[] token,UUID key);
    /** P0-BC-02: compare the exact contact in the locked candidate submission. */
    void consumeAttestation(CustomerAdmission.Context actor,UUID attestationId,UUID applicationId,long revision,String expectedEmail);
    /** Historical used fact; no proof consume, mail or grant side effect. */
    default BusinessContactAttestation submittedAttestation(CustomerAdmission.Context actor,UUID attestationId,UUID applicationId,long revision,String expectedEmail){throw new com.elogistic.backend.identity.api.IdentityCommandFailure(com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED);}
    default BusinessContactAttestation submittedAttestation(com.elogistic.backend.identity.api.admin.AdminCommandContext actor,UUID attestationId,UUID applicationId,UUID applicantId,long revision,String expectedEmail){throw new com.elogistic.backend.identity.api.IdentityCommandFailure(com.elogistic.backend.identity.api.IdentityCommandFailure.Code.TOKEN_INVALID_OR_EXPIRED);}
    /** Finite public failure prevents exposing an internal limiter type across module boundaries. */
    final class Limited extends RuntimeException {
        private final int retryAfter;
        public Limited(int seconds){super("RATE_LIMITED");retryAfter=Math.max(1,Math.min(3600,seconds));}
        public int retryAfter(){return retryAfter;}
    }
    record ProofAccepted(UUID requestId,boolean accepted){}
    record BusinessContactAttestation(UUID id,UUID applicationId,Instant verifiedAt,Instant expiresAt){}
}
