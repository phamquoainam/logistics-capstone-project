package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.time.Instant;
import java.util.*;

/** Identity SQL joins the exact DataSource already owned by CustomerAdmission. */
public interface CustomerReauthenticationStore {
    record Receipt(CustomerEnrollmentStore.Receipt binding,UUID sessionId,long authVersion,CustomerProof proof) {
        @Override public String toString(){return "CustomerReauthenticationReceipt[redacted]";}
    }
    String passwordVerifier(UUID account);
    Optional<Receipt> receipt(UUID account,UUID key);
    Optional<CustomerProof> proof(UUID id);
    void replace(CustomerProof proof,Instant now);
    void consume(CustomerProof before,CustomerProof after);
    void receipt(UUID account,UUID key,Receipt receipt,Instant now);
    void audit(UUID account,UUID request,String event,Instant now);
}
