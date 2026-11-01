package com.elogistic.backend.pricing.internal.domain;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.elogistic.backend.pricing.internal.domain.RoadPilotCandidate.*;
class RoadPilotCandidateTests {
    private Parcel p(long grams){return new Parcel(grams,100,100,100);}
    @Test void roundsEachParcelAtFiveHundredGramBoundary(){assertEquals(25000,estimate(Region.INTRA,List.of(p(500))).baseFeeVnd());assertEquals(28000,estimate(Region.INTRA,List.of(p(501))).baseFeeVnd());assertEquals(43000,estimate(Region.INTER,List.of(p(501))).baseFeeVnd());assertEquals(76000,estimate(Region.INTER,List.of(p(1),p(499))).baseFeeVnd());}
    @Test void dimensionalWeightUsesExactIntegerUnitsAndPerParcelSum(){var result=estimate(Region.INTRA,List.of(new Parcel(500,400,400,400),p(500)));assertEquals(13000,result.lines().getFirst().chargeableGrams());assertEquals(125000,result.baseFeeVnd());}
    @Test void rejectsUnsupportedSizeGirthWeightAndParcelCount(){assertThrows(IllegalArgumentException.class,()->estimate(Region.INTRA,List.of(p(20001))));assertThrows(IllegalArgumentException.class,()->estimate(Region.INTRA,List.of(new Parcel(500,1001,1,1))));assertThrows(IllegalArgumentException.class,()->estimate(Region.INTRA,List.of(new Parcel(500,1000,400,400))));assertThrows(IllegalArgumentException.class,()->estimate(Region.INTRA,Collections.nCopies(6,p(500))));}
}
