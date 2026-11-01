package com.elogistic.backend.merchant.internal.domain;

import com.elogistic.backend.merchant.api.customer.PersonalAddresses.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerAddressTests {
    private static final Instant NOW=Instant.parse("2026-11-01T00:00:00Z");
    private Input input(String phone){return new Input("SENDER"," Home "," Synthetic name ",phone,new Location("VN"," Area "," Locality "," Address line "),true);}
    private CustomerAddress address(){return new CustomerAddress(UUID.randomUUID(),UUID.randomUUID(),input("0901234567"),1,NOW,NOW,null);}
    @Test void domesticContactNormalizesWithoutBecomingIdentityCredential(){for(String phone:List.of("0901-234-567","+84 901 234 567","84901234567")){var value=CustomerAddress.normalize(input(phone));assertEquals("+84901234567",value.contactPhone());assertEquals("Home",value.label());assertEquals("Area",value.address().administrativeArea());assertFalse(value.toString().contains("901"));}}
    @Test void invalidPhonesControlCharactersAndUnknownCountryFailBeforePersistence(){
        for(String phone:List.of("123","+1 901234567","0901234567\n","0".repeat(33)))assertThrows(IllegalArgumentException.class,()->CustomerAddress.normalize(input(phone)));
        for(var value:List.of(new Input("OWNER",null,"Name","0901234567",input("0").address(),false),new Input("SENDER"," ","Name","0901234567",input("0").address(),false),new Input("RECIPIENT",null,"Name\n","0901234567",input("0").address(),false),new Input("SENDER",null,"Name","0901234567",new Location("US","Area","Locality","Line"),false)))assertThrows(IllegalArgumentException.class,()->CustomerAddress.normalize(value));
    }
    @Test void archivePreservesBindingAndHistoryVersionButRemovesDefault(){var before=address();var after=before.archive(NOW.plusSeconds(1));assertEquals(before.id(),after.id());assertEquals(before.accountId(),after.accountId());assertEquals(2,after.version());assertFalse(after.input().isDefault());assertEquals(after.updatedAt(),after.archivedAt());assertThrows(IllegalArgumentException.class,()->after.replace(before.input(),NOW.plusSeconds(2)));assertThrows(IllegalArgumentException.class,()->after.withoutDefault(NOW.plusSeconds(2)));}
    @Test void snapshotsAndQueriesNeverPrintPrivateContact(){var value=address();assertFalse(value.toString().contains("Synthetic"));assertFalse(value.view().toString().contains("Address line"));assertFalse(value.input().address().toString().contains("Locality"));assertFalse(new Query(0,20,"private name","SENDER").toString().contains("private name"));}
}
