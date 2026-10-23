package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.application.ProvisionInput;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** Reuses the accepted Identity admission/administration transaction, never independent commits. */
public interface IdentityProvisionStore {
    <T> T transaction(Function<Transaction,T> work);
    record Receipt(UUID targetId, long resultVersion, String fingerprint) {}
    interface Transaction {
        IdentityGrantStore.Transaction admission();
        Optional<Receipt> receipt(UUID actor, UUID key);
        boolean usernameExists(Username username);
        void insertPending(IdentityAccount account, ProvisionInput input, Instant now);
        void appendProof(ActionProof proof, byte[] tokenHash, String verifierKeyId);
        void appendAudit(UUID actor, UUID target, UUID request, Instant now);
        void appendReceipt(UUID actor, UUID key, Receipt receipt, Instant now, Instant expires);
    }
}
