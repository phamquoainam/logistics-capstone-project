package com.elogistic.backend.identity.internal.application.port;

import java.time.Instant;
import java.util.UUID;

/** Trusted per-request production access admission, NEVER a body/ID/boolean-based authorization.
 * T-002-06 verifies signature/issuer/audience/sub/jti/sid/iat/nbf/exp/authVersion in the
 * separate production trust domain. No Demo principal or unsigned AccessGrant fallback.
 * MEANINGFUL_ACTIVITY additionally attests an actually completed, authorized user action,
 * classified by trusted server use-case policy, never a client flag or polling request.
 * No default adapter: absent admission fails composition closed. */
@FunctionalInterface
public interface CurrentSessionAdmission {
    enum Operation { CURRENT_SESSION, MEANINGFUL_ACTIVITY }
    Context admit(Operation operation, UUID requestId);
    record Context(UUID sessionId, UUID accountId, long authVersion, Instant issuedAt, Instant expiresAt) {
        @Override public String toString() { return "CurrentSessionContext[redacted]"; }
    }
}
