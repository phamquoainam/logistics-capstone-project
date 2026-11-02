package com.elogistic.backend.orders.api;
import java.time.Instant;
import java.util.*;
/** Orders-owned stable labels and business events; Carrier access rechecks live scoped authority. */
public interface PilotHandling {
    interface CarrierAccess {UUID actorId();UUID requestId();String stationCode();void requireCurrent();}
    record ParcelState(UUID parcelId,int parcelNumber,String barcode,String state) {}
    record Event(UUID id,String type,String stationCode,Instant occurredAt,String provenance) {}
    record Handling(UUID shipmentId,String state,long version,List<ParcelState> parcels,List<Event> events,boolean labelAvailable,boolean simulation) {
        public Handling {parcels=List.copyOf(parcels);events=List.copyOf(events);}
    }
    record Cargo(PilotShipments.Shipment shipment,Handling handling) {public String dropOffCode(){return shipment.quote().route().dropOffCode();}@Override public String toString(){return "PilotCargo[redacted]";}}
    Handling self(ShippingDrafts.Access access,UUID shipment);
    Cargo lookup(CarrierAccess access,String barcode);
    Cargo atStation(CarrierAccess access,UUID shipment);
    void accept(CarrierAccess access,UUID shipment,long expectedVersion,UUID acceptance,Instant occurredAt);
    byte[] label(ShippingDrafts.Access access,UUID shipment);
    byte[] stationLabel(CarrierAccess access,UUID shipment);
}
