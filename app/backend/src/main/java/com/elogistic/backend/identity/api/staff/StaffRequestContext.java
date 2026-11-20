package com.elogistic.backend.identity.api.staff;

import java.util.UUID;

/** Server request metadata only; StaffAdmission must authorize every operation. */
public interface StaffRequestContext {
    Request current();
    record Request(UUID requestId,String encodedAccess) {
        @Override public String toString(){return "StaffRequest[redacted]";}
    }
}
