package com.elogistic.backend.notifications.internal.application;

import com.elogistic.backend.identity.api.admin.*;
import com.elogistic.backend.notifications.api.AdminDashboardReads;
import com.elogistic.backend.orders.api.AdminShipmentFacts;
import com.elogistic.backend.finance.api.AdminObligationFacts;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdminDashboardTests {
    static final Instant NOW=Instant.parse("2026-11-21T05:00:00Z");
    final AdminDashboardService service=new AdminDashboardService(new byte[32]);
    AdminReadContext actor(Set<String> permissions){return new AdminReadContext(){public UUID accountId(){return new UUID(0,1);}public UUID realmId(){return new UUID(0,2);}public String displayName(){return "Synthetic Admin";}public String environment(){return "TEST";}public String permissionVersion(){return "version-one";}public Set<String> permissions(){return permissions;}public void requireCurrent(){}};}
    AdminDashboardReads.Query query(){return service.query(Map.of("from","2026-11-01","to","2026-11-21","grain","DAY"),NOW,false);}
    AdminShipmentFacts.Row row(int n,int parcels,String status){return new AdminShipmentFacts.Row(new UUID(0,n),"AP-SIM-"+String.format("%032x",n),n%2==0?"BUSINESS":"PERSONAL",NOW.minusSeconds(n),status,parcels);}
    @Test void exactCountsMoneyAndSeparateUnavailableSources(){
        var a=actor(AdminReadContext.CODES);var rows=List.of(row(1,3,"BOOKED"),row(2,2,"AT_STATION"));
        var money=List.of(new AdminObligationFacts.Row(rows.get(0).id(),new BigInteger("9007199254740993"),"UNPAID"),new AdminObligationFacts.Row(rows.get(1).id(),BigInteger.valueOf(25000),"UNPAID"));
        var d=service.dashboard(a,query(),NOW,rows,money,Set.of("MERCHANT","NETWORK"));
        var metrics=d.sections().stream().flatMap(s->s.metrics().stream()).toList();
        assertEquals("2",metrics.stream().filter(m->m.key().equals("BOOKINGS")).findFirst().orElseThrow().value());
        assertEquals("5",metrics.stream().filter(m->m.key().equals("PARCELS")).findFirst().orElseThrow().value());
        assertEquals("9007199254765993",metrics.stream().filter(m->m.key().equals("UNPAID_FEES")).findFirst().orElseThrow().value());
        assertTrue(d.sections().stream().filter(s->s.key().equals("MERCHANT")).allMatch(s->s.metrics().isEmpty()));
        assertFalse(metrics.stream().anyMatch(m->m.key().contains("REVENUE")));
        var list=service.shipments(a,query(),d.snapshotId(),NOW,rows,money,"WAITING_HANDOVER",0,20);
        assertEquals(List.of(rows.get(0)),list.items());
    }
    @Test void validationTimezoneAndBoundedFilters(){
        assertEquals(Instant.parse("2026-10-31T17:00:00Z"),query().from().atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
        for(var bad:List.of(Map.of("from","2026-11-01","to","2026-11-21","regionId","SIM-A1"),Map.of("from","2026-11-01","to","2026-11-21","grain","YEAR"),Map.of("from","2026-11-01","to","2026-11-21","permission","ADMIN_FINANCE_READ"),Map.of("from","2025-01-01","to","2026-11-21")))assertThrows(AdminFailure.class,()->service.query(bad,NOW,false));
    }
    @Test void snapshotActorQueryExpiryTamperAndDriftDeny(){
        var a=actor(Set.of(AdminReadContext.ENTRY,AdminReadContext.SHIPMENT));var rows=List.of(row(1,1,"BOOKED"));var d=service.dashboard(a,query(),NOW,rows,List.of(),Set.of());
        assertThrows(AdminFailure.class,()->service.selector(a,query(),d.snapshotId()+"x",NOW));
        assertThrows(AdminFailure.class,()->service.selector(a,query(),d.snapshotId(),NOW.plusSeconds(900)));
        assertThrows(AdminFailure.class,()->service.shipments(a,query(),d.snapshotId(),NOW,List.of(row(1,2,"BOOKED")),List.of(),"BOOKINGS",0,20));
        assertThrows(AdminFailure.class,()->service.selector(a,new AdminDashboardReads.Query(query().from(),query().to(),"MONTH",null,null),d.snapshotId(),NOW));
    }
    @Test void permissionsEmptyCohortAndFinancialDrillRequireBothCapabilities(){
        var a=actor(Set.of(AdminReadContext.ENTRY,AdminReadContext.SHIPMENT));var d=service.dashboard(a,query(),NOW,List.of(),List.of(),Set.of());
        assertTrue(d.sections().stream().noneMatch(s->s.key().equals("FINANCE")));
        assertTrue(d.sections().stream().filter(s->s.key().equals("SHIPMENTS")).flatMap(s->s.metrics().stream()).allMatch(m->m.value().equals("0")));
        assertThrows(AdminFailure.class,()->service.shipments(a,query(),d.snapshotId(),NOW,List.of(),List.of(),"CONFIRMED_FEES",0,20));
        var finance=actor(Set.of(AdminReadContext.ENTRY,AdminReadContext.FINANCE));var f=service.dashboard(finance,query(),NOW,List.of(),List.of(),Set.of());
        assertTrue(f.sections().stream().flatMap(s->s.metrics().stream()).allMatch(m->m.drilldown()==null));
        assertThrows(AdminFailure.class,()->service.shipments(finance,query(),f.snapshotId(),NOW,List.of(),List.of(),"BOOKINGS",0,20));
    }
    @Test void weekMonthAndSingleDayBucketsClipCalendarEdgesWithoutDoubleCounting(){
        var a=actor(Set.of(AdminReadContext.ENTRY,AdminReadContext.SHIPMENT));var rows=List.of(row(1,2,"BOOKED"),row(2,1,"AT_STATION"));
        for(String grain:List.of("DAY","WEEK","MONTH")){
            var q=service.query(Map.of("from","2026-11-01","to","2026-11-21","grain",grain),NOW,false);var result=service.dashboard(a,q,NOW,rows,List.of(),Set.of());
            var series=result.sections().getFirst().series().stream().filter(p->p.metric().equals("PARCELS")).toList();assertEquals(q.from(),series.getFirst().from());assertEquals(q.to().plusDays(1),series.getLast().to());assertEquals(BigInteger.valueOf(3),series.stream().map(p->new BigInteger(p.value())).reduce(BigInteger.ZERO,BigInteger::add));
            for(int n=1;n<series.size();n++)assertEquals(series.get(n-1).to(),series.get(n).from());
        }
    }
}
