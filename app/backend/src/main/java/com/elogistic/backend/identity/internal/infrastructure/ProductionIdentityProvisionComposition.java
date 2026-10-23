package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityProvisionCommands;
import com.elogistic.backend.identity.internal.application.ProvisionIdentityAccount;
import com.elogistic.backend.identity.internal.application.port.InvitationIssuer;
import java.time.*;
import javax.sql.DataSource;

/** Explicit trusted-infrastructure composition only; never auto-enabled in Demo or HTTP.
 * Missing keys/adapter fail without writes. Explicit secure composition queues only: no sender. */
public final class ProductionIdentityProvisionComposition {
    private ProductionIdentityProvisionComposition() {}
    public static IdentityProvisionCommands create(DataSource runtime,Clock clock,Duration retention) {
        return create(runtime,null,clock,retention);
    }
    public static IdentityProvisionCommands create(DataSource runtime,InvitationIssuer invitations,Clock clock,Duration retention) {
        return new ProvisionIdentityAccount(new JdbcIdentityProvisionStore(runtime),
            new JdbcIdentityRealmGrantAuthority(runtime),invitations,clock,retention);
    }
    public static IdentityProvisionCommands secure(DataSource runtime,InvitationProtection protection,
                                                   JdbcInvitationOutbox.Policy policy,Clock clock,Duration retention) {
        if(protection==null)throw new IllegalStateException("PROVISIONING_INVITATION_KEYS_REQUIRED");
        if(policy==null)throw new IllegalStateException("PROVISIONING_INVITATION_DELIVERY_POLICY_REQUIRED");
        var outbox=new JdbcInvitationOutbox(runtime,protection,policy,clock);
        return create(runtime,new SecureInvitationIssuer(protection,outbox),clock,retention);
    }
}
