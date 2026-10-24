package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.api.IdentityAdministrationCommands;
import com.elogistic.backend.identity.internal.application.IdentityAdministration;
import com.elogistic.backend.identity.internal.application.port.CurrentSessionAdmission;
import java.time.*;
import javax.sql.DataSource;

/** Explicit/unregistered. No HTTP/JWT fallback, external authority or delivery worker. */
public final class ProductionIdentityAdministrationComposition {
    private ProductionIdentityAdministrationComposition(){}
    public static IdentityAdministrationCommands create(DataSource runtime,CurrentSessionAdmission admission,Clock clock,Duration retention){
        return new IdentityAdministration(new JdbcIdentityAdministrationStore(runtime),admission,new JdbcIdentityRealmGrantAuthority(runtime),null,clock,retention);
    }
    public static IdentityAdministrationCommands secure(DataSource runtime,CurrentSessionAdmission admission,InvitationProtection protection,JdbcInvitationOutbox.Policy policy,Clock clock,Duration retention){
        var outbox=new JdbcInvitationOutbox(runtime,protection,policy,clock);
        return new IdentityAdministration(new JdbcIdentityAdministrationStore(runtime),admission,new JdbcIdentityRealmGrantAuthority(runtime),new SecureInvitationIssuer(protection,outbox),clock,retention);
    }
}
