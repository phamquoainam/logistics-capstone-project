package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.notifications.api.*;
import com.elogistic.backend.orders.api.AnalyticsShipments;
import com.elogistic.backend.finance.api.AnalyticsObligations;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PersonalAnalyticsTests {
    static final Instant NOW=Instant.parse("2026-11-03T05:00:00Z");
    static final Map<String,String> PERIOD=Map.of("from","2026-11-01","until","2026-11-04");
    static final PersonalAnalytics.Access ACCESS=new PersonalAnalytics.Access(){public UUID accountId(){return new UUID(0,1);}public UUID requestId(){return new UUID(0,2);}public void requireCurrent(){}public void meaningfulAction(){fail("ANALYTICS_MUST_NOT_EXTEND_ACTIVITY");}};
    static AnalyticsShipments.Fact fact(int id,String time,long fee,boolean accepted,Long...weights){return new AnalyticsShipments.Fact(new UUID(0,id),"AP-SIM-"+id,new UUID(1,id),Instant.parse(time),fee,"SIM-A1","SIM-A2","ARGO-SIM-COVERAGE-1",accepted?Instant.parse(time).plusSeconds(60):null,List.of(weights),Arrays.stream(weights).map(w->((w+499)/500)*500).toList());}
    static PersonalAnalyticsService service(List<AnalyticsShipments.Fact> facts){return new PersonalAnalyticsService((a,f,u)->{a.requireCurrent();return facts.stream().filter(v->!v.bookedAt().isBefore(f)&&v.bookedAt().isBefore(u)).toList();},(a,ids)->facts.stream().filter(f->ids.contains(f.id())).map(f->new AnalyticsObligations.Fact(f.id(),f.quoteId(),f.confirmedVnd(),"VND","UNPAID")).toList(),Clock.fixed(NOW,ZoneOffset.UTC),true);}
    static PersonalAnalytics.Metric metric(PersonalAnalytics.Report report,String id){return report.metrics().stream().filter(m->m.id().equals(id)).findFirst().orElseThrow();}
    static Map<String,String> filters(String...pairs){Map<String,String> p=new HashMap<>(PERIOD);for(int i=0;i<pairs.length;i+=2)p.put(pairs[i],pairs[i+1]);return p;}

    @Test void distinctShipmentParcelAndUnpaidTotalsReconcileWithoutCollectedRevenue(){
        var report=service(List.of(fact(1,"2026-11-01T01:00:00Z",81000,true,500L,1000L),fact(2,"2026-11-02T01:00:00Z",25000,false,500L))).read(ACCESS,PERIOD);
        assertEquals("2",metric(report,"BOOKED_SHIPMENTS").value());assertEquals("3",metric(report,"CREATED_PARCELS").value());assertEquals("1",metric(report,"ACCEPTED_SHIPMENTS").value());assertEquals("1",metric(report,"AWAITING_HANDOVER").value());assertEquals("106000",metric(report,"CONFIRMED_FEES").value());assertEquals("106000",metric(report,"UNPAID_OBLIGATIONS").value());assertEquals("2",report.shipments().total());assertEquals(2,report.shipments().items().size());assertEquals("2",report.routes().getFirst().shipments());assertEquals("1",report.parcels().multiParcelShipments());
        assertTrue(report.metrics().stream().noneMatch(m->Set.of("DELIVERED","PAID","REVENUE","SLA","COD").contains(m.id())));
        var extended=service(List.of(fact(1,"2026-11-01T01:00:00Z",81000,true,500L,1000L),fact(2,"2026-11-02T01:00:00Z",25000,false,500L))).readShippingReport(ACCESS,PERIOD);
        assertEquals("PERSONAL_REPORTS_1",extended.schemaVersion());assertEquals(report,extended.report());assertEquals("1",extended.acceptanceTiming().sampleShipments());assertEquals("60000",extended.acceptanceTiming().totalDurationMillis());assertEquals("60000",extended.acceptanceTiming().minimumMillis());assertEquals("60000",extended.acceptanceTiming().maximumMillis());assertEquals("2000",extended.parcelWeights().actualGrams());assertEquals("2000",extended.parcelWeights().chargeableGrams());
        for(var m:report.metrics())assertEquals(new java.math.BigInteger(m.value()),m.points().stream().map(v->new java.math.BigInteger(v.value())).reduce(java.math.BigInteger.ZERO,java.math.BigInteger::add));
    }
    @Test void emptyCohortIsRealZeroWithNoPreviousDenominator(){
        var report=service(List.of()).read(ACCESS,PERIOD);assertTrue(report.metrics().stream().allMatch(m->m.value().equals("0")&&m.changePercent()==null));assertTrue(report.services().isEmpty());assertTrue(report.routes().isEmpty());assertEquals("0",report.shipments().total());assertFalse(report.shipments().hasNext());assertEquals(3,report.volume().size());var empty=service(List.of()).readShippingReport(ACCESS,PERIOD);assertEquals("0",empty.acceptanceTiming().sampleShipments());assertEquals("0",empty.acceptanceTiming().totalDurationMillis());assertNull(empty.acceptanceTiming().minimumMillis());assertNull(empty.acceptanceTiming().maximumMillis());assertEquals("0",empty.parcelWeights().chargeableGrams());
    }
    @Test void timezoneBoundsAndPartialComparisonHaveEqualElapsedPeriods(){
        var report=service(List.of(fact(1,"2026-10-31T16:59:59Z",1,false,1L),fact(2,"2026-10-31T17:00:00Z",2,false,1L),fact(3,"2026-11-03T05:00:00.000001Z",3,false,1L),fact(4,"2026-10-30T01:00:00Z",1,false,1L))).read(ACCESS,PERIOD);
        assertEquals("1",metric(report,"BOOKED_SHIPMENTS").value());assertEquals("1",metric(report,"BOOKED_SHIPMENTS").previousValue());assertEquals("0.0",metric(report,"BOOKED_SHIPMENTS").changePercent());assertEquals(Instant.parse("2026-10-31T17:00:00Z"),report.period().startInclusive());assertEquals(NOW.plusNanos(1000),report.period().endExclusive());assertEquals(Duration.between(report.period().startInclusive(),report.period().endExclusive()),Duration.between(report.period().previousStartInclusive(),report.period().previousEndExclusive()));
    }
    @Test void currentStatusRouteDayAndWeightFiltersUseTheSameContributingCohort(){
        var source=service(List.of(fact(1,"2026-11-01T01:00:00Z",25000,true,500L,1000L),fact(2,"2026-11-02T01:00:00Z",25000,false,500L)));
        var report=source.read(ACCESS,filters("status","AT_STATION","origin","SIM-A1","destination","SIM-A2","day","2026-11-01","weight","UP_TO_5KG","multi","true"));
        assertEquals("1",metric(report,"BOOKED_SHIPMENTS").value());assertEquals("1",report.status().getLast().shipments());assertEquals(1,report.shipments().items().size());assertEquals("2",report.parcels().count());assertEquals("1",report.parcels().weight().get(1).parcels());assertEquals("25000",report.shipments().items().getFirst().unpaidVnd());
    }
    @Test void moneyRemainsExactBeyondJavascriptAndLongSumRanges(){
        var report=service(List.of(fact(1,"2026-11-01T01:00:00Z",Long.MAX_VALUE,false,1L),fact(2,"2026-11-02T01:00:00Z",Long.MAX_VALUE,false,1L))).read(ACCESS,PERIOD);
        assertEquals("18446744073709551614",metric(report,"CONFIRMED_FEES").value());assertEquals(metric(report,"CONFIRMED_FEES").value(),metric(report,"UNPAID_OBLIGATIONS").value());assertNull(metric(report,"CONFIRMED_FEES").changePercent());
    }
    @Test void stableSortPaginationAndLiteralCodeSearchNeverChangeAggregateDefinitions(){
        var facts=new ArrayList<AnalyticsShipments.Fact>();for(int i=1;i<=25;i++)facts.add(fact(i,"2026-11-01T01:00:00Z",i,false,1L));var source=service(facts);
        var a=source.read(ACCESS,filters("sort","FEE_DESC"));var b=source.read(ACCESS,filters("sort","FEE_DESC","page","1"));assertEquals("25",a.shipments().total());assertEquals(a.metrics(),b.metrics());assertEquals(20,a.shipments().items().size());assertEquals(5,b.shipments().items().size());assertTrue(a.shipments().hasNext());assertFalse(b.shipments().hasNext());var ext=source.readShippingReport(ACCESS,filters("page","1"));assertEquals("25",ext.parcelWeights().actualGrams());assertEquals("12500",ext.parcelWeights().chargeableGrams());assertEquals("25",ext.report().shipments().total());assertEquals(5,ext.report().shipments().items().size());assertEquals("25",a.shipments().items().getFirst().confirmedFeeVnd());assertEquals("5",b.shipments().items().getFirst().confirmedFeeVnd());
        assertEquals("0",source.read(ACCESS,filters("search","%_")).shipments().total());
    }
    @Test void malformedSelectorsAndUnsupportedBusinessContextFailClosed(){
        var source=service(List.of());for(var parameters:List.of(filters("ownerId","foreign"),filters("organizationId","foreign"),filters("context","BUSINESS"),filters("status","DELIVERED"),filters("origin","SIM-A2","destination","SIM-B1"),filters("from","2026-02-30"),filters("until","2027-11-04"),filters("page","-1"),filters("compare","invented"),filters("search","\n")))assertEquals(AnalyticsFailure.Code.VALIDATION_FAILED,assertThrows(AnalyticsFailure.class,()->source.read(ACCESS,parameters)).code());
    }
    @Test void missingFinanceSourceAndDuplicateShipmentCannotBecomeZeroOrDoubleCount(){
        var f=fact(1,"2026-11-01T01:00:00Z",25000,false,500L);var source=new PersonalAnalyticsService((a,start,end)->List.of(f),(a,ids)->List.of(),Clock.fixed(NOW,ZoneOffset.UTC),true);
        assertEquals(AnalyticsFailure.Code.ANALYTICS_UNAVAILABLE,assertThrows(AnalyticsFailure.class,()->source.read(ACCESS,PERIOD)).code());
        var duplicated=new PersonalAnalyticsService((a,start,end)->List.of(f,f),(a,ids)->List.of(),Clock.fixed(NOW,ZoneOffset.UTC),true);assertThrows(AnalyticsFailure.class,()->duplicated.read(ACCESS,PERIOD));
        for(var invalid:List.of(new AnalyticsShipments.Fact(f.id(),f.code(),f.quoteId(),f.bookedAt(),f.confirmedVnd(),f.origin(),f.destination(),f.datasetVersion(),null,f.weights(),List.of(499L)),new AnalyticsShipments.Fact(f.id(),f.code(),f.quoteId(),f.bookedAt(),f.confirmedVnd(),f.origin(),f.destination(),f.datasetVersion(),f.bookedAt().minusNanos(1000),f.weights(),List.of(500L))))assertEquals(AnalyticsFailure.Code.ANALYTICS_UNAVAILABLE,assertThrows(AnalyticsFailure.class,()->service(List.of(invalid)).readShippingReport(ACCESS,PERIOD)).code());
    }
    @Test void rangeLimitAndDisabledEnvironmentNeverTruncateOrQueryAsSuccess(){
        var many=new PersonalAnalyticsService((a,start,end)->Collections.nCopies(10001,fact(1,"2026-11-01T01:00:00Z",1,false,1L)),(a,ids)->{fail();return List.of();},Clock.fixed(NOW,ZoneOffset.UTC),true);
        assertEquals(AnalyticsFailure.Code.ANALYTICS_RANGE_TOO_LARGE,assertThrows(AnalyticsFailure.class,()->many.read(ACCESS,PERIOD)).code());
        var disabled=new PersonalAnalyticsService((a,f,u)->{fail();return List.of();},(a,ids)->List.of(),Clock.fixed(NOW,ZoneOffset.UTC),false);assertEquals(AnalyticsFailure.Code.PILOT_DISABLED,assertThrows(AnalyticsFailure.class,()->disabled.read(ACCESS,PERIOD)).code());assertEquals(AnalyticsFailure.Code.PILOT_DISABLED,assertThrows(AnalyticsFailure.class,()->disabled.readShippingReport(ACCESS,PERIOD)).code());
        for(String profile:List.of("prod","production"))try(var context=new org.springframework.context.annotation.AnnotationConfigApplicationContext()){
            context.getEnvironment().setActiveProfiles("customer",profile);
            context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("synthetic-mode",Map.of("shipping.pilot.environment","LOCAL","customer.rollout","SANDBOX")));
            context.registerBean("identityHttpClock",Clock.class,()->Clock.fixed(NOW,ZoneOffset.UTC));
            context.registerBean(AnalyticsShipments.class,()->(a,f,u)->{fail("PRODUCTION_MUST_NOT_QUERY");return List.of();});
            context.registerBean(AnalyticsObligations.class,()->(a,ids)->List.of());
            context.register(com.elogistic.backend.notifications.internal.AnalyticsConfiguration.class);context.refresh();
            assertEquals(AnalyticsFailure.Code.PILOT_DISABLED,assertThrows(AnalyticsFailure.class,()->context.getBean(PersonalAnalytics.class).read(ACCESS,PERIOD)).code());
        }
    }
    @Test void weeklyMonthlyAndNoComparisonRetainReconciledSourceCounts(){
        var source=service(List.of(fact(1,"2026-11-01T01:00:00Z",25000,false,500L),fact(2,"2026-11-02T01:00:00Z",25000,false,500L)));
        for(String bucket:List.of("day","week","month")){var report=source.read(ACCESS,filters("bucket",bucket,"compare","none"));assertEquals("2",metric(report,"BOOKED_SHIPMENTS").value());assertNull(report.period().previousStartInclusive());assertTrue(report.metrics().stream().allMatch(m->m.previousValue()==null&&m.changePercent()==null));assertEquals(2,report.volume().stream().mapToInt(v->Integer.parseInt(v.shipments())).sum());var selected=source.read(ACCESS,filters("bucket",bucket,"day",report.volume().getFirst().date().toString()));assertEquals(report.volume().getFirst().shipments(),selected.shipments().total());}
    }
}
