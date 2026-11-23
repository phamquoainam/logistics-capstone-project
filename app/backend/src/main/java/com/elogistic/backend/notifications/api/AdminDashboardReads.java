package com.elogistic.backend.notifications.api;

import com.elogistic.backend.identity.api.admin.AdminReadContext;
import com.elogistic.backend.orders.api.AdminShipmentFacts;
import com.elogistic.backend.finance.api.AdminObligationFacts;
import java.time.*;
import java.util.*;

public interface AdminDashboardReads {
    record Query(LocalDate from,LocalDate to,String grain,String shipmentStatus,String regionId) {}
    record Drilldown(String path,Map<String,String> filters,String metric) { public Drilldown{filters=Map.copyOf(filters);} }
    record Metric(String key,String value,String unit,Drilldown drilldown) {}
    record Point(LocalDate from,LocalDate to,String metric,String value) {}
    record Section(String key,String availability,String reasonCode,List<Metric> metrics,List<Point> series) { public Section{metrics=List.copyOf(metrics);series=List.copyOf(series);} }
    record Dashboard(Query query,Instant generatedAt,String snapshotId,List<Section> sections) { public Dashboard{sections=List.copyOf(sections);} }
    record Page(List<AdminShipmentFacts.Row> items,int page,int size,boolean hasNext,String snapshotId) { public Page{items=List.copyOf(items);} }
    record ApplicationItem(UUID id,String state,Instant submittedAt,Instant updatedAt){}
    record ApplicationPage(List<ApplicationItem> items,int page,int size,boolean hasNext,String snapshotId){public ApplicationPage{items=List.copyOf(items);}}
    record NetworkItem(UUID id,String code,String kind,String status){}
    record NetworkPage(List<NetworkItem> items,int page,int size,boolean hasNext,String snapshotId){public NetworkPage{items=List.copyOf(items);}}
    record Selector(Query query,Instant cutoff) {}
    Query query(Map<String,String> parameters,Instant now,boolean page);
    Dashboard dashboard(AdminReadContext actor,Query query,Instant cutoff,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> obligations,Set<String> unavailable);
    Selector selector(AdminReadContext actor,Query query,String snapshotId,Instant now);
    Page shipments(AdminReadContext actor,Query query,String snapshotId,Instant now,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> obligations,String metric,int page,int size);
}
