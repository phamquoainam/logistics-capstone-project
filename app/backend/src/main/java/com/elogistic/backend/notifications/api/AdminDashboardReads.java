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
    record QueryV2(LocalDate from,LocalDate to,String grain,String shipmentStatus,String regionId,String applicationState,UUID policyId){public Query legacy(){return new Query(from,to,grain,shipmentStatus,regionId);}}
    record MetricV2(String key,String value,String unit,Drilldown drilldown){}
    record SectionV2(String key,String availability,String reasonCode,List<MetricV2> metrics,List<Point> series){public SectionV2{metrics=List.copyOf(metrics);series=List.copyOf(series);}}
    record MetricMetadataV2(String metricKey,String definitionVersion,String cohortKind,String timeZone,String numerator,String denominator,String unit,String sampleCount,Instant lastSourceEventAt,Instant sourceCheckedAt,String freshness,String availabilityReason){}
    record ResourceScopeV2(String kind,UUID id,List<String> capabilities,String version){public ResourceScopeV2{capabilities=List.copyOf(capabilities);}}
    record ContextV2(String schemaVersion,UUID accountId,String displayName,List<String> permissions,String environment,String timeZone,boolean regionFilterAvailable,String resourceScopeVersion,List<ResourceScopeV2> resourceScopes){public ContextV2{permissions=List.copyOf(permissions);resourceScopes=List.copyOf(resourceScopes);}}
    record DashboardV2(String schemaVersion,QueryV2 query,Instant generatedAt,String snapshotId,List<SectionV2> sections,String resourceScopeVersion,List<MetricMetadataV2> metricMetadata){public DashboardV2{sections=List.copyOf(sections);metricMetadata=List.copyOf(metricMetadata);}}
    record PageV2<T>(String schemaVersion,List<T> items,int page,int size,boolean hasNext,String snapshotId,String resourceScopeVersion,List<MetricMetadataV2> metricMetadata){public PageV2{items=List.copyOf(items);metricMetadata=List.copyOf(metricMetadata);}}
    record SourceStatusV2(String availability,List<Metric> metrics){public SourceStatusV2{metrics=List.copyOf(metrics);}}
    record PlatformV2(String schemaVersion,Instant checkedAt,String apiAvailability,SourceStatusV2 queues,SourceStatusV2 scheduledJobs,SourceStatusV2 integrationFailures){}
    record SelectorV2(QueryV2 query,Instant cutoff,String resourceScopeVersion,String fingerprint){}
    default QueryV2 queryV2(Map<String,String> parameters,Instant now,boolean page){throw new com.elogistic.backend.identity.api.admin.AdminFailure("SERVICE_UNAVAILABLE");}
    default String snapshotV2(AdminReadContext actor,QueryV2 query,Instant cutoff,String resourceScopeVersion,String fingerprint){throw new com.elogistic.backend.identity.api.admin.AdminFailure("SERVICE_UNAVAILABLE");}
    default SelectorV2 selectorV2(AdminReadContext actor,QueryV2 query,String snapshotId,Instant now){throw new com.elogistic.backend.identity.api.admin.AdminFailure("SNAPSHOT_EXPIRED");}
    Query query(Map<String,String> parameters,Instant now,boolean page);
    Dashboard dashboard(AdminReadContext actor,Query query,Instant cutoff,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> obligations,Set<String> unavailable);
    Selector selector(AdminReadContext actor,Query query,String snapshotId,Instant now);
    Page shipments(AdminReadContext actor,Query query,String snapshotId,Instant now,List<AdminShipmentFacts.Row> rows,List<AdminObligationFacts.Row> obligations,String metric,int page,int size);
}
