package com.elogistic.backend.identity.api.customer;

import java.time.Instant;
import java.util.UUID;

/** A current verified human consents for themself; this reference never grants or recovers ownership. */
public interface CustomerOwnerRecoveryConsents {
    Result create(Command command);
    record Command(UUID key,UUID organizationId,UUID reauthenticationProofId){
        @Override public String toString(){return "OwnerRecoveryConsentCommand[redacted]";}
    }
    record Result(UUID id,UUID organizationId,Instant expiresAt){
        @Override public String toString(){return "OwnerRecoveryConsent[redacted]";}
    }
}
