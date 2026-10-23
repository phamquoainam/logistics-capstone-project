package com.elogistic.backend.identity.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Backend-only accepted provisionIdentityAccount boundary; verified session comes from security.
 * No client target ID, password, verification flag, Realm registration or bootstrap operation. */
public interface IdentityProvisionCommands {
    IdentityGrantCommands.Result provisionIdentityAccount(Command command);

    record Command(UUID sessionId, String username, String displayName, String email,
                   List<IdentityGrantCommands.GrantData> grants, UUID operationKey, UUID requestId) {
        public Command { if (grants != null) grants = Collections.unmodifiableList(new ArrayList<>(grants)); }
        @Override public String toString() { return "IdentityProvisionCommand[redacted]"; }
    }
}
