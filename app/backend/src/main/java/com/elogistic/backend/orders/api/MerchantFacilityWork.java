package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;

/** Orders-owned preparation of real booked Parcels; never Carrier custody/acceptance. */
public interface MerchantFacilityWork {
    interface Access extends BusinessPreparations.Access {}
    record Check(UUID parcelId,String result,String note){@Override public String toString(){return "MerchantParcelCheck[redacted]";}}
    record Preparation(UUID id,UUID organizationId,UUID facilityId,UUID shipmentId,String shipmentCode,String state,long version,long cycle,Instant createdAt,Instant updatedAt,List<Check> checks,List<PilotShipments.Parcel> parcels,List<String> allowedActions){public Preparation{checks=List.copyOf(checks);parcels=List.copyOf(parcels);allowedActions=List.copyOf(allowedActions);}@Override public String toString(){return "MerchantParcelPreparation[redacted]";}}
    record WorkItem(UUID shipmentId,String shipmentCode,int parcelCount,Preparation preparation){}
    record WorkPage(List<WorkItem> items,int page,int size,boolean hasNext){public WorkPage{items=List.copyOf(items);}}
    record BatchShipment(UUID shipmentId,String shipmentCode,List<PilotShipments.Parcel> parcels){public BatchShipment{parcels=List.copyOf(parcels);}@Override public String toString(){return "MerchantBatchShipment[redacted]";}}
    record Event(String operation,UUID actorAccountId,String reason,Instant occurredAt){@Override public String toString(){return "MerchantWorkEvent[redacted]";}}
    record Batch(UUID id,UUID organizationId,UUID facilityId,String state,long version,long cycle,Instant createdAt,Instant updatedAt,List<BatchShipment> shipments,List<Check> checks,List<Event> history,List<String> allowedActions){public Batch{shipments=List.copyOf(shipments);checks=List.copyOf(checks);history=List.copyOf(history);allowedActions=List.copyOf(allowedActions);}@Override public String toString(){return "MerchantHandoverPreparation[redacted]";}}
    record BatchPage(List<Batch> items,int page,int size,boolean hasNext){public BatchPage{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    WorkPage list(Access access,UUID facilityId,String preparationState,int page);
    Preparation create(Access access,UUID shipmentId,UUID key);
    Preparation get(Access access,UUID id);
    Preparation check(Access access,UUID id,Check check,long version,UUID key);
    Preparation transition(Access access,UUID id,String action,String reason,long version,UUID key);
    BatchPage batches(Access access,UUID facilityId,String state,int page);
    Batch createBatch(Access access,UUID facilityId,List<UUID> shipmentIds,UUID key);
    Batch batch(Access access,UUID id);
    Batch items(Access access,UUID id,List<UUID> shipmentIds,long version,UUID key);
    Batch checkBatch(Access access,UUID id,Check check,long version,UUID key);
    Batch transitionBatch(Access access,UUID id,String action,String reason,long version,UUID key);
    byte[] document(Access access,UUID id);
    Receipt receipt(Access access,String operation,UUID key);
}
