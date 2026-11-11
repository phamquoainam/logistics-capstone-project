package com.elogistic.backend.notifications.api;

import java.time.*;
import java.util.*;

public interface BusinessDashboard {
    interface Access { void requireCurrent(); Context context(); Set<String> permissions(); }
    final class Failure extends RuntimeException { public Failure(String code){super(code);} }
    record Context(UUID organizationId,String displayName,UUID membershipId,long authorityVersion){}
    record Period(LocalDate from,LocalDate to,Instant startInclusive,Instant endExclusive,String interval){}
    record Capability(String id,String state,String reasonCode){}
    record Metric(String id,String unit,String state,String value,List<Object> points){}
    record Chart(String id,String state,List<Object> series){}
    record SourceList(String state,List<Object> items){}
    record Report(String schemaVersion,Context context,Instant generatedAt,String timeZone,Period period,List<Capability> capabilities,List<Metric> metrics,List<Chart> charts,SourceList recentShipments,SourceList attention,List<Capability> quickActions){}
    Report read(Access access,Map<String,String> parameters);
}
