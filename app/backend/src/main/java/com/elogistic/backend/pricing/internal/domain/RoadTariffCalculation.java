package com.elogistic.backend.pricing.internal.domain;
import com.elogistic.backend.pricing.api.PilotQuotes.*;
import java.math.BigInteger;
import java.util.*;
public final class RoadTariffCalculation {
    private RoadTariffCalculation(){}
    public static List<Line> calculate(Rules rules,String classification,List<Parcel> parcels){
        if(rules==null||!Set.of("INTRA","INTER").contains(classification)||parcels==null||parcels.isEmpty()||parcels.size()>rules.maxParcels())throw new IllegalArgumentException();List<Line> lines=new ArrayList<>();int number=0;
        for(var p:parcels){if(p==null||p.weightGrams()<1||p.weightGrams()>rules.maxActualGrams()||p.lengthMm()<1||p.widthMm()<1||p.heightMm()<1||p.lengthMm()>rules.maxDimensionMm()||p.widthMm()>rules.maxDimensionMm()||p.heightMm()>rules.maxDimensionMm()||p.lengthMm()+2*(p.widthMm()+p.heightMm())>rules.maxGirthMm())throw new IllegalArgumentException();
            BigInteger denominator=BigInteger.valueOf(rules.volumetricDivisor()).multiply(BigInteger.valueOf(rules.stepGrams()));BigInteger volume=BigInteger.valueOf(p.lengthMm()).multiply(BigInteger.valueOf(p.widthMm())).multiply(BigInteger.valueOf(p.heightMm()));long dimensionalSteps=volume.add(denominator.subtract(BigInteger.ONE)).divide(denominator).longValueExact();long steps=Math.max((p.weightGrams()+rules.stepGrams()-1)/rules.stepGrams(),dimensionalSteps);boolean intra=classification.equals("INTRA");long fee=Math.addExact(intra?rules.intraFirstVnd():rules.interFirstVnd(),Math.multiplyExact(steps-1,intra?rules.intraStepVnd():rules.interStepVnd()));lines.add(new Line(++number,p.weightGrams(),Math.multiplyExact(steps,rules.stepGrams()),fee));
        }return List.copyOf(lines);
    }
}
