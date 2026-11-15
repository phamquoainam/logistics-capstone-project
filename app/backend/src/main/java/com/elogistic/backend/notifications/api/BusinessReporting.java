package com.elogistic.backend.notifications.api;

import com.elogistic.backend.orders.api.BusinessReportSources;
import java.time.*;
import java.util.*;

/** Bounded reporting projections, no financial or Carrier mutation authority. */
public interface BusinessReporting {
    record Query(Map<String,String> source,String interval,LocalDate from,LocalDate until,int page){public Query{source=Map.copyOf(source);}}
    record Period(LocalDate from,LocalDate until,String interval,Instant startInclusive,Instant endExclusive){}
    record Average(String totalVnd,String shipmentCount){}
    record Summary(String shipments,String parcels,String actualGrams,String chargeableGrams,String waitingForHandover,String unassigned,String confirmedFeesVnd,String unpaidFeesVnd,Average averageFee){}
    record Bucket(LocalDate from,LocalDate until,boolean partial,String shipments,String parcels,String actualGrams,String chargeableGrams,String confirmedFeesVnd,String unpaidFeesVnd){}
    record Dimension(String key,String label,String shipments,String parcels,String actualGrams,String chargeableGrams,String confirmedFeesVnd,String unpaidFeesVnd){}
    record Breakdowns(List<Dimension> facility,List<Dimension> creator,List<Dimension> channel,List<Dimension> service,List<Dimension> route){}
    record Capability(String id,String state,String reasonCode){}
    record Operations(String schemaVersion,UUID organizationId,Instant generatedAt,String timeZone,Period period,String financialState,Summary summary,List<Bucket> trend,Breakdowns breakdowns,BusinessReportSources.Preparation preparations,BusinessReportSources.Activity activity,List<Capability> capabilities){}
    record FinanceSummary(String shipments,String confirmedFeesVnd,String unpaidFeesVnd,String quoteFeesVnd,String differenceVnd,Average averageFee){}
    record FeeLine(int parcelNumber,long actualGrams,long chargeableGrams,long feeVnd){}
    record FinanceItem(UUID shipmentId,String code,Instant createdAt,UUID facilityId,UUID creatorAccountId,String sourceChannel,String sourceOrderReference,String service,String origin,String destination,UUID contractId,long contractVersion,String tariffVersion,UUID quoteId,String quoteVnd,String confirmedVnd,String differenceVnd,String paymentState,String actualGrams,String chargeableGrams,List<FeeLine> feeLines){}
    record Finance(String schemaVersion,UUID organizationId,Instant generatedAt,String timeZone,Period period,FinanceSummary summary,List<Bucket> trend,Breakdowns breakdowns,String total,int page,int size,boolean hasNext,List<FinanceItem> items,List<Capability> capabilities){}
    record Export(UUID exportId,String organizationId,Instant generatedAt,String timeZone,String kind,String section,int rowCount,List<String> columns,List<List<String>> rows,String schemaVersion){}
    record ExportRows(List<String> columns,List<List<String>> rows){public ExportRows{columns=List.copyOf(columns);rows=rows.stream().map(List::copyOf).toList();}}
    Query query(Map<String,String> parameters,Instant now,String kind,boolean exporting);
    Operations operations(UUID org,Query query,BusinessReportSources.Cohort source);
    Finance finance(UUID org,Query query,BusinessReportSources.Cohort source);
    ExportRows export(UUID org,Query query,BusinessReportSources.Cohort source,String kind,String section);
}
