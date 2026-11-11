package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.notifications.api.BusinessDashboard;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** No Merchant shipping source exists yet. Missing facts never become observed zero. */
public final class BusinessDashboardService implements BusinessDashboard {
    private final Clock clock; private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    public BusinessDashboardService(Clock clock){this.clock=clock;}
    @Override public Report read(Access scope,Map<String,String> query){
        scope.requireCurrent();var org=scope.context();var permissions=scope.permissions();
        if(!permissions.contains("dashboard.read"))throw new Failure("BUSINESS_CONTEXT_UNAVAILABLE");
        Period period;
        try{
            if(!Set.of("from","to","interval").containsAll(query.keySet()))throw new IllegalArgumentException();
            var today=LocalDate.now(clock.withZone(ZONE));
            LocalDate from=query.containsKey("from")?date(query.get("from")):today.minusDays(29),to=query.containsKey("to")?date(query.get("to")):today;
            String interval=query.getOrDefault("interval","DAY");
            if(!Set.of("DAY","WEEK","MONTH").contains(interval)||to.isBefore(from)||to.isAfter(today)||ChronoUnit.DAYS.between(from,to)>365)throw new IllegalArgumentException();
            period=new Period(from,to,from.atStartOfDay(ZONE).toInstant(),to.plusDays(1).atStartOfDay(ZONE).toInstant(),interval);
        }catch(RuntimeException invalid){throw new Failure("VALIDATION_FAILED");}
        var metrics=new ArrayList<Metric>();
        for(String id:List.of("BOOKED_SHIPMENTS","AWAITING_HANDOVER","ACCEPTED_SHIPMENTS","IN_TRANSIT","DELIVERED","DELIVERY_FAILED","RETURNED","ACTION_REQUIRED"))metrics.add(metric(id,"SHIPMENT"));
        metrics.add(metric("PARCELS","PARCEL"));metrics.add(metric("ACTUAL_WEIGHT_GRAMS","GRAM"));
        for(String id:List.of("DELIVERY_SUCCESS_RATE","RETURN_RATE","PICKUP_SUCCESS_RATE"))metrics.add(metric(id,"PERCENT"));
        metrics.add(metric("BOOKING_TO_ACCEPTANCE_MS","MILLISECOND"));
        if(permissions.contains("dashboard.finance.read")){
            for(String id:List.of("CONFIRMED_FEES","OUTSTANDING_FEES","COD_EXPECTED","COD_COLLECTED","COD_RECONCILED","COD_REMITTED"))metrics.add(metric(id,"VND"));
            metrics.add(metric("COST_PER_SHIPMENT","VND_PER_SHIPMENT"));
        }
        var charts=new ArrayList<Chart>();
        for(String id:List.of("VOLUME_TREND","STATUS_DISTRIBUTION","SERVICE_BREAKDOWN","ROUTE_BREAKDOWN","FACILITY_BREAKDOWN","CHANNEL_BREAKDOWN"))charts.add(chart(id));
        if(permissions.contains("dashboard.finance.read"))charts.add(chart("FEE_TREND"));
        if(permissions.contains("dashboard.staff.read"))charts.add(chart("STAFF_BREAKDOWN"));
        var capabilities=new ArrayList<Capability>();capabilities.add(new Capability("SHIPPING","SOURCE_NOT_CONNECTED","MERCHANT_SHIPPING_SOURCE_REQUIRED"));
        if(permissions.contains("dashboard.finance.read"))capabilities.add(new Capability("FINANCE","SOURCE_NOT_CONNECTED","MERCHANT_FINANCE_SOURCE_REQUIRED"));
        var actions=List.of("CREATE","IMPORT","LABELS","MANIFEST","PICKUP","ACTION_CENTER").stream().map(id->new Capability(id,"NOT_SUPPORTED","MERCHANT_WORKFLOW_REQUIRED")).toList();
        var result=new Report("BUSINESS_DASHBOARD_V1",new Context(org.organizationId(),org.displayName(),org.membershipId(),org.authorityVersion()),clock.instant(),ZONE.getId(),period,List.copyOf(capabilities),List.copyOf(metrics),List.copyOf(charts),new SourceList("SOURCE_NOT_CONNECTED",List.of()),new SourceList("SOURCE_NOT_CONNECTED",List.of()),actions);
        scope.requireCurrent();return result;
    }
    private static LocalDate date(String value){var d=LocalDate.parse(value);if(!d.toString().equals(value)||d.getYear()<2000||d.getYear()>2100)throw new IllegalArgumentException();return d;}
    private static Metric metric(String id,String unit){return new Metric(id,unit,"SOURCE_NOT_CONNECTED",null,List.of());}
    private static Chart chart(String id){return new Chart(id,"SOURCE_NOT_CONNECTED",List.of());}
}
