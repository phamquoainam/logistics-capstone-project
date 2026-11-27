package com.elogistic.backend.identity.api.staff;

import java.util.UUID;
import java.util.function.Function;

/** Identity-owned OPS audience. A constructed record never conveys authority. */
public interface OperationalStaffAdmission {
    record Subject(UUID accountId, String displayName, long securityVersion) {}
    record Context(UUID requestId, UUID sessionId, Subject actor) {}
    <T> T execute(String encodedAccess, UUID requestId, Function<Context,T> work);
    default void requireCurrent(Context context) { throw new IllegalStateException("OPS_CONTEXT_REQUIRED"); }
    default void meaningfulAction(Context context) { requireCurrent(context); }
}
