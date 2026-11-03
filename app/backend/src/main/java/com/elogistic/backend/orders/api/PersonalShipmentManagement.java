package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Self-owned read projection; no new Shipment lifecycle or financial authority. */
public interface PersonalShipmentManagement {
    record Summary(String totalShipments,String waitingForHandover,String atStation){}
    record Item(PilotShipments.Shipment shipment,String status){}
    record Page(Instant generatedAt,Summary summary,String total,int page,boolean hasNext,List<Item> items){
        public Page{items=List.copyOf(items);}
    }
    Page read(ShippingDrafts.Access access,Map<String,String> parameters);
}
