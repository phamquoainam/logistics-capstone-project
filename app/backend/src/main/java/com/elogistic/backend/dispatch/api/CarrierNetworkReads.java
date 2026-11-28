package com.elogistic.backend.dispatch.api;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/** Carrier sites, never Merchant warehouses or an implied transport lifecycle. */
public interface CarrierNetworkReads {
    interface Access { void requireNetworkFacts(); }
    record SiteQuery(String state,String type,String areaCode,int page,int size) {}
    record SitePage(List<SiteDetail> items,long total,int page,int size) { public SitePage {items=List.copyOf(items);} }
    record SiteDetail(UUID id,String code,String name,String type,String state,long version,String locationText,String areaCode,BigDecimal latitude,BigDecimal longitude,String timezone,Map<String,List<String>> operatingHours,List<String> capabilities,Instant effectiveFrom,Instant effectiveTo,String provenance,List<SiteLink> outgoing) {
        public SiteDetail {operatingHours=Map.copyOf(operatingHours);capabilities=List.copyOf(capabilities);outgoing=List.copyOf(outgoing);}
    }
    record SiteLink(UUID id,UUID toSiteId,String serviceCode,String datasetVersion,String areaCode,String dropOffCode,Instant effectiveFrom,Instant effectiveTo,long version) {}
    SitePage sites(Access access,SiteQuery query);
    SiteDetail site(Access access,UUID id);
}
