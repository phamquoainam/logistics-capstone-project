package com.elogistic.backend.identity.internal.application.port;

import com.elogistic.backend.identity.api.IdentityGrantCommands.Account;
import com.elogistic.backend.identity.internal.domain.IdentityAccount;
import com.elogistic.backend.identity.internal.domain.IdentitySession;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/** One Identity-local transaction; all security-sensitive writes share its connection. */
public interface IdentityGrantStore {
    <T> T transaction(Function<Transaction, T> work);

    record Receipt(UUID targetId, long originalVersion, long resultVersion, String fingerprint) {}
    interface Transaction {
        /** Serialize all Identity admin-population changes before loading actor/target.
         * Future disable/provision paths must participate in the same lock protocol. */
        void lockAdministration();
        Optional<UUID> sessionAccount(UUID sessionId);
        Optional<IdentityAccount> lockAccount(UUID accountId);
        Optional<IdentitySession> lockSession(UUID sessionId);
        long activeAdministrators();
        Optional<Receipt> receipt(UUID actorId, UUID key);
        void replace(IdentityAccount before, IdentityAccount after, Instant now);
        void revokeSessions(UUID accountId, Instant now);
        void appendAudit(UUID actorId, UUID targetId, UUID requestId, String reason, Instant now);
        void appendReceipt(UUID actorId, UUID key, Receipt receipt, Instant now, Instant expiresAt);
        Account accountView(UUID targetId);
    }
}
