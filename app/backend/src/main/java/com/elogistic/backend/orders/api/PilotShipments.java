package com.elogistic.backend.orders.api;
import com.elogistic.backend.pricing.api.PilotQuotes;
import java.time.Instant;
import java.util.*;
public interface PilotShipments {
    record Parcel(UUID id,int parcelNumber,ShippingDrafts.Parcel facts){}
    record Shipment(UUID id,UUID accountId,String code,UUID draftId,long draftVersion,String bookingState,String carrierState,ShippingDrafts.Input input,PilotQuotes.Quote quote,long totalVnd,Instant createdAt,List<Parcel> parcels){public Shipment{parcels=List.copyOf(parcels);}@Override public String toString(){return "PilotShipment[redacted]";}}
    record Page(List<Shipment> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Receipt(UUID targetId,String outcome){}
    Optional<Shipment> replay(ShippingDrafts.Access access,UUID draft,long version,UUID quote,UUID key);
    ShippingDrafts.Draft requireBookable(ShippingDrafts.Access access,UUID draft,long version);
    Shipment confirm(ShippingDrafts.Access access,UUID draft,long version,UUID quoteId,UUID key,UUID shipmentId,PilotQuotes.Quote quote);
    Shipment get(ShippingDrafts.Access access,UUID id);
    Page list(ShippingDrafts.Access access,int page,String search,String status);
    Receipt receipt(ShippingDrafts.Access access,UUID key);
}
