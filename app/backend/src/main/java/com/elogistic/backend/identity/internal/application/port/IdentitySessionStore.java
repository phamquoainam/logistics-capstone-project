package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

public interface IdentitySessionStore {
    <T>T transaction(Function<Transaction,T> work);
    record Refresh(UUID id, UUID sessionId, Instant expiresAt, Instant consumedAt, Instant revokedAt) {
        @Override public String toString() { return "StoredRefresh[redacted]"; }
    }
    interface Transaction {
        IdentityGrantStore.Transaction accounts();
        Optional<UUID> findUsername(String exactUsername);
        String passwordVerifier(UUID accountId);
        boolean eligibleRealm(UUID accountId);
        Optional<Refresh> findRefresh(byte[] digest);
        Optional<RefreshFamily> family(IdentitySession session);
        void insertSession(IdentitySession session);
        void insertRefresh(UUID id, UUID predecessor, IdentitySession session, byte[] digest, Instant now);
        void consumeRefresh(UUID id, Instant now);
        boolean revokeFamily(UUID sessionId, Instant now);
        /** Only Domain-approved idle/last-activity fields; caller uses the owned transaction. */
        void recordActivity(IdentitySession before, IdentitySession after);
        void audit(UUID account, UUID request, String event, Instant now);
    }
}
