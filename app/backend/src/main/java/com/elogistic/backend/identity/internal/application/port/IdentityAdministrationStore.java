package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public interface IdentityAdministrationStore {
    <T> T transaction(Function<Transaction,T> work);
    record Realm(UUID id, String displayName) {}
    record Receipt(UUID targetId, long originalVersion, long resultVersion, String fingerprint, Instant expiresAt) {}
    interface Transaction {
        IdentityGrantStore.Transaction accounts();
        Optional<Realm> activeRealm();
        List<UUID> visibleAccountIds(UUID actor, UUID realm, UUID after, int limit);
        Optional<Receipt> receipt(UUID actor, String operation, UUID key);
        void updateState(IdentityAccount before, IdentityAccount after, Instant now);
        void invalidateProofs(UUID account, Instant now);
        void appendProof(ActionProof proof, byte[] hash, String keyId);
        void audit(UUID actor, UUID target, UUID request, String operation, String reason, Instant now);
        void receipt(UUID actor, String operation, UUID key, Receipt receipt, int status, Instant now);
    }
}
