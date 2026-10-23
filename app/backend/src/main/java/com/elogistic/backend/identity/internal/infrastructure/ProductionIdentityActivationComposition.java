package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityInvitationCommands;
import com.elogistic.backend.identity.internal.application.AcceptIdentityInvitation;
import java.time.*;
import java.util.Objects;
import javax.sql.DataSource;

/** Explicit trusted composition only. No Spring registration, HTTP, bootstrap, default keys or sender. */
public final class ProductionIdentityActivationComposition {
    private ProductionIdentityActivationComposition() {}
    public static IdentityInvitationCommands create(DataSource runtime,InvitationProtection protection,Clock clock,Duration retention) {
        return new AcceptIdentityInvitation(new JdbcIdentityInvitationStore(Objects.requireNonNull(runtime)),
            new ProtectedInvitationCredentials(Objects.requireNonNull(protection)),clock,retention);
    }
}
