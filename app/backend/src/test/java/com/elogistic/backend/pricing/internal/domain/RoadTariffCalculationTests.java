package com.elogistic.backend.pricing.internal.domain;
import com.elogistic.backend.pricing.api.PilotQuotes.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RoadTariffCalculationTests {
    private Rules rules(){return new Rules("version-1",25000,3000,38000,5000,500,5000,5,20000,1000,2000,900,"SIMULATION_NOT_TAX_INVOICE");}
    @Test void calculatesApprovedPerParcelFeesFromVersionedRulesWithExactUnits(){var lines=RoadTariffCalculation.calculate(rules(),"INTER",List.of(new Parcel(501,100,100,100),new Parcel(500,400,400,400)));assertEquals(43000,lines.getFirst().feeVnd());assertEquals(13000,lines.getLast().chargeableGrams());assertEquals(163000,lines.getLast().feeVnd());}
    @Test void enforcesEveryApprovedBoundaryWithoutImplyingPayment(){assertEquals(142000,RoadTariffCalculation.calculate(rules(),"INTRA",List.of(new Parcel(20000,1000,250,250))).getFirst().feeVnd());assertThrows(IllegalArgumentException.class,()->RoadTariffCalculation.calculate(rules(),"INTRA",List.of(new Parcel(20001,1,1,1))));assertThrows(IllegalArgumentException.class,()->RoadTariffCalculation.calculate(rules(),"INTRA",List.of(new Parcel(500,1000,251,250))));assertThrows(IllegalArgumentException.class,()->RoadTariffCalculation.calculate(rules(),"UNKNOWN",List.of(new Parcel(500,100,100,100))));}
}
