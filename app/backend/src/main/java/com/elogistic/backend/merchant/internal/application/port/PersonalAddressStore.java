package com.elogistic.backend.merchant.internal.application.port;

import com.elogistic.backend.merchant.api.customer.PersonalAddresses.Query;
import com.elogistic.backend.merchant.internal.domain.CustomerAddress;
import java.time.Instant;
import java.util.*;

public interface PersonalAddressStore {
    void lockOwner(UUID account);
    Optional<CustomerAddress> find(UUID account,UUID id,boolean lock);
    Optional<CustomerAddress> activeDefault(UUID account,String kind,UUID excluding);
    List<CustomerAddress> list(UUID account,Query query);
    void save(CustomerAddress before,CustomerAddress after);
    CustomerAddress version(UUID account,UUID id,long version);
    Optional<Receipt> receipt(UUID account,String operation,UUID key);
    void receipt(UUID account,String operation,UUID key,Receipt receipt,Instant now);
    void audit(UUID account,String operation,UUID id,long before,long after,UUID request,Instant now);
    record Receipt(UUID target,String keyId,byte[] fingerprint,long originalVersion,long resultVersion){
        public Receipt{fingerprint=fingerprint.clone();}public byte[] fingerprint(){return fingerprint.clone();}
        @Override public String toString(){return "PersonalAddressReceipt[redacted]";}
    }
}
