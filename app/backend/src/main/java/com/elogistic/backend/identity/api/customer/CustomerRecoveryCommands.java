package com.elogistic.backend.identity.api.customer;

import java.util.UUID;

/** Preauth possession flow; stored verified email is the only recovery destination. */
public interface CustomerRecoveryCommands {
    CustomerEnrollmentCommands.Accepted request(Email command);
    void confirm(Confirmation command);
    Verified verify(Verification command);
    record Verification(UUID proofId,String proof,UUID key,UUID requestId){@Override public String toString(){return "CustomerRecoveryVerification[redacted]";}}
    record Verified(UUID proofId,String proof,java.time.Instant expiresAt){@Override public String toString(){return "CustomerRecoveryVerified[redacted]";}}
    record Email(String email,UUID key,UUID requestId){@Override public String toString(){return "CustomerRecoveryRequest[redacted]";}}
    record Confirmation(UUID proofId,String proof,String newPassword,UUID key,UUID requestId){@Override public String toString(){return "CustomerRecoveryConfirmation[redacted]";}}
}
