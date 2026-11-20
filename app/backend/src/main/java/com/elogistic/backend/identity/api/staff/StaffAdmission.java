package com.elogistic.backend.identity.api.staff;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/** Callback-scoped Staff authority; references never become authenticated actors. */
public interface StaffAdmission {
    enum PrincipalType { STAFF, CUSTOMER, NONE }
    record Subject(UUID accountId,String displayName,PrincipalType principalType,long securityVersion,boolean eligible) {}
    record Context(UUID requestId,UUID sessionId,Subject actor,Map<UUID,Subject> subjects) {
        public Context { subjects=Map.copyOf(subjects); }
    }
    <T>T execute(String encodedAccess,UUID requestId,Set<UUID> referencedAccounts,Function<Context,T> work);
    void requireCurrent(Context context);
    void meaningfulAction(Context context);
}
