package com.elogistic.backend.identity.api.customer;

import java.util.UUID;

/** Preauth possession flow; stored verified email is the only recovery destination. */
public interface CustomerRecoveryCommands {
    CustomerEnrollmentCommands.Accepted request(Email command);
    void confirm(Confirmation command);
    record Email(String email,UUID key,UUID requestId){@Override public String toString(){return "CustomerRecoveryRequest[redacted]";}}
    record Confirmation(UUID proofId,String proof,String newPassword,UUID key,UUID requestId){@Override public String toString(){return "CustomerRecoveryConfirmation[redacted]";}}
}
