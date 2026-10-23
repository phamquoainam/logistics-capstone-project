package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** Per-request trusted security port: exact Origin, server-bound CSRF, family binding and throttling.
 * A caller-supplied request/account/session ID is NOT proof. Implementations must validate actual
 * server-side request state; no default implementation or boolean client admission flag exists.
 * LOGIN/REFRESH rate limiting and HTTP cookie/challenge rebinding belong to T-002-06. */
@FunctionalInterface
public interface SessionAdmission {
    enum Operation { LOGIN, REFRESH, LOGOUT }
    void authorize(Operation operation, UUID requestId, UUID resolvedSessionId, boolean refreshPresent);
}
