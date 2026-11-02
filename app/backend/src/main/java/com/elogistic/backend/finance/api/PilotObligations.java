package com.elogistic.backend.finance.api;
import java.time.Instant;
import java.util.UUID;
public interface PilotObligations {
    interface Access {UUID accountId();UUID requestId();void requireCurrent();}
    /** Server-created, live Station permission checked by workflow; no client-selected owner. */
    interface HandoverAccess {void requireCurrent();}
    record Obligation(UUID id,UUID accountId,UUID shipmentId,UUID quoteId,long amountVnd,String currency,String state,String payer,String method,String policyVersion,Instant createdAt){}
    Obligation recordUnpaid(Access access,UUID shipment,UUID quote,long amountVnd);
    Obligation get(Access access,UUID shipment);
    default void requireUnpaidHandover(HandoverAccess access,UUID owner,UUID shipment,UUID quote,long amountVnd){throw new IllegalStateException("PILOT_HANDOVER_FINANCE_REQUIRED");}
}
