package com.elogistic.backend.workflow.internal.application;

import com.elogistic.backend.merchant.api.AdminApplicationFacts.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdminApplicationMetricsTests {
    private static Row row(String state,String first,String terminal){Instant start=Instant.parse(first),end=terminal==null?null:Instant.parse(terminal);return new Row(UUID.randomUUID(),state,start,end==null?start:end,end,end==null?start:end);}
    private static Capture capture(List<Row> rows){return new Capture("AVAILABLE","CANONICAL_FIRST_SUBMITTED_COHORT","a".repeat(64),"b".repeat(64),rows);}
    @Test void submittedPopulationAndTerminalDurationUseDifferentCorrectDenominators(){
        var rows=List.of(row("REJECTED","2026-11-01T00:00:00Z","2026-11-01T00:00:01.001Z"),row("APPROVED","2026-11-01T00:00:00Z","2026-11-01T00:00:02.002Z"),row("UNDER_REVIEW","2026-11-02T00:00:00Z",null));
        var result=AdminApplicationMetrics.measure(capture(rows));assertEquals("3",result.get("APPLICATIONS_SUBMITTED").value());assertNull(result.get("APPLICATIONS_SUBMITTED").denominator());
        var average=result.get("APPLICATION_PROCESSING_AVG_MS");assertEquals("1501",average.value());assertEquals("3003",average.numerator());assertEquals("2",average.denominator());assertEquals("2",average.sampleCount());
        assertEquals("1001",result.get("APPLICATION_PROCESSING_MIN_MS").value());assertEquals("2002",result.get("APPLICATION_PROCESSING_MAX_MS").value());assertEquals("1",result.get("APPLICATIONS_UNDER_REVIEW").value());
    }
    @Test void emptyAndUnavailableSourcesNeverInventObservedProcessingTimes(){
        var empty=AdminApplicationMetrics.measure(capture(List.of()));assertEquals("0",empty.get("APPLICATIONS_SUBMITTED").value());assertNull(empty.get("APPLICATION_PROCESSING_AVG_MS").value());assertEquals("0",empty.get("APPLICATION_PROCESSING_AVG_MS").denominator());assertFalse(empty.containsKey("APPLICATION_PROCESSING_MIN_MS"));assertFalse(empty.containsKey("APPLICATION_PROCESSING_MAX_MS"));
        assertTrue(AdminApplicationMetrics.measure(new Capture("POLICY_NOT_CONFIGURED","MERCHANT_APPLICATION_SCOPE_REQUIRED","a","b",List.of())).isEmpty());
    }
    @Test void invalidStatesDuplicateIdsAndUnboundTerminalFactsFailClosed(){
        Row existing=row("SUBMITTED","2026-11-01T00:00:00Z",null);assertThrows(RuntimeException.class,()->AdminApplicationMetrics.measure(capture(List.of(existing,existing))));
        assertThrows(RuntimeException.class,()->AdminApplicationMetrics.measure(capture(List.of(row("DRAFT","2026-11-01T00:00:00Z",null)))));
        assertThrows(RuntimeException.class,()->AdminApplicationMetrics.measure(capture(List.of(row("APPROVED","2026-11-01T00:00:00Z",null)))));
    }
    @Test void vietnamFirstSubmissionBucketsPartitionCalendarWithoutCountingTerminalEventDay(){
        var rows=List.of(row("APPROVED","2026-10-31T17:00:00Z","2026-11-03T00:00:00Z"),row("SUBMITTED","2026-11-01T17:00:00Z",null));
        for(String grain:List.of("DAY","WEEK","MONTH")){
            var points=AdminApplicationMetrics.series(capture(rows),LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-03"),grain);
            assertEquals(2,points.stream().filter(p->p.metric().equals("APPLICATIONS_SUBMITTED")).mapToInt(p->Integer.parseInt(p.value())).sum());
            var first=points.stream().filter(p->p.metric().equals("APPLICATIONS_APPROVED")).findFirst().orElseThrow();assertEquals("1",first.value());assertEquals(LocalDate.parse("2026-11-01"),first.from());
        }
    }
}
