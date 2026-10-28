package com.elogistic.backend.merchant.api.customer;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.util.*;

/** Applicant self scope only. A saved draft and its references confer no Merchant authority. */
public interface MerchantApplications {
    Application create(CustomerAdmission.Context context,Input input,UUID key);
    Application update(CustomerAdmission.Context context,UUID id,Input input,long expectedVersion,UUID key);
    Application get(CustomerAdmission.Context context,UUID id);
    Page list(CustomerAdmission.Context context,Query query);
    Receipt receipt(CustomerAdmission.Context context,String operation,UUID key);
    record Address(String country,String administrativeArea,String locality,String addressLine) {
        @Override public String toString(){return "ApplicationAddress[redacted]";}
    }
    record Declaration(boolean accuracy,boolean representation,String consentVersion) {}
    record Input(String entityType,String jurisdiction,String registeredName,String legalIdentifier,
                 Address registeredAddress,Address operatingAddress,String representativeName,String representativeEmail,
                 String representativeRelationship,UUID businessContactAttestationId,List<UUID> evidenceIds,Declaration declarations) {
        public Input {if(evidenceIds!=null)evidenceIds=List.copyOf(evidenceIds);}
        @Override public String toString(){return "ApplicationInput[redacted]";}
    }
    record Application(UUID id,UUID applicantAccountId,String state,long revision,long version,Input draft) {
        @Override public String toString(){return "MerchantApplication[redacted]";}
    }
    record Query(int page,int size,String search,String state) {
        @Override public String toString(){return "ApplicationQuery[redacted]";}
    }
    record Page(List<Application> items,int page,int size,boolean hasNext) {public Page{items=List.copyOf(items);}}
    record Receipt(String operationId,UUID targetId,String outcome,long version) {}
}
