package com.elogistic.backend.pricing.internal.domain;
import java.util.*;

/** Inactive decision-support calculation only: never issues a Quote, service grant or amount due. */
public final class RoadPilotCandidate {
    public enum Region { INTRA,INTER }
    public record Parcel(long weightGrams,long lengthMm,long widthMm,long heightMm){}
    public record Line(long actualGrams,long chargeableGrams,long baseFeeVnd){}
    public record Estimate(List<Line> lines,long baseFeeVnd){public Estimate{lines=List.copyOf(lines);}}
    private RoadPilotCandidate(){}
    public static Estimate estimate(Region region,List<Parcel> parcels){
        if(region==null||parcels==null)throw new IllegalArgumentException("CANDIDATE_INPUT_INVALID");
        var rules=new com.elogistic.backend.pricing.api.PilotQuotes.Rules("candidate-compatibility-only",25000,3000,38000,5000,500,5000,5,20000,1000,2000,900,"SIMULATION_NOT_TAX_INVOICE");
        var inputs=parcels.stream().map(p->{if(p==null)throw new IllegalArgumentException();return new com.elogistic.backend.pricing.api.PilotQuotes.Parcel(p.weightGrams(),p.lengthMm(),p.widthMm(),p.heightMm());}).toList();
        var lines=RoadTariffCalculation.calculate(rules,region.name(),inputs).stream().map(l->new Line(l.actualGrams(),l.chargeableGrams(),l.feeVnd())).toList();long total=0;for(var line:lines)total=Math.addExact(total,line.baseFeeVnd());return new Estimate(lines,total);
    }
}
