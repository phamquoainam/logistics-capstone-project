package com.elogistic.backend.finance.api;

import java.time.Instant;
import java.util.UUID;

public interface BusinessObligations {
    interface Access {UUID actorAccountId();UUID requestId();UUID organizationId();void requirePermission(String permission,UUID facilityId);void requireCurrent();}
    record Obligation(UUID id,UUID organizationId,UUID facilityId,UUID shipmentId,UUID quoteId,long amountVnd,String currency,String state,String payer,String method,String policyVersion,Instant createdAt){}
    Obligation recordUnpaid(Access access,UUID facility,UUID shipment,UUID quote,long amount);
    Obligation get(Access access,UUID facility,UUID shipment);
}
