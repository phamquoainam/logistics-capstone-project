package com.elogistic.backend.orders.internal.infrastructure;

import com.elogistic.backend.orders.api.BusinessPreparations;
import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BusinessManagementFiltersTests {
    private static final Instant NOW=Instant.parse("2026-11-13T05:00:00Z");
    private static final String ID="10000000-0000-4000-8000-000000000001";
    private static Map<String,String> p(String... pairs){Map<String,String> p=new HashMap<>();p.put("view","management");for(int i=0;i<pairs.length;i+=2)p.put(pairs[i],pairs[i+1]);return p;}
    private void denied(Map<String,String> p){assertEquals("VALIDATION_FAILED",assertThrows(BusinessPreparations.Failure.class,()->BusinessManagementFilters.parse(p,NOW,false)).getMessage());}
    @Test void managementRequiresExactViewAndKnownFields(){denied(Map.of());denied(p("view","legacy"));denied(p("actorAccountId",ID));denied(p("organizationId",ID));denied(p("includePii","true"));denied(p("view","management","ids",ID));assertEquals(0,BusinessManagementFilters.parse(p(),NOW,false).page());}
    @Test void datePairUsesVietnamMidnightAndExactExclusiveBoundary(){var v=BusinessManagementFilters.parse(p("from","2026-11-12","until","2026-11-14"),NOW,false);assertEquals(Instant.parse("2026-11-11T17:00:00Z"),v.from());assertEquals(NOW.plusNanos(1000),v.until());var old=BusinessManagementFilters.parse(p("from","2026-11-01","until","2026-11-02"),NOW,false);assertEquals(Instant.parse("2026-11-01T17:00:00Z"),old.until());denied(p("from","2026-11-01"));denied(p("from","2026-02-30","until","2026-03-01"));denied(p("from","2026-11-12","until","2026-11-15"));denied(p("from","2026-11-12","until","2026-11-12"));}
    @Test void maximum366DayWindowDoesNotBecome367(){assertNotNull(BusinessManagementFilters.parse(p("from","2025-11-13","until","2026-11-14"),NOW,false));denied(p("from","2025-11-12","until","2026-11-14"));denied(p("from","2026-11-14","until","2026-11-13"));}
    @Test void paginationAndSortAreNumericAndAllowlisted(){assertEquals(Integer.MAX_VALUE,BusinessManagementFilters.parse(p("page",String.valueOf(Integer.MAX_VALUE),"sort","FEE_DESC"),NOW,false).page());for(String value:List.of("-1","01","1.5","2147483648"," 1"))denied(p("page",value));denied(p("size","200"));denied(p("sort","NEWEST;DROP TABLE orders.pilot_shipment"));}
    @Test void onlyApprovedRoutesAndCapabilitiesAreQueryable(){for(String value:List.of("SIM-A1/SIM-A2","SIM-A2/SIM-A1","SIM-A1/SIM-B1","SIM-B1/SIM-A1")){String[] route=value.split("/");assertNotNull(BusinessManagementFilters.parse(p("origin",route[0],"destination",route[1],"status","WAITING_FOR_HANDOVER","handover","DROP_OFF","obligation","UNPAID","service","ROAD_STANDARD"),NOW,false));}denied(p("origin","SIM-A1"));denied(p("origin","SIM-A2","destination","SIM-B1"));denied(p("handover","PICKUP"));denied(p("obligation","PAID"));denied(p("status","DELIVERED"));}
    @Test void uuidFiltersAndTextCannotContainHiddenControlOrOwnerInjection(){assertEquals(UUID.fromString(ID),BusinessManagementFilters.parse(p("facilityId",ID,"creatorId",ID,"batchId",ID,"assignee","NONE"),NOW,false).facility());denied(p("facilityId","00000000-0000-0000-0000-000000000000"));denied(p("assignee","other"));denied(p("search","x\ny"));denied(p("search","x".repeat(161)));denied(p("search"," "));denied(p("sourceChannel","x".repeat(41)));}
    @Test void exportsAreUnpagedBoundedDistinctSelectionsAndPiiIsExplicit(){Map<String,String> p=Map.of("ids",String.join(",",IntStream.range(1,21).mapToObj(i->new UUID(1,i).toString()).toList()),"includePii","true");var result=BusinessManagementFilters.parse(p,NOW,true);assertEquals(20,result.selected().size());assertTrue(result.includePii());for(Map<String,String> invalid:List.of(Map.of("page","0"),Map.of("size","20"),Map.of("ids",ID+","+ID),Map.of("ids",""),Map.of("includePii","1"),Map.of("view","management")))assertThrows(BusinessPreparations.Failure.class,()->BusinessManagementFilters.parse(invalid,NOW,true));assertFalse(BusinessManagementFilters.parse(Map.of(),NOW,true).includePii());}
    @Test void literalSearchEscapingAndOrderingNeverTrustSqlExpressions(){assertEquals("%a!%!_!!%",BusinessManagementFilters.like(" A%_! "));var v=BusinessManagementFilters.parse(p("search","' OR 1=1 --","sort","OLDEST"),NOW,false);assertEquals("s.created_at ASC,s.id ASC",v.order());assertEquals("' OR 1=1 --",v.search());assertThrows(UnsupportedOperationException.class,()->v.parameters().put("scope","ALL"));}
}
