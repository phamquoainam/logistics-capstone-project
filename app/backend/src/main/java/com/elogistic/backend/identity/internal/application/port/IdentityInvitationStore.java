package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

/** Identity-only V3-V5 transaction, sharing the existing administration lock. */
public interface IdentityInvitationStore {
    <T>T transaction(Function<Transaction,T> work);
    record StoredProof(ActionProof proof, String keyId, byte[] hash) {
        public StoredProof {hash=hash.clone();}
        @Override public byte[] hash(){return hash.clone();}
        @Override public String toString(){return "StoredInvitationProof[redacted]";}
    }
    record Receipt(UUID targetId, long version, byte[] fingerprint, Instant expiresAt) {
        public Receipt {fingerprint=fingerprint.clone();}
        @Override public byte[] fingerprint(){return fingerprint.clone();}
        @Override public String toString(){return "InvitationReceipt[redacted]";}
    }
    interface Transaction {
        IdentityGrantStore.Transaction admission();
        Optional<StoredProof> lockProof(UUID proofId);
        boolean eligibleRealm(UUID accountId,UUID proofId);
        Optional<Receipt> receipt(UUID proofId,UUID operationKey);
        void activate(IdentityAccount before,IdentityAccount.ProofChange change,String verifier,Instant now);
        void clearDelivery(UUID proofId,Instant now);
        void appendAudit(UUID accountId,UUID requestId,Instant now);
        void appendReceipt(UUID proofId,UUID key,Receipt receipt,long originalVersion,Instant now);
    }
}
