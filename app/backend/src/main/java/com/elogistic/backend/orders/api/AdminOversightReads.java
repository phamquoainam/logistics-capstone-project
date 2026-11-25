package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Masked facts only. Access/selector are server capabilities, never an HTTP request body. */
public interface AdminOversightReads {
    final class Failure extends RuntimeException {
        public Failure(String code){super(Set.of("ACCESS_DENIED","RESOURCE_NOT_FOUND","SERVICE_UNAVAILABLE").contains(code)?code:"SERVICE_UNAVAILABLE");}
        public String code(){return getMessage();}
    }
    interface Access {void requireOversightFacts();}
    record Selector(Instant from,Instant until,Instant cutoff,String status,Set<UUID> contributingIds){
        public Selector{contributingIds=Set.copyOf(contributingIds);}
    }
    record Parcel(UUID id,String code,long actualGrams,long lengthMm,long widthMm,long heightMm){}
    record Event(UUID id,String kind,Instant occurredAt){}
    record Acceptance(UUID eventId,Instant occurredAt,String stationCode){}
    record Facts(AdminShipmentFacts.Row shipment,List<Parcel> parcels,List<Event> tracking,Acceptance acceptance){
        public Facts{parcels=List.copyOf(parcels);tracking=List.copyOf(tracking);}
    }
    Facts get(Access actor,UUID shipmentId,Selector selector);
}
