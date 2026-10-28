package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.CustomerProof;
import java.time.Instant;
import java.util.*;

/** Identity-owned consent and immutable receipt in the same admitted transaction as recent-proof consumption. */
public interface CustomerOwnerConsentStore {
    record Receipt(CustomerEnrollmentStore.Receipt binding,CustomerProof proof,UUID sourceReauthentication){
        @Override public String toString(){return "OwnerConsentReceipt[redacted]";}
    }
    Optional<Receipt> receipt(UUID account,UUID key);
    void replace(CustomerProof proof,Instant now);
    void receipt(UUID account,UUID key,Receipt receipt,Instant now);
    void audit(UUID account,UUID request,Instant now);
}
