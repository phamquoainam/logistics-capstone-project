package com.elogistic.backend.orders.api;

import com.elogistic.backend.pricing.api.BusinessQuotes;
import java.time.Instant;
import java.util.*;

public interface BusinessShipments {
    record Shipment(UUID id,UUID organizationId,UUID facilityId,UUID creatorAccountId,String code,UUID preparationId,long preparationVersion,
                    String bookingState,String carrierState,ShippingDrafts.Input input,BusinessQuotes.Quote quote,long totalVnd,Instant createdAt,List<PilotShipments.Parcel> parcels){
        public Shipment{parcels=List.copyOf(parcels);}
        @Override public String toString(){return "BusinessShipment[redacted]";}
    }
    record Page(List<Shipment> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    Optional<Shipment> replay(BusinessPreparations.Access access,UUID preparation,long version,UUID quote,UUID key);
    BusinessPreparations.Draft requireBookable(BusinessPreparations.Access access,UUID preparation,long version);
    Shipment confirm(BusinessPreparations.Access access,BusinessPreparations.Draft draft,BusinessQuotes.Quote quote,UUID key,UUID shipment);
    Shipment get(BusinessPreparations.Access access,UUID id);
    Page list(BusinessPreparations.Access access,int page,String search,String status);
    UUID receipt(BusinessPreparations.Access access,UUID key);
    byte[] labels(BusinessPreparations.Access access,UUID id);
}
