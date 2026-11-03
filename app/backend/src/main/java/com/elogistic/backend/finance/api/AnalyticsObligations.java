package com.elogistic.backend.finance.api;

import java.util.*;

/** Finance-owned bounded self query. No collection or inferred payment authority. */
public interface AnalyticsObligations {
    record Fact(UUID shipmentId,UUID quoteId,long amountVnd,String currency,String state) {}
    List<Fact> read(PilotObligations.Access access,List<UUID> shipmentIds);
}
