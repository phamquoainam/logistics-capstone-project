package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentitySessionCommands;
import com.elogistic.backend.identity.internal.application.IdentitySessions;
import com.elogistic.backend.identity.internal.application.port.SessionAdmission;
import java.time.Clock;
import javax.sql.DataSource;

/** Explicit unregistered composition. A genuine per-request security adapter is mandatory.
 * No HTTP, JWT signer, CSRF/throttle implementation, default admission or Demo fallback. */
public final class ProductionIdentitySessionComposition {
    private ProductionIdentitySessionComposition() {}
    public static IdentitySessionCommands create(DataSource runtime,SessionAdmission admission,Clock clock) {
        java.util.Objects.requireNonNull(admission);
        return new IdentitySessions(new JdbcIdentitySessionStore(runtime),new ProtectedSessionCredentials(),admission,clock);
    }
}
