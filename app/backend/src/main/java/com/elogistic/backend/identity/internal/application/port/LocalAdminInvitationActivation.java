package com.elogistic.backend.identity.internal.application.port;

import java.util.UUID;

/** AON-IF-02 verification-only inward boundary; caller owns its token buffer. */
@FunctionalInterface
public interface LocalAdminInvitationActivation {
    void accept(UUID proofId,byte[] token,String password,UUID operationKey,UUID requestId);
}
