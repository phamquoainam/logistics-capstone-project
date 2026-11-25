package com.elogistic.backend.identity.api.admin;

import java.util.*;
import java.util.function.Function;

/** Identity owns one bounded SYSTEM governance transaction; neither IDs nor records are authority. */
public interface AdminCommandAdmission {
    <T> T execute(UUID requestId, ReferencedSubjects references, Function<AdminCommandContext,T> work);
    /** Fail closed for adapters which cannot prove exact issued-context ownership. */
    default void requireCurrent(AdminCommandContext context){throw new AdminFailure("ACCESS_DENIED");}

    record ReferencedSubjects(Set<UUID> customers, Set<UUID> admins) {
        public ReferencedSubjects {
            if(customers==null || admins==null || customers.stream().anyMatch(Objects::isNull) || admins.stream().anyMatch(Objects::isNull))
                throw new AdminFailure("VALIDATION_FAILED");
            customers=Set.copyOf(customers);admins=Set.copyOf(admins);
        }
        @Override public String toString(){return "ReferencedSubjects[redacted]";}
    }
}
