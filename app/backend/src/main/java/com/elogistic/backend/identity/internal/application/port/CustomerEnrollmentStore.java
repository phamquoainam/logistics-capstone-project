package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Identity-only physical transaction. No SMTP or private-storage operation may run in it. */
public interface CustomerEnrollmentStore {
    <T>T transaction(Function<Transaction,T> work);
    record Receipt(String operation,String keyId,byte[] fingerprint,UUID requestId){
        public Receipt{fingerprint=fingerprint.clone();}
        @Override public byte[] fingerprint(){return fingerprint.clone();}
        @Override public String toString(){return "CustomerReceipt[redacted]";}
    }
    record StoredProof(CustomerProof proof,UUID enrollmentId,String verifierKey,byte[] verifier){
        public StoredProof{verifier=verifier.clone();}
        @Override public byte[] verifier(){return verifier.clone();}
        @Override public String toString(){return "StoredCustomerProof[redacted]";}
    }
    interface Transaction {
        IdentityGrantStore.Transaction accounts();
        Optional<Receipt> receipt(UUID binding,UUID key);
        void receipt(UUID binding,UUID key,Receipt receipt,Instant now);
        boolean emailOccupied(String exactEmail);
        Optional<CustomerEnrollment> pending(String exactEmail);
        Optional<CustomerEnrollment> enrollment(UUID id);
        Optional<StoredProof> proof(UUID id);
        void insert(CustomerEnrollment enrollment);
        void update(CustomerEnrollment before,CustomerEnrollment after);
        void revokePending(UUID enrollment,Instant now);
        void issue(CustomerEnrollment enrollment,CustomerProof proof,CustomerProofProtection.Protected protectedProof,Instant now);
        void activate(CustomerEnrollment enrollment,CustomerProof proof,Instant now);
        void audit(UUID subject,UUID request,String event,Instant now);
    }
}
