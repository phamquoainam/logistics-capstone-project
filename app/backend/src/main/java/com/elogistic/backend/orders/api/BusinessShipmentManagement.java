package com.elogistic.backend.orders.api;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

/** Organization read projection and internal responsibility; never Carrier Tracking. */
public interface BusinessShipmentManagement {
    interface Access extends BusinessPreparations.Access { boolean eligibleAccount(UUID account); }
    record FeeBinding(UUID shipmentId,UUID quoteId,UUID facilityId,long amountVnd){}
    record Source(String sourceChannel,String sourceOrderReference,UUID facilityId,List<UUID> importJobIds){public Source{importJobIds=List.copyOf(importJobIds);}}
    record Assignment(UUID shipmentId,UUID organizationId,UUID assigneeAccountId,long version,Instant updatedAt){}
    record Summary(String bookedShipments,String waitingForHandover,String unassigned,String confirmedFeesVnd,String unpaidFeesVnd){}
    record Item(BusinessShipments.Shipment shipment,Source source,Assignment assignment,List<String> allowedActions){public Item{allowedActions=List.copyOf(allowedActions);}}
    record Page(Instant generatedAt,Summary summary,String total,int page,boolean hasNext,List<Item> items){public Page{items=List.copyOf(items);}}
    record History(UUID id,String operation,UUID actorAccountId,UUID priorAssigneeAccountId,UUID assigneeAccountId,long priorVersion,long newVersion,Instant occurredAt){}
    record HistoryPage(List<History> items,int page,boolean hasNext){public HistoryPage{items=List.copyOf(items);}}
    record Receipt(UUID targetId,long version,String outcome){}
    record Export(UUID exportId,UUID organizationId,Instant generatedAt,boolean piiIncluded,int rowCount,List<String> columns,List<List<String>> rows){public Export{columns=List.copyOf(columns);rows=rows.stream().map(List::copyOf).toList();}}
    Page read(Access access,Map<String,String> parameters,Consumer<List<FeeBinding>> validateFees);
    Export export(Access access,Map<String,String> parameters,Consumer<List<FeeBinding>> validateFees);
    Assignment assignment(Access access,UUID shipment);
    Assignment assign(Access access,UUID shipment,UUID assignee,String reason,long expected,UUID key);
    Receipt receipt(Access access,UUID key);
    HistoryPage history(Access access,UUID shipment,int page);
}
