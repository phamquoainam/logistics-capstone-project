package com.elogistic.backend.finance.api;

import java.time.Instant;
import java.util.UUID;
import java.util.List;

public interface BusinessObligations {
    interface Access {UUID actorAccountId();UUID requestId();UUID organizationId();void requirePermission(String permission,UUID facilityId);void requireCurrent();}
    record Obligation(UUID id,UUID organizationId,UUID facilityId,UUID shipmentId,UUID quoteId,long amountVnd,String currency,String state,String payer,String method,String policyVersion,Instant createdAt){}
    Obligation recordUnpaid(Access access,UUID facility,UUID shipment,UUID quote,long amount);
    Obligation get(Access access,UUID facility,UUID shipment);
    default void requireUnpaidHandover(PilotObligations.HandoverAccess actor,UUID organizationId,UUID facilityId,UUID shipmentId,UUID quoteId,long amountVnd){throw new IllegalStateException("BUSINESS_HANDOVER_FINANCE_REQUIRED");}
    record Captured(UUID shipmentId,UUID quoteId,UUID facilityId,long amountVnd){}
    /** Verify every captured immutable Orders binding, in bounded chunks. */
    default void validateCaptured(Access access,List<Captured> rows){throw new IllegalStateException("BUSINESS_FINANCE_COHORT_REQUIRED");}
}
