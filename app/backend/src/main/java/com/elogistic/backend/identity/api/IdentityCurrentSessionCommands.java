package com.elogistic.backend.identity.api;

import java.util.UUID;

/** Backend-only Application boundary. No client account/session ID or activity flag is accepted.
 * T-002-06 supplies verified request admission; this does not register HTTP/JWT endpoints. */
public interface IdentityCurrentSessionCommands {
    IdentityGrantCommands.Account getIdentityMe(UUID requestId);
    /** Called only for a completed, authorized meaningful action attested by trusted admission.
     * Refresh, CSRF, health, /me and background polling must never qualify. */
    void recordMeaningfulActivity(UUID requestId);
}
