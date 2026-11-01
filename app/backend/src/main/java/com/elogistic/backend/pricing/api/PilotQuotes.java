package com.elogistic.backend.pricing.api;
import java.time.Instant;
import java.util.*;
public interface PilotQuotes {
    interface Access {UUID accountId();void requireCurrent();void meaningfulAction();}
    record Parcel(long weightGrams,long lengthMm,long widthMm,long heightMm){}
    record Route(String datasetVersion,String originCode,String destinationCode,String classification,String dropOffCode){}
    record Request(UUID draftId,long draftVersion,Route route,List<Parcel> parcels,boolean ordinaryGoodsConfirmed){public Request{parcels=List.copyOf(parcels);}}
    record Rules(String version,long intraFirstVnd,long intraStepVnd,long interFirstVnd,long interStepVnd,int stepGrams,int volumetricDivisor,int maxParcels,int maxActualGrams,int maxDimensionMm,int maxGirthMm,int ttlSeconds,String taxTreatment){}
    record Line(int parcelNumber,long actualGrams,long chargeableGrams,long feeVnd){}
    record Quote(UUID id,UUID accountId,UUID draftId,long draftVersion,String tariffVersion,Route route,List<Parcel> parcels,List<Line> feeLines,long totalVnd,String taxTreatment,Instant createdAt,Instant expiresAt,UUID consumedBy){public Quote{parcels=List.copyOf(parcels);feeLines=List.copyOf(feeLines);}@Override public String toString(){return "PilotQuote[redacted]";}}
    boolean enabled();
    Rules rules(Access access);
    Quote create(Access access,Request request,UUID key);
    Quote get(Access access,UUID id);
    UUID receipt(Access access,UUID key);
    Quote consume(Access access,UUID id,UUID draft,long version,UUID shipment);
}
