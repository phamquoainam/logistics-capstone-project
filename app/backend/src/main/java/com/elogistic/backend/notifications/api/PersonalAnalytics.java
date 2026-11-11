package com.elogistic.backend.notifications.api;

import com.elogistic.backend.orders.api.ShippingDrafts;
import com.elogistic.backend.finance.api.PilotObligations;
import java.time.*;
import java.util.*;

public interface PersonalAnalytics {
    interface Access extends ShippingDrafts.Access,PilotObligations.Access {}
    record Period(Instant startInclusive,Instant endExclusive,Instant previousStartInclusive,Instant previousEndExclusive){}
    record Point(LocalDate date,String value){}
    record Metric(String id,String unit,String value,String previousValue,String changePercent,List<Point> points){}
    record Status(String key,String shipments){}
    record Volume(LocalDate date,String shipments,String confirmedFeeVnd,String unpaidVnd){}
    record Service(String code,String shipments,String confirmedFeeVnd){}
    record Route(String datasetVersion,String origin,String destination,String shipments,String confirmedFeeVnd){}
    record Weight(String key,String parcels){}
    record Parcels(String count,String multiParcelShipments,List<Weight> weight){}
    record Shipment(UUID id,String code,Instant bookedAt,String status,String service,String origin,String destination,String parcels,String weightGrams,String confirmedFeeVnd,String unpaidVnd){}
    record Page(String total,int page,int size,boolean hasNext,List<Shipment> items){}
    record Report(String schemaVersion,String context,String timeZone,Instant generatedAt,Period period,List<Metric> metrics,List<Status> status,List<Volume> volume,List<Service> services,List<Route> routes,Parcels parcels,Page shipments){}
    record AcceptanceTiming(String sampleShipments,String totalDurationMillis,String minimumMillis,String maximumMillis){}
    record ParcelWeights(String actualGrams,String chargeableGrams){}
    record ShippingReport(String schemaVersion,Report report,AcceptanceTiming acceptanceTiming,ParcelWeights parcelWeights){}
    Report read(Access access,Map<String,String> parameters);
    ShippingReport readShippingReport(Access access,Map<String,String> parameters);
}
