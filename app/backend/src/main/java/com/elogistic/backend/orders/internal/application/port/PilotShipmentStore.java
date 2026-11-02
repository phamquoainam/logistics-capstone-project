package com.elogistic.backend.orders.internal.application.port;
import com.elogistic.backend.orders.api.PilotShipments.*;
import java.util.*;
public interface PilotShipmentStore {
    record BookingReceipt(UUID draft,long version,UUID quote,UUID shipment){}
    Optional<BookingReceipt> receipt(UUID owner,UUID key);
    Shipment find(UUID owner,UUID id);
    List<Shipment> list(UUID owner,int page,String search,String status);
    void insert(Shipment shipment);
    void receipt(UUID owner,UUID key,BookingReceipt receipt,java.time.Instant now);
}
