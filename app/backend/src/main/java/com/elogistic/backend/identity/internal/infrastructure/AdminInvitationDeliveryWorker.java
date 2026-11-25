package com.elogistic.backend.identity.internal.infrastructure;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Existing protected outbox and SMTP transport; sending never activates an account or realm. */
public final class AdminInvitationDeliveryWorker {
    private final JdbcInvitationOutbox outbox;private final SmtpCustomerDelivery transport;private final BooleanSupplier available;
    public AdminInvitationDeliveryWorker(JdbcInvitationOutbox outbox,SmtpCustomerDelivery transport,BooleanSupplier available){this.outbox=Objects.requireNonNull(outbox);this.transport=Objects.requireNonNull(transport);this.available=Objects.requireNonNull(available);}
    public boolean runOne(){
        if(!available.getAsBoolean())return false;
        var claim=outbox.claim();if(claim.isEmpty())return false;var lease=claim.get();JdbcInvitationOutbox.Outcome result;
        try(var delivery=outbox.open(lease)){
            var sent=transport.sendAdminInvitation(delivery);
            result=switch(sent.kind()){case ACCEPTED_BY_PROVIDER->JdbcInvitationOutbox.Outcome.SENT;case TRANSIENT_FAILURE->JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE;default->JdbcInvitationOutbox.Outcome.PERMANENT_FAILURE;};
        }
        outbox.complete(lease,result);return true;
    }
}
