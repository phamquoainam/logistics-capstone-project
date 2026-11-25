package com.elogistic.backend.identity.api.admin;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.UUID;

/** Server-created callback capability. No captured/constructed instance authorizes a later request. */
public interface AdminCommandContext {
    UUID accountId(); UUID realmId(); UUID sessionId(); UUID requestId();
    long authVersion(); String permissionVersion(); String environment();
    void requireCurrent();
    CustomerAdmission.Subject customer(UUID referencedAccountId);
    AdminEligibility admin(UUID referencedAccountId);

    record AdminEligibility(UUID accountId,UUID realmId,String displayName,String permissionVersion,boolean eligible) {
        @Override public String toString(){return "AdminEligibility[redacted]";}
    }
}
