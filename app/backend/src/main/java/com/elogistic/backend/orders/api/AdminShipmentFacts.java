package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** PII-free confirmed facts; Finance uses the same cohort without gaining list permission. */
public interface AdminShipmentFacts {
    interface Access { void requireShipmentFacts(); }
    record Row(UUID id,String code,String ownerKind,Instant bookedAt,String status,int parcelCount) {}
    List<Row> capture(Access actor,Instant from,Instant until,Instant cutoff,String status);
}
