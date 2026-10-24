package com.elogistic.backend.identity.internal.application.port;

import java.time.Instant;
import java.util.UUID;

/** Authoritative persisted timing for conversion from integer JWT NumericDate. No authority fallback. */
@FunctionalInterface
public interface AccessSessionTiming {
    Instant createdAt(UUID sessionId, UUID accountId, long authVersion);
}
