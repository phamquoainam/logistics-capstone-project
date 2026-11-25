package com.elogistic.backend.identity.api.admin;

import java.time.Instant;
import java.util.UUID;

/** Joins the current Identity command transaction. Merchant scope must be checked by the caller. */
public interface AdminGovernanceReauthentication {
    StepUpResult request(AdminCommandContext actor,AdminStepUpInput input,String authorityVersion,UUID key);
    void consume(AdminCommandContext actor,UUID proofId,String authorityVersion,String operation,UUID targetId,long expectedVersion,byte[] inputDigest);

    record AdminStepUpInput(String operation,UUID targetId,long expectedVersion,String commandDigest,String password){
        @Override public String toString(){return "AdminStepUpInput[redacted]";}
    }
    record StepUpResult(UUID proofId,Instant expiresAt){}
}
