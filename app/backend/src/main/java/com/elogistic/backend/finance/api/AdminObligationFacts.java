package com.elogistic.backend.finance.api;

import java.math.BigInteger;
import java.util.*;

/** Confirmed obligations, never receipts, revenue, COD or a ledger mutation. */
public interface AdminObligationFacts {
    interface Access { void requireFinanceFacts(); }
    record Row(UUID shipmentId,BigInteger amountVnd,String state) {}
    List<Row> capture(Access actor,Set<UUID> shipments);
}
