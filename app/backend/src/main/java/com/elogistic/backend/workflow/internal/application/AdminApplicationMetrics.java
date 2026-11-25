package com.elogistic.backend.workflow.internal.application;

import com.elogistic.backend.identity.api.admin.AdminFailure;
import com.elogistic.backend.merchant.api.AdminApplicationFacts.*;
import java.math.BigInteger;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/** Code-owned MET-P0-02 definitions. No source query, authority issuance or financial inference. */
final class AdminApplicationMetrics {
    static final String VERSION="ARGO_ADMIN_METRICS_2";
    private static final List<String> STATES=List.of("SUBMITTED","UNDER_REVIEW","NEEDS_INFORMATION","APPROVED","REJECTED","WITHDRAWN");
    record Measured(String value,String numerator,String denominator,String unit,String sampleCount,Instant lastSourceEventAt){}
    record Point(LocalDate from,LocalDate to,String metric,String value){}
    private AdminApplicationMetrics(){}
    private static AdminFailure invalid(){return new AdminFailure("SERVICE_UNAVAILABLE");}
    private static void validate(Capture source){
        if(source==null||source.rows().size()>100000||source.rows().stream().map(Row::id).distinct().count()!=source.rows().size())throw invalid();
        for(var row:source.rows())if(row.id()==null||!STATES.contains(row.state())||row.submittedAt()==null||row.updatedAt()==null||row.lastSourceEventAt()==null||row.lastSourceEventAt().isBefore(row.submittedAt())||row.terminalAt()!=null&&row.terminalAt().isBefore(row.submittedAt())||Set.of("APPROVED","REJECTED").contains(row.state())!=(row.terminalAt()!=null))throw invalid();
        if(!source.availability().equals("AVAILABLE")&&(!Set.of("SOURCE_NOT_IMPLEMENTED","POLICY_NOT_CONFIGURED").contains(source.availability())||!source.rows().isEmpty()))throw invalid();
    }
    private static Instant latest(List<Row> rows){return rows.stream().map(Row::lastSourceEventAt).max(Comparator.naturalOrder()).orElse(null);}
    private static BigInteger duration(Row row){var duration=Duration.between(row.submittedAt(),row.terminalAt());return BigInteger.valueOf(duration.getSeconds()).multiply(BigInteger.valueOf(1000)).add(BigInteger.valueOf(duration.getNano()/1000000));}
    static Map<String,Measured> measure(Capture source){
        validate(source);if(!source.availability().equals("AVAILABLE"))return Map.of();
        Map<String,Measured> result=new LinkedHashMap<>();var rows=source.rows();
        result.put("APPLICATIONS_SUBMITTED",new Measured(Integer.toString(rows.size()),Integer.toString(rows.size()),null,"APPLICATION",null,latest(rows)));
        for(String state:STATES.subList(1,STATES.size())){var selected=rows.stream().filter(r->r.state().equals(state)).toList();String count=Integer.toString(selected.size());result.put("APPLICATIONS_"+state,new Measured(count,count,null,"APPLICATION",null,latest(selected)));}
        var samples=rows.stream().filter(r->r.terminalAt()!=null).toList();String sampleCount=Integer.toString(samples.size());BigInteger total=samples.stream().map(AdminApplicationMetrics::duration).reduce(BigInteger.ZERO,BigInteger::add);Instant last=samples.stream().map(Row::terminalAt).max(Comparator.naturalOrder()).orElse(null);
        result.put("APPLICATION_PROCESSING_TOTAL_MS",new Measured(total.toString(),total.toString(),null,"MILLISECOND",sampleCount,last));
        result.put("APPLICATION_PROCESSING_AVG_MS",new Measured(samples.isEmpty()?null:total.divide(BigInteger.valueOf(samples.size())).toString(),total.toString(),sampleCount,"MILLISECOND",sampleCount,last));
        if(!samples.isEmpty())for(String extreme:List.of("MIN","MAX")){var values=samples.stream().map(AdminApplicationMetrics::duration);String value=(extreme.equals("MIN")?values.min(Comparator.naturalOrder()):values.max(Comparator.naturalOrder())).orElseThrow().toString();result.put("APPLICATION_PROCESSING_"+extreme+"_MS",new Measured(value,value,null,"MILLISECOND",sampleCount,last));}
        return Collections.unmodifiableMap(result);
    }
    static List<Point> series(Capture source,LocalDate from,LocalDate to,String grain){
        validate(source);if(from==null||to==null||from.isAfter(to)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>=366||!Set.of("DAY","WEEK","MONTH").contains(Objects.toString(grain,"")))throw new AdminFailure("VALIDATION_FAILED");
        if(!source.availability().equals("AVAILABLE"))return List.of();
        ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");var points=new ArrayList<Point>();LocalDate end=to.plusDays(1),cursor=from;
        while(cursor.isBefore(end)){
            LocalDate next=switch(grain){case "WEEK"->cursor.with(TemporalAdjusters.next(DayOfWeek.MONDAY));case "MONTH"->cursor.withDayOfMonth(1).plusMonths(1);default->cursor.plusDays(1);};if(next.isAfter(end))next=end;
            Instant start=cursor.atStartOfDay(zone).toInstant(),until=next.atStartOfDay(zone).toInstant();var rows=source.rows().stream().filter(r->!r.submittedAt().isBefore(start)&&r.submittedAt().isBefore(until)).toList();
            points.add(new Point(cursor,next,"APPLICATIONS_SUBMITTED",Integer.toString(rows.size())));
            for(String state:STATES.subList(1,STATES.size()))points.add(new Point(cursor,next,"APPLICATIONS_"+state,Long.toString(rows.stream().filter(r->r.state().equals(state)).count())));
            cursor=next;
        }
        return List.copyOf(points);
    }
}
