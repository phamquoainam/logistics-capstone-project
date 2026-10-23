package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityGrantCommands;
import com.elogistic.backend.identity.internal.application.FailClosedGrantAuthority;
import com.elogistic.backend.identity.internal.application.ReplaceIdentityGrants;
import com.elogistic.backend.identity.internal.application.port.GrantAuthority;
import java.time.Clock;
import java.time.Duration;
import javax.sql.DataSource;

/** Explicit module-local production composition, not an HTTP principal adapter.
 * Default authority implements approved Identity-only same-Realm delegation.
 * Unsupported/external authority still denies after current persisted admission.
 * No Demo component, automatic profile/configuration or permissive provider is installed.
 */
public final class ProductionIdentityGrantComposition {
    private ProductionIdentityGrantComposition() {}

    public static IdentityGrantCommands create(DataSource runtime, Clock clock, Duration retention) {
        return create(runtime, new JdbcIdentityRealmGrantAuthority(runtime), clock, retention);
    }

    /** Trusted infrastructure injection only, never bind this port from client input.
     * Delegate availability alone proves no authority: unchanged Domain guards validate
     * actor/authVersion/target/version and complete current/requested grant sets.
     * Business-owner providers require approved public contracts before installation.
     */
    public static IdentityGrantCommands create(DataSource runtime, GrantAuthority resourceAuthority,
                                               Clock clock, Duration retention) {
        return new ReplaceIdentityGrants(new JdbcIdentityGrantStore(runtime),
            new FailClosedGrantAuthority(resourceAuthority), clock, retention);
    }
}
