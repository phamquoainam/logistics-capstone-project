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
    record CarrierCargo(UUID shipmentId,String shipmentCode,String ownerType,UUID accountId,UUID organizationId,UUID facilityId,
                        UUID quoteId,long totalVnd,Instant createdAt,ShippingDrafts.Input input,com.elogistic.backend.pricing.api.PilotQuotes.Route route,
                        List<PilotShipments.Parcel> parcels,Handling handling) {
        public CarrierCargo {
            parcels=List.copyOf(parcels);
            Objects.requireNonNull(createdAt,"CARRIER_BOOKING_TIME_REQUIRED");
            if (!("PERSONAL".equals(ownerType)&&accountId!=null&&organizationId==null&&facilityId==null)
                && !("BUSINESS".equals(ownerType)&&accountId==null&&organizationId!=null)) throw new IllegalArgumentException("CARRIER_OWNER_BINDING_REQUIRED");
        }
        @Override public String toString(){return "CarrierCargo[redacted]";}
    }
    default CarrierCargo carrierLookup(CarrierAccess actor,String barcode){throw new IllegalStateException("CARRIER_HANDLING_UNAVAILABLE");}
    default CarrierCargo carrierAtStation(CarrierAccess actor,UUID shipment){throw new IllegalStateException("CARRIER_HANDLING_UNAVAILABLE");}
    default void carrierAccept(CarrierAccess actor,UUID shipment,long expectedVersion,UUID acceptanceId,Instant occurredAt){throw new IllegalStateException("CARRIER_HANDLING_UNAVAILABLE");}
    default byte[] carrierLabel(CarrierAccess actor,UUID shipment){throw new IllegalStateException("CARRIER_HANDLING_UNAVAILABLE");}
    default Handling businessSelf(BusinessPreparations.Access actor,UUID shipment){throw new IllegalStateException("CARRIER_HANDLING_UNAVAILABLE");}
    Handling self(ShippingDrafts.Access access,UUID shipment);
    Cargo lookup(CarrierAccess access,String barcode);
    Cargo atStation(CarrierAccess access,UUID shipment);
    void accept(CarrierAccess access,UUID shipment,long expectedVersion,UUID acceptance,Instant occurredAt);
    byte[] label(ShippingDrafts.Access access,UUID shipment);
    byte[] stationLabel(CarrierAccess access,UUID shipment);
}
