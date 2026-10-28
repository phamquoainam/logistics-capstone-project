package com.elogistic.backend.identity.api.customer;

import java.util.UUID;

/** Request data only. Business authority requires CustomerAdmission.execute, never this object alone. */
public interface CustomerRequestContext {
    Request current();
    record Request(UUID requestId,String encodedAccess){@Override public String toString(){return "CustomerRequest[redacted]";}}
}
