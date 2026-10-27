package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** Mandatory same-request HTTPS/Origin/CSRF evidence established only by the customer HTTP adapter. */
@FunctionalInterface
public interface CustomerRequestAdmission {
    void authorize(UUID requestId, UUID sessionId);
}
