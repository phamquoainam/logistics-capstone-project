package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Current scoped non-contact sources; never a Carrier or ledger command. */
public interface BusinessReportSources {
    record Route(String originCode,String destinationCode){}
    record Parcel(long weightGrams){}
    record Line(int parcelNumber,long actualGrams,long chargeableGrams,long feeVnd){}
    record Quote(UUID id,UUID contractId,long contractVersion,String tariffVersion,Route route,List<Parcel> parcels,List<Line> feeLines,long totalVnd){public Quote{parcels=List.copyOf(parcels);feeLines=List.copyOf(feeLines);}}
    record Row(UUID shipmentId,String code,Instant createdAt,UUID facilityId,UUID creatorAccountId,
               String sourceChannel,String sourceOrderReference,UUID assigneeAccountId,Quote quote){}
    record Preparation(String state,String reasonCode,String total,String draft,String booked,String archived){}
    record Actor(UUID accountId,String created,String booked){}
    record Activity(String state,String reasonCode,String created,String booked,List<Actor> byActor){public Activity{byActor=List.copyOf(byActor);}}
    record Cohort(Instant generatedAt,boolean financial,List<Row> rows,Preparation preparations,Activity activity){public Cohort{rows=List.copyOf(rows);}}
    record ExportHistory(UUID exportId,String kind,String section,Map<String,String> query,int rowCount,Instant occurredAt){public ExportHistory{query=Map.copyOf(query);}}
    record HistoryPage(List<ExportHistory> items,int page,boolean hasNext){public HistoryPage{items=List.copyOf(items);}}
    Cohort capture(BusinessShipmentManagement.Access access,Map<String,String> filters,String kind,boolean export,Consumer<List<BusinessShipmentManagement.FeeBinding>> validate);
    UUID auditExport(BusinessShipmentManagement.Access access,String kind,String section,Map<String,String> query,List<UUID> shipments,int rowCount,Instant at);
    HistoryPage history(BusinessShipmentManagement.Access access,int page);
}
