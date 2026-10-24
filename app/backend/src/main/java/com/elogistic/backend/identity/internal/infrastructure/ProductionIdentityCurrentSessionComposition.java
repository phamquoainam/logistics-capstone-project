package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityCurrentSessionCommands;
import com.elogistic.backend.identity.internal.application.IdentityCurrentSessions;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import java.time.Clock;
import java.util.Objects;
import javax.sql.DataSource;

/** Explicit/unregistered. Production access and meaningful-action admission MUST be supplied.
 * No permissive provider, Demo fallback, controller, JWT signer or automatic idle heartbeat. */
public final class ProductionIdentityCurrentSessionComposition {
    private ProductionIdentityCurrentSessionComposition() {}
    public static IdentityCurrentSessionCommands create(DataSource runtime,CurrentSessionAdmission admission,Clock clock) {
        return new IdentityCurrentSessions(new JdbcIdentitySessionStore(runtime),Objects.requireNonNull(admission),clock);
    }
}
