package com.elogistic.backend.identity.api.customer;

import java.util.*;
import java.util.function.Function;

/** Identity owns the physical business-admission transaction. No caller actor ID or grant is trusted. */
public interface CustomerAdmission {
    <T> T execute(String encodedAccess, UUID requestId, Set<UUID> referencedAccounts, Function<Context,T> work);
    /** Mandatory at every business public boundary. A constructed/captured context is not authority. */
    void requireCurrent(Context context);
    /** Only a newly committed successful business command qualifies, never receipt replay/read/poll. */
    void meaningfulAction(Context context);

    record Subject(UUID accountId, String displayName, String verifiedEmail, long securityVersion, boolean eligible) {
        @Override public String toString() {return "CustomerSubject[redacted]";}
    }
    record Context(UUID requestId, UUID sessionId, Subject actor, Map<UUID,Subject> subjects) {
        public Context {subjects=Map.copyOf(subjects);}
        @Override public String toString() {return "CustomerAdmissionContext[redacted]";}
    }
}
