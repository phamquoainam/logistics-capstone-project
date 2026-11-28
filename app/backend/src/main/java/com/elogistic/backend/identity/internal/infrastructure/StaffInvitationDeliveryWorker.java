package com.elogistic.backend.identity.internal.infrastructure;

import java.util.Objects;

/** Bounded existing protected outbox. SMTP outcome never activates an account or issues authority. */
public final class StaffInvitationDeliveryWorker {
    private final JdbcInvitationOutbox outbox;
    private final boolean operations;
    private final SmtpCustomerDelivery transport;
    private final java.util.function.BooleanSupplier available;
    public StaffInvitationDeliveryWorker(JdbcInvitationOutbox outbox,SmtpCustomerDelivery transport){this(outbox,transport,()->true);}
    public StaffInvitationDeliveryWorker(JdbcInvitationOutbox outbox,SmtpCustomerDelivery transport,java.util.function.BooleanSupplier available){this(outbox,transport,available,false);}
    public StaffInvitationDeliveryWorker(JdbcInvitationOutbox outbox,SmtpCustomerDelivery transport,java.util.function.BooleanSupplier available,boolean operations){this.operations=operations;this.outbox=Objects.requireNonNull(outbox);this.transport=Objects.requireNonNull(transport);this.available=Objects.requireNonNull(available);}
    public boolean runOne(){
        if(!available.getAsBoolean())return false;
        var claim=outbox.claim();if(claim.isEmpty())return false;
        var lease=claim.get();JdbcInvitationOutbox.Outcome result;
        try(var delivery=outbox.open(lease)){
            var sent=operations?transport.sendOperationsInvitation(delivery):transport.sendStaffInvitation(delivery);
            result=switch(sent.kind()){case ACCEPTED_BY_PROVIDER->JdbcInvitationOutbox.Outcome.SENT;case TRANSIENT_FAILURE->JdbcInvitationOutbox.Outcome.TRANSIENT_FAILURE;default->JdbcInvitationOutbox.Outcome.PERMANENT_FAILURE;};
        }
        outbox.complete(lease,result);return true;
    }
}
