package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.MerchantFacilities.*;
import com.elogistic.backend.merchant.api.customer.PersonalAddresses.Location;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FacilityInputTests {
    Contact contact(){return new Contact(" Synthetic contact ","090 123 4567",new Location("VN","Synthetic area","Synthetic locality","Synthetic street"));}
    Input input(Hours hours){return new Input(" Site ","wh-01","WAREHOUSE",contact(),null,contact(),hours);}
    Hours hours(List<Interval> slots){return new Hours("Asia/Ho_Chi_Minh",FacilityInput.DAYS.stream().map(day->new Day(day,slots)).toList());}
    @Test void normalizedContactCodeAndUnknownHoursNeverImplyCarrierService(){
        var value=FacilityInput.normalize(input(null));assertEquals("WH-01",value.code());assertEquals("+84901234567",value.contact().phone());assertEquals("Site",value.name());assertNull(value.operatingHours());assertFalse(value.toString().contains("Synthetic"));
    }
    @Test void sevenDaysTwoOrderedIntervalsClosingMidnightAndClosedDays(){
        assertEquals("24:00",FacilityInput.normalize(input(hours(List.of(new Interval("08:00","12:00"),new Interval("13:00","24:00"))))).operatingHours().days().getFirst().intervals().getLast().closesAt());
        assertTrue(FacilityInput.normalize(input(hours(List.of()))).operatingHours().days().getFirst().intervals().isEmpty());
    }
    @Test void malformedOverlappingDuplicateAndUnsupportedHoursAreRejected(){
        for(var slot:List.of(List.of(new Interval("24:00","24:00")),List.of(new Interval("8:00","12:00")),List.of(new Interval("13:00","12:00")),List.of(new Interval("08:00","13:00"),new Interval("12:00","15:00"))))assertThrows(IllegalArgumentException.class,()->FacilityInput.normalize(input(hours(slot))));
        assertThrows(IllegalArgumentException.class,()->FacilityInput.hours(new Hours("UTC",hours(List.of()).days())));
        assertThrows(IllegalArgumentException.class,()->FacilityInput.hours(new Hours("Asia/Ho_Chi_Minh",Collections.nCopies(7,new Day("MONDAY",List.of())))));
    }
    @Test void strictBoundariesUnicodeAndControlCharacters(){
        assertEquals(160,FacilityInput.normalize(new Input("a".repeat(160),"x".repeat(40),"STORE",contact(),null,null,null)).name().length());
        for(String code:List.of("../site","x".repeat(41),"a\nb"))assertThrows(IllegalArgumentException.class,()->FacilityInput.normalize(new Input("Site",code,"STORE",contact(),null,null,null)));
        assertThrows(IllegalArgumentException.class,()->FacilityInput.normalize(new Input("Site","CODE","STATION",contact(),null,null,null)));
        assertThrows(IllegalArgumentException.class,()->FacilityInput.normalize(new Input("Site","CODE","STORE",new Contact("Bad\nName","0901234567",contact().address()),null,null,null)));
    }
}
