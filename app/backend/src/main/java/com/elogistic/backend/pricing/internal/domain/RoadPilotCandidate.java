package com.elogistic.backend.pricing.internal.domain;
import java.math.BigInteger;
import java.util.*;

/** Inactive decision-support calculation only: never issues a Quote, service grant or amount due. */
public final class RoadPilotCandidate {
    public enum Region { INTRA,INTER }
    public record Parcel(long weightGrams,long lengthMm,long widthMm,long heightMm){}
    public record Line(long actualGrams,long chargeableGrams,long baseFeeVnd){}
    public record Estimate(List<Line> lines,long baseFeeVnd){public Estimate{lines=List.copyOf(lines);}}
    private RoadPilotCandidate(){}
    public static Estimate estimate(Region region,List<Parcel> parcels){
        if(region==null||parcels==null||parcels.isEmpty()||parcels.size()>5)throw new IllegalArgumentException("CANDIDATE_INPUT_INVALID");List<Line> lines=new ArrayList<>();long total=0;
        for(var p:parcels){if(p==null||p.weightGrams()<1||p.weightGrams()>20000||p.lengthMm()<1||p.widthMm()<1||p.heightMm()<1||p.lengthMm()>1000||p.widthMm()>1000||p.heightMm()>1000||p.lengthMm()+2*(p.widthMm()+p.heightMm())>2000)throw new IllegalArgumentException("CANDIDATE_PARCEL_LIMIT");
            BigInteger volume=BigInteger.valueOf(p.lengthMm()).multiply(BigInteger.valueOf(p.widthMm())).multiply(BigInteger.valueOf(p.heightMm()));long volumetricSteps=volume.add(BigInteger.valueOf(2499999)).divide(BigInteger.valueOf(2500000)).longValueExact();long steps=Math.max((p.weightGrams()+499)/500,volumetricSteps);
            long base=region==Region.INTRA?25000:38000,increment=region==Region.INTRA?3000:5000,fee=Math.addExact(base,Math.multiplyExact(steps-1,increment));lines.add(new Line(p.weightGrams(),steps*500,fee));total=Math.addExact(total,fee);
        }return new Estimate(lines,total);
    }
}
