package com.elogistic.backend.identity.internal.application.port;

import java.time.Instant;
import java.util.UUID;

/** Inward port for the purpose-separated customer RS256 verifier, with no SYSTEM/Demo fallback. */
@FunctionalInterface
public interface CustomerAccessVerifier {
    Claim verify(String encodedAccess);
    record Claim(UUID sessionId, UUID accountId, long authVersion, Instant issuedAt, Instant expiresAt) {
        @Override public String toString() {return "CustomerAccessClaim[redacted]";}
    }
}
