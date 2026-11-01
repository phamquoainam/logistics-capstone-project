package com.elogistic.backend.orders.internal.domain;
import com.elogistic.backend.orders.api.ShippingDrafts.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ShippingPreparationTests {
    private Input input(){var contact=new Contact(" Test ","0901234567",new Address("VN","Area","Locality","Street"));return new Input(null,null,contact,contact,List.of(new Parcel("Box",1,501,100,100,100,0)));}
    @Test void normalizesIndependentSnapshotsWithoutClaimingServiceEligibility(){var i=ShippingPreparation.normalize(input());assertEquals("+84901234567",i.sender().phone());assertEquals("Test",i.sender().name());assertEquals(501,i.parcels().getFirst().weightGrams());assertFalse(i.toString().contains("849"));}
    @Test void rejectsInvalidIntegersContactsAndParcelCount(){var i=input();assertThrows(IllegalArgumentException.class,()->ShippingPreparation.normalize(new Input(null,null,i.sender(),i.recipient(),List.of())));assertThrows(IllegalArgumentException.class,()->ShippingPreparation.normalize(new Input(null,null,i.sender(),i.recipient(),Collections.nCopies(6,i.parcels().getFirst()))));assertThrows(IllegalArgumentException.class,()->ShippingPreparation.normalize(new Input(null,null,i.sender(),i.recipient(),List.of(new Parcel("Box",1,0,1,1,1,0)))));}
    @Test void preparationValidationDoesNotActivateCandidateCommercialLimits(){var i=input();assertEquals(20001,ShippingPreparation.normalize(new Input(null,null,i.sender(),i.recipient(),List.of(new Parcel("Box",1,20001,1001,1001,1001,0)))).parcels().getFirst().weightGrams());}
}
