package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Shared preparation domain. Access is server-created by the authorized workflow, never deserialized. */
public interface ShippingDrafts {
    interface Access { UUID accountId(); UUID requestId(); void requireCurrent(); void meaningfulAction(); }
    record Address(String country,String administrativeArea,String locality,String addressLine){@Override public String toString(){return "Address[redacted]";}}
    record Contact(String name,String phone,Address address){@Override public String toString(){return "Contact[redacted]";}}
    record Parcel(String description,long quantity,long weightGrams,long lengthMm,long widthMm,long heightMm,long declaredValueVnd){@Override public String toString(){return "Parcel[redacted]";}}
    record Input(String reference,String instructions,Contact sender,Contact recipient,List<Parcel> parcels){public Input{if(parcels!=null)parcels=List.copyOf(parcels);}@Override public String toString(){return "DraftInput[redacted]";}}
    record Draft(UUID id,UUID accountId,String state,Input input,long version,Instant createdAt,Instant updatedAt){@Override public String toString(){return "ShippingDraft[redacted]";}}
    record Page(List<Draft> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    Draft create(Access access,Input input,UUID key);
    Draft update(Access access,UUID id,Input input,long expected,UUID key);
    void archive(Access access,UUID id,long expected,UUID key);
    Draft get(Access access,UUID id);
    Page list(Access access,int page,String search);
    Receipt receipt(Access access,String operation,UUID key);
}
