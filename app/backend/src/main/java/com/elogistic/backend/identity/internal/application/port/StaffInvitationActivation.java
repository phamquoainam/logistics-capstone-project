package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** Existing proof/credential transaction behind the Staff HTTP adapter. No operator provisioning authority. */
@FunctionalInterface
public interface StaffInvitationActivation {
    void accept(UUID proofId,byte[] token,String password,UUID key,UUID request);
}
