package com.elogistic.backend.merchant.api.customer;

import java.time.Instant;
import java.util.*;

/** Organization contacts are unverified address snapshots, never Identity credentials. */
public interface OrganizationContacts {
    interface Access { UUID actorAccountId(); UUID requestId(); BusinessWorkspaces.Scope scope(); void requireCurrent(); void meaningfulAction(); }
    record Input(String kind,UUID facilityId,String label,String reference,String group,String contactName,String contactPhone,PersonalAddresses.Location address) {
        @Override public String toString(){return "OrganizationContactInput[redacted]";}
    }
    record Contact(UUID id,UUID organizationId,String kind,UUID facilityId,String label,String reference,String group,String contactName,String contactPhone,PersonalAddresses.Location address,String state,String verificationStatus,long version,Instant createdAt,Instant updatedAt,Instant archivedAt,List<String> allowedActions){
        public Contact{allowedActions=List.copyOf(allowedActions);}
        @Override public String toString(){return "OrganizationContact[redacted]";}
    }
    record Query(int page,int size,String search,String kind,String state,UUID facilityId,String group){@Override public String toString(){return "OrganizationContactQuery[redacted]";}}
    record Page(List<Contact> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}@Override public String toString(){return "OrganizationContacts[redacted]";}}
    record Default(String kind,UUID facilityId,UUID contactId,long version,Instant updatedAt){}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
    Page list(Access access,Query query);
    Contact get(Access access,UUID id);
    Contact create(Access access,Input input,UUID key);
    Contact update(Access access,UUID id,Input input,long version,UUID key);
    Contact archive(Access access,UUID id,long version,UUID key);
    Contact restore(Access access,UUID id,long version,UUID key);
    Page duplicates(Access access,Input input);
    Default defaultContact(Access access,String kind,UUID facility);
    Default setDefault(Access access,String kind,UUID facility,UUID contactId,long version,UUID key);
    Receipt receipt(Access access,String operation,UUID key);
    final class Failure extends RuntimeException {public Failure(String code){super(code);}}
}
