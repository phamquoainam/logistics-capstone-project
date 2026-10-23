package com.elogistic.backend.identity.internal.infrastructure;

import com.elogistic.backend.identity.internal.application.port.InvitationIssuer;
import com.elogistic.backend.identity.internal.domain.*;
import java.time.Instant;
import java.util.*;

/** Production crypto + durable V5 adapter. No network, raw token return, sender or activation. */
public final class SecureInvitationIssuer implements InvitationIssuer {
    private final InvitationProtection protection;
    private final JdbcInvitationOutbox outbox;
    public SecureInvitationIssuer(InvitationProtection protection,JdbcInvitationOutbox outbox) {
        this.protection=Objects.requireNonNull(protection);this.outbox=Objects.requireNonNull(outbox);
    }
    @Override public Prepared prepare(IdentityAccount pending,String destination,Instant now) {
        if(pending==null || pending.activation()!=IdentityAccount.Activation.UNVERIFIED || pending.grants().size()!=1)
            throw new IllegalStateException("INVITATION_PENDING_ACCOUNT_REQUIRED");
        var grant=pending.grants().iterator().next();
        if(!grant.role().value().equals("IDENTITY_MANAGEMENT") || grant.scope().type()!=ResourceScope.Type.IDENTITY)
            throw new IllegalStateException("INVITATION_REALM_REQUIRED");
        var proof=ActionProof.invitation(UUID.randomUUID(),pending,now);
        var context=protection.context(proof,grant.scope().id(),destination);
        var sealed=protection.issue(context);
        return new Prepared() {
            private boolean closed,appended;
            private void check(){if(closed)throw new IllegalStateException("INVITATION_PREPARATION_CLOSED");}
            public ActionProof proof(){check();return proof;}
            public byte[] tokenHash(){check();return sealed.verifier();}
            public String verifierKeyId(){check();return context.verifierKeyId();}
            public void appendDeliveryIntent(){
                check();if(appended)throw new IllegalStateException("INVITATION_ALREADY_QUEUED");
                outbox.enqueue(context,sealed);appended=true;
            }
            public void close(){sealed.close();closed=true;}
            @Override public String toString(){return "PreparedInvitation[redacted]";}
        };
    }
}
