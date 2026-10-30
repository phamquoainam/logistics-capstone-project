package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** Mandatory live same-request TLS/Origin/preauth-CSRF plus durable abuse reservation.
 * Binding is the server-stored CSRF row ID, never a client-supplied authority field. */
public interface CustomerEnrollmentAdmission {
    enum Operation {ENROLL,RESEND,VERIFY,RECOVERY_REQUEST,RECOVERY_VERIFY,RECOVERY_CONFIRM}
    Binding admit(Operation operation,UUID requestId,String exactSubject);
    void requireCurrent(Binding binding);
    record Binding(UUID id){public Binding{if(id==null)throw new IllegalArgumentException("CUSTOMER_PREAUTH_REQUIRED");}}
}
