package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Reduced source-owned facts, never contacts or mutable booking commands. */
public interface AnalyticsShipments {
    record Fact(UUID id,String code,UUID quoteId,Instant bookedAt,long confirmedVnd,
                String origin,String destination,String datasetVersion,Instant acceptedAt,List<Long> weights,List<Long> chargeableWeights) {
        public Fact { weights=List.copyOf(weights);chargeableWeights=List.copyOf(chargeableWeights); }
    }
    List<Fact> read(ShippingDrafts.Access access,Instant from,Instant until);
}
