package com.elogistic.backend.identity.api;

import java.util.List;
import java.util.UUID;

/** Backend-only command boundary. The security adapter supplies a verified session ID,
 * never authorities or a Demo principal. This interface is not an HTTP binding. */
public interface IdentityGrantCommands {
    Result replaceIdentityGrants(Command command);

    record GrantData(String role, String scopeType, UUID scopeId) {}

    record Command(UUID sessionId, UUID targetId, List<GrantData> grants,
                   long expectedVersion, UUID operationKey, String reason, UUID requestId) {
        public Command { if (grants != null) grants = List.copyOf(grants); }
        @Override public String toString() { return "IdentityGrantCommand[redacted]"; }
    }

    /** No credentials, persistence entities or internal Domain types cross this API. */
    record Account(UUID id, String username, String displayName, String email, String status,
                   List<GrantData> grants, long version, boolean emailVerified) {
        public Account { grants = List.copyOf(grants); }
        @Override public String toString() { return "IdentityAccountResult[redacted]"; }
    }

    /** Replay carries a freshly authorized account view and the original applied version.
     * HTTP/ETag rendering is intentionally left to the later API adapter. */
    record Result(Account account, long appliedVersion, boolean replayed) {}
}
