package com.elogistic.backend.identity.api.customer;

import java.time.Instant;
import java.util.UUID;

/** Recent password proof belongs to the current customer session; its UUID is never a bearer credential. */
public interface CustomerReauthentication {
    Result request(Command command);
    /** Joins the current admission transaction; caller's business failure rolls consumption back. */
    void consume(CustomerAdmission.Context context,UUID proofId);
    record Command(UUID key,String email,String password) {
        @Override public String toString(){return "CustomerReauthenticationCommand[redacted]";}
    }
    record Result(UUID proofId,Instant expiresAt) {}
}
