package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.notifications.api.*;
import com.elogistic.backend.notifications.api.PersonalAnalytics.*;
import com.elogistic.backend.orders.api.AnalyticsShipments;
import com.elogistic.backend.finance.api.AnalyticsObligations;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.*;
import static com.elogistic.backend.notifications.api.AnalyticsFailure.Code.*;

/** One captured self cohort drives every aggregate and its contributing records. */
public final class PersonalAnalyticsService implements PersonalAnalytics {
    private final AnalyticsShipments shipments;
    private final AnalyticsObligations obligations;
    private final Clock clock;
    private final boolean enabled;
    public PersonalAnalyticsService(AnalyticsShipments shipments,AnalyticsObligations obligations,Clock clock,boolean enabled){this.shipments=shipments;this.obligations=obligations;this.clock=clock;this.enabled=enabled;}
    private record Row(AnalyticsShipments.Fact fact,long unpaid,boolean accepted){}
    @Override public Report read(Access access,Map<String,String> parameters){
        access.requireCurrent();if(!enabled)throw new AnalyticsFailure(PILOT_DISABLED);
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        var q=AnalyticsQuery.parse(parameters,now);var start=q.start();var end=q.end(now);
        Instant priorStart=q.compare()?start.atZone(AnalyticsQuery.ZONE).minusDays(q.days()).toInstant():null;
        Instant priorEnd=q.compare()?end.atZone(AnalyticsQuery.ZONE).minusDays(q.days()).toInstant():null;
        var current=cohort(access,q,start,end,now,false);
        var previous=q.compare()?cohort(access,q,priorStart,priorEnd,now,true):List.<Row>of();
        List<Metric> metrics=new ArrayList<>();
        String[] ids={"BOOKED_SHIPMENTS","AWAITING_HANDOVER","ACCEPTED_SHIPMENTS","CONFIRMED_FEES","UNPAID_OBLIGATIONS","CREATED_PARCELS"};
        for(String id:ids){
            BigInteger value=value(current,id),prior=q.compare()?value(previous,id):null;
            String percent=prior==null||prior.signum()==0?null:new BigDecimal(value.subtract(prior).multiply(BigInteger.valueOf(100))).divide(new BigDecimal(prior),1,RoundingMode.HALF_UP).toPlainString();
            List<Point> points=buckets(q).stream().map(d->new Point(d,value(current.stream().filter(r->group(q,day(r)).equals(d)).toList(),id).toString())).toList();
            metrics.add(new Metric(id,id.endsWith("FEES")||id.equals("UNPAID_OBLIGATIONS")?"VND":id.equals("CREATED_PARCELS")?"PARCEL":"SHIPMENT",value.toString(),prior==null?null:prior.toString(),percent,points));
        }
        var status=List.of(new Status("WAITING_FOR_HANDOVER",Long.toString(current.stream().filter(r->!r.accepted()).count())),new Status("AT_STATION",Long.toString(current.stream().filter(Row::accepted).count())));
        var volume=buckets(q).stream().map(d->{var rows=current.stream().filter(r->group(q,day(r)).equals(d)).toList();return new Volume(d,value(rows,"BOOKED_SHIPMENTS").toString(),value(rows,"CONFIRMED_FEES").toString(),value(rows,"UNPAID_OBLIGATIONS").toString());}).toList();
        List<Service> services=current.isEmpty()?List.of():List.of(new Service("ROAD_STANDARD",Integer.toString(current.size()),value(current,"CONFIRMED_FEES").toString()));
        Map<String,List<Row>> routes=new TreeMap<>();for(var r:current)routes.computeIfAbsent(r.fact().origin()+"/"+r.fact().destination(),k->new ArrayList<>()).add(r);
        var routeViews=routes.values().stream().map(rows->{var f=rows.getFirst().fact();return new Route(f.datasetVersion(),f.origin(),f.destination(),Integer.toString(rows.size()),value(rows,"CONFIRMED_FEES").toString());}).toList();
        var weights=List.of("UP_TO_500G","UP_TO_5KG","OVER_5KG").stream().map(key->new Weight(key,Long.toString(current.stream().flatMap(r->r.fact().weights().stream()).filter(w->AnalyticsQuery.weightKey(w).equals(key)).count()))).toList();
        var parcelView=new Parcels(value(current,"CREATED_PARCELS").toString(),Long.toString(current.stream().filter(r->r.fact().weights().size()>1).count()),weights);
        Comparator<Row> ordering=switch(q.sort()){
            case "OLDEST"->Comparator.comparing(r->r.fact().bookedAt());
            case "FEE_DESC"->Comparator.<Row>comparingLong(r->r.fact().confirmedVnd()).reversed();
            case "FEE_ASC"->Comparator.comparingLong(r->r.fact().confirmedVnd());
            default->Comparator.<Row,Instant>comparing(r->r.fact().bookedAt()).reversed();
        };
        var sorted=current.stream().sorted(ordering.thenComparing(r->r.fact().id().toString())).toList();
        int offset=Math.min(q.page()*20,sorted.size()),last=Math.min(offset+20,sorted.size());
        var items=sorted.subList(offset,last).stream().map(r->{var f=r.fact();return new Shipment(f.id(),f.code(),f.bookedAt(),r.accepted()?"AT_STATION":"WAITING_FOR_HANDOVER","ROAD_STANDARD",f.origin(),f.destination(),Integer.toString(f.weights().size()),Long.toString(f.weights().stream().mapToLong(Long::longValue).sum()),Long.toString(f.confirmedVnd()),Long.toString(r.unpaid()));}).toList();
        access.requireCurrent();
        return new Report("PERSONAL_ANALYTICS_1","PERSONAL",AnalyticsQuery.ZONE.getId(),now,new Period(start,end,priorStart,priorEnd),List.copyOf(metrics),status,volume,services,routeViews,parcelView,new Page(Integer.toString(current.size()),q.page(),20,last<sorted.size(),items));
    }
    private List<Row> cohort(Access access,AnalyticsQuery q,Instant from,Instant until,Instant now,boolean previous){
        var facts=shipments.read(access,from,until);
        if(facts.size()>10000)throw new AnalyticsFailure(ANALYTICS_RANGE_TOO_LARGE);
        Set<UUID> seen=new HashSet<>();
        for(var f:facts)if(!seen.add(f.id())||f.bookedAt().isBefore(from)||!f.bookedAt().isBefore(until)||f.confirmedVnd()<1||f.weights().isEmpty()||f.weights().stream().anyMatch(w->w<1||w>20000)||!"ARGO-SIM-COVERAGE-1".equals(f.datasetVersion())||!Set.of("SIM-A1/SIM-A2","SIM-A2/SIM-A1","SIM-A1/SIM-B1","SIM-B1/SIM-A1").contains(f.origin()+"/"+f.destination()))throw new AnalyticsFailure(ANALYTICS_UNAVAILABLE);
        var selected=facts.stream().filter(f->{boolean accepted=f.acceptedAt()!=null&&!f.acceptedAt().isAfter(now);return (q.status().isEmpty()||q.status().equals(accepted?"AT_STATION":"WAITING_FOR_HANDOVER"))&&(q.origin().isEmpty()||q.origin().equals(f.origin())&&q.destination().equals(f.destination()))&&(!q.multi()||f.weights().size()>1)&&f.weights().stream().anyMatch(q::weightMatches)&&q.selectedDay(f.bookedAt().atZone(AnalyticsQuery.ZONE).toLocalDate(),previous)&&f.code().toLowerCase(Locale.ROOT).contains(q.search());}).toList();
        var money=obligations.read(access,selected.stream().map(AnalyticsShipments.Fact::id).toList());Map<UUID,AnalyticsObligations.Fact> byId=new HashMap<>();
        for(var o:money)if(byId.put(o.shipmentId(),o)!=null)throw new AnalyticsFailure(ANALYTICS_UNAVAILABLE);
        if(byId.size()!=selected.size())throw new AnalyticsFailure(ANALYTICS_UNAVAILABLE);
        return selected.stream().map(f->{var o=byId.get(f.id());if(o==null||!o.quoteId().equals(f.quoteId())||o.amountVnd()!=f.confirmedVnd()||!"UNPAID".equals(o.state())||!"VND".equals(o.currency()))throw new AnalyticsFailure(ANALYTICS_UNAVAILABLE);return new Row(f,o.amountVnd(),f.acceptedAt()!=null&&!f.acceptedAt().isAfter(now));}).toList();
    }
    private static LocalDate day(Row row){return row.fact().bookedAt().atZone(AnalyticsQuery.ZONE).toLocalDate();}
    private static LocalDate group(AnalyticsQuery q,LocalDate date){var key=q.group(date);return key.isBefore(q.from())?q.from():key;}
    private static List<LocalDate> buckets(AnalyticsQuery q){var days=new TreeSet<LocalDate>();for(LocalDate d=q.from();d.isBefore(q.until());d=d.plusDays(1))days.add(group(q,d));return List.copyOf(days);}
    private static BigInteger value(List<Row> rows,String metric){return rows.stream().map(r->switch(metric){case "AWAITING_HANDOVER"->r.accepted()?BigInteger.ZERO:BigInteger.ONE;case "ACCEPTED_SHIPMENTS"->r.accepted()?BigInteger.ONE:BigInteger.ZERO;case "CONFIRMED_FEES"->BigInteger.valueOf(r.fact().confirmedVnd());case "UNPAID_OBLIGATIONS"->BigInteger.valueOf(r.unpaid());case "CREATED_PARCELS"->BigInteger.valueOf(r.fact().weights().size());default->BigInteger.ONE;}).reduce(BigInteger.ZERO,BigInteger::add);}
}
