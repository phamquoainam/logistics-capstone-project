package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.notifications.api.BusinessReporting;
import com.elogistic.backend.orders.api.BusinessReportSources;
import com.elogistic.backend.orders.api.BusinessPreparations;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;

/** Exact integer/calendar geometry over one already-authorized captured cohort. */
public final class BusinessReportingService implements BusinessReporting {
    private static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
    private static BusinessPreparations.Failure invalid(){return new BusinessPreparations.Failure("VALIDATION_FAILED");}
    private static final Set<String> COMMON=Set.of("from","until","interval","service","origin","destination","facilityId","creatorId","assignee","sourceChannel","batchId","status","obligation","handover");
    @Override public Query query(Map<String,String> values,Instant now,String kind,boolean exporting){
        var allowed=new HashSet<>(COMMON);if(kind.equals("FINANCE")&&!exporting)allowed.addAll(Set.of("page","size","search","sort"));
        if(!Set.of("OPERATIONS","FINANCE").contains(kind)||!allowed.containsAll(values.keySet()))throw invalid();
        String interval=values.getOrDefault("interval","DAY");if(!Set.of("DAY","WEEK","MONTH","QUARTER","YEAR").contains(interval))throw invalid();
        var source=new HashMap<>(values);source.remove("interval");LocalDate today=now.atZone(ZONE).toLocalDate(),from,until;
        try{if(values.containsKey("from")!=values.containsKey("until"))throw invalid();from=LocalDate.parse(values.getOrDefault("from",today.minusDays(29).toString()));until=LocalDate.parse(values.getOrDefault("until",today.plusDays(1).toString()));
            if(from.getYear()<2000||until.isAfter(today.plusDays(1))||!until.isAfter(from)||java.time.temporal.ChronoUnit.DAYS.between(from,until)>366)throw invalid();
            source.put("from",from.toString());source.put("until",until.toString());String raw=source.getOrDefault("page","0");if(!raw.matches("0|[1-9][0-9]{0,9}")||!"20".equals(source.getOrDefault("size","20")))throw invalid();int page=Integer.parseInt(raw);return new Query(source,interval,from,until,page);
        }catch(java.time.DateTimeException|NumberFormatException bad){throw invalid();}
    }
    private static final class Totals {
        BigInteger shipments=BigInteger.ZERO,parcels=BigInteger.ZERO,actual=BigInteger.ZERO,chargeable=BigInteger.ZERO,fees=BigInteger.ZERO,unassigned=BigInteger.ZERO;
        void add(BusinessReportSources.Row row){shipments=shipments.add(BigInteger.ONE);parcels=parcels.add(BigInteger.valueOf(row.quote().parcels().size()));fees=fees.add(BigInteger.valueOf(row.quote().totalVnd()));if(row.assigneeAccountId()==null)unassigned=unassigned.add(BigInteger.ONE);
            for(var parcel:row.quote().parcels())actual=actual.add(BigInteger.valueOf(parcel.weightGrams()));for(var line:row.quote().feeLines())chargeable=chargeable.add(BigInteger.valueOf(line.chargeableGrams()));}
        Average average(boolean financial){return financial?new Average(fees.toString(),shipments.toString()):null;}
        String money(boolean financial){return financial?fees.toString():null;}
        Summary summary(boolean financial){return new Summary(shipments.toString(),parcels.toString(),actual.toString(),chargeable.toString(),shipments.toString(),unassigned.toString(),money(financial),money(financial),average(financial));}
        Dimension dimension(String key,String label,boolean financial){return new Dimension(key,label,shipments.toString(),parcels.toString(),actual.toString(),chargeable.toString(),money(financial),money(financial));}
    }
    private Totals totals(List<BusinessReportSources.Row> rows){var total=new Totals();rows.forEach(total::add);return total;}
    private Period period(Query q){return new Period(q.from(),q.until(),q.interval(),q.from().atStartOfDay(ZONE).toInstant(),q.until().atStartOfDay(ZONE).toInstant());}
    private LocalDate start(LocalDate date,String interval){return switch(interval){case "WEEK"->date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));case "MONTH"->date.withDayOfMonth(1);case "QUARTER"->LocalDate.of(date.getYear(),(date.getMonthValue()-1)/3*3+1,1);case "YEAR"->date.withDayOfYear(1);default->date;};}
    private LocalDate next(LocalDate date,String interval){return switch(interval){case "WEEK"->date.plusWeeks(1);case "MONTH"->date.plusMonths(1);case "QUARTER"->date.plusMonths(3);case "YEAR"->date.plusYears(1);default->date.plusDays(1);};}
    private List<Bucket> trend(Query query,BusinessReportSources.Cohort source){
        Map<LocalDate,Totals> groups=new HashMap<>();for(var row:source.rows())groups.computeIfAbsent(start(row.createdAt().atZone(ZONE).toLocalDate(),query.interval()),key->new Totals()).add(row);
        var result=new ArrayList<Bucket>();for(LocalDate calendar=start(query.from(),query.interval());calendar.isBefore(query.until());calendar=next(calendar,query.interval())){
            LocalDate calendarUntil=next(calendar,query.interval()),from=calendar.isBefore(query.from())?query.from():calendar,until=calendarUntil.isAfter(query.until())?query.until():calendarUntil;var t=groups.getOrDefault(calendar,new Totals());
            boolean partial=!from.equals(calendar)||!until.equals(calendarUntil)||source.generatedAt().isBefore(calendarUntil.atStartOfDay(ZONE).toInstant());result.add(new Bucket(from,until,partial,t.shipments.toString(),t.parcels.toString(),t.actual.toString(),t.chargeable.toString(),t.money(source.financial()),t.money(source.financial())));
        }return List.copyOf(result);
    }
    private List<Dimension> dimension(BusinessReportSources.Cohort source,Function<BusinessReportSources.Row,String> key,Function<BusinessReportSources.Row,String> label){
        var groups=new TreeMap<String,Totals>();var labels=new HashMap<String,String>();for(var row:source.rows()){String id=key.apply(row);groups.computeIfAbsent(id,k->new Totals()).add(row);labels.put(id,label.apply(row));if(groups.size()>1000)throw new BusinessPreparations.Failure("REPORT_LIMIT_EXCEEDED");}
        return groups.entrySet().stream().map(e->e.getValue().dimension(e.getKey(),labels.get(e.getKey()),source.financial())).toList();
    }
    private Breakdowns breakdowns(BusinessReportSources.Cohort c){return new Breakdowns(
        dimension(c,r->r.facilityId()==null?"UNBOUND":r.facilityId().toString(),r->r.facilityId()==null?"UNBOUND":r.facilityId().toString()),
        dimension(c,r->r.creatorAccountId().toString(),r->r.creatorAccountId().toString()),
        dimension(c,r->r.sourceChannel()==null?"UNBOUND":"CHANNEL:"+r.sourceChannel(),r->r.sourceChannel()==null?"UNBOUND":r.sourceChannel()),
        dimension(c,r->"ROAD_STANDARD",r->"ROAD_STANDARD"),dimension(c,r->r.quote().route().originCode()+"/"+r.quote().route().destinationCode(),r->r.quote().route().originCode()+"/"+r.quote().route().destinationCode()));}
    private List<Capability> capabilities(){return List.of(new Capability("PACKING","SOURCE_NOT_CONNECTED","PREPARATION_TRANSITION_SOURCE_REQUIRED"),new Capability("PRINT_PRODUCTIVITY","SOURCE_NOT_CONNECTED","PRINT_CONFIRMATION_SOURCE_REQUIRED"),new Capability("CARRIER","SOURCE_NOT_CONNECTED","BUSINESS_CARRIER_LIFECYCLE_REQUIRED"),new Capability("PICKUP","NOT_SUPPORTED","PICKUP_POLICY_AND_AUTHORITY_REQUIRED"),new Capability("QUALITY_SLA","SOURCE_NOT_CONNECTED","DELIVERY_AND_SLA_SOURCE_REQUIRED"),new Capability("COD","NOT_SUPPORTED","BUSINESS_COD_POLICY_REQUIRED"),new Capability("PAYMENT","NOT_SUPPORTED","COLLECTION_AUTHORITY_REQUIRED"),new Capability("CREDIT_DUE","SOURCE_NOT_CONNECTED","COMMERCIAL_CREDIT_SOURCE_REQUIRED"),new Capability("RECONCILIATION","NOT_SUPPORTED","CARRIER_FINANCE_WORKFLOW_REQUIRED"),new Capability("COMMERCIAL_DOCUMENTS","NOT_SUPPORTED","VALID_ISSUING_AUTHORITY_REQUIRED"),new Capability("FINANCIAL_DISPUTES","NOT_SUPPORTED","ORGANIZATION_FINANCE_TICKET_REQUIRED"),new Capability("SCHEDULED_DELIVERY","NOT_SUPPORTED","RECIPIENT_CONSENT_AND_DELIVERY_POLICY_REQUIRED"));}
    @Override public Operations operations(UUID org,Query q,BusinessReportSources.Cohort c){return new Operations("ORGANIZATION_OPERATIONS_1",org,c.generatedAt(),ZONE.getId(),period(q),c.financial()?"AVAILABLE":"FORBIDDEN",totals(c.rows()).summary(c.financial()),trend(q,c),breakdowns(c),c.preparations(),c.activity(),capabilities());}
    private FinanceItem item(BusinessReportSources.Row r){var q=r.quote();var t=totals(List.of(r));return new FinanceItem(r.shipmentId(),r.code(),r.createdAt(),r.facilityId(),r.creatorAccountId(),r.sourceChannel(),r.sourceOrderReference(),"ROAD_STANDARD",q.route().originCode(),q.route().destinationCode(),q.contractId(),q.contractVersion(),q.tariffVersion(),q.id(),Long.toString(q.totalVnd()),Long.toString(q.totalVnd()),"0","UNPAID",t.actual.toString(),t.chargeable.toString(),q.feeLines().stream().map(l->new FeeLine(l.parcelNumber(),l.actualGrams(),l.chargeableGrams(),l.feeVnd())).toList());}
    @Override public Finance finance(UUID org,Query q,BusinessReportSources.Cohort c){if(!c.financial())throw new IllegalStateException("FINANCIAL_SCOPE_REQUIRED");var t=totals(c.rows());long offset=(long)q.page()*20;return new Finance("ORGANIZATION_FINANCE_1",org,c.generatedAt(),ZONE.getId(),period(q),new FinanceSummary(t.shipments.toString(),t.fees.toString(),t.fees.toString(),t.fees.toString(),"0",t.average(true)),trend(q,c),breakdowns(c),t.shipments.toString(),q.page(),20,offset+20<c.rows().size(),c.rows().stream().skip(offset).limit(20).map(this::item).toList(),capabilities());}
    @Override public ExportRows export(UUID org,Query query,BusinessReportSources.Cohort source,String kind,String section){
        var report=operations(org,query,source);List<List<String>> rows=new ArrayList<>();var columns=List.of("group","from","until","key","label","shipments","parcels","actualGrams","chargeableGrams","confirmedFeesVnd","unpaidFeesVnd");
        if(section.equals("trend")){for(var b:report.trend())rows.add(List.of(query.interval(),b.from().toString(),b.until().toString(),b.from().toString(),b.partial()?"PARTIAL":"COMPLETE",b.shipments(),b.parcels(),b.actualGrams(),b.chargeableGrams(),Objects.toString(b.confirmedFeesVnd(),""),Objects.toString(b.unpaidFeesVnd(),"")));}
        else{var data=switch(section){case "facility"->report.breakdowns().facility();case "creator"->report.breakdowns().creator();case "channel"->report.breakdowns().channel();case "service"->report.breakdowns().service();case "route"->report.breakdowns().route();default->throw invalid();};for(var d:data)rows.add(List.of(section,query.from().toString(),query.until().toString(),d.key(),d.label(),d.shipments(),d.parcels(),d.actualGrams(),d.chargeableGrams(),Objects.toString(d.confirmedFeesVnd(),""),Objects.toString(d.unpaidFeesVnd(),"")));}
        if(rows.size()>1000)throw new BusinessPreparations.Failure("REPORT_LIMIT_EXCEEDED");return new ExportRows(columns,rows);
    }
}
