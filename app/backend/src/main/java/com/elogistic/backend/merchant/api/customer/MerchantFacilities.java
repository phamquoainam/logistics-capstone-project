package com.elogistic.backend.merchant.api.customer;

import java.time.Instant;
import java.util.*;

/** Merchant site configuration, not Carrier Station/Hub or stock inventory. */
public interface MerchantFacilities {
    interface Access { UUID actorAccountId(); UUID requestId(); BusinessWorkspaces.Scope scope(); void requireCurrent(); void meaningfulAction(); }
    record Contact(String name,String phone,PersonalAddresses.Location address){@Override public String toString(){return "FacilityContact[redacted]";}}
    record Interval(String opensAt,String closesAt){}
    record Day(String day,List<Interval> intervals){public Day{intervals=intervals==null?null:List.copyOf(intervals);}}
    record Hours(String timeZone,List<Day> days){public Hours{days=days==null?null:List.copyOf(days);}}
    record Input(String name,String code,String kind,Contact contact,Contact senderContact,Contact returnContact,Hours operatingHours){@Override public String toString(){return "FacilityInput[redacted]";}}
    record Facility(UUID id,UUID organizationId,String name,Contact contact,String state,long version,Instant createdAt,Instant updatedAt,String code,String kind,Contact senderContact,Contact returnContact,Hours operatingHours,List<String> allowedActions){public Facility{allowedActions=List.copyOf(allowedActions);}@Override public String toString(){return "MerchantFacility[redacted]";}}
    record Query(int page,int size,String state,String kind,String code,String search){@Override public String toString(){return "FacilityQuery[redacted]";}}
    record Page(List<Facility> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}@Override public String toString(){return "FacilityPage[redacted]";}}
    record Default(UUID facilityId,long version,Instant updatedAt){}
    record Responsibility(UUID accountId,String responsibility){}
    record Responsibilities(UUID facilityId,List<Responsibility> items,long version,Instant updatedAt){public Responsibilities{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    Page list(Access access,Query query);
    Facility get(Access access,UUID id);
    Facility create(Access access,Input input,UUID key);
    Facility update(Access access,UUID id,Input input,long version,UUID key);
    Facility archive(Access access,UUID id,long version,UUID key);
    Default defaultFacility(Access access);
    Default setDefault(Access access,UUID facilityId,long version,UUID key);
    Responsibilities responsibilities(Access access,UUID facilityId);
    Responsibilities setResponsibilities(Access access,UUID facilityId,List<Responsibility> items,long version,UUID key);
    Receipt receipt(Access access,String operation,UUID key);
    final class Failure extends RuntimeException {public Failure(String code){super(code);}}
}
