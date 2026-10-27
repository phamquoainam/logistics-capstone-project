package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Account/security mutation owns its physical commit and preserves grants/username/contact. */
public interface CustomerRecoveryStore {
    <T>T transaction(Function<Transaction,T> work);
    interface Transaction {
        IdentityGrantStore.Transaction accounts();
        Optional<UUID> accountForEmail(String exactEmail);
        Optional<String> currentEmail(UUID account);
        void lockSessions(UUID account);
        Optional<UUID> proofSubject(UUID proof);
        Optional<CustomerEnrollmentStore.StoredProof> proof(UUID proof);
        Optional<CustomerEnrollmentStore.Receipt> receipt(UUID binding,UUID key);
        void receipt(UUID binding,UUID key,CustomerEnrollmentStore.Receipt receipt,Instant now);
        Optional<CustomerEnrollmentStore.Receipt> proofReceipt(UUID proof,UUID key);
        void proofReceipt(UUID proof,UUID key,CustomerEnrollmentStore.Receipt receipt,Instant now);
        void revokePending(UUID account,Instant now);
        void issue(CustomerProof proof,String destination,CustomerProofProtection.Protected protectedProof,Instant now);
        void reset(IdentityAccount before,IdentityAccount.CustomerProofChange change,String passwordHash,byte[] suppliedProof,Instant now);
        void audit(UUID account,UUID request,String event,Instant now);
    }
}
