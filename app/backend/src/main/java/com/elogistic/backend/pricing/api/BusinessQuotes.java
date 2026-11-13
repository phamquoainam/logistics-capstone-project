package com.elogistic.backend.pricing.api;

import java.time.Instant;
import java.util.*;

/** BSC-06: explicit Merchant ownership, never an organization UUID disguised as an account. */
public interface BusinessQuotes {
    interface Access {
        UUID actorAccountId(); UUID requestId(); UUID organizationId();
        void requirePermission(String permission,UUID facilityId);
        void requireCurrent(); void meaningfulAction();
    }
    record Contract(UUID id,UUID organizationId,long version,String tariffVersion,String state,Instant effectiveFrom,Instant effectiveTo){}
    record Request(UUID preparationId,long preparationVersion,UUID facilityId,PilotQuotes.Route route,List<PilotQuotes.Parcel> parcels,boolean ordinaryGoodsConfirmed){public Request{parcels=List.copyOf(parcels);}}
    record Quote(UUID id,UUID organizationId,UUID facilityId,UUID creatorAccountId,UUID preparationId,long preparationVersion,
                 UUID contractId,long contractVersion,String tariffVersion,PilotQuotes.Route route,List<PilotQuotes.Parcel> parcels,
                 List<PilotQuotes.Line> feeLines,long totalVnd,String taxTreatment,Instant createdAt,Instant expiresAt,UUID consumedBy){
        public Quote{parcels=List.copyOf(parcels);feeLines=List.copyOf(feeLines);}
        @Override public String toString(){return "BusinessQuote[redacted]";}
    }
    Optional<Contract> contract(Access access);
    PilotQuotes.Rules rules(Access access);
    Quote create(Access access,Request request,UUID key);
    Quote get(Access access,UUID id);
    boolean usable(Access access,Quote quote);
    UUID receipt(Access access,UUID key);
    Quote consume(Access access,UUID id,UUID preparation,long version,UUID shipment);
}
