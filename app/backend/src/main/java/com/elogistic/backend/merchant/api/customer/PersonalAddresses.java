package com.elogistic.backend.merchant.api.customer;

import com.elogistic.backend.identity.api.customer.CustomerAdmission;
import java.time.Instant;
import java.util.*;

/** Self-owned contacts only. No Identity credentials, tenant authority or shipping eligibility. */
public interface PersonalAddresses {
    Page list(CustomerAdmission.Context context,Query query);
    Address get(CustomerAdmission.Context context,UUID id);
    Address create(CustomerAdmission.Context context,Input input,UUID key);
    Address update(CustomerAdmission.Context context,UUID id,Input input,long expectedVersion,UUID key);
    void archive(CustomerAdmission.Context context,UUID id,long expectedVersion,UUID key);
    Receipt receipt(CustomerAdmission.Context context,String operation,UUID key);
    record Location(String country,String administrativeArea,String locality,String addressLine){@Override public String toString(){return "AddressLocation[redacted]";}}
    record Input(String kind,String label,String contactName,String contactPhone,Location address,boolean isDefault){@Override public String toString(){return "PersonalAddressInput[redacted]";}}
    record Address(UUID id,UUID accountId,Input input,long version,Instant createdAt,Instant updatedAt){@Override public String toString(){return "PersonalAddress[redacted]";}}
    record Query(int page,int size,String search,String kind){@Override public String toString(){return "PersonalAddressQuery[redacted]";}}
    record Page(List<Address> items,int page,int size,boolean hasNext){public Page{items=List.copyOf(items);}@Override public String toString(){return "PersonalAddressPage[redacted]";}}
    record Receipt(String operationId,UUID targetId,String outcome,long version){}
}
